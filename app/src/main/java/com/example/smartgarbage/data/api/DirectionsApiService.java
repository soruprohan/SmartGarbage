package com.example.smartgarbage.data.api;

import com.example.smartgarbage.data.model.DirectionsResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface DirectionsApiService {

    @GET("maps/api/directions/json")
    Call<DirectionsResponse> getDirections(
            @Query("origin") String origin,
            @Query("destination") String destination,
            @Query("waypoints") String waypoints,
            @Query("departure_time") String departureTime,
            @Query("key") String apiKey
    );
}