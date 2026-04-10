package com.example.smartgarbage.ui.auth;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.smartgarbage.R;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private TextView tvEmailError, tvStatusMessage, tvBackToLogin;
    private Button btnSendReset;
    private ProgressBar progressBar;
    private LoginViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        etEmail        = findViewById(R.id.etEmail);
        tvEmailError   = findViewById(R.id.tvEmailError);
        tvStatusMessage = findViewById(R.id.tvStatusMessage);
        btnSendReset   = findViewById(R.id.btnSendReset);
        progressBar    = findViewById(R.id.progressBar);
        tvBackToLogin  = findViewById(R.id.tvBackToLogin);

        btnSendReset.setOnClickListener(v -> attemptSendReset());
        tvBackToLogin.setOnClickListener(v -> finish());

        observeViewModel();
    }

    private void attemptSendReset() {
        String email = etEmail.getText().toString().trim();
        tvEmailError.setVisibility(View.GONE);
        tvStatusMessage.setVisibility(View.GONE);

        if (email.isEmpty()) {
            tvEmailError.setText(getString(R.string.error_empty_email));
            tvEmailError.setVisibility(View.VISIBLE);
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tvEmailError.setText(getString(R.string.error_invalid_email));
            tvEmailError.setVisibility(View.VISIBLE);
            return;
        }

        viewModel.sendForgotPassword(email);
    }

    private void observeViewModel() {
        viewModel.getForgotLoading().observe(this, isLoading -> {
            btnSendReset.setEnabled(!isLoading);
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.getForgotSuccess().observe(this, msg -> {
            if (msg == null) return;
            tvStatusMessage.setText(msg);
            tvStatusMessage.setTextColor(Color.parseColor("#388E3C"));
            tvStatusMessage.setBackgroundColor(Color.parseColor("#1A388E3C"));
            tvStatusMessage.setVisibility(View.VISIBLE);
            btnSendReset.setEnabled(false); // Prevent re-send spam
        });

        viewModel.getForgotError().observe(this, error -> {
            if (error == null) return;
            tvStatusMessage.setText(error);
            tvStatusMessage.setTextColor(Color.parseColor("#D32F2F"));
            tvStatusMessage.setBackgroundColor(Color.parseColor("#1AD32F2F"));
            tvStatusMessage.setVisibility(View.VISIBLE);
        });
    }
}