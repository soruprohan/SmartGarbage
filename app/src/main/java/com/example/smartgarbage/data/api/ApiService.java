package com.example.smartgarbage.data.api;

import com.example.smartgarbage.data.model.Bin;
import com.example.smartgarbage.data.model.DriverHomeResponse;
import com.example.smartgarbage.data.model.ForgotPasswordRequest;
import com.example.smartgarbage.data.model.ForgotPasswordResponse;
import com.example.smartgarbage.data.model.LoginRequest;
import com.example.smartgarbage.data.model.LoginResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {

    @POST("api/drivers/login")
    Call<LoginResponse> loginDriver(@Body LoginRequest request);

    @POST("api/drivers/logout")
    Call<Void> logoutDriver();

    @POST("api/drivers/forgot-password")
    Call<ForgotPasswordResponse> forgotPassword(@Body ForgotPasswordRequest request);

    @GET("api/drivers/home")
    Call<DriverHomeResponse> getDriverHome();

    @GET("api/bins/")
    Call<List<Bin>> getAllBins();

    @GET("api/bins/{id}")
    Call<Bin> getBinById(@Path("id") int id);
}