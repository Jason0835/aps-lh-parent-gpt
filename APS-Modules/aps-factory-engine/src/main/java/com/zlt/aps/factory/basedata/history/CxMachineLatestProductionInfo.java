package com.zlt.aps.factory.basedata.history;

import lombok.Getter;

import java.io.Serializable;

/**
 * 机台近1个月生产分组的最近日期
 *
 * @author ZLT
 * @date 20260128
 */
@Getter
public class CxMachineLatestProductionInfo implements Serializable {
    /**
     * 机台编号
     */
    private String cxMachineCode;
    /**
     * 排产结构
     */
    private String groupName;
    /**
     * 最近排产日
     */
    private Integer productionDay;

    /**
     * 构造函数
     *
     * @param cxMachineCode 成型机台
     * @param groupName     分组
     * @param productionDay 最近排产日
     */
    public CxMachineLatestProductionInfo(String cxMachineCode, String groupName, Integer productionDay) {
        this.cxMachineCode = cxMachineCode;
        this.groupName = groupName;
        this.productionDay = productionDay;
    }
}
