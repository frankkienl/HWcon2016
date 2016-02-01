package nl.frankkie.hwcon2016.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.plus.Plus;

import org.acra.ACRA;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import nl.frankkie.hwcon2016.R;
import nl.frankkie.hwcon2016.util.GoogleApiUtil;
import nl.frankkie.hwcon2016.util.Util;

/**
 * Created by FrankkieNL on 13-1-2015.
 */
public class AboutAppActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

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

    int timesClicked = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_app);

        // Show the Up button in the action bar.
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(getTitle());
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Fill data
        TextView version = (TextView) findViewById(R.id.aboutapp_version);
        String versionString = "";
        try {
            versionString = "Version: " + this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName + "-" + this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException nnfe) {
            nnfe.printStackTrace();
            ACRA.getErrorReporter().handleException(nnfe);
        }
        version.setText(versionString);

        final View btnViewLicences = findViewById(R.id.aboutapp_viewlicences);
        btnViewLicences.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLicenceInfo();
            }
        });
        btnViewLicences.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mGoogleApiClient.isConnected()){
                    Games.Achievements.unlock(mGoogleApiClient, getString(R.string.achievement_another_easter_egg));
                }
                Toast.makeText(AboutAppActivity.this, R.string.another_easteregg_found, Toast.LENGTH_LONG).show();
                return true;
            }
        });

        //Easter Egg stuff
        ImageView image = (ImageView) findViewById(R.id.aboutapp_frankkienl_image);
        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timesClicked++;
                if (timesClicked >= 10) {
                    Intent i = new Intent(AboutAppActivity.this, EasterEggActivity.class);
                    startActivity(i);
                    timesClicked = 0; //reset
                }
            }
        });

        initGoogleApi();
    }

    public void showLicenceInfo() {
        //Read licence-info from licences.txt in assets.
        String licencesText = "";
        try {
            //http://stackoverflow.com/questions/5771366/reading-a-simple-text-file
            //http://stackoverflow.com/questions/2492076/android-reading-from-an-input-stream-efficiently
            BufferedReader r = new BufferedReader(new InputStreamReader(getAssets().open("licences.txt")));
            StringBuilder total = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                total.append(line);
            }
            licencesText = total.toString();
        } catch (IOException e) {
            //cannot load..
            licencesText = "Cannot load licences information.";
            Util.sendACRAReport("btnViewLicences.onClick", "IOException", "", e);
        }

        //Read licence info from Google Play Services
        String googleLicences = GoogleApiAvailability.getInstance().getOpenSourceSoftwareLicenseInfo(this);
        licencesText += "\n" + googleLicences;

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(R.string.aboutapp_licences);
        b.setMessage(licencesText);
        b.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //nothing, just remove dialog
            }
        });
        //maybe implement this: (when I have time left)
        //http://stackoverflow.com/questions/7557265/prevent-dialog-dismissal-on-screen-rotation-in-android
        //or even better
        //https://www.bignerdranch.com/blog/open-source-licenses-and-android/
        b.create().show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            NavUtils.navigateUpTo(this, new Intent(this, EventListActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
