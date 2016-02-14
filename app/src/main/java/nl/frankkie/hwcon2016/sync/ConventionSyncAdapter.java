package nl.frankkie.hwcon2016.sync;

import android.accounts.Account;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import org.acra.ACRA;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import nl.frankkie.hwcon2016.R;
import nl.frankkie.hwcon2016.data.EventContract;
import nl.frankkie.hwcon2016.util.GcmUtil;
import nl.frankkie.hwcon2016.util.GoogleApiUtil;
import nl.frankkie.hwcon2016.util.Util;

/**
 * Created by FrankkieNL on 6-12-2014.
 */
public class ConventionSyncAdapter extends AbstractThreadedSyncAdapter {

    ContentResolver mContentResolver;
    public static final String TAG = "Convention";

    public ConventionSyncAdapter(Context c, boolean autoInit) {
        super(c, autoInit);
        mContentResolver = c.getContentResolver();
    }

    public ConventionSyncAdapter(Context c, boolean autoInit, boolean allowParallel) {
        super(c, autoInit, allowParallel);
        mContentResolver = c.getContentResolver();
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        //Placed it (download and parsing) into separate methods, because the IDE complained this method was too complex.
        Log.d(TAG, "SyncAdapter: onPerformSync");
        int syncFlags = extras.getInt("syncflags", Util.SYNCFLAG_CONVENTION_DATA);
        //http://stackoverflow.com/questions/6067411/checking-flag-bits-java
        if ((syncFlags & Util.SYNCFLAG_CONVENTION_DATA) == Util.SYNCFLAG_CONVENTION_DATA) {
            String regId = GcmUtil.gcmGetRegId(getContext());
            //CHANGE THIS URL WHEN USING FOR OTHER CONVENTION
            String json = Util.httpDownload("https://wofje.8s.nl/hwcon2016/api/v1/downloadconventiondata.php?regId=" + regId + "&username=" + GoogleApiUtil.getUserEmail(getContext()));
            if (json != null) {
                parseConventionDataJSON(json);
            }
        }
        if ((syncFlags & Util.SYNCFLAG_DOWNLOAD_FAVORITES) == Util.SYNCFLAG_DOWNLOAD_FAVORITES) {
            String regId = GcmUtil.gcmGetRegId(getContext());
            //With username "&username=", for syncing between devices of same user
            String json = Util.httpDownload("https://wofje.8s.nl/hwcon2016/api/v1/downloadfavorites.php?regId=" + regId + "&username=" + GoogleApiUtil.getUserEmail(getContext()));
            if (json != null) {
                parseFavoritesDataJson(json);
            }
        }
        if ((syncFlags & Util.SYNCFLAG_UPLOAD_FAVORITES) == Util.SYNCFLAG_UPLOAD_FAVORITES) {
            //this is for uploading all the favorites. For a delta, use Asynctask instead. See Util.
            try {
                Cursor cursor = getContext().getContentResolver().query(EventContract.FavoritesEntry.CONTENT_URI,
                        new String[]{EventContract.FavoritesEntry.COLUMN_NAME_ITEM_ID},
                        EventContract.FavoritesEntry.COLUMN_NAME_TYPE + " = 'event'", null, null);
                if (cursor.getCount() != 0) {
                    //Only do this when there is data to be send
                    JSONObject root = new JSONObject();
                    JSONArray events = new JSONArray();
                    cursor.moveToFirst();
                    do {
                        events.put(cursor.getString(0));
                    } while (cursor.moveToNext());
                    cursor.close();
                    root.put("events", events);
                    JSONObject device = new JSONObject();
                    device.put("regId", GcmUtil.gcmGetRegId(getContext()));
                    device.put("username", GoogleApiUtil.getUserEmail(getContext()));
                    device.put("nickname", GoogleApiUtil.getUserNickname(getContext()));
                    root.put("device", device);
                    JSONObject wrapper = new JSONObject();
                    wrapper.put("data", root);
                    String json = wrapper.toString();
                    String postData = "json=" + json;
                    ////////////////////////////
                    String response = Util.httpPost(getContext(), "https://wofje.8s.nl/hwcon2016/api/v1/uploadfavorites.php", postData);
                    if (!"ok".equals(response.trim())) {
                        //There muse be something wrong
                        Util.sendACRAReport("Server did not send 'ok', Favorites", "https://wofje.8s.nl/hwcon2016/api/v1/uploadfavorites.php", postData + "\n" + response);
                    }
                }
                /////////////////////////////
            } catch (Exception e) {
                ACRA.getErrorReporter().handleException(e);
                e.printStackTrace();
            }
        }
        ///
        if ((syncFlags & Util.SYNCFLAG_UPLOAD_QRFOUND) == Util.SYNCFLAG_UPLOAD_QRFOUND) {
            try {
                Cursor cursor = getContext().getContentResolver().query(
                        EventContract.QrFoundEntry.CONTENT_URI, //table
                        new String[]{EventContract.QrFoundEntry.COLUMN_NAME_QR_ID, //projection
                                EventContract.QrFoundEntry.COLUMN_NAME_TIME},
                        null, //selection
                        null, //selectionArgs
                        null //sort-order
                );
                if (cursor.getCount() != 0) {
                    JSONObject root = new JSONObject();
                    JSONArray qrsfound = new JSONArray();
                    cursor.moveToFirst();
                    do {
                        JSONObject qrfound = new JSONObject();
                        qrfound.put("qr_id", cursor.getString(0));
                        long unixTimestamp = Long.parseLong(cursor.getString(1));
                        qrfound.put("found_time", unixTimestamp);
                        qrsfound.put(qrfound);
                    } while (cursor.moveToNext());
                    cursor.close();
                    root.put("qrsfound", qrsfound);
                    JSONObject device = new JSONObject();
                    device.put("regId", GcmUtil.gcmGetRegId(getContext()));
                    device.put("username", GoogleApiUtil.getUserEmail(getContext()));
                    device.put("nickname", GoogleApiUtil.getUserNickname(getContext()));
                    root.put("device", device);
                    JSONObject wrapper = new JSONObject();
                    wrapper.put("data", root);
                    String json = wrapper.toString();
                    String postData = "json=" + json;
                    ////////////////////////////
                    String response = Util.httpPost(getContext(), "https://wofje.8s.nl/hwcon2016/api/v1/uploadqrfound.php", postData);
                    if (!"ok".equals(response.trim())) {
                        //There muse be something wrong
                        Util.sendACRAReport("Server did not send 'ok', QRs", "https://wofje.8s.nl/hwcon2016/api/v1/uploadqrfound.php", postData + "\n" + response);
                    }
                }
            } catch (Exception e) {
                ACRA.getErrorReporter().handleException(e);
                e.printStackTrace();
            }
        }
        ////
        if ((syncFlags & Util.SYNCFLAG_DOWNLOAD_QRFOUND) == Util.SYNCFLAG_DOWNLOAD_QRFOUND) {
            String regId = GcmUtil.gcmGetRegId(getContext());
            String json = Util.httpDownload("https://wofje.8s.nl/hwcon2016/api/v1/downloadqrfound.php?regId=" + regId + "&username=" + GoogleApiUtil.getUserEmail(getContext()));
            if (json != null) {
                parseQrFoundDataJson(json);
            }
        }
    }

