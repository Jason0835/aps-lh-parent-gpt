package com.zlt.aps.monthplan.factory.helper;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 订单需求信息
 *
 * @author ZLT
 * @date 20250922
 */
@Data
public class MonthPlanProductionRequirementHelper implements Serializable {
    /**
     * 物料编码
     */
    private String productCode;
    /**
     * 总分配数
     */
    private Long allocationQty;
    /**
     * 总需求量
     */
    private Long sumQty;
    /**
     * 净需求
     */
    private Long netDemandQty;
    /**
     * 库位需求量
     */
    private List<MonthPlanProductionRequirementLocationHelper> locationRequirementList;
    /**
     * 渠道需求量
     */
    private List<MonthPlanProductionRequirementChannelHelper> channelRequirementList;
}
