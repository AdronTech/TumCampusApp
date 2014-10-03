package de.tum.in.tumcampus.auxiliary;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import de.tum.in.tumcampus.models.managers.CacheManager;

/**
 * Class for common helper functions used by a lot of classes
 */
public class Utils {
    /* Device id */
    private static String uniqueID = null;
    private static final String PREF_UNIQUE_ID = "PREF_UNIQUE_ID";

    /**
     * Builds a HTML document out of a css file and the body content.
     *
     * @param css  The CSS specification
     * @param body The body content
     * @return The HTML document.
     */
    public static String buildHTMLDocument(String css, String body) {
        String header = "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"de\" lang=\"de\">" +
                "<head><meta name=\"viewport\" content=\"width=device-width\" />" +
                "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" /></head>";
        css = "<style type=\"text/css\">" + css + "</style>";
        body = "<body>" + body + "</body>";
        String footer = "</html>";
        return header + css + body + footer;
    }

    /**
     * Cut substring from a text.
     *
     * @param text        The text.
     * @param startString Start string where the cutting begins.
     * @param endString   End string where the cutting ends.
     * @return The cut text.
     */
    public static String cutText(String text, String startString, String endString) {
        int startPos = text.indexOf(startString);
        int endPos = text.indexOf(endString, startPos);

        if (startPos == -1) {
            startPos = 0;
        }
        if (endPos == -1 || endPos < startPos) {
            endPos = text.length();
        }

        return text.substring(startPos + startString.length(), endPos);
    }

    /**
     * Returns the number of datasets in a table
     *
     * @param db    Database connection
     * @param table Table name
     * @return number of datasets in a table
     */
    public static int dbGetTableCount(SQLiteDatabase db, String table) {
        Cursor c = db.rawQuery("SELECT count(*) FROM " + table, null);
        if (c.moveToNext()) {
            return c.getInt(0);
        }
        return 0;
    }

    /**
     * Start loading a file in the same thread
     *
     * @param url Download location
     * @return Gets an InputStream to the file
     * @throws Exception
     */
    private static InputStream downloadFileStream(String url) throws Exception {
        logv("Download file: " + url);
        HttpClient httpclient = new DefaultHttpClient();
        HttpEntity entity = httpclient.execute(new HttpGet(url)).getEntity();

        if (entity == null) {
            return null;
        }
        return entity.getContent();
    }


    public static String downloadFileAndCache(Context context, String url, int validity) {
        try {
            CacheManager cacheManager = new CacheManager(context);
            String content = cacheManager.getFromCache(url);
            if (content != null) {
                return content;
            }

            HttpClient httpClient = new DefaultHttpClient();

            HttpParams params = httpClient.getParams();
            HttpConnectionParams.setSoTimeout(params, Const.HTTP_TIMEOUT);
            HttpConnectionParams.setConnectionTimeout(params, Const.HTTP_TIMEOUT);

            HttpEntity entity = httpClient.execute(new HttpGet(url)).getEntity();

            content = EntityUtils.toString(entity);

            cacheManager.addToCache(url, content, validity, CacheManager.CACHE_TYP_DATA);
            return content;
        } catch (Exception e) {
            log(e);
            return null;
        }
    }

    /**
     * Download a file in the same thread.
     * If file already exists the method returns immediately
     * without downloading anything
     *
     * @param url    Download location
     * @param target Target filename in local file system
     * @throws Exception
     */
    private static void downloadToFile(String url, String target) throws Exception {
        File f = new File(target);
        if (f.exists()) {
            return;
        }

        File file = new File(target);
        InputStream in = downloadFileStream(url);
        if (in == null) {
            return;
        }

        FileOutputStream out = new FileOutputStream(file);
        byte[] buffer = new byte[8192];
        int count;
        while ((count = in.read(buffer)) != -1) {
            out.write(buffer, 0, count);
        }
        out.flush();
        out.close();
        in.close();
    }

    /**
     * Downloads an image synchronously from the given url
     *
     * @param url Image url
     * @return Downloaded image as {@link Bitmap}
     */
    public static File downloadImage(Context context, String url) {
        try {
            url = url.replaceAll(" ", "%20");

            CacheManager cacheManager = new CacheManager(context);
            String file = cacheManager.getFromCache(url);
            if (file == null) {
                File cache = context.getCacheDir();
                file = cache.getAbsolutePath() + "/" + md5(url) + ".jpg";
                cacheManager.addToCache(url, file, CacheManager.VALIDITY_TEN_DAYS, CacheManager.CACHE_TYP_IMAGE);
            }

            // If file already exists/was loaded it will return immediately
            // Use this to be sure cache has not been cleaned manually
            File f = new File(file);
            downloadToFile(url, file);

            return f;
        } catch (Exception e) {
            log(e, url);
            return null;
        }
    }

