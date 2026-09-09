package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import cn.hutool.core.bean.BeanUtil;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.component.StructureEndingAlignmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.function.IntSupplier;
import java.util.function.BooleanSupplier;

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
     * 执行历史优先候选，失败必须恢复完整基线后交还原选择路径。
     *
     * <p>只供额外的历史候选尝试使用；普通提交仍保持原终局未排处理语义。</p>
     *
     * @param context 排程上下文
     * @param attemptAction 原换活字块排产动作
     * @return 是否形成有效结果；失败或异常恢复全部资源，异常继续传播
     */
    public boolean tryPreviousAlternation(LhScheduleContext context, BooleanSupplier attemptAction) {
        // S4.4原循环持有机台对象引用，失败恢复时需一并恢复这些引用，避免继续使用试排后的旧对象。
        Map<String, MachineScheduleDTO> originalMachines =
                new LinkedHashMap<String, MachineScheduleDTO>(context.getMachineScheduleMap());
        ScheduleSubstitutionAttemptSnapshot snapshot = ScheduleSubstitutionAttemptSnapshot.capture(
                context, new ArrayList<SkuScheduleDTO>(context.getNewSpecSkuList()));
        boolean success = false;
        try {
            success = attemptAction.getAsBoolean();
            return success;
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
