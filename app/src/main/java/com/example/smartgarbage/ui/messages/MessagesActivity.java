package com.example.smartgarbage.ui.messages;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartgarbage.R;
import com.example.smartgarbage.data.model.AdminInfo;
import com.example.smartgarbage.data.model.Message;
import com.example.smartgarbage.ui.bins.BinListActivity;
import com.example.smartgarbage.ui.home.HomeActivity;
import com.example.smartgarbage.ui.map.MapActivity;
import com.example.smartgarbage.utils.Resource;
import com.example.smartgarbage.utils.SocketManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.util.List;

public class MessagesActivity extends AppCompatActivity {

    private MessagesViewModel viewModel;
    private MessageAdapter adapter;
    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend;
    private ProgressBar progressBar;
    private TextView tvAdminName;
    private TextView tvConnectionStatus;

    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        viewModel = new ViewModelProvider(this).get(MessagesViewModel.class);

        bindViews();
        setupRecyclerView();
        setupBottomNav();
        setupSendButton();
        observeViewModel();

        viewModel.loadAdminInfo();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Begin 500ms polling. If adminId isn't known yet, the ViewModel
        // will auto-start polling the moment setAdminIdAndStartPolling() is called.
        viewModel.startPolling();

        // Keep socket alive for sending (it's still useful for outgoing messages)
        String token = viewModel.getTokenManager().getToken();
        if (token != null) {
            SocketManager.getInstance().connect(token);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Pause polling while the screen is not visible.
        viewModel.stopPolling();
    }

    // ─── View binding ───

    private void bindViews() {
        rvMessages         = findViewById(R.id.rvMessages);
        etMessage          = findViewById(R.id.etMessage);
        btnSend            = findViewById(R.id.btnSend);
        progressBar        = findViewById(R.id.progressBarMessages);
        tvAdminName        = findViewById(R.id.tvAdminName);
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
    }

    // ─── RecyclerView ───

    private void setupRecyclerView() {
        adapter = new MessageAdapter(viewModel.getDriverId());
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);
    }

    private void scrollToBottom() {
        if (adapter.getItemCount() > 0) {
            rvMessages.smoothScrollToPosition(adapter.getItemCount() - 1);
        }
    }

    // ─── Bottom navigation ───

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_messages);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_messages) {
                return true;
            } else if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_bins) {
                startActivity(new Intent(this, BinListActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_map) {
                startActivity(new Intent(this, MapActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                Toast.makeText(this, "Profile coming in Phase 6", Toast.LENGTH_SHORT).show();
                return false;
            }
            return false;
        });
    }

    // ─── Send button ───

    private void setupSendButton() {
        btnSend.setOnClickListener(v -> {
            String content = etMessage.getText().toString().trim();
            if (content.isEmpty()) return;
            if (viewModel.getAdminId() < 0) {
                Toast.makeText(this, "Admin not found yet", Toast.LENGTH_SHORT).show();
                return;
            }

            etMessage.setText("");

            if (SocketManager.getInstance().isConnected()) {
                // Send via socket — backend saves it and the next poll will pick it up
                SocketManager.getInstance().sendMessage(viewModel.getAdminId(), content);
            } else {
                // Fallback: REST API
                viewModel.sendMessageViaRest(content);
            }
        });
    }

    // ─── ViewModel observers ───

    private void observeViewModel() {
        // 1. Admin info discovery
        viewModel.getAdminInfoLiveData().observe(this, resource -> {
            if (resource.status == Resource.Status.LOADING) {
                progressBar.setVisibility(View.VISIBLE);
            } else if (resource.status == Resource.Status.SUCCESS) {
                progressBar.setVisibility(View.GONE);

                int resolvedAdminId;
                if (resource.data != null) {
                    AdminInfo admin = resource.data;
                    resolvedAdminId = admin.getId();
                    tvAdminName.setText(admin.getName() != null ? admin.getName() : "Admin");
                } else {
                    resolvedAdminId = 1;
                    tvAdminName.setText("Admin");
                    Toast.makeText(this, "No prior conversations. Start a new message!", Toast.LENGTH_SHORT).show();
                }

                // This sets the admin ID AND kicks off polling (and joinRoom for socket)
                viewModel.setAdminIdAndStartPolling(resolvedAdminId);
                SocketManager.getInstance().joinRoom(viewModel.getDriverId(), resolvedAdminId);
                viewModel.loadMessages();

            } else if (resource.status == Resource.Status.ERROR) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Error: " + resource.message, Toast.LENGTH_SHORT).show();
            }
        });

        // 2. Messages list — updated by both initial load and every poll tick
        viewModel.getMessagesLiveData().observe(this, resource -> {
            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                adapter.setMessages(resource.data);
                scrollToBottom();
            }
        });

        // 3. REST send fallback result
        viewModel.getSendMessageResult().observe(this, resource -> {
            if (resource.status == Resource.Status.ERROR) {
                Toast.makeText(this, "Send failed: " + resource.message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}