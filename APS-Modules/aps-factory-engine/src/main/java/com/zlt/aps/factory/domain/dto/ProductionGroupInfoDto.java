package com.zlt.aps.factory.domain.dto;

import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.vo.MouldDayProductionVo;
import com.zlt.aps.factory.enums.ProductionOrientEnum;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * 排产分组信息对象
 *
 * @author ZLT
 * @date 20250715
 */
@Data
public class ProductionGroupInfoDto implements Serializable {
    /**
     * 排产分组编号值
     */
    private String productionGroupValue;
    /**
     * 分组模台数：1-为单模台 2-为双模台
     */
    private Integer mouldNumber;
    /**
     * 已排完日期
     */
    private Set<Integer> finishedDay;
    /**
     * 当前左模
     */
    private String leftMouldCode;
    /**
     * 当前右模
     */
    private String rightMouldCode;
    /**
     * 当前排产方向
     */
    private ProductionOrientEnum productionOrient;
    /**
     * 是否为空分组--初始时都为空分组
     */
    private Boolean emptyGroup;
    /**
     * 是否拼模排产
     */
    private boolean assemble;
    /**
     * 是否排产完毕
     */
    private boolean productionFinish;
    /**
     * 分组--模台信息
     */
    private List<MouldTableInfoDto> mouldTableInfoList;
    /**
     * 剩余产能--需要实时计算得出
     */
    private Long leftOverQty;
    /**
     * 可排产日--初始化后不再变化
     */
    private Set<Integer> needProductionDateSet;
    /**
     * 是否续作排产分组--默认false
     */
    private boolean continueGroup;

    /**
     * 是否排产
     * true 表示已排产
     * false 表示未排产
     * 空分组则为false
     * 模台信息为空集合，则也是false
     * 模台的排产信息都为空，则也是false
     *
     * @return
     */
    public boolean isProductionSchedule() {
        if (emptyGroup) {
            return false;
        }
        if (CollectionUtils.isEmpty(mouldTableInfoList)) {
            return false;
        }
        boolean isProductionFlag = false;
        for (MouldTableInfoDto mouldTableInfo : mouldTableInfoList) {
            if (null != mouldTableInfo && null != mouldTableInfo.getLastProductionInfo()) {
                isProductionFlag = true;
                break;
            }
        }
        return isProductionFlag;
    }

    /**
     * 判断排产分组是否单模或是双模一起能够连续排产continueDays
     * isDouble为true，表示双模台连续排产，false表示单模连续排产
     * 单模连续排产则只要有一模台可连续排产则表示能连续排产
     * 双模台连续排产，则表示双模台共有的连续排产天数能够达到continueDays
     *
     * @param continueDays 连续排产天数
     * @param isDouble     true 双模台 false 单模台
     * @return
     */
    public boolean isContinueProductionDayCapacity(Integer continueDays, boolean isDouble) {
        //单模且单模台
        if (!isDouble && ProductionConstant.SINGLE_MOULD_QTY.equals(mouldNumber)) {
            MouldTableInfoDto singleMouldTable = mouldTableInfoList.get(BigDecimal.ZERO.intValue());
            Integer findContinueDays = findContinueDays(singleMouldTable);
            return findContinueDays >= continueDays;
        }
        //单模且双模台
        if (!isDouble && ProductionConstant.DOUBLE_MOULD_QTY.equals(mouldNumber)) {
            Integer findFirstContinueDays = findContinueDays(mouldTableInfoList.get(BigDecimal.ZERO.intValue()));
            Integer findSecondContinueDays = findContinueDays(mouldTableInfoList.get(BigDecimal.ONE.intValue()));
            Integer findContinueDays = Math.max(findFirstContinueDays, findSecondContinueDays);
            return findContinueDays >= continueDays;
        }
        //双模且单模台
        if (isDouble && ProductionConstant.SINGLE_MOULD_QTY.equals(mouldNumber)) {
            return false;
        }
        Integer assemblingContinueDays = getAssemblingContinueDays();
        return assemblingContinueDays >= continueDays;
    }

