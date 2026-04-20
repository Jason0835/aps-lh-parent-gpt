package com.zlt.aps.lh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.redis.service.RedisService;
import com.zlt.aps.lh.api.domain.entity.LhParams;
import com.zlt.aps.lh.api.domain.entity.LhPrecisionPlan;
import com.zlt.aps.lh.api.domain.vo.LhPrecisionPlanImportVO;
import com.zlt.aps.lh.api.domain.vo.LhPrecisionPlanVo;
import com.zlt.aps.lh.mapper.LhPrecisionPlanMapper;
import com.zlt.aps.lh.service.ILhParamsService;
import com.zlt.aps.lh.service.ILhPrecisionPlanService;
import com.zlt.aps.maindata.enums.MsgTemplateEnums;
import com.zlt.aps.maindata.mapper.MdmDevMaintenancePlanEntityMapper;
import com.zlt.aps.maindata.utils.MessageServiceUtils;
import com.zlt.aps.mp.api.domain.entity.MdmDevMaintenancePlan;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.core.dao.basedao.BaseDao;
import com.zlt.sysdef.domain.SysDocType;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Date;

@Slf4j
@Service
public class LhPrecisionPlanServiceImpl extends AbstractDocService<LhPrecisionPlan> implements ILhPrecisionPlanService {

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
    private static final Integer DEFAULT_INTERVAL_YEARS = 1;

    @Autowired
    private MdmDevMaintenancePlanEntityMapper mdmDevMaintenancePlanEntityMapper;

    @Autowired
    private MessageServiceUtils messageServiceUtils;

    @Autowired
    private LhPrecisionPlanMapper lhPrecisionPlanMapper;

    @Autowired
    private RedisService redisService;

    @Autowired
    private ILhParamsService lhParamsService;

    @Autowired
    private BaseDao baseDao;

    @Override
    protected String getDocTypeCode() {
        return "LH_PRECISION_PLAN";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("LH_PRECISION_PLAN");
        return sysDocType;
    }

    @Override
    public List<LhPrecisionPlan> selectLhPrecisionPlanList(LhPrecisionPlanVo vo) {
        return lhPrecisionPlanMapper.selectLhPrecisionPlanList(vo);
    }

    @Override
    public int save(LhPrecisionPlan entity) {
        calculateDaysToDue(entity);
        return super.save(entity);
    }

    private void calculateDaysToDue(LhPrecisionPlan entity) {
        if (entity.getPlanDate() != null) {
            LocalDate today = LocalDate.now();
            LocalDate planDate = entity.getPlanDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            int daysToDue = (int) ChronoUnit.DAYS.between(today, planDate);
            if (daysToDue < 0) {
                daysToDue = 0;
            }
            entity.setDaysToDue(daysToDue);
            
            if (entity.getYear() == null) {
                entity.setYear(new BigDecimal(planDate.getYear()));
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult importDataFeign(List<LhPrecisionPlanImportVO> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<LhPrecisionPlan> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            LhPrecisionPlanImportVO importVO = list.get(i);
            
            // 校验必填项
            if (StringUtil.isBlank(importVO.getMachineCode())) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.lh.precision.plan.machineCodeRequired");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                    errorNum, String.format(message, errorNum), importErrorLogs);
                continue;
            }

            if (importVO.getPlanDate() == null) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.lh.precision.plan.planDateRequired");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                    errorNum, String.format(message, errorNum), importErrorLogs);
                continue;
            }

            if (importVO.getActualDate() == null) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.lh.precision.plan.actualDateRequired");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                    errorNum, String.format(message, errorNum), importErrorLogs);
                continue;
            }

            // 计算年度
            BigDecimal year = new BigDecimal(importVO.getPlanDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().getYear());
            
            // 按年度+机台号先删后插
            QueryWrapper<LhPrecisionPlan> existWrapper = new QueryWrapper<>();
            existWrapper.eq("MACHINE_CODE", importVO.getMachineCode());
            existWrapper.eq("YEAR", year);
            existWrapper.eq("IS_DELETE", 0);
            List<LhPrecisionPlan> existList = lhPrecisionPlanMapper.selectList(existWrapper);
            
            if (!existList.isEmpty()) {
                for (LhPrecisionPlan exist : existList) {
                    lhPrecisionPlanMapper.deleteById(exist.getId());
                }
            }
            
            // 转换为实体对象
            LhPrecisionPlan entity = new LhPrecisionPlan();
            entity.setMachineCode(importVO.getMachineCode());
            entity.setPrecisionType("精度计划");
            entity.setPlanDate(importVO.getPlanDate());
            entity.setActualDate(importVO.getActualDate());
            entity.setRemark(importVO.getRemark());
            entity.setIsDelete(0);
            entity.setWarningStatus(WARNING_STATUS_NO);
            entity.setIsWarningSent(WARNING_SENT_NO);
            entity.setCompletionStatus(COMPLETION_STATUS_PENDING);
            entity.setDataSource(DATA_SOURCE_MES);
            entity.setYear(year);
            calculateDaysToDue(entity);
            
            importList.add(entity);
            successNum++;
        }

