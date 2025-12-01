package com.zlt.mix.schedule.engine.service.glueschedule;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zlt.mix.schedule.engine.util.GlueScheduleStockPool;
import com.zlt.mix.schedule.engine.vo.GlueScheduleResultVo;
import com.zlt.mix.schedule.engine.vo.GlueSpanReceiveVo;
import com.zlt.mix.schedule.engine.vo.MesPmtRecipeVo;

/**
 * 终炼母炼排程插单服务
 * 
 * @author hakimryan
 *
 */
public interface GlueScheduleEngineModifyService {

	/**
	 * 构建插单排程记录
	 * 
	 * @param scheduleResult  待插单排程信息
	 * @param recipe          配方
	 * @param glueStock       库存
	 * @param params          排程设置参数
	 * @param allScheduleList 插单机台的所有排产信息
	 * @return 待新增与更新的排程记录
	 */
	List<GlueScheduleResultVo> insertOrder(GlueScheduleResultVo scheduleResult, MesPmtRecipeVo recipe,
			GlueScheduleStockPool glueStock, Map<String, String> params, List<GlueScheduleResultVo> allScheduleList);

	/**
	 * 转机台服务
	 * 
	 * @param scheduleList         待插单排程信息
	 * @param recipeList           配方列表
	 * @param params               排程设置参数
	 * @param allScheduleList      当天难所有排产信息
	 * @param isUpdateProduceOrder 是否更新生产顺序，true：需要以参数传入的顺序为准
	 * @return
	 */
	List<GlueScheduleResultVo> changeMachine(List<GlueScheduleResultVo> scheduleList, List<MesPmtRecipeVo> recipeList,
			Map<String, String> glueParams, List<GlueScheduleResultVo> allScheduleList, boolean isUpdateProduceOrder);

	/**
	 * 修改排程后重算涉及机台的所有的预计时间
	 * 
	 * @param scheduleResult      修改后的排程
	 * @param oldScheduleResult   修改前的排程
	 * @param machineScheduleList 该机台的其他所有排程记录
	 * @param recipe              对应的配方信息
	 * @param params              排程设置
	 * @return
	 */
	List<GlueScheduleResultVo> recaculateAllExpectTime(GlueScheduleResultVo scheduleResult,
			GlueScheduleResultVo oldScheduleResult, List<GlueScheduleResultVo> machineScheduleList,
			MesPmtRecipeVo recipe, Map<String, String> params);

	/**
	 * 重算整个排程机台的预计时间
	 * 
	 * 
	 * @param scheduleDate 排产日
	 * @param scheduleList 待重算排程列表
	 * @param params       排产参数
	 * @return
	 */
	void recaculateExpectTimeInList(Date scheduleDate, List<GlueScheduleResultVo> scheduleList,
			Map<String, String> params);

	/**
	 * 级联修改子胶料排程
	 * 
	 * @param newSchedule     修改的排程记录
	 * @param glue            前端修改的胶料号
	 * @param allScheduleList 所有排程记录
	 * @param recipeMap       配方信息
	 * @param stockPool       库存信息
	 * @param modifyGlueSet   已修改胶料列表，主要用于记录已级联修改的胶料，防止配方层级有错形成闭环出现死循环
	 * @return
	 */
	List<GlueScheduleResultVo> cascadeUpdateChildGlueSchedule(GlueScheduleResultVo newSchedule, String glue,
			List<GlueScheduleResultVo> allScheduleList, Map<String, List<MesPmtRecipeVo>> recipeMap,
			Map<String, String> params, GlueScheduleStockPool glueStock, Set<String> modifyGlueSet);

	/**
	 * 接收跨区生产
	 * 
	 * @param scheduleDate     排产日
	 * @param mixArea          密炼区
	 * @param batchNo          批次号
	 * @param allScheduleList  已拍计划
	 * @param mesPmtRecipeList 配方列表
	 * @param glueStock        库存信息
	 * @param params           排产参数
	 * @return 本次修改到的内容
	 */
	List<GlueScheduleResultVo> createGlueSpanReceiveSchedule(Date scheduleDate, String mixArea, String batchNo,
			List<GlueSpanReceiveVo> glueSpanReceiveList, List<GlueScheduleResultVo> allScheduleList,
			List<MesPmtRecipeVo> mesPmtRecipeList, GlueScheduleStockPool glueStock, Map<String, String> params);
}
