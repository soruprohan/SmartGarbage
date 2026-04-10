package com.example.smartgarbage.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Full driver profile returned by GET /api/drivers/{id}
 * Includes bins array and all editable fields.
 */
public class DriverProfile {

    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("phone")
    private String phone;

    @SerializedName("photo_url")
    private String photoUrl;

    @SerializedName("status")
    private String status;

    @SerializedName("bins")
    private List<Bin> bins;

    // Getters
    public int getId()           { return id; }
    public String getName()      { return name; }
    public String getEmail()     { return email; }
    public String getPhone()     { return phone; }
    public String getPhotoUrl()  { return photoUrl; }
    public String getStatus()    { return status; }
    public List<Bin> getBins()   { return bins; }

    public int getAssignedBinCount() {
        if (bins == null) return 0;
        // bins list may contain null entries from LEFT JOIN when no bins assigned
        int count = 0;
        for (Bin b : bins) { if (b != null && b.getId() != 0) count++; }
        return count;
    }
}