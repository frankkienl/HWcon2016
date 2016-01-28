package nl.frankkie.hwcon2016.activities;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import nl.frankkie.hwcon2016.R;
import nl.frankkie.hwcon2016.util.Util;


public class AboutActivity extends AppCompatActivity {

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
    }

    public void initUI() {
        Button btnViewMap = (Button) findViewById(R.id.about_view_maps);
        btnViewMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent();
                i.setClass(AboutActivity.this, AboutVenueLocationActivity.class);
                startActivity(i);
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
        v.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                changeAppIcon(true);
                return true;
            }
        });
    }

    public void changeAppIcon(boolean showMessage) {
        //http://stackoverflow.com/questions/17409907/how-to-enable-and-disable-a-component
        PackageManager pm = getApplicationContext().getPackageManager();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        ComponentName componentName1 = new ComponentName("nl.frankkie.hwcon2016", "nl.frankkie.hwcon2016.activities.Splash1Activity");
        ComponentName componentName2 = new ComponentName("nl.frankkie.hwcon2016", "nl.frankkie.hwcon2016.activities.Splash2Activity");
        //which one is enabled? (only one of them is enabled, so just check 1, which is disable in manifest(default))
        if (pm.getComponentEnabledSetting(componentName1) == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                || pm.getComponentEnabledSetting(componentName1) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
            //1 is disabled, so: enable 1, disable 2
            pm.setComponentEnabledSetting(componentName1, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            pm.setComponentEnabledSetting(componentName2, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            prefsEditor.putInt("app_icon", R.drawable.ic_launcher_hwcon2016_1_web);
        } else {
            //1 is enabled, so: disable 1, enable 2
            pm.setComponentEnabledSetting(componentName1, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            pm.setComponentEnabledSetting(componentName2, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            prefsEditor.putInt("app_icon", R.drawable.ic_launcher_hwcon2016_2_web);
        }
        prefsEditor.commit();
        if (!showMessage) {
            return;
        }
        Snackbar.make(findViewById(R.id.container), R.string.about_changedicon, Snackbar.LENGTH_LONG).setAction(android.R.string.cancel, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeAppIcon(false);
            }
        }).show();
    }

}
