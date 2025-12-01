package com.zlt.mix.common.engine.domain;

import com.zlt.mix.common.core.domain.ZltBaseEntity;
import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * 自动排程日志对象 t_auto_schedule_log
 * 
 * @author zlt
 * @date 2021-07-16
 */
@Data
@ApiModel(value = "自动排程日志对象", description = "自动排程日志对象 ")
public class AutoScheduleLog extends ZltBaseEntity{

    private static final long serialVersionUID = 1L;

    public AutoScheduleLog() {
    }

    public AutoScheduleLog(String procedureCode, String batchNo, String orderNo, String title, String logDetail) {
        this.procedureCode = procedureCode;
        this.batchNo = batchNo;
        this.orderNo = orderNo;
        this.title = title;
        this.logDetail = logDetail;
    }

    /** 主键ID，对应序列SEQ_T_AUTO_SCHEDULE_LOG */
    private Long id;

    private String procedureCode;

    /** 批次号 */
    private String batchNo;

    /** 工单号 */
    private String orderNo;

    /** 标题 */
    private String title;

    /** 日志明细 */
    private String logDetail;
}
