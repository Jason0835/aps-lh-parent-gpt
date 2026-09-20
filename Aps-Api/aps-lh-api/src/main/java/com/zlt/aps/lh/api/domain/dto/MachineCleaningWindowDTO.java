package com.zlt.aps.lh.api.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * 机台清洗时间窗口。
 *
 * @author APS
 */
@Data
public class MachineCleaningWindowDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 计划自然日内已实际安排的喷砂序号；配对侧共享同一序号 */
    private Integer sandBlastSequence;
    /** 当前运行侧首检条数快照；同一物理事件在单控左右侧守恒分配 */
    private Integer sandBlastFirstInspectionQty;
    /** 首检结束归属班次的标准开始时间，用于跨班边界识别 */
    private Date sandBlastInspectionShiftStartTime;

    /** 来源设备停机计划主键，用于最终处置阶段精确回填排程日期 */
    private Long sourcePlanId;
    /** 机台编码 */
    private String lhCode;
    /** 清洗类型 */
    private String cleanType;
    /** 左右模标识 */
    private String leftRightMould;
    /** 模具号 */
    private String mouldCode;
    /** 清洗开始时间 */
    private Date cleanStartTime;
    /** 清洗结束时间 */
    private Date cleanEndTime;
    /** 清洗结束后机台再次可开产时间 */
    private Date readyTime;
    /** 来源设备停机计划的计划开始时间；只用于清洗与换模原始计划重叠判定，不作为实际清洗开始时间 */
    private Date sourcePlanStartTime;
    /** 来源设备停机计划的计划结束时间；只用于清洗与换模原始计划重叠判定，不作为实际清洗结束时间 */
    private Date sourcePlanEndTime;
    /** 数据来源 */
    private String dataSource;
    /** 仅喷砂处置时的续作物料，离机后仍用于交替计划追溯。 */
    private String continuationMaterialCode;
    /** 仅喷砂处置时的物料名称。 */
    private String continuationMaterialDesc;
    /** 仅喷砂处置时的产品状态。 */
    private String continuationProductStatus;
    /** 同 SKU 置换后接替生产的精确模具号；为空表示没有可用替换模具。 */
    private String replacementMouldCode;
    /** 预占前的原模具可用边界，续作提前结束取消置换时精确恢复。 */
    private Map<String, Date> mouldReleaseTimeBeforeReplacement = new LinkedHashMap<>(4);
    /** 已确认模具离机清洗，不再占用原机台或承接原机台喷砂首检。 */
    private boolean offMachineCleaning;

    /** 清洗备注 */
    private String remark;
}
