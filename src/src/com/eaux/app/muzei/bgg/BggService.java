package com.eaux.app.muzei.bgg;

import java.util.List;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

import retrofit.http.GET;
import android.content.Intent;
import android.net.Uri;

public interface BggService {
	@GET("/xmlapi2/hot?type=boardgame")
	HotGamesResponse getHotGames();

	/**
	 * 
	 * The Hotness list from BoardGameGeek.com
	 * 
	 * <h3>Example</h3>
	 * 
	 * <pre>
	 * &lt;items termsofuse=&quot;http://boardgamegeek.com/xmlapi/termsofuse&quot;&gt;
	 *   &lt;item /&gt;
	 *   ...
	 * &lt;/items&gt;
	 * </pre>
	 */
	static class HotGamesResponse {
		@Attribute(name = "termsofuse")
		String termsOfUse;
		@ElementList(name = "items", inline = true)
		List<HotGame> hotGames;

	}

	/**
	 * A game on The Hotness list
	 * 
	 * <h3>Example</h3>
	 * 
	 * <pre>
	 * &lt;item id=&quot;124742&quot; rank=&quot;3&quot;&gt;
	 *   &lt;thumbnail value=&quot;http://cf.geekdo-images.com/images/pic1324609_t.jpg&quot;/&gt;
	 *   &lt;name value=&quot;Android: Netrunner&quot;/&gt;
	 *   &lt;yearpublished value=&quot;2012&quot;/&gt;
	 * &lt;/item&gt;
	 * </pre>
	 */
	@Root(name = "item")
	static class HotGame {
		@Attribute
		int id;
		@Attribute
		int rank;
		@Attribute(name = "value")
		@Path("thumbnail")
		String thumbnailUrl;
		@Attribute(name = "value")
		@Path("name")
		String name;
		@Attribute(name = "value")
		@Path("yearpublished")
		int yearPublished;

		public Intent getIntent() {
			return new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.boardgamegeek.com/boardgame/" + id));
		}

		public Uri getUri() {
			String url = thumbnailUrl;
			int extensionDot = thumbnailUrl.lastIndexOf('.');
			if (extensionDot != -1) {
				if (thumbnailUrl.substring(0, extensionDot).endsWith("_t")) {
					url = thumbnailUrl.substring(0, extensionDot - 2) + thumbnailUrl.substring(extensionDot);
				}
			}
			return Uri.parse(url);
		}
	}
}
