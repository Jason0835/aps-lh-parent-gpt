package com.zlt.aps.tq.engine.strategy;

import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.tq.api.domain.entity.TqStockShiftConfig;
import com.zlt.aps.tq.engine.context.TqScheduleContext;
import com.zlt.aps.tq.engine.domain.TqRuleTrace;
import com.zlt.aps.tq.engine.enums.TqScheduleRuleCodeEnum;
import com.zlt.aps.tq.engine.enums.TqScheduleRuleResultEnum;
import com.zlt.aps.tq.engine.vo.TqMonthSurplusVo;
import com.zlt.aps.tq.engine.vo.TqScheduleParams;
import com.zlt.aps.tq.engine.vo.TqScheduleResultVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 胎圈需求量/计划量计算公共工具。
 *
 * <p>从原 {@code TqDemandCalcHandler} 抽取的公共辅助方法，供：</p>
 * <ul>
 *   <li>{@link TqSupplyTimeByStockStrategy}、{@link TqSupplyTimeByShiftStrategy}（供应时长策略）</li>
 *   <li>{@link TqDefaultDemandQtyStrategy}（需求量策略）</li>
 *   <li>{@link TqDefaultPlanQtyStrategy}（计划量策略）</li>
 *   <li>{@code TqStockPredictHandler}、{@code TqDemandQtyCalcHandler}、{@code TqPlanQtyCalcHandler}（拆分后的 Handler）</li>
 * </ul>
 * 共享调用，保证算法等价性。
 *
 * <p>本工具类为静态方法集合，所有方法均为纯算法实现，不持有状态。</p>
 *
 * @author APS
 */
public final class TqDemandCalcHelper {

    /** 日志分隔符（保持与原 Handler 一致） */
    public static final String DIVISION = "\r\n---------------------------------------------------\r\n";

    private TqDemandCalcHelper() {
        // 工具类禁止实例化
    }

    // ==================== 班次计划量读写 ====================

    /**
     * 按班次索引设置对应的胎圈计划量。
     *
     * @param scheduleVo 排程结果 VO
     * @param classNum   班次（1-6）
     * @param qty        计划量
     */
    public static void setClassPlanQtyByIndex(TqScheduleResultVo scheduleVo, int classNum, double qty) {
        switch (classNum) {
            case 1: scheduleVo.setClass1PlanQty(qty); break;
            case 2: scheduleVo.setClass2PlanQty(qty); break;
            case 3: scheduleVo.setClass3PlanQty(qty); break;
            case 4: scheduleVo.setClass4PlanQty(qty); break;
            case 5: scheduleVo.setClass5PlanQty(qty); break;
            case 6: scheduleVo.setClass6PlanQty(qty); break;
            default: break;
        }
    }

    /**
     * 按班次索引获取对应的胎圈计划量。
     *
     * @param scheduleVo 排程结果 VO
     * @param classNum   班次（1-6）
     * @return 计划量（null 视为 0）
     */
    public static double getClassPlanQtyByIndex(TqScheduleResultVo scheduleVo, int classNum) {
        switch (classNum) {
            case 1: return scheduleVo.getClass1PlanQty() == null ? 0D : scheduleVo.getClass1PlanQty();
            case 2: return scheduleVo.getClass2PlanQty() == null ? 0D : scheduleVo.getClass2PlanQty();
            case 3: return scheduleVo.getClass3PlanQty() == null ? 0D : scheduleVo.getClass3PlanQty();
            case 4: return scheduleVo.getClass4PlanQty() == null ? 0D : scheduleVo.getClass4PlanQty();
            case 5: return scheduleVo.getClass5PlanQty() == null ? 0D : scheduleVo.getClass5PlanQty();
            case 6: return scheduleVo.getClass6PlanQty() == null ? 0D : scheduleVo.getClass6PlanQty();
            default: return 0D;
        }
    }

    // ==================== 成型消耗与需求系数 ====================

    /**
     * 成型消耗量乘以需求系数。
     *
     * @param cxPlan      成型计划量（null 视为 0）
     * @param coefficient 需求系数
     * @return 胎圈消耗量
     */
    public static double mulCxPlan(Integer cxPlan, double coefficient) {
        double plan = cxPlan == null ? 0 : cxPlan;
        return BigDecimalUtil.mul(plan, coefficient);
    }

