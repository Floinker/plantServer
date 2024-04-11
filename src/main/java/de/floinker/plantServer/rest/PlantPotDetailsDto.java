package de.floinker.plantServer.rest;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

import java.util.List;

@Value
@Data
@Builder(toBuilder = true)
public class PlantPotDetailsDto {
    int plantPotId;
    int currentHumidity;
    int currentWaterLevel;
    List<LineChatHistoryDto> humidityHistory;
    List<LineChatHistoryDto> waterLevelHistory;
}
