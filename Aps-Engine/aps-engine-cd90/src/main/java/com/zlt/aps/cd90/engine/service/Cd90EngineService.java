package com.zlt.aps.cd90.engine.service;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;

import java.util.Date;
import java.util.List;

/**
 * 90度裁断自动排程服务
 *
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-14 10:33:57
 * @Version 1.0
 */
public interface Cd90EngineService {

	/**
	 * 90度裁断自动排程
     *
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-14 11:26:33
	 * @Param scheduleDate 排产日期
	 * @Return
	 */
	public void autoCd90Schedule(Date scheduleDate);

	/**
	 * 90度裁断插单
     *
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-25 09:31:42
	 * @param scheduleResult 插单记录
	 */
	int insertCd90Order(Cd90ScheduleResult scheduleResult);

	/**
     *
	 * 90度裁断转机台
     *
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-25 11:32:01
	 * @param oldMachineIds  确认前，旧的机台id
	 * @param scheduleResult 确认排产记录
	 */
	void changeCd90Machine(String oldMachineIds, Cd90ScheduleResult scheduleResult);

	/**
	 * 确认机台
     *
	 * @param oldMachineIds  选择台前，旧的机台id
	 * @param scheduleResult 选择机台后的排产记录
	 */
	void confirmCd90Machine(Cd90ScheduleResult scheduleResult);

	/**
	 * 将指定日期的90度裁断排产结果做平衡处理
     *
	 * @param scheduleDate 排产日期
	 */
	void handleEquilibrium(Date scheduleDate);

	/**
	 * 批量导入90度裁断排程记录
     *
	 * @param scheduleDate 排程日志
	 * @param scheduleList 排程数据
	 * @return 导入异常日志
	 */
	List<ImportErrorLog> batchSaveCd90Schedule(Date scheduleDate, List<Cd90ScheduleResult> scheduleList);

    void batchUpdateBatchNoAndOrderNo(Date scheduleDate);
}
