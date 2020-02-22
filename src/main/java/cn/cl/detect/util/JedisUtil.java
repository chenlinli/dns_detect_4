package cn.cl.detect.util;

import cn.cl.detect.config.ConfigurationManager;
import cn.cl.detect.config.RedisClient;
import cn.cl.detect.constant.Constants;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Map;
import java.util.Set;

public class JedisUtil {
    private static JedisPool pool = RedisClient.pool();

    //获取hashmap所有的key value对
    public static Map<String,String> hgetAll(String key){
        Jedis jedis = getJedis();
        Map<String, String> res = jedis.hgetAll(key);
        close(jedis);
        return res;
    }

    //获取hashmap的name对应的value
    public static String hget(String key,String name){
        Jedis jedis = getJedis();
        String res = jedis.hget(key, name);
        close(jedis);
        return res;
    }



    public static Jedis getJedis(){
        Jedis j = pool.getResource();
        j.select(ConfigurationManager.getInteger(Constants.REDIS_DB_INDEX));
        return j;
    }

    private static void close(Jedis jedis){
        if(jedis!=null){
            jedis.close();
        }
    }

    //获取set里的所有value
    public static Set<String> smembers(String key) {
        Jedis jedis = getJedis();
        Set<String> values = jedis.smembers(key);
        close(jedis);
//        System.out.println("black_list:"+values);
        if(values.size()==0)
            return null;
        return values;
    }
    //向set里添加1-n个
    public static Long sadd(String key,String ...values){
        Jedis jedis = getJedis();
        Long addSuccessNum = jedis.sadd(key, values); //成功添加的个数
        close(jedis);
        return addSuccessNum;
    }

    //member是否是set的元素
    public static boolean sismedmber(String key,String member){
        Jedis jedis = getJedis();
        Boolean sismember = jedis.sismember(key, member);
        close(jedis);
        return sismember;
    }

    public static void hset(String key, String field,String value) {
        Jedis jedis = getJedis();
        jedis.hset(key,field,value);
        close(jedis);
    }

    public static void hmset(String key, Map<String, String> hash){
        Jedis jedis = getJedis();
        jedis.hmset(key,hash);
        close(jedis);
    }
}
