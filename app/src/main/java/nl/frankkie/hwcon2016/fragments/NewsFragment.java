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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.plus.Plus;

import org.acra.ACRA;

import nl.frankkie.hwcon2016.adapters.NewsListAdapter;
import nl.frankkie.hwcon2016.R;
import nl.frankkie.hwcon2016.data.EventContract;
import nl.frankkie.hwcon2016.util.Util;

/**
 * Created by FrankkieNL on 1-1-2016.
 */
public class NewsFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>, SwipeRefreshLayout.OnRefreshListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    //<editor-fold desc="Silent Google Play Games login">
    private GoogleApiClient mGoogleApiClient;

    public void initGoogleApi() {
        mGoogleApiClient = buildGoogleApiClient();
    }

    private GoogleApiClient buildGoogleApiClient() {
        return new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();
        try {
            mGoogleApiClient.connect();
        } catch (Exception e) {
            ACRA.getErrorReporter().handleException(e);
        }

    }

    @Override
    public void onStop() {
        super.onStop();
        try {
            if (mGoogleApiClient.isConnected()) {
                mGoogleApiClient.disconnect();
            }
        } catch (Exception e) {
            ACRA.getErrorReporter().handleException(e);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            mGoogleApiClient.connect();
        } catch (Exception e) {
            ACRA.getErrorReporter().handleException(e);
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {
        //silently ignore errors
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        //silently ignore errors
    }
    //</editor-fold>

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
                        //https://developer.chrome.com/multidevice/android/customtabs
                        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                        builder.setShowTitle(true);
                        if (Build.VERSION.SDK_INT >= 23) {
                            builder.setToolbarColor(getResources().getColor(R.color.actionbar_background, null));
                        } else {
                            builder.setToolbarColor(getResources().getColor(R.color.actionbar_background));
                        }
                        builder.build().launchUrl(getActivity(), Uri.parse(url));
                        //
                        if (mGoogleApiClient.isConnected()){
                            Games.Achievements.increment(mGoogleApiClient, getString(R.string.achievement_egghead), 1);
                        }
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

        initGoogleApi();
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

        if (mGoogleApiClient.isConnected()){
            Games.Achievements.unlock(mGoogleApiClient, getString(R.string.achievement_latest_news));
        }
    }
}
