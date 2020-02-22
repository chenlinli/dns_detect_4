package cn.cl.detect.data.dname;


import cn.cl.detect.config.ConfigurationManager;
import cn.cl.detect.config.RedisClient;
import cn.cl.detect.constant.Constants;
import redis.clients.jedis.Jedis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 将umbrella的随机10万条数据的文件
 * 构造为一张bigram表：64*64的二维数组，存入redis
 * $ 0 1 2 3 4 5 6 7 8 9 a b c …… z - 38个字符  大小写等价
 */

public class GenerateBiGramTable {
    //是一个table : character--》hashmap(chari，<character,chari>对应的次数)
//    就是一个38*38的二维数组
    HashMap<Character, HashMap<Character, Double>> table = null;

    //统计38个字符每个字符出现的次数，其中'^'代表开头，没有任何意义。
    HashMap<Character,Long> char2Totalount = null;
    char beginChar = '^';  //为了方便计算p(abc) = p(^|a)*P(a|b)*P(b|c)
    char[] chars = new char[]{
            '^',
            '0','1','2','3','4','5','6','7','8','9',
            'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z',
//            'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z',
            '-'};

    int dbIndex;
    String hashKey = null;

    public void generateTableFromFile2Redis(String path){
        init();
        umbrellaDomains2Tablle(path);
        table2Redis();
    }

    /**
     * 初始化table 和 char2Totalount
     */
    private void init(){
        table = new HashMap<>();
        char2Totalount = new HashMap<>();
        for(int i = 0;i<chars.length;i++){
            table.put(chars[i],new HashMap<>());
            char2Totalount.put(chars[i],0l);
        }

        for(Map.Entry<Character,HashMap<Character,Double>> e:table.entrySet()){
            HashMap<Character, Double> map = e.getValue();
            for(int i= 0;i<chars.length;i++){
                map.put(chars[i],0.0);
            }
        }
        dbIndex = ConfigurationManager.getInteger(Constants.REDIS_DB_INDEX);
        hashKey= ConfigurationManager.getProperty(Constants.REDIS_UMBRELLA_TABLE_NAME);
    }

    /**
     * 条件概率table 存入redis的hashset
     */
    private void table2Redis(){
        Jedis jedis = RedisClient.pool().getResource();
        jedis.select(dbIndex);
        for(Map.Entry<Character,HashMap<Character,Double>> e:table.entrySet()){
            HashMap<Character, Double> map = e.getValue();
            char start = e.getKey();
            for(Map.Entry<Character,Double> en:map.entrySet()) {
                Character end = en.getKey();
                Double f = en.getValue(); //条件概率
                jedis.hset(hashKey,start+","+end,f.toString());

            }
        }

        jedis.close();

    }
    /**
     * 读取文件20万条记录，对每个域名统计bigram出现的次数 baidu --> ^b,ba,ai,id,du
     * @param path
     */
    public void umbrellaDomains2Tablle(String path){
        File file = new File(path);
        BufferedReader in = null;
        try{
            in = new BufferedReader(new FileReader(file));
            String line = null;
            int count = 0;
            while ((line = in.readLine())!=null){
                name2Bigram(line);
                count++;
            }
//            printTable();
//            System.out.println("----------------------------------------");
            generateRealTable(count);
        }catch (IOException e){
            e.printStackTrace();
        } catch (Exception e) {
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



    /**
     * 生成p(a|b)概率的table 也就是"ab"出现的条件概率表格
     * p(a|b) = p(ab)|p(b)
     * @param lineCount :为了计算（^,a）: p(a|^) = p(^a)/p(^) :a开头的行占所有行的比例
     */
    private void generateRealTable(long  lineCount) {
        char2Totalount.put(beginChar,lineCount);
        for(Map.Entry<Character,HashMap<Character,Double>> e:table.entrySet()) {
            char start = e.getKey();
            HashMap<Character, Double> map = e.getValue();
            Long count = char2Totalount.get(start);  //start出现的次数
            for(int i=0;i<chars.length;i++){
                char end = chars[i];
                if(end != beginChar) {  //x,^没有意义，也不可能出现
                    map.put(end, map.get(end) / count); //f(start&&end) /f(start)
                }
            }
        }
    }

    /**
     * 将一个字母拆成bigran
     * baidu --> ^b,ba,ai,id,du
     * @param domianName
     */
    public void name2Bigram(String domianName) throws Exception {
        String[] names = domianName.split("\\.");
        for(String name:names) {
            if (name == null || name.length() <= 0) {
                return;
            }
            char[] array = name.toCharArray();
            char start = '^', end=array[array.length-1] ;
            addValue2OldValue(start, array[0], 1);

            //封装函数
            char2Totalount.put(end,char2Totalount.get(end)+1);
            for (int i = 0; i < array.length - 1; i++) {
                start = array[i];
                end = array[i + 1];
                addValue2OldValue(start, end, 1);
                char2Totalount.put(start,char2Totalount.get(start)+1);
            }

        }
    }

    private HashMap<Character, Double> getMapByStartChar(char start){
        return table.get(start);
    }

    /**
     * 获取table 中<start,end>对应的值（次数/条件概率P(start|end)）
     * @param start
     * @param end
     * @return
     * @throws Exception
     */
    private double getValueByStartAndEnd(char start,char end) throws Exception {
        HashMap<Character, Double> map = getMapByStartChar(start);
        if(map == null) {
            throw new Exception(start + " doesn't exists in table");
        }else {
            if (!map.containsKey(end)) {
                throw new Exception(end + "doesn't exists in table");
            }
            return map.get(end);
        }
    }

    /**
     * table里以start出现后end出现 次数增加 increment
     * @param start
     * @param end
     * @param increment
     * @throws Exception
     */
    private void addValue2OldValue(char start,char end,double increment) throws Exception {
        double old = getValueByStartAndEnd(start, end);
        table.get(start).put(end,old+increment);
    }


    /**
     * 打印条件概率table
     */
    public void printTable(){
        for(Map.Entry<Character,HashMap<Character,Double>> e:table.entrySet()){
            HashMap<Character, Double> map = e.getValue();
            char key = e.getKey();
            for(Map.Entry<Character,Double> en:map.entrySet()){
                System.out.print(key+","+en.getKey()+"="+en.getValue()+"\t");
//                if(en.getValue()==0.0 && en.getKey()!=beginChar){
//                    System.out.println(key+":"+en.getKey());
//                }
            }
            System.out.println();
        }
    }

    /**
     * 打印每个字符出现次数的map
     */
    private void printMap(){
        for(Map.Entry<Character,Long> entry:char2Totalount.entrySet()){
            System.out.print(entry.getKey()+"="+entry.getValue()+"\t");
        }
    }
    public static void main(String[] args) throws Exception {
        GenerateBiGramTable generateBiGramTable = new GenerateBiGramTable();
//        generateBiGramTable.init();
//        generateBiGramTable.umbrellaDomains2Tablle("names_for_table");
//        //generateBiGramTable.name2Bigram("baidu");
        generateBiGramTable.generateTableFromFile2Redis("names_for_table");
        generateBiGramTable.printTable();
         generateBiGramTable.printMap();
    }

}
