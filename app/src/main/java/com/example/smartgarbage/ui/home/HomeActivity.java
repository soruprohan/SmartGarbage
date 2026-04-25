package com.example.smartgarbage.ui.home;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.smartgarbage.R;
import com.example.smartgarbage.data.api.ApiService;
import com.example.smartgarbage.data.api.RetrofitClient;
import com.example.smartgarbage.data.model.Bin;
import com.example.smartgarbage.data.model.DriverProfile;
import com.example.smartgarbage.data.repository.BinRepository;
import com.example.smartgarbage.ui.auth.LoginActivity;
import com.example.smartgarbage.ui.bins.BinAdapter;
import com.example.smartgarbage.ui.bins.BinListActivity;
import com.example.smartgarbage.ui.profile.ProfileActivity;
import com.example.smartgarbage.utils.TokenManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    // ── Request codes for collect feature ──────────────────────────────────────
    private static final int REQUEST_CAMERA_PERMISSION = 201;
    private static final int REQUEST_COLLECT_PHOTO     = 101;

    private HomeViewModel viewModel;
    private TokenManager tokenManager;

    // Views
    private TextView tvWelcome;
    private TextView tvTotalBins;
    private TextView tvCriticalBins;
    private TextView tvOkBins;
    private ProgressBar progressBar;
    private LinearLayout layoutError;
    private TextView tvErrorMessage;
    private Button btnRetry;
    private LinearLayout layoutEmpty;
    private RecyclerView rvCriticalBins;
    private SwipeRefreshLayout swipeRefresh;
    private Button btnStartRoute;
    private TextView btnViewAll;
    private View btnHomeProfileAvatar;
    private ImageView ivHomeAvatar;

    private BinAdapter criticalAdapter;

    // ── Fields for collect feature ─────────────────────────────────────────────
    private FusedLocationProviderClient fusedLocationClient;
    private int pendingCollectBinId = -1;
    private Uri photoUri;
    private BinRepository binRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        tokenManager = new TokenManager(this);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        binRepository = new BinRepository(tokenManager);

        bindViews();
        setupRecyclerView();
        setupBottomNav();
        setupClickListeners();
        observeViewModel();
        setupLocationClient();

        // Welcome message
        tvWelcome.setText("Welcome back,\n" + viewModel.getDriverName());

        viewModel.loadAssignedBins();
    }

    // ── Location client for GPS proximity check ───────────────────────────────
    private void setupLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    criticalAdapter.setDriverLocation(location);
                }
            });
        }
    }

    // ── Called from adapter when driver taps "Mark as Collected" ────────────────
    private void onCollectClicked(int binId) {
        pendingCollectBinId = binId;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                REQUEST_CAMERA_PERMISSION);
        } else {
            launchCamera();
        }
    }

    // ── Open camera ───────────────────────────────────────────────────────────
    private void launchCamera() {
        try {
            File photoFile = new File(getCacheDir(),
                    "collect_" + pendingCollectBinId + ".jpg");
            photoUri = FileProvider.getUriForFile(
                    this,
                    "com.example.smartgarbage.provider",
                    photoFile);

            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            startActivityForResult(cameraIntent, REQUEST_COLLECT_PHOTO);

        } catch (Exception e) {
            Toast.makeText(this, "Could not open camera: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ── Handle camera permission result ────────────────────────────────────────
    @Override
    public void onRequestPermissionsResult(int requestCode,
            String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                Toast.makeText(this,
                    "Camera permission is required to submit collection proof.",
                    Toast.LENGTH_LONG).show();
            }
        }
    }

    // ── Handle camera result and upload ────────────────────────────────────────
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_COLLECT_PHOTO && resultCode == RESULT_OK) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(photoUri);
                Bitmap photo = BitmapFactory.decodeStream(inputStream);
                uploadAndMarkCollected(pendingCollectBinId, photo);
            } catch (Exception e) {
                Toast.makeText(this, "Failed to read photo: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ── Convert bitmap → bytes → multipart → call API ─────────────────────────
    private void uploadAndMarkCollected(int binId, Bitmap photo) {
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setMessage("Submitting collection proof...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        photo.compress(Bitmap.CompressFormat.JPEG, 80, stream);
        byte[] byteArray = stream.toByteArray();

        RequestBody photoBody = RequestBody.create(
                MediaType.parse("image/jpeg"), byteArray);
        MultipartBody.Part photoPart = MultipartBody.Part.createFormData(
                "photo", "collect_" + binId + ".jpg", photoBody);

        binRepository.markBinCollected(binId, photoPart)
                .observe(this, resource -> {
                    if (resource == null) return;

                    switch (resource.status) {
                        case LOADING:
                            break;

                        case SUCCESS:
                            progressDialog.dismiss();
                            Toast.makeText(this,
                                "✓ Bin marked as collected!",
                                Toast.LENGTH_SHORT).show();
                            viewModel.loadAssignedBins(); // refresh the list
                            break;

                        case ERROR:
                            progressDialog.dismiss();
                            if (resource.message != null
                                    && resource.message.contains("Session expired")) {
                                tokenManager.clearAll();
                                Intent intent = new Intent(this, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(this,
                                    resource.message != null
                                        ? resource.message
                                        : "Failed to submit collection.",
                                    Toast.LENGTH_LONG).show();
                            }
                            break;
                    }
                });
    }

    // ── Everything below is unchanged ─────────────────────────────────────────

    private void bindViews() {
        tvWelcome      = findViewById(R.id.tvWelcome);
        tvTotalBins    = findViewById(R.id.tvTotalBins);
        tvCriticalBins = findViewById(R.id.tvCriticalBins);
        tvOkBins       = findViewById(R.id.tvOkBins);
        progressBar    = findViewById(R.id.progressBar);
        layoutError    = findViewById(R.id.layoutError);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        btnRetry       = findViewById(R.id.btnRetry);
        layoutEmpty    = findViewById(R.id.layoutEmpty);
        rvCriticalBins = findViewById(R.id.rvCriticalBins);
        swipeRefresh   = findViewById(R.id.swipeRefresh);
        btnStartRoute  = findViewById(R.id.btnStartRoute);
        btnViewAll     = findViewById(R.id.btnViewAllBins);
        btnHomeProfileAvatar = findViewById(R.id.btnHomeProfileAvatar);
        ivHomeAvatar   = findViewById(R.id.ivHomeAvatar);
    }

    private void setupRecyclerView() {
        criticalAdapter = new BinAdapter(bin -> {
            // Navigate to bin list (can later open bin detail)
            startActivity(new Intent(this, BinListActivity.class));
        });

        // Wire up the collect button callback
        criticalAdapter.setCollectListener(binId -> onCollectClicked(binId));

        rvCriticalBins.setLayoutManager(new LinearLayoutManager(this));
        rvCriticalBins.setAdapter(criticalAdapter);
        rvCriticalBins.setNestedScrollingEnabled(false);
    }

    private void setupClickListeners() {
        btnRetry.setOnClickListener(v -> viewModel.loadAssignedBins());

        swipeRefresh.setColorSchemeResources(R.color.primary_green);
        swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.bg_card);
        swipeRefresh.setOnRefreshListener(() -> viewModel.loadAssignedBins());

        btnViewAll.setOnClickListener(v ->
                startActivity(new Intent(this, BinListActivity.class)));

        btnHomeProfileAvatar.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            overridePendingTransition(0, 0);
        });

        // ── Phase 4: Start Route opens MapActivity ──
        btnStartRoute.setOnClickListener(v ->
                startActivity(new Intent(this, com.example.smartgarbage.ui.map.MapActivity.class)));
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_bins) {
                startActivity(new Intent(this, BinListActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_map) {
                // ── Phase 4: Map tab opens MapActivity ──
                startActivity(new Intent(this, com.example.smartgarbage.ui.map.MapActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_messages) {
                startActivity(new Intent(this, com.example.smartgarbage.ui.messages.MessagesActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, com.example.smartgarbage.ui.profile.ProfileActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

    private void observeViewModel() {
        viewModel.getBins().observe(this, resource -> {
            if (resource == null) return;

            switch (resource.status) {
                case LOADING:
                    showLoading();
                    break;

                case SUCCESS:
                    swipeRefresh.setRefreshing(false);
                    List<Bin> bins = resource.data;
                    if (bins == null || bins.isEmpty()) {
                        showEmptyState();
                    } else {
                        showContent(bins);
                    }
                    break;

                case ERROR:
                    swipeRefresh.setRefreshing(false);
                    // If 401, token expired → back to login
                    if (resource.message != null && resource.message.contains("Session expired")) {
                        tokenManager.clearAll();
                        Intent intent = new Intent(this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        showError(resource.message);
                    }
                    break;
            }
        });
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        layoutError.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        layoutError.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
        tvErrorMessage.setText(message != null ? message : "Something went wrong. Please try again.");
    }

    private void showEmptyState() {
        progressBar.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);

        // Zero out the stat cards
        tvTotalBins.setText("0");
        tvCriticalBins.setText("0");
        tvOkBins.setText("0");
        criticalAdapter.setBins(null);
    }

    private void showContent(List<Bin> bins) {
        progressBar.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);

        // Update stat cards
        tvTotalBins.setText(String.valueOf(viewModel.getTotalCount(bins)));
        tvCriticalBins.setText(String.valueOf(viewModel.getCriticalCount(bins)));
        tvOkBins.setText(String.valueOf(viewModel.getOkCount(bins)));

        // Show top critical bins (highest fill first, max 5 preview items)
        List<Bin> sorted = viewModel.getSortedByFillDescending(bins);
        List<Bin> preview = sorted.size() > 5 ? sorted.subList(0, 5) : sorted;
        criticalAdapter.setBins(preview);
    }

    private void loadHomeAvatarFromCache() {
        String photoUrl = tokenManager.getDriverPhotoUrl();
        if (photoUrl != null && !photoUrl.trim().isEmpty()) {
            ivHomeAvatar.setPadding(0, 0, 0, 0);
            Glide.with(this)
                    .load(photoUrl)
                    .circleCrop()
                    .error(R.drawable.ic_nav_profile)
                    .into(ivHomeAvatar);
        } else {
            applyHomeAvatarFallback();
        }
    }

    private void refreshHomeAvatarFromApi() {
        int driverId = tokenManager.getDriverId();
        if (driverId <= 0) return;

        ApiService api = RetrofitClient.getAuthService(tokenManager);
        api.getDriverById(driverId).enqueue(new Callback<DriverProfile>() {
            @Override
            public void onResponse(Call<DriverProfile> call, Response<DriverProfile> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String photoUrl = response.body().getPhotoUrl();
                    tokenManager.saveDriverPhotoUrl(photoUrl);
                    if (photoUrl != null && !photoUrl.trim().isEmpty()) {
                        ivHomeAvatar.setPadding(0, 0, 0, 0);
                        Glide.with(HomeActivity.this)
                                .load(photoUrl)
                                .circleCrop()
                                .error(R.drawable.ic_nav_profile)
                                .into(ivHomeAvatar);
                    } else {
                        applyHomeAvatarFallback();
                    }
                }
            }

            @Override
            public void onFailure(Call<DriverProfile> call, Throwable t) {
                // Keep cached avatar or fallback; no UI interruption needed.
            }
        });
    }

    private void applyHomeAvatarFallback() {
        int iconInset = (int) (8 * getResources().getDisplayMetrics().density);
        ivHomeAvatar.setPadding(iconInset, iconInset, iconInset, iconInset);
        ivHomeAvatar.setImageResource(R.drawable.ic_nav_profile);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning from BinListActivity
        viewModel.loadAssignedBins();
        loadHomeAvatarFromCache();
        refreshHomeAvatarFromApi();
    }
}
