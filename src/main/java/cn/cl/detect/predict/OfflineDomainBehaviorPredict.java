package cn.cl.detect.predict;

import cn.cl.detect.config.ConfigurationManager;
import cn.cl.detect.constant.Constants;
import cn.cl.detect.util.*;
import com.sun.java.browser.plugin2.DOM;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.Optional;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.mllib.classification.LogisticRegressionModel;
import org.apache.spark.mllib.classification.NaiveBayesModel;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.Vectors;
import org.apache.spark.mllib.tree.model.DecisionTreeModel;
import org.apache.spark.mllib.tree.model.RandomForestModel;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import scala.Tuple2;

import java.util.*;

public class OfflineDomainBehaviorPredict {
    static String rfModelName = ConfigurationManager.getProperty(Constants.ACTION_RANDOM_FOREST_MODEL);
    static String dtModelName = ConfigurationManager.getProperty(Constants.ACTION_DECISION_TREE_MODEL);
    static String lrModelName = ConfigurationManager.getProperty(Constants.ACTION_LR_MODEL);
    static String nbModelName = ConfigurationManager.getProperty(Constants.ACTION_NAIVEBAYES_MODEL);
    //训练好的模型
    static LogisticRegressionModel lrModel = null;
    static DecisionTreeModel dtModel = null;
    static RandomForestModel rfModel = null;
    static NaiveBayesModel nbModel = null;

    private static final String TXT = "16";
    private static final String CNAME = "5";
    private static final String A ="1";
    private static final String AAAA = "28";

    private static int windowSize = 5;

    static String blacklistKey=ConfigurationManager.getProperty(Constants.REDIS_DOMAIN_BLACKlIST_KEY);
    static String whitelistKey=ConfigurationManager.getProperty(Constants.REDIS_DOMAIN_WHITELIST_KEY);

    public static void main(String[] args) throws InterruptedException {
            /*
        判断应用程序是否在本地执行
         */
        JavaStreamingContext jssc = null;
        SparkSession spark = null;
        Boolean onLocal = ConfigurationManager.getBoolean(Constants.SPARK_LOCAL);
        if (onLocal) {
            SparkConf conf = new SparkConf().setAppName(Constants.SPARK_APP_NAME_4)
                    .setMaster("local[4]");  //streaming至少需要两个线程：接收数据，处理数据
            jssc = new JavaStreamingContext(conf, Durations.seconds(5));  //5秒一个rdd
            spark = SparkSession.builder().getOrCreate();
        }else{
            System.out.println("=================Remote================");
            spark = SparkSession.builder().appName(Constants.SPARK_APP_NAME_4)
                    //.enableHiveSupport()
                    .getOrCreate();
            jssc = new JavaStreamingContext(new JavaSparkContext(spark.sparkContext()),Durations.minutes(1));
        }

        init(spark,onLocal);
        Broadcast<String> broadcastBlacklistKey = new JavaSparkContext(spark.sparkContext()).broadcast(blacklistKey);
        Broadcast<String> broadcastWhitelistKey = new JavaSparkContext(spark.sparkContext()).broadcast(whitelistKey);

        //Kafka作为数据源，消费kafka的数据
        Map<String, Object> kafkaParams = new HashMap<>();
        Set<String> topicSet = KafkaConsumerConfig.consumerConfig(kafkaParams,"MyGroupId2");
        JavaInputDStream<ConsumerRecord<String, String>> dnsFlowDF = KafkaUtils.createDirectStream(
                jssc,
                LocationStrategies.PreferConsistent(),
                ConsumerStrategies.<String, String>Subscribe(topicSet, kafkaParams));

        dnsActionPredict(dnsFlowDF,onLocal,broadcastBlacklistKey,broadcastWhitelistKey);
        jssc.start();
        jssc.awaitTermination();
        jssc.stop();
    }

