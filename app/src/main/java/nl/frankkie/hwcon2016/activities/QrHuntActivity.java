package nl.frankkie.hwcon2016.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import nl.frankkie.hwcon2016.util.GoogleApiUtil;
import nl.frankkie.hwcon2016.util.Util;

/**
 * Created by FrankkieNL on 18-1-2015.
 */
public class QrHuntActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, GoogleApiUtil.GiveMeGoogleApiClient {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_qrhunt);

        initToolbar();

        initGoogleApi();

        if (!GoogleApiUtil.isUserLoggedIn(this)){
            AlertDialog.Builder b = new AlertDialog.Builder(this);
            b.setTitle(R.string.qr_login_required_title);
            b.setMessage(R.string.qr_login_required_message);
            b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //Go to LoginActivity
                    Intent i = new Intent(QrHuntActivity.this, LoginActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                    finish();
                }
            });
            b.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    //Haha, you thought you could do the QR hunt without logging in by removing this dialog.
                    //No, you're not :P Login is required.
                    //Go to LoginActivity
                    Intent i = new Intent(QrHuntActivity.this, LoginActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                    finish();
                }
            });
            b.create().show();
        } else {
            showQrTip();
        }
    }
    public void showQrTip() {
        //Show QR-tip
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean shownStarTip = prefs.getBoolean("prefs_shown_qr_tip", false);
        if (!shownStarTip) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.title_section_qr_hunt);
            //builder.setMessage(R.string.qr_tip_message);
            builder.setView(this.getLayoutInflater().inflate(R.layout.qr_tip, null, false));
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //Set to shown=true, when user presses the OK-button.
                    prefs.edit().putBoolean("prefs_shown_qr_tip", true).apply();
                    //Using apply (instead of commit), because we don't want to stall the UI-thread.
                    //apply will make the change in memory, and then save it to persistent story
                    //on a background thread.
                }
            });
            builder.create().show();
        }
    }
}
