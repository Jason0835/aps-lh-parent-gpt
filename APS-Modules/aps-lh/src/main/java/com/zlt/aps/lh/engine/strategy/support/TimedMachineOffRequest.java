package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import lombok.Data;

import java.time.LocalDate;

/** 前置选机冻结的按时间下机需求，仅在本批运行期使用，不重新决定机台及排序。 */
@Data
public class TimedMachineOffRequest {
    /** 来源 SKU，正式数量消费仍使用原账本。 */
    private SkuScheduleDTO sourceSku;
    /** 原选机规则确定的最早下机业务日。 */
    private LocalDate startDate;
    /** 清零前构造的真实物理机台产能画像，双模单控包含两侧。 */
    private ContinuationEndingMachineProfile profile;
}
