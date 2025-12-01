package com.zlt.aps.tq.engine.service;

import com.zlt.aps.tq.api.domain.dto.TqScheduleResultDto;
import com.zlt.aps.tq.engine.vo.TqScheduleResultVo;

import java.util.List;

public interface TqEngineService {

    /**
     * 胎圈胶自动排程
     * @param scheduleDate 排程日期，格式：yyyy-MM-dd
     */
    void autoTqSchedule(String scheduleDate);

    /**
     * 胎圈插单
     * @param scheduleVo
     */
    int inertTqOrder(TqScheduleResultVo scheduleVo);

    /**
     * 批量更新或新增排程记录信息
     * @param scheduleDate 排程日志，格式：yyyy-MM-dd
     * @param scheduleList 排程数据
     */
    int batchSaveTqSchedule(String scheduleDate, List<TqScheduleResultVo> scheduleList);

//    /**
//     * 转机台后，修改排程结果表相应字段数据
//     * @param oldMachineIds  转机台前，旧的机台id
//     * @param scheduleResult
//     */
//    void changeTqMachine(String oldMachineIds, TqScheduleResultVo scheduleResult);

    /**
     * 确认自动排程机台
     * @param scheduleResult  排程信息
     */
    void confirmTqMachine(TqScheduleResultDto scheduleResult);

    void batchUpdateBatchNoAndOrderNo(String scheduleDate);
}
