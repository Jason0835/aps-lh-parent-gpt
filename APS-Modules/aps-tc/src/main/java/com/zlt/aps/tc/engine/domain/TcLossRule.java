package com.zlt.aps.tc.engine.domain;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 胎侧自动排程损耗率规则。
 *
 * <p>由数据加载阶段从业务实体映射而来，供引擎在机台已知后按优先级匹配最终损耗率。</p>
 */
@Data
public class TcLossRule {

    /** 工厂编号 */
    private String factoryCode;

    /** 胎侧编码 */
    private String sidewallCode;

    /** 机台编码 */
    private String machineCode;

    /** 损耗率，百分比 */
    private BigDecimal lossRate;

    /** 优先级 */
    private Integer priority;
}