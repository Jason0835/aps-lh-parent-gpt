package com.zlt.aps.factory.domain.dto;

import lombok.Getter;

import java.io.Serializable;
import java.util.Set;

/**
 * 排产计划-成型停机信息
 *
 * @author ZLT
 * @date 20251215
 */
@Getter
public class CxDevicePlanShutInfoHelper implements Serializable {

    /**
     * 成型机台
     */
    private String cxMachineCode;

    /**
     * 停机天数
     */
    private Set<Integer> stopDaySet;

    /**
     * 构造函数
     *
     * @param cxMachineCode
     * @param stopDaySet
     */
    public CxDevicePlanShutInfoHelper(String cxMachineCode, Set<Integer> stopDaySet) {
        this.cxMachineCode = cxMachineCode;
        this.stopDaySet = stopDaySet;
    }
}
