/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.wespot.pim.controller;

import android.annotation.TargetApi;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.*;
import android.view.ViewGroup.LayoutParams;
import android.widget.*;
import daoBase.DaoConfiguration;
import net.wespot.pim.BuildConfig;
import net.wespot.pim.R;
import net.wespot.pim.utils.images.ImageCache.ImageCacheParams;
import net.wespot.pim.utils.images.ImageFetcher;
import net.wespot.pim.utils.images.Utils;
import net.wespot.pim.utils.layout.RecyclingImageView;
import org.celstec.arlearn2.android.delegators.ARL;
import org.celstec.arlearn2.android.events.FileDownloadStatus;
import org.celstec.arlearn2.android.events.ResponseEvent;
import org.celstec.arlearn2.android.listadapter.AbstractResponsesLazyListAdapter;
import org.celstec.arlearn2.android.listadapter.ListItemClickInterface;
import org.celstec.dao.gen.GeneralItemLocalObject;
import org.celstec.dao.gen.ResponseLocalObject;

/**
 * The main fragment that powers the ImageGridActivity screen. Fairly straight forward GridView
 * implementation with the key addition being the ImageWorker class w/ImageCache to load children
 * asynchronously, keeping the UI nice and smooth and caching thumbnails for quick retrieval. The
 * cache is retained over configuration changes like orientation change so the images are populated
 * quickly if, for example, the user rotates the device.
 */
public class    ImageGridFragment extends Fragment implements AdapterView.OnItemClickListener, ListItemClickInterface<ResponseLocalObject> {
    private static final String TAG = "ImageGridFragment";
    private static final String IMAGE_CACHE_DIR = "thumbs";

    private int mImageThumbSize;
    private int mImageThumbSpacing;
    private ImageAdapter mAdapter;
    private ImageFetcher mImageFetcher;

    private GeneralItemLocalObject giLocalObject;

    /**
     * Empty constructor as per the Fragment documentation
     */
    public ImageGridFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        ARL.eventBus.register(this);

        Bundle extras = getArguments();

        giLocalObject = DaoConfiguration.getInstance().getGeneralItemLocalObjectDao().load(extras.getLong("generalItemId"));
        mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
        mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

        mAdapter = new ImageAdapter(getActivity(), extras.getLong("generalItemId"));
        mAdapter.setOnListItemClickCallback(this);

        ImageCacheParams cacheParams = new ImageCacheParams(getActivity(), IMAGE_CACHE_DIR);

        cacheParams.setMemCacheSizePercent(0.25f); // Set memory cache to 25% of app memory

