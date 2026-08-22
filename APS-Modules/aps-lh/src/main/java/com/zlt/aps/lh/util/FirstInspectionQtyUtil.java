package com.zlt.aps.lh.util;

import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.constant.LhScheduleParamConstant;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.enums.ConstructionStageEnum;
import com.zlt.aps.lh.api.enums.ScheduleTypeEnum;
import com.zlt.aps.lh.context.LhScheduleConfig;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.FirstInspectionAllocationPlan;
import com.zlt.aps.lh.engine.strategy.support.FirstInspectionShiftAllocation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 换模与换活字块首检数量工具。
 *
 * <p>业务口径：换模和换活字块都需要首检，切换耗时已经包含首检，
 * 首检数量只影响班次计划量归属和班产占用，不额外增加切换耗时。</p>
 *
 * @author APS
 */
@Slf4j
public final class FirstInspectionQtyUtil {

    /** 班次原因分析中表示当前班次发生首检的固定文案。 */
    public static final String FIRST_INSPECTION_ANALYSIS = "首检";

    /** 单控机台首检数量折算系数，与单控班产 /2 口径一致 */
    private static final int SINGLE_CONTROL_FIRST_INSPECTION_DIVISOR = 2;

    /** 同班次前2台使用前2台首检参数 */
    private static final int FIRST_TWO_INSPECTION_LIMIT = 2;

    /** 班次首检计数键分隔符 */
    private static final String SHIFT_COUNTER_KEY_SEPARATOR = "#";

    /** 试制首检产能折算使用的标准班次时长（小时） */
    private static final int TRIAL_SHIFT_DURATION_HOURS = 8;

    /** 试制首检固定占用中班时长（小时） */
    private static final int TRIAL_FIRST_INSPECTION_HOURS = 2;

    private FirstInspectionQtyUtil() {
    }

    /**
     * 获取同班次非前2台普通机台首检数量。
     *
     * <p>单控机台（机台编码以 L/R 结尾）首检数量需折半，请使用
     * {@link #getFirstInspectionQty(LhScheduleContext, String)}。</p>
     *
     * @param context 排程上下文
     * @return 首检数量，未配置时默认 2，负数按 0 处理
     */
    public static int getFirstInspectionQty(LhScheduleContext context) {
        LhScheduleConfig config = Objects.isNull(context) ? null : context.getScheduleConfig();
        if (Objects.isNull(config)) {
            return LhScheduleConstant.FIRST_INSPECTION_QTY;
        }
        return Math.max(0, config.getFirstInspectionQty());
    }

    /**
     * 获取同班次前2台普通机台首检数量。
     *
     * @param context 排程上下文
     * @return 首检数量，未配置时默认 4，负数按 0 处理
     */
    public static int getFirstTwoFirstInspectionQty(LhScheduleContext context) {
        LhScheduleConfig config = Objects.isNull(context) ? null : context.getScheduleConfig();
        if (Objects.isNull(config)) {
            return LhScheduleConstant.FIRST_TWO_FIRST_INSPECTION_QTY;
        }
        return Math.max(0, config.getFirstTwoFirstInspectionQty());
    }

    /**
     * 获取按机台类型折算后的首检数量。
     *
     * <p>业务口径：普通机台首检数量 = 硫化参数 SYS0303003（默认 2）；
     * 单控机台（机台编码以 L/R 结尾，例如 K1501L、K1501R）首检数量 = 参数值 / 2，
     * 向下取整，与 {@link ShiftCapacityResolverUtil#resolveRuntimeShiftCapacity} 的单控班产折半口径一致。
     * 单控首检数量同样计入硫化余量和排产量。</p>
     *
     * @param context     排程上下文
     * @param machineCode 运行态机台编码
     * @return 折算后的首检数量，单控机台按参数折半向下取整
     */
    public static int getFirstInspectionQty(LhScheduleContext context, String machineCode) {
        int configuredQty = getFirstInspectionQty(context);
        if (!LhSingleControlMachineUtil.isSingleMouldMachine(machineCode)) {
            return configuredQty;
        }
        // 单控机台首检数量折半，向下取整，与单控班产 /2 口径对齐
        return configuredQty / SINGLE_CONTROL_FIRST_INSPECTION_DIVISOR;
    }

    /**
     * 根据换模完成时间解析首检数量归属班次。
     *
     * <p>班次统一使用半开区间 {@code [start, end)}。当完成时间正好等于两个班次边界时，
     * 归入后一个刚开始的班次，例如14:00归中班、22:00归晚班。</p>
     *
     * @param shifts 排程窗口班次
     * @param mouldChangeCompleteTime 换模完成时间
     * @return 首检归属班次，未命中返回 null
     */
    public static LhShiftConfigVO resolveAttributionShift(List<LhShiftConfigVO> shifts,
                                                          Date mouldChangeCompleteTime) {
        // 首检、选机与通用班次索引共用同一个[start,end)入口，边界不得在本类重复实现。
        return LhScheduleTimeUtil.resolveShiftByTime(shifts, mouldChangeCompleteTime);
    }

    /**
     * 解析换模/换活字块后的首检归属班次。
     *
     * <p>默认仍按切换完成时间归属；仅当试制 SKU 的切换完成归属早班时，
     * 首检与生产开产归属调整为同业务日中班。找不到同日中班时返回 null，
     * 由上游按无可归属班次处理，不构造兜底时间。</p>
     *
     * @param context 排程上下文
     * @param sku SKU 排程信息
     * @param shifts 排程窗口班次
     * @param switchCompleteTime 换模/换活字块完成时间
     * @param scheduleType 排程类型
     * @return 首检归属班次，未命中返回 null
     */
    public static LhShiftConfigVO resolveFirstInspectionAttributionShift(LhScheduleContext context,
                                                                         SkuScheduleDTO sku,
                                                                         List<LhShiftConfigVO> shifts,
                                                                         Date switchCompleteTime,
                                                                         String scheduleType) {
        LhShiftConfigVO defaultShift = resolveAttributionShift(shifts, switchCompleteTime);
        if (!isTrialMorningSwitchAttribution(sku, defaultShift, scheduleType)) {
            return defaultShift;
        }
        LhShiftConfigVO afternoonShift = resolveAfternoonShiftOnSameWorkDate(shifts, defaultShift);
        if (Objects.isNull(afternoonShift)) {
            log.warn("试制SKU早班切换后未找到同业务日中班，首检无归属班次, batchNo: {}, materialCode: {}, "
                            + "scheduleType: {}, 切换完成: {}, 早班日期: {}, 早班班次: {}",
                    Objects.isNull(context) ? null : context.getBatchNo(),
                    Objects.isNull(sku) ? null : sku.getMaterialCode(),
                    scheduleType, LhScheduleTimeUtil.formatDateTime(switchCompleteTime),
                    LhScheduleTimeUtil.formatDate(defaultShift.getWorkDate()), defaultShift.getShiftIndex());
            return null;
        }
        log.debug("试制SKU早班切换首检归属调整为中班, batchNo: {}, materialCode: {}, scheduleType: {}, "
                        + "切换完成: {}, 原归属班次: {}, 调整后班次: {}",
                Objects.isNull(context) ? null : context.getBatchNo(),
                Objects.isNull(sku) ? null : sku.getMaterialCode(),
                scheduleType, LhScheduleTimeUtil.formatDateTime(switchCompleteTime),
                defaultShift.getShiftIndex(), afternoonShift.getShiftIndex());
        return afternoonShift;
    }

    /**
     * 解析首检资源占用起点。
     *
     * <p>试制 SKU 早班切换完成时，首检资源从同业务日中班开始占用；
     * 其他 SKU 仍以切换完成时间作为首检资源占用起点。</p>
     *
     * @param context 排程上下文
     * @param sku SKU 排程信息
     * @param shifts 排程窗口班次
     * @param switchCompleteTime 换模/换活字块完成时间
     * @param scheduleType 排程类型
     * @return 首检资源占用起点，未命中返回 null
     */
    public static Date resolveFirstInspectionAttributionTime(LhScheduleContext context,
                                                             SkuScheduleDTO sku,
                                                             List<LhShiftConfigVO> shifts,
                                                             Date switchCompleteTime,
                                                             String scheduleType) {
        LhShiftConfigVO defaultShift = resolveAttributionShift(shifts, switchCompleteTime);
        LhShiftConfigVO attributionShift = resolveFirstInspectionAttributionShift(
                context, sku, shifts, switchCompleteTime, scheduleType);
        if (Objects.isNull(attributionShift)) {
            return null;
        }
        if (isTrialMorningSwitchAttribution(sku, defaultShift, scheduleType)) {
            return attributionShift.getShiftStartDateTime();
        }
        return switchCompleteTime;
    }