    public void parseQrFoundDataJson(String json) {
        try {
            JSONObject data = new JSONObject(json).getJSONObject("data");
            JSONArray qrsfound = data.getJSONArray("qrsfound");
            ContentValues[] qrfCVs = new ContentValues[qrsfound.length()];
            for (int i = 0; i < qrsfound.length(); i++) {
                JSONObject qrf = qrsfound.getJSONObject(i);
                ContentValues qrCV = new ContentValues();
                qrCV.put(EventContract.QrFoundEntry.COLUMN_NAME_QR_ID, qrf.getString("qr_id"));
                qrCV.put(EventContract.QrFoundEntry.COLUMN_NAME_TIME, qrf.getString("found_time"));
                qrfCVs[i] = qrCV;
            }

            //Delete old values
            getContext().getContentResolver().delete(EventContract.QrFoundEntry.CONTENT_URI, null, null); //null deletes all rows
            //Insert new ones
            getContext().getContentResolver().bulkInsert(EventContract.QrFoundEntry.CONTENT_URI, qrfCVs);
            //Notify observers
            getContext().getContentResolver().notifyChange(EventContract.QrFoundEntry.CONTENT_URI, null);
            //Notify QR list.
            getContext().getContentResolver().notifyChange(EventContract.QrEntry.CONTENT_URI, null);
        } catch (Exception e) {
            ACRA.getErrorReporter().handleException(e);
        }
    }

