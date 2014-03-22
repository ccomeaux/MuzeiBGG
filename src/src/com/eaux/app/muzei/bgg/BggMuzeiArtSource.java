package com.eaux.app.muzei.bgg;

import java.util.Random;

import org.simpleframework.xml.core.Persister;

import retrofit.RestAdapter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.eaux.app.muzei.bgg.BggService.HotGame;
import com.eaux.app.muzei.bgg.BggService.HotGamesResponse;
import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;
import com.mobprofs.retrofit.converters.SimpleXmlConverter;

public class BggMuzeiArtSource extends RemoteMuzeiArtSource {
	private static final String TAG = "MuzeiBgg";
	private static final String SOURCE_NAME = "BggMuzeiArtSource";

	private static final int ROTATE_TIME_MILLIS = 3 * 60 * 60 * 1000; // rotate every 3 hours
	private static final int INITIAL_RETRY_TIME_MILLIS = 10 * 1000; // start retry every 10 seconds
	private static final String PREF_NO_WIFI_RETRY_ATTEMPT = "no_wifi_retry_attempt";

	public BggMuzeiArtSource() {
		super(SOURCE_NAME);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		setUserCommands(BUILTIN_COMMAND_ID_NEXT_ARTWORK);
	}

	@Override
	protected void onTryUpdate(int reason) throws RetryException {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (reason == UPDATE_REASON_SCHEDULED || reason == UPDATE_REASON_OTHER) {
			if (prefs.getBoolean(getString(R.string.settings_key_wifi_only), false) && !isWifiConnected()) {
				Log.w(TAG, "Not connected to Wi-Fi.");
				int retryAttempt = prefs.getInt(PREF_NO_WIFI_RETRY_ATTEMPT, 0);
				scheduleUpdate(System.currentTimeMillis() + (INITIAL_RETRY_TIME_MILLIS << retryAttempt));
				prefs.edit().putInt(PREF_NO_WIFI_RETRY_ATTEMPT, retryAttempt + 1).apply();
				return;
			}
		}
		prefs.edit().remove(PREF_NO_WIFI_RETRY_ATTEMPT).apply();

		String currentToken = (getCurrentArtwork() != null) ? getCurrentArtwork().getToken() : null;

		RestAdapter restAdapter = new RestAdapter.Builder().setEndpoint("http://boardgamegeek.com/")
			.setConverter(new SimpleXmlConverter(new Persister())).build();

		BggService service = restAdapter.create(BggService.class);
		HotGamesResponse response = service.getHotGames();

		if (response == null || response.hotGames == null || response.hotGames.size() == 0) {
			Log.w(TAG, "Invalid response");
			throw new RetryException();
		}

		Random random = new Random();
		HotGame hg;
		String token;
		while (response.hotGames.size() > 0) {
			hg = response.hotGames.remove(random.nextInt(response.hotGames.size()));
			token = Integer.toString(hg.id);
			if (hg.isValid() && !TextUtils.equals(token, currentToken)) {
				publishArtwork(new Artwork.Builder().title(hg.name)
					.byline("#" + hg.rank + " " + getString(R.string.byline_suffix_hotness_boardgame))
					.imageUri(hg.getUri()).token(token).viewIntent(hg.getIntent()).build());
				break;
			}
		}

		scheduleUpdate(System.currentTimeMillis() + ROTATE_TIME_MILLIS);
	}

	private boolean isWifiConnected() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		return ni != null && ni.isConnected();
	}
}
