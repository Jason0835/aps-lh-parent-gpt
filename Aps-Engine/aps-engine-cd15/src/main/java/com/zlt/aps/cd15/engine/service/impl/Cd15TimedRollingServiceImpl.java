package com.zlt.aps.cd15.engine.service.impl;

import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleInput;
import com.zlt.aps.cd15.engine.model.Cd15LaneAllocationDraft;
import com.zlt.aps.cd15.engine.model.Cd15MultiShiftScheduleResult;
import com.zlt.aps.cd15.engine.model.Cd15RollingPrefixResourceUsage;
import com.zlt.aps.cd15.engine.model.Cd15RollingTarget;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleResultDraft;
import com.zlt.aps.cd15.engine.model.Cd15SingleShiftScheduleResult;
import com.zlt.aps.cd15.engine.model.Cd15TimedRollingOutput;
import com.zlt.aps.cd15.engine.service.Cd15AutoScheduleInputService;
import com.zlt.aps.cd15.engine.service.Cd15MultiShiftScheduleExecutor;
import com.zlt.aps.cd15.engine.service.Cd15ScheduleProgressListener;
import com.zlt.aps.cd15.engine.service.Cd15TimedRollingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** CD15目标班次后缀滚动Engine实现。 */
@Service
@RequiredArgsConstructor
public class Cd15TimedRollingServiceImpl implements Cd15TimedRollingService {

    private final Cd15AutoScheduleInputService inputService;
    private final Cd15MultiShiftScheduleExecutor multiShiftScheduleExecutor;

    @Override
    public Cd15TimedRollingOutput execute(Cd15RollingTarget target, String inputVersion, int agingPeriodHours) {
        return this.execute(target, inputVersion, agingPeriodHours, Collections.emptyList(),
                Cd15ScheduleProgressListener.NO_OP);
    }

    @Override
    public Cd15TimedRollingOutput execute(Cd15RollingTarget target, String inputVersion, int agingPeriodHours,
                                          List<Cd15RollingPrefixResourceUsage> prefixResourceUsages) {
        return this.execute(target, inputVersion, agingPeriodHours, prefixResourceUsages,
                Cd15ScheduleProgressListener.NO_OP);
    }

    @Override
    public Cd15TimedRollingOutput execute(Cd15RollingTarget target, String inputVersion, int agingPeriodHours,
                                          List<Cd15RollingPrefixResourceUsage> prefixResourceUsages,
                                          Cd15ScheduleProgressListener listener) {
        Cd15AutoScheduleInput input = inputService.load(target.getFactoryCode(), target.getScheduleDate(),
                target.getTargetClassField(), target.getTargetShiftCode(), agingPeriodHours);
        input.setPrefixResourceUsages(prefixResourceUsages == null ? Collections.emptyList() : prefixResourceUsages);
        Cd15MultiShiftScheduleResult trialOutput = multiShiftScheduleExecutor.execute(input, target.getTargetClassIndex(),
                listener);
        List<Cd15ScheduleResultDraft> replacementResults = this.filterSuffix(
                trialOutput == null ? null : trialOutput.getScheduledDrafts(), target.getTargetClassIndex());
        List<Cd15SingleShiftScheduleResult> unscheduledResults = this.filterSuffixUnscheduled(
                trialOutput == null ? null : trialOutput.getUnscheduledResults(), target.getTargetClassIndex());
        List<Cd15LaneAllocationDraft> replacementLaneAllocations = replacementResults.stream()
                .map(Cd15ScheduleResultDraft::getLaneAllocations)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        return Cd15TimedRollingOutput.builder()
                .inputVersion(inputVersion)
                .replacementResults(replacementResults)
                .replacementLaneAllocations(replacementLaneAllocations)
                .unscheduledResults(unscheduledResults)
                .build();
    }
    private List<Cd15ScheduleResultDraft> filterSuffix(List<Cd15ScheduleResultDraft> drafts, int targetClassIndex) {
        return drafts == null ? Collections.emptyList() : drafts.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getClassIndex() >= targetClassIndex)
                .collect(Collectors.toList());
    }

    private List<Cd15SingleShiftScheduleResult> filterSuffixUnscheduled(
            List<Cd15SingleShiftScheduleResult> results, int targetClassIndex) {
        return results == null ? Collections.emptyList() : results.stream()
                .filter(Objects::nonNull)
                .filter(item -> this.classIndex(item.getClassField()) >= targetClassIndex)
                .collect(Collectors.toList());
    }

    private int classIndex(String classField) {
        if (classField == null) {
            return 0;
        }
        String normalized = classField.trim().toUpperCase().replace("CLASS", "");
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}