package com.zlt.mix.schedule.engine.service.glueschedule;

import com.zlt.mix.schedule.api.domain.entity.GlueCollectPlan;
import com.zlt.mix.schedule.engine.util.CombinedMapKey;
import com.zlt.mix.schedule.engine.util.GlueScheduleStockPool;
import com.zlt.mix.schedule.engine.vo.GlueFactoryRequireVo;
import com.zlt.mix.schedule.engine.vo.GlueScheduleResultVo;
import com.zlt.mix.schedule.engine.vo.MesPmtRecipeVo;
import com.zlt.mix.setting.api.domain.entity.MixingProductionModel;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 终炼母炼排程基础服务
 * 
 * @author hakimryan
 *
 */
public interface GlueScheduleEngineBaseService {

	/**
	 * 生成排程批次号
	 * 
	 * @param scheduleDate 排产日期
	 * @param mixArea      密炼区
	 * @return
	 */
	String createBatchNo(Date scheduleDate, String mixArea);

	/**
	 * 生成工单号
	 * 
	 * @param batchNo 批次号
	 * @return
	 */
	String createOrderNo(String batchNo);

	/**
	 * 根据胶料拆分明细、配方，生成排程结果列表
	 *
	 * @param scheduleDate              排产日期
	 * @param glueStock                 库存
	 * @param params                    排程参数设置
	 * @param mixArea                   密炼区
	 * @param mixingTimeMap             胶料间隔时间
	 * @param latestScheduleList        昨日早班的最后一个排程计划
	 * @param mixingPriorityProductMap  炼胶优先配置
	 * @param mixingProductionModelList 生产模式列表
	 * @param glueRecipeMap             胶料配方映射的胶料名称Map
	 * @param reserveGlueRecipeMap      胶料配方映射的反转白班计划量的Map
	 * @param glueRecipeOnlyGlueMap     胶料配方映射的胶料映射Map
	 * @return
	 */
	List<GlueScheduleResultVo> createBaseScheduleResultList(Date scheduleDate, List<MesPmtRecipeVo> mesPmtRecipeList,
															GlueScheduleStockPool glueStock, Map<String, String> params, String mixArea, Map<String, Long> mixingTimeMap,
															List<GlueScheduleResultVo> latestScheduleList,
															Map<String, String> mixingPriorityProductMap,
															List<MixingProductionModel> mixingProductionModelList,
															Map<String, String> glueRecipeMap,
															Map<String, String> reserveGlueRecipeMap,
															Map<String, String> glueRecipeOnlyGlueMap);

	/**
	 * 初始化胶料排程记录的基本字段
	 * 
	 * @param scheduleResult 胶料排程记录
	 * @return
	 */
	void initBaseScheduleProperties(GlueScheduleResultVo scheduleResult);
	
	/**
	 * 将配方的排产相关信息拷贝至排程记录中
	 * 
	 * @param scheduleResult 胶料排程记录
	 * @param recipe         配方
	 * @return
	 */
	void copyRecipeProperties(GlueScheduleResultVo scheduleResult, MesPmtRecipeVo recipe);

	/**
	 * 将分厂未提报，但是不够安全库存的胶料生成排程
	 * 
	 * @param scheduleDate 排产日期
	 * @param glueStock    库存信息
	 * @param mixArea      密炼区
	 * @param batchNo      批次号
	 * @param scheduleList 分厂提报的胶料
	 */
	List<GlueScheduleResultVo> createNoRequireSchedule(Date scheduleDate, GlueScheduleStockPool glueStock,
			String mixArea, String batchNo, List<GlueScheduleResultVo> scheduleList);

	/**
	 * 构建分厂需求量列表
	 *
	 * @param scheduleDate          排产日
	 * @param mixArea               密炼区
	 * @param resultList            排产列表
	 * @param glueStock             库存列表
	 * @param glueRecipeOnlyGlueMap 胶料配方映射的胶料映射Map
	 * @return
	 */
	Map<String, GlueFactoryRequireVo> buildGlueFactoryRequire(Date scheduleDate, String mixArea,
			List<GlueScheduleResultVo> resultList, GlueScheduleStockPool glueStock, Map<String, String> glueRecipeOnlyGlueMap);
	
	/**
	 * 将因机台产能不足无法排产的计划移除掉
	 * 
	 * @param glueResultList
	 * @param totalRequireQty
	 * @param totalSurplusQty
	 */
	void removeNoSchedule(List<GlueScheduleResultVo> baseScheduleResult);
	
	/**
	 * 构建排程列表排序器
	 * @return
	 */
	Comparator<GlueScheduleResultVo> createScheduleSorter();


	/**
	 * 计算19点的预计库存
	 *
	 * @param mixArea                密炼区
	 * @param scheduleDate           排产日期
	 * @param params                 排产参数
	 * @param glueRecipeMap          胶料配方映射的胶料名称Map
	 * @param reserveGlueRecipeMap   胶料配方映射的反转白班计划量的Map
	 * @param deductYesterdayRequire 扣减昨日需求
	 * @return
	 */
	void caculate16pmEstimateStock(GlueScheduleStockPool glueStock, String mixArea, Date scheduleDate,
								   List<MesPmtRecipeVo> mesPmtRecipeList, Map<String, String> params,
								   Map<String, String> glueRecipeMap,
								   Map<String, String> reserveGlueRecipeMap,
								   boolean deductYesterdayRequire);

	/**
	 * 构建排程对应的塑炼胶优先级映射，记录塑炼排产后优先排产的记录
	 *
	 * @param baseScheduleResult 排程结果
	 * @param needSlScheduleMap  需要塑炼排产记录
	 * @return 塑炼胶优先配方
	 */
	Map<String, List<GlueScheduleResultVo>> buildSlPriorityMap(List<GlueScheduleResultVo> baseScheduleResult, Map<String, List<GlueScheduleResultVo>> needSlScheduleMap);

	/**
	 * 计算优先级时，昨日日用量先满足，再优先满足今日的日用量
	 *
	 * @param collectPlanMap 汇总胶料需求计划
	 * @param lastDayPlanMap 昨日胶料需求计划
	 */
	void computeDailyDose(Map<String, GlueCollectPlan> collectPlanMap, Map<String, GlueCollectPlan> lastDayPlanMap);

	/**
	 * 获取当日和昨日的的分厂需求胶料汇总量
	 *
	 * @param scheduleDate 排产
	 * @param mixArea      密炼区
	 * @return 分厂需求胶料汇总量
	 */
	Map<String, GlueCollectPlan> getGlueCollectPlanMap(Date scheduleDate, String mixArea);

	/**
	 *
	 * 根据指定条件选择配方<br/>
	 * 1、zz类型的配方<br/>
	 * 2、最后一段母炼胶的所有配方<br/>
	 * 3、其他配方则选择原料库存充足的配方
	 *
	 * @param glueCode        胶料
	 * @param machineCode     机台
	 * @param mesPmtRecipeMap 配方集合
	 * @param glueStock       胶料库存
	 * @param isDecompose     是否分解胶料产生的
	 * @param params          排程参数设置
	 * @return
	 */
	List<MesPmtRecipeVo> chooseRecipe(String glueCode, String machineCode,
									  Map<CombinedMapKey, List<MesPmtRecipeVo>> mesPmtRecipeMap, GlueScheduleStockPool glueStock,
									  boolean isDecompose);
}
