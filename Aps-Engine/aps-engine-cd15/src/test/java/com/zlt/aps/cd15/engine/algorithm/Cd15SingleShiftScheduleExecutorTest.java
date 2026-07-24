package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleInput;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleParameters;
import com.zlt.aps.cd15.engine.model.Cd15ConstructionMaterial;
import com.zlt.aps.cd15.engine.model.Cd15LossRateRule;
import com.zlt.aps.cd15.engine.model.Cd15MachineTailState;
import com.zlt.aps.cd15.engine.model.Cd15MachineResource;
import com.zlt.aps.cd15.engine.model.Cd15MachineResourceSnapshot;
import com.zlt.aps.cd15.engine.model.Cd15MachineRollBinding;
import com.zlt.aps.cd15.engine.model.Cd15RollingScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleCandidate;
import com.zlt.aps.cd15.engine.model.Cd15ShiftDemandDecision;
import com.zlt.aps.cd15.engine.model.Cd15ShiftDescriptor;
import com.zlt.aps.cd15.engine.model.Cd15ShiftExecutionResult;
import com.zlt.aps.cd15.engine.model.Cd15ShiftResourceState;
import com.zlt.aps.cd15.engine.model.Cd15StorageLaneState;
import com.zlt.aps.cd15.engine.service.Cd15MachineResourceService;
import com.zlt.aps.cd15.engine.service.Cd15ScheduleCandidatePreparationService;
import com.zlt.aps.cd15.engine.service.Cd15ShiftDemandProvider;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** 单班执行器规格失败隔离测试。 */
public class Cd15SingleShiftScheduleExecutorTest {

    /** 两个班初机尾续作共享27个空闲工装时，首个候选最多按13个工装试算。 */
    @Test
    public void shouldFairShareAvailableToolingBetweenShiftStartTails() throws Exception {
        Cd15SingleShiftScheduleExecutor executor = new Cd15SingleShiftScheduleExecutor(
                null, null, null, null, null,
                new Cd15CloseOutCalculator(), new Cd15ScheduleCandidateSorter(),
                new Cd15SplitCutGroupBuilder(), new Cd15SpecShiftQuantityLimitResolver());
        Cd15ScheduleCandidate candidate = candidate("C2");
        Cd15ShiftResourceState state = Cd15ShiftResourceState.builder()
                .lanes(Collections.singletonList(Cd15StorageLaneState.builder()
                        .laneCode("L1").steelStripCode("C1")
                        .vehicleCount(673).maxVehicleCount(700).build()))
                .totalToolingCount(700).occupiedToolingCount(673)
                .remainingSecondsByMachine(new HashMap<>())
                .tailSpecByMachine(new HashMap<>())
                .tailByMachine(new HashMap<>())
                .tasks(new java.util.ArrayList<>()).build();
        java.lang.reflect.Method fairShare = Cd15SingleShiftScheduleExecutor.class
                .getDeclaredMethod("applyToolingFairShare", Cd15ScheduleCandidate.class,
                        Cd15ShiftResourceState.class, boolean.class, int.class);
        fairShare.setAccessible(true);
        fairShare.invoke(executor, candidate, state, true, 2);
        java.lang.reflect.Method occupied = Cd15SingleShiftScheduleExecutor.class
                .getDeclaredMethod("trialOccupiedVehicleCount",
                        Cd15ShiftResourceState.class, Cd15ScheduleCandidate.class,
                        int.class);
        occupied.setAccessible(true);
        int trialOccupied = (Integer) occupied.invoke(executor, state, candidate, 700);
        assertEquals(Integer.valueOf(13), candidate.getMaxToolingVehicleCount());
        assertTrue(candidate.isToolingFairShareApplied());
        assertEquals(687, trialOccupied);
    }

    @Test
    public void shouldContinueNextCandidateWhenPreviousConstructionIsMissing() {
        Cd15ScheduleCandidatePreparationService candidates = (context, input, classField, rolling) ->
                Arrays.asList(candidate("C1"), candidate("C2"));
        Cd15ShiftDemandProvider demandProvider = (context, input, shift, candidate, rolling) ->
                Cd15ShiftDemandDecision.builder().netDemandQuantity(new BigDecimal("100"))
                        .planSurplusQuantity(null).build();
        Cd15MachineResourceService machineService = (factoryCode, start, end) -> machineSnapshot();
        Cd15SingleShiftScheduleExecutor executor = new Cd15SingleShiftScheduleExecutor(
                candidates, demandProvider, machineService, trialPreparation(), committer(),
                new Cd15CloseOutCalculator(), new Cd15ScheduleCandidateSorter(), new Cd15SplitCutGroupBuilder(),
                new Cd15SpecShiftQuantityLimitResolver());

        Cd15ShiftExecutionResult result = executor.execute(context(), input(), shift(), state(), null);

        assertEquals(1, result.getTasks().size());
        assertEquals("C2", result.getTasks().get(0).getSteelStripCode());
        assertEquals("CONSTRUCTION_MISSING", result.getFailures().get("C1"));
        assertEquals(2, result.getAttemptTraces().size());
        com.zlt.aps.cd15.engine.model.Cd15ScheduleAttemptTrace missingTrace =
                result.getAttemptTraces().stream()
                        .filter(item -> "C1".equals(item.getSteelStripCode()))
                        .findFirst().orElseThrow(AssertionError::new);
        com.zlt.aps.cd15.engine.model.Cd15ScheduleAttemptTrace scheduledTrace =
                result.getAttemptTraces().stream()
                        .filter(item -> "C2".equals(item.getSteelStripCode()))
                        .findFirst().orElseThrow(AssertionError::new);
        assertEquals(new BigDecimal("100"), missingTrace.getNetDemandQuantity());
        assertEquals("CONSTRUCTION_MISSING", missingTrace.getFailureReason());
        assertEquals(new BigDecimal("160"), scheduledTrace.getScheduledQuantity());
        assertEquals(2, result.getTasks().get(0).getVehicleCount());
    }


