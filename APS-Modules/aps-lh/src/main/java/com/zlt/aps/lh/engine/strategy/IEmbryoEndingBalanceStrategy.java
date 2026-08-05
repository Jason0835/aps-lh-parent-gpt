/**
 * Copyright (c) 2008, 智立通（厦门）科技有限公司 All rights reserved。
 */
package com.zlt.aps.lh.engine.strategy;

import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.context.LhScheduleContext;

import java.util.List;

/**
 * 共用胎胚/同SKU多机台收尾均衡策略接口。
 *
 * <p>续作排产完成后、日计划账本扣减和换活字块排产之前执行：
 * 对共用胎胚组内多台收尾机台（含单胎胚同SKU多机台）的尾量进行分摊、补量或减量，
 * 使后物料的换模/换活字块尽量分散到同一天不同班次，并满足每日换模次数硬限制。</p>
 *
 * <p>本策略只做模拟计数和过程日志，不预占真实 {@code dailyMouldChangeCountMap}，
 * 后续换活字块和新增排产仍通过现有主链正式登记换模次数。</p>
 *
 * @author APS
 */
public interface IEmbryoEndingBalanceStrategy {

    /**
     * 执行共用胎胚/同SKU多机台收尾均衡。
     *
     * @param context 排程上下文
     * @param shifts 排程窗口班次
     * @return true-至少执行了一次均衡调整；false-未触发调整
     */
    boolean balanceSharedEmbryoEnding(LhScheduleContext context, List<LhShiftConfigVO> shifts);
}
