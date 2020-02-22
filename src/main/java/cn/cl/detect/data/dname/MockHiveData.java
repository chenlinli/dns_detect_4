package cn.cl.detect.data.dname;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * 从*_names_for_train里读取训练数据，实际项目应该将四个文件导入hive,
 * 通过hive查询出来，这里只是为了训练模型，将数据构建为临时表，统一后续本地和hive操作
 * 避免操作hive数据库时，打包到linux执行，模拟hive的表
 * 只有训练在线模型时使用到
 */
public class MockHiveData {

    public static void mockHiveData(JavaSparkContext sc, SparkSession spark){
        ArrayList<Row> dataList = new ArrayList<>();
        ArrayList<String> paths = new ArrayList<>();
        paths.add("umbrella_names_for_train");
        paths.add("dns2tcp_names_for_train");
        paths.add("dnscat2_names_for_train");
        paths.add("iodine_names_for_train");

        loadData2RowFromFile(paths,dataList);

        JavaRDD<Row> rowRDD = sc.parallelize(dataList);
        StructType onlineDomainSchema = DataTypes.createStructType(Arrays.asList(
                DataTypes.createStructField("domain_name", DataTypes.StringType, true),
                DataTypes.createStructField("label", DataTypes.StringType, true)
        ));

        Dataset<Row> ds = spark.createDataFrame(rowRDD, onlineDomainSchema);
        System.out.println("hive online_domain data mock:");
        ds.show();
        ds.registerTempTable("online_domain");

    }

    private static void loadData2RowFromFile(ArrayList<String> paths, ArrayList<Row> dataList) {
        BufferedReader in = null;
        for(String path:paths){
            try {
                in = new BufferedReader(new FileReader(path));
                String line = null;
                while ((line = in.readLine())!=null){
                    String[] split = line.split("\t");
                    Row row = RowFactory.create(split[0], split[1]);
                    dataList.add(row);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                if(in!=null){
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
