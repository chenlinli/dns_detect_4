package cn.cl.detect.predict;

import cn.cl.detect.config.ConfigurationManager;
import cn.cl.detect.constant.Constants;
import cn.cl.detect.util.*;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.Optional;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.mllib.classification.LogisticRegressionModel;
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
import java.util.stream.Collectors;

/**
 * 从kafka接收抓到的域名，在线预测
 */
public class OnlineDomainPredict {

    static String rfModelName = ConfigurationManager.getProperty(Constants.ONLINE_RANDOM_FOREST_MODEL);
    static String dtModelName = ConfigurationManager.getProperty(Constants.ONLINE_DECISION_TREE_MODEL);
    static String lrModelName = ConfigurationManager.getProperty(Constants.ONLINE_LR_MODEL);
    //黑名单的hash key
    static String blacklistKey = ConfigurationManager.getProperty(Constants.REDIS_DOMAIN_BLACKlIST_KEY);
//    static Broadcast<Map<String, String>> broadcastWhitelistMap = null;
    //训练好的模型
    static LogisticRegressionModel lrModel = null;
    static DecisionTreeModel dtModel = null;
    static RandomForestModel rfModel = null;

    //计算bigram的基础table，从redis加载
    //static  Map<String, String> table = null;


    public static void main(String[] args) throws InterruptedException {
           /*
        判断应用程序是否在本地执行
         */
        JavaStreamingContext jssc = null;
        SparkSession spark = null;
        Boolean onLocal = ConfigurationManager.getBoolean(Constants.SPARK_LOCAL);
        if (onLocal) {
            SparkConf conf = new SparkConf().setAppName(Constants.SPARK_APP_NAME_2)
                    .setMaster("local[4]");  //streaming至少需要两个线程：接收数据，处理数据
            jssc = new JavaStreamingContext(conf, Durations.seconds(5));  //5秒一个rdd
            spark = SparkSession.builder().getOrCreate();
        }else{
            System.out.println("=================Remote================");
            spark = SparkSession.builder().appName(Constants.SPARK_APP_NAME_2)
                    //.enableHiveSupport()
                    .getOrCreate();
            jssc = new JavaStreamingContext(new JavaSparkContext(spark.sparkContext()),Durations.seconds(5));
        }

        //加载模型和table
        Broadcast<Map<String, String>> broadcastTable = init(spark, onLocal);
        Broadcast<String> broadcastBlacklistKey = new JavaSparkContext(spark.sparkContext()).broadcast(blacklistKey);

        //Kafka作为数据源，消费kafka的数据
        Map<String, Object> kafkaParams = new HashMap<>();
        Set<String> topicSet = KafkaConsumerConfig.consumerConfig(kafkaParams,"MyGroupId1");
        JavaInputDStream<ConsumerRecord<String, String>> domainRealTimeLogDS = KafkaUtils.createDirectStream(
                jssc,
                LocationStrategies.PreferConsistent(),
                ConsumerStrategies.<String, String>Subscribe(topicSet, kafkaParams));

        onlineDomainPredict(domainRealTimeLogDS,onLocal,broadcastTable,broadcastBlacklistKey,null);

        jssc.start();
        jssc.awaitTermination();
        jssc.stop();

    }

    /**
     * 加载预测模型和bigram table
     * @param spark
     * @param onLocal
     */
    private static Broadcast<Map<String, String>> init(SparkSession spark, Boolean onLocal) {
        String rfPath = ModelUtil.getModelPathByName(rfModelName, onLocal);
        String dtPath = ModelUtil.getModelPathByName(dtModelName, onLocal);
        String lrPath = ModelUtil.getModelPathByName(lrModelName, onLocal);

        lrModel = LogisticRegressionModel.load(spark.sparkContext(), lrPath);
        dtModel = DecisionTreeModel.load(spark.sparkContext(), dtPath);
        rfModel = RandomForestModel.load(spark.sparkContext(), rfPath);

        Map<String, String> table = JedisUtil.hgetAll(ConfigurationManager.getProperty(Constants.REDIS_UMBRELLA_TABLE_NAME));
//        Map<String,String> whitelistMap = JedisUtil.hgetAll(ConfigurationManager.getProperty(Constants.REDIS_DOMAIN_WHITELIST_KEY));
//        broadcastWhitelistMap = new JavaSparkContext(spark.sparkContext()).broadcast(whitelistMap);
        return new JavaSparkContext(spark.sparkContext()).broadcast(table);
    }

