package com.zlt.aps.mp.enums;

import com.zlt.aps.enums.StockHedgingOptionsEnum;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanSaleOrder;

import java.util.Comparator;

/**
 * 库存对冲项配置枚举定义类
 *
 * @author ZLT
 * @date 20250217
 */
public enum StockHedgingComparatorEnum {
    /**
     * LCB 库存类别 升序
     */
    LOCATION_INFO(StockHedgingOptionsEnum.LOCATION_INFO, Comparator.comparing(MonthPlanSaleOrder::getLocationSortValue, Comparator.nullsLast(Comparator.naturalOrder()))),
    /**
     * IMC 重要客户优先 降序
     */
    IMPORTANT_CUSTOM(StockHedgingOptionsEnum.IMPORTANT_CUSTOM, Comparator.comparing(MonthPlanSaleOrder::getIsImportantCustom, Comparator.nullsLast(Comparator.naturalOrder()))),
    /**
     * ER 急单优先 降序
     */
    EMERGENCY(StockHedgingOptionsEnum.EMERGENCY, Comparator.comparing(MonthPlanSaleOrder::getIsEmergency, Comparator.nullsLast(Comparator.naturalOrder()))),
    /**
     * EP 必保计划优先 降序
     */
    ENSURE_PLAN(StockHedgingOptionsEnum.ENSURE_PLAN, Comparator.comparing(MonthPlanSaleOrder::getIsEnsurePlan, Comparator.nullsLast(Comparator.naturalOrder()))),
    /**
     * DDD 交付日期早优先 升序
     */
    DELIVERY_DATE_DUE(StockHedgingOptionsEnum.DELIVERY_DATE_DUE, Comparator.comparing(MonthPlanSaleOrder::getDeliveryDateDue, Comparator.nullsLast(Comparator.naturalOrder()))),
    /**
     * SMD 提报日期早优先 升序
     */
    SUBMISSION_DATE(StockHedgingOptionsEnum.SUBMISSION_DATE, Comparator.comparing(MonthPlanSaleOrder::getSubmissionDate, Comparator.nullsLast(Comparator.naturalOrder())));

    private StockHedgingOptionsEnum option;

    private Comparator comparator;

    /**
     * 构造函数
     *
     * @param option     配置项编码
     * @param comparator 比较器
     */
    StockHedgingComparatorEnum(StockHedgingOptionsEnum option, Comparator comparator) {
        this.option = option;
        this.comparator = comparator;
    }

    /**
     * 根据配置项编码，获取对应的库存对冲项枚举对象实例
     *
     * @param option 配置项编码
     * @return
     */
    public static StockHedgingComparatorEnum getInstance(StockHedgingOptionsEnum option) {
        if (null == option) {
            return null;
        }
        for (StockHedgingComparatorEnum hedgingOption : values()) {
            if (hedgingOption.getOption() == option) {
                return hedgingOption;
            }
        }
        return null;
    }

    public StockHedgingOptionsEnum getOption() {
        return option;
    }

    public Comparator getComparator() {
        return comparator;
    }
}
