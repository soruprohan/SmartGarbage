package com.example.smartgarbage;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartgarbage.ui.home.HomeActivity;
import com.example.smartgarbage.ui.auth.LoginActivity;
import com.example.smartgarbage.utils.TokenManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TokenManager tokenManager = new TokenManager(this);

        if (tokenManager.isLoggedIn()) {
            // Token exists → go straight to the dashboard
            startActivity(new Intent(this, HomeActivity.class));
        } else {
            startActivity(new Intent(this, LoginActivity.class));
        }
        finish();
    }
}