    /**
     * 解析试制 SKU 切换后的实际开产时间。
     *
     * <p>仅当试制 SKU 的切换完成归属早班时，开产时间不得早于同业务日中班开始；
     * 量试、正规、小批量沿用传入的默认开产时间。</p>
     *
     * @param context 排程上下文
     * @param sku SKU 排程信息
     * @param shifts 排程窗口班次
     * @param switchCompleteTime 换模/换活字块完成时间
     * @param defaultProductionStartTime 默认开产时间
     * @param scheduleType 排程类型
     * @return 实际开产时间，未命中必要归属班次时返回 null
     */
    public static Date resolveTrialProductionStartTime(LhScheduleContext context,
                                                       SkuScheduleDTO sku,
                                                       List<LhShiftConfigVO> shifts,
                                                       Date switchCompleteTime,
                                                       Date defaultProductionStartTime,
                                                       String scheduleType) {
        if (Objects.isNull(defaultProductionStartTime)) {
            return null;
        }
        LhShiftConfigVO defaultShift = resolveAttributionShift(shifts, switchCompleteTime);
        if (!isTrialMorningSwitchAttribution(sku, defaultShift, scheduleType)) {
            return defaultProductionStartTime;
        }
        LhShiftConfigVO attributionShift = resolveFirstInspectionAttributionShift(
                context, sku, shifts, switchCompleteTime, scheduleType);
        if (Objects.isNull(attributionShift) || Objects.isNull(attributionShift.getShiftStartDateTime())) {
            return null;
        }
        Date afternoonStartTime = attributionShift.getShiftStartDateTime();
        return defaultProductionStartTime.before(afternoonStartTime)
                ? afternoonStartTime : defaultProductionStartTime;
    }

    /**
     * 预读当前班次下一台首检的顺序，不写入计数器。
     *
     * @param context 排程上下文
     * @param attributionShift 首检归属班次
     * @return 下一台首检顺序，从1开始
     */
    public static int resolveNextFirstInspectionSequence(LhScheduleContext context,
                                                         LhShiftConfigVO attributionShift) {
        String counterKey = buildShiftCounterKey(attributionShift);
        if (Objects.isNull(context) || Objects.isNull(counterKey)) {
            return 1;
        }
        Integer currentCount = context.getShiftFirstInspectionCountMap().get(counterKey);
        return Math.max(0, Objects.isNull(currentCount) ? 0 : currentCount) + 1;
    }

    /**
     * 登记当前班次一台首检，换模和换活字块共用同一个计数器。
     *
     * @param context 排程上下文
     * @param attributionShift 首检归属班次
     * @return 登记后的首检顺序，从1开始
     */
    public static int recordFirstInspectionSequence(LhScheduleContext context,
                                                    LhShiftConfigVO attributionShift) {
        String counterKey = buildShiftCounterKey(attributionShift);
        if (Objects.isNull(context) || Objects.isNull(counterKey)) {
            return 1;
        }
        int sequence = resolveNextFirstInspectionSequence(context, attributionShift);
        context.getShiftFirstInspectionCountMap().put(counterKey, sequence);
        return sequence;
    }

    /**
     * 回滚当前班次一台首检登记。
     *
     * @param context 排程上下文
     * @param attributionShift 首检归属班次
     */
    public static void rollbackFirstInspectionSequence(LhScheduleContext context,
                                                       LhShiftConfigVO attributionShift) {
        String counterKey = buildShiftCounterKey(attributionShift);
        if (Objects.isNull(context) || Objects.isNull(counterKey)) {
            return;
        }
        Integer currentCount = context.getShiftFirstInspectionCountMap().get(counterKey);
        if (Objects.isNull(currentCount) || currentCount <= 0) {
            return;
        }
        if (currentCount == 1) {
            context.getShiftFirstInspectionCountMap().remove(counterKey);
            return;
        }
        context.getShiftFirstInspectionCountMap().put(counterKey, currentCount - 1);
    }

    /**
     * 解析当前首检顺序使用的参数编码。
     *
     * @param sequence 当前班次首检顺序
     * @return 参数编码
     */
    public static String resolveFirstInspectionParamCode(int sequence) {
        return sequence <= FIRST_TWO_INSPECTION_LIMIT
                ? LhScheduleParamConstant.FIRST_TWO_FIRST_INSPECTION_QTY
                : LhScheduleParamConstant.FIRST_INSPECTION_QTY;
    }

    /**
     * 解析当前首检顺序使用的参数原始数量。
     *
     * @param context 排程上下文
     * @param sequence 当前班次首检顺序
     * @return 参数原始首检数量
     */
    public static int resolveRawFirstInspectionQty(LhScheduleContext context, int sequence) {
        if (sequence <= FIRST_TWO_INSPECTION_LIMIT) {
            return getFirstTwoFirstInspectionQty(context);
        }
        return getFirstInspectionQty(context);
    }

    /**
     * 解析当前首检顺序和机台类型折算后的首检数量。
     *
     * @param context 排程上下文
     * @param sequence 当前班次首检顺序
     * @param machineCode 机台编码
     * @param logOddSingleControlParam 是否记录单控奇数参数提示
     * @return 折算后的首检数量
     */
    public static int resolveAdjustedFirstInspectionQty(LhScheduleContext context,
                                                        int sequence,
                                                        String machineCode,
                                                        boolean logOddSingleControlParam) {
        int rawQty = resolveRawFirstInspectionQty(context, sequence);
        if (!LhSingleControlMachineUtil.isSingleMouldMachine(machineCode)) {
            return rawQty;
        }
        if (logOddSingleControlParam && rawQty % SINGLE_CONTROL_FIRST_INSPECTION_DIVISOR != 0) {
            log.warn("单控机台首检参数为奇数，按项目既有单控班产折半口径向下取整, machineCode: {}, "
                            + "首检顺序: {}, 参数编码: {}, 参数值: {}, 折算后: {}",
                    machineCode, sequence, resolveFirstInspectionParamCode(sequence), rawQty,
                    rawQty / SINGLE_CONTROL_FIRST_INSPECTION_DIVISOR);
        }
        return rawQty / SINGLE_CONTROL_FIRST_INSPECTION_DIVISOR;
    }

    /**
     * 预读当前班次下一台首检的有效数量，不写入计数器。
     *
     * @param context 排程上下文
     * @param attributionShift 首检归属班次
     * @param shiftCapacity 运行态班产
     * @param remainingQty 当前剩余目标量
     * @param scheduleType 排程类型
     * @param machineCode 机台编码
     * @return 受班产和目标量截断后的首检数量
     */
    public static int resolvePreviewFirstInspectionQty(LhScheduleContext context,
                                                       LhShiftConfigVO attributionShift,
                                                       int shiftCapacity,
                                                       int remainingQty,
                                                       String scheduleType,
                                                       String machineCode) {
        return resolvePreviewFirstInspectionQty(
                context, null, attributionShift, shiftCapacity, remainingQty, scheduleType, machineCode);
    }

    /**
     * 按 SKU 类型预读当前班次下一台首检的有效数量，不写入计数器。
     *
     * <p>试制 SKU 使用固定2小时首检时间扣减中班产能，不生成首检条数，因此直接返回0，
     * 也不会读取 SYS0303002、SYS0303003 或推进当班首检数量顺序。量试、正规和小批量
     * SKU 继续沿用现有首检条数参数及单控折半规则。</p>
     *
     * @param context 排程上下文
     * @param sku SKU排程信息，传null时保持原有非试制逻辑
     * @param attributionShift 首检归属班次
     * @param shiftCapacity 运行态班产
     * @param remainingQty 当前剩余目标量
     * @param scheduleType 排程类型
     * @param machineCode 机台编码
     * @return 受班产和目标量截断后的首检数量；试制SKU固定返回0
     */
    public static int resolvePreviewFirstInspectionQty(LhScheduleContext context,
                                                       SkuScheduleDTO sku,
                                                       LhShiftConfigVO attributionShift,
                                                       int shiftCapacity,
                                                       int remainingQty,
                                                       String scheduleType,
                                                       String machineCode) {
        if (isTrialTimeBasedFirstInspection(sku, attributionShift, scheduleType)) {
            return 0;
        }
        int sequence = resolveNextFirstInspectionSequence(context, attributionShift);
        return resolveEffectiveFirstInspectionQty(
                context, sku, attributionShift, shiftCapacity, remainingQty,
                scheduleType, machineCode, sequence, false);
    }

