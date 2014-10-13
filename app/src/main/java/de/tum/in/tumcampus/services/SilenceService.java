package de.tum.in.tumcampus.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;

import de.tum.in.tumcampus.auxiliary.Const;
import de.tum.in.tumcampus.auxiliary.Utils;
import de.tum.in.tumcampus.models.managers.CalendarManager;

/** Service used to silence the mobile during lectures */
public class SilenceService extends IntentService {

	/**
	 * Interval in milliseconds to check for current lectures
	 */
	private static final int CHECK_INTERVAL = 60000 * 15; // 15 Minutes
	private static final String SILENCE_SERVICE = "SilenceService";

	/**
	 * default init (run intent in new thread)
	 */
	public SilenceService() {
		super(SILENCE_SERVICE);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Utils.log("SilenceService has started");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Utils.log("SilenceService has stopped");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		// loop until silence mode gets disabled in settings
		while (true) {
			try {
				if (Utils.getSettingBool(this, Const.SILENCE_SERVICE, false)) {
					Utils.log("SilenceService enabled, checking for lectures ...");

					AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

					CalendarManager calendarManager = new CalendarManager(this);
					if (!calendarManager.hasLectures()) {
						Utils.logv("No lectures available");
						return;
					}

					Cursor cursor = calendarManager.getCurrentFromDb();
					Utils.log("Current lectures: " + String.valueOf(cursor.getCount()));
					
					if (cursor.getCount() != 0) {
						// if current lecture(s) found, silence the mobile
						Utils.setInternalSetting(this, Const.SILENCE_ON, true);

                        // Set into silent mode
                        String mode = Utils.getSetting(this, "silent_mode_set_to", "0");
                        if(mode.equals("0")) {
                            Utils.log("set ringer mode: vibration");
                            am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                        } else {
                            Utils.log("set ringer mode: silent");
                            am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                        }
					} else if (Utils.getInternalSettingBool(this, Const.SILENCE_ON, false)) {
						// default: no silence
						Utils.log("set ringer mode: normal");
						am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
						Utils.setInternalSetting(this, Const.SILENCE_ON, false);
					}
					cursor.close();
				}
				// wait until next check
				synchronized (this) {
					wait(CHECK_INTERVAL);
				}
			} catch (Exception e) {
				Utils.log(e, "");
			}
		}
	}
}