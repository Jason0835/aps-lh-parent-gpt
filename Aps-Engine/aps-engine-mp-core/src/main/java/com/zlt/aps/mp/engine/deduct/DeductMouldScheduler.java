package com.zlt.aps.mp.engine.deduct;

import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.domain.dto.CxContinueSkuInfoHelper;
import com.zlt.aps.mp.engine.scheduling.cxcapacity.ProductionCapacityParamConfiguration;
import com.zlt.aps.mp.api.domain.deduct.DailyScheduleVo;
import com.zlt.aps.mp.api.domain.deduct.DeductMouldContext;
import com.zlt.aps.mp.api.domain.deduct.DeductMouldVo;
import com.zlt.common.utils.PubUtil;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 轮胎APS降模排产系统
 * 核心逻辑：
 * 1. 所有排产量必须在结构收尾日前全部排完
 * 2. 第1天，延续上个月不变
 * 3. 按机台数量和收尾临近天数动态降模
 *
 * @author Sandy
 * @date 2025/12/24
 */
public class DeductMouldScheduler {

    /**
     * 构建降膜排产参数对象
     *
     * @param deadLineDay        收尾日
     * @param stopDays           停工日集合
     * @param openDays           开产日集合
     * @param paramConfiguration 排产参数对象
     * @param continueSkuInfo    续作Sku信息
     * @return
     */
    public static DeductMouldVo createDeductMouldBySku(Integer deadLineDay, Set<Integer> stopDays, Set<Integer> openDays, ProductionCapacityParamConfiguration paramConfiguration, CxContinueSkuInfoHelper
            continueSkuInfo) {
        DeductMouldVo deductMould = new DeductMouldVo();
        //参数设置
        deductMould.setShutDownDaySet(stopDays);
        deductMould.setProductionStartDaySet(openDays);
        //降膜排产-相关的参数 当前的硫化机台数超过该值 默认为3
        deductMould.setParamAssignedMachines(paramConfiguration.getDeductMouldMinLhMachineCount());
        //降膜排产-相关的参数 7天时降到3台
        deductMould.setParamNearDeadline7(paramConfiguration.getFirstNearDeadLineDay());
        deductMould.setParamReduceMachines3(paramConfiguration.getFirstNearDeadLineMaxLhMachineCount());
        //降膜排产-相关的参数 5天时降到2台
        deductMould.setParamNearDeadline5(paramConfiguration.getSecondNearDeadLineDay());
        deductMould.setParamReduceMachines2(paramConfiguration.getSecondNearDeadLineMaxLhMachineCount());
        //降膜排产-相关的参数 2天时降到1台
        deductMould.setParamNearDeadline2(paramConfiguration.getLastNearDeadLineDay());
        deductMould.setParamReduceMachines1(paramConfiguration.getLastNearDeadLineMaxLhMachineCount());

        //续作Sku信息 -
        deductMould.setMaterialCode(continueSkuInfo.getMaterialDesc());
        deductMould.setStartDate(ProductionConstant.MONTH_START_DAY);
        deductMould.setDeadline(deadLineDay);
        deductMould.setTotalQty(continueSkuInfo.getPlanDemandQty());
        deductMould.setRemainingQty(continueSkuInfo.getPlanDemandQty());
        Integer startLhMachineCount = continueSkuInfo.getMouldNumber() / ProductionConstant.DOUBLE_MOULD_PRODUCTION;
        deductMould.setMachinesAssigned(startLhMachineCount);
        deductMould.setDailyOutputPerMachine(continueSkuInfo.getMaxDaySingleLhMachineQty());
        return deductMould;
    }

