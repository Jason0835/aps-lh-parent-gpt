package com.zlt.aps.gsq.engine.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 钢丝圈排程规则编码枚举。
 *
 * <p>定义钢丝圈排程各阶段命中的规则编码，用于结构化证据 {@link com.zlt.aps.gsq.engine.domain.GsqRuleTrace} 落库。</p>
 *
 * <p>对齐胎圈 {@code TqScheduleRuleCodeEnum}，同时包含钢丝圈独有规则：</p>
 * <ul>
 *   <li>S2.1 库存预测：供应时长算法选择与计算结果</li>
 *   <li>S2.2 需求量计算：BOM 分解、备库触发、收尾判断</li>
 *   <li>S2.3 计划量计算：6 班滚动分摊、工装车限制</li>
 *   <li>S3 机台分配：过滤策略命中（含钢丝直径、产线、换盘等钢丝圈独有策略）</li>
 *   <li>S5.6 剩余产能回填</li>
 *   <li>S4 停产协调：胎圈/钢丝圈停产场景</li>
 *   <li>S5 班次均衡</li>
 *   <li>S5.5 定额校验</li>
 * </ul>
 *
 * @author APS
 */
@Getter
@AllArgsConstructor
public enum GsqScheduleRuleCodeEnum {

    // ========== S2.1 库存预测阶段 ==========
    /** 供应时长算法选择 */
    SUPPLY_TIME_ALGORITHM("S2_1_SUPPLY_TIME_ALGORITHM", "供应时长算法选择"),
    /** 库存供应时长计算结果 */
    SUPPLY_TIME_RESULT("S2_1_SUPPLY_TIME_RESULT", "库存供应时长计算结果"),
    /** 库存预测（供应时长低于阈值触发） */
    STOCK_PREDICT("S2_1_STOCK_PREDICT", "库存预测"),
    /** 末班估值 */
    LAST_SHIFT_ESTIMATE("S2_1_LAST_SHIFT_ESTIMATE", "末班估值"),

    // ========== S2.2 需求量计算阶段 ==========
    /** BOM 分解（钢丝圈→胎圈→胎胚） */
    BOM_DECOMPOSE("S2_2_BOM_DECOMPOSE", "BOM分解"),
    /** 备库班数配置匹配 */
    BACKUP_SHIFT_CONFIG_MATCH("S2_2_BACKUP_SHIFT_CONFIG_MATCH", "备库班数配置匹配"),
    /** 备库触发判断 */
    BACKUP_TRIGGER("S2_2_BACKUP_TRIGGER", "备库触发判断"),
    /** 收尾判断 */
    CLOSE_OUT_JUDGE("S2_2_CLOSE_OUT_JUDGE", "收尾判断"),

    // ========== S2.3 计划量计算阶段 ==========
    /** 6 班滚动计划量计算 */
    PLAN_QTY_CALC("S2_3_PLAN_QTY_CALC", "6班滚动计划量计算"),
    /** 备库总量分摊 */
    BACKUP_ALLOCATE("S2_3_BACKUP_ALLOCATE", "备库总量分摊"),
    /** 计划量取整与工装车限制 */
    PLAN_QTY_ROUNDING("S2_3_PLAN_QTY_ROUNDING", "计划量取整与工装车限制"),

    // ========== S3 机台分配阶段 ==========
    /** 机台过滤策略命中（含钢丝直径、产线、寸口、检修等） */
    MACHINE_FILTER("S3_MACHINE_FILTER", "机台过滤策略命中"),
    /** 机台评分结果 */
    MACHINE_SCORE("S3_MACHINE_SCORE", "机台评分结果"),
    /** 机台定额约束 */
    MACHINE_QUOTA_LIMIT("S3_MACHINE_QUOTA_LIMIT", "机台定额约束"),
    /** 换盘判断（钢丝圈独有） */
    WIRE_COIL_SWITCH("S3_WIRE_COIL_SWITCH", "换盘判断"),

    // ========== S5.6 最终剩余产能回填阶段 ==========
    /** 剩余产能回填 */
    RESIDUAL_CAPACITY_FILL("S5_6_RESIDUAL_CAPACITY_FILL", "剩余产能回填"),

    // ========== S4 停产协调阶段 ==========
    /** 胎圈停产场景（影响钢丝圈消耗） */
    TQ_STOP_COORDINATION("S4_TQ_STOP_COORDINATION", "胎圈停产场景"),
    /** 钢丝圈停产场景 */
    GSQ_STOP_COORDINATION("S4_GSQ_STOP_COORDINATION", "钢丝圈停产场景"),
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
    PRODUCE_ORDER_RESET("S5_5_PRODUCE_ORDER_RESET", "生产顺序重置"),

    // ========== S6 保鲜期判断 ==========
    /** 保鲜期超期判断（钢丝圈独有） */
    FRESH_EXPIRED_CHECK("S6_FRESH_EXPIRED_CHECK", "保鲜期超期判断");

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
    public static GsqScheduleRuleCodeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (GsqScheduleRuleCodeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
