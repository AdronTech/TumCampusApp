package de.tum.in.tumcampus.cards;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.tum.in.tumcampus.R;
import de.tum.in.tumcampus.models.managers.CardManager;


public class FirstUseCard2 extends Card {

    public FirstUseCard2(Context context) {
        super(context);
    }

    @Override
    public int getTyp() {
        return CardManager.CARD_FIRST_USE_2;
    }

    @Override
    public View getCardView(Context context, ViewGroup parent) {
        mContext = context;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return mInflater.inflate(R.layout.card_first_use2, parent, false);
    }

    @Override
    public void discard(Editor editor) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        prefs.edit().putBoolean(CardManager.SHOW_TUTORIAL_2, false).commit();
    }

    @Override
    public boolean shouldShow(SharedPreferences p) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        return prefs.getBoolean(CardManager.SHOW_TUTORIAL_2, true);
    }
}
