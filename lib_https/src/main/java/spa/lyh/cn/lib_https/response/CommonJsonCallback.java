package spa.lyh.cn.lib_https.response;

import android.os.Handler;
import android.os.Looper;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.Response;
import spa.lyh.cn.lib_https.exception.OkHttpException;
import spa.lyh.cn.lib_https.listener.DisposeDataHandle;
import spa.lyh.cn.lib_https.listener.DisposeDataListener;
import spa.lyh.cn.lib_https.listener.DisposeHandleCookieListener;
import spa.lyh.cn.lib_https.log.LyhLog;
import spa.lyh.cn.lib_https.response.base.CommonBase;

/**
 * @author liyuhao
 * @function 专门处理JSON的回调
 */
public class CommonJsonCallback extends CommonBase implements Callback {




    /**
     * 将其它线程的数据转发到UI线程
     */
    private Handler mDeliveryHandler;
    private DisposeDataListener mListener;
    private TypeReference<?> typeReference;
    private boolean devMode;

    public CommonJsonCallback(DisposeDataHandle handle) {
        this.mListener = handle.mListener;
        this.typeReference = handle.typeReference;
        this.devMode = handle.devMode;
        this.mDeliveryHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onFailure(final Call call, final IOException ioexception) {
        if(ioexception != null){
            ioexception.printStackTrace();
            /**
             * 此时还在非UI线程，因此要转发
             */
            mDeliveryHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (ioexception.getMessage() != null){
                        if (ioexception.getMessage().equals("Canceled")){
                            mListener.onFailure(new OkHttpException(OkHttpException.CANCEL_REQUEST, CANCEL_MSG));
                        }else {
                            mListener.onFailure(new OkHttpException(OkHttpException.NETWORK_ERROR, NET_MSG));
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onResponse(final Call call, final Response response) throws IOException {
        final String result = response.body().string();
        final ArrayList<String> cookieLists = handleCookie(response.headers());
        mDeliveryHandler.post(new Runnable() {
            @Override
            public void run() {
                handleResponse(result);
                /**
                 * handle the cookie
                 */
                if (mListener instanceof DisposeHandleCookieListener) {
                    ((DisposeHandleCookieListener) mListener).onCookie(cookieLists);
                }
            }
        });
    }

    private ArrayList<String> handleCookie(Headers headers) {
        ArrayList<String> tempList = new ArrayList<String>();
        for (int i = 0; i < headers.size(); i++) {
            if (headers.name(i).equalsIgnoreCase(COOKIE_STORE)) {
                tempList.add(headers.value(i));
            }
        }
        return tempList;
    }

    private void handleResponse(Object responseObj) {
        if (responseObj == null || responseObj.toString().trim().equals("")) {
            mListener.onFailure(new OkHttpException(OkHttpException.NETWORK_ERROR, EMPTY_MSG));//网络错误
            return;
        }
        String responseString = responseObj.toString();
        //是否在控制台打印信息
        if (devMode){
            LyhLog.e(TAG,responseString);
        }

        try {
            //范型为空时，直接回调成功的字符串
            if (typeReference == null){
                mListener.onSuccess(responseObj.toString());
                return;
            }
            //按照范型解析
            Object realResult = JSONObject.parseObject(responseObj.toString(), typeReference);

            if (realResult != null){
                mListener.onSuccess(realResult);
            }else {
                mListener.onFailure(new OkHttpException(OkHttpException.OTHER_ERROR, EMPTY_MSG));
            }
        } catch (Exception e) {
            e.printStackTrace();
            mListener.onFailure(new OkHttpException(OkHttpException.JSON_ERROR,JSON_MSG_TYPEREFERENCE));//json解析错误
        }
    }
}