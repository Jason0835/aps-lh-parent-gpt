package com.zlt.aps.cd90.service;

import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleParameters;
import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleParameterService;
import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleInputVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

/** 最终提交前复核自动排程参数版本。 */
@Component
@RequiredArgsConstructor
public class Cd90AutoScheduleVersionVerifier {

    private final Cd90AutoScheduleParameterService parameterService;
    private final Cd90AutoScheduleInputVersionService inputVersionService;

    /** 参数指纹发生变化时拒绝提交，避免覆盖基于旧参数计算的结果。 */
    public void verify(Cd90AutoScheduleContext context) {
        Cd90AutoScheduleParameters current = parameterService.load(
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
