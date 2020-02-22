package cn.cl.detect.train;

import cn.cl.detect.config.ConfigurationManager;
import cn.cl.detect.config.RedisClient;
import cn.cl.detect.constant.Constants;
import cn.cl.detect.util.DomainUtil;
import cn.cl.detect.util.ModelUtil;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.ml.feature.MinMaxScaler;
import org.apache.spark.ml.feature.MinMaxScalerModel;
import org.apache.spark.ml.linalg.DenseVector;
import org.apache.spark.mllib.classification.LogisticRegressionModel;
import org.apache.spark.mllib.classification.LogisticRegressionWithLBFGS;
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

import java.util.HashMap;

public class OfflineDomainBehaviorTrain {
    private static final int classes = 10;

    public static void main(String[] args) {
           /*
        判断应用程序是否在本地执行
         */
        JavaSparkContext sc = null;
        SparkSession spark = null;
        Boolean onLocal = ConfigurationManager.getBoolean(Constants.SPARK_LOCAL);
        String path = null;
        if (onLocal) {
            SparkConf conf = new SparkConf().setAppName(Constants.SPARK_APP_NAME_3)
                    .setMaster("local");
            sc = new JavaSparkContext(conf);
            spark = SparkSession.builder().getOrCreate();
            path = ConfigurationManager.getProperty(Constants.ACTION_DATA_LOCAL_FILE_PATH);
        }else{
            System.out.println("=================REMOTE================");
            spark = SparkSession.builder().appName(Constants.SPARK_APP_NAME_3)
                    .enableHiveSupport().getOrCreate();
            sc = new JavaSparkContext(spark.sparkContext());
            path = ConfigurationManager.getProperty(Constants.ACTION_DATA_REMOTE_FILE_PATH);
        }
        trainOnlineModel(path,spark,sc,onLocal);

    }

    private static void trainOnlineModel(String path, SparkSession spark, JavaSparkContext sc, Boolean onLocal) {
        Dataset<Row> df = spark.read().format("libsvm").load(path);
        df.show();

        //数据归一化
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

        JavaRDD<LabeledPoint>[] splits = data.randomSplit(new double[]{0.7, 0.3},10);
        JavaRDD<LabeledPoint> trainData = splits[0];
        JavaRDD<LabeledPoint> testData = splits[1];

        //100%
        ModelUtil.LRClassify(trainData,testData,sc,onLocal,classes,
                ConfigurationManager.getProperty(Constants.ACTION_LR_MODEL));
//        //99.8%
        ModelUtil.decisionTreeClassify(trainData,testData,sc,onLocal,classes,
                ConfigurationManager.getProperty(Constants.ACTION_DECISION_TREE_MODEL),4);
//        //92%
        ModelUtil.randomForestClassify(trainData,testData,sc,onLocal,classes,
                ConfigurationManager.getProperty(Constants.ACTION_RANDOM_FOREST_MODEL),4,5);
//        //90%
        ModelUtil.naiveBayesClassify(trainData,testData,sc,onLocal,classes,
                ConfigurationManager.getProperty(Constants.ACTION_NAIVEBAYES_MODEL));
    }

}
