package com.example.smartgarbage.ui.bins;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
import com.example.smartgarbage.ui.home.HomeActivity;
import com.example.smartgarbage.utils.TokenManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

public class BinListActivity extends AppCompatActivity {

    private BinListViewModel viewModel;
    private TokenManager tokenManager;

    private RecyclerView recyclerView;
    private BinAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private LinearLayout layoutError;
    private TextView tvErrorMessage;
    private LinearLayout layoutEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bin_list);

        tokenManager = new TokenManager(this);
        viewModel = new ViewModelProvider(this).get(BinListViewModel.class);

        bindViews();
        setupRecyclerView();
        setupBottomNav();
        observeViewModel();

        viewModel.loadAssignedBins();
    }

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
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        swipeRefresh.setColorSchemeResources(R.color.primary_green);
        swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.bg_card);
        swipeRefresh.setOnRefreshListener(() -> viewModel.loadAssignedBins());
    }

    private void showBinDetail(Bin bin) {
        // Phase 4 will open map/detail — show a toast for now
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
                startActivity(new Intent(this, com.example.smartgarbage.ui.map.MapActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_messages) {
                startActivity(new Intent(this, com.example.smartgarbage.ui.messages.MessagesActivity.class));
                overridePendingTransition(0, 0);
                finish();
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
                    if (resource.message != null && resource.message.contains("Session expired")) {
                        tokenManager.clearAll();
                        Intent intent = new Intent(this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
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
