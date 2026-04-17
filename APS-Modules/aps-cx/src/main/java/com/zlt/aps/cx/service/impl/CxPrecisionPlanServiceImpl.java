package com.zlt.aps.cx.service.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import com.zlt.aps.maindata.mapper.MdmMoldingMachineEntityMapper;
import com.zlt.aps.maindata.utils.MessageServiceUtils;
import com.zlt.aps.mp.api.domain.entity.MdmDevMaintenancePlan;

import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachine;
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
import java.util.*;
import java.util.stream.Collectors;

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

    @Autowired
    private MdmMoldingMachineEntityMapper mdmMoldingMachineEntityMapper;

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
            Long daysToDue = DateUtil.betweenDay(DateUtil.date(), entity.getPlanDate(), true);
            entity.setDaysToDue(daysToDue);
            if (entity.getYear() == null) {
                entity.setYear(new BigDecimal(DateUtil.year(entity.getPlanDate())));
            }
        }
    }

    @Override
    public String checkUnique(CxPrecisionPlan entity) {
        if (entity == null || StringUtil.isBlank(entity.getMachineCode()) || entity.getPlanDate() == null
            || StringUtil.isBlank(entity.getPrecisionType())) {
            return UserConstants.UNIQUE;
        }

        CxPrecisionPlan existEntity = selectExistingPlan(entity.getMachineCode(), entity.getPrecisionType(),
            entity.getPlanDate(), entity.getId());
        return existEntity != null ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
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
        Map<String, MdmMoldingMachine> machineMap = buildMachineMap(list);

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            CxPrecisionPlan docEntity = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated,
                "machineCode", "precisionType", "planDate");
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
            if (!machineMap.containsKey(docEntity.getMachineCode())) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.data.alert.cxPrecisionPlan.machineNotExist");
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

            CxPrecisionPlan existEntity = selectExistingPlan(docEntity.getMachineCode(), docEntity.getPrecisionType(),
                docEntity.getPlanDate(), null);
            if (existEntity == null) {
                initImportEntity(docEntity);
                importList.add(docEntity);
                successNum++;
            } else {
                if (updateSupport) {
                    docEntity.setId(existEntity.getId());
                    initImportEntity(docEntity);
                    importList.add(docEntity);
                    successNum++;
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

    /**
     * 按机台编码、精度类型、计划日期查询已存在的精度计划。
     */
    private CxPrecisionPlan selectExistingPlan(String machineCode, String precisionType, Date planDate, Long excludeId) {
        if (StringUtil.isBlank(machineCode) || StringUtil.isBlank(precisionType) || planDate == null) {
            return null;
        }
        QueryWrapper<CxPrecisionPlan> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("MACHINE_CODE", machineCode);
        queryWrapper.eq("PRECISION_TYPE", precisionType);
        queryWrapper.eq("PLAN_DATE", DateUtil.beginOfDay(planDate));
        queryWrapper.eq("IS_DELETE", 0);
        if (excludeId != null) {
            queryWrapper.ne("ID", excludeId);
        }
        queryWrapper.last("limit 1");
        return cxPrecisionPlanMapper.selectOne(queryWrapper);
    }

    /**
     * 初始化导入数据的默认值，保持导入新增与覆盖时的字段一致性。
     */
    private void initImportEntity(CxPrecisionPlan docEntity) {
        docEntity.setPlanDate(DateUtil.beginOfDay(docEntity.getPlanDate()));
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
    }

    /**
     * 批量查询导入数据涉及的机台主数据，避免逐行访问数据库。
     */
    private Map<String, MdmMoldingMachine> buildMachineMap(List<CxPrecisionPlan> list) {

        List<String>  machineCodes = list.stream().map(CxPrecisionPlan::getMachineCode)
                .distinct()
                .collect(Collectors.toList());
        List<List<String>> machineCodeList = ListUtil.partition(machineCodes, 1000);
        LambdaUpdateWrapper<MdmMoldingMachine> queryWrapper = new LambdaUpdateWrapper<>();
        queryWrapper.and(i ->{
            for(List<String> newList: machineCodeList){
                i.or().in(MdmMoldingMachine::getCxMachineCode, newList);
            }
        });
        List<MdmMoldingMachine> machineList = mdmMoldingMachineEntityMapper.selectList(queryWrapper);
        Map<String, MdmMoldingMachine> machineMap = machineList.stream()
                .collect(Collectors.toMap(MdmMoldingMachine::getCxMachineCode, i -> i));
        return machineMap;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 从 MES 数据生成年度成型精度计划（带分布式锁）。
     */
    public int generatePlansFromMes(Integer year) {
        Integer targetYear = year == null ? DateUtil.thisYear() : year;
        String lockKey = "generate:cx:precision:plan:" + targetYear;
        if (redisService.getCacheObject(lockKey) != null) {
            throw new RuntimeException(I18nUtil.getMessage("ui.lh.precisionPlan.generate.in.progress"));
        }

        try {
            redisService.setCacheObject(lockKey, "1");
            log.info("开始从MES同步数据生成{}年度成型精度计划", targetYear);

            // 查询历史同步时间，用于增量同步
            LambdaQueryWrapper<CxPrecisionPlan> syncWrapper = new LambdaQueryWrapper<>();
            syncWrapper.isNotNull(CxPrecisionPlan::getSyncTime)
                .eq(CxPrecisionPlan::getDataSource, DATA_SOURCE_AUTO)
                .eq(CxPrecisionPlan::getIsDelete, 0)
                .orderByDesc(CxPrecisionPlan::getSyncTime)
                .last("limit 1");
            CxPrecisionPlan latestPlan = cxPrecisionPlanMapper.selectOne(syncWrapper);
            Date lastSyncTime = latestPlan != null ? latestPlan.getSyncTime() : null;

            LambdaQueryWrapper<MdmDevMaintenancePlan> wrapper = new LambdaQueryWrapper<>();
            wrapper.like(MdmDevMaintenancePlan::getPrecisionType, PRECISION_TYPE_CX)
                .eq(MdmDevMaintenancePlan::getIsDelete, 0);
            // 增量同步：只查询updateTime大于等于lastSyncTime的数据（避免漏同步同一时刻的记录）
            if (lastSyncTime != null) {
                wrapper.ge(MdmDevMaintenancePlan::getUpdateTime, lastSyncTime);
            }

            List<MdmDevMaintenancePlan> mesPlans = mdmDevMaintenancePlanEntityMapper.selectList(wrapper);
            if (CollectionUtils.isEmpty(mesPlans)) {
                log.warn("MES成型精度数据为空");
                return 0;
            }

            // 按机台增量处理：先锁定本次变更涉及的机台，再查询每个机台最新的一条MES记录，避免“旧记录被更新”导致计划回退
            Set<String> machineCodes = new HashSet<>();
            for (MdmDevMaintenancePlan mesPlan : mesPlans) {
                if (StringUtils.hasText(mesPlan.getDevCode())) {
                    machineCodes.add(mesPlan.getDevCode());
                }
            }

            Map<String, MdmDevMaintenancePlan> latestMesPlanMap = new HashMap<>();
            for (String machineCode : machineCodes) {
                LambdaQueryWrapper<MdmDevMaintenancePlan> latestWrapper = new LambdaQueryWrapper<>();
                latestWrapper.eq(MdmDevMaintenancePlan::getDevCode, machineCode)
                    .like(MdmDevMaintenancePlan::getPrecisionType, PRECISION_TYPE_CX)
                    .eq(MdmDevMaintenancePlan::getIsDelete, 0)
                    .isNotNull(MdmDevMaintenancePlan::getFirstWashTime)
                    .orderByDesc(MdmDevMaintenancePlan::getFirstWashTime)
                    .orderByDesc(MdmDevMaintenancePlan::getUpdateTime)
                    .last("limit 1");
                MdmDevMaintenancePlan latest = mdmDevMaintenancePlanEntityMapper.selectOne(latestWrapper);
                if (latest != null && StringUtils.hasText(latest.getDevCode())) {
                    latestMesPlanMap.put(latest.getDevCode(), latest);
                }
            }

            int affected = 0;
            for (Map.Entry<String, MdmDevMaintenancePlan> entry : latestMesPlanMap.entrySet()) {
                CxPrecisionPlan plan = createPlanFromMes(entry.getKey(), entry.getValue(), targetYear);
                if (plan != null) {
                    affected += upsertByMesSourceId(plan);
                }
            }

            return affected;
        } finally {
            redisService.deleteObject(lockKey);
        }
    }

    /**
     * 按MES来源ID增量同步：mesSourceId 对应 {@link MdmDevMaintenancePlan} 的 id，有则更新，无则新增。
     */
    private int upsertByMesSourceId(CxPrecisionPlan plan) {
        if (plan.getMesSourceId() == null) {
            return 0;
        }
        LambdaQueryWrapper<CxPrecisionPlan> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(CxPrecisionPlan::getMesSourceId, plan.getMesSourceId());
        List<CxPrecisionPlan> existList = cxPrecisionPlanMapper.selectList(existWrapper);
        if (PubUtil.isEmpty(existList)) {
            cxPrecisionPlanMapper.insert(plan);
            return 1;
        }
        for (CxPrecisionPlan cxPrecisionPlan : existList) {
            applyMesFields(cxPrecisionPlan, plan);
            cxPrecisionPlanMapper.updateById(cxPrecisionPlan);
        }
        return 1;
    }

    /**
     * 仅更新从MES同步得到的字段，避免覆盖用户维护的完成/预警等状态。
     */
    private void applyMesFields(CxPrecisionPlan target, CxPrecisionPlan source) {
        target.setMachineCode(source.getMachineCode());
        target.setPrecisionType(source.getPrecisionType());
        target.setPrecisionCycle(source.getPrecisionCycle());
        target.setCompanyCode(source.getCompanyCode());
        target.setFactoryCode(source.getFactoryCode());
        target.setMesSourceId(source.getMesSourceId());
        target.setDataSource(source.getDataSource());
        target.setSyncTime(source.getSyncTime());
        target.setActualDate(source.getActualDate());
        target.setLastMaintenanceDate(source.getLastMaintenanceDate());
        target.setPlanDate(source.getPlanDate());
        target.setDueDate(source.getDueDate());
        target.setYear(source.getYear());
        target.setDaysToDue(source.getDaysToDue());
        target.setIsDelete(0);

        if (StringUtil.isBlank(target.getCompletionStatus())) {
            target.setCompletionStatus(COMPLETION_STATUS_PENDING);
        }
        if (StringUtil.isBlank(target.getWarningStatus())) {
            target.setWarningStatus(WARNING_STATUS_NO);
        }
        if (StringUtil.isBlank(target.getIsWarningSent())) {
            target.setIsWarningSent(WARNING_SENT_NO);
        }
    }

    /**
     * 将 MES 记录转换为成型精度计划实体。
     */
    private CxPrecisionPlan createPlanFromMes(String machineCode, MdmDevMaintenancePlan mesPlan, Integer year) {
        Date actualDate = parseDate(mesPlan.getFirstWashTime());
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
        plan.setSyncTime(mesPlan.getUpdateTime());
        plan.setActualDate(actualDate);
        plan.setLastMaintenanceDate(actualDate);

        Integer cycleDays = safeParseInt(plan.getPrecisionCycle());
        if (cycleDays == null) {
            return null;
        }
        Date planDate = DateUtil.offsetDay(actualDate, cycleDays);
        plan.setPlanDate(planDate);
        plan.setDueDate(planDate);
        plan.setYear(new BigDecimal(year));
        plan.setDaysToDue(DateUtil.betweenDay(DateUtil.date(), planDate, true));

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
        Date lastActualDate = lastPlan.getActualDate();
        if (lastActualDate == null) {
            return null;
        }

        CxPrecisionPlan plan = new CxPrecisionPlan();
        plan.setMachineCode(machineCode);
        plan.setPrecisionType(StringUtils.hasText(lastPlan.getPrecisionType()) ? lastPlan.getPrecisionType() : PRECISION_TYPE_CX);
        plan.setDataSource(DATA_SOURCE_AUTO);
        plan.setFactoryCode(lastPlan.getFactoryCode());
        plan.setCompanyCode(lastPlan.getCompanyCode());

        Date planDate = DateUtil.offsetDay(lastActualDate, intervalDays);
        plan.setPlanDate(planDate);
        plan.setDueDate(planDate);
        plan.setYear(new BigDecimal(year));
        plan.setLastMaintenanceDate(lastActualDate);
        plan.setDaysToDue(DateUtil.betweenDay(DateUtil.date(), planDate, true));

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

        Date now = DateUtil.beginOfDay(DateUtil.date());
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

        Date actualDateParsed = parseDate(actualDate);
        if (actualDateParsed == null) {
            return false;
        }

        plan.setBaseVale(plan.getId());
        plan.setActualDate(actualDateParsed);
        plan.setCompletionStatus(COMPLETION_STATUS_COMPLETED);
        plan.setDueDate(DateUtil.offsetYear(actualDateParsed, 1));

        int result = cxPrecisionPlanMapper.updateById(plan);
        return result > 0;
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
//            plan.setCycleDays(cycleDays);

            if (operTime != null) {
                plan.setPlanDate(operTime);
                plan.setScheduleDate(operTime);
            }

            plan.setActualDate(firstWashTime);
//            plan.setLastPrecisionDate(firstWashTime);

            LocalDate dueDateLocal = firstWashTime.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .plusDays(cycleDays);
            plan.setDueDate(Date.from(dueDateLocal.atStartOfDay(ZoneId.systemDefault()).toInstant()));

//            plan.setEstimatedHours(new BigDecimal("4.0"));
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
//                        .eq(CxPrecisionPlan::getCycleDays, cycleDays)
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
//                    newPlan.setCycleDays(cycleDays);
                    newPlan.setPlanDate(Date.from(nextPlanDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
                    newPlan.setScheduleDate(Date.from(nextPlanDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
                    newPlan.setDueDate(Date.from(nextPlanDate.plusDays(cycleDays).atStartOfDay(ZoneId.systemDefault()).toInstant()));
//                    newPlan.setLastPrecisionDate(lastPlan.getActualDate());
//                    newPlan.setEstimatedHours(new BigDecimal("4.0"));
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



    /**
     * 解析字符串日期（支持 yyyy/MM/dd 与 yyyy-MM-dd）。
     */
    private Date parseDate(String dateStr) {
        if (!StringUtils.hasText(dateStr)) {
            return null;
        }

        try {
            Date parsed = DateUtil.parse(dateStr, "yyyy/MM/dd");
            return DateUtil.beginOfDay(parsed);
        } catch (Exception e) {
            try {
                Date parsed = DateUtil.parse(dateStr, "yyyy-MM-dd");
                return DateUtil.beginOfDay(parsed);
            } catch (Exception ex) {
                log.warn("解析日期失败: {}", dateStr, ex);
                return null;
            }
        }
    }

    /**
     * 解析 Date 为 LocalDate。
     */
    private Date parseDate(Date date) {
        if (date == null) {
            return null;
        }
        try {
            return DateUtil.beginOfDay(date);
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
            String planDateStr = plan.getPlanDate() == null ? "" : DateUtil.format(plan.getPlanDate(), "yyyy-MM-dd");
            messageServiceUtils.sendWarning(templateCode, (String) null,
                plan.getMachineCode(),
                planDateStr,
                plan.getDaysToDue());
        } catch (Exception e) {
            log.error("发送成型精度预警失败: machineCode={}, planDate={}", plan.getMachineCode(), plan.getPlanDate(), e);
        }
    }

    private Integer safeParseInt(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
