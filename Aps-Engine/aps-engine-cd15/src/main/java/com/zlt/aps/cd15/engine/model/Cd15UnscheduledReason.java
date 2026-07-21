package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

/** 标准未排原因及其失败阶段。 */
@Data
@Builder
public class Cd15UnscheduledReason {

    private String reasonCode;
    private String failStage;
    private String reasonDescription;
}
