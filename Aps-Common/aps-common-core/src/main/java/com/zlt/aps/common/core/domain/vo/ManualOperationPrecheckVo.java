package com.zlt.aps.common.core.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 人工排程提交前只读预校验结果。
 *
 * <p>该对象只承载告警，不改变提交语义；页面确认后仍由人工链表入口按完整计划量执行。</p>
 */
@Data
public class ManualOperationPrecheckVo {

    /** 是否存在班次产能超限。 */
    private boolean capacityExceeded;

    /** 是否存在工装可用数量超限。 */
    private boolean toolExceeded;

    /** 各机台班次预计超产能量，key=机台|班次。 */
    private Map<String, BigDecimal> capacityOverflowMap = new LinkedHashMap<>();

    /** 告警信息。 */
    private List<String> warnings = new ArrayList<>();

    /** 是否需要页面二次确认。 */
    public boolean needConfirm() {
        return this.capacityExceeded || this.toolExceeded;
    }
}
