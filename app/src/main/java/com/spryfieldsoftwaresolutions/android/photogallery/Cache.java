package com.spryfieldsoftwaresolutions.android.photogallery;


import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

/**
 * Created by Adam Baxter on 26/02/18.
 * <p>
 * Class to handle Cacheing images using LruCache
 */

public class Cache {

    private static Cache instance;
    private LruCache<Object, Object> lru;

    // Get max available memory, exceedign this amt will throw OutOfMemory exception
    // stored in kB
    final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

    //use 1/8th of avail memory
    final int cacheSize = maxMemory / 8;

    private Cache() {

        lru = new LruCache<Object, Object>(cacheSize);
    }

    public static Cache getInstance() {
        if (instance == null) {
            instance = new Cache();
        }
        return instance;
    }

    public LruCache<Object, Object> getLru() {
        return lru;
    }

    public static void saveBitmapToCache(String url, Bitmap bitmap) {
        if (retrieveBitmapFromCache(url) == null) {
            Cache.getInstance().getLru().put(url, bitmap);
        }
    }

    public static Bitmap retrieveBitmapFromCache(String url) {

        Bitmap bitmap = (Bitmap) Cache.getInstance().getLru().get(url);

        return bitmap;
    }

    public static void clearCache() {
        Cache.getInstance().getLru().evictAll();
    }

}
