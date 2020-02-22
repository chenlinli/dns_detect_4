package cn.cl.detect.util;

import scala.Tuple2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapUtil {
    /**
     * 从table里获取bigram串对应的频率
     * @param map
     * @param key
     * @return
     * @throws Exception
     */
    public static double getValue(Map<String,String> map, String key) throws Exception {
        if(!map.containsKey(key)){
            throw new Exception(key+" doesn't exists in table! check key");
        }else{
            return Double.parseDouble(map.get(key));
        }
    }

    /**
     * 将黑名单包装为List<Tuple>方便并行划为RDD
     * @param map，有可能size=0,但是不会null
     * @return
     */
    public static List<Tuple2<String, Boolean>> map2List(Map<String, String> map) {
        List<Tuple2<String, Boolean>> res = new ArrayList<>();
        for(Map.Entry<String,String> entry:map.entrySet()){
            res.add(new Tuple2<String,Boolean>(entry.getKey(),Boolean.valueOf(entry.getValue())));
        }
        return res;
    }

}
