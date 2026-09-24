package com.zlt.aps.lh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.enums.MouldChangeTypeEnum;
import com.zlt.aps.lh.api.enums.ShiftEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.context.MouldChangeQuotaLimits;
import com.zlt.aps.lh.context.MouldChangeQuotaSnapshot;
import com.zlt.aps.lh.mapper.LhMouldChangePlanEntityMapper;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.*;

/** 历史上限专用统计；不参与反选候选筛选，不写入本次实际次数账本。 */
@Slf4j
@Service
public class HistoricalMouldChangeQuotaService {
    /** 已有明确仅下机标记，不按宽泛的“模具置换”备注排除。 */
    private static final String OFF_MACHINE_ONLY_PREFIX = "临时性故障模具下机：";
    /** 分组中日期、班次的位置。 */
    private static final int DATE_INDEX = 0;
    private static final int SHIFT_INDEX = 1;
    /** 分组内左侧、右侧、未拆侧的次数槽位。 */
    private static final int LEFT_INDEX = 0;
    private static final int RIGHT_INDEX = 1;
    private static final int WHOLE_INDEX = 2;
    private static final int SIDE_COUNT_SIZE = 3;
    private static final int COVERED_DAYS = 2;
    /** 日期统计的早中班槽位，不使用窗口班次序号。 */
    private static final int MORNING_COUNT_INDEX = 0;
    private static final int AFTERNOON_COUNT_INDEX = 1;
    private static final int SHIFT_COUNT_SIZE = 2;

    @Resource
    private LhMouldChangePlanEntityMapper lhMouldChangePlanMapper;

    /**
     * 基础数据完成后初始化一次；查询异常直接透传到原加载失败机制。
     * @param context 当前排程上下文
     */
    public void initializeSnapshot(LhScheduleContext context) {
        if (Objects.nonNull(context.getMouldChangeQuotaSnapshot())) {
            return;
        }
        Date sourceDate = LhScheduleTimeUtil.clearTime(
                LhScheduleTimeUtil.addDays(context.getScheduleTargetDate(), -1));
        // 专用单表查询不限制精确交替类型，逻辑删除由框架处理；不扩大反选候选列表。
        List<LhMouldChangePlan> plans = lhMouldChangePlanMapper.selectList(
                new LambdaQueryWrapper<LhMouldChangePlan>()
                        .select(LhMouldChangePlan::getId, LhMouldChangePlan::getFactoryCode,
                                LhMouldChangePlan::getLhResultBatchNo, LhMouldChangePlan::getScheduleDate,
                                LhMouldChangePlan::getPlanDate, LhMouldChangePlan::getClassIndex,
                                LhMouldChangePlan::getChangeMouldType, LhMouldChangePlan::getAfterMaterialCode,
                                LhMouldChangePlan::getLhMachineCode, LhMouldChangePlan::getRemark)
                        .eq(LhMouldChangePlan::getFactoryCode, context.getFactoryCode())
                        .eq(LhMouldChangePlan::getScheduleDate, sourceDate));
        // 完整统计成功后才发布快照，失败不得留下半成品或参数兜底快照。
        context.setMouldChangeQuotaSnapshot(this.buildSnapshot(context, plans));
    }

