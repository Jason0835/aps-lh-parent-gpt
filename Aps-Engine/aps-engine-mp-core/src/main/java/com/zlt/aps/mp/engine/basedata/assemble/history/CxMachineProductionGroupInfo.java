package com.zlt.aps.mp.engine.basedata.assemble.history;

import lombok.Getter;

import java.io.Serializable;

/**
 * 机台近n个月生产分组的次数
 *
 * @author ZLT
 * @date 20260128
 */
@Getter
public class CxMachineProductionGroupInfo implements Serializable {
    /**
     * 成型机台编号
     */
    private String cxMachineCode;
    /**
     * 排产分组
     */
    private String groupName;
    /**
     * 近n个月排产次数
     */
    private Integer productionCount;

    /**
     * 构造函数
     *
     * @param cxMachineCode
     * @param groupName
     * @param productionCount
     */
    public CxMachineProductionGroupInfo(String cxMachineCode, String groupName, Integer productionCount) {
        this.cxMachineCode = cxMachineCode;
        this.groupName = groupName;
        this.productionCount = productionCount;
    }
}
