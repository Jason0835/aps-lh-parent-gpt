package com.zlt.aps.common.engine.domain;

import java.io.Serializable;
import java.util.Date;

import com.zlt.aps.common.core.domain.ApsBaseEntity;
import lombok.Data;

/**
 * 用于存储月度计划汇总过程涉及步骤日志
 * @TableName T_MONTH_SUM_PROCESS_LOG
 */
@Data
public class MonthSumProcessLog extends ApsBaseEntity {
    /**
     * 主键ID,序列SEQ_AUTO_SCHEDULE_LOG
     */
    private Long id;

    /**
     * 生产排程记录主计划版本号,年+月+日+01，02
     */
    private String monthPlanApsVersion;

    /**
     * 日志所属触发的模块描述
     */
    private String title;

    /**
     * 日志明细
     */
    private String logDetail;

    private static final long serialVersionUID = 1L;
}