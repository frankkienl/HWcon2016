package nl.frankkie.hwcon2016.util;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;


import org.acra.ACRA;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

import nl.frankkie.hwcon2016.R;

/**
 * Created by FrankkieNL on 20-12-2014.
 */
public class GcmUtil {
    //GCM
    public static final String GCM_PROJECT_ID = "445536900909";

    /**
     * Gives GCM Reg ID or "" on error (or not registered yet)
     *
     * @param context
     * @return GCM Reg ID or "" on error (or not registered yet)
     */
    public static String gcmGetRegId(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            if (prefs.getInt("gcm_app_version", Integer.MIN_VALUE) !=
                    context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode) {
                Log.e(context.getString(R.string.app_name), "gcm wrong app version");
                return "";
            }
        } catch (PackageManager.NameNotFoundException nnfe) {
            Log.e(context.getString(R.string.app_name), "This app is apparently not installed. Weird.\n" + nnfe);
            Log.e(context.getString(R.string.app_name), "gcm package does not exist");
            Util.sendACRAReport("GcmUtil.gcmGetRegId", "This app is not installed", "gcm_reg_id", nnfe);
            return "";
        }
        String regId = prefs.getString("gcm_reg_id", "");
        if ("".equals(regId)) {
            Log.e(context.getString(R.string.app_name), "gcm not in SharedPreferences");
        }
        return regId;
    }

    /**
     * This is called from the AsyncTask started in gcmRegister
     *
     * @param context
     * @param regId
     * @throws java.io.IOException will be handled in AsyncTask :P
     */
    public static void gcmSendRegIdToServer(Context context, String regId) throws IOException, PackageManager.NameNotFoundException {
        HttpURLConnection urlConnection = null;
        BufferedReader br = null;
        PrintWriter pw = null;
        //String postData = "regId=" + regId + "&deviceName=" + URLEncoder.encode(getDeviceName(), "UTF-8");
        /*
        _id	regId	userId	androidVersion	androidVersionInt	appVersion	brand	model	lastConnect	locale	comment
         */
        String locale = Locale.getDefault().toString();
        PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        int appVersion = pInfo.versionCode;
        String postData = "regId=" + regId + "&androidVersion=" + Build.VERSION.RELEASE + "&androidVersionInt=" + Build.VERSION.SDK_INT + "&brand=" + Build.BRAND + "&model=" + Build.MODEL + "&locale=" + locale + "&appVersion=" + appVersion;
        try {
            //For rant, see nl.frankkie.hwcon2016.util.Util
            URL url = new URL("https://wofje.8s.nl/hwcon2016/api/v1/gcmregister.php");
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
                Log.e(context.getString(R.string.app_name), "gcmSendRegId: Empty Response");
                Util.sendACRAReport("GcmUtil.gcmSendRegIdToServer", "Empty Response", url.toString() + "\n" + postData);
            } else {
                Log.e(context.getString(R.string.app_name), "gcmSendRegId response:\n" + sb.toString().trim());
                if (!"ok".equals(sb.toString().trim())) { //remove whitespace
                    //Server should return 'ok' onSucces, something else otherwise
                    //So some error has occured
                    IOException e = new IOException("gcmSendRegId: Server did not send 'ok', something must be wrong.");
                    Util.sendACRAReport("GcmUtil.gcmSendRegIdToServer", "Server did not send 'ok', something must be wrong.", url.toString() + "\n" + postData, e);
                    throw e;
                }
            }
        } catch (IOException ioe) {
            Log.e(context.getString(R.string.app_name), "gcmSendRegId: IOException");
            Util.sendACRAReport("GcmUtil.gcmSendRegIdToServer", "", "", ioe);
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
                    Util.sendACRAReport("GcmUtil.gcmSendRegIdToServer", "Error closing BufferedReader", "", e);
                    e.printStackTrace();
                }
            }
            if (pw != null) {
                pw.close();
            }
        }
    }

    public static void gcmHandleMessage(Context context, Bundle data) {
        String action = data.getString("action");
        if ("downloadConventionData".equals(action)) {
            Util.syncConventionData(context);
        } else if ("notification".equals(action)) {
            String message = data.getString("message");
            Util.showNotification(context, message);
        } else if ("downloadFavorites".equals(action)) {
            Util.syncData(context, Util.SYNCFLAG_DOWNLOAD_FAVORITES);
        } else if ("uploadFavorites".equals(action)){
            Util.syncData(context, Util.SYNCFLAG_UPLOAD_FAVORITES);   
        } else if ("generateErrorReport".equals(action)) {
            ACRA.getErrorReporter().handleException(new RuntimeException("Error Report triggered by GCM"));
        } else if ("uploadQrFound".equals(action)){
            Util.syncData(context,Util.SYNCFLAG_UPLOAD_QRFOUND);
        } else if ("downloadQrFound".equals(action)){
            Util.syncData(context, Util.SYNCFLAG_DOWNLOAD_QRFOUND);
        } else if ("updateSettings".equals(action)){
            Util.updateSettingsFromGCM(context, data);
        }
    }

   }
