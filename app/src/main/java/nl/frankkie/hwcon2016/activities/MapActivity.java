package nl.frankkie.hwcon2016.activities;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.Toast;

import java.io.File;

import nl.frankkie.hwcon2016.R;
import nl.frankkie.hwcon2016.util.MapDownloadIntentService;
import nl.frankkie.hwcon2016.util.Util;


public class MapActivity extends AppCompatActivity {

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

    WebView wv;
    private static final int MY_PERMISSIONS_REQUEST = 1234;
    MyLocalBroadcastReceiver myLocalBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        initToolbar();

        initUI();
    }

    @Override
    public void onBackPressed() {
        if (wv != null) {
            if (wv.canGoBack()) {
                wv.goBack();
                return;
            }
        }
        super.onBackPressed();
    }

    public void initUI() {
        wv = (WebView) findViewById(R.id.map_webview);
        //http://stackoverflow.com/questions/3808532/how-to-set-the-initial-zoom-width-for-a-webview
        wv.getSettings().setLoadWithOverviewMode(true);
        wv.getSettings().setUseWideViewPort(true);
        wv.getSettings().setBuiltInZoomControls(true);

        wv.loadUrl("file:///android_asset/map/map_not_downloaded.html");

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        myLocalBroadcastReceiver = new MyLocalBroadcastReceiver();
        localBroadcastManager.registerReceiver(myLocalBroadcastReceiver, new IntentFilter());

        checkMapDownloaded();
    }

    public void checkMapDownloaded() {
        askPermssion();
        //Map stuff
        File mapDir = new File(getExternalFilesDir(null), "/map/");
        File indexFile = new File(mapDir, "index.html");

        if (!indexFile.exists()) {
            //So, not downloaded yet, load the 'not downloaded yet'-map
            wv.loadUrl("file:///android_asset/map/map_not_downloaded.html");
        } else {
            wv.loadUrl("file://" + indexFile.getAbsolutePath());
        }

        downloadMap();
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.unregisterReceiver(myLocalBroadcastReceiver);
    }

    public void downloadMap() {
        Intent i = new Intent(this, MapDownloadIntentService.class);
        startService(i);
    }

    public void askPermssion() {
        //Check SD card Permission
        if (true) {
            //Apparently, you don't need to ask permission for ExternalFilesDir.
            //http://developer.android.com/guide/topics/data/data-storage.html#AccessingExtFiles
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //Request
            if (ActivityCompat.shouldShowRequestPermissionRationale(thisAct,
                    android.Manifest.permission.GET_ACCOUNTS)) {
                Toast.makeText(this, "You need to give permission to access these features", Toast.LENGTH_LONG).show();
            }
            ActivityCompat.requestPermissions(thisAct,
                    new String[]{android.Manifest.permission.GET_ACCOUNTS},
                    MY_PERMISSIONS_REQUEST);

            return; //We'll get back to map-stuff after permission is granted
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkMapDownloaded();
        } else {
            Toast.makeText(this, "You need to give permission to access these features", Toast.LENGTH_LONG).show();
        }
    }


    public class MyLocalBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            checkMapDownloaded();
        }
    }
}
