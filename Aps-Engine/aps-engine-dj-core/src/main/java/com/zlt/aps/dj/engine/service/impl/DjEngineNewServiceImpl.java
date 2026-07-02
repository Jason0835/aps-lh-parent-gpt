package com.zlt.aps.dj.engine.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.text.MessageFormat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.common.engine.enums.MachineRangeEnum;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.dj.api.domain.entity.DjCurlRoll;
import com.zlt.aps.dj.api.domain.entity.DjDepthConfig;
import com.zlt.aps.dj.api.domain.entity.DjGlueOrder;
import com.zlt.aps.dj.api.domain.entity.DjLossSetting;
import com.zlt.aps.dj.api.domain.entity.DjMachineInfo;
import com.zlt.aps.dj.api.domain.entity.DjMachineMaintenance;
import com.zlt.aps.dj.api.domain.entity.DjParams;
import com.zlt.aps.dj.api.domain.entity.DjScheduleResult;
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
import com.zlt.aps.dj.engine.mapper.DjEngineScheduleResultLogMapper;
import com.zlt.aps.dj.engine.mapper.DjEngineScheduleResultMapper;
import com.zlt.aps.dj.engine.mapper.DjEngineSpecifyMachineMapper;
import com.zlt.aps.dj.engine.mapper.DjEngineStockMapper;
import com.zlt.aps.dj.engine.constant.DjEngineConstants;
import com.zlt.aps.dj.engine.model.DjPaddingDemand;
import com.zlt.aps.dj.engine.model.DjScheduleContext;
import com.zlt.aps.dj.engine.service.DjEngineNewService;
import com.zlt.aps.dj.engine.service.IDjOrderGeneratorService;
import com.zlt.aps.exception.BusinessException;
import com.zlt.aps.mdm.api.domain.entity.MdmConstructionInfo;
import com.zlt.aps.mp.api.domain.entity.MpMonthPlanMonitor;
import com.zlt.core.dao.basedao.BaseDao;

