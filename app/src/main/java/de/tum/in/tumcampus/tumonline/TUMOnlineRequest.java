package de.tum.in.tumcampus.tumonline;

import android.content.Context;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import de.tum.in.tumcampus.R;
import de.tum.in.tumcampus.auxiliary.Const;
import de.tum.in.tumcampus.auxiliary.Utils;
import de.tum.in.tumcampus.models.managers.CacheManager;

/**
 * This class will handle all action needed to communicate with the TUMOnline
 * XML-RPC backend. ALl communications is based on the base-url which is
 * attached by the Token and additional parameters.
 */
public class TUMOnlineRequest {
	// server address
	private static final String SERVICE_BASE_URL = "https://campus.tum.de/tumonline/wbservicesbasic.";

    /** String possibly contained in response from server */
    private static final String NO_FUNCTION_RIGHTS = "Keine Rechte für Funktion";

    /** String possibly contained in response from server */
    private static final String TOKEN_NICHT_BESTAETIGT = "Token ist nicht bestätigt oder ungültig!";

    // force to fetch data and fill cache
    private boolean fillCache = false;

    // set to null, if not needed
	private String accessToken = null;

	/** asynchronous task for interactive fetch */
    private AsyncTask<Void, Void, String> backgroundTask = null;

	/** http client instance for fetching */
	private final HttpClient client;

	/** method to call */
	private TUMOnlineConst method = null;

	/** a list/map for the needed parameters */
	private Map<String, String> parameters;
    private final CacheManager cacheManager;

    private TUMOnlineRequest(Context context) {
        cacheManager = new CacheManager(context);
		client = getThreadSafeClient();
		resetParameters();
		HttpParams params = client.getParams();
		HttpConnectionParams.setSoTimeout(params, Const.HTTP_TIMEOUT);
		HttpConnectionParams.setConnectionTimeout(params, Const.HTTP_TIMEOUT);
	}

	public TUMOnlineRequest(TUMOnlineConst method, Context context, boolean needsToken) {
		this(context);
		this.method = method;

        if(needsToken) {
            loadAccessTokenFromPreferences(context);
        }
	}

    public TUMOnlineRequest(TUMOnlineConst method, Context context) {
        this(method, context, true);
        this.fillCache = true;
    }

	public void cancelRequest(boolean mayInterruptIfRunning) {
		// Cancel background task just if one has been established
		if (backgroundTask != null) {
			backgroundTask.cancel(mayInterruptIfRunning);
		}
	}

	/**
	 * Fetches the result of the HTTPRequest (which can be seen by using {@link #getRequestURL()})
     *
	 * @return output will be a raw String
	 */
	public String fetch() {
		String result;
		String url = getRequestURL();
		Utils.log("fetching URL " + url);

		try {
            result = cacheManager.getFromCache(url);
            if(result==null || fillCache) {
                HttpGet request = new HttpGet(url);
                HttpResponse response = client.execute(request);
                HttpEntity responseEntity = response.getEntity();

                if (responseEntity != null) {
                    // do something with the response
                    result = EntityUtils.toString(responseEntity);
                    cacheManager.addToCache(url, result);
                    Utils.logv("added to cache " + url);
                }
            } else {
                Utils.logv("loaded from cache " + url);
            }
		} catch (Exception e) {
			Utils.log(e, "FetchError");
			return e.getMessage();
		}
		return result;
	}

	/**
	 * this fetch method will fetch the data from the TUMOnline Request and will
	 * address the listeners onFetch if the fetch succeeded, else the
	 * onFetchError will be called
	 * 
	 * @param context the current context (may provide the current activity)
	 * @param listener the listener, which takes the result
	 */
	public void fetchInteractive(final Context context, final TUMOnlineRequestFetchListener listener) {

		if (!loadAccessTokenFromPreferences(context)) {
			listener.onFetchCancelled();
		}

		// fetch information in a background task and show progress dialog in
		// meantime
		backgroundTask = new AsyncTask<Void, Void, String>() {

			/** property to determine if there is an internet connection */
			boolean isOnline;

			@Override
			protected String doInBackground(Void... params) {
				// set parameter on the TUMOnline request an fetch the results
				isOnline = Utils.isConnected(context);
				if (!isOnline) {
					// not online, fetch does not make sense
					return null;
				}
				// we are online, return fetch result
				return fetch();
			}

			@Override
			protected void onPostExecute(String result) {
				if (result != null) {
					Utils.logv("Received result <" + result + ">");
				} else {
					Utils.log("No result available");
				}
				// Handles result
				if (!isOnline) {
					listener.onCommonError(context.getString(R.string.no_internet_connection));
					return;
				}
				if (result == null) {
					listener.onFetchError(context.getString(R.string.empty_result));
					return;
				} else if (result.contains(TOKEN_NICHT_BESTAETIGT)) {
					listener.onFetchError(context.getString(R.string.dialog_access_token_invalid));
					return;
				} else if (result.contains(NO_FUNCTION_RIGHTS)) {
					listener.onFetchError(context.getString(R.string.dialog_no_rights_function));
					return;
				}
				// If there could not be found any problems return usual on
				// Fetch method
				listener.onFetch(result);
			}

		};
		backgroundTask.execute();
	}

	/**
	 * This will return the URL to the TUMOnlineRequest with regard to the set
	 * parameters
	 * 
	 * @return a String URL
	 */
    String getRequestURL() {
		String url = SERVICE_BASE_URL + method + "?";

		// Builds to be fetched URL based on the base-url and additional parameters
        for (Entry<String, String> pairs : parameters.entrySet()) {
            url += pairs.getKey() + "=" + pairs.getValue() + "&";
        }
		return url;
	}

	private DefaultHttpClient getThreadSafeClient() {
		DefaultHttpClient client = new DefaultHttpClient();
		ClientConnectionManager mgr = client.getConnectionManager();
		HttpParams params = client.getParams();

		client = new DefaultHttpClient(new ThreadSafeClientConnManager(params,
				mgr.getSchemeRegistry()), params);

		return client;
	}

	/**
	 * Check if TUMOnline access token can be retrieved from shared preferences.
	 * 
	 * @param context The context
	 * @return true if access token is available; false otherwise
	 */
	private boolean loadAccessTokenFromPreferences(Context context) {
		accessToken = PreferenceManager.getDefaultSharedPreferences(context).getString(Const.ACCESS_TOKEN, null);

		// no access token set, or it is obviously wrong
		if (accessToken == null || accessToken.length() < 1) {
			return false;
		}

		Utils.logv("AccessToken = " + accessToken);

		// ok, access token seems valid (at first)
		setParameter(Const.P_TOKEN, accessToken);
		return true;
	}

	/** Reset parameters to an empty Map */
    void resetParameters() {
		parameters = new HashMap<String, String>();
		// set accessToken as parameter if available
		if (accessToken != null) {
			parameters.put(Const.P_TOKEN, accessToken);
		}
	}

	/**
	 * Sets one parameter name to its given value
	 * 
	 * @param name identifier of the parameter
	 * @param value value of the parameter
	 */
	public void setParameter(String name, String value) {
        try {
            parameters.put(name, URLEncoder.encode(value, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            Utils.log(e);
        }
    }

    public void setForce(boolean force) {
        fillCache = force;
    }
}