    /**
     * 执行降模排产计划
     *
     * @return 每日排产计划列表
     */
    public static List<DailyScheduleVo> scheduleProduction(DeductMouldVo deductMouldVo) {

        DeductMouldContext context = new DeductMouldContext();
        List<DailyScheduleVo> schedules = new ArrayList<>();
        context.setCurrentDate(getValidDate(deductMouldVo.getStartDate(), deductMouldVo, schedules));

        //收尾日
        context.setDeadLineDate(deductMouldVo.getDeadline());
        // 1、第1天：延续上个月不变
        if (deductMouldVo.isFirstDayDelay()){
            DailyScheduleVo firstDayVo = createFirstDaySchedule(deductMouldVo, context.getCurrentDate());
            schedules.add(firstDayVo);
            updateRemainingQuantities(deductMouldVo, firstDayVo);
            context.setCurrentDate(getValidDate(context.getCurrentDate() + 1, deductMouldVo, schedules));
        }

        // 初始化：第1天延续上月配置
        context.setPreDayMachines(deductMouldVo.getMachinesAssigned());
        context.setPreRemainQty(deductMouldVo.getTotalQty());
        // 前日计划量 = 前日机台数 * 每日硫化量
        context.setPreDayQty(context.getPreDayMachines() * deductMouldVo.getDailyOutputPerMachine());
        // 预计收尾天数 = 前日剩余量/前日计划
        context.setExpectedDays((int) Math.ceil((double) context.getPreRemainQty() / context.getPreDayQty()));

        // 2、从第2天开始排产
        while (hasRemainingProduction(deductMouldVo)
                && context.getCurrentDate() <= context.getDeadLineDate()) {
            // 创建每日计划
            DailyScheduleVo dailyScheduleVo = createDailySchedule(deductMouldVo, context);
            schedules.add(dailyScheduleVo);
            // 更新前一天机台配置
            context.setPreDayMachines(dailyScheduleVo.getSkuMachines());
            context.setPreRemainQty(context.getPreRemainQty() - dailyScheduleVo.getSkuPreQty());
            // 前日计划量
            context.setPreDayQty(dailyScheduleVo.getSkuQuantity());
            // 预计收尾天数 = 前日剩余量/前日计划
            context.setExpectedDays((int) Math.ceil((double) context.getPreRemainQty() / context.getPreDayQty()));
            updateRemainingQuantities(deductMouldVo, dailyScheduleVo);
            context.setCurrentDate(getValidDate(context.getCurrentDate() + 1, deductMouldVo, schedules));

            // 检查是否所有任务都已完成
            if (!hasRemainingProduction(deductMouldVo)) {
                break;
            }
        }

        return schedules;
    }

    /**
     * 获取有效日期
     *
     * @param currentDate   当前日期
     * @param deductMouldVo 当前降模Vo
     * @param schedules     排程列表
     * @return 有效日期
     */
    private static int getValidDate(int currentDate, DeductMouldVo deductMouldVo, List<DailyScheduleVo> schedules) {
        if (PubUtil.isEmpty(deductMouldVo.getShutDownDaySet())) {
            return currentDate;
        }
        while (deductMouldVo.getShutDownDaySet().contains(currentDate)) {
            if (schedules != null) {
                DailyScheduleVo dailyScheduleVo = new DailyScheduleVo();
                dailyScheduleVo.setMaterialCode(deductMouldVo.getMaterialCode());
                dailyScheduleVo.setScheduleDate(currentDate);
                dailyScheduleVo.setSkuMachines(0);
                dailyScheduleVo.setSkuQuantity(0);
                dailyScheduleVo.setSkuPreQty(0);
                schedules.add(dailyScheduleVo);
            }
            currentDate += 1;
            if (!deductMouldVo.getShutDownDaySet().contains(currentDate)) {
                break;
            }
        }
        return currentDate;
    }

