package com.zlt.aps.monthplan.api.domain.capacity;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * 日产能限制
 */
@Data
public class MpDailyCapacityLimitVo {

    /**
     * 每日
     */
    private Integer dailyDate;

    /**
     * 最大硫化机台数
     */
    private Integer maxLhMachines;

    /**
     * 最大胎胚种类数
     */
    private Integer maxEmbryoTypes;

    /**
     * 主花纹向下的  已用硫化机台数
     */
    private Integer patternUsedLhMachines;

    /**
     * 已用硫化机台数
     */
    private Integer usedLhMachines;

    /**
     * 已用胎胚种类数
     */
    private Integer usedEmbryoTypes;

    /**
     *  当日使用的胎胚编码
     */
    private Set<String> embryoCodes = new HashSet<>();

}
