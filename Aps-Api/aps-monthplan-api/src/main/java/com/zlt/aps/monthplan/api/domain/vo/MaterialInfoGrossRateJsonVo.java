package com.zlt.aps.monthplan.api.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author Chen
 */
@Data
public class MaterialInfoGrossRateJsonVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 公用类型，字典 biz_common_type，1 公用规格 2 外销专用 3 内销专用 4 OE专用
     */
    private String commonType;

    /**
     * 毛利率
     */
    private BigDecimal grossRate;
}