    /**
     * 根据规则确定当前激活机台数
     *
     * @param deductMouldVo 降模Vo
     * @param expectedDays  预计收尾天数
     * @return 应该激活的机台数
     */
    private static int getActiveMachinesByRule(DeductMouldVo deductMouldVo, int expectedDays) {
        // 第1天延续上月配置（调用处处理）
        if (deductMouldVo.getMachinesAssigned() <= 0) {
            return 0;
        }

        // 规则2: 单个SKU机台等于3台
        if (deductMouldVo.getMachinesAssigned().equals(deductMouldVo.getParamAssignedMachines())) {
            if (expectedDays <= deductMouldVo.getParamNearDeadline2()) {
                // 临近收尾2天，降到1台
                return deductMouldVo.getParamReduceMachines1();
            } else if (expectedDays <= deductMouldVo.getParamNearDeadline5()) {
                // 临近收尾5天，降到2台
                return deductMouldVo.getParamReduceMachines2();
            } else {
                // 正常3台
                return deductMouldVo.getParamReduceMachines3();
            }
        }

        // 规则3: 单个SKU机台大于3台
        if (deductMouldVo.getMachinesAssigned() > deductMouldVo.getParamAssignedMachines()) {
            if (expectedDays <= deductMouldVo.getParamNearDeadline2()) {
                // 临近收尾2天，降到1台
                return deductMouldVo.getParamReduceMachines1();
            } else if (expectedDays <= deductMouldVo.getParamNearDeadline5()) {
                // 临近收尾5天，降到2台
                return deductMouldVo.getParamReduceMachines2();
            } else if (expectedDays <= deductMouldVo.getParamNearDeadline7()) {
                // 临近收尾7天，降到3台
                return deductMouldVo.getParamReduceMachines3();
            } else {
                // 正常全开
                return deductMouldVo.getMachinesAssigned();
            }
        }

        // 其他情况（小于3台）：保持不变
        return deductMouldVo.getMachinesAssigned();
    }

    /**
     * 创建第1天降模排产计划（延续上个月不变）
     */
    private static DailyScheduleVo createFirstDaySchedule(DeductMouldVo deductMouldVo, int currentDate) {
        DailyScheduleVo dailyScheduleVo = new DailyScheduleVo();
        dailyScheduleVo.setMaterialCode(deductMouldVo.getMaterialCode());
        dailyScheduleVo.setScheduleDate(currentDate);
        if (deductMouldVo.getRemainingQty() <= 0) {
            return dailyScheduleVo;
        }
        // 第1天延续上月机台配置
        int activeMachines = deductMouldVo.getMachinesAssigned();
        // 当首日低于需要的机台数，按低的取激活台数
        int needMachines = (int) Math.ceil((double) deductMouldVo.getRemainingQty() / deductMouldVo.getDailyOutputPerMachine());
        activeMachines = Math.min(activeMachines, needMachines);
        dailyScheduleVo.setSkuMachines(activeMachines);
        // 日计划量 = 激活台数 * 每日硫化量
        int dailyCapacity = activeMachines * deductMouldVo.getDailyOutputPerMachine();
        // 处理开产首日计划量
        dailyCapacity = doProductionStartDayRatio(dailyScheduleVo.getScheduleDate(), dailyCapacity, deductMouldVo);
        int todayOutput = Math.min(deductMouldVo.getRemainingQty(), dailyCapacity);
        dailyScheduleVo.setSkuQuantity(todayOutput);
        dailyScheduleVo.setSkuPreQty(dailyCapacity);
        return dailyScheduleVo;
    }

    /**
     * 处理开产首日计划量
     *
     * @param scheduleDate  排产日
     * @param dailyCapacity 排产量
     * @param deductMouldVo 降模Vo
     * @return 开产首日计划量
     */
    private static int doProductionStartDayRatio(int scheduleDate, int dailyCapacity, DeductMouldVo deductMouldVo) {
        if (PubUtil.isEmpty(deductMouldVo.getProductionStartDaySet())) {
            return dailyCapacity;
        }

        //若排产日 = 开产日，则日计划量 = 日计划量*开产比例
        if (deductMouldVo.getProductionStartDaySet().contains(scheduleDate)) {
            dailyCapacity = (int) Math.ceil((double) dailyCapacity * deductMouldVo.getParamStartDayRatio());
            // 判断奇数,奇数加1
            if (dailyCapacity % 2 == 1) {
                dailyCapacity += 1;
            }
        }
        return dailyCapacity;
    }

