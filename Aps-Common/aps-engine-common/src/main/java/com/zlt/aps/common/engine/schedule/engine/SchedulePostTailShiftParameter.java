package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

/**
 * 收尾后起排班次参数快照。
 *
 * <p>该对象只保存一次自动排程运行中使用的参数解析结果，不读取数据库，
 * 也不在运行过程中重新解析配置。</p>
 */
@Data
public class SchedulePostTailShiftParameter {

    /**
     * 参数编码。
     */
    private String paramCode;

    /**
     * 约定的参数名称，用于检测编码冲突。
     */
    private String expectedParamName;

    /**
     * 参数表原始值；代码默认快照时为空。
     */
    private String rawValue;

    /**
     * 参数表默认值。
     */
    private String defaultValue;

    /**
     * 解析后的有效起排班次数。
     */
    private Integer effectiveShiftCount;

    /**
     * 参数来源。
     */
    private String source;

    /**
     * 回退原因；正常使用原始正整数时为空。
     */
    private String fallbackReason;
}
