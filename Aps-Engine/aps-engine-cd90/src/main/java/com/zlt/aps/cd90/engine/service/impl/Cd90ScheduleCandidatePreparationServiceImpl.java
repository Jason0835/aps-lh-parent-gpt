package com.zlt.aps.cd90.engine.service.impl;

import com.zlt.aps.cd90.engine.algorithm.Cd90ScheduleCandidateBuilder;
import com.zlt.aps.cd90.engine.algorithm.Cd90ScheduleCandidateSorter;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleInput;
import com.zlt.aps.cd90.engine.model.Cd90DemandShift;
import com.zlt.aps.cd90.engine.model.Cd90NewSpecAdvanceInfo;
import com.zlt.aps.cd90.engine.model.Cd90RollingScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90ScheduleCandidate;
import com.zlt.aps.cd90.engine.service.Cd90ScheduleCandidatePreparationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 当前直裁班次候选规格内存准备服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd90ScheduleCandidatePreparationServiceImpl
        implements Cd90ScheduleCandidatePreparationService {

    private static final Pattern CLASS_FIELD_PATTERN = Pattern.compile("CLASS([1-8])");
    private static final LocalTime FIRST_FORMING_DEMAND_TIME = LocalTime.of(22, 0);

    private final Cd90ScheduleCandidateBuilder candidateBuilder;
    private final Cd90ScheduleCandidateSorter candidateSorter;

    /**
     * 解析当前直裁班次对应的首个成型供应班次，并构建候选规格。
     *
     * @param context 自动排程上下文
     * @param input 自动排程输入快照
     * @param classField 当前直裁班次字段
     * @param rolling 多班滚动排程共享的内存上下文，提供续作和新增规格提前需求
     * @return 已排序候选规格
     */
    @Override
    public List<Cd90ScheduleCandidate> prepare(Cd90AutoScheduleContext context,
                                               Cd90AutoScheduleInput input,
                                               String classField,
                                               Cd90RollingScheduleContext rolling) {
        if (context == null || context.getScheduleDate() == null || context.getParameters() == null) {
            throw new IllegalArgumentException("自动排程上下文及参数不能为空");
        }
        if (input == null) {
            throw new IllegalArgumentException("自动排程输入快照不能为空");
        }
        Matcher matcher = CLASS_FIELD_PATTERN.matcher(classField == null ? "" : classField);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("直裁班次字段只能取CLASS1至CLASS8");
        }

        int classIndex = Integer.parseInt(matcher.group(1));
        LocalDateTime currentDemandStart = context.getScheduleDate().minusDays(1)
                .atTime(FIRST_FORMING_DEMAND_TIME)
                .plusHours((classIndex - 1L) * 8L);
        Map<String, BigDecimal> continueDemandByCloth = rolling == null
                ? Collections.emptyMap()
                : rolling.getContinueDemandByCloth();
        List<Cd90ScheduleCandidate> candidates = this.candidateBuilder.build(
                this.planningDemands(input), input.getStocksAtSix(), currentDemandStart,
                input.getDepthClassQtyByCloth(), continueDemandByCloth);
        candidates = this.appendNewSpecAdvanceCandidates(candidates, rolling);

        log.info("[直裁自动排程] 当前班次候选准备完成, factoryCode={}, scheduleDate={}, "
                        + "classField={}, demandStart={}, candidateCount={}",
                context.getFactoryCode(), context.getScheduleDate(), classField,
                currentDemandStart, candidates.size());
        return candidates;
    }

    /** 将尚未首次成功提交的新增规格提前需求补入候选集合。 */
    private List<Cd90ScheduleCandidate> appendNewSpecAdvanceCandidates(
            List<Cd90ScheduleCandidate> candidates,
            Cd90RollingScheduleContext rolling) {
        Map<String, Cd90ScheduleCandidate> candidateByCloth = candidates.stream()
                .collect(Collectors.toMap(Cd90ScheduleCandidate::getClothCode,
                        Function.identity(), (first, second) -> first, LinkedHashMap::new));
        Map<String, BigDecimal> remainingByCloth = rolling == null
                || rolling.getNewSpecAdvanceRemainingByCloth() == null
                ? Collections.emptyMap() : rolling.getNewSpecAdvanceRemainingByCloth();
        Map<String, Cd90NewSpecAdvanceInfo> infoByCloth = rolling == null
                || rolling.getNewSpecAdvanceInfoByCloth() == null
                ? Collections.emptyMap() : rolling.getNewSpecAdvanceInfoByCloth();
        remainingByCloth.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().signum() > 0)
                .forEach(entry -> {
                    Cd90ScheduleCandidate candidate = candidateByCloth.computeIfAbsent(
                            entry.getKey(), clothCode -> Cd90ScheduleCandidate.builder()
                                    .clothCode(clothCode).build());
                    Cd90NewSpecAdvanceInfo info = infoByCloth.get(entry.getKey());
                    candidate.setNewSpecAdvance(true);
                    candidate.setNewSpecAdvanceQuantityNormalized(rolling != null
                            && rolling.getNormalizedNewSpecAdvanceClothCodes() != null
                            && rolling.getNormalizedNewSpecAdvanceClothCodes()
                                    .contains(entry.getKey()));
                    candidate.setNewSpecAdvanceAnalysis(info == null ? null : info.getAnalysis());
                });
        return this.candidateSorter.sort(new ArrayList<>(candidateByCloth.values()));
    }

    /** 优先使用去重计划需求；未建立新增规格快照时兼容原始输入。 */
    private List<Cd90DemandShift> planningDemands(Cd90AutoScheduleInput input) {
        return input.getPlanningDemandShifts() == null
                ? this.safe(input.getDemandShifts())
                : this.safe(input.getPlanningDemandShifts());
    }

    /** 空列表保护。 */
    private List<Cd90DemandShift> safe(List<Cd90DemandShift> values) {
        return values == null ? Collections.emptyList() : values;
    }
}
