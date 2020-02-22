package cn.cl.detect.train;

import cn.cl.detect.config.ConfigurationManager;
import cn.cl.detect.config.RedisClient;
import cn.cl.detect.constant.Constants;
import cn.cl.detect.data.dname.MockHiveData;
import cn.cl.detect.util.DomainUtil;
import cn.cl.detect.util.ModelUtil;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.ml.feature.MinMaxScaler;
import org.apache.spark.ml.feature.MinMaxScalerModel;
import org.apache.spark.ml.linalg.DenseVector;
import org.apache.spark.mllib.classification.LogisticRegressionModel;
import org.apache.spark.mllib.classification.LogisticRegressionWithLBFGS;
import org.apache.spark.mllib.evaluation.MulticlassMetrics;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.Vectors;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.mllib.tree.DecisionTree;
import org.apache.spark.mllib.tree.RandomForest;
import org.apache.spark.mllib.tree.model.DecisionTreeModel;
import org.apache.spark.mllib.tree.model.RandomForestModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import redis.clients.jedis.Jedis;
import scala.Tuple2;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 实时检测部分，提取域名特征，使用不同的算法生成训练模型，模型保存到hdfs里
 * 同时将模型的hdfs路径保存在redis里，方便获取模型
 */
public class OnlineDomainTrain {

    private static final int classes = 4;

    public static void main(String[] args) {
          /*
        判断应用程序是否在本地执行
         */

        JavaSparkContext sc = null;
        SparkSession spark = null;
        Boolean onLocal = ConfigurationManager.getBoolean(Constants.SPARK_LOCAL);
        String path = null;
        if (onLocal) {
            SparkConf conf = new SparkConf().setAppName(Constants.SPARK_APP_NAME_1)
                    .setMaster("local");
            sc = new JavaSparkContext(conf);
            spark = SparkSession.builder().getOrCreate();
            MockHiveData.mockHiveData(sc,spark);  //创建临时表online_domain
            path = ConfigurationManager.getProperty(Constants.ONLINE_DATA_LOCAL_FILE_PATH);
        }else{
            System.out.println("=================REMOTE================");
            spark = SparkSession.builder().appName(Constants.SPARK_APP_NAME_1)
                    .enableHiveSupport().getOrCreate();
            sc = new JavaSparkContext(spark.sparkContext());
//            spark.sql("use "+ ConfigurationManager.getProperty(Constants.HIVE_DATABASE));  //集群运行就使用hive里的模拟数据
            path = ConfigurationManager.getProperty(Constants.ONLINE_DATA_REMOTE_FILE_PATH);
        }
        //testLocalUseOfHive(spark);

        /**
         * 计算每个域名出现的概率,长度，层级个数，字母比例，数字比例，异常字符比例6个特征存入
         * /lisvb格式的txt文件（放到hdfs上）：只用执行一次，训练模型的时候使用hdfs上的文件，本地执行使用本地文件
         */
        //calculateDomainFeaturesAndSaveToFile(sc, spark);
        trainOnlineModel(path,spark,sc,onLocal);
        spark.stop();
    }

    /**
     * 一个域名的测试
     * @param spark
     * @throws Exception
     */
    private static void testByOfflineModel(SparkSession spark) throws Exception {
        Jedis jedis = RedisClient.pool().getResource();
        jedis.select(ConfigurationManager.getInteger(Constants.REDIS_DB_INDEX));
        Map<String, String> table = jedis.hgetAll(ConfigurationManager.getProperty(Constants.REDIS_UMBRELLA_TABLE_NAME));
        jedis.close();
        double[] features = DomainUtil.getDomainFeatures(table, "static2.hc.my");
        Vector v = Vectors.dense(features);
        LogisticRegressionModel lrModel = LogisticRegressionModel.load(spark.sparkContext(), "model/online_lr_model");
        DecisionTreeModel dtModel = DecisionTreeModel.load(spark.sparkContext(), "model/online_decision_tree_model");
        RandomForestModel rfModel = RandomForestModel.load(spark.sparkContext(), "model/online_random_forest_model");
        double p1 = lrModel.predict(v);
        double p2 = dtModel.predict(v);
        double p3 = rfModel.predict(v);
        System.out.println("lr:"+p1+" dt:"+p2+" rf:"+p3);

    }

