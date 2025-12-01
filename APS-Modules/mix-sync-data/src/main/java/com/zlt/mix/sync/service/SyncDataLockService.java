package com.zlt.mix.sync.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Component;

import com.ruoyi.common.utils.StringUtils;

/**
 * 同步数据锁服务
 * 
 * @author hakimryan
 *
 */
@Component
public class SyncDataLockService {
	/**
	 * ktr文件锁
	 */
	private Map<String, ReentrantLock> KTR_LOCK_MAP = new HashMap<>();
	/**
	 * 全局锁，加锁解锁都要争夺该锁
	 */
	private final static Object GLOBAL_LOCK = new Object();

	/**
	 * 加锁
	 * 
	 * @param ktrName ktr文件名
	 */
	public void addLock(String ktrName) {
		synchronized (GLOBAL_LOCK) {
			ReentrantLock lock = KTR_LOCK_MAP.getOrDefault(ktrName, new ReentrantLock());
			if (lock.isLocked()) {
				throw new RuntimeException(StringUtils.format("同步接口{}运行中，本次不执行！！！", ktrName));
			}
			lock.lock();
			KTR_LOCK_MAP.put(ktrName, lock);
		}
	}

	/**
	 * 释放锁
	 * 
	 * @param ktrName ktr文件名
	 */
	public void releaseLock(String ktrName) {
		synchronized (GLOBAL_LOCK) {
			ReentrantLock lock = KTR_LOCK_MAP.get(ktrName);
			if (lock != null) {
				lock.unlock();
			}
		}
	}
}
