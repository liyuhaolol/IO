package spa.lyh.cn.lib_https.model;

/**
 * Created by liyuhao on 2017/9/6.
 */

public class Progress {
    private int progress;
    private String currentSize;
    private String sumSize;

    public Progress(int progress, String currentSize,String sumSize){
        this.progress = progress;
        this.currentSize = currentSize;
        this.sumSize = sumSize;
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
