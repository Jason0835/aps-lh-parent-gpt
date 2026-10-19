package com.zlt.aps.lh.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.enums.ShiftEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.component.TargetScheduleQtyResolver;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.engine.strategy.support.OffMachineDecision;
import com.zlt.aps.lh.engine.strategy.support.TimedMachineOffReleaseResolver;
import com.zlt.aps.lh.engine.strategy.support.TimedMachineOffRequest;
import com.zlt.aps.lh.engine.strategy.support.TimedMachineOffLoadPreview;
import com.zlt.aps.lh.engine.strategy.support.PreviousAlternatePlanReleaseEvent;
import com.zlt.aps.lh.engine.strategy.support.PreviousAlternateMachineOrder;
import com.zlt.aps.lh.util.LhMouldCodeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import com.zlt.aps.lh.util.ResultShiftTimeUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 按时间下机的独立班次决策与提交服务。
 * 在已选集合内确定规则节点，并根据最终生产完成发布真实释放，不选择后料或预占正式换模次数。
 * 所有状态存于批次上下文；本服务不保存跨批次可变状态。
 */
@Slf4j
@Service
public class TimedMachineOffShiftService {
    /**
     * 次数账本早班槽位，沿用正式换模统计口径。
     */
    private static final int MORNING_INDEX = 0;
    /**
     * 次数账本中班槽位。
     */
    private static final int AFTERNOON_INDEX = 1;
    /**
     * 结果分析中统一标明独立时间边界。
     */
    private static final String TIMED_OFF_ANALYSIS = "按时间下机边界调整";
    /**
     * 仅调用无副作用余量预览，正式扣账仍由原续作入口完成。
     */
    private final TargetScheduleQtyResolver quantityResolver = new TargetScheduleQtyResolver();
    /**
     * 根据最终生产事实计算释放，预演与正式提交共用。
     */
    private final TimedMachineOffReleaseResolver releaseResolver = new TimedMachineOffReleaseResolver();

    /**
     * 从需求日逐日读取正式次数，选择窗口内合法的早班或中班。
     *
     * @param context 本批上下文，包含参数、真实班次和正式次数账本
     * @param startDate 前置选机确定的需求日期
     * @param machineCode 已选定机台，仅用于日志
     * @param sku 已选定机台的来源 SKU，仅用于日志
     * @return 决策；无合法班次时下机时间为空，占用保持到窗口末端
     */
    public OffMachineDecision resolveOffMachineTime(LhScheduleContext context, LocalDate startDate,
            String machineCode, SkuScheduleDTO sku) {
        return this.resolveOffMachineTime(context, startDate, machineCode, sku, null, null);
    }

    /**
     * 正式计算复用同一日期循环，额外校验真实画像的物理交接边界。
     *
     * @param context 上下文
     * @param startDate 下机需求日
     * @param machineCode 已选机台
     * @param sku 来源SKU
     * @param request 已冻结画像；纯班次规则预览时为空
     * @param preview 独立预演次数账本
     * @return 日期、班次、实际下机或窗口占用决策
     */
    private OffMachineDecision resolveOffMachineTime(LhScheduleContext context, LocalDate startDate,
            String machineCode, SkuScheduleDTO sku, TimedMachineOffRequest request,
            TimedMachineOffLoadPreview preview) {
        return this.resolveOffMachineTime(context, startDate, machineCode, sku, request, preview, false);
    }

