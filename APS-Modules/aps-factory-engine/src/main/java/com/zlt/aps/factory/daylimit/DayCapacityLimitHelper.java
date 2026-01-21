package com.zlt.aps.factory.daylimit;

import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.scheduling.cxcapacity.ProductionCapacityParamConfiguration;
import lombok.Getter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

/**
 * 日产能控制对象
 * 排产日、最大产能上限、最低产能，产能比例
 *
 * @author ZLT
 * @date 20250106
 */
@Getter
public class DayCapacityLimitHelper implements Serializable {

    /**
     * 排产日
     */
    private Integer productionDay;
    /**
     * 每日产能上限
     */
    private Integer maxCapacity;
    /**
     * 每日产能下限
     */
    private Integer minCapacity;
    /**
     * 每日切换结构次数上限
     */
    private Integer maxChangeCxMachineCount;
    /**
     * 每日换模硫化机台数上限
     */
    private Integer maxChangeLhMachineCount;
    /**
     * 产能比例值
     */
    private Integer capacityRatio;
    /**
     * 切换结构使用次数
     */
    private Integer usedChangeCxMachineCount;
    /**
     * 切换换模硫化机台使用次数
     */
    private Integer usedChangeLhMachineCount;
    /**
     * 总的排产量(包含换模等导致的损耗)
     */
    private Integer sumProductionCapacityQty;
    /**
     * 存储切换：机台-分组信息
     * 机台|*|分组
     */
    private Set<String> changeCxMachineInfo;
    /**
     * 根据产能比例，构建初始的日产能限制对象
     *
     * @param productionDay      排产日
     * @param paramConfiguration 参数
     * @param ratio              比例 1~100的值
     * @return
     */
    public static DayCapacityLimitHelper createInit(Integer productionDay, ProductionCapacityParamConfiguration paramConfiguration, Integer ratio) {
        DayCapacityLimitHelper initLimit = new DayCapacityLimitHelper(productionDay);
        if (null != ratio) {
            initLimit.capacityRatio = ratio;
        }
        Integer dayMaxCapacity = paramConfiguration.getDayMaxCapacity();
        if (null != dayMaxCapacity) {
            Integer realDayMaxCapacity = BigDecimal.valueOf(dayMaxCapacity).multiply(BigDecimal.valueOf(ProductionConstant.PERCENTAGE)).divide(BigDecimal.valueOf(ratio), 0, RoundingMode.UP).intValue();
            initLimit.maxCapacity = realDayMaxCapacity;
        }
        Integer dayMinCapacity = paramConfiguration.getDayMinCapacity();
        if (null != dayMinCapacity) {
            initLimit.minCapacity = dayMinCapacity;
        }
        Integer changeCxMachineCount = paramConfiguration.getDayChangeGroupCount();
        if (null != changeCxMachineCount) {
            initLimit.maxChangeCxMachineCount = changeCxMachineCount;
        }
        Integer changeLhMachineCount = paramConfiguration.getChangeMouldLhMachineNumber();
        if (null != changeLhMachineCount) {
            initLimit.maxChangeLhMachineCount = changeLhMachineCount;
        }
        return initLimit;
    }

    /**
     * 构建初始的
     *
     * @param productionDay
     */
    private DayCapacityLimitHelper(Integer productionDay) {
        this.productionDay = productionDay;
        this.maxCapacity = Integer.MAX_VALUE;
        this.minCapacity = BigDecimal.ZERO.intValue();
        this.capacityRatio = ProductionConstant.PERCENTAGE;
        this.maxChangeCxMachineCount = Integer.MAX_VALUE;
        this.maxChangeLhMachineCount = Integer.MAX_VALUE;
        this.usedChangeCxMachineCount = BigDecimal.ZERO.intValue();
        this.usedChangeLhMachineCount = BigDecimal.ZERO.intValue();
        this.sumProductionCapacityQty = BigDecimal.ZERO.intValue();
    }
}
