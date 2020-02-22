package cn.cl.detect.util;

import cn.cl.detect.config.ConfigurationManager;
import cn.cl.detect.config.RedisClient;
import cn.cl.detect.constant.Constants;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.mllib.classification.LogisticRegressionModel;
import org.apache.spark.mllib.classification.LogisticRegressionWithLBFGS;
import org.apache.spark.mllib.classification.NaiveBayes;
import org.apache.spark.mllib.classification.NaiveBayesModel;
import org.apache.spark.mllib.evaluation.MulticlassMetrics;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.mllib.tree.DecisionTree;
import org.apache.spark.mllib.tree.RandomForest;
import org.apache.spark.mllib.tree.model.DecisionTreeModel;
import org.apache.spark.mllib.tree.model.RandomForestModel;
import redis.clients.jedis.Jedis;
import scala.Tuple2;

import java.util.HashMap;
import java.util.List;

public class ModelUtil {
    /**
     * 随机森林预测:0.982408149790124
     * @param trainData
     * @param testData
     * @param sc
     * @param onLocal
     */
    public static void randomForestClassify(JavaRDD<LabeledPoint> trainData,
                                             JavaRDD<LabeledPoint> testData,
                                             JavaSparkContext sc,
                                             Boolean onLocal,
                                             Integer classes,
                                             String modelName,
                                             Integer numTrees,
                                             Integer maxDepth) {
        int numClasses = classes;
        //随机特征选取策略
        String featureSubsetStratege = "onethird";

        //每个特征可取值有几类,由于这里都是离散变量，map.size=0;
        HashMap<Integer, Integer> map = new HashMap<>();
        String impurity = "entropy";
        int maxBins = 32;
        int seed = 10;
        RandomForestModel model = RandomForest.trainClassifier(trainData, numClasses, map, numTrees, featureSubsetStratege, impurity, maxDepth, maxBins, seed);
        //测试
        JavaRDD<Tuple2<Object,Object>> predictAndLabels = testData.map(labelPoint -> {
            double label = labelPoint.label();
            Vector features = labelPoint.features();
            double predict = model.predict(features);
            return new Tuple2<Object,Object>(predict, label);
        });
        String modelPath = null;
        if(onLocal){
            modelPath = ConfigurationManager.getProperty(Constants.LOCAL_MODEL_PATH);
        }else {
            modelPath = ConfigurationManager.getProperty(Constants.HDFS_MODEL_PATH);
        }

        model.save(sc.sc(),modelPath+modelName);
        if(!onLocal) {
            Jedis jedis = RedisClient.pool().getResource();
            jedis.select(ConfigurationManager.getInteger(Constants.REDIS_DB_INDEX));
            String modelKey = ConfigurationManager.getProperty(Constants.REDIS_MODEL_KEY);
            jedis.hset(modelKey, modelName, modelPath + modelName);
            jedis.close();
        }
        showPredictingPrecision(predictAndLabels);
    }

    /**
     * 决策树预测：0.9642739235013913
     * @param trainData
     * @param testData
     * @param sc
     * @param onLocal
     */
    public static void decisionTreeClassify(JavaRDD<LabeledPoint> trainData,
                                             JavaRDD<LabeledPoint> testData,
                                             JavaSparkContext sc,
                                             Boolean onLocal,
                                             Integer classes,
                                             String modelName,
                                             Integer maxDepth) {
        int numClasses = classes;
        //每个特征可取值有几类,由于这里都是离散变量，map.size=0;
        HashMap<Integer, Integer> map = new HashMap<>();
        String impurity = "entropy";
//        int maxDepth = 5;
        int maxBins = 16;
        //
        DecisionTreeModel model = DecisionTree.trainClassifier(trainData, numClasses, map, impurity, maxDepth, maxBins);
        //测试
        JavaRDD<Tuple2<Object,Object>> predictAndLabels = testData.map(labelPoint -> {
            double label = labelPoint.label();
            Vector features = labelPoint.features();
            double predict = model.predict(features);
            return new Tuple2<Object,Object>(predict, label);
        });

        String modelPath = null;
        if(onLocal){
            modelPath = ConfigurationManager.getProperty(Constants.LOCAL_MODEL_PATH);
        }else {
            modelPath = ConfigurationManager.getProperty(Constants.HDFS_MODEL_PATH);
        }
        model.save(sc.sc(),modelPath+modelName);
        if(!onLocal) {
            Jedis jedis = RedisClient.pool().getResource();
            jedis.select(ConfigurationManager.getInteger(Constants.REDIS_DB_INDEX));
            String modelKey = ConfigurationManager.getProperty(Constants.REDIS_MODEL_KEY);
            jedis.hset(modelKey, modelName, modelPath + modelName);
            jedis.close();
        }

        showPredictingPrecision(predictAndLabels);
    }

