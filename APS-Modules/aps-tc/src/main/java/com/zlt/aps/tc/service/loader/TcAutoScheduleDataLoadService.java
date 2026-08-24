package com.zlt.aps.tc.service.loader;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.core.utils.SixShiftWorkCalendarUtil;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.domain.entity.*;
import com.zlt.aps.tc.api.enums.*;
import com.zlt.aps.tc.domain.vo.*;
import com.zlt.aps.tc.engine.domain.*;
import com.zlt.aps.tc.mapper.*;
import com.zlt.aps.tc.service.cache.TcAutoScheduleRedisCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎侧自动排程数据加载服务。
 *
 * <p>该服务属于胎侧业务模块，负责在自动排程入口事务内读取排程需要的基础数据并填充
 * {@link TcScheduleContext}。服务只做数据加载和任务草稿构造，不写排程结果、不修改任务链。</p>
 */
@Slf4j
@Service
public class TcAutoScheduleDataLoadService {

    /** 施工表胎侧长度单位由毫米换算为米的除数。 */
    private static final BigDecimal CONSTRUCTION_LENGTH_UNIT_DIVISOR = BigDecimal.valueOf(1000L);

    private final TcAutoScheduleRedisCacheService tcAutoScheduleRedisCacheService;
    /** 参数装载组件，只负责构建单次排程参数快照 */
    private final TcScheduleParamLoader tcScheduleParamLoader;
    /** 损耗规则装载组件，只负责业务配置到引擎规则的转换 */
    private final TcLossRuleLoader tmLossRuleLoader;
    @Resource
    private TcParamsMapper tmParamsMapper;
    @Resource
    private TcMachineInfoMapper tmMachineInfoMapper;
    @Resource
    private TcMouthPlateMapper tmMouthPlateMapper;
    @Resource
    private TcGlueMachineRealMapper tmGlueMachineRealMapper;
    @Resource
    private TcSpecifyMachineMapper tmSpecifyMachineMapper;
    @Resource
    private TcMachineSpeedMapper tmMachineSpeedMapper;
    @Resource
    private TcMachineMaintenanceMapper tmMachineMaintenanceMapper;
    @Resource
    private TcCurlRollMapper tmCurlRollMapper;
    @Resource
    private TcLossSettingMapper tmLossSettingMapper;
    @Resource
    private TcAutoScheduleDataLoadMapper tcAutoScheduleDataLoadMapper;
    @Resource
    private TcStockMapper tmStockMapper;
    @Resource
    private TcScheduleResultMapper tcScheduleResultMapper;
    @Resource
    private TcDepthConfigMapper tmDepthConfigMapper;
    @Resource
    private TcShiftConfigMapper tmShiftConfigMapper;
    @Resource
    private TcDjSharedMachineMapper tcDjSharedMachineMapper;

    /**
     * 创建胎侧自动排程数据加载服务。
     *
     * @param tcAutoScheduleRedisCacheService 自动排程 Redis 缓存服务
     */
    public TcAutoScheduleDataLoadService(TcAutoScheduleRedisCacheService tcAutoScheduleRedisCacheService) {
        this.tcAutoScheduleRedisCacheService = tcAutoScheduleRedisCacheService;
        this.tcScheduleParamLoader = new TcScheduleParamLoader();
        this.tmLossRuleLoader = new TcLossRuleLoader();
    }

    /**
     * 加载自动排程所需数据。
     *
     * @param context 自动排程上下文，必须包含工厂和排程日期
     * @throws IllegalArgumentException 上下文、工厂或排程日期为空时抛出
     */
    public void loadAllData(TcScheduleContext context) {
        validateContext(context);
        this.tcScheduleParamLoader.load(context, tmParamsMapper, tcAutoScheduleRedisCacheService);
        List<TcMachineInfo> machineList = loadMachineInfo(context);
        context.setMachineCandidateList(loadMachineCandidates(context, machineList));
        context.setLossRuleList(this.tmLossRuleLoader.load(context, tmLossSettingMapper));
        List<TcTaskDraft> taskDraftList = loadFormingDemandTasks(context, machineList);
        this.applyCalendarRules(context, taskDraftList, machineList);
        this.addCurrentDayShutdownTraces(context, taskDraftList);
        fillTaskAuxiliaryData(context, taskDraftList);
        context.setTaskDraftList(taskDraftList);
        log.info("[TC_AUTO_SCHEDULE_LOAD] factoryCode={}, scheduleDate={}, taskCount={}, machineCount={}",
                context.getFactoryCode(), DateUtil.formatDate(context.getScheduleDate()),
                taskDraftList.size(), machineList.size());
    }

