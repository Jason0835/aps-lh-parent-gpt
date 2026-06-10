package com.zlt.aps.tq.engine.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 胎圈排程步骤枚举。
 *
 * <p>定义胎圈排程4个阶段的步骤编码和描述，
 * 用于Context中的currentStep标识、日志追踪和异常定位。</p>
 *
 * <pre>
 * S1: 前置校验与数据加载 → 校验施工信息、加载参数/库存/机台/定点/口型板/损耗率/月度剩余
 * S2: 需求计算与均衡     → 供应时长计算、计划量计算、收尾判断、两天均衡
 * S3: 机台分配与排序     → 定点/口型板/寸口/维修多维度机台过滤、生产顺序设置
 * S4: 结果校验与持久化   → 外协分离、历史合并、数据落库、日志记录
 * </pre>
 *
 * @author APS
 */
@Getter
@AllArgsConstructor
public enum TqScheduleStepEnum {

    S1_PRE_VALIDATION("S1", "前置校验与数据加载"),
    S2_DEMAND_CALC("S2", "需求计算与均衡"),
    S3_MACHINE_ASSIGN("S3", "机台分配与排序"),
    S4_RESULT_PERSIST("S4", "结果校验与持久化");

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
