package com.zlt.aps.mp.engine.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 特殊材料信息对象
 *
 * @author ZLT
 * @date 20251222
 */
@Data
public class SpecialMaterialStockVo implements Serializable {
    /**
     * 特殊原材料编码
     */
    private String materialCode;
    /**
     * 特殊原材料描述
     */
    private String materialDesc;
    /**
     * 标准长（转换成重量=批次箱数 * 总重量/总箱数）
     */
    private Long standardLength;
    /**
     * 标准长（实际是批次编号，转换前的数）
     */
    private Long oriStandardLength;
    /**
     * 库存量
     */
    private Long stock;

}
