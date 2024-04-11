package de.floinker.plantServer.rest;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

@Value
@Data
@Builder
public class LineChatHistoryDto {
    int id;
    int value;
    String timestamp;
}
