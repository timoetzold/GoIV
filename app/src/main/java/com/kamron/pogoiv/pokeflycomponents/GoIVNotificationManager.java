package com.kamron.pogoiv.pokeflycomponents;

import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import android.widget.Toast;

import com.kamron.pogoiv.GoIVSettings;
import com.kamron.pogoiv.Pokefly;
import com.kamron.pogoiv.R;
import com.kamron.pogoiv.ScreenGrabber;
import com.kamron.pogoiv.ScreenShotHelper;
import com.kamron.pogoiv.activities.MainActivity;
import com.kamron.pogoiv.activities.OcrCalibrationResultActivity;
import com.kamron.pogoiv.activities.SettingsActivity;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION;

/**
 * Created by johan on 2017-07-06.
 * <p>
 * An object which can display a running or paused notification for goiv
 */

public class GoIVNotificationManager {

    private static final int NOTIFICATION_REQ_CODE = 8959;

    private static final String NOTIFICATION_CHANNEL_ID = "8959";

    private static Pokefly pokefly;

    public static final String ACTION_RECALIBRATE_SCANAREA = "com.kamron.pogoiv.ACTION_RECALIBRATE_SCANAREA";

    public GoIVNotificationManager(Pokefly pokefly) {
        GoIVNotificationManager.pokefly = pokefly;
    }


    /**
     * Show a paused notification in the system notification tray.
     */
    public void showPausedNotification() {
        int updateCurrentImmutable = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

        // Open app action
        Intent openAppIntent = new Intent(pokefly, MainActivity.class);
        PendingIntent openAppPendingIntent = PendingIntent.getActivity(
                pokefly, 0, openAppIntent, updateCurrentImmutable);

        // Open settings action
        Intent startSettingAppIntent = new Intent(pokefly, SettingsActivity.class);
        PendingIntent startSettingsPendingIntent = PendingIntent.getActivity(
                pokefly, 0, startSettingAppIntent, updateCurrentImmutable);

        // Start pokefly action
        Intent startServiceIntent = new Intent(pokefly, MainActivity.class)
                .setAction(MainActivity.ACTION_START_POKEFLY);
        PendingIntent startServicePendingIntent = PendingIntent.getActivity(
                pokefly, 0, startServiceIntent, updateCurrentImmutable);

        // Build notification
        NotificationCompat.Builder notification = new NotificationCompat.Builder(pokefly, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(pokefly.getString(R.string.notification_title_goiv_stopped))
                .setContentText(pokefly.getString(R.string.notification_title_tap_to_open))
                .setColor(ContextCompat.getColor(pokefly, R.color.colorAccent))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setContentIntent(openAppPendingIntent)
                .addAction(R.drawable.ic_settings_24px, pokefly.getString(R.string.settings_page_title),
                        startSettingsPendingIntent)
                .addAction(R.drawable.ic_play_circle_outline_24px, pokefly.getString(R.string.main_start),
                        startServicePendingIntent)
                .setOngoing(false);

        NotificationManager notificationManager =
                (NotificationManager) pokefly.getSystemService(Context.NOTIFICATION_SERVICE);

        initNotificationChannel(notificationManager);

        notificationManager.notify(NOTIFICATION_REQ_CODE, notification.build());
    }

    /**
     * Show a running notification in the system notification tray.
     */
    public void showRunningNotification() {
        int updateCurrentImmutable = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

        // Open app action
        Intent openAppIntent = new Intent(pokefly, MainActivity.class);
        PendingIntent openAppPendingIntent = PendingIntent.getActivity(
                pokefly, 0, openAppIntent, updateCurrentImmutable);

        // Recalibrate action
        Intent recalibrateScreenScanningIntent = new Intent(pokefly, NotificationActionService.class)
                .setAction(ACTION_RECALIBRATE_SCANAREA);
        PendingIntent recalibrateScreenScanningPendingIntent = PendingIntent.getService(
                pokefly, 0, recalibrateScreenScanningIntent, updateCurrentImmutable);

        // Stop service action
        Intent stopServiceIntent = Pokefly.createStopIntent(pokefly);
        PendingIntent stopServicePendingIntent = PendingIntent.getService(
                pokefly, 0, stopServiceIntent, updateCurrentImmutable);

        // Build notification
        NotificationCompat.Builder notification = new NotificationCompat.Builder(pokefly, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon_play)
                .setContentTitle(pokefly.getString(R.string.notification_title, pokefly.getTrainerLevel()))
                .setContentText(pokefly.getString(R.string.notification_title_tap_to_open))
                .setColor(ContextCompat.getColor(pokefly, R.color.colorPrimary))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setContentIntent(openAppPendingIntent)
                .addAction(R.drawable.ic_recalibrate_24px, pokefly.getString(R.string.recalibrate_goiv_notification),
                        recalibrateScreenScanningPendingIntent)
                .addAction(R.drawable.ic_pause_circle_outline_24px, pokefly.getString(R.string.pause_goiv_notification),
                        stopServicePendingIntent)
                .setOngoing(true);

        NotificationManager notificationManager =
                (NotificationManager) pokefly.getSystemService(Context.NOTIFICATION_SERVICE);

        initNotificationChannel(notificationManager);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            pokefly.startForeground(NOTIFICATION_REQ_CODE, notification.build(),
                    FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            pokefly.startForeground(NOTIFICATION_REQ_CODE, notification.build());
        }
    }

    /**
     * The class which create channel notification for oreo.
     */
    private void initNotificationChannel(NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager
                .getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
            // Create notification channel
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    pokefly.getString(R.string.notification_channel), NotificationManager.IMPORTANCE_LOW);

            channel.setShowBadge(false);
            channel.enableLights(false);

            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * The class which receives the intent to recalibrate the scan area.
     */
    public static class NotificationActionService extends IntentService {
        public NotificationActionService() {
            super(NotificationActionService.class.getSimpleName());
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            String action = intent.getAction();
            if (ACTION_RECALIBRATE_SCANAREA.equals(action)) {
                Handler mainThreadHandler = new Handler(Looper.getMainLooper());

                if (GoIVSettings.getInstance(this).isManualScreenshotModeEnabled()) {
                    // Close the notification shade
                    sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
                    // Tell the user that the next screenshot will be used to recalibrate GoIV
                    ScreenShotHelper.sShouldRecalibrateWithNextScreenshot = true;
                    mainThreadHandler.post(() -> Toast.makeText(NotificationActionService.this,
                            R.string.ocr_calibration_screenshot_mode, Toast.LENGTH_LONG).show());

                } else { // Start calibration!
                    // Close the notification shade so we can screenshot pogo
                    sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));

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

                        OcrCalibrationResultActivity.startCalibration(NotificationActionService.this,
                                ScreenGrabber.getInstance().grabScreen(),
                                pokefly.getCurrentStatusBarHeight(),
                                pokefly.getCurrentNavigationBarHeight());
                    }, 4100);
                }
            }
        }
    }
}
