package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 单次直裁自动排程的内存计算上下文。
 */
@Data
@Builder
public class Cd90AutoScheduleContext {

    /** 工厂编码。 */
    private String factoryCode;
    /** 排程日期。 */
    private LocalDate scheduleDate;
    /** 本次计算启动时间。 */
    private LocalDateTime startTime;
    /** 页面自动排程启动时冻结的资源快照业务日期。 */
    private LocalDate resourceBaselineDate;
    /** 页面自动排程启动时冻结的资源快照班次。 */
    private String resourceBaselineShiftCode;
    /** 当前执行阶段。 */
    private String currentStage;
    /** 启动时固定的参数快照。 */
    private Cd90AutoScheduleParameters parameters;
    /** 本次输出窗口内按时间排序的班次描述。 */
    private List<Cd90ShiftDescriptor> shifts;
    /** 工厂当前全部启用班次数，用于最终参数复核。 */
    private int enabledShiftCount;
    /** 自动排程全部关键输入的启动版本指纹。 */
    private String inputVersionFingerprint;
}
