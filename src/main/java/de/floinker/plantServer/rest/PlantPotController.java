package de.floinker.plantServer.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.floinker.plantServer.db.model.PlantPotConfig;
import de.floinker.plantServer.task.Humidity;
import de.floinker.plantServer.task.WaterAmount;
import de.floinker.plantServer.task.WaterLevel;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/stats")
public class PlantPotController {

    @Autowired
    DataSource dataSource;

    @GetMapping("/pots")
    public ResponseEntity<String> getAllPlantPots() throws SQLException, JsonProcessingException {
        System.out.println("Getting all Plant Pot Configs...");
        List<PlantPotConfig> plantPots = new ArrayList<>();
        ObjectMapper om = new ObjectMapper();

        Statement statement = dataSource.getConnection().createStatement();
        ResultSet resultSet = statement.executeQuery("""
                    SELECT * FROM plant_pot_config;
                """);

        while (resultSet.next()) {
            plantPots.add(PlantPotConfig.builder()
                    .id(resultSet.getInt(1))
                    .ipAddress(resultSet.getString(2))
                    .waterAmount(resultSet.getInt(3))
                    .warnThreshold(resultSet.getString(4))
                    .warnDuration(resultSet.getString(5))
                    .build());
        }

        return new ResponseEntity<>(om.writeValueAsString(plantPots), new HttpHeaders(), HttpStatus.OK);
    }

    @GetMapping("/pots/{potId}/water-level")
    public ResponseEntity<String> getCurrentPlantPotWaterLevel(@PathVariable String potId) throws SQLException, JsonProcessingException {
        System.out.println("Getting Latest Water-Level for Pot [" + potId + "]...");
        WaterLevel waterLevel = null;
        ObjectMapper om = new ObjectMapper();


        PreparedStatement statement = dataSource.getConnection().prepareStatement("""
                    SELECT water_level AS water_level FROM plant_pot_data WHERE plant_pot_id = ? ORDER BY id DESC LIMIT 1;
                """);
        statement.setInt(1, Integer.parseInt(potId));

        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            waterLevel = new WaterLevel(resultSet.getInt(1));
        }

