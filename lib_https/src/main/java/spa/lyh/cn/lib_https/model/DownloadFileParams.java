package spa.lyh.cn.lib_https.model;

/**
 * Created by liyuhao on 2017/12/11.
 * 下载文件需要的参数
 */

public class DownloadFileParams {
    private String url;
    private String path;

    public DownloadFileParams(String url, String path){
        this.url = url;
        this.path = path;
    }

    public String getUrl() {
        return url;
    }

    public String getPath() {
        return path;
    }
}
