package com.zlt.aps.lh.engine.strategy.support;
import cn.hutool.core.bean.BeanUtil;
import com.zlt.aps.lh.api.domain.dto.CleaningScheduleDateFillItem;
import com.zlt.aps.lh.api.domain.dto.MachineCleaningWindowDTO;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.IdentityHashMap;
import java.util.stream.Collectors;
/**
 * 当前批次独立的清洗运行态，接管原清洗回填、喷砂交接和回流余量容器。
 * 上下文原有访问器委托本对象，不维护第二套清洗账本。
 *
 * @author APS
 * @since 2026/10/19
 */
@Getter
@Setter
public final class CleaningPlanRuntimeState {
    /**
     * 清洗日期回填候选，成功安排及初始化收尾跳过分别保留实际清洗日或收尾业务日。
     * 配对侧不重复回填，最终由结果落库事务按来源主键去重；取消清洗不自动删除回填项。
     */
    private List<CleaningScheduleDateFillItem> scheduleDateFillList = new ArrayList<>();
    /**
     * 已通过来源和日期校验的归一清洗窗口，按运行侧索引；独立执行额度另行保存。
     * 初始化收尾跳过仍保留事实；候选只投影副本，不能将后料的切换时刻写回来源。
     */
    private final Map<String, List<MachineCleaningWindowDTO>> sourceWindowMap = new LinkedHashMap<>();
    /**
     * 独立清洗已取得名额的实际班次起点，键为来源主键和运行侧。
     * 正规换模仅覆盖来源、不生成独立清洗；换活字块保留清洗时必须使用本名额。
     */
    private final Map<String, Date> independentQuotaStartMap = new LinkedHashMap<>();
    /**
     * 仅喷砂实际释放机台后的来源事件，保存旧SKU及机台从清洗开始释放、原模具到清洗结束
     * 才可复用以及真实余量回流三项事实；喷砂与其他停机重叠时不登记该专用处置。
     */
    private Map<String, MachineCleaningWindowDTO> releaseWindowMap = new LinkedHashMap<>();
    /**
     * 喷砂置换或离机事件，保留被移出机台容量列表的清洗事实。
     */
    private Map<String, MachineCleaningWindowDTO> sandBlastWindowMap = new LinkedHashMap<>();
    /**
     * 同SKU置换已占用的统一换模额度，键包含物理机台、来源、实际开始时间及SKU状态。
     * 同源L/R共用一个键，取消时移除身份后只回退一次；与清洗事件一同进入候选快照。
     */
    private Map<String, Date> replacementQuotaReservationMap = new LinkedHashMap<>();
    /**
     * 仅喷砂释放后的真实回流余量，数量仍由生产策略维护。
     */
    private Map<String, Integer> remainingQtyMap = new LinkedHashMap<>();
    /**
     * 已发布的各来源运行侧处置；只读候选不写入。
     */
    private final Map<String, CleaningPlanResult.Event> appliedEventMap = new LinkedHashMap<>();
    /**
     * 正式约束变更版本，供候选、容量及时间缓存识别。
     */
    private long constraintVersion;
    /**
     * 发布实际事件结论；相同事实重复复核不递增版本。
     *
     * @param event 基于实际清洗事实和正式结果的结论
     * @return 事件资源或处置是否变化
     */
    public boolean apply(CleaningPlanResult.Event event) {
        CleaningPlanResult.Event previous = appliedEventMap.get(event.getEventKey());
        if (previous == event || previous != null
                && previous.matches(event.getWindow(), event.getStatus(), event.getReason())) {
            return false;
        }
        appliedEventMap.put(event.getEventKey(), event);
        constraintVersion++;
        return true;
    }
    /**
     * 复制清洗运行态，失败尝试和班次9副本均不能回写来源事件。
     *
     * @return 独立运行态副本
     */
    public CleaningPlanRuntimeState copy() {
        CleaningPlanRuntimeState target = new CleaningPlanRuntimeState();
        target.scheduleDateFillList = scheduleDateFillList.stream()
                .map(item -> BeanUtil.copyProperties(item, CleaningScheduleDateFillItem.class))
                .collect(Collectors.toCollection(ArrayList::new));
        target.scheduleDateFillList.forEach(item -> item.setScheduleDate(copyDate(item.getScheduleDate())));
        Map<MachineCleaningWindowDTO, MachineCleaningWindowDTO> copies = new IdentityHashMap<>();
        sourceWindowMap.forEach((machineCode, windows) ->
                target.sourceWindowMap.put(machineCode, copyWindows(windows)));
        independentQuotaStartMap.forEach((key, time) -> target.independentQuotaStartMap.put(key, copyDate(time)));
        sandBlastWindowMap.forEach((key, window) -> target.sandBlastWindowMap.put(key,
                copies.computeIfAbsent(window, CleaningPlanRuntimeState::copyWindow)));
        releaseWindowMap.forEach((key, window) -> target.releaseWindowMap.put(key,
                copies.computeIfAbsent(window, CleaningPlanRuntimeState::copyWindow)));
        replacementQuotaReservationMap.forEach((key, time) ->
                target.replacementQuotaReservationMap.put(key, copyDate(time)));
        target.remainingQtyMap.putAll(remainingQtyMap);
        target.appliedEventMap.putAll(appliedEventMap);
        target.constraintVersion = constraintVersion;
        return target;
    }
    /**
     * 复制窗口和其中可变日期、原模具释放快照。
     *
     * @param source 来源窗口
     * @return 独立窗口；来源为空时返回空
     */
    public static MachineCleaningWindowDTO copyWindow(MachineCleaningWindowDTO source) {
        if (source == null) {
            return null;
        }
        MachineCleaningWindowDTO target = BeanUtil.copyProperties(source, MachineCleaningWindowDTO.class);
        target.setCleanStartTime(copyDate(source.getCleanStartTime()));
        target.setCleanEndTime(copyDate(source.getCleanEndTime()));
        target.setReadyTime(copyDate(source.getReadyTime()));
        target.setSandBlastInspectionStartTime(copyDate(source.getSandBlastInspectionStartTime()));
        target.setReplacementInspectionStartTime(copyDate(source.getReplacementInspectionStartTime()));
        target.setReplacementReadyTime(copyDate(source.getReplacementReadyTime()));
        target.setReplacementInspectionShiftStartTime(copyDate(source.getReplacementInspectionShiftStartTime()));
        target.setSourcePlanStartTime(copyDate(source.getSourcePlanStartTime()));
        target.setSourcePlanEndTime(copyDate(source.getSourcePlanEndTime()));
        target.setSandBlastInspectionShiftStartTime(copyDate(source.getSandBlastInspectionShiftStartTime()));
        Map<String, Date> releases = new LinkedHashMap<>();
        if (source.getMouldReleaseTimeBeforeReplacement() != null) {
            source.getMouldReleaseTimeBeforeReplacement().forEach((key, time) -> releases.put(key, copyDate(time)));
        }
        target.setMouldReleaseTimeBeforeReplacement(releases);
        return target;
    }
    /**
     * 深复制机台窗口容器。
     *
     * @param windows 来源清洗窗口
     * @return 可独立变更的窗口集合
     */
    public static List<MachineCleaningWindowDTO> copyWindows(List<MachineCleaningWindowDTO> windows) {
        return windows == null ? new ArrayList<>() : windows.stream()
                .map(CleaningPlanRuntimeState::copyWindow).collect(Collectors.toCollection(ArrayList::new));
    }
    /**
     * 复制日期值并保留JDBC返回日期的实际类型，保持恢复后的原等值契约。
     *
     * @param time 来源日期
     * @return 日期副本
     */
    private static Date copyDate(Date time) {
        return time == null ? null : (Date) time.clone();
    }
}
