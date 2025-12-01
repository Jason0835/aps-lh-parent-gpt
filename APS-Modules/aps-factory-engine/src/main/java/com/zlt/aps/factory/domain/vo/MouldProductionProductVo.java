package com.zlt.aps.factory.domain.vo;

import lombok.Data;

/**
 * 模具正在硫化的物料信息
 *
 * @author ZLT
 * @date 20250219
 */
@Data
public class MouldProductionProductVo {

    /**
     * 分厂编号
     */
    private String factoryCode;

    /**
     * 物料编号
     */
    private String productCode;

    /**
     * 模具号
     */
    private String mouldCode;
    /**
     * 规格代号
     */
    private String specCode;
    /**
     * 排产分组信息
     */
    private String productionGroupValue;

    /**
     * 排产分组-排产模台数
     */
    private Integer mouldQty;
    /**
     * 排产分组-本身模台数
     */
    private Integer mouldNumber;
}
