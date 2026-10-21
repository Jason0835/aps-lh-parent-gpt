package com.zlt.aps.lh.engine.strategy.support;
import com.zlt.aps.lh.api.domain.dto.MachineCleaningWindowDTO;
import lombok.Getter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
/**
 * 一次清洗评估的统一结果；实际事件与当前场景容量窗口分别返回，候选评估不发布运行态。
 *
 * @author APS
 * @since 2026/10/19
 */
public final class CleaningPlanResult {
    /**
     * 无清洗场景共享空结果，避免高频候选分配空对象。
     */
    private static final CleaningPlanResult EMPTY = new CleaningPlanResult(
            Collections.emptyList(), Collections.emptyList());
    /**
     * 获取无清洗结果。
     *
     * @return 共享不可变空结果
     */
    public static CleaningPlanResult empty() {
        return EMPTY;
    }
    /**
     * 每个来源计划及运行侧独立的清洗结论。
     */
    private final List<Event> events;
    /**
     * 仅供当前场景计算容量的窗口，与事件是否取消独立。
     */
    private final List<MachineCleaningWindowDTO> effectiveWindows;
    /**
     * 冻结事件及当前场景窗口容器；容量窗口只读，事件资源快照独立。
     *
     * @param events 各来源事件结论
     * @param effectiveWindows 当前场景容量窗口
     */
    public CleaningPlanResult(List<Event> events, List<MachineCleaningWindowDTO> effectiveWindows) {
        this.events = Collections.unmodifiableList(new ArrayList<>(events));
        // 容量消费者沿用只读窗口契约，仅隔离列表；完整深复制限于事件发布和回滚快照。
        this.effectiveWindows = effectiveWindows.isEmpty() ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(effectiveWindows));
    }
    /**
     * 获取不可变事件结论集合。
     *
     * @return 独立来源事件结论
     */
    public List<Event> getEvents() {
        return events;
    }
    /**
     * 获取当前场景可用窗口的独立容器；窗口字段遵守原只读契约。
     *
     * @return 容量窗口容器；为空不代表事件取消
     */
    public List<MachineCleaningWindowDTO> getEffectiveWindows() {
        return new ArrayList<>(effectiveWindows);
    }
    /**
     * 获取仍须执行的实际清洗事件，不受当前切换窗口过滤影响。
     *
     * @return 仍安排清洗的事件窗口副本
     */
    public List<MachineCleaningWindowDTO> getScheduledWindows() {
        return events.stream().filter(event -> event.getStatus() == CleaningPlanStatus.SCHEDULED)
                .map(Event::getWindow).collect(Collectors.toList());
    }
    /**
     * 一个来源计划、一个运行侧的只读处置和时间快照。
     */
    @Getter
    public static final class Event {
        /**
         * 来源计划主键与运行侧构成的事件身份。
         */
        private final String eventKey;
        /**
         * 显式处置状态。
         */
        private final CleaningPlanStatus status;
        /**
         * 处置原因，不以空窗口代替原因。
         */
        private final String reason;
        /**
         * 当前阶段冻结的来源与资源事实。
         */
        private final MachineCleaningWindowDTO window;
        /**
         * 清洗及恢复时间结果。
         */
        private final CleaningPlanTimeline timeline;
        /**
         * 冻结单侧事件结论。
         *
         * @param window 来源清洗事实
         * @param status 本阶段状态
         * @param reason 本阶段原因
         */
        public Event(MachineCleaningWindowDTO window, CleaningPlanStatus status, String reason) {
            this.window = CleaningPlanRuntimeState.copyWindow(window);
            this.eventKey = window.getSourcePlanId() + "|" + window.getLhCode();
            this.status = status;
            this.reason = reason;
            this.timeline = new CleaningPlanTimeline(window);
        }
        /**
         * 判断当前事实是否等于已冻结结论，避免重复复核时深复制窗口。
         *
         * @param current 当前事实
         * @param currentStatus 当前状态
         * @param currentReason 当前原因
         * @return 是否可以复用已发布的不可变结论
         */
        public boolean matches(MachineCleaningWindowDTO current, CleaningPlanStatus currentStatus,
                String currentReason) {
            return status == currentStatus && java.util.Objects.equals(reason, currentReason)
                    && java.util.Objects.equals(window, current);
        }
        /**
         * 获取独立的事件资源快照。
         *
         * @return 清洗事件窗口副本
         */
        public MachineCleaningWindowDTO getWindow() {
            return CleaningPlanRuntimeState.copyWindow(window);
        }
    }
}
