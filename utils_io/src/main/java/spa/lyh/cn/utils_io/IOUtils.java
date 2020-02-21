package spa.lyh.cn.utils_io;

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

public class IOUtils {
    private static String android = "/Android";//内部路径

    private String mainPath;
    private String fileName;


    public FileOutputStream getFileOutputStream(Context context,String dirPath, String fileName) throws FileNotFoundException {
        String storagePath = Environment.getExternalStorageDirectory().getPath();

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
                Log.e("liyuhao","内部存储");
                //Environment.DIRECTORY_SCREENSHOTS
                if (mainPath.startsWith(storagePath+android)){
                    //进入内部存储路径
                    return new FileOutputStream(ioUnder9(mainPath,fileName));
                }else {
                    //外服存储路径
                    Log.e("liyuhao","外部存储");
                    ContentValues values = new ContentValues();
                    //这里用download，其实用谁都无所谓，只是一个string
                    values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                    values.put(MediaStore.Downloads.MIME_TYPE, getMimeType(fileName));//MediaStore对应类型名
                    values.put(MediaStore.Downloads.RELATIVE_PATH,
                            mainPath.substring(storagePath.length()+1));//公共目录下目录名
                    Uri external = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;//内部存储的Download路径
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
                return new FileOutputStream(ioUnder9(mainPath,fileName));
            }else{
                //路径不正确
                return null;
            }
        }
    }

    private String getMimeType(String fileName){
        return "image/jpeg";
    }

    private File ioUnder9(String dirPath,String fileName){
        checkLocalFilePath(dirPath);
        //这里按道理，用10的标准，要做文件查重，先暂时搁置
        this.fileName = fileName;
        String filePath = dirPath+"/"+fileName;
        return new File(filePath);

    }

    public String getFilePath(){
        return mainPath + "/" + fileName;
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
}
