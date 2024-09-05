package com.zlt.aps.gsq.engine.service;

import com.zlt.aps.gsq.engine.vo.GsqMonthSurplusVo;

import java.util.Map;

/**
 * 钢丝圈月度汇总service
 */
public interface GsqEngineMonthSurplusService {

    /**
     * 获得月度计划剩余量、完成量
     * @param scheduleDate 排程日期
     * @return
     */
    Map<String, GsqMonthSurplusVo> getMonthSurplus(String scheduleDate);
}
