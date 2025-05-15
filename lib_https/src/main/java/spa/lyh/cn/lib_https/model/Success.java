package spa.lyh.cn.lib_https.model;

/**
 * Created by liyuhao on 2017/12/12.
 */

public class Success {
    private String filePath;
    private String fileName;

    public Success(String filePath, String fileName){
        this.filePath = filePath;
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileName() {
        return fileName;
    }
}
