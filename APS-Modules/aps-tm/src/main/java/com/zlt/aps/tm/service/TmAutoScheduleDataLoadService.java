package com.zlt.aps.tm.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.engine.utils.DepthConfigResolver;
import com.zlt.aps.tm.api.domain.entity.*;
import com.zlt.aps.tm.domain.vo.TmExperimentSpecMonthPlanRowVo;
import com.zlt.aps.tm.domain.vo.TmFormingDemandRowVo;
import com.zlt.aps.tm.domain.vo.TmWorkCalendarRowVo;
import com.zlt.aps.tm.engine.domain.*;
import com.zlt.aps.tm.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

    private static final String PARAM_ALGORITHM_SWITCH = "TM_ALGORITHM_SWITCH";

    private static final String PARAM_MIN_STOCK_CLASS = "TM_MIN_STOCK_CLASS";

    private static final String PARAM_MIN_START_QTY = "TM_MIN_START_QTY";

    private static final String PARAM_DEFAULT_CURL_LENGTH = "TM_DEFAULT_CURL_LENGTH";

    private static final String PARAM_TOOL_TOTAL_QTY = "TM_TOOL_TOTAL_QTY";

    private static final String PARAM_SHUTDOWN_REDISTRIBUTION_ENABLED = "TM_SHUTDOWN_REDISTRIBUTION_ENABLED";

    private static final String PARAM_PLAN_QTY_STRATEGY = "TM_PLAN_QTY_STRATEGY";

    private static final String PARAM_TASK_SORT_STRATEGY = "TM_TASK_SORT_STRATEGY";

    private static final String PARAM_NEW_SPEC_LOOKBACK_DAYS = "TM_NEW_SPEC_LOOKBACK_DAYS";

    private static final String PARAM_NEW_SPEC_ADVANCE_SHIFT_COUNT = "TM_NEW_SPEC_ADVANCE_SHIFT_COUNT";

    private static final String PARAM_EXPERIMENT_SPEC_LOOKBACK_DAYS = "TM_EXPERIMENT_SPEC_LOOKBACK_DAYS";

    private static final String PARAM_EXPERIMENT_SPEC_PLAN_QTY = "TM_EXPERIMENT_SPEC_PLAN_QTY";

    private static final String PARAM_FORMING_SHIFT_OFFSET = "TM_FORMING_SHIFT_OFFSET";

    private static final String PARAM_SMALL_GLUE_CODES = "TM_SMALL_GLUE_CODES";

    private static final String PROC_CODE_CX = "03";

    private static final String PROC_CODE_TM = "04";

    private static final String YES = "1";

    private static final String NO = "0";

    private static final String CLOSE_OUT_TIP = "0";

    private static final String JOB_TYPE_ALLOW = "0";

    private static final String JOB_TYPE_FORBID = "1";

    private static final String CONSTRUCTION_STAGE_EXPERIMENT = "01";

    private static final int EXPERIMENT_SPEC_SHIFT_ORDER = 1;


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
    private TmAutoScheduleRedisCacheService tmAutoScheduleRedisCacheService = new TmAutoScheduleRedisCacheService();

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
        context.setLossRuleList(loadLossRules(context));
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
        List<TmParams> paramsList = tmAutoScheduleRedisCacheService.getCachedList("params:" + context.getFactoryCode(),
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
        putDefaultParam(paramMap, PARAM_MIN_STOCK_CLASS, "1");
        putDefaultParam(paramMap, PARAM_MIN_START_QTY, "0");
        putDefaultParam(paramMap, PARAM_DEFAULT_CURL_LENGTH, "0");
        putDefaultParam(paramMap, PARAM_TOOL_TOTAL_QTY, "0");
        putDefaultParam(paramMap, PARAM_SHUTDOWN_REDISTRIBUTION_ENABLED, "1");
        putDefaultParam(paramMap, PARAM_PLAN_QTY_STRATEGY, "DEFAULT");
        putDefaultParam(paramMap, PARAM_TASK_SORT_STRATEGY, "DEFAULT");
        putDefaultParam(paramMap, PARAM_NEW_SPEC_LOOKBACK_DAYS, "7");
        putDefaultParam(paramMap, PARAM_NEW_SPEC_ADVANCE_SHIFT_COUNT, "2");
        putDefaultParam(paramMap, PARAM_EXPERIMENT_SPEC_LOOKBACK_DAYS, "5");
        putDefaultParam(paramMap, PARAM_EXPERIMENT_SPEC_PLAN_QTY, "30");
        putDefaultParam(paramMap, PARAM_FORMING_SHIFT_OFFSET, "2");
        putDefaultParam(paramMap, PARAM_SMALL_GLUE_CODES, "");
        context.setParamMap(paramMap);
        context.setSmallGlueCodeSet(this.parseSmallGlueCodes(paramMap.get(PARAM_SMALL_GLUE_CODES)));
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
        return tmAutoScheduleRedisCacheService.getCachedList("machine:" + context.getFactoryCode(),
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
        fillCandidatePredecessor(context, candidateMap);
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
        if (rowList == null) {
            rowList = Collections.emptyList();
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
        // 成型来源需求不在数据加载阶段聚合，解释表需要逐条追溯原成型排程结果。
        List<TmFormingDemandRowVo> demandRowList = rowList;
        String algorithmCode = getParamValue(context, PARAM_ALGORITHM_SWITCH, "1");
        BigDecimal minStartQty = getDecimalParam(context, PARAM_MIN_START_QTY);
        BigDecimal defaultCurlLength = getDecimalParam(context, PARAM_DEFAULT_CURL_LENGTH);
        BigDecimal toolTotalQty = getDecimalParam(context, PARAM_TOOL_TOTAL_QTY);
        Integer fallbackGuardShiftCount = getIntegerParam(context, PARAM_MIN_STOCK_CLASS, 1);
        Integer newSpecLookbackDays = getPositiveIntegerParam(context, PARAM_NEW_SPEC_LOOKBACK_DAYS, 7);
        Integer newSpecAdvanceShiftCount = getPositiveIntegerParam(context, PARAM_NEW_SPEC_ADVANCE_SHIFT_COUNT, 2);
        Integer formingShiftOffset = getNonNegativeIntegerParam(context, PARAM_FORMING_SHIFT_OFFSET, 2);
        Map<String, TmNewSpecInfo> newSpecInfoMap = buildNewSpecInfoMap(context, demandRowList,
                newSpecLookbackDays, newSpecAdvanceShiftCount);
        List<TmLossRule> lossRuleList = context.getLossRuleList();
        List<TmDepthConfig> depthConfigList = this.loadDepthConfigs(context);
        TmWorkCalendarRowVo tmCalendar = loadWorkCalendar(context, PROC_CODE_TM);
        TmWorkCalendarRowVo cxCalendar = loadWorkCalendar(context, PROC_CODE_CX);
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
            Integer guardShiftCount = this.resolveGuardShiftCount(context, row, depthConfigList, fallbackGuardShiftCount);
            boolean noShutdownAvailableShift = redistributeShutdownDemand(context, classQtyArray, tmCalendar, cxCalendar);
            for (int shiftOrder = 1; shiftOrder <= 6; shiftOrder++) {
                BigDecimal formingQty = resolveFormingQty(classQtyArray, shiftOrder, algorithmCode, formingShiftOffset);
                BigDecimal demandQty = formingQty.multiply(treadLength);
                if (demandQty.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                TmNewSpecInfo taskNewSpecInfo = buildTaskNewSpecInfo(newSpecInfoMap.get(treadCode), shiftOrder, demandQty);
                int targetShiftOrder = resolveTargetShiftOrder(taskNewSpecInfo, shiftOrder);
                TmTaskDraft taskDraft = new TmTaskDraft();
                taskDraft.setOrderNo(row.getOrderNo() + "-CLASS" + shiftOrder);
                taskDraft.setSourceOrderNos(row.getOrderNo());
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
                taskDraft.setNewSpecInfo(taskNewSpecInfo);
                taskDraft.setTreadShoulderLength(treadLength);
                taskDraft.setTailFlag(CLOSE_OUT_TIP.equals(row.getMarkCloseOutTip()) ? YES : NO);
                taskDraft.setTailBalanceQty(nvl(row.getCxRemainQty()));
                taskDraft.setCurrentShiftDemandQty(demandQty);
                taskDraft.setGuardDemandQty(calculateGuardDemand(classQtyArray, shiftOrder, guardShiftCount,
                        formingShiftOffset).multiply(treadLength));
                taskDraft.setDemandQty(demandQty);
                taskDraft.setGuardShiftCount(guardShiftCount);
                taskDraft.setMinStartQty(minStartQty);
                taskDraft.setDefaultCurlRollLength(defaultCurlLength);
                if (toolTotalQty.compareTo(BigDecimal.ZERO) > 0) {
                    taskDraft.setTotalToolQty(toolTotalQty);
                }
                if (noShutdownAvailableShift && !isShiftOpen(tmCalendar, targetShiftOrder) && isShiftOpen(cxCalendar, shiftOrder)) {
                    taskDraft.setUnplannedReasonCode("TM_SHUTDOWN_NO_AVAILABLE_SHIFT");
                    taskDraft.setUnplannedReasonDesc("胎面停产且无可分配班次，成型需求无法重分配");
                }
                taskDraftList.add(taskDraft);
            }
        }
        appendExperimentSpecTasks(context, taskDraftList, lossRuleList, minStartQty, defaultCurlLength, toolTotalQty);
        return taskDraftList;
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
        Integer lookbackDays = getPositiveIntegerParam(context, PARAM_EXPERIMENT_SPEC_LOOKBACK_DAYS, 5);
        BigDecimal experimentPlanQty = getPositiveDecimalParam(context, PARAM_EXPERIMENT_SPEC_PLAN_QTY, BigDecimal.valueOf(30));
        if (experimentPlanQty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        Date experimentPlanDate = DateUtil.offsetDay(context.getScheduleDate(), -lookbackDays);
        String dayColumn = buildExperimentDayColumn(experimentPlanDate);
        Integer yearMonth = Integer.valueOf(DateUtil.format(experimentPlanDate, "yyyyMM"));
        List<TmExperimentSpecMonthPlanRowVo> rowList;
        try {
            rowList = tmAutoScheduleDataLoadMapper.selectExperimentSpecMonthPlanRows(context.getFactoryCode(), yearMonth,
                    dayColumn, experimentPlanDate);
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
                .filter(row -> row != null && CONSTRUCTION_STAGE_EXPERIMENT.equals(row.getConstructionStage()))
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
        info.setLookbackDaysSource(getPositiveIntegerParamSource(context, PARAM_EXPERIMENT_SPEC_LOOKBACK_DAYS, 5));
        info.setPlanQty(experimentPlanQty);
        info.setPlanQtySource(getPositiveDecimalParamSource(context, PARAM_EXPERIMENT_SPEC_PLAN_QTY));
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
                .filter(task -> Integer.valueOf(EXPERIMENT_SPEC_SHIFT_ORDER).equals(task.getShiftOrder()))
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
                + "-CLASS" + EXPERIMENT_SPEC_SHIFT_ORDER);
        taskDraft.setSourceOrderNos(appendSourceOrderNos(null, experimentSpecInfo.getProductionNos()));
        taskDraft.setBusinessKeySuffix("EXP-" + StrUtil.blankToDefault(row.getProductionNo(), String.valueOf(row.getMonthPlanId()))
                + "-CLASS" + EXPERIMENT_SPEC_SHIFT_ORDER);
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
        taskDraft.setShiftOrder(EXPERIMENT_SPEC_SHIFT_ORDER);
        taskDraft.setTreadShoulderLength(nvl(row.getTreadShoulderLength()));
        taskDraft.setTailFlag(NO);
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
        String sourceKey = StrUtil.blankToDefault(sourceOrderNo, "ROW" + sourceRowIndex);
        return sourceKey + "-CLASS" + shiftOrder + "-ROW" + sourceRowIndex;
    }

    /**
     * 构建胎面新规格判断结果。
     *
     * @param context 自动排程上下文
     * @param demandRowList 成型需求行
     * @param lookbackDays 回看天数
     * @param advanceShiftCount 提前班次数
     * @return 胎面编码到新规格证据的映射
     */
    private Map<String, TmNewSpecInfo> buildNewSpecInfoMap(TmScheduleContext context,
                                                           List<TmFormingDemandRowVo> demandRowList,
                                                           Integer lookbackDays,
                                                           Integer advanceShiftCount) {
        Map<String, TmNewSpecInfo> resultMap = new HashMap<>();
        if (CollUtil.isEmpty(demandRowList)) {
            return resultMap;
        }
        List<String> treadCodes = demandRowList.stream()
                .map(TmFormingDemandRowVo::getTreadCode)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(treadCodes)) {
            return resultMap;
        }
        Date previousDate = DateUtil.offsetDay(context.getScheduleDate(), -1);
        Date historyStartDate = DateUtil.offsetDay(context.getScheduleDate(), -lookbackDays);
        Map<String, BigDecimal> previousStockMap = queryPreviousDayStockMap(context, treadCodes, previousDate);
        Map<String, Boolean> historyPlanMap = queryHistoryPlanExistsMap(context, treadCodes, historyStartDate, previousDate);
        String lookbackSource = getPositiveIntegerParamSource(context, PARAM_NEW_SPEC_LOOKBACK_DAYS, 7);
        String advanceSource = getPositiveIntegerParamSource(context, PARAM_NEW_SPEC_ADVANCE_SHIFT_COUNT, 2);
        for (String treadCode : treadCodes) {
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
     * @return 按机台数量降序排列的库存保证班数配置；未配置或查询失败时返回空集合
     */
    private List<TmDepthConfig> loadDepthConfigs(TmScheduleContext context) {
        if (tmDepthConfigMapper == null) {
            return Collections.emptyList();
        }
        try {
            LambdaQueryWrapper<TmDepthConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TmDepthConfig::getFactoryCode, context.getFactoryCode());
            wrapper.orderByDesc(TmDepthConfig::getMachineQty);
            return Optional.ofNullable(tmDepthConfigMapper.selectList(wrapper)).orElse(Collections.emptyList()).stream()
                    .sorted(Comparator.comparing((TmDepthConfig config) -> config.getMachineQty() == null
                            ? Integer.MIN_VALUE : config.getMachineQty()).reversed())
                    .collect(Collectors.toList());
        } catch (RuntimeException ex) {
            log.warn("[TM_DEPTH_CONFIG_LOAD] factoryCode={} 加载库存保证班数配置失败，原因={}，将回退参数 {}",
                    context.getFactoryCode(), ex.getMessage(), PARAM_MIN_STOCK_CLASS);
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
     * @param row                     成型需求行
     * @param depthConfigList         库存保证班数配置
     * @param fallbackGuardShiftCount 参数兜底库存保证班数
     * @return 当前成型来源使用的库存保证班数
     */
    private Integer resolveGuardShiftCount(TmScheduleContext context, TmFormingDemandRowVo row,
                                           List<TmDepthConfig> depthConfigList, Integer fallbackGuardShiftCount) {
        Integer lhMachineQty = this.resolveLhMachineQty(row.getLhMachineCode());
        if (lhMachineQty == null) {
            log.warn("[TM_DEPTH_CONFIG_MATCH] factoryCode={}, orderNo={} 硫化机编码为空或无法解析，回退参数 {}={}",
                    context.getFactoryCode(), row.getOrderNo(), PARAM_MIN_STOCK_CLASS, fallbackGuardShiftCount);
            return fallbackGuardShiftCount;
        }
        if (CollUtil.isEmpty(depthConfigList)) {
            log.warn("[TM_DEPTH_CONFIG_MATCH] factoryCode={}, orderNo={}, lhMachineQty={} 未维护库存保证班数配置，回退参数 {}={}",
                    context.getFactoryCode(), row.getOrderNo(), lhMachineQty, PARAM_MIN_STOCK_CLASS, fallbackGuardShiftCount);
            return fallbackGuardShiftCount;
        }
        Optional<TmDepthConfig> exactConfigOptional = depthConfigList.stream()
                .filter(depthConfig -> "EQ".equals(depthConfig.getMachineRange())
                        && Objects.equals(depthConfig.getMachineQty(), lhMachineQty))
                .findFirst();
        if (exactConfigOptional.isPresent()) {
            return this.resolveMatchedGuardShiftCount(context, row, lhMachineQty, exactConfigOptional.get(),
                    fallbackGuardShiftCount);
        }
        for (TmDepthConfig depthConfig : depthConfigList) {
            DepthConfigResolver.DepthConfigVo configVo = new DepthConfigResolver.DepthConfigVo(
                    depthConfig.getMachineQty(), depthConfig.getMachineRange(), depthConfig.getDepthClassQty());
            BigDecimal matchedDepthClassQty = DepthConfigResolver.resolveDepthClassQty(lhMachineQty,
                    Collections.singletonList(configVo));
            if (matchedDepthClassQty == null) {
                continue;
            }
            return this.resolveMatchedGuardShiftCount(context, row, lhMachineQty, depthConfig, fallbackGuardShiftCount);
        }
        log.warn("[TM_DEPTH_CONFIG_MATCH] factoryCode={}, orderNo={}, lhMachineQty={} 未命中库存保证班数配置，回退参数 {}={}",
                context.getFactoryCode(), row.getOrderNo(), lhMachineQty, PARAM_MIN_STOCK_CLASS, fallbackGuardShiftCount);
        return fallbackGuardShiftCount;
    }

    /**
     * 将已命中的深度配置转换为库存保证班数。
     *
     * @param context                 自动排程上下文
     * @param row                     成型需求行
     * @param lhMachineQty            硫化机数量
     * @param depthConfig             已命中的深度配置
     * @param fallbackGuardShiftCount 参数兜底库存保证班数
     * @return 当前成型来源使用的库存保证班数
     */
    private Integer resolveMatchedGuardShiftCount(TmScheduleContext context, TmFormingDemandRowVo row,
                                                  Integer lhMachineQty, TmDepthConfig depthConfig,
                                                  Integer fallbackGuardShiftCount) {
        Integer guardShiftCount = this.toPositiveIntegerDepthClassQty(depthConfig.getDepthClassQty());
        if (guardShiftCount != null) {
            return guardShiftCount;
        }
        log.warn("[TM_DEPTH_CONFIG_MATCH] factoryCode={}, orderNo={}, lhMachineQty={}, machineRange={}, machineQty={}, depthClassQty={} 不是正整数，回退参数 {}={}",
                context.getFactoryCode(), row.getOrderNo(), lhMachineQty, depthConfig.getMachineRange(),
                depthConfig.getMachineQty(), depthConfig.getDepthClassQty(), PARAM_MIN_STOCK_CLASS, fallbackGuardShiftCount);
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
     * 加载启用的胎面损耗率配置。
     *
     * @param context 自动排程上下文
     * @return 损耗率配置列表；未配置时返回空集合
     */
    private List<TmLossRule> loadLossRules(TmScheduleContext context) {
        if (tmLossSettingMapper == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<TmLossSetting> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmLossSetting::getFactoryCode, context.getFactoryCode());
        wrapper.eq(TmLossSetting::getEnableStatus, YES);
        return Optional.ofNullable(tmLossSettingMapper.selectList(wrapper)).orElse(Collections.emptyList())
                .stream()
                .map(this::buildLossRule)
                .collect(Collectors.toList());
    }

    /**
     * 将业务损耗率配置映射为引擎规则对象。
     *
     * @param setting 损耗率业务实体
     * @return 引擎损耗率规则
     */
    private TmLossRule buildLossRule(TmLossSetting setting) {
        TmLossRule rule = new TmLossRule();
        rule.setFactoryCode(setting.getFactoryCode());
        rule.setTreadCode(setting.getTreadCode());
        rule.setMachineCode(setting.getMachineCode());
        rule.setLossRate(setting.getLossRate());
        rule.setPriority(setting.getPriority());
        return rule;
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
            String cacheKey = "calendar:" + context.getFactoryCode() + ":" + procCode + ":" + DateUtil.formatDate(calendarDate);
            List<TmWorkCalendarRowVo> rowList = tmAutoScheduleRedisCacheService.getCachedList(cacheKey,
                    () -> tmAutoScheduleDataLoadMapper.selectWorkCalendarRows(context.getFactoryCode(), procCode, calendarDate));
            return CollUtil.isEmpty(rowList) ? null : rowList.get(0);
        } catch (RuntimeException ex) {
            log.warn("[TM_AUTO_SCHEDULE_LOAD] 加载工作日历失败，procCode={}，scheduleDate={}，原因={}",
                    procCode, DateUtil.formatDate(context.getScheduleDate()), ex.getMessage());
            return null;
        }
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
     * 计算库存保证范围内的成型需求计划量。
     *
     * @param classQtyArray 成型班次计划量数组
     * @param shiftOrder 胎面排程班次，从 1 开始
     * @param guardShiftCount 库存最低保证班数
     * @param formingShiftOffset 胎面班次到成型班次的偏移量
     * @return 库存保证范围内的成型计划量合计
     */
    private BigDecimal calculateGuardDemand(BigDecimal[] classQtyArray, int shiftOrder, int guardShiftCount,
            int formingShiftOffset) {
        BigDecimal total = BigDecimal.ZERO;
        int startIndex = resolveFormingStartIndex(shiftOrder, formingShiftOffset);
        int count = Math.max(guardShiftCount, 1);
        for (int index = startIndex; index < startIndex + count; index++) {
            total = total.add(readClassQty(classQtyArray, index));
        }
        return total;
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

    /**
     * 解析小胶种参数编码集合。
     *
     * @param paramValue 参数快照值
     * @return 小胶种编码集合；参数为空时返回空集合
     */
    private Set<String> parseSmallGlueCodes(TmParamValue paramValue) {
        if (paramValue == null) {
            return new LinkedHashSet<>();
        }
        String effectiveValue = StrUtil.blankToDefault(paramValue.getParamValue(), paramValue.getDefaultValue());
        if (StrUtil.isBlank(effectiveValue)) {
            return new LinkedHashSet<>();
        }
        return Arrays.stream(effectiveValue.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
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
            return "DEFAULT";
        }
        try {
            Integer parsedValue = Integer.valueOf(value.getParamValue());
            return parsedValue.compareTo(0) > 0 ? "PARAM" : "DEFAULT";
        } catch (NumberFormatException ex) {
            return "DEFAULT";
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
            return "DEFAULT";
        }
        try {
            BigDecimal parsedValue = new BigDecimal(value.getParamValue());
            return parsedValue.compareTo(BigDecimal.ZERO) > 0 ? "PARAM" : "DEFAULT";
        } catch (NumberFormatException ex) {
            return "DEFAULT";
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
