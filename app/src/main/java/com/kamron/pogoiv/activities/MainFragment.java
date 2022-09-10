package com.kamron.pogoiv.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.core.view.ViewCompat;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.kamron.pogoiv.GoIVSettings;
import com.kamron.pogoiv.NpTrainerLevelPickerListener;
import com.kamron.pogoiv.Pokefly;
import com.kamron.pogoiv.R;
import com.kamron.pogoiv.databinding.FragmentMainBinding;
import com.kamron.pogoiv.scanlogic.Data;
import com.kamron.pogoiv.widgets.PlayerTeamAdapter;

import timber.log.Timber;

public class MainFragment extends Fragment {

    private static final String ACTION_UPDATE_LAUNCH_BUTTON = "com.kamron.pogoiv.ACTION_UPDATE_LAUNCH_BUTTON";
    private static final String EXTRA_BUTTON_TEXT_RES_ID = "btn_txt_res_id";
    private static final String EXTRA_BUTTON_ENABLED = "btn_enabled";

    private FragmentMainBinding binding;

    private final BroadcastReceiver launchButtonChange = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(EXTRA_BUTTON_TEXT_RES_ID)) {
                binding.startButton.setText(intent.getIntExtra(EXTRA_BUTTON_TEXT_RES_ID, 0));
            }
            if (intent.hasExtra(EXTRA_BUTTON_ENABLED)) {
                binding.startButton.setEnabled(intent.getBooleanExtra(EXTRA_BUTTON_ENABLED, true));
            }
        }
    };


    public static void updateLaunchButtonText(@NonNull Context context,
                                              @StringRes int stringResId,
                                              @Nullable Boolean enabled) {
        Intent i = new Intent(ACTION_UPDATE_LAUNCH_BUTTON);
        i.putExtra(EXTRA_BUTTON_TEXT_RES_ID, stringResId);
        if (enabled != null) {
            i.putExtra(EXTRA_BUTTON_ENABLED, enabled);
        }
        LocalBroadcastManager.getInstance(context).sendBroadcastSync(i);
    }


    public MainFragment() {
        super();
    }

    @Override
    @Nullable
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMainBinding.inflate(inflater, container, false);
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
        initiateGui();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            PreferenceManager.setDefaultValues(getContext(), R.xml.settings, false);
        }
    }

    @Override public void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(launchButtonChange,
                new IntentFilter(ACTION_UPDATE_LAUNCH_BUTTON));

        if (Pokefly.isRunning()) {
            updateLaunchButtonText(getContext(), R.string.main_stop, true);
        } else {
            updateLaunchButtonText(getContext(), R.string.main_start, true);
        }
    }

    @Override public void onPause() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(launchButtonChange);
        super.onPause();
    }

    /**
     * Initiates all the gui components.
     */
    private void initiateGui() {
        binding.versionNumber.setText(String.format("v%s", getVersionName()));
        initiateLevelPicker();
        initiateTeamPickerSpinner();
        initiateHelpButton();
        initiateCommunityButtons();
        initiateStartButton();
    }

    /**
     * Initiates the team picker spinner.
     */
    private void initiateTeamPickerSpinner() {
        PlayerTeamAdapter adapter = new PlayerTeamAdapter(getContext());
        binding.teamPickerSpinner.setAdapter(adapter);

        binding.teamPickerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                GoIVSettings.getInstance(getContext()).setPlayerTeam(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }

        });
        binding.teamPickerSpinner.setSelection(GoIVSettings.getInstance(getContext()).playerTeam());
    }

    /**
     * Initiates the links to reddit and github.
     */
    private void initiateCommunityButtons() {
        binding.redditButton.setOnClickListener(v -> openUrl("https://www.reddit.com/r/GoIV/"));
        binding.githubButton.setOnClickListener(v -> openUrl("https://github.com/GoIV-Devs/GoIV"));
    }

    private void openUrl(String url) {
        Activity activity = getActivity();
        if (activity != null) {
            Uri uriUrl = Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW, uriUrl);
            if (intent.resolveActivity(activity.getPackageManager()) != null) {
                startActivity(intent);
            }
        }
    }

    private void initiateHelpButton() {
        final Context context = getContext();
        if (context == null) {
            return;
        }
        binding.helpButton.setOnClickListener(v -> new AlertDialog.Builder(context)
                .setTitle(R.string.instructions_title)
                .setMessage(R.string.instructions_message)
                .setPositiveButton(android.R.string.ok, null)
                .show());
    }

    /**
     * Configures the logic for the start button.
     */
    private void initiateStartButton() {
        ViewCompat.setBackgroundTintList(binding.startButton, null);
        binding.startButton.setOnClickListener(v -> {
            Activity activity = getActivity();
            if (activity instanceof MainActivity) {
                // This call to clearFocus will accept whatever input the user pressed, without
                // forcing him to press the green check mark on the keyboard.
                // Otherwise the typed value won't be read if either:
                // - the user presses Start before closing the keyboard, or
                // - the user closes the keyboard with the back button (note that does not cancel
                //   the typed text).
                binding.trainerLevelPicker.clearFocus();
                GoIVSettings.getInstance(activity).setLevel(binding.trainerLevelPicker.getValue());
                ((MainActivity) activity).runStartButtonLogic();
            }
        });
    }

    /**
     * Initiates the scrollable level picker.
     */
    private void initiateLevelPicker() {
        binding.trainerLevelPicker.setMinValue(Data.MINIMUM_TRAINER_LEVEL);
        binding.trainerLevelPicker.setMaxValue(Data.MAXIMUM_TRAINER_LEVEL);
        binding.trainerLevelPicker.setWrapSelectorWheel(false);
        binding.trainerLevelPicker.setValue(GoIVSettings.getInstance(getContext()).getLevel());
        NpTrainerLevelPickerListener listener = new NpTrainerLevelPickerListener(getContext());
        binding.trainerLevelPicker.setOnScrollListener(listener);
        binding.trainerLevelPicker.setOnValueChangedListener(listener);
    }

    private String getVersionName() {
        Context context = getContext();
        if (context == null) {
            return "";
        }

        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Timber.e("Exception thrown while getting version name");
            Timber.e(e);
        }
        return "Error while getting version name";
    }

}