    /**
     * 创建每日排产计划（第2天及以后）
     */
    private static DailyScheduleVo createDailySchedule(DeductMouldVo deductMouldVo,
                                                       DeductMouldContext context) {
        DailyScheduleVo dailyScheduleVo = new DailyScheduleVo();
        dailyScheduleVo.setScheduleDate(context.getCurrentDate());
        if (deductMouldVo.getRemainingQty() <= 0) {
            return dailyScheduleVo;
        }

        int activeMachines;
        //1、模拟计算，判断在收尾日前是否可以正常排产完
        boolean isRemain = isRemainBySimulationCalc(deductMouldVo, context);
        if (isRemain) {
            //1.1 在收尾日不能排产完，激活机台数延续前日的机台数
            activeMachines = context.getPreDayMachines();
            //1.2 若在收尾日，激活机台数不能大于需要的机台数；
            if (context.getCurrentDate().equals(context.getDeadLineDate())) {
                int needMachines = (int) Math.ceil((double) deductMouldVo.getRemainingQty() / deductMouldVo.getDailyOutputPerMachine());
                activeMachines = Math.min(activeMachines, needMachines);
            }
        } else {
            //1.3 在收尾日通排产完，则根据规则确定激活机台数
            activeMachines = getActiveMachinesByRule(deductMouldVo, context.getExpectedDays());
            // 若前日机台数 与 激活机台数 相差>=3台，采取均降策略
            int diffMachines = context.getPreDayMachines() - activeMachines;
            if (diffMachines >= FactoryConstant.FRONT_ACTIVE_DIFF_MACHINES){
                activeMachines = (int) Math.ceil((double) context.getPreDayMachines() / FactoryConstant.AVG_VALUE);
            }else{
                // 动态计算所需机台数 sandy+ 2026.4.13
                // 在结构未收尾的情况下，不再依赖固定天数，而是根据剩余需求、剩余天数、单台日产量动态决定当日应使用的模具数，逐日降1台，
                // 即采取提前降模策略，使得计划量可以拉满到结构收尾
                int remainingDays = deductMouldVo.getDeadline() - context.getCurrentDate();
                int requiredDailyCapacity = (int) Math.ceil((double) deductMouldVo.getRemainingQty() / remainingDays);
                int neededMachines = (int) Math.ceil((double) requiredDailyCapacity / deductMouldVo.getDailyOutputPerMachine());
                activeMachines = Math.min(activeMachines, context.getPreDayMachines());
                if (activeMachines > neededMachines){
                    activeMachines -= 1;
                }
            }
        }

        //2、在收尾日倒数第2天 且前日机台数大于3台，优化激活台数
        if (context.getPreDayMachines() >= FactoryConstant.FRONT_MACHINES_THRESHOLD
                && (context.getCurrentDate() + 1) == context.getDeadLineDate()) {
            //在保障全部收尾的情况下，采取均分策略
            int avgMachines = (int) Math.ceil((double) context.getPreDayMachines() / FactoryConstant.AVG_VALUE);
            int dailyCapacity = FactoryConstant.AVG_VALUE * avgMachines * deductMouldVo.getDailyOutputPerMachine();
            if (deductMouldVo.getRemainingQty() <= dailyCapacity) {
                activeMachines = avgMachines;
            }
        }

        // 保证激活机台数不超过前日分配数
        activeMachines = Math.min(activeMachines, context.getPreDayMachines());

        // 4、若调用方有输入 最大限制机台，且激活台数大于最大限制台数，则激活台数 = 输入的最大限制台
        Integer limitMaxMachines = 0;
        if (deductMouldVo.getDayMaxMachinesLimitMap() != null && deductMouldVo.getDayMaxMachinesLimitMap().get(context.getCurrentDate()) != null){
            limitMaxMachines =deductMouldVo.getDayMaxMachinesLimitMap().get(context.getCurrentDate());
        }
        if (limitMaxMachines > 0 && activeMachines > limitMaxMachines){
            activeMachines = limitMaxMachines;
        }

        // 5、计算当日产量，并组装日计划实体
        int dailyCapacity = activeMachines * deductMouldVo.getDailyOutputPerMachine();
        // 处理开产首日计划量
        dailyCapacity = doProductionStartDayRatio(dailyScheduleVo.getScheduleDate(), dailyCapacity, deductMouldVo);
        int todayOutput = Math.min(deductMouldVo.getRemainingQty(), dailyCapacity);

        if (todayOutput > 0) {
            dailyScheduleVo.setMaterialCode(deductMouldVo.getMaterialCode());
            dailyScheduleVo.setSkuQuantity(todayOutput);
            dailyScheduleVo.setSkuMachines(activeMachines);
            dailyScheduleVo.setSkuPreQty(context.getPreDayQty());
        }

        return dailyScheduleVo;
    }

