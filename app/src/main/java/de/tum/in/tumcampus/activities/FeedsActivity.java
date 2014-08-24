package de.tum.in.tumcampus.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import de.tum.in.tumcampus.R;
import de.tum.in.tumcampus.auxiliary.Const;
import de.tum.in.tumcampus.auxiliary.ImplicitCounter;
import de.tum.in.tumcampus.models.Feed;
import de.tum.in.tumcampus.models.managers.FeedManager;

/**
 * Displays all available RSS feeds and allow to add them
 * 
 * @review Sascha Moecker
 * 
 */
public class FeedsActivity extends ActionBarActivity implements OnItemClickListener,
		OnItemLongClickListener {

	// Id and name needed for transmitting to the detail activity
	private static String feedId;
	private static String feedName;

    private SimpleCursorAdapter adapter;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//Counting the number of times that the user used this activity for intelligent reordering 
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (sharedPrefs.getBoolean("implicitly_id", true)){
				ImplicitCounter.Counter("rss_feeds_id", getApplicationContext());
		}
		setContentView(R.layout.activity_feeds);

		FeedManager fm = new FeedManager(this);
		Cursor c = fm.getAllFromDb();

		adapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_list_item_1, c, c.getColumnNames(),
				new int[] { android.R.id.text1 });

		ListView lv = (ListView) findViewById(R.id.listView);
		lv.setAdapter(adapter);
		lv.setOnItemClickListener(this);
		lv.setOnItemLongClickListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_feeds, menu);
		return true;
	}


	@Override
	public void onItemClick(AdapterView<?> av, View v, int position, long id) {

		// click on feed in list
		if (position != -1) {
			Cursor c = (Cursor) av.getAdapter().getItem(position);
			feedId = c.getString(c.getColumnIndex(Const.ID_COLUMN));
			feedName = c.getString(c.getColumnIndex(Const.NAME_COLUMN));
		}

		Intent intent = new Intent(this, FeedsDetailsActivity.class);
		intent.putExtra(Const.FEED_ID, feedId);
		intent.putExtra(Const.FEED_NAME, feedName);
		startActivity(intent);
	}

	@Override
	public boolean onItemLongClick(final AdapterView<?> av, View v,
			final int position, long id) {
		if (id == -1) {
			return false;
		}

		// confirm delete
		DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int id) {

				// delete feed from list, refresh feed list
				Cursor c = (Cursor) av.getAdapter().getItem(position);
				int _id = c.getInt(c.getColumnIndex(Const.ID_COLUMN));

				FeedManager fm = new FeedManager(av.getContext());
				fm.deleteFromDb(_id);
				adapter.changeCursor(fm.getAllFromDb());
			}
		};
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getString(R.string.really_delete));
		builder.setPositiveButton(getString(R.string.yes), listener);
		builder.setNegativeButton(getString(R.string.no), null);
		builder.show();
		return false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_add:
			final Dialog dialog = new Dialog(this);
			dialog.setContentView(R.layout.dialog_feeds_add);
			dialog.setTitle(R.string.add_rss);
			Button saveButton = (Button) dialog.findViewById(R.id.save);
			saveButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					// add a new feed
					EditText editName = (EditText) dialog
							.findViewById(R.id.name);
					EditText editUrl = (EditText) dialog.findViewById(R.id.url);

					String name = editName.getText().toString();
					String url = editUrl.getText().toString();

					// prepend http:// if needed
					if (url.length() > 0 && !url.contains(":")) {
						url = "http://" + url;
					}
					FeedManager fm = new FeedManager(getParent());
					try {
						Feed feed = new Feed(name, url);
						fm.insertUpdateIntoDb(feed);
					} catch (Exception e) {
						Log.e(getClass().getSimpleName(), e.getMessage());
					}

					// refresh feed list
					adapter.changeCursor(fm.getAllFromDb());

					// clear form
					editName.setText("");
					editUrl.setText("");
					dialog.dismiss();
				}
			});
			dialog.show();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}