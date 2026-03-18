package com.example.smartgarbage.ui.messages;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

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

    private static final String TAG = "MessagesViewModel";
    private static final long POLL_INTERVAL_MS = 500;

    private final MessageRepository repository;
    private final TokenManager tokenManager;

    private final MutableLiveData<Resource<AdminInfo>> adminInfoLiveData = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<Message>>> messagesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Resource<Message>> sendMessageResult = new MutableLiveData<>();

    // Tracks the last known message count so we only update the UI when something new arrives
    private int lastKnownCount = 0;
    private boolean isFetching = false; // prevents overlapping requests

    private int adminId = -1;

    // Polling machinery
    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private boolean isPolling = false;

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isPolling || adminId < 0) return;
            fetchLatestMessages();
            pollHandler.postDelayed(this, POLL_INTERVAL_MS);
        }
    };

    public MessagesViewModel(@NonNull Application application) {
        super(application);
        tokenManager = new TokenManager(application);
        repository = new MessageRepository(tokenManager);
    }

    public TokenManager getTokenManager() { return tokenManager; }
    public int getDriverId()              { return tokenManager.getDriverId(); }
    public int getAdminId()               { return adminId; }
    public void setAdminId(int id)        { this.adminId = id; }

    // ─── Admin discovery ───

    public LiveData<Resource<AdminInfo>> getAdminInfoLiveData() { return adminInfoLiveData; }

    public void loadAdminInfo() {
        repository.getAdminInfo().observeForever(resource ->
                adminInfoLiveData.setValue(resource));
    }

    // ─── Message list (initial full load) ───

    public LiveData<Resource<List<Message>>> getMessagesLiveData() { return messagesLiveData; }

    public void loadMessages() {
        if (adminId < 0) return;
        messagesLiveData.setValue(Resource.loading());
        repository.getMessages(getDriverId(), adminId).observeForever(resource -> {
            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                lastKnownCount = resource.data.size();
            }
            messagesLiveData.setValue(resource);
        });
    }

    // ─── Polling ───

    /** Call from onStart — begins 500 ms polling after admin is known. */
    public void startPolling() {
        if (isPolling) return;
        if (adminId < 0) {
            Log.d(TAG, "startPolling: adminId not set yet, will auto-start once set");
            return;
        }
        isPolling = true;
        pollHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
        Log.d(TAG, "Polling started");
    }

    /** Call from onStop — stops polling while screen is hidden. */
    public void stopPolling() {
        isPolling = false;
        pollHandler.removeCallbacks(pollRunnable);
        Log.d(TAG, "Polling stopped");
    }

    /**
     * Called by the admin-info observer once adminId is confirmed,
     * in case startPolling() was called before adminId was ready.
     */
    public void setAdminIdAndStartPolling(int id) {
        this.adminId = id;
        if (!isPolling) startPolling();
    }

    private void fetchLatestMessages() {
        if (isFetching || adminId < 0) return;
        isFetching = true;

        repository.getMessages(getDriverId(), adminId).observeForever(resource -> {
            isFetching = false;
            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                int newCount = resource.data.size();
                if (newCount != lastKnownCount) {
                    // Only push to UI when message count actually changes
                    lastKnownCount = newCount;
                    messagesLiveData.setValue(Resource.success(resource.data));
                    Log.d(TAG, "Poll: new messages detected, count=" + newCount);
                }
            }
        });
    }

    // ─── Send message (via socket, with REST as fallback) ───

    public LiveData<Resource<Message>> getSendMessageResult() { return sendMessageResult; }

    public void sendMessageViaRest(String content) {
        if (adminId < 0) return;
        repository.sendMessage(adminId, content).observeForever(resource -> {
            sendMessageResult.setValue(resource);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopPolling();
    }
}