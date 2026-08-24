package com.zlt.aps.tc.engine.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.zlt.aps.tc.api.enums.TcYesNoEnum;
import com.zlt.aps.tc.engine.domain.TcTaskDraft;
import com.zlt.aps.tc.engine.service.ITcPlanTailDecisionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 基于现有收尾标识和收尾余量的胎侧兼容判定实现。
 *
 * <p>TODO：月计划余量来源表确定后，改为按工厂、日期、胎侧代码查询；
 * 余量大于零为非收尾，余量小于等于零为收尾。</p>
 */
@Service
@Slf4j
public class TcLegacyPlanTailDecisionService implements ITcPlanTailDecisionService {

    /**
     * 汇总来源任务的收尾信息。
     *
     * <p>收尾标识由计划量汇总前的生产属性校验保证一致；同一胎侧计划组内的成型余量
     * 代表同一成型余量，按来源工单和业务键稳定排序后取一条非空值，不能重复累加。</p>
     *
     * @param aggregateTask 汇总生产任务
     * @param sourceTaskList 原始来源任务
     */
    @Override
    public void applyTailDecision(TcTaskDraft aggregateTask, List<TcTaskDraft> sourceTaskList) {
        if (aggregateTask == null || CollUtil.isEmpty(sourceTaskList)) {
            return;
        }
        String tailFlag = sourceTaskList.stream()
                .filter(Objects::nonNull)
                .map(TcTaskDraft::getTailFlag)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        aggregateTask.setTailFlag(tailFlag);
        if (!TcYesNoEnum.YES.getCode().equals(tailFlag)) {
            aggregateTask.setTailBalanceQty(BigDecimal.ZERO);
            return;
        }
        aggregateTask.setTailBalanceQty(this.selectStableTailBalanceQty(sourceTaskList));
    }

    /**
     * 按来源工单和业务键稳定排序后选择一条收尾余量。
     *
     * @param sourceTaskList 原始来源任务
     * @return 首个非空收尾余量；没有余量时返回零
     */
    private BigDecimal selectStableTailBalanceQty(List<TcTaskDraft> sourceTaskList) {
        List<TcTaskDraft> sortedTaskList = sourceTaskList.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing(this::resolveSourceOrderNo, Comparator.nullsLast(String::compareTo))
                        .thenComparing(TcTaskDraft::getBusinessKey, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());
        List<BigDecimal> remainQtyList = sortedTaskList.stream()
                .map(TcTaskDraft::getTailBalanceQty)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (remainQtyList.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal selectedValue = remainQtyList.get(0);
        boolean conflict = remainQtyList.stream()
                .anyMatch(value -> value.compareTo(selectedValue) != 0);
        if (conflict) {
            log.warn("[TC_TAIL_BALANCE_QTY_CONFLICT] sidewallCode={}, selectedValue={}, sourceValues={}",
                    sortedTaskList.stream()
                            .map(TcTaskDraft::getSidewallCode)
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElse(null), selectedValue, remainQtyList);
        }
        return selectedValue;
    }

    /**
     * 解析来源成型工单号，用于确定性排序。
     *
     * @param task 来源任务
     * @return 去除首尾空格后的来源工单号；空值返回 null
     */
    private String resolveSourceOrderNo(TcTaskDraft task) {
        if (task == null || task.getSourceOrderNos() == null
                || task.getSourceOrderNos().trim().isEmpty()) {
            return null;
        }
        return task.getSourceOrderNos().trim();
    }
}
