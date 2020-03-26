package spa.lyh.cn.utils_io;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import spa.lyh.cn.utils_io.model.FileData;
import spa.lyh.cn.utils_io.model.FileDetail;


public class IOUtils {
    private static String TAG = "IOUtils";
    private static String android = "/Android";//内部路径

    private static final String DEAFULT_FILE_NAME = "deafult";

    private static List<String> list;

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
        mimeTypeList.put(".flac","audio/quicktime");
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


    public static Uri getFileUri(Context context, String filePath){
        //实际测试，不管有没有权限，file.exists方法都可以使用
        File file = new File(filePath);
        if (file.exists()){
            //文件物理存在
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                FileDetail detail = queryFile(context,filePath);
                if (detail != null){
                    Uri contentUri = getUri(filePath,getMimeType(getFileName(filePath)));
                    if (contentUri != null){
                        return ContentUris.withAppendedId(contentUri, detail.getId());
                    }else {
                        return null;
                    }
                }else {
                    return null;
                }
            }else {
                return Uri.fromFile(file);
            }
        }else {
            return null;
        }
    }

    public static FileInputStream getFileInputStream(Context context, String filePath){
        //实际测试，不管有没有权限，file.exists方法都可以使用
        File file = new File(filePath);
        if (file.exists()){
            //文件物理存在
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                FileDetail detail = queryFile(context,filePath);
                if (detail != null){
                    Uri contentUri = getUri(filePath,getMimeType(getFileName(filePath)));
                    if (contentUri != null){
                        Uri fileUri = ContentUris.withAppendedId(contentUri, detail.getId());
                        try{
                            return (FileInputStream) context.getContentResolver().openInputStream(fileUri);
                        }catch (FileNotFoundException e){
                            e.printStackTrace();
                            return null;
                        }
                    }else {
                        return null;
                    }
                }else {
                    return null;
                }
            }else {
                try{
                    return new FileInputStream(file);
                }catch (FileNotFoundException e){
                    e.printStackTrace();
                    return null;
                }
            }
        }else {
            return null;
        }
    }

    public static FileOutputStream getFileOutputStream(Context context, String filePath){
        //实际测试，不管有没有权限，file.exists方法都可以使用
        File file = new File(filePath);
        if (file.exists()){
            //文件物理存在
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                FileDetail detail = queryFile(context,filePath);
                if (detail != null){
                    Uri contentUri = getUri(filePath,getMimeType(getFileName(filePath)));
                    if (contentUri != null){
                        Uri fileUri = ContentUris.withAppendedId(contentUri, detail.getId());
                        try{
                            return (FileOutputStream) context.getContentResolver().openOutputStream(fileUri);
                        }catch (FileNotFoundException e){
                            e.printStackTrace();
                            return null;
                        }
                    }else {
                        return null;
                    }
                }else {
                    return null;
                }
            }else {
                try {
                    return new FileOutputStream(file);
                }catch (FileNotFoundException e){
                    e.printStackTrace();
                    return null;
                }
            }
        }else {
            return null;
        }
    }


    public static FileData createFileOutputStream(Context context, String dirPath, String fileName){
        String storagePath = Environment.getExternalStorageDirectory().getPath();
        String lowFileName = getLowSuffixRightFileName(fileName);
        String mainPath;
        if (dirPath.startsWith("/sdcard")){
            //非标准写法,转换为标准写法
            mainPath = storagePath + dirPath.substring(7);

        }else {
            mainPath = dirPath;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            //Log.e("liyuhao","Android10以上");
            //Android10以上
            if (verifyStoragePath(mainPath)){
                //路径是起步正确的
                //Environment.DIRECTORY_SCREENSHOTS
                if (mainPath.startsWith(storagePath+android)){
                    //Log.e("liyuhao","私有存储空间");
                    //进入私有存储空间
                    return ioUnder9(mainPath,lowFileName);
                }else {
                    //进入公有存储空间
                    //Log.e("liyuhao","公有存储空间");
                    String mimeType = getMimeType(lowFileName);
                    ContentValues values = new ContentValues();
                    //这里用download，其实用谁都无所谓，只是一个string
                    values.put(MediaStore.Downloads.DISPLAY_NAME, lowFileName);
                    values.put(MediaStore.Downloads.MIME_TYPE, mimeType);//MediaStore对应类型名
                    values.put(MediaStore.Downloads.RELATIVE_PATH,
                            mainPath.substring(storagePath.length()+1));//公共目录下目录名
                    Uri external = getUri(mainPath,mimeType);
                    if (external == null){
                        return null;
                    }
                    ContentResolver resolver = context.getContentResolver();

                    Uri insertUri = resolver.insert(external, values);//使用ContentResolver创建需要操作的文件
                    if (insertUri != null) {
                        FileData data = new FileData();
                        data.setFilePath(getRealFilePath(context,insertUri));
                        data.setFileName(getFileName(data.getFilePath()));
                        try{
                            data.setFos((FileOutputStream) resolver.openOutputStream(insertUri));
                            return data;
                        }catch (FileNotFoundException e){
                            e.printStackTrace();
                            return null;
                        }

                    }else {
                        return null;
                    }
                }
            }else {
                return null;
            }
        }else {
            //Android9以下
            //Log.e("liyuhao","Android9以下");
            if (verifyStoragePath(mainPath)){
                //路径是起步正确的
                return ioUnder9(mainPath,lowFileName);
            }else{
                //路径不正确
                return null;
            }
        }
    }


    public static boolean delete(Context context,String filePath){
        String storagePath = Environment.getExternalStorageDirectory().getPath();
        String tempPath;
        if (filePath.startsWith("/sdcard")){
            tempPath = storagePath + filePath.substring(7);
        }else {
            tempPath = filePath;
        }
        File file;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            String dirPath = tempPath.substring(0,tempPath.lastIndexOf("/"));
            String displayName = tempPath.substring(tempPath.lastIndexOf("/")+1);
            if (verifyStoragePath(dirPath)){
                if (dirPath.startsWith(storagePath+android)){
                    //Log.e("liyuhao","私有存储空间");
                    //进入私有存储空间
                    file = new File(tempPath);
                    if (file.exists()){
                        return file.delete();
                    }else {
                        return false;
                    }
                }else {
                    Uri external = getUri(dirPath,getMimeType(displayName));
                    ContentResolver resolver = context.getContentResolver();
                    if (external != null){
                        String relativePath = dirPath.replace(storagePath+"/","") + "/";
                        int yes = resolver.delete(
                                external,
                                MediaStore.Downloads.DISPLAY_NAME + " = ? and "+MediaStore.Downloads.RELATIVE_PATH+ " = ?",
                                new String[] {displayName,relativePath});
                        if (yes > 0){
                            //有记录被删了
                            file = new File(filePath);
                            if (file.exists()){
                                fileScan(context,filePath);
                                if (isApkInDebug(context)){
                                    Log.e(TAG,"目前通过实践发现Android10系统下的某些公共文件夹，无法按照代码预期进行文件移除。"
                                    + "基本确认是Android10的BUG，Android11测试已将此问题修复。"
                                    + "如果项目实际使用中碰到了这个问题,请避免使用这个文件夹。或者引导用户手动移除该文件。");
                                }
                                return false;
                            }else {
                                return true;
                            }
                        }else {
                            return false;
                        }
                    }else {
                        return false;
                    }
                }
            }else {
                return false;
            }
        }else {
            if (verifyStoragePath(tempPath)){
                file = new File(tempPath);
                if (file.exists()){
                    return file.delete();
                }else {
                    return false;
                }
            }else {
                return false;
            }
        }
    }

    @TargetApi(29)
    private static Uri getUri(String path,String mimeType){
        String pubPath = Environment.getExternalStorageDirectory().getPath();
        if (path.startsWith(pubPath+"/"+Environment.DIRECTORY_DOWNLOADS)){
            //return MediaStore.Downloads.EXTERNAL_CONTENT_URI;//官方api竟然不好使，你敢信，你敢信？
            return MediaStore.Files.getContentUri("external");
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
        }
        return null;
    }

    private static String getMimeType(String fileName){
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

    private static String syncFileName(String contnet,boolean isSuffix){
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

    private static FileData ioUnder9(String dirPath,String fileName){
        checkLocalFilePath(dirPath);
        //这里按道理，用10的标准，要做文件查重，先暂时搁置
        FileData data = new FileData();
        data.setFileName(createFileName(dirPath,fileName));
        data.setFilePath(dirPath+"/"+data.getFileName());
        try {
            data.setFos(new FileOutputStream(new File(data.getFilePath())));
            return data;
        }catch (FileNotFoundException e){
            e.printStackTrace();
            return null;
        }
    }

    private static String createFileName(String dirPath,String fileName){
        if (list == null){
            list = new ArrayList<>();
        }else {
            list.clear();
        }
        File file = new File(dirPath);
        if (file.list() != null){
            for (String string : file.list()) {
                if (new File(file.getAbsolutePath(), string).isFile()) {
                    list.add(string);
                }
            }
        }

        if (list.size() == 0){
            return getLowSuffixRightFileName(fileName);
        }
        return returnName(fileName,0);
    }

    private static String returnName(String fileName,int index){
        //Log.e("liyuhao","开始遍历");
        int mIndex = index;
        String front;
        String behind;
        String newFileName = fileName;

        for (String string : list) {
            String readName = getLowSuffixRightFileName(string);
            //Log.e("liyuhao",readName);
            if (fileName.equals(readName)){
                //存在相同的文件名
                mIndex++;
                front = getFront(newFileName);
                behind = getBehind(newFileName);
                if (index == 0 && mIndex == 1){
                    //第一次出现重名
                    newFileName = front + " ("+mIndex+")" + behind;
                }else {
                    //多次出现重名
                    int place = newFileName.lastIndexOf("(");
                    newFileName = front.substring(0,place+1) + mIndex+")" + behind;
                }
            }
        }

        if (mIndex == index){
            return newFileName;
        }else {
            return returnName(newFileName,mIndex);
        }
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
    private static String getRealFilePath(Context context,Uri uri ) {
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
    private static String getFileName(String filePath){
        String[] strArr = filePath.split("/");
        return strArr[strArr.length-1];
    }

    private static String getLowSuffixRightFileName(String fileName){
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

    private static String getFront(String fileName){
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

    private static String getBehind(String fileName){
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

    @TargetApi(29)
    public static FileDetail queryFile(Context context, String filePath) {
        return queryFile(context,filePath,1);
    }

    @TargetApi(29)
    private static FileDetail queryFile(Context context, String filePath,int times){
        if (times <= 2){
            String storagePath = Environment.getExternalStorageDirectory().getPath();
            String fileName = getFileName(filePath);
            String lowFileName = getLowSuffixRightFileName(fileName);
            String mainPath;
            FileDetail detail = null;
            if (filePath.startsWith("/sdcard")){
                //非标准写法,转换为标准写法
                mainPath = filePath.substring(8).replace(fileName,"");

            }else {
                mainPath = filePath.replace(storagePath+"/","").replace(fileName,"");
            }
            try {
                String displayKey = MediaStore.Images.Media.DISPLAY_NAME;
                String pathKey = MediaStore.Images.Media.RELATIVE_PATH;
                //查询的条件语句
                String selection = displayKey + " = ? and "+pathKey + " = ?";
                //查询的sql
                //Uri：指向外部存储Uri
                //projection：查询那些结果
                //selection：查询的where条件
                //sortOrder：排序
                Uri uri = getUri(filePath,getMimeType(lowFileName));
                if (uri != null){
                    Cursor cursor = context
                            .getContentResolver()
                            .query(uri,
                                    new String[]{MediaStore.Images.Media._ID,
                                            MediaStore.Images.Media.DATA,
                                            MediaStore.Images.Media.MIME_TYPE,
                                            MediaStore.Images.Media.DISPLAY_NAME,
                                            MediaStore.Images.Media.TITLE,
                                            MediaStore.Images.Media.RELATIVE_PATH},
                                    selection,
                                    new String[]{lowFileName,mainPath},
                                    null);

                    //是否查询到了
                    if (cursor != null && cursor.moveToFirst()) {
                        //循环取出所有查询到的数据
                        detail = new FileDetail();
                        do {
                            //一张图片的基本信息
                            detail.setId(cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media._ID)));
                            detail.setData(cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA)));
                            detail.setMineType(cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)));
                            detail.setDisplayName(cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)));
                            detail.setTitle(cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.TITLE)));
                            detail.setRelativePath(cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)));
                            //并不会有下一次，这个方法的逻辑指定到具体文件夹下的文件检索，不会出现复数文件
                        } while (cursor.moveToNext());
                    }
                    if (cursor != null)
                        cursor.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            if (detail != null){
                return detail;
            }else {
                File file = new File(filePath);
                if (file.exists()){
                    //文件存在，但是未能检索到
                    fileScan(context,filePath);
                    return queryFile(context,filePath,times+1);
                }else {
                    return null;
                }
            }
        }else {
            return null;
        }
    }

    public static void fileScan(Context context,String filePath) {
        File file = new File(filePath);
        if (file.exists()){
            Uri data = Uri.fromFile(new File(filePath));
            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, data));
        }
    }

    /**
     * 判断当前应用是否是debug状态
     */
    private static boolean isApkInDebug(Context context) {
        try {
            ApplicationInfo info = context.getApplicationInfo();
            return (info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        } catch (Exception e) {
            return false;
        }
    }
}
