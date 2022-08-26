package com.kamron.pogoiv.pokeflycomponents.fractions;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.kamron.pogoiv.Pokefly;
import com.kamron.pogoiv.databinding.FractionIvCombinationsBinding;
import com.kamron.pogoiv.utils.fractions.Fraction;
import com.kamron.pogoiv.widgets.recyclerviews.adapters.IVResultsAdapter;

public class IVCombinationsFraction extends Fraction {

    private final Context context;
    private final Pokefly pokefly;

    private FractionIvCombinationsBinding binding;

    public IVCombinationsFraction(@NonNull Pokefly pokefly) {
        this.context = pokefly;
        this.pokefly = pokefly;
    }

    @Override
    public void onCreate(LayoutInflater inflater, ViewGroup parent, boolean attachToParent) {
        binding = FractionIvCombinationsBinding.inflate(inflater, parent, attachToParent);

        // All IV combinations RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        binding.rvResults.setLayoutManager(layoutManager);
        binding.rvResults.setHasFixedSize(true);
        Pokefly.scanResult.sortIVCombinations();
        binding.rvResults.setAdapter(new IVResultsAdapter(Pokefly.scanResult, pokefly));

        binding.btnBack.setOnClickListener(view -> onBack());
        binding.btnClose.setOnClickListener(view -> onClose());
    }

    @Override
    public void onDestroy() {
        binding = null;
        // Nothing to do
    }

    @Override
    public Anchor getAnchor() {
        return Anchor.BOTTOM;
    }

    @Override
    public int getVerticalOffset(@NonNull DisplayMetrics displayMetrics) {
        return 0;
    }

    void onBack() {
        pokefly.navigateToIVResultFraction();
    }

    void onClose() {
        pokefly.closeInfoDialog();
    }
}

