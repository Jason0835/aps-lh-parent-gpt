package com.zlt.aps.tq.engine.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 胎圈排程步骤枚举。
 *
 * <p>定义胎圈排程各阶段的步骤编码和描述，
 * 用于 Context 中的 currentStep 标识、日志追踪和异常定位。</p>
 *
 * <pre>
 * S1:    前置校验与数据加载  → 校验施工信息、加载参数/库存/机台/定点/口型板/损耗率/月度剩余/胎胚关联/停产计划
 * S2.1:  库存预测           → 供应时长计算（策略可插拔，算法1/算法2）
 * S2.2:  需求量计算         → 备库班数配置匹配、备库触发判断、收尾判断、备库总量计算
 * S2.3:  计划量计算         → 6 班滚动计划量、备库总量分摊、计划量取整与工装限制、算法1模式筛选
 * S3:    班次排产分配       → 3 步排产策略（当前班→切换机台→延至下班）、定额控制
 * S4:    成型/胎圈停产协调  → 成型停产策略（1 天/≥2 天）、胎圈停产补量逻辑
 * S5:    班次均衡调整       → 按定额控制均衡、按日均衡
 * S5.5:  定额校验与顺序重置 → S4/S5 修改计划量后校验机台定额超量、延后超出部分、重置生产顺序
 * S5.6:  最终剩余产能回填   → S5.5 定额校验后回收机台剩余产能，按优先级回填备库胎圈及其他规格，避免被 S4/S5/S5.5 覆盖
 * S6:    结果校验与持久化   → 外协分离、历史合并、数据落库、日志记录、解释 JSON 写入
 * </pre>
 *
 * @author APS
 */
@Getter
@AllArgsConstructor
public enum TqScheduleStepEnum {

    S1_PRE_VALIDATION("S1", "前置校验与数据加载"),
    /** S2.1 库存预测：供应时长计算（策略可插拔） */
    S2_1_STOCK_PREDICT("S2.1", "库存预测"),
    /** S2.2 需求量计算：备库触发、收尾判断 */
    S2_2_DEMAND_QTY_CALC("S2.2", "需求量计算"),
    /** S2.3 计划量计算：6 班滚动、备库分摊、取整工装限制 */
    S2_3_PLAN_QTY_CALC("S2.3", "计划量计算"),
    S3_MACHINE_ASSIGN("S3", "班次排产分配"),
    S4_STOP_COORDINATION("S4", "成型/胎圈停产协调"),
    S5_BALANCE("S5", "班次均衡调整"),
    S5_5_QUOTA_VALIDATE("S5.5", "定额校验与顺序重置"),
    S5_6_RESIDUAL_CAPACITY("S5.6", "最终剩余产能回填"),
    S6_RESULT_PERSIST("S6", "结果校验与持久化");

    /** 步骤编码 */
    private final String code;

    /** 步骤描述 */
    private final String description;

    /**
     * 根据编码获取枚举
     *
     * @param code 步骤编码
     * @return 排程步骤枚举，未找到返回 null
     */
    public static TqScheduleStepEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (TqScheduleStepEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
