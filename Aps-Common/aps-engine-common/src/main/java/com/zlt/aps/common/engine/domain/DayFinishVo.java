package com.zlt.aps.common.engine.domain;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author Gim
 */
@Data
public class DayFinishVo {
    // 物料号
    private String materialCode;
    // 完成量
    private BigDecimal dayFinishQty;

}
