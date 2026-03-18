package com.example.smartgarbage.data.model;

import com.google.gson.annotations.SerializedName;

public class SendMessageRequest {

    @SerializedName("receiver_role")
    private String receiverRole;

    @SerializedName("receiver_id")
    private int receiverId;

    @SerializedName("content")
    private String content;

    public SendMessageRequest(String receiverRole, int receiverId, String content) {
        this.receiverRole = receiverRole;
        this.receiverId = receiverId;
        this.content = content;
    }
}