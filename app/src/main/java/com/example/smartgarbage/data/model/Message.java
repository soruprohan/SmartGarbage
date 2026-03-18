package com.example.smartgarbage.data.model;

import com.google.gson.annotations.SerializedName;

public class Message {

    @SerializedName("id")
    private int id;

    @SerializedName("sender_role")
    private String senderRole;

    @SerializedName("sender_id")
    private int senderId;

    @SerializedName("receiver_role")
    private String receiverRole;

    @SerializedName("receiver_id")
    private int receiverId;

    @SerializedName("content")
    private String content;

    @SerializedName("read_status")
    private boolean readStatus;

    @SerializedName("created_at")
    private String createdAt;

    public int getId() { return id; }
    public String getSenderRole() { return senderRole; }
    public int getSenderId() { return senderId; }
    public String getReceiverRole() { return receiverRole; }
    public int getReceiverId() { return receiverId; }
    public String getContent() { return content; }
    public boolean isReadStatus() { return readStatus; }
    public String getCreatedAt() { return createdAt; }
}