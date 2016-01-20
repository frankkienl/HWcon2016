package nl.frankkie.hwcon2016.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import nl.frankkie.hwcon2016.R;

/**
 * http://developer.android.com/guide/components/tasks-and-back-stack.html#Clearing
 * http://www.androidhive.info/2013/07/how-to-implement-android-splash-screen-2/
 * http://developer.android.com/guide/topics/manifest/activity-element.html
 * <p/>
 * Created by fbouwens on 19-1-2016.
 */
public class SplashActivity extends AppCompatActivity {

    int delay = 2 * 1000; /* 2 seconds */
    Handler handler = new Handler();

    /*
     * Implementing killswitch, to make sure you get send to the mainactivity,
     * if you press back in the splashscreen. Because that would be weird.
     */
    boolean killSwitch = false;

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

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                goToMain();
            }
        }, delay);
    }

    public boolean shouldShowSplash() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return prefs.getBoolean("show_splash", true);
    }

    public void goToMain() {
        if (killSwitch) {
            return;
        }
        Intent i = new Intent();
        i.setClass(this, EventListActivity.class);
        startActivity(i);
        killSwitch = true; //launched main, don't allow to launch again.

        //close this one.
        //The uses should not return here on back-press.
        finish();
    }

    public void initUI(){
        setContentView(R.layout.activity_splash);
    }

    @Override
    protected void onPause() {
        super.onPause();

        killSwitch = true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        killSwitch = false;
    }
}
