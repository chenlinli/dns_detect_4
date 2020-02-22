package cn.cl.detect.util;

import cn.hutool.core.util.ReUtil;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.mllib.evaluation.MulticlassMetrics;
import org.apache.spark.sql.sources.In;
import org.pcap4j.packet.DnsDomainName;
import scala.Tuple2;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class DomainUtil {
    /**
     * 域名预处理，转小写，去除结尾的.（如果有），去除顶级域名
     * @param queryDomain
     * @param deleteTopName
     * @return
     */
    public static String domainDealing(String queryDomain, boolean deleteTopName){
        //去除最后的.
        queryDomain = queryDomain.toLowerCase();
        if(queryDomain.charAt(queryDomain.length()-1) == '.') {
            queryDomain = queryDomain.substring(0, queryDomain.length() - 1);
        }
        //不需要去除顶级域
        if(!deleteTopName){
            return queryDomain;
        }
        int i = queryDomain.lastIndexOf(".");
        if(i != -1) {
            queryDomain = queryDomain.substring(0,i);
        }
        return queryDomain;
    }

    /**
     * 域名预处理：转小写，去除结尾的.（如果有）
     * @param queryDomain
     * @return
     */
    public static String domainDealing(String queryDomain){
       return domainDealing(queryDomain,false);
    }

    /**
     * 获得域名对应的特征值数组
     * @param map
     * @param domain：预处理后的域名
     * @return
     * @throws Exception
     */
    public static double[] getDomainFeatures(Map<String, String> map ,String domain) throws Exception {
        String malRegex = "[^a-zA-Z0-9\\.\\-]";
        String letterRegex = "[a-z]";
        String numRegex = "[0-9]";
        double[] features = new double[6];
        int domainLength = domain.length();  //域名长度
        features[1] = domainLength;
        int letterSize = ReUtil.findAll(letterRegex, domain, 0).size(); //字母个数
        int numSize = ReUtil.findAll(numRegex, domain, 0).size(); //数字个数
        String dealtDomain = ReUtil.replaceAll(domain,malRegex,"");
        int malCharSize = domainLength-dealtDomain.length();  //非法字符个数

        //只有去除了非法字符才能计算bigram
        domain = dealtDomain;
        String[] names = domain.split("\\.");
        features[2] = names.length;  //label的长度


        StringBuilder sb = null;
        for (String name : names) {
            name = name.trim();
            if (name == null || name.length() <= 0) {
                continue;
            }
            sb = new StringBuilder("^").append(",").append(name.charAt(0));
            features[0] += Math.abs(Math.log(MapUtil.getValue(map,sb.toString())));
            sb = null;
            for (int i = 0; i < name.length() - 1; i++) {
                sb = new StringBuilder(name.charAt(i)+"").append(",").append(name.charAt(i + 1));
                features[0] += Math.abs(Math.log(MapUtil.getValue(map,sb.toString())));
            }
        }
        features[3] = letterSize*1.0/domainLength;
        features[4] = numSize*1.0/domainLength;
        features[5] = malCharSize*1.0/domainLength;


        //return new Tuple2<String, String>(label, f+","+domainLength+","+labelsLength+","+letterRate+","+numRate+","+malRate);
        //做成libsvm格式
        return features;
    }

    /**
     * ？？？需要定义什么是主域名:二级/三级域名
     * 获取主域名() eg:"baidu.com"
     * @param name： name完整没有预处理的域名：www.baidu.com
     * @return
     */
    public static String getMainDomain(String name){
        String[] split = name.split("\\.");
        int labelCount = split.length;
        //域名级数最大为3则，主域名是两级域名,否则是三级域名
        if(labelCount<=3){
            return split[labelCount-2]+"."+split[labelCount-1];
        }else
            return split[labelCount-3]+"."+split[labelCount-2]+"."+split[labelCount-1];
    }
    /**
     * 根据主域名和完整域名获取主机名
     * @param queryDomain
     * @param mainDomain
     * @return
     */
    public static String getHostName(String queryDomain, String mainDomain) {
        int index = queryDomain.indexOf(mainDomain);
        return queryDomain.substring(0,index);
    }

    /**
     * 两个String类型的值相加
     * @param a
     * @param b
     * @return
     */
    public static String plus(String a,String b){
        return String.valueOf(Integer.parseInt(a)+Integer.parseInt(b));
    }

    /**
     * dns行为分析特征转化
     * @param featurePres：//包个数(0) A个数(1) AAAA个数(2) CNAME个数(3) TXT个数(4) Other个数(5)
     *                   qType(6) anDataLen(7) rDataLen(8) query个数(9) response个数(10) 主机名(11)
     * @param windowSize：窗口时间长度
     * @return
     */
    public static double[] getActionFeatures(String[] featurePres, int windowSize) {
        System.out.println(featurePres);
        double[] features = new double[16]; //16个特征
        double totalCount = Double.parseDouble(featurePres[0]);
        features[0] = totalCount/(windowSize);  //速度
        features[1] = Integer.parseInt(featurePres[8]);    //rDataLen
        features[2] = Integer.parseInt(featurePres[7]); //anDataLen
        int reponseCount = Integer.parseInt(featurePres[10]);
        if(reponseCount!=0){
            features[3] = Double.parseDouble(featurePres[8])/reponseCount; //averageResourceDataLength
        }else{
            features[3] = 0.0;
        }
        features[4] = featurePres[11].split(",").length;  //hosts
        features[5] = featurePres[6].split(",").length;  //qtypes

        features[6] = Integer.parseInt(featurePres[1]);
        features[7] = features[6]/totalCount;
        features[8] = Integer.parseInt(featurePres[2]);
        features[9] = features[8]/totalCount;
        features[10] = Integer.parseInt(featurePres[3]);
        features[11] = features[10]/totalCount;
        features[12] = Integer.parseInt(featurePres[4]);
        features[13] = features[12]/totalCount;
        features[14] = Integer.parseInt(featurePres[5]);
        features[15] = features[14]/totalCount;  //acount
        return features;
    }


    /**
     * dns行为分析特征转化
     * @param featurePres：//包个数(0) A个数(1) AAAA个数(2)
     *                qType(3) anDataLen(4) query个数(5) response个数(6) 主机名(7)
     * @param windowSize：窗口时间长度
     * @return
     */
    public static double[] getActionFeatures8Attr(String[] featurePres, int windowSize) {
        for(int i=0;i<featurePres.length;i++){
            System.out.print(featurePres[i]+"\t");
        }
        System.out.println();
        double[] features = new double[8]; //16个特征
        double totalCount = Double.parseDouble(featurePres[0]);
        features[0] = totalCount/(windowSize);  //速度
        features[1] = Integer.parseInt(featurePres[4]); //anDataLen
        features[2] = featurePres[7].split(",").length;  //hosts
        features[3] = featurePres[3].split(",").length;  //qtypes

        features[4] = Integer.parseInt(featurePres[1]);  //a个数
        features[5] = features[4]/totalCount;
        features[6] = Integer.parseInt(featurePres[2]);  //aaaa个数
        features[7] = features[6]/totalCount;
        return features;
    }
}
