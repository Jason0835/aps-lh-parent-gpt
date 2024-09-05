package com.zlt.aps.common.engine.service.impl;

import com.zlt.aps.common.engine.domain.CxTCd15Params;
import com.zlt.aps.common.engine.domain.XwyyParamsVo;
import com.zlt.aps.common.engine.mapper.CxTCd15ParamsMapper;
import com.zlt.aps.common.engine.service.CxTCd15ParamsService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Gim
 */
@Service
public class CxTCd15ParamsServiceImpl implements CxTCd15ParamsService {

    @Resource
    private CxTCd15ParamsMapper mapper;

    @Override
    public int add(CxTCd15Params entity) {
        return mapper.insert(entity);
    }

    @Override
    public int update(CxTCd15Params timeLimit) {
        return mapper.updateByPrimaryKey(timeLimit);
    }

    @Override
    public CxTCd15Params getById(Long id) {
        return mapper.selectByPrimaryKey(id);
    }

    @Override
    public CxTCd15Params getByParamCode(String paramCode) {
        return mapper.selectOneByParamCodeAndDelFlag(paramCode, "0");
    }

    @Override
    public List<XwyyParamsVo> listGdyyParams() {
        return mapper.listGdyyParams();
    }
}
