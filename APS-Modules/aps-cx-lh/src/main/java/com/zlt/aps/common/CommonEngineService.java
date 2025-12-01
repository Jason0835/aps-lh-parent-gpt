package com.zlt.aps.common;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.config.CxShiftConfig;
import com.zlt.aps.constants.CxEngineConstants;
import com.zlt.aps.cx.service.ICxScheduleStopInfoService;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxParams;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.cxlh.cx.api.domain.vo.CxMachineInfoVo;
import com.zlt.aps.cxlh.cx.api.domain.vo.CxProductConstructionInfoDto;
import com.zlt.aps.cxlh.cx.api.domain.vo.LhAlgorithmScheduleResultDto;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.monthplan.api.domain.entity.CxEmbryoMonthPlanSurplus;
import com.zlt.aps.monthplan.api.domain.entity.ProductMoldingLimit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 成型排程单任务排载核心逻辑实现
 *
 * @author 16799
 */
@Component("CommonEngineService")
@Slf4j
public class CommonEngineService extends CommonLogService {

    @Resource
    private ICxScheduleStopInfoService iCxScheduleStopInfoService;

    /**
     * 班制配置类
     */
    public static CxShiftConfig cxShiftConfig;

    /**
     * <成型参数, 成型参数值上下文>
     **/
    public static final Map<String, CxParams> CX_PARAMS_CONTEXT_MAP = new ConcurrentHashMap<>();

    /**
     * <机台寸口上下文>
     */
    public static  final Map<String, BigDecimal> CX_MACHINE_QUOTA_CONTEXT_MAP = new ConcurrentHashMap<>();

    /**
     * <成型机编号, 成型机对象上下文 >
     **/
    public static final Map<String, CxMachineInfoVo> CX_MACHINE_ACCTIVE_TIME_CONTEXT_MAP = new ConcurrentHashMap<>();


    /**
     * title：日期格式化
     * 指定格式 yyyy-MM-dd HH:mm:ss
     */
    public DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public final static String FORMATTER_STR = "yyyy-MM-dd HH:mm:ss";
    @Autowired
    private CommonCacheService commonCacheService;

    public String formatDate(Date date) {
        return DateUtils.parseDateToStr(FORMATTER_STR, date);
    }
    
    /**
     * title：规格机台分配服务
     *
     * @param resultDtoList 分组后的任务列表
     */
    protected void allocateMachinesForProduction(List<LhAlgorithmScheduleResultDto> resultDtoList) {
        logInfo("开始任务分配机台流程，总任务数: {}", resultDtoList.size());

        // 阶段一：处理大规格轮胎
        logInfo("=== 阶段一：处理大规格轮胎 ===");
        processTireSpecification(resultDtoList, true,
                createLargeTireComparator(),
                createLargeMachineComparator());

        // 阶段二：处理小规格轮胎
        logInfo("=== 阶段二：处理小规格轮胎 ===");
        processTireSpecification(resultDtoList, false,
                createSmallTireComparator(),
                createSmallMachineComparator());

        logInfo("分配机台流程完成，已处理任务数: {}", resultDtoList.size());
    }

//========================= 核心处理流程 =========================//

