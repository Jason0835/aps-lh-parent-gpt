package com.zlt.aps.lh.util;

import com.zlt.aps.lh.api.domain.dto.MachineCleaningWindowDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhMachineOnlineInfo;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.enums.MouldChangeTypeEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.CleaningPlanRuntimeState;
import com.zlt.aps.lh.engine.strategy.support.PreviousAlternatePlanReleaseEvent;
import com.zlt.aps.mdm.api.domain.entity.MdmDevicePlanShut;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuMouldRel;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 历史换料卸下旧模后离线喷砂的公共身份和时间约束。
 * 事件保存在既有清洗来源快照中，不创建需求、不分配模具或修改生产账本。
 *
 * @author APS
 * @date 2026/11/15
 */
public final class HistoricalMouldCleaningTransferSupport {

    /**
     * 禁止实例化公共规则工具。
     */
    private HistoricalMouldCleaningTransferSupport() {
    }

    /**
     * 由明确停机模具、来源换料和异机指定接收三方证据冻结旧模身份。
     * 不因当前在机已经是后料而改用后料模具，也不从模具备注猜测物料。
     *
     * @param context 本批已加载的历史计划及物料模具关系
     * @param stopPlan 明确指定旧模的设备停机计划
     * @param window 已按原喷砂名额及参数计算的窗口
     */
    public static void initializeSource(LhScheduleContext context, MdmDevicePlanShut stopPlan,
            MachineCleaningWindowDTO window) {
        if (!MachineCleaningOverlapUtil.isSandBlastCleaning(window)
                || StringUtils.isBlank(stopPlan.getInMachineMouldCode())) {
            return;
        }
        Set<String> moulds = LhMouldCodeUtil.splitMouldCodeBySetSeparator(stopPlan.getInMachineMouldCode());
        List<LhMouldChangePlan> sources = context.getHistoricalReverseMouldChangePlanList().stream()
                .filter(Objects::nonNull)
                .filter(plan -> StringUtils.equals(plan.getLhMachineCode(), window.getLhCode()))
                .filter(plan -> MouldChangeTypeEnum.REGULAR.getCode().equals(plan.getChangeMouldType()))
                .filter(plan -> StringUtils.isNotBlank(plan.getBeforeMaterialCode())
                        && !StringUtils.equals(plan.getBeforeMaterialCode(), plan.getAfterMaterialCode()))
                .filter(plan -> plan.getPlanDate() != null && LhScheduleTimeUtil.clearTime(plan.getPlanDate())
                        .equals(LhScheduleTimeUtil.clearTime(window.getCleanStartTime())))
                .filter(plan -> context.getScheduleWindowShifts().stream()
                        .anyMatch(shift -> Objects.equals(shift.getWorkDate(),
                                LhScheduleTimeUtil.clearTime(plan.getPlanDate()))
                                && shift.resolveShiftTypeEnum() != null
                                && StringUtils.equals(shift.resolveShiftTypeEnum().getCode(), plan.getClassIndex())
                                && !shift.getShiftStartDateTime().after(window.getCleanStartTime())))
                .filter(plan -> relationMoulds(context, plan.getBeforeMaterialCode()).containsAll(moulds))
                .filter(plan -> context.getHistoricalReverseMouldChangePlanList().stream()
                        .anyMatch(receiver -> receiver != null && receiver.getPlanDate() != null
                                && !receiver.getPlanDate().before(
                                LhScheduleTimeUtil.clearTime(window.getCleanStartTime()))
                                && !StringUtils.equals(receiver.getLhMachineCode(), window.getLhCode())
                                && StringUtils.equals(receiver.getAfterMaterialCode(), plan.getBeforeMaterialCode())
                                && plannedMoulds(context, receiver).equals(moulds)))
                .collect(Collectors.toList());
        if (sources.size() != 1 || sources.get(0).getId() == null) {
            return;
        }
        LhMouldChangePlan source = sources.get(0);
        List<String> statuses = context.getHistoricalReverseMouldChangePlanList().stream()
                .filter(receiver -> receiver != null
                        && StringUtils.equals(receiver.getAfterMaterialCode(), source.getBeforeMaterialCode())
                        && plannedMoulds(context, receiver).equals(moulds))
                .map(LhMouldChangePlan::getProductStatus).filter(StringUtils::isNotBlank)
                .distinct().collect(Collectors.toList());
        if (statuses.size() != 1) {
            return;
        }
        window.setSourceProductStatus(statuses.get(0));
        window.setHistoricalSourceChangePlanId(source.getId());
        window.setHistoricalSourceAfterMaterialCode(source.getAfterMaterialCode());
        window.setSourceMaterialCode(source.getBeforeMaterialCode());
        window.setContinuationMaterialCode(source.getBeforeMaterialCode());
        window.setContinuationProductStatus(window.getSourceProductStatus());
        window.setMouldCode(LhMouldCodeUtil.joinMouldCode(moulds));
        window.setOffMachineCleaning(isDetached(context, window));
    }

