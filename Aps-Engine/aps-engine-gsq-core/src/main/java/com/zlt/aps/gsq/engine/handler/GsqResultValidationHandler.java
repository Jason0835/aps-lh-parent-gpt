package com.zlt.aps.gsq.engine.handler;

import com.ruoyi.common.core.utils.SecurityUtils;
import com.zlt.aps.gsq.engine.context.GsqScheduleContext;
import com.zlt.aps.gsq.engine.mapper.GsqEngineMapper;
import com.zlt.aps.gsq.engine.vo.GsqScheduleResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * S6: 钢丝圈结果校验与持久化Handler。
 *
 * <p>核心逻辑：</p>
 * <ol>
 *   <li>校验排程结果完整性（批次号、工单号、机台分配等）</li>
 *   <li>设置保鲜期超期标记</li>
 *   <li>删除当天已有排程记录</li>
 *   <li>批量插入新排程记录</li>
 *   <li>记录排程日志</li>
 * </ol>
 *
 * @author APS
 */
@Slf4j
@Component
public class GsqResultValidationHandler extends AbsGsqScheduleStepHandler {

    @Resource
    private GsqEngineMapper gsqEngineMapper;

    @Override
    protected String getStepName() {
        return "S6-结果校验与持久化";
    }

    @Override
    protected void doHandle(GsqScheduleContext context) {
        List<GsqScheduleResultVo> scheduleList = context.getScheduleList();
        if (scheduleList.isEmpty()) {
            log.warn("[S6] 排程结果为空, 跳过持久化");
            return;
        }

        // 1. 结果校验
        validateResult(scheduleList, context);

        // 2. 设置保鲜期超期标记
        markFreshExpired(scheduleList, context);

        // 3. 备份当天已有排程记录到日志表，然后删除
        gsqEngineMapper.syncGsqScheduleToLog(context.getScheduleDate());
        gsqEngineMapper.deleteGsqSchedule(context.getScheduleDate());

        // 4. 批量插入新排程记录
        String username = SecurityUtils.getUsername();
        Date now = new Date();
        scheduleList.forEach(vo -> {
            vo.setUpdateBy(username);
            vo.setUpdateTime(now);
        });

        gsqEngineMapper.batchCreateScheduleResult(scheduleList);
        context.setInsertedCount(scheduleList.size());

        log.info("[S6] 排程结果持久化完成, 插入记录数: {}", scheduleList.size());
    }

    /**
     * 结果完整性校验。
     */
    private void validateResult(List<GsqScheduleResultVo> scheduleList, GsqScheduleContext context) {
        List<String> errors = new ArrayList<>();
        for (GsqScheduleResultVo vo : scheduleList) {
            if (vo.getBatchNo() == null || vo.getBatchNo().isEmpty()) {
                errors.add("规格[" + vo.getSteelRingCode() + "]批次号为空");
            }
            if (vo.getOrderNo() == null || vo.getOrderNo().isEmpty()) {
                errors.add("规格[" + vo.getSteelRingCode() + "]工单号为空");
            }
            if (vo.getScheduleDate() == null) {
                errors.add("规格[" + vo.getSteelRingCode() + "]排程日期为空");
            }
        }

        if (!errors.isEmpty()) {
            context.addValidationError("结果校验失败：" + String.join("; ", errors));
            log.warn("[S6] 结果校验存在告警: {}", errors);
        }
    }

    /**
     * 设置保鲜期超期标记。
     *
     * <p>规则：钢丝圈产出时间到胎圈消耗时间超过72小时，则标记为超期。</p>
     * <p>简化逻辑：当规格在1班排产但对应胎圈消耗在4班之后（超过3班次=72小时），标记为超期。</p>
     */
    private void markFreshExpired(List<GsqScheduleResultVo> scheduleList, GsqScheduleContext context) {
        Double freshPeriodHours = context.getParams().getFreshPeriodHours();
        if (freshPeriodHours == null || freshPeriodHours <= 0) {
            freshPeriodHours = 72D;
        }

        // 每班次8小时，保鲜期允许的最大班次跨度
        int maxShiftSpan = (int) Math.floor(freshPeriodHours / 8);

        for (GsqScheduleResultVo vo : scheduleList) {
            boolean expired = false;

            // 检查每个班次的产出是否会超期
            for (int classIndex = 1; classIndex <= 6; classIndex++) {
                Double planQty = getShiftPlan(vo, classIndex);
                if (planQty == null || planQty <= 0) {
                    continue;
                }

                // 对应胎圈消耗班次 = 钢丝圈班次 + 1
                int tqConsumeClass = classIndex + 1;
                int shiftSpan = tqConsumeClass - classIndex;
                if (shiftSpan > maxShiftSpan) {
                    expired = true;
                    break;
                }
            }

            if (expired) {
                vo.setFreshExpiredFlag("1");
                log.warn("[S6] 规格[{}] 存在保鲜期超期", vo.getSteelRingCode());
            }
        }
    }

    /**
     * 获取指定班次的计划量。
     */
    private Double getShiftPlan(GsqScheduleResultVo vo, int classIndex) {
        Object value = vo.getFieldValueByFieldName("class" + classIndex + "PlanQty");
        return value == null ? null : ((Number) value).doubleValue();
    }
}
