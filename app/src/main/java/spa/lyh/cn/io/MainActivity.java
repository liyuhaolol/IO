package spa.lyh.cn.io;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.Result;

import okhttp3.Call;
import spa.lyh.cn.lib_https.CommonOkHttpClient;
import spa.lyh.cn.lib_https.listener.DisposeDataHandle;
import spa.lyh.cn.lib_https.listener.DisposeDownloadListener;
import spa.lyh.cn.lib_https.request.CommonRequest;
import spa.lyh.cn.lib_https.request.RequestParams;
import spa.lyh.cn.peractivity.ManifestPro;
import spa.lyh.cn.peractivity.PermissionActivity;
import spa.lyh.cn.utils_io.IOUtils;
import spa.lyh.cn.utils_io.model.FileData;

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
        //askForPermission(NOT_REQUIRED_ONLY_REQUEST, ManifestPro.permission.WRITE_EXTERNAL_STORAGE);

        dir = Environment.getExternalStorageDirectory()+ "/" +Environment.DIRECTORY_DOWNLOADS+"/Q";
/*        String filePath = Environment.getExternalStorageDirectory()+ "/" +Environment.DIRECTORY_DOWNLOADS+"/a.jpeg";
        File file = new File(filePath);
        if (file.exists()){
            Log.e("qwer","文件存在");
            Log.e("qwer","文件大小"+file.length());
            try {
                //FileInputStream fis = new FileInputStream(file);
                Uri uri = Uri.fromFile(file);
                FileInputStream fis2 = (FileInputStream) getContentResolver().openInputStream(uri);
            } catch (FileNotFoundException e) {
               e.printStackTrace();
            }
        }*/
        //dir = getExternalCacheDir()+"/Q";
        fileName = "uuid.txt";
        //fileName = "5-140FGZ248-53.gif";
        /*File file = new File(dir+"/"+fileName);
        if (file.exists()){
            Log.e("qwer","存在文件");
        }*/

        //showPic(dir+"/5-140FGZ248-53.gif");

        //readShow();

        /*try {
            *//*RandomAccessFile mFile = new RandomAccessFile(dir+"/5-140FGZ248-53.gif","r");
            Log.e("qwer",mFile.length()+"");*//*
            FileInputStream fis = IOUtils.getFileInputStream(this,dir+"/5-140FGZ248-53.gif");
            Log.e("qwer",fis.getChannel().size()+"");
        }catch (Exception e){
            e.printStackTrace();
        }*/
        /*dir = getExternalCacheDir()+"/Q";
        Log.e("qwer",dir);
        File file = new File(dir);
        if (!file.exists()){
            file.mkdir();
        }*/
        /*String a = "/storage/emulated/0/Android/data/spa.lyh.cn.io/cache/Q/1.jpg";
        showPic(a);*/
        //dir = "/storage/80FF-1C10/Download";
        //makeFile();
        createFile();

    }
    //getExternalCacheDir().getPath()+ "/Q"
    //Environment.getExternalStorageDirectory()+ "/" +Environment.DIRECTORY_DOWNLOADS+"/Q"
    //getObbDir().getPath()+"/Q"

    private void createFile(){
        String testFileName = "aaaa.zip(1)";
        String mimeType = IOUtils.getMimeType(testFileName);
        ContentValues values = new ContentValues();
        String relativePath = dir.substring(Environment.getExternalStorageDirectory().getPath().length()+1);
        //这里用download，其实用谁都无所谓，只是一个string
        values.put(MediaStore.Downloads.DISPLAY_NAME, testFileName);
        values.put(MediaStore.Downloads.MIME_TYPE, mimeType);//MediaStore对应类型名
        values.put(MediaStore.Downloads.RELATIVE_PATH, relativePath);//公共目录下目录名
        Uri external = getUri(dir+"/"+testFileName);
        if (external == null){
            Log.e("qwer","external为空");
            return;
        }
        ContentResolver resolver = getContentResolver();
        Uri insertUri;
        try {
            insertUri = resolver.insert(external, values);//使用ContentResolver创建需要操作的文件
        }catch (Exception e){
            e.printStackTrace();
            return;
        }
        if (insertUri != null) {
            FileData data = new FileData();
            data.setFilePath(IOUtils.getFilePath(this,insertUri));
            data.setFileName(IOUtils.getFileName(data.getFilePath()));
            Log.e("qwer","文件名为："+data.getFileName());
/*            ContentValues updateValues = new ContentValues();
            updateValues.put(MediaStore.Downloads.DISPLAY_NAME, "aaaa(1).zip");
            updateValues.put(MediaStore.Downloads.MIME_TYPE, mimeType);//MediaStore对应类型名
            updateValues.put(MediaStore.Downloads.RELATIVE_PATH, relativePath);//公共目录下目录名
            try {
                int updated = resolver.update(insertUri, updateValues, null, null);
                if (updated > 0) {
                    Log.e("qwer","文件名修改成功");
                    //FileData data2 = new FileData();
                    //data2.setFilePath(IOUtils.getFilePath(this,insertUri));
                    //data2.setFileName(IOUtils.getFileName(data2.getFilePath()));
                    //Log.e("qwer","文件名为："+data2.getFileName());
                }
            } catch (Exception e) {
                // 如果重命名失败，删除临时文件
                e.printStackTrace();
                Log.e("qwer","重命名失败，移除临时文件");
                resolver.delete(insertUri, null, null);
            }*/

        }else {
            Log.e("qwer","insertUri为空");
        }
    }


    private void makeFile(){
        String pathA = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)+"/did";
        String fileNameA = "did.png";
        File file = new File(pathA +"/"+ fileNameA);
        if (file.exists()){
            //存在
            try {
                FileInputStream in = IOUtils.getFileInputStream(MainActivity.this,pathA+"/"+fileNameA);
                if (in != null){
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));
                    String lineTxt = br.readLine();
                    if (lineTxt != null){
                        tv.setText(lineTxt);
                    }
                    br.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }else {
            //不存在
            try {
                FileData data = IOUtils.createFileOutputStream(this,pathA,fileNameA,IOUtils.OVERWRITE_FIRST);
                if (data != null){
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(data.getFos()));
                    out.write("12345");
                    out.flush();
                    out.close();
                    readShow();
                }else {
                    Log.e("qwer","无法创建文件");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

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
                        //"https://codeload.github.com/Molunerfinn/PicGo/zip/refs/heads/dev",
                        dir,
                        IOUtils.OVERWRITE_FIRST,
                        new DisposeDownloadListener() {
                            @Override
                            public void onSuccess(String filePath, String fileName) {
                                Toast.makeText(MainActivity.this,"下载成功",Toast.LENGTH_SHORT).show();
                                //showPic(filePath);
                            }

                            @Override
                            public void onFailure(Object reasonObj) {
                                Toast.makeText(MainActivity.this,"下载失败",Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onProgress(boolean haveFileSize, int progress, String currentSize, String sumSize) {

                            }
                        });
                break;
            case R.id.insert:
                try {
                    FileData data = IOUtils.createFileOutputStream(this,dir,fileName,IOUtils.OVERWRITE_FIRST);
                    if (data != null){
                        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(data.getFos()));
                        out.write("测试文档1");
                        out.flush();
                        out.close();
                        readShow();
                    }else {
                        Log.e("qwer","无法创建文件");
                    }
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
                    readShow();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.delete:
                if (IOUtils.delete(MainActivity.this,dir+"/"+fileName)){
                    Toast.makeText(MainActivity.this,"删除成功",Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(MainActivity.this,"删除失败",Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }


    public static Call downloadFile(Context context, String url, String path, int mod,DisposeDownloadListener listener) {
        RequestParams params = new RequestParams();
        return CommonOkHttpClient.getInstance(context).downloadFile(context,
                CommonRequest.createDownloadRequest(url, null, params, true),
                new DisposeDataHandle(listener, path, true),mod);
    }



    private void showPic(String filePath){
        Glide.with(MainActivity.this)
                .asDrawable()
                .load(IOUtils.getFileUri(this,filePath))
                .into(image);
    }

    private void readShow(){
        try {
            FileInputStream in = IOUtils.getFileInputStream(MainActivity.this,dir+"/"+fileName);
            if (in != null){
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String lineTxt = br.readLine();
                if (lineTxt != null){
                    tv.setText(lineTxt);
                }
                br.close();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }



    @TargetApi(29)
    private static Uri getUri(String path){
        String pubPath = Environment.getExternalStorageDirectory().getPath();
        String mimeType = IOUtils.getMimeType(path);
        if (path.startsWith(pubPath+"/"+Environment.DIRECTORY_DOWNLOADS)){
            return MediaStore.Downloads.EXTERNAL_CONTENT_URI;//官方api竟然不好使，你敢信，你敢信？
            //return MediaStore.Files.getContentUri("external");
        }else if (path.startsWith(pubPath+"/"+Environment.DIRECTORY_DCIM)){
            if (mimeType.startsWith("video")){
                return MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            }else {
                return MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            }
        }else if (path.startsWith(pubPath+"/"+Environment.DIRECTORY_MOVIES)){
            return MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        }else if (path.startsWith(pubPath+"/"+Environment.DIRECTORY_PICTURES)){
            return MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }else if (path.startsWith(pubPath+"/"+Environment.DIRECTORY_ALARMS)){
            return MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }else if (path.startsWith(pubPath+"/"+Environment.DIRECTORY_MUSIC)){
            return MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }else if (path.startsWith(pubPath+"/"+Environment.DIRECTORY_NOTIFICATIONS)){
            return MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }else if (path.startsWith(pubPath+"/"+Environment.DIRECTORY_PODCASTS)){
            return MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }else if (path.startsWith(pubPath+"/"+Environment.DIRECTORY_RINGTONES)){
            return MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }else if (path.startsWith(pubPath+"/"+Environment.DIRECTORY_DOCUMENTS)){
            return MediaStore.Files.getContentUri("external");
        }else{
            return MediaStore.Files.getContentUri("external");
        }
    }







}
