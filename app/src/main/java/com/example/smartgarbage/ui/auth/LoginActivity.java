package com.example.smartgarbage.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.smartgarbage.R;
import com.example.smartgarbage.data.model.DriverHomeResponse;
import com.example.smartgarbage.ui.MainActivity;
import com.example.smartgarbage.utils.TokenManager;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private TextView tvEmailError, tvPasswordError, tvLoginError, tvForgotPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private ImageView ivTogglePassword;

    private LoginViewModel viewModel;
    private TokenManager tokenManager;
    private boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        tokenManager = new TokenManager(this);
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        bindViews();
        setupListeners();
        observeViewModel();
    }

    private void bindViews() {
        etEmail         = findViewById(R.id.etEmail);
        etPassword      = findViewById(R.id.etPassword);
        tvEmailError    = findViewById(R.id.tvEmailError);
        tvPasswordError = findViewById(R.id.tvPasswordError);
        tvLoginError    = findViewById(R.id.tvLoginError);
        btnLogin        = findViewById(R.id.btnLogin);
        progressBar     = findViewById(R.id.progressBar);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());

        // Allow "Done" on keyboard to trigger login
        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin();
                return true;
            }
            return false;
        });

        // Toggle password visibility
        ivTogglePassword.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            if (passwordVisible) {
                etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                ivTogglePassword.setImageResource(android.R.drawable.ic_secure);
            } else {
                etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                ivTogglePassword.setImageResource(android.R.drawable.ic_menu_view);
            }
            // Move cursor to end
            etPassword.setSelection(etPassword.getText().length());
        });

        // Clear field errors on focus
        etEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) clearFieldError(tvEmailError);
        });
        etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) clearFieldError(tvPasswordError);
        });

        tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });
    }

    private void observeViewModel() {
        viewModel.getLoginLoading().observe(this, isLoading -> {
            btnLogin.setEnabled(!isLoading);
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            btnLogin.setText(isLoading ? getString(R.string.logging_in) : getString(R.string.btn_login));
        });

        viewModel.getLoginError().observe(this, error -> {
            if (error != null) {
                tvLoginError.setText(error);
                tvLoginError.setVisibility(View.VISIBLE);
            } else {
                tvLoginError.setVisibility(View.GONE);
            }
        });

        viewModel.getLoginSuccess().observe(this, result -> {
            if (result == null) return;
            // Save the token first
            tokenManager.saveToken(result.token);
            // Then fetch and cache driver info, then navigate
            viewModel.fetchDriverInfo(result.token, driverHomeResponse -> {
                if (driverHomeResponse != null && driverHomeResponse.getDriver() != null) {
                    DriverHomeResponse.Driver d = driverHomeResponse.getDriver();
                    tokenManager.saveDriverInfo(d.getId(), d.getName(), d.getEmail());
                }
                navigateToMain();
            });
        });
    }

    private void attemptLogin() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        boolean valid = true;

        if (email.isEmpty()) {
            showFieldError(tvEmailError, getString(R.string.error_empty_email));
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showFieldError(tvEmailError, getString(R.string.error_invalid_email));
            valid = false;
        }

        if (password.isEmpty()) {
            showFieldError(tvPasswordError, getString(R.string.error_empty_password));
            valid = false;
        }

        if (!valid) return;

        tvLoginError.setVisibility(View.GONE);
        viewModel.login(email, password);
    }

    private void showFieldError(TextView errorView, String message) {
        errorView.setText(message);
        errorView.setVisibility(View.VISIBLE);
    }

    private void clearFieldError(TextView errorView) {
        errorView.setVisibility(View.GONE);
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}