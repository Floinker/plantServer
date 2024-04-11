package de.floinker.plantServer.db.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlantPotConfig {
    private int id;
    private String ipAddress;
    private int waterAmount;
    private String warnThreshold;
    private String warnDuration;
}
