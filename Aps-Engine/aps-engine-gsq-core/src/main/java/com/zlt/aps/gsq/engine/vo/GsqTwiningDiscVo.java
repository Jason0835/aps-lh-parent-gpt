package com.zlt.aps.gsq.engine.vo;

import lombok.Data;

/**
 * 钢丝圈 对应的 缠绕盘 尺寸和排列
 */
@Data
public class GsqTwiningDiscVo {

    /**
     * 钢丝圈代码
     */
    private String steelRingCode;

    /**
     * 尺寸~排列
     */
    private String specOrder;
}
