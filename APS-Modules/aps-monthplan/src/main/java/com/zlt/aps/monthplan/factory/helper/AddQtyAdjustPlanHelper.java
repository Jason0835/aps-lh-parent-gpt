package com.zlt.aps.monthplan.factory.helper;

import com.zlt.aps.monthplan.api.domain.entity.FactoryProductionVersion;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 调增计划参数辅助类
 *
 * @author ZLT
 * @date 20250607
 */
@Data
public class AddQtyAdjustPlanHelper {
    /**
     * 模具
     */
    private String mouldNo;
    /**
     * SAP代码
     */
    private String productCode;
    /**
     * 寸口
     */
    private BigDecimal proSize;
    /**
     * 开始调整天数
     */
    private Integer startAdjustDay;
    /**
     * 增量
     */
    private Long addQty;
    /**
     * 版本信息
     */
    private FactoryProductionVersion productionVersion;
    /**
     * 最大天数
     */
    private Integer monthMaxDays;
}
