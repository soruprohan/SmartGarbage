package com.example.smartgarbage.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MessagesResponse {

    @SerializedName("data")
    private List<Message> data;

    public List<Message> getData() { return data; }
}