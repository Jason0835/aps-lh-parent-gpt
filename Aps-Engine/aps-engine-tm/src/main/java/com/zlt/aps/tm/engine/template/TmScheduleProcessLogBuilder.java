package com.zlt.aps.tm.engine.template;

import cn.hutool.core.date.DateUtil;
import com.zlt.aps.tm.api.enums.TmScheduleStepEnum;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;

import java.util.stream.Collectors;

/**
 * 胎面自动排程步骤过程日志摘要构建器。
 *
 * <p>本类只读取既有上下文构建步骤开始和结束摘要，不写入日志、不更新进度，
 * 以保持模板类原有日志保存时机、成功路径和失败路径不变。</p>
 */
final class TmScheduleProcessLogBuilder {

    /**
     * 构建步骤开始或结束时写入的过程日志摘要。
     *
     * @param context 排程上下文
     * @param stepEnum 排程步骤
     * @param input 是否为步骤开始摘要
     * @return 当前步骤摘要
     */
    String buildStepSummary(TmScheduleContext context, TmScheduleStepEnum stepEnum, boolean input) {
        if (context == null) {
            return "排程上下文为空";
        }
        switch (stepEnum) {
            case BOOTSTRAP:
                return input ? "工厂编号=" + context.getFactoryCode() + "，排程日期="
                        + (context.getScheduleDate() == null ? null : DateUtil.formatDate(context.getScheduleDate()))
                        : "任务数量=" + context.getTaskDraftList().size() + "，机台数量="
                        + context.getMachineCandidateList().size() + "，参数数量=" + context.getParamMap().size();
            case INVENTORY_PREDICT:
                return input ? "胎面数量=" + context.getTaskDraftList().stream()
                        .map(TmTaskDraft::getTreadCode).filter(code -> code != null && code.trim().length() > 0)
                        .collect(Collectors.toSet()).size()
                        : "库存预测数量=" + context.getStockForecastMap().size();
            case PLAN_CALC:
                return input ? "任务数量=" + context.getTaskDraftList().size()
                        : "已计算计划量任务数量=" + context.getTaskDraftList().stream()
                        .filter(task -> task.getPlanQty() != null).count() + "，未排任务数量=" + context.getTaskDraftList().stream()
                        .filter(task -> task.isUnassigned() || (task.getUnplannedReasonCode() != null
                                && task.getUnplannedReasonCode().trim().length() > 0)).count();
            case TASK_SORT:
                return input ? "任务数量=" + context.getTaskDraftList().size()
                        : "已排序任务数量=" + context.getTaskDraftList().size();
            case MACHINE_ASSIGN:
                return input ? "任务数量=" + context.getTaskDraftList().size()
                        : "已分配任务数量=" + context.getTaskDraftList().stream().filter(task -> !task.isUnassigned()).count()
                        + "，未排任务数量=" + context.getTaskDraftList().stream().filter(TmTaskDraft::isUnassigned).count();
            case SNAPSHOT_BUILD:
                return input ? "任务数量=" + context.getTaskDraftList().size()
                        : "解释快照数量=" + context.getSnapshotMap().size()
                        + "，结果数量=" + (context.getPersistResult() == null ? 0 : context.getPersistResult().getResultCount())
                        + "，未排数量=" + (context.getPersistResult() == null ? 0 : context.getPersistResult().getUnplannedCount())
                        + "，异常数量=" + (context.getPersistResult() == null ? 0 : context.getPersistResult().getErrorCount());
            default:
                return stepEnum.getDesc();
        }
    }
}
