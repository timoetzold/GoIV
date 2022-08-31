package com.kamron.pogoiv.activities;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.common.base.Strings;
import com.kamron.pogoiv.R;
import com.kamron.pogoiv.clipboardlogic.ClipboardResultMode;
import com.kamron.pogoiv.clipboardlogic.ClipboardToken;
import com.kamron.pogoiv.clipboardlogic.ClipboardTokenHandler;
import com.kamron.pogoiv.clipboardlogic.tokens.CustomAppraiseSign;
import com.kamron.pogoiv.clipboardlogic.tokens.CustomNameToken;
import com.kamron.pogoiv.clipboardlogic.tokens.CustomSeparatorToken;
import com.kamron.pogoiv.clipboardlogic.tokens.HasBeenAppraisedToken;
import com.kamron.pogoiv.clipboardlogic.tokens.PokemonNameToken;
import com.kamron.pogoiv.clipboardlogic.tokens.SeparatorToken;
import com.kamron.pogoiv.databinding.EdittextDialogBinding;
import com.kamron.pogoiv.databinding.FragmentClipboardModifierChildBinding;
import com.kamron.pogoiv.widgets.recyclerviews.adapters.TokensPreviewAdapter;
import com.kamron.pogoiv.widgets.recyclerviews.adapters.TokensShowcaseAdapter;
import com.kamron.pogoiv.widgets.recyclerviews.decorators.MarginItemDecorator;
import com.kamron.pogoiv.widgets.recyclerviews.layoutmanagers.TokenGridLayoutManager;

import java.util.Locale;

import static androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL;


public class ClipboardModifierChildFragment extends Fragment implements ClipboardToken.OnTokenSelectedListener {

    private static final String ARG_RESULT_MODE = "a_srm";

    private FragmentClipboardModifierChildBinding binding;

    private ClipboardResultMode resultMode;
    private boolean maxEvolutionVariant = true;
    private TokensPreviewAdapter tokenPreviewAdapter;
    private TokensShowcaseAdapter tokenShowcaseAdapter;
    private ClipboardToken selectedToken = null;
    private ClipboardTokenHandler cth;


