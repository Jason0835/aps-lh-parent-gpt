package com.zlt.aps.gsq.engine.vo;

import lombok.Data;

/**
 * 缠绕盘-机台关系VO（多对多，来自 T_GSQ_TWINING_DISC_MACHINE）
 */
@Data
public class GsqTwiningDiscMachineVo {

    /**
     * 缠绕盘编码（关联 T_GSQ_TWINING_DISC.TWINING_DISC_CODE）
     */
    private String twiningDiscCode;

    /**
     * 机台编号，多个逗号分割（已过滤：启用盘+启用机台+启用关系）
     */
    private String machineIds;
}