    /**
     * 聚合独立历史查询的结果；包级入口供无数据库的统计回归测试使用。
     * @param context 当前排程上下文
     * @param plans 专用查询结果，空列表表示正常无历史，异常不得转换为空列表
     * @return 本次只读限额快照
     */
    MouldChangeQuotaSnapshot buildSnapshot(LhScheduleContext context, List<LhMouldChangePlan> plans) {
        LocalDate startDate = LocalDate.parse(LhScheduleTimeUtil.formatDate(context.getScheduleDate()));
        LocalDate sourceDate = LocalDate.parse(LhScheduleTimeUtil.formatDate(context.getScheduleTargetDate())).minusDays(1);
        MouldChangeQuotaLimits base = new MouldChangeQuotaLimits(
                LhScheduleTimeUtil.getMorningMouldChangeLimit(context),
                LhScheduleTimeUtil.getAfternoonMouldChangeLimit(context),
                LhScheduleTimeUtil.getDailyMouldChangeLimit(context));
        Map<List<String>, int[]> groups = new LinkedHashMap<>(Math.max(16, plans.size()));
        Map<String, int[]> raw = this.emptyDayCounts(startDate);
        Set<Long> sourceIds = new HashSet<>(Math.max(16, plans.size()));
        Set<String> sourceBatches = new LinkedHashSet<>(4);
        for (LhMouldChangePlan plan : plans) {
            // 校验、动作过滤与源记录去重仅影响历史上限统计，不改变实际事件账本。
            if (!this.isCountable(context, plan, sourceDate, startDate)) {
                continue;
            }
            if (!sourceIds.add(plan.getId())) {
                this.logExcluded(context, plan, "同一源记录重复读取");
                continue;
            }
            sourceBatches.add(plan.getLhResultBatchNo());
            String dateKey = LhScheduleTimeUtil.formatDate(plan.getPlanDate());
            this.addCount(raw.get(dateKey), plan.getClassIndex(), 1);
            List<String> key = Arrays.asList(dateKey, plan.getClassIndex(), this.normalizeAction(plan),
                    plan.getAfterMaterialCode(), LhSingleControlMachineUtil.resolvePhysicalMachineCode(plan.getLhMachineCode()));
            int[] sides = groups.computeIfAbsent(key, group -> new int[SIDE_COUNT_SIZE]);
            // 仅按源机台编码归侧，不依赖当前SKU模式或候选配对侧状态。
            sides[this.resolveSideIndex(plan.getLhMachineCode())]++;
        }
        Map<String, int[]> merged = this.emptyDayCounts(startDate);
        groups.forEach((key, sides) -> this.addCount(merged.get(key.get(DATE_INDEX)), key.get(SHIFT_INDEX),
                Math.max(sides[LEFT_INDEX], sides[RIGHT_INDEX]) + sides[WHOLE_INDEX]));
        Map<String, MouldChangeQuotaLimits> rawCounts = new LinkedHashMap<>(COVERED_DAYS);
        Map<String, MouldChangeQuotaLimits> historicalCounts = new LinkedHashMap<>(COVERED_DAYS);
        Map<String, MouldChangeQuotaLimits> effectiveLimits = new LinkedHashMap<>(COVERED_DAYS);
        merged.forEach((dateKey, counts) -> {
            MouldChangeQuotaLimits history = this.toLimits(counts);
            MouldChangeQuotaLimits effective = new MouldChangeQuotaLimits(
                    Math.max(base.getMorningLimit(), history.getMorningLimit()),
                    Math.max(base.getAfternoonLimit(), history.getAfternoonLimit()),
                    Math.max(base.getDailyLimit(), history.getDailyLimit()));
            rawCounts.put(dateKey, this.toLimits(raw.get(dateKey)));
            historicalCounts.put(dateKey, history);
            effectiveLimits.put(dateKey, effective);
            log.info("历史换模限额快照, 工厂={}, 本次批次={}, 原T={}, 来源保存日期={}, 来源批次={}, 计划日期={}, "
                            + "查询条数={}, 有效原始次数={}, 合并后次数={}, 参数={}, 有效上限={}",
                    context.getFactoryCode(), context.getBatchNo(), startDate, sourceDate, sourceBatches, dateKey,
                    plans.size(), rawCounts.get(dateKey), history, base, effective);
        });
        return new MouldChangeQuotaSnapshot(context.getFactoryCode(), context.getBatchNo(), startDate,
                sourceDate, base, rawCounts, historicalCounts, effectiveLimits);
    }

    /**
     * 检查可用于上限统计的历史字段及业务范围，不从时间或当前SKU模式猜测班次。
     * @param context 当前上下文，仅用于工厂核对和日志
     * @param plan 历史源记录
     * @param sourceDate 历史保存日
     * @param startDate 原T日
     * @return 是否计入历史统计
     */
    private boolean isCountable(LhScheduleContext context, LhMouldChangePlan plan,
            LocalDate sourceDate, LocalDate startDate) {
        if (Objects.isNull(plan) || Objects.isNull(plan.getId()) || Objects.isNull(plan.getScheduleDate())
                || Objects.isNull(plan.getPlanDate()) || StringUtils.isEmpty(plan.getFactoryCode())
                || StringUtils.isEmpty(plan.getClassIndex()) || StringUtils.isEmpty(plan.getChangeMouldType())
                || StringUtils.isEmpty(StringUtils.trim(plan.getAfterMaterialCode()))
                || StringUtils.isEmpty(LhSingleControlMachineUtil.resolvePhysicalMachineCode(plan.getLhMachineCode()))) {
            this.logExcluded(context, plan, "历史关键字段缺失");
            return false;
        }
        if (!StringUtils.equals(context.getFactoryCode(), plan.getFactoryCode())
                || !StringUtils.equals(sourceDate.toString(), LhScheduleTimeUtil.formatDate(plan.getScheduleDate()))) {
            this.logExcluded(context, plan, "工厂或来源保存日期不匹配");
            return false;
        }
        String dateKey = LhScheduleTimeUtil.formatDate(plan.getPlanDate());
        if (!StringUtils.equals(dateKey, startDate.toString())
                && !StringUtils.equals(dateKey, startDate.plusDays(1).toString())) {
            return false;
        }
        if (!StringUtils.equals(ShiftEnum.MORNING_SHIFT.getCode(), plan.getClassIndex())
                && !StringUtils.equals(ShiftEnum.AFTERNOON_SHIFT.getCode(), plan.getClassIndex())) {
            this.logExcluded(context, plan, "非早中班有效班次编码");
            return false;
        }
        return !StringUtils.startsWith(plan.getRemark(), OFF_MACHINE_ONLY_PREFIX)
                && MouldChangeTypeEnum.containsAnyCode(plan.getChangeMouldType(),
                        MouldChangeTypeEnum.REGULAR.getCode(), MouldChangeTypeEnum.TYPE_BLOCK.getCode());
    }

