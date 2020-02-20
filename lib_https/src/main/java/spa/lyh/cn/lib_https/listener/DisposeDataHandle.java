package spa.lyh.cn.lib_https.listener;

import com.alibaba.fastjson.TypeReference;

/**
 * @author liyuhao
 *
 */
public class DisposeDataHandle {

	public DisposeDataListener mListener = null;
	public DisposeDownloadListener downloadListener = null;
	//public Class<?> mClass = null;
	public TypeReference<?> typeReference = null;
	public boolean devMode;

	public String mSource = null;

	/**
	 * 不指定对应的clazz，应该用不到
	 * @param listener
	 */
	public DisposeDataHandle(DisposeDataListener listener)
	{
		this.mListener = listener;
	}

	public DisposeDataHandle(DisposeDataListener listener, TypeReference<?> typeReference,boolean devMode)
	{
		this.mListener = listener;
		this.typeReference = typeReference;
		this.devMode = devMode;
	}

	public DisposeDataHandle(DisposeDownloadListener listener, String source,boolean devMode)
	{
		this.downloadListener = listener;
		this.mSource = source;
		this.devMode = devMode;
	}
}