package nl.frankkie.hwcon2016.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import nl.frankkie.hwcon2016.R;

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

        handler.postDelayed(runGoToMain, delay);
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

    public void initUI() {
        setContentView(R.layout.activity_splash);

        //If the user presses the logo,
        //go to main directly. No more delay
        View v = findViewById(R.id.splash_logo);
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
