package com.example.smartgarbage.ui.map;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.smartgarbage.BuildConfig;
import com.example.smartgarbage.data.api.DirectionsRetrofitClient;
import com.example.smartgarbage.data.api.RetrofitClient;
import com.example.smartgarbage.data.model.Bin;
import com.example.smartgarbage.data.model.DirectionsResponse;
import com.example.smartgarbage.utils.Resource;
import com.example.smartgarbage.utils.TokenManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapViewModel extends AndroidViewModel {

    private static final String TAG = "MapViewModel";
    private static final int CRITICAL_THRESHOLD = 80;

    private final TokenManager tokenManager;

    private final MutableLiveData<Resource<List<Bin>>> binsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Resource<DirectionsResponse>> directionsLiveData = new MutableLiveData<>();

    public MapViewModel(@NonNull Application application) {
        super(application);
        tokenManager = new TokenManager(application);
    }

    public LiveData<Resource<List<Bin>>> getBins() {
        return binsLiveData;
    }

    public LiveData<Resource<DirectionsResponse>> getDirections() {
        return directionsLiveData;
    }

    public void loadAssignedBins() {
        binsLiveData.setValue(Resource.loading());

        RetrofitClient.getAuthService(tokenManager).getAssignedBins()
                .enqueue(new Callback<List<Bin>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<Bin>> call,
                                           @NonNull Response<List<Bin>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            binsLiveData.setValue(Resource.success(response.body()));
                        } else if (response.code() == 401) {
                            binsLiveData.setValue(Resource.error("Session expired", null));
                        } else {
                            binsLiveData.setValue(Resource.error("Failed to load bins", null));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<Bin>> call, @NonNull Throwable t) {
                        binsLiveData.setValue(Resource.error("Network error: " + t.getMessage(), null));
                    }
                });
    }

    /**
     * Returns bins with fill >= CRITICAL_THRESHOLD, sorted fill descending.
     */
    public List<Bin> getRouteBins(List<Bin> allBins) {
        List<Bin> route = new ArrayList<>();
        for (Bin b : allBins) {
            if (b.getFillPercentage() >= CRITICAL_THRESHOLD
                    && b.getLatitude() != null
                    && b.getLongitude() != null) {
                route.add(b);
            }
        }
        // Sort by fill % descending (highest priority first — Google will re-optimize order)
        route.sort((a, b) -> Integer.compare(b.getFillPercentage(), a.getFillPercentage()));
        return route;
    }

    /**
     * Fetch optimized directions.
     * - optimize:true tells Google to reorder the waypoints for the fastest route
     * - departure_time=now factors in live traffic
     */
    public void fetchOptimizedRoute(double originLat, double originLng, List<Bin> routeBins) {
        if (routeBins == null || routeBins.isEmpty()) {
            directionsLiveData.setValue(Resource.error("No critical bins to route", null));
            return;
        }

        directionsLiveData.setValue(Resource.loading());

        String origin = originLat + "," + originLng;

        // Last bin is the destination (Google never reorders the destination)
        Bin lastBin = routeBins.get(routeBins.size() - 1);
        String destination = lastBin.getLatitude() + "," + lastBin.getLongitude();

        // Build waypoints string with optimize:true
        // Only middle bins (all except the last) go in as waypoints
        String waypoints = null;
        if (routeBins.size() > 1) {
            StringBuilder waypointsBuilder = new StringBuilder("optimize:true");
            for (int i = 0; i < routeBins.size() - 1; i++) {
                Bin bin = routeBins.get(i);
                waypointsBuilder.append("|")
                        .append(bin.getLatitude())
                        .append(",")
                        .append(bin.getLongitude());
            }
            waypoints = waypointsBuilder.toString();
        }

        String apiKey = BuildConfig.MAPS_API_KEY;

        Log.d(TAG, "Fetching route: origin=" + origin
                + " dest=" + destination + " waypoints=" + waypoints);

        DirectionsRetrofitClient.getInstance()
                .getDirections(origin, destination, waypoints, "now", apiKey)
                .enqueue(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<DirectionsResponse> call,
                                           @NonNull Response<DirectionsResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            DirectionsResponse body = response.body();
                            if ("OK".equals(body.status) && body.routes != null && !body.routes.isEmpty()) {
                                directionsLiveData.setValue(Resource.success(body));
                            } else {
                                directionsLiveData.setValue(
                                        Resource.error("Directions API: " + body.status, null));
                            }
                        } else {
                            directionsLiveData.setValue(
                                    Resource.error("Failed to fetch route: HTTP " + response.code(), null));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<DirectionsResponse> call, @NonNull Throwable t) {
                        directionsLiveData.setValue(
                                Resource.error("Route network error: " + t.getMessage(), null));
                    }
                });
    }

    public TokenManager getTokenManager() {
        return tokenManager;
    }
}