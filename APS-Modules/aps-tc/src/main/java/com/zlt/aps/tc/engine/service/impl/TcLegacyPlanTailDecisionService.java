package com.zlt.aps.tc.engine.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.zlt.aps.tc.api.enums.TcYesNoEnum;
import com.zlt.aps.tc.engine.domain.TcTaskDraft;
import com.zlt.aps.tc.engine.service.ITcPlanTailDecisionService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * 基于现有收尾标识和收尾余量的胎侧兼容判定实现。
 *
 * <p>TODO：月计划余量来源表确定后，改为按工厂、日期、胎侧代码查询；
 * 余量大于零为非收尾，余量小于等于零为收尾。</p>
 */
@Service
public class TcLegacyPlanTailDecisionService implements ITcPlanTailDecisionService {

    /**
     * 汇总来源任务的收尾信息。
     *
     * <p>收尾标识由计划量汇总前的生产属性校验保证一致；成型余量属于独立来源行级数据，
     * 需累加为汇总任务的组级收尾余量，不能复制首条来源值。</p>
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
        BigDecimal groupTailBalanceQty = sourceTaskList.stream()
                .filter(Objects::nonNull)
                .map(TcTaskDraft::getTailBalanceQty)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        aggregateTask.setTailBalanceQty(groupTailBalanceQty);
    }
}
