package com.example.smartgarbage.ui.bins;

import android.content.Context;
import android.content.res.ColorStateList;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartgarbage.R;
import com.example.smartgarbage.data.model.Bin;

import java.util.ArrayList;
import java.util.List;

public class BinAdapter extends RecyclerView.Adapter<BinAdapter.BinViewHolder> {

    // Existing click listener — for tapping the whole card
    public interface OnBinClickListener {
        void onBinClick(Bin bin);
    }

    // New listener — for the "Mark as Collected" button only
    public interface OnCollectClickListener {
        void onCollectClick(int binId);
    }

    private List<Bin> bins = new ArrayList<>();
    private final OnBinClickListener listener;
    private OnCollectClickListener collectListener;
    private Location driverLocation; // driver's current GPS location

    public BinAdapter(OnBinClickListener listener) {
        this.listener = listener;
    }

    public void setCollectListener(OnCollectClickListener collectListener) {
        this.collectListener = collectListener;
    }

    // Called from BinListActivity after getting the driver's location
    public void setDriverLocation(Location location) {
        this.driverLocation = location;
    }

    public void setBins(List<Bin> bins) {
        this.bins = bins != null ? bins : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BinViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bin, parent, false);
        return new BinViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BinViewHolder holder, int position) {
        holder.bind(bins.get(position));
    }

    @Override
    public int getItemCount() {
        return bins.size();
    }

    class BinViewHolder extends RecyclerView.ViewHolder {
        TextView tvBinName;
        TextView tvLocation;
        TextView tvFillPercent;
        TextView tvStatus;
        ProgressBar progressFill;
        Button btnMarkCollected;

        BinViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBinName        = itemView.findViewById(R.id.tvBinName);
            tvLocation       = itemView.findViewById(R.id.tvBinAddress);
            tvFillPercent    = itemView.findViewById(R.id.tvFillPercentage);
            tvStatus         = itemView.findViewById(R.id.tvBinStatus);
            progressFill     = itemView.findViewById(R.id.pbFillLevel);
            btnMarkCollected = itemView.findViewById(R.id.btnMarkCollected);
        }

        void bind(Bin bin) {
            Context ctx = itemView.getContext();

            tvBinName.setText(bin.getName());
            tvLocation.setText(bin.getLocation() != null ? bin.getLocation() : "—");

            int pct = bin.getFillPercentage();
            tvFillPercent.setText(pct + "%");
            progressFill.setProgress(pct);

            // Color-code fill bar
            int fillColor;
            if (pct >= 80) {
                fillColor = ContextCompat.getColor(ctx, R.color.fill_high);
            } else if (pct >= 50) {
                fillColor = ContextCompat.getColor(ctx, R.color.fill_medium);
            } else {
                fillColor = ContextCompat.getColor(ctx, R.color.fill_low);
            }
            progressFill.setProgressTintList(ColorStateList.valueOf(fillColor));
            tvFillPercent.setTextColor(fillColor);

            // Status badge
            String status = bin.getStatus();
            if (status == null) status = "unknown";
            tvStatus.setText(status.toUpperCase());
            switch (status.toLowerCase()) {
                case "working":
                    tvStatus.setBackgroundTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(ctx, R.color.success_green)));
                    break;
                case "maintenance":
                    tvStatus.setBackgroundTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(ctx, R.color.warning_orange)));
                    break;
                default:
                    tvStatus.setBackgroundTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(ctx, R.color.text_hint)));
                    break;
            }

            // Whole card click
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onBinClick(bin);
            });

            // ── Mark as Collected button ──────────────────────────────────
            btnMarkCollected.setOnClickListener(v -> {

                // GPS proximity check — block if location is unavailable
                if (driverLocation == null) {
                    Toast.makeText(ctx,
                        "Waiting for your GPS location. Please try again in a moment.",
                        Toast.LENGTH_LONG).show();
                    return;
                }

                if (bin.getLatitude() != null && bin.getLongitude() != null) {
                    Location binLocation = new Location("");
                    binLocation.setLatitude(bin.getLatitude());
                    binLocation.setLongitude(bin.getLongitude());

                    float distance = driverLocation.distanceTo(binLocation);

                    if (distance > 100) {
                        Toast.makeText(ctx,
                            "You must be within 100m of the bin.\nCurrently "
                                + (int) distance + "m away.",
                            Toast.LENGTH_LONG).show();
                        return; // blocked
                    }
                }

                // GPS check passed — tell the Activity to open the camera
                if (collectListener != null) {
                    collectListener.onCollectClick(bin.getId());
                }
            });
        }
    }
}
