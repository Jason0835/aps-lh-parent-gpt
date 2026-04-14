package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cx.mapper.CxPrecisionPlanMapper;
import com.zlt.aps.cx.mapper.MdmMoldingMachineMapper;
import com.zlt.aps.cx.service.ICxPrecisionPlanService;
import com.zlt.aps.maindata.mapper.MdmDevMaintenancePlanEntityMapper;
import com.zlt.aps.cx.api.domain.entity.CxPrecisionPlan;
import com.zlt.aps.mp.api.domain.entity.MdmDevMaintenancePlan;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachine;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.PubUtil;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 鎴愬瀷绮惧害璁″垝鏈嶅姟瀹炵幇绫?
 *
 * @author APS Team
 */
@Slf4j
@Service
public class CxPrecisionPlanServiceImpl extends AbstractDocService<CxPrecisionPlan> implements ICxPrecisionPlanService {

    /**
     * 精度类型：成型精度
     */
    private static final String PRECISION_TYPE_CX = "成型精度";
    private static final BigDecimal DEFAULT_ESTIMATED_HOURS = new BigDecimal("4.0");
    private static final int PLAN_INTERVAL_MONTHS = 2;

    @Autowired
    private CxPrecisionPlanMapper cxPrecisionPlanMapper;
    @Autowired
    private MdmMoldingMachineMapper moldingMachineMapper;
    @Autowired
    private MdmDevMaintenancePlanEntityMapper mdmDevMaintenancePlanEntityMapper;

    @Override
    public String checkUnique(CxPrecisionPlan entity) {
        Long id = entity.getId();
        String machineCode = entity.getMachineCode();
        Date planDate = entity.getPlanDate();

        LambdaQueryWrapper<CxPrecisionPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CxPrecisionPlan::getMachineCode, machineCode)
                .eq(CxPrecisionPlan::getPlanDate, planDate);

        if (id != null) {
            wrapper.ne(CxPrecisionPlan::getId, id);
        }

