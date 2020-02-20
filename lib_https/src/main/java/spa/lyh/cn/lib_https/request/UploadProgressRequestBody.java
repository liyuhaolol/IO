package spa.lyh.cn.lib_https.request;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.text.DecimalFormat;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import spa.lyh.cn.lib_https.listener.UploadProgressListener;

public class UploadProgressRequestBody extends RequestBody {
    private MultipartBody mMultipartBody;
    private UploadProgressListener mProgressListener;
    private boolean isDev;

    private long mCurrentLength;

    private int lastSize = -1;

    public UploadProgressRequestBody(MultipartBody multipartBody){
        this(multipartBody,false,null);
    }

    public UploadProgressRequestBody(MultipartBody multipartBody,boolean isDev,UploadProgressListener progressListener){
        this.mMultipartBody = multipartBody;
        this.mProgressListener = progressListener;
        this.isDev = isDev;
    }
    @Override
    public MediaType contentType() {
        return mMultipartBody.contentType();
    }

    @Override
    public long contentLength() throws IOException {
        return mMultipartBody.contentLength();
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        //这里需要另一个代理类来获取写入的长度
        ForwardingSink forwardingSink = new ForwardingSink(sink) {
            final long totalLength = contentLength();
            @Override
            public void write(Buffer source, long byteCount) throws IOException {
                //这里可以获取到写入的长度
                mCurrentLength += byteCount;
                //计算百分比
                float mProgress = getNumber((float)mCurrentLength/(float)totalLength)*100;
                int currentSize = (int) mProgress;
                if (currentSize != lastSize){
                    lastSize = currentSize;
                    //回调进度
                    if(mProgressListener != null){
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                mProgressListener.onProgress(lastSize);
                            }
                        });
                    }
                    if (isDev){
                        Log.e("UploadRequest",lastSize+"%   "+convertFileSize(mCurrentLength)+"/"+convertFileSize(totalLength));
                    }
                }
                super.write(source, byteCount);
            }
        };
        //转一下
        BufferedSink bufferedSink = Okio.buffer(forwardingSink);
        //写数据
        mMultipartBody.writeTo(bufferedSink);
        //刷新一下数据
        bufferedSink.flush();
    }

    /**
     * 得到对应位数小数<P/>
     * Created by liyuhao on 2016/3/24.<P/>
     * @param number float的数
     * @return float的数
     */
    private float getNumber(float number){
        DecimalFormat df = new DecimalFormat("#.##############");
        float f=Float.valueOf(df.format(number));
        return f;
    }


    /**
     * 计算文件大小<P/>
     * Created by liyuhao on 2016/3/24.<P/>
     * @param size 字节数
     * @return 对应的G，M，K
     */
    private String convertFileSize(long size) {
        long kb = 1024;
        long mb = kb * 1024;
        long gb = mb * 1024;

        if (size >= gb) {
            return String.format("%.2f GB", (float) size / gb);
        } else if (size >= mb) {
            float f = (float) size / mb;
            return String.format(f > 100 ? "%.0f MB" : "%.2f MB", f);
        } else if (size >= kb) {
            float f = (float) size / kb;
            return String.format(f > 100 ? "%.0f KB" : "%.2f KB", f);
        } else
            return String.format("%d B", size);
    }
}
