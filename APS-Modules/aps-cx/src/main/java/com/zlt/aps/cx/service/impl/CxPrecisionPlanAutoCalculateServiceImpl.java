package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.redis.service.RedisService;
import com.zlt.aps.cx.service.ICxPrecisionPlanAutoCalculateService;
import com.zlt.aps.cx.service.ICxPrecisionPlanService;
import com.zlt.aps.maindata.mapper.MdmDevMaintenancePlanEntityMapper;
import com.zlt.aps.mp.api.domain.entity.MdmDevMaintenancePlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 成型精度计划自动推算服务实现
 *
 * @author APS Team
 * @since 2026/04/13
 */
@Slf4j
@Service
public class CxPrecisionPlanAutoCalculateServiceImpl implements ICxPrecisionPlanAutoCalculateService {

    private static final String LOCK_KEY_CX_15 = "precision:plan:calculate:cx:15:";
    private static final String LOCK_KEY_CX_60 = "precision:plan:calculate:cx:60:";
    private static final String LOCK_KEY_GENERATE = "precision:plan:generate:cx:";
    private static final long LOCK_EXPIRE_TIME = 300;

    @Autowired
    private ICxPrecisionPlanService cxPrecisionPlanService;

    @Autowired
    private MdmDevMaintenancePlanEntityMapper mdmDevMaintenancePlanMapper;

    @Autowired
    private RedisService redisService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult autoCalculateCxPrecisionPlan15Days(Integer year) {
        String lockKey = LOCK_KEY_CX_15 + year;

        if (!tryLock(lockKey)) {
            return AjaxResult.error("成型精度计划（15天）推算任务正在执行中，请稍后再试");
        }

        try {
            log.info("开始自动推算{}年度成型精度计划（15天周期）", year);
            int count = cxPrecisionPlanService.autoGenerateByCycle(year, 15);
            String msg = "成功推算" + count + "条成型精度计划（15天周期）";
            log.info(msg);
            return AjaxResult.success(msg);
        } catch (Exception e) {
            log.error("自动推算成型精度计划（15天）失败", e);
            throw e;
        } finally {
            releaseLock(lockKey);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult autoCalculateCxPrecisionPlan60Days(Integer year) {
        String lockKey = LOCK_KEY_CX_60 + year;

        if (!tryLock(lockKey)) {
            return AjaxResult.error("成型精度计划（60天）推算任务正在执行中，请稍后再试");
        }

        try {
            log.info("开始自动推算{}年度成型精度计划（60天周期）", year);
            int count = cxPrecisionPlanService.autoGenerateByCycle(year, 60);
            String msg = "成功推算" + count + "条成型精度计划（60天周期）";
            log.info(msg);
            return AjaxResult.success(msg);
        } catch (Exception e) {
            log.error("自动推算成型精度计划（60天）失败", e);
            throw e;
        } finally {
            releaseLock(lockKey);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult generateFromMaintenancePlanByIds(List<Long> maintenancePlanIds, Integer cycleDays) {
        if (CollectionUtils.isEmpty(maintenancePlanIds)) {
            return AjaxResult.success("设备保养计划ID列表为空");
        }

        String lockKey = LOCK_KEY_GENERATE + cycleDays + ":" + System.currentTimeMillis();
        if (!tryLock(lockKey)) {
            return AjaxResult.error("成型精度计划生成任务正在执行中，请稍后再试");
        }

        try {
            log.info("开始根据设备保养计划ID生成并推算成型精度计划（{}天），共{}条", cycleDays, maintenancePlanIds.size());

            String precisionType = cycleDays == 15 ? "成型15天" : "成型60天";
            QueryWrapper<MdmDevMaintenancePlan> wrapper = new QueryWrapper<>();
            wrapper.in("id", maintenancePlanIds);
            wrapper.eq("precision_type", precisionType);
            List<MdmDevMaintenancePlan> maintenancePlans = mdmDevMaintenancePlanMapper.selectList(wrapper);

            if (CollectionUtils.isEmpty(maintenancePlans)) {
                return AjaxResult.success("未找到符合条件的设备保养计划");
            }

            int count = cxPrecisionPlanService.generateFromMaintenancePlan(maintenancePlans, cycleDays);

            try {
                cxPrecisionPlanService.autoGenerateByCycle(LocalDate.now().getYear(), cycleDays);
            } catch (Exception e) {
                log.error("自动推算成型精度计划（{}天）失败", cycleDays, e);
            }

            String msg = String.format("成功生成成型精度计划（%d天）%d条", cycleDays, count);
            log.info(msg);

            return AjaxResult.success(msg);
        } catch (Exception e) {
            log.error("根据设备保养计划生成并推算成型精度计划失败", e);
            throw e;
        } finally {
            releaseLock(lockKey);
        }
    }

    private boolean tryLock(String lockKey) {
        try {
            Boolean hasKey = redisService.hasKey(lockKey);
            if (Boolean.TRUE.equals(hasKey)) {
                return false;
            }
            redisService.setCacheObject(lockKey, "1", LOCK_EXPIRE_TIME, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            log.error("获取分布式锁失败: {}", lockKey, e);
            return false;
        }
    }

    private void releaseLock(String lockKey) {
        try {
            redisService.deleteObject(lockKey);
        } catch (Exception e) {
            log.error("释放分布式锁失败: {}", lockKey, e);
        }
    }
}
