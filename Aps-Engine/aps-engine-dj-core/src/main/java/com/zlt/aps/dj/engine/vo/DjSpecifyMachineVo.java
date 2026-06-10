package com.zlt.aps.dj.engine.vo;

import lombok.Data;

/**
 * 机台选择vo
 */
@Data
public class DjSpecifyMachineVo {

    /**
     * 垫胶代码
     */
    private String liningCode;

    /**
     * 机台id，多个逗号分割
     */
    private String machineIds;
}
