package nl.frankkie.hwcon2016.util;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import com.google.zxing.client.android.Intents;

import org.acra.ACRA;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import nl.frankkie.hwcon2016.R;
import nl.frankkie.hwcon2016.activities.AboutActivity;
import nl.frankkie.hwcon2016.activities.EventListActivity;
import nl.frankkie.hwcon2016.activities.LoginActivity;
import nl.frankkie.hwcon2016.activities.MapActivity;
import nl.frankkie.hwcon2016.activities.NewsActivity;
import nl.frankkie.hwcon2016.activities.QrHuntActivity;
import nl.frankkie.hwcon2016.activities.ScheduleActivity;
import nl.frankkie.hwcon2016.fragments.AppIconDialogFragment;

/**
 * Created by fbouwens on 10-12-14.
 */
public class Util {
    public static final int navigationDrawerIntentFlags = Intent.FLAG_ACTIVITY_CLEAR_TOP;

    public static final String DATE_FORMAT = "E, HH:mm"; //example: Sunday, 16:30
    public static SimpleDateFormat displayDateFormat = new SimpleDateFormat(DATE_FORMAT);
    public static SimpleDateFormat mysqlDateFormat = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");

    public static Map<String, Class<? extends Activity>> sectionsToClass = new HashMap<String, Class<? extends Activity>>();

    static {
        sectionsToClass.put("browse", EventListActivity.class);
        sectionsToClass.put("schedule", ScheduleActivity.class);
        sectionsToClass.put("map", MapActivity.class);
        sectionsToClass.put("qrhunt", QrHuntActivity.class);
        sectionsToClass.put("login", LoginActivity.class);
        sectionsToClass.put("news", NewsActivity.class);
        sectionsToClass.put("about", AboutActivity.class);
    }


    public static Class<? extends Activity> getSectionClass(String sectionName) {
        return sectionsToClass.get(sectionName);
    }

    public static Map<Integer, Class<? extends Activity>> sectionsIdToClass = new HashMap<Integer, Class<? extends Activity>>();

    static {
        sectionsIdToClass.put(R.id.navigation_item_schedule, ScheduleActivity.class);
        sectionsIdToClass.put(R.id.navigation_item_browse, EventListActivity.class);
        sectionsIdToClass.put(R.id.navigation_item_map, MapActivity.class);
        sectionsIdToClass.put(R.id.navigation_item_qrhunt, QrHuntActivity.class);
        sectionsIdToClass.put(R.id.navigation_item_login, LoginActivity.class);
        sectionsIdToClass.put(R.id.navigation_item_news, NewsActivity.class);
        sectionsIdToClass.put(R.id.navigation_item_about, AboutActivity.class);
    }

    public static Class<? extends Activity> getSectionIdClass(int sectionId) {
        return sectionsIdToClass.get(sectionId);
    }

    public static String getDataTimeString(String timeStringFromDatabase) {
        //Parse MySQL DateType format.
        //http://stackoverflow.com/questions/9945072/convert-string-to-date-in-java
        Date parsedDate = new Date();
        try {
            parsedDate = mysqlDateFormat.parse(timeStringFromDatabase);
        } catch (Exception e) {
            //oops
            sendACRAReport("Util.getDataTimeString", e.toString(), "date to parse: " + timeStringFromDatabase, e);
        }
        return displayDateFormat.format(parsedDate);
    }

    public static void navigateFromNavDrawer(Activity from, Intent to) {
        //Inspired by NavUtils.navigateToUp
        //see: https://android.googlesource.com/platform/frameworks/support/+/refs/heads/master/v4/java/android/support/v4/app/NavUtils.java
        to.addFlags(navigationDrawerIntentFlags);
        from.startActivity(to);
        from.finish();
    }

    public static Account createDummyAccount(Context context) {
        //TODO: Change domain when using for a different convention
        Account account = new Account("dummyaccount", "nl.frankkie.hwcon2016");
        AccountManager accountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        boolean success = accountManager.addAccountExplicitly(account, null, null);
        if (!success) {
            Log.e(context.getString(R.string.app_name), "Cannot create account for Sync.");
        }
        return account;
    }

    public static final int SYNCFLAG_NONE = 0;
    public static final int SYNCFLAG_CONVENTION_DATA = 1;
    public static final int SYNCFLAG_DOWNLOAD_FAVORITES = 2;
    public static final int SYNCFLAG_UPLOAD_FAVORITES = 4;
    public static final int SYNCFLAG_UPLOAD_QRFOUND = 8;
    public static final int SYNCFLAG_DOWNLOAD_QRFOUND = 16; //for sync between devices

