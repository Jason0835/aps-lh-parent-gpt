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
import java.util.stream.Collectors;

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
            LocalDate planDate = parseDate(entity.getPlanDate());
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
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");
        String excelDuplicateMsg = I18nUtil.getMessage("ui.lh.precision.plan.excelDuplicate");

        // 1. 校验必填项 + Excel内部数据重复校验
        Set<String> excelUniqueSet = new HashSet<>();
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            LhPrecisionPlanImportVO importVO = list.get(i);

            if (StringUtil.isBlank(importVO.getFactoryCode())) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.lh.precision.plan.factoryCodeRequired");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                    errorNum, String.format(message, errorNum), importErrorLogs);
                importVO.setRowNum(-999);
                continue;
            }

            if (StringUtil.isBlank(importVO.getMachineCode())) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.lh.precision.plan.machineCodeRequired");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                    errorNum, String.format(message, errorNum), importErrorLogs);
                importVO.setRowNum(-999);
                continue;
            }

            if (importVO.getPlanDate() == null) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.lh.precision.plan.planDateRequired");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                    errorNum, String.format(message, errorNum), importErrorLogs);
                importVO.setRowNum(-999);
                continue;
            }

            if (importVO.getActualDate() == null) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.lh.precision.plan.actualDateRequired");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                    errorNum, String.format(message, errorNum), importErrorLogs);
                importVO.setRowNum(-999);
                continue;
            }

            // Excel内部重复校验：工厂+硫化机台+计划日期
            String uniqueKey = importVO.getFactoryCode() + "-" + importVO.getMachineCode() + "-" + importVO.getPlanDate();
            if (excelUniqueSet.contains(uniqueKey)) {
                failureNum++;
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                    errorNum, String.format(excelDuplicateMsg, errorNum), importErrorLogs);
                importVO.setRowNum(-999);
                continue;
            }
            excelUniqueSet.add(uniqueKey);
        }

        // 2. 数据库重复校验（工厂+硫化机台+计划日期）
        List<String> validFactoryCodes = list.stream()
                .filter(vo -> vo.getRowNum() == null || vo.getRowNum() != -999)
                .map(LhPrecisionPlanImportVO::getFactoryCode)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<String> validMachineCodes = list.stream()
                .filter(vo -> vo.getRowNum() == null || vo.getRowNum() != -999)
                .map(LhPrecisionPlanImportVO::getMachineCode)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<Date> validPlanDates = list.stream()
                .filter(vo -> vo.getRowNum() == null || vo.getRowNum() != -999)
                .map(LhPrecisionPlanImportVO::getPlanDate)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<String, List<LhPrecisionPlan>> dbExistMap = new HashMap<>();
        if (!validFactoryCodes.isEmpty() && !validMachineCodes.isEmpty() && !validPlanDates.isEmpty()) {
            List<LhPrecisionPlan> dbExistList = lhPrecisionPlanMapper.selectByFactoryMachinePlanBatch(validFactoryCodes, validMachineCodes, validPlanDates);
            dbExistMap = dbExistList.stream()
                    .collect(Collectors.groupingBy(p -> p.getFactoryCode() + "-" + p.getMachineCode() + "-" + parseDate(p.getPlanDate()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
        }

        List<Long> idsToDelete = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            LhPrecisionPlanImportVO importVO = list.get(i);

            if (importVO.getRowNum() != null && importVO.getRowNum() == -999) {
                continue;
            }

            BigDecimal year = new BigDecimal(parseDate(importVO.getPlanDate()).getYear());

            String dbKey = importVO.getFactoryCode() + "-" + importVO.getMachineCode() + "-" + parseDate(importVO.getPlanDate()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            List<LhPrecisionPlan> existList = dbExistMap.getOrDefault(dbKey, Collections.emptyList());

            if (!existList.isEmpty()) {
                if (!updateSupport) {
                    failureNum++;
                    ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, String.format(uniqueMsg, errorNum), importErrorLogs);
                    continue;
                } else {
                    for (LhPrecisionPlan exist : existList) {
                        idsToDelete.add(exist.getId());
                    }
                }
            }
            
            // 转换为实体对象
            LhPrecisionPlan entity = new LhPrecisionPlan();
            entity.setFactoryCode(importVO.getFactoryCode());
            entity.setMachineCode(importVO.getMachineCode());
            entity.setPrecisionType("精度计划");
            entity.setPlanDate(importVO.getPlanDate());
            entity.setActualDate(importVO.getActualDate());
            entity.setRemark(importVO.getRemark());
            entity.setIsDelete(0);
            entity.setWarningStatus(WARNING_STATUS_NO);
            entity.setIsWarningSent(WARNING_SENT_NO);
            entity.setCompletionStatus(COMPLETION_STATUS_PENDING);
            entity.setDataSource(DATA_SOURCE_AUTO);
            entity.setYear(year);
            calculateDaysToDue(entity);
            
            importList.add(entity);
            successNum++;
        }

        if (!idsToDelete.isEmpty()) {
            lhPrecisionPlanMapper.deleteBatchIds(idsToDelete);
            log.info("导入时批量删除已存在数据{}条", idsToDelete.size());
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

            // 查询APS本地表中硫化精度类型的最大版本号，只处理最新版本的数据
            String maxVersion = mdmDevMaintenancePlanEntityMapper.selectMaxDataVersion(PRECISION_TYPE_LH);
            if (maxVersion == null || maxVersion.isEmpty()) {
                log.warn("APS本地表中无硫化精度版本数据，跳过处理");
                return 0;
            }
            log.info("硫化精度最新版本号：{}", maxVersion);

            LambdaQueryWrapper<MdmDevMaintenancePlan> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MdmDevMaintenancePlan::getPrecisionType, PRECISION_TYPE_LH)
                   .eq(MdmDevMaintenancePlan::getIsDelete, 0)
                   .eq(MdmDevMaintenancePlan::getDataVersion, maxVersion)
                   .and(w -> w.isNotNull(MdmDevMaintenancePlan::getFirstWashTime).or().isNotNull(MdmDevMaintenancePlan::getOperTime));

            List<MdmDevMaintenancePlan> mesPlans = mdmDevMaintenancePlanEntityMapper.selectList(wrapper);
            if (mesPlans == null || mesPlans.isEmpty()) {
                log.warn("从MES查询硫化精度数据（计划时间或实际时间不为空）为空");
                return 0;
            }

            log.info("从MES查询到{}条硫化精度数据（计划时间或实际时间不为空）", mesPlans.size());

            int intervalYears = getIntervalYears();

            List<LhPrecisionPlan> plansToSave = new ArrayList<>();
            Map<String, MdmDevMaintenancePlan> latestActualPlanMap = new HashMap<>();
            Map<String, MdmDevMaintenancePlan> latestOperPlanMap = new HashMap<>();
            
            for (MdmDevMaintenancePlan mesPlan : mesPlans) {
                String machineCode = mesPlan.getDevCode();
                LocalDate actualDate = parseDate(mesPlan.getFirstWashTime());
                LocalDate operDate = parseDate(mesPlan.getOperTime());
                
                if (actualDate != null) {
                    MdmDevMaintenancePlan existing = latestActualPlanMap.get(machineCode);
                    if (existing == null || actualDate.isAfter(parseDate(existing.getFirstWashTime()))) {
                        latestActualPlanMap.put(machineCode, mesPlan);
                    }
                } else if (operDate != null) {
                    MdmDevMaintenancePlan existing = latestOperPlanMap.get(machineCode);
                    if (existing == null || operDate.isAfter(parseDate(existing.getOperTime()))) {
                        latestOperPlanMap.put(machineCode, mesPlan);
                    }
                }
            }

            for (Map.Entry<String, MdmDevMaintenancePlan> entry : latestActualPlanMap.entrySet()) {
                String machineCode = entry.getKey();
                MdmDevMaintenancePlan mesPlan = entry.getValue();
                
                LhPrecisionPlan plan = createPlanFromMes(machineCode, mesPlan, year, intervalYears);
                if (plan != null) {
                    plansToSave.add(plan);
                    log.info("准备生成硫化精度计划（基于实际时间）：机台={}, 计划日期={}", machineCode, plan.getPlanDate());
                }
            }

            List<String> operMachineCodes = new ArrayList<>(latestOperPlanMap.keySet());
            operMachineCodes.removeAll(latestActualPlanMap.keySet());
            Map<String, LhPrecisionPlan> operExistingPlanMap = new HashMap<>();
            if (!operMachineCodes.isEmpty()) {
                List<LhPrecisionPlan> operExistingPlans = lhPrecisionPlanMapper.selectByMachineCodesAndYear(operMachineCodes, year);
                for (LhPrecisionPlan p : operExistingPlans) {
                    operExistingPlanMap.put(p.getMachineCode() + "_" + p.getYear().intValue(), p);
                }
            }

            for (Map.Entry<String, MdmDevMaintenancePlan> entry : latestOperPlanMap.entrySet()) {
                String machineCode = entry.getKey();
                if (latestActualPlanMap.containsKey(machineCode)) {
                    continue;
                }
                MdmDevMaintenancePlan mesPlan = entry.getValue();

                LocalDate operDateLocal = parseDate(mesPlan.getOperTime());
                if (operDateLocal == null) {
                    log.warn("机台{}的计划时间为空，跳过", machineCode);
                    continue;
                }
                int planYear = operDateLocal.getYear();
                String existKey = machineCode + "_" + planYear;
                if (operExistingPlanMap.containsKey(existKey)) {
                    log.info("机台{}在{}年已有计划，跳过新建", machineCode, planYear);
                    continue;
                }

                LhPrecisionPlan plan = createPlanFromMesByOperTimeNoCheck(machineCode, mesPlan);
                if (plan != null) {
                    plansToSave.add(plan);
                    operExistingPlanMap.put(existKey, plan);
                    log.info("准备生成硫化精度计划（基于计划时间，实际时间为空）：机台={}, 计划日期={}", machineCode, plan.getPlanDate());
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
        plan.setDataSource(DATA_SOURCE_MES);

        LocalDate actualDateLocal = parseDate(mesPlan.getFirstWashTime());
        if (actualDateLocal == null) {
            log.warn("机台{}的实际执行时间为空，跳过", machineCode);
            return null;
        }
        
        Date actualDate = Date.from(actualDateLocal.atStartOfDay(ZoneId.systemDefault()).toInstant());
        plan.setActualDate(null);
        plan.setLastMaintenanceDate(actualDate);
        
        LocalDate planDateLocal = actualDateLocal.plusYears(intervalYears);
        Date planDate = Date.from(planDateLocal.atStartOfDay(ZoneId.systemDefault()).toInstant());
        plan.setPlanDate(planDate);
        plan.setYear(new BigDecimal(planDateLocal.getYear()));

        LocalDate today = LocalDate.now();
        plan.setDaysToDue((int) ChronoUnit.DAYS.between(today, planDateLocal));

        plan.setCompletionStatus(COMPLETION_STATUS_PENDING);
        plan.setWarningStatus(WARNING_STATUS_NO);
        plan.setIsWarningSent(WARNING_SENT_NO);
        plan.setIsDelete(0);
        plan.setBaseVale(null);

        return plan;
    }

    private LhPrecisionPlan createPlanFromMesByOperTime(String machineCode, MdmDevMaintenancePlan mesPlan, Integer year) {
        if (mesPlan == null) {
            return null;
        }

        LocalDate operDateLocal = parseDate(mesPlan.getOperTime());
        if (operDateLocal == null) {
            log.warn("机台{}的计划时间为空，跳过", machineCode);
            return null;
        }

        int planYear = operDateLocal.getYear();
        LhPrecisionPlan existingPlan = lhPrecisionPlanMapper.selectByMachineCodeAndYear(machineCode, planYear);
        if (existingPlan != null) {
            log.info("机台{}在{}年已有计划，跳过新建", machineCode, planYear);
            return null;
        }

        return buildPlanFromOperTime(machineCode, mesPlan, operDateLocal, planYear);
    }

    private LhPrecisionPlan createPlanFromMesByOperTimeNoCheck(String machineCode, MdmDevMaintenancePlan mesPlan) {
        if (mesPlan == null) {
            return null;
        }

        LocalDate operDateLocal = parseDate(mesPlan.getOperTime());
        if (operDateLocal == null) {
            log.warn("机台{}的计划时间为空，跳过", machineCode);
            return null;
        }

        int planYear = operDateLocal.getYear();
        return buildPlanFromOperTime(machineCode, mesPlan, operDateLocal, planYear);
    }

    private LhPrecisionPlan buildPlanFromOperTime(String machineCode, MdmDevMaintenancePlan mesPlan,
                                                    LocalDate operDateLocal, int planYear) {
        LhPrecisionPlan plan = new LhPrecisionPlan();
        plan.setMachineCode(machineCode);
        plan.setPrecisionType(PRECISION_TYPE_LH);
        plan.setCompanyCode(mesPlan.getCompanyCode());
        plan.setFactoryCode(mesPlan.getFactoryCode());
        plan.setMesSourceId(mesPlan.getId());
        plan.setDataSource(DATA_SOURCE_MES);

        Date planDate = Date.from(operDateLocal.atStartOfDay(ZoneId.systemDefault()).toInstant());
        plan.setActualDate(null);
        plan.setScheduleDate(null);
        plan.setPlanDate(planDate);
        plan.setYear(new BigDecimal(planYear));

        LocalDate today = LocalDate.now();
        plan.setDaysToDue((int) ChronoUnit.DAYS.between(today, operDateLocal));

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
    public int generatePlansFromMesByVersionPrefix(String versionPrefix, Integer year) {
        String lockKey = "generate:precision:plan:prefix:" + versionPrefix + ":" + year;
        if (redisService.getCacheObject(lockKey) != null) {
            throw new RuntimeException(I18nUtil.getMessage("ui.lh.precisionPlan.generate.in.progress"));
        }

        try {
            redisService.setCacheObject(lockKey, "1");
            log.info("开始从MES同步数据生成{}年度硫化精度计划（版本前缀={}）", year, versionPrefix);

            // 查询APS本地表中指定版本前缀和硫化精度类型的最大版本号
            String maxVersion = mdmDevMaintenancePlanEntityMapper.selectMaxDataVersionByPrefix(PRECISION_TYPE_LH, versionPrefix);
            if (maxVersion == null || maxVersion.isEmpty()) {
                log.warn("APS本地表中无版本前缀为{}的硫化精度版本数据，跳过处理", versionPrefix);
                return 0;
            }
            log.info("版本前缀={}的硫化精度最新版本号：{}", versionPrefix, maxVersion);

            LambdaQueryWrapper<MdmDevMaintenancePlan> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MdmDevMaintenancePlan::getPrecisionType, PRECISION_TYPE_LH)
                   .eq(MdmDevMaintenancePlan::getIsDelete, 0)
                   .eq(MdmDevMaintenancePlan::getDataVersion, maxVersion)
                   .and(w -> w.isNotNull(MdmDevMaintenancePlan::getFirstWashTime).or().isNotNull(MdmDevMaintenancePlan::getOperTime));

            List<MdmDevMaintenancePlan> mesPlans = mdmDevMaintenancePlanEntityMapper.selectList(wrapper);
            if (mesPlans == null || mesPlans.isEmpty()) {
                log.warn("从MES查询版本前缀={}的硫化精度数据（计划时间或实际时间不为空）为空", versionPrefix);
                return 0;
            }

            log.info("从MES查询到版本前缀={}的硫化精度数据{}条（计划时间或实际时间不为空）", versionPrefix, mesPlans.size());

            int intervalYears = getIntervalYears();

            List<LhPrecisionPlan> plansToSave = new ArrayList<>();
            Map<String, MdmDevMaintenancePlan> latestActualPlanMap = new HashMap<>();
            Map<String, MdmDevMaintenancePlan> latestOperPlanMap = new HashMap<>();

            for (MdmDevMaintenancePlan mesPlan : mesPlans) {
                String machineCode = mesPlan.getDevCode();
                LocalDate actualDate = parseDate(mesPlan.getFirstWashTime());
                LocalDate operDate = parseDate(mesPlan.getOperTime());

                if (actualDate != null) {
                    MdmDevMaintenancePlan existing = latestActualPlanMap.get(machineCode);
                    if (existing == null || actualDate.isAfter(parseDate(existing.getFirstWashTime()))) {
                        latestActualPlanMap.put(machineCode, mesPlan);
                    }
                } else if (operDate != null) {
                    MdmDevMaintenancePlan existing = latestOperPlanMap.get(machineCode);
                    if (existing == null || operDate.isAfter(parseDate(existing.getOperTime()))) {
                        latestOperPlanMap.put(machineCode, mesPlan);
                    }
                }
            }

            // 优先处理有实际执行时间的数据，生成新的硫化精度计划（计划时间=实际时间+间隔年数，实际时间为空）
            for (Map.Entry<String, MdmDevMaintenancePlan> entry : latestActualPlanMap.entrySet()) {
                String machineCode = entry.getKey();
                MdmDevMaintenancePlan mesPlan = entry.getValue();

                LhPrecisionPlan plan = createPlanFromMes(machineCode, mesPlan, year, intervalYears);
                if (plan != null) {
                    plansToSave.add(plan);
                    log.info("准备生成硫化精度计划（基于实际时间，版本前缀={}）：机台={}, 计划日期={}", versionPrefix, machineCode, plan.getPlanDate());
                }
            }

            // 处理只有计划时间的数据，防重复校验
            List<String> operMachineCodes = new ArrayList<>(latestOperPlanMap.keySet());
            operMachineCodes.removeAll(latestActualPlanMap.keySet());
            Map<String, LhPrecisionPlan> operExistingPlanMap = new HashMap<>();
            if (!operMachineCodes.isEmpty()) {
                List<LhPrecisionPlan> operExistingPlans = lhPrecisionPlanMapper.selectByMachineCodesAndYear(operMachineCodes, year);
                for (LhPrecisionPlan p : operExistingPlans) {
                    operExistingPlanMap.put(p.getMachineCode() + "_" + p.getYear().intValue(), p);
                }
            }

            for (Map.Entry<String, MdmDevMaintenancePlan> entry : latestOperPlanMap.entrySet()) {
                String machineCode = entry.getKey();
                if (latestActualPlanMap.containsKey(machineCode)) {
                    continue;
                }
                MdmDevMaintenancePlan mesPlan = entry.getValue();

                LocalDate operDateLocal = parseDate(mesPlan.getOperTime());
                if (operDateLocal == null) {
                    log.warn("机台{}的计划时间为空，跳过", machineCode);
                    continue;
                }
                int planYear = operDateLocal.getYear();
                String existKey = machineCode + "_" + planYear;
                if (operExistingPlanMap.containsKey(existKey)) {
                    log.info("机台{}在{}年已有计划，跳过新建", machineCode, planYear);
                    continue;
                }

                LhPrecisionPlan plan = createPlanFromMesByOperTimeNoCheck(machineCode, mesPlan);
                if (plan != null) {
                    plansToSave.add(plan);
                    operExistingPlanMap.put(existKey, plan);
                    log.info("准备生成硫化精度计划（基于计划时间，实际时间为空，版本前缀={}）：机台={}, 计划日期={}", versionPrefix, machineCode, plan.getPlanDate());
                }
            }

            if (!plansToSave.isEmpty()) {
                baseDao.insertBatch(plansToSave);
                log.info("从MES同步数据生成{}年度硫化精度计划完成（版本前缀={}），共生成{}条", year, versionPrefix, plansToSave.size());
            }

            return plansToSave.size();
        } catch (Exception e) {
            log.error("从MES同步数据生成硫化精度计划失败（版本前缀={}）", versionPrefix, e);
            throw e;
        } finally {
            redisService.deleteObject(lockKey);
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

            baseDao.updateBatch(plans);
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
        int intervalYears = getIntervalYears();
        Date dueDate = Date.from(actualDateParsed.plusYears(intervalYears).atStartOfDay(ZoneId.systemDefault()).toInstant());
        plan.setDueDate(dueDate);

        int result = lhPrecisionPlanMapper.updateById(plan);
        if (result > 0) {
            log.info("更新实际完成时间成功：机台={}, 实际日期={}", plan.getMachineCode(), actualDateParsed);
        }

        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchFillActualDateAndGenerateNext(List<Map<String, Object>> fillList) {
        if (fillList == null || fillList.isEmpty()) {
            return 0;
        }

        log.info("开始批量MES回填实际精度执行日期，共{}条", fillList.size());

        List<String> allMachineCodes = fillList.stream()
                .map(m -> (String) m.get("machineCode"))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<String> allFactoryCodes = fillList.stream()
                .map(m -> (String) m.get("factoryCode"))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<LhPrecisionPlan> pendingActualDatePlans = Collections.emptyList();
        if (!allMachineCodes.isEmpty() && !allFactoryCodes.isEmpty()) {
            pendingActualDatePlans = lhPrecisionPlanMapper.selectPendingActualDatePlans(allMachineCodes, allFactoryCodes);
        }
        Map<String, List<LhPrecisionPlan>> pendingPlanMap = pendingActualDatePlans.stream()
                .collect(Collectors.groupingBy(p -> p.getMachineCode() + "_" + p.getFactoryCode()));

        int intervalYears = getIntervalYears();

        Set<Integer> allNextYears = new HashSet<>();
        for (Map<String, Object> item : fillList) {
            Date actualDate = (Date) item.get("actualDate");
            if (actualDate != null) {
                LocalDate actualLocal = parseDate(actualDate);
                allNextYears.add(actualLocal.plusYears(intervalYears).getYear());
            }
        }

        Map<String, LhPrecisionPlan> existingNextYearPlanMap = new HashMap<>();
        for (Integer year : allNextYears) {
            if (!allMachineCodes.isEmpty()) {
                List<LhPrecisionPlan> yearPlans = lhPrecisionPlanMapper.selectByMachineCodesAndYear(allMachineCodes, year);
                for (LhPrecisionPlan p : yearPlans) {
                    existingNextYearPlanMap.put(p.getMachineCode() + "_" + year, p);
                }
            }
        }

        List<LhPrecisionPlan> plansToUpdate = new ArrayList<>();
        List<LhPrecisionPlan> plansToInsert = new ArrayList<>();
        int successCount = 0;

        for (Map<String, Object> item : fillList) {
            String machineCode = (String) item.get("machineCode");
            String factoryCode = (String) item.get("factoryCode");
            Date actualDate = (Date) item.get("actualDate");

            if (machineCode == null || factoryCode == null || actualDate == null) {
                continue;
            }

            LhPrecisionPlan matchedPlan = findNearestScheduleDatePlan(pendingPlanMap, machineCode, factoryCode, actualDate);
            if (matchedPlan == null) {
                log.warn("批量回填：未找到机台{}分厂{}下最接近计划排程精度日期且实际执行时间为空的硫化精度计划", machineCode, factoryCode);
                continue;
            }

            matchedPlan.setBaseVale(matchedPlan.getId());
            matchedPlan.setActualDate(actualDate);
            matchedPlan.setCompletionStatus(COMPLETION_STATUS_COMPLETED);
            LocalDate actualDateLocal = parseDate(actualDate);
            Date dueDate = Date.from(actualDateLocal.plusYears(intervalYears).atStartOfDay(ZoneId.systemDefault()).toInstant());
            matchedPlan.setDueDate(dueDate);
            plansToUpdate.add(matchedPlan);

            LocalDate nextPlanDateLocal = actualDateLocal.plusYears(intervalYears);
            int nextYear = nextPlanDateLocal.getYear();
            String nextKey = machineCode + "_" + nextYear;
            if (!existingNextYearPlanMap.containsKey(nextKey)) {
                LhPrecisionPlan newPlan = buildNextPrecisionPlan(matchedPlan, actualDateLocal, intervalYears, null, DATA_SOURCE_AUTO);
                if (newPlan != null) {
                    plansToInsert.add(newPlan);
                    existingNextYearPlanMap.put(nextKey, newPlan);
                    log.info("批量回填-推算生成下一次硫化精度计划：机台={}, 计划日期={}, 年度={}", machineCode, newPlan.getPlanDate(), nextYear);
                }
            } else {
                log.info("批量回填-机台{}在{}年已有计划，跳过生成下一次精度计划", machineCode, nextYear);
            }

            successCount++;
        }

        if (!plansToUpdate.isEmpty()) {
            baseDao.updateBatch(plansToUpdate);
            log.info("批量更新回填实际执行日期{}条", plansToUpdate.size());
        }
        if (!plansToInsert.isEmpty()) {
            baseDao.insertBatch(plansToInsert);
            log.info("批量插入新生成的硫化精度计划{}条", plansToInsert.size());
        }

        log.info("批量MES回填实际精度执行日期完成，成功{}条", successCount);
        return successCount;
    }

    private LhPrecisionPlan createYearlyPlan(String machineCode, Date lastActualDate, Integer year, int intervalYears) {
        if (lastActualDate == null) {
            log.warn("机台{}的上次实际日期为空，无法生成计划", machineCode);
            return null;
        }

        LhPrecisionPlan plan = new LhPrecisionPlan();

        plan.setMachineCode(machineCode);
        plan.setPrecisionType(PRECISION_TYPE_LH);
        plan.setDataSource(DATA_SOURCE_MES);

        LocalDate lastActualDateLocal = parseDate(lastActualDate);
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
            if (date instanceof java.sql.Date) {
                return ((java.sql.Date) date).toLocalDate();
            }
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
            
            LocalDate planDateLocal = parseDate(plan.getPlanDate());
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

        List<Long> mesSourceIds = maintenancePlans.stream()
                .map(MdmDevMaintenancePlan::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Set<Long> existingMesSourceIdSet = Collections.emptySet();
        if (!mesSourceIds.isEmpty()) {
            List<LhPrecisionPlan> existingByMesList = lhPrecisionPlanMapper.selectByMesSourceIdBatch(mesSourceIds);
            existingMesSourceIdSet = existingByMesList.stream()
                    .map(LhPrecisionPlan::getMesSourceId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }

        List<String> allMachineCodes = maintenancePlans.stream()
                .map(MdmDevMaintenancePlan::getDevCode)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<String> allFactoryCodes = maintenancePlans.stream()
                .map(MdmDevMaintenancePlan::getFactoryCode)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<LhPrecisionPlan> pendingActualDatePlans = Collections.emptyList();
        if (!allMachineCodes.isEmpty() && !allFactoryCodes.isEmpty()) {
            pendingActualDatePlans = lhPrecisionPlanMapper.selectPendingActualDatePlans(allMachineCodes, allFactoryCodes);
        }
        Map<String, List<LhPrecisionPlan>> pendingPlanMap = pendingActualDatePlans.stream()
                .collect(Collectors.groupingBy(p -> p.getMachineCode() + "_" + p.getFactoryCode()));

        Set<Integer> allYears = new HashSet<>();
        for (MdmDevMaintenancePlan mesPlan : maintenancePlans) {
            LocalDate actualDateLocal = parseDate(mesPlan.getFirstWashTime());
            LocalDate operDateLocal = parseDate(mesPlan.getOperTime());
            if (actualDateLocal != null) {
                allYears.add(actualDateLocal.plusYears(intervalYears).getYear());
            }
            if (operDateLocal != null) {
                allYears.add(operDateLocal.getYear());
            }
        }

        Map<String, LhPrecisionPlan> existingPlanMap = new HashMap<>();
        for (Integer year : allYears) {
            if (!allMachineCodes.isEmpty()) {
                List<LhPrecisionPlan> yearPlans = lhPrecisionPlanMapper.selectByMachineCodesAndYear(allMachineCodes, year);
                for (LhPrecisionPlan p : yearPlans) {
                    existingPlanMap.put(p.getMachineCode() + "_" + year, p);
                }
            }
        }

        List<LhPrecisionPlan> plansToUpdate = new ArrayList<>();
        List<LhPrecisionPlan> plansToInsert = new ArrayList<>();
        int processedCount = 0;

        for (MdmDevMaintenancePlan mesPlan : maintenancePlans) {
            String machineCode = mesPlan.getDevCode();
            LocalDate actualDateLocal = parseDate(mesPlan.getFirstWashTime());
            LocalDate operDateLocal = parseDate(mesPlan.getOperTime());

            if (actualDateLocal == null && operDateLocal == null) {
                log.warn("机台{}的计划时间和实际执行时间均为空，跳过", machineCode);
                continue;
            }

            Long mesSourceId = mesPlan.getId();

            if (existingMesSourceIdSet.contains(mesSourceId)) {
                log.info("MES来源ID={}已存在对应的硫化精度计划，跳过防止重复生成", mesSourceId);
                continue;
            }

            if (actualDateLocal != null) {
                Date actualDate = Date.from(actualDateLocal.atStartOfDay(ZoneId.systemDefault()).toInstant());
                String factoryCode = mesPlan.getFactoryCode();

                LhPrecisionPlan matchedPlan = findNearestScheduleDatePlan(pendingPlanMap, machineCode, factoryCode, actualDate);

                if (matchedPlan != null) {
                    matchedPlan.setBaseVale(matchedPlan.getId());
                    matchedPlan.setActualDate(actualDate);
                    matchedPlan.setCompletionStatus(COMPLETION_STATUS_COMPLETED);
                    Date dueDate = Date.from(actualDateLocal.plusYears(intervalYears).atStartOfDay(ZoneId.systemDefault()).toInstant());
                    matchedPlan.setDueDate(dueDate);
                    plansToUpdate.add(matchedPlan);
                    log.info("回填实际执行日期：机台={}, 计划ID={}, 实际日期={}", machineCode, matchedPlan.getId(), actualDate);

                    LocalDate nextPlanDateLocal = actualDateLocal.plusYears(intervalYears);
                    int nextYear = nextPlanDateLocal.getYear();
                    String nextKey = machineCode + "_" + nextYear;
                    if (!existingPlanMap.containsKey(nextKey)) {
                        LhPrecisionPlan newPlan = buildNextPrecisionPlan(matchedPlan, actualDateLocal, intervalYears, mesSourceId, DATA_SOURCE_MES);
                        if (newPlan != null) {
                            plansToInsert.add(newPlan);
                            existingPlanMap.put(nextKey, newPlan);
                            log.info("推算生成下一次硫化精度计划：机台={}, 计划日期={}, 年度={}", machineCode, newPlan.getPlanDate(), nextYear);
                        }
                    } else {
                        log.info("机台{}在{}年已有计划，跳过生成下一次精度计划", machineCode, nextYear);
                    }
                    processedCount++;
                } else {
                    LocalDate planDateLocal = actualDateLocal.plusYears(intervalYears);
                    int year = planDateLocal.getYear();

                    String existKey = machineCode + "_" + year;
                    if (existingPlanMap.containsKey(existKey)) {
                        log.info("机台{}在{}年已有计划，跳过新建", machineCode, year);
                        continue;
                    }

                    Date planDate = Date.from(planDateLocal.atStartOfDay(ZoneId.systemDefault()).toInstant());

                    LhPrecisionPlan newPlan = new LhPrecisionPlan();
                    newPlan.setMachineCode(machineCode);
                    newPlan.setPrecisionType(PRECISION_TYPE_LH);
                    newPlan.setCompanyCode(mesPlan.getCompanyCode());
                    newPlan.setFactoryCode(factoryCode);
                    newPlan.setMesSourceId(mesSourceId);
                    newPlan.setDataSource(DATA_SOURCE_MES);
                    newPlan.setActualDate(null);
                    newPlan.setLastMaintenanceDate(actualDate);
                    newPlan.setPlanDate(planDate);
                    newPlan.setYear(new BigDecimal(year));

                    LocalDate today = LocalDate.now();
                    newPlan.setDaysToDue((int) ChronoUnit.DAYS.between(today, planDateLocal));

                    newPlan.setCompletionStatus(COMPLETION_STATUS_PENDING);
                    newPlan.setWarningStatus(WARNING_STATUS_NO);
                    newPlan.setIsWarningSent(WARNING_SENT_NO);
                    newPlan.setIsDelete(0);
                    newPlan.setBaseVale(null);

                    plansToInsert.add(newPlan);
                    existingPlanMap.put(existKey, newPlan);
                    log.info("新建硫化精度计划（基于实际时间）：机台={}, 计划日期={}, MES来源ID={}", machineCode, planDate, mesSourceId);
                    processedCount++;
                }
            } else {
                String factoryCode = mesPlan.getFactoryCode();
                int year = operDateLocal.getYear();

                String existKey = machineCode + "_" + year;
                if (existingPlanMap.containsKey(existKey)) {
                    log.info("机台{}在{}年已有计划，跳过新建", machineCode, year);
                    continue;
                }

                Date planDate = Date.from(operDateLocal.atStartOfDay(ZoneId.systemDefault()).toInstant());

                LhPrecisionPlan newPlan = new LhPrecisionPlan();
                newPlan.setMachineCode(machineCode);
                newPlan.setPrecisionType(PRECISION_TYPE_LH);
                newPlan.setCompanyCode(mesPlan.getCompanyCode());
                newPlan.setFactoryCode(factoryCode);
                newPlan.setMesSourceId(mesSourceId);
                newPlan.setDataSource(DATA_SOURCE_MES);
                newPlan.setActualDate(null);
                newPlan.setScheduleDate(null);
                newPlan.setPlanDate(planDate);
                newPlan.setYear(new BigDecimal(year));

                LocalDate today = LocalDate.now();
                newPlan.setDaysToDue((int) ChronoUnit.DAYS.between(today, operDateLocal));

                newPlan.setCompletionStatus(COMPLETION_STATUS_PENDING);
                newPlan.setWarningStatus(WARNING_STATUS_NO);
                newPlan.setIsWarningSent(WARNING_SENT_NO);
                newPlan.setIsDelete(0);
                newPlan.setBaseVale(null);

                plansToInsert.add(newPlan);
                existingPlanMap.put(existKey, newPlan);
                log.info("新建硫化精度计划（基于计划时间，实际时间为空）：机台={}, 计划日期={}, MES来源ID={}", machineCode, planDate, mesSourceId);
                processedCount++;
            }
        }

        if (!plansToUpdate.isEmpty()) {
            baseDao.updateBatch(plansToUpdate);
            log.info("批量更新硫化精度计划{}条", plansToUpdate.size());
        }
        if (!plansToInsert.isEmpty()) {
            baseDao.insertBatch(plansToInsert);
            log.info("批量插入硫化精度计划{}条", plansToInsert.size());
        }

        log.info("根据设备保养计划生成硫化精度计划完成，共处理{}条", processedCount);
        return processedCount;
    }

    private LhPrecisionPlan findNearestScheduleDatePlan(Map<String, List<LhPrecisionPlan>> pendingPlanMap,
                                                         String machineCode, String factoryCode, Date actualDate) {
        String key = machineCode + "_" + factoryCode;
        List<LhPrecisionPlan> candidates = pendingPlanMap.get(key);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        LhPrecisionPlan nearest = null;
        long minDiff = Long.MAX_VALUE;
        LocalDate actualLocal = parseDate(actualDate);

        for (LhPrecisionPlan plan : candidates) {
            if (plan.getScheduleDate() == null) {
                continue;
            }
            LocalDate scheduleLocal = parseDate(plan.getScheduleDate());
            long diff = Math.abs(ChronoUnit.DAYS.between(scheduleLocal, actualLocal));
            if (diff < minDiff) {
                minDiff = diff;
                nearest = plan;
            }
        }
        return nearest;
    }

    private LhPrecisionPlan buildNextPrecisionPlan(LhPrecisionPlan currentPlan, LocalDate actualDateLocal,
                                                    int intervalYears, Long mesSourceId, String dataSource) {
        LocalDate nextPlanDateLocal = actualDateLocal.plusYears(intervalYears);
        int nextYear = nextPlanDateLocal.getYear();

        Date nextPlanDate = Date.from(nextPlanDateLocal.atStartOfDay(ZoneId.systemDefault()).toInstant());

        LhPrecisionPlan newPlan = new LhPrecisionPlan();
        newPlan.setMachineCode(currentPlan.getMachineCode());
        newPlan.setPrecisionType(PRECISION_TYPE_LH);
        newPlan.setCompanyCode(currentPlan.getCompanyCode());
        newPlan.setFactoryCode(currentPlan.getFactoryCode());
        newPlan.setMesSourceId(mesSourceId);
        newPlan.setDataSource(dataSource);
        newPlan.setActualDate(null);
        newPlan.setScheduleDate(null);
        newPlan.setLastMaintenanceDate(currentPlan.getActualDate());
        newPlan.setPlanDate(nextPlanDate);
        newPlan.setYear(new BigDecimal(nextYear));

        LocalDate today = LocalDate.now();
        newPlan.setDaysToDue((int) ChronoUnit.DAYS.between(today, nextPlanDateLocal));

        newPlan.setCompletionStatus(COMPLETION_STATUS_PENDING);
        newPlan.setWarningStatus(WARNING_STATUS_NO);
        newPlan.setIsWarningSent(WARNING_SENT_NO);
        newPlan.setIsDelete(0);
        newPlan.setBaseVale(null);

        return newPlan;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int fillScheduleDate(String machineCode, String factoryCode, Date scheduleDate) {
        log.info("硫化排程回填计划排程精度日期：机台={}, 分厂={}, 计划排程精度日期={}", machineCode, factoryCode, scheduleDate);

        LambdaQueryWrapper<LhPrecisionPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LhPrecisionPlan::getMachineCode, machineCode)
               .eq(LhPrecisionPlan::getFactoryCode, factoryCode)
               .isNull(LhPrecisionPlan::getActualDate)
               .eq(LhPrecisionPlan::getIsDelete, 0);

        List<LhPrecisionPlan> plans = lhPrecisionPlanMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(plans)) {
            log.warn("未找到机台{}分厂{}下实际执行日期为空的硫化精度计划", machineCode, factoryCode);
            return 0;
        }

        for (LhPrecisionPlan plan : plans) {
            plan.setBaseVale(plan.getId());
            plan.setScheduleDate(scheduleDate);
            log.info("回填计划排程精度日期：机台={}, 计划ID={}, 计划排程精度日期={}", machineCode, plan.getId(), scheduleDate);
        }

        baseDao.updateBatch(plans);
        log.info("硫化排程回填计划排程精度日期完成：机台={}, 回填{}条", machineCode, plans.size());
        return plans.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchFillScheduleDate(List<Map<String, Object>> fillList) {
        if (fillList == null || fillList.isEmpty()) {
            return 0;
        }

        log.info("开始批量硫化排程回填计划排程精度日期，共{}条", fillList.size());

        List<String> allMachineCodes = fillList.stream()
                .map(m -> (String) m.get("machineCode"))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<String> allFactoryCodes = fillList.stream()
                .map(m -> (String) m.get("factoryCode"))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<LhPrecisionPlan> pendingPlans = Collections.emptyList();
        if (!allMachineCodes.isEmpty() && !allFactoryCodes.isEmpty()) {
            pendingPlans = lhPrecisionPlanMapper.selectPendingScheduleDatePlans(allMachineCodes, allFactoryCodes);
        }

        Map<String, List<LhPrecisionPlan>> pendingPlanMap = pendingPlans.stream()
                .collect(Collectors.groupingBy(p -> p.getMachineCode() + "_" + p.getFactoryCode()));

        List<LhPrecisionPlan> plansToUpdate = new ArrayList<>();
        int successCount = 0;

        for (Map<String, Object> item : fillList) {
            String machineCode = (String) item.get("machineCode");
            String factoryCode = (String) item.get("factoryCode");
            Date scheduleDate = (Date) item.get("scheduleDate");

            if (machineCode == null || factoryCode == null || scheduleDate == null) {
                continue;
            }

            String key = machineCode + "_" + factoryCode;
            List<LhPrecisionPlan> plans = pendingPlanMap.get(key);
            if (plans == null || plans.isEmpty()) {
                log.warn("批量回填计划排程精度日期：未找到机台{}分厂{}下实际执行日期为空的硫化精度计划", machineCode, factoryCode);
                continue;
            }

            for (LhPrecisionPlan plan : plans) {
                plan.setBaseVale(plan.getId());
                plan.setScheduleDate(scheduleDate);
                plansToUpdate.add(plan);
                log.info("批量回填计划排程精度日期：机台={}, 计划ID={}, 计划排程精度日期={}", machineCode, plan.getId(), scheduleDate);
            }

            successCount++;
        }

        if (!plansToUpdate.isEmpty()) {
            baseDao.updateBatch(plansToUpdate);
            log.info("批量更新计划排程精度日期{}条", plansToUpdate.size());
        }

        log.info("批量硫化排程回填计划排程精度日期完成，成功回填{}台机台", successCount);
        return successCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean fillActualDateAndGenerateNext(String machineCode, String factoryCode, Date actualDate) {
        log.info("MES回填实际精度执行日期：机台={}, 分厂={}, 实际日期={}", machineCode, factoryCode, actualDate);

        LhPrecisionPlan plan = lhPrecisionPlanMapper.selectNearestScheduleDatePlan(machineCode, factoryCode, actualDate);
        if (plan == null) {
            log.warn("未找到机台{}分厂{}下最接近计划排程精度日期且实际执行时间为空的硫化精度计划", machineCode, factoryCode);
            return false;
        }

        plan.setBaseVale(plan.getId());
        plan.setActualDate(actualDate);
        plan.setCompletionStatus(COMPLETION_STATUS_COMPLETED);
        int intervalYears = getIntervalYears();
        LocalDate actualDateLocal = parseDate(actualDate);
        Date dueDate = Date.from(actualDateLocal.plusYears(intervalYears).atStartOfDay(ZoneId.systemDefault()).toInstant());
        plan.setDueDate(dueDate);

        int result = lhPrecisionPlanMapper.updateById(plan);
        if (result > 0) {
            log.info("回填实际执行日期成功：机台={}, 计划ID={}, 实际日期={}", machineCode, plan.getId(), actualDate);
        }

        generateNextPrecisionPlan(plan, actualDateLocal, intervalYears, null, DATA_SOURCE_AUTO);

        return result > 0;
    }

    private void generateNextPrecisionPlan(LhPrecisionPlan currentPlan, LocalDate actualDateLocal, int intervalYears,
                                            Long mesSourceId, String dataSource) {
        LocalDate nextPlanDateLocal = actualDateLocal.plusYears(intervalYears);
        int nextYear = nextPlanDateLocal.getYear();

        LhPrecisionPlan existingPlan = lhPrecisionPlanMapper.selectByMachineCodeAndYear(currentPlan.getMachineCode(), nextYear);
        if (existingPlan != null) {
            log.info("机台{}在{}年已有计划，跳过生成下一次精度计划", currentPlan.getMachineCode(), nextYear);
            return;
        }

        LhPrecisionPlan newPlan = new LhPrecisionPlan();
        newPlan.setMachineCode(currentPlan.getMachineCode());
        newPlan.setPrecisionType(PRECISION_TYPE_LH);
        newPlan.setCompanyCode(currentPlan.getCompanyCode());
        newPlan.setFactoryCode(currentPlan.getFactoryCode());
        newPlan.setMesSourceId(mesSourceId);
        newPlan.setDataSource(dataSource);
        newPlan.setActualDate(null);
        newPlan.setScheduleDate(null);
        newPlan.setLastMaintenanceDate(currentPlan.getActualDate());

        Date nextPlanDate = Date.from(nextPlanDateLocal.atStartOfDay(ZoneId.systemDefault()).toInstant());
        newPlan.setPlanDate(nextPlanDate);
        newPlan.setYear(new BigDecimal(nextYear));

        LocalDate today = LocalDate.now();
        newPlan.setDaysToDue((int) ChronoUnit.DAYS.between(today, nextPlanDateLocal));

        newPlan.setCompletionStatus(COMPLETION_STATUS_PENDING);
        newPlan.setWarningStatus(WARNING_STATUS_NO);
        newPlan.setIsWarningSent(WARNING_SENT_NO);
        newPlan.setIsDelete(0);
        newPlan.setBaseVale(null);

        lhPrecisionPlanMapper.insert(newPlan);
        log.info("推算生成下一次硫化精度计划：机台={}, 计划日期={}, 年度={}, MES来源ID={}", currentPlan.getMachineCode(), nextPlanDate, nextYear, mesSourceId);
    }

    @Override
    public List<LhPrecisionPlan> selectPendingIssuePlans() {
        LambdaQueryWrapper<LhPrecisionPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(LhPrecisionPlan::getScheduleDate)
               .isNull(LhPrecisionPlan::getActualDate)
               .eq(LhPrecisionPlan::getIsDelete, 0);
        return lhPrecisionPlanMapper.selectList(wrapper);
    }

    @Override
    public List<com.zlt.aps.lh.api.domain.entity.LhPrecisionPlanIssue> listPendingIssuePlans(String factoryCode) {
        LambdaQueryWrapper<LhPrecisionPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(LhPrecisionPlan::getScheduleDate)
               .isNull(LhPrecisionPlan::getActualDate)
               .eq(LhPrecisionPlan::getIsDelete, 0);
        if (factoryCode != null && !factoryCode.isEmpty()) {
            wrapper.eq(LhPrecisionPlan::getFactoryCode, factoryCode);
        }

        List<LhPrecisionPlan> plans = lhPrecisionPlanMapper.selectList(wrapper);
        List<com.zlt.aps.lh.api.domain.entity.LhPrecisionPlanIssue> result = new ArrayList<>();

        for (LhPrecisionPlan plan : plans) {
            com.zlt.aps.lh.api.domain.entity.LhPrecisionPlanIssue issue = new com.zlt.aps.lh.api.domain.entity.LhPrecisionPlanIssue();
            issue.setId(plan.getId());
            issue.setMachineCode(plan.getMachineCode());
            issue.setPrecisionType(plan.getPrecisionType());
            if (plan.getScheduleDate() != null) {
                issue.setScheduleDate(parseDate(plan.getScheduleDate()));
            }
            if (plan.getPlanDate() != null) {
                issue.setPlanDate(parseDate(plan.getPlanDate()));
            }
            issue.setFactoryCode(plan.getFactoryCode());
            issue.setCompanyCode(plan.getCompanyCode());
            result.add(issue);
        }

        return result;
    }
}