    @Test
    public void shouldRejectSpecificationWhenAgingStockTimeIsMissing() {
        Cd15ScheduleCandidatePreparationService candidates = (context, input, classField, rolling) ->
                Collections.singletonList(candidate("C2"));
        Cd15ShiftDemandProvider demandProvider = (context, input, shift, candidate, rolling) ->
                Cd15ShiftDemandDecision.builder().netDemandQuantity(new BigDecimal("100")).build();
        Cd15MachineResourceService machineService = (factoryCode, start, end) -> machineSnapshot();
        Cd15SingleShiftScheduleExecutor executor = new Cd15SingleShiftScheduleExecutor(
                candidates, demandProvider, machineService, trialPreparation(), committer(),
                new Cd15CloseOutCalculator(), new Cd15ScheduleCandidateSorter(), new Cd15SplitCutGroupBuilder(),
                new Cd15SpecShiftQuantityLimitResolver());
        Cd15AutoScheduleInput input = input();
        input.setBigRollAgingDataMissingCodes(Collections.singleton("BR2"));

        Cd15ShiftExecutionResult result = executor.execute(context(), input, shift(), state(), null);

        assertEquals(0, result.getTasks().size());
        assertEquals("DATA_MISSING", result.getFailures().values().iterator().next());
        assertEquals("DATA_MISSING", result.getAttemptTraces().get(0).getFailureReason());
    }

    @Test
    public void shouldRoundNetDemandUpByCraftWidthBeforeTrial() {
        Cd15ScheduleCandidatePreparationService candidates = (context, input, classField, rolling) ->
                Collections.singletonList(candidate("C2"));
        Cd15ShiftDemandProvider demandProvider = (context, input, shift, candidate, rolling) ->
                Cd15ShiftDemandDecision.builder().netDemandQuantity(new BigDecimal("81")).build();
        Cd15MachineResourceService machineService = (factoryCode, start, end) -> machineSnapshot();
        Cd15SingleShiftScheduleExecutor executor = new Cd15SingleShiftScheduleExecutor(
                candidates, demandProvider, machineService, trialPreparation(), committer(),
                new Cd15CloseOutCalculator(), new Cd15ScheduleCandidateSorter(), new Cd15SplitCutGroupBuilder(),
                new Cd15SpecShiftQuantityLimitResolver());

        Cd15ShiftExecutionResult result = executor.execute(context(), input(), shift(), state(), null);

        assertEquals(1, result.getTasks().size());
        assertEquals(new BigDecimal("160"), result.getTasks().get(0).getPlanQuantity());
        assertEquals(new BigDecimal("81.04"),
                result.getAttemptTraces().get(0).getNetDemandQuantity());
    }
    /** 验证机台试算接收按斜裁宽度换算后的计划量，而不是原胎体长度方向米数。 */
    @Test
    public void shouldPassCraftWidthDemandToMachineTrial() {
        Cd15ScheduleCandidatePreparationService candidates = (context, input, classField, rolling) ->
                Collections.singletonList(candidate("C2"));
        Cd15ShiftDemandProvider demandProvider = (context, input, shift, candidate, rolling) ->
                Cd15ShiftDemandDecision.builder().netDemandQuantity(new BigDecimal("1000")).build();
        Cd15SingleShiftScheduleExecutor executor = new Cd15SingleShiftScheduleExecutor(
                candidates, demandProvider, (factoryCode, start, end) -> machineSnapshot(),
                trialPreparation(), committer(), new Cd15CloseOutCalculator(),
                new Cd15ScheduleCandidateSorter(), new Cd15SplitCutGroupBuilder(),
                new Cd15SpecShiftQuantityLimitResolver());
        Cd15AutoScheduleInput input = Cd15AutoScheduleInput.builder()
                .constructionMaterials(Collections.singletonList(Cd15ConstructionMaterial.builder()
                        .steelStripCode("C2").bigRollCode("BR2").cuttingAngle("15")
                        .unitConsumeMillimeter(new BigDecimal("2000"))
                        .craftWidth(new BigDecimal("280"))
                        .curlLength(new BigDecimal("100")).build()))
                .build();
        Cd15ShiftResourceState state = state();
        state.getLanes().get(0).setMaxVehicleCount(100);
        state.setTotalToolingCount(100);
        Cd15AutoScheduleContext context = context();
        context.getParameters().setMinStartQty(BigDecimal.ONE);

        Cd15ShiftExecutionResult result = executor.execute(context, input, shift(), state, null);

        assertEquals(new BigDecimal("140"), result.getAttemptTraces().get(0)
                .getNetDemandQuantity());
        assertEquals(new BigDecimal("200"), result.getTasks().get(0).getPlanQuantity());
    }
    /** 验证均分后的新增规格剩余量已是斜裁米数，后续班次不再按施工长度重复换算。 */
    @Test
    public void shouldNotConvertNormalizedNewSpecAdvanceDemandAgain() {
        Cd15ScheduleCandidate candidate = candidate("C2");
        candidate.setNewSpecAdvance(true);
        candidate.setNewSpecAdvanceQuantityNormalized(true);
        Cd15ScheduleCandidatePreparationService candidates = (context, input, classField, rolling) ->
                Collections.singletonList(candidate);
        Cd15ShiftDemandProvider demandProvider = (context, input, shift, selected, rolling) ->
                Cd15ShiftDemandDecision.builder().netDemandQuantity(new BigDecimal("70")).build();
        Cd15SingleShiftScheduleExecutor executor = new Cd15SingleShiftScheduleExecutor(
                candidates, demandProvider, (factoryCode, start, end) -> machineSnapshot(),
                trialPreparation(), committer(), new Cd15CloseOutCalculator(),
                new Cd15ScheduleCandidateSorter(), new Cd15SplitCutGroupBuilder(),
                new Cd15SpecShiftQuantityLimitResolver());
        Cd15AutoScheduleInput input = Cd15AutoScheduleInput.builder()
                .constructionMaterials(Collections.singletonList(Cd15ConstructionMaterial.builder()
                        .steelStripCode("C2").bigRollCode("BR2").cuttingAngle("15")
                        .unitConsumeMillimeter(new BigDecimal("2000"))
                        .craftWidth(new BigDecimal("280"))
                        .curlLength(new BigDecimal("100")).build()))
                .build();
        Cd15ShiftResourceState state = state();
        state.getLanes().get(0).setMaxVehicleCount(100);
        state.setTotalToolingCount(100);
        com.zlt.aps.cd15.engine.model.Cd15RollingScheduleContext rolling =
                com.zlt.aps.cd15.engine.model.Cd15RollingScheduleContext.builder()
                        .continueDemandBySteelStrip(new HashMap<>())
                        .newSpecAdvanceRemainingBySteelStrip(new HashMap<>(
                                Collections.singletonMap("C2", new BigDecimal("70"))))
                        .normalizedNewSpecAdvanceSteelStripCodes(new java.util.HashSet<>(
                                Collections.singleton("C2")))
                        .build();
        Cd15AutoScheduleContext context = context();
        context.getParameters().setMinStartQty(BigDecimal.ONE);

        Cd15ShiftExecutionResult result = executor.execute(
                context, input, shift(), state, rolling);

        assertEquals(new BigDecimal("70"), result.getAttemptTraces().get(0)
                .getNetDemandQuantity());
        assertEquals(new BigDecimal("70"), result.getTasks().get(0).getPlanQuantity());
    }

