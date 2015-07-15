package de.tum.in.tumcampus.widgets.RemoteViewFactories;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.List;

import de.tum.in.tumcampus.R;
import de.tum.in.tumcampus.auxiliary.Utils;
import de.tum.in.tumcampus.models.MVVDeparture;
import de.tum.in.tumcampus.models.MVVObject;
import de.tum.in.tumcampus.models.MVVSuggestion;
import de.tum.in.tumcampus.models.managers.MVVDelegate;
import de.tum.in.tumcampus.models.managers.MVVJsoupParser;
import de.tum.in.tumcampus.models.managers.RecentsManager;

/**
 * Remote View Factory that generates the data related to the most recent station searched on the
 * TCA MVV
 */
public class MVVRemoteViewFactory implements RemoteViewsService.RemoteViewsFactory, MVVDelegate {

    private Context applicationContext;
    private RecentsManager recentsManager;
    private List<MVVObject> mvvList;
    private AppWidgetManager appWidgetManager;
    private Intent intent;


    public MVVRemoteViewFactory(Context applicationContext, Intent intent) {
        this.applicationContext = applicationContext;
        this.intent = intent;
    }

    @Override
    public void onCreate() {
        //Load from Shared preferences the last searched station
        recentsManager = new RecentsManager(applicationContext,RecentsManager.STATIONS);
        callRecentVisitedStation();
        appWidgetManager = AppWidgetManager.getInstance(applicationContext);

    }

    @Override
    public void onDataSetChanged() {

    }

    @Override
    public void onDestroy() {
    }

    @Override
    public int getCount() {
        if (mvvList != null) {
            return mvvList.size();
        }
        else{
            return 0;
        }
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews rv = new RemoteViews(applicationContext.getPackageName(), R.layout.mvv_widget_item);
        MVVDeparture currentSearch = (MVVDeparture)mvvList.get(position);
        //Loading most recent searched station
        SharedPreferences sharedPreferences = applicationContext.getSharedPreferences(applicationContext.getString(R.string.mvv_shared_pref_key), Context.MODE_PRIVATE);
        String mostRecentStationName = sharedPreferences.getString(applicationContext.getString(R.string.most_recent_station), null);

        if(currentSearch !=null) {
            int icon_id = getImageResource(currentSearch);
            String number = currentSearch.getLine(); // get station number
            String station = currentSearch.getDirection(); // get destination station name
            String minutes = String.valueOf(currentSearch.getMin() + " min"); // get minutes remaining before departure

            rv.setImageViewResource(R.id.mvv_icon, icon_id); // set public transportation icon
            rv.setTextViewText(R.id.line_number, number); // set station number
            // create string containing both origin station and destination station
            String stationString = (mostRecentStationName == null ? station : mostRecentStationName + " to " + station);
            rv.setTextViewText(R.id.station, stationString);
            rv.setTextViewText(R.id.minutes, minutes);
            return rv;

        }
        return null;
    }



    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    public boolean callRecentVisitedStation(){
        // get cached most recent seearched station
        SharedPreferences sharedPreferences = applicationContext.getSharedPreferences(applicationContext.getString(R.string.mvv_shared_pref_key), Context.MODE_PRIVATE);
        String queryString = sharedPreferences.getString(applicationContext.getString(R.string.most_recent_station), null);
        boolean isValid = true;
        Utils.log("WidgetMVV queryString is: " + queryString);
        try {
            // execute query asynchronously
            if (queryString != null)
                (new MVVJsoupParser(this)).execute(new String[]{queryString});
            else {
                Utils.showToast(applicationContext, applicationContext.getString(R.string.no_recent_found));
            }

        }catch (Exception e){
            Utils.showToast(applicationContext, applicationContext.getString(R.string.something_is_wrong));
            e.printStackTrace();
            isValid = false;
            return isValid;
        }

        return isValid;
    }

    /**
     *  method showing the empty view if the listview is empty
     */
    private void showEmptyView() {
        RemoteViews rv = new RemoteViews(applicationContext.getPackageName(),R.layout.mvv_widget);
        rv.setEmptyView(R.layout.mvv_widget, R.id.empty_view);
    }

    /**
     * method that calls the first suggestion available if the result of the query is a sugestion
     * and not a real station
     * @param sug
     */
    @Override
    public void showSuggestionList(MVVObject sug) {
        try {
            mvvList = sug.getResultList();
            final MVVSuggestion sugestion = (MVVSuggestion) mvvList.get(0);
            final MVVDelegate delegate = this;
            // execute real query from the suggestion
            ( new MVVJsoupParser(delegate) ).execute(sugestion.getName());


            mvvList = null;
        }catch (Exception e){
            Utils.log(e);
            Utils.showToast(applicationContext, applicationContext.getString(R.string.something_is_wrong));
        }
    }

    /**
     * Method called when the query result is ready.
     * All the data is sent to the listview to be shown
     * @param dep
     */
    @Override
    public void showDepartureList(MVVObject dep) {
        try {
            // set data for listview
            mvvList = dep.getResultList();
            int widgetID = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1);
            // notify update
            appWidgetManager.notifyAppWidgetViewDataChanged(widgetID, R.id.mvv_widget_item);

        }catch (Exception e){
            Utils.log(e);
            Utils.showToast(applicationContext, applicationContext.getString(R.string.something_is_wrong));
        }
    }

    /**
     * Method called if the query execution failed
     * @param object
     */

    @Override
    public void showError(MVVObject object) {
        Utils.log("WidgetMVV " + object.getMessage());
        Utils.showToast(applicationContext, applicationContext.getString(R.string.something_is_wrong));
    }


    /**
     * gets the drawable for related departure in mvv
     * because of efficiency used this method, retrieving resources by string name
     * is not efficient.
     *
     * @param departure MVVDeparture object
     * @return int, id of the icon related to this departure
     */
    public int getImageResource(MVVDeparture departure) {
        MVVDeparture.TransportationType type = departure.getTransportationType();
        switch (type) {
            case UBAHN:
                return R.drawable.mvv_ubahn;
            case SBAHN:
                return R.drawable.mvv_sbahn;
            case BUS_TRAM:
                return R.drawable.mvv_bustram;
        }
        return -1;
    }

}
