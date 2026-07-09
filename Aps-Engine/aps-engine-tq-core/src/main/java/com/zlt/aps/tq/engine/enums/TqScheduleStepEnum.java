package com.zlt.aps.tq.engine.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 胎圈排程步骤枚举。
 *
 * <p>定义胎圈排程6个阶段的步骤编码和描述，
 * 用于Context中的currentStep标识、日志追踪和异常定位。</p>
 *
 * <pre>
 * S1:   前置校验与数据加载 → 校验施工信息、加载参数/库存/机台/定点/口型板/损耗率/月度剩余/胎胚关联/停产计划
 * S2:   需求计算与机台分配 → 供应时长计算、计划量计算(含定额约束)、收尾判断(胎胚关联)
 * S3:   班次排产分配       → 3步排产策略(当前班→切换机台→延至下班)、定额控制
 * S3.5: 剩余产能分配       → 机台剩余产能按优先级回填备库胎圈及其他规格；第6班塞入所有剩余量
 * S4:   成型/胎圈停产协调  → 成型停产策略(1天/≥2天)、胎圈停产补量逻辑
 * S5:   班次均衡调整       → 按定额控制均衡、按日均衡
 * S6:   结果校验与持久化   → 外协分离、历史合并、数据落库、日志记录
 * </pre>
 *
 * @author APS
 */
@Getter
@AllArgsConstructor
public enum TqScheduleStepEnum {

    S1_PRE_VALIDATION("S1", "前置校验与数据加载"),
    S2_DEMAND_CALC("S2", "需求计算与机台分配"),
    S3_MACHINE_ASSIGN("S3", "班次排产分配"),
    S3_5_RESIDUAL_CAPACITY("S3.5", "剩余产能分配"),
    S4_STOP_COORDINATION("S4", "成型/胎圈停产协调"),
    S5_BALANCE("S5", "班次均衡调整"),
    S6_RESULT_PERSIST("S6", "结果校验与持久化");

    /** 步骤编码 */
    private final String code;

    /** 步骤描述 */
    private final String description;

    /**
     * 根据编码获取枚举
     *
     * @param code 步骤编码
     * @return 排程步骤枚举，未找到返回null
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
