package com.eaux.app.muzei.bgg;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.simpleframework.xml.core.Persister;

import retrofit.RestAdapter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.eaux.app.muzei.bgg.BggService.HotItem;
import com.eaux.app.muzei.bgg.BggService.HotnessResponse;
import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;
import com.mobprofs.retrofit.converters.SimpleXmlConverter;

public class BggMuzeiArtSource extends RemoteMuzeiArtSource {
	private static final String TAG = "MuzeiBgg";
	private static final String SOURCE_NAME = "BggMuzeiArtSource";

	private static final int DEFAULT_UPDATE_FREQUENCY = 3 * 60 * 60 * 1000; // update every 3 hours
	private static final int INITIAL_RETRY_TIME_MILLIS = 10 * 1000; // start retry every 10 seconds
	private static final String PREF_NO_WIFI_RETRY_ATTEMPT = SettingsActivity.PREFIX + "no_wifi_retry_attempt";
	private Random mRandom;
	private SharedPreferences mPrefs;

	public BggMuzeiArtSource() {
		super(SOURCE_NAME);
		mRandom = new Random();
	}

	@Override
	public void onCreate() {
		super.onCreate();
		setUserCommands(BUILTIN_COMMAND_ID_NEXT_ARTWORK);
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
	}

	@Override
	protected void onTryUpdate(int reason) throws RetryException {
		Log.w(TAG, "onTryUpdate " + reason);
		if (reason == UPDATE_REASON_SCHEDULED || reason == UPDATE_REASON_OTHER) {
			if (mPrefs.getBoolean(getString(R.string.settings_key_wifi_only), false) && !isWifiConnected()) {
				Log.w(TAG, "Not connected to Wi-Fi.");
				scheduleUpdate(System.currentTimeMillis() + getRetryTime());
				return;
			}
		}
		mPrefs.edit().remove(PREF_NO_WIFI_RETRY_ATTEMPT).apply();

		String currentToken = (getCurrentArtwork() != null) ? getCurrentArtwork().getToken() : null;

		BggService service = createService();
		String hotnessType = selectRandomHotnessType();
		HotnessResponse response = service.getHotness(hotnessType);

		if (response == null || response.hotItems == null || response.hotItems.size() == 0) {
			Log.w(TAG, "Invalid response");
			throw new RetryException();
		}

		HotItem hotItem;
		String token;
		while (response.hotItems.size() > 0) {
			hotItem = response.hotItems.remove(mRandom.nextInt(response.hotItems.size()));
			token = Integer.toString(hotItem.id);
			if (hotItem.isValid() && !TextUtils.equals(token, currentToken)) {
				Log.w(TAG, "Publishing token " + token);
				publishArtwork(new Artwork.Builder()
					.title(hotItem.name)
					.byline(
						"#" + hotItem.rank + " "
							+ getString(R.string.byline_suffix_hotness, getHotnessTypeDescription(hotnessType)))
					.imageUri(hotItem.getUri()).token(token).viewIntent(hotItem.getIntent(hotnessType)).build());
				break;
			}
		}

		long updateFreq = getUpdateFrequency();
		if (updateFreq > 0) {
			scheduleUpdate(System.currentTimeMillis() + updateFreq);
		}
	}

	private BggService createService() {
		RestAdapter restAdapter = new RestAdapter.Builder().setEndpoint("http://boardgamegeek.com/")
			.setConverter(new SimpleXmlConverter(new Persister())).build();
		BggService service = restAdapter.create(BggService.class);
		return service;
	}

	private String selectRandomHotnessType() {
		Set<String> hotTypes = mPrefs
			.getStringSet(getString(R.string.settings_key_hotness_type), new HashSet<String>());
		if (hotTypes != null && hotTypes.size() > 0) {
			int index = mRandom.nextInt(hotTypes.size());
			int i = 0;
			for (String hotType : hotTypes) {
				if (i == index) {
					return hotType;
				}
				i++;
			}
		}
		return "boardgame";
	}

	private String getHotnessTypeDescription(String type) {
		String[] entryValues = getResources().getStringArray(R.array.settings_hotness_type_entry_values);
		String[] entries = getResources().getStringArray(R.array.settings_hotness_type_entries);
		for (int i = 0; i < entryValues.length; i++) {
			if (type.equals(entryValues[i])) {
				return entries[i];
			}
		}
		return "";
	}

	private boolean isWifiConnected() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		return ni != null && ni.isConnected();
	}

	private long getRetryTime() {
		int retryAttempt = mPrefs.getInt(PREF_NO_WIFI_RETRY_ATTEMPT, 0);
		Log.w(TAG, "Retry #" + retryAttempt);
		long retryTime = Math.min(INITIAL_RETRY_TIME_MILLIS << retryAttempt, getUpdateFrequency());
		mPrefs.edit().putInt(PREF_NO_WIFI_RETRY_ATTEMPT, retryAttempt + 1).apply();
		return retryTime;
	}

	private long getUpdateFrequency() {
		String updateFreqString = mPrefs.getString(getString(R.string.settings_key_update_freq),
			getString(R.string.settings_update_freq_default));
		long updateFreq = DEFAULT_UPDATE_FREQUENCY;
		try {
			updateFreq = Integer.parseInt(updateFreqString) * 60 * 1000;
		} catch (NumberFormatException e) {
		}
		return updateFreq;
	}
}