    /**
     * Downloads an image synchronously from the given url
     *
     * @param url Image url
     * @return Downloaded image as {@link Bitmap}
     */
    public static Bitmap downloadImageToBitmap(Context context, final String url) {
        File f = downloadImage(context, url);
        if (f == null)
            return null;
        return BitmapFactory.decodeFile(f.getAbsolutePath());
    }

    /**
     * Download an image in background and sets the image to the image view
     *
     * @param context   Context
     * @param url       URL
     * @param imageView Image
     */
    public static void loadAndSetImage(final Context context, final String url, final ImageView imageView) {
        synchronized (CacheManager.bitmapCache) {
            Bitmap bmp = CacheManager.bitmapCache.get(url);
            if (bmp != null) {
                imageView.setImageBitmap(bmp);
                return;
            }
        }
        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                CacheManager.imageViews.put(imageView, url);
                imageView.setImageBitmap(null);
            }

            @Override
            protected Bitmap doInBackground(Void... voids) {
                return downloadImageToBitmap(context, url);
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (bitmap == null)
                    return;
                synchronized (CacheManager.bitmapCache) {
                    CacheManager.bitmapCache.put(url, bitmap);
                }
                String tag = CacheManager.imageViews.get(imageView);
                if (tag != null && tag.equals(url)) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }.execute();
    }

    /**
     * Download a JSON stream from a URL
     *
     * @param url Valid URL
     * @return JSONObject
     * @throws Exception
     */
    public static JSONObject downloadJson(String url) throws Exception {
        logv("DownloadJson load from " + url);

        HttpClient httpClient = new DefaultHttpClient();

        HttpParams params = httpClient.getParams();
        HttpConnectionParams.setSoTimeout(params, Const.HTTP_TIMEOUT);
        HttpConnectionParams.setConnectionTimeout(params, Const.HTTP_TIMEOUT);

        HttpEntity entity = httpClient.execute(new HttpGet(url)).getEntity();

        String data = "";
        if (entity != null) {
            // JSON Response Read
            data = EntityUtils.toString(entity);
            logv("downloadJson " + data);
        }
        return new JSONObject(data);
    }

    /**
     * Download a JSON stream from a URL or load it from cache
     *
     * @param context   Context
     * @param url       Valid URL
     * @param fillCache Load data anyway and fill cache, even if valid cached version exists
     * @return JSONObject
     */
    public static JSONArray downloadJsonArray(Context context, String url, boolean fillCache, int validity) {
        logv("downloadJson load from " + url);

        String result;
        try {
            CacheManager cacheManager = new CacheManager(context);
            result = cacheManager.getFromCache(url);
            if (result == null || fillCache) {
                HttpClient httpClient = new DefaultHttpClient();

                HttpParams params = httpClient.getParams();
                HttpConnectionParams.setSoTimeout(params, Const.HTTP_TIMEOUT);
                HttpConnectionParams.setConnectionTimeout(params, Const.HTTP_TIMEOUT);

                HttpGet get = new HttpGet(url);
                get.addHeader("X-DEVICE-ID", getDeviceID(context));

                HttpEntity entity = httpClient.execute(get).getEntity();

                if (entity != null) {
                    // do something with the response
                    result = EntityUtils.toString(entity);
                    cacheManager.addToCache(url, result, validity, CacheManager.CACHE_TYP_DATA);
                    logv("added to cache " + url);
                    logv("downloadJson " + result);
                }
            } else {
                logv("loaded from cache " + url);
            }

            return new JSONArray(result);
        } catch (Exception e) {
            log(e);
            return null;
        }
    }

    /**
     * Returns the full path of a cache directory and checks if it is readable and writable
     *
     * @param directory directory postfix (e.g. feeds/cache)
     * @return full path of the cache directory
     * @throws IOException
     */
    public static String getCacheDir(String directory) throws IOException {
        File f = new File(Environment.getExternalStorageDirectory().getPath() + "/tumcampus/" + directory);
        if (!f.exists()) {
            f.mkdirs();
        }
        if (!f.canRead()) {
            throw new IOException("Cannot read from SD-Card");
        }
        if (!f.canWrite()) {
            throw new IOException("Cannot write to SD-Card");
        }
        return f.getPath() + "/";
    }

    /**
     * Converts a date-string to Date
     *
     * @param str String with ISO-Date (yyyy-mm-dd)
     * @return Date
     */
    public static Date getDate(String str) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            return dateFormat.parse(str);
        } catch (Exception e) {
            log(e, str);
        }
        return new Date();
    }

    /**
     * Converts Date to an ISO date-string
     *
     * @param d Date
     * @return String (yyyy-mm-dd)
     */
    public static String getDateString(Date d) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormat.format(d);
    }

    /**
     * Converts Date to an ISO datetime-string
     *
     * @param d Date
     * @return String (yyyy-mm-dd hh:mm:ss)
     */
    public static String getDateTimeString(Date d) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(d);
    }

    /**
     * Converts a datetime-string to Date
     *
     * @param str String with ISO-DateTime (yyyy-mm-dd hh:mm:ss)
     * @return Date
     */
    public static Date getISODateTime(String str) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return dateFormat.parse(str);
        } catch (Exception e) {
            log(e, str);
        }
        return new Date();
    }

    /**
     * Get a value from the default shared preferences
     *
     * @param c   Context
     * @param key setting name
     * @return setting value, "" if undefined
     */
    public static String getSetting(Context c, String key) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        return sp.getString(key, "");
    }

    /**
     * Return the boolean value of a setting
     *
     * @param c          Context
     * @param name       setting name
     * @param defaultVal default value
     * @return true if setting was checked, else value
     */
    public static boolean getSettingBool(Context c, String name, boolean defaultVal) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        return sp.getBoolean(name, defaultVal);
    }

    /**
     * Check if a network connection is available or can be available soon
     *
     * @return true if available
     */
    public static boolean isConnected(Context con) {
        ConnectivityManager cm = (ConnectivityManager) con
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    /**
     * Check if a network connection is available or can be available soon
     * and if the available connection is a mobile internet connection
     *
     * @return true if available
     */
    public static boolean isConnectedMobileData(Context con) {
        ConnectivityManager cm = (ConnectivityManager) con
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        return netInfo != null && netInfo.isConnectedOrConnecting() && netInfo.getType() == ConnectivityManager.TYPE_MOBILE;
    }

    /**
     * Logs an exception and additional information
     * Use this anywhere in the app when a fatal error occurred.
     * If you can give a better description of what went wrong
     * use {@link #log(Exception, String)} instead.
     *
     * @param e Exception (source for message and stack trace)
     */
    public static void log(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String s = Thread.currentThread().getStackTrace()[3].getClassName().replaceAll("[a-zA-Z0-9.]+\\.", "");
        Log.e(s, e + "\n" + sw.toString());
    }

    /**
     * Logs an exception and additional information
     * Use this anywhere in the app when a fatal error occurred.
     * If you can't give an exact error description simply use
     * {@link #log(Exception)} instead.
     *
     * @param e       Exception (source for message and stack trace)
     * @param message Additional information for exception message
     */
    public static void log(Exception e, String message) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String s = Thread.currentThread().getStackTrace()[3].getClassName().replaceAll("[a-zA-Z0-9.]+\\.", "");
        Log.e(s, e + " " + message + "\n" + sw.toString());
    }

    /**
     * Logs a message
     * Use this to log the current app state.
     *
     * @param message Information or Debug message
     */
    public static void log(String message) {
        String s = Thread.currentThread().getStackTrace()[3].getClassName().replaceAll("[a-zA-Z0-9.]+\\.", "");
        Log.d(s, message);
    }

    /**
     * Logs a message
     * Use this to log additional information that is not important in most cases.
     *
     * @param message Information or Debug message
     */
    public static void logv(String message) {
        String s = Thread.currentThread().getStackTrace()[3].getClassName().replaceAll("[a-zA-Z0-9.]+\\.", "");
        Log.v(s, message);
    }

    /**
     * Get md5 hash from string
     *
     * @param str String to hash
     * @return md5 hash as string
     */
    public static String md5(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.reset();
            md.update(str.getBytes());
            BigInteger bigInt = new BigInteger(1, md.digest());
            return bigInt.toString(16);
        } catch (Exception e) {
            log(e, str);
        }
        return "";
    }

    /**
     * Returns a String[]-List from a CSV input stream
     *
     * @param fin CSV input stream
     * @return String[]-List with Columns matched to array values
     */
    public static List<String[]> readCsv(InputStream fin) {
        List<String[]> list = new ArrayList<String[]>();
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(fin, "ISO-8859-1"));
            String reader;
            while ((reader = in.readLine()) != null) {
                list.add(splitCsvLine(reader));
            }
            in.close();
        } catch (Exception e) {
            log(e, "");
        }
        return list;
    }

    /**
     * Sets the value of a setting
     *
     * @param c   Context
     * @param key setting key
     */
    public static void setSetting(Context c, String key, boolean value) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        sp.edit().putBoolean(key, value).apply();
    }

    /**
     * Sets the value of a setting
     *
     * @param c   Context
     * @param key setting key
     */
    public static void setSetting(Context c, String key, String value) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        sp.edit().putString(key, value).apply();
    }

    /**
     * Shows a long {@link Toast} message.
     *
     * @param context The activity where the toast is shown
     * @param msg     The toast message id
     */
    public static void showToast(Context context, int msg) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }

    /**
     * Shows a long {@link Toast} message.
     *
     * @param context The activity where the toast is shown
     * @param msg     The toast message
     */
    public static void showToast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }

    /**
     * Splits a line from a CSV file into column values
     * <p/>
     * e.g. "aaa;aaa";"bbb";1 gets aaa,aaa;bbb;1;
     *
     * @param str CSV line
     * @return String[] with CSV column values
     */
    private static String[] splitCsvLine(String str) {
        StringBuilder result = new StringBuilder();
        boolean open = false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '"') {
                open = !open;
                continue;
            }
            if (open && c == ';') {
                result.append(",");
            } else {
                result.append(c);
            }
        }
        // fix trailing ";", e.g. ";;;".split().length = 0
        result.append("; ");
        return result.toString().split(";");
    }

    /**
     * Converts a meter based value to a formatted string
     *
     * @param meters Meters to represent
     * @return Formatted meters. e.g. 10m, 12.5km
     */
    public static String formatDist(float meters) {
        if (meters < 1000) {
            return ((int) meters) + "m";
        } else if (meters < 10000) {
            int front = (int) (meters / 1000f);
            int back = (int) Math.abs(meters / 100f) % 10;
            return front + "." + back + "km";
        } else {
            return ((int) (meters / 1000f)) + "km";
        }
    }

    /**
     * Sets an internal preference's boolean value
     *
     * @param context Context
     * @param key     Key
     * @param value   Value
     */
    public static void setInternalSetting(Context context, String key, boolean value) {
        SharedPreferences prefs = context.getSharedPreferences(Const.INTERNAL_PREFS, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(key, value).apply();
    }

    /**
     * Sets an internal preference's integer value
     *
     * @param context Context
     * @param key     Key
     * @param value   Value
     */
    public static void setInternalSetting(Context context, String key, int value) {
        SharedPreferences prefs = context.getSharedPreferences(Const.INTERNAL_PREFS, Context.MODE_PRIVATE);
        prefs.edit().putInt(key, value).apply();
    }

    /**
     * Sets an internal preference's string value
     *
     * @param context Context
     * @param key     Key
     * @param value   Value
     */
    public static void setInternalSetting(Context context, String key, String value) {
        SharedPreferences prefs = context.getSharedPreferences(Const.INTERNAL_PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(key, value).apply();
    }

    /**
     * Gets an internal preference's boolean value
     *
     * @param context Context
     * @param key     Key
     * @param value   Default value
     * @return The value of the setting or the default value,
     * if no setting with the specified key exists
     */
    public static boolean getInternalSettingBool(Context context, String key, boolean value) {
        SharedPreferences prefs = context.getSharedPreferences(Const.INTERNAL_PREFS, Context.MODE_PRIVATE);
        return prefs.getBoolean(key, value);
    }

    /**
     * Gets an internal preference's integer value
     *
     * @param context Context
     * @param key     Key
     * @param value   Default value
     * @return The value of the setting or the default value,
     * if no setting with the specified key exists
     */
    public static int getInternalSettingInt(Context context, String key, int value) {
        SharedPreferences prefs = context.getSharedPreferences(Const.INTERNAL_PREFS, Context.MODE_PRIVATE);
        return prefs.getInt(key, value);
    }

    /**
     * Gets an internal preference's string value
     *
     * @param context Context
     * @param key     Key
     * @param value   Default value
     * @return The value of the setting or the default value,
     * if no setting with the specified key exists
     */
    public static String getInternalSettingString(Context context, String key, String value) {
        SharedPreferences prefs = context.getSharedPreferences(Const.INTERNAL_PREFS, Context.MODE_PRIVATE);
        return prefs.getString(key, value);
    }

    /**
     * Gets an unique id that identifies this device
     *
     * @param context Context
     * @return Unique device id
     */
    public synchronized static String getDeviceID(Context context) {
        if (uniqueID == null) {
            uniqueID = getInternalSettingString(context, PREF_UNIQUE_ID, null);
            if (uniqueID == null) {
                uniqueID = UUID.randomUUID().toString();
                setInternalSetting(context, PREF_UNIQUE_ID, uniqueID);
            }
        }
        return uniqueID;
    }
}
