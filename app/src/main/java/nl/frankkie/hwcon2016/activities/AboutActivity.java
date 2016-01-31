package nl.frankkie.hwcon2016.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
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
                showChangeIconDialog();
                return true;
            }
        });
    }

    public static final int[] ICONS = {R.drawable.ic_launcher_hwcon2016_1_web, R.drawable.ic_launcher_hwcon2016_2_web, R.drawable.ic_launcher_hwcon2016_3_web};

    public void showChangeIconDialog() {
        String[] iconNames = new String[ICONS.length];
        for (int i = 0; i < ICONS.length; i++) {
            iconNames[i] = "Icon " + (i + 1);
        }
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setItems(iconNames, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                changeAppIcon(which);
            }
        });
        b.create().show();
    }

    public void changeAppIcon(int choiceIndex) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putInt("app_icon", ICONS[choiceIndex]);
        prefsEditor.commit();
        //
        PackageManager pm = getApplicationContext().getPackageManager();
        for (int i = 0; i < ICONS.length; i++) {
            ComponentName componentName = new ComponentName(getPackageName(), getPackageName() + ".activities.Splash" + (i + 1) + "Activity");
            if (i == choiceIndex) {
                //enable this one
                pm.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            } else {
                //disable this one
                pm.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            }
        }
        //
        Snackbar.make(findViewById(R.id.container), R.string.about_changedicon, Snackbar.LENGTH_LONG).show();
    }

}
