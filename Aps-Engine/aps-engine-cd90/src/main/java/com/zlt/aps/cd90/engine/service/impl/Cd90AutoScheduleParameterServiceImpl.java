package com.zlt.aps.cd90.engine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cd90.api.domain.entity.Cd90Params;
import com.zlt.aps.cd90.engine.constant.Cd90AutoScheduleParamCode;
import com.zlt.aps.cd90.engine.mapper.Cd90AutoScheduleParamsMapper;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleParameters;
import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleParameterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 直裁自动排程参数服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd90AutoScheduleParameterServiceImpl implements Cd90AutoScheduleParameterService {

    private final Cd90AutoScheduleParamsMapper paramsMapper;
    private final Cd90AutoScheduleParameterParser parameterParser;

    @Override
    public Cd90AutoScheduleParameters load(String factoryCode, int enabledShiftCount) {
        log.info("[直裁自动排程] 开始加载参数, factoryCode={}, paramCodeCount={}",
                factoryCode, Cd90AutoScheduleParamCode.ALL_CODES.size());
        try {
            LambdaQueryWrapper<Cd90Params> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Cd90Params::getFactoryCode, factoryCode)
                    .in(Cd90Params::getParamCode, Cd90AutoScheduleParamCode.ALL_CODES)
                    .orderByAsc(Cd90Params::getParamCode);
            List<Cd90Params> params = paramsMapper.selectList(wrapper);
            Cd90AutoScheduleParameters result = parameterParser.parse(factoryCode, params, enabledShiftCount);

            log.info("[直裁自动排程] 参数加载完成, factoryCode={}, scheduleWindow={}, fingerprint={}",
                    factoryCode, result.getScheduleWindow(), result.getFingerprint());
            return result;
        } catch (RuntimeException exception) {
            log.error("[直裁自动排程] 参数加载或校验失败, factoryCode={}, message={}",
                    factoryCode, exception.getMessage(), exception);
            throw exception;
        }
    }
}
