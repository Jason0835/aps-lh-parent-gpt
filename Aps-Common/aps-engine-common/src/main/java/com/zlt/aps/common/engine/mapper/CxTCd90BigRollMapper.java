package com.zlt.aps.common.engine.mapper;

import com.zlt.aps.common.engine.domain.CxTCd90BigRoll;

import java.util.List;

/**
 * @Entity com.zlt.aps.common.engine.domain.CxTCd90BigRoll
 */
public interface CxTCd90BigRollMapper {

    int deleteByPrimaryKey(Long id);

    int insert(CxTCd90BigRoll record);

    int insertSelective(CxTCd90BigRoll record);

    CxTCd90BigRoll selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(CxTCd90BigRoll record);

    int updateByPrimaryKey(CxTCd90BigRoll record);

    int deleteByIds(Long[] ids);

//    int insertBatch(List<CxTCd90BigRoll> cxTCd90BigRollCollection);

    List<CxTCd90BigRoll> getByParams(CxTCd90BigRoll entity);

    List<CxTCd90BigRoll> getByCrodSpecList(List<String> list);
}




