package com.zlt.aps.tq.engine.service;

import com.zlt.aps.tq.engine.vo.TqMonthSurplusVo;

import java.util.Map;

/**
 * 胎圈月度汇总service
 */
public interface TqEngineMonthSurplusService {

    /**
     * 获得月度计划剩余量、完成量
     * @param scheduleDate 排程日期
     * @return
     */
    Map<String, TqMonthSurplusVo> getMonthSurplus(String scheduleDate);
}
