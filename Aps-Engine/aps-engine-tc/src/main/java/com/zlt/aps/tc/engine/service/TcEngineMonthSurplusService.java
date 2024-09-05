package com.zlt.aps.tc.engine.service;

import com.zlt.aps.tc.engine.vo.TcMonthSurplusVo;

import java.util.Map;

/**
 * 胎侧月度汇总service
 */
public interface TcEngineMonthSurplusService {

    /**
     * 获得月度计划剩余量、完成量
     * @param scheduleDate 排程日期
     * @return
     */
    Map<String, TcMonthSurplusVo> getMonthSurplus(String scheduleDate);
}
