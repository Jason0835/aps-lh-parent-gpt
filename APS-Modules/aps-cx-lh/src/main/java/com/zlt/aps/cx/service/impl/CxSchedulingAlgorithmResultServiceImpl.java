package com.zlt.aps.cx.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.*;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.config.CxShiftConfig;
import com.zlt.aps.constants.CxEngineConstants;
import com.zlt.aps.constants.CxPrefixConstants;
import com.zlt.aps.cx.mapper.entity.CxPersionTrainSettingEntityMapper;
import com.zlt.aps.cx.mapper.entity.CxScheduleResultEntityMapper;
import com.zlt.aps.cx.mapper.entity.CxScheduleStopInfoEntityMapper;
import com.zlt.aps.cx.service.CxMatchingSpecifyMachineService;
import com.zlt.aps.cx.service.CxScheduleResultService;
import com.zlt.aps.cx.service.CxSchedulingAlgorithmResultService;
import com.zlt.aps.cx.service.ICxStockService;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxProductConstructionInfo;
import com.zlt.aps.cxlh.cx.api.domain.entity.*;
import com.zlt.aps.cxlh.cx.api.domain.vo.CxMachineInfoVo;
import com.zlt.aps.cxlh.cx.api.domain.vo.CxProductConstructionInfoDto;
import com.zlt.aps.cxlh.cx.api.domain.vo.LhAlgorithmScheduleResultDto;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.service.LhScheduleResultService;
import com.zlt.aps.maindata.mapper.ProductMoldingLimitMapper;
import com.zlt.aps.maindata.utils.CxLhEngineUtils;
import com.zlt.aps.mp.api.domain.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.zlt.aps.cx.service.impl.CxPersionTrainSettingServiceImpl.getSpecifiedMachineQuotas;
import static com.zlt.aps.cx.service.impl.CxPersionTrainSettingServiceImpl.getUnspecifiedMachineQuotas;

