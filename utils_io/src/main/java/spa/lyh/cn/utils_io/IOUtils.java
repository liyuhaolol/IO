package spa.lyh.cn.utils_io;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IOUtils {
    private static String android = "/Android";//内部路径

    private String mainPath;
    private String fileName;
    private static final String DEAFULT_FILE_NAME = "deafult";

    private static HashMap<String,String> mimeTypeList;

    static {
        mimeTypeList = new HashMap<>();
        mimeTypeList.put(".3gp","video/3gpp");
        mimeTypeList.put(".apk","application/vnd.android.package-archive");
        mimeTypeList.put(".asf","video/x-ms-asf");
        mimeTypeList.put(".avi","video/x-msvideo");
        mimeTypeList.put(".bin","application/octet-stream");
        mimeTypeList.put(".bmp","image/bmp");
        mimeTypeList.put(".c","text/plain");
        mimeTypeList.put(".class","application/octet-stream");
        mimeTypeList.put(".conf","text/plain");
        mimeTypeList.put(".cpp","text/plain");
        mimeTypeList.put(".doc","application/msword");
        mimeTypeList.put(".docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        mimeTypeList.put(".xls","application/vnd.ms-excel");
        mimeTypeList.put(".xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        mimeTypeList.put(".exe","application/octet-stream");
        mimeTypeList.put(".flv","video/x-flv");
        mimeTypeList.put(".gif","image/gif");
        mimeTypeList.put(".gtar","application/x-gtar");
        mimeTypeList.put(".gz","application/x-gzip");
        mimeTypeList.put(".h","text/plain");
        mimeTypeList.put(".htm","text/html");
        mimeTypeList.put(".html","text/html");
        mimeTypeList.put(".jar","application/java-archive");
        mimeTypeList.put(".java","text/plain");
        mimeTypeList.put(".jpeg","image/jpeg");
        mimeTypeList.put(".jpg","image/jpeg");
        mimeTypeList.put(".js","application/x-javascript");
        mimeTypeList.put(".ipk","application/vnd.shana.informed.package");
        mimeTypeList.put(".log","text/plain");
        mimeTypeList.put(".m3u","audio/x-mpegurl");
        mimeTypeList.put(".m4a","audio/mp4a-latm");
        mimeTypeList.put(".m4b","audio/mp4a-latm");
        mimeTypeList.put(".m4p","audio/mp4a-latm");
        mimeTypeList.put(".m4u","video/x-m4v");
        mimeTypeList.put(".mov","video/quicktime");
        mimeTypeList.put(".mp2","audio/x-mpeg");
        mimeTypeList.put(".mp3","audio/x-mpeg");
        mimeTypeList.put(".mp4","video/mp4");
        mimeTypeList.put(".mpc","application/vnd.mpohun.certificate");
        mimeTypeList.put(".mpe","video/mpeg");
        mimeTypeList.put(".mpeg","video/mpeg");
        mimeTypeList.put(".mpg","video/mpeg");
        mimeTypeList.put(".mpg4","video/mp4");
        mimeTypeList.put(".mpga","audio/mpeg");
        mimeTypeList.put(".msg","application/vnd.ms-outlook");
        mimeTypeList.put(".ogg","audio/ogg");
        mimeTypeList.put(".pdf","application/pdf");
        mimeTypeList.put(".png","image/png");
        mimeTypeList.put(".pps","application/vnd.ms-powerpoint");
        mimeTypeList.put(".ppt","application/vnd.ms-powerpoint");
        mimeTypeList.put(".pptx","application/vnd.openxmlformats-officedocument.presentationml.presentation");
        mimeTypeList.put(".prop","text/plain");
        mimeTypeList.put(".rc","text/plain");
        mimeTypeList.put(".rmvb","audio/x-pn-realaudio");
        mimeTypeList.put(".rtf","application/rtf");
        mimeTypeList.put(".sh","text/plain");
        mimeTypeList.put(".tar","application/x-tar");
        mimeTypeList.put(".tgz","application/x-compressed");
        mimeTypeList.put(".txt","text/plain");
        mimeTypeList.put(".wav","audio/x-wav");
        mimeTypeList.put(".wma","audio/x-ms-wma");
        mimeTypeList.put(".wmv","audio/x-ms-wmv");
        mimeTypeList.put(".wps","application/vnd.ms-works");
        mimeTypeList.put(".xml","text/plain");
        mimeTypeList.put(".z","application/x-compress");
        mimeTypeList.put(".zip","application/x-zip-compressed");
        mimeTypeList.put("","*/*");

    }


    public FileOutputStream getFileOutputStream(Context context,String dirPath, String fileName) throws FileNotFoundException {
        String storagePath = Environment.getExternalStorageDirectory().getPath();
        String lowFileName = getLowSuffixRightFileName(fileName);
        if (dirPath.startsWith("/sdcard")){
            //非标准写法,转换为标准写法
            mainPath = storagePath + dirPath.substring(7);

        }else {
            mainPath = dirPath;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            Log.e("liyuhao","Android10以上");
            //Android10以上
            if (verifyStoragePath(mainPath)){
                //路径是起步正确的
                //Environment.DIRECTORY_SCREENSHOTS
                if (mainPath.startsWith(storagePath+android)){
                    Log.e("liyuhao","私有存储空间");
                    //进入私有存储空间
                    return new FileOutputStream(ioUnder9(mainPath,lowFileName));
                }else {
                    //进入公有存储空间
                    Log.e("liyuhao","公有存储空间");
                    String mimeType = getMimeType(lowFileName);
                    ContentValues values = new ContentValues();
                    //这里用download，其实用谁都无所谓，只是一个string
                    values.put(MediaStore.Downloads.DISPLAY_NAME, lowFileName);
                    values.put(MediaStore.Downloads.MIME_TYPE, mimeType);//MediaStore对应类型名
                    values.put(MediaStore.Downloads.RELATIVE_PATH,
                            mainPath.substring(storagePath.length()+1));//公共目录下目录名
                    Uri external = getUri(mainPath,mimeType);
                    ContentResolver resolver = context.getContentResolver();

                    Uri insertUri = resolver.insert(external, values);//使用ContentResolver创建需要操作的文件
                    if (insertUri != null) {
                        this.mainPath = getRealFilePath(context,insertUri);
                        this.fileName = getName(mainPath);
                        return (FileOutputStream) resolver.openOutputStream(insertUri);
                    }else {
                        return null;
                    }
                }
            }else {
                return null;
            }
        }else {
            //Android9以下
            Log.e("liyuhao","Android9以下");
            if (verifyStoragePath(mainPath)){
                //路径是起步正确的
                return new FileOutputStream(ioUnder9(mainPath,lowFileName));
            }else{
                //路径不正确
                return null;
            }
        }
    }

    @TargetApi(29)
    private Uri getUri(String path,String mimeType){
        String pubPath = Environment.getExternalStorageDirectory().getPath();
        if (path.startsWith(pubPath+"/"+Environment.DIRECTORY_DOWNLOADS)){
            return MediaStore.Downloads.EXTERNAL_CONTENT_URI;
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
        }
        return null;
    }

    private String getMimeType(String fileName){
        String behind;
        int  spot = fileName.lastIndexOf(".");
        if (spot == 0){
            //点在首位
            behind = fileName.substring(spot);

        }else if (spot == (fileName.length()-1)){
            //点在末尾
            behind = "";
        }else if (spot != -1){
            //点在中间
            behind = fileName.substring(spot);
        }else {
            //不存在点
            behind = "";
        }
        //如果只有.则去除后缀
        if(behind.equals(".")){
            behind = "";
        }
        //已经将文件名按照.分为2段,分别验证是否合法
        behind = syncFileName(behind,true);
        behind = behind.toLowerCase();
        String mimeType = mimeTypeList.get(behind);
        if (mimeType == null){
            mimeType = "*/*";
        }
        return mimeType;
    }

    private String syncFileName(String contnet,boolean isSuffix){
        //取得内容
        String syncContent = contnet;
        //判空,因为name有初值，所以这里判断是否存在后缀，没有就没有
        if (syncContent.equals("")){
            return syncContent;
        }

        //
        int flag = contnet.length();

        String[] patterns = new String[]{"\\","/",":","*","?","\"","<",">","|"};

        for (String pattern:patterns){
            int index = contnet.indexOf(pattern);
            if (index != -1){
                if (index < flag){
                    flag = index;
                }
            }
        }
        //取得最近非法字符index
        if (isSuffix){
            //当为后缀时,flag必然不为0
            if (flag == 1){
                //点后第一位就是非法字符
                return "";
            }
        }else {
            //当为name时
            if (flag == 0){
                //name第一位就是非法字符
                return DEAFULT_FILE_NAME;
            }
        }

        if (flag < contnet.length()){
            syncContent = syncContent.substring(0,flag);
        }

        return syncContent;

    }

    private File ioUnder9(String dirPath,String fileName){
        checkLocalFilePath(dirPath);
        //这里按道理，用10的标准，要做文件查重，先暂时搁置
        this.fileName = fileName;
        String filePath = dirPath+"/"+fileName;
        return new File(filePath);

    }

    public String createFileName(String dirPath,String fileName){
        int index = 0;
        File file = new File(dirPath);
        String newFileName = fileName;
        String front;
        String behind;
        if (file.list() != null){
            for (String string : file.list()) {
                if (new File(file.getAbsolutePath(), string).isFile()) {
                    //list.add(string);
                    Log.e("liyuhao",string);
                    String readName = getLowSuffixRightFileName(string);
                    //Log.e("liyuhao",readName);
                    if (fileName.equals(readName)){
                        //存在相同的文件名
                        index++;
                        front = getFront(newFileName);
                        behind = getBehind(newFileName);
                        if (index == 1){
                            //第一次出现重名
                            newFileName = front + " ("+index+")" + behind;
                        }else {
                            //多次出现重名
                            newFileName = front.substring(0,front.length()-2) + index+")" + behind;
                        }
                    }
                }
            }
        }

        return newFileName;
    }

    public String getFilePath(){
        return mainPath;
    }

    public String getFileName(){
        return fileName;
    }
    /**
     * 检查路径是否起码合法
     * @param path
     * @return
     */
    private static boolean verifyStoragePath(String path){
        if (path.startsWith(Environment.getExternalStorageDirectory().getPath())){
            //为正确的路径
            return true;
        }else {
            Log.e("IOUtils","绝对路径可能存在错误");
            return false;
        }
    }

    private static void checkLocalFilePath(String localFilePath) {
        File path = new File(localFilePath);
        if (!path.exists()) {
            path.mkdirs();
        }
    }

    /**
     * 通过游标得到正确的文件名
     * @param context
     * @param uri
     * @return
     */
    private String getRealFilePath(Context context,Uri uri ) {
        if ( null == uri ) return null;
        final String scheme = uri.getScheme();
        String data = null;
        if ( scheme == null )
            data = uri.getPath();
        else if ( ContentResolver.SCHEME_FILE.equals( scheme ) ) {
            data = uri.getPath();
        } else if ( ContentResolver.SCHEME_CONTENT.equals( scheme ) ) {
            Cursor cursor = context.getContentResolver().query( uri, new String[] { MediaStore.Video.VideoColumns.DATA }, null, null, null );
            if ( null != cursor ) {
                if ( cursor.moveToFirst() ) {
                    int index = cursor.getColumnIndex( MediaStore.Video.VideoColumns.DATA );
                    if ( index > -1 ) {
                        data = cursor.getString( index );
                    }
                }
                cursor.close();
            }
        }
        return data;
    }

    /**
     * 通过文件路径得到文件名
     * @param filePath
     * @return
     */
    private String getName(String filePath){
        String[] strArr = filePath.split("/");
        return strArr[strArr.length-1];
    }

    private String getLowSuffixRightFileName(String fileName){
        String front;
        String behind = "";
        int  spot = fileName.lastIndexOf(".");
        if (spot == 0){
            //点在首位
            front = DEAFULT_FILE_NAME;
            behind = fileName.substring(spot);

        }else if (spot == (fileName.length()-1)){
            //点在末尾
            front = fileName.substring(0,spot);
        }else if (spot != -1){
            //点在中间
            front = fileName.substring(0,spot);
            behind = fileName.substring(spot);
        }else {
            //不存在点
            front = fileName;
        }
        //如果只有.则去除后缀
        if(behind.equals(".")){
            behind = "";
        }
        //已经将文件名按照.分为2段,分别验证是否合法
        front = syncFileName(front,false);
        behind = syncFileName(behind,true);
        behind = behind.toLowerCase();

        return front+behind;
    }

    private String getFront(String fileName){
        int  spot = fileName.lastIndexOf(".");
        String front;
        //不要判断其他情况在这个位置
        if (spot != -1){
            //点在中间
            front = fileName.substring(0,spot);
        }else {
            //不存在点
            front = fileName;
        }
        return front;
    }

    private String getBehind(String fileName){
        int  spot = fileName.lastIndexOf(".");
        String behind;
        //不要判断其他情况在这个位置
        if (spot != -1){
            //点在中间
            behind = fileName.substring(spot);
        }else {
            //不存在点
            behind = "";
        }
        return behind;
    }
}