    /**
     * 读取当前班次最近一次已经登记的实际首检数量。
     *
     * <p>该方法只用于结果提交后的审计日志。首检写入时已经推进班次顺序计数器，若此时再次调用
     * {@link #resolvePreviewFirstInspectionQty(LhScheduleContext, SkuScheduleDTO, LhShiftConfigVO, int,
     * int, String, String)}，会错误读取“下一台”首检参数。例如当前结果是班次内第2台首检，
     * 实际应使用 SYS0303002，但二次预演会按第3台读取 SYS0303003。</p>
     *
     * @param context 排程上下文
     * @param sku 当前 SKU
     * @param attributionShift 首检归属班次
     * @param shiftCapacity 运行态班产
     * @param remainingQty 当前机台目标量
     * @param scheduleType 排程类型
     * @param machineCode 机台编码
     * @return 最近一次已登记首检的实际数量；当前班次尚未登记时返回0
     */
    public static int resolveLastRecordedFirstInspectionQty(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LhShiftConfigVO attributionShift,
            int shiftCapacity,
            int remainingQty,
            String scheduleType,
            String machineCode) {
        if (isTrialTimeBasedFirstInspection(sku, attributionShift, scheduleType)) {
            return 0;
        }
        int recordedSequence = resolveNextFirstInspectionSequence(
                context, attributionShift) - 1;
        if (recordedSequence <= 0) {
            return 0;
        }
        return resolveEffectiveFirstInspectionQty(
                context, sku, attributionShift, shiftCapacity, remainingQty,
                scheduleType, machineCode, recordedSequence, false);
    }

    /**
     * 将普通换模首检数量写入归属班次。
     *
     * <p>首检数量参与排产量和硫化余量消耗，因此写入结果前会按剩余目标量与班产上限收敛。
     * 单控机台（L/R）首检数量按参数折半向下取整。</p>
     *
     * @param context 排程上下文
     * @param result 排程结果
     * @param shifts 排程窗口班次
     * @param mouldChangeCompleteTime 换模完成时间
     * @param shiftCapacity 运行态班产
     * @param remainingQty 当前结果剩余目标量
     * @param scheduleType 排程类型
     * @return 实际写入的首检数量
     */
    public static int addFirstInspectionQtyToResult(LhScheduleContext context,
                                                    LhScheduleResult result,
                                                    List<LhShiftConfigVO> shifts,
                                                    Date mouldChangeCompleteTime,
                                                    int shiftCapacity,
                                                    int remainingQty,
                                                    String scheduleType) {
        return addFirstInspectionQtyToResult(
                context, null, result, shifts, mouldChangeCompleteTime, shiftCapacity, remainingQty, scheduleType);
    }

    /**
     * 按 SKU 类型将换模首检数量写入归属班次。
     *
     * @param context 排程上下文
     * @param sku SKU排程信息，传null时保持原有非试制逻辑
     * @param result 排程结果
     * @param shifts 排程窗口班次
     * @param mouldChangeCompleteTime 换模完成时间
     * @param shiftCapacity 运行态班产
     * @param remainingQty 当前结果剩余目标量
     * @param scheduleType 排程类型
     * @return 实际写入的首检数量；试制SKU固定返回0
     */
    public static int addFirstInspectionQtyToResult(LhScheduleContext context,
                                                    SkuScheduleDTO sku,
                                                    LhScheduleResult result,
                                                    List<LhShiftConfigVO> shifts,
                                                    Date mouldChangeCompleteTime,
                                                    int shiftCapacity,
                                                    int remainingQty,
                                                    String scheduleType) {
        LhShiftConfigVO attributionShift = resolveAttributionShift(shifts, mouldChangeCompleteTime);
        return addFirstInspectionQtyToResult(
                context, sku, result, attributionShift, mouldChangeCompleteTime,
                shiftCapacity, remainingQty, scheduleType);
    }

    /**
     * 将首检数量写入指定归属班次。
     *
     * <p>用于试制 SKU 早班切换后将首检归属调整到中班；普通场景仍可传入
     * {@link #resolveAttributionShift(List, Date)} 的结果。</p>
     *
     * @param context 排程上下文
     * @param result 排程结果
     * @param attributionShift 首检归属班次
     * @param switchCompleteTime 换模/换活字块完成时间
     * @param shiftCapacity 运行态班产
     * @param remainingQty 当前结果剩余目标量
     * @param scheduleType 排程类型
     * @return 实际写入的首检数量
     */
    public static int addFirstInspectionQtyToResult(LhScheduleContext context,
                                                    LhScheduleResult result,
                                                    LhShiftConfigVO attributionShift,
                                                    Date switchCompleteTime,
                                                    int shiftCapacity,
                                                    int remainingQty,
                                                    String scheduleType) {
        return addFirstInspectionQtyToResult(
                context, null, result, attributionShift, switchCompleteTime,
                shiftCapacity, remainingQty, scheduleType);
    }

    /**
     * 按 SKU 类型将首检数量写入指定归属班次。
     *
     * <p>试制 SKU 的首检只以固定2小时方式压缩中班最大生产量，不写首检条数；
     * 其他 SKU 继续写入参数首检条数，并登记当班首检数量顺序。</p>
     *
     * @param context 排程上下文
     * @param sku SKU排程信息，传null时保持原有非试制逻辑
     * @param result 排程结果
     * @param attributionShift 首检归属班次
     * @param switchCompleteTime 换模/换活字块完成时间
     * @param shiftCapacity 运行态班产
     * @param remainingQty 当前结果剩余目标量
     * @param scheduleType 排程类型
     * @return 实际写入的首检数量；试制SKU固定返回0
     */
    public static int addFirstInspectionQtyToResult(LhScheduleContext context,
                                                    SkuScheduleDTO sku,
                                                    LhScheduleResult result,
                                                    LhShiftConfigVO attributionShift,
                                                    Date switchCompleteTime,
                                                    int shiftCapacity,
                                                    int remainingQty,
                                                    String scheduleType) {
        if (isTrialTimeBasedFirstInspection(sku, attributionShift, scheduleType)) {
            return 0;
        }
        int firstInspectionSequence = resolveNextFirstInspectionSequence(context, attributionShift);
        int firstInspectionQty = resolveEffectiveFirstInspectionQty(
                context, sku, attributionShift, shiftCapacity, remainingQty, scheduleType,
                Objects.isNull(result) ? null : result.getLhMachineCode(), firstInspectionSequence, true);
        if (Objects.isNull(result) || Objects.isNull(attributionShift) || firstInspectionQty <= 0) {
            return 0;
        }
        Integer existingQty = ShiftFieldUtil.getShiftPlanQty(result, attributionShift.getShiftIndex());
        int basePlanQty = Math.max(0, Objects.isNull(existingQty) ? 0 : existingQty);
        int mergedQty = basePlanQty + firstInspectionQty;
        ShiftFieldUtil.setShiftPlanQty(result, attributionShift.getShiftIndex(), mergedQty,
                attributionShift.getShiftStartDateTime(), attributionShift.getShiftEndDateTime());
        ShiftFieldUtil.appendShiftAnalysis(
                result, attributionShift.getShiftIndex(), FIRST_INSPECTION_ANALYSIS);
        recordFirstInspectionSequence(context, attributionShift);
        boolean singleControl = LhSingleControlMachineUtil.isSingleMouldMachine(result.getLhMachineCode());
        int rawFirstInspectionQty = resolveRawFirstInspectionQty(context, firstInspectionSequence);
        boolean attributionDelayed = Objects.nonNull(switchCompleteTime)
                && Objects.nonNull(attributionShift.getShiftStartDateTime())
                && switchCompleteTime.before(attributionShift.getShiftStartDateTime());
        log.info("首检数量归属班次, scene: {}, batchNo: {}, materialCode: {}, machineCode: {}, "
                        + "是否单控: {}, 切换完成: {}, 归属日期: {}, 归属班次: {}, 当班首检顺序: {}, "
                        + "参数编码: {}, 参数原始首检数量: {}, 单控折半后首检数量: {}, "
                        + "扣除切换后的可生产量: {}, 加首检后的最终班次计划量: {}, 班产校验上限: {}, "
                        + "剩余目标量校验上限: {}, 归属是否后移: {}, 说明: 切换耗时已包含首检，首检只影响数量归属和班产占用",
                resolveSceneName(scheduleType),
                result.getBatchNo(), result.getMaterialCode(), result.getLhMachineCode(),
                singleControl, LhScheduleTimeUtil.formatDateTime(switchCompleteTime),
                LhScheduleTimeUtil.formatDate(attributionShift.getWorkDate()), attributionShift.getShiftIndex(),
                firstInspectionSequence, resolveFirstInspectionParamCode(firstInspectionSequence),
                rawFirstInspectionQty, firstInspectionQty, basePlanQty, mergedQty,
                resolveShiftCapacityCap(context, attributionShift, shiftCapacity, scheduleType), remainingQty,
                attributionDelayed ? 1 : 0);
        return firstInspectionQty;
    }

