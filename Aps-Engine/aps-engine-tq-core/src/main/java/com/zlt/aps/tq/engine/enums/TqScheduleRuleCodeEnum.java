package com.zlt.aps.tq.engine.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 胎圈排程规则编码枚举。
 *
 * <p>定义胎圈排程各阶段命中的规则编码，用于结构化证据 {@link com.zlt.aps.tq.engine.domain.TqRuleTrace} 落库。</p>
 *
 * <p>覆盖阶段：</p>
 * <ul>
 *   <li>S2.1 库存预测：供应时长算法选择与计算结果</li>
 *   <li>S2.2 需求量计算：备库触发、收尾判断</li>
 *   <li>S2.3 计划量计算：6 班滚动分摊、阈值限制、取整工装限制</li>
 *   <li>S3 机台分配：过滤策略命中、评分结果</li>
 *   <li>S3.5 剩余产能分配：三级优先级回填</li>
 *   <li>S4 停产协调：成型/胎圈停产场景</li>
 *   <li>S5 班次均衡：定额约束、按日均衡</li>
 *   <li>S5.5 定额校验：超出延后、顺序重置</li>
 * </ul>
 *
 * @author APS
 */
@Getter
@AllArgsConstructor
public enum TqScheduleRuleCodeEnum {

    // ========== S2.1 库存预测阶段 ==========
    /** 供应时长算法选择（算法1/算法2） */
    SUPPLY_TIME_ALGORITHM("S2_1_SUPPLY_TIME_ALGORITHM", "供应时长算法选择"),
    /** 库存供应时长计算结果 */
    SUPPLY_TIME_RESULT("S2_1_SUPPLY_TIME_RESULT", "库存供应时长计算结果"),

    // ========== S2.2 需求量计算阶段 ==========
    /** 备库班数配置匹配（按成型机台数） */
    BACKUP_SHIFT_CONFIG_MATCH("S2_2_BACKUP_SHIFT_CONFIG_MATCH", "备库班数配置匹配"),
    /** 备库触发判断（主动/被动） */
    BACKUP_TRIGGER("S2_2_BACKUP_TRIGGER", "备库触发判断"),
    /** 收尾判断（基于胎胚关联汇总） */
    CLOSE_OUT_JUDGE("S2_2_CLOSE_OUT_JUDGE", "收尾判断"),

    // ========== S2.3 计划量计算阶段 ==========
    /** 6 班滚动计划量计算 */
    PLAN_QTY_CALC("S2_3_PLAN_QTY_CALC", "6 班滚动计划量计算"),
    /** 备库总量分摊（按阈值） */
    BACKUP_ALLOCATE("S2_3_BACKUP_ALLOCATE", "备库总量分摊"),
    /** 计划量取整与工装限制 */
    PLAN_QTY_ROUNDING("S2_3_PLAN_QTY_ROUNDING", "计划量取整与工装限制"),
    /** 算法1模式库存保证班数筛选 */
    ALGORITHM_1_FILTER("S2_3_ALGORITHM_1_FILTER", "算法1模式库存保证班数筛选"),

    // ========== S3 机台分配阶段 ==========
    /** 机台过滤策略命中 */
    MACHINE_FILTER("S3_MACHINE_FILTER", "机台过滤策略命中"),
    /** 机台评分结果 */
    MACHINE_SCORE("S3_MACHINE_SCORE", "机台评分结果"),
    /** 机台定额约束 */
    MACHINE_QUOTA_LIMIT("S3_MACHINE_QUOTA_LIMIT", "机台定额约束"),

    // ========== S3.5 剩余产能分配阶段 ==========
    /** 剩余产能回填（按优先级） */
    RESIDUAL_CAPACITY_FILL("S3_5_RESIDUAL_CAPACITY_FILL", "剩余产能回填"),

    // ========== S4 停产协调阶段 ==========
    /** 成型停产场景 */
    CX_STOP_COORDINATION("S4_CX_STOP_COORDINATION", "成型停产场景"),
    /** 胎圈停产场景 */
    TQ_STOP_COORDINATION("S4_TQ_STOP_COORDINATION", "胎圈停产场景"),
    /** 停产交集日开产 */
    STOP_INTERSECTION_REOPEN("S4_STOP_INTERSECTION_REOPEN", "停产交集日开产"),

    // ========== S5 班次均衡阶段 ==========
    /** 定额约束调整 */
    QUOTA_CONSTRAINT_ADJUST("S5_QUOTA_CONSTRAINT_ADJUST", "定额约束调整"),
    /** 按日均衡调整 */
    DAILY_BALANCE_ADJUST("S5_DAILY_BALANCE_ADJUST", "按日均衡调整"),

    // ========== S5.5 定额校验阶段 ==========
    /** 定额超出延后 */
    QUOTA_EXCEED_DEFER("S5_5_QUOTA_EXCEED_DEFER", "定额超出延后"),
    /** 生产顺序重置 */
    PRODUCE_ORDER_RESET("S5_5_PRODUCE_ORDER_RESET", "生产顺序重置");

    /** 规则编码 */
    private final String code;

    /** 规则描述 */
    private final String description;

    /**
     * 根据编码获取枚举
     *
     * @param code 规则编码
     * @return 规则枚举，未找到返回 null
     */
    public static TqScheduleRuleCodeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (TqScheduleRuleCodeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
