package com.zlt.aps.cx.service;

import com.zlt.aps.cx.vo.ScheduleRequestVo;
import com.zlt.aps.cx.vo.ScheduleResult;

/**
 * 排程管理服务接口
 *
 * 负责排程的整体流程管理，包括：
 * - 排程执行
 * - 重排程
 * - 动态调整
 * - 试制排程
 * - 排程验证
 *
 * @author APS Team
 */
public interface ScheduleService {

    /**
     * 执行排程（核心方法）
     *
     * @param request 排程请求
     * @return 排程结果
     */
    ScheduleResult executeSchedule(ScheduleRequestVo request);
}