    /**
     * 在真实班次、配额及前序历史机台释放边界内确定合法下机时间。
     *
     * @param context 上下文
     * @param startDate 最早日期
     * @param machineCode 机台编码
     * @param sku 来源SKU
     * @param request 冻结画像，纯班次预览时为空
     * @param preview 独立事件账本
     * @param preferStructureHandoff 是否复核已验证的结构接替
     * @return 合法下机决策或窗口末端占用决策
     */
    private OffMachineDecision resolveOffMachineTime(LhScheduleContext context, LocalDate startDate,
            String machineCode, SkuScheduleDTO sku, TimedMachineOffRequest request,
            TimedMachineOffLoadPreview preview, boolean preferStructureHandoff) {
        List<LhShiftConfigVO> shifts = context.getScheduleWindowShifts();
        if (Objects.isNull(startDate) || CollectionUtils.isEmpty(shifts)) {
            throw new IllegalArgumentException("按时间下机缺少需求日期或排程班次");
        }
        LocalDate lastDate = shifts.stream().map(shift -> this.toLocalDate(shift.getWorkDate()))
                .max(LocalDate::compareTo).get();
        Date orderBoundary = Objects.isNull(request) ? null : this.resolvePreviousReleaseBoundary(context, request);
        for (LocalDate date = startDate; !date.isAfter(lastDate); date = date.plusDays(1)) {
            // 逐日重新读取限额及其中位阈值，不沿用顺延前日期的历史上限。
            int morningMax = LhScheduleTimeUtil.getMorningMouldChangeLimit(context, date.toString());
            int middleMax = LhScheduleTimeUtil.getAfternoonMouldChangeLimit(context, date.toString());
            int dailyMax = LhScheduleTimeUtil.getDailyMouldChangeLimit(context, date.toString());

            int[] counts = Objects.isNull(preview) ? context.getDailyMouldChangeCountMap().get(date.toString())
                    : preview.countsFor(date, request.getProfile().getOriginals().stream()
                            .map(LhScheduleResult::getLhMachineCode).collect(Collectors.toList()));
            int morning = Objects.nonNull(counts) ? counts[MORNING_INDEX] : 0;
            int middle = Objects.nonNull(counts) ? counts[AFTERNOON_INDEX] : 0;
            // 日额度独立于班次额度；用长整数避免求和或中位比较溢出。
            if ((long) morning + middle >= dailyMax || (morning >= morningMax && middle >= middleMax)) {
                this.logDecision(context, sku, machineCode, date, morning, middle, morningMax,
                        middleMax, dailyMax, null, "6.1.7 当日换模额度已满，下一日早班优先");
                continue;
            }
            boolean deferredDay = date.isAfter(startDate);
            boolean afternoon = !deferredDay && (morning >= morningMax
                    || (2L * morning > morningMax && 2L * middle <= morningMax));
            String reason = deferredDay ? "6.1.7 顺延日优先早班，逐班检查硬约束"
                    : this.resolveRuleReason(morning, middle, morningMax);
            String handoffReason = preferStructureHandoff && Objects.nonNull(preview)
                    ? preview.structureHandoffCandidateReason(request.getProfile().getOriginals().stream()
                            .map(LhScheduleResult::getLhMachineCode).collect(Collectors.toList()), date) : null;
            if (preferStructureHandoff && (Objects.isNull(handoffReason) || morning >= morningMax)) {
                continue;
            }
            if (Objects.nonNull(handoffReason)) {
                afternoon = false;
                reason = "同结构合法接替优先于中位均衡，" + handoffReason;
            }
            // 首选失败后只向后检查当天剩余班次；顺延日重新从早班开始，不再套中位偏好。
            int firstSlot = afternoon ? AFTERNOON_INDEX : MORNING_INDEX;
            int lastSlot = preferStructureHandoff ? MORNING_INDEX : AFTERNOON_INDEX;
            for (int slot = firstSlot; slot <= lastSlot; slot++) {
                String shiftType = slot == MORNING_INDEX
                        ? ShiftEnum.MORNING_SHIFT.getCode() : ShiftEnum.AFTERNOON_SHIFT.getCode();
                LhShiftConfigVO candidate = this.findShift(shifts, date, shiftType);
                if ((slot == MORNING_INDEX ? morning >= morningMax : middle >= middleMax)
                        || Objects.isNull(candidate) || !candidate.isAllowMouldChange()
                        || LhScheduleTimeUtil.isNoMouldChangeTime(context, candidate.getShiftStartDateTime())) {
                    this.logDecision(context, sku, machineCode, date, morning, middle, morningMax,
                            middleMax, dailyMax, null, "候选班次=" + shiftType + "，额度或换模窗口不合法，尝试下一候选");
                    continue;
                }
                if (Objects.nonNull(orderBoundary) && candidate.getShiftStartDateTime().before(orderBoundary)) {
                    this.logDecision(context, sku, machineCode, date, morning, middle, morningMax,
                            middleMax, dailyMax, null, "候选班次=" + shiftType + "，早于同SKU前序规则边界，尝试下一候选");
                    continue;
                }
                OffMachineDecision decision = new OffMachineDecision();
                decision.setPlanDate(date);
                decision.setShift(shiftType);
                decision.setRuleOffMachineTime(candidate.getShiftStartDateTime());
                decision.setOffMachineTime(candidate.getShiftStartDateTime());
                decision.setOccupancyEndTime(candidate.getShiftStartDateTime());
                decision.setReason(slot == firstSlot ? reason : reason + "；首选不合法，改用当天中班");
                if (Objects.nonNull(request) && !this.isPhysicalReleaseFeasible(context, request, decision)) {
                    this.logDecision(context, sku, machineCode, date, morning, middle, morningMax,
                            middleMax, dailyMax, null, "候选班次=" + shiftType + "，早于真实物理交接时间，尝试下一候选");
                    continue;
                }
                this.logDecision(context, sku, machineCode, date, morning, middle, morningMax,
                        middleMax, dailyMax, decision, decision.getReason());
                return decision;
            }
        }
        OffMachineDecision decision = new OffMachineDecision();
        decision.setOccupancyEndTime(shifts.stream().map(LhShiftConfigVO::getShiftEndDateTime)
                .max(Date::compareTo).get());
        decision.setReason("排程窗口内无合法规则节点，先计算窗口内生产量，再按实际完成时间释放");
        String detail = String.format("SKU=%s, 机台=%s, 最终下机时间=无, 占用截止=%s, 原因=%s",
                sku.getMaterialCode(), machineCode, LhScheduleTimeUtil.formatDateTime(decision.getOccupancyEndTime()),
                decision.getReason());
        log.info("按时间下机窗口未安排, {}", detail);
        PriorityTraceLogHelper.appendProcessLog(context, "按时间下机窗口未安排", detail);
        return decision;
    }

    /**
     * 余量收尾后、统一扣账前执行，在既定集合内消费前置历史机台顺序。
     *
     * @param context 本批上下文
     */
    public void resolveAndApply(LhScheduleContext context) {
        this.resolveAndApply(context, null);
    }