    public static void syncData(Context context, int syncWhatFlags) {
        //Create Account needed for SyncAdapter
        Account acc = createDummyAccount(context);
        //Sync
        Bundle syncBundle = new Bundle();
        syncBundle.putInt("syncflags", syncWhatFlags);
        syncBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        syncBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true); //as in: run NOW.
        ContentResolver.requestSync(acc, "nl.frankkie.hwcon2016", syncBundle);
    }

    public static void syncConventionData(Context context) {
        syncData(context, SYNCFLAG_CONVENTION_DATA);
    }

    public static void sendFavoriteDelta(Context context, String id, boolean isFavorite) {
        //This does send all favorites, not just a delta. because im lazy
        Util.syncData(context, Util.SYNCFLAG_UPLOAD_FAVORITES);
    }

    public static void sendQrFound(Context context) {
        //Upload found QR-codes.
        Util.syncData(context, Util.SYNCFLAG_UPLOAD_QRFOUND);
    }

    public static void showNotification(Context context, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentTitle(context.getString(R.string.app_name));
        builder.setContentText(message);
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        builder.setCategory(NotificationCompat.CATEGORY_MESSAGE);
        builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher_hwcon2015_2));
        builder.setVibrate(new long[]{50, 250, 50, 250}); //delay,vibrate,delay,etc.
        //http://stackoverflow.com/questions/8801122/set-notification-sound-from-assets-folder
        //The docs are not clear about how to add sound, StackOverflow to the rescue!
        builder.setSound(Uri.parse("android.resource://nl.frankkie.hwcon2016/raw/yay"));
        builder.setSmallIcon(R.drawable.ic_stat_notification_heart);
        Intent i = new Intent(context, EventListActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 0, i, 0);
        builder.setContentIntent(pi);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(1, builder.build());
    }

    public static String httpDownload(String urlToDownload) {
        //<editor-fold desc="boring http downloading code">
        String json = null;
        HttpURLConnection urlConnection = null;
        BufferedReader br = null;

        try {
            URL url = new URL(urlToDownload);
            //sending regId to update the lastConnected-status on the database
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            //<rant>
            //Read it with streams, using much boilerplate,
            //because apparently that is more awesome compared to just using the Apache HttpClient Libs.
            //Srsly, this is 2014, we have libs to do this for us now.
            //https://github.com/udacity/Sunshine/blob/6.10-update-map-intent/app/src/main/java/com/example/android/sunshine/app/sync/SunshineSyncAdapter.java#L118
            //</rant>
            InputStream is = urlConnection.getInputStream();
            if (is == null) {
                //Apparently there is no inputstream.
                //We're done here
                return null;
            }

            //Why does Sunshine use a StringBuffer instead of a StringBuilder?
            //A StringBuffer is ThreadSafe (Synchronised) but has worse performance.
            //There is no need to use a ThreadSafe StringBuffer here, this sync-option will never be called multiple times from other threads.
            //Because of 'android:allowParallelSyncs="false"' in R.xml.syncadapter
            //See:
            //https://github.com/udacity/Sunshine/blob/6.10-update-map-intent/app/src/main/java/com/example/android/sunshine/app/sync/SunshineSyncAdapter.java#L120
            //http://stackoverflow.com/questions/355089/stringbuilder-and-stringbuffer
            StringBuilder sb = new StringBuilder();
            br = new BufferedReader(new InputStreamReader(is));
            String line;
            while (true) {
                line = br.readLine();
                if (line == null) {
                    break;
                }
                //Chained append is better than concat
                //https://github.com/udacity/Sunshine/blob/6.10-update-map-intent/app/src/main/java/com/example/android/sunshine/app/sync/SunshineSyncAdapter.java#L132
                sb.append(line).append("\n");
            }
            if (sb.length() == 0) {
                //empty
                sendACRAReport("Util.httpDownload", "Empty Response", urlToDownload);
                return null;
            }

            json = sb.toString();
        } catch (IOException e) {
            Log.e("Convention", "Error while downloading http data from " + urlToDownload, e);
            sendACRAReport("Util.httpDownload", "Error while downloading (IOException)", urlToDownload, e);
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (br != null) {
                //*cough* boilerplate *cough*
                try {
                    br.close();
                } catch (IOException e) {
                    Log.e("Convention", "Error closing BufferedReader", e);
                    sendACRAReport("Util.httpDownload", "Error closing BufferedReader (IOException)", urlToDownload, e);
                }
            }
        }
        //</editor-fold>
        return json;
    }

    public static String httpPost(Context context, String urlString, String postData) throws IOException {
        HttpURLConnection urlConnection = null;
        BufferedReader br = null;
        PrintWriter pw = null;
        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            //http://stackoverflow.com/questions/4205980/java-sending-http-parameters-via-post-method-easily
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true); //output, because post-data
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            //urlConnection.setRequestProperty("charset","utf-8");
            urlConnection.setRequestProperty("Content-Length", "" + postData.getBytes().length); //simple int to String casting.
            urlConnection.setUseCaches(false);
            urlConnection.connect();
            OutputStream os = new BufferedOutputStream(urlConnection.getOutputStream());
            pw = new PrintWriter(os);
            pw.print(postData);
            pw.flush();
            pw.close();
            InputStream is = urlConnection.getInputStream();
            StringBuilder sb = new StringBuilder();
            br = new BufferedReader(new InputStreamReader(is));
            String line;
            while (true) {
                line = br.readLine();
                if (line == null) {
                    break;
                }
                sb.append(line).append("\n");
            }
            if (sb.length() == 0) {
                Log.e(context.getString(R.string.app_name), "Util.httpPost: Empty Response\n");
                sendACRAReport("Util.httpPost", "Empty Response", urlString);
            } else {
                Log.e(context.getString(R.string.app_name), "Util.httpPost response:\n" + sb.toString().trim());
                return sb.toString();
            }
        } catch (IOException ioe) {
            Log.e(context.getString(R.string.app_name), "Util.httpPost: IOException");
            sendACRAReport("Util.httpPost", "IOException", "error", ioe);
            ioe.printStackTrace();
            throw new IOException(ioe); //throw to method that called this.
        } finally {
            //*cough* boilerplate *cough*
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    Log.e(context.getString(R.string.app_name), "Error closing BufferedReader", e);
                    sendACRAReport("Util.httpPost", "Error closing BufferedReader (IOException)", urlString, e);
                    e.printStackTrace();
                }
            }
            if (pw != null) {
                pw.close();
            }
        }
        return null;
    }

    /**
     * Send ACRA report
     *
     * @param errormethod
     * @param errorname
     * @param errordata
     */
    public static void sendACRAReport(String errormethod, String errorname, String errordata) {
        ACRA.getErrorReporter().putCustomData("custom_errormethod", errormethod);
        ACRA.getErrorReporter().putCustomData("custom_errorname", errorname);
        ACRA.getErrorReporter().putCustomData("custom_errordata", errordata);
        ACRA.getErrorReporter().handleException(new RuntimeException(errormethod + ": " + errorname + " \n" + errordata));
    }

    /**
     * Send ACRA report
     *
     * @param errormethod
     * @param errorname
     * @param errordata
     */
    public static void sendACRAReport(String errormethod, String errorname, String errordata, Exception e) {
        ACRA.getErrorReporter().putCustomData("custom_errormethod", errormethod);
        ACRA.getErrorReporter().putCustomData("custom_errorname", errorname);
        ACRA.getErrorReporter().putCustomData("custom_errordata", errordata);
        ACRA.getErrorReporter().handleException(e);
    }

    public static void sendACRAReport(String errormethod, Exception e) {
        sendACRAReport(errormethod, e.toString(), e.getMessage(), e);
    }

    /**
     * SHA1 hash
     *
     * @param toHash
     * @return hash
     */
    public static String sha1Hash(String toHash) {
        // http://stackoverflow.com/questions/5980658/how-to-sha1-hash-a-string-in-android
        String hash = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = toHash.getBytes("UTF-8");
            digest.update(bytes, 0, bytes.length);
            bytes = digest.digest();

            // This is ~55x faster than looping and String.formating()
            hash = bytesToHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleException(e);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleException(e);
        }
        return hash;
    }

    //using lower-case now.
    final protected static char[] hexArray = "0123456789abcdef".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        // http://stackoverflow.com/questions/5980658/how-to-sha1-hash-a-string-in-android
        // http://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static void fixNavigationView(Activity context, NavigationView navigationView) {
        //remove items that are hidden by SharedPreferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Menu navigationMenu = navigationView.getMenu();
        if (!isTester(context)) {
            //testers get all sections
            if (!prefs.getBoolean("section_schedule_enabled", false)) {
                navigationMenu.removeItem(R.id.navigation_item_schedule);
            }
            if (!prefs.getBoolean("section_browse_enabled", false)) {
                navigationMenu.removeItem(R.id.navigation_item_browse);
            }
            if (!prefs.getBoolean("section_map_enabled", false)) {
                navigationMenu.removeItem(R.id.navigation_item_map);
            }
            if (!prefs.getBoolean("section_qrhunt_enabled", false)) {
                navigationMenu.removeItem(R.id.navigation_item_qrhunt);
            }
            if (!prefs.getBoolean("section_login_enabled", false)) {
                navigationMenu.removeItem(R.id.navigation_item_login);
            }
            if (!prefs.getBoolean("section_about_enabled", true)) {
                navigationMenu.removeItem(R.id.navigation_item_about);
            }
            if (!prefs.getBoolean("section_news_enabled", true)) {
                navigationMenu.removeItem(R.id.navigation_item_news);
            }
        }

        //set the current section selected
        if (context instanceof ScheduleActivity) {
            navigationMenu.findItem(R.id.navigation_item_schedule).setChecked(true);
        }
        if (context instanceof EventListActivity) {
            navigationMenu.findItem(R.id.navigation_item_browse).setChecked(true);
        }
        if (context instanceof MapActivity) {
            navigationMenu.findItem(R.id.navigation_item_map).setChecked(true);
        }
        if (context instanceof QrHuntActivity) {
            navigationMenu.findItem(R.id.navigation_item_qrhunt).setChecked(true);
        }
        if (context instanceof LoginActivity) {
            navigationMenu.findItem(R.id.navigation_item_login).setChecked(true);
        }
        if (context instanceof AboutActivity) {
            navigationMenu.findItem(R.id.navigation_item_about).setChecked(true);
        }
        if (context instanceof NewsActivity) {
            navigationMenu.findItem(R.id.navigation_item_news).setChecked(true);
        }

        //Set image same as splash / launcher icon
        //http://stackoverflow.com/questions/33194594/navigationview-get-find-header-layout
        ((ImageView) navigationView.getHeaderView(0).findViewById(R.id.nav_header_image)).setImageResource(getAppIconResourceId(context));
    }

    public static int getAppIconResourceId(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        //set default here
        return prefs.getInt("app_icon", R.drawable.ic_launcher_hwcon2016_4_web);
    }

    public static int getPlaceholderIconResourceId(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        //set default here
        return prefs.getInt("app_icon_mipmap", R.mipmap.ic_launcher_hwcon2016_4);
    }

    public static void navigationItemSelected(Activity thisAct, NavigationView navigationView, DrawerLayout drawerLayout, MenuItem menuItem) {
        //http://www.android4devs.com/2015/06/navigation-view-material-design-support.html
        if (!menuItem.isChecked()) {
            menuItem.setChecked(true);
        }
        Intent i = new Intent();
        navigateFromNavDrawer(thisAct, i.setClass(thisAct, getSectionIdClass(menuItem.getItemId())));
        drawerLayout.closeDrawers();
    }

    public static boolean isTester(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c).getBoolean("tester", false);
    }

    public static void updateSettingsFromGCM(Context context, Bundle data) {
        try {
            SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(context).edit();
            JSONObject j = new JSONObject(data.getString("message", "{}"));
            /*
            {
                "tester": {
                    "type": "boolean",
                    "value": true
                },
                "a": {
                    "type": "string",
                    "value": "koekjes"
                }
            }
             */
            Iterator<String> keys = j.keys();
            while (keys.hasNext()) {
                String settingName = keys.next();
                JSONObject setting = j.getJSONObject(settingName);
                if ("icon".equals(settingName)){
                    AppIconDialogFragment.changeAppIcon(context, setting.getInt("value"));
                    continue;
                }
                if ("boolean".equals(setting.getString("type"))) {
                    e.putBoolean(settingName, setting.getBoolean("value")).commit();
                } else if ("string".equals(setting.getString("type"))) {
                    e.putString(settingName, setting.getString("value")).commit();
                } else if ("int".equals(setting.getString("type"))) {
                    e.putInt(settingName, setting.getInt("value")).commit();
                }
            }
        } catch (Exception ex) {
            //ignore
            sendACRAReport("Util.updateSettingsFromGCM", ex);
        }
    }
}
