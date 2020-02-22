package cn.cl.detect.dao;

import cn.cl.detect.domain.ActionDnscat2;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface ActionDnscat2Mapper {
    int deleteByPrimaryKey(Integer id);

    int insert(ActionDnscat2 record);

    int insertSelective(ActionDnscat2 record);

    ActionDnscat2 selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(ActionDnscat2 record);

    int updateByPrimaryKey(ActionDnscat2 record);

    public List<ActionDnscat2> selectByTimeScope(@Param("start") Date start, @Param("end") Date currentEnd);
}