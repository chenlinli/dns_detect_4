package cn.cl.detect.dao;

import cn.cl.detect.domain.ActionNormal;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface ActionNormalMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(ActionNormal record);

    int insertSelective(ActionNormal record);

    ActionNormal selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(ActionNormal record);

    int updateByPrimaryKey(ActionNormal record);

    List<ActionNormal> selectByTimeScope(@Param("start") Date start, @Param("end") Date currentEnd);
}