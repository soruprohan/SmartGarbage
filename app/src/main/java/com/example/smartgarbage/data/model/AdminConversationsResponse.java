package com.example.smartgarbage.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AdminConversationsResponse {

    @SerializedName("data")
    private List<AdminInfo> data;

    public List<AdminInfo> getData() { return data; }
}