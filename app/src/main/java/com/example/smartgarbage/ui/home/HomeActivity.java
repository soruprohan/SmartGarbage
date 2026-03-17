package com.example.smartgarbage.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.smartgarbage.R;
import com.example.smartgarbage.data.model.Bin;
import com.example.smartgarbage.ui.auth.LoginActivity;
import com.example.smartgarbage.ui.bins.BinAdapter;
import com.example.smartgarbage.ui.bins.BinListActivity;
import com.example.smartgarbage.utils.TokenManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

public class HomeActivity extends AppCompatActivity {

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

    private BinAdapter criticalAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        tokenManager = new TokenManager(this);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        bindViews();
        setupRecyclerView();
        setupBottomNav();
        setupClickListeners();
        observeViewModel();

        // Welcome message
        tvWelcome.setText("Welcome back,\n" + viewModel.getDriverName() + " 👋");

        viewModel.loadAssignedBins();
    }

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
    }

    private void setupRecyclerView() {
        criticalAdapter = new BinAdapter(bin -> {
            // Navigate to bin list (can later open bin detail)
            startActivity(new Intent(this, BinListActivity.class));
        });
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
                Toast.makeText(this, "Messages coming in Phase 5", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_profile) {
                Toast.makeText(this, "Profile coming in Phase 6", Toast.LENGTH_SHORT).show();
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

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning from BinListActivity
        viewModel.loadAssignedBins();
    }
}
