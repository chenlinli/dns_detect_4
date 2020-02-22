package cn.cl.detect.exception;

/**
 * 网卡设备找不到异常
 */
public class NoDeviceFoundException extends Exception {

	private static final long serialVersionUID = 1L;

	public NoDeviceFoundException(String message) {
        super(message);
    }
}