package com.zlt.aps.mdm.api.domain.vo;

import lombok.Data;

/**
 * 分厂成型正在生产的品种页面Vo，抓取对象、返回成型法
 */
@Data
public class MdmProductionMoldingPageVo {
    /**
     * 年
     */
    private Long year;
    /**
     * 月
     */
    private Long month;

    /**
     * 物料编号
     */
    private String productCode;
    /**
     * 分厂编号
     */
    private String factoryCode;
    /**
     * 机台编号
     */
    private String machineCode;
    /**
     * 成型法
     */
    private Integer moldingMethod;
}
