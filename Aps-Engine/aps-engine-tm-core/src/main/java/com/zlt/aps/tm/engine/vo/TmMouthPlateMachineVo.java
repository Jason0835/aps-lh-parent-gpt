package com.zlt.aps.tm.engine.vo;

import lombok.Data;

/**
 * 机台选择vo
 */
@Data
public class TmMouthPlateMachineVo {

    /**
     * 口型板代码
     */
    private String mouthPlateCode;

    /**
     * 机台id，多个逗号分割
     */
    private String machineIds;
}