    private static void trainOnlineModel(String path, SparkSession spark, JavaSparkContext sc, Boolean onLocal) {

        Dataset<Row> df = spark.read().format("libsvm").load(path);
        df.show();
//        //数据归一化
        //最大最小值归一化
        MinMaxScalerModel minMaxScalerModel = new MinMaxScaler().setInputCol("features")
                .setOutputCol("scalerFeatures")
                .fit(df);
        Dataset<Row> rows = minMaxScalerModel.transform(df);
        rows.show();

        //获取最大最小值归一化后的整体数据
        JavaRDD<LabeledPoint> data = rows.javaRDD().map(r -> {
            double label = r.<Double>getAs("label");
            Vector dealtFeatures = Vectors.fromML(r.<DenseVector>getAs("scalerFeatures"));
            return new LabeledPoint(label, dealtFeatures);
        });

        JavaRDD<LabeledPoint>[] splits = data.randomSplit(new double[]{0.8, 0.2},10);
        JavaRDD<LabeledPoint> trainData = splits[0];
        JavaRDD<LabeledPoint> testData = splits[1];
        ModelUtil.LRClassify(trainData,testData,sc,onLocal,classes,
                ConfigurationManager.getProperty(Constants.ONLINE_LR_MODEL));

        ModelUtil.decisionTreeClassify(trainData,testData,sc,onLocal,classes,
                ConfigurationManager.getProperty(Constants.ONLINE_DECISION_TREE_MODEL),5);

        ModelUtil.randomForestClassify(trainData,testData,sc,onLocal,classes,
                ConfigurationManager.getProperty(Constants.ONLINE_RANDOM_FOREST_MODEL),5,5);
    }



    /**
     * 根据域名数据：大约20万条，计算每个域名的出现的概率、长度、层级个数、字母比例、数字比例、异常字符比例，6个特征，
     * 可以保存为csv/libsvm格式 csv格式是为了用execl打开作图观察，libsvm是为了训练模型时的数据源格式，方便最大最小值归一化
     * p(www.baidu) = p(w|^)*p(w|w)*p(w|w)*p(b|W)*p(a|b)*p(i|a)*p(d|i)*p(u|d)：右边的每个概率从table获取
     * 避免数据太小，使用log变乘为加
     * @param sc
     * @param spark
     * @return
     */
    private static void calculateDomainFeaturesAndSaveToFile(JavaSparkContext sc, SparkSession spark) {
        String sql = "select domain_name,label from online_domain";

        Dataset<Row> onlineDomainDF = spark.sql(sql);
        //redis table取出，广播变量
        Jedis jedis = RedisClient.pool().getResource();
        jedis.select(ConfigurationManager.getInteger(Constants.REDIS_DB_INDEX));
        Map<String, String> table = jedis.hgetAll(ConfigurationManager.getProperty(Constants.REDIS_UMBRELLA_TABLE_NAME));
        jedis.close();

        Broadcast<Map<String, String>> broadcast = sc.broadcast(table);
        
        JavaPairRDD<String, String> domain2LabelRDD = onlineDomainDF.toJavaRDD().mapToPair(r -> {
            return new Tuple2<>(r.getString(0), r.getString(1));
        });


        JavaRDD<Tuple2<String, String>> label2FRDD = domain2LabelRDD.map(t -> {
            String label = t._2;
            Map<String, String> map = broadcast.getValue();
            String domain = t._1;  //事先已经全部大写转为小写了
            double[] features = DomainUtil.getDomainFeatures(map, domain);
            //return new Tuple2<String, String>(label, f+","+domainLength+","+labelsLength+","+letterRate+","+numRate+","+malRate);
            //做成libsvm格式
            return new Tuple2<String, String>(label, "1:"+features[0]+" 2:"+features[1]+" 3:"+features[2]+
                    " 4:"+features[3]+" 5:"+features[4]+" 6:"+features[5]);

        });

        features2File(label2FRDD);

    }

    /**
     * 写入frequent文件里，便于制作图表观察两类域名的频率情况，相同的四分数据，计算的结果一致，只需要生成一次
     * @param label2FRDD
     */
    private static void features2File(JavaRDD<Tuple2<String, String>> label2FRDD){
       //
        List<Tuple2<String, String>> collect = label2FRDD.collect();
        BufferedWriter bufferedWriter = null;
        //File file = new File("domain_features.csv");
        ////libsvm格式
        File file = new File("domain_features.txt");
        if(file.exists()){
           file.delete();
        }

        try {
            file.createNewFile();
             bufferedWriter= new BufferedWriter(new FileWriter(file));
            for(Tuple2<String,String> t:collect){
                //bufferedWriter.write(t._1+","+t._2+"\n");
                //libsvm格式
                bufferedWriter.write(t._1+" "+t._2+"\n");
            }
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(bufferedWriter!=null){
                try {
                    bufferedWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
