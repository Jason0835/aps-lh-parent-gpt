package com.zlt.aps.cd15.engine.mapper;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.zlt.aps.cd15.api.domain.entity.Cd15AssistSpec;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.engine.vo.Cd15ParamsVo;
import com.zlt.aps.cd15.engine.vo.Cd15ScheduleRecordVo;
import com.zlt.aps.cd15.engine.vo.Cd15ScheduleResultVo;
import com.zlt.aps.common.engine.domain.EngineProductConstructionInfo;

/**
 * 15度裁断排产mapper
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-19 11:40:19
 * @Version 1.0
 */
public interface Cd15EngineMapper {

	/**
	 * 根据排产日期从成型排程获取15度裁断的基础排程信息
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-9 11:14:06
	 * @Param scheduleDate 排产日期
	 * @Param isProductStage 仅对投产阶段的规格排产
	 * @Return
	 */
	List<Cd15ScheduleResultVo> selectCd15ScheduleBaseList(@Param("scheduleDate") Date scheduleDate,
			@Param("isProductStage") boolean isProductStage);

	/**
	 * 根据排产日期从成型排程关联施工信息，用于施工数据校验
	 * 
	 * @param scheduleDate 排产日期
	 * @return
	 */
	List<EngineProductConstructionInfo> listConstructionInfo(@Param("scheduleDate") Date scheduleDate);

	/**
	 * 新增自动排产记录
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-12 11:01:54
	 * @Param recordVo 自动排产记录
	 * @Return
	 */
	void insertScheduleRecord(Cd15ScheduleRecordVo recordVo);

	/**
	 * 批量新增排程结果数据
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-12 11:11:56
	 * @Param
	 * @Return
	 */
	void insertScheduleResultList(@Param("scheduleResultList") List<Cd15ScheduleResultVo> scheduleResultList);

	/**
	 * 批量合并排程结果表（根据唯一字段，做更新或新增）
	 * 
	 * @param scheduleResultList
	 */
	int mergeCd15ScheduleResult(@Param("scheduleResultList") List<Cd15ScheduleResult> scheduleResultList);

	/**
	 * 把排程数据同步到log表，用于备份历史信息
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-12 15:22:04
	 * @param scheduleDate
	 */
	void insertCd15ScheduleLog(@Param("scheduleDate") Date scheduleDate);

	/**
	 * 删除指定日期的排程结果明细
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-12 15:21:52
	 * @param scheduleDate
	 */
	void deleteCd15ScheduleResult(@Param("scheduleDate") Date scheduleDate);

	/**
	 * 逻辑删除指定日期的排程主表
	 * 
	 * @param scheduleDate
	 */
	int logicDeleteCd15ScheduleRecord(Cd15ScheduleRecordVo recordVo);

	/**
	 * 抓取系统参数
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-26 11:21:48
	 * @return
	 */
	List<Cd15ParamsVo> listCd15Params();

	/**
	 * 查询当前排程的批次号
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-26 10:35:34
	 * @param scheduleDate 排产日期
	 * @return
	 */
	String getCurrentBatchNo(@Param("scheduleDate") Date scheduleDate);

	/**
	 * 获取插单需要的信息
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-26 10:31:50
	 * @param scheduleResult 15度裁断排产记录
	 * @return
	 */
	List<Cd15ScheduleResultVo> listInsertOrderBaseInfo(
			@Param("scheduleResultList") List<Cd15ScheduleResult> scheduleResultList);

	/**
	 * 获取指定排产日的15度裁断的排程信息
	 * 
	 * @param scheduleDate 排产日期
	 * @return
	 */
	List<Cd15ScheduleResultVo> selectCd15ScheduleList(@Param("scheduleDate") Date scheduleDate);

	/**
	 * 批量更新15度裁断排程量
	 * 
	 * @param scheduleResultList
	 */
	int updateCd15ScheduleResultPlanQty(@Param("scheduleResultList") List<Cd15ScheduleResultVo> scheduleResultList);

	/**
	 * 查询15度裁断外协规格清单
	 * 
	 * @return
	 */
	List<Cd15AssistSpec> selectCd15AssistSpecList();

	/**
	 * 批量新增外协排程结果数据
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-12 11:11:56
	 * @Param
	 * @Return
	 */
	void insertScheduleAssistList(@Param("scheduleAssistList") List<Cd15ScheduleResultVo> scheduleResultList);

	/**
	 * 删除指定日期的外协排程结果明细
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-12 15:21:52
	 * @param scheduleDate
	 */
	void deleteCd15ScheduleAssist(@Param("scheduleDate") Date scheduleDate);
}
