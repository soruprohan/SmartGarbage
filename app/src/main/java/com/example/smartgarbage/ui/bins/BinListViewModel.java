package com.example.smartgarbage.ui.bins;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.example.smartgarbage.data.model.Bin;
import com.example.smartgarbage.data.repository.BinRepository;
import com.example.smartgarbage.utils.Resource;
import com.example.smartgarbage.utils.TokenManager;

import java.util.List;

public class BinListViewModel extends AndroidViewModel {

    private final BinRepository binRepository;
    private final MediatorLiveData<Resource<List<Bin>>> binsLiveData = new MediatorLiveData<>();

    public BinListViewModel(@NonNull Application application) {
        super(application);
        TokenManager tokenManager = new TokenManager(application);
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
}