    /**
     * 将已经预演通过的跨班首检计划写入排程结果。
     *
     * <p>一次换模/换活字块事件只推进一次“同班次前2台”计数，计数班次仍取切换结束时间
     * 所属班次；首检数量则严格按计划中的真实重叠时间写入一个或多个班次。正式写入前会
     * 校验计数顺序未被其它事件改变，禁止预演使用4条、提交却按2条的前后不一致。</p>
     *
     * @param context 排程上下文
     * @param result 排程结果
     * @param plan 候选选机阶段形成的首检分摊计划
     * @param scheduleType 排程类型
     * @return 实际写入的首检总量；计划无效或计数已变化时返回0
     */
    public static int addFirstInspectionAllocationToResult(
            LhScheduleContext context,
            LhScheduleResult result,
            FirstInspectionAllocationPlan plan,
            String scheduleType) {
        if (Objects.isNull(result) || Objects.isNull(plan) || !plan.isValid()
                || plan.getInspectionQty() <= 0 || Objects.isNull(plan.getCountingShift())) {
            return 0;
        }
        int currentSequence = resolveNextFirstInspectionSequence(context, plan.getCountingShift());
        if (currentSequence != plan.getSequence()) {
            log.warn("首检分摊计划提交时计数顺序已变化，拒绝二次计算, batchNo: {}, materialCode: {}, "
                            + "machineCode: {}, 预演顺序: {}, 当前顺序: {}, 计数班次: class{}",
                    result.getBatchNo(), result.getMaterialCode(), result.getLhMachineCode(),
                    plan.getSequence(), currentSequence, plan.getCountingShift().getShiftIndex());
            return 0;
        }
        StringBuilder allocationDetail = new StringBuilder(128);
        int writtenQty = 0;
        int completionShiftIndex = resolveInspectionCompletionShiftIndex(plan);
        for (FirstInspectionShiftAllocation allocation : plan.getShiftAllocations()) {
            if (Objects.isNull(allocation) || allocation.getQuantity() <= 0
                    || Objects.isNull(allocation.getShift())) {
                continue;
            }
            int shiftIndex = allocation.getShift().getShiftIndex();
            Integer existingQty = ShiftFieldUtil.getShiftPlanQty(result, shiftIndex);
            Date existingStartTime = ShiftFieldUtil.getShiftStartTime(result, shiftIndex);
            Date existingEndTime = ShiftFieldUtil.getShiftEndTime(result, shiftIndex);
            int mergedQty = Math.max(0, Objects.isNull(existingQty) ? 0 : existingQty)
                    + allocation.getQuantity();
            Date mergedStartTime = Objects.isNull(existingStartTime)
                    || allocation.getOverlapStartTime().before(existingStartTime)
                    ? allocation.getOverlapStartTime() : existingStartTime;
            Date mergedEndTime = Objects.isNull(existingEndTime)
                    || allocation.getOverlapEndTime().after(existingEndTime)
                    ? allocation.getOverlapEndTime() : existingEndTime;
            ShiftFieldUtil.setShiftPlanQty(
                    result, shiftIndex, mergedQty, mergedStartTime, mergedEndTime);
            // 首检可跨多个班次写入计划量，但原因分析仅归属首检完成所在班次，避免前序班次重复备注。
            if (Objects.equals(shiftIndex, completionShiftIndex)) {
                ShiftFieldUtil.appendShiftAnalysis(result, shiftIndex, FIRST_INSPECTION_ANALYSIS);
            }
            writtenQty += allocation.getQuantity();
            if (allocationDetail.length() > 0) {
                allocationDetail.append("; ");
            }
            allocationDetail.append("class").append(shiftIndex)
                    .append('=')
                    .append(allocation.getQuantity())
                    .append('[')
                    .append(LhScheduleTimeUtil.formatDateTime(allocation.getOverlapStartTime()))
                    .append(',')
                    .append(LhScheduleTimeUtil.formatDateTime(allocation.getOverlapEndTime()))
                    .append(')');
        }
        if (writtenQty != plan.getInspectionQty()) {
            log.warn("首检分摊结果写入总量不守恒，拒绝推进首检顺序, batchNo: {}, materialCode: {}, "
                            + "machineCode: {}, 应写: {}, 实写: {}",
                    result.getBatchNo(), result.getMaterialCode(), result.getLhMachineCode(),
                    plan.getInspectionQty(), writtenQty);
            return 0;
        }
        recordFirstInspectionSequence(context, plan.getCountingShift());
        String sceneName = resolveSceneName(scheduleType);
        log.info("首检按真实时间跨班分摊完成, scene: {}, batchNo: {}, materialCode: {}, machineCode: {}, "
                        + "计数日期: {}, 计数班次: class{}, 当班首检顺序: {}, 参数编码: {}, "
                        + "首检总量: {}, 小时产量: {}, 首检时长秒: {}, 首检区间: [{}, {}), 分摊: {}",
                sceneName, result.getBatchNo(), result.getMaterialCode(),
                result.getLhMachineCode(), LhScheduleTimeUtil.formatDate(plan.getCountingShift().getWorkDate()),
                plan.getCountingShift().getShiftIndex(), plan.getSequence(),
                resolveFirstInspectionParamCode(plan.getSequence()), plan.getInspectionQty(),
                plan.getHourlyOutput(), plan.getInspectionDurationSeconds(),
                LhScheduleTimeUtil.formatDateTime(plan.getInspectionStartTime()),
                LhScheduleTimeUtil.formatDateTime(plan.getInspectionEndTime()), allocationDetail);
        return writtenQty;
    }

    /**
     * 解析首检时间区间实际结束所在班次。
     *
     * <p>计数班次通常与完成班次一致，但切换完成时间恰好落在班次边界时，
     * 计数班次会命中后一个班次；原因分析仍应归到首检区间实际结束的前一个班次。</p>
     *
     * @param plan 首检时间分摊计划
     * @return 首检完成所在班次索引
     */
    private static int resolveInspectionCompletionShiftIndex(FirstInspectionAllocationPlan plan) {
        Date inspectionEndTime = plan.getInspectionEndTime();
        for (FirstInspectionShiftAllocation allocation : plan.getShiftAllocations()) {
            if (Objects.nonNull(allocation)
                    && Objects.nonNull(allocation.getShift())
                    && Objects.nonNull(allocation.getOverlapEndTime())
                    && Objects.equals(inspectionEndTime, allocation.getOverlapEndTime())) {
                return allocation.getShift().getShiftIndex();
            }
        }
        return plan.getCountingShift().getShiftIndex();
    }

