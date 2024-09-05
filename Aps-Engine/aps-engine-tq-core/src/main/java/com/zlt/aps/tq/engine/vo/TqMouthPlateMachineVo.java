package com.zlt.aps.tq.engine.vo;

import lombok.Data;

/**
 * 机台选择vo
 */
@Data
public class TqMouthPlateMachineVo {

    /**
     * 口型板代码
     */
    private String mouthPlateCode;

    /**
     * 机台id，多个逗号分割
     */
    private String machineIds;
}
