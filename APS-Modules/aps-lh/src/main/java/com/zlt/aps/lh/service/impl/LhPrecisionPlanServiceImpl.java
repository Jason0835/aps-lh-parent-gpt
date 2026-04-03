package com.zlt.aps.lh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.lh.api.domain.entity.LhPrecisionPlan;
import com.zlt.aps.lh.api.domain.vo.LhPrecisionPlanVo;
import com.zlt.aps.lh.mapper.LhPrecisionPlanMapper;
import com.zlt.aps.lh.service.ILhPrecisionPlanService;
import com.zlt.aps.maindata.enums.MsgTemplateEnums;
import com.zlt.aps.maindata.mapper.MdmDevMaintenancePlanEntityMapper;
import com.zlt.aps.maindata.utils.MessageServiceUtils;
import com.zlt.aps.mp.api.domain.entity.MdmDevMaintenancePlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 硫化精度计划Service实现类
 *
 * @author APS Team
 */
@Slf4j
@Service
public class LhPrecisionPlanServiceImpl extends ServiceImpl<LhPrecisionPlanMapper, LhPrecisionPlan>
        implements ILhPrecisionPlanService {

    private static final String PRECISION_TYPE_LH = "硫化精度";
    private static final String COMPLETION_STATUS_PENDING = "0";
    private static final String COMPLETION_STATUS_COMPLETED = "1";
    private static final String WARNING_STATUS_NO = "0";
    private static final String WARNING_STATUS_YES = "1";
    private static final String WARNING_SENT_NO = "0";
    private static final String WARNING_SENT_YES = "1";
    private static final String DATA_SOURCE_MES = "0";
    private static final String DATA_SOURCE_AUTO = "1";
    private static final Integer WARNING_DAYS = 30;

    @Autowired
    private MdmDevMaintenancePlanEntityMapper mdmDevMaintenancePlanEntityMapper;

    @Autowired
    private MessageServiceUtils messageServiceUtils;

    @Override
    public List<LhPrecisionPlan> selectLhPrecisionPlanList(LhPrecisionPlanVo vo) {
        return baseMapper.selectLhPrecisionPlanList(vo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generatePlansFromMes() {
        log.info("开始从MES同步数据生成硫化精度初版计划");

        try {
            LambdaQueryWrapper<MdmDevMaintenancePlan> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MdmDevMaintenancePlan::getPrecisionType, PRECISION_TYPE_LH)
                   .eq(MdmDevMaintenancePlan::getIsDelete, 0);

            List<MdmDevMaintenancePlan> mesPlans = mdmDevMaintenancePlanEntityMapper.selectList(wrapper);
            if (mesPlans == null || mesPlans.isEmpty()) {
                log.warn("从MES查询硫化精度数据为空");
                return 0;
            }

            log.info("从MES查询到{}条硫化精度数据", mesPlans.size());

            List<LhPrecisionPlan> plansToSave = new ArrayList<>();
            for (MdmDevMaintenancePlan mesPlan : mesPlans) {
                if (existsByMesSourceId(mesPlan.getId())) {
                    log.debug("MES数据ID={}已存在，跳过", mesPlan.getId());
                    continue;
                }

                LhPrecisionPlan plan = convertFromMes(mesPlan);
                if (plan != null) {
                    plansToSave.add(plan);
                    log.info("准备生成硫化精度计划：机台={}, 计划日期={}", plan.getMachineCode(), plan.getPlanDate());
                }
            }

            if (!plansToSave.isEmpty()) {
                saveBatch(plansToSave);
                log.info("从MES同步数据生成硫化精度计划完成，共生成{}条", plansToSave.size());
            }

            return plansToSave.size();
        } catch (Exception e) {
            log.error("从MES同步数据生成硫化精度计划失败", e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int autoGenerateYearlyPlans(Integer year) {
        log.info("开始自动生成{}年度硫化精度计划", year);

        try {
            LambdaQueryWrapper<LhPrecisionPlan> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(LhPrecisionPlan::getCompletionStatus, COMPLETION_STATUS_COMPLETED)
                   .eq(LhPrecisionPlan::getYear, year - 1)
                   .isNotNull(LhPrecisionPlan::getActualDate)
                   .eq(LhPrecisionPlan::getIsDelete, BigDecimal.ZERO)
                   .orderByDesc(LhPrecisionPlan::getActualDate);

            List<LhPrecisionPlan> lastYearPlans = list(wrapper);
            log.info("查询到{}年已完成计划{}条", year - 1, lastYearPlans.size());

            Map<String, LhPrecisionPlan> machinePlanMap = new HashMap<>();
            for (LhPrecisionPlan plan : lastYearPlans) {
                String machineCode = plan.getMachineCode();
                if (!machinePlanMap.containsKey(machineCode)) {
                    machinePlanMap.put(machineCode, plan);
                }
            }

            List<LhPrecisionPlan> plansToSave = new ArrayList<>();
            for (Map.Entry<String, LhPrecisionPlan> entry : machinePlanMap.entrySet()) {
                String machineCode = entry.getKey();
                LhPrecisionPlan lastPlan = entry.getValue();

                LhPrecisionPlan existingPlan = baseMapper.selectByMachineCodeAndYear(machineCode, year);
                if (existingPlan != null) {
                    log.debug("机台{}在{}年已有计划，跳过", machineCode, year);
                    continue;
                }

                LhPrecisionPlan newPlan = createYearlyPlan(machineCode, lastPlan.getActualDate(), year);
                if (newPlan != null) {
                    plansToSave.add(newPlan);
                    log.info("准备自动生成硫化精度计划：机台={}, 计划日期={}", machineCode, newPlan.getPlanDate());
                }
            }

            if (!plansToSave.isEmpty()) {
                saveBatch(plansToSave);
                log.info("自动生成{}年度硫化精度计划完成，共生成{}条", year, plansToSave.size());
            }

            return plansToSave.size();
        } catch (Exception e) {
            log.error("自动生成{}年度硫化精度计划失败", year, e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int checkWarning() {
        log.info("开始执行30天预警检查");

        try {
            List<LhPrecisionPlan> plans = baseMapper.selectPendingWarningPlans(WARNING_DAYS);
            log.info("查询到待预警计划{}条", plans.size());

            if (plans.isEmpty()) {
                return 0;
            }

            LocalDate now = LocalDate.now();
            for (LhPrecisionPlan plan : plans) {
                plan.setBaseVale(plan.getId());
                plan.setWarningStatus(WARNING_STATUS_YES);
                plan.setWarningDate(now);
                plan.setIsWarningSent(WARNING_SENT_YES);

                log.info("触发预警：机台={}, 计划日期={}, 剩余天数={}", 
                    plan.getMachineCode(), plan.getPlanDate(), plan.getDaysToDue());

                sendWarning(plan);
            }

            updateBatchById(plans);
            log.info("30天预警检查完成，共预警{}条", plans.size());

            return plans.size();
        } catch (Exception e) {
            log.error("30天预警检查失败", e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdateDaysToDue() {
        log.info("开始批量更新到期天数");
        int count = baseMapper.batchUpdateDaysToDue();
        log.info("批量更新到期天数完成，更新{}条", count);
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateActualDate(Long mesSourceId, String actualDate) {
        log.info("MES回传实际完成时间：MES_SOURCE_ID={}, ACTUAL_DATE={}", mesSourceId, actualDate);

        LambdaQueryWrapper<LhPrecisionPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LhPrecisionPlan::getMesSourceId, mesSourceId)
               .eq(LhPrecisionPlan::getIsDelete, BigDecimal.ZERO);

        LhPrecisionPlan plan = getOne(wrapper);
        if (plan == null) {
            log.warn("未找到MES_SOURCE_ID={}的计划", mesSourceId);
            return false;
        }

        LocalDate actualDateParsed = parseDate(actualDate);
        if (actualDateParsed == null) {
            log.warn("解析实际日期失败：{}", actualDate);
            return false;
        }

        plan.setBaseVale(plan.getId());
        plan.setActualDate(actualDateParsed);
        plan.setCompletionStatus(COMPLETION_STATUS_COMPLETED);
        plan.setDueDate(actualDateParsed.plusYears(1));

        boolean result = updateById(plan);
        if (result) {
            log.info("更新实际完成时间成功：机台={}, 实际日期={}", plan.getMachineCode(), actualDateParsed);
        }

        return result;
    }

    private boolean existsByMesSourceId(Long mesSourceId) {
        LambdaQueryWrapper<LhPrecisionPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LhPrecisionPlan::getMesSourceId, mesSourceId)
               .eq(LhPrecisionPlan::getIsDelete, BigDecimal.ZERO);
        return count(wrapper) > 0;
    }

    private LhPrecisionPlan convertFromMes(MdmDevMaintenancePlan mesPlan) {
        if (mesPlan == null || !PRECISION_TYPE_LH.equals(mesPlan.getPrecisionType())) {
            return null;
        }

        LhPrecisionPlan plan = new LhPrecisionPlan();

        plan.setMachineCode(mesPlan.getDevCode());
        plan.setPrecisionType(mesPlan.getPrecisionType());
        plan.setCompanyCode(mesPlan.getCompanyCode());
        plan.setFactoryCode(mesPlan.getFactoryCode());
        plan.setMesSourceId(mesPlan.getId());
        plan.setDataSource(DATA_SOURCE_MES);

        LocalDate planDate = parseDate(mesPlan.getOperTime());
        if (planDate == null) {
            log.warn("解析计划日期失败：{}", mesPlan.getOperTime());
            return null;
        }
        plan.setPlanDate(planDate);
        plan.setYear(new BigDecimal(planDate.getYear()));
        plan.setDueDate(planDate.plusYears(1));

        if (mesPlan.getFirstWashTime() != null) {
            LocalDate actualDate = parseDate(mesPlan.getFirstWashTime());
            if (actualDate != null) {
                plan.setActualDate(actualDate);
                plan.setCompletionStatus(COMPLETION_STATUS_COMPLETED);
                plan.setDueDate(actualDate.plusYears(1));
            }
        } else {
            plan.setCompletionStatus(COMPLETION_STATUS_PENDING);
        }

        LocalDate today = LocalDate.now();
        plan.setDaysToDue((int) ChronoUnit.DAYS.between(today, planDate));

        plan.setWarningStatus(WARNING_STATUS_NO);
        plan.setIsWarningSent(WARNING_SENT_NO);
        plan.setIsDelete(0);
        plan.setBaseVale(null);

        return plan;
    }

    private LhPrecisionPlan createYearlyPlan(String machineCode, LocalDate lastActualDate, Integer year) {
        if (lastActualDate == null) {
            log.warn("机台{}的上次实际日期为空，无法生成计划", machineCode);
            return null;
        }

        LhPrecisionPlan plan = new LhPrecisionPlan();

        plan.setMachineCode(machineCode);
        plan.setPrecisionType(PRECISION_TYPE_LH);
        plan.setDataSource(DATA_SOURCE_AUTO);

        LocalDate planDate = lastActualDate.plusYears(1);
        plan.setPlanDate(planDate);
        plan.setYear(new BigDecimal(year));
        plan.setLastMaintenanceDate(lastActualDate);
        plan.setDueDate(planDate.plusYears(1));

        LocalDate today = LocalDate.now();
        plan.setDaysToDue((int) ChronoUnit.DAYS.between(today, planDate));

        plan.setCompletionStatus(COMPLETION_STATUS_PENDING);
        plan.setWarningStatus(WARNING_STATUS_NO);
        plan.setIsWarningSent(WARNING_SENT_NO);
        plan.setIsDelete(0);
        plan.setBaseVale(null);

        return plan;
    }

    private LocalDate parseDate(String dateStr) {
        if (!StringUtils.hasText(dateStr)) {
            return null;
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
            return LocalDate.parse(dateStr, formatter);
        } catch (Exception e) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                return LocalDate.parse(dateStr, formatter);
            } catch (Exception ex) {
                log.warn("解析日期失败：{}", dateStr, ex);
                return null;
            }
        }
    }

    private LocalDate parseDate(Date date) {
        if (date == null) {
            return null;
        }
        try {
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        } catch (Exception e) {
            log.warn("解析Date失败：{}", date, e);
            return null;
        }
    }

    private void sendWarning(LhPrecisionPlan plan) {
        log.info("发送预警通知：机台={}, 计划日期={}, 剩余天数={}", 
            plan.getMachineCode(), plan.getPlanDate(), plan.getDaysToDue());

        try {
            String templateCode = MsgTemplateEnums.LH_PRECISION_PLAN_WARNING.getCode();
            
            String planDateStr = plan.getPlanDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            messageServiceUtils.sendWarning(templateCode, (String) null, 
                plan.getMachineCode(), 
                planDateStr, 
                plan.getDaysToDue());
            
            log.info("预警通知发送成功：机台={}, 计划日期={}", plan.getMachineCode(), plan.getPlanDate());
        } catch (Exception e) {
            log.error("预警通知发送失败：机台={}, 计划日期={}", plan.getMachineCode(), plan.getPlanDate(), e);
        }
    }
}
