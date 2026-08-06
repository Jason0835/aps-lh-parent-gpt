package com.zlt.aps.tc.engine.strategy;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.MachineOpenShiftCodeUtil;
import com.zlt.aps.common.engine.schedule.ScheduleRuleResult;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.enums.TcMachineFilterReasonEnum;
import com.zlt.aps.tc.api.enums.TcScheduleErrorCodeEnum;
import com.zlt.aps.tc.api.enums.TcScheduleStrategyEnum;
import com.zlt.aps.tc.engine.domain.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎侧默认机台过滤规则链。
 *
 * <p>按启用、机台开机班次、剩余产能、口型板、胶料机台关系、共用机台错班、定点生产、定点不可生产顺序执行，
 * 任一否决即过滤。方法会修改候选机台的过滤状态和证据，不修改任务链。
 * 通过 {@link Component} 注册为 Spring Bean，由 {@link TcStrategyRegistry}
 * 按 {@link TcScheduleStrategyEnum#DEFAULT} 编码收集。</p>
 */
@Component
public class TcDefaultMachineFilterRule implements ITcMachineFilterRule {

    private static final String RULE_MACHINE_STATUS = "MACHINE_STATUS";
    private static final String RULE_MACHINE_OPEN_SHIFT = "MACHINE_OPEN_SHIFT";
    private static final String RULE_REMAIN_CAPACITY = "REMAIN_CAPACITY";
    private static final String RULE_MOUTH_PLATE = "MOUTH_PLATE";
    private static final String RULE_GLUE_MACHINE = "GLUE_MACHINE";
    private static final String RULE_SHARED_MACHINE = "SHARED_MACHINE";
    private static final String RULE_FIXED_MACHINE = "FIXED_MACHINE";
    private static final String RULE_EXCLUDE_FIXED = "EXCLUDE_FIXED";

    /**
     * 获取规则编码。
     *
     * @return 规则编码
     */
    @Override
    public String getRuleCode() {
        return TcScheduleStrategyEnum.DEFAULT.getCode();
    }

    /**
     * 执行机台过滤规则。
     *
     * @param candidate 候选机台
     * @param context   机台规则上下文
     * @return 规则执行结果，passed=true 表示通过过滤
     * @throws ServiceException 候选机台或上下文为空时抛出
     */
    @Override
    public ScheduleRuleResult evaluate(TcMachineCandidate candidate, TcMachineRuleContext context) {
        return this.evaluateInternal(candidate, context, false);
    }

    /**
     * 执行不含剩余产能判断的静态机台过滤。
     *
     * @param candidate 候选机台
     * @param context   机台规则上下文
     * @return 规则执行结果，passed=true 表示静态硬约束通过
     */
    @Override
    public ScheduleRuleResult evaluateStatic(TcMachineCandidate candidate, TcMachineRuleContext context) {
        return this.evaluateInternal(candidate, context, true);
    }

    /**
     * 按指定过滤范围执行默认机台规则链。
     *
     * @param candidate 候选机台
     * @param context 机台规则上下文
     * @param staticOnly true 表示跳过当前班次剩余产能判断
     * @return 规则执行结果
     */
    private ScheduleRuleResult evaluateInternal(TcMachineCandidate candidate, TcMachineRuleContext context,
                                                boolean staticOnly) {
        if (candidate == null || context == null) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_MACHINE_CANDIDATE_EMPTY.getDefaultMessage());
        }
        TcTaskDraft task = context.getTaskDraft();
        List<String> ruleOrder = this.resolveRuleOrder(context);
        candidate.getEvidence().put(staticOnly ? "staticFilterRuleOrder" : "filterRuleOrder", ruleOrder);
        for (String ruleCode : ruleOrder) {
            if (staticOnly && RULE_REMAIN_CAPACITY.equals(ruleCode)) {
                candidate.getEvidence().put("staticFilterSkipped:" + ruleCode, Boolean.TRUE);
                continue;
            }
            if (!this.isRuleEnabled(context, ruleCode)) {
                candidate.getEvidence().put("filterRuleDisabled:" + ruleCode, Boolean.TRUE);
                continue;
            }
            TcMachineFilterReasonEnum rejectReason = this.evaluateRule(ruleCode, candidate, context);
            if (rejectReason != null) {
                return this.reject(candidate, rejectReason);
            }
        }
        candidate.setFiltered(Boolean.FALSE);
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("task", task == null ? null : task.getBusinessKey());
        evidence.put("machineCode", candidate.getMachineCode());
        candidate.getEvidence().putAll(evidence);
        return ScheduleRuleResult.pass(TcMachineFilterReasonEnum.DEFAULT_PASS.getCode(),
                TcMachineFilterReasonEnum.DEFAULT_PASS.getDesc(), evidence);
    }

    /**
     * 按规则编码执行单项机台过滤判断。
     *
     * @param ruleCode 过滤规则编码
     * @param candidate 候选机台
     * @param context 机台规则上下文
     * @return 不通过时返回未排原因，通过或未知规则返回 null
     */
    private TcMachineFilterReasonEnum evaluateRule(String ruleCode, TcMachineCandidate candidate,
                                                    TcMachineRuleContext context) {
        if (RULE_MACHINE_STATUS.equals(ruleCode) && Boolean.FALSE.equals(candidate.getEnabled())) {
            return TcMachineFilterReasonEnum.MACHINE_DISABLED;
        }
        if (RULE_MACHINE_OPEN_SHIFT.equals(ruleCode) && this.hasMachineOpenShiftConflict(candidate, context)) {
            return TcMachineFilterReasonEnum.MACHINE_SHIFT_NOT_OPEN;
        }
        if (RULE_REMAIN_CAPACITY.equals(ruleCode) && (candidate.getRemainCapacity() == null
                || candidate.getRemainCapacity().compareTo(BigDecimal.ZERO) <= 0)) {
            return TcMachineFilterReasonEnum.NO_REMAIN_CAPACITY;
        }
        if (RULE_MOUTH_PLATE.equals(ruleCode) && Boolean.FALSE.equals(candidate.getMouthPlateMatched())) {
            return TcMachineFilterReasonEnum.MOUTH_PLATE_NOT_MATCH;
        }
        if (RULE_GLUE_MACHINE.equals(ruleCode) && Boolean.FALSE.equals(candidate.getGlueMachineMatched())) {
            return TcMachineFilterReasonEnum.GLUE_MACHINE_NOT_MATCH;
        }
        if (RULE_SHARED_MACHINE.equals(ruleCode) && this.hasSharedShiftConflict(candidate, context)) {
            return TcMachineFilterReasonEnum.SHARED_MACHINE_SHIFT_NOT_MATCH;
        }
        if (RULE_FIXED_MACHINE.equals(ruleCode) && Boolean.FALSE.equals(candidate.getFixedMachineSelected())) {
            return TcMachineFilterReasonEnum.FIXED_MACHINE_NOT_SELECTED;
        }
        if (RULE_EXCLUDE_FIXED.equals(ruleCode) && Boolean.TRUE.equals(candidate.getFixedMachineExcluded())) {
            return TcMachineFilterReasonEnum.FIXED_MACHINE_EXCLUDED;
        }
        if (!this.isKnownRule(ruleCode)) {
            candidate.getEvidence().put("unknownFilterRule:" + ruleCode, Boolean.TRUE);
        }
        return null;
    }

    /**
     * 解析本次排程实际使用的过滤规则顺序。
     *
     * @param context 机台规则上下文
     * @return 去空并转为大写的过滤规则编码列表
     */
    private List<String> resolveRuleOrder(TcMachineRuleContext context) {
        String configuredRuleOrder = null;
        if (context.getScheduleContext() != null) {
            TcParamValue paramValue = context.getScheduleContext().getParamMap()
                    .get(TcScheduleConstants.PARAM_FILTER_RULE_ORDER);
            if (paramValue != null && paramValue.getEffectiveValue() != null
                    && !paramValue.getEffectiveValue().trim().isEmpty()) {
                configuredRuleOrder = paramValue.getEffectiveValue();
            }
        }
        List<String> defaultRuleOrder = Arrays.stream(TcScheduleConstants.DEFAULT_FILTER_RULE_ORDER.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(String::toUpperCase)
                .collect(Collectors.toList());
        if (configuredRuleOrder == null) {
            return defaultRuleOrder;
        }
        List<String> configuredRuleList = Arrays.stream(configuredRuleOrder.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(String::toUpperCase)
                .collect(Collectors.toList());
        Set<String> configuredRuleSet = new HashSet<>();
        for (String ruleCode : configuredRuleList) {
            if (!this.isKnownRule(ruleCode) || !configuredRuleSet.add(ruleCode)) {
                throw new ServiceException(MessageFormat.format(
                        I18nUtil.getMessage("ui.tc.schedule.filterRuleOrderInvalid"), ruleCode));
            }
        }
        List<String> resolvedRuleOrder = new ArrayList<>(configuredRuleList);
        defaultRuleOrder.stream()
                .filter(ruleCode -> !configuredRuleSet.contains(ruleCode))
                .forEach(resolvedRuleOrder::add);
        return this.normalizeHardRuleOrder(resolvedRuleOrder);
    }

    /**
     * 固定机台状态、开机班次和剩余产能三项硬约束的先后关系。
     *
     * @param resolvedRuleOrder 已合并默认项的规则顺序
     * @return 状态在前、开机班次居中、产能在后的规则顺序
     */
    private List<String> normalizeHardRuleOrder(List<String> resolvedRuleOrder) {
        List<String> normalizedRuleOrder = new ArrayList<>(resolvedRuleOrder);
        normalizedRuleOrder.remove(RULE_MACHINE_STATUS);
        normalizedRuleOrder.remove(RULE_MACHINE_OPEN_SHIFT);
        int remainCapacityIndex = normalizedRuleOrder.indexOf(RULE_REMAIN_CAPACITY);
        int insertIndex = remainCapacityIndex < 0 ? 0 : remainCapacityIndex;
        normalizedRuleOrder.add(insertIndex, RULE_MACHINE_STATUS);
        normalizedRuleOrder.add(insertIndex + 1, RULE_MACHINE_OPEN_SHIFT);
        return normalizedRuleOrder;
    }

    /**
     * 判断单项过滤规则是否启用，未配置时默认启用。
     *
     * @param context 机台规则上下文
     * @param ruleCode 过滤规则编码
     * @return true 表示执行该规则
     */
    private boolean isRuleEnabled(TcMachineRuleContext context, String ruleCode) {
        if (RULE_MACHINE_OPEN_SHIFT.equals(ruleCode)) {
            return true;
        }
        if (context.getScheduleContext() == null) {
            return true;
        }
        String paramCode = TcScheduleConstants.PARAM_FILTER_RULE_ENABLED_PREFIX
                + ruleCode + TcScheduleConstants.PARAM_FILTER_RULE_ENABLED_SUFFIX;
        TcParamValue paramValue = context.getScheduleContext().getParamMap().get(paramCode);
        return paramValue == null || !"0".equals(paramValue.getEffectiveValue());
    }

    /**
     * 判断是否为内置过滤规则编码。
     *
     * @param ruleCode 过滤规则编码
     * @return true 表示为内置规则
     */
    private boolean isKnownRule(String ruleCode) {
        return RULE_MACHINE_STATUS.equals(ruleCode)
                || RULE_MACHINE_OPEN_SHIFT.equals(ruleCode)
                || RULE_REMAIN_CAPACITY.equals(ruleCode)
                || RULE_MOUTH_PLATE.equals(ruleCode)
                || RULE_GLUE_MACHINE.equals(ruleCode)
                || RULE_SHARED_MACHINE.equals(ruleCode)
                || RULE_FIXED_MACHINE.equals(ruleCode)
                || RULE_EXCLUDE_FIXED.equals(ruleCode);
    }

    /**
     * 判断候选机台是否未开放当前任务班次。
     *
     * @param candidate 候选机台
     * @param context 机台规则上下文
     * @return true 表示机台当前班次未开机
     */
    private boolean hasMachineOpenShiftConflict(TcMachineCandidate candidate, TcMachineRuleContext context) {
        TcTaskDraft taskDraft = context.getTaskDraft();
        TcShiftTimeWindow shiftTimeWindow = taskDraft == null || context.getScheduleContext() == null
                ? null : context.getScheduleContext().getShiftTimeWindowMap().get(taskDraft.getShiftOrder());
        String currentShiftCode = shiftTimeWindow == null ? null : shiftTimeWindow.getShiftCode();
        Set<String> openShiftCodes = candidate.getOpenShiftCodes();
        candidate.getEvidence().put("shiftOrder", taskDraft == null ? null : taskDraft.getShiftOrder());
        candidate.getEvidence().put("shiftCode", currentShiftCode);
        candidate.getEvidence().put("machineOpenShiftCodes",
                openShiftCodes == null ? Collections.emptySet() : openShiftCodes);
        candidate.getEvidence().put("legacyOpenShiftCode",
                MachineOpenShiftCodeUtil.resolveLegacyOpenShiftCode(currentShiftCode));
        return !MachineOpenShiftCodeUtil.isMachineShiftOpen(openShiftCodes, currentShiftCode);
    }

    /**
     * 判断胎侧与垫胶共用机台是否发生班次冲突。
     *
     * @param candidate 候选机台
     * @param context 机台规则上下文
     * @return true 表示当前班次不能安排胎侧任务
     */
    private boolean hasSharedShiftConflict(TcMachineCandidate candidate, TcMachineRuleContext context) {
        if (!Boolean.TRUE.equals(candidate.getTcDjSharedMachine())
                || candidate.getAllowedTcShiftCodes() == null
                || candidate.getAllowedTcShiftCodes().isEmpty()
                || context.getTaskDraft() == null
                || context.getScheduleContext() == null) {
            return false;
        }
        com.zlt.aps.tc.engine.domain.TcShiftTimeWindow shiftTimeWindow = context.getScheduleContext()
                .getShiftTimeWindowMap().get(context.getTaskDraft().getShiftOrder());
        String currentShiftCode = shiftTimeWindow == null ? null : shiftTimeWindow.getShiftCode();
        candidate.getEvidence().put("tcDjSharedMachine", Boolean.TRUE);
        candidate.getEvidence().put("currentTcShiftCode", currentShiftCode);
        candidate.getEvidence().put("allowedTcShiftCodes", candidate.getAllowedTcShiftCodes());
        candidate.getEvidence().put("sharedDjShiftCodes", candidate.getSharedDjShiftCodes());
        return currentShiftCode == null || !candidate.getAllowedTcShiftCodes().contains(currentShiftCode);
    }

    /**
     * 标记候选机台被过滤并返回规则结果。
     *
     * @param candidate  候选机台
     * @param reason 过滤原因
     * @return 未通过过滤的规则结果
     */
    private ScheduleRuleResult reject(TcMachineCandidate candidate, TcMachineFilterReasonEnum reason) {
        candidate.setFiltered(Boolean.TRUE);
        candidate.setFilterReasonCode(reason.getCode());
        candidate.setFilterReasonDesc(reason.getDesc());
        candidate.getEvidence().put("ruleCode", reason.getCode());
        candidate.getEvidence().put("reasonDesc", reason.getDesc());
        return ScheduleRuleResult.reject(reason.getCode(), reason.getDesc(), candidate.getEvidence());
    }
}
