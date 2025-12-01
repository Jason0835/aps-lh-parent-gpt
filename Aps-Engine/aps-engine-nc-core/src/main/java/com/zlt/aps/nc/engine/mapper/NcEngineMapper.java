package com.zlt.aps.nc.engine.mapper;


import com.zlt.aps.common.engine.domain.EngineConstructionInfo;
import com.zlt.aps.nc.engine.vo.NcParamsVo;
import com.zlt.aps.nc.engine.vo.NcScheduleBaseInfoVo;
import com.zlt.aps.nc.engine.vo.NcScheduleResultVo;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface NcEngineMapper {

    /**
     * 根据成型排程记录 统计出 内衬胶排程记录基础数据
     * @param scheduleDate 排程日期
     * @param productionStage 仅投产阶段规格排产标识
     * @return
     */
    List<NcScheduleResultVo> statNcScheduleBase(@Param("scheduleDate") String scheduleDate, @Param("productionStage") String productionStage);

    /**
     * 创建自动排程记录
     * @param params
     */
    void createScheduleRecord(Map<String, Object> params);

    /**
     * 删除指定日期的排程数据
     * @param scheduleDate
     */
    void deleteNcSchedule(@Param("scheduleDate") String scheduleDate);

    /**
     * 删除指定日期的外协排程数据
     * @param scheduleDate
     */
    void deleteNcAssistSchedule(@Param("scheduleDate") String scheduleDate);

    /**
     * 把排程数据同步到log表
     * @param scheduleDate
     */
    void syncNcScheduleToLog(@Param("scheduleDate") String scheduleDate);

    /**
     * 批量新增排程结果数据
     * @param scheduleResultList
     */
    void batchCreateScheduleResult(@Param("scheduleResultList") List<NcScheduleResultVo> scheduleResultList);

    /**
     * 批量新增外协排程结果数据
     * @param scheduleResultList
     */
    void batchCreateAssistScheduleResult(@Param("scheduleResultList") List<NcScheduleResultVo> scheduleResultList);

    /**
     * 返回内衬参数计划
     * @return
     */
    List<NcParamsVo> listNcParams();

    /**
     * 查询当前排程的批次号
     * @param scheduleDate 排程日期 yyyy-MM-dd
     * @return
     */
    String getNcCurrentBatchNo(@Param("scheduleDate") String scheduleDate);

    /**
     * 根据排程code查询出关联施工表的其他信息
     * @param liningCodes 内衬code列表
     * @return
     */
    List<NcScheduleBaseInfoVo> listNcScheduleBaseInfo(@Param("liningCodes") List<String> liningCodes, @Param("productionStage") String productionStage);

//    /**
//     * 新增单挑内衬排程记录
//     * @param scheduleResultVo
//     * @return
//     */
//    int insertNcScheduleResult(NcScheduleResultVo scheduleResultVo);

    /**
     * 批量合并排程结果表（根据唯一字段，做更新或新增）
     * @param scheduleResultList
     * @return
     */
    int mergeNcScheduleResult(@Param("scheduleResultList") List<NcScheduleResultVo> scheduleResultList);

    /**
     * 查询出内衬需要的施工信息字段
     * @param scheduleDate 排程日期
     * @param productionStage 仅投产阶段规格排产标识
     * @return
     */
    List<EngineConstructionInfo> listNcNeedConstruction(@Param("scheduleDate") String scheduleDate, @Param("productionStage") String productionStage);

    /**
     * 查询指定日期的排程数据
     * @param scheduleDate 排程日期
     * @return
     */
    List<NcScheduleResultVo> listNcEnginSchedule(@Param("scheduleDate") String scheduleDate);

    /**
     * 批量更新各班的生产顺序
     * @param scheduleDate 排程日期
     * @param scheduleResultList 排程列表
     */
    void batchUpdateProduceOrder(@Param("scheduleDate") String scheduleDate, @Param("list") List<NcScheduleResultVo> scheduleResultList);

    int createTempTable();

    int dropTempTable();

    int insertTempTable(@Param("scheduleResultList") List<NcScheduleResultVo> scheduleResultList);

    /**
     * 获得外协规格列表
     * @return
     */
    List<String> listAssistSpec();


    /**
     * 批量更新各班的计划量
     * @param scheduleDate 排程日期
     * @param scheduleResultList 排程列表
     */
    void batchUpdatePlanQty(@Param("scheduleDate") String scheduleDate, @Param("list") List<NcScheduleResultVo> scheduleResultList);

	/**
	 * 查询当天的收尾规格
     *
	 * @param scheduleDate   排程日期
	 * @param closeOutDays   收尾判断天数，
	 * @param isProductStage 是否投产规格
	 */
	List<String> listCloseOutSpec(@Param("scheduleDate") Date scheduleDate, @Param("closeOutDays") int closeOutDays,
			@Param("isProductStage") boolean isProductStage);

    int batchUpdateBatchNoAndOrderNo(@Param("list") List<NcScheduleResultVo> scheduleResultVoList);

}
