package com.zlt.aps.monthplan.api.domain.dto;

import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 月计划定稿事件Dto
 *
 * @author Chen
 * @since 2026/1/15
 */
@Data
public class MonthPlanFinalizedEventDto implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 年
     */
    private Integer year;

    /**
     * 月
     */
    private Integer month;

    /**
     * 工厂
     */
    private String factoryCode;

    /**
     * 月计划版本
     */
    private String monthPlanVersion;

    /**
     * 物料总量Map
     */
    private Map<String, Integer> materialTotalQtyMap;

    /**
     * 定稿参数
     */
    private FactoryMonthPlanProductionFinalResult param;

    /**
     * 定稿结果数据
     */
    private List<FactoryMonthPlanProductionFinalResult> finalList;
}
