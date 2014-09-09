package de.tum.in.tumcampus.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import de.tum.in.tumcampus.R;
import de.tum.in.tumcampus.activities.generic.ActivityForDownloadingExternal;
import de.tum.in.tumcampus.adapters.CafeteriaDetailsSectionsPagerAdapter;
import de.tum.in.tumcampus.auxiliary.Const;
import de.tum.in.tumcampus.auxiliary.Utils;
import de.tum.in.tumcampus.models.managers.LocationManager;
import de.tum.in.tumcampus.models.Cafeteria;

/**
 * Lists all dishes at given cafeteria
 * 
 * @author Sascha Moecker, Haris Iltifat, Thomas Krex
 * 
 */
public class CafeteriaActivity extends ActivityForDownloadingExternal implements ActionBar.OnNavigationListener {

    private ViewPager mViewPager;
    private int mCafeteriaId = -1;
    private CafeteriaDetailsSectionsPagerAdapter mSectionsPagerAdapter;
    private List<Cafeteria> mCafeterias;

    public CafeteriaActivity() {
        super(Const.CAFETERIAS, R.layout.activity_cafeteria);
    }

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Get Id and name from intent (calling activity)
        final Intent intent = getIntent();
        if(intent!=null && intent.getExtras()!=null
                && intent.getExtras().containsKey(Const.CAFETERIA_ID))
    		mCafeteriaId = intent.getExtras().getInt(Const.CAFETERIA_ID);
        mViewPager = (ViewPager) findViewById(R.id.pager);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_section_fragment_cafeteria_details, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId()==R.id.action_ingredients) {
			// Build a alert dialog containing the mapping of ingredients to the numbers
			new AlertDialog.Builder(this).setTitle(R.string.action_ingredients)
			    .setMessage(menuToSpan(this, getResources().getString(R.string.cafeteria_ingredients)))
                .setPositiveButton(android.R.string.ok, null).create().show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

    @SuppressWarnings("deprecation")
    @Override
    protected void onStart() {
        super.onStart();

        // Get all available cafeterias from database
        mCafeterias = new LocationManager(this).getCafeterias();

        int selIndex = -1;
        for(int i=0;i<mCafeterias.size();i++) {
            Cafeteria c = mCafeterias.get(i);
            if(mCafeteriaId==-1 || mCafeteriaId == c.id) {
                mCafeteriaId = c.id;
                selIndex = i;
                break;
            }
        }

        if (mCafeterias.size() == 0) {
            // If something went wrong or no cafeterias found
            showErrorLayout();
            return;
        }

        // Adapter for drop-down navigation
        ArrayAdapter adapterCafeterias = new ArrayAdapter<Cafeteria>(this, R.layout.simple_spinner_item_actionbar, mCafeterias) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(LAYOUT_INFLATER_SERVICE);

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View v = inflater.inflate(R.layout.simple_spinner_dropdown_item_actionbar, parent, false);
                Cafeteria c = getItem(position);

                // Set name
                TextView name = (TextView) v.findViewById(android.R.id.text1);
                name.setText(c.name);

                // Set address
                TextView address = (TextView) v.findViewById(android.R.id.text2);
                address.setText(c.address);

                // Set distance
                TextView dist = (TextView) v.findViewById(R.id.distance);
                dist.setText(Utils.formatDist(c.distance));

                return v;
            }
        };
        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        getSupportActionBar().setListNavigationCallbacks(adapterCafeterias, this);

        // Select item
        if(selIndex>-1)
            getSupportActionBar().setSelectedNavigationItem(selIndex);
    }

    @Override
    public boolean onNavigationItemSelected(int pos, long id) {
        mCafeteriaId = mCafeterias.get(pos).id;

        // Create the adapter that will return a fragment for each of the primary sections of the app.
        if (mSectionsPagerAdapter == null) {
            mSectionsPagerAdapter = new CafeteriaDetailsSectionsPagerAdapter(getSupportFragmentManager());
            mSectionsPagerAdapter.setCafeteriaId(this, mCafeteriaId);
            mViewPager.setAdapter(mSectionsPagerAdapter);
        } else {
            mSectionsPagerAdapter.setCafeteriaId(this, mCafeteriaId);
        }
        return true;
    }

    public static SpannableString menuToSpan(Context context, String menu) {
        int len;
        do {
            len = menu.length();
            menu = menu.replaceFirst("\\(([A-Za-z0-9]+),", "($1)(");
        } while (menu.length() > len);
        SpannableString text = new SpannableString(menu);
        replaceWithImg(context, menu, text, "(v)", R.drawable.meal_vegan);
        replaceWithImg(context, menu, text, "(f)", R.drawable.meal_veggie);
        replaceWithImg(context, menu, text, "(R)", R.drawable.meal_beef);
        replaceWithImg(context, menu, text, "(S)", R.drawable.meal_pork);
        replaceWithImg(context, menu, text, "(GQB)", R.drawable.ic_gqb);
        replaceWithImg(context, menu, text, "(99)", R.drawable.meal_alcohol);
        return text;
    }

    private static void replaceWithImg(Context context, String menu, SpannableString text, String sym, int drawable) {
        int ind = menu.indexOf(sym);
        while (ind >= 0) {
            ImageSpan is = new ImageSpan(context, drawable);
            text.setSpan(is, ind, ind + sym.length(), 0);
            ind = menu.indexOf(sym, ind + sym.length());
        }
    }
}
