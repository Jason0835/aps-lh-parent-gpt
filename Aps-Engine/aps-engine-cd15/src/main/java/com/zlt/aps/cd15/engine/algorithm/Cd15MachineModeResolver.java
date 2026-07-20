package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.constant.Cd15CutMode;
import com.zlt.aps.cd15.engine.model.Cd15MachineResource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 集中解析斜裁机台的裁断模式与对应班产能力。
 */
@Component
public class Cd15MachineModeResolver {

    /**
     * 校验任务裁断模式与机台主数据是否一致。DAILY_OUTPUT为双模式机台，
     * 不再按日产量阈值切换，直接按任务是单裁还是分裁校验支持能力。
     */
    public boolean matches(Cd15MachineResource machine,
                           boolean splitCut) {
        if (machine == null) {
            return false;
        }
        String requiredMode = splitCut ? Cd15CutMode.SPLIT : Cd15CutMode.SINGLE;
        String configuredMode = this.normalize(machine.getDefaultCutMode());
        if (!Cd15CutMode.SINGLE.equals(configuredMode)
                && !Cd15CutMode.SPLIT.equals(configuredMode)
                && !Cd15CutMode.DAILY_OUTPUT.equals(configuredMode)) {
            throw new IllegalStateException(
                    "机台未维护有效默认裁断模式: " + machine.getMachineCode());
        }
        if (!Cd15CutMode.DAILY_OUTPUT.equals(configuredMode)
                && !requiredMode.equals(configuredMode)) {
            return false;
        }
        return splitCut ? machine.isSplitCutSupported()
                : machine.isSingleCutSupported();
    }

    /** 获取任务当前模式的满班能力，不使用历史生产定额和日产量阈值。 */
    public BigDecimal capacity(Cd15MachineResource machine,
                               boolean splitCut) {
        if (!this.matches(machine, splitCut)) {
            return BigDecimal.ZERO;
        }
        BigDecimal modeCapacity = splitCut
                ? machine.getSplitShiftCapacity() : machine.getSingleShiftCapacity();
        return modeCapacity == null ? BigDecimal.ZERO : modeCapacity;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

}