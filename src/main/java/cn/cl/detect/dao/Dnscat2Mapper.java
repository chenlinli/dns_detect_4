package cn.cl.detect.dao;

import cn.cl.detect.domain.Dnscat2;
import org.apache.ibatis.annotations.Param;

import java.util.HashSet;
import java.util.List;

public interface Dnscat2Mapper {
    int deleteByPrimaryKey(Long id);

    int insert(Dnscat2 record);

    int insertSelective(Dnscat2 record);

    Dnscat2 selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Dnscat2 record);

    int updateByPrimaryKey(Dnscat2 record);

    List<String> selectByIds(@Param("ids") HashSet<Long> ids);
}