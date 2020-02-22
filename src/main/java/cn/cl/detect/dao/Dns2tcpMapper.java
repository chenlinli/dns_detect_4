package cn.cl.detect.dao;

import cn.cl.detect.domain.Dns2tcp;
import org.apache.ibatis.annotations.Param;

import java.util.HashSet;
import java.util.List;

public interface Dns2tcpMapper {
    int deleteByPrimaryKey(Long id);

    int insert(Dns2tcp record);

    int insertSelective(Dns2tcp record);

    Dns2tcp selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Dns2tcp record);

    int updateByPrimaryKey(Dns2tcp record);

    List<String> selectByIds(@Param("ids") HashSet<Long> ids);
}