    /**
     * 模拟计算，判断在收尾日前是否可以正常排产完
     *
     * @param deductMouldVo 降模Vo
     * @param context       降模上下文
     * @return false-有剩余不能排产完；true-可以排产完
     */
    private static boolean isRemainBySimulationCalc(DeductMouldVo deductMouldVo, DeductMouldContext context) {
        // 当前计算日期
        int currentDate = context.getCurrentDate();
        // 收尾日
        int deadLineDate = context.getDeadLineDate();
        // 预计收尾天数
        int expectedDays = context.getExpectedDays();
        // 前日计划量
        int preDayQty = context.getPreDayQty();
        // 前日剩余量
        int preRemainQty = context.getPreRemainQty();
        // 目前剩余量
        int remainQty = deductMouldVo.getRemainingQty();

        int activeMachines, dailyCapacity, todayOutput;
        while (remainQty > 0 && currentDate <= deadLineDate) {
            // 根据规则确定激活机台数
            activeMachines = getActiveMachinesByRule(deductMouldVo, expectedDays);
            // 保证激活机台数不超过总分配数
            activeMachines = Math.min(activeMachines, deductMouldVo.getMachinesAssigned());

            // 计算当日产量
            dailyCapacity = activeMachines * deductMouldVo.getDailyOutputPerMachine();
            // 处理开产首日计划量
            dailyCapacity = doProductionStartDayRatio(currentDate, dailyCapacity, deductMouldVo);

            todayOutput = Math.min(remainQty, dailyCapacity);

            // 更新前一天的剩余量
            preRemainQty = preRemainQty - preDayQty;

            // 前日计划量
            preDayQty = dailyCapacity;
            // 预计收尾天数 = 前日剩余量/前日计划
            expectedDays = (int) Math.ceil((double) preRemainQty / preDayQty);

            remainQty = remainQty - todayOutput;
            currentDate = getValidDate(currentDate + 1, deductMouldVo, null);
            // 检查是否所有任务都已完成
            if (remainQty <= 0) {
                break;
            }
        }

        if (remainQty > 0) {
            return true;
        }
        return false;
    }

    /**
     * 更新剩余产量
     */
    private static void updateRemainingQuantities(DeductMouldVo deductMouldVo, DailyScheduleVo dailyScheduleVo) {
        int remainingQty = deductMouldVo.getRemainingQty() - dailyScheduleVo.getSkuQuantity();
        deductMouldVo.setRemainingQty(remainingQty);
    }

    /**
     * 检查是否还有剩余产量
     */
    private static boolean hasRemainingProduction(DeductMouldVo deductMouldVo) {
        return deductMouldVo.getRemainingQty() > 0;
    }

    /**
     * 当前日期到收尾日的天数
     *
     * @param currentDate  当前日期
     * @param deadLineDate 收尾日
     * @return 间隔天数
     */
    private static long getDaysToDeadLine(LocalDate currentDate, LocalDate deadLineDate) {
        return ChronoUnit.DAYS.between(currentDate, deadLineDate) + 1;
    }
}

