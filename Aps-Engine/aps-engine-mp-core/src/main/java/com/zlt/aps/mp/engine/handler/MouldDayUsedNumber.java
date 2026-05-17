package com.zlt.aps.mp.engine.handler;

import com.zlt.aps.mp.engine.constant.ProductionConstant;
import lombok.Getter;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 模具日使用数
 *
 * @author ZLT
 * @date 20260101
 */
@Getter
public class MouldDayUsedNumber implements Serializable {
    /**
     * 排产日
     */
    private Integer productionDay;
    /**
     * 使用的模具数
     */
    private Integer usedMoldNumber;
    /**
     * 使用的硫化机台数--即模具数/2
     */
    private Integer usedLhMachineCount;

    /**
     * 构建模具使用信息对象
     *
     * @param productionDay  排产日
     * @param usedMoldNumber 使用模具数
     */
    public MouldDayUsedNumber(Integer productionDay, Integer usedMoldNumber) {
        this.productionDay = productionDay;
        this.usedMoldNumber = usedMoldNumber;
        Integer lhMachineCount = usedMoldNumber / ProductionConstant.DOUBLE_MOULD_PRODUCTION;
        this.usedLhMachineCount = lhMachineCount;
    }

    /**
     * 构建空排产模具数对象
     *
     * @param productionDay
     * @return
     */
    public static MouldDayUsedNumber buildEmpty(Integer productionDay) {
        MouldDayUsedNumber empty = new MouldDayUsedNumber();
        empty.productionDay = productionDay;
        empty.usedMoldNumber = BigDecimal.ZERO.intValue();
        empty.usedLhMachineCount = BigDecimal.ZERO.intValue();
        return empty;
    }

    private MouldDayUsedNumber() {

    }
}