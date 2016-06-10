package de.tum.in.tumcampusapp.models.managers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import de.tum.in.tumcampusapp.auxiliary.Const;
import de.tum.in.tumcampusapp.auxiliary.NetUtils;
import de.tum.in.tumcampusapp.auxiliary.Utils;
import de.tum.in.tumcampusapp.cards.Card;
import de.tum.in.tumcampusapp.cards.SurveyCard;
import de.tum.in.tumcampusapp.models.CalendarRowSet;
import de.tum.in.tumcampusapp.models.Faculty;
import de.tum.in.tumcampusapp.models.Kino;
import de.tum.in.tumcampusapp.models.News;
import de.tum.in.tumcampusapp.models.Question;
import de.tum.in.tumcampusapp.models.TUMCabeClient;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by aser on 5/5/16.
 */
public class SurveyManager extends AbstractManager implements Card.ProvidesCard {

    private static int TIME_TO_SYNC = 1800; // weiss nicht wie oft
    private static final String FACULTY_URL = "https://tumcabe.in.tum.de/Api/faculty";
    private static final String OPEN_QUESTIONS_URL = "https://tumcabe.in.tum.de/Api/question/";

    public SurveyManager(Context context) {
        super(context);

        db.execSQL("CREATE TABLE IF NOT EXISTS surveyQuestions (id INTEGER PRIMARY KEY, question VARCHAR, yes BOOLEAN, no BOOLEAN, flagged BOOLEAN, answered BOOLEAN, synced BOOLEAN)");
        db.execSQL("CREATE TABLE IF NOT EXISTS faculties (faculty INTEGER, name VARCHAR)");
        db.execSQL("CREATE TABLE IF NOT EXISTS survey1 (id INTEGER PRIMARY KEY AUTOINCREMENT, date VARCHAR,userID VARCHAR, question TEXT, faculties TEXT, "
                + "yes INTEGER,  no INTEGER, flags INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS openQuestions (question INTEGER PRIMARY KEY, text VARCHAR, yes BOOLEAN, no BOOLEAN, flagged BOOLEAN, answered BOOLEAN, synced BOOLEAN)");
        db.execSQL("CREATE TABLE IF NOT EXISTS ownQuestions (question INTEGER PRIMARY KEY, text VARCHAR, yes INTEGER, no INTEGER, deleted BOOLEAN, synced BOOLEAN)");
        //generateTestData(); // Untill the API is done
        //dropTestData(); // untill the API is done
    }

    @Override
    public void onRequestCard(Context context) {
        Cursor rows = getUnansweredQuestions();//getNextQuestions();
        if (rows.moveToFirst()) {
            SurveyCard card = new SurveyCard(context);
            card.seQuestions(rows); // Questions from local DB (that were downloaded using the API) should be given here.
            card.apply();
        }
    }

    public Cursor getAllFaculties() {
        return db.rawQuery("SELECT * FROM faculties", null);
    }


    // Get the relevant Questions for the Survey Card (not answered)
    public Cursor getNextQuestions() {
        //return db.rawQuery("SELECT id, question, yes, no, flagged, answered, synced FROM surveyQuestions where answered=0", null);
        return db.rawQuery("SELECT question, text FROM openQuestions", null);
    }

    public Cursor getUnansweredQuestions() {
        Log.d("getUnansweredQuestions", "ichLebe");
        //return db.rawQuery("SELECT question, text FROM openQuestions WHERE answered=?",new String[]{"0"}); Irgendwie wenn man die App nochmal startet kommen die nochmal
        return db.rawQuery("SELECT question, text FROM openQuestions WHERE answered=0", null);
    }

    // For testing purposes untill the API is done
    public void generateTestData() {
        ContentValues cv = new ContentValues(7);
        for (int i = 0; i < 10; i++) {
            cv.put("id", i);
            cv.put("question", "Question " + i);
            cv.put("yes", 0);
            cv.put("no", 0);
            cv.put("flagged", 0);
            cv.put("answered", 0);
            cv.put("synced", 0);
            db.insert("surveyQuestions", null, cv);
        }
    }

    // For Testing Purposes untill the API is done
    public void dropTestData() {
        db.delete("surveyQuestions", null, null);
    }

    /**
     * updates the field of a given question
     *
     * @param question
     * @param updateField: yes || no || flag
     */
    public void updateQuestion(Question question, String updateField) {
        ContentValues cv = new ContentValues();
        cv.put(updateField, 1);

        //Handle that this card was finished and should not be shown again
        if (!updateField.equals("answered")) {
            cv.put("answered", 1);
        }

        //Commit update to database
        db.update("openQuestions", cv, "question = ?", new String[]{question.getQuestion().toString()});
        Log.d("Question " + question.getQuestion() + "", updateField + " is set");

        //Tigger sync if we are connected currently
        if (NetUtils.isConnected(mContext)) {
            Log.d("DeviceIsConnected", "true");
            syncOpenQuestionsTable();
        }

    }