        if (CollectionUtils.isEmpty(importList)) {
            return AjaxResult.error("成功 0条，失败 " + failureNum + "条", importErrorLogs);
        }

        baseDao.insertBatch(importList);

        if (failureNum > 0) {
            return AjaxResult.error("成功 " + successNum + "条，失败 " + failureNum + "条", importErrorLogs);
        } else {
            return AjaxResult.success("成功 " + successNum + "条，失败 " + failureNum + "条");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generatePlansFromMes(Integer year) {
        String lockKey = "generate:precision:plan:" + year;
        if (redisService.getCacheObject(lockKey) != null) {
            throw new RuntimeException(I18nUtil.getMessage("ui.lh.precisionPlan.generate.in.progress"));
        }

        try {
            redisService.setCacheObject(lockKey, "1");
            log.info("开始从MES同步数据生成{}年度硫化精度计划", year);

            LambdaQueryWrapper<MdmDevMaintenancePlan> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MdmDevMaintenancePlan::getPrecisionType, PRECISION_TYPE_LH)
                   .eq(MdmDevMaintenancePlan::getIsDelete, 0);

            List<MdmDevMaintenancePlan> mesPlans = mdmDevMaintenancePlanEntityMapper.selectList(wrapper);
            if (mesPlans == null || mesPlans.isEmpty()) {
                log.warn("从MES查询硫化精度数据为空");
                return 0;
            }

            log.info("从MES查询到{}条硫化精度数据", mesPlans.size());

            QueryWrapper<LhPrecisionPlan> deleteWrapper = new QueryWrapper<>();
            deleteWrapper.eq("YEAR", year);
            deleteWrapper.eq("DATA_SOURCE", DATA_SOURCE_AUTO);
            deleteWrapper.eq("IS_DELETE", 0);
            lhPrecisionPlanMapper.delete(deleteWrapper);
            log.info("已删除{}年度系统生成的计划数据", year);

            int intervalYears = getIntervalYears();

            List<LhPrecisionPlan> plansToSave = new ArrayList<>();
            Map<String, MdmDevMaintenancePlan> latestMesPlanMap = new HashMap<>();
            
            for (MdmDevMaintenancePlan mesPlan : mesPlans) {
                String machineCode = mesPlan.getDevCode();
                LocalDate actualDate = parseDate(mesPlan.getFirstWashTime());
                
                if (actualDate != null) {
                    MdmDevMaintenancePlan existing = latestMesPlanMap.get(machineCode);
                    if (existing == null || actualDate.isAfter(parseDate(existing.getFirstWashTime()))) {
                        latestMesPlanMap.put(machineCode, mesPlan);
                    }
                }
            }

            for (Map.Entry<String, MdmDevMaintenancePlan> entry : latestMesPlanMap.entrySet()) {
                String machineCode = entry.getKey();
                MdmDevMaintenancePlan mesPlan = entry.getValue();
                
                LhPrecisionPlan plan = createPlanFromMes(machineCode, mesPlan, year, intervalYears);
                if (plan != null) {
                    plansToSave.add(plan);
                    log.info("准备生成硫化精度计划：机台={}, 计划日期={}", machineCode, plan.getPlanDate());
                }
            }

            if (!plansToSave.isEmpty()) {
                baseDao.insertBatch(plansToSave);
                log.info("从MES同步数据生成{}年度硫化精度计划完成，共生成{}条", year, plansToSave.size());
            }

            return plansToSave.size();
        } catch (Exception e) {
            log.error("从MES同步数据生成硫化精度计划失败", e);
            throw e;
        } finally {
            redisService.deleteObject(lockKey);
        }
    }

