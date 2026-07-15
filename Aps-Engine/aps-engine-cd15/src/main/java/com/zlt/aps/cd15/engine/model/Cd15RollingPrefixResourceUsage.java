package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** CD15 定时滚动前缀已排结果占用资源。 */
@Data
@Builder
public class Cd15RollingPrefixResourceUsage {

    private Long scheduleResultId;
    private String classField;
    private int classIndex;
    private String steelStripCode;
    private String bigRollCode;
    private String storageLaneCode;
    private BigDecimal steelStripConsumeMeters;
    private BigDecimal bigRollConsumeMeters;
    private Integer allocatedCartCount;
}