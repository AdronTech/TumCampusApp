/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.tum.in.tumcampus.auxiliary.calendar;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.os.Handler;
import android.os.Process;
import android.provider.CalendarContract.EventDays;
import android.text.format.Time;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import de.tum.in.tumcampus.models.managers.CalendarManager;

public class EventLoaderCalendar {

    private Context mContext;
    private Handler mHandler = new Handler();
    private AtomicInteger mSequenceNumber = new AtomicInteger();

    private LinkedBlockingQueue<LoadRequest> mLoaderQueue;
    private LoaderThread mLoaderThread;
    private ContentResolver mResolver;

    public EventLoaderCalendar(Context context) {
        mContext = context;
        mLoaderQueue = new LinkedBlockingQueue<>();
        mResolver = context.getContentResolver();
    }

    /**
     * Call this from the activity's onResume()
     */
    public void startBackgroundThread() {
        mLoaderThread = new LoaderThread(mLoaderQueue, this);
        mLoaderThread.start();
    }

    /**
     * Call this from the activity's onPause()
     */
    public void stopBackgroundThread() {
        mLoaderThread.shutdown();
    }

    /**
     * Loads "numDays" days worth of events, starting at start, into events.
     * Posts uiCallback to the {@link android.os.Handler} for this view, which will run in the UI thread.
     * Reuses an existing background thread, if events were already being loaded in the background.
     * NOTE: events and uiCallback are not used if an existing background thread gets reused --
     * the ones that were passed in on the call that results in the background thread getting
     * created are used, and the most recent call's worth of data is loaded into events and posted
     * via the uiCallback.
     */
    public void loadEventsInBackground(final int numDays, final ArrayList<Event> events,
                                       int startDay, final Runnable successCallback, final Runnable cancelCallback) {

        // Increment the sequence number for requests.  We don't care if the
        // sequence numbers wrap around because we test for equality with the
        // latest one.
        int id = mSequenceNumber.incrementAndGet();

        // Send the load request to the background thread
        LoadEventsRequest request = new LoadEventsRequest(id, startDay, numDays,
                events, successCallback, cancelCallback);

        try {
            mLoaderQueue.put(request);
        } catch (InterruptedException ex) {
            // The put() method fails with InterruptedException if the
            // queue is full. This should never happen because the queue
            // has no limit.
            Log.e("Cal", "loadEventsInBackground() interrupted!");
        }
    }

    /**
     * Sends a request for the days with events to be marked. Loads "numDays"
     * worth of days, starting at start, and fills in eventDays to express which
     * days have events.
     *
     * @param startDay   First day to check for events
     * @param numDays    Days following the start day to check
     * @param eventDays  Whether or not an event exists on that day
     * @param uiCallback What to do when done (log data, redraw screen)
     */
    void loadEventDaysInBackground(int startDay, int numDays, boolean[] eventDays,
                                   final Runnable uiCallback) {
        // Send load request to the background thread
        LoadEventDaysRequest request = new LoadEventDaysRequest(startDay, numDays,
                eventDays, uiCallback);
        try {
            mLoaderQueue.put(request);
        } catch (InterruptedException ex) {
            // The put() method fails with InterruptedException if the
            // queue is full. This should never happen because the queue
            // has no limit.
            Log.e("Cal", "loadEventDaysInBackground() interrupted!");
        }
    }

    private interface LoadRequest {
        void processRequest(EventLoaderCalendar eventLoader);

        void skipRequest(EventLoaderCalendar eventLoader);
    }

    private static class ShutdownRequest implements LoadRequest {
        public void processRequest(EventLoaderCalendar eventLoader) {
        }

        public void skipRequest(EventLoaderCalendar eventLoader) {
        }
    }

    /**
     *
     * Code for handling requests to get whether days have an event or not
     * and filling in the eventDays array.
     *
     */
    private static class LoadEventDaysRequest implements LoadRequest {
        /**
         * The projection used by the EventDays query.
         */
        private static final String[] PROJECTION = {
                EventDays.STARTDAY, EventDays.ENDDAY
        };
        public int startDay;
        public int numDays;
        public boolean[] eventDays;
        public Runnable uiCallback;

        public LoadEventDaysRequest(int startDay, int numDays, boolean[] eventDays,
                                    final Runnable uiCallback) {
            this.startDay = startDay;
            this.numDays = numDays;
            this.eventDays = eventDays;
            this.uiCallback = uiCallback;
        }