    /**
     * 逻辑回归预测精度：0.8706786775456303
     * @param trainData
     * @param testData
     * @param sc
     * @param onLocal
     */
    public static void LRClassify(JavaRDD<LabeledPoint> trainData,
                                  JavaRDD<LabeledPoint> testData,
                                  JavaSparkContext sc,
                                  Boolean onLocal,
                                  Integer classes,
                                  String modelName) {
        //训练
        LogisticRegressionWithLBFGS lr = new LogisticRegressionWithLBFGS();
        lr.setNumClasses(classes).setIntercept(true);

        LogisticRegressionModel model = lr.run(trainData.rdd());

        //测试
        JavaRDD<Tuple2<Object,Object>> predictAndLabels = testData.map(labelPoint -> {
            double label = labelPoint.label();
            Vector features = labelPoint.features();
            double predict = model.predict(features);
            return new Tuple2<Object,Object>(predict, label);
        });
        String modelPath = null;
        if(onLocal){
            modelPath = ConfigurationManager.getProperty(Constants.LOCAL_MODEL_PATH);
        }else {
            modelPath = ConfigurationManager.getProperty(Constants.HDFS_MODEL_PATH);
        }
        model.save(sc.sc(),modelPath+modelName);
        if(!onLocal) {
            Jedis jedis = RedisClient.pool().getResource();
            jedis.select(ConfigurationManager.getInteger(Constants.REDIS_DB_INDEX));
            String modelKey = ConfigurationManager.getProperty(Constants.REDIS_MODEL_KEY);
            jedis.hset(modelKey, modelName, modelPath + modelName);
            jedis.close();
        }
        showPredictingPrecision(predictAndLabels);

    }

    /**
     * kmeans预测:0.5451115408196954
     * @param trainData
     * @param testData
     */
    public static void naiveBayesClassify(JavaRDD<LabeledPoint> trainData,
                                           JavaRDD<LabeledPoint> testData,
                                           JavaSparkContext sc,
                                           Boolean onLocal,
                                           Integer classes,
                                           String modelName) {
        NaiveBayesModel model = NaiveBayes.train(trainData.rdd(), 1.0);
        //测试
        JavaRDD<Tuple2<Object,Object>> predictAndLabels = testData.map(labelPoint -> {
            double label = labelPoint.label();
            Vector features = labelPoint.features();
            double predict = model.predict(features);
            return new Tuple2<Object,Object>(predict, label);
        });
        String modelPath = null;
        if(onLocal){
            modelPath = ConfigurationManager.getProperty(Constants.LOCAL_MODEL_PATH);
        }else {
            modelPath = ConfigurationManager.getProperty(Constants.HDFS_MODEL_PATH);
        }
        model.save(sc.sc(),modelPath+modelName);
        if(!onLocal) {
            Jedis jedis = RedisClient.pool().getResource();
            jedis.select(ConfigurationManager.getInteger(Constants.REDIS_DB_INDEX));
            String modelKey = ConfigurationManager.getProperty(Constants.REDIS_MODEL_KEY);
            jedis.hset(modelKey, modelName, modelPath + modelName);
            jedis.close();
        }
        showPredictingPrecision(predictAndLabels);
    }



    /**
     * 模型精度输出
     * @param predictAndLabels
     */
    public static void showPredictingPrecision(JavaRDD<Tuple2<Object,Object>> predictAndLabels){
        predictAndLabels.foreach(x->{
            System.out.println("预测类型："+x._1+"\t真实类型"+x._2);
        });
        double correct=0,total=0;
        List<Tuple2<Object, Object>> collect = predictAndLabels.collect();
        for(Tuple2<Object,Object> t:collect){
            double p = (Double) t._1;
            double r = (Double) t._2;
            if(p==r){
                correct++;
            }
            total++;
        }
        System.out.println("正确率："+(correct/total));

        MulticlassMetrics metrics = new MulticlassMetrics(predictAndLabels.rdd());
        System.out.println("precision:"+metrics.precision());
        System.out.println("recall :"+metrics.recall());


    }

    /**
     * local/remote获取模型路径
     * @param name
     * @param onLocal
     * @return
     */
    public static String getModelPathByName(String name, Boolean onLocal) {
        if(onLocal){
            String modelPath = ConfigurationManager.getProperty(Constants.LOCAL_MODEL_PATH);
            return modelPath+name;
        }else {
            return JedisUtil.hget(ConfigurationManager.getProperty(Constants.REDIS_MODEL_KEY), name);
        }
    }

}
