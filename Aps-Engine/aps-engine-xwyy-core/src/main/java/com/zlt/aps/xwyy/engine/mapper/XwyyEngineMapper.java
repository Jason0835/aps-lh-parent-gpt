package com.zlt.aps.xwyy.engine.mapper;

import com.zlt.aps.common.engine.domain.EngineConstructionInfo;
import com.zlt.aps.xwyy.api.domain.dto.XwyyScheduleResultDto;
import com.zlt.aps.xwyy.api.domain.entity.XwyyAssistSpec;
import com.zlt.aps.xwyy.api.domain.entity.XwyyBigRollOriginalBrand;
import com.zlt.aps.xwyy.api.domain.entity.XwyyBigRollRubberCarRelation;
import com.zlt.aps.xwyy.api.domain.entity.XwyyMachineInfo;
import com.zlt.aps.xwyy.engine.vo.*;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 纤维压延断排产mapper
 *
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-22 11:40:19
 * @Version 1.0
 */
public interface XwyyEngineMapper {

	/**
	 * 根据排产日期从90度裁断排程获取纤维压延的基础排程信息
     *
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-22 11:14:06
	 * @Param scheduleDate 排产日期
	 * @param breadth 幅宽
	 * @Param isProductStage 仅对投产阶段的规格排产
	 * @Return
	 */
	List<XwyyScheduleResultVo> selectXwyyScheduleBaseList(@Param("scheduleDate") Date scheduleDate,
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
	List<XwyyDayUsedVo> listXwyyDayUsed(@Param("scheduleDate") Date scheduleDate, @Param("breadth") Double breadth,
			@Param("bigRollCodeList") List<String> bigRollCodeList, @Param("isProductStage") boolean isProductStage);

	/**
	 * 抓取系统参数
     *
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-22 11:21:48
	 * @return
	 */
	List<XwyyParamsVo> listXwyyParams();

	/**
	 * 从BOM信息中抓取原线代码
     *
	 * @return
	 */
	List<String> getOriginalLineCode();

	/**
	 * 查询当前排程的批次号
     *
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-29 09:01:59
	 * @param scheduleDate 排产日期
	 * @return
	 */
	String getCurrentBatchNo(@Param("scheduleDate") Date scheduleDate);

	/**
	 * 新增自动排产记录
     *
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-22 11:01:54
	 * @Param recordVo 自动排产记录
	 * @Return
	 */
	void insertScheduleRecord(XwyyScheduleRecordVo recordVo);

	/**
	 * 批量新增排程结果数据
     *
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-22 11:11:56
	 * @Param
	 * @Return
	 */
	void insertScheduleResultList(@Param("scheduleResultList") List<XwyyScheduleResultVo> scheduleResultList);

	/**
	 * 把排程数据同步到log表，用于备份历史信息
     *
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-22 15:22:04
	 * @param scheduleDate
	 */
	void insertXwyyScheduleLog(@Param("scheduleDate") Date scheduleDate);

	/**
	 * 删除指定日期的排程数据
     *
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-22 15:21:52
	 * @param scheduleDate
	 */
	void deleteXwyyScheduleResult(@Param("scheduleDate") Date scheduleDate);

	/**
	 * 查询在施工信息中 没有对应记录的90度裁断代码
     *
	 * @param scheduleDate 排程日期
	 * @return
	 */
	List<String> listLossConstructionForCd90(@Param("scheduleDate") Date scheduleDate);

	/**
	 * 批量合并排程结果表（根据唯一字段，做更新或新增）
     *
	 * @param scheduleResultList
	 */
	int mergeXwyyScheduleResult(@Param("scheduleResultList") List<XwyyScheduleResultDto> scheduleResultList);

	/**
	 * 获取插单需要的信息
     *
	 * @param scheduleResultList 钢带压延排程信息
	 * @return
	 */
	List<XwyyScheduleResultVo> listInsertOrderBaseInfo(
			@Param("scheduleResultList") List<XwyyScheduleResultDto> scheduleResultList,
			@Param("scheduleDate") Date scheduleDate, @Param("breadth") Double breadth);

	/**
	 * 逻辑删除指定日期的排程主表
     *
	 * @param scheduleDate
	 */
	int logicDeleteXwyyScheduleRecord(XwyyScheduleRecordVo recordVo);

	/**
	 * 查询纤维压延外协规格清单
     *
	 * @return
	 */
	List<XwyyAssistSpec> selectXwyyAssistSpecList();

	/**
	 * 批量新增外协排程结果数据
     *
	 * @Author hakimryan
	 * @Description
	 * @Date 2022-2-14 11:11:56
	 * @Param
	 * @Return
	 */
	void insertScheduleAssistList(@Param("scheduleAssistList") List<XwyyScheduleResultVo> scheduleResultList);

	/**
	 * 删除指定日期的外协排程结果明细
     *
	 * @Author hakimryan
	 * @Description
	 * @Date 2022-2-14 15:21:52
	 * @param scheduleDate
	 */
	void deleteXwyyScheduleAssist(@Param("scheduleDate") Date scheduleDate);

	/**
	 * 查询原线代码规格设置
	 */
	List<XwyyOriginalLineSpec> selectOriginalLineSpec();

	/**
	 * 查询纤维压延外厂需求
     *
	 * @param scheduleDate 排产日
	 * @return
	 */
	List<XwyyAssistRequirement> selectAssistRequirement(@Param("scheduleDate") Date scheduleDate);

	/**
	 * 查询符合条件的钢带压延记录
     *
	 * @param scheduleDate
	 * @return
	 */
	List<XwyyScheduleResultVo> selectXwyyScheduleList(@Param("scheduleDate") Date scheduleDate);

	/**
	 * 查询大卷与胶料车数的关系配置
     *
	 * @return
	 */
	List<XwyyBigRollRubberCarRelation> selectXwyyBigRollRubCarRelation();

	/**
	 * 查询大卷与品牌配置
     *
	 * @return
	 */
	List<XwyyBigRollOriginalBrand> selectXwyyBigRollOriginalBrand();

	int createTempTable();

	int dropTempTable();

	int insertTempTable(@Param("scheduleResultList") List<XwyyScheduleResultVo> scheduleResultList);

	/**
	 * 更新原线品牌个数
	 * @param updateScheduleList
	 */
	void updateScheduleResultOriginalBrand(@Param("scheduleDate") String scheduleDate, @Param("updateScheduleList") List<XwyyScheduleResultVo> updateScheduleList);

	/**
	 *
	 * @param machineIdList
	 * @return
	 */
    List<XwyyMachineInfo> listMachineShift(@Param("machineIdList") List<String> machineIdList);

    void batchUpdateBatchNoAndOrderNo(@Param("list") List<XwyyScheduleResultVo> scheduleResultVoList);

}
