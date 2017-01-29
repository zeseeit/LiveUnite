package com.liveunite.chat.helper;

import android.content.Context;
import android.content.Intent;

import com.liveunite.LiveUniteMains.LiveUnite;
import com.liveunite.R;

/**
 * Created by Ankit on 1/18/2017.
 */

public class HomeLaucherSetup {

    public HomeLaucherSetup() {
    }

    public void setHome(Context context) {

        if(LiveUnite.getInstance().getPreferenceManager().homeIconCreated())
            return;

        Intent shortcutIntent = new Intent();
        shortcutIntent.setClassName("com.liveunite", "com.liveunite.activities.Splash");
        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "LiveUnite");
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(context, R.mipmap.ic_launcher));
        addIntent.putExtra("duplicate", false);
        addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        context.sendBroadcast(addIntent);

        LiveUnite.getInstance().getPreferenceManager().setHomeIconCreated(true);

    }
}