    // Not done yet
    public void syncOpenQuestionsTable() {
        Cursor cursor = db.rawQuery("SELECT question, yes, no, flagged FROM openQuestions WHERE synced=0 AND answered=1", null);
        try {
            if (cursor.moveToFirst()) {
                do {
                    String question = cursor.getString(cursor.getColumnIndex("question"));
                    String yes = cursor.getString(cursor.getColumnIndex("yes"));
                    String no = cursor.getString(cursor.getColumnIndex("no"));
                    String flagged = cursor.getString(cursor.getColumnIndex("flagged"));


                    Question answeredQuestion;
                    if (!"0".equals(yes) && "0".equals(no) && "0".equals(flagged)) {
                        answeredQuestion = new Question(question, yes);
                        TUMCabeClient.getInstance(mContext).submitAnswer(answeredQuestion, new Callback<Question>() {
                            @Override
                            public void success(Question question, Response response) {
                                Log.e("Test_resp_submitQues", "Succeeded: " + response.getBody().toString());
                            }

                            @Override
                            public void failure(RetrofitError error) {
                                Log.e("Test_resp_submitQues", "Failure");
                            }
                        });
                    } else if ("0".equals(yes) && !"0".equals(no) && "0".equals(flagged)) {
                        answeredQuestion = new Question(question, no);
                        TUMCabeClient.getInstance(mContext).submitAnswer(answeredQuestion, new Callback<Question>() {
                            @Override
                            public void success(Question question, Response response) {
                                Log.e("Test_resp_submitQues", "Succeeded: " + response.getBody().toString());
                            }

                            @Override
                            public void failure(RetrofitError error) {
                                Log.e("Test_resp_submitQues", "Failure" + error.toString());
                            }
                        });
                    } else if ("0".equals(yes) && "0".equals(no) && !"0".equals(flagged)) { // until flagged is available in the API
                    /*answeredQuestion = new Question(question,flagged);
                    TUMCabeClient.getInstance(mContext).submitAnswer(answeredQuestion, new Callback<Question>() {
                        @Override
                        public void success(Question question, Response response) {
                            Log.e("Test_resp_submitQues","Succeeded: "+response.getBody().toString());
                        }

                        @Override
                        public void failure(RetrofitError error) {
                            Log.e("Test_resp_submitQues","Failure");
                        }
                    });*/
                    }
                    ContentValues cv = new ContentValues();
                    cv.put("synced", "1");
                    db.update("openQuestions", cv, "question = ?", new String[]{cursor.getString(cursor.getColumnIndex("question")) + ""});
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.d("SyncException", e.toString());
        }
    }

    public void insertOwnQuestions(String date, String userID, String question, String faculties) {
        ContentValues cv = new ContentValues(8);
        cv.put("date", date);
        cv.put("userID", userID);
        cv.put("question", question);
        cv.put("faculties", faculties);
        cv.put("yes", 0);
        cv.put("no", 0);
        cv.put("flags", 0);
        db.insert("survey1", null, cv);
    }

    // Helpfunction used for testing in Survey Acitvity untill the API is implemented
    public Cursor numberOfQuestionsFrom(String weekago) {
        return db.rawQuery("SELECT COUNT(*) FROM survey1 WHERE date >= '" + weekago + "'", null);
    }

    public Cursor getMyQuestions() {
        return db.rawQuery("SELECT * FROM myQuestions", null);

    }


    // Helpfunction used for testing in Survey Acitvity untill the API is implemented
    public Cursor lastDateFromLastWeek(String weekAgo) {
        return db.rawQuery("SELECT date FROM survey1 WHERE date >= '" + weekAgo + "'", null);
    }

    public Cursor getFacultyID(String facultyName) {
        return db.rawQuery("SELECT faculty FROM faculties WHERE name=?", new String[]{facultyName});
    }

    public void downloadFromExternal(boolean force) throws Exception {

        if (!force && !SyncManager.needSync(db, this, TIME_TO_SYNC)) {
            return;
        }

        NetUtils net = new NetUtils(mContext);

        // Load all faculties
        JSONArray jsonArray = net.downloadJsonArray(FACULTY_URL, CacheManager.VALIDITY_ONE_DAY, force);
        if (jsonArray == null) {
            return;
        }

        db.beginTransaction();
        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                replaceIntoDb(getFromJson(obj));
            }
            SyncManager.replaceIntoDb(db, this);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }


    // For the SurveyCard
    public void downLoadOpenQuestions() {
        ArrayList<Question> openQuestions = new ArrayList<Question>();
        try {
            openQuestions = TUMCabeClient.getInstance(mContext).getOpenQuestions();
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (int i = 0; i < openQuestions.size(); i++) {
            List<String> openQuestionFaculties = Arrays.asList(openQuestions.get(i).getFacultiesOfOpenQuestions());
            if (openQuestionFaculties.contains(Utils.getInternalSettingString(mContext, "user_major", ""))) {
                replaceIntoOpenQuestions(openQuestions.get(i));
            }
        }
    }

    void replaceIntoOpenQuestions(Question q) {
        db.execSQL("REPLACE INTO openQuestions (question, text, yes, no, flagged, answered, synced) VALUES (?, ?, ?, ?, ?, ?, ?)",
                new Object[]{q.getQuestion(), q.getText(), 0, 0, 0, 0, 0});
    }

    void replaceIntoDb(Faculty f) {
        db.execSQL("REPLACE INTO faculties (faculty, name) VALUES (?, ?)",
                new String[]{f.getId(), f.getName()});
    }

    private static Faculty getFromJson(JSONObject json) throws Exception {
        String id = json.getString(Const.JSON_FACULTY);
        String name = json.getString(Const.JSON_FACULTY_NAME);
        return new Faculty(id, name);
    }
}
