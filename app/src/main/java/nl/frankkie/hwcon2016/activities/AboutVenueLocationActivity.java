package nl.frankkie.hwcon2016.activities;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import nl.frankkie.hwcon2016.R;
import nl.frankkie.hwcon2016.util.Util;

/**
 * Created by fbouwens on 14-1-2016.
 */
public class AboutVenueLocationActivity extends AppCompatActivity {

    TextView tvTravelTimes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_venue_location);
        initUI();
    }

    public void initUI() {

        // Show the Up button in the action bar.
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(getTitle());
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        tvTravelTimes = (TextView) findViewById(R.id.about_venue_location_travel_times);
        getTravelTimes();

        final Button viewInGoogleMaps = (Button) findViewById(R.id.about_view_maps);
        viewInGoogleMaps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewInMapApp();
            }
        });
    }

    public void getTravelTimes() {
        //Google Maps API Call
        TravelTimesAsyncTask task = new TravelTimesAsyncTask(new WeakReference<AboutVenueLocationActivity>(this));
        task.execute();
    }

    public void viewInMapApp() {
        Intent i = new Intent(Intent.ACTION_VIEW);
        //i.setData(Uri.parse("https://www.google.nl/maps?t=m&z=15&cid=11779929733433826402"));
        //i.setData(Uri.parse("geo:52.3118607,4.6636143"));
        //i.setData(Uri.parse("geo:0,0?q=52.3118607,4.6636143(Venue)"));
        i.setData(Uri.parse("geo:0,0?q=IJweg%201094%202133%20MH%20Hoofddorp"));
        try {
            startActivity(i);
        } catch (ActivityNotFoundException anfe) {
            //This happens on the emulator, when google maps is not installed
            Toast.makeText(AboutVenueLocationActivity.this, R.string.about_map_app_not_found, Toast.LENGTH_LONG).show();
        }
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

    public class TravelTimesAsyncTask extends AsyncTask<Void, Void, String> {

        WeakReference<AboutVenueLocationActivity> act;

        public TravelTimesAsyncTask(WeakReference<AboutVenueLocationActivity> o) {
            act = o;
        }

        @Override
        protected String doInBackground(Void... params) {
            //Maybe use Retrofit instead?
            //API KEY??!
            Util.httpDownload("https://maps.googleapis.com/maps/api/directions/json?" +
                    "origin=" + //TODO: current location in here
                    "&destination=IJweg%201094%202133%20MH%20Hoofddorp" +
                    "&traffic_model=best_guess");
            return "";
        }

        @Override
        protected void onPostExecute(String s) {
            act.get().tvTravelTimes.setText(s);
        }
    }
}
