package spa.lyh.cn.io;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
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
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.style.PictureSelectorUIStyle;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import spa.lyh.cn.lib_https.CommonOkHttpClient;
import spa.lyh.cn.lib_https.listener.DisposeDataHandle;
import spa.lyh.cn.lib_https.listener.DisposeDownloadListener;
import spa.lyh.cn.lib_https.request.CommonRequest;
import spa.lyh.cn.lib_https.request.RequestParams;
import spa.lyh.cn.peractivity.ManifestPro;
import spa.lyh.cn.peractivity.PermissionActivity;
import spa.lyh.cn.utils_io.IOUtils;

public class MainActivity extends PermissionActivity implements View.OnClickListener {
    private Button download,delete,insert,insert2,select;
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
        select = findViewById(R.id.select);
        select.setOnClickListener(this);
        askForPermission(NOT_REQUIRED_ONLY_REQUEST, ManifestPro.permission.WRITE_EXTERNAL_STORAGE);

        dir = Environment.getExternalStorageDirectory()+ "/" +Environment.DIRECTORY_DOWNLOADS+"/Q";
        //dir = getExternalCacheDir()+"/Q";
        fileName = "uuid.txt";
        //fileName = "5-140FGZ248-53.gif";
        File file = new File(dir+"/"+fileName);
        if (file.exists()){
            Log.e("qwer","存在文件");
        }

        showPic(dir+"/5-140FGZ248-53.gif");

        readShow();

        try {
            /*RandomAccessFile mFile = new RandomAccessFile(dir+"/5-140FGZ248-53.gif","r");
            Log.e("qwer",mFile.length()+"");*/
            FileInputStream fis = IOUtils.getFileInputStream(this,dir+"/5-140FGZ248-53.gif");
            Log.e("qwer",fis.getChannel().size()+"");
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
                        IOUtils.OVERWRITE_FIRST,
                        new DisposeDownloadListener() {
                            @Override
                            public void onSuccess(String filePath, String fileName) {
                                Toast.makeText(MainActivity.this,"下载成功",Toast.LENGTH_SHORT).show();
                                showPic(filePath);
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
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(IOUtils.createFileOutputStream(this,dir,fileName,IOUtils.OVERWRITE_FIRST).getFos()));
                    out.write("测试文档1");
                    out.flush();
                    out.close();
                    readShow();
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
            case R.id.select:
                open();
                break;
        }
    }


    public static Call downloadFile(Context context, String url, String path, int mod,DisposeDownloadListener listener) {
        RequestParams params = new RequestParams();
        return CommonOkHttpClient.getInstance(context).downloadFile(context,
                CommonRequest.createDownloadRequest(url, null, params, true),
                new DisposeDataHandle(listener, path, true),mod);
    }

    private void open(){
        PictureSelector.create(this)
                .openGallery(PictureMimeType.ofImage())
                .imageEngine(GlideEngine.createGlideEngine())
                //.setPictureUIStyle(new PictureSelectorUIStyle())
                //.setPictureCropStyle(PicStyle.getCropStyle(activity))
                .isWeChatStyle(false)
                .setPictureWindowAnimationStyle(PicStyle.getPicAnimation(this))
                .isWithVideoImage(false)
                .imageFormat(PictureMimeType.PNG)
                .maxSelectNum(1)// 最大图片选择数量
                .minSelectNum(1)// 最小选择数量
                .imageSpanCount(4)// 每行显示个数
                .isReturnEmpty(false)// 未选择数据时点击按钮是否可以返回
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                .selectionMode(PictureConfig.SINGLE)// 多选 or 单选
                .isSingleDirectReturn(true)// 单选模式下是否直接返回，PictureConfig.SINGLE模式下有效
                .isPreviewImage(true)// 是否可预览图片
                .isCamera(false)
                .isZoomAnim(false)// 图片列表点击 缩放效果 默认true
                //.enableCrop(true)
                .isCompress(false)
                .synOrAsy(false)
                //.withAspectRatio(1,1)
                .isGif(true)
                //.freeStyleCropEnabled(false)
                //.compressSavePath(cacheFilePath)
                //.setOutputCameraPath(cacheFilePath.substring(cacheFilePath.indexOf("Android")-1))
                //.circleDimmedLayer(false)
                //.showCropFrame(true)
                //.showCropGrid(true)
                .isOpenClickSound(false)// 是否开启点击声音
                //.isDragFrame(false)
                //.cutOutQuality(90)// 裁剪输出质量 默认100
                //.minimumCompressSize(100)// 小于100kb的图片不压缩
                .forResult(PictureConfig.CHOOSE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case PictureConfig.CHOOSE_REQUEST:
                List<LocalMedia> selectList = PictureSelector.obtainMultipleResult(data);
                // 例如 LocalMedia 里面返回三种path
                // 1.media.getPath(); 为原图path
                // 2.media.getCutPath();为裁剪后path，需判断media.isCut();是否为true  注意：音视频除外
                // 3.media.getCompressPath();为压缩后path，需判断media.isCompressed();是否为true  注意：音视频除外
                // 如果裁剪并压缩了，以取压缩路径为准，因为是先裁剪后压缩的
                String path = "";
                for (LocalMedia localMedia : selectList) {
                    path = localMedia.getRealPath();
                    //Log.e("qwer",path);
                    //Uri uri = IOUtils.getFileUri();
                    Glide.with(MainActivity.this)
                            .asDrawable()
                            .load(IOUtils.getFileUri(MainActivity.this,path))
                            .into(image);
                }
                Uri uri = IOUtils.getFileUri(MainActivity.this,path);
                Log.e("qwer",IOUtils.getFilePath(MainActivity.this,uri));
                break;

        }
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











}
