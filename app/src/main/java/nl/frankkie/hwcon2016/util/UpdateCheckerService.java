package nl.frankkie.hwcon2016.util;

import android.app.IntentService;
import android.content.Intent;

/**
 * Created by FrankkieNL on 1/23/2016.
 */
public class UpdateCheckerService extends IntentService {

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public UpdateCheckerService(String name) {
        super("UpdateChecker HWcon2016");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        //TODO: update checker
        // check versionnumber on server
        // if update available, download apk
        // install
        
    }
}
