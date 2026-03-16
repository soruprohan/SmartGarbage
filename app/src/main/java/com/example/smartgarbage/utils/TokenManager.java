package com.example.smartgarbage.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class TokenManager {

    private SharedPreferences prefs;

    public TokenManager(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            prefs = EncryptedSharedPreferences.create(
                    context,
                    Constants.PREF_FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    public void saveToken(String token) {
        prefs.edit().putString(Constants.KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return prefs.getString(Constants.KEY_TOKEN, null);
    }

    public void clearToken() {
        prefs.edit().remove(Constants.KEY_TOKEN).apply();
    }

    public void saveDriverInfo(int id, String name, String email) {
        prefs.edit()
                .putInt(Constants.KEY_DRIVER_ID, id)
                .putString(Constants.KEY_DRIVER_NAME, name)
                .putString(Constants.KEY_DRIVER_EMAIL, email)
                .apply();
    }

    public int getDriverId() {
        return prefs.getInt(Constants.KEY_DRIVER_ID, -1);
    }

    public String getDriverName() {
        return prefs.getString(Constants.KEY_DRIVER_NAME, null);
    }

    public String getDriverEmail() {
        return prefs.getString(Constants.KEY_DRIVER_EMAIL, null);
    }

    public void clearAll() {
        prefs.edit().clear().apply();
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }
}