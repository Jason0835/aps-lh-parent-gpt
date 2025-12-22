package com.zlt.sync.service;

import com.zlt.sync.domain.AuxReqSyncDataLogs;

/**
 * mq消息处理
 * @author zlt
 *
 */
public interface SyncDataMQService {
	/**
	 * 处理mq消息
	 * @param messageStr
	 * @return
	 */
	AuxReqSyncDataLogs handleMQProcess(String messageStr);
}
