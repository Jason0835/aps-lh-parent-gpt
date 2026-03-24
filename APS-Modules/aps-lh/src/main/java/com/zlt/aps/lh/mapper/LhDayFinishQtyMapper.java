package com.zlt.aps.lh.mapper;

import com.zlt.aps.lh.api.domain.entity.LhDayFinishQty;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：LhDayFinishQtyMapper.java
 * 描    述：硫化排程日完成量Mapper接口
 *@author zlt
 *@date 2025-02-21
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Mapper
public interface LhDayFinishQtyMapper extends CommBaseMapper<LhDayFinishQty> {

    /**
     * 更新月计划剩余量
     * @param scheduleYear 年份
     * @param scheduleMonth 月份
     */
    void updateMonthPlanSurplus(@Param("scheduleYear") Integer scheduleYear,
                                @Param("scheduleMonth") Integer scheduleMonth);

    /**
     * 根据硫化排程的完成量更新日完成量
     * @param scheduleDate 日期
     */
    void updateByScheduleResult(@Param("scheduleYear") Integer scheduleYear,
                                @Param("scheduleMonth") Integer scheduleMonth,
                                @Param("scheduleDate") Date scheduleDate);

    /**
     * 根据硫化排程的完成量新增日完成量
     *
     * @return 结果
     */
    int insertByScheduleResultNotExist(@Param("scheduleYear") Integer scheduleYear,
                                       @Param("scheduleMonth") Integer scheduleMonth,
                                       @Param("scheduleDate") Date scheduleDate);
}
