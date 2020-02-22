package cn.cl.detect.dao;

import cn.cl.detect.domain.ActionIodine;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface ActionIodineMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(ActionIodine record);

    int insertSelective(ActionIodine record);

    ActionIodine selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(ActionIodine record);

    int updateByPrimaryKey(ActionIodine record);

    List<ActionIodine> selectByQtypeAndTimeScope(@Param("q_type") String qtype,
                                                 @Param("start") Date start,
                                                 @Param("end") Date currentEnd);

}