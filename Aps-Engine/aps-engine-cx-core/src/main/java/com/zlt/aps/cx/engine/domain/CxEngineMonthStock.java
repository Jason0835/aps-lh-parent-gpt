package com.zlt.aps.cx.engine.domain;

import com.zlt.aps.cx.api.domain.dto.CxMonthStockDto;
import lombok.Data;


/**
 * 成型工序月结库存信息
 */
@Data
public class CxEngineMonthStock extends CxMonthStockDto {

    /**
     * 月结库存查询月份日期
     */
    private String stockMonthStr;
}
