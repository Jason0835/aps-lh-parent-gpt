package com.zlt.aps.mp.engine.domain.vo;

import lombok.Getter;

/**
 * 使用硫化组信息
 *
 * @author ZLT
 * @date 20260115
 */
@Getter
public class CxMachineUsedLhInfo {
    /**
     * 排产日
     */
    private Integer productionDay;
    /**
     * 排产数
     */
    private Integer usedLhMachineCount;

    /**
     * 创建对象
     *
     * @param productionDay
     * @param usedLhMachineCount
     * @return
     */
    public static CxMachineUsedLhInfo build(Integer productionDay, Integer usedLhMachineCount) {
        return new CxMachineUsedLhInfo(productionDay, usedLhMachineCount);
    }

    /**
     * 构造函数
     *
     * @param productionDay
     * @param usedLhMachineCount
     */
    private CxMachineUsedLhInfo(Integer productionDay, Integer usedLhMachineCount) {
        this.productionDay = productionDay;
        this.usedLhMachineCount = usedLhMachineCount;
    }
}
