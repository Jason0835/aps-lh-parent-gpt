package com.zlt.aps.cd90.engine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cd90.api.domain.entity.Cd90Params;
import com.zlt.aps.cd90.engine.constant.Cd90RollingParamCode;
import com.zlt.aps.cd90.engine.mapper.Cd90AutoScheduleParamsMapper;
import com.zlt.aps.cd90.engine.model.Cd90RollingParameters;
import com.zlt.aps.cd90.engine.service.Cd90RollingParameterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 定时滚动参数服务实现。 */
@Service
@RequiredArgsConstructor
public class Cd90RollingParameterServiceImpl implements Cd90RollingParameterService {

    private static final int DEFAULT_EARLY_MINUTES = 30;
    private static final int DEFAULT_LATE_MINUTES = 15;
    private static final int DEFAULT_STABLE_MINUTES = 5;

    private final Cd90AutoScheduleParamsMapper paramsMapper;

    /** 加载滚动专用参数，不扩大全量自动排程必填参数集合。 */
    @Override
    public Cd90RollingParameters load(String factoryCode) {
        Map<String, Cd90Params> values = paramsMapper.selectList(
                        new LambdaQueryWrapper<Cd90Params>()
                                .eq(Cd90Params::getFactoryCode, factoryCode)
                                .in(Cd90Params::getParamCode, Cd90RollingParamCode.ALL_CODES))
                .stream().collect(Collectors.toMap(Cd90Params::getParamCode,
                        Function.identity(), (first, second) -> first));
        return Cd90RollingParameters.builder()
                .earlyMinutes(this.nonNegative(values, Cd90RollingParamCode.EARLY_MINUTES,
                        DEFAULT_EARLY_MINUTES))
                .lateMinutes(this.nonNegative(values, Cd90RollingParamCode.LATE_MINUTES,
                        DEFAULT_LATE_MINUTES))
                .stableMinutes(this.nonNegative(values, Cd90RollingParamCode.STABLE_MINUTES,
                        DEFAULT_STABLE_MINUTES))
                .build();
    }

    /** 解析非负分钟参数，空值或非法值回退到默认值。 */
    private int nonNegative(Map<String, Cd90Params> values, String code, int defaultValue) {
        Cd90Params parameter = values.get(code);
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
