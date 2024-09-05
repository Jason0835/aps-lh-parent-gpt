package com.zlt.aps.gsq.engine.vo;

import lombok.Data;

/**
 * 机台选择vo
 */
@Data
public class GsqSpecifyMachineVo {

    /**
     * 钢丝圈代码
     */
    private String steelRingCode;

    /**
     * 机台id，多个逗号分割
     */
    private String machineIds;
}