        return new ResponseEntity<>(om.writeValueAsString(waterLevel), new HttpHeaders(), HttpStatus.OK);
    }

    @GetMapping("/pots/{potId}/humidity")
    public ResponseEntity<String> getCurrentPlantPotHumidity(@PathVariable String potId) throws SQLException, JsonProcessingException {
        System.out.println("Getting Latest Humidity for Pot [" + potId + "]...");
        Humidity humidity = null;
        ObjectMapper om = new ObjectMapper();


        PreparedStatement statement = dataSource.getConnection().prepareStatement("""
                    SELECT humidity AS humidity FROM plant_pot_data WHERE plant_pot_id = ? ORDER BY id DESC LIMIT 1;
                """);
        statement.setInt(1, Integer.parseInt(potId));

        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            humidity = new Humidity(resultSet.getInt(1));
        }

        return new ResponseEntity<>(om.writeValueAsString(humidity), new HttpHeaders(), HttpStatus.OK);
    }

    @GetMapping("/pots/{potId}")
    public ResponseEntity<String> getPlantPotDetails(@PathVariable String potId) throws SQLException, JsonProcessingException {
        System.out.println("Getting Details for Pot [" + potId + "]...");
        PlantPotDetailsDto plantPotDetails = PlantPotDetailsDto.builder().plantPotId(Integer.parseInt(potId)).build();
        List<LineChatHistoryDto> humidityHistory = new ArrayList<>();
        List<LineChatHistoryDto> waterLevelHistory = new ArrayList<>();
        ObjectMapper om = new ObjectMapper();


        PreparedStatement statement = dataSource.getConnection().prepareStatement("""
                    SELECT id AS id, humidity AS humidity, water_level AS water_level,timestamp AS timestamp FROM plant_pot_data WHERE plant_pot_id = ? ORDER BY id DESC LIMIT 24;
                """);
        statement.setInt(1, Integer.parseInt(potId));

        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            humidityHistory.add(LineChatHistoryDto.builder()
                    .id(resultSet.getInt(1))
                    .value(resultSet.getInt(2))
                    .timestamp(resultSet.getString(4))
                    .build());
            waterLevelHistory.add(LineChatHistoryDto.builder()
                    .id(resultSet.getInt(1))
                    .value(resultSet.getInt(3))
                    .timestamp(resultSet.getString(4))
                    .build());
        }

        List<LineChatHistoryDto> reversedHumidityList = humidityHistory.subList(0, humidityHistory.size());
        Collections.reverse(reversedHumidityList);
        List<LineChatHistoryDto> reversedWaterLevelList = waterLevelHistory.subList(0, waterLevelHistory.size());
        Collections.reverse(reversedWaterLevelList);

        plantPotDetails = plantPotDetails.toBuilder().humidityHistory(reversedHumidityList).waterLevelHistory(reversedWaterLevelList).currentHumidity(reversedHumidityList.get(reversedHumidityList.size() - 1).getValue()).currentWaterLevel(reversedWaterLevelList.get(reversedWaterLevelList.size() - 1).getValue()).build();

        return new ResponseEntity<>(om.writeValueAsString(plantPotDetails), new HttpHeaders(), HttpStatus.OK);
    }

    @GetMapping("/pots/{potId}/water-level/history")
    public ResponseEntity<String> getPlantPotWaterLevelHistory(@PathVariable String potId) throws SQLException, JsonProcessingException {
        System.out.println("Getting Water Level History for Pot [" + potId + "]...");
        List<LineChatHistoryDto> waterLevelHistory = new ArrayList<>();
        ObjectMapper om = new ObjectMapper();


        PreparedStatement statement = dataSource.getConnection().prepareStatement("""
                    SELECT id AS id, water_level AS water_level, timestamp AS timestamp FROM plant_pot_data WHERE plant_pot_id = ? ORDER BY id DESC LIMIT 24;
                """);
        statement.setInt(1, Integer.parseInt(potId));

        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            waterLevelHistory.add(LineChatHistoryDto.builder()
                    .id(resultSet.getInt(1))
                    .value(resultSet.getInt(2))
                    .timestamp(resultSet.getString(3))
                    .build());
        }

        List<?> reversedList = waterLevelHistory.subList(0, waterLevelHistory.size());
        Collections.reverse(reversedList);

        return new ResponseEntity<>(om.writeValueAsString(reversedList), new HttpHeaders(), HttpStatus.OK);
    }

    @GetMapping("/pots/{potId}/humidity/history")
    public ResponseEntity<String> getPlantPotHumidityHistory(@PathVariable String potId) throws SQLException, JsonProcessingException {
        System.out.println("Getting Humidity History for Pot [" + potId + "]...");
        List<LineChatHistoryDto> humidityHistory = new ArrayList<>();
        ObjectMapper om = new ObjectMapper();


        PreparedStatement statement = dataSource.getConnection().prepareStatement("""
                    SELECT id AS id, humidity AS humidity, timestamp AS timestamp FROM plant_pot_data WHERE plant_pot_id = ? ORDER BY id DESC LIMIT 24;
                """);
        statement.setInt(1, Integer.parseInt(potId));

        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            humidityHistory.add(LineChatHistoryDto.builder()
                    .id(resultSet.getInt(1))
                    .value(resultSet.getInt(2))
                    .timestamp(resultSet.getString(3))
                    .build());
        }

        List<?> reversedList = humidityHistory.subList(0, humidityHistory.size());
        Collections.reverse(reversedList);

        return new ResponseEntity<>(om.writeValueAsString(reversedList), new HttpHeaders(), HttpStatus.OK);
    }

    @PostMapping("/pots/{potId}/identify")
    public ResponseEntity<String> identifyPot(@PathVariable String potId, @RequestBody DurationDto duration) throws SQLException {
        System.out.println("Identifying Pot [" + potId + "]...");

        PreparedStatement statement = dataSource.getConnection().prepareStatement("""
                    SELECT ip_address AS ip_address FROM plant_pot_config WHERE id = ? LIMIT 1;
                """);
        statement.setInt(1, Integer.parseInt(potId));
        ResultSet resultSet = statement.executeQuery();
        String plantPotIpAddress = "";
        while (resultSet.next()) {
            plantPotIpAddress = resultSet.getString(1);
        }

        try {
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + plantPotIpAddress + "/identify"))
                    .POST(HttpRequest.BodyPublishers.ofString(new ObjectMapper().writeValueAsString(duration)))
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("{}", new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>("{}", new HttpHeaders(), HttpStatus.OK);
    }

    @PostMapping("/pots/{potId}/activate-pump")
    public ResponseEntity<String> postActivatePump(@PathVariable String potId, @RequestBody WaterAmount waterAmount) throws SQLException {
        System.out.println("Activating pump for additional " + waterAmount.getWaterAmount() + "ms for Pot [" + potId + "]...");
        ObjectMapper om = new ObjectMapper();

        PreparedStatement statement = dataSource.getConnection().prepareStatement("""
                    SELECT ip_address AS ip_address FROM plant_pot_config WHERE id = ? LIMIT 1;
                """);
        statement.setInt(1, Integer.parseInt(potId));
        ResultSet resultSet = statement.executeQuery();
        String plantPotIpAddress = "";
        while (resultSet.next()) {
            plantPotIpAddress = resultSet.getString(1);
        }

        try {
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + plantPotIpAddress + "/activate-pump"))
                    .POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(waterAmount)))
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("{}", new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>("{}", new HttpHeaders(), HttpStatus.OK);
    }

    @GetMapping("/pots/{potId}/ping")
    public ResponseEntity<String> pingPot(@PathVariable String potId) throws SQLException {
        System.out.println("Pinging Pot [" + potId + "]...");

        PreparedStatement statement = dataSource.getConnection().prepareStatement("""
                    SELECT ip_address AS ip_address FROM plant_pot_config WHERE id = ? LIMIT 1;
                """);
        statement.setInt(1, Integer.parseInt(potId));
        ResultSet resultSet = statement.executeQuery();
        String plantPotIpAddress = "";
        while (resultSet.next()) {
            plantPotIpAddress = resultSet.getString(1);
        }

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            httpClient.execute(new HttpGet("http://" + plantPotIpAddress + "/humidity"),
                    response -> response
            );
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("{}", new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>("{}", new HttpHeaders(), HttpStatus.OK);
    }

    @PutMapping("pots/{potId}/settings")
    public ResponseEntity<String> putPlantPotSettings(@PathVariable String potId, @RequestBody PlantPotConfig plantPotConfig) throws SQLException, JsonProcessingException {
        System.out.println("Updating Pot [" + potId + "]...");
        System.out.println(new ObjectMapper().writeValueAsString(plantPotConfig));


        PreparedStatement statement = dataSource.getConnection().prepareStatement("""
                    UPDATE plant_pot_config SET ip_address = ?, water_amount = ?, warn_threshold = ?, warn_duration = ? WHERE id = ?;
                """);
        statement.setString(1, plantPotConfig.getIpAddress());
        statement.setInt(2, plantPotConfig.getWaterAmount());
        statement.setString(3, plantPotConfig.getWarnThreshold());
        statement.setString(4, plantPotConfig.getWarnDuration());

        statement.setInt(5, Integer.parseInt(potId));
        statement.executeUpdate();

        return new ResponseEntity<>("{}", new HttpHeaders(), HttpStatus.OK);
    }
}
