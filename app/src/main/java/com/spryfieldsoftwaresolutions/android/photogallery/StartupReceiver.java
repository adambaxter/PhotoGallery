package com.spryfieldsoftwaresolutions.android.photogallery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by Adam Baxter on 02/03/18.
 * <p>
 * Class to recieven broadcast once startup is complete.
 */

public class StartupReceiver extends BroadcastReceiver {
    private static final String TAG = "StartupReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Receiced broadcast intent: " + intent.getAction());
    }
}