    /**
     * 通用轮胎规格处理流程
     *
     * @param isLarge           是否处理大规格
     * @param tireComparator    任务排序策略
     * @param machineComparator 机台排序策略
     */
    private void processTireSpecification(List<LhAlgorithmScheduleResultDto> resultList,
                                          boolean isLarge,
                                          Comparator<LhAlgorithmScheduleResultDto> tireComparator,
                                          Comparator<CxMachineInfoVo> machineComparator) {
        String specType = isLarge ? "大规格" : "小规格";
        logInfo("开始处理{}轮胎，初始任务数: {}", specType, resultList.size());

        // 0.过滤条件处理
        Predicate<LhAlgorithmScheduleResultDto> sizePredicate = isLarge ?
                LhAlgorithmScheduleResultDto::getIsLargeTire :
                LhAlgorithmScheduleResultDto::getIsSmallTire;

        // 1. 按规格类型过滤并排序任务
        List<LhAlgorithmScheduleResultDto> filteredList = resultList.stream()
                // 过滤空元素
                .filter(Objects::nonNull)
                .filter(sizePredicate)
                .sorted(tireComparator)
                .collect(Collectors.toList());

        logDebug("{}过滤后任务数: {}", specType, filteredList.size());
        if (filteredList.isEmpty()) {
            logWarn("{}没有待处理任务", specType);
            return;
        }

        // 2. 遍历处理每个任务
        for (LhAlgorithmScheduleResultDto task : filteredList) {
            logDebug("正在处理{}任务[ID:{}]，",
                    specType, task.getLhScheduleResult().getMergeIds());
            processSingleTask(task, machineComparator);
        }
    }

    //========================= 任务分配流程 =========================//

    /**
     * title：处理单个任务分配流程
     *
     * @param task              任务
     * @param machineComparator 机台排序策略
     */
    private void processSingleTask(LhAlgorithmScheduleResultDto task,
                                   Comparator<CxMachineInfoVo> machineComparator) {
        logInfo("开始处理任务胎胚[{}][任务详细:{}] "
                , task.getLhScheduleResult().getEmbryoCode(),buildStringFromEntity(task));

        // 1. 筛选可用机台
        List<CxMachineInfoVo> availableMachines = getAvailableMachines(task);
        if (availableMachines.isEmpty()) {
            logWarn("任务[ID:{}] 无可用机台，标记为终止排产", task.getLhScheduleResult().getMergeIds());
            task.setStopScheduleReason(task.getStopScheduleReason() == null ?  "无可用机台，标记为终止排产" : (task.getStopScheduleReason() + "无可用机台，标记为终止排产"));
            handleUnScheduleAbleTask(task);
            return;
        }
        logDebug("任务[ID:{}] 候选机台数: {}", task.getLhScheduleResult().getMergeIds(), availableMachines.size());

        // 2. 可用机台评估[施工相似度],[剩余产能],[T日预做量]
        prepareMachineEvaluation(task, availableMachines);

        // 3. 排序候选机台
        availableMachines.sort(machineComparator);
        logDebug("排序后机台列表: {}",
                availableMachines.stream()
                        .map(CxMachineInfoVo::getMoldingMachineCode)
                        .collect(Collectors.toList()));

        // 4. 尝试分配机台
        if (!tryAllocateMachine(task, availableMachines)) {

            if (task.getIsLimitTire() && commonCacheService.getCxRePlanLimitMachineSwitch(CX_PARAMS_CONTEXT_MAP)) {
                log.warn("任务[ID:{}] 机台分配失败，并且是限制作业，开始抢占续作机台", task.getLhScheduleResult().getMergeIds());
                reScheduleAbleTask(task, availableMachines);
                return;
            }


            if (task.getLastOccupiedMachines() != null && !task.getLastOccupiedMachines().isEmpty()) {
                // 获取第一个机台进行继续安排
                Optional<String> machineOpt = task.getLastOccupiedMachines().stream()
                        .findFirst()
                        .map(Object::toString);

                String machine = machineOpt.get();
                CxMachineInfoVo machineContext = CX_MACHINE_ACCTIVE_TIME_CONTEXT_MAP.get(machine);

                if (machineContext != null) {
                    log.debug("任务[ID:{}] 使用前一天已占用的机台[{}]继续生产",
                            task.getLhScheduleResult().getMergeIds(), machine);
                    if (machine.equals("L102")){
                        int a = 1;
                    }
                    allocateToMachine(task, machineContext);
                } else {
                    log.warn("任务[ID:{}] 无法使用机台[{}]，该机台上下文不存在",
                            task.getLhScheduleResult().getMergeIds(), machine);
                }
                return;
            }



            log.warn("任务[ID:{}] 机台分配失败", task.getLhScheduleResult().getMergeIds());
            task.setStopScheduleReason(task.getStopScheduleReason() == null ? "": task.getStopScheduleReason() + "所有候选机台均无法分配");
            task.setStopScheduleReason(task.getStopScheduleReason() == null ? "": task.getStopScheduleReason() + "机台分配失败");
            handleUnScheduleAbleTask(task);
        } else {
            logInfo("任务[ID:{}] 成功分配机台", task.getLhScheduleResult().getMergeIds());
        }
    }


