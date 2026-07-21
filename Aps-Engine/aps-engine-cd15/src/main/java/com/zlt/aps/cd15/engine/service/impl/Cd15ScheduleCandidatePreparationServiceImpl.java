package com.zlt.aps.cd15.engine.service.impl;

import com.zlt.aps.cd15.engine.algorithm.Cd15ScheduleCandidateBuilder;
import com.zlt.aps.cd15.engine.algorithm.Cd15ScheduleCandidateSorter;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleInput;
import com.zlt.aps.cd15.engine.model.Cd15DemandShift;
import com.zlt.aps.cd15.engine.model.Cd15NewSpecAdvanceInfo;
import com.zlt.aps.cd15.engine.model.Cd15RollingScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleCandidate;
import com.zlt.aps.cd15.engine.service.Cd15ScheduleCandidatePreparationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 当前斜裁班次候选规格内存准备服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd15ScheduleCandidatePreparationServiceImpl
        implements Cd15ScheduleCandidatePreparationService {

    private static final Pattern CLASS_FIELD_PATTERN = Pattern.compile("CLASS([1-8])");
    private static final LocalTime FIRST_FORMING_DEMAND_TIME = LocalTime.of(22, 0);

    private final Cd15ScheduleCandidateBuilder candidateBuilder;
    private final Cd15ScheduleCandidateSorter candidateSorter;

    /**
     * 解析当前斜裁班次对应的首个成型供应班次，并构建候选规格。
     *
     * @param context 自动排程上下文
     * @param input 自动排程输入快照
     * @param classField 当前斜裁班次字段
     * @param rolling 多班滚动排程共享的内存上下文，提供续作和新增规格提前需求
     * @return 已排序候选规格
     */
    @Override
    public List<Cd15ScheduleCandidate> prepare(Cd15AutoScheduleContext context,
                                               Cd15AutoScheduleInput input,
                                               String classField,
                                               Cd15RollingScheduleContext rolling) {
        if (context == null || context.getScheduleDate() == null || context.getParameters() == null) {
            throw new IllegalArgumentException("自动排程上下文及参数不能为空");
        }
        if (input == null) {
            throw new IllegalArgumentException("自动排程输入快照不能为空");
        }
        Matcher matcher = CLASS_FIELD_PATTERN.matcher(classField == null ? "" : classField);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("斜裁班次字段只能取CLASS1至CLASS8");
        }

        int classIndex = Integer.parseInt(matcher.group(1));
        LocalDateTime currentDemandStart = context.getScheduleDate().minusDays(1)
                .atTime(FIRST_FORMING_DEMAND_TIME)
                .plusHours((classIndex - 1L) * 8L);
        Map<String, BigDecimal> continueDemandBySteelStrip = rolling == null
                ? Collections.emptyMap()
                : rolling.getContinueDemandBySteelStrip();
        List<Cd15ScheduleCandidate> candidates = this.candidateBuilder.build(
                this.planningDemands(input), input.getStocksAtSix(), currentDemandStart,
                input.getDepthClassQtyBySteelStrip(), continueDemandBySteelStrip);
        candidates = this.appendNewSpecAdvanceCandidates(candidates, input, rolling);

        log.info("[斜裁自动排程] 当前班次候选准备完成, factoryCode={}, scheduleDate={}, "
                        + "classField={}, demandStart={}, candidateCount={}",
                context.getFactoryCode(), context.getScheduleDate(), classField,
                currentDemandStart, candidates.size());
        return candidates;
    }

    /** 将尚未首次成功提交的新增规格提前需求补入候选集合。 */
    private List<Cd15ScheduleCandidate> appendNewSpecAdvanceCandidates(
            List<Cd15ScheduleCandidate> candidates,
            Cd15AutoScheduleInput input,
            Cd15RollingScheduleContext rolling) {
        Map<String, Cd15ScheduleCandidate> candidateByMaterial = candidates.stream()
                .collect(Collectors.toMap(
                        item -> StringUtils.hasText(item.getMaterialKey())
                                ? item.getMaterialKey() : item.getSteelStripCode(),
                        item -> item, (first, second) -> first, LinkedHashMap::new));
        Map<String, BigDecimal> remainingBySteelStrip = rolling == null
                || rolling.getNewSpecAdvanceRemainingBySteelStrip() == null
                ? Collections.emptyMap() : rolling.getNewSpecAdvanceRemainingBySteelStrip();
        Map<String, Cd15NewSpecAdvanceInfo> infoBySteelStrip = rolling == null
                || rolling.getNewSpecAdvanceInfoBySteelStrip() == null
                ? Collections.emptyMap() : rolling.getNewSpecAdvanceInfoBySteelStrip();
        remainingBySteelStrip.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().signum() > 0)
                .forEach(entry -> {
                    Cd15ScheduleCandidate candidate = candidateByMaterial.values().stream()
                            .filter(item -> entry.getKey().equals(item.getSteelStripCode()))
                            .findFirst()
                            .orElseGet(() -> {
                                Cd15ScheduleCandidate created = this.safe(input.getDemandShifts()).stream()
                                        .filter(item -> item != null
                                                && entry.getKey().equals(item.getSteelStripCode()))
                                        .sorted(java.util.Comparator
                                                .comparing(Cd15DemandShift::getCuttingAngle,
                                                        java.util.Comparator.nullsLast(String::compareTo))
                                                .thenComparing(Cd15DemandShift::getMaterialKey,
                                                        java.util.Comparator.nullsLast(String::compareTo)))
                                        .map(this::candidateFromDemand)
                                        .findFirst()
                                        .orElseGet(() -> Cd15ScheduleCandidate.builder()
                                                .steelStripCode(entry.getKey()).build());
                                String key = StringUtils.hasText(created.getMaterialKey())
                                        ? created.getMaterialKey() : created.getSteelStripCode();
                                candidateByMaterial.put(key, created);
                                return created;
                            });
                    Cd15NewSpecAdvanceInfo info = infoBySteelStrip.get(entry.getKey());
                    candidate.setNewSpecAdvance(true);
                    candidate.setNewSpecAdvanceQuantityNormalized(rolling != null
                            && rolling.getNormalizedNewSpecAdvanceSteelStripCodes() != null
                            && rolling.getNormalizedNewSpecAdvanceSteelStripCodes()
                                    .contains(entry.getKey()));
                    candidate.setNewSpecAdvanceAnalysis(info == null ? null : info.getAnalysis());
                });
        return this.candidateSorter.sort(new ArrayList<>(candidateByMaterial.values()));
    }

    private Cd15ScheduleCandidate candidateFromDemand(Cd15DemandShift demand) {
        return Cd15ScheduleCandidate.builder()
                .materialKey(demand.getMaterialKey())
                .steelStripCode(demand.getSteelStripCode())
                .bigRollCode(demand.getBigRollCode())
                .cuttingAngle(demand.getCuttingAngle())
                .craftWidth(demand.getCraftWidth())
                .unitConsumeMillimeter(demand.getUnitConsumeMillimeter())
                .cordWidth(demand.getCordWidth())
                .curlLength(demand.getCurlLength())
                .build();
    }

    /** 优先使用去重计划需求；未建立新增规格快照时兼容原始输入。 */
    private List<Cd15DemandShift> planningDemands(Cd15AutoScheduleInput input) {
        return input.getPlanningDemandShifts() == null
                ? this.safe(input.getDemandShifts())
                : this.safe(input.getPlanningDemandShifts());
    }

    /** 空列表保护。 */
    private List<Cd15DemandShift> safe(List<Cd15DemandShift> values) {
        return values == null ? Collections.emptyList() : values;
    }
}
