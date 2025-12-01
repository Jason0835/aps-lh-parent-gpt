package com.zlt.mix.schedule.engine.service.glueschedule;

import java.util.Date;
import java.util.List;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.schedule.api.domain.entity.GlueScheduleResult;
import com.zlt.mix.schedule.api.domain.entity.GlueSpanReceive;

/**
 * 终炼胶母炼胶日计划服务
 * 
 * @author hakimryan
 *
 */
public interface GlueScheduleEngineService {
	/**
	 * 终炼胶母炼胶日计划自动排程
	 * 
	 * @param scheduleDate 排产日
	 * @param mixArea      密炼区
	 */
	void autoGlueSchedule(Date scheduleDate, String mixArea);

	/**
	 * 插单
	 * 
	 * @param scheduleResult 待插单数据
	 */
	List<GlueScheduleResult> insertOrder(GlueScheduleResult scheduleResult);

	/**
	 * 导入排程
	 * 
	 * @param resultList 待插单数据
	 * @return 导入错误日志
	 */
	List<ImportErrorLog> importSchedule(List<GlueScheduleResult> resultList);

	/**
	 * 转机台
	 * 
	 * @param resultList 待转机台排程记录
	 */
	List<GlueScheduleResult> changeMachine(List<GlueScheduleResult> resultList);

	/**
	 * 根据生产顺序重算预计时间
	 * 
	 * @param result 待修改的排程
	 * @return 返回本次修改到的记录
	 */
	List<GlueScheduleResult> recaculateExpectTime(GlueScheduleResult scheduleResult);

	/**
	 * 下发排程数据给MES
	 * 
     * @param glueScheduleResult 发布参数
	 * @param resultIdList 待下发的排程ID列表
	 */
	AjaxResult publishToMes(GlueScheduleResult glueScheduleResult, List<Long> resultIdList);
	

    /**
     * 更新下发状态
     * 
     * @param resultIdList 待下发的排程ID列表
     */
    AjaxResult updateRelaseStatus(List<Long> resultIdList, String relaseStatus);

	/**
	 * 重算总剩余量
	 * 
	 * @param idList              排程id
	 * @param isExclude           是否排除掉参数中的id，主要用于删除
	 * @param isChangeMasterbatch 是否联级修改母炼胶标识
	 */
	void recaculateTotalSurplus(List<Long> idList, boolean isExclude);

	/**
	 * 删除排程后重算相关信息
	 * 
	 * @param idList              id列表
	 * @param isChangeMasterbatch 是否联级修改母炼胶标识
	 */
	void deleteSchedule(List<Long> idList, Boolean isChangeMasterbatch);

	/**
	 * 跨区接收引擎算法
	 * 
	 * @param receiveList 批量接收的记录
	 */
	void glueSpanReceive(List<GlueSpanReceive> receiveList);
}
