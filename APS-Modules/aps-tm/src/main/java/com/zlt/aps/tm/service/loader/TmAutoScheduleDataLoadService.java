package com.zlt.aps.tm.service.loader;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.tm.api.constant.TmScheduleConstants;
import com.zlt.aps.tm.api.domain.entity.*;
import com.zlt.aps.tm.api.enums.*;
import com.zlt.aps.tm.domain.vo.*;
import com.zlt.aps.tm.engine.domain.*;
import com.zlt.aps.tm.mapper.*;
import com.zlt.aps.tm.service.cache.TmAutoScheduleRedisCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎面自动排程数据加载服务。
 *
 * <p>该服务属于胎面业务模块，负责在自动排程入口事务内读取排程需要的基础数据并填充
 * {@link TmScheduleContext}。服务只做数据加载和任务草稿构造，不写排程结果、不修改任务链。</p>
 */
@Slf4j
@Service
public class TmAutoScheduleDataLoadService {

    private final TmAutoScheduleRedisCacheService tmAutoScheduleRedisCacheService;
    /** 参数装载组件，只负责构建单次排程参数快照 */
    private final TmScheduleParamLoader tmScheduleParamLoader;
    /** 损耗规则装载组件，只负责业务配置到引擎规则的转换 */
    private final TmLossRuleLoader tmLossRuleLoader;
    @Resource
    private TmParamsMapper tmParamsMapper;
    @Resource
    private TmMachineInfoMapper tmMachineInfoMapper;
    @Resource
    private TmMouthPlateMapper tmMouthPlateMapper;
    @Resource
    private TmGlueMachineRealMapper tmGlueMachineRealMapper;
    @Resource
    private TmSpecifyMachineMapper tmSpecifyMachineMapper;
    @Resource
    private TmMachineSpeedMapper tmMachineSpeedMapper;
    @Resource
    private TmMachineMaintenanceMapper tmMachineMaintenanceMapper;
    @Resource
    private TmCurlRollMapper tmCurlRollMapper;
    @Resource
    private TmLossSettingMapper tmLossSettingMapper;
    @Resource
    private TmAutoScheduleDataLoadMapper tmAutoScheduleDataLoadMapper;
    @Resource
    private TmStockMapper tmStockMapper;
    @Resource
    private TmScheduleResultMapper tmScheduleResultMapper;
    @Resource
    private TmDepthConfigMapper tmDepthConfigMapper;
    @Resource
    private TmShiftConfigMapper tmShiftConfigMapper;

    /**
     * 创建胎面自动排程数据加载服务。
     *
     * @param tmAutoScheduleRedisCacheService 自动排程 Redis 缓存服务
     */
    public TmAutoScheduleDataLoadService(TmAutoScheduleRedisCacheService tmAutoScheduleRedisCacheService) {
        this.tmAutoScheduleRedisCacheService = tmAutoScheduleRedisCacheService;
        this.tmScheduleParamLoader = new TmScheduleParamLoader();
        this.tmLossRuleLoader = new TmLossRuleLoader();
    }

    /**
     * 加载自动排程所需数据。
     *
     * @param context 自动排程上下文，必须包含工厂和排程日期
     * @throws IllegalArgumentException 上下文、工厂或排程日期为空时抛出
     */
    public void loadAllData(TmScheduleContext context) {
        validateContext(context);
        this.tmScheduleParamLoader.load(context, tmParamsMapper, tmAutoScheduleRedisCacheService);
        List<TmMachineInfo> machineList = loadMachineInfo(context);
        context.setMachineCandidateList(loadMachineCandidates(context, machineList));
        context.setLossRuleList(this.tmLossRuleLoader.load(context, tmLossSettingMapper));
        List<TmTaskDraft> taskDraftList = loadFormingDemandTasks(context, machineList);
        this.applyCalendarRules(context, taskDraftList, machineList);
        this.addCurrentDayShutdownTraces(context, taskDraftList);
        fillTaskAuxiliaryData(context, taskDraftList);
        context.setTaskDraftList(taskDraftList);
        log.info("[TM_AUTO_SCHEDULE_LOAD] factoryCode={}, scheduleDate={}, taskCount={}, machineCount={}",
                context.getFactoryCode(), DateUtil.formatDate(context.getScheduleDate()),
                taskDraftList.size(), machineList.size());
    }

