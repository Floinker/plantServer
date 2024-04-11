package de.floinker.plantServer.task;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder(toBuilder = true)
public class PlantData {
    private int id;
    private int plantPotId;
    private Humidity humidity;
    private WaterLevel waterLevel;
    private LocalDateTime timestamp;
}
