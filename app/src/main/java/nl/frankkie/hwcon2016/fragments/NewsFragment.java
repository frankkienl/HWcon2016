package nl.frankkie.hwcon2016.fragments;


import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import nl.frankkie.hwcon2016.adapters.NewsListAdapter;
import nl.frankkie.hwcon2016.R;
import nl.frankkie.hwcon2016.data.EventContract;
import nl.frankkie.hwcon2016.util.Util;

/**
 * Created by FrankkieNL on 1-1-2016.
 */
public class NewsFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>, SwipeRefreshLayout.OnRefreshListener {

    /*
    Show list of News articles
     */

    int NEWS_LOADER = 0;
    private NewsListAdapter mNewsListAdapter;
    private ListView mListView;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    public static final int COL_ID = 0;
    public static final int COL_TITLE = 1;
    public static final int COL_IMAGE = 2;
    public static final int COL_URL = 3;
    //https://developer.chrome.com/multidevice/android/customtabs
    private static final String EXTRA_CUSTOM_TABS_SESSION = "android.support.customtabs.extra.SESSION";
    private static final String EXTRA_CUSTOM_TABS_TOOLBAR_COLOR = "android.support.customtabs.extra.TOOLBAR_COLOR";

    public static final String[] NEWS_COLUMNS = {
            EventContract.NewsEntry.TABLE_NAME + "." + EventContract.NewsEntry._ID,
            EventContract.NewsEntry.COLUMN_NAME_TITLE,
            EventContract.NewsEntry.COLUMN_NAME_IMAGE,
            EventContract.NewsEntry.COLUMN_NAME_URL
    };

    Handler handler = new Handler();

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri = Uri.parse("content://nl.frankkie.hwcon2016/news/");
        CursorLoader cl = new CursorLoader(getActivity(), uri, NEWS_COLUMNS, null, null, "_id DESC");
        return cl;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mNewsListAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mNewsListAdapter.swapCursor(null);
    }

    //mandatory empty constuctor
    public NewsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_news_list, container, false);
        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mNewsListAdapter = new NewsListAdapter(getActivity(), null, 0);
        mListView = getListView();
        mListView.setAdapter(mNewsListAdapter);
        mListView.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        //start browser with URL
                        Cursor c = (Cursor) mNewsListAdapter.getItem(position);
                        String url = c.getString(COL_URL);
                        /////
                        
                        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                        builder.setShowTitle(true);
                        if (Build.VERSION.SDK_INT >= 23) {
                            builder.setToolbarColor(getResources().getColor(R.color.actionbar_background, null));
                        } else {
                            builder.setToolbarColor(getResources().getColor(R.color.actionbar_background));
                        }
                        builder.build().launchUrl(getActivity(), Uri.parse(url));
                    }
                }
        );

        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swiperefresh);
        mSwipeRefreshLayout.setOnRefreshListener(this);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(NEWS_LOADER, null, this);
    }

    @Override
    public void onRefresh() {
        Util.syncConventionData(getActivity());
        //I have no callback when the Sync is done, so just remove after 2 seconds..
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mSwipeRefreshLayout.setRefreshing(false);
            }
        }, 2000);
    }
}