    /**
     * 将已经随排程结果最终提交的首检真实时间分摊写入过程日志。
     *
     * <p>该方法必须在结果、日计划账本及机台占用全部通过后调用。
     * 候选构建阶段只允许调用 {@link #addFirstInspectionAllocationToResult}
     * 写入暂存结果，不能提前写过程日志；否则日计划回裁、精度计划拒绝等后续分支
     * 会留下“候选已被拒绝，但首检仍显示已落地”的伪日志。</p>
     *
     * <p>换模和换活字块共用本方法，日志直接格式化选机/排产阶段传递的
     * 同一份分摊计划，禁止为日志再次计算首检数量或班次。</p>
     *
     * @param context 排程上下文
     * @param result 已经进入最终排程结果集的结果
     * @param plan 选机/排产阶段已实际使用的首检分摊计划
     * @param scheduleType 排程类型，用于区分换模和换活字块日志场景
     */
    public static void appendCommittedFirstInspectionAllocationProcessLog(
            LhScheduleContext context,
            LhScheduleResult result,
            FirstInspectionAllocationPlan plan,
            String scheduleType) {
        if (Objects.isNull(context) || Objects.isNull(result) || Objects.isNull(plan)
                || !plan.isValid() || plan.getInspectionQty() <= 0
                || Objects.isNull(plan.getCountingShift())
                || !context.getScheduleResultList().contains(result)) {
            return;
        }
        String sceneName = resolveSceneName(scheduleType);
        String allocationDetail = buildFirstInspectionAllocationDetail(plan);
        StringBuilder detailBuilder = new StringBuilder(384);
        detailBuilder.append("批次=").append(result.getBatchNo())
                .append("，场景=").append(sceneName)
                .append("，物料=").append(result.getMaterialCode())
                .append("，机台=").append(result.getLhMachineCode())
                .append("，计数日期=")
                .append(LhScheduleTimeUtil.formatDate(plan.getCountingShift().getWorkDate()))
                .append("，计数班次=class").append(plan.getCountingShift().getShiftIndex())
                .append("，当班首检顺序=").append(plan.getSequence())
                .append("，参数编码=").append(resolveFirstInspectionParamCode(plan.getSequence()))
                .append("，首检总量=").append(plan.getInspectionQty())
                .append("，小时产量=").append(plan.getHourlyOutput())
                .append("，首检时长秒=").append(plan.getInspectionDurationSeconds())
                .append("，首检区间=[")
                .append(LhScheduleTimeUtil.formatDateTime(plan.getInspectionStartTime()))
                .append(',')
                .append(LhScheduleTimeUtil.formatDateTime(plan.getInspectionEndTime()))
                .append(")，班次分摊=").append(allocationDetail);
        PriorityTraceLogHelper.appendProcessLog(
                context, "首检真实时间分摊", detailBuilder.toString());
    }

    /**
     * 将已确认的各班次首检分摊格式化为可对账日志明细。
     *
     * @param plan 首检分摊计划
     * @return 按真实时间顺序排列的班次数量与区间
     */
    private static String buildFirstInspectionAllocationDetail(
            FirstInspectionAllocationPlan plan) {
        StringBuilder allocationDetail = new StringBuilder(128);
        for (FirstInspectionShiftAllocation allocation : plan.getShiftAllocations()) {
            if (Objects.isNull(allocation) || allocation.getQuantity() <= 0
                    || Objects.isNull(allocation.getShift())) {
                continue;
            }
            if (allocationDetail.length() > 0) {
                allocationDetail.append("; ");
            }
            allocationDetail.append("class")
                    .append(allocation.getShift().getShiftIndex())
                    .append('=')
                    .append(allocation.getQuantity())
                    .append('[')
                    .append(LhScheduleTimeUtil.formatDateTime(allocation.getOverlapStartTime()))
                    .append(',')
                    .append(LhScheduleTimeUtil.formatDateTime(allocation.getOverlapEndTime()))
                    .append(')');
        }
        return allocationDetail.toString();
    }

    /**
     * 校验结果在后置账本裁剪后是否仍完整保留本次首检分摊。
     *
     * <p>首检属于切换阶段已经真实发生的目标量，不能被后续日计划回裁删除。单控整机先按
     * L/R 合计结果扣账时，{@code quantityMultiplier} 传2；普通机台和回写到单侧
     * 结果时传1。任一班次少于已分摊首检量都表示候选时间轴已被破坏，调用方
     * 必须拒绝当前结果，不得通过重算首检数量兜底。</p>
     *
     * @param result 已写入首检的结果或单控整机合计结果
     * @param plan 本次实际使用的首检分摊计划
     * @param quantityMultiplier 数量倍数；普通结果为1，单控L/R整机合计结果为2
     * @return true-全部班次仍保留完整首检；false-至少一个班次已被裁掉
     */
    public static boolean isFirstInspectionAllocationRetained(
            LhScheduleResult result,
            FirstInspectionAllocationPlan plan,
            int quantityMultiplier) {
        if (Objects.isNull(plan) || !plan.isValid() || plan.getInspectionQty() <= 0) {
            return true;
        }
        if (Objects.isNull(result)) {
            return false;
        }
        int resolvedMultiplier = Math.max(1, quantityMultiplier);
        for (FirstInspectionShiftAllocation allocation : plan.getShiftAllocations()) {
            if (Objects.isNull(allocation) || Objects.isNull(allocation.getShift())) {
                return false;
            }
            int requiredInspectionQty = allocation.getQuantity() * resolvedMultiplier;
            Integer resultShiftQty = ShiftFieldUtil.getShiftPlanQty(
                    result, allocation.getShift().getShiftIndex());
            if (Math.max(0, Objects.isNull(resultShiftQty) ? 0 : resultShiftQty)
                    < requiredInspectionQty) {
                return false;
            }
        }
        return true;
    }

    /**
     * 按日计划账本回裁单班数量，同时保护已经真实发生的首检数量与时间区间。
     *
     * <p>账本只能减少首检之后的正式生产量。当回裁后仅余首检时，结果开始/结束
     * 必须恢复为计划中的真实重叠区间，不能保留原整班生产结束时间。单控整机
     * 合计结果使用倍数2，保证后续均分回L/R时每侧首检不丢失。</p>
     *
     * @param result 待回裁结果
     * @param shiftIndex 回裁班次索引
     * @param quotaRetainedQty 日计划账本允许保留的数量
     * @param plan 本次已提交的首检分摊计划；无首检时可为null
     * @param quantityMultiplier 数量倍数；普通结果为1，单控整机合计结果为2
     * @return 最终保留的班次数量
     */
    public static int trimShiftPlanQtyPreservingInspection(
            LhScheduleResult result,
            int shiftIndex,
            int quotaRetainedQty,
            FirstInspectionAllocationPlan plan,
            int quantityMultiplier) {
        if (Objects.isNull(result)) {
            return 0;
        }
        FirstInspectionShiftAllocation protectedAllocation = null;
        if (Objects.nonNull(plan) && plan.isValid() && plan.getInspectionQty() > 0) {
            for (FirstInspectionShiftAllocation allocation : plan.getShiftAllocations()) {
                if (Objects.nonNull(allocation) && Objects.nonNull(allocation.getShift())
                        && Objects.equals(allocation.getShift().getShiftIndex(), shiftIndex)) {
                    protectedAllocation = allocation;
                    break;
                }
            }
        }
        int resolvedMultiplier = Math.max(1, quantityMultiplier);
        int protectedInspectionQty = Objects.isNull(protectedAllocation)
                ? 0 : protectedAllocation.getQuantity() * resolvedMultiplier;
        int finalQty = Math.max(Math.max(0, quotaRetainedQty), protectedInspectionQty);
        if (finalQty <= 0) {
            ShiftFieldUtil.setShiftPlanQty(result, shiftIndex, 0, null, null);
            return 0;
        }
        if (Objects.nonNull(protectedAllocation) && finalQty == protectedInspectionQty) {
            ShiftFieldUtil.setShiftPlanQty(
                    result, shiftIndex, finalQty,
                    protectedAllocation.getOverlapStartTime(),
                    protectedAllocation.getOverlapEndTime());
            return finalQty;
        }
        ShiftFieldUtil.setShiftPlanQty(
                result, shiftIndex, finalQty,
                ShiftFieldUtil.getShiftStartTime(result, shiftIndex), null);
        return finalQty;
    }

    /**
     * 使用跨班首检计划调整“首检 + 正式生产”班次总产能图。
     *
     * <p>首检覆盖正式开产前班次时，即使原正常生产产能图中没有该班次，也会补入真实首检量。
     * 同班同时存在首检和正式生产时，两个时间段前后相邻且不重叠：首检区间截止到切换结束，
     * 正式生产从真实可开产时间开始。因此这里合并两段各自已经按停机、清洗、维修和班次管控
     * 计算出的产能；完整班产及日标准上限由调用方后续统一收敛，禁止拿首检短区间容量再次
     * 截断正式生产时段。</p>
     *
     * @param shifts 完整排程班次
     * @param shiftCapacityMap 正式生产产能图
     * @param plan 首检跨班分摊计划
     * @return 首检占用合并后的班次总产能图
     */
    public static Map<Integer, Integer> applyFirstInspectionAllocationToCapacityMap(
            List<LhShiftConfigVO> shifts,
            Map<Integer, Integer> shiftCapacityMap,
            FirstInspectionAllocationPlan plan) {
        Map<Integer, Integer> adjustedMap = new LinkedHashMap<Integer, Integer>(
                CollectionUtils.isEmpty(shifts) ? 0 : shifts.size());
        if (CollectionUtils.isEmpty(shifts)) {
            return adjustedMap;
        }
        Map<Integer, Integer> inspectionQtyMap = FirstInspectionAllocationUtil.toShiftQtyMap(plan);
        for (LhShiftConfigVO shift : shifts) {
            if (Objects.isNull(shift) || Objects.isNull(shift.getShiftIndex())) {
                continue;
            }
            int shiftIndex = shift.getShiftIndex();
            int normalCapacity = CollectionUtils.isEmpty(shiftCapacityMap)
                    ? 0 : Math.max(0, shiftCapacityMap.getOrDefault(shiftIndex, 0));
            int inspectionQty = Math.max(0, inspectionQtyMap.getOrDefault(shiftIndex, 0));
            if (inspectionQty <= 0) {
                if (!CollectionUtils.isEmpty(shiftCapacityMap)
                        && shiftCapacityMap.containsKey(shiftIndex)) {
                    adjustedMap.put(shiftIndex, normalCapacity);
                }
                continue;
            }
            long mergedCapacity = (long) inspectionQty + normalCapacity;
            adjustedMap.put(shiftIndex, mergedCapacity > Integer.MAX_VALUE
                    ? Integer.MAX_VALUE : (int) mergedCapacity);
        }
        return adjustedMap;
    }