    // ==================== 备库班数配置匹配 ====================

    /**
     * 根据成型机台数匹配胎圈备库班数配置规则，得到需备库班数 N。
     *
     * <p>规则匹配逻辑：遍历配置列表，按 MACHINE_COUNT 和 MACHINE_RANGE（LT/LE/EQ/GE/GT）判断是否命中。
     * 配置列表已按 MACHINE_COUNT 升序排列，匹配到第一条即返回。</p>
     *
     * @param machineCount 成型机台数（null 则不匹配）
     * @param configList   备库班数配置列表
     * @return 命中的备库班数；若无配置或机台数为空则返回 null
     */
    public static Integer matchBackupShiftCount(Integer machineCount, List<TqStockShiftConfig> configList) {
        if (machineCount == null || machineCount <= 0 || CollectionUtils.isEmpty(configList)) {
            return null;
        }
        for (TqStockShiftConfig config : configList) {
            if (matchMachineRange(machineCount, config.getMachineRange(), config.getMachineCount())) {
                return config.getShiftCount();
            }
        }
        return null;
    }

    /**
     * 判断成型机台数是否命中配置规则的机台范围。
     *
     * <p>支持 5 种范围操作符：LT(小于)、LE(小于等于)、EQ(等于)、GE(大于等于)、GT(大于)</p>
     *
     * @param machineCount 实际机台数
     * @param machineRange 范围操作符
     * @param configCount  配置的机台数
     * @return true=命中 false=不命中
     */
    public static boolean matchMachineRange(Integer machineCount, String machineRange, Integer configCount) {
        if (machineCount == null || configCount == null) {
            return false;
        }
        if (StringUtils.isBlank(machineRange)) {
            return false;
        }
        switch (machineRange.toUpperCase()) {
            case "LT": return machineCount < configCount;
            case "LE": return machineCount <= configCount;
            case "EQ": return machineCount.equals(configCount);
            case "GE": return machineCount >= configCount;
            case "GT": return machineCount > configCount;
            default: return false;
        }
    }

    /**
     * 判断是否触发胎圈备库班数配置逻辑。
     *
     * <p>触发条件：当前可用库存不足以支撑成型 1 个班的消耗量（参数 SYS1101001 配置的阈值）</p>
     *
     * @param availableStock 当前可用库存
     * @param params         工序参数
     * @return true=触发备库 false=不触发
     */
    public static boolean shouldTriggerBackup(double availableStock, TqScheduleParams params) {
        Double classStockReference = params.getClassStockReference();
        if (classStockReference == null || classStockReference <= 0) {
            return false;
        }
        return availableStock < classStockReference;
    }

    // ==================== 备库总量计算 ====================

    /**
     * 计算备库 N 个班的总消耗量。
     *
     * <p>从触发班次对应的成型班次开始连续取 N 个班的胎圈消耗量 × 需求系数之和。</p>
     * <p>对应关系：胎圈 N 班对应成型 (N+2) 班，因此触发胎圈 X 班时，备库从成型 (X+2) 班开始连续 N 个班。</p>
     * <p>若备库班数超出成型计划（成型只有 8 班），剩余班次使用"最后 3 个非停产成型班次"的平均消耗量估算。</p>
     *
     * @param triggerClass     触发备库的胎圈班次（1-5）
     * @param backupShiftCount 需备库的班数 N
     * @param scheduleVo      排程结果 VO（含成型 1-8 班消耗量）
     * @param coefficient     需求系数
     * @return 备库 N 个班的总消耗量
     */
    public static double calculateBackupTotalQty(int triggerClass, int backupShiftCount,
                                                  TqScheduleResultVo scheduleVo, double coefficient) {
        // 成型 8 个班的胎圈消耗量（按成型班次顺序）
        double[] cxConsumes = {
                scheduleVo.getCxClass1Plan() == null ? 0 : scheduleVo.getCxClass1Plan(),
                scheduleVo.getCxClass2Plan() == null ? 0 : scheduleVo.getCxClass2Plan(),
                scheduleVo.getCxClass3Plan() == null ? 0 : scheduleVo.getCxClass3Plan(),
                scheduleVo.getCxClass4Plan() == null ? 0 : scheduleVo.getCxClass4Plan(),
                scheduleVo.getCxClass5Plan() == null ? 0 : scheduleVo.getCxClass5Plan(),
                scheduleVo.getCxClass6Plan() == null ? 0 : scheduleVo.getCxClass6Plan(),
                scheduleVo.getCxClass7Plan() == null ? 0 : scheduleVo.getCxClass7Plan(),
                scheduleVo.getCxClass8Plan() == null ? 0 : scheduleVo.getCxClass8Plan()
        };

        // 备库从成型 (triggerClass+2) 班开始连续 N 个班
        int startCxClass = triggerClass + 2;
        double totalQty = 0;
        int coveredShifts = 0;

        // 阶段1：从成型计划中取胎圈消耗量（计划内的班次）
        while (coveredShifts < backupShiftCount && startCxClass <= 8) {
            double consume = cxConsumes[startCxClass - 1];
            totalQty = BigDecimalUtil.add(totalQty, BigDecimalUtil.mul(consume, coefficient));
            startCxClass++;
            coveredShifts++;
        }

        // 阶段2：超出成型计划的班次，使用"最后 3 个非停产成型班次"的平均消耗量 × 系数
        if (coveredShifts < backupShiftCount) {
            double avgConsume = calculateAvgLast3NonStopConsume(cxConsumes);
            while (coveredShifts < backupShiftCount) {
                totalQty = BigDecimalUtil.add(totalQty, BigDecimalUtil.mul(avgConsume, coefficient));
                coveredShifts++;
            }
        }

        return totalQty;
    }

