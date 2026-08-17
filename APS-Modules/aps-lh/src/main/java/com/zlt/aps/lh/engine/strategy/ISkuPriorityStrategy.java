/**
 * Copyright (c) 2008, 智立通（厦门）科技有限公司 All rights reserved。
 */
package com.zlt.aps.lh.engine.strategy;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.context.LhScheduleContext;

import java.util.List;

/**
 * SKU排产优先级策略接口
 * <p>对结构下的SKU进行排产顺序排列</p>
 *
 * @author APS
 */
public interface ISkuPriorityStrategy {

    /**
     * 对context中的newSpecSkuList按优先级排序
     * <p>
     * 排序规则:
     * <ol>
     *   <li>有发货要求的优先(锁定上机日期)</li>
     *   <li>延误上机的优先(延误越久越优先)</li>
     *   <li>结构转产表最大END_DAY距T日的包含首尾天数严格小于SYS0304002时，
     *       同结构全部候选SKU进入原结构收尾层级，结构收尾日越晚越优先上机</li>
     *   <li>供应链优先级(高优先级->周期排产->中优先级->搭配排产)</li>
     * </ol>
     * </p>
     *
     * @param context 排程上下文
     */
    void sortByPriority(LhScheduleContext context);

    /**
     * 对当前业务日实际待排的新增 SKU 重新执行统一优先级排序。
     *
     * <p>日驱动排产中，前一业务日未选到机台的 SKU 只保留业务属性和剩余目标量，
     * 不保留前一日排序位置。本方法复用 {@link #sortByPriority(LhScheduleContext)} 的新增
     * SKU 比较器，并按当前待排集合重新回写名次及排序说明。</p>
     *
     * @param context 排程上下文
     * @param pendingNewSpecSkuList 当前业务日实际待排的新增 SKU 列表
     */
    void sortNewSpecByPriority(LhScheduleContext context,
                               List<SkuScheduleDTO> pendingNewSpecSkuList);
}
