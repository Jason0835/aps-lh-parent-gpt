package com.zlt.aps.cd90.engine.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.engine.algorithm.Cd90NewSpecAdvanceResolver;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineScheduleResultMapper;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleInput;
import com.zlt.aps.cd90.engine.model.Cd90DemandShift;
import com.zlt.aps.cd90.engine.model.Cd90NewSpecAdvanceInfo;
import com.zlt.aps.cd90.engine.model.Cd90NewSpecAdvanceResult;
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
public class Cd90NewSpecAdvanceInputPreparer {

    private final Cd90EngineScheduleResultMapper scheduleResultMapper;
    private final Cd90NewSpecAdvanceResolver resolver;

    /**
     * 查询历史排程计划量并生成首班新增规格快照。
     *
     * @param context 自动排程上下文
     * @param input 首班原始输入
     * @return 新增规格证据和去重计划需求
     */
    public Cd90NewSpecAdvanceResult prepare(Cd90AutoScheduleContext context,
                                            Cd90AutoScheduleInput input) {
        this.validate(context, input);
        int lookbackDays = context.getParameters().getNewSpecLookbackDays();
        int advanceDays = context.getParameters().getNewSpecAdvanceDays();
        LocalDate historyStartDate = context.getScheduleDate().minusDays(lookbackDays);
        LocalDate historyEndDate = context.getScheduleDate().minusDays(1);
        Set<String> clothCodes = this.positiveDemandClothCodes(input.getDemandShifts());
        List<Cd90ScheduleResult> historyResults = clothCodes.isEmpty()
                ? Collections.emptyList()
                : Optional.ofNullable(this.scheduleResultMapper.selectList(
                        Wrappers.<Cd90ScheduleResult>lambdaQuery()
                                .eq(Cd90ScheduleResult::getFactoryCode,
                                        context.getFactoryCode())
                                .between(Cd90ScheduleResult::getScheduleDate,
                                        Date.valueOf(historyStartDate),
                                        Date.valueOf(historyEndDate))
                                .in(Cd90ScheduleResult::getClothCode, clothCodes)))
                        .orElse(Collections.emptyList());
        Set<String> scheduledClothCodes = historyResults.stream()
                .filter(this::hasAnyScheduledQuantity)
                .map(Cd90ScheduleResult::getClothCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        Cd90NewSpecAdvanceResult result = this.resolver.resolve(
                context.getScheduleDate(), lookbackDays, advanceDays,
                input.getDemandShifts(), scheduledClothCodes);
        log.info("[直裁自动排程] 新增规格历史排程判断完成, factoryCode={}, scheduleDate={}, "
                        + "historyRange={}~{}, candidateClothCount={}, scheduledClothCount={}, "
                        + "newSpecClothCount={}",
                context.getFactoryCode(), context.getScheduleDate(),
                historyStartDate, historyEndDate, clothCodes.size(),
                scheduledClothCodes.size(), result.getAdvanceInfoByCloth().size());
        return result;
    }

    /**
     * 将首班快照套用到后续班次重新加载的输入。
     *
     * @param input 后续班次原始输入
     * @param infoByCloth 首班锁定的新增规格证据
     */
    public void applySnapshot(Cd90AutoScheduleInput input,
                              Map<String, Cd90NewSpecAdvanceInfo> infoByCloth) {
        if (input == null) {
            throw new IllegalArgumentException("新增规格快照应用输入不能为空");
        }
        input.setPlanningDemandShifts(this.resolver.applySnapshot(
                input.getDemandShifts(), infoByCloth));
        input.setNewSpecAdvanceInfoByCloth(infoByCloth == null
                ? Collections.emptyMap() : infoByCloth);
    }

    /** 收集存在正需求的帘布代号。 */
    private Set<String> positiveDemandClothCodes(List<Cd90DemandShift> demandShifts) {
        return this.safe(demandShifts).stream()
                .filter(item -> item != null && StringUtils.hasText(item.getClothCode()))
                .filter(item -> item.isIncluded()
                        && this.value(item.getClothDemandQuantity()).signum() > 0)
                .map(Cd90DemandShift::getClothCode)
                .collect(Collectors.toSet());
    }

    /** 任一班次计划量大于0即表示历史窗口内已经排过。 */
    private boolean hasAnyScheduledQuantity(Cd90ScheduleResult result) {
        return result != null && IntStream.rangeClosed(1, 8)
                .mapToObj(index -> result.getFieldValueByFieldName(
                        String.format("class%dPlanQty", index)))
                .map(BigDecimalUtils::valueOf)
                .anyMatch(quantity -> quantity.signum() > 0);
    }

    /** 校验首班快照准备所需上下文。 */
    private void validate(Cd90AutoScheduleContext context,
                          Cd90AutoScheduleInput input) {
        if (context == null || context.getScheduleDate() == null
                || context.getParameters() == null
                || !StringUtils.hasText(context.getFactoryCode())
                || input == null) {
            throw new IllegalArgumentException("新增规格历史判断上下文和输入不能为空");
        }
    }

    /** 空列表保护。 */
    private List<Cd90DemandShift> safe(List<Cd90DemandShift> values) {
        return values == null ? Collections.emptyList() : values;
    }

    /** 空数值按0处理。 */
    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}