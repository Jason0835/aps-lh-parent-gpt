package com.zlt.aps.mp.factory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 结构转产表导出切换次数子表
 * @author hak
 *
 */
@Data
@AllArgsConstructor
public class MpStructureAllocationExportChangeCountVo {
    /**
     * 切换次数
     */
    private Integer changeCount;
    /**
     * 台数
     */
    private Integer machineCount;

    /**
     * 导出数据类型，1：明细记录，5：总计
     */
    private String dataType;
}
