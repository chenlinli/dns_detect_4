package cn.cl.detect.dao;

import cn.cl.detect.domain.ActionDns2tcp;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface ActionDns2tcpMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(ActionDns2tcp record);

    int insertSelective(ActionDns2tcp record);

    ActionDns2tcp selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(ActionDns2tcp record);

    int updateByPrimaryKey(ActionDns2tcp record);

    List<ActionDns2tcp> selectByQtypeAndTimeScope(@Param("q_type") String qtype,
                                                  @Param("start") Date start,
                                                  @Param("end") Date currentEnd);
}