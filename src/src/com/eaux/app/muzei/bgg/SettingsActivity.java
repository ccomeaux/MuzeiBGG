package com.eaux.app.muzei.bgg;

import java.util.HashMap;
import java.util.List;

import android.app.ActionBar;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

public class SettingsActivity extends PreferenceActivity {
	public final static String PREFIX = "com.eaux.app.muzei.bgg.settings.";
	private final static String ACTION_UPDATES = PREFIX + "UPDATES";
	private final static String ACTION_ABOUT = PREFIX + "ABOUT";
	private static final HashMap<String, Integer> mFragmentMap = buildFragmentMap();

	private static HashMap<String, Integer> buildFragmentMap() {
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		map.put(ACTION_UPDATES, R.xml.preference_updates);
		map.put(ACTION_ABOUT, R.xml.preference_about);
		return map;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ActionBar actionBar = getActionBar();
		actionBar.setIcon(null);
		actionBar.setDisplayUseLogoEnabled(false);
		actionBar.setDisplayShowTitleEnabled(true);
	}

	@Override
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.preference_headers, target);
	}

	public static class SettingsFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			String fragment = getArguments().getString("fragment");
			if (fragment != null) {
				Integer fragmentId = mFragmentMap.get(fragment);
				if (fragmentId != null) {
					addPreferencesFromResource(fragmentId);
				}
			}

			final ListPreference listPreference = (ListPreference) findPreference(getString(R.string.settings_key_update_freq));
			if (listPreference != null) {
				listPreference.setSummary(listPreference.getEntry());
				listPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference, Object newValue) {
						listPreference.setValue(newValue.toString());
						preference.setSummary(listPreference.getEntry());
						return true;
					}
				});
			}
		}
	}

	@Override
	protected boolean isValidFragment(String fragmentName) {
		return "com.eaux.app.muzei.bgg.SettingsActivity$SettingsFragment".equals(fragmentName);
	}
}