    /**
     * 核实旧模已由在机快照或本轮真实正量换料卸下；历史意向本身不证明离机。
     *
     * @param context 当前在机证据及已提交结果
     * @param source 清洗来源窗口
     * @return 旧模已在清洗开始前离机时返回 true
     */
    public static boolean isDetached(LhScheduleContext context, MachineCleaningWindowDTO source) {
        if (source == null || source.getHistoricalSourceChangePlanId() == null) {
            return false;
        }
        LhMachineOnlineInfo online = context.getMachineOnlineInfoMap().get(source.getLhCode());
        if (online != null && online.getOnlineDate() != null
                && !online.getOnlineDate().after(source.getCleanStartTime())
                && StringUtils.equals(online.getMaterialCode(), source.getHistoricalSourceAfterMaterialCode())
                && StringUtils.isNotBlank(online.getInMachineMouldCode())
                && Collections.disjoint(LhMouldCodeUtil.splitMouldCode(source.getMouldCode()),
                        LhMouldCodeUtil.splitMouldCode(online.getInMachineMouldCode()))) {
            return true;
        }
        return context.getPreviousAlternateResultPlanMap().entrySet().stream()
                .anyMatch(entry -> Objects.equals(source.getHistoricalSourceChangePlanId(), entry.getValue().getId())
                        && entry.getKey().getDailyPlanQty() != null && entry.getKey().getDailyPlanQty() > 0
                        && "1".equals(entry.getKey().getIsChangeMould())
                        && !"1".equals(entry.getKey().getIsTypeBlock())
                        && entry.getKey().getMouldChangeStartTime() != null
                        && !entry.getKey().getMouldChangeStartTime().after(source.getCleanStartTime()));
    }

    /**
     * 读取来源快照中的历史旧模清洗事件，不依赖已被移除的机台停机窗口。
     *
     * @param context 清洗来源运行态
     * @return 本批已冻结的历史旧模事件流
     */
    public static Stream<MachineCleaningWindowDTO> sources(LhScheduleContext context) {
        return context.getCleaningPlanRuntimeState().getSourceWindowMap().values().stream()
                .flatMap(List::stream).filter(source -> source.getHistoricalSourceChangePlanId() != null);
    }

    /**
     * 返回精确历史接收事件，同料的其他模具或普通补机不继承专用首检。
     *
     * @param context 当前历史承接作用域
     * @param sku 本次接收物料状态
     * @param machineCode 接收机台
     * @return 已离机且整组指定匹配的来源，无匹配时为空
     */
    public static MachineCleaningWindowDTO resolveReceiving(LhScheduleContext context,
            SkuScheduleDTO sku, String machineCode) {
        if (context == null || sku == null || !context.isPreviousAlternateAction(sku, machineCode)) {
            return null;
        }
        PreviousAlternatePlanReleaseEvent event = context.getActivePreviousAlternateEvent();
        MachineCleaningWindowDTO source = resolveReceivingSource(context, event.getPlan());
        return source != null && isDetached(context, source)
                && StringUtils.equals(source.getSourceProductStatus(), sku.getProductStatus()) ? source : null;
    }

