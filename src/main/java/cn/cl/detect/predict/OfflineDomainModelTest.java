package cn.cl.detect.predict;

import cn.cl.detect.config.ConfigurationManager;
import cn.cl.detect.constant.Constants;
import cn.cl.detect.util.ModelUtil;
import org.apache.spark.mllib.classification.LogisticRegressionModel;
import org.apache.spark.mllib.classification.NaiveBayesModel;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.Vectors;
import org.apache.spark.mllib.tree.model.DecisionTreeModel;
import org.apache.spark.mllib.tree.model.RandomForestModel;
import org.apache.spark.sql.SparkSession;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class OfflineDomainModelTest {
    static String rfModelName = ConfigurationManager.getProperty(Constants.ACTION_RANDOM_FOREST_MODEL);
    static String dtModelName = ConfigurationManager.getProperty(Constants.ACTION_DECISION_TREE_MODEL);
    static String lrModelName = ConfigurationManager.getProperty(Constants.ACTION_LR_MODEL);
    static String nbModelName = ConfigurationManager.getProperty(Constants.ACTION_NAIVEBAYES_MODEL);
    //训练好的模型
    static LogisticRegressionModel lrModel = null;
    static DecisionTreeModel dtModel = null;
    static RandomForestModel rfModel = null;
    static NaiveBayesModel nbModel = null;

    public static void main(String[] args) {
        SparkSession spark = SparkSession.builder().appName("test").master("local").getOrCreate();
        init(spark,true);
        BufferedReader in = null;
        try{
            String filename = "action_features/8attr/11types/action_iodine_8";
            in = new BufferedReader(new FileReader(filename));
            String line = null;
            int  j=0,lrCorrect=0,deCorrect=0,rfCorrect=0,nbCorrect =0;
            while ((line = in.readLine())!=null){
                j++;
                String[] split = line.split(",");
                double[] features = new double[split.length];
                for(int i=0;i<split.length;i++){
                    features[i] = Double.parseDouble(split[i]);
                }
                Vector vector = Vectors.dense(features);
                double p1 = lrModel.predict(vector);
                if(p1==0){
                    lrCorrect++;
                }
                double p2 = dtModel.predict(vector);
                if(p2==0){
                    deCorrect++;
                }
                double p3 = rfModel.predict(vector);
                if(p3==0){
                    rfCorrect++;
                }
                double p4 = nbModel.predict(vector);
                if(p4==0){
                    nbCorrect++;
                }
                System.out.println(j+"行： 逻辑回归预测结果："+p1+" 决策树预测结果："+p2+" 随机森林预测结果："+p3+
                        "朴素贝叶斯预测结果："+p4);
            }

            System.out.println("逻辑回归预测结果："+lrCorrect+" 决策树预测结果："+deCorrect+" 随机森林预测结果："+rfCorrect+ "朴素贝叶斯预测结果："+nbCorrect);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
