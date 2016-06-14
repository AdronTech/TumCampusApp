package de.tum.in.tumcampusapp.activities;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import de.tum.in.tumcampusapp.R;
import de.tum.in.tumcampusapp.activities.generic.ProgressActivity;
import de.tum.in.tumcampusapp.auxiliary.Const;
import de.tum.in.tumcampusapp.auxiliary.NetUtils;
import de.tum.in.tumcampusapp.auxiliary.Utils;
import de.tum.in.tumcampusapp.models.Question;
import de.tum.in.tumcampusapp.models.TUMCabeClient;
import de.tum.in.tumcampusapp.models.managers.SurveyManager;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class SurveyActivity extends ProgressActivity {

    Spinner aSpinner1;
    TextView selectTv;
    TabHost tabHost;
    Button submitSurveyButton, facultiesButton;
    ArrayList<String> questions = new ArrayList<>();
    ArrayList<String> selectedFaculties = new ArrayList<>();
    boolean[] checked = new boolean[14];
    LinearLayout mainResponseLayout, questionsLayout;
    String chosenFaculties = "", newDate = "", lrzId;
    ArrayList<String> fetchedFaculties = new ArrayList<>();
    ViewGroup parentView;
    String[] numQues = new String[3];
    int numberOfQuestion=0;
    private SurveyManager surveyManager;
    BroadcastReceiver connectivityChangeReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if(NetUtils.isConnected(getApplicationContext())) {
                restartActivity();
                unregisterReceiver(connectivityChangeReceiver);
            }
        }
    };

    public SurveyActivity(){super(R.layout.activity_survey);}

   @Override
    public void onCreate(Bundle savedInstanceState) {
        surveyManager = new SurveyManager(this);
        super.onCreate(savedInstanceState);
        lrzId = Utils.getSetting(this, Const.LRZ_ID, "");
        registerReceiver(connectivityChangeReceiver,new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        if(NetUtils.isConnected(this)) {
            findViewsById();
            setUpTabHost();
            setUpSpinner();
            setUpSelectTargets();
            submitAndTabListeners();
            userAllowed();
            unregisterReceiver(connectivityChangeReceiver);
        }
        else
            setContentView(R.layout.layout_no_internet);

    }

    @Override
    public void onRefresh(){}

  //set up the respone tab layout dynamically depending on number of questions
    @SuppressLint("SetTextI18n")
    private void setUpResponseTab() {
        Cursor c = surveyManager.getMyOwnQuestionsSince(Utils.getDateTimeString(new Date()));
        int numberofquestion = c.getCount();
        //get response and question from database->set i<Number of question
        for (int i = 0; i < numberofquestion; i++) {

            c.moveToNext();
            String questionText = c.getString(c.getColumnIndex("text"));
            int yes = c.getInt(c.getColumnIndex("yes"));
            int no = c.getInt(c.getColumnIndex("no"));
            int total = yes + no;
            int id = c.getInt(c.getColumnIndex("question"));
            //linear layout for every question
            LinearLayout ques = new LinearLayout(this);
            LinearLayout.LayoutParams quesParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            ques.setOrientation(LinearLayout.VERTICAL);
            ques.setWeightSum(5);
            mainResponseLayout.addView(ques, quesParams);


            LinearLayout l = new LinearLayout(this);
            LinearLayout.LayoutParams lParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            l.setOrientation(LinearLayout.HORIZONTAL);
            l.setWeightSum(5);
            ques.addView(l, lParams);

            LinearLayout l1 = new LinearLayout(this);
            l1.setOrientation(LinearLayout.VERTICAL);
            l1.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 4.5f));
            l.addView(l1);

            LinearLayout l2 = new LinearLayout(this);
            l2.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.3f));
            l2.setOrientation(LinearLayout.VERTICAL);
            l.addView(l2);
            //adding quesion tv
            TextView questionTv = new TextView(this);
            LinearLayout.LayoutParams tvparams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            tvparams.setMargins(50, 0, 0, 0);
            questionTv.setLayoutParams(tvparams);
            questionTv.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.color_primary_dark));
            questionTv.setTypeface(null, Typeface.BOLD);
            //setText(question)
            questionTv.setText(questionText);
            l1.addView(questionTv);
            //adding button delete
            float inPixels = getResources().getDimension(R.dimen.dimen_buttonHeight_in_dp);
            Button deleteButton = new Button(this);
            deleteButton.setLayoutParams(new LinearLayout.LayoutParams((int) inPixels, (int) inPixels));
            deleteButton.setBackgroundResource((R.drawable.minusicon));
            deleteButton.setOnClickListener(clicks);
            deleteButton.setTag(id);
            l2.addView(deleteButton);

            //adding progress bar with answers
            RelativeLayout r = new RelativeLayout(this);
            LinearLayout.LayoutParams Params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            Params.setMargins(50, 50, 50, 50);
            ques.addView(r, Params);

            float inPixels2 = getResources().getDimension(R.dimen.dimen_progressHeight_in_dp);
            ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);

            progress.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, (int) inPixels2));
            progress.setMinimumHeight((int) inPixels2);
            progress.setProgressDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.progressbar, null));
            progress.setProgress(yes);
            progress.setMax(total);
            progress.setId(R.id.p1);
            r.addView(progress);

            TextView yesAnswers = new TextView(this);
            RelativeLayout.LayoutParams params =
                    new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_LEFT, progress.getId());
            params.addRule(RelativeLayout.CENTER_IN_PARENT);
            yesAnswers.setPadding(15, 0, 0, 0);
            //set number of yes answers
            yesAnswers.setText("YES:"+yes);
            r.addView(yesAnswers, params);

            TextView noAnswers = new TextView(this);
            RelativeLayout.LayoutParams params1 =
                    new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params1.addRule(RelativeLayout.ALIGN_RIGHT, progress.getId());
            params1.addRule(RelativeLayout.CENTER_IN_PARENT);
            //set number of no answers
            noAnswers.setText("NO:"+no);
            noAnswers.setPadding(0, 0, 20, 0);
            r.addView(noAnswers, params1);
        }

    }

    private void zoomOutanimation(View v) {
        ScaleAnimation zoomOut = new ScaleAnimation(1f, 0f, 1, 0f, Animation.RELATIVE_TO_SELF, (float) 0.5, Animation.RELATIVE_TO_SELF, (float) 0.5);
        zoomOut.setDuration(500);
        zoomOut.setFillAfter(true);
        parentView = (ViewGroup) v.getParent().getParent().getParent();
        parentView.startAnimation(zoomOut);
        zoomOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                parentView.removeAllViews();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    //delete button click
    View.OnClickListener clicks = new View.OnClickListener() {

        @Override
        public void onClick(final View v) {
            if(NetUtils.isConnected(getApplicationContext())){
            //remove view and delete from database.
            v.setEnabled(false);
            int tag = (int) v.getTag();
            surveyManager.deleteMyOwnQuestion(tag);
            zoomOutanimation(v);
             Snackbar.make(findViewById(R.id.drawer_layout), getResources().getString(R.string.question_deleted), Snackbar.LENGTH_LONG).show();
            }
            else
                restartActivity();
        }

    };

    //get day before 1week
    private String getDateBefore1Week() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        Date newDate = calendar.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMANY);
        return sdf.format(calendar.getTime());

    }

    //get all views by IDs
    private void findViewsById() {
        mainResponseLayout = (LinearLayout) findViewById(R.id.mainRes);
        aSpinner1 = (Spinner) findViewById(R.id.spinner);
        submitSurveyButton = (Button) findViewById(R.id.submitSurveyButton);
        selectTv = (TextView) findViewById(R.id.selectTv);
        questionsLayout = (LinearLayout) findViewById(R.id.questionsEts);

    }

    //Departments spinner.
    private void setUpSelectTargets() {

        // fetch faulties from DB
        Cursor cursor = surveyManager.getAllFaculties();
        if (fetchedFaculties.isEmpty() && cursor.moveToFirst()) {
            do {
                fetchedFaculties.add(cursor.getString(cursor.getColumnIndex("name")));
            } while (cursor.moveToNext());

        }

        final String[] faculties = fetchedFaculties.toArray(new String[fetchedFaculties.size()]);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.quiz_target_faculty));

        facultiesButton = (Button) findViewById(R.id.button_faculties);
        facultiesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final AlertDialog dialog;

                builder.setMultiChoiceItems(faculties, checked, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                        //reserve the checked faculties when closing spinner
                        if (b && !selectedFaculties.contains(faculties[i])) {
                            selectedFaculties.add(faculties[i]);
                            checked[i] = true;
                        } else {
                            selectedFaculties.remove(faculties[i]);
                            checked[i] = false;
                        }
                    }
                }).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {//if ok show question EditText

                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        selectedFaculties.clear();
                        for (int y = 0; y < checked.length; y++) {
                            checked[y] = false;
                        }
                    }
                });

                dialog = builder.create();
                dialog.show();

            }
        });
    }

    //get survey data (number of questions,departments,questions)
    private boolean checkSurveyData() {
        int numberOfQuestion = aSpinner1.getSelectedItemPosition() + 1;
        boolean done = true;
        for (int i = 0; i < numberOfQuestion; i++) {
            EditText v = (EditText) questionsLayout.findViewWithTag("question" + (i + 1));
            if (hasQuestion(v)) {
                questions.add(v.getText().toString());
            } else {
                done = false;
                questions.clear();
                break;
            }
        }

        if (done) {
            newDate = getDateTime();
            chosenFaculties = getSelectedFaculties(selectedFaculties);
            return true;
        } else
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.complete_question_form), Toast.LENGTH_SHORT).show();

        return false;
    }

    //clear data
    private void clearData() {
        selectedFaculties.clear();
        chosenFaculties = "";
        questions.clear();
        newDate = "";
        for (int i = 0; i < checked.length; i++) {
            checked[i] = false;
        }
        aSpinner1.setSelection(0);
        questionsLayout.removeAllViews();
    }

    //get selected faculties from spinner
    public String getSelectedFaculties(ArrayList<String> arrayList) {
        String facs = "";
        for (int i = 0; i < arrayList.size(); i++) {
            if (i < arrayList.size() - 1) {
                facs += arrayList.get(i) + ',';
            } else {
                facs += arrayList.get(i);
            }
        }

        return facs;
    }

    //submit survey listener --> Still have to handle if there is no internet
    private void submitAndTabListeners() {
        submitSurveyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!checkSurveyData()) {
                    return;
                }

                final ArrayList<String> selectedFacIds = new ArrayList<String>();

                // Get selected faculties to be sent with the question to the server
                if (!selectedFaculties.isEmpty()) {
                    for (int j = 0; j < selectedFaculties.size(); j++) {
                        for (int x = 0; x < fetchedFaculties.size(); x++) {
                            if (selectedFaculties.get(j).equals(fetchedFaculties.get(x))) {
                                Cursor cursor = surveyManager.getFacultyID(selectedFaculties.get(j));
                                if (cursor.moveToFirst()) {
                                    selectedFacIds.add(cursor.getString(cursor.getColumnIndex("faculty")));
                                }
                            }
                        }
                    }
                } else { // no faculties selected -> all faculites.
                    Cursor c = surveyManager.getAllFaculties();
                    if (c.moveToFirst()) {
                        do {
                            selectedFacIds.add(c.getString(c.getColumnIndex("faculty")));
                        } while (c.moveToNext());
                    }
                }


                if (NetUtils.isConnected(getApplication())) {
                    for (int i = 0; i < aSpinner1.getSelectedItemPosition() + 1; i++) {
                        Question ques = new Question(questions.get(i), selectedFacIds);

                        // Submit Question to the survey
                        try {
                            TUMCabeClient.getInstance(getApplicationContext()).createQuestion(ques, new Callback<Question>() {
                                @Override
                                public void success(Question question, Response response) {
                                    Utils.log("Succeeded: " + response.getBody().toString());
                                    numberOfQuestion++;
                                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.survey_submitted), Toast.LENGTH_SHORT).show();

                                }

                                @Override
                                public void failure(RetrofitError error) {
                                    Utils.log("Failure: " + error.toString());
                                }
                            });
                        } catch (Exception e) {
                            Utils.log(e.toString());
                        }
                    }
                    clearData();

                    // gets newly created questions, in order to show them directly in responses
                    new AsyncTask<Void, Void, Void>() {

                        @Override
                        protected Void doInBackground(Void... voids) {
                            surveyManager.downLoadOwnQuestions();
                            return null;
                        }
                        @Override
                        protected void onPostExecute(Void v)
                        { restartActivity();}}.execute();
                }else
                    restartActivity();
            }
        });

        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String s) {
                int currentTab = tabHost.getCurrentTab();
                Utils.log("Current tab: " + currentTab);
                if (currentTab == 0) {
                    mainResponseLayout.removeAllViews();
                } else {
                    if (NetUtils.isConnected(getApplication())) {
                        // gets newly created questions, in order to show them directly in responses
                        new AsyncTask<Void, Void, Void>() {

                            @Override
                            protected Void doInBackground(Void... voids) {
                                surveyManager.downLoadOwnQuestions();
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Void v) {
                                setUpResponseTab();
                            }
                        }.execute();

                    } else {
                        // Without newly created questions
                        setUpResponseTab();
                    }
                }
            }
        });
    }

    private void restartActivity()
    {   Intent i = getIntent();
        i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        finish();
        startActivity(i);}

    private String getDateTime() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMANY);
        return sdf.format(c.getTime());
    }


    //setup tab host (survey,Responses)
    private void setUpTabHost() {
        tabHost = (TabHost) findViewById(R.id.tabHost);
        tabHost.setup();

        // First Tab
        TabHost.TabSpec tabSpec = tabHost.newTabSpec(getResources().getString(R.string.tab_survey));
        tabSpec.setContent(R.id.tabAskQuestion);
        tabSpec.setIndicator(getResources().getString(R.string.tab_survey));
        tabHost.addTab(tabSpec);

        // Second Tab
        tabSpec = tabHost.newTabSpec(getResources().getString(R.string.tab_responses));
        tabSpec.setContent(R.id.tabSeeResponses);
        tabSpec.setIndicator(getResources().getString(R.string.tab_responses));
        tabHost.addTab(tabSpec);

    }

    //check if edittext is empty
    private boolean hasQuestion(EditText et) {
        return !et.getText().toString().isEmpty();
    }

    //check if user is allowed to submit survey depending on the last survey date and number of question he submitted before
    @SuppressLint("SetTextI18n")
    private void userAllowed() {

        String weekAgo = getDateBefore1Week();
        Utils.log("userAllowed- WeekAgo: " + weekAgo);
        //Cursor c = db.rawQuery("SELECT COUNT(*) FROM survey1 WHERE date >= '"+weekAgo+"'", null);
        Cursor c = surveyManager.numberOfQuestionsFrom(weekAgo);
        if (c.getCount() > 0) {
            c.moveToFirst();
        }
        int x = c.getInt(0);
        if (x < 3) {
            numQues = new String[3 - x];
            for (int i = 0; i < numQues.length; i++) {
                numQues[i] = String.valueOf(i + 1);
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, numQues);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            aSpinner1.setAdapter(adapter);
            if (x == 2) {
                selectTv.setText(getResources().getString(R.string.one_question_left));
            }
        } else {
            String strDate = getNextPossibleDate();
            selectTv.setVisibility(View.VISIBLE);
            selectTv.setText(getResources().getString(R.string.next_possible_survey_date) + " " + strDate);
            submitSurveyButton.setVisibility(View.GONE);
            questionsLayout.setVisibility(View.GONE);
            facultiesButton.setVisibility(View.GONE);
            aSpinner1.setVisibility(View.GONE);

        }
    }

    //spinner for number of questions selection
    private void setUpSpinner() {
        numQues[0] = "1";
        numQues[1] = "2";
        numQues[2] = "3";

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, numQues);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        aSpinner1.setAdapter(adapter);

        aSpinner1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                int numberOfQuestions = adapterView.getSelectedItemPosition();
                questionsLayout.removeAllViews();

                for (int y = 0; y <= numberOfQuestions; y++) {
                    EditText questionEt = new EditText(getApplicationContext());
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    questionEt.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.color_primary));
                    questionEt.setInputType(EditorInfo.TYPE_CLASS_TEXT);
                    params.setMargins(0, 30, 0, 0);
                    questionEt.setFocusable(true);
                    questionEt.setLayoutParams(params);
                    questionEt.setHint(getResources().getString(R.string.enter_question_survey) + " " + (y + 1));
                    questionEt.setTag("question" + (y + 1));
                    questionsLayout.addView(questionEt);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    //show the user the next possible survey date.
    private String getNextPossibleDate() {
        String nextPossibleDate = "";
        ArrayList<String> dates = new ArrayList<String>();
        String weekAgo = getDateBefore1Week();
        Cursor c = surveyManager.lastDateFromLastWeek(weekAgo);

        while (c.moveToNext()) {
            dates.add(c.getString(0));
            nextPossibleDate = c.getString(0);
        }

        for (int i = 0; i < dates.size(); i++) {
            for (int z = 0; z < nextPossibleDate.length(); z++) {
                if (dates.get(i).charAt(z) < nextPossibleDate.charAt(z)) {
                    nextPossibleDate = dates.get(i);
                }
            }
        }

        String dtStart = nextPossibleDate;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMANY);
        try {
            Date date = format.parse(dtStart);
            Calendar a = Calendar.getInstance();
            a.setTime(date);
            a.add(Calendar.DAY_OF_MONTH, +7);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMANY);
            nextPossibleDate = sdf.format(a.getTime());
        } catch (ParseException e) {
            Utils.log("getNextPossibleDate: " + e.toString());
        }

        return nextPossibleDate;
    }


}