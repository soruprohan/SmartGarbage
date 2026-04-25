package com.example.smartgarbage.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Response from PUT /api/bins/{id}/collect
 * { "success": true, "message": "...", "bin": { ... } }
 */
public class CollectBinResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("bin")
    private Bin bin;

    public boolean isSuccess() { return success; }
    public String getMessage()  { return message; }
    public Bin getBin()         { return bin; }
}
