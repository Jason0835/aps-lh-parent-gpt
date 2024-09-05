package com.zlt.aps.tm.engine.service;

import com.zlt.aps.tm.engine.vo.TmMonthSurplusVo;

import java.util.Map;

/**
 * 胎面月度汇总service
 */
public interface TmEngineMonthSurplusService {

    /**
     * 获得月度计划剩余量、完成量
     * @param scheduleDate 排程日期
     * @return
     */
    Map<String, TmMonthSurplusVo> getMonthSurplus(String scheduleDate);
}
