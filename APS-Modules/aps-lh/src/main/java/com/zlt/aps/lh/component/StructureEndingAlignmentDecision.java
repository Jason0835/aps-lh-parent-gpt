package com.zlt.aps.lh.component;

import lombok.Data;

/**
 * 结构收尾对齐选机判断结果。
 *
 * <p>承载单次候选机台判断的全部关键输入与结论，供新增选机候选校验、命中标识写入
 * 和过程日志复用；后续换活字块、特殊材料等场景也可直接使用同一判断结果。</p>
 *
 * @author APS
 */
@Data
public class StructureEndingAlignmentDecision {

    /** 是否触发结构收尾对齐约束（同结构在机机台数 &lt; 最低机台数 - 1） */
    private boolean triggered;
    /** 当前候选机台是否允许选择（触发时仅同结构放行，未触发时始终放行） */
    private boolean allowed = true;
    /** 当前待排SKU的结构名称 */
    private String structureName;
    /** 候选机台当前在产物料编码（前物料） */
    private String previousMaterialCode;
    /** 候选机台前物料所属结构名称 */
    private String previousStructureName;
    /** 在机统计使用的班次序号（机台收尾时间所在班次，缺失回退最早可换模班次） */
    private int countingShiftIndex;
    /** 统计班次内同结构在机物理机台数 */
    private int inMachineCount;
    /** 结构最低硫化机台数 */
    private int minimumMachineCount;
    /** 排除原因；未排除时为空 */
    private String excludedReason;
}
