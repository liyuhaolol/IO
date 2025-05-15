package spa.lyh.cn.lib_https.listener;

import java.util.ArrayList;

/**********************************************************
 * cookie的处理
 **********************************************************/
public interface DisposeHandleCookieListener extends DisposeDataListener
{
	public void onCookie(ArrayList<String> cookieStrLists);
}
