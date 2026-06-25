package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 当前班次机台试算基础数据只读快照。
 */
@Data
@Builder
public class Cd90MachineResourceSnapshot {

    private List<Cd90MachineResource> machines;
    private List<Cd90MachineRollBinding> bindings;
    private List<Cd90MachineRestriction> restrictions;
    private List<Cd90LossRateRule> lossRateRules;
}