    private LhPrecisionPlan createPlanFromMes(String machineCode, MdmDevMaintenancePlan mesPlan, Integer year, int intervalYears) {
        if (mesPlan == null) {
            return null;
        }

        LhPrecisionPlan plan = new LhPrecisionPlan();

        plan.setMachineCode(machineCode);
        plan.setPrecisionType(PRECISION_TYPE_LH);
        plan.setCompanyCode(mesPlan.getCompanyCode());
        plan.setFactoryCode(mesPlan.getFactoryCode());
        plan.setMesSourceId(mesPlan.getId());
        plan.setDataSource(DATA_SOURCE_AUTO);

        LocalDate actualDateLocal = parseDate(mesPlan.getFirstWashTime());
        if (actualDateLocal == null) {
            log.warn("机台{}的实际执行时间为空，跳过", machineCode);
            return null;
        }
        
        Date actualDate = Date.from(actualDateLocal.atStartOfDay(ZoneId.systemDefault()).toInstant());
        plan.setActualDate(actualDate);
        plan.setLastMaintenanceDate(actualDate);
        
        LocalDate planDateLocal = actualDateLocal.plusYears(intervalYears);
        Date planDate = Date.from(planDateLocal.atStartOfDay(ZoneId.systemDefault()).toInstant());
        plan.setPlanDate(planDate);
        plan.setYear(new BigDecimal(year));

        LocalDate today = LocalDate.now();
        plan.setDaysToDue((int) ChronoUnit.DAYS.between(today, planDateLocal));

        plan.setCompletionStatus(COMPLETION_STATUS_PENDING);
        plan.setWarningStatus(WARNING_STATUS_NO);
        plan.setIsWarningSent(WARNING_SENT_NO);
        plan.setIsDelete(0);
        plan.setBaseVale(null);

        return plan;
    }

