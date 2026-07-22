package com.zlt.aps.dj.engine.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.text.MessageFormat;
import java.util.StringJoiner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.dj.api.domain.entity.DjCurlRoll;
import com.zlt.aps.dj.api.domain.entity.DjDepthConfig;
import com.zlt.aps.dj.api.domain.entity.DjGlueOrder;
import com.zlt.aps.dj.api.domain.entity.DjLossSetting;
import com.zlt.aps.dj.api.domain.entity.DjMachineInfo;
import com.zlt.aps.dj.api.domain.entity.DjMachineMaintenance;
import com.zlt.aps.dj.api.domain.entity.DjParams;
import com.zlt.aps.dj.api.domain.entity.DjScheduleProcessLog;
import com.zlt.aps.dj.api.domain.entity.DjScheduleResult;
import com.zlt.aps.dj.api.domain.entity.DjShiftConfig;
import com.zlt.aps.dj.api.domain.entity.DjSpecifyMachine;
import com.zlt.aps.dj.api.domain.entity.DjStock;
import com.zlt.aps.dj.engine.util.DjEngineUtil;
import com.zlt.aps.dj.engine.mapper.DjEngineConstructionInfoMapper;
import com.zlt.aps.dj.engine.mapper.DjEngineCurlRollMapper;
import com.zlt.aps.dj.engine.mapper.DjEngineCxScheduleResultMapper;
import com.zlt.aps.dj.engine.mapper.DjEngineDepthConfigMapper;
import com.zlt.aps.dj.engine.mapper.DjEngineGlueMapper;
import com.zlt.aps.dj.engine.mapper.DjEngineLossMapper;
import com.zlt.aps.dj.engine.mapper.DjEngineMachineMapper;
import com.zlt.aps.dj.engine.mapper.DjEngineMonthPlanMonitorMapper;
import com.zlt.aps.dj.engine.mapper.DjEngineParamsMapper;
import com.zlt.aps.dj.engine.mapper.DjEngineCxShiftConfigMapper;
import com.zlt.aps.dj.engine.mapper.DjEngineScheduleResultLogMapper;
import com.zlt.aps.dj.engine.mapper.DjEngineScheduleProcessLogMapper;
import com.zlt.aps.dj.engine.mapper.DjEngineScheduleResultMapper;
import com.zlt.aps.dj.engine.mapper.DjEngineShiftConfigMapper;
import com.zlt.aps.dj.engine.mapper.DjEngineSpecifyMachineMapper;
import com.zlt.aps.dj.engine.mapper.DjEngineStockMapper;
import com.zlt.aps.dj.engine.constant.DjEngineConstants;
import com.zlt.aps.dj.engine.model.DjPaddingDemand;
import com.zlt.aps.dj.engine.model.DjScheduleContext;
import com.zlt.aps.dj.engine.model.SupplementaryResult;
import com.zlt.aps.dj.engine.service.DjEngineNewService;
import com.zlt.aps.dj.engine.service.IDjOrderGeneratorService;
import com.zlt.aps.exception.BusinessException;
import com.zlt.aps.mdm.api.domain.entity.MdmConstructionInfo;
import com.zlt.aps.mp.api.domain.entity.MpMonthPlanMonitor;
import com.zlt.core.dao.basedao.BaseDao;

import cn.hutool.core.date.DateUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 垫胶自动排程新算法实现 根据设计文档《垫胶自动排程算法设计.md》实现
 */
@Slf4j
@Service
public class DjEngineNewServiceImpl implements DjEngineNewService {

    // ==================== Mapper 注入 ====================

    @Autowired
    private DjEngineCxScheduleResultMapper djEngineCxScheduleResultMapper;

    @Autowired
    private DjEngineScheduleResultMapper djEngineScheduleResultMapper;

    @Autowired
    private DjEngineScheduleResultLogMapper djEngineScheduleResultLogMapper;

    @Autowired
    private DjEngineMachineMapper djEngineMachineMapper;

    @Autowired
    private DjEngineStockMapper djEngineStockMapper;

    @Autowired
    private DjEngineCurlRollMapper djEngineCurlRollMapper;

    @Autowired
    private DjEngineLossMapper djEngineLossMapper;

    @Autowired
    private DjEngineGlueMapper djEngineGlueMapper;

    @Autowired
    private DjEngineConstructionInfoMapper djEngineConstructionInfoMapper;

    @Autowired
    private DjEngineParamsMapper djEngineParamsMapper;

    @Autowired
    private DjEngineShiftConfigMapper djEngineShiftConfigMapper;

    @Autowired
    private DjEngineMonthPlanMonitorMapper djEngineMonthPlanMonitorMapper;

    @Autowired
    private DjEngineSpecifyMachineMapper djEngineSpecifyMachineMapper;

    @Autowired
    private DjEngineDepthConfigMapper djEngineDepthConfigMapper;

    @Autowired
    private BaseDao baseDao;

    @Autowired
    private IDjOrderGeneratorService iDjOrderGeneratorService;

    @Autowired
    private DjEngineScheduleProcessLogMapper djEngineScheduleProcessLogMapper;

    @Autowired
    private DjEngineCxShiftConfigMapper djEngineCxShiftConfigMapper;

    // ==================== 主入口 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<DjScheduleResult> autoDjSchedule(String factoryCode, Date scheduleDate) {
        log.info("===== 垫胶自动排程开始，工厂：{}，排产日期：{} =====", factoryCode, DateUtil.formatDate(scheduleDate));

        // 初始化上下文
        DjScheduleContext context = new DjScheduleContext();
        context.setFactoryCode(factoryCode);
        context.setScheduleDate(scheduleDate);
        context.setLastSpecOfPrevShift(new HashMap<>());

        // ==================== 步骤1：加载成型计划 ====================
        List<CxScheduleResult> cxScheduleList = this.loadCxSchedule(factoryCode, scheduleDate);
        if (CollectionUtils.isEmpty(cxScheduleList)) {
            throw new BusinessException(I18nUtil.getMessage("ui.dj.engine.noCxSchedule"));
        }
        log.info("步骤1：加载成型计划 {} 条", cxScheduleList.size());

        // ==================== 步骤2：解析成型计划，获取垫胶施工清单 ====================
        this.step2ParseConstruction(context, factoryCode, cxScheduleList);
        this.step2InitShiftConfig(context, factoryCode);
        this.step2InitSupplyDepth(context, factoryCode);

        // ==================== 步骤3：计算垫胶需求清单 ====================
        List<DjPaddingDemand> demandList = this.step3CalcDemandList(context, scheduleDate);

        // ==================== 步骤4：选择机台 ====================
        this.step4SelectMachine(context, factoryCode, demandList);

        // ==================== 步骤5：排产 ====================
        List<DjScheduleResult> scheduleResults = this.executeSchedule(demandList, context);
        if (CollectionUtils.isEmpty(scheduleResults)) {
            throw new BusinessException(I18nUtil.getMessage("ui.dj.engine.noScheduleResult"));
        }
        log.info("步骤5.1~5.4：排产完成，生成 {} 条排产结果", scheduleResults.size());

