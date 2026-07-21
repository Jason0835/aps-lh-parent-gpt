package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 单个钢带规格的全部候选机台试算及最佳内存方案。
 */
@Data
@Builder
public class Cd15MachineTrialPlan {

    private List<Cd15MachineTrial> trials;
    private Cd15MachineTrial selectedTrial;
    private String failureReason;
    private List<String> boundMachineCodes;
}
