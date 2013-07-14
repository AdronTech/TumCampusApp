package de.tum.in.tumcampusapp.activities.generic;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;
import de.tum.in.tumcampusapp.R;
import de.tum.in.tumcampusapp.auxiliary.Const;
import de.tum.in.tumcampusapp.auxiliary.Utils;
import de.tum.in.tumcampusapp.services.DownloadService;

public class ActivityForDownloadingExternal extends FragmentActivity {
	private Activity activity = this;

	private RelativeLayout errorLayout;
	private int layoutId;
	private String method;
	private RelativeLayout progressLayout;

	public BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			if (!intent.getAction().equals(DownloadService.BROADCAST_NAME)) {
				return;
			}

			String action = intent.getStringExtra(Const.ACTION_EXTRA);
			if (action.length() != 0) {
				Log.i(activity.getClass().getSimpleName(),
						"Broadcast received  <" + action + ">");
				if (action.equals(Const.COMPLETED)) {
					progressLayout.setVisibility(View.GONE);
					errorLayout.setVisibility(View.GONE);
					// Calls onStart() to simulate a new start of the activity
					// without downloading new data, since this receiver
					// receives data from a new download
					onStart();
				}
				if (action.equals(Const.WARNING)) {
					String message = intent
							.getStringExtra(Const.WARNING_MESSAGE);
					Toast.makeText(activity, message, Toast.LENGTH_SHORT)
							.show();
					progressLayout.setVisibility(View.GONE);
				}
				if (action.equals(Const.ERROR)) {
					String message = intent.getStringExtra(Const.ERROR_MESSAGE);
					Toast.makeText(activity, message, Toast.LENGTH_SHORT)
							.show();
					progressLayout.setVisibility(View.GONE);
					errorLayout.setVisibility(View.VISIBLE);
				}
			}
		}
	};

	public ActivityForDownloadingExternal(String method, int layoutId) {
		this.method = method;
		this.layoutId = layoutId;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(layoutId);

		progressLayout = (RelativeLayout) findViewById(R.id.progress_layout);
		errorLayout = (RelativeLayout) findViewById(R.id.error_layout);

		if (progressLayout == null || errorLayout == null) {
			Log.e(getClass().getSimpleName(),
					"Cannot find layouts, did you forget to provide error and progress layouts?");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(
				R.menu.menu_activity_for_accessing_tum_online, menu);
		return true;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_update:
			requestDownload(true);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(receiver);
		Intent service = new Intent(this, DownloadService.class);
		stopService(service);
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(receiver, new IntentFilter(
				DownloadService.BROADCAST_NAME));
	}

	public void requestDownload(boolean forceDownload) {
		if (Utils.isConnected(this)) {
			progressLayout.setVisibility(View.VISIBLE);
			Intent service = new Intent(this, DownloadService.class);
			service.putExtra(Const.ACTION_EXTRA, method);
			service.putExtra(Const.FORCE_DOWNLOAD, forceDownload);
			startService(service);
		} else {
			Toast.makeText(this, R.string.no_internet_connection,
					Toast.LENGTH_SHORT).show();
		}
	}

	public void requestDownloadWithExtras(Bundle extras, boolean forceDownload) {
		if (Utils.isConnected(this)) {
			progressLayout.setVisibility(View.VISIBLE);
			Intent service = new Intent(this, DownloadService.class);
			service.putExtra(Const.ACTION_EXTRA, method);
			service.putExtras(extras);
			startService(service);
		} else {
			Toast.makeText(this, R.string.no_internet_connection,
					Toast.LENGTH_SHORT).show();
		}
	}

	public void showErrorLayout() {
		errorLayout.setVisibility(View.VISIBLE);
	}
}
