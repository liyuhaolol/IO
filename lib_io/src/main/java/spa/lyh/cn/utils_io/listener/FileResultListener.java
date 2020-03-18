package spa.lyh.cn.utils_io.listener;

import java.io.FileOutputStream;

public interface FileResultListener {
    /**
     * 文件创建成功
     */
    void onSuccess(FileOutputStream fos,String filePath,String fileName);

    /**
     * 文件创建失败
     */
    void onFailure();
}