    /**
     * 按统一历史顺序完成两轮联合决策，预演与正式计算复用同一释放约束。
     *
     * @param context 本批上下文
     * @param preview 一次性有界负荷预演，空值只读取已提交次数
     */
    public void resolveAndApply(LhScheduleContext context, TimedMachineOffLoadPreview preview) {
        if (CollectionUtils.isEmpty(context.getTimedMachineOffRequests())) {
            return;
        }
        if (context.isContinuousDailyQuotaSynced() || !CollectionUtils.isEmpty(context.getTimedMachineOffDecisionMap())) {
            throw new IllegalStateException("按时间下机必须在统一扣账前执行且不可重复提交");
        }
        TimedMachineOffLoadPreview ledger = Objects.isNull(preview) ? null : preview.copyForDecision();
        // 仅在同SKU的既定候选位置内排序，不改变SKU之间的竞争顺序和已选集合。
        List<TimedMachineOffRequest> orderedRequests = this.orderRequests(context);
        // 首轮逐请求迁移自己的成功交接事件，后续请求读取更新后的负荷和前序释放边界。
        for (TimedMachineOffRequest request : orderedRequests) {
            if (this.hasFixedRelease(context, request)) {
                PriorityTraceLogHelper.appendProcessLog(context, "按时间下机硬边界保留",
                        "SKU=" + request.getSourceSku().getMaterialCode() + ", 机台="
                                + request.getProfile().getOriginals().get(0).getLhMachineCode()
                                + ", 原因=已有强制交替、故障、喷砂、精度或后续占用，不搬动既定边界");
                continue;
            }
            OffMachineDecision decision = this.resolveOffMachineTime(context, request.getStartDate(),
                    request.getProfile().getOriginals().get(0).getLhMachineCode(), request.getSourceSku(), request, ledger);
            decision = this.applyProductionBoundary(context, request, decision);
            this.movePreviewActions(context, ledger, request, decision);
        }
        if (Objects.isNull(ledger)) {
            return;
        }
        // 第二轮只允许结构接替请求向前收敛，绝不再次搬回其他请求，避免早中班来回振荡。
        // 最多两次线性请求扫描，不再次调用候选选机、历史承接或整批排程。
        int adjustedCount = 0;
        for (TimedMachineOffRequest request : orderedRequests) {
            LhScheduleResult original = request.getProfile().getOriginals().get(0);
            OffMachineDecision current = context.getTimedMachineOffDecisionMap().get(original);
            if (Objects.isNull(current) || this.hasFixedRelease(context, request)) {
                continue;
            }
            List<String> machineCodes = request.getProfile().getOriginals().stream()
                    .map(LhScheduleResult::getLhMachineCode).collect(Collectors.toList());
            if (!ledger.hasStructureHandoffCandidate(machineCodes)) {
                continue;
            }
            TimedMachineOffLoadPreview accepted = this.tryStructureHandoff(context, request, current, ledger, machineCodes);
            if (Objects.nonNull(accepted)) {
                ledger = accepted;
                adjustedCount++;
            }
        }
        String detail = "批次=" + context.getBatchNo() + ", 请求数=" + context.getTimedMachineOffRequests().size()
                + ", 请求扫描轮数=2, 结构接替提前释放数=" + adjustedCount + ", 整批负荷预演=沿用一次结果, 额外选机次数=0, 固定后料复核=按事件执行, 正式次数账本=未修改";
        log.info("按时间下机联合决策完成, {}", detail);
        PriorityTraceLogHelper.appendProcessLog(context, "按时间下机联合决策完成", detail);
    }

    /**
     * 在隔离候选状态提前释放当前普通降模机台，完整固定重放通过后才保留决定。
     * 主预演证据仅用于形成试算节点，失败不能清零前料或搬动余量收尾。
     *
     * @param context 当前正式时间决定及冻结余量收尾结果
     * @param request 当前可调整的普通时间下机请求
     * @param current 已确认的首轮释放决定
     * @param ledger 当前独立模拟负荷
     * @param machineCodes 当前物理机台的运行态编码
     * @return 完整复核通过的模拟账本，候选不成立时为空
     */
    private TimedMachineOffLoadPreview tryStructureHandoff(LhScheduleContext context, TimedMachineOffRequest request,
            OffMachineDecision current, TimedMachineOffLoadPreview ledger, List<String> machineCodes) {
        List<SkuScheduleDTO> sources = new ArrayList<>(context.getContinuousSkuList());
        sources.addAll(context.getNewSpecSkuList());
        sources.addAll(context.getPreviousAlternateNewSpecCandidates());
        sources.addAll(context.getScheduleResultSourceSkuMap().values());
        sources.add(request.getSourceSku());
        TimedMachineOffPreviewSnapshot snapshot = new TimedMachineOffPreviewSnapshot(context, sources);
        boolean accepted = false;
        boolean attempted = false;
        try {
            OffMachineDecision candidate = this.resolveOffMachineTime(context, request.getStartDate(),
                    request.getProfile().getOriginals().get(0).getLhMachineCode(),
                    request.getSourceSku(), request, ledger, true);
            if (Objects.isNull(candidate.getOffMachineTime())
                    || !candidate.getOffMachineTime().before(current.getOccupancyEndTime())) {
                return null;
            }
            attempted = true;
            log.info("按时间下机结构接替隔离试算, 工厂={}, 批次={}, 机台={}, 首轮释放={}, 候选规则节点={}",
                    context.getFactoryCode(), context.getBatchNo(), String.join(",", machineCodes),
                    LhScheduleTimeUtil.formatDateTime(current.getOccupancyEndTime()),
                    LhScheduleTimeUtil.formatDateTime(candidate.getRuleOffMachineTime()));
            candidate = this.applyProductionBoundary(context, request, candidate);
            TimedMachineOffLoadPreview candidateLedger = ledger.copyForDecision();
            this.movePreviewActions(context, candidateLedger, request, candidate);
            String movements = candidateLedger.refreshStructureHandoffs(context);
            LocalDate date = candidate.getPlanDate();
            int[] counts = candidateLedger.countsFor(date, machineCodes);
            if (Objects.isNull(candidateLedger.structureHandoffReason(machineCodes, date))
                    || counts[MORNING_INDEX] >= LhScheduleTimeUtil.getMorningMouldChangeLimit(context, date.toString())
                    || (long) counts[MORNING_INDEX] + counts[AFTERNOON_INDEX]
                    >= LhScheduleTimeUtil.getDailyMouldChangeLimit(context, date.toString())) {
                return null;
            }
            String detail = "批次=" + context.getBatchNo() + ", 机台=" + String.join(",", machineCodes)
                    + ", 事件=" + movements + ", 结构接替证据=已随固定动作重验，正式次数账本=未修改";
            log.info("按时间下机结构接替负荷复核, {}", detail);
            PriorityTraceLogHelper.appendProcessLog(context, "按时间下机结构接替负荷复核", detail);
            accepted = true;
            return candidateLedger;
        } finally {
            if (!accepted) {
                snapshot.restore(context);
                if (attempted) {
                    String detail = "工厂=" + context.getFactoryCode() + ", 批次=" + context.getBatchNo()
                            + ", 机台=" + String.join(",", machineCodes) + ", 恢复释放="
                            + LhScheduleTimeUtil.formatDateTime(current.getOccupancyEndTime())
                            + ", 结论=隔离试算未提交，前料数量和首次下机决定已恢复";
                    log.info("按时间下机结构接替未采用, {}", detail);
                    PriorityTraceLogHelper.appendProcessLog(context, "按时间下机结构接替未采用", detail);
                }
            }
        }
    }

