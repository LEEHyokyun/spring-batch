package com.system.batch.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NukeImpactResult {
    private String targetId;
    private String continent;
    private double latitude;
    private double longitude;
    private long predictedCasualties;
    private String destructionLevel;
    private boolean falloutExpected;
}
