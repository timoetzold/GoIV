package com.kamron.pogoiv.widgets.recyclerviews.adapters;

import android.graphics.Color;
import android.graphics.Typeface;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kamron.pogoiv.Pokefly;
import com.kamron.pogoiv.databinding.IvResultsItemBinding;
import com.kamron.pogoiv.scanlogic.IVCombination;
import com.kamron.pogoiv.scanlogic.ScanResult;
import com.kamron.pogoiv.utils.GuiUtil;

import java.util.List;

/**
 * Created by OskO on 29/08/16.
 */
public class IVResultsAdapter extends RecyclerView.Adapter<IVResultsAdapter.ResultsViewHolder> {
    private final List<IVCombination> dataSet;
    private final Pokefly pokefly;

    public IVResultsAdapter(ScanResult scanResult, Pokefly pokefly) {
        dataSet = scanResult.getIVCombinations();
        this.pokefly = pokefly;
    }

    @NonNull
    @Override
    public ResultsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        IvResultsItemBinding binding = IvResultsItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent,
                false);
        return new ResultsViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(ResultsViewHolder holder, int position) {
        IVCombination currentSet = dataSet.get(position);

        holder.resultAttack.setText(String.valueOf(currentSet.att));
        holder.resultDefense.setText(String.valueOf(currentSet.def));
        holder.resultHP.setText(String.valueOf(currentSet.sta));
        holder.resultPercentage.setText(String.valueOf(currentSet.percentPerfect));

        GuiUtil.setTextColorByIV(holder.resultAttack, currentSet.att);
        GuiUtil.setTextColorByIV(holder.resultDefense, currentSet.def);
        GuiUtil.setTextColorByIV(holder.resultHP, currentSet.sta);
        GuiUtil.setTextColorByPercentage(holder.resultPercentage, currentSet.percentPerfect);

        if (currentSet.equals(Pokefly.scanResult.selectedIVCombination)) {
            holder.resultAttack.setTypeface(null, Typeface.BOLD);
            holder.resultDefense.setTypeface(null, Typeface.BOLD);
            holder.resultHP.setTypeface(null, Typeface.BOLD);
            holder.resultPercentage.setTypeface(null, Typeface.BOLD);
            holder.llRvResult.setBackgroundColor(Color.parseColor("#E8F8FF"));
        } else {
            holder.resultAttack.setTypeface(null, Typeface.NORMAL);
            holder.resultDefense.setTypeface(null, Typeface.NORMAL);
            holder.resultHP.setTypeface(null, Typeface.NORMAL);
            holder.resultPercentage.setTypeface(null, Typeface.NORMAL);
            if (position % 2 != 0) {
                holder.llRvResult.setBackgroundColor(Color.parseColor("#EFEFEF"));
            } else {
                holder.llRvResult.setBackgroundColor(Color.parseColor("#FFFFFF"));
            }
        }
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    class ResultsViewHolder extends RecyclerView.ViewHolder {
        final TextView resultAttack;
        final TextView resultDefense;
        final TextView resultHP;
        final TextView resultPercentage;
        final LinearLayout llRvResult;

        ResultsViewHolder(IvResultsItemBinding binding) {
            super(binding.getRoot());

            resultAttack = binding.resultAttack;
            resultDefense = binding.resultDefense;
            resultHP = binding.resultHP;
            resultPercentage = binding.resultPercentage;
            llRvResult = binding.llRvResult;

            binding.getRoot().setOnClickListener(v -> {
                pokefly.addSpecificIVClipboard(dataSet.get(getBindingAdapterPosition()));
                IVResultsAdapter.this.notifyDataSetChanged();
            });
        }

    }


}