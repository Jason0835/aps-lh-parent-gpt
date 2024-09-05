package com.zlt.aps.tq.engine.vo;

import lombok.Data;

/**
 * 机台选择vo
 */
@Data
public class TqSpecifyMachineVo {

    /**
     * 胎圈代码
     */
    private String beadCode;

    /**
     * 机台id，多个逗号分割
     */
    private String machineIds;
}