import cn.hutool.core.date.DateUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
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
    private DjEngineMonthPlanMonitorMapper djEngineMonthPlanMonitorMapper;

    @Autowired
    private DjEngineSpecifyMachineMapper djEngineSpecifyMachineMapper;

    @Autowired
    private DjEngineDepthConfigMapper djEngineDepthConfigMapper;

    @Autowired
    private BaseDao baseDao;

    @Autowired
    private IDjOrderGeneratorService iDjOrderGeneratorService;

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

        // 构建施工号 -> 施工信息 Map
        Map<String, MdmConstructionInfo> constructionMap = constructionList.stream()
                .collect(Collectors.toMap(MdmConstructionInfo::getConstructionCode, c -> c, (a, b) -> a));

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
        for (Map.Entry<String, MdmConstructionInfo> entry : constructionMap.entrySet()) {
            MdmConstructionInfo construction = entry.getValue();
            if (StringUtils.isEmpty(construction.getPaddingCode())
                    || construction.getPaddingLength() == null
                    || construction.getPaddingLength().compareTo(BigDecimal.ZERO) <= 0) {
                invalidConstructionCodes.add(entry.getKey());
            }
        }
        if (!invalidConstructionCodes.isEmpty()) {
            log.warn("步骤2：以下胎胚代码施工数据中垫胶代码或垫胶长度无效：{}", invalidConstructionCodes);
            throw new BusinessException(MessageFormat.format(
                    I18nUtil.getMessage("ui.dj.engine.invalidConstruction"), invalidConstructionCodes));
        }

        // 2.2 关联成型计划与施工数据，解析垫胶消耗量
        // 按垫胶规格统计对应的成型机台数量
        Map<String, Integer> paddingCxMachineCount = this.calcPaddingCxMachineCount(cxScheduleList, constructionMap);
        context.setPaddingCxMachineCount(paddingCxMachineCount);
        log.info("步骤2.2：垫胶规格数：{}，各规格成型机台数：{}", paddingCxMachineCount.size(), paddingCxMachineCount);

        // 加载排产参数
        Map<String, DjParams> paramsMap = this.loadParamsMap(factoryCode);
        context.setParamsMap(paramsMap);

        // 根据排程首班班次参数构建班次索引映射
        DjParams startShiftParam = paramsMap.get(DjEngineConstants.PARAM_SCHEDULE_START_SHIFT);
        String startShiftValue = this.resolveParamValue(startShiftParam);
        if (startShiftValue == null) {
            String name = (startShiftParam != null && startShiftParam.getParamName() != null)
                    ? startShiftParam.getParamName() : DjEngineConstants.PARAM_SCHEDULE_START_SHIFT;
            throw new BusinessException(MessageFormat.format(
                    I18nUtil.getMessage("ui.dj.engine.paramNotConfigured"), name, DjEngineConstants.PARAM_SCHEDULE_START_SHIFT));
        }
        String[] shiftClassMap = DjEngineUtil.buildShiftClassMap(startShiftValue);
        context.setShiftClassMap(shiftClassMap);
        log.info("步骤2.2：排程首班班次={}，班次映射={}", startShiftValue, String.join(",", shiftClassMap));

        // 按垫胶规格分别解析供应窗口（不同规格排产深度可能不同）
        Map<String, Integer> paddingSupplyDepth = new HashMap<>();
        int maxSupplyDepth = 0;
        for (Map.Entry<String, Integer> entry : paddingCxMachineCount.entrySet()) {
            int depth = this.parseSupplyDepth(factoryCode, entry.getValue());
            paddingSupplyDepth.put(entry.getKey(), depth);
            if (depth > maxSupplyDepth) {
                maxSupplyDepth = depth;
            }
        }
        context.setPaddingSupplyDepth(paddingSupplyDepth);
        log.info("步骤2.2：排产深度（供应窗口），最大 {} 个班", maxSupplyDepth);

        // 整理消耗量 Map<paddingCode, Map<班次索引, 消耗量>>
        Map<String, Map<Integer, BigDecimal>> consumeQty = this.parseConsumeQty(cxScheduleList, constructionMap,
                paddingSupplyDepth);
        context.setConsumeQty(consumeQty);
        log.info("步骤2.2：解析出 {} 个垫胶规格的消耗量", consumeQty.size());

        // 获取所有垫胶代码
        Set<String> paddingCodes = consumeQty.keySet();

        // ==================== 步骤3：计算垫胶需求清单 ====================
        // 3.1 加载库存
        Map<String, BigDecimal> effectiveStockMap = this.loadEffectiveStock(factoryCode, new ArrayList<>(paddingCodes),
                context);
        context.setEffectiveStockMap(effectiveStockMap);
        log.info("步骤3.1：加载有效库存 {} 个规格", effectiveStockMap.size());

        // 判断新规格：库存表中不存在的垫胶代码即为新规格
        Set<String> newSpecPaddingCodes = new HashSet<>(paddingCodes);
        newSpecPaddingCodes.removeAll(effectiveStockMap.keySet());

        // 3.2 供应窗口超出成型范围时，加载月计划余量预估
        // 筛选出排产深度超出成型班数的垫胶规格
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
            // 获取成型计划最后一天的最大班需求量
            Map<String, BigDecimal> maxShiftConsume = this.getMaxShiftConsume(cxScheduleList, constructionMap);
            // 加载月计划余量
            String yearMonth = DateUtil.format(scheduleDate, "yyyyMM");
            List<MpMonthPlanMonitor> monthPlanMonitors = this.loadMonthPlanMonitor(factoryCode, yearMonth);
            // 按垫胶代码整理月计划余量
            Map<String, BigDecimal> paddingMonthSurplus = this.calcPaddingMonthSurplus(monthPlanMonitors,
                    constructionMap, maxShiftConsume, maxExceedShifts);
            // 补充超出范围的消耗量
            if (!paddingMonthSurplus.isEmpty()) {
                this.supplementOutOfRangeConsume(consumeQty, context, exceedPaddingMap, maxShiftConsume,
                        paddingMonthSurplus);
            }
            // 存储月度剩余量到上下文（用于净需求计算中 n>8 时的月计划约束）
            Map<String, BigDecimal> rawPaddingRemaining = this.calcRawPaddingRemaining(monthPlanMonitors,
                    constructionMap);
            if (!rawPaddingRemaining.isEmpty()) {
                context.setPaddingRemainingMap(rawPaddingRemaining);
            }
        }

        // 3.3 构建垫胶需求清单
        BigDecimal standardCurlLength = this.getParamAsDecimal(context, DjEngineConstants.PARAM_STANDARD_CRIMP_LENGTH);
        List<DjPaddingDemand> demandList = this.buildDemandList(paddingCodes, consumeQty, effectiveStockMap,
                constructionMap, newSpecPaddingCodes, standardCurlLength);
        log.info("步骤3.3：生成垫胶需求清单 {} 个规格", demandList.size());

        // 第1班接班库存不等同于有效库存（早上6点快照），需预估6:00~14:00（早班）的库存变化
        // 公式：接班库存 = 有效库存 + 前日早班垫胶计划量 - 当日早班成型消耗量
        // 前日早班垫胶计划量：T-1日排产结果中 class3（早班 6:00~14:00）的计划量
        // 当日早班成型消耗量：CX T日 class1 计划量按单耗换算
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
            // CX class1（早班）消耗量
            BigDecimal cxConsume = BigDecimal.ZERO;
            Map<Integer, BigDecimal> shiftConsume = consumeQty.get(paddingCode);
            if (shiftConsume != null) {
                cxConsume = shiftConsume.getOrDefault(1, BigDecimal.ZERO);
            }
            BigDecimal inventory = stock.add(addPlan).subtract(cxConsume);
            if (inventory.compareTo(BigDecimal.ZERO) < 0) {
                inventory = BigDecimal.ZERO;
            }
            handoverInventory.put(paddingCode, inventory);
        }
        context.setHandoverInventory(handoverInventory);

        // ==================== 步骤4：选择机台 ====================
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
        List<DjSpecifyMachine> specifyMachineList = this.loadSpecifyMachines(factoryCode,
                new ArrayList<>(paddingCodes));
        log.info("步骤4.2：加载定点机台 {} 条", specifyMachineList.size());

        // 4.3 关联机台
        this.assignMachine(demandList, machineMap, specifyMachineList);

        // 4.4 加载辅助配置数据
        this.loadAuxiliaryData(factoryCode, context, new ArrayList<>(paddingCodes));
        log.info("步骤4.4：辅助配置数据加载完成");

        // ==================== 步骤5：排产 ====================
        // 步骤5.1~5.4 核心排产循环
        List<DjScheduleResult> scheduleResults = this.executeSchedule(demandList, context);
        if (CollectionUtils.isEmpty(scheduleResults)) {
            throw new BusinessException(I18nUtil.getMessage("ui.dj.engine.noScheduleResult"));
        }
        log.info("步骤5.1~5.4：排产完成，生成 {} 条排产结果", scheduleResults.size());

        // 步骤5.5 结果输出
        // 5.5.1 损耗率转换（未收尾规格）
        this.convertPlanQtyWithLoss(scheduleResults, context);

        // 5.5.2 生成批次号并设置批次号和订单号
        String batchNo = iDjOrderGeneratorService.fillOrderInfo(scheduleResults, factoryCode, scheduleDate);
        context.setCurrentBatchNo(batchNo);

        // 5.5.3 归档旧数据 + 写入新数据
        this.archiveAndSave(factoryCode, scheduleDate, scheduleResults);

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
     * 解析消耗量
     */
    private Map<String, Map<Integer, BigDecimal>> parseConsumeQty(List<CxScheduleResult> cxScheduleList,
            Map<String, MdmConstructionInfo> constructionMap, Map<String, Integer> paddingSupplyDepth) {
        Map<String, Map<Integer, BigDecimal>> result = new HashMap<>();

        for (CxScheduleResult cx : cxScheduleList) {
            MdmConstructionInfo construction = constructionMap.get(cx.getEmbryoCode());
            if (construction == null || construction.getPaddingCode() == null) {
                continue;
            }
            String paddingCode = construction.getPaddingCode();
            // 垫胶长度单位是毫米(mm)，需要换算成米(m)
            BigDecimal unitConsume = construction.getPaddingLength() != null
                    ? construction.getPaddingLength().divide(DjEngineConstants.MM_TO_M_DIVISOR, 6, RoundingMode.HALF_UP)
                    : BigDecimal.ONE;

            result.putIfAbsent(paddingCode, new HashMap<>());
            Map<Integer, BigDecimal> shiftConsume = result.get(paddingCode);

            // 使用该垫胶规格对应的排产深度
            int specSupplyDepth = paddingSupplyDepth.getOrDefault(paddingCode, DjEngineConstants.CX_SHIFT_COUNT);
            // 遍历8个班的计划量，只取排产深度范围内的
            for (int i = 1; i <= specSupplyDepth && i <= DjEngineConstants.CX_SHIFT_COUNT; i++) {
                BigDecimal classPlanQty = this.getClassPlanQty(cx, i);
                if (classPlanQty != null && classPlanQty.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal consume = classPlanQty.multiply(unitConsume).setScale(2, RoundingMode.HALF_UP);
                    shiftConsume.merge(i, consume, BigDecimal::add);
                }
            }
        }
        return result;
    }

    /**
     * 获取成型计划某班的计划量
     */
    private BigDecimal getClassPlanQty(CxScheduleResult cx, int classIndex) {
        String fieldName = String.format(DjEngineConstants.CLASS_PLAN_QTY_FIELD, classIndex);
        return BigDecimalUtils.valueOf(cx.getFieldValueByFieldName(fieldName));
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
            Map<String, MdmConstructionInfo> constructionMap) {
        Map<String, Set<String>> paddingMachines = new HashMap<>();
        for (CxScheduleResult cx : cxScheduleList) {
            MdmConstructionInfo construction = constructionMap.get(cx.getEmbryoCode());
            if (construction == null || construction.getPaddingCode() == null || cx.getCxMachineCode() == null) {
                continue;
            }
            paddingMachines.computeIfAbsent(construction.getPaddingCode(), k -> new HashSet<>())
                    .add(cx.getCxMachineCode());
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
     * 匹配规则：按 MACHINE_QTY 降序排序，对给定的成型机台数逐行检查 MACHINE_RANGE 条件，
     * 取第一个满足条件的配置行对应的 DEPTH_CLASS_QTY 值作为排产深度。
     * 若所有行均不匹配，使用默认排产深度 1。
     * </p>
     */
    private int parseSupplyDepth(String factoryCode, int cxMachineCount) {
        // 查询当前工厂的所有深度配置，按 MACHINE_QTY 降序
        List<DjDepthConfig> configList = djEngineDepthConfigMapper.selectList(
                new LambdaQueryWrapper<DjDepthConfig>()
                        .eq(DjDepthConfig::getFactoryCode, factoryCode)
                        .orderByDesc(DjDepthConfig::getMachineQty));
        if (CollectionUtils.isEmpty(configList)) {
            return 1;
        }
        for (DjDepthConfig config : configList) {
            Integer configQty = config.getMachineQty();
            if (configQty == null) {
                continue;
            }
            MachineRangeEnum rangeEnum = MachineRangeEnum.getByCode(config.getMachineRange());
            if (rangeEnum != null && rangeEnum.matches(cxMachineCount, configQty)) {
                BigDecimal depth = config.getDepthClassQty();
                return depth != null ? depth.intValue() : 1;
            }
        }
        return 1;
    }

    /**
     * 获取成型计划最后一天的最大班需求量（用于预估超出部分）
     */
    private Map<String, BigDecimal> getMaxShiftConsume(List<CxScheduleResult> cxScheduleList,
            Map<String, MdmConstructionInfo> constructionMap) {
        Map<String, BigDecimal> maxShiftConsume = new HashMap<>();
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
            MdmConstructionInfo construction = constructionMap.get(cx.getEmbryoCode());
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
    private Map<String, BigDecimal> calcPaddingMonthSurplus(List<MpMonthPlanMonitor> monthPlanMonitors,
            Map<String, MdmConstructionInfo> constructionMap, Map<String, BigDecimal> maxShiftConsume,
            int exceedShifts) {
        if (CollectionUtils.isEmpty(monthPlanMonitors)) {
            return Collections.emptyMap();
        }

        Map<String, BigDecimal> result = new HashMap<>();
        for (MpMonthPlanMonitor monitor : monthPlanMonitors) {
            // 通过施工表找到垫胶代码
            for (Map.Entry<String, MdmConstructionInfo> entry : constructionMap.entrySet()) {
                MdmConstructionInfo construction = entry.getValue();
                if (construction.getPaddingCode() == null) {
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
            Map<String, MdmConstructionInfo> constructionMap) {
        if (CollectionUtils.isEmpty(monthPlanMonitors)) {
            return Collections.emptyMap();
        }
        Map<String, BigDecimal> result = new HashMap<>();
        for (MpMonthPlanMonitor monitor : monthPlanMonitors) {
            for (Map.Entry<String, MdmConstructionInfo> entry : constructionMap.entrySet()) {
                MdmConstructionInfo construction = entry.getValue();
                if (construction.getPaddingCode() == null) {
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
     * 补充超出供应窗口的消耗量
     */
    private void supplementOutOfRangeConsume(Map<String, Map<Integer, BigDecimal>> consumeQty,
            DjScheduleContext context, Map<String, Integer> paddingExceedShifts,
            Map<String, BigDecimal> maxShiftConsume, Map<String, BigDecimal> paddingMonthSurplus) {
        for (Map.Entry<String, BigDecimal> entry : paddingMonthSurplus.entrySet()) {
            String paddingCode = entry.getKey();
            BigDecimal monthRemaining = entry.getValue();
            BigDecimal maxShift = maxShiftConsume.getOrDefault(paddingCode, BigDecimal.ZERO);

            int exceedShifts = paddingExceedShifts.getOrDefault(paddingCode, 0);
            if (exceedShifts <= 0) {
                continue;
            }

            consumeQty.putIfAbsent(paddingCode, new HashMap<>());
            Map<Integer, BigDecimal> shiftConsume = consumeQty.get(paddingCode);

            BigDecimal accumulated = BigDecimal.ZERO;
            int startShift = DjEngineConstants.CX_SHIFT_COUNT + 1;
            for (int i = 0; i < exceedShifts; i++) {
                int shiftIndex = startShift + i;
                BigDecimal consume = maxShift;
                // 累计不能超过月剩余量
                if (accumulated.add(consume).compareTo(monthRemaining) > 0) {
                    consume = monthRemaining.subtract(accumulated);
                    if (consume.compareTo(BigDecimal.ZERO) <= 0) {
                        break;
                    }
                }
                shiftConsume.put(shiftIndex, consume);
                accumulated = accumulated.add(consume);
            }
        }
    }

    /**
     * 构建垫胶需求清单
     */
    private List<DjPaddingDemand> buildDemandList(Set<String> paddingCodes,
            Map<String, Map<Integer, BigDecimal>> consumeQty, Map<String, BigDecimal> effectiveStockMap,
            Map<String, MdmConstructionInfo> constructionMap, Set<String> newSpecPaddingCodes,
            BigDecimal standardCurlLength) {
        List<DjPaddingDemand> demandList = new ArrayList<>();

        // 收集各规格的总消耗量和最早需求时间
        for (String paddingCode : paddingCodes) {
            DjPaddingDemand demand = new DjPaddingDemand();
            demand.setPaddingCode(paddingCode);

            // 从施工信息获取单耗、胶料、垫胶物料名等
            for (MdmConstructionInfo construction : constructionMap.values()) {
                if (paddingCode.equals(construction.getPaddingCode())) {
                    if (demand.getUnitConsume() == null) {
                        demand.setUnitConsume(construction.getPaddingLength() != null ? construction.getPaddingLength()
                                : BigDecimal.ONE);
                        demand.setConstructionCode(construction.getConstructionCode());
                        demand.setProductionStatus(construction.getProductionStage());
                        demand.setPaddingName(construction.getPaddingName());
                        demand.setGlueCode(construction.getPaddingRubber());
                    }
                    break;
                }
            }

            // 判断是否已收尾
            demand.setTailFinished(
                    DjEngineConstants.CX_PRODUCTION_STATUS_FINISHED.equals(demand.getProductionStatus()));

            // 判断是否新规格
            demand.setNewSpec(newSpecPaddingCodes.contains(paddingCode));

            // 计算总消耗量（净需求 = 总消耗 - 有效库存）
            Map<Integer, BigDecimal> shiftConsume = consumeQty.getOrDefault(paddingCode, new HashMap<>());
            BigDecimal totalConsume = shiftConsume.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal effectiveStock = effectiveStockMap.getOrDefault(paddingCode, BigDecimal.ZERO);
            BigDecimal netDemand = totalConsume.subtract(effectiveStock);
            if (netDemand.compareTo(BigDecimal.ZERO) < 0) {
                netDemand = BigDecimal.ZERO;
            }
            demand.setNetDemand(netDemand);
            demand.setRemainingDemand(netDemand);
            demand.setIncomingInventory(effectiveStock);
            demand.setTrolleyCapacity(standardCurlLength);
            demand.setNeedProduce(netDemand.compareTo(BigDecimal.ZERO) > 0);

            demandList.add(demand);
        }
        return demandList;
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
                    lossRateMap.put(lossKey, BigDecimal.valueOf(loss.getLossRate()));
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
        List<DjScheduleResult> results = new ArrayList<>();
        Map<String, Map<Integer, BigDecimal>> consumeQty = context.getConsumeQty();
        Map<String, BigDecimal> handoverInventory = context.getHandoverInventory();
        Map<String, DjMachineInfo> machineMap = context.getMachineMap();

        // 供应窗口班次对应的消耗量（成型计划班次消耗量）
        // 用于检查库存是否满足供应窗口内需求

        // 存储各机台每个班次的剩余产能
        // Map<machineCode, Map<shiftIndex, remainingCapacity>>
        Map<String, Map<Integer, BigDecimal>> capacityMatrix = calcEffectiveCapacity(machineMap, context);

        // 计算每班的台车工装约束上限（班次总产量上限）
        BigDecimal shiftTrolleyLimit = this.calcShiftTrolleyLimit(context);
        if (shiftTrolleyLimit != null) {
            log.info("班次台车约束上限：{} 米", shiftTrolleyLimit);
        }

        // 存储各机台各班的最后生产规格
        Map<String, String> lastSpecInShift = new HashMap<>();

        // 存储排产结果
        Map<String, DjScheduleResult> resultMap = new HashMap<>();

        // 遍历6个班次
        for (int shiftIndex = 1; shiftIndex <= DjEngineConstants.SHIFT_COUNT; shiftIndex++) {
            log.debug("排产班次 {}/{}（中班→夜班→早班循环）", shiftIndex, DjEngineConstants.SHIFT_COUNT);

            // 班次开始前：检查各规格接班库存是否满足供应窗口内的成型消耗量
            this.checkDemandForShift(demandList, shiftIndex, context);

            // 计算本班实际可用的产能上限 = min(班次总产量上限, 所有开机机台产能之和)
            BigDecimal shiftTotalCapacity = this.calcShiftTotalCapacity(capacityMatrix, shiftIndex,
                    shiftTrolleyLimit);
            BigDecimal shiftRemainingCapacity = shiftTotalCapacity;

            // 遍历所有机台
            for (Map.Entry<String, DjMachineInfo> machineEntry : machineMap.entrySet()) {
                String machineCode = machineEntry.getKey();
                DjMachineInfo machine = machineEntry.getValue();

                // 机台本班是否可用
                if (!this.isMachineAvailable(machine, shiftIndex, context.getShiftClassMap())) {
                    log.trace("机台 {} 班次 {} 不可用", machineCode, shiftIndex);
                    continue;
                }

                // 获取本班该机台的剩余产能
                Map<Integer, BigDecimal> machineCapacity = capacityMatrix.get(machineCode);
                if (machineCapacity == null) {
                    continue;
                }
                BigDecimal remainingCapacity = machineCapacity.get(shiftIndex);
                if (remainingCapacity == null || remainingCapacity.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                // 获取本机台本班待排产规格
                List<DjPaddingDemand> pendingSpecs = getSpecsByMachine(machineCode, demandList);

                // 获取上一班最后生产的规格
                String lastSpecCode = lastSpecInShift.get(machineCode);

                // 优先级排序
                pendingSpecs.sort(buildPriorityComparator(demandList, lastSpecCode, context, shiftIndex));

                // 机台剩余产能不能超过本班台车约束的剩余量
                remainingCapacity = remainingCapacity.min(shiftRemainingCapacity);

                // 逐个规格安排排产
                for (DjPaddingDemand spec : pendingSpecs) {
                    if (!spec.isNeedProduce() || spec.getRemainingDemand() == null
                            || spec.getRemainingDemand().compareTo(BigDecimal.ZERO) <= 0) {
                        continue;
                    }
                    if (remainingCapacity.compareTo(BigDecimal.ZERO) <= 0) {
                        break; // 本班该机台产能已用完
                    }

                    // 计算机台切换损失
                    BigDecimal mouthPlateSwitchTime = this.getParamAsDecimal(context, DjEngineConstants.PARAM_MOUTH_PLATE_SWITCH_TIME);
                    BigDecimal switchLoss = this.calcSwitchLoss(machine, lastSpecCode, spec, mouthPlateSwitchTime);

                    // 生产量计算
                    BigDecimal produceQty = calcProduceQty(spec, remainingCapacity.subtract(switchLoss));
                    if (produceQty == null || produceQty.compareTo(BigDecimal.ZERO) <= 0) {
                        continue; // 放不下此规格
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

                    // 扣减本班台车约束剩余量
                    shiftRemainingCapacity = shiftRemainingCapacity.subtract(produceQty);
                    if (shiftRemainingCapacity.compareTo(BigDecimal.ZERO) < 0) {
                        shiftRemainingCapacity = BigDecimal.ZERO;
                    }
                }
            }

            // 本班排产完毕后，计算各规格交班库存
            for (DjPaddingDemand spec : demandList) {
                BigDecimal produceQtyThisShift = this.getScheduledQty(resultMap, spec.getPaddingCode(), shiftIndex);
                // 本班成型消耗量
                BigDecimal consumeQtyThisShift = consumeQty.getOrDefault(spec.getPaddingCode(), new HashMap<>())
                        .getOrDefault(shiftIndex, BigDecimal.ZERO);
                BigDecimal incomingInv = handoverInventory.getOrDefault(spec.getPaddingCode(), BigDecimal.ZERO);
                BigDecimal newInventory = incomingInv.add(produceQtyThisShift).subtract(consumeQtyThisShift);
                if (newInventory.compareTo(BigDecimal.ZERO) < 0) {
                    newInventory = BigDecimal.ZERO;
                }
                handoverInventory.put(spec.getPaddingCode(), newInventory);
            }

            // 检查终止条件：所有需求已排完
            boolean allMet = demandList.stream().allMatch(
                    d -> d.getRemainingDemand() == null || d.getRemainingDemand().compareTo(BigDecimal.ZERO) <= 0);
            if (allMet) {
                log.info("所有垫胶需求已排完，提前终止排产（班次 {}/{}）", shiftIndex, DjEngineConstants.SHIFT_COUNT);
                break;
            }
        }

        // 转换结果为列表
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
                // 记录排程时使用的有效库存
                result.setStockQty(
                        context.getEffectiveStockMap().getOrDefault(spec.getPaddingCode(), BigDecimal.ZERO));
                results.add(result);
            }
        }

        return results;
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
        Map<String, BigDecimal> handoverInventory = context.getHandoverInventory();
        Map<String, Map<Integer, BigDecimal>> consumeQty = context.getConsumeQty();
        Map<String, Integer> paddingSupplyDepth = context.getPaddingSupplyDepth();
        Map<String, BigDecimal> paddingRemainingMap = context.getPaddingRemainingMap();

        // 排产触发阈值：当前库存可覆盖班次数 ≤ 此值时触发排产
        int scheduleThreshold = this.getParamAsDecimal(context, DjEngineConstants.PARAM_SCHEDULE_THRESHOLD).intValue();

        for (DjPaddingDemand spec : demandList) {
            BigDecimal incomingInventory = handoverInventory.getOrDefault(spec.getPaddingCode(), BigDecimal.ZERO);
            spec.setIncomingInventory(incomingInventory);

            // 该规格的备库班数（排产深度）
            int specSupplyDepth = paddingSupplyDepth.getOrDefault(spec.getPaddingCode(),
                    DjEngineConstants.CX_SHIFT_COUNT);

            // ===== 步骤1：计算可覆盖班次数 coverableShiftCount =====
            Map<Integer, BigDecimal> shiftConsume = consumeQty.getOrDefault(spec.getPaddingCode(), new HashMap<>());
            BigDecimal accumulateConsume = BigDecimal.ZERO;
            int coverableShiftCount = 0;
            int maxCheckShift = Math.min(shiftIndex + specSupplyDepth - 1, DjEngineConstants.CX_SHIFT_COUNT);
            for (int k = shiftIndex; k <= maxCheckShift; k++) {
                BigDecimal consume = shiftConsume.getOrDefault(k, BigDecimal.ZERO);
                if (consume.compareTo(BigDecimal.ZERO) <= 0) {
                    coverableShiftCount++; // 无消耗的班次视为可覆盖
                    continue;
                }
                if (accumulateConsume.add(consume).compareTo(incomingInventory) <= 0) {
                    accumulateConsume = accumulateConsume.add(consume);
                    coverableShiftCount++;
                } else {
                    break; // 当前班次的消耗量已无法完全覆盖
                }
            }
            spec.setCoverableShiftCount(coverableShiftCount);

            // ===== 步骤2：判断是否触发排产 =====
            if (coverableShiftCount > scheduleThreshold) {
                spec.setNeedProduce(false); // 库存充足，不排产
                continue;
            }

            // ===== 步骤3：触发排产，计算净需求量 =====
            spec.setNeedProduce(true);
            BigDecimal netDemand;

            if (shiftIndex + specSupplyDepth - 1 <= DjEngineConstants.CX_SHIFT_COUNT) {
                // 全部在成型计划范围内（n ≤ 8）
                BigDecimal demandInWindow = BigDecimal.ZERO;
                for (int i = shiftIndex; i < shiftIndex + specSupplyDepth; i++) {
                    demandInWindow = demandInWindow.add(shiftConsume.getOrDefault(i, BigDecimal.ZERO));
                }
                netDemand = demandInWindow.subtract(incomingInventory);
                if (netDemand.compareTo(BigDecimal.ZERO) < 0) {
                    netDemand = BigDecimal.ZERO;
                }
            } else {
                // 超出成型计划的8个班（n > 8）
                // part1: 第 shiftIndex~8 班部分
                BigDecimal part1 = BigDecimal.ZERO;
                for (int i = shiftIndex; i <= DjEngineConstants.CX_SHIFT_COUNT; i++) {
                    part1 = part1.add(shiftConsume.getOrDefault(i, BigDecimal.ZERO));
                }
                part1 = part1.subtract(incomingInventory);
                if (part1.compareTo(BigDecimal.ZERO) < 0) {
                    part1 = BigDecimal.ZERO;
                }

                // 最后3个班次消耗量之和（第6/7/8班）
                BigDecimal last3Sum = BigDecimal.ZERO;
                for (int i = 6; i <= DjEngineConstants.CX_SHIFT_COUNT; i++) {
                    last3Sum = last3Sum.add(shiftConsume.getOrDefault(i, BigDecimal.ZERO));
                }
                // 如果最后3班无数据，用当前班次到第8班的消耗量总计兜底
                if (last3Sum.compareTo(BigDecimal.ZERO) <= 0) {
                    for (int i = shiftIndex; i <= DjEngineConstants.CX_SHIFT_COUNT; i++) {
                        last3Sum = last3Sum.add(shiftConsume.getOrDefault(i, BigDecimal.ZERO));
                    }
                }
                BigDecimal avgLast3Shifts = last3Sum.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);

                // 超出部分班次数
                int exceedShiftCount = specSupplyDepth - DjEngineConstants.CX_SHIFT_COUNT;
                BigDecimal estimatedExceed = avgLast3Shifts.multiply(BigDecimal.valueOf(exceedShiftCount));

                // 月计划剩余量约束
                BigDecimal paddingRemaining = paddingRemainingMap != null
                        ? paddingRemainingMap.getOrDefault(spec.getPaddingCode(), BigDecimal.ZERO)
                        : BigDecimal.ZERO;
                BigDecimal part2 = estimatedExceed;
                BigDecimal part1PlusPart2 = part1.add(part2);
                if (part1PlusPart2.compareTo(paddingRemaining) > 0) {
                    part2 = paddingRemaining.subtract(part1);
                    if (part2.compareTo(BigDecimal.ZERO) < 0) {
                        part2 = BigDecimal.ZERO;
                    }
                }
                netDemand = part1.add(part2);
            }

            // ===== 步骤4：已收尾规格净需求含损耗率 =====
            if (spec.isTailFinished()) {
                BigDecimal lossRate = this.getLossRate(spec.getPaddingCode(), spec.getMachineCode(), context);
                netDemand = netDemand.multiply(BigDecimal.ONE.add(lossRate)).setScale(2, RoundingMode.HALF_UP);
            }

            spec.setNetDemand(netDemand);
            // 剩余待排产量初始=净需求
            if (spec.getRemainingDemand() == null || spec.getRemainingDemand().compareTo(netDemand) < 0) {
                spec.setRemainingDemand(netDemand);
            }
        }
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
     * 构建优先级比较器（9级规则）
     */
    private Comparator<DjPaddingDemand> buildPriorityComparator(List<DjPaddingDemand> demandList, String lastSpecCode,
            DjScheduleContext context, int shiftIndex) {
        return (a, b) -> {
            // 1. 本班有缺口优先
            if (a.isNeedProduce() != b.isNeedProduce()) {
                return a.isNeedProduce() ? -1 : 1;
            }

            // 2. 规格续作优先
            boolean aIsLast = a.getPaddingCode() != null && a.getPaddingCode().equals(lastSpecCode);
            boolean bIsLast = b.getPaddingCode() != null && b.getPaddingCode().equals(lastSpecCode);
            if (aIsLast != bIsLast) {
                return aIsLast ? -1 : 1;
            }

            // 3. 胶料相同 + 口型相同优先
            if (lastSpecCode != null) {
                DjPaddingDemand lastSpec = this.findSpecByCode(demandList, lastSpecCode);
                if (lastSpec != null) {
                    boolean aMatchGlueAndMouth = this.isGlueAndMouthMatch(a, lastSpec);
                    boolean bMatchGlueAndMouth = this.isGlueAndMouthMatch(b, lastSpec);
                    if (aMatchGlueAndMouth != bMatchGlueAndMouth) {
                        return aMatchGlueAndMouth ? -1 : 1;
                    }

                    // 4. 胶料相同优先
                    boolean aGlueMatch = a.getGlueCode() != null && a.getGlueCode().equals(lastSpec.getGlueCode());
                    boolean bGlueMatch = b.getGlueCode() != null && b.getGlueCode().equals(lastSpec.getGlueCode());
                    if (aGlueMatch != bGlueMatch) {
                        return aGlueMatch ? -1 : 1;
                    }

                    // 5. 口型相同优先
                    boolean aMouthMatch = a.getMouthPlateCode() != null
                            && a.getMouthPlateCode().equals(lastSpec.getMouthPlateCode());
                    boolean bMouthMatch = b.getMouthPlateCode() != null
                            && b.getMouthPlateCode().equals(lastSpec.getMouthPlateCode());
                    if (aMouthMatch != bMouthMatch) {
                        return aMouthMatch ? -1 : 1;
                    }
                }
            }

            // 6. 胶料组相同优先
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

            // 7. 胶料序号升序
            Map<String, Integer> glueOrderMap = context.getGlueOrderMap();
            Integer aSeq = glueOrderMap != null ? glueOrderMap.get(a.getGlueCode()) : null;
            Integer bSeq = glueOrderMap != null ? glueOrderMap.get(b.getGlueCode()) : null;
            if (aSeq != null && bSeq != null && !aSeq.equals(bSeq)) {
                return Integer.compare(aSeq, bSeq);
            }

            // 8. 库消比升序
            BigDecimal aRatio = this.calcStockConsumeRatio(a, context);
            BigDecimal bRatio = this.calcStockConsumeRatio(b, context);
            if (aRatio != null && bRatio != null) {
                int ratioCmp = aRatio.compareTo(bRatio);
                if (ratioCmp != 0) {
                    return ratioCmp;
                }
            }

            // 9. 垫胶编码升序（兜底）
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
        Map<String, Map<Integer, BigDecimal>> consumeQty = context.getConsumeQty();
        Map<Integer, BigDecimal> shiftConsume = consumeQty.get(spec.getPaddingCode());
        if (shiftConsume == null || shiftConsume.isEmpty()) {
            return null;
        }
        // 计算日均消耗量（取前3个班）
        BigDecimal avgDailyConsume = BigDecimal.ZERO;
        int count = 0;
        for (int i = 1; i <= 3; i++) {
            BigDecimal consume = shiftConsume.get(i);
            if (consume != null && consume.compareTo(BigDecimal.ZERO) > 0) {
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
        // 切换损失 = (切换时长 / 8小时) × 定额
        BigDecimal quata = machine.getQuata() != null ? machine.getQuata() : BigDecimal.ZERO;
        return mouthPlateSwitchTime.divide(DjEngineConstants.SHIFT_HOURS, 4, RoundingMode.HALF_UP).multiply(quata).setScale(2,
                RoundingMode.HALF_UP);
    }

    /**
     * 计算生产量
     */
    private BigDecimal calcProduceQty(DjPaddingDemand spec, BigDecimal remainingCapacity) {
        if (remainingCapacity == null || remainingCapacity.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal remainingDemand = spec.getRemainingDemand() != null ? spec.getRemainingDemand() : BigDecimal.ZERO;
        if (remainingDemand.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // 已收尾规格：精确排产，不要求台车整倍数
        if (spec.isTailFinished()) {
            return remainingDemand.min(remainingCapacity);
        }

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

        return BigDecimal.valueOf(actualTrolleys).multiply(trolleyCapacity);
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
        result.setFieldValueByFieldName(String.format(DjEngineConstants.CLASS_PLAN_QTY_FIELD, shiftIndex), produceQty);
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
}
