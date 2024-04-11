package de.floinker.plantServer.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.floinker.plantServer.db.model.PlantPotConfig;
import de.floinker.plantServer.rest.DurationDto;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.sql.DataSource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Configuration
@EnableScheduling
public class GetPlantDataScheduler {
    @Autowired
    DataSource dataSource;

    private ObjectMapper objectMapper = new ObjectMapper();


    @Scheduled(fixedDelay = 10000)
    public void getPlantPotData() {
        List<PlantPotConfig> plantPotConfigs = getPlantPotConfigs();
        AtomicReference<PlantData> plantData = new AtomicReference<>(PlantData.builder().build());

        for (PlantPotConfig plantPotConfig : plantPotConfigs) {
            try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
                plantData.set(plantData.get().toBuilder().plantPotId(plantPotConfig.getId()).build());
                httpClient.execute(new HttpGet("http://" + plantPotConfig.getIpAddress() + "/humidity"),
                        response -> {
                            ;
                            Humidity humidity = objectMapper.readValue(EntityUtils.toString(response.getEntity()).replace("[", "").replace("]", ""), Humidity.class);
                            plantData.set(plantData.get().toBuilder().humidity(humidity).build());
                            return response;
                        });
                httpClient.execute(new HttpGet("http://" + plantPotConfig.getIpAddress() + "/water-level"),
                        response -> {
                            WaterLevel waterLevel = objectMapper.readValue(EntityUtils.toString(response.getEntity()).replace("[", "").replace("]", ""), WaterLevel.class);
                            plantData.set(plantData.get().toBuilder().waterLevel(waterLevel).build());
                            return response;
                        });
                persistPlantData(plantData.get());

                checkShouldCanWater(plantPotConfig, plantData.get());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void checkShouldCanWater(PlantPotConfig plantPotConfig, PlantData plantData) {
        if (plantData.getWaterLevel().getWaterLevel() > 20 && plantData.getHumidity().getHumidity() < 40) {
            try {
                System.out.println("Pumping additional " + plantPotConfig.getWaterAmount() + "ms for plant pot " + plantPotConfig.getId());

                HttpClient httpClient = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://" + plantPotConfig.getIpAddress() + "/activate-pump"))
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(WaterAmount.builder().waterAmount(plantPotConfig.getWaterAmount()))))
                        .build();
                httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (plantData.getWaterLevel().getWaterLevel() < 20) {
            try {
                System.out.println("Water Level low for plant pot " + plantPotConfig.getId() + "! Blinking LED for " + plantPotConfig.getWarnDuration() + " seconds!");

                HttpClient httpClient = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://" + plantPotConfig.getIpAddress() + "/identify"))
                        .POST(HttpRequest.BodyPublishers.ofString(new ObjectMapper().writeValueAsString(DurationDto.builder().duration(Integer.parseInt(plantPotConfig.getWarnDuration())).build())))
                        .build();
                httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void persistPlantData(PlantData plantData) throws SQLException {
        String insert = """
                INSERT INTO plant_pot_data (id,
                                              plant_pot_id,
                                              humidity,
                                              water_level,
                                              timestamp)
                VALUES (
                """;
        insert += "NULL,"
                + plantData.getPlantPotId() + ","
                + plantData.getHumidity().getHumidity() + ","
                + plantData.getWaterLevel().getWaterLevel() + ","
                + "CURRENT_TIMESTAMP" + ")";

        Statement statement = dataSource.getConnection().createStatement();

        statement.executeUpdate(insert);
    }

    private List<PlantPotConfig> getPlantPotConfigs() {
        List<PlantPotConfig> plantPotConfigs = new ArrayList<>();
        try {
            Statement statement = dataSource.getConnection().createStatement();
            ResultSet resultSet = statement.executeQuery("""
                        SELECT id AS id, ip_address AS ip_address, water_amount AS water_amount, warn_threshold AS warn_threshold, warn_duration AS warn_duration FROM plant_pot_config;
                    """);
            while (resultSet.next()) {
                plantPotConfigs.add(
                        PlantPotConfig.builder()
                                .id(resultSet.getInt("id"))
                                .ipAddress(resultSet.getString("ip_address"))
                                .waterAmount(resultSet.getInt("water_amount"))
                                .warnThreshold(resultSet.getString("warn_threshold"))
                                .warnDuration(resultSet.getString("warn_duration"))
                                .warnDuration(resultSet.getString("warn_duration"))
                                .build());
            }
            statement.close();
            return plantPotConfigs;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
