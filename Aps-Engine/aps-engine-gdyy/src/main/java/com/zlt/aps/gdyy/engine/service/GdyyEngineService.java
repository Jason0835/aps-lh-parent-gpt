package com.zlt.aps.gdyy.engine.service;

import java.util.Date;
import java.util.List;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.aps.gdyy.api.domain.dto.GdyyScheduleResultDto;

/**
 * 钢带压延自动排程服务
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-16 10:33:57
 * @Version 1.0
 */
public interface GdyyEngineService {

	/**
	 * 自动排程
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-16 11:26:33
	 * @Param	scheduleDate 排产日期
	 * @Return
	 */
	public void autoGdyySchedule(Date scheduleDate);
	
    /**
     * 90度裁断插单
     * @Author hakimryan
     * @Description
     * @Date 2021-7-26 09:31:42
     * @param scheduleResult	插单记录
     */
    int insertGdyyOrder(GdyyScheduleResultDto scheduleResult);

	/**
	 * 批量导入的钢带压延排程记录
	 * 
	 * @param scheduleDate 排程日志
	 * @param scheduleList 排程数据
	 * @return 导入异常日志
	 */
    List<ImportErrorLog> batchSaveGdyySchedule(Date scheduleDate, List<GdyyScheduleResultDto> scheduleList);
}
