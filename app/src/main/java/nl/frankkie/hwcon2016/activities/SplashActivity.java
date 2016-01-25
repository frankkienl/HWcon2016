package nl.frankkie.hwcon2016.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import org.acra.ACRA;

import java.util.HashMap;
import java.util.Map;

import nl.frankkie.hwcon2016.R;
import nl.frankkie.hwcon2016.RegistrationIntentService;
import nl.frankkie.hwcon2016.util.Util;

/**
 * http://developer.android.com/guide/components/tasks-and-back-stack.html#Clearing
 * http://www.androidhive.info/2013/07/how-to-implement-android-splash-screen-2/
 * http://developer.android.com/guide/topics/manifest/activity-element.html
 * <p/>
 * Created by fbouwens on 19-1-2016.
 */
public class SplashActivity extends AppCompatActivity {

    int delay = 3 * 1000; /* 3 seconds */
    Handler handler = new Handler();

    /*
     * Implementing killswitch, to make sure you get send to the mainactivity,
     * if you press back in the splashscreen. Because that would be weird.
     */
    boolean killSwitch = false;

    Runnable runGoToMain = new Runnable() {
        @Override
        public void run() {
            goToMain();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!shouldShowSplash()) {
            //go to main directly
            killSwitch = false;
            goToMain();
            return;
        }

        initUI();

        //Sync ContentProvider using SyncAdapter
        Util.syncConventionData(this);
    }

    public boolean shouldShowSplash() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return prefs.getBoolean("show_splash", true);
    }

    public void goToMain() {
        if (killSwitch) {
            handler.removeCallbacks(runGoToMain);
            return;
        }

        Intent i = new Intent();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //which screen to go to
        String sectionAfterSplash = prefs.getString("section_after_splash", "browse");
        i.setClass(this, Util.getSectionClass(sectionAfterSplash));
        startActivity(i);

        killSwitch = true; //launched main, don't allow to launch again.

        //close this one.
        //The uses should not return here on back-press.
        finish();
    }

    public void initUI() {
        setContentView(R.layout.activity_splash);

        //If the user presses the logo,
        //go to main directly. No more delay
        ImageView v = (ImageView) findViewById(R.id.splash_logo);
        v.setImageResource(getIconResourceId());
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                killSwitch = false;
                goToMain();

                //remove callback from handler
                //no need to run as we already did.
                handler.removeCallbacks(runGoToMain);
                //it does acutally does not matter that much
                //as the killswitch would have been true anyway
                //( killswitch gets set to true in goToMain )
            }
        });
    }

    public int getIconResourceId() {
        return R.drawable.ic_launcher_hwcon2016_2_web;
    }

    @Override
    protected void onPause() {
        super.onPause();

        killSwitch = true;
        handler.removeCallbacks(runGoToMain);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //let the user go to next screen, after delay
        killSwitch = false;
        handler.postDelayed(runGoToMain, delay);

        //Check for Google Play Service
        int flag = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (flag != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(flag)) {
                GooglePlayServicesUtil.showErrorNotification(flag, this);
            } else {
                //killswitch, we don't want to let the user go to the next screen
                //if there is no Google Play Services
                killSwitch = true;
                Log.e(getString(R.string.app_name), "Google Play Services not supported.");
                ACRA.getErrorReporter().handleException(new RuntimeException("Google Play Services not supported."));
                AlertDialog.Builder b = new AlertDialog.Builder(this);
                b.setMessage("This device is not supported, because it does not have Google Play Services.");
                b.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //close app, when ok is pressed
                        finish();
                    }
                });
                b.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        //close app, when dialog is canceled
                        finish();
                    }
                });
                b.create().show();
            }
        } else {
            //GCM is available!!
            Intent i = new Intent(this, RegistrationIntentService.class);
            startService(i);
        }
    }
}
