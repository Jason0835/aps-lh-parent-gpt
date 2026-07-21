package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** 候选机台过滤结果及诊断信息。 */
@Data
@Builder
public class Cd15MachineCandidateResolution {
    private List<Cd15MachineCandidate> candidates;
    private List<String> boundMachineCodes;
    private String failureReason;
}
