package com.kamron.pogoiv.widgets.recyclerviews.adapters.viewholders;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.kamron.pogoiv.R;
import com.kamron.pogoiv.clipboardlogic.ClipboardToken;
import com.kamron.pogoiv.clipboardlogic.tokens.SeparatorToken;
import com.kamron.pogoiv.databinding.LayoutTokenPreviewBinding;

public class TokenViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private final LayoutTokenPreviewBinding binding;

    private final ClipboardToken.OnTokenSelectedListener onTokenSelectedListener;
    private final ClipboardToken.OnTokenDeleteListener onTokenDeleteListener;

    private final boolean showPreview;

    public TokenViewHolder(LayoutTokenPreviewBinding binding,
                           ClipboardToken.OnTokenSelectedListener onTokenSelectedListener,
                           ClipboardToken.OnTokenDeleteListener onTokenDeleteListener,
                           boolean widthMatchParent,
                           boolean showPreview) {
        super(binding.getRoot());
        this.binding = binding;
        this.onTokenSelectedListener = onTokenSelectedListener;
        this.onTokenDeleteListener = onTokenDeleteListener;
        this.showPreview = showPreview;

        if (!showPreview) {
            binding.text2.setVisibility(View.GONE);
        }
        if (onTokenDeleteListener != null) {
            binding.btnDelete.setOnClickListener(this);
        } else {
            binding.btnDelete.setVisibility(View.GONE);
        }
        if (onTokenSelectedListener != null) {
            itemView.setOnClickListener(this);
        }
        if (widthMatchParent) {
            itemView.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
        }
    }

    public void bind(ClipboardToken token) {
        itemView.setTag(token);
        itemView.setActivated(token.maxEv);
        binding.evolvedVariantImageView.setVisibility(token.maxEv ? View.VISIBLE : View.GONE);
        if (showPreview && token instanceof SeparatorToken) {
            binding.text1.setText(null);
        } else {
            binding.text1.setText(token.getTokenName(itemView.getContext()));
        }
        if (showPreview) {
            binding.text2.setText(token.getPreview());
        } else {
            binding.text2.setVisibility(View.GONE);
        }
    }

    @Override public void onClick(View v) {
        if (v.getId() == R.id.btnDelete) {
            if (onTokenDeleteListener != null) {
                onTokenDeleteListener.onTokenDeleted(getBindingAdapterPosition());
            }
        } else if (onTokenSelectedListener != null) {
            onTokenSelectedListener.onTokenSelected(((ClipboardToken) v.getTag()), getBindingAdapterPosition());
        }
    }
}
