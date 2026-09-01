package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.component.StructureEndingAlignmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Objects;
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
