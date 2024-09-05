package com.zlt.aps.common.engine.service.impl;

import com.zlt.aps.common.engine.domain.CxTCd90Params;
import com.zlt.aps.common.engine.domain.XwyyParamsVo;
import com.zlt.aps.common.engine.mapper.CxTCd90ParamsMapper;
import com.zlt.aps.common.engine.service.CxTCd90ParamsService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Gim
 */
@Service
public class CxTCd90ParamsServiceImpl implements CxTCd90ParamsService {

    @Resource
    private CxTCd90ParamsMapper mapper;

    @Override
    public int add(CxTCd90Params entity) {
        return mapper.insert(entity);
    }

    @Override
    public int update(CxTCd90Params timeLimit) {
        return mapper.updateByPrimaryKey(timeLimit);
    }

    @Override
    public CxTCd90Params getById(Long id) {
        return mapper.selectByPrimaryKey(id);
    }

    @Override
    public CxTCd90Params getByParamCode(String paramCode) {
        return mapper.selectOneByParamCodeAndDelFlag(paramCode, "0");
    }

    @Override
    public List<XwyyParamsVo> listXwyyParams() {
        return mapper.listXwyyParams();
    }
}