    /**
     * 按首检数量调整班次产能图。
     *
     * <p>首检落在开产班次之前时，补入该班次首检数量；落在开产班次内时，
     * 首检数量和正常生产量共享该班次班产上限。
     * 单控机台（L/R）首检数量按参数折半向下取整。</p>
     *
     * @param context 排程上下文
     * @param shifts 排程窗口班次
     * @param mouldChangeCompleteTime 换模完成时间
     * @param shiftCapacityMap 正常生产产能图
     * @param shiftCapacity 运行态班产
     * @param remainingQty 当前剩余目标量
     * @param scheduleType 排程类型
     * @param machineCode 运行态机台编码，用于单控折半
     * @return 调整后的产能图
     */
    public static Map<Integer, Integer> applyFirstInspectionQtyToCapacityMap(
            LhScheduleContext context,
            List<LhShiftConfigVO> shifts,
            Date mouldChangeCompleteTime,
            Map<Integer, Integer> shiftCapacityMap,
            int shiftCapacity,
            int remainingQty,
            String scheduleType,
            String machineCode) {
        return applyFirstInspectionQtyToCapacityMap(
                context, null, shifts, mouldChangeCompleteTime, shiftCapacityMap,
                shiftCapacity, remainingQty, scheduleType, machineCode);
    }

    /**
     * 按 SKU 类型调整换模首检对应的班次产能图。
     *
     * @param context 排程上下文
     * @param sku SKU排程信息，传null时保持原有非试制逻辑
     * @param shifts 排程窗口班次
     * @param mouldChangeCompleteTime 换模完成时间
     * @param shiftCapacityMap 正常生产产能图
     * @param shiftCapacity 运行态班产
     * @param remainingQty 当前剩余目标量
     * @param scheduleType 排程类型
     * @param machineCode 运行态机台编码
     * @return 调整后的产能图
     */
    public static Map<Integer, Integer> applyFirstInspectionQtyToCapacityMap(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            List<LhShiftConfigVO> shifts,
            Date mouldChangeCompleteTime,
            Map<Integer, Integer> shiftCapacityMap,
            int shiftCapacity,
            int remainingQty,
            String scheduleType,
            String machineCode) {
        Map<Integer, Integer> adjustedMap = new LinkedHashMap<Integer, Integer>(
                CollectionUtils.isEmpty(shifts) ? 0 : shifts.size());
        if (CollectionUtils.isEmpty(shifts)) {
            return adjustedMap;
        }
        LhShiftConfigVO attributionShift = resolveAttributionShift(shifts, mouldChangeCompleteTime);
        return applyFirstInspectionQtyToCapacityMap(
                context, sku, shifts, attributionShift, shiftCapacityMap,
                shiftCapacity, remainingQty, scheduleType, machineCode);
    }

    /**
     * 按指定首检归属班次调整班次产能图。
     *
     * @param context 排程上下文
     * @param shifts 排程窗口班次
     * @param attributionShift 首检归属班次
     * @param shiftCapacityMap 正常生产产能图
     * @param shiftCapacity 运行态班产
     * @param remainingQty 当前剩余目标量
     * @param scheduleType 排程类型
     * @param machineCode 运行态机台编码，用于单控折半
     * @return 调整后的产能图
     */
    public static Map<Integer, Integer> applyFirstInspectionQtyToCapacityMap(
            LhScheduleContext context,
            List<LhShiftConfigVO> shifts,
            LhShiftConfigVO attributionShift,
            Map<Integer, Integer> shiftCapacityMap,
            int shiftCapacity,
            int remainingQty,
            String scheduleType,
            String machineCode) {
        return applyFirstInspectionQtyToCapacityMap(
                context, null, shifts, attributionShift, shiftCapacityMap,
                shiftCapacity, remainingQty, scheduleType, machineCode);
    }

    /**
     * 按 SKU 类型和指定首检归属班次调整产能图。
     *
     * <p>试制 SKU 不补首检条数，仅把中班正常生产上限压缩到实际中班班产的75%；
     * 非试制 SKU 继续把首检条数与正常生产量合并，并共享现有班产上限。</p>
     *
     * @param context 排程上下文
     * @param sku SKU排程信息，传null时保持原有非试制逻辑
     * @param shifts 排程窗口班次
     * @param attributionShift 首检归属班次
     * @param shiftCapacityMap 正常生产产能图
     * @param shiftCapacity 运行态班产
     * @param remainingQty 当前剩余目标量
     * @param scheduleType 排程类型
     * @param machineCode 运行态机台编码
     * @return 调整后的产能图
     */
    public static Map<Integer, Integer> applyFirstInspectionQtyToCapacityMap(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            List<LhShiftConfigVO> shifts,
            LhShiftConfigVO attributionShift,
            Map<Integer, Integer> shiftCapacityMap,
            int shiftCapacity,
            int remainingQty,
            String scheduleType,
            String machineCode) {
        Map<Integer, Integer> adjustedMap = new LinkedHashMap<Integer, Integer>(
                CollectionUtils.isEmpty(shifts) ? 0 : shifts.size());
        if (CollectionUtils.isEmpty(shifts)) {
            return adjustedMap;
        }
        int firstInspectionQty = resolvePreviewFirstInspectionQty(
                context, sku, attributionShift, shiftCapacity, remainingQty, scheduleType, machineCode);
        for (LhShiftConfigVO shift : shifts) {
            if (Objects.isNull(shift) || Objects.isNull(shift.getShiftIndex())) {
                continue;
            }
            Integer originalCapacity = CollectionUtils.isEmpty(shiftCapacityMap)
                    ? null : shiftCapacityMap.get(shift.getShiftIndex());
            if (Objects.nonNull(originalCapacity)
                    && Objects.nonNull(attributionShift)
                    && Objects.equals(shift.getShiftIndex(), attributionShift.getShiftIndex())
                    && isTrialTimeBasedFirstInspection(sku, attributionShift, scheduleType)) {
                adjustedMap.put(shift.getShiftIndex(), resolveTrialCapacityAfterFirstInspection(
                        context, sku, shift, originalCapacity, shiftCapacity, scheduleType, machineCode));
                continue;
            }
            if (Objects.nonNull(attributionShift)
                    && Objects.equals(shift.getShiftIndex(), attributionShift.getShiftIndex())
                    && firstInspectionQty > 0) {
                int normalCapacity = Math.max(0, originalCapacity == null ? 0 : originalCapacity);
                int cap = resolveShiftCapacityCap(context, shift, shiftCapacity, scheduleType);
                adjustedMap.put(shift.getShiftIndex(),
                        firstInspectionQty + Math.min(normalCapacity, Math.max(0, cap - firstInspectionQty)));
                continue;
            }
            if (originalCapacity != null) {
                adjustedMap.put(shift.getShiftIndex(), Math.max(0, originalCapacity));
            }
        }
        return adjustedMap;
    }

