package spa.lyh.cn.io;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import okhttp3.Call;
import spa.lyh.cn.lib_https.CommonOkHttpClient;
import spa.lyh.cn.lib_https.listener.DisposeDataHandle;
import spa.lyh.cn.lib_https.listener.DisposeDownloadListener;
import spa.lyh.cn.lib_https.request.CommonRequest;
import spa.lyh.cn.lib_https.request.RequestParams;
import spa.lyh.cn.utils_io.IOUtils;

public class MainActivity extends PermissionActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (hasPermission(NOT_REQUIRED_ONLY_REQUEST, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            //doAfterPermission();
        }
        Log.e("liyuhao",
                new IOUtils().createFileName(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DOWNLOADS + "/Q","5-140FGZ248-53.gif"));
    }
    //Environment.getExternalStorageDirectory()+ "/" +Environment.DIRECTORY_DOWNLOADS+"/Q"
    //getObbDir().getPath()+"/Q"
    @Override
    public void doAfterPermission() {
        //IOUtils.testMethord("/sdcard/A.txt");
        //IOUtils.testMethord("/sdcard");
        //http://edge.ivideo.sina.com.cn/6265508.flv?KID=sina,viask&Expires=1582646400&ssig=bs686OJicS
        //https://ss0.bdstatic.com/70cFvHSh_Q1YnxGkpoWK1HF6hhy/it/u=3208238474,2536510412&fm=26&gp=0.jpg
        //https://downloads.openwrt.org/releases/19.07.1/targets/x86/64/packages/comgt-directip_0.32-32_x86_64.ipk
        //http://www.lanrentuku.com/savepic/img/allimg/1407/5-140FGZ248-53.gif
        downloadFile(MainActivity.this,
                "http://www.lanrentuku.com/savepic/img/allimg/1407/5-140FGZ248-53.gif",
                Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DOWNLOADS + "/Q",
                new DisposeDownloadListener() {
                    @Override
                    public void onSuccess(String filePath, String fileName) {

                    }

                    @Override
                    public void onFailure(Object reasonObj) {
                        Log.e("liyuhao","下载失败");
                    }

                    @Override
                    public void onProgress(boolean haveFileSize, int progress, String currentSize, String sumSize) {

                    }
                });
    }

    @Override
    public void rejectAfterPermission() {
        doAfterPermission();
    }


    public static Call downloadFile(Context context, String url, String path, DisposeDownloadListener listener) {
        RequestParams params = new RequestParams();
        return CommonOkHttpClient.getInstance(context).downloadFile(context,
                CommonRequest.createDownloadRequest(url, null, params, true),
                new DisposeDataHandle(listener, path, true));
    }
}
