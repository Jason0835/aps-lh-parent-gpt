package com.zlt.aps.cx.vo;

import lombok.Data;

/**
 * 单条分配记录：胎胚编码 + 源任务 + 分配到该机台的硫化机台数。
 *
 * @author APS Team
 */
@Data
@lombok.AllArgsConstructor
public class EmbryoAssignment {
    private String embryoCode;
    private DailyEmbryoTask task;
    /** 分配到该机台的硫化机台数（非条数） */
    private int assignedQty;
}
