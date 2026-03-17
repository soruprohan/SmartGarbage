package com.example.smartgarbage.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.smartgarbage.data.api.ApiService;
import com.example.smartgarbage.data.api.RetrofitClient;
import com.example.smartgarbage.data.model.Bin;
import com.example.smartgarbage.utils.Resource;
import com.example.smartgarbage.utils.TokenManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BinRepository {

    private final ApiService apiService;

    public BinRepository(TokenManager tokenManager) {
        this.apiService = RetrofitClient.getAuthService(tokenManager);
    }

    /**
     * Fetches all bins assigned to the authenticated driver via
     * GET /api/bins/assigned  (authenticateDriver middleware).
     */
    public LiveData<Resource<List<Bin>>> getAssignedBins() {
        MutableLiveData<Resource<List<Bin>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        apiService.getAssignedBins().enqueue(new Callback<List<Bin>>() {
            @Override
            public void onResponse(Call<List<Bin>> call, Response<List<Bin>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(Resource.success(response.body()));
                } else if (response.code() == 401) {
                    result.setValue(Resource.error("Session expired. Please log in again.", null));
                } else {
                    result.setValue(Resource.error("Failed to load bins (code " + response.code() + ")", null));
                }
            }

            @Override
            public void onFailure(Call<List<Bin>> call, Throwable t) {
                result.setValue(Resource.error("Network error: " + t.getMessage(), null));
            }
        });

        return result;
    }
}
