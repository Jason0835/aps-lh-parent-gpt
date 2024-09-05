package com.zlt.aps.tm.engine.vo;

import com.ruoyi.common.core.annotation.Excel;
import lombok.Data;

/**
 * 损耗VO
 */
@Data
public class TmLossVo {

    /**
     * 损耗率key（物料编号#机台id）
     */
    private String lossKey;

    /**
     * 耗损率（百分比）
     */
    private Double lossRate;
}
