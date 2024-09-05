package com.zlt.aps.gsq.engine.vo;

import lombok.Data;

/**
 * 钢丝圈 对应的成型胚胎、成型机code
 */
@Data
public class GsqQuotaParam {

    /**
     * 钢丝圈代号
     */
    private String steelRingCode;

    /**
     * 机台code$胎胚代码，多个逗号分割， 用来计算成型平均定额使用
     */
    private String quotaKeys;
}
