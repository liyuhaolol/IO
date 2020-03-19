package spa.lyh.cn.utils_io.model;

import java.io.FileOutputStream;

public class FileData {
    private FileOutputStream fos;
    private String fileName;
    private String filePath;


    public FileOutputStream getFos() {
        return fos;
    }

    public void setFos(FileOutputStream fos) {
        this.fos = fos;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
