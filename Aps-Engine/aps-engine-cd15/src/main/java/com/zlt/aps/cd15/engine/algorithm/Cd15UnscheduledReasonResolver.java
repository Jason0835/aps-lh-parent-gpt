package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15UnscheduledReason;
import org.springframework.stereotype.Component;

/** 将执行器内部失败原因转换为稳定未排原因。 */
@Component
public class Cd15UnscheduledReasonResolver {

    /**
     * 解析内部失败原因。
     *
     * @param failureReason 内部原因
     * @return 标准原因
     */
    public Cd15UnscheduledReason resolve(String failureReason) {
        if ("CONSTRUCTION_MISSING".equals(failureReason)) {
            return reason("DATA_MISSING", "DATA_PREPARATION", "施工信息或必要基础数据缺失");
        }
        if ("MACHINE_PROHIBITED".equals(failureReason)) {
            return reason("MACHINE_PROHIBITED", "MACHINE_FILTER", "大卷绑定机台均不可作业");
        }
        if ("WIDTH_MISMATCH".equals(failureReason)) {
            return reason("WIDTH_MISMATCH", "MACHINE_FILTER", "施工斜裁宽度超出机台裁断宽度范围");
        }
        if ("ROLL_TOOL_LIMIT".equals(failureReason) || "TOOLING_LIMIT".equals(failureReason)) {
            return reason("ROLL_TOOL_LIMIT", "ROLL_TOOL", "工装不足");
        }
        if ("CAPACITY_LIMIT".equals(failureReason)) {
            return reason("NO_AVAILABLE_MACHINE", "MACHINE_FILTER", "机台产能不足");
        }
        if ("STORAGE_LANE_LIMIT".equals(failureReason)) {
            return reason("STORAGE_LANE_LIMIT", "STORAGE_LANE", "库排容量不足");
        }
        if ("AGING_PERIOD_LIMIT".equals(failureReason)) {
            return reason("AGING_PERIOD_LIMIT", "BIG_ROLL_AGING", "大卷静置期未满");
        }
        if ("SPEC_START_COUNT_LIMIT".equals(failureReason)) {
            return reason("SPEC_START_COUNT_LIMIT", "SPEC_START_COUNT", "连续四班上机次数达到上限");
        }
        if ("SCHEDULE_WINDOW_LIMIT".equals(failureReason)) {
            return reason("SCHEDULE_WINDOW_LIMIT", "SCHEDULE_WINDOW", "排程窗口结束仍有未安排数量");
        }
        return reason("NO_AVAILABLE_MACHINE", "MACHINE_FILTER", "动态状态或产能约束导致无可选候选机台");
    }

    private Cd15UnscheduledReason reason(String code, String stage, String description) {
        return Cd15UnscheduledReason.builder().reasonCode(code)
                .failStage(stage).reasonDescription(description).build();
    }
}
