package com.zlt.aps.gsq.engine.handler;

import com.zlt.aps.gsq.engine.context.GsqScheduleContext;
import com.zlt.aps.gsq.engine.vo.GsqScheduleResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * S4: 钢丝圈停产协调Handler。
 *
 * <p>核心逻辑：</p>
 * <ol>
 *   <li>读取胎圈停产班次配置</li>
 *   <li>读取钢丝圈停产班次配置</li>
 *   <li>对处于停产班次的排程记录，将计划量调整为0，并标注停产原因</li>
 *   <li>胎圈停产时，对应钢丝圈班次需求量同步清零（上游无消耗则下游不备货）</li>
 * </ol>
 *
 * @author APS
 */
@Slf4j
@Component
public class GsqStopCoordinationHandler extends AbsGsqScheduleStepHandler {

    @Override
    protected String getStepName() {
        return "S4-胎圈/钢丝圈停产协调";
    }

    @Override
    protected void doHandle(GsqScheduleContext context) {
        List<GsqScheduleResultVo> scheduleList = context.getScheduleList();
        if (scheduleList.isEmpty()) {
            return;
        }

        int stopAdjustedCount = 0;
        for (GsqScheduleResultVo vo : scheduleList) {
            // 处理6个班次的停产协调
            for (int classIndex = 1; classIndex <= 6; classIndex++) {
                boolean adjusted = handleClassStop(vo, classIndex, context);
                if (adjusted) {
                    stopAdjustedCount++;
                }
            }
        }

        log.info("[S4] 停产协调完成, 调整记录数: {}", stopAdjustedCount);
    }

    /**
     * 处理指定班次的停产协调。
     *
     * <p>规则：</p>
     * <ul>
     *   <li>钢丝圈N班停产 → 钢丝圈N班计划量清零，分析注明"钢丝圈停产"</li>
     *   <li>胎圈N+1班停产 → 钢丝圈N班无需求，计划量清零，分析注明"胎圈停产"</li>
     * </ul>
     */
    private boolean handleClassStop(GsqScheduleResultVo vo, int classIndex, GsqScheduleContext context) {
        // 对应胎圈班次索引（钢丝圈N班供应胎圈N+1班）
        int tqClassIndex = classIndex + 1;
        String tqClassKey = buildClassKey(context.getScheduleDate(), tqClassIndex);
        String gsqClassKey = buildClassKey(context.getScheduleDate(), classIndex);

        boolean tqStop = Boolean.TRUE.equals(context.getTqStopShiftMap().get(tqClassKey));
        boolean gsqStop = Boolean.TRUE.equals(context.getGsqStopShiftMap().get(gsqClassKey));

        if (!tqStop && !gsqStop) {
            return false;
        }

        // 计划量清零
        setShiftPlanZero(vo, classIndex);

        // 分析说明
        String reason;
        if (gsqStop && tqStop) {
            reason = "钢丝圈/胎圈均停产";
        } else if (gsqStop) {
            reason = "钢丝圈停产";
        } else {
            reason = "胎圈停产";
        }
        setShiftAnalysis(vo, classIndex, reason);

        return true;
    }

    /**
     * 构建班次Map的key：日期|班次编码
     */
    private String buildClassKey(String scheduleDate, int classIndex) {
        // 班次编码：1/4=中班03, 2/5=夜班01, 3/6=早班02
        String classCode;
        switch (classIndex) {
            case 1: case 4: classCode = "03"; break;
            case 2: case 5: classCode = "01"; break;
            case 3: case 6: classCode = "02"; break;
            default: classCode = "";
        }
        return scheduleDate + "|" + classCode;
    }

    /**
     * 将指定班次计划量清零。
     */
    private void setShiftPlanZero(GsqScheduleResultVo vo, int classIndex) {
        vo.setFieldValueByFieldName("class" + classIndex + "PlanQty", 0D);
    }

    /**
     * 设置指定班次分析说明。
     */
    private void setShiftAnalysis(GsqScheduleResultVo vo, int classIndex, String analysis) {
        vo.setFieldValueByFieldName("class" + classIndex + "Analysis", analysis);
    }
}