/**
 * Description: 成型排程算法引擎实现类
 *
 * @author Nick
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class CxSchedulingAlgorithmResultServiceImpl extends CommonEngineService implements CxSchedulingAlgorithmResultService {


    @Autowired
    private CxPersionTrainSettingEntityMapper cxPersionTrainSettingEntityMapper;
    @Autowired
    private CxScheduleResultService cxScheduleResultService;
    @Autowired
    private CxScheduleResultEntityMapper cxScheduleResultEntityMapper;
    @Autowired
    private LhScheduleResultService lhScheduleResultService;
    @Resource
    private ICxStockService cxStockService;
    @Autowired
    private ProductMoldingLimitMapper productMoldingLimitMapper;
    @Autowired
    private CxScheduleStopInfoEntityMapper cxScheduleStopInfoEntityMapper;
    @Autowired
    private CxMatchingSpecifyMachineService iCxMatchingSpecifyMachineService;

    /**
     * 算法辅助类
     */
    @Resource
    private CommonCacheService commonCacheService;
    @Resource
    private CommonRedisService commonRedisService;
    @Resource
    private CommonQueryCacheService commonQueryCacheService;

    /**
     * <成型机编号, 成型机今日排程记录上下文>
     **/
    private static final Map<String, CxScheduleResultOld> CX_SCHEDULE_RESULT_CONTEXT_MAP = new ConcurrentHashMap<>();


    /**
     * <生胎代码, 生胎代码实时库存以上下文>
     **/
    private static final Map<String, CxStock> CX_STOCK_NUM_CONTEXT_MAP = new ConcurrentHashMap<>();

    /**
     * <生胎代码, 生胎代码实时胎胚月度剩余量上下文计算>
     */
    private static final Map<String, CxEmbryoMonthPlanSurplus> CX_STOCK_MONTH_REMAIN_CONTEXT_MAP = new ConcurrentHashMap<>();

    /**
     * <待生产的胎胚任务上下文>
     **/
    private static final List<LhAlgorithmScheduleResultDto> LH_MISS_TIRE_TIME_CONTEXT_MAP = new LinkedList<>();


    /**
     * 施工相似度字段
     */
    public static String[] PCRSimilarityFields = new String[]{"tireFabricCode1", "tireFabricCode2", "tireFabricCode3", "originalLineCode",
            "insideCode", "sidewallCode", "supportCode", "beadCode", "tireRingCode", "apexCode", "beltCode1", "beltCode2", "treadCode"};



    /**
     * title: 成型自动排程入口
     *
     * @param scheduleDate 排程日期
     * @param durationDays 连续排程天数
     * @param factoryCode 工厂
     */
    @Override
    public void calculateMoldingPlan(Date scheduleDate, int durationDays, String factoryCode) {
        //==================== 清空上下文内容 ====================//
        String cxBatchNo = initializeScheduleEnvironment(scheduleDate);

        // ==================== 多日排程循环 =============//
        for (int dayOffset = 0; dayOffset <= durationDays; dayOffset++) {
            LH_MISS_TIRE_TIME_CONTEXT_MAP.clear();
            // 1. 计算当前排程日（基准日）
            Date currentDay = CxLhEngineUtils.calculateDate(scheduleDate, dayOffset);
            logInfo("开始处理第{}天排程，基准日期：[{}]", dayOffset + 1, formatDate(currentDay));

            // 2. 获取当日（基准日）硫化任务
            List<LhScheduleResult> dailyTasks = lhScheduleResultService.getScheduleLhScheduleResults(currentDay, scheduleLog);

            // 3. 执行核心排程逻辑
            calculateSingleDayMoldingPlan(dailyTasks, currentDay,dayOffset + 1 > 1, cxBatchNo);

            // 4. 组装排程结果逻辑
            generateFinalSchedule(scheduleDate, dayOffset + 1 > 1, cxBatchNo, scheduleDate, factoryCode);
        }

        // 5. 持久化逻辑
        persistScheduleResults(cxBatchNo);
    }

    /**
     * 初始化排程环境
     */
    private String initializeScheduleEnvironment(Date scheduleDate) {
        // 1. 清理上下文数据
        clearAllContexts();

        // 2. 重置日志记录器
        resetScheduleLogger();

        // 3. 初始化班制配置
        initializeShiftConfig();

        // 4. 生成批次号
        return generateBatchNumber(scheduleDate);
    }


    /**
     * 清理所有上下文数据
     */
    private void clearAllContexts() {
        CX_SCHEDULE_RESULT_CONTEXT_MAP.clear();
        CX_MACHINE_ACCTIVE_TIME_CONTEXT_MAP.clear();
        LH_MISS_TIRE_TIME_CONTEXT_MAP.clear();
        CX_STOCK_NUM_CONTEXT_MAP.clear();
        CX_PARAMS_CONTEXT_MAP.clear();
        CX_STOCK_MONTH_REMAIN_CONTEXT_MAP.clear();
        logDebug("上下文数据已清空");
    }


    /**
     * 初始化班制配置
     */
    private void initializeShiftConfig() {
        cxShiftConfig = null;
        logInfo("班制配置已重置");
    }


    /**
     * 生成批次号
     */
    public String generateBatchNumber(Date scheduleDate) {
        //更新半部件删除字段
        LambdaUpdateWrapper<CxScheduleResultOld> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CxScheduleResultOld::getScheduleDate, scheduleDate)
                .set(CxScheduleResultOld::getDelFlag, 1)
                .set(CxScheduleResultOld::getIsDelete,1);
        cxScheduleResultEntityMapper.update(null, updateWrapper);

        //删除未排产的排程记录
        LambdaUpdateWrapper<CxScheduleStopInfo> deleteWrapper = new LambdaUpdateWrapper<>();
        deleteWrapper.eq(CxScheduleStopInfo::getScheduleDate, scheduleDate)
                .set(CxScheduleStopInfo::getIsDelete,1);
        cxScheduleStopInfoEntityMapper.update(null, deleteWrapper);

        String dateStr = DateUtils.parseDateToStr("yyyyMMdd", scheduleDate);
        String batchNo = commonRedisService.getSequence(
                CxPrefixConstants.SCHEDULE_BATCH_NO_PREFIX + dateStr,
                CxPrefixConstants.CX_BATCH_NO_PREFIX + dateStr
        );
        logDebug("批次号生成成功：{}", batchNo);
        return batchNo;
    }


    /**
     * 持久化排程结果
     */
    private void persistScheduleResults(String batchNo) {
        logInfo("===== 开始持久化排程结果 =====");
        cxScheduleResultService.generateFinalSchedule(CX_SCHEDULE_RESULT_CONTEXT_MAP);
        cxScheduleResultService.genScheduleLog(scheduleLog.toString(), batchNo);
        logInfo("持久化完成，批次号：{}", batchNo);
    }


    /**
     * 成型自动排程【全机台排程】核心流程
     * 1. 初始化排程上下文环境
     * 2. 处理任务合并与库存计算
     * 3. 多优先级任务分批次排产
     *
     * @param lhScheduleResults 硫化工序计划列表（需处理生胎数据）
     * @param scheduleDate      目标排程日期（决定计算基准时间）
     * @param isTomorrow        是否为次日排程
     * @param cxBatchNo         批次号
     */
    @Override
    public void calculateSingleDayMoldingPlan(List<LhScheduleResult> lhScheduleResults, Date scheduleDate, boolean isTomorrow, String cxBatchNo) {
        logDebug("// ==================== 环境准备阶段 ====================//");
        final Date previousDay = CxLhEngineUtils.calculateDate(scheduleDate, -1);
        logDebug("确认排程前日：[{}]",  DateUtils.parseDateToStr(FORMATTER_STR, previousDay));
        // 步骤0: 动态加载班次
        initializeCxParamsContext(previousDay);

        logDebug("//==================== 数据处理阶段 ====================//");
        // 步骤1: 合并相同生胎代码的硫化任务
        logDebug("开始合并→：相同生胎代码的硫化任务");
        mergeTasks(lhScheduleResults, cxBatchNo);
        logDebug("结束合并→：相同生胎代码的硫化任务");

        // 步骤2:获取前日调整后的成型排程
        List<CxScheduleResultOld> lastDayCxResults = cxScheduleResultService.getScheduleCxScheduleResults(previousDay, scheduleLog, CX_SCHEDULE_RESULT_CONTEXT_MAP.values());
        // 步骤2:获取前日调整后的硫化排程
        List<LhScheduleResult> lastDayLhResults = lhScheduleResultService.getScheduleLhScheduleResults(previousDay, scheduleLog);

        // 步骤2: 库存预测与欠胎时间计算
        analyzeYesterdayPlanAndCalculateStock(lastDayLhResults, lastDayCxResults, previousDay,isTomorrow);
        logDebug("开始计算→欠胎时间, 总成型任务数：{}条！", LH_MISS_TIRE_TIME_CONTEXT_MAP.size());
        calculateMissTireTimeForSpecification();
        logDebug("完成计算→欠胎时间, 总成型任务数：{}条！", LH_MISS_TIRE_TIME_CONTEXT_MAP.size());


        logDebug("//==================== 任务分类阶段====================//");
        // 步骤4: 任务类型标记（限制/续作/收尾等）
        markTasks(lastDayCxResults, lastDayLhResults, scheduleDate);
        logDebug("完成任务标记，上下文记录数[{}]", LH_MISS_TIRE_TIME_CONTEXT_MAP.size());


        logDebug("//==================== 排产准备阶段====================//");
        // 步骤5: 初始化机台可用时间上下文
        initializeMachineActiveTimeContext(scheduleDate);


        logDebug("//==================== 多级排产阶段====================//");
        // 步骤6: 优先处理续作任务（保证生产连续性）
        processPriorityTasks("续作任务",
                item -> item.getIsContinueTire() && !item.getIsScheduleEnd() && !item.getIsStopSchedule());

        // 步骤7: 处理限制任务（满足特殊约束条件）
        processPriorityTasks("限制任务",
                item -> item.getIsLimitTire() && !item.getIsScheduleEnd() && !item.getIsStopSchedule());

        // 步骤8: 处理普通任务（常规生产任务）
        processPriorityTasks("普通任务",
                item -> item.getIsNormalTire() && !item.getIsScheduleEnd() && !item.getIsStopSchedule());


        logDebug("//==================== 收尾阶段====================//");


        if (commonCacheService.getCxPlanBalanceSwitch(CX_PARAMS_CONTEXT_MAP)) {
            // 步骤9: 计算最大备库（平衡产能）
            calculateMaxStockForSpecification();
            // 步骤9: 任务补偿处理（平衡产能）
            compensateProductionVolume();
        }

        if (commonCacheService.getAllocateRemainingCapacitySwitch(CX_PARAMS_CONTEXT_MAP)) {
            // 步骤10: 将每个机台的剩余产能分配到各个规格直到没有产能
            allocateRemainingCapacity(lastDayCxResults);
        }
    }




    /**
     * 处理优先级任务排产（内部流程复用）
     *
     * @param taskTypeDesc    任务类型描述（用于日志）
     * @param filterPredicate 任务过滤条件
     */
    private void processPriorityTasks(String taskTypeDesc, Predicate<LhAlgorithmScheduleResultDto> filterPredicate) {
        logInfo("=== 开始处理{}排产 ===", taskTypeDesc);

        List<LhAlgorithmScheduleResultDto> targetTasks = LH_MISS_TIRE_TIME_CONTEXT_MAP.stream()
                .filter(filterPredicate)
                .collect(Collectors.toList());

        logDebug("待处理{}数量: {}", taskTypeDesc, targetTasks.size());
        if (targetTasks.isEmpty()) {
            logWarn("没有需要处理的{}", taskTypeDesc);
            return;
        }

        logInfo("执行{}机台分配...", taskTypeDesc);
        allocateMachinesForProduction(targetTasks);

        long successCount = targetTasks.stream().filter(LhAlgorithmScheduleResultDto::getIsScheduleEnd).count();
        logInfo("{}排产完成，成功分配数: {}/{}", taskTypeDesc, successCount, targetTasks.size());
    }

    /**
     * ------------------------------- Step 0: 初始化成型参数-----------------------------
     *
     * @param scheduleDate 排程日期
     */
    private void initializeCxParamsContext(Date scheduleDate) {
        // 1.获取成型参数配置, 初始化参数上下文
        commonQueryCacheService.queryCxParams().forEach(item -> {
            CX_PARAMS_CONTEXT_MAP.put(item.getParamCode(), item);
        });

        // 2.从参数上下文获取[班制], 动态加载各班次开始时间
        cxShiftConfig = CxShiftConfig.of(commonCacheService.getCxShiftSystem(CX_PARAMS_CONTEXT_MAP));
        cxShiftConfig.setStartTime(scheduleDate, commonCacheService.getCxShiftSystemStartHour(CX_PARAMS_CONTEXT_MAP));
    }


    /**
     * -----------------------------
     * Step1: 依据硫化生胎代码合并初始化排程任务
     * -----------------------------
     * 优化要点：
     * 1. 使用结构化代码块增强可读性
     * 2. 增加详细日志跟踪
     * 3. 使用辅助变量提升可维护性
     * 4. 添加空集合安全保护
     * 5. 统一条件判断逻辑
     * -----------------------------
     *
     * @param lhScheduleResultList 硫化计划列表
     * @param cxBatchNo   批次号
     */
    private void mergeTasks(List<LhScheduleResult> lhScheduleResultList, String cxBatchNo) {
        // 日志记录初始状态
        logInfo("原始硫化任务数：{}", lhScheduleResultList.size());


        //==================== 阶段1：数据准备 ====================//
        // 1. 按胎胚代码分组
        Map<String, List<LhScheduleResult>> embryoGroupMap = lhScheduleResultList.stream()
                .collect(Collectors.groupingBy(LhScheduleResult::getEmbryoCode));
        logDebug("胎胚代码分组完成，分组数：{}", embryoGroupMap.size());


        // 2. 获取物料信息并分组，用于合并后填充物料信息
        List<MdmMaterialInfo> specInfos = commonQueryCacheService.querySulfurSpecInfo(lhScheduleResultList);
        Map<String, List<MdmMaterialInfo>> specInfoMap = specInfos.stream()
                .collect(Collectors.groupingBy(item -> item.getFactoryCode() + item.getMaterialCode()));

        //==================== 阶段2：分组处理 ====================//
        embryoGroupMap.forEach((embryoCode, taskGroup) -> {
            logInfo("⌂ 胎胚代码：[{}]，开始合并{}个原始硫化任务", embryoCode, taskGroup.size());

            // 初始化任务分类容器
            List<LhScheduleResult> todayTasks = new ArrayList<>();
            List<LhScheduleResult> tomorrowTasks = new ArrayList<>();

            // 遍历任务进行分类
            for (LhScheduleResult task : taskGroup) {
                // 计算今日计划总量
                int todayPlanSum = 0;
                List<String> todayShiftCodes = cxShiftConfig.getTodayClasses("class", "PlanQty");
                for (String shiftCode : todayShiftCodes) {
                    todayPlanSum += (int) task.getFieldValueByFieldName(shiftCode);
                }

                // 分类今日任务
                if (todayPlanSum > 0) {
                    todayTasks.add(task);
                    logDebug("硫化任务[{}]分类为生产的任务，[1、2]班总硫化计划量：{}", task.getId(), todayPlanSum);
                    continue;
                }

                // 计算明日计划总量
                int tomorrowPlanSum = 0;
                List<String> tomorrowShiftCodes = cxShiftConfig.getTomorrowClasses("class", "PlanQty");
                for (String shiftCode : tomorrowShiftCodes) {
                    tomorrowPlanSum += (int) task.getFieldValueByFieldName(shiftCode);
                }

                // 分类明日任务
                if (tomorrowPlanSum > 0) {
                    tomorrowTasks.add(task);
                    logDebug("硫化任务[{}]分类为预排生产的任务，[4、5]班总硫化计划量：{}", task.getId(), tomorrowPlanSum);
                }
            }

            // 记录分类结果
            logInfo("胎胚[{}]对应的原始硫化任务分类完成其中：生产任务[{}]，预排任务[{}]",
                    embryoCode, todayTasks.size(), tomorrowTasks.size());

            // 执行任务合并
            if (!todayTasks.isEmpty()) {
                logInfo("开始合并生产任务，数量：{}", todayTasks.size());
                mergeBatchLhResults(todayTasks, specInfoMap, false, cxBatchNo);
            }

            if (!tomorrowTasks.isEmpty() && commonCacheService.getCxPlanTomorrowSwitch(CX_PARAMS_CONTEXT_MAP)) {
                logInfo("开始合并预排任务，数量：{}", tomorrowTasks.size());
                mergeBatchLhResults(tomorrowTasks, specInfoMap, true, cxBatchNo);
             }

            logInfo("⌂ 胎胚代码：[{}]，结束合并{}个原始硫化任务", embryoCode, taskGroup.size());
        });
    }


    /**
     * -----------------------------Step：1-合并硫化任务-----------------------------
     *
     * @param batch                    硫化计划
     * @param mdmMaterialInfoInfoDtoMap 规格物料施工分组
     * @param isTomorrowNewSpec        是否是明日新增
     * @param cxBatchNo 批次号
     */
    private void mergeBatchLhResults(List<LhScheduleResult> batch, Map<String, List<MdmMaterialInfo>> mdmMaterialInfoInfoDtoMap, boolean isTomorrowNewSpec, String cxBatchNo) {
        // 0.列表进行排序后合并，按照使用模数降序
        batch.sort((o1, o2) -> o2.getMoldQty().compareTo(o1.getMoldQty()));

        // 1. 获取列表第一个记录
        LhScheduleResult target = batch.get(0);
        // 2. 保存硫化任务ID
        target.setMergeIds(String.valueOf(target.getId()));
        // 3. 处理异常字段
        target.setLeftRightMold(StringUtils.isEmpty(target.getLeftRightMold()) ? "LR" : target.getLeftRightMold());
        target.setEmbryoStock(target.getEmbryoStock() ==  null ? 0 : target.getEmbryoStock());

        // 4.合并
        List<MdmMaterialInfo> lhScheduleResultMdmMaterialInfo = new ArrayList<>();
        if (mdmMaterialInfoInfoDtoMap.get(target.getSpecCode()) != null) {
            // 保存外胎物料信息
            lhScheduleResultMdmMaterialInfo.addAll(mdmMaterialInfoInfoDtoMap.get(target.getSpecCode()));
        }

        for (int i = 1; i < batch.size(); i++) {
            //获取硫化对象
            LhScheduleResult next = batch.get(i);
            //合并单双模
            next.setLeftRightMold(StringUtils.isEmpty(target.getLeftRightMold()) ? "LR" : target.getLeftRightMold());
            target.setLeftRightMold(joinSafe(target.getLeftRightMold(), next.getLeftRightMold()));
            //合并工单号
            target.setOrderNo(joinSafe(target.getOrderNo(), next.getOrderNo()));
            //合并硫化机台编号
            target.setLhMachineCode(joinSafe(target.getLhMachineCode(), next.getLhMachineCode()));
            //合并硫化机
            target.setLhMachineName(joinSafe(target.getLhMachineName(), next.getLhMachineName()));
            if (mdmMaterialInfoInfoDtoMap.containsKey(next.getSpecCode())) {
                // 保存外胎物料信息
                lhScheduleResultMdmMaterialInfo.addAll(mdmMaterialInfoInfoDtoMap.get(next.getSpecCode()));
            }

            //合并物料编码
            target.setProductCode(joinSafe(target.getProductCode(), next.getProductCode()));
            //合并外胎编号
            target.setSpecCode(joinSafe(target.getSpecCode(), next.getSpecCode()));
            //合并规格描述信息
            target.setSpecDesc(joinSafe(target.getSpecDesc(), next.getSpecDesc()));
            // 合并日计划数
            target.setDailyPlanQty(addSafe(target.getDailyPlanQty(), next.getDailyPlanQty()));
            //合并月度计划模数
            target.setMoldQty(addSafe(target.getMoldQty(), next.getMoldQty()));
            //合并使用模数
            target.setMpMoldQty(addSafe(target.getMpMoldQty(), next.getMpMoldQty()));
            //合并ID
            target.setMergeIds(target.getMergeIds() + "/" + next.getId());
            //合并一班
            target.setClass1PlanQty(addSafe(target.getClass1PlanQty(), next.getClass1PlanQty()));
            //合并二班
            target.setClass2PlanQty(addSafe(target.getClass2PlanQty(), next.getClass2PlanQty()));
            //合并三班
            target.setClass3PlanQty(addSafe(target.getClass3PlanQty(), next.getClass3PlanQty()));
            //合并四班
            target.setClass4PlanQty(addSafe(target.getClass4PlanQty(), next.getClass4PlanQty()));
            //合并五班
            target.setClass5PlanQty(addSafe(target.getClass5PlanQty(), next.getClass5PlanQty()));
            //合并六班
            target.setClass6PlanQty(addSafe(target.getClass6PlanQty(), next.getClass6PlanQty()));
            //处理是否交期标识
            if (next.getIsDelivery() != null && ApsConstant.TRUE.equals(next.getIsDelivery())) {
                target.setIsDelivery(ApsConstant.TRUE);
                //处理交期数量
                target.setDeliveryNum(target.getClass1PlanQty() + target.getClass2PlanQty() + target.getClass3PlanQty());
            }
            //合并模具信息
            target.setMoldInfo(joinSafe(target.getMoldInfo(), next.getMoldInfo()));
            //合并胎胚库存
            target.setEmbryoStock(addSafe(target.getEmbryoStock(), next.getEmbryoStock()));
            //校验target施工是否存在
            if(StringUtils.isEmpty(target.getBomVersion())){
                throw new RuntimeException(String.format("硫化排程发现生胎代码：[%s]，规格代码：[%s]，Bom信息：[%s}版本，数据错误，请检查硫化排程计划！",  target.getEmbryoCode(), target.getSpecCode(), target.getBomVersion()));
            }
            //校验next施工是否存在
            if(StringUtils.isEmpty(next.getBomVersion())){
                throw new RuntimeException(String.format("硫化排程发现生胎代码：[%s]，规格代码：[%s]，Bom信息：[%s]版本，数据错误，请检查硫化排程计划！",  next.getEmbryoCode(), next.getSpecCode(), next.getBomVersion()));
            }
            //校验target施工与next施工,Bom版本是否一致,不一致后续排程会有匹配问题
            if(!target.getBomVersion().equals(next.getBomVersion())){
               throw new RuntimeException(String.format("硫化排程发现同一生胎代码：[%s]，使用不同Bom：[%s/%s]版本，导致无法合并，请检查硫化排程计划！",  target.getEmbryoCode(), target.getBomVersion(), next.getBomVersion()));
            }
        }

        // 5. 构建任务,保存上下文
        LhAlgorithmScheduleResultDto lhAlgorithmScheduleResultDto = new LhAlgorithmScheduleResultDto();
        lhAlgorithmScheduleResultDto.setLhScheduleResult(target);
        lhAlgorithmScheduleResultDto.setBatchNo(cxBatchNo);
        lhAlgorithmScheduleResultDto.setMdmMaterialInfoList(lhScheduleResultMdmMaterialInfo);
        if (isTomorrowNewSpec) {
            lhAlgorithmScheduleResultDto.setIsNewTire(Boolean.TRUE);
        }
        LH_MISS_TIRE_TIME_CONTEXT_MAP.add(lhAlgorithmScheduleResultDto);
        // 6. 保存日志
        logInfo(buildStringFromEntity(target));
    }


    /**
     * ----------------------------- Step 2: 取今日7点库存初始化库存上下文-----------------------------
     *
     * @param scheduleDate 排程日期
     */
    public void queryToday7amStockInitializeStockContext(Date scheduleDate) {
        if (!CX_STOCK_NUM_CONTEXT_MAP.isEmpty()) {
            logDebug("▶ 已经加载：[{}]真实MES库存数据[{}]条，跳过重复加载", formatDate(scheduleDate), CX_STOCK_NUM_CONTEXT_MAP.size());
            return;
        }

        // 2.0.3: 执行查询
        List<CxStock> stockRecords = cxStockService.queryStockByDate(scheduleDate);

        // 2.0.4: 处理查询结果并保存到上下文
        if (stockRecords == null || stockRecords.isEmpty()) {
            String errorMsg = String.format("库存数据缺失：未找到%s真实MES库存数据",
                    DateUtils.parseDateToStr(FORMATTER_STR, scheduleDate));
            logError(errorMsg);
        } else {
            stockRecords.forEach(record -> {
                // 判空检查
                record.setBadNum(record.getBadNum() == null ? 0 : record.getBadNum());
                record.setOverTimeStock(record.getOverTimeStock() == null ? 0 : record.getOverTimeStock());
                record.setModifyNum(record.getModifyNum() == null ? 0 : record.getModifyNum());
                record.setStockNum(record.getStockNum() == null ? 0 : record.getStockNum());

                // 计算实际可用库存公式：库存 + 修正 - 不良 - 超期
                int realStock = calculateRealStock(
                        record.getStockNum(),
                        record.getModifyNum(),
                        record.getBadNum(),
                        record.getOverTimeStock()
                );

                // 1.3.2：库存最低是0不能是负数
                if (realStock < 0 && !commonCacheService.getIsAllowNegativeStock(CX_PARAMS_CONTEXT_MAP)) {
                    realStock = 0;
                }

                // 调试日志
                logDebug("◉ 胎胚[{}]库存计算：{}(原始) + {}(修正) - {}(不良) - {}(超期) = {}",
                        record.getEmbryoCode(),
                        record.getStockNum(),
                        record.getModifyNum(),
                        record.getBadNum(),
                        record.getOverTimeStock(),
                        realStock);
                record.setStockNum(realStock);

                // 保存上下文
                CX_STOCK_NUM_CONTEXT_MAP.put(record.getEmbryoCode(), record);
            });
        }
    }


    /**
     * ----------------------------- Step 2: 分析前日计划预计库存-----------------------------
     *
     * @param correctedLhScheduleResults 前日硫化排程计划
     * @param correctedCxScheduleResults 前日成型排程计划
     * @param scheduleDate               排程日期-1天
     * @param isTomorrow  是否预排
     */
    private void analyzeYesterdayPlanAndCalculateStock(List<LhScheduleResult> correctedLhScheduleResults, List<CxScheduleResultOld> correctedCxScheduleResults, Date scheduleDate, boolean isTomorrow) {
        // 1.1: 取排程开始班次库存：即库存初始化库存上下文
        logDebug("//==================== 预测胎胚库存 ====================//");
        logInfo("▶ 开始加载：[{}]真实MES库存数据", formatDate(scheduleDate));
        queryToday7amStockInitializeStockContext(scheduleDate);
        logInfo("▶ 结束加载：[{}]真实MES库存数据, 库存数据：{}条", formatDate(scheduleDate), CX_STOCK_NUM_CONTEXT_MAP.size());


        // 1.2: 前日成型,更新库存上下文
        correctedCxScheduleResults.forEach(cxScheduleResult -> {
            CxStock cxStock = CX_STOCK_NUM_CONTEXT_MAP.get(cxScheduleResult.getEmbryoCode());
            if (cxStock == null) {
                // 如果库存对象不存在，创建一个新的并初始化
                cxStock = new CxStock();
                cxStock.setEmbryoCode(cxScheduleResult.getEmbryoCode());
                // 初始库存量为 0
                cxStock.setStockNum(0);
            }


            // 1.2.1: 预测库存 = 实际库存量 + 【7点到19点】成型
            int currentStock = getCxCurrentStock(isTomorrow, cxScheduleResult, cxStock);

            // 1.2.2: 更新库存上下文
            cxStock.setStockNum(currentStock);
            CX_STOCK_NUM_CONTEXT_MAP.put(cxScheduleResult.getEmbryoCode(), cxStock);
        });


        // 1.3: 前日硫化,更新库存上下文
        correctedLhScheduleResults.forEach(lhScheduleResult -> {
            CxStock cxStock = CX_STOCK_NUM_CONTEXT_MAP.get(lhScheduleResult.getEmbryoCode());
            if (cxStock == null) {
                // 如果库存对象不存在，创建一个新的并初始化
                cxStock = new CxStock();
                cxStock.setEmbryoCode(lhScheduleResult.getEmbryoCode());
                // 初始库存量为 0
                cxStock.setStockNum(0);
            }

            // 1.3.1: 预测库存 = 7点实际库存量 - 【7点到19点】硫化
            int currentStock = getLhCurrentStock(isTomorrow, lhScheduleResult, cxStock);

            // 1.3.2: 更新库存上下文
            cxStock.setStockNum(currentStock);
            logDebug("◉ 胎胚[{}]库存依据前日硫化消耗：{}条，预测量更新为：{}", cxStock.getEmbryoCode(), lhScheduleResult.getClass2PlanQty(), currentStock);
            CX_STOCK_NUM_CONTEXT_MAP.put(lhScheduleResult.getEmbryoCode(), cxStock);
        });
    }

    private static int getLhCurrentStock(boolean isTomorrow, LhScheduleResult lhScheduleResult, CxStock cxStock) {
        int currentStock = cxStock.getStockNum();

        if (isTomorrow) {
            currentStock = currentStock - (lhScheduleResult.getClass1PlanQty() == null ? 0 : lhScheduleResult.getClass1PlanQty());
            currentStock = currentStock - (lhScheduleResult.getClass2PlanQty() == null ? 0 : lhScheduleResult.getClass2PlanQty());
            currentStock = currentStock - (lhScheduleResult.getClass3PlanQty() == null ? 0 : lhScheduleResult.getClass3PlanQty());
        } else {
            currentStock = currentStock - (lhScheduleResult.getClass1PlanQty() == null ? 0 : lhScheduleResult.getClass1PlanQty());
            currentStock = currentStock - (lhScheduleResult.getClass2PlanQty() == null ? 0 : lhScheduleResult.getClass2PlanQty());
        }

        // 1.3.2：库存最低是0不能是负数
        if (currentStock < 0) {
            currentStock = 0;
        }
        return currentStock;
    }


    private  int getCxCurrentStock(boolean isTomorrow, CxScheduleResultOld cxScheduleResult, CxStock cxStock) {
        int currentStock = cxStock.getStockNum();
        logDebug("◉ 胎胚[{}]当前库存：{}条", cxStock.getEmbryoCode(),currentStock);
        if (isTomorrow) {
            currentStock = currentStock + (cxScheduleResult.getClass1PlanQty() == null ? 0 : cxScheduleResult.getClass1PlanQty());
            currentStock = currentStock + (cxScheduleResult.getClass2ModifyQty() == null ? 0 : cxScheduleResult.getClass2ModifyQty());
            currentStock = currentStock + (cxScheduleResult.getClass3ModifyQty() == null ? 0 : cxScheduleResult.getClass3ModifyQty());
            logDebug("◉ 胎胚[{}]当前库存预测：{}条", cxStock.getEmbryoCode(), currentStock);
        } else {
            // currentStock = currentStock + (cxScheduleResult.getClass2ModifyQty() == null ? 0 : cxScheduleResult.getClass2ModifyQty());
            //logDebug("◉ 胎胚[{}]库存依据前日成型新增：{}条，预测量更新为：{}", cxStock.getEmbryoCode(), cxScheduleResult.getClass2ModifyQty(), currentStock);
            // todo 现场核对调整
            currentStock = currentStock + (cxScheduleResult.getClass1PlanQty() == null ? 0 : cxScheduleResult.getClass1PlanQty());
            logDebug("◉ 胎胚[{}]库存依据前日成型新增：{}条，预测量更新为：{}", cxStock.getEmbryoCode(), cxScheduleResult.getClass1PlanQty(), currentStock);
            currentStock = currentStock + (cxScheduleResult.getClass2ModifyQty() == null ? 0 : cxScheduleResult.getClass2ModifyQty());
            logDebug("◉ 胎胚[{}]库存依据前日成型新增：{}条，预测量更新为：{}", cxStock.getEmbryoCode(), cxScheduleResult.getClass2ModifyQty(), currentStock);
        }

        // 1.3.2：库存最低是0不能是负数
        if (currentStock < 0) {
            currentStock = 0;
        }
        return currentStock;
    }


    /**
     * ----------------------------- Step 2: 计算欠胎时间-----------------------------
     * 根据当前库存和班次计划，计算每个规格的欠胎时间
     */
    private void calculateMissTireTimeForSpecification() {
        if (LH_MISS_TIRE_TIME_CONTEXT_MAP.isEmpty()) {
            logDebug("无待处理的欠胎时间计算任务");
            return;
        }

        LH_MISS_TIRE_TIME_CONTEXT_MAP.forEach(dto -> {
            try {
                processSingleSpecification(dto);
            } catch (Exception e) {
                logError("计算任务[{}]欠胎时间发生异常：{}", dto, e.getMessage(), e);
            }
        });
    }


    /**
     * 处理单个规格的欠胎时间计算
     * @param dto 调度结果数据传输对象
     */
    private void processSingleSpecification(LhAlgorithmScheduleResultDto dto) {
        LhScheduleResult lhScheduleResult = dto.getLhScheduleResult();
        String embryoCode = lhScheduleResult.getEmbryoCode();
        logDebug("▷ 开始处理胎胚[{}]任务ID[{}]欠胎时间！", embryoCode, lhScheduleResult.getMergeIds());

        // 获取或创建库存信息
        CxStock stock = getOrCreateStock(embryoCode);
        int remainingStock = stock.getStockNum();
        logDebug("胎胚[{}]初始库存量：{}", embryoCode, remainingStock);
        dto.setInitialInventory(remainingStock);

        boolean timeCalculated = false;
        List<Integer> shiftConfigValidClasses = cxShiftConfig.getValidClasses();

        // 遍历班次判断欠胎
        for (int classCode : shiftConfigValidClasses) {
            int planQty = (int) lhScheduleResult.getFieldValueByFieldName("class" + classCode + "PlanQty");

            if (planQty < 0) {
                continue;
            }

            if (planQty >  remainingStock) {
                logInfo("◉ 胎胚[{}]在[{}]班消耗{}欠胎", embryoCode, classCode, planQty);
                calculateAndSetTime(embryoCode, dto, lhScheduleResult, classCode, remainingStock, planQty);
                timeCalculated = true;
                break;
            }

            remainingStock = remainingStock - planQty;
            logInfo("◉ 胎胚[{}]在[{}]班消耗{}不欠胎,  下一个班胎胚库存预测为：{}", embryoCode, classCode, planQty, remainingStock);
        }

        if (!timeCalculated) {
            dto.setPreviousTireTime(cxShiftConfig.parseToDayLastShiftEndTime());
            logInfo("◉ 胎胚[{}]欠胎时间计算：连续1日不欠胎 - 最终欠胎时间：[{}] ",
                    embryoCode,
                    dto.getPreviousTireTime().format(formatter)
            );
        }
    }


    /**
     * 获取或创建库存对象
     * @param embryoCode 胎胚编码
     * @return 库存对象
     */
    private CxStock getOrCreateStock(String embryoCode) {
        CxStock stock = CX_STOCK_NUM_CONTEXT_MAP.get(embryoCode);
        if (stock == null) {
            logWarn("胎胚[{}]库存数据缺失，创建默认库存", embryoCode);
            stock = new CxStock();
            stock.setEmbryoCode(embryoCode);
            stock.setStockNum(0);
            CX_STOCK_NUM_CONTEXT_MAP.put(embryoCode, stock);
        }
        return stock;
    }


    /**
     * 计算并设置欠胎时间（核心算法）
     */
    public void calculateAndSetTime(
            String embryoCode,
            LhAlgorithmScheduleResultDto dto,
            LhScheduleResult schedule,
            int classCode,
            int usedStock, int planQty) {
        try {
            // 参数校验
            if (schedule.getMoldQty() == 0) {
                logError("X 模数不能为0，任务ID：{}", buildStringFromEntity(dto));
                return;
            }

            // 时间计算公式[向上取整]：单胎硫化时长：单位秒 * 库存 / 模数
            BigDecimal seconds = BigDecimal.valueOf(usedStock)
                    .multiply(BigDecimal.valueOf(cxShiftConfig.getShiftDuration() * 3600L))
                    .divide(BigDecimal.valueOf(planQty), 0, RoundingMode.UP);

            // 获取指定班次开始时间
            LocalDateTime startTime = LocalDateTime.parse(cxShiftConfig.getShiftTimeByString("class" + classCode + "PlanQty").get("startTime"), formatter);

            // 获取冷却时间
            Long coolingTime = commonCacheService.getLhCoolingTime(CX_PARAMS_CONTEXT_MAP);

            // 最终结果和一班的开始时间比较
            LocalDateTime finalTime = startTime.plusSeconds(seconds.longValue()).minusSeconds(coolingTime);

            // 欠胎时间与班次开始时间比较更新
            if (finalTime.isBefore(cxShiftConfig.parseToDayFirstShiftStartTime())){
                finalTime = cxShiftConfig.parseToDayFirstShiftStartTime();
            }

            // 最终结果和最后一个班结束时间比较
            if (finalTime.isAfter(cxShiftConfig.parseToDayLastShiftEndTime())){
                finalTime = cxShiftConfig.parseToDayLastShiftEndTime();
            }

            // 设置计算结果
            dto.setPreviousTireTime(finalTime);

            logInfo("◉ 胎胚[{}]欠胎时间计算：第[{}] 班：开始时间：{} +  库存 ：{} / 总计划量：{}  * 班次时长：{} - 冷却时间：{} = 最终欠胎时间：[{}] ",
                    embryoCode,
                    classCode,
                    startTime.format(formatter),
                    usedStock,
                    planQty,
                    cxShiftConfig.getShiftDuration(),
                    coolingTime,
                    dto.getPreviousTireTime().format(formatter)
            );
        } catch (Exception e) {
            logError("计算欠胎时间失败：{}", e.getMessage());
        }
    }


    /**
     * ------------------------------- Step 3: 标记限制/续作/收尾/普通/新增/大规格/小规格 -----------------------------
     * 对任务进行标记，包括限制、续作、收尾、普通、新增、大规格、小规格等。
     *
     * @param correctedCxScheduleResults 前日成型排程计划
     * @param yesterdayLhScheduleResults 前日硫化排程计划
     * @param scheduleDate               排程日期
     */
    private void markTasks(List<CxScheduleResultOld> correctedCxScheduleResults, List<LhScheduleResult> yesterdayLhScheduleResults, Date scheduleDate) {
        logInfo("【Step 3.1: 任务标记限制/续作/收尾/普通/新增/大规格/小规格开始=========》】");

        // 3.1.数据准备：前日成型排程按[机台编号]分组
        Map<String, List<CxScheduleResultOld>> groupResultByMachineCode = correctedCxScheduleResults.stream()
                .collect(Collectors.groupingBy(CxScheduleResultOld::getCxMachineCode));
        logInfo("前日成型排程按机台编号分组完成，分组数量：" + groupResultByMachineCode.size());

        // 3.2.数据准备：限制机台信息依据[胎胚]分组
        QueryWrapper<ProductMoldingLimit> queryWrapperProductMoldingLimit = new QueryWrapper<>();
        List<ProductMoldingLimit> limitMachines = productMoldingLimitMapper.selectList(queryWrapperProductMoldingLimit);
        Map<String, List<ProductMoldingLimit>> limitMachinesByMachineCode = limitMachines.stream()
                .collect(Collectors.groupingBy(ProductMoldingLimit::getEmbryoCode));
        logInfo("限制机台信息按胎胚分组完成，分组数量：" + limitMachinesByMachineCode.size());

        // 3.3.数据准备：获取成型[胎胚+Bom]汇总
        List<CxEmbryoMonthPlanSurplus> cxEmbryoMonthPlanSurplusList = commonQueryCacheService.getMonthRemainQtyList(scheduleDate);
        for (CxEmbryoMonthPlanSurplus item : cxEmbryoMonthPlanSurplusList) {
            CX_STOCK_MONTH_REMAIN_CONTEXT_MAP.put(item.getMaterialCode() + item.getBomDataVersion(), item);
        }

        // 3.4.数据准备：将前日成型依据[胎胚+Bom]分组
        Map<String, List<CxScheduleResultOld>> groupResultByEmbryoCode = correctedCxScheduleResults.stream()
                .collect(Collectors.groupingBy(item -> item.getEmbryoCode() + item.getBomDataVersion()));
        logInfo("前日成型排程按胎胚+Bom分组完成，分组数量：" + groupResultByEmbryoCode.size());

        // 3.5.数据准备：施工信息依据[胎+Bom]分组
        Map<String, List<CxProductConstructionInfoDto>> cxProductConstructionInfoDtoMap = commonQueryCacheService.queryEmbryoCodeInfo(LH_MISS_TIRE_TIME_CONTEXT_MAP).stream()
                .collect(Collectors.groupingBy(item -> item.getEmbryoCode() + item.getEmbryoVersion()));
        logInfo("施工信息按胎+Bom分组完成，分组数量：" + cxProductConstructionInfoDtoMap.size());

        // 3.6.遍历任务上下文进行标记
        for (LhAlgorithmScheduleResultDto lhAlgorithmScheduleResultDto : LH_MISS_TIRE_TIME_CONTEXT_MAP) {
            logInfo("开始处理任务：" + lhAlgorithmScheduleResultDto.getLhScheduleResult());

            // 3.6.1.标记续作：判断是否为续作任务
            boolean isContinueTire = isContinueTire(lhAlgorithmScheduleResultDto, groupResultByMachineCode);
            lhAlgorithmScheduleResultDto.setIsContinueTire(isContinueTire);
            logInfo("标记续作完成，结果：" + isContinueTire);

            // 3.6.2.标记限制：判断是否为限制任务
            boolean isLimitTire = isLimitTire(lhAlgorithmScheduleResultDto, limitMachinesByMachineCode);
            lhAlgorithmScheduleResultDto.setIsLimitTire(isLimitTire);
            logInfo("标记限制完成，结果：" + isLimitTire);

            // 3.6.3.标记收尾：判断是否为收尾任务
            boolean isEndTire =  isEndTire(lhAlgorithmScheduleResultDto,CX_STOCK_MONTH_REMAIN_CONTEXT_MAP);
            lhAlgorithmScheduleResultDto.setIsEndTire(isEndTire);
            logInfo("标记收尾完成，结果：" + isEndTire);

            // 3.6.4.标记新增：判断是否为新增任务
            boolean isNewTire =  isNewTire(lhAlgorithmScheduleResultDto, groupResultByEmbryoCode);
            lhAlgorithmScheduleResultDto.setIsNewTire(isNewTire);
            logInfo("标记新增完成，结果：" + isNewTire);

            // 3.6.5.标记普通：判断是否为普通任务
            boolean isNormalTire =isNormalTire(lhAlgorithmScheduleResultDto, groupResultByEmbryoCode);
            lhAlgorithmScheduleResultDto.setIsNormalTire(isNormalTire);
            logInfo("标记普通完成，结果：" + isNormalTire);

            // 3.6.6.标记大小规格：判断是否为大规格任务
            boolean isLargeTire = isLargeSpecification(lhAlgorithmScheduleResultDto);
            lhAlgorithmScheduleResultDto.setIsLargeTire(isLargeTire);
            logInfo("标记大规格完成，结果：" + isLargeTire);

            // 3.6.7.匹配施工信息：关联施工信息
            String key = lhAlgorithmScheduleResultDto.getLhScheduleResult().getEmbryoCode() + lhAlgorithmScheduleResultDto.getLhScheduleResult().getBomVersion();
            if (cxProductConstructionInfoDtoMap.containsKey(key)) {
                List<CxProductConstructionInfoDto> cxProductConstructionInfoDtoList = cxProductConstructionInfoDtoMap.get(key);
                if (!cxProductConstructionInfoDtoList.isEmpty()) {
                    lhAlgorithmScheduleResultDto.setCxProductConstructionInfoDto(cxProductConstructionInfoDtoList.get(0));
                    logInfo("匹配施工信息完成，关联成功");
                }
            } else {
                logInfo("匹配施工信息完成，未找到关联信息");
            }

            // 3.6.8.日志保存：记录任务详细信息
            logInfo("任务处理完成，详细信息：" + buildStringFromEntity(lhAlgorithmScheduleResultDto));
        }

        logInfo("【Step 3.1: 任务标记限制/续作/收尾/普通/新增/大规格/小规格结束=========》】");
    }


    /**
     * 3.3.1 标记判断规格是否续作规格
     *
     * @param lhAlgorithmScheduleResultDto 硫化任务对象
     * @param groupResultByMachineCode     前日成型排程按机台编号分组列表
     * @return 是否续作
     */
    @Override
    public boolean isContinueTire(LhAlgorithmScheduleResultDto lhAlgorithmScheduleResultDto, Map<String, List<CxScheduleResultOld>> groupResultByMachineCode) {
        // 1 遍历前日成型排程按机台编号分组列表
        for (Map.Entry<String, List<CxScheduleResultOld>> entry : groupResultByMachineCode.entrySet()) {
            LhScheduleResult lhScheduleResult = lhAlgorithmScheduleResultDto.getLhScheduleResult();
            List<CxScheduleResultOld> results = entry.getValue();

            // 找到机台中前日最后一个班次 Sort 最大 并且  plan > 0 的
            String classPlanKey = "class" + cxShiftConfig.getShiftCount() + "ModifyQty";
            String classSortKey = "class" + cxShiftConfig.getShiftCount() + "ModifySort";
            Optional<CxScheduleResultOld> maxResult = results.stream()
                    .filter(result -> result.getFieldValueByFieldName(classPlanKey) != null && (int) result.getFieldValueByFieldName(classPlanKey) > 0 && result.getFieldValueByFieldName(classSortKey) != null)
                    .max(Comparator.comparing(item -> (int) item.getFieldValueByFieldName(classSortKey)));

            if (maxResult.isPresent() && lhScheduleResult.getEmbryoCode().equals(maxResult.get().getEmbryoCode())) {
                logInfo("胎胚：[{}]，是前日机台：[{}], 最后一个班生产的最后规格，标记续作", maxResult.get().getEmbryoCode(), entry.getKey());
                return Boolean.TRUE;
            }
        }

        return Boolean.FALSE;
    }


    /**
     * ----------------------------- Step 2: 初始化机台上下文 -----------------------------
     * 功能：构建可用机台的时间上下文，并匹配培训额度到指定/非指定机台
     * 逻辑：
     * 1. 获取当天可用机台集合
     * 2. 查询当日培训档数配置
     * 3. 初始化机台基础时间配置
     * 4. 遍历所有班次和成型法类型进行培训额度匹配
     *
     * @param scheduleDate 排程基准日期（时区敏感，需与配置对齐）
     */
    private void initializeMachineActiveTimeContext(Date scheduleDate) {
        try {
            // Step 1.1:日志标记步骤开始
            logInfo(">>>>>> 开始初始化机台上下文 <<<<<<");

            // Step 2.1: 获取可用成型机集合（Key: 机台编码, Value: 机台详情）
            Map<String, CxMachineInfoVo> moldingMachines = iCxMatchingSpecifyMachineService.getAvailableMoldingMachine(scheduleDate);
            logInfo("当前可用机台数量: {}", moldingMachines.size());

            // Step 2.2: 获取当日培训档数配置（数据库查询）
            QueryWrapper<CxPersionTrainSetting> trainSettingQuery = new QueryWrapper<>();
            // 按排程日期过滤
            trainSettingQuery.eq("SCHEDULE_DATE", scheduleDate);
            List<CxPersionTrainSetting> trainSettings = cxPersionTrainSettingEntityMapper.selectList(trainSettingQuery);
            logInfo("获取培训档数配置记录数: {}", trainSettings.size());
            logInfo("---- 开始初始化机台时间配置 ----");
            moldingMachines.values().forEach(machine -> {
                // 设置机台时段配置（依赖cxShiftConfig解析）首班开始时间
                machine.setAvailableBeginTime(cxShiftConfig.parseToDayFirstShiftStartTime());
                // 设置机台时段配置（依赖cxShiftConfig解析）末班结束时间
                machine.setAvailableEndTime(cxShiftConfig.parseToDayLastShiftEndTime());

                // 计算总可用时间 = 单班时长(小时) * 总班次数
                int totalMinutes = cxShiftConfig.getShiftDuration() * cxShiftConfig.getShiftCount();
                machine.setRemainTime(BigDecimal.valueOf(totalMinutes));

                // 加入全局上下文缓存
                CX_MACHINE_ACCTIVE_TIME_CONTEXT_MAP.put(machine.getCxMachineCode(), machine);
                logDebug("机台[{}]初始化完成 | 开始时间: {} | 结束时间: {} | 总剩余可用时长: {}小时",
                        machine.getCxMachineCode(),
                        machine.getAvailableBeginTime(),
                        machine.getAvailableEndTime(),
                        totalMinutes);
            });

            logInfo("---- 开始培训额度匹配 ----");
            // 总班次数
            final int shiftCount = cxShiftConfig.getShiftCount();
            // 成型法类型上限
            final int machineTypeMax = Integer.parseInt(CxEngineConstants.MACHINE_TYPE_TWICE);

            // 遍历所有班次 (i=班次序号)
            for (int shiftIndex = 1; shiftIndex <= shiftCount; shiftIndex++) {
                // 遍历所有成型法类型 (j=成型法类型)
                for (int moldingType = 1; moldingType <= machineTypeMax; moldingType++) {
                    logDebug("正在处理班次[{}]-成型法[{}]...", shiftIndex, moldingType);

                    // 匹配逻辑分两类：指定机台逻辑
                    specifiedMachineMatching(scheduleDate, trainSettings, moldingType, shiftIndex);
                    // 匹配逻辑分两类：非指定机台逻辑
                    unspecifiedMachineMatching(scheduleDate, trainSettings, moldingType, shiftIndex);
                }
            }

            logInfo(">>>>>> 机台上下文初始化完成 <<<<<<");
        } catch (Exception e) {
            logError("初始化机台上下文发生异常: {}", e.getMessage(), e);
            throw new RuntimeException("初始化机台上下文失败，请检查配置或数据");
        }
    }

    /**
     * 培训档数指定机台的培训额匹配
     * 功能：将指定机台的培训配额分配到对应机台子类
     * 逻辑：
     * 1. 获取指定机台的培训配额映射表（机台编码 → 配额数）
     * 2. 遍历每个机台，更新其所有子类的对应班次配额字段
     *
     * @param scheduleDate             排程日期（时区需与上下文配置一致）
     * @param cxPersonTrainSettingList 人员培训配置列表（不可为null，建议空集合）
     * @param moldingMethod            成型法类型（范围：1-2，对应Constants.MACHINE_TYPE_*）
     * @param shift                    班次序号（范围：1-{shiftCount}）
     */
    private void specifiedMachineMatching(Date scheduleDate,
                                          List<CxPersionTrainSetting> cxPersonTrainSettingList,
                                          int moldingMethod,
                                          int shift) {
        try {
            logInfo(">>>>>> 开始处理指定机台培训配额匹配 [成型法{}][班次{}] <<<<<<", moldingMethod, shift);

            // 获取指定机台的人员培训数量配置（Key: 机台编码, Value: 培训数量）
            Map<String, Integer> machineQuotas = getSpecifiedMachineQuotas(
                    cxPersonTrainSettingList,
                    scheduleDate,
                    moldingMethod,
                    shift
            );
            logDebug("获取到{}条的指定配置", machineQuotas.size());

            // 统计更新的寸口子类数量
            int updatedSubclassCount = 0;
            // 获取培训档定额系数
            double trainingCoefficient = commonCacheService.getTrainingCoefficient(CX_PARAMS_CONTEXT_MAP);
            for (Map.Entry<String, Integer> entry : machineQuotas.entrySet()) {
                String machineCode = entry.getKey();
                Integer trainQuota = entry.getValue();
                trainQuota = (int) (trainQuota * trainingCoefficient);



                // 1. 获取机台上下文信息
                CxMachineInfoVo machine = CX_MACHINE_ACCTIVE_TIME_CONTEXT_MAP.get(machineCode);
                if (machine == null) {
                    logWarn("机台[{}]在上下文中不存在，跳过配额分配", machineCode);
                    continue;
                }

                // 2. 遍历机台所有寸口子类配置
                List<MdmMoldingMachineClsB> subClasses = machine.getMoldingMachineClassList();
                for (MdmMoldingMachineClsB clsB : subClasses) {
                    // 动态设置班次配额字段（如class1MachineQty）
                    String fieldName = String.format("class%dMachineQty", shift);
                    clsB.setFieldValueByFieldName(fieldName, trainQuota);

                    logDebug("更新机台寸口子类配额 | 机台={} | 子类寸口={}存 | 班次={} | 定额={}",
                            machineCode, clsB.getProSize(), shift, trainQuota);
                    updatedSubclassCount++;
                }
            }

            logInfo("完成定额分配 | 总机台数={} | 影响子类数={}", machineQuotas.size(), updatedSubclassCount);
        } catch (Exception e) {
            logError("指定机台配额匹配发生异常: {}", e.getMessage(), e);
            throw new RuntimeException("指定机台培训配额分配失败，请检查数据或配置");
        }
    }


    /**
     * 培训档数未指定机台的培训额匹配最佳机台
     * 功能：为未指定具体机台的培训额度寻找最匹配的机台子类（生产定额最接近的）
     * 逻辑：
     * 1. 获取未指定机台的培训额度列表
     * 2. 对每个额度寻找最佳匹配机台：
     * a. 过滤符合成型法的机台
     * b. 排除已指定配额的子类
     * c. 计算生产定额差异，记录最小差异的机台
     * 3. 更新最佳机台所有子类的配额
     *
     * @param scheduleDate             排程日期（时区需与上下文一致）
     * @param cxPersonTrainSettingList 培训配置列表（需包含未指定机台的记录）
     * @param moldingMethod            成型法类型（范围：1-2，对应Constants.MOLD_METHOD_*）
     * @param shift                    班次序号（范围：1-{shiftCount}）
     */
    private void unspecifiedMachineMatching(Date scheduleDate,
                                            List<CxPersionTrainSetting> cxPersonTrainSettingList,
                                            int moldingMethod,
                                            int shift) {
        try {
            logInfo(">>>>>> 开始未指定机台配额匹配 [成型法{}][班次{}] <<<<<<", moldingMethod, shift);

            List<Integer> quotas = getUnspecifiedMachineQuotas(
                    cxPersonTrainSettingList,
                    scheduleDate,
                    moldingMethod,
                    shift
            );
            logInfo("未指定机台的培训配额数量: {}", quotas.size());
            String skipMachineNo  = commonCacheService.getSkipMachineNo(CX_PARAMS_CONTEXT_MAP);
            logInfo("不参与培训配额成型机: {}", skipMachineNo);
            // 获取培训档定额系数
            double trainingCoefficient = commonCacheService.getTrainingCoefficient(CX_PARAMS_CONTEXT_MAP);


            //遍历没有指定机台的培训额列表
            for (Integer targetQuota : quotas) {
                logDebug("正在处理配额: {}", targetQuota);

                // 最佳匹配追踪器
                int minDifference = Integer.MAX_VALUE;
                MdmMoldingMachineClsB bestSubclass = null;
                CxMachineInfoVo bestMachine = null;

                // 遍历机台上下文
                for (CxMachineInfoVo machine : CX_MACHINE_ACCTIVE_TIME_CONTEXT_MAP.values()) {
                    // 1. 过滤成型法不匹配的机台
                    if (!isMoldingMethodMatch(machine, moldingMethod)) {
                        logDebug("机台[{}]成型法不匹配，跳过", machine.getCxMachineCode());
                        continue;
                    }

                    if (skipMachineNo != null && skipMachineNo.contains(machine.getCxMachineCode())) {
                        logDebug("机台[{}]在跳过名单中，跳过", machine.getCxMachineCode());
                        continue;
                    }

                    //获取昨日成型寸口
                    BigDecimal yesterdayMachineSize = CX_MACHINE_QUOTA_CONTEXT_MAP.get(machine.getCxMachineCode());

                    // 2. 获取当前机台可用子类列表（未分配配额且生产定额最小）
                    // 过滤未分配配额的子类
                    // 按生产定额升序排序（取昨日续作的寸口）
                    Optional<MdmMoldingMachineClsB> optionalSubclass = Optional.empty();
                    try {
                        optionalSubclass = machine.getMoldingMachineClassList()
                                .stream()
                                // 过滤未分配配额的子类
                                .filter(sub -> sub.getFieldValueByFieldName("class" + shift + "MachineQty") == null)
                                .filter(sub -> sub.getProSize().compareTo(yesterdayMachineSize) == 0)
                                .findFirst();
                    }catch (Exception e){
                        throw new RuntimeException("机台[{"+machine.getCxMachineCode()+"}]获取昨日寸口失败");
                    }


                    // 3. 无可用子类时跳过
                    if (!optionalSubclass.isPresent()) {
                        logDebug("机台[{}]无可用子类（所有子类已分配配额）", machine.getCxMachineCode());
                        continue;
                    }

                    // 4. 获取目标子类及其数据
                    MdmMoldingMachineClsB targetSubclass = optionalSubclass.get();
                    Integer currentQuota = targetSubclass.getProductionQuotaQty();

                    // 5. 计算差异值
                    int difference = Math.abs(currentQuota - targetQuota);
                    logDebug("差异计算 | 机台={} 子类寸口={} 当前配额={} 目标={} 差异={}",
                            machine.getCxMachineCode(),
                            targetSubclass.getProSize(),
                            currentQuota,
                            targetQuota,
                            difference);

                    // 6. 更新最佳匹配记录
                    if (difference < minDifference) {
                        minDifference = difference;
                        bestSubclass = targetSubclass;
                        bestMachine = machine;
                        logDebug("发现更优匹配 | 机台={} 子类寸口={} 差异={}",
                                machine.getCxMachineCode(),
                                targetSubclass.getProSize(),
                                difference);
                    }
                }

                if (bestMachine != null) {
                    logInfo("分配配额 | 机台={} 子类寸口={} 班次={} 配额={} 差异={}",
                            bestMachine.getCxMachineCode(), bestSubclass.getProSize(),
                            shift, targetQuota, minDifference);

                    // 更新该机台所有子类（根据业务需求确认是否需要更新全部子类）
                    String fieldName = String.format("class%dMachineQty", shift);
                    for (MdmMoldingMachineClsB subclass : bestMachine.getMoldingMachineClassList()) {
                        subclass.setFieldValueByFieldName(fieldName, (int) targetQuota * trainingCoefficient);
                        logDebug("更新子类配额 | 机台={} 子类寸口={} 字段={} 值={}",
                                bestMachine.getCxMachineCode(), subclass.getProSize(),
                                fieldName, (int) targetQuota * trainingCoefficient);
                    }

                } else {
                    logWarn("未找到符合条件的机台 | 目标配额={} 成型法={} 班次={}",
                            targetQuota, moldingMethod, shift);
                }
            }

            logInfo(">>>>>> 未指定机台配额匹配完成 <<<<<<");
        } catch (Exception e) {
            logError("未指定机台配额匹配异常: {}", e.getMessage(), e);
            throw new RuntimeException("未指定机台培训配额分配失败");
        }
    }


    /**
     * 培训档数未指定机台的培训额匹配最佳机台: 辅助方法
     *
     * @param machine      机台
     * @param targetMethod 成型法
     * @return 是否匹配
     */
    private boolean isMoldingMethodMatch(CxMachineInfoVo machine, int targetMethod) {
        return machine.getMoldingMachineCls().getMouldMethod().equals(targetMethod);
    }

    /**
     * 核心逻辑：当新任务抢占机台时，计算机台时间窗口变化，拆分受影响任务的产能，并重新调度任务
     *
     * @param task    需要进行抢占的调度任务
     * @param machine 被抢占的目标机台
     * @throws NullPointerException 当关键参数为null时抛出
     */
    @Override
    public void performMachineOccupation(LhAlgorithmScheduleResultDto task, CxMachineInfoVo machine) {
        // 抢占任务的开始时间
        final LocalDateTime previousTireTime = task.getPreviousTireTime();

        // 获取施工信息
        CxProductConstructionInfoDto taskSpec = task.getCxProductConstructionInfoDto();
        CxProductConstructionInfoDto machineSpec = machine.getCxProductConstructionInfo();
        if (machineSpec == null) {
            logWarn("机台[{}]无前序任务，直接分配新任务", machine.getCxMachineCode());
            tryAllocateToMachine(task, machine);
            return;
        }

        // 换装时间计算
        final BigDecimal changeoverHours = BigDecimal.valueOf(changeSpecTime(taskSpec, machineSpec,Integer.valueOf(machine.getRollOverType())))
                .setScale(2, RoundingMode.HALF_UP);
        logDebug("换装时间计算 | 规格变更: {} → {} | 耗时: {}小时",
                taskSpec.getEmbryoCode(), machineSpec.getEmbryoCode(), changeoverHours);

        // 获取前规格
        LhAlgorithmScheduleResultDto previousTask = machine.getLhAlgorithmScheduleResultDto();

        // 前规格的开始时间
        LocalDateTime originalStartTime = previousTask.getStartTime();

        // 前规格的结束时间[依据抢占任务重新计算的]
        final long changeoverSeconds = changeoverHours.multiply(BigDecimal.valueOf(3600)).longValueExact();
        final LocalDateTime machineNewAvailableTime = previousTireTime.minusSeconds(changeoverSeconds);

        //  前规格的新的结束时间 - 前规格开始时间
        final long secondsDifference = ChronoUnit.SECONDS.between(machineNewAvailableTime, originalStartTime);
        if (secondsDifference <= 0) {
            // 前规格可生产时间
            int previousTaskTaskPlanQuantity = previousTask.getTaskPlanQuantity();

            // 机台将前规格回滚 传 isReload = true 还有计划量 = 0
            previousTask.setTaskPlanQuantity(0);
            executeShiftScheduling(previousTask, machine, originalStartTime, true);

            // 前任务解绑机台,需要将任务标识[是否安排]改掉,进入普通分组
            previousTask.setTaskPlanQuantity(previousTaskTaskPlanQuantity);
            previousTask.setIsScheduleEnd(false);

            // 抢占任务安排机台
            tryAllocateToMachine(task, machine);
            return;
        }


        final int currentShift = cxShiftConfig.getShiftNumber(machineNewAvailableTime);
        final String quotaField = "class" + currentShift + "MachineQty";

        // 班次定额获取
        final Object rawQuota = machine.getFieldValueByFieldName(quotaField);
        final BigDecimal shiftQuota = BigDecimal.valueOf((int) rawQuota);

        // 每秒产能产能计算
        final BigDecimal shiftDuration = BigDecimal.valueOf(cxShiftConfig.getShiftDuration() * 3600L);
        final BigDecimal hourlyOutput = shiftQuota.divide(shiftDuration, 2, RoundingMode.HALF_UP);
        logDebug("产能计算 | 班次: {} | 定额: {} | 每秒产能: {}", currentShift, shiftQuota, hourlyOutput);

        // 需要拆分的产能量
        final BigDecimal splitQty = hourlyOutput.multiply(BigDecimal.valueOf(secondsDifference))
                .setScale(0, RoundingMode.HALF_DOWN);
        logInfo("任务拆分需求 | 需拆分产能: {}个 | 计算公式: 可用时间{}秒 × 每秒产能{}",
                splitQty, secondsDifference, hourlyOutput);

        if (splitQty.compareTo(BigDecimal.ZERO) > 0) {
            logDebug("启动任务拆分 | 原任务ID: {}", previousTask.getLhScheduleResult().getMergeIds());
            splitTask(previousTask, splitQty.intValueExact());
        }

        // 前规格重排
        executeShiftScheduling(previousTask, machine, originalStartTime, true);

        // 抢占规格安排机台
        tryAllocateToMachine(task, machine);
    }

    /**
     * 判断是否收尾
     *
     * @param lhAlgorithmScheduleResultDto 排查任务对象
     * @param cxEmbryoMonthPlanSurplusMap  月度剩余量分组集合
     * @return 是否收尾
     */
    @Override
    public boolean isEndTire(LhAlgorithmScheduleResultDto lhAlgorithmScheduleResultDto, Map<String, CxEmbryoMonthPlanSurplus> cxEmbryoMonthPlanSurplusMap) {
        // 获取成型参数一次性收尾
        int onceCloseOutQtyParamValue = commonCacheService.getOncePlanMonthRemain(CX_PARAMS_CONTEXT_MAP);
        LhScheduleResult lhScheduleResult = lhAlgorithmScheduleResultDto.getLhScheduleResult();
        // 匹配月度剩余量
        if (cxEmbryoMonthPlanSurplusMap.containsKey(lhScheduleResult.getEmbryoCode() + lhScheduleResult.getBomVersion())) {
            CxEmbryoMonthPlanSurplus cxEmbryoMonthPlanSurplus = cxEmbryoMonthPlanSurplusMap.get(lhScheduleResult.getEmbryoCode() + lhAlgorithmScheduleResultDto.getLhScheduleResult().getBomVersion());
            lhAlgorithmScheduleResultDto.setCxEmbryoMonthPlanSurplus(cxEmbryoMonthPlanSurplus);
            // 月度剩余量是0 则标记为收尾
            if (cxEmbryoMonthPlanSurplus != null && cxEmbryoMonthPlanSurplus.getMonthRemainQty() == 0) {
                logInfo("胎胚：[{}]，胎胚月剩余量为0，标记收尾", lhScheduleResult.getEmbryoCode());
                lhAlgorithmScheduleResultDto.setIsEndTire(Boolean.TRUE);
                return Boolean.TRUE;
            }
            // 月度剩余量是一次收尾量则标记为收尾
            if (cxEmbryoMonthPlanSurplus != null && cxEmbryoMonthPlanSurplus.getMonthRemainQty() <= onceCloseOutQtyParamValue) {
                logInfo("胎胚：[{}]，胎胚月剩余量为：[{}]，满足一次性收尾，标记收尾", lhScheduleResult.getEmbryoCode(),cxEmbryoMonthPlanSurplus.getMonthRemainQty());
                lhAlgorithmScheduleResultDto.setIsEndTire(Boolean.TRUE);
                return Boolean.TRUE;
            }
            logInfo("胎胚：[{}]，胎胚月剩余量为：[{}]，不满足一次性收尾", lhScheduleResult.getEmbryoCode(),cxEmbryoMonthPlanSurplus == null ? 0 : cxEmbryoMonthPlanSurplus.getMonthRemainQty());
        }
        return Boolean.FALSE;
    }


    /**
     * 判断是否大规格
     *
     * @param lhAlgorithmScheduleResultDto 排程任务
     * @return 是否大规格
     */
    @Override
    public boolean isLargeSpecification(LhAlgorithmScheduleResultDto lhAlgorithmScheduleResultDto) {
        LhScheduleResult lhScheduleResult = lhAlgorithmScheduleResultDto.getLhScheduleResult();
        // 获取参数
        int masSpecQtyParamValue = commonCacheService.getBigSpecMold(CX_PARAMS_CONTEXT_MAP);
        if (lhScheduleResult.getMoldQty() >= masSpecQtyParamValue) {
            lhAlgorithmScheduleResultDto.setIsLargeTire(Boolean.TRUE);
            lhAlgorithmScheduleResultDto.setIsSmallTire(Boolean.FALSE);
            logInfo("胎胚：[{}]，使用模数：[{}],  满足大规格限制[{}]，标记大规格", lhScheduleResult.getEmbryoCode(), lhScheduleResult.getMoldQty(), masSpecQtyParamValue);
            return Boolean.TRUE;
        } else {
            logInfo("胎胚：[{}]，使用模数：[{}],  小于大规格限制[{}]，标记小规格", lhScheduleResult.getEmbryoCode(), lhScheduleResult.getMoldQty(), masSpecQtyParamValue);
            lhAlgorithmScheduleResultDto.setIsLargeTire(Boolean.FALSE);
            lhAlgorithmScheduleResultDto.setIsSmallTire(Boolean.TRUE);
            return Boolean.FALSE;
        }
    }

    /**
     * 检查欠胎时间是否落点有效
     */
    @Override
    public boolean isTimeWindowValid(LocalDateTime availableTime,
                                     LocalDateTime previousTireTime) {
        // 1.获取结合硫化可等待时间
        int lhWaitTime = commonCacheService.getLhWaitTime(CX_PARAMS_CONTEXT_MAP);
        // 2.机台可生产任务开始时间 - 硫化能等待时间 > 欠胎时间  ？ true 无法落点 ： false  可以落点
        return !availableTime.minusMinutes(lhWaitTime).isAfter(previousTireTime);
    }


    /**
     * 执行班次排产
     * @param task 任务
     * @param machine 机台
     * @param startTime  任务需要开始生产的时间
     * @param isReload 是否重排
     * @return 是否安排成功
     */
    @Override
    public boolean executeShiftScheduling(LhAlgorithmScheduleResultDto task,
                                          CxMachineInfoVo machine,
                                          LocalDateTime startTime, boolean isReload) {
        task.setStartTime(startTime);
        logInfo("初始化排产参数 | 任务ID[{}] | 机台[{}] | 任务开始在机台上可以开始生产的时间[{}]",
                task.getLhScheduleResult().getMergeIds(),
                machine.getCxMachineCode(),
                startTime);
        double changeTime = 0;

        final int totalShifts = cxShiftConfig.getShiftCount();
        final int currentShift = cxShiftConfig.getShiftNumber(startTime);
        final int maxChangeSpecNum = commonCacheService.getChangeSpecNum(CX_PARAMS_CONTEXT_MAP);

        if (task.getTaskPlanQuantity() == 0) {
            logInfo("任务量是0，不安排");
            return true;
        }

        // 重排回滚记录字段
        if (isReload){
            // 机台排程的班次恢复 ： 任务开始的班次
            machine.setCurrentShift(currentShift);
            // 机台排程的班次顺序恢复 ：成任务开始的班次顺序 -1
            String sortField = "class" + currentShift + "Sort";
            machine.setCurrentShiftSort((int)task.getFieldValueByFieldName(sortField) -1);

            // 机台排程的换工装次数恢复 ：成任务开始的班次顺序 -1 (最小值保持为0)
            String changeNumField = "class" + currentShift + "ChangeNum";
            Integer currentValue = (Integer) machine.getFieldValueByFieldName(changeNumField);
            int newValue = (currentValue == null || currentValue <= 0) ? 0 : currentValue - 1;
            machine.setFieldValueByFieldName(changeNumField, newValue);
            // 其它换工装次数归0
            for (int item = currentShift + 1 ; item <= 6; item++){
                String itemChangeNumField = "class" + item + "ChangeNum";
                machine.setFieldValueByFieldName(itemChangeNumField, 0);
            }

            // 胎胚的月度剩余量恢复排程前的剩余量
            CxEmbryoMonthPlanSurplus cxEmbryoMonthPlanSurplus = CX_STOCK_MONTH_REMAIN_CONTEXT_MAP.get(task.getLhScheduleResult().getEmbryoCode() + task.getLhScheduleResult().getBomVersion());
            if (cxEmbryoMonthPlanSurplus != null) {
                cxEmbryoMonthPlanSurplus.setMonthRemainQty(
                        (cxEmbryoMonthPlanSurplus.getMonthRemainQty() != null ? cxEmbryoMonthPlanSurplus.getMonthRemainQty() : 0)
                                + (task.getClass1PlanQty() != null ? task.getClass1PlanQty() : 0)
                                + (task.getClass2PlanQty() != null ? task.getClass2PlanQty() : 0)
                                + (task.getClass3PlanQty() != null ? task.getClass3PlanQty() : 0)
                );
                CX_STOCK_MONTH_REMAIN_CONTEXT_MAP.put(task.getLhScheduleResult().getEmbryoCode() + task.getLhScheduleResult().getBomVersion(), cxEmbryoMonthPlanSurplus);
            }

            // 任务清空设定值
            task.setClass1PlanQty(0);
            task.setClass2PlanQty(0);
            task.setClass3PlanQty(0);
            task.setClass1StartTime(null);
            task.setClass2StartTime(null);
            task.setClass3StartTime(null);
            task.setClass1EndTime(null);
            task.setClass2EndTime(null);
            task.setClass3EndTime(null);
            task.setClass1Sort(null);
            task.setClass2Sort(null);
            task.setClass3Sort(null);
            task.setClassMachineDefaultQty(null);
            task.setClass1MachineQty(null);
            task.setClass2MachineQty(null);
            task.setClass3MachineQty(null);
//            task.setClass1Analysis(null);
//            task.setClass2Analysis(null);
//            task.setClass3Analysis(null);
//            task.setEndTireDesc("");

        }else {
             changeTime = changeSpecTime(
                    task.getCxProductConstructionInfoDto(),
                    machine.getCxProductConstructionInfo(),
                    Integer.valueOf(machine.getRollOverType())
            );

            if (changeTime > 0) {
                // 更新机台换工装次数
                String changeNumField = "class" + currentShift + "ChangeNum";
                int changeNum = (int) machine.getFieldValueByFieldName(changeNumField);
                machine.setFieldValueByFieldName(changeNumField, changeNum + 1);
                logInfo("班次[{}]换工装次数 → {}", currentShift, changeNum + 1);
                task.setFieldValueByFieldName("class" + currentShift + "Analysis", String.format("换工装开班共耗时[%s]", changeTime) + (task.getEndTireDesc() == null ? "" : task.getEndTireDesc()));
            }else {
                logInfo("换工装时间是：0.0 ,班次[{}]换工装次数 → {}, 不变", currentShift, 0);
                task.setFieldValueByFieldName("class" + currentShift + "Analysis", (task.getEndTireDesc() == null ? "" : task.getEndTireDesc()));
            }
        }

        int remainingQty = task.getTaskPlanQuantity();
        logDebug("排产循环启动 | 班制: {} | 当前班次: {} | 最大换工装次数: {} | 初始剩余量: {}",
                totalShifts, currentShift, maxChangeSpecNum, remainingQty);

        for (int shift = currentShift; shift <= totalShifts && remainingQty > 0; shift++) {
            logDebug("班次处理开始 | 班次[{}] | 剩余量[{}]", shift, remainingQty);
            // 阶段2：检查换工装限制
            String changeNumField = "class" + shift + "ChangeNum";

            // 阶段1：获取班次定额
            Integer shiftQuota = getShiftQuota(machine, shift, task.getCxProductConstructionInfoDto(), task);
            if (shiftQuota == null || shiftQuota == 0) {
                logWarn("机台寸口没有维护定额 | 班次定额缺失 | 班次[{}] | 终止排产", shift);
                int changeNum = (int) machine.getFieldValueByFieldName(changeNumField);
                machine.setFieldValueByFieldName(changeNumField, changeNum - 1);
                return false;
            }

            int currentChangeNum = (int) machine.getFieldValueByFieldName(changeNumField);
            if (currentChangeNum > maxChangeSpecNum) {
                logWarn("换工装次数超限 | 班次[{}] | 当前[{}]/最大[{}]",
                        shift, currentChangeNum, maxChangeSpecNum);
                int changeNum = (int) machine.getFieldValueByFieldName(changeNumField);
                machine.setFieldValueByFieldName(changeNumField, changeNum - 1);
                return false;
            }

            // 获取本班次结束时间
            LocalDateTime shiftEndTime = LocalDateTime.parse(
                    cxShiftConfig.getShiftTimeByString("class" + shift).get("endTime"), formatter
            );

            // 计算本班次可用时长 = 班次结束时间 - 任务开始时间
            Duration availableDuration = Duration.between(startTime, shiftEndTime);
            BigDecimal availableSeconds = BigDecimal.valueOf(availableDuration.getSeconds());

            // 计算理论产量 = 班次定额  / 班次时长  * 可用时长
            BigDecimal plannedQty = BigDecimal.valueOf(shiftQuota).multiply(availableSeconds).divide(BigDecimal.valueOf(cxShiftConfig.getShiftDuration() * 3600L), 0, RoundingMode.DOWN);
            // 计算理论产量耗时
            BigDecimal actualQtyTime;
            if (plannedQty.compareTo(BigDecimal.valueOf(remainingQty)) >= 0) {
                plannedQty = BigDecimal.valueOf(remainingQty);
                actualQtyTime = BigDecimal.valueOf(remainingQty).multiply(BigDecimal.valueOf(cxShiftConfig.getShiftDuration() * 3600L).divide(BigDecimal.valueOf(shiftQuota), 8, RoundingMode.UP));
            } else {
                actualQtyTime = availableSeconds;
            }
            logDebug("班次[{}]产能计算 | 可用时长:{}s | 理论产量:{} | 实际分配:{} | 生产耗时：{}s",
                    shift, availableSeconds, shiftQuota, plannedQty, actualQtyTime);

            //更新动态计算栏位
            task.setFieldValueByFieldName("class" + shift + "StartTime", startTime);
            startTime = startTime.plusSeconds(actualQtyTime.longValue());
            task.setFieldValueByFieldName("class" + shift + "EndTime", startTime);
            task.setEndTime(startTime);
            remainingQty = updateProductionData(task, machine, startTime, shift, plannedQty.intValueExact(), remainingQty, actualQtyTime, shiftQuota,changeTime);


            //更新成型胎胚月度剩余量
            CxEmbryoMonthPlanSurplus cxEmbryoMonthPlanSurplus = CX_STOCK_MONTH_REMAIN_CONTEXT_MAP.get(task.getLhScheduleResult().getEmbryoCode() + task.getLhScheduleResult().getBomVersion());
            if (cxEmbryoMonthPlanSurplus != null) {
                cxEmbryoMonthPlanSurplus.setMonthRemainQty(cxEmbryoMonthPlanSurplus.getMonthRemainQty() - plannedQty.intValueExact());
                logDebug("胎胚月度剩余量更新 | 胎胚[{}]月度剩余量 → {}", task.getLhScheduleResult().getEmbryoCode(), cxEmbryoMonthPlanSurplus.getMonthRemainQty());
                CX_STOCK_MONTH_REMAIN_CONTEXT_MAP.put(task.getLhScheduleResult().getEmbryoCode() + task.getLhScheduleResult().getBomVersion(), cxEmbryoMonthPlanSurplus);
            }
        }

//        BigDecimal remainTime = machine.getRemainTime();
//        remainTime = remainTime
//                .subtract(BigDecimal.valueOf(changeTime));
//        machine.setRemainTime(remainTime);
//        logDebug("机台剩余时间依据换工装{}更新 → {} 小时", changeTime, remainTime);

        //占用机台后更新标记
        updateFinalStatus(task, machine);
        return true;
    }



    private void updateFinalStatus(LhAlgorithmScheduleResultDto task, CxMachineInfoVo machine) {
        // --- 最终状态更新 ---
        // 更新机台施工信息
        machine.setCxProductConstructionInfo(task.getCxProductConstructionInfoDto());
        logDebug("机台[{}]绑定最后一个规格的施工信息: {}", machine.getCxMachineCode(), task.getCxProductConstructionInfoDto());

        // 更新机台任务计数
        machine.setTaskNum(machine.getTaskNum() + 1);
        logInfo("机台[{}]累计任务数 → {}", machine.getCxMachineCode(), machine.getTaskNum());

        // 更新机台最后占用任务是否有交期
        machine.setLhAlgorithmScheduleResultDto(task);

        // 更新机台最后占用任务是否有交期
        machine.setIsDelivery(task.getLhScheduleResult().getIsDelivery());

        // 标记任务排产完成
        task.setIsScheduleEnd(true);
        task.getLastOccupiedMachines().add(machine.getCxMachineCode());
        task.setFinalMachine(machine.getCxMachineCode());
        logInfo("任务[{}]排产完成，占用机台: {}", task.getLhScheduleResult().getMergeIds(), machine.getCxMachineCode());
    }

    private int updateProductionData(LhAlgorithmScheduleResultDto task, CxMachineInfoVo machine, LocalDateTime startTime, int shift, int actualQty, int remainingQty, BigDecimal actualQtyTime, Integer shiftQuota, double changeTime) {
        // 更新任务班次计划量字段（例如：class1PlanQty）
        String planField = "class" + shift + "PlanQty";
        task.setFieldValueByFieldName(planField, actualQty);

        // 更新机台班次及顺序号
        if (machine.getCurrentShift() != shift) {
            logDebug("机台[{}]当前班次：[{}],切换至新班次：[{}]，重置顺序号", machine.getCxMachineCode(), machine.getCurrentShift(), shift);
            machine.setCurrentShift(shift);
            machine.setCurrentShiftSort(1);
        } else {
            machine.setCurrentShiftSort(machine.getCurrentShiftSort() + 1);
            logDebug("机台[{}]班次[{}]顺序号+1 → {}",
                    machine.getCxMachineCode(), shift, machine.getCurrentShiftSort());
        }

        // 更新机台可用开始时间（累加已排产时间）
        machine.setAvailableBeginTime(startTime);
        logDebug("更新机台可用开始时间 → {}", machine.getAvailableBeginTime().format(formatter));

        // 更新任务班次顺序（例如：class1ShiftSort）
        String sortField = "class" + shift + "Sort";
        task.setFieldValueByFieldName(sortField, machine.getCurrentShiftSort());

        // 更新机台当日剩余时间
//        actualQtyTime = BigDecimal.valueOf(actualQty).multiply(BigDecimal.valueOf(cxShiftConfig.getShiftDuration()).divide(BigDecimal.valueOf(shiftQuota), 8, RoundingMode.UP));
//        BigDecimal remainTime = machine.getRemainTime()
//                .subtract(actualQtyTime);
//        machine.setRemainTime(remainTime);
//        logDebug("机台剩余时间更新 → {} 小时", remainTime);
        //获取今天结束时间
        LocalDateTime todayEndTime = cxShiftConfig.parseToDayLastShiftEndTime();
        // 计算本班次可用时长 = 班次结束时间 - 任务开始时间
        Duration availableDuration = Duration.between(startTime, todayEndTime);
        BigDecimal availableSeconds = BigDecimal.valueOf(availableDuration.getSeconds());
        machine.setRemainTime(availableSeconds.divide(BigDecimal.valueOf(3600),8, RoundingMode.UP));
        logDebug("机台剩余时间更新 → {} 小时", machine.getRemainTime());

        remainingQty -= actualQty;
        logDebug("剩余量更新 → {} ", remainingQty);

        return remainingQty;
    }


    /**
     * 获取班次定额配置
     */
    public Integer getShiftQuota(CxMachineInfoVo machine,
                                  int shift,
                                  CxProductConstructionInfoDto productInfo, LhAlgorithmScheduleResultDto task) {
        Integer quota = 0;
        for (MdmMoldingMachineClsB cls : machine.getMoldingMachineClassList()) {
            if (cls.getProSize().compareTo(BigDecimal.valueOf(productInfo.getDimension())) != 0) {
                log.warn("寸口不匹配跳过 | 机台尺寸:{} | 胎胚尺寸:{}",
                        cls.getProSize(), productInfo.getDimension());
                continue;
            }

            quota = cls.getProductionQuotaQty();
            logDebug("机台使用班次默认定额 | 字段:{} | 值:{}", "productionQuotaQty", quota);
            task.setClassMachineDefaultQty(cls.getProductionQuotaQty());
            String shiftQuotaField = "class" + shift + "MachineQty";
            Object shiftQuota = cls.getFieldValueByFieldName(shiftQuotaField);
            if (shiftQuota != null) {
                quota = (Integer) shiftQuota;

                // 更新任务班次定额 (例如：class1MachineQty)
                String quotaField = "class" + shift + "MachineQty";
                task.setFieldValueByFieldName(quotaField, shiftQuota);

                logDebug("机台使用班次特定定额 | 字段:{} | 值:{}", shiftQuotaField, quota);
            }
            break;
        }
        return quota;
    }



    /**
     * 任务拆分
     *
     * @param item           排程任务
     * @param remainCapacity 拆分的量
     */
    @Override
    public void splitTask(LhAlgorithmScheduleResultDto item, Integer remainCapacity) {
        if (item == null) {
            logError("任务拆分失败：传入任务项为空");
            throw new IllegalArgumentException("任务项不能为空");
        }
        if (remainCapacity == null || remainCapacity <= 0) {
            logError("任务[{}]拆分失败：无效的拆分数量{}", item.getLhScheduleResult(), remainCapacity);
            throw new IllegalArgumentException("拆分数量必须大于0");
        }
        if (item.getTaskPlanQuantity() <= remainCapacity) {
            logWarn("任务[{}]拆分数量{}超过剩余量{}，将进行完全分配",
                    item.getLhScheduleResult(), remainCapacity, item.getTaskPlanQuantity());
            remainCapacity = item.getTaskPlanQuantity();
        }

        logInfo("开始拆分任务[{}]，当前数量：{}，拆分量：{}",
                item.getLhScheduleResult(), item.getTaskPlanQuantity(), remainCapacity);

        item.setTireCount(item.getTireCount() + 1);
        int newQuantity = item.getTaskPlanQuantity() - remainCapacity;
        item.setTaskPlanQuantity(newQuantity);
        logDebug("原始任务[{}]更新后数量：{}", item.getLhScheduleResult(), newQuantity);

        LhAlgorithmScheduleResultDto newTask = new LhAlgorithmScheduleResultDto();
        try {
            // 使用安全属性拷贝
            BeanUtils.copyProperties(item, newTask);
        } catch (Exception e) {
            logError("任务[{}]属性拷贝失败：{}", item.getLhScheduleResult(), e.getMessage());
            throw new RuntimeException("任务拆分时发生属性拷贝错误", e);
        }

        //重置标记
        newTask.setIsContinueTire(false);
        newTask.setIsLargeTire(false);
        newTask.setIsSmallTire(true);
        newTask.setTaskPlanQuantity(remainCapacity);

        newTask.setLhScheduleResult(item.getLhScheduleResult());
        newTask.setCxProductConstructionInfoDto(item.getCxProductConstructionInfoDto());
        newTask.setCxEmbryoMonthPlanSurplus(item.getCxEmbryoMonthPlanSurplus());

        // 安全设置列表（防止修改原始列表）
        if (item.getMdmMaterialInfoList() != null) {
            newTask.setMdmMaterialInfoList(new ArrayList<>(item.getMdmMaterialInfoList()));
        }

        try {
            LH_MISS_TIRE_TIME_CONTEXT_MAP.add(newTask);
            logInfo("成功拆分任务[{}]，生成新任务[{}]，拆分数量：{}",
                    item.getLhScheduleResult(), newTask.getLhScheduleResult(), remainCapacity);
        } catch (Exception e) {
            logError("任务[{}]拆分记录失败：{}", item.getLhScheduleResult(), e.getMessage());
            throw new RuntimeException("任务拆分记录失败", e);
        }

        if (item.getTaskPlanQuantity() < 0) {
            logError("任务[{}]拆分后出现负数量：{}", item.getLhScheduleResult(), item.getTaskPlanQuantity());
            throw new IllegalStateException("任务拆分后数量异常");
        }
    }


    /**
     * 计算成型T日计划量
     * 实现逻辑：
     * 1. 获取当前库存和当日硫化需求
     * 2. 计算备库时间
     * 3. 计算T+1日预测量
     * 4. 综合计算T日计划量
     * 5. 应用多重约束条件：
     * - 不超过次日需求
     * - 不超过月度剩余量
     *
     * @param item        成型任务
     * @param specialPlan 指定计划量
     */
    @Override
    public void calculateTheExpectedTireQuantity(LhAlgorithmScheduleResultDto item, int specialPlan) {
        LhScheduleResult lhResult = item.getLhScheduleResult();
        String taskIdentifier = lhResult.getMergeIds();
        String embryoCode = lhResult.getEmbryoCode();
        BigDecimal lhTime = lhResult.getLhTime();
        logInfo("[任务:{}][胚胎:{}]计算计划量", taskIdentifier, embryoCode);

        // 校验关键业务参数有效性
        if (lhResult.getMoldQty() <= 0) {
            String errorMsg = String.format("模数配置错误需>0，当前值：%d", lhResult.getMoldQty());
            logError(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        if (lhTime == null){
            throw new RuntimeException(String.format("硫化排程发现生胎代码：[%s]，规格代码：[%s]，硫化时间：[%s}，数据错误，请检查硫化排程计划！",  lhResult.getEmbryoCode(), lhResult.getSpecCode(), lhResult.getLhTime()));
        }

        if (lhTime.compareTo(BigDecimal.ZERO) <= 0) {
            String errorMsg = String.format("发现生胎代码：[%s]，当前硫化计划中硫化时间值：%.2f秒，硫化时间需 > 0",  lhResult.getEmbryoCode() , lhResult.getLhTime());
            logError(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // 库存数据获取
        int todayStock = CX_STOCK_NUM_CONTEXT_MAP.getOrDefault(embryoCode, new CxStock()).getStockNum();
        logDebug("当前库存[胚胎:{}]：{}条", embryoCode, todayStock);

        // 当日硫化需求汇总
        List<String> todayDemandFields = cxShiftConfig.getTodayClasses("class", "PlanQty");
        int todayLhDemand = sumFieldValues(lhResult, todayDemandFields);
        logDebug("当日硫化需求[胚胎:{}]：{}条", embryoCode, todayLhDemand);

        // 初步计算成型T日计划 =  T日硫化需求量  -  T日库存 +  T+1成型预做的量
        int initialPlan = todayLhDemand - todayStock;
        logInfo("初始计划量[需求:{}][库存:{}] => {}条",
                todayLhDemand, todayStock, initialPlan);

        // 重排特殊计划量覆盖
        if (specialPlan > 0) {
            logWarn("计划量人工干预[原始值:{}][新值:{}]", initialPlan, specialPlan);
            initialPlan = specialPlan;
        }

        int adjustedPlan = initialPlan;

        // 获取该规格硫化的第二天计划量
        LhScheduleResult lhScheduleResult = item.getLhScheduleResult();
        double nextPlan = (lhScheduleResult.getClass4PlanQty() == null ? 0 : lhScheduleResult.getClass4PlanQty())
                + (lhScheduleResult.getClass5PlanQty() == null ? 0 : lhScheduleResult.getClass5PlanQty())
                + (lhScheduleResult.getClass6PlanQty() == null ? 0 : lhScheduleResult.getClass6PlanQty());

        if (nextPlan > 0) {
            // 备胎量大前提是不能超硫化第二天一个班硫化的产能:准确理解为不超第二天备库量的一半
            nextPlan = (int) Math.round(nextPlan  *  commonCacheService.getEmbryoStockRatio(CX_PARAMS_CONTEXT_MAP));
        }

        // 备库量依据模数进行更准确调整：模数越多备胎量越少
        if (nextPlan > 0 && lhScheduleResult.getMoldQty() != null && lhScheduleResult.getMoldQty() > 2) {
//                        多个灶 , 备胎量 * (1- 模数*0.1)
//                        单个灶 , 备胎量 * (1)
            //计算使用灶数
            double useLhMachineQty = (double) lhScheduleResult.getMoldQty() / 2;
            nextPlan = (useLhMachineQty * nextPlan) * (0.6 - (useLhMachineQty * 0.1));
        }

        adjustedPlan = (int) (adjustedPlan + nextPlan);

        // 约束2：月度剩余量
        String monthlyKey = embryoCode + lhResult.getBomVersion();
        CxEmbryoMonthPlanSurplus monthlySurplus = CX_STOCK_MONTH_REMAIN_CONTEXT_MAP.get(monthlyKey);
        if (monthlySurplus != null) {
            final int monthlyRemain = monthlySurplus.getMonthRemainQty();
            if (adjustedPlan > monthlyRemain) {
                logWarn("超出月度剩余[当前:{}][剩余:{}]，计划量修正", adjustedPlan, monthlyRemain);
                adjustedPlan = monthlyRemain;
            }

            int onceCloseOutQtyParamValue = commonCacheService.getOncePlanMonthRemain(CX_PARAMS_CONTEXT_MAP);
            if (monthlyRemain > 0 && monthlyRemain <= onceCloseOutQtyParamValue) {
                logWarn("满足一次性收尾[月度剩余:{}][一次收尾:{}]，计划量修正", monthlyRemain, onceCloseOutQtyParamValue);
                item.setEndTireDesc("剩余"+monthlyRemain+"收尾，一次性收尾");
                adjustedPlan = monthlyRemain;
            }
        }

        // 约束3：非负校验
        if (adjustedPlan < 0) {
            logWarn("计划量负值[修正前:{}]，自动归零", adjustedPlan);
            adjustedPlan = 0;
        }

        // 最终结果设置
        item.setTaskPlanQuantity(adjustedPlan);
        logInfo("最终计划量[任务:{}]：{}条，[库存]：{}条",
                taskIdentifier, adjustedPlan, todayStock);
    }

    /**
     * 聚合指定字段值（防御性实现）
     * @param result 数据实体
     * @param fields 字段标识列表
     * @return 聚合后的数值总和
     */
    private int sumFieldValues(LhScheduleResult result, List<String> fields) {
        int sum = 0;
        for (String field : fields) {
            Object value = result.getFieldValueByFieldName(field);
            if (value instanceof Number) {
                sum += ((Number) value).intValue();
            } else {
                logWarn("字段[{}]值类型非法，预期为数值类型，实际类型：{}", field, value != null ? value.getClass() : "null");
            }
        }
        return sum;
    }

    /**
     * 计算机台剩余产能（优化版）
     * 实现逻辑：
     * 1. 遍历所有候选机台
     * 2. 查找匹配当前任务规格的机台配置
     * 3. 根据公式计算剩余产能：
     * 剩余产能 = (班次定额 × 总班次时长) - [班次定额/总班次时长 × (剩余时间 - 换装时间)]
     *
     * @param item                当前排程任务
     * @param cxMachineInfoVoList 候选机台列表
     */
    @Override
    public void calculateTheRemainingCapacity(LhAlgorithmScheduleResultDto item, List<CxMachineInfoVo> cxMachineInfoVoList) {
        // 参数校验日志
        if (item == null || item.getCxProductConstructionInfoDto() == null) {
            logWarn("任务或施工信息为空，无法计算剩余产能");
            return;
        }
        if (cxMachineInfoVoList == null || cxMachineInfoVoList.isEmpty()) {
            logDebug("候选机台列表为空，跳过剩余产能计算");
            return;
        }

        // 1.任务对应施工取寸口
        CxProductConstructionInfoDto taskSpec = item.getCxProductConstructionInfoDto();
        BigDecimal taskDimension = BigDecimal.valueOf(taskSpec.getDimension());
        logInfo("开始预计任务[{}]的机台剩余产能，规格寸口：{}寸", item.getLhScheduleResult().getMergeIds(), taskDimension);

        // 2.遍历候选机台
        for (CxMachineInfoVo machine : cxMachineInfoVoList) {
            logDebug("计算机台[{}]的剩余产能", machine.getCxMachineCode());

            // 获取机台寸口配置子表
            List<MdmMoldingMachineClsB> configList = machine.getMoldingMachineClassList();
            if (configList == null || configList.isEmpty()) {
                throw new IllegalStateException(
                        String.format("机台[%s]无配置定额信息，无法计算！", machine.getCxMachineCode())
                );
            }

            boolean foundMatch = false;
            // 遍历机台配置寻找匹配规格
            for (MdmMoldingMachineClsB config : configList) {
                // 跳过空配置项
                if (config == null || config.getProSize() == null) {
                    log.trace("机台[{}]存在空成型机类型子表，跳过", machine.getCxMachineCode());
                    continue;
                }

                // 规格匹配检查
                if (BigDecimalUtils.safeCompare(config.getProSize(), taskDimension) != 0) {
                    log.trace("机台[{}]配置寸口不匹配（当前：{}mm/需要：{}mm）",
                            machine.getCxMachineCode(), config.getProSize(), taskDimension);
                    continue;
                }

                foundMatch = true;
                logDebug("找到机台[{}]的匹配寸口，开始计算剩余产能", machine.getCxMachineCode());

                // 单班定额
                int quotaPerShift = config.getProductionQuotaQty();
                // 总班次数
                int totalShifts = cxShiftConfig.getShiftCount();
                // 单班时长(小时)
                int hoursPerShift = cxShiftConfig.getShiftDuration();

                // 计算总产能
                int totalCapacityBase = quotaPerShift * totalShifts;

                // 机台剩余时间(小时)
                BigDecimal remainingTime = machine.getRemainTime();
                // 换装时间(小时)
                double changeoverTime = changeSpecTime(taskSpec, machine.getCxProductConstructionInfo(), Integer.valueOf(machine.getRollOverType()));
                // 计算有效生产时间（剩余时间 - 换装时间）* 60
                BigDecimal effectiveTime = (remainingTime.subtract(BigDecimal.valueOf(changeoverTime)).multiply(BigDecimal.valueOf(60)));
                logDebug("机台[{}]剩余有效生产时间计算：机台剩余时间={}小时，换装时间={}小时，最终有效生产时间={}分钟", machine.getCxMachineCode(), remainingTime, changeoverTime,effectiveTime);
                if (effectiveTime.compareTo(BigDecimal.ZERO) < 0) {
                    logWarn("机台[{}]有效生产时间为负数（剩余：{}分钟，换装需：{}分钟）",
                            machine.getCxMachineCode(), remainingTime, changeoverTime);
                    effectiveTime = BigDecimal.ZERO;
                }

                // 计算时间系数（总班次时长转为分钟）
                double totalShiftMinutes = hoursPerShift * 60;
                // 计算实际剩余产能: 班次总产能 /  班次总时长 * 有效生产时间
                BigDecimal remainCapacity = BigDecimal.valueOf(quotaPerShift).multiply(effectiveTime).divide(BigDecimal.valueOf(totalShiftMinutes), 0, RoundingMode.DOWN);

                // 预计完剩余产能并记录日志
                machine.setRemainCapacity(remainCapacity.intValue());
                logDebug("机台[{}]预计产能计算完成：机台定额={}，班制={}，基准产能={}, 有效时间={}分钟, 最终产能={}",
                        machine.getCxMachineCode(), quotaPerShift, totalShifts, totalCapacityBase, effectiveTime, remainCapacity);
                break;
            }

            if (!foundMatch) {
                throw new IllegalStateException(
                        String.format("机台[%s]没有匹配%smm规格的成型机类型子表，无法继续排程！",
                                machine.getCxMachineCode(), taskDimension)
                );
            }
        }
    }


    /**
     * 计算规格切换所需换工装时间（单位：小时）（自动排程只会排小换工装，大换工装在手工插单或导入需要考虑）
     * 业务规则：
     * 1. 当新旧规格的机头宽度和扣圈盘直径完全相同时，无需换装
     * 2. 仅机头宽度相同时也无需换装
     * 3. 其他情况使用配置的最小换装时间
     *
     * @param currentSpec  当前施工规格（切换后规格）
     * @param previousSpec 机台原有规格（切换前规格）
     * @return 换装耗时（小时），精度保留两位小数
     * @throws IllegalArgumentException 参数不合法时抛出
     */
    @Override
    public double changeSpecTime(CxProductConstructionInfoDto currentSpec,
                                 CxProductConstructionInfoDto previousSpec, Integer mouldMethod) {
        if (previousSpec == null || currentSpec == null) {
            logDebug("机台没有前规格,不需要考虑换工装！");
            return 0;
        }

        // 参数校验日志
        logDebug("开始计算换装时间，当前规格：{}，原有规格：{}",
                currentSpec.getEmbryoCode(), previousSpec.getEmbryoCode());

        //判断机台成型法
        if (mouldMethod == 0) {
            throw new IllegalArgumentException("机台没有成型法，无法计算换工装时间！");
        }

        try {
            // 配置时间（分钟）转小时
            double minutes = commonCacheService.getChangeSpecTime(CX_PARAMS_CONTEXT_MAP);

            // 一次法换工装耗时是1小时
            if (mouldMethod == 1) {
                    minutes = commonCacheService.getBigChangeSpecTime(CX_PARAMS_CONTEXT_MAP);
            }

            double hours = BigDecimal.valueOf(minutes / CxEngineConstants.ONE_MINUTE_SECOND)
                    .setScale(CxEngineConstants.TWO_SCALE, RoundingMode.HALF_UP)
                    .doubleValue();


            logInfo("规格[{} -> {}]需要换装，计算耗时：{}小时（配置值：{}分钟）",
                    previousSpec.getEmbryoCode(), currentSpec.getEmbryoCode(),
                    hours, minutes);
            return hours;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * 获取任务可用机台列表（优化版）
     *
     * @param task 调度任务
     * @return 可用机台列表，当无可用机台时返回空列表
     */
    @Override
    public List<CxMachineInfoVo> getAvailableMachines(LhAlgorithmScheduleResultDto task) {

        // 参数校验日志
        if (task == null || task.getCxProductConstructionInfoDto() == null || task.getLhScheduleResult() == null) {
            logError("任务参数不完整，无法获取可用机台");
            if (task != null) {
                task.setStopScheduleReason(task.getStopScheduleReason() == null ?  "缺少施工信息，请同步施工重试！" : (task.getStopScheduleReason() + "无可用机台，标记为终止排产"));
            }
            return Collections.emptyList();
        }

        // 记录任务基本信息
        logInfo("开始处理任务ID:[{}]的机台筛选，任务规格:{}mm",
                task.getLhScheduleResult().getMergeIds(),
                task.getCxProductConstructionInfoDto().getDimension());

        //=============== 准备任务参数 ===============//
        CxProductConstructionInfo productInfo = task.getCxProductConstructionInfoDto();
        LhScheduleResult scheduleResult = task.getLhScheduleResult();


        // 基础参数
        Set<String> limitMachines = task.getLimitMachines();
        Set<String> forbidMachines = task.getForbidMachines();
        String requiredMouldMethod = String.valueOf(scheduleResult.getMouldMethod());
        String embryoCode = scheduleResult.getEmbryoCode();
        Double requiredDimension = productInfo.getDimension();
        Double requiredFlipDisc = productInfo.getFlipDiscDiameter();
        Set<String> historyMachines = task.getLastOccupiedMachines();
        Double sectionWidth = productInfo.getSectionWidth();

        // 解析扁平比
        String specDesc = productInfo.getSpecDesc();
        // 扁平比
        Double flatRatio = parseFlatRatioFromSpec(specDesc);


        List<CxMachineInfoVo> availableMachines = new ArrayList<>();

        //=============== 机台筛选流程 ===============//
        for (CxMachineInfoVo machine : CX_MACHINE_ACCTIVE_TIME_CONTEXT_MAP.values()) {
            String machineCode = machine.getCxMachineCode();
            logDebug("正在检查机台: {}", machineCode);

            // 1. 机台限制检查
            if (!limitMachines.isEmpty() && !limitMachines.contains(machineCode)) {
                logDebug("机台[{}]未在限制列表中，跳过", machineCode);
                continue;
            }
            if (!forbidMachines.isEmpty() && forbidMachines.contains(machineCode)) {
                logDebug("机台[{}]在禁用列表中，跳过", machineCode);
                continue;
            }

            // 2. 历史机台检查
            boolean isHistory = historyMachines.contains(machine.getCxMachineCode());
            machine.setIsHistoryMachine(isHistory);
            if (isHistory) {
                // 历史机台就是中心机构满足,不用再判断【寸口】/【扁平比】
                logDebug("规格的历史机台[{}]", JSON.toJSONString(historyMachines));
                logDebug("标记机台[{}]为历史使用机台", machine.getCxMachineCode());
            } else {
                // 成型法检查
                MdmMoldingMachineCls machineCls = machine.getMoldingMachineCls();
                if (machineCls == null) {
                    String errorMsg = StringUtils.format(I18nUtil.getMessage("机台[{}]成型机类型没有维护！"),
                            machine.getCxMachineCode());
                    throw new IllegalArgumentException(errorMsg);
                } else if (machineCls.getMouldMethod() == null || requiredMouldMethod == null) {
                    String errorMsg = StringUtils.format(I18nUtil.getMessage("机台[{}]成型法[{}]、胎胚[{}]对应施工维护成型法[{}]，缺失请补充！"),
                            machine.getCxMachineCode(),
                            machineCls.getMouldMethod(),
                            embryoCode,
                            requiredMouldMethod
                    );
                    throw new IllegalArgumentException(errorMsg);
                } else if (!String.valueOf(machineCls.getMouldMethod()).equals(requiredMouldMethod)) {
                    logDebug("机台[{}]成型法不匹配(机台实际成型法:{}/任务要求成型法:{})",
                            machineCode,
                            machineCls.getMouldMethod(),
                            requiredMouldMethod);
                    continue;
                }
//
//                // 寸口检查
//                if (machine.getMinSize().compareTo(BigDecimal.valueOf(requiredDimension)) > 0) {
//                    logDebug("机台[{}]最小尺寸限制(机台:{}寸/任务:{}寸)",
//                            machineCode, machine.getMinSize(), requiredDimension);
//                    continue;
//                }
//                if (machine.getMaxSize().compareTo(BigDecimal.valueOf(requiredDimension)) < 0) {
//                    logDebug("机台[{}]最大尺寸限制(机台:{}寸/任务:{}寸)",
//                            machineCode, machine.getMaxSize(), requiredDimension);
//                    continue;
//                }
//
//                // 扁平比检查
//                if (flatRatio != null && StringUtils.isNotEmpty(machine.getMoldingDrumMax()) && Integer.parseInt(machine.getMoldingDrumMax()) > flatRatio) {
//                    logDebug("机台[{}]最大扁平比限制(机台:{}/任务:{})",
//                            machineCode, machine.getMoldingDrumMax(), flatRatio);
//                    continue;
//                }
//                if (flatRatio != null && StringUtils.isNotEmpty(machine.getMoldingDrumMin()) && Integer.parseInt(machine.getMoldingDrumMin()) > flatRatio) {
//                    logDebug("机台[{}]最小扁平比限制(机台:{}/任务:{})",
//                            machineCode, machine.getMoldingDrumMin(), flatRatio);
//                    continue;
//                }
//
//
//                // 断面宽检查
//                if (sectionWidth != null && machine.getSectionWidthMax() != null && machine.getSectionWidthMax() > sectionWidth) {
//                    logDebug("机台[{}]最大断面宽限制(机台:{}/任务:{})",
//                            machineCode, machine.getSectionWidthMax(), sectionWidth);
//                    continue;
//                }
//                if (sectionWidth != null && machine.getSectionWidthMin() != null && machine.getSectionWidthMin() > sectionWidth) {
//                    logDebug("机台[{}]最小断面宽限制(机台:{}/任务:{})",
//                            machineCode, machine.getSectionWidthMin(), sectionWidth);
//                    continue;
//                }


                // 单机班产检查
                boolean foundMatch = false;
                List<MdmMoldingMachineClsB> configList = machine.getMoldingMachineClassList();

                if (configList == null || configList.isEmpty()) {
                    continue;
                }

                for (MdmMoldingMachineClsB config : configList) {

                    if (config == null || config.getProSize() == null) {
                        continue;
                    }

                    // 规格不匹配，跳过当前配置项
                    if (BigDecimalUtils.safeCompare(
                            config.getProSize(),
                            BigDecimal.valueOf(requiredDimension)) != 0) {
                        continue;
                    }

                    // 找到匹配规格的配置项
                    foundMatch = true;
                }

                // 如果没有找到任何匹配规格的配置项
                if (!foundMatch) {
                    logDebug("机台[{}]没有匹配{}寸规格的成型机类型子表，跳过",
                            machine.getCxMachineCode(), requiredDimension);
                    continue;  // 跳过当前机台，继续处理下一个机台
                }

                // 胎体布层数检查
                String carSpecList = commonCacheService.getAllowLightCarSpec(CX_PARAMS_CONTEXT_MAP);
                if (!carSpecList.contains(embryoCode)) {
                    int taskClothTypeNum = 0;

                    if (StringUtils.isNotEmpty(productInfo.getTireFabricCode1())) {
                        taskClothTypeNum += 1;
                    }

                    if (StringUtils.isNotEmpty(productInfo.getTireFabricCode2())) {
                        taskClothTypeNum += 1;
                    }

                    if (StringUtils.isNotEmpty(productInfo.getTireFabricCode3())) {
                        taskClothTypeNum += 1;
                    }

                    if (taskClothTypeNum == 3 && !"L505".equals(machine.getCxMachineCode())){
//                        logDebug("机台[{}]的胎体布层数为{}，不能安排在{}层胎体布的机台, 3层只能安排在505机台", machine.getCxMachineCode(), taskClothTypeNum, machine.getCarcassClothType());
                        continue;
                    }

//                    if (taskClothTypeNum != machine.getCarcassClothType()) {
//                        logDebug("机台[{}]的胎体布层数为{}，不能安排在{}层胎体布的机台", machine.getCxMachineCode(), taskClothTypeNum, machine.getCarcassClothType());
//                        continue;
//                    }
                }
            }
            availableMachines.add(machine);
        }

        //=============== 处理筛选结果 ===============//
        if (availableMachines.isEmpty()) {
            return Collections.emptyList();
        }
        logInfo("任务[{}]一次筛选机台，中心机构满足机台保[{}]", task.getLhScheduleResult().getMergeIds(), JSON.toJSONString(availableMachines.stream().map(CxMachineInfoVo::getCxMachineCode).collect(Collectors.toList())));

        //=============== 二次筛选逻辑 ===============//
        List<CxMachineInfoVo> finalCandidates = new ArrayList<>();
        for (CxMachineInfoVo machine : availableMachines) {

            //历史机台满足二次筛选
            if (machine.getIsHistoryMachine()) {
                finalCandidates.add(machine);
                continue;
            }

            CxProductConstructionInfo machineSpec = machine.getCxProductConstructionInfo();

            if (machineSpec != null) {
                // 解析扁平比
                String machineSpecDesc = machineSpec.getSpecDesc();
                // 扁平比
                Double machineSpecFlatRatio = parseFlatRatioFromSpec(machineSpecDesc);

                //寸口确定
                if (!machineSpec.getDimension().equals(requiredDimension)) {
                    logDebug("机台[{}]当前规格尺寸不匹配(实际:{}/要求:{})",
                            machine.getCxMachineCode(),
                            machineSpec.getDimension(),
                            requiredDimension);
                    continue;
                }

                // 检查二次成型机特殊要求
                MdmMoldingMachineCls cls = machine.getMoldingMachineCls();
                if (cls != null && cls.getMouldMethod().equals(Integer.valueOf(CxEngineConstants.MACHINE_TYPE_TWICE))) {
                    Objects.requireNonNull(machineSpec.getFlipDiscDiameter(),
                            "二次成型机[" + machine.getCxMachineCode() + "]的扣圈盘直径未配置");
                    Objects.requireNonNull(requiredFlipDisc, "任务胎胚[" + embryoCode + "]要求的扣圈盘直径未配置！");

                    if (!machineSpec.getFlipDiscDiameter().equals(requiredFlipDisc)) {
                        logDebug("二次成型机[{}]扣圈盘不匹配(实际:{}/任务要求:{})",
                                machine.getCxMachineCode(),
                                machineSpec.getFlipDiscDiameter(),
                                requiredFlipDisc);
                        continue;
                    }
                }

                // 扁平比只能是上一个规格的 +- 参数值
                int ratioDiff = commonCacheService.getFlatRatioDiff(CX_PARAMS_CONTEXT_MAP);
                if (machineSpecFlatRatio != null && flatRatio != null) {
                    double machineFlatRatio = machineSpecFlatRatio;
                    double requiredFlatRatio = flatRatio;
                    if (Math.abs(machineFlatRatio-requiredFlatRatio) > ratioDiff) {
                        logDebug("机台[{}]的扁平比不匹配(实际:{}/要求:{})",
                                machine.getCxMachineCode(),
                                machineFlatRatio,
                                requiredFlatRatio);
                        continue;
                    }
                }

                // 断面宽只能是上一个规格的 +- 参数值
                int sectionWidthDiff = commonCacheService.getSectionWidthDiff(CX_PARAMS_CONTEXT_MAP);
                double machineSpecSectionWidth  = machineSpec.getSectionWidth();
                double taskSpecSectionWidth = productInfo.getSectionWidth();
                if (Math.abs(machineSpecSectionWidth-taskSpecSectionWidth) > sectionWidthDiff) {
                    logDebug("机台[{}]的断面宽不匹配(实际:{}/要求:{})",
                            machine.getCxMachineCode(),
                            machineSpecSectionWidth,
                            taskSpecSectionWidth);
                    continue;
                }
            }

            finalCandidates.add(machine);
        }

        logInfo("任务[{}]最终可选机台数:{}", task.getLhScheduleResult().getMergeIds(), finalCandidates.size());
        return finalCandidates;
    }


    /**
     * 从规格描述中解析扁平比（兼容公制、简化公制、英寸制及各种变体）
     */
    private Double parseFlatRatioFromSpec(String specDesc) {
        if (StringUtils.isEmpty(specDesc)) {
            return null;
        }

        // 统一替换特殊字符（如*和×都视为乘号）
        String normalizedSpec = specDesc.replace('*', '×').trim();

        // 情况1：标准公制规格（如225/60R18 或 225/50ZR17）
        Pattern metricPattern = Pattern.compile("(\\d+)/(\\d+)([A-Z]?)R.*");
        Matcher metricMatcher = metricPattern.matcher(normalizedSpec);
        if (metricMatcher.find()) {
            // 直接返回扁平比数字
            return Double.valueOf(metricMatcher.group(2));
        }

        // 情况2：简化公制规格（如185R14LT 或 175R14LT 8PR）
        Pattern simplifiedPattern = Pattern.compile("(\\d+)R\\d+([A-Z]*)");
        if (simplifiedPattern.matcher(normalizedSpec.split(" ")[0]).find()) {
            // 简化公制默认80%扁平比
            return 80.0;
        }

        // 情况3：英寸制规格（如31×10.50R15 或 31*10.50R15LT）
        Pattern inchPattern = Pattern.compile("(\\d+\\.?\\d+)[×x](\\d+\\.?\\d+)R.*");
        Matcher inchMatcher = inchPattern.matcher(normalizedSpec);
        if (inchMatcher.find()) {
            try {
                double outerDiameter = Double.parseDouble(inchMatcher.group(1));
                double width = Double.parseDouble(inchMatcher.group(2));
                // 提取轮毂直径（如R15后的15）
                String rimPart = normalizedSpec.split("R")[1];
                int rimDiameter = Integer.parseInt(rimPart.replaceAll("\\D", ""));

                double sidewallHeight = (outerDiameter - rimDiameter) / 2.0;
                double calculatedRatio = (sidewallHeight / width) * 100;
                return (double) Math.round(calculatedRatio);
            } catch (Exception e) {
                log.warn("英寸制规格解析失败: {}", specDesc, e);
            }
        }

        // 情况4：特殊规格（如带ZR的225/50ZR17）
        Pattern zrPattern = Pattern.compile("(\\d+)/(\\d+)ZR.*");
        if (zrPattern.matcher(normalizedSpec).find()) {
            return Double.valueOf(normalizedSpec.split("/")[1].replaceAll("ZR.*", ""));
        }

        // 无法识别的格式返回null（或可改为抛出异常）
        log.warn("无法识别的轮胎规格格式: {}", specDesc);
        return 0.0;
    }

    /**
     * 计算施工相似度（优化版）
     * 实现逻辑：
     * 1. 遍历所有机台和每个相似度字段
     * 2. 对每个字段进行有效性检查：
     * - 双方都为空 → 不比较
     * - 一方为空一方非空 → 不比较
     * - 双方都非空 → 比较字段值
     * 3. 相同字段值增加相似度计数
     *
     * @param task     排程任务
     * @param machines 可选机台列表
     */
    @Override
    public void calculateSimilarity(LhAlgorithmScheduleResultDto task, List<CxMachineInfoVo> machines) {
        // 参数校验日志
        if (task == null || task.getCxProductConstructionInfoDto() == null) {
            logWarn("任务或施工信息为空，无法计算相似度");
            return;
        }
        if (machines == null || machines.isEmpty()) {
            logDebug("无可选机台，跳过相似度计算");
            return;
        }

        // 获取任务施工信息和比较字段
        CxProductConstructionInfo taskSpec = task.getCxProductConstructionInfoDto();
        String[] compareFields = PCRSimilarityFields;
        logInfo("开始计算任务[{}]施工相似度，比较字段数：{}", task.getLhScheduleResult().getMergeIds(), compareFields.length);

        for (CxMachineInfoVo machine : machines) {
            // 初始化相似度
            machine.setSimilarity(0);
            CxProductConstructionInfo machineSpec = machine.getCxProductConstructionInfo();

            // 处理无机台施工信息的情况
            if (machineSpec == null) {
                logDebug("机台[{}]无施工信息，相似度保持0", machine.getCxMachineCode());
                continue;
            }

            logDebug("开始比较机台[{}]施工相似度", machine.getCxMachineCode());

            // 遍历每个比较字段
            for (String field : compareFields) {
                // 获取字段值
                Object taskValue = taskSpec.getFieldValueByFieldName(field);
                Object machineValue = machineSpec.getFieldValueByFieldName(field);

                // 判断是否需要进行比较
                boolean bothEmpty = CxLhEngineUtils.isEmpty(taskValue) && CxLhEngineUtils.isEmpty(machineValue);
                boolean eitherEmpty = CxLhEngineUtils.isEmpty(taskValue) ^ CxLhEngineUtils.isEmpty(machineValue);

                if (bothEmpty) {
                    log.trace("字段[{}]双方都为空，跳过比较", field);
                    continue;
                }
                if (eitherEmpty) {
                    log.trace("字段[{}]一方为空（任务值:{}，机台值:{}），跳过比较",
                            field, taskValue, machineValue);
                    continue;
                }

                // 执行字段值比较
                if (CxLhEngineUtils.isFieldValueEqual(taskValue, machineValue)) {
                    machine.setSimilarity(machine.getSimilarity() + 1);
                    logDebug("字段[{}]匹配成功，当前相似度：{}",
                            field, machine.getSimilarity());
                }
            }

            logInfo("机台[{}]最终相似度：{}", machine.getCxMachineCode(), machine.getSimilarity());
        }
    }


    /**
     * 规格最大备库计算 2025-08-04 备库逻辑有问题 （重构 Nick+）
     */
    private void calculateMaxStockForSpecification() {
        // 1.已安排任务按照机台分组
        Map<String, List<LhAlgorithmScheduleResultDto>> machineTaskMap = LH_MISS_TIRE_TIME_CONTEXT_MAP.stream()
                .filter(LhAlgorithmScheduleResultDto::getIsScheduleEnd)
                .collect(Collectors.groupingBy(LhAlgorithmScheduleResultDto::getFinalMachine));

        // 2.遍历机台
        for (Map.Entry<String, List<LhAlgorithmScheduleResultDto>> entry : machineTaskMap.entrySet()) {
            String machineCode = entry.getKey();
            CxMachineInfoVo machine = CX_MACHINE_ACCTIVE_TIME_CONTEXT_MAP.get(machineCode);
            List<LhAlgorithmScheduleResultDto> tasks = entry.getValue();
            logInfo("开始处理机台[{}]的任务补偿，共{}个任务", machineCode, tasks.size());

            // 从最后一个任务开始备胎
            for (int i = tasks.size() - 1; i >= 0; i--) {
                LhAlgorithmScheduleResultDto currentTask = tasks.get(i);


                // 获取该规格硫化的第二天计划量
                LhScheduleResult lhScheduleResult = currentTask.getLhScheduleResult();
                double nextPlan = (lhScheduleResult.getClass4PlanQty() == null ? 0 : lhScheduleResult.getClass4PlanQty())
                        + (lhScheduleResult.getClass5PlanQty() == null ? 0 : lhScheduleResult.getClass5PlanQty())
                        + (lhScheduleResult.getClass6PlanQty() == null ? 0 : lhScheduleResult.getClass6PlanQty());

                if (nextPlan > 0) {
                    // 备胎量大前提是不能超硫化第二天一个班硫化的产能:准确理解为不超第二天备库量的一半
                    nextPlan = (int) Math.round(nextPlan  *  commonCacheService.getEmbryoStockRatio(CX_PARAMS_CONTEXT_MAP));
                }

                // 备库量依据模数进行更准确调整：模数越多备胎量越少
                if (nextPlan > 0 && lhScheduleResult.getMoldQty() != null && lhScheduleResult.getMoldQty() > 2) {
//                        多个灶 , 备胎量 * (1- 模数*0.1)
//                        单个灶 , 备胎量 * (1)
                    //计算使用灶数
                    double useLhMachineQty = (double) lhScheduleResult.getMoldQty() / 2;
                    nextPlan = (useLhMachineQty * nextPlan) * (0.6 - (useLhMachineQty * 0.1));
                }

                // 备胎量，不能超月度剩余量
                String monthlyKey = currentTask.getLhScheduleResult().getEmbryoCode() + currentTask.getLhScheduleResult().getBomVersion();
                CxEmbryoMonthPlanSurplus monthlySurplus = CX_STOCK_MONTH_REMAIN_CONTEXT_MAP.get(monthlyKey);
                if (monthlySurplus != null) {
                    final int monthlyRemain = monthlySurplus.getMonthRemainQty();
                    if (nextPlan > monthlyRemain) {
                        nextPlan = monthlyRemain;
                    }
                }

                // 保存最大备胎数量
                currentTask.setMaxTireQty(nextPlan);
                logDebug("机台[{}]的最大备胎量：{}", machineCode, nextPlan);

                // 获取规格头部可以延迟生产的量(这个挤出的量也就是上一个规格的延迟生产量)
                long secondsDiff = Duration.between(currentTask.getStartTime(), currentTask.getEndTime().isAfter(currentTask.getPreviousTireTime()) ? currentTask.getPreviousTireTime() : currentTask.getEndTime()).getSeconds();

                // 如果规格不是欠产规格
                if (secondsDiff > 0) {
                    // 机台默认产能
                    int quotaPerShift = currentTask.getClassMachineDefaultQty();

                    // 计算可以让出的计划量
                    long canAddQuota = secondsDiff * quotaPerShift / (cxShiftConfig.getShiftDuration() * 3600L);

                    // 可以压缩的量
                    currentTask.setMaxCompressionQty(canAddQuota);
                }else {
                    currentTask.setMaxCompressionQty(0L);
                }
            }
        }
    }

    /**
     * 任务补偿处理 - 根据机台任务间的欠胎时间差异进行生产量补偿
     */
    private void compensateProductionVolume() {
        // 1.已安排任务按照机台分组
        Map<String, List<LhAlgorithmScheduleResultDto>> machineTaskMap = LH_MISS_TIRE_TIME_CONTEXT_MAP.stream()
                .filter(LhAlgorithmScheduleResultDto::getIsScheduleEnd)
                .collect(Collectors.groupingBy(LhAlgorithmScheduleResultDto::getFinalMachine));

        // 2.遍历机台
        for (Map.Entry<String, List<LhAlgorithmScheduleResultDto>> entry : machineTaskMap.entrySet()) {
            String machineCode = entry.getKey();
            CxMachineInfoVo machine = CX_MACHINE_ACCTIVE_TIME_CONTEXT_MAP.get(machineCode);
            List<LhAlgorithmScheduleResultDto> tasks = entry.getValue();
            logInfo("开始处理机台[{}]的任务补偿，共{}个任务", machineCode, tasks.size());

            if (machine.getCxMachineCode().equals("L102")){
                //todo debug 快速定位
                int a = 1;
            }

            // 按照欠胎时间排序（从小到大）
            tasks.sort(Comparator.comparing(LhAlgorithmScheduleResultDto::getStartTime));

            long finalCanAddNum = 0;
            // 从最后一个任务开始延迟生产
            for (int i =  tasks.size() - 1; i >= 0; i--) {
                LhAlgorithmScheduleResultDto currentTask = tasks.get(i);

                //如果是最后一个规格
                if (i == tasks.size() - 1) {

                    // 获取机台的剩余产能
                    double maxAddNum = machine.getRemainCapacity();

                    // 剩余产能，与备胎量比较
                    if (maxAddNum > currentTask.getMaxTireQty() + currentTask.getMaxCompressionQty()) {
                        maxAddNum = currentTask.getMaxTireQty() + currentTask.getMaxCompressionQty();
                    }

                    // 获取规格头部可以延迟生产的量(这个挤出的量也就是上一个规格的延迟生产量)
                    long secondsDiff = Duration.between(currentTask.getStartTime(), currentTask.getEndTime().isAfter(currentTask.getPreviousTireTime()) ? currentTask.getPreviousTireTime() : currentTask.getEndTime()).getSeconds();

                    // 如果规格不是欠产规格
                    if (secondsDiff > 0) {
                        // 机台默认产能
                        int quotaPerShift = currentTask.getClassMachineDefaultQty();

                        // 计算可以让出的计划量
                        long canAddQuota = secondsDiff * quotaPerShift / (cxShiftConfig.getShiftDuration() * 3600L);

                        // 不能超上一个规格的最大备胎量
                        if (i >= 1) {
                            if (canAddQuota > tasks.get(i - 1).getMaxTireQty() + tasks.get(i - 1).getMaxCompressionQty()) {
                                canAddQuota = tasks.get(i - 1).getMaxTireQty().intValue() + tasks.get(i - 1).getMaxCompressionQty();;
                                secondsDiff = canAddQuota * (cxShiftConfig.getShiftDuration() * 3600L) / quotaPerShift;
                            }
                        }

                        // 压缩的量不能超拉伸的量否则不满足硫化
                        if (canAddQuota  > machine.getRemainCapacity() -  maxAddNum) {
                            finalCanAddNum = (long) (machine.getRemainCapacity() -  maxAddNum);
                            secondsDiff = finalCanAddNum * (cxShiftConfig.getShiftDuration() * 3600L) / quotaPerShift;
                        } else {
                            //2025/07/16如果是试验或者是首做规格不能多生产
                            if (ApsConstant.APS_STRING_1.equals(currentTask.getLhScheduleResult().getIsFirst())){
                                // 首排规格
                                maxAddNum = finalCanAddNum;
                            }else if (ApsConstant.APS_STRING_1.equals(currentTask.getLhScheduleResult().getIsTrial())){
                                // 试产试制规格
                                maxAddNum = finalCanAddNum;
                            }else {
                                finalCanAddNum = canAddQuota;
                            }
                        }

                        // 如果仅有一个规格那么不需要往前缩
                        if (tasks.size() == 1) {
                            currentTask.setStartTime(currentTask.getStartTime());
                        } else {
                            // 重算开始时间
                            currentTask.setStartTime(currentTask.getStartTime().plusSeconds(secondsDiff));
                        }

                        // 重排计划
                        currentTask.setTaskPlanQuantity((int) (currentTask.getTaskPlanQuantity() + maxAddNum));
                        executeShiftScheduling(currentTask, machine, currentTask.getStartTime(), true);
                        currentTask.setMaxTireQty(currentTask.getMaxTireQty() - maxAddNum);
                    } else {
                        finalCanAddNum = 0;
                        currentTask.setTaskPlanQuantity((int) (currentTask.getTaskPlanQuantity() + maxAddNum));
                        executeShiftScheduling(currentTask, machine, currentTask.getStartTime(), true);
                        currentTask.setMaxTireQty(currentTask.getMaxTireQty() - maxAddNum);
                    }
                }else if (i != 0){
                    // 获取前一个规格释放出来的量
                    long maxAddNum = finalCanAddNum;

                    // 计算本规格能压缩的量
                    long secondsDiff = Duration.between(currentTask.getStartTime(), currentTask.getEndTime().isAfter(currentTask.getPreviousTireTime()) ? currentTask.getPreviousTireTime() : currentTask.getEndTime()).getSeconds();

                    // 能挤出量
                    if (secondsDiff > 0) {

                        // 机台默认产能
                        int quotaPerShift = currentTask.getClassMachineDefaultQty();
                        // 计算可以让出的计划量
                        long canAddQuota = secondsDiff * quotaPerShift / (cxShiftConfig.getShiftDuration() * 3600L);

                        // 不能超上一个规格的备胎量
                        if (canAddQuota > tasks.get(i - 1).getMaxTireQty() + tasks.get(i - 1).getMaxCompressionQty()) {
                            canAddQuota = tasks.get(i - 1).getMaxTireQty().intValue() + tasks.get(i - 1).getMaxCompressionQty();
                            secondsDiff = canAddQuota * (cxShiftConfig.getShiftDuration() * 3600L) / quotaPerShift;
                        }

                        // 挤出的量不能超拉伸的量
                        if (canAddQuota < maxAddNum) {
                            finalCanAddNum = canAddQuota;
                        } else {
                            secondsDiff = finalCanAddNum * (cxShiftConfig.getShiftDuration() * 3600L) / quotaPerShift;
                        }

                        if (secondsDiff > 0) {
                            currentTask.setStartTime(currentTask.getStartTime().plusSeconds(secondsDiff));
                            currentTask.setTaskPlanQuantity((int) (currentTask.getTaskPlanQuantity() + maxAddNum - finalCanAddNum));
                            currentTask.setCompensationQty(maxAddNum - finalCanAddNum);
                            currentTask.setMaxTireQty(currentTask.getMaxTireQty() - maxAddNum + finalCanAddNum);
                            executeShiftScheduling(currentTask, machine, currentTask.getStartTime(), true);
                        }
                    } else {
                        finalCanAddNum = 0;
                        currentTask.setTaskPlanQuantity((int) (currentTask.getTaskPlanQuantity() + maxAddNum - finalCanAddNum));
                        currentTask.setCompensationQty(maxAddNum - finalCanAddNum);
                        executeShiftScheduling(currentTask, machine, currentTask.getStartTime(), true);
                    }
                }else {
                    // 获取机台剩余产能
                    long maxAddNum = finalCanAddNum;
                    finalCanAddNum = 0;
                    currentTask.setTaskPlanQuantity((int) (currentTask.getTaskPlanQuantity()+maxAddNum-finalCanAddNum));
                    currentTask.setCompensationQty(maxAddNum-finalCanAddNum);
                    currentTask.setMaxTireQty(currentTask.getMaxTireQty() - maxAddNum);
                    executeShiftScheduling(currentTask,machine,currentTask.getStartTime(),true);
                }
            }

            // 生产顺序重新编排
            ScheduleSortUtil.fillSortByStartTimes(tasks);
        }
    }




    /**
     * 将机台剩余产能分配
     */
    private void allocateRemainingCapacity(List<CxScheduleResultOld> lastDayCxResults) {
        // 1.已安排任务按照机台分组
        Map<String, List<LhAlgorithmScheduleResultDto>> machineTaskMap = LH_MISS_TIRE_TIME_CONTEXT_MAP.stream()
                .filter(LhAlgorithmScheduleResultDto::getIsScheduleEnd)
                .collect(Collectors.groupingBy(LhAlgorithmScheduleResultDto::getFinalMachine));

        // 2.遍历机台
        for (Map.Entry<String, List<LhAlgorithmScheduleResultDto>> entry : machineTaskMap.entrySet()) {

            String machineCode = entry.getKey();
            int remainingQty = 0;
            CxMachineInfoVo machine = CX_MACHINE_ACCTIVE_TIME_CONTEXT_MAP.get(machineCode);
            List<LhAlgorithmScheduleResultDto> tasks = entry.getValue();
            logInfo("开始处理机台[{}]的任务补偿，共{}个任务", machineCode, tasks.size());


            // 按照欠胎时间排序（从小到大）
            tasks.sort(Comparator.comparing(LhAlgorithmScheduleResultDto::getStartTime));

            // 获取最后安排的规格
            LhAlgorithmScheduleResultDto lastTask = tasks.get(tasks.size()-1);
            machine.setLhAlgorithmScheduleResultDto(lastTask);
            machine.setCxProductConstructionInfo(lastTask.getCxProductConstructionInfoDto());

            // 获取规格的结束时间
            LocalDateTime startTime = lastTask.getEndTime();

            //获取今天结束时间
            LocalDateTime todayEndTime = cxShiftConfig.parseToDayLastShiftEndTime();

            // 这里需要注意欠胎时间如果大于任务结束时间，那么不能计算到欠胎时间
            long secondsDiff = Duration.between(startTime,todayEndTime).getSeconds();

            // 判断是否有剩余产能
            if (secondsDiff > 0) {
                // 机台默认产能
                int quotaPerShift = lastTask.getClassMachineDefaultQty();
                // 计算可以让出的计划量
                remainingQty = (int) (secondsDiff * quotaPerShift / (cxShiftConfig.getShiftDuration() * 3600L));
                machine.setRemainCapacity(remainingQty);
            }else {
                continue;
            }

            // 获取最后一个任务的顺序
            int lastTaskOrder = 0;
            if (lastTask.getClass3Sort() != null) {
                // 优先3班顺序
                lastTaskOrder = lastTask.getClass3Sort();
            } else if (lastTask.getClass2Sort() != null) {
                // 其次2班顺序
                lastTaskOrder = lastTask.getClass2Sort();
            } else if (lastTask.getClass1Sort() != null) {
                // 最后1班顺序
                lastTaskOrder = lastTask.getClass1Sort();
            }

            // 机台班次顺序
            machine.setCurrentShiftSort(lastTaskOrder);
            final int totalShifts = cxShiftConfig.getShiftCount();
            final int currentShift = cxShiftConfig.getShiftNumber(startTime);
            // 机台班次数
            machine.setCurrentShift(currentShift);
            // 机台班次开始时间
            machine.setAvailableBeginTime(startTime);
            // 机台剩余产能
            machine.setRemainTime(BigDecimal.valueOf(secondsDiff/3600));

            // 最大换规格次数
            final int maxChangeSpecNum = commonCacheService.getChangeSpecNum(CX_PARAMS_CONTEXT_MAP);

            for (int i = 0; i <= tasks.size()-1;i++) {

                LhAlgorithmScheduleResultDto currencyTask = tasks.get(i);

                // 跳过收尾规格
                if (currencyTask.getIsEndTire()){
                    continue;
                }

                //如果规格本班次已经安排了计划则跳过:防止倒班
                if (currencyTask.getFieldValueByFieldName("class" + currentShift + "PlanQty") !=null && (int)  currencyTask.getFieldValueByFieldName("class" + currentShift + "PlanQty") > 0){
                    continue;
                }


                // 跳过首排规格
                if (ApsConstant.APS_STRING_1.equals(currencyTask.getLhScheduleResult().getIsFirst())){
                    continue;
                }

                // 跳过试产试制规格
                if (ApsConstant.APS_STRING_1.equals(currencyTask.getLhScheduleResult().getIsTrial())){
                    continue;
                }

                // 没有剩余产能则跳过
                if (remainingQty <= 0){
                    break;
                }

                // 跳过备库是0的规格
                if (currencyTask.getMaxTireQty() < 1){
                    continue;
                }

                double maxAddNum = currencyTask.getMaxTireQty();

                // 如果剩余产能不够则取剩余产能
                if (remainingQty < maxAddNum){
                    maxAddNum = remainingQty;
                }

                //检查是不是有下一个
//                boolean hasNext = false;
//                for (int j = i +1; j <= tasks.size() - 1;j++) {
//
//                    // 跳过首排规格
//                    if (ApsConstant.APS_STRING_1.equals(tasks.get(j).getLhScheduleResult().getIsFirst())){
//                        continue;
//                    }
//
//                    // 跳过试产试制规格
//                    if (ApsConstant.APS_STRING_1.equals(tasks.get(j).getLhScheduleResult().getIsTrial())){
//                        continue;
//                    }
//
//                    //如果规格本班次已经安排了计划则跳过:防止倒班
//                    if (tasks.get(j).getFieldValueByFieldName("class" + currentShift + "PlanQty") !=null){
//                        continue;
//                    }
//
//                    //如果计划在本班排查过则跳过
//                    if (!tasks.get(j).getIsEndTire()) {
//                        hasNext = true;
//                        break;
//                    }
//                }

                //如果没有下一个则全下
//                if (!hasNext) {
//                    maxAddNum = remainingQty;
//                }

                //考虑换工装时间
                double changeTime =  changeSpecTime(currencyTask.getCxProductConstructionInfoDto(),machine.getCxProductConstructionInfo(),Integer.valueOf(machine.getRollOverType()));

                remainingQty = (int) (remainingQty - maxAddNum);

                if (changeTime > 0) {
                    // 更新机台换工装次数
                    String changeNumField = "class" + currentShift + "ChangeNum";
                    int changeNum = (int) machine.getFieldValueByFieldName(changeNumField);
                    machine.setFieldValueByFieldName(changeNumField, changeNum + 1);
                    logInfo("班次[{}]换工装次数 → {}", currentShift, changeNum + 1);
                    currencyTask.setFieldValueByFieldName("class" + currentShift + "Analysis", String.format("换工装开班共耗时[%s]", changeTime) + (currencyTask.getEndTireDesc() == null ? "" : currencyTask.getEndTireDesc()));

                    startTime = startTime.plusSeconds((long) (changeTime * 3600));

                    // 机台默认产能
                    int quotaPerShift = currencyTask.getClassMachineDefaultQty();

                    // 计算可以让出的计划量
                    remainingQty = remainingQty - (int) (changeTime * 3600 * quotaPerShift / (cxShiftConfig.getShiftDuration() * 3600L));

                }else {
                    logInfo("换工装时间是：0.0 ,班次[{}]换工装次数 → {}, 不变", currentShift, 0);
                    currencyTask.setFieldValueByFieldName("class" + currentShift + "Analysis", (currencyTask.getEndTireDesc() == null ? "" : currencyTask.getEndTireDesc()));
                }

                for (int shift = currentShift; shift <= totalShifts && maxAddNum > 0; shift++) {
                    logDebug("班次处理开始 | 班次[{}] | 剩余量[{}]", shift, maxAddNum);

                    // 阶段1：获取班次定额
                    Integer shiftQuota = getShiftQuota(machine, shift, currencyTask.getCxProductConstructionInfoDto(), currencyTask);
                    if (shiftQuota == null || shiftQuota == 0) {
                        logWarn("机台寸口没有维护定额 | 班次定额缺失 | 班次[{}] | 终止排产", shift);
                        break;
                    }

                    // 阶段2：检查换工装限制
                    String changeNumField = "class" + shift + "ChangeNum";
                    int currentChangeNum = (int) machine.getFieldValueByFieldName(changeNumField);
                    if (currentChangeNum > maxChangeSpecNum) {
                        logWarn("换工装次数超限 | 班次[{}] | 当前[{}]/最大[{}]",
                                shift, currentChangeNum, maxChangeSpecNum);
                        break;
                    }

                    // 获取本班次结束时间
                    LocalDateTime shiftEndTime = LocalDateTime.parse(
                            cxShiftConfig.getShiftTimeByString("class" + shift).get("endTime"), formatter
                    );

                    // 计算本班次可用时长 = 班次结束时间 - 任务开始时间
                    Duration availableDuration = Duration.between(startTime, shiftEndTime);
                    BigDecimal availableSeconds = BigDecimal.valueOf(availableDuration.getSeconds());
                    if (availableSeconds.compareTo(BigDecimal.ZERO) <= 0){
                        availableSeconds = BigDecimal.valueOf(0);
                    }

                    // 计算理论产量 = 班次定额  / 班次时长  * 可用时长
                    BigDecimal plannedQty = BigDecimal.valueOf(shiftQuota).multiply(availableSeconds).divide(BigDecimal.valueOf(cxShiftConfig.getShiftDuration() * 3600L), 0, RoundingMode.DOWN);
                    // 计算理论产量耗时
                    BigDecimal actualQtyTime;
                    if (plannedQty.compareTo(BigDecimal.valueOf(maxAddNum)) >= 0) {
                        plannedQty = BigDecimal.valueOf(maxAddNum);
                        actualQtyTime = BigDecimal.valueOf(maxAddNum).multiply(BigDecimal.valueOf(cxShiftConfig.getShiftDuration() * 3600L).divide(BigDecimal.valueOf(shiftQuota), 8, RoundingMode.UP));
                    } else {
                        actualQtyTime = availableSeconds;
                    }
                    logDebug("班次[{}]产能计算 | 可用时长:{}s | 理论产量:{} | 实际分配:{} | 生产耗时：{}s",
                            shift, availableSeconds, shiftQuota, plannedQty, actualQtyTime);

                    //更新动态计算栏位
                    boolean alreadySchedule = false;
                    if (currencyTask.getFieldValueByFieldName("class" + currentShift + "PlanQty")  != null && (int)currencyTask.getFieldValueByFieldName("class" + currentShift + "PlanQty") > 0) {
                        //说明当前规格在本班次拉动过
                        //原来基础上更新任务班次计划量字段（例如：class1PlanQty）
                        String planField = "class" + shift + "PlanQty";
                        currencyTask.setFieldValueByFieldName(planField, (int)currencyTask.getFieldValueByFieldName(planField) + plannedQty.intValueExact());
                        alreadySchedule = true;
                    }else {
                        currencyTask.setFieldValueByFieldName("class" + shift + "StartTime", startTime);
                        // 更新任务班次计划量字段（例如：class1PlanQty）
                        String planField = "class" + shift + "PlanQty";
                        Object currentValue = currencyTask.getFieldValueByFieldName(planField);
                        int currentQty = currentValue != null ? (int)currentValue : 0;
                        currencyTask.setFieldValueByFieldName(planField, currentQty + plannedQty.setScale(0, RoundingMode.HALF_UP).intValueExact());
                    }

                    startTime = startTime.plusSeconds(actualQtyTime.longValue());
                    currencyTask.setFieldValueByFieldName("class" + shift + "EndTime", startTime);
                    currencyTask.setEndTime(startTime);

                    // 更新机台班次及顺序号
                    if (machine.getCurrentShift() != shift) {
                        logDebug("机台[{}]当前班次：[{}],切换至新班次：[{}]，重置顺序号", machine.getCxMachineCode(), machine.getCurrentShift(), shift);
                        machine.setCurrentShift(shift);
                        machine.setCurrentShiftSort(1);
                    } else {
                        machine.setCurrentShiftSort(machine.getCurrentShiftSort() + 1);
                        logDebug("机台[{}]班次[{}]顺序号+1 → {}",
                                machine.getCxMachineCode(), shift, machine.getCurrentShiftSort());
                    }

                    // 更新机台可用开始时间（累加已排产时间）
                    machine.setAvailableBeginTime(startTime);
                    logDebug("更新机台可用开始时间 → {}", machine.getAvailableBeginTime().format(formatter));

                    // 更新任务班次顺序（例如：class1ShiftSort）
                    String sortField = "class" + shift + "Sort";

                    //更新动态计算栏位
                    if (alreadySchedule) {
                        currencyTask.setFieldValueByFieldName("class" + currentShift + "Analysis", String.format("班次顺序由[%s]-[%s]", currencyTask.getFieldValueByFieldName(sortField) , machine.getCurrentShiftSort()) + currencyTask.getFieldValueByFieldName("class" + currentShift + "Analysis") );
                    }

                    currencyTask.setFieldValueByFieldName(sortField, machine.getCurrentShiftSort());


                    // 更新机台当日剩余时间
                    actualQtyTime = BigDecimal.valueOf(plannedQty.intValue()).multiply(BigDecimal.valueOf(cxShiftConfig.getShiftDuration()).divide(BigDecimal.valueOf(shiftQuota), 8, RoundingMode.UP));
                    BigDecimal remainTime = machine.getRemainTime()
                            .subtract(actualQtyTime);
                    machine.setRemainTime(remainTime);
                    logDebug("机台剩余时间更新 → {} 小时", remainTime);

                    maxAddNum -= plannedQty.intValue();
                    logDebug("剩余量更新 → {} ", remainingQty);

                    //更新成型胎胚月度剩余量
                    CxEmbryoMonthPlanSurplus cxEmbryoMonthPlanSurplus = CX_STOCK_MONTH_REMAIN_CONTEXT_MAP.get(currencyTask.getLhScheduleResult().getEmbryoCode() + currencyTask.getLhScheduleResult().getBomVersion());
                    if (cxEmbryoMonthPlanSurplus != null) {
                        cxEmbryoMonthPlanSurplus.setMonthRemainQty(cxEmbryoMonthPlanSurplus.getMonthRemainQty() - plannedQty.intValue());
                        logDebug("胎胚月度剩余量更新 | 胎胚[{}]月度剩余量 → {}", currencyTask.getLhScheduleResult().getEmbryoCode(), cxEmbryoMonthPlanSurplus.getMonthRemainQty());
                        CX_STOCK_MONTH_REMAIN_CONTEXT_MAP.put(currencyTask.getLhScheduleResult().getEmbryoCode() + currencyTask.getLhScheduleResult().getBomVersion(), cxEmbryoMonthPlanSurplus);
                    }
                }
                //占用机台后更新标记
                updateFinalStatus(currencyTask, machine);
            }
        }
    }

    /**
     * 计算可硫化班数（精密计算版）
     * 公式：可硫化班数 = 胎胚库存 / 单班硫化量
     * 注意：
     * 1. 结果保留2位小数（四舍五入）
     */
    private void calculateClassAvailableLiShift(LhAlgorithmScheduleResultDto dto) {
        // 获取任务胎胚
        String embryoCode = dto.getLhScheduleResult().getEmbryoCode();
        // 获取胎胚库存
        Integer stock = CX_STOCK_NUM_CONTEXT_MAP.get(embryoCode).getStockNum();
        // 获取任务单班硫化量
        Integer singleShiftQty = dto.getLhScheduleResult().getSingleMoldShiftLhQty();
        // 如果没有单班硫化量
        if (singleShiftQty == null || singleShiftQty == 0) {
            throw new RuntimeException("机台[" + dto.getFinalMachine() + "]-"+ embryoCode +"单班硫化量为空，无法计算可硫化班数");
        }
        //计算单班硫化量
        BigDecimal result =  BigDecimal.valueOf(stock).divide(BigDecimal.valueOf(singleShiftQty), 2, RoundingMode.HALF_UP);
        logInfo("可硫化班数 | 库存:{} / 单班量硫化量:{} = {}班",
                    stock, singleShiftQty, result);

        dto.setClassShiftNum(result.doubleValue());
    }


    /**
     * 生成最终于排程结果
     *
     * @param scheduleDate    排程日期
     * @param isTomorrow      是否预排排程
     * @param cxBatchNo       批次号
     * @param scheduleDateStr 排程日期字符串
     * @param factoryCode
     */
    private void generateFinalSchedule(Date scheduleDate, boolean isTomorrow, String cxBatchNo, Date scheduleDateStr, String factoryCode) {
        final String logPrefix = "【排程生成】";
        logInfo("{}开始生成排程结果 | 日期:{} 是否预排:{}", logPrefix, scheduleDate, isTomorrow);

        for (LhAlgorithmScheduleResultDto task : LH_MISS_TIRE_TIME_CONTEXT_MAP) {
            logDebug("{}处理任务 | 任务ID:{}", logPrefix, task);

            if ("L102".equals(task.getFinalMachine())){
                //todo debug 快速定位
                int a = 1;
            }

            // 校验机台和胎胚信息
            if (task.getFinalMachine() == null || task.getLhScheduleResult() == null) {
                log.warn("{}任务跳过 | 原因:无可用机台或胎胚信息", logPrefix);
                continue;
            }

            String resultKey = task.getLhScheduleResult().getEmbryoCode() +
                    task.getFinalMachine() +
                    task.getLhScheduleResult().getBomVersion();
            CxScheduleResultOld cxScheduleResult = CX_SCHEDULE_RESULT_CONTEXT_MAP.get(resultKey);

            if (cxScheduleResult == null) {
                logDebug("{}创建新排程结果 | 任务ID:{}", logPrefix, task);
                cxScheduleResult = new CxScheduleResultOld();
                CX_SCHEDULE_RESULT_CONTEXT_MAP.put(resultKey,cxScheduleResult);
                BeanUtils.copyProperties(task, cxScheduleResult);


                // 设置基础属性
                cxScheduleResult.setCxBatchNo(cxBatchNo);
                cxScheduleResult.setDelFlag(0);
                String scheduleStr = DateUtils.parseDateToStr("yyyyMMdd", scheduleDateStr);
                cxScheduleResult.setOrderNo(commonRedisService.getSequence(
                        CxPrefixConstants.SCHEDULE_ORDER_NO_PREFIX + scheduleStr,
                        CxPrefixConstants.CX_ORDER_NO_PREFIX + scheduleStr));
                cxScheduleResult.setProductionStatus(CxEngineConstants.PRODUCTION_STATUS_DOING);
                cxScheduleResult.setIsRelease(CxEngineConstants.IS_PUBLISH_NO);
                cxScheduleResult.setFactoryCode(SecurityUtils.getUserCurrentFactory());
                if (SecurityUtils.getUserCurrentFactory() == null){
                    cxScheduleResult.setFactoryCode(factoryCode);
                }
                cxScheduleResult.setScheduleDate(scheduleDate);
                cxScheduleResult.setPublishSuccessCount(0);
                cxScheduleResult.setEmbryoCode(task.getLhScheduleResult().getEmbryoCode());
                cxScheduleResult.setClass1PlanQty(0);
                cxScheduleResult.setClass2PlanQty(0);
                cxScheduleResult.setClass3PlanQty(0);
                cxScheduleResult.setClass4PlanQty(0);
                cxScheduleResult.setClass5PlanQty(0);
                cxScheduleResult.setClass6PlanQty(0);
                cxScheduleResult.setClass1FinishQty(0);
                cxScheduleResult.setClass2FinishQty(0);
                cxScheduleResult.setClass3FinishQty(0);
                cxScheduleResult.setClass4FinishQty(0);
                cxScheduleResult.setClass5FinishQty(0);
                cxScheduleResult.setClass6FinishQty(0);
                cxScheduleResult.setClass1FinishRate(0);
                cxScheduleResult.setClass2FinishRate(0);
                cxScheduleResult.setClass3FinishRate(0);
                cxScheduleResult.setClass4FinishRate(0);
                cxScheduleResult.setClass5FinishRate(0);
                cxScheduleResult.setClass6FinishRate(0);
                cxScheduleResult.setClass1Sort(null);
                cxScheduleResult.setClass2Sort(null);
                cxScheduleResult.setClass3Sort(null);
                cxScheduleResult.setClass4Sort(null);
                cxScheduleResult.setClass5Sort(null);
                cxScheduleResult.setClass6Sort(null);
                cxScheduleResult.setClass1Analysis(null);
                cxScheduleResult.setClass2Analysis(null);
                cxScheduleResult.setClass3Analysis(null);
                cxScheduleResult.setClass4Analysis(null);
                cxScheduleResult.setClass5Analysis(null);
                cxScheduleResult.setClass6Analysis(null);
                cxScheduleResult.setCxMachineQty(null);
                cxScheduleResult.setClass1MachineQuota((double) 0);
                cxScheduleResult.setClass2MachineQuota((double) 0);
                cxScheduleResult.setClass3MachineQuota((double) 0);
                cxScheduleResult.setClass4MachineQuota((double) 0);
                cxScheduleResult.setClass5MachineQuota((double) 0);
                cxScheduleResult.setClass6MachineQuota((double) 0);
                cxScheduleResult.setLhClass1Plan((double) 0);
                cxScheduleResult.setLhClass2Plan((double) 0);
                cxScheduleResult.setLhClass3Plan((double) 0);
                cxScheduleResult.setLhClass4Plan((double) 0);
                cxScheduleResult.setLhClass5Plan((double) 0);
                cxScheduleResult.setLhClass6Plan((double) 0);
                cxScheduleResult.setProductNum(
                        (double) ((task.getClass1PlanQty() == null ? 0 : task.getClass1PlanQty()) +
                                                        (task.getClass2PlanQty() == null ? 0 : task.getClass2PlanQty()) +
                                                        (task.getClass3PlanQty() == null ? 0 : task.getClass3PlanQty()))
                );
                cxScheduleResult.setTotalStock(task.getInitialInventory());
                cxScheduleResult.setPreviousTireTime(
                        Date.from(task.getPreviousTireTime().atZone(ZoneId.of("GMT+8")).toInstant())
                );
                cxScheduleResult.setRemark(task.getEndTireDesc());
            }

            cxScheduleResult.setCxMachineCode(task.getFinalMachine());
            cxScheduleResult.setCxMachineName(task.getFinalMachine());

            CxMachineInfoVo machineInfoVo = CX_MACHINE_ACCTIVE_TIME_CONTEXT_MAP.get(task.getFinalMachine());
            if (machineInfoVo != null) {
                MdmMoldingMachineCls machineCls = machineInfoVo.getMoldingMachineCls();
                cxScheduleResult.setCxMachineType(String.valueOf(machineCls.getMouldMethod()));

                // 处理寸口范围
                List<MdmMoldingMachineClsB> list = machineInfoVo.getMoldingMachineClassList();
                if (list != null && !list.isEmpty()) {
                    String proRange = list.stream()
                            .filter(Objects::nonNull)
                            .map(MdmMoldingMachineClsB::getProSize)
                            .filter(Objects::nonNull)
                            .map(size -> new BigDecimal(String.valueOf(size)).setScale(2, RoundingMode.HALF_UP).toString())
                            .distinct()
                            .sorted(Comparator.naturalOrder())
                            .collect(Collectors.joining("/"));
                    cxScheduleResult.setCxMachineProRange(proRange);
                    logDebug("{}寸口范围计算 | 结果:{}", logPrefix, proRange);
                }
            }

            for (String classCode : isTomorrow ? cxShiftConfig.getTomorrowClasses("", "") : cxShiftConfig.getTodayClasses("", "")) {
                int modClass = Integer.parseInt(classCode) > 3 ? Integer.parseInt(classCode) % 3: Integer.parseInt(classCode);

                // 机台定额
                Object machineQty = task.getFieldValueByFieldName("class" + modClass + "MachineQty");
                if (machineQty == null) {
                    cxScheduleResult.setFieldValueByFieldName("class" + classCode + "MachineQuota",
                            task.getClassMachineDefaultQty() == null ? 0 : task.getClassMachineDefaultQty());
                } else {
                    cxScheduleResult.setFieldValueByFieldName("class" + classCode + "MachineQuota",
                             machineQty);
                    cxScheduleResult.setFieldValueByFieldName("class" + classCode + "Analysis",
                            machineQty + "培训");
                }

                // 计划设定
                cxScheduleResult.setFieldValueByFieldName("class" + classCode + "PlanQty",
                        task.getFieldValueByFieldName("class" + modClass + "PlanQty") == null ? 0 : task.getFieldValueByFieldName("class" + modClass + "PlanQty"));

                // 计划顺序
                cxScheduleResult.setFieldValueByFieldName("class" + classCode + "Sort",
                        task.getFieldValueByFieldName("class" + modClass + "Sort") );

                // 原因分析
                cxScheduleResult.setFieldValueByFieldName("class" + classCode + "Analysis",
                        (cxScheduleResult.getFieldValueByFieldName("class" + classCode + "Analysis") == null ? "" : cxScheduleResult.getFieldValueByFieldName("class" + classCode + "Analysis")) + " " +
                                (task.getFieldValueByFieldName("class" + modClass + "Analysis") == null ? "" : task.getFieldValueByFieldName("class" + modClass + "Analysis")) );


                // 计划开始时间
                Object startTime = task.getFieldValueByFieldName("class" + modClass + "StartTime");
                if (startTime instanceof LocalDateTime) {
                    // 将 LocalDateTime 转换为 Date
                    LocalDateTime ldt = (LocalDateTime) startTime;
                    Date convertedStartTime = Date.from(ldt.atZone(ZoneId.of("GMT+8")).toInstant());
                    cxScheduleResult.setFieldValueByFieldName("class" + classCode + "StartTime", convertedStartTime);
                } else {
                    cxScheduleResult.setFieldValueByFieldName("class" + classCode + "StartTime", startTime);
                }

                // 计划结束时间
                Object endTime = task.getFieldValueByFieldName("class" + modClass + "EndTime");
                if (endTime instanceof LocalDateTime) {
                    // 将 LocalDateTime 转换为 Date
                    LocalDateTime ldt = (LocalDateTime) endTime;
                    Date convertedEndTime = Date.from(ldt.atZone(ZoneId.of("GMT+8")).toInstant());
                    cxScheduleResult.setFieldValueByFieldName("class" + classCode + "EndTime", convertedEndTime);
                } else {
                    cxScheduleResult.setFieldValueByFieldName("class" + classCode + "EndTime", endTime);
                }

                //硫化1/2/3班消耗
                cxScheduleResult.setFieldValueByFieldName("lhClass" + classCode + "Plan",
                        task.getLhScheduleResult().getFieldValueByFieldName("class" + modClass + "PlanQty") == null ? 0 : task.getLhScheduleResult().getFieldValueByFieldName("class" + modClass + "PlanQty"));

            }

            LhScheduleResult lhScheduleResult = task.getLhScheduleResult();
            cxScheduleResult.setCxMachineQty(task.getClassMachineDefaultQty());
            cxScheduleResult.setLhMachineCode(lhScheduleResult.getLhMachineCode());
            cxScheduleResult.setLhMachineName(lhScheduleResult.getLhMachineCode());
            cxScheduleResult.setLhScheduleIds(lhScheduleResult.getMergeIds());
            // For SAP Code (from ProductCode)
            String productCode = lhScheduleResult.getProductCode();
            cxScheduleResult.setSapCode(removeDuplicatesAndJoin(productCode, "/"));

            // For Spec Code
            String specCode = lhScheduleResult.getSpecCode();
            cxScheduleResult.setSpecCode(removeDuplicatesAndJoin(specCode, "/"));

            // For Spec Description
            String specDesc = lhScheduleResult.getSpecDesc();
            cxScheduleResult.setSpecDesc(removeDuplicatesAndJoin(specDesc, "/"));
            cxScheduleResult.setLhMachineQty(new BigDecimal(lhScheduleResult.getMoldQty()).doubleValue());
            cxScheduleResult.setSingleShiftLhQty(lhScheduleResult.getSingleMoldShiftLhQty());
            cxScheduleResult.setLhSingleTireTime(lhScheduleResult.getLhTime().doubleValue());


            cxScheduleResult.setBomDataVersion(lhScheduleResult.getBomVersion());
            cxScheduleResult.setSpecDimension(task.getCxProductConstructionInfoDto().getDimension());
            logInfo("{}任务处理完成 | 任务ID:{}", logPrefix, task);


            //原因分析去重
            String class1Analysis = cxScheduleResult.getClass1Analysis();
            cxScheduleResult.setClass1Analysis(removeDuplicatesAndJoin(class1Analysis, " "));
            String class2Analysis = cxScheduleResult.getClass2Analysis();
            cxScheduleResult.setClass2Analysis(removeDuplicatesAndJoin(class2Analysis, " "));
            String class3Analysis = cxScheduleResult.getClass3Analysis();
            cxScheduleResult.setClass3Analysis(removeDuplicatesAndJoin(class3Analysis, " "));
            String class4Analysis = cxScheduleResult.getClass4Analysis();
            cxScheduleResult.setClass4Analysis(removeDuplicatesAndJoin(class4Analysis, " "));
            String class5Analysis = cxScheduleResult.getClass5Analysis();
            cxScheduleResult.setClass5Analysis(removeDuplicatesAndJoin(class5Analysis, " "));
            String class6Analysis = cxScheduleResult.getClass6Analysis();
            cxScheduleResult.setClass6Analysis(removeDuplicatesAndJoin(class6Analysis, " "));
        }

        logInfo("{}排程结果生成完成", logPrefix);
    }


    // Helper method to handle duplicates and joining
    private String removeDuplicatesAndJoin(String input, String delimiter) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // Split by delimiter if it exists in the input
        String[] parts = input.split(Pattern.quote(delimiter));

        // Remove duplicates while preserving order
        Set<String> uniqueParts = new LinkedHashSet<>(Arrays.asList(parts));

        // Join back with delimiter
        return String.join(delimiter, uniqueParts);
    }
}









