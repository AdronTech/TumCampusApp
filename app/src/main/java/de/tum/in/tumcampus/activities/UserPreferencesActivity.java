package de.tum.in.tumcampus.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;

import de.tum.in.tumcampus.R;
import de.tum.in.tumcampus.activities.generic.BaseActivity;
import de.tum.in.tumcampus.auxiliary.Const;
import de.tum.in.tumcampus.auxiliary.ImplicitCounter;
import de.tum.in.tumcampus.fragments.SettingsFragment;

/**
 * Provides the preferences, encapsulated into an own activity.
 */
public class UserPreferencesActivity extends BaseActivity  implements
        PreferenceFragmentCompat.OnPreferenceStartScreenCallback{

    public UserPreferencesActivity() {
        super(R.layout.activity_user_preferences);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ImplicitCounter.Counter(this);

        //Enable the direct access of a specific sub section, e.g.: cards
        Intent intent = getIntent();
        Bundle args = new Bundle();
        if (intent != null && intent.getExtras() != null && intent.getExtras().containsKey(Const.PREFERENCE_SCREEN)) {
            args.putString(Const.PREFERENCE_SCREEN, intent.getExtras().getString(Const.PREFERENCE_SCREEN));
        }

        if (savedInstanceState == null) {
            // Create the fragment only when the activity is created for the first time. ie. not after orientation changes
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(SettingsFragment.FRAGMENT_TAG);
            if (fragment == null) {
                fragment = new SettingsFragment();
            }
            fragment.setArguments(args);
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.settings_frame, fragment, SettingsFragment.FRAGMENT_TAG);
            ft.commit();
        }
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragmentCompat preferenceFragmentCompat, PreferenceScreen preferenceScreen) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, preferenceScreen.getKey());
        fragment.setArguments(args);

        ft.add(R.id.settings_frame, fragment, preferenceScreen.getKey());
        ft.addToBackStack(preferenceScreen.getKey());
        ft.commit();
        return true;
    }

}