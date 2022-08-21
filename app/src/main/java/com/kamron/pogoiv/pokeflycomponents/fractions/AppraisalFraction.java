package com.kamron.pogoiv.pokeflycomponents.fractions;

import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import com.kamron.pogoiv.Pokefly;
import com.kamron.pogoiv.R;
import com.kamron.pogoiv.databinding.FractionAppraisalBinding;
import com.kamron.pogoiv.pokeflycomponents.AppraisalManager;
import com.kamron.pogoiv.utils.GUIColorFromPokeType;
import com.kamron.pogoiv.utils.ReactiveColorListener;
import com.kamron.pogoiv.utils.fractions.MovableFraction;

import static com.kamron.pogoiv.GoIVSettings.APPRAISAL_WINDOW_POSITION;

public class AppraisalFraction extends MovableFraction implements AppraisalManager.OnAppraisalEventListener,
        ReactiveColorListener {

    private final Pokefly pokefly;
    private final AppraisalManager appraisalManager;

    private FractionAppraisalBinding binding;

    private boolean insideUpdate = false;


    public AppraisalFraction(@NonNull Pokefly pokefly,
                             @NonNull SharedPreferences sharedPrefs,
                             @NonNull AppraisalManager appraisalManager) {
        super(sharedPrefs);
        this.pokefly = pokefly;
        this.appraisalManager = appraisalManager;
    }

    @Override
    @Nullable
    protected String getVerticalOffsetSharedPreferencesKey() {
        return APPRAISAL_WINDOW_POSITION;
    }

    @Override
    public void onCreate(LayoutInflater inflater, ViewGroup parent, boolean attachToParent) {
        binding = FractionAppraisalBinding.inflate(inflater, parent, attachToParent);

        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!insideUpdate) {
                    if (appraisalManager.attack != binding.atkSeek.getProgress()) {
                        appraisalManager.attackValid = true;
                    }
                    appraisalManager.attack = binding.atkSeek.getProgress();
                    if (appraisalManager.defense != binding.defSeek.getProgress()) {
                        appraisalManager.defenseValid = true;
                    }
                    appraisalManager.defense = binding.defSeek.getProgress();
                    if (appraisalManager.stamina != binding.staSeek.getProgress()) {
                        appraisalManager.staminaValid = true;
                    }
                    appraisalManager.stamina = binding.staSeek.getProgress();
                    updateValueTexts();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Nothing to do here
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Nothing to do here
            }
        };

        binding.atkSeek.setOnSeekBarChangeListener(listener);
        binding.defSeek.setOnSeekBarChangeListener(listener);
        binding.staSeek.setOnSeekBarChangeListener(listener);
        setSpinnerSelection();

        // Listen for new appraisal info
        appraisalManager.addOnAppraisalEventListener(this);
        binding.btnRetry.setImageResource(R.drawable.ic_play_circle_outline_24px);

        binding.atkEnabled.setOnCheckedChangeListener((compoundButton, b) -> onEnabled());
        binding.defEnabled.setOnCheckedChangeListener((compoundButton, b) -> onEnabled());
        binding.staEnabled.setOnCheckedChangeListener((compoundButton, b) -> onEnabled());

        binding.positionHandler.setOnTouchListener((view, motionEvent) -> positionHandlerTouchEvent(view, motionEvent));
        binding.additionalRefiningHeader.setOnTouchListener((view, motionEvent) -> positionHandlerTouchEvent(view, motionEvent));

        binding.statsButton.setOnClickListener(view -> onStats());
        binding.btnClose.setOnClickListener(view -> onClose());
        binding.btnCheckIv.setOnClickListener(view -> checkIv());
        binding.btnRetry.setOnClickListener(view -> onRetryClick());

        GUIColorFromPokeType.getInstance().setListenTo(this);
        updateGuiColors();
    }

    @Override
    public void onDestroy() {
        appraisalManager.removeOnAppraisalEventListener(this);
        GUIColorFromPokeType.getInstance().removeListener(this);
        binding = null;
    }

    @Override
    public Anchor getAnchor() {
        return Anchor.TOP;
    }

    @Override
    public int getDefaultVerticalOffset(DisplayMetrics displayMetrics) {
        return 0;
    }

    /**
     * Sets the background for the appropriate checkbox group depending on where we are at in the appraisal process.
     */
    @Override
    public void highlightActiveUserInterface() {
        binding.valueLayout.setBackgroundResource(R.drawable.highlight_rectangle);
        binding.pbScanning.setVisibility(View.VISIBLE);
        binding.btnRetry.setVisibility(View.INVISIBLE);
    }


    /**
     * Update the text on the 'next' button to indicate quick IV overview
     */
    private void updateIVPreviewInButton() {
        InputFraction.updateIVPreview(pokefly, binding.btnCheckIv);
    }

    public void onEnabled() {
        if (!insideUpdate) {
            appraisalManager.attackValid = binding.atkEnabled.isChecked();
            appraisalManager.defenseValid = binding.defEnabled.isChecked();
            appraisalManager.staminaValid = binding.staEnabled.isChecked();
            updateIVPreviewInButton();
        }
    }

    @Override
    public void refreshSelection() {
        setSpinnerSelection();
        binding.valueLayout.setBackground(null);
        binding.pbScanning.setVisibility(View.INVISIBLE);
        binding.btnRetry.setVisibility(View.VISIBLE);
    }

    /**
     * Updates the checkboxes, labels and IV preview in the button.
     */
    private void updateValueTexts() {
        binding.atkValue.setText(String.valueOf(appraisalManager.attack));
        binding.defValue.setText(String.valueOf(appraisalManager.defense));
        binding.staValue.setText(String.valueOf(appraisalManager.stamina));
        binding.atkEnabled.setChecked(appraisalManager.attackValid);
        binding.defEnabled.setChecked(appraisalManager.defenseValid);
        binding.staEnabled.setChecked(appraisalManager.staminaValid);
        updateIVPreviewInButton();
    }

    /**
     * Sets the progress bars and calls {@code updateValueTexts()}, mainly used to update it from the outside.
     */
    private void setSpinnerSelection() {
        insideUpdate = true;
        updateValueTexts();
        binding.atkSeek.setProgress(appraisalManager.attack);
        binding.defSeek.setProgress(appraisalManager.defense);
        binding.staSeek.setProgress(appraisalManager.stamina);
        insideUpdate = false;
    }

    boolean positionHandlerTouchEvent(View v, MotionEvent event) {
        return super.onTouch(v, event);
    }

    void onStats() {
        pokefly.navigateToInputFraction();
    }

    void onClose() {
        pokefly.closeInfoDialog();
    }

    void checkIv() {
        pokefly.computeIv();
    }

    void onRetryClick() {
        if (appraisalManager.isRunning()) {
            appraisalManager.stop();
        } else {
            appraisalManager.start();
        }
    }

    @Override
    public void updateGuiColors() {
        int c = GUIColorFromPokeType.getInstance().getColor();
        binding.btnCheckIv.setBackgroundColor(c);
        binding.statsButton.setBackgroundColor(c);
        binding.headerAppraisal.setBackgroundColor(c);
    }
}
