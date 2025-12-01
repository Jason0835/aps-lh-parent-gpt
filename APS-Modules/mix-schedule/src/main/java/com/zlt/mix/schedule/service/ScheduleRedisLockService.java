package com.zlt.mix.schedule.service;

/**
 * 排程锁服务
 * 
 * @author hakimryan
 *
 */
public interface ScheduleRedisLockService {
	/**
	 * 检查锁是否已被上锁
	 * 
	 * @param lockKey      锁类型
	 * @param identifyCode 识别码
	 * @return
	 */
	boolean checkLocking(String lockKey, String identifyCode);
}
