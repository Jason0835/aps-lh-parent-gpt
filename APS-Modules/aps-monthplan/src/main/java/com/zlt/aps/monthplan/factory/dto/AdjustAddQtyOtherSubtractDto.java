package com.zlt.aps.monthplan.factory.dto;

import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;
import com.zlt.aps.monthplan.api.domain.entity.MouldingProductionResultHelper;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 调整--调增时，最大调增量及其它计划需调减量
 *
 * @author ZLT
 * @date 20250402
 */
@Getter
public class AdjustAddQtyOtherSubtractDto implements Serializable {
    /**
     * 最大调增量
     */
    private Long maxAddQty;
    /**
     * 调减计划--调减后
     */
    List<FactoryMonthPlanProdFinal> subtractPlanList;
    /**
     * 调减计划-调减后，调减减硫化时间的模具信息
     */
    private Map<String, MouldingProductionResultHelper> subtractCuringTimeMouldMap;
    /**
     * 调减计划错误信息
     */
    private String checkSubtractErrorInfo;

    /**
     * 构造函数
     *
     * @param maxAddQty        最大可调增量
     * @param subtractPlanList 需调减的调减后的计划
     */
    public AdjustAddQtyOtherSubtractDto(Long maxAddQty, List<FactoryMonthPlanProdFinal> subtractPlanList, Map<String, MouldingProductionResultHelper> subtractCuringTimeMouldMap, String checkSubtractErrorInfo) {
        this.maxAddQty = maxAddQty;
        this.subtractPlanList = subtractPlanList;
        this.subtractCuringTimeMouldMap = subtractCuringTimeMouldMap;
        this.checkSubtractErrorInfo = checkSubtractErrorInfo;
    }
}
