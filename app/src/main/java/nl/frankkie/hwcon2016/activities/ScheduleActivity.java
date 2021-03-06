package nl.frankkie.hwcon2016.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.plus.Plus;

import org.acra.ACRA;

import nl.frankkie.hwcon2016.R;
import nl.frankkie.hwcon2016.fragments.EventDetailFragment;
import nl.frankkie.hwcon2016.fragments.EventListFragment;
import nl.frankkie.hwcon2016.fragments.ScheduleListFragment;
import nl.frankkie.hwcon2016.util.GoogleApiUtil;
import nl.frankkie.hwcon2016.util.Util;


/**
 * THIS IS LIKE A COPY OF EventListActivity
 * <p/>
 * An activity representing a list of Events. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link EventDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p/>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link EventListFragment} and the item details
 * (if present) is a {@link EventDetailFragment}.
 * <p/>
 * This activity also implements the required
 * {@link EventListFragment.Callbacks} interface
 * to listen for item selections.
 * <p/>
 * To use ActionBar and Fragments, extend from AppCompatActivity,
 * as AppCompatActivity supports Fragments.
 * http://stackoverflow.com/questions/18451575/action-bar-fragment-activity
 */
public class ScheduleActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiUtil.GiveMeGoogleApiClient,
        ScheduleListFragment.Callbacks {

    //<editor-fold desc="ActionBar Stuff">
    private NavigationView navigationView;
    private DrawerLayout drawerLayout;
    Activity thisAct = this;

    Toolbar mToolbar;
    ActionBarDrawerToggle mDrawerToggle;

    public void initToolbar() {
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(getTitle());
        setSupportActionBar(mToolbar);
        ///
        navigationView = (NavigationView) findViewById(R.id.navigation_drawer);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                Util.navigationItemSelected(thisAct, navigationView, drawerLayout, menuItem);
                return false;
            }
        });
        Util.fixNavigationView(this, navigationView);
        //
        mDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.setDrawerListener(mDrawerToggle);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    //</editor-fold>

    //<editor-fold desc="Silent Google Play Games login">
    private GoogleApiClient mGoogleApiClient;

    public void initGoogleApi() {
        mGoogleApiClient = buildGoogleApiClient();
    }

    private GoogleApiClient buildGoogleApiClient() {
        return new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            mGoogleApiClient.connect();
        } catch (Exception e) {
            ACRA.getErrorReporter().handleException(e);
        }

    }

    @Override
    protected void onStop() {
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data); //send to fragment
        try {
            mGoogleApiClient.connect();
        } catch (Exception e) {
            ACRA.getErrorReporter().handleException(e);
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        //empty, add achievements later.
    }

    @Override
    public void onConnectionSuspended(int i) {
        //silently ignore errors
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        //silently ignore errors
    }

    @Override
    public GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }
    //</editor-fold>


    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_list);


        if (findViewById(R.id.event_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            ((ScheduleListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.schedule_list))
                    .setActivateOnItemClick(true);
        }

        initToolbar();
        Util.syncData(this, Util.SYNCFLAG_DOWNLOAD_FAVORITES);
        initGoogleApi();
    }

    /**
     * Callback method from {@link EventListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(String id, String shareTitle) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putString(EventDetailFragment.ARG_ITEM_ID, id);
            arguments.putString(EventDetailFragment.ARG_ITEM_SHARETITLE, shareTitle);
            EventDetailFragment fragment = new EventDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.event_detail_container, fragment)
                    .commit();

        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, EventDetailActivity.class);
            detailIntent.putExtra(EventDetailFragment.ARG_ITEM_ID, id);
            detailIntent.putExtra(EventDetailFragment.ARG_ITEM_SHARETITLE, shareTitle);
            startActivity(detailIntent);
        }
    }
}
