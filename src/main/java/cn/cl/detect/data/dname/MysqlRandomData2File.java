package cn.cl.detect.data.dname;

import cn.cl.detect.dao.Dns2tcpMapper;
import cn.cl.detect.dao.Dnscat2Mapper;
import cn.cl.detect.dao.IodineMapper;
import cn.cl.detect.dao.NormalRecordMapper;
import cn.cl.detect.util.DomainUtil;
import cn.cl.detect.util.SqlSessionFactoryUtil;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 数据准备：不考虑顶级域名
 1. umbrella 随机10万条数据，写入文件 normal.txt，交给之后的程序处理文件
 2. umbrella 随机训练数据 4万:
 3. dns2tcp: 2万
 Key 25（1万）: 255130
 TXT 16（1万）： 290882
 4. dnscat2 随机1.95万
 15	 MX （6500） 79596
 16	CNAME （6500 ）79927
 5	TXT （6500）79854
 4. iodine  随机抽取 2.8万
 A 1(4000) 200188
 CNAME 5 (4000) 4053
 TXT 16 (4000) 23302
 NULL 10 (4000) 35601
 MX 15 (4000) 27953
 SRV 33 (4000) 14944
 Unknow 65399（4000）  61504
 *
 * 将训练数据4万+2万+1.95万+2万：条插入hive表：online_domian_train(id,domain_name，label)
 *

 */
public class MysqlRandomData2File {
    //数据库各个类别的数据总数
    private static final int UMBRELLA_TOTAL_COUNT = 1000000;  //一百万条记录
    private static final int DNS2TCP_TOTAL_COUNT = 546012;
    private static final int DNSCAT2_TOTAL_COUNT = 239377;
    private static final int IODINE_TOTAL_COUNT = 367545;

    //构造table的umbrella的数据
    private static final int UMBRELLA_BASE_COUNT = 500000;
    //要训练的数据
    private static final int UMBRELLA_TRAIN_COUNT = 100000;
    private static final int DNS2TCP_TRAIN_COUNT = 30000;
    private static final int DNSCAT2_TRAIN_COUNT = 34500;
    private static final int IODINE_TRAIN_COUNT = 56000;

    private static final String NORMAL = "0";
    private static final String DNS2TCP = "1";
    private static final String DNSCAT2 = "2";
    private static final String IODINE = "3";

