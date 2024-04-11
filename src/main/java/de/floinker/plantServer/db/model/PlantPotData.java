package de.floinker.plantServer.db.model;

import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Builder
public class PlantPotData {
    private int id;
    private int plantPotId;
    private int humidty;
    private int fillLevel;
    private Timestamp timestamp;
}
