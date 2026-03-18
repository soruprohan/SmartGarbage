package com.example.smartgarbage.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.smartgarbage.data.api.RetrofitClient;
import com.example.smartgarbage.data.model.AdminConversationsResponse;
import com.example.smartgarbage.data.model.AdminInfo;
import com.example.smartgarbage.data.model.Message;
import com.example.smartgarbage.data.model.MessagesResponse;
import com.example.smartgarbage.data.model.SendMessageRequest;
import com.example.smartgarbage.data.model.SendMessageResponse;
import com.example.smartgarbage.utils.Resource;
import com.example.smartgarbage.utils.TokenManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessageRepository {

    private final TokenManager tokenManager;

    public MessageRepository(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    public LiveData<Resource<AdminInfo>> getAdminInfo() {
        MutableLiveData<Resource<AdminInfo>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        RetrofitClient.getAuthService(tokenManager)
                .getDriverConversations()
                .enqueue(new Callback<AdminConversationsResponse>() {
                    @Override
                    public void onResponse(Call<AdminConversationsResponse> call,
                                           Response<AdminConversationsResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<AdminInfo> list = response.body().getData();
                            if (list != null && !list.isEmpty()) {
                                result.setValue(Resource.success(list.get(0)));
                            } else {
                                // No prior conversation — return null so Activity opens chat anyway
                                result.setValue(Resource.success(null));
                            }
                        } else {
                            result.setValue(Resource.error("Failed to load admin info", null));
                        }
                    }

                    @Override
                    public void onFailure(Call<AdminConversationsResponse> call, Throwable t) {
                        result.setValue(Resource.error(t.getMessage(), null));
                    }
                });

        return result;
    }

    public LiveData<Resource<List<Message>>> getMessages(int driverId, int adminId) {
        MutableLiveData<Resource<List<Message>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        RetrofitClient.getAuthService(tokenManager)
                .getMessages("driver", driverId, "admin", adminId)
                .enqueue(new Callback<MessagesResponse>() {
                    @Override
                    public void onResponse(Call<MessagesResponse> call,
                                           Response<MessagesResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            result.setValue(Resource.success(response.body().getData()));
                        } else {
                            result.setValue(Resource.error("Failed to load messages", null));
                        }
                    }

                    @Override
                    public void onFailure(Call<MessagesResponse> call, Throwable t) {
                        result.setValue(Resource.error(t.getMessage(), null));
                    }
                });

        return result;
    }

    public LiveData<Resource<Message>> sendMessage(int adminId, String content) {
        MutableLiveData<Resource<Message>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        SendMessageRequest req = new SendMessageRequest("admin", adminId, content);

        RetrofitClient.getAuthService(tokenManager)
                .sendMessage(req)
                .enqueue(new Callback<SendMessageResponse>() {
                    @Override
                    public void onResponse(Call<SendMessageResponse> call,
                                           Response<SendMessageResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            result.setValue(Resource.success(response.body().getData()));
                        } else {
                            result.setValue(Resource.error("Failed to send message", null));
                        }
                    }

                    @Override
                    public void onFailure(Call<SendMessageResponse> call, Throwable t) {
                        result.setValue(Resource.error(t.getMessage(), null));
                    }
                });

        return result;
    }
}