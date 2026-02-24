package com.zlt.aps.monthplan.api.domain.vo;

import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * @author Chen
 * @date 2025/4/2
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SaleFinishVo extends BaseEntity {

    /**
     * 年份
     */
    private Integer year;

    /**
     * 月份
     */
    private Integer month;

    /**
     * 物料号
     */
    private String sapMaterialCode;

    /**
     * 完成量
     */
    private BigDecimal finishQuantity;
}
