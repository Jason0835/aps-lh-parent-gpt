package com.zlt.aps.gsq.engine.service;

import com.zlt.aps.gsq.api.domain.dto.GsqScheduleResultDto;
import com.zlt.aps.gsq.engine.vo.GsqScheduleResultVo;

import java.util.List;

public interface GsqEngineService {

    /**
     * 钢丝圈胶自动排程
     * @param scheduleDate 排程日期，格式：yyyy-MM-dd
     */
    void autoGsqSchedule(String scheduleDate);

    /**
     * 钢丝圈插单
     * @param scheduleVo
     */
    int inertGsqOrder(GsqScheduleResultVo scheduleVo);

    /**
     * 批量更新或新增排程记录信息
     * @param scheduleDate 排程日志，格式：yyyy-MM-dd
     * @param scheduleList 排程数据
     */
    int batchSaveGsqSchedule(String scheduleDate, List<GsqScheduleResultVo> scheduleList);

//    /**
//     * 转机台后，修改排程结果表相应字段数据
//     * @param oldMachineIds  转机台前，旧的机台id
//     * @param scheduleResultVo
//     */
//    void changeGsqMachine(String oldMachineIds, GsqScheduleResultVo scheduleResultVo);

    /**
     * 确认自动排程机台
     * @param scheduleResult  排程信息
     */
    void confirmGsqMachine(GsqScheduleResultDto scheduleResult);
}
