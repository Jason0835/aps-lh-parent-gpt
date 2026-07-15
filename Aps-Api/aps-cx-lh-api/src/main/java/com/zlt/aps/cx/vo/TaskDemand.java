package com.zlt.aps.cx.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 硫化任务需求
 *
 * @author APS Team
 */
@Data
@AllArgsConstructor
public class TaskDemand {
    private String taskKey;       // 硫化任务唯一键：lhId
    private int demand;
    private String materialCode;  // 物料编码
    private String productStatus; // 产品状态/计划类型
    private String shiftName;     // 班次名称
}
