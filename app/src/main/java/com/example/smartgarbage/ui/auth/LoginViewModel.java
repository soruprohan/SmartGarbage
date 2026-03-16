package com.example.smartgarbage.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.smartgarbage.data.api.ApiService;
import com.example.smartgarbage.data.api.RetrofitClient;
import com.example.smartgarbage.data.model.DriverHomeResponse;
import com.example.smartgarbage.data.model.ForgotPasswordRequest;
import com.example.smartgarbage.data.model.ForgotPasswordResponse;
import com.example.smartgarbage.data.model.LoginRequest;
import com.example.smartgarbage.data.model.LoginResponse;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginViewModel extends ViewModel {

    // Login state
    private final MutableLiveData<Boolean> loginLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> loginError = new MutableLiveData<>();
    private final MutableLiveData<LoginResult> loginSuccess = new MutableLiveData<>();

    // Forgot password state
    private final MutableLiveData<Boolean> forgotLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> forgotError = new MutableLiveData<>();
    private final MutableLiveData<String> forgotSuccess = new MutableLiveData<>();

    public LiveData<Boolean> getLoginLoading() { return loginLoading; }
    public LiveData<String> getLoginError() { return loginError; }
    public LiveData<LoginResult> getLoginSuccess() { return loginSuccess; }
    public LiveData<Boolean> getForgotLoading() { return forgotLoading; }
    public LiveData<String> getForgotError() { return forgotError; }
    public LiveData<String> getForgotSuccess() { return forgotSuccess; }

    // ----- Login -----

    public void login(String email, String password) {
        loginLoading.setValue(true);
        loginError.setValue(null);

        ApiService api = RetrofitClient.getPublicService();
        api.loginDriver(new LoginRequest(email, password))
                .enqueue(new Callback<LoginResponse>() {
                    @Override
                    public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                        loginLoading.setValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            String token = response.body().getToken();
                            loginSuccess.setValue(new LoginResult(token));
                        } else {
                            // Parse error body from server
                            String msg = parseErrorMessage(response);
                            loginError.setValue(msg);
                        }
                    }

                    @Override
                    public void onFailure(Call<LoginResponse> call, Throwable t) {
                        loginLoading.setValue(false);
                        loginError.setValue("Network error: " + t.getMessage());
                    }
                });
    }

    // After login, fetch driver info using the token
    public void fetchDriverInfo(String token,
                                OnDriverInfoFetchedCallback callback) {
        ApiService api = RetrofitClient.getAuthService(token);
        api.getDriverHome().enqueue(new Callback<DriverHomeResponse>() {
            @Override
            public void onResponse(Call<DriverHomeResponse> call,
                                   Response<DriverHomeResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().getDriver() != null) {
                    callback.onSuccess(response.body());
                } else {
                    // Not fatal — we still have the token; proceed without driver info cache
                    callback.onSuccess(null);
                }
            }
            @Override
            public void onFailure(Call<DriverHomeResponse> call, Throwable t) {
                callback.onSuccess(null); // Non-fatal
            }
        });
    }

    // ----- Forgot Password -----

    public void sendForgotPassword(String email) {
        forgotLoading.setValue(true);
        forgotError.setValue(null);

        ApiService api = RetrofitClient.getPublicService();
        api.forgotPassword(new ForgotPasswordRequest(email))
                .enqueue(new Callback<ForgotPasswordResponse>() {
                    @Override
                    public void onResponse(Call<ForgotPasswordResponse> call,
                                           Response<ForgotPasswordResponse> response) {
                        forgotLoading.setValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            forgotSuccess.setValue(response.body().getMessage() != null
                                    ? response.body().getMessage()
                                    : "Check your email for a reset link");
                        } else {
                            forgotError.setValue(parseErrorMessage(response));
                        }
                    }

                    @Override
                    public void onFailure(Call<ForgotPasswordResponse> call, Throwable t) {
                        forgotLoading.setValue(false);
                        forgotError.setValue("Network error: " + t.getMessage());
                    }
                });
    }

    // ----- Helpers -----

    private String parseErrorMessage(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String errStr = response.errorBody().string();
                JSONObject json = new JSONObject(errStr);
                if (json.has("message")) return json.getString("message");
                if (json.has("error"))   return json.getString("error");
            }
        } catch (Exception ignored) {}
        return "Request failed (code " + response.code() + ")";
    }

    // ----- Inner types -----

    public static class LoginResult {
        public final String token;
        public LoginResult(String token) { this.token = token; }
    }

    public interface OnDriverInfoFetchedCallback {
        void onSuccess(DriverHomeResponse response); // null = skip caching
    }
}