package spa.lyh.cn.utils_io;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import spa.lyh.cn.utils_io.model.FileData;


public class IOUtils {
    private static String TAG = "IOUtils";
    private static String fakeStartPath = "/sdcard/.transforms/synthetic/";
    private static String fakeStartPath2 = "/storage/emulated/0/.transforms/synthetic/";

    private static final String DEFAULT_FILE_NAME = "default";

    /**
     * 将文件路径转换为可使用的 Uri。
     *
     * <p>兼容策略（minSdk 19 起）：
     * <ul>
     *   <li>API 19–28：无分区存储，File API 可直接访问共享存储，统一返回 {@code file://}。</li>
     *   <li>API 29+（Android 10 分区存储；Android 11+ 强制生效，
     *       {@code requestLegacyExternalStorage} 已被系统忽略）：
     *       <ul>
     *         <li>应用私有目录（内部存储、{@code Android/data|media|obb/<pkg>} 等）：
     *             File API 仍可用且不在 MediaStore 索引中，返回 {@code file://}。</li>
     *         <li>共享存储（含外置 SD 卡）：优先从 MediaStore 换取 {@code content://}。</li>
     *       </ul>
     *   </li>
     * </ul>
     *
     * <p>关于 {@link File#exists()}：实测无需存储权限即可调用并反映文件是否存在，
     * 因此本方法保留它作为快速路径。但分区存储下可能出现「MediaStore 可见、
     * File API 不可见」的文件（例如刚 insert 尚未落地的记录），故
     * {@code exists()==false} 时仍会查询 MediaStore，避免误返回 null；
     * 反过来 MediaStore 未命中而 exists() 为 true 时，退回 {@code file://} 保底。
     *
     * <p>注意：返回的 {@code file://} 仅适合进程内使用（如 Glide 加载）。
     * 若需跨应用传递（{@code Intent}），请改用 {@link #getShareableUri}，
     * 否则 API 24+ 会抛 {@code FileUriExposedException}。
     *
     * @param context  上下文，不可为 null
     * @param filePath 绝对路径，支持 {@code /sdcard} 等常见别名
     * @return 可用的 Uri；无法定位到文件时返回 {@code null}
     */
    public static Uri getFileUri(Context context, String filePath){
        if (context == null || TextUtils.isEmpty(filePath)){
            return null;
        }

        //统一解析成规范绝对路径，消除 /sdcard、/mnt/sdcard、多余分隔符等差异
        //否则后续私有目录前缀判断、MediaStore 的 _data 精确匹配都会失败
        String absPath = resolveAbsolutePath(new File(filePath));

        //应用私有目录：File API 一直可用，且基本不进 MediaStore，直接 file://
        if (isAppPrivatePath(context, absPath)){
            return Uri.fromFile(new File(absPath));
        }

        //API 19–28：无分区存储，file:// 即可满足进程内使用
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
            return new File(absPath).exists() ? Uri.fromFile(new File(absPath)) : null;
        }

        //API 29+ 共享存储：以 MediaStore 的 content:// 为权威结果
        //exists() 结果不作为前置闸门，避免分区存储下误杀「MediaStore 可见」的文件
        Uri contentUri = queryMediaStoreUri(context, absPath, filePath);
        if (contentUri != null){
            return contentUri;
        }

