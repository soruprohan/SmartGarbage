package com.example.smartgarbage.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Request body for PUT /api/drivers/{id} (text fields only — no photo).
 * Used when updating name/phone without changing the photo.
 */
public class UpdateDriverRequest {

    @SerializedName("name")
    private String name;

    @SerializedName("phone")
    private String phone;

    public UpdateDriverRequest(String name, String phone) {
        this.name  = name;
        this.phone = phone;
    }

    public String getName()  { return name; }
    public String getPhone() { return phone; }
}