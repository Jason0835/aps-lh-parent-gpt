package com.zlt.aps.common.engine.schedule.constraint;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 当日成型总余量额度采集结果。
 *
 * <p>包含按桶建好的台账和采集阶段的告警；告警只写日志与解释证据，不阻断排程。</p>
 */
public class ScheduleTailQuotaCollectResult {

    /**
     * 额度桶键到台账的映射；未启用额度的产品不出现
     */
    private final Map<String, ScheduleTailQuotaLedger> ledgerMap = new LinkedHashMap<>();

    /**
     * 采集告警编码列表
     */
    private final List<String> warningList = new ArrayList<>();

    /**
     * 获取额度台账映射。
     *
     * @return 桶键到台账的映射
     */
    public Map<String, ScheduleTailQuotaLedger> getLedgerMap() {
        return this.ledgerMap;
    }

    /**
     * 获取采集告警编码列表。
     *
     * @return 告警编码列表
     */
    public List<String> getWarningList() {
        return this.warningList;
    }

    /**
     * 判断是否未启用任何额度。
     *
     * @return 没有任何台账时返回true
     */
    public boolean isEmpty() {
        return this.ledgerMap.isEmpty();
    }
}
