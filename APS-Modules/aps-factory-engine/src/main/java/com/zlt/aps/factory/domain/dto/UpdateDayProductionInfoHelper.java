package com.zlt.aps.factory.domain.dto;

import lombok.Getter;

import java.io.Serializable;
import java.util.Set;

/**
 * 日产更新参数辅助类
 *
 * @author ZLT
 * @date 20251231
 */
@Getter
public class UpdateDayProductionInfoHelper implements Serializable {
    /**
     * 排产日
     */
    private Integer productionDay;
    /**
     * 排产量
     */
    private Integer realDayProductionQty;
    /**
     * 天产能是否消耗完成
     */
    private boolean isDayFinish;
    /**
     * 使用成型机信息
     */
    private Set<String> usedCxMachineInfo;
    /**
     * 换模或是换活字块损失的产能
     */
    private Integer lossQty;

    /**
     * 构造函数
     *
     * @param productionDay        排产日
     * @param realDayProductionQty 排产量
     * @param isDayFinish          天产能是否消耗完成
     * @param usedCxMachineInfo    使用的成型机
     * @param lossQty              换模或是换活字块损失的产能
     */
    public UpdateDayProductionInfoHelper(Integer productionDay, Integer realDayProductionQty, boolean isDayFinish, Set<String> usedCxMachineInfo, Integer lossQty) {
        this.productionDay = productionDay;
        this.realDayProductionQty = realDayProductionQty;
        this.isDayFinish = isDayFinish;
        this.usedCxMachineInfo = usedCxMachineInfo;
        this.lossQty = lossQty;
    }
}