    /** @param machineCode 历史机台编码 @return 左侧、右侧或未拆侧统计槽位 */
    private int resolveSideIndex(String machineCode) {
        if (!LhSingleControlMachineUtil.isSingleMouldMachine(machineCode)) {
            return WHOLE_INDEX;
        }
        return LhSingleControlMachineUtil.isLeftSide(machineCode) ? LEFT_INDEX : RIGHT_INDEX;
    }

    /** @param plan 历史计划 @return 去除清洗附属类型、按固定顺序归一化的换模动作组合 */
    private String normalizeAction(LhMouldChangePlan plan) {
        return Arrays.stream(new MouldChangeTypeEnum[]{MouldChangeTypeEnum.REGULAR, MouldChangeTypeEnum.TYPE_BLOCK})
                .filter(type -> MouldChangeTypeEnum.containsCode(plan.getChangeMouldType(), type.getCode()))
                .map(MouldChangeTypeEnum::getCode).collect(java.util.stream.Collectors.joining(","));
    }

    /** @param startDate 原T @return 两个覆盖日期的独立早中班计数容器 */
    private Map<String, int[]> emptyDayCounts(LocalDate startDate) {
        Map<String, int[]> counts = new LinkedHashMap<>(COVERED_DAYS);
        for (int day = 0; day < COVERED_DAYS; day++) {
            counts.put(startDate.plusDays(day).toString(), new int[SHIFT_COUNT_SIZE]);
        }
        return counts;
    }

    /** @param counts 早中班统计 @param shiftCode 历史班次编码 @param count 本组次数 */
    private void addCount(int[] counts, String shiftCode, int count) {
        counts[StringUtils.equals(shiftCode, ShiftEnum.MORNING_SHIFT.getCode()) ? MORNING_COUNT_INDEX : AFTERNOON_COUNT_INDEX] += count;
    }

    /** @param counts 早中班原始或合并次数 @return 只读三个维度的次数 */
    private MouldChangeQuotaLimits toLimits(int[] counts) {
        return new MouldChangeQuotaLimits(counts[MORNING_COUNT_INDEX], counts[AFTERNOON_COUNT_INDEX],
                counts[MORNING_COUNT_INDEX] + counts[AFTERNOON_COUNT_INDEX]);
    }

    /** @param context 本次上下文 @param plan 异常源记录 @param reason 排除原因 */
    private void logExcluded(LhScheduleContext context, LhMouldChangePlan plan, String reason) {
        log.warn("历史换模限额记录排除, 工厂={}, 本次批次={}, 源ID={}, 来源批次={}, 保存日期={}, 计划日期={}, "
                        + "班次={}, 机台={}, 后物料={}, 原因={}", context.getFactoryCode(), context.getBatchNo(),
                Objects.isNull(plan) ? null : plan.getId(), Objects.isNull(plan) ? null : plan.getLhResultBatchNo(),
                Objects.isNull(plan) ? null : plan.getScheduleDate(), Objects.isNull(plan) ? null : plan.getPlanDate(),
                Objects.isNull(plan) ? null : plan.getClassIndex(), Objects.isNull(plan) ? null : plan.getLhMachineCode(),
                Objects.isNull(plan) ? null : plan.getAfterMaterialCode(), reason);
    }
}
