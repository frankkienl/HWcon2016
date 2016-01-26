package nl.frankkie.hwcon2016.activities;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.webkit.WebView;

import nl.frankkie.hwcon2016.R;
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
        wv.loadUrl("file:///android_asset/map.html");
        wv.getSettings().setBuiltInZoomControls(true);
    }

}