    /**
     * title：任务获取可用机台列表（需要重写）
     *
     * @param task 任务
     * @return 可用机台列表
     */
    public List<CxMachineInfoVo> getAvailableMachines(LhAlgorithmScheduleResultDto task) {
        // todo 继续类实现筛选机台方法
        return new ArrayList<>();
    }


    /**
     * 准备机台评估数据（相似度/剩余产能）
     */
    private void prepareMachineEvaluation(LhAlgorithmScheduleResultDto task,
                                          List<CxMachineInfoVo> machines) {
        calculateSimilarity(task, machines);
        calculateTheRemainingCapacity(task, machines);
        calculateTheExpectedTireQuantity(task, -1);
    }

    public void calculateTheExpectedTireQuantity(LhAlgorithmScheduleResultDto task, int specialPlanNum) {
        // todo 继续类计算T日预做的量
    }

    public void calculateTheRemainingCapacity(LhAlgorithmScheduleResultDto task, List<CxMachineInfoVo> machines) {
        // todo 继续类计算剩余产能
    }

    public void calculateSimilarity(LhAlgorithmScheduleResultDto task, List<CxMachineInfoVo> machines) {
        // todo 继续类计算施工相似度
    }


    /**
     * 任务尝试分配机台逻辑
     * ========================= 机台分配核心 =========================
     *
     * @param task       任务
     * @param candidates 候选机台列表
     * @return 是否分配成功
     */
    private boolean tryAllocateMachine(LhAlgorithmScheduleResultDto task,
                                       List<CxMachineInfoVo> candidates) {
        logDebug("尝试为任务[ID:{}]分配机台，候选机台数: {}", task.getLhScheduleResult().getMergeIds(), candidates.size());

        for (CxMachineInfoVo machine : candidates) {
            logDebug("尝试分配至机台[{}]", machine.getMoldingMachineCode());
            if ("L102".equals(machine.getMoldingMachineCode())){
                int a = 1; //todo debug 快速定位
            }
            if (tryAllocateToMachine(task, machine)) {
                logInfo("任务[ID:{}] 成功分配至机台[{}]",
                        task.getLhScheduleResult().getMergeIds(), machine.getMoldingMachineCode());
                return true;
            }
        }
        log.warn("所有{}个候选机台均无法分配", candidates.size());
        return false;
    }

