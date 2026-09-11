package com.zlt.aps.lh.api.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 临时故障原始禁产窗口，仅供时间产能计算及设备计划日期回填。
 * 不参与维修、清洗、精度、换模或换活字块的业务调度。
 */
@Data
public class MachineFaultWindowDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 来源设备停机计划主键。 */
    private Long planId;
    /** 来源机台编码。 */
    private String machineCode;
    /** 故障原始开始时间，也是实际开始时间。 */
    private Date startTime;
    /** 故障原始结束时间，也是实际结束时间，无预热。 */
    private Date endTime;
}