    /** 均分余量完成后，续作量、待办任务和均分标记必须同时清理。 */
    @Test
    public void shouldClearEqualShareStateWhenRemainderCompleted() {
        Cd15ScheduleCandidate candidate = candidate("C2");
        String materialKey = "C2|BR2|15|80|80|80";
        candidate.setMaterialKey(materialKey);
        Cd15ScheduleCandidatePreparationService candidates = (context, input, classField, rolling) ->
                Collections.singletonList(candidate);
        Cd15ShiftDemandProvider demandProvider = (context, input, shift, selected, rolling) ->
                Cd15ShiftDemandDecision.builder().netDemandQuantity(new BigDecimal("160")).build();
        Cd15SingleShiftScheduleExecutor executor = new Cd15SingleShiftScheduleExecutor(
                candidates, demandProvider, (factoryCode, start, end) -> machineSnapshot(),
                trialPreparation(), committer(), new Cd15CloseOutCalculator(),
                new Cd15ScheduleCandidateSorter(), new Cd15SplitCutGroupBuilder(),
                new Cd15SpecShiftQuantityLimitResolver());
        com.zlt.aps.cd15.engine.model.Cd15RollingScheduleContext rolling =
                com.zlt.aps.cd15.engine.model.Cd15RollingScheduleContext.builder()
                        .continueDemandBySteelStrip(new HashMap<>(
                                Collections.singletonMap(materialKey, new BigDecimal("160"))))
                        .pendingTasks(new java.util.ArrayList<>(Collections.singletonList(
                                com.zlt.aps.cd15.engine.model.Cd15RollingPendingTask.builder()
                                        .materialKey(materialKey).build())))
                        .equalSharePendingMaterialKeys(new java.util.HashSet<>(
                                Collections.singleton(materialKey)))
                        .build();
        Cd15AutoScheduleInput input = input();
        Cd15ShiftResourceState state = state();
        Cd15AutoScheduleContext context = context();
        context.getParameters().setMinStartQty(BigDecimal.ONE);
        Cd15ShiftExecutionResult result = executor.execute(
                context, input, shift(), state, rolling);

        assertEquals(1, result.getTasks().size());
        assertTrue(rolling.getContinueDemandBySteelStrip().isEmpty());
        assertTrue(rolling.getPendingTasks().isEmpty());
        assertTrue(rolling.getEqualSharePendingMaterialKeys().isEmpty());
    }
    /** 班初存在多台机尾连续候选时，应按机台顺序优先续作各自机尾规格。 */
    @Test
    public void shouldPrioritizeShiftStartTailCandidatesByMachineOrder() {
        Cd15ScheduleCandidatePreparationService candidates = (context, input, classField, rolling) ->
                Arrays.asList(candidate("211400161"), candidate("211400022"));
        Cd15ShiftDemandProvider demandProvider = (context, input, shift, candidate, rolling) ->
                Cd15ShiftDemandDecision.builder().netDemandQuantity(new BigDecimal("100")).build();
        Cd15SingleShiftScheduleExecutor executor = new Cd15SingleShiftScheduleExecutor(
                candidates, demandProvider, (factoryCode, start, end) -> twoMachineSnapshot(),
                trialPreparation(), committer(), new Cd15CloseOutCalculator(),
                new Cd15ScheduleCandidateSorter(), new Cd15SplitCutGroupBuilder(),
                new Cd15SpecShiftQuantityLimitResolver());
        Cd15ShiftResourceState state = twoMachineTailState();
        Cd15AutoScheduleContext context = context();
        context.getParameters().setMinStartQty(BigDecimal.ONE);

        Cd15ShiftExecutionResult result = executor.execute(context, twoTailInput(), shift(), state, null);

        assertEquals(result.getFailures().toString(), 2, result.getTasks().size());
        assertEquals("211400022", result.getTasks().get(0).getSteelStripCode());
        assertEquals("G1301", result.getTasks().get(0).getMachineCode());
        assertEquals("211400161", result.getTasks().get(1).getSteelStripCode());
        assertEquals("G1302", result.getTasks().get(1).getMachineCode());
    }

    /** 与CD90一致，班初先执行上一班真实机尾候选，再处理普通候选。 */
    @Test
    public void shouldPrioritizeInheritedMachineTailAtShiftStartLikeCd90() {
        Cd15ScheduleCandidate inheritedTail = candidate("211400161");
        Cd15ScheduleCandidate ordinaryCandidate = candidate("211400022");
        Cd15ScheduleCandidatePreparationService candidates =
                (context, input, classField, rolling) -> Arrays.asList(
                        ordinaryCandidate, inheritedTail);
        Cd15ShiftDemandProvider demandProvider = (context, input, shift, candidate, rolling) ->
                Cd15ShiftDemandDecision.builder().netDemandQuantity(
                        new BigDecimal("100")).build();
        Cd15SingleShiftScheduleExecutor executor = new Cd15SingleShiftScheduleExecutor(
                candidates, demandProvider, (factoryCode, start, end) -> twoMachineSnapshot(),
                trialPreparation(), committer(), new Cd15CloseOutCalculator(),
                new Cd15ScheduleCandidateSorter(), new Cd15SplitCutGroupBuilder(),
                new Cd15SpecShiftQuantityLimitResolver());
        Cd15ShiftResourceState state = twoMachineTailState();
        state.getTailByMachine().put("G1301", Cd15MachineTailState.builder()
                .steelStripCode("211400161").build());
        Cd15AutoScheduleContext context = context();
        context.getParameters().setMinStartQty(BigDecimal.ONE);

        Cd15ShiftExecutionResult result = executor.execute(
                context, twoTailInput(), shift(), state, null);

        assertEquals(result.getFailures().toString(), 2, result.getTasks().size());
        assertEquals("211400161", result.getTasks().get(0).getSteelStripCode());
        assertEquals("211400022", result.getTasks().get(1).getSteelStripCode());
    }