    /**
     * 任务尝试分配指定机台
     *
     * @param task    任务
     * @param machine 机台
     * @return 是否分配成功
     */
    public boolean tryAllocateToMachine(LhAlgorithmScheduleResultDto task,
                                         CxMachineInfoVo machine) {
        // 1. 计算任务能够开始生产的时间
        LocalDateTime availableTime = calculateAvailableTime(task, machine);
        logDebug("计算得到可用开始时间: {}", availableTime.format(formatter));

        // 2. 检查机台
        if (availableTime.isAfter(cxShiftConfig.parseToDayLastShiftEndTime())) {
            task.setStopScheduleReason( task.getStopScheduleReason() == null ? "": task.getStopScheduleReason() + String.format("机台[%s]机台已经安排满了,没有剩余产能！",
                    machine.getMoldingMachineCode()));
            return false;
        }

        // 2. 检查欠胎时间是否落点有效
        if (!isTimeWindowValid(availableTime, task.getPreviousTireTime())) {
            logWarn("欠胎时间是否落点无效 可用时间[{}] 欠胎时间[{}]",
                    availableTime, task.getPreviousTireTime());
            task.setStopScheduleReason( task.getStopScheduleReason() == null ? "": task.getStopScheduleReason() + String.format("机台[%s]可用时间无法满足欠胎！",
                    machine.getMoldingMachineCode()));
            return false;
        }

        // 3. 产能是否充足
        if (needSplitTask(task, machine)) {
            logInfo("需要拆单 任务量[{}] 机台剩余产能[{}]",
                    task.getTaskPlanQuantity(), machine.getRemainCapacity());
            handleTaskSplit(task, machine);
        }

        if (machine.getRemainCapacity() < task.getTaskPlanQuantity() && !task.getIsSatisfySpecification()) {
            task.setStopScheduleReason( task.getStopScheduleReason() == null ? "": task.getStopScheduleReason() + String.format("机台[%s]产能不足！",
                    machine.getMoldingMachineCode()));
            return false;
        }

        // 4. 执行班次排产
        logDebug("尝试执行班次排产...");
        boolean result = executeShiftScheduling(task, machine, availableTime, false);
        logInfo("班次排产结果: {}", result ? "成功" : "失败");
        return result;
    }


    /**
     * 任务定向分配指定机台
     *
     * @param task    任务
     * @param machine 机台
     * @return 是否分配成功
     */
    public boolean allocateToMachine(LhAlgorithmScheduleResultDto task,
                                        CxMachineInfoVo machine) {
        // 1. 计算任务能够开始生产的时间
        LocalDateTime availableTime = calculateAvailableTime(task, machine);
        logDebug("计算得到可用开始时间: {}", availableTime.format(formatter));

        // 2. 检查机台
        if (availableTime.isAfter(cxShiftConfig.parseToDayLastShiftEndTime())) {
            task.setStopScheduleReason( task.getStopScheduleReason() == null ? "": task.getStopScheduleReason() + String.format("机台[%s]机台已经安排满了,没有剩余产能！",
                    machine.getMoldingMachineCode()));
            return false;
        }

        // 4. 执行班次排产
        logDebug("尝试执行班次排产...");
        boolean result = executeShiftScheduling(task, machine, availableTime, false);
        logInfo("班次排产结果: {}", result ? "成功" : "失败");
        return result;
    }

    /**
     * 判断是否需要拆单
     *
     * @param task    任务
     * @param machine 机台
     * @return 是否需要拆单
     */
    public boolean needSplitTask(LhAlgorithmScheduleResultDto task, CxMachineInfoVo machine) {
        // todo 默认判断是否拆弹是已经剩余产能，继承类可以实现判断是否拆单逻辑
        if (task.getIsLargeTire()) {
            boolean needSplit = task.getTaskPlanQuantity() > machine.getRemainCapacity();
            logDebug("拆单检查 任务量：[{}]， 机台剩余产能：[{}]， ，是否需要拆单:{}",
                    task.getTaskPlanQuantity(), machine.getRemainCapacity(), needSplit);
            return needSplit;
        }else {
            return false;
        }
    }


    /**
     * 计算任务能够在机台开始生产的时间
     *
     * @param task    任务
     * @param machine 机台
     * @return 任务能够开始生产的时间
     */
    public LocalDateTime calculateAvailableTime(LhAlgorithmScheduleResultDto task,
                                                 CxMachineInfoVo machine) {

        double changeTime = changeSpecTime(
                task.getCxProductConstructionInfoDto(),
                machine.getCxProductConstructionInfo(),
                machine.getMouldMethod()
        );
        logDebug("换工装耗时计算: {}小时", changeTime);

        return machine.getAvailableBeginTime()
                .plusSeconds((long) (changeTime*3600));
    }


