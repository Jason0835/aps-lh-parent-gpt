package com.zlt.mix.common.engine.service;

/**
 * 终炼母炼胶排产日志服务
 * 
 * @author hakimryan
 *
 */
public interface GlueScheduleEngineLogService {
	/**
	 * 添加日志
	 * 
	 * @param content
	 */
	void record(String content);

	/**
	 * 保存日志
	 * 
	 * @param batchNo 批次号
	 * @param title   日志标题
	 */
	void save(String batchNo, String title);
}
