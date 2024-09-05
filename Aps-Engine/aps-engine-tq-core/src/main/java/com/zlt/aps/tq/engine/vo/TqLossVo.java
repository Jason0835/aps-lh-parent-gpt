package com.zlt.aps.tq.engine.vo;

import com.ruoyi.common.core.annotation.Excel;
import lombok.Data;

/**
 * 损耗VO
 */
@Data
public class TqLossVo {

    /**
     * 损耗率key（物料编号#机台id）
     */
    private String lossKey;

    /**
     * 耗损率（百分比）
     */
    private Double lossRate;
}
