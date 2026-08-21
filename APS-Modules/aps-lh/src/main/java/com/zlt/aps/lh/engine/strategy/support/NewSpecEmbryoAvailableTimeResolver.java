package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.SkuDailyPlanQuotaDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.enums.ConstructionStageEnum;
import com.zlt.aps.lh.api.enums.ScheduleTypeEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 新增规格生产时间下限解析器。
 *
 * <p>本解析器统一提供 S4.4 结构切换提前生产、S4.5 新增排产需要的胎胚时间读取、
 * SKU 生产时间下限和半开班次定位能力。生产下限只限制首检及正式生产，
 * 不直接移动换模或换活字块等准备动作；调用方可以在排程窗口内提前完成准备，但最终
 * 开产时间必须取既有规则理论开产时间、试制/量试中班门禁和胎胚可供时间中的较晚值。</p>
 *
 * <p>正规、小批量 SKU 不再设置“当前业务日首班”生产门禁，实际开产时间只取
 * max(换模/首检完成时间, 胎胚可供时间)；试制（X）和量试（T）统一从首次正计划日的
 * 中班开始生产。
 * 日计划判断只读取 {@link SkuDailyPlanQuotaDTO#getDayPlanQty()} 原始月计划节奏，禁止读取
 * 排程过程中会被收尾补量、欠产滚动和实际扣账持续修改的 remainingQty。</p>
 *
 * @author APS
 */
public final class NewSpecEmbryoAvailableTimeResolver {

    /** 胎胚时间超出三天排程窗口时的统一未排原因 */
    public static final String OUT_OF_SCHEDULE_WINDOW_REASON = "胎胚最早可供硫化时间超出排程窗口";

    /** 当前业务日尚未到达胎胚时间时的延期原因 */
    public static final String NOT_AVAILABLE_IN_CURRENT_DAY_REASON = "胎胚最早可供硫化时间尚未到达当前业务日";

    /** 结果行换活字块标识：1-换活字块，0-普通续作或新增 */
    private static final String TYPE_BLOCK_RESULT_YES = "1";

    private NewSpecEmbryoAvailableTimeResolver() {
    }

    /**
     * 按 SKU 结构名称获取胎胚最早可供硫化时间。
     *
     * <p>结构名称直接作为 Map 精确键，不做 trim 或大小写转换，保证与配置表
     * STRUCTURE_NAME 的大小写敏感匹配口径一致。</p>
     *
     * @param context 排程上下文
     * @param sku 待排 SKU
     * @return 命中有效配置时返回最早可供时间，否则返回 null
     */
    public static Date resolveEarliestAvailableTime(LhScheduleContext context, SkuScheduleDTO sku) {
        if (Objects.isNull(context) || Objects.isNull(sku)
                || StringUtils.isEmpty(sku.getStructureName())) {
            return null;
        }
        Map<String, Date> earliestTimeMap = context.getStructureEarliestLhTimeMap();
        if (CollectionUtils.isEmpty(earliestTimeMap)) {
            return null;
        }
        return earliestTimeMap.get(sku.getStructureName());
    }

    /**
     * 判断当前 SKU 是否命中原始胎胚时间配置。
     *
     * <p>该方法只判断配置是否存在，不考虑本批次同结构续作排产对配置的抑制；需要判断
     * 当前是否真正生效时，调用 {@link #isEffectiveConstrained(LhScheduleContext, SkuScheduleDTO)}。</p>
     *
     * @param context 排程上下文
     * @param sku 待排 SKU
     * @return true-命中原始时间配置；false-未命中原始时间配置
     */
    public static boolean isConstrained(LhScheduleContext context, SkuScheduleDTO sku) {
        return Objects.nonNull(resolveEarliestAvailableTime(context, sku));
    }

    /**
     * 判断当前 SKU 对应结构在本次续作排产中是否已经形成有效排产结果。
     *
     * <p>不能只判断 {@code continuousSkuList} 是否包含该结构，因为续作列表中可能存在
     * 当前窗口无计划、被释放或最终被裁剪为零量的 SKU。这里以当前批次结果列表中的有效
     * 续作结果作为“有排过”的唯一口径；换活字块结果不属于本判断范围。</p>
     *
     * @param context 排程上下文
     * @param sku 待排 SKU
     * @return true-同结构已经形成有效续作排产；false-未形成有效续作排产
     */
    public static boolean isStructureScheduledInCurrentContinuation(
            LhScheduleContext context, SkuScheduleDTO sku) {
        if (Objects.isNull(context) || Objects.isNull(sku)
                || StringUtils.isEmpty(sku.getStructureName())
                || CollectionUtils.isEmpty(context.getScheduleResultList())) {
            return false;
        }
        return context.getScheduleResultList().stream()
                .filter(Objects::nonNull)
                .filter(result -> StringUtils.equals(
                        ScheduleTypeEnum.CONTINUOUS.getCode(), result.getScheduleType()))
                .filter(result -> !StringUtils.equals(
                        TYPE_BLOCK_RESULT_YES, result.getIsTypeBlock()))
                .filter(result -> StringUtils.equals(
                        sku.getStructureName(), result.getStructureName()))
                .anyMatch(result -> ShiftFieldUtil.resolveScheduledQty(result) > 0);
    }

    /**
     * 获取当前批次实际生效的胎胚最早可供硫化时间。
     *
     * <p>同结构已经在本次续作中形成有效排产时，说明该结构已经完成续作衔接，后续 SKU
     * 不再受配置的最早胎胚时间限制；其余场景继续返回原始结构配置时间。</p>
     *
     * @param context 排程上下文
     * @param sku 待排 SKU
     * @return 当前实际生效的胎胚可供时间；被续作结构规则抑制或未配置时返回 null
     */
    public static Date resolveEffectiveEarliestAvailableTime(
            LhScheduleContext context, SkuScheduleDTO sku) {
        if (isStructureScheduledInCurrentContinuation(context, sku)) {
            return null;
        }
        return resolveEarliestAvailableTime(context, sku);
    }

    /**
     * 判断当前 SKU 是否命中当前批次实际生效的胎胚时间约束。
     *
     * @param context 排程上下文
     * @param sku 待排 SKU
     * @return true-胎胚时间实际生效；false-未配置或已被续作结构规则抑制
     */
    public static boolean isEffectiveConstrained(LhScheduleContext context, SkuScheduleDTO sku) {
        return Objects.nonNull(resolveEffectiveEarliestAvailableTime(context, sku));
    }

    /**
     * 解析当前新增 SKU 正式生产不得早于的统一时间。
     *
     * <p>正规、小批量 SKU 只返回当前生效的胎胚最早可供时间（未配置或被续作结构规则
     * 抑制时返回 null）；试制、量试 SKU 返回“首次正计划日中班”与当前生效胎胚时间
     * 中的较晚值。试制、量试中班门禁不会因胎胚时间被抑制而消失。</p>
     *
     * @param context 排程上下文，提供当前日驱动业务日和胎胚可供时间
     * @param sku 待排 SKU
     * @param shifts 完整排程窗口班次；必须保持时间升序
     * @return 正式生产时间下限；未配置任何下限时返回 null
     */
    public static Date resolveProductionNotBeforeTime(LhScheduleContext context,
                                                      SkuScheduleDTO sku,
                                                      List<LhShiftConfigVO> shifts) {
        Date skuProductionGateTime = resolveSkuProductionGateTime(context, sku, shifts);
        Date earliestEmbryoAvailableTime = resolveEffectiveEarliestAvailableTime(context, sku);
        return resolveActualProductionStartTime(
                skuProductionGateTime, earliestEmbryoAvailableTime);
    }

    /**
     * 解析 SKU 类型对应的开产门禁时间。
     *
     * <p>正规、小批量 SKU 不再设置 SKU 类型门禁，直接返回 null，实际开产时间由调用方
     * 按“max(换模/首检完成时间, 胎胚可供时间)”计算。试制、量试 SKU 保留首次正计划日中班
     * 的硬下限，防止候选扩展后误在早班开产。</p>
     *
     * @param context 排程上下文
     * @param sku 待排 SKU
     * @param shifts 完整排程窗口班次
     * @return SKU 类型门禁时间；正规及小批量返回 null，试制量试首次计划日在窗口外时返回该日零点供调用方判定超窗
     */
    public static Date resolveSkuProductionGateTime(LhScheduleContext context,
                                                    SkuScheduleDTO sku,
                                                    List<LhShiftConfigVO> shifts) {
        if (Objects.isNull(context) || Objects.isNull(sku) || CollectionUtils.isEmpty(shifts)) {
            return null;
        }
        // 去除正规/小批量的生产门禁，只保留试制、量试“首次正计划日中班开产”的 SKU 类型下限。
        boolean trialOrMassTrial = isTrialOrMassTrial(sku);
        if (!trialOrMassTrial) {
            return null;
        }
        LocalDate currentBusinessDate = resolveCurrentBusinessDate(context, shifts);
        if (Objects.isNull(currentBusinessDate)) {
            return null;
        }
        LocalDate eligibilityDate = currentBusinessDate;
        LocalDate firstPositivePlanDate = resolveFirstPositivePlanDate(
                context, sku, currentBusinessDate);
        if (Objects.nonNull(firstPositivePlanDate)
                && firstPositivePlanDate.isAfter(eligibilityDate)) {
            eligibilityDate = firstPositivePlanDate;
        }
        Date gateTime = resolveShiftStartOnOrAfterDate(
                shifts, eligibilityDate, true);
        if (Objects.nonNull(gateTime)) {
            return gateTime;
        }
        /*
         * 首次正计划日在当前八班窗口之外时，返回该日零点作为窗口外下限。
         * 调用方随后会按窗口结束时间判定延期/未排；这里不使用固定14:00魔法值，
         * 避免未来工厂班次配置调整后仍按旧时刻错误放行。
         */
        return Date.from(eligibilityDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 解析 X/T 首次正日计划日期。
     *
     * <p>优先扫描当前 SKU 的原始 dayPlanQty；当前窗口没有正计划但存在本月历史欠产时，
     * 说明首次计划日已经到达，以当前业务日作为补排准入日；其余场景复用现有提前生产日期
     * 解析器继续查找阈值内未来计划日。</p>
     *
     * @param context 排程上下文
     * @param sku 试制或量试 SKU
     * @param currentBusinessDate 当前日驱动业务日
     * @return 首次正计划日期；无法解析时返回 null
     */
    private static LocalDate resolveFirstPositivePlanDate(LhScheduleContext context,
                                                          SkuScheduleDTO sku,
                                                          LocalDate currentBusinessDate) {
        LocalDate firstPositivePlanDate = null;
        if (!CollectionUtils.isEmpty(sku.getDailyPlanQuotaMap())) {
            for (Map.Entry<LocalDate, SkuDailyPlanQuotaDTO> entry
                    : sku.getDailyPlanQuotaMap().entrySet()) {
                if (Objects.isNull(entry.getKey()) || Objects.isNull(entry.getValue())
                        || entry.getValue().getDayPlanQty() <= 0) {
                    continue;
                }
                if (Objects.isNull(firstPositivePlanDate)
                        || entry.getKey().isBefore(firstPositivePlanDate)) {
                    firstPositivePlanDate = entry.getKey();
                }
            }
        }
        if (Objects.nonNull(firstPositivePlanDate)) {
            return firstPositivePlanDate;
        }
        if (Math.max(0, sku.getMonthlyHistoryShortageQty()) > 0) {
            return currentBusinessDate;
        }
        return EarlyProductionChecker.resolveFirstFuturePlanDate(
                context, sku, currentBusinessDate);
    }

    /**
     * 在窗口班次中查找指定业务日及之后的首个合法开产班次。
     *
     * @param shifts 完整排程窗口班次
     * @param eligibilityDate 最早允许开产的业务日期
     * @param afternoonOnly true-只允许中班；false-允许当日首班
     * @return 合法班次开始时间；窗口内没有对应班次时返回 null
     */
    private static Date resolveShiftStartOnOrAfterDate(List<LhShiftConfigVO> shifts,
                                                       LocalDate eligibilityDate,
                                                       boolean afternoonOnly) {
        for (LhShiftConfigVO shift : shifts) {
            LocalDate workDate = resolveShiftWorkDate(shift);
            if (Objects.isNull(workDate) || workDate.isBefore(eligibilityDate)
                    || (afternoonOnly && !shift.isAfternoonShift())
                    || Objects.isNull(shift.getShiftStartDateTime())) {
                continue;
            }
            return shift.getShiftStartDateTime();
        }
        return null;
    }

    /**
     * 解析日驱动主链当前正在处理的业务日。
     *
     * @param context 排程上下文
     * @param shifts 完整排程窗口班次
     * @return 当前业务日；上下文未设置时回退到窗口首班业务日
     */
    private static LocalDate resolveCurrentBusinessDate(LhScheduleContext context,
                                                        List<LhShiftConfigVO> shifts) {
        if (Objects.nonNull(context.getCurrentScheduleDate())) {
            return context.getCurrentScheduleDate().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
        }
        for (LhShiftConfigVO shift : shifts) {
            LocalDate workDate = resolveShiftWorkDate(shift);
            if (Objects.nonNull(workDate)) {
                return workDate;
            }
        }
        return null;
    }

    /**
     * 解析班次业务日期，优先使用班次 workDate，兼容仅初始化起止时间的测试和历史调用。
     *
     * @param shift 班次配置
     * @return 班次业务日期；无法解析时返回 null
     */
    private static LocalDate resolveShiftWorkDate(LhShiftConfigVO shift) {
        if (Objects.isNull(shift)) {
            return null;
        }
        Date workDate = shift.getWorkDate();
        Date sourceDate = Objects.nonNull(workDate)
                ? workDate : shift.getShiftStartDateTime();
        return Objects.isNull(sourceDate) ? null : sourceDate.toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * 判断 SKU 是否为需要中班首次开产的试制或量试状态。
     *
     * @param sku 待排 SKU
     * @return true-试制或量试；false-正规、小批量或其它状态
     */
    private static boolean isTrialOrMassTrial(SkuScheduleDTO sku) {
        return Objects.nonNull(sku)
                && (StringUtils.equals(
                        ConstructionStageEnum.TRIAL.getCode(), sku.getConstructionStage())
                || StringUtils.equals(
                        ConstructionStageEnum.MASS_TRIAL.getCode(), sku.getConstructionStage()));
    }

    /**
     * 计算两个生产时间下限收敛后的实际开始时间。
     *
     * @param theoreticalProductionStartTime 现有规则或 SKU 类型门禁确定的生产下限
     * @param earliestAvailableTime 胎胚可供时间或其它需要继续叠加的生产下限
     * @return 两个时间中的较晚值；任一为空时返回另一有效时间
     */
    public static Date resolveActualProductionStartTime(Date theoreticalProductionStartTime,
                                                        Date earliestAvailableTime) {
        if (Objects.isNull(theoreticalProductionStartTime)) {
            return earliestAvailableTime;
        }
        if (Objects.isNull(earliestAvailableTime)
                || theoreticalProductionStartTime.after(earliestAvailableTime)) {
            return theoreticalProductionStartTime;
        }
        return earliestAvailableTime;
    }

    /**
     * 计算命中统一生产门禁后当前班次真正允许生产的开始时间。
     *
     * <p>班次管控窗口只说明该班次允许生产的边界，不能覆盖实际生产时间下限。
     * 因此，命中生产门禁时必须在班次管控开始时间和实际生产开始时间之间取较晚值；
     * 若较晚值已到达班次管控结束时间，则该班次没有任何可用于首检或正式生产的时间。</p>
     *
     * @param controlStartTime 班次管控后的可生产开始时间
     * @param controlEndTime 班次管控后的可生产结束时间
     * @param actualProductionStartTime SKU 类型门禁、胎胚约束及既有规则共同确定的实际生产开始时间
     * @return 当前班次有效生产开始时间；当前班次无有效窗口时返回 null
     */
    public static Date resolveEffectiveProductionWindowStart(Date controlStartTime,
                                                              Date controlEndTime,
                                                              Date actualProductionStartTime) {
        if (Objects.isNull(controlStartTime) || Objects.isNull(controlEndTime)
                || !controlStartTime.before(controlEndTime)) {
            return null;
        }
        Date effectiveStartTime = Objects.nonNull(actualProductionStartTime)
                && actualProductionStartTime.after(controlStartTime)
                ? actualProductionStartTime : controlStartTime;
        return effectiveStartTime.before(controlEndTime) ? effectiveStartTime : null;
    }

    /**
     * 计算有效生产时间窗的秒数。
     *
     * <p>部分班次产能折算必须使用实际参与生产的时间窗长度，而不是完整班次长度；
     * 该方法为命中统一生产门禁的新增排产调用方提供统一口径。</p>
     *
     * @param windowStartTime 有效生产开始时间
     * @param windowEndTime 有效生产结束时间
     * @return 有效生产秒数；窗口无效时返回 0
     */
    public static long resolveProductionWindowSeconds(Date windowStartTime, Date windowEndTime) {
        if (Objects.isNull(windowStartTime) || Objects.isNull(windowEndTime)
                || !windowStartTime.before(windowEndTime)) {
            return 0L;
        }
        return Math.max(0L, (windowEndTime.getTime() - windowStartTime.getTime()) / 1000L);
    }

    /**
     * 按左闭右开区间定位实际生产班次。
     *
     * <p>时间等于班次开始时归当前班；时间等于班次结束时不归当前班，
     * 由下一个班次继续匹配，避免在胎胚可供前一班生成首检或计划量。</p>
     *
     * @param shifts 待定位的有序班次列表
     * @param productionStartTime 实际生产开始时间
     * @return 命中的生产班次；未命中返回 null
     */
    public static LhShiftConfigVO resolveProductionShift(List<LhShiftConfigVO> shifts,
                                                         Date productionStartTime) {
        if (CollectionUtils.isEmpty(shifts) || Objects.isNull(productionStartTime)) {
            return null;
        }
        for (LhShiftConfigVO shift : shifts) {
            if (Objects.isNull(shift)
                    || Objects.isNull(shift.getShiftStartDateTime())
                    || Objects.isNull(shift.getShiftEndDateTime())) {
                continue;
            }
            if (!productionStartTime.before(shift.getShiftStartDateTime())
                    && productionStartTime.before(shift.getShiftEndDateTime())) {
                return shift;
            }
        }
        return null;
    }

    /**
     * 判断统一生产时间下限是否已经到达或越过指定业务日结束时间。
     *
     * @param productionNotBeforeTime SKU 类型门禁与胎胚可供时间收敛后的生产下限
     * @param dayEndTime 当前业务日结束时间
     * @return true-当前业务日不可生产；false-当前业务日内可继续试排
     */
    public static boolean reachesOrPassesDayEnd(Date productionNotBeforeTime, Date dayEndTime) {
        return Objects.nonNull(productionNotBeforeTime)
                && Objects.nonNull(dayEndTime)
                && !productionNotBeforeTime.before(dayEndTime);
    }
}