    /** 同一钢带存在多个角度时，下一班顺序1必须承接上一班的完整机尾材料。 */
    @Test
    public void shouldPrioritizeExactMaterialTailForSameSteelDifferentAngles() {
        Cd15ScheduleCandidate angle15 = splitCandidate("211500015", "15");
        Cd15ScheduleCandidate angle24 = splitCandidate("211500015", "24");
        Cd15SingleShiftScheduleExecutor executor = new Cd15SingleShiftScheduleExecutor(
                (context, input, classField, rolling) ->
                        Arrays.asList(angle15, angle24),
                (context, input, shift, candidate, rolling) ->
                        Cd15ShiftDemandDecision.builder()
                                .netDemandQuantity(new BigDecimal("100")).build(),
                (factoryCode, start, end) -> splitMachineSnapshotWithAngles(),
                trialPreparation(), committer(), new Cd15CloseOutCalculator(),
                new Cd15ScheduleCandidateSorter(), new Cd15SplitCutGroupBuilder(),
                new Cd15SpecShiftQuantityLimitResolver());
        Cd15ShiftResourceState state = splitState();
        state.getTailByMachine().put("G1401", Cd15MachineTailState.builder()
                .materialKey("211500015|BR-SPLIT|24|80|80|80")
                .steelStripCode("211500015")
                .bigRollCode("BR-SPLIT")
                .cuttingAngle("24")
                .build());
        Cd15AutoScheduleContext context = context();
        context.getParameters().setMinStartQty(BigDecimal.ONE);

        Cd15ShiftExecutionResult result = executor.execute(
                context,
                Cd15AutoScheduleInput.builder()
                        .constructionMaterials(Arrays.asList(
                                splitMaterial("211500015", "15"),
                                splitMaterial("211500015", "24")))
                        .build(),
                shift(), state, null);

        assertEquals(result.getFailures().toString(), 2, result.getTasks().size());
        assertEquals("24", result.getTasks().get(0).getCuttingAngle());
        assertEquals("G1401", result.getTasks().get(0).getMachineCode());
        assertEquals(1, result.getTasks().get(0).getProduceOrder());
    }
    /** 滚动规划器已给出稳定顺序时，班初机尾连续性不能再次打乱候选顺序。 */
    @Test
    public void shouldPreservePreparedOrderBeforeShiftStartTailPriority() {
        Cd15SingleShiftScheduleExecutor executor = new Cd15SingleShiftScheduleExecutor(
                (context, input, classField, rolling) -> Collections.emptyList(),
                (context, input, shift, candidate, rolling) ->
                        Cd15ShiftDemandDecision.builder().netDemandQuantity(new BigDecimal("100")).build(),
                (factoryCode, start, end) -> twoMachineSnapshot(),
                trialPreparation(), committer(), new Cd15CloseOutCalculator(),
                new Cd15ScheduleCandidateSorter(), new Cd15SplitCutGroupBuilder(),
                new Cd15SpecShiftQuantityLimitResolver());
        Cd15AutoScheduleContext context = context();
        context.getParameters().setMinStartQty(BigDecimal.ONE);
        Cd15ShiftExecutionResult result = executor.executePrepared(
                context, twoTailInput(), shift(), twoMachineTailState(), null,
                Arrays.asList(candidate("211400161"), candidate("211400022")),
                Collections.emptyMap(), true);

        assertEquals(result.getFailures().toString(), 2, result.getTasks().size());
        assertEquals("211400161", result.getTasks().get(0).getSteelStripCode());
        assertEquals("211400022", result.getTasks().get(1).getSteelStripCode());
    }

    /** 单规格分裁两路各补一车后按阈值跨四班排完，且合计保持完整计划量。 */
    @Test
    public void shouldScheduleSingleSpecSplitEqualShareAcrossFourShifts() {
        Cd15SingleShiftScheduleExecutor executor = new Cd15SingleShiftScheduleExecutor(
                (context, input, classField, rolling) -> {
                    Cd15ScheduleCandidate prepared = splitCandidate("C1");
                    if (rolling != null
                            && rolling.getEqualSharePendingMaterialKeys() != null
                            && !rolling.getEqualSharePendingMaterialKeys().isEmpty()) {
                        prepared.setContinueFromPreviousShift(true);
                        prepared.setSourceMachineCode("G1401");
                    }
                    return Collections.singletonList(prepared);
                },
                (context, input, shift, selected, rolling) ->
                        Cd15ShiftDemandDecision.builder()
                                .netDemandQuantity(BigDecimal.ONE).build(),
                (factoryCode, start, end) -> splitMachineSnapshot(),
                trialPreparation(), committer(), new Cd15CloseOutCalculator(),
                new Cd15ScheduleCandidateSorter(), new Cd15SplitCutGroupBuilder(),
                new Cd15SpecShiftQuantityLimitResolver());
        Cd15AutoScheduleInput input = Cd15AutoScheduleInput.builder()
                .constructionMaterials(Collections.singletonList(
                        splitMaterial("C1")))
                .build();
        Cd15RollingScheduleContext rolling = Cd15RollingScheduleContext.builder()
                .continueDemandBySteelStrip(new HashMap<>())
                .pendingTasks(new java.util.ArrayList<>())
                .equalSharePendingMaterialKeys(new java.util.HashSet<>())
                .build();
        Cd15AutoScheduleContext context = context();
        context.getParameters().setMinStartQty(BigDecimal.ONE);
        context.getParameters().setEqualShareThreshold(new BigDecimal("50"));

        Cd15ShiftExecutionResult firstShift = executor.execute(
                context, input, shift(), splitState(), rolling);
        assertEquals(1, rolling.getEqualSharePendingMaterialKeys().size());
        assertEquals(1, rolling.getPendingTasks().size());
        assertEquals("SPLIT", rolling.getPendingTasks().get(0).getCutMode());
        Cd15ShiftExecutionResult secondShift = executor.execute(
                context, input, shift(), splitState(), rolling);
        Cd15ShiftExecutionResult thirdShift = executor.execute(
                context, input, shift(), splitState(), rolling);
        Cd15ShiftExecutionResult fourthShift = executor.execute(
                context, input, shift(), splitState(), rolling);

        assertEquals(new BigDecimal("40"),
                firstShift.getTasks().get(0).getPlanQuantity());
        assertEquals(new BigDecimal("40"),
                secondShift.getTasks().get(0).getPlanQuantity());
        assertEquals(new BigDecimal("40"),
                thirdShift.getTasks().get(0).getPlanQuantity());
        assertEquals(new BigDecimal("40"),
                fourthShift.getTasks().get(0).getPlanQuantity());
        assertEquals(new BigDecimal("160"),
                firstShift.getTasks().get(0).getPlanQuantity().add(
                        secondShift.getTasks().get(0).getPlanQuantity()).add(
                        thirdShift.getTasks().get(0).getPlanQuantity()).add(
                        fourthShift.getTasks().get(0).getPlanQuantity()));
        assertEquals("G1401", firstShift.getTasks().get(0).getMachineCode());
        assertEquals("G1401", secondShift.getTasks().get(0).getMachineCode());
        assertEquals("G1401", thirdShift.getTasks().get(0).getMachineCode());
        assertEquals("G1401", fourthShift.getTasks().get(0).getMachineCode());
        assertEquals("SPLIT", firstShift.getTasks().get(0).getCutMode());
        assertEquals("SPLIT", secondShift.getTasks().get(0).getCutMode());
        assertEquals("SPLIT", thirdShift.getTasks().get(0).getCutMode());
        assertEquals("SPLIT", fourthShift.getTasks().get(0).getCutMode());
        assertTrue(rolling.getContinueDemandBySteelStrip().isEmpty());
        assertTrue(rolling.getPendingTasks().isEmpty());
        assertTrue(rolling.getEqualSharePendingMaterialKeys().isEmpty());
    }

