package com.zlt.aps.enums;

import org.apache.commons.lang3.StringUtils;

/**
 * 库存对冲项枚举定义类
 *
 * @author ZLT
 * @date 20250217
 */
public enum StockHedgingOptionsEnum {
    /**
     * LCB 库存类别 升序
     */
    LOCATION_INFO("LCB", "ui.data.column.stockHedging.locationType", "ASC", "库位类别"),
    /**
     * IMC 重要客户优先 降序
     */
    IMPORTANT_CUSTOM("IMC", "ui.data.column.stockHedging.importantCustom", "DESC", "重要客户优先"),
    /**
     * ER 急单优先 降序
     */
    EMERGENCY("ER", "ui.data.column.stockHedging.emergency", "DESC", "急单优先"),
    /**
     * EP 必保计划优先 降序
     */
    ENSURE_PLAN("EP", "ui.data.column.stockHedging.ensurePlan", "DESC", "必保计划优先"),
    /**
     * DDD 交付日期早优先 升序
     */
    DELIVERY_DATE_DUE("DDD", "ui.data.column.stockHedging.deliveryDateDue", "ASC", "交付日期早优先"),
    /**
     * SMD 提报日期早优先 升序
     */
    SUBMISSION_DATE("SMD", "ui.data.column.stockHedging.submissionDate", "ASC", "提报日期早优先");

    private String optionCode;

    private String optionDescI18nKey;

    private String sortType;

    private String remark;

    /**
     * 构造函数
     *
     *
     * @param optionCode 配置项编码
     * @param optionDescI18nKey 对应的国际化label-key
     * @param sortType 排序类型 ASC 或是DESC
     * @param remark 备注说明
     */
    StockHedgingOptionsEnum(String optionCode, String optionDescI18nKey, String sortType, String remark) {
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
    public static StockHedgingOptionsEnum getInstance(String optionCode) {
        if (StringUtils.isBlank(optionCode)) {
            return null;
        }
        for (StockHedgingOptionsEnum hedgingOption : values()) {
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
