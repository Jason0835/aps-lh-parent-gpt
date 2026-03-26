package com.zlt.aps.mp.api.domain.capacity;

import lombok.Data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
    private Integer maxLhMachines = 0;

    /**
     * 最大胎胚种类数
     */
    private Integer maxEmbryoTypes = 0;

    /**
     * 主花纹向下的  已用硫化机台数
     */
    private Integer patternUsedLhMachines = 0;

    /**
     * 已用硫化机台数
     */
    private Integer usedLhMachines = 0;

    /**
     * 已用胎胚种类数
     */
    private Integer usedEmbryoTypes = 0;

    /**
     * 已用换模次数
     */
    private Integer usedChangeMould = 0;

    /**
     *  当日使用的胎胚编码
     */
    private Set<String> embryoCodes = new HashSet<>();

    /**
     *  最大日产量
     */
    private Integer maxDayProductionQty = 0;

    /**
     *  (结构+日)剩余最大日产量
     */
    private Integer remainMaxDayProductionQty = 0;

    /**
     *  (结构+日)剩余硫化机台数
     */
    private Integer remainLhMachines = 0;

    /**
     *  是否开产首日
     */
    private boolean openProductionFirstDay = false;

    /**
     * 日开停产标识
     */
    private String dayOpenCloseFlag;

    /**
     * 日产比例
     */
    private Integer dayProductionRate;

}