    /**
     * 计算成型最后 3 个非停产班次的平均消耗量。
     *
     * <p>停产班次的特征：成型消耗量为 0（停产意味着该班次无产量）。</p>
     * <p>从成型 8 班倒序遍历，收集非 0 消耗的班次，取最后 3 个的平均值。</p>
     *
     * @param cxConsumes 成型 1-8 班的消耗量数组
     * @return 最后 3 个非停产班次的平均消耗量
     */
    public static double calculateAvgLast3NonStopConsume(double[] cxConsumes) {
        List<Double> nonStopConsumes = new ArrayList<>();
        for (int i = cxConsumes.length - 1; i >= 0; i--) {
            if (cxConsumes[i] > 0) {
                nonStopConsumes.add(cxConsumes[i]);
                if (nonStopConsumes.size() >= 3) {
                    break;
                }
            }
        }
        if (nonStopConsumes.isEmpty()) {
            return 0;
        }
        double sum = 0;
        for (Double consume : nonStopConsumes) {
            sum = BigDecimalUtil.add(sum, consume);
        }
        return BigDecimalUtil.div(sum, nonStopConsumes.size(), 4);
    }

    // ==================== 收尾判断 ====================

    /**
     * 收尾判断（基于胎胚关联汇总）。
     *
     * <p>一个胎圈可能对应多个胎胚，需考虑所有关联胎胚是否都收尾：</p>
     * <ol>
     *   <li>通过胎圈编码从 beadEmbryoMap 获取所有关联胎胚编码</li>
     *   <li>按胎胚号 group 汇总所有胎胚的月计划余量</li>
     *   <li>收尾条件：该胎圈所有关联胎胚的月计划余量都 ≤ 6 班总需排产量</li>
     *   <li>无关联胎胚时走原逻辑（按胎圈编码查月计划余量）</li>
     * </ol>
     *
     * @param scheduleVo    排程结果 VO
     * @param context       排程上下文
     * @param totalTqDemand 6 班总需排产量
     * @return true=收尾规格 false=非收尾规格
     */
    public static boolean checkCloseOutByEmbryo(TqScheduleResultVo scheduleVo, TqScheduleContext context, double totalTqDemand) {
        String beadCode = scheduleVo.getBeadCode();
        List<String> embryoList = context.getBeadEmbryoMap().get(beadCode);

        if (embryoList == null || embryoList.isEmpty()) {
            // 无关联胎胚，走原逻辑：按胎圈编码查月计划余量
            TqMonthSurplusVo monthSurplusVo = context.getMonthSurplusMap().get(beadCode);
            double monthRemainQty = monthSurplusVo == null ? 0D : monthSurplusVo.getMonthRemainQty();
            return monthRemainQty > 0 && monthRemainQty <= totalTqDemand;
        }

        // 按胎胚号 group 汇总，所有胎胚都满足收尾条件才算收尾
        for (String embryoCode : embryoList) {
            TqMonthSurplusVo embryoSurplus = context.getMonthSurplusMap().get(embryoCode);
            double embryoRemainQty = embryoSurplus == null ? 0D : embryoSurplus.getMonthRemainQty();
            if (embryoRemainQty > totalTqDemand) {
                // 任一胎胚不满足收尾条件，则该胎圈非收尾
                return false;
            }
        }
        return true;
    }