    /**
     * 换工装计算
     *
     * @param cxProductConstructionInfoDto 施工
     * @param cxProductConstructionInfo    施工
     * @return 换工装时间 （小时）
     */
    public double changeSpecTime(CxProductConstructionInfoDto cxProductConstructionInfoDto, CxProductConstructionInfoDto cxProductConstructionInfo, Integer m){
        //todo 继承类需要实现换工装计算方法
        return 0;
    }


    /**
     * 检查欠胎时间是否落点有效
     *
     * @param availableTime    机台开始生产任务的时间
     * @param previousTireTime 任务欠胎时间
     * @return 是否有效
     */
    public boolean isTimeWindowValid(LocalDateTime availableTime,
                                     LocalDateTime previousTireTime) {
        // todo 继承类需要实现检查欠胎时间是否落点有效
        return false;
    }


    /**
     * 执行拆单逻辑
     *
     * @param task    任务
     * @param machine 机台
     * @return 是否拆单成功
     */
    private boolean handleTaskSplit(LhAlgorithmScheduleResultDto task,
                                    CxMachineInfoVo machine) {

        if (task.getTireCount() > 1) {
            task.setIsStopSchedule(true);
            task.setStopScheduleReason(task.getStopScheduleReason() == null ? "": task.getStopScheduleReason() + "任务已经拆单一次，标记为终止排产");
            iCxScheduleStopInfoService.createCxScheduleStopInfo(task);
            return false;
        }
        splitTask(task, machine.getRemainCapacity());
        return true;
    }


    /**
     * 拆分任务
     *
     * @param task           任务
     * @param remainCapacity 拆分量
     */
    public void splitTask(LhAlgorithmScheduleResultDto task, Integer remainCapacity) {
        // todo 继承类需要实现拆分任务的方法
    }


    /**
     * ========================= 班次排产逻辑 =========================
     * 执行班次排产操作
     */
    public boolean executeShiftScheduling(LhAlgorithmScheduleResultDto task,
                                          CxMachineInfoVo machine,
                                          LocalDateTime startTime,
                                          boolean isReload) {
        return false;
    }


    /**
     * 处理无法排产的情况
     */
    private void handleUnScheduleAbleTask(LhAlgorithmScheduleResultDto task) {
        log.warn("处理无法排产任务[ID:{}]", task.getLhScheduleResult());
        task.setIsStopSchedule(Boolean.TRUE);
        iCxScheduleStopInfoService.createCxScheduleStopInfo(task);
        logInfo("已创建停排记录");
    }

    /**
     * 限制作业重新强制分配机台逻辑
     * 核心策略：
     * 1. 优先选择无交期机台
     * 2. 全部机台有交期时选择首台
     *
     * @param task     待分配任务
     * @param machines 候选机台列表（需非空）
     */
    public void reScheduleAbleTask(LhAlgorithmScheduleResultDto task, List<CxMachineInfoVo> machines) {
        logInfo("▶ 限制作业强占机台，任务胎胚：{},", task.getLhScheduleResult().getEmbryoCode());
        task.setIsStopSchedule(Boolean.FALSE);

        if (machines == null || machines.isEmpty()) {
            task.setStopScheduleReason(task.getStopScheduleReason() == null ? "": task.getStopScheduleReason() + "无可用机台，标记为终止排产");
            handleUnScheduleAbleTask(task);
            return;
        }

        // 第一轮筛选：寻找机台最后一个规格不是交期任务的进行占用
        CxMachineInfoVo targetMachine = null;
        for (CxMachineInfoVo machine : machines) {
            boolean hasDelivery = ApsConstant.APS_STRING_1.equals(machine.getIsDelivery());
            logDebug("机台[{}]当前生产的规格是否有交期: {}", machine.getMoldingMachineCode(), hasDelivery ? "有" : "无");

            if (!hasDelivery) {
                targetMachine = machine;
                logInfo("选中无交期机台：{}", machine.getMoldingMachineCode());
                break;
            }
        }

        // 兜底策略：选择首台机台
        if (targetMachine == null) {
            targetMachine = machines.get(0);
            log.warn("所有{}台机台均有交期，默认选择首台：{}",
                    machines.size(), targetMachine.getMoldingMachineCode());
        }

        logInfo("▷ 开始抢占机台[{}]给任务[{}]", targetMachine.getMoldingMachineCode(), task);
        performMachineOccupation(task, targetMachine);
        logInfo("◉ 机台强占完成，任务[{}] → 机台[{}]",
                task, targetMachine.getMoldingMachineCode());
    }