    /**
     * 获取拼模可连续排产天数值
     *
     * @return
     */
    public Integer getAssemblingContinueDays() {
        if (ProductionConstant.SINGLE_MOULD_QTY.equals(mouldNumber)) {
            return BigDecimal.ZERO.intValue();
        }
        //双模且双模台
        if (emptyGroup) {
            return needProductionDateSet.size();
        }
        MouldTableInfoDto firstMouldTable = mouldTableInfoList.get(BigDecimal.ZERO.intValue());
        MouldTableInfoDto secondMouldTable = mouldTableInfoList.get(BigDecimal.ONE.intValue());
        Integer firstStartDay = firstMouldTable.getStartDay(ProductionOrientEnum.FORWARD);
        Integer firstEndDay = firstMouldTable.getEndDay(ProductionOrientEnum.FORWARD);
        Integer maxDay = Math.max(firstStartDay, firstEndDay);

        Integer secondStartDay = secondMouldTable.getStartDay(ProductionOrientEnum.FORWARD);
        Integer secondEndDay = secondMouldTable.getEndDay(ProductionOrientEnum.FORWARD);
        Integer minDay = Math.min(secondStartDay, secondEndDay);
        //没有交叉集
        if (maxDay <= minDay) {
            return BigDecimal.ZERO.intValue();
        }
        //值有交叉集合
        Integer realStartDay = Math.max(firstStartDay, secondStartDay);
        Integer realEndDay = Math.min(firstEndDay, secondEndDay);
        return firstMouldTable.getContinueDays(realStartDay, realEndDay);
    }

