package de.tum.in.tumcampus.activities;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;

import de.tum.in.tumcampus.R;
import de.tum.in.tumcampus.activities.generic.ActivityForSearching;
import de.tum.in.tumcampus.auxiliary.Const;
import de.tum.in.tumcampus.auxiliary.ImplicitCounter;
import de.tum.in.tumcampus.auxiliary.MVVStationSuggestionProvider;
import de.tum.in.tumcampus.auxiliary.Utils;
import de.tum.in.tumcampus.models.managers.TransportManager;

/**
 * Activity to show transport stations and departures
 */
public class TransportationActivity extends ActivityForSearching implements OnItemClickListener {

    private TextView infoTextView;
    private ListView listViewResults;
    private Thread runningSearch = null;
    private SimpleCursorAdapter adapterStations;
    private SimpleCursorAdapter adapterResults;
    private TransportManager transportationManager;

    public TransportationActivity() {
        super(R.layout.activity_transportation, MVVStationSuggestionProvider.AUTHORITY);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPrefs.getBoolean("implicitly_id", true)) {
            ImplicitCounter.Counter("mvv_id", this.getApplicationContext());
        }

        // get all stations from db
        transportationManager = new TransportManager(this);

        infoTextView = (TextView) this.findViewById(R.id.activity_transport_textview_info);
        listViewResults = (ListView) this.findViewById(R.id.activity_transport_listview_result);
        listViewResults.setOnItemClickListener(this);

        // Initialize stations adapter
        Cursor stationCursor = transportationManager.getAllFromDb();
        adapterStations = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, stationCursor,
                stationCursor.getColumnNames(), new int[]{android.R.id.text1});

        // initialize empty departure adapter, disable on click in list
        MatrixCursor departureCursor = new MatrixCursor(new String[]{"symbol", "name", "time", "_id"});
        adapterResults = new SimpleCursorAdapter(this, R.layout.activity_transportation_departure_listview, departureCursor,
                departureCursor.getColumnNames(), new int[]{R.id.line_symbol, R.id.line_name, R.id.line_time}) {
            @Override
            public boolean isEnabled(int position) {
                return false;
            }
        };
        adapterResults.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int i) {
                if (view.getId() == R.id.line_symbol) {
                    TransportManager.setSymbol(TransportationActivity.this, (TextView) view, cursor.getString(0));
                    return true;
                }
                return false;
            }
        });

        onNewIntent(getIntent());

        if(mQuery==null) {
            listViewResults.setAdapter(adapterStations);
            listViewResults.requestFocus();
        }
    }

    /**
     * Click on station in list
     * */
    @Override
    public void onItemClick(final AdapterView<?> av, View v, int position, long id) {
        Cursor departureCursor = (Cursor) av.getAdapter().getItem(position);
        final String location = departureCursor.getString(departureCursor.getColumnIndex(Const.NAME_COLUMN));

        this.listViewResults.setEnabled(true);
        mSearchView.setQuery(location, false);

        showStation(location);
    }

    public void showStation(final String location) {
        // save clicked station into db and refresh station list
        // (could be clicked on search result list)
        transportationManager.replaceIntoDb(location);

        this.progressLayout.setVisibility(View.VISIBLE);
        this.infoTextView.setVisibility(View.GONE);

        if (!Utils.isConnected(this)) {
            this.progressLayout.setVisibility(View.GONE);
            this.errorLayout.setVisibility(View.VISIBLE);
            Toast.makeText(this, R.string.no_internet_connection, Toast.LENGTH_SHORT).show();
            adapterResults.changeCursor(transportationManager.getAllFromDb());
            listViewResults.setAdapter(adapterResults);
            return;
        }

        new Thread(new Runnable() {
            int message;

            @Override
            public void run() {
                // get departures from website
                Cursor departureCursor = null;
                try {
                    departureCursor = transportationManager.getDeparturesFromExternal(location);
                } catch (NoSuchElementException e) {
                    this.message = R.string.no_departures_found;
                } catch (TimeoutException e) {
                    this.message = R.string.exception_timeout;
                } catch (Exception e) {
                    this.message = R.string.exception_unknown;
                }

                // show departures in list
                TransportationActivity.this.runOnUiThread(new ResultUpdater(departureCursor, adapterResults, message));
            }
        }).start();
    }

    @Override
    public void onBackPressed() {
        if (runningSearch != null) {
            runningSearch.interrupt();
            progressLayout.setVisibility(View.GONE);
            runningSearch = null;
        } else if(listViewResults.getAdapter()==adapterResults) {
            performSearchAlgorithm("");
        } else {
            finish();
        }
    }

    /**
     * Searchs the Webservice for stations
     *
     * @param query the text entered by the user
     */

    @Override
    public void performSearchAlgorithm(String query) {
        // TODO: Workaround, because MVV does not find a station with the full name as a text input
        String inputTextToCheck = query;
        if (query.length() > 2) {
            inputTextToCheck = query.substring(0, query.length() - 1);
        } else if(query.isEmpty()) {
            Cursor stationCursor = transportationManager.getAllFromDb();
            adapterStations.changeCursor(stationCursor);
            listViewResults.setAdapter(adapterStations);
            return;
        }
        final String inputText = inputTextToCheck;
        infoTextView.setVisibility(View.GONE);

        runningSearch = new Thread(new Runnable() {

            @Override
            public void run() {
                // Remember my Thread
                Thread me = TransportationActivity.this.runningSearch;

                // Get Information
                Cursor stationCursor = null;
                int message = 0;
                try {
                    stationCursor = transportationManager.getStationsFromExternal(inputText);
                } catch (NoSuchElementException e) {
                    message = R.string.no_station_found;
                } catch (TimeoutException e) {
                    message = R.string.exception_timeout;
                } catch (Exception e) {
                    message = R.string.exception_unknown;
                }

                // Drop results if canceled
                if (TransportationActivity.this.runningSearch == null || TransportationActivity.this.runningSearch != me) {
                    return;
                }

                // If there is exactly one station open results directly
                if(stationCursor!=null && stationCursor.getCount()==1) {
                    stationCursor.moveToFirst();
                    final String location = stationCursor.getString(0);
                    TransportationActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showStation(location);
                        }
                    });
                    return;
                }

                // show stations from search result in station list, show error message if necessary
                TransportationActivity.this.runOnUiThread(new ResultUpdater(stationCursor, adapterStations, message));
            }
        });
        runningSearch.start();
    }

    /**
     * Brings the results of the query to ui
    * */
    private class ResultUpdater implements Runnable {

        private final SimpleCursorAdapter mAdapter;
        private final Cursor mCursor;
        private final int mMessage;

        private ResultUpdater(Cursor cursor, SimpleCursorAdapter adapter, int message) {
            mCursor = cursor;
            mAdapter = adapter;
            mMessage = message;
        }

        @Override
        public void run() {
            mAdapter.changeCursor(mCursor);
            listViewResults.setAdapter(mAdapter);

            TransportationActivity.this.progressLayout.setVisibility(View.GONE);
            TransportationActivity.this.errorLayout.setVisibility(View.GONE);

            if (mMessage!= 0) {
                TransportationActivity.this.infoTextView.setText(mMessage);
                TransportationActivity.this.infoTextView.setVisibility(View.VISIBLE);
            }
            runningSearch = null;
        }
    }
}