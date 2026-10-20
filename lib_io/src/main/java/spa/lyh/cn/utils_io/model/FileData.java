package spa.lyh.cn.utils_io.model;

import java.io.OutputStream;

/**
 * 建文件 / 开流的结果。
 * 理论上所有字段都可能为 null，调用方需自行判空。
 *
 * <p>注意 {@link #fileName} 是**最终落盘名**：
 * 若请求的是 {@code foo.jpg} 而返回 {@code foo (1).jpg}，
 * 说明覆盖失败、已按 add 逻辑另存（或系统自动改名）。
 */
public class FileData {
    private OutputStream os;
    private String fileName;
    private String filePath;


    public OutputStream getOs() {
        return os;
    }

    public void setOs(OutputStream os) {
        this.os = os;
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