    /** 毫米口径施工的跨班续作量已经是计划米数，不得再次按单耗和宽度换算。 */
    @Test
    public void shouldKeepNormalizedRemainderForMillimeterConstruction() {
        Cd15ScheduleCandidate candidate = millimeterSplitCandidate();
        candidate.setContinueFromPreviousShift(true);
        candidate.setSourceMachineCode("G1401");
        String materialKey = candidate.getMaterialKey();
        Cd15SingleShiftScheduleExecutor executor = new Cd15SingleShiftScheduleExecutor(
                (context, input, classField, rolling) ->
                        Collections.singletonList(candidate),
                (context, input, shift, selected, rolling) ->
                        Cd15ShiftDemandDecision.builder()
                                .netDemandQuantity(BigDecimal.ONE).build(),
                (factoryCode, start, end) -> splitMachineSnapshot(),
                trialPreparation(), committer(), new Cd15CloseOutCalculator(),
                new Cd15ScheduleCandidateSorter(), new Cd15SplitCutGroupBuilder(),
                new Cd15SpecShiftQuantityLimitResolver());
        Cd15RollingScheduleContext rolling = Cd15RollingScheduleContext.builder()
                .continueDemandBySteelStrip(new HashMap<>(Collections.singletonMap(
                        materialKey, new BigDecimal("301.0968"))))
                .pendingTasks(new java.util.ArrayList<>(Collections.singletonList(
                        com.zlt.aps.cd15.engine.model.Cd15RollingPendingTask.builder()
                                .materialKey(materialKey)
                                .cutMode("SPLIT")
                                .sourceMachineCode("G1401")
                                .build())))
                .equalSharePendingMaterialKeys(new java.util.HashSet<>(
                        Collections.singleton(materialKey)))
                .lastMachineBySteelStrip(new HashMap<>(Collections.singletonMap(
                        materialKey, "G1401")))
                .build();
        Cd15AutoScheduleContext context = context();
        context.getParameters().setMinStartQty(new BigDecimal("300"));
        context.getParameters().setEqualShareThreshold(new BigDecimal("2000"));
        context.getParameters().setRollTotalCount(200);

        Cd15ShiftExecutionResult result = executor.execute(
                context,
                Cd15AutoScheduleInput.builder()
                        .constructionMaterials(Collections.singletonList(
                                millimeterSplitMaterial()))
                        .build(),
                shift(), millimeterContinuationState(), rolling);

        assertEquals(result.getFailures().toString(), 1, result.getTasks().size());
        assertEquals(new BigDecimal("301.0968"),
                result.getTasks().get(0).getPlanQuantity());
        assertTrue(rolling.getContinueDemandBySteelStrip().isEmpty());
        assertTrue(rolling.getPendingTasks().isEmpty());
        assertTrue(rolling.getEqualSharePendingMaterialKeys().isEmpty());
    }
    /** 单规格分裁锁定后原机台不可排时必须保留余量，不得降级到其他单裁机台。 */
    @Test
    public void shouldNotDowngradeLockedSingleSpecSplitToSingleCut() {
        Cd15ScheduleCandidate candidate = splitCandidate("C1");
        candidate.setContinueFromPreviousShift(true);
        candidate.setSourceMachineCode("G1401");
        String materialKey = "C1|BR-SPLIT|15|80|80|80";
        candidate.setMaterialKey(materialKey);
        Cd15SingleShiftScheduleExecutor executor = new Cd15SingleShiftScheduleExecutor(
                (context, input, classField, rolling) ->
                        Collections.singletonList(candidate),
                (context, input, shift, selected, rolling) ->
                        Cd15ShiftDemandDecision.builder()
                                .netDemandQuantity(BigDecimal.ONE).build(),
                (factoryCode, start, end) -> splitMachineSnapshot(),
                trialPreparation(), committer(), new Cd15CloseOutCalculator(),
                new Cd15ScheduleCandidateSorter(), new Cd15SplitCutGroupBuilder(),
                new Cd15SpecShiftQuantityLimitResolver());
        Cd15RollingScheduleContext rolling = Cd15RollingScheduleContext.builder()
                .continueDemandBySteelStrip(new HashMap<>(
                        Collections.singletonMap(materialKey, new BigDecimal("40"))))
                .pendingTasks(new java.util.ArrayList<>(Collections.singletonList(
                        com.zlt.aps.cd15.engine.model.Cd15RollingPendingTask.builder()
                                .materialKey(materialKey)
                                .cutMode("SPLIT")
                                .sourceMachineCode("G1401")
                                .build())))
                .singleSpecSplitMaterialKeys(new java.util.HashSet<>(
                        Collections.singleton(materialKey)))
                .lastMachineBySteelStrip(new HashMap<>(
                        Collections.singletonMap(materialKey, "G1401")))
                .build();
        Cd15AutoScheduleContext context = context();
        context.getParameters().setMinStartQty(BigDecimal.ONE);

        Cd15ShiftExecutionResult result = executor.execute(
                context,
                Cd15AutoScheduleInput.builder()
                        .constructionMaterials(Collections.singletonList(
                                splitMaterial("C1")))
                        .build(),
                shift(), lockedSplitSourceMachineUnavailableState(), rolling);

        assertTrue(result.getTasks().toString(), result.getTasks().isEmpty());
        assertEquals(new BigDecimal("40"),
                rolling.getContinueDemandBySteelStrip().get(materialKey));
        assertTrue(rolling.getSingleSpecSplitMaterialKeys().contains(materialKey));
        assertTrue(result.getFailures().toString(), result.getFailures().containsValue("CAPACITY_LIMIT"));
    }
    /** 新排程仅生成单规格分裁，不再把两个不同钢带组成新分裁组。 */
    @Test
    public void shouldCommitEachCandidateAsIndependentSingleSpecSplit() {
        Cd15ScheduleCandidatePreparationService candidates = (context, input, classField, rolling) ->
                Arrays.asList(splitCandidate("C1"), splitCandidate("C2"));
        Cd15SingleShiftScheduleExecutor executor = new Cd15SingleShiftScheduleExecutor(
                candidates,
                (context, input, shift, candidate, rolling) ->
                        Cd15ShiftDemandDecision.builder()
                                .netDemandQuantity(new BigDecimal("100")).build(),
                (factoryCode, start, end) -> splitMachineSnapshot(),
                trialPreparation(), committer(), new Cd15CloseOutCalculator(),
                new Cd15ScheduleCandidateSorter(), new Cd15SplitCutGroupBuilder(),
                new Cd15SpecShiftQuantityLimitResolver());
        Cd15AutoScheduleContext context = context();
        context.getParameters().setMinStartQty(BigDecimal.ONE);

        Cd15ShiftExecutionResult result = executor.execute(
                context, splitInput(), shift(), splitState(), null);

        assertEquals(result.getFailures().toString(), 2, result.getTasks().size());
        assertEquals("G1401", result.getTasks().get(0).getMachineCode());
        assertEquals("G1401", result.getTasks().get(1).getMachineCode());
        assertEquals("SPLIT", result.getTasks().get(0).getCutMode());
        assertEquals("SPLIT", result.getTasks().get(1).getCutMode());
        assertEquals(null, result.getTasks().get(0).getSplitGroupKey());
        assertEquals(null, result.getTasks().get(1).getSplitGroupKey());
        assertEquals(false, result.getTasks().get(0).getProduceOrder()
                == result.getTasks().get(1).getProduceOrder());
    }