        Long count = cxPrecisionPlanMapper.selectCount(wrapper);
        return count > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }


    @Override
    public String getDocTypeCode() {
        return "CX_PRECISION_PLAN";
    }

    @Override
    public AjaxResult importData(List<CxPrecisionPlan> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<CxPrecisionPlan> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");
        String machineNotExistMsg = I18nUtil.getMessage("ui.data.alert.cxPrecisionPlan.machineNotExist");

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            CxPrecisionPlan docEntity = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated);
            if (PubUtil.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            CxPrecisionPlan docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }

            if (StringUtil.isBlank(docEntity.getMachineCode())) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.data.alert.cxPrecisionPlan.machineCodeRequired");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, String.format(message, errorNum), importErrorLogs);
                continue;
            }

            if (docEntity.getPlanDate() == null) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.data.alert.cxPrecisionPlan.planDateRequired");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, String.format(message, errorNum), importErrorLogs);
                continue;
            }

            LambdaQueryWrapper<MdmMoldingMachine> machineWrapper = new LambdaQueryWrapper<>();
            machineWrapper.eq(MdmMoldingMachine::getCxMachineCode, docEntity.getMachineCode());
            if (StringUtil.isNotBlank(docEntity.getFactoryCode())) {
                machineWrapper.eq(MdmMoldingMachine::getFactoryCode, docEntity.getFactoryCode());
            }
            machineWrapper.last("LIMIT 1");
            MdmMoldingMachine machine = moldingMachineMapper.selectOne(machineWrapper);
            if (machine == null) {
                failureNum++;
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, String.format(machineNotExistMsg, errorNum, docEntity.getMachineCode()), importErrorLogs);
                continue;
            }

            if (docEntity.getPlanStartTime() != null && docEntity.getPlanDate() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                String planDateStr = sdf.format(docEntity.getPlanDate());
                String startTimeStr = sdf.format(docEntity.getPlanStartTime());
                if (startTimeStr.compareTo(planDateStr) < 0) {
                    failureNum++;
                    String message = I18nUtil.getMessage("ui.data.alert.cxPrecisionPlan.startTimeBeforePlanDate");
                    ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                            errorNum, String.format(message, errorNum), importErrorLogs);
                    continue;
                }
            }

            if (docEntity.getPlanStartTime() != null && docEntity.getPlanEndTime() != null) {
                if (docEntity.getPlanEndTime().before(docEntity.getPlanStartTime())) {
                    failureNum++;
                    String message = I18nUtil.getMessage("ui.data.alert.cxPrecisionPlan.endTimeBeforeStartTime");
                    ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                            errorNum, String.format(message, errorNum), importErrorLogs);
                    continue;
                }
                long diffMillis = docEntity.getPlanEndTime().getTime() - docEntity.getPlanStartTime().getTime();
                double hours = diffMillis / (1000.0 * 60 * 60);
                BigDecimal estimatedHours = BigDecimal.valueOf(hours).setScale(1, RoundingMode.HALF_UP);
                docEntity.setEstimatedHours(estimatedHours);
            }

            if (docEntity.getPlanDate() != null && docEntity.getLastPrecisionDate() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                String planDateStr = sdf.format(docEntity.getPlanDate());
                String lastPrecisionStr = sdf.format(docEntity.getLastPrecisionDate());
                if (lastPrecisionStr.compareTo(planDateStr) >= 0) {
                    failureNum++;
                    String message = I18nUtil.getMessage("ui.data.alert.cxPrecisionPlan.lastPrecisionDateAfterStartTime");
                    ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                            errorNum, String.format(message, errorNum), importErrorLogs);
                    continue;
                }
            }

            if (docEntity.getPlanDate() != null && docEntity.getDueDate() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                String planDateStr = sdf.format(docEntity.getPlanDate());
                String dueDateStr = sdf.format(docEntity.getDueDate());
                if (dueDateStr.compareTo(planDateStr) <= 0) {
                    failureNum++;
                    String message = I18nUtil.getMessage("ui.data.alert.cxPrecisionPlan.dueDateBeforePlanDate");
                    ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                            errorNum, String.format(message, errorNum), importErrorLogs);
                    continue;
                }
            }

            if (checkUnique(docEntity).equals(UserConstants.UNIQUE)) {
                importList.add(docEntity);
                successNum++;
            } else {
                if (updateSupport) {
                    QueryWrapper<CxPrecisionPlan> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("MACHINE_CODE", docEntity.getMachineCode());
                    queryWrapper.eq("PLAN_DATE", docEntity.getPlanDate());
                    CxPrecisionPlan existEntity = cxPrecisionPlanMapper.selectOne(queryWrapper);
                    if (existEntity != null) {
                        docEntity.setId(existEntity.getId());
                        importList.add(docEntity);
                        successNum++;
                    }
                } else {
                    failureNum++;
                    ImportExcelValidatedUtils.addImportErrorLog(importLogId, errorNum,
                            String.format(uniqueMsg, errorNum), importErrorLogs);
                }
            }
        }

        if (CollectionUtils.isEmpty(importList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }

        for (CxPrecisionPlan entity : importList) {
            if (entity.getId() != null) {
                cxPrecisionPlanMapper.updateById(entity);
            } else {
                cxPrecisionPlanMapper.insert(entity);
            }
        }

        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum + "," + failureNum);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generatePlansFromMes(Integer year) {
        Integer targetYear = year == null ? LocalDate.now().getYear() : year;
        log.info("开始从MES同步数据生成{}年度成型精度初版计划", targetYear);
        List<MdmDevMaintenancePlan> mesPlans = mdmDevMaintenancePlanEntityMapper.selectList(
                new LambdaQueryWrapper<MdmDevMaintenancePlan>()
                        .like(MdmDevMaintenancePlan::getPrecisionType, PRECISION_TYPE_CX)
        );

        if (CollectionUtils.isEmpty(mesPlans)) {
            log.warn("从MES未查询到成型精度计划数据");
            return 0;
        }

        int saveCount = 0;
        for (MdmDevMaintenancePlan mesPlan : mesPlans) {
            String machineCode = mesPlan.getDevCode();
            Date planDate = mesPlan.getOperTime();
            if (StringUtil.isBlank(machineCode) || planDate == null) {
                log.debug("MES计划数据不完整：机台={}，计划日={}", machineCode, planDate);
                continue;
            }
            LocalDate mesPlanDate = toLocalDateFromUtil(planDate);
            if (mesPlanDate == null || mesPlanDate.getYear() != targetYear) {
                continue;
            }

            if (existsByMachineAndDate(machineCode, planDate)) {
                log.debug("机台{} 日期{} 已存在计划，跳过", machineCode, planDate);
                continue;
            }

            CxPrecisionPlan plan = new CxPrecisionPlan();
            plan.setId(mesPlan.getId());
            plan.setMachineCode(machineCode);
//            plan.setMachineName(machineCode);
            plan.setFactoryCode(mesPlan.getFactoryCode());
            plan.setPlanDate(planDate);
            plan.setLastPrecisionDate(mesPlan.getFirstWashTime());
            plan.setEstimatedHours(DEFAULT_ESTIMATED_HOURS);
            plan.setDueDate(addMonths(planDate, PLAN_INTERVAL_MONTHS));
            if (plan.getDueDate() != null) {
                LocalDate today = LocalDate.now();
                LocalDate dueDate = toLocalDateFromUtil(plan.getDueDate());
                if (dueDate != null) {
                    plan.setDaysToDue((int) ChronoUnit.DAYS.between(today, dueDate));
                }
            }

            // 同步设备名称，保证展示一致
            MdmMoldingMachine machine = findMachine(machineCode, mesPlan.getFactoryCode());
            if (machine != null) {
                plan.setMachineName(machine.getMachineName());
            }
            cxPrecisionPlanMapper.insert(plan);
            saveCount++;
            log.info("生成成型精度初版计划成功：机台{}，计划日{}", machineCode, planDate);
        }

        return saveCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int autoGenerateYearlyPlans(Integer year) {
        log.info("开始自动生成{}年度成型精度计划", year);

        LocalDate currentYearStart = LocalDate.of(year, 1, 1);
        LocalDate currentYearEnd = LocalDate.of(year, 12, 31);
        LocalDate lastYearStart = currentYearStart.minusYears(1);
        LocalDate lastYearEnd = currentYearEnd.minusYears(1);

        List<CxPrecisionPlan> lastYearPlans = cxPrecisionPlanMapper.selectList(
                new QueryWrapper<CxPrecisionPlan>()
                        .between("PLAN_DATE",
                                Date.from(lastYearStart.atStartOfDay(ZoneId.systemDefault()).toInstant()),
                                Date.from(lastYearEnd.atStartOfDay(ZoneId.systemDefault()).toInstant()))
        );

        if (CollectionUtils.isEmpty(lastYearPlans)) {
            log.warn("未找到{}年度上一年度的数据支撑生成", year);
            return 0;
        }

        // 按机台取最近一条计划
        Map<String, CxPrecisionPlan> latestByMachine = lastYearPlans.stream()
                .filter(p -> PubUtil.isNotEmpty(p.getMachineCode()) && p.getPlanDate() != null)
                .collect(Collectors.toMap(CxPrecisionPlan::getMachineCode, p -> p, (p1, p2) ->
                        p1.getPlanDate().after(p2.getPlanDate()) ? p1 : p2));

        int saveCount = 0;
        for (Map.Entry<String, CxPrecisionPlan> entry : latestByMachine.entrySet()) {
            String machineCode = entry.getKey();
            CxPrecisionPlan lastPlan = entry.getValue();

            LocalDate baseDate = toLocalDate(lastPlan.getPlanDate());
            if (baseDate == null) {
                continue;
            }

            LocalDate newPlanDate = baseDate.plusYears(1);
            // 跨区按时间对齐年度
            newPlanDate = newPlanDate.withYear(year);

            if (existsInYear(machineCode, currentYearStart, currentYearEnd)) {
                log.debug("机台{} 在{}年度已存在计划，跳过", machineCode, year);
                continue;
            }

            CxPrecisionPlan plan = new CxPrecisionPlan();
            plan.setMachineCode(machineCode);
            plan.setFactoryCode(lastPlan.getFactoryCode());
            plan.setMachineName(lastPlan.getMachineName());
            plan.setPlanDate(toDate(newPlanDate));
            plan.setLastPrecisionDate(lastPlan.getPlanDate());
            plan.setEstimatedHours(DEFAULT_ESTIMATED_HOURS);
            plan.setDueDate(addMonths(plan.getPlanDate(), PLAN_INTERVAL_MONTHS));

            cxPrecisionPlanMapper.insert(plan);
            saveCount++;
            log.info("自动生成成型精度计划：机台{}，计划日{}", machineCode, plan.getPlanDate());
        }

        return saveCount;
    }

    private boolean existsByMachineAndDate(String machineCode, Date planDate) {
        QueryWrapper<CxPrecisionPlan> wrapper = new QueryWrapper<>();
        wrapper.eq("MACHINE_CODE", machineCode);
        wrapper.eq("PLAN_DATE", planDate);
        return cxPrecisionPlanMapper.selectCount(wrapper) > 0;
    }

    private boolean existsInYear(String machineCode, LocalDate yearStart, LocalDate yearEnd) {
        QueryWrapper<CxPrecisionPlan> wrapper = new QueryWrapper<>();
        wrapper.eq("MACHINE_CODE", machineCode);
        wrapper.between("PLAN_DATE",
                Date.from(yearStart.atStartOfDay(ZoneId.systemDefault()).toInstant()),
                Date.from(yearEnd.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        return cxPrecisionPlanMapper.selectCount(wrapper) > 0;
    }

    private MdmMoldingMachine findMachine(String machineCode, String factoryCode) {
        LambdaQueryWrapper<MdmMoldingMachine> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MdmMoldingMachine::getCxMachineCode, machineCode);
        if (StringUtil.isNotBlank(factoryCode)) {
            wrapper.eq(MdmMoldingMachine::getFactoryCode, factoryCode);
        }
        wrapper.last("LIMIT 1");
        return moldingMachineMapper.selectOne(wrapper);
    }

    private Date addMonths(Date date, int months) {
        if (date == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MONTH, months);
        return calendar.getTime();
    }

    private LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private Date toDate(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdateDaysToDue() {
        log.info("开始批量更新到期天数");
        List<CxPrecisionPlan> plans = cxPrecisionPlanMapper.selectList(
                new LambdaQueryWrapper<CxPrecisionPlan>()
                        .isNotNull(CxPrecisionPlan::getDueDate)
        );

        if (CollectionUtils.isEmpty(plans)) {
            log.info("未找到需要更新的计划");
            return 0;
        }

        LocalDate today = LocalDate.now();
        int count = 0;
        for (CxPrecisionPlan plan : plans) {
            LocalDate dueDate = toLocalDateFromUtil(plan.getDueDate());
            if (dueDate != null) {
                int daysToDue = (int) java.time.temporal.ChronoUnit.DAYS.between(today, dueDate);
                plan.setDaysToDue(daysToDue);
                cxPrecisionPlanMapper.updateById(plan);
                count++;
            }
        }
        log.info("批量更新到期天数完成，更新{}条", count);
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateActualDate(Long mesSourceId, String actualDate) {
        log.info("MES回传实际完成时间：MES_SOURCE_ID={}, ACTUAL_DATE={}", mesSourceId, actualDate);

        LambdaQueryWrapper<CxPrecisionPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CxPrecisionPlan::getId, mesSourceId);

        CxPrecisionPlan plan = cxPrecisionPlanMapper.selectOne(wrapper);
        if (plan == null) {
            log.warn("未找到ID={}的计划", mesSourceId);
            return false;
        }

        LocalDate actualDateParsed = parseDateString(actualDate);
        if (actualDateParsed == null) {
            log.warn("解析实际日期失败：{}", actualDate);
            return false;
        }

        Date actualDateUtil = Date.from(actualDateParsed.atStartOfDay(ZoneId.systemDefault()).toInstant());
        plan.setActualDate(actualDateUtil);
        plan.setDueDate(addMonths(actualDateUtil, PLAN_INTERVAL_MONTHS));

        int result = cxPrecisionPlanMapper.updateById(plan);
        if (result > 0) {
            log.info("更新实际完成时间成功：机台={}, 实际日期={}", plan.getMachineCode(), actualDateParsed);
            return true;
        }
        return false;
    }

    private LocalDate toLocalDateFromUtil(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private LocalDate parseDateString(String dateStr) {
        if (StringUtil.isBlank(dateStr)) {
            return null;
        }
        try {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd");
            return LocalDate.parse(dateStr, formatter);
        } catch (Exception e) {
            try {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
                return LocalDate.parse(dateStr, formatter);
            } catch (Exception ex) {
                log.warn("解析日期失败：{}", dateStr, ex);
                return null;
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generateFromMaintenancePlan(List<MdmDevMaintenancePlan> maintenancePlans, Integer cycleDays) {
        if (CollectionUtils.isEmpty(maintenancePlans)) {
            log.warn("设备保养计划列表为空");
            return 0;
        }

        log.info("开始根据设备保养计划生成成型精度计划，周期={}天，共{}条", cycleDays, maintenancePlans.size());

        List<CxPrecisionPlan> plansToSave = new ArrayList<>();

        for (MdmDevMaintenancePlan mesPlan : maintenancePlans) {
            String machineCode = mesPlan.getDevCode();

            Date operTime = mesPlan.getOperTime();
            Date firstWashTime = mesPlan.getFirstWashTime();

            if (firstWashTime == null) {
                log.warn("机台{}的实际执行时间为空，跳过", machineCode);
                continue;
            }

            CxPrecisionPlan plan = new CxPrecisionPlan();
            plan.setMachineCode(machineCode);
            plan.setFactoryCode(mesPlan.getFactoryCode());
//            plan.setPrecisionType("成型精度");
            plan.setCycleDays(cycleDays);

            if (operTime != null) {
                plan.setPlanDate(operTime);
                plan.setScheduleDate(operTime);
            }

            plan.setActualDate(firstWashTime);
            plan.setLastPrecisionDate(firstWashTime);

            LocalDate dueDateLocal = firstWashTime.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .plusDays(cycleDays);
            plan.setDueDate(Date.from(dueDateLocal.atStartOfDay(ZoneId.systemDefault()).toInstant()));

            plan.setEstimatedHours(new BigDecimal("4.0"));
            plan.setIsDelete(0);

            plansToSave.add(plan);
            log.info("准备生成成型精度计划：机台={}, 周期={}天, 计划日期={}", machineCode, cycleDays, plan.getPlanDate());
        }

        if (!plansToSave.isEmpty()) {
            baseDao.insertBatch(plansToSave);
            log.info("成功生成{}条成型精度计划（周期={}天）", plansToSave.size(), cycleDays);
        }

        return plansToSave.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int autoGenerateByCycle(Integer year, Integer cycleDays) {
        log.info("开始自动生成{}年度成型精度计划，周期={}天", year, cycleDays);

        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        List<CxPrecisionPlan> lastPlans = cxPrecisionPlanMapper.selectList(
            new LambdaQueryWrapper<CxPrecisionPlan>()
//                .eq(CxPrecisionPlan::getPrecisionType, "成型精度")
                .eq(CxPrecisionPlan::getCycleDays, cycleDays)
                .isNotNull(CxPrecisionPlan::getActualDate)
                .eq(CxPrecisionPlan::getIsDelete, 0)
                .orderByDesc(CxPrecisionPlan::getActualDate)
        );

        if (CollectionUtils.isEmpty(lastPlans)) {
            log.warn("未查询到周期为{}天的成型精度计划历史数据", cycleDays);
            return 0;
        }

        Map<String, CxPrecisionPlan> machinePlanMap = lastPlans.stream()
            .collect(Collectors.toMap(
                CxPrecisionPlan::getMachineCode,
                p -> p,
                (p1, p2) -> p1
            ));

        List<CxPrecisionPlan> plansToSave = new ArrayList<>();

        for (Map.Entry<String, CxPrecisionPlan> entry : machinePlanMap.entrySet()) {
            String machineCode = entry.getKey();
            CxPrecisionPlan lastPlan = entry.getValue();

            LocalDate lastActualDate = lastPlan.getActualDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

            LocalDate nextPlanDate = lastActualDate.plusDays(cycleDays);

            while (!nextPlanDate.isAfter(yearEnd)) {
                if (!nextPlanDate.isBefore(yearStart)) {
                    CxPrecisionPlan newPlan = new CxPrecisionPlan();
                    newPlan.setMachineCode(machineCode);
                    newPlan.setFactoryCode(lastPlan.getFactoryCode());
//                    newPlan.setPrecisionType("成型精度");
                    newPlan.setCycleDays(cycleDays);
                    newPlan.setPlanDate(Date.from(nextPlanDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
                    newPlan.setScheduleDate(Date.from(nextPlanDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
                    newPlan.setDueDate(Date.from(nextPlanDate.plusDays(cycleDays).atStartOfDay(ZoneId.systemDefault()).toInstant()));
                    newPlan.setLastPrecisionDate(lastPlan.getActualDate());
                    newPlan.setEstimatedHours(new BigDecimal("4.0"));
                    newPlan.setIsDelete(0);

                    plansToSave.add(newPlan);
                    log.info("准备自动生成成型精度计划：机台={}, 周期={}天, 计划日期={}", machineCode, cycleDays, nextPlanDate);
                }

                nextPlanDate = nextPlanDate.plusDays(cycleDays);
            }
        }

        if (!plansToSave.isEmpty()) {
            baseDao.insertBatch(plansToSave);
            log.info("成功自动生成{}条成型精度计划（周期={}天）", plansToSave.size(), cycleDays);
        }

        return plansToSave.size();
    }
}

