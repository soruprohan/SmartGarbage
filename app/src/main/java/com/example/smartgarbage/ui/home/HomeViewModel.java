package com.example.smartgarbage.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.example.smartgarbage.data.model.Bin;
import com.example.smartgarbage.data.repository.BinRepository;
import com.example.smartgarbage.utils.Resource;
import com.example.smartgarbage.utils.TokenManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeViewModel extends AndroidViewModel {

    private final BinRepository binRepository;
    private final TokenManager tokenManager;

    // Exposed LiveData for the UI
    private final MediatorLiveData<Resource<List<Bin>>> binsLiveData = new MediatorLiveData<>();

    public HomeViewModel(@NonNull Application application) {
        super(application);
        tokenManager = new TokenManager(application);
        binRepository = new BinRepository(tokenManager);
    }

    public LiveData<Resource<List<Bin>>> getBins() {
        return binsLiveData;
    }

    public void loadAssignedBins() {
        LiveData<Resource<List<Bin>>> source = binRepository.getAssignedBins();
        binsLiveData.addSource(source, resource -> {
            binsLiveData.setValue(resource);
            if (resource != null && !resource.isLoading()) {
                binsLiveData.removeSource(source);
            }
        });
    }

    // --- Computed stats helpers (called after data is loaded) ---

    public int getTotalCount(List<Bin> bins) {
        return bins != null ? bins.size() : 0;
    }

    public int getCriticalCount(List<Bin> bins) {
        if (bins == null) return 0;
        int count = 0;
        for (Bin b : bins) {
            if (b.getFillPercentage() >= 80) count++;
        }
        return count;
    }

    public int getOkCount(List<Bin> bins) {
        if (bins == null) return 0;
        int count = 0;
        for (Bin b : bins) {
            if (b.getFillPercentage() < 80) count++;
        }
        return count;
    }

    /**
     * Returns bins sorted by fill percentage descending (most critical first).
     * Used for the "critical bins" preview list on the dashboard.
     */
    public List<Bin> getSortedByFillDescending(List<Bin> bins) {
        if (bins == null) return new ArrayList<>();
        List<Bin> sorted = new ArrayList<>(bins);
        Collections.sort(sorted, (a, b) -> b.getFillPercentage() - a.getFillPercentage());
        return sorted;
    }

    public String getDriverName() {
        String name = tokenManager.getDriverName();
        return (name != null && !name.isEmpty()) ? name : "Driver";
    }
}
