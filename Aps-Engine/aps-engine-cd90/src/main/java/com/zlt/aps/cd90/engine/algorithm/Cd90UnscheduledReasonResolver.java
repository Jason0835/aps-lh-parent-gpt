package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90UnscheduledReason;
import org.springframework.stereotype.Component;

/** 将执行器内部失败原因转换为稳定未排原因。 */
@Component
public class Cd90UnscheduledReasonResolver {

    /**
     * 解析内部失败原因。
     *
     * @param failureReason 内部原因
     * @return 标准原因
     */
    public Cd90UnscheduledReason resolve(String failureReason) {
        if ("CONSTRUCTION_MISSING".equals(failureReason)) {
            return reason("DATA_MISSING", "DATA_PREPARATION", "施工信息或必要基础数据缺失");
        }
        if ("NO_MACHINE_MAPPING".equals(failureReason)) {
            return reason("NO_MACHINE_MAPPING", "MACHINE_FILTER", "大卷未配置绑定机台");
        }
        if ("MACHINE_PROHIBITED".equals(failureReason)) {
            return reason("MACHINE_PROHIBITED", "MACHINE_FILTER", "大卷绑定机台均不可作业");
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
        if ("SPEC_START_COUNT_LIMIT".equals(failureReason)) {
            return reason("SPEC_START_COUNT_LIMIT", "SPEC_START_COUNT", "连续四班上机次数达到上限");
        }
        if ("SCHEDULE_WINDOW_LIMIT".equals(failureReason)) {
            return reason("SCHEDULE_WINDOW_LIMIT", "SCHEDULE_WINDOW", "排程窗口结束仍有未安排数量");
        }
        return reason("NO_AVAILABLE_MACHINE", "MACHINE_FILTER", "动态状态或产能约束导致无可选候选机台");
    }

    private Cd90UnscheduledReason reason(String code, String stage, String description) {
        return Cd90UnscheduledReason.builder().reasonCode(code)
                .failStage(stage).reasonDescription(description).build();
    }
}
