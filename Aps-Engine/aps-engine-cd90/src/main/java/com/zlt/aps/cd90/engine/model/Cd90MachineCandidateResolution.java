package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** 候选机台过滤结果及诊断信息。 */
@Data
@Builder
public class Cd90MachineCandidateResolution {
    private List<Cd90MachineCandidate> candidates;
    private List<String> boundMachineCodes;
    private String failureReason;
}
