package com.spryfieldsoftwaresolutions.android.photogallery;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.util.List;

import static android.app.Activity.RESULT_OK;
import static android.content.ContentValues.TAG;

/**
 * Created by Adam Baxter on 01/03/18.
 * <p>
 * Class that polls Flickr for search results using JobService for devices
 * on Lollipop and above.
 */

public class PollJobService extends JobService {
    private static final String TAG = "PollJobService";
    public static final String ACTION_SHOW_NOTIFICATION =
            "com.spryfieldsoftwaresolutions.android.photogallery.SHOW_NOTIFICATION";
    public static final String PERM_PRIVATE = "com.spryfieldsoftwaresolutions.android.photogallery.PRIVATE";
    public static final String REQUEST_CODE = "REQUEST_CODE";
    public static final String NOTIFICATION = "NOTIFICATION";

    private PollTask mCurrentTask;

    @Override
    public boolean onStartJob(JobParameters params) {
        mCurrentTask = new PollTask();
        mCurrentTask.execute(params);
        // Log.e(TAG, "mCurrentTask executed");
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if (mCurrentTask != null) {
            mCurrentTask.cancel(true);
        }
        return false;
    }

    private class PollTask extends AsyncTask<JobParameters, Void, List<GalleryItem>> {

        @Override
        protected List<GalleryItem> doInBackground(JobParameters... params) {
            JobParameters jobParams = params[0];

            Log.i(TAG, "Poll FLickr for new images");
            String query = QueryPreferences.getStoredQuery(PollJobService.this);
            List<GalleryItem> items;

            if (query == null) {
                Log.i(TAG, "Query == null");
                items = new FlickrFetchr().fetchRecentPhotos();
            } else {
                Log.i(TAG, "Query != null");
                items = new FlickrFetchr().searchPhotos(query);
            }
            jobFinished(jobParams, false);
            return items;
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            Log.e(TAG, "POLLJOBSERVICE----------------");

            if (items.size() == 0) {
                return;
            }

            String lastResultId = QueryPreferences.getLastResultId(PollJobService.this);
            String resultId = items.get(0).getId();

            if (resultId.equals(lastResultId)) {
                Log.e(TAG, "Got an old result: " + resultId);
            } else {
                Log.e(TAG, "Got a new result: " + resultId);

                String channelId = "PhotoGalleryChannel";
                Resources resources = getResources();
                Intent i = PhotoGalleryActivity.newIntent(PollJobService.this);
                PendingIntent pi = PendingIntent.getActivity(PollJobService.this, 0, i, 0);

                NotificationManager mNotificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                //sendBroadcast(new Intent(ACTION_SHOW_NOTIFICATION), PERM_PRIVATE);

                NotificationChannel mChannel;
                int importance = NotificationManager.IMPORTANCE_DEFAULT;

                NotificationCompat.Builder builder = new NotificationCompat.Builder(PollJobService.this, channelId)
                        .setTicker(resources.getString(R.string.new_pictures_title))
                        .setSmallIcon(android.R.drawable.ic_menu_report_image)
                        .setContentText(resources.getString(R.string.new_pictures_text))
                        .setContentIntent(pi);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mChannel = new NotificationChannel(channelId, PollJobService.this.getString(R.string.app_name), importance);
                    mChannel.setDescription("notification");
                    mChannel.enableLights(true);
                    mChannel.setLightColor(Color.BLUE);
                    mChannel.enableVibration(true);
                    mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
                    mNotificationManager.createNotificationChannel(mChannel);
                } else {
                    builder.setContentTitle(resources.getString(R.string.new_pictures_title))
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                }
                builder.setChannelId(channelId)
                        .setAutoCancel(true);
                Notification notification = builder.build();
                //      Log.e(TAG, "NOTIFICATION: " + notification);
                // mNotificationManager.notify(0, builder.build());
                showBackgroundNotification(0, notification);

            }

            QueryPreferences.setLastResultId(PollJobService.this, resultId);
        }

        private void showBackgroundNotification(int requestCode, Notification notification) {
            // Log.e(TAG, "REQUESTCODE: " + requestCode + "\nNOTIFICATION 2: " + notification);
            Intent i = new Intent(ACTION_SHOW_NOTIFICATION);
            i.putExtra(REQUEST_CODE, requestCode);
            i.putExtra(NOTIFICATION, notification);
            sendOrderedBroadcast(i, PERM_PRIVATE, null, null,
                    RESULT_OK, null, null);
        }
    }


}
