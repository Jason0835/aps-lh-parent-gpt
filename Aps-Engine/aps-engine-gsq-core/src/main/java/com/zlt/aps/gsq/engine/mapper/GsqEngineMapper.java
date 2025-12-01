package com.zlt.aps.gsq.engine.mapper;


import com.zlt.aps.common.engine.domain.EngineConstructionInfo;
import com.zlt.aps.gsq.engine.vo.GsqParamsVo;
import com.zlt.aps.gsq.engine.vo.GsqQuotaParam;
import com.zlt.aps.gsq.engine.vo.GsqScheduleBaseInfoVo;
import com.zlt.aps.gsq.engine.vo.GsqScheduleResultVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface GsqEngineMapper {

    /**
     * 根据成型排程记录 统计出 钢丝圈胶排程记录基础数据
     * @param scheduleDate 排程日期
     * @param productionStage 仅投产阶段规格排产标识
     * @return
     */
    List<GsqScheduleResultVo> statGsqScheduleBase(@Param("scheduleDate") String scheduleDate, @Param("productionStage") String productionStage);

    /**
     * 获取钢丝圈对应的成型胎胚code和机台code
     * @param scheduleDate
     * @return
     */
    List<GsqQuotaParam> listQuotaParam(@Param("scheduleDate") String scheduleDate, @Param("productionStage") String productionStage);

    /**
     * 创建自动排程记录
     * @param params
     */
    void createScheduleRecord(Map<String, Object> params);

    /**
     * 删除指定日期的排程数据
     * @param scheduleDate
     */
    void deleteGsqSchedule(@Param("scheduleDate") String scheduleDate);

    /**
     * 删除指定日期的外协排程数据
     * @param scheduleDate
     */
    void deleteGsqAssistSchedule(@Param("scheduleDate") String scheduleDate);

    /**
     * 把排程数据同步到log表
     * @param scheduleDate
     */
    void syncGsqScheduleToLog(@Param("scheduleDate") String scheduleDate);

    /**
     * 批量新增排程结果数据
     * @param scheduleResultList
     */
    void batchCreateScheduleResult(@Param("scheduleResultList") List<GsqScheduleResultVo> scheduleResultList);

    /**
     * 批量新增外协排程结果数据
     * @param scheduleResultList
     */
    void batchCreateAssistScheduleResult(@Param("scheduleResultList") List<GsqScheduleResultVo> scheduleResultList);

    /**
     * 返回钢丝圈参数计划
     * @return
     */
    List<GsqParamsVo> listGsqParams();

    /**
     * 查询当前排程的批次号
     * @param scheduleDate 排程日期 yyyy-MM-dd
     * @return
     */
    String getGsqCurrentBatchNo(@Param("scheduleDate") String scheduleDate);

    /**
     * 根据排程code查询出关联施工表的其他信息
     * @param steelRingCode 钢丝圈code列表
     * @return
     */
    List<GsqScheduleBaseInfoVo> listGsqScheduleBaseInfo(@Param("steelRingCodes") List<String> steelRingCode, @Param("productionStage") String productionStage);

    /**
     * 查询指定日期的排程数据
     * @param scheduleDate 排程日期
     * @return
     */
    List<GsqScheduleResultVo> listGsqEnginSchedule(@Param("scheduleDate") String scheduleDate);

//    /**
//     * 新增单挑钢丝圈排程记录
//     * @param scheduleResultVo
//     * @return
//     */
//    int insertGsqScheduleResult(GsqScheduleResultVo scheduleResultVo);

    /**
     * 批量合并排程结果表（根据唯一字段，做更新或新增）
     * @param scheduleResultList
     * @return
     */
    int mergeGsqScheduleResult(@Param("scheduleResultList") List<GsqScheduleResultVo> scheduleResultList);

    /**
     * 查询出钢丝圈需要的施工信息字段
     * @param scheduleDate 排程日期
     * @param productionStage 仅投产阶段规格排产标识
     * @return
     */
    List<EngineConstructionInfo> listGsqNeedConstruction(@Param("scheduleDate") String scheduleDate, @Param("productionStage") String productionStage);

    /**
     * 获得外协规格列表
     * @return
     */
    List<String> listAssistSpec();

    void batchUpdateBatchNoAndOrderNo(@Param("list") List<GsqScheduleResultVo> scheduleResultVoList);
}
