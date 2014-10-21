package de.tum.in.tumcampus.models.managers;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.gson.Gson;

import java.security.PrivateKey;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import de.tum.in.tumcampus.auxiliary.Utils;
import de.tum.in.tumcampus.models.ChatClient;
import de.tum.in.tumcampus.models.ChatMember;
import de.tum.in.tumcampus.models.ChatMessage;
import de.tum.in.tumcampus.models.ChatVerification;

/**
 * TUMOnline cache manager, allows caching of TUMOnline requests
 */
public class ChatMessageManager {


    public static final int COL_ID = 0;
    public static final int COL_PREVIOUS = 1;
    public static final int COL_ROOM = 2;
    public static final int COL_TEXT = 3;
    public static final int COL_TIMESTAMP = 4;
    public static final int COL_SIGNATURE = 5;
    public static final int COL_MEMBER = 6;

    /**
     * Database connection
     */
    private final SQLiteDatabase db;
    private final int mChatRoom;
    private Context mContext;

    /**
     * Constructor, open/create database, create table if necessary
     *
     * @param context Context
     */
    public ChatMessageManager(Context context, int room) {
        db = DatabaseManager.getDb(context);
        mContext = context;
        mChatRoom = room;
        init(db);
    }

    private static void init(SQLiteDatabase db) {
        // create tables if needed
        db.execSQL("CREATE TABLE IF NOT EXISTS chat_message (_id INTEGER PRIMARY KEY, previous INTEGER, room INTEGER, " +
                "text TEXT, timestamp VARCHAR, signature TEXT, member BLOB, status INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS unsent_chat_message (_id INTEGER PRIMARY KEY AUTOINCREMENT, room INTEGER, text TEXT, member BLOB)");

        // Delete all entries that are too old
        db.rawQuery("DELETE FROM chat_message WHERE timestamp<datetime('now','-1 month')", null);
    }

    /**
     * Gets all messages for the room
     *
     * @return List of chat messages
     */
    public Cursor getAll() {
        markAsRead();
        return db.rawQuery("SELECT c.* FROM chat_message c, (SELECT c1._id " +
                "FROM chat_message c1 LEFT JOIN chat_message c2 ON c2._id=c1.previous " +
                "WHERE c2._id IS NULL AND c1.room=? " +
                "ORDER BY c1._id DESC " +
                "LIMIT 1) AS until " +
                "WHERE c._id>=until._id AND c.room=? " +
                "ORDER BY c._id", new String[]{""+mChatRoom, ""+mChatRoom});
    }

    public void markAsRead() {
        db.execSQL("UPDATE chat_message SET status=1 WHERE status=0 AND room=?", new String[]{"" + mChatRoom});
    }

    /**
     * Gets all unsent chat messages
     */
    public static ArrayList<ChatMessage> getAllUnsent(Context context) {
        SQLiteDatabase db = DatabaseManager.getDb(context);
        init(db);
        Cursor cur = db.rawQuery("SELECT member, text, room, _id FROM unsent_chat_message ORDER BY _id", null);
        ArrayList<ChatMessage> list = new ArrayList<ChatMessage>(cur.getCount());
        if(cur.moveToFirst()) {
            do {
                ChatMember member = new Gson().fromJson(cur.getString(0), ChatMember.class);
                ChatMessage msg = new ChatMessage(cur.getString(1), member);
                msg.setRoom(cur.getInt(2));
                msg.setId(cur.getInt(3));
                list.add(msg);
            } while(cur.moveToNext());
        }
        cur.close();
        return list;
    }

    /**
     * Gets all unsent chat messages from the current room
     */
    public ArrayList<ChatMessage> getAllUnsent() {
        return getAllUnsent(mContext);
    }

    /**
     * Saves the given message into database
     */
    public void addToUnsent(ChatMessage m) {
        Log.e("TCA Chat", "replace into unsent " + m.getText() + " " + m.getId() + " " + m.getPrevious() + " " + m.getStatus());
        db.execSQL("REPLACE INTO unsent_chat_message (text,room,member) VALUES (?,?,?)",
                new String[]{"" + m.getText(), "" + mChatRoom, new Gson().toJson(m.getMember())});
    }

    /**
     * Removes the message from unsent database
     * */
    public void removeFromUnsent(ChatMessage message) {
        db.execSQL("DELETE FROM unsent_chat_message WHERE _id=?", new String[]{"" + message.getId()});
    }

