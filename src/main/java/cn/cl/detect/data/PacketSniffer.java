package cn.cl.detect.data;

import cn.cl.detect.config.ConfigurationManager;
import cn.cl.detect.constant.Constants;
import cn.cl.detect.util.DateUtil;
import cn.cl.detect.util.DeviceUtil;
import org.pcap4j.core.*;
import org.pcap4j.packet.DnsPacket;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV6Packet;
import org.pcap4j.packet.Packet;

import java.text.SimpleDateFormat;

/**
 * 没有使用，存在丢包和不能解析的现象
 */
public class PacketSniffer {

	static int count = 0;
	public static void main(String[] args) throws PcapNativeException, NotOpenException {
		String filter ="udp and port 53";
		PcapHandle handle = DeviceUtil.getHandle(ConfigurationManager.getProperty(Constants.ONLINE_LISTEN_DEVICE_IP));
		handle.setFilter(filter, BpfProgram.BpfCompileMode.OPTIMIZE);
		SimpleDateFormat timeFormat = DateUtil.TIME_FORMAT;
		//监听包

		PacketListener listener = new PacketListener() {
			@Override
			public void gotPacket(Packet packet) {
				System.out.println("************************************************************");
				System.out.println("dns包个数："+(++count));
				System.out.println(packet);
				IpV4Packet ipV4Packet = packet.get(IpV4Packet.class);
				String srcAddr = null;
				String dstAddr = null;
				if(ipV4Packet!=null) {
					IpV4Packet.IpV4Header ipV4Header = ipV4Packet.getHeader();
					srcAddr = ipV4Header.getSrcAddr().getHostAddress();
					dstAddr = ipV4Header.getDstAddr().getHostAddress();
				}else{
					IpV6Packet.IpV6Header ipV6Header = packet.get(IpV6Packet.class).getHeader();
					srcAddr = ipV6Header.getSrcAddr().getHostAddress();
					dstAddr = ipV6Header.getDstAddr().getHostAddress();
				}

				String time = timeFormat.format(handle.getTimestamp().getTime());
				DnsPacket dnsPacket = packet.get(DnsPacket.class);
				//
				PacketProducer.packet2Kafka(dnsPacket,srcAddr,dstAddr,time);
			}

		};
		try {
			//循环将抓到的包交给listener处理
			handle.loop(-1,listener);
		} catch (PcapNativeException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (NotOpenException e) {
			e.printStackTrace();
		}

	}



}
