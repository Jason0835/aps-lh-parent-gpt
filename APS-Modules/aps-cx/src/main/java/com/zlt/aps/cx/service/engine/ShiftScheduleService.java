package com.zlt.aps.cx.service.engine;

import com.zlt.aps.cx.constant.ScheduleConstants;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.cx.vo.MonthPlanProductLhCapacityVo;
import com.zlt.aps.cx.vo.DailyEmbryoTask;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.cx.vo.ShiftProductionResult;
import com.zlt.aps.mp.api.domain.entity.MdmDevicePlanShut;
import com.zlt.aps.mp.api.domain.entity.MdmStructureLhRatio;
import com.zlt.aps.cx.api.domain.entity.CxPrecisionPlan;
import com.zlt.aps.cx.api.domain.entity.CxStructureTreadConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * S5.3.7 班次精排服务 — 将机台级任务分配落到具体班次时间片，产出 {@link ShiftProductionResult}。
 *
 * <h3>流水线位置</h3>
 * <pre>
 * CoreScheduleAlgorithmServiceImpl.executeShiftSchedule
 *   → 5.3.4 合并 continue + new + trial 的 MachineAllocationResult
 *   → 5.3.5 applyPrecisionPlanSelection（可能扣减 TaskAllocation 条数）
 *   → 5.3.7 本类：逐机台 × 逐 TaskAllocation 调用 {@link #scheduleTaskToShifts}
 *   → 汇总为 CxScheduleResult（CLASS1~8）
 * </pre>
 *
 * <h3>上游约定</h3>
 * <ul>
 *   <li>计划条数：以 {@code DailyEmbryoTask.endingExtraInventory} 为准（TaskGroupService 收尾/立库封顶后），
 *       <b>非</b> {@code plannedProduction}。</li>
 *   <li>机台与硫化机台数：由 ContinueTaskProcessor / NewTaskProcessor / TrialTaskProcessor 已确定。</li>
 *   <li>开停产标志：{@code isOpeningDayTask} / {@code isClosingDayTask} 等由 TaskGroupService 写入；
 *       班次类型语义与 {@link com.zlt.aps.cx.enums.ShiftType} 对齐。</li>
 * </ul>
 *
 * <h3>任务类型路由（{@link #scheduleTaskToShifts}，优先级不可调换）</h3>
 * <table>
 *   <tr><th>优先级</th><th>条件</th><th>策略方法</th><th>要点</th></tr>
 *   <tr><td>1</td><td>试制</td><td>{@code scheduleTrialTask}</td><td>早/中班、双数、不补整车</td></tr>
 *   <tr><td>2</td><td>量试且开产日</td><td>{@code scheduleTrialTask}</td><td>同试制班次限制</td></tr>
 *   <tr><td>3</td><td>停产班</td><td>{@code scheduleClosingTask}</td><td>停锅班不补整车；之前班整车</td></tr>
 *   <tr><td>4</td><td>开产日</td><td>{@code scheduleOpeningTask} / normal</td><td>成型开产首班 6h 封顶；关键产品首班不排</td></tr>
 *   <tr><td>5</td><td>收尾</td><td>{@code scheduleEndingTask}</td><td>不晚于硫化收尾班；末班可非整车</td></tr>
 *   <tr><td>6</td><td>默认</td><td>{@code scheduleNormalTask}</td><td>波浪分车，相邻班差≤1 车</td></tr>
 * </table>
 *
 * <h3>产能与时间</h3>
 * <ul>
 *   <li>小时产能：{@link #getMachineHourlyCapacity} — 日硫化量 × 结构配比 → 单胎秒数 → 条/小时</li>
 *   <li>单车容量：{@link #getTripCapacity} — 结构胎面配置 / 默认 12 条</li>
 *   <li>班次窗口：{@link #calculateStartTime} / {@link #calculateShiftEndTime}（跨天班 IS_CROSS_DAY=1）</li>
 * </ul>
 *
 * @author APS Team
 * @see ScheduleDayTypeHelper
 * @see com.zlt.aps.cx.service.impl.CoreScheduleAlgorithmServiceImpl
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShiftScheduleService {

    // ==================== 业务阈值常量 ====================

    /** 无法从日硫化量+配比推算时的小时产能兜底（条/小时） */
    private static final int DEFAULT_HOURLY_CAPACITY = 50;

    /** 开产首班（OPEN_START / formingOpeningShiftOrder）产能时长上限（小时） */
    private static final int OPENING_FIRST_SHIFT_HOURS = 6;

    /** 班次配置未填 shiftHours 时的默认班时长（小时） */
    private static final int DEFAULT_SHIFT_HOURS = 8;

    /** 机台班初准备时间扣减（分钟），计入 calculateStartTime */
    private static final int DEFAULT_MACHINE_PREPARE_MINUTES = 30;

    // ==================== 班次编码（与 CxShiftConfig.shiftCode 对齐） ====================

    /** 班次编码：夜班 */
    public static final String SHIFT_NIGHT = "SHIFT_NIGHT";
    /** 班次编码：早班 */
    public static final String SHIFT_DAY = "SHIFT_DAY";
    /** 班次编码：中班 */
    public static final String SHIFT_AFTERNOON = "SHIFT_AFTERNOON";

    private final ProductionCalculator productionCalculator;

    // ==================== 5.3.7 精排入口 ====================

    /**
     * 单任务班次精排 — 本类唯一 public 业务入口。
     *
     * <p>将 {@code task.endingExtraInventory}（条）分配到 {@code dayShifts} 各时间片，并计算计划起止时间。
     * 单班次排程模式下 {@code dayShifts} 通常仅含当前班一条配置。
     *
     * <p><b>路由</b>：见类 Javadoc 任务类型表；量试非开产日不走试制分支，落入收尾/普通路径。
     *
     * <p><b>开产首班特殊分支</b>（{@code dayShifts.size()==1}）：用 {@code formingOpeningShiftOrder}
     * 与 {@code lhOpeningShiftOrder} 判断是否为成型开产首班；关键产品首班直接返回空结果。
     *
     * @param task         精排用任务（多数字段由 TaskAllocation 反构）
     * @param machineCode  目标成型机台
     * @param context      排程上下文（产能映射、关键产品、精度计划等）
     * @param dayShifts    本班待精排的班次配置列表
     * @param scheduleDate 排程日
     * @return 本任务在本机台上的班次排产明细（可多条，通常单班一条）
     */
    public List<ShiftProductionResult> scheduleTaskToShifts(
            DailyEmbryoTask task,
            String machineCode,
            ScheduleContextVo context,
            List<CxShiftConfig> dayShifts,
            LocalDate scheduleDate) {

        List<ShiftProductionResult> results = new ArrayList<>();

        Integer endingExtraInventory = task.getEndingExtraInventory();
        if (endingExtraInventory == null || endingExtraInventory <= 0) {
            return results;
        }

        int tripCapacity = getTripCapacity(task.getStructureName(), task.getEmbryoCode(), context);

        // 调试日志
        log.info("scheduleTaskToShifts: embryo={}, material={}, 待排={}条/{}台, " +
                        "试制={}, 量试={}, 停产={}, 开产={}, 收尾={}, 续作={}, 库存={}h, dayShifts={}",
                task.getEmbryoCode(), task.getMaterialCode(),
                endingExtraInventory, task.getVulcanizeMachineCount(),
                task.getIsTrialTask(), task.getIsProductionTrial(), task.getIsClosingDayTask(),
                task.getIsOpeningDayTask(), task.getIsEndingTask(), task.getIsContinueTask(),
                task.getStockHours(), dayShifts != null ? dayShifts.size() : "null");

        // --- 5.3.7.0 任务类型路由（优先级：试制 > 量试开产 > 停产 > 开产 > 收尾 > 普通）---
        boolean isTrial = Boolean.TRUE.equals(task.getIsTrialTask());
        boolean isProductionTrial = Boolean.TRUE.equals(task.getIsProductionTrial());
        boolean isClosingDay = Boolean.TRUE.equals(task.getIsClosingDayTask());
        boolean isOpeningDay = Boolean.TRUE.equals(task.getIsOpeningDayTask());
        boolean isEnding = Boolean.TRUE.equals(task.getIsEndingTask()) || Boolean.TRUE.equals(task.getIsUrgentEnding());
        boolean isLastEndingBatch = Boolean.TRUE.equals(task.getIsLastEndingBatch());

        // 5.3.7.1 试制：早/中班、双数、不补整车
        if (isTrial) {
            return scheduleTrialTask(task, machineCode, context, dayShifts, scheduleDate, tripCapacity);
        }

        // 5.3.7.1.1 量试且开产日：班次限制同试制
        if (isProductionTrial && isOpeningDay) {
            return scheduleTrialTask(task, machineCode, context, dayShifts, scheduleDate, tripCapacity);
        }

        // 5.3.7.2 停产班：endingExtraInventory 已在 TaskGroupService 反推封顶
        if (isClosingDay) {
            return scheduleClosingTask(task, machineCode, context, dayShifts, scheduleDate, tripCapacity);
        }

        // 5.3.7.3 开产日：首班 6h / 关键产品过滤
        if (isOpeningDay) {
            // 单班模式：formingOpeningShiftOrder 判定成型开产首班
            if (dayShifts.size() == 1) {
                CxShiftConfig singleShift = dayShifts.get(0);
                int shiftOrder = singleShift.getDayShiftOrder() != null ? singleShift.getDayShiftOrder() : 1;
                Integer formingOpeningShiftOrder = task.getFormingOpeningShiftOrder();
                Integer lhOpeningShiftOrder = task.getLhOpeningShiftOrder();

                // 用 formingOpeningShiftOrder 判断是否为成型开产首班
                boolean isOpeningFirstShift = formingOpeningShiftOrder != null
                        && shiftOrder == formingOpeningShiftOrder
                        && formingOpeningShiftOrder < (lhOpeningShiftOrder != null ? lhOpeningShiftOrder : Integer.MAX_VALUE);

                if (isOpeningFirstShift) {
                    // 成型开产首班：关键产品不排产，非关键产品用6小时产能
                    if (isKeyProduct(task, context)) {
                        log.info("开产首班关键产品 {} 不排产，等待下一班次", task.getEmbryoCode());
                        return results;
                    }
                    return scheduleOpeningTask(task, machineCode, context, dayShifts, scheduleDate, tripCapacity);
                } else {
                    return scheduleNormalTask(task, machineCode, context, dayShifts, scheduleDate, tripCapacity);
                }
            }
            // 多班次模式（兼容旧逻辑）：走 scheduleOpeningTask
            return scheduleOpeningTask(task, machineCode, context, dayShifts, scheduleDate, tripCapacity);
        }

        // 5.3.7.4 收尾任务：收尾班次或之前安排，末班可非整车
        if (isEnding || isLastEndingBatch) {
            return scheduleEndingTask(task, machineCode, context, dayShifts, scheduleDate, tripCapacity);
        }

        // 5.3.7.5 普通任务：波浪放置（相邻班次车数差≤1）
        return scheduleNormalTask(task, machineCode, context, dayShifts, scheduleDate, tripCapacity);
    }

    // ==================== 5.3.7.1 试制/量试精排 ====================

    /**
     * 试制/量试班次分配。
     *
     * <p><b>约束</b>：
     * <ul>
     *   <li>跳过 {@link #SHIFT_NIGHT} 夜班</li>
     *   <li>总量与各班分配量均为偶数（不足则减 1）</li>
     *   <li>不补整车：车数 = shiftQty / tripCapacity 向下取整</li>
     *   <li>班末时间超限则按可用分钟折减产量</li>
     * </ul>
     *
     * <p>试制始终走本方法；量试仅开产日由 {@link #scheduleTaskToShifts} 路由至此。
     */
    private List<ShiftProductionResult> scheduleTrialTask(
            DailyEmbryoTask task,
            String machineCode,
            ScheduleContextVo context,
            List<CxShiftConfig> dayShifts,
            LocalDate scheduleDate,
            int tripCapacity) {

        List<ShiftProductionResult> results = new ArrayList<>();
        int totalQty = task.getEndingExtraInventory();

        // 确保双数
        if (totalQty % 2 != 0) {
            totalQty = totalQty - 1;
        }
        if (totalQty <= 0) {
            return results;
        }

        // 按均分方式分配到早班和中班（跳过夜班）
        // 先统计可排班次
        List<CxShiftConfig> trialShifts = new ArrayList<>();
        for (CxShiftConfig shiftConfig : dayShifts) {
            if (!SHIFT_NIGHT.equals(shiftConfig.getShiftCode())) {
                trialShifts.add(shiftConfig);
            }
        }
        int trialShiftCount = Math.max(trialShifts.size(), 1);

        // 按班次均分总量
        int[] shiftQuantities = distributeQuantityEvenly(totalQty, trialShiftCount);
        // 确保双数
        for (int i = 0; i < shiftQuantities.length; i++) {
            if (shiftQuantities[i] % 2 != 0) {
                shiftQuantities[i] = shiftQuantities[i] - 1;
            }
        }

        int remainingQty = totalQty;
        int shiftIndex = 0;

        for (CxShiftConfig shiftConfig : dayShifts) {
            String shiftCode = shiftConfig.getShiftCode();

            // 夜班不排试制
            if (SHIFT_NIGHT.equals(shiftCode)) {
                continue;
            }

            int shiftQty = shiftIndex < shiftQuantities.length ? shiftQuantities[shiftIndex] : 0;
            shiftIndex++;

            // 不能超过剩余量
            shiftQty = Math.min(shiftQty, remainingQty);
            if (shiftQty % 2 != 0) {
                shiftQty = shiftQty - 1;
            }
            if (shiftQty <= 0) {
                continue;
            }

            // 计算时间
            double hourlyCapacity = getMachineHourlyCapacity(machineCode, task.getMaterialCode(), task.getStructureName(), context);
            double productionHours = shiftQty / hourlyCapacity;
            LocalDateTime startTime = calculateStartTime(machineCode, shiftConfig, scheduleDate, context);
            LocalDateTime endTime = startTime.plusMinutes((long) (productionHours * 60));

            // 班次结束时间检查
            LocalDateTime shiftEndTime = calculateShiftEndTime(shiftConfig, scheduleDate);
            if (endTime.isAfter(shiftEndTime)) {
                long availableMinutes = Duration.between(startTime, shiftEndTime).toMinutes();
                int availableQty = (int) (availableMinutes * hourlyCapacity / 60);
                // 双数
                if (availableQty % 2 != 0) {
                    availableQty = availableQty - 1;
                }
                shiftQty = Math.max(0, availableQty);
                endTime = shiftEndTime;
            }

            if (shiftQty <= 0) {
                continue;
            }

            // 试制任务不补整车，cars按实际计算（不向上取整）
            int trialCars = tripCapacity > 0 ? shiftQty / tripCapacity : 1;
            ShiftProductionResult result = buildResult(machineCode, shiftConfig, task, shiftQty,
                    tripCapacity, trialCars,
                    startTime, endTime, true, false, task.getIsContinueTask());

            results.add(result);
            remainingQty -= shiftQty;
        }

        log.info("{}{} 班次排产完成：总计划 {}，已排 {}",
                Boolean.TRUE.equals(task.getIsProductionTrial()) ? "量试" : "试制",
                task.getEmbryoCode(), totalQty,
                totalQty - remainingQty);
        return results;
    }

    // ==================== 5.3.7.2 停产班精排 ====================

    /**
     * 停产班任务精排。
     *
     * <p>{@code endingExtraInventory} 已在 TaskGroupService 按停锅反推封顶，此处不再重算反推量。
     *
     * <p><b>分支</b>：
     * <ul>
     *   <li>{@code closingShiftOrder == 当前 dayShiftOrder}：停锅班，按实量下、不补整车</li>
     *   <li>停锅前班：按班产能扣停机/精度后整车取整</li>
     * </ul>
     */
    private List<ShiftProductionResult> scheduleClosingTask(
            DailyEmbryoTask task,
            String machineCode,
            ScheduleContextVo context,
            List<CxShiftConfig> dayShifts,
            LocalDate scheduleDate,
            int tripCapacity) {

        List<ShiftProductionResult> results = new ArrayList<>();
        int totalQty = task.getEndingExtraInventory();
        if (totalQty <= 0) {
            return results;
        }

        Integer closingShiftOrder = task.getClosingShiftOrder();
        int currentDayShiftOrder = dayShifts != null && dayShifts.size() == 1 && dayShifts.get(0).getDayShiftOrder() != null
                ? dayShifts.get(0).getDayShiftOrder() : 0;

        // 判断当前班次是否为停锅班次
        boolean isClosingShift = closingShiftOrder != null && currentDayShiftOrder == closingShiftOrder;

        double hourlyCapacity = getMachineHourlyCapacity(machineCode, task.getMaterialCode(), task.getStructureName(), context);

        for (CxShiftConfig shiftConfig : dayShifts) {
            if (totalQty <= 0) {
                break;
            }

            int shiftQty;
            if (isClosingShift) {
                // 停锅班次：不补整车，按实量下
                shiftQty = totalQty;
            } else {
                // 停锅班次之前的班次：整车取整
                int shiftHours = calculateShiftHours(shiftConfig);
                int shiftCapacity = (int)(shiftHours * hourlyCapacity);
                shiftCapacity -= calculateShiftShutdownDeduction(machineCode, shiftConfig, hourlyCapacity, context);
                shiftCapacity -= calculateShiftPrecisionDeduction(machineCode, shiftConfig, hourlyCapacity, context);
                shiftCapacity = Math.max(0, shiftCapacity);
                int cars = shiftCapacity / Math.max(tripCapacity, 1);
                shiftQty = Math.min(cars * tripCapacity, totalQty);
            }

            if (shiftQty <= 0) {
                continue;
            }

            // 计算时间
            LocalDateTime startTime = calculateStartTime(machineCode, shiftConfig, scheduleDate, context);
            double productionHours = shiftQty / hourlyCapacity;
            LocalDateTime endTime = startTime.plusMinutes((long) (productionHours * 60));

            LocalDateTime shiftEndTime = calculateShiftEndTime(shiftConfig, scheduleDate);
            if (endTime.isAfter(shiftEndTime)) {
                long availableMinutes = Duration.between(startTime, shiftEndTime).toMinutes();
                shiftQty = Math.max(0, (int) (availableMinutes * hourlyCapacity / 60));
                // 停锅班次不补整车；非停锅班次整车取整
                if (!isClosingShift && tripCapacity > 0) {
                    shiftQty = (shiftQty / tripCapacity) * tripCapacity;
                }
                endTime = shiftEndTime;
            }

            if (shiftQty <= 0) {
                continue;
            }

            int cars = isClosingShift
                    ? (tripCapacity > 0 ? (shiftQty + tripCapacity - 1) / tripCapacity : 1)
                    : (tripCapacity > 0 ? shiftQty / tripCapacity : 1);

            ShiftProductionResult result = buildResult(machineCode, shiftConfig, task, shiftQty,
                    tripCapacity, cars, startTime, endTime, false, false, task.getIsContinueTask());

            results.add(result);
            totalQty -= shiftQty;
        }

        log.info("停产任务 {} 班次排产完成: closingShiftOrder={}, isClosingShift={}, 已排={}",
                task.getEmbryoCode(), closingShiftOrder, isClosingShift,
                task.getEndingExtraInventory() - totalQty);
        return results;
    }

    // ==================== 5.3.7.3 开产班精排 ====================

    /**
     * 开产日任务精排。
     *
     * <p>{@code endingExtraInventory} 已在 TaskGroupService 做开产首班 6h 封顶；本方法负责按班切分与时间计算。
     *
     * <p><b>成型开产首班</b>（{@code formingOpeningShiftOrder} 匹配且早于硫化开产班）：
     * 关键产品不排；非关键产品用封顶后全量、不补整车。
     * 非首班按班产能整车取整。
     */
    private List<ShiftProductionResult> scheduleOpeningTask(
            DailyEmbryoTask task,
            String machineCode,
            ScheduleContextVo context,
            List<CxShiftConfig> dayShifts,
            LocalDate scheduleDate,
            int tripCapacity) {

        List<ShiftProductionResult> results = new ArrayList<>();
        int totalQty = task.getEndingExtraInventory();
        if (totalQty <= 0) {
            return results;
        }

        Integer formingOpeningShiftOrder = task.getFormingOpeningShiftOrder();
        Integer lhOpeningShiftOrder = task.getLhOpeningShiftOrder();
        int currentDayShiftOrder = dayShifts != null && dayShifts.size() == 1 && dayShifts.get(0).getDayShiftOrder() != null
                ? dayShifts.get(0).getDayShiftOrder() : 0;

        // 判断当前班次是否为成型开产首班
        boolean isOpeningFirstShift = formingOpeningShiftOrder != null && currentDayShiftOrder == formingOpeningShiftOrder
                && formingOpeningShiftOrder < (lhOpeningShiftOrder != null ? lhOpeningShiftOrder : Integer.MAX_VALUE);

        boolean isKeyProduct = isKeyProduct(task, context);

        // 开产首班且关键产品：不排
        if (isOpeningFirstShift && isKeyProduct) {
            log.info("开产首班关键产品 {} 不排产，等待下一班次", task.getEmbryoCode());
            return results;
        }

        double hourlyCapacity = getMachineHourlyCapacity(machineCode, task.getMaterialCode(), task.getStructureName(), context);

        for (CxShiftConfig shiftConfig : dayShifts) {
            if (totalQty <= 0) {
                break;
            }

            int shiftQty;
            if (isOpeningFirstShift) {
                // 成型开产首班：endingExtraInventory已是6h封顶值，不补整车
                shiftQty = totalQty;
            } else {
                // 非首班：按整车分配
                int shiftHours = calculateShiftHours(shiftConfig);
                int shiftCapacity = (int)(shiftHours * hourlyCapacity);
                shiftCapacity -= calculateShiftShutdownDeduction(machineCode, shiftConfig, hourlyCapacity, context);
                shiftCapacity -= calculateShiftPrecisionDeduction(machineCode, shiftConfig, hourlyCapacity, context);
                shiftCapacity = Math.max(0, shiftCapacity);
                int cars = shiftCapacity / Math.max(tripCapacity, 1);
                shiftQty = Math.min(cars * tripCapacity, totalQty);
            }

            if (shiftQty <= 0) {
                continue;
            }

            // 计算时间
            LocalDateTime startTime = calculateStartTime(machineCode, shiftConfig, scheduleDate, context);
            double productionHours = shiftQty / hourlyCapacity;
            LocalDateTime endTime = startTime.plusMinutes((long) (productionHours * 60));

            LocalDateTime shiftEndTime = calculateShiftEndTime(shiftConfig, scheduleDate);
            if (endTime.isAfter(shiftEndTime)) {
                long availableMinutes = Duration.between(startTime, shiftEndTime).toMinutes();
                shiftQty = Math.max(0, (int) (availableMinutes * hourlyCapacity / 60));
                if (!isOpeningFirstShift && tripCapacity > 0) {
                    // 非首班整车取整
                    shiftQty = (shiftQty / tripCapacity) * tripCapacity;
                }
                endTime = shiftEndTime;
            }

            if (shiftQty <= 0) {
                continue;
            }

            int cars = isOpeningFirstShift
                    ? (tripCapacity > 0 ? (shiftQty + tripCapacity - 1) / tripCapacity : 1)
                    : (tripCapacity > 0 ? shiftQty / tripCapacity : 1);

            ShiftProductionResult result = buildResult(machineCode, shiftConfig, task, shiftQty,
                    tripCapacity, cars, startTime, endTime, false, false, task.getIsContinueTask());

            results.add(result);
            totalQty -= shiftQty;
        }

        log.info("开产任务 {} 班次排产完成: formingOpeningShiftOrder={}, isOpeningFirstShift={}, 关键产品={}, 已排={}",
                task.getEmbryoCode(), formingOpeningShiftOrder, isOpeningFirstShift, isKeyProduct,
                task.getEndingExtraInventory() - totalQty);
        return results;
    }

    /**
     * 开产首班 6 小时产量估算（条）。
     *
     * <p>公式：单胎成型秒数 = 86400 / (配比 × 日硫化量)；产量 = 6×3600 / 单胎秒数（向下取整）。
     * 配比/日硫化量缺失时回退 {@code OPENING_FIRST_SHIFT_HOURS × getMachineHourlyCapacity}。
     *
     * <p>注：当前精排主路径以 TaskGroupService 写入的 endingExtraInventory 为准；本方法供辅助计算/遗留调用。
     */
    private int calculateOpeningFirstShiftCapacity(
            DailyEmbryoTask task,
            String machineCode,
            ScheduleContextVo context) {

        // 1. 获取日硫化量
        Integer dailyLhCapacity = null;
        Map<String, MonthPlanProductLhCapacityVo> lhCapacityMap = context.getMaterialLhCapacityMap();
        if (lhCapacityMap != null && task.getMaterialCode() != null) {
            MonthPlanProductLhCapacityVo capacityVo = lhCapacityMap.get(task.getMaterialCode());
            if (capacityVo != null) {
                // 按日硫化量计算模式（DAY_VULCANIZATION_MODE）取值，与计划量计算口径保持一致
                dailyLhCapacity = capacityVo.getDayVulcanizationQty();
            }
        }

        if (dailyLhCapacity == null || dailyLhCapacity <= 0) {
            log.warn("开产首班产能计算：无法获取物料 {} 的日硫化量，使用默认值", task.getEmbryoCode());
            return (int)(OPENING_FIRST_SHIFT_HOURS * getMachineHourlyCapacity(machineCode, task.getMaterialCode(), task.getStructureName(), context));
        }

        // 2. 获取配比（机型+结构 → 配比）
        int ratio = 1;
        if (context.getStructureLhRatioMap() != null && task.getStructureName() != null) {
            MdmStructureLhRatio lhRatio = context.getStructureLhRatioMap().get(task.getStructureName());
            if (lhRatio != null && lhRatio.getLhMachineMaxQty() != null && lhRatio.getLhMachineMaxQty() > 0) {
                ratio = lhRatio.getLhMachineMaxQty();
            }
        }

        // 3. 成型一条胎时间(s) = 24×3600 / (配比 × 日硫化量)
        BigDecimal formingTimePerTire = BigDecimal.valueOf(ScheduleConstants.SECONDS_PER_DAY)
                .divide(BigDecimal.valueOf((long) ratio * dailyLhCapacity), 2, RoundingMode.HALF_UP);

        // 4. 首班6小时产量 = 6×3600 / 成型一条胎时间(s)
        int firstShiftCapacity = BigDecimal.valueOf(OPENING_FIRST_SHIFT_HOURS * ScheduleConstants.SECONDS_PER_HOUR)
                .divide(formingTimePerTire, 0, RoundingMode.FLOOR)
                .intValue();

        log.debug("开产首班产能计算：日硫化量={}, 配比={}, 成型单条时间={}s, 首班产量={}",
                dailyLhCapacity, ratio, formingTimePerTire, firstShiftCapacity);

        return Math.max(firstShiftCapacity, 0);
    }

    // ==================== 5.3.7.4 收尾精排 ====================

    /**
     * 收尾任务精排：波浪分车 + 硫化收尾班截止约束。
     *
     * <p>通过 {@link #getVulcanizeEndingShift} 得到硫化最后有计划量的班次编码，
     * 将 {@link #calculateWaveCars} 中晚于该班的车数前移到截止班。
     *
     * <p>最后一个有产量的班次使用 {@code remainingQty} 实量（可非整车）。
     * {@code isEndingTask} 标记用 {@code task.isEndingTask}（物料收尾耗尽），
     * 非「最后一个有量班次」；{@code isLastEndingBatch} 在末班且任务标记时写入结果。
     */
    private List<ShiftProductionResult> scheduleEndingTask(
            DailyEmbryoTask task,
            String machineCode,
            ScheduleContextVo context,
            List<CxShiftConfig> dayShifts,
            LocalDate scheduleDate,
            int tripCapacity) {

        List<ShiftProductionResult> results = new ArrayList<>();
        int totalQty = task.getEndingExtraInventory();
        if (totalQty <= 0) {
            return results;
        }

        // 获取硫化的收尾班次索引
        String vulcanizeEndingShift = getVulcanizeEndingShift(task, context);
        int maxShiftIndex = getShiftIndex(vulcanizeEndingShift, dayShifts);
        if (maxShiftIndex < 0) {
            maxShiftIndex = dayShifts.size() - 1;
        }

        // 计算波浪分配
        int requiredCars = tripCapacity > 0 ? (totalQty + tripCapacity - 1) / tripCapacity : 1;
        int[] shiftCars = calculateWaveCars(requiredCars, dayShifts);
        log.info("【波浪分配】胎胚={}, 待排={}条, 每车={}条, 需={}车, 各班分配={}",
                task.getEmbryoCode(), totalQty, tripCapacity, requiredCars, Arrays.toString(shiftCars));

        // 收尾班次约束：只能在 maxShiftIndex 或之前的班次安排
        for (int i = maxShiftIndex + 1; i < shiftCars.length; i++) {
            if (shiftCars[i] > 0) {
                shiftCars[maxShiftIndex] += shiftCars[i];
                shiftCars[i] = 0;
            }
        }

        double hourlyCapacity = getMachineHourlyCapacity(machineCode, task.getMaterialCode(), task.getStructureName(), context);
        int remainingQty = totalQty;

        for (int i = 0; i < dayShifts.size() && remainingQty > 0; i++) {
            if (shiftCars[i] <= 0) {
                continue;
            }

            CxShiftConfig shiftConfig = dayShifts.get(i);

            // 计算该班次分配的量
            int shiftQty = shiftCars[i] * tripCapacity;
            boolean isLastProductive = isLastShiftWithQty(i, shiftCars);

            if (isLastProductive) {
                // 最后一个有量的班次：使用剩余量，可以不是整车
                shiftQty = remainingQty;
            }

            shiftQty = Math.min(shiftQty, remainingQty);
            if (shiftQty <= 0) {
                continue;
            }

            // 计算时间
            LocalDateTime startTime = calculateStartTime(machineCode, shiftConfig, scheduleDate, context);
            double productionHours = shiftQty / hourlyCapacity;
            LocalDateTime endTime = startTime.plusMinutes((long) (productionHours * 60));

            LocalDateTime shiftEndTime = calculateShiftEndTime(shiftConfig, scheduleDate);
            if (endTime.isAfter(shiftEndTime)) {
                long availableMinutes = Duration.between(startTime, shiftEndTime).toMinutes();
                shiftQty = Math.max(0, (int) (availableMinutes * hourlyCapacity / 60));
                endTime = shiftEndTime;
            }

            if (shiftQty <= 0) {
                continue;
            }

            int cars = tripCapacity > 0 ? (shiftQty + tripCapacity - 1) / tripCapacity : 1;

            // isEndingTask：物料级收尾（成型余量耗尽）；isLastEndingBatch：末班+任务标记
            boolean isMaterialEnding = Boolean.TRUE.equals(task.getIsEndingTask());
            ShiftProductionResult result = buildResult(machineCode, shiftConfig, task, shiftQty,
                    tripCapacity, cars, startTime, endTime, false, isMaterialEnding, task.getIsContinueTask());
            // buildResult会从task拷贝isLastEndingBatch，此处覆盖为仅最后一个有产量的班次
            result.setIsLastEndingBatch(isLastProductive && Boolean.TRUE.equals(task.getIsLastEndingBatch()));

            results.add(result);
            remainingQty -= shiftQty;
        }

        return results;
    }

    // ==================== 5.3.7.5 普通任务精排 ====================

    /**
     * 普通任务精排：{@link #calculateWaveCars} 波浪分车，相邻班次车数差不超过 1。
     *
     * <p>班末时间不足时按可用产能折减条数，不强制补整车；未排完量打 warn。
     */
    private List<ShiftProductionResult> scheduleNormalTask(
            DailyEmbryoTask task,
            String machineCode,
            ScheduleContextVo context,
            List<CxShiftConfig> dayShifts,
            LocalDate scheduleDate,
            int tripCapacity) {

        List<ShiftProductionResult> results = new ArrayList<>();
        int totalQty = task.getEndingExtraInventory();
        if (totalQty <= 0) {
            return results;
        }

        int requiredCars = tripCapacity > 0 ? (totalQty + tripCapacity - 1) / tripCapacity : 1;
        int[] shiftCars = calculateWaveCars(requiredCars, dayShifts);
        log.info("【波浪分配】胎胚={}, 待排={}条, 每车={}条, 需={}车, 各班分配={}",
                task.getEmbryoCode(), totalQty, tripCapacity, requiredCars, Arrays.toString(shiftCars));

        double hourlyCapacity = getMachineHourlyCapacity(machineCode, task.getMaterialCode(), task.getStructureName(), context);
        int remainingCars = requiredCars;
        int remainingQty = totalQty;

        for (int i = 0; i < dayShifts.size() && remainingCars > 0 && remainingQty > 0; i++) {
            CxShiftConfig shiftConfig = dayShifts.get(i);
            int carsForShift = shiftCars[i];

            if (carsForShift <= 0) {
                continue;
            }

            carsForShift = Math.min(carsForShift, remainingCars);
            int batchQty = Math.min(carsForShift * tripCapacity, remainingQty);

            // 计算时间
            LocalDateTime startTime = calculateStartTime(machineCode, shiftConfig, scheduleDate, context);
            double productionHours = batchQty / hourlyCapacity;
            LocalDateTime endTime = startTime.plusMinutes((long) (productionHours * 60));

            // 班次结束时间检查
            LocalDateTime shiftEndTime = calculateShiftEndTime(shiftConfig, scheduleDate);
            if (shiftEndTime != null && endTime.isAfter(shiftEndTime)) {
                long availableMinutes = Duration.between(startTime, shiftEndTime).toMinutes();
                int availableQty = (int) (availableMinutes * hourlyCapacity / 60);
                // 产能不足时按实际可生产量下，不强制整车取整
                batchQty = Math.min(Math.max(0, availableQty), remainingQty);
                carsForShift = tripCapacity > 0 ? (batchQty + tripCapacity - 1) / tripCapacity : (batchQty > 0 ? 1 : 0);
                endTime = shiftEndTime;
            }

            if (batchQty <= 0) {
                log.info("scheduleNormalTask: 跳过 batchQty=0, carsForShift={}, remainingCars={}", carsForShift, remainingCars);
                continue;
            }

            // 根据 batchQty 重新计算实际车数（不足一车算1车）
            carsForShift = tripCapacity > 0 ? (batchQty + tripCapacity - 1) / tripCapacity : 1;

            log.info("【硫化排产完成】胎胚={}, 班次={}, 产量={}条, 车数={}",
                    task.getEmbryoCode(), shiftConfig.getShiftCode(), batchQty, carsForShift);
            ShiftProductionResult result = buildResult(machineCode, shiftConfig, task, batchQty,
                    tripCapacity, carsForShift, startTime, endTime, false, false, task.getIsContinueTask());

            results.add(result);
            remainingCars -= carsForShift;
            remainingQty -= batchQty;
        }

        if (remainingQty > 0) {
            log.warn("普通任务 {} 还有 {} 条未排产，产能不足", task.getEmbryoCode(), remainingQty);
        }

        return results;
    }

    // ==================== 波浪/均分算法 ====================

    /**
     * 将总车数波浪分配到各班次：基础均分 + 余数从两端向中间对称 +1，保证相邻班差≤1 车。
     *
     * @param requiredCars 总车数
     * @param dayShifts    班次列表（顺序即分配下标）
     * @return 与 dayShifts 等长的每班车数数组
     */
    private int[] calculateWaveCars(int requiredCars, List<CxShiftConfig> dayShifts) {

        int shiftCount = dayShifts.size();
        int[] shiftCars = new int[shiftCount];

        if (requiredCars <= 0) {
            return shiftCars;
        }

        // 均分车数：每个班次车数相差不超过1
        int base = requiredCars / shiftCount;
        int remainder = requiredCars % shiftCount;

        // 全部初始化为基础车数
        for (int i = 0; i < shiftCount; i++) {
            shiftCars[i] = base;
        }

        // 将余数对称分配：从外向内配对，每对两侧各+1；奇数余数给中间班次
        int left = 0;
        int right = shiftCount - 1;
        while (remainder > 0 && left <= right) {
            if (left == right) {
                // 中间班次，+1
                shiftCars[left]++;
                remainder--;
            } else if (remainder >= 2) {
                // 两侧对称各+1
                shiftCars[left]++;
                shiftCars[right]++;
                remainder -= 2;
            } else {
                // remainder == 1，给中间
                shiftCars[shiftCount / 2]++;
                remainder--;
            }
            left++;
            right--;
        }

        log.debug("波浪分配：需要{}车，分配结果：{}", requiredCars, Arrays.toString(shiftCars));

        return shiftCars;
    }

    /**
     * 将总条数对称均分到各班次（试制路径用，逻辑同 calculateWaveCars 但单位为条）。
     */
    private int[] distributeQuantityEvenly(int totalQuantity, int shiftCount) {
        int[] quantities = new int[shiftCount];
        if (totalQuantity <= 0 || shiftCount <= 0) {
            return quantities;
        }

        int base = totalQuantity / shiftCount;
        int remainder = totalQuantity % shiftCount;

        for (int i = 0; i < shiftCount; i++) {
            quantities[i] = base;
        }

        // 对称分配余数
        int left = 0;
        int right = shiftCount - 1;
        while (remainder > 0 && left <= right) {
            if (left == right) {
                quantities[left]++;
                remainder--;
            } else if (remainder >= 2) {
                quantities[left]++;
                quantities[right]++;
                remainder -= 2;
            } else {
                quantities[shiftCount / 2]++;
                remainder--;
            }
            left++;
            right--;
        }

        return quantities;
    }

    // ==================== 硫化排程关联（收尾班判定） ====================

    /** 按 lhId 在 context.lhScheduleResults 中查找硫化任务行 */
    private LhScheduleResult findLhScheduleResult(Long lhId, ScheduleContextVo context) {
        if (lhId == null || context.getLhScheduleResults() == null) {
            return null;
        }
        for (LhScheduleResult result : context.getLhScheduleResults()) {
            if (lhId.equals(result.getId())) {
                return result;
            }
        }
        return null;
    }

    /** 在 shiftCars 数组中，currentIndex 之后是否再无正车数 */
    private boolean isLastShiftWithQty(int currentIndex, int[] shiftCars) {
        for (int i = currentIndex + 1; i < shiftCars.length; i++) {
            if (shiftCars[i] > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * 推断硫化侧「最后有计划量的班次」编码。
     *
     * <p>从 LhScheduleResult CLASS8→CLASS1 倒序找首个 planQty&gt;0，映射为 SHIFT_NIGHT/DAY/AFTERNOON。
     */
    private String getVulcanizeEndingShift(DailyEmbryoTask task,
                                           ScheduleContextVo context) {
        LhScheduleResult lhResult = findLhScheduleResult(task.getLhId(), context);
        if (lhResult == null) {
            return SHIFT_DAY; // 默认早班
        }

        // 从后往前找最后一个有计划量的班次
        for (int i = 8; i >= 1; i--) {
            Integer planQty = productionCalculator.getClassPlanQtyByIndex(lhResult, i);
            if (planQty != null && planQty > 0) {
                // class index → shift code 映射
                // 一般: 1-2=夜班, 3-4=早班, 5-6=中班, 7-8=次日班次
                if (i <= 2) return SHIFT_NIGHT;
                if (i <= 4) return SHIFT_DAY;
                if (i <= 6) return SHIFT_AFTERNOON;
                return SHIFT_AFTERNOON;
            }
        }

        return SHIFT_DAY;
    }

    /** shiftCode 在 dayShifts 中的下标，未找到返回 -1 */
    private int getShiftIndex(String shiftCode, List<CxShiftConfig> dayShifts) {
        for (int i = 0; i < dayShifts.size(); i++) {
            if (dayShifts.get(i).getShiftCode().equals(shiftCode)) {
                return i;
            }
        }
        return -1;
    }

    // ==================== 班产能扣减与时间窗口 ====================

    /** 班次有效生产小时数（配置 shiftHours 或默认 8） */
    private int calculateShiftHours(CxShiftConfig shiftConfig) {
        Integer shiftHours = shiftConfig.getShiftHours();
        if (shiftHours != null && shiftHours > 0) {
            return shiftHours;
        }
        return DEFAULT_SHIFT_HOURS;
    }

    /**
     * 设备计划停机扣减条数（占位：遍历 devicePlanShuts，详细时段扣减待实现）。
     */
    private int calculateShiftShutdownDeduction(
            String machineCode,
            CxShiftConfig shiftConfig,
            double hourlyCapacity,
            ScheduleContextVo context) {

        if (context.getDevicePlanShuts() == null || context.getDevicePlanShuts().isEmpty()) {
            return 0;
        }

        int totalDeduction = 0;

        for (MdmDevicePlanShut shutdown : context.getDevicePlanShuts()) {
            if (!machineCode.equals(shutdown.getMachineCode())) {
                continue;
            }
            // TODO: 实现详细的班次停机时间扣减计算
        }

        return totalDeduction;
    }

    /**
     * 精度计划未完成时扣减本班产能（条）。
     *
     * <p>机台匹配 precisionPlans 且 completionStatus≠1；按 precisionCycle 估时长，至少扣 4 小时产能。
     * 全局 precisionPlanApplied 后不再扣减。
     */
    private int calculateShiftPrecisionDeduction(
            String machineCode,
            CxShiftConfig shiftConfig,
            double hourlyCapacity,
            ScheduleContextVo context) {

        if (context.isPrecisionPlanApplied()) {
            return 0;
        }

        if (context.getPrecisionPlans() == null || context.getPrecisionPlans().isEmpty()) {
            return 0;
        }

        for (CxPrecisionPlan plan : context.getPrecisionPlans()) {
            if (machineCode.equals(plan.getMachineCode())) {
                // 只扣减未完成的精度计划（completionStatus=0）
                if ("1".equals(plan.getCompletionStatus())) {
                    continue;
                }

                // 根据 precisionCycle 计算扣减时长
                // precisionCycle: 15=15分钟, 60=60分钟
                int precisionMinutes = 60; // 默认60分钟
                if ("15".equals(plan.getPrecisionCycle())) {
                    precisionMinutes = 15;
                } else if ("60".equals(plan.getPrecisionCycle())) {
                    precisionMinutes = 60;
                }

                // 精度时长(小时) × 机台小时产能
                int precisionHours = (int) Math.ceil(precisionMinutes / 60.0);
                // 至少扣减4小时（精度校验标准时长）
                if (precisionHours < 4) {
                    precisionHours = 4;
                }
                return (int)(precisionHours * hourlyCapacity);
            }
        }

        return 0;
    }

    /**
     * 计划生产开始时间 = 班初时刻（跨天班日期 -1）+ 机台准备分钟。
     */
    private LocalDateTime calculateStartTime(
            String machineCode,
            CxShiftConfig shiftConfig,
            LocalDate scheduleDate,
            ScheduleContextVo context) {

        LocalTime shiftStart = shiftConfig.getShiftStartTime();
        LocalDate startDate = scheduleDate;
        if (shiftConfig.getIsCrossDay() != null && shiftConfig.getIsCrossDay() == 1) {
            startDate = scheduleDate.minusDays(1);
        }
        LocalDateTime startTime = LocalDateTime.of(startDate, shiftStart);
        startTime = startTime.plusMinutes(getMachinePrepareMinutes(machineCode, context));

        return startTime;
    }

    /**
     * 班次结束时刻；IS_CROSS_DAY=1 时结束日期 +1 天。
     */
    private LocalDateTime calculateShiftEndTime(CxShiftConfig shiftConfig, LocalDate scheduleDate) {
        LocalTime shiftEnd = shiftConfig.getShiftEndTime();
        if (shiftEnd == null) {
            log.warn("calculateShiftEndTime: shiftEnd is null, shiftCode={}", shiftConfig.getShiftCode());
            return null;
        }
        LocalDateTime endTime = LocalDateTime.of(scheduleDate, shiftEnd);

        // 跨天班次（isCrossDay=1）结束时间加1天
        if (shiftConfig.getIsCrossDay() != null && shiftConfig.getIsCrossDay() == 1) {
            endTime = endTime.plusDays(1);
        }

        return endTime;
    }

    /**
     * 计算机台小时产能（条/小时）— 可被外部调用。
     *
     * <p>链：materialLhCapacityMap[materialCode].dayVulcanizationQty →
     * structureLhRatioMap[machineType|structure].lhMachineMaxQty →
     * 单胎秒数 = 86400/(配比×日硫化量) → 3600/单胎秒数。
     */
    public double getMachineHourlyCapacity(String machineCode, String materialCode,
                                           String structureName, ScheduleContextVo context) {
        // 1. 获取日硫化量（materialLhCapacityMap 的 key 是 materialCode，不是 embryoCode）
        Integer dailyLhCapacity = null;
        Map<String, MonthPlanProductLhCapacityVo> lhCapacityMap = context.getMaterialLhCapacityMap();
        if (lhCapacityMap != null && materialCode != null) {
            MonthPlanProductLhCapacityVo capacityVo = lhCapacityMap.get(materialCode);
            if (capacityVo != null) {
                // 按日硫化量计算模式（DAY_VULCANIZATION_MODE）取值，与计划量计算口径保持一致
                dailyLhCapacity = capacityVo.getDayVulcanizationQty();
            }
        }

        // 2. 获取配比（结构+机型 → lhMachineMaxQty）
        int ratio = 1;
        if (context.getStructureLhRatioMap() != null && structureName != null && machineCode != null) {
            // 先通过机台编码查机型，再以 机型+结构 组合 key 查配比
            Map<String, String> machineTypeCodeMap = context.getMachineTypeCodeMap();
            String machineTypeCode = machineTypeCodeMap != null ? machineTypeCodeMap.get(machineCode) : null;
            if (machineTypeCode != null) {
                MdmStructureLhRatio lhRatio = context.getStructureLhRatioMap().get(machineTypeCode + "|" + structureName);
                if (lhRatio != null && lhRatio.getLhMachineMaxQty() != null && lhRatio.getLhMachineMaxQty() > 0) {
                    ratio = lhRatio.getLhMachineMaxQty();
                }
            }
        }

        if (dailyLhCapacity != null && dailyLhCapacity > 0) {
            // 3. 成型一条胎的时间(s) = 86400 / (配比 × 日硫化量)
            BigDecimal timePerTire = BigDecimal.valueOf(ScheduleConstants.SECONDS_PER_DAY)
                    .divide(BigDecimal.valueOf((long) ratio * dailyLhCapacity), 2, RoundingMode.HALF_UP);

            // 4. 小时产能 = 3600 / 成型一条胎的时间(s)，保留2位小数避免整数截断导致产能低估
            if (timePerTire.compareTo(BigDecimal.ZERO) > 0) {
                double hourlyCapacity = BigDecimal.valueOf(ScheduleConstants.SECONDS_PER_HOUR)
                        .divide(timePerTire, 2, RoundingMode.HALF_UP)
                        .doubleValue();
                log.info("机台 {} 物料 {} 小时产能计算: 日硫化量={}, 配比={}, 单条耗时={}s, 产能={}条/h",
                        machineCode, materialCode, dailyLhCapacity, ratio, timePerTire, hourlyCapacity);
                return hourlyCapacity;
            }
        }

        log.warn("无法计算机台 {} 物料 {} 的小时产能(日硫化量={}, 配比={})，使用默认值 {}",
                machineCode, materialCode, dailyLhCapacity, ratio, DEFAULT_HOURLY_CAPACITY);
        return (double) DEFAULT_HOURLY_CAPACITY;
    }

    /** 结构+胎胚匹配 CxStructureTreadConfig.treadCount，否则 context 默认或 {@link ScheduleConstants#DEFAULT_TRIP_CAPACITY} */
    private int getTripCapacity(String structureCode, String embryoCode, ScheduleContextVo context) {
        if (context.getStructureShiftCapacities() != null) {
            for (CxStructureTreadConfig capacity : context.getStructureShiftCapacities()) {
                if (capacity.getStructureCode() != null
                        && capacity.getStructureCode().equals(structureCode)
                        && (embryoCode == null || embryoCode.equals(capacity.getEmbryoCode()))) {
                    if (capacity.getTreadCount() != null && capacity.getTreadCount() > 0) {
                        return capacity.getTreadCount();
                    }
                }
            }
        }
        return context.getDefaultTripCapacity() != null ? context.getDefaultTripCapacity() : ScheduleConstants.DEFAULT_TRIP_CAPACITY;
    }

    /** 机台班初准备时间（分钟），当前为全局常量 */
    private int getMachinePrepareMinutes(String machineCode, ScheduleContextVo context) {
        return DEFAULT_MACHINE_PREPARE_MINUTES;
    }

    /** 胎胚或物料编码命中 context.keyProductCodes 视为关键产品（开产首班不排） */
    private boolean isKeyProduct(DailyEmbryoTask task, ScheduleContextVo context) {
        if (task == null || context == null) {
            return false;
        }
        Set<String> keyProductCodes = context.getKeyProductCodes();
        if (keyProductCodes == null || keyProductCodes.isEmpty()) {
            return false;
        }
        return keyProductCodes.contains(task.getEmbryoCode())
                || keyProductCodes.contains(task.getMaterialCode());
    }

    // ==================== 结果构建 ====================

    /**
     * 组装单条 {@link ShiftProductionResult}，并挂载 sourceTask 供下游取 vulcanizeMachineCount。
     */
    private ShiftProductionResult buildResult(
            String machineCode,
            CxShiftConfig shiftConfig,
            DailyEmbryoTask task,
            int quantity,
            int tripCapacity,
            int cars,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Boolean isTrialTask,
            Boolean isEndingTask,
            Boolean isContinueTask) {

        // 从班次配置获取班次序号
        int sequence = shiftConfig.getDayShiftOrder() != null ? shiftConfig.getDayShiftOrder() : 1;

        ShiftProductionResult result = new ShiftProductionResult();
        result.setMachineCode(machineCode);
        result.setShiftCode(shiftConfig.getShiftCode());
        result.setShiftName(shiftConfig.getShiftName());
        result.setEmbryoCode(task.getEmbryoCode());
        result.setMaterialCode(task.getMaterialCode());
        result.setMaterialDesc(task.getMaterialDesc());
        result.setMainMaterialDesc(task.getMainMaterialDesc());
        result.setStructureName(task.getStructureName());
        result.setQuantity(quantity);
        result.setTripNo(String.valueOf(sequence));
        result.setTripCapacity(tripCapacity);
        result.setStockHours(task.getStockHours());
        result.setSequence(sequence);
        result.setCarsForShift(cars);
        result.setPlanStartTime(startTime);
        result.setPlanEndTime(endTime);
        result.setIsTrialTask(isTrialTask);
        result.setIsEndingTask(isEndingTask);
        result.setIsContinueTask(isContinueTask);
        result.setIsLastEndingBatch(task.getIsLastEndingBatch());
        result.setSourceTask(task);
        result.setIsEndProduction(task.getIsEndProduction());
        result.setProductStatus(task.getProductStatus());
        result.setConstructionStage(task.getConstructionStage());

        return result;
    }
}
