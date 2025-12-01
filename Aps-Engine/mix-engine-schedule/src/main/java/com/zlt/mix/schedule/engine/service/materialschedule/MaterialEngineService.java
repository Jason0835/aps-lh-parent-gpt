package com.zlt.mix.schedule.engine.service.materialschedule;

import com.zlt.mix.schedule.api.domain.entity.GlueDecomposePlan;
import com.zlt.mix.schedule.api.domain.entity.MaterialScheduleResult;
import com.zlt.mix.schedule.engine.vo.MaterialScheduleResultVo;
import com.zlt.mix.schedule.engine.vo.MaterialSpanVo;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 硫磺辅料日计划排程引擎接口
 */
public interface MaterialEngineService {

    /**
     * 硫磺辅料自动排程接口
     * @param scheduleDate  排产日期
     * @param mixArea  密炼区
     */
    void autoSchedule(Date scheduleDate, String mixArea);

    /**
     * 自动排程后，根据跨区设置表，自动生产相应的跨区发送和接收记录
     * @param mixArea 排程密炼区
     * @param scheduleDate  排程日期
     * @return
     */
    MaterialSpanVo autoCreateSpanRecord(String mixArea, Date scheduleDate);

    /**
     * 修改了各班计划量，修改了顺序。都需要把此机台下的排产重新进行计算
     * @param oldSchedule  修改前的排产信息
     * @param newSchedule  修改后的排产信息
     */
    List<MaterialScheduleResult> retrySchedule(MaterialScheduleResult oldSchedule, MaterialScheduleResult newSchedule);

    /**
     * 转机台后默认把转机台的排产放到最后，并重新计算顺序和预计完成时间
     * @param oldSchedule  转机台前的排产记录
     * @param schedule
     * @return
     */
    MaterialScheduleResult retryMachine(MaterialScheduleResult oldSchedule, MaterialScheduleResult schedule);

    /**
     * 转机台（新）。转机台后，创建新的排产记录；之前的记录保留。新机台上的各班计划量=原计划量 -  完成量
     * @param oldSchedule  转机台前的排产记录
     * @param schedule 排程记录
     * @return
     */
    List<MaterialScheduleResult> retryMachineNew(MaterialScheduleResult oldSchedule, MaterialScheduleResult schedule);

    /**
     * 当密炼区当天已经进行了硫磺辅料自动排程后，再去接收跨区的硫磺辅料的生产计划，此时接收的数据都会被安排到对应机台的最后去
     * @param mixArea  密炼区
     * @param scheduleDate  排程日期
     * @param receiveIds  跨区接收列表
     * @return
     */
    List<MaterialScheduleResult> spanReceivedEngine(String mixArea, Date scheduleDate, List<Long> receiveIds);

    /**
     * 批量导入引擎接口
     * @param scheduleDate
     * @param mixArea
     * @param list
     */
    void batchAddEngineSchedule(Date scheduleDate, String mixArea, List<MaterialScheduleResult> list);

    /**
     * 插单引擎接口（插后重新刷新同一个机台下的 预计完成时间）
     * @param schedule
     * @return
     */
    List<MaterialScheduleResult> addEngineSchedule(MaterialScheduleResult schedule);
    
    /**
     * 校验是否有生产顺序重复
     * @param scheduleResultVo
     * @param machineScheduleList
     */
    void checkProduceOrderRepeat(MaterialScheduleResult scheduleResultVo,
			List<MaterialScheduleResultVo> machineScheduleList);
}
