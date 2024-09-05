package com.zlt.aps.mps.mapper;

import com.zlt.aps.mps.domain.TLhClassShiftFinishQty;

/**
 * @Entity com.zlt.aps.mps.domain.TLhClassShiftFinishQty
 */
public interface TLhClassShiftFinishQtyMapper {

    int deleteByPrimaryKey(Long id);

    int insert(TLhClassShiftFinishQty record);

    int insertSelective(TLhClassShiftFinishQty record);

    TLhClassShiftFinishQty selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(TLhClassShiftFinishQty record);

    int updateByPrimaryKey(TLhClassShiftFinishQty record);

}




