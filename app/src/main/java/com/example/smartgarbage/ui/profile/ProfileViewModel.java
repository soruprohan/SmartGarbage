package com.example.smartgarbage.ui.profile;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.smartgarbage.data.model.DriverProfile;
import com.example.smartgarbage.data.model.UpdateDriverResponse;
import com.example.smartgarbage.data.repository.ProfileRepository;
import com.example.smartgarbage.utils.Resource;
import com.example.smartgarbage.utils.TokenManager;

import java.io.File;

public class ProfileViewModel extends AndroidViewModel {

    private final ProfileRepository repository;
    private final TokenManager tokenManager;

    // Observed by ProfileActivity
    private final MutableLiveData<Resource<DriverProfile>> profileData = new MutableLiveData<>();
    private final MutableLiveData<Resource<UpdateDriverResponse>> updateResult = new MutableLiveData<>();
    private final MutableLiveData<Resource<UpdateDriverResponse>> passwordResult = new MutableLiveData<>();

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        repository = new ProfileRepository(application);
        tokenManager = repository.getTokenManager();
    }

    // ── Getters for LiveData ──────────────────────────────────────────────────

    public LiveData<Resource<DriverProfile>> getProfileData()           { return profileData; }
    public LiveData<Resource<UpdateDriverResponse>> getUpdateResult()   { return updateResult; }
    public LiveData<Resource<UpdateDriverResponse>> getPasswordResult() { return passwordResult; }

    // ── TokenManager helpers (for initial display before API returns) ─────────

    public String getCachedName()  { return tokenManager.getDriverName(); }
    public String getCachedEmail() { return tokenManager.getDriverEmail(); }
    public int    getDriverId()    { return tokenManager.getDriverId(); }

    // ── Actions ───────────────────────────────────────────────────────────────

    public void loadProfile() {
        repository.fetchProfile().observeForever(resource -> profileData.postValue(resource));
    }

    public void saveProfile(String name, String phone) {
        repository.updateProfile(getDriverId(), name, phone)
                .observeForever(resource -> updateResult.postValue(resource));
    }

    public void saveProfileWithPhoto(String name, String phone, File photoFile) {
        repository.updateProfileWithPhoto(getDriverId(), name, phone, photoFile)
                .observeForever(resource -> updateResult.postValue(resource));
    }

    public void submitPasswordChange(String newPassword) {
        repository.changePassword(getDriverId(), newPassword)
                .observeForever(resource -> passwordResult.postValue(resource));
    }

    public void logout() {
        tokenManager.clearAll();
    }
}