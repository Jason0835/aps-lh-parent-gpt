package com.zlt.aps.cd15.engine.service;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;

import java.util.Date;
import java.util.List;

/**
 * 15度裁断自动排程服务
 *
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-7 10:33:57
 * @Version 1.0
 */
public interface Cd15EngineService {

	/**
	 * 自动排程
     *
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-19 11:26:33
	 * @Param scheduleDate 排产日期
	 * @Return
	 */
	public void autoCd15Schedule(Date scheduleDate);

	/**
	 * 15度裁断插单
     *
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-24 20:40:48
	 * @param scheduleResult 插单记录
	 */
	int insertCd15Order(Cd15ScheduleResult scheduleResult);

	/**
     *
	 * 15度裁断转机台
     *
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-24 21:11:58
	 * @param oldMachineIds  确认前，旧的机台id
	 * @param scheduleResult 确认排产记录
	 */
	void changeCd15Machine(String oldMachineIds, Cd15ScheduleResult scheduleResult);

	/**
	 * 确认机台
     *
	 * @param scheduleResult 选择机台后的排产记录
	 */
	void confirmCd15Machine(Cd15ScheduleResult scheduleResult);

	/**
	 * 将指定日期的15度裁断排产结果做平衡处理
     *
	 * @param scheduleDate 排产日期
	 */
	void handleEquilibrium(Date scheduleDate);

	/**
	 * 批量导入15度裁断排程记录
     *
	 * @param scheduleDate 排程日志
	 * @param scheduleList 排程数据
	 * @return 导入异常日志
	 */
	List<ImportErrorLog> batchSaveCd15Schedule(Date scheduleDate, List<Cd15ScheduleResult> scheduleList);

    void batchUpdateBatchNoAndOrderNo(Date scheduleDate);
}
