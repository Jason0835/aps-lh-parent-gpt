package com.zlt.aps.lh.api.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 排程日期对象列表查询参数
 *
 * @author APS
 */
@Data
public class LhScheduleShiftDateQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 排程日期（窗口结束日），格式 yyyy-MM-dd */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date scheduleDate;

}
