package com.zlt.aps.gsq.engine.handler;

import com.zlt.aps.gsq.engine.context.GsqScheduleContext;
import com.zlt.aps.gsq.engine.vo.GsqScheduleResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * S5.5: 钢丝圈定额校验与顺序重置Handler。
 *
 * <p>核心逻辑：</p>
 * <ol>
 *   <li>校验每个班次每个机台的分配计划量不超过机台定额</li>
 *   <li>对超过定额的部分，记录分析信息</li>
 *   <li>按"库存供应时长"重新设置排程顺序号（class1Sequence~class6Sequence）</li>
 * </ol>
 *
 * @author APS
 */
@Slf4j
@Component
public class GsqQuotaValidateHandler extends AbsGsqScheduleStepHandler {

    @Override
    protected String getStepName() {
        return "S5.5-定额校验与顺序重置";
    }

    @Override
    protected void doHandle(GsqScheduleContext context) {
        List<GsqScheduleResultVo> scheduleList = context.getScheduleList();
        if (scheduleList.isEmpty()) {
            return;
        }

        // 1. 定额校验
        validateQuota(scheduleList);

        // 2. 6班次顺序重置
        resetSequence(scheduleList, context);

        log.info("[S5.5] 定额校验与顺序重置完成");
    }

    /**
     * 定额校验：检查每个班次的计划量是否超出机台定额。
     */
    private void validateQuota(List<GsqScheduleResultVo> scheduleList) {
        for (GsqScheduleResultVo vo : scheduleList) {
            validateClassQuota(vo, 1, vo.getClass1PlanQty(), vo.getClass1MachineQuota());
            validateClassQuota(vo, 2, vo.getClass2PlanQty(), vo.getClass2MachineQuota());
            validateClassQuota(vo, 3, vo.getClass3PlanQty(), vo.getClass3MachineQuota());
            validateClassQuota(vo, 4, vo.getClass4PlanQty(), vo.getClass4MachineQuota());
            validateClassQuota(vo, 5, vo.getClass5PlanQty(), vo.getClass5MachineQuota());
            validateClassQuota(vo, 6, vo.getClass6PlanQty(), vo.getClass6MachineQuota());
        }
    }

    /**
     * 校验单班次定额。
     */
    private void validateClassQuota(GsqScheduleResultVo vo, int classIndex, Double planQty, Double quota) {
        if (planQty == null || planQty <= 0) {
            return;
        }
        if (quota == null || quota <= 0) {
            return;
        }
        if (planQty > quota) {
            String warning = "计划量" + planQty + "超过机台定额" + quota;
            appendAnalysis(vo, classIndex, warning);
            log.warn("[S5.5] 规格[{}] 班次[{}] {}", vo.getSteelRingCode(), classIndex, warning);
        }
    }

    /**
     * 6班次顺序重置：按库存供应时长升序设置顺序号。
     */
    private void resetSequence(List<GsqScheduleResultVo> scheduleList, GsqScheduleContext context) {
        // 按预计库存升序排序（库存少=紧急=顺序号小）
        List<GsqScheduleResultVo> sorted = scheduleList.stream()
                .sorted(Comparator.comparingDouble(vo -> vo.getPlanStockQty() == null ? 0 : vo.getPlanStockQty()))
                .collect(Collectors.toList());

        for (int i = 0; i < sorted.size(); i++) {
            GsqScheduleResultVo vo = sorted.get(i);
            int seq = i + 1;
            vo.setClass1Sequence(seq);
            vo.setClass2Sequence(seq);
            vo.setClass3Sequence(seq);
            vo.setClass4Sequence(seq);
            vo.setClass5Sequence(seq);
            vo.setClass6Sequence(seq);
        }
    }

    /**
     * 追加班次分析信息。
     */
    private void appendAnalysis(GsqScheduleResultVo vo, int classIndex, String analysis) {
        String existing = getShiftAnalysis(vo, classIndex);
        String newAnalysis = existing == null || existing.isEmpty() ? analysis : existing + ";" + analysis;
        setShiftAnalysis(vo, classIndex, newAnalysis);
    }

    private String getShiftAnalysis(GsqScheduleResultVo vo, int classIndex) {
        switch (classIndex) {
            case 1: return vo.getClass1Analysis();
            case 2: return vo.getClass2Analysis();
            case 3: return vo.getClass3Analysis();
            case 4: return vo.getClass4Analysis();
            case 5: return vo.getClass5Analysis();
            case 6: return vo.getClass6Analysis();
            default: return "";
        }
    }

    private void setShiftAnalysis(GsqScheduleResultVo vo, int classIndex, String analysis) {
        switch (classIndex) {
            case 1: vo.setClass1Analysis(analysis); break;
            case 2: vo.setClass2Analysis(analysis); break;
            case 3: vo.setClass3Analysis(analysis); break;
            case 4: vo.setClass4Analysis(analysis); break;
            case 5: vo.setClass5Analysis(analysis); break;
            case 6: vo.setClass6Analysis(analysis); break;
            default: break;
        }
    }
}