    /*
    umbrella前50万条数据，作为table构造源
     */
    public static void tabelDataPrepare(){
        SqlSessionFactory sqlSessionFactory = SqlSessionFactoryUtil.getInstance();
        SqlSession sqlSession = sqlSessionFactory.openSession(true);
        NormalRecordMapper mapper = sqlSession.getMapper(NormalRecordMapper.class);

        List<String> names = mapper.selectByIdScope(1, UMBRELLA_BASE_COUNT);
        File file = new File("names_for_table");
        BufferedWriter bufferedWriter = null;
        if(file.exists()){
            file.delete();
        }
        try {
            file.createNewFile();
            bufferedWriter = new BufferedWriter(new FileWriter(file));
            for(String name : names){
                int index = name.lastIndexOf(".");
                if(index != -1) {
                    bufferedWriter.write(name.substring(0, index) + "\n");
                }else{
                    //System.out.println(name);
                    bufferedWriter.write(name + "\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                bufferedWriter.flush();
                bufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
    //umbrella 数据：2份 200000 50000
    public static void umbrellaDataPrepare(){
        Random rand = new Random();
        Set<Long> idsForTrain = new HashSet<>();
        int bound = UMBRELLA_TOTAL_COUNT-UMBRELLA_BASE_COUNT;
        while (idsForTrain.size()<UMBRELLA_TRAIN_COUNT){
            long id = UMBRELLA_BASE_COUNT +rand.nextInt(bound)+1;
            idsForTrain.add(id);
        }

        SqlSessionFactory sqlSessionFactory = SqlSessionFactoryUtil.getInstance();
        SqlSession sqlSession = sqlSessionFactory.openSession(true);
        NormalRecordMapper mapper = sqlSession.getMapper(NormalRecordMapper.class);

        List<String> namesForTrain = mapper.selectByIds(idsForTrain);
        writeRecord2File(namesForTrain,"umbrella_names_for_train",NORMAL);
        sqlSession.close();
    }

    /**
     * 将域名list和其他信息写入文件
     * @param namesForTrain
     * @param fileName
     * @param type
     */
    private static void writeRecord2File(List<String> namesForTrain, String fileName, String type) {
        HashSet<String> namesSet = new HashSet<>();
        File file = new File(fileName);
        BufferedWriter bufferedWriter = null;
        if(file.exists()){
            file.delete();
        }
        try {
            file.createNewFile();
            bufferedWriter = new BufferedWriter(new FileWriter(file));
            for(String name : namesForTrain){
                if(namesSet.contains(name)){
                    continue;
                }else{
                    namesSet.add(name);
                }
                //转小写，去除最后的.（如果存在）
                name = DomainUtil.domainDealing(name);
                bufferedWriter.write( name + "\t" + type+ "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                bufferedWriter.flush();
                bufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    //dns2tcp
    /**
     Key 5: 255130
     TXT 16 ： 290882
     */
    public static void dns2tcpDataPrepare() {
        Random random = new Random();
        HashSet<Long> idsForTrain = new HashSet<>();
        HashSet<String> namesSet = new HashSet<>();

        //30000训练数据
        while (idsForTrain.size()<DNS2TCP_TRAIN_COUNT){
            long id = random.nextInt(DNS2TCP_TOTAL_COUNT) + 1;
            idsForTrain.add(id);
        }

        SqlSessionFactory sqlSessionFactory = SqlSessionFactoryUtil.getInstance();
        SqlSession sqlSession = sqlSessionFactory.openSession(true);

        Dns2tcpMapper mapper = sqlSession.getMapper(Dns2tcpMapper.class);
        List<String> trainData = mapper.selectByIds(idsForTrain);
        writeRecord2File(trainData,"dns2tcp_names_for_train",DNS2TCP);
        sqlSession.close();
    }

    public static void dnscat2DataPrepare(){
        Random random = new Random();
        HashSet<Long> idsForTrain = new HashSet<>();
        HashSet<String> namesSet = new HashSet<>();

        while (idsForTrain.size()<DNSCAT2_TRAIN_COUNT){
            long id = random.nextInt(DNSCAT2_TOTAL_COUNT);
            idsForTrain.add(id);
        }
        SqlSessionFactory sqlSessionFactory = SqlSessionFactoryUtil.getInstance();
        SqlSession sqlSession = sqlSessionFactory.openSession(true);

        Dnscat2Mapper mapper = sqlSession.getMapper(Dnscat2Mapper.class);
        List<String> data = mapper.selectByIds(idsForTrain);
        writeRecord2File(data,"dnscat2_names_for_train",DNSCAT2);
        sqlSession.close();
    }

    public static void iodineDataPrepare(){
        Random random = new Random();
        HashSet<Long> idsForTrain = new HashSet<>();
        HashSet<String> namesSet = new HashSet<>();

        while (idsForTrain.size()<IODINE_TRAIN_COUNT){
            long id = random.nextInt(IODINE_TOTAL_COUNT);
            idsForTrain.add(id);
        }
        SqlSessionFactory sqlSessionFactory = SqlSessionFactoryUtil.getInstance();
        SqlSession sqlSession = sqlSessionFactory.openSession(true);

        IodineMapper mapper = sqlSession.getMapper(IodineMapper.class);
        List<String> data = mapper.selectByIds(idsForTrain);
        writeRecord2File(data,"iodine_names_for_train",IODINE);
        sqlSession.close();
    }


    public static void main(String[] args) {

//        String s = domainDealing("rcyad\\xc4a\\xcf\\xc6\\xe2Yzm\\xc4Jr\\xc6\\xe2Yzm\\xc4Jr\\xc6\\xe2Yzm\\xc4Jr\\xc6\\xe2Yzm\\xc4Jr\\xc6\\xe2Yzm\\xc4Jr\\xc6\\xe2Yzm\\xc4Jr\\xc6\\xe2Yzm\\xc4.Jr\\xc6\\xe2Yzm\\xc4Jr\\xc6\\xe2Yzm\\xc4Jr\\xc6\\xe2Yzm\\xc4Jr\\xc6\\xe2Yzm\\xc4Jr\\xc6\\xe2Yzm\\xc4Jr\\xc6\\xe2Yzm\\xc4Jr\\xc6\\xe2Yzm\\xc4J.r",false);
//        System.out.println(s);
        tabelDataPrepare();
        umbrellaDataPrepare();
        dns2tcpDataPrepare();
        dnscat2DataPrepare();
        iodineDataPrepare();
    }
}
