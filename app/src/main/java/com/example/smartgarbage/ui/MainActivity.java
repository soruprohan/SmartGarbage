package com.example.smartgarbage.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartgarbage.R;
import com.example.smartgarbage.ui.auth.LoginActivity;
import com.example.smartgarbage.utils.TokenManager;

public class MainActivity extends AppCompatActivity {

    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tokenManager = new TokenManager(this);

        // --- Populate driver info ---
        TextView tvName  = findViewById(R.id.tvDriverName);
        TextView tvEmail = findViewById(R.id.tvDriverEmail);
        TextView tvId    = findViewById(R.id.tvDriverId);

        String name  = tokenManager.getDriverName();
        String email = tokenManager.getDriverEmail();
        int    id    = tokenManager.getDriverId();

        tvName.setText("Name:  " + (name  != null ? name  : "NOT SAVED"));
        tvEmail.setText("Email: " + (email != null ? email : "NOT SAVED"));
        tvId.setText("ID:    " + (id != -1 ? String.valueOf(id) : "NOT SAVED"));

        // --- Token status ---
        TextView tvTokenStatus  = findViewById(R.id.tvTokenStatus);
        TextView tvTokenPreview = findViewById(R.id.tvTokenPreview);

        String token = tokenManager.getToken();
        if (token != null) {
            tvTokenStatus.setText("✅ Token present (" + token.length() + " chars)");
            // Show first 40 chars + "..."
            String preview = token.length() > 40
                    ? token.substring(0, 40) + "…"
                    : token;
            tvTokenPreview.setText(preview);
        } else {
            tvTokenStatus.setText("❌ No token found!");
            tvTokenPreview.setText("");
        }

        // --- Logout test ---
        Button btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            tokenManager.clearAll();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}