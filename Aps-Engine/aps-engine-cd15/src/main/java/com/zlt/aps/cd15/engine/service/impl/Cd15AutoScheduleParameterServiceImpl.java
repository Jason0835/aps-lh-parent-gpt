package com.zlt.aps.cd15.engine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cd15.api.domain.entity.Cd15Params;
import com.zlt.aps.cd15.engine.constant.Cd15AutoScheduleParamCode;
import com.zlt.aps.cd15.engine.mapper.Cd15AutoScheduleParamsMapper;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleParameters;
import com.zlt.aps.cd15.engine.service.Cd15AutoScheduleParameterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 斜裁自动排程参数服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd15AutoScheduleParameterServiceImpl implements Cd15AutoScheduleParameterService {

    private final Cd15AutoScheduleParamsMapper paramsMapper;
    private final Cd15AutoScheduleParameterParser parameterParser;

    @Override
    public Cd15AutoScheduleParameters load(String factoryCode, int enabledShiftCount) {
        log.info("[斜裁自动排程] 开始加载参数, factoryCode={}, paramCodeCount={}",
                factoryCode, Cd15AutoScheduleParamCode.ALL_CODES.size());
        try {
            LambdaQueryWrapper<Cd15Params> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Cd15Params::getFactoryCode, factoryCode)
                    .in(Cd15Params::getParamCode, Cd15AutoScheduleParamCode.ALL_CODES)
                    .orderByAsc(Cd15Params::getParamCode);
            List<Cd15Params> params = paramsMapper.selectList(wrapper);
            Cd15AutoScheduleParameters result = parameterParser.parse(factoryCode, params, enabledShiftCount);

            log.info("[斜裁自动排程] 参数加载完成, factoryCode={}, scheduleWindow={}, fingerprint={}",
                    factoryCode, result.getScheduleWindow(), result.getFingerprint());
            return result;
        } catch (RuntimeException exception) {
            log.error("[斜裁自动排程] 参数加载或校验失败, factoryCode={}, message={}",
                    factoryCode, exception.getMessage(), exception);
            throw exception;
        }
    }
}
