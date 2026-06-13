package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 单个帘布规格的全部候选机台试算及最佳内存方案。
 */
@Data
@Builder
public class Cd90MachineTrialPlan {

    private List<Cd90MachineTrial> trials;
    private Cd90MachineTrial selectedTrial;
}
