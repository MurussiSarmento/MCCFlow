package com.sap.flowdeconstruct.model;

import java.util.UUID;
import java.util.Date;

/**
 * Simple timeline event with label and logical position [0..1]
 */
public class TimelineEvent {
    private final String id;
    private String label;
    private double position; // range [0..1]
    private Date timestamp; // data/hora do evento

    public TimelineEvent() {
        this.id = UUID.randomUUID().toString();
        this.label = "Event";
        this.position = 0.5;
        this.timestamp = new Date();
    }

    public TimelineEvent(String label, double position) {
        this.id = UUID.randomUUID().toString();
        this.label = label != null ? label : "Event";
        this.position = clamp01(position);
        this.timestamp = new Date();
    }

    // Construtor auxiliar para snapshot/cópia
    public TimelineEvent(String label, double position, Date timestamp) {
        this.id = UUID.randomUUID().toString();
        this.label = label != null ? label : "Event";
        this.position = clamp01(position);
        this.timestamp = timestamp != null ? (Date) timestamp.clone() : null;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label != null ? label : this.label;
    }

    public double getPosition() {
        return position;
    }

    public void setPosition(double position) {
        this.position = clamp01(position);
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp != null ? (Date) timestamp.clone() : null;
    }

    private double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}