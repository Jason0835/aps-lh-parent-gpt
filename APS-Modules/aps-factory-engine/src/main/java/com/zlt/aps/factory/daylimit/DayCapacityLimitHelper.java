package com.zlt.aps.factory.daylimit;

import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.scheduling.cxcapacity.ProductionCapacityParamConfiguration;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 日产能控制对象
 * 排产日、最大产能上限、最低产能，产能比例
 *
 * @author ZLT
 * @date 20250106
 */
@Data
public class DayCapacityLimitHelper implements Serializable {

    private Integer productionDay;

    private Integer maxCapacity;

    private Integer minCapacity;

    private Integer capacityRatio;

    /**
     * 根据产能比例，构建初始的日产能限制对象
     *
     * @param productionDay      排产日
     * @param paramConfiguration 参数
     * @param ratio              比例 1~100的值
     * @return
     */
    public static DayCapacityLimitHelper createInit(Integer productionDay, ProductionCapacityParamConfiguration paramConfiguration, Integer ratio) {
        DayCapacityLimitHelper initLimit = new DayCapacityLimitHelper();
        initLimit.setCapacityRatio(ratio);
        Integer dayMaxCapacity = paramConfiguration.getDayMaxCapacity();
        BigDecimal.valueOf(dayMaxCapacity).multiply(BigDecimal.valueOf(ProductionConstant.PERCENTAGE)).divide(BigDecimal.valueOf(ratio), 0, RoundingMode.UP);
        initLimit.setMaxCapacity(dayMaxCapacity);
        initLimit.setMinCapacity(paramConfiguration.getDayMinCapacity());
        initLimit.setProductionDay(productionDay);
        return initLimit;
    }
}
