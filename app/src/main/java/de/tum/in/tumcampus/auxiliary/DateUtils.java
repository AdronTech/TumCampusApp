package de.tum.in.tumcampus.auxiliary;

import android.content.Context;
import android.text.format.Time;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

    private static final long MINUTE_MILLIS = android.text.format.DateUtils.MINUTE_IN_MILLIS;
    private static final long HOUR_MILLIS = android.text.format.DateUtils.HOUR_IN_MILLIS;
    private static final long DAY_MILLIS = android.text.format.DateUtils.DAY_IN_MILLIS;

    private static final String formatSQL = "yyyy-MM-dd HH:mm:ss"; // 2014-06-30 16:31:57
    private static final String formatISO = "yyyy-MM-dd'T'HH:mm:ss'Z'"; // 2014-06-30T16:31:57.878Z
    private static final String logTag = "DateUtils";

    public static String getRelativeTimeISO(String timestamp, Context context) {
            return DateUtils.getRelativeTime(DateUtils.parseIsoDate(timestamp), context);
    }

    private static String getRelativeTime(Date date, Context context) {
        if (date == null) {
            return "";
        }

        return android.text.format.DateUtils.getRelativeDateTimeString(context, date.getTime(),
                MINUTE_MILLIS, DAY_MILLIS * 2L, 0).toString();
    }

    public static String getTimeOrDay(String datetime) {
        return DateUtils.getTimeOrDay(DateUtils.parseSqlDate(datetime));
    }

    public static String getTimeOrDay(Date time) {
        if (time == null) {
            return "";
        }

        long timeInMillis = time.getTime();
        long now = DateUtils.getCurrentTime().toMillis(false);

        //Catch future dates: current clock might be running behind
        if (timeInMillis > now || timeInMillis <= 0) {
            return "Gerade eben";
        }

        // TODO: localize
        final long diff = now - timeInMillis;
        if (diff < MINUTE_MILLIS) {
            return "Gerade eben";
        } else if (diff < 24 * HOUR_MILLIS) {
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
            return formatter.format(time);
        } else if (diff < 48 * HOUR_MILLIS) {
            return "Gestern";
        } else {
            SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy", Locale.ENGLISH);
            return formatter.format(time);
        }
    }

    public static Date parseSqlDate(String datetime) {
        if(datetime == null) {
            return null;
        }
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(DateUtils.formatSQL, Locale.ENGLISH); // 2014-06-30 16:31:57
            return formatter.parse(datetime);
        } catch (ParseException e) {
            Log.e(logTag, "Parsing SQL date failed");
        }
        return null;
    }

    public static Date parseIsoDate(String datetime) {
        if(datetime == null) {
            return null;
        }
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(formatISO, Locale.ENGLISH);
            return formatter.parse(datetime);
        } catch (ParseException e) {
            Log.e(logTag, "Parsing SQL date failed");
        }
        return null;
    }

    private static Time getCurrentTime() {
        Time now = new Time();
        now.setToNow();
        return now;
    }
}
