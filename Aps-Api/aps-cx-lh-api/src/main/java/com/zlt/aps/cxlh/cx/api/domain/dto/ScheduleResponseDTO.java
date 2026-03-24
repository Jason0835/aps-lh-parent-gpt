package com.zlt.aps.cxlh.cx.api.domain.dto;


import com.zlt.aps.cxlh.cx.api.domain.entity.CxScheduleResult;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 排程响应DTO
 */
@Data
public class ScheduleResponseDTO {

    /** 版本号 */
    private String versionNo;

    /** 排程日期 */
    private Date scheduleDate;

    /** 总任务数 */
    private int totalTasks;

    /** 已完成任务数 */
    private int completedTasks;

    /** 排程结果列表 */
    private List<CxScheduleResult> scheduleList;

    /** 消息 */
    private String message;

    /** 是否成功 */
    private Boolean success;
}
