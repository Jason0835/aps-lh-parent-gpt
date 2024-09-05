package com.zlt.aps.lh.engine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.lh.engine.domain.LhEngineParams;
import com.zlt.aps.lh.engine.mapper.LhEngineParamsMapper;
import com.zlt.aps.lh.engine.service.LhEngineParamsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 硫化工序参数
 */
@Slf4j
@Service("lhEngineParamsService")
public class LhEngineParamsServiceImpl extends ServiceImpl<LhEngineParamsMapper, LhEngineParams> implements LhEngineParamsService {

    @Autowired
    private LhEngineParamsMapper paramsMapper;
    @Override
    public LhEngineParams selectParamsById(Long id) {
        LambdaQueryWrapper<LhEngineParams> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LhEngineParams::getDelFlag, ApsConstant.DEL_FLAG_NORMAL);
        wrapper.eq(LhEngineParams::getId, id);
        return paramsMapper.selectOne(wrapper);
    }

    @Override
    public List<LhEngineParams> selectParamsList(LhEngineParams params) {
        return paramsMapper.listParams(params);
    }
}
