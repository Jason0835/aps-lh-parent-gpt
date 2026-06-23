package com.zlt.aps.tm.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.tm.api.domain.entity.*;
import com.zlt.aps.tm.domain.vo.TmFormingDemandRowVo;
import com.zlt.aps.tm.domain.vo.TmWorkCalendarRowVo;
import com.zlt.aps.tm.engine.domain.TmMachineCandidate;
import com.zlt.aps.tm.engine.domain.TmParamValue;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import com.zlt.aps.tm.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
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

    private static final String PARAM_ALGORITHM_SWITCH = "TM_ALGORITHM_SWITCH";

    private static final String PARAM_STOCK_GUARD_SHIFT_COUNT = "TM_STOCK_GUARD_SHIFT_COUNT";

    private static final String PARAM_MIN_START_QTY = "TM_MIN_START_QTY";

    private static final String PARAM_DEFAULT_CURL_LENGTH = "TM_DEFAULT_CURL_LENGTH";

    private static final String PARAM_SHUTDOWN_REDISTRIBUTION_ENABLED = "TM_SHUTDOWN_REDISTRIBUTION_ENABLED";

    private static final String PARAM_PLAN_QTY_STRATEGY = "TM_PLAN_QTY_STRATEGY";

    private static final String PARAM_TASK_SORT_STRATEGY = "TM_TASK_SORT_STRATEGY";

    private static final String PROC_CODE_CX = "03";

    private static final String PROC_CODE_TM = "04";

    private static final String YES = "1";

    private static final String NO = "0";

    private static final String CLOSE_OUT_TIP = "0";

    private static final String JOB_TYPE_ALLOW = "0";

    private static final String JOB_TYPE_FORBID = "1";

    /** 基础数据本地缓存有效期，单位毫秒 */
    private static final long BASE_DATA_CACHE_TTL_MILLIS = 5 * 60 * 1000L;

    /** 参数缓存，key=工厂 */
    private final Map<String, TmLocalCacheEntry<List<TmParams>>> paramsCacheMap = new ConcurrentHashMap<>();

    /** 机台缓存，key=工厂 */
    private final Map<String, TmLocalCacheEntry<List<TmMachineInfo>>> machineCacheMap = new ConcurrentHashMap<>();

    /** 工作日历缓存，key=工厂+工序+日期 */
    private final Map<String, TmLocalCacheEntry<List<TmWorkCalendarRowVo>>> calendarCacheMap = new ConcurrentHashMap<>();

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

    /**
     * 加载自动排程所需数据。
     *
     * @param context 自动排程上下文，必须包含工厂和排程日期
     * @throws IllegalArgumentException 上下文、工厂或排程日期为空时抛出
     */
    public void loadAllData(TmScheduleContext context) {
        validateContext(context);
        loadParams(context);
        List<TmMachineInfo> machineList = loadMachineInfo(context);
        context.setMachineCandidateList(loadMachineCandidates(context, machineList));
        List<TmTaskDraft> taskDraftList = loadFormingDemandTasks(context, machineList);
        fillTaskAuxiliaryData(context, taskDraftList);
        context.setTaskDraftList(taskDraftList);
        log.info("[TM_AUTO_SCHEDULE_LOAD] factoryCode={}, scheduleDate={}, taskCount={}, machineCount={}",
                context.getFactoryCode(), DateUtil.formatDate(context.getScheduleDate()),
                taskDraftList.size(), machineList.size());
    }

    /**
     * 加载胎面排程参数快照。
     *
     * @param context 自动排程上下文
     */
    private void loadParams(TmScheduleContext context) {
        LambdaQueryWrapper<TmParams> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmParams::getFactoryCode, context.getFactoryCode());
        wrapper.eq(TmParams::getEnableStatus, YES);
        List<TmParams> paramsList = getCachedList(paramsCacheMap, context.getFactoryCode(),
                () -> tmParamsMapper.selectList(wrapper));
        Map<String, TmParamValue> paramMap = new HashMap<>();
        if (CollUtil.isNotEmpty(paramsList)) {
            for (TmParams params : paramsList) {
                TmParamValue value = new TmParamValue();
                value.setParamCode(params.getParamCode());
                value.setParamValue(params.getParamValue());
                value.setDefaultValue(params.getDefaultValue());
                value.setSource("T_TM_PARAMS");
                paramMap.put(params.getParamCode(), value);
            }
        }
        putDefaultParam(paramMap, PARAM_ALGORITHM_SWITCH, "1");
        putDefaultParam(paramMap, PARAM_STOCK_GUARD_SHIFT_COUNT, "2");
        putDefaultParam(paramMap, PARAM_MIN_START_QTY, "0");
        putDefaultParam(paramMap, PARAM_DEFAULT_CURL_LENGTH, "0");
        putDefaultParam(paramMap, PARAM_SHUTDOWN_REDISTRIBUTION_ENABLED, "1");
        putDefaultParam(paramMap, PARAM_PLAN_QTY_STRATEGY, "DEFAULT");
        putDefaultParam(paramMap, PARAM_TASK_SORT_STRATEGY, "DEFAULT");
        context.setParamMap(paramMap);
    }

    /**
     * 加载胎面机台基础资料。
     *
     * @param context 自动排程上下文
     * @return 已启用或可参与排程的机台列表
     */
    private List<TmMachineInfo> loadMachineInfo(TmScheduleContext context) {
        LambdaQueryWrapper<TmMachineInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmMachineInfo::getFactoryCode, context.getFactoryCode());
        wrapper.eq(TmMachineInfo::getMachineStatus, ApsConstant.APS_YES_NO_1);
        wrapper.orderByAsc(TmMachineInfo::getMachineCode);
        return getCachedList(machineCacheMap, context.getFactoryCode(),
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
            candidate.setMaxCapacity(nvl(machineInfo.getMaxCapacity()));
            candidate.setRemainCapacity(nvl(machineInfo.getMaxCapacity()));
            candidate.setMaintenanceHours(BigDecimal.ZERO);
            candidate.setSwitchCostHours(BigDecimal.ZERO);
            candidate.setMouthPlateCodes(new HashSet<>());
            candidate.setForbiddenGlueCodes(new HashSet<>());
            candidate.setFixedAllowTreadCodes(new HashSet<>());
            candidate.setFixedForbidTreadCodes(new HashSet<>());
            candidateMap.put(machineInfo.getMachineCode(), candidate);
        }
        fillCandidateMouthPlate(context, candidateMap);
        fillCandidateGlueRule(context, candidateMap);
        fillCandidateSpecifyRule(context, candidateMap);
        fillCandidateSpeed(context, candidateMap);
        fillCandidateMaintenance(context, candidateMap);
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
        wrapper.eq(TmGlueMachineReal::getEnableStatus, YES);
        wrapper.in(TmGlueMachineReal::getMachineCode, candidateMap.keySet());
        List<TmGlueMachineReal> glueRuleList = tmGlueMachineRealMapper.selectList(wrapper);
        for (TmGlueMachineReal glueRule : glueRuleList) {
            if (StrUtil.isBlank(glueRule.getGlueCode()) || !NO.equals(glueRule.getAllowFlag())) {
                continue;
            }
            TmMachineCandidate candidate = candidateMap.get(glueRule.getMachineCode());
            if (candidate == null) {
                continue;
            }
            if (candidate.getForbiddenGlueCodes() == null) {
                candidate.setForbiddenGlueCodes(new HashSet<>());
            }
            candidate.getForbiddenGlueCodes().add(glueRule.getGlueCode());
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
        wrapper.eq(TmSpecifyMachine::getEnableStatus, YES);
        wrapper.in(TmSpecifyMachine::getMachineCode, candidateMap.keySet());
        List<TmSpecifyMachine> specifyList = tmSpecifyMachineMapper.selectList(wrapper);
        for (TmSpecifyMachine specify : specifyList) {
            TmMachineCandidate candidate = candidateMap.get(specify.getMachineCode());
            if (candidate == null || StrUtil.isBlank(specify.getTreadCode())) {
                continue;
            }
            if (JOB_TYPE_ALLOW.equals(specify.getJobType())) {
                if (candidate.getFixedAllowTreadCodes() == null) {
                    candidate.setFixedAllowTreadCodes(new HashSet<>());
                }
                candidate.getFixedAllowTreadCodes().add(specify.getTreadCode());
            } else if (JOB_TYPE_FORBID.equals(specify.getJobType())) {
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
     * @param context      自动排程上下文
     * @param candidateMap 候选机台映射
     */
    private void fillCandidateMaintenance(TmScheduleContext context, Map<String, TmMachineCandidate> candidateMap) {
        if (tmMachineMaintenanceMapper == null || candidateMap.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<TmMachineMaintenance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmMachineMaintenance::getFactoryCode, context.getFactoryCode());
        wrapper.in(TmMachineMaintenance::getMachineCode, candidateMap.keySet());
        List<TmMachineMaintenance> maintenanceList = tmMachineMaintenanceMapper.selectList(wrapper);
        Date dayStart = DateUtil.beginOfDay(context.getScheduleDate());
        Date dayEnd = DateUtil.endOfDay(context.getScheduleDate());
        for (TmMachineMaintenance maintenance : maintenanceList) {
            TmMachineCandidate candidate = candidateMap.get(maintenance.getMachineCode());
            if (candidate == null || maintenance.getStopStartTime() == null || maintenance.getStopEndTime() == null) {
                continue;
            }
            Date overlapStart = maintenance.getStopStartTime().after(dayStart) ? maintenance.getStopStartTime() : dayStart;
            Date overlapEnd = maintenance.getStopEndTime().before(dayEnd) ? maintenance.getStopEndTime() : dayEnd;
            if (!overlapStart.before(overlapEnd)) {
                continue;
            }
            BigDecimal hours = BigDecimal.valueOf(DateUtil.between(overlapStart, overlapEnd, DateUnit.MINUTE))
                    .divide(BigDecimal.valueOf(60), 6, RoundingMode.HALF_UP);
            candidate.setMaintenanceHours(nvl(candidate.getMaintenanceHours()).add(hours));
        }
    }

    /**
     * 从成型计划和施工信息构造胎面待排任务。
     *
     * @param context     自动排程上下文
     * @param machineList 胎面机台列表
     * @return 胎面待排任务列表
     */
    private List<TmTaskDraft> loadFormingDemandTasks(TmScheduleContext context, List<TmMachineInfo> machineList) {
        List<TmFormingDemandRowVo> rowList;
        try {
            rowList = tmAutoScheduleDataLoadMapper.selectFormingDemandRows(context.getFactoryCode(), context.getScheduleDate());
        } catch (RuntimeException ex) {
            log.warn("[TM_AUTO_SCHEDULE_LOAD] 加载成型计划和施工信息失败，scheduleDate={}，原因={}",
                    DateUtil.formatDate(context.getScheduleDate()), ex.getMessage());
            return Collections.emptyList();
        }
        if (CollUtil.isEmpty(rowList)) {
            return Collections.emptyList();
        }
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
            throw new RuntimeException(errorMsg.toString());
        }
        // 同一胎面、胶料、口型板的成型需求先聚合，避免同一库存缺口按多个成型工单重复参与计划量计算。
        List<TmFormingDemandRowVo> demandRowList = mergeFormingDemandRows(rowList);
        String algorithmCode = getParamValue(context, PARAM_ALGORITHM_SWITCH, "1");
        BigDecimal minStartQty = getDecimalParam(context, PARAM_MIN_START_QTY);
        BigDecimal defaultCurlLength = getDecimalParam(context, PARAM_DEFAULT_CURL_LENGTH);
        Integer guardShiftCount = getIntegerParam(context, PARAM_STOCK_GUARD_SHIFT_COUNT, 2);
        List<TmLossSetting> lossSettingList = loadLossSettings(context);
        TmWorkCalendarRowVo tmCalendar = loadWorkCalendar(context, PROC_CODE_TM);
        TmWorkCalendarRowVo cxCalendar = loadWorkCalendar(context, PROC_CODE_CX);
        List<TmTaskDraft> taskDraftList = new ArrayList<>();
        for (TmFormingDemandRowVo row : demandRowList) {
            String treadCode = row.getTreadCode();
            BigDecimal treadLength = nvl(row.getTreadShoulderLength());
            if (StrUtil.isBlank(treadCode) || treadLength.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal[] classQtyArray = buildClassQtyArray(row);
            boolean noShutdownAvailableShift = redistributeShutdownDemand(context, classQtyArray, tmCalendar, cxCalendar);
            for (int shiftOrder = 1; shiftOrder <= 6; shiftOrder++) {
                BigDecimal formingQty = resolveFormingQty(classQtyArray, shiftOrder, algorithmCode);
                BigDecimal demandQty = formingQty.multiply(treadLength);
                if (demandQty.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                TmTaskDraft taskDraft = new TmTaskDraft();
                taskDraft.setOrderNo(row.getOrderNo() + "-CLASS" + shiftOrder);
                taskDraft.setSourceOrderNos(row.getOrderNo());
                taskDraft.setTreadCode(treadCode);
                taskDraft.setGlueCode(row.getTreadRubberCategory());
                taskDraft.setMouthPlateCode(row.getTreadMouthPlate());
                taskDraft.setShiftOrder(shiftOrder);
                taskDraft.setTreadShoulderLength(treadLength);
                taskDraft.setTailFlag(CLOSE_OUT_TIP.equals(row.getMarkCloseOutTip()) ? YES : NO);
                taskDraft.setTailBalanceQty(nvl(row.getCxRemainQty()));
                taskDraft.setLossRate(resolveLossRate(row.getTreadCode(), lossSettingList));
                taskDraft.setCurrentShiftDemandQty(demandQty);
                taskDraft.setGuardDemandQty(calculateGuardDemand(classQtyArray, shiftOrder, guardShiftCount).multiply(treadLength));
                taskDraft.setDemandQty(demandQty);
                taskDraft.setGuardShiftCount(guardShiftCount);
                taskDraft.setMinStartQty(minStartQty);
                taskDraft.setDefaultCurlRollLength(defaultCurlLength);
                if (noShutdownAvailableShift && !isShiftOpen(tmCalendar, shiftOrder) && isShiftOpen(cxCalendar, shiftOrder)) {
                    taskDraft.setUnplannedReasonCode("TM_SHUTDOWN_NO_AVAILABLE_SHIFT");
                    taskDraft.setUnplannedReasonDesc("胎面停产且无可分配班次，成型需求无法重分配");
                }
                taskDraftList.add(taskDraft);
            }
        }
        return taskDraftList;
    }

    /**
     * 按胎面、胶料和口型板聚合成型需求。
     *
     * <p>胎面自动排程以胎面规格作为生产对象，成型工单号只用于解释追踪。这里先把相同胎面规格的
     * 班次需求合并，保证库存缺口和收尾判断只针对同一胎面需求计算一次。</p>
     *
     * @param rowList 成型需求明细
     * @return 聚合后的成型需求
     */
    private List<TmFormingDemandRowVo> mergeFormingDemandRows(List<TmFormingDemandRowVo> rowList) {
        if (CollUtil.isEmpty(rowList)) {
            return Collections.emptyList();
        }
        Map<String, TmFormingDemandRowVo> demandMap = new LinkedHashMap<>();
        for (TmFormingDemandRowVo row : rowList) {
            String key = buildDemandMergeKey(row);
            TmFormingDemandRowVo merged = demandMap.get(key);
            if (merged == null) {
                demandMap.put(key, copyDemandRow(row));
                continue;
            }
            mergeDemandQty(merged, row);
        }
        return new ArrayList<>(demandMap.values());
    }

    /**
     * 构造成型需求聚合键。
     *
     * @param row 成型需求
     * @return 聚合键
     */
    private String buildDemandMergeKey(TmFormingDemandRowVo row) {
        return StrUtil.blankToDefault(row.getTreadCode(), "")
                + "|" + StrUtil.blankToDefault(row.getTreadRubberCategory(), "")
                + "|" + StrUtil.blankToDefault(row.getTreadMouthPlate(), "");
    }

    /**
     * 复制成型需求行，避免聚合过程修改 mapper 返回的原始对象。
     *
     * @param row 原始成型需求
     * @return 可聚合的成型需求副本
     */
    private TmFormingDemandRowVo copyDemandRow(TmFormingDemandRowVo row) {
        TmFormingDemandRowVo target = new TmFormingDemandRowVo();
        target.setOrderNo(row.getOrderNo());
        target.setEmbryoCode(row.getEmbryoCode());
        target.setTreadCode(row.getTreadCode());
        target.setTreadShoulderLength(row.getTreadShoulderLength());
        target.setTreadMouthPlate(row.getTreadMouthPlate());
        target.setTreadRubberCategory(row.getTreadRubberCategory());
        target.setMarkCloseOutTip(row.getMarkCloseOutTip());
        target.setCxRemainQty(row.getCxRemainQty());
        target.setClass1PlanQty(nvl(row.getClass1PlanQty()));
        target.setClass2PlanQty(nvl(row.getClass2PlanQty()));
        target.setClass3PlanQty(nvl(row.getClass3PlanQty()));
        target.setClass4PlanQty(nvl(row.getClass4PlanQty()));
        target.setClass5PlanQty(nvl(row.getClass5PlanQty()));
        target.setClass6PlanQty(nvl(row.getClass6PlanQty()));
        return target;
    }

    /**
     * 将同规格成型需求累加到聚合行。
     *
     * @param merged 已聚合需求
     * @param row    当前成型需求
     */
    private void mergeDemandQty(TmFormingDemandRowVo merged, TmFormingDemandRowVo row) {
        merged.setOrderNo(appendSourceOrderNo(merged.getOrderNo(), row.getOrderNo()));
        merged.setClass1PlanQty(nvl(merged.getClass1PlanQty()).add(nvl(row.getClass1PlanQty())));
        merged.setClass2PlanQty(nvl(merged.getClass2PlanQty()).add(nvl(row.getClass2PlanQty())));
        merged.setClass3PlanQty(nvl(merged.getClass3PlanQty()).add(nvl(row.getClass3PlanQty())));
        merged.setClass4PlanQty(nvl(merged.getClass4PlanQty()).add(nvl(row.getClass4PlanQty())));
        merged.setClass5PlanQty(nvl(merged.getClass5PlanQty()).add(nvl(row.getClass5PlanQty())));
        merged.setClass6PlanQty(nvl(merged.getClass6PlanQty()).add(nvl(row.getClass6PlanQty())));
        if (CLOSE_OUT_TIP.equals(row.getMarkCloseOutTip())) {
            merged.setMarkCloseOutTip(CLOSE_OUT_TIP);
        }
        // 收尾余量来自施工口径，同规格多工单可能重复带出同一余量，不能随成型工单数量累加。
        merged.setCxRemainQty(nvl(merged.getCxRemainQty()).max(nvl(row.getCxRemainQty())));
    }

    /**
     * 追加来源成型工单号。
     *
     * @param existing 已有来源工单号
     * @param orderNo  当前来源工单号
     * @return 去重后的逗号分隔工单号
     */
    private String appendSourceOrderNo(String existing, String orderNo) {
        if (StrUtil.isBlank(orderNo)) {
            return existing;
        }
        if (StrUtil.isBlank(existing)) {
            return orderNo;
        }
        List<String> orderNoList = Arrays.asList(existing.split(","));
        return orderNoList.contains(orderNo) ? existing : existing + "," + orderNo;
    }

    /**
     * 加载启用的胎面损耗率配置。
     *
     * @param context 自动排程上下文
     * @return 损耗率配置列表；未配置时返回空集合
     */
    private List<TmLossSetting> loadLossSettings(TmScheduleContext context) {
        if (tmLossSettingMapper == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<TmLossSetting> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmLossSetting::getFactoryCode, context.getFactoryCode());
        wrapper.eq(TmLossSetting::getEnableStatus, YES);
        return Optional.ofNullable(tmLossSettingMapper.selectList(wrapper)).orElse(Collections.emptyList());
    }

    /**
     * 按胎面编码解析计划量阶段可使用的损耗率。
     *
     * <p>计划量计算发生在机台分配之前，因此当前只能应用胎面级和默认级配置；
     * 同层级多条配置按 priority 小值优先。</p>
     *
     * @param treadCode       胎面编码
     * @param lossSettingList 损耗率配置列表
     * @return 损耗率，未配置时返回 0
     */
    private BigDecimal resolveLossRate(String treadCode, List<TmLossSetting> lossSettingList) {
        if (CollUtil.isEmpty(lossSettingList)) {
            return BigDecimal.ZERO;
        }
        Optional<TmLossSetting> treadSetting = lossSettingList.stream()
                .filter(item -> item.getLossRate() != null)
                .filter(item -> StrUtil.isBlank(item.getMachineCode()))
                .filter(item -> StrUtil.isNotBlank(item.getTreadCode()) && item.getTreadCode().equals(treadCode))
                .min(Comparator.comparing(item -> item.getPriority() == null ? Integer.MAX_VALUE : item.getPriority()));
        if (treadSetting.isPresent()) {
            return treadSetting.get().getLossRate();
        }
        return lossSettingList.stream()
                .filter(item -> item.getLossRate() != null)
                .filter(item -> StrUtil.isBlank(item.getMachineCode()))
                .filter(item -> StrUtil.isBlank(item.getTreadCode()))
                .min(Comparator.comparing(item -> item.getPriority() == null ? Integer.MAX_VALUE : item.getPriority()))
                .map(TmLossSetting::getLossRate)
                .orElse(BigDecimal.ZERO);
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
        if (!YES.equals(getParamValue(context, PARAM_SHUTDOWN_REDISTRIBUTION_ENABLED, YES))) {
            return false;
        }
        if (!isShutdownDay(tmCalendar) || isShutdownDay(cxCalendar)) {
            return false;
        }
        List<Integer> shutdownShiftList = new ArrayList<>();
        List<Integer> availableShiftList = new ArrayList<>();
        BigDecimal shutdownQty = BigDecimal.ZERO;
        for (int shiftOrder = 1; shiftOrder <= 6; shiftOrder++) {
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
            log.warn("[TM_AUTO_SCHEDULE_SHUTDOWN] factoryCode={}, scheduleDate={} 胎面停产且无可分配班次",
                    context.getFactoryCode(), DateUtil.formatDate(context.getScheduleDate()));
            return true;
        }
        BigDecimal increaseQty = shutdownQty.divide(new BigDecimal(availableShiftList.size()), 6, RoundingMode.HALF_UP);
        for (Integer shiftOrder : shutdownShiftList) {
            classQtyArray[shiftOrder - 1] = BigDecimal.ZERO;
        }
        for (Integer shiftOrder : availableShiftList) {
            classQtyArray[shiftOrder - 1] = classQtyArray[shiftOrder - 1].add(increaseQty);
        }
        log.info("[TM_AUTO_SCHEDULE_SHUTDOWN] factoryCode={}, scheduleDate={}, shutdownQty={}, availableShiftCount={}",
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
            String cacheKey = context.getFactoryCode() + ":" + procCode + ":" + DateUtil.formatDate(calendarDate);
            List<TmWorkCalendarRowVo> rowList = getCachedList(calendarCacheMap, cacheKey,
                    () -> tmAutoScheduleDataLoadMapper.selectWorkCalendarRows(context.getFactoryCode(), procCode, calendarDate));
            return CollUtil.isEmpty(rowList) ? null : rowList.get(0);
        } catch (RuntimeException ex) {
            log.warn("[TM_AUTO_SCHEDULE_LOAD] 加载工作日历失败，procCode={}，scheduleDate={}，原因={}",
                    procCode, DateUtil.formatDate(context.getScheduleDate()), ex.getMessage());
            return null;
        }
    }

    /**
     * 读取带短 TTL 的本地集合缓存。
     *
     * @param cacheMap 缓存容器
     * @param cacheKey 缓存 key
     * @param loader   缓存失效时的数据加载器
     * @param <T>      集合元素类型
     * @return 集合副本
     */
    private <T> List<T> getCachedList(Map<String, TmLocalCacheEntry<List<T>>> cacheMap, String cacheKey,
                                      Supplier<List<T>> loader) {
        long now = System.currentTimeMillis();
        TmLocalCacheEntry<List<T>> entry = cacheMap.get(cacheKey);
        if (entry == null || entry.isExpired(now)) {
            List<T> value = Optional.ofNullable(loader.get()).orElse(Collections.emptyList());
            entry = new TmLocalCacheEntry<>(new ArrayList<>(value), now + BASE_DATA_CACHE_TTL_MILLIS);
            cacheMap.put(cacheKey, entry);
        }
        return new ArrayList<>(entry.getValue());
    }

    private boolean isShutdownDay(TmWorkCalendarRowVo calendar) {
        if (calendar == null) {
            return false;
        }
        return NO.equals(calendar.getDayFlag())
                || (!isShiftOpen(calendar, 1) && !isShiftOpen(calendar, 2) && !isShiftOpen(calendar, 3));
    }

    private boolean isShiftOpen(TmWorkCalendarRowVo calendar, int shiftOrder) {
        if (calendar == null) {
            return true;
        }
        int calendarShift = mapToCalendarShift(shiftOrder);
        if (calendarShift == 1) {
            return YES.equals(calendar.getOneShiftFlag());
        }
        if (calendarShift == 2) {
            return YES.equals(calendar.getTwoShiftFlag());
        }
        return YES.equals(calendar.getThreeShiftFlag());
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

    private BigDecimal resolveFormingQty(BigDecimal[] classQtyArray, int shiftOrder, String algorithmCode) {
        if ("2".equals(algorithmCode)) {
            int nextIndex = Math.min(shiftOrder, classQtyArray.length - 1);
            return classQtyArray[nextIndex];
        }
        return Arrays.stream(classQtyArray, 0, Math.min(3, classQtyArray.length))
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal calculateGuardDemand(BigDecimal[] classQtyArray, int shiftOrder, int guardShiftCount) {
        BigDecimal total = BigDecimal.ZERO;
        int startIndex = Math.max(shiftOrder - 1, 0);
        int endIndex = Math.min(startIndex + Math.max(guardShiftCount, 1), classQtyArray.length);
        for (int i = startIndex; i < endIndex; i++) {
            total = total.add(classQtyArray[i]);
        }
        return total;
    }

    private BigDecimal[] buildClassQtyArray(TmFormingDemandRowVo row) {
        return new BigDecimal[]{
                nvl(row.getClass1PlanQty()),
                nvl(row.getClass2PlanQty()),
                nvl(row.getClass3PlanQty()),
                nvl(row.getClass4PlanQty()),
                nvl(row.getClass5PlanQty()),
                nvl(row.getClass6PlanQty())
        };
    }

    /**
     * 判断机台是否启用。
     *
     * @param machineInfo 机台基础资料
     * @return true 表示可参与排程
     */
    private boolean isMachineEnabled(TmMachineInfo machineInfo) {
        return machineInfo != null && (StrUtil.isBlank(machineInfo.getMachineStatus()) || YES.equals(machineInfo.getMachineStatus()));
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

    private void putDefaultParam(Map<String, TmParamValue> paramMap, String paramCode, String defaultValue) {
        if (paramMap.containsKey(paramCode)) {
            return;
        }
        TmParamValue value = new TmParamValue();
        value.setParamCode(paramCode);
        value.setDefaultValue(defaultValue);
        value.setSource("DEFAULT");
        paramMap.put(paramCode, value);
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
