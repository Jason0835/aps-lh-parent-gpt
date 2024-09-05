package com.zlt.aps.lh.api.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import lombok.Data;

import java.util.Date;

/**
 * 甘特图vo
 */
@Data
public class Gante extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 机台编号/规格编号
     */
    private String codeId;

    /**
     * 开始日
     */
    private String startDay;

    /**
     * 结束日
     */
    private String endDay;

    /**
     * 排程日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date scheduleDate;

    /**
     * 开始日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date startDate;

    /**
     * 结束日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date endDate;

    /**
     * 开始时间
     */
    private String startHour;

    /**
     * 结束时间
     */
    private String endHour;

    /**
     * 规格/规格明细
     */
    private String innerMsg;

    /**
     * 类型（是否停产）
     */
    private String type;

    /**
     * SAP品号
     */
    private String cpdh;

    /**
     * 胎胚代号
     */
    private String specdh;

    /**
     * 阶段
     */
    private String jd;

    /**
     * 版次
     */
    private String bc;

    /**
     * 预定量
     */
    private String ydl;

    /**
     * 已完成量
     */
    private String ywcl;

    /**
     * 1=机台，2=规格
     */
    private int flag;

    /**
     * 时差间隔
     */
    private int hourInterval;

    /**
     * 72小时制的起始时间
     */
    private int hourStart;


}
