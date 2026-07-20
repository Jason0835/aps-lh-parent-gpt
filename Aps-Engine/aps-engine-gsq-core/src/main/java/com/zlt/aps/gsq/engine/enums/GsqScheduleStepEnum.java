package com.zlt.aps.gsq.engine.enums;

/**
 * 钢丝圈排程步骤枚举。
 *
 * <p>对应模板方法的8个阶段：</p>
 * <ul>
 *   <li>S1: 前置校验与数据加载</li>
 *   <li>S2: 需求计算与机台分配</li>
 *   <li>S3: 班次排产分配</li>
 *   <li>S3.5: 剩余产能分配</li>
 *   <li>S4: 胎圈/钢丝圈停产协调</li>
 *   <li>S5: 班次均衡调整</li>
 *   <li>S5.5: 定额校验与顺序重置</li>
 *   <li>S6: 结果校验与持久化</li>
 * </ul>
 *
 * @author APS
 */
public enum GsqScheduleStepEnum {

    S1_PRE_VALIDATION("S1", "前置校验与数据加载"),
    S2_DEMAND_CALC("S2", "需求计算与机台分配"),
    S3_MACHINE_ASSIGN("S3", "班次排产分配"),
    S3_5_RESIDUAL_CAPACITY("S3.5", "剩余产能分配"),
    S4_STOP_COORDINATION("S4", "胎圈/钢丝圈停产协调"),
    S5_BALANCE("S5", "班次均衡调整"),
    S5_5_QUOTA_VALIDATE("S5.5", "定额校验与顺序重置"),
    S6_RESULT_PERSIST("S6", "结果校验与持久化");

    private final String code;
    private final String desc;

    GsqScheduleStepEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
