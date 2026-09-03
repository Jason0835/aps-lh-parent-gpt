package com.zlt.aps.lh.engine.strategy.support;

import java.util.Objects;

/**
 * 同一台机台在单次日期池扫描中的普通/跨日准备双桶提案。
 *
 * <p>普通提案和目标日跨日准备提案必须分别保留最佳结果，避免较早日期池或历史资源
 * 层级中的跨日提案，在单提案压缩阶段提前淘汰后续日期池的普通可开产提案。</p>
 *
 * @author APS
 */
public final class NewSpecMachineProposalBuckets {

    /** 空桶，避免调用方为不可排机台创建额外对象 */
    private static final NewSpecMachineProposalBuckets EMPTY =
            new NewSpecMachineProposalBuckets(null, null);

    /** 当前业务日普通可开产的最佳提案 */
    private final NewSpecScheduleProposal ordinaryProposal;
    /** 目标日跨日准备的最佳提案 */
    private final NewSpecScheduleProposal crossDayProposal;

    private NewSpecMachineProposalBuckets(NewSpecScheduleProposal ordinaryProposal,
                                          NewSpecScheduleProposal crossDayProposal) {
        this.ordinaryProposal = ordinaryProposal;
        this.crossDayProposal = crossDayProposal;
    }

    /**
     * 创建空桶。
     *
     * @return 空桶
     */
    public static NewSpecMachineProposalBuckets empty() {
        return EMPTY;
    }

    /**
     * 创建普通/跨日双桶。
     *
     * @param ordinaryProposal 普通可开产提案；可为null
     * @param crossDayProposal 目标日跨日准备提案；可为null
     * @return 双桶结果
     */
    public static NewSpecMachineProposalBuckets of(NewSpecScheduleProposal ordinaryProposal,
                                                   NewSpecScheduleProposal crossDayProposal) {
        if (Objects.isNull(ordinaryProposal) && Objects.isNull(crossDayProposal)) {
            return EMPTY;
        }
        return new NewSpecMachineProposalBuckets(ordinaryProposal, crossDayProposal);
    }

    /**
     * 判断是否存在普通可开产提案。
     *
     * @return true-存在普通提案
     */
    public boolean hasOrdinaryProposal() {
        return Objects.nonNull(ordinaryProposal);
    }

    /**
     * 判断是否存在目标日跨日准备提案。
     *
     * @return true-存在跨日准备提案
     */
    public boolean hasCrossDayProposal() {
        return Objects.nonNull(crossDayProposal);
    }

    /**
     * 获取普通可开产的最佳提案。
     *
     * @return 普通提案；不存在时返回null
     */
    public NewSpecScheduleProposal getOrdinaryProposal() {
        return ordinaryProposal;
    }

    /**
     * 获取目标日跨日准备的最佳提案。
     *
     * @return 跨日准备提案；不存在时返回null
     */
    public NewSpecScheduleProposal getCrossDayProposal() {
        return crossDayProposal;
    }

    /**
     * 解析单提案入口使用的优先提案。
     *
     * <p>普通提案永远优先于跨日准备提案；该方法是固定指令、共享顺序等旧单提案入口
     * 的兼容出口，动态标准竞争应直接使用两个桶。</p>
     *
     * @return 优先提案；两个桶均为空时返回null
     */
    public NewSpecScheduleProposal resolvePreferredProposal() {
        return Objects.nonNull(ordinaryProposal) ? ordinaryProposal : crossDayProposal;
    }
}
