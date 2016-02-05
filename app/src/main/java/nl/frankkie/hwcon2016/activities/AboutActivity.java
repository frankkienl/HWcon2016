package nl.frankkie.hwcon2016.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.plus.Plus;

import org.acra.ACRA;

import nl.frankkie.hwcon2016.R;
import nl.frankkie.hwcon2016.fragments.AppIconDialogFragment;
import nl.frankkie.hwcon2016.util.Util;


public class AboutActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        initToolbar();
        initUI();
        initGoogleApi();
    }

    public void initUI() {
        Button btnViewMap = (Button) findViewById(R.id.about_view_maps);
        btnViewMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent i = new Intent();
//                i.setClass(AboutActivity.this, AboutVenueLocationActivity.class);
//                startActivity(i);
                viewInMapApp();
            }
        });
        Button btnAboutApp = (Button) findViewById(R.id.about_btn_aboutapp);
        btnAboutApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent();
                i.setClass(AboutActivity.this, AboutAppActivity.class);
                startActivity(i);
            }
        });
        View v = findViewById(R.id.about_banner);
        if (Util.isTester(this)) {
            //only testers are allowed to change the icon.
            v.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showChangeIconDialog();
                    return true;
                }
            });
        }
    }

    public void showChangeIconDialog() {
        AppIconDialogFragment a = new AppIconDialogFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        a.show(fragmentTransaction, "Pick Icon Dialog");

        if (mGoogleApiClient.isConnected()) {
            Games.Achievements.unlock(mGoogleApiClient, getString(R.string.achievement_personal_style));
        }
    }

    public void viewInMapApp() {
        Intent i = new Intent(Intent.ACTION_VIEW);
        //i.setData(Uri.parse("https://www.google.nl/maps?t=m&z=15&cid=11779929733433826402"));
        //i.setData(Uri.parse("geo:52.3118607,4.6636143"));
        //i.setData(Uri.parse("geo:0,0?q=52.3118607,4.6636143(Venue)"));
        i.setData(Uri.parse("geo:0,0?q=IJweg%201094%202133%20MH%20Hoofddorp"));
        try {
            startActivity(i);
        } catch (ActivityNotFoundException anfe) {
            //This happens on the emulator, when google maps is not installed
            Toast.makeText(AboutActivity.this, R.string.about_map_app_not_found, Toast.LENGTH_LONG).show();
        }
    }
}
