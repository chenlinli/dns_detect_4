package cn.cl.detect.data;

import cn.cl.detect.config.ConfigurationManager;
import cn.cl.detect.constant.Constants;
import cn.cl.detect.util.DomainUtil;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.pcap4j.packet.DnsDomainName;
import org.pcap4j.packet.DnsPacket;
import org.pcap4j.packet.DnsQuestion;
import org.pcap4j.packet.DnsResourceRecord;

import java.util.List;
import java.util.Properties;

/**
 * 没有使用，存在丢包和不能解析的现象
 */
public class PacketProducer {

    private static KafkaProducer<String,String> producer;

    private static String topic = null;
    static {
        producer = new KafkaProducer<String, String>(createProducerConfig());
        topic = ConfigurationManager.getProperty(Constants.KAFKA_TOPICS);
    }

    private static Properties createProducerConfig() {
        Properties properties = new Properties();
        String bootstrapServers = Constants.BOOTSTRAP_SERVERS;
        properties.put(bootstrapServers, ConfigurationManager.getProperty(bootstrapServers));
        properties.put("key.serializer",  "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer","org.apache.kafka.common.serialization.StringSerializer");
        return properties;
    }

    /**
     * dns packet的 transcationId,queryDomain,srcIp,dstIp,qType,qr,recordTime,anRDataLength发给kafka
     * @param dnsPacket
     * @param srcAddr
     * @param dstAddr
     * @param time
     */
    public static void packet2Kafka(DnsPacket dnsPacket, String srcAddr, String dstAddr, String time) {
        DnsPacket.DnsHeader header = dnsPacket.getHeader();
        //获得无符号整型的事务id
        int unsigned = header.getId() & 0xFFFF;
        String transactionId = "0x"+Integer.toHexString(unsigned);
        boolean qr = header.isResponse();  //是否是应答 0 不是，1是
        String queryDomain = null;
        String qType = null;
        int rdataLength = 0;   //所有资源记录的长度
        String mainDomain = "";
        List<DnsQuestion> questions = header.getQuestions();
        for (DnsQuestion question:questions){
            DnsDomainName qName = question.getQName();
            queryDomain= qName.getName();
            mainDomain = DomainUtil.getMainDomain(queryDomain);
            qType = question.getQType().valueAsString();
        }
        if(qr){
            List<DnsResourceRecord> answers = header.getAnswers();
            System.out.println("an:");
            for(DnsResourceRecord resourceRecord :answers){
                System.out.println(resourceRecord);
               //rdataLength += resourceRecord.getRdLengthAsInt();
            }

            answers = header.getAuthorities();
            System.out.println("au:");
            for(DnsResourceRecord resourceRecord:answers){
                System.out.println(resourceRecord);
                //rdataLength += resourceRecord.getRdLengthAsInt();
            }

            answers = header.getAdditionalInfo();
            System.out.println("ad:");
            for(DnsResourceRecord resourceRecord:answers){
                System.out.println(resourceRecord);
                //rdataLength += resourceRecord.getRdLengthAsInt();
            }
        }
        StringBuilder log = new StringBuilder()
                .append(transactionId+"\t")  //0
                .append(queryDomain+"\t")    //1
                .append(srcAddr+"\t")        //2
                .append(dstAddr+"\t")        //3
                .append(qType+"\t")          //4:查询类型
                .append((qr?1:0)+"\t")  //0查询，1回复  5
                .append(time+"\t")           //6
                .append(rdataLength+"\t")    //7
                .append(mainDomain)          //8
                //.append(anDatalength)        //9
        ;
        System.out.println(log);
        //数据如何提供线下分析？
        //插入hive表
        producer.send(new ProducerRecord<String, String>(topic,log.toString()));
    }

  }
