package com.zlt.aps.common.core.domain.vo;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 自动滚动解释只读查询参数，工厂权限在BootUI检查。
 */
@Data
public class AutoRollingExplainQuery {
    /**
     * 必选工厂。
     */
    private String factoryCode;
    /**
     * 排程日期。
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate scheduleDate;
    /**
     * 班序。
     */
    private Integer shiftOrder;
    /**
     * 精确物料编码。
     */
    private String materialCode;
    /**
     * 业务终态。
     */
    private String status;
    /**
     * 执行详情中的物料状态筛选；与执行摘要状态筛选分开。
     */
    private String itemStatus;
    /**
     * 开始时间。
     */
    private String startTime;
    /**
     * 结束时间。
     */
    private String endTime;
    /**
     * 页码。
     */
    private Integer pageNum = 1;
    /**
     * 每页条数。
     */
    private Integer pageSize = 20;
}
