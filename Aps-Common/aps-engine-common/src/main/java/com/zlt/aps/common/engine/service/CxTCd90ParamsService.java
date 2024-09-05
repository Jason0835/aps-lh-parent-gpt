package com.zlt.aps.common.engine.service;

import com.zlt.aps.common.engine.domain.CxTCd90Params;
import com.zlt.aps.common.engine.domain.XwyyParamsVo;

import java.util.Collection;
import java.util.List;

/**
 * @author Gim
 */
public interface CxTCd90ParamsService {
    int add(CxTCd90Params entity);

    int update(CxTCd90Params timeLimit);

    CxTCd90Params getById(Long id);

    CxTCd90Params getByParamCode(String paramCode);

    List<XwyyParamsVo> listXwyyParams();
}
