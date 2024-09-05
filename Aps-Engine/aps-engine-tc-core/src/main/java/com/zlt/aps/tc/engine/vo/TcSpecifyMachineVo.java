package com.zlt.aps.tc.engine.vo;

import lombok.Data;

/**
 * 机台选择vo
 */
@Data
public class TcSpecifyMachineVo {

    /**
     * 胎侧代码
     */
    private String sidewallCode;

    /**
     * 机台id，多个逗号分割
     */
    private String machineIds;
}
