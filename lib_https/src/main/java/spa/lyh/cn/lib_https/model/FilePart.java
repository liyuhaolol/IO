package spa.lyh.cn.lib_https.model;

import okhttp3.RequestBody;

/**
 * Created by liyuhao on 2017/12/6.
 * 上传文件的集合
 */

public class FilePart {
    public String name = "";
    public String filename = "";
    public RequestBody body;
}
