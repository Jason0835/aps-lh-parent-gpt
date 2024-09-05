package com.zlt.aps.gsq.engine.vo;

import lombok.Data;

/**
 * 缠绕盘
 */
@Data
public class GsqTwiningDiscMachineVo {

    /**
     * 规格尺寸
     */
    private String spec;

    /**
     * 钢丝圈排列方式
     */
    private String orderWay;

    /**
     * 机台id，多个逗号分割
     */
    private String machineIds;
}
