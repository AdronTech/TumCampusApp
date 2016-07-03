package de.tum.in.tumcampusapp.adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

import de.tum.in.tumcampusapp.R;
import de.tum.in.tumcampusapp.tumonline.TUMRoomFinderRequest;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

/**
 * Custom UI adapter for a list of employees.
 */
public class RoomFinderListAdapter extends BaseAdapter implements StickyListHeadersAdapter {

    private final List<Map<String, String>> data;
    private final LayoutInflater inflater;

    static class ViewHolder {
        TextView tvRoomTitle;
        TextView tvBuildingTitle;
    }

    public RoomFinderListAdapter(Activity activity, List<Map<String, String>> d) {
        data = d;
        inflater = LayoutInflater.from(activity.getApplicationContext());
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_roomfinder_item, parent, false);

            holder = new ViewHolder();
            holder.tvRoomTitle = (TextView) convertView.findViewById(R.id.startup_actionbar_title);
            holder.tvBuildingTitle = (TextView) convertView.findViewById(R.id.building);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Map<String, String> room = data.get(position);

        // Setting all values in listView
        holder.tvRoomTitle.setText(room.get(TUMRoomFinderRequest.KEY_ROOM_TITLE));
        holder.tvBuildingTitle.setText(room.get(TUMRoomFinderRequest.KEY_BUILDING_TITLE));
        return convertView;
    }

    // Generate header view
    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        HeaderViewHolder holder;
        if (convertView == null) {
            holder = new HeaderViewHolder();
            convertView = inflater.inflate(R.layout.header, parent, false);
            holder.text = (TextView) convertView.findViewById(R.id.lecture_header);
            convertView.setTag(holder);
        } else {
            holder = (HeaderViewHolder) convertView.getTag();
        }

        //set header text as first char in name
        Map<String, String> room = data.get(position);
        String headerText = room.get(TUMRoomFinderRequest.KEY_CAMPUS_TITLE);
        holder.text.setText(headerText);
        return convertView;
    }

    @Override
    public long getHeaderId(int i) {
        String headerText = data.get(i).get(TUMRoomFinderRequest.KEY_CAMPUS_TITLE);

        if (headerText == null) {
            return 'Z';
        }
        return headerText.hashCode();
    }

    static class HeaderViewHolder {
        TextView text;
    }
}
