package com.zlt.aps.monthplan.api.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 物料排产信息对象
 *
 * @author ZLT
 * @date 20250324
 */
@Data
public class ProductProductionInfoVo implements Serializable {
    /**
     * 物料编码
     */
    private String productCode;
    /**
     * 排产数量
     */
    private Long productionQty;

}
