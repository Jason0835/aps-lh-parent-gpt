package com.zlt.sync.service.impl;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.RedisLock;
import com.zlt.aps.itf.vo.SyncDataLogs;
import com.zlt.sync.mapper.SyncDataLogsMapper;
import com.zlt.sync.handle.SyncDataHandle;
import com.zlt.sync.service.SyncDataLogsService;
import com.zlt.sync.utils.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 *
 * @Description 同步日志服务接口
 * @Author zlt
 * @Date 2022-3-9 10:23:36
 */
@Service("syncDataLogsService")
public class SyncDataLogsServiceImpl implements SyncDataLogsService {

	@Autowired
	private SyncDataLogsMapper syncDataLogsMapper;

	@Autowired
	private SyncDataHandle syncDataHandle;

	@Autowired
	private RedisTemplate redisTemplate;
	/**
	 * 反馈超时时间（秒）
	 */
	@Value("${syncdata.feedback.timeOut:60}")
	private Integer feedbackTimeOut;

	/**
	 * 防重复发布锁定时间（毫秒）
	 */
	@Value("${syncdata.publish.lockTime:1000}")
	private Integer publishLockTime;

    /**
     * 检查开关
     */
    @Value("${syncdata.publish.checkSyncResult:0}")
    private String checkSyncResult;

	/**
	 * 获取数据版本
	 *
	 * @param syncKey 同步标识
	 * @return 数据版本号
	 */
	@Override
	public String getDataVersion(String syncKey) {
		return syncDataHandle.getDataVersion(syncKey);
	}

	/**
	 * 获取同步日志的反馈状态
	 *
	 * @param dataVersion 数据版本
	 * @return
	 */
	@Override
	public SyncDataLogs getSyncDataResult(String dataVersion) {
        if ("0".equals(checkSyncResult)) { // 是否检查接口返回状态，不检查则直接返回
            SyncDataLogs logs = new SyncDataLogs();
            logs.setStatus(ApsConstant.IS_RELEASE);
            return logs;
        }
		// 扫描截止时间：30秒后
		Date endTime = DateUtil.secondLater(feedbackTimeOut);
		while (true) {
			SyncDataLogs logs = syncDataLogsMapper.getSyncDataLogs(dataVersion);
			SyncDataLogs resultLog = this.checkLogStatus(dataVersion, logs, endTime);
			if (resultLog != null) {
				return resultLog;
			}
		}
	}

	/**
	 * 检查日志状态
	 * @param dataVersion
	 * @param logs
	 * @param endTime
	 * @return
	 */
	private SyncDataLogs checkLogStatus(String dataVersion, SyncDataLogs logs, Date endTime) {
		if (logs != null) {
			if (ApsConstant.IS_RELEASE.equals(logs.getStatus())
					|| ApsConstant.FAILURE_RELEASE.equals(logs.getStatus())) {
				// 异常情况，需要处理异常信息
				if (ApsConstant.FAILURE_RELEASE.equals(logs.getStatus())) {
					String msg = I18nUtil.getMessage("ui.common.column.schuedule.publish.error") + logs.getMsg();
					logs.setMsg(msg);
				}
				// 成功或者失败，状态确定，因此返回结果
				return logs;
			}
		}
		Date currentTime = DateUtil.now();
		if (currentTime.compareTo(endTime) > 0) {
			// 超时，直接返回超时状态
			logs = new SyncDataLogs();
			logs.setDataVersion(dataVersion);
			logs.setMsg(I18nUtil.getMessage("ui.common.column.schuedule.publish.timeOut"));
			logs.setStatus(ApsConstant.TIMEOUT_FAILURE);
			return logs;
		}
		// 等待3秒后重新扫描
		try {
			Thread.sleep(3000L);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		return null;
	}


	/**
	 * 获取请求日志的反馈状态
	 *
	 * @param dataVersion 数据版本
	 * @return
	 */
	@Override
	public SyncDataLogs getReqDataResult(String dataVersion) {
		// 扫描截止时间：30秒后
		Date endTime = DateUtil.secondLater(feedbackTimeOut);
		while (true) {
			SyncDataLogs logs = syncDataLogsMapper.getReqDataLogs(dataVersion);
			SyncDataLogs resultLog = this.checkLogStatus(dataVersion, logs, endTime);
			if (resultLog != null) {
				return resultLog;
			}
		}
	}

	/**
	 * 检查待发布排程记录是否已被锁定
	 *
	 * @param lockKey    锁key
	 * @param publishIds 待发布记录ID
	 * @return
	 */
	@Override
	public boolean checkPublishLocking(String lockKey, Long[] publishIds) {
		if (publishIds == null || publishIds.length == 0) {
			// 如果传入的记录ID为空，则相当于没锁定，由调用服务自行处理
			return false;
		}
		// 将ID拼接到key上，作为redis锁的key
		StringBuffer lockKeyBuffer = new StringBuffer(lockKey);
		lockKeyBuffer.append(":").append(StringUtils.join(publishIds));
		RedisLock redisLock = new RedisLock(redisTemplate, lockKeyBuffer.toString(), publishLockTime);
		return !redisLock.lock();
	}
}