    /**
     * 使用3种模型预测域名是否是隧道流量
     * @param domainRealTimeLogDS
     * @param onLocal
     * @param broadcastTable
     * @param broadcastBlacklistKey
     * @param broadcastWhitelistMap
     */
    private static void onlineDomainPredict(
            JavaInputDStream<ConsumerRecord<String, String>> domainRealTimeLogDS,
            Boolean onLocal,
            Broadcast<Map<String, String>> broadcastTable,
            Broadcast<String> broadcastBlacklistKey,
            Broadcast<Map<String, String>> broadcastWhitelistMap) {

//        Map<String, String> whitelistMap = broadcastWhitelistMap.getValue();
//
//        //过滤白名单
//        JavaDStream<ConsumerRecord<String, String>> filterWhiteListDS = domainRealTimeLogDS.filter(r -> {
//            return !whitelistMap.containsKey(r.value().split("\t")[8].toLowerCase());
//        });

        //使用transform过滤黑名单，返回过滤后的dns
        JavaPairDStream<String, String> filteredLogDS = domainRealTimeLogDS.transformToPair(
                new Function<JavaRDD<ConsumerRecord<String, String>>, JavaPairRDD<String, String>>() {
            @Override
            public JavaPairRDD<String, String> call(JavaRDD<ConsumerRecord<String, String>> rdd) throws Exception {
                //System.out.println("收到消息："+r.value());
                System.out.println("*****************get**********************");
                Map<String, String> blackListMap = JedisUtil.hgetAll(broadcastBlacklistKey.getValue());
                //每个rdd处理会去连接一次redis，不是每条记录取一次
                System.out.println("*****************got**********************");
                List<Tuple2<String,Boolean>> blackList = MapUtil.map2List(blackListMap);
                JavaSparkContext jsc = new JavaSparkContext(rdd.context());
                JavaPairRDD<String, Boolean> blacklistRDD = jsc.parallelizePairs(blackList);

                JavaPairRDD<String, String> mainDomain2domainPair = rdd.mapToPair(r -> {
                    String[] split = r.value().split("\t");
                    return new Tuple2<>(split[8], split[1]);  //主域名，完整域名
                });

                return mainDomain2domainPair.leftOuterJoin(blacklistRDD).filter(t->{
                            Optional<Boolean> optional = t._2._2;
                            return !(optional.isPresent() && optional.get());
                        }).mapToPair(t->{
                            return new Tuple2<String,String>(t._1,t._2._1);  //主域名，完整域名
                        });
            }
        });

        Map<String, String> table = broadcastTable.getValue();
        //域名的特征向量DS
        JavaDStream<Tuple2<String,Vector>> featuresDS = filteredLogDS.map(t -> {
            //域名预处理：转小写，去除最后的.
            String domain = DomainUtil.domainDealing(t._2);
            double[] features = DomainUtil.getDomainFeatures(table, domain);
            return new Tuple2<String,Vector>(t._2+"_"+t._1,Vectors.dense(features));  //完整域名_主域名，features
        });

        featuresDS.foreachRDD(rdd->{
            rdd.foreach(v->{
                double p1 = lrModel.predict(v._2);
                double p2 = dtModel.predict(v._2);
                double p3 = rfModel.predict(v._2);
                String[] split = v._1.split("_");
                System.out.println(split[0]+" 逻辑回归预测结果："+p1+" 决策树预测结果："+p2+" 随机森林预测结果："+p3);
                //只有当三个预测都为异常才认为是异常
                if(p1>0 && p2>0 && p3>0){
                    //顶级域名存入redis和数据库的黑名单
                    JedisUtil.hset(blacklistKey,split[1],"true");  //1表示异常
                }

            });
        });

    }


}
