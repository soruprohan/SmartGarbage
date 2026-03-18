package com.example.smartgarbage.data.model;

import com.google.gson.annotations.SerializedName;

public class AdminInfo {

    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("last_message")
    private String lastMessage;

    @SerializedName("last_message_time")
    private String lastMessageTime;

    @SerializedName("unread_count")
    private int unreadCount;

    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getLastMessage() { return lastMessage; }
    public String getLastMessageTime() { return lastMessageTime; }
    public int getUnreadCount() { return unreadCount; }
}