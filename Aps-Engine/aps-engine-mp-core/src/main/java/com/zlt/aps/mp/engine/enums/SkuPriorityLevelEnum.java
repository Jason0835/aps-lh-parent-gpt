package com.zlt.aps.mp.engine.enums;

import lombok.Getter;

/**
 * Sku优先等级枚举类
 * <p>
 * 0 Sku优先-供应链优先
 * 1 模具受限优先
 * 2 库销比约束
 * 3 小于50条
 * 4 排产量
 *
 * @author ZLT
 * @date 20260403
 */
@Getter
public enum SkuPriorityLevelEnum {
    /**
     * 0 Sku优先
     */
    ASSIGNED_PRIORITY(0, "Sku优先-供应链优先"),
    /**
     * 1 模具受限优先
     */
    RESTRICTED_PRIORITY(1, "模具受限优先"),
    /**
     * 高优先级量优先
     */
    SALE_RATIO_PRIORITY(2, "库销比约束"),
    /**
     * 净需求量优先
     */
    LESS_MIN_PRIORITY(3, "小于50条"),
    /**
     * 排产量 量大优先
     */
    NET_REQUIREMENT_PRIORITY(4, "小于50条");
    /**
     * 等级值 越低优先级越高
     */
    private Integer levelValue;
    /**
     * 优先级说明
     */
    private String priorityDesc;

    SkuPriorityLevelEnum(Integer levelValue, String priorityDesc) {
        this.levelValue = levelValue;
        this.priorityDesc = priorityDesc;
    }
}