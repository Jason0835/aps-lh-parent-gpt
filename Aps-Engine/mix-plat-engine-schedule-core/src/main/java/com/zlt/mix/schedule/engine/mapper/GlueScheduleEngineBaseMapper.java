package com.zlt.mix.schedule.engine.mapper;

import com.zlt.mix.schedule.api.domain.entity.GlueCollectPlan;
import com.zlt.mix.schedule.api.domain.entity.GlueDecomposePlan;
import com.zlt.mix.schedule.engine.vo.GlueDecomposePlanVo;
import com.zlt.mix.schedule.engine.vo.GlueScheduleResultVo;
import com.zlt.mix.setting.api.domain.entity.GlueFinish;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 胶料排程引擎mapper
 * 
 * @author hakimryan
 *
 */
public interface GlueScheduleEngineBaseMapper {
	/**
	 * 查询胶料分解数据
	 * 
	 * @param scheduleDate 排产日
	 * @param mixArea      密炼区
	 * @param glueCode     胶料（可以为空）
	 * @return
	 */
	List<GlueDecomposePlanVo> selectGlueDecomposePlan(@Param("scheduleDate") Date scheduleDate,
			@Param("mixArea") String mixArea, @Param("glueCode") String glueCode);

	/**
	 * 删除指定排产日的排程记录
	 * 
	 * @param scheduleDate 排产日
	 * @param mixArea      密炼区
	 */
	void deleteScheduleResultList(@Param("scheduleDate") Date scheduleDate, @Param("mixArea") String mixArea);

	/**
	 * 将排指定排产日的排程记录拷贝至日志表中
	 * 
	 * @param scheduleDate 排产日
	 * @param mixArea      密炼区
	 */
	void copyScheduleResultListToLog(@Param("scheduleDate") Date scheduleDate, @Param("mixArea") String mixArea);

	/**
	 * 批量保存排程结果
	 * 
	 * @param list
	 */
	void batchInsertScheduleResult(@Param("list") List<GlueScheduleResultVo> list);

	/**
	 * 通过组合过滤条件查询终炼母炼日计划列表
	 * 
	 * @param idList       ID列表
	 * @param releaseState 发布状态
	 */
	List<GlueScheduleResultVo> selectScheduleResult(@Param("params") GlueScheduleResultVo params);

	/**
	 * 合并排程数据
	 * 
	 * @param idList       ID列表
	 * @param releaseState 发布状态
	 */
	void mergeScheduleResult(@Param("list") List<GlueScheduleResultVo> list);

	/**
	 * 更新预计时间
	 * 
	 * @param list
	 */
	void updateExpectTime(@Param("scheduleResult") GlueScheduleResultVo scheduleResult);

	/**
	 * 通过ID条件查询终炼母炼日计划列表，查出同一天同一密炼区同一种胶料的所有记录
	 * 
	 * @param idList ID列表
	 */
	List<GlueScheduleResultVo> selectScheduleResultSameGlue(@Param("idList") List<Long> idList);

	/**
	 * 通过ID条件查询终炼母炼日计划列表，查出同一天同一密炼区的所有记录
	 * 
	 * @param idList ID列表
	 */
	List<GlueScheduleResultVo> selectScheduleResultSameArea(@Param("idList") List<Long> idList);

	/**
	 * 查询终炼母炼日完成情况
	 * 
	 * @param scheduleDate
	 * @param mixArea
	 * @return
	 */
	List<GlueFinish> selectGlueFinishList(@Param("scheduleDate") Date scheduleDate, @Param("mixArea") String mixArea);

	/**
	 * 获取排产日的需求计划汇总
	 * 
	 * @param scheduleDate
	 * @param mixArea
	 * @return
	 */
	List<GlueCollectPlan> selectGlueCollectPlanList(@Param("scheduleDate") Date scheduleDate,
			@Param("mixArea") String mixArea);

	/**
	 * 将排指定排产日的排程记录拷贝至初始排程日志表中
	 * 
	 * @param scheduleDate 排产日
	 * @param mixArea      密炼区
	 */
	void copyScheduleResultListToInitLog(@Param("scheduleDate") Date scheduleDate, @Param("mixArea") String mixArea);

	/**
	 * 删除指定排产日的初始排程日志记录
	 * 
	 * @param scheduleDate 排产日
	 * @param mixArea      密炼区
	 */
	void deleteScheduleInitLogList(@Param("scheduleDate") Date scheduleDate, @Param("mixArea") String mixArea);

	/**
	 * 删除补量记录
	 * 
	 * @param scheduleDate 排产日
	 * @param mixArea      密炼区
	 */
	void deleteGlueScheduleSupplement(@Param("scheduleDate") Date scheduleDate, @Param("mixArea") String mixArea);

	/**
	 * 查询预计库存小于0（表示夜班计划需要完成的部分）的分解计划
	 *
	 * @param scheduleDate 排产日
	 * @param mixArea      密炼区
	 * @return 预计库存小于0（表示夜班计划需要完成的部分）的分解计划
	 */
	List<GlueDecomposePlan> selectNegativeStockDecompose(@Param("scheduleDate") Date scheduleDate, @Param("mixArea") String mixArea);
}