    /**
     * 在同物料同状态的可调整请求位置内稳定排序，预演和正式两轮共用同一入口。
     * 未命中机台保持相对顺序，固定边界机台以及其他SKU的位置不移动。
     *
     * @param context 本批已选请求及固定历史顺序
     * @return 独立有序列表，不修改上下文请求集合
     */
    private List<TimedMachineOffRequest> orderRequests(LhScheduleContext context) {
        List<TimedMachineOffRequest> ordered = new ArrayList<>(context.getTimedMachineOffRequests());
        Map<String, List<Integer>> positions = new LinkedHashMap<>();
        for (int position = 0; position < ordered.size(); position++) {
            TimedMachineOffRequest request = ordered.get(position);
            if (!this.hasFixedRelease(context, request)) {
                SkuScheduleDTO sku = request.getSourceSku();
                positions.computeIfAbsent(sku.getMaterialCode() + "|" + sku.getProductStatus(),
                        key -> new ArrayList<>()).add(position);
            }
        }
        positions.forEach((skuKey, slots) -> {
            List<TimedMachineOffRequest> candidates = slots.stream().map(ordered::get).collect(Collectors.toList());
            candidates.sort(Comparator.comparingInt(request -> this.requestRank(context, request)));
            for (int position = 0; position < slots.size(); position++) {
                ordered.set(slots.get(position), candidates.get(position));
            }
            if (candidates.stream().anyMatch(request -> this.requestRank(context, request) < PreviousAlternateMachineOrder.UNMATCHED)) {
                String detail = "批次=" + context.getBatchNo() + ", SKU=" + skuKey
                        + ", 预演=" + context.isTimedMachineOffLoadPreview() + ", 机台顺序="
                        + candidates.stream().map(request -> request.getProfile().getOriginals().get(0).getLhMachineCode()
                        + ":" + this.requestRank(context, request)).collect(Collectors.joining(","));
                PriorityTraceLogHelper.appendProcessLog(context, "按时间下机历史机台顺序", detail);
                log.info("按时间下机历史机台顺序, {}", detail);
            }
        });
        return ordered;
    }

    /**
     * 读取物理机台请求的统一历史排名。
     *
     * @param context 已初始化排名的上下文
     * @param request 固定物理机台请求
     * @return 历史排名，未命中时为 UNMATCHED
     */
    private int requestRank(LhScheduleContext context, TimedMachineOffRequest request) {
        return context.getPreviousAlternateMachineOrder()
                .rank(request.getProfile().getOriginals().get(0).getLhMachineCode());
    }

    /**
     * 读取同SKU中更高历史优先级请求已经确认的合法释放时间。
     * 固定边界或尚无合法规则节点的机台不阻塞其他机台；实际量尽释放不受此排序延长。
     *
     * @param context 本批请求及已确认时间决策
     * @param request 当前待决策请求
     * @return 当前机台不得早于的时间；没有可比较前序时为空
     */
    private Date resolvePreviousReleaseBoundary(LhScheduleContext context, TimedMachineOffRequest request) {
        int rank = this.requestRank(context, request);
        return context.getTimedMachineOffRequests().stream()
                .filter(previous -> previous != request
                        && StringUtils.equals(previous.getSourceSku().getMaterialCode(), request.getSourceSku().getMaterialCode())
                        && StringUtils.equals(previous.getSourceSku().getProductStatus(), request.getSourceSku().getProductStatus()))
                .filter(previous -> this.requestRank(context, previous) < rank && !this.hasFixedRelease(context, previous))
                .map(previous -> context.getTimedMachineOffDecisionMap().get(previous.getProfile().getOriginals().get(0)))
                .filter(Objects::nonNull).map(OffMachineDecision::getRuleOffMachineTime).filter(Objects::nonNull)
                .max(Date::compareTo).orElse(null);
    }

