package spa.lyh.cn.lib_https.exception;

/**********************************************************
 * okhttp的exception.
 **********************************************************/
public class OkHttpException extends Exception {
	private static final long serialVersionUID = 1L;

	/**
	 * the java layer exception, do not same to the logic error
	 */
	public final static int NETWORK_ERROR = -1; // the network relative error
	public final static int JSON_ERROR = -2; // the JSON relative error
	public final static int OTHER_ERROR = -3; // the unknow error
	public final static int CANCEL_REQUEST = -4; // cancel request
	public final static int SERVER_ERROR = -5; // server throw error code
	public final static int IO_ERROR = -6; // IO error during downloading

	/**
	 * the server return code
	 */
	private int ecode;

	/**
	 * the server return error message
	 */
	private String emsg;


	public OkHttpException(int ecode, String emsg) {
		this.ecode = ecode;
		this.emsg = emsg;
	}

	public int getEcode() {
		return ecode;
	}

	public String getEmsg() {
		return emsg;
	}

}