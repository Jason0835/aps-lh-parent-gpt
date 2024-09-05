package com.zlt.aps.tc.engine.mapper;


import com.zlt.aps.common.engine.domain.EngineConstructionInfo;
import com.zlt.aps.tc.engine.vo.TcParamsVo;
import com.zlt.aps.tc.engine.vo.TcScheduleBaseInfoVo;
import com.zlt.aps.tc.engine.vo.TcScheduleResultVo;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface TcEngineMapper {

    /**
     * 根据成型排程记录 统计出 胎侧胶排程记录基础数据
     * @param scheduleDate 排程日期
     * @param productionStage 仅投产阶段规格排产标识
     * @return
     */
    List<TcScheduleResultVo> statTcScheduleBase(@Param("scheduleDate") String scheduleDate, @Param("productionStage") String productionStage);

    /**
     * 创建自动排程记录
     * @param params
     */
    void createScheduleRecord(Map<String, Object> params);

    /**
     * 删除指定日期的排程数据
     * @param scheduleDate
     */
    void deleteTcSchedule(@Param("scheduleDate") String scheduleDate);

    /**
     * 删除指定日期的外协排程数据
     * @param scheduleDate
     */
    void deleteTcAssistSchedule(@Param("scheduleDate") String scheduleDate);


    /**
     * 把排程数据同步到log表
     * @param scheduleDate
     */
    void syncTcScheduleToLog(@Param("scheduleDate") String scheduleDate);

    /**
     * 批量新增排程结果数据
     * @param scheduleResultList
     */
    void batchCreateScheduleResult(@Param("scheduleResultList") List<TcScheduleResultVo> scheduleResultList);

    /**
     * 批量新增外协排程结果数据
     * @param scheduleResultList
     */
    void batchCreateAssistScheduleResult(@Param("scheduleResultList") List<TcScheduleResultVo> scheduleResultList);

    /**
     * 返回胎侧参数计划
     * @return
     */
    List<TcParamsVo> listTcParams();

    /**
     * 查询当前排程的批次号
     * @param scheduleDate 排程日期 yyyy-MM-dd
     * @return
     */
    String getTcCurrentBatchNo(@Param("scheduleDate") String scheduleDate);

    /**
     * 根据排程id查询出关联施工表的其他信息
     * @param sidewallCode 胎侧code
     * @return
     */
    TcScheduleResultVo getTcScheduleBaseInfo(@Param("sidewallCode") String sidewallCode);

    /**
     * 根据排程code查询出关联施工表的其他信息
     * @param sidewallCodes 胎侧code列表
     * @return
     */
    List<TcScheduleBaseInfoVo> listTcScheduleBaseInfo(@Param("sidewallCodes") List<String> sidewallCodes, @Param("productionStage") String productionStage);

//    /**
//     * 新增单挑胎侧排程记录
//     * @param scheduleResultVo
//     * @return
//     */
//    int insertTcScheduleResult(TcScheduleResultVo scheduleResultVo);

    /**
     * 批量合并排程结果表（根据唯一字段，做更新或新增）
     * @param scheduleResultList
     * @return
     */
    int mergeTcScheduleResult(@Param("scheduleResultList") List<TcScheduleResultVo> scheduleResultList);

    /**
     * 查询出胎侧需要的施工信息字段
     * @param scheduleDate 排程日期
     * @param productionStage 仅投产阶段规格排产标识
     * @return
     */
    List<EngineConstructionInfo> listTcNeedConstruction(@Param("scheduleDate") String scheduleDate, @Param("productionStage") String productionStage);

    /**
     * 查询指定日期的排程数据
     * @param scheduleDate 排程日期
     * @return
     */
    List<TcScheduleResultVo> listTcEnginSchedule(@Param("scheduleDate") String scheduleDate);

    /**
     * 批量更新各班的生产顺序
     * @param scheduleDate 排程日期
     * @param scheduleResultList 排程列表
     */
    void batchUpdateProduceOrder(@Param("scheduleDate") String scheduleDate, @Param("list") List<TcScheduleResultVo> scheduleResultList);

    /**
     * 批量更新各班的计划量
     * @param scheduleDate 排程日期
     * @param scheduleResultList 排程列表
     */
    void batchUpdatePlanQty(@Param("scheduleDate") String scheduleDate, @Param("list") List<TcScheduleResultVo> scheduleResultList);

    /**
     * 获得外协规格列表
     * @return
     */
    List<String> listAssistSpec();

	/**
	 * 查询当天的收尾规格
	 * 
	 * @param scheduleDate   排程日期
	 * @param closeOutDays   收尾判断天数，
	 * @param isProductStage 是否投产规格
	 */
	List<String> listCloseOutSpec(@Param("scheduleDate") Date scheduleDate, @Param("closeOutDays") int closeOutDays,
			@Param("isProductStage") boolean isProductStage);
}
