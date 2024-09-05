package com.zlt.aps.nc.engine.service;

import com.zlt.aps.nc.api.domain.entity.NcScheduleResult;
import com.zlt.aps.nc.engine.vo.NcScheduleResultVo;

import java.util.List;

public interface NcEngineService {

    /**
     * 内衬胶自动排程
     * @param scheduleDate 排程日期，格式：yyyy-MM-dd
     */
    void autoNcSchedule(String scheduleDate);

    /**
     * 内衬插单
     * @param scheduleVo
     */
    int inertNcOrder(NcScheduleResultVo scheduleVo);

    /**
     * 批量更新或新增排程记录信息
     * @param scheduleDate 排程日志，格式：yyyy-MM-dd
     * @param scheduleList 排程数据
     */
    int batchSaveNcSchedule(String scheduleDate, List<NcScheduleResultVo> scheduleList);

//    /**
//     * 转机台后，修改排程结果表相应字段数据
//     * @param oldMachineIds  转机台前，旧的机台id
//     * @param scheduleResult
//     */
//    void changeNcMachine(String oldMachineIds, NcScheduleResult scheduleResult);

    /**
     * 确认自动排程机台
     * @param scheduleResult  排程信息
     */
    void confirmNcMachine(NcScheduleResult scheduleResult);

    /**
     * 手动均衡和重新设置生产顺序
     * @param scheduleDate 排程日期,格式：yyyy-mm-dd
     */
    void handEquilibriumAndProduceOrder(String scheduleDate);

    /**
     * 手动 同胶料合并生产
     * @param scheduleDate
     */
    void handGlueMerge(String scheduleDate);
}
