package spa.lyh.cn.lib_https.cookie;

import android.content.Context;

import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

public final class SimpleCookieJar implements CookieJar {

	private PersistentCookieStore cookieStore;

	public SimpleCookieJar(Context context){
		this.cookieStore = new PersistentCookieStore(context);
	}

	@Override
	public synchronized void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
		if (cookies != null && cookies.size() > 0){
			for (Cookie item : cookies){
				cookieStore.add(url,item);
			}
		}
	}

	@Override
	public synchronized List<Cookie> loadForRequest(HttpUrl url) {
		return cookieStore.get(url);
	}
}