    /** 定时滚动重排已有分裁组时必须沿用原分组键。 */
    @Test
    public void shouldPreserveExistingSplitGroupDuringRolling() {
        Cd15ScheduleCandidate first = splitCandidate("C1");
        first.setRollingRequestedQuantity(new BigDecimal("100"));
        first.setCutMode("SPLIT");
        first.setSplitGroupKey("ORIGINAL-SPLIT-GROUP");
        Cd15ScheduleCandidate second = splitCandidate("C2");
        second.setRollingRequestedQuantity(new BigDecimal("100"));
        second.setCutMode("SPLIT");
        second.setSplitGroupKey("ORIGINAL-SPLIT-GROUP");
        Cd15SingleShiftScheduleExecutor executor = new Cd15SingleShiftScheduleExecutor(
                (context, input, classField, rolling) -> Collections.emptyList(),
                (context, input, shift, candidate, rolling) ->
                        Cd15ShiftDemandDecision.builder()
                                .netDemandQuantity(new BigDecimal("100")).build(),
                (factoryCode, start, end) -> splitMachineSnapshot(),
                trialPreparation(), committer(), new Cd15CloseOutCalculator(),
                new Cd15ScheduleCandidateSorter(), new Cd15SplitCutGroupBuilder(),
                new Cd15SpecShiftQuantityLimitResolver());
        Cd15AutoScheduleContext context = context();
        context.getParameters().setMinStartQty(BigDecimal.ONE);

        Cd15ShiftExecutionResult result = executor.executePrepared(
                context, splitInput(), shift(), splitState(), null,
                Arrays.asList(first, second), Collections.emptyMap(), true);

        assertEquals(result.getFailures().toString(), 2, result.getTasks().size());
        assertEquals("ORIGINAL-SPLIT-GROUP",
                result.getTasks().get(0).getSplitGroupKey());
        assertEquals("ORIGINAL-SPLIT-GROUP",
                result.getTasks().get(1).getSplitGroupKey());
    }

    private Cd15MachineTrialPreparationService trialPreparation() {
        return new Cd15MachineTrialPreparationService(
                new Cd15MachineCandidateResolver(),
                new Cd15CandidateMachineTrialCalculator(
                        new Cd15LossRateResolver(), new Cd15ScheduleQuantityCalculator(),
                        new Cd15ToolingCalculator(), new Cd15MachineCapacityCalculator(),
                        new Cd15BigRollAgingAllocator(), new Cd15BigRollMeterCalculator()),
                new Cd15MachineTrialSelector(),
                new Cd15VehiclePlanQuantityCalculator(),
                new Cd15MachineModeResolver());
    }

    private Cd15ShiftResourceCommitter committer() {
        return new Cd15ShiftResourceCommitter(
                new Cd15StorageLaneAllocator(), new Cd15MachineTrialSelector(),
                new Cd15BigRollAgingAllocator(), new Cd15BigRollMeterCalculator());
    }

    private Cd15ScheduleCandidate splitCandidate(String steelStripCode) {
        return this.splitCandidate(steelStripCode, "15");
    }

    private Cd15ScheduleCandidate splitCandidate(
            String steelStripCode, String cuttingAngle) {
        return Cd15ScheduleCandidate.builder()
                .materialKey(steelStripCode + "|BR-SPLIT|" + cuttingAngle
                        + "|80|80|80|false")
                .steelStripCode(steelStripCode)
                .bigRollCode("BR-SPLIT")
                .cuttingAngle(cuttingAngle)
                .craftWidth(new BigDecimal("80"))
                .unitConsumeMillimeter(new BigDecimal("80"))
                .build();
    }

    private Cd15AutoScheduleInput splitInput() {
        return Cd15AutoScheduleInput.builder()
                .constructionMaterials(Arrays.asList(
                        splitMaterial("C1"), splitMaterial("C2")))
                .build();
    }

    private Cd15ConstructionMaterial splitMaterial(String steelStripCode) {
        return this.splitMaterial(steelStripCode, "15");
    }

    private Cd15ConstructionMaterial splitMaterial(
            String steelStripCode, String cuttingAngle) {
        return Cd15ConstructionMaterial.builder()
                .steelStripCode(steelStripCode)
                .bigRollCode("BR-SPLIT")
                .cuttingAngle(cuttingAngle)
                .unitConsumeMillimeter(new BigDecimal("80"))
                .craftWidth(new BigDecimal("80"))
                .curlLength(new BigDecimal("80"))
                .build();
    }

