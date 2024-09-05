package com.zlt.aps.cx.engine.domain;

import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.Date;

/**
 * 硫化工序在产规格对象
 */
@ApiModel(value = "硫化工序在产规格对象", description = "硫化工序在产规格对象")
@Data
public class MesLhProductionSpec extends ApsBaseEntity {

    private Long id;

    /**
     * 生产日期
     */
    private Date productDate;

    /**
     * 硫化机台编号
     */
    private String lhMachineCode;

    /**
     * 蒸锅编号
     */
    private String lhStreamCode;

    /**
     * sap品号
     */
    private String sapCode;

    /**
     * 左右模信息
     */
    private String leftRightMold;

    /**
     * 生产日期yyyyMMdd搜索条件
     */
    private String productDateStr;
}
