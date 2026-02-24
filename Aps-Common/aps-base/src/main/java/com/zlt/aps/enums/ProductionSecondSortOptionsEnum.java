package com.zlt.aps.enums;

import org.apache.commons.lang3.StringUtils;

/**
 * 排产第二顺序项枚举定义类
 *
 * @author ZLT
 * @date 20250217
 */
public enum ProductionSecondSortOptionsEnum {
    /**
     * LCB 库存类别 升序
     */
    LOCATION_INFO("LCB", "ui.data.column.productionSort.locationType", "ASC", "库位类别"),
    /**
     * ER 急单优先 降序
     */
    EMERGENCY("ER", "ui.data.column.productionSort.emergency", "DESC", "急单优先"),
    /**
     * CNU 续作优先 降序
     */
    CONTINUE("CNU", "ui.data.column.productionSort.continue", "DESC", "续作优先"),
    /**
     * PFV 利润值高优先 升序
     */
    PROFIT_VALUE("PFV", "ui.data.column.productionSort.profitValue", "ASC", "利润值高优先"),
    /**
     * EES 欠产优先
     */
    ESTIMATE_EXCEED_SHORT("EES", "ui.data.column.productionSort.estimateExceedShort", "ASC", "欠产优先"),
    /**
     * FXP 定点优先
     */
//    FIXED_POINT("FXP", "ui.data.column.productionSort.fixedPoint", "ASC", "定点优先"),
    /**
     * IMC 重要客户优先 降序
     */
    IMPORTANT_CUSTOM("IMC", "ui.data.column.productionSort.importantCustom", "DESC", "重要客户优先"),

    /**
     * EP 必保计划优先 降序
     */
    ENSURE_PLAN("EP", "ui.data.column.productionSort.ensurePlan", "DESC", "必保计划优先"),
    /**
     * DDD 交付日期早优先 升序
     */
    DELIVERY_DATE_DUE("DDD", "ui.data.column.productionSort.deliveryDateDue", "ASC", "交付日期早优先"),
    /**
     * SMD 提报日期早优先 升序
     */
//    SUBMISSION_DATE("SMD", "ui.data.column.productionSort.submissionDate", "ASC", "提报日期早优先"),
    /**
     * NPQ 可排产量降序
     */
    NEED_PRODUCTION_QTY("NPQ", "ui.data.column.productionSort.needProductionQty", "DESC", "可排产量大优先");

    private String optionCode;

    private String optionDescI18nKey;

    private String sortType;

    private String remark;

    /**
     * 构造函数
     *
     * @param optionCode        配置项编码
     * @param optionDescI18nKey 对应的国际化label-key
     * @param sortType          排序类型 ASC 或是DESC
     * @param remark            备注说明
     */
    ProductionSecondSortOptionsEnum(String optionCode, String optionDescI18nKey, String sortType, String remark) {
        this.optionCode = optionCode;
        this.optionDescI18nKey = optionDescI18nKey;
        this.sortType = sortType;
        this.remark = remark;
    }

    /**
     * 根据配置项编码，获取对应的库存对冲项枚举对象实例
     *
     * @param optionCode 配置项编码
     * @return
     */
    public static ProductionSecondSortOptionsEnum getInstance(String optionCode) {
        if (StringUtils.isBlank(optionCode)) {
            return null;
        }
        for (ProductionSecondSortOptionsEnum hedgingOption : values()) {
            if (hedgingOption.getOptionCode().equalsIgnoreCase(optionCode)) {
                return hedgingOption;
            }
        }
        return null;
    }

    public String getOptionCode() {
        return optionCode;
    }

    public String getOptionDescI18nKey() {
        return optionDescI18nKey;
    }

    public String getSortType() {
        return sortType;
    }

    public String getRemark() {
        return remark;
    }
}
