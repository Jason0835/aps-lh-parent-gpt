package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** 定时滚动排程单项调整及前后快照草稿。 */
@Data
@Builder
public class Cd90RollingAdjustmentDraft {

    private String rollingItemKey;
    private Long scheduleResultId;
    private String clothCode;
    private String bigRollCode;
    private String adjustType;
    private String beforeClassField;
    private Integer beforeProduceOrder;
    private BigDecimal beforeQuantity;
    private String beforeMachineCode;
    private String afterClassField;
    private Integer afterProduceOrder;
    private BigDecimal afterQuantity;
    private String afterMachineCode;
    private String reasonCode;
    private String reasonDetail;
    private Object beforeSnapshot;
    private Object afterSnapshot;
}
