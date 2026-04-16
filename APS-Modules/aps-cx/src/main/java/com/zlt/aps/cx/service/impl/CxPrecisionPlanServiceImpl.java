package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.redis.service.RedisService;
import com.zlt.aps.cx.api.domain.entity.CxPrecisionPlan;
import com.zlt.aps.cx.mapper.CxPrecisionPlanMapper;
import com.zlt.aps.cx.service.ICxPrecisionPlanService;
import com.zlt.aps.maindata.enums.MsgTemplateEnums;
import com.zlt.aps.maindata.mapper.MdmDevMaintenancePlanEntityMapper;
import com.zlt.aps.maindata.utils.MessageServiceUtils;
import com.zlt.aps.mp.api.domain.entity.MdmDevMaintenancePlan;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
/**
 * 成型精度计划服务实现。
 * <p>
 * 负责成型精度计划的导入、生成、预警、回传等业务逻辑。
 */
public class CxPrecisionPlanServiceImpl extends AbstractDocService<CxPrecisionPlan> implements ICxPrecisionPlanService {

    /** 成型精度类型 */
    private static final String PRECISION_TYPE_CX = "成型精度";
    /** 完成状态：未完成 */
    private static final String COMPLETION_STATUS_PENDING = "0";
    /** 完成状态：已完成 */
    private static final String COMPLETION_STATUS_COMPLETED = "1";
    /** 预警状态：未预警 */
    private static final String WARNING_STATUS_NO = "0";
    /** 预警状态：已预警 */
    private static final String WARNING_STATUS_YES = "1";
    /** 预警发送状态：未发送 */
    private static final String WARNING_SENT_NO = "0";
    /** 预警发送状态：已发送 */
    private static final String WARNING_SENT_YES = "1";
    /** 数据来源：MES同步 */
    private static final String DATA_SOURCE_MES = "0";
    /** 数据来源：系统自动生成 */
    private static final String DATA_SOURCE_AUTO = "1";
    /** 预警阈值（天） */
    private static final Integer WARNING_DAYS = 30;
    /** 默认计划间隔（天） */
    private static final Integer DEFAULT_INTERVAL_DAYS = 365;

    @Autowired
    private CxPrecisionPlanMapper cxPrecisionPlanMapper;

    @Autowired
    private MdmDevMaintenancePlanEntityMapper mdmDevMaintenancePlanEntityMapper;

    @Autowired
    private MessageServiceUtils messageServiceUtils;

    @Autowired
    private RedisService redisService;

    @Override
    protected String getDocTypeCode() {
        return "CX_PRECISION_PLAN";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("CX_PRECISION_PLAN");
        return sysDocType;
    }

    @Override
    public int save(CxPrecisionPlan entity) {
        calculateDaysToDue(entity);
        return super.save(entity);
    }

    /**
     * 计算到期剩余天数；若年度为空则按计划日期回填年度。
     */
    private void calculateDaysToDue(CxPrecisionPlan entity) {
        if (entity.getPlanDate() != null) {
            LocalDate today = LocalDate.now();
            int daysToDue = (int) ChronoUnit.DAYS.between(today, entity.getPlanDate());
            entity.setDaysToDue(daysToDue);
            if (entity.getYear() == null) {
                entity.setYear(new BigDecimal(entity.getPlanDate().getYear()));
            }
        }
    }

