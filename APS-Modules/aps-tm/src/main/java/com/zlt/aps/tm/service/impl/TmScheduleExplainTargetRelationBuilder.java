package com.zlt.aps.tm.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.engine.quantity.PlanQuantityAllocationItem;
import com.zlt.aps.common.engine.quantity.PlanQuantityAllocationUtils;
import com.zlt.aps.tm.api.constant.TmScheduleConstants;
import com.zlt.aps.tm.api.domain.entity.TmScheduleExplainTargetRel;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResultExplain;
import com.zlt.aps.tm.engine.domain.TmPlanTaskGroup;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.*;

/**
 * 胎面来源解释与最终目标关联构建器。
 *
 * <p>本类只把已完成落库主键回填的结果片段、未排片段与来源解释转换为关联记录；
 * 不访问数据库、不保存数据，也不改变持久化服务的事务和写入顺序。</p>
 */
final class TmScheduleExplainTargetRelationBuilder {

    /**
     * 构建来源解释与结果或未排片段的关联记录。
     *
     * @param context 排程上下文
     * @param explainList 已保存解释列表
     * @param resultIdMap 实际片段业务键与结果主键映射
     * @param unplannedIdMap 实际片段业务键与未排主键映射
     * @param planGroupFragmentMap 正计划量片段索引
     * @return 按原计划组、片段和来源权重顺序生成的关联记录
     * @throws ServiceException 正计划量片段无法定位最终目标时抛出
     */
    List<TmScheduleExplainTargetRel> build(TmScheduleContext context, List<TmScheduleResultExplain> explainList,
                                           Map<String, Long> resultIdMap, Map<String, Long> unplannedIdMap,
                                           Map<String, List<TmTaskDraft>> planGroupFragmentMap) {
        Map<String, TmScheduleResultExplain> explainMap = explainList.stream()
                .collect(java.util.stream.Collectors.toMap(TmScheduleResultExplain::getTaskBusinessKey,
                        explain -> explain, (first, second) -> first, LinkedHashMap::new));
        List<TmScheduleExplainTargetRel> relationList = new ArrayList<>();
        for (TmPlanTaskGroup taskGroup : context.getPlanTaskGroupMap().values()) {
            List<TmTaskDraft> fragmentList = planGroupFragmentMap.getOrDefault(
                    taskGroup.getPlanGroupKey(), Collections.emptyList());
            for (TmTaskDraft fragment : fragmentList) {
                Map<String, BigDecimal> allocationMap = this.allocateByWeight(
                        fragment.getPlanQty(), taskGroup.getSourceWeightMap());
                for (Map.Entry<String, BigDecimal> allocationEntry : allocationMap.entrySet()) {
                    if (!this.isPositiveQty(allocationEntry.getValue())) {
                        continue;
                    }
                    TmScheduleResultExplain explain = explainMap.get(allocationEntry.getKey());
                    if (explain == null) {
                        continue;
                    }
                    relationList.add(this.buildRelation(context, taskGroup, fragment, allocationEntry,
                            explain, resultIdMap, unplannedIdMap));
                }
            }
        }
        return relationList;
    }

    /**
     * 根据单个计划片段和来源分摊量创建关联实体。
     *
     * @param context 排程上下文
     * @param taskGroup 计划汇总组
     * @param fragment 最终计划片段
     * @param allocationEntry 来源分摊量
     * @param explain 来源解释记录
     * @param resultIdMap 结果主键映射
     * @param unplannedIdMap 未排主键映射
     * @return 已填充目标主键的关联实体
     */
    private TmScheduleExplainTargetRel buildRelation(TmScheduleContext context, TmPlanTaskGroup taskGroup,
                                                     TmTaskDraft fragment,
                                                     Map.Entry<String, BigDecimal> allocationEntry,
                                                     TmScheduleResultExplain explain,
                                                     Map<String, Long> resultIdMap,
                                                     Map<String, Long> unplannedIdMap) {
        boolean unplanned = fragment.isUnassigned() && this.isPositiveQty(fragment.getPlanQty());
        Long targetId = unplanned ? unplannedIdMap.get(fragment.getBusinessKey())
                : resultIdMap.get(fragment.getBusinessKey());
        if (targetId == null) {
            throw new ServiceException(MessageFormat.format(
                    I18nUtil.getMessage("ui.tm.schedule.explainTargetMissing"), fragment.getBusinessKey()));
        }
        TmScheduleExplainTargetRel relation = new TmScheduleExplainTargetRel();
        relation.setFactoryCode(context.getFactoryCode());
        relation.setBatchNo(context.getBatchNo());
        relation.setScheduleDate(context.getScheduleDate());
        relation.setExplainId(explain.getId());
        relation.setPlanGroupKey(taskGroup.getPlanGroupKey());
        relation.setSourceTaskBusinessKey(allocationEntry.getKey());
        relation.setTargetType(unplanned ? "UNPLANNED" : "RESULT");
        relation.setTargetId(targetId);
        relation.setTargetBusinessKey(fragment.getBusinessKey());
        relation.setShiftOrder(fragment.getShiftOrder());
        relation.setMachineCode(fragment.getMachineCode());
        relation.setAllocatedQty(allocationEntry.getValue());
        return relation;
    }

    /**
     * 按来源权重分摊指定数量。
     *
     * @param totalQty 汇总数量
     * @param sourceWeightMap 来源权重
     * @return 来源业务键与分摊数量映射
     */
    private Map<String, BigDecimal> allocateByWeight(BigDecimal totalQty,
                                                      Map<String, BigDecimal> sourceWeightMap) {
        List<PlanQuantityAllocationItem> allocationItemList = sourceWeightMap.entrySet().stream()
                .map(entry -> new PlanQuantityAllocationItem(entry.getKey(), entry.getValue(), BigDecimal.ZERO))
                .collect(java.util.stream.Collectors.toList());
        return PlanQuantityAllocationUtils.allocate(totalQty, allocationItemList,
                        TmScheduleConstants.DECIMAL_CALCULATION_SCALE).stream()
                .collect(java.util.stream.Collectors.toMap(PlanQuantityAllocationItem::getSourceBusinessKey,
                        PlanQuantityAllocationItem::getAllocatedQty,
                        BigDecimal::add, LinkedHashMap::new));
    }

    /**
     * 判断计划量是否大于零。
     *
     * @param planQty 计划量
     * @return true 表示计划量大于零
     */
    private boolean isPositiveQty(BigDecimal planQty) {
        return planQty != null && planQty.compareTo(BigDecimal.ZERO) > 0;
    }
}
