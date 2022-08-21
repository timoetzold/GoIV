package com.kamron.pogoiv.widgets.recyclerviews.adapters.viewholders;

import androidx.recyclerview.widget.RecyclerView;

import com.kamron.pogoiv.clipboardlogic.ClipboardToken;
import com.kamron.pogoiv.databinding.LayoutTokenHeaderBinding;

public class TokenHeaderViewHolder extends RecyclerView.ViewHolder {

    private final LayoutTokenHeaderBinding binding;

    public TokenHeaderViewHolder(LayoutTokenHeaderBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    public void bind(ClipboardToken.Category category) {
        binding.text1.setText(category.toString(binding.text1.getContext()));
    }

}