    // ==================== 计划量取整 ====================

    /**
     * 计划量取整 + 工装限制。
     *
     * <p>规则：</p>
     * <ul>
     *   <li>非收尾：按工装容量向上取整</li>
     *   <li>收尾：min(需排产量, 月计划余量) × (100% + 损耗率)</li>
     *   <li>工装限制：可用工装数量 = 工装总数 - 库存/整车个数；需排产量 = min(可用工装数量 × 整车个数, 需排产量)</li>
     *   <li>如果超过月度剩余量，则截断到月度剩余量</li>
     * </ul>
     *
     * <p>注：单规格/多规格机台的差异由 S3阶段（TqMachineAssignHandler）处理，S2阶段统一按工装容量取整。</p>
     *
     * @param scheduleVo       排程结果 VO
     * @param planQty          原始计划量
     * @param toolCapacity     工装容量
     * @param totalConsumeQty  月计划余量
     * @param context          排程上下文
     * @return 取整和工装限制后的计划量
     */
    public static double planQtyRounding(TqScheduleResultVo scheduleVo, double planQty, BigDecimal toolCapacity,
                                          Double totalConsumeQty, TqScheduleContext context) {
        if (planQty <= 0) {
            return 0D;
        }

        boolean isCloseOutSpec = "0".equals(scheduleVo.getCloseOutSpecFlag());
        double lossRate = context.getParams().getLossRate() == null ? 0D : context.getParams().getLossRate();

        if (isCloseOutSpec) {
            // 收尾规格：min(需排产量, 月计划余量) × (1 + 损耗率)（损耗率以比率存储，如0.02表示2%）
            if (totalConsumeQty != null && totalConsumeQty > 0 && planQty > totalConsumeQty) {
                planQty = totalConsumeQty;
            }
            double rate = BigDecimalUtil.add(1, lossRate);
            planQty = BigDecimalUtil.mul(planQty, rate);
        } else {
            // 非收尾规格：按工装容量向上取整
            if (toolCapacity.doubleValue() > 0 && planQty > 0) {
                planQty = Math.ceil(planQty / toolCapacity.doubleValue()) * toolCapacity.doubleValue();
            }
            // 超过月度剩余量则截断
            if (totalConsumeQty != null && totalConsumeQty > 0 && planQty > totalConsumeQty) {
                planQty = totalConsumeQty;
            }
        }

        // 工装限制：工装车总数从参数配置获取（全局统一值），整车容量按胎圈编码从容量表获取
        // 可用工装数量 = 工装车总数 - 库存/整车个数；需排产量 = min(可用工装数量 × 整车个数, 需排产量)
        String beadCode = scheduleVo.getBeadCode();
        Integer toolingTotal = context.getParams().getToolingTotal();
        Integer cartCapacity = context.getCartCapacityMap().get(beadCode);
        if (toolingTotal != null && toolingTotal > 0 && cartCapacity != null && cartCapacity > 0) {
            double stockQty = scheduleVo.getStockQty() == null ? 0D : scheduleVo.getStockQty();
            double usedTooling = Math.ceil(stockQty / cartCapacity);
            double availableTooling = Math.max(0, toolingTotal - usedTooling);
            double maxPlanByTooling = availableTooling * cartCapacity;
            if (availableTooling == 0 && planQty > 0) {
                maxPlanByTooling = cartCapacity;
            }
            if (planQty > maxPlanByTooling) {
                planQty = maxPlanByTooling;
            }
        }

        return planQty;
    }

    // ==================== 机台定额超排容忍判断（SYS1101031） ====================