    private Cd15ScheduleCandidate millimeterSplitCandidate() {
        return Cd15ScheduleCandidate.builder()
                .materialKey("C1|BR-SPLIT|15|37.2|610|87")
                .steelStripCode("C1")
                .bigRollCode("BR-SPLIT")
                .cuttingAngle("15")
                .craftWidth(new BigDecimal("37.2"))
                .unitConsumeMillimeter(new BigDecimal("610"))
                .curlLength(new BigDecimal("87"))
                .build();
    }

    private Cd15ConstructionMaterial millimeterSplitMaterial() {
        return Cd15ConstructionMaterial.builder()
                .steelStripCode("C1")
                .bigRollCode("BR-SPLIT")
                .cuttingAngle("15")
                .unitConsumeMillimeter(new BigDecimal("610"))
                .craftWidth(new BigDecimal("37.2"))
                .curlLength(new BigDecimal("87"))
                .build();
    }
    private Cd15MachineResourceSnapshot splitMachineSnapshot() {
        return Cd15MachineResourceSnapshot.builder()
                .machines(Arrays.asList(
                        Cd15MachineResource.builder().machineCode("G1101").status("1")
                                .openMachineClass("SHIFT1").singleCutSupported(true)
                                .defaultCutMode("SINGLE")
                                .singleShiftCapacity(new BigDecimal("800"))
                                .build(),
                        Cd15MachineResource.builder().machineCode("G1401").status("1")
                                .openMachineClass("SHIFT1").splitCutSupported(true)
                                .defaultCutMode("SPLIT")
                                .splitShiftCapacity(new BigDecimal("800"))
                                .build()))
                .bindings(Arrays.asList(
                        Cd15MachineRollBinding.builder().machineCode("G1101")
                                .bigRollCode("BR-SPLIT").shiftCode("SHIFT1").build(),
                        Cd15MachineRollBinding.builder().machineCode("G1401")
                                .bigRollCode("BR-SPLIT").shiftCode("SHIFT1").build()))
                .restrictions(Collections.emptyList())
                .lossRateRules(Collections.singletonList(Cd15LossRateRule.builder()
                        .lossRatePercent(BigDecimal.ZERO).build()))
                .angleWidthMaxByAngle(Collections.singletonMap(
                        "15", new BigDecimal("1000")))
                .build();
    }

    private Cd15MachineResourceSnapshot splitMachineSnapshotWithAngles() {
        Cd15MachineResourceSnapshot snapshot = splitMachineSnapshot();
        HashMap<String, BigDecimal> angleWidths = new HashMap<>(
                snapshot.getAngleWidthMaxByAngle());
        angleWidths.put("24", new BigDecimal("1000"));
        snapshot.setAngleWidthMaxByAngle(angleWidths);
        return snapshot;
    }
    private Cd15MachineResourceSnapshot equalShareTwoSplitMachineSnapshot() {
        return Cd15MachineResourceSnapshot.builder()
                .machines(Arrays.asList(
                        Cd15MachineResource.builder().machineCode("G1401").status("1")
                                .openMachineClass("SHIFT1").splitCutSupported(true)
                                .defaultCutMode("SPLIT")
                                .splitShiftCapacity(new BigDecimal("800")).build(),
                        Cd15MachineResource.builder().machineCode("G1501").status("1")
                                .openMachineClass("SHIFT1").splitCutSupported(true)
                                .defaultCutMode("SPLIT")
                                .splitShiftCapacity(new BigDecimal("800")).build()))
                .bindings(Arrays.asList(
                        Cd15MachineRollBinding.builder().machineCode("G1401")
                                .bigRollCode("BR-SPLIT").shiftCode("SHIFT1").build(),
                        Cd15MachineRollBinding.builder().machineCode("G1501")
                                .bigRollCode("BR-SPLIT").shiftCode("SHIFT1").build()))
                .restrictions(Collections.emptyList())
                .lossRateRules(Collections.singletonList(Cd15LossRateRule.builder()
                        .lossRatePercent(BigDecimal.ZERO).build()))
                .angleWidthMaxByAngle(Collections.singletonMap(
                        "15", new BigDecimal("1000")))
                .build();
    }

    private Cd15ShiftResourceState millimeterContinuationState() {
        HashMap<String, Integer> seconds = new HashMap<>();
        seconds.put("G1101", 28800);
        seconds.put("G1401", 28800);
        return Cd15ShiftResourceState.builder()
                .lanes(Arrays.asList(
                        Cd15StorageLaneState.builder().laneCode("L1")
                                .vehicleCount(0).maxVehicleCount(100).build(),
                        Cd15StorageLaneState.builder().laneCode("L2")
                                .vehicleCount(0).maxVehicleCount(100).build()))
                .totalToolingCount(200).occupiedToolingCount(0)
                .remainingSecondsByMachine(seconds)
                .tailSpecByMachine(new HashMap<>())
                .tailByMachine(new HashMap<>())
                .tasks(new java.util.ArrayList<>()).build();
    }
    private Cd15ShiftResourceState lockedSplitSourceMachineUnavailableState() {
        HashMap<String, Integer> seconds = new HashMap<>();
        seconds.put("G1401", 0);
        seconds.put("G1101", 28800);
        return Cd15ShiftResourceState.builder()
                .lanes(Arrays.asList(
                        Cd15StorageLaneState.builder().laneCode("L1")
                                .vehicleCount(0).maxVehicleCount(10).build(),
                        Cd15StorageLaneState.builder().laneCode("L2")
                                .vehicleCount(0).maxVehicleCount(10).build()))
                .totalToolingCount(20).occupiedToolingCount(0)
                .remainingSecondsByMachine(seconds)
                .tailSpecByMachine(new HashMap<>())
                .tailByMachine(new HashMap<>())
                .tasks(new java.util.ArrayList<>()).build();
    }
    private Cd15ShiftResourceState splitState() {
        HashMap<String, Integer> seconds = new HashMap<>();
        seconds.put("G1101", 28800);
        seconds.put("G1401", 28800);
        return Cd15ShiftResourceState.builder()
                .lanes(Arrays.asList(
                        Cd15StorageLaneState.builder().laneCode("L1")
                                .vehicleCount(0).maxVehicleCount(10).build(),
                        Cd15StorageLaneState.builder().laneCode("L2")
                                .vehicleCount(0).maxVehicleCount(10).build()))
                .totalToolingCount(20).occupiedToolingCount(0)
                .remainingSecondsByMachine(seconds)
                .tailSpecByMachine(new HashMap<>())
                .tailByMachine(new HashMap<>())
                .tasks(new java.util.ArrayList<>()).build();
    }

