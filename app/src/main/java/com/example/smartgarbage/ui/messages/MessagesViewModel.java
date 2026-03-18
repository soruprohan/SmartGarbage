package com.example.smartgarbage.ui.messages;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.smartgarbage.data.model.AdminInfo;
import com.example.smartgarbage.data.model.Message;
import com.example.smartgarbage.data.repository.MessageRepository;
import com.example.smartgarbage.utils.Resource;
import com.example.smartgarbage.utils.TokenManager;

import java.util.ArrayList;
import java.util.List;

public class MessagesViewModel extends AndroidViewModel {

    private final MessageRepository repository;
    private final TokenManager tokenManager;

    // Observed by the Activity
    private final MutableLiveData<Resource<AdminInfo>> adminInfoLiveData = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<Message>>> messagesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Resource<Message>> sendMessageResult = new MutableLiveData<>();

    // In-memory message list so we can append real-time messages
    private final List<Message> currentMessages = new ArrayList<>();

    // Cached admin id after discovery
    private int adminId = -1;

    public MessagesViewModel(@NonNull Application application) {
        super(application);
        tokenManager = new TokenManager(application);
        repository = new MessageRepository(tokenManager);
    }

    public TokenManager getTokenManager() { return tokenManager; }

    public int getDriverId() { return tokenManager.getDriverId(); }

    public int getAdminId() { return adminId; }

    public void setAdminId(int id) { this.adminId = id; }

    // ─── Admin discovery ───

    public LiveData<Resource<AdminInfo>> getAdminInfoLiveData() { return adminInfoLiveData; }

    public void loadAdminInfo() {
        repository.getAdminInfo().observeForever(resource -> {
            adminInfoLiveData.setValue(resource);
        });
    }

    // ─── Message history ───

    public LiveData<Resource<List<Message>>> getMessagesLiveData() { return messagesLiveData; }

    public void loadMessages() {
        if (adminId < 0) return;
        repository.getMessages(getDriverId(), adminId).observeForever(resource -> {
            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                currentMessages.clear();
                currentMessages.addAll(resource.data);
            }
            messagesLiveData.setValue(resource);
        });
    }

    // ─── Append real-time message ───

    public void appendMessage(Message message) {
        currentMessages.add(message);
        messagesLiveData.setValue(Resource.success(new ArrayList<>(currentMessages)));
    }

    // ─── Send message (REST fallback) ───

    public LiveData<Resource<Message>> getSendMessageResult() { return sendMessageResult; }

    public void sendMessageViaRest(String content) {
        if (adminId < 0) return;
        repository.sendMessage(adminId, content).observeForever(resource -> {
            sendMessageResult.setValue(resource);
            // Append immediately to local list for instant feedback
            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                appendMessage(resource.data);
            }
        });
    }
}