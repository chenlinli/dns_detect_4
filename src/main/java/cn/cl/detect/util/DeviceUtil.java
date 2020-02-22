package cn.cl.detect.util;

import cn.cl.detect.exception.NoDeviceFoundException;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 监听的网卡设备
 */
public class DeviceUtil {
    private static final  int snapLen = 65536;
	private static final int timeout = 10;

	private static PcapHandle handle = null;


	public static PcapHandle getHandle(String ip) {
		try {
			PcapNetworkInterface.PromiscuousMode mode = PcapNetworkInterface.PromiscuousMode.PROMISCUOUS;
			handle = getDeviceByName(ip).openLive(snapLen, mode, timeout);
		} catch (NoDeviceFoundException e) {
			e.printStackTrace();
		} catch (PcapNativeException e) {
			e.printStackTrace();
		}
		return handle;
	}

	/**
	 * Selects the network device to read the data from.
	 * 
	 * @return the pcap interface device selected
	 */
	public static PcapNetworkInterface getDeviceByName(String name) throws NoDeviceFoundException {
		InetAddress addr;
		try {
			addr = InetAddress.getByName(name);
			PcapNetworkInterface nif = Pcaps.getDevByAddress(addr);
			return nif;
		} catch (PcapNativeException e) {
			throw new NoDeviceFoundException("NoDeviceFoundException: " + e.getMessage());
		} catch (UnknownHostException e) {
			throw new NoDeviceFoundException("UnknownHostException: " + e.getMessage());
		}
	}

}