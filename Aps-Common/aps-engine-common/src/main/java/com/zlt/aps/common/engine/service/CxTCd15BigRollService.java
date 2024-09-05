package com.zlt.aps.common.engine.service;

import com.zlt.aps.common.engine.domain.CxTCd15BigRoll;

import java.util.List;

/**
 * @author Gim
 */
public interface CxTCd15BigRollService {
    List<CxTCd15BigRoll> getByParams(CxTCd15BigRoll entity);

    int add(CxTCd15BigRoll entity);

//    void addBatch(List<CxTCd15BigRoll> list);

    int deleteByIds(Long[] ids);

    int update(CxTCd15BigRoll timeLimit);

    CxTCd15BigRoll getById(Long id);

    List<CxTCd15BigRoll> getByBeltSpecList(List<String> list);
}
