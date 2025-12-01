package com.zlt.aps.factory.domain.dto;

import com.zlt.aps.factory.domain.vo.MouldInfoVO;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 双模排产参数值传递辅助类
 *
 * @author ZLT
 * @date 20250420
 */
@Getter
public class DoubleMouldProductionParamHelper {
    /**
     * 排产物料编码
     */
    private String productCode;
    /**
     * 排产日期
     */
    private Integer startProductionDate;
    /**
     * 需要排产量
     */
    private Long needProductionQty;
    /**
     * 排产计划ID
     */
    private Long monthPlanId;
    /**
     * 第一副模
     */
    private MouldInfoVO first;
    /**
     * 第二副模
     */
    private MouldInfoVO second;
    /**
     * 下一个排产日
     */
    private Integer nextProductionDate;
    /**
     * 上一个排产日
     */
    private Integer previousProductionDate;

    private List<MouldInfoVO> productionMouldList;

    /**
     * 构造函数
     *
     * @param productCode            物料
     * @param previousProductionDate 前一个排产日
     * @param startProductionDate    排产日
     * @param nextProductionDate     下一个排产日
     * @param needProductionQty      需排产量
     * @param monthPlanId            排产计划ID
     * @param first                  第一副模
     * @param second                 第二副模
     */
    public DoubleMouldProductionParamHelper(String productCode, Integer previousProductionDate, Integer startProductionDate, Integer nextProductionDate, Long needProductionQty, Long monthPlanId, MouldInfoVO first, MouldInfoVO second) {
        this.productCode = productCode;
        this.previousProductionDate = previousProductionDate;
        this.startProductionDate = startProductionDate;
        this.nextProductionDate = nextProductionDate;
        this.needProductionQty = needProductionQty;
        this.monthPlanId = monthPlanId;
        this.first = first;
        this.second = second;
        productionMouldList = new ArrayList<>();
        productionMouldList.add(first);
        productionMouldList.add(second);
    }
}
