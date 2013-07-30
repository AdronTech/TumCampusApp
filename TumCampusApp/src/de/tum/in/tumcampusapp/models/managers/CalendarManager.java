package de.tum.in.tumcampusapp.models.managers;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import de.tum.in.tumcampusapp.auxiliary.Utils;
import de.tum.in.tumcampusapp.models.CalendarRow;
import de.tum.in.tumcampusapp.models.CalendarRowSet;

public class CalendarManager {
	public static int TIME_TO_SYNC = 86400; // 1 day
	private SQLiteDatabase db;

	public CalendarManager(Context context) {
		db = DatabaseManager.getDb(context);

		// create table if needed
		db.execSQL("CREATE TABLE IF NOT EXISTS kalendar_events ("
				+ "nr VARCHAR PRIMARY KEY, status VARCHAR, url VARCHAR, "
				+ "title VARCHAR, description VARCHAR, dtstart VARCHAR, dtend VARCHAR, "
				+ "location VARCHAR, longitude VARCHAR, latitude VARCHAR)");
	}

	public Cursor getAllFromDb() {
		return db.rawQuery("SELECT * FROM kalendar_events", null);
	}

	// TODO not working
	@SuppressWarnings("deprecation")
	public Cursor getFromDbForDate(Date date) {
		Log.d("test", "%" + Utils.getDateTimeString(date) + "%");
		return db
				.rawQuery(
						"SELECT title, dtstart, dtend FROM kalendar_events WHERE dtstart LIKE ?",
						new String[] { "%" + Utils.getDateTimeString(date)
								+ "%" });
	}

	public void importKalendar(String rawResponse) {
		// reader for xml
		Serializer serializer = new Persister();

		// KalendarRowSet will contain list of events in KalendarRow
		CalendarRowSet myKalendarList = new CalendarRowSet();

		myKalendarList.setKalendarList(new ArrayList<CalendarRow>());

		try {
			// reading xml
			myKalendarList = serializer.read(CalendarRowSet.class, rawResponse);
			Iterator itr = myKalendarList.getKalendarList().iterator();
			while (itr.hasNext()) {
				CalendarRow row = (CalendarRow) itr.next();
				// insert into database
				try {
					replaceIntoDb(row);
				} catch (Exception e) {
					boolean success = false;
					Log.d("SIMPLEXML", "Error in field: " + e.getMessage());
					e.printStackTrace();
				}
			}
			SyncManager.replaceIntoDb(db, this);
		} catch (Exception e) {
			boolean success = false;
			Log.d("SIMPLEXML", "wont work: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public boolean needsSync() {
		return SyncManager.needSync(db, this, TIME_TO_SYNC);
	}

	/**
	 * Removes all cache items
	 */
	public void removeCache() {
		db.execSQL("DELETE FROM kalendar_events");
	}

	public void replaceIntoDb(CalendarRow row) throws Exception {
		Utils.log(row.toString());

		if (row.getNr().length() == 0)
			throw new Exception("Invalid id.");

		if (row.getTitle().length() == 0)
			throw new Exception("Invalid lecture Title.");

		if (row.getGeo() != null)
			db.execSQL(
					"REPLACE INTO kalendar_events (nr, status, url, title, "
							+ "description, dtstart, dtend, location, longitude, latitude) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
					new String[] { row.getNr(), row.getStatus(), row.getUrl(),
							row.getTitle(), row.getDescription(),
							row.getDtstart(), row.getDtend(),
							row.getLocation(), row.getGeo().getLongitude(),
							row.getGeo().getLatitude() });
		else
			db.execSQL(
					"REPLACE INTO kalendar_events (nr, status, url, title, "
							+ "description, dtstart, dtend, location) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
					new String[] { row.getNr(), row.getStatus(), row.getUrl(),
							row.getTitle(), row.getDescription(),
							row.getDtstart(), row.getDtend(), row.getLocation() });
	}
}
