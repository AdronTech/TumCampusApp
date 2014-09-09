package de.tum.in.tumcampus.activities.generic;

import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SearchRecentSuggestionsProvider;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.provider.SearchRecentSuggestions;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.Toast;

import de.tum.in.tumcampus.R;
import de.tum.in.tumcampus.auxiliary.Utils;

/**
 * Generic class for searching. Provides basic functions for a text search field
 * and typical processes related to search.
 * 
 */
public abstract class ActivityForSearching extends ProgressActivity {
    protected SearchView mSearchView;
    protected String mQuery = null;
    private String mAuthority;
    private int mMinLength;

    protected abstract void onStartSearch();
    protected abstract void onStartSearch(String s);

    public ActivityForSearching(int layoutIt, String auth, int minLen) {
		super(layoutIt);
        mAuthority = auth;
        mMinLength = minLen;
	}

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_activity_for_searching, menu);

        // Get SearchView
        final MenuItem searchItem = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) MenuItemCompat.getActionView(searchItem);

        // Set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchableInfo info = searchManager.getSearchableInfo(getComponentName());
        mSearchView.setSearchableInfo(info);

        // Optical tweaks to match application theme
        styleSearchView(info.getHintId());

        if (mSearchView != null) {
            if(mQuery!=null) {
                mSearchView.setQuery(mQuery, false);
                MenuItemCompat.expandActionView(searchItem);
            }

            mSearchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
                @Override
                public boolean onSuggestionSelect(int i) {
                    return false;
                }

                @Override
                public boolean onSuggestionClick(int position) {
                    String suggestion = getSuggestion(position);
                    mSearchView.setQuery(suggestion, true);
                    return true;
                }

                private String getSuggestion(int position) {
                    Cursor cursor = (Cursor) mSearchView.getSuggestionsAdapter().getItem(position);
                    return cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1));
                }
            });

            mSearchView.setOnCloseListener(new SearchView.OnCloseListener() {
                @Override
                public boolean onClose() {
                    MenuItemCompat.collapseActionView(searchItem);
                    mQuery = null;
                    onStartSearch();
                    return false;
                }
            });
        }
		return true;
	}

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(intent==null)
            return;
        setIntent(intent);
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            requestSearch(intent.getStringExtra(SearchManager.QUERY));
        }
    }

    private void styleSearchView(int hintId) {
        // Adjust small lense icon and hint text
        SearchView.SearchAutoComplete searchAutoComplete = (SearchView.SearchAutoComplete)mSearchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        SpannableStringBuilder ssb = new SpannableStringBuilder("   ");
        ssb.append(getString(hintId));
        Drawable searchIcon = getResources().getDrawable(R.drawable.ic_action_search);
        int textSize = (int) (searchAutoComplete.getTextSize() * 1.4);
        searchIcon.setBounds(0, 0, textSize, textSize);
        ssb.setSpan(new ImageSpan(searchIcon), 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        searchAutoComplete.setHint(ssb);
        searchAutoComplete.setHintTextColor(getResources().getColor(R.color.searchview_hint_color));
        searchAutoComplete.setTextColor(Color.WHITE);
        searchAutoComplete.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI|EditorInfo.IME_ACTION_SEARCH);

        // Adjust the color of the border shown below the EditText
        View searchPlate = mSearchView.findViewById(android.support.v7.appcompat.R.id.search_plate);
        searchPlate.setBackgroundResource(R.drawable.searchview_textfield_holo_blue);

        // Set a new x symbol
        ImageView searchCloseIcon = (ImageView)mSearchView.findViewById(android.support.v7.appcompat.R.id.search_close_btn);
        searchCloseIcon.setImageResource(R.drawable.ic_action_cancel);

        // Replace collapsed icon
        ImageView collapsedSearchIcon = (ImageView) mSearchView.findViewById(android.support.v7.appcompat.R.id.search_button);
        Drawable bigSearchIcon = getResources().getDrawable(R.drawable.ic_action_search);
        collapsedSearchIcon.setImageDrawable(bigSearchIcon);
    }

	private boolean requestSearch(String query) {
        if(query.length()<mMinLength) {
            String text = String.format(getString(R.string.min_search_len),mMinLength);
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
            return false;
        }

		if (!Utils.isConnected(this)) {
            showError(R.string.no_internet_connection);
			return false;
		}

        // Add query to recents
        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this, mAuthority, SearchRecentSuggestionsProvider.DATABASE_MODE_QUERIES);
        suggestions.saveRecentQuery(query, null);

        // Tell activity to start searching
        mQuery = query;
        onStartSearch(query);
		return true;
	}
}