        return this.step5ProcessResults(scheduleResults, context, factoryCode, scheduleDate);
    }

    // ==================== 步骤2：解析成型计划，获取垫胶施工清单 ====================

    /**
     * 步骤2.1~2.2：加载施工数据、校验、构建垫胶机台映射
     */
    private void step2ParseConstruction(DjScheduleContext context, String factoryCode,
            List<CxScheduleResult> cxScheduleList) {
        // 2.1 获取胎胚代码列表
        Set<String> constructionCodes = cxScheduleList.stream().map(CxScheduleResult::getEmbryoCode)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (constructionCodes.isEmpty()) {
            throw new BusinessException(I18nUtil.getMessage("ui.dj.engine.noEmbryoCode"));
        }

        // 加载施工数据
        List<MdmConstructionInfo> constructionList = this.loadConstructionInfo(factoryCode,
                new ArrayList<>(constructionCodes));
        if (CollectionUtils.isEmpty(constructionList)) {
            throw new BusinessException(MessageFormat.format(
                    I18nUtil.getMessage("ui.dj.engine.noConstruction"), constructionCodes));
        }
        log.info("步骤2.1：加载施工数据 {} 条", constructionList.size());

        // 构建施工号 -> 施工信息列表 Map
        Map<String, List<MdmConstructionInfo>> constructionMap = constructionList.stream()
                .collect(Collectors.groupingBy(MdmConstructionInfo::getConstructionCode));

        // 校验：所有胎胚代码必须能匹配到施工数据
        List<String> unmatchedCodes = constructionCodes.stream()
                .filter(code -> !constructionMap.containsKey(code))
                .collect(Collectors.toList());
        if (!unmatchedCodes.isEmpty()) {
            log.warn("步骤2：以下胎胚代码无对应施工数据：{}", unmatchedCodes);
            throw new BusinessException(MessageFormat.format(
                    I18nUtil.getMessage("ui.dj.engine.noConstruction"), unmatchedCodes));
        }

        // 校验：施工数据中垫胶代码和垫胶长度必须有效
        List<String> invalidConstructionCodes = new ArrayList<>();
        for (Map.Entry<String, List<MdmConstructionInfo>> entry : constructionMap.entrySet()) {
            List<MdmConstructionInfo> constructions = entry.getValue();
            boolean hasValid = constructions.stream().anyMatch(c ->
                    StringUtils.isNotEmpty(c.getPaddingCode())
                            && c.getPaddingLength() != null
                            && c.getPaddingLength().compareTo(BigDecimal.ZERO) > 0);
            if (!hasValid) {
                invalidConstructionCodes.add(entry.getKey());
            }
        }
        if (!invalidConstructionCodes.isEmpty()) {
            log.warn("步骤2：以下胎胚代码施工数据中垫胶代码或垫胶长度无效：{}", invalidConstructionCodes);
            throw new BusinessException(MessageFormat.format(
                    I18nUtil.getMessage("ui.dj.engine.invalidConstruction"), invalidConstructionCodes));
        }

        // 存储成型计划和施工数据到上下文
        context.setCxScheduleList(cxScheduleList);
        context.setConstructionMap(constructionMap);
        // 预填充施工缓存
        this.initConstructionCache(context);

        // 2.2 按垫胶规格统计对应的成型机台数量
        Map<String, Integer> paddingCxMachineCount = this.calcPaddingCxMachineCount(cxScheduleList, constructionMap);
        context.setPaddingCxMachineCount(paddingCxMachineCount);

        // 构建垫胶规格→成型机台号集合映射
        Map<String, Set<String>> paddingCxMachineSet = new HashMap<>();
        for (CxScheduleResult cx : cxScheduleList) {
            if (cx.getCxMachineCode() == null) {
                continue;
            }
            for (int shiftIdx = 1; shiftIdx <= DjEngineConstants.CX_SHIFT_COUNT; shiftIdx++) {
                MdmConstructionInfo construction = this.resolveConstructionForShift(cx, shiftIdx, context);
                if (construction != null && construction.getPaddingCode() != null) {
                    paddingCxMachineSet.computeIfAbsent(construction.getPaddingCode(), k -> new HashSet<>())
                            .add(cx.getCxMachineCode());
                }
            }
        }
        context.setPaddingCxMachineSet(paddingCxMachineSet);

        // 构建垫胶编码→物料名映射
        Map<String, String> paddingCodeToNameMap = new HashMap<>();
        for (List<MdmConstructionInfo> constructions : constructionMap.values()) {
            for (MdmConstructionInfo c : constructions) {
                if (StringUtils.isNotEmpty(c.getPaddingCode()) && StringUtils.isNotEmpty(c.getPaddingName())) {
                    paddingCodeToNameMap.putIfAbsent(c.getPaddingCode(), c.getPaddingName());
                }
            }
        }
        context.setPaddingCodeToNameMap(paddingCodeToNameMap);

        // 日志输出
        Map<String, Integer> paddingCxMachineCountByName = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : paddingCxMachineCount.entrySet()) {
            paddingCxMachineCountByName.put(context.getPaddingNameByCode(e.getKey()), e.getValue());
        }
        log.info("步骤2.2：垫胶规格数：{}，各规格成型机台数：{}", paddingCxMachineCount.size(), paddingCxMachineCountByName);
    }

    /**
     * 步骤2.2（续）：初始化班次配置、排产日映射、成型班次配置
     */
    private void step2InitShiftConfig(DjScheduleContext context, String factoryCode) {
        // 加载排产参数
        Map<String, DjParams> paramsMap = this.loadParamsMap(factoryCode);
        context.setParamsMap(paramsMap);

        // 获取首班班次（openFlag="1"的班次为启用班次，即排程首班）
        DjShiftConfig startShiftConfig = djEngineShiftConfigMapper.selectOne(
                new LambdaQueryWrapper<DjShiftConfig>()
                        .eq(DjShiftConfig::getOpenFlag, "1")
                        .orderByAsc(DjShiftConfig::getShiftOrder)
                        .last("LIMIT 1"));
        if (startShiftConfig == null || startShiftConfig.getShiftCode() == null) {
            throw new BusinessException(MessageFormat.format(
                    I18nUtil.getMessage("ui.dj.engine.shiftConfigNotConfigured"),
                    factoryCode));
        }
        String startShiftValue = startShiftConfig.getShiftCode();
        String[] shiftClassMap = DjEngineUtil.buildShiftClassMap(startShiftValue);
        context.setShiftClassMap(shiftClassMap);

        // 构建各班次索引的排产日映射
        List<DjShiftConfig> allEnabledShifts = djEngineShiftConfigMapper.selectList(
                new LambdaQueryWrapper<DjShiftConfig>()
                        .eq(DjShiftConfig::getOpenFlag, "1")
                        .orderByAsc(DjShiftConfig::getShiftOrder));
        List<String> orderedShiftCodes = allEnabledShifts.stream()
                .map(DjShiftConfig::getShiftCode)
                .collect(Collectors.toList());
        int[] scheduleDays = new int[DjEngineConstants.SHIFT_COUNT];
        int lastIdx = orderedShiftCodes.size() - 1;
        for (int i = 0; i < DjEngineConstants.SHIFT_COUNT; i++) {
            if (i == 0) {
                scheduleDays[i] = 1;
            } else if (shiftClassMap[i - 1] != null
                    && shiftClassMap[i - 1].equals(orderedShiftCodes.get(lastIdx))) {
                scheduleDays[i] = scheduleDays[i - 1] + 1;
            } else {
                scheduleDays[i] = scheduleDays[i - 1];
            }
        }
        context.setScheduleDays(scheduleDays);
        context.setShiftCountPerDay(allEnabledShifts.size());
        log.info("步骤2.2：启用的班次顺序={}，各垫胶班次排产日={}，每日班次数={}",
                String.join(",", orderedShiftCodes), java.util.Arrays.toString(scheduleDays),
                allEnabledShifts.size());

        // 成型班次偏移量
        int formingShiftOffset = Integer.parseInt(startShiftValue) - 1;
        log.info("步骤2.2：排程首班班次={}，班次映射={}，成型班次偏移量={}",
                startShiftValue, String.join(",", shiftClassMap), formingShiftOffset);
        context.setFormingShiftOffset(formingShiftOffset);

        // 加载成型班次配置映射
        List<CxShiftConfig> cxShiftConfigs = djEngineCxShiftConfigMapper.selectList(
                new LambdaQueryWrapper<CxShiftConfig>()
                        .eq(CxShiftConfig::getFactoryCode, factoryCode)
                        .eq(CxShiftConfig::getIsActive, 1));
        Map<String, Integer> cxShiftClassMap = new HashMap<>();
        for (CxShiftConfig cfg : cxShiftConfigs) {
            if (cfg.getScheduleDay() != null && cfg.getShiftName() != null && cfg.getClassField() != null) {
                String classField = cfg.getClassField().replace("CLASS", "");
                int cxShiftNum = Integer.parseInt(classField);
                String key = cfg.getScheduleDay() + "|" + cfg.getShiftName();
                cxShiftClassMap.put(key, cxShiftNum);
            }
        }
        context.setCxShiftClassMap(cxShiftClassMap);
        log.info("步骤2.2：加载成型班次配置 {} 条", cxShiftConfigs.size());
    }

    /**
     * 步骤2.2（续）：解析各垫胶规格的供应窗口（排产深度），预填充消耗量缓存
     */
    private void step2InitSupplyDepth(DjScheduleContext context, String factoryCode) {
        Map<String, Set<String>> paddingCxMachineSet = context.getPaddingCxMachineSet();
        Map<String, Integer> paddingSupplyDepth = new HashMap<>();
        int maxSupplyDepth = 0;
        for (String paddingCode : paddingCxMachineSet.keySet()) {
            Set<String> machineSet = paddingCxMachineSet.get(paddingCode);
            int machineCount = machineSet != null ? machineSet.size() : 0;
            int depth = this.parseSupplyDepth(factoryCode, machineCount);
            paddingSupplyDepth.put(paddingCode, depth);
            if (depth > maxSupplyDepth) {
                maxSupplyDepth = depth;
            }
        }
        context.setPaddingSupplyDepth(paddingSupplyDepth);
        log.info("步骤2.2：排产深度（供应窗口），最大 {} 个班", maxSupplyDepth);

        // 预填充各班各规格消耗量缓存
        this.initShiftConsumeCache(context, paddingSupplyDepth.keySet());
    }

    // ==================== 步骤3：计算垫胶需求清单 ====================

    /**
     * 步骤3.1~3.3：加载库存、判断新规格、加载月计划余量、构建需求清单、计算接班库存
     */
    private List<DjPaddingDemand> step3CalcDemandList(DjScheduleContext context, Date scheduleDate) {
        String factoryCode = context.getFactoryCode();
        Set<String> paddingCodes = context.getPaddingSupplyDepth().keySet();
        Map<String, List<MdmConstructionInfo>> constructionMap = context.getConstructionMap();

        // 3.1 加载有效库存
        Map<String, BigDecimal> effectiveStockMap = this.loadEffectiveStock(factoryCode, new ArrayList<>(paddingCodes),
                context);
        context.setEffectiveStockMap(effectiveStockMap);
        log.info("步骤3.1：加载有效库存 {} 个规格", effectiveStockMap.size());
        context.appendLog("===== 步骤3.1：有效库存 =====");
        for (Map.Entry<String, BigDecimal> entry : effectiveStockMap.entrySet()) {
            context.appendLog("  规格 {0}：有效库存 {1}", context.getPaddingNameByCode(entry.getKey()), entry.getValue());
        }

        // 判断新规格
        int newSpecDaysThreshold = this.getParamAsDecimal(context, DjEngineConstants.PARAM_NEW_SPEC_DAYS_THRESHOLD).intValue();
        List<Map<String, Object>> lastScheduleList = djEngineScheduleResultMapper.selectLastScheduleDate(
                factoryCode, scheduleDate, new ArrayList<>(paddingCodes));
        Map<String, Date> lastScheduleMap = new HashMap<>();
        if (lastScheduleList != null) {
            for (Map<String, Object> row : lastScheduleList) {
                String code = (String) row.get("PADDING_CODE");
                Object lastDateObj = row.get("LAST_DATE");
                Date lastDate = null;
                if (lastDateObj instanceof LocalDateTime) {
                    lastDate = Date.from(((LocalDateTime) lastDateObj).atZone(ZoneId.systemDefault()).toInstant());
                } else if (lastDateObj instanceof Date) {
                    lastDate = (Date) lastDateObj;
                }
                lastScheduleMap.put(code, lastDate);
            }
        }
        Set<String> newSpecPaddingCodes = new HashSet<>();
        for (String code : paddingCodes) {
            Date lastDate = lastScheduleMap.get(code);
            if (lastDate == null) {
                newSpecPaddingCodes.add(code);
            } else {
                long daysBetween = DateUtil.betweenDay(lastDate, scheduleDate, true);
                if (daysBetween >= newSpecDaysThreshold) {
                    newSpecPaddingCodes.add(code);
                }
            }
        }

        // 3.2 供应窗口超出成型范围时，加载月计划余量
        Map<String, Integer> paddingSupplyDepth = context.getPaddingSupplyDepth();
        Map<String, Integer> exceedPaddingMap = new HashMap<>();
        int maxExceedShifts = 0;
        for (Map.Entry<String, Integer> entry : paddingSupplyDepth.entrySet()) {
            int exceed = entry.getValue() - DjEngineConstants.CX_SHIFT_COUNT;
            if (exceed > 0) {
                exceedPaddingMap.put(entry.getKey(), exceed);
                if (exceed > maxExceedShifts) {
                    maxExceedShifts = exceed;
                }
            }
        }
        if (!exceedPaddingMap.isEmpty()) {
            log.info("步骤3.2：{} 个垫胶规格供应窗口超出成型计划 {} 班，最大超出 {} 个班", exceedPaddingMap.size(),
                    DjEngineConstants.CX_SHIFT_COUNT, maxExceedShifts);
            Map<String, BigDecimal> maxShiftConsume = this.getMaxShiftConsume(context);
            String yearMonth = DateUtil.format(scheduleDate, "yyyyMM");
            List<MpMonthPlanMonitor> monthPlanMonitors = this.loadMonthPlanMonitor(factoryCode, yearMonth);
            this.calcPaddingMonthSurplus(monthPlanMonitors, constructionMap, maxShiftConsume, maxExceedShifts);
            Map<String, BigDecimal> rawPaddingRemaining = this.calcRawPaddingRemaining(monthPlanMonitors,
                    constructionMap);
            if (!rawPaddingRemaining.isEmpty()) {
                context.setPaddingRemainingMap(rawPaddingRemaining);
            }
        }

        // 3.3 构建垫胶需求清单
        BigDecimal standardCurlLength = this.getParamAsDecimal(context, DjEngineConstants.PARAM_STANDARD_CRIMP_LENGTH);
        List<DjPaddingDemand> demandList = this.buildDemandList(paddingCodes,
                context, effectiveStockMap, newSpecPaddingCodes, standardCurlLength);
        log.info("步骤3.3：生成垫胶需求清单 {} 个规格", demandList.size());

        // 计算第1班接班库存
        Map<String, BigDecimal> handoverInventory = new HashMap<>();
        Date prevDate = DateUtil.offsetDay(scheduleDate, -1);
        List<DjScheduleResult> prevDayResults = djEngineScheduleResultMapper.selectList(
                new LambdaQueryWrapper<DjScheduleResult>()
                        .eq(DjScheduleResult::getScheduleDate, prevDate));
        Map<String, BigDecimal> prevDayClass3Plan = new HashMap<>();
        if (!CollectionUtils.isEmpty(prevDayResults)) {
            for (DjScheduleResult r : prevDayResults) {
                BigDecimal class3Plan = r.getClass3PlanQty();
                if (class3Plan != null && class3Plan.compareTo(BigDecimal.ZERO) > 0) {
                    prevDayClass3Plan.merge(r.getPaddingCode(), class3Plan, BigDecimal::add);
                }
            }
        }
        for (DjPaddingDemand demand : demandList) {
            String paddingCode = demand.getPaddingCode();
            BigDecimal stock = effectiveStockMap.getOrDefault(paddingCode, BigDecimal.ZERO);
            BigDecimal addPlan = prevDayClass3Plan.getOrDefault(paddingCode, BigDecimal.ZERO);
            BigDecimal cxConsume = this.calcShiftConsume(context, paddingCode, 1);
            BigDecimal inventory = stock.add(addPlan).subtract(cxConsume);
            if (inventory.compareTo(BigDecimal.ZERO) < 0) {
                inventory = BigDecimal.ZERO;
            }
            handoverInventory.put(paddingCode, inventory);
        }
        context.setHandoverInventory(handoverInventory);
        context.appendLog("===== 接班库存（第1班） =====");
        for (Map.Entry<String, BigDecimal> entry : handoverInventory.entrySet()) {
            context.appendLog("  规格 {0}：接班库存 {1}（有效库存 {2} + 前日早班计划 {3} - 当日早班消耗 {4})",
                    context.getPaddingNameByCode(entry.getKey()), entry.getValue(),
                    effectiveStockMap.getOrDefault(entry.getKey(), BigDecimal.ZERO),
                    prevDayClass3Plan.getOrDefault(entry.getKey(), BigDecimal.ZERO),
                    this.calcShiftConsume(context, entry.getKey(), 1));
        }

        return demandList;
    }

    // ==================== 步骤4：选择机台 ====================

    /**
     * 步骤4.1~4.4：加载机台、定点机台、关联分配、辅助配置
     */
    private void step4SelectMachine(DjScheduleContext context, String factoryCode,
            List<DjPaddingDemand> demandList) {
        // 4.1 加载垫胶机台
        List<DjMachineInfo> machineList = this.loadDjMachines(factoryCode);
        if (CollectionUtils.isEmpty(machineList)) {
            throw new BusinessException(I18nUtil.getMessage("ui.dj.engine.noMachine"));
        }
        Map<String, DjMachineInfo> machineMap = machineList.stream()
                .collect(Collectors.toMap(DjMachineInfo::getMachineCode, m -> m));
        context.setMachineMap(machineMap);
        log.info("步骤4.1：加载垫胶机台 {} 台", machineList.size());

        // 4.2 加载定点机台
        Set<String> paddingCodes = demandList.stream().map(DjPaddingDemand::getPaddingCode).collect(Collectors.toSet());
        List<DjSpecifyMachine> specifyMachineList = this.loadSpecifyMachines(factoryCode,
                new ArrayList<>(paddingCodes));
        log.info("步骤4.2：加载定点机台 {} 条", specifyMachineList.size());

        // 4.3 关联机台
        this.assignMachine(demandList, machineMap, specifyMachineList);

        // 4.4 加载辅助配置数据
        this.loadAuxiliaryData(factoryCode, context, new ArrayList<>(paddingCodes));
        log.info("步骤4.4：辅助配置数据加载完成");

        context.appendLog("===== 步骤4：机台分配 =====");
        for (DjPaddingDemand demand : demandList) {
            context.appendLog("  规格 {0}：是否新规格={1}，是否收尾={2}，是否量试/试制={3}，机台={4}",
                    DjScheduleContext.buildDisplayName(demand.getPaddingName(), demand.getPaddingCode()),
                    demand.isNewSpec(), demand.isTailFinished(),
                    this.hasTrialConsumption(context, demand.getPaddingCode()),
                    demand.getMachineCode());
        }
    }

    // ==================== 步骤5：排产结果处理 ====================

    /**
     * 步骤5.5：损耗率转换、日志汇总、批次号生成、归档、保存日志
     */
    private List<DjScheduleResult> step5ProcessResults(List<DjScheduleResult> scheduleResults,
            DjScheduleContext context, String factoryCode, Date scheduleDate) {
        // 5.5.1 损耗率转换（未收尾规格）
        this.convertPlanQtyWithLoss(scheduleResults, context);

        // 记录排程结果汇总
        context.appendLog("===== 排程结果汇总 =====");
        for (DjScheduleResult result : scheduleResults) {
            BigDecimal totalQty = BigDecimal.ZERO;
            StringBuilder shiftsSb = new StringBuilder();
            for (int i = 1; i <= DjEngineConstants.SHIFT_COUNT; i++) {
                BigDecimal qty = this.getClassPlanQtyFromResult(result, i);
                if (qty != null && qty.compareTo(BigDecimal.ZERO) > 0) {
                    totalQty = totalQty.add(qty);
                    shiftsSb.append(" 班").append(i).append("=").append(qty);
                }
            }
            context.appendLog("  机台={0}，规格={1}：总产量={2}{3}",
                    result.getMachineCode(),
                    DjScheduleContext.buildDisplayName(result.getPaddingName(), result.getPaddingCode()),
                    totalQty, shiftsSb.toString());
        }

        log.info(context.getProcessLog().toString());

        // 5.5.2 生成批次号
        String batchNo = iDjOrderGeneratorService.fillOrderInfo(scheduleResults, factoryCode, scheduleDate);
        context.setCurrentBatchNo(batchNo);

        // 5.5.3 归档旧数据 + 写入新数据
        this.archiveAndSave(factoryCode, scheduleDate, scheduleResults);

        // 保存排程过程日志
        this.saveScheduleProcessLog(context, batchNo);

        log.info("===== 垫胶自动排程结束，共生成 {} 条排产结果 =====", scheduleResults.size());
        return scheduleResults;
    }

    // ==================== 步骤1：加载成型计划 ====================

    /**
     * 加载成型计划
     */
    private List<CxScheduleResult> loadCxSchedule(String factoryCode, Date scheduleDate) {
        // 使用 DjEngineCxScheduleResultMapper（CommBaseMapper<CxScheduleResult>）直接查询
        LambdaQueryWrapper<CxScheduleResult> wrapper = new LambdaQueryWrapper<CxScheduleResult>()
                .eq(CxScheduleResult::getFactoryCode, factoryCode)
                .eq(CxScheduleResult::getScheduleDate, DateUtil.formatDate(scheduleDate));
        return djEngineCxScheduleResultMapper.selectList(wrapper);
    }

    // ==================== 步骤2：解析成型计划 ====================

    /**
     * 加载施工数据
     */
    private List<MdmConstructionInfo> loadConstructionInfo(String factoryCode, List<String> constructionCodes) {
        // 通过 DjEngineConstructionInfoMapper（CommBaseMapper<MdmConstructionInfo>）直接查询
        LambdaQueryWrapper<MdmConstructionInfo> wrapper = new LambdaQueryWrapper<MdmConstructionInfo>()
                .eq(MdmConstructionInfo::getFactoryCode, factoryCode)
                .in(MdmConstructionInfo::getConstructionCode, constructionCodes);
        return djEngineConstructionInfoMapper.selectList(wrapper);
    }

    /**
     * 动态计算指定垫胶规格在指定成型班次的消耗量
     * <p>
     * 遍历成型计划，找到匹配该垫胶规格的胎胚，按单耗换算为该班的消耗量。
     * 每班循环中调用此方法，从当前排产班次开始往后计算，不再提前预计算所有班次。
     * </p>
     *
     * @param cxScheduleList 成型计划列表
     * @param constructionMap 施工数据 Map
     * @param paddingCode 垫胶代码
     * @param formingClassIndex 成型班次索引（1~8），已由调用方完成垫胶→成型班次的偏移映射
     * @return 该班次的消耗量（米）
     */
    private BigDecimal calcShiftConsume(DjScheduleContext context,
            String paddingCode, int formingClassIndex) {
        // 优先从缓存读取
        Map<Integer, BigDecimal> shiftCache = context.getShiftConsumeCache().get(paddingCode);
        if (shiftCache != null && shiftCache.containsKey(formingClassIndex)) {
            return shiftCache.get(formingClassIndex);
        }
        return BigDecimal.ZERO;
    }

    /**
     * 获取该垫胶在指定成型班次的量试/试制消耗量（从 trial 缓存读取）
     * <p>
     * 用于在 checkDemandForShift 中分拆量试需求与正式需求，量试需求按照参数提前 N 个班次排产。
     * </p>
     *
     * @param context           排产上下文（含 trial 消耗缓存）
     * @param paddingCode       垫胶编码
     * @param formingClassIndex 成型班次索引（1~8）
     * @return 该班次的量试/试制消耗量（米）
     */
    private BigDecimal calcTrialShiftConsume(DjScheduleContext context,
            String paddingCode, int formingClassIndex) {
        Map<Integer, BigDecimal> trialCache = context.getShiftConsumeTrialCache().get(paddingCode);
        if (trialCache != null && trialCache.containsKey(formingClassIndex)) {
            return trialCache.get(formingClassIndex);
        }
        return BigDecimal.ZERO;
    }

    /**
     * 获取实验量试提前排产班次数
     *
     * @param context 排产上下文
     * @return 提前班次数，默认 3
     */
    private int getTrialAdvanceShifts(DjScheduleContext context) {
        return this.getParamAsDecimal(context, DjEngineConstants.PARAM_TRIAL_ADVANCE_SHIFTS).intValue();
    }

    /**
     * 判断指定垫胶规格是否存在量试/试制消耗量
     *
     * @param context     排产上下文
     * @param paddingCode 垫胶编码
     * @return true 存在量试/试制消耗
     */
    private boolean hasTrialConsumption(DjScheduleContext context, String paddingCode) {
        Map<Integer, BigDecimal> trialCache = context.getShiftConsumeTrialCache().get(paddingCode);
        if (trialCache == null) {
            return false;
        }
        for (Map.Entry<Integer, BigDecimal> entry : trialCache.entrySet()) {
            if (entry.getValue() != null && entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取成型计划某班的计划量
     */
    private BigDecimal getClassPlanQty(CxScheduleResult cx, int classIndex) {
        String fieldName = String.format(DjEngineConstants.CLASS_PLAN_QTY_FIELD, classIndex);
        return BigDecimalUtils.valueOf(cx.getFieldValueByFieldName(fieldName));
    }

    /**
     * 获取成型计划某班的示方书编号（逗号分隔时取第一个）
     *
     * @param cx        成型计划结果
     * @param shiftIndex 班次索引（1~8）
     * @return 该班次第一个示方书编号，无值时返回 null
     */
    private String getShiftRecipeNo(CxScheduleResult cx, int shiftIndex) {
        String fieldName = String.format("class%dRecipeNo", shiftIndex);
        String recipeNo = (String) cx.getFieldValueByFieldName(fieldName);
        if (StringUtils.isEmpty(recipeNo)) {
            return null;
        }
        // 逗号分隔取第一个
        String first = recipeNo.split(",")[0].trim();
        return StringUtils.isNotEmpty(first) ? first : null;
    }

    /**
     * 
     * 预填充施工信息缓存，遍历所有成型记录各班次，提前计算并缓存施工匹配结果。 匹配逻辑：<br/>
     * 1. 获取成型计划的胎胚代码<br/>
     * 2. 从施工数据 Map 中查找该胎胚代码对应的施工记录列表<br/>
     * 3. 根据排程日期+班次获取成型示方书号（RecipeNo），尝试精确匹配施工版本<br/>
     * 4. 若精确匹配失败，去掉施工版本号最后2位后进行前缀匹配<br/>
     * 5. 前缀匹配到多个施工版本时，取小版本号最大的那个<br/>
     * 6. 若 RecipeNo 为空或无匹配，返回第一个施工记录（向后兼容）
     * </p>
     * 否则抛出异常终止排程。
     * 
     * @param context 排产上下文（含施工数据 Map 和施工信息缓存）
     */
    private void initConstructionCache(DjScheduleContext context) {
        List<CxScheduleResult> cxScheduleList = context.getCxScheduleList();
        Map<String, List<MdmConstructionInfo>> constructionMap = context.getConstructionMap();
        Map<String, Map<Integer, MdmConstructionInfo>> constructionCache = context.getConstructionCache();
        for (CxScheduleResult cx : cxScheduleList) {
            String embryoCode = cx.getEmbryoCode();
            if (embryoCode == null) {
                continue;
            }
            for (int shiftIndex = 1; shiftIndex <= DjEngineConstants.CX_SHIFT_COUNT; shiftIndex++) {
                BigDecimal planQty = this.getClassPlanQty(cx, shiftIndex);
                if (planQty.compareTo(BigDecimal.ZERO) <= 0) {
                    // 无计划量不需要
                    continue;
                }
                String recipeNo = this.getShiftRecipeNo(cx, shiftIndex);
                if (StringUtils.isEmpty(recipeNo)) {
                    continue;
                }
                // 获取示方书号匹配的施工记录
                MdmConstructionInfo constructionInfo = this.matchConstructionByRecipe(constructionMap.get(embryoCode), recipeNo);
                if (constructionInfo == null) {
                    throw new BusinessException(MessageFormat.format(
                            I18nUtil.getMessage("ui.dj.engine.recipeNoNotMatch"), recipeNo, embryoCode, shiftIndex));
                }
                // 写入缓存
                constructionCache.computeIfAbsent(embryoCode, k -> new HashMap<>()).put(shiftIndex, constructionInfo);
            }
        }
    }

    /**
     * 预填充各班各规格垫胶消耗量缓存，遍历所有垫胶规格各班次，提前计算消耗量
     * <p>
     * 按施工的生产阶段（productionStage）分拆为正式消耗缓存和量试/试制消耗缓存，
     * 用于后续合并需求时分别应用不同的供应窗口偏移。
     * </p>
     */
    private void initShiftConsumeCache(DjScheduleContext context, Set<String> paddingCodes) {
        Map<String, Map<Integer, BigDecimal>> cache = context.getShiftConsumeCache();
        Map<String, Map<Integer, BigDecimal>> trialCache = context.getShiftConsumeTrialCache();
        // 获取量试/试制生产阶段编码集合
        Set<String> trialStages = this.getTrialProductionStages(context);
        for (String paddingCode : paddingCodes) {
            Map<Integer, BigDecimal> shiftCache = cache.computeIfAbsent(paddingCode, k -> new HashMap<>());
            Map<Integer, BigDecimal> trialShiftCache = trialCache.computeIfAbsent(paddingCode, k -> new HashMap<>());
            for (int classIdx = 1; classIdx <= DjEngineConstants.CX_SHIFT_COUNT; classIdx++) {
                BigDecimal totalConsume = shiftCache.get(classIdx);
                BigDecimal trialConsume = trialShiftCache.get(classIdx);
                if (totalConsume == null) {
                    totalConsume = BigDecimal.ZERO;
                    trialConsume = BigDecimal.ZERO;
                    List<CxScheduleResult> cxScheduleList = context.getCxScheduleList();
                    for (CxScheduleResult cx : cxScheduleList) {
                        // 根据班次示方书编号匹配对应的施工版本
                        MdmConstructionInfo construction = this.resolveConstructionForShift(cx, classIdx, context);
                        if (construction == null || !paddingCode.equals(construction.getPaddingCode())) {
                            continue;
                        }
                        // 垫胶长度单位是毫米(mm)，需要换算成米(m)
                        BigDecimal unitConsume = construction.getPaddingLength() != null
                                ? construction.getPaddingLength().divide(DjEngineConstants.MM_TO_M_DIVISOR, 6,
                                        RoundingMode.HALF_UP)
                                : BigDecimal.ONE;
                        BigDecimal classPlanQty = this.getClassPlanQty(cx, classIdx);
                        if (classPlanQty != null && classPlanQty.compareTo(BigDecimal.ZERO) > 0) {
                            BigDecimal consume = classPlanQty.multiply(unitConsume).setScale(2, RoundingMode.HALF_UP);
                            totalConsume = totalConsume.add(consume);
                            // 判断是否量试/试制阶段
                            if (trialStages.contains(construction.getProductionStage())) {
                                trialConsume = trialConsume.add(consume);
                            }
                        }
                    }
                    shiftCache.put(classIdx, totalConsume);
                    trialShiftCache.put(classIdx, trialConsume);
                }
            }
        }
    }

    /**
     * 获取量试/试制生产阶段编码集合
     */
    private Set<String> getTrialProductionStages(DjScheduleContext context) {
        String stages = this.getParamValue(context, DjEngineConstants.PARAM_TRIAL_PRODUCTION_STAGES);
        if (StringUtils.isEmpty(stages)) {
            stages = "T,X"; // 默认量试(T)、试制(X)
        }
        return new HashSet<>(Arrays.asList(stages.split(",")));
    }

    /**
     * 获取参数原始值
     */
    private String getParamValue(DjScheduleContext context, String paramCode) {
        DjParams param = context.getParamsMap() != null
                ? context.getParamsMap().get(paramCode)
                : null;
        return param != null ? param.getParamValue() : null;
    }

    /**
     * 解析成型计划某班次对应的施工信息（从缓存读取）
     * <p>
     * @param cx         成型计划结果
     * @param shiftIndex 班次索引（1~8）
     * @param context    排产上下文（含施工数据 Map 和施工信息缓存）
     * @return 匹配到的施工数据，无匹配时返回第一个施工记录
     */
    private MdmConstructionInfo resolveConstructionForShift(CxScheduleResult cx, int shiftIndex,
            DjScheduleContext context) {
        // 优先从缓存读取
        String embryoCode = cx.getEmbryoCode();
        Map<String, Map<Integer, MdmConstructionInfo>> constructionCache = context.getConstructionCache();
        Map<Integer, MdmConstructionInfo> shiftCache = constructionCache.get(embryoCode);
        if (shiftCache != null && shiftCache.containsKey(shiftIndex)) {
            return shiftCache.get(shiftIndex);
        }
        return null;
    }

    /**
     * 在多个施工版本中按示方书编号匹配（精确匹配→前缀匹配取最大小版本）
     */
    private MdmConstructionInfo matchConstructionByRecipe(List<MdmConstructionInfo> constructions, String recipeNo) {
        if (CollectionUtils.isEmpty(constructions)) {
            return null;
        }
        // 1. 精确匹配
        for (MdmConstructionInfo c : constructions) {
            if (recipeNo.equals(c.getConstructionVersion())) {
                return c;
            }
        }

        // 2. 前缀匹配：施工版本号去掉最后2位小版本号后与 RecipeNo 比较
        MdmConstructionInfo bestMatch = null;
        String maxSuffix = null;

        for (MdmConstructionInfo c : constructions) {
            String version = c.getConstructionVersion();
            if (StringUtils.isEmpty(version)) {
                continue;
            }
            // 去掉最后2位小版本号
            String prefix = version.length() > 2
                    ? version.substring(0, version.length() - 2)
                    : version;

            if (prefix.equals(recipeNo)) {
                String suffix = version.length() > 2
                        ? version.substring(version.length() - 2)
                        : "00";
                if (bestMatch == null || (maxSuffix != null && suffix.compareTo(maxSuffix) > 0)) {
                    bestMatch = c;
                    maxSuffix = suffix;
                }
            }
        }

        return bestMatch != null ? bestMatch : constructions.get(0);
    }

    // ==================== 步骤3：计算垫胶需求清单 ====================

    /**
     * 加载有效库存
     */
    private Map<String, BigDecimal> loadEffectiveStock(String factoryCode, List<String> paddingCodes,
            DjScheduleContext context) {
        Map<String, BigDecimal> stockMap = new HashMap<>();
        List<DjStock> stockList = djEngineStockMapper.selectList(new LambdaQueryWrapper<DjStock>()
                .eq(DjStock::getFactoryCode, factoryCode)
                .eq(DjStock::getStockDate, DateUtil.offsetDay(context.getScheduleDate(), -1)));
        if (stockList != null) {
            // 按垫胶代码分组取最新库存
            stockList.stream().filter(s -> paddingCodes.contains(s.getMaterialCode())).forEach(s -> {
                BigDecimal stock = BigDecimalUtils.valueOf(s.getStockNum())
                        .add(BigDecimalUtils.valueOf(s.getModifyNum()))
                        .subtract(BigDecimalUtils.valueOf(s.getBadNum()));
                stockMap.merge(s.getMaterialCode(), stock, BigDecimal::add);
            });
        }
        return stockMap;
    }

    /**
     * 按垫胶规格统计对应的成型机台数量
     */
    private Map<String, Integer> calcPaddingCxMachineCount(List<CxScheduleResult> cxScheduleList,
            Map<String, List<MdmConstructionInfo>> constructionMap) {
        Map<String, Set<String>> paddingMachines = new HashMap<>();
        for (CxScheduleResult cx : cxScheduleList) {
            List<MdmConstructionInfo> constructions = constructionMap.get(cx.getEmbryoCode());
            if (constructions == null || constructions.isEmpty() || cx.getCxMachineCode() == null) {
                continue;
            }
            // 收集该胎胚所有BOM版本的垫胶代码
            for (MdmConstructionInfo construction : constructions) {
                if (construction.getPaddingCode() != null) {
                    paddingMachines.computeIfAbsent(construction.getPaddingCode(), k -> new HashSet<>())
                            .add(cx.getCxMachineCode());
                }
            }
        }
        Map<String, Integer> result = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : paddingMachines.entrySet()) {
            result.put(entry.getKey(), entry.getValue().size());
        }
        return result;
    }

    /**
     * 解析供应窗口（排产深度）
     * <p>
     * 从 T_DJ_DEPTH_CONFIG（垫胶备库班数与供成型机数配置）表中获取排产深度。
     * 匹配规则：按 MIN_MACHINE_QTY 升序排序，取第一个满足
     * {@code minMachineQty ≤ cxMachineCount ≤ maxMachineQty} 的配置行。
     * {@code maxMachineQty} 为 {@code null} 表示无上限（仅末行允许）。
     * 若所有行均不匹配，使用默认排产深度 1。
     * </p>
     */
    private int parseSupplyDepth(String factoryCode, int cxMachineCount) {
        // 查询当前工厂的所有深度配置，按 MIN_MACHINE_QTY 升序
        List<DjDepthConfig> configList = djEngineDepthConfigMapper.selectList(
                new LambdaQueryWrapper<DjDepthConfig>()
                        .eq(DjDepthConfig::getFactoryCode, factoryCode)
                        .orderByAsc(DjDepthConfig::getMinMachineQty));
        if (CollectionUtils.isEmpty(configList)) {
            return 1;
        }
        for (DjDepthConfig config : configList) {
            Integer minQty = config.getMinMachineQty();
            Integer maxQty = config.getMaxMachineQty();
            if (minQty == null) {
                continue;
            }
            if (cxMachineCount >= minQty && (maxQty == null || cxMachineCount <= maxQty)) {
                BigDecimal depth = config.getDepthClassQty();
                return depth != null ? depth.intValue() : 1;
            }
        }
        // 未匹配到任何配置，返回默认排产深度 1
        return 1;
    }

    /**
     * 获取成型计划最后一天的最大班需求量（用于预估超出部分）
     */
    private Map<String, BigDecimal> getMaxShiftConsume(DjScheduleContext context) {
        Map<String, BigDecimal> maxShiftConsume = new HashMap<>();
        List<CxScheduleResult> cxScheduleList = context.getCxScheduleList();
        Map<String, List<MdmConstructionInfo>> constructionMap = context.getConstructionMap();
        // 找到最后一天（取最大的日期）
        Date lastDay = null;
        for (CxScheduleResult cx : cxScheduleList) {
            if (cx.getScheduleDate() != null) {
                if (lastDay == null || cx.getScheduleDate().after(lastDay)) {
                    lastDay = cx.getScheduleDate();
                }
            }
        }
        if (lastDay == null) {
            return maxShiftConsume;
        }

        Date finalLastDay = lastDay;
        // 过滤最后一天的数据
        for (CxScheduleResult cx : cxScheduleList) {
            if (cx.getScheduleDate() == null || !DateUtil.isSameDay(cx.getScheduleDate(), finalLastDay)) {
                continue;
            }
            // 取第1班匹配的施工数据（各班次取最大版本）
            MdmConstructionInfo construction = null;
            for (int i = 1; i <= 3 && construction == null; i++) {
                construction = this.resolveConstructionForShift(cx, i, context);
            }
            if (construction == null || construction.getPaddingCode() == null) {
                continue;
            }
            String paddingCode = construction.getPaddingCode();
            BigDecimal unitConsume = construction.getPaddingLength() != null ? construction.getPaddingLength()
                    : BigDecimal.ONE;

            // 取三个班中的最大计划量
            BigDecimal maxPlan = BigDecimal.ZERO;
            for (int i = 1; i <= 3; i++) {
                BigDecimal plan = this.getClassPlanQty(cx, i);
                if (plan != null && plan.compareTo(maxPlan) > 0) {
                    maxPlan = plan;
                }
            }
            if (maxPlan.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal consume = maxPlan.multiply(unitConsume);
                maxShiftConsume.merge(paddingCode, consume, BigDecimal::max);
            }
        }
        return maxShiftConsume;
    }

    /**
     * 加载月计划余量
     */
    private List<MpMonthPlanMonitor> loadMonthPlanMonitor(String factoryCode, String yearMonth) {
        String year = yearMonth.substring(0, 4);
        String month = yearMonth.substring(4, 6);
        LambdaQueryWrapper<MpMonthPlanMonitor> wrapper = new LambdaQueryWrapper<MpMonthPlanMonitor>()
                .eq(MpMonthPlanMonitor::getFactoryCode, factoryCode)
                .eq(MpMonthPlanMonitor::getYear, Integer.valueOf(year))
                .eq(MpMonthPlanMonitor::getMonth, Integer.valueOf(month));
        return djEngineMonthPlanMonitorMapper.selectList(wrapper);
    }

    /**
     * 计算垫胶月度剩余量
     */
    /**
     * 从施工数据列表中取第一个有效的施工记录（用于计算月计划余量等不需要按班次区分的场景）
     */
    private MdmConstructionInfo getFirstValidConstruction(
            Map<String, List<MdmConstructionInfo>> constructionMap, String paddingCode) {
        for (List<MdmConstructionInfo> constructions : constructionMap.values()) {
            for (MdmConstructionInfo c : constructions) {
                if (paddingCode.equals(c.getPaddingCode())) {
                    return c;
                }
            }
        }
        return null;
    }

    private Map<String, BigDecimal> calcPaddingMonthSurplus(List<MpMonthPlanMonitor> monthPlanMonitors,
            Map<String, List<MdmConstructionInfo>> constructionMap, Map<String, BigDecimal> maxShiftConsume,
            int exceedShifts) {
        if (CollectionUtils.isEmpty(monthPlanMonitors)) {
            return Collections.emptyMap();
        }

        Map<String, BigDecimal> result = new HashMap<>();
        for (MpMonthPlanMonitor monitor : monthPlanMonitors) {
            // 通过施工表找到垫胶代码
            for (List<MdmConstructionInfo> constructions : constructionMap.values()) {
                MdmConstructionInfo construction = constructions.stream()
                        .filter(c -> c.getPaddingCode() != null)
                        .findFirst().orElse(null);
                if (construction == null) {
                    continue;
                }
                String paddingCode = construction.getPaddingCode();
                if (result.containsKey(paddingCode)) {
                    continue;
                }

                // 计算垫胶月度剩余量 = 月剩余量 × 单耗
                BigDecimal unitConsume = construction.getPaddingLength() != null ? construction.getPaddingLength()
                        : BigDecimal.ONE;
                BigDecimal paddingRemaining = BigDecimal
                        .valueOf(monitor.getLhMargin() != null ? monitor.getLhMargin() : 0).multiply(unitConsume);

                // 预估值不得超过月剩余量
                BigDecimal maxShift = maxShiftConsume.getOrDefault(paddingCode, BigDecimal.ZERO);
                BigDecimal estimatedTotal = maxShift.multiply(BigDecimal.valueOf(exceedShifts));
                if (estimatedTotal.compareTo(paddingRemaining) > 0) {
                    estimatedTotal = paddingRemaining;
                }
                result.put(paddingCode, estimatedTotal);
                break;
            }
        }
        return result;
    }

    /**
     * 计算垫胶月度剩余量（原始值，不按超出班次截断）
     * <p>返回 Map<paddingCode, paddingRemaining>，paddingRemaining = lhMargin × unitConsume</p>
     */
    private Map<String, BigDecimal> calcRawPaddingRemaining(List<MpMonthPlanMonitor> monthPlanMonitors,
            Map<String, List<MdmConstructionInfo>> constructionMap) {
        if (CollectionUtils.isEmpty(monthPlanMonitors)) {
            return Collections.emptyMap();
        }
        Map<String, BigDecimal> result = new HashMap<>();
        for (MpMonthPlanMonitor monitor : monthPlanMonitors) {
            for (List<MdmConstructionInfo> constructions : constructionMap.values()) {
                MdmConstructionInfo construction = constructions.stream()
                        .filter(c -> c.getPaddingCode() != null)
                        .findFirst().orElse(null);
                if (construction == null) {
                    continue;
                }
                String paddingCode = construction.getPaddingCode();
                if (result.containsKey(paddingCode)) {
                    continue;
                }
                BigDecimal unitConsume = construction.getPaddingLength() != null ? construction.getPaddingLength()
                        : BigDecimal.ONE;
                BigDecimal paddingRemaining = BigDecimal
                        .valueOf(monitor.getLhMargin() != null ? monitor.getLhMargin() : 0).multiply(unitConsume);
                result.put(paddingCode, paddingRemaining);
                break;
            }
        }
        return result;
    }

    /**
     * 构建垫胶需求清单
     */
    private List<DjPaddingDemand> buildDemandList(Set<String> paddingCodes,
            DjScheduleContext context, Map<String, BigDecimal> effectiveStockMap,
            Set<String> newSpecPaddingCodes,
            BigDecimal standardCurlLength) {
        List<DjPaddingDemand> demandList = new ArrayList<>();

        Map<String, List<MdmConstructionInfo>> constructionMap = context.getConstructionMap();

        // 收集各规格的总消耗量和最早需求时间
        for (String paddingCode : paddingCodes) {
            DjPaddingDemand demand = new DjPaddingDemand();
            demand.setPaddingCode(paddingCode);

            // 从施工信息获取单耗、胶料、垫胶物料名等（取第一个有效版本）
            MdmConstructionInfo firstConstruction = this.getFirstValidConstruction(constructionMap, paddingCode);
            if (firstConstruction != null) {
                demand.setUnitConsume(firstConstruction.getPaddingLength() != null
                        ? firstConstruction.getPaddingLength() : BigDecimal.ONE);
                demand.setConstructionCode(firstConstruction.getConstructionCode());
                demand.setPaddingName(firstConstruction.getPaddingName());
                demand.setGlueCode(firstConstruction.getPaddingRubber());
                demand.setMouthPlateCode(firstConstruction.getPaddingMouthPlate());
            }

            // 判断是否已收尾：遍历使用该垫胶的所有成型计划，各班原因分析均含收尾关键字时才视为收尾
            demand.setTailFinished(this.isAllFormingPlansFinished(context, paddingCode));

            // 判断是否新规格：15日内未排产且当前库存为0
            // BigDecimal stock = effectiveStockMap.getOrDefault(paddingCode, BigDecimal.ZERO);
            demand.setNewSpec(newSpecPaddingCodes.contains(paddingCode));
                    // && stock.compareTo(BigDecimal.ZERO) <= 0);

            // 计算所有班次的总消耗量（从成型计划动态计算）
            BigDecimal totalConsume = BigDecimal.ZERO;
            for (int i = 1; i <= DjEngineConstants.CX_SHIFT_COUNT; i++) {
                totalConsume = totalConsume.add(
                        this.calcShiftConsume(context, paddingCode, i));
            }
            BigDecimal effectiveStock = effectiveStockMap.getOrDefault(paddingCode, BigDecimal.ZERO);
            BigDecimal netDemand = totalConsume.subtract(effectiveStock);
            if (netDemand.compareTo(BigDecimal.ZERO) < 0) {
                netDemand = BigDecimal.ZERO;
            }
            demand.setRemainingDemand(netDemand);
            demand.setIncomingInventory(effectiveStock);
            demand.setTrolleyCapacity(standardCurlLength);
            demand.setNeedProduce(netDemand.compareTo(BigDecimal.ZERO) > 0);

            demandList.add(demand);
        }
        return demandList;
    }

    /**
     * 判断使用该垫胶的所有成型计划是否都已收尾
     * <p>
     * 遍历成型计划列表，找到使用该垫胶的所有成型计划，
     * 检查每个成型计划各班的系统原因分析或手动原因分析是否包含收尾关键字，
     * 任意一个班包含即表示该成型计划已收尾。
     * 必须所有成型计划都收尾，该规格垫胶才算收尾。
     * </p>
     *
     * @param context 排产上下文（含成型计划列表及参数）
     * @param paddingCode 垫胶编码
     * @return true 表示所有使用该垫胶的成型计划都已收尾
     */
    private boolean isAllFormingPlansFinished(DjScheduleContext context, String paddingCode) {
        List<CxScheduleResult> cxScheduleList = context.getCxScheduleList();
        if (CollectionUtils.isEmpty(cxScheduleList)) {
            return false;
        }
        // 从参数中获取收尾关键字，默认"收尾"
        DjParams param = context.getParamsMap() != null
                ? context.getParamsMap().get(DjEngineConstants.PARAM_CX_ANALYSIS_CLOSEOUT_KEYWORD)
                : null;
        String keyword = (param != null && StringUtils.isNotEmpty(param.getParamValue()))
                ? param.getParamValue()
                : "收尾";

        boolean hasMatched = false;
        for (CxScheduleResult cx : cxScheduleList) {
            // 遍历各班次检查是否使用了该垫胶
            for (int shiftIndex = 1; shiftIndex <= DjEngineConstants.CX_SHIFT_COUNT; shiftIndex++) {
                MdmConstructionInfo construction = this.resolveConstructionForShift(cx, shiftIndex, context);
                if (construction != null && paddingCode.equals(construction.getPaddingCode())) {
                    hasMatched = true;
                    // 检查该成型计划任意一个班的原因分析是否包含收尾关键字
                    if (!isCxPlanFinishedByAnalysis(cx, keyword)) {
                        return false;
                    }
                    break; // 该成型计划已匹配，无需继续检查其他班次
                }
            }
        }
        // 没有找到使用该垫胶的成型计划，视为未收尾
        return hasMatched;
    }

    /**
     * 判断单个成型计划是否已收尾（根据各班原因分析字段判断）
     * <p>
     * 遍历 1~8 班的系统原因分析和手动原因分析字段，
     * 任意一个班包含收尾关键字即视为收尾。
     * </p>
     *
     * @param cx 成型计划
     * @param keyword 收尾关键字
     * @return true 表示该成型计划已收尾
     */
    private boolean isCxPlanFinishedByAnalysis(CxScheduleResult cx, String keyword) {
        for (int shiftIndex = 1; shiftIndex <= DjEngineConstants.CX_SHIFT_COUNT; shiftIndex++) {
            // 检查系统原因分析
            String sysAnalysisField = String.format(DjEngineConstants.CLASS_ANALYSIS_FIELD, shiftIndex);
            String sysAnalysis = (String) cx.getFieldValueByFieldName(sysAnalysisField);
            if (sysAnalysis != null && sysAnalysis.contains(keyword)) {
                return true;
            }
            // 检查手动输入原因分析
            String inputAnalysisField = String.format(DjEngineConstants.CLASS_ANALYSIS_INPUT_FIELD, shiftIndex);
            String inputAnalysis = (String) cx.getFieldValueByFieldName(inputAnalysisField);
            if (inputAnalysis != null && inputAnalysis.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    // ==================== 步骤4：选择机台 ====================

    /**
     * 加载垫胶机台
     */
    private List<DjMachineInfo> loadDjMachines(String factoryCode) {
        return djEngineMachineMapper
                .selectList(new LambdaQueryWrapper<DjMachineInfo>().eq(DjMachineInfo::getFactoryCode, factoryCode));
    }

    /**
     * 加载定点机台
     */
    private List<DjSpecifyMachine> loadSpecifyMachines(String factoryCode, List<String> paddingCodes) {
        return djEngineSpecifyMachineMapper.selectList(
                new LambdaQueryWrapper<DjSpecifyMachine>().eq(DjSpecifyMachine::getFactoryCode, factoryCode));
    }

    /**
     * 关联机台分配 策略：优先填满一个机台，产能不足再拆分
     */
    private void assignMachine(List<DjPaddingDemand> demandList, Map<String, DjMachineInfo> machineMap,
            List<DjSpecifyMachine> specifyMachineList) {
        // 整理定点机台关系 Map<paddingCode, List<定点机台>>
        Map<String, List<DjSpecifyMachine>> specifyMap = specifyMachineList.stream()
                .filter(s -> DjEngineConstants.JOB_TYPE_FORBIDDEN.equals(s.getJobType()))
                .collect(Collectors.groupingBy(DjSpecifyMachine::getPaddingCode));

        for (DjPaddingDemand demand : demandList) {
            List<DjSpecifyMachine> machines = specifyMap.get(demand.getPaddingCode());
            if (machines == null || machines.isEmpty()) {
                // 未配置定点机台，默认使用所有启用的机台
                demand.setMachineCode(machineMap.keySet().iterator().next());
                continue;
            }

            // 按生产线优先，备用线其次
            List<DjSpecifyMachine> productionLines = machines.stream()
                    .filter(m -> DjEngineConstants.LINE_TYPE_PRODUCTION.equals(m.getLineType()))
                    .collect(Collectors.toList());
            List<DjSpecifyMachine> backupLines = machines.stream()
                    .filter(m -> DjEngineConstants.LINE_TYPE_BACKUP.equals(m.getLineType()))
                    .collect(Collectors.toList());

            // 优先分配生产线
            if (!productionLines.isEmpty()) {
                demand.setMachineCode(productionLines.get(0).getMachineCode());
            } else if (!backupLines.isEmpty()) {
                // 备用线按定额从高到低
                List<DjSpecifyMachine> sortedBackup = backupLines.stream().sorted((a, b) -> {
                    DjMachineInfo ma = machineMap.get(a.getMachineCode());
                    DjMachineInfo mb = machineMap.get(b.getMachineCode());
                    BigDecimal qa = ma != null && ma.getQuata() != null ? ma.getQuata() : BigDecimal.ZERO;
                    BigDecimal qb = mb != null && mb.getQuata() != null ? mb.getQuata() : BigDecimal.ZERO;
                    return qb.compareTo(qa);
                }).collect(Collectors.toList());
                demand.setMachineCode(sortedBackup.get(0).getMachineCode());
            }
        }
    }

    /**
     * 加载辅助配置数据
     */
    private void loadAuxiliaryData(String factoryCode, DjScheduleContext context, List<String> paddingCodes) {
        // 加载卷曲信息
        Map<String, BigDecimal> curlLengthMap = new HashMap<>();
        List<DjCurlRoll> curlRollList = djEngineCurlRollMapper
                .selectList(new LambdaQueryWrapper<DjCurlRoll>().eq(DjCurlRoll::getFactoryCode, factoryCode));
        if (curlRollList != null) {
            for (DjCurlRoll curl : curlRollList) {
                if (curl.getCurlLength() != null) {
                    curlLengthMap.put(curl.getPaddingCode(), curl.getCurlLength());
                }
            }
        }
        context.setCurlLengthMap(curlLengthMap);

        // 加载损耗率
        Map<String, BigDecimal> lossRateMap = new HashMap<>();
        List<DjLossSetting> lossList = djEngineLossMapper
                .selectList(new LambdaQueryWrapper<DjLossSetting>().eq(DjLossSetting::getFactoryCode, factoryCode)
                        .in(DjLossSetting::getPaddingCode, paddingCodes));
        if (lossList != null) {
            for (DjLossSetting loss : lossList) {
                if (loss.getLossRate() != null) {
                    String lossKey = loss.getPaddingCode() + "#" + loss.getMachineCode();
                    lossRateMap.put(lossKey, BigDecimalUtils.valueOf(loss.getLossRate()));
                }
            }
        }
        context.setLossRateMap(lossRateMap);

        // 加载胶料顺序
        Map<String, Integer> glueOrderMap = new HashMap<>();
        List<DjGlueOrder> glueOrderList = djEngineGlueMapper
                .selectList(new LambdaQueryWrapper<DjGlueOrder>().eq(DjGlueOrder::getFactoryCode, factoryCode));
        if (glueOrderList != null) {
            for (DjGlueOrder order : glueOrderList) {
                if (order.getOrderNum() != null) {
                    glueOrderMap.put(order.getGlueCode(), order.getOrderNum());
                }
            }
        }
        context.setGlueOrderMap(glueOrderMap);
    }

    // ==================== 步骤5：核心排产 ====================

    /**
     * 执行排产
     */
    private List<DjScheduleResult> executeSchedule(List<DjPaddingDemand> demandList, DjScheduleContext context) {
        Map<String, BigDecimal> handoverInventory = context.getHandoverInventory();
        Map<String, DjMachineInfo> machineMap = context.getMachineMap();

        // 有效产能矩阵 Map<machineCode, Map<shiftIndex, remainingCapacity>>
        Map<String, Map<Integer, BigDecimal>> capacityMatrix = calcEffectiveCapacity(machineMap, context);

        // 每班台车工装约束上限
        BigDecimal shiftTrolleyLimit = this.calcShiftTrolleyLimit(context);
        if (shiftTrolleyLimit != null) {
            log.info("班次台车约束上限：{} 米", shiftTrolleyLimit);
        }

        // 卷曲参数
        BigDecimal trolleyStdCurlLength = this.getParamAsDecimal(context, DjEngineConstants.PARAM_STANDARD_CRIMP_LENGTH);
        BigDecimal trolleyFullRate = this.getParamAsDecimal(context, DjEngineConstants.PARAM_TROLLEY_FULL_RATE);
        // 数据库存储百分比值（如 80），转换为小数（0.8）
        trolleyFullRate = trolleyFullRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        if (trolleyFullRate.compareTo(BigDecimal.ZERO) <= 0) {
            trolleyFullRate = BigDecimal.ONE;
        }

        // 各机台各班的最后生产规格
        Map<String, String> lastSpecInShift = new HashMap<>();
        // 排产结果
        Map<String, DjScheduleResult> resultMap = new HashMap<>();

        // 遍历6个班次
        for (int shiftIndex = 1; shiftIndex <= DjEngineConstants.SHIFT_COUNT; shiftIndex++) {
            log.debug("排产班次 {}/{}（中班→夜班→早班循环）", shiftIndex, DjEngineConstants.SHIFT_COUNT);
            context.appendLog("");
            context.appendLog("===== 班次 {0} =====", shiftIndex);

            // 班次准备：重置标志、记录库存、需求判定
            this.prepareShift(demandList, shiftIndex, context, handoverInventory);

            // 计算本班实际可用的产能上限
            BigDecimal shiftRemainingCapacity = this.calcShiftTotalCapacity(capacityMatrix, shiftIndex,
                    shiftTrolleyLimit);
            BigDecimal shiftRemainingTrolleys = calcRemainingTrolleys(context, handoverInventory);
            context.appendLog("班次 {0} 总产能上限：{1}，初始剩余台车数：{2}",
                    shiftIndex, shiftRemainingCapacity, shiftRemainingTrolleys);

            // 遍历所有机台
            for (Map.Entry<String, DjMachineInfo> machineEntry : machineMap.entrySet()) {
                SupplementaryResult machineResult = this.processMachineInShift(
                        machineEntry.getValue(), machineEntry.getKey(),
                        shiftIndex, demandList, resultMap, capacityMatrix, lastSpecInShift,
                        shiftRemainingCapacity, shiftRemainingTrolleys,
                        trolleyStdCurlLength, trolleyFullRate, context);
                shiftRemainingCapacity = machineResult.getShiftRemainingCapacity();
                shiftRemainingTrolleys = machineResult.getShiftRemainingTrolleys();
            }

            // 班次结束：计算各规格交班库存
            this.calcEndOfShiftInventory(demandList, shiftIndex, context, resultMap, handoverInventory);
        }

        // 转换排产结果
        return this.buildScheduleResults(demandList, resultMap, context);
    }

    // ===== 以下为 executeSchedule 的提取方法 =====

    /**
     * 记录 checkDemandForShift 后的需求判定结果日志
     */
    private void logDemandDecision(List<DjPaddingDemand> demandList, DjScheduleContext context, int shiftIndex) {
        context.appendLog("--- 班次 {0} 需求判定结果 ---", shiftIndex);
        for (DjPaddingDemand spec : demandList) {
            String needProduceStr = spec.isNeedProduce() ? "需要排产" : "库存充足不排产";
            if (spec.isNeedProduce()) {
                String tailNote = "";
                if (spec.isTailFinished() && spec.getLossRatePercent() != null
                        && spec.getLossRatePercent().compareTo(BigDecimal.ZERO) > 0
                        && spec.getPreLossRateDemand() != null) {
                    BigDecimal rateFactor = BigDecimal.ONE.add(
                            spec.getLossRatePercent().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
                    tailNote = MessageFormat.format(" (收尾，{0} × {1}%)",
                            spec.getPreLossRateDemand().setScale(2, RoundingMode.HALF_UP),
                            rateFactor.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP));
                }
                context.appendLog("  规格 {0}：{1}，可覆盖班次={2}，触发阈值≤{3}，剩余待排={4}{5}",
                        context.getPaddingNameByCode(spec.getPaddingCode()), needProduceStr, spec.getCoverableShiftCount(),
                        this.getParamAsDecimal(context, DjEngineConstants.PARAM_SCHEDULE_THRESHOLD).intValue(),
                        spec.getRemainingDemand(), tailNote);
            } else if (spec.getRemainingDemand() != null
                    && spec.getRemainingDemand().compareTo(BigDecimal.ZERO) > 0) {
                context.appendLog("  规格 {0}：{1}，可覆盖班次={2}，触发阈值≤{3}",
                        context.getPaddingNameByCode(spec.getPaddingCode()), needProduceStr, spec.getCoverableShiftCount(),
                        this.getParamAsDecimal(context, DjEngineConstants.PARAM_SCHEDULE_THRESHOLD).intValue());
            }
        }
    }

    /**
     * 当班多规格接班库存不足时，前 n-1 个标记为供应缺口填补模式
     * <p>补缺口模式：按供应缺口 + 安全水位(SYS1401005)排产，不补供应窗口净需求</p>
     */
    private void markMultiSpecGapMode(List<DjPaddingDemand> demandList, DjScheduleContext context, int shiftIndex) {
        List<DjPaddingDemand> needProduceSpecs = demandList.stream()
                .filter(s -> s.isNeedProduce() && s.getRemainingDemand() != null
                        && s.getRemainingDemand().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
        if (needProduceSpecs.size() >= 2) {
            needProduceSpecs.sort(buildPriorityComparator(demandList, null, context, shiftIndex));
            for (int i = 0; i < needProduceSpecs.size() - 1; i++) {
                needProduceSpecs.get(i).setSupplyGapMode(true);
            }
            context.appendLog("当班多规格接班库存不足：共 {0} 个规格需补量，前 {1} 个按供应缺口+安全水位排产",
                    needProduceSpecs.size(), needProduceSpecs.size() - 1);
        }
    }

    /**
     * 主循环排产后机台仍有剩余产能时，进行二次补量
     * <p>第一轮：supplyGapMode 规格各补 SCHEDULE_THRESHOLD 班消耗量</p>
     * <p>第二轮：所有规格补满供应窗口需求量</p>
     *
     * @return 补量后的班次状态（shiftRemainingCapacity + shiftRemainingTrolleys）
     */
    private SupplementaryResult executeSupplementaryProduction(
            List<DjPaddingDemand> pendingSpecs, int shiftIndex, String machineCode,
            Map<String, DjScheduleResult> resultMap, Map<String, String> lastSpecInShift,
            Map<Integer, BigDecimal> machineCapacity, BigDecimal shiftRemainingCapacity,
            BigDecimal shiftRemainingTrolleys, BigDecimal trolleyStdCurlLength,
            BigDecimal trolleyFullRate, DjScheduleContext context) {

        BigDecimal remainingCapacity = machineCapacity.get(shiftIndex);
        if (remainingCapacity == null || remainingCapacity.compareTo(BigDecimal.ZERO) <= 0
                || shiftRemainingTrolleys == null || shiftRemainingTrolleys.compareTo(BigDecimal.ZERO) <= 0) {
            return new SupplementaryResult(shiftRemainingCapacity, shiftRemainingTrolleys);
        }

        int scheduleThreshold = this.getParamAsDecimal(context, DjEngineConstants.PARAM_SCHEDULE_THRESHOLD).intValue();

        // 第一轮补量：对 supplyGapMode=true 的规格，各补 SCHEDULE_THRESHOLD 班消耗量
        int firstFormingClass = this.getFormingClassByShiftIndex(shiftIndex, context);
        if (firstFormingClass < 1) {
            firstFormingClass = shiftIndex + (context.getFormingShiftOffset() != null ? context.getFormingShiftOffset() : 0);
        }
        context.appendLog("  --- 补量第一轮：供应缺口规格各补 {0} 班 ---", scheduleThreshold);
        for (DjPaddingDemand spec : pendingSpecs) {
            if (remainingCapacity.compareTo(BigDecimal.ZERO) <= 0
                    || shiftRemainingTrolleys.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            if (!spec.isSupplyGapMode()) continue;
            if (spec.getRemainingDemand() == null || spec.getRemainingDemand().compareTo(BigDecimal.ZERO) <= 0) continue;

            // 计算 SCHEDULE_THRESHOLD 个成型班次的消耗量
            BigDecimal thresholdConsume = BigDecimal.ZERO;
            for (int fc = firstFormingClass + 1; fc <= firstFormingClass + scheduleThreshold; fc++) {
                if (fc > DjEngineConstants.CX_SHIFT_COUNT) break;
                thresholdConsume = thresholdConsume.add(this.calcShiftConsume(context, spec.getPaddingCode(), fc));
            }
            // 补量上限 = min(阈值消耗量, 剩余需求, 剩余产能)
            BigDecimal maxProduce = thresholdConsume.min(spec.getRemainingDemand()).min(remainingCapacity);
            if (maxProduce.compareTo(BigDecimal.ZERO) <= 0) continue;

            // 向上取整到整台车
            BigDecimal trolleyCapacity = spec.getTrolleyCapacity();
            if (trolleyCapacity == null || trolleyCapacity.compareTo(BigDecimal.ZERO) <= 0) continue;
            int maxTrolleys = remainingCapacity.divide(trolleyCapacity, 0, RoundingMode.FLOOR).intValue();
            int needTrolleys = maxProduce.divide(trolleyCapacity, 0, RoundingMode.CEILING).intValue();
            int actualTrolleys = Math.min(maxTrolleys, needTrolleys);
            if (actualTrolleys <= 0) continue;

            BigDecimal produceQty = BigDecimal.valueOf(actualTrolleys).multiply(trolleyCapacity);
            if (produceQty.compareTo(BigDecimal.ZERO) <= 0) continue;

            // 扣减剩余需求
            spec.setRemainingDemand(spec.getRemainingDemand().subtract(produceQty));

            // 记录排产结果
            this.recordSchedule(resultMap, spec, shiftIndex, produceQty, machineCode, context);
            context.appendLog("  补量规格 {0}：生产 {1}，剩余待排={2}",
                    context.getPaddingNameByCode(spec.getPaddingCode()), produceQty, spec.getRemainingDemand());

            lastSpecInShift.put(machineCode, spec.getPaddingCode());
            remainingCapacity = remainingCapacity.subtract(produceQty);
            machineCapacity.put(shiftIndex, remainingCapacity);
            shiftRemainingCapacity = shiftRemainingCapacity.subtract(produceQty).max(BigDecimal.ZERO);

            BigDecimal consumedTrolleys = produceQty.divide(trolleyStdCurlLength, 4, RoundingMode.HALF_UP)
                    .divide(trolleyFullRate, 4, RoundingMode.HALF_UP)
                    .setScale(0, RoundingMode.FLOOR);
            shiftRemainingTrolleys = shiftRemainingTrolleys.subtract(consumedTrolleys).max(BigDecimal.ZERO);
        }

        // 第二轮补量：所有规格补满供应窗口需求量
        context.appendLog("  --- 补量第二轮：所有规格补满供应窗口 ---");
        for (DjPaddingDemand spec : pendingSpecs) {
            if (remainingCapacity.compareTo(BigDecimal.ZERO) <= 0
                    || shiftRemainingTrolleys.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            // 供应缺口模式规格已在第1轮补量，本轮跳过
            if (spec.isSupplyGapMode()) continue;

            // 计算供应窗口净需求量
            BigDecimal windowDemand = this.calcWindowNetDemand(spec, shiftIndex, context);
            if (windowDemand.compareTo(BigDecimal.ZERO) <= 0) continue;

            // 已排产量
            BigDecimal scheduledQty = this.getScheduledQty(resultMap, spec.getPaddingCode(), shiftIndex);
            BigDecimal remainingWindowDemand = windowDemand.subtract(scheduledQty);
            if (remainingWindowDemand.compareTo(BigDecimal.ZERO) <= 0) continue;

            // 临时设置 remainingDemand 以复用 calcProduceQty
            BigDecimal oldRemainingDemand = spec.getRemainingDemand();
            spec.setRemainingDemand(remainingWindowDemand);
            boolean oldSupplyGapMode = spec.isSupplyGapMode();
            spec.setSupplyGapMode(false);
            BigDecimal produceQty = this.calcProduceQty(spec, remainingCapacity, shiftIndex, context);
            spec.setSupplyGapMode(oldSupplyGapMode);
            spec.setRemainingDemand(oldRemainingDemand);

            if (produceQty == null || produceQty.compareTo(BigDecimal.ZERO) <= 0) continue;

            // 记录排产结果
            this.recordSchedule(resultMap, spec, shiftIndex, produceQty, machineCode, context);
            context.appendLog("  补量规格 {0}：生产 {1}（窗口剩余={2}）",
                    context.getPaddingNameByCode(spec.getPaddingCode()), produceQty, remainingWindowDemand);

            lastSpecInShift.put(machineCode, spec.getPaddingCode());
            remainingCapacity = remainingCapacity.subtract(produceQty);
            machineCapacity.put(shiftIndex, remainingCapacity);
            shiftRemainingCapacity = shiftRemainingCapacity.subtract(produceQty).max(BigDecimal.ZERO);

            BigDecimal consumedTrolleys = produceQty.divide(trolleyStdCurlLength, 4, RoundingMode.HALF_UP)
                    .divide(trolleyFullRate, 4, RoundingMode.HALF_UP)
                    .setScale(0, RoundingMode.FLOOR);
            shiftRemainingTrolleys = shiftRemainingTrolleys.subtract(consumedTrolleys).max(BigDecimal.ZERO);
        }

        machineCapacity.put(shiftIndex, remainingCapacity);
        return new SupplementaryResult(shiftRemainingCapacity, shiftRemainingTrolleys);
    }

    /**
     * 班次准备：重置供应缺口标志、记录接班库存、执行需求判定
     */
    private void prepareShift(List<DjPaddingDemand> demandList, int shiftIndex, DjScheduleContext context,
            Map<String, BigDecimal> handoverInventory) {
        // 重置各规格的供应缺口填补标志（避免跨班次残留）
        for (DjPaddingDemand spec : demandList) {
            spec.setSupplyGapMode(false);
        }

        // 记录各规格接班库存
        context.appendLog("--- 班次 {0} 接班库存 ---", shiftIndex);
        for (DjPaddingDemand spec : demandList) {
            BigDecimal inv = handoverInventory.getOrDefault(spec.getPaddingCode(), BigDecimal.ZERO);
            context.appendLog("  规格 {0}：接班库存 {1}",
                    DjScheduleContext.buildDisplayName(spec.getPaddingName(), spec.getPaddingCode()), inv);
        }

        // 检查各规格接班库存是否满足供应窗口内的成型消耗量
        this.checkDemandForShift(demandList, shiftIndex, context);
        // 记录需求判定结果
        this.logDemandDecision(demandList, context, shiftIndex);
        // 多规格不足场景标记
        this.markMultiSpecGapMode(demandList, context, shiftIndex);
    }

    /**
     * 处理单台机在单个班次的排产：可用性检查 → 规格优先级排序 → 逐个分配排产 → 补量
     *
     * @return 补后排班次剩余产能和剩余台车数
     */
    private SupplementaryResult processMachineInShift(DjMachineInfo machine, String machineCode,
            int shiftIndex, List<DjPaddingDemand> demandList, Map<String, DjScheduleResult> resultMap,
            Map<String, Map<Integer, BigDecimal>> capacityMatrix, Map<String, String> lastSpecInShift,
            BigDecimal shiftRemainingCapacity, BigDecimal shiftRemainingTrolleys,
            BigDecimal trolleyStdCurlLength, BigDecimal trolleyFullRate, DjScheduleContext context) {

        // 机台本班是否可用
        if (!this.isMachineAvailable(machine, shiftIndex, context.getShiftClassMap())) {
            log.trace("机台 {} 班次 {} 不可用", machineCode, shiftIndex);
            return new SupplementaryResult(shiftRemainingCapacity, shiftRemainingTrolleys);
        }

        // 获取本班该机台的剩余产能
        Map<Integer, BigDecimal> machineCapacity = capacityMatrix.get(machineCode);
        if (machineCapacity == null) {
            return new SupplementaryResult(shiftRemainingCapacity, shiftRemainingTrolleys);
        }
        BigDecimal remainingCapacity = machineCapacity.get(shiftIndex);
        if (remainingCapacity == null || remainingCapacity.compareTo(BigDecimal.ZERO) <= 0) {
            return new SupplementaryResult(shiftRemainingCapacity, shiftRemainingTrolleys);
        }

        // 获取本机台本班待排产规格
        List<DjPaddingDemand> pendingSpecs = getSpecsByMachine(machineCode, demandList);
        // 获取上一班最后生产的规格
        String lastSpecCode = lastSpecInShift.get(machineCode);

        // 日志：记录待排产规格
        context.appendLog("--- 机台 {0}（产能={1}，上一规格={2}）---",
                machineCode, remainingCapacity, lastSpecCode != null ? lastSpecCode : "无");

        // 优先级排序
        pendingSpecs.sort(buildPriorityComparator(demandList, lastSpecCode, context, shiftIndex));

        // 记录排序后的待排产规格
        StringJoiner priorityJoiner = new StringJoiner(" → ");
        for (DjPaddingDemand ps : pendingSpecs) {
            if (ps.isNeedProduce() && ps.getRemainingDemand() != null
                    && ps.getRemainingDemand().compareTo(BigDecimal.ZERO) > 0) {
                priorityJoiner.add(context.getPaddingNameByCode(ps.getPaddingCode()));
            }
        }
        String priorityOrderStr = priorityJoiner.toString();
        if (!priorityOrderStr.isEmpty()) {
            context.appendLog("  优先级排序：{0}", priorityOrderStr);
        }
        // 记录各规格的库消比
        for (DjPaddingDemand ps : pendingSpecs) {
            BigDecimal ratio = this.calcStockConsumeRatio(ps, context);
            context.appendLog("    规格 {0}：剩余需求={1}，库消比={2}，胶料={3}，口型={4}",
                    context.getPaddingNameByCode(ps.getPaddingCode()),
                    (ps.getRemainingDemand() != null ? ps.getRemainingDemand() : BigDecimal.ZERO),
                    (ratio != null ? ratio : "N/A"),
                    ps.getGlueCode() != null ? ps.getGlueCode() : "无",
                    ps.getMouthPlateCode() != null ? ps.getMouthPlateCode() : "无");
        }

        // 机台剩余产能不能超过本班台车约束的剩余量
        remainingCapacity = remainingCapacity.min(shiftRemainingCapacity);

        // 平均（免费）切换次数，超过后才算损失
        int avgSwitchCount = this.getParamAsInt(context, DjEngineConstants.PARAM_AVG_SWITCH_COUNT, 3);
        int switchCount = 0;
        BigDecimal totalSwitchLoss = BigDecimal.ZERO;
        BigDecimal mouthPlateSwitchTime = this.getParamAsDecimal(context, DjEngineConstants.PARAM_MOUTH_PLATE_SWITCH_TIME);

        // 逐个规格安排排产
        for (DjPaddingDemand spec : pendingSpecs) {
            if (!spec.isNeedProduce() || spec.getRemainingDemand() == null
                    || spec.getRemainingDemand().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (remainingCapacity.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            // 计算机台切换损失
            BigDecimal switchLoss = this.calcSwitchLoss(machine, lastSpecCode, spec, mouthPlateSwitchTime);
            if (lastSpecCode != null && !lastSpecCode.equals(spec.getPaddingCode())) {
                switchCount++;
                if (switchCount <= avgSwitchCount) {
                    switchLoss = BigDecimal.ZERO;
                }
            }

            // 生产量计算
            BigDecimal produceQty = calcProduceQty(spec, remainingCapacity.subtract(switchLoss),
                    shiftIndex, context);
            if (produceQty == null || produceQty.compareTo(BigDecimal.ZERO) <= 0) {
                context.appendLog("    规格 {0}：剩余产能不足以生产至少1台车，跳过",
                        context.getPaddingNameByCode(spec.getPaddingCode()));
                continue;
            }

            // 记录规格选择和满足的条件
            StringJoiner conditionJoiner = new StringJoiner("、");
            conditionJoiner.add("有缺口");
            if (lastSpecCode != null) {
                boolean isContinue = spec.getPaddingCode().equals(lastSpecCode);
                if (isContinue) {
                    conditionJoiner.add("续做");
                }
                boolean glueMatch = spec.getGlueCode() != null && lastSpecCode != null
                        && spec.getGlueCode().equals(findSpecByCode(demandList, lastSpecCode) != null
                                ? findSpecByCode(demandList, lastSpecCode).getGlueCode() : null);
                if (glueMatch) {
                    conditionJoiner.add("胶料相同");
                }
            }
            context.appendLog("  选择规格 {0}：{1}，切换损失={2}，计划生产={3}",
                    context.getPaddingNameByCode(spec.getPaddingCode()), conditionJoiner.toString(),
                    switchLoss, produceQty);
            if (spec.isSupplyGapMode()) {
                context.appendLog("    供应缺口模式：接班库存={0}，计划生产={1}（仅补本班消耗缺口）",
                        spec.getIncomingInventory(), produceQty);
            }

            // 记录排产结果
            this.recordSchedule(resultMap, spec, shiftIndex, produceQty, machineCode, context);

            // 扣减剩余需求
            spec.setRemainingDemand(spec.getRemainingDemand().subtract(produceQty));

            // 更新最后规格
            lastSpecCode = spec.getPaddingCode();
            lastSpecInShift.put(machineCode, lastSpecCode);

            // 扣减产能
            remainingCapacity = remainingCapacity.subtract(produceQty).subtract(switchLoss);
            machineCapacity.put(shiftIndex, remainingCapacity);

            // 累计规格切换损耗
            totalSwitchLoss = totalSwitchLoss.add(switchLoss);

            // 扣减本班台车约束剩余量
            shiftRemainingCapacity = shiftRemainingCapacity.subtract(produceQty);
            if (shiftRemainingCapacity.compareTo(BigDecimal.ZERO) < 0) {
                shiftRemainingCapacity = BigDecimal.ZERO;
            }

            // 扣减剩余台车数
            BigDecimal consumedTrolleys = produceQty.divide(trolleyStdCurlLength, 4, RoundingMode.HALF_UP)
                    .divide(trolleyFullRate, 4, RoundingMode.HALF_UP)
                    .setScale(0, RoundingMode.FLOOR);
            shiftRemainingTrolleys = shiftRemainingTrolleys.subtract(consumedTrolleys);
            if (shiftRemainingTrolleys.compareTo(BigDecimal.ZERO) < 0) {
                shiftRemainingTrolleys = BigDecimal.ZERO;
            }
        }

        // 补量阶段：主循环排产后机台仍有剩余产能，进行二次补量
        SupplementaryResult supplResult = this.executeSupplementaryProduction(
                pendingSpecs, shiftIndex, machineCode, resultMap, lastSpecInShift,
                machineCapacity, shiftRemainingCapacity, shiftRemainingTrolleys,
                trolleyStdCurlLength, trolleyFullRate, context);
        shiftRemainingCapacity = supplResult.getShiftRemainingCapacity();
        shiftRemainingTrolleys = supplResult.getShiftRemainingTrolleys();
        remainingCapacity = machineCapacity.get(shiftIndex);

        context.appendLog("  机台 {0} 班次 {1} 剩余产能：{2}，剩余台车数：{3}，规格切换损耗：{4}",
                machineCode, shiftIndex,
                remainingCapacity.max(BigDecimal.ZERO),
                shiftRemainingTrolleys.max(BigDecimal.ZERO), totalSwitchLoss);

        return new SupplementaryResult(shiftRemainingCapacity, shiftRemainingTrolleys);
    }

    /**
     * 班次结束：计算各规格交班库存并记录日志
     */
    private void calcEndOfShiftInventory(List<DjPaddingDemand> demandList, int shiftIndex,
            DjScheduleContext context, Map<String, DjScheduleResult> resultMap,
            Map<String, BigDecimal> handoverInventory) {
        context.appendLog("--- 班次 {0} 交班库存 ---", shiftIndex);
        for (DjPaddingDemand spec : demandList) {
            BigDecimal incomingInv = handoverInventory.getOrDefault(spec.getPaddingCode(), BigDecimal.ZERO);
            BigDecimal produceQtyThisShift = this.getScheduledQty(resultMap, spec.getPaddingCode(), shiftIndex);
            int formingClassForConsume = this.getFormingClassByShiftIndex(shiftIndex, context);
            if (formingClassForConsume < 1) {
                int formingShiftOffset = context.getFormingShiftOffset() != null
                        ? context.getFormingShiftOffset() : 0;
                formingClassForConsume = shiftIndex + formingShiftOffset;
            }
            BigDecimal consumeQtyThisShift = this.calcShiftConsume(
                    context, spec.getPaddingCode(), formingClassForConsume);
            BigDecimal newInventory = incomingInv.add(produceQtyThisShift).subtract(consumeQtyThisShift);
            if (newInventory.compareTo(BigDecimal.ZERO) < 0) {
                newInventory = BigDecimal.ZERO;
            }
            handoverInventory.put(spec.getPaddingCode(), newInventory);
            context.appendLog("  规格 {0}：接班={1} + 生产={2} - 消耗={3} = 交班={4}",
                    context.getPaddingNameByCode(spec.getPaddingCode()),
                    incomingInv, produceQtyThisShift, consumeQtyThisShift, newInventory);
        }
    }

    /**
     * 将排产结果 Map 转换为列表，填充工厂编码、排程日期等常量字段
     */
    private List<DjScheduleResult> buildScheduleResults(List<DjPaddingDemand> demandList,
            Map<String, DjScheduleResult> resultMap, DjScheduleContext context) {
        List<DjScheduleResult> results = new ArrayList<>();
        for (DjPaddingDemand spec : demandList) {
            String key = spec.getMachineCode() + ":" + spec.getPaddingCode();
            DjScheduleResult result = resultMap.get(key);
            if (result != null) {
                result.setFactoryCode(context.getFactoryCode());
                result.setScheduleDate(context.getScheduleDate());
                result.setPaddingCode(spec.getPaddingCode());
                result.setGlueCode(spec.getGlueCode());
                result.setTailFlag(
                        spec.isTailFinished() ? DjEngineConstants.TAIL_FLAG_YES : DjEngineConstants.TAIL_FLAG_NO);
                result.setReleaseStatus(DjEngineConstants.RELEASE_STATUS_UNPUBLISHED);
                result.setDataSource(DjEngineConstants.DATA_SOURCE_AUTO);
                result.setStockQty(
                        context.getEffectiveStockMap().getOrDefault(spec.getPaddingCode(), BigDecimal.ZERO));
                results.add(result);
            }
        }
        return results;
    }

    /**
     * 根据垫胶班次索引获取对应的成型班次序号
     * <p>优先通过成型班次配置表(CxShiftConfig)的映射获取，回退使用偏移量计算</p>
     *
     * @param shiftIndex 垫胶班次索引（1~6）
     * @param context 排程上下文
     * @return 成型班次序号（1~8），无法映射时返回 -1
     */
    private int getFormingClassByShiftIndex(int shiftIndex, DjScheduleContext context) {
        Map<String, Integer> cxShiftClassMap = context.getCxShiftClassMap();
        if (cxShiftClassMap != null && !cxShiftClassMap.isEmpty()) {
            int scheduleDay = context.getScheduleDays() != null
                    && shiftIndex - 1 < context.getScheduleDays().length
                    ? context.getScheduleDays()[shiftIndex - 1] : 1;
            String[] shiftClassMap = context.getShiftClassMap();
            if (shiftClassMap != null && shiftIndex - 1 < shiftClassMap.length) {
                String shiftName = shiftClassMap[shiftIndex - 1];
                String key = scheduleDay + "|" + shiftName;
                Integer cxShiftNum = cxShiftClassMap.get(key);
                if (cxShiftNum != null) {
                    return cxShiftNum;
                }
            }
        }
        // 回退：使用偏移量计算
        int formingShiftOffset = context.getFormingShiftOffset() != null ? context.getFormingShiftOffset() : 0;
        int formingClass = shiftIndex + formingShiftOffset;
        if (formingClass >= 1 && formingClass <= DjEngineConstants.CX_SHIFT_COUNT) {
            return formingClass;
        }
        return -1;
    }

    /**
     * 检查各规格在当前班次是否需要排产，并计算净需求量
     * <p>
     * 算法逻辑（按设计文档步骤3.4/5.1）：
     * 1. 计算当前接班库存可覆盖的成型生产班次数 coverableShiftCount
     * 2. 若 coverableShiftCount > scheduleThreshold（默认1），库存充足，不排产
     * 3. 若 coverableShiftCount ≤ scheduleThreshold，触发排产
     *    3.1 根据 T_DJ_DEPTH_CONFIG 匹配备库班数 n
     *    3.2 合计后续连续 n 个班的需求量计算净需求
     *    3.3 n>8 时使用最后3个班平均值预估超出部分，受月计划余量约束
     * 4. 已收尾规格净需求含损耗率
     * </p>
     */
    private void checkDemandForShift(List<DjPaddingDemand> demandList, int shiftIndex, DjScheduleContext context) {
        context.appendLog("--- 班次 {0} 消耗量 ---", shiftIndex);
        Map<String, BigDecimal> handoverInventory = context.getHandoverInventory();
        Map<String, Integer> paddingSupplyDepth = context.getPaddingSupplyDepth();
        Map<String, BigDecimal> paddingRemainingMap = context.getPaddingRemainingMap();

        // 成型班次偏移量：垫胶班次索引 → 成型班次索引 = 垫胶班次索引 + formingShiftOffset
        int formingShiftOffset = context.getFormingShiftOffset() != null ? context.getFormingShiftOffset() : 0;

        // 排产触发阈值：当前库存可覆盖班次数 ≤ 此值时触发排产
        int scheduleThreshold = this.getParamAsDecimal(context, DjEngineConstants.PARAM_SCHEDULE_THRESHOLD).intValue();

        for (DjPaddingDemand spec : demandList) {
            BigDecimal incomingInventory = handoverInventory.getOrDefault(spec.getPaddingCode(), BigDecimal.ZERO);
            spec.setIncomingInventory(incomingInventory);

            // 该规格的备库班数（排产深度）
            int specSupplyDepth = paddingSupplyDepth.getOrDefault(spec.getPaddingCode(),
                    DjEngineConstants.CX_SHIFT_COUNT);

            // ===== 步骤1：计算成型供应窗口内各班消耗量 =====
            // 窗口算法：垫胶当前班 shiftIndex → firstFormingClass（成型班次序号）
            // 窗口 = [firstFormingClass+1, firstFormingClass+depth]，跳过当前班（生产的垫胶下一个班才可用）
            // 所有计算在成型班次空间（class 1~8）内进行
            int firstFormingClass = this.getFormingClassByShiftIndex(shiftIndex, context);
            if (firstFormingClass < 1) {
                firstFormingClass = shiftIndex + formingShiftOffset;
            }

            // ===== 步骤1a：检查当前成型班次（firstFormingClass）消耗是否可被接班库存覆盖 =====
            // 垫胶本班生产的产品下个成型班才可用，因此本班成型消耗必须由接班库存承担
            BigDecimal currentShiftConsume = this.calcShiftConsume(
                    context, spec.getPaddingCode(), firstFormingClass);
            boolean hasCurrentGap = currentShiftConsume.compareTo(BigDecimal.ZERO) > 0
                    && incomingInventory.compareTo(currentShiftConsume) < 0;

            int windowStartClass = firstFormingClass + 1;
            int windowEndClass = Math.min(firstFormingClass + specSupplyDepth,
                    DjEngineConstants.CX_SHIFT_COUNT);

            // 新规格特殊处理：窗口结束班次后移覆盖提前备料天数
            // 窗口开始保持与正常逻辑一致（firstFormingClass + 1），结束班次至少到 开班班次 + advanceDays × shiftCountPerDay
            if (spec.isNewSpec()) {
                int advanceDays = this.getParamAsDecimal(context,
                        DjEngineConstants.PARAM_NEW_SPEC_ADVANCE_DAYS).intValue();
                if (advanceDays < 1) {
                    advanceDays = 1;
                } else if (advanceDays > 2) {
                    advanceDays = 2;
                }
                int minWindowEnd = windowStartClass + advanceDays * context.getShiftCountPerDay();
                windowEndClass = Math.min(Math.max(windowEndClass, minWindowEnd),
                        DjEngineConstants.CX_SHIFT_COUNT);
            }

            Map<Integer, BigDecimal> shiftConsume = new HashMap<>();
            BigDecimal windowDemandSum = BigDecimal.ZERO;
            for (int fc = windowStartClass; fc <= windowEndClass; fc++) {
                BigDecimal consume = this.calcShiftConsume(context,
                        spec.getPaddingCode(), fc);
                shiftConsume.put(fc, consume);
                windowDemandSum = windowDemandSum.add(consume);
            }
            // 供应窗口（当前班之后）是否有成型需求：非开产模式下若后续无需求，本班不生产则无补救机会
            spec.setWindowHasDemand(windowDemandSum.compareTo(BigDecimal.ZERO) > 0);

            // 供应窗口实际可用库存 = max(0, 接班库存 - 当前班成型消耗量)
            BigDecimal windowEffectiveInventory = incomingInventory.subtract(currentShiftConsume);
            if (windowEffectiveInventory.compareTo(BigDecimal.ZERO) < 0) {
                windowEffectiveInventory = BigDecimal.ZERO;
            }

            BigDecimal accumulateConsume = BigDecimal.ZERO;
            int coverableShiftCount = 0;
            for (int fc = windowStartClass; fc <= windowEndClass; fc++) {
                BigDecimal consume = shiftConsume.getOrDefault(fc, BigDecimal.ZERO);
                if (consume.compareTo(BigDecimal.ZERO) <= 0) {
                    coverableShiftCount++; // 无消耗的班次视为可覆盖
                    continue;
                }
                if (accumulateConsume.add(consume).compareTo(windowEffectiveInventory) <= 0) {
                    accumulateConsume = accumulateConsume.add(consume);
                    coverableShiftCount++;
                } else {
                    break; // 当前班次的消耗量已无法完全覆盖
                }
            }
            // 若当前成型班次存在供应缺口，可覆盖班次应设为0（表示即时缺口）
            if (hasCurrentGap) {
                coverableShiftCount = 0;
            }
            spec.setCoverableShiftCount(coverableShiftCount);

            // ===== 步骤2：判断是否触发排产 =====
            // 触发条件：
            //   a) 当前成型班次存在供应缺口（接班库存 < 本班成型消耗量），或
            //   b) 供应窗口可覆盖班次数 ≤ 阈值
            if (!hasCurrentGap && coverableShiftCount > scheduleThreshold) {
                spec.setNeedProduce(false); // 库存充足，不排产
                spec.setRemainingDemand(BigDecimal.ZERO); // 清空剩余需求，避免残留到后续班次
                // 从成型班次配置表获取当班消耗量：通过(scheduleDay, shiftName)匹配成型班次序号
                BigDecimal shiftConsumeQty = this.getShiftConsumeQty(shiftIndex, context, spec);
                if (shiftConsumeQty.compareTo(BigDecimal.ZERO) > 0) { // 如果本班消耗量大于0，则还是需要记录日志
                    this.appendShiftConsumeLog(shiftIndex, context, spec, specSupplyDepth, firstFormingClass,
                            windowStartClass, windowEndClass, shiftConsume, shiftConsumeQty);
                }
                continue;
            }

            // ===== 步骤3：触发排产，计算净需求量 =====
            spec.setNeedProduce(true);
            BigDecimal netDemand;

            if (firstFormingClass + specSupplyDepth <= DjEngineConstants.CX_SHIFT_COUNT) {
                // 供应窗口全部在成型计划范围内
                // 如果有量试/试制需求，用量试合并窗口计算（量试需求提前 N 个班次排产）
                BigDecimal demandInWindow;
                int trialAdvanceShifts = this.getTrialAdvanceShifts(context);
                if (trialAdvanceShifts > 0 && this.hasTrialConsumption(context, spec.getPaddingCode())) {
                    demandInWindow = this.calcCombinedWindowDemand(context, spec.getPaddingCode(),
                            firstFormingClass, specSupplyDepth, trialAdvanceShifts);
                } else {
                    demandInWindow = BigDecimal.ZERO;
                    for (int fc = windowStartClass; fc <= windowEndClass; fc++) {
                        demandInWindow = demandInWindow.add(this.calcShiftConsume(
                                context, spec.getPaddingCode(), fc));
                    }
                }
                // 供应窗口实际可用库存 = max(0, 接班库存 - 本班成型消耗)
                BigDecimal availableForWindow = incomingInventory.subtract(currentShiftConsume);
                if (availableForWindow.compareTo(BigDecimal.ZERO) < 0) {
                    availableForWindow = BigDecimal.ZERO;
                }
                netDemand = demandInWindow.subtract(availableForWindow);
                if (netDemand.compareTo(BigDecimal.ZERO) < 0) {
                    netDemand = BigDecimal.ZERO;
                }
            } else {
                // 供应窗口超出成型计划（firstFormingClass + depth > 8）
                // part1: 窗口内可计算的成型班次部分（含量试合并窗口）
                BigDecimal part1;
                int trialAdvanceShifts = this.getTrialAdvanceShifts(context);
                if (trialAdvanceShifts > 0 && this.hasTrialConsumption(context, spec.getPaddingCode())) {
                    part1 = this.calcCombinedWindowDemand(context, spec.getPaddingCode(),
                            firstFormingClass, specSupplyDepth, trialAdvanceShifts);
                } else {
                    part1 = BigDecimal.ZERO;
                    for (int fc = windowStartClass; fc <= windowEndClass; fc++) {
                        part1 = part1.add(this.calcShiftConsume(
                                context, spec.getPaddingCode(), fc));
                    }
                }
                // 供应窗口实际可用库存 = max(0, 接班库存 - 本班成型消耗)
                BigDecimal availableForWindow = incomingInventory.subtract(currentShiftConsume);
                if (availableForWindow.compareTo(BigDecimal.ZERO) < 0) {
                    availableForWindow = BigDecimal.ZERO;
                }
                part1 = part1.subtract(availableForWindow);
                if (part1.compareTo(BigDecimal.ZERO) < 0) {
                    part1 = BigDecimal.ZERO;
                }

                // 最后3个成型班次消耗量之和（成型班次第6/7/8班 = 成型 class6/7/8）
                BigDecimal last3Sum = BigDecimal.ZERO;
                for (int i = 6; i <= DjEngineConstants.CX_SHIFT_COUNT; i++) {
                    last3Sum = last3Sum.add(this.calcShiftConsume(
                            context, spec.getPaddingCode(), i));
                }
                // 如果最后3班无数据，用窗口内可计算班次的消耗量总计兜底
                if (last3Sum.compareTo(BigDecimal.ZERO) <= 0) {
                    for (int fc = windowStartClass; fc <= DjEngineConstants.CX_SHIFT_COUNT; fc++) {
                        last3Sum = last3Sum.add(this.calcShiftConsume(
                                context, spec.getPaddingCode(), fc));
                    }
                }
                BigDecimal avgLast3Shifts = last3Sum.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);

                // 超出部分班次数
                int exceedShiftCount = firstFormingClass + specSupplyDepth - DjEngineConstants.CX_SHIFT_COUNT;
                BigDecimal estimatedExceed = avgLast3Shifts.multiply(BigDecimal.valueOf(exceedShiftCount));

                // 月计划剩余量约束（仅当有月计划数据时约束预估，否则使用完整预估值）
                BigDecimal paddingRemaining = paddingRemainingMap != null
                        ? paddingRemainingMap.getOrDefault(spec.getPaddingCode(), BigDecimal.ZERO)
                        : BigDecimal.ZERO;
                boolean hasPaddingRemaining = paddingRemainingMap != null
                        && paddingRemainingMap.containsKey(spec.getPaddingCode())
                        && paddingRemaining.compareTo(BigDecimal.ZERO) > 0;
                BigDecimal part2 = BigDecimal.ZERO;
                if (!spec.isTailFinished()) {
                    part2 = estimatedExceed;
                    if (hasPaddingRemaining) {
                        BigDecimal part1PlusPart2 = part1.add(part2);
                        if (part1PlusPart2.compareTo(paddingRemaining) > 0) {
                            part2 = paddingRemaining.subtract(part1);
                            if (part2.compareTo(BigDecimal.ZERO) < 0) {
                                part2 = BigDecimal.ZERO;
                            }
                            spec.setConstrainedEstimatedPart2(part2);
                        }
                    }
                }
                netDemand = part1.add(part2);
            }

            // ===== 步骤3a：当前成型班次有供应缺口时，叠加本班消耗缺口 =====
            // 本班成型消耗由接班库存承担，若库存不足则缺口（currentShiftConsume - incomingInventory）
            // 需由本班生产填补。供应窗口实际可用库存已在上方扣除了本班消耗，此处只补缺口。
            if (hasCurrentGap) {
                netDemand = netDemand.add(currentShiftConsume.subtract(incomingInventory));
            }

            // ===== 步骤4：已收尾规格净需求含损耗率 =====
            if (spec.isTailFinished()) {
                BigDecimal lossRate = this.getLossRate(spec.getPaddingCode(), spec.getMachineCode(), context);
                // 存储收尾损耗信息（日志输出用）
                spec.setPreLossRateDemand(netDemand);
                spec.setLossRatePercent(lossRate);
                // 损耗率存储为百分比值（如 2 表示 2%），需除以 100
                BigDecimal lossRateDecimal = lossRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                netDemand = netDemand.multiply(BigDecimal.ONE.add(lossRateDecimal)).setScale(2, RoundingMode.HALF_UP);
            }

            // 每个班次重新计算剩余需求，基于当前接班库存和排产深度
            spec.setRemainingDemand(netDemand);

            // 从成型班次配置表获取当班消耗量：通过(scheduleDay, shiftName)匹配成型班次序号
            BigDecimal shiftConsumeQty = this.getShiftConsumeQty(shiftIndex, context, spec);
            this.appendShiftConsumeLog(shiftIndex, context, spec, specSupplyDepth, firstFormingClass,
                    windowStartClass, windowEndClass, shiftConsume, shiftConsumeQty);
        }
    }
    
    /**
     * 计算合并窗口需求量（含正式窗口 + 量试提前窗口）
     * <p>
     * 正式窗口 = [firstFormingClass+1, firstFormingClass+depth]<br>
     * 量试窗口 = [max(1, firstFormingClass+1-trialAdvance), firstFormingClass-trialAdvance+depth]<br>
     * 两个窗口取并集：属于正式窗口的班次取全部消耗量，仅属量试窗口的班次只取量试部分消耗量，
     * 仅属正式窗口的班次取正式部分消耗量（总量 - 量试量）。
     * </p>
     *
     * @param context              排产上下文
     * @param paddingCode          垫胶编码
     * @param firstFormingClass    当前垫胶班次对应的成型班次
     * @param supplyDepth          供应窗口深度
     * @param trialAdvanceShifts   量试提前班次数
     * @return 合并窗口总需求量
     */
    private BigDecimal calcCombinedWindowDemand(DjScheduleContext context, String paddingCode,
            int firstFormingClass, int supplyDepth, int trialAdvanceShifts) {
        int normalStart = firstFormingClass + 1;
        int normalEnd = Math.min(firstFormingClass + supplyDepth, DjEngineConstants.CX_SHIFT_COUNT);
        int trialStart = Math.max(firstFormingClass + 1 - trialAdvanceShifts, 1);
        int trialEnd = Math.min(firstFormingClass - trialAdvanceShifts + supplyDepth, DjEngineConstants.CX_SHIFT_COUNT);
        int combinedStart = Math.min(normalStart, trialStart);
        int combinedEnd = Math.max(normalEnd, trialEnd);
        BigDecimal total = BigDecimal.ZERO;
        for (int fc = combinedStart; fc <= combinedEnd; fc++) {
            BigDecimal fcTotal = this.calcShiftConsume(context, paddingCode, fc);
            BigDecimal fcTrial = this.calcTrialShiftConsume(context, paddingCode, fc);
            boolean inNormal = fc >= normalStart && fc <= normalEnd;
            boolean inTrial = fc >= trialStart && fc <= trialEnd;
            if (inNormal && inTrial) {
                total = total.add(fcTotal);
            } else if (inTrial) {
                total = total.add(fcTrial);
            } else if (inNormal) {
                total = total.add(fcTotal.subtract(fcTrial));
            }
        }
        return total;
    }
    
    /**
     * 添加班次消耗量日志
     * @param shiftIndex         垫胶班次序号
     * @param context            上下文
     * @param spec               垫胶需求
     * @param specSupplyDepth    排产深度
     * @param firstFormingClass  当前垫胶班次对应的成型班次序号（用于判断是否超出8班）
     * @param windowStartClass   供应窗口起始成型班次
     * @param windowEndClass     供应窗口结束成型班次
     * @param shiftConsume       供应窗口各成型班次消耗量 Map<formingClass, consumeQty>
     * @param shiftConsumeQty    当班消耗量
     */
    private void appendShiftConsumeLog(int shiftIndex, DjScheduleContext context, DjPaddingDemand spec,
            int specSupplyDepth, int firstFormingClass, int windowStartClass, int windowEndClass,
            Map<Integer, BigDecimal> shiftConsume, BigDecimal shiftConsumeQty) {
        // 遍历成型窗口班次，按示方书版本匹配施工后取胎胚代码（与 calcShiftConsume 同口径）
        StringBuilder shiftInfo = new StringBuilder();
        if (windowStartClass <= windowEndClass) {
            for (int fc = windowStartClass; fc <= windowEndClass; fc++) {
                BigDecimal consume = shiftConsume.getOrDefault(fc, BigDecimal.ZERO);
                shiftInfo.append("班").append(fc).append("=").append(consume).append(" ");
            }
        }
        // 供应窗口超出成型8个班时，追加预估班次消耗信息（收尾规格无需预估）
        if (firstFormingClass + specSupplyDepth > DjEngineConstants.CX_SHIFT_COUNT && !spec.isTailFinished()) {
            BigDecimal last3Sum = BigDecimal.ZERO;
            for (int i = 6; i <= DjEngineConstants.CX_SHIFT_COUNT; i++) {
                last3Sum = last3Sum.add(this.calcShiftConsume(context, spec.getPaddingCode(), i));
            }
            if (last3Sum.compareTo(BigDecimal.ZERO) <= 0) {
                for (int fc = windowStartClass; fc <= DjEngineConstants.CX_SHIFT_COUNT; fc++) {
                    last3Sum = last3Sum.add(this.calcShiftConsume(context, spec.getPaddingCode(), fc));
                }
            }
            BigDecimal avgLast3Shifts = last3Sum.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
            int exceedShiftCount = firstFormingClass + specSupplyDepth - DjEngineConstants.CX_SHIFT_COUNT;
            shiftInfo.append("(预估: 超出").append(exceedShiftCount).append("班×").append(avgLast3Shifts);
            // 被月计划余量约束时追加限制说明
            if (spec.getConstrainedEstimatedPart2() != null) {
                shiftInfo.append("，月计划余量限制：").append(spec.getConstrainedEstimatedPart2().setScale(2, RoundingMode.HALF_UP));
            }
            shiftInfo.append(") ");
        }
        // 对应胎胚：遍历当班+窗口范围内所有成型班次，收集关联胎胚
        // windowEndClass 在主流程中已根据新规格/普通规格做了相应调整
        Set<String> embryoSet = new HashSet<>();
        List<CxScheduleResult> cxScheduleList = context.getCxScheduleList();
        if (firstFormingClass <= windowEndClass) {
            for (int fc = firstFormingClass; fc <= windowEndClass; fc++) {
                for (CxScheduleResult cx : cxScheduleList) {
                    MdmConstructionInfo construction = this.resolveConstructionForShift(cx, fc, context);
                    if (construction != null && spec.getPaddingCode().equals(construction.getPaddingCode())) {
                        embryoSet.add(cx.getEmbryoCode());
                    }
                }
            }
        }
        String embryoCodes = String.join("/", embryoSet);
        // 胎胚为空时不输出日志（对应垫胶班次在该成型班次上无排产计划）
        if (embryoCodes.isEmpty()) {
            return;
        }
        // 对应成型机
        Set<String> cxMachineSet = context.getPaddingCxMachineSet().get(spec.getPaddingCode());
        String cxMachineCodes = cxMachineSet != null ? String.join("/", cxMachineSet) : "无";
        context.appendLog("  规格 {0}：胎胚={1}，成型机={2}，深度={3}班，单耗={4}，当班消耗={5}，成型窗口内计划：{6}",
                DjScheduleContext.buildDisplayName(spec.getPaddingName(), spec.getPaddingCode()), embryoCodes,
                cxMachineCodes, specSupplyDepth, spec.getUnitConsume(), shiftConsumeQty, shiftInfo.toString());
    }
    
    /**
     * 从成型班次配置表获取当班消耗量
     * @param shiftIndex
     * @param context
     * @param cxScheduleList
     * @param constructionMap
     * @param spec
     * @return
     */
    private BigDecimal getShiftConsumeQty(int shiftIndex, DjScheduleContext context,
            DjPaddingDemand spec) {
        BigDecimal shiftConsumeQty = BigDecimal.ZERO;
        Map<String, Integer> cxShiftClassMap = context.getCxShiftClassMap();
        int scheduleDay = context.getScheduleDays() != null
                && shiftIndex - 1 < context.getScheduleDays().length
                ? context.getScheduleDays()[shiftIndex - 1] : 1;
        String shiftName = context.getShiftClassMap()[shiftIndex - 1];
        String cxShiftKey = scheduleDay + "|" + shiftName;
        Integer cxShiftNum = cxShiftClassMap != null ? cxShiftClassMap.get(cxShiftKey) : null;
        if (cxShiftNum != null) {
            return this.calcShiftConsume(context,
                    spec.getPaddingCode(), cxShiftNum);
        }
        return shiftConsumeQty;
    }

    /**
     * 计算机台在班次的有效可用产能（含检修停机扣减）
     */
    private Map<String, Map<Integer, BigDecimal>> calcEffectiveCapacity(Map<String, DjMachineInfo> machineMap,
            DjScheduleContext context) {
        Map<String, Map<Integer, BigDecimal>> capacityMatrix = new HashMap<>();

        for (Map.Entry<String, DjMachineInfo> entry : machineMap.entrySet()) {
            String machineCode = entry.getKey();
            DjMachineInfo machine = entry.getValue();
            Map<Integer, BigDecimal> shiftCapacities = new HashMap<>();

            // 如果机台定额为空，跳过
            if (machine.getQuata() == null || machine.getQuata().compareTo(BigDecimal.ZERO) <= 0) {
                capacityMatrix.put(machineCode, shiftCapacities);
                continue;
            }

            BigDecimal baseQuata = machine.getQuata();

            for (int shiftIndex = 1; shiftIndex <= DjEngineConstants.SHIFT_COUNT; shiftIndex++) {
                // 先判断是否可用
                if (!this.isMachineAvailable(machine, shiftIndex, context.getShiftClassMap())) {
                    shiftCapacities.put(shiftIndex, BigDecimal.ZERO);
                    continue;
                }

                // 扣减检修停机损失
                BigDecimal effectiveCapacity = baseQuata;
                // 查找该机台该班次的停机计划
                // 检修计划在上下文中维护
                if (context.getMaintenanceMap() != null) {
                    List<DjMachineMaintenance> maintenanceList = context.getMaintenanceMap().get(machineCode);
                    if (maintenanceList != null) {
                        for (DjMachineMaintenance maintenance : maintenanceList) {
                            if (this.isMaintenanceInShift(maintenance, shiftIndex, context.getScheduleDate(),
                                    context.getShiftClassMap())) {
                                BigDecimal stopHours = this.calcStopHours(maintenance);
                                BigDecimal lossQty = stopHours
                                        .divide(DjEngineConstants.SHIFT_HOURS, 4, RoundingMode.HALF_UP)
                                        .multiply(baseQuata);
                                effectiveCapacity = effectiveCapacity.subtract(lossQty);
                            }
                        }
                    }
                }

                if (effectiveCapacity.compareTo(BigDecimal.ZERO) < 0) {
                    effectiveCapacity = BigDecimal.ZERO;
                }
                shiftCapacities.put(shiftIndex, effectiveCapacity.setScale(2, RoundingMode.HALF_UP));
            }
            capacityMatrix.put(machineCode, shiftCapacities);
        }
        return capacityMatrix;
    }

    /**
     * 计算每班的台车工装约束上限（班次总产量上限）
     * <p>公式：班次总产量上限 = (工装总数 - (有效库存 / 标准卷曲米数) × 整车率) × 台车容量</p>
     *
     * @param context 排产上下文
     * @return 班次总产量上限（米），若未配置工装总数则返回 null（不限制）
     */
    private BigDecimal calcShiftTrolleyLimit(DjScheduleContext context) {
        Map<String, DjParams> paramsMap = context.getParamsMap();

        // 工装（台车）总数
        BigDecimal toolTotalNum = this.getParamAsDecimal(paramsMap, DjEngineConstants.PARAM_TOOL_TOTAL_NUM);
        if (toolTotalNum.compareTo(BigDecimal.ZERO) <= 0) {
            return null; // 未配置台车总数，不限制
        }

        // 整车率
        BigDecimal trolleyFullRate = this.getParamAsDecimal(paramsMap, DjEngineConstants.PARAM_TROLLEY_FULL_RATE);
        // 数据库存储百分比值（如 80），转换为小数（0.8）
        trolleyFullRate = trolleyFullRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        if (trolleyFullRate.compareTo(BigDecimal.ZERO) <= 0) {
            trolleyFullRate = BigDecimal.ONE; // 默认整车率为100%
        }

        // 标准卷曲米数（台车容量）
        BigDecimal standardCurlLength = this.getParamAsDecimal(paramsMap,
                DjEngineConstants.PARAM_STANDARD_CRIMP_LENGTH);
        if (standardCurlLength.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        // 有效库存总和
        Map<String, BigDecimal> effectiveStockMap = context.getEffectiveStockMap();
        BigDecimal totalEffectiveStock = effectiveStockMap.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 计算：工装总数 - (有效库存 / 卷曲米数) × 整车率
        BigDecimal stockTrolleys = totalEffectiveStock.divide(standardCurlLength, 4, RoundingMode.HALF_UP)
                .multiply(trolleyFullRate);
        BigDecimal availableTrolleys = toolTotalNum.subtract(stockTrolleys);
        if (availableTrolleys.compareTo(BigDecimal.ZERO) < 0) {
            availableTrolleys = BigDecimal.ZERO;
        }

        // × 台车容量 = 班次总产量上限
        BigDecimal limit = availableTrolleys.multiply(standardCurlLength).setScale(2, RoundingMode.HALF_UP);
        log.debug("台车约束计算：工装总数={}, 有效库存={}, 整车率={}, 标准卷曲={}, 可用台车={}, 上限={}",
                toolTotalNum, totalEffectiveStock, trolleyFullRate, standardCurlLength, availableTrolleys, limit);
        return limit;
    }

    /**
     * 计算本班初始剩余台车数
     * <p>公式：工装总数 - 交班库存 / 卷曲米数 / 整车率，结果向下取整</p>
     *
     * @param context           排产上下文
     * @param handoverInventory 当前交班库存 Map<paddingCode, inventory>
     * @return 剩余台车数（向下取整）
     */
    private BigDecimal calcRemainingTrolleys(DjScheduleContext context, Map<String, BigDecimal> handoverInventory) {
        Map<String, DjParams> paramsMap = context.getParamsMap();

        // 工装（台车）总数
        BigDecimal toolTotalNum = this.getParamAsDecimal(paramsMap, DjEngineConstants.PARAM_TOOL_TOTAL_NUM);

        // 标准卷曲米数
        BigDecimal standardCurlLength = this.getParamAsDecimal(paramsMap,
                DjEngineConstants.PARAM_STANDARD_CRIMP_LENGTH);

        // 整车率
        BigDecimal trolleyFullRate = this.getParamAsDecimal(paramsMap, DjEngineConstants.PARAM_TROLLEY_FULL_RATE);
        // 数据库存储百分比值（如 80），转换为小数（0.8）
        trolleyFullRate = trolleyFullRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        if (trolleyFullRate.compareTo(BigDecimal.ZERO) <= 0) {
            trolleyFullRate = BigDecimal.ONE;
        }

        // 交班库存总和
        BigDecimal totalHandoverInv = handoverInventory.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 计算：工装总数 - 交班库存 / 卷曲米数 / 整车率
        BigDecimal invTrolleys = totalHandoverInv.divide(standardCurlLength, 4, RoundingMode.HALF_UP)
                .divide(trolleyFullRate, 4, RoundingMode.HALF_UP);
        BigDecimal remaining = toolTotalNum.subtract(invTrolleys);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            remaining = BigDecimal.ZERO;
        }
        // 向下取整
        return remaining.setScale(0, RoundingMode.FLOOR);
    }

    /**
     * 计算某班次的实际可用产能上限（取台车约束与所有开机机台产能之和的较小值）
     *
     * @param capacityMatrix  机台产能矩阵 Map<machineCode, Map<shiftIndex, remainingCapacity>>
     * @param shiftIndex      班次索引
     * @param shiftTrolleyLimit 台车工装约束上限，可为 null（不限制）
     * @return 本班实际可用产能上限
     */
    private BigDecimal calcShiftTotalCapacity(Map<String, Map<Integer, BigDecimal>> capacityMatrix, int shiftIndex,
            BigDecimal shiftTrolleyLimit) {
        // 计算所有开机机台在本班的产能之和
        BigDecimal totalMachineCapacity = capacityMatrix.values().stream()
                .map(mc -> mc.getOrDefault(shiftIndex, BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 取台车约束与机台产能之和的较小值
        if (shiftTrolleyLimit != null && shiftTrolleyLimit.compareTo(BigDecimal.ZERO) > 0
                && shiftTrolleyLimit.compareTo(totalMachineCapacity) < 0) {
            log.debug("班次 {} 台车约束({}) 小于机台总产能({}), 取台车约束值", shiftIndex, shiftTrolleyLimit, totalMachineCapacity);
            return shiftTrolleyLimit;
        }
        return totalMachineCapacity;
    }

    /**
     * 判断检修计划是否落在某班次内
     */
    private boolean isMaintenanceInShift(DjMachineMaintenance maintenance, int shiftIndex, Date scheduleDate,
            String[] shiftClassMap) {
        if (maintenance.getStopStartTime() == null || maintenance.getStopEndTime() == null) {
            return false;
        }
        // 通过停机班次字段匹配
        if (maintenance.getStopShift() != null) {
            String shiftClass = shiftClassMap[shiftIndex - 1];
            return shiftClass.equals(maintenance.getStopShift());
        }
        return false;
    }

    /**
     * 计算停机时长（小时）
     */
    private BigDecimal calcStopHours(DjMachineMaintenance maintenance) {
        long diff = maintenance.getStopEndTime().getTime() - maintenance.getStopStartTime().getTime();
        return BigDecimal.valueOf(diff).divide(BigDecimal.valueOf(3600000), 2, RoundingMode.HALF_UP);
    }

    /**
     * 判断机台在某班次是否可用
     */
    private boolean isMachineAvailable(DjMachineInfo machine, int shiftIndex, String[] shiftClassMap) {
        if (machine.getStatus() != null && !DjEngineConstants.MACHINE_STATUS_ENABLED.equals(machine.getStatus())) {
            return false;
        }
        String openMachineClass = machine.getOpenMachineClass();
        if (StringUtils.isEmpty(openMachineClass)) {
            return false;
        }
        // shiftIndex 对应 classIndex
        String classIndex = shiftClassMap[shiftIndex - 1];
        return openMachineClass.contains(classIndex);
    }

    /**
     * 获取某机台在某班的待排产规格列表
     */
    private List<DjPaddingDemand> getSpecsByMachine(String machineCode, List<DjPaddingDemand> demandList) {
        return demandList.stream().filter(d -> machineCode.equals(d.getMachineCode())).collect(Collectors.toList());
    }

    /**
     * 构建优先级比较器（10级规则）
     */
    private Comparator<DjPaddingDemand> buildPriorityComparator(List<DjPaddingDemand> demandList, String lastSpecCode,
            DjScheduleContext context, int shiftIndex) {
        return (a, b) -> {
            // 1. 本班有供应缺口且后续窗口无需求的最优先（若本班不生产则无补救机会）
            boolean aCritical = a.isNeedProduce() && !a.isWindowHasDemand();
            boolean bCritical = b.isNeedProduce() && !b.isWindowHasDemand();
            if (aCritical != bCritical) {
                return aCritical ? -1 : 1;
            }

            // 2. 本班有缺口优先
            if (a.isNeedProduce() != b.isNeedProduce()) {
                return a.isNeedProduce() ? -1 : 1;
            }

            // 3. 新规格且无库存优先（在补供应缺口的规格之后）
            boolean aNoInvNewSpec = a.isNewSpec() && a.getIncomingInventory() != null
                    && a.getIncomingInventory().compareTo(BigDecimal.ZERO) <= 0;
            boolean bNoInvNewSpec = b.isNewSpec() && b.getIncomingInventory() != null
                    && b.getIncomingInventory().compareTo(BigDecimal.ZERO) <= 0;
            if (aNoInvNewSpec != bNoInvNewSpec) {
                return aNoInvNewSpec ? -1 : 1;
            }

            // 4. 规格续作优先
            boolean aIsLast = a.getPaddingCode() != null && a.getPaddingCode().equals(lastSpecCode);
            boolean bIsLast = b.getPaddingCode() != null && b.getPaddingCode().equals(lastSpecCode);
            if (aIsLast != bIsLast) {
                return aIsLast ? -1 : 1;
            }

            // 5. 胶料相同 + 口型相同优先
            if (lastSpecCode != null) {
                DjPaddingDemand lastSpec = this.findSpecByCode(demandList, lastSpecCode);
                if (lastSpec != null) {
                    boolean aMatchGlueAndMouth = this.isGlueAndMouthMatch(a, lastSpec);
                    boolean bMatchGlueAndMouth = this.isGlueAndMouthMatch(b, lastSpec);
                    if (aMatchGlueAndMouth != bMatchGlueAndMouth) {
                        return aMatchGlueAndMouth ? -1 : 1;
                    }

                    // 6. 胶料相同优先
                    boolean aGlueMatch = a.getGlueCode() != null && a.getGlueCode().equals(lastSpec.getGlueCode());
                    boolean bGlueMatch = b.getGlueCode() != null && b.getGlueCode().equals(lastSpec.getGlueCode());
                    if (aGlueMatch != bGlueMatch) {
                        return aGlueMatch ? -1 : 1;
                    }

                    // 7. 口型相同优先
                    boolean aMouthMatch = a.getMouthPlateCode() != null
                            && a.getMouthPlateCode().equals(lastSpec.getMouthPlateCode());
                    boolean bMouthMatch = b.getMouthPlateCode() != null
                            && b.getMouthPlateCode().equals(lastSpec.getMouthPlateCode());
                    if (aMouthMatch != bMouthMatch) {
                        return aMouthMatch ? -1 : 1;
                    }
                }
            }

            // 8. 胶料组相同优先
            Map<String, String> glueGroupMap = context.getGlueGroupMap();
            String aGroup = glueGroupMap != null ? glueGroupMap.get(a.getGlueCode()) : null;
            String bGroup = glueGroupMap != null ? glueGroupMap.get(b.getGlueCode()) : null;
            if (aGroup != null && bGroup != null && !aGroup.equals(bGroup)) {
                Integer aGroupOrder = context.getGlueGroupOrderMap() != null
                        ? context.getGlueGroupOrderMap().get(aGroup)
                        : null;
                Integer bGroupOrder = context.getGlueGroupOrderMap() != null
                        ? context.getGlueGroupOrderMap().get(bGroup)
                        : null;
                if (aGroupOrder != null && bGroupOrder != null && !aGroupOrder.equals(bGroupOrder)) {
                    return Integer.compare(aGroupOrder, bGroupOrder);
                }
            }

            // 9. 胶料序号升序
            Map<String, Integer> glueOrderMap = context.getGlueOrderMap();
            Integer aSeq = glueOrderMap != null ? glueOrderMap.get(a.getGlueCode()) : null;
            Integer bSeq = glueOrderMap != null ? glueOrderMap.get(b.getGlueCode()) : null;
            if (aSeq != null && bSeq != null && !aSeq.equals(bSeq)) {
                return Integer.compare(aSeq, bSeq);
            }

            // 10. 库消比升序
            BigDecimal aRatio = this.calcStockConsumeRatio(a, context);
            BigDecimal bRatio = this.calcStockConsumeRatio(b, context);
            if (aRatio != null && bRatio != null) {
                int ratioCmp = aRatio.compareTo(bRatio);
                if (ratioCmp != 0) {
                    return ratioCmp;
                }
            }

            // 11. 垫胶编码升序（兜底）
            if (a.getPaddingCode() != null && b.getPaddingCode() != null) {
                return a.getPaddingCode().compareTo(b.getPaddingCode());
            }
            return 0;
        };
    }

    /**
     * 按规格编码查找规格
     */
    private DjPaddingDemand findSpecByCode(List<DjPaddingDemand> demandList, String paddingCode) {
        return demandList.stream().filter(d -> paddingCode.equals(d.getPaddingCode())).findFirst().orElse(null);
    }

    /**
     * 判断两个规格的胶料和口型是否相同
     */
    private boolean isGlueAndMouthMatch(DjPaddingDemand a, DjPaddingDemand b) {
        return Objects.equals(a.getGlueCode(), b.getGlueCode())
                && Objects.equals(a.getMouthPlateCode(), b.getMouthPlateCode());
    }

    /**
     * 计算库消比
     */
    private BigDecimal calcStockConsumeRatio(DjPaddingDemand spec, DjScheduleContext context) {
        // 从成型计划动态计算前3个班的日均消耗量
        String paddingCode = spec.getPaddingCode();

        BigDecimal avgDailyConsume = BigDecimal.ZERO;
        int count = 0;
        for (int i = 1; i <= 3; i++) {
            BigDecimal consume = this.calcShiftConsume(context, paddingCode, i);
            if (consume.compareTo(BigDecimal.ZERO) > 0) {
                avgDailyConsume = avgDailyConsume.add(consume);
                count++;
            }
        }
        if (count > 0) {
            avgDailyConsume = avgDailyConsume.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        }
        if (avgDailyConsume.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal inventory = spec.getIncomingInventory() != null ? spec.getIncomingInventory() : BigDecimal.ZERO;
        return inventory.divide(avgDailyConsume, 4, RoundingMode.HALF_UP);
    }

    /**
     * 获取损耗率
     */
    private BigDecimal getLossRate(String paddingCode, String machineCode, DjScheduleContext context) {
        if (context.getLossRateMap() != null) {
            String key1 = paddingCode + "#" + machineCode;
            BigDecimal rate = context.getLossRateMap().get(key1);
            if (rate != null) {
                return rate;
            }
        }
        // 从参数获取全局默认损耗率
        BigDecimal defaultLoss = this.getParamAsDecimal(context, DjEngineConstants.PARAM_LOSS_RATE);
        return defaultLoss != null ? defaultLoss : BigDecimal.ZERO;
    }

    /**
     * 计算机台切换损失
     */
    private BigDecimal calcSwitchLoss(DjMachineInfo machine, String lastSpecCode, DjPaddingDemand currentSpec,
            BigDecimal mouthPlateSwitchTime) {
        // 本班第一个规格或续做同一规格，无切换损失
        if (lastSpecCode == null || lastSpecCode.equals(currentSpec.getPaddingCode())) {
            return BigDecimal.ZERO;
        }
        // 需要切换，需知道上一规格的胶料和口型
        // 这里简化处理：按切换胶料处理（全量切换），调用方需传入上下文中的上一规格信息
        // 切换损失 = (切换时长（分钟） / 480分钟) × 定额
        BigDecimal quata = machine.getQuata() != null ? machine.getQuata() : BigDecimal.ZERO;
        return mouthPlateSwitchTime.divide(DjEngineConstants.SHIFT_MINUTES, 4, RoundingMode.HALF_UP).multiply(quata).setScale(2,
                RoundingMode.HALF_UP);
    }

    /**
     * 计算生产量
     * <p>
     * 已收尾规格精确排产，不要求台车整倍数；
     * 未收尾规格需按台车容量向上取整台车。
     * 受 SYS1401013（单规格每班最大排产量）约束：
     * - 已收尾规格超过限制时直接截断；
     * - 未收尾规格截断后再向上取整台车，因取整车多出的部分允许超过限制。
     * 供应缺口填补模式（supplyGapMode=true）时，补本班成型消耗缺口 + 安全水位(SYS1401005)，
     * 生产量受缺口量约束，超出部分留待后续班次排产。
     * </p>
     *
     * @param spec              垫胶需求规格
     * @param remainingCapacity 机台本班剩余产能
     * @param shiftIndex        当前班次索引（用于计算成型消耗量）
     * @param context           排产上下文（用于读取参数）
     * @return 本班生产量（米）
     */
    private BigDecimal calcProduceQty(DjPaddingDemand spec, BigDecimal remainingCapacity,
            int shiftIndex, DjScheduleContext context) {
        if (remainingCapacity == null || remainingCapacity.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal remainingDemand = spec.getRemainingDemand() != null ? spec.getRemainingDemand() : BigDecimal.ZERO;
        if (remainingDemand.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // 供应缺口填补模式：仅补本班成型消耗缺口，不补供应窗口净需求
        if (spec.isSupplyGapMode()) {
            int firstFormingClass = this.getFormingClassByShiftIndex(shiftIndex, context);
            if (firstFormingClass < 1) {
                int formingShiftOffset = context.getFormingShiftOffset() != null ? context.getFormingShiftOffset() : 0;
                firstFormingClass = shiftIndex + formingShiftOffset;
            }
            BigDecimal currentShiftConsume = this.calcShiftConsume(
                    context, spec.getPaddingCode(), firstFormingClass);
            BigDecimal incomingInv = spec.getIncomingInventory() != null ? spec.getIncomingInventory() : BigDecimal.ZERO;
            BigDecimal supplyGap = currentShiftConsume.subtract(incomingInv);
            if (supplyGap.compareTo(BigDecimal.ZERO) <= 0) {
                return BigDecimal.ZERO; // 无供应缺口，不需要生产
            }
            // 供应缺口填补模式：补充缺口 + 安全水位库存
            // 安全水位量 = nextFormingClass 开始连续 SAFETY_STOCK_LEVEL 个成型班次的消耗量之和
            int safetyStockLevel = this.getParamAsDecimal(context, DjEngineConstants.PARAM_SAFETY_STOCK_LEVEL).intValue();
            BigDecimal safetyStockQty = BigDecimal.ZERO;
            for (int fc = firstFormingClass + 1; fc <= firstFormingClass + safetyStockLevel; fc++) {
                if (fc <= DjEngineConstants.CX_SHIFT_COUNT) {
                    safetyStockQty = safetyStockQty.add(
                            this.calcShiftConsume(context, spec.getPaddingCode(), fc));
                }
            }
            remainingDemand = supplyGap.add(safetyStockQty);
        }

        // 计算基础排产量（暂不限制最大排产量）
        BigDecimal baseProduceQty;

        // 已收尾规格：精确排产，不要求台车整倍数
        if (spec.isTailFinished()) {
            baseProduceQty = remainingDemand.min(remainingCapacity);
        } else {
            // 未收尾规格：需台车容量整倍数
            BigDecimal trolleyCapacity = spec.getTrolleyCapacity();
            if (trolleyCapacity == null || trolleyCapacity.compareTo(BigDecimal.ZERO) <= 0) {
                return BigDecimal.ZERO;
            }

            // 计算最大可生产的整台车数
            int maxTrolleys = remainingCapacity.divide(trolleyCapacity, 0, RoundingMode.FLOOR).intValue();
            // 计算剩余需求所需台车数
            int needTrolleys = remainingDemand.divide(trolleyCapacity, 0, RoundingMode.CEILING).intValue();

            int actualTrolleys = Math.min(maxTrolleys, needTrolleys);
            if (actualTrolleys <= 0) {
                // 连一台车都放不下，本班不生产此规格
                if (remainingDemand.compareTo(BigDecimal.ZERO) > 0 && remainingCapacity.compareTo(trolleyCapacity) >= 0) {
                    // 剩余需求不足一台车但至少生产一台车
                    actualTrolleys = 1;
                } else {
                    return BigDecimal.ZERO;
                }
            }
            baseProduceQty = BigDecimal.valueOf(actualTrolleys).multiply(trolleyCapacity);
        }

        // ===== 单规格每班最大排产量限制 =====
        BigDecimal maxShiftProduceQty = this.getParamAsDecimal(context, DjEngineConstants.PARAM_MAX_SHIFT_PRODUCE_QTY);
        if (maxShiftProduceQty != null && maxShiftProduceQty.compareTo(BigDecimal.ZERO) > 0
                && baseProduceQty.compareTo(maxShiftProduceQty) > 0) {
            if (spec.isTailFinished()) {
                // 已收尾规格：直接截断至最大排产量
                baseProduceQty = maxShiftProduceQty;
            } else {
                // 未收尾规格：允许超出最大排产限制一台车以内的量
                BigDecimal trolleyCapacity = spec.getTrolleyCapacity();
                if (trolleyCapacity != null && trolleyCapacity.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal relaxedLimit = maxShiftProduceQty.add(trolleyCapacity);
                    int trolleysWithinLimit = relaxedLimit.divide(trolleyCapacity, 0, RoundingMode.FLOOR).intValue();
                    if (trolleysWithinLimit >= 1) {
                        baseProduceQty = BigDecimal.valueOf(trolleysWithinLimit).multiply(trolleyCapacity);
                    }
                    // 若整车容量本身超过限制（trolleysWithinLimit < 1），保留原基础排产量
                }
            }
        }

        return baseProduceQty;
    }

    /**
     * 计算规格在供应窗口内的净需求量（与 checkDemandForShift 步骤3同口径）
     *
     * @param spec      规格
     * @param shiftIndex 当前垫胶班次索引
     * @param context   排程上下文
     * @return 供应窗口净需求量
     */
    private BigDecimal calcWindowNetDemand(DjPaddingDemand spec, int shiftIndex, DjScheduleContext context) {
        Map<String, BigDecimal> handoverInventory = context.getHandoverInventory();
        Map<String, Integer> paddingSupplyDepth = context.getPaddingSupplyDepth();

        BigDecimal incomingInventory = handoverInventory.getOrDefault(spec.getPaddingCode(), BigDecimal.ZERO);
        int specSupplyDepth = paddingSupplyDepth.getOrDefault(spec.getPaddingCode(),
                DjEngineConstants.CX_SHIFT_COUNT);

        int formingShiftOffset = context.getFormingShiftOffset() != null ? context.getFormingShiftOffset() : 0;
        int firstFormingClass = this.getFormingClassByShiftIndex(shiftIndex, context);
        if (firstFormingClass < 1) {
            firstFormingClass = shiftIndex + formingShiftOffset;
        }

        int windowStartClass = firstFormingClass + 1;
        int windowEndClass = Math.min(firstFormingClass + specSupplyDepth,
                DjEngineConstants.CX_SHIFT_COUNT);

        BigDecimal demandInWindow;
        int trialAdvanceShifts = this.getTrialAdvanceShifts(context);
        if (trialAdvanceShifts > 0 && this.hasTrialConsumption(context, spec.getPaddingCode())) {
            demandInWindow = this.calcCombinedWindowDemand(context, spec.getPaddingCode(),
                    firstFormingClass, specSupplyDepth, trialAdvanceShifts);
        } else {
            demandInWindow = BigDecimal.ZERO;
            for (int fc = windowStartClass; fc <= windowEndClass; fc++) {
                demandInWindow = demandInWindow.add(this.calcShiftConsume(
                        context, spec.getPaddingCode(), fc));
            }
        }

        // 本班成型消耗由接班库存承担，供应窗口实际可用库存 = max(0, 接班库存 - 本班消耗)
        BigDecimal currentShiftConsume = this.calcShiftConsume(context, spec.getPaddingCode(), firstFormingClass);
        BigDecimal availableForWindow = incomingInventory.subtract(currentShiftConsume);
        if (availableForWindow.compareTo(BigDecimal.ZERO) < 0) {
            availableForWindow = BigDecimal.ZERO;
        }
        BigDecimal netDemand = demandInWindow.subtract(availableForWindow);
        if (netDemand.compareTo(BigDecimal.ZERO) < 0) {
            netDemand = BigDecimal.ZERO;
        }

        // 已收尾规格含损耗率
        if (spec.isTailFinished()) {
            BigDecimal lossRate = this.getLossRate(spec.getPaddingCode(), spec.getMachineCode(), context);
            // 损耗率存储为百分比值（如 2 表示 2%），需除以 100
            BigDecimal lossRateDecimal = lossRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            netDemand = netDemand.multiply(BigDecimal.ONE.add(lossRateDecimal)).setScale(2, RoundingMode.HALF_UP);
        }

        return netDemand;
    }

    /**
     * 记录排产结果
     */
    private void recordSchedule(Map<String, DjScheduleResult> resultMap, DjPaddingDemand spec, int shiftIndex,
            BigDecimal produceQty, String machineCode, DjScheduleContext context) {
        String key = machineCode + ":" + spec.getPaddingCode();
        DjScheduleResult result = resultMap.computeIfAbsent(key, k -> {
            DjScheduleResult r = new DjScheduleResult();
            r.setMachineCode(machineCode);
            r.setPaddingCode(spec.getPaddingCode());
            r.setGlueCode(spec.getGlueCode());
            r.setPaddingName(spec.getPaddingName());
            r.setMouthPlateCode(spec.getMouthPlateCode());
            // 记录排程首班班次（class1 对应的班次）
            if (context.getShiftClassMap() != null) {
                r.setScheduleShiftClass(context.getShiftClassMap()[0]);
            }
            return r;
        });

        // 各机台各班次使用独立的生产顺序计数器
        Map<String, Map<Integer, Integer>> shiftSeqMap = context.getShiftSequenceMap();
        if (shiftSeqMap == null) {
            shiftSeqMap = new HashMap<>();
            context.setShiftSequenceMap(shiftSeqMap);
        }
        Map<Integer, Integer> machineShiftSeq = shiftSeqMap.computeIfAbsent(machineCode, k -> new HashMap<>());
        int seq = machineShiftSeq.getOrDefault(shiftIndex, 0) + 1;
        machineShiftSeq.put(shiftIndex, seq);

        // 使用 setFieldValueByFieldName 动态设置对应班次的计划量和顺序
        // 需累加已有排产量（主循环和补量阶段可能多次记录同一规格同一班次）
        String planQtyFieldName = String.format(DjEngineConstants.CLASS_PLAN_QTY_FIELD, shiftIndex);
        BigDecimal currentQty = BigDecimalUtils.valueOf(result.getFieldValueByFieldName(planQtyFieldName));
        result.setFieldValueByFieldName(planQtyFieldName, currentQty.add(produceQty));
        result.setFieldValueByFieldName(String.format(DjEngineConstants.CLASS_SEQUENCE_FIELD, shiftIndex), seq);
    }

    /**
     * 获取某规格在某班次的排产量
     */
    private BigDecimal getScheduledQty(Map<String, DjScheduleResult> resultMap, String paddingCode, int shiftIndex) {
        for (DjScheduleResult result : resultMap.values()) {
            if (paddingCode.equals(result.getPaddingCode())) {
                String fieldName = String.format(DjEngineConstants.CLASS_PLAN_QTY_FIELD, shiftIndex);
                return BigDecimalUtils.valueOf(result.getFieldValueByFieldName(fieldName));
            }
        }
        return BigDecimal.ZERO;
    }

    // ==================== 步骤5.5：结果输出 ====================

    /**
     * 损耗率转换（仅未收尾规格）
     */
    private void convertPlanQtyWithLoss(List<DjScheduleResult> results, DjScheduleContext context) {
        for (DjScheduleResult result : results) {
            if (DjEngineConstants.TAIL_FLAG_YES.equals(result.getTailFlag())) {
                continue; // 已收尾规格跳过（已在步骤5.1含损耗率）
            }
            BigDecimal lossRate = this.getLossRate(result.getPaddingCode(), result.getMachineCode(), context);
            if (lossRate == null || lossRate.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // 各班次计划量 × (1 + 损耗率)
            for (int i = 1; i <= 6; i++) {
                BigDecimal planQty = this.getClassPlanQtyFromResult(result, i);
                if (planQty != null && planQty.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal finalPlanQty = planQty.multiply(BigDecimal.ONE.add(lossRate)).setScale(0,
                            RoundingMode.CEILING);
                    this.setClassPlanQtyToResult(result, i, finalPlanQty);
                }
            }
        }
    }

    /**
     * 从排产结果获取某班产量
     */
    private BigDecimal getClassPlanQtyFromResult(DjScheduleResult result, int classIndex) {
        String fieldName = String.format(DjEngineConstants.CLASS_PLAN_QTY_FIELD, classIndex);
        return BigDecimalUtils.valueOf(result.getFieldValueByFieldName(fieldName));
    }

    /**
     * 设置排产结果某班产量
     */
    private void setClassPlanQtyToResult(DjScheduleResult result, int classIndex, BigDecimal planQty) {
        result.setFieldValueByFieldName(String.format(DjEngineConstants.CLASS_PLAN_QTY_FIELD, classIndex), planQty);
    }


    /**
     * 归档旧数据并写入新数据
     */
    private void archiveAndSave(String factoryCode, Date scheduleDate, List<DjScheduleResult> newResults) {
        // 1. 查询旧数据（使用父类 selectList）
        List<DjScheduleResult> oldRecords = djEngineScheduleResultMapper.selectList(
                new LambdaQueryWrapper<DjScheduleResult>().eq(DjScheduleResult::getScheduleDate, scheduleDate));
        if (!CollectionUtils.isEmpty(oldRecords)) {
            // 2. 归档到日志表
            // 使用 mapper.xml 中定义的批量 insert 语句
            djEngineScheduleResultLogMapper.syncDjScheduleToLog(DateUtil.formatDate(scheduleDate));

            // 3. 删除主表旧数据
            djEngineScheduleResultMapper.deleteDjSchedule(DateUtil.formatDate(scheduleDate));
            log.info("归档 {} 条旧排产记录", oldRecords.size());
        }

        // 4. 批量写入新数据
        if (!CollectionUtils.isEmpty(newResults)) {
            baseDao.saveBatch(newResults);
            log.info("写入 {} 条新排产记录", newResults.size());
        }
    }

    /**
     * 保存排程过程日志
     * <p>将排程过程中收集的所有日志汇总为文本，保存到排程日志表</p>
     *
     * @param context 排程上下文
     * @param batchNo 批次号
     */
    private void saveScheduleProcessLog(DjScheduleContext context, String batchNo) {
        String logText = context.getProcessLogText();
        if (StringUtils.isEmpty(logText)) {
            return;
        }
        DjScheduleProcessLog processLog = new DjScheduleProcessLog();
        processLog.setBatchNo(batchNo);
        processLog.setLogDetail(logText);
        djEngineScheduleProcessLogMapper.insert(processLog);
        log.info("排程过程日志已保存，batchNo={}，日志长度={} 字符", batchNo, logText.length());
    }

    // ==================== 辅助方法 ====================

    /**
     * 加载排产参数
     */
    private Map<String, DjParams> loadParamsMap(String factoryCode) {
        List<DjParams> paramsList = djEngineParamsMapper
                .selectList(new LambdaQueryWrapper<DjParams>().eq(DjParams::getFactoryCode, factoryCode));
        if (paramsList == null) {
            return new HashMap<>();
        }
        return paramsList.stream().filter(p -> p.getParamCode() != null)
                .collect(Collectors.toMap(DjParams::getParamCode, p -> p));
    }

    /**
     * 解析参数值：优先取 paramValue，没有则取 defauleValue，仍没有则返回 null
     */
    private String resolveParamValue(DjParams param) {
        if (param == null) {
            return null;
        }
        if (param.getParamValue() != null && !param.getParamValue().isEmpty()) {
            return param.getParamValue();
        }
        return (param.getDefauleValue() != null && !param.getDefauleValue().isEmpty())
                ? param.getDefauleValue() : null;
    }

    /**
     * 获取参数整型值，参数未正确配置时抛异常
     */
    private int getParamAsInt(Map<String, DjParams> paramsMap, String key) {
        DjParams param = (paramsMap != null) ? paramsMap.get(key) : null;
        String val = this.resolveParamValue(param);
        if (val == null) {
            String name = (param != null && param.getParamName() != null) ? param.getParamName() : key;
            throw new BusinessException(MessageFormat.format(
                    I18nUtil.getMessage("ui.dj.engine.paramNotConfigured"), name, key));
        }
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            String name = param.getParamName() != null ? param.getParamName() : key;
            throw new BusinessException(MessageFormat.format(
                    I18nUtil.getMessage("ui.dj.engine.paramNotConfigured"), name, key));
        }
    }

    /**
     * 获取参数小数类型值，参数未正确配置时抛异常
     */
    private BigDecimal getParamAsDecimal(Map<String, DjParams> paramsMap, String key) {
        DjParams param = (paramsMap != null) ? paramsMap.get(key) : null;
        String val = this.resolveParamValue(param);
        if (val == null) {
            String name = (param != null && param.getParamName() != null) ? param.getParamName() : key;
            throw new BusinessException(MessageFormat.format(
                    I18nUtil.getMessage("ui.dj.engine.paramNotConfigured"), name, key));
        }
        try {
            return new BigDecimal(val);
        } catch (NumberFormatException e) {
            String name = param.getParamName() != null ? param.getParamName() : key;
            throw new BusinessException(MessageFormat.format(
                    I18nUtil.getMessage("ui.dj.engine.paramNotConfigured"), name, key));
        }
    }

    /**
     * 获取参数小数类型值（通过 context）
     */
    private BigDecimal getParamAsDecimal(DjScheduleContext context, String key) {
        return this.getParamAsDecimal(context.getParamsMap(), key);
    }

    /**
     * 获取参数整型值（通过 context），参数不存在时返回默认值
     */
    private int getParamAsInt(DjScheduleContext context, String key, int defaultValue) {
        DjParams param = (context.getParamsMap() != null) ? context.getParamsMap().get(key) : null;
        String val = this.resolveParamValue(param);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
