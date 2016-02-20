package nl.frankkie.hwcon2016.util;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import nl.frankkie.hwcon2016.R;

/**
 * Created by FrankkieNL on 2/14/2016.
 */
public class MapDownloadIntentService extends IntentService {

    public MapDownloadIntentService() {
        super("Convention Map Download");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        intent.getBooleanArrayExtra("force");
        doStuff(intent.getBooleanExtra("force", false));
    }

    /**
     * force=true will download the map again, even when you have the latest version already
     */
    public void doStuff(boolean force) {
        //check, is it needed to download
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.contains("map_download_url")) {
            Log.e(getString(R.string.app_name), "No map download url");
            return; //we're done here
        }
        int latestMapVersion = prefs.getInt("map_latest_version", -1);
        int downloadedMapVersion = prefs.getInt("map_downloaded_version", -1);
        if (downloadedMapVersion == latestMapVersion && !force) {
            //latest version already downloaded!
            Log.i(getString(R.string.app_name), "latest map version already downloaded");
            return;
        }

        String urlToDownload = "";
        HttpURLConnection urlConnection = null;
        File mapZipFile = new File(getExternalFilesDir(null), "/mapDownload.zip");
        urlToDownload = prefs.getString("map_download_url", "");

        try {
            URL url = new URL(urlToDownload);
            //sending regId to update the lastConnected-status on the database
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
            if (inputStream == null) {
                //Apparently there is no inputstream.
                //We're done here
                return;
            }

            if (!mapZipFile.exists()) {
                mapZipFile.createNewFile();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(mapZipFile);
            byte[] buffer = new byte[8192];
            int byteCount = 0;
            while ((byteCount = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, byteCount);
            }
            fileOutputStream.flush();
            fileOutputStream.close();
            inputStream.close();


        } catch (IOException e) {
            Log.e("Convention", "Error while downloading http data from " + urlToDownload, e);
            Util.sendACRAReport("MapDownloadIntentService.doStuff", "Error while downloading (IOException)", urlToDownload, e);
            return;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        //Unzip
        try {
            //delete previous files
            File mapDir = new File(getExternalFilesDir(null), "/map/");
            mapDir.mkdirs(); //make folder
            deleteContentsOfDir(mapDir);
            unzip(mapZipFile, mapDir);
        } catch (IOException e) {
            Util.sendACRAReport("MapDownloadIntentService.doStuff", e);
            return;
        }

        //set downloaded version
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("map_downloaded_version", latestMapVersion).commit();

        //send local broadcast
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        Intent i = new Intent("hwcon2016_map");
        i.setData(Uri.parse("hwcon2016://map/"));
        i.putExtra("action", "mapDownloaded");
        localBroadcastManager.sendBroadcast(i);
    }

    public static void deleteContentsOfDir(File dir) {
        for (File f : dir.listFiles()) {
            if (f.isDirectory())
                deleteContentsOfDir(f);
            else
                f.delete();
        }
    }

    public static void unzip(File zipFile, File targetDirectory) throws IOException {
        //http://stackoverflow.com/questions/3382996/how-to-unzip-files-programmatically-in-android
        ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(zipFile)));
        try {
            ZipEntry ze;
            int count;
            byte[] buffer = new byte[8192];
            while ((ze = zis.getNextEntry()) != null) {
                File file = new File(targetDirectory, ze.getName());
                File dir = ze.isDirectory() ? file : file.getParentFile();
                if (!dir.isDirectory() && !dir.mkdirs())
                    throw new FileNotFoundException("Failed to ensure directory: " +
                            dir.getAbsolutePath());
                if (ze.isDirectory())
                    continue;
                FileOutputStream fout = new FileOutputStream(file);
                try {
                    while ((count = zis.read(buffer)) != -1)
                        fout.write(buffer, 0, count);
                } finally {
                    fout.close();
                }
            /* if time should be restored as well
            long time = ze.getTime();
            if (time > 0)
                file.setLastModified(time);
            */
            }
        } finally {
            zis.close();
        }
    }
}
