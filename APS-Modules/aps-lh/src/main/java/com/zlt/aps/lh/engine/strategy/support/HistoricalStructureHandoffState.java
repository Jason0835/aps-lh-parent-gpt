package com.zlt.aps.lh.engine.strategy.support;

/**
 * 历史结构承接关系在本窗口内的状态；临时等待与确定不可承接分别管理。
 */
public enum HistoricalStructureHandoffState {
    /**
     * 尚未取得确定准入证据，继续保留原指定关系。
     */
    PENDING,
    /**
     * 基础等待可通过，当前班次或资源尚未满足，保留重试资格。
     */
    WAITING,
    /**
     * 已有正式结果承接该历史关系，不再排他保留。
     */
    COMMITTED,
    /**
     * 后料真实需求已消耗完毕，不再排他保留。
     */
    NO_DEMAND,
    /**
     * 冻结的基础供胚等待已超限，本窗口后续非负偏移无法恢复资格。
     */
    WINDOW_INFEASIBLE
}
