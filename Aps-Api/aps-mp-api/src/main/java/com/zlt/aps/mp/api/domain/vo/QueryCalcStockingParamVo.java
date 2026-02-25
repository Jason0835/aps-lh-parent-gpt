package com.zlt.aps.mp.api.domain.vo;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

/**
 * 计算备货查询参数
 *
 * @author hsc
 * @since 2025/2/17
 */
@Data
@ApiModel(value = "计算备货查询参数对象Vo", description = "计算备货查询参数对象Vo")
public class QueryCalcStockingParamVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 轮胎类型
     */
    private String tireType;

    /**
     * 选择月份范围 3：近三个月 6:：近6个月 12：近12个月 24：近24个月 36：近36个月
     */
    private Long monthRange;
    /**
     * 分厂编码
     */
    private String factoryCode;
}
