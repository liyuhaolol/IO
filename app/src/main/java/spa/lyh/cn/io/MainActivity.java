package spa.lyh.cn.io;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;

import okhttp3.Call;
import spa.lyh.cn.lib_https.CommonOkHttpClient;
import spa.lyh.cn.lib_https.listener.DisposeDataHandle;
import spa.lyh.cn.lib_https.listener.DisposeDownloadListener;
import spa.lyh.cn.lib_https.request.CommonRequest;
import spa.lyh.cn.utils_io.IOUtils;

public class MainActivity extends PermissionActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (hasPermission(NOT_REQUIRED_LOAD_METHOD, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            doAfterPermission();
        }
    }
    //Environment.getExternalStorageDirectory()+ "/" +Environment.DIRECTORY_DOWNLOADS+"/Q"
    //getObbDir().getPath()+"/Q"
    @Override
    public void doAfterPermission() {
        //IOUtils.testMethord("/sdcard/A.txt");
        //IOUtils.testMethord("/sdcard");
        downloadFile(MainActivity.this,
                "https://ss0.bdstatic.com/70cFvHSh_Q1YnxGkpoWK1HF6hhy/it/u=3208238474,2536510412&fm=26&gp=0.jpg",
                Environment.getExternalStorageDirectory()+ "/" +Environment.DIRECTORY_PICTURES+"/Q",
                new DisposeDownloadListener() {
                    @Override
                    public void onSuccess(String filePath, String fileName) {

                    }

                    @Override
                    public void onFailure(Object reasonObj) {

                    }

                    @Override
                    public void onProgress(int progress, String currentSize, String sumSize) {

                    }
                });
    }

    @Override
    public void rejectAfterPermission() {

    }


    public static Call downloadFile(Context context, String url, String path, DisposeDownloadListener listener) {
        return CommonOkHttpClient.getInstance(context).downloadFile(context,
                CommonRequest.createGetRequest(url, null, null, true),
                new DisposeDataHandle(listener, path, true));
    }
}
