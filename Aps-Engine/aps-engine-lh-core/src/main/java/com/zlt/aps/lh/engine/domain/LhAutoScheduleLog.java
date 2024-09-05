package com.zlt.aps.lh.engine.domain;

import com.zlt.aps.common.core.domain.ApsBaseDto;
import lombok.Data;

import java.util.Date;

/**
 * 硫化自动排程工单过程日志表
 */
@Data
public class LhAutoScheduleLog extends ApsBaseDto {

    private Long id;

    /**
     * 排程日期
     */
    private Date scheduleDate;

    /**
     * 硫化工单号
     */
    private String orderNo;

    /**
     * 日志信息表
     */
    private String logDetail;
}
