package com.spryfieldsoftwaresolutions.android.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by Adam Baxter on 26/02/18.
 * Class to help with downloading thumbnails from flickr
 */

public class ThumbnailDownloader<T> extends HandlerThread {
    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;
    private static final int MESSAGE_PRELOAD = 1;

    private boolean mHasQuit = false;
    private Handler mRequestHandler;
    private Handler mResponseHandler;
    private ConcurrentMap<T, String> mRequestMap = new ConcurrentHashMap<>();
    private ThumbnailDownloadListener<T> mTThumbnailDownloadListener;

    public interface ThumbnailDownloadListener<T> {
        void onThumbnailDownloaded(T target, Bitmap thumbnail);
    }

    public void setTThumbnailDownloadListener(ThumbnailDownloadListener<T> listener) {
        mTThumbnailDownloadListener = listener;
    }


    public ThumbnailDownloader(Handler responseHandler) {
        super(TAG);
        mResponseHandler = responseHandler;
    }

    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    T target = (T) msg.obj;
                    // Log.e(TAG, "Got a request for URL: " + mRequestMap.get(target));
                    handleRequest(target);
                } else if (msg.what == MESSAGE_PRELOAD) {
                    String url = (String) msg.obj;
                    downloadImg(url);
                    // Log.e(TAG, "Got a request to preload URL: " + url );
                }
            }
        };
    }

    @Override
    public boolean quit() {
        mHasQuit = true;
        return super.quit();
    }

    public void preloadImage(String url) {
        //Log.e(TAG, "Got a URL to PRELOAD: " + url);
        mRequestHandler.obtainMessage(MESSAGE_PRELOAD, url).sendToTarget();
    }

    public void queueThumbnail(T target, String url) {
        // Log.i(TAG, "Got a URL: " + url);

        if (url == null) {
            mRequestMap.remove(target);
        } else {
            mRequestMap.put(target, url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target)
                    .sendToTarget();
        }
    }

    public void clearQueue() {
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
        mRequestHandler.removeMessages(MESSAGE_PRELOAD);
        mRequestMap.clear();
    }

    private void handleRequest(final T target) {
        final String url = mRequestMap.get(target);
        final Bitmap bitmap;

        if (url == null) {
            return;
        }

        bitmap = downloadImg(url);
        //Log.e(TAG, "URL: " + url);
        mResponseHandler.post(new Runnable() {
            public void run() {
                Log.e(TAG, "mRequestMap: " + (mRequestMap == null) + "\nTARGET:" + (target == null) + "\nurl:" + url + "\nmHasQuit:" + mHasQuit);

                if ((mRequestMap != null && !(url.equals(mRequestMap.get(target)))) || mHasQuit) {
                    //if ((mRequestMap != null && !(mRequestMap.get(target).equals(url))) || mHasQuit) {
                    return;
                }
                mRequestMap.remove(target);
                mTThumbnailDownloadListener.onThumbnailDownloaded(target, bitmap);
            }
        });

    }

    private Bitmap downloadImg(String url) {
        final Bitmap bitmap;

        if (url == null) {
            //  Log.e(TAG, "No Image downloaded, url is null");
            return null;
        }

        if (Cache.retrieveBitmapFromCache(url) != null) {
            bitmap = Cache.retrieveBitmapFromCache(url);
            // Log.i(TAG, "Bitmap retrieved from Cache");
            return bitmap;
        } else {
            try {
                byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
                bitmap = BitmapFactory
                        .decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
                //     Log.i(TAG, "Bitmap created");

                Cache.saveBitmapToCache(url, bitmap);
                //  Log.i(TAG, "Bitmap cached");

                return bitmap;
            } catch (IOException ioe) {
                Log.e(TAG, "Error downloading image", ioe);
                return null;
            }
        }

    }
}

