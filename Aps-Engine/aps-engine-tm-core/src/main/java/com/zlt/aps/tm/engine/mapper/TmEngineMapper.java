package com.zlt.aps.tm.engine.mapper;


import com.zlt.aps.common.engine.domain.EngineConstructionInfo;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.engine.vo.TmParamsVo;
import com.zlt.aps.tm.engine.vo.TmScheduleBaseInfoVo;
import com.zlt.aps.tm.engine.vo.TmScheduleResultVo;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface TmEngineMapper {

    /**
     * 根据成型排程记录 统计出 胎面胶排程记录基础数据
     * @param scheduleDate 排程日期
     * @param productionStage 仅投产阶段规格排产标识
     * @return
     */
    List<TmScheduleResultVo> statTmScheduleBase(@Param("scheduleDate") String scheduleDate, @Param("productionStage") String productionStage);

    /**
     * 创建自动排程记录
     * @param params
     */
    void createScheduleRecord(Map<String, Object> params);

    /**
     * 删除指定日期的排程数据
     * @param scheduleDate
     */
    void deleteTmSchedule(@Param("scheduleDate") String scheduleDate);

    /**
     * 删除指定日期的外协排程数据
     * @param scheduleDate
     */
    void deleteTmAssistSchedule(@Param("scheduleDate") String scheduleDate);

    /**
     * 把排程数据同步到log表
     * @param scheduleDate
     */
    void syncTmScheduleToLog(@Param("scheduleDate") String scheduleDate);

    /**
     * 批量新增排程结果数据
     * @param scheduleResultList
     */
    void batchCreateScheduleResult(@Param("scheduleResultList") List<TmScheduleResultVo> scheduleResultList);

    /**
     * 批量新增外协排程结果数据
     * @param scheduleResultList
     */
    void batchCreateAssistScheduleResult(@Param("scheduleResultList") List<TmScheduleResultVo> scheduleResultList);

    /**
     * 返回胎面参数计划
     * @return
     */
    List<TmParamsVo> listTmParams();

    /**
     * 查询当前排程的批次号
     * @param scheduleDate 排程日期 yyyy-MM-dd
     * @return
     */
    String getTmCurrentBatchNo(@Param("scheduleDate") String scheduleDate);

    /**
     * 根据排程code查询出关联施工表的其他信息
     * @param treadCodes 胎面code列表
     * @return
     */
    List<TmScheduleBaseInfoVo> listTmScheduleBaseInfo(@Param("treadCodes") List<String> treadCodes, @Param("productionStage") String productionStage);

//    /**
//     * 新增单挑胎面排程记录
//     * @param scheduleResultVo
//     * @return
//     */
//    int insertTmScheduleResult(TmScheduleResultVo scheduleResultVo);

    /**
     * 批量合并排程结果表（根据唯一字段，做更新或新增）
     * @param scheduleResultList
     * @return
     */
    int mergeTmScheduleResult(@Param("scheduleResultList") List<TmScheduleResultVo> scheduleResultList);

    /**
     * 查询出胎面需要的施工信息字段
     * @param scheduleDate 排程日期
     * @param productionStage 仅投产阶段规格排产标识
     * @return
     */
    List<EngineConstructionInfo> listTmNeedConstruction(@Param("scheduleDate") String scheduleDate, @Param("productionStage") String productionStage);

    /**
     * 查询指定日期的排程数据
     * @param scheduleDate 排程日期
     * @return
     */
    List<TmScheduleResultVo> listTmEnginSchedule(@Param("scheduleDate") String scheduleDate);

    /**
     * 批量更新各班的生产顺序
     * @param scheduleDate 排程日期
     * @param scheduleResultList 排程列表
     */
    void batchUpdateProduceOrder(@Param("scheduleDate") String scheduleDate, @Param("list") List<TmScheduleResultVo> scheduleResultList);

    /**
     * 获得外协规格列表
     * @return
     */
    List<String> listAssistSpec();

    int createTempTable();

    int dropTempTable();

    int insertTempTable(@Param("scheduleResultList") List<TmScheduleResultVo> scheduleResultList);

    /**
     * 批量更新各班的计划量
     * @param scheduleDate 排程日期
     * @param scheduleResultList 排程列表
     */
    void batchUpdatePlanQty(@Param("scheduleDate") String scheduleDate, @Param("list") List<TmScheduleResultVo> scheduleResultList);

	/**
	 * 查询当天的收尾规格
     *
	 * @param scheduleDate   排程日期
	 * @param closeOutDays   收尾判断天数，
	 * @param isProductStage 是否投产规格
	 */
	List<String> listCloseOutSpec(@Param("scheduleDate") Date scheduleDate, @Param("closeOutDays") int closeOutDays,
			@Param("isProductStage") boolean isProductStage);

    /**
     * 查询成型消耗量
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    List<TmScheduleResult> getCxConsume4List(@Param("scheduleDate") String scheduleDate);

    /**
     * 根据ID批量更新对应的批次号及工单号
     *
     * @param tmScheduleResultList 排程列表
     * @return 影响行数
     */
    int batchUpdateBatchNoAndOrderNo(@Param("list") List<TmScheduleResultVo> tmScheduleResultList);
}
