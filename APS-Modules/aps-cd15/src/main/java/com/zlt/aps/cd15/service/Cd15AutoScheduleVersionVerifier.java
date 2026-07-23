package com.zlt.aps.cd15.service;

import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleParameters;
import com.zlt.aps.cd15.engine.service.Cd15AutoScheduleParameterService;
import com.zlt.aps.cd15.engine.service.Cd15AutoScheduleInputVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

/** 最终提交前复核自动排程参数版本。 */
@Component
@RequiredArgsConstructor
public class Cd15AutoScheduleVersionVerifier {

    private final Cd15AutoScheduleParameterService parameterService;
    private final Cd15AutoScheduleInputVersionService inputVersionService;

    /** 参数指纹发生变化时拒绝提交，避免覆盖基于旧参数计算的结果。 */
    public void verify(Cd15AutoScheduleContext context) {
        Cd15AutoScheduleParameters current = parameterService.load(
                context.getFactoryCode(), context.getEnabledShiftCount());
        if (!Objects.equals(context.getParameters().getFingerprint(), current.getFingerprint())) {
            throw new IllegalStateException("自动排程参数已发生变化，请重新发起排程");
        }
        String currentInputVersion = inputVersionService.fingerprint(
                context.getFactoryCode(), context.getScheduleDate(),
                context.getResourceBaselineDate(),
                context.getResourceBaselineShiftCode());
        if (!Objects.equals(context.getInputVersionFingerprint(), currentInputVersion)) {
            throw new IllegalStateException("自动排程关键输入已发生变化，请重新发起排程");
        }
    }
}
