package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.enums.MouldChangeTypeEnum;
import com.zlt.aps.lh.api.enums.ShiftEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.PreviousAlternateMachineOrder;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 在续作收尾与时间下机之前统一初始化前日交替计划机台列表。
 * 最新批次和过期过滤复用已准备的历史资格，列表只发布排序事实。
 *
 * @author Codex
 * @date 2026/10/09
 */
@Slf4j
@Service
public class PreviousAlternateMachineOrderService {
    /**
     * 从本批已冻结资格按业务日期、班次稳定排序，再归并物理机台。
     * 同日期同班次保留资格列表原顺序，历史多条关系仅决定最早排名。
     *
     * @param context 已完成前日交替资格准备的批次上下文
     */
    public void initializePreviousAlternateMachineOrder(LhScheduleContext context) {
        Set<LocalDate> workDates = context.getScheduleWindowShifts().stream()
                .map(shift -> shift.getWorkDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                .collect(Collectors.toSet());
        List<LhMouldChangePlan> plans = context.getEligiblePreviousAlternatePlans().stream()
                .filter(Objects::nonNull)
                .filter(plan -> StringUtils.isNotEmpty(plan.getLhMachineCode()) && Objects.nonNull(plan.getPlanDate()))
                .filter(plan -> workDates.contains(this.planDay(plan)))
                .filter(plan -> ShiftEnum.MORNING_SHIFT.getCode().equals(plan.getClassIndex())
                        || ShiftEnum.AFTERNOON_SHIFT.getCode().equals(plan.getClassIndex()))
                .filter(plan -> MouldChangeTypeEnum.containsAnyCode(plan.getChangeMouldType(),
                        MouldChangeTypeEnum.REGULAR.getCode(), MouldChangeTypeEnum.TYPE_BLOCK.getCode()))
                .sorted(Comparator.comparing(this::planDay).thenComparing(LhMouldChangePlan::getClassIndex))
                .collect(Collectors.toList());
        List<String> machineCodes = plans.stream().map(LhMouldChangePlan::getLhMachineCode)
                .map(LhSingleControlMachineUtil::resolvePhysicalMachineCode).distinct().collect(Collectors.toList());
        // 歧义判断沿用完整有效关系集合，不能先过滤掉其他动作后把多条关系误认成唯一。
        Map<String, List<LhMouldChangePlan>> relations = context.getEligiblePreviousAlternatePlans().stream()
                .collect(Collectors.groupingBy(plan -> plan.getLhMachineCode() + "|" + plan.getBeforeMaterialCode(),
                        LinkedHashMap::new, Collectors.toList()));
        Map<String, LhMouldChangePlan> uniquePlans = new LinkedHashMap<>();
        relations.forEach((key, matches) -> {
            if (matches.size() == 1 && plans.contains(matches.get(0))) {
                uniquePlans.put(key, matches.get(0));
            }
        });
        context.setPreviousAlternateMachineOrder(new PreviousAlternateMachineOrder(machineCodes, uniquePlans));
        String detail = String.format("工厂=%s, 批次=%s, 目标日=%s, 机台顺序=%s, 来源计划=%s",
                context.getFactoryCode(), context.getBatchNo(), context.getScheduleTargetDate(), machineCodes,
                plans.stream().map(plan -> plan.getLhMachineCode() + ":" + this.planDay(plan)
                        + "/" + plan.getClassIndex() + "/" + plan.getId()).collect(Collectors.joining(",")));
        PriorityTraceLogHelper.appendProcessLog(context, "初始化前日交替计划机台列表", detail);
        log.info("初始化前日交替计划机台列表, {}", detail);
    }

    /**
     * 提取计划业务日期，避免当天的时分秒覆盖班次顺序。
     *
     * @param plan 日期完整的历史计划
     * @return 计划业务日期
     */
    private LocalDate planDay(LhMouldChangePlan plan) {
        return plan.getPlanDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
