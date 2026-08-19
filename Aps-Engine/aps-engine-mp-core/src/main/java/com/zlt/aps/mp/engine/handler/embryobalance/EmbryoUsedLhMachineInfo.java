package com.zlt.aps.mp.engine.handler.embryobalance;

import lombok.Getter;

import java.io.Serializable;

/**
 * 胎胚平衡检查业务
 * 胎胚使用硫化机台信息
 *
 * @author ZLT
 * @date 20260814
 */
@Getter
public class EmbryoUsedLhMachineInfo implements Serializable {
    /**
     * 生胎代码
     */
    private String embryoCode;
    /**
     * 使用硫化机台数
     */
    private Integer usedLhMachines;

    public EmbryoUsedLhMachineInfo(String embryoCode, Integer usedLhMachines) {
        this.embryoCode = embryoCode;
        this.usedLhMachines = usedLhMachines;
    }
}
