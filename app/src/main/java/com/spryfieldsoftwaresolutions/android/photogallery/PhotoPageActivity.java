package com.spryfieldsoftwaresolutions.android.photogallery;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.webkit.WebView;

/**
 * Created by Adam Baxter on 04/03/18.
 * <p>
 * Activity to launch a webview to view photos from the gallery
 */

public class PhotoPageActivity extends SingleFragmentActivity {

    public static Intent newIntent(Context context, Uri photoPageUri) {
        Intent i = new Intent(context, PhotoPageActivity.class);
        i.setData(photoPageUri);
        return i;
    }

    @Override
    protected Fragment createFragment() {
        return PhotoPageFragment.newInstance(getIntent().getData());
    }

    @Override
    public void onBackPressed() {
        WebView wv = findViewById(R.id.web_view);

        if (wv.canGoBack()) {
            wv.goBack();
        } else {
            super.onBackPressed();
        }

    }
}
