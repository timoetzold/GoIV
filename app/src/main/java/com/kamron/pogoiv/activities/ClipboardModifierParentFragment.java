package com.kamron.pogoiv.activities;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayoutMediator;
import com.kamron.pogoiv.GoIVSettings;
import com.kamron.pogoiv.R;
import com.kamron.pogoiv.clipboardlogic.ClipboardResultMode;
import com.kamron.pogoiv.clipboardlogic.ClipboardToken;
import com.kamron.pogoiv.databinding.FragmentClipboardModifierParentBinding;

import java.util.EnumSet;

import static com.kamron.pogoiv.clipboardlogic.ClipboardResultMode.GENERAL_RESULT;
import static com.kamron.pogoiv.clipboardlogic.ClipboardResultMode.PERFECT_IV_RESULT;
import static com.kamron.pogoiv.clipboardlogic.ClipboardResultMode.SINGLE_RESULT;

public class ClipboardModifierParentFragment extends Fragment {

    private FragmentClipboardModifierParentBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentClipboardModifierParentBinding.inflate(inflater, container, false);
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

        ClipboardResultMode[] resultModesEnabled = getResultModesEnabled();
        ModePagerAdapter pagerAdapter = new ModePagerAdapter(this, resultModesEnabled);
        binding.pager.setAdapter(pagerAdapter);

        new TabLayoutMediator(binding.pagerTabStrip, binding.pager, (tab, position) ->
                tab.setText(getTabTitle(resultModesEnabled, position))
        ).attach();

        final ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            //actionBar.setTitle(R.string.clipboard_activity_title);

            if (resultModesEnabled.length > 1) {
                actionBar.setElevation(0);
            } else {
                binding.pagerTabStrip.setVisibility(View.GONE);
            }
        }
    }

    private String getTabTitle(ClipboardResultMode[] resultModesEnabled, int position) {
        switch (resultModesEnabled[position]) {
            case GENERAL_RESULT:
                return "Multiple results";
            case SINGLE_RESULT:
                return "Single result";
            case PERFECT_IV_RESULT:
                return "Perfect IV result";
            default:
                return null;
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.clipboard_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.save) {
            AppCompatActivity activity = ((AppCompatActivity) getActivity());
            for (Fragment f : getChildFragmentManager().getFragments()) {
                if (f instanceof ClipboardModifierChildFragment) {
                    ((ClipboardModifierChildFragment) f).saveConfiguration();
                }
            }
            if (GoIVSettings.getInstance(activity).shouldCopyToClipboard()) {
                Toast.makeText(activity, R.string.configuration_saved, Toast.LENGTH_LONG).show();
            } else {
                // User saved clipboard configuration but the feature is disabled
                String text = getString(R.string.configuration_saved_feature_disabled,
                        getString(R.string.copy_to_clipboard_setting_title));
                Toast.makeText(activity, text, Toast.LENGTH_LONG).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Sets the description for the selected token.
     */
    public void updateTokenDescription(ClipboardToken selectedToken) {
        if (selectedToken == null) {
            binding.clipboardDescription.setText(R.string.no_token_selected);
        } else if (selectedToken.maxEv) {
            binding.clipboardDescription.setText(getResources().getString(R.string.token_max_evolution,
                    selectedToken.getLongDescription(getContext())));
        } else { // Selected token not max evolution
            binding.clipboardDescription.setText(selectedToken.getLongDescription(getContext()));
        }
    }

    private ClipboardResultMode[] getResultModesEnabled() {
        GoIVSettings settings = GoIVSettings.getInstance(getContext());
        EnumSet<ClipboardResultMode> clipboardResultModes = EnumSet.of(GENERAL_RESULT);

        if (settings.shouldCopyToClipboardSingle()) {
            clipboardResultModes.add(SINGLE_RESULT);
        }
        if (settings.shouldCopyToClipboardPerfectIV()) {
            clipboardResultModes.add(PERFECT_IV_RESULT);
        }

        return clipboardResultModes.toArray(new ClipboardResultMode[0]);
    }

    private static class ModePagerAdapter extends FragmentStateAdapter {

        final ClipboardResultMode[] resultModesEnabled;

        ModePagerAdapter(Fragment fragment, ClipboardResultMode[] resultModesEnabled) {
            super(fragment);
            this.resultModesEnabled = resultModesEnabled;
        }

        @Override
        public int getItemCount() {
            return resultModesEnabled.length;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return ClipboardModifierChildFragment.newInstance(resultModesEnabled[position]);
        }
    }

}
