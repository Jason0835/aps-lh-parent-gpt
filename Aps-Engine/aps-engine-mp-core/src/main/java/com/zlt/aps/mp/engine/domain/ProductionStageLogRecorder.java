package com.zlt.aps.mp.engine.domain;

import com.zlt.aps.mp.engine.enums.LogRecorderStageEnum;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 排产阶段-日志记录器对象
 *
 * @author ZLT
 * @date 20260524
 */
@Slf4j
@Getter
public class ProductionStageLogRecorder {
    /**
     * 排产阶段
     */
    private LogRecorderStageEnum stage;
    /**
     * 日志记录器
     */
    private StringBuilder logBuilder;

    /**
     * 构建排产阶段-日志记录器
     *
     * @param stage
     */
    public ProductionStageLogRecorder(LogRecorderStageEnum stage) {
        this.stage = stage;
        this.logBuilder = new StringBuilder();
    }
}
