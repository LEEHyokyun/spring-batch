package com.system.batch.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class ResistanceActivity implements Serializable {
    private static final long serialVersionUID = 1L;

    private String targetId;
    private String continent;
    private double latitude;
    private double longitude;
    private String activityType;
    private int threatLevel;
}
