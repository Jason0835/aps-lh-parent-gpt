package com.zlt.aps.factory.enums;

import com.tlt.aps.enums.ProductionSecondSortOptionsEnum;
import com.zlt.aps.factory.domain.vo.MonthPlanManufacturingRequirementVo;

import java.util.Comparator;

/**
 * 排产顺序枚举定义类
 * 根据配置的业务，定义起排序字段及排序方式
 *
 * @author ZLT
 * @date 20250217
 */
public enum PlanProductionSortEnum {
    /**
     * DDD 交付日期早优先 升序
     */
    DELIVERY_DATE(ProductionSecondSortOptionsEnum.DELIVERY_DATE_DUE, Comparator.comparing(MonthPlanManufacturingRequirementVo::getDeliveryDateDue, Comparator.nullsLast(Comparator.naturalOrder()))),
    /**
     * LCB 库位类别优先 升序
     */
    IMPORTANT_CUSTOM(ProductionSecondSortOptionsEnum.LOCATION_INFO, Comparator.comparing(MonthPlanManufacturingRequirementVo::getLocationSortValue, Comparator.nullsLast(Comparator.naturalOrder()))),
    /**
     * ER 急单优先 降序
     */
    CONTINUE(ProductionSecondSortOptionsEnum.CONTINUE, Comparator.comparing(MonthPlanManufacturingRequirementVo::getIsContinue, Comparator.nullsLast(Comparator.naturalOrder())).reversed()),
    /**
     * PFV 利润值高优先 升序
     */
    PROFIT_VALUE(ProductionSecondSortOptionsEnum.PROFIT_VALUE, Comparator.comparing(MonthPlanManufacturingRequirementVo::getProfitGrade, Comparator.nullsLast(Comparator.naturalOrder()))),
    /**
     * ER 急单优先 降序
     */
    EMERGENCY(ProductionSecondSortOptionsEnum.EMERGENCY, Comparator.comparing(MonthPlanManufacturingRequirementVo::getIsEmergency, Comparator.nullsLast(Comparator.naturalOrder())).reversed()),
    /**
     * EP 必保计划优先 降序
     */
    ENSURE_PLAN(ProductionSecondSortOptionsEnum.ENSURE_PLAN, Comparator.comparing(MonthPlanManufacturingRequirementVo::getIsEnsurePlan, Comparator.nullsLast(Comparator.naturalOrder())).reversed()),
    /**
     * IMC 重要客户优先 降序
     */
    DELIVERY_DATE_DUE(ProductionSecondSortOptionsEnum.IMPORTANT_CUSTOM, Comparator.comparing(MonthPlanManufacturingRequirementVo::getIsImportantCustom, Comparator.nullsLast(Comparator.naturalOrder())).reversed()),
    /**
     * EES 欠产优先 降序
     */
    ESTIMATE_EXCEED_SHORT(ProductionSecondSortOptionsEnum.ESTIMATE_EXCEED_SHORT, Comparator.comparing(MonthPlanManufacturingRequirementVo::getIsDebitPlan, Comparator.nullsLast(Comparator.naturalOrder())).reversed()),
    /**
     * NPQ 可排产量 降序
     */
    NEED_PRODUCTION_QTY(ProductionSecondSortOptionsEnum.NEED_PRODUCTION_QTY, Comparator.comparing(MonthPlanManufacturingRequirementVo::getProductionQty, Comparator.nullsLast(Comparator.naturalOrder())).reversed());

    private ProductionSecondSortOptionsEnum option;

    private Comparator comparator;

    /**
     * 构造函数
     *
     * @param option     配置项编码
     * @param comparator 取值器
     */
    PlanProductionSortEnum(ProductionSecondSortOptionsEnum option, Comparator comparator) {
        this.option = option;
        this.comparator = comparator;
    }

    /**
     * 根据配置项编码，获取对应的库存对冲项枚举对象实例
     *
     * @param option 配置项编码
     * @return
     */
    public static PlanProductionSortEnum getInstance(ProductionSecondSortOptionsEnum option) {
        if (null == option) {
            return null;
        }
        for (PlanProductionSortEnum sortEnum : values()) {
            if (sortEnum.getOption() == option) {
                return sortEnum;
            }
        }
        return null;
    }

    public ProductionSecondSortOptionsEnum getOption() {
        return option;
    }

    public Comparator getComparator() {
        return comparator;
    }
}
