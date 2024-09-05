package com.zlt.aps.tm.engine.vo;

import lombok.Data;

/**
 * 机台选择vo
 */
@Data
public class TmSpecifyMachineVo {

    /**
     * 胎面代码
     */
    private String treadCode;

    /**
     * 机台id，多个逗号分割
     */
    private String machineIds;
}
