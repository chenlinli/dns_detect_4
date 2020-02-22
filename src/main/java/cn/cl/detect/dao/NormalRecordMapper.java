package cn.cl.detect.dao;

import cn.cl.detect.domain.NormalRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

public interface NormalRecordMapper {
    int deleteByPrimaryKey(Long id);

    int insert(NormalRecord record);

    int insertSelective(NormalRecord record);

    NormalRecord selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(NormalRecord record);

    int updateByPrimaryKey(NormalRecord record);


    List<String> selectByIds(@Param("idsFortable") Set<Long> idsForTable);

    List<String> selectByIdScope(@Param("from") int from, @Param("to") int to);

    List<String> selectMainDomainNameWithLimit(@Param("start") int start, @Param("end") int end);
}