    /**
     * 判断排产分组是否与productCode同规格排产
     * 以最后一个排产信息判断
     * 只要有一个模台最后排产信息中的SAP代码与productCode
     * 匹配，则表示同规格排产分组
     *
     * @param productCode SAP代码
     * @return
     */
    public boolean isSameProductCode(String productCode) {
        if (StringUtils.isBlank(productCode)) {
            return false;
        }
        //空分组
        if (emptyGroup) {
            return false;
        }
        if (CollectionUtils.isEmpty(mouldTableInfoList)) {
            return false;
        }
        for (MouldTableInfoDto mouldTableInfo : mouldTableInfoList) {
            MouldDayProductionVo lastProductionInfo = mouldTableInfo.getLastProductionInfo();
            if (null == lastProductionInfo) {
                continue;
            }
            if (productCode.equals(lastProductionInfo.getProductCode())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断排产分组是否与embryoCode、mouldNo共生胎同模具排产
     * 以最后一个排产信息判断
     * 只要有一个模台最后排产信息中的生胎代码及模具与embryoCode、mouldNo
     * 匹配，则表示共生胎、同模具排产分组
     *
     * @param embryoCode 生胎代码
     * @param mouldNo    模具
     * @return
     */
    public boolean isSameEmbryoCodeAndMouldNo(String embryoCode, String mouldNo) {
        if (StringUtils.isBlank(embryoCode) || StringUtils.isNotBlank(mouldNo)) {
            return false;
        }
        //空分组
        if (emptyGroup) {
            return false;
        }
        if (CollectionUtils.isEmpty(mouldTableInfoList)) {
            return false;
        }
        for (MouldTableInfoDto mouldTableInfo : mouldTableInfoList) {
            MouldDayProductionVo lastProductionInfo = mouldTableInfo.getLastProductionInfo();
            if (null == lastProductionInfo) {
                continue;
            }
            if (embryoCode.equals(lastProductionInfo.getEmbryoCode()) && mouldNo.equals(lastProductionInfo.getMouldNo())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断排产分组是否与embryoCode、mouldNo共生胎不同模具排产
     * 以最后一个排产信息判断
     * 只要有一个模台最后排产信息中的生胎代码与embryoCode匹配，而模具不与mouldNo
     * 匹配，则表示共生胎、不同模具排产分组
     *
     * @param embryoCode 生胎代码
     * @param mouldNo    模具
     * @return
     */
    public boolean isSameEmbryoCodeAndNoMouldNo(String embryoCode, String mouldNo) {
        if (StringUtils.isBlank(embryoCode) || StringUtils.isNotBlank(mouldNo)) {
            return false;
        }
        //空分组
        if (emptyGroup) {
            return false;
        }
        if (CollectionUtils.isEmpty(mouldTableInfoList)) {
            return false;
        }
        //是否共生胎、同模具，默认为false
        boolean isSameEmbryoCodeAndMouldNo = false;
        for (MouldTableInfoDto mouldTableInfo : mouldTableInfoList) {
            MouldDayProductionVo lastProductionInfo = mouldTableInfo.getLastProductionInfo();
            if (null == lastProductionInfo) {
                continue;
            }
            if (embryoCode.equals(lastProductionInfo.getEmbryoCode()) && mouldNo.equals(lastProductionInfo.getMouldNo())) {
                isSameEmbryoCodeAndMouldNo = true;
                break;
            }
        }
        //已经是共生胎、同模具，则一定不是共生胎、不同模具
        if (isSameEmbryoCodeAndMouldNo) {
            return false;
        }
        //只要有共生胎，则就一定不同模具
        for (MouldTableInfoDto mouldTableInfo : mouldTableInfoList) {
            MouldDayProductionVo lastProductionInfo = mouldTableInfo.getLastProductionInfo();
            if (null == lastProductionInfo) {
                continue;
            }
            if (embryoCode.equals(lastProductionInfo.getEmbryoCode())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断排产分组是否与mouldNo同模具排产
     * 以最后一个排产信息判断
     * 只要有一个模台最后排产信息中的模具与与mouldNo
     * 匹配，则表示同模具排产分组
     *
     * @param mouldNo 模具
     * @return
     */
    public boolean isSameMouldNo(String mouldNo) {
        if (StringUtils.isNotBlank(mouldNo)) {
            return false;
        }
        //空分组
        if (emptyGroup) {
            return false;
        }
        if (CollectionUtils.isEmpty(mouldTableInfoList)) {
            return false;
        }
        for (MouldTableInfoDto mouldTableInfo : mouldTableInfoList) {
            MouldDayProductionVo lastProductionInfo = mouldTableInfo.getLastProductionInfo();
            if (null == lastProductionInfo) {
                continue;
            }
            if (mouldNo.equals(lastProductionInfo.getMouldNo())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断排产分组是否与proSize同寸口排产
     * 以最后一个排产信息判断
     * 只要有一个模台最后排产信息中的寸口与与proSize
     * 匹配，则表示同寸口排产分组
     *
     * @param proSize 寸口
     * @return
     */
    public boolean isSameProSize(BigDecimal proSize) {
        if (null == proSize) {
            return false;
        }
        //空分组
        if (emptyGroup) {
            return false;
        }
        if (CollectionUtils.isEmpty(mouldTableInfoList)) {
            return false;
        }
        for (MouldTableInfoDto mouldTableInfo : mouldTableInfoList) {
            MouldDayProductionVo lastProductionInfo = mouldTableInfo.getLastProductionInfo();
            if (null == lastProductionInfo) {
                continue;
            }
            if (proSize.equals(lastProductionInfo.getProSize())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 剩余产能是否能覆盖需求量needQty
     *
     * @param needQty 需求量
     * @return
     */
    public boolean isFulfillment(Long needQty) {
        if (null == needQty) {
            needQty = BigDecimal.ZERO.longValue();
        }
        Long realLeftOverQty = leftOverQty;
        if (null == realLeftOverQty) {
            realLeftOverQty = BigDecimal.ZERO.longValue();
        }
        return realLeftOverQty >= needQty;
    }

    /**
     * 剩余产能天数
     *
     * @return
     */
    public Integer getLeftOverDays() {
        if (CollectionUtils.isEmpty(mouldTableInfoList)) {
            return BigDecimal.ZERO.intValue();
        }
        Integer leftOverDay = BigDecimal.ZERO.intValue();
        for (MouldTableInfoDto mouldTableInfo : mouldTableInfoList) {
            leftOverDay = leftOverDay + mouldTableInfo.getLeftOverDays();
        }
        return leftOverDay;
    }

    /**
     * 获取模台可连续排产的天数
     *
     * @param mouldTableInfo
     * @return
     */
    private Integer findContinueDays(MouldTableInfoDto mouldTableInfo) {
        MouldDayProductionVo lastProductionInfo = mouldTableInfo.getLastProductionInfo();
        if (null == lastProductionInfo) {
            return needProductionDateSet.size();
        }
        Integer startDay = lastProductionInfo.getProductionDate();
        Integer endDay = mouldTableInfo.getEndDay(productionOrient);
        return mouldTableInfo.getContinueDays(startDay, endDay);
    }

}
