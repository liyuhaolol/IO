package spa.lyh.cn.utils_io;

import android.os.Build;
import android.os.Environment;
import android.util.Log;

public class IOUtils {


    public static void testMethord(String path){
        String storagePath = Environment.getExternalStorageDirectory().getPath();
        String filePath;
        if (path.startsWith("/sdcard")){
            //非标准写法,转换为标准写法
            filePath = storagePath + path.substring(7);

        }else {
            filePath = path;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            //Android10以上
            if (verifyStoragePath(filePath)){
                //路径是起步正确的
                //Environment.DIRECTORY_SCREENSHOTS
                Log.e("liyuhao",filePath);
            }



        }else {
            //Android9以下
            if (verifyStoragePath(filePath)){
                //路径是起步正确的
            }
        }
    }

    private static boolean verifyStoragePath(String path){
        if (path.startsWith(Environment.getExternalStorageDirectory().getPath())){
            //为正确的路径
            return true;
        }else {
            Log.e("IOUtils","绝对路径可能存在错误");
            return false;
        }
    }
}