    private Cd15MachineResourceSnapshot machineSnapshot() {
        return Cd15MachineResourceSnapshot.builder()
                .machines(Collections.singletonList(Cd15MachineResource.builder()
                        .machineCode("M1").status("1").openMachineClass("SHIFT1")
                        .singleCutSupported(true).defaultCutMode("SINGLE")
                        .singleShiftCapacity(new BigDecimal("800"))
                        .build()))
                .bindings(Collections.singletonList(Cd15MachineRollBinding.builder()
                        .machineCode("M1").bigRollCode("BR2")
                        .shiftCode("SHIFT1").build()))
                .restrictions(Collections.emptyList())
                .lossRateRules(Collections.singletonList(Cd15LossRateRule.builder()
                        .lossRatePercent(BigDecimal.ZERO).build()))
                .angleWidthMaxByAngle(Collections.singletonMap(
                        "15", new BigDecimal("1000")))
                .build();
    }

    private Cd15MachineResourceSnapshot twoMachineSnapshot() {
        return Cd15MachineResourceSnapshot.builder()
                .machines(Arrays.asList(
                        Cd15MachineResource.builder().machineCode("G1301").status("1")
                                .openMachineClass("SHIFT1").singleCutSupported(true)
                                .defaultCutMode("SINGLE")
                                .singleShiftCapacity(new BigDecimal("800")).build(),
                        Cd15MachineResource.builder().machineCode("G1302").status("1")
                                .openMachineClass("SHIFT1").singleCutSupported(true)
                                .defaultCutMode("SINGLE")
                                .singleShiftCapacity(new BigDecimal("800")).build()))
                .bindings(Arrays.asList(
                        Cd15MachineRollBinding.builder().machineCode("G1301").bigRollCode("CSTA6023")
                                .shiftCode("SHIFT1").build(),
                        Cd15MachineRollBinding.builder().machineCode("G1302").bigRollCode("CSTA6023")
                                .shiftCode("SHIFT1").build()))
                .restrictions(Collections.emptyList())
                .lossRateRules(Collections.singletonList(Cd15LossRateRule.builder()
                        .lossRatePercent(BigDecimal.ZERO).build()))
                .angleWidthMaxByAngle(Collections.singletonMap(
                        "15", new BigDecimal("1000")))
                .build();
    }

    private Cd15AutoScheduleInput twoTailInput() {
        return Cd15AutoScheduleInput.builder()
                .constructionMaterials(Arrays.asList(
                        material("211400022"), material("211400161")))
                .build();
    }

    private Cd15ConstructionMaterial material(String steelStripCode) {
        return Cd15ConstructionMaterial.builder()
                .steelStripCode(steelStripCode).bigRollCode("CSTA6023")
                .cuttingAngle("15").unitConsumeMillimeter(new BigDecimal("80"))
                .craftWidth(new BigDecimal("80"))
                .curlLength(new BigDecimal("80")).build();
    }

    private Cd15ShiftResourceState twoMachineTailState() {
        HashMap<String, Integer> seconds = new HashMap<>();
        seconds.put("G1301", 28800);
        seconds.put("G1302", 28800);
        LinkedHashMap<String, Cd15MachineTailState> tails = new LinkedHashMap<>();
        tails.put("G1301", Cd15MachineTailState.builder()
                .steelStripCode("211400022").bigRollCode("CSTA6023")
                .cuttingAngle("15").build());
        tails.put("G1302", Cd15MachineTailState.builder()
                .steelStripCode("211400161").bigRollCode("CSTA6023")
                .cuttingAngle("15").build());
        return Cd15ShiftResourceState.builder()
                .lanes(Arrays.asList(
                        Cd15StorageLaneState.builder().laneCode("L1").vehicleCount(0).maxVehicleCount(10).build(),
                        Cd15StorageLaneState.builder().laneCode("L2").vehicleCount(0).maxVehicleCount(10).build()))
                .totalToolingCount(20).occupiedToolingCount(0)
                .remainingSecondsByMachine(seconds).tailSpecByMachine(new HashMap<>())
                .tailByMachine(tails).tasks(new java.util.ArrayList<>()).build();
    }

    private Cd15AutoScheduleInput input() {
        return Cd15AutoScheduleInput.builder()
                .constructionMaterials(Collections.singletonList(Cd15ConstructionMaterial.builder()
                        .steelStripCode("C2").bigRollCode("BR2").cuttingAngle("15")
                        .unitConsumeMillimeter(new BigDecimal("80"))
                        .craftWidth(new BigDecimal("80"))
                        .curlLength(new BigDecimal("80")).build()))
                .build();
    }

    private Cd15ShiftResourceState state() {
        HashMap<String, Integer> seconds = new HashMap<>();
        seconds.put("M1", 28800);
        return Cd15ShiftResourceState.builder()
                .lanes(Collections.singletonList(Cd15StorageLaneState.builder()
                        .laneCode("L1").steelStripCode("C2").vehicleCount(0).maxVehicleCount(10).build()))
                .totalToolingCount(10).occupiedToolingCount(0)
                .remainingSecondsByMachine(seconds).tailSpecByMachine(new HashMap<>())
                .tasks(new java.util.ArrayList<>()).build();
    }

    private Cd15AutoScheduleContext context() {
        return Cd15AutoScheduleContext.builder().factoryCode("116")
                .scheduleDate(LocalDate.of(2026, 6, 13))
                .parameters(Cd15AutoScheduleParameters.builder()
                        .minStartQty(new BigDecimal("100"))
                        .equalShareThreshold(new BigDecimal("2000"))
                        .rollCoilMeter(new BigDecimal("100")).rollTotalCount(10)
                        .machinePriority(Collections.singletonList("M1"))
                        .specChangeMinutes(10).build()).build();
    }

    private Cd15ShiftDescriptor shift() {
        return Cd15ShiftDescriptor.builder().shiftCode("SHIFT1").classField("CLASS1")
                .startTime(LocalDateTime.of(2026, 6, 12, 14, 0))
                .endTime(LocalDateTime.of(2026, 6, 12, 22, 0))
                .durationSeconds(28800).build();
    }

    private Cd15ScheduleCandidate candidate(String steelStripCode) {
        return Cd15ScheduleCandidate.builder().steelStripCode(steelStripCode).build();
    }
}
