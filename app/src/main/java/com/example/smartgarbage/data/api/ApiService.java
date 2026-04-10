package com.example.smartgarbage.data.api;

import com.example.smartgarbage.data.model.AdminConversationsResponse;
import com.example.smartgarbage.data.model.Bin;
import com.example.smartgarbage.data.model.ChangePasswordRequest;
import com.example.smartgarbage.data.model.DriverHomeResponse;
import com.example.smartgarbage.data.model.DriverProfile;
import com.example.smartgarbage.data.model.ForgotPasswordRequest;
import com.example.smartgarbage.data.model.ForgotPasswordResponse;
import com.example.smartgarbage.data.model.LoginRequest;
import com.example.smartgarbage.data.model.LoginResponse;
import com.example.smartgarbage.data.model.MessagesResponse;
import com.example.smartgarbage.data.model.SendMessageRequest;
import com.example.smartgarbage.data.model.SendMessageResponse;
import com.example.smartgarbage.data.model.UpdateDriverResponse;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // ── Auth ──────────────────────────────────────────────────────────────────

    @POST("api/drivers/login")
    Call<LoginResponse> loginDriver(@Body LoginRequest request);

    @POST("api/drivers/logout")
    Call<Void> logoutDriver();

    @POST("api/drivers/forgot-password")
    Call<ForgotPasswordResponse> forgotPassword(@Body ForgotPasswordRequest request);

    @GET("api/drivers/home")
    Call<DriverHomeResponse> getDriverHome();

    // ── Profile ───────────────────────────────────────────────────────────────

    @GET("api/drivers/{id}")
    Call<DriverProfile> getDriverById(@Path("id") int id);

    @Multipart
    @PUT("api/drivers/{id}")
    Call<UpdateDriverResponse> updateDriverMultipart(
            @Path("id") int id,
            @Part("name") RequestBody name,
            @Part("phone") RequestBody phone
    );

    @Multipart
    @PUT("api/drivers/{id}")
    Call<UpdateDriverResponse> updateDriverWithPhoto(
            @Path("id") int id,
            @Part("name") RequestBody name,
            @Part("phone") RequestBody phone,
            @Part MultipartBody.Part photo
    );

    @PUT("api/drivers/{id}")
    Call<UpdateDriverResponse> changePassword(
            @Path("id") int id,
            @Body ChangePasswordRequest body
    );

    // ── Bins ──────────────────────────────────────────────────────────────────

    @GET("api/bins/assigned")
    Call<List<Bin>> getAssignedBins();

    @GET("api/bins/")
    Call<List<Bin>> getAllBins();

    @GET("api/bins/{id}")
    Call<Bin> getBinById(@Path("id") int id);

    // ── Phase 5: Messaging ────────────────────────────────────────────────────

    @GET("api/messages/driver-conversations")
    Call<AdminConversationsResponse> getDriverConversations();

    @GET("api/messages/driver")
    Call<MessagesResponse> getMessages(
            @Query("user_role") String userRole,
            @Query("user_id") int userId,
            @Query("other_role") String otherRole,
            @Query("other_id") int otherId
    );

    @POST("api/messages/driver")
    Call<SendMessageResponse> sendMessage(@Body SendMessageRequest request);
}