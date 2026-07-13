package com.zlt.aps.mp.engine.utils;

import com.zlt.aps.mp.engine.domain.dto.ProductGroupCxCapacityInfo;
import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;

/**
 * 排产业务中的排序工具类
 *
 * @author ZLT
 * @date 20260517
 */
@Slf4j
public class ProductionComparatorUtils {

    /**
     * sandy+ 2026.3.26
     * 分组(结构)在产机台多于需求要求机台时，续作机台优先下机选择顺序
     * 优先释放通用性好的（固定结构优先级差的、固定结构种类数多的），配比大的，成型编号大的
     *
     * @return
     */
    public static Comparator getContinueCxMachineOffSort() {
        return Comparator.comparing(ProductGroupCxCapacityInfo::getFixedPriority, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ProductGroupCxCapacityInfo::getFixedProSizeTypes, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ProductGroupCxCapacityInfo::getMaxLhMachineCount, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ProductGroupCxCapacityInfo::getCxMachineCode, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    /**
     * sandy+ 2026.3.26
     * 分组(结构)在产机台 = 需求预估机台时，标记预计可能下机选择顺序
     * 优先释放通用性好的（固定结构优先级差的、固定结构种类数多的）
     *
     * @return
     */
    public static Comparator getContinueCxMachinePreOffSort() {
        return Comparator.comparing(CxMachineBaseInfoVo::getFixedPriority, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(CxMachineBaseInfoVo::getFixedProSizeTypes, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    /**
     * 分组匹配可生产机台的排产
     * 生产切换效率优先
     * 1、同规格优先
     * 2、同英寸优先
     * 3、断面宽差值优先
     * 4、近1个月生产最近优先
     * 5、近3个月生产最多优先
     * 6、无零度供料架优先
     * 7、成型编号大优先
     *
     * @return
     */
    public static Comparator getEfficiencyPrioritySort() {
        return Comparator.comparing(CxMachineBaseInfoVo::getSameSpecifications, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(CxMachineBaseInfoVo::getSameProSize, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(CxMachineBaseInfoVo::getSectionWidthCondition, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(CxMachineBaseInfoVo::getLastBoardingDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(CxMachineBaseInfoVo::getProductionCount, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(CxMachineBaseInfoVo::getIsZeroRack)
                .thenComparing(CxMachineBaseInfoVo::getCxMachineCode, Comparator.reverseOrder());
    }

    /**
     * 分组匹配可生产机台的排序
     * 前提条件：
     * 已经设置CxMachineBaseInfoVo:CxMachineGroupPriorityValueHelper比较属性
     * 在以下场景下isProductionLongTime = true
     * 1、固定机台优先排产
     * 2、间断分组优先排产
     * 3、分组有多段分配优先排产
     * 其他场景 isProductionLongTime = false
     * isProductionLongTime = true则按生产时间长优先
     * 否则按优先级优先
     *
     * @param isProductionLongTime true|false
     * @return
     */
    public static Comparator getMatchPrioritySort(boolean isProductionLongTime) {
        if (isProductionLongTime) {
            return getProductionDayLongSort();
        }
        return getPrioritySort();
    }

    /**
     * 前提条件：分组计划已经找出可生产的成型机台
     * 挑选可生产匹配机台，按匹配优先级优先规则
     * 1、前后规格按机台优先级优先
     * 2、差量小优先
     *
     * @return
     */
    private static Comparator getPrioritySort() {
        return Comparator.comparing(CxMachineBaseInfoVo::getSelectedPriorityValue)
                .thenComparing(CxMachineBaseInfoVo::getSelectedPriorityDiffValue);
    }

    /**
     * 前提条件：分组计划已经找出可生产的成型机台
     * 使用场景：
     * 1、固定机台优先排产
     * 2、间断分组优先排产
     * 3、分组有多段分配优先排产
     * 挑选可生产匹配机台，按生产时间长优先规则
     * 1、可生产时间长的优先
     * 2、前后规格按机台优先级优先
     * 3、差量小优先
     *
     * @return
     */
    private static Comparator getProductionDayLongSort() {
        return Comparator.comparing(CxMachineBaseInfoVo::getSelectedProductionDays, Comparator.reverseOrder())
                .thenComparing(CxMachineBaseInfoVo::getSelectedPriorityValue)
                .thenComparing(CxMachineBaseInfoVo::getSelectedPriorityDiffValue);
    }
}
