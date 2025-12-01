package com.zlt.aps.factory.domain.vo;

import com.zlt.aps.factory.domain.dto.MouldTableInfoDto;
import com.zlt.aps.factory.domain.dto.ProductionGroupInfoDto;
import com.zlt.aps.factory.enums.ProductionOrientEnum;
import lombok.Getter;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 双模排产-衔接分组辅助类
 *
 * @author ZLT
 * @date 20250807
 */
@Getter
public class DoubleMouldProductionGroupHelper implements Serializable {
    /**
     * 第一副模台
     */
    private MouldTableInfoDto firstMouldTable;
    /**
     * 第二副模台
     */
    private MouldTableInfoDto secondMouldTable;
    /**
     * 起始日
     */
    private Integer productionGroupStartDate;
    /**
     * 结束日
     */
    private Integer productionGroupEndDate;

    /**
     * 构造辅助类，-值传递
     *
     * @param firstMouldTable          第一副模台
     * @param secondMouldTable         第二副模台
     * @param productionGroupStartDate 起始日
     * @param productionGroupEndDate   结束日
     */
    public DoubleMouldProductionGroupHelper(MouldTableInfoDto firstMouldTable, MouldTableInfoDto secondMouldTable, Integer productionGroupStartDate, Integer productionGroupEndDate) {
        this.firstMouldTable = firstMouldTable;
        this.secondMouldTable = secondMouldTable;
        this.productionGroupStartDate = productionGroupStartDate;
        this.productionGroupEndDate = productionGroupEndDate;
    }

    /**
     * 构建信息
     * 根据排产方向，取得双模能同时排产的时间段信息
     * doubleMouldTableHelper对象不为空，则表示排产分组的两个模台已排产时间不一致
     * 且跟后续排产的规格满足拼模条件
     *
     * @param selectedProductionGroupHelper 选中的排产分组信息
     * @param productionOrient              排产方向
     * @return
     */
    public static DoubleMouldProductionGroupHelper buildInstance(SelectedProductionGroupHelper selectedProductionGroupHelper, ProductionOrientEnum productionOrient) {
        ProductionGroupInfoDto selectedProductionGroup = selectedProductionGroupHelper.getSelectedProductionGroup();
        MouldTableInfoDto firstMouldTable = selectedProductionGroup.getMouldTableInfoList().get(BigDecimal.ZERO.intValue());
        MouldTableInfoDto secondMouldTable = selectedProductionGroup.getMouldTableInfoList().get(BigDecimal.ONE.intValue());
        Integer productionGroupStartDate = selectedProductionGroupHelper.getProductionGroupStartDate();
        Integer productionGroupEndDate = selectedProductionGroupHelper.getProductionGroupEndDate();
        DoubleMouldTableHelper tableHelper = selectedProductionGroupHelper.getDoubleMouldTableHelper();
        //时间不一致的处理--有一段时间走单模排
        if (null != tableHelper && ProductionOrientEnum.FORWARD == productionOrient) {
            productionGroupStartDate = Math.max(tableHelper.getFirstStartDate(), tableHelper.getSecondStartDate());
            productionGroupEndDate = Math.min(tableHelper.getFirstEndDate(), tableHelper.getSecondEndDate());
        } else if (null != tableHelper && ProductionOrientEnum.REVERSE == productionOrient) {
            productionGroupStartDate = Math.min(tableHelper.getFirstStartDate(), tableHelper.getSecondStartDate());
            productionGroupEndDate = Math.max(tableHelper.getFirstEndDate(), tableHelper.getSecondEndDate());
        }
        return new DoubleMouldProductionGroupHelper(firstMouldTable, secondMouldTable, productionGroupStartDate, productionGroupEndDate);
    }

    /**
     * 根据模具排产起始日及排产方向，获取真实排产开始日
     * 1、如果分组排产开始日为空，则取模具排产开始日
     * 2、否则，比较分组排产开始日与模具排产开始日
     * 2.1、如果是正向排产，则取时间最晚的，即分组排产开始日与模具排产开始日中取最大的
     * 2.2、如果是反向排产，则取时间最早的，即分组排产开始日与模具排产开始日中取最小的
     *
     * @param mouldStartDate   模具排产开始日
     * @param productionOrient 排产方向
     * @return
     */
    public Integer getRealStartProductionDate(Integer mouldStartDate, ProductionOrientEnum productionOrient) {
        if (null == productionGroupStartDate) {
            return mouldStartDate;
        }
        if (ProductionOrientEnum.FORWARD == productionOrient) {
            return Math.max(productionGroupStartDate, mouldStartDate);
        }
        return Math.min(productionGroupStartDate, mouldStartDate);
    }


    /**
     * 根据模具排产截止日及排产方向，获取真实排产截止日
     * 1、如果分组排产截止日为空，则取模具截止排产日
     * 2、否则，比较分组排产截止日与模具排产截止日
     * 2.1、如果是正向排产，则取时间最早的，即分组排产截止日与模具排产截止日中取最小的
     * 2.2、如果是反向排产，则取时间最晚的，即分组排产截止日与模具排产截止日中取最大的
     *
     * @param mouldEndDate     模具截止排产日
     * @param productionOrient 排产方向
     * @return
     */
    public Integer getRealEndProductionDate(Integer mouldEndDate, ProductionOrientEnum productionOrient) {
        if (null == productionGroupEndDate) {
            return mouldEndDate;
        }
        if (ProductionOrientEnum.FORWARD == productionOrient) {
            return Math.min(productionGroupEndDate, mouldEndDate);
        }
        return Math.max(productionGroupEndDate, mouldEndDate);
    }
}
