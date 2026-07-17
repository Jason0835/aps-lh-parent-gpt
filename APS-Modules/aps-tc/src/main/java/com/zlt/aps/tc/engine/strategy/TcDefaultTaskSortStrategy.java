package com.zlt.aps.tc.engine.strategy;

import com.zlt.aps.tc.api.enums.TcScheduleStrategyEnum;
import com.zlt.aps.tc.engine.domain.TcScheduleContext;
import com.zlt.aps.tc.engine.domain.TcTaskDraft;
import com.zlt.aps.tc.engine.util.TcGlueSimilarityUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * 胎侧默认待排任务排序策略。
 *
 * <p>按多级维度构建比较器，保证库存紧急的规格优先排产，同胶料连续生产减少切换。
 * 排序维度（优先级从高到低）：
 * <ol>
 *   <li>可供成型班次分组：按班次从早到晚排序</li>
 *   <li>最晚开始时间：同一班次内越早越优先，未计算时保持既有规则</li>
 *   <li>库存紧急度：supplyHours 越小越优先</li>
 *   <li>主胶料分组：同一班次内按主胶料分组</li>
 *   <li>基部胶相似度：基部胶相同个数越多优先级越高</li>
 *   <li>同种胶料内供应时长：同一种胶料内按 supplyHours 从小到大排序</li>
 *   <li>口型聚集：同种预口型尽量安排在一起生产</li>
 *   <li>稳定兜底：按 businessKey 升序，保证相同输入重复运行结果一致</li>
 * </ol>
 * 通过 {@link Component} 注册为 Spring Bean，由 {@link TcStrategyRegistry}
 * 按 {@link TcScheduleStrategyEnum#DEFAULT} 编码收集。</p>
 */
@Component
public class TcDefaultTaskSortStrategy implements ITcTaskSortStrategy {

    /**
     * 获取策略编码。
     *
     * @return 策略编码
     */
    @Override
    public String getStrategyCode() {
        return TcScheduleStrategyEnum.DEFAULT.getCode();
    }

    /**
     * 构建多级任务排序比较器。
     *
     * @param context 胎侧排程上下文
     * @return 多级比较器
     */
    @Override
    public Comparator<TcTaskDraft> buildComparator(TcScheduleContext context) {
        Map<String, BigDecimal> glueGroupEarliestSupplyMap = this.buildGlueGroupEarliestSupplyMap(context);
        Map<TcTaskDraft, Integer> baseGlueSimilarityMap = this.buildBaseGlueSimilarityMap(context);
        return Comparator
                // 1. 可供成型班次分组：按班次从早到晚排序
                .comparing(TcTaskDraft::getShiftOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                // 2. 同一班次内最晚开始时间越早越优先；无法计算时落到既有排序规则。
                .thenComparing(TcTaskDraft::getLatestStartTime,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                // 3-4. 库存紧急度和主胶料分组：按同班次胶料组最早供应时长排序，同胶料聚在一起
                .thenComparing(task -> this.resolveGlueGroupEarliestSupply(task, glueGroupEarliestSupplyMap),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(TcTaskDraft::getGlueCode,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                // 4. 基部胶相似度：同班次基部胶相同个数越多优先级越高
                .thenComparing(task -> this.resolveBaseGlueSimilarity(task, baseGlueSimilarityMap),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(TcTaskDraft::getBaseGlueCode,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                // 5. 同种胶料内供应时长：同一种胶料内按 supplyHours 从小到大排序
                .thenComparing(TcTaskDraft::getSupplyHours,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                // 6. 口型聚集：同种预口型尽量安排在一起生产
                .thenComparing(TcTaskDraft::getMouthPlateCode,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                // 7. 稳定兜底：按 businessKey 升序，保证相同输入重复运行结果一致
                .thenComparing(TcTaskDraft::getBusinessKey,
                        Comparator.nullsLast(Comparator.naturalOrder()));
    }

    /**
     * 构建同一班次、同一主胶料组内的最早供应时长。
     *
     * @param context 胎侧排程上下文
     * @return 班次和胶料组合 -> 最早供应时长
     */
    private Map<String, BigDecimal> buildGlueGroupEarliestSupplyMap(TcScheduleContext context) {
        Map<String, BigDecimal> groupMap = new HashMap<>();
        if (context == null || context.getTaskDraftList() == null) {
            return groupMap;
        }
        for (TcTaskDraft task : context.getTaskDraftList()) {
            if (task == null || task.getSupplyHours() == null) {
                continue;
            }
            String key = this.buildShiftGroupKey(task.getShiftOrder(), task.getGlueCode());
            BigDecimal existing = groupMap.get(key);
            if (existing == null || task.getSupplyHours().compareTo(existing) < 0) {
                groupMap.put(key, task.getSupplyHours());
            }
        }
        return groupMap;
    }

    /**
     * 构建同一班次内基部胶元素交集累计分，用于表达基部胶相似度。
     *
     * @param context 胎侧排程上下文
     * @return 任务草稿 -> 同班次基部胶交集累计数量
     */
    private Map<TcTaskDraft, Integer> buildBaseGlueSimilarityMap(TcScheduleContext context) {
        Map<TcTaskDraft, Integer> similarityMap = new IdentityHashMap<>();
        if (context == null || context.getTaskDraftList() == null) {
            return similarityMap;
        }
        List<TcTaskDraft> taskDraftList = context.getTaskDraftList();
        for (TcTaskDraft task : taskDraftList) {
            Set<String> baseGlueSet = TcGlueSimilarityUtils.parseCodeSet(
                    task == null ? null : task.getBaseGlueCode());
            if (baseGlueSet.isEmpty()) {
                continue;
            }
            int similarityScore = 0;
            for (TcTaskDraft otherTask : taskDraftList) {
                if (task == otherTask || otherTask == null
                        || !Objects.equals(task.getShiftOrder(), otherTask.getShiftOrder())) {
                    continue;
                }
                similarityScore += TcGlueSimilarityUtils.calculateIntersectionCount(baseGlueSet,
                        TcGlueSimilarityUtils.parseCodeSet(otherTask.getBaseGlueCode()));
            }
            similarityMap.put(task, similarityScore);
        }
        return similarityMap;
    }

    /**
     * 读取任务所属胶料组的最早供应时长。
     *
     * @param task 任务草稿
     * @param groupMap 胶料组供应时长映射
     * @return 供应时长
     */
    private BigDecimal resolveGlueGroupEarliestSupply(TcTaskDraft task, Map<String, BigDecimal> groupMap) {
        if (task == null) {
            return null;
        }
        return groupMap.get(this.buildShiftGroupKey(task.getShiftOrder(), task.getGlueCode()));
    }

    /**
     * 读取任务基部胶相似度分值。
     *
     * @param task 任务草稿
     * @param similarityMap 基部胶相似度映射
     * @return 相似度分值
     */
    private Integer resolveBaseGlueSimilarity(TcTaskDraft task, Map<TcTaskDraft, Integer> similarityMap) {
        if (task == null || TcGlueSimilarityUtils.parseCodeSet(task.getBaseGlueCode()).isEmpty()) {
            return null;
        }
        return similarityMap.get(task);
    }

    /**
     * 构造班次维度分组键。
     *
     * @param shiftOrder 班次顺序
     * @param code 分组编码
     * @return 分组键
     */
    private String buildShiftGroupKey(Integer shiftOrder, String code) {
        return shiftOrder + "|" + (code == null ? "" : code);
    }
}
