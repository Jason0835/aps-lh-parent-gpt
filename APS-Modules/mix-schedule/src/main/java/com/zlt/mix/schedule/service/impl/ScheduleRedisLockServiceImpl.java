package com.zlt.mix.schedule.service.impl;

import org.apache.poi.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.zlt.mix.common.core.utils.RedisLock;
import com.zlt.mix.schedule.service.ScheduleRedisLockService;

/**
 * 排程锁服务
 * 
 * @author hakimryan
 *
 */
@Service("scheduleRedisLockService")
public class ScheduleRedisLockServiceImpl implements ScheduleRedisLockService {
	@Autowired
	private RedisTemplate redisTemplate;
	/**
	 * 防重复请求的锁定时间（毫秒），默认1000毫秒
	 */
	@Value("${syncdata.schedule.lockTime:1000}")
	private Integer scheduleLockTime;

	/**
	 * 检查锁是否已被上锁
	 * 
	 * @param lockKey      锁类型
	 * @param identifyCode 识别码
	 * @return
	 */
	@Override
	public boolean checkLocking(String lockKey, String identifyCode) {
		StringBuffer lockKeyBuffer = new StringBuffer(lockKey);
		lockKeyBuffer.append(":").append(identifyCode);
		RedisLock redisLock = new RedisLock(redisTemplate, lockKeyBuffer.toString(), scheduleLockTime);
		return !redisLock.lock();
	}

}
