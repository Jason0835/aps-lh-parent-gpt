package com.zlt.aps.gsq.engine.handler;

import com.zlt.aps.gsq.engine.context.GsqScheduleContext;
import com.zlt.aps.gsq.engine.vo.GsqScheduleResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * S3.5: 钢丝圈剩余产能分配Handler。
 *
 * <p>核心逻辑：</p>
 * <ol>
 *   <li>找出S3阶段未排产或排产量不足的规格</li>
 *   <li>查找同班次其他机台的剩余产能</li>
 *   <li>将剩余产能分配给未排产规格</li>
 * </ol>
 *
 * @author APS
 */
@Slf4j
@Component
public class GsqResidualCapacityHandler extends AbsGsqScheduleStepHandler {

    @Override
    protected String getStepName() {
        return "S3.5-剩余产能分配";
    }

    @Override
    protected void doHandle(GsqScheduleContext context) {
        List<GsqScheduleResultVo> scheduleList = context.getScheduleList();
        if (scheduleList.isEmpty()) {
            return;
        }

        int allocatedCount = 0;
        for (GsqScheduleResultVo vo : scheduleList) {
            if (!"1".equals(vo.getUnscheduledFlag())) {
                continue;
            }

            // 尝试将剩余产能分配给未排产规格
            boolean allocated = tryAllocateResidualCapacity(vo, context);
            if (allocated) {
                vo.setUnscheduledFlag("0");
                allocatedCount++;
                log.info("[S3.5] 规格[{}] 通过剩余产能分配成功", vo.getSteelRingCode());
            }
        }

        log.info("[S3.5] 剩余产能分配完成, 成功分配规格数: {}", allocatedCount);
    }

    /**
     * 尝试从同班次其他机台剩余产能中分配给当前规格。
     *
     * <p>TODO: 实际实现需要根据机台任务链的剩余产能、规格切换成本等因素综合决策，
     * 当前为占位实现，等待Phase 5策略链完善后补充完整逻辑。</p>
     */
    private boolean tryAllocateResidualCapacity(GsqScheduleResultVo vo, GsqScheduleContext context) {
        // 占位实现：当前阶段不进行实际分配，直接返回失败
        // 完整逻辑将在后续迭代中补充
        return false;
    }
}
