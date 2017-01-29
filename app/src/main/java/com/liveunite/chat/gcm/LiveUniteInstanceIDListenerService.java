package com.liveunite.chat.gcm;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.iid.InstanceIDListenerService;

/**
 * Created by Ankit on 12/7/2016.
 */

public class LiveUniteInstanceIDListenerService extends InstanceIDListenerService {
    private static final String TAG = LiveUniteInstanceIDListenerService.class.getSimpleName();

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. This call is initiated by the
     * InstanceID provider.
     */
    @Override
    public void onTokenRefresh() {
        Log.e(TAG, "onTokenRefresh");
        // Fetch updated Instance ID token and notify our app's server of any changes (if applicable).
        Intent intent = new Intent(this, LiveUniteGCMIntentService.class);
        startService(intent);
    }

}
