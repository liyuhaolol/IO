package spa.lyh.cn.io;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
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
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;

import okhttp3.Call;
import spa.lyh.cn.lib_https.CommonOkHttpClient;
import spa.lyh.cn.lib_https.listener.DisposeDataHandle;
import spa.lyh.cn.lib_https.listener.DisposeDownloadListener;
import spa.lyh.cn.lib_https.request.CommonRequest;
import spa.lyh.cn.lib_https.request.RequestParams;
import spa.lyh.cn.utils_io.IOUtils;

public class MainActivity extends PermissionActivity implements View.OnClickListener {
    private Button download,delete;
    private String dir,fileName;
    private ImageView image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        image = findViewById(R.id.img);
        download = findViewById(R.id.download);
        download.setOnClickListener(this);
        delete = findViewById(R.id.delete);
        delete.setOnClickListener(this);
        hasPermission(NOT_REQUIRED_ONLY_REQUEST, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        dir = Environment.getExternalStorageDirectory()+ "/" +Environment.DIRECTORY_DOCUMENTS+"/Q";
        fileName = "uuid.txt";
        //new IOUtils().querySignImage(this,"5-140FGZ248-53.gif");
        /*ContentResolver resolver = getContentResolver();
        Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, 95);
        try {
            ParcelFileDescriptor parcelFd = resolver.openFileDescriptor(contentUri, "r");
            //FileOutputStream outputStream = new FileOutputStream(parcelFd.getFileDescriptor());
            Drawable d=Drawable.createFromStream(resolver.openInputStream(contentUri),null);
            Glide.with(MainActivity.this)
                                        .asDrawable()
                                        .load(d)
                                        .into(image);

        }catch (Exception e){
            e.printStackTrace();
        }*/
        //resolver.openFileDescriptor();

       /*FileOutputStream ss = new IOUtils().getFileOutputStream(this,dir,"uuid.txt");
       try {
           ss.flush();
           ss.close();
       }catch (Exception e){
           e.printStackTrace();
       }*/
        //new IOUtils().querySignImage(this,"uuid.txt");
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


                /*downloadFile(MainActivity.this,
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
                        });*/
                FileOutputStream ss = new IOUtils().getFileOutputStream(this,dir,fileName);
                try {
                   BufferedWriter out = new BufferedWriter(new OutputStreamWriter(ss));
                   out.write("你好");
                   ss.flush();
                   ss.close();
                   out.flush();
                   out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.delete:
                if (new IOUtils().delete(MainActivity.this,dir+"/"+fileName)){
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
