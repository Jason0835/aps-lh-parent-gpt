package com.zlt.aps.mps.service.impl;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.RedisLock;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.mps.common.SyncKeyEnum;
import com.zlt.aps.mps.domain.BomInfo;
import com.zlt.aps.mps.domain.TMesBomInfo;
import com.zlt.aps.mps.mapper.TMesBomInfoMapper;
import com.zlt.aps.mps.mapper.TMesPlmConstructionInfoMapper;
import com.zlt.aps.mps.service.MesBaseInfoService;
import com.zlt.aps.mps.service.MesConstructionInfoService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author Gim
 */
@Service
@Slf4j
public class MesBaseInfoServiceImpl implements MesBaseInfoService {

    @Resource
    private TMesBomInfoMapper bomInfoMapper;
    @Resource
    private TMesPlmConstructionInfoMapper plmConstructionInfoMapper;
    @Resource
    private MesConstructionInfoService mesConstructionInfoService;
	@Autowired
	private RedisTemplate redisTemplate;
	/**
	 * 防重复执行间隔时长（半小时）
	 */
	@Value("${syncdata.sync.lockTime:1800000}")
	private Integer publishLockTime;

    @Override
    public AjaxResult mergeBomInfo(String dataVersion) {
    	if (this.checkSyncLocking(SyncKeyEnum.BOM_INFO_SYNC.getDescription(), dataVersion)) {
    		return null; // 定时任务已被锁定
    	}
        // 合并
        bomInfoMapper.mergeSql(dataVersion);
        try {
        	// 开始更新前记录时间点
    		Date currentDate = this.truncSecond(DateUtils.getNowDate());
			// bom更新后，施工表要跟着更新
			mesConstructionInfoService.mergeBomToConstruction(dataVersion);
			// 把本次更新过的的施工记录刷新到投产施工表中
			return mesConstructionInfoService.mergeProductConstructionInfo(currentDate);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			String errorMessage = e.getCause() != null ? e.getCause().toString() : e.getMessage();
			return AjaxResult.error(errorMessage);
		}
    }

    /**
     * PLM参数同步，将中间表的PLM参数合并到业务表中
     * @param dataVersion	同步版本
     */
    @Override
    public AjaxResult mergePlmConstructionInfo(String dataVersion) {
    	if (this.checkSyncLocking(SyncKeyEnum.PLM_CONSTRUCTION_INFO.getDescription(), dataVersion)) {
    		return null; // 定时任务已被锁定
    	}
    	// 更新PLM参数业务表
    	plmConstructionInfoMapper.mergeSql(dataVersion);
        try {
        	// 开始更新前记录时间点
    		Date currentDate = this.truncSecond(DateUtils.getNowDate());
	        // plm参数更新后，施工表要跟着更新
	        mesConstructionInfoService.mergePlmToConstruction(dataVersion);
			// 把本次更新过的的施工记录刷新到投产施工表中
			return mesConstructionInfoService.mergeProductConstructionInfo(currentDate);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			String errorMessage = e.getCause() != null ? e.getCause().toString() : e.getMessage();
			return AjaxResult.error(errorMessage);
		}
    }
    
    /**
     * 将日期的秒数、毫秒数设置为0
     * @param time
     * @return
     */
    private Date truncSecond(Date time) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(time);
		cal.set(Calendar.MILLISECOND, 0);
		cal.set(Calendar.SECOND, 0);
		return cal.getTime();
    }
    
    /**
     * 校验同步任务是否被锁定了
     * @param syncKey
     * @param dataVersion
     * @return
     */
	private boolean checkSyncLocking(String syncKey, String dataVersion) {
		// 将synckey、dataversion拼接到key上，作为redis锁的key
		StringBuffer lockKeyBuffer = new StringBuffer("sync:lock");
		lockKeyBuffer.append(":").append(syncKey).append(":").append(dataVersion);
		RedisLock redisLock = new RedisLock(redisTemplate, lockKeyBuffer.toString(), publishLockTime);
		return !redisLock.lock();
	}
}
