package nl.frankkie.hwcon2016.activities;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import nl.frankkie.hwcon2016.fragments.NavigationDrawerFragment;
import nl.frankkie.hwcon2016.R;
import nl.frankkie.hwcon2016.util.Util;


public class AboutActivity extends AppCompatActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    //<editor-fold desc="ActionBar Stuff">
    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    Toolbar mToolbar;
    ActionBarDrawerToggle mDrawerToggle;

    public void initToolbar() {
        mTitle = getTitle();
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(mTitle);
        setSupportActionBar(mToolbar);
        ///
        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        // Set up the drawer.
        mDrawerToggle = mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
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

    public void restoreActionBar() {
        getSupportActionBar().setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        restoreActionBar();
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Callback from Hamburger-menu
     *
     * @param position
     */
    @Override
    public void onNavigationDrawerItemSelected(int position) {
        Util.navigateFromNavDrawer(this, position);
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
        ComponentName componentName1 = new ComponentName("nl.frankkie.hwcon2016","nl.frankkie.hwcon2016.activities.Splash1Activity");
        ComponentName componentName2 = new ComponentName("nl.frankkie.hwcon2016","nl.frankkie.hwcon2016.activities.Splash2Activity");
        //which one is enabled? (only one of them is enabled, so just check 1, which is disable in manifest(default))
        if (pm.getComponentEnabledSetting(componentName1) == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                ||pm.getComponentEnabledSetting(componentName1) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED ){
            //1 is disabled, so: enable 1, disable 2
            pm.setComponentEnabledSetting(componentName1, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            pm.setComponentEnabledSetting(componentName2, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        } else {
            //1 is enabled, so: disable 1, enable 2
            pm.setComponentEnabledSetting(componentName1, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            pm.setComponentEnabledSetting(componentName2, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        }

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
