package com.zlt.aps.monthplan.factory.handler;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 物料规格扣减对象-辅助类
 *
 * @author ZLT
 * @date 20250324
 */
@Data
public class ProductSubtractVo implements Serializable {
    /**
     * 扣减维度key
     */
    private String sumKey;
    /**
     * 扣减数量
     */
    private Long sumSubtractQty;
    /**
     * 扣减的硫化时间-单位秒
     */
    private BigDecimal sumSubtractCuringTime;
}
