package de.tum.in.tumcampus.cards;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.view.ViewGroup;

import de.tum.in.tumcampus.R;
import de.tum.in.tumcampus.activities.TuitionFeesActivity;
import de.tum.in.tumcampus.models.Tuition;
import de.tum.in.tumcampus.models.managers.CardManager;


public class TuitionFeesCard extends Card {

    private static final String LAST_FEE_FRIST = "fee_frist";
    private static final String LAST_FEE_SOLL = "fee_soll";
    private Tuition mTuition;

    public TuitionFeesCard(Context context) {
        super(context, "card_tuition_fee_setting");
    }

    @Override
    public int getTyp() {
        return CardManager.CARD_TUITION_FEE;
    }

    @Override
    public String getTitle() {
        return mContext.getString(R.string.tuition_fees);
    }

    @Override
    public View getCardView(Context context, ViewGroup parent) {
        super.getCardView(context, parent);

        if (mTuition.getSoll().equals("0")) {
            addTextView(String.format(mContext.getString(R.string.reregister_success), mTuition.getSemesterBez()));
        } else {
            addTextView(mTuition.getSoll() + "€");
            addTextView(String.format(mContext.getString(R.string.reregister_todo), mTuition.getFrist()));
        }

        return mCard;
    }

    @Override
    public void discard(Editor editor) {
        editor.putString(LAST_FEE_FRIST, mTuition.getFrist());
        editor.putString(LAST_FEE_SOLL, mTuition.getSoll());
    }

    @Override
    public boolean shouldShow(SharedPreferences prefs) { //TODO: Rethink
        String prevFrist = prefs.getString(LAST_FEE_FRIST, "");
        String prevSoll = prefs.getString(LAST_FEE_SOLL, mTuition.getSoll());
        return prevFrist.compareTo(mTuition.getFrist()) < 0 || prevSoll.compareTo(mTuition.getSoll()) > 0;
    }

    @Override
    protected Notification fillNotification(NotificationCompat.Builder notificationBuilder) {
        if (mTuition.getSoll().equals("0")) {
            notificationBuilder.setContentText(String.format(mContext.getString(R.string.reregister_success), mTuition.getSemesterBez()));
        } else {
            notificationBuilder.setContentText(mTuition.getSoll() + "€\n" + String.format(mContext.getString(R.string.reregister_todo), mTuition.getFrist()));
        }
        return notificationBuilder.build();
    }

    @Override
    public Intent getIntent() {
        return new Intent(mContext, TuitionFeesActivity.class);
    }

    public void setTuition(Tuition tuition) {
        mTuition = tuition;
    }
}
