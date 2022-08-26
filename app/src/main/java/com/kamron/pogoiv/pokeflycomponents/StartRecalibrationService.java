package com.kamron.pogoiv.pokeflycomponents;

import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.kamron.pogoiv.GoIVSettings;
import com.kamron.pogoiv.Pokefly;
import com.kamron.pogoiv.R;
import com.kamron.pogoiv.ScreenGrabber;
import com.kamron.pogoiv.ScreenShotHelper;
import com.kamron.pogoiv.activities.OcrCalibrationResultActivity;

/**
 * The class which receives the intent to recalibrate the scan area.
 */
public class StartRecalibrationService extends IntentService {

    public static final String ACTION_START_RECALIBRATION = "com.kamron.pogoiv.ACTION_START_RECALIBRATION";

    private static Pokefly pokefly;

    public static void setPokefly(Pokefly pokefly) {
        StartRecalibrationService.pokefly = pokefly;
    }

    public StartRecalibrationService() {
        super(StartRecalibrationService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        if (ACTION_START_RECALIBRATION.equals(action)) {
            openPokemonGoApp();

            Handler mainThreadHandler = new Handler(Looper.getMainLooper());

            if (GoIVSettings.getInstance(this).isManualScreenshotModeEnabled()) {
                // Tell the user that the next screenshot will be used to recalibrate GoIV
                ScreenShotHelper.sShouldRecalibrateWithNextScreenshot = true;
                mainThreadHandler.post(() -> Toast.makeText(StartRecalibrationService.this,
                        R.string.ocr_calibration_screenshot_mode, Toast.LENGTH_LONG).show());
            } else { // Start calibration!
                mainThreadHandler.post(() -> {
                    if (pokefly != null
                            && pokefly.getScreenWatcher() != null
                            && pokefly.getIvButton() != null) {
                        // Hide IV button: it might interfere
                        pokefly.getIvButton().setShown(false, false);
                        // Cancel pending quick IV previews: they might get the IV button to show again
                        pokefly.getScreenWatcher().cancelPendingScreenScan();
                    }
                });
                mainThreadHandler.post(() ->
                        Toast.makeText(pokefly, R.string.recalibrating_please_wait, Toast.LENGTH_SHORT).show()
                );
                mainThreadHandler.postDelayed(() -> {
                    if (ScreenGrabber.getInstance() == null) {
                        return; // Don't recalibrate when screen watching isn't running!!!
                    }

                    OcrCalibrationResultActivity.startCalibration(StartRecalibrationService.this,
                            ScreenGrabber.getInstance().grabScreen(),
                            pokefly.getCurrentStatusBarHeight(),
                            pokefly.getCurrentNavigationBarHeight());
                }, 4100);
            }
        }
    }

    /**
     * Runs a launch intent for Pokemon GO.
     */
    private void openPokemonGoApp() {
        Intent i = getPackageManager().getLaunchIntentForPackage("com.nianticlabs.pokemongo");
        if (i != null) {
            i.addFlags(FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
        }
    }
}