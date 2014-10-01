package de.tum.in.tumcampus.activities;

import android.os.Bundle;
import android.webkit.WebView;

import de.tum.in.tumcampus.R;
import de.tum.in.tumcampus.activities.generic.ActivityForLoadingInBackground;
import de.tum.in.tumcampus.auxiliary.Utils;
import de.tum.in.tumcampus.models.managers.CacheManager;

/**
 * Activity to display the curricula details of different programs.
 *
 * NEEDS: CurriculaActivity.URL set in incoming bundle (url to load study plan from)
 *        CurriculaActivity.NAME set in incoming bundle (name of the study program)
 */
public class CurriculaDetailsActivity extends ActivityForLoadingInBackground<String,String> {

	private WebView browser;

    public CurriculaDetailsActivity() {
        super(R.layout.activity_curriculadetails);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        browser = (WebView) findViewById(R.id.activity_curricula_web_view);
        browser.getSettings().setBuiltInZoomControls(true);

        String url = getIntent().getExtras().getString(CurriculaActivity.URL);
        String name = getIntent().getExtras().getString(CurriculaActivity.NAME);

        setTitle(name);
        startLoading(url);
    }

    /**
     * Fetch information in a background task and show progress dialog in meantime
     */
    @Override
    protected String onLoadInBackground(String... params) {
        return fetchCurriculum(params[0]);
    }

    /**
     * Fetches the curriculum document and extracts all relevant information.
     *  @param url URL of the curriculum document
     *
     */
    private String fetchCurriculum(String url) {
        String text = Utils.buildHTMLDocument(Utils.downloadFileAndCache(this,
                        "http://www.in.tum.de/fileadmin/_src/add.css", CacheManager.VALIDITY_ONE_MONTH),
                "<div id=\"maincontent\"><div class=\"inner\">" + extractResultsFromURL(url) + "</div></div>");
        return text.replace("href=\"fuer-studierende-der-tum", "href=\"http://www.in.tum.de/fuer-studierende-der-tum");
    }

    /**
	 * Extract the results from a document fetched from the given URL.
	 * 
	 * @param url URL pointing to a document where the results are extracted from.
	 * @return The results.
	 */
	private String extractResultsFromURL(String url) {
		String text = Utils.downloadFileAndCache(this, url, CacheManager.VALIDITY_ONE_MONTH);

        if (text==null && !Utils.isConnected(this)) {
            showNoInternetLayout();
        } else if (text == null) {
			showError(R.string.something_wrong);
		}
		return Utils.cutText(text, "<!--TYPO3SEARCH_begin-->", "<!--TYPO3SEARCH_end-->");
	}

    /**
     * When html data is loaded show it in webView
     * @param result File
     */
    @Override
    protected void onLoadFinished(String result) {
        browser.loadData(result, "text/html; charset=UTF-8", null);
        showLoadingEnded();
    }
}
