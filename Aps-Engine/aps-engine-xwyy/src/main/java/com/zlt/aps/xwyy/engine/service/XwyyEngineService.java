package com.zlt.aps.xwyy.engine.service;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.aps.xwyy.api.domain.dto.XwyyScheduleResultDto;

import java.util.Date;
import java.util.List;

/**
 * 纤维压延自动排程服务
 *
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-22 10:33:57
 * @Version 1.0
 */
public interface XwyyEngineService {

	/**
	 * 纤维压延自动排程
     *
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-22 11:26:33
	 * @Param scheduleDate 排产日期
	 * @Return
	 */
	void autoXwyySchedule(Date scheduleDate);

	/**
	 * 纤维压延插单
     *
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-29 09:31:42
	 * @param scheduleResult 插单记录
	 */
	int insertXwyyOrder(XwyyScheduleResultDto scheduleResult);

	/**
	 * 批量导入纤维压延排程记录
     *
	 * @param scheduleDate 排程日志
	 * @param scheduleList 排程数据
	 * @return 导入异常日志
	 */
	List<ImportErrorLog> batchSaveXwyySchedule(Date scheduleDate, List<XwyyScheduleResultDto> scheduleList);

	/**
     *
	 * 纤维压延转机台
     *
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-29 11:32:01
	 * @param oldMachineIds  转机台前，旧的机台id
	 * @param scheduleResult 转机台排产记录
	 */
	void changeXwyyMachine(String oldMachineIds, XwyyScheduleResultDto scheduleResult);

	/**
	 * 确认机台
     *
	 * @param oldMachineIds  选择台前，旧的机台id
	 * @param scheduleResult 选择机台后的排产记录
	 */
	void confirmXwyyMachine(XwyyScheduleResultDto scheduleResult);

    void batchUpdateBatchNoAndOrderNo(Date scheduleDate);
}
