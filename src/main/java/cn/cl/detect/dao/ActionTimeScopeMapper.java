package cn.cl.detect.dao;

import cn.cl.detect.domain.ActionTimeScope;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

public interface ActionTimeScopeMapper {
    int insert(ActionTimeScope record);

    int insertSelective(ActionTimeScope record);

    ActionTimeScope getTimeScopeBySource(@Param("source") String source);

    Date getStartTime(@Param("source") String source);
}