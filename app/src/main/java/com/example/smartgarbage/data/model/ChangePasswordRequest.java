package com.example.smartgarbage.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Request body for changing password via PUT /api/drivers/{id}.
 * The backend uses COALESCE so only provided fields are updated.
 * We send the new hashed password via the `password` field.
 */
public class ChangePasswordRequest {

    @SerializedName("password")
    private String password;

    public ChangePasswordRequest(String newPlainPassword) {
        this.password = newPlainPassword;
    }

    public String getPassword() { return password; }
}