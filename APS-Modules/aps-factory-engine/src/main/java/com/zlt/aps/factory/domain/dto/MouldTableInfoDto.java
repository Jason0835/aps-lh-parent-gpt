package com.zlt.aps.factory.domain.dto;

import com.zlt.aps.factory.domain.vo.MouldDayProductionVo;
import com.zlt.aps.factory.domain.vo.MouldTableCycleProductionVo;
import com.zlt.aps.factory.enums.ProductionOrientEnum;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * 排产分组-模台信息对象
 *
 * @author ZLT
 * @date 20250715
 */
@Data
public class MouldTableInfoDto implements Serializable {
    /**
     * 排产分组编号值
     */
    private String productionGroupValue;
    /**
     * 当前排产模具
     */
    private String currentMouldCode;
    /**
     * 最后一个排产信息
     */
    private MouldDayProductionVo lastProductionInfo;
    /**
     * 排产信息
     */
    private List<MouldDayProductionVo> productionList;
    /**
     * 已排产日
     */
    private Set<Integer> productionDateSet;
    /**
     * 可排产日--初始化后不再变化
     */
    private Set<Integer> needProductionDateSet;
    /**
     * 最早和最晚日期数对象
     */
    private MouldTableCycleProductionVo cycleDate;

    /**
     * 获取剩余产能天数
     *
     * @return
     */
    public final Integer getLeftOverDays() {
        if (CollectionUtils.isEmpty(needProductionDateSet)) {
            return BigDecimal.ZERO.intValue();
        }
        Integer leftOverDay = BigDecimal.ZERO.intValue();
        for (Integer day : needProductionDateSet) {
            if (productionDateSet.contains(day)) {
                continue;
            }
            leftOverDay = leftOverDay + BigDecimal.ONE.intValue();
        }
        return leftOverDay;
    }

    /**
     * 根据排产方向，获取模台起始排产日
     *
     * @param productionOrient 排产方向
     * @return
     */
    public final Integer getStartDay(ProductionOrientEnum productionOrient) {
        if (null == lastProductionInfo && ProductionOrientEnum.FORWARD == productionOrient) {
            return cycleDate.getMinStartDay();
        }
        if (null == lastProductionInfo && ProductionOrientEnum.REVERSE == productionOrient) {
            return cycleDate.getMaxEndDay();
        }
        return lastProductionInfo.getProductionDate();
    }

    /**
     * 根据排产方向，获取第一个大于startDay的排产日
     * 如果排产方向为正向，则取第一个大于startDay的日期
     * 如果排产方向为反向，则取第一个小于startDay的日期
     *
     * @param productionOrient
     * @return
     */
    public final Integer getEndDay(ProductionOrientEnum productionOrient) {
        if (null == lastProductionInfo && ProductionOrientEnum.FORWARD == productionOrient) {
            return cycleDate.getMaxEndDay();
        }
        if (null == lastProductionInfo && ProductionOrientEnum.REVERSE == productionOrient) {
            return cycleDate.getMinStartDay();
        }
        Integer startDay = lastProductionInfo.getProductionDate();
        if (CollectionUtils.isEmpty(productionDateSet)) {
            if (ProductionOrientEnum.FORWARD == productionOrient) {
                return cycleDate.getMaxEndDay();
            }
            return cycleDate.getMinStartDay();
        }
        List<Integer> productionDateList = new ArrayList<>(productionDateSet);
        //正向
        if (ProductionOrientEnum.FORWARD == productionOrient) {
            productionDateList.sort(Comparator.comparing(Integer::intValue));
            Integer findDay = null;
            for (Integer day : productionDateList) {
                if (day > startDay) {
                    findDay = day;
                    break;
                }
            }
            if (null == findDay) {
                return cycleDate.getMaxEndDay();
            }
            return findDay;
        }
        //反向
        productionDateList.sort(Comparator.comparing(Integer::intValue, Comparator.reverseOrder()));
        Integer findDay = null;
        for (Integer day : productionDateList) {
            if (day < startDay) {
                findDay = day;
                break;
            }
        }
        if (null == findDay) {
            return cycleDate.getMinStartDay();
        }
        return findDay;
    }


    /**
     * 获取在startDay与endDay直接连续天数值
     *
     * @param startDay 起始天数
     * @param endDay   结束天数
     * @return
     */
    public final Integer getContinueDays(Integer startDay, Integer endDay) {
        Integer realStartDay = Math.min(startDay, endDay);
        Integer realEndDay = Math.max(startDay, endDay);
        if (realStartDay.equals(realEndDay)) {
            return BigDecimal.ZERO.intValue();
        }
        Integer continueDays = BigDecimal.ZERO.intValue();
        for (Integer start = realStartDay; start < realEndDay; start++) {
            if (needProductionDateSet.contains(start)) {
                continueDays = continueDays + BigDecimal.ONE.intValue();
            }
        }
        if (realStartDay.equals(cycleDate.getMinStartDay())) {
            continueDays = continueDays + BigDecimal.ONE.intValue();
        }
        if (realEndDay.equals(cycleDate.getMaxEndDay())) {
            continueDays = continueDays + BigDecimal.ONE.intValue();
        }
        return continueDays;
    }

    /**
     * 判断是否共生胎，不同模具排产模台
     *
     * @param embryoCode 生胎代码
     * @param mouldNo    模具
     * @return
     */
    public final boolean isSameEmbryoCodeAndNoMouldNo(String embryoCode, String mouldNo) {
        if (StringUtils.isBlank(embryoCode) || StringUtils.isNotBlank(mouldNo)) {
            return false;
        }
        if (null == lastProductionInfo) {
            return false;
        }
        return embryoCode.equals(lastProductionInfo.getEmbryoCode()) && !mouldNo.equals(lastProductionInfo.getMouldNo());
    }
}
