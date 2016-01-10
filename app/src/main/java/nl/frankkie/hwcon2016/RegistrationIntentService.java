package nl.frankkie.hwcon2016;

import android.app.IntentService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;

import nl.frankkie.hwcon2016.util.GcmUtil;
import nl.frankkie.hwcon2016.util.Util;

/**
 * Created by FrankkieNL on 1/10/2016.
 */
public class RegistrationIntentService extends IntentService {

    private static final String TAG = "RegIntentService";
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public RegistrationIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        InstanceID instanceID = InstanceID.getInstance(this);
        try {
            String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);

            Log.i(TAG, "GCM Registration Token: " + token);

            GcmUtil.gcmSendRegIdToServer(this,token);
        } catch (IOException e) {
            Util.sendACRAReport("MyInstanceIDListenerService.onHandleIntent", e.toString(), e.getMessage(), e);
            e.printStackTrace();
        } catch (PackageManager.NameNotFoundException e) {
            Util.sendACRAReport("MyInstanceIDListenerService.onHandleIntent", e.toString(), e.getMessage(), e);
            e.printStackTrace();
        }

    }
}
