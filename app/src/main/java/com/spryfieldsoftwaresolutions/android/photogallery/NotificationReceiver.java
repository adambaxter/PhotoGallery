package com.spryfieldsoftwaresolutions.android.photogallery;

import android.app.Activity;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

/**
 * Created by Adam Baxter on 03/03/18.
 */

public class NotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationReceiver";

    @Override
    public void onReceive(Context c, Intent i) {
        Log.i(TAG, "received result: " + getResultCode());
        if (getResultCode() != Activity.RESULT_OK) {
            // A foreground activity cancelled the broadcast
            return;
        }

        int requestCode = i.getIntExtra(PollJobService.REQUEST_CODE, 0);
        Notification notification = (Notification)
                i.getParcelableExtra(PollJobService.NOTIFICATION);
        // Log.e(TAG, "NOTIFICATION:3 " + notification);

        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(c);
        //  Log.e(TAG, "REQUEST CODE: " + requestCode + "\nNOTIFICATION: " + notification);
        notificationManager.notify(requestCode, notification);
    }
}
