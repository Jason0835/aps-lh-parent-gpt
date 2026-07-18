package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 单次斜裁自动排程的内存计算上下文。
 */
@Data
@Builder
public class Cd15AutoScheduleContext {

    /** 工厂编码。 */
    private String factoryCode;
    /** 排程日期。 */
    private LocalDate scheduleDate;
    /** 本次计算启动时间。 */
    private LocalDateTime startTime;
    /** 当前执行阶段。 */
    private String currentStage;
    /** 启动时固定的参数快照。 */
    private Cd15AutoScheduleParameters parameters;
    /** 本次输出窗口内按时间排序的班次描述。 */
    private List<Cd15ShiftDescriptor> shifts;
    /** 工厂当前全部启用班次数，用于最终参数复核。 */
    private int enabledShiftCount;
    /** 成型计划、6点库存和库排资源的启动版本指纹。 */
    private String inputVersionFingerprint;
}