    /**
     * 仅重验当前物理机首次后料动作，并更新独立模拟次数。
     *
     * @param context 上下文
     * @param ledger 模拟事件账本
     * @param request 请求
     * @param decision 新的合法下机边界
     */
    private void movePreviewActions(LhScheduleContext context, TimedMachineOffLoadPreview ledger,
            TimedMachineOffRequest request, OffMachineDecision decision) {
        if (Objects.isNull(ledger)) {
            return;
        }
        String movements = ledger.moveOwnActions(context, request.getProfile().getOriginals().stream()
                .map(LhScheduleResult::getLhMachineCode).collect(Collectors.toList()), decision);
        if (!movements.isEmpty()) {
            String detail = "批次=" + context.getBatchNo() + ", 事件=" + movements
                    + ", 槽位0=早班/1=中班, 仅迁移模拟次数，正式后料仍须重验时间轴";
            log.info("按时间下机模拟事件迁移, {}", detail);
            PriorityTraceLogHelper.appendProcessLog(context, "按时间下机模拟事件迁移", detail);
        }
    }

    /**
     * 检查候选节点能否完成物理交接；复用画像内的胶囊、停机、单控释放约束。
     *
     * @param context 上下文
     * @param request 清零前画像
     * @param decision 待检查的下机节点
     * @return 实际物理可释放时间不晚于候选节点
     */
    private boolean isPhysicalReleaseFeasible(LhScheduleContext context, TimedMachineOffRequest request,
            OffMachineDecision decision) {
        LhShiftConfigVO productionStart = TimedMachineOffReleaseResolver.resolveProductionStartShift(
                context.getScheduleWindowShifts(), request.getStartDate());
        int quantity = this.resolveProfileQuantity(context, request, productionStart, decision);
        Date completed = request.getProfile().completionTime(quantity);
        Date physicalReady = request.getProfile().physicalReadyTime(
                Objects.nonNull(completed) ? completed : productionStart.getShiftStartDateTime(),
                decision.getRuleOffMachineTime());
        return Objects.nonNull(physicalReady) && !physicalReady.after(decision.getOffMachineTime());
    }

    /**
     * 按真实画像补齐等待期间生产，只改需求日起的班次；原前缀和余量收尾不受影响。
     *
     * @param context 上下文
     * @param request 已选物理机台及清零前画像
     * @param decision 日期循环计算出的规则边界
     * @return 按最终生产量计算的独立物理释放决策
     */
    private OffMachineDecision applyProductionBoundary(LhScheduleContext context, TimedMachineOffRequest request,
            OffMachineDecision decision) {
        List<LhScheduleResult> originals = request.getProfile().getOriginals();
        List<LhScheduleResult> proposed = originals.stream()
                .map(result -> BeanUtil.copyProperties(result, LhScheduleResult.class)).collect(Collectors.toList());
        List<LhShiftConfigVO> shifts = context.getScheduleWindowShifts();
        LhShiftConfigVO productionStart = TimedMachineOffReleaseResolver.resolveProductionStartShift(
                shifts, request.getStartDate());
        // 原画像提供真实产能，SKU共享余量限制实际补量；不得挤占其他已保留机台或虚增尾量。
        int quantity = this.resolveProfileQuantity(context, request, productionStart, decision);
        request.getProfile().writePlan(proposed, quantity);
        for (int side = 0; side < originals.size(); side++) {
            LhScheduleResult result = originals.get(side);
            LhScheduleResult proposal = proposed.get(side);
            for (LhShiftConfigVO shift : shifts) {
                if (shift.getShiftStartDateTime().before(productionStart.getShiftStartDateTime())) {
                    continue;
                }
                int index = shift.getShiftIndex();
                ShiftFieldUtil.setShiftPlanQty(result, index, ShiftFieldUtil.getShiftPlanQty(proposal, index),
                        ShiftFieldUtil.getShiftStartTime(proposal, index), ShiftFieldUtil.getShiftEndTime(proposal, index));
                ShiftFieldUtil.appendShiftAnalysis(result, index, TIMED_OFF_ANALYSIS);
            }
            // 原对象身份保留，来源 SKU 与机台分配索引继续引用同一份结果。
            context.getEndingFillAllowedOverQtyMap().remove(result);
            context.getEndingFillBeforeQtyMap().remove(result);
            context.getSharedEmbryoEndingStaggerAllowedOverQtyMap().remove(result);
        }
        OffMachineDecision actual = this.releaseResolver.resolve(request, decision, shifts,
                productionStart.getShiftStartDateTime());
        this.registerReleaseDecision(context, request, actual);
        return actual;
    }

    /**
     * 将同一物理释放事实回写各侧，保留原结果身份及预演快照中的旧决策对象。
     *
     * @param context 本批上下文
     * @param request 已冻结的物理机台请求
     * @param decision 新生成的最终释放决策
     */
    private void registerReleaseDecision(LhScheduleContext context, TimedMachineOffRequest request,
            OffMachineDecision decision) {
        request.getProfile().getOriginals().forEach(result ->
                context.getTimedMachineOffDecisionMap().put(result, decision));
        for (LhScheduleResult result : request.getProfile().getOriginals()) {
            ResultShiftTimeUtil.refreshSummary(context, result);
            this.updateReleaseBoundary(context, result, decision);
        }
        String detail = String.format("工厂=%s, 批次=%s, SKU=%s, 状态=%s, 机台=%s, 规则节点=%s, "
                        + "前料总量=%s, 实际完成=%s, 物理释放=%s, 后料动作=需独立校验",
                context.getFactoryCode(), context.getBatchNo(), request.getSourceSku().getMaterialCode(),
                request.getSourceSku().getProductStatus(),
                request.getProfile().getOriginals().get(0).getLhMachineCode(),
                LhScheduleTimeUtil.formatDateTime(decision.getRuleOffMachineTime()), decision.getScheduledQty(),
                LhScheduleTimeUtil.formatDateTime(decision.getProductionEndTime()),
                LhScheduleTimeUtil.formatDateTime(decision.getOccupancyEndTime()));
        PriorityTraceLogHelper.appendProcessLog(context, "按时间下机实际释放", detail);
        log.info("按时间下机实际释放, {}", detail);
    }

