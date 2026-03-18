package com.example.smartgarbage.data.api;

import com.example.smartgarbage.data.model.AdminConversationsResponse;
import com.example.smartgarbage.data.model.Bin;
import com.example.smartgarbage.data.model.DriverHomeResponse;
import com.example.smartgarbage.data.model.ForgotPasswordRequest;
import com.example.smartgarbage.data.model.ForgotPasswordResponse;
import com.example.smartgarbage.data.model.LoginRequest;
import com.example.smartgarbage.data.model.LoginResponse;
import com.example.smartgarbage.data.model.MessagesResponse;
import com.example.smartgarbage.data.model.SendMessageRequest;
import com.example.smartgarbage.data.model.SendMessageResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @POST("api/drivers/login")
    Call<LoginResponse> loginDriver(@Body LoginRequest request);

    @POST("api/drivers/logout")
    Call<Void> logoutDriver();

    @POST("api/drivers/forgot-password")
    Call<ForgotPasswordResponse> forgotPassword(@Body ForgotPasswordRequest request);

    @GET("api/drivers/home")
    Call<DriverHomeResponse> getDriverHome();

    @GET("api/bins/assigned")
    Call<List<Bin>> getAssignedBins();

    @GET("api/bins/")
    Call<List<Bin>> getAllBins();

    @GET("api/bins/{id}")
    Call<Bin> getBinById(@Path("id") int id);

    // ── Phase 5: Messaging ──

    /** Fetch all admins the driver has conversed with (to discover admin ID) */
    @GET("api/messages/driver-conversations")
    Call<AdminConversationsResponse> getDriverConversations();

    /** Fetch full message history with a specific admin */
    @GET("api/messages/driver")
    Call<MessagesResponse> getMessages(
            @Query("user_role") String userRole,
            @Query("user_id") int userId,
            @Query("other_role") String otherRole,
            @Query("other_id") int otherId
    );

    /** Send a message (REST fallback) */
    @POST("api/messages/driver")
    Call<SendMessageResponse> sendMessage(@Body SendMessageRequest request);
}