package com.zlt.aps.tm.service;

import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.bill.common.service.IDocService;

import java.util.Date;

/**
 * 胎面排程结果表 服务接口
 */
public interface ITmScheduleResultService extends IDocService<TmScheduleResult> {

    /**
     * 修改胎面排程结果
     * @param scheduleResult 胎面排程结果
     * @return 结果
     */
    int updateTmScheduleResult(TmScheduleResult scheduleResult);

    /**
     * 根据id查询当前日期发布状态为"发布中"或"超时失败"的记录数
     * @param ids id数组
     * @return 符合条件的记录数
     */
    int isReleasingOrTimeoutByIds(Long[] ids);

    /**
     * 记录调度员操作日志
     * @param operType 操作类型：0--转机台、1--调量
     * @param newSchedule 操作后的排程数据
     */
    void insetDispatcherLog(String operType, TmScheduleResult newSchedule);

    /**
     * 按工厂和排程日期逻辑删除当前有效批次数据
     * @param factoryCode 工厂编号
     * @param scheduleDate 排程日期
     */
    void logicDeleteByFactoryCodeAndScheduleDate(String factoryCode, Date scheduleDate);
}
