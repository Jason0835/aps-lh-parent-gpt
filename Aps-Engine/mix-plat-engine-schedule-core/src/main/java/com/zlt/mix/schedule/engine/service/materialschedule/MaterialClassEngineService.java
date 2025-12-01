package com.zlt.mix.schedule.engine.service.materialschedule;

import com.zlt.mix.schedule.api.domain.entity.MaterialScheduleResult;
import com.zlt.mix.schedule.engine.vo.MaterialScheduleResultVo;
import com.zlt.mix.setting.api.domain.entity.LhflMachine;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 硫磺辅料日计划排程引擎接口
 */
public interface MaterialClassEngineService {

    /**
     * 计算各班计划量、计划开始时间、计划完成时间、生产顺序
     * @param scheduleList
     * @param params  参数map
     * @param machineMap  机台信息map
     * @param publishScheduleList	已发布计划，用于确定每个机台每个班的初始编号
     */
    void staScheduleClassInfo(List<MaterialScheduleResultVo> scheduleList, Map<String, String> params, Map<String, LhflMachine> machineMap, List<MaterialScheduleResultVo> publishScheduleList);

    /**
     * 创建常用规格安全库存的排产记录。（常用规格保持每天用量的{safeStockRate}的安全库存。    ）
     * @param safeStockScheduleList 安全库存列表
     * @param scheduleList	排产列表
     * @param machineMap 机台列表
     * @param materialIntervalTime 不同胶料的间隔时间
     */
    void createSafeStockSchedule(List<MaterialScheduleResultVo> safeStockScheduleList, List<MaterialScheduleResultVo> scheduleList, Map<String, LhflMachine> machineMap, int materialIntervalTime);
    
    /**
     * 修改了中班生产顺序后，这个机台下的全部排程的计划完成时间都需要重新计算
     * @param scheduleList
     * @param materialIntervalTime
     */
    void modifyMidProduceOrder(List<MaterialScheduleResultVo> scheduleList , int materialIntervalTime);

    /**
     * 修改了夜班生产顺序后，这个机台下的全部排程的计划完成时间都需要重新计算
     * @param scheduleList
     * @param materialIntervalTime
     */
    void modifyNightProduceOrder(List<MaterialScheduleResultVo> scheduleList , int materialIntervalTime);

    /**
     * 修改了白班生产顺序后，这个机台下的全部排程的计划完成时间都需要重新计算
     * @param scheduleList
     * @param materialIntervalTime
     */
    void modifyDayProduceOrder(List<MaterialScheduleResultVo> scheduleList , int materialIntervalTime);

    /**
     * 转机台后重新排序
     * @param scheduleList
     * @param materialIntervalTime
     */
    void modifyMachine(List<MaterialScheduleResultVo> scheduleList , int materialIntervalTime);

    /**
     * 转机台后默认把转机台的排产放到最后，并重新计算顺序和预计完成时间
     * @param schedule
     * @param maxSchedule
     * @param materialIntervalTime
     * @param oldClassShift  转机台前的机台班制
     */
    void retryMachine(MaterialScheduleResultVo schedule, MaterialScheduleResult maxSchedule, int materialIntervalTime, int oldClassShift);

    /**
     * 转机台时，前后机台的班制不一样时，需要根据规则把计划量合并到新机台的班次中
     * @param schedule
     * @param oldClassShift
     */
    void transferClassShiftPlan(MaterialScheduleResultVo schedule, int oldClassShift);

    /**
     * 跨区批量接收记录，排到各个机台最后
     * @param spanReceiveList
     * @param scheduleList
     * @param materialIntervalTime
     */
    void batchSpanReceived(List<MaterialScheduleResultVo> spanReceiveList, List<MaterialScheduleResultVo> scheduleList, int materialIntervalTime);

    /**
     * 跨区接收后，排到各个机台最后
     * @param schedule
     * @param maxSchedule
     * @param oldScheduleList
     * @param materialIntervalTime
	 * @return 接收后会影响到的其他排程计划
     */
    List<MaterialScheduleResultVo> spanReceivedClassEngine(MaterialScheduleResultVo schedule, MaterialScheduleResult maxSchedule, List<MaterialScheduleResultVo> oldScheduleList, int materialIntervalTime);

    /**
	 * 合并跨区扣减需求，将同物料同机台的合并成一条。有正有负的情况，从正数的扣减对应的值
	 * @param spanScheduleList
	 * @return
	 */
	List<MaterialScheduleResultVo> mergeSubtractSpanSchedule(List<MaterialScheduleResultVo> spanScheduleList);
    
    /**
     * 清空对应班的字段信息
     * @param schedule
     * @param classType
     */
    void clearClassField(MaterialScheduleResult schedule, String classType);

}
