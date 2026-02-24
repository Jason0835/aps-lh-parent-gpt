package com.zlt.aps.mp.factory.helper;

import lombok.Data;

import java.io.Serializable;

/**
 * 订单渠道需求信息
 *
 * @author ZLT
 * @date 20250922
 */
@Data
public class MonthPlanProductionRequirementChannelHelper implements Serializable {
    /**
     * 渠道编码
     */
    private String code;
    /**
     * 需求量
     */
    private Long qty;

}
