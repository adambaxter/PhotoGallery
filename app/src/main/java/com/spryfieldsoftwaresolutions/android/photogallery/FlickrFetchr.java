package com.spryfieldsoftwaresolutions.android.photogallery;

import android.net.Uri;
import android.util.Log;

import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static android.view.View.GONE;

/**
 * Created by Adam Baxter on 23/02/18.
 * <p>
 * Class to handle our networking ie fetching data from flickr
 */

public class FlickrFetchr {

    private static final String TAG = "FlickrFetchr";
    private static final String API_KEY = "ecc46c0e8a66a0c74afea80c7d51d187";
    static int mMaxPages = -1;
    static int mItemsPerPage = -1;
    static int mTotalItems = -1;
    static int mCurrentPage = 1;

    private static final String FETCH_RECENTS_METHOD = "flickr.photos.getRecent";
    private static final String SEARCH_METHOD = "flickr.photos.search";
    private static final Uri ENDPOINT = Uri
            .parse("https://api.flickr.com/services/rest/")
            .buildUpon()
            .appendQueryParameter("method", "flickr.photos.getRecent")
            .appendQueryParameter("api_key", API_KEY)
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
//            .appendQueryParameter("page", Integer.toString(mCurrentPage))
            .appendQueryParameter("extras", "url_s")
            .build();


    public byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() +
                        ": with " + urlSpec);
            }

            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    public String getUrlString(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }

    public List<GalleryItem> fetchRecentPhotos() {
        String url = buildUrl(FETCH_RECENTS_METHOD, null);
        return downloadGalleryItems(url);
    }

    public List<GalleryItem> searchPhotos(String query) {
        String url = buildUrl(SEARCH_METHOD, query);
        return downloadGalleryItems(url);
    }

    private List<GalleryItem> downloadGalleryItems(String url) {
        List<GalleryItem> items = new ArrayList<>();


        try {
            String jsonString = getUrlString(url);
            Log.e(TAG, "Recieved JSON: " + jsonString);

            //Challenge 1
            parseItems(items, jsonString);
            //JSONObject jsonBody = new JSONObject(jsonString);
            //parseItems(items, jsonBody);
            //mCurrentPage++;
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to fetch items", ioe);
        } catch (JsonParseException je) {
            Log.e(TAG, "Failed to parse JSON", je);
        }

        return items;
    }

    private String buildUrl(String method, String query) {
        Uri.Builder uriBuilder = ENDPOINT.buildUpon()
                .appendQueryParameter("method", method)
                .appendQueryParameter("page", Integer.toString(mCurrentPage));

        if (method.equals(SEARCH_METHOD)) {
            uriBuilder.appendQueryParameter("text", query);
        }

        //Log.e(TAG, "CURRENT PAGE----------- " + mCurrentPage);
        return uriBuilder.build().toString();
    }

    private void parseItems(List<GalleryItem> items, String jsonBody)
    /** throws IOException, JSONException **/
    {

        // Chanllenge 1
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(GalleryItem[].class, new ChallengeDeserializer())
                .create();

        GalleryItem[] photoList = gson.fromJson(jsonBody, GalleryItem[].class);

        for (GalleryItem item : photoList) {
            if (item.getUrl() != null) {
                items.add(item);
            }
        }

        // Log.e(TAG, "ITEMS: " + items.size());


        /** JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
         JSONArray photoJsonArray = photosJsonObject.getJSONArray("photo");

         for (int i = 0; i < photoJsonArray.length(); i++) {
         JSONObject photoJsonObject = photoJsonArray.getJSONObject(i);

         GalleryItem item = new GalleryItem();
         item.setId(photoJsonObject.getString("id"));
         item.setCaption(photoJsonObject.getString("title"));

         if (!photoJsonObject.has("url_s")) {
         continue;
         }

         item.setUrl(photoJsonObject.getString("url_s"));
         items.add(item);**/
    }


    class ChallengeDeserializer implements JsonDeserializer<GalleryItem[]> {

        @Override
        public GalleryItem[] deserialize(JsonElement je, Type type, JsonDeserializationContext jdc)
                throws JsonParseException {

            JsonElement photos = je.getAsJsonObject().get("photos");

            mMaxPages = photos.getAsJsonObject().get("pages").getAsInt();
            mItemsPerPage = photos.getAsJsonObject().get("perpage").getAsInt();
            mTotalItems = photos.getAsJsonObject().get("total").getAsInt();

            JsonElement photoArray = photos.getAsJsonObject().get("photo");
            Gson gson = new GsonBuilder()
                    .setFieldNamingStrategy(new ChallengeFieldNamingStrategy())
                    .create();

            return gson.fromJson(photoArray, GalleryItem[].class);


        }

    }

    class ChallengeFieldNamingStrategy implements FieldNamingStrategy {

        @Override
        public String translateName(Field f) {
            switch (f.getName()) {
                case "mId":
                    return "id";
                case "mCaption":
                    return "title";
                case "mUrl":
                    return "url_s";
                default:
                    return f.getName();
            }
        }
    }


}