        @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        @Override
        public void processRequest(EventLoaderCalendar eventLoader) {
            final Handler handler = eventLoader.mHandler;
            ContentResolver cr = eventLoader.mResolver;

            // Clear the event days
            Arrays.fill(eventDays, false);

            //query which days have events
            Cursor cursor = EventDays.query(cr, startDay, numDays, PROJECTION);
            try {
                int startDayColumnIndex = cursor.getColumnIndexOrThrow(EventDays.STARTDAY);
                int endDayColumnIndex = cursor.getColumnIndexOrThrow(EventDays.ENDDAY);

                //Set all the days with events to true
                while (cursor.moveToNext()) {
                    int firstDay = cursor.getInt(startDayColumnIndex);
                    int lastDay = cursor.getInt(endDayColumnIndex);
                    //we want the entire range the event occurs, but only within the month
                    int firstIndex = Math.max(firstDay - startDay, 0);
                    int lastIndex = Math.min(lastDay - startDay, 30);

                    for (int i = firstIndex; i <= lastIndex; i++) {
                        eventDays[i] = true;
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            handler.post(uiCallback);
        }

        @Override
        public void skipRequest(EventLoaderCalendar eventLoader) {
        }
    }

    private static class LoadEventsRequest implements LoadRequest {

        public int id;
        public int startDay;
        public int numDays;
        public ArrayList<Event> events;
        public Runnable successCallback;
        public Runnable cancelCallback;

        public LoadEventsRequest(int id, int startDay, int numDays, ArrayList<Event> events,
                                 final Runnable successCallback, final Runnable cancelCallback) {
            this.id = id;
            this.startDay = startDay;
            this.numDays = numDays;
            this.events = events;
            this.successCallback = successCallback;
            this.cancelCallback = cancelCallback;
        }

        /**
         * Loads <i>days</i> days worth of instances starting at <i>startDay</i>.
         */
        public static void loadEvents(Context context, ArrayList<Event> events, int startDay, int days) {
            events.clear();

            CalendarManager cm = new CalendarManager(context);
            Time date = new Time();

            for (int curDay = startDay; curDay < startDay + days; curDay++) {
                date.setJulianDay(curDay);
                Cursor cEvents = cm.getFromDbForDate(new Date(date.toMillis(false)));

                while (cEvents.moveToNext())
                    events.add(Event.generateEventFromCursor(cEvents));
            }
        }

        public void processRequest(EventLoaderCalendar eventLoader) {
            loadEvents(eventLoader.mContext, events, startDay, numDays);

            // Check if we are still the most recent request.
            if (id == eventLoader.mSequenceNumber.get()) {
                eventLoader.mHandler.post(successCallback);
            } else {
                eventLoader.mHandler.post(cancelCallback);
            }
        }

        public void skipRequest(EventLoaderCalendar eventLoader) {
            eventLoader.mHandler.post(cancelCallback);
        }
    }

    private static class LoaderThread extends Thread {
        LinkedBlockingQueue<LoadRequest> mQueue;
        EventLoaderCalendar mEventLoader;

        public LoaderThread(LinkedBlockingQueue<LoadRequest> queue, EventLoaderCalendar eventLoader) {
            mQueue = queue;
            mEventLoader = eventLoader;
        }

        public void shutdown() {
            try {
                mQueue.put(new ShutdownRequest());
            } catch (InterruptedException ex) {
                // The put() method fails with InterruptedException if the
                // queue is full. This should never happen because the queue
                // has no limit.
                Log.e("Cal", "LoaderThread.shutdown() interrupted!");
            }
        }

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            while (true) {
                try {
                    // Wait for the next request
                    LoadRequest request = mQueue.take();

                    // If there are a bunch of requests already waiting, then
                    // skip all but the most recent request.
                    while (!mQueue.isEmpty()) {
                        // Let the request know that it was skipped
                        request.skipRequest(mEventLoader);

                        // Skip to the next request
                        request = mQueue.take();
                    }

                    if (request instanceof ShutdownRequest) {
                        return;
                    }
                    request.processRequest(mEventLoader);
                } catch (InterruptedException ex) {
                    Log.e("Cal", "background LoaderThread interrupted!");
                }
            }
        }
    }
}
