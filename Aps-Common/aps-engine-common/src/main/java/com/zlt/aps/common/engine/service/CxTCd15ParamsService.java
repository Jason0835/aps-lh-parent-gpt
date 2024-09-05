package com.zlt.aps.common.engine.service;

import com.zlt.aps.common.engine.domain.CxTCd15Params;
import com.zlt.aps.common.engine.domain.XwyyParamsVo;

import java.util.List;

/**
 * @author Gim
 */
public interface CxTCd15ParamsService {
    int add(CxTCd15Params entity);

    int update(CxTCd15Params timeLimit);

    CxTCd15Params getById(Long id);

    CxTCd15Params getByParamCode(String paramCode);

    List<XwyyParamsVo> listGdyyParams();
}
