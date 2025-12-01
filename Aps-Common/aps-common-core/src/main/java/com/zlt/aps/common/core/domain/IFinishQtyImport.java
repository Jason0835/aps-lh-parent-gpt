package com.zlt.aps.common.core.domain;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author Chen
 */
public interface IFinishQtyImport {

    /**
     * 获取排程日期
     * @return 结果
     */
    Date getScheduleDate();

    /**
     * 获取夜班计划量
     * @return 夜班计划量
     */
    BigDecimal getClass1FinishQty();

    /**
     * 获取早班计划量
     * @return 早班计划量
     */
    BigDecimal getClass2FinishQty();

    /**
     * 获取工单号
     * @return 结果
     */
    String getOrderNo();

    /**
     * 获取代码对应的字段值
     * @return 结果
     */
    String getCodeField();

    /**
     * 获取代码对应的字段值
     * @return 结果
     */
    String getCodeField1();

    /**
     * 赋值排程日期
     * @param scheduleDate 排程日期
     */
    void setScheduleDate(Date scheduleDate);

    /**
     * 赋值夜班计划量
     * @param nightFinishQty 夜班计划量
     */
    void setNightFinishQty(BigDecimal nightFinishQty);

    /**
     * 赋值早班计划量
     * @param dayFinishQty 早班计划量
     */
    void setDayFinishQty(BigDecimal dayFinishQty);

    /**
     * 赋值工单号
     * @param orderNo 工单号
     */
    void setOrderNo(String orderNo);

    /**
     * 设置创建人、创建时间、更新人、更新时间
     * @param id ID，为空赋值创建人、创建时间，不为空赋值更新人、更新时间
     */
    void setBaseVale(Long id);
}
