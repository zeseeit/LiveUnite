package com.liveunite.chat.helper;

import java.util.Calendar;

/**
 * Created by Ankit on 12/27/2016.
 */

public class LiveUniteTime {

    private static LiveUniteTime mInstance;

    public LiveUniteTime() {
    }

    public static LiveUniteTime getInstance() {
        if (mInstance == null) {
            mInstance = new LiveUniteTime();
        }
        return mInstance;
    }

    public String getDateTime() {

        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.YEAR) + ":" + (calendar.get(Calendar.MONTH)) + ":" + calendar.get(Calendar.DAY_OF_MONTH) + ":" + calendar.get(Calendar.HOUR) + ":" + calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND)+":"+calendar.get(Calendar.AM_PM);

    }

}