        // The ImageFetcher takes care of loading images into our ImageView children asynchronously
        mImageFetcher = new ImageFetcher(getActivity(), mImageThumbSize);
        mImageFetcher.setLoadingImage(R.drawable.empty_photo);
        mImageFetcher.addImageCache(getActivity().getSupportFragmentManager(), cacheParams);
    }


    private void onEventAsync(final FileDownloadStatus fileDownloadStatus){
//        Log.e(TAG, fileDownloadStatus.getFileName()+" "
//                +fileDownloadStatus.getBytesDownloaded()+"/"+fileDownloadStatus.getAmountOfBytes());
//        if (fileDownloadStatus.getStatus() == FileDownloadStatus.FINISHED){
            // Run code on UIThread
//            getActivity().runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    Log.e(TAG, fileDownloadStatus.getFileName()+" - downloaded");
//                    mAdapter.notifyDataSetChanged();
//                }
//            });
//        }
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View v = inflater.inflate(R.layout.fragment_image_grid, container, false);
        final GridView mGridView = (GridView) v.findViewById(R.id.gridView);
        mGridView.setAdapter(mAdapter);
        mGridView.setOnItemClickListener(this);
        mGridView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int scrollState) {
                // Pause fetcher to ensure smoother scrolling when flinging
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
                    // Before Honeycomb pause image loading on scroll to help with performance
                    if (!Utils.hasHoneycomb()) {
                        mImageFetcher.setPauseWork(true);
                    }
                } else {
                    mImageFetcher.setPauseWork(false);
                }
            }

            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem,
                    int visibleItemCount, int totalItemCount) {
            }
        });

        // This listener is used to get the final width of the GridView and then calculate the
        // number of columns and the width of each column. The width of each column is variable
        // as the GridView has stretchMode=columnWidth. The column width is used to set the height
        // of each view so we get nice square thumbnails.
        mGridView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (mAdapter.getNumColumns() == 0) {
                            final int numColumns = (int) Math.floor(
                                    mGridView.getWidth() / (mImageThumbSize + mImageThumbSpacing));
                            if (numColumns > 0) {
                                final int columnWidth =
                                        (mGridView.getWidth() / numColumns) - mImageThumbSpacing;
                                mAdapter.setNumColumns(numColumns);
                                mAdapter.setItemHeight(columnWidth);
                                if (BuildConfig.DEBUG) {
                                    Log.d(TAG, "onCreateView - numColumns set to " + numColumns);
                                }
                                if (Utils.hasJellyBean()) {
                                    mGridView.getViewTreeObserver()
                                            .removeOnGlobalLayoutListener(this);
                                } else {
                                    mGridView.getViewTreeObserver()
                                            .removeGlobalOnLayoutListener(this);
                                }
                            }
                        }
                    }
                });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        mImageFetcher.setExitTasksEarly(false);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        super.onPause();
        mImageFetcher.setPauseWork(false);
        mImageFetcher.setExitTasksEarly(true);
        mImageFetcher.flushCache();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ARL.eventBus.unregister(this);
        mImageFetcher.closeCache();
    }

    @TargetApi(VERSION_CODES.JELLY_BEAN)
    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        final Intent intent = new Intent(getActivity(), ImageDetailActivity.class);

        intent.putExtra("DataCollectionTaskGeneralItemId", giLocalObject.getId());
        intent.putExtra(ImageDetailActivity.EXTRA_IMAGE, (int) id);

        if (Utils.hasJellyBean()) {
            // makeThumbnailScaleUpAnimation() looks kind of ugly here as the loading spinner may
            // show plus the thumbnail image in GridView is cropped. so using
            // makeScaleUpAnimation() instead.
            ActivityOptions options =
                    ActivityOptions.makeScaleUpAnimation(v, 0, 0, v.getWidth(), v.getHeight());
            getActivity().startActivity(intent, options.toBundle());
        } else {
            startActivity(intent);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        inflater.inflate(R.menu.main_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    private void onMainThread(ResponseEvent responseEvent){
        Log.e(TAG, "Responses synchronized");
        mAdapter.notifyDataSetChanged();

    }

    @Override
    public void onListItemClick(View v, int position, ResponseLocalObject item) {
        final Intent intent = new Intent(getActivity(), ImageDetailActivity.class);

        intent.putExtra(ImageDetailActivity.GENERAL_ITEM_ID, item.getGeneralItemLocalObject().getId());
        intent.putExtra(ImageDetailActivity.EXTRA_IMAGE, item.getId());

        if (Utils.hasJellyBean()) {
            // makeThumbnailScaleUpAnimation() looks kind of ugly here as the loading spinner may
            // show plus the thumbnail image in GridView is cropped. so using
            // makeScaleUpAnimation() instead.
            ActivityOptions options =
                    ActivityOptions.makeScaleUpAnimation(v, 0, 0, v.getWidth(), v.getHeight());
            getActivity().startActivity(intent, options.toBundle());
        } else {
            startActivity(intent);
        }
    }

    @Override
    public boolean setOnLongClickListener(View v, int position, ResponseLocalObject object) {
        return false;
    }

    /**
     * The main adapter that backs the GridView. This is fairly standard except the number of
     * columns in the GridView is used to create a fake top row of empty views as we use a
     * transparent ActionBar and don't want the real top row of images to start off covered by it.
     */
    private class ImageAdapter extends AbstractResponsesLazyListAdapter {

        private final Context mContext;
        private int mItemHeight = 0;
        private int mNumColumns = 0;
        private int mActionBarHeight = 0;
        private GridView.LayoutParams mImageViewLayoutParams;

        public ImageAdapter(Context context, long id) {
            super(context, id);
            mContext = context;
            mImageViewLayoutParams = new GridView.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        }




//        @Override
//        public int getCount() {
//            // If columns have yet to be determined, return no items
//            if (getNumColumns() == 0) {
//                return 0;
//            }
//
//            // Size + number of columns for top empty row
//            return giLocalObject.getResponses().size() + mNumColumns;
//        }



//        @Override
//        public Object getItem(int position) {
//            return position < mNumColumns ?
//                    null : giLocalObject.getResponses().get(position - mNumColumns).getThumbnailUriAsString();
//        }
//
//        @Override
//        public long getItemId(int position) {
//            return position < mNumColumns ? 0 : position - mNumColumns;
//        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            // Two types of views, the normal ImageView and the top row of empty views
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return (position < mNumColumns) ? 1 : 0;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        public  View newView(Context context, ResponseLocalObject item, ViewGroup parent) {

//        }
//        @Override
//        public View getView(int position, View convertView, ViewGroup container) {
            // First check if this is the top row
//            if (position < mNumColumns) {
//                if (convertView == null) {
//                    convertView = new View(mContext);
//                }
//                // Set empty view with height of ActionBar
//                convertView.setLayoutParams(new AbsListView.LayoutParams(
//                        LayoutParams.MATCH_PARENT, mActionBarHeight));
//                return convertView;
//            }

            // Now handle the main ImageView thumbnails
            ImageView imageView;
//            if (convertView == null) { // if it's not recycled, instantiate and initialize
////                Log.e(TAG, "Convert view null");

                imageView = new RecyclingImageView(mContext);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setLayoutParams(mImageViewLayoutParams);
//            } else { // Otherwise re-use the converted view
////                Log.e(TAG, "Convert view no null");
//                imageView = (ImageView) convertView;
//            }

            // Check the height matches our calculated column width
            if (imageView.getLayoutParams().height != mItemHeight) {
                imageView.setLayoutParams(mImageViewLayoutParams);
            }


            // Finally load the image asynchronously into the ImageView, this also takes care of
            // setting a placeholder image while the background thread runs
//            Log.e(TAG, "Current grid element: "+DaoConfiguration.getInstance()
//                    .getResponseLocalObjectDao()
//                    ._queryGeneralItemLocalObject_Responses(generalItemId)
//                    .get(position - mNumColumns).getThumbnailUriAsString());


            return imageView;
        }

        @Override
        public void bindView(View imageView, Context context, ResponseLocalObject item) {
            mImageFetcher.loadImage(item.getThumbnailUriAsString(), ((ImageView)imageView));

        }



        /**
         * Sets the item height. Useful for when we know the column width so the height can be set
         * to match.
         *
         * @param height
         */
        public void setItemHeight(int height) {
            if (height == mItemHeight) {
                return;
            }
            mItemHeight = height;
            mImageViewLayoutParams =
                    new GridView.LayoutParams(LayoutParams.MATCH_PARENT, mItemHeight);
            mImageFetcher.setImageSize(height);
            notifyDataSetChanged();
        }

        public void setNumColumns(int numColumns) {
            mNumColumns = numColumns;
        }

        public int getNumColumns() {
            return mNumColumns;
        }
    }


}
