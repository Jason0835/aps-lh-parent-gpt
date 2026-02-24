package com.zlt.aps.monthplan.api.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 日排产信息对象
 *
 * @author ZLT
 * @date 20250324
 */
@Data
public class DayProductionInfoVo {
    /**
     * 日在月中所处天数
     * 1~31
     */
    private Integer day;
    /**
     * 排产物料列表，按顺序
     */
    private List<ProductProductionInfoVo> productionList;
}
