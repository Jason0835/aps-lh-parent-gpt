package com.zlt.aps.common.engine.service;

import com.zlt.aps.common.engine.domain.CxTCd90BigRoll;

import java.util.List;

/**
 * @author Gim
 */
public interface CxTCd90BigRollService {
    List<CxTCd90BigRoll> getByParams(CxTCd90BigRoll entity);

    int add(CxTCd90BigRoll entity);

//    void addBatch(List<CxTCd90BigRoll> list);

    int deleteByIds(Long[] ids);

    int update(CxTCd90BigRoll timeLimit);

    CxTCd90BigRoll getById(Long id);

    List<CxTCd90BigRoll> getByCrodSpecList(List<String> list);
}
