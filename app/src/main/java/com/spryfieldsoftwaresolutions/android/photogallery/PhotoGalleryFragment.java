package com.spryfieldsoftwaresolutions.android.photogallery;


import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.widget.AbsListView.OnScrollListener.SCROLL_STATE_IDLE;
import static com.spryfieldsoftwaresolutions.android.photogallery.FlickrFetchr.mCurrentPage;
import static com.spryfieldsoftwaresolutions.android.photogallery.FlickrFetchr.mItemsPerPage;
import static com.spryfieldsoftwaresolutions.android.photogallery.FlickrFetchr.mMaxPages;
import static com.spryfieldsoftwaresolutions.android.photogallery.FlickrFetchr.mTotalItems;


/**
 * Created by Adam Baxter on 23/02/18.
 */

public class PhotoGalleryFragment extends VisibleFragment {

    private static final String TAG = "PhotoGalleryFragment";
    private static final int COLUMN_SIZE = 400;

    private RecyclerView mPhotoRecyclerView;
    public static ProgressBar mProgressBar;
    private TextView mCurrentPageView;
    private LinearLayoutManager mGridlayoutManager;
    private List<GalleryItem> mItems = new ArrayList<>();
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;
    private int mGridColumns;
    private RecyclerView.SmoothScroller mSmoothScroller;
    private static final int JOB_ID = 1;


