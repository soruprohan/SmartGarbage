package com.example.smartgarbage.data.model;

public class Bin {
    private int id;
    private String name;
    private String location;
    private Integer driver_id;
    private String driver_name;
    private int capacity;
    private int current_level;
    private String status;
    private Double latitude;
    private Double longitude;
    private String updated_at;

    public int getId() { return id; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public Integer getDriverId() { return driver_id; }
    public String getDriverName() { return driver_name; }
    public int getCapacity() { return capacity; }
    public int getCurrentLevel() { return current_level; }
    public String getStatus() { return status; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getUpdatedAt() { return updated_at; }

    public int getFillPercentage() {
        if (capacity <= 0) return 0;
        return (current_level * 100) / capacity;
    }
}