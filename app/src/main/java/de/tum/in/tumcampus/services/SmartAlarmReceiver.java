package de.tum.in.tumcampus.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.io.Serializable;

import de.tum.in.tumcampus.auxiliary.AlarmSchedulerTask;
import de.tum.in.tumcampus.models.SmartAlarmInfo;

public class SmartAlarmReceiver extends BroadcastReceiver {
    public static final int PRE_ALARM_DIFF = 1;

    public static final int REQUEST_PRE_ALARM = 0;
    public static final int REQUEST_ALARM = 1;

    public static final String INFO = "INFO";
    public static final String REQUEST_CODE = "REQUEST_CODE";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getIntExtra(REQUEST_CODE, REQUEST_PRE_ALARM) == REQUEST_PRE_ALARM) {
            handlePreAlarm(context, intent);
        } else {
            handleAlarm(context, intent);
        }
    }

    /**
     * Recalculate public transport route in case of delays etc.
     * @param c Context
     * @param intent Intent containing a SmartAlarmInfo object in extra field INFO
     */
    private void handlePreAlarm(Context c, Intent intent) {
        // TODO: show route info on widget

        SmartAlarmInfo sai = (SmartAlarmInfo) intent.getExtras().get(INFO);
        new AlarmSchedulerTask(c, sai, SmartAlarmReceiver.REQUEST_ALARM).execute();
    }

    /**
     * Displays alarm on screen and vibrates phone
     * @param c Context
     * @param intent Intent containing a SmartAlarmInfo object in extra field INFO
     */
    private void handleAlarm(Context c, Intent intent) {
        // TODO: update route info on widget

        Intent showAlert = new Intent(c, SmartAlarmService.class);
        if (intent.hasExtra(INFO)) {
            showAlert.putExtra(INFO, (Serializable) intent.getExtras().get(INFO));
        }
        c.startService(showAlert);
    }

    // TODO: add reboot handling (schedule alarm again)
    // TODO: add long time no lectures handling (schedule PreAlarmScheduler)
}
