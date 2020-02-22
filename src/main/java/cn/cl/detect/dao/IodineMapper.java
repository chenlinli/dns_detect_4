package cn.cl.detect.dao;

import cn.cl.detect.domain.Iodine;
import org.apache.ibatis.annotations.Param;

import java.util.HashSet;
import java.util.List;

public interface IodineMapper {
    int deleteByPrimaryKey(Long id);

    int insert(Iodine record);

    int insertSelective(Iodine record);

    Iodine selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Iodine record);

    int updateByPrimaryKey(Iodine record);

    List<String> selectByIds(@Param("ids") HashSet<Long> idsForTrain);
}