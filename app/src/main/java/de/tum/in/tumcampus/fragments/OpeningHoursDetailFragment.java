package de.tum.in.tumcampus.fragments;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;

import java.util.regex.Pattern;

import de.tum.in.tumcampus.R;
import de.tum.in.tumcampus.activities.OpeningHoursDetailActivity;
import de.tum.in.tumcampus.activities.OpeningHoursListActivity;
import de.tum.in.tumcampus.auxiliary.Const;
import de.tum.in.tumcampus.models.managers.OpenHoursManager;

/**
 * A fragment representing a single Item detail screen. This fragment is either
 * contained in a {@link OpeningHoursListActivity} in two-pane mode (on tablets)
 * or a {@link OpeningHoursDetailActivity} on handsets.
 *
 * NEEDS: ARG_ITEM_ID and ARG_ITEM_CONTENT set in arguments
 */
public class OpeningHoursDetailFragment extends Fragment implements ViewBinder {
	public static final String ARG_ITEM_ID = "item_id";
    public static final String ARG_ITEM_CONTENT = "item_content";
	public static final String TWO_PANE = "two_pane";

	private int mItemId;
    private String mItemContent;

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public OpeningHoursDetailFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getArguments().containsKey(ARG_ITEM_ID)) {
			mItemId = getArguments().getInt(ARG_ITEM_ID);
            mItemContent = getArguments().getString(ARG_ITEM_CONTENT);
		}
		if (getArguments().containsKey(TWO_PANE)) {
			if (!getArguments().getBoolean(TWO_PANE)) {
				getActivity().setTitle(mItemContent);
			}
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_item_detail, container, false);

		// click on category in list
		OpenHoursManager lm = new OpenHoursManager(getActivity());
		String[] categories = {"library","info","cafeteria_gar","cafeteria_grh","cafeteria","cafeteria_pas","cafeteria_wst"};
		Cursor c = lm.getAllHoursFromDb(categories[mItemId]);

		@SuppressWarnings("deprecation")
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(getActivity(),
				android.R.layout.two_line_list_item, c, c.getColumnNames(),
				new int[] { android.R.id.text1, android.R.id.text2 }) {

			@Override
			public boolean isEnabled(int position) {
				// disable onclick
				return false;
			}
		};
		adapter.setViewBinder(this);

		ListView lv2 = (ListView) rootView.findViewById(R.id.fragment_item_detail_listview);
		lv2.setAdapter(adapter);

		return rootView;
	}

	/**
	 * change presentation of locations in the list
	 */
	@Override
	public boolean setViewValue(View view, Cursor c, int index) {
		if (view.getId() == android.R.id.text2) {
			String transport = c.getString(c.getColumnIndex(Const.TRANSPORT_COLUMN));
			String address = c.getString(c.getColumnIndex(Const.ADDRESS_COLUMN));
			String hours = c.getString(c.getColumnIndex(Const.HOURS_COLUMN));
			String remark = c.getString(c.getColumnIndex(Const.REMARK_COLUMN));
			String room = c.getString(c.getColumnIndex(Const.ROOM_COLUMN));
            String url = c.getString(c.getColumnIndex(Const.URL_COLUMN));

			StringBuilder sb = new StringBuilder(hours + "\n" + address);
			if (room.length() > 0) {
				sb.append(", ").append(room);
			}
			if (transport.length() > 0) {
				sb.append(" (").append(transport).append(")");
			}
			if (remark.length() > 0) {
				sb.append("\n").append(remark.replaceAll("\\\\n", "\n"));
			}
            if (url.length() > 0) {
                sb.append("\n").append(url.replaceAll("\\\\n", "\n"));
            }
			TextView tv = (TextView) view;
			tv.setText(sb.toString());

			// linkify email addresses and phone numbers (e.g. 089-123456)
			// don't linkify room numbers 00.01.123
			Linkify.addLinks(tv, Linkify.EMAIL_ADDRESSES);
            Linkify.addLinks(tv, Linkify.WEB_URLS);
			Linkify.addLinks(tv, Pattern.compile("[0-9-]{6,}"), "tel:");
			return true;
		}
		return false;
	}
}
