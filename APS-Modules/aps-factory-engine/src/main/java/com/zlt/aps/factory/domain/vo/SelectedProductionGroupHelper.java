package com.zlt.aps.factory.domain.vo;

import com.zlt.aps.factory.domain.dto.MouldTableInfoDto;
import com.zlt.aps.factory.domain.dto.ProductionGroupInfoDto;
import com.zlt.aps.factory.enums.ProductionOrientEnum;
import lombok.Getter;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 排产分组选中信息值传递对象
 *
 * @author ZLT
 * @date 20250718
 */
@Getter
public class SelectedProductionGroupHelper implements Serializable {
    /**
     * 选中的分组
     */
    private ProductionGroupInfoDto selectedProductionGroup;
    /**
     * 起始时间
     */
    private Integer productionGroupStartDate;
    /**
     * 结束日期
     */
    private Integer productionGroupEndDate;
    /**
     * 时间不一致时，使用
     */
    private DoubleMouldTableHelper doubleMouldTableHelper;

    /**
     * 构造函数
     *
     * @param selectedProductionGroup  选中的分组
     * @param productionGroupStartDate 开始日期
     * @param productionGroupEndDate   结束日期
     * @param doubleMouldTableHelper   时间不一致信息
     */
    public SelectedProductionGroupHelper(ProductionGroupInfoDto selectedProductionGroup, Integer productionGroupStartDate, Integer productionGroupEndDate, DoubleMouldTableHelper doubleMouldTableHelper) {
        this.selectedProductionGroup = selectedProductionGroup;
        this.productionGroupStartDate = productionGroupStartDate;
        this.productionGroupEndDate = productionGroupEndDate;
        this.doubleMouldTableHelper = doubleMouldTableHelper;
    }

    /**
     * 获取排产分组的排产时间范围信息
     * 1、时间一致取分组的时间
     * 2、时间不一致
     * 2.1、正向，取模台最小时间和模台最大时间
     * 2.2、方向，则取模台最大时间和模台最小时间
     *
     * @return
     */
    public String getProductionDateRangeInfo(ProductionOrientEnum productionOrient) {
        String dateRangeFormat = "[%d]-[%d]";
        //时间一致
        if (null == doubleMouldTableHelper) {
            return String.format(dateRangeFormat, productionGroupStartDate, productionGroupEndDate);
        }
        //时间不一致-正向
        if (ProductionOrientEnum.FORWARD == productionOrient) {
            Integer startProductionDate = Math.min(doubleMouldTableHelper.getFirstStartDate(), doubleMouldTableHelper.getSecondStartDate());
            Integer endProductionDate = Math.max(doubleMouldTableHelper.getFirstEndDate(), doubleMouldTableHelper.getSecondEndDate());
            return String.format(dateRangeFormat, startProductionDate, endProductionDate);
        }
        //时间不一致，反向
        Integer startProductionDate = Math.max(doubleMouldTableHelper.getFirstStartDate(), doubleMouldTableHelper.getSecondStartDate());
        Integer endProductionDate = Math.min(doubleMouldTableHelper.getFirstEndDate(), doubleMouldTableHelper.getSecondEndDate());
        return String.format(dateRangeFormat, startProductionDate, endProductionDate);
    }

    /**
     * 根据选择的排产分组信息及排产方向，获取需要进行时间差值处理的信息
     * 包含处理的模台，处理的时间段：开始日~截止日
     * 1、排产方向为正向：则处理的截止日=分组排产的开始日
     * 处理的模台 = 模台中最后排产日最早的模台
     * 处理的开始日 = 模台中最后排产日中最早的日
     * 2、排产方向为反向：则处理的开始日 = 分组排产的截止日
     * 处理的模台 = 模台中最后排产日最晚的模台
     * 处理的截止日 = 模台中最后排产日中最晚的日
     *
     * @param productionGroupHelper 双模分组信息--共时间段
     * @param productionOrient      排产方向
     * @return
     */
    public SingleMouldSelectedMouldTableHelper getHandlerDiffDateInfo(DoubleMouldProductionGroupHelper productionGroupHelper, ProductionOrientEnum productionOrient) {
        //排产方向：正向，则截止时间为排产分组的开始日，开始日为模台中最后排产日最早的，处理的模台也是最后排产日最早的
        if (ProductionOrientEnum.FORWARD == productionOrient) {
            Integer endDate = productionGroupHelper.getProductionGroupStartDate();
            Integer firstStartDate = doubleMouldTableHelper.getFirstStartDate();
            Integer secondStartDate = doubleMouldTableHelper.getSecondStartDate();
            if (firstStartDate < secondStartDate) {
                Integer startDate = firstStartDate;
                MouldTableInfoDto mouldTableInfo = selectedProductionGroup.getMouldTableInfoList().get(BigDecimal.ZERO.intValue());
                return new SingleMouldSelectedMouldTableHelper(mouldTableInfo, startDate, endDate);
            }
            Integer startDate = secondStartDate;
            MouldTableInfoDto mouldTableInfo = selectedProductionGroup.getMouldTableInfoList().get(BigDecimal.ONE.intValue());
            return new SingleMouldSelectedMouldTableHelper(mouldTableInfo, startDate, endDate);
        }
        //排产方向：反向，则开始日为排产分组的截止日，截止日为模台中最后排产日最晚的，处理的模台也是最后排产日最晚的
        Integer startDate = productionGroupHelper.getProductionGroupEndDate();
        Integer firstEndDate = doubleMouldTableHelper.getFirstEndDate();
        Integer secondEndDate = doubleMouldTableHelper.getSecondEndDate();
        if (firstEndDate < secondEndDate) {
            Integer endDate = firstEndDate;
            MouldTableInfoDto mouldTableInfo = selectedProductionGroup.getMouldTableInfoList().get(BigDecimal.ZERO.intValue());
            return new SingleMouldSelectedMouldTableHelper(mouldTableInfo, startDate, endDate);
        }
        Integer endDate = secondEndDate;
        MouldTableInfoDto mouldTableInfo = selectedProductionGroup.getMouldTableInfoList().get(BigDecimal.ONE.intValue());
        return new SingleMouldSelectedMouldTableHelper(mouldTableInfo, startDate, endDate);
    }

}