    /**
     * 执行机台抢占操作
     */
    public void performMachineOccupation(LhAlgorithmScheduleResultDto task, CxMachineInfoVo machine) {
        // TODO: 继承类实现实际抢占逻辑
    }


    //========================= 工具方法 =========================//

    /**
     * 创建大规格任务排序策略：首先按 [使用模数] 降序排序 /  后按 [欠胎时间] 降序排序
     */
    private Comparator<LhAlgorithmScheduleResultDto> createLargeTireComparator() {
        return Comparator
                .comparing((LhAlgorithmScheduleResultDto dto) ->
                                dto.getLhScheduleResult() != null ?
                                        dto.getLhScheduleResult().getMoldQty() : 0,
                        Comparator.nullsLast(Comparator.reverseOrder())
                )
                .thenComparing(LhAlgorithmScheduleResultDto::getPreviousTireTime);
    }

    /**
     * 创建小规格任务排序策略：按 [欠胎时间] 降序排序
     */
    private Comparator<LhAlgorithmScheduleResultDto> createSmallTireComparator() {
        return Comparator.comparing(LhAlgorithmScheduleResultDto::getPreviousTireTime);
    }

    /**
     * 创建大规格机台排序策略 ：首先按 [剩余产能] 降序排序 /  后按 [相似度] 降序排序 /  后按 [历史机台] 降序排序 /  后按 [机台编码] 升序排序
     */
    private Comparator<CxMachineInfoVo> createLargeMachineComparator() {
        return Comparator
                .comparing(CxMachineInfoVo::getIsHistoryMachine, Comparator.reverseOrder())
                .thenComparing((CxMachineInfoVo vo) ->
                                Optional.ofNullable(vo.getRemainCapacity()).orElse(0),
                        Comparator.reverseOrder())
                .thenComparing(CxMachineInfoVo::getSimilarity, Comparator.reverseOrder())
                .thenComparing(CxMachineInfoVo::getMoldingMachineCode);
    }

    /**
     * 创建小规格机台排序策略：按 [历史机台] 降序排序 / 后按 [相似度] 降序排序  / 后按 [剩余产能] 降序排序 /  后按 [机台编码] 升序排序
     */
    private Comparator<CxMachineInfoVo> createSmallMachineComparator() {
        return Comparator
                .comparing(CxMachineInfoVo::getIsHistoryMachine, Comparator.reverseOrder())
                .thenComparing(CxMachineInfoVo::getSimilarity, Comparator.reverseOrder())
                .thenComparing(vo -> Optional.ofNullable(vo.getRemainCapacity()).orElse(0),
                        Comparator.reverseOrder())
                .thenComparing(CxMachineInfoVo::getMoldingMachineCode);
    }


    /**
     * 计算实际可用库存（内联方法替代）
     */
    public int calculateRealStock(int baseStock, int modify, int bad, int overdue) {
        return baseStock + modify - bad - overdue;
    }

    //========================= 标记方法 =========================//

    /**
     * 3.3.1 标记判断规格是否续作规格
     *
     * @param lhAlgorithmScheduleResultDto 硫化任务对象
     * @param groupResultByMachineCode     前日成型排程按机台编号分组列表
     * @return 是否续作
     */
    public boolean isContinueTire(LhAlgorithmScheduleResultDto lhAlgorithmScheduleResultDto, Map<String, List<CxScheduleResult>> groupResultByMachineCode) {
        // 继承类实现实际判断逻辑
        return false;
    }


