package com.zlt.aps.lh.service;

import com.zlt.aps.lh.api.domain.entity.LhEndingDiscard;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * 硫化收尾舍弃计划记录服务，不参与排产判断及业务账本修改。
 *
 * @author APS
 * @date 2026/10/28
 */
public interface ILhEndingDiscardService extends IDocService<LhEndingDiscard> {

    /**
     * 以最终未排确认有效规则命中，并按 SKU 与原因生成唯一舍弃记录。
     *
     * @param context 已完成所有主排程及最终未排收口的上下文
     * @return 使用规则命中时实际余量生成的本批记录
     */
    List<LhEndingDiscard> buildRecords(LhScheduleContext context);

    /**
     * 在主排程事务中替换同工厂同目标日舍弃记录，空结果也清理旧数据。
     *
     * @param context 已完成主排程收口的上下文
     * @throws org.springframework.transaction.IllegalTransactionStateException 调用时没有主排程事务
     */
    void replaceForSchedule(LhScheduleContext context);
}
