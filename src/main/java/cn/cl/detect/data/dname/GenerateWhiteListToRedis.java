package cn.cl.detect.data.dname;

import cn.cl.detect.config.ConfigurationManager;
import cn.cl.detect.constant.Constants;
import cn.cl.detect.dao.NormalRecordMapper;
import cn.cl.detect.util.JedisUtil;
import cn.cl.detect.util.SqlSessionFactoryUtil;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.util.HashMap;
import java.util.List;

/**
 * 取Alexa知名的前10万个主域名作为白名单
 */
public class GenerateWhiteListToRedis {

    public static void main(String[] args) {
        SqlSessionFactory sqlSessionFactory = SqlSessionFactoryUtil.getInstance();
        SqlSession sqlSession = sqlSessionFactory.openSession();
        NormalRecordMapper mapper = sqlSession.getMapper(NormalRecordMapper.class);
        List<String> whiteNames = mapper.selectMainDomainNameWithLimit(1,100000);
        HashMap<String, String> map = new HashMap<>();
        for(String name:whiteNames){
            map.putIfAbsent(name,"true");
        }
        JedisUtil.hmset(ConfigurationManager.getProperty(Constants.REDIS_DOMAIN_WHITELIST_KEY),map);
    }
}
