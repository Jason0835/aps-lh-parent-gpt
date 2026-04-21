package com.zlt.sync.service;

import com.zlt.aps.itf.vo.SyncDataLogs;

/**
 *
 * @Description 同步日志服务接口
 * @Author zlt
 * @Date 2022-3-9 10:23:36
 */
public interface SyncDataLogsService {
	/**
	 * 获取数据版本
	 *
	 * @param syncKey 同步标识
	 * @return 数据版本号
	 */
	String getDataVersion(String syncKey);

	/**
	 * 获取同步日志的反馈状态
	 *
	 * @param dataVersion 数据版本
	 * @return
	 */
	SyncDataLogs getSyncDataResult(String dataVersion);

	/**
	 * 获取请求日志的反馈状态
	 *
	 * @param dataVersion 数据版本
	 * @return
	 */
	SyncDataLogs getReqDataResult(String dataVersion);

	/**
	 * 检查待发布排程记录是否已被锁定
	 *
	 * @param lockKey    锁key
	 * @param publishIds 待发布记录ID
	 * @return
	 */
	boolean checkPublishLocking(String lockKey, Long[] publishIds);
}
