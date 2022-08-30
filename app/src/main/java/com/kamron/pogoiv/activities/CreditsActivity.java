package com.kamron.pogoiv.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.kamron.pogoiv.R;
import com.kamron.pogoiv.databinding.ActivityCreditsBinding;


public class CreditsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityCreditsBinding binding = ActivityCreditsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.app_credits_title);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
    }
}