    /**
     * 按胎胚最早可供时间路径调整首检班次的部分班次总产能。
     *
     * <p>该方法供 S4.5 新增排产及 S4.4 结构切换提前生产命中胎胚时间配置时调用。
     * 普通 SKU 的首检条数属于部分班次总产能的一部分，因此不再额外叠加；若残余
     * 总产能不足完整首检，当前班次直接归零。试制 SKU 不生成首检条数，仍按现有规则
     * 固定扣减两小时对应产能。</p>
     *
     * @param context 排程上下文
     * @param sku 当前待排 SKU
     * @param shifts 当前业务日班次
     * @param attributionShift 实际生产开始班次，即首检归属班次
     * @param shiftCapacityMap 已按实际生产开始时间折算的班次总产能
     * @param shiftCapacity 运行态完整班产
     * @param remainingQty 当前候选目标量
     * @param scheduleType 排程类型
     * @param machineCode 机台编码
     * @return 胎胚时间及首检共同收口后的班次总产能图
     */
    public static Map<Integer, Integer> applyEmbryoAvailableFirstInspectionCapacity(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            List<LhShiftConfigVO> shifts,
            LhShiftConfigVO attributionShift,
            Map<Integer, Integer> shiftCapacityMap,
            int shiftCapacity,
            int remainingQty,
            String scheduleType,
            String machineCode) {
        Map<Integer, Integer> adjustedMap = new LinkedHashMap<Integer, Integer>(
                CollectionUtils.isEmpty(shifts) ? 0 : shifts.size());
        if (CollectionUtils.isEmpty(shifts)) {
            return adjustedMap;
        }
        int firstInspectionQty = resolvePreviewFirstInspectionQty(
                context, sku, attributionShift, shiftCapacity, remainingQty, scheduleType, machineCode);
        for (LhShiftConfigVO shift : shifts) {
            if (Objects.isNull(shift) || Objects.isNull(shift.getShiftIndex())) {
                continue;
            }
            Integer originalCapacity = CollectionUtils.isEmpty(shiftCapacityMap)
                    ? null : shiftCapacityMap.get(shift.getShiftIndex());
            if (Objects.isNull(originalCapacity)) {
                continue;
            }
            int adjustedCapacity = Math.max(0, originalCapacity);
            if (Objects.nonNull(attributionShift)
                    && Objects.equals(shift.getShiftIndex(), attributionShift.getShiftIndex())) {
                adjustedCapacity = resolveEmbryoAvailableShiftCapacity(
                        context, sku, shift, adjustedCapacity, firstInspectionQty,
                        shiftCapacity, scheduleType, machineCode);
            }
            adjustedMap.put(shift.getShiftIndex(), adjustedCapacity);
        }
        return adjustedMap;
    }

    /**
     * 解析胎胚时间所在部分班次扣除首检后的可用总产能。
     *
     * @param context 排程上下文
     * @param sku 当前待排 SKU
     * @param shift 首检归属班次
     * @param partialShiftCapacity 从实际开始时间至班次结束的物理总产能
     * @param firstInspectionQty 普通 SKU 完整首检条数
     * @param shiftCapacity 运行态完整班产
     * @param scheduleType 排程类型
     * @param machineCode 机台编码
     * @return 普通 SKU 返回可容纳完整首检的部分班次总产能；试制返回扣除两小时后的生产产能
     */
    public static int resolveEmbryoAvailableShiftCapacity(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LhShiftConfigVO shift,
            int partialShiftCapacity,
            int firstInspectionQty,
            int shiftCapacity,
            String scheduleType,
            String machineCode) {
        int currentCapacity = Math.max(0, partialShiftCapacity);
        if (isTrialTimeBasedFirstInspection(sku, shift, scheduleType)) {
            int actualShiftCapacity = resolveShiftCapacityCap(context, shift, shiftCapacity, scheduleType);
            int inspectionCapacity = (int) ((long) actualShiftCapacity * TRIAL_FIRST_INSPECTION_HOURS
                    / TRIAL_SHIFT_DURATION_HOURS);
            int finalCapacity = Math.max(0, currentCapacity - inspectionCapacity);
            log.debug("试制SKU胎胚可供部分班次首检产能收口, batchNo: {}, materialCode: {}, machineCode: {}, "
                            + "班次: {}, 部分班次总产能: {}, 固定2小时首检折算量: {}, 剩余生产产能: {}",
                    Objects.isNull(context) ? null : context.getBatchNo(),
                    Objects.isNull(sku) ? null : sku.getMaterialCode(), machineCode,
                    Objects.isNull(shift) ? null : shift.getShiftIndex(),
                    currentCapacity, inspectionCapacity, finalCapacity);
            return finalCapacity;
        }
        if (firstInspectionQty > 0 && currentCapacity < firstInspectionQty) {
            return 0;
        }
        return currentCapacity;
    }

    /**
     * 解析当前班次扣除首检后的正常生产上限。
     *
     * @param context 排程上下文
     * @param shift 当前班次
     * @param shiftMaxQty 当前班次正常生产上限
     * @param firstInspectionShiftIndex 首检归属班次
     * @param firstInspectionQty 首检数量
     * @param shiftCapacity 运行态班产
     * @param scheduleType 排程类型
     * @return 扣除首检占用后的正常生产上限
     */
    public static int resolveNormalCapacityAfterFirstInspection(
            LhScheduleContext context,
            LhShiftConfigVO shift,
            int shiftMaxQty,
            int firstInspectionShiftIndex,
            int firstInspectionQty,
            int shiftCapacity,
            String scheduleType) {
        return resolveNormalCapacityAfterFirstInspection(
                context, null, shift, shiftMaxQty, firstInspectionShiftIndex,
                firstInspectionQty, shiftCapacity, scheduleType, null);
    }

    /**
     * 按 SKU 类型解析首检后的正常生产上限。
     *
     * <p>试制 SKU 不扣首检条数，而是把首检归属中班的最大生产量限制为实际班产的75%；
     * 非试制 SKU 仍按首检条数占用班产。调用方继续在该上限之外叠加停机、清洗、保养、
     * 日标准产量和剩余目标量约束，最终自然取各类上限的最小值。</p>
     *
     * @param context 排程上下文
     * @param sku SKU排程信息，传null时保持原有非试制逻辑
     * @param shift 当前班次
     * @param shiftMaxQty 当前班次正常生产上限
     * @param firstInspectionShiftIndex 首检归属班次
     * @param firstInspectionQty 首检数量
     * @param shiftCapacity 运行态班产
     * @param scheduleType 排程类型
     * @param machineCode 机台编码，用于记录诊断日志
     * @return 首检规则收口后的正常生产上限
     */
    public static int resolveNormalCapacityAfterFirstInspection(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LhShiftConfigVO shift,
            int shiftMaxQty,
            int firstInspectionShiftIndex,
            int firstInspectionQty,
            int shiftCapacity,
            String scheduleType,
            String machineCode) {
        if (Objects.isNull(shift) || !Objects.equals(shift.getShiftIndex(), firstInspectionShiftIndex)) {
            return Math.max(0, shiftMaxQty);
        }
        if (isTrialTimeBasedFirstInspection(sku, shift, scheduleType)) {
            return resolveTrialCapacityAfterFirstInspection(
                    context, sku, shift, shiftMaxQty, shiftCapacity, scheduleType, machineCode);
        }
        if (firstInspectionQty <= 0) {
            return Math.max(0, shiftMaxQty);
        }
        int cap = resolveShiftCapacityCap(context, shift, shiftCapacity, scheduleType);
        return Math.min(Math.max(0, shiftMaxQty), Math.max(0, cap - firstInspectionQty));
    }

    /**
     * 按胎胚可供部分班次口径解析首检后的正常生产量。
     *
     * <p>仅在 S4.5 新增排产或 S4.4 结构切换提前生产命中胎胚时间配置时启用。
     * 传入的 shiftMaxQty 已是从实际开始时间到班次结束的物理产能，普通 SKU 需直接
     * 从该产能扣除首检条数；试制 SKU 需直接扣除固定两小时产能。未启用时完整复用
     * 原方法，其他排程入口不受影响。</p>
     *
     * @param context 排程上下文
     * @param sku 当前待排 SKU
     * @param shift 当前班次
     * @param shiftMaxQty 当前部分班次物理产能
     * @param firstInspectionShiftIndex 首检归属班次
     * @param firstInspectionQty 普通 SKU 首检条数
     * @param shiftCapacity 运行态完整班产
     * @param scheduleType 排程类型
     * @param machineCode 机台编码
     * @param embryoAvailableTimeConstrained 是否启用胎胚时间部分班次口径
     * @return 首检扣减后的正常生产量
     */
    public static int resolveNormalCapacityAfterFirstInspection(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LhShiftConfigVO shift,
            int shiftMaxQty,
            int firstInspectionShiftIndex,
            int firstInspectionQty,
            int shiftCapacity,
            String scheduleType,
            String machineCode,
            boolean embryoAvailableTimeConstrained) {
        if (!embryoAvailableTimeConstrained
                || Objects.isNull(shift)
                || !Objects.equals(shift.getShiftIndex(), firstInspectionShiftIndex)) {
            return resolveNormalCapacityAfterFirstInspection(
                    context, sku, shift, shiftMaxQty, firstInspectionShiftIndex,
                    firstInspectionQty, shiftCapacity, scheduleType, machineCode);
        }
        int availableCapacity = resolveEmbryoAvailableShiftCapacity(
                context, sku, shift, shiftMaxQty, firstInspectionQty,
                shiftCapacity, scheduleType, machineCode);
        if (isTrialTimeBasedFirstInspection(sku, shift, scheduleType)) {
            return availableCapacity;
        }
        return Math.max(0, availableCapacity - Math.max(0, firstInspectionQty));
    }

