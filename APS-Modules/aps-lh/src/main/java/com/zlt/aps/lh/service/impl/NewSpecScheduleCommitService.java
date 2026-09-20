package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import cn.hutool.core.bean.BeanUtil;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.component.StructureEndingAlignmentService;
import com.zlt.aps.lh.engine.strategy.support.StructureSwitchSchedulingPolicy;
import org.apache.commons.lang3.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.function.IntSupplier;

/**
 * S4.5 新增排产提案一次性提交服务。
 *
 * <p>现有新增内核包含模具、换模、首检、胶囊、班次结果、日计划、胎胚和机台运行态等成熟逻辑。
 * 本服务在调用该内核前复用置换链已经验证的完整内存快照：形成有效结果或终局未排时保留提交，
 * 普通Machine×SKU失败或异常时一次性恢复全部共享状态，禁止依赖分散rollback保证一致性。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class NewSpecScheduleCommitService {

    /** 失败恢复后重建结构×班次物理机台索引 */
    @Resource
    private StructureEndingAlignmentService structureEndingAlignmentService;

    /**
     * 原子执行单个Machine×SKU提案。
     *
     * @param context 排程上下文
     * @param selectedSku 当前提案SKU
     * @param commitAction 提前生产激活及正式提交动作
     * @return 明确区分有效结果、终局状态和完整回滚的提交结果
     */
    public NewSpecScheduleCommitResult commit(LhScheduleContext context,
                                              SkuScheduleDTO selectedSku,
                                              IntSupplier commitAction) {
        Objects.requireNonNull(context, "排程上下文不能为空");
        Objects.requireNonNull(selectedSku, "新增排产提案SKU不能为空");
        Objects.requireNonNull(commitAction, "新增排产提交动作不能为空");
        ScheduleSubstitutionAttemptSnapshot snapshot =
                ScheduleSubstitutionAttemptSnapshot.capture(
                        context, new ArrayList<SkuScheduleDTO>(context.getNewSpecSkuList()));
        int beforeResultCount = context.getScheduleResultList().size();
        int beforeUnscheduledCount = context.getUnscheduledResultList().size();
        boolean selectedSkuPendingBefore = this.containsByIdentity(
                context, selectedSku);
        try {
            int scheduledCount = commitAction.getAsInt();
            String switchFailure = StructureSwitchSchedulingPolicy.validateCommittedResults(context, beforeResultCount);
            if (StringUtils.isNotEmpty(switchFailure)) {
                snapshot.restore(context);
                this.rebuildStructureInMachineIndex(context);
                log.info("结构切换实际提交校验失败，完整回滚, batchNo={}, reason={}", context.getBatchNo(), switchFailure);
                return NewSpecScheduleCommitResult.rolledBack();
            }
            boolean terminalStateCommitted = context.getScheduleResultList().size() > beforeResultCount
                    || context.getUnscheduledResultList().size() > beforeUnscheduledCount
                    || (selectedSkuPendingBefore && !this.containsByIdentity(context, selectedSku));
            if (scheduledCount > 0) {
                return NewSpecScheduleCommitResult.resultCommitted(scheduledCount);
            }
            if (terminalStateCommitted) {
                return NewSpecScheduleCommitResult.terminalStateCommitted();
            }
            snapshot.restore(context);
            this.rebuildStructureInMachineIndex(context);
            log.info("新增排产提案未形成有效结果，完整运行态已恢复, batchNo: {}, materialCode: {}, productStatus: {}",
                    context.getBatchNo(), selectedSku.getMaterialCode(), selectedSku.getProductStatus());
            return NewSpecScheduleCommitResult.rolledBack();
        } catch (RuntimeException exception) {
            snapshot.restore(context);
            this.rebuildStructureInMachineIndex(context);
            log.error("新增排产提案提交异常，完整运行态已恢复, batchNo: {}, materialCode: {}, productStatus: {}",
                    context.getBatchNo(), selectedSku.getMaterialCode(),
                    selectedSku.getProductStatus(), exception);
            throw exception;
        }
    }

    /**
     * 执行指定组合，未形成正量结果时恢复全部资源及待排身份。
     *
     * <p>固定组合失败不代表物料整体不可排；普通提交仍保持原终局未排处理语义。</p>
     *
     * @param context 排程上下文
     * @param attemptAction 共用排产动作
     * @return 有效结果提交数或完整回滚；异常恢复后继续传播
     */
    public NewSpecScheduleCommitResult commitResultOnly(LhScheduleContext context, IntSupplier attemptAction) {
        int beforeResultCount = context.getScheduleResultList().size();
        // 前置阶段持有机台对象引用，失败恢复时同步原对象，避免下一组合使用试排后的状态。
        Map<String, MachineScheduleDTO> originalMachines =
                new LinkedHashMap<String, MachineScheduleDTO>(context.getMachineScheduleMap());
        ScheduleSubstitutionAttemptSnapshot snapshot = ScheduleSubstitutionAttemptSnapshot.capture(
                context, new ArrayList<SkuScheduleDTO>(context.getNewSpecSkuList()));
        boolean success = false;
        try {
            int scheduledCount = attemptAction.getAsInt();
            String switchFailure = StructureSwitchSchedulingPolicy.validateCommittedResults(context, beforeResultCount);
            success = scheduledCount > 0 && StringUtils.isEmpty(switchFailure);
            if (StringUtils.isNotEmpty(switchFailure)) {
                log.info("结构切换指定组合提交拒绝, batchNo={}, reason={}", context.getBatchNo(), switchFailure);
            }
            return success ? NewSpecScheduleCommitResult.resultCommitted(scheduledCount)
                    : NewSpecScheduleCommitResult.rolledBack();
        } finally {
            if (!success) {
                snapshot.restore(context);
                originalMachines.forEach((machineCode, originalMachine) -> {
                    BeanUtil.copyProperties(context.getMachineScheduleMap().get(machineCode), originalMachine);
                    context.getMachineScheduleMap().put(machineCode, originalMachine);
                });
                this.rebuildStructureInMachineIndex(context);
            }
        }
    }

    private boolean containsByIdentity(LhScheduleContext context,
                                       SkuScheduleDTO selectedSku) {
        for (SkuScheduleDTO pendingSku : context.getNewSpecSkuList()) {
            if (pendingSku == selectedSku) {
                return true;
            }
        }
        return false;
    }

    private void rebuildStructureInMachineIndex(LhScheduleContext context) {
        structureEndingAlignmentService.prepareStructureEndingAlignmentIndex(context);
    }

}
