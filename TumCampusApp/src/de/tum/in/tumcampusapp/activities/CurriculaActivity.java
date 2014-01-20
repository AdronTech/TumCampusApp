package de.tum.in.tumcampusapp.activities;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Vector;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import de.tum.in.tumcampus.R;

/**
 * Activity to fetch and display the curricula of different study programs.
 * 
 * @author Vincenz Doelle
 * @review Daniel G. Mayr
 * @review Thomas Behrens
 * @review Sascha Moecker
 */
public class CurriculaActivity extends Activity implements OnItemClickListener {

	public static final String NAME = "name";

	public static final String URL = "url";
	private SharedPreferences sharedPrefs;
	Hashtable<String, String> options;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (sharedPrefs.getBoolean("implicitly_id", true)==true){
		Counter();
		}

		setContentView(R.layout.activity_curricula);

		ListView list = (ListView) findViewById(R.id.activity_curricula_list_view);

		// Puts all hardcoded web addresses into the hash map
		options = new Hashtable<String, String>();
		options.put(
				getString(R.string.informatics_bachelor),
				"http://www.in.tum.de/fuer-studierende-der-tum/bachelor-studiengaenge/informatik/studienplan/studienbeginn-ab-ws-20072008.html");
		options.put(
				getString(R.string.business_informatics_bachelor_0809),
				"http://www.in.tum.de/fuer-studierende-der-tum/bachelor-studiengaenge/wirtschaftsinformatik/studienplan/studienbeginn-ab-ws-20112012.html");
		options.put(
				getString(R.string.business_informatics_bachelor_1112),
				"http://www.in.tum.de/fuer-studierende-der-tum/bachelor-studiengaenge/wirtschaftsinformatik/studienplan/studienbeginn-ab-ws-20082009.html");
		options.put(
				getString(R.string.bioinformatics_bachelor),
				"http://www.in.tum.de/fuer-studierende-der-tum/bachelor-studiengaenge/bioinformatik/studienplan/ws-20072008.html");
		options.put(
				getString(R.string.games_engineering_bachelor),
				"http://www.in.tum.de/fuer-studierende-der-tum/bachelor-studiengaenge/informatik-games-engineering/studienplan-games.html");
		options.put(
				getString(R.string.informatics_master),
				"http://www.in.tum.de/fuer-studierende-der-tum/master-studiengaenge/informatik/studienplan/studienplan-fpo-2007.html");
		options.put(
				getString(R.string.business_informatics_master),
				"http://www.in.tum.de/fuer-studierende-der-tum/master-studiengaenge/wirtschaftsinformatik/studienplan");

		options.put(
				getString(R.string.bioinformatics_master),
				"http://www.in.tum.de/fuer-studierende-der-tum/master-studiengaenge/bioinformatik/studienplan/ws-20072008.html");
		options.put(
				getString(R.string.automotive_master),
				"http://www.in.tum.de/fuer-studierende-der-tum/master-studiengaenge/automotive-software-engineering/studienplanung.html");
		options.put(
				getString(R.string.computational_science_master),
				"http://www.in.tum.de/fuer-studieninteressierte/master-studiengaenge/computational-science-and-engineering/course/course-plan.html");

		// Sort curricula options and attach them to the list
		Vector<String> sortedOptions = new Vector<String>(options.keySet());
		Collections.sort(sortedOptions);
		String[] optionsArray = sortedOptions.toArray(new String[0]);

		// Sets the adapter with list items
		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, optionsArray);
		list = (ListView) findViewById(R.id.activity_curricula_list_view);
		list.setAdapter(arrayAdapter);
		list.setOnItemClickListener(this);
	}
	public void Counter()
	{
		//Counting number of the times that the user used this activity.
				SharedPreferences sp = getSharedPreferences(getString(R.string.MyPrefrences), Activity.MODE_PRIVATE);
				int myvalue = sp.getInt("study_plans_id",0);
				myvalue=myvalue+1;
				SharedPreferences.Editor editor = sp.edit();
				editor.putInt("study_plans_id",myvalue);
				editor.commit();
				////

				 int myIntValue = sp.getInt("study_plans_id",0);
				 if(myIntValue==5){
						sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
						SharedPreferences.Editor editor1 = sharedPrefs.edit();
						editor1.putBoolean("study_plans_id", true);
						editor1.commit();
						editor.putInt("study_plans_id",0);
						editor.commit();
					 
				 Toast.makeText(this, String.valueOf(myIntValue),
							Toast.LENGTH_LONG).show();
				 }
				 //////////
		
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
		String curriculumName = ((TextView) view).getText().toString();

		// Puts URL and name into an intent and starts the detail view
		Intent intent = new Intent(this, CurriculaDetailsActivity.class);
		intent.putExtra(URL, options.get(curriculumName));
		intent.putExtra(NAME, curriculumName);
		startActivity(intent);
	}
}
