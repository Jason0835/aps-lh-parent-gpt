package com.zlt.aps.lh.component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cx.constant.ScheduleConstants;
import com.zlt.aps.cx.entity.config.CxEmbryoLhTime;
import com.zlt.aps.lh.api.domain.entity.LhShiftConfig;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.enums.ShiftEnum;
import com.zlt.aps.lh.api.util.ShiftBoundaryUtil;
import com.zlt.aps.lh.api.util.ShiftTypeParseUtil;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.handler.ScheduleAdjustHandler;
import com.zlt.aps.lh.mapper.CxEmbryoLhTimeMapper;
import com.zlt.aps.lh.mapper.LhMpStructureAllocationMapper;
import com.zlt.aps.lh.mapper.LhShiftConfigMapper;
import com.zlt.aps.lh.mapper.MdmStructureLhRatioMapper;
import com.zlt.aps.lh.mapper.MdmSkuScheduleCategoryMapper;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.mdm.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuLhCapacity;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuScheduleCategory;
import com.zlt.aps.mdm.api.domain.entity.MdmWorkCalendar;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.mp.api.domain.entity.MdmStructureLhRatio;
import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 结构可收尾时间重算服务：按最新成型余量与机台班产，重算并回写 T_CX_EMBRYO_LH_TIME。
 *
 * <p><b>目的</b>：成型排程写入的 {@code ENDING_TIME} / {@code EARLIEST_LH_TIME} 是排程批次时点的
 * 静态快照；硫化排程开工前现场余量已经变化，继续用旧快照会给出偏离实际的「胎胚最早可供硫化时间」。
 * 本服务以参考时刻的最新余量重算一次，让硫化拿到更准的时间下限。
 *
 * <p><b>两条重算路径（按记录所属的结构切换场景分流）</b>：
 *
 * <p><b>路径 1：日历锚点（降机台 / 结构交换 / 转产降机台）</b>——开口时刻由结构排产配置的切换日
 * （{@code BEGIN_DAY}）决定，与余量多少无关（余量由接力机台消化，或由转入机台接力生产）。
 * 落点与成型侧 {@code CoreScheduleAlgorithmServiceImpl#applyCalendarSwitchDeduct} 的换模扣减点同源
 * （该方法的命中条件只有 {@code kind == DOWNGRADE && nextBeginComposite == 当天}，
 * 而 {@code calculateShiftDate} 只依赖 {@code SCHEDULE_DAY}，故扣减必然落在 BEGIN_DAY 当天的第一个非停产班次）：
 * <ul>
 *   <li><b>纯降机台 / 结构交换</b>：切换日 = 该机台切换边的 {@code nextBeginComposite}；</li>
 *   <li><b>转产降机台（转出侧）</b>：切换日 = 转入机台的 {@code toBeginComposite}（等转入机台空，非原地日历日）；</li>
 *   <li><b>换英寸档（4h/8h）</b>：夜班不换模（成型侧 {@code continue} 跳过），锚点顺延到切换日的早班，
 *       即前结构在生产早班前段收尾、换模从早班开始；</li>
 *   <li><b>同规格档（2h）</b>：无班次门禁，锚点 = 切换日第一个非停产班次的开始时刻
 *       （首班为夜班时即切换日前一天深夜）。</li>
 * </ul>
 * {@code ENDING_TIME} 取锚点时刻（= 前结构实际停止生产时刻），
 * {@code EARLIEST_LH_TIME = ENDING_TIME + SWITCH_HOURS}，与表内既有约定一致。
 *
 * <p><b>路径 2：余量推算（全结构收尾 / 无接力升机台 / 提前收尾等无日历锚点的记录）</b>——
 * 口径与成型排程场景2 收尾预测逐条对齐（预测窗口常量只允许共用
 * {@link ScheduleConstants#MAX_PREDICT_SHIFTS}，禁止两处各自写 12）：
 * <ol>
 *   <li>剩余工作量：成型侧为「结构初始成型余量 − 本窗口已排产量」，本服务为「结构最新成型余量」；</li>
 *   <li>结构班产能：本服务按「结构全部胎胚的（配比 × 日硫化量）折算小时产能 × 班次时长 × 可用机台数」合计；</li>
 *   <li>还需班次 = ⌈剩余工作量 ÷ 结构班产能⌉（整数向上取整，宁晚勿早）；</li>
 *   <li>收尾班次 = 参考时刻之后的第「还需班次」个有效班次，其班次结束时刻即 ENDING_TIME；</li>
 *   <li>有效班次数超过 {@link ScheduleConstants#MAX_PREDICT_SHIFTS} 时<b>不写时间</b>
 *       （保留成型快照原值），与成型侧「没有可靠依据的时间比没有时间更危险」同口径。</li>
 * </ol>
 *
 * <p><b>成型余量口径</b>（复用硫化侧现成链路，与
 * {@code LhBaseDataServiceImpl#loadEmbryoEndingFlagMap} 完全一致）：
 * <pre>
 *   胎胚硫化余量 = Σ 该胎胚下各物料/产品状态的硫化余量（ScheduleAdjustHandler#calculatePlanSurplusQty）
 *   胎胚成型余量 = Max(0, 胎胚硫化余量 − 胎胚实时库存)
 *   结构成型余量 = Σ 该结构下全部胎胚的胎胚成型余量（非主销且 ≤ 收尾舍弃阈值按 0 计）
 * </pre>
 *
 * <p><b>只读约束</b>：本服务不改写 {@code CX_MACHINE_CODE} / {@code NEXT_STRUCTURE_NAME} /
 * {@code SWITCH_HOURS} / {@code IS_WAIT_NOTIFY} / {@code IS_BIG_INCH_CHANGE} /
 * {@code STRUCTURE_CHANGE_REMAINING}（后者语义仍是「排程前的结构成型余量」快照），
 * 重算余量只落在新增列 {@code RECALC_REMAINDER} / {@code RECALC_SHIFT_CAPACITY} / {@code RECALC_TIME}。
 *
 * <p><b>可用机台</b>只取参考日仍占机的结构排产配置段（按 BEGIN_DAY/END_DAY 覆盖区间判定），
 * 不纳入提前生产机台（本服务算的是机台收尾，不是提前开产）。
 *
 * @author APS
 */
@Slf4j
@Component
public class StructureEndingRecalcService {

    /** 早班中文标签（工作日历班次停产标记的判定顺序：早、中、夜） */
    private static final String MORNING_LABEL = "早";

    /** 中班中文标签 */
    private static final String AFTERNOON_LABEL = "中";

    /** 夜班中文标签 */
    private static final String NIGHT_LABEL = "夜";

    /** 工作日历停产标记值 */
    private static final String FULL_DAY_CLOSED_FLAG = "0";

    /** 主销产品排产类型（与 t_mdm_sku_schedule_category.SCHEDULE_TYPE='01' 一致） */
    private static final String MAIN_PRODUCT_SCHEDULE_TYPE = "01";

    /** 收尾舍弃阈值参数编码（与成型侧 SYS04050001 同源，缺省 2） */
    private static final String PARAM_ENDING_DISCARD_THRESHOLD = "SYS04050001";

    /** 收尾舍弃阈值缺省值 */
    private static final int DEFAULT_ENDING_DISCARD_THRESHOLD = 2;

    /**
     * 班次日历取数表标识：窗口班次缺失时的日模式兜底来源。
     *
     * <p>默认走硫化班次配置（{@code T_LH_SHIFT_CONFIG}）：正常路径下日模式直接取
     * {@code context.scheduleWindowShifts}（已由排程初始化解析、含绝对起止时刻），
     * 只有窗口班次为空时才回退查库。
     */
    private static final String SHIFT_CALENDAR_SOURCE_LH = "LH";

    /** 当前使用的班次日历取数表标识 */
    private static final String SHIFT_CALENDAR_SOURCE = SHIFT_CALENDAR_SOURCE_LH;

    /** 结构排产配置的正常计划类型 */
    private static final String STRUCTURE_ALLOCATION_NORMAL_PLAN_TYPE = "01";

    /** 推算收尾备注前缀 */
    private static final String REMARK_PREFIX_SURPLUS = "按最新成型余量推算收尾";

    /** 日历锚点备注前缀 */
    private static final String REMARK_PREFIX_CALENDAR = "按结构切换日历锚定收尾";

    /** 单班次默认时长（小时，班次配置缺失时兜底） */
    private static final int DEFAULT_SHIFT_DURATION_HOURS = 8;

    /** 班次日历生成时在「还需班次」之上额外扫描的天数 */
    private static final int MAX_SCAN_DAY_EXTRA = ScheduleConstants.MAX_PREDICT_SHIFTS;

    /** 日历锚点向外扫描的最大天数 */
    private static final int CALENDAR_ANCHOR_SCAN_DAYS = 40;

    /** 日历锚点重算原因：纯降机台 / 结构交换 */
    private static final String REASON_DOWNGRADE = "降机台/结构交换：按该机台切换边后结构 BEGIN_DAY 锚定";

    /** 日历锚点重算原因：转产降机台（转出侧） */
    private static final String REASON_TRANSFER_OUT = "转产降机台：按转入机台 BEGIN_DAY 锚定";

    @Resource
    private ScheduleAdjustHandler scheduleAdjustHandler;

    @Resource
    private LhMpStructureAllocationMapper lhMpStructureAllocationMapper;

    @Resource
    private LhShiftConfigMapper lhShiftConfigMapper;

    @Resource
    private MdmStructureLhRatioMapper mdmStructureLhRatioMapper;

    @Resource
    private MdmSkuScheduleCategoryMapper mdmSkuScheduleCategoryMapper;

    @Resource
    private CxEmbryoLhTimeMapper cxEmbryoLhTimeMapper;

    /**
     * 按最新成型余量重算结构可收尾时间，并回写 T_CX_EMBRYO_LH_TIME。
     *
     * <p>硫化余量默认按 {@link ScheduleAdjustHandler#calculatePlanSurplusQty} 逐月计划行现算
     * （与排程阶段同源）；调用方已持有更贴近现场的口径时，可经
     * {@link #recalc(LhScheduleContext, Date, Map)} 显式传入覆盖。
     *
     * @param context       硫化排程上下文（须已完成基础数据加载：月计划、胎胚库存、物料信息、工作日历、班次窗口、日硫化产能）
     * @param referenceTime 参考时刻（「前日期时间」）：余量时点与班次外推起点；为空取排程日 00:00
     * @return 重算摘要（各结构余量、组班产、还需班次、更新行数与跳过原因）
     */
    public StructureEndingRecalcSummary recalc(LhScheduleContext context, Date referenceTime) {
        return this.recalc(context, referenceTime, null);
    }

    /**
     * 按最新成型余量重算结构可收尾时间（支持外部传入硫化余量覆盖）。
     *
     * @param context               硫化排程上下文
     * @param referenceTime         参考时刻；为空取排程日 00:00
     * @param vulcanizingSurplusMap 硫化余量覆盖（胎胚编码 → 余量）；为空则内部按月计划现算
     * @return 重算摘要
     */
    public StructureEndingRecalcSummary recalc(LhScheduleContext context, Date referenceTime,
                                               Map<String, Integer> vulcanizingSurplusMap) {
        StructureEndingRecalcSummary summary = new StructureEndingRecalcSummary();
        if (Objects.isNull(context) || StringUtils.isEmpty(context.getFactoryCode())) {
            log.warn("结构可收尾时间重算跳过, 原因: 排程上下文或分厂编码为空");
            return summary;
        }
        String factoryCode = context.getFactoryCode();
        Date scheduleDate = LhScheduleTimeUtil.clearTime(context.getScheduleDate());
        Date effectiveReferenceTime = Objects.nonNull(referenceTime) ? referenceTime : scheduleDate;
        summary.setFactoryCode(factoryCode);
        summary.setScheduleDate(scheduleDate);
        summary.setReferenceTime(effectiveReferenceTime);

        // 1. 读取本批次结构切换记录（工厂 + 排程日），无记录直接返回
        List<CxEmbryoLhTime> rows = this.loadSwitchRows(factoryCode, scheduleDate);
        if (CollectionUtils.isEmpty(rows)) {
            log.info("结构可收尾时间重算跳过, 原因: 本批次无结构切换记录, factoryCode: {}, scheduleDate: {}",
                    factoryCode, LhScheduleTimeUtil.formatDate(scheduleDate));
            return summary;
        }

        // 2. 准备两条路径共用的基础数据
        //    2.1 班次日历（含参考日之后按日模式生成的班次，供锚点/find 首个有效班次与余量外推共用）
        List<RecalcShiftSlot> shiftSlots = this.buildShiftCalendar(context, effectiveReferenceTime);
        //    2.2 结构排产配置索引（同机台切换边、转产实例），供日历锚点判定
        AllocationIndex allocationIndex = this.buildAllocationIndex(context, effectiveReferenceTime);
        //    2.3 结构成型余量（胎胚余量 = 合并硫化余量 − 胎胚库存，再按结构上卷）
        Map<String, Integer> structureRemainderMap =
                this.calcStructureFormingRemainder(context, summary, vulcanizingSurplusMap);
        //    2.4 结构组班产（组内可用机台按（配比 × 日硫化量）折算小时产能 × 班次时长合计）
        Map<String, Integer> structureShiftCapacityMap =
                this.calcStructureShiftCapacity(context, effectiveReferenceTime);

        // 3. 逐结构推算收尾时刻并回写（同结构多行同值刷新）
        for (Map.Entry<String, List<CxEmbryoLhTime>> rowEntry : this.groupRowsByStructure(rows).entrySet()) {
            String structureName = rowEntry.getKey();
            List<CxEmbryoLhTime> structureRows = rowEntry.getValue();
            summary.setTotalStructureCount(summary.getTotalStructureCount() + 1);
            CxEmbryoLhTime firstRow = structureRows.get(0);
            int remainder = structureRemainderMap.getOrDefault(structureName, 0);
            int shiftCapacity = structureShiftCapacityMap.getOrDefault(structureName, 0);

            // 3.1 路径 1：日历锚点（降机台 / 结构交换 / 转产降机台）——开口时刻由配置切换日决定，不看余量
            CalendarAnchor calendarAnchor = this.resolveCalendarAnchor(
                    context, firstRow, shiftSlots, allocationIndex);
            if (Objects.nonNull(calendarAnchor)) {
                int updatedRows = this.writeBack(factoryCode, scheduleDate, structureName, structureRows,
                        calendarAnchor.getEndingTime(), calendarAnchor.getSwitchHours(),
                        remainder, shiftCapacity, effectiveReferenceTime,
                        this.buildCalendarRemark(calendarAnchor));
                summary.getRecalculatedDetails().add(new StructureEndingRecalcSummary.StructureRecalcDetail(
                        structureName, remainder, shiftCapacity, 0, calendarAnchor.getEndingTime(),
                        calendarAnchor.getReason()));
                summary.setCalendarAnchoredCount(summary.getCalendarAnchoredCount() + 1);
                summary.setUpdatedRowCount(summary.getUpdatedRowCount() + updatedRows);
                log.info("结构可收尾时间重算完成（日历锚点）, structureName: {}, 机台: {}, 原因: {}, 后结构: {}, "
                                + "锚点来源: {}, 收尾时间: {}, 最早可供硫化时间: {}, 成型余量: {}, 更新行数: {}",
                        structureName, firstRow.getCxMachineCode(), calendarAnchor.getReason(),
                        calendarAnchor.getNextStructureName(),
                        LhScheduleTimeUtil.formatDateTime(calendarAnchor.getAnchorSourceTime()),
                        LhScheduleTimeUtil.formatDateTime(calendarAnchor.getEndingTime()),
                        LhScheduleTimeUtil.formatDateTime(LhScheduleTimeUtil.addHours(
                                calendarAnchor.getEndingTime(), calendarAnchor.getSwitchHours())),
                        remainder, updatedRows);
                continue;
            }

            // 3.2 路径 2：余量推算（无日历锚点的记录）
            if (remainder <= 0) {
                summary.getSkippedRemainderZeroStructures().add(structureName);
                log.info("结构可收尾时间重算跳过, 原因: 无日历锚点且成型余量已耗尽, structureName: {}, 参考时刻: {}",
                        structureName, LhScheduleTimeUtil.formatDateTime(effectiveReferenceTime));
                continue;
            }
            if (shiftCapacity <= 0) {
                summary.getSkippedCapacityZeroStructures().add(structureName);
                log.warn("结构可收尾时间重算跳过, 原因: 结构组班产为0（无可用机台/无配比/无日硫化量）, "
                        + "structureName: {}, 成型余量: {}", structureName, remainder);
                continue;
            }
            int extraShifts = resolveExtraShifts(remainder, shiftCapacity);
            Date endingTime = this.extrapolateEndingTime(shiftSlots, effectiveReferenceTime, extraShifts);
            if (Objects.isNull(endingTime)) {
                summary.getSkippedOverLimitStructures().add(structureName);
                log.warn("结构可收尾时间重算跳过, 原因: 还需{}班超过预测窗口{}班, structureName: {}, 成型余量: {}, 组班产: {}",
                        extraShifts, ScheduleConstants.MAX_PREDICT_SHIFTS, structureName, remainder, shiftCapacity);
                continue;
            }
            int updatedRows = this.writeBack(factoryCode, scheduleDate, structureName, structureRows,
                    endingTime, null, remainder, shiftCapacity, effectiveReferenceTime,
                    String.format("%s：成型余量%d ÷ 组班产%d = 还需%d班",
                            REMARK_PREFIX_SURPLUS, remainder, shiftCapacity, extraShifts));
            summary.getRecalculatedDetails().add(new StructureEndingRecalcSummary.StructureRecalcDetail(
                    structureName, remainder, shiftCapacity, extraShifts, endingTime, "余量推算"));
            summary.setUpdatedRowCount(summary.getUpdatedRowCount() + updatedRows);
            log.info("结构可收尾时间重算完成（余量推算）, structureName: {}, 成型余量: {}, 组班产: {}, 还需班次: {}, "
                            + "收尾时间: {}, 更新行数: {}, 参考时刻: {}",
                    structureName, remainder, shiftCapacity, extraShifts,
                    LhScheduleTimeUtil.formatDateTime(endingTime), updatedRows,
                    LhScheduleTimeUtil.formatDateTime(effectiveReferenceTime));
        }
        log.info("结构可收尾时间重算结束, factoryCode: {}, scheduleDate: {}, 参考时刻: {}, 结构数: {}, 日历锚点: {}, "
                        + "余量推算: {}, 更新行数: {}, 余量为0跳过: {}, 班产为0跳过: {}, 超预测窗口跳过: {}",
                factoryCode, LhScheduleTimeUtil.formatDate(scheduleDate),
                LhScheduleTimeUtil.formatDateTime(effectiveReferenceTime),
                summary.getTotalStructureCount(), summary.getCalendarAnchoredCount(),
                summary.getRecalculatedDetails().size() - summary.getCalendarAnchoredCount(),
                summary.getUpdatedRowCount(), summary.getSkippedRemainderZeroStructures().size(),
                summary.getSkippedCapacityZeroStructures().size(), summary.getSkippedOverLimitStructures().size());
        return summary;
    }

    // ==================== 日历锚点（降机台 / 结构交换 / 转产降机台） ====================

    /**
     * 解析记录的日历锚点：该行的结构切换是否为「按配置切换日开口」的三类场景之一。
     *
     * <p>判定顺序（与成型侧 {@code StructureChangeClassifier} 同源）：
     * <ol>
     *   <li><b>转产降机台</b>：该机台是该结构的转出机台（{@code Transfer.fromMachine}）→ 切换日取转入机台的
     *       {@code toBeginComposite}（等转入机台空，非原地日历日），后结构取 {@code fromNextStructure}；</li>
     *   <li><b>纯降机台 / 结构交换</b>：该机台该结构存在 {@code DOWNGRADE} 切换边 → 切换日取
     *       {@code nextBeginComposite}，后结构取 {@code nextStructure}。</li>
     * </ol>
     *
     * <p>换英寸档（4h/8h）夜班不换模，锚点顺延到切换日的早班（与成型侧 {@code applyCalendarSwitchDeduct}
     * 跳过夜班一致）；同规格档（2h）无班次门禁，锚点 = 切换日第一个非停产班次。
     *
     * @param context         排程上下文
     * @param row             该结构首行记录（取机台、前结构、后结构编码）
     * @param shiftSlots      班次日历
     * @param allocationIndex 结构排产配置索引
     * @return 日历锚点；该行不属于三类场景或无法解析班次时返回 null（回退余量推算）
     */
    private CalendarAnchor resolveCalendarAnchor(LhScheduleContext context, CxEmbryoLhTime row,
                                                 List<RecalcShiftSlot> shiftSlots,
                                                 AllocationIndex allocationIndex) {
        String machineCode = row.getCxMachineCode();
        String structureName = row.getStructureName();
        if (StringUtils.isEmpty(machineCode) || StringUtils.isEmpty(structureName)) {
            return null;
        }
        String anchorKey = machineCode + "|" + structureName;
        String nextStructureName;
        LocalDate anchorDate;
        String reason;
        TransferInfo transfer = allocationIndex.getTransferMap().get(anchorKey);
        if (Objects.nonNull(transfer)) {
            nextStructureName = transfer.getFromNextStructure();
            anchorDate = transfer.getToBeginDate();
            reason = REASON_TRANSFER_OUT;
        } else {
            EdgeInfo edge = allocationIndex.getDowngradeEdgeMap().get(anchorKey);
            if (Objects.isNull(edge)) {
                return null;
            }
            nextStructureName = edge.getNextStructure();
            anchorDate = edge.getNextBeginDate();
            reason = REASON_DOWNGRADE;
        }
        if (Objects.isNull(anchorDate)) {
            return null;
        }
        // 换模档位：同规格（2h）可夜班换模；换英寸（4h/8h）夜班不换模，锚点顺延到切换日早班
        int switchHours = this.resolveSwitchHours(context, structureName, nextStructureName,
                row.getSwitchHours());
        boolean sameSpecSwitch = this.isSameSpecSwitch(context, structureName, nextStructureName);
        Date anchorTime = this.resolveFirstValidShiftStart(shiftSlots, anchorDate, !sameSpecSwitch);
        if (Objects.isNull(anchorTime)) {
            log.warn("结构可收尾时间重算: 日历锚点无法解析切换日班次, structureName: {}, machineCode: {}, 切换日: {}, "
                            + "换英寸: {}，回退余量推算",
                    structureName, machineCode, anchorDate, !sameSpecSwitch);
            return null;
        }
        return new CalendarAnchor(nextStructureName, anchorTime, switchHours, reason);
    }

    /**
     * 解析切换耗时（小时）：优先沿用成型侧写入的 {@code SWITCH_HOURS}，缺失时按规格比较给档位值。
     *
     * @param context           排程上下文
     * @param prevStructure     前结构
     * @param nextStructure     后结构
     * @param originSwitchHours 原记录切换耗时（可空）
     * @return 切换耗时（小时）
     */
    private int resolveSwitchHours(LhScheduleContext context, String prevStructure, String nextStructure,
                                   Integer originSwitchHours) {
        if (Objects.nonNull(originSwitchHours)) {
            return originSwitchHours;
        }
        // 记录未带切换耗时时按同规格默认档兜底，并告警提示现场补数
        log.warn("结构可收尾时间重算: 记录未带切换耗时, prevStructure: {}, nextStructure: {}, 按同规格默认档{}h处理",
                prevStructure, nextStructure, ScheduleConstants.DEFAULT_SAME_INCH_SWITCH_HOURS);
        return ScheduleConstants.DEFAULT_SAME_INCH_SWITCH_HOURS;
    }

    /**
     * 判定前后结构是否同规格（成型侧 {@code ProductionCalculator.isSameSpecSwitch} 同口径）。
     *
     * @param context       排程上下文
     * @param prevStructure 前结构
     * @param nextStructure 后结构
     * @return true=同规格（2h 档，可夜班换模）；规格缺失按不同规格（保守，锚点落到早班）
     */
    private boolean isSameSpecSwitch(LhScheduleContext context, String prevStructure, String nextStructure) {
        String prevSpec = this.findStructureSpecification(context, prevStructure);
        String nextSpec = this.findStructureSpecification(context, nextStructure);
        return StringUtils.isNotEmpty(prevSpec) && StringUtils.equals(prevSpec, nextSpec);
    }

    /**
     * 取结构的规格标识：优先物料主数据的规格字段，回退按结构名解析英寸。
     *
     * @param context       排程上下文
     * @param structureName 结构名
     * @return 规格串；无法解析返回 null
     */
    private String findStructureSpecification(LhScheduleContext context, String structureName) {
        if (StringUtils.isEmpty(structureName) || CollectionUtils.isEmpty(context.getMaterialInfoMap())) {
            return null;
        }
        for (MdmMaterialInfo materialInfo : context.getMaterialInfoMap().values()) {
            if (Objects.nonNull(materialInfo) && structureName.equals(materialInfo.getStructureName())) {
                String proSize = materialInfo.getProSize();
                if (StringUtils.isNotEmpty(proSize)) {
                    return proSize;
                }
            }
        }
        return null;
    }

    /**
     * 解析指定切换日的锚点时刻：该日第一个非停产班次的开始时刻，可选跳过夜班（换英寸档夜班不换模，
     * 与成型侧 {@code applyCalendarSwitchDeduct} 跳过夜班一致）。
     *
     * <p>业务日口径：夜班归属次日（如「D 日夜班」= D-1 日 22:00 开始），与本方法按
     * {@link RecalcShiftSlot#getWorkDate()} 匹配切换日一致。
     *
     * @param shiftSlots   班次日历
     * @param targetDate   切换日（结构配置的 BEGIN_DAY）
     * @param skipNight    是否跳过夜班（换英寸档为 true）
     * @return 锚点时刻；该日无有效班次时返回 null
     */
    static Date resolveFirstValidShiftStart(List<RecalcShiftSlot> shiftSlots, LocalDate targetDate,
                                            boolean skipNight) {
        if (CollectionUtils.isEmpty(shiftSlots) || Objects.isNull(targetDate)) {
            return null;
        }
        for (RecalcShiftSlot slot : shiftSlots) {
            if (Objects.isNull(slot) || Objects.isNull(slot.getShiftStart()) || slot.isStopped()) {
                continue;
            }
            LocalDate workDate = slot.getWorkDate();
            if (Objects.isNull(workDate) || !workDate.isEqual(targetDate)) {
                continue;
            }
            // 换英寸档：夜班不换模，逐个向后找该日早班
            if (skipNight && slot.isNightShift()) {
                continue;
            }
            return slot.getShiftStart();
        }
        return null;
    }

    /**
     * 构建结构排产配置索引：机台|前结构 → 降机台切换边、机台|结构 → 转产转出实例。
     *
     * <p>口径与成型侧 {@code StructureChangeClassifier.classify / classifyTransfers} 一致：
     * 边按覆盖区间推相邻段（后段 begin ≥ 前段 end），接力机台非空为降机台（有接力）；
     * 转产为「结构从机台 A 迁到机台 B，且 A 的下段 X ≠ B 的前段 C」。
     *
     * @param context       排程上下文
     * @param referenceTime 参考时刻（决定取哪个月的排产配置）
     * @return 结构排产配置索引
     */
    private AllocationIndex buildAllocationIndex(LhScheduleContext context, Date referenceTime) {
        Date referenceDay = LhScheduleTimeUtil.clearTime(referenceTime);
        int referenceYear = this.resolveYear(referenceDay);
        int referenceMonth = this.resolveMonth(referenceDay);
        LambdaQueryWrapper<MpStructureAllocation> wrapper = new LambdaQueryWrapper<MpStructureAllocation>()
                .eq(MpStructureAllocation::getFactoryCode, context.getFactoryCode())
                .eq(MpStructureAllocation::getYear, referenceYear)
                .eq(MpStructureAllocation::getMonth, referenceMonth)
                .eq(MpStructureAllocation::getPlanType, STRUCTURE_ALLOCATION_NORMAL_PLAN_TYPE);
        if (StringUtils.isNotEmpty(context.getProductionVersion())) {
            wrapper.eq(MpStructureAllocation::getProductionVersion, context.getProductionVersion());
        }
        List<MpStructureAllocation> allocations = lhMpStructureAllocationMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(allocations)) {
            log.info("结构可收尾时间重算: 结构排产配置为空, factoryCode: {}, 年月: {}-{}",
                    context.getFactoryCode(), referenceYear, referenceMonth);
            return new AllocationIndex();
        }
        AllocationIndex index = buildAllocationIndex(allocations, referenceDay);
        log.info("结构可收尾时间重算: 结构排产配置索引构建完成, 降机台边: {}, 转产实例: {}, 年月: {}-{}",
                index.getDowngradeEdgeMap().size(), index.getTransferMap().size(),
                referenceYear, referenceMonth);
        return index;
    }

    /**
     * 由结构排产配置构建索引（纯计算，无数据库依赖，供单测直接喂真实配置数据验证四类场景）。
     *
     * <p>口径与成型侧 {@code StructureChangeClassifier.classify / classifyTransfers} 一致：
     * 边按覆盖区间推相邻段（后段 begin ≥ 前段 end），接力机台非空为降机台（有接力）；
     * 转产为「结构从机台 A 迁到机台 B，且 A 的下段 X ≠ B 的前段 C」（X == C 属于结构对调 SWAP，不算转产）。
     *
     * @param allocations  结构排产配置列表（同工厂、同年月、同排产版本、正常计划类型）
     * @param referenceDay 参考日 00:00（年报错时仅用于日志）
     * @return 结构排产配置索引
     */
    static AllocationIndex buildAllocationIndex(List<MpStructureAllocation> allocations, Date referenceDay) {
        AllocationIndex index = new AllocationIndex();
        if (CollectionUtils.isEmpty(allocations)) {
            return index;
        }
        // 按机台归集配置段（机台 → 段列表，按开始日升序）
        Map<String, List<AllocationSegment>> segmentMapByMachine = new LinkedHashMap<>();
        // 按结构归集配置段（结构 → 段列表，按开始日升序），用于转产识别
        Map<String, List<AllocationSegment>> segmentMapByStructure = new LinkedHashMap<>();
        // 结构覆盖终点（结构 → 各段），用于接力机台判定
        Map<String, List<AllocationSegment>> structEnds = new LinkedHashMap<>();
        for (MpStructureAllocation allocation : allocations) {
            AllocationSegment segment = AllocationSegment.of(allocation);
            if (Objects.isNull(segment)) {
                continue;
            }
            segmentMapByMachine.computeIfAbsent(segment.getMachineCode(), key -> new ArrayList<>()).add(segment);
            segmentMapByStructure.computeIfAbsent(segment.getStructureName(), key -> new ArrayList<>()).add(segment);
            structEnds.computeIfAbsent(segment.getStructureName(), key -> new ArrayList<>()).add(segment);
        }
        Comparator<AllocationSegment> byBegin = Comparator.comparingInt(AllocationSegment::getBeginDateValue)
                .thenComparingInt(AllocationSegment::getEndDateValue);
        segmentMapByMachine.values().forEach(segments -> segments.sort(byBegin));
        segmentMapByStructure.values().forEach(segments -> segments.sort(byBegin));

        // 1. 机台相邻切换边：接力机台非空 = 降机台（有接力）
        for (Map.Entry<String, List<AllocationSegment>> machineEntry : segmentMapByMachine.entrySet()) {
            String machineCode = machineEntry.getKey();
            List<AllocationSegment> segments = machineEntry.getValue();
            for (AllocationSegment prev : segments) {
                AllocationSegment next = findNextSegment(prev, segments);
                if (Objects.isNull(next)) {
                    continue;
                }
                List<String> relayMachines = resolveRelayMachines(prev, machineCode, structEnds);
                if (relayMachines.isEmpty()) {
                    // 无接力（升机台）：必须余量收完才切，无日历锚点
                    continue;
                }
                index.getDowngradeEdgeMap().put(machineCode + "|" + prev.getStructureName(),
                        new EdgeInfo(prev.getStructureName(), next.getStructureName(), next.getBeginDate()));
            }
        }
        // 2. 转产识别：结构 H 从 A 迁到 B（B 的 H 段开始晚于 A 的 H 段结束），且 A 的下段 X ≠ B 的前段 C
        for (Map.Entry<String, List<AllocationSegment>> structureEntry : segmentMapByStructure.entrySet()) {
            List<AllocationSegment> segments = structureEntry.getValue();
            for (int i = 0; i + 1 < segments.size(); i++) {
                AllocationSegment from = segments.get(i);
                AllocationSegment to = segments.get(i + 1);
                if (from.getMachineCode().equals(to.getMachineCode())) {
                    continue;
                }
                if (to.getBeginDateValue() <= from.getEndDateValue()) {
                    continue;
                }
                AllocationSegment fromNext = findNextSegment(from, segmentMapByMachine.get(from.getMachineCode()));
                AllocationSegment toPrev = findPrevSegment(to, segmentMapByMachine.get(to.getMachineCode()));
                if (Objects.isNull(fromNext) || Objects.isNull(toPrev)
                        || StringUtils.equals(fromNext.getStructureName(), toPrev.getStructureName())) {
                    // X == C 属于结构对调（SWAP），不是转产
                    continue;
                }
                index.getTransferMap().put(from.getMachineCode() + "|" + from.getStructureName(),
                        new TransferInfo(from.getStructureName(),
                                fromNext.getStructureName(), to.getMachineCode(), to.getBeginDate()));
            }
        }
        return index;
    }

    /**
     * 机台上某段之后的下一个不同结构段（按覆盖区间，begin ≥ 前段 end，取 begin 最小）。
     *
     * @param prev     前段
     * @param segments 该机台全部段
     * @return 后段；不存在返回 null
     */
    private static AllocationSegment findNextSegment(AllocationSegment prev, List<AllocationSegment> segments) {
        if (CollectionUtils.isEmpty(segments)) {
            return null;
        }
        AllocationSegment next = null;
        for (AllocationSegment candidate : segments) {
            if (candidate == prev || StringUtils.equals(candidate.getStructureName(), prev.getStructureName())) {
                continue;
            }
            if (candidate.getBeginDateValue() < prev.getEndDateValue()) {
                continue;
            }
            if (Objects.isNull(next) || candidate.getBeginDateValue() < next.getBeginDateValue()) {
                next = candidate;
            }
        }
        return next;
    }

    /**
     * 机台上某段之前的前一个不同结构段（按覆盖区间，end ≤ 当前段 begin，取 end 最大）。
     *
     * @param current  当前段
     * @param segments 该机台全部段
     * @return 前段；不存在返回 null
     */
    private static AllocationSegment findPrevSegment(AllocationSegment current, List<AllocationSegment> segments) {
        if (CollectionUtils.isEmpty(segments)) {
            return null;
        }
        AllocationSegment prev = null;
        for (AllocationSegment candidate : segments) {
            if (candidate == current || StringUtils.equals(candidate.getStructureName(), current.getStructureName())) {
                continue;
            }
            if (candidate.getEndDateValue() > current.getBeginDateValue()) {
                continue;
            }
            if (Objects.isNull(prev) || candidate.getEndDateValue() > prev.getEndDateValue()) {
                prev = candidate;
            }
        }
        return prev;
    }

    /**
     * 接力机台：前结构在其他机台上覆盖区间结束于前段 END 之后（即前结构在别处还有更晚覆盖）。
     *
     * @param prev        前段
     * @param machineCode 当前机台（排除自身）
     * @param structEnds  结构 → 全部段
     * @return 接力机台编码列表
     */
    private static List<String> resolveRelayMachines(AllocationSegment prev, String machineCode,
                                                     Map<String, List<AllocationSegment>> structEnds) {
        List<String> relayMachines = new ArrayList<>();
        List<AllocationSegment> ends = structEnds.get(prev.getStructureName());
        if (CollectionUtils.isEmpty(ends)) {
            return relayMachines;
        }
        for (AllocationSegment segment : ends) {
            if (segment.getMachineCode().equals(machineCode)) {
                continue;
            }
            if (segment.getEndDateValue() > prev.getEndDateValue()
                    && !relayMachines.contains(segment.getMachineCode())) {
                relayMachines.add(segment.getMachineCode());
            }
        }
        return relayMachines;
    }

    // ==================== 余量推算（无日历锚点的记录） ====================

    /**
     * 读取本批次（工厂 + 排程日）全部结构切换记录。
     *
     * @param factoryCode  分厂编码
     * @param scheduleDate 排程日 00:00
     * @return 切换记录列表（按结构名、主键排序；逻辑删除由框架过滤）
     */
    private List<CxEmbryoLhTime> loadSwitchRows(String factoryCode, Date scheduleDate) {
        return cxEmbryoLhTimeMapper.selectList(new LambdaQueryWrapper<CxEmbryoLhTime>()
                .eq(CxEmbryoLhTime::getFactoryCode, factoryCode)
                .eq(CxEmbryoLhTime::getScheduleDate, scheduleDate)
                .orderByAsc(CxEmbryoLhTime::getStructureName)
                .orderByAsc(CxEmbryoLhTime::getId));
    }

    /**
     * 把切换记录按前结构分组：同结构多行必须同批整体刷新（硫化读侧取「同结构最早值」）。
     *
     * @param rows 切换记录列表
     * @return 前结构名 → 该结构全部记录
     */
    private Map<String, List<CxEmbryoLhTime>> groupRowsByStructure(List<CxEmbryoLhTime> rows) {
        Map<String, List<CxEmbryoLhTime>> grouped = new LinkedHashMap<>();
        for (CxEmbryoLhTime row : rows) {
            if (Objects.isNull(row) || StringUtils.isEmpty(row.getStructureName())) {
                continue;
            }
            grouped.computeIfAbsent(row.getStructureName(), key -> new ArrayList<>()).add(row);
        }
        return grouped;
    }

    /**
     * 计算结构成型余量（口径与 {@code LhBaseDataServiceImpl#loadEmbryoEndingFlagMap} 一致）。
     *
     * <p>逐月计划行复用 {@link ScheduleAdjustHandler#calculatePlanSurplusQty} 得到硫化余量
     * （调用方传入 {@code vulcanizingSurplusMap} 时以传入值为准），按胎胚合并后减胎胚实时库存；
     * 再按物料归属结构上卷，非主销且余量 ≤ 收尾舍弃阈值按 0 计。
     *
     * @param context               排程上下文
     * @param summary               重算摘要（记录非主销舍弃胎胚数）
     * @param vulcanizingSurplusMap 硫化余量覆盖（胎胚编码 → 余量）；为空则内部现算
     * @return 结构名 → 成型余量（仅含 &gt; 0 的结构）
     */
    private Map<String, Integer> calcStructureFormingRemainder(LhScheduleContext context,
                                                               StructureEndingRecalcSummary summary,
                                                               Map<String, Integer> vulcanizingSurplusMap) {
        if (CollectionUtils.isEmpty(context.getMonthPlanList())) {
            return new LinkedHashMap<>();
        }
        // 主销产品集合：与胎胚收尾标识同源口径（SCHEDULE_TYPE='01'）
        Set<String> mainProductCodeSet = new HashSet<>();
        List<MdmSkuScheduleCategory> mainCategoryList = mdmSkuScheduleCategoryMapper.selectList(
                new LambdaQueryWrapper<MdmSkuScheduleCategory>()
                        .eq(MdmSkuScheduleCategory::getScheduleType, MAIN_PRODUCT_SCHEDULE_TYPE));
        for (MdmSkuScheduleCategory category : mainCategoryList) {
            if (Objects.nonNull(category) && StringUtils.isNotEmpty(category.getMaterialCode())) {
                mainProductCodeSet.add(category.getMaterialCode());
            }
        }
        Map<String, Integer> embryoSurplusMap = new LinkedHashMap<>();
        Map<String, Boolean> embryoHasMainMap = new LinkedHashMap<>();
        for (FactoryMonthPlanProductionFinalResult plan : context.getMonthPlanList()) {
            if (Objects.isNull(plan) || StringUtils.isEmpty(plan.getEmbryoCode())) {
                continue;
            }
            int vulcanizingSurplus = this.resolveVulcanizingSurplus(context, plan, vulcanizingSurplusMap);
            embryoSurplusMap.merge(plan.getEmbryoCode(), vulcanizingSurplus, Integer::sum);
            if (mainProductCodeSet.contains(plan.getMaterialCode())) {
                embryoHasMainMap.put(plan.getEmbryoCode(), Boolean.TRUE);
            } else {
                embryoHasMainMap.putIfAbsent(plan.getEmbryoCode(), Boolean.FALSE);
            }
        }
        Map<String, Integer> embryoStockMap = context.getEmbryoRealtimeStockMap();
        int discardThreshold = context.getParamIntValue(PARAM_ENDING_DISCARD_THRESHOLD,
                DEFAULT_ENDING_DISCARD_THRESHOLD);
        Map<String, Integer> structureRemainderMap = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> embryoEntry : embryoSurplusMap.entrySet()) {
            String embryoCode = embryoEntry.getKey();
            int embryoStock = Objects.nonNull(embryoStockMap) ? embryoStockMap.getOrDefault(embryoCode, 0) : 0;
            int embryoRemainder = embryoEntry.getValue() - embryoStock;
            if (embryoRemainder <= 0) {
                continue;
            }
            String structureName = this.resolveEmbryoStructure(context, embryoCode);
            if (StringUtils.isEmpty(structureName)) {
                continue;
            }
            boolean hasMain = embryoHasMainMap.getOrDefault(embryoCode, Boolean.FALSE);
            // 非主销且余量 ≤ 收尾舍弃阈值：业务上不排，与成型侧收尾舍弃口径一致按 0 计
            if (!hasMain && embryoRemainder <= discardThreshold) {
                summary.setDiscardedNonMainCount(summary.getDiscardedNonMainCount() + 1);
                continue;
            }
            structureRemainderMap.merge(structureName, embryoRemainder, Integer::sum);
        }
        structureRemainderMap.entrySet().removeIf(entry -> entry.getValue() <= 0);
        return structureRemainderMap;
    }

    /**
     * 解析单条月计划行的硫化余量：调用方传入覆盖时以传入值为准，否则复用排程阶段口径现算。
     *
     * @param context               排程上下文
     * @param plan                  月生产计划记录
     * @param vulcanizingSurplusMap 硫化余量覆盖（胎胚编码 → 余量），可为空
     * @return 硫化余量
     */
    private int resolveVulcanizingSurplus(LhScheduleContext context,
                                          FactoryMonthPlanProductionFinalResult plan,
                                          Map<String, Integer> vulcanizingSurplusMap) {
        if (!CollectionUtils.isEmpty(vulcanizingSurplusMap)) {
            Integer overrideSurplus = vulcanizingSurplusMap.get(plan.getEmbryoCode());
            if (Objects.nonNull(overrideSurplus)) {
                return overrideSurplus;
            }
        }
        return scheduleAdjustHandler.calculatePlanSurplusQty(context, plan);
    }

    /**
     * 解析胎胚归属结构：按胎胚编码在物料主数据中匹配首个非空结构。
     *
     * @param context    排程上下文
     * @param embryoCode 胎胚编码
     * @return 结构名；无法归属返回 null
     */
    private String resolveEmbryoStructure(LhScheduleContext context, String embryoCode) {
        MdmMaterialInfo materialInfo = this.findEmbryoMaterialInfo(context, embryoCode);
        return Objects.nonNull(materialInfo) ? materialInfo.getStructureName() : null;
    }

    /**
     * 取胎胚代表物料（同胎胚多产品状态时取首个命中记录，日硫化量口径一致）。
     *
     * @param context    排程上下文
     * @param embryoCode 胎胚编码
     * @return 物料主数据；未命中返回 null
     */
    private MdmMaterialInfo findEmbryoMaterialInfo(LhScheduleContext context, String embryoCode) {
        if (StringUtils.isEmpty(embryoCode) || CollectionUtils.isEmpty(context.getMaterialInfoMap())) {
            return null;
        }
        for (MdmMaterialInfo materialInfo : context.getMaterialInfoMap().values()) {
            if (Objects.nonNull(materialInfo) && embryoCode.equals(materialInfo.getEmbryoCode())) {
                return materialInfo;
            }
        }
        return null;
    }

    /**
     * 计算结构组班产：结构全部胎胚的（配比 × 日硫化量）折算小时产能 × 班次时长 × 可用机台数。
     *
     * <p>配比按结构取「结构代表配比」= 该结构在各机型 {@code LH_MACHINE_MAX_QTY} 的平均值（至少 1），
     * 与成型侧 {@code ProductionCalculator#getStructureRepresentativeRatio} 同口径；
     * 可用机台只统计参考日仍占机的配置段（按覆盖区间判定）。
     *
     * @param context       排程上下文
     * @param referenceTime 参考时刻（决定取哪个月的排产配置）
     * @return 结构名 → 组班产（条/班，仅含 &gt; 0 的结构）
     */
    private Map<String, Integer> calcStructureShiftCapacity(LhScheduleContext context, Date referenceTime) {
        Map<String, Integer> capacityMap = new LinkedHashMap<>();
        Map<String, List<String>> structureEmbryoMap = this.groupEmbryoByStructure(context);
        if (structureEmbryoMap.isEmpty()) {
            return capacityMap;
        }
        Date referenceDay = LhScheduleTimeUtil.clearTime(referenceTime);
        int referenceYear = this.resolveYear(referenceDay);
        int referenceMonth = this.resolveMonth(referenceDay);
        LambdaQueryWrapper<MpStructureAllocation> allocationWrapper =
                new LambdaQueryWrapper<MpStructureAllocation>()
                        .eq(MpStructureAllocation::getFactoryCode, context.getFactoryCode())
                        .eq(MpStructureAllocation::getYear, referenceYear)
                        .eq(MpStructureAllocation::getMonth, referenceMonth)
                        .eq(MpStructureAllocation::getPlanType, STRUCTURE_ALLOCATION_NORMAL_PLAN_TYPE);
        if (StringUtils.isNotEmpty(context.getProductionVersion())) {
            allocationWrapper.eq(MpStructureAllocation::getProductionVersion, context.getProductionVersion());
        }
        List<MpStructureAllocation> allocations = lhMpStructureAllocationMapper.selectList(allocationWrapper);
        Map<String, Integer> structureRatioMap = this.loadStructureRatioMap(context.getFactoryCode());
        int shiftDurationHours = this.resolveShiftDurationHours(context);
        for (Map.Entry<String, List<String>> structureEntry : structureEmbryoMap.entrySet()) {
            String structureName = structureEntry.getKey();
            // 参考日仍占机的配置段才算可用机台（按覆盖区间判定，不取提前生产机台）
            long availableMachineCount = allocations.stream()
                    .filter(allocation -> structureName.equals(allocation.getStructureName()))
                    .filter(allocation -> this.isAllocationActiveOnDay(allocation, referenceDay))
                    .map(MpStructureAllocation::getCxMachineCode)
                    .filter(StringUtils::isNotEmpty)
                    .distinct()
                    .count();
            if (availableMachineCount <= 0) {
                continue;
            }
            int ratio = structureRatioMap.getOrDefault(structureName, 1);
            int embryoCapacitySum = 0;
            for (String embryoCode : structureEntry.getValue()) {
                MdmMaterialInfo materialInfo = this.findEmbryoMaterialInfo(context, embryoCode);
                int hourlyCapacity = this.calculateHourlyCapacity(context,
                        Objects.nonNull(materialInfo) ? materialInfo.getMaterialCode() : null, ratio);
                embryoCapacitySum += hourlyCapacity * shiftDurationHours;
            }
            int groupShiftCapacity = (int) availableMachineCount * embryoCapacitySum;
            if (groupShiftCapacity > 0) {
                capacityMap.put(structureName, groupShiftCapacity);
            }
        }
        return capacityMap;
    }

    /**
     * 按胎胚归集结构：结构名 → 该结构下的胎胚编码列表（去重）。
     *
     * @param context 排程上下文
     * @return 结构名 → 胎胚编码列表
     */
    private Map<String, List<String>> groupEmbryoByStructure(LhScheduleContext context) {
        Map<String, List<String>> structureEmbryoMap = new LinkedHashMap<>();
        if (CollectionUtils.isEmpty(context.getMonthPlanList())) {
            return structureEmbryoMap;
        }
        for (FactoryMonthPlanProductionFinalResult plan : context.getMonthPlanList()) {
            if (Objects.isNull(plan) || StringUtils.isEmpty(plan.getEmbryoCode())) {
                continue;
            }
            String structureName = this.resolveEmbryoStructure(context, plan.getEmbryoCode());
            if (StringUtils.isEmpty(structureName)) {
                continue;
            }
            List<String> embryoCodeList = structureEmbryoMap.computeIfAbsent(structureName, key -> new ArrayList<>());
            if (!embryoCodeList.contains(plan.getEmbryoCode())) {
                embryoCodeList.add(plan.getEmbryoCode());
            }
        }
        return structureEmbryoMap;
    }

    /**
     * 加载结构硫化配比：结构名 → 结构代表配比（各机型 LH_MACHINE_MAX_QTY 平均值，至少 1）。
     *
     * @param factoryCode 分厂编码
     * @return 结构名 → 配比
     */
    private Map<String, Integer> loadStructureRatioMap(String factoryCode) {
        // 逻辑删除由框架统一过滤，不手写 IS_DELETE 条件
        List<MdmStructureLhRatio> ratioList = mdmStructureLhRatioMapper.selectList(
                new LambdaQueryWrapper<MdmStructureLhRatio>()
                        .eq(MdmStructureLhRatio::getFactoryCode, factoryCode));
        Map<String, List<Integer>> structureRatioListMap = new LinkedHashMap<>();
        for (MdmStructureLhRatio ratio : ratioList) {
            if (Objects.isNull(ratio) || StringUtils.isEmpty(ratio.getStructureName())
                    || Objects.isNull(ratio.getLhMachineMaxQty()) || ratio.getLhMachineMaxQty() <= 0) {
                continue;
            }
            structureRatioListMap.computeIfAbsent(ratio.getStructureName(), key -> new ArrayList<>())
                    .add(ratio.getLhMachineMaxQty());
        }
        Map<String, Integer> structureRatioMap = new LinkedHashMap<>();
        for (Map.Entry<String, List<Integer>> entry : structureRatioListMap.entrySet()) {
            int sum = entry.getValue().stream().mapToInt(Integer::intValue).sum();
            int average = BigDecimal.valueOf(sum)
                    .divide(BigDecimal.valueOf(entry.getValue().size()), 0, RoundingMode.HALF_UP)
                    .intValue();
            structureRatioMap.put(entry.getKey(), Math.max(1, average));
        }
        return structureRatioMap;
    }

    /**
     * 单台成型机小时产能（与成型侧 {@code ProductionCalculator#calculateHourlyCapacity} 同公式）。
     *
     * <p>小时产能 = 3600 ÷ 单胎耗时，单胎耗时 = 86400 ÷ (配比 × 日硫化量)。
     *
     * @param context      排程上下文
     * @param materialCode 物料编码
     * @param ratio        结构配比
     * @return 小时产能（条/小时）；无有效日硫化量时返回 0（不参与班产合计）
     */
    private int calculateHourlyCapacity(LhScheduleContext context, String materialCode, int ratio) {
        int dailyLhCapacity = this.resolveDailyLhCapacity(context, materialCode);
        if (dailyLhCapacity <= 0 || ratio <= 0) {
            return 0;
        }
        BigDecimal timePerTire = BigDecimal.valueOf(ScheduleConstants.SECONDS_PER_DAY)
                .divide(BigDecimal.valueOf(ratio).multiply(BigDecimal.valueOf(dailyLhCapacity)),
                        2, RoundingMode.HALF_UP);
        if (timePerTire.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return BigDecimal.valueOf(ScheduleConstants.SECONDS_PER_HOUR)
                .divide(timePerTire, 0, RoundingMode.FLOOR)
                .intValue();
    }

    /**
     * 解析物料双模日硫化量（取数优先级与成型侧日硫化量口径一致：APS → 标准 → MES）。
     *
     * @param context      排程上下文
     * @param materialCode 物料编码
     * @return 日硫化量；无有效值返回 0
     */
    private int resolveDailyLhCapacity(LhScheduleContext context, String materialCode) {
        if (StringUtils.isEmpty(materialCode) || CollectionUtils.isEmpty(context.getSkuLhCapacityMap())) {
            return 0;
        }
        MdmSkuLhCapacity capacity = context.getSkuLhCapacityMap().get(materialCode);
        if (Objects.isNull(capacity)) {
            return 0;
        }
        if (Objects.nonNull(capacity.getApsCapacity()) && capacity.getApsCapacity() > 0) {
            return capacity.getApsCapacity();
        }
        if (Objects.nonNull(capacity.getStandardCapacity()) && capacity.getStandardCapacity() > 0) {
            return capacity.getStandardCapacity();
        }
        if (Objects.nonNull(capacity.getMesCapacity()) && capacity.getMesCapacity() > 0) {
            return capacity.getMesCapacity();
        }
        return 0;
    }

    /**
     * 解析班次时长（小时）：优先排程配置，回退窗口首个班次配置，最后兜底 8 小时。
     *
     * @param context 排程上下文
     * @return 班次时长（小时，至少 1）
     */
    private int resolveShiftDurationHours(LhScheduleContext context) {
        if (Objects.nonNull(context.getScheduleConfig())
                && context.getScheduleConfig().getShiftDurationHours() > 0) {
            return context.getScheduleConfig().getShiftDurationHours();
        }
        if (!CollectionUtils.isEmpty(context.getScheduleWindowShifts())) {
            Integer shiftDuration = context.getScheduleWindowShifts().get(0).getShiftDuration();
            if (Objects.nonNull(shiftDuration) && shiftDuration > 0) {
                return shiftDuration;
            }
        }
        return DEFAULT_SHIFT_DURATION_HOURS;
    }

    /**
     * 构建班次日历：窗口班次 + 从窗口时序终点按日模式向外生成的班次。
     *
     * <p>窗口班次直接取 {@code scheduleWindowShifts}（含绝对起止时刻）；窗口之外按
     * 「早 → 中 → 夜」日模式逐日生成（夜班归属次日，与前一日 22:00 跨零点结束一致），
     * 与成型侧 {@code extrapolateShiftEndAfterWindow} 的日模式推导思路一致。
     * 日历锚点用到的切换日通常在窗口之外较远处，故生成范围按「参考时刻之后的最大扫描天数」铺满。
     *
     * @param context       排程上下文
     * @param referenceTime 参考时刻（生成起点）
     * @return 按开始时刻升序的班次槽位列表（含停产标记）；缺班次配置时返回窗口内已解析部分
     */
    private List<RecalcShiftSlot> buildShiftCalendar(LhScheduleContext context, Date referenceTime) {
        List<RecalcShiftSlot> slotList = new ArrayList<>();
        Map<LocalDate, MdmWorkCalendar> calendarMap = this.loadCalendarMap(context);
        Date windowEnd = null;
        if (!CollectionUtils.isEmpty(context.getScheduleWindowShifts())) {
            for (LhShiftConfigVO shift : context.getScheduleWindowShifts()) {
                Date shiftStart = shift.getShiftStartDateTime();
                Date shiftEnd = shift.getShiftEndDateTime();
                if (Objects.isNull(shiftStart) || Objects.isNull(shiftEnd)) {
                    continue;
                }
                slotList.add(new RecalcShiftSlot(shiftStart, shiftEnd, this.resolveShiftWorkDate(shiftStart),
                        ShiftTypeParseUtil.parse(shift.getShiftType()),
                        this.isShiftStoppedByCalendar(calendarMap, shiftStart, shift.getShiftType())));
                if (Objects.isNull(windowEnd) || shiftEnd.after(windowEnd)) {
                    windowEnd = shiftEnd;
                }
            }
        }
        Map<String, String[]> dayPatternMap = this.resolveDayPattern(context);
        Date scanAnchor = Objects.nonNull(windowEnd) ? windowEnd : referenceTime;
        if (Objects.isNull(scanAnchor) || dayPatternMap.isEmpty()) {
            slotList.sort(Comparator.comparing(RecalcShiftSlot::getShiftStart));
            log.info("结构可收尾时间重算: 班次日历仅覆盖排程窗口（窗口终点或日模式缺失），窗口外不参与外推, 槽位数: {}",
                    slotList.size());
            return slotList;
        }
        int maxScanDays = CALENDAR_ANCHOR_SCAN_DAYS + MAX_SCAN_DAY_EXTRA;
        LocalDate scanStart = this.resolveShiftWorkDate(scanAnchor);
        for (int dayOffset = 0; dayOffset <= maxScanDays; dayOffset++) {
            LocalDate workDate = scanStart.plusDays(dayOffset);
            Date workDateStart = Date.from(workDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            for (String shiftTypeLabel : this.resolveDayPatternOrder()) {
                String[] pattern = dayPatternMap.get(shiftTypeLabel);
                ShiftEnum shiftEnum = ShiftTypeParseUtil.parse(shiftTypeLabel);
                if (Objects.isNull(pattern) || Objects.isNull(shiftEnum)) {
                    continue;
                }
                Date[] bounds;
                if (ShiftEnum.NIGHT_SHIFT == shiftEnum) {
                    // 夜班归属日 D：D-1 日 22:00 → D 日 06:00（与排程模板「T+1 日夜班」一致）
                    Date previousDayStart = Date.from(workDate.minusDays(1)
                            .atStartOfDay(ZoneId.systemDefault()).toInstant());
                    bounds = ShiftBoundaryUtil.resolveAbsoluteBounds(previousDayStart, 0,
                            ShiftEnum.NIGHT_SHIFT, pattern[0], pattern[1]);
                } else {
                    bounds = ShiftBoundaryUtil.resolveAbsoluteBounds(workDateStart, 0,
                            shiftEnum, pattern[0], pattern[1]);
                }
                if (Objects.isNull(bounds[0]) || Objects.isNull(bounds[1]) || !bounds[1].after(bounds[0])) {
                    continue;
                }
                // 窗口内已按真实配置入列，避免重复
                if (Objects.nonNull(windowEnd) && !bounds[0].after(windowEnd)) {
                    continue;
                }
                slotList.add(new RecalcShiftSlot(bounds[0], bounds[1], workDate, shiftEnum,
                        this.isShiftStoppedByCalendar(calendarMap, bounds[0], shiftEnum.getCode())));
            }
        }
        slotList.sort(Comparator.comparing(RecalcShiftSlot::getShiftStart));
        log.debug("结构可收尾时间重算班次日历构建完成, 槽位数: {}, 顺序终点: {}",
                slotList.size(), LhScheduleTimeUtil.formatDateTime(scanAnchor));
        return slotList;
    }

    /**
     * 解析班次日历数据源：默认成型班次配置（与成型侧换模扣减点严格一致），可切换为硫化班次配置。
     *
     * @param context 排程上下文
     * @return 班次配置列表（未启用日模式推导时为空，由窗口班次兜底）
     */
    private List<LhShiftConfigVO> loadShiftCalendarSource(LhScheduleContext context) {
        Date scheduleDate = context.getScheduleDate();
        List<LhShiftConfig> configList = lhShiftConfigMapper.selectList(
                new LambdaQueryWrapper<LhShiftConfig>()
                        .eq(LhShiftConfig::getFactoryCode, context.getFactoryCode())
                        .orderByAsc(LhShiftConfig::getShiftIndex));
        List<LhShiftConfigVO> voList = new ArrayList<>();
        if (CollectionUtils.isEmpty(configList)) {
            return voList;
        }
        for (LhShiftConfig config : configList) {
            LhShiftConfigVO vo = new LhShiftConfigVO();
            vo.setFactoryCode(config.getFactoryCode());
            vo.setShiftCode(config.getShiftCode());
            vo.setShiftName(config.getShiftName());
            vo.setShiftIndex(config.getShiftIndex());
            vo.setShiftType(config.getShiftType());
            vo.setStartTime(config.getStartTime());
            vo.setEndTime(config.getEndTime());
            vo.setDateOffset(config.getDateOffset());
            vo.setShiftDuration(config.getShiftDuration());
            vo.setScheduleBaseDate(scheduleDate);
            voList.add(vo);
        }
        return voList;
    }

    /**
     * 解析日模式（班次标签 → [开始时刻, 结束时刻]）。
     *
     * @param context 排程上下文
     * @return 日模式映射；窗口班次缺失时返回空映射
     */
    private Map<String, String[]> resolveDayPattern(LhScheduleContext context) {
        Map<String, String[]> dayPatternMap = new LinkedHashMap<>();
        List<LhShiftConfigVO> sourceList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(context.getScheduleWindowShifts())) {
            sourceList.addAll(context.getScheduleWindowShifts());
        } else if (SHIFT_CALENDAR_SOURCE_LH.equals(SHIFT_CALENDAR_SOURCE)) {
            sourceList.addAll(this.loadShiftCalendarSource(context));
        }
        for (LhShiftConfigVO shift : sourceList) {
            String shiftTypeLabel = this.resolveShiftTypeLabel(shift);
            if (StringUtils.isEmpty(shiftTypeLabel) || StringUtils.isEmpty(shift.getStartTime())
                    || StringUtils.isEmpty(shift.getEndTime())) {
                continue;
            }
            dayPatternMap.putIfAbsent(shiftTypeLabel, new String[]{shift.getStartTime(), shift.getEndTime()});
        }
        return dayPatternMap;
    }

    /**
     * 日模式推演顺序：早 → 中 → 夜（与成型侧「按开始时刻升序」口径一致）。
     *
     * @return 固定顺序的班次标签列表
     */
    private List<String> resolveDayPatternOrder() {
        List<String> orderList = new ArrayList<>(3);
        orderList.add(MORNING_LABEL);
        orderList.add(AFTERNOON_LABEL);
        orderList.add(NIGHT_LABEL);
        return orderList;
    }

    /**
     * 班次配置 → 班次标签（早/中/夜）。
     *
     * @param shift 班次配置
     * @return 标签；无法识别返回 null
     */
    private String resolveShiftTypeLabel(LhShiftConfigVO shift) {
        ShiftEnum shiftEnum = Objects.nonNull(shift) ? shift.resolveShiftTypeEnum() : null;
        if (Objects.isNull(shiftEnum)) {
            return null;
        }
        if (ShiftEnum.MORNING_SHIFT == shiftEnum) {
            return MORNING_LABEL;
        }
        if (ShiftEnum.AFTERNOON_SHIFT == shiftEnum) {
            return AFTERNOON_LABEL;
        }
        return NIGHT_LABEL;
    }

    /**
     * 加载工作日历（按业务日）。
     *
     * @param context 排程上下文
     * @return 业务日 → 日历；上下文无日历时返回空映射
     */
    private Map<LocalDate, MdmWorkCalendar> loadCalendarMap(LhScheduleContext context) {
        Map<LocalDate, MdmWorkCalendar> calendarMap = new LinkedHashMap<>();
        if (CollectionUtils.isEmpty(context.getWorkCalendarList())) {
            return calendarMap;
        }
        for (MdmWorkCalendar calendar : context.getWorkCalendarList()) {
            if (Objects.isNull(calendar) || Objects.isNull(calendar.getProductionDate())) {
                continue;
            }
            LocalDate productionDate = calendar.getProductionDate().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
            calendarMap.putIfAbsent(productionDate, calendar);
        }
        return calendarMap;
    }

    /**
     * 判断班次是否停产（口径与成型侧 {@code ScheduleDayTypeHelper} 一致）。
     *
     * <p>整天停产：DAY_FLAG = 0；单班停产：早/中/夜对应的 ONE/TWO/THREE_SHIFT_FLAG = 0。
     * 无日历时按不停产处理（不因缺数据凭空推迟收尾）。
     *
     * @param calendarMap 业务日 → 日历
     * @param shiftStart  班次开始时刻
     * @param shiftType   班次类型编码（01 夜 / 02 早 / 03 中）
     * @return true=停产
     */
    private boolean isShiftStoppedByCalendar(Map<LocalDate, MdmWorkCalendar> calendarMap,
                                            Date shiftStart, String shiftType) {
        if (CollectionUtils.isEmpty(calendarMap) || Objects.isNull(shiftStart)) {
            return false;
        }
        LocalDate workDate = this.resolveShiftWorkDate(shiftStart);
        MdmWorkCalendar calendar = calendarMap.get(workDate);
        if (Objects.isNull(calendar)) {
            return false;
        }
        if (this.isFullDayStopped(calendar)) {
            return true;
        }
        ShiftEnum shiftEnum = ShiftTypeParseUtil.parse(shiftType);
        if (Objects.isNull(shiftEnum)) {
            return false;
        }
        String shiftFlag;
        if (ShiftEnum.MORNING_SHIFT == shiftEnum) {
            shiftFlag = calendar.getOneShiftFlag();
        } else if (ShiftEnum.AFTERNOON_SHIFT == shiftEnum) {
            shiftFlag = calendar.getTwoShiftFlag();
        } else {
            shiftFlag = calendar.getThreeShiftFlag();
        }
        return FULL_DAY_CLOSED_FLAG.equals(shiftFlag);
    }

    /**
     * 是否整天停产：DAY_FLAG = 0，或早/中/夜三个班次标记全部为 0。
     *
     * @param calendar 工作日历
     * @return true=整天停产
     */
    private boolean isFullDayStopped(MdmWorkCalendar calendar) {
        if (Objects.isNull(calendar)) {
            return false;
        }
        if (FULL_DAY_CLOSED_FLAG.equals(calendar.getDayFlag())) {
            return true;
        }
        return FULL_DAY_CLOSED_FLAG.equals(calendar.getOneShiftFlag())
                && FULL_DAY_CLOSED_FLAG.equals(calendar.getTwoShiftFlag())
                && FULL_DAY_CLOSED_FLAG.equals(calendar.getThreeShiftFlag());
    }

    /**
     * 还需班次：⌈成型余量 ÷ 组班产⌉（整数向上取整，宁晚勿早，与成型侧同写法）。
     *
     * @param remainder     结构成型余量
     * @param shiftCapacity 结构组班产
     * @return 还需班次数；参数无效返回 0
     */
    static int resolveExtraShifts(int remainder, int shiftCapacity) {
        if (remainder <= 0 || shiftCapacity <= 0) {
            return 0;
        }
        return (remainder + shiftCapacity - 1) / shiftCapacity;
    }

    /**
     * 推算收尾时刻：参考时刻之后的第 extraShifts 个有效班次的结束时刻。
     *
     * <p>上限与成型侧完全一致：有效班次数累计超过
     * {@link ScheduleConstants#MAX_PREDICT_SHIFTS} 仍未数满时返回 null（调用方不写时间）。
     *
     * @param shiftSlots    班次日历（按开始时刻升序）
     * @param referenceTime 参考时刻
     * @param extraShifts   还需班次数
     * @return 收尾时刻；超预测窗口或缺班次日历返回 null
     */
    Date extrapolateEndingTime(List<RecalcShiftSlot> shiftSlots, Date referenceTime, int extraShifts) {
        if (extraShifts <= 0 || CollectionUtils.isEmpty(shiftSlots) || Objects.isNull(referenceTime)) {
            return null;
        }
        int effectiveShiftCount = 0;
        for (RecalcShiftSlot slot : shiftSlots) {
            if (Objects.isNull(slot) || Objects.isNull(slot.getShiftStart())
                    || Objects.isNull(slot.getShiftEnd())) {
                continue;
            }
            // 参考时刻之前（含正在进行的班次起点）的班次已过去，不计入有效班次
            if (!slot.getShiftStart().after(referenceTime)) {
                continue;
            }
            if (slot.isStopped()) {
                continue;
            }
            effectiveShiftCount++;
            if (effectiveShiftCount > ScheduleConstants.MAX_PREDICT_SHIFTS) {
                return null;
            }
            if (effectiveShiftCount >= extraShifts) {
                return slot.getShiftEnd();
            }
        }
        return null;
    }

    /**
     * 回写结构可收尾时间：同结构全部行同值刷新（读侧取「同结构最早值」，只改一行会被其它行压住）。
     *
     * @param factoryCode    分厂编码
     * @param scheduleDate   排程日
     * @param structureName  前结构
     * @param structureRows  该结构全部记录
     * @param endingTime     重算后的收尾时刻
     * @param fixedSwitchHours 固定切换耗时（日历锚点用；为空则逐行取原记录 {@code SWITCH_HOURS}）
     * @param remainder      重算用成型余量
     * @param shiftCapacity  重算用结构组班产
     * @param referenceTime  参考时刻
     * @param recalcRemark   重算依据备注（追加到原备注后）
     * @return 实际更新行数
     */
    private int writeBack(String factoryCode, Date scheduleDate, String structureName,
                          List<CxEmbryoLhTime> structureRows, Date endingTime, Integer fixedSwitchHours,
                          int remainder, int shiftCapacity, Date referenceTime, String recalcRemark) {
        int updatedRows = 0;
        for (CxEmbryoLhTime row : structureRows) {
            // 切换耗时口径：日历锚点统一用锚点档位值（保证 EARLIEST = ENDING_TIME + 档位耗时），
            // 余量推算沿用原记录值（重算只改"前结构何时收完"，不改"换模要几小时"）
            int switchHours;
            if (Objects.nonNull(fixedSwitchHours)) {
                switchHours = fixedSwitchHours;
            } else if (Objects.nonNull(row.getSwitchHours())) {
                switchHours = row.getSwitchHours();
            } else {
                switchHours = 0;
                log.warn("结构可收尾时间重算: 切换耗时为空的记录按0小时处理, structureName: {}, machineCode: {}, id: {}",
                        structureName, row.getCxMachineCode(), row.getId());
            }
            CxEmbryoLhTime updateEntity = new CxEmbryoLhTime();
            updateEntity.setId(row.getId());
            updateEntity.setEndingTime(endingTime);
            updateEntity.setEarliestLhTime(LhScheduleTimeUtil.addHours(endingTime, switchHours));
            updateEntity.setRecalcTime(referenceTime);
            updateEntity.setRecalcRemainder(remainder);
            updateEntity.setRecalcShiftCapacity(shiftCapacity);
            updateEntity.setRemark(StringUtils.isEmpty(row.getRemark())
                    ? recalcRemark : row.getRemark() + "；" + recalcRemark);
            updateEntity.setUpdateTime(new Date());
            updatedRows += cxEmbryoLhTimeMapper.updateById(updateEntity);
        }
        // 同一「工厂 + 排程日 + 机台 + 前结构」出现多行时一并同值更新，只告警不静默漏改
        Map<String, Long> machineRowCountMap = structureRows.stream()
                .filter(row -> StringUtils.isNotEmpty(row.getCxMachineCode()))
                .collect(Collectors.groupingBy(CxEmbryoLhTime::getCxMachineCode, Collectors.counting()));
        machineRowCountMap.forEach((machineCode, rowCount) -> {
            if (rowCount > 1) {
                log.warn("结构可收尾时间重算: 同机台同结构存在{}行（已同值更新）, factoryCode: {}, scheduleDate: {}, "
                                + "machineCode: {}, structureName: {}",
                        rowCount, factoryCode, LhScheduleTimeUtil.formatDate(scheduleDate), machineCode, structureName);
            }
        });
        return updatedRows;
    }

    /**
     * 构建日历锚点备注：写明锚定的后结构与切换日来源，便于现场核对。
     *
     * @param anchor 日历锚点
     * @return 重算依据备注
     */
    private String buildCalendarRemark(CalendarAnchor anchor) {
        return String.format("%s：%s，后结构%s，收尾时间%s",
                REMARK_PREFIX_CALENDAR, anchor.getReason(), anchor.getNextStructureName(),
                LhScheduleTimeUtil.formatDateTime(anchor.getEndingTime()));
    }

    /**
     * 结构排产配置段在指定日是否仍占机（按覆盖区间判定，不取提前生产机台）。
     *
     * @param allocation   结构排产配置
     * @param referenceDay 参考日 00:00
     * @return true=当日占机
     */
    private boolean isAllocationActiveOnDay(MpStructureAllocation allocation, Date referenceDay) {
        if (Objects.isNull(allocation) || Objects.isNull(allocation.getBeginDay())
                || Objects.isNull(allocation.getEndDay()) || Objects.isNull(allocation.getYear())
                || Objects.isNull(allocation.getMonth())) {
            return false;
        }
        int referenceComposite = this.resolveYear(referenceDay) * 10000
                + this.resolveMonth(referenceDay) * 100 + this.resolveDay(referenceDay);
        int beginComposite = allocation.getYear() * 10000
                + allocation.getMonth() * 100 + allocation.getBeginDay();
        int endComposite = allocation.getYear() * 10000
                + allocation.getMonth() * 100 + allocation.getEndDay();
        return referenceComposite >= beginComposite && referenceComposite <= endComposite;
    }

    /**
     * 取班次开始时刻所属的业务日（默认时区）。
     *
     * @param shiftStart 班次开始时刻
     * @return 业务日；入参为空返回 null
     */
    private LocalDate resolveShiftWorkDate(Date shiftStart) {
        if (Objects.isNull(shiftStart)) {
            return null;
        }
        return shiftStart.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * 取日期年份（默认时区）。
     *
     * @param date 日期
     * @return 年份
     */
    private int resolveYear(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().getYear();
    }

    /**
     * 取日期月份（默认时区）。
     *
     * @param date 日期
     * @return 月份
     */
    private int resolveMonth(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().getMonthValue();
    }

    /**
     * 取日期日（默认时区）。
     *
     * @param date 日期
     * @return 日
     */
    private int resolveDay(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().getDayOfMonth();
    }

    /**
     * 日历锚点（重算内部使用）：三类日历场景的开口时刻。
     */
    private static final class CalendarAnchor {

        /** 后结构名（锚点用于该结构的胎胚可供硫化时间） */
        private final String nextStructureName;

        /** 锚点时刻（= 前结构实际停止生产时刻），即重算后的 ENDING_TIME */
        private final Date anchorSourceTime;

        /** 切换耗时（小时），EARLIEST_LH_TIME = 锚点时刻 + 该值 */
        private final int switchHours;

        /** 重算原因（区分纯降机台与转产转出） */
        private final String reason;

        CalendarAnchor(String nextStructureName, Date anchorSourceTime, int switchHours, String reason) {
            this.nextStructureName = nextStructureName;
            this.anchorSourceTime = anchorSourceTime;
            this.switchHours = switchHours;
            this.reason = reason;
        }

        String getNextStructureName() {
            return nextStructureName;
        }

        Date getAnchorSourceTime() {
            return anchorSourceTime;
        }

        Date getEndingTime() {
            return anchorSourceTime;
        }

        int getSwitchHours() {
            return switchHours;
        }

        String getReason() {
            return reason;
        }
    }

    /**
     * 结构排产配置段（重算内部使用）。
     */
    static final class AllocationSegment {

        /** 结构名 */
        private final String structureName;

        /** 成型机台编码 */
        private final String machineCode;

        /** 配置开始日（yyyyMMdd 复合值） */
        private final int beginDateValue;

        /** 配置结束日（yyyyMMdd 复合值） */
        private final int endDateValue;

        /** 配置开始日 */
        private final LocalDate beginDate;

        /** 配置结束日 */
        private final LocalDate endDate;

        private AllocationSegment(String structureName, String machineCode,
                                  int beginDateValue, int endDateValue,
                                  LocalDate beginDate, LocalDate endDate) {
            this.structureName = structureName;
            this.machineCode = machineCode;
            this.beginDateValue = beginDateValue;
            this.endDateValue = endDateValue;
            this.beginDate = beginDate;
            this.endDate = endDate;
        }

        /**
         * 由结构排产配置构建段快照。
         *
         * @param allocation 结构排产配置
         * @return 段快照；关键字段缺失返回 null
         */
        static AllocationSegment of(MpStructureAllocation allocation) {
            if (Objects.isNull(allocation) || StringUtils.isEmpty(allocation.getStructureName())
                    || StringUtils.isEmpty(allocation.getCxMachineCode())
                    || Objects.isNull(allocation.getBeginDay()) || Objects.isNull(allocation.getEndDay())
                    || Objects.isNull(allocation.getYear()) || Objects.isNull(allocation.getMonth())) {
                return null;
            }
            int beginDateValue = allocation.getYear() * 10000
                    + allocation.getMonth() * 100 + allocation.getBeginDay();
            int endDateValue = allocation.getYear() * 10000
                    + allocation.getMonth() * 100 + allocation.getEndDay();
            LocalDate beginDate = LocalDate.of(allocation.getYear(), allocation.getMonth(), allocation.getBeginDay());
            LocalDate endDate = LocalDate.of(allocation.getYear(), allocation.getMonth(), allocation.getEndDay());
            return new AllocationSegment(allocation.getStructureName(), allocation.getCxMachineCode(),
                    beginDateValue, endDateValue, beginDate, endDate);
        }

        String getStructureName() {
            return structureName;
        }

        String getMachineCode() {
            return machineCode;
        }

        int getBeginDateValue() {
            return beginDateValue;
        }

        int getEndDateValue() {
            return endDateValue;
        }

        LocalDate getBeginDate() {
            return beginDate;
        }

        LocalDate getEndDate() {
            return endDate;
        }
    }

    /**
     * 结构排产配置索引（重算内部使用）：降机台切换边 + 转产实例。
     */
    static final class AllocationIndex {

        /** 机台|前结构 → 降机台切换边 */
        private final Map<String, EdgeInfo> downgradeEdgeMap =
                new LinkedHashMap<>();

        /** 机台|结构 → 转产转出实例 */
        private final Map<String, TransferInfo> transferMap =
                new LinkedHashMap<>();

        Map<String, EdgeInfo> getDowngradeEdgeMap() {
            return downgradeEdgeMap;
        }

        Map<String, TransferInfo> getTransferMap() {
            return transferMap;
        }
    }

    /**
     * 降机台切换边（重算内部使用，字段与成型侧 {@code StructureChangeClassifier.Edge} 对齐）。
     */
    static final class EdgeInfo {

        /** 前结构 */
        private final String prevStructure;

        /** 后结构 */
        private final String nextStructure;

        /** 后结构配置开始日（切换日） */
        private final LocalDate nextBeginDate;

        EdgeInfo(String prevStructure, String nextStructure, LocalDate nextBeginDate) {
            this.prevStructure = prevStructure;
            this.nextStructure = nextStructure;
            this.nextBeginDate = nextBeginDate;
        }

        String getPrevStructure() {
            return prevStructure;
        }

        String getNextStructure() {
            return nextStructure;
        }

        LocalDate getNextBeginDate() {
            return nextBeginDate;
        }
    }

    /**
     * 转产转出实例（重算内部使用，字段与成型侧 {@code StructureChangeClassifier.Transfer} 对齐）。
     */
    static final class TransferInfo {

        /** 迁移的结构 */
        private final String structure;

        /** 转出机台的下段结构 */
        private final String fromNextStructure;

        /** 转入机台 */
        private final String toMachine;

        /** 转入机台该结构的配置开始日（切换日） */
        private final LocalDate toBeginDate;

        TransferInfo(String structure, String fromNextStructure, String toMachine, LocalDate toBeginDate) {
            this.structure = structure;
            this.fromNextStructure = fromNextStructure;
            this.toMachine = toMachine;
            this.toBeginDate = toBeginDate;
        }

        String getStructure() {
            return structure;
        }

        String getFromNextStructure() {
            return fromNextStructure;
        }

        String getToMachine() {
            return toMachine;
        }

        LocalDate getToBeginDate() {
            return toBeginDate;
        }
    }

    /**
     * 班次槽位（重算内部使用）：绝对起止时刻 + 业务日 + 班次类型 + 是否停产。
     */
    static final class RecalcShiftSlot {

        /** 班次开始时刻 */
        private final Date shiftStart;

        /** 班次结束时刻 */
        private final Date shiftEnd;

        /** 班次业务日 */
        private final LocalDate workDate;

        /** 班次类型 */
        private final ShiftEnum shiftEnum;

        /** 是否停产（停产班不参与班次计数） */
        private final boolean stopped;

        RecalcShiftSlot(Date shiftStart, Date shiftEnd, LocalDate workDate,
                        ShiftEnum shiftEnum, boolean stopped) {
            this.shiftStart = shiftStart;
            this.shiftEnd = shiftEnd;
            this.workDate = workDate;
            this.shiftEnum = shiftEnum;
            this.stopped = stopped;
        }

        Date getShiftStart() {
            return shiftStart;
        }

        Date getShiftEnd() {
            return shiftEnd;
        }

        LocalDate getWorkDate() {
            return workDate;
        }

        boolean isNightShift() {
            return ShiftEnum.NIGHT_SHIFT == shiftEnum;
        }

        boolean isStopped() {
            return stopped;
        }
    }
}
