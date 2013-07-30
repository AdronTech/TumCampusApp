package de.tum.in.tumcampusapp.fragments;

import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import de.tum.in.tumcampusapp.R;
import de.tum.in.tumcampusapp.auxiliary.Utils;
import de.tum.in.tumcampusapp.models.managers.CalendarManager;

/**
 * Fragment for each category-page.
 */
public class CalendarSectionFragment extends Fragment {
	private Activity activity;

	private Date currentDate = new Date();
	private ArrayList<RelativeLayout> eventList = new ArrayList<RelativeLayout>();
	private RelativeLayout eventView;
	private CalendarManager kalMgr;
	private RelativeLayout mainScheduleLayout;

	public CalendarSectionFragment() {
		kalMgr = new CalendarManager(getActivity());
	}

	private RelativeLayout inflateEventView() {
		LayoutInflater layoutInflater = (LayoutInflater) activity
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		return (RelativeLayout) layoutInflater.inflate(
				R.layout.layout_time_entry, null);
	}

	private LayoutParams initLayoutParams(float hours) {
		int oneHourHeight = (int) activity.getResources().getDimension(
				R.dimen.time_gap)
				+ (int) activity.getResources().getDimension(
						R.dimen.time_line_thickness);
		int height = (int) (oneHourHeight * hours);
		return new LayoutParams(LayoutParams.MATCH_PARENT, height);
	}

	@SuppressWarnings("unchecked")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_calendar_section,
				container, false);

		activity = getActivity();

		String date = getArguments().getString("date");
		currentDate = Utils.getDateTimeISO(date);

		mainScheduleLayout = (RelativeLayout) rootView
				.findViewById(R.id.main_schedule_layout);
		
		updateCalendarView();

		return rootView;
	}
	
	@SuppressWarnings("deprecation")
	private void parseEvents() {
		Date dateStart;
		Date dateEnd;
		float start;
		float end;
		float hours;

		// Cursor cursor = kalMgr.getFromDbForDate(currentDate);
		Cursor cursor = kalMgr.getAllFromDb();
		while (cursor.moveToNext()) {
			final String status = cursor.getString(1);
			final String strStart = cursor.getString(5);
			final String strEnd = cursor.getString(6);

			int year = Integer.valueOf(currentDate.getYear()) + 1900;
			int month = Integer.valueOf(currentDate.getMonth()) + 1;
			int day = Integer.valueOf(currentDate.getDate());

			String requestedDateString = year + "-"
					+ String.format("%02d", month) + "-"
					+ String.format("%02d", day);

			if (strStart.contains(requestedDateString)
					&& !status.equals("CANCEL")) {
				eventView = inflateEventView();
				dateStart = Utils.getISODateTime(strStart);
				dateEnd = Utils.getISODateTime(strEnd);

				start = dateStart.getHours() * 60 + dateStart.getMinutes();
				end = dateEnd.getHours() * 60 + dateStart.getMinutes();

				hours = (end - start) / 60f;

				// Set params to eventLayout
				LayoutParams params = initLayoutParams(hours);
				setStartOfEntry(params, start / 60f);
				setText(eventView, cursor.getString(3));

				eventView.setLayoutParams(params);
				eventList.add(eventView);
			}
		}
	}

	private void setStartOfEntry(LayoutParams params, float start) {
		int oneHourHeight = (int) activity.getResources().getDimension(
				R.dimen.time_one_hour);
		int marginTop = (int) (oneHourHeight * start);
		params.setMargins(0, marginTop, 0, 0);
	}

	private void setText(RelativeLayout entry, String text) {
		TextView textView = (TextView) entry.findViewById(R.id.entry_title);
		textView.setText(text);
	}

	private void updateCalendarView() {
		eventList.clear();
		parseEvents();
		mainScheduleLayout.removeAllViews();
		Log.i("Lectures found", String.valueOf(eventList.size()));
		for (RelativeLayout event : eventList) {
			mainScheduleLayout.addView(event);
		}
	}
}