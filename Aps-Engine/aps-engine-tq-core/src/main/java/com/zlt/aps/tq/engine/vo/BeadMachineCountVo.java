package com.zlt.aps.tq.engine.vo;

import lombok.Data;

/**
 * 胎圈规格对应成型机台数统计VO（排程引擎专用）。
 *
 * <p>通过成型排程结果表与施工信息表关联统计，
 * 用于胎圈备库班数配置匹配时获取该胎圈规格对应的成型机台数。</p>
 *
 * @author APS
 */
@Data
public class BeadMachineCountVo {

    /** 胎圈编码 */
    private String beadCode;

    /** 成型机台数（DISTINCT CX_MACHINE_CODE 统计） */
    private Integer machineCount;
}
