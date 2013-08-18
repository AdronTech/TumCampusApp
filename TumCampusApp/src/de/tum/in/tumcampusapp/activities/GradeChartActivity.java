package de.tum.in.tumcampusapp.activities;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import de.tum.in.tumcampusapp.R;

/**
 * Displays charts generated with GradesActivity
 * 
 */
public class GradeChartActivity extends Activity {

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gradechart);
		WebView Webview = (WebView) findViewById(R.id.webView1);

		Bundle extras = getIntent().getExtras();
		String chartContent = extras.getString("chartContent");

		WebSettings WebSettings = Webview.getSettings();
		WebSettings.setJavaScriptEnabled(true);
		WebSettings.setUseWideViewPort(true);
		WebSettings.setLoadWithOverviewMode(true);
		Webview.requestFocusFromTouch();
		Webview.loadDataWithBaseURL("file:///android_asset/", chartContent,
				"text/html", "utf-8", null);
	}
}
