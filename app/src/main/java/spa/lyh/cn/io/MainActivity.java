package spa.lyh.cn.io;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import okhttp3.Call;
import spa.lyh.cn.lib_https.CommonOkHttpClient;
import spa.lyh.cn.lib_https.listener.DisposeDataHandle;
import spa.lyh.cn.lib_https.listener.DisposeDownloadListener;
import spa.lyh.cn.lib_https.request.CommonRequest;
import spa.lyh.cn.lib_https.request.RequestParams;
import spa.lyh.cn.utils_io.IOUtils;

public class MainActivity extends PermissionActivity implements View.OnClickListener {
    private Button download,delete,insert,insert2;
    private String dir,fileName;
    private ImageView image;
    private TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        image = findViewById(R.id.img);
        tv = findViewById(R.id.tv);
        download = findViewById(R.id.download);
        download.setOnClickListener(this);
        delete = findViewById(R.id.delete);
        delete.setOnClickListener(this);
        insert = findViewById(R.id.insert);
        insert.setOnClickListener(this);
        insert2 = findViewById(R.id.insert2);
        insert2.setOnClickListener(this);
        hasPermission(NOT_REQUIRED_ONLY_REQUEST, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        dir = Environment.getExternalStorageDirectory()+ "/" +Environment.DIRECTORY_DOWNLOADS+"/Q";
        fileName = "uuid";
        File file = new File(dir+"/"+fileName);
        if (file.exists()){
            Log.e("liyuhao","存在uuid");
        }
        Glide.with(MainActivity.this)
                .asDrawable()
                .load(IOUtils.getFileUri(this,dir+"/5-140FGZ248-53.gif"))
                .into(image);

       try {
           FileInputStream in = IOUtils.getFileInputStream(MainActivity.this,dir+"/"+fileName);
           if (in != null){
               BufferedReader br = new BufferedReader(new InputStreamReader(in));
               String lineTxt = br.readLine();
               if (lineTxt != null){
                   Log.e("liyuhao","内容为："+lineTxt);
                   tv.setText(lineTxt);
               }
               br.close();
           }
       }catch (Exception e){
           e.printStackTrace();
       }
    }
    //getExternalCacheDir().getPath()+ "/Q"
    //Environment.getExternalStorageDirectory()+ "/" +Environment.DIRECTORY_DOWNLOADS+"/Q"
    //getObbDir().getPath()+"/Q"

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.download:
                //http://edge.ivideo.sina.com.cn/6265508.flv?KID=sina,viask&Expires=1582646400&ssig=bs686OJicS
                //https://ss0.bdstatic.com/70cFvHSh_Q1YnxGkpoWK1HF6hhy/it/u=3208238474,2536510412&fm=26&gp=0.jpg
                //https://downloads.openwrt.org/releases/19.07.1/targets/x86/64/packages/comgt-directip_0.32-32_x86_64.ipk
                //http://www.lanrentuku.com/savepic/img/allimg/1407/5-140FGZ248-53.gif


                downloadFile(MainActivity.this,
                        "http://www.lanrentuku.com/savepic/img/allimg/1407/5-140FGZ248-53.gif",
                        dir,
                        new DisposeDownloadListener() {
                            @Override
                            public void onSuccess(String filePath, String fileName) {
                                Toast.makeText(MainActivity.this,"下载成功",Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailure(Object reasonObj) {
                                Toast.makeText(MainActivity.this,"下载失败",Toast.LENGTH_SHORT).show();
                                Log.e("liyuhao","下载失败");
                            }

                            @Override
                            public void onProgress(boolean haveFileSize, int progress, String currentSize, String sumSize) {

                            }
                        });
                break;
            case R.id.insert:
                try {
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(IOUtils.createFileOutputStream(this,dir,fileName).getFos()));
                    out.write("测试文档1");
                    out.flush();
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.insert2:
                try {
                    FileOutputStream fileOut = IOUtils.getFileOutputStream(MainActivity.this,dir+"/"+fileName);
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(fileOut));
                    out.write("测试文档2");
                    out.flush();
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.delete:
                if (IOUtils.delete(MainActivity.this,dir+"/"+fileName)){
                    Toast.makeText(MainActivity.this,"删除成功",Toast.LENGTH_SHORT).show();
                    Log.e("liyuhao","删除成功");
                }else {
                    Toast.makeText(MainActivity.this,"删除失败",Toast.LENGTH_SHORT).show();
                    Log.e("liyuhao","删除失败");
                }
                break;
        }
    }


    public static Call downloadFile(Context context, String url, String path, DisposeDownloadListener listener) {
        RequestParams params = new RequestParams();
        return CommonOkHttpClient.getInstance(context).downloadFile(context,
                CommonRequest.createDownloadRequest(url, null, params, true),
                new DisposeDataHandle(listener, path, true));
    }
}
