package com.zlt.mix.schedule.engine.mapper;

import com.zlt.mix.schedule.api.domain.entity.MaterialScheduleResult;
import com.zlt.mix.schedule.engine.vo.MaterialAreaMachineVo;
import com.zlt.mix.schedule.engine.vo.MaterialScheduleResultVo;
import com.zlt.mix.setting.api.domain.entity.LhflMachine;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 硫磺辅料排程引擎mapper
 */
public interface MaterialEngineMapper {

    /**
     * 从终炼母炼排程日计划中统计出 硫磺辅料日计划排程的基础信息
     * @param scheduleDate  排程日期
     * @param mixArea  密炼区
     * @return
     */
    List<MaterialScheduleResultVo> listBaseMaterialSchedule(@Param("scheduleDate") Date scheduleDate, @Param("mixArea")  String mixArea);

    /**
     * 跨区接受列表（其他密炼区发送给当前密炼区生产的物料列表）
     * @param scheduleDate 排程日期
     * @param mixArea 密炼区
     * @return
     */
    List<MaterialScheduleResultVo> listSpanReceive(@Param("scheduleDate") Date scheduleDate, @Param("mixArea")  String mixArea);

    /**
     * 根据id跨区接受列表（其他密炼区发送给当前密炼区生产的物料列表）
     * @param receiveIds 跨区接收id
     * @return
     */
    List<MaterialScheduleResultVo> listSpanReceiveByIds(@Param("receiveIds") List<Long> receiveIds);

    /**
     * 把硫磺辅料日计划排程同步到日志表中
     * @param scheduleDate
     * @param mixArea
     */
    void syncMaterialScheduleToLog(@Param("scheduleDate") Date scheduleDate, @Param("mixArea")  String mixArea);

    /**
     * 物理删除磺辅料日计划排程数据
     * @param scheduleDate  排程日期
     * @param mixArea  密炼区
     */
    void deleteMaterialSchedule(@Param("scheduleDate") Date scheduleDate, @Param("mixArea")  String mixArea);

    /**
     * 批量新增硫磺辅料排程记录
     * @param list
     */
    void batchInsertMaterialSchedule(@Param("list") List<MaterialScheduleResultVo> list);

    /**
     * 查询出常用规格的硫化辅料物料名称（近{commonlyUsedDay}日内都有排程的规格，就是常用规格）
     * @param mixArea  密炼区
     * @param scheduleDate  排程日期
     * @param commonlyUsedDay  近{commonlyUsedDay}日内都有排程的规格，就是常用规格
     * @return
     */
    List<String> listCommonlyUsedMaterial(@Param("mixArea") String mixArea, @Param("scheduleDate") Date scheduleDate,
                                          @Param("areaMaterialList") List<MaterialAreaMachineVo> areaMaterialList, @Param("commonlyUsedDay") int commonlyUsedDay);

    /**
     * 查询出机台下的排程信息
     * @param schedule
     * @param machineList  机台列表
     * @return
     */
    List<MaterialScheduleResultVo> listMaterialSchedule(@Param("schedule") MaterialScheduleResult schedule, @Param("machineList") List<String> machineList);

    /**
     * 查询出机台下各班最大的顺序 和 预计完成时间
     * @param schedule
     * @return
     */
    MaterialScheduleResult maxMachineOrderAndFinishTime(MaterialScheduleResult schedule);

    /**
     * 查询批次号
     * @param mixArea
     * @param scheduleDate
     * @return
     */
    String queryMaterialBatchNo(@Param("mixArea") String mixArea, @Param("scheduleDate") Date scheduleDate);

    /**
     * 查询当前硫磺辅料排程记录中的机台班制和产能信息
     * @param mixArea  密炼区
     * @param scheduleDate  排程日期
     * @param machineCode  机台编号
     * @return
     */
    LhflMachine queryScheduleMachineInfo(@Param("mixArea") String mixArea, @Param("scheduleDate") Date scheduleDate, @Param("machineCode") String machineCode);

    /**
     * 查询硫磺辅料安全库存排产信息
     * @param mixArea  密炼区
     * @return
     */
    List<MaterialScheduleResultVo> listSafeStockMaterialSchedule(@Param("mixArea") String mixArea);

    /**
     * 把硫磺辅料日计划排程同步到排程初始日志表中
     * @param scheduleDate
     * @param mixArea
     */
    void syncMaterialScheduleToInitLog(@Param("scheduleDate") Date scheduleDate, @Param("mixArea")  String mixArea);

    /**
     * 物理删除磺辅料日计划排程初始日志表数据
     * @param scheduleDate  排程日期
     * @param mixArea  密炼区
     */
    void deleteMaterialInitLog(@Param("scheduleDate") Date scheduleDate, @Param("mixArea")  String mixArea);
}
