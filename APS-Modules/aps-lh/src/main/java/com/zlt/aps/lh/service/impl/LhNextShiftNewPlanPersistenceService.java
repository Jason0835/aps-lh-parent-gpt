package com.zlt.aps.lh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.lh.api.domain.entity.LhNextShiftNewPlan;
import com.zlt.aps.lh.mapper.LhNextShiftNewPlanMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Objects;

/**
 * 硫化班次9新计划独立持久化服务。
 *
 * <p>按工厂、排程日期和批次号原子刷新目标范围。该事务在原8班结果提交后独立开启，
 * 班次9写入失败只回滚本表，不影响已经完成的结果、未排、换模和过程日志。</p>
 *
 * @author APS
 */
@Slf4j
@Service
public class LhNextShiftNewPlanPersistenceService {

    /** 系统自动排程默认操作人。 */
    private static final String DEFAULT_OPERATOR = "system";

    @Resource
    private LhNextShiftNewPlanMapper nextShiftNewPlanMapper;

    /** 在班次9已有独立事务中回填实际故障或维修日期。 */
    @Resource
    private LhDeviceStopPlanScheduleService deviceStopPlanScheduleService;

    /**
     * 原子刷新班次9结果。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 本次排程请求日期
     * @param batchNo 本次排程批次号
     * @param planList 有效班次9计划；空集合表示清理旧结果
     * @param operator 操作人
     * @return 实际保存记录数
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public int replaceByScope(String factoryCode,
                              Date scheduleDate,
                              String batchNo,
                              List<LhNextShiftNewPlan> planList,
                              String operator) {
        return this.replaceByScope(factoryCode, scheduleDate, batchNo, planList, operator,
                Collections.<Long, Date>emptyMap());
    }

    /**
     * 复用班次9原事务，同时保存本次独立计算真实覆盖的设备停机日期。
     * @param factoryCode 工厂
     * @param scheduleDate 请求日期
     * @param batchNo 批次
     * @param planList 班次9结果
     * @param operator 操作人
     * @param stopScheduleDates 实际执行的设备计划日期，不能使用请求日期替代
     * @return 保存数量
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public int replaceByScope(String factoryCode, Date scheduleDate, String batchNo,
            List<LhNextShiftNewPlan> planList, String operator, Map<Long, Date> stopScheduleDates) {
        if (StringUtils.isEmpty(factoryCode) || Objects.isNull(scheduleDate)
                || StringUtils.isEmpty(batchNo)) {
            throw new IllegalArgumentException("班次9计划持久化范围不能为空");
        }
        int deletedCount = nextShiftNewPlanMapper.delete(
                new LambdaQueryWrapper<LhNextShiftNewPlan>()
                        .eq(LhNextShiftNewPlan::getFactoryCode, factoryCode)
                        .eq(LhNextShiftNewPlan::getScheduleDate, scheduleDate));
        if (!CollectionUtils.isEmpty(stopScheduleDates)) {
            // 无产出也可能因故障整班禁产；日期回填依赖实际覆盖窗口，不依赖正量结果。
            deviceStopPlanScheduleService.batchFillStopScheduleDates(stopScheduleDates);
        }
        if (CollectionUtils.isEmpty(planList)) {
            log.info("班次9计划幂等刷新完成, factoryCode: {}, scheduleDate: {}, batchNo: {}, "
                            + "deletedCount: {}, savedCount: 0",
                    factoryCode, scheduleDate, batchNo, deletedCount);
            return 0;
        }
        Date now = new Date();
        String actualOperator = StringUtils.isEmpty(operator) ? DEFAULT_OPERATOR : operator;
        planList.stream()
                .filter(Objects::nonNull)
                .forEach(plan -> {
                    plan.setCreateBy(actualOperator);
                    plan.setCreateTime(now);
                    plan.setUpdateBy(actualOperator);
                    plan.setUpdateTime(now);
                    plan.setIsDelete(0);
                });
        for (LhNextShiftNewPlan plan : planList) {
            nextShiftNewPlanMapper.insert(plan);
        }
        log.info("班次9计划幂等刷新完成, factoryCode: {}, scheduleDate: {}, batchNo: {}, "
                        + "deletedCount: {}, savedCount: {}",
                factoryCode, scheduleDate, batchNo, deletedCount, planList.size());
        return planList.size();
    }
}