    @Override
    public String checkUnique(CxPrecisionPlan entity) {
        if (entity == null || StringUtil.isBlank(entity.getMachineCode()) || entity.getPlanDate() == null) {
            return UserConstants.UNIQUE;
        }

        QueryWrapper<CxPrecisionPlan> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("MACHINE_CODE", entity.getMachineCode());
        queryWrapper.eq("PLAN_DATE", entity.getPlanDate());
        queryWrapper.eq("IS_DELETE", 0);

        if (entity.getId() != null) {
            queryWrapper.ne("ID", entity.getId());
        }

        long count = cxPrecisionPlanMapper.selectCount(queryWrapper);
        return count > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 导入成型精度计划数据，支持可选覆盖更新。
     */
    public AjaxResult importData(List<CxPrecisionPlan> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<CxPrecisionPlan> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");

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

            if (checkUnique(docEntity).equals(UserConstants.UNIQUE)) {
                docEntity.setIsDelete(0);
                if (StringUtil.isBlank(docEntity.getCompletionStatus())) {
                    docEntity.setCompletionStatus(COMPLETION_STATUS_PENDING);
                }
                if (StringUtil.isBlank(docEntity.getWarningStatus())) {
                    docEntity.setWarningStatus(WARNING_STATUS_NO);
                }
                if (StringUtil.isBlank(docEntity.getIsWarningSent())) {
                    docEntity.setIsWarningSent(WARNING_SENT_NO);
                }
                if (StringUtil.isBlank(docEntity.getDataSource())) {
                    docEntity.setDataSource(DATA_SOURCE_MES);
                }
                calculateDaysToDue(docEntity);
                importList.add(docEntity);
                successNum++;
            } else {
                if (updateSupport) {
                    QueryWrapper<CxPrecisionPlan> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("MACHINE_CODE", docEntity.getMachineCode());
                    queryWrapper.eq("PLAN_DATE", docEntity.getPlanDate());
                    queryWrapper.eq("IS_DELETE", 0);
                    CxPrecisionPlan existEntity = cxPrecisionPlanMapper.selectOne(queryWrapper);
                    if (existEntity != null) {
                        docEntity.setId(existEntity.getId());
                        docEntity.setIsDelete(0);
                        calculateDaysToDue(docEntity);
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
        }
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum + "," + failureNum);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 从 MES 数据生成年度成型精度计划（带分布式锁）。
     */
    public int generatePlansFromMes(Integer year) {
        Integer targetYear = year == null ? LocalDate.now().getYear() : year;
        String lockKey = "generate:cx:precision:plan:" + targetYear;
        if (redisService.getCacheObject(lockKey) != null) {
            throw new RuntimeException(I18nUtil.getMessage("ui.lh.precisionPlan.generate.in.progress"));
        }

        try {
            redisService.setCacheObject(lockKey, "1");
            log.info("开始从MES同步数据生成{}年度成型精度计划", targetYear);

            LambdaQueryWrapper<MdmDevMaintenancePlan> wrapper = new LambdaQueryWrapper<>();
            wrapper.like(MdmDevMaintenancePlan::getPrecisionType, PRECISION_TYPE_CX)
                .eq(MdmDevMaintenancePlan::getIsDelete, 0);

            List<MdmDevMaintenancePlan> mesPlans = mdmDevMaintenancePlanEntityMapper.selectList(wrapper);
            if (CollectionUtils.isEmpty(mesPlans)) {
                log.warn("MES成型精度数据为空");
                return 0;
            }

            QueryWrapper<CxPrecisionPlan> deleteWrapper = new QueryWrapper<>();
            deleteWrapper.eq("YEAR", targetYear)
                .eq("DATA_SOURCE", DATA_SOURCE_AUTO)
                .eq("IS_DELETE", 0);
            cxPrecisionPlanMapper.delete(deleteWrapper);

            Map<String, MdmDevMaintenancePlan> latestMesPlanMap = new HashMap<>();
            for (MdmDevMaintenancePlan mesPlan : mesPlans) {
                String machineCode = mesPlan.getDevCode();
                LocalDate actualDate = parseDate(mesPlan.getFirstWashTime());
                if (!StringUtils.hasText(machineCode) || actualDate == null) {
                    continue;
                }
                MdmDevMaintenancePlan exist = latestMesPlanMap.get(machineCode);
                LocalDate existDate = exist == null ? null : parseDate(exist.getFirstWashTime());
                if (existDate == null || actualDate.isAfter(existDate)) {
                    latestMesPlanMap.put(machineCode, mesPlan);
                }
            }

            int intervalDays = getIntervalDays();
            List<CxPrecisionPlan> plansToSave = new ArrayList<>();
            for (Map.Entry<String, MdmDevMaintenancePlan> entry : latestMesPlanMap.entrySet()) {
                CxPrecisionPlan plan = createPlanFromMes(entry.getKey(), entry.getValue(), targetYear, intervalDays);
                if (plan != null) {
                    plansToSave.add(plan);
                }
            }

            if (!plansToSave.isEmpty()) {
                baseDao.insertBatch(plansToSave);
            }

            return plansToSave.size();
        } finally {
            redisService.deleteObject(lockKey);
        }
    }

    /**
     * 将 MES 记录转换为成型精度计划实体。
     */
    private CxPrecisionPlan createPlanFromMes(String machineCode, MdmDevMaintenancePlan mesPlan, Integer year, int intervalDays) {
        LocalDate actualDate = parseDate(mesPlan.getFirstWashTime());
        if (actualDate == null) {
            return null;
        }

        CxPrecisionPlan plan = new CxPrecisionPlan();
        plan.setMachineCode(machineCode);
        plan.setPrecisionType(extractNumberFromPrecisionType(mesPlan.getPrecisionType()));
        plan.setPrecisionCycle(extractNumberFromPrecisionType(mesPlan.getPrecisionType()));
        plan.setCompanyCode(mesPlan.getCompanyCode());
        plan.setFactoryCode(mesPlan.getFactoryCode());
        plan.setMesSourceId(mesPlan.getId());
        plan.setDataSource(DATA_SOURCE_AUTO);
        plan.setActualDate(actualDate);
        plan.setLastMaintenanceDate(actualDate);

        LocalDate planDate = actualDate.plusDays(intervalDays);
        plan.setPlanDate(planDate);
        plan.setDueDate(planDate);
        plan.setYear(new BigDecimal(year));
        plan.setDaysToDue((int) ChronoUnit.DAYS.between(LocalDate.now(), planDate));

        plan.setCompletionStatus(COMPLETION_STATUS_PENDING);
        plan.setWarningStatus(WARNING_STATUS_NO);
        plan.setIsWarningSent(WARNING_SENT_NO);
        plan.setIsDelete(0);
        plan.setBaseVale(null);
        return plan;
    }

    /**
     * 从精度类型中提取数字作为周期。
     * 例如："15天精度" -> "15", "60天精度" -> "60"
     */
    private String extractNumberFromPrecisionType(String precisionType) {
        if (!StringUtils.hasText(precisionType)) {
            return null;
        }
        return precisionType.replaceAll("[^0-9]", "");
    }
    /**
     * 获取计划间隔天数。
     */
    private int getIntervalDays() {
        return DEFAULT_INTERVAL_DAYS;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 根据历史完成记录自动生成年度计划。
     */
    public int autoGenerateYearlyPlans(Integer year) {
        log.info("开始自动生成{}年度成型精度计划", year);

        LambdaQueryWrapper<CxPrecisionPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CxPrecisionPlan::getCompletionStatus, COMPLETION_STATUS_COMPLETED)
            .isNotNull(CxPrecisionPlan::getActualDate)
            .orderByDesc(CxPrecisionPlan::getActualDate);

        List<CxPrecisionPlan> lastYearPlans = cxPrecisionPlanMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(lastYearPlans)) {
            return 0;
        }

        Map<String, CxPrecisionPlan> machinePlanMap = new HashMap<>();
        for (CxPrecisionPlan plan : lastYearPlans) {
            if (StringUtils.hasText(plan.getMachineCode()) && !machinePlanMap.containsKey(plan.getMachineCode())) {
                machinePlanMap.put(plan.getMachineCode(), plan);
            }
        }

        int intervalDays = getIntervalDays();
        List<CxPrecisionPlan> plansToSave = new ArrayList<>();
        for (Map.Entry<String, CxPrecisionPlan> entry : machinePlanMap.entrySet()) {
            String machineCode = entry.getKey();
            CxPrecisionPlan existingPlan = selectByMachineCodeAndYear(machineCode, year);
            if (existingPlan != null) {
                continue;
            }

            CxPrecisionPlan newPlan = createYearlyPlan(machineCode, entry.getValue(), year, intervalDays);
            if (newPlan != null) {
                plansToSave.add(newPlan);
            }
        }

        for (CxPrecisionPlan plan : plansToSave) {
            cxPrecisionPlanMapper.insert(plan);
        }

        return plansToSave.size();
    }

    /**
     * 查询机台在目标年度是否已有计划。
     */
    private CxPrecisionPlan selectByMachineCodeAndYear(String machineCode, Integer year) {
        QueryWrapper<CxPrecisionPlan> wrapper = new QueryWrapper<>();
        wrapper.eq("MACHINE_CODE", machineCode)
            .eq("YEAR", year)
            .eq("IS_DELETE", 0)
            .last("limit 1");
        return cxPrecisionPlanMapper.selectOne(wrapper);
    }

    /**
     * 基于上一条计划生成新的年度计划。
     */
    private CxPrecisionPlan createYearlyPlan(String machineCode, CxPrecisionPlan lastPlan, Integer year, int intervalDays) {
        LocalDate lastActualDate = lastPlan.getActualDate();
        if (lastActualDate == null) {
            return null;
        }

        CxPrecisionPlan plan = new CxPrecisionPlan();
        plan.setMachineCode(machineCode);
        plan.setPrecisionType(StringUtils.hasText(lastPlan.getPrecisionType()) ? lastPlan.getPrecisionType() : PRECISION_TYPE_CX);
        plan.setDataSource(DATA_SOURCE_AUTO);
        plan.setFactoryCode(lastPlan.getFactoryCode());
        plan.setCompanyCode(lastPlan.getCompanyCode());

        LocalDate planDate = lastActualDate.plusDays(intervalDays);
        plan.setPlanDate(planDate);
        plan.setDueDate(planDate);
        plan.setYear(new BigDecimal(year));
        plan.setLastMaintenanceDate(lastActualDate);
        plan.setDaysToDue((int) ChronoUnit.DAYS.between(LocalDate.now(), planDate));

        plan.setCompletionStatus(COMPLETION_STATUS_PENDING);
        plan.setWarningStatus(WARNING_STATUS_NO);
        plan.setIsWarningSent(WARNING_SENT_NO);
        plan.setIsDelete(0);
        plan.setBaseVale(null);
        return plan;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 执行预警检查并发送预警消息。
     */
    public int checkWarning() {
        List<CxPrecisionPlan> plans = cxPrecisionPlanMapper.selectPendingWarningPlans(WARNING_DAYS);
        if (CollectionUtils.isEmpty(plans)) {
            return 0;
        }

        LocalDate now = LocalDate.now();
        for (CxPrecisionPlan plan : plans) {
            plan.setBaseVale(plan.getId());
            plan.setWarningStatus(WARNING_STATUS_YES);
            plan.setWarningDate(now);
            plan.setIsWarningSent(WARNING_SENT_YES);
            sendWarning(plan);
        }

        for (CxPrecisionPlan plan : plans) {
            cxPrecisionPlanMapper.updateById(plan);
        }
        return plans.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 批量刷新未完成计划的到期剩余天数。
     */
    public int batchUpdateDaysToDue() {
        return cxPrecisionPlanMapper.batchUpdateDaysToDue();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 处理 MES 回传的实际完成日期并更新计划状态。
     */
    public boolean updateActualDate(Long mesSourceId, String actualDate) {
        LambdaQueryWrapper<CxPrecisionPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CxPrecisionPlan::getMesSourceId, mesSourceId)
            .eq(CxPrecisionPlan::getIsDelete, 0);

        CxPrecisionPlan plan = cxPrecisionPlanMapper.selectOne(wrapper);
        if (plan == null) {
            return false;
        }

        LocalDate actualDateParsed = parseDate(actualDate);
        if (actualDateParsed == null) {
            return false;
        }

        plan.setBaseVale(plan.getId());
        plan.setActualDate(actualDateParsed);
        plan.setCompletionStatus(COMPLETION_STATUS_COMPLETED);
        plan.setDueDate(actualDateParsed.plusYears(1));

        int result = cxPrecisionPlanMapper.updateById(plan);
        return result > 0;
    }

    /**
     * 解析字符串日期（支持 yyyy/MM/dd 与 yyyy-MM-dd）。
     */
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
                log.warn("解析日期失败: {}", dateStr, ex);
                return null;
            }
        }
    }

    /**
     * 解析 Date 为 LocalDate。
     */
    private LocalDate parseDate(Date date) {
        if (date == null) {
            return null;
        }
        try {
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        } catch (Exception e) {
            log.warn("解析Date失败: {}", date, e);
            return null;
        }
    }

    /**
     * 发送预警消息通知。
     */
    private void sendWarning(CxPrecisionPlan plan) {
        try {
            String templateCode = MsgTemplateEnums.LH_PRECISION_PLAN_WARNING.getCode();
            String planDateStr = plan.getPlanDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            messageServiceUtils.sendWarning(templateCode, (String) null,
                plan.getMachineCode(),
                planDateStr,
                plan.getDaysToDue());
        } catch (Exception e) {
            log.error("发送成型精度预警失败: machineCode={}, planDate={}", plan.getMachineCode(), plan.getPlanDate(), e);
        }
    }
}
