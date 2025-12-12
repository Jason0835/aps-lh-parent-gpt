package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 原材料月计划差异表实体
 */
@Data
@TableName("T_RAW_MATERIAL_MONTH_DIFF")
public class RawMaterialMonthDiff extends BaseEntity {

    private String factoryCode;
    private Integer year;
    private Integer month;
    private String materialCode;
    private String materialName;

    // 差异类型：新增/减少
    private String diffType;

    // 差异数量
    private BigDecimal diffQty;

    // 上一月数量
    private BigDecimal prevMonthQty;

    // 当前月数量
    private BigDecimal curMonthQty;
}