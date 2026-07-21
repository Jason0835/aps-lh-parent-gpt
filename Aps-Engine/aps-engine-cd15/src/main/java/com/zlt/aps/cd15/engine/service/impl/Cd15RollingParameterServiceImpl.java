package com.zlt.aps.cd15.engine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cd15.api.domain.entity.Cd15Params;
import com.zlt.aps.cd15.engine.constant.Cd15RollingParamCode;
import com.zlt.aps.cd15.engine.mapper.Cd15AutoScheduleParamsMapper;
import com.zlt.aps.cd15.engine.model.Cd15RollingParameters;
import com.zlt.aps.cd15.engine.service.Cd15RollingParameterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 定时滚动参数服务实现。 */
@Service
@RequiredArgsConstructor
public class Cd15RollingParameterServiceImpl implements Cd15RollingParameterService {

    private static final int DEFAULT_EARLY_MINUTES = 30;
    private static final int DEFAULT_LATE_MINUTES = 15;
    private static final int DEFAULT_STABLE_MINUTES = 5;

    private final Cd15AutoScheduleParamsMapper paramsMapper;

    /** 加载滚动专用参数，不扩大全量自动排程必填参数集合。 */
    @Override
    public Cd15RollingParameters load(String factoryCode) {
        Map<String, Cd15Params> values = paramsMapper.selectList(
                        new LambdaQueryWrapper<Cd15Params>()
                                .eq(Cd15Params::getFactoryCode, factoryCode)
                                .in(Cd15Params::getParamCode, Cd15RollingParamCode.ALL_CODES))
                .stream().collect(Collectors.toMap(Cd15Params::getParamCode,
                        Function.identity(), (first, second) -> first));
        return Cd15RollingParameters.builder()
                .earlyMinutes(this.nonNegative(values, Cd15RollingParamCode.EARLY_MINUTES,
                        DEFAULT_EARLY_MINUTES))
                .lateMinutes(this.nonNegative(values, Cd15RollingParamCode.LATE_MINUTES,
                        DEFAULT_LATE_MINUTES))
                .stableMinutes(this.nonNegative(values, Cd15RollingParamCode.STABLE_MINUTES,
                        DEFAULT_STABLE_MINUTES))
                .build();
    }

    /** 解析非负分钟参数，空值或非法值回退到默认值。 */
    private int nonNegative(Map<String, Cd15Params> values, String code, int defaultValue) {
        Cd15Params parameter = values.get(code);
        if (parameter == null || parameter.getParamValue() == null) {
            return defaultValue;
        }
        try {
            return Math.max(0, Integer.parseInt(parameter.getParamValue().trim()));
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }
}