    /**
     * 加载胎侧机台基础资料。
     *
     * @param context 自动排程上下文
     * @return 已启用或可参与排程的机台列表
     */
    private List<TcMachineInfo> loadMachineInfo(TcScheduleContext context) {
        LambdaQueryWrapper<TcMachineInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcMachineInfo::getFactoryCode, context.getFactoryCode());
        wrapper.eq(TcMachineInfo::getMachineStatus, ApsConstant.APS_YES_NO_1);
        wrapper.orderByAsc(TcMachineInfo::getMachineCode);
        return tcAutoScheduleRedisCacheService.getCachedList("machine:" + context.getFactoryCode(),
                () -> tmMachineInfoMapper.selectList(wrapper)).stream()
                .filter(machine -> StrUtil.isNotBlank(machine.getMachineCode()))
                .collect(Collectors.toList());
    }

    /**
     * 构建胎侧候选机台基础数据。
     *
     * <p>该方法只准备与任务无关或可复用的机台能力集合；口型、胶料、定点/禁排和剩余产能会在
     * {@code TcMachineAssignService} 中结合具体任务再次计算，避免不同任务复用同一候选列表导致误判。</p>
     *
     * @param context     自动排程上下文
     * @param machineList 机台基础资料
     * @return 候选机台列表
     */
    private List<TcMachineCandidate> loadMachineCandidates(TcScheduleContext context, List<TcMachineInfo> machineList) {
        if (CollUtil.isEmpty(machineList)) {
            return Collections.emptyList();
        }
        Map<String, TcMachineCandidate> candidateMap = new LinkedHashMap<>();
        for (TcMachineInfo machineInfo : machineList) {
            TcMachineCandidate candidate = new TcMachineCandidate();
            candidate.setMachineCode(machineInfo.getMachineCode());
            candidate.setEnabled(isMachineEnabled(machineInfo));
            candidate.setOpenShiftCodes(this.parseOpenShiftCodes(machineInfo.getOpenShiftCode()));
            BigDecimal machineMaxCapacity = this.resolveMachineMaxCapacity(machineInfo.getMaxCapacity());
            candidate.setMaxCapacity(machineMaxCapacity);
            candidate.setRemainCapacity(machineMaxCapacity);
            candidate.setMaintenanceHours(BigDecimal.ZERO);
            candidate.setSwitchCostHours(BigDecimal.ZERO);
            candidate.setConfiguredMouthPlateCodes(new HashSet<>());
            candidate.setMouthPlateCodes(new HashSet<>());
            candidate.setConfiguredGlueCodes(new HashSet<>());
            candidate.setAllowedGlueCodes(new HashSet<>());
            candidate.setForbiddenGlueCodes(new HashSet<>());
            candidate.setConfiguredFixedAllowSidewallCodes(new HashSet<>());
            candidate.setFixedAllowSidewallCodes(new HashSet<>());
            candidate.setFixedForbidSidewallCodes(new HashSet<>());
            candidate.setTcDjSharedMachine(Boolean.FALSE);
            candidate.setAllowedTcShiftCodes(new HashSet<>());
            candidate.setSharedDjShiftCodes(new HashSet<>());
            candidateMap.put(machineInfo.getMachineCode(), candidate);
        }
        fillCandidateMouthPlate(context, candidateMap);
        fillCandidateGlueRule(context, candidateMap);
        fillCandidateSpecifyRule(context, candidateMap);
        fillCandidateSpeed(context, candidateMap);
        List<TcShiftConfig> shiftConfigList = this.loadOpenShiftConfigs(context);
        this.fillCandidateSharedMachineRule(context, candidateMap);
        fillCandidateMaintenance(context, candidateMap, shiftConfigList);
        fillCandidatePredecessor(context, candidateMap);
        fillShiftHoursMap(context, shiftConfigList);
        return new ArrayList<>(candidateMap.values());
    }

    /**
     * 加载胎侧与垫胶共用机台的错班配置。
     *
     * @param context 自动排程上下文
     * @param candidateMap 候选机台映射
     */
    private void fillCandidateSharedMachineRule(TcScheduleContext context,
                                                Map<String, TcMachineCandidate> candidateMap) {
        if (tcDjSharedMachineMapper == null || candidateMap.isEmpty()) {
            return;
        }
        List<TcDjSharedMachine> sharedMachineList = tcDjSharedMachineMapper.selectList(
                new LambdaQueryWrapper<TcDjSharedMachine>()
                        .eq(TcDjSharedMachine::getFactoryCode, context.getFactoryCode())
                        .eq(TcDjSharedMachine::getEnableStatus, TcYesNoEnum.YES.getCode())
                        .in(TcDjSharedMachine::getMachineCode, candidateMap.keySet()));
        nullToEmpty(sharedMachineList).stream()
                .filter(sharedMachine -> StrUtil.isNotBlank(sharedMachine.getMachineCode()))
                .forEach(sharedMachine -> {
                    TcMachineCandidate candidate = candidateMap.get(sharedMachine.getMachineCode());
                    if (candidate == null) {
                        return;
                    }
                    candidate.setTcDjSharedMachine(Boolean.TRUE);
                    if (StrUtil.isNotBlank(sharedMachine.getTcShiftCode())) {
                        candidate.getAllowedTcShiftCodes().add(sharedMachine.getTcShiftCode());
                    }
                    if (StrUtil.isNotBlank(sharedMachine.getDjShiftCode())) {
                        candidate.getSharedDjShiftCodes().add(sharedMachine.getDjShiftCode());
                    }
                });
    }

    /**
     * 填充候选机台口型板能力。
     *
     * @param context      自动排程上下文
     * @param candidateMap 候选机台映射
     */
    private void fillCandidateMouthPlate(TcScheduleContext context, Map<String, TcMachineCandidate> candidateMap) {
        if (tmMouthPlateMapper == null || candidateMap.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<TcMouthPlate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcMouthPlate::getFactoryCode, context.getFactoryCode());
        wrapper.eq(TcMouthPlate::getPlateStatus, TcYesNoEnum.YES.getCode());
        List<TcMouthPlate> mouthPlateList = tmMouthPlateMapper.selectList(wrapper);
        for (TcMouthPlate mouthPlate : mouthPlateList) {
            if (StrUtil.isBlank(mouthPlate.getMouthPlateCode())
                    || !TcYesNoEnum.YES.getCode().equals(mouthPlate.getPlateStatus())) {
                continue;
            }
            context.getConfiguredMouthPlateCodeSet().add(mouthPlate.getMouthPlateCode());
            TcMachineCandidate candidate = candidateMap.get(mouthPlate.getMachineCode());
            if (candidate == null) {
                continue;
            }
            // 候选机台只记录自身具备的口型板，是否启用白名单由工厂级配置集合判断。
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
     * 填充候选机台胶料关系。
     *
     * @param context      自动排程上下文
     * @param candidateMap 候选机台映射
     */
    private void fillCandidateGlueRule(TcScheduleContext context, Map<String, TcMachineCandidate> candidateMap) {
        if (tmGlueMachineRealMapper == null || candidateMap.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<TcGlueMachineReal> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcGlueMachineReal::getFactoryCode, context.getFactoryCode());
        wrapper.eq(TcGlueMachineReal::getEnableStatus, TcYesNoEnum.YES.getCode());
        List<TcGlueMachineReal> glueRuleList = tmGlueMachineRealMapper.selectList(wrapper);
        for (TcGlueMachineReal glueRule : glueRuleList) {
            if (StrUtil.isBlank(glueRule.getGlueCode())
                    || !TcYesNoEnum.YES.getCode().equals(glueRule.getEnableStatus())) {
                continue;
            }
            context.getConfiguredGlueCodeSet().add(glueRule.getGlueCode());
            TcMachineCandidate candidate = candidateMap.get(glueRule.getMachineCode());
            if (candidate == null) {
                continue;
            }
            // 胶料关系只表达主胶料可生产机台，allowFlag 暂不参与排程判断。
            if (candidate.getConfiguredGlueCodes() == null) {
                candidate.setConfiguredGlueCodes(new HashSet<>());
            }
            candidate.getConfiguredGlueCodes().add(glueRule.getGlueCode());
        }
    }

    /**
     * 填充候选机台定点和禁排规则。
     *
     * @param context      自动排程上下文
     * @param candidateMap 候选机台映射
     */
    private void fillCandidateSpecifyRule(TcScheduleContext context, Map<String, TcMachineCandidate> candidateMap) {
        if (tmSpecifyMachineMapper == null || candidateMap.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<TcSpecifyMachine> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcSpecifyMachine::getFactoryCode, context.getFactoryCode());
        wrapper.eq(TcSpecifyMachine::getEnableStatus, TcYesNoEnum.YES.getCode());
        wrapper.in(TcSpecifyMachine::getMachineCode, candidateMap.keySet());
        List<TcSpecifyMachine> specifyList = tmSpecifyMachineMapper.selectList(wrapper);
        Set<String> configuredFixedAllowSidewallCodes = specifyList.stream()
                .filter(specify -> TcSpecifyMachineJobTypeEnum.ALLOW.getCode().equals(specify.getJobType()))
                .map(TcSpecifyMachine::getSidewallCode)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
        for (TcMachineCandidate candidate : candidateMap.values()) {
            if (candidate.getConfiguredFixedAllowSidewallCodes() == null) {
                candidate.setConfiguredFixedAllowSidewallCodes(new HashSet<>());
            }
            candidate.getConfiguredFixedAllowSidewallCodes().addAll(configuredFixedAllowSidewallCodes);
        }
        for (TcSpecifyMachine specify : specifyList) {
            TcMachineCandidate candidate = candidateMap.get(specify.getMachineCode());
            if (candidate == null || StrUtil.isBlank(specify.getSidewallCode())) {
                continue;
            }
            if (TcSpecifyMachineJobTypeEnum.ALLOW.getCode().equals(specify.getJobType())) {
                if (candidate.getFixedAllowSidewallCodes() == null) {
                    candidate.setFixedAllowSidewallCodes(new HashSet<>());
                }
                candidate.getFixedAllowSidewallCodes().add(specify.getSidewallCode());
            } else if (TcSpecifyMachineJobTypeEnum.FORBID.getCode().equals(specify.getJobType())) {
                if (candidate.getFixedForbidSidewallCodes() == null) {
                    candidate.setFixedForbidSidewallCodes(new HashSet<>());
                }
                candidate.getFixedForbidSidewallCodes().add(specify.getSidewallCode());
            }
        }
    }

    /**
     * 填充候选机台生产速度。
     *
     * @param context      自动排程上下文
     * @param candidateMap 候选机台映射
     */
    private void fillCandidateSpeed(TcScheduleContext context, Map<String, TcMachineCandidate> candidateMap) {
        if (tmMachineSpeedMapper == null || candidateMap.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<TcMachineSpeed> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcMachineSpeed::getFactoryCode, context.getFactoryCode());
        List<TcMachineSpeed> speedList = tmMachineSpeedMapper.selectList(wrapper);
        for (TcMachineSpeed speed : speedList) {
            if (speed.getProductSpeed() == null || speed.getProductSpeed().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (StrUtil.isBlank(speed.getMachineCode()) && StrUtil.isNotBlank(speed.getSidewallCode())) {
                for (TcMachineCandidate candidate : candidateMap.values()) {
                    candidate.getSidewallSpeedMap().putIfAbsent(speed.getSidewallCode(), speed.getProductSpeed());
                }
                continue;
            }
            TcMachineCandidate candidate = candidateMap.get(speed.getMachineCode());
            if (candidate == null) {
                continue;
            }
            if (StrUtil.isBlank(speed.getSidewallCode())) {
                candidate.setMachineSpeed(speed.getProductSpeed());
            } else {
                candidate.getSidewallSpeedMap().put(speed.getSidewallCode(), speed.getProductSpeed());
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
    private void fillCandidateMaintenance(TcScheduleContext context, Map<String, TcMachineCandidate> candidateMap,
                                          List<TcShiftConfig> shiftConfigList) {
        if (tmMachineMaintenanceMapper == null || candidateMap.isEmpty()) {
            return;
        }
        Map<Integer, Date[]> shiftWindowMap = this.buildShiftWindowMap(context, shiftConfigList);
        if (CollUtil.isNotEmpty(shiftWindowMap)) {
            candidateMap.values().forEach(candidate -> shiftWindowMap.keySet()
                    .forEach(shiftOrder -> candidate.getMaintenanceHoursByShift().put(shiftOrder, BigDecimal.ZERO)));
        }
        LambdaQueryWrapper<TcMachineMaintenance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcMachineMaintenance::getFactoryCode, context.getFactoryCode());
        wrapper.in(TcMachineMaintenance::getMachineCode, candidateMap.keySet());
        List<TcMachineMaintenance> maintenanceList = tmMachineMaintenanceMapper.selectList(wrapper);
        Date dayStart = DateUtil.beginOfDay(context.getScheduleDate());
        Date dayEnd = DateUtil.endOfDay(context.getScheduleDate());
        for (TcMachineMaintenance maintenance : nullToEmpty(maintenanceList)) {
            TcMachineCandidate candidate = candidateMap.get(maintenance.getMachineCode());
            Date maintenanceStartTime = maintenance.getStopStartTime();
            Date maintenanceEndTime = maintenance.getStopEndTime();
            if (candidate == null || maintenanceStartTime == null || maintenanceEndTime == null) {
                continue;
            }
            BigDecimal dayHours = this.calculateOverlapHours(maintenanceStartTime, maintenanceEndTime,
                    dayStart, dayEnd);
            candidate.setMaintenanceHours(nvl(candidate.getMaintenanceHours()).add(dayHours));
            if (CollUtil.isEmpty(shiftWindowMap)) {
                continue;
            }
            for (Map.Entry<Integer, Date[]> entry : shiftWindowMap.entrySet()) {
                Date[] shiftWindow = entry.getValue();
                BigDecimal shiftHours = this.calculateOverlapHours(maintenanceStartTime,
                        maintenanceEndTime, shiftWindow[0], shiftWindow[1]);
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
    private void fillCandidatePredecessor(TcScheduleContext context, Map<String, TcMachineCandidate> candidateMap) {
        if (tcScheduleResultMapper == null || candidateMap.isEmpty()) {
            context.setMachinePredecessorMap(Collections.emptyMap());
            return;
        }
        Date previousDate = DateUtil.offsetDay(DateUtil.beginOfDay(context.getScheduleDate()), -1);
        LambdaQueryWrapper<TcScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcScheduleResult::getFactoryCode, context.getFactoryCode());
        wrapper.eq(TcScheduleResult::getScheduleDate, previousDate);
        wrapper.in(TcScheduleResult::getMachineCode, candidateMap.keySet());
        List<TcScheduleResult> resultList = Optional.ofNullable(tcScheduleResultMapper.selectList(wrapper))
                .orElse(Collections.emptyList());
        Map<String, TcTaskPredecessor> predecessorMap = new LinkedHashMap<>();
        for (TcScheduleResult result : resultList) {
            if (result == null || StrUtil.isBlank(result.getMachineCode())) {
                continue;
            }
            TcTaskPredecessor predecessor = this.resolveLatestPredecessor(result);
            if (predecessor == null) {
                continue;
            }
            TcTaskPredecessor exists = predecessorMap.get(predecessor.getMachineCode());
            if (exists == null || this.isLaterPredecessor(predecessor, exists)) {
                predecessorMap.put(predecessor.getMachineCode(), predecessor);
            }
        }
        for (Map.Entry<String, TcTaskPredecessor> entry : predecessorMap.entrySet()) {
            TcMachineCandidate candidate = candidateMap.get(entry.getKey());
            if (candidate == null) {
                continue;
            }
            TcTaskPredecessor predecessor = entry.getValue();
            candidate.setTailMainGlueCode(predecessor.getGlueCode());
            candidate.setTailBaseGlueCode(predecessor.getBaseGlueCode());
            candidate.setTailMouthPlateCode(predecessor.getMouthPlateCode());
        }
        context.setMachinePredecessorMap(predecessorMap);
        log.info("[TC_MACHINE_PREDECESSOR_LOAD] factoryCode={}, scheduleDate={}, previousDate={}, predecessorCount={}, predecessors={}",
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
    private Map<Integer, Date[]> buildShiftWindowMap(TcScheduleContext context, List<TcShiftConfig> shiftConfigList) {
        Map<Integer, Date[]> shiftWindowMap = new LinkedHashMap<>();
        if (context == null || context.getScheduleDate() == null || CollUtil.isEmpty(shiftConfigList)) {
            return shiftWindowMap;
        }
        String scheduleDateText = DateUtil.formatDate(context.getScheduleDate());
        Date previousEndTime = null;
        for (TcShiftConfig config : shiftConfigList) {
            if (config == null || config.getShiftOrder() == null
                    || StrUtil.isBlank(config.getPlanStartTime()) || StrUtil.isBlank(config.getPlanEndTime())) {
                continue;
            }
            try {
                Date startTime = DateUtil.parse(scheduleDateText + " " + config.getPlanStartTime());
                Date endTime = DateUtil.parse(scheduleDateText + " " + config.getPlanEndTime());
            if (TcYesNoEnum.YES.getCode().equals(config.getCrossDayFlag()) || !endTime.after(startTime)) {
                    endTime = DateUtil.offsetDay(endTime, 1);
                }
                while (previousEndTime != null && startTime.before(previousEndTime)) {
                    startTime = DateUtil.offsetDay(startTime, 1);
                    endTime = DateUtil.offsetDay(endTime, 1);
                }
                shiftWindowMap.put(config.getShiftOrder(), new Date[]{startTime, endTime});
                previousEndTime = endTime;
            } catch (Exception exception) {
                log.warn("[TC_SHIFT_WINDOW_PARSE_FAIL] factoryCode={}, scheduleDate={}, shiftOrder={}, startTime={}, endTime={}",
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
                .divide(BigDecimal.valueOf(TcScheduleConstants.MINUTES_PER_HOUR),
                        TcScheduleConstants.DECIMAL_CALCULATION_SCALE, RoundingMode.HALF_UP);
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
    private List<TcShiftConfig> loadOpenShiftConfigs(TcScheduleContext context) {
        if (tmShiftConfigMapper == null) {
            return Collections.emptyList();
        }
        List<TcShiftConfig> configs = tmShiftConfigMapper.selectList(
                new LambdaQueryWrapper<TcShiftConfig>()
                        .eq(TcShiftConfig::getFactoryCode, context.getFactoryCode()));
        return nullToEmpty(configs).stream()
                .filter(config -> config.getShiftOrder() != null)
                .sorted(Comparator.comparing(TcShiftConfig::getShiftOrder))
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
    private void fillShiftHoursMap(TcScheduleContext context, List<TcShiftConfig> shiftConfigList) {
        Map<Integer, BigDecimal> shiftHoursMap = new HashMap<>();
        Map<Integer, TcShiftTimeWindow> shiftTimeWindowMap = new HashMap<>();
        if (CollUtil.isEmpty(shiftConfigList)) {
            context.setShiftHoursMap(shiftHoursMap);
            context.setShiftTimeWindowMap(shiftTimeWindowMap);
            return;
        }
        for (TcShiftConfig config : shiftConfigList) {
            if (config.getShiftOrder() == null) {
                continue;
            }
            if (config.getShiftHours() != null && config.getShiftHours() > 0) {
                shiftHoursMap.put(config.getShiftOrder(), BigDecimal.valueOf(config.getShiftHours()));
            }
            TcShiftTimeWindow window = new TcShiftTimeWindow();
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
    private TcTaskPredecessor resolveLatestPredecessor(TcScheduleResult result) {
        for (int shiftOrder = 6; shiftOrder >= 1; shiftOrder--) {
            BigDecimal planQty = this.toBigDecimal(result.getFieldValueByFieldName(
                    String.format("class%dPlanQty", shiftOrder)));
            Integer sequence = this.toInteger(result.getFieldValueByFieldName(
                    String.format("class%dSequence", shiftOrder)));
            if (planQty.compareTo(BigDecimal.ZERO) <= 0 || sequence == null) {
                continue;
            }
            TcTaskPredecessor predecessor = new TcTaskPredecessor();
            predecessor.setMachineCode(result.getMachineCode());
            predecessor.setSidewallCode(result.getSidewallCode());
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
    private boolean isLaterPredecessor(TcTaskPredecessor candidatePredecessor,
                                       TcTaskPredecessor currentPredecessor) {
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
     * 成型计划已加载但按 TC_FORMING_SHIFT_OFFSET 偏移后无可排程班次时，记录细化提示到上下文。
     *
     * <p>偏移后胎侧班次 N 读取成型 CLASS(N+offset)，6 班覆盖 class(offset+1)~class(6+offset)，
     * 统一夹到 CLASS1~CLASS8 区间；仅当成型行数大于 0 且区间有效时写入，供响应阶段优先展示。</p>
     *
     * @param context            自动排程上下文
     * @param formingRowCount    已加载的成型计划行数
     * @param formingShiftOffset 成型班次偏移量
     */
    private void recordEmptyFormingTaskMessage(TcScheduleContext context, int formingRowCount, int formingShiftOffset) {
        if (formingRowCount <= 0) {
            return;
        }
        // CLASS1~CLASS8 共 8 个班次，偏移后区间夹到 [1,8]
        int clampedFirst = Math.max(1, Math.min(formingShiftOffset + 1, 8));
        int clampedLast = Math.max(1, Math.min(TcScheduleConstants.TC_MAX_SHIFT_ORDER + formingShiftOffset, 8));
        if (clampedFirst > clampedLast) {
            return;
        }
        String template = I18nUtil.getMessage("ui.tc.schedule.noTaskGeneratedWithOffset");
        if (StrUtil.isBlank(template) || "ui.tc.schedule.noTaskGeneratedWithOffset".equals(template)) {
            template = "胎侧自动排程未生成结果：成型计划已加载 {0} 行，但按当前 TC_FORMING_SHIFT_OFFSET 偏移后无可排程班次，偏移后班次：class{1}-class{2}，请确认成型排程班次与 TC_FORMING_SHIFT_OFFSET 配置是否配套";
        }
        context.setEmptyFormingTaskMessage(MessageFormat.format(template, formingRowCount, clampedFirst, clampedLast));
    }

    /**
     * 成型班次有需求但示方书为空或未命中施工时，记录未生成任务的准确提示。
     *
     * @param context                  自动排程上下文
     * @param formingRowCount          已加载的成型计划行数
     * @param constructionMissingCount 因示方书为空或未命中有效施工而跳过的排程班次数
     */
    private void recordConstructionMissingTaskMessage(TcScheduleContext context, int formingRowCount,
                                                      int constructionMissingCount) {
        if (formingRowCount <= 0 || constructionMissingCount <= 0) {
            return;
        }
        String template = I18nUtil.getMessage("ui.tc.schedule.noTaskGeneratedWithConstructionMissing");
        if (StrUtil.isBlank(template)
                || "ui.tc.schedule.noTaskGeneratedWithConstructionMissing".equals(template)) {
            template = "胎侧自动排程未生成结果：成型计划已加载 {0} 行，但偏移后有需求的 {1} 个排程班次未匹配到有效施工，请确认成型计划示方书与施工信息的胎胚编码、施工版本是否匹配";
        }
        context.setEmptyFormingTaskMessage(MessageFormat.format(template, formingRowCount,
                constructionMissingCount));
    }

    /**
     * 从成型计划和施工信息构造胎侧待排任务。
     *
     * <p>按参数 {@code TC_VERSION_MATCH_MODE} 分流：{@code RECIPE}（默认）走逐班示方书版本解析，
     * {@code BOM} 走 {@code BOM_DATA_VERSION} 关联逻辑。仅识别 {@code RECIPE}/{@code BOM},
     * 其他值(含早期内部 {@code B})由 {@link TcVersionMatchModeEnum#resolve} 回退为 {@code RECIPE}。</p>
     *
     * @param context     自动排程上下文
     * @param machineList 胎侧机台列表
     * @return 胎侧待排任务列表
     */
    private List<TcTaskDraft> loadFormingDemandTasks(TcScheduleContext context, List<TcMachineInfo> machineList) {
        String versionMatchMode = getParamValue(context, TcScheduleConstants.PARAM_VERSION_MATCH_MODE,
                TcScheduleConstants.DEFAULT_VERSION_MATCH_MODE);
        log.info("[TC_BOOTSTRAP_DETAIL] factoryCode={}, scheduleDate={} 版本匹配模式={}",
                context.getFactoryCode(), DateUtil.formatDate(context.getScheduleDate()), versionMatchMode);
        if (TcVersionMatchModeEnum.BOM == TcVersionMatchModeEnum.resolve(versionMatchMode)) {
            return loadFormingDemandTasksByBom(context, machineList);
        }
        return loadFormingDemandTasksByRecipe(context, machineList);
    }

    /**
     * BOM 模式：原 BOM_DATA_VERSION 关联逻辑，一行成型对应一套胎侧属性、6 班共用。
     *
     * @param context     自动排程上下文
     * @param machineList 胎侧机台列表
     * @return 胎侧待排任务列表
     */
    private List<TcTaskDraft> loadFormingDemandTasksByBom(TcScheduleContext context, List<TcMachineInfo> machineList) {
        List<TcFormingDemandRowVo> rowList;
        try {
            rowList = tcAutoScheduleDataLoadMapper.selectFormingDemandRows(context.getFactoryCode(), context.getScheduleDate());
        } catch (RuntimeException ex) {
            log.warn("[TC_AUTO_SCHEDULE_LOAD] 加载成型计划和施工信息失败，scheduleDate={}，原因={}",
                    DateUtil.formatDate(context.getScheduleDate()), ex.getMessage());
            return Collections.emptyList();
        }
        if (rowList == null) {
            rowList = Collections.emptyList();
        }
        log.info("[TC_BOOTSTRAP_DETAIL] factoryCode={}, scheduleDate={} BOM模式成型计划原始行数={}",
                context.getFactoryCode(), DateUtil.formatDate(context.getScheduleDate()), rowList.size());
        // 校验成型关联施工的关键字段是否为空，收集所有有问题的规格统一提示
        Set<String> sidewallCodeEmptyList = new HashSet<>();
        Set<String> sidewallLengthEmptyList = new HashSet<>();
        Set<String> mouthPlateEmptyList = new HashSet<>();
        Set<String> rubberCategoryEmptyList = new HashSet<>();
        for (TcFormingDemandRowVo row : rowList) {
            String orderNo = row.getOrderNo();
            String embryoCode = row.getEmbryoCode();
            String sidewallCode = row.getSidewallCode();
            BigDecimal sidewallLength = nvl(row.getSidewallLength());
            String mouthPlate = row.getSidewallMouthPlate();
            String rubberCategory = row.getSidewallRubber();
            if (StrUtil.isBlank(sidewallCode)) {
                sidewallCodeEmptyList.add(embryoCode);
            }
            if (sidewallLength == null || sidewallLength.compareTo(BigDecimal.ZERO) <= 0) {
                sidewallLengthEmptyList.add(embryoCode);
            }
            if (StrUtil.isBlank(mouthPlate)) {
                mouthPlateEmptyList.add(embryoCode);
            }
            if (StrUtil.isBlank(rubberCategory)) {
                rubberCategoryEmptyList.add(embryoCode);
            }
        }
        // 统一抛出校验异常
        String errorMsg = this.buildConstructionFieldErrorMessage(sidewallCodeEmptyList,
                sidewallLengthEmptyList, mouthPlateEmptyList, rubberCategoryEmptyList);
        if (StrUtil.isNotBlank(errorMsg)) {
            context.getIssueCollector().addIssue(TcAutoScheduleIssueLevelEnum.ERROR,
                    TcScheduleStepEnum.BOOTSTRAP, TcAutoScheduleIssueCategoryEnum.CONSTRUCTION_FIELD_MISSING,
                    errorMsg);
            throw new RuntimeException(errorMsg);
        }
        // 成型来源需求不在数据加载阶段聚合，解释表需要逐条追溯原成型排程结果。
        List<TcFormingDemandRowVo> demandRowList = rowList;
        String algorithmCode = getParamValue(context, TcScheduleConstants.PARAM_ALGORITHM_SWITCH,
                TcScheduleConstants.DEFAULT_ALGORITHM_SWITCH);
        Integer alg1LookbackShifts = getPositiveIntegerParam(context,
                TcScheduleConstants.PARAM_ALG1_LOOKBACK_SHIFTS,
                TcScheduleConstants.DEFAULT_ALG1_LOOKBACK_SHIFTS_VALUE);
        BigDecimal minStartQty = getDecimalParam(context, TcScheduleConstants.PARAM_MIN_START_QTY);
        BigDecimal defaultCurlLength = getDecimalParam(context, TcScheduleConstants.PARAM_DEFAULT_CURL_LENGTH);
        BigDecimal toolTotalQty = getDecimalParam(context, TcScheduleConstants.PARAM_TOOL_TOTAL_QTY);
        Integer fallbackGuardShiftCount = getIntegerParam(context, TcScheduleConstants.PARAM_MIN_STOCK_CLASS,
                TcScheduleConstants.DEFAULT_MIN_STOCK_CLASS_VALUE);
        Integer newSpecLookbackDays = getPositiveIntegerParam(context,
                TcScheduleConstants.PARAM_NEW_SPEC_LOOKBACK_DAYS,
                TcScheduleConstants.DEFAULT_NEW_SPEC_LOOKBACK_DAYS_VALUE);
        Integer newSpecAdvanceShiftCount = getPositiveIntegerParam(context,
                TcScheduleConstants.PARAM_NEW_SPEC_ADVANCE_SHIFT_COUNT,
                TcScheduleConstants.DEFAULT_NEW_SPEC_ADVANCE_SHIFT_COUNT_VALUE);
        Integer formingShiftOffset = getNonNegativeIntegerParam(context,
                TcScheduleConstants.PARAM_FORMING_SHIFT_OFFSET,
                TcScheduleConstants.DEFAULT_FORMING_SHIFT_OFFSET_VALUE);
        Map<String, TcNewSpecInfo> newSpecInfoMap = buildNewSpecInfoMap(context, demandRowList.stream()
                .map(TcFormingDemandRowVo::getSidewallCode).filter(StrUtil::isNotBlank)
                .distinct().collect(Collectors.toList()), newSpecLookbackDays, newSpecAdvanceShiftCount);
        List<TcLossRule> lossRuleList = context.getLossRuleList();
        List<TcDepthConfig> depthConfigList = this.loadDepthConfigs(context);
        String depthMachineMatchMode = this.resolveDepthMachineMatchMode(context);
        Map<String, Set<String>> sidewallMachineCodeMap = this.buildSidewallMachineCodeMapByBom(demandRowList);
        Map<String, TcWorkCalendarRowVo> tmCalendarMap = this.loadSixShiftWorkCalendarMap(context,
                TcProcessCodeEnum.SIDEWALL.getCode());
        Map<String, TcWorkCalendarRowVo> cxCalendarMap = this.loadSixShiftWorkCalendarMap(context,
                TcProcessCodeEnum.FORMING.getCode());
        List<TcTaskDraft> taskDraftList = new ArrayList<>();
        int sourceRowIndex = 0;
        for (TcFormingDemandRowVo row : demandRowList) {
            sourceRowIndex++;
            String sidewallCode = row.getSidewallCode();
            BigDecimal sidewallLength = this.convertConstructionLengthToMeter(row.getSidewallLength());
            if (StrUtil.isBlank(sidewallCode) || sidewallLength.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal[] classQtyArray = buildClassQtyArray(row);
            BigDecimal[] originalClassQtyArray = Arrays.copyOf(classQtyArray, classQtyArray.length);
            BigDecimal totalFormingPlanQty = this.calculateTotalFormingPlanQty(originalClassQtyArray);
            boolean closeOut = this.isCloseOutByPlanSurplus(row.getCxRemainQty(), totalFormingPlanQty);
            this.logCloseOutJudge(context, row.getOrderNo(), row.getEmbryoCode(), originalClassQtyArray,
                    totalFormingPlanQty, row.getCxRemainQty(), closeOut, "BOM");
            Set<String> depthMachineCodeSet = this.resolveDepthMachineCodeSet(depthMachineMatchMode,
                    row.getLhMachineCode(),
                    sidewallMachineCodeMap.get(this.normalizeProductCode(sidewallCode)));
            Integer guardShiftCount = this.resolveGuardShiftCount(context, sidewallCode, row.getOrderNo(),
                    depthConfigList, fallbackGuardShiftCount, depthMachineMatchMode,
                    depthMachineCodeSet, this.resolveDepthMachineFieldName(depthMachineMatchMode));
            boolean noShutdownAvailableShift = this.redistributeShutdownDemand(context, classQtyArray,
                    tmCalendarMap, cxCalendarMap);
            for (int shiftOrder = 1; shiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER + 1; shiftOrder++) {
                int currentDemandStartIndex = this.resolveCurrentDemandStartIndex(shiftOrder, formingShiftOffset);
                BigDecimal formingQty = this.resolveCurrentShiftFormingQty(classQtyArray, shiftOrder, algorithmCode,
                        formingShiftOffset, alg1LookbackShifts);
                BigDecimal demandQty = formingQty.multiply(sidewallLength);
                if (demandQty.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                TcNewSpecInfo taskNewSpecInfo = buildTaskNewSpecInfo(newSpecInfoMap.get(sidewallCode), shiftOrder, demandQty);
                int effectiveGuardShiftCount = this.resolveEffectiveGuardShiftCount(taskNewSpecInfo,
                        guardShiftCount, shiftOrder, formingShiftOffset, algorithmCode);
                int targetShiftOrder = resolveTargetShiftOrder(taskNewSpecInfo, shiftOrder);
                TcTaskDraft taskDraft = new TcTaskDraft();
                taskDraft.setOrderNo(row.getOrderNo() + "-CLASS" + shiftOrder);
                taskDraft.setSourceOrderNos(row.getOrderNo());
                taskDraft.setEmbryoCode(row.getEmbryoCode());
                taskDraft.setCxMachineCode(row.getCxMachineCode());
                taskDraft.setBusinessKeySuffix(buildSourceTaskBusinessKeySuffix(row, sourceRowIndex, shiftOrder));
                taskDraft.setSidewallCode(sidewallCode);
                taskDraft.setConstructionVersion(row.getConstructionVersion());
                taskDraft.setSidewallCraft(row.getSidewallCraft());
                taskDraft.setSidewallWeight(row.getSidewallWeight());
                taskDraft.setSidewallWearpRubberWeight(row.getSidewallWearpRubberWeight());
                // 第一个非空分段作为主胶，其余非空分段按原顺序组成基部胶。
                this.fillRubberCodes(taskDraft, row.getSidewallRubber());
                taskDraft.setSmallGlueFlag(this.isSmallGlueCode(context, taskDraft.getGlueCode()));
                taskDraft.setMouthPlateCode(row.getSidewallMouthPlate());
                taskDraft.setShiftOrder(targetShiftOrder);
                taskDraft.setSourceShiftOrder(shiftOrder);
                if (currentDemandStartIndex >= 0 && currentDemandStartIndex < originalClassQtyArray.length) {
                    taskDraft.setFormingLogicalShiftOrder(currentDemandStartIndex + 1);
                    taskDraft.setFormingShutdownCloseOutDemandQty(
                            this.readClassQty(originalClassQtyArray, currentDemandStartIndex).multiply(sidewallLength));
                }
                taskDraft.setNewSpecInfo(taskNewSpecInfo);
                taskDraft.setSidewallLength(sidewallLength);
                taskDraft.setTailFlag(closeOut ? TcYesNoEnum.YES.getCode() : TcYesNoEnum.NO.getCode());
                taskDraft.setTailBalanceQty(nvl(row.getCxRemainQty()));
                taskDraft.setCurrentShiftDemandQty(demandQty);
                taskDraft.setOriginalCurrentShiftDemandQty(demandQty);
                BigDecimal rawGuardFormingQty = this.calculateGuardFormingQty(classQtyArray, shiftOrder,
                        effectiveGuardShiftCount, formingShiftOffset, algorithmCode);
                BigDecimal cappedGuardFormingQty = this.capGuardFormingQty(rawGuardFormingQty,
                        row.getLhRemainQty());
                taskDraft.setGuardDemandQty(cappedGuardFormingQty.multiply(sidewallLength));
                Map<Integer, BigDecimal> formingGuardWindowQtyMap = this.buildGuardWindowByBom(classQtyArray,
                        shiftOrder, effectiveGuardShiftCount, formingShiftOffset, algorithmCode, sidewallLength,
                        cappedGuardFormingQty);
                taskDraft.setFormingGuardWindowQtyMap(this.buildSupplyWindowQtyMap(shiftOrder, formingShiftOffset,
                        demandQty, formingGuardWindowQtyMap));
                taskDraft.setDemandQty(demandQty);
                taskDraft.setGuardShiftCount(effectiveGuardShiftCount);
                this.addDepthConfigMatchTrace(context, taskDraft, sidewallCode, depthMachineMatchMode,
                        this.resolveDepthMachineFieldName(depthMachineMatchMode), depthMachineCodeSet,
                        guardShiftCount, depthConfigList, fallbackGuardShiftCount);
                this.addCloseOutJudgeTrace(context, taskDraft, originalClassQtyArray, totalFormingPlanQty,
                        row.getCxRemainQty(), "BOM");
                this.addGuardDemandEstimateTrace(context, taskDraft, classQtyArray, shiftOrder,
                        effectiveGuardShiftCount,
                        formingShiftOffset, algorithmCode, row.getLhRemainQty(), rawGuardFormingQty, cappedGuardFormingQty, "BOM");
                this.fillGuardRangeHours(context, taskDraft, shiftOrder, effectiveGuardShiftCount, formingShiftOffset,
                        algorithmCode);
                taskDraft.setMinStartQty(minStartQty);
                taskDraft.setDefaultCurlRollLength(defaultCurlLength);
                if (toolTotalQty.compareTo(BigDecimal.ZERO) > 0) {
                    taskDraft.setTotalToolQty(toolTotalQty);
                }
                if (noShutdownAvailableShift
                        && !this.isSixShiftOpen(context, tmCalendarMap, targetShiftOrder)
                        && this.isSixShiftOpen(context, cxCalendarMap, shiftOrder)) {
                    taskDraft.setUnplannedReasonCode(TcUnplannedReasonEnum.TC_SHUTDOWN_NO_AVAILABLE_SHIFT.getCode());
                    taskDraft.setUnplannedReasonDesc(TcUnplannedReasonEnum.TC_SHUTDOWN_NO_AVAILABLE_SHIFT.getDesc());
                }
                this.addVersionMatchTrace(context, taskDraft, "BOM", row.getBomDataVersion(),
                        row.getConstructionVersion(), !Objects.equals(row.getBomDataVersion(), row.getConstructionVersion()));
                taskDraftList.add(taskDraft);
            }
        }
        taskDraftList = this.prepareTwoShiftDemandTasks(taskDraftList);
        appendExperimentSpecTasks(context, taskDraftList, lossRuleList, minStartQty, defaultCurlLength, toolTotalQty);
        log.info("[TC_BOOTSTRAP_DETAIL] factoryCode={}, scheduleDate={} BOM模式任务生成汇总：成型行数={}，生成任务={}",
                context.getFactoryCode(), DateUtil.formatDate(context.getScheduleDate()),
                rowList.size(), taskDraftList.size());
        if (taskDraftList.isEmpty()) {
            this.recordEmptyFormingTaskMessage(context, rowList.size(), formingShiftOffset);
        }
        return taskDraftList;
    }

    /**
     * RECIPE 模式：按 CD90 式逐班示方书版本解析施工，同一成型行不同班次可对应不同胎侧规格。
     *
     * <p>分两阶段加载：先查成型排程结果（含 CLASS1~8_RECIPE_NO），再按 (EMBRYO_CODE, CONSTRUCTION_VERSION)
     * 批量查施工胎侧属性，Java 中按 (embryoCode, classNRecipeNo) 逐班关联。某班次示方书为空或未命中施工时
     * 记 warn 跳过该班次，不抛异常（CD90 风格）。</p>
     *
     * @param context     自动排程上下文
     * @param machineList 胎侧机台列表
     * @return 胎侧待排任务列表
     */
    private List<TcTaskDraft> loadFormingDemandTasksByRecipe(TcScheduleContext context, List<TcMachineInfo> machineList) {
        List<TcFormingDemandRecipeRowVo> rowList;
        try {
            rowList = tcAutoScheduleDataLoadMapper.selectFormingDemandRowsByRecipe(
                    context.getFactoryCode(), context.getScheduleDate());
        } catch (RuntimeException ex) {
            log.warn("[TC_AUTO_SCHEDULE_LOAD] RECIPE 模式加载成型计划失败，scheduleDate={}，原因={}",
                    DateUtil.formatDate(context.getScheduleDate()), ex.getMessage());
            return Collections.emptyList();
        }
        if (CollUtil.isEmpty(rowList)) {
            log.warn("[TC_BOOTSTRAP_DETAIL] factoryCode={}, scheduleDate={} RECIPE模式查询成型计划结果为空，无排程任务可生成",
                    context.getFactoryCode(), DateUtil.formatDate(context.getScheduleDate()));
            return Collections.emptyList();
        }
        log.info("[TC_BOOTSTRAP_DETAIL] factoryCode={}, scheduleDate={} RECIPE模式成型计划原始行数={}",
                context.getFactoryCode(), DateUtil.formatDate(context.getScheduleDate()), rowList.size());
        // 收集胎胚代码与所有班次示方书版本，批量加载施工胎侧属性
        Set<String> embryoCodes = rowList.stream().map(TcFormingDemandRecipeRowVo::getEmbryoCode)
                .filter(StrUtil::isNotBlank).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> recipeVersions = new LinkedHashSet<>();
        for (TcFormingDemandRecipeRowVo row : rowList) {
            for (String recipeNo : buildRecipeNoArray(row)) {
                if (StrUtil.isNotBlank(recipeNo)) {
                    recipeVersions.add(recipeNo.trim());
                }
            }
        }
        List<TcConstructionSidewallRowVo> constructionList;
        if (embryoCodes.isEmpty() || recipeVersions.isEmpty()) {
            log.warn("[TC_RECIPE_MATCH] factoryCode={}, scheduleDate={} 成型计划未提供胎胚代码或示方书版本，跳过施工解析",
                    context.getFactoryCode(), DateUtil.formatDate(context.getScheduleDate()));
            constructionList = Collections.emptyList();
        } else {
            constructionList = tcAutoScheduleDataLoadMapper.selectConstructionInfoRows(
                    context.getFactoryCode(), embryoCodes, recipeVersions);
        }
        Map<String, TcConstructionSidewallRowVo> constructionMap = new HashMap<>();
        for (TcConstructionSidewallRowVo construction : nullToEmpty(constructionList)) {
            if (construction == null || StrUtil.isBlank(construction.getConstructionCode())
                    || StrUtil.isBlank(construction.getConstructionVersion())) {
                continue;
            }
            constructionMap.putIfAbsent(construction.getConstructionCode() + "|" + construction.getConstructionVersion(),
                    construction);
        }
        log.info("[TC_BOOTSTRAP_DETAIL] factoryCode={}, scheduleDate={} RECIPE模式施工胎侧属性：胚编码数={}，示方书版本数={}，命中施工行数={}，构造映射数={}",
                context.getFactoryCode(), DateUtil.formatDate(context.getScheduleDate()),
                embryoCodes.size(), recipeVersions.size(),
                nullToEmpty(constructionList).size(), constructionMap.size());
        // 参数与基础数据
        String algorithmCode = getParamValue(context, TcScheduleConstants.PARAM_ALGORITHM_SWITCH,
                TcScheduleConstants.DEFAULT_ALGORITHM_SWITCH);
        Integer alg1LookbackShifts = getPositiveIntegerParam(context,
                TcScheduleConstants.PARAM_ALG1_LOOKBACK_SHIFTS,
                TcScheduleConstants.DEFAULT_ALG1_LOOKBACK_SHIFTS_VALUE);
        BigDecimal minStartQty = getDecimalParam(context, TcScheduleConstants.PARAM_MIN_START_QTY);
        BigDecimal defaultCurlLength = getDecimalParam(context, TcScheduleConstants.PARAM_DEFAULT_CURL_LENGTH);
        BigDecimal toolTotalQty = getDecimalParam(context, TcScheduleConstants.PARAM_TOOL_TOTAL_QTY);
        Integer fallbackGuardShiftCount = getIntegerParam(context, TcScheduleConstants.PARAM_MIN_STOCK_CLASS,
                TcScheduleConstants.DEFAULT_MIN_STOCK_CLASS_VALUE);
        Integer newSpecLookbackDays = getPositiveIntegerParam(context,
                TcScheduleConstants.PARAM_NEW_SPEC_LOOKBACK_DAYS,
                TcScheduleConstants.DEFAULT_NEW_SPEC_LOOKBACK_DAYS_VALUE);
        Integer newSpecAdvanceShiftCount = getPositiveIntegerParam(context,
                TcScheduleConstants.PARAM_NEW_SPEC_ADVANCE_SHIFT_COUNT,
                TcScheduleConstants.DEFAULT_NEW_SPEC_ADVANCE_SHIFT_COUNT_VALUE);
        Integer formingShiftOffset = getNonNegativeIntegerParam(context,
                TcScheduleConstants.PARAM_FORMING_SHIFT_OFFSET,
                TcScheduleConstants.DEFAULT_FORMING_SHIFT_OFFSET_VALUE);
        List<TcLossRule> lossRuleList = context.getLossRuleList();
        List<TcDepthConfig> depthConfigList = this.loadDepthConfigs(context);
        Map<String, TcWorkCalendarRowVo> tmCalendarMap = this.loadSixShiftWorkCalendarMap(context,
                TcProcessCodeEnum.SIDEWALL.getCode());
        Map<String, TcWorkCalendarRowVo> cxCalendarMap = this.loadSixShiftWorkCalendarMap(context,
                TcProcessCodeEnum.FORMING.getCode());

        // 预解析每行各班次施工规格，并收集有效胎侧编码用于新规格判断
        List<BigDecimal[]> classQtyArrayList = new ArrayList<>();
        List<TcConstructionSidewallRowVo[]> specByClassList = new ArrayList<>();
        Set<String> allSidewallCodes = new LinkedHashSet<>();
        Set<String> sidewallCodeEmptyList = new LinkedHashSet<>();
        Set<String> sidewallLengthEmptyList = new LinkedHashSet<>();
        Set<String> mouthPlateEmptyList = new LinkedHashSet<>();
        Set<String> rubberCategoryEmptyList = new LinkedHashSet<>();
        for (TcFormingDemandRecipeRowVo row : rowList) {
            BigDecimal[] classQtyArray = buildClassQtyArrayByRecipe(row);
            classQtyArrayList.add(classQtyArray);
            String[] recipeNoByClass = buildRecipeNoArray(row);
            TcConstructionSidewallRowVo[] specByClass = new TcConstructionSidewallRowVo[8];
            for (int i = 0; i < 8; i++) {
                String recipeNo = recipeNoByClass[i];
                if (StrUtil.isBlank(recipeNo)) {
                    continue;
                }
                TcConstructionSidewallRowVo spec = constructionMap.get(row.getEmbryoCode() + "|" + recipeNo.trim());
                specByClass[i] = spec;
                if (spec == null || readClassQty(classQtyArray, i).compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                String sourceKey = StrUtil.blankToDefault(row.getEmbryoCode(), row.getOrderNo()) + "/" + recipeNo.trim();
                if (StrUtil.isBlank(spec.getSidewallCode())) {
                    sidewallCodeEmptyList.add(sourceKey);
                }
                if (nvl(spec.getSidewallLength()).compareTo(BigDecimal.ZERO) <= 0) {
                    sidewallLengthEmptyList.add(sourceKey);
                }
                if (StrUtil.isBlank(spec.getSidewallMouthPlate())) {
                    mouthPlateEmptyList.add(sourceKey);
                }
                if (StrUtil.isBlank(spec.getSidewallRubber())) {
                    rubberCategoryEmptyList.add(sourceKey);
                }
                if (StrUtil.isNotBlank(spec.getSidewallCode())
                        && nvl(spec.getSidewallLength()).compareTo(BigDecimal.ZERO) > 0) {
                    allSidewallCodes.add(spec.getSidewallCode());
                }
            }
            specByClassList.add(specByClass);
        }
        String errorMsg = this.buildConstructionFieldErrorMessage(sidewallCodeEmptyList,
                sidewallLengthEmptyList, mouthPlateEmptyList, rubberCategoryEmptyList);
        if (StrUtil.isNotBlank(errorMsg)) {
            context.getIssueCollector().addIssue(TcAutoScheduleIssueLevelEnum.ERROR,
                    TcScheduleStepEnum.BOOTSTRAP, TcAutoScheduleIssueCategoryEnum.CONSTRUCTION_FIELD_MISSING,
                    errorMsg);
            throw new RuntimeException(errorMsg);
        }
        Map<String, TcNewSpecInfo> newSpecInfoMap = buildNewSpecInfoMap(context, allSidewallCodes,
                newSpecLookbackDays, newSpecAdvanceShiftCount);
        String depthMachineMatchMode = this.resolveDepthMachineMatchMode(context);
        Map<String, Set<String>> sidewallMachineCodeMap = this.buildSidewallMachineCodeMapByRecipe(rowList,
                classQtyArrayList, specByClassList);

        // 逐行逐班次生成任务
        List<TcTaskDraft> taskDraftList = new ArrayList<>();
        int sourceRowIndex = 0;
        int skippedShiftNoFormingQty = 0;
        int skippedShiftNoSpec = 0;
        int skippedShiftNoDemand = 0;
        for (int rowIdx = 0; rowIdx < rowList.size(); rowIdx++) {
            TcFormingDemandRecipeRowVo row = rowList.get(rowIdx);
            sourceRowIndex++;
            BigDecimal[] classQtyArray = classQtyArrayList.get(rowIdx);
            BigDecimal[] originalClassQtyArray = Arrays.copyOf(classQtyArray, classQtyArray.length);
            BigDecimal totalFormingPlanQty = this.calculateTotalFormingPlanQty(originalClassQtyArray);
            boolean closeOut = this.isCloseOutByPlanSurplus(row.getCxRemainQty(), totalFormingPlanQty);
            this.logCloseOutJudge(context, row.getOrderNo(), row.getEmbryoCode(), originalClassQtyArray,
                    totalFormingPlanQty, row.getCxRemainQty(), closeOut, "RECIPE");
            TcConstructionSidewallRowVo[] specByClass = specByClassList.get(rowIdx);
            this.recordAllPlannedShiftConstructionMissingIssue(context, row, classQtyArray, specByClass);
            Integer rowGuardShiftCount = null;
            if (TcScheduleConstants.DEPTH_MACHINE_MATCH_MODE_ROW.equals(depthMachineMatchMode)) {
                rowGuardShiftCount = this.resolveGuardShiftCount(context, null, row.getOrderNo(),
                        depthConfigList, fallbackGuardShiftCount,
                        TcScheduleConstants.DEPTH_MACHINE_MATCH_MODE_ROW,
                        this.resolveDepthMachineCodeSet(TcScheduleConstants.DEPTH_MACHINE_MATCH_MODE_ROW,
                                row.getLhMachineCode(), Collections.emptySet()),
                        "LH_MACHINE_CODE");
            }
            boolean noShutdownAvailableShift = this.redistributeShutdownDemand(context, classQtyArray,
                    tmCalendarMap, cxCalendarMap);
            for (int shiftOrder = 1; shiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER + 1; shiftOrder++) {
                BigDecimal formingQty = this.resolveCurrentShiftFormingQty(classQtyArray, shiftOrder, algorithmCode,
                        formingShiftOffset, alg1LookbackShifts);
                if (formingQty.compareTo(BigDecimal.ZERO) <= 0) {
                    skippedShiftNoFormingQty++;
                    continue;
                }
                int startIndex = this.resolveCurrentDemandStartIndex(shiftOrder, formingShiftOffset);
                int primarySpecIndex = Math.min(startIndex, 7);
                TcConstructionSidewallRowVo primarySpec = (primarySpecIndex >= 0 && primarySpecIndex < 8)
                        ? specByClass[primarySpecIndex] : null;
                if (primarySpec == null || StrUtil.isBlank(primarySpec.getSidewallCode())
                        || nvl(primarySpec.getSidewallLength()).compareTo(BigDecimal.ZERO) <= 0) {
                    String missingReason = primarySpec == null ? "示方书为空或未命中施工" : "施工胎侧编码或肩长无效";
                    log.warn("[TC_RECIPE_MATCH] 跳过班次：factoryCode={}, orderNo={}, embryoCode={}, shiftOrder={}, startIndex={}, formingQty={}, 原因={}",
                            context.getFactoryCode(), row.getOrderNo(), row.getEmbryoCode(), shiftOrder, startIndex, formingQty,
                            missingReason);
                    skippedShiftNoSpec++;
                    continue;
                }
                String sidewallCode = primarySpec.getSidewallCode();
                BigDecimal sidewallLength = this.convertConstructionLengthToMeter(primarySpec.getSidewallLength());
                BigDecimal demandQty = formingQty.multiply(sidewallLength);
                if (demandQty.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                TcNewSpecInfo taskNewSpecInfo = buildTaskNewSpecInfo(newSpecInfoMap.get(sidewallCode), shiftOrder, demandQty);
                Set<String> depthMachineCodeSet = this.resolveDepthMachineCodeSet(depthMachineMatchMode,
                        row.getLhMachineCode(),
                        sidewallMachineCodeMap.get(this.normalizeProductCode(sidewallCode)));
                Integer guardShiftCount = TcScheduleConstants.DEPTH_MACHINE_MATCH_MODE_CODE.equals(depthMachineMatchMode)
                        ? this.resolveGuardShiftCount(context, sidewallCode, row.getOrderNo(),
                        depthConfigList, fallbackGuardShiftCount, depthMachineMatchMode, depthMachineCodeSet,
                        this.resolveDepthMachineFieldName(depthMachineMatchMode)) : rowGuardShiftCount;
                int effectiveGuardShiftCount = this.resolveEffectiveGuardShiftCount(taskNewSpecInfo,
                        guardShiftCount, shiftOrder, formingShiftOffset, algorithmCode);
                int targetShiftOrder = resolveTargetShiftOrder(taskNewSpecInfo, shiftOrder);
                TcTaskDraft taskDraft = new TcTaskDraft();
                taskDraft.setOrderNo(row.getOrderNo() + "-CLASS" + shiftOrder);
                taskDraft.setSourceOrderNos(row.getOrderNo());
                taskDraft.setEmbryoCode(row.getEmbryoCode());
                taskDraft.setCxMachineCode(row.getCxMachineCode());
                taskDraft.setBusinessKeySuffix(buildSourceTaskBusinessKeySuffix(row, sourceRowIndex, shiftOrder));
                taskDraft.setSidewallCode(sidewallCode);
                taskDraft.setConstructionVersion(primarySpec.getSidewallVersion());
                taskDraft.setSidewallCraft(primarySpec.getSidewallCraft());
                taskDraft.setSidewallWeight(primarySpec.getSidewallWeight());
                taskDraft.setSidewallWearpRubberWeight(primarySpec.getSidewallWearpRubberWeight());
                // 第一个非空分段作为主胶，其余非空分段按原顺序组成基部胶。
                this.fillRubberCodes(taskDraft, primarySpec.getSidewallRubber());
                taskDraft.setSmallGlueFlag(this.isSmallGlueCode(context, taskDraft.getGlueCode()));
                taskDraft.setMouthPlateCode(primarySpec.getSidewallMouthPlate());
                taskDraft.setShiftOrder(targetShiftOrder);
                taskDraft.setSourceShiftOrder(shiftOrder);
                if (startIndex >= 0 && startIndex < originalClassQtyArray.length) {
                    taskDraft.setFormingLogicalShiftOrder(startIndex + 1);
                    taskDraft.setFormingShutdownCloseOutDemandQty(
                            this.readClassQty(originalClassQtyArray, startIndex).multiply(sidewallLength));
                }
                taskDraft.setNewSpecInfo(taskNewSpecInfo);
                taskDraft.setSidewallLength(sidewallLength);
                taskDraft.setTailFlag(closeOut ? TcYesNoEnum.YES.getCode() : TcYesNoEnum.NO.getCode());
                taskDraft.setTailBalanceQty(nvl(row.getCxRemainQty()));
                taskDraft.setCurrentShiftDemandQty(demandQty);
                taskDraft.setOriginalCurrentShiftDemandQty(demandQty);
                BigDecimal rawGuardFormingQty = this.calculateGuardFormingQty(classQtyArray, shiftOrder,
                        effectiveGuardShiftCount, formingShiftOffset, algorithmCode);
                BigDecimal cappedGuardFormingQty = this.capGuardFormingQty(rawGuardFormingQty,
                        row.getLhRemainQty());
                Map<Integer, BigDecimal> formingGuardWindowQtyMap = this.buildGuardWindowByRecipe(classQtyArray,
                        specByClass, shiftOrder, effectiveGuardShiftCount, formingShiftOffset, algorithmCode, sidewallLength,
                        cappedGuardFormingQty);
                taskDraft.setGuardDemandQty(formingGuardWindowQtyMap.values().stream()
                        .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
                taskDraft.setFormingGuardWindowQtyMap(this.buildSupplyWindowQtyMap(shiftOrder, formingShiftOffset,
                        demandQty, formingGuardWindowQtyMap));
                taskDraft.setDemandQty(demandQty);
                taskDraft.setGuardShiftCount(effectiveGuardShiftCount);
                this.addDepthConfigMatchTrace(context, taskDraft, sidewallCode, depthMachineMatchMode,
                        this.resolveDepthMachineFieldName(depthMachineMatchMode), depthMachineCodeSet,
                        guardShiftCount, depthConfigList, fallbackGuardShiftCount);
                this.addCloseOutJudgeTrace(context, taskDraft, originalClassQtyArray, totalFormingPlanQty,
                        row.getCxRemainQty(), "RECIPE");
                this.addGuardDemandEstimateTrace(context, taskDraft, classQtyArray, shiftOrder,
                        effectiveGuardShiftCount,
                        formingShiftOffset, algorithmCode, row.getLhRemainQty(), rawGuardFormingQty, cappedGuardFormingQty, "RECIPE");
                this.fillGuardRangeHours(context, taskDraft, shiftOrder, effectiveGuardShiftCount, formingShiftOffset,
                        algorithmCode);
                taskDraft.setMinStartQty(minStartQty);
                taskDraft.setDefaultCurlRollLength(defaultCurlLength);
                if (toolTotalQty.compareTo(BigDecimal.ZERO) > 0) {
                    taskDraft.setTotalToolQty(toolTotalQty);
                }
                if (noShutdownAvailableShift
                        && !this.isSixShiftOpen(context, tmCalendarMap, targetShiftOrder)
                        && this.isSixShiftOpen(context, cxCalendarMap, shiftOrder)) {
                    taskDraft.setUnplannedReasonCode(TcUnplannedReasonEnum.TC_SHUTDOWN_NO_AVAILABLE_SHIFT.getCode());
                    taskDraft.setUnplannedReasonDesc(TcUnplannedReasonEnum.TC_SHUTDOWN_NO_AVAILABLE_SHIFT.getDesc());
                }
                this.addVersionMatchTrace(context, taskDraft, "RECIPE",
                        primarySpec.getConstructionVersion(), primarySpec.getSidewallVersion(), Boolean.FALSE);
                taskDraftList.add(taskDraft);
            }
        }
        taskDraftList = this.prepareTwoShiftDemandTasks(taskDraftList);
        appendExperimentSpecTasks(context, taskDraftList, lossRuleList, minStartQty, defaultCurlLength, toolTotalQty);
        log.info("[TC_BOOTSTRAP_DETAIL] factoryCode={}, scheduleDate={} RECIPE模式任务生成汇总：成型行数={}，跳过(成型量=0)={}班次，跳过(示方书/施工不匹配)={}班次，生成任务={}",
                context.getFactoryCode(), DateUtil.formatDate(context.getScheduleDate()),
                rowList.size(), skippedShiftNoFormingQty, skippedShiftNoSpec, taskDraftList.size());
        if (taskDraftList.isEmpty()) {
            if (skippedShiftNoSpec > 0) {
                this.recordConstructionMissingTaskMessage(context, rowList.size(), skippedShiftNoSpec);
            } else {
                this.recordEmptyFormingTaskMessage(context, rowList.size(), formingShiftOffset);
            }
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
    private void recordAllPlannedShiftConstructionMissingIssue(TcScheduleContext context,
                                                                 TcFormingDemandRecipeRowVo row,
                                                                 BigDecimal[] classQtyArray,
                                                                 TcConstructionSidewallRowVo[] specByClass) {
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
                I18nUtil.getMessage("ui.tc.schedule.allPlannedShiftsConstructionMissing"),
                row.getOrderNo(), row.getEmbryoCode(), shiftOrders, recipeNos);
        context.getIssueCollector().addConstructionIssue(TcAutoScheduleIssueLevelEnum.WARN,
                TcAutoScheduleIssueCategoryEnum.CONSTRUCTION_MISSING,
                row.getOrderNo(), row.getEmbryoCode(), null, null, "recipeNo", message);
        log.warn("[TC_RECIPE_MATCH] 工单有计划量班次全部未命中施工：factoryCode={}, orderNo={}, embryoCode={}, plannedShiftOrders={}, recipeNos={}",
                context.getFactoryCode(), row.getOrderNo(), row.getEmbryoCode(), shiftOrders, recipeNos);
    }

    /**
     * 记录施工版本匹配或回退证据。
     *
     * @param context 排程上下文
     * @param taskDraft 任务草稿
     * @param mode 版本匹配模式
     * @param requestedVersion 请求版本
     * @param selectedVersion 实际采用的胎侧施工版本
     * @param fallback 是否发生回退
     */
    private void addVersionMatchTrace(TcScheduleContext context, TcTaskDraft taskDraft, String mode,
                                      String requestedVersion, String selectedVersion, boolean fallback) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("mode", mode);
        evidence.put("requestedVersion", requestedVersion);
        evidence.put("selectedVersion", selectedVersion);
        evidence.put("fallback", fallback);
        // 版本未精确命中而回退取最新有效记录时，任务仍正常排产，统一记 PASS 并在 evidence 保留 fallback=true，
        // 不再记 SKIP（SKIP 易误判为任务被跳过不排产，与实际行为不符）。
        context.getRuleTraceMap().computeIfAbsent(taskDraft.getBusinessKey(), key -> new TcRuleTrace())
                .addRuleHit(TcScheduleRuleCodeEnum.VERSION_MATCH,
                        TcScheduleRuleResultEnum.PASS, evidence);
    }

    /**
     * RECIPE 模式库存保证范围内的成型需求米数：按各班次实际单耗加权求和。
     *
     * <p>窗口内 CLASS1~8 乘以各班次示方书命中的胎侧长度；超过 CLASS8 的估算量使用当前任务长度。
     * 按硫化余量封顶后的条数依班次顺序消耗，确保不同施工长度下仍先按条数封顶。</p>
     *
     * @param classQtyArray   成型班次计划量数组
     * @param specByClass     各班次命中的施工胎侧属性，未命中为 null
     * @param shiftOrder      胎侧排程班次，从 1 开始
     * @param guardShiftCount 库存最低保证班数
     * @param formingShiftOffset 胎侧班次到成型班次的偏移量
     * @param algorithmCode 需求量算法编码
     * @param currentSidewallLength 当前任务胎侧长度，供超过 CLASS8 的班次换算
     * @param guardFormingQtyLimit 按硫化余量封顶后的保证需求条数
     * @return 库存保证范围内的成型需求米数合计
     */
    private BigDecimal calculateGuardDemandByRecipe(BigDecimal[] classQtyArray, TcConstructionSidewallRowVo[] specByClass,
                                                    int shiftOrder, int guardShiftCount, int formingShiftOffset,
                                                    String algorithmCode,
                                                    BigDecimal currentSidewallLength,
                                                    BigDecimal guardFormingQtyLimit) {
        return this.buildGuardWindowByRecipe(classQtyArray, specByClass, shiftOrder, guardShiftCount,
                formingShiftOffset, algorithmCode, currentSidewallLength, guardFormingQtyLimit).values().stream()
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 构建 BOM 模式成型备库窗口明细，按成型班次顺序执行硫化余量封顶。
     *
     * @param classQtyArray 成型班次计划条数
     * @param shiftOrder 胎侧排程班次
     * @param guardShiftCount 备库班数
     * @param formingShiftOffset 成型班次偏移
     * @param algorithmCode 需求量算法编码
     * @param sidewallLength 当前胎侧长度
     * @param guardFormingQtyLimit 封顶后的保证条数
     * @return 班次到换算后长度的窗口明细
     */
    private Map<Integer, BigDecimal> buildGuardWindowByBom(BigDecimal[] classQtyArray, int shiftOrder,
                                                            int guardShiftCount, int formingShiftOffset,
                                                            String algorithmCode,
                                                            BigDecimal sidewallLength,
                                                            BigDecimal guardFormingQtyLimit) {
        Map<Integer, BigDecimal> windowQtyMap = new LinkedHashMap<>();
        BigDecimal remainingGuardFormingQty = this.nvl(guardFormingQtyLimit).max(BigDecimal.ZERO);
        int startIndex = this.resolveGuardStartIndex(shiftOrder, formingShiftOffset, algorithmCode);
        int count = Math.max(guardShiftCount, 1);
        for (int index = startIndex; index < startIndex + count; index++) {
            BigDecimal formingQty = this.resolveGuardClassQty(classQtyArray, index);
            BigDecimal appliedFormingQty = formingQty.min(remainingGuardFormingQty);
            windowQtyMap.put(index + 1, appliedFormingQty.multiply(this.nvl(sidewallLength)));
            remainingGuardFormingQty = remainingGuardFormingQty.subtract(appliedFormingQty);
        }
        return windowQtyMap;
    }

    /**
     * 构建 RECIPE 模式成型备库窗口明细，按班次施工长度并按硫化余量顺序封顶。
     *
     * @param classQtyArray 成型班次计划条数
     * @param specByClass 各成型班次施工属性
     * @param shiftOrder 胎侧排程班次
     * @param guardShiftCount 备库班数
     * @param formingShiftOffset 成型班次偏移
     * @param algorithmCode 需求量算法编码
     * @param currentSidewallLength 当前胎侧长度
     * @param guardFormingQtyLimit 封顶后的保证条数
     * @return 班次到换算后长度的窗口明细
     */
    private Map<Integer, BigDecimal> buildGuardWindowByRecipe(BigDecimal[] classQtyArray,
                                                                TcConstructionSidewallRowVo[] specByClass,
                                                                int shiftOrder, int guardShiftCount,
                                                                int formingShiftOffset, String algorithmCode,
                                                                BigDecimal currentSidewallLength,
                                                                BigDecimal guardFormingQtyLimit) {
        Map<Integer, BigDecimal> windowQtyMap = new LinkedHashMap<>();
        BigDecimal remainingGuardFormingQty = this.nvl(guardFormingQtyLimit).max(BigDecimal.ZERO);
        int startIndex = this.resolveGuardStartIndex(shiftOrder, formingShiftOffset, algorithmCode);
        int count = Math.max(guardShiftCount, 1);
        for (int index = startIndex; index < startIndex + count; index++) {
            BigDecimal formingQty = this.resolveGuardClassQty(classQtyArray, index);
            BigDecimal appliedFormingQty = formingQty.min(remainingGuardFormingQty);
            BigDecimal sidewallLength = (index >= 0 && index < 8 && specByClass != null && specByClass[index] != null)
                    ? this.convertConstructionLengthToMeter(specByClass[index].getSidewallLength())
                    : this.nvl(currentSidewallLength);
            windowQtyMap.put(index + 1, appliedFormingQty.multiply(sidewallLength));
            remainingGuardFormingQty = remainingGuardFormingQty.subtract(appliedFormingQty);
        }
        return windowQtyMap;
    }

    /**
     * 构建库存供应时长使用的连续需求窗口。
     *
     * <p>需求量计算中的保证窗口必须与当班需求保持不重叠，但库存供应时长需要从当班对应的
     * 成型需求开始扣减，因此在保证窗口前补入当班需求。例如胎侧 CLASS1 对应成型 CLASS2 时，
     * 供应时长先扣成型 CLASS2，再继续扣保证窗口中的 CLASS3 及后续班次。</p>
     *
     * @param shiftOrder 胎侧排程班次
     * @param formingShiftOffset 成型班次偏移量
     * @param currentShiftDemandQty 当班对应的成型需求量
     * @param guardWindowQtyMap 不含当班需求的保证窗口
     * @return 从当班成型需求开始的有序供应时长窗口
     */
    private Map<Integer, BigDecimal> buildSupplyWindowQtyMap(int shiftOrder, int formingShiftOffset,
                                                              BigDecimal currentShiftDemandQty,
                                                              Map<Integer, BigDecimal> guardWindowQtyMap) {
        Map<Integer, BigDecimal> supplyWindowQtyMap = new LinkedHashMap<>();
        int currentFormingShiftOrder = this.resolveCurrentDemandStartIndex(shiftOrder, formingShiftOffset) + 1;
        supplyWindowQtyMap.put(currentFormingShiftOrder, this.nvl(currentShiftDemandQty));
        if (guardWindowQtyMap != null) {
            supplyWindowQtyMap.putAll(guardWindowQtyMap);
        }
        return supplyWindowQtyMap;
    }

    /**
     * 构建成型班次计划量数组（RECIPE 模式）。
     *
     * @param row 成型需求行（示方书版本模式）
     * @return 1~8 班成型计划量数组
     */
    private BigDecimal[] buildClassQtyArrayByRecipe(TcFormingDemandRecipeRowVo row) {
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
     * 构建成型班次示方书编号数组（RECIPE 模式）。
     *
     * @param row 成型需求行（示方书版本模式）
     * @return 1~8 班示方书编号数组
     */
    private String[] buildRecipeNoArray(TcFormingDemandRecipeRowVo row) {
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
     * 追加月计划定稿实验规格胎侧任务。
     *
     * @param context 自动排程上下文
     * @param taskDraftList 待排任务列表
     * @param lossRuleList 损耗配置列表
     * @param minStartQty 最小起排量
     * @param defaultCurlLength 默认卷曲长度
     * @param toolTotalQty 总工装数量
     */
    private void appendExperimentSpecTasks(TcScheduleContext context, List<TcTaskDraft> taskDraftList,
                                           List<TcLossRule> lossRuleList, BigDecimal minStartQty,
                                           BigDecimal defaultCurlLength, BigDecimal toolTotalQty) {
        Integer lookbackDays = getPositiveIntegerParam(context,
                TcScheduleConstants.PARAM_EXPERIMENT_SPEC_LOOKBACK_DAYS,
                TcScheduleConstants.DEFAULT_EXPERIMENT_SPEC_LOOKBACK_DAYS_VALUE);
        BigDecimal experimentPlanQty = getPositiveDecimalParam(context,
                TcScheduleConstants.PARAM_EXPERIMENT_SPEC_PLAN_QTY,
                BigDecimal.valueOf(TcScheduleConstants.DEFAULT_EXPERIMENT_SPEC_PLAN_QTY_VALUE));
        if (experimentPlanQty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        Date experimentPlanDate = DateUtil.offsetDay(context.getScheduleDate(), -lookbackDays);
        String dayColumn = buildExperimentDayColumn(experimentPlanDate);
        Integer yearMonth = Integer.valueOf(DateUtil.format(experimentPlanDate, "yyyyMM"));
        List<TcExperimentSpecMonthPlanRowVo> rowList;
        try {
            rowList = tcAutoScheduleDataLoadMapper.selectExperimentSpecMonthPlanRows(context.getFactoryCode(), yearMonth,
                    dayColumn, experimentPlanDate, TcConstructionStageEnum.EXPERIMENT.getCode());
        } catch (RuntimeException ex) {
            log.warn("[TC_AUTO_SCHEDULE_LOAD] 加载实验规格月计划失败，scheduleDate={}，experimentPlanDate={}，原因={}",
                    DateUtil.formatDate(context.getScheduleDate()), DateUtil.formatDate(experimentPlanDate), ex.getMessage());
            return;
        }
        List<TcExperimentSpecMonthPlanRowVo> validRows = filterExperimentSpecRows(rowList);
        if (CollUtil.isEmpty(validRows)) {
            return;
        }
        validateExperimentSpecRows(validRows);
        Map<String, List<TcExperimentSpecMonthPlanRowVo>> rowMap = validRows.stream()
                .collect(Collectors.groupingBy(TcExperimentSpecMonthPlanRowVo::getSidewallCode, LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<String, List<TcExperimentSpecMonthPlanRowVo>> entry : rowMap.entrySet()) {
            List<TcExperimentSpecMonthPlanRowVo> sidewallRows = entry.getValue();
            TcExperimentSpecInfo experimentSpecInfo = buildExperimentSpecInfo(context, sidewallRows, lookbackDays,
                    experimentPlanQty, experimentPlanDate);
            TcTaskDraft existingTask = findExperimentMergeTarget(taskDraftList, entry.getKey());
            if (existingTask != null) {
                mergeExperimentSpecTask(existingTask, experimentPlanQty, experimentSpecInfo);
                continue;
            }
            taskDraftList.add(buildExperimentSpecTask(context, sidewallRows.get(0), experimentPlanQty, experimentSpecInfo,
                    lossRuleList, minStartQty, defaultCurlLength, toolTotalQty));
        }
    }

    /**
     * 过滤实验规格月计划有效行。
     *
     * @param rowList 月计划查询行
     * @return 施工阶段为实验且当天数量大于 0 的行
     */
    private List<TcExperimentSpecMonthPlanRowVo> filterExperimentSpecRows(List<TcExperimentSpecMonthPlanRowVo> rowList) {
        if (CollUtil.isEmpty(rowList)) {
            return Collections.emptyList();
        }
        return rowList.stream()
                .filter(row -> row != null
                        && TcConstructionStageEnum.EXPERIMENT.getCode().equals(row.getConstructionStage()))
                .filter(row -> nvl(row.getDayQty()).compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
    }

    /**
     * 校验实验规格映射后的胎侧基础字段。
     *
     * @param rowList 实验规格月计划行
     * @throws RuntimeException 胎侧编码、胎侧长、口型板或胶料缺失时抛出
     */
    private void validateExperimentSpecRows(List<TcExperimentSpecMonthPlanRowVo> rowList) {
        Set<String> sidewallCodeEmptyList = new HashSet<>();
        Set<String> sidewallLengthEmptyList = new HashSet<>();
        Set<String> mouthPlateEmptyList = new HashSet<>();
        Set<String> rubberCategoryEmptyList = new HashSet<>();
        for (TcExperimentSpecMonthPlanRowVo row : rowList) {
            String embryoCode = row.getEmbryoCode();
            if (StrUtil.isBlank(row.getSidewallCode())) {
                sidewallCodeEmptyList.add(embryoCode);
            }
            if (nvl(row.getSidewallLength()).compareTo(BigDecimal.ZERO) <= 0) {
                sidewallLengthEmptyList.add(embryoCode);
            }
            if (StrUtil.isBlank(row.getSidewallMouthPlate())) {
                mouthPlateEmptyList.add(embryoCode);
            }
            if (StrUtil.isBlank(row.getSidewallRubber())) {
                rubberCategoryEmptyList.add(embryoCode);
            }
        }
        String errorMsg = this.buildConstructionFieldErrorMessage(sidewallCodeEmptyList,
                sidewallLengthEmptyList, mouthPlateEmptyList, rubberCategoryEmptyList);
        if (StrUtil.isNotBlank(errorMsg)) {
            throw new RuntimeException(errorMsg);
        }
    }

    /**
     * 构建实验规格证据对象。
     *
     * @param context 自动排程上下文
     * @param rowList 同胎侧月计划行
     * @param lookbackDays 回看天数
     * @param experimentPlanQty 固定实验计划量
     * @param experimentPlanDate 月计划定稿生产日期
     * @return 实验规格证据对象
     */
    private TcExperimentSpecInfo buildExperimentSpecInfo(TcScheduleContext context,
                                                         List<TcExperimentSpecMonthPlanRowVo> rowList,
                                                         Integer lookbackDays,
                                                         BigDecimal experimentPlanQty,
                                                         Date experimentPlanDate) {
        TcExperimentSpecInfo info = new TcExperimentSpecInfo();
        info.setExperimentSpec(Boolean.TRUE);
        info.setLookbackDays(lookbackDays);
        info.setLookbackDaysSource(getPositiveIntegerParamSource(context,
                TcScheduleConstants.PARAM_EXPERIMENT_SPEC_LOOKBACK_DAYS,
                TcScheduleConstants.DEFAULT_EXPERIMENT_SPEC_LOOKBACK_DAYS_VALUE));
        info.setPlanQty(experimentPlanQty);
        info.setPlanQtySource(getPositiveDecimalParamSource(context,
                TcScheduleConstants.PARAM_EXPERIMENT_SPEC_PLAN_QTY));
        info.setScheduleDate(context.getScheduleDate());
        info.setExperimentPlanDate(experimentPlanDate);
        info.setMonthPlanDayQty(rowList.stream().map(TcExperimentSpecMonthPlanRowVo::getDayQty)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
        info.setMonthPlanIds(rowList.stream().map(TcExperimentSpecMonthPlanRowVo::getMonthPlanId)
                .filter(Objects::nonNull).collect(Collectors.toList()));
        info.setProductionNos(rowList.stream().map(TcExperimentSpecMonthPlanRowVo::getProductionNo)
                .filter(StrUtil::isNotBlank).collect(Collectors.toList()));
        info.setEmbryoCodes(rowList.stream().map(TcExperimentSpecMonthPlanRowVo::getEmbryoCode)
                .filter(StrUtil::isNotBlank).collect(Collectors.toList()));
        info.setReason("月计划定稿命中实验规格，按固定米数生成胎侧实验计划");
        return info;
    }

    /**
     * 查找同胎侧一班叠加目标任务。
     *
     * @param taskDraftList 待排任务列表
     * @param sidewallCode 胎侧编码
     * @return 同胎侧一班任务；不存在时返回 null
     */
    private TcTaskDraft findExperimentMergeTarget(List<TcTaskDraft> taskDraftList, String sidewallCode) {
        if (CollUtil.isEmpty(taskDraftList) || StrUtil.isBlank(sidewallCode)) {
            return null;
        }
        return taskDraftList.stream()
                .filter(task -> sidewallCode.equals(task.getSidewallCode()))
                .filter(task -> Integer.valueOf(TcScheduleConstants.EXPERIMENT_SPEC_SHIFT_ORDER)
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
    private void mergeExperimentSpecTask(TcTaskDraft task, BigDecimal experimentPlanQty,
                                         TcExperimentSpecInfo experimentSpecInfo) {
        BigDecimal originalCurrentShiftDemandQty = task.getOriginalCurrentShiftDemandQty() == null
                ? nvl(task.getCurrentShiftDemandQty()) : task.getOriginalCurrentShiftDemandQty();
        task.setCurrentShiftDemandQty(nvl(task.getCurrentShiftDemandQty()).add(experimentPlanQty));
        task.setOriginalCurrentShiftDemandQty(originalCurrentShiftDemandQty.add(experimentPlanQty));
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
    private TcTaskDraft buildExperimentSpecTask(TcExperimentSpecMonthPlanRowVo row, BigDecimal experimentPlanQty,
                                                TcExperimentSpecInfo experimentSpecInfo,
                                                List<TcLossRule> lossRuleList, BigDecimal minStartQty,
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
    private TcTaskDraft buildExperimentSpecTask(TcScheduleContext context, TcExperimentSpecMonthPlanRowVo row,
                                                BigDecimal experimentPlanQty,
                                                TcExperimentSpecInfo experimentSpecInfo,
                                                List<TcLossRule> lossRuleList, BigDecimal minStartQty,
                                                BigDecimal defaultCurlLength, BigDecimal toolTotalQty) {
        TcTaskDraft taskDraft = new TcTaskDraft();
        taskDraft.setOrderNo("EXP-" + StrUtil.blankToDefault(row.getProductionNo(), String.valueOf(row.getMonthPlanId()))
                + "-CLASS" + TcScheduleConstants.EXPERIMENT_SPEC_SHIFT_ORDER);
        taskDraft.setSourceOrderNos(appendSourceOrderNos(null, experimentSpecInfo.getProductionNos()));
        taskDraft.setEmbryoCode(CollUtil.isEmpty(experimentSpecInfo.getEmbryoCodes()) ? null
                : String.join(",", experimentSpecInfo.getEmbryoCodes()));
        taskDraft.setBusinessKeySuffix("EXP-" + StrUtil.blankToDefault(row.getProductionNo(), String.valueOf(row.getMonthPlanId()))
                + "-CLASS" + TcScheduleConstants.EXPERIMENT_SPEC_SHIFT_ORDER);
        taskDraft.setSidewallCode(row.getSidewallCode());
        taskDraft.setConstructionVersion(row.getConstructionVersion());
        taskDraft.setSidewallCraft(row.getSidewallCraft());
        taskDraft.setSidewallWeight(row.getSidewallWeight());
        taskDraft.setSidewallWearpRubberWeight(row.getSidewallWearpRubberWeight());
        // 第一个非空分段作为主胶，其余非空分段按原顺序组成基部胶。
        this.fillRubberCodes(taskDraft, row.getSidewallRubber());
        taskDraft.setSmallGlueFlag(this.isSmallGlueCode(context, taskDraft.getGlueCode()));
        taskDraft.setMouthPlateCode(row.getSidewallMouthPlate());
        taskDraft.setShiftOrder(TcScheduleConstants.EXPERIMENT_SPEC_SHIFT_ORDER);
        taskDraft.setSidewallLength(nvl(row.getSidewallLength()));
        taskDraft.setTailFlag(TcYesNoEnum.NO.getCode());
        taskDraft.setTailBalanceQty(BigDecimal.ZERO);
        taskDraft.setCurrentShiftDemandQty(experimentPlanQty);
        taskDraft.setOriginalCurrentShiftDemandQty(experimentPlanQty);
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
     * 关联同一来源、同一胎侧编码的下一排程班需求，并补齐仅下一班有需求的提前候选任务。
     *
     * <p>数据加载时临时生成逻辑第七班任务，仅用于给第六班提供下一班需求；返回结果不会保留
     * 逻辑第七班本身。新规格提前排产任务不参与本门槛，避免改变其既有排产班次和补量口径。</p>
     *
     * @param taskDraftList 含逻辑第七班的原始任务草稿
     * @return 已写入下一班需求且只保留一至六班排程任务的列表
     */
    private List<TcTaskDraft> prepareTwoShiftDemandTasks(List<TcTaskDraft> taskDraftList) {
        if (CollUtil.isEmpty(taskDraftList)) {
            return taskDraftList;
        }
        List<TcTaskDraft> regularTaskList = taskDraftList.stream()
                .filter(Objects::nonNull)
                .filter(task -> task.getSourceShiftOrder() != null)
                .filter(task -> !this.isNewSpecAdvanceTask(task))
                .collect(Collectors.toList());
        Map<String, List<TcTaskDraft>> sourceShiftTaskMap = regularTaskList.stream()
                .collect(Collectors.groupingBy(task -> this.buildTwoShiftSourceKey(task,
                        task.getSourceShiftOrder()), LinkedHashMap::new, Collectors.toList()));

        regularTaskList.stream()
                .filter(task -> task.getSourceShiftOrder() <= TcScheduleConstants.TC_MAX_SHIFT_ORDER)
                .forEach(task -> task.setNextShiftDemandQty(sourceShiftTaskMap
                        .getOrDefault(this.buildTwoShiftSourceKey(task, task.getSourceShiftOrder() + 1),
                                Collections.emptyList())
                        .stream().map(TcTaskDraft::getCurrentShiftDemandQty).map(this::nvl)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)));

        List<TcTaskDraft> leadTaskList = regularTaskList.stream()
                .filter(task -> task.getSourceShiftOrder() > 1
                        && task.getSourceShiftOrder() <= TcScheduleConstants.TC_MAX_SHIFT_ORDER + 1)
                .filter(task -> sourceShiftTaskMap.getOrDefault(
                        this.buildTwoShiftSourceKey(task, task.getSourceShiftOrder() - 1),
                        Collections.emptyList()).isEmpty())
                .map(this::buildTwoShiftLeadTask)
                .collect(Collectors.toList());

        List<TcTaskDraft> resultList = taskDraftList.stream()
                .filter(Objects::nonNull)
                .filter(task -> task.getSourceShiftOrder() == null
                        || task.getSourceShiftOrder() <= TcScheduleConstants.TC_MAX_SHIFT_ORDER)
                .collect(Collectors.toCollection(ArrayList::new));
        resultList.addAll(leadTaskList);
        return resultList;
    }

    /**
     * 根据下一排程班任务生成当前班零需求提前候选。
     *
     * @param nextTask 下一排程班同产品任务
     * @return 当前班零需求、携带下一班需求的候选任务
     */
    private TcTaskDraft buildTwoShiftLeadTask(TcTaskDraft nextTask) {
        TcTaskDraft leadTask = new TcTaskDraft();
        BeanUtils.copyProperties(nextTask, leadTask);
        int leadShiftOrder = nextTask.getSourceShiftOrder() - 1;
        leadTask.setOrderNo(nextTask.getOrderNo() + "-TWO-SHIFT-LEAD-CLASS" + leadShiftOrder);
        leadTask.setBusinessKeySuffix(nextTask.getBusinessKeySuffix()
                + "-TWO-SHIFT-LEAD-CLASS" + leadShiftOrder);
        leadTask.setShiftOrder(leadShiftOrder);
        leadTask.setSourceShiftOrder(leadShiftOrder);
        leadTask.setCurrentShiftDemandQty(BigDecimal.ZERO);
        leadTask.setOriginalCurrentShiftDemandQty(BigDecimal.ZERO);
        leadTask.setNextShiftDemandQty(this.nvl(nextTask.getCurrentShiftDemandQty()));
        leadTask.setDemandQty(BigDecimal.ZERO);
        leadTask.setGuardDemandQty(this.resolveLeadGuardDemandQty(nextTask));
        leadTask.setTwoShiftLeadTask(Boolean.TRUE);
        leadTask.setTwoShiftDemandQty(null);
        leadTask.setTwoShiftStockGapQty(null);
        leadTask.setTwoShiftStockCovered(null);
        leadTask.setUnplannedReasonCode(null);
        leadTask.setUnplannedReasonDesc(null);
        return leadTask;
    }

    /**
     * 计算零需求提前候选在当前班应沿用的保证范围需求。
     *
     * @param nextTask 下一排程班任务
     * @return 以下一班需求为首班，并保留剩余保证班数的需求量
     */
    private BigDecimal resolveLeadGuardDemandQty(TcTaskDraft nextTask) {
        return this.nvl(nextTask.getGuardDemandQty());
    }

    /**
     * 构造两班需求关联键，确保不同来源及不同胎侧编码之间互不串量。
     *
     * @param task       胎侧任务
     * @param shiftOrder 要关联的来源班次
     * @return 来源行、胎侧编码和班次组成的关联键
     */
    private String buildTwoShiftSourceKey(TcTaskDraft task, int shiftOrder) {
        String businessKeySuffix = StrUtil.blankToDefault(task.getBusinessKeySuffix(), "");
        int classMarkerIndex = businessKeySuffix.lastIndexOf("-CLASS");
        String sourceKey = classMarkerIndex >= 0
                ? businessKeySuffix.substring(0, classMarkerIndex) : businessKeySuffix;
        return String.join("|", sourceKey, StrUtil.blankToDefault(task.getSidewallCode(), ""),
                String.valueOf(shiftOrder));
    }

    /**
     * 判断任务是否为新规格提前排产任务。
     *
     * @param task 胎侧任务
     * @return true 表示保持新规格既有行为并绕过两班门槛
     */
    private boolean isNewSpecAdvanceTask(TcTaskDraft task) {
        return task.getNewSpecInfo() != null && task.getNewSpecInfo().isNewSpecHit();
    }

    /**
     * 构造来源任务业务键后缀。
     *
     * <p>同胎侧、胶料、口型板和班次可能对应多条原成型排程结果，后缀用于防止快照、规则证据和解释记录互相覆盖。</p>
     *
     * @param row            成型需求行
     * @param sourceRowIndex 来源行顺序，从 1 开始
     * @param shiftOrder     胎侧排程班次
     * @return 来源任务业务键后缀
     */
    private String buildSourceTaskBusinessKeySuffix(TcFormingDemandRowVo row, int sourceRowIndex, int shiftOrder) {
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
     * @param shiftOrder     胎侧排程班次
     * @return 来源任务业务键后缀
     */
    private String buildSourceTaskBusinessKeySuffix(TcFormingDemandRecipeRowVo row, int sourceRowIndex, int shiftOrder) {
        String sourceOrderNo = row == null ? null : row.getOrderNo();
        String sourceKey = row != null && row.getSourceRecordId() != null
                ? "ID" + row.getSourceRecordId()
                : StrUtil.blankToDefault(sourceOrderNo, "ROW" + sourceRowIndex);
        return sourceKey + "-CLASS" + shiftOrder;
    }

    /**
     * 构建胎侧新规格判断结果。
     *
     * @param context 自动排程上下文
     * @param sidewallCodes 待判断的胎侧编码集合（BOM 模式取成型行 sidewallCode，RECIPE 模式取逐班解析命中的 sidewallCode 并集）
     * @param lookbackDays 回看天数
     * @param advanceShiftCount 提前班次数
     * @return 胎侧编码到新规格证据的映射
     */
    private Map<String, TcNewSpecInfo> buildNewSpecInfoMap(TcScheduleContext context,
                                                           Collection<String> sidewallCodes,
                                                           Integer lookbackDays,
                                                           Integer advanceShiftCount) {
        Map<String, TcNewSpecInfo> resultMap = new HashMap<>();
        if (CollUtil.isEmpty(sidewallCodes)) {
            return resultMap;
        }
        List<String> sidewallCodeList = sidewallCodes.stream()
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(sidewallCodeList)) {
            return resultMap;
        }
        Date previousDate = DateUtil.offsetDay(context.getScheduleDate(), -1);
        Date historyStartDate = DateUtil.offsetDay(context.getScheduleDate(), -lookbackDays);
        Map<String, BigDecimal> previousStockMap = queryPreviousDayStockMap(context, sidewallCodeList, previousDate);
        Map<String, Boolean> historyPlanMap = queryHistoryPlanExistsMap(context, sidewallCodeList, historyStartDate, previousDate);
        String lookbackSource = getPositiveIntegerParamSource(context,
                TcScheduleConstants.PARAM_NEW_SPEC_LOOKBACK_DAYS,
                TcScheduleConstants.DEFAULT_NEW_SPEC_LOOKBACK_DAYS_VALUE);
        String advanceSource = getPositiveIntegerParamSource(context,
                TcScheduleConstants.PARAM_NEW_SPEC_ADVANCE_SHIFT_COUNT,
                TcScheduleConstants.DEFAULT_NEW_SPEC_ADVANCE_SHIFT_COUNT_VALUE);
        for (String sidewallCode : sidewallCodeList) {
            BigDecimal previousStockQty = previousStockMap.getOrDefault(sidewallCode, BigDecimal.ZERO);
            boolean previousStockExists = previousStockQty.compareTo(BigDecimal.ZERO) > 0;
            boolean historyPlanExists = Boolean.TRUE.equals(historyPlanMap.get(sidewallCode));
            TcNewSpecInfo info = new TcNewSpecInfo();
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
            resultMap.put(sidewallCode, info);
        }
        return resultMap;
    }

    /**
     * 查询前一天胎侧净库存。
     *
     * @param context 自动排程上下文
     * @param sidewallCodes 胎侧编码集合
     * @param previousDate 前一天库存日期
     * @return 胎侧编码到净库存的映射
     */
    private Map<String, BigDecimal> queryPreviousDayStockMap(TcScheduleContext context, List<String> sidewallCodes, Date previousDate) {
        Map<String, BigDecimal> stockMap = new HashMap<>();
        if (tmStockMapper == null || CollUtil.isEmpty(sidewallCodes)) {
            return stockMap;
        }
        LambdaQueryWrapper<TcStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcStock::getFactoryCode, context.getFactoryCode());
        wrapper.eq(TcStock::getStockDate, previousDate);
        wrapper.in(TcStock::getSidewallCode, sidewallCodes);
        List<TcStock> stockList = Optional.ofNullable(tmStockMapper.selectList(wrapper)).orElse(Collections.emptyList());
        for (TcStock stock : stockList) {
            if (stock == null || StrUtil.isBlank(stock.getSidewallCode())) {
                continue;
            }
            BigDecimal stockQty = nvl(stock.getStockQty()).subtract(nvl(stock.getBadQty())).add(nvl(stock.getAdjustQty()));
            stockMap.merge(stock.getSidewallCode(), stockQty, BigDecimal::add);
        }
        return stockMap;
    }

    /**
     * 查询历史回看期是否存在胎侧排程计划量。
     *
     * @param context 自动排程上下文
     * @param sidewallCodes 胎侧编码集合
     * @param historyStartDate 回看开始日期
     * @param historyEndDate 回看结束日期
     * @return 胎侧编码到是否存在计划量的映射
     */
    private Map<String, Boolean> queryHistoryPlanExistsMap(TcScheduleContext context, List<String> sidewallCodes,
                                                           Date historyStartDate, Date historyEndDate) {
        Map<String, Boolean> historyPlanMap = new HashMap<>();
        if (tcScheduleResultMapper == null || CollUtil.isEmpty(sidewallCodes)) {
            return historyPlanMap;
        }
        LambdaQueryWrapper<TcScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcScheduleResult::getFactoryCode, context.getFactoryCode());
        wrapper.between(TcScheduleResult::getScheduleDate, historyStartDate, historyEndDate);
        wrapper.in(TcScheduleResult::getSidewallCode, sidewallCodes);
        List<TcScheduleResult> resultList = Optional.ofNullable(tcScheduleResultMapper.selectList(wrapper)).orElse(Collections.emptyList());
        for (TcScheduleResult result : resultList) {
            if (result == null || StrUtil.isBlank(result.getSidewallCode()) || !hasAnyPlanQty(result)) {
                continue;
            }
            historyPlanMap.put(result.getSidewallCode(), Boolean.TRUE);
        }
        return historyPlanMap;
    }

    /**
     * 构建单任务的新规格证据副本。
     *
     * @param source 胎侧级新规格证据
     * @param normalShiftOrder 原正常目标班次
     * @param demandQty 当前任务需求量
     * @return 单任务新规格证据
     */
    private TcNewSpecInfo buildTaskNewSpecInfo(TcNewSpecInfo source, int normalShiftOrder, BigDecimal demandQty) {
        if (source == null) {
            return null;
        }
        TcNewSpecInfo target = new TcNewSpecInfo();
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
    private int resolveTargetShiftOrder(TcNewSpecInfo newSpecInfo, int normalShiftOrder) {
        if (newSpecInfo == null || !newSpecInfo.isNewSpecHit()) {
            return normalShiftOrder;
        }
        return Math.max(1, normalShiftOrder - Math.max(newSpecInfo.getAdvanceShiftCount(), 1));
    }

    /**
     * 解析任务实际使用的库存保证班数，并补充新规格成型需求窗口证据。
     *
     * <p>普通规格保持基础保证班数；新规格至少扩展到提前班数，但不会缩短深度配置或
     * {@code TC_MIN_STOCK_CLASS} 已给出的更深窗口。窗口超过成型 CLASS8 的部分仍由现有末三班平均量规则估算。</p>
     *
     * @param newSpecInfo 新规格判断与窗口证据
     * @param baseGuardShiftCount 深度配置或参数解析出的基础保证班数
     * @param shiftOrder 胎侧排程来源班次
     * @param formingShiftOffset 胎侧班次到成型班次的偏移量
     * @param algorithmCode 需求量算法编码
     * @return 当前任务实际使用的库存保证班数
     */
    private int resolveEffectiveGuardShiftCount(TcNewSpecInfo newSpecInfo, int baseGuardShiftCount,
                                                int shiftOrder, int formingShiftOffset, String algorithmCode) {
        int normalizedBaseGuardShiftCount = Math.max(baseGuardShiftCount, 1);
        int effectiveGuardShiftCount = normalizedBaseGuardShiftCount;
        if (newSpecInfo != null && newSpecInfo.isNewSpecHit()) {
            int advanceShiftCount = newSpecInfo.getAdvanceShiftCount() == null
                    ? 1 : Math.max(newSpecInfo.getAdvanceShiftCount(), 1);
            effectiveGuardShiftCount = Math.max(normalizedBaseGuardShiftCount, advanceShiftCount);
        }
        if (newSpecInfo != null) {
            int formingWindowStartClass = this.resolveGuardStartIndex(shiftOrder, formingShiftOffset, algorithmCode) + 1;
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
     * @param result 胎侧排程结果
     * @return true 表示存在任一班次计划量大于0
     */
    private boolean hasAnyPlanQty(TcScheduleResult result) {
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
    private List<TcDepthConfig> loadDepthConfigs(TcScheduleContext context) {
        if (tmDepthConfigMapper == null) {
            return Collections.emptyList();
        }
        try {
            LambdaQueryWrapper<TcDepthConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TcDepthConfig::getFactoryCode, context.getFactoryCode());
            wrapper.orderByAsc(TcDepthConfig::getMinMachineQty);
            return Optional.ofNullable(tmDepthConfigMapper.selectList(wrapper)).orElse(Collections.emptyList()).stream()
                    .sorted(Comparator.comparing((TcDepthConfig config) -> config.getMinMachineQty() == null
                            ? Integer.MAX_VALUE : config.getMinMachineQty()))
                    .collect(Collectors.toList());
        } catch (RuntimeException ex) {
            log.warn("[TC_DEPTH_CONFIG_LOAD] factoryCode={} 加载库存保证班数配置失败，原因={}，将回退参数 {}",
                    context.getFactoryCode(), ex.getMessage(), TcScheduleConstants.PARAM_MIN_STOCK_CLASS);
            return Collections.emptyList();
        }
    }

    /**
     * 解析库存深度机台数量匹配方式，无配置或非法值时按代码合并方式处理。
     *
     * @param context 自动排程上下文
     * @return 有效的库存深度机台数量匹配方式
     */
    private String resolveDepthMachineMatchMode(TcScheduleContext context) {
        String configuredMode = this.getParamValue(context, TcScheduleConstants.PARAM_DEPTH_MACHINE_MATCH_MODE,
                TcScheduleConstants.DEFAULT_DEPTH_MACHINE_MATCH_MODE);
        String normalizedMode = StrUtil.trim(configuredMode).toUpperCase(Locale.ROOT);
        if (!TcScheduleConstants.DEPTH_MACHINE_MATCH_MODE_CODE.equals(normalizedMode)
                && !TcScheduleConstants.DEPTH_MACHINE_MATCH_MODE_ROW.equals(normalizedMode)) {
            log.warn("[TC_DEPTH_CONFIG_MATCH] factoryCode={}, depthMachineMatchMode={} 非法，使用默认值 {}",
                    context.getFactoryCode(), configuredMode,
                    TcScheduleConstants.DEFAULT_DEPTH_MACHINE_MATCH_MODE);
            return TcScheduleConstants.DEFAULT_DEPTH_MACHINE_MATCH_MODE;
        }
        return normalizedMode;
    }

    /**
     * 按 BOM 成型来源构建胎侧代码对应的去重成型机集合。
     *
     * @param demandRowList BOM 模式成型来源行
     * @return 胎侧代码与成型机编码集合的映射
     */
    private Map<String, Set<String>> buildSidewallMachineCodeMapByBom(List<TcFormingDemandRowVo> demandRowList) {
        Map<String, Set<String>> machineCodeMap = new LinkedHashMap<>();
        if (CollUtil.isEmpty(demandRowList)) {
            return machineCodeMap;
        }
        demandRowList.stream()
                .filter(row -> StrUtil.isNotBlank(row.getSidewallCode())
                        && this.convertConstructionLengthToMeter(row.getSidewallLength())
                        .compareTo(BigDecimal.ZERO) > 0
                        && this.hasPositiveClassQty(this.buildClassQtyArray(row)))
                .forEach(row -> this.appendMachineCodesByProductCode(machineCodeMap, row.getSidewallCode(),
                        row.getCxMachineCode()));
        return machineCodeMap;
    }

    /**
     * 按 RECIPE 解析出的有效施工代码构建胎侧代码对应的去重成型机集合。
     *
     * @param rowList 成型来源行
     * @param classQtyArrayList 各来源行八班成型计划量
     * @param specByClassList 各来源行按班次解析出的施工资料
     * @return 胎侧代码与成型机编码集合的映射
     */
    private Map<String, Set<String>> buildSidewallMachineCodeMapByRecipe(List<TcFormingDemandRecipeRowVo> rowList,
                                                                          List<BigDecimal[]> classQtyArrayList,
                                                                          List<TcConstructionSidewallRowVo[]> specByClassList) {
        Map<String, Set<String>> machineCodeMap = new LinkedHashMap<>();
        if (CollUtil.isEmpty(rowList)) {
            return machineCodeMap;
        }
        for (int rowIndex = 0; rowIndex < rowList.size(); rowIndex++) {
            TcFormingDemandRecipeRowVo row = rowList.get(rowIndex);
            BigDecimal[] classQtyArray = classQtyArrayList.get(rowIndex);
            TcConstructionSidewallRowVo[] specByClass = specByClassList.get(rowIndex);
            for (int classIndex = 0; classIndex < specByClass.length; classIndex++) {
                TcConstructionSidewallRowVo spec = specByClass[classIndex];
                if (spec == null || this.readClassQty(classQtyArray, classIndex).compareTo(BigDecimal.ZERO) <= 0
                        || StrUtil.isBlank(spec.getSidewallCode())
                        || this.convertConstructionLengthToMeter(spec.getSidewallLength())
                        .compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                this.appendMachineCodesByProductCode(machineCodeMap, spec.getSidewallCode(),
                        row.getCxMachineCode());
            }
        }
        return machineCodeMap;
    }

    /**
     * 向代码机台索引追加一行来源的机台编码。
     *
     * @param machineCodeMap 代码机台索引
     * @param productCode 胎侧代码
     * @param machineCodeText 逗号分隔的成型机编码
     */
    private void appendMachineCodesByProductCode(Map<String, Set<String>> machineCodeMap, String productCode,
                                                 String machineCodeText) {
        String normalizedProductCode = this.normalizeProductCode(productCode);
        if (StrUtil.isBlank(normalizedProductCode)) {
            return;
        }
        machineCodeMap.computeIfAbsent(normalizedProductCode, key -> new LinkedHashSet<>())
                .addAll(this.parseMachineCodes(machineCodeText));
    }

    /**
     * 根据匹配方式解析当前任务用于深度匹配的机台集合。
     *
     * @param matchMode 深度机台数量匹配方式
     * @param rowLhMachineCodeText 当前来源行硫化机编码
     * @param codeMachineCodeSet 相同代码汇总后的机台集合
     * @return 去重后的机台编码集合
     */
    private Set<String> resolveDepthMachineCodeSet(String matchMode, String rowLhMachineCodeText,
                                                    Set<String> codeMachineCodeSet) {
        if (TcScheduleConstants.DEPTH_MACHINE_MATCH_MODE_CODE.equals(matchMode)) {
            return codeMachineCodeSet == null ? new LinkedHashSet<>() : new LinkedHashSet<>(codeMachineCodeSet);
        }
        return this.parseMachineCodes(rowLhMachineCodeText);
    }

    /**
     * 获取当前匹配方式对应的机台字段名。
     *
     * @param matchMode 深度机台数量匹配方式
     * @return 机台字段名
     */
    private String resolveDepthMachineFieldName(String matchMode) {
        return TcScheduleConstants.DEPTH_MACHINE_MATCH_MODE_CODE.equals(matchMode)
                ? "CX_MACHINE_CODE" : "LH_MACHINE_CODE";
    }

    /**
     * 解析逗号分隔机台编码并去重。
     *
     * @param machineCodeText 逗号分隔的机台编码
     * @return 去空格、去空值、去重后的机台编码集合
     */
    private Set<String> parseMachineCodes(String machineCodeText) {
        if (StrUtil.isBlank(machineCodeText)) {
            return new LinkedHashSet<>();
        }
        return Arrays.stream(machineCodeText.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 标准化产品代码作为索引键。
     *
     * @param productCode 胎侧代码
     * @return 去除首尾空格后的胎侧代码
     */
    private String normalizeProductCode(String productCode) {
        return StrUtil.trim(productCode);
    }

    /**
     * 判断八班计划量中是否存在正向成型计划。
     *
     * @param classQtyArray 八班计划量
     * @return 存在正向计划时返回 true
     */
    private boolean hasPositiveClassQty(BigDecimal[] classQtyArray) {
        return Arrays.stream(classQtyArray)
                .map(this::nvl)
                .anyMatch(value -> value.compareTo(BigDecimal.ZERO) > 0);
    }

    /**
     * 解析单条成型需求应使用的库存保证班数。
     *
     * @param context 自动排程上下文
     * @param sidewallCode 当前胎侧代码
     * @param orderNo 成型工单号
     * @param depthConfigList 库存保证班数配置
     * @param fallbackGuardShiftCount 参数兜底库存保证班数
     * @param matchMode 深度机台数量匹配方式
     * @param machineCodeSet 当前匹配口径下的去重机台集合
     * @param machineFieldName 参与统计的机台字段名
     * @return 当前成型来源使用的库存保证班数
     */
    private Integer resolveGuardShiftCount(TcScheduleContext context, String sidewallCode, String orderNo,
                                           List<TcDepthConfig> depthConfigList, Integer fallbackGuardShiftCount,
                                           String matchMode, Set<String> machineCodeSet, String machineFieldName) {
        Integer machineQty = machineCodeSet == null || machineCodeSet.isEmpty() ? null : machineCodeSet.size();
        String machineCodes = machineCodeSet == null ? "" : String.join(",", machineCodeSet);
        if (machineQty == null) {
            log.warn("[TC_DEPTH_CONFIG_MATCH] factoryCode={}, orderNo={}, sidewallCode={}, matchMode={}, machineField={}, machineCodes={} 机台编码为空或无法解析，回退参数 {}={}",
                    context.getFactoryCode(), orderNo, sidewallCode, matchMode, machineFieldName, machineCodes,
                    TcScheduleConstants.PARAM_MIN_STOCK_CLASS, fallbackGuardShiftCount);
            return fallbackGuardShiftCount;
        }
        if (CollUtil.isEmpty(depthConfigList)) {
            log.warn("[TC_DEPTH_CONFIG_MATCH] factoryCode={}, orderNo={}, sidewallCode={}, matchMode={}, machineField={}, machineCodes={}, machineQty={} 未维护库存保证班数配置，回退参数 {}={}",
                    context.getFactoryCode(), orderNo, sidewallCode, matchMode, machineFieldName, machineCodes,
                    machineQty, TcScheduleConstants.PARAM_MIN_STOCK_CLASS, fallbackGuardShiftCount);
            return fallbackGuardShiftCount;
        }
        Optional<TcDepthConfig> matchedConfigOptional = depthConfigList.stream()
                .filter(depthConfig -> depthConfig.getMinMachineQty() != null
                        && machineQty >= depthConfig.getMinMachineQty()
                        && (depthConfig.getMaxMachineQty() == null
                        || machineQty <= depthConfig.getMaxMachineQty()))
                .findFirst();
        if (matchedConfigOptional.isPresent()) {
            return this.resolveMatchedGuardShiftCount(context, sidewallCode, orderNo, machineQty,
                    matchedConfigOptional.get(), fallbackGuardShiftCount, matchMode, machineFieldName, machineCodes);
        }
        log.warn("[TC_DEPTH_CONFIG_MATCH] factoryCode={}, orderNo={}, sidewallCode={}, matchMode={}, machineField={}, machineCodes={}, machineQty={} 未命中库存保证班数配置，回退参数 {}={}",
                context.getFactoryCode(), orderNo, sidewallCode, matchMode, machineFieldName, machineCodes, machineQty,
                TcScheduleConstants.PARAM_MIN_STOCK_CLASS, fallbackGuardShiftCount);
        return fallbackGuardShiftCount;
    }

    /**
     * 将已命中的深度配置转换为库存保证班数。
     *
     * @param context 自动排程上下文
     * @param sidewallCode 当前胎侧代码
     * @param orderNo 成型工单号
     * @param machineQty 匹配使用的机台数量
     * @param depthConfig 已命中的深度配置
     * @param fallbackGuardShiftCount 参数兜底库存保证班数
     * @param matchMode 深度机台数量匹配方式
     * @param machineFieldName 参与统计的机台字段名
     * @param machineCodes 去重后的机台编码文本
     * @return 当前成型来源使用的库存保证班数
     */
    private Integer resolveMatchedGuardShiftCount(TcScheduleContext context, String sidewallCode, String orderNo,
                                                  Integer machineQty, TcDepthConfig depthConfig,
                                                  Integer fallbackGuardShiftCount, String matchMode,
                                                  String machineFieldName, String machineCodes) {
        Integer guardShiftCount = this.toPositiveIntegerDepthClassQty(depthConfig.getDepthClassQty());
        if (guardShiftCount != null) {
            log.info("[TC_DEPTH_CONFIG_MATCH] factoryCode={}, orderNo={}, sidewallCode={}, matchMode={}, machineField={}, machineCodes={}, machineQty={}, minMachineQty={}, maxMachineQty={}, depthClassQty={}",
                    context.getFactoryCode(), orderNo, sidewallCode, matchMode, machineFieldName, machineCodes,
                    machineQty, depthConfig.getMinMachineQty(), depthConfig.getMaxMachineQty(), guardShiftCount);
            return guardShiftCount;
        }
        log.warn("[TC_DEPTH_CONFIG_MATCH] factoryCode={}, orderNo={}, sidewallCode={}, matchMode={}, machineField={}, machineCodes={}, machineQty={}, minMachineQty={}, maxMachineQty={}, depthClassQty={} 不是正整数，回退参数 {}={}",
                context.getFactoryCode(), orderNo, sidewallCode, matchMode, machineFieldName, machineCodes, machineQty,
                depthConfig.getMinMachineQty(), depthConfig.getMaxMachineQty(), depthConfig.getDepthClassQty(),
                TcScheduleConstants.PARAM_MIN_STOCK_CLASS, fallbackGuardShiftCount);
        return fallbackGuardShiftCount;
    }

    /**
     * 将库存深度匹配事实写入任务规则证据。
     *
     * @param context 自动排程上下文
     * @param taskDraft 当前任务草稿
     * @param sidewallCode 胎侧代码
     * @param matchMode 深度机台数量匹配方式
     * @param machineFieldName 参与统计的机台字段名
     * @param machineCodeSet 去重后的机台编码集合
     * @param guardShiftCount 最终库存保证班数
     * @param depthConfigList 库存保证班数配置
     * @param fallbackGuardShiftCount 兜底库存保证班数
     */
    private void addDepthConfigMatchTrace(TcScheduleContext context, TcTaskDraft taskDraft, String sidewallCode,
                                          String matchMode, String machineFieldName, Set<String> machineCodeSet,
                                          Integer guardShiftCount, List<TcDepthConfig> depthConfigList,
                                          Integer fallbackGuardShiftCount) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        String depthMatchReason = this.resolveDepthMatchReason(machineCodeSet, depthConfigList);
        evidence.put("sidewallCode", sidewallCode);
        evidence.put("matchMode", matchMode);
        evidence.put("machineField", machineFieldName);
        evidence.put("machineCodes", machineCodeSet == null ? Collections.emptyList()
                : new ArrayList<>(machineCodeSet));
        evidence.put("machineQty", machineCodeSet == null ? 0 : machineCodeSet.size());
        evidence.put("guardShiftCount", guardShiftCount);
        evidence.put("matchResult", "MATCHED".equals(depthMatchReason) ? "MATCHED" : "FALLBACK");
        evidence.put("fallbackReason", "MATCHED".equals(depthMatchReason) ? null : depthMatchReason);
        evidence.put("fallbackGuardShiftCount", fallbackGuardShiftCount);
        context.getRuleTraceMap().computeIfAbsent(taskDraft.getBusinessKey(), key -> new TcRuleTrace())
                .addRuleHit(TcScheduleRuleCodeEnum.DEPTH_CONFIG_MATCH, TcScheduleRuleResultEnum.PASS, evidence);
    }

    /**
     * 解析库存深度匹配的最终命中或回退原因。
     *
     * @param machineCodeSet 当前匹配口径下的去重机台集合
     * @param depthConfigList 库存保证班数配置
     * @return MATCHED 或具体回退原因编码
     */
    private String resolveDepthMatchReason(Set<String> machineCodeSet, List<TcDepthConfig> depthConfigList) {
        if (CollUtil.isEmpty(machineCodeSet)) {
            return "MACHINE_CODE_EMPTY";
        }
        if (CollUtil.isEmpty(depthConfigList)) {
            return "DEPTH_CONFIG_EMPTY";
        }
        Optional<TcDepthConfig> matchedConfigOptional = depthConfigList.stream()
                .filter(depthConfig -> depthConfig.getMinMachineQty() != null
                        && machineCodeSet.size() >= depthConfig.getMinMachineQty()
                        && (depthConfig.getMaxMachineQty() == null
                        || machineCodeSet.size() <= depthConfig.getMaxMachineQty()))
                .findFirst();
        if (!matchedConfigOptional.isPresent()) {
            return "DEPTH_CONFIG_NOT_MATCHED";
        }
        return this.toPositiveIntegerDepthClassQty(matchedConfigOptional.get().getDepthClassQty()) == null
                ? "DEPTH_CLASS_QTY_INVALID" : "MATCHED";
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
     * 补充任务草稿的胎侧辅助基础数据。
     *
     * @param context       自动排程上下文
     * @param taskDraftList 待排任务草稿
     */
    private void fillTaskAuxiliaryData(TcScheduleContext context, List<TcTaskDraft> taskDraftList) {
        if (CollUtil.isEmpty(taskDraftList) || tmCurlRollMapper == null) {
            return;
        }
        List<String> sidewallCodes = taskDraftList.stream()
                .map(TcTaskDraft::getSidewallCode)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(sidewallCodes)) {
            return;
        }
        LambdaQueryWrapper<TcCurlRoll> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcCurlRoll::getFactoryCode, context.getFactoryCode());
        wrapper.in(TcCurlRoll::getSidewallCode, sidewallCodes);
        Map<String, BigDecimal> curlLengthMap = tmCurlRollMapper.selectList(wrapper).stream()
                .filter(curlRoll -> StrUtil.isNotBlank(curlRoll.getSidewallCode()))
                .collect(Collectors.toMap(TcCurlRoll::getSidewallCode,
                        curlRoll -> nvl(curlRoll.getCurlLength()),
                        (existing, replacement) -> existing));
        for (TcTaskDraft taskDraft : taskDraftList) {
            BigDecimal curlLength = curlLengthMap.get(taskDraft.getSidewallCode());
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
    private void addCurrentDayShutdownTraces(TcScheduleContext context, List<TcTaskDraft> taskDraftList) {
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
                    TcScheduleRuleResultEnum result = TcScheduleRuleResultEnum.valueOf(
                            String.valueOf(evidence.remove("result")));
                    context.getRuleTraceMap().computeIfAbsent(task.getBusinessKey(), key -> new TcRuleTrace())
                            .addRuleHit(TcScheduleRuleCodeEnum.CURRENT_DAY_SHUTDOWN_REDISTRIBUTION, result, evidence);
                });
    }

    /**
     * 应用未来停产需求提前均摊和整日停产后首班识别规则。
     *
     * @param context       排程上下文
     * @param taskDraftList 当前六班任务
     * @param machineList   胎侧机台列表
     */
    private void applyCalendarRules(TcScheduleContext context, List<TcTaskDraft> taskDraftList,
                                    List<TcMachineInfo> machineList) {
        int checkWindow = Math.min(Math.max(getIntegerParam(context,
                TcScheduleConstants.PARAM_SHUTDOWN_CHECK_WINDOW,
                TcScheduleConstants.DEFAULT_SHUTDOWN_CHECK_WINDOW_VALUE),
                TcScheduleConstants.MIN_SHUTDOWN_CHECK_WINDOW),
                TcScheduleConstants.MAX_SHUTDOWN_CHECK_WINDOW);
        Date startDate = DateUtil.beginOfDay(DateUtil.offsetDay(context.getScheduleDate(), -1));
        Date endDate = DateUtil.beginOfDay(DateUtil.offsetDay(context.getScheduleDate(),
                Math.max(checkWindow - 1, 1)));
        Date closeOutEndDate = this.resolveFormingShutdownCloseOutEndDate(context, taskDraftList);
        if (closeOutEndDate.after(endDate)) {
            endDate = closeOutEndDate;
        }
        Map<String, TcWorkCalendarRowVo> tmCalendarMap = this.loadWorkCalendarRange(context,
                TcProcessCodeEnum.SIDEWALL.getCode(),
                startDate, endDate);
        Map<String, TcWorkCalendarRowVo> cxCalendarMap = this.loadWorkCalendarRange(context,
                TcProcessCodeEnum.FORMING.getCode(),
                startDate, endDate);
        this.resolveWorkCalendarStoppedShiftOrders(context, tmCalendarMap);
        this.resolveStartupShiftOrders(context, tmCalendarMap);
        this.applyFormingContinuousShutdownCloseOut(context, taskDraftList, cxCalendarMap);
        for (int dayOffset = 1; dayOffset < checkWindow; dayOffset++) {
            Date sourceDate = DateUtil.beginOfDay(DateUtil.offsetDay(context.getScheduleDate(), dayOffset));
            TcWorkCalendarRowVo tmCalendar = tmCalendarMap.get(DateUtil.formatDate(sourceDate));
            TcWorkCalendarRowVo cxCalendar = cxCalendarMap.get(DateUtil.formatDate(sourceDate));
            if (!this.isShutdownDay(tmCalendar) || this.isShutdownDay(cxCalendar)) {
                continue;
            }
            List<Integer> targetShiftOrders = this.resolveFutureShutdownTargetShifts(context, sourceDate, tmCalendarMap);
            TcScheduleContext futureContext = this.buildFutureDemandContext(context, sourceDate);
            List<TcTaskDraft> futureTaskList = this.loadFormingDemandTasks(futureContext, machineList).stream()
                    .filter(task -> task.getExperimentSpecInfo() == null)
                    .filter(task -> task.getShiftOrder() != null && task.getShiftOrder() <= 3)
                    .collect(Collectors.toList());
            this.redistributeFutureShutdownTasks(context, taskDraftList, futureTaskList, sourceDate,
                    targetShiftOrders);
        }
    }

    /**
     * 解析成型连续停产收尾所需的日历查询结束日期。
     *
     * @param context       排程上下文
     * @param taskDraftList 当前任务列表
     * @return 最晚成型来源班次后两个完整工作日的结束日期
     */
    private Date resolveFormingShutdownCloseOutEndDate(TcScheduleContext context,
                                                        List<TcTaskDraft> taskDraftList) {
        return nullToEmpty(taskDraftList).stream()
                .filter(Objects::nonNull)
                .map(TcTaskDraft::getFormingLogicalShiftOrder)
                .filter(Objects::nonNull)
                .filter(shiftOrder -> shiftOrder >= 1 && shiftOrder <= 8)
                .map(shiftOrder -> SixShiftWorkCalendarUtil.resolveFormingProductionDate(
                        context.getScheduleDate(), shiftOrder))
                .map(productionDate -> DateUtil.beginOfDay(DateUtil.offsetDay(productionDate, 2)))
                .max(Date::compareTo)
                .orElse(DateUtil.beginOfDay(DateUtil.offsetDay(context.getScheduleDate(), 2)));
    }

    /**
     * 标记停产前最后开放成型班次对应的胎侧任务。
     *
     * <p>仅当该班之后本日无开放班，且随后两个完整工作日均全天停产时命中；
     * 实验规格不参与，需求量使用原始成型单班需求，不使用算法一窗口最大值。</p>
     *
     * @param context       排程上下文
     * @param taskDraftList 当前任务列表
     * @param cxCalendarMap 成型工作日历
     */
    private void applyFormingContinuousShutdownCloseOut(TcScheduleContext context,
                                                         List<TcTaskDraft> taskDraftList,
                                                         Map<String, TcWorkCalendarRowVo> cxCalendarMap) {
        nullToEmpty(taskDraftList).stream()
                .filter(Objects::nonNull)
                .filter(task -> task.getExperimentSpecInfo() == null)
                .filter(task -> task.getFormingLogicalShiftOrder() != null)
                .filter(task -> task.getFormingLogicalShiftOrder() >= 1
                        && task.getFormingLogicalShiftOrder() <= 8)
                .filter(task -> this.isLastOpenFormingShiftBeforeTwoShutdownDays(
                        context, cxCalendarMap, task.getFormingLogicalShiftOrder()))
                .forEach(task -> this.markFormingShutdownCloseOutTask(context, task));
    }

    /**
     * 判断指定成型逻辑班是否为连续两天停产前的最后开放班。
     *
     * @param context                  排程上下文
     * @param cxCalendarMap            成型工作日历
     * @param formingLogicalShiftOrder 成型逻辑班次
     * @return true表示该班为最后开放班且随后两天全天停产
     */
    private boolean isLastOpenFormingShiftBeforeTwoShutdownDays(TcScheduleContext context,
                                                                 Map<String, TcWorkCalendarRowVo> cxCalendarMap,
                                                                 int formingLogicalShiftOrder) {
        Date productionDate = SixShiftWorkCalendarUtil.resolveFormingProductionDate(
                context.getScheduleDate(), formingLogicalShiftOrder);
        int calendarShiftOrder = SixShiftWorkCalendarUtil.resolveFormingCalendarShiftOrder(
                formingLogicalShiftOrder);
        TcWorkCalendarRowVo productionCalendar = cxCalendarMap.get(DateUtil.formatDate(productionDate));
        if (this.isShutdownDay(productionCalendar)
                || !this.isShiftOpen(productionCalendar, calendarShiftOrder)) {
            return false;
        }
        for (int shiftOrder = calendarShiftOrder + 1; shiftOrder <= 3; shiftOrder++) {
            if (this.isShiftOpen(productionCalendar, shiftOrder)) {
                return false;
            }
        }
        Date firstShutdownDate = DateUtil.beginOfDay(DateUtil.offsetDay(productionDate, 1));
        Date secondShutdownDate = DateUtil.beginOfDay(DateUtil.offsetDay(productionDate, 2));
        return this.isShutdownDay(cxCalendarMap.get(DateUtil.formatDate(firstShutdownDate)))
                && this.isShutdownDay(cxCalendarMap.get(DateUtil.formatDate(secondShutdownDate)));
    }

    /**
     * 写入成型连续停产收尾任务标识、规则证据和运行日志。
     *
     * @param context 排程上下文
     * @param task    待标记胎侧任务
     */
    private void markFormingShutdownCloseOutTask(TcScheduleContext context, TcTaskDraft task) {
        BigDecimal closeOutDemandQty = nvl(task.getFormingShutdownCloseOutDemandQty());
        Date productionDate = SixShiftWorkCalendarUtil.resolveFormingProductionDate(
                context.getScheduleDate(), task.getFormingLogicalShiftOrder());
        Date firstShutdownDate = DateUtil.beginOfDay(DateUtil.offsetDay(productionDate, 1));
        Date secondShutdownDate = DateUtil.beginOfDay(DateUtil.offsetDay(productionDate, 2));
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("ruleCode", TcScheduleRuleCodeEnum.FORMING_CONTINUOUS_SHUTDOWN_CLOSE_OUT.getCode());
        evidence.put("formingProcCode", TcProcessCodeEnum.FORMING.getCode());
        evidence.put("lastOpenProductionDate", DateUtil.formatDate(productionDate));
        evidence.put("lastOpenCalendarShiftOrder", SixShiftWorkCalendarUtil.resolveFormingCalendarShiftOrder(
                task.getFormingLogicalShiftOrder()));
        evidence.put("formingLogicalShiftOrder", task.getFormingLogicalShiftOrder());
        evidence.put("sourceShiftOrder", task.getSourceShiftOrder());
        evidence.put("targetShiftOrder", task.getShiftOrder());
        evidence.put("shutdownStartDate", DateUtil.formatDate(firstShutdownDate));
        evidence.put("shutdownEndDate", DateUtil.formatDate(secondShutdownDate));
        evidence.put("consecutiveShutdownDays", 2);
        evidence.put("closeOutDemandQty", closeOutDemandQty);
        TcScheduleRuleResultEnum result = closeOutDemandQty.compareTo(BigDecimal.ZERO) > 0
                ? TcScheduleRuleResultEnum.PASS : TcScheduleRuleResultEnum.SKIP;
        if (result == TcScheduleRuleResultEnum.PASS) {
            task.setFormingShutdownCloseOutFlag(Boolean.TRUE);
            task.setTailFlag(TcYesNoEnum.YES.getCode());
        } else {
            evidence.put("skipReason", "LAST_OPEN_SHIFT_DEMAND_NOT_POSITIVE");
        }
        context.getRuleTraceMap().computeIfAbsent(task.getBusinessKey(), key -> new TcRuleTrace())
                .addRuleHit(TcScheduleRuleCodeEnum.FORMING_CONTINUOUS_SHUTDOWN_CLOSE_OUT, result, evidence);
        context.appendProcessLog("成型连续停产收尾判定：胎侧={0}，最后开放成型日期={1}，最后开放成型班次={2}，"
                        + "连续停产开始日={3}，连续停产结束日={4}，连续停产天数={5}，停产收尾需求量={6}米，"
                        + "规则结果={7}，成型停产收尾标识={8}，收尾标识={9}，处理原因={10}",
                task.getSidewallCode(), DateUtil.formatDate(productionDate),
                evidence.get("lastOpenCalendarShiftOrder"), evidence.get("shutdownStartDate"),
                evidence.get("shutdownEndDate"), evidence.get("consecutiveShutdownDays"), closeOutDemandQty,
                result.getCode(), result == TcScheduleRuleResultEnum.PASS,
                task.getTailFlag(), evidence.getOrDefault("skipReason", "已按停产前最后开放成型班需求收尾"));
        log.info("[TC_FORMING_SHUTDOWN_CLOSE_OUT] batchNo={}, traceId={}, factoryCode={}, sidewallCode={}, "
                        + "sourceShiftOrder={}, targetShiftOrder={}, formingLogicalShiftOrder={}, "
                        + "shutdownStartDate={}, shutdownEndDate={}, closeOutDemandQty={}, result={}",
                context.getBatchNo(), context.getTraceId(), context.getFactoryCode(), task.getSidewallCode(),
                task.getSourceShiftOrder(), task.getShiftOrder(), task.getFormingLogicalShiftOrder(),
                DateUtil.formatDate(firstShutdownDate), DateUtil.formatDate(secondShutdownDate),
                closeOutDemandQty, result.getCode());
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
    private Map<String, TcWorkCalendarRowVo> loadWorkCalendarRange(TcScheduleContext context, String procCode,
                                                                   Date startDate, Date endDate) {
        try {
            List<TcWorkCalendarRowVo> rowList = tcAutoScheduleDataLoadMapper.selectWorkCalendarRowsByRange(
                    context.getFactoryCode(), procCode, startDate, endDate);
            return nullToEmpty(rowList).stream()
                    .filter(row -> row != null && row.getProductionDate() != null)
                    .collect(Collectors.toMap(row -> DateUtil.formatDate(row.getProductionDate()),
                            row -> row, (existing, replacement) -> existing, LinkedHashMap::new));
        } catch (RuntimeException exception) {
            log.error("[TC_CALENDAR_RANGE] factoryCode={}, procCode={}, startDate={}, endDate={}, result=FAILED",
                    context.getFactoryCode(), procCode, DateUtil.formatDate(startDate), DateUtil.formatDate(endDate),
                    exception);
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.workCalendarQueryFailed"),
                    exception);
        }
    }

    /**
     * 识别当前六班窗口内整日停产后的首个开放班次。
     *
     * @param context       排程上下文
     * @param tmCalendarMap 胎侧日历映射
     */
    private void resolveStartupShiftOrders(TcScheduleContext context,
                                           Map<String, TcWorkCalendarRowVo> tmCalendarMap) {
        Set<Integer> startupShiftOrders = new LinkedHashSet<>();
        Date previousDate = DateUtil.offsetDay(context.getScheduleDate(), -1);
        for (int dayOffset = 0; dayOffset <= 1; dayOffset++) {
            Date currentDate = DateUtil.offsetDay(context.getScheduleDate(), dayOffset);
            TcWorkCalendarRowVo previousCalendar = tmCalendarMap.get(DateUtil.formatDate(previousDate));
            TcWorkCalendarRowVo currentCalendar = tmCalendarMap.get(DateUtil.formatDate(currentDate));
            if (this.isShutdownDay(previousCalendar) && !this.isShutdownDay(currentCalendar)) {
                for (int calendarShift = 1; calendarShift <= 3; calendarShift++) {
                    if (this.isShiftOpen(currentCalendar, calendarShift)) {
                        int startupShiftOrder = dayOffset * 3 + calendarShift + 1;
                        if (startupShiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER) {
                            startupShiftOrders.add(startupShiftOrder);
                        }
                        log.info("[TC_STARTUP_SHIFT] batchNo={}, traceId={}, factoryCode={}, previousDate={}, currentDate={}, startupShiftOrder={}, calendarShift={}, detectionScope=PREVIOUS_FULL_DAY_SHUTDOWN",
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
        log.info("[TC_STARTUP_SHIFT_SUMMARY] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, startupShiftOrders={}",
                context.getBatchNo(), context.getTraceId(), context.getFactoryCode(),
                DateUtil.formatDate(context.getScheduleDate()), startupShiftOrders);
    }

    /**
     * 将胎侧工作日历停产班次写入排程上下文，供所有机台分配路径执行硬过滤。
     *
     * @param context       排程上下文
     * @param tmCalendarMap 胎侧工作日历日期映射
     */
    private void resolveWorkCalendarStoppedShiftOrders(TcScheduleContext context,
                                                        Map<String, TcWorkCalendarRowVo> tmCalendarMap) {
        Set<Integer> stoppedShiftOrders = new LinkedHashSet<>();
        Map<Integer, Map<String, Object>> stoppedShiftEvidenceMap = new LinkedHashMap<>();
        for (int shiftOrder = 1; shiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER; shiftOrder++) {
            if (!this.isSixShiftOpen(context, tmCalendarMap, shiftOrder)) {
                stoppedShiftOrders.add(shiftOrder);
                Date productionDate = SixShiftWorkCalendarUtil.resolveProductionDate(
                        context.getScheduleDate(), shiftOrder);
                TcWorkCalendarRowVo calendar = tmCalendarMap.get(DateUtil.formatDate(productionDate));
                Map<String, Object> evidence = new LinkedHashMap<>();
                evidence.put("procCode", TcProcessCodeEnum.SIDEWALL.getCode());
                evidence.put("productionDate", DateUtil.formatDate(productionDate));
                evidence.put("shiftOrder", shiftOrder);
                evidence.put("calendarField", calendar != null
                        && TcYesNoEnum.NO.getCode().equals(calendar.getDayFlag())
                        ? "DAY_FLAG" : SixShiftWorkCalendarUtil.resolveCalendarShiftField(shiftOrder));
                evidence.put("calendarFieldValue", "0");
                stoppedShiftEvidenceMap.put(shiftOrder, evidence);
            }
        }
        context.setWorkCalendarStoppedShiftOrderSet(stoppedShiftOrders);
        context.setWorkCalendarStoppedShiftEvidenceMap(stoppedShiftEvidenceMap);
        context.appendWorkCalendarStoppedShiftProcessLogs();
    }

    /**
     * 解析未来停产需求可提前承接的当前六班开放班次。
     *
     * @param context       排程上下文
     * @param shutdownDate  停产日期
     * @param tmCalendarMap 胎侧日历映射
     * @return 可承接班次顺序
     */
    private List<Integer> resolveFutureShutdownTargetShifts(TcScheduleContext context, Date shutdownDate,
                                                             Map<String, TcWorkCalendarRowVo> tmCalendarMap) {
        List<Integer> targetShiftOrders = new ArrayList<>();
        for (int shiftOrder = 1; shiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER; shiftOrder++) {
            Date targetDate = SixShiftWorkCalendarUtil.resolveProductionDate(context.getScheduleDate(), shiftOrder);
            if (!targetDate.before(shutdownDate)) {
                continue;
            }
            if (this.isSixShiftOpen(context, tmCalendarMap, shiftOrder)) {
                targetShiftOrders.add(shiftOrder);
            }
        }
        return targetShiftOrders;
    }

    /**
     * 构造未来成型需求加载上下文，复用当前 RECIPE/B 版本模式和需求算法。
     *
     * @param sourceContext 当前排程上下文
     * @param sourceDate    未来需求日期
     * @return 未来需求加载上下文
     */
    private TcScheduleContext buildFutureDemandContext(TcScheduleContext sourceContext, Date sourceDate) {
        TcScheduleContext futureContext = new TcScheduleContext();
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
    private void redistributeFutureShutdownTasks(TcScheduleContext context, List<TcTaskDraft> currentTaskList,
                                                 List<TcTaskDraft> futureTaskList, Date sourceDate,
                                                 List<Integer> targetShiftOrders) {
        Set<String> sourceKeySet = new HashSet<>();
        for (TcTaskDraft sourceTask : futureTaskList) {
            String sourceKey = DateUtil.formatDate(sourceDate) + "|" + sourceTask.getSourceOrderNos()
                    + "|" + sourceTask.getShiftOrder() + "|" + sourceTask.getSidewallCode();
            if (!sourceKeySet.add(sourceKey)) {
                continue;
            }
            BigDecimal originalDemandQty = nvl(sourceTask.getCurrentShiftDemandQty());
            if (originalDemandQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (CollUtil.isEmpty(targetShiftOrders)) {
                TcTaskDraft unplannedTask = this.copyFutureShutdownTask(sourceTask, 6, originalDemandQty,
                        sourceDate, TcScheduleConstants.FUTURE_SHUTDOWN_NO_TARGET_SUFFIX);
                unplannedTask.setPlanQty(BigDecimal.ZERO);
                unplannedTask.setUnplannedReasonCode(TcUnplannedReasonEnum.TC_SHUTDOWN_NO_AVAILABLE_SHIFT.getCode());
                unplannedTask.setUnplannedReasonDesc(TcUnplannedReasonEnum.TC_SHUTDOWN_NO_AVAILABLE_SHIFT.getDesc());
                currentTaskList.add(unplannedTask);
                this.addFutureShutdownTrace(context, unplannedTask, sourceDate, sourceTask.getShiftOrder(), null,
                        originalDemandQty, BigDecimal.ZERO, TcScheduleRuleResultEnum.REJECT);
                continue;
            }
            BigDecimal allocatedQty = originalDemandQty.divide(BigDecimal.valueOf(targetShiftOrders.size()),
                    TcScheduleConstants.DECIMAL_CALCULATION_SCALE, RoundingMode.HALF_UP);
            for (Integer targetShiftOrder : targetShiftOrders) {
                TcTaskDraft targetTask = currentTaskList.stream()
                        .filter(task -> Objects.equals(task.getSidewallCode(), sourceTask.getSidewallCode()))
                        .filter(task -> Objects.equals(task.getShiftOrder(), targetShiftOrder))
                        .filter(task -> !Boolean.TRUE.equals(task.getFormingShutdownCloseOutFlag()))
                        .findFirst().orElse(null);
                if (targetTask == null) {
                    targetTask = this.copyFutureShutdownTask(sourceTask, targetShiftOrder, allocatedQty,
                            sourceDate, String.valueOf(sourceTask.getShiftOrder()));
                    currentTaskList.add(targetTask);
                } else {
                    BigDecimal originalCurrentShiftDemandQty = targetTask.getOriginalCurrentShiftDemandQty() == null
                            ? nvl(targetTask.getCurrentShiftDemandQty())
                            : targetTask.getOriginalCurrentShiftDemandQty();
                    targetTask.setCurrentShiftDemandQty(nvl(targetTask.getCurrentShiftDemandQty()).add(allocatedQty));
                    targetTask.setOriginalCurrentShiftDemandQty(
                            originalCurrentShiftDemandQty.add(allocatedQty));
                    targetTask.setGuardDemandQty(nvl(targetTask.getGuardDemandQty()).add(allocatedQty));
                    targetTask.setDemandQty(nvl(targetTask.getDemandQty()).add(allocatedQty));
                    targetTask.setSourceOrderNos(this.appendSourceOrderNos(targetTask.getSourceOrderNos(),
                            Collections.singletonList(sourceTask.getSourceOrderNos())));
                }
                this.addFutureShutdownTrace(context, targetTask, sourceDate, sourceTask.getShiftOrder(),
                        targetShiftOrder, originalDemandQty, allocatedQty, TcScheduleRuleResultEnum.PASS);
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
    private TcTaskDraft copyFutureShutdownTask(TcTaskDraft sourceTask, Integer targetShift, BigDecimal allocatedQty,
                                               Date sourceDate, String sourceShiftCode) {
        TcTaskDraft targetTask = new TcTaskDraft();
        targetTask.setOrderNo(sourceTask.getOrderNo());
        targetTask.setSourceOrderNos(sourceTask.getSourceOrderNos());
        targetTask.setEmbryoCode(sourceTask.getEmbryoCode());
        targetTask.setCxMachineCode(sourceTask.getCxMachineCode());
        targetTask.setBusinessKeySuffix("FUTURE_SHUTDOWN_" + DateUtil.format(sourceDate, "yyyyMMdd")
                + "_CLASS" + sourceShiftCode + "_TO_CLASS" + targetShift);
        targetTask.setSidewallCode(sourceTask.getSidewallCode());
        targetTask.setConstructionVersion(sourceTask.getConstructionVersion());
        targetTask.setSidewallCraft(sourceTask.getSidewallCraft());
        targetTask.setSidewallWeight(sourceTask.getSidewallWeight());
        targetTask.setSidewallWearpRubberWeight(sourceTask.getSidewallWearpRubberWeight());
        targetTask.setMonthSurplusQty(sourceTask.getMonthSurplusQty());
        targetTask.setGlueCode(sourceTask.getGlueCode());
        targetTask.setBaseGlueCode(sourceTask.getBaseGlueCode());
        targetTask.setMouthPlateCode(sourceTask.getMouthPlateCode());
        targetTask.setShiftOrder(targetShift);
        targetTask.setSidewallLength(sourceTask.getSidewallLength());
        targetTask.setTailFlag(sourceTask.getTailFlag());
        targetTask.setTailBalanceQty(sourceTask.getTailBalanceQty());
        targetTask.setCurrentShiftDemandQty(allocatedQty);
        targetTask.setOriginalCurrentShiftDemandQty(allocatedQty);
        targetTask.setGuardDemandQty(allocatedQty);
        targetTask.setFormingGuardWindowQtyMap(sourceTask.getFormingGuardWindowQtyMap());
        targetTask.setFormingGuardWindowHoursMap(sourceTask.getFormingGuardWindowHoursMap());
        targetTask.setDemandQty(allocatedQty);
        targetTask.setGuardShiftCount(sourceTask.getGuardShiftCount());
        targetTask.setGuardRangeHours(sourceTask.getGuardRangeHours());
        targetTask.setSupplyHours(sourceTask.getSupplyHours());
        targetTask.setMinStartQty(sourceTask.getMinStartQty());
        targetTask.setDefaultCurlRollLength(sourceTask.getDefaultCurlRollLength());
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
    private void addFutureShutdownTrace(TcScheduleContext context, TcTaskDraft task, Date sourceDate,
                                        Integer sourceShiftOrder, Integer targetShiftOrder,
                                        BigDecimal originalDemand, BigDecimal adjustedQty,
                                        TcScheduleRuleResultEnum result) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("ruleCode", TcScheduleRuleCodeEnum.FUTURE_SHUTDOWN_REDISTRIBUTION.getCode());
        evidence.put("date", DateUtil.formatDate(sourceDate));
        evidence.put("sourceDate", DateUtil.formatDate(sourceDate));
        evidence.put("sourceShiftOrder", sourceShiftOrder);
        evidence.put("targetShiftOrder", targetShiftOrder);
        evidence.put("originalDemandQty", originalDemand);
        evidence.put("adjustedQty", adjustedQty);
        evidence.put("threshold", null);
        evidence.put("finalDemandQty", task.getCurrentShiftDemandQty());
        context.getRuleTraceMap().computeIfAbsent(task.getBusinessKey(), key -> new TcRuleTrace())
                .addRuleHit(TcScheduleRuleCodeEnum.FUTURE_SHUTDOWN_REDISTRIBUTION, result, evidence);
    }
    /**
     * 根据工作日历处理当前排程日停产需求重分配。
     *
     * @param context       自动排程上下文
     * @param classQtyArray 六班成型数量
     * @param tmCalendarMap 胎侧工作日历日期映射
     * @param cxCalendarMap 成型工作日历日期映射
     * @return true 表示胎侧停产且没有可接收重分配需求的班次
     */
    private boolean redistributeShutdownDemand(TcScheduleContext context, BigDecimal[] classQtyArray,
                                               Map<String, TcWorkCalendarRowVo> tmCalendarMap,
                                               Map<String, TcWorkCalendarRowVo> cxCalendarMap) {
        if (!TcYesNoEnum.YES.getCode().equals(getParamValue(context,
                TcScheduleConstants.PARAM_SHUTDOWN_REDISTRIBUTION_ENABLED,
                TcYesNoEnum.YES.getCode()))) {
            return false;
        }
        List<Integer> shutdownShiftList = new ArrayList<>();
        List<Integer> availableShiftList = new ArrayList<>();
        BigDecimal shutdownQty = BigDecimal.ZERO;
        for (int shiftOrder = 1; shiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER; shiftOrder++) {
            if (this.isSixShiftOpen(context, tmCalendarMap, shiftOrder)) {
                availableShiftList.add(shiftOrder);
                continue;
            }
            if (this.isSixShiftOpen(context, cxCalendarMap, shiftOrder)) {
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
                evidence.put("ruleCode", TcScheduleRuleCodeEnum.CURRENT_DAY_SHUTDOWN_REDISTRIBUTION.getCode());
                evidence.put("date", DateUtil.formatDate(context.getScheduleDate()));
                evidence.put("sourceShiftOrders", Collections.singletonList(sourceShiftOrder));
                evidence.put("targetShiftOrder", null);
                evidence.put("originalDemandQty", classQtyArray[sourceShiftOrder - 1]);
                evidence.put("adjustedQty", BigDecimal.ZERO);
                evidence.put("threshold", null);
                evidence.put("result", TcScheduleRuleResultEnum.REJECT.getCode());
                context.getCurrentDayShutdownEvidenceMap().put(sourceShiftOrder, evidence);
            }
            log.warn("[CURRENT_DAY_SHUTDOWN_REDISTRIBUTION] factoryCode={}, scheduleDate={} 胎侧停产且无可分配班次",
                    context.getFactoryCode(), DateUtil.formatDate(context.getScheduleDate()));
            return true;
        }
        BigDecimal increaseQty = shutdownQty.divide(new BigDecimal(availableShiftList.size()),
                TcScheduleConstants.DECIMAL_CALCULATION_SCALE, RoundingMode.HALF_UP);
        for (Integer shiftOrder : shutdownShiftList) {
            classQtyArray[shiftOrder - 1] = BigDecimal.ZERO;
        }
        BigDecimal remainingRedistributeQty = shutdownQty;
        for (int targetIndex = 0; targetIndex < availableShiftList.size(); targetIndex++) {
            Integer shiftOrder = availableShiftList.get(targetIndex);
            BigDecimal currentIncreaseQty = targetIndex == availableShiftList.size() - 1
                    ? remainingRedistributeQty : increaseQty;
            remainingRedistributeQty = remainingRedistributeQty.subtract(currentIncreaseQty);
            classQtyArray[shiftOrder - 1] = classQtyArray[shiftOrder - 1].add(currentIncreaseQty);
            Map<String, Object> evidence = context.getCurrentDayShutdownEvidenceMap()
                    .computeIfAbsent(shiftOrder, key -> new LinkedHashMap<>());
            evidence.put("ruleCode", TcScheduleRuleCodeEnum.CURRENT_DAY_SHUTDOWN_REDISTRIBUTION.getCode());
            evidence.put("date", DateUtil.formatDate(context.getScheduleDate()));
            evidence.put("sourceShiftOrders", new ArrayList<>(shutdownShiftList));
            evidence.put("targetShiftOrder", shiftOrder);
            evidence.put("originalDemandQty", nvl((BigDecimal) evidence.get("originalDemandQty")).add(shutdownQty));
            evidence.put("adjustedQty", nvl((BigDecimal) evidence.get("adjustedQty")).add(currentIncreaseQty));
            evidence.put("threshold", null);
            evidence.put("result", TcScheduleRuleResultEnum.PASS.getCode());
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
    private Map<String, TcWorkCalendarRowVo> loadSixShiftWorkCalendarMap(TcScheduleContext context,
                                                                          String procCode) {
        Date startDate = DateUtil.beginOfDay(DateUtil.offsetDay(context.getScheduleDate(), -1));
        Date endDate = DateUtil.beginOfDay(DateUtil.offsetDay(context.getScheduleDate(), 1));
        return this.loadWorkCalendarRange(context, procCode, startDate, endDate);
    }

    /**
     * 判断六班结果班次在指定工序工作日历中是否开放。
     *
     * @param context     排程上下文
     * @param calendarMap 工作日历日期映射
     * @param shiftOrder  结果班次顺序
     * @return true表示开放；日历缺失、标志为空或六班窗口外均按开放兼容
     */
    private boolean isSixShiftOpen(TcScheduleContext context, Map<String, TcWorkCalendarRowVo> calendarMap,
                                   int shiftOrder) {
        if (shiftOrder < 1 || shiftOrder > TcScheduleConstants.TC_MAX_SHIFT_ORDER) {
            return true;
        }
        Date productionDate = SixShiftWorkCalendarUtil.resolveProductionDate(context.getScheduleDate(), shiftOrder);
        TcWorkCalendarRowVo calendar = calendarMap.get(DateUtil.formatDate(productionDate));
        if (calendar == null) {
            return true;
        }
        if (TcYesNoEnum.NO.getCode().equals(calendar.getDayFlag())) {
            return false;
        }
        int calendarShiftOrder = SixShiftWorkCalendarUtil.resolveCalendarShiftOrder(shiftOrder);
        String shiftFlag = calendarShiftOrder == 1 ? calendar.getOneShiftFlag()
                : (calendarShiftOrder == 2 ? calendar.getTwoShiftFlag() : calendar.getThreeShiftFlag());
        return shiftFlag == null || !TcYesNoEnum.NO.getCode().equals(shiftFlag);
    }

    private boolean isShutdownDay(TcWorkCalendarRowVo calendar) {
        if (calendar == null) {
            return false;
        }
        return TcYesNoEnum.NO.getCode().equals(calendar.getDayFlag())
                || (!isShiftOpen(calendar, 1) && !isShiftOpen(calendar, 2) && !isShiftOpen(calendar, 3));
    }

    private boolean isShiftOpen(TcWorkCalendarRowVo calendar, int shiftOrder) {
        if (calendar == null) {
            return true;
        }
        int calendarShift = shiftOrder;
        String shiftFlag;
        if (calendarShift == 1) {
            shiftFlag = calendar.getOneShiftFlag();
        } else if (calendarShift == 2) {
            shiftFlag = calendar.getTwoShiftFlag();
        } else {
            shiftFlag = calendar.getThreeShiftFlag();
        }
        return shiftFlag == null || !TcYesNoEnum.NO.getCode().equals(shiftFlag);
    }

    /**
     * 根据算法和已计算的成型需求起点解析胎侧当前班需求对应的成型计划量。
     *
     * @param classQtyArray 成型班次计划量数组，下标 0 对应成型 CLASS1
     * @param startIndex 成型需求起点，下标 0 对应成型 CLASS1
     * @param algorithmCode 需求量算法编码
     * @param alg1LookbackShifts 算法1连续回看的成型班次数
     * @return 对应成型计划量；超过已加载成型班次时返回 0
     */
    private BigDecimal resolveFormingQty(BigDecimal[] classQtyArray, int startIndex, String algorithmCode,
            int alg1LookbackShifts) {
        if ("2".equals(algorithmCode)) {
            return readClassQty(classQtyArray, startIndex);
        }
        BigDecimal maxQty = BigDecimal.ZERO;
        int lookbackShifts = Math.max(alg1LookbackShifts, 1);
        for (int index = startIndex; index < startIndex + lookbackShifts; index++) {
            maxQty = maxQty.max(readClassQty(classQtyArray, index));
        }
        return maxQty;
    }

    /**
     * 解析胎侧当前班需求对应的成型计划量。
     *
     * <p>需求起点超过成型 CLASS8 时，使用 CLASS6、CLASS7、CLASS8 的固定三班平均量估算未来需求；
     * 其他场景仍调用 {@link #resolveFormingQty(BigDecimal[], int, String, int)}，保持越界按零处理。</p>
     *
     * @param classQtyArray 成型班次计划量数组，下标 0 对应成型 CLASS1
     * @param shiftOrder 胎侧排程班次，从 1 开始
     * @param algorithmCode 需求量算法编码
     * @param formingShiftOffset 胎侧班次到成型班次的偏移量，0 表示同序号班次
     * @param alg1LookbackShifts 算法1连续回看的成型班次数
     * @return 当前班需求对应的成型计划量
     */
    private BigDecimal resolveCurrentShiftFormingQty(BigDecimal[] classQtyArray, int shiftOrder, String algorithmCode,
                                                      int formingShiftOffset, int alg1LookbackShifts) {
        int startIndex = this.resolveCurrentDemandStartIndex(shiftOrder, formingShiftOffset);
        if (startIndex >= 8) {
            return this.calculateLastThreeClassAverageQty(classQtyArray);
        }
        return this.resolveFormingQty(classQtyArray, startIndex, algorithmCode, alg1LookbackShifts);
    }

    /**
     * 计算库存保证范围内的成型需求计划量。
     *
     * @param classQtyArray 成型班次计划量数组
     * @param shiftOrder 胎侧排程班次，从 1 开始
     * @param guardShiftCount 库存最低保证班数
     * @param formingShiftOffset 胎侧班次到成型班次的偏移量
     * @return 库存保证范围内的成型计划量合计
     */
    private BigDecimal calculateGuardFormingQty(BigDecimal[] classQtyArray, int shiftOrder, int guardShiftCount,
                                                int formingShiftOffset, String algorithmCode) {
        BigDecimal total = BigDecimal.ZERO;
        int startIndex = this.resolveGuardStartIndex(shiftOrder, formingShiftOffset, algorithmCode);
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
                .divide(BigDecimal.valueOf(3), TcScheduleConstants.DECIMAL_CALCULATION_SCALE,
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
     * @param shiftOrder 胎侧排程班次
     * @param guardShiftCount 保证班数
     * @param formingShiftOffset 成型班次偏移量
     * @param algorithmCode 需求量算法编码
     * @param lhRemainQty 硫化余量
     * @param rawGuardFormingQty 封顶前保证需求条数
     * @param cappedGuardFormingQty 封顶后保证需求条数
     * @param mode 施工版本匹配模式
     */
    private void addGuardDemandEstimateTrace(TcScheduleContext context, TcTaskDraft taskDraft,
                                             BigDecimal[] classQtyArray, int shiftOrder, int guardShiftCount,
                                             int formingShiftOffset, String algorithmCode, BigDecimal lhRemainQty,
                                             BigDecimal rawGuardFormingQty, BigDecimal cappedGuardFormingQty,
                                             String mode) {
        int startIndex = this.resolveGuardStartIndex(shiftOrder, formingShiftOffset, algorithmCode);
        int count = Math.max(guardShiftCount, 1);
        int exceedShiftCount = Math.max(startIndex + count - 8, 0);
        TcNewSpecInfo newSpecInfo = taskDraft.getNewSpecInfo();
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
        context.getRuleTraceMap().computeIfAbsent(taskDraft.getBusinessKey(), key -> new TcRuleTrace())
                .addRuleHit(TcScheduleRuleCodeEnum.GUARD_DEMAND_ESTIMATE,
                        TcScheduleRuleResultEnum.PASS, evidence);
        log.info("[TC_GUARD_DEMAND_ESTIMATE] batchNo={}, orderNo={}, evidence={}",
                context.getBatchNo(), taskDraft.getOrderNo(), evidence);
    }

    /**
     * 将胎侧班次和配置偏移量转换为成型计划量数组下标。
     *
     * @param shiftOrder 胎侧排程班次，从 1 开始
     * @param formingShiftOffset 胎侧班次到成型班次的偏移量
     * @return 成型计划量数组下标
     */
    private int resolveFormingStartIndex(int shiftOrder, int formingShiftOffset) {
        return Math.max(shiftOrder, 1) + Math.max(formingShiftOffset, 0) - 1;
    }

    /**
     * 解析当前胎侧班需求的成型班次起点。
     *
     * <p>偏移量为 0 时当前需求取同序号成型班；偏移量大于 0 时，当前需求取原偏移窗口的前一班，
     * 使其与后续库存保证窗口互不重叠。</p>
     *
     * @param shiftOrder 胎侧排程班次，从 1 开始
     * @param formingShiftOffset 成型班次偏移量
     * @return 成型计划量数组下标
     */
    private int resolveCurrentDemandStartIndex(int shiftOrder, int formingShiftOffset) {
        int currentDemandOffset = Math.max(formingShiftOffset, 1) - 1;
        return this.resolveFormingStartIndex(shiftOrder, currentDemandOffset);
    }

    /**
     * 解析库存保证范围的成型班次起点。
     *
     * <p>算法 2 的保证范围从当前单班需求的下一班开始；算法 1 的保证范围从当前最大值窗口结束后的下一班开始。</p>
     *
     * @param shiftOrder 胎侧排程班次，从 1 开始
     * @param formingShiftOffset 成型班次偏移量
     * @param algorithmCode 需求量算法编码
     * @return 成型计划量数组下标
     */
    private int resolveGuardStartIndex(int shiftOrder, int formingShiftOffset, String algorithmCode) {
        int currentDemandStartIndex = this.resolveCurrentDemandStartIndex(shiftOrder, formingShiftOffset);
        int currentDemandWindowSize = "1".equals(algorithmCode) ? 3 : 1;
        return currentDemandStartIndex + currentDemandWindowSize;
    }

    /**
     * 计算库存保证范围总时长，供需求量策略计算供应时长 supplyHours 与排序库存紧急度使用。
     *
     * <p>供应时长窗口先补入当前胎侧班对应的成型班次，再追加保证范围内的成型班次；保证范围时长
     * {@code guardRangeHours} 仍只累计保证班次。任一窗口班次时长缺失或非正时置 null 并记 SKIP，
     * 避免 supplyHours 误算。移植自胎面 TmAutoScheduleDataLoadService。</p>
     *
     * @param context 排程上下文
     * @param taskDraft 任务草稿
     * @param shiftOrder 胎侧排程班次
     * @param guardShiftCount 库存保证班数
     * @param formingShiftOffset 成型班次偏移量
     * @param algorithmCode 需求量算法编码
     */
    private void fillGuardRangeHours(TcScheduleContext context, TcTaskDraft taskDraft, int shiftOrder,
                                     int guardShiftCount, int formingShiftOffset, String algorithmCode) {
        int currentLogicalShiftOrder = this.resolveCurrentDemandStartIndex(shiftOrder, formingShiftOffset) + 1;
        int logicalStartShiftOrder = this.resolveGuardStartIndex(shiftOrder, formingShiftOffset, algorithmCode) + 1;
        int count = Math.max(guardShiftCount, 1);
        BigDecimal guardRangeHours = BigDecimal.ZERO;
        List<Integer> mappedShiftOrders = new ArrayList<>();
        List<BigDecimal> shiftHours = new ArrayList<>();
        Map<Integer, BigDecimal> guardWindowHoursMap = new LinkedHashMap<>();
        String skipReason = null;
        int currentMappedShiftOrder = this.mapGuardLogicalShiftOrder(currentLogicalShiftOrder);
        BigDecimal currentDemandShiftHours = context.getShiftHoursMap().get(currentMappedShiftOrder);
        mappedShiftOrders.add(currentMappedShiftOrder);
        shiftHours.add(currentDemandShiftHours);
        if (currentDemandShiftHours == null || currentDemandShiftHours.compareTo(BigDecimal.ZERO) <= 0) {
            skipReason = "CURRENT_SHIFT_HOURS_MISSING_OR_NON_POSITIVE";
        } else {
            guardWindowHoursMap.put(currentLogicalShiftOrder, currentDemandShiftHours);
        }
        for (int index = 0; index < count; index++) {
            if (skipReason != null) {
                break;
            }
            int logicalShiftOrder = logicalStartShiftOrder + index;
            int mappedShiftOrder = this.mapGuardLogicalShiftOrder(logicalShiftOrder);
            BigDecimal currentShiftHours = context.getShiftHoursMap().get(mappedShiftOrder);
            mappedShiftOrders.add(mappedShiftOrder);
            shiftHours.add(currentShiftHours);
            if (currentShiftHours == null || currentShiftHours.compareTo(BigDecimal.ZERO) <= 0) {
                skipReason = "SHIFT_HOURS_MISSING_OR_NON_POSITIVE";
                break;
            }
            guardWindowHoursMap.put(logicalShiftOrder, currentShiftHours);
            guardRangeHours = guardRangeHours.add(currentShiftHours);
        }
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("currentLogicalShiftOrder", currentLogicalShiftOrder);
        evidence.put("logicalStartShiftOrder", logicalStartShiftOrder);
        evidence.put("guardShiftCount", count);
        evidence.put("mappedShiftOrders", mappedShiftOrders);
        evidence.put("shiftHours", shiftHours);
        evidence.put("supplyWindowHoursMap", guardWindowHoursMap);
        if (skipReason == null) {
            taskDraft.setGuardRangeHours(guardRangeHours);
            taskDraft.setFormingGuardWindowHoursMap(guardWindowHoursMap);
            evidence.put("guardRangeHours", guardRangeHours);
            context.getRuleTraceMap().computeIfAbsent(taskDraft.getBusinessKey(), key -> new TcRuleTrace())
                    .addRuleHit(TcScheduleRuleCodeEnum.GUARD_RANGE_HOURS, TcScheduleRuleResultEnum.PASS, evidence);
            return;
        }
        taskDraft.setGuardRangeHours(null);
        taskDraft.setFormingGuardWindowHoursMap(Collections.emptyMap());
        evidence.put("guardRangeHours", null);
        evidence.put("reason", skipReason);
        context.getRuleTraceMap().computeIfAbsent(taskDraft.getBusinessKey(), key -> new TcRuleTrace())
                .addRuleHit(TcScheduleRuleCodeEnum.GUARD_RANGE_HOURS, TcScheduleRuleResultEnum.SKIP, evidence);
    }

    /**
     * 将超过六班的逻辑班次按三班日周期映射到实际班次配置。
     *
     * @param logicalShiftOrder 从一开始连续增长的逻辑班次
     * @return 一至六范围内的实际班次顺序
     */
    private int mapGuardLogicalShiftOrder(int logicalShiftOrder) {
        if (logicalShiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER) {
            return logicalShiftOrder;
        }
        return ((logicalShiftOrder - TcScheduleConstants.TC_MAX_SHIFT_ORDER - 1) % 3) + 1;
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

    private BigDecimal[] buildClassQtyArray(TcFormingDemandRowVo row) {
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
     * 判断机台是否启用。
     *
     * @param machineInfo 机台基础资料
     * @return true 表示可参与排程
     */
    private boolean isMachineEnabled(TcMachineInfo machineInfo) {
        return machineInfo != null && (StrUtil.isBlank(machineInfo.getMachineStatus())
                || TcYesNoEnum.YES.getCode().equals(machineInfo.getMachineStatus()));
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
     * 将施工表中的胎侧长度由毫米换算为自动排程使用的米。
     *
     * <p>仅用于成型计划关联施工信息的任务草稿；原始值仍用于施工字段完整性校验。</p>
     *
     * @param constructionLength 施工表胎侧长度，单位毫米
     * @return 换算后的胎侧长度，单位米；空值按0处理
     */
    private BigDecimal convertConstructionLengthToMeter(BigDecimal constructionLength) {
        return this.nvl(constructionLength).divide(CONSTRUCTION_LENGTH_UNIT_DIVISOR);
    }

    /**
     * 汇总成型来源行 CLASS1 至 CLASS8 的有效计划量。
     *
     * <p>空值和负数不计入合计，且必须在班次偏移、需求算法和停产重分配前调用。</p>
     *
     * @param classQtyArray 成型来源行原始八班计划量
     * @return 八班正计划量合计，单位条
     */
    private BigDecimal calculateTotalFormingPlanQty(BigDecimal[] classQtyArray) {
        if (classQtyArray == null) {
            return BigDecimal.ZERO;
        }
        return Arrays.stream(classQtyArray)
                .filter(Objects::nonNull)
                .filter(classQty -> classQty.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 按成型来源行八班计划合计判断单胎胚是否收尾。
     * <p>月计划余量和成型计划合计均为条数，必须直接比较，不能混入胎侧长度后的米数；
     * 余量缺失、余量为负或八班计划合计为零时保持非收尾。</p>
     *
     * @param planSurplusQty 胎胚月计划余量，单位条
     * @param totalFormingPlanQty 来源行 CLASS1 至 CLASS8 正计划量合计，单位条
     * @return true-月计划余量可由来源行八班计划完成，false-仍按非收尾规格处理
     */
    private boolean isCloseOutByPlanSurplus(BigDecimal planSurplusQty, BigDecimal totalFormingPlanQty) {
        return planSurplusQty != null
                && planSurplusQty.compareTo(BigDecimal.ZERO) >= 0
                && totalFormingPlanQty != null
                && totalFormingPlanQty.compareTo(BigDecimal.ZERO) > 0
                && planSurplusQty.compareTo(totalFormingPlanQty) <= 0;
    }

    /**
     * 将来源行收尾判定写入任务解释链路。
     *
     * @param context 排程上下文
     * @param taskDraft 任务草稿
     * @param originalClassQtyArray 来源行原始八班计划量
     * @param totalFormingPlanQty 八班正计划量合计
     * @param planSurplusQty 胎胚月计划余量
     * @param loadMode 成型需求加载模式
     */
    private void addCloseOutJudgeTrace(TcScheduleContext context, TcTaskDraft taskDraft,
                                       BigDecimal[] originalClassQtyArray, BigDecimal totalFormingPlanQty,
                                       BigDecimal planSurplusQty, String loadMode) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("sourceOrderNo", taskDraft.getSourceOrderNos());
        evidence.put("loadMode", loadMode);
        evidence.put("decisionMode", "SOURCE_ROW_CLASS1_TO_CLASS8_TOTAL");
        for (int classIndex = 0; classIndex < originalClassQtyArray.length; classIndex++) {
            evidence.put("class" + (classIndex + 1) + "PlanQty", this.readClassQty(originalClassQtyArray, classIndex));
        }
        evidence.put("totalFormingPlanQty", totalFormingPlanQty);
        evidence.put("cxRemainQty", planSurplusQty);
        evidence.put("tailFlag", taskDraft.getTailFlag());
        context.getRuleTraceMap().computeIfAbsent(taskDraft.getBusinessKey(), key -> new TcRuleTrace())
                .addRuleHit(TcScheduleRuleCodeEnum.CLOSE_OUT_JUDGE, TcScheduleRuleResultEnum.PASS, evidence);
    }

    /**
     * 按来源行记录一次收尾判定运行日志，避免按任务重复输出。
     *
     * @param context 排程上下文
     * @param sourceOrderNo 成型来源工单号
     * @param embryoCode 胎胚编码
     * @param originalClassQtyArray 来源行原始八班计划量
     * @param totalFormingPlanQty 八班正计划量合计
     * @param planSurplusQty 胎胚月计划余量
     * @param closeOut 是否收尾
     * @param loadMode 成型需求加载模式
     */
    private void logCloseOutJudge(TcScheduleContext context, String sourceOrderNo, String embryoCode,
                                  BigDecimal[] originalClassQtyArray, BigDecimal totalFormingPlanQty,
                                  BigDecimal planSurplusQty, boolean closeOut, String loadMode) {
        log.info("[TC_CLOSE_OUT_JUDGE] batchNo={}, sourceOrderNo={}, embryoCode={}, loadMode={}, "
                        + "classPlanQty={}, totalFormingPlanQty={}, cxRemainQty={}, tailFlag={}",
                context.getBatchNo(), sourceOrderNo, embryoCode, loadMode, Arrays.toString(originalClassQtyArray),
                totalFormingPlanQty, planSurplusQty,
                closeOut ? TcYesNoEnum.YES.getCode() : TcYesNoEnum.NO.getCode());
    }

    /**
     * 解析胎侧机台最大班产，基础数据无效时使用固定兼容值。
     *
     * @param maxCapacity 机台表最大班产
     * @return 正数最大班产
     */
    private BigDecimal resolveMachineMaxCapacity(BigDecimal maxCapacity) {
        if (maxCapacity == null || maxCapacity.compareTo(BigDecimal.ZERO) <= 0) {
            return new BigDecimal(TcScheduleConstants.DEFAULT_MACHINE_MAX_CAPACITY);
        }
        return maxCapacity;
    }

    /**
     * 按英文逗号拆分施工胶料并写入任务。
     *
     * <p>第一个非空分段作为主胶料，其余非空分段去除首尾空格后按原顺序组成基部胶料，
     * 不从整条胶料编码或机台关系反推胶料。</p>
     *
     * @param taskDraft 待写入胶料编码的任务草稿
     * @param rubberCategory 施工胶料原始文本
     */
    private void fillRubberCodes(TcTaskDraft taskDraft, String rubberCategory) {
        if (taskDraft == null) {
            return;
        }
        List<String> rubberCodeList = StrUtil.isBlank(rubberCategory)
                ? Collections.emptyList()
                : Arrays.stream(rubberCategory.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(rubberCodeList)) {
            taskDraft.setGlueCode(null);
            taskDraft.setBaseGlueCode(null);
            return;
        }
        taskDraft.setGlueCode(rubberCodeList.get(0));
        taskDraft.setBaseGlueCode(rubberCodeList.size() > 1
                ? String.join(",", rubberCodeList.subList(1, rubberCodeList.size())) : null);
    }

    /**
     * 构造施工关键字段聚合校验提示。
     *
     * <p>明细使用稳定字段编码，提示前缀统一从国际化资源取得，避免将中文字段名硬编码到前端异常。</p>
     *
     * @param sidewallCodeSourceSet 胎侧编码缺失的来源集合
     * @param sidewallLengthSourceSet 胎侧长度非法的来源集合
     * @param mouthPlateSourceSet 口型板缺失的来源集合
     * @param rubberSourceSet 胶料缺失的来源集合
     * @return 国际化聚合提示，无异常字段时返回空字符串
     */
    private String buildConstructionFieldErrorMessage(Set<String> sidewallCodeSourceSet,
                                                        Set<String> sidewallLengthSourceSet,
                                                        Set<String> mouthPlateSourceSet,
                                                        Set<String> rubberSourceSet) {
        List<String> detailList = new ArrayList<>();
        this.appendConstructionFieldDetail(detailList, "SIDEWALL_CODE", sidewallCodeSourceSet);
        this.appendConstructionFieldDetail(detailList, "SIDEWALL_LENGTH", sidewallLengthSourceSet);
        this.appendConstructionFieldDetail(detailList, "SIDEWALL_MOUTH_PLATE", mouthPlateSourceSet);
        this.appendConstructionFieldDetail(detailList, "SIDEWALL_RUBBER", rubberSourceSet);
        if (CollUtil.isEmpty(detailList)) {
            return "";
        }
        return MessageFormat.format(I18nUtil.getMessage("ui.tc.schedule.constructionFieldsInvalid"),
                String.join("; ", detailList));
    }

    /**
     * 追加单个施工字段的异常来源明细。
     *
     * @param detailList 目标明细列表
     * @param fieldCode 施工字段编码
     * @param sourceSet 异常来源集合
     */
    private void appendConstructionFieldDetail(List<String> detailList, String fieldCode, Set<String> sourceSet) {
        if (CollUtil.isEmpty(sourceSet)) {
            return;
        }
        detailList.add(fieldCode + "=[" + sourceSet.stream()
                .filter(StrUtil::isNotBlank)
                .sorted()
                .collect(Collectors.joining(",")) + "]");
    }

    private String getParamValue(TcScheduleContext context, String paramCode, String defaultValue) {
        TcParamValue value = context.getParamMap().get(paramCode);
        return value == null || StrUtil.isBlank(value.getEffectiveValue()) ? defaultValue : value.getEffectiveValue();
    }

    private BigDecimal getDecimalParam(TcScheduleContext context, String paramCode) {
        String value = getParamValue(context, paramCode, "0");
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private Integer getIntegerParam(TcScheduleContext context, String paramCode, Integer defaultValue) {
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
    private boolean isSmallGlueCode(TcScheduleContext context, String glueCode) {
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
    private Integer getNonNegativeIntegerParam(TcScheduleContext context, String paramCode, Integer defaultValue) {
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
    private Integer getPositiveIntegerParam(TcScheduleContext context, String paramCode, Integer defaultValue) {
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
    private String getPositiveIntegerParamSource(TcScheduleContext context, String paramCode, Integer defaultValue) {
        TcParamValue value = context.getParamMap().get(paramCode);
        if (value == null || StrUtil.isBlank(value.getParamValue())) {
            return TcParamValueSourceEnum.DEFAULT.getCode();
        }
        try {
            Integer parsedValue = Integer.valueOf(value.getParamValue());
            return parsedValue.compareTo(0) > 0
                    ? TcParamValueSourceEnum.PARAM.getCode() : TcParamValueSourceEnum.DEFAULT.getCode();
        } catch (NumberFormatException ex) {
            return TcParamValueSourceEnum.DEFAULT.getCode();
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
    private BigDecimal getPositiveDecimalParam(TcScheduleContext context, String paramCode, BigDecimal defaultValue) {
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
    private String getPositiveDecimalParamSource(TcScheduleContext context, String paramCode) {
        TcParamValue value = context.getParamMap().get(paramCode);
        if (value == null || StrUtil.isBlank(value.getParamValue())) {
            return TcParamValueSourceEnum.DEFAULT.getCode();
        }
        try {
            BigDecimal parsedValue = new BigDecimal(value.getParamValue());
            return parsedValue.compareTo(BigDecimal.ZERO) > 0
                    ? TcParamValueSourceEnum.PARAM.getCode() : TcParamValueSourceEnum.DEFAULT.getCode();
        } catch (NumberFormatException ex) {
            return TcParamValueSourceEnum.DEFAULT.getCode();
        }
    }
    private void validateContext(TcScheduleContext context) {
        if (context == null) {
            throw new IllegalArgumentException(I18nUtil.getMessage("ui.tc.schedule.contextEmpty"));
        }
        if (StrUtil.isBlank(context.getFactoryCode())) {
            throw new IllegalArgumentException(I18nUtil.getMessage("ui.tc.schedule.factoryCodeEmpty"));
        }
        if (context.getScheduleDate() == null) {
            throw new IllegalArgumentException(I18nUtil.getMessage("ui.tc.schedule.scheduleDateEmpty"));
        }
    }
}
