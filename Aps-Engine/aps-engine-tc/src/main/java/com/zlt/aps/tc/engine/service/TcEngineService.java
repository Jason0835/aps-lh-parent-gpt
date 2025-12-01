package com.zlt.aps.tc.engine.service;

import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import com.zlt.aps.tc.engine.vo.TcScheduleResultVo;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface TcEngineService {

    /**
     * 胎侧胶自动排程
     * @param scheduleDate 排程日期，格式：yyyy-MM-dd
     */
    void autoTcSchedule(String scheduleDate);

    /**
     * 胎侧插单
     * @param scheduleVo
     */
    int inertTcOrder(TcScheduleResultVo scheduleVo);

    /**
     * 批量更新或新增排程记录信息
     * @param scheduleDate 排程日志，格式：yyyy-MM-dd
     * @param scheduleList 排程数据
     */
    int batchSaveTcSchedule(String scheduleDate, List<TcScheduleResultVo> scheduleList);

//    /**
//     * 转机台后，修改排程结果表相应字段数据
//     * @param oldMachineIds  转机台前，旧的机台id
//     * @param scheduleResult
//     */
//    void changeTcMachine(String oldMachineIds, TcScheduleResult scheduleResult);

    /**
     * 确认自动排程机台
     * @param scheduleResult  排程信息
     */
    void confirmTcMachine(TcScheduleResult scheduleResult);

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

    /**
     * 批量设置批次号和订单号
     *
     * @param scheduleDate 排程日期，格式：yyyy-mm-dd
     */
    @Transactional(rollbackFor = Exception.class)
    void batchUpdateBatchNoAndOrderNo(String scheduleDate);
}
