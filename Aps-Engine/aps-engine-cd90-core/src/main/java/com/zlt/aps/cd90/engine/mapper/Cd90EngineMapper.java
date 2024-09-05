package com.zlt.aps.cd90.engine.mapper;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.zlt.aps.cd90.api.domain.entity.Cd90AssistSpec;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.engine.vo.Cd90ParamsVo;
import com.zlt.aps.cd90.engine.vo.Cd90ScheduleRecordVo;
import com.zlt.aps.cd90.engine.vo.Cd90ScheduleResultVo;
import com.zlt.aps.common.engine.domain.EngineProductConstructionInfo;

/**
 * 90度裁断排产mapper
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-14 11:40:19
 * @Version 1.0
 */
public interface Cd90EngineMapper {

	/**
	 * 根据排产日期从成型排程获取90度裁断的基础排程信息
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-14 11:14:06
	 * @Param scheduleDate 排产日期
	 * @Param isProductStage 仅对投产阶段的规格排产
	 * @Return
	 */
	List<Cd90ScheduleResultVo> selectCd90ScheduleBaseList(@Param("scheduleDate") Date scheduleDate,
			@Param("isProductStage") boolean isProductStage);

	/**
	 * 根据排产日期从成型排程关联施工信息，用于施工数据校验
	 * 
	 * @param scheduleDate
	 * @return
	 */
	List<EngineProductConstructionInfo> listConstructionInfo(@Param("scheduleDate") Date scheduleDate);

	/**
	 * 抓取系统参数
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-27 11:21:48
	 * @return
	 */
	List<Cd90ParamsVo> listCd90Params();

	/**
	 * 查询当前排程的批次号
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-27 10:35:34
	 * @param scheduleDate 排产日期
	 * @return
	 */
	String getCurrentBatchNo(@Param("scheduleDate") Date scheduleDate);

	/**
	 * 获取插单需要的信息
	 * 
	 * @param scheduleResultList 90度裁断插单信息
	 * @return
	 */
	List<Cd90ScheduleResultVo> listInsertOrderBaseInfo(
			@Param("scheduleResultList") List<Cd90ScheduleResult> scheduleResultList,
			@Param("scheduleDate") Date scheduleDate);

	/**
	 * 批量合并排程结果表（根据唯一字段，做更新或新增）
	 * 
	 * @param scheduleResultList
	 */
	int mergeCd90ScheduleResult(@Param("scheduleResultList") List<Cd90ScheduleResult> scheduleResultList);

	/**
	 * 新增自动排产记录
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-14 11:01:54
	 * @Param recordVo 自动排产记录
	 * @Return
	 */
	void insertScheduleRecord(Cd90ScheduleRecordVo recordVo);

	/**
	 * 批量新增排程结果数据
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-14 11:11:56
	 * @Param
	 * @Return
	 */
	void insertScheduleResultList(@Param("scheduleResultList") List<Cd90ScheduleResultVo> scheduleResultList);

	/**
	 * 把排程数据同步到log表，用于备份历史信息
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-14 15:22:04
	 * @param scheduleDate
	 */
	void insertCd90ScheduleLog(@Param("scheduleDate") Date scheduleDate);

	/**
	 * 删除指定日期的排程数据
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-14 15:21:52
	 * @param scheduleDate
	 */
	void deleteCd90ScheduleResult(@Param("scheduleDate") Date scheduleDate);

	/**
	 * 逻辑删除指定日期的排程主表
	 * 
	 * @param scheduleDate
	 */
	int logicDeleteCd90ScheduleRecord(Cd90ScheduleRecordVo recordVo);

	/**
	 * 获取指定排产日的90度裁断的排程信息
	 * 
	 * @param scheduleDate 排产日期
	 * @return
	 */
	List<Cd90ScheduleResultVo> selectCd90ScheduleList(@Param("scheduleDate") Date scheduleDate);

	/**
	 * 批量更新90度裁断排程量
	 * 
	 * @param scheduleResultList
	 */
	int updateCd90ScheduleResultPlanQty(@Param("scheduleResultList") List<Cd90ScheduleResultVo> scheduleResultList);

	/**
	 * 查询90度裁断外协规格清单
	 * 
	 * @return
	 */
	List<Cd90AssistSpec> selectCd90AssistSpecList();

	/**
	 * 批量新增外协排程结果数据
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-12 11:11:56
	 * @Param
	 * @Return
	 */
	void insertScheduleAssistList(@Param("scheduleAssistList") List<Cd90ScheduleResultVo> scheduleResultList);

	/**
	 * 删除指定日期的外协排程结果明细
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-12 15:21:52
	 * @param scheduleDate
	 */
	void deleteCd90ScheduleAssist(@Param("scheduleDate") Date scheduleDate);
}