    private static void dnsActionPredict(JavaInputDStream<ConsumerRecord<String, String>> dnsFlowDF,
                                         Boolean onLocal,
                                         Broadcast<String> broadcastBlacklistKey,
                                         Broadcast<String> broadcastWhitelistKey) {

        //过滤黑名单和白名单
        JavaPairDStream<String, String> filteredPairDF = dnsFlowDF.transformToPair(new Function<JavaRDD<ConsumerRecord<String, String>>, JavaPairRDD<String, String>>() {
            @Override
            public JavaPairRDD<String, String> call(JavaRDD<ConsumerRecord<String, String>> rdd) throws Exception {

                Map<String, String> blackListMap = JedisUtil.hgetAll(broadcastBlacklistKey.getValue());
                Map<String, String> whiteListMap = JedisUtil.hgetAll(broadcastWhitelistKey.getValue());

                //每个rdd处理会去连接一次redis，不是每条记录取一次
                //map类型的黑名单转为List
                List<Tuple2<String, Boolean>> blackList = MapUtil.map2List(blackListMap);
                JavaSparkContext jsc = new JavaSparkContext(rdd.context());
                JavaPairRDD<String, Boolean> blacklistRDD = jsc.parallelizePairs(blackList);
                JavaPairRDD<String, String> mainDomain2RowPairs = rdd.mapToPair(r -> {
                    String row = r.value();
                    String[] split = row.split("\t");
                    return new Tuple2<>(split[8], row);  //主域名，完整域名
                });

                return mainDomain2RowPairs.leftOuterJoin(blacklistRDD).filter(t -> {
                    Optional<Boolean> optional = t._2._2;  //true
                    return !(optional.isPresent() && optional.get());
                }).mapToPair(t -> {
//                    transactionId+\t+queryDomain+\t+src+\t+dst+\t+
//                    qType+\t+qr+\t+time+\t+rdataLength+\tmainDamain+\t+andataLength
                    String row = t._2._1;
                    //0x4826	wdl1.cache.wps.cn	fe80::4cb5:9e5d:78c3:4fac	fe80::1	1	0	2020-02-07 15:15:40	0	cache.wps.cn	0
                    String[] msg = row.split("\t");
                    StringBuilder sb = new StringBuilder("1 ");  //0
                    switch (msg[4]){  //1-5
                        case A: sb.append("1 0 0 0 0 ");break;
                        case AAAA:sb.append("0 1 0 0 0 ");break;
                        case CNAME:sb.append("0 0 1 0 0 ");break;
                        case TXT:sb.append("0 0 0 1 0 ");break;
                        default:sb.append("0 0 0 0 1 ");break;
                    }
                    sb.append(msg[4]+" "); //查询类型  //6
                    if(msg[5].equals("1")){  //应答
                        sb.append(msg[9]+" ").append(msg[7]+" ").append("0 1 ");  //7-10
                    }else{
                        sb.append("0 ").append("0 ").append("1 0 ");
                    }
                    sb.append(DomainUtil.getHostName(msg[1],msg[8]));  //11
                    return new Tuple2<String, String>(t._1,sb.toString());  //主域名，行记录
//                    return new Tuple2<String, String>(t._1,t._2._1);
                });
            }
        });

        filteredPairDF.foreachRDD(rdd->{
            rdd.foreach(t->{
                System.out.println("filteredRDD:"+t._1+"---------"+t._2);
            });
        });
        //相同的主域名的在一起
        JavaPairDStream<String, String> reducedPairDF = filteredPairDF.reduceByKeyAndWindow(new Function2<String, String, String>() {
            @Override

            public String call(String f1, String f2) throws Exception {
                String[] a = f1.split(" ");
                //包个数 A个数 AAAA个数 CNAME个数 TXT个数 Other个数 qType anDataLen rDataLen query个数 response个数 主机名
                String[] b = f2.split(" ");
                StringBuilder sb = new StringBuilder();
                sb.append(DomainUtil.plus(a[0], b[0]) + " ");//包个数相加
                sb.append(DomainUtil.plus(a[1], b[1]) + " "); //A类型相加
                sb.append(DomainUtil.plus(a[2], b[2]) + " "); //AAAA类型相加
                sb.append(DomainUtil.plus(a[3], b[3]) + " "); //CNAME类型相加
                sb.append(DomainUtil.plus(a[4], b[4]) + " "); //TXT类型相加
                sb.append(DomainUtil.plus(a[5], b[5]) + " "); //Other类型相加
                HashSet<String> set = new HashSet<>();
                set.addAll(Arrays.asList(a[6].split(",")));
                set.addAll(Arrays.asList(b[6].split(",")));
                for(String s :set){
                    sb.append(s+",");
                }
                sb = new StringBuilder(sb.substring(0,sb.length()-1)+" "); //去除最后的,
                sb.append(DomainUtil.plus(a[7], b[7]) + " "); //anDataLen类型相加
                sb.append(DomainUtil.plus(a[8], b[8]) + " "); //rDataLen类型相加
                sb.append(DomainUtil.plus(a[9], b[9]) + " "); //query个数相加
                sb.append(DomainUtil.plus(a[10], b[10]) + " "); //reponse个数相加
                set.clear();
                set.addAll(Arrays.asList(a[11].split(",")));
                set.addAll(Arrays.asList(b[11].split(",")));
                for(String s :set){
                    sb.append(s+",");
                }
                return sb.substring(0,sb.length()-1);
            }
        }, Durations.minutes(windowSize), Durations.minutes(1));

        reducedPairDF.foreachRDD(rdd->{
            rdd.foreach(t->{
                System.out.println("reducedPairdDF:"+t._1+">>>>>>>>>>.."+t._2);
            });
        });

        JavaDStream<Tuple2<String,Vector>> mainDomain2Features = reducedPairDF.map(t -> {
            String mainDomain = t._1;
            String[] featurePres = t._2.split(" ");
            double[] features = DomainUtil.getActionFeatures(featurePres, windowSize);
            System.out.println("features:*****************");
            for(int i= 0;i<features.length;i++){
                System.out.print(features[i]+"\t");
            }
            System.out.println();
            return new Tuple2<String,Vector>(mainDomain, Vectors.dense(features));
        });


        mainDomain2Features.foreachRDD(rdd->{
            rdd.foreach(v->{
                double p1 = lrModel.predict(v._2);
                double p2 = dtModel.predict(v._2);
                double p3 = rfModel.predict(v._2);
                double p4 = nbModel.predict(v._2);
                System.out.println(v._1+" 逻辑回归预测结果："+p1+" 决策树预测结果："+p2+" 随机森林预测结果："+p3+
                "朴素贝叶斯预测结果："+p4);
                //只有当三个预测都为异常才认为是异常
//                if(p1>0 && p2>0 && p3>0){
//                    //顶级域名存入redis和数据库的黑名单
//                    JedisUtil.hset(blacklistKey,split[1],"true");  //1表示异常
//                }

            });
        });

    }

    /**
     * 加载预测模型和bigram table
     * @param spark
     * @param onLocal
     */
    private static void init(SparkSession spark, Boolean onLocal) {
        String rfPath = ModelUtil.getModelPathByName(rfModelName, onLocal);
        String dtPath = ModelUtil.getModelPathByName(dtModelName, onLocal);
        String lrPath = ModelUtil.getModelPathByName(lrModelName, onLocal);
        String nbPath = ModelUtil.getModelPathByName(nbModelName,onLocal);

        lrModel = LogisticRegressionModel.load(spark.sparkContext(), lrPath);
        dtModel = DecisionTreeModel.load(spark.sparkContext(), dtPath);
        rfModel = RandomForestModel.load(spark.sparkContext(), rfPath);
        nbModel = NaiveBayesModel.load(spark.sparkContext(),nbPath);
    }
}
