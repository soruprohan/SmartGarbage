package com.example.smartgarbage.ui.bins;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
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

import com.example.smartgarbage.R;
import com.example.smartgarbage.data.model.Bin;
import com.example.smartgarbage.data.repository.BinRepository;
import com.example.smartgarbage.ui.auth.LoginActivity;
import com.example.smartgarbage.ui.home.HomeActivity;
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

public class BinListActivity extends AppCompatActivity {

    // ── Request codes ──────────────────────────────────────────────────────────
    private static final int REQUEST_CAMERA_PERMISSION = 201;
    private static final int REQUEST_COLLECT_PHOTO     = 101;

    // ── Existing fields ────────────────────────────────────────────────────────
    private BinListViewModel viewModel;
    private TokenManager tokenManager;

    private RecyclerView recyclerView;
    private BinAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private LinearLayout layoutError;
    private TextView tvErrorMessage;
    private LinearLayout layoutEmpty;

    // ── New fields for collect feature ─────────────────────────────────────────
    private FusedLocationProviderClient fusedLocationClient;
    private int pendingCollectBinId = -1; // which bin is being collected
    private Uri photoUri;                 // where the camera saves the photo
    private BinRepository binRepository;  // for the collect API call

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bin_list);

        tokenManager   = new TokenManager(this);
        viewModel      = new ViewModelProvider(this).get(BinListViewModel.class);
        binRepository  = new BinRepository(tokenManager);

        bindViews();
        setupRecyclerView();
        setupBottomNav();
        observeViewModel();
        setupLocationClient();

        viewModel.loadAssignedBins();
    }

    // ── Set up location client and fetch driver's current position ─────────────
    private void setupLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    adapter.setDriverLocation(location);
                }
            });
        }
        // Note: location permission is already requested by MapActivity when
        // the user first opens the app. We just read the cached last location here.
    }

    // ── Called from adapter when driver taps "Mark as Collected" ────────────────
    private void onCollectClicked(int binId) {
        pendingCollectBinId = binId;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Ask for camera permission first
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                REQUEST_CAMERA_PERMISSION);
        } else {
            launchCamera();
        }
    }

    // ── Open camera, writing full-res photo to a temp file ─────────────────────
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
                // Read the full-resolution photo from the URI (NOT data.getExtras())
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
        // Show loading dialog
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setMessage("Submitting collection proof...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        // Convert bitmap to JPEG bytes (80% quality is enough for proof)
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        photo.compress(Bitmap.CompressFormat.JPEG, 80, stream);
        byte[] byteArray = stream.toByteArray();

        // Build multipart photo part — field name must be "photo" (matches backend)
        RequestBody photoBody = RequestBody.create(
                MediaType.parse("image/jpeg"), byteArray);
        MultipartBody.Part photoPart = MultipartBody.Part.createFormData(
                "photo", "collect_" + binId + ".jpg", photoBody);

        // Call repository
        binRepository.markBinCollected(binId, photoPart)
                .observe(this, resource -> {
                    if (resource == null) return;

                    switch (resource.status) {
                        case LOADING:
                            break; // dialog is already showing

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

    // ── Everything below is unchanged from the original ───────────────────────

    private void bindViews() {
        recyclerView   = findViewById(R.id.rvBins);
        swipeRefresh   = findViewById(R.id.swipeRefresh);
        progressBar    = findViewById(R.id.progressBar);
        layoutError    = findViewById(R.id.layoutError);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        layoutEmpty    = findViewById(R.id.layoutEmpty);

        findViewById(R.id.btnRetry).setOnClickListener(v -> viewModel.loadAssignedBins());
    }

    private void setupRecyclerView() {
        adapter = new BinAdapter(bin -> showBinDetail(bin));

        // Wire up the collect button callback
        adapter.setCollectListener(binId -> onCollectClicked(binId));

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        swipeRefresh.setColorSchemeResources(R.color.primary_green);
        swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.bg_card);
        swipeRefresh.setOnRefreshListener(() -> viewModel.loadAssignedBins());
    }

    private void showBinDetail(Bin bin) {
        String info = bin.getName() + " — " + bin.getFillPercentage() + "% full";
        Toast.makeText(this, info, Toast.LENGTH_SHORT).show();
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_bins);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_bins) {
                return true;
            } else if (id == R.id.nav_home) {
                Intent intent = new Intent(this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_map) {
                startActivity(new Intent(this,
                        com.example.smartgarbage.ui.map.MapActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_messages) {
                startActivity(new Intent(this,
                        com.example.smartgarbage.ui.messages.MessagesActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this,
                        com.example.smartgarbage.ui.profile.ProfileActivity.class));
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
                    progressBar.setVisibility(View.VISIBLE);
                    layoutError.setVisibility(View.GONE);
                    layoutEmpty.setVisibility(View.GONE);
                    break;

                case SUCCESS:
                    swipeRefresh.setRefreshing(false);
                    progressBar.setVisibility(View.GONE);
                    List<Bin> bins = resource.data;
                    if (bins == null || bins.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        adapter.setBins(null);
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        adapter.setBins(bins);
                    }
                    break;

                case ERROR:
                    swipeRefresh.setRefreshing(false);
                    progressBar.setVisibility(View.GONE);
                    if (resource.message != null
                            && resource.message.contains("Session expired")) {
                        tokenManager.clearAll();
                        Intent intent = new Intent(this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        layoutError.setVisibility(View.VISIBLE);
                        tvErrorMessage.setText(resource.message != null
                                ? resource.message : "Something went wrong.");
                    }
                    break;
            }
        });
    }
}
