package com.zlt.aps.gdyy.engine.mapper;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.zlt.aps.common.engine.domain.EngineConstructionInfo;
import com.zlt.aps.gdyy.api.domain.dto.GdyyScheduleResultDto;
import com.zlt.aps.gdyy.engine.vo.GdyyDayUsedVo;
import com.zlt.aps.gdyy.engine.vo.GdyyNoteVo;
import com.zlt.aps.gdyy.engine.vo.GdyyParamsVo;
import com.zlt.aps.gdyy.engine.vo.GdyyScheduleRecordVo;
import com.zlt.aps.gdyy.engine.vo.GdyyScheduleResultVo;

/**
 * 钢带压延排产mapper
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-16 11:40:19
 * @Version 1.0
 */
public interface GdyyEngineMapper {

	/**
	 * 根据排产日期从成型排程获取90度裁断的基础排程信息
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-16 11:14:06
	 * @Param scheduleDate 排产日期
	 * @param breadth 幅宽
	 * @Param isProductStage 仅对投产阶段的规格排产
	 * @Return
	 */
	List<GdyyScheduleResultVo> selectGdyyScheduleBaseList(@Param("scheduleDate") Date scheduleDate,
			@Param("breadth") Double breadth, @Param("isProductStage") boolean isProductStage);

	/**
	 * 根据排产日期从成型排程关联施工信息，用于施工数据校验
	 * 
	 * @param scheduleDate
	 * @Param isProductStage 仅对投产阶段的规格排产
	 * @return
	 */
	List<EngineConstructionInfo> listConstructionInfo(@Param("scheduleDate") Date scheduleDate,
			@Param("isProductStage") boolean isProductStage);

	/**
	 * 计算纤维压延日用参考量
	 * 
	 * @param scheduleDate    排产日
	 * @param breadth         幅宽
	 * @param bigRollCodeList 大卷编号列表
	 * @Param isProductStage 仅对投产阶段的规格排产
	 * @return
	 */
	List<GdyyDayUsedVo> listGdyyDayUsed(@Param("scheduleDate") Date scheduleDate, @Param("breadth") Double breadth,
			@Param("bigRollCodeList") List<String> bigRollCodeList, @Param("isProductStage") boolean isProductStage);

	/**
	 * 抓取系统参数
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-16 11:21:48
	 * @return
	 */
	List<GdyyParamsVo> listGdyyParams();

	/**
	 * 查询当前排程的批次号
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-28 10:35:34
	 * @param scheduleDate 排产日期
	 * @return
	 */
	String getCurrentBatchNo(@Param("scheduleDate") Date scheduleDate);

	/**
	 * 获取钢带压延注意事项
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-16 16:05:37
	 * @param scheduleDate
	 * @return
	 */
	List<GdyyNoteVo> listGdyyNote(@Param("scheduleDate") Date scheduleDate);

	/**
	 * 新增自动排产记录
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-16 11:01:54
	 * @Param recordVo 自动排产记录
	 * @Return
	 */
	void insertScheduleRecord(GdyyScheduleRecordVo recordVo);

	/**
	 * 批量新增排程结果数据
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-25 11:11:56
	 * @Param
	 * @Return
	 */
	void insertScheduleResultList(@Param("scheduleResultList") List<GdyyScheduleResultVo> scheduleResultList);

	/**
	 * 把排程数据同步到log表，用于备份历史信息
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-16 15:22:04
	 * @param scheduleDate
	 */
	void insertGdyyScheduleLog(@Param("scheduleDate") Date scheduleDate);

	/**
	 * 删除指定日期的排程数据
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-16 15:21:52
	 * @param scheduleDate
	 */
	void deleteGdyyScheduleResult(@Param("scheduleDate") Date scheduleDate);

	/**
	 * 查询在施工信息中 没有对应记录的15度裁断代码
	 * 
	 * @param scheduleDate 排程日期
	 * @return
	 */
	List<String> listLossConstructionForCd15(@Param("scheduleDate") Date scheduleDate);

	/**
	 * 批量合并排程结果表（根据唯一字段，做更新或新增）
	 * 
	 * @param scheduleResultList
	 */
	int mergeGdyyScheduleResult(@Param("scheduleResultList") List<GdyyScheduleResultDto> scheduleResultList);

	/**
	 * 获取插单需要的信息
	 * 
	 * @param scheduleResultList 钢带压延排程信息
	 * @param scheduleDate       排产日
	 * @return
	 */
	List<GdyyScheduleResultVo> listInsertOrderBaseInfo(
			@Param("scheduleResultList") List<GdyyScheduleResultDto> scheduleResultList,
			@Param("scheduleDate") Date scheduleDate);

	/**
	 * 逻辑删除指定日期的排程主表
	 * 
	 * @param scheduleDate
	 */
	int logicDeleteGdyyScheduleRecord(GdyyScheduleRecordVo recordVo);

	/**
	 * 查询符合条件的钢带压延记录
	 * 
	 * @param scheduleDate
	 * @return
	 */
	List<GdyyScheduleResultVo> selectGdyyScheduleList(@Param("scheduleDate") Date scheduleDate);
}
