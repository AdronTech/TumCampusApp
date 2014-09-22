package de.tum.in.tumcampus.adapters;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import de.tum.in.tumcampus.R;
import de.tum.in.tumcampus.models.LectureAppointmentsRow;

/**
 * Generates the output of the ListView on the {@link de.tum.in.tumcampus.activities.LecturesAppointmentsActivity} activity.
 */
public class LectureAppointmentsListAdapter extends BaseAdapter {

	// the layout
	static class ViewHolder {
		TextView tvTerminBetreff;
		TextView tvTerminOrt;
		TextView tvTerminZeit;
	}

	// list of Appointments to one lecture
	private static List<LectureAppointmentsRow> appointmentList;

	private final LayoutInflater mInflater;

	public LectureAppointmentsListAdapter(Context context, List<LectureAppointmentsRow> results) {
		appointmentList = results;
		mInflater = LayoutInflater.from(context);
	}

	@Override
	public int getCount() {
		return appointmentList.size();
	}

	@Override
	public Object getItem(int position) {
		return appointmentList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@SuppressWarnings("deprecation")
    @Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.activity_lecturesappointments_listview, parent, false);

			// save UI elements in view holder
            holder = new ViewHolder();
			holder.tvTerminZeit = (TextView) convertView.findViewById(R.id.tvTerminZeit);
			holder.tvTerminOrt = (TextView) convertView.findViewById(R.id.tvTerminOrt);
			holder.tvTerminBetreff = (TextView) convertView.findViewById(R.id.tvTerminBetreff);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		LectureAppointmentsRow lvItem = appointmentList.get(position);

		// only show if lecture has a title and enough info
		if (lvItem != null) {
			holder.tvTerminOrt.setText(lvItem.getOrt());
			String line2 = lvItem.getArt();
			// only show betreff if available
			if (lvItem.getTermin_betreff() != null) {
				line2 += " - " + lvItem.getTermin_betreff();
			}
			holder.tvTerminBetreff.setText(line2);

			// zeitdarstellung setzen
			// parse dates
			// this is the template for the date in the xml file
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			try {
				Date start = formatter.parse(lvItem.getBeginn_datum_zeitpunkt());
				Date ende = formatter.parse(lvItem.getEnde_datum_zeitpunkt());

				// make two calendar instances
				Calendar cnow = Calendar.getInstance();
				Calendar cstart = Calendar.getInstance();
				cstart.setTime(start);

				// date formats for the day output
				SimpleDateFormat endHoursOutput = new SimpleDateFormat("HH:mm");
				SimpleDateFormat StartDateOutput = new SimpleDateFormat("EEE dd.MM.yyyy HH:mm");
                SimpleDateFormat EndDateOutput = new SimpleDateFormat("dd.MM.yyyy HH:mm");

				// output if same day: we only show the date once
                String output;
				if (start.getMonth() == ende.getMonth() && start.getDate() == ende.getDate()) {
					output = StartDateOutput.format(start) + " - " + endHoursOutput.format(ende);
				} else {
					// show it normally
					output = StartDateOutput.format(start) + " - " + EndDateOutput.format(ende);
				}

				// grey it, if in past
				if (cstart.before(cnow)) {
					output = "<font color=\"#444444\">" + output + "</font>";
				}

				holder.tvTerminZeit.setText(Html.fromHtml(output));

			} catch (Exception ex) {
				holder.tvTerminZeit.setText(lvItem.getBeginn_datum_zeitpunkt()
						+ " - " + lvItem.getEnde_datum_zeitpunkt());
			}

		}
		return convertView;
	}
}
