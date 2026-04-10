package com.example.smartgarbage.ui.map;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.smartgarbage.R;
import com.example.smartgarbage.data.model.Bin;
import com.example.smartgarbage.data.model.DirectionsResponse;
import com.example.smartgarbage.ui.bins.BinListActivity;
import com.example.smartgarbage.ui.home.HomeActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.maps.android.PolyUtil;

import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    private MapViewModel viewModel;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;

    private List<Bin> allBins = new ArrayList<>();
    private List<Bin> routeBins = new ArrayList<>();
    private Location driverLocation;

    private CardView cardRouteInfo;
    private TextView tvRouteTitle;
    private TextView tvRouteInfo;
    private LinearLayout layoutRouteLoading;
    private TextView tvNoCriticalBins;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        viewModel = new ViewModelProvider(this).get(MapViewModel.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        bindViews();
        setupToolbar();
        setupBottomNav();
        setupMap();
        observeViewModel();
    }

    private void bindViews() {
        cardRouteInfo      = findViewById(R.id.cardRouteInfo);
        tvRouteTitle       = findViewById(R.id.tvRouteTitle);
        tvRouteInfo        = findViewById(R.id.tvRouteInfo);
        layoutRouteLoading = findViewById(R.id.layoutRouteLoading);
        tvNoCriticalBins   = findViewById(R.id.tvNoCriticalBins);

        findViewById(R.id.btnNavigate).setOnClickListener(v -> openGoogleMapsNavigation());
    }

    private void setupToolbar() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);

        if (hasLocationPermission()) {
            enableMyLocation();
            getCurrentLocationAndLoad();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        }
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressWarnings("MissingPermission")
    private void enableMyLocation() {
        if (googleMap != null) googleMap.setMyLocationEnabled(true);
    }

    @SuppressWarnings("MissingPermission")
    private void getCurrentLocationAndLoad() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            driverLocation = location;
            viewModel.loadAssignedBins();
        }).addOnFailureListener(e -> viewModel.loadAssignedBins());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
                getCurrentLocationAndLoad();
            } else {
                Toast.makeText(this,
                        "Location permission denied. Route unavailable.", Toast.LENGTH_LONG).show();
                viewModel.loadAssignedBins();
            }
        }
    }

    private void observeViewModel() {
        viewModel.getBins().observe(this, resource -> {
            if (resource == null) return;
            switch (resource.status) {
                case LOADING:
                    break;
                case SUCCESS:
                    if (resource.data != null) {
                        allBins = resource.data;
                        plotBinMarkers(allBins);
                        routeBins = viewModel.getRouteBins(allBins);
                        if (!routeBins.isEmpty() && driverLocation != null) {
                            viewModel.fetchOptimizedRoute(
                                    driverLocation.getLatitude(),
                                    driverLocation.getLongitude(),
                                    routeBins);
                        } else if (routeBins.isEmpty()) {
                            tvNoCriticalBins.setVisibility(View.VISIBLE);
                        } else {
                            Toast.makeText(this,
                                    "Could not get your location. Route unavailable.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;
                case ERROR:
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show();
                    break;
            }
        });

        viewModel.getDirections().observe(this, resource -> {
            if (resource == null) return;
            switch (resource.status) {
                case LOADING:
                    layoutRouteLoading.setVisibility(View.VISIBLE);
                    cardRouteInfo.setVisibility(View.GONE);
                    break;
                case SUCCESS:
                    layoutRouteLoading.setVisibility(View.GONE);
                    if (resource.data != null) {

                        // ── Reorder routeBins to match Google's optimized sequence ──
                        // Google only reorders the MIDDLE stops (waypoints), not the destination.
                        // waypoint_order is a list like [2, 0, 1] meaning:
                        //   visit original waypoint[2] first, then [0], then [1]
                        // The last bin (destination) is never reordered — always stays last.
                        if (resource.data.routes != null
                                && !resource.data.routes.isEmpty()) {

                            List<Integer> order =
                                    resource.data.routes.get(0).waypointOrder;

                            if (order != null && !order.isEmpty()) {
                                // Separate the waypoints (all but last) from the fixed destination
                                List<Bin> waypoints = new ArrayList<>(
                                        routeBins.subList(0, routeBins.size() - 1));
                                Bin destination = routeBins.get(routeBins.size() - 1);

                                // Rebuild routeBins in Google's optimized order
                                List<Bin> optimizedBins = new ArrayList<>();
                                for (int idx : order) {
                                    optimizedBins.add(waypoints.get(idx));
                                }
                                optimizedBins.add(destination); // destination always last
                                routeBins = optimizedBins;
                            }
                        }

                        drawRoute(resource.data);
                        showRouteInfo(resource.data);
                        addNumberedStopMarkers(routeBins); // now numbered in correct optimized order
                    }
                    break;
                case ERROR:
                    layoutRouteLoading.setVisibility(View.GONE);
                    Toast.makeText(this,
                            "Route error: " + resource.message, Toast.LENGTH_LONG).show();
                    break;
            }
        });
    }

    private void plotBinMarkers(List<Bin> bins) {
        if (googleMap == null) return;
        googleMap.clear();

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boolean hasValidCoords = false;

        for (Bin bin : bins) {
            if (bin.getLatitude() == null || bin.getLongitude() == null) continue;

            LatLng pos = new LatLng(bin.getLatitude(), bin.getLongitude());
            int fill = bin.getFillPercentage();

            float hue;
            if (fill >= 80) {
                hue = BitmapDescriptorFactory.HUE_RED;
            } else if (fill >= 50) {
                hue = BitmapDescriptorFactory.HUE_ORANGE;
            } else {
                hue = BitmapDescriptorFactory.HUE_GREEN;
            }

            googleMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title(bin.getName())
                    .snippet("Fill: " + fill + "% | " + bin.getLocation())
                    .icon(BitmapDescriptorFactory.defaultMarker(hue)));

            boundsBuilder.include(pos);
            hasValidCoords = true;
        }

        if (driverLocation != null) {
            boundsBuilder.include(new LatLng(
                    driverLocation.getLatitude(), driverLocation.getLongitude()));
            hasValidCoords = true;
        }

        if (hasValidCoords) {
            try {
                googleMap.animateCamera(
                        CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120));
            } catch (Exception e) {
                if (!bins.isEmpty() && bins.get(0).getLatitude() != null) {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(bins.get(0).getLatitude(),
                                    bins.get(0).getLongitude()), 13f));
                }
            }
        }
    }

    private void drawRoute(DirectionsResponse response) {
        if (googleMap == null || response.routes == null || response.routes.isEmpty()) return;

        String encodedPolyline = response.routes.get(0).overviewPolyline.points;
        List<LatLng> points = PolyUtil.decode(encodedPolyline);

        googleMap.addPolyline(new PolylineOptions()
                .addAll(points)
                .width(10f)
                .color(Color.parseColor("#2196F3"))
                .geodesic(true));
    }

    private void addNumberedStopMarkers(List<Bin> stops) {
        if (googleMap == null) return;
        for (int i = 0; i < stops.size(); i++) {
            Bin bin = stops.get(i);
            if (bin.getLatitude() == null || bin.getLongitude() == null) continue;

            LatLng pos = new LatLng(bin.getLatitude(), bin.getLongitude());
            int fill = bin.getFillPercentage();

            googleMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title("Stop " + (i + 1) + ": " + bin.getName())
                    .snippet("Fill: " + fill + "% | " + bin.getLocation())
                    .icon(createNumberedMarkerIcon(i + 1))
                    .zIndex(1.0f));
        }
    }

    private BitmapDescriptor createNumberedMarkerIcon(int number) {
        int size = 80;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(Color.parseColor("#D32F2F"));
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2, circlePaint);

        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setColor(Color.WHITE);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(3f);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2, strokePaint);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(32f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

        Rect bounds = new Rect();
        String text = String.valueOf(number);
        textPaint.getTextBounds(text, 0, text.length(), bounds);
        float y = size / 2f - bounds.exactCenterY();
        canvas.drawText(text, size / 2f, y, textPaint);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private void showRouteInfo(DirectionsResponse response) {
        cardRouteInfo.setVisibility(View.VISIBLE);
        int stopCount = routeBins.size();
        tvRouteTitle.setText("Optimized Route — " + stopCount
                + " stop" + (stopCount == 1 ? "" : "s"));

        if (response.routes != null && !response.routes.isEmpty()
                && response.routes.get(0).legs != null
                && !response.routes.get(0).legs.isEmpty()) {

            int totalSeconds = 0;
            int totalMeters = 0;
            for (DirectionsResponse.Leg leg : response.routes.get(0).legs) {
                totalSeconds += leg.duration.value;
                totalMeters += leg.distance.value;
            }

            String distText = totalMeters >= 1000
                    ? String.format("%.1f km", totalMeters / 1000.0)
                    : totalMeters + " m";
            int minutes = totalSeconds / 60;
            String durText = minutes >= 60
                    ? (minutes / 60) + "h " + (minutes % 60) + "m"
                    : minutes + " min";

            tvRouteInfo.setText("Est. " + durText + " · " + distText);
        } else {
            tvRouteInfo.setText(stopCount + " bin" + (stopCount == 1 ? "" : "s") + " to collect");
        }
    }

    private void openGoogleMapsNavigation() {
        if (routeBins.isEmpty()) {
            Toast.makeText(this, "No route to navigate", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder urlBuilder = new StringBuilder("https://www.google.com/maps/dir/?api=1");

        if (driverLocation != null) {
            urlBuilder.append("&origin=")
                    .append(driverLocation.getLatitude())
                    .append(",")
                    .append(driverLocation.getLongitude());
        }

        Bin dest = routeBins.get(routeBins.size() - 1);
        urlBuilder.append("&destination=")
                .append(dest.getLatitude())
                .append(",")
                .append(dest.getLongitude());

        if (routeBins.size() > 1) {
            urlBuilder.append("&waypoints=");
            for (int i = 0; i < routeBins.size() - 1; i++) {
                if (i > 0) urlBuilder.append("|");
                Bin bin = routeBins.get(i);
                urlBuilder.append(bin.getLatitude()).append(",").append(bin.getLongitude());
            }
        }

        urlBuilder.append("&travelmode=driving");

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlBuilder.toString()));
        intent.setPackage("com.google.android.apps.maps");

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            intent.setPackage(null);
            startActivity(intent);
        }
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_map);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_map) {
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
}