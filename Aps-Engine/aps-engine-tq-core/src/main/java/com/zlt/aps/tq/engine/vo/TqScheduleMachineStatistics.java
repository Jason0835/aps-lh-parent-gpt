package com.zlt.aps.tq.engine.vo;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * 胎圈排产机台统计信息
 *
 */
@Data
public class TqScheduleMachineStatistics {
    /**
     * 胶料分配机台
     */
    private Map<String, List<Long>> glueMap = new HashMap<>();
    /**
     * 口型版分配机台
     */
    private Map<String, List<Long>> mouthPlatMap = new HashMap<>();
    
    /**
     * 按胶料统计计划量
     */
    private Map<String, BigDecimal> gluePlanMap = new HashMap<>();
    
    /**
     * 按口型统计计划量
     */
    private Map<String, BigDecimal> mouthPlatPlanMap = new HashMap<>();
}
