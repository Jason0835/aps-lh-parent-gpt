package com.zlt.aps.factory.domain.dto;

import com.zlt.aps.factory.domain.vo.ProductionInfoVo;
import lombok.Getter;

/**
 * 双模排产结果值传递辅助类
 *
 * @author ZLT
 * @date 20250420
 */
@Getter
public class DoubleMouldProductionResultHelper {
    /**
     * 下一个排产日
     */
    private Integer nextProductionDate;
    /**
     * 还需排产量
     */
    private Long needProductionQty;
    /**
     * 第一副模排产结果
     */
    private ProductionInfoVo firstProductionInfo;
    /**
     * 第二副模排产结果
     */
    private ProductionInfoVo secondProductionInfo;

    /**
     * 双模排产结果对象构建
     *
     * @param nextProductionDate   下一排产日
     * @param needProductionQty    还需排产日
     * @param firstProductionInfo  第一副模排产结果
     * @param secondProductionInfo 第二副模排产结果
     */
    public DoubleMouldProductionResultHelper(Integer nextProductionDate, Long needProductionQty, ProductionInfoVo firstProductionInfo, ProductionInfoVo secondProductionInfo) {
        this.nextProductionDate = nextProductionDate;
        this.needProductionQty = needProductionQty;
        this.firstProductionInfo = firstProductionInfo;
        this.secondProductionInfo = secondProductionInfo;
    }
}
