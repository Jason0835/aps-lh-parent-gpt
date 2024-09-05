package com.zlt.aps.common.engine.mapper;

import com.zlt.aps.common.engine.domain.EmbryoVersionVo;
import com.zlt.aps.common.engine.domain.TSapEmbryoBadNumber;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * @Entity com.zlt.aps.common.engine.domain.TSapEmbryoBadNumber
 */
public interface TSapEmbryoBadNumberMapper {

    int deleteByPrimaryKey(Long id);

    int insert(TSapEmbryoBadNumber record);

    int insertSelective(TSapEmbryoBadNumber record);

    TSapEmbryoBadNumber selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(TSapEmbryoBadNumber record);

    int updateByPrimaryKey(TSapEmbryoBadNumber record);

    List<TSapEmbryoBadNumber> getByParams(TSapEmbryoBadNumber entity);
    List<TSapEmbryoBadNumber> getSapByParams(@Param("badDate") String badDate, @Param("sapCodeList") List<String> sapCodeList);
    List<TSapEmbryoBadNumber> getEmbryoByParams(@Param("badDate") String badDate, @Param("embryoCodeList") List<String> embryoCodeList);
    List<TSapEmbryoBadNumber> getByEmbryoVersionList(@Param("badDate") String badDate, @Param("list") List<EmbryoVersionVo> list);

    void mergeSql(List<TSapEmbryoBadNumber> list);
}




