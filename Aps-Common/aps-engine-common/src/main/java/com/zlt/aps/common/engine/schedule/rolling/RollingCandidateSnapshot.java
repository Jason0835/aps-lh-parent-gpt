package com.zlt.aps.common.engine.schedule.rolling;

import lombok.Data;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 定时自动滚动专用轻量快照，不映射数据库实体。
 */
@Data
public class RollingCandidateSnapshot {
    /**
     * 候选机台。
     */
    private String machineCode = null;
    /**
     * 现有策略评分。
     */
    private BigDecimal score = BigDecimal.ZERO;
    /**
     * 标定生产速度米每小时。
     */
    private BigDecimal speed = null;
    /**
     * 速度来源及推导证据。
     */
    private String speedSource = null;
    /**
     * 适用损耗百分比。
     */
    private BigDecimal lossRate = BigDecimal.ZERO;
    /**
     * 拒绝原因，为空表示合格。
     */
    private String reason = null;
    /**
     * 候选机台静态过滤及速度推导的具体检查依据。
     */
    private Map<String, Object> evidence = new LinkedHashMap<>();
}