    /**
     * 通过历史计划的精确整组模具解析来源，未离机时仍保护指定组而不替换其他模具。
     *
     * @param context 清洗来源及模具关系
     * @param plan 当前历史接收计划
     * @return 唯一来源；无匹配或多来源歧义时为空
     */
    public static MachineCleaningWindowDTO resolveReceivingSource(LhScheduleContext context, LhMouldChangePlan plan) {
        Set<String> moulds = plannedMoulds(context, plan);
        List<MachineCleaningWindowDTO> matches = sources(context)
                .filter(source -> !StringUtils.equals(source.getLhCode(), plan.getLhMachineCode())
                        && StringUtils.equals(source.getSourceMaterialCode(), plan.getAfterMaterialCode())
                        && !moulds.isEmpty() && moulds.equals(LhMouldCodeUtil.splitMouldCode(source.getMouldCode())))
                .collect(Collectors.toList());
        return matches.size() == 1 ? matches.get(0) : null;
    }

    /**
     * 取得包含离线清洗的真实模具释放下限，防止机台换模取消覆盖旧模清洗边界。
     *
     * @param context 既有模具释放账本和清洗来源
     * @param mouldCode 具体模具编码
     * @return 最晚有效释放时间，没有约束时为空
     */
    public static Date resolveMouldReleaseTime(LhScheduleContext context, String mouldCode) {
        Date cleaningEnd = sources(context)
                .filter(source -> LhMouldCodeUtil.splitMouldCode(source.getMouldCode()).contains(mouldCode))
                .map(MachineCleaningWindowDTO::getCleanEndTime).filter(Objects::nonNull)
                .max(Date::compareTo).orElse(null);
        Date previous = context.getPreScheduledMouldReleaseTimeMap().get(mouldCode);
        if (previous == null || cleaningEnd != null && cleaningEnd.after(previous)) {
            return cleaningEnd;
        }
        return previous;
    }

    /**
     * 解析历史指定模具，文本说明不参与模具身份，只有有效关系中的完整编码参与匹配。
     *
     * @param context 本批物料模具关系
     * @param plan 历史交替计划
     * @return 指定的有效关系模具集合
     */
    public static Set<String> plannedMoulds(LhScheduleContext context, LhMouldChangePlan plan) {
        if (StringUtils.isBlank(plan.getMouldCode())) {
            return Collections.emptySet();
        }
        Set<String> relations = relationMoulds(context, plan.getAfterMaterialCode());
        return Arrays.stream(StringUtils.split(plan.getMouldCode(), ",，/ \t\r\n"))
                .filter(relations::contains).collect(Collectors.toSet());
    }

    /**
     * 获取物料已有模具关系，用于身份验证，不代替台账、占用和完整组资源校验。
     *
     * @param context 关系快照
     * @param materialCode 物料编码
     * @return 已定义的模具编码集合
     */
    private static Set<String> relationMoulds(LhScheduleContext context, String materialCode) {
        return context.getSkuMouldRelMap().getOrDefault(materialCode, Collections.emptyList()).stream()
                .filter(Objects::nonNull).map(MdmSkuMouldRel::getMouldCode)
                .filter(StringUtils::isNotBlank).collect(Collectors.toSet());
    }

    /**
     * 将已离机来源投影为输出事件，保留快照原值供失败预演恢复。
     *
     * @param context 本批真实离机证据
     * @param source 原来源窗口
     * @return 独立离机事件副本，未离机时返回原对象
     */
    public static MachineCleaningWindowDTO projectDetached(LhScheduleContext context,
            MachineCleaningWindowDTO source) {
        if (!isDetached(context, source)) {
            return source;
        }
        MachineCleaningWindowDTO event = CleaningPlanRuntimeState.copyWindow(source);
        event.setOffMachineCleaning(true);
        return event;
    }

    /**
     * 精确旧模接收的机台准备仅承接喷砂首检，不再次串行叠加普通换模总时长。
     *
     * @param context 当前历史接收作用域
     * @param sku 接收物料
     * @param machineCode 接收机台
     * @param ordinaryHours 原换模时长
     * @return 喷砂首检小时数或原时长
     */
    public static int resolvePreparationHours(LhScheduleContext context, SkuScheduleDTO sku,
            String machineCode, int ordinaryHours) {
        MachineCleaningWindowDTO source = resolveReceiving(context, sku, machineCode);
        return source == null ? ordinaryHours
                : Math.toIntExact((source.getReadyTime().getTime() - source.getCleanEndTime().getTime())
                        / (60L * 60L * 1000L));
    }
}
