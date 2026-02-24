package com.zlt.aps.mdm.api.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author Chen
 * @date 2025/3/28
 */
@Data
public class CxScheduleResultReportVo implements Serializable {

    /**
     * 物料号
     */
    private String productCode;

    /**
     * 寸口
     */
    private BigDecimal proSize;

    /**
     * 条数
     */
    private BigDecimal count;

    /**
     * 总计划量
     */
    private BigDecimal qty;
}
