package com.zlt.aps.factory.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 模具维修返厂对象
 *
 * @author ZLT
 * @date 20250306
 */
@Data
public class MouldMaintenanceConfigurationVo implements Serializable {

    /**
     * 分厂编号
     */
    private String factoryCode;

    /**
     * 年份
     */
    private Integer year;

    /**
     * 月份
     */
    private Integer month;

    /**
     * 模具号
     */
    private String mouldCode;
    /**
     * 模具
     */
    private String mouldNo;
    /**
     * 维修开始时间 yyyy-MM-DD
     */
    private Date beginDate;

    /**
     * 结束日期:yyyy-MM-DD
     */
    private Date endDay;
}