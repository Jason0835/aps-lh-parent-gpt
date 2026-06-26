package com.zlt.aps.tq.engine.mapper;


import com.zlt.aps.common.engine.domain.EngineConstructionInfo;
import com.zlt.aps.tq.api.domain.entity.TqStockShiftConfig;
import com.zlt.aps.tq.engine.vo.BeadMachineCountVo;
import com.zlt.aps.tq.engine.vo.TqParamsVo;
import com.zlt.aps.tq.engine.vo.TqScheduleBaseInfoVo;
import com.zlt.aps.tq.engine.vo.TqScheduleResultVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface TqEngineMapper {

    /**
     * 根据成型排程记录 统计出 胎圈胶排程记录基础数据
     * @param scheduleDate 排程日期
     * @param productionStage 仅投产阶段规格排产标识
     * @return
     */
    List<TqScheduleResultVo> statTqScheduleBase(@Param("scheduleDate") String scheduleDate, @Param("productionStage") String productionStage);

    /**
     * 创建自动排程记录
     * @param params
     */
    void createScheduleRecord(Map<String, Object> params);

    /**
     * 删除指定日期的排程数据
     * @param scheduleDate
     */
    void deleteTqSchedule(@Param("scheduleDate") String scheduleDate);

    /**
     * 删除指定日期的外协排程数据
     * @param scheduleDate
     */
    void deleteTqAssistSchedule(@Param("scheduleDate") String scheduleDate);

    /**
     * 把排程数据同步到log表
     * @param scheduleDate
     */
    void syncTqScheduleToLog(@Param("scheduleDate") String scheduleDate);

    /**
     * 批量新增排程结果数据
     * @param scheduleResultList
     */
    void batchCreateScheduleResult(@Param("scheduleResultList") List<TqScheduleResultVo> scheduleResultList);

    /**
     * 批量新增外协排程结果数据
     * @param scheduleResultList
     */
    void batchCreateAssistScheduleResult(@Param("scheduleResultList") List<TqScheduleResultVo> scheduleResultList);

    /**
     * 返回胎圈参数计划
     * @return
     */
    List<TqParamsVo> listTqParams();

    /**
     * 查询当前排程的批次号
     * @param scheduleDate 排程日期 yyyy-MM-dd
     * @return
     */
    String getTqCurrentBatchNo(@Param("scheduleDate") String scheduleDate);

    /**
     * 查询指定日期的排程数据
     * @param scheduleDate 排程日期
     * @return
     */
    List<TqScheduleResultVo> listTqEnginSchedule(@Param("scheduleDate") String scheduleDate);

    /**
     * 根据排程code查询出关联施工表的其他信息
     * @param beadCodes 胎圈code列表
     * @return
     */
    List<TqScheduleBaseInfoVo> listTqScheduleBaseInfo(@Param("beadCodes") List<String> beadCodes, @Param("productionStage") String productionStage);

//    /**
//     * 新增单挑胎圈排程记录
//     * @param scheduleResultVo
//     * @return
//     */
//    int insertTqScheduleResult(TqScheduleResultVo scheduleResultVo);

    /**
     * 批量合并排程结果表（根据唯一字段，做更新或新增）
     * @param scheduleResultList
     * @return
     */
    int mergeTqScheduleResult(@Param("scheduleResultList") List<TqScheduleResultVo> scheduleResultList);

    /**
     * 查询出胎圈需要的施工信息字段
     * @param scheduleDate 排程日期
     * @param productionStage 仅投产阶段规格排产标识
     * @return
     */
    List<EngineConstructionInfo> listTqNeedConstruction(@Param("scheduleDate") String scheduleDate, @Param("productionStage") String productionStage);

    /**
     * 获得外协规格列表
     * @return
     */
    List<String> listAssistSpec();

    int batchUpdateBatchNoAndOrderNo(@Param("list") List<TqScheduleResultVo> scheduleResultVoList);

    /**
     * 查询胎圈工装车容量数据（整车容量）
     * @return 胎圈编码→整车容量映射
     */
    List<Map<String, Object>> listToolingCartCapacity();

    /**
     * 查询胎圈机台检修计划数据
     * @param scheduleDate 排程日期
     * @param factoryCode 分厂编码（按工厂过滤检修计划）
     * @return 检修计划列表
     */
    List<Map<String, Object>> listMaintenancePlan(@Param("scheduleDate") String scheduleDate, @Param("factoryCode") String factoryCode);

    /**
     * 查询胎圈工作日历（停产班次信息）
     * @param scheduleDate 排程日期
     * @return 工作日历列表
     */
    List<Map<String, Object>> listWorkCalendar(@Param("scheduleDate") String scheduleDate);

    /**
     * 查询胎圈备库班数配置列表（按工厂过滤）
     * @param factoryCode 分厂编码（可选，为空则查询全部）
     * @return 备库班数配置列表
     */
    List<TqStockShiftConfig> listStockShiftConfig(@Param("factoryCode") String factoryCode);

    /**
     * 统计胎圈规格对应的成型机台数
     * 通过成型排程结果表与施工信息表关联，按胎圈编码分组统计DISTINCT成型机台数
     * @param scheduleDate 排程日期
     * @return 胎圈编码→成型机台数列表
     */
    List<BeadMachineCountVo> statBeadMachineCount(@Param("scheduleDate") String scheduleDate);
}
