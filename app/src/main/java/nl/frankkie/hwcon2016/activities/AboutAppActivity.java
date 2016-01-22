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

import com.google.android.gms.common.GoogleApiAvailability;

import org.acra.ACRA;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import nl.frankkie.hwcon2016.R;
import nl.frankkie.hwcon2016.util.Util;

/**
 * Created by FrankkieNL on 13-1-2015.
 */
public class AboutAppActivity extends AppCompatActivity {

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
    }

    public void showLicenceInfo(){
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
