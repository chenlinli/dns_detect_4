package cn.cl.detect.util;

import cn.cl.detect.dao.*;
import cn.cl.detect.domain.*;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

/**
 * MyBatis下，全局唯一SqlSessionFactory，使用单例模式获取
 */
public class SqlSessionFactoryUtil {

    //首先创建静态成员变量sqlSessionFactory，静态变量被所有的对象所共享。
    private static SqlSessionFactory sqlSessionFactory = null;


    private SqlSessionFactoryUtil() {}
    
    //使用静态代码块保证线程安全问题
    static{
        
        String resource = "SqlMapConfig.xml";
        try {
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
            
        } catch (IOException e) {

            e.printStackTrace();
        }
        
    }

    public static SqlSessionFactory getInstance() {
        return sqlSessionFactory;
    }

    public static void main(String[] args) {
        SqlSessionFactory sqlSessionFactory = SqlSessionFactoryUtil.getInstance();
        //自动提交：为false则需要手动提交
        SqlSession sqlSession = sqlSessionFactory.openSession(true);
//        NormalRecordMapper mapper = sqlSession.getMapper(NormalRecordMapper.class);
//        NormalRecord normalRecord = mapper.selectByPrimaryKey(1l);
//        System.out.println(normalRecord);
//        Dns2tcpMapper mapper = sqlSession.getMapper(Dns2tcpMapper.class);
//        System.out.println(mapper.selectByPrimaryKey(1l));
//        HashSet<Long> ids = new HashSet<>();
//        ids.add(1l);
//        ids.add(2l);
//        List<String> names = mapper.selectByIds(ids);
//        names.stream().forEach(n-> System.out.println(n));

//        ActionTimeScopeMapper mapper = sqlSession.getMapper(ActionTimeScopeMapper.class);
//        ActionTimeScope timeScopeBySource = mapper.getTimeScopeBySource("1");
//        System.out.println(timeScopeBySource);
//        Date startTime = mapper.getStartTime("1");
//        System.out.println(startTime);

//        ActionDns2tcpMapper mapper = sqlSession.getMapper(ActionDns2tcpMapper.class);
        ActionIodineMapper mapper = sqlSession.getMapper(ActionIodineMapper.class);
        List<ActionIodine> actionDns2tcpList = mapper.selectByQtypeAndTimeScope("25",
                DateUtil.getDate("2020-02-04 12:23:55"),
                DateUtil.getDate("2020-02-04 12::55"));
        System.out.println(actionDns2tcpList.size());
//        new ActionDns2tcp();
//        new ActionDns2tcp(1,"","","","","","",new Date(),1,"",1);
        sqlSession.close();

    }

}