    public static ClipboardModifierChildFragment newInstance(ClipboardResultMode resultMode) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_RESULT_MODE, resultMode);
        ClipboardModifierChildFragment f = new ClipboardModifierChildFragment();
        f.setArguments(args);
        return f;
    }

    public ClipboardModifierChildFragment() {
        super();
    }

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        resultMode = (ClipboardResultMode) getArguments().get(ARG_RESULT_MODE);
        cth = new ClipboardTokenHandler(getContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentClipboardModifierChildBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Populate the token preview RecyclerView with all configured tokens.
        tokenPreviewAdapter = new TokensPreviewAdapter();
        tokenPreviewAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            private void updateMaxLength() {
                binding.clipboardMaxLength.setText(String.format(Locale.getDefault(),
                        getString(R.string.token_max_characters), tokenPreviewAdapter.getMaxLength()));
            }

            @Override public void onChanged() {
                updateMaxLength();
            }

            @Override public void onItemRangeInserted(int positionStart, int itemCount) {
                updateMaxLength();
            }

            @Override public void onItemRangeRemoved(int positionStart, int itemCount) {
                updateMaxLength();
            }
        });
        tokenPreviewAdapter.setData(cth.getTokens(resultMode));
        binding.tokenPreviewRecyclerView.setHasFixedSize(false);
        binding.tokenPreviewRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), HORIZONTAL, false));
        binding.tokenPreviewRecyclerView.addItemDecoration(new MarginItemDecorator(2, 0, 2, 0));
        binding.tokenPreviewRecyclerView.setAdapter(tokenPreviewAdapter);
        new ItemTouchHelper(new TokensPreviewAdapter.TokenTouchCallback(tokenPreviewAdapter))
                .attachToRecyclerView(binding.tokenPreviewRecyclerView);

        // Populate the token showcase RecyclerView with all possible tokens. The TokenListAdapter will put them in
        // their respective category while TokenGridLayoutManager will arrange them in a grid with category headers
        // that span the entire RecyclerView width.
        tokenShowcaseAdapter = new TokensShowcaseAdapter(getContext(), maxEvolutionVariant, this);
        binding.tokenShowcaseRecyclerView.setHasFixedSize(false);
        binding.tokenShowcaseRecyclerView.setLayoutManager(new TokenGridLayoutManager(getContext(), tokenShowcaseAdapter));
        binding.tokenShowcaseRecyclerView.addItemDecoration(new MarginItemDecorator(2, 4, 2, 4));
        binding.tokenShowcaseRecyclerView.setAdapter(tokenShowcaseAdapter);

        // Set the drawable here since app:srcCompat attribute in XML isn't working and android:src crashes on API 19
        binding.btnAdd.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_add_white_24px));
        binding.btnAdd.setOnClickListener(v -> addToken());

        // Set the drawable here since app:srcCompat attribute in XML isn't working and android:src crashes on API 19
        binding.btnMaxEvolution.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_star_white_18dp));
        binding.btnMaxEvolution.setOnClickListener(v -> {
            maxEvolutionVariant = !maxEvolutionVariant;
            if (maxEvolutionVariant) {
                binding.btnMaxEvolution.setImageDrawable(
                        ContextCompat.getDrawable(getContext(), R.drawable.ic_star_white_18dp));
                Toast.makeText(getContext(), R.string.token_show_max_evo_variant, Toast.LENGTH_SHORT).show();
            } else {
                binding.btnMaxEvolution.setImageDrawable(
                        ContextCompat.getDrawable(getContext(), R.drawable.ic_star_border_white_18dp));
                Toast.makeText(getContext(), R.string.token_show_standard, Toast.LENGTH_SHORT).show();
            }
            tokenShowcaseAdapter.setEvolvedVariant(maxEvolutionVariant);
        });
    }

    public void saveConfiguration() {
        cth.setTokenList(tokenPreviewAdapter.getData(), resultMode);
    }

    /**
     * Check if the user has edited the list of tokens without saving.
     *
     * @return true if there are unsaved changes
     */
    public boolean hasUnsavedChanges() {
        return !cth.savedConfigurationEquals(tokenPreviewAdapter.getData(), resultMode);
    }

    /**
     * Select a token to show its description.
     *
     * @param token Which token to show.
     */
    @Override
    public void onTokenSelected(ClipboardToken token, int adapterPosition) {
        selectedToken = token;

        final AppCompatActivity activity = (AppCompatActivity) getActivity();
        for (Fragment f : activity.getSupportFragmentManager().getFragments()) {
            if (f instanceof ClipboardModifierParentFragment) {
                ((ClipboardModifierParentFragment) f).updateTokenDescription(selectedToken);
            }
        }
    }

    /**
     * Add a token to the user settings.
     */
    public void addToken() {
        if (selectedToken != null) {
            if (selectedToken instanceof CustomSeparatorToken) {
                buildCustomSeparatorToken();
            }else if (selectedToken instanceof CustomAppraiseSign) {
                buildCustomAppraiseToken();
            }else if (selectedToken instanceof CustomNameToken) {
                buildCustomNameToken();
            } else {
                tokenPreviewAdapter.addItem(selectedToken);
                binding.tokenPreviewRecyclerView.smoothScrollToPosition(tokenPreviewAdapter.getItemCount() - 1);
            }
        } else {
            Toast.makeText(getContext(), R.string.clipboard_no_token_selected, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Builds a Dialog to create a custom string token to be added to the user settings.
     */
    public void buildCustomSeparatorToken() {
        // The custom separator will be written in this EditText
        final EdittextDialogBinding dialogBinding = EdittextDialogBinding.inflate(LayoutInflater.from(getContext()),
                null, false);

        // This dialog will implement the user interaction
        new AlertDialog.Builder(getContext())
                .setView(dialogBinding.getRoot())
                .setMessage(R.string.token_input_custom_separator)
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    String separator = dialogBinding.editText.getText().toString();
                    if (Strings.isNullOrEmpty(separator)) {
                        Toast.makeText(getContext(),
                                R.string.token_fill_custom_separator, Toast.LENGTH_LONG).show();
                    } else if (separator.contains(".")) {
                        Toast.makeText(getContext(),
                                R.string.token_not_dot_separator, Toast.LENGTH_LONG).show();
                    } else {
                        selectedToken = new SeparatorToken(separator);
                        addToken();
                    }
                })
                .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> dialogInterface.cancel())
                .show();
    }

    /**
     * Builds a Dialog to create a custom appraisal token to be added to the user settings.
     */
    public void buildCustomAppraiseToken() {
        // The custom separator will be written in this EditText
        final EdittextDialogBinding dialogBinding = EdittextDialogBinding.inflate(LayoutInflater.from(getContext()),
                null, false);

        // This dialog will implement the user interaction
        new AlertDialog.Builder(getContext())
                .setView(dialogBinding.getRoot())
                .setMessage("Please input two symbols, the first representing appraised, and the second unappraised.")
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    String text = dialogBinding.editText.getText().toString();
                    if (Strings.isNullOrEmpty(text)) {
                        Toast.makeText(getContext(),
                               "Please input two symbols, the first representing appraised, and the second unappraised.", Toast.LENGTH_LONG).show();
                    } else if (text.contains(".")) {
                        Toast.makeText(getContext(),
                                R.string.token_not_dot_separator, Toast.LENGTH_LONG).show();

                    } else if (text.length() <2) {
                        Toast.makeText(getContext(),
                                "Please input two symbols", Toast.LENGTH_LONG).show();

                    } else {
                        selectedToken = new HasBeenAppraisedToken(maxEvolutionVariant, text.substring(0,1),text
                                .substring(1,2));
                        addToken();
                    }
                })
                .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> dialogInterface.cancel())
                .show();
    }


    /**
     * Builds a Dialog to create a custom name token to be added to the user settings.
     */
    public void buildCustomNameToken() {
        // The custom separator will be written in this EditText
        final EdittextDialogBinding dialogBinding = EdittextDialogBinding.inflate(LayoutInflater.from(getContext()),
                null, false);

        // This dialog will implement the user interaction
        new AlertDialog.Builder(getContext())
                .setView(dialogBinding.getRoot())
                .setMessage("Please input max allowed length of pokemon name. ")
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    String text = dialogBinding.editText.getText().toString();
                    if (Strings.isNullOrEmpty(text)) {
                        Toast.makeText(getContext(),
                                "Please input max allowed length of pokemon name.", Toast.LENGTH_LONG).show();
                    } else if (text.contains(".")) {
                        Toast.makeText(getContext(),
                                R.string.token_not_dot_separator, Toast.LENGTH_LONG).show();

                    }  else {
                        int input = 0;
                        try {
                            input = Integer.parseInt(text);
                            if (input < 0) { input = 0;}
                        } catch (NumberFormatException e) {
                            Toast.makeText(getContext(),
                                    "Please put a normal number", Toast.LENGTH_LONG).show();
                        }
                        selectedToken = new PokemonNameToken(maxEvolutionVariant, input);
                        addToken();
                    }
                })
                .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> dialogInterface.cancel())
                .show();
    }
}
