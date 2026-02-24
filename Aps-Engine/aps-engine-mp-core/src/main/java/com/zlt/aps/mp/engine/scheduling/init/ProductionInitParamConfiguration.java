package com.zlt.aps.mp.engine.scheduling.init;

import com.zlt.aps.mp.engine.enums.DayVulcanizationModeEnum;
import lombok.Data;

/**
 * 初始化排产参数配置对象
 *
 * @author ZLT
 * @date 20251212
 */
@Data
public class ProductionInitParamConfiguration {
    /**
     * SYS0202001 是否开启模具预占计算
     *
     */
    private String openPreemptionMouldCapacity;

    /**
     * SYS0202003 是否采用损耗率计算损耗
     */
    private String openLevelRatio;

    /**
     * SYS0202002 日硫化量使用的模式值
     */
    private DayVulcanizationModeEnum dayVulcanizationQtyConfiguration;
}
