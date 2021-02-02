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
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
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
    public final static int OVERWRITE_FIRST = 1;
    public final static int ADD_ONLY = 2;

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
                    Uri contentUri = getUri();
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

    /**
     * 通过游标得到正确的文件路径
     * @param context
     * @param uri
     * @return
     */
    public static String getFilePath(Context context, Uri uri) {
        if ( null == uri ){
            return null;
        }
        final String scheme = uri.getScheme();
        String data = null;
        if ( scheme == null ){
            data = uri.getPath();
        } else if ( ContentResolver.SCHEME_FILE.equals( scheme ) ) {
            data = uri.getPath();
        } else if (DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                // ExternalStorageProvider
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    data = Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            } else if (isDownloadsDocument(uri)) {
                // DownloadsProvider
                final String id = DocumentsContract.getDocumentId(uri);
                if (id.startsWith("raw:")) {
                    return id.replaceFirst("raw:", "");
                }
                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),
                        Long.valueOf(id));
                data = getDataColumn(context, contentUri, null, null);
            } else if (isMediaDocument(uri)) {
                // MediaProvider
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};
                if (contentUri != null){
                    data = getDataColumn(context, contentUri, selection, selectionArgs);
                }
            }
        }else if ( ContentResolver.SCHEME_CONTENT.equals( scheme ) ) {
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

    public static FileInputStream getFileInputStream(Context context, String filePath){
        String storagePath = Environment.getExternalStorageDirectory().getPath();
        //实际测试，不管有没有权限，file.exists方法都可以使用
        File file = new File(filePath);
        if (file.exists()){
            //文件物理存在
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                //Android10以上
                if (filePath.startsWith(storagePath)){
                    //是内置存储目录
                    if (filePath.startsWith(storagePath+android)){
                        //进入私有存储空间
                        try {
                            return new FileInputStream(file);
                        }catch (FileNotFoundException e){
                            e.printStackTrace();
                            return null;
                        }

                    }else {
                        //进入共享存储空间
                        FileDetail detail = queryFile(context,filePath);
                        if (detail != null){
                            Uri contentUri = getUri();
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
                    }
                }else {
                    //路径错误或不合法
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

    public static FileInputStream getFileInputStream(Context context, Uri uri){
        FileInputStream fis = null;
        try{
            fis = (FileInputStream) context.getContentResolver().openInputStream(uri);
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }
        return fis;
    }

    public static FileOutputStream getFileOutputStream(Context context, String filePath){
        String storagePath = Environment.getExternalStorageDirectory().getPath();
        //实际测试，不管有没有权限，file.exists方法都可以使用
        File file = new File(filePath);
        if (file.exists()){
            //文件物理存在
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                if (filePath.startsWith(storagePath)){
                    //是内置存储目录
                    if (filePath.startsWith(storagePath+android)){
                        //进入私有存储空间
                        try {
                            return new FileOutputStream(file);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }else {
                        //进入共享存储空间
                        FileDetail detail = queryFile(context,filePath);
                        if (detail != null){
                            Uri contentUri = getUri();
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
                    }
                }else {
                    //路径错误或不合法
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

    public static FileOutputStream getFileOutputStream(Context context, Uri uri){
        FileOutputStream fos = null;
        try{
            fos = (FileOutputStream) context.getContentResolver().openOutputStream(uri);
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }
        return fos;
    }


    public static FileData createFileOutputStream(Context context, String dirPath, String fileName,int mod){
        if (isApkInDebug(context)){
            Log.e(TAG,"MOD仅影响在应用内部存储逻辑，公共路径永久为ADD_ONLY。");
        }
        String storagePath = Environment.getExternalStorageDirectory().getPath();
        String lowFileName = getLowSuffixRightFileName(fileName);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            //Android10以上
            if (dirPath.startsWith(storagePath)){
                //是内置存储目录
                if (dirPath.startsWith(storagePath+android)){
                    //进入私有存储空间
                    return ioUnder9(dirPath,lowFileName,mod);
                }else {
                    //进入公有存储空间
                    String mimeType = getMimeType(lowFileName);
                    ContentValues values = new ContentValues();
                    String relativePath = dirPath.substring(storagePath.length()+1);
                    //这里用download，其实用谁都无所谓，只是一个string
                    values.put(MediaStore.Downloads.DISPLAY_NAME, lowFileName);
                    values.put(MediaStore.Downloads.MIME_TYPE, mimeType);//MediaStore对应类型名
                    values.put(MediaStore.Downloads.RELATIVE_PATH, relativePath);//公共目录下目录名
                    Uri external = getUri();
                    if (external == null){
                        return null;
                    }
                    ContentResolver resolver = context.getContentResolver();

                    Uri insertUri = resolver.insert(external, values);//使用ContentResolver创建需要操作的文件
                    if (insertUri != null) {
                        FileData data = new FileData();
                        data.setFilePath(getFilePath(context,insertUri));
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
                //其他存储目录，或者路径格式不规范
                return null;
            }
        }else {
            //Android9以下
            //路径是起步正确的
            if (dirPath.startsWith(storagePath+android)){
                //进入私有存储空间
                return ioUnder9(dirPath,lowFileName,mod);
            }else {
                return ioUnder9(dirPath,lowFileName,ADD_ONLY);
            }
        }
    }


    /**
     * 删除文件
     * @param context
     * @param filePath
     * @return
     */
    public static boolean delete(Context context,String filePath){
        String storagePath = Environment.getExternalStorageDirectory().getPath();
        File file;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            if (filePath.startsWith(storagePath+android)){
                //进入私有存储空间
                file = new File(filePath);
                if (file.exists()){
                    return file.delete();
                }else {
                    return false;
                }
            }else {
                Uri external = getUri();
                ContentResolver resolver = context.getContentResolver();
                if (external != null){
                    String selection = MediaStore.Downloads.DATA + " = ?";
                    int yes = resolver.delete(
                            external,
                            selection,
                            new String[] {filePath});
                    if (yes > 0){
                        //有记录被删了
                        file = new File(filePath);
                        if (file.exists()){
                            sendSystemScanBroadcast(context,filePath);
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
            //Android9以下直接File操作删除
            file = new File(filePath);
            if (file.exists()){
                return file.delete();
            }else {
                return false;
            }
        }
    }

    public static boolean delete(Context context,Uri uri){
        String filePath = getFilePath(context,uri);
        int yes = context.getContentResolver().delete(uri, null, null);
        if (yes > 0){
            //有记录被删了
            if(TextUtils.isEmpty(filePath)){
                //可能是非媒体文件无法获取路径，只能假设删除成功
                return true;
            }
            File file = new File(filePath);
            if (file.exists()){
                sendSystemScanBroadcast(context,filePath);
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
    }

    @TargetApi(29)
    private static Uri getUri(){
        /*String pubPath = Environment.getExternalStorageDirectory().getPath();
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
        return null;*/
        return MediaStore.Files.getContentUri("external");
    }


    /**
     * 验证文件名是否存在非法字符并裁切处理
     * @param contnet
     * @param isSuffix
     * @return
     */
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

    /**
     * 创建文件流的调用放方法
     * @param dirPath
     * @param fileName
     * @param mod
     * @return
     */
    private static FileData ioUnder9(String dirPath,String fileName,int mod){
        checkLocalFilePath(dirPath);
        //这里按道理，用10的标准，要做文件查重，先暂时搁置
        FileData data = new FileData();
        switch (mod){
            case OVERWRITE_FIRST:
                data.setFileName(fileName);
                break;
            case ADD_ONLY:
                data.setFileName(createFileName(dirPath,fileName));
                break;
        }
        data.setFilePath(dirPath+"/"+data.getFileName());
        try {
            data.setFos(new FileOutputStream(new File(data.getFilePath())));
            return data;
        }catch (FileNotFoundException e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 生产一个合法的文件名前置方法
     * 这个方法是用来扫描对应文件夹路径下
     * 所有存在的文件形成列表，传给下个方法进行文件命名
     * @param dirPath
     * @param fileName
     * @return
     */
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

    /**
     * 返回一个合法没有存在的文件名
     * 如果存在a.jpg,那么返回a(1).jpg按照Android的命名逻辑
     * @param fileName
     * @param index
     * @return
     */
    private static String returnName(String fileName,int index){
        int mIndex = index;
        String front;
        String behind;
        String newFileName = fileName;

        for (String string : list) {
            String readName = getLowSuffixRightFileName(string);
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
     * 检查文件夹是否存在，不存在就创建文件夹
     * @param localFilePath
     */
    public static void checkLocalFilePath(String localFilePath) {
        File path = new File(localFilePath);
        if (!path.exists()) {
            path.mkdirs();
        }
    }

    /**
     * 获取MimeType的类型
     * @param fileName
     * @return
     */
    public static String getMimeType(String fileName){
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

    /**
     * 通过文件路径得到文件名
     * @param filePath
     * @return
     */
    public static String getFileName(String filePath){
        String[] strArr = filePath.split("/");
        return strArr[strArr.length-1];
    }

    /**
     * 将文件名后缀转换为小写，用于后续匹配mineType等操作
     * 一般文件后缀也不应为大写
     * @param fileName
     * @return
     */
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

    /**
     * 得到纯文件名
     * @param fileName
     * @return
     */
    public static String getFront(String fileName){
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

    /**
     * 得到纯后缀
     * @param fileName
     * @return
     */
    public static String getBehind(String fileName){
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
    private static FileDetail queryFile(Context context, String filePath) {
        return queryFile(context,filePath,1);
    }

    @TargetApi(29)
    private static FileDetail queryFile(Context context, String filePath,int times){
        if (times <= 2){
            //String storagePath = Environment.getExternalStorageDirectory().getPath();
            //String fileName = getFileName(filePath);
            //String lowFileName = getLowSuffixRightFileName(fileName);
            //String mainPath;
            FileDetail detail = null;

            try {
                //String displayKey = MediaStore.Images.Media.DISPLAY_NAME;
                //String pathKey = MediaStore.Images.Media.RELATIVE_PATH;
                String dataKey = MediaStore.Images.Media.DATA;
                //查询的条件语句
                //String selection = displayKey + " = ? and "+pathKey + " = ?";
                //String selection = displayKey + " = ?";
                String selection = dataKey + " = ?";
                //查询的sql
                //Uri：指向外部存储Uri
                //projection：查询那些结果
                //selection：查询的where条件
                //sortOrder：排序
                Uri uri = getUri();
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
                                    new String[]{filePath},
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
                    sendSystemScanBroadcast(context,filePath);
                    return queryFile(context,filePath,times+1);
                }else {
                    return null;
                }
            }
        }else {
            return null;
        }
    }

    /***
     * 将文件通知系统扫描，可以被整个系统检索到
     * 比如你使用File对象在公有路径新建一个图片，在系统轮询搜索之前
     * 这个文件都不会被系统收集，相册内也无法显示，使用本方法通知
     * 系统去主动收录传入文件
     * 本方法一般使用在Android9.0以下用File对象操作时才会需要
     * Android10以上一般使用Uri操作，自带此通知逻辑
     * 本框架已经对Android9.0以下进行了兼容，不需要单独调用此方法
     * @param context
     * @param filePath
     */
    public static void sendSystemScanBroadcast(Context context,String filePath) {
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

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }
    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }
    private static boolean isMediaDocument(Uri uri) {  return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }
}
