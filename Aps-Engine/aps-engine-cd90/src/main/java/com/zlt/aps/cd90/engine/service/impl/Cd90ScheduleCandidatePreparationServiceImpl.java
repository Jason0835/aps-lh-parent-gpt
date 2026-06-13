package com.zlt.aps.cd90.engine.service.impl;

import com.zlt.aps.cd90.engine.algorithm.Cd90ScheduleCandidateBuilder;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleInput;
import com.zlt.aps.cd90.engine.model.Cd90ScheduleCandidate;
import com.zlt.aps.cd90.engine.service.Cd90ScheduleCandidatePreparationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /**
     * 解析当前直裁班次对应的首个成型供应班次，并构建候选规格。
     *
     * @param context 自动排程上下文
     * @param input 自动排程输入快照
     * @param classField 当前直裁班次字段
     * @return 已排序候选规格
     */
    @Override
    public List<Cd90ScheduleCandidate> prepare(Cd90AutoScheduleContext context,
                                               Cd90AutoScheduleInput input,
                                               String classField) {
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
        List<Cd90ScheduleCandidate> candidates = candidateBuilder.build(
                input.getDemandShifts(), input.getStocksAtSix(), currentDemandStart,
                context.getParameters().getDemandWindow());

        log.info("[直裁自动排程] 当前班次候选准备完成, factoryCode={}, scheduleDate={}, "
                        + "classField={}, demandStart={}, candidateCount={}",
                context.getFactoryCode(), context.getScheduleDate(), classField,
                currentDemandStart, candidates.size());
        return candidates;
    }
}
