package spa.lyh.cn.lib_https.model;

/**
 * Created by liyuhao on 2017/9/6.
 */

public class Progress {
    private boolean haveFileSize;
    private int progress;
    private String currentSize;
    private String sumSize;

    public Progress(boolean haveFileSize,int progress, String currentSize,String sumSize){
        this.haveFileSize = haveFileSize;
        this.progress = progress;
        this.currentSize = currentSize;
        this.sumSize = sumSize;
    }

    public boolean haveFileSize() {
        return haveFileSize;
    }

    public void setHaveFileSize(boolean haveFileSize) {
        this.haveFileSize = haveFileSize;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public void setCurrentSize(String currentSize) {
        this.currentSize = currentSize;
    }

    public void setSumSize(String sumSize) {
        this.sumSize = sumSize;
    }

    public int getProgress() {
        return progress;
    }

    public String getCurrentSize() {
        return currentSize;
    }

    public String getSumSize() {
        return sumSize;
    }
}
