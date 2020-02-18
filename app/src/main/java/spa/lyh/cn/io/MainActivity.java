package spa.lyh.cn.io;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.os.Bundle;
import android.os.Environment;

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

    @Override
    public void doAfterPermission() {
        //IOUtils.testMethord("/sdcard/A.txt");
        IOUtils.testMethord(getExternalCacheDir().getPath());
    }

    @Override
    public void rejectAfterPermission() {

    }
}