    /**
     * 加载胎面机台基础资料。
     *
     * @param context 自动排程上下文
     * @return 工厂下全部机台列表，停用机台由引擎硬约束过滤并保留拒绝证据
     */
    private List<TmMachineInfo> loadMachineInfo(TmScheduleContext context) {
        LambdaQueryWrapper<TmMachineInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmMachineInfo::getFactoryCode, context.getFactoryCode());
        wrapper.orderByAsc(TmMachineInfo::getMachineCode);
        return tmAutoScheduleRedisCacheService.getCachedList("machine:v2:" + context.getFactoryCode(),
                () -> tmMachineInfoMapper.selectList(wrapper)).stream()
                .filter(machine -> StrUtil.isNotBlank(machine.getMachineCode()))
                .collect(Collectors.toList());
    }

    /**
     * 构建胎面候选机台基础数据。
     *
     * <p>该方法只准备与任务无关或可复用的机台能力集合；口型、胶料、定点/禁排和剩余产能会在
     * {@code TmMachineAssignService} 中结合具体任务再次计算，避免不同任务复用同一候选列表导致误判。</p>
     *
     * @param context     自动排程上下文
     * @param machineList 机台基础资料
     * @return 候选机台列表
     */
    private List<TmMachineCandidate> loadMachineCandidates(TmScheduleContext context, List<TmMachineInfo> machineList) {
        if (CollUtil.isEmpty(machineList)) {
            return Collections.emptyList();
        }
        Map<String, TmMachineCandidate> candidateMap = new LinkedHashMap<>();
        for (TmMachineInfo machineInfo : machineList) {
            TmMachineCandidate candidate = new TmMachineCandidate();
            candidate.setMachineCode(machineInfo.getMachineCode());
            candidate.setEnabled(isMachineEnabled(machineInfo));
            candidate.setOpenShiftCodes(this.parseOpenShiftCodes(machineInfo.getOpenShiftCode()));
            candidate.setMaxCapacity(nvl(machineInfo.getMaxCapacity()));
            candidate.setRemainCapacity(nvl(machineInfo.getMaxCapacity()));
            candidate.setMaintenanceHours(BigDecimal.ZERO);
            candidate.setSwitchCostHours(BigDecimal.ZERO);
            candidate.setConfiguredMouthPlateCodes(new HashSet<>());
            candidate.setMouthPlateCodes(new HashSet<>());
            candidate.setConfiguredGlueCodes(new HashSet<>());
            candidate.setAllowedGlueCodes(new HashSet<>());
            candidate.setForbiddenGlueCodes(new HashSet<>());
            candidate.setConfiguredFixedAllowTreadCodes(new HashSet<>());
            candidate.setFixedAllowTreadCodes(new HashSet<>());
            candidate.setFixedForbidTreadCodes(new HashSet<>());
            candidateMap.put(machineInfo.getMachineCode(), candidate);
        }
        fillCandidateMouthPlate(context, candidateMap);
        fillCandidateGlueRule(context, candidateMap);
        fillCandidateSpecifyRule(context, candidateMap);
        fillCandidateSpeed(context, candidateMap);
        List<TmShiftConfig> shiftConfigList = this.loadOpenShiftConfigs(context);
        fillCandidateMaintenance(context, candidateMap, shiftConfigList);
        fillCandidatePredecessor(context, candidateMap);
        fillShiftHoursMap(context, shiftConfigList);
        return new ArrayList<>(candidateMap.values());
    }

    /**
     * 填充候选机台口型板能力。
     *
     * @param context      自动排程上下文
     * @param candidateMap 候选机台映射
     */
    private void fillCandidateMouthPlate(TmScheduleContext context, Map<String, TmMachineCandidate> candidateMap) {
        if (tmMouthPlateMapper == null || candidateMap.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<TmMouthPlate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmMouthPlate::getFactoryCode, context.getFactoryCode());
        wrapper.in(TmMouthPlate::getMachineCode, candidateMap.keySet());
        List<TmMouthPlate> mouthPlateList = tmMouthPlateMapper.selectList(wrapper);
        for (TmMouthPlate mouthPlate : mouthPlateList) {
            TmMachineCandidate candidate = candidateMap.get(mouthPlate.getMachineCode());
            if (candidate == null || StrUtil.isBlank(mouthPlate.getMouthPlateCode())) {
                continue;
            }
            // 口型板配置必须按机台隔离；候选机台自身无有效配置时，空集合表示不限制口型板。
            if (candidate.getConfiguredMouthPlateCodes() == null) {
                candidate.setConfiguredMouthPlateCodes(new HashSet<>());
            }
            candidate.getConfiguredMouthPlateCodes().add(mouthPlate.getMouthPlateCode());
            if (candidate.getMouthPlateCodes() == null) {
                candidate.setMouthPlateCodes(new HashSet<>());
            }
            candidate.getMouthPlateCodes().add(mouthPlate.getMouthPlateCode());
        }
    }

    /**
     * 填充候选机台胶料禁用规则。
     *
     * @param context      自动排程上下文
     * @param candidateMap 候选机台映射
     */
    private void fillCandidateGlueRule(TmScheduleContext context, Map<String, TmMachineCandidate> candidateMap) {
        if (tmGlueMachineRealMapper == null || candidateMap.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<TmGlueMachineReal> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmGlueMachineReal::getFactoryCode, context.getFactoryCode());
        wrapper.eq(TmGlueMachineReal::getEnableStatus, TmYesNoEnum.YES.getCode());
        wrapper.in(TmGlueMachineReal::getMachineCode, candidateMap.keySet());
        List<TmGlueMachineReal> glueRuleList = tmGlueMachineRealMapper.selectList(wrapper);
        for (TmGlueMachineReal glueRule : glueRuleList) {
            if (StrUtil.isBlank(glueRule.getGlueCode())
                    || (!TmYesNoEnum.YES.getCode().equals(glueRule.getAllowFlag())
                    && !TmYesNoEnum.NO.getCode().equals(glueRule.getAllowFlag()))) {
                continue;
            }
            TmMachineCandidate candidate = candidateMap.get(glueRule.getMachineCode());
            if (candidate == null) {
                continue;
            }
            // 胶料关系必须按机台隔离；当前机台未配置该主胶料时默认不限制。
            if (candidate.getConfiguredGlueCodes() == null) {
                candidate.setConfiguredGlueCodes(new HashSet<>());
            }
            candidate.getConfiguredGlueCodes().add(glueRule.getGlueCode());
            if (TmYesNoEnum.YES.getCode().equals(glueRule.getAllowFlag())) {
                if (candidate.getAllowedGlueCodes() == null) {
                    candidate.setAllowedGlueCodes(new HashSet<>());
                }
                candidate.getAllowedGlueCodes().add(glueRule.getGlueCode());
            } else {
                if (candidate.getForbiddenGlueCodes() == null) {
                    candidate.setForbiddenGlueCodes(new HashSet<>());
                }
                candidate.getForbiddenGlueCodes().add(glueRule.getGlueCode());
            }
        }
    }

    /**
     * 填充候选机台定点和禁排规则。
     *
     * @param context      自动排程上下文
     * @param candidateMap 候选机台映射
     */
    private void fillCandidateSpecifyRule(TmScheduleContext context, Map<String, TmMachineCandidate> candidateMap) {
        if (tmSpecifyMachineMapper == null || candidateMap.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<TmSpecifyMachine> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmSpecifyMachine::getFactoryCode, context.getFactoryCode());
        wrapper.eq(TmSpecifyMachine::getEnableStatus, TmYesNoEnum.YES.getCode());
        wrapper.in(TmSpecifyMachine::getMachineCode, candidateMap.keySet());
        List<TmSpecifyMachine> specifyList = tmSpecifyMachineMapper.selectList(wrapper);
        Set<String> configuredFixedAllowTreadCodes = specifyList.stream()
                .filter(specify -> TmSpecifyMachineJobTypeEnum.ALLOW.getCode().equals(specify.getJobType()))
                .map(TmSpecifyMachine::getTreadCode)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
        for (TmMachineCandidate candidate : candidateMap.values()) {
            if (candidate.getConfiguredFixedAllowTreadCodes() == null) {
                candidate.setConfiguredFixedAllowTreadCodes(new HashSet<>());
            }
            candidate.getConfiguredFixedAllowTreadCodes().addAll(configuredFixedAllowTreadCodes);
        }
        for (TmSpecifyMachine specify : specifyList) {
            TmMachineCandidate candidate = candidateMap.get(specify.getMachineCode());
            if (candidate == null || StrUtil.isBlank(specify.getTreadCode())) {
                continue;
            }
            if (TmSpecifyMachineJobTypeEnum.ALLOW.getCode().equals(specify.getJobType())) {
                if (candidate.getFixedAllowTreadCodes() == null) {
                    candidate.setFixedAllowTreadCodes(new HashSet<>());
                }
                candidate.getFixedAllowTreadCodes().add(specify.getTreadCode());
            } else if (TmSpecifyMachineJobTypeEnum.FORBID.getCode().equals(specify.getJobType())) {
                if (candidate.getFixedForbidTreadCodes() == null) {
                    candidate.setFixedForbidTreadCodes(new HashSet<>());
                }
                candidate.getFixedForbidTreadCodes().add(specify.getTreadCode());
            }
        }
    }

    /**
     * 填充候选机台生产速度。
     *
     * @param context      自动排程上下文
     * @param candidateMap 候选机台映射
     */
    private void fillCandidateSpeed(TmScheduleContext context, Map<String, TmMachineCandidate> candidateMap) {
        if (tmMachineSpeedMapper == null || candidateMap.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<TmMachineSpeed> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmMachineSpeed::getFactoryCode, context.getFactoryCode());
        List<TmMachineSpeed> speedList = tmMachineSpeedMapper.selectList(wrapper);
        for (TmMachineSpeed speed : speedList) {
            if (speed.getProductSpeed() == null || speed.getProductSpeed().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (StrUtil.isBlank(speed.getMachineCode()) && StrUtil.isNotBlank(speed.getTreadCode())) {
                for (TmMachineCandidate candidate : candidateMap.values()) {
                    candidate.getTreadSpeedMap().putIfAbsent(speed.getTreadCode(), speed.getProductSpeed());
                }
                continue;
            }
            TmMachineCandidate candidate = candidateMap.get(speed.getMachineCode());
            if (candidate == null) {
                continue;
            }
            if (StrUtil.isBlank(speed.getTreadCode())) {
                candidate.setMachineSpeed(speed.getProductSpeed());
            } else {
                candidate.getTreadSpeedMap().put(speed.getTreadCode(), speed.getProductSpeed());
            }
        }
    }

    /**
     * 填充候选机台排程日检修时长。
     *
     * <p>当存在启用班次配置时，按维修窗口与每个班次窗口的实际重叠分钟分摊到班次；
     * 当班次配置缺失时，保留旧逻辑按排程自然日汇总到机台维度，避免基础数据缺失导致扣减完全失效。</p>
     *
     * @param context         自动排程上下文
     * @param candidateMap    候选机台映射
     * @param shiftConfigList 启用班次配置列表
     */
    private void fillCandidateMaintenance(TmScheduleContext context, Map<String, TmMachineCandidate> candidateMap,
                                          List<TmShiftConfig> shiftConfigList) {
        if (tmMachineMaintenanceMapper == null || candidateMap.isEmpty()) {
            return;
        }
        Map<Integer, Date[]> shiftWindowMap = this.buildShiftWindowMap(context, shiftConfigList);
        if (CollUtil.isNotEmpty(shiftWindowMap)) {
            candidateMap.values().forEach(candidate -> shiftWindowMap.keySet()
                    .forEach(shiftOrder -> candidate.getMaintenanceHoursByShift().put(shiftOrder, BigDecimal.ZERO)));
        }
        LambdaQueryWrapper<TmMachineMaintenance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmMachineMaintenance::getFactoryCode, context.getFactoryCode());
        wrapper.in(TmMachineMaintenance::getMachineCode, candidateMap.keySet());
        List<TmMachineMaintenance> maintenanceList = tmMachineMaintenanceMapper.selectList(wrapper);
        Date dayStart = DateUtil.beginOfDay(context.getScheduleDate());
        Date dayEnd = DateUtil.endOfDay(context.getScheduleDate());
        for (TmMachineMaintenance maintenance : nullToEmpty(maintenanceList)) {
            TmMachineCandidate candidate = candidateMap.get(maintenance.getMachineCode());
            if (candidate == null || maintenance.getStopStartTime() == null || maintenance.getStopEndTime() == null) {
                continue;
            }
            BigDecimal dayHours = this.calculateOverlapHours(maintenance.getStopStartTime(), maintenance.getStopEndTime(),
                    dayStart, dayEnd);
            candidate.setMaintenanceHours(nvl(candidate.getMaintenanceHours()).add(dayHours));
            if (CollUtil.isEmpty(shiftWindowMap)) {
                continue;
            }
            for (Map.Entry<Integer, Date[]> entry : shiftWindowMap.entrySet()) {
                Date[] shiftWindow = entry.getValue();
                BigDecimal shiftHours = this.calculateOverlapHours(maintenance.getStopStartTime(),
                        maintenance.getStopEndTime(), shiftWindow[0], shiftWindow[1]);
                if (shiftHours.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                candidate.getMaintenanceHoursByShift().merge(entry.getKey(), shiftHours, BigDecimal::add);
            }
        }
    }

    /**
     * 填充当前排程日一班开始前的同机台前置任务快照。
     *
     * <p>前置任务只取当前排程日前一天同工厂、同机台有效排程结果中有顺序且计划量大于0的最后任务，
     * 作为一班链式连续排序的虚拟链尾。</p>
     *
     * @param context 自动排程上下文
     * @param candidateMap 候选机台映射
     */
    private void fillCandidatePredecessor(TmScheduleContext context, Map<String, TmMachineCandidate> candidateMap) {
        if (tmScheduleResultMapper == null || candidateMap.isEmpty()) {
            context.setMachinePredecessorMap(Collections.emptyMap());
            return;
        }
        Date previousDate = DateUtil.offsetDay(DateUtil.beginOfDay(context.getScheduleDate()), -1);
        LambdaQueryWrapper<TmScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmScheduleResult::getFactoryCode, context.getFactoryCode());
        wrapper.eq(TmScheduleResult::getScheduleDate, previousDate);
        wrapper.in(TmScheduleResult::getMachineCode, candidateMap.keySet());
        List<TmScheduleResult> resultList = Optional.ofNullable(tmScheduleResultMapper.selectList(wrapper))
                .orElse(Collections.emptyList());
        Map<String, TmTaskPredecessor> predecessorMap = new LinkedHashMap<>();
        for (TmScheduleResult result : resultList) {
            if (result == null || StrUtil.isBlank(result.getMachineCode())) {
                continue;
            }
            TmTaskPredecessor predecessor = this.resolveLatestPredecessor(result);
            if (predecessor == null) {
                continue;
            }
            TmTaskPredecessor exists = predecessorMap.get(predecessor.getMachineCode());
            if (exists == null || this.isLaterPredecessor(predecessor, exists)) {
                predecessorMap.put(predecessor.getMachineCode(), predecessor);
            }
        }
        for (Map.Entry<String, TmTaskPredecessor> entry : predecessorMap.entrySet()) {
            TmMachineCandidate candidate = candidateMap.get(entry.getKey());
            if (candidate == null) {
                continue;
            }
            TmTaskPredecessor predecessor = entry.getValue();
            candidate.setTailMainGlueCode(predecessor.getGlueCode());
            candidate.setTailBaseGlueCode(predecessor.getBaseGlueCode());
            candidate.setTailMouthPlateCode(predecessor.getMouthPlateCode());
        }
        context.setMachinePredecessorMap(predecessorMap);
        log.info("[TM_MACHINE_PREDECESSOR_LOAD] factoryCode={}, scheduleDate={}, previousDate={}, predecessorCount={}, predecessors={}",
                context.getFactoryCode(), DateUtil.formatDate(context.getScheduleDate()), DateUtil.formatDate(previousDate),
                predecessorMap.size(), predecessorMap);
    }

    /**
     * 构建排程日各班次的时间窗口。
     *
     * <p>班次配置只保存时分秒，运行时结合当前排程日期生成完整时间；跨天班次的结束时间顺延一天，
     * 用于把机台检修时长精确分摊到每个班次。</p>
     *
     * @param context         自动排程上下文
     * @param shiftConfigList 启用班次配置列表
     * @return 班次顺序到开始、结束时间的映射；配置不完整时跳过对应班次
     */
    private Map<Integer, Date[]> buildShiftWindowMap(TmScheduleContext context, List<TmShiftConfig> shiftConfigList) {
        Map<Integer, Date[]> shiftWindowMap = new LinkedHashMap<>();
        if (context == null || context.getScheduleDate() == null || CollUtil.isEmpty(shiftConfigList)) {
            return shiftWindowMap;
        }
        String scheduleDateText = DateUtil.formatDate(context.getScheduleDate());
        Date previousEndTime = null;
        for (TmShiftConfig config : shiftConfigList) {
            if (config == null || config.getShiftOrder() == null
                    || StrUtil.isBlank(config.getPlanStartTime()) || StrUtil.isBlank(config.getPlanEndTime())) {
                continue;
            }
            try {
                Date startTime = DateUtil.parse(scheduleDateText + " " + config.getPlanStartTime());
                Date endTime = DateUtil.parse(scheduleDateText + " " + config.getPlanEndTime());
            if (TmYesNoEnum.YES.getCode().equals(config.getCrossDayFlag()) || !endTime.after(startTime)) {
                    endTime = DateUtil.offsetDay(endTime, 1);
                }
                while (previousEndTime != null && startTime.before(previousEndTime)) {
                    startTime = DateUtil.offsetDay(startTime, 1);
                    endTime = DateUtil.offsetDay(endTime, 1);
                }
                shiftWindowMap.put(config.getShiftOrder(), new Date[]{startTime, endTime});
                previousEndTime = endTime;
            } catch (Exception exception) {
                log.warn("[TM_SHIFT_WINDOW_PARSE_FAIL] factoryCode={}, scheduleDate={}, shiftOrder={}, startTime={}, endTime={}",
                        context.getFactoryCode(), scheduleDateText, config.getShiftOrder(),
                        config.getPlanStartTime(), config.getPlanEndTime(), exception);
            }
        }
        return shiftWindowMap;
    }

    /**
     * 计算两个时间段的重叠小时数。
     *
     * @param sourceStart 源时间段开始时间
     * @param sourceEnd   源时间段结束时间
     * @param targetStart 目标时间段开始时间
     * @param targetEnd   目标时间段结束时间
     * @return 重叠小时数；无重叠时返回 0
     */
    private BigDecimal calculateOverlapHours(Date sourceStart, Date sourceEnd, Date targetStart, Date targetEnd) {
        if (sourceStart == null || sourceEnd == null || targetStart == null || targetEnd == null) {
            return BigDecimal.ZERO;
        }
        Date overlapStart = sourceStart.after(targetStart) ? sourceStart : targetStart;
        Date overlapEnd = sourceEnd.before(targetEnd) ? sourceEnd : targetEnd;
        if (!overlapStart.before(overlapEnd)) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(DateUtil.between(overlapStart, overlapEnd, DateUnit.MINUTE))
                .divide(BigDecimal.valueOf(TmScheduleConstants.MINUTES_PER_HOUR),
                        TmScheduleConstants.DECIMAL_CALCULATION_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 将可能为空的列表转换为空列表兜底。
     *
     * @param sourceList 原始列表
     * @param <T>        列表元素类型
     * @return 非空列表
     */
    private <T> List<T> nullToEmpty(List<T> sourceList) {
        return sourceList == null ? Collections.emptyList() : sourceList;
    }

    /**
     * 加载工厂启用班次配置。
     *
     * <p>数据加载阶段只查询一次启用班次配置，后续检修产能扣减、班次小时数和任务时间窗口均复用该列表，
     * 避免引擎层直接访问数据库。</p>
     *
     * @param context 自动排程上下文
     * @return 启用班次配置列表；未配置时返回空列表
     */
    private List<TmShiftConfig> loadOpenShiftConfigs(TmScheduleContext context) {
        if (tmShiftConfigMapper == null) {
            return Collections.emptyList();
        }
        List<TmShiftConfig> configs = tmShiftConfigMapper.selectList(
                new LambdaQueryWrapper<TmShiftConfig>()
                        .eq(TmShiftConfig::getFactoryCode, context.getFactoryCode()));
        return nullToEmpty(configs).stream()
                .filter(config -> config.getShiftOrder() != null)
                .sorted(Comparator.comparing(TmShiftConfig::getShiftOrder))
                .collect(Collectors.toList());
    }

    /**
     * 解析机台维护的开机班次编码。
     *
     * @param openShiftCode 逗号分隔的开机班次编码
     * @return 去空、去重后的班次编码集合；未维护时返回空集合
     */
    private Set<String> parseOpenShiftCodes(String openShiftCode) {
        if (StrUtil.isBlank(openShiftCode)) {
            return new LinkedHashSet<>();
        }
        return Arrays.stream(openShiftCode.split(","))
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 加载班次小时数和时间窗口映射。
     *
     * <p>查询工厂启用的班次配置，将班次顺序映射到班次时长和计划起止时间，
     * 供机台分配阶段计算生产速度，并供任务链阶段推算预计开始和结束时间。</p>
     *
     * @param context 自动排程上下文
     * @param shiftConfigList 启用班次配置列表
     */
    private void fillShiftHoursMap(TmScheduleContext context, List<TmShiftConfig> shiftConfigList) {
        Map<Integer, BigDecimal> shiftHoursMap = new HashMap<>();
        Map<Integer, TmShiftTimeWindow> shiftTimeWindowMap = new HashMap<>();
        if (CollUtil.isEmpty(shiftConfigList)) {
            context.setShiftHoursMap(shiftHoursMap);
            context.setShiftTimeWindowMap(shiftTimeWindowMap);
            return;
        }
        for (TmShiftConfig config : shiftConfigList) {
            if (config.getShiftOrder() == null) {
                continue;
            }
            if (config.getShiftHours() != null && config.getShiftHours() > 0) {
                shiftHoursMap.put(config.getShiftOrder(), BigDecimal.valueOf(config.getShiftHours()));
            }
            TmShiftTimeWindow window = new TmShiftTimeWindow();
            window.setShiftOrder(config.getShiftOrder());
            window.setShiftCode(config.getShiftCode());
            window.setPlanStartTime(config.getPlanStartTime());
            window.setPlanEndTime(config.getPlanEndTime());
            window.setCrossDayFlag(config.getCrossDayFlag());
            window.setShiftHours(config.getShiftHours() == null ? null : BigDecimal.valueOf(config.getShiftHours()));
            shiftTimeWindowMap.put(config.getShiftOrder(), window);
        }
        context.setShiftHoursMap(shiftHoursMap);
        context.setShiftTimeWindowMap(shiftTimeWindowMap);
    }


    /**
     * 从单条排程结果中解析最后一个有效班次任务。
     *
     * @param result 历史排程结果
     * @return 最后有效前置任务；不存在时返回 null
     */
    private TmTaskPredecessor resolveLatestPredecessor(TmScheduleResult result) {
        for (int shiftOrder = 6; shiftOrder >= 1; shiftOrder--) {
            BigDecimal planQty = this.toBigDecimal(result.getFieldValueByFieldName(
                    String.format("class%dPlanQty", shiftOrder)));
            Integer sequence = this.toInteger(result.getFieldValueByFieldName(
                    String.format("class%dSequence", shiftOrder)));
            if (planQty.compareTo(BigDecimal.ZERO) <= 0 || sequence == null) {
                continue;
            }
            TmTaskPredecessor predecessor = new TmTaskPredecessor();
            predecessor.setMachineCode(result.getMachineCode());
            predecessor.setTreadCode(result.getTreadCode());
            predecessor.setGlueCode(result.getGlueCode());
            predecessor.setBaseGlueCode(result.getBaseGlueCode());
            predecessor.setMouthPlateCode(result.getMouthPlateCode());
            predecessor.setShiftOrder(shiftOrder);
            predecessor.setSequence(sequence);
            predecessor.setBusinessKey(String.valueOf(result.getId()));
            return predecessor;
        }
        return null;
    }

    /**
     * 判断候选前置任务是否晚于当前前置任务。
     *
     * @param candidatePredecessor 候选前置任务
     * @param currentPredecessor 当前前置任务
     * @return true 表示候选任务更靠后
     */
    private boolean isLaterPredecessor(TmTaskPredecessor candidatePredecessor,
                                       TmTaskPredecessor currentPredecessor) {
        int candidateShiftOrder = candidatePredecessor.getShiftOrder() == null ? 0 : candidatePredecessor.getShiftOrder();
        int currentShiftOrder = currentPredecessor.getShiftOrder() == null ? 0 : currentPredecessor.getShiftOrder();
        if (candidateShiftOrder != currentShiftOrder) {
            return candidateShiftOrder > currentShiftOrder;
        }
        int candidateSequence = candidatePredecessor.getSequence() == null ? 0 : candidatePredecessor.getSequence();
        int currentSequence = currentPredecessor.getSequence() == null ? 0 : currentPredecessor.getSequence();
        return candidateSequence > currentSequence;
    }

    /**
     * 将动态字段值转换为计划量数值。
     *
     * @param value 原始字段值
     * @return 计划量；空值或非法值返回0
     */
    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof String && StrUtil.isBlank((String) value)) {
            return BigDecimal.ZERO;
        }
        try {
            return BigDecimalUtils.valueOf(value);
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 将动态字段值转换为顺序号。
     *
     * @param value 原始字段值
     * @return 顺序号；空值或非法值返回 null
     */
    private Integer toInteger(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String && StrUtil.isNotBlank((String) value)) {
            try {
                return Integer.valueOf((String) value);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    /**
     * 成型计划已加载但按 TM_FORMING_SHIFT_OFFSET 偏移后无可排程班次时，记录细化提示到上下文。
     *
     * <p>偏移后胎面班次 N 读取成型 CLASS(N+offset)，6 班覆盖 class(offset+1)~class(6+offset)，
     * 统一夹到 CLASS1~CLASS8 区间；仅当成型行数大于 0 且区间有效时写入，供响应阶段优先展示。</p>
     *
     * @param context            自动排程上下文
     * @param formingRowCount    已加载的成型计划行数
     * @param formingShiftOffset 成型班次偏移量
     */
    private void recordEmptyFormingTaskMessage(TmScheduleContext context, int formingRowCount, int formingShiftOffset) {
        if (formingRowCount <= 0) {
            return;
        }
        // CLASS1~CLASS8 共 8 个班次，偏移后区间夹到 [1,8]
        int clampedFirst = Math.max(1, Math.min(formingShiftOffset + 1, 8));
        int clampedLast = Math.max(1, Math.min(TmScheduleConstants.TM_MAX_SHIFT_ORDER + formingShiftOffset, 8));
        if (clampedFirst > clampedLast) {
            return;
        }
        String template = I18nUtil.getMessage("ui.data.alert.tm.schedule.noTaskGeneratedWithOffset");
        if (StrUtil.isBlank(template) || "ui.data.alert.tm.schedule.noTaskGeneratedWithOffset".equals(template)) {
            template = "胎面自动排程未生成结果：成型计划已加载 {0} 行，但按当前 TM_FORMING_SHIFT_OFFSET 偏移后无可排程班次，偏移后班次：class{1}-class{2}，请确认成型排程班次与 TM_FORMING_SHIFT_OFFSET 配置是否配套";
        }
        context.setEmptyFormingTaskMessage(MessageFormat.format(template, formingRowCount, clampedFirst, clampedLast));
    }

    /**
     * 从成型计划和施工信息构造胎面待排任务。
     *
     * <p>按参数 {@code TM_VERSION_MATCH_MODE} 分流：{@code RECIPE}（默认）走逐班示方书版本解析，
     * {@code BOM} 走原 {@code BOM_DATA_VERSION} 关联逻辑（可随时切换回退）。</p>
     *
     * @param context     自动排程上下文
     * @param machineList 胎面机台列表
     * @return 胎面待排任务列表
     */
    private List<TmTaskDraft> loadFormingDemandTasks(TmScheduleContext context, List<TmMachineInfo> machineList) {
        String versionMatchMode = getParamValue(context, TmScheduleConstants.PARAM_VERSION_MATCH_MODE,
                TmScheduleConstants.DEFAULT_VERSION_MATCH_MODE);
        log.info("[TM_BOOTSTRAP_DETAIL] factoryCode={}, scheduleDate={} 版本匹配模式={}",
                context.getFactoryCode(), DateUtil.formatDate(context.getScheduleDate()), versionMatchMode);
        if (TmVersionMatchModeEnum.BOM == TmVersionMatchModeEnum.resolve(versionMatchMode)) {
            return loadFormingDemandTasksByBom(context, machineList);
        }
        return loadFormingDemandTasksByRecipe(context, machineList);
    }

    /**
     * BOM 模式：原 BOM_DATA_VERSION 关联逻辑，一行成型对应一套胎面属性、6 班共用。
     *
     * @param context     自动排程上下文
     * @param machineList 胎面机台列表
     * @return 胎面待排任务列表
     */
    private List<TmTaskDraft> loadFormingDemandTasksByBom(TmScheduleContext context, List<TmMachineInfo> machineList) {
        List<TmFormingDemandRowVo> rowList;
        try {
            rowList = tmAutoScheduleDataLoadMapper.selectFormingDemandRows(context.getFactoryCode(), context.getScheduleDate());
        } catch (RuntimeException ex) {
            log.warn("[TM_AUTO_SCHEDULE_LOAD] 加载成型计划和施工信息失败，scheduleDate={}，原因={}",
                    DateUtil.formatDate(context.getScheduleDate()), ex.getMessage());
            return Collections.emptyList();
        }
        if (rowList == null) {
            rowList = Collections.emptyList();
        }
        log.info("[TM_BOOTSTRAP_DETAIL] factoryCode={}, scheduleDate={} BOM模式成型计划原始行数={}",
                context.getFactoryCode(), DateUtil.formatDate(context.getScheduleDate()), rowList.size());
        // 校验成型关联施工的关键字段是否为空，收集所有有问题的规格统一提示
        Set<String> treadCodeEmptyList = new HashSet<>();
        Set<String> treadLengthEmptyList = new HashSet<>();
        Set<String> mouthPlateEmptyList = new HashSet<>();
        Set<String> rubberCategoryEmptyList = new HashSet<>();
        for (TmFormingDemandRowVo row : rowList) {
            String orderNo = row.getOrderNo();
            String embryoCode = row.getEmbryoCode();
            String treadCode = row.getTreadCode();
            BigDecimal treadLength = nvl(row.getTreadShoulderLength());
            String mouthPlate = row.getTreadMouthPlate();
            String rubberCategory = row.getTreadRubberCategory();
            if (StrUtil.isBlank(treadCode)) {
                treadCodeEmptyList.add(embryoCode);
            }
            if (treadLength == null || treadLength.compareTo(BigDecimal.ZERO) <= 0) {
                treadLengthEmptyList.add(embryoCode);
            }
            if (StrUtil.isBlank(mouthPlate)) {
                mouthPlateEmptyList.add(embryoCode);
            }
            if (StrUtil.isBlank(rubberCategory)) {
                rubberCategoryEmptyList.add(embryoCode);
            }
        }
        // 统一抛出校验异常
        StringBuilder errorMsg = new StringBuilder();
        if (CollUtil.isNotEmpty(treadCodeEmptyList)) {
            errorMsg.append("成型规格：").append(String.join("、", treadCodeEmptyList)).append("，胎面代码为空；");
        }
        if (CollUtil.isNotEmpty(treadLengthEmptyList)) {
            errorMsg.append("成型规格：").append(String.join("、", treadLengthEmptyList)).append("，胎面长为空；");
        }
        if (CollUtil.isNotEmpty(mouthPlateEmptyList)) {
            errorMsg.append("成型规格：").append(String.join("、", mouthPlateEmptyList)).append("，胎面口型板为空；");
        }
        if (CollUtil.isNotEmpty(rubberCategoryEmptyList)) {
            errorMsg.append("成型规格：").append(String.join("、", rubberCategoryEmptyList)).append("，胎面胶料为空；");
        }
        if (errorMsg.length() > 0) {
            // 移除末尾的分号
            errorMsg.setLength(errorMsg.length() - 1);
            context.getIssueCollector().addIssue(TmAutoScheduleIssueLevelEnum.ERROR,
                    TmScheduleStepEnum.BOOTSTRAP, TmAutoScheduleIssueCategoryEnum.CONSTRUCTION_FIELD_MISSING,
                    errorMsg.toString());
            throw new RuntimeException(errorMsg.toString());
        }
        // 成型来源需求不在数据加载阶段聚合，解释表需要逐条追溯原成型排程结果。
        List<TmFormingDemandRowVo> demandRowList = rowList;
        String algorithmCode = getParamValue(context, TmScheduleConstants.PARAM_ALGORITHM_SWITCH,
                TmScheduleConstants.DEFAULT_ALGORITHM_SWITCH);
        BigDecimal minStartQty = getDecimalParam(context, TmScheduleConstants.PARAM_MIN_START_QTY);
        BigDecimal defaultCurlLength = getDecimalParam(context, TmScheduleConstants.PARAM_DEFAULT_CURL_LENGTH);
        BigDecimal toolTotalQty = getDecimalParam(context, TmScheduleConstants.PARAM_TOOL_TOTAL_QTY);
        Integer fallbackGuardShiftCount = getIntegerParam(context, TmScheduleConstants.PARAM_MIN_STOCK_CLASS,
                TmScheduleConstants.DEFAULT_MIN_STOCK_CLASS_VALUE);
        Integer newSpecLookbackDays = getPositiveIntegerParam(context,
                TmScheduleConstants.PARAM_NEW_SPEC_LOOKBACK_DAYS,
                TmScheduleConstants.DEFAULT_NEW_SPEC_LOOKBACK_DAYS_VALUE);
        Integer newSpecAdvanceShiftCount = getPositiveIntegerParam(context,
                TmScheduleConstants.PARAM_NEW_SPEC_ADVANCE_SHIFT_COUNT,
                TmScheduleConstants.DEFAULT_NEW_SPEC_ADVANCE_SHIFT_COUNT_VALUE);
        Integer formingShiftOffset = getNonNegativeIntegerParam(context,
                TmScheduleConstants.PARAM_FORMING_SHIFT_OFFSET,
                TmScheduleConstants.DEFAULT_FORMING_SHIFT_OFFSET_VALUE);
        Map<String, TmNewSpecInfo> newSpecInfoMap = buildNewSpecInfoMap(context, demandRowList.stream()
                .map(TmFormingDemandRowVo::getTreadCode).filter(StrUtil::isNotBlank)
                .distinct().collect(Collectors.toList()), newSpecLookbackDays, newSpecAdvanceShiftCount);
        List<TmLossRule> lossRuleList = context.getLossRuleList();
        List<TmDepthConfig> depthConfigList = this.loadDepthConfigs(context);
        TmWorkCalendarRowVo tmCalendar = loadWorkCalendar(context, TmProcessCodeEnum.TREAD.getCode());
        TmWorkCalendarRowVo cxCalendar = loadWorkCalendar(context, TmProcessCodeEnum.FORMING.getCode());
        List<TmTaskDraft> taskDraftList = new ArrayList<>();
        int sourceRowIndex = 0;
        for (TmFormingDemandRowVo row : demandRowList) {
            sourceRowIndex++;
            String treadCode = row.getTreadCode();
            BigDecimal treadLength = nvl(row.getTreadShoulderLength());
            if (StrUtil.isBlank(treadCode) || treadLength.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal[] classQtyArray = buildClassQtyArray(row);
            BigDecimal[] classFinishQtyArray = buildClassFinishQtyArray(row);
            Integer guardShiftCount = this.resolveGuardShiftCount(context, row.getLhMachineCode(), row.getOrderNo(),
                    depthConfigList, fallbackGuardShiftCount);
            boolean noShutdownAvailableShift = redistributeShutdownDemand(context, classQtyArray, tmCalendar, cxCalendar);
            for (int shiftOrder = 1; shiftOrder <= TmScheduleConstants.TM_MAX_SHIFT_ORDER; shiftOrder++) {
                BigDecimal formingQty = this.resolveCurrentShiftFormingQty(classQtyArray, shiftOrder, algorithmCode,
                        formingShiftOffset);
                BigDecimal demandQty = formingQty.multiply(treadLength);
                if (demandQty.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                TmNewSpecInfo taskNewSpecInfo = buildTaskNewSpecInfo(newSpecInfoMap.get(treadCode), shiftOrder, demandQty);
                int effectiveGuardShiftCount = this.resolveEffectiveGuardShiftCount(taskNewSpecInfo,
                        guardShiftCount, shiftOrder, formingShiftOffset);
                int targetShiftOrder = resolveTargetShiftOrder(taskNewSpecInfo, shiftOrder);
                TmTaskDraft taskDraft = new TmTaskDraft();
                taskDraft.setOrderNo(row.getOrderNo() + "-CLASS" + shiftOrder);
                taskDraft.setSourceOrderNos(row.getOrderNo());
                taskDraft.setMaterialCode(row.getMaterialCode());
                taskDraft.setMaterialDesc(row.getMaterialDesc());
                taskDraft.setEmbryoCode(row.getEmbryoCode());
                taskDraft.setMainMaterialDesc(row.getMainMaterialDesc());
                taskDraft.setCxMachineCode(row.getCxMachineCode());
                taskDraft.setLhMachineCode(row.getLhMachineCode());
                taskDraft.setBusinessKeySuffix(buildSourceTaskBusinessKeySuffix(row, sourceRowIndex, shiftOrder));
                taskDraft.setTreadCode(treadCode);
                // 拆分胶料类别：第一个值为主胶料编码，其余值为基部胶编码
                String rubberCategory = row.getTreadRubberCategory();
                if (StrUtil.isNotBlank(rubberCategory)) {
                    String[] glueParts = rubberCategory.split(",");
                    taskDraft.setGlueCode(glueParts[0].trim());
                    if (glueParts.length > 1) {
                        // 使用英文逗号拼接剩余部分作为基部胶编码
                        String baseGlueCode = String.join(",", Arrays.copyOfRange(glueParts, 1, glueParts.length));
                        taskDraft.setBaseGlueCode(baseGlueCode);
                    }
                } else {
                    taskDraft.setGlueCode(null);
                    taskDraft.setBaseGlueCode(null);
                }
                taskDraft.setSmallGlueFlag(this.isSmallGlueCode(context, taskDraft.getGlueCode()));
                taskDraft.setMouthPlateCode(row.getTreadMouthPlate());
                taskDraft.setShiftOrder(targetShiftOrder);
                taskDraft.setSourceShiftOrder(shiftOrder);
                taskDraft.setNewSpecInfo(taskNewSpecInfo);
                taskDraft.setTreadShoulderLength(treadLength);
                taskDraft.setTailFlag(this.isCloseOutByPlanSurplus(row.getCxRemainQty(), formingQty)
                        ? TmYesNoEnum.YES.getCode() : TmYesNoEnum.NO.getCode());
                taskDraft.setTailBalanceQty(nvl(row.getCxRemainQty()));
                taskDraft.setCurrentShiftDemandQty(demandQty);
                taskDraft.setCurrentShiftFormingFinishQty(resolveFormingQty(classFinishQtyArray, shiftOrder,
                        algorithmCode, formingShiftOffset).multiply(treadLength));
                BigDecimal rawGuardFormingQty = this.calculateGuardFormingQty(classQtyArray, shiftOrder,
                        effectiveGuardShiftCount, formingShiftOffset);
                BigDecimal cappedGuardFormingQty = this.capGuardFormingQty(rawGuardFormingQty,
                        row.getLhRemainQty());
                taskDraft.setGuardDemandQty(cappedGuardFormingQty.multiply(treadLength));
                taskDraft.setFormingGuardWindowQtyMap(this.buildGuardWindowByBom(classQtyArray, shiftOrder,
                        effectiveGuardShiftCount, formingShiftOffset, treadLength, cappedGuardFormingQty));
                taskDraft.setDemandQty(demandQty);
                taskDraft.setGuardShiftCount(effectiveGuardShiftCount);
                this.addGuardDemandEstimateTrace(context, taskDraft, classQtyArray, shiftOrder,
                        effectiveGuardShiftCount,
                        formingShiftOffset, row.getLhRemainQty(), rawGuardFormingQty, cappedGuardFormingQty, "BOM");
                this.fillGuardRangeHours(context, taskDraft, shiftOrder, effectiveGuardShiftCount, formingShiftOffset);
                taskDraft.setMinStartQty(minStartQty);
                taskDraft.setDefaultCurlRollLength(defaultCurlLength);
                if (toolTotalQty.compareTo(BigDecimal.ZERO) > 0) {
                    taskDraft.setTotalToolQty(toolTotalQty);
                }
                if (noShutdownAvailableShift && !isShiftOpen(tmCalendar, targetShiftOrder) && isShiftOpen(cxCalendar, shiftOrder)) {
                    taskDraft.setUnplannedReasonCode(TmUnplannedReasonEnum.TM_SHUTDOWN_NO_AVAILABLE_SHIFT.getCode());
                    taskDraft.setUnplannedReasonDesc(TmUnplannedReasonEnum.TM_SHUTDOWN_NO_AVAILABLE_SHIFT.getDesc());
                }
                taskDraftList.add(taskDraft);
            }
        }
        appendExperimentSpecTasks(context, taskDraftList, lossRuleList, minStartQty, defaultCurlLength, toolTotalQty);
        log.info("[TM_BOOTSTRAP_DETAIL] factoryCode={}, scheduleDate={} BOM模式任务生成汇总：成型行数={}，生成任务={}",
                context.getFactoryCode(), DateUtil.formatDate(context.getScheduleDate()),
                rowList.size(), taskDraftList.size());
        if (taskDraftList.isEmpty()) {
            this.recordEmptyFormingTaskMessage(context, rowList.size(), formingShiftOffset);
        }
        return taskDraftList;
    }

    /**
     * RECIPE 模式：按 CD90 式逐班示方书版本解析施工，同一成型行不同班次可对应不同胎面规格。
     *
     * <p>分两阶段加载：先查成型排程结果（含 CLASS1~8_RECIPE_NO），再按 (EMBRYO_CODE, CONSTRUCTION_VERSION)
     * 批量查施工胎面属性，Java 中按 (embryoCode, classNRecipeNo) 逐班关联。某班次示方书为空或未命中施工时
     * 记 warn 跳过该班次，不抛异常（CD90 风格）。</p>
     *
     * @param context     自动排程上下文
     * @param machineList 胎面机台列表
     * @return 胎面待排任务列表
     */
    private List<TmTaskDraft> loadFormingDemandTasksByRecipe(TmScheduleContext context, List<TmMachineInfo> machineList) {
        List<TmFormingDemandRecipeRowVo> rowList;
        try {
            rowList = tmAutoScheduleDataLoadMapper.selectFormingDemandRowsByRecipe(
                    context.getFactoryCode(), context.getScheduleDate());
        } catch (RuntimeException ex) {
            log.warn("[TM_AUTO_SCHEDULE_LOAD] RECIPE 模式加载成型计划失败，scheduleDate={}，原因={}",
                    DateUtil.formatDate(context.getScheduleDate()), ex.getMessage());
            return Collections.emptyList();
        }
        if (CollUtil.isEmpty(rowList)) {
            log.warn("[TM_BOOTSTRAP_DETAIL] factoryCode={}, scheduleDate={} RECIPE模式查询成型计划结果为空，无排程任务可生成",
                    context.getFactoryCode(), DateUtil.formatDate(context.getScheduleDate()));
            return Collections.emptyList();
        }
        log.info("[TM_BOOTSTRAP_DETAIL] factoryCode={}, scheduleDate={} RECIPE模式成型计划原始行数={}",
                context.getFactoryCode(), DateUtil.formatDate(context.getScheduleDate()), rowList.size());
        // 收集胎胚代码与所有班次示方书版本，批量加载施工胎面属性
        Set<String> embryoCodes = rowList.stream().map(TmFormingDemandRecipeRowVo::getEmbryoCode)
                .filter(StrUtil::isNotBlank).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> recipeVersions = new LinkedHashSet<>();
        for (TmFormingDemandRecipeRowVo row : rowList) {
            for (String recipeNo : buildRecipeNoArray(row)) {
                if (StrUtil.isNotBlank(recipeNo)) {
                    recipeVersions.add(recipeNo.trim());
                }
            }
        }
        List<TmConstructionTreadRowVo> constructionList;
        if (embryoCodes.isEmpty() || recipeVersions.isEmpty()) {
            log.warn("[TM_RECIPE_MATCH] factoryCode={}, scheduleDate={} 成型计划未提供胎胚代码或示方书版本，跳过施工解析",
                    context.getFactoryCode(), DateUtil.formatDate(context.getScheduleDate()));
            constructionList = Collections.emptyList();
        } else {
            constructionList = tmAutoScheduleDataLoadMapper.selectConstructionInfoRows(
                    context.getFactoryCode(), embryoCodes, recipeVersions);
        }
        Map<String, TmConstructionTreadRowVo> constructionMap = new HashMap<>();
        for (TmConstructionTreadRowVo construction : nullToEmpty(constructionList)) {
            if (construction == null || StrUtil.isBlank(construction.getConstructionCode())
                    || StrUtil.isBlank(construction.getConstructionVersion())) {
                continue;
            }
            constructionMap.putIfAbsent(construction.getConstructionCode() + "|" + construction.getConstructionVersion(),
                    construction);
        }
        log.info("[TM_BOOTSTRAP_DETAIL] factoryCode={}, scheduleDate={} RECIPE模式施工胎面属性：胚编码数={}，示方书版本数={}，命中施工行数={}，构造映射数={}",
                context.getFactoryCode(), DateUtil.formatDate(context.getScheduleDate()),
                embryoCodes.size(), recipeVersions.size(),
                nullToEmpty(constructionList).size(), constructionMap.size());
        // 参数与基础数据
        String algorithmCode = getParamValue(context, TmScheduleConstants.PARAM_ALGORITHM_SWITCH,
                TmScheduleConstants.DEFAULT_ALGORITHM_SWITCH);
        BigDecimal minStartQty = getDecimalParam(context, TmScheduleConstants.PARAM_MIN_START_QTY);
        BigDecimal defaultCurlLength = getDecimalParam(context, TmScheduleConstants.PARAM_DEFAULT_CURL_LENGTH);
        BigDecimal toolTotalQty = getDecimalParam(context, TmScheduleConstants.PARAM_TOOL_TOTAL_QTY);
        Integer fallbackGuardShiftCount = getIntegerParam(context, TmScheduleConstants.PARAM_MIN_STOCK_CLASS,
                TmScheduleConstants.DEFAULT_MIN_STOCK_CLASS_VALUE);
        Integer newSpecLookbackDays = getPositiveIntegerParam(context,
                TmScheduleConstants.PARAM_NEW_SPEC_LOOKBACK_DAYS,
                TmScheduleConstants.DEFAULT_NEW_SPEC_LOOKBACK_DAYS_VALUE);
        Integer newSpecAdvanceShiftCount = getPositiveIntegerParam(context,
                TmScheduleConstants.PARAM_NEW_SPEC_ADVANCE_SHIFT_COUNT,
                TmScheduleConstants.DEFAULT_NEW_SPEC_ADVANCE_SHIFT_COUNT_VALUE);
        Integer formingShiftOffset = getNonNegativeIntegerParam(context,
                TmScheduleConstants.PARAM_FORMING_SHIFT_OFFSET,
                TmScheduleConstants.DEFAULT_FORMING_SHIFT_OFFSET_VALUE);
        List<TmLossRule> lossRuleList = context.getLossRuleList();
        List<TmDepthConfig> depthConfigList = this.loadDepthConfigs(context);
        TmWorkCalendarRowVo tmCalendar = loadWorkCalendar(context, TmProcessCodeEnum.TREAD.getCode());
        TmWorkCalendarRowVo cxCalendar = loadWorkCalendar(context, TmProcessCodeEnum.FORMING.getCode());

        // 预解析每行各班次施工规格，并收集有效胎面编码用于新规格判断
        List<BigDecimal[]> classQtyArrayList = new ArrayList<>();
        List<BigDecimal[]> classFinishQtyArrayList = new ArrayList<>();
        List<TmConstructionTreadRowVo[]> specByClassList = new ArrayList<>();
        Set<String> allTreadCodes = new LinkedHashSet<>();
        Set<String> treadCodeEmptyList = new LinkedHashSet<>();
        Set<String> treadLengthEmptyList = new LinkedHashSet<>();
        Set<String> mouthPlateEmptyList = new LinkedHashSet<>();
        Set<String> rubberCategoryEmptyList = new LinkedHashSet<>();
        for (TmFormingDemandRecipeRowVo row : rowList) {
            BigDecimal[] classQtyArray = buildClassQtyArrayByRecipe(row);
            classQtyArrayList.add(classQtyArray);
            classFinishQtyArrayList.add(buildClassFinishQtyArrayByRecipe(row));
            String[] recipeNoByClass = buildRecipeNoArray(row);
            TmConstructionTreadRowVo[] specByClass = new TmConstructionTreadRowVo[8];
            for (int i = 0; i < 8; i++) {
                String recipeNo = recipeNoByClass[i];
                if (StrUtil.isBlank(recipeNo)) {
                    continue;
                }
                TmConstructionTreadRowVo spec = constructionMap.get(row.getEmbryoCode() + "|" + recipeNo.trim());
                specByClass[i] = spec;
                if (spec == null || readClassQty(classQtyArray, i).compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                String sourceKey = StrUtil.blankToDefault(row.getEmbryoCode(), row.getOrderNo()) + "/" + recipeNo.trim();
                if (StrUtil.isBlank(spec.getTreadCode())) {
                    treadCodeEmptyList.add(sourceKey);
                }
                if (nvl(spec.getTreadShoulderLength()).compareTo(BigDecimal.ZERO) <= 0) {
                    treadLengthEmptyList.add(sourceKey);
                }
                if (StrUtil.isBlank(spec.getTreadMouthPlate())) {
                    mouthPlateEmptyList.add(sourceKey);
                }
                if (StrUtil.isBlank(spec.getTreadRubberCategory())) {
                    rubberCategoryEmptyList.add(sourceKey);
                }
                if (StrUtil.isNotBlank(spec.getTreadCode())
                        && nvl(spec.getTreadShoulderLength()).compareTo(BigDecimal.ZERO) > 0) {
                    allTreadCodes.add(spec.getTreadCode());
                }
            }
            specByClassList.add(specByClass);
        }
        StringBuilder errorMsg = new StringBuilder();
        if (CollUtil.isNotEmpty(treadCodeEmptyList)) {
            errorMsg.append("成型规格/示方书：").append(String.join("、", treadCodeEmptyList)).append("，胎面代码为空；");
        }
        if (CollUtil.isNotEmpty(treadLengthEmptyList)) {
            errorMsg.append("成型规格/示方书：").append(String.join("、", treadLengthEmptyList)).append("，胎面长为空；");
        }
        if (CollUtil.isNotEmpty(mouthPlateEmptyList)) {
            errorMsg.append("成型规格/示方书：").append(String.join("、", mouthPlateEmptyList)).append("，胎面口型板为空；");
        }
        if (CollUtil.isNotEmpty(rubberCategoryEmptyList)) {
            errorMsg.append("成型规格/示方书：").append(String.join("、", rubberCategoryEmptyList)).append("，胎面胶料为空；");
        }
        if (errorMsg.length() > 0) {
            errorMsg.setLength(errorMsg.length() - 1);
            context.getIssueCollector().addIssue(TmAutoScheduleIssueLevelEnum.ERROR,
                    TmScheduleStepEnum.BOOTSTRAP, TmAutoScheduleIssueCategoryEnum.CONSTRUCTION_FIELD_MISSING,
                    errorMsg.toString());
            throw new RuntimeException(errorMsg.toString());
        }
        Map<String, TmNewSpecInfo> newSpecInfoMap = buildNewSpecInfoMap(context, allTreadCodes,
                newSpecLookbackDays, newSpecAdvanceShiftCount);

        // 逐行逐班次生成任务
        List<TmTaskDraft> taskDraftList = new ArrayList<>();
        int sourceRowIndex = 0;
        int skippedShiftNoFormingQty = 0;
        int skippedShiftNoSpec = 0;
        int skippedShiftNoDemand = 0;
        for (int rowIdx = 0; rowIdx < rowList.size(); rowIdx++) {
            TmFormingDemandRecipeRowVo row = rowList.get(rowIdx);
            sourceRowIndex++;
            BigDecimal[] classQtyArray = classQtyArrayList.get(rowIdx);
            BigDecimal[] classFinishQtyArray = classFinishQtyArrayList.get(rowIdx);
            TmConstructionTreadRowVo[] specByClass = specByClassList.get(rowIdx);
            this.recordAllPlannedShiftConstructionMissingIssue(context, row, classQtyArray, specByClass);
            Integer guardShiftCount = this.resolveGuardShiftCount(context, row.getLhMachineCode(), row.getOrderNo(),
                    depthConfigList, fallbackGuardShiftCount);
            boolean noShutdownAvailableShift = redistributeShutdownDemand(context, classQtyArray, tmCalendar, cxCalendar);
            for (int shiftOrder = 1; shiftOrder <= TmScheduleConstants.TM_MAX_SHIFT_ORDER; shiftOrder++) {
                BigDecimal formingQty = this.resolveCurrentShiftFormingQty(classQtyArray, shiftOrder, algorithmCode,
                        formingShiftOffset);
                if (formingQty.compareTo(BigDecimal.ZERO) <= 0) {
                    skippedShiftNoFormingQty++;
                    continue;
                }
                int startIndex = resolveFormingStartIndex(shiftOrder, formingShiftOffset);
                int primarySpecIndex = Math.min(startIndex, 7);
                TmConstructionTreadRowVo primarySpec = (primarySpecIndex >= 0 && primarySpecIndex < 8)
                        ? specByClass[primarySpecIndex] : null;
                if (primarySpec == null || StrUtil.isBlank(primarySpec.getTreadCode())
                        || nvl(primarySpec.getTreadShoulderLength()).compareTo(BigDecimal.ZERO) <= 0) {
                    String missingReason = primarySpec == null ? "示方书为空或未命中施工" : "施工胎面编码或肩长无效";
                    log.warn("[TM_RECIPE_MATCH] 跳过班次：factoryCode={}, orderNo={}, embryoCode={}, shiftOrder={}, startIndex={}, formingQty={}, 原因={}",
                            context.getFactoryCode(), row.getOrderNo(), row.getEmbryoCode(), shiftOrder, startIndex, formingQty,
                            missingReason);
                    skippedShiftNoSpec++;
                    continue;
                }
                String treadCode = primarySpec.getTreadCode();
                BigDecimal treadLength = nvl(primarySpec.getTreadShoulderLength());
                BigDecimal demandQty = formingQty.multiply(treadLength);
                if (demandQty.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                TmNewSpecInfo taskNewSpecInfo = buildTaskNewSpecInfo(newSpecInfoMap.get(treadCode), shiftOrder, demandQty);
                int effectiveGuardShiftCount = this.resolveEffectiveGuardShiftCount(taskNewSpecInfo,
                        guardShiftCount, shiftOrder, formingShiftOffset);
                int targetShiftOrder = resolveTargetShiftOrder(taskNewSpecInfo, shiftOrder);
                TmTaskDraft taskDraft = new TmTaskDraft();
                taskDraft.setOrderNo(row.getOrderNo() + "-CLASS" + shiftOrder);
                taskDraft.setSourceOrderNos(row.getOrderNo());
                taskDraft.setMaterialCode(row.getMaterialCode());
                taskDraft.setMaterialDesc(row.getMaterialDesc());
                taskDraft.setEmbryoCode(row.getEmbryoCode());
                taskDraft.setMainMaterialDesc(row.getMainMaterialDesc());
                taskDraft.setCxMachineCode(row.getCxMachineCode());
                taskDraft.setLhMachineCode(row.getLhMachineCode());
                taskDraft.setBusinessKeySuffix(buildSourceTaskBusinessKeySuffix(row, sourceRowIndex, shiftOrder));
                taskDraft.setTreadCode(treadCode);
                // 拆分胶料类别：第一个值为主胶料编码，其余值为基部胶编码
                String rubberCategory = primarySpec.getTreadRubberCategory();
                if (StrUtil.isNotBlank(rubberCategory)) {
                    String[] glueParts = rubberCategory.split(",");
                    taskDraft.setGlueCode(glueParts[0].trim());
                    if (glueParts.length > 1) {
                        String baseGlueCode = String.join(",", Arrays.copyOfRange(glueParts, 1, glueParts.length));
                        taskDraft.setBaseGlueCode(baseGlueCode);
                    }
                } else {
                    taskDraft.setGlueCode(null);
                    taskDraft.setBaseGlueCode(null);
                }
                taskDraft.setSmallGlueFlag(this.isSmallGlueCode(context, taskDraft.getGlueCode()));
                taskDraft.setMouthPlateCode(primarySpec.getTreadMouthPlate());
                taskDraft.setShiftOrder(targetShiftOrder);
                taskDraft.setSourceShiftOrder(shiftOrder);
                taskDraft.setNewSpecInfo(taskNewSpecInfo);
                taskDraft.setTreadShoulderLength(treadLength);
                taskDraft.setTailFlag(this.isCloseOutByPlanSurplus(row.getCxRemainQty(), formingQty)
                        ? TmYesNoEnum.YES.getCode() : TmYesNoEnum.NO.getCode());
                taskDraft.setTailBalanceQty(nvl(row.getCxRemainQty()));
                taskDraft.setCurrentShiftDemandQty(demandQty);
                taskDraft.setCurrentShiftFormingFinishQty(resolveFormingQty(classFinishQtyArray, shiftOrder,
                        algorithmCode, formingShiftOffset).multiply(treadLength));
                BigDecimal rawGuardFormingQty = this.calculateGuardFormingQty(classQtyArray, shiftOrder,
                        effectiveGuardShiftCount, formingShiftOffset);
                BigDecimal cappedGuardFormingQty = this.capGuardFormingQty(rawGuardFormingQty,
                        row.getLhRemainQty());
                Map<Integer, BigDecimal> formingGuardWindowQtyMap = this.buildGuardWindowByRecipe(classQtyArray,
                        specByClass, shiftOrder, effectiveGuardShiftCount, formingShiftOffset, treadLength,
                        cappedGuardFormingQty);
                taskDraft.setFormingGuardWindowQtyMap(formingGuardWindowQtyMap);
                taskDraft.setGuardDemandQty(formingGuardWindowQtyMap.values().stream()
                        .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
                taskDraft.setDemandQty(demandQty);
                taskDraft.setGuardShiftCount(effectiveGuardShiftCount);
                this.addGuardDemandEstimateTrace(context, taskDraft, classQtyArray, shiftOrder,
                        effectiveGuardShiftCount,
                        formingShiftOffset, row.getLhRemainQty(), rawGuardFormingQty, cappedGuardFormingQty, "RECIPE");
                this.fillGuardRangeHours(context, taskDraft, shiftOrder, effectiveGuardShiftCount, formingShiftOffset);
                taskDraft.setMinStartQty(minStartQty);
                taskDraft.setDefaultCurlRollLength(defaultCurlLength);
                if (toolTotalQty.compareTo(BigDecimal.ZERO) > 0) {
                    taskDraft.setTotalToolQty(toolTotalQty);
                }
                if (noShutdownAvailableShift && !isShiftOpen(tmCalendar, targetShiftOrder) && isShiftOpen(cxCalendar, shiftOrder)) {
                    taskDraft.setUnplannedReasonCode(TmUnplannedReasonEnum.TM_SHUTDOWN_NO_AVAILABLE_SHIFT.getCode());
                    taskDraft.setUnplannedReasonDesc(TmUnplannedReasonEnum.TM_SHUTDOWN_NO_AVAILABLE_SHIFT.getDesc());
                }
                taskDraftList.add(taskDraft);
            }
        }
        appendExperimentSpecTasks(context, taskDraftList, lossRuleList, minStartQty, defaultCurlLength, toolTotalQty);
        log.info("[TM_BOOTSTRAP_DETAIL] factoryCode={}, scheduleDate={} RECIPE模式任务生成汇总：成型行数={}，跳过(成型量=0)={}班次，跳过(示方书/施工不匹配)={}班次，生成任务={}",
                context.getFactoryCode(), DateUtil.formatDate(context.getScheduleDate()),
                rowList.size(), skippedShiftNoFormingQty, skippedShiftNoSpec, taskDraftList.size());
        if (taskDraftList.isEmpty()) {
            this.recordEmptyFormingTaskMessage(context, rowList.size(), formingShiftOffset);
        }
        return taskDraftList;
    }

    /**
     * 当成型工单所有有计划量班次均未命中施工时，记录一条工单级排程异常。
     *
     * <p>仅统计 CLASS1~CLASS8 中计划量大于 0 的班次。无计划量班次的示方书为空不提示；
     * 只要任一有计划量班次命中施工，也不展示部分班次未命中的异常。</p>
     *
     * @param context       自动排程上下文
     * @param row           成型工单行
     * @param classQtyArray CLASS1~CLASS8 计划量
     * @param specByClass   CLASS1~CLASS8 施工匹配结果
     */
    private void recordAllPlannedShiftConstructionMissingIssue(TmScheduleContext context,
                                                                 TmFormingDemandRecipeRowVo row,
                                                                 BigDecimal[] classQtyArray,
                                                                 TmConstructionTreadRowVo[] specByClass) {
        List<Integer> plannedShiftOrders = java.util.stream.IntStream.range(0, classQtyArray.length)
                .filter(index -> readClassQty(classQtyArray, index).compareTo(BigDecimal.ZERO) > 0)
                .mapToObj(index -> index + 1)
                .collect(Collectors.toList());
        if (plannedShiftOrders.isEmpty()) {
            return;
        }
        boolean constructionMatched = plannedShiftOrders.stream()
                .map(shiftOrder -> specByClass[shiftOrder - 1])
                .anyMatch(Objects::nonNull);
        if (constructionMatched) {
            return;
        }
        String[] recipeNoByClass = buildRecipeNoArray(row);
        List<String> recipeNoList = plannedShiftOrders.stream()
                .map(shiftOrder -> recipeNoByClass[shiftOrder - 1])
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
        String shiftOrders = plannedShiftOrders.stream().map(String::valueOf).collect(Collectors.joining(","));
        String recipeNos = CollUtil.isEmpty(recipeNoList) ? "-" : String.join(",", recipeNoList);
        String message = MessageFormat.format(
                I18nUtil.getMessage("ui.data.alert.tm.schedule.allPlannedShiftsConstructionMissing"),
                row.getOrderNo(), row.getEmbryoCode(), shiftOrders, recipeNos);
        context.getIssueCollector().addConstructionIssue(TmAutoScheduleIssueLevelEnum.WARN,
                TmAutoScheduleIssueCategoryEnum.CONSTRUCTION_MISSING,
                row.getOrderNo(), row.getEmbryoCode(), null, null, "recipeNo", message);
        log.warn("[TM_RECIPE_MATCH] 工单有计划量班次全部未命中施工：factoryCode={}, orderNo={}, embryoCode={}, plannedShiftOrders={}, recipeNos={}",
                context.getFactoryCode(), row.getOrderNo(), row.getEmbryoCode(), shiftOrders, recipeNos);
    }

    /**
     * RECIPE 模式库存保证范围内的成型需求米数：按各班次实际单耗加权求和。
     *
     * <p>窗口内 CLASS1~8 乘以各班次示方书命中的胎面肩长；超过 CLASS8 的估算量使用当前任务肩长。
     * 按硫化余量封顶后的条数依班次顺序消耗，确保不同施工长度下仍先按条数封顶。</p>
     *
     * @param classQtyArray   成型班次计划量数组
     * @param specByClass     各班次命中的施工胎面属性，未命中为 null
     * @param shiftOrder      胎面排程班次，从 1 开始
     * @param guardShiftCount 库存最低保证班数
     * @param formingShiftOffset 胎面班次到成型班次的偏移量
     * @param currentTreadLength 当前任务胎面肩长，供超过 CLASS8 的班次换算
     * @param guardFormingQtyLimit 按硫化余量封顶后的保证需求条数
     * @return 库存保证范围内的成型需求米数合计
     */
    private BigDecimal calculateGuardDemandByRecipe(BigDecimal[] classQtyArray, TmConstructionTreadRowVo[] specByClass,
                                                    int shiftOrder, int guardShiftCount, int formingShiftOffset,
                                                    BigDecimal currentTreadLength,
                                                    BigDecimal guardFormingQtyLimit) {
        return this.buildGuardWindowByRecipe(classQtyArray, specByClass, shiftOrder, guardShiftCount,
                formingShiftOffset, currentTreadLength, guardFormingQtyLimit).values().stream()
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 构建 BOM 模式成型备库窗口明细，按成型班次顺序执行硫化余量封顶。
     *
     * @param classQtyArray 成型班次计划条数
     * @param shiftOrder 胎面排程班次
     * @param guardShiftCount 备库班数
     * @param formingShiftOffset 成型班次偏移
     * @param treadLength 当前胎面长度
     * @param guardFormingQtyLimit 封顶后的保证条数
     * @return 班次到换算后长度的窗口明细
     */
    private Map<Integer, BigDecimal> buildGuardWindowByBom(BigDecimal[] classQtyArray, int shiftOrder,
                                                            int guardShiftCount, int formingShiftOffset,
                                                            BigDecimal treadLength,
                                                            BigDecimal guardFormingQtyLimit) {
        Map<Integer, BigDecimal> windowQtyMap = new LinkedHashMap<>();
        BigDecimal remainingGuardFormingQty = this.nvl(guardFormingQtyLimit).max(BigDecimal.ZERO);
        int startIndex = this.resolveFormingStartIndex(shiftOrder, formingShiftOffset);
        int count = Math.max(guardShiftCount, 1);
        for (int index = startIndex; index < startIndex + count; index++) {
            if (remainingGuardFormingQty.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal formingQty = this.resolveGuardClassQty(classQtyArray, index);
            BigDecimal appliedFormingQty = formingQty.min(remainingGuardFormingQty);
            if (appliedFormingQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            windowQtyMap.put(index + 1, appliedFormingQty.multiply(this.nvl(treadLength)));
            remainingGuardFormingQty = remainingGuardFormingQty.subtract(appliedFormingQty);
        }
        return windowQtyMap;
    }

    /**
     * 构建 RECIPE 模式成型备库窗口明细，按班次施工长度并按硫化余量顺序封顶。
     *
     * @param classQtyArray 成型班次计划条数
     * @param specByClass 各成型班次施工属性
     * @param shiftOrder 胎面排程班次
     * @param guardShiftCount 备库班数
     * @param formingShiftOffset 成型班次偏移
     * @param currentTreadLength 当前胎面长度
     * @param guardFormingQtyLimit 封顶后的保证条数
     * @return 班次到换算后长度的窗口明细
     */
    private Map<Integer, BigDecimal> buildGuardWindowByRecipe(BigDecimal[] classQtyArray,
                                                                TmConstructionTreadRowVo[] specByClass,
                                                                int shiftOrder, int guardShiftCount,
                                                                int formingShiftOffset, BigDecimal currentTreadLength,
                                                                BigDecimal guardFormingQtyLimit) {
        Map<Integer, BigDecimal> windowQtyMap = new LinkedHashMap<>();
        BigDecimal remainingGuardFormingQty = this.nvl(guardFormingQtyLimit).max(BigDecimal.ZERO);
        int startIndex = this.resolveFormingStartIndex(shiftOrder, formingShiftOffset);
        int count = Math.max(guardShiftCount, 1);
        for (int index = startIndex; index < startIndex + count; index++) {
            if (remainingGuardFormingQty.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal formingQty = this.resolveGuardClassQty(classQtyArray, index);
            BigDecimal appliedFormingQty = formingQty.min(remainingGuardFormingQty);
            if (appliedFormingQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal treadLength = (index >= 0 && index < 8 && specByClass != null && specByClass[index] != null)
                    ? this.nvl(specByClass[index].getTreadShoulderLength()) : this.nvl(currentTreadLength);
            windowQtyMap.put(index + 1, appliedFormingQty.multiply(treadLength));
            remainingGuardFormingQty = remainingGuardFormingQty.subtract(appliedFormingQty);
        }
        return windowQtyMap;
    }

    /**
     * 构建成型班次计划量数组（RECIPE 模式）。
     *
     * @param row 成型需求行（示方书版本模式）
     * @return 1~8 班成型计划量数组
     */
    private BigDecimal[] buildClassQtyArrayByRecipe(TmFormingDemandRecipeRowVo row) {
        return new BigDecimal[]{
                nvl(row.getClass1PlanQty()),
                nvl(row.getClass2PlanQty()),
                nvl(row.getClass3PlanQty()),
                nvl(row.getClass4PlanQty()),
                nvl(row.getClass5PlanQty()),
                nvl(row.getClass6PlanQty()),
                nvl(row.getClass7PlanQty()),
                nvl(row.getClass8PlanQty())
        };
    }

    /**
     * 构建成型班次完成量数组（RECIPE 模式）。
     *
     * @param row 成型需求行
     * @return 一至八班成型完成量数组，空值按零处理
     */
    private BigDecimal[] buildClassFinishQtyArrayByRecipe(TmFormingDemandRecipeRowVo row) {
        return new BigDecimal[]{
                nvl(row.getClass1FinishQty()),
                nvl(row.getClass2FinishQty()),
                nvl(row.getClass3FinishQty()),
                nvl(row.getClass4FinishQty()),
                nvl(row.getClass5FinishQty()),
                nvl(row.getClass6FinishQty()),
                nvl(row.getClass7FinishQty()),
                nvl(row.getClass8FinishQty())
        };
    }

    /**
     * 构建成型班次示方书编号数组（RECIPE 模式）。
     *
     * @param row 成型需求行（示方书版本模式）
     * @return 1~8 班示方书编号数组
     */
    private String[] buildRecipeNoArray(TmFormingDemandRecipeRowVo row) {
        return new String[]{
                row.getClass1RecipeNo(),
                row.getClass2RecipeNo(),
                row.getClass3RecipeNo(),
                row.getClass4RecipeNo(),
                row.getClass5RecipeNo(),
                row.getClass6RecipeNo(),
                row.getClass7RecipeNo(),
                row.getClass8RecipeNo()
        };
    }

    /**
     * 追加月计划定稿实验规格胎面任务。
     *
     * @param context 自动排程上下文
     * @param taskDraftList 待排任务列表
     * @param lossRuleList 损耗配置列表
     * @param minStartQty 最小起排量
     * @param defaultCurlLength 默认卷曲长度
     * @param toolTotalQty 总工装数量
     */
    private void appendExperimentSpecTasks(TmScheduleContext context, List<TmTaskDraft> taskDraftList,
                                           List<TmLossRule> lossRuleList, BigDecimal minStartQty,
                                           BigDecimal defaultCurlLength, BigDecimal toolTotalQty) {
        Integer lookbackDays = getPositiveIntegerParam(context,
                TmScheduleConstants.PARAM_EXPERIMENT_SPEC_LOOKBACK_DAYS,
                TmScheduleConstants.DEFAULT_EXPERIMENT_SPEC_LOOKBACK_DAYS_VALUE);
        BigDecimal experimentPlanQty = getPositiveDecimalParam(context,
                TmScheduleConstants.PARAM_EXPERIMENT_SPEC_PLAN_QTY,
                BigDecimal.valueOf(TmScheduleConstants.DEFAULT_EXPERIMENT_SPEC_PLAN_QTY_VALUE));
        if (experimentPlanQty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        Date experimentPlanDate = DateUtil.offsetDay(context.getScheduleDate(), -lookbackDays);
        String dayColumn = buildExperimentDayColumn(experimentPlanDate);
        Integer yearMonth = Integer.valueOf(DateUtil.format(experimentPlanDate, "yyyyMM"));
        List<TmExperimentSpecMonthPlanRowVo> rowList;
        try {
            rowList = tmAutoScheduleDataLoadMapper.selectExperimentSpecMonthPlanRows(context.getFactoryCode(), yearMonth,
                    dayColumn, experimentPlanDate, TmConstructionStageEnum.EXPERIMENT.getCode());
        } catch (RuntimeException ex) {
            log.warn("[TM_AUTO_SCHEDULE_LOAD] 加载实验规格月计划失败，scheduleDate={}，experimentPlanDate={}，原因={}",
                    DateUtil.formatDate(context.getScheduleDate()), DateUtil.formatDate(experimentPlanDate), ex.getMessage());
            return;
        }
        List<TmExperimentSpecMonthPlanRowVo> validRows = filterExperimentSpecRows(rowList);
        if (CollUtil.isEmpty(validRows)) {
            return;
        }
        validateExperimentSpecRows(validRows);
        Map<String, List<TmExperimentSpecMonthPlanRowVo>> rowMap = validRows.stream()
                .collect(Collectors.groupingBy(TmExperimentSpecMonthPlanRowVo::getTreadCode, LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<String, List<TmExperimentSpecMonthPlanRowVo>> entry : rowMap.entrySet()) {
            List<TmExperimentSpecMonthPlanRowVo> treadRows = entry.getValue();
            TmExperimentSpecInfo experimentSpecInfo = buildExperimentSpecInfo(context, treadRows, lookbackDays,
                    experimentPlanQty, experimentPlanDate);
            TmTaskDraft existingTask = findExperimentMergeTarget(taskDraftList, entry.getKey());
            if (existingTask != null) {
                mergeExperimentSpecTask(existingTask, experimentPlanQty, experimentSpecInfo);
                continue;
            }
            taskDraftList.add(buildExperimentSpecTask(context, treadRows.get(0), experimentPlanQty, experimentSpecInfo,
                    lossRuleList, minStartQty, defaultCurlLength, toolTotalQty));
        }
    }

    /**
     * 过滤实验规格月计划有效行。
     *
     * @param rowList 月计划查询行
     * @return 施工阶段为实验且当天数量大于 0 的行
     */
    private List<TmExperimentSpecMonthPlanRowVo> filterExperimentSpecRows(List<TmExperimentSpecMonthPlanRowVo> rowList) {
        if (CollUtil.isEmpty(rowList)) {
            return Collections.emptyList();
        }
        return rowList.stream()
                .filter(row -> row != null
                        && TmConstructionStageEnum.EXPERIMENT.getCode().equals(row.getConstructionStage()))
                .filter(row -> nvl(row.getDayQty()).compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
    }

    /**
     * 校验实验规格映射后的胎面基础字段。
     *
     * @param rowList 实验规格月计划行
     * @throws RuntimeException 胎面编码、胎面长、口型板或胶料缺失时抛出
     */
    private void validateExperimentSpecRows(List<TmExperimentSpecMonthPlanRowVo> rowList) {
        Set<String> treadCodeEmptyList = new HashSet<>();
        Set<String> treadLengthEmptyList = new HashSet<>();
        Set<String> mouthPlateEmptyList = new HashSet<>();
        Set<String> rubberCategoryEmptyList = new HashSet<>();
        for (TmExperimentSpecMonthPlanRowVo row : rowList) {
            String embryoCode = row.getEmbryoCode();
            if (StrUtil.isBlank(row.getTreadCode())) {
                treadCodeEmptyList.add(embryoCode);
            }
            if (nvl(row.getTreadShoulderLength()).compareTo(BigDecimal.ZERO) <= 0) {
                treadLengthEmptyList.add(embryoCode);
            }
            if (StrUtil.isBlank(row.getTreadMouthPlate())) {
                mouthPlateEmptyList.add(embryoCode);
            }
            if (StrUtil.isBlank(row.getTreadRubberCategory())) {
                rubberCategoryEmptyList.add(embryoCode);
            }
        }
        StringBuilder errorMsg = new StringBuilder();
        if (CollUtil.isNotEmpty(treadCodeEmptyList)) {
            errorMsg.append("实验规格月计划：").append(String.join("、", treadCodeEmptyList)).append("，胎面代码为空；");
        }
        if (CollUtil.isNotEmpty(treadLengthEmptyList)) {
            errorMsg.append("实验规格月计划：").append(String.join("、", treadLengthEmptyList)).append("，胎面长为空；");
        }
        if (CollUtil.isNotEmpty(mouthPlateEmptyList)) {
            errorMsg.append("实验规格月计划：").append(String.join("、", mouthPlateEmptyList)).append("，胎面口型板为空；");
        }
        if (CollUtil.isNotEmpty(rubberCategoryEmptyList)) {
            errorMsg.append("实验规格月计划：").append(String.join("、", rubberCategoryEmptyList)).append("，胎面胶料为空；");
        }
        if (errorMsg.length() > 0) {
            errorMsg.setLength(errorMsg.length() - 1);
            throw new RuntimeException(errorMsg.toString());
        }
    }

    /**
     * 构建实验规格证据对象。
     *
     * @param context 自动排程上下文
     * @param rowList 同胎面月计划行
     * @param lookbackDays 回看天数
     * @param experimentPlanQty 固定实验计划量
     * @param experimentPlanDate 月计划定稿生产日期
     * @return 实验规格证据对象
     */
    private TmExperimentSpecInfo buildExperimentSpecInfo(TmScheduleContext context,
                                                         List<TmExperimentSpecMonthPlanRowVo> rowList,
                                                         Integer lookbackDays,
                                                         BigDecimal experimentPlanQty,
                                                         Date experimentPlanDate) {
        TmExperimentSpecInfo info = new TmExperimentSpecInfo();
        info.setExperimentSpec(Boolean.TRUE);
        info.setLookbackDays(lookbackDays);
        info.setLookbackDaysSource(getPositiveIntegerParamSource(context,
                TmScheduleConstants.PARAM_EXPERIMENT_SPEC_LOOKBACK_DAYS,
                TmScheduleConstants.DEFAULT_EXPERIMENT_SPEC_LOOKBACK_DAYS_VALUE));
        info.setPlanQty(experimentPlanQty);
        info.setPlanQtySource(getPositiveDecimalParamSource(context,
                TmScheduleConstants.PARAM_EXPERIMENT_SPEC_PLAN_QTY));
        info.setScheduleDate(context.getScheduleDate());
        info.setExperimentPlanDate(experimentPlanDate);
        info.setMonthPlanDayQty(rowList.stream().map(TmExperimentSpecMonthPlanRowVo::getDayQty)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
        info.setMonthPlanIds(rowList.stream().map(TmExperimentSpecMonthPlanRowVo::getMonthPlanId)
                .filter(Objects::nonNull).collect(Collectors.toList()));
        info.setProductionNos(rowList.stream().map(TmExperimentSpecMonthPlanRowVo::getProductionNo)
                .filter(StrUtil::isNotBlank).collect(Collectors.toList()));
        info.setEmbryoCodes(rowList.stream().map(TmExperimentSpecMonthPlanRowVo::getEmbryoCode)
                .filter(StrUtil::isNotBlank).collect(Collectors.toList()));
        info.setReason("月计划定稿命中实验规格，按固定米数生成胎面实验计划");
        return info;
    }

    /**
     * 查找同胎面一班叠加目标任务。
     *
     * @param taskDraftList 待排任务列表
     * @param treadCode 胎面编码
     * @return 同胎面一班任务；不存在时返回 null
     */
    private TmTaskDraft findExperimentMergeTarget(List<TmTaskDraft> taskDraftList, String treadCode) {
        if (CollUtil.isEmpty(taskDraftList) || StrUtil.isBlank(treadCode)) {
            return null;
        }
        return taskDraftList.stream()
                .filter(task -> treadCode.equals(task.getTreadCode()))
                .filter(task -> Integer.valueOf(TmScheduleConstants.EXPERIMENT_SPEC_SHIFT_ORDER)
                        .equals(task.getShiftOrder()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 将实验规格固定计划量叠加到已有任务。
     *
     * @param task 目标任务
     * @param experimentPlanQty 实验固定计划量
     * @param experimentSpecInfo 实验规格证据
     */
    private void mergeExperimentSpecTask(TmTaskDraft task, BigDecimal experimentPlanQty,
                                         TmExperimentSpecInfo experimentSpecInfo) {
        task.setCurrentShiftDemandQty(nvl(task.getCurrentShiftDemandQty()).add(experimentPlanQty));
        task.setGuardDemandQty(nvl(task.getGuardDemandQty()).add(experimentPlanQty));
        task.setDemandQty(nvl(task.getDemandQty()).add(experimentPlanQty));
        if (task.getPlanQty() != null) {
            task.setPlanQty(task.getPlanQty().add(experimentPlanQty));
        }
        task.setExperimentSpecInfo(experimentSpecInfo);
        experimentSpecInfo.setMergedToExistingTask(Boolean.TRUE);
        task.setSourceOrderNos(appendSourceOrderNos(task.getSourceOrderNos(), experimentSpecInfo.getProductionNos()));
    }

    /**
     * 构建实验规格独立任务。
     *
     * @param row 月计划实验规格行
     * @param experimentPlanQty 实验固定计划量
     * @param experimentSpecInfo 实验规格证据
     * @param lossRuleList 损耗配置列表
     * @param minStartQty 最小起排量
     * @param defaultCurlLength 默认卷曲长度
     * @return 实验规格独立任务
     */
    private TmTaskDraft buildExperimentSpecTask(TmExperimentSpecMonthPlanRowVo row, BigDecimal experimentPlanQty,
                                                TmExperimentSpecInfo experimentSpecInfo,
                                                List<TmLossRule> lossRuleList, BigDecimal minStartQty,
                                                BigDecimal defaultCurlLength, BigDecimal toolTotalQty) {
        return this.buildExperimentSpecTask(null, row, experimentPlanQty, experimentSpecInfo,
                lossRuleList, minStartQty, defaultCurlLength, toolTotalQty);
    }

    /**
     * 构建实验规格任务草稿，并按本次参数快照标记小胶种。
     *
     * @param context 自动排程上下文
     * @param row 实验规格月计划行
     * @param experimentPlanQty 实验规格固定计划量
     * @param experimentSpecInfo 实验规格识别证据
     * @param lossRuleList 损耗配置列表
     * @param minStartQty 最小开车量
     * @param defaultCurlLength 默认卷曲长度
     * @param toolTotalQty 工装总量
     * @return 实验规格独立任务
     */
    private TmTaskDraft buildExperimentSpecTask(TmScheduleContext context, TmExperimentSpecMonthPlanRowVo row,
                                                BigDecimal experimentPlanQty,
                                                TmExperimentSpecInfo experimentSpecInfo,
                                                List<TmLossRule> lossRuleList, BigDecimal minStartQty,
                                                BigDecimal defaultCurlLength, BigDecimal toolTotalQty) {
        TmTaskDraft taskDraft = new TmTaskDraft();
        taskDraft.setOrderNo("EXP-" + StrUtil.blankToDefault(row.getProductionNo(), String.valueOf(row.getMonthPlanId()))
                + "-CLASS" + TmScheduleConstants.EXPERIMENT_SPEC_SHIFT_ORDER);
        taskDraft.setSourceOrderNos(appendSourceOrderNos(null, experimentSpecInfo.getProductionNos()));
        taskDraft.setBusinessKeySuffix("EXP-" + StrUtil.blankToDefault(row.getProductionNo(), String.valueOf(row.getMonthPlanId()))
                + "-CLASS" + TmScheduleConstants.EXPERIMENT_SPEC_SHIFT_ORDER);
        taskDraft.setTreadCode(row.getTreadCode());
        // 拆分胶料类别：第一个值为主胶料编码，其余值为基部胶编码
        String rubberCategory = row.getTreadRubberCategory();
        if (StrUtil.isNotBlank(rubberCategory)) {
            String[] glueParts = rubberCategory.split(",");
            taskDraft.setGlueCode(glueParts[0].trim());
            if (glueParts.length > 1) {
                // 使用英文逗号拼接剩余部分作为基部胶编码
                String baseGlueCode = String.join(",", Arrays.copyOfRange(glueParts, 1, glueParts.length));
                taskDraft.setBaseGlueCode(baseGlueCode);
            }
        } else {
            taskDraft.setGlueCode(null);
            taskDraft.setBaseGlueCode(null);
        }
        taskDraft.setSmallGlueFlag(this.isSmallGlueCode(context, taskDraft.getGlueCode()));
        taskDraft.setMouthPlateCode(row.getTreadMouthPlate());
        taskDraft.setShiftOrder(TmScheduleConstants.EXPERIMENT_SPEC_SHIFT_ORDER);
        taskDraft.setTreadShoulderLength(nvl(row.getTreadShoulderLength()));
        taskDraft.setTailFlag(TmYesNoEnum.NO.getCode());
        taskDraft.setTailBalanceQty(BigDecimal.ZERO);
        taskDraft.setCurrentShiftDemandQty(experimentPlanQty);
        taskDraft.setGuardDemandQty(experimentPlanQty);
        taskDraft.setDemandQty(experimentPlanQty);
        taskDraft.setPlanQty(experimentPlanQty);
        taskDraft.setBaseDemandQty(experimentPlanQty);
        taskDraft.setMinStartQty(minStartQty);
        taskDraft.setDefaultCurlRollLength(defaultCurlLength);
        if (toolTotalQty.compareTo(BigDecimal.ZERO) > 0) {
            taskDraft.setTotalToolQty(toolTotalQty);
        }
        taskDraft.setExperimentSpecInfo(experimentSpecInfo);
        experimentSpecInfo.setMergedToExistingTask(Boolean.FALSE);
        return taskDraft;
    }

    /**
     * 合并来源工单号。
     *
     * @param currentSourceOrderNos 当前来源工单号
     * @param appendSourceOrderNos 追加来源工单号
     * @return 合并后的来源工单号
     */
    private String appendSourceOrderNos(String currentSourceOrderNos, List<String> appendSourceOrderNos) {
        List<String> resultList = new ArrayList<>();
        if (StrUtil.isNotBlank(currentSourceOrderNos)) {
            resultList.addAll(Arrays.asList(currentSourceOrderNos.split(",")));
        }
        if (CollUtil.isNotEmpty(appendSourceOrderNos)) {
            resultList.addAll(appendSourceOrderNos);
        }
        return resultList.stream().filter(StrUtil::isNotBlank).distinct().collect(Collectors.joining(","));
    }

    /**
     * 构建月计划日期列名。
     *
     * @param experimentPlanDate 月计划定稿生产日期
     * @return DAY_1 到 DAY_31 的安全列名
     */
    private String buildExperimentDayColumn(Date experimentPlanDate) {
        int dayOfMonth = DateUtil.calendar(experimentPlanDate).get(Calendar.DAY_OF_MONTH);
        return "DAY_" + dayOfMonth;
    }
    /**
     * 构造来源任务业务键后缀。
     *
     * <p>同胎面、胶料、口型板和班次可能对应多条原成型排程结果，后缀用于防止快照、规则证据和解释记录互相覆盖。</p>
     *
     * @param row            成型需求行
     * @param sourceRowIndex 来源行顺序，从 1 开始
     * @param shiftOrder     胎面排程班次
     * @return 来源任务业务键后缀
     */
    private String buildSourceTaskBusinessKeySuffix(TmFormingDemandRowVo row, int sourceRowIndex, int shiftOrder) {
        String sourceOrderNo = row == null ? null : row.getOrderNo();
        String sourceKey = row != null && row.getSourceRecordId() != null
                ? "ID" + row.getSourceRecordId()
                : StrUtil.blankToDefault(sourceOrderNo, "ROW" + sourceRowIndex);
        return sourceKey + "-CLASS" + shiftOrder;
    }

    /**
     * 构造来源任务业务键后缀（RECIPE 模式重载）。
     *
     * @param row            成型需求行（示方书版本模式）
     * @param sourceRowIndex 来源行顺序，从 1 开始
     * @param shiftOrder     胎面排程班次
     * @return 来源任务业务键后缀
     */
    private String buildSourceTaskBusinessKeySuffix(TmFormingDemandRecipeRowVo row, int sourceRowIndex, int shiftOrder) {
        String sourceOrderNo = row == null ? null : row.getOrderNo();
        String sourceKey = row != null && row.getSourceRecordId() != null
                ? "ID" + row.getSourceRecordId()
                : StrUtil.blankToDefault(sourceOrderNo, "ROW" + sourceRowIndex);
        return sourceKey + "-CLASS" + shiftOrder;
    }

    /**
     * 构建胎面新规格判断结果。
     *
     * @param context 自动排程上下文
     * @param treadCodes 待判断的胎面编码集合（BOM 模式取成型行 treadCode，RECIPE 模式取逐班解析命中的 treadCode 并集）
     * @param lookbackDays 回看天数
     * @param advanceShiftCount 提前班次数
     * @return 胎面编码到新规格证据的映射
     */
    private Map<String, TmNewSpecInfo> buildNewSpecInfoMap(TmScheduleContext context,
                                                           Collection<String> treadCodes,
                                                           Integer lookbackDays,
                                                           Integer advanceShiftCount) {
        Map<String, TmNewSpecInfo> resultMap = new HashMap<>();
        if (CollUtil.isEmpty(treadCodes)) {
            return resultMap;
        }
        List<String> treadCodeList = treadCodes.stream()
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(treadCodeList)) {
            return resultMap;
        }
        Date previousDate = DateUtil.offsetDay(context.getScheduleDate(), -1);
        Date historyStartDate = DateUtil.offsetDay(context.getScheduleDate(), -lookbackDays);
        Map<String, BigDecimal> previousStockMap = queryPreviousDayStockMap(context, treadCodeList, previousDate);
        Map<String, Boolean> historyPlanMap = queryHistoryPlanExistsMap(context, treadCodeList, historyStartDate, previousDate);
        String lookbackSource = getPositiveIntegerParamSource(context,
                TmScheduleConstants.PARAM_NEW_SPEC_LOOKBACK_DAYS,
                TmScheduleConstants.DEFAULT_NEW_SPEC_LOOKBACK_DAYS_VALUE);
        String advanceSource = getPositiveIntegerParamSource(context,
                TmScheduleConstants.PARAM_NEW_SPEC_ADVANCE_SHIFT_COUNT,
                TmScheduleConstants.DEFAULT_NEW_SPEC_ADVANCE_SHIFT_COUNT_VALUE);
        for (String treadCode : treadCodeList) {
            BigDecimal previousStockQty = previousStockMap.getOrDefault(treadCode, BigDecimal.ZERO);
            boolean previousStockExists = previousStockQty.compareTo(BigDecimal.ZERO) > 0;
            boolean historyPlanExists = Boolean.TRUE.equals(historyPlanMap.get(treadCode));
            TmNewSpecInfo info = new TmNewSpecInfo();
            info.setNewSpec(!previousStockExists && !historyPlanExists);
            info.setLookbackDays(lookbackDays);
            info.setLookbackDaysSource(lookbackSource);
            info.setAdvanceShiftCount(advanceShiftCount);
            info.setAdvanceShiftCountSource(advanceSource);
            info.setPreviousStockDate(previousDate);
            info.setPreviousDayStockQty(previousStockQty);
            info.setPreviousDayStockExists(previousStockExists);
            info.setHistoryStartDate(historyStartDate);
            info.setHistoryEndDate(previousDate);
            info.setHistorySchedulePlanExists(historyPlanExists);
            info.setReason(Boolean.TRUE.equals(info.getNewSpec())
                    ? "新规格前一天无有效库存且历史回看期无排程计划量"
                    : "非新规格，存在前一天有效库存或历史回看期排程计划量");
            resultMap.put(treadCode, info);
        }
        return resultMap;
    }

    /**
     * 查询前一天胎面净库存。
     *
     * @param context 自动排程上下文
     * @param treadCodes 胎面编码集合
     * @param previousDate 前一天库存日期
     * @return 胎面编码到净库存的映射
     */
    private Map<String, BigDecimal> queryPreviousDayStockMap(TmScheduleContext context, List<String> treadCodes, Date previousDate) {
        Map<String, BigDecimal> stockMap = new HashMap<>();
        if (tmStockMapper == null || CollUtil.isEmpty(treadCodes)) {
            return stockMap;
        }
        LambdaQueryWrapper<TmStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmStock::getFactoryCode, context.getFactoryCode());
        wrapper.eq(TmStock::getStockDate, previousDate);
        wrapper.in(TmStock::getTreadCode, treadCodes);
        wrapper.eq(TmStock::getIsDelete, 0);
        List<TmStock> stockList = Optional.ofNullable(tmStockMapper.selectList(wrapper)).orElse(Collections.emptyList());
        for (TmStock stock : stockList) {
            if (stock == null || StrUtil.isBlank(stock.getTreadCode())) {
                continue;
            }
            BigDecimal stockQty = nvl(stock.getStockQty()).subtract(nvl(stock.getBadQty())).add(nvl(stock.getAdjustQty()));
            stockMap.merge(stock.getTreadCode(), stockQty, BigDecimal::add);
        }
        return stockMap;
    }

    /**
     * 查询历史回看期是否存在胎面排程计划量。
     *
     * @param context 自动排程上下文
     * @param treadCodes 胎面编码集合
     * @param historyStartDate 回看开始日期
     * @param historyEndDate 回看结束日期
     * @return 胎面编码到是否存在计划量的映射
     */
    private Map<String, Boolean> queryHistoryPlanExistsMap(TmScheduleContext context, List<String> treadCodes,
                                                           Date historyStartDate, Date historyEndDate) {
        Map<String, Boolean> historyPlanMap = new HashMap<>();
        if (tmScheduleResultMapper == null || CollUtil.isEmpty(treadCodes)) {
            return historyPlanMap;
        }
        LambdaQueryWrapper<TmScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmScheduleResult::getFactoryCode, context.getFactoryCode());
        wrapper.between(TmScheduleResult::getScheduleDate, historyStartDate, historyEndDate);
        wrapper.in(TmScheduleResult::getTreadCode, treadCodes);
        wrapper.eq(TmScheduleResult::getIsDelete, 0);
        List<TmScheduleResult> resultList = Optional.ofNullable(tmScheduleResultMapper.selectList(wrapper)).orElse(Collections.emptyList());
        for (TmScheduleResult result : resultList) {
            if (result == null || StrUtil.isBlank(result.getTreadCode()) || !hasAnyPlanQty(result)) {
                continue;
            }
            historyPlanMap.put(result.getTreadCode(), Boolean.TRUE);
        }
        return historyPlanMap;
    }

    /**
     * 构建单任务的新规格证据副本。
     *
     * @param source 胎面级新规格证据
     * @param normalShiftOrder 原正常目标班次
     * @param demandQty 当前任务需求量
     * @return 单任务新规格证据
     */
    private TmNewSpecInfo buildTaskNewSpecInfo(TmNewSpecInfo source, int normalShiftOrder, BigDecimal demandQty) {
        if (source == null) {
            return null;
        }
        TmNewSpecInfo target = new TmNewSpecInfo();
        target.setNewSpec(source.getNewSpec());
        target.setLookbackDays(source.getLookbackDays());
        target.setLookbackDaysSource(source.getLookbackDaysSource());
        target.setAdvanceShiftCount(source.getAdvanceShiftCount());
        target.setAdvanceShiftCountSource(source.getAdvanceShiftCountSource());
        target.setPreviousStockDate(source.getPreviousStockDate());
        target.setPreviousDayStockQty(source.getPreviousDayStockQty());
        target.setPreviousDayStockExists(source.getPreviousDayStockExists());
        target.setHistoryStartDate(source.getHistoryStartDate());
        target.setHistoryEndDate(source.getHistoryEndDate());
        target.setHistorySchedulePlanExists(source.getHistorySchedulePlanExists());
        target.setNormalTargetShift(normalShiftOrder);
        target.setDemandShift(normalShiftOrder);
        target.setDemandQty(demandQty);
        int adjustedShiftOrder = resolveTargetShiftOrder(target, normalShiftOrder);
        target.setAdjustedTargetShift(adjustedShiftOrder);
        target.setAdjustedTargetWindow(buildAdvanceWindow(adjustedShiftOrder));
        target.setReason(source.getReason());
        return target;
    }

    /**
     * 根据新规格证据解析最终目标班次。
     *
     * @param newSpecInfo 新规格证据
     * @param normalShiftOrder 原正常目标班次
     * @return 最终目标班次
     */
    private int resolveTargetShiftOrder(TmNewSpecInfo newSpecInfo, int normalShiftOrder) {
        if (newSpecInfo == null || !newSpecInfo.isNewSpecHit()) {
            return normalShiftOrder;
        }
        return Math.max(1, normalShiftOrder - Math.max(newSpecInfo.getAdvanceShiftCount(), 1));
    }

    /**
     * 解析任务实际使用的库存保证班数，并补充新规格成型需求窗口证据。
     *
     * <p>普通规格保持基础保证班数；新规格至少扩展到提前班数，但不会缩短深度配置或
     * {@code TM_MIN_STOCK_CLASS} 已给出的更深窗口。窗口超过成型 CLASS8 的部分仍由现有末三班平均量规则估算。</p>
     *
     * @param newSpecInfo 新规格判断与窗口证据
     * @param baseGuardShiftCount 深度配置或参数解析出的基础保证班数
     * @param shiftOrder 胎面排程来源班次
     * @param formingShiftOffset 胎面班次到成型班次的偏移量
     * @return 当前任务实际使用的库存保证班数
     */
    private int resolveEffectiveGuardShiftCount(TmNewSpecInfo newSpecInfo, int baseGuardShiftCount,
                                                int shiftOrder, int formingShiftOffset) {
        int normalizedBaseGuardShiftCount = Math.max(baseGuardShiftCount, 1);
        int effectiveGuardShiftCount = normalizedBaseGuardShiftCount;
        if (newSpecInfo != null && newSpecInfo.isNewSpecHit()) {
            int advanceShiftCount = newSpecInfo.getAdvanceShiftCount() == null
                    ? 1 : Math.max(newSpecInfo.getAdvanceShiftCount(), 1);
            effectiveGuardShiftCount = Math.max(normalizedBaseGuardShiftCount, advanceShiftCount);
        }
        if (newSpecInfo != null) {
            int formingWindowStartClass = this.resolveFormingStartIndex(shiftOrder, formingShiftOffset) + 1;
            int formingWindowEndClass = formingWindowStartClass + effectiveGuardShiftCount - 1;
            newSpecInfo.setBaseGuardShiftCount(normalizedBaseGuardShiftCount);
            newSpecInfo.setEffectiveGuardShiftCount(effectiveGuardShiftCount);
            newSpecInfo.setFormingWindowStartClass(formingWindowStartClass);
            newSpecInfo.setFormingWindowEndClass(formingWindowEndClass);
            newSpecInfo.setFormingWindowEstimatedShiftCount(Math.max(formingWindowEndClass - 8, 0));
        }
        return effectiveGuardShiftCount;
    }

    /**
     * 构建从一班开始到调整目标班次的提前窗口。
     *
     * @param adjustedShiftOrder 调整后的目标班次
     * @return 提前窗口班次集合
     */
    private List<Integer> buildAdvanceWindow(int adjustedShiftOrder) {
        List<Integer> shiftWindow = new ArrayList<>();
        for (int shiftOrder = 1; shiftOrder <= adjustedShiftOrder; shiftOrder++) {
            shiftWindow.add(shiftOrder);
        }
        return shiftWindow;
    }

    /**
     * 判断排程结果六个班次是否存在计划量。
     *
     * @param result 胎面排程结果
     * @return true 表示存在任一班次计划量大于0
     */
    private boolean hasAnyPlanQty(TmScheduleResult result) {
        return nvl(result.getClass1PlanQty()).compareTo(BigDecimal.ZERO) > 0
                || nvl(result.getClass2PlanQty()).compareTo(BigDecimal.ZERO) > 0
                || nvl(result.getClass3PlanQty()).compareTo(BigDecimal.ZERO) > 0
                || nvl(result.getClass4PlanQty()).compareTo(BigDecimal.ZERO) > 0
                || nvl(result.getClass5PlanQty()).compareTo(BigDecimal.ZERO) > 0
                || nvl(result.getClass6PlanQty()).compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 直接加载当前工厂的库存保证班数配置。
     *
     * <p>该配置参与自动排程核心计算，每次自动排程都直接查询数据库，不使用 Redis 或本地缓存，
     * 避免配置调整后仍使用旧库存保证班数。</p>
     *
     * @param context 自动排程上下文
     * @return 按区间起始机台数升序排列的库存保证班数配置；未配置或查询失败时返回空集合
     */
    private List<TmDepthConfig> loadDepthConfigs(TmScheduleContext context) {
        if (tmDepthConfigMapper == null) {
            return Collections.emptyList();
        }
        try {
            LambdaQueryWrapper<TmDepthConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TmDepthConfig::getFactoryCode, context.getFactoryCode());
            wrapper.orderByAsc(TmDepthConfig::getMinMachineQty);
            return Optional.ofNullable(tmDepthConfigMapper.selectList(wrapper)).orElse(Collections.emptyList()).stream()
                    .sorted(Comparator.comparing((TmDepthConfig config) -> config.getMinMachineQty() == null
                            ? Integer.MAX_VALUE : config.getMinMachineQty()))
                    .collect(Collectors.toList());
        } catch (RuntimeException ex) {
            log.warn("[TM_DEPTH_CONFIG_LOAD] factoryCode={} 加载库存保证班数配置失败，原因={}，将回退参数 {}",
                    context.getFactoryCode(), ex.getMessage(), TmScheduleConstants.PARAM_MIN_STOCK_CLASS);
            return Collections.emptyList();
        }
    }

    /**
     * 解析单条成型需求应使用的库存保证班数。
     *
     * <p>优先根据成型结果的硫化机数量匹配 {@code T_TM_DEPTH_CONFIG}；硫化机数量为空、
     * 未命中配置或命中配置的保证班数不是正整数时，回退原参数 {@code TM_MIN_STOCK_CLASS}。</p>
     *
     * @param context                 自动排程上下文
     * @param lhMachineCode           硫化机编码（成型需求行）
     * @param orderNo                 成型工单号（日志追溯）
     * @param depthConfigList         库存保证班数配置
     * @param fallbackGuardShiftCount 参数兜底库存保证班数
     * @return 当前成型来源使用的库存保证班数
     */
    private Integer resolveGuardShiftCount(TmScheduleContext context, String lhMachineCode, String orderNo,
                                           List<TmDepthConfig> depthConfigList, Integer fallbackGuardShiftCount) {
        Integer lhMachineQty = this.resolveLhMachineQty(lhMachineCode);
        if (lhMachineQty == null) {
            log.warn("[TM_DEPTH_CONFIG_MATCH] factoryCode={}, orderNo={} 硫化机编码为空或无法解析，回退参数 {}={}",
                    context.getFactoryCode(), orderNo, TmScheduleConstants.PARAM_MIN_STOCK_CLASS,
                    fallbackGuardShiftCount);
            return fallbackGuardShiftCount;
        }
        if (CollUtil.isEmpty(depthConfigList)) {
            log.warn("[TM_DEPTH_CONFIG_MATCH] factoryCode={}, orderNo={}, lhMachineQty={} 未维护库存保证班数配置，回退参数 {}={}",
                    context.getFactoryCode(), orderNo, lhMachineQty, TmScheduleConstants.PARAM_MIN_STOCK_CLASS,
                    fallbackGuardShiftCount);
            return fallbackGuardShiftCount;
        }
        Optional<TmDepthConfig> matchedConfigOptional = depthConfigList.stream()
                .filter(depthConfig -> depthConfig.getMinMachineQty() != null
                        && lhMachineQty >= depthConfig.getMinMachineQty()
                        && (depthConfig.getMaxMachineQty() == null
                        || lhMachineQty <= depthConfig.getMaxMachineQty()))
                .findFirst();
        if (matchedConfigOptional.isPresent()) {
            return this.resolveMatchedGuardShiftCount(context, orderNo, lhMachineQty, matchedConfigOptional.get(),
                    fallbackGuardShiftCount);
        }
        log.warn("[TM_DEPTH_CONFIG_MATCH] factoryCode={}, orderNo={}, lhMachineQty={} 未命中库存保证班数配置，回退参数 {}={}",
                    context.getFactoryCode(), orderNo, lhMachineQty, TmScheduleConstants.PARAM_MIN_STOCK_CLASS,
                    fallbackGuardShiftCount);
        return fallbackGuardShiftCount;
    }

    /**
     * 将已命中的深度配置转换为库存保证班数。
     *
     * @param context                 自动排程上下文
     * @param orderNo                 成型工单号（日志追溯）
     * @param lhMachineQty            硫化机数量
     * @param depthConfig             已命中的深度配置
     * @param fallbackGuardShiftCount 参数兜底库存保证班数
     * @return 当前成型来源使用的库存保证班数
     */
    private Integer resolveMatchedGuardShiftCount(TmScheduleContext context, String orderNo,
                                                  Integer lhMachineQty, TmDepthConfig depthConfig,
                                                  Integer fallbackGuardShiftCount) {
        Integer guardShiftCount = this.toPositiveIntegerDepthClassQty(depthConfig.getDepthClassQty());
        if (guardShiftCount != null) {
            return guardShiftCount;
        }
        log.warn("[TM_DEPTH_CONFIG_MATCH] factoryCode={}, orderNo={}, lhMachineQty={}, minMachineQty={}, maxMachineQty={}, depthClassQty={} 不是正整数，回退参数 {}={}",
                context.getFactoryCode(), orderNo, lhMachineQty, depthConfig.getMinMachineQty(),
                    depthConfig.getMaxMachineQty(), depthConfig.getDepthClassQty(),
                    TmScheduleConstants.PARAM_MIN_STOCK_CLASS, fallbackGuardShiftCount);
        return fallbackGuardShiftCount;
    }

    /**
     * 根据成型结果硫化机编码解析硫化机数量。
     *
     * @param lhMachineCode 硫化机编码，多个编码使用英文逗号分隔
     * @return 去重后的硫化机数量；为空或无法解析时返回 null
     */
    private Integer resolveLhMachineQty(String lhMachineCode) {
        if (StrUtil.isBlank(lhMachineCode)) {
            return null;
        }
        Set<String> machineCodeSet = Arrays.stream(lhMachineCode.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return machineCodeSet.isEmpty() ? null : machineCodeSet.size();
    }

    /**
     * 将库存保证班数配置值转换为正整数。
     *
     * @param depthClassQty 配置的库存保证班数
     * @return 正整数班数；为空、非正数或非整数时返回 null
     */
    private Integer toPositiveIntegerDepthClassQty(BigDecimal depthClassQty) {
        if (depthClassQty == null || depthClassQty.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        try {
            return depthClassQty.stripTrailingZeros().intValueExact();
        } catch (ArithmeticException ex) {
            return null;
        }
    }

    /**
     * 补充任务草稿的胎面辅助基础数据。
     *
     * @param context       自动排程上下文
     * @param taskDraftList 待排任务草稿
     */
    private void fillTaskAuxiliaryData(TmScheduleContext context, List<TmTaskDraft> taskDraftList) {
        if (CollUtil.isEmpty(taskDraftList) || tmCurlRollMapper == null) {
            return;
        }
        List<String> treadCodes = taskDraftList.stream()
                .map(TmTaskDraft::getTreadCode)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(treadCodes)) {
            return;
        }
        LambdaQueryWrapper<TmCurlRoll> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmCurlRoll::getFactoryCode, context.getFactoryCode());
        wrapper.in(TmCurlRoll::getTreadCode, treadCodes);
        Map<String, BigDecimal> curlLengthMap = tmCurlRollMapper.selectList(wrapper).stream()
                .filter(curlRoll -> StrUtil.isNotBlank(curlRoll.getTreadCode()))
                .collect(Collectors.toMap(TmCurlRoll::getTreadCode,
                        curlRoll -> nvl(curlRoll.getCurlLength()),
                        (existing, replacement) -> existing));
        for (TmTaskDraft taskDraft : taskDraftList) {
            BigDecimal curlLength = curlLengthMap.get(taskDraft.getTreadCode());
            if (curlLength != null && curlLength.compareTo(BigDecimal.ZERO) > 0) {
                taskDraft.setCurlRollLength(curlLength);
            }
        }
    }

    /**
     * 将当前日停产重分配结果写入目标班次任务规则证据。
     *
     * @param context       自动排程上下文
     * @param taskDraftList 当前六班任务
     */
    private void addCurrentDayShutdownTraces(TmScheduleContext context, List<TmTaskDraft> taskDraftList) {
        if (context.getCurrentDayShutdownEvidenceMap() == null
                || context.getCurrentDayShutdownEvidenceMap().isEmpty() || CollUtil.isEmpty(taskDraftList)) {
            return;
        }
        taskDraftList.stream()
                .filter(Objects::nonNull)
                .filter(task -> context.getCurrentDayShutdownEvidenceMap().containsKey(task.getShiftOrder()))
                .forEach(task -> {
                    Map<String, Object> evidence = new LinkedHashMap<>(
                            context.getCurrentDayShutdownEvidenceMap().get(task.getShiftOrder()));
                    evidence.put("finalDemandQty", task.getCurrentShiftDemandQty());
                    TmScheduleRuleResultEnum result = TmScheduleRuleResultEnum.valueOf(
                            String.valueOf(evidence.remove("result")));
                    context.getRuleTraceMap().computeIfAbsent(task.getBusinessKey(), key -> new TmRuleTrace())
                            .addRuleHit(TmScheduleRuleCodeEnum.CURRENT_DAY_SHUTDOWN_REDISTRIBUTION, result, evidence);
                });
    }

    /**
     * 应用未来停产需求提前均摊和整日停产后首班识别规则。
     *
     * @param context       排程上下文
     * @param taskDraftList 当前六班任务
     * @param machineList   胎面机台列表
     */
    private void applyCalendarRules(TmScheduleContext context, List<TmTaskDraft> taskDraftList,
                                    List<TmMachineInfo> machineList) {
        int checkWindow = Math.min(Math.max(getIntegerParam(context,
                TmScheduleConstants.PARAM_SHUTDOWN_CHECK_WINDOW,
                TmScheduleConstants.DEFAULT_SHUTDOWN_CHECK_WINDOW_VALUE),
                TmScheduleConstants.MIN_SHUTDOWN_CHECK_WINDOW),
                TmScheduleConstants.MAX_SHUTDOWN_CHECK_WINDOW);
        Date startDate = DateUtil.beginOfDay(DateUtil.offsetDay(context.getScheduleDate(), -1));
        Date endDate = DateUtil.beginOfDay(DateUtil.offsetDay(context.getScheduleDate(), checkWindow - 1));
        Map<String, TmWorkCalendarRowVo> tmCalendarMap = this.loadWorkCalendarRange(context,
                TmProcessCodeEnum.TREAD.getCode(),
                startDate, endDate);
        Map<String, TmWorkCalendarRowVo> cxCalendarMap = this.loadWorkCalendarRange(context,
                TmProcessCodeEnum.FORMING.getCode(),
                startDate, endDate);
        this.resolveStartupShiftOrders(context, tmCalendarMap);
        for (int dayOffset = 1; dayOffset < checkWindow; dayOffset++) {
            Date sourceDate = DateUtil.beginOfDay(DateUtil.offsetDay(context.getScheduleDate(), dayOffset));
            TmWorkCalendarRowVo tmCalendar = tmCalendarMap.get(DateUtil.formatDate(sourceDate));
            TmWorkCalendarRowVo cxCalendar = cxCalendarMap.get(DateUtil.formatDate(sourceDate));
            if (!this.isShutdownDay(tmCalendar) || this.isShutdownDay(cxCalendar)) {
                continue;
            }
            List<Integer> targetShiftOrders = this.resolveFutureShutdownTargetShifts(context, sourceDate, tmCalendarMap);
            TmScheduleContext futureContext = this.buildFutureDemandContext(context, sourceDate);
            List<TmTaskDraft> futureTaskList = this.loadFormingDemandTasks(futureContext, machineList).stream()
                    .filter(task -> task.getExperimentSpecInfo() == null)
                    .filter(task -> task.getShiftOrder() != null && task.getShiftOrder() <= 3)
                    .collect(Collectors.toList());
            this.redistributeFutureShutdownTasks(context, taskDraftList, futureTaskList, sourceDate,
                    targetShiftOrders);
        }
    }

    /**
     * 查询日期区间工作日历，数据库异常必须阻断本批。
     *
     * @param context   排程上下文
     * @param procCode  工序编码
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 日期文本到工作日历的映射
     */
    private Map<String, TmWorkCalendarRowVo> loadWorkCalendarRange(TmScheduleContext context, String procCode,
                                                                   Date startDate, Date endDate) {
        try {
            List<TmWorkCalendarRowVo> rowList = tmAutoScheduleDataLoadMapper.selectWorkCalendarRowsByRange(
                    context.getFactoryCode(), procCode, startDate, endDate);
            return nullToEmpty(rowList).stream()
                    .filter(row -> row != null && row.getProductionDate() != null)
                    .collect(Collectors.toMap(row -> DateUtil.formatDate(row.getProductionDate()),
                            row -> row, (existing, replacement) -> existing, LinkedHashMap::new));
        } catch (RuntimeException exception) {
            log.error("[TM_CALENDAR_RANGE] factoryCode={}, procCode={}, startDate={}, endDate={}, result=FAILED",
                    context.getFactoryCode(), procCode, DateUtil.formatDate(startDate), DateUtil.formatDate(endDate),
                    exception);
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.workCalendarQueryFailed"),
                    exception);
        }
    }

    /**
     * 识别当前六班窗口内整日停产后的首个开放班次。
     *
     * @param context       排程上下文
     * @param tmCalendarMap 胎面日历映射
     */
    private void resolveStartupShiftOrders(TmScheduleContext context,
                                           Map<String, TmWorkCalendarRowVo> tmCalendarMap) {
        Set<Integer> startupShiftOrders = new LinkedHashSet<>();
        Date previousDate = DateUtil.offsetDay(context.getScheduleDate(), -1);
        for (int dayOffset = 0; dayOffset <= 1; dayOffset++) {
            Date currentDate = DateUtil.offsetDay(context.getScheduleDate(), dayOffset);
            TmWorkCalendarRowVo previousCalendar = tmCalendarMap.get(DateUtil.formatDate(previousDate));
            TmWorkCalendarRowVo currentCalendar = tmCalendarMap.get(DateUtil.formatDate(currentDate));
            if (this.isShutdownDay(previousCalendar) && !this.isShutdownDay(currentCalendar)) {
                for (int calendarShift = 1; calendarShift <= 3; calendarShift++) {
                    if (this.isShiftOpen(currentCalendar, calendarShift)) {
                        int startupShiftOrder = dayOffset * 3 + calendarShift;
                        startupShiftOrders.add(startupShiftOrder);
                        log.info("[TM_STARTUP_SHIFT] batchNo={}, traceId={}, factoryCode={}, previousDate={}, currentDate={}, startupShiftOrder={}, calendarShift={}, detectionScope=PREVIOUS_FULL_DAY_SHUTDOWN",
                                context.getBatchNo(), context.getTraceId(), context.getFactoryCode(),
                                DateUtil.formatDate(previousDate), DateUtil.formatDate(currentDate),
                                startupShiftOrder, calendarShift);
                        break;
                    }
                }
            }
            previousDate = currentDate;
        }
        context.setStartupShiftOrderSet(startupShiftOrders);
        log.info("[TM_STARTUP_SHIFT_SUMMARY] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, startupShiftOrders={}",
                context.getBatchNo(), context.getTraceId(), context.getFactoryCode(),
                DateUtil.formatDate(context.getScheduleDate()), startupShiftOrders);
    }

    /**
     * 解析未来停产需求可提前承接的当前六班开放班次。
     *
     * @param context       排程上下文
     * @param shutdownDate  停产日期
     * @param tmCalendarMap 胎面日历映射
     * @return 可承接班次顺序
     */
    private List<Integer> resolveFutureShutdownTargetShifts(TmScheduleContext context, Date shutdownDate,
                                                             Map<String, TmWorkCalendarRowVo> tmCalendarMap) {
        List<Integer> targetShiftOrders = new ArrayList<>();
        for (int shiftOrder = 1; shiftOrder <= TmScheduleConstants.TM_MAX_SHIFT_ORDER; shiftOrder++) {
            int dayOffset = (shiftOrder - 1) / 3;
            Date targetDate = DateUtil.offsetDay(context.getScheduleDate(), dayOffset);
            if (!targetDate.before(shutdownDate)) {
                continue;
            }
            TmWorkCalendarRowVo calendar = tmCalendarMap.get(DateUtil.formatDate(targetDate));
            if (this.isShiftOpen(calendar, shiftOrder)) {
                targetShiftOrders.add(shiftOrder);
            }
        }
        return targetShiftOrders;
    }

    /**
     * 构造未来成型需求加载上下文，复用当前 RECIPE/BOM 版本模式和需求算法。
     *
     * @param sourceContext 当前排程上下文
     * @param sourceDate    未来需求日期
     * @return 未来需求加载上下文
     */
    private TmScheduleContext buildFutureDemandContext(TmScheduleContext sourceContext, Date sourceDate) {
        TmScheduleContext futureContext = new TmScheduleContext();
        futureContext.setFactoryCode(sourceContext.getFactoryCode());
        futureContext.setScheduleDate(sourceDate);
        futureContext.setBatchNo(sourceContext.getBatchNo());
        futureContext.setTraceId(sourceContext.getTraceId());
        futureContext.setOperator(sourceContext.getOperator());
        futureContext.setParamMap(sourceContext.getParamMap());
        futureContext.setLossRuleList(sourceContext.getLossRuleList());
        futureContext.setSmallGlueCodeSet(sourceContext.getSmallGlueCodeSet());
        return futureContext;
    }

    /**
     * 将未来整日停产需求均摊到停产前的当前六班开放班次。
     *
     * @param context           当前排程上下文
     * @param currentTaskList   当前任务列表
     * @param futureTaskList    未来停产日来源任务
     * @param sourceDate        来源停产日期
     * @param targetShiftOrders 可承接班次
     */
    private void redistributeFutureShutdownTasks(TmScheduleContext context, List<TmTaskDraft> currentTaskList,
                                                 List<TmTaskDraft> futureTaskList, Date sourceDate,
                                                 List<Integer> targetShiftOrders) {
        Set<String> sourceKeySet = new HashSet<>();
        for (TmTaskDraft sourceTask : futureTaskList) {
            String sourceKey = DateUtil.formatDate(sourceDate) + "|" + sourceTask.getSourceOrderNos()
                    + "|" + sourceTask.getShiftOrder() + "|" + sourceTask.getTreadCode();
            if (!sourceKeySet.add(sourceKey)) {
                continue;
            }
            BigDecimal originalDemandQty = nvl(sourceTask.getCurrentShiftDemandQty());
            if (originalDemandQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (CollUtil.isEmpty(targetShiftOrders)) {
                TmTaskDraft unplannedTask = this.copyFutureShutdownTask(sourceTask, 6, originalDemandQty,
                        sourceDate, TmScheduleConstants.FUTURE_SHUTDOWN_NO_TARGET_SUFFIX);
                unplannedTask.setPlanQty(BigDecimal.ZERO);
                unplannedTask.setUnplannedReasonCode(TmUnplannedReasonEnum.TM_SHUTDOWN_NO_AVAILABLE_SHIFT.getCode());
                unplannedTask.setUnplannedReasonDesc(TmUnplannedReasonEnum.TM_SHUTDOWN_NO_AVAILABLE_SHIFT.getDesc());
                currentTaskList.add(unplannedTask);
                this.addFutureShutdownTrace(context, unplannedTask, sourceDate, sourceTask.getShiftOrder(), null,
                        originalDemandQty, BigDecimal.ZERO, TmScheduleRuleResultEnum.REJECT);
                continue;
            }
            BigDecimal allocatedQty = originalDemandQty.divide(BigDecimal.valueOf(targetShiftOrders.size()),
                    TmScheduleConstants.DECIMAL_CALCULATION_SCALE, RoundingMode.HALF_UP);
            for (Integer targetShiftOrder : targetShiftOrders) {
                TmTaskDraft targetTask = currentTaskList.stream()
                        .filter(task -> Objects.equals(task.getTreadCode(), sourceTask.getTreadCode()))
                        .filter(task -> Objects.equals(task.getShiftOrder(), targetShiftOrder))
                        .findFirst().orElse(null);
                if (targetTask == null) {
                    targetTask = this.copyFutureShutdownTask(sourceTask, targetShiftOrder, allocatedQty,
                            sourceDate, String.valueOf(sourceTask.getShiftOrder()));
                    currentTaskList.add(targetTask);
                } else {
                    targetTask.setCurrentShiftDemandQty(nvl(targetTask.getCurrentShiftDemandQty()).add(allocatedQty));
                    targetTask.setGuardDemandQty(nvl(targetTask.getGuardDemandQty()).add(allocatedQty));
                    targetTask.setDemandQty(nvl(targetTask.getDemandQty()).add(allocatedQty));
                    targetTask.setSourceOrderNos(this.appendSourceOrderNos(targetTask.getSourceOrderNos(),
                            Collections.singletonList(sourceTask.getSourceOrderNos())));
                }
                this.addFutureShutdownTrace(context, targetTask, sourceDate, sourceTask.getShiftOrder(),
                        targetShiftOrder, originalDemandQty, allocatedQty, TmScheduleRuleResultEnum.PASS);
            }
        }
    }

    /**
     * 复制未来停产来源任务为当前六班补充任务。
     *
     * @param sourceTask      来源任务
     * @param targetShift     目标班次
     * @param allocatedQty    分配量
     * @param sourceDate      来源日期
     * @param sourceShiftCode 来源班次标识
     * @return 补充任务
     */
    private TmTaskDraft copyFutureShutdownTask(TmTaskDraft sourceTask, Integer targetShift, BigDecimal allocatedQty,
                                               Date sourceDate, String sourceShiftCode) {
        TmTaskDraft targetTask = new TmTaskDraft();
        targetTask.setOrderNo(sourceTask.getOrderNo());
        targetTask.setSourceOrderNos(sourceTask.getSourceOrderNos());
        targetTask.setMaterialCode(sourceTask.getMaterialCode());
        targetTask.setMaterialDesc(sourceTask.getMaterialDesc());
        targetTask.setEmbryoCode(sourceTask.getEmbryoCode());
        targetTask.setMainMaterialDesc(sourceTask.getMainMaterialDesc());
        targetTask.setCxMachineCode(sourceTask.getCxMachineCode());
        targetTask.setLhMachineCode(sourceTask.getLhMachineCode());
        targetTask.setBusinessKeySuffix("FUTURE_SHUTDOWN_" + DateUtil.format(sourceDate, "yyyyMMdd")
                + "_CLASS" + sourceShiftCode + "_TO_CLASS" + targetShift);
        targetTask.setTreadCode(sourceTask.getTreadCode());
        targetTask.setGlueCode(sourceTask.getGlueCode());
        targetTask.setBaseGlueCode(sourceTask.getBaseGlueCode());
        targetTask.setMouthPlateCode(sourceTask.getMouthPlateCode());
        targetTask.setShiftOrder(targetShift);
        targetTask.setTreadShoulderLength(sourceTask.getTreadShoulderLength());
        targetTask.setTailFlag(sourceTask.getTailFlag());
        targetTask.setTailBalanceQty(sourceTask.getTailBalanceQty());
        targetTask.setCurrentShiftDemandQty(allocatedQty);
        targetTask.setGuardDemandQty(allocatedQty);
        targetTask.setFormingGuardWindowQtyMap(sourceTask.getFormingGuardWindowQtyMap());
        targetTask.setDemandQty(allocatedQty);
        targetTask.setGuardShiftCount(sourceTask.getGuardShiftCount());
        targetTask.setGuardRangeHours(sourceTask.getGuardRangeHours());
        targetTask.setSupplyHours(sourceTask.getSupplyHours());
        targetTask.setMinStartQty(sourceTask.getMinStartQty());
        targetTask.setDefaultCurlRollLength(sourceTask.getDefaultCurlRollLength());
        targetTask.setCurlRollLength(sourceTask.getCurlRollLength());
        targetTask.setSixClockStockQty(sourceTask.getSixClockStockQty());
        targetTask.setTotalToolQty(sourceTask.getTotalToolQty());
        targetTask.setSmallGlueFlag(sourceTask.getSmallGlueFlag());
        return targetTask;
    }

    /**
     * 写入未来停产需求重分配规则证据。
     *
     * @param context          排程上下文
     * @param task             目标任务
     * @param sourceDate       来源日期
     * @param sourceShiftOrder 来源班次
     * @param targetShiftOrder 目标班次
     * @param originalDemand   原需求
     * @param adjustedQty      调整量
     * @param result           规则结果
     */
    private void addFutureShutdownTrace(TmScheduleContext context, TmTaskDraft task, Date sourceDate,
                                        Integer sourceShiftOrder, Integer targetShiftOrder,
                                        BigDecimal originalDemand, BigDecimal adjustedQty,
                                        TmScheduleRuleResultEnum result) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("ruleCode", TmScheduleRuleCodeEnum.FUTURE_SHUTDOWN_REDISTRIBUTION.getCode());
        evidence.put("date", DateUtil.formatDate(sourceDate));
        evidence.put("sourceDate", DateUtil.formatDate(sourceDate));
        evidence.put("sourceShiftOrder", sourceShiftOrder);
        evidence.put("targetShiftOrder", targetShiftOrder);
        evidence.put("originalDemandQty", originalDemand);
        evidence.put("adjustedQty", adjustedQty);
        evidence.put("threshold", null);
        evidence.put("finalDemandQty", task.getCurrentShiftDemandQty());
        context.getRuleTraceMap().computeIfAbsent(task.getBusinessKey(), key -> new TmRuleTrace())
                .addRuleHit(TmScheduleRuleCodeEnum.FUTURE_SHUTDOWN_REDISTRIBUTION, result, evidence);
    }
    /**
     * 根据工作日历处理当前排程日停产需求重分配。
     *
     * @param context       自动排程上下文
     * @param classQtyArray 六班成型数量
     * @param tmCalendar    胎面工作日历
     * @param cxCalendar    成型工作日历
     * @return true 表示胎面停产且没有可接收重分配需求的班次
     */
    private boolean redistributeShutdownDemand(TmScheduleContext context, BigDecimal[] classQtyArray,
                                               TmWorkCalendarRowVo tmCalendar, TmWorkCalendarRowVo cxCalendar) {
        if (!TmYesNoEnum.YES.getCode().equals(getParamValue(context,
                TmScheduleConstants.PARAM_SHUTDOWN_REDISTRIBUTION_ENABLED,
                TmYesNoEnum.YES.getCode()))) {
            return false;
        }
        if (!isShutdownDay(tmCalendar) || isShutdownDay(cxCalendar)) {
            return false;
        }
        List<Integer> shutdownShiftList = new ArrayList<>();
        List<Integer> availableShiftList = new ArrayList<>();
        BigDecimal shutdownQty = BigDecimal.ZERO;
        for (int shiftOrder = 1; shiftOrder <= TmScheduleConstants.TM_MAX_SHIFT_ORDER; shiftOrder++) {
            if (isShiftOpen(tmCalendar, shiftOrder)) {
                availableShiftList.add(shiftOrder);
                continue;
            }
            if (isShiftOpen(cxCalendar, shiftOrder)) {
                shutdownShiftList.add(shiftOrder);
                shutdownQty = shutdownQty.add(classQtyArray[shiftOrder - 1]);
            }
        }
        if (CollUtil.isEmpty(shutdownShiftList) || shutdownQty.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        if (CollUtil.isEmpty(availableShiftList)) {
            for (Integer sourceShiftOrder : shutdownShiftList) {
                Map<String, Object> evidence = new LinkedHashMap<>();
                evidence.put("ruleCode", TmScheduleRuleCodeEnum.CURRENT_DAY_SHUTDOWN_REDISTRIBUTION.getCode());
                evidence.put("date", DateUtil.formatDate(context.getScheduleDate()));
                evidence.put("sourceShiftOrders", Collections.singletonList(sourceShiftOrder));
                evidence.put("targetShiftOrder", null);
                evidence.put("originalDemandQty", classQtyArray[sourceShiftOrder - 1]);
                evidence.put("adjustedQty", BigDecimal.ZERO);
                evidence.put("threshold", null);
                evidence.put("result", TmScheduleRuleResultEnum.REJECT.getCode());
                context.getCurrentDayShutdownEvidenceMap().put(sourceShiftOrder, evidence);
            }
            log.warn("[CURRENT_DAY_SHUTDOWN_REDISTRIBUTION] factoryCode={}, scheduleDate={} 胎面停产且无可分配班次",
                    context.getFactoryCode(), DateUtil.formatDate(context.getScheduleDate()));
            return true;
        }
        BigDecimal increaseQty = shutdownQty.divide(new BigDecimal(availableShiftList.size()),
                TmScheduleConstants.DECIMAL_CALCULATION_SCALE, RoundingMode.HALF_UP);
        for (Integer shiftOrder : shutdownShiftList) {
            classQtyArray[shiftOrder - 1] = BigDecimal.ZERO;
        }
        for (Integer shiftOrder : availableShiftList) {
            classQtyArray[shiftOrder - 1] = classQtyArray[shiftOrder - 1].add(increaseQty);
            Map<String, Object> evidence = context.getCurrentDayShutdownEvidenceMap()
                    .computeIfAbsent(shiftOrder, key -> new LinkedHashMap<>());
            evidence.put("ruleCode", TmScheduleRuleCodeEnum.CURRENT_DAY_SHUTDOWN_REDISTRIBUTION.getCode());
            evidence.put("date", DateUtil.formatDate(context.getScheduleDate()));
            evidence.put("sourceShiftOrders", new ArrayList<>(shutdownShiftList));
            evidence.put("targetShiftOrder", shiftOrder);
            evidence.put("originalDemandQty", nvl((BigDecimal) evidence.get("originalDemandQty")).add(shutdownQty));
            evidence.put("adjustedQty", nvl((BigDecimal) evidence.get("adjustedQty")).add(increaseQty));
            evidence.put("threshold", null);
            evidence.put("result", TmScheduleRuleResultEnum.PASS.getCode());
        }
        log.info("[CURRENT_DAY_SHUTDOWN_REDISTRIBUTION] factoryCode={}, scheduleDate={}, shutdownQty={}, availableShiftCount={}",
                context.getFactoryCode(), DateUtil.formatDate(context.getScheduleDate()), shutdownQty, availableShiftList.size());
        return false;
    }

    /**
     * 加载指定工序的当前排程日工作日历。
     *
     * @param context  自动排程上下文
     * @param procCode 工序编码
     * @return 工作日历行，未维护时返回 null
     */
    private TmWorkCalendarRowVo loadWorkCalendar(TmScheduleContext context, String procCode) {
        try {
            Date calendarDate = DateUtil.beginOfDay(context.getScheduleDate());
            String cacheKey = "calendar:" + context.getFactoryCode() + ":" + procCode + ":" + DateUtil.formatDate(calendarDate);
            List<TmWorkCalendarRowVo> rowList = tmAutoScheduleRedisCacheService.getCachedList(cacheKey,
                    () -> tmAutoScheduleDataLoadMapper.selectWorkCalendarRows(context.getFactoryCode(), procCode, calendarDate));
            return CollUtil.isEmpty(rowList) ? null : rowList.get(0);
        } catch (RuntimeException ex) {
            log.error("[TM_AUTO_SCHEDULE_LOAD] 加载工作日历失败，procCode={}，scheduleDate={}",
                    procCode, DateUtil.formatDate(context.getScheduleDate()), ex);
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.workCalendarQueryFailed"), ex);
        }
    }

    private boolean isShutdownDay(TmWorkCalendarRowVo calendar) {
        if (calendar == null) {
            return false;
        }
        return TmYesNoEnum.NO.getCode().equals(calendar.getDayFlag())
                || (!isShiftOpen(calendar, 1) && !isShiftOpen(calendar, 2) && !isShiftOpen(calendar, 3));
    }

    private boolean isShiftOpen(TmWorkCalendarRowVo calendar, int shiftOrder) {
        if (calendar == null) {
            return true;
        }
        int calendarShift = mapToCalendarShift(shiftOrder);
        if (calendarShift == 1) {
                return TmYesNoEnum.YES.getCode().equals(calendar.getOneShiftFlag());
        }
        if (calendarShift == 2) {
                return TmYesNoEnum.YES.getCode().equals(calendar.getTwoShiftFlag());
        }
                return TmYesNoEnum.YES.getCode().equals(calendar.getThreeShiftFlag());
    }

    /**
     * 将排程六班序号映射为工作日历三班序号。
     *
     * @param shiftOrder 排程班次序号
     * @return 工作日历班次序号，取值范围为1到3
     */
    private int mapToCalendarShift(int shiftOrder) {
        return ((shiftOrder - 1) % 3) + 1;
    }

    /**
     * 根据算法和成型班次偏移量解析胎面当前班需求对应的成型计划量。
     *
     * @param classQtyArray 成型班次计划量数组，下标 0 对应成型 CLASS1
     * @param shiftOrder 胎面排程班次，从 1 开始
     * @param algorithmCode 需求量算法编码
     * @param formingShiftOffset 胎面班次到成型班次的偏移量，0 表示同序号班次
     * @return 对应成型计划量；超过已加载成型班次时返回 0
     */
    private BigDecimal resolveFormingQty(BigDecimal[] classQtyArray, int shiftOrder, String algorithmCode,
            int formingShiftOffset) {
        int startIndex = resolveFormingStartIndex(shiftOrder, formingShiftOffset);
        if ("2".equals(algorithmCode)) {
            return readClassQty(classQtyArray, startIndex);
        }
        BigDecimal maxQty = BigDecimal.ZERO;
        for (int index = startIndex; index < startIndex + 3; index++) {
            maxQty = maxQty.max(readClassQty(classQtyArray, index));
        }
        return maxQty;
    }

    /**
     * 解析胎面当前班需求对应的成型计划量。
     *
     * <p>需求起点超过成型 CLASS8 时，使用 CLASS6、CLASS7、CLASS8 的固定三班平均量估算未来需求；
     * 成型完成量等非需求场景仍调用 {@link #resolveFormingQty(BigDecimal[], int, String, int)}，保持越界按零处理。</p>
     *
     * @param classQtyArray 成型班次计划量数组，下标 0 对应成型 CLASS1
     * @param shiftOrder 胎面排程班次，从 1 开始
     * @param algorithmCode 需求量算法编码
     * @param formingShiftOffset 胎面班次到成型班次的偏移量，0 表示同序号班次
     * @return 当前班需求对应的成型计划量
     */
    private BigDecimal resolveCurrentShiftFormingQty(BigDecimal[] classQtyArray, int shiftOrder, String algorithmCode,
                                                      int formingShiftOffset) {
        int startIndex = this.resolveFormingStartIndex(shiftOrder, formingShiftOffset);
        if (startIndex >= 8) {
            return this.calculateLastThreeClassAverageQty(classQtyArray);
        }
        return this.resolveFormingQty(classQtyArray, shiftOrder, algorithmCode, formingShiftOffset);
    }

    /**
     * 计算库存保证范围内的成型需求计划量。
     *
     * @param classQtyArray 成型班次计划量数组
     * @param shiftOrder 胎面排程班次，从 1 开始
     * @param guardShiftCount 库存最低保证班数
     * @param formingShiftOffset 胎面班次到成型班次的偏移量
     * @return 库存保证范围内的成型计划量合计
     */
    private BigDecimal calculateGuardFormingQty(BigDecimal[] classQtyArray, int shiftOrder, int guardShiftCount,
                                                int formingShiftOffset) {
        BigDecimal total = BigDecimal.ZERO;
        int startIndex = this.resolveFormingStartIndex(shiftOrder, formingShiftOffset);
        int count = Math.max(guardShiftCount, 1);
        for (int index = startIndex; index < startIndex + count; index++) {
            total = total.add(this.resolveGuardClassQty(classQtyArray, index));
        }
        return total;
    }

    /**
     * 读取保证窗口中指定成型班次的计划量，超过 CLASS8 时使用最后三班平均量。
     *
     * @param classQtyArray 成型班次计划量数组
     * @param classIndex 成型班次数组下标，下标 0 对应 CLASS1
     * @return 指定班次成型计划量
     */
    private BigDecimal resolveGuardClassQty(BigDecimal[] classQtyArray, int classIndex) {
        if (classIndex < 8) {
            return this.readClassQty(classQtyArray, classIndex);
        }
        return this.calculateLastThreeClassAverageQty(classQtyArray);
    }

    /**
     * 按 CLASS6、CLASS7、CLASS8 固定三班计算平均计划量，空值或零仍计入除数。
     *
     * @param classQtyArray 成型班次计划量数组
     * @return 最后三班平均计划量，保留六位小数并四舍五入
     */
    private BigDecimal calculateLastThreeClassAverageQty(BigDecimal[] classQtyArray) {
        return this.readClassQty(classQtyArray, 5)
                .add(this.readClassQty(classQtyArray, 6))
                .add(this.readClassQty(classQtyArray, 7))
                .divide(BigDecimal.valueOf(3), TmScheduleConstants.DECIMAL_CALCULATION_SCALE,
                        RoundingMode.HALF_UP);
    }

    /**
     * 使用成型硫化余量封顶保证窗口需求条数。
     *
     * @param rawGuardFormingQty 封顶前保证需求条数
     * @param lhRemainQty 硫化余量；为空时兼容旧数据不封顶
     * @return 封顶后保证需求条数
     */
    private BigDecimal capGuardFormingQty(BigDecimal rawGuardFormingQty, BigDecimal lhRemainQty) {
        BigDecimal nonNegativeGuardFormingQty = this.nvl(rawGuardFormingQty).max(BigDecimal.ZERO);
        if (lhRemainQty == null) {
            return nonNegativeGuardFormingQty;
        }
        return nonNegativeGuardFormingQty.min(lhRemainQty.max(BigDecimal.ZERO));
    }

    /**
     * 写入保证需求超八班估算及硫化余量封顶证据。
     *
     * @param context 排程上下文
     * @param taskDraft 任务草稿
     * @param classQtyArray 成型班次计划量数组
     * @param shiftOrder 胎面排程班次
     * @param guardShiftCount 保证班数
     * @param formingShiftOffset 成型班次偏移量
     * @param lhRemainQty 硫化余量
     * @param rawGuardFormingQty 封顶前保证需求条数
     * @param cappedGuardFormingQty 封顶后保证需求条数
     * @param mode 施工版本匹配模式
     */
    private void addGuardDemandEstimateTrace(TmScheduleContext context, TmTaskDraft taskDraft,
                                             BigDecimal[] classQtyArray, int shiftOrder, int guardShiftCount,
                                             int formingShiftOffset, BigDecimal lhRemainQty,
                                             BigDecimal rawGuardFormingQty, BigDecimal cappedGuardFormingQty,
                                             String mode) {
        int startIndex = this.resolveFormingStartIndex(shiftOrder, formingShiftOffset);
        int count = Math.max(guardShiftCount, 1);
        int exceedShiftCount = Math.max(startIndex + count - 8, 0);
        TmNewSpecInfo newSpecInfo = taskDraft.getNewSpecInfo();
        boolean newSpecWindow = newSpecInfo != null && newSpecInfo.isNewSpecHit();
        if (exceedShiftCount <= 0 && lhRemainQty == null && !newSpecWindow) {
            return;
        }
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("mode", mode);
        evidence.put("startFormingClass", startIndex + 1);
        evidence.put("guardShiftCount", count);
        evidence.put("exceedShiftCount", exceedShiftCount);
        if (newSpecWindow) {
            evidence.put("baseGuardShiftCount", newSpecInfo.getBaseGuardShiftCount());
            evidence.put("effectiveGuardShiftCount", newSpecInfo.getEffectiveGuardShiftCount());
            evidence.put("formingWindowStartClass", newSpecInfo.getFormingWindowStartClass());
            evidence.put("formingWindowEndClass", newSpecInfo.getFormingWindowEndClass());
            evidence.put("formingWindowEstimatedShiftCount", newSpecInfo.getFormingWindowEstimatedShiftCount());
        }
        evidence.put("class6PlanQty", this.readClassQty(classQtyArray, 5));
        evidence.put("class7PlanQty", this.readClassQty(classQtyArray, 6));
        evidence.put("class8PlanQty", this.readClassQty(classQtyArray, 7));
        evidence.put("lastThreeAverageQty", this.calculateLastThreeClassAverageQty(classQtyArray));
        evidence.put("lhRemainQty", lhRemainQty);
        evidence.put("rawGuardFormingQty", rawGuardFormingQty);
        evidence.put("cappedGuardFormingQty", cappedGuardFormingQty);
        evidence.put("capApplied", lhRemainQty != null
                && this.nvl(rawGuardFormingQty).compareTo(this.nvl(cappedGuardFormingQty)) > 0);
        context.getRuleTraceMap().computeIfAbsent(taskDraft.getBusinessKey(), key -> new TmRuleTrace())
                .addRuleHit(TmScheduleRuleCodeEnum.GUARD_DEMAND_ESTIMATE,
                        TmScheduleRuleResultEnum.PASS, evidence);
        log.info("[TM_GUARD_DEMAND_ESTIMATE] batchNo={}, orderNo={}, evidence={}",
                context.getBatchNo(), taskDraft.getOrderNo(), evidence);
    }

    /**
     * 按需求计算使用的逻辑班次窗口累计库存保证范围时长。
     *
     * <p>逻辑班次一至六直接使用对应配置，超过六班后按三班日周期映射到
     * CLASS1 至 CLASS3。窗口中任一班次时长缺失或非正数时不使用固定时长兜底，
     * 保持 {@code guardRangeHours} 为空并写入跳过证据。</p>
     *
     * @param context 排程上下文
     * @param taskDraft 任务草稿
     * @param shiftOrder 胎面排程班次
     * @param guardShiftCount 库存保证班数
     * @param formingShiftOffset 成型班次偏移量
     */
    private void fillGuardRangeHours(TmScheduleContext context, TmTaskDraft taskDraft, int shiftOrder,
                                     int guardShiftCount, int formingShiftOffset) {
        int logicalStartShiftOrder = this.resolveFormingStartIndex(shiftOrder, formingShiftOffset) + 1;
        int count = Math.max(guardShiftCount, 1);
        BigDecimal guardRangeHours = BigDecimal.ZERO;
        List<Integer> mappedShiftOrders = new ArrayList<>();
        List<BigDecimal> shiftHours = new ArrayList<>();
        String skipReason = null;
        for (int index = 0; index < count; index++) {
            int logicalShiftOrder = logicalStartShiftOrder + index;
            int mappedShiftOrder = this.mapGuardLogicalShiftOrder(logicalShiftOrder);
            BigDecimal currentShiftHours = context.getShiftHoursMap().get(mappedShiftOrder);
            mappedShiftOrders.add(mappedShiftOrder);
            shiftHours.add(currentShiftHours);
            if (currentShiftHours == null || currentShiftHours.compareTo(BigDecimal.ZERO) <= 0) {
                skipReason = "SHIFT_HOURS_MISSING_OR_NON_POSITIVE";
                break;
            }
            guardRangeHours = guardRangeHours.add(currentShiftHours);
        }
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("logicalStartShiftOrder", logicalStartShiftOrder);
        evidence.put("guardShiftCount", count);
        evidence.put("mappedShiftOrders", mappedShiftOrders);
        evidence.put("shiftHours", shiftHours);
        if (skipReason == null) {
            taskDraft.setGuardRangeHours(guardRangeHours);
            evidence.put("guardRangeHours", guardRangeHours);
            context.getRuleTraceMap().computeIfAbsent(taskDraft.getBusinessKey(), key -> new TmRuleTrace())
                    .addRuleHit(TmScheduleRuleCodeEnum.GUARD_RANGE_HOURS, TmScheduleRuleResultEnum.PASS, evidence);
            return;
        }
        taskDraft.setGuardRangeHours(null);
        evidence.put("guardRangeHours", null);
        evidence.put("reason", skipReason);
        context.getRuleTraceMap().computeIfAbsent(taskDraft.getBusinessKey(), key -> new TmRuleTrace())
                .addRuleHit(TmScheduleRuleCodeEnum.GUARD_RANGE_HOURS, TmScheduleRuleResultEnum.SKIP, evidence);
    }

    /**
     * 将超过六班的逻辑班次按三班日周期映射到实际班次配置。
     *
     * @param logicalShiftOrder 从一开始连续增长的逻辑班次
     * @return 一至六范围内的实际班次顺序
     */
    private int mapGuardLogicalShiftOrder(int logicalShiftOrder) {
        if (logicalShiftOrder <= TmScheduleConstants.TM_MAX_SHIFT_ORDER) {
            return logicalShiftOrder;
        }
        return ((logicalShiftOrder - TmScheduleConstants.TM_MAX_SHIFT_ORDER - 1) % 3) + 1;
    }

    /**
     * 将胎面班次和配置偏移量转换为成型计划量数组下标。
     *
     * @param shiftOrder 胎面排程班次，从 1 开始
     * @param formingShiftOffset 胎面班次到成型班次的偏移量
     * @return 成型计划量数组下标
     */
    private int resolveFormingStartIndex(int shiftOrder, int formingShiftOffset) {
        return Math.max(shiftOrder, 1) + Math.max(formingShiftOffset, 0) - 1;
    }

    /**
     * 读取成型班次计划量，超过数组范围时按 0 处理。
     *
     * @param classQtyArray 成型班次计划量数组
     * @param index 数组下标
     * @return 成型班次计划量
     */
    private BigDecimal readClassQty(BigDecimal[] classQtyArray, int index) {
        if (classQtyArray == null || index < 0 || index >= classQtyArray.length) {
            return BigDecimal.ZERO;
        }
        return nvl(classQtyArray[index]);
    }

    private BigDecimal[] buildClassQtyArray(TmFormingDemandRowVo row) {
        return new BigDecimal[]{
                nvl(row.getClass1PlanQty()),
                nvl(row.getClass2PlanQty()),
                nvl(row.getClass3PlanQty()),
                nvl(row.getClass4PlanQty()),
                nvl(row.getClass5PlanQty()),
                nvl(row.getClass6PlanQty()),
                nvl(row.getClass7PlanQty()),
                nvl(row.getClass8PlanQty())
        };
    }

    /**
     * 构建成型班次完成量数组（BOM 模式）。
     *
     * @param row 成型需求行
     * @return 一至八班成型完成量数组，空值按零处理
     */
    private BigDecimal[] buildClassFinishQtyArray(TmFormingDemandRowVo row) {
        return new BigDecimal[]{
                nvl(row.getClass1FinishQty()),
                nvl(row.getClass2FinishQty()),
                nvl(row.getClass3FinishQty()),
                nvl(row.getClass4FinishQty()),
                nvl(row.getClass5FinishQty()),
                nvl(row.getClass6FinishQty()),
                nvl(row.getClass7FinishQty()),
                nvl(row.getClass8FinishQty())
        };
    }

    /**
     * 判断机台是否启用。
     *
     * @param machineInfo 机台基础资料
     * @return true 表示可参与排程
     */
    private boolean isMachineEnabled(TmMachineInfo machineInfo) {
        return machineInfo != null && TmYesNoEnum.YES.getCode().equals(machineInfo.getMachineStatus());
    }

    /**
     * 空值转0。
     *
     * @param value 原始值
     * @return 非空值
     */
    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 按斜裁相同口径判断单胎胚当前班次是否收尾。
     * <p>月计划余量和成型计划量均为条数，必须直接比较，不能混入胎面长度后的米数；
     * 余量缺失或非法时保持非收尾，避免把数据缺失误判为可收尾。</p>
     *
     * @param planSurplusQty 胎胚月计划余量，单位条
     * @param formingPlanQty 当前班次成型计划量，单位条
     * @return true-月计划余量可由当前班次计划完成，false-仍按非收尾规格处理
     */
    private boolean isCloseOutByPlanSurplus(BigDecimal planSurplusQty, BigDecimal formingPlanQty) {
        return planSurplusQty != null
                && planSurplusQty.compareTo(BigDecimal.ZERO) >= 0
                && formingPlanQty != null
                && formingPlanQty.compareTo(BigDecimal.ZERO) > 0
                && planSurplusQty.compareTo(formingPlanQty) <= 0;
    }

    private String getParamValue(TmScheduleContext context, String paramCode, String defaultValue) {
        TmParamValue value = context.getParamMap().get(paramCode);
        return value == null || StrUtil.isBlank(value.getEffectiveValue()) ? defaultValue : value.getEffectiveValue();
    }

    private BigDecimal getDecimalParam(TmScheduleContext context, String paramCode) {
        String value = getParamValue(context, paramCode, "0");
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private Integer getIntegerParam(TmScheduleContext context, String paramCode, Integer defaultValue) {
        String value = getParamValue(context, paramCode, String.valueOf(defaultValue));
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    /**
     * 判断主胶料编码是否命中小胶种参数。
     *
     * @param context 自动排程上下文
     * @param glueCode 主胶料编码
     * @return true 表示该任务需要启用小胶种连续生产规则
     */
    private boolean isSmallGlueCode(TmScheduleContext context, String glueCode) {
        return context != null && CollUtil.isNotEmpty(context.getSmallGlueCodeSet())
                && StrUtil.isNotBlank(glueCode) && context.getSmallGlueCodeSet().contains(glueCode.trim());
    }
    /**
     * 读取非负整数参数，非法或小于 0 时返回默认值。
     *
     * @param context 自动排程上下文
     * @param paramCode 参数编码
     * @param defaultValue 默认值
     * @return 非负整数参数值
     */
    private Integer getNonNegativeIntegerParam(TmScheduleContext context, String paramCode, Integer defaultValue) {
        String value = getParamValue(context, paramCode, String.valueOf(defaultValue));
        try {
            Integer parsedValue = Integer.valueOf(value);
            if (parsedValue.compareTo(0) >= 0) {
                return parsedValue;
            }
            return defaultValue;
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    /**
     * 读取正整数参数，非法时返回默认值。
     *
     * @param context 自动排程上下文
     * @param paramCode 参数编码
     * @param defaultValue 默认值
     * @return 正整数参数值
     */
    private Integer getPositiveIntegerParam(TmScheduleContext context, String paramCode, Integer defaultValue) {
        String value = getParamValue(context, paramCode, String.valueOf(defaultValue));
        try {
            Integer parsedValue = Integer.valueOf(value);
            if (parsedValue.compareTo(0) > 0) {
                return parsedValue;
            }
            return defaultValue;
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    /**
     * 读取正整数参数来源，非法或缺失时标记为默认值。
     *
     * @param context 自动排程上下文
     * @param paramCode 参数编码
     * @param defaultValue 默认值
     * @return PARAM 或 DEFAULT
     */
    private String getPositiveIntegerParamSource(TmScheduleContext context, String paramCode, Integer defaultValue) {
        TmParamValue value = context.getParamMap().get(paramCode);
        if (value == null || StrUtil.isBlank(value.getParamValue())) {
            return TmParamValueSourceEnum.DEFAULT.getCode();
        }
        try {
            Integer parsedValue = Integer.valueOf(value.getParamValue());
            return parsedValue.compareTo(0) > 0
                    ? TmParamValueSourceEnum.PARAM.getCode() : TmParamValueSourceEnum.DEFAULT.getCode();
        } catch (NumberFormatException ex) {
            return TmParamValueSourceEnum.DEFAULT.getCode();
        }
    }

    /**
     * 读取正数类型参数，非法时返回默认值。
     *
     * @param context 自动排程上下文
     * @param paramCode 参数编码
     * @param defaultValue 默认值
     * @return 正数参数值
     */
    private BigDecimal getPositiveDecimalParam(TmScheduleContext context, String paramCode, BigDecimal defaultValue) {
        String value = getParamValue(context, paramCode, String.valueOf(defaultValue));
        try {
            BigDecimal parsedValue = new BigDecimal(value);
            if (parsedValue.compareTo(BigDecimal.ZERO) > 0) {
                return parsedValue;
            }
            return defaultValue;
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    /**
     * 读取正数类型参数来源，非法或缺失时标记为默认值。
     *
     * @param context 自动排程上下文
     * @param paramCode 参数编码
     * @return PARAM 或 DEFAULT
     */
    private String getPositiveDecimalParamSource(TmScheduleContext context, String paramCode) {
        TmParamValue value = context.getParamMap().get(paramCode);
        if (value == null || StrUtil.isBlank(value.getParamValue())) {
            return TmParamValueSourceEnum.DEFAULT.getCode();
        }
        try {
            BigDecimal parsedValue = new BigDecimal(value.getParamValue());
            return parsedValue.compareTo(BigDecimal.ZERO) > 0
                    ? TmParamValueSourceEnum.PARAM.getCode() : TmParamValueSourceEnum.DEFAULT.getCode();
        } catch (NumberFormatException ex) {
            return TmParamValueSourceEnum.DEFAULT.getCode();
        }
    }
    private void validateContext(TmScheduleContext context) {
        if (context == null) {
            throw new IllegalArgumentException("自动排程上下文不能为空");
        }
        if (StrUtil.isBlank(context.getFactoryCode())) {
            throw new IllegalArgumentException("自动排程工厂编号不能为空");
        }
        if (context.getScheduleDate() == null) {
            throw new IllegalArgumentException("自动排程日期不能为空");
        }
    }
}