    public void parseConventionDataJSON(String json) {
        //<editor-fold desc="boring json parsing and DB inserting code">
        try {
            JSONObject data = new JSONObject(json).getJSONObject("data");

            //<editor-fold desc="events">
            JSONArray events = data.getJSONArray("events");
            ContentValues[] eventCVs = new ContentValues[events.length()];
            for (int i = 0; i < events.length(); i++) {
                JSONObject event = events.getJSONObject(i);
                ContentValues values = new ContentValues();
                values.put(EventContract.EventEntry._ID, event.getInt("_id"));
                values.put(EventContract.EventEntry.COLUMN_NAME_TITLE, event.getString("title"));
                values.put(EventContract.EventEntry.COLUMN_NAME_TITLE_NL, event.getString("title_nl"));
                values.put(EventContract.EventEntry.COLUMN_NAME_DESCRIPTION, event.getString("description"));
                values.put(EventContract.EventEntry.COLUMN_NAME_DESCRIPTION_NL, event.getString("description_nl"));
                values.put(EventContract.EventEntry.COLUMN_NAME_KEYWORDS, event.getString("keywords"));
                values.put(EventContract.EventEntry.COLUMN_NAME_IMAGE, event.getString("image"));
                values.put(EventContract.EventEntry.COLUMN_NAME_START_TIME, event.getString("start_time"));
                values.put(EventContract.EventEntry.COLUMN_NAME_END_TIME, event.getString("end_time"));
                values.put(EventContract.EventEntry.COLUMN_NAME_LOCATION_ID, event.getInt("location_id"));
                values.put(EventContract.EventEntry.COLUMN_NAME_SORT_ORDER, event.getInt("sort_order"));
                eventCVs[i] = values;
            }

            //Delete old values
            getContext().getContentResolver().delete(EventContract.EventEntry.CONTENT_URI, null, null); //null deletes all rows
            //Insert new ones
            getContext().getContentResolver().bulkInsert(EventContract.EventEntry.CONTENT_URI, eventCVs);
            //Notify observers
            getContext().getContentResolver().notifyChange(EventContract.EventEntry.CONTENT_URI, null);
            //</editor-fold>

            //<editor-fold desc="speakers">
            JSONArray speakers = data.getJSONArray("speakers");
            ContentValues[] speakerCVs = new ContentValues[speakers.length()];
            for (int i = 0; i < speakers.length(); i++) {
                JSONObject speaker = speakers.getJSONObject(i);
                ContentValues values = new ContentValues();
                values.put(EventContract.SpeakerEntry._ID, speaker.getInt("_id"));
                values.put(EventContract.SpeakerEntry.COLUMN_NAME_NAME, speaker.getString("name"));
                values.put(EventContract.SpeakerEntry.COLUMN_NAME_NAME_NL, speaker.getString("name_nl"));
                values.put(EventContract.SpeakerEntry.COLUMN_NAME_DESCRIPTION, speaker.getString("description"));
                values.put(EventContract.SpeakerEntry.COLUMN_NAME_DESCRIPTION_NL, speaker.getString("description_nl"));
                values.put(EventContract.SpeakerEntry.COLUMN_NAME_IMAGE, speaker.getString("image"));
                speakerCVs[i] = values;
            }
            getContext().getContentResolver().delete(EventContract.SpeakerEntry.CONTENT_URI, null, null);
            getContext().getContentResolver().bulkInsert(EventContract.SpeakerEntry.CONTENT_URI, speakerCVs);
            getContext().getContentResolver().notifyChange(EventContract.SpeakerEntry.CONTENT_URI, null);
            //</editor-fold>

            //<editor-fold desc="locations">
            JSONArray locations = data.getJSONArray("locations");
            ContentValues[] locationCVs = new ContentValues[locations.length()];
            for (int i = 0; i < locations.length(); i++) {
                JSONObject location = locations.getJSONObject(i);
                ContentValues values = new ContentValues();
                values.put(EventContract.LocationEntry._ID, location.getInt("_id"));
                values.put(EventContract.LocationEntry.COLUMN_NAME_NAME, location.getString("name"));
                values.put(EventContract.LocationEntry.COLUMN_NAME_NAME_NL, location.getString("name_nl"));
                values.put(EventContract.LocationEntry.COLUMN_NAME_DESCRIPTION, location.getString("description"));
                values.put(EventContract.LocationEntry.COLUMN_NAME_DESCRIPTION_NL, location.getString("description_nl"));
                values.put(EventContract.LocationEntry.COLUMN_NAME_MAP_LOCATION, location.getString("map_location"));
                values.put(EventContract.LocationEntry.COLUMN_NAME_FLOOR, location.getInt("floor"));
                locationCVs[i] = values;
            }
            getContext().getContentResolver().delete(EventContract.LocationEntry.CONTENT_URI, null, null);
            getContext().getContentResolver().bulkInsert(EventContract.LocationEntry.CONTENT_URI, locationCVs);
            getContext().getContentResolver().notifyChange(EventContract.LocationEntry.CONTENT_URI, null);
            //</editor-fold>

            //<editor-fold desc="speakers in events">
            JSONArray speakersInEvents = data.getJSONArray("speakers_in_events");
            ContentValues[] sieCVs = new ContentValues[speakersInEvents.length()];
            for (int i = 0; i < speakersInEvents.length(); i++) {
                JSONObject sie = speakersInEvents.getJSONObject(i);
                ContentValues sieCV = new ContentValues();
                sieCV.put(EventContract.SpeakersInEventsEntry._ID, sie.getInt("_id"));
                sieCV.put(EventContract.SpeakersInEventsEntry.COLUMN_NAME_EVENT_ID, sie.getInt("event_id"));
                sieCV.put(EventContract.SpeakersInEventsEntry.COLUMN_NAME_SPEAKER_ID, sie.getInt("speaker_id"));
                sieCVs[i] = sieCV;
            }
            getContext().getContentResolver().delete(EventContract.SpeakersInEventsEntry.CONTENT_URI, null, null);
            getContext().getContentResolver().bulkInsert(EventContract.SpeakersInEventsEntry.CONTENT_URI, sieCVs);
            getContext().getContentResolver().notifyChange(EventContract.SpeakersInEventsEntry.CONTENT_URI, null);
            //</editor-fold>

            //<editor-fold desc="news">
            JSONArray news = data.getJSONArray("news");
            ContentValues[] nCVs = new ContentValues[news.length()];
            for (int i = 0; i < news.length(); i++) {
                JSONObject n = news.getJSONObject(i);
                ContentValues ncv = new ContentValues();
                ncv.put(EventContract.NewsEntry._ID, n.getInt("_id"));
                ncv.put(EventContract.NewsEntry.COLUMN_NAME_TITLE, n.getString("title"));
                ncv.put(EventContract.NewsEntry.COLUMN_NAME_IMAGE, n.getString("image"));
                ncv.put(EventContract.NewsEntry.COLUMN_NAME_URL, n.getString("url"));
                nCVs[i] = ncv;
            }
            getContext().getContentResolver().delete(EventContract.NewsEntry.CONTENT_URI, null, null);
            getContext().getContentResolver().bulkInsert(EventContract.NewsEntry.CONTENT_URI, nCVs);
            getContext().getContentResolver().notifyChange(EventContract.NewsEntry.CONTENT_URI, null);
            //</editor-fold>

            //<editor-fold desc="qr">
            //QR is a separate table, to keep the userdata (found or not) separate from the convention-data
            if (data.has("qr")) {
                JSONArray qrs = data.getJSONArray("qr");
                ContentValues[] qrCVs = new ContentValues[qrs.length()];
                for (int i = 0; i < qrs.length(); i++) {
                    JSONObject qr = qrs.getJSONObject(i);
                    ContentValues qrCV = new ContentValues();
                    qrCV.put(EventContract.QrEntry._ID, qr.getInt("_id"));
                    qrCV.put(EventContract.QrEntry.COLUMN_NAME_HASH, qr.getString("hash"));
                    qrCV.put(EventContract.QrEntry.COLUMN_NAME_NAME, qr.getString("name"));
                    qrCV.put(EventContract.QrEntry.COLUMN_NAME_NAME_NL, qr.getString("name_nl"));
                    qrCV.put(EventContract.QrEntry.COLUMN_NAME_DESCRIPTION, qr.getString("description"));
                    qrCV.put(EventContract.QrEntry.COLUMN_NAME_DESCRIPTION_NL, qr.getString("description_nl"));
                    qrCV.put(EventContract.QrEntry.COLUMN_NAME_IMAGE, qr.getString("image"));
                    qrCVs[i] = qrCV;
                }
                getContext().getContentResolver().delete(EventContract.QrEntry.CONTENT_URI, null, null);
                getContext().getContentResolver().bulkInsert(EventContract.QrEntry.CONTENT_URI, qrCVs);
                getContext().getContentResolver().notifyChange(EventContract.QrEntry.CONTENT_URI, null);
            }
            //</editor-fold>

            //<editor-fold desc="app config">
            //App configuration and version-checking
            if (data.has("app")) {
                JSONObject app = data.getJSONObject("app");
                //this is the versioncode
                //production, as in Google Play Store
                int latestProdVersion = app.getInt("latestProdVersion");
                //beta, as in download apk
                int latestBetaVersion = app.getInt("latestBetaVersion");
                //download beat apk from here
                String latestBetaApkUrl = app.getString("latestBetaApkUrl");

                try {
                    PackageInfo pInfo = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0);

                    if (latestProdVersion > pInfo.versionCode) {
                        //update via Google Play Store
                        NotificationCompat.Builder b = new NotificationCompat.Builder(getContext());
                        b.setSmallIcon(R.drawable.ic_stat_notification_heart);
                        b.setContentTitle("Update available");
                        b.setContentText("Please update " + getContext().getString(R.string.app_name) + " via the Google Play Store");
                        b.setAutoCancel(true);
                        Intent playStoreIntent = new Intent();
                        playStoreIntent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + getContext().getPackageName()));
                        PendingIntent pi = PendingIntent.getActivity(getContext(), 0, playStoreIntent, 0);
                        b.setContentIntent(pi);
                        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(getContext());
                        notificationManagerCompat.notify(0, b.build());
                    }

                    if (Util.isTester(getContext())) {
                        if (latestBetaVersion > pInfo.versionCode) {
                            //TODO: better management of beta-users.
                            //only beta-testers should get a notification that there is a new APK
                            //regular uses should just upgrade via the play store,
                            //even if that versioncode is lower that the beta versioncode.
                            boolean isBetaUser = true;
                            if (isBetaUser) {
                                NotificationCompat.Builder b = new NotificationCompat.Builder(getContext());
                                b.setSmallIcon(R.drawable.ic_stat_notification_heart);
                                b.setContentTitle("Update available [BETA]");
                                b.setContentText("Please update " + getContext().getString(R.string.app_name) + " [BETA]");
                                b.setAutoCancel(true);
                                Intent playStoreIntent = new Intent();
                                playStoreIntent.setData(Uri.parse(latestBetaApkUrl));
                                PendingIntent pi = PendingIntent.getActivity(getContext(), 0, playStoreIntent, 0);
                                b.setContentIntent(pi);
                                NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(getContext());
                                notificationManagerCompat.notify(0, b.build());
                            }
                        }
                    }
                } catch (Exception e) {
                    Util.sendACRAReport("ConventionSyncAdapter.parseConventionDataJSON#app", e.toString(), e.getMessage(), e);
                }

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                SharedPreferences.Editor prefsEdit = prefs.edit();
                if (app.has("sections")) {
                    //check which sections are enabled
                    JSONObject sections = app.getJSONObject("sections");
                    prefsEdit.putBoolean("section_schedule_enabled", sections.getBoolean("schedule"));
                    prefsEdit.putBoolean("section_browse_enabled", sections.getBoolean("browse"));
                    prefsEdit.putBoolean("section_map_enabled", sections.getBoolean("map"));
                    prefsEdit.putBoolean("section_qrhunt_enabled", sections.getBoolean("qrhunt"));
                    prefsEdit.putBoolean("section_login_enabled", sections.getBoolean("login"));
                    prefsEdit.putBoolean("section_news_enabled", sections.getBoolean("news"));
                    prefsEdit.putBoolean("section_about_enabled", sections.getBoolean("about"));
                    prefsEdit.putBoolean("show_splash", sections.getBoolean("splash"));
                    prefsEdit.putString("section_after_splash", sections.getString("sectionAfterSplash"));
                } else {
                    //if no sections part, enable all sections
                    prefsEdit.putBoolean("section_schedule_enabled", true);
                    prefsEdit.putBoolean("section_browse_enabled", true);
                    prefsEdit.putBoolean("section_map_enabled", true);
                    prefsEdit.putBoolean("section_qrhunt_enabled", true);
                    prefsEdit.putBoolean("section_login_enabled", true);
                    prefsEdit.putBoolean("section_news_enabled", true);
                    prefsEdit.putBoolean("section_about_enabled", true);
                    prefsEdit.putBoolean("show_splash", true);
                    prefsEdit.putString("section_after_splash", "browse");
                }
                if (app.has("map")){
                    JSONObject map = app.getJSONObject("map");
                    prefsEdit.putString("map_download_url",map.getString("url"));
                    prefsEdit.putInt("map_latest_version",map.getInt("version"));
                }
                prefsEdit.commit();
            }
            //</editor-fold>
        } catch (JSONException e) {
            Log.e("Convention", "Error in SyncAdapter.onPerformSync, ConventionData JSON ", e);
            ACRA.getErrorReporter().handleException(e);
        }
        //</editor-fold>
    }

    public void parseFavoritesDataJson(String json) {
        try {
            JSONObject root = new JSONObject(json);
            JSONObject data = root.getJSONObject("data");
            JSONArray events = data.getJSONArray("events");
            ContentValues[] eCVs = new ContentValues[events.length()];
            for (int i = 0; i < events.length(); i++) {
                int id = Integer.parseInt(events.getString(i));
                ContentValues eCV = new ContentValues();
                eCV.put(EventContract.FavoritesEntry.COLUMN_NAME_TYPE, EventContract.FavoritesEntry.TYPE_EVENT);
                eCV.put(EventContract.FavoritesEntry.COLUMN_NAME_ITEM_ID, id);
                eCVs[i] = eCV;
            }
            getContext().getContentResolver().delete(EventContract.FavoritesEntry.CONTENT_URI, null, null);
            getContext().getContentResolver().bulkInsert(EventContract.FavoritesEntry.CONTENT_URI, eCVs);
            getContext().getContentResolver().notifyChange(EventContract.FavoritesEntry.CONTENT_URI, null);
            //TODO add code to sync other types of favorites.
        } catch (JSONException e) {
            Log.e("Convention", "Error in SyncAdapter.onPerformSync, ConventionData JSON ", e);
            ACRA.getErrorReporter().handleException(e);
        }
    }
}
