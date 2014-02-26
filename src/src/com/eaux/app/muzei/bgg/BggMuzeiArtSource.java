package com.eaux.app.muzei.bgg;

import java.util.Random;

import org.simpleframework.xml.core.Persister;

import retrofit.RestAdapter;
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

	public BggMuzeiArtSource() {
		super(SOURCE_NAME);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		setUserCommands(BUILTIN_COMMAND_ID_NEXT_ARTWORK);
	}

	@Override
	protected void onTryUpdate(int arg0) throws RetryException {
		String currentToken = (getCurrentArtwork() != null) ? getCurrentArtwork().getToken() : null;

		RestAdapter restAdapter = new RestAdapter.Builder().setEndpoint("http://boardgamegeek.com/")
			.setConverter(new SimpleXmlConverter(new Persister())).build();

		BggService service = restAdapter.create(BggService.class);
		HotGamesResponse response = service.getHotGames();

		if (response == null || response.hotGames == null) {
			throw new RetryException();
		}

		if (response.hotGames.size() == 0) {
			Log.w(TAG, "No games returned from API.");
			scheduleUpdate(System.currentTimeMillis() + ROTATE_TIME_MILLIS);
			return;
		}

		Random random = new Random();
		HotGame hg;
		String token;
		while (true) {
			hg = response.hotGames.get(random.nextInt(response.hotGames.size()));
			token = Integer.toString(hg.id);
			if (response.hotGames.size() <= 1 || !TextUtils.equals(token, currentToken)) {
				break;
			}
		}

		publishArtwork(new Artwork.Builder().title(hg.name)
			.byline("#" + hg.rank + " " + getString(R.string.byline_suffix_hotness_boardgame)).imageUri(hg.getUri())
			.token(token).viewIntent(hg.getIntent()).build());

		scheduleUpdate(System.currentTimeMillis() + ROTATE_TIME_MILLIS);
	}
}
