package cn.cl.detect.util;

import cn.cl.detect.config.ConfigurationManager;
import cn.cl.detect.constant.Constants;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class KafkaConsumerConfig {
    /**
     * kafka消费者配置
     * @param kafkaParams
     */
    public static Set<String> consumerConfig(Map<String, Object> kafkaParams,String groupId) {
        //192.168.137.107:9092,192.168.137.108:9092,192.168.137.109:9092
        String brokers = ConfigurationManager.getProperty(Constants.BOOTSTRAP_SERVERS);
        kafkaParams.put(Constants.BOOTSTRAP_SERVERS, brokers);
        kafkaParams.put("key.deserializer", StringDeserializer.class);
        kafkaParams.put("value.deserializer", StringDeserializer.class);
        kafkaParams.put("group.id",groupId);

        String topics = ConfigurationManager.getProperty(Constants.KAFKA_TOPICS);
        String[] split = topics.split(",");
        //数组-》set
        Set<String> topicSet = Arrays.stream(split).collect(Collectors.toSet());
        return topicSet;

    }
}