        //兜底：MediaStore 未收录但 File API 仍可见（刚写入尚未入库、OEM 虚拟路径等）
        //此 file:// 只保证进程内可用
        if (new File(absPath).exists()){
            return Uri.fromFile(new File(absPath));
        }
        return null;
    }

    /**
     * 获取**可跨应用分享**的 Uri（{@code content://}）。
     *
     * <p>与 {@link #getFileUri} 的分工：
     * <ul>
     *   <li>{@link #getFileUri}：进程内使用（Glide、自己读写），可能返回 {@code file://}；</li>
     *   <li>本方法：塞进 {@code Intent} 给别的 App 用，必须是 {@code content://}。</li>
     * </ul>
     *
     * <p>策略（minSdk 19 起）：
     * <ol>
     *   <li>API 29+ 共享存储：优先 MediaStore 的 {@code content://}（系统本就可授权）；</li>
     *   <li>其余可读到的本地文件：{@code FileProvider.getUriForFile}；</li>
     *   <li>FileProvider 不在配置根内等失败时：API &lt; 24 退回 {@code file://}（老系统可直传），
     *       API 24+ 仍退回 {@code file://} 但**仅供进程内**，调用方须知。</li>
     * </ol>
     *
     * <p>分享时请加临时读权限：
     * <pre>
     * Uri uri = IOUtils.getShareableUri(context, path);
     * Intent intent = new Intent(Intent.ACTION_VIEW);
     * intent.setDataAndType(uri, "application/pdf");
     * intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
     * </pre>
     *
     * @param context  上下文，不可为 null
     * @param filePath 绝对路径，支持 {@code /sdcard} 等常见别名
     * @return 可分享的 Uri；文件不存在或无法定位时返回 {@code null}
     */
    public static Uri getShareableUri(Context context, String filePath){
        if (context == null || TextUtils.isEmpty(filePath)){
            return null;
        }

        String absPath = resolveAbsolutePath(new File(filePath));
        File file = new File(absPath);

        //1) API 29+ 共享存储：MediaStore 的 content:// 可直接跨应用授权
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && !isAppPrivatePath(context, absPath)){
            Uri mediaUri = queryMediaStoreUri(context, absPath, filePath);
            if (mediaUri != null){
                return mediaUri;
            }
        }

        //2) 本地文件（私有目录、Q- 共享、未入库文件）：FileProvider
        if (file.exists()){
            try {
                return FileProvider.getUriForFile(
                        context,
                        getFileProviderAuthority(context),
                        file);
            } catch (Exception e) {
                //路径不在 file_paths 配置内等，走兜底
                e.printStackTrace();
            }

            //3) 兜底 file://：API < 24 可跨应用；24+ 仅进程内使用
            return Uri.fromFile(file);
        }
        return null;
    }

    /**
     * 与 Manifest 中 FileProvider 的 authorities 对应。
     * 必须是 {@code ${applicationId}.utils_io.fileprovider}。
     */
    private static String getFileProviderAuthority(Context context){
        return context.getPackageName() + ".utils_io.fileprovider";
    }

    /**
     * 尽力把 Uri 反查成文件系统绝对路径。
     *
     * <p>按 scheme 分发：
     * <ul>
     *   <li>{@code file://} / 无 scheme：直接取 path；</li>
     *   <li>SAF document Uri：解析 ExternalStorage / Downloads / Media 三大 Provider；</li>
     *   <li>其他 {@code content://}：走游标查 {@code _data} 列。</li>
     * </ul>
     *
     * <p>注意（分区存储现实）：{@code MediaColumns._data} 自 API 29 起已 deprecated，
     * 且很多 content Uri（网盘、SAF 临时文件）**本来就没有**文件系统路径。
     * 此时返回 {@code null} 是正常结果，不代表方法坏了；
     * Android 10+ 优先直接拿 Uri 用 {@code ContentResolver} 读写，而不是反查路径。
     *
     * @param context 上下文，不可为 null
     * @param uri     待反查的 Uri
     * @return 文件绝对路径；无法解析时返回 {@code null}（不会抛异常）
     */
    public static String getFilePath(Context context, Uri uri) {
        if (context == null || uri == null){
            return null;
        }

        String scheme = uri.getScheme();

        //file:// 或无 scheme：getPath() 已做解码，直接用
        if (scheme == null || ContentResolver.SCHEME_FILE.equals(scheme)){
            return uri.getPath();
        }

        //SAF 文档 Uri（DocumentsProvider）
        if (DocumentsContract.isDocumentUri(context, uri)){
            try {
                return getDocumentPath(context, uri);
            } catch (Exception e) {
                //文档 id 格式随系统版本变化，解析失败不应让调用方崩溃
                e.printStackTrace();
                return null;
            }
        }

        //其余 content://：对 Uri 本身查 _data
        if (ContentResolver.SCHEME_CONTENT.equals(scheme)){
            return getDataColumn(context, uri, null, null);
        }

        return null;
    }

    /**
     * 按文件路径打开输入流。
     *
     * <p>打开策略（minSdk 19 起）：
     * <ol>
     *   <li>File API 能直接打开就优先走 {@link FileInputStream}——
     *       私有目录、以及分区存储下 FUSE/sdcardfs 已放行的共享存储文件，
     *       {@code exists()} 为 true 时必可读，比绕道 MediaStore 更快且不依赖索引；</li>
     *   <li>File API 打不开时（如 MediaStore 可见而 stat 不可见），
     *       再用 MediaStore 换 {@code content://} 打开（仅 API 29+）。</li>
     * </ol>
     *
     * <p>返回类型是 {@link InputStream} 而不是 {@link FileInputStream}：
     * {@code ContentResolver.openInputStream()} 的契约返回值就是 {@code InputStream}，
     * 其底层实现虽多数是 {@code FileInputStream} 子类，但不是 API 保证，强转会埋雷。
     *
     * @param context  上下文
     * @param filePath 绝对路径，支持 {@code /sdcard} 等常见别名
     * @return 输入流；无法打开时返回 {@code null}（不会抛异常）
     */
    public static InputStream openInputStream(Context context, String filePath){
        if (context == null || TextUtils.isEmpty(filePath)){
            return null;
        }

        String absPath = resolveAbsolutePath(new File(filePath));
        File file = new File(absPath);

        //1) File API 快速路径：能 stat 到就直接开文件流
        if (file.exists()){
            InputStream in = openFileQuietly(file);
            if (in != null){
                return in;
            }
            //存在但打不开（是目录、权限被回收等），继续尝试 MediaStore
        }

        //2) MediaStore 兜底：File API 不可见、但媒体库有记录的文件
        //   API 19-28 无分区存储，到不了这一步
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            Uri contentUri = queryMediaStoreUri(context, absPath, filePath);
            if (contentUri != null){
                return openUriQuietly(context, contentUri);
            }
        }
        return null;
    }

    /**
     * 打开 Uri 对应的输入流（content:// / file:// 均可）。
     *
     * @return 输入流；无法打开时返回 {@code null}（不会抛异常）
     */
    public static InputStream openInputStream(Context context, Uri uri){
        if (context == null || uri == null){
            return null;
        }
        return openUriQuietly(context, uri);
    }

    /**
     * 按文件路径打开输出流（覆盖写）。
     * 等价于 {@code openOutputStream(context, filePath, false)}。
     *
     * @param context  上下文
     * @param filePath 绝对路径，支持 {@code /sdcard} 等常见别名；文件不存在时会创建
     * @return 输出流；无法打开时返回 {@code null}（不会抛异常）
     */
    public static OutputStream openOutputStream(Context context, String filePath){
        return openOutputStream(context, filePath, false);
    }

    /**
     * 按文件路径打开输出流。
     *
     * <p>语义对齐 {@link FileOutputStream}：
     * <ul>
     *   <li>父目录不存在时自动 {@code mkdirs}；</li>
     *   <li>文件不存在时创建；</li>
     *   <li>{@code append=false} 覆盖写，{@code append=true} 追加写（保留旧内容）。</li>
     * </ul>
     *
     * <p>打开策略（minSdk 19 起）：
     * <ol>
     *   <li>File API 优先——私有目录、以及分区存储下已放行的媒体文件都能直接写，
     *       顺便自然支持「创建新文件」；</li>
     *   <li>File API 打不开时（如共享存储非媒体文件且 File 不可见），
     *       退化为从 MediaStore 换 {@code content://} 打开（仅 API 29+）。
     *       注意：此路径只能打开**媒体库已有记录**的文件，新建共享存储文件
     *       请用 {@link #createOutputStream}。</li>
     * </ol>
     *
     * <p>返回类型是 {@link OutputStream} 而不是 {@link FileOutputStream}：
     * {@code ContentResolver.openOutputStream()} 的契约返回值就是 {@code OutputStream}，
     * 不做强转，避免依赖实现细节。
     *
     * @param context  上下文
     * @param filePath 绝对路径
     * @param append   {@code true} 追加，{@code false} 覆盖
     * @return 输出流；无法打开时返回 {@code null}（不会抛异常）
     */
    public static OutputStream openOutputStream(Context context, String filePath, boolean append){
        if (context == null || TextUtils.isEmpty(filePath)){
            return null;
        }

        String absPath = resolveAbsolutePath(new File(filePath));
        File file = new File(absPath);

        //父目录不存在则创建，对齐 FileOutputStream「不存在就建」的语义
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()){
            parent.mkdirs();
        }

        //1) File API 快速路径：可写则直接开，能顺带创建新文件
        OutputStream out = openFileOutputStreamQuietly(file, append);
        if (out != null){
            return out;
        }

        //2) MediaStore 兜底：File API 不可见、但媒体库已有记录的文件
        //   私有目录一定在上面就成功了，不会走到这里
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && !isAppPrivatePath(context, absPath)){
            Uri contentUri = queryMediaStoreUri(context, absPath, filePath);
            if (contentUri != null){
                return openUriOutputStreamQuietly(context, contentUri, append);
            }
        }
        return null;
    }

    /**
     * 打开 Uri 对应的输出流（覆盖写）。
     * 等价于 {@code openOutputStream(context, uri, false)}。
     *
     * @return 输出流；无法打开时返回 {@code null}（不会抛异常）
     */
    public static OutputStream openOutputStream(Context context, Uri uri){
        return openOutputStream(context, uri, false);
    }

    /**
     * 打开 Uri 对应的输出流。
     *
     * @param context 上下文
     * @param uri     目标 Uri（content:// / file:// 均可），须已存在
     * @param append  {@code true} 追加，{@code false} 覆盖
     * @return 输出流；无法打开时返回 {@code null}（不会抛异常）
     */
    public static OutputStream openOutputStream(Context context, Uri uri, boolean append){
        if (context == null || uri == null){
            return null;
        }
        return openUriOutputStreamQuietly(context, uri, append);
    }


    /**
     * 在指定目录创建文件并打开输出流（覆盖优先）。
     * 等价于 {@code createOutputStream(context, dirPath, fileName, false)}。
     *
     * @return 文件信息；失败返回 {@code null}
     */
    public static FileData createOutputStream(Context context, String dirPath, String fileName){
        return createOutputStream(context, dirPath, fileName, false);
    }

    /**
     * 在指定目录创建文件并打开输出流。
     *
     * <p>命名规则与系统一致：{@code foo.jpg} 被占用时生成 {@code foo (1).jpg}、{@code foo (2).jpg} …
     * 返回的 {@link FileData#getFileName()} 是**最终落盘名**——若与请求名不同，
     * 说明覆盖失败、已按 add 逻辑另存（或 MediaStore 自动改名），调用方以此为准。
     *
     * <p>行为：
     * <ul>
     *   <li>{@code addOnly=false}（默认）：优先覆写同名文件；
     *       无权限 / 打不开时转为 add（新建带序号文件），不丢数据；
     *       debug 包会打日志说明无法覆盖的原因与最终文件名；</li>
     *   <li>{@code addOnly=true}：不碰已有文件，直接走 add（原名未被占用时仍用原名）。</li>
     * </ul>
     *
     * <p>实现路径（minSdk 19 起）：
     * <ul>
     *   <li>应用私有目录、API 19–28：File API（原子 {@code createNewFile} 防重名）；</li>
     *   <li>API 29+ 共享存储：MediaStore。覆写走 {@code openOutputStream(uri, "w")}，
     *       新建走 {@code insert}（重名由 MediaProvider 自动唯一化）。</li>
     * </ul>
     *
     * @param context  上下文
     * @param dirPath  目标目录（不存在会尝试创建）
     * @param fileName 期望文件名，非法字符会被清洗，后缀转小写
     * @param addOnly  {@code true} 只新增不覆盖；{@code false} 覆盖优先
     * @return 含最终文件名、路径与输出流的 {@link FileData}；失败返回 {@code null}
     */
    public static FileData createOutputStream(Context context, String dirPath, String fileName, boolean addOnly){
        if (context == null || TextUtils.isEmpty(dirPath) || TextUtils.isEmpty(fileName)){
            return null;
        }

        //清洗非法字符、剥路径、后缀小写（内部保证非空）
        String safeName = getLowSuffixRightFileName(fileName);

        String absDir = resolveAbsolutePath(new File(dirPath));

        //API 29+ 共享存储走 MediaStore；私有目录 / 低版本走 File
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && !isAppPrivatePath(context, absDir)){
            return createViaMediaStore(context, absDir, safeName, addOnly);
        }
        return createViaFile(context, new File(absDir), safeName, addOnly);
    }

    /**
     * 按文件路径删除文件。
     *
     * <p>策略（minSdk 19 起）：
     * <ul>
     *   <li>应用私有目录、API 19–28：{@link File#delete()} 物理删除；</li>
     *   <li>API 29+ 共享存储：先在 MediaStore 定位，再 {@code ContentResolver.delete}。
     *       <b>以 delete 返回值为准</b>，不再用 {@code File.exists()} 反推成败——
     *       分区存储下 File API 与媒体库视图并不同步，旧写法在 Android 10 上
     *       容易把「记录已删、FUSE 尚有缓存」误判成删除失败。</li>
     * </ul>
     *
     * <p>返回 {@code true} 表示「对用户可见的这条文件记录已移除」；
     * 若磁盘因厂商策略残留实体文件，不影响返回值（会再尝试 {@code File.delete()} 清理）。
     *
     * @param context  上下文
     * @param filePath 绝对路径，支持 {@code /sdcard} 等常见别名
     * @return {@code true} 删除成功；{@code false} 未找到 / 无权限 / 删除失败
     */
    public static boolean delete(Context context, String filePath){
        if (context == null || TextUtils.isEmpty(filePath)){
            return false;
        }

        String absPath = resolveAbsolutePath(new File(filePath));
        File file = new File(absPath);

        //私有目录、低版本：File API 即可，物理删除
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                || isAppPrivatePath(context, absPath)){
            return deleteFileQuietly(file);
        }

        //API 29+ 共享存储：以 MediaStore 为准
        Uri mediaUri = queryMediaStoreUri(context, absPath, filePath);
        if (mediaUri != null){
            int rows = deleteUriQuietly(context, mediaUri);
            if (rows > 0){
                //记录已删。若厂商 ROM 留下实体文件，补一刀，不影响返回值
                if (file.exists()){
                    deleteFileQuietly(file);
                }
                return true;
            }
            //有记录但删不掉（无权限删他人文件等）
            return false;
        }

        //媒体库中没有：可能是未入库的文件，仍尝试 File 删除
        //（自己刚写的、或 OEM 虚拟路径等）
        return deleteFileQuietly(file);
    }

    /**
     * 按 Uri 删除文件 / 媒体库记录。
     *
     * <p>以 {@code ContentResolver.delete} 的返回值为准。
     * {@code file://} 走 File 删除。不再用 {@code File.exists()} 验收
     * （网盘等 provider 本就没有文件系统路径，分区存储下 exists 也不可信）。
     *
     * @return {@code true} 删除成功；{@code false} 失败或记录不存在
     */
    public static boolean delete(Context context, Uri uri){
        if (context == null || uri == null){
            return false;
        }

        //file:// 直接物理删除
        if (ContentResolver.SCHEME_FILE.equals(uri.getScheme()) || uri.getScheme() == null){
            String path = uri.getPath();
            if (TextUtils.isEmpty(path)){
                return false;
            }
            return deleteFileQuietly(new File(resolveAbsolutePath(new File(path))));
        }

        //content:// / documents 等：信任 provider
        return deleteUriQuietly(context, uri) > 0;
    }

    /**
     * 获取文件大小（字节）。
     *
     * <p>分情况取值（效率与覆盖兼顾）：
     * <ol>
     *   <li>File API 能 {@code stat} 到（私有目录、已放行的媒体等）→ {@link File#length()}，本地调用最快；</li>
     *   <li>File 不可见时（分区存储下的非媒体文件等）→ 转 MediaStore 查 {@code SIZE} 列。</li>
     * </ol>
     *
     * <p>注意：不要用 {@code InputStream.available()} 当文件大小，那只是缓冲估计值。
     *
     * @param context  上下文
     * @param filePath 绝对路径，支持 {@code /sdcard} 等常见别名
     * @return 文件字节数；不存在、无权限或无法确定时返回 {@code -1}
     *         （{@code 0} 是合法的空文件大小，故未知不用 0 表示）
     */
    public static long getFileSize(Context context, String filePath){
        if (TextUtils.isEmpty(filePath)){
            return -1;
        }
        String absPath = resolveAbsolutePath(new File(filePath));
        File file = new File(absPath);

        //1) File API 快速路径
        //   length() 在无权限/不可见时返回 0，因此必须先 exists() 且 >0 才敢信
        //   真正的空文件会落到下一步 MediaStore，拿到真实 0
        if (file.exists()){
            long len = file.length();
            if (len > 0){
                return len;
            }
        }

        //2) MediaStore 兜底（API 29+ 共享存储才有媒体库记录；私有目录查不到属正常）
        if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && !isAppPrivatePath(context, absPath)){
            Uri mediaUri = queryMediaStoreUri(context, absPath, filePath);
            if (mediaUri != null){
                return getFileSize(context, mediaUri);
            }
        }

        //File 能看见但是空文件（exists && length==0）
        if (file.exists() && file.isFile()){
            return file.length(); // 0
        }
        return -1;
    }

    /**
     * 获取 Uri 对应文件大小（字节）。
     * 优先查 {@link MediaStore.MediaColumns#SIZE}，再退回 {@code AssetFileDescriptor.getLength()}。
     *
     * @return 字节数；未知或无权限时返回 {@code -1}
     */
    public static long getFileSize(Context context, Uri uri){
        if (context == null || uri == null){
            return -1;
        }

        //file:// 不用走 Provider
        if (ContentResolver.SCHEME_FILE.equals(uri.getScheme()) || uri.getScheme() == null){
            String path = uri.getPath();
            return TextUtils.isEmpty(path) ? -1 : getFileSize(context, path);
        }

        //1) 查 SIZE 列
        long size = querySizeColumn(context, uri);
        if (size >= 0){
            return size;
        }

        //2) AssetFileDescriptor：部分 provider 不回 SIZE 列但能给长度
        AssetFileDescriptor fd = null;
        try {
            fd = context.getContentResolver().openAssetFileDescriptor(uri, "r");
            if (fd != null){
                long length = fd.getLength();
                return length >= 0 ? length : -1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fd != null){
                try {
                    fd.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return -1;
    }

    /**
     * 查 MediaStore 的 SIZE 列；缺失或异常返回 -1。
     */
    private static long querySizeColumn(Context context, Uri uri){
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    uri,
                    new String[]{MediaStore.MediaColumns.SIZE},
                    null, null, null);
            if (cursor != null && cursor.moveToFirst()){
                int index = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE);
                if (index >= 0 && !cursor.isNull(index)){
                    return cursor.getLong(index);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null){
                cursor.close();
            }
        }
        return -1;
    }

    /**
     * 确保目录存在且确为目录。
     *
     * @param localFilePath 目录路径
     * @return {@code true} 目录可用；入参为空、路径被同名文件占用、创建失败时 {@code false}
     */
    public static boolean checkLocalFilePath(String localFilePath) {
        if (TextUtils.isEmpty(localFilePath)){
            return false;
        }
        File path = new File(localFilePath);
        if (path.exists()){
            //已存在但可能是同名文件
            return path.isDirectory();
        }
        return path.mkdirs();
    }

    /**
     * 获取文件名对应的 MIME 类型。
     * 使用系统 {@link MimeTypeMap} 查询；未收录的后缀返回通配类型。
     *
     * @param fileName 文件名或路径（只看后缀）
     * @return MIME 类型，不会为 null
     */
    public static String getMimeType(String fileName){
        if (TextUtils.isEmpty(fileName)){
            return "*/*";
        }
        String behind = getBehind(fileName); // ".jpg" 或 ""
        if (TextUtils.isEmpty(behind)){
            return "*/*";
        }
        String ext = behind.substring(1).toLowerCase(Locale.US); // "jpg"
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        return !TextUtils.isEmpty(mimeType) ? mimeType : "*/*";
    }

    /**
     * 从路径取出文件名。
     * 自动忽略尾部斜杠：{@code "Download/a.txt/"} → {@code "a.txt"}。
     *
     * @param filePath 文件路径
     * @return 文件名；入参为空返回 {@code null}
     */
    public static String getFileName(String filePath){
        if (TextUtils.isEmpty(filePath)){
            return null;
        }
        //去掉尾部斜杠
        int end = filePath.length();
        while (end > 0 && filePath.charAt(end - 1) == '/'){
            end--;
        }
        if (end == 0){
            return null;
        }
        int slash = filePath.lastIndexOf('/', end - 1);
        return filePath.substring(slash + 1, end);
    }

    /***
     * 通知系统媒体库扫描该文件，使其可被相册 / 全局检索到。
     *
     * <p>主要使用场景是 **API 29 以下** 用 File API 在公共目录写入后的补登记。
     * Android 10+ 走 MediaStore 写入会自动入库，通常无需调用；
     * 旧广播 {@code ACTION_MEDIA_SCANNER_SCAN_FILE} 在高版本上常被忽略，
     * 这里统一改用 {@link MediaScannerConnection#scanFile}。
     *
     * @param context  上下文
     * @param filePath 待扫描的文件路径
     */
    public static void sendSystemScanBroadcast(Context context, String filePath) {
        if (context == null || TextUtils.isEmpty(filePath)){
            return;
        }
        File file = new File(filePath);
        if (!file.exists()){
            return;
        }
        MediaScannerConnection.scanFile(
                context,
                new String[]{file.getAbsolutePath()},
                null,
                null);
    }

    /**
     * 得到主名（不含扩展名的点）。
     * 约定：扩展名仅当点不在首位且不在末位时才存在。
     * <ul>
     *   <li>{@code foo.jpg} → {@code foo}</li>
     *   <li>{@code .gitignore} → {@code .gitignore}（隐藏文件整体是主名）</li>
     *   <li>{@code a.b.txt} → {@code a.b}</li>
     *   <li>{@code foo.} → {@code foo}</li>
     * </ul>
     *
     * @param fileName 文件名
     * @return 主名；入参为 null 时返回空串
     */
    public static String getFront(String fileName){
        return splitBaseExt(fileName)[0];
    }

    /**
     * 得到扩展名（含点；无扩展名时为空串）。
     * 与 {@link #getFront(String)} 使用同一套切分约定。
     * <ul>
     *   <li>{@code foo.jpg} → {@code .jpg}</li>
     *   <li>{@code .gitignore} → {@code ""}（隐藏文件无扩展名）</li>
     *   <li>{@code foo.} → {@code ""}（孤点不算扩展名）</li>
     * </ul>
     *
     * @param fileName 文件名
     * @return 扩展名（含点）或空串；入参为 null 时返回空串
     */
    public static String getBehind(String fileName){
        return splitBaseExt(fileName)[1];
    }


    /**
     * 去掉 Android / Linux 文件名中真正非法的字符。
     *
     * <p>Linux 文件系统只禁止 {@code /} 与 NUL（{@code '\0'}）；
     * Windows 所谓的非法字符 {@code : * ? " < > | \} 在 Android 上都是合法的，
     * 不应裁掉（例如 {@code 12:30.txt}）。
     *
     * <p>采取「删除非法字符」而不是「从非法字符起截断」，避免误伤正常名字。
     */
    private static String sanitizeFileNamePart(String content){
        if (content == null || content.length() == 0){
            return content == null ? "" : content;
        }
        StringBuilder sb = new StringBuilder(content.length());
        for (int i = 0; i < content.length(); i++){
            char c = content.charAt(i);
            if (c == '/' || c == '\0'){
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * 清洗文件名：先剥掉路径段，再去掉非法字符，后缀转小写。
     * 与 {@link #getFront(String)} / {@link #getBehind(String)} 共用同一套切分约定，
     * 隐藏文件（如 {@code .gitignore}）整体作为主名保留。
     * 保证返回非空（清洗后为空则用 {@link #DEFAULT_FILE_NAME}）。
     */
    private static String getLowSuffixRightFileName(String fileName){
        if (TextUtils.isEmpty(fileName)){
            return DEFAULT_FILE_NAME;
        }
        //只取最后一段，避免 "subdir/a.jpg" 把路径当文件名清洗
        String name = getFileName(fileName);
        if (TextUtils.isEmpty(name)){
            return DEFAULT_FILE_NAME;
        }
        String front = sanitizeFileNamePart(getFront(name));
        String behind = sanitizeFileNamePart(getBehind(name));
        if (TextUtils.isEmpty(front)){
            front = DEFAULT_FILE_NAME;
        }
        return front + behind.toLowerCase(Locale.US);
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
        return uri != null
                && "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return uri != null
                && "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return uri != null
                && "com.android.providers.media.documents".equals(uri.getAuthority());
    }
    /**
     * 按 _data 列从 ContentProvider 反查绝对路径。
     * 查询失败（无权限、列不存在、provider 不支持）一律视为查不到，返回 null。
     */
    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        if (context == null || uri == null){
            return null;
        }
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    uri,
                    new String[]{MediaStore.MediaColumns.DATA},
                    selection,
                    selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()){
                //用 getColumnIndex 而不是 OrThrow：列可能被 provider 裁掉
                int columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
                if (columnIndex >= 0){
                    return cursor.getString(columnIndex);
                }
            }
        } catch (Exception e) {
            //SecurityException / IllegalArgumentException 等：对工具方法都降级为 null
            e.printStackTrace();
        } finally {
            if (cursor != null){
                cursor.close();
            }
        }
        return null;
    }


    /**
     * 是否落在内部存储的本应用目录下。
     * 要求 pkg 必须是完整路径段，避免 com.foo 误匹配 com.foo.bar。
     */
    private static boolean isInternalAppPath(String path, String pkg){
        if (path.startsWith("/data/data/" + pkg + "/")){
            return true;
        }
        //多用户：/data/user/<userId>/<pkg>/... 与 /data/user_de/<userId>/<pkg>/...
        String[] prefixes = new String[]{"/data/user/", "/data/user_de/"};
        for (String prefix : prefixes){
            if (!path.startsWith(prefix)){
                continue;
            }
            int userEnd = path.indexOf('/', prefix.length());
            if (userEnd < 0){
                continue;
            }
            String rest = path.substring(userEnd + 1);
            if (rest.equals(pkg) || rest.startsWith(pkg + "/")){
                return true;
            }
        }
        return false;
    }

    /**
     * absPath 是否位于 dir（或就是 dir 本身）之下。
     */
    private static boolean isUnderFile(String absPath, File dir){
        if (absPath == null || dir == null){
            return false;
        }
        String dirPath = resolveAbsolutePath(dir);
        return absPath.equals(dirPath) || absPath.startsWith(dirPath + "/");
    }

    /**
     * 在 MediaStore 中按绝对路径查找文件，返回 content:// Uri。
     *
     * <p>选表规则按「媒体类型」而不是所在文件夹：
     * MediaStore 的 Images/Video/Audio/Files 是按 MIME 分表的，
     * DCIM 里的 txt、Pictures 里的 pdf 都不在 Images 表里。
     * Download 目录另有 Downloads 表（API 29+）。
     *
     * @param absPath 规范绝对路径，用于和 _data 列精确匹配
     * @param rawPath 调用方原始路径；少数 ROM 的 _data 记录别名路径时兜底再查一次
     */
    @TargetApi(29)
    private static Uri queryMediaStoreUri(Context context, String absPath, String rawPath){
        List<Uri> candidates = getCandidateCollections(absPath);
        //先用规范路径匹配
        for (Uri collection : candidates){
            Long id = queryIdByData(context, collection, absPath);
            if (id != null){
                return ContentUris.withAppendedId(collection, id);
            }
        }
        //规范路径未命中时，再用原始入参试一次（应对 _data 记录别名路径的 ROM）
        if (rawPath != null && !rawPath.equals(absPath)){
            for (Uri collection : candidates){
                Long id = queryIdByData(context, collection, rawPath);
                if (id != null){
                    return ContentUris.withAppendedId(collection, id);
                }
            }
        }
        return null;
    }

    /**
     * 生成待查询的 MediaStore 集合，按命中概率排序并去重。
     */
    @TargetApi(29)
    private static List<Uri> getCandidateCollections(String absPath){
        List<Uri> candidates = new ArrayList<>();
        String mimeType = getMimeType(absPath);
        String volume = resolveMediaVolume(absPath);

        //1) 媒体文件的权威表由 MIME 决定
        if (mimeType != null && mimeType.startsWith("image/")){
            addUri(candidates, MediaStore.Images.Media.getContentUri(volume));
            addUri(candidates, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        } else if (mimeType != null && mimeType.startsWith("video/")){
            addUri(candidates, MediaStore.Video.Media.getContentUri(volume));
            addUri(candidates, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        } else if (mimeType != null && mimeType.startsWith("audio/")){
            addUri(candidates, MediaStore.Audio.Media.getContentUri(volume));
            addUri(candidates, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
        }

        //2) Download 目录的非媒体文件在 Downloads 表（API 29+）
        //   其中的媒体文件仍以 Images/Video/Audio 为准，所以放在 MIME 之后
        String pubPath = Environment.getExternalStorageDirectory().getPath();
        String downloadsRoot = pubPath + "/" + Environment.DIRECTORY_DOWNLOADS;
        if (absPath.equals(downloadsRoot) || absPath.startsWith(downloadsRoot + "/")){
            addUri(candidates, MediaStore.Downloads.EXTERNAL_CONTENT_URI);
        }

        //3) 其余非媒体文件在 Files 表；同时作为最终兜底
        addUri(candidates, MediaStore.Files.getContentUri(volume));
        addUri(candidates, MediaStore.Files.getContentUri("external"));
        return candidates;
    }

    /**
     * 解析路径所在存储卷的 MediaStore volume 名。
     * 主卷返回 external_primary；外置 SD 卡返回形如 ABCD-1234 的卷名。
     */
    @TargetApi(29)
    private static String resolveMediaVolume(String absPath){
        String primaryRoot = Environment.getExternalStorageDirectory().getAbsolutePath();
        if (absPath.equals(primaryRoot) || absPath.startsWith(primaryRoot + "/")){
            return MediaStore.VOLUME_EXTERNAL_PRIMARY;
        }
        //外置卷：/storage/<volumeName>/...
        if (absPath.startsWith("/storage/")){
            String rest = absPath.substring("/storage/".length());
            int slash = rest.indexOf('/');
            String head = slash >= 0 ? rest.substring(0, slash) : rest;
            //过滤 emulated、self 等非卷名目录
            if (head.length() > 0
                    && !"emulated".equals(head)
                    && !"self".equals(head)
                    && !"sdcardfs".equals(head)){
                return head;
            }
        }
        return MediaStore.VOLUME_EXTERNAL_PRIMARY;
    }

    /**
     * 在指定集合中按 _data = absPath 查 MediaStore 的 _id。
     * MediaColumns.DATA 虽已 deprecated，但仍是按绝对路径检索最直接可靠的方式。
     *
     * @return 命中返回 _id；未命中或异常返回 null
     */
    private static Long queryIdByData(Context context, Uri collection, String absPath){
        if (context == null || collection == null || TextUtils.isEmpty(absPath)){
            return null;
        }
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    collection,
                    new String[]{MediaStore.MediaColumns._ID},
                    MediaStore.MediaColumns.DATA + " = ?",
                    new String[]{absPath},
                    null);
            if (cursor != null && cursor.moveToFirst()){
                //_id 在 MediaStore 中是 long，避免用 getInt 截断
                return cursor.getLong(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null){
                cursor.close();
            }
        }
        return null;
    }

    /**
     * 去重追加 Uri，保持候选顺序。
     */
    private static void addUri(List<Uri> list, Uri uri){
        if (uri != null && !list.contains(uri)){
            list.add(uri);
        }
    }

    /**
     * 把任意写法的路径解析成规范绝对路径。
     * {@code /sdcard}、{@code /mnt/sdcard}、{@code /storage/self/primary}
     * 等符号链接会被解析为 {@code /storage/emulated/0} 这类真实路径，
     * 保证后续 startsWith 判断与 MediaStore 的 _data 列能对上。
     */
    private static String resolveAbsolutePath(File file){
        if (file == null){
            return null;
        }
        try {
            //getCanonicalPath 不要求文件真实存在，仍会解析父级符号链接
            return file.getCanonicalPath();
        } catch (IOException e) {
            //路径含非法字符等极端情况退回绝对路径
            return file.getAbsolutePath();
        } catch (SecurityException e) {
            return file.getAbsolutePath();
        }
    }

    /**
     * 判断规范绝对路径是否位于应用私有存储。
     * 这类目录 File API 始终可用，且不会进入 MediaStore 索引，
     * 因此应返回 file:// 而不是 content://。
     *
     * <p>注意：只认「本应用」的私有目录。原实现用
     * {@code startsWith(storagePath + "/Android")} 过宽，
     * 会把 AndroidX、AndroidDev 等普通目录也当成私有目录。
     */
    private static boolean isAppPrivatePath(Context context, String absPath){
        if (TextUtils.isEmpty(absPath)){
            return false;
        }
        //补齐尾部斜杠，保证前缀比较落在目录边界上，避免 com.foo 命中 com.foo.bar
        String path = absPath.endsWith("/") ? absPath : absPath + "/";
        String pkg = context.getPackageName();

        //1) 内部存储：/data/data/<pkg>/、/data/user/N/<pkg>/、/data/user_de/N/<pkg>/
        if (isInternalAppPath(path, pkg)){
            return true;
        }

        //2) 外部 app-specific 目录（任意存储卷根下都可能出现）
        if (path.contains("/Android/data/" + pkg + "/")
                || path.contains("/Android/media/" + pkg + "/")
                || path.contains("/Android/obb/" + pkg + "/")){
            return true;
        }

        //3) Context 明确持有的目录兜底（覆盖多用户、多卷、自定义目录等）
        if (isUnderFile(absPath, context.getFilesDir())
                || isUnderFile(absPath, context.getCacheDir())
                || isUnderFile(absPath, context.getExternalFilesDir(null))
                || isUnderFile(absPath, context.getExternalCacheDir())){
            return true;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            //getNoBackupFilesDir / getCodeCacheDir / getObbDir 均为 API 21+
            if (isUnderFile(absPath, context.getNoBackupFilesDir())
                    || isUnderFile(absPath, context.getCodeCacheDir())
                    || isUnderFile(absPath, context.getObbDir())){
                return true;
            }
        }

        //4) 特殊虚拟路径：File API 可访问，但不会进入 MediaStore
        //兼容 /sdcard 与 /storage/emulated/0 两种写法
        if (path.startsWith(fakeStartPath)
                || path.startsWith(fakeStartPath2)){
            return true;
        }
        return false;
    }

    /**
     * 分发 SAF 文档 Uri 到对应 Provider 的解析逻辑。
     * 已由 getFilePath 用 try-catch 包住，这里只负责路由。
     */
    private static String getDocumentPath(Context context, Uri uri){
        if (isExternalStorageDocument(uri)){
            return getPathFromExternalStorageDocument(uri);
        }
        if (isDownloadsDocument(uri)){
            return getPathFromDownloadsDocument(context, uri);
        }
        if (isMediaDocument(uri)){
            return getPathFromMediaDocument(context, uri);
        }
        //其他 DocumentsProvider（网盘、文件管理器等）没有通用路径约定
        return null;
    }

    /**
     * 解析 ExternalStorageProvider 文档。
     * docId 形如 {@code primary:Download/a.pdf} 或 {@code ABCD-1234:DCIM/b.jpg}。
     * type 是存储卷名：primary 为主卷，其余（ABCD-1234）为外置 SD 卡。
     */
    private static String getPathFromExternalStorageDocument(Uri uri){
        String docId = DocumentsContract.getDocumentId(uri);
        if (TextUtils.isEmpty(docId)){
            return null;
        }
        //限制只切第一刀，相对路径里即便有冒号也不再被切开
        String[] split = docId.split(":", 2);
        if (split.length < 2){
            return null;
        }
        String type = split[0];
        String relativePath = split[1];
        if (relativePath.length() == 0){
            return null;
        }

        if ("primary".equalsIgnoreCase(type)){
            return Environment.getExternalStorageDirectory() + "/" + relativePath;
        }
        //外置 SD 卡：标准挂载点是 /storage/<volumeName>/...
        //若某 ROM 挂载点不同，此处返回的路径可能无效，属设备差异
        return "/storage/" + type + "/" + relativePath;
    }

    /**
     * 解析 DownloadsProvider 文档。
     *
     * <p>文档 id 格式随系统版本变化很大，必须分类处理，不能直接 Long.valueOf：
     * <ul>
     *   <li>{@code raw:/storage/...}：id 即路径，直接返回；</li>
     *   <li>纯数字：在 public/my/all_downloads 三表中按 _id 查；</li>
     *   <li>{@code msf:123}：MediaStore.Files 的记录 id（Android 10+）；</li>
     *   <li>其他：退回对文档 Uri 本身查 _data。</li>
     * </ul>
     */
    private static String getPathFromDownloadsDocument(Context context, Uri uri){
        String id = DocumentsContract.getDocumentId(uri);
        if (TextUtils.isEmpty(id)){
            return null;
        }

        //raw: 前缀：id 剩余部分就是绝对路径
        //用 substring 而非 replaceFirst，避免被当成正则替换到中间
        if (id.startsWith("raw:")){
            return id.substring("raw:".length());
        }

        //纯数字 id：依次尝试 downloads 的三个入口
        //（不同 ROM / 权限模型下命中不同的表）
        if (TextUtils.isDigitsOnly(id)){
            String[] authorities = new String[]{
                    "content://downloads/public_downloads",
                    "content://downloads/my_downloads",
                    "content://downloads/all_downloads",
            };
            for (String authority : authorities){
                try {
                    Uri contentUri = ContentUris.withAppendedId(
                            Uri.parse(authority), Long.parseLong(id));
                    String path = getDataColumn(context, contentUri, null, null);
                    if (path != null){
                        return path;
                    }
                } catch (Exception e) {
                    //单个入口失败不影响其他入口
                    e.printStackTrace();
                }
            }
        }

        //msf: 前缀：映射到 MediaStore.Files 的 _id
        if (id.startsWith("msf:")){
            String fileId = id.substring("msf:".length());
            if (TextUtils.isDigitsOnly(fileId)){
                String path = getDataColumn(
                        context,
                        MediaStore.Files.getContentUri("external"),
                        MediaStore.MediaColumns._ID + "=?",
                        new String[]{fileId});
                if (path != null){
                    return path;
                }
            }
        }

        //最后兜底：直接对文档 Uri 查 _data
        return getDataColumn(context, uri, null, null);
    }

    /**
     * 解析 MediaProvider 文档。
     * docId 形如 {@code image:123} / {@code video:456} / {@code audio:789}。
     * 媒体文件的权威表由文档类型（即 MIME 大类）决定，与所在文件夹无关。
     */
    private static String getPathFromMediaDocument(Context context, Uri uri){
        String docId = DocumentsContract.getDocumentId(uri);
        if (TextUtils.isEmpty(docId)){
            return null;
        }
        String[] split = docId.split(":", 2);
        if (split.length < 2){
            return null;
        }
        String type = split[0];
        String mediaId = split[1];
        if (!TextUtils.isDigitsOnly(mediaId)){
            return null;
        }

        Uri contentUri;
        if ("image".equals(type)){
            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else if ("video".equals(type)){
            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        } else if ("audio".equals(type)){
            contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        } else {
            //MediaProvider 只有 image/video/audio 三类文档
            return null;
        }
        return getDataColumn(
                context,
                contentUri,
                MediaStore.MediaColumns._ID + "=?",
                new String[]{mediaId});
    }

    /**
     * 打开本地文件输入流，失败（不存在、是目录、无权限等）返回 null。
     */
    private static InputStream openFileQuietly(File file){
        try {
            return new FileInputStream(file);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 通过 ContentResolver 打开 Uri 输入流，失败返回 null。
     * 直接返回 {@code ContentResolver.openInputStream()} 的结果，不做 FileInputStream 强转——
     * 那是实现细节，不是 API 契约。
     */
    private static InputStream openUriQuietly(Context context, Uri uri){
        try {
            return context.getContentResolver().openInputStream(uri);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 打开本地文件输出流，失败返回 null。
     * 不存在则创建；{@code append} 控制追加/覆盖。
     */
    private static OutputStream openFileOutputStreamQuietly(File file, boolean append){
        try {
            return new FileOutputStream(file, append);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 通过 ContentResolver 打开 Uri 输出流，失败返回 null。
     * {@code append=true} 用 "wa"（write append），{@code false} 用默认 "w"（截断）。
     * 不做强转为 FileOutputStream，那是实现细节。
     */
    private static OutputStream openUriOutputStreamQuietly(Context context, Uri uri, boolean append){
        try {
            if (append){
                return context.getContentResolver().openOutputStream(uri, "wa");
            }
            return context.getContentResolver().openOutputStream(uri);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * File API 路径：私有目录、API 19–28 共享存储、以及各种 OEM 虚拟路径。
     */
    private static FileData createViaFile(Context context, File dir, String safeName, boolean addOnly){
        if (!dir.exists() && !dir.mkdirs()){
            return null;
        }

        String overwriteFailReason = null;

        //覆盖优先：目标存在则截断写，不存在则直接创建（同名）
        if (!addOnly){
            File target = new File(dir, safeName);
            OutputStream out = null;
            try {
                out = new FileOutputStream(target, false);
            } catch (Exception e) {
                overwriteFailReason = describeException(e);
            }
            if (out != null){
                return buildFileData(target.getPath(), target.getName(), out);
            }
            //打不开（被占用、是目录、无权限等）→ 落到 add 逻辑
        }

        //add 逻辑：原子 createNewFile 占坑，冲突则 foo (1).jpg / foo (2).jpg …
        File created = createUniqueFile(dir, safeName);
        if (created == null){
            return null;
        }
        OutputStream out = openFileOutputStreamQuietly(created, false);
        if (out == null){
            //开了流却失败，清掉空占位，避免留下 0 字节垃圾
            //noinspection ResultOfMethodCallIgnored
            created.delete();
            return null;
        }
        //覆盖失败而改走新建时，debug 包说明原因与最终文件名
        logOverwriteFallback(context, safeName, overwriteFailReason, created.getName());
        return buildFileData(created.getPath(), created.getName(), out);
    }

    /**
     * 在目录下按 Android 规则取一个未被占用的文件名并创建文件。
     * 用 {@link File#createNewFile()}（原子 O_EXCL）判断占用，无需 list 扫描，无竞态。
     * 原名空闲则用原名；被占则 {@code name (1).ext}、{@code name (2).ext} …
     *
     * @return 新建的文件；连续冲突 9999 次或 IO 失败返回 {@code null}
     */
    private static File createUniqueFile(File dir, String fileName){
        File candidate = new File(dir, fileName);
        if (safeCreateNewFile(candidate)){
            return candidate;
        }
        String base = getFront(fileName);
        String ext = getBehind(fileName);
        for (int i = 1; i <= 9999; i++){
            candidate = new File(dir, base + " (" + i + ")" + ext);
            if (safeCreateNewFile(candidate)){
                return candidate;
            }
        }
        return null;
    }

    private static boolean safeCreateNewFile(File file){
        try {
            return file.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * API 29+ 共享存储：覆写已有媒体库记录，或 insert 新建。
     */
    @TargetApi(29)
    private static FileData createViaMediaStore(Context context, String absDir, String safeName, boolean addOnly){
        String fullPath = absDir.endsWith("/") ? absDir + safeName : absDir + "/" + safeName;
        String overwriteFailReason = null;

        //覆盖优先：查到同路径记录就尝试截断写
        if (!addOnly){
            Uri existing = queryMediaStoreUri(context, fullPath, fullPath);
            if (existing != null){
                OutputStream out = null;
                try {
                    out = context.getContentResolver().openOutputStream(existing, "w");
                } catch (Exception e) {
                    //典型：无权限写其他应用创建的文件（SecurityException）
                    overwriteFailReason = describeException(e);
                }
                if (out != null){
                    return buildFileDataFromUri(context, existing, out);
                }
                if (overwriteFailReason == null){
                    overwriteFailReason = "openOutputStream 返回 null";
                }
            }
        }

        //add：insert 新建，DISPLAY_NAME 冲突时 MediaProvider 自动加 (N)
        String relativePath = buildRelativePath(absDir);
        if (TextUtils.isEmpty(relativePath)){
            return null;
        }
        String mimeType = getMimeType(safeName);
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, safeName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath);

        Uri collection = getInsertCollection(absDir, mimeType);
        Uri insertUri = null;
        try {
            insertUri = context.getContentResolver().insert(collection, values);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        if (insertUri == null){
            return null;
        }

        OutputStream out = openUriOutputStreamQuietly(context, insertUri, false);
        if (out == null){
            //insert 成功但开流失败：回滚，避免媒体库留下孤儿记录
            try {
                context.getContentResolver().delete(insertUri, null, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
        FileData data = buildFileDataFromUri(context, insertUri, out);
        logOverwriteFallback(context, safeName, overwriteFailReason, data.getFileName());
        return data;
    }

    /**
     * 由规范目录路径算出 MediaStore 的 RELATIVE_PATH（以 / 结尾）。
     * 支持主卷 /storage/emulated/0 与外置卷 /storage/XXXX-XXXX。
     */
    @TargetApi(29)
    private static String buildRelativePath(String absDir){
        String primary = Environment.getExternalStorageDirectory().getAbsolutePath();
        String relative;
        if (absDir.equals(primary)){
            relative = "";
        } else if (absDir.startsWith(primary + "/")){
            relative = absDir.substring(primary.length() + 1);
        } else if (absDir.startsWith("/storage/")){
            //外置卷：/storage/<volume>/相对路径
            String rest = absDir.substring("/storage/".length());
            int slash = rest.indexOf('/');
            if (slash < 0 || slash + 1 >= rest.length()){
                return null;
            }
            relative = rest.substring(slash + 1);
        } else {
            return null;
        }
        if (TextUtils.isEmpty(relative)){
            return null;
        }
        return relative.endsWith("/") ? relative : relative + "/";
    }

    /**
     * insert 用的集合必须同时满足「MIME 类型」与「MediaProvider 允许的目录」：
     * <ul>
     *   <li>Images 仅允许 DCIM / Pictures；Video 允许 DCIM / Movies / Pictures；</li>
     *   <li>Audio 允许 Music / Alarms / Ringtones / Podcasts / Notifications / Audiobooks；</li>
     *   <li>Download 目录走 Downloads 表（类型不限）；</li>
     *   <li>Documents、自定义目录，以及「类型与目录不匹配」（如 Download 里的 gif、DCIM 里的 txt）
     *       一律 Files。注意：进 Files 的图片/视频不一定出现在系统相册，那是平台目录白名单限制。</li>
     * </ul>
     * 因此不能只看 MIME 选表。
     */
    @TargetApi(29)
    private static Uri getInsertCollection(String absDir, String mimeType){
        String volume = resolveMediaVolume(absDir);

        //Download 目录：Downloads 表，任意类型都可登记
        if (isUnderPublicDirName(absDir, Environment.DIRECTORY_DOWNLOADS)){
            return MediaStore.Downloads.getContentUri(volume);
        }

        if (mimeType != null){
            //图片：仅 DCIM / Pictures
            if (mimeType.startsWith("image/")
                    && (isUnderPublicDirName(absDir, Environment.DIRECTORY_DCIM)
                        || isUnderPublicDirName(absDir, Environment.DIRECTORY_PICTURES))){
                return MediaStore.Images.Media.getContentUri(volume);
            }
            //视频：DCIM / Movies / Pictures
            if (mimeType.startsWith("video/")
                    && (isUnderPublicDirName(absDir, Environment.DIRECTORY_DCIM)
                        || isUnderPublicDirName(absDir, Environment.DIRECTORY_MOVIES)
                        || isUnderPublicDirName(absDir, Environment.DIRECTORY_PICTURES))){
                return MediaStore.Video.Media.getContentUri(volume);
            }
            //音频：系统标准铃声/音乐/有声书目录
            //DIRECTORY_AUDIOBOOKS 为 API 30+ 常量，这里用字符串兼容 minSdk 19
            if (mimeType.startsWith("audio/")
                    && (isUnderPublicDirName(absDir, Environment.DIRECTORY_ALARMS)
                        || isUnderPublicDirName(absDir, Environment.DIRECTORY_MUSIC)
                        || isUnderPublicDirName(absDir, Environment.DIRECTORY_NOTIFICATIONS)
                        || isUnderPublicDirName(absDir, Environment.DIRECTORY_PODCASTS)
                        || isUnderPublicDirName(absDir, Environment.DIRECTORY_RINGTONES)
                        || isUnderPublicDirName(absDir, "Audiobooks"))){
                return MediaStore.Audio.Media.getContentUri(volume);
            }
        }

        //Documents、自定义目录、类型与目录不匹配 → Files
        return MediaStore.Files.getContentUri(volume);
    }

    /**
     * absDir 是否落在某个公共目录名之下（任意存储卷）。
     * 用路径段匹配，避免 {@code MyDownload} 误命中 {@code Download}。
     */
    private static boolean isUnderPublicDirName(String absDir, String dirName){
        if (TextUtils.isEmpty(absDir) || TextUtils.isEmpty(dirName)){
            return false;
        }
        String path = absDir.endsWith("/") ? absDir : absDir + "/";
        return path.contains("/" + dirName + "/");
    }

    private static FileData buildFileData(String path, String name, OutputStream out){
        FileData data = new FileData();
        data.setOs(out);
        data.setFilePath(path);
        data.setFileName(name);
        return data;
    }

    /**
     * 从已打开的 Uri 回填路径与最终文件名。
     * 分区存储下 getFilePath 可能为 null，此时以 DISPLAY_NAME 为准。
     */
    private static FileData buildFileDataFromUri(Context context, Uri uri, OutputStream out){
        FileData data = new FileData();
        data.setOs(out);
        String path = getFilePath(context, uri);
        if (path != null){
            data.setFilePath(path);
            data.setFileName(getFileName(path));
        } else {
            data.setFileName(queryDisplayName(context, uri));
        }
        return data;
    }

    private static String queryDisplayName(Context context, Uri uri){
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    uri,
                    new String[]{MediaStore.MediaColumns.DISPLAY_NAME},
                    null, null, null);
            if (cursor != null && cursor.moveToFirst()){
                int index = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                if (index >= 0){
                    return cursor.getString(index);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null){
                cursor.close();
            }
        }
        return null;
    }

    /**
     * 把异常收成一行可读原因，供日志使用。
     */
    private static String describeException(Exception e){
        String msg = e.getMessage();
        if (TextUtils.isEmpty(msg)){
            return e.getClass().getSimpleName();
        }
        return e.getClass().getSimpleName() + ": " + msg;
    }

    /**
     * 覆盖失败转 add 成功后，debug 包打出原因与最终文件名。
     * {@code failReason} 为 null 表示并未发生「覆盖失败」，不打日志。
     */
    private static void logOverwriteFallback(Context context, String requestedName, String failReason, String actualName){
        if (failReason == null){
            return;
        }
        if (isApkInDebug(context)){
            Log.e(TAG, "无法覆盖「" + requestedName + "」：" + failReason + "，已新建「" + actualName + "」。");
        }
    }

    /**
     * File 删除，吞掉异常。文件本就不存在时返回 false。
     */
    private static boolean deleteFileQuietly(File file){
        if (file == null){
            return false;
        }
        try {
            return file.delete();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * ContentResolver 删除，吞掉异常，返回删除的行数（失败为 0）。
     */
    private static int deleteUriQuietly(Context context, Uri uri){
        if (context == null || uri == null){
            return 0;
        }
        try {
            return context.getContentResolver().delete(uri, null, null);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 把文件名切成 [主名, 扩展名]。
     * 扩展名含点，且仅当点不在首位、不在末位时才存在。
     * 隐藏文件 {@code .gitignore} 主名是整个文件名，无扩展名。
     */
    private static String[] splitBaseExt(String fileName){
        if (fileName == null){
            return new String[]{"", ""};
        }
        int spot = fileName.lastIndexOf('.');
        //点在中间：标准的 name.ext
        if (spot > 0 && spot < fileName.length() - 1){
            return new String[]{fileName.substring(0, spot), fileName.substring(spot)};
        }
        //点在首位（隐藏文件）或末位（孤点）或没有点：整体是主名，无扩展名
        // ".gitignore" → [".gitignore", ""]； "foo." → ["foo.", ""] ？
        if (spot == fileName.length() - 1 && spot > 0){
            //尾部孤点丢掉："foo." → ["foo", ""]
            return new String[]{fileName.substring(0, spot), ""};
        }
        return new String[]{fileName, ""};
    }

}
