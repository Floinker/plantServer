package de.floinker.plantServer.db;

import de.floinker.plantServer.db.model.PlantPotConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@Configuration
public class DBinitializeConfig {
    private final List<PlantPotConfig> plantPots = List.of(
            PlantPotConfig
                    .builder()
                    .id(1)
                    .ipAddress("192.168.1.189:80")
                    .waterAmount(500)
                    .warnThreshold("10")
                    .warnDuration("30000")
                    .build());
    @Autowired
    private DataSource dataSource;

    public DBinitializeConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void initialize() {
        try {
            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS plant_pot_config(" +
                            "id INTEGER Primary key, " +
                            "ip_address VARCHAR(30) not null," +
                            "water_amount INTEGER not null," +
                            "warn_threshold INTEGER not null," +
                            "warn_duration INTEGER not null)"
            );
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS plant_pot_data(" +
                            "id INTEGER Primary key, " +
                            "plant_pot_id INTEGER not null," +
                            "humidity INTEGER not null," +
                            "water_level INTEGER not null, " +
                            "timestamp TIMESTAMP not null," +
                            "   FOREIGN KEY(plant_pot_id)" +
                            "       REFERENCES plant_pot_config(id))"
            );

            ResultSet resultSet = statement.executeQuery("""
                        SELECT COUNT(*) AS config_count FROM plant_pot_config;
                    """);

            if (resultSet.getInt("config_count") == 0) {
                for (PlantPotConfig plantPotConfig : plantPots) {
                    String insert = """
                            INSERT INTO plant_pot_config (id,
                                                          ip_address,
                                                          water_amount,
                                                          warn_threshold,
                                                          warn_duration)
                            VALUES (
                            """;
                    insert += plantPotConfig.getId() + ",'"
                            + plantPotConfig.getIpAddress() + "',"
                            + plantPotConfig.getWaterAmount() + ","
                            + plantPotConfig.getWarnThreshold() + ","
                            + plantPotConfig.getWarnDuration() + ")";
                    statement.executeUpdate(insert);
                }
            }

            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
