package de.tum.in.tumcampus.activities;

import android.os.Bundle;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

import de.tum.in.tumcampus.R;
import de.tum.in.tumcampus.activities.generic.ActivityForAccessingTumOnline;
import de.tum.in.tumcampus.auxiliary.Utils;
import de.tum.in.tumcampus.models.TuitionList;
import de.tum.in.tumcampus.tumonline.TUMOnlineConst;

/**
 * Activity to show the user's tuition ; based on grades.java / quick solution
 */
public class TuitionFeesActivity extends ActivityForAccessingTumOnline<TuitionList> {

    private TextView amountTextView;
    private TextView deadlineTextView;
    private TextView semesterTextView;

    public TuitionFeesActivity() {
        super(TUMOnlineConst.TUITION_FEE_STATUS, R.layout.activity_tuitionfees);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        amountTextView = (TextView) findViewById(R.id.soll);
        deadlineTextView = (TextView) findViewById(R.id.frist);
        semesterTextView = (TextView) findViewById(R.id.semester);

        requestFetch();
    }

    /**
     * Handle the response by de-serializing it into model entities.
     *
     * @param tuitionList TUMOnline response
     */
    @Override
    public void onFetch(TuitionList tuitionList) {
        amountTextView.setText(tuitionList.getTuitions().get(0).getSoll() + "€");
        Date date = Utils.getDate(tuitionList.getTuitions().get(0).getFrist());
        deadlineTextView.setText(SimpleDateFormat.getDateInstance().format(date));
        semesterTextView.setText(tuitionList.getTuitions().get(0).getSemesterBez().toUpperCase());

        showLoadingEnded();
    }
}