    private FlickrFetchr mFlickrFetchr = new FlickrFetchr();
    Boolean amLoading = false;


    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        updateItems();

        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setTThumbnailDownloadListener(
                new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
                    @Override
                    public void onThumbnailDownloaded(PhotoHolder photoHolder, Bitmap bitmap) {
                        mProgressBar.setVisibility(View.GONE);
                        Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                        photoHolder.bindDrawable(drawable);
                    }
                }
        );
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "Background thread started");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInsanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mPhotoRecyclerView = v.findViewById(R.id.photo_recycler_view);
        // mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));

        //  mCurrentPageView = v.findViewById(R.id.current_page_view);
        mProgressBar = v.findViewById(R.id.indeterminateBar);
        mProgressBar.setVisibility(View.GONE);

        mPhotoRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mPhotoRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        int width = mPhotoRecyclerView.getWidth();
                        mGridColumns = width / COLUMN_SIZE;

                        mGridlayoutManager = new GridLayoutManager(getActivity(), mGridColumns);
                        mPhotoRecyclerView.setLayoutManager(mGridlayoutManager);
                        setupAdapter();
                    }
                }
        );

        mGridlayoutManager = (LinearLayoutManager) mPhotoRecyclerView.getLayoutManager();

        mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                //  super.onScrolled(recyclerView, dx, dy);
                if (dy > 0 || dy < 0) {
                    //  Log.e(TAG, "LAST VISIBLE POSTITION: " + mGridlayoutManager.findLastVisibleItemPosition() +
                    //    "\nITEMS: " + mItems.size() + "\namLoading: " + amLoading + "\nCURRENTPAGE: " + mCurrentPage + "\nMAXPAGES: " + mMaxPages);

                    //Scrolling up or down
                    if ((!amLoading) && (dy > 0) &&
                            mGridlayoutManager.findLastVisibleItemPosition() + 1 >= mItems.size() && mCurrentPage < mMaxPages) {
                        // We have scrolled to the last row of data. need to fetch more
                        Log.i(TAG, "Fetching more items");
                        amLoading = true;
                        new FetchItemsTask(QueryPreferences.getStoredQuery(getActivity())).execute();
                        mCurrentPage++;
                    } else {
                        //Make sure page value is correct
                        int firstVisibleItem = mGridlayoutManager.findFirstVisibleItemPosition();
                        int calcPage = 0;
                        if (firstVisibleItem < mItemsPerPage) {
                            calcPage = 1;
                        } else {
                            calcPage = (firstVisibleItem / mItemsPerPage) +
                                    (firstVisibleItem % mItemsPerPage == 0 ? 0 : 1);
                        }

                        if (calcPage != mCurrentPage) {
                            mCurrentPage = calcPage;
                        }
                        //   setCurrentPageView(firstVisibleItem);
                    }
                }

                /**   if ( dy == 0) {


                 }**/
            }

            @Override
            public void onScrollStateChanged(RecyclerView view, int scrollState) {
                if (scrollState == SCROLL_STATE_IDLE) {
                    Log.i(TAG, "No Longer Scrolling");
                    int firstVisible = mGridlayoutManager.findFirstVisibleItemPosition();
                    int lastVisible = mGridlayoutManager.findLastVisibleItemPosition();

                    for (int i = firstVisible; i > firstVisible - 10 && i > 0; i--) {

                        mThumbnailDownloader.preloadImage(mItems.get(i).getUrl());
                        //Log.e(TAG, "IMAGE AT POSITION: " + i + " SENT FOR PRELOADING. URL: " + mItems.get(i).getUrl());

                    }

                    for (int i = lastVisible; i < lastVisible + 25 && i < mItems.size(); i++) {
                        mThumbnailDownloader.preloadImage(mItems.get(i).getUrl());
                        //Log.e(TAG, "IMAGE AT POSITION: " + i + " SENT FOR PRELOADING. URL: " + mItems.get(i).getUrl());

                    }
                }
            }
        });
        setupAdapter();

        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        Cache.clearCache();
        Log.i(TAG, "Background thread destroyed and cache cleared");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.fragment_photo_gallery, menu);

        final MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                //  Log.e(TAG, "QueryTextSubmit: " + s);
                QueryPreferences.setStoredQuery(getActivity(), s);
                searchView.clearFocus();
                searchView.setQuery("", false);
                searchView.setIconified(true);
                searchView.setIconified(true);
                mProgressBar.setVisibility(View.VISIBLE);
                clearGallery();
                updateItems();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                //Log.e(TAG, "QueryTextChange: " + s);
                return false;
            }
        });

        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = QueryPreferences.getStoredQuery(getActivity());
                searchView.setQuery(query, false);
            }
        });

        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
        if (PollService.isServiceAlarmOn(getActivity()) || isJobScheduled(JOB_ID)) {
            toggleItem.setTitle(R.string.stop_polling);
        } else {
            toggleItem.setTitle(R.string.start_polling);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_clear:
                clearGallery();
                QueryPreferences.setStoredQuery(getActivity(), null);
                updateItems();
                return true;
            case R.id.menu_item_toggle_polling:
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
                    PollService.setServiceAlarm(getActivity(), shouldStartAlarm);
                } else {
                    JobScheduler scheduler = (JobScheduler)
                            getActivity().getSystemService(Context.JOB_SCHEDULER_SERVICE);
                    final int JOB_ID = 1;
                    if (isJobScheduled(JOB_ID)) {
                        Log.i(TAG, "scheduler.cancel(JOB_ID)");
                        scheduler.cancel(JOB_ID);
                        //   Log.e(TAG, "PENDING JOBS: " + scheduler.getAllPendingJobs());

                    } else {
                        Log.i(TAG, "scheduler.schedule(jobInfo)");
                        JobInfo jobInfo = new JobInfo.Builder(
                                JOB_ID, new ComponentName(getActivity(), PollJobService.class))
                                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                                .setPeriodic(1000 * 60 * 15)
                                .setPersisted(true)
                                .build();
                        scheduler.schedule(jobInfo);
                        // Log.e(TAG, "PENDING JOBS: " + scheduler.getAllPendingJobs());
                    }
                }
                //  Log.e(TAG, "Polling toggled");
                getActivity().invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean isJobScheduled(int jobId) {
        JobScheduler scheduler = (JobScheduler)
                getActivity().getSystemService(Context.JOB_SCHEDULER_SERVICE);
        boolean hasBeenScheduled = false;
        for (JobInfo jobInfo : scheduler.getAllPendingJobs()) {
            if (jobInfo.getId() == jobId) {
                hasBeenScheduled = true;
            }
        }
        return hasBeenScheduled;
    }

    private void updateItems() {
        String query = QueryPreferences.getStoredQuery(getActivity());
        new FetchItemsTask(query).execute();
    }

    public void clearGallery() {
        mCurrentPage = 1;
        mItems.clear();
        mThumbnailDownloader.clearQueue();
        mPhotoRecyclerView.getAdapter().notifyDataSetChanged();
    }


    /**
     * private void setCurrentPageView() {
     * setCurrentPageView(-1);
     * }
     * <p>
     * private void setCurrentPageView(int firstVisibleItem) {
     * if (firstVisibleItem == -1) {
     * firstVisibleItem = mGridlayoutManager.findFirstVisibleItemPosition();
     * }
     * mCurrentPageView.setText("Current Fetched Page: " + mCurrentPage +
     * " of " + ((mMaxPages ==0) ? "<unknown>" : mMaxPages) + ", " +
     * ((mItemsPerPage==0) ? "<unknown>" : mItemsPerPage) + " items per page " +
     * ((mTotalItems==0)?"<unknown>":mTotalItems) + "total items, you've scrolled past: " +
     * (firstVisibleItem <=0 ? 0 : firstVisibleItem) + " items. Last Visible Position: " + mGridlayoutManager.findLastVisibleItemPosition());
     * <p>
     * }
     **/

    private void setupAdapter() {
        if (isAdded()) {
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {
        private ImageView mItemImageView;

        public PhotoHolder(View itemView) {
            super(itemView);

            mItemImageView = (ImageView) itemView.findViewById(R.id.item_image_view);
        }

        public void bindDrawable(Drawable drawable) {
            mItemImageView.setImageDrawable(drawable);
        }
    }

    private class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>> {
        private String mQuery;

        public FetchItemsTask(String query) {
            mQuery = query;
        }

        @Override
        protected List<GalleryItem> doInBackground(Void... params) {

            //String query = "beach girl"; //just for testing

            if (mQuery == null) {
                return new FlickrFetchr().fetchRecentPhotos();
            } else {
                return mFlickrFetchr.searchPhotos(mQuery);
            }
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            if (mItems.size() == 0) {
                mItems.addAll(items);
                setupAdapter();
                //  setCurrentPageView();
            } else {
                final int oldSize = mItems.size();
                mItems.addAll(items);
                mPhotoRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mPhotoRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        //Scroll to first row of newly added set
                        mPhotoRecyclerView.smoothScrollToPosition(oldSize);
                        //     setCurrentPageView();
                        amLoading = false;
                    }
                });
                mPhotoRecyclerView.getAdapter().notifyDataSetChanged();
            }
        }


    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {
        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.list_item_gallery, viewGroup, false);

            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder photoHolder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);
            Drawable placeholder = getResources().getDrawable(R.drawable.placeholder);
            photoHolder.bindDrawable(placeholder);
            if (Cache.retrieveBitmapFromCache(galleryItem.getUrl()) == null) {
                mThumbnailDownloader.queueThumbnail(photoHolder, galleryItem.getUrl());
                Log.i(TAG, "No Cached bitmap");
            } else {
                Drawable drawable = new BitmapDrawable(getResources(), Cache.retrieveBitmapFromCache(galleryItem.getUrl()));
                photoHolder.bindDrawable(drawable);
                Log.i(TAG, "Loaded cached bitmap");
            }
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }

}