    /**
     * 3.3.2 标记判断规格是否限制规格
     *
     * @param lhAlgorithmScheduleResultDto 硫化任务对象
     * @param limitMachines                限制机台信息列表
     * @return 是否限制
     */
    public boolean isLimitTire(LhAlgorithmScheduleResultDto lhAlgorithmScheduleResultDto, Map<String, List<ProductMoldingLimit>> limitMachines) {
        if (limitMachines.containsKey(lhAlgorithmScheduleResultDto.getLhScheduleResult().getEmbryoCode())) {
            //获取任务胎胚对应的限制机台列表
            List<ProductMoldingLimit> productMoldingLimits = limitMachines.get(lhAlgorithmScheduleResultDto.getLhScheduleResult().getEmbryoCode());

            //获取胎胚对应的外胎
            LhScheduleResult lhScheduleResult = lhAlgorithmScheduleResultDto.getLhScheduleResult();
            String[] sapCodeArrays = lhScheduleResult.getSpecCode().split("/");
            Set<String> sapCodeSet = new HashSet<>(Arrays.asList(sapCodeArrays));

            //遍历限制机台列表
            for (ProductMoldingLimit productMoldingLimit : productMoldingLimits) {
                //将限制机台保存
                if (sapCodeSet.contains(productMoldingLimit.getSapCode()) && Objects.equals(productMoldingLimit.getJobType(), CxEngineConstants.SPECIFY_JOB_TYPE_YES)) {
                    lhAlgorithmScheduleResultDto.getLimitMachines().add(String.valueOf(productMoldingLimit.getMachineCode()));
                    //打上限制标记
                    logInfo("胎胚：[{}]，是限制作业机台：[{}], 对应外胎：[{}]，标记限制", lhScheduleResult.getEmbryoCode(), productMoldingLimit.getMachineCode(), lhScheduleResult.getSpecCode());
                    return Boolean.TRUE;
                }

                //将禁用机台保存
                if (sapCodeSet.contains(productMoldingLimit.getSapCode()) && Objects.equals(productMoldingLimit.getJobType(), CxEngineConstants.SPECIFY_JOB_TYPE_NO)) {
                    lhAlgorithmScheduleResultDto.getForbidMachines().add(String.valueOf(productMoldingLimit.getMachineCode()));
                    //打上限制标记
                    logInfo("胎胚：[{}]，是禁止作业机台：[{}],  对应外胎：[{}]，标记限制", lhScheduleResult.getEmbryoCode(), productMoldingLimit.getMachineCode(), lhScheduleResult.getSpecCode());
                    return Boolean.TRUE;
                }
            }
        }
        return Boolean.FALSE;
    }

    /**
     * 判断是否收尾
     *
     * @param lhAlgorithmScheduleResultDto 排查任务对象
     * @param cxEmbryoMonthPlanSurplusMap  月度剩余量分组集合
     * @return 是否收尾
     */
    public boolean isEndTire(LhAlgorithmScheduleResultDto lhAlgorithmScheduleResultDto, Map<String, CxEmbryoMonthPlanSurplus> cxEmbryoMonthPlanSurplusMap) {
        //TODO: 继承类实现实际判断逻辑
        return Boolean.FALSE;
    }


