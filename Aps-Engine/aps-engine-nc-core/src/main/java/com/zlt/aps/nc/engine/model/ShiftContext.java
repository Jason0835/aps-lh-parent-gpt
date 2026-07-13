package com.zlt.aps.nc.engine.model;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.zlt.aps.nc.api.domain.entity.NcScheduleResult;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 顺延上下文
 *
 * @author zlt
 */
@Data
@Accessors(chain = true)
public class ShiftContext {
    /** 工厂编码 */
    private String factoryCode;
    /** 排产日期 */
    private Date scheduleDate;
    /** 机台编码 */
    private String machineCode;
    /** 目标班次索引（1~6） */
    private int targetClass;
    /** 目标顺位 */
    private int targetSeq;
    /** 插单/调整规格名称 */
    private String insertSpecName;
    /** 插单计划量 */
    private BigDecimal insertPlanQty;
    /** 当前排产日所有排程结果列表 */
    private List<NcScheduleResult> scheduleResults;
    /** 操作类型：insert/adjust */
    private String operType;
    /** 操作人 */
    private String operator;
}
