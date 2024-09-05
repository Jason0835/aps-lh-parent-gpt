package com.zlt.aps.common.engine.domain;

import com.zlt.aps.common.core.domain.ApsBaseEntity;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author Gim
 */
@Data
public class MonthPlanSurplusBaseEntity extends ApsBaseEntity {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 生产排程记录主计划版本号,年+月+日+01，02
     */
    private String monthPlanApsVersion;

    /**
     * 主计划版本号
     */
    private String monthPlanVersion;

    /**
     * 主计划所属年份
     */
    private String year;

    /**
     * 主计划所属月份
     */
    private String month;

    /**
     * 物料编码
     */
    private String materialCode;

    /**
     * 月度计划量
     */
    private BigDecimal monthPlanQty = BigDecimal.ZERO;

    /**
     * 月度计划调整量
     */
    private BigDecimal monthPlanModifyQty = BigDecimal.ZERO;

    /**
     * 月度完成量
     */
    private BigDecimal monthFinishQty = BigDecimal.ZERO;

    /**
     * 月剩余量
     */
    private BigDecimal monthRemainQty = BigDecimal.ZERO;
}