    /**
     * 判断是否新增规格
     *
     * @param lhAlgorithmScheduleResultDto 排查任务对象
     * @param yesterdayCxScheduleResults   前日成型排程计划
     * @return 是否新增规格
     */
    public boolean isNewTire(LhAlgorithmScheduleResultDto lhAlgorithmScheduleResultDto, Map<String, List<CxScheduleResult>> yesterdayCxScheduleResults) {
        LhScheduleResult lhScheduleResult = lhAlgorithmScheduleResultDto.getLhScheduleResult();
        lhAlgorithmScheduleResultDto.setIsNewTire(Boolean.TRUE);
        if (yesterdayCxScheduleResults.containsKey(lhScheduleResult.getEmbryoCode() + lhScheduleResult.getBomVersion())) {
            List<CxScheduleResult> cxScheduleResults = yesterdayCxScheduleResults.get(lhScheduleResult.getEmbryoCode() + lhScheduleResult.getBomVersion());
            // 所有相关胎胚的计划量累计
            int totalPlanQty = 0;
            for (CxScheduleResult cxScheduleResult : cxScheduleResults) {
                // 使用Optional更安全的处理方式
                totalPlanQty = Optional.ofNullable(cxScheduleResult.getClass1PlanQty()).orElse(0)
                        + Optional.ofNullable(cxScheduleResult.getClass2PlanQty()).orElse(0)
                        + Optional.ofNullable(cxScheduleResult.getClass3PlanQty()).orElse(0);
                CX_MACHINE_QUOTA_CONTEXT_MAP.put(cxScheduleResult.getCxMachineCode(), BigDecimal.valueOf(cxScheduleResult.getSpecDimension()));
            }
            //昨天没有安排量就是新增规格
            if (totalPlanQty > 0) {
                lhAlgorithmScheduleResultDto.setIsNewTire(Boolean.FALSE);
                logInfo("胎胚：[{}]，前日成型1/2/3班中存在计划量[{}]，标记不是新增", lhScheduleResult.getEmbryoCode(), totalPlanQty);
            }
        }
        return lhAlgorithmScheduleResultDto.getIsNewTire();
    }


    /**
     * 判断是否普通规格
     *
     * @param lhAlgorithmScheduleResultDto 排查任务对象
     * @param yesterdayCxScheduleResults   前日成型排程计划
     * @return 是否普通规格
     */
    public boolean isNormalTire(LhAlgorithmScheduleResultDto lhAlgorithmScheduleResultDto, Map<String, List<CxScheduleResult>> yesterdayCxScheduleResults) {
        LhScheduleResult lhScheduleResult = lhAlgorithmScheduleResultDto.getLhScheduleResult();
        if (yesterdayCxScheduleResults.containsKey(lhScheduleResult.getEmbryoCode()+lhScheduleResult.getBomVersion())) {
            List<CxScheduleResult> cxScheduleResults = yesterdayCxScheduleResults.get(lhScheduleResult.getEmbryoCode()+lhScheduleResult.getBomVersion());
            for (CxScheduleResult cxScheduleResult : cxScheduleResults) {
                // 保存上次占用机台
                lhAlgorithmScheduleResultDto.getLastOccupiedMachines().add(String.valueOf(cxScheduleResult.getCxMachineCode()));
                logInfo("胎胚：[{}]，昨天在机台生产过[{}]", lhScheduleResult.getEmbryoCode(),cxScheduleResult.getCxMachineCode());
            }
            logInfo("胎胚：[{}]，默认也是普通规格", lhScheduleResult.getEmbryoCode());
        }
        return Boolean.TRUE;
    }


    /**
     * 判断是否大规格
     *
     * @param lhAlgorithmScheduleResultDto 排程任务
     * @return 是否大规格
     */
    public boolean isLargeSpecification(LhAlgorithmScheduleResultDto lhAlgorithmScheduleResultDto) {
        //TODO: 继承类实现实际判断逻辑
        return Boolean.FALSE;
    }


    /**
     * 安全的拼接字符串方法
     * @param str1 字符串1
     * @param str2 字符串2
     * @return 返回拼接后的字符串
     */
    public String joinSafe(String str1, String str2) {
        return Stream.of(str1, str2)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining("/"));
    }

    /**
     * 安全的加法方法
     * @param num1 数字1
     * @param num2 数字2
     * @return 结果
     */
    public int addSafe(Integer num1, Integer num2) {
        return (num1 == null ? 0 : num1) + (num2 == null ? 0 : num2);
    }

}

