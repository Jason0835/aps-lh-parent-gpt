package com.zlt.aps.cd15.engine.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.engine.algorithm.Cd15NewSpecAdvanceResolver;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineScheduleResultMapper;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleInput;
import com.zlt.aps.cd15.engine.model.Cd15DemandShift;
import com.zlt.aps.cd15.engine.model.Cd15NewSpecAdvanceInfo;
import com.zlt.aps.cd15.engine.model.Cd15NewSpecAdvanceResult;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 新增规格提前生产输入准备器。
 *
 * <p>首班查询历史排程计划量并生成规则快照，后续班次仅套用首班快照，
 * 防止排程执行过程中历史数据变化导致同批次口径不一致。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Cd15NewSpecAdvanceInputPreparer {

    private final Cd15EngineScheduleResultMapper scheduleResultMapper;
    private final Cd15NewSpecAdvanceResolver resolver;

    /**
     * 查询历史排程计划量并生成首班新增规格快照。
     *
     * @param context 自动排程上下文
     * @param input 首班原始输入
     * @return 新增规格证据和去重计划需求
     */
    public Cd15NewSpecAdvanceResult prepare(Cd15AutoScheduleContext context,
                                            Cd15AutoScheduleInput input) {
        this.validate(context, input);
        int lookbackDays = context.getParameters().getNewSpecLookbackDays();
        if (lookbackDays <= 0) {
            log.info("[斜裁自动排程] 新增规格历史回看天数={}, 不启用新增规格提前生产", lookbackDays);
            return Cd15NewSpecAdvanceResult.builder()
                    .adjustedDemandShifts(input.getDemandShifts())
                    .advanceInfoBySteelStrip(Collections.emptyMap())
                    .build();
        }
        int advanceDays = context.getParameters().getNewSpecAdvanceDays();
        LocalDate historyStartDate = context.getScheduleDate().minusDays(lookbackDays);
        LocalDate historyEndDate = context.getScheduleDate().minusDays(1);
        Set<String> steelStripCodes = this.positiveDemandSteelStripCodes(input.getDemandShifts());
        List<Cd15ScheduleResult> historyResults = steelStripCodes.isEmpty()
                ? Collections.emptyList()
                : Optional.ofNullable(this.scheduleResultMapper.selectList(
                        Wrappers.<Cd15ScheduleResult>lambdaQuery()
                                .eq(Cd15ScheduleResult::getFactoryCode,
                                        context.getFactoryCode())
                                .between(Cd15ScheduleResult::getScheduleDate,
                                        Date.valueOf(historyStartDate),
                                        Date.valueOf(historyEndDate))
                                .in(Cd15ScheduleResult::getSteelStripCode, steelStripCodes)))
                        .orElse(Collections.emptyList());
        Set<String> scheduledSteelStripCodes = historyResults.stream()
                .filter(this::hasAnyScheduledQuantity)
                .map(Cd15ScheduleResult::getSteelStripCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        Cd15NewSpecAdvanceResult result = this.resolver.resolve(
                context.getScheduleDate(), lookbackDays, advanceDays,
                input.getDemandShifts(), scheduledSteelStripCodes);
        log.info("[斜裁自动排程] 新增规格历史排程判断完成, factoryCode={}, scheduleDate={}, "
                        + "historyRange={}~{}, candidateSteelStripCount={}, scheduledSteelStripCount={}, "
                        + "newSpecSteelStripCount={}",
                context.getFactoryCode(), context.getScheduleDate(),
                historyStartDate, historyEndDate, steelStripCodes.size(),
                scheduledSteelStripCodes.size(), result.getAdvanceInfoBySteelStrip().size());
        return result;
    }

    /**
     * 将首班快照套用到后续班次重新加载的输入。
     *
     * @param input 后续班次原始输入
     * @param infoBySteelStrip 首班锁定的新增规格证据
     */
    public void applySnapshot(Cd15AutoScheduleInput input,
                              Map<String, Cd15NewSpecAdvanceInfo> infoBySteelStrip) {
        if (input == null) {
            throw new IllegalArgumentException("新增规格快照应用输入不能为空");
        }
        input.setPlanningDemandShifts(this.resolver.applySnapshot(
                input.getDemandShifts(), infoBySteelStrip));
        input.setNewSpecAdvanceInfoBySteelStrip(infoBySteelStrip == null
                ? Collections.emptyMap() : infoBySteelStrip);
    }

    /** 收集存在正需求的钢带代号。 */
    private Set<String> positiveDemandSteelStripCodes(List<Cd15DemandShift> demandShifts) {
        return this.safe(demandShifts).stream()
                .filter(item -> item != null && StringUtils.hasText(item.getSteelStripCode()))
                .filter(item -> item.isIncluded()
                        && this.value(item.getSteelStripDemandQuantity()).signum() > 0)
                .map(Cd15DemandShift::getSteelStripCode)
                .collect(Collectors.toSet());
    }

    /** 任一班次计划量大于0即表示历史窗口内已经排过。 */
    private boolean hasAnyScheduledQuantity(Cd15ScheduleResult result) {
        return result != null && IntStream.rangeClosed(1, 8)
                .mapToObj(index -> result.getFieldValueByFieldName(
                        String.format("class%dPlanQty", index)))
                .map(BigDecimalUtils::valueOf)
                .anyMatch(quantity -> quantity.signum() > 0);
    }

    /** 校验首班快照准备所需上下文。 */
    private void validate(Cd15AutoScheduleContext context,
                          Cd15AutoScheduleInput input) {
        if (context == null || context.getScheduleDate() == null
                || context.getParameters() == null
                || !StringUtils.hasText(context.getFactoryCode())
                || input == null) {
            throw new IllegalArgumentException("新增规格历史判断上下文和输入不能为空");
        }
    }

    /** 空列表保护。 */
    private List<Cd15DemandShift> safe(List<Cd15DemandShift> values) {
        return values == null ? Collections.emptyList() : values;
    }

    /** 空数值按0处理。 */
    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}