package de.tum.in.tumcampusapp.services;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import de.tum.in.tumcampusapp.R;
import de.tum.in.tumcampusapp.activities.GradesActivity;
import de.tum.in.tumcampusapp.auxiliary.Const;
import de.tum.in.tumcampusapp.auxiliary.Utils;
import de.tum.in.tumcampusapp.models.ExamList;
import de.tum.in.tumcampusapp.tumonline.TUMOnlineRequest;

/** Service used to silence the mobile during lectures */
public class BackgrdService extends IntentService {

	/**
	 * Interval in milliseconds to check for current lectures
	 */
	public static int CHECK_INTERVAL = 60000 * 60*24; // 1 Day
	
	public static final String Background_SERVICE = "BackgrdService";

	/**
	 * default init (run intent in new thread)
	 */
	public BackgrdService() {
		super(Background_SERVICE);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Utils.log(""); // log create
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Utils.log(""); // log destroy
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		while(true){
			try {
				// if background mode is enabled in settings
				if (Utils.getSettingBool(this, Const.BACKGROUND_MODE)) {
					//fetching xml from tum online
					TUMOnlineRequest requestHandler = new TUMOnlineRequest(
							Const.NOTEN, getApplicationContext());
					String rawResponse = requestHandler.fetch();
					Serializer serializer = new Persister();
					ExamList examList = null;
					// Deserializes XML response
					examList = serializer.read(ExamList.class, rawResponse);
					//generating notification
					generateNotification(examList);
					// wait until next check

				}

				synchronized (this) {
					wait(CHECK_INTERVAL);
				} 
			}
			catch(Exception e){
				Utils.log(e, "");
			}
		}


	}
	
	private void generateNotification(ExamList examList) {

		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);
		int gradeCount = settings.getInt(Const.Grade_Count, 0);
		//Log.d("Grade Count", "" + gradeCount);
		int newSize = examList.getExams().size();
		if (gradeCount != 0) {
			if (newSize > gradeCount) {
				Editor editor = settings.edit();
				editor.putInt(Const.Grade_Count, newSize);
				editor.commit();
				int icon = R.drawable.ic_notification;
				// Generating Notification
				NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
						this).setSmallIcon(icon)
						.setContentTitle(getString(R.string.notification_title))
						.setContentText(getString(R.string.notification_content));
				// Creates an explicit intent for an Activity in your app
				Intent resultIntent = new Intent(this, GradesActivity.class);

				// The stack builder object will contain an artificial back
				// stack for the
				// started Activity.
				// This ensures that navigating backward from the Activity leads
				// out of
				// your application to the Home screen.
				TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
				// Adds the back stack for the Intent (but not the Intent
				// itself)
				stackBuilder.addParentStack(GradesActivity.class);
				// Adds the Intent that starts the Activity to the top of the
				// stack
				stackBuilder.addNextIntent(resultIntent);
				PendingIntent resultPendingIntent = stackBuilder
						.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
				mBuilder.setContentIntent(resultPendingIntent);
				NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				int mId = 0;
				// mId allows you to update the notification later on.
				mNotificationManager.notify(mId, mBuilder.build());
			}
		} else {
			Editor editor = settings.edit();
			editor.putInt(Const.Grade_Count, newSize);
			editor.commit();
		}
	}
}