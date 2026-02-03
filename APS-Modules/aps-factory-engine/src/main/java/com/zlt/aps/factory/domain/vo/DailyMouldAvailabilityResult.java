package com.zlt.aps.factory.domain.vo;

import lombok.Data;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author nick
 */
@Data
public class DailyMouldAvailabilityResult {
    /**
     * 天数
     */
    private Integer dayOfCycle;

    /**
     * 型腔 KEY --> 型腔数量
     */
    private Map<String, Integer> cavityResults;

    /**
     * 插入结果 KEY --> 活块数量
     */
    private Map<String, Integer> insertResults;


    public DailyMouldAvailabilityResult() {
        this.cavityResults = new HashMap<>();
        this.insertResults = new HashMap<>();
    }

    /**
     * 创建空结果
     */
    public static DailyMouldAvailabilityResult emptyResult() {
        return new DailyMouldAvailabilityResult();
    }

    /**
     * 创建周期外结果
     */
    public static DailyMouldAvailabilityResult outOfCycleResult() {
        return new DailyMouldAvailabilityResult();
    }

}