    /**
     * 判断尾数是否可在当班超排（SYS1101031 机台定额超排容忍阈值）。
     *
     * <p>当计划量超出机台剩余产能/定额，且超出部分 ≤ 超排容忍阈值时，
     * 允许当班超排（突破机台定额），不延后到下一班单独排产。</p>
     *
     * @param overflowQty 超出量（计划量 - 剩余产能/定额）
     * @param tolerance   超排容忍阈值（SYS1101031，null 或 ≤0 表示不启用）
     * @return true=可当班超排，false=需延后到下一班
     */
    public static boolean canOverAssignInCurrentClass(double overflowQty, Double tolerance) {
        double t = tolerance == null ? 0D : tolerance;
        return t > 0 && overflowQty > 0 && overflowQty <= t;
    }

    // ==================== 收尾标识与生产状态 ====================

    /**
     * 设置收尾提示标识和生产状态字段（基于胎胚关联汇总判断）。
     *
     * @param scheduleVo 排程结果 VO
     * @param context    排程上下文
     */
    public static void setStatusAndCloseTip(TqScheduleResultVo scheduleVo, TqScheduleContext context) {
        TqMonthSurplusVo monthSurplusVo = context.getMonthSurplusMap().get(scheduleVo.getBeadCode());
        Double closeOutNum = context.getParams().getCloseOutNum();

        if (monthSurplusVo == null) {
            scheduleVo.setMarkCloseOutTip(EngineConstants.CLOSE_TIP_NOT);
            scheduleVo.setProductionStatus(EngineConstants.PRODUCTION_STATUS_NOT);
            return;
        }

        Double monthRemainQty = monthSurplusVo.getMonthRemainQty();
        if (monthRemainQty < closeOutNum) {
            scheduleVo.setMarkCloseOutTip(EngineConstants.CLOSE_TIP_NEED);
        } else {
            scheduleVo.setMarkCloseOutTip(EngineConstants.CLOSE_TIP_NOT);
        }

        // 月计划完成量（monthFinishQty）不再从t_mp_month_plan_prod_final查询，
        // 改为MES回报后通过t_tq_sche_finish_qty回填。自动排程阶段仅基于monthRemainQty判断生产状态。
        if (monthRemainQty <= 0) {
            scheduleVo.setProductionStatus(EngineConstants.PRODUCTION_STATUS_FINISH);
        } else if (monthRemainQty < closeOutNum) {
            // 有剩余量且低于收尾阈值，视为生产中
            scheduleVo.setProductionStatus(EngineConstants.PRODUCTION_STATUS_ING);
        } else {
            scheduleVo.setProductionStatus(EngineConstants.PRODUCTION_STATUS_NOT);
        }
    }

    // ==================== 规则证据辅助 ====================

    /**
     * 记录规则证据到 Context 中的 ruleTraceMap。
     *
     * @param context  排程上下文
     * @param beadCode 胎圈编码
     * @param ruleCode 规则编码
     * @param result   规则结果
     * @param evidence 证据对象（任意可序列化为 JSON 的对象）
     */
    public static void addRuleTrace(TqScheduleContext context, String beadCode,
                                     TqScheduleRuleCodeEnum ruleCode, TqScheduleRuleResultEnum result,
                                     Object evidence) {
        TqRuleTrace trace = context.getRuleTrace(beadCode);
        trace.addRuleHit(ruleCode, result, evidence);
    }

    /**
     * 构建规则证据 Map（链式调用辅助）。
     *
     * @param key   键
     * @param value 值
     * @return 包含单条数据的 Map
     */
    public static Map<String, Object> evidence(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    // ==================== 日志辅助 ====================

    /**
     * 拼接多段日志为分隔的字符串（保持与原 Handler 一致的日志格式）。
     *
     * @param messages 日志段
     * @return 拼接后的字符串
     */
    public static String logSplit(String... messages) {
        StringBuilder sb = new StringBuilder();
        for (String msg : messages) {
            sb.append(msg).append(DIVISION);
        }
        return sb.toString();
    }

    /**
     * 记录排程日志（封装 AutoScheduleLogService 调用，允许 null service 时跳过）。
     *
     * @param autoScheduleLogService 日志服务
     * @param batchNo                批次号
     * @param orderNo                工单号
     * @param logType                日志类型
     * @param logContent             日志内容
     */
    public static void logSchedule(AutoScheduleLogService autoScheduleLogService,
                                    String batchNo, String orderNo, String logType, String logContent) {
        if (autoScheduleLogService == null) {
            return;
        }
        autoScheduleLogService.insertTqScheduleLog(batchNo, orderNo, logType, logContent);
    }
}
