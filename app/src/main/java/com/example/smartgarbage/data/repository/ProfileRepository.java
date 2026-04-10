package com.example.smartgarbage.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.smartgarbage.data.api.ApiService;
import com.example.smartgarbage.data.api.RetrofitClient;
import com.example.smartgarbage.data.model.ChangePasswordRequest;
import com.example.smartgarbage.data.model.DriverHomeResponse;
import com.example.smartgarbage.data.model.DriverProfile;
import com.example.smartgarbage.data.model.UpdateDriverResponse;
import com.example.smartgarbage.utils.Resource;
import com.example.smartgarbage.utils.TokenManager;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileRepository {

    private final ApiService api;
    private final TokenManager tokenManager;

    public ProfileRepository(Application app) {
        tokenManager = new TokenManager(app);
        api = RetrofitClient.getAuthService(tokenManager);
    }

    // ── Fetch profile ─────────────────────────────────────────────────────────

    /**
     * Tries GET /api/drivers/{id} first (full profile with bins).
     * The backend protects this with admin middleware, so we fall back
     * to GET /api/drivers/home if we get a 401/403.
     */
    public LiveData<Resource<DriverProfile>> fetchProfile() {
        MutableLiveData<Resource<DriverProfile>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        int driverId = tokenManager.getDriverId();

        api.getDriverById(driverId).enqueue(new Callback<DriverProfile>() {
            @Override
            public void onResponse(Call<DriverProfile> call, Response<DriverProfile> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.postValue(Resource.success(response.body()));
                } else if (response.code() == 401 || response.code() == 403) {
                    // Fallback: build a minimal DriverProfile from /home endpoint
                    fetchProfileFallback(result);
                } else {
                    result.postValue(Resource.error("Failed to load profile (code " + response.code() + ")", null));
                }
            }

            @Override
            public void onFailure(Call<DriverProfile> call, Throwable t) {
                result.postValue(Resource.error("Network error: " + t.getMessage(), null));
            }
        });

        return result;
    }

    private void fetchProfileFallback(MutableLiveData<Resource<DriverProfile>> result) {
        api.getDriverHome().enqueue(new Callback<DriverHomeResponse>() {
            @Override
            public void onResponse(Call<DriverHomeResponse> call, Response<DriverHomeResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().getDriver() != null) {
                    // Wrap minimal data in a DriverProfile-like object via subclass trick
                    DriverHomeResponse.Driver d = response.body().getDriver();
                    // We can't easily construct DriverProfile (all private fields / Gson sets them),
                    // so we create a simple wrapper using the FallbackProfile inner class
                    result.postValue(Resource.success(buildFallbackProfile(d)));
                } else {
                    result.postValue(Resource.error("Failed to load profile", null));
                }
            }

            @Override
            public void onFailure(Call<DriverHomeResponse> call, Throwable t) {
                result.postValue(Resource.error("Network error: " + t.getMessage(), null));
            }
        });
    }

    /**
     * Builds a DriverProfile object from the minimal /home response
     * using TokenManager cached values.
     */
    private DriverProfile buildFallbackProfile(DriverHomeResponse.Driver d) {
        // Use Gson to deserialize a hand-crafted JSON string — avoids reflection issues
        com.google.gson.Gson gson = new com.google.gson.Gson();
        String json = "{"
                + "\"id\":" + d.getId() + ","
                + "\"name\":\"" + escapeJson(d.getName()) + "\","
                + "\"email\":\"" + escapeJson(d.getEmail()) + "\","
                + "\"phone\":null,"
                + "\"photo_url\":null,"
                + "\"status\":\"active\","
                + "\"bins\":[]"
                + "}";
        return gson.fromJson(json, DriverProfile.class);
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // ── Update profile info ────────────────────────────────────────────────────

    public LiveData<Resource<UpdateDriverResponse>> updateProfile(int driverId, String name, String phone) {
        MutableLiveData<Resource<UpdateDriverResponse>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        RequestBody namePart  = RequestBody.create(MediaType.parse("text/plain"), name != null ? name : "");
        RequestBody phonePart = RequestBody.create(MediaType.parse("text/plain"), phone != null ? phone : "");

        api.updateDriverMultipart(driverId, namePart, phonePart)
                .enqueue(new Callback<UpdateDriverResponse>() {
                    @Override
                    public void onResponse(Call<UpdateDriverResponse> call, Response<UpdateDriverResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            // Update cached name in TokenManager
                            if (response.body().getDriver() != null) {
                                tokenManager.saveDriverInfo(
                                        driverId,
                                        response.body().getDriver().getName(),
                                        response.body().getDriver().getEmail()
                                );
                            }
                            result.postValue(Resource.success(response.body()));
                        } else {
                            result.postValue(Resource.error("Update failed (code " + response.code() + ")", null));
                        }
                    }

                    @Override
                    public void onFailure(Call<UpdateDriverResponse> call, Throwable t) {
                        result.postValue(Resource.error("Network error: " + t.getMessage(), null));
                    }
                });

        return result;
    }

    // ── Update with photo ──────────────────────────────────────────────────────

    public LiveData<Resource<UpdateDriverResponse>> updateProfileWithPhoto(
            int driverId, String name, String phone, File photoFile) {
        MutableLiveData<Resource<UpdateDriverResponse>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        RequestBody namePart  = RequestBody.create(MediaType.parse("text/plain"), name != null ? name : "");
        RequestBody phonePart = RequestBody.create(MediaType.parse("text/plain"), phone != null ? phone : "");

        RequestBody fileBody = RequestBody.create(MediaType.parse("image/*"), photoFile);
        MultipartBody.Part photoPart = MultipartBody.Part.createFormData("photo", photoFile.getName(), fileBody);

        api.updateDriverWithPhoto(driverId, namePart, phonePart, photoPart)
                .enqueue(new Callback<UpdateDriverResponse>() {
                    @Override
                    public void onResponse(Call<UpdateDriverResponse> call, Response<UpdateDriverResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            if (response.body().getDriver() != null) {
                                tokenManager.saveDriverInfo(
                                        driverId,
                                        response.body().getDriver().getName(),
                                        response.body().getDriver().getEmail()
                                );
                            }
                            result.postValue(Resource.success(response.body()));
                        } else {
                            result.postValue(Resource.error("Photo upload failed (code " + response.code() + ")", null));
                        }
                    }

                    @Override
                    public void onFailure(Call<UpdateDriverResponse> call, Throwable t) {
                        result.postValue(Resource.error("Network error: " + t.getMessage(), null));
                    }
                });

        return result;
    }

    // ── Change password ────────────────────────────────────────────────────────

    public LiveData<Resource<UpdateDriverResponse>> changePassword(int driverId, String newPassword) {
        MutableLiveData<Resource<UpdateDriverResponse>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        api.changePassword(driverId, new ChangePasswordRequest(newPassword))
                .enqueue(new Callback<UpdateDriverResponse>() {
                    @Override
                    public void onResponse(Call<UpdateDriverResponse> call, Response<UpdateDriverResponse> response) {
                        if (response.isSuccessful()) {
                            result.postValue(Resource.success(response.body()));
                        } else {
                            result.postValue(Resource.error("Password change failed (code " + response.code() + ")", null));
                        }
                    }

                    @Override
                    public void onFailure(Call<UpdateDriverResponse> call, Throwable t) {
                        result.postValue(Resource.error("Network error: " + t.getMessage(), null));
                    }
                });

        return result;
    }

    public TokenManager getTokenManager() { return tokenManager; }
}