    /**
     * 解析首检归属班次索引。
     *
     * @param shifts 排程窗口班次
     * @param mouldChangeCompleteTime 换模完成时间
     * @return 班次索引，未命中返回 -1
     */
    public static int resolveAttributionShiftIndex(List<LhShiftConfigVO> shifts, Date mouldChangeCompleteTime) {
        LhShiftConfigVO shift = resolveAttributionShift(shifts, mouldChangeCompleteTime);
        return shift == null || shift.getShiftIndex() == null ? -1 : shift.getShiftIndex();
    }

    private static int resolveEffectiveFirstInspectionQty(LhScheduleContext context,
                                                          SkuScheduleDTO sku,
                                                          LhShiftConfigVO attributionShift,
                                                          int shiftCapacity,
                                                          int remainingQty,
                                                          String scheduleType,
                                                          String machineCode,
                                                          int sequence,
                                                          boolean logOddSingleControlParam) {
        if (Objects.isNull(attributionShift) || remainingQty <= 0) {
            return 0;
        }
        if (isTrialTimeBasedFirstInspection(sku, attributionShift, scheduleType)) {
            return 0;
        }
        // 按机台类型折算首检数量：单控机台（L/R）按参数折半向下取整
        int configuredQty = resolveAdjustedFirstInspectionQty(
                context, sequence, machineCode, logOddSingleControlParam);
        if (configuredQty <= 0) {
            return 0;
        }
        int cap = resolveShiftCapacityCap(context, attributionShift, shiftCapacity, scheduleType);
        return Math.max(0, Math.min(Math.min(configuredQty, remainingQty), cap));
    }

    /**
     * 计算试制 SKU 扣除固定2小时首检后的中班正常生产上限。
     *
     * <p>先按现有奇数班产修正规则取得当前中班实际班产，再按6/8向下取整。
     * 最终只作为最大值上限与当前物理可排产能取小，不重复扣减停机、清洗或保养时间。</p>
     *
     * @param context 排程上下文
     * @param sku 试制SKU
     * @param shift 首检归属中班
     * @param shiftMaxQty 其他规则计算后的当前班次上限
     * @param shiftCapacity 运行态班产
     * @param scheduleType 排程类型
     * @param machineCode 机台编码
     * @return 扣除试制首检时间后的中班最大生产量
     */
    private static int resolveTrialCapacityAfterFirstInspection(LhScheduleContext context,
                                                                SkuScheduleDTO sku,
                                                                LhShiftConfigVO shift,
                                                                int shiftMaxQty,
                                                                int shiftCapacity,
                                                                String scheduleType,
                                                                String machineCode) {
        int currentCapacity = Math.max(0, shiftMaxQty);
        int actualShiftCapacity = resolveShiftCapacityCap(context, shift, shiftCapacity, scheduleType);
        int productiveHours = TRIAL_SHIFT_DURATION_HOURS - TRIAL_FIRST_INSPECTION_HOURS;
        int trialCapacityCap = (int) ((long) actualShiftCapacity * productiveHours
                / TRIAL_SHIFT_DURATION_HOURS);
        int finalCapacity = Math.min(currentCapacity, Math.max(0, trialCapacityCap));
        log.debug("试制SKU中班首检产能收口, batchNo: {}, materialCode: {}, machineCode: {}, scheduleType: {}, "
                        + "班次: {}, 原始运行态班产: {}, 奇数修正后班产: {}, 标准班次时长: {}, "
                        + "首检占用小时: {}, 首检前现有上限: {}, 试制中班75%上限: {}, 最终中班上限: {}",
                Objects.isNull(context) ? null : context.getBatchNo(),
                Objects.isNull(sku) ? null : sku.getMaterialCode(), machineCode, scheduleType,
                Objects.isNull(shift) ? null : shift.getShiftIndex(), shiftCapacity, actualShiftCapacity,
                TRIAL_SHIFT_DURATION_HOURS, TRIAL_FIRST_INSPECTION_HOURS,
                currentCapacity, trialCapacityCap, finalCapacity);
        return finalCapacity;
    }

    private static int resolveShiftCapacityCap(LhScheduleContext context,
                                               LhShiftConfigVO shift,
                                               int shiftCapacity,
                                               String scheduleType) {
        if (Objects.isNull(shift) || shiftCapacity <= 0) {
            return 0;
        }
        String configPlusShiftType = ShiftCapacityResolverUtil.resolveOddShiftCapacityPlusShiftType(context);
        return Math.max(0, ShiftCapacityResolverUtil.resolveActualShiftPlanQty(
                shiftCapacity, shift, configPlusShiftType, scheduleType));
    }

    private static String buildShiftCounterKey(LhShiftConfigVO shift) {
        if (Objects.isNull(shift) || Objects.isNull(shift.getShiftIndex())
                || Objects.isNull(shift.getWorkDate())) {
            return null;
        }
        return LhScheduleTimeUtil.formatDate(shift.getWorkDate())
                + SHIFT_COUNTER_KEY_SEPARATOR + shift.getShiftIndex();
    }

    private static boolean isTrialMorningSwitchAttribution(SkuScheduleDTO sku,
                                                           LhShiftConfigVO defaultShift,
                                                           String scheduleType) {
        if (Objects.isNull(sku) || Objects.isNull(defaultShift)) {
            return false;
        }
        if (!Objects.equals(ConstructionStageEnum.TRIAL.getCode(), sku.getConstructionStage())) {
            return false;
        }
        if (!Objects.equals(ScheduleTypeEnum.NEW_SPEC.getCode(), scheduleType)
                && !Objects.equals(ScheduleTypeEnum.TYPE_BLOCK.getCode(), scheduleType)) {
            return false;
        }
        return defaultShift.isMorningShift();
    }

    /**
     * 判断当前首检是否采用试制 SKU 固定2小时的时间扣减规则。
     *
     * @param sku SKU排程信息
     * @param attributionShift 首检归属班次
     * @param scheduleType 排程类型
     * @return true-试制新增/换活字块中班首检；false-沿用首检条数规则
     */
    public static boolean isTrialTimeBasedFirstInspection(SkuScheduleDTO sku,
                                                          LhShiftConfigVO attributionShift,
                                                          String scheduleType) {
        if (Objects.isNull(sku) || Objects.isNull(attributionShift)
                || !attributionShift.isAfternoonShift()) {
            return false;
        }
        if (!Objects.equals(ConstructionStageEnum.TRIAL.getCode(), sku.getConstructionStage())) {
            return false;
        }
        return Objects.equals(ScheduleTypeEnum.NEW_SPEC.getCode(), scheduleType)
                || Objects.equals(ScheduleTypeEnum.TYPE_BLOCK.getCode(), scheduleType);
    }

    private static LhShiftConfigVO resolveAfternoonShiftOnSameWorkDate(List<LhShiftConfigVO> shifts,
                                                                       LhShiftConfigVO morningShift) {
        if (CollectionUtils.isEmpty(shifts) || Objects.isNull(morningShift)
                || Objects.isNull(morningShift.getWorkDate())) {
            return null;
        }
        String morningWorkDate = LhScheduleTimeUtil.formatDate(morningShift.getWorkDate());
        for (LhShiftConfigVO shift : shifts) {
            if (Objects.isNull(shift) || Objects.isNull(shift.getWorkDate()) || !shift.isAfternoonShift()) {
                continue;
            }
            if (Objects.equals(morningWorkDate, LhScheduleTimeUtil.formatDate(shift.getWorkDate()))) {
                return shift;
            }
        }
        return null;
    }

    private static String resolveSceneName(String scheduleType) {
        if (Objects.equals(ScheduleTypeEnum.TYPE_BLOCK.getCode(), scheduleType)) {
            return "换活字块";
        }
        return "换模";
    }
}
