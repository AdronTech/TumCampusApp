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
import android.os.AsyncTask;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import de.tum.in.tumcampusapp.R;
import de.tum.in.tumcampusapp.activities.generic.ProgressActivity;
import de.tum.in.tumcampusapp.auxiliary.NetUtils;
import de.tum.in.tumcampusapp.auxiliary.Utils;
import de.tum.in.tumcampusapp.models.Question;
import de.tum.in.tumcampusapp.models.TUMCabeClient;
import de.tum.in.tumcampusapp.models.managers.SurveyManager;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * SurveyActivity for handling submitting ownQuesitons and reviewing responses
 */
public class SurveyActivity extends ProgressActivity {

    private Spinner aSpinner1;
    private TextView selectTv;
    private TabHost tabHost;
    private Button submitSurveyButton, facultiesButton;
    private ArrayList<String> questions = new ArrayList<>();
    private ArrayList<String> selectedFaculties = new ArrayList<>();
    private boolean[] checked = new boolean[14];
    private LinearLayout mainResponseLayout, questionsLayout;
    String chosenFaculties = "";
    private ArrayList<String> fetchedFaculties = new ArrayList<>();
    private ViewGroup parentView;
    private String[] numQues = new String[3];
    private SurveyManager surveyManager;

    // for handling change in internet connectivity. If initially had no connection, then connected, then restart activity
    BroadcastReceiver connectivityChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (NetUtils.isConnected(getApplicationContext())) {
                restartActivity();
                unregisterReceiver(connectivityChangeReceiver);
            }
        }
    };

    public SurveyActivity() {
        super(R.layout.activity_survey);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        surveyManager = new SurveyManager(this);
        super.onCreate(savedInstanceState);
        registerReceiver(connectivityChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        if (NetUtils.isConnected(this)) { // Opening activity is possible with internet connection
            findViewsById();
            setUpTabHost();
            setUpSpinnerForQuestionsNumber();
            setUpSelectedTargetFacultiesSpinner();
            submitAndHostTabListener();
            numberOfQuestionsUserAllowed();
            unregisterReceiver(connectivityChangeReceiver);
        } else {
            setContentView(R.layout.layout_no_internet);
        }

    }

    @Override
    public void onRefresh() {
    }

    //set up the respone tab layout dynamically depending on number of questions
    @SuppressLint("SetTextI18n")
    private void setUpResponseTab() {
        Cursor c = surveyManager.getMyRelevantOwnQuestionsSince(Utils.getDateTimeString(new Date()));
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
            //add Yes asnwers inside progressbar
            TextView yesAnswers = new TextView(this);
            RelativeLayout.LayoutParams params =
                    new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_LEFT, progress.getId());
            params.addRule(RelativeLayout.CENTER_IN_PARENT);
            yesAnswers.setPadding(15, 0, 0, 0);
            //set number of yes answers
            yesAnswers.setText("YES:" + yes);
            r.addView(yesAnswers, params);
            //add No asnwers inside progressbar
            TextView noAnswers = new TextView(this);
            RelativeLayout.LayoutParams params1 =
                    new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params1.addRule(RelativeLayout.ALIGN_RIGHT, progress.getId());
            params1.addRule(RelativeLayout.CENTER_IN_PARENT);
            //set number of no answers
            noAnswers.setText("NO:" + no);
            noAnswers.setPadding(0, 0, 20, 0);
            r.addView(noAnswers, params1);
        }

    }

    //Handels clicking on 'delete' button of an own question in responses tab
    View.OnClickListener clicks = new View.OnClickListener() {

        @Override
        public void onClick(final View v) {
            if (NetUtils.isConnected(getApplicationContext())) {
                //remove view and delete from database.
                v.setEnabled(false);
                int tag = (int) v.getTag();
                surveyManager.deleteMyOwnQuestion(tag);
                zoomOutanimation(v); // provides a smoth delete animation of the question
                Snackbar.make(findViewById(R.id.drawer_layout), getResources().getString(R.string.question_deleted), Snackbar.LENGTH_LONG).show();
            } else {
                restartActivity();
            }
        }

    };

    /**
     * provides a smoth delete animation of the question
     * @param v: view to be deleted, where an ownQuestion with respective responses gets deleted
     */
    private void zoomOutanimation(View v) {
        ScaleAnimation zoomOut = new ScaleAnimation(1f, 0f, 1, 0f, Animation.RELATIVE_TO_SELF, (float) 0.5, Animation.RELATIVE_TO_SELF, (float) 0.5);
        zoomOut.setDuration(500);
        zoomOut.setFillAfter(true);
        parentView = (ViewGroup) v.getParent().getParent().getParent();
        parentView.startAnimation(zoomOut);
        zoomOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                parentView.removeAllViews();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }



    /**
     * For restarting SurveyAcitivity. Used upon reconnecting to internet after no connection, or when submitting question(s)
     */
    private void restartActivity() {
        Intent i = getIntent();
        i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        finish();
        startActivity(i);
    }

    /**
     * get all views by IDs
     */
    private void findViewsById() {
        mainResponseLayout = (LinearLayout) findViewById(R.id.mainRes);
        aSpinner1 = (Spinner) findViewById(R.id.spinner);
        submitSurveyButton = (Button) findViewById(R.id.submitSurveyButton);
        selectTv = (TextView) findViewById(R.id.selectTv);
        questionsLayout = (LinearLayout) findViewById(R.id.questionsEts);
    }

    /**
     * Sets up the spinner for selecting target faculties and handles choosing them
     */
    private void setUpSelectedTargetFacultiesSpinner() {

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

        // Handls reserving chosen faculties in sponner
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
                    //if Ok do nothing-> keep selecte faculties
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    //if cancel clear selected faculties
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

    /**
     * Handles submitting questions and changing hosttabs
     */
    private void submitAndHostTabListener() {

        // Upon submitting question(s)
        submitSurveyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!checkSurveyData()) { // plausibility check
                    return;
                }

                // facultyIds to be sent to server upon submitting question(s)
                final ArrayList<String> selectedFacIds = new ArrayList<>();


                if (!selectedFaculties.isEmpty()) { // In case at least one faculty is selected
                    // Adds the ids of selected faculties by match selected faculty names with fetched faculties names
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
                } else { // if no faculty is selected, add faculties as target upon submitting question(s).
                    Cursor c = surveyManager.getAllFaculties();
                    if (c.moveToFirst()) {
                        do {
                            selectedFacIds.add(c.getString(c.getColumnIndex("faculty")));
                        } while (c.moveToNext());
                    }
                }

                //if connected to internet submit questions
                if (NetUtils.isConnected(getApplication())) {
                    /**
                     * 1. onPreExecute: submit the questions to the server
                     * 2. doInBackGround: download the questions we just submitted to server, in order to show them directly in
                     * responses tab in case the user changes tabs or to check if the user can still enter further questions this week
                     * 3. onPostExecute: finish activity, cleardata(clear all layout entries) and restart activity (userallowed() gets called and it will be checked whether the user can enter further questions.
                     */
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected void onPreExecute() {
                            for (int i = 0; i < aSpinner1.getSelectedItemPosition() + 1; i++) {
                                Question ques = new Question(questions.get(i), selectedFacIds);
                                // Submit Question to the server
                                try {
                                    TUMCabeClient.getInstance(getApplicationContext()).createQuestion(ques, new Callback<Question>() {
                                        @Override
                                        public void success(Question question, Response response) {
                                            Snackbar.make(findViewById(R.id.drawer_layout), getResources().getString(R.string.survey_submitted), Snackbar.LENGTH_LONG).show();
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
                        }

                        @Override
                        protected Void doInBackground(Void... voids) {
                            try {
                                Thread.sleep(1000); // Waits to make sure that the questions got sent to the server in order to avoid fetching ownQuestions without the newly created questions
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            surveyManager.downLoadOwnQuestions();
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void v) {
                            finish();
                            clearData(); // clear layout entries
                            restartActivity(); // restart activity where it will be checked if the user can create further questions in this week (in numberOfQuestionsUserAllowed)
                        }
                    }.execute();
                } else { // if not connected, then restartActivity and the broadcastreceiver for no connectivity will show the no internet layout.
                    restartActivity();
                }
            }
        });

        //
        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String s) {
                int currentTab = tabHost.getCurrentTab();
                if (currentTab == 0) { // when  the user is currently in first tab 'survey' the views of response tap shoud be removed in order to avoid any duplication in displaying ownQuestions upon tab change again
                    mainResponseLayout.removeAllViews(); // to avoid
                } else { // in case the user changes to 'responses' tab
                    if (NetUtils.isConnected(getApplication())) {

                        //gets newly created questions, in order to show them directly in responses
                        /**
                         * 1. doInBackground: downloadOwnQuestions so that the user can see uptodate answers on ownQuestions
                         * 2. onPostExecute: setUpResponse tab
                         */
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
                        setUpResponseTab(); // setup responses tab Without possible fresh answeres.
                    }
                }
            }
        });
    }


    /**
     * Help function for plausibility check of the question form
     *
     * @return true if everything was entered correctly else snackbar for requesting to complete questions.
     */
    private boolean checkSurveyData() {
        boolean done = true;
        for (int i = 0; i < aSpinner1.getSelectedItemPosition() + 1; i++) { // Iterates on each questionEditText
            EditText v = (EditText) questionsLayout.findViewWithTag("question" + (i + 1));
            if (!v.getText().toString().isEmpty()) { // if textfield is not empty, add to questions
                questions.add(v.getText().toString());
            } else { // else plausibility check failed
                done = false;
                questions.clear();
                break;
            }
        }
        if (done) { // if plausibility passed, then save selected faculties as they will be needed upon submitting question
            chosenFaculties = getSelectedFaculties(selectedFaculties);
            return true;
        } else { // else notify the user with a snackbar to complete the question form
            Snackbar.make(findViewById(R.id.drawer_layout), getResources().getString(R.string.complete_question_form), Snackbar.LENGTH_LONG).show();
        }
        return false;
    }

    /**
     * Help function for clearing data and layout entries after submitting questions
     */
    private void clearData() {
        selectedFaculties.clear();
        chosenFaculties = "";
        questions.clear();
        for (int i = 0; i < checked.length; i++) { // uncheck selected faculties
            checked[i] = false;
        }
        aSpinner1.setSelection(0); //
    }

    //get selected faculties from spinner

    /**
     * Help function for getting all faculties as a string from the spinner
     * @param arrayList: selected faculties in spinner as arraylist
     * @return: selected faculties as String
     */
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


    /**
     * Sets up tabhost for submitting questions and reviewing responses
     */
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


    /**
     * check if user is allowed to submit survey depending on the last survey date and number of question he submitted in the last week
     */
    @SuppressLint("SetTextI18n")
    private void numberOfQuestionsUserAllowed() {

        String weekAgo = getDateBefore1Week();
        Cursor c = surveyManager.ownQuestionsSince(weekAgo); // gets number of questions submitted during last week
        if (c.getCount() > 0) {
            c.moveToFirst();
        }
        int x = c.getCount();
        if (x < 3) { // if below 3, then set the spinner with the numbers of questions user allowed to ask respectively
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
        } else { // else notify user he reached the max. number of questions this week and show him the next possible date for entering questions
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
    private void setUpSpinnerForQuestionsNumber() {
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

    /**
     * Help function for get Date before 1 week to check if user allowed to submit survey
     * @return return this date as a string
     */
    private String getDateBefore1Week() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMANY);
        return sdf.format(calendar.getTime());

    }

    /**
     * Get the next possible date for the user to enter survey questions
     * @return this date as a string
     */
    private String getNextPossibleDate() {
        String nextPossibleDate = "";
        ArrayList<String> dates = new ArrayList<String>();
        String weekAgo = getDateBefore1Week();
        Cursor c = surveyManager.ownQuestionsSince(weekAgo);

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