package com.zlt.aps.factory.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 模具日排产信息对象
 *
 * @author ZLT
 * @date 20250312
 */
@Data
public class MouldDayProductionVo implements Serializable {
    /**
     * 模具编号
     */
    private String mouldCode;
    /**
     * 模具
     */
    private String mouldNo;
    /**
     * 物料编码
     */
    private String productCode;
    /**
     * 物料对应的寸口
     */
    private BigDecimal proSize;
    /**
     * 计划ID
     */
    private Long monthPlanId;
    /**
     * 规格代号
     */
    private String specCode;
    /**
     * 生胎代号
     */
    private String embryoCode;
    /**
     * 成型法: MACHINE_TYPE
     * 1-1次法
     * 2-2次法
     */
    private String mouldMethod;
    /**
     * 排产数量
     */
    private Long productionQty;
    /**
     * 排产日 1~31
     */
    private Integer productionDate;
    /**
     * 排产类型 0 正常 1 停工日 2 维修日 3 洗模日
     */
    private Integer productionType;
    /**
     * 消耗的硫化时间--到秒
     */
    private BigDecimal usedCuringTime;
}
