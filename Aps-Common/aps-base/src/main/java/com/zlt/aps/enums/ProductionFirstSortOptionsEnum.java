package com.zlt.aps.enums;

import org.apache.commons.lang3.StringUtils;

/**
 * 排产第一顺序项枚举定义类
 *
 * @author ZLT
 * @date 20250217
 */
public enum ProductionFirstSortOptionsEnum {
    /**
     * 有交期订单优先 升序
     */
    DELIVERY_DATE("DD", "ui.data.column.productionSort.deliveryDate", "ASC", "有交期优先"),
    /**
     * ER 急单优先 降序
     */
    EMERGENCY("ER", "ui.data.column.productionSort.emergency", "ASC", "急单优先"),
    /**
     * EES 欠产优先
     */
    ESTIMATE_EXCEED_SHORT("EES", "ui.data.column.productionSort.estimateExceedShort", "ASC", "欠产优先"),
    /**
     * IMC 重要客户优先 降序
     */
    IMPORTANT_CUSTOM("IMC", "ui.data.column.productionSort.importantCustom", "ASC", "重要客户优先"),
    /**
     * EP 必保计划优先 降序
     */
    ENSURE_PLAN("EP", "ui.data.column.productionSort.ensurePlan", "ASC", "必保计划优先"),
    /**
     * other 其它 降序
     */
    OTHER_PLAN("other", "ui.data.column.productionSort.otherPlan", "ASC", "其它计划");
    /**
     * 配置项
     */
    private String optionCode;
    /**
     * 国际化Key
     */
    private String optionDescI18nKey;
    /**
     * 排序方式
     */
    private String sortType;
    /**
     * 备注说明
     */
    private String remark;

    /**
     * 构造函数
     *
     * @param optionCode        配置项编码
     * @param optionDescI18nKey 对应的国际化label-key
     * @param sortType          排序类型 ASC 或是DESC
     * @param remark            备注说明
     */
    ProductionFirstSortOptionsEnum(String optionCode, String optionDescI18nKey, String sortType, String remark) {
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
    public static ProductionFirstSortOptionsEnum getInstance(String optionCode) {
        if (StringUtils.isBlank(optionCode)) {
            return null;
        }
        for (ProductionFirstSortOptionsEnum hedgingOption : values()) {
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