    /**
     * Gets all messages marked as unread
     */
    public Cursor getUnread() {
        return db.rawQuery("SELECT c.* FROM chat_message c, (SELECT c1._id " +
                "FROM chat_message c1 LEFT JOIN chat_message c2 ON c2._id=c1.previous " +
                "WHERE (c2._id IS NULL OR c1.status=1) AND c1.room=? " +
                "ORDER BY c1._id DESC " +
                "LIMIT 1) AS until " +
                "WHERE c._id>until._id AND c.room=? " +
                "ORDER BY c._id", new String[]{""+mChatRoom, ""+mChatRoom});
    }

    /**
     * Gets all unread chat messages
     */
    public ArrayList<ChatMessage> getLastUnread() {
        Cursor cur = db.rawQuery("SELECT c.* FROM chat_message c, (SELECT c1._id " +
                "FROM chat_message c1 LEFT JOIN chat_message c2 ON c2._id=c1.previous " +
                "WHERE (c2._id IS NULL OR c1.status=1) AND c1.room=? " +
                "ORDER BY c1._id DESC " +
                "LIMIT 1) AS until " +
                "WHERE c._id>until._id AND c.room=? " +
                "ORDER BY c._id " +
                "LIMIT 5", new String[]{"" + mChatRoom, "" + mChatRoom});
        ArrayList<ChatMessage> list = new ArrayList<ChatMessage>(cur.getCount());
        if(cur.moveToFirst()) {
            do {
                ChatMember member = new Gson().fromJson(cur.getString(0), ChatMember.class);
                ChatMessage msg = new ChatMessage(cur.getString(1), member);
                list.add(msg);
            } while(cur.moveToNext());
        }
        cur.close();
        return list;
    }

    /**
     * Saves the given message into database
     */
    public void replaceInto(ChatMessage m, boolean mine) {
        if (m == null || m.getText() == null) {
            Log.e("TCA Chat", "Message empty");
            return;
        }

        Log.e("TCA Chat", "replace " + m.getText() + " " + m.getId() + " "+ m.getPrevious()+ " "+ m.getStatus());
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);
        Date date;
        try {
            date = formatter.parse(m.getTimestamp());
        } catch (ParseException e) {
            date = new Date();
        }
        db.beginTransaction();
        // Query status from the previous message and use this status as well if it is "0"
        Cursor cur = db.rawQuery("SELECT status FROM chat_message WHERE _id=?", new String[] {""+m.getPrevious()});
        if(cur.moveToFirst()) {
            if(cur.getInt(0)==0)
                mine = false;
        }
        cur.close();
        db.execSQL("REPLACE INTO chat_message (_id,previous,room,text,timestamp,signature,member,status) VALUES (?,?,?,?,?,?,?,?)",
                new String[]{"" + m.getId(), "" + m.getPrevious(), "" + mChatRoom, m.getText(), Utils.getDateTimeString(date),
                        m.getSignature(), (new Gson().toJson(m.getMember())), mine ? "1" : "0"});
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    /**
     * Saves the given message into database
     */
    public boolean replaceInto(List<ChatMessage> m) {
        db.beginTransaction();
        Cursor cur = db.rawQuery("SELECT _id FROM chat_message WHERE room=?", new String[]{"" + mChatRoom});
        HashSet<Integer> set = new HashSet<Integer>();
        int min = Integer.MAX_VALUE;
        if (cur.moveToFirst()) {
            do {
                final int val = cur.getInt(0);
                if (val < min)
                    min = val;
                set.add(val);
            } while (cur.moveToNext());
        }
        cur.close();
        int minNew = Integer.MAX_VALUE;
        boolean newMessages = false;
        for (ChatMessage msg : m) {
            if (!set.contains(msg.getId())) {
                newMessages = true;
                replaceInto(msg, false);
            }
            if (msg.getId() < minNew)
                minNew = msg.getId();
        }
        db.setTransactionSuccessful();
        db.endTransaction();
        return newMessages || minNew > min;
    }

    public static ChatMessage toObject(Cursor cursor) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);
        int id = cursor.getInt(COL_ID);
        String text = cursor.getString(COL_TEXT);
        String time = formatter.format(Utils.getISODateTime(cursor.getString(COL_TIMESTAMP)));
        int previous = cursor.getInt(COL_PREVIOUS);
        ChatMember member = new Gson().fromJson(cursor.getString(COL_MEMBER), ChatMember.class);
        ChatMessage msg = new ChatMessage(id, text, member, time, previous);
        msg.setSignature(cursor.getString(COL_SIGNATURE));
        msg.setRoom(cursor.getInt(COL_ROOM));
        return msg;
    }

    public Cursor getNewMessages(PrivateKey pk, ChatMember member) {
        ArrayList<ChatMessage> messages = ChatClient.getInstance(mContext).getNewMessages(mChatRoom, new ChatVerification(pk, member));
        replaceInto(messages);
        return getUnread();
    }
}