    /**
     * 保留需求日前实际前缀，仅用同SKU未分配余量补足等待下机的生产，双侧按画像统一归整。
     *
     * @param context 上下文
     * @param request 当前物理机台请求
     * @param productionStart 需求日首个配置的早班或中班
     * @param decision 决策边界
     * @return 用于原画像写入的累计数量，不改变正式账本
     */
    private int resolveProfileQuantity(LhScheduleContext context, TimedMachineOffRequest request,
            LhShiftConfigVO productionStart, OffMachineDecision decision) {
        List<LhScheduleResult> own = request.getProfile().getOriginals();
        SkuScheduleDTO sku = request.getSourceSku();
        long otherQty = context.getScheduleResultList().stream().filter(result -> !own.contains(result))
                .filter(result -> StringUtils.equals(sku.getMaterialCode(), result.getMaterialCode())
                        && StringUtils.equals(sku.getProductStatus(), result.getProductStatus()))
                .mapToLong(ShiftFieldUtil::resolveScheduledQty).sum();
        long prefixQty = own.stream().mapToLong(result -> context.getScheduleWindowShifts().stream()
                .filter(shift -> shift.getShiftStartDateTime().before(productionStart.getShiftStartDateTime()))
                .mapToLong(shift -> {
                    Integer qty = ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex());
                    return Objects.nonNull(qty) ? qty : 0;
                }).sum()).sum();
        long tailBudget = Math.max(0L, this.quantityResolver.previewProductionRemainingQty(context, sku)
                - otherQty - prefixQty);
        long profileLimit = request.getProfile().quantityUntil(productionStart.getShiftStartDateTime()) + tailBudget;
        return request.getProfile().floorQuantity((int) Math.min(profileLimit,
                request.getProfile().quantityUntil(decision.getOccupancyEndTime())));
    }

    /**
     * 保留强制下机、已提交其他物料及同物料多状态专用链的原有时间语义。
     *
     * @param context 上下文
     * @param request 已选物理机台
     * @return 是否有不能搬动的既定时间边界
     */
    boolean hasFixedRelease(LhScheduleContext context, TimedMachineOffRequest request) {
        List<LhScheduleResult> own = request.getProfile().getOriginals();
        for (LhScheduleResult result : own) {
            String code = result.getLhMachineCode();
            MachineScheduleDTO machine = context.getMachineScheduleMap().get(code);
            if ((context.getPreviousAlternateReleaseEventMap().containsKey(code)
                    && !this.isAdjustablePreviousAlternate(context, request, code))
                    || context.getOnlySandBlastContinuationReleaseWindowMap().containsKey(code)
                    || context.getContinuationTemporaryFaultTransferEventMap().containsKey(code)
                    || context.isContinuousStopHoldMachine(code)
                    || context.getContinuationSurplusEndingAllocatedResults().contains(result)
                    || (Objects.nonNull(machine) && !CollectionUtils.isEmpty(machine.getMaintenanceWindowList())
                    && machine.getMaintenanceWindowList().stream().anyMatch(window -> window.isForceDown()))) {
                return true;
            }
            if (context.getScheduleResultList().stream().anyMatch(other -> !own.contains(other)
                    && StringUtils.equals(code, other.getLhMachineCode())
                    && (ShiftFieldUtil.resolveScheduledQty(other) > 0 || Objects.nonNull(other.getMouldChangeStartTime())))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断普通异料历史交替是否允许随时间下机同步释放。
     *
     * @param context 本批上下文
     * @param request 普通降模请求
     * @param code 运行态机台编码
     * @return 普通异料历史交替可参与动态下机
     */
    private boolean isAdjustablePreviousAlternate(LhScheduleContext context, TimedMachineOffRequest request, String code) {
        PreviousAlternatePlanReleaseEvent event = context.getPreviousAlternateReleaseEventMap().get(code);
        return Objects.nonNull(event) && !event.isRemainderFinishDeferred() && !event.isSingleMouldReplacement()
                && !event.isBeforeMaterialNotScheduled() && !request.getSourceSku().isStrictTargetQty()
                && StringUtils.equals(event.getMaterialCode(), request.getSourceSku().getMaterialCode())
                && !StringUtils.equals(event.getPlan().getBeforeMaterialCode(), event.getPlan().getAfterMaterialCode());
    }

    /**
     * 统一扣账及机台状态汇总后发布事实，避免零量清理或末班反算覆盖真实交接时间。
     *
     * @param context 已完成统一续作扣账的上下文
     */
    public void publishReleasedState(LhScheduleContext context) {
        for (TimedMachineOffRequest request : context.getTimedMachineOffRequests()) {
            OffMachineDecision previous = context.getTimedMachineOffDecisionMap()
                    .get(request.getProfile().getOriginals().get(0));
            if (Objects.isNull(previous)) {
                continue;
            }
            LhShiftConfigVO productionStart = TimedMachineOffReleaseResolver.resolveProductionStartShift(
                    context.getScheduleWindowShifts(), request.getStartDate());
            OffMachineDecision decision = this.releaseResolver.resolve(request, previous,
                    context.getScheduleWindowShifts(), productionStart.getShiftStartDateTime());
            this.registerReleaseDecision(context, request, decision);
            for (LhScheduleResult result : request.getProfile().getOriginals()) {
                MachineScheduleDTO machine = context.getMachineScheduleMap().get(result.getLhMachineCode());
                machine.setEstimatedEndTime(decision.getOccupancyEndTime());
                machine.setCurrentMaterialCode(result.getMaterialCode());
                machine.setCurrentMaterialDesc(result.getMaterialDesc());
                machine.setEnding(Objects.nonNull(decision.getOffMachineTime()));
                LhMouldCodeUtil.resolveInMachineMouldCodeSet(context, result.getLhMachineCode()).forEach(mould ->
                        context.getPreScheduledMouldReleaseTimeMap().put(mould, decision.getOccupancyEndTime()));
                this.refreshCapacityAndIndex(context, request, result, decision);
                if (ShiftFieldUtil.resolveScheduledQty(result) > 0) {
                    result.setSpecEndTime(decision.getOccupancyEndTime());
                    result.setTdaySpecEndTime(decision.getOccupancyEndTime());
                }
            }
        }
    }

    /**
     * 同步实际有量末班及分组释放日期，班中释放必须保留该班已生产数量。
     *
     * @param context 上下文
     * @param result 原机台结果
     * @param decision 最终决策
     */
    private void updateReleaseBoundary(LhScheduleContext context, LhScheduleResult result, OffMachineDecision decision) {
        int lastProductionShift = context.getScheduleWindowShifts().stream()
                .filter(shift -> !shift.getShiftEndDateTime().after(decision.getOccupancyEndTime())
                        || (Objects.nonNull(ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex()))
                        && ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex()) > 0))
                .mapToInt(LhShiftConfigVO::getShiftIndex).max().orElse(0);
        context.registerContinuousReducedMachineReleaseBoundary(result.getLhMachineCode(), lastProductionShift);
        PreviousAlternatePlanReleaseEvent event = context.getPreviousAlternateReleaseEventMap().get(result.getLhMachineCode());
        if (Objects.nonNull(event)) {
            // 普通降模历史承接必须消费同一时间下机事实，后置恢复不得将前料重新提前释放。
            event.setOfflineTime(decision.getOccupancyEndTime());
            context.getPreviousAlternateReleasedMachineTimeMap().put(result.getLhMachineCode(), decision.getOccupancyEndTime());
        }
        context.markContinuousStopHoldMachineReleased(result.getLhMachineCode());
        // 该键与既有降模分组一致，正式补偿阶段从新的释放业务日评估需求。
        String groupKey = MonthPlanDateResolver.buildMaterialStatusKey(result.getMaterialCode(), result.getProductStatus());
        LocalDate releaseDate = context.getTimedMachineOffDecisionMap().entrySet().stream()
                .filter(entry -> StringUtils.equals(result.getMaterialCode(), entry.getKey().getMaterialCode())
                        && StringUtils.equals(result.getProductStatus(), entry.getKey().getProductStatus()))
                .map(entry -> TimedMachineOffReleaseResolver.resolveReleaseBusinessDate(
                        context.getScheduleWindowShifts(), entry.getValue().getOccupancyEndTime()))
                .max(LocalDate::compareTo).orElse(TimedMachineOffReleaseResolver.resolveReleaseBusinessDate(
                        context.getScheduleWindowShifts(), decision.getOccupancyEndTime()));
        // 未参与本次调整的既定降模机台仍贡献分组末次释放日；旧模拟节点不再单向累积。
        for (LhScheduleResult other : context.getScheduleResultList()) {
            if (context.getTimedMachineOffDecisionMap().containsKey(other)
                    || !StringUtils.equals(result.getMaterialCode(), other.getMaterialCode())
                    || !StringUtils.equals(result.getProductStatus(), other.getProductStatus())) {
                continue;
            }
            Integer boundary = context.getContinuousReducedMachineReleaseBoundaryShiftIndex(other.getLhMachineCode());
            if (Objects.nonNull(boundary) && Objects.nonNull(other.getSpecEndTime())) {
                LocalDate otherDate = TimedMachineOffReleaseResolver.resolveReleaseBusinessDate(
                        context.getScheduleWindowShifts(), other.getSpecEndTime());
                if (otherDate.isAfter(releaseDate)) {
                    releaseDate = otherDate;
                }
            }
        }
        // 零量行可能已经移除，仍从降模来源身份和独立机台释放事实保留其他机台的晚释放。
        for (Map.Entry<String, Map<String, SkuScheduleDTO>> entry
                : context.getReducedContinuationMachineBeforeSkuMap().entrySet()) {
            boolean timedMachine = context.getTimedMachineOffDecisionMap().keySet().stream()
                    .anyMatch(original -> StringUtils.equals(
                            LhSingleControlMachineUtil.resolvePhysicalMachineCode(entry.getKey()),
                            LhSingleControlMachineUtil.resolvePhysicalMachineCode(original.getLhMachineCode())));
            boolean sameGroup = entry.getValue().values().stream().anyMatch(sku ->
                    StringUtils.equals(result.getMaterialCode(), sku.getMaterialCode())
                            && StringUtils.equals(result.getProductStatus(), sku.getProductStatus()));
            if (timedMachine || !sameGroup) {
                continue;
            }
            Date released = context.getPreviousAlternateReleasedMachineTimeMap().get(entry.getKey());
            MachineScheduleDTO otherMachine = context.getMachineScheduleMap().get(entry.getKey());
            if (Objects.isNull(released) && Objects.nonNull(otherMachine)) {
                released = otherMachine.getEstimatedEndTime();
            }
            if (Objects.nonNull(released)) {
                LocalDate otherDate = TimedMachineOffReleaseResolver.resolveReleaseBusinessDate(
                        context.getScheduleWindowShifts(), released);
                if (otherDate.isAfter(releaseDate)) {
                    releaseDate = otherDate;
                }
            }
        }
        context.registerReducedContinuationGroupLastReleaseDate(groupKey, releaseDate);
    }

    /**
     * 从精确物理释放时刻刷新班内剩余容量；后料仍按自身节拍、换模和首检重新计算。
     *
     * @param context 上下文
     * @param request 原物理产能画像
     * @param result 当前侧结果
     * @param decision 实际占用边界
     */
    private void refreshCapacityAndIndex(LhScheduleContext context, TimedMachineOffRequest request,
            LhScheduleResult result, OffMachineDecision decision) {
        MachineScheduleDTO machine = context.getMachineScheduleMap().get(result.getLhMachineCode());
        int[] machineCapacity = machine.getShiftRemainingCapacity();
        int[] contextCapacity = context.getMachineShiftCapacityMap().get(result.getLhMachineCode());
        List<LhShiftConfigVO> shifts = context.getScheduleWindowShifts();
        int side = request.getProfile().getOriginals().indexOf(result);
        for (int position = 0; position < shifts.size(); position++) {
            LhShiftConfigVO shift = shifts.get(position);
            int remaining = request.getProfile().remainingCapacityAfter(side, position, decision.getOccupancyEndTime());
            int index = shift.getShiftIndex();
            if (Objects.nonNull(machineCapacity) && index < machineCapacity.length) {
                machineCapacity[index] = remaining;
            }
            if (Objects.nonNull(contextCapacity) && index < contextCapacity.length) {
                contextCapacity[index] = remaining;
            }
            LocalDate date = this.toLocalDate(shift.getWorkDate());
            boolean occupied = shifts.stream().filter(candidate -> Objects.equals(candidate.getWorkDate(), shift.getWorkDate()))
                    .map(candidate -> ShiftFieldUtil.getShiftPlanQty(result, candidate.getShiftIndex()))
                    .filter(Objects::nonNull).anyMatch(quantity -> quantity > 0);
            if (occupied) {
                context.recordScheduledMachine(date, result.getStructureName(), result.getMaterialCode(),
                        result.getProductStatus(), result.getLhMachineCode());
            } else {
                context.removeScheduledMachine(date, result.getStructureName(), result.getMaterialCode(),
                        result.getProductStatus(), result.getLhMachineCode());
            }
        }
        ResultShiftTimeUtil.refreshMachine(context, result);
        // 零量行已被清理时，通用汇总不能推翻独立释放事实。
        machine.setEstimatedEndTime(decision.getOccupancyEndTime());
    }

    /**
     * 按满额优先于中位偏好的顺序解释命中的业务规则。
     *
     * @param morning 早班占用
     * @param middle 中班占用
     * @param morningMax 早班上限
     * @return 与优先级一致的命中原因
     */
    private String resolveRuleReason(int morning, int middle, int morningMax) {
        if (morning >= morningMax) {
            return "6.1.5 早班已满，中班未满，优先中班";
        }
        if (2L * morning <= morningMax) {
            return "6.1.2 早班不超过中位值";
        }
        if (2L * middle <= morningMax) {
            return "6.1.3 早班超过中位值，中班不超过中位值";
        }
        return "6.1.4 早中班均超过中位值，早班未满";
    }

    /**
     * 查找指定业务日与班别对应的真实窗口班次。
     *
     * @param shifts 实际班次
     * @param date 业务日
     * @param type 班次类型
     * @return 对应班次，窗口不存在则为空
     */
    private LhShiftConfigVO findShift(List<LhShiftConfigVO> shifts, LocalDate date, String type) {
        return shifts.stream().filter(shift -> date.equals(this.toLocalDate(shift.getWorkDate()))
                && StringUtils.equals(type, shift.getShiftType())).findFirst().orElse(null);
    }

    /**
     * 将排程时间转换为运行环境的业务日期。
     *
     * @param time 业务时间
     * @return 所属本地日期
     */
    private LocalDate toLocalDate(Date time) {
        return time.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /** @param context 上下文 @param sku 来源SKU @param machine 机台 @param date 判断日 @param morning 早班占用
     * @param middle 中班占用 @param morningMax 早班上限 @param middleMax 中班上限 @param dailyMax 每日上限
     * @param decision 最终决策，顺延时为空 @param reason 命中规则 */
    private void logDecision(LhScheduleContext context, SkuScheduleDTO sku, String machine, LocalDate date,
            int morning, int middle, int morningMax, int middleMax, int dailyMax,
            OffMachineDecision decision, String reason) {
        String detail = String.format("工厂=%s, 批次=%s, SKU=%s, 状态=%s, 机台=%s, 判断日期=%s, "
                        + "早班=%s/%s, 中班=%s/%s, 每日上限=%s, 中位阈值=%s, 最终班次=%s, 最终下机时间=%s, 规则=%s",
                context.getFactoryCode(), context.getBatchNo(), sku.getMaterialCode(), sku.getProductStatus(), machine,
                date, morning, morningMax, middle, middleMax, dailyMax, morningMax / 2.0,
                Objects.nonNull(decision) ? decision.getShift() : null,
                Objects.nonNull(decision) ? LhScheduleTimeUtil.formatDateTime(decision.getOffMachineTime()) : null, reason);
        log.info("按时间下机班次决策, {}", detail);
        PriorityTraceLogHelper.appendProcessLog(context, "按时间下机班次决策", detail);
    }
}
