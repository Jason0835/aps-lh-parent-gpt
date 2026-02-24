package com.zlt.aps.monthplan.factory.helper;

import lombok.Data;

import java.io.Serializable;

/**
 * 订单库位需求信息
 *
 * @author ZLT
 * @date 20250922
 */
@Data
public class MonthPlanProductionRequirementLocationHelper implements Serializable {
    /**
     * 库位类型
     */
    private String type;
    /**
     * 需求量
     */
    private Long qty;
}