    private int getIntervalYears() {
        LhParams params = lhParamsService.selectOneByParamCode("PRECISION_PLAN_INTERVAL_YEARS", null);
        if (params != null && params.getParamValue() != null) {
            try {
                return Integer.parseInt(params.getParamValue());
            } catch (NumberFormatException e) {
                log.warn("硫化精度计划间隔年数参数配置错误，使用默认值1");
            }
        }
        return DEFAULT_INTERVAL_YEARS;
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
                   .eq(LhPrecisionPlan::getIsDelete, 0)
                   .orderByDesc(LhPrecisionPlan::getActualDate);

            List<LhPrecisionPlan> lastYearPlans = lhPrecisionPlanMapper.selectList(wrapper);
            log.info("查询到{}年已完成计划{}条", year - 1, lastYearPlans.size());

            Map<String, LhPrecisionPlan> machinePlanMap = new HashMap<>();
            for (LhPrecisionPlan plan : lastYearPlans) {
                String machineCode = plan.getMachineCode();
                if (!machinePlanMap.containsKey(machineCode)) {
                    machinePlanMap.put(machineCode, plan);
                }
            }

            int intervalYears = getIntervalYears();
            List<LhPrecisionPlan> plansToSave = new ArrayList<>();
            for (Map.Entry<String, LhPrecisionPlan> entry : machinePlanMap.entrySet()) {
                String machineCode = entry.getKey();
                LhPrecisionPlan lastPlan = entry.getValue();

                LhPrecisionPlan existingPlan = lhPrecisionPlanMapper.selectByMachineCodeAndYear(machineCode, year);
                if (existingPlan != null) {
                    log.debug("机台{}在{}年已有计划，跳过", machineCode, year);
                    continue;
                }

                LhPrecisionPlan newPlan = createYearlyPlan(machineCode, lastPlan.getActualDate(), year, intervalYears);
                if (newPlan != null) {
                    plansToSave.add(newPlan);
                    log.info("准备自动生成硫化精度计划：机台={}, 计划日期={}", machineCode, newPlan.getPlanDate());
                }
            }

            if (!plansToSave.isEmpty()) {
                for (LhPrecisionPlan plan : plansToSave) {
                    lhPrecisionPlanMapper.insert(plan);
                }
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
            List<LhPrecisionPlan> plans = lhPrecisionPlanMapper.selectPendingWarningPlans(WARNING_DAYS);
            log.info("查询到待预警计划{}条", plans.size());

            if (plans.isEmpty()) {
                return 0;
            }

            Date now = new Date();
            for (LhPrecisionPlan plan : plans) {
                plan.setBaseVale(plan.getId());
                plan.setWarningStatus(WARNING_STATUS_YES);
                plan.setWarningDate(now);
                plan.setIsWarningSent(WARNING_SENT_YES);

                log.info("触发预警：机台={}, 计划日期={}, 剩余天数={}", 
                    plan.getMachineCode(), plan.getPlanDate(), plan.getDaysToDue());

                sendWarning(plan);
            }

            for (LhPrecisionPlan plan : plans) {
                lhPrecisionPlanMapper.updateById(plan);
            }
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
        int count = lhPrecisionPlanMapper.batchUpdateDaysToDue();
        log.info("批量更新到期天数完成，更新{}条", count);
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateActualDate(Long mesSourceId, String actualDate) {
        log.info("MES回传实际完成时间：MES_SOURCE_ID={}, ACTUAL_DATE={}", mesSourceId, actualDate);

        LambdaQueryWrapper<LhPrecisionPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LhPrecisionPlan::getMesSourceId, mesSourceId)
               .eq(LhPrecisionPlan::getIsDelete, 0);

        LhPrecisionPlan plan = lhPrecisionPlanMapper.selectOne(wrapper);
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
        Date actualDateObj = Date.from(actualDateParsed.atStartOfDay(ZoneId.systemDefault()).toInstant());
        plan.setActualDate(actualDateObj);
        plan.setCompletionStatus(COMPLETION_STATUS_COMPLETED);
        Date dueDate = Date.from(actualDateParsed.plusYears(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        plan.setDueDate(dueDate);

        int result = lhPrecisionPlanMapper.updateById(plan);
        if (result > 0) {
            log.info("更新实际完成时间成功：机台={}, 实际日期={}", plan.getMachineCode(), actualDateParsed);
        }

        return result > 0;
    }

    private LhPrecisionPlan createYearlyPlan(String machineCode, Date lastActualDate, Integer year, int intervalYears) {
        if (lastActualDate == null) {
            log.warn("机台{}的上次实际日期为空，无法生成计划", machineCode);
            return null;
        }

        LhPrecisionPlan plan = new LhPrecisionPlan();

        plan.setMachineCode(machineCode);
        plan.setPrecisionType(PRECISION_TYPE_LH);
        plan.setDataSource(DATA_SOURCE_AUTO);

        LocalDate lastActualDateLocal = lastActualDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate planDateLocal = lastActualDateLocal.plusYears(intervalYears);
        Date planDate = Date.from(planDateLocal.atStartOfDay(ZoneId.systemDefault()).toInstant());
        plan.setPlanDate(planDate);
        plan.setYear(new BigDecimal(year));
        plan.setLastMaintenanceDate(lastActualDate);

        LocalDate today = LocalDate.now();
        plan.setDaysToDue((int) ChronoUnit.DAYS.between(today, planDateLocal));

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
            
            LocalDate planDateLocal = plan.getPlanDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            String planDateStr = planDateLocal.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            messageServiceUtils.sendWarning(templateCode, (String) null, 
                plan.getMachineCode(), 
                planDateStr, 
                plan.getDaysToDue());
            
            log.info("预警通知发送成功：机台={}, 计划日期={}", plan.getMachineCode(), plan.getPlanDate());
        } catch (Exception e) {
            log.error("预警通知发送失败：机台={}, 计划日期={}", plan.getMachineCode(), plan.getPlanDate(), e);
        }
    }

    @Override
    public String checkUnique(LhPrecisionPlan docEntityVO) {
        if (docEntityVO == null) {
            return UserConstants.UNIQUE;
        }
        
        QueryWrapper<LhPrecisionPlan> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("MACHINE_CODE", docEntityVO.getMachineCode());
        queryWrapper.eq("YEAR", docEntityVO.getYear());
        queryWrapper.eq("IS_DELETE", 0);
        
        if (docEntityVO.getId() != null) {
            queryWrapper.ne("ID", docEntityVO.getId());
        }
        
        long count = lhPrecisionPlanMapper.selectCount(queryWrapper);
        return count > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generateFromMaintenancePlan(List<MdmDevMaintenancePlan> maintenancePlans) {
        if (CollectionUtils.isEmpty(maintenancePlans)) {
            log.warn("设备保养计划列表为空");
            return 0;
        }

        log.info("开始根据设备保养计划生成硫化精度计划，共{}条", maintenancePlans.size());

        int intervalYears = getIntervalYears();
        List<LhPrecisionPlan> plansToSave = new ArrayList<>();

        for (MdmDevMaintenancePlan mesPlan : maintenancePlans) {
            String machineCode = mesPlan.getDevCode();
            LocalDate actualDateLocal = parseDate(mesPlan.getFirstWashTime());

            if (actualDateLocal == null) {
                log.warn("机台{}的实际执行时间为空，跳过", machineCode);
                continue;
            }

            LocalDate planDateLocal = actualDateLocal.plusYears(intervalYears);
            int year = planDateLocal.getYear();

            LhPrecisionPlan existingPlan = lhPrecisionPlanMapper.selectByMachineCodeAndYear(machineCode, year);
            if (existingPlan != null) {
                log.debug("机台{}在{}年已有计划，跳过", machineCode, year);
                continue;
            }

            Date actualDate = Date.from(actualDateLocal.atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date planDate = Date.from(planDateLocal.atStartOfDay(ZoneId.systemDefault()).toInstant());

            LhPrecisionPlan plan = new LhPrecisionPlan();
            plan.setMachineCode(machineCode);
            plan.setPrecisionType(PRECISION_TYPE_LH);
            plan.setCompanyCode(mesPlan.getCompanyCode());
            plan.setFactoryCode(mesPlan.getFactoryCode());
            plan.setMesSourceId(mesPlan.getId());
            plan.setDataSource(DATA_SOURCE_MES);
            plan.setActualDate(actualDate);
            plan.setLastMaintenanceDate(actualDate);
            plan.setPlanDate(planDate);
            plan.setYear(new BigDecimal(year));

            LocalDate today = LocalDate.now();
            plan.setDaysToDue((int) ChronoUnit.DAYS.between(today, planDateLocal));

            plan.setCompletionStatus(COMPLETION_STATUS_PENDING);
            plan.setWarningStatus(WARNING_STATUS_NO);
            plan.setIsWarningSent(WARNING_SENT_NO);
            plan.setIsDelete(0);
            plan.setBaseVale(null);

            plansToSave.add(plan);
            log.info("准备生成硫化精度计划：机台={}, 计划日期={}", machineCode, plan.getPlanDate());
        }

        if (!plansToSave.isEmpty()) {
            baseDao.insertBatch(plansToSave);
            log.info("成功生成{}条硫化精度计划", plansToSave.size());
        }

        return plansToSave.size();
    }
}
