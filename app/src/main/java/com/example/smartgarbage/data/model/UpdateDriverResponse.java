package com.example.smartgarbage.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Response from PUT /api/drivers/{id}
 * { "message": "...", "driver": { ... } }
 */
public class UpdateDriverResponse {

    @SerializedName("message")
    private String message;

    @SerializedName("driver")
    private DriverProfile driver;

    public String getMessage()       { return message; }
    public DriverProfile getDriver() { return driver; }
}