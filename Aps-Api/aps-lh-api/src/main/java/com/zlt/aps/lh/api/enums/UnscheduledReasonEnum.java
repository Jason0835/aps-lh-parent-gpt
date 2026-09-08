/**
 * Copyright (c) 2008, 智立通（厦门）科技有限公司 All rights reserved。
 */
package com.zlt.aps.lh.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * 硫化未排原因枚举。
 *
 * <p>优先级只用于同一原始需求存在多个仍有效阻断时选择主原因，数值越大越优先；
 * 不参与排产准入、机台选择、排序、排量或资源扣账。</p>
 *
 * @author zlt
 */
@Getter
@AllArgsConstructor
public enum UnscheduledReasonEnum {

    NO_AVAILABLE_MOULD("01", "S4.4/S4.5", "无可用模具", 160, false),
    MOULD_CHANGE_EXCEEDED("02", "S4.4/S4.5", "换模换活字块次数限制", 178, false),
    NO_AVAILABLE_MACHINE("03", "S4.5", "无可用机台", 130, false),
    CAPACITY_INSUFFICIENT("04", "S4.4/S4.5", "无有效产能", 155, false),
    EMBRYO_STOCK_INSUFFICIENT("05", "S4.3/S4.4/S4.5", "胎胚库存不足", 150, false),
    CONSTRUCTION_NOT_READY("06", "S4.3", "施工信息未就绪", 125, false),
    NO_ORIGINAL_TARGET("NO_ORIGINAL_TARGET", "S4.3", "无排产目标量", 100, true),
    NO_DAILY_PLAN_IN_WINDOW("NO_DAILY_PLAN_IN_WINDOW", "S4.3", "排产窗口内无日计划", 110, true),
    NO_DAILY_PLAN_IN_FULL_RANGE("NO_DAILY_PLAN_IN_FULL_RANGE", "S4.3", "排产窗口及提前范围内无日计划", 115, true),
    ZERO_SURPLUS_AND_EMBRYO("ZERO_SURPLUS_AND_EMBRYO", "S4.3", "余量和胎胚库存均为0", 120, true),
    SHARED_EMBRYO_ALLOCATION_BLOCKED("SHARED_EMBRYO_ALLOCATION_BLOCKED", "S4.3", "共用胎胚零余量被分配规则阻断", 150, true),
    SKU_DECREMENT_HIT("SKU_DECREMENT_HIT", "S4.3", "命中SKU减量清单", 200, true),
    TRIAL_MASS_TRIAL_BLOCKED("TRIAL_MASS_TRIAL_BLOCKED", "S4.3/S4.5", "试制量试准入拦截", 190, true),
    HISTORY_SHORTAGE_SKIPPED("HISTORY_SHORTAGE_SKIPPED", "S4.4/S4.5", "仅历史欠产不排产", 175, false),
    CONTINUATION_LOCKED_REMAINING("CONTINUATION_LOCKED_REMAINING", "S4.4", "同物料多状态续作跨窗口延续", 155, false),
    SINGLE_CONTROL_CONTINUATION_BLOCKED("SINGLE_CONTROL_CONTINUATION_BLOCKED", "S4.4", "单控整机续作条件不满足", 175, false),
    TARGET_CLEARED_BY_RULE("TARGET_CLEARED_BY_RULE", "S4.3/S4.4", "排产目标被规则清零", 170, true),
    SMALL_ENDING_SURPLUS("SMALL_ENDING_SURPLUS", "S4.4/S4.5", "收尾小余量不排产", 180, true),
    MOULD_QUANTITY_INSUFFICIENT("MOULD_QUANTITY_INSUFFICIENT", "S4.4/S4.5", "模具数量不足", 165, false),
    MOULD_LEDGER_UNAVAILABLE("MOULD_LEDGER_UNAVAILABLE", "S4.4/S4.5", "模具台账不可用", 170, false),
    NO_HARD_MATCH_MACHINE("NO_HARD_MATCH_MACHINE", "S4.5", "无硬匹配机台", 130, false),
    NO_REMAINING_MATCHED_MACHINE("NO_REMAINING_MATCHED_MACHINE", "S4.5", "无可匹配剩余机台", 135, false),
    STRUCTURE_MACHINE_LIMIT("STRUCTURE_MACHINE_LIMIT", "S4.5", "结构机台数限制", 175, false),
    SHIFT_TOTAL_QTY_LIMIT("SHIFT_TOTAL_QTY_LIMIT", "S4.5", "班次总量限制", 172, false),
    CHANGEOVER_OUT_OF_BUSINESS_DAY("CHANGEOVER_OUT_OF_BUSINESS_DAY", "S4.4/S4.5", "切换超出业务日", 140, false),
    CHANGEOVER_OUT_OF_WINDOW("CHANGEOVER_OUT_OF_WINDOW", "S4.4/S4.5", "切换超出排产窗口", 145, false),
    EMBRYO_AVAILABLE_OUT_OF_WINDOW("EMBRYO_AVAILABLE_OUT_OF_WINDOW", "S4.4/S4.5", "胎胚可供时间超出排产窗口", 185, false),
    BASE_DATA_MISSING("BASE_DATA_MISSING", "S4.3/S4.5", "基础数据缺失", 125, false),
    OPEN_PRODUCTION_CONTROL_SHORTAGE("OPEN_PRODUCTION_CONTROL_SHORTAGE", "S4.3", "开产管控产生未排缺口", 170, false),
    PRECISION_FORCE_DOWN("PRECISION_FORCE_DOWN", "S4.4", "精度计划强制下机", 205, false),
    PRECISION_PRE_INSERT_ROLLBACK("PRECISION_PRE_INSERT_ROLLBACK", "S4.6", "精度插排撤销", 220, false),
    VIRTUAL_MACHINE_CAPACITY_INSUFFICIENT("VIRTUAL_MACHINE_CAPACITY_INSUFFICIENT", "S4.5.3", "虚拟机台容量不足", 210, false),
    SUBSTITUTION_REMAINING("SUBSTITUTION_REMAINING", "S4.5.1", "置换后仍有待排量", 195, false),
    PARTIAL_UNSCHEDULED("PARTIAL_UNSCHEDULED", "S4.5", "部分排产后仍有剩余", 150, false),
    WINDOW_FINAL_UNSCHEDULED("WINDOW_FINAL_UNSCHEDULED", "S4.5", "排产窗口结束仍未完成", 100, false),
    OTHER("OTHER", "UNKNOWN", "其他未排原因", 1, false);

    /** 原因编码 */
    private final String code;

    /** 原因阶段。 */
    private final String stage;
    /** 可读摘要。 */
    private final String summary;
    /** 主原因优先级。 */
    private final int priority;
    /** 是否允许保留数量为0的不排说明。 */
    private final boolean retainZeroQty;

    /**
     * 兼容既有调用方获取原因描述。
     *
     * @return 可读摘要
     */
    public String getDescription() {
        return summary;
    }

    /**
     * 根据编码获取枚举
     *
     * @param code 原因编码
     * @return 未排产原因枚举，未找到返回null
     */
    public static UnscheduledReasonEnum getByCode(String code) {
        for (UnscheduledReasonEnum reason : values()) {
            if (StringUtils.equals(reason.getCode(), code)) {
                return reason;
            }
        }
        return null;
    }

    /**
     * 按编码获取原因定义。
     *
     * @param code 原因编码
     * @return 原因定义，未匹配时返回OTHER
     */
    public static UnscheduledReasonEnum fromCode(String code) {
        UnscheduledReasonEnum reason = getByCode(code);
        return Objects.isNull(reason) ? OTHER : reason;
    }
}
