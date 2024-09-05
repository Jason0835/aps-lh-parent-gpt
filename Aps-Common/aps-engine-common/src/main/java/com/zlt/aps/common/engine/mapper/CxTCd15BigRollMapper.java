package com.zlt.aps.common.engine.mapper;

import com.zlt.aps.common.engine.domain.CxTCd15BigRoll;

import java.util.List;

/**
 * @Entity com.zlt.aps.common.engine.domain.CxTCd15BigRoll
 */
public interface CxTCd15BigRollMapper {

    int deleteByPrimaryKey(Long id);

    int insert(CxTCd15BigRoll record);

    int insertSelective(CxTCd15BigRoll record);

    CxTCd15BigRoll selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(CxTCd15BigRoll record);

    int updateByPrimaryKey(CxTCd15BigRoll record);

    int deleteByIds(Long[] ids);

//    int insertBatch(List<CxTCd15BigRoll> cxTCd15BigRollCollection);

    List<CxTCd15BigRoll> getByParams(CxTCd15BigRoll entity);

    List<CxTCd15BigRoll> getByBeltSpecList(List<String> list);
}




