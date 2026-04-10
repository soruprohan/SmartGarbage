package com.example.smartgarbage.ui.profile;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.smartgarbage.R;
import com.example.smartgarbage.data.model.DriverProfile;
import com.example.smartgarbage.ui.auth.LoginActivity;
import com.example.smartgarbage.ui.bins.BinListActivity;
import com.example.smartgarbage.ui.home.HomeActivity;
import com.example.smartgarbage.ui.messages.MessagesActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ProfileActivity extends AppCompatActivity {

    private ProfileViewModel viewModel;

    // Header views
    private ImageView ivAvatar;
    private ImageView ivEditPhoto;
    private TextView tvName;
    private TextView tvEmail;
    private TextView tvStatus;
    private TextView tvBinCount;
    private TextView tvDriverId;

    // Edit profile views
    private EditText etName;
    private EditText etEmail;
    private EditText etPhone;
    private Button btnSaveProfile;
    private ProgressBar progressSaveProfile;

    // Change password views
    private EditText etNewPassword;
    private EditText etConfirmPassword;
    private Button btnChangePassword;
    private ProgressBar progressChangePassword;

    // Logout
    private Button btnLogout;

    // Holds the file selected from gallery (null if no new photo chosen)
    private File pendingPhotoFile = null;

    // Activity result launcher for gallery photo pick
    private final ActivityResultLauncher<Intent> photoPickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Uri uri = result.getData().getData();
                            if (uri != null) {
                                handlePhotoSelected(uri);
                            }
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        bindViews();
        setupBottomNav();
        setupClickListeners();
        observeViewModel();

        // Show cached values immediately while API loads
        tvName.setText(viewModel.getCachedName());
        tvEmail.setText(viewModel.getCachedEmail());
        tvDriverId.setText(String.valueOf(viewModel.getDriverId()));
        etName.setText(viewModel.getCachedName());
        etEmail.setText(viewModel.getCachedEmail());

        // Fetch full profile from API
        viewModel.loadProfile();
    }

    // ── View binding ──────────────────────────────────────────────────────────

    private void bindViews() {
        ivAvatar              = findViewById(R.id.ivAvatar);
        ivEditPhoto           = findViewById(R.id.ivEditPhoto);
        tvName                = findViewById(R.id.tvName);
        tvEmail               = findViewById(R.id.tvEmail);
        tvStatus              = findViewById(R.id.tvStatus);
        tvBinCount            = findViewById(R.id.tvBinCount);
        tvDriverId            = findViewById(R.id.tvDriverId);
        etName                = findViewById(R.id.etName);
        etEmail               = findViewById(R.id.etEmail);
        etPhone               = findViewById(R.id.etPhone);
        btnSaveProfile        = findViewById(R.id.btnSaveProfile);
        progressSaveProfile   = findViewById(R.id.progressSaveProfile);
        etNewPassword         = findViewById(R.id.etNewPassword);
        etConfirmPassword     = findViewById(R.id.etConfirmPassword);
        btnChangePassword     = findViewById(R.id.btnChangePassword);
        progressChangePassword = findViewById(R.id.progressChangePassword);
        btnLogout             = findViewById(R.id.btnLogout);
    }

    // ── Click listeners ───────────────────────────────────────────────────────

    private void setupClickListeners() {

        // Open gallery to pick a profile photo
        ivEditPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            photoPickerLauncher.launch(intent);
        });

        // Save profile (name + phone, ± photo)
        btnSaveProfile.setOnClickListener(v -> {
            String name  = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();

            if (name.isEmpty()) {
                etName.setError("Name is required");
                return;
            }

            if (pendingPhotoFile != null) {
                viewModel.saveProfileWithPhoto(name, phone, pendingPhotoFile);
            } else {
                viewModel.saveProfile(name, phone);
            }
        });

        // Change password
        btnChangePassword.setOnClickListener(v -> {
            String newPass     = etNewPassword.getText().toString();
            String confirmPass = etConfirmPassword.getText().toString();

            if (newPass.isEmpty()) {
                etNewPassword.setError("Enter new password");
                return;
            }
            if (newPass.length() < 6) {
                etNewPassword.setError("Password must be at least 6 characters");
                return;
            }
            if (!newPass.equals(confirmPass)) {
                etConfirmPassword.setError("Passwords do not match");
                return;
            }

            viewModel.submitPasswordChange(newPass);
        });

        // Logout with confirmation dialog
        btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Logout", (dialog, which) -> performLogout())
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    // ── ViewModel observers ───────────────────────────────────────────────────

    private void observeViewModel() {

        // Profile data
        viewModel.getProfileData().observe(this, resource -> {
            if (resource == null) return;
            switch (resource.status) {
                case LOADING:
                    // Cached values already showing — no full-screen loader needed
                    break;
                case SUCCESS:
                    if (resource.data != null) populateProfile(resource.data);
                    break;
                case ERROR:
                    Snackbar.make(findViewById(android.R.id.content),
                            "Could not load profile: " + resource.message,
                            Snackbar.LENGTH_LONG).show();
                    break;
            }
        });

        // Update profile result
        viewModel.getUpdateResult().observe(this, resource -> {
            if (resource == null) return;
            switch (resource.status) {
                case LOADING:
                    progressSaveProfile.setVisibility(View.VISIBLE);
                    btnSaveProfile.setEnabled(false);
                    break;
                case SUCCESS:
                    progressSaveProfile.setVisibility(View.GONE);
                    btnSaveProfile.setEnabled(true);
                    pendingPhotoFile = null;
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    // Reload to reflect latest data
                    viewModel.loadProfile();
                    break;
                case ERROR:
                    progressSaveProfile.setVisibility(View.GONE);
                    btnSaveProfile.setEnabled(true);
                    Toast.makeText(this, "Update failed: " + resource.message, Toast.LENGTH_LONG).show();
                    break;
            }
        });

        // Change password result
        viewModel.getPasswordResult().observe(this, resource -> {
            if (resource == null) return;
            switch (resource.status) {
                case LOADING:
                    progressChangePassword.setVisibility(View.VISIBLE);
                    btnChangePassword.setEnabled(false);
                    break;
                case SUCCESS:
                    progressChangePassword.setVisibility(View.GONE);
                    btnChangePassword.setEnabled(true);
                    etNewPassword.setText("");
                    etConfirmPassword.setText("");
                    Toast.makeText(this, "Password changed successfully!", Toast.LENGTH_SHORT).show();
                    break;
                case ERROR:
                    progressChangePassword.setVisibility(View.GONE);
                    btnChangePassword.setEnabled(true);
                    Toast.makeText(this, "Failed: " + resource.message, Toast.LENGTH_LONG).show();
                    break;
            }
        });
    }

    // ── Profile population ────────────────────────────────────────────────────

    private void populateProfile(DriverProfile profile) {
        tvName.setText(profile.getName());
        tvEmail.setText(profile.getEmail());
        tvDriverId.setText(String.valueOf(profile.getId()));
        tvBinCount.setText(String.valueOf(profile.getAssignedBinCount()));

        // Status badge: keep text white and tint the background by status.
        String status = profile.getStatus();
        if (status != null) {
            tvStatus.setText(status);
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.white));
            int badgeColor = ContextCompat.getColor(
                    this,
                    "active".equalsIgnoreCase(status) ? R.color.success_green : R.color.warning_orange
            );
            tvStatus.setBackgroundTintList(ColorStateList.valueOf(badgeColor));
        }

        // Editable fields
        etName.setText(profile.getName());
        etEmail.setText(profile.getEmail());
        if (profile.getPhone() != null) etPhone.setText(profile.getPhone());

        // Load avatar from Cloudinary URL using Glide
        if (profile.getPhotoUrl() != null && !profile.getPhotoUrl().isEmpty()) {
            ivAvatar.setPadding(0, 0, 0, 0);
            Glide.with(this)
                    .load(profile.getPhotoUrl())
                    .placeholder(R.drawable.ic_nav_profile)
                    .error(R.drawable.ic_nav_profile)
                    .circleCrop()
                    .into(ivAvatar);
        } else {
            // Keep a bit of inset only for the default icon state.
            int iconInset = (int) (20 * getResources().getDisplayMetrics().density);
            ivAvatar.setPadding(iconInset, iconInset, iconInset, iconInset);
            ivAvatar.setImageResource(R.drawable.ic_nav_profile);
        }
    }

    // ── Photo selection ───────────────────────────────────────────────────────

    private void handlePhotoSelected(Uri uri) {
        try {
            // Copy URI to a temp File so Retrofit can send it
            InputStream inputStream = getContentResolver().openInputStream(uri);
            File tempFile = File.createTempFile("avatar_", ".jpg", getCacheDir());
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            inputStream.close();
            outputStream.close();

            pendingPhotoFile = tempFile;

            // Preview selected photo edge-to-edge inside the avatar circle.
            ivAvatar.setPadding(0, 0, 0, 0);
            Glide.with(this)
                    .load(uri)
                    .circleCrop()
                    .into(ivAvatar);

            Toast.makeText(this, "Photo selected. Tap 'Save Changes' to upload.", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Toast.makeText(this, "Failed to load photo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    private void performLogout() {
        viewModel.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ── Bottom navigation ─────────────────────────────────────────────────────

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_profile);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) {
                return true;
            } else if (id == R.id.nav_home) {
                Intent intent = new Intent(this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_bins) {
                startActivity(new Intent(this, BinListActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_map) {
                startActivity(new Intent(this, com.example.smartgarbage.ui.map.MapActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_messages) {
                startActivity(new Intent(this, MessagesActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }
}
