package com.zlt.aps.lh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.redis.service.RedisService;
import com.zlt.aps.lh.service.ILhPrecisionPlanService;
import com.zlt.aps.lh.service.IPrecisionPlanAutoCalculateService;
import com.zlt.aps.maindata.mapper.MdmDevMaintenancePlanEntityMapper;
import com.zlt.aps.mp.api.domain.entity.MdmDevMaintenancePlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 精度计划自动推算服务实现
 *
 * @author APS Team
 * @since 2026/04/13
 */
@Slf4j
@Service
public class PrecisionPlanAutoCalculateServiceImpl implements IPrecisionPlanAutoCalculateService {

    private static final String LOCK_KEY_LH = "precision:plan:calculate:lh:";
    private static final String LOCK_KEY_GENERATE = "precision:plan:generate:";
    private static final long LOCK_EXPIRE_TIME = 300;

    @Autowired
    private ILhPrecisionPlanService lhPrecisionPlanService;

    @Autowired
    private MdmDevMaintenancePlanEntityMapper mdmDevMaintenancePlanMapper;

    @Autowired
    private RedisService redisService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult autoCalculateLhPrecisionPlan(Integer year) {
        String lockKey = LOCK_KEY_LH + year;

        if (!tryLock(lockKey)) {
            return AjaxResult.error("硫化精度计划推算任务正在执行中，请稍后再试");
        }

        try {
            log.info("开始自动推算{}年度硫化精度计划", year);
            int count = lhPrecisionPlanService.autoGenerateYearlyPlans(year);
            String msg = "成功推算" + count + "条硫化精度计划";
            log.info(msg);
            return AjaxResult.success(msg);
        } catch (Exception e) {
            log.error("自动推算硫化精度计划失败", e);
            throw e;
        } finally {
            releaseLock(lockKey);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult generateFromMaintenancePlanByIds(List<Long> maintenancePlanIds, String precisionType) {
        if (CollectionUtils.isEmpty(maintenancePlanIds)) {
            return AjaxResult.success("设备保养计划ID列表为空");
        }

        String lockKey = LOCK_KEY_GENERATE + System.currentTimeMillis();
        if (!tryLock(lockKey)) {
            return AjaxResult.error("精度计划生成任务正在执行中，请稍后再试");
        }

        try {
            log.info("开始根据设备保养计划ID生成并推算硫化精度计划，共{}条", maintenancePlanIds.size());

            QueryWrapper<MdmDevMaintenancePlan> wrapper = new QueryWrapper<>();
            wrapper.in("id", maintenancePlanIds);
            wrapper.eq("precision_type", precisionType);
            List<MdmDevMaintenancePlan> maintenancePlans = mdmDevMaintenancePlanMapper.selectList(wrapper);

            if (CollectionUtils.isEmpty(maintenancePlans)) {
                return AjaxResult.success("未找到符合条件的设备保养计划");
            }

            int count = lhPrecisionPlanService.generateFromMaintenancePlan(maintenancePlans);

            Integer nextYear = LocalDate.now().getYear() + 1;
            try {
                lhPrecisionPlanService.autoGenerateYearlyPlans(nextYear);
            } catch (Exception e) {
                log.error("自动推算硫化精度计划失败", e);
            }

            String msg = String.format("成功生成硫化精度计划%d条", count);
            log.info(msg);

            return AjaxResult.success(msg);
        } catch (Exception e) {
            log.error("根据设备保养计划生成并推算硫化精度计划失败", e);
            throw e;
        } finally {
            releaseLock(lockKey);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult generateAndCalculateFromMaintenancePlan(List<MdmDevMaintenancePlan> maintenancePlans) {
        if (CollectionUtils.isEmpty(maintenancePlans)) {
            return AjaxResult.success("设备保养计划数据为空");
        }

        String lockKey = LOCK_KEY_GENERATE + System.currentTimeMillis();
        if (!tryLock(lockKey)) {
            return AjaxResult.error("精度计划生成任务正在执行中，请稍后再试");
        }

        try {
            log.info("开始根据设备保养计划生成并推算硫化精度计划，共{}条", maintenancePlans.size());

            List<MdmDevMaintenancePlan> lhPlans = new ArrayList<>();
            for (MdmDevMaintenancePlan plan : maintenancePlans) {
                if ("硫化精度".equals(plan.getPrecisionType())) {
                    lhPlans.add(plan);
                }
            }

            int lhCount = 0;
            if (!lhPlans.isEmpty()) {
                lhCount = lhPrecisionPlanService.generateFromMaintenancePlan(lhPlans);

                Integer nextYear = LocalDate.now().getYear() + 1;
                try {
                    lhPrecisionPlanService.autoGenerateYearlyPlans(nextYear);
                } catch (Exception e) {
                    log.error("自动推算硫化精度计划失败", e);
                }
            }

            String msg = String.format("生成硫化精度计划%d条", lhCount);
            log.info(msg);

            return AjaxResult.success(msg);
        } catch (Exception e) {
            log.error("根据设备保养计划生成并推算硫化精度计划失败", e);
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
