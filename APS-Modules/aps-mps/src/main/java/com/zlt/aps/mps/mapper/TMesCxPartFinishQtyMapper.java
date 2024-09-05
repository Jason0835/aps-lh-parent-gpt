package com.zlt.aps.mps.mapper;
import java.util.List;

import com.zlt.aps.mps.domain.TCxPartFinishQty;
import org.apache.ibatis.annotations.Param;

import com.zlt.aps.mps.domain.TMesCxPartFinishQty;

/**
 * @Entity com.zlt.aps.mps.domain.TMesCxPartFinishQty
 */
public interface TMesCxPartFinishQtyMapper {

    int deleteByPrimaryKey(Long id);

    int insert(TMesCxPartFinishQty record);

    int insertSelective(TMesCxPartFinishQty record);

    TMesCxPartFinishQty selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(TMesCxPartFinishQty record);

    int updateByPrimaryKey(TMesCxPartFinishQty record);

    List<TMesCxPartFinishQty> getAllByDataVersionAndIsDelete(@Param("dataVersion") String dataVersion);

    void mergeCxPart(List<TCxPartFinishQty> list);

}




