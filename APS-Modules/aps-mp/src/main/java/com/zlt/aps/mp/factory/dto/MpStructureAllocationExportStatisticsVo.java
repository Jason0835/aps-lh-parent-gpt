package com.zlt.aps.mp.factory.dto;

import java.util.List;

import lombok.Data;

/**
 * 结构转产表导出统计对象
 * 
 * @author hak
 *
 */
@Data
public class MpStructureAllocationExportStatisticsVo {
    /**
     * 工厂
     */
    private String factoryCode;
    /**
     * 年份
     */
    private Integer year;
    /**
     * 月份
     */
    private Integer month;
    /**
     * 产品品类 数据字典：biz_product_type 全钢 PCR 半钢
     */
    private String productTypeCode;
    /**
     * 需求计划版本
     */
    private String monthPlanVersion;
    /**
     * 排产版本号
     */
    private String productionVersion;
    /**
     * 英寸交替
     */
    private Integer proSizeChangeCount;
    /**
     * 结构切换
     */
    private Integer structureChangeCount;
    /**
     * 头部行
     */
    private List<MpStructureAllocationExportVo> headList;
    /**
     * 主表明细记录（含小计、合计行等所有内容）
     */
    private List<MpStructureAllocationExportVo> recordList;
    /**
     * 机台切换次数统计
     */
    private List<MpStructureAllocationExportChangeCountVo> changeCountList;
}
