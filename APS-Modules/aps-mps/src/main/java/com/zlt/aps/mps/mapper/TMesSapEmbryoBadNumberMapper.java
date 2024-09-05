package com.zlt.aps.mps.mapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;

import com.zlt.aps.mps.domain.TMesSapEmbryoBadNumber;

/**
 * @Entity com.zlt.aps.mps.domain.TMesSapEmbryoBadNumber
 */
public interface TMesSapEmbryoBadNumberMapper {

    int deleteByPrimaryKey(Long id);

    int insert(TMesSapEmbryoBadNumber record);

    int insertSelective(TMesSapEmbryoBadNumber record);

    TMesSapEmbryoBadNumber selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(TMesSapEmbryoBadNumber record);

    int updateByPrimaryKey(TMesSapEmbryoBadNumber record);

    List<TMesSapEmbryoBadNumber> selectAllByDataVersionAndIsDelete(@Param("dataVersion") String dataVersion);
}




