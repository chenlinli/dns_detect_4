package cn.cl.detect;

/**
 mvn clean scala:compile assembly:assembly -Dmaven.test.skip=true

 drop table if EXISTS online_domain;
 create table if not EXISTS online_domain(
 id bigint,
 domain_name string,
 label string,


 source string
 )row format delimited fields terminated by '\t';

 load data local inpath '/usr/local/dns_detect_data/umbrella_names_for_train' into table online_domain;
 load data local inpath '/usr/local/dns_detect_data/dns2tcp_names_for_train' into table online_domain;
 load data local inpath '/usr/local/dns_detect_data/dnscat2_names_for_train' into table online_domain;
 load data local inpath '/usr/local/dns_detect_data/iodine_names_for_train' into table online_domain;

 ./bin/kafka-topics.sh --zookeeper 192.168.137.107:2181,192.168.137.108:2181,192.168.137.109:2181 --list
 ./bin/kafka-topics.sh --zookeeper 192.168.137.107:2181,192.168.137.108:2181,192.168.137.109:2181 --topic dns_realtime_log --replication-factor 1 --partitions 1 --create
 ./bin/kafka-console-consumer.sh --zookeeper 192.168.137.107:2181,192.168.137.108:2181,192.168.137.109:2181 --topic dns_realtime_log --from-beginning
 ./bin/kafka-console-producer.sh --broker-list 192.168.137.107:9092,192.168.137.108:9092,192.168.137.109:9092 --topic  dns_realtime_log


 *
 */
public class App 
{
    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );
    }
}
