package com.zlt.aps.maindata.domain.vo;

import com.tlt.aps.enums.FormingMethodTypeEnum;
import com.tlt.aps.enums.WorkWearTypeEnum;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Set;

/**
 * 寸口产能配置-参数
 *
 * @author ZLT
 * @date 20250709
 */
@Data
public class SizeCapacityParamVo implements Serializable {
    /**
     * 一次法扣减的产能量
     */
    private Integer oneMouldMethodSubtractQty;
    /**
     * 二次法扣减的产能量
     */
    private Integer twoMouldMethodSubtractQty;
    /**
     * 额外扣减的次数
     */
    private Integer additionalCount;
    /**
     * 默认的切换次数
     */
    private Integer defaultCount;
    /**
     * 月份最大天数--剔除停工日(动态计算-后续补充值)
     */
    private Integer monthMaxDays;
    /**
     * 剩余天数不进行切换寸口
     */
    private Integer maxLeftOverDays;
    /**
     * 最小分配天数
     */
    private Integer minAllocationDay;
    /**
     * 是否开启大寸口挤占在产寸口需求
     */
    private String openCrowdOut;
    /**
     * 18寸二次法大鼓机台
     */
    private String bigDrumCapacityValue;
    /**
     * 18寸二次法最大工装数
     */
    private Integer maxLimitCapacity;
    /**
     * 18寸二次法已分配成型信息
     */
    private Set<String> specialRestrictionSet;
    /**
     * 20寸一次法工装数
     */
    private Integer maxWorkWeekNumber;
    /**
     * 20寸一次法已分配成型信息
     */
    private Set<String> limitRestrictionSet;

    /**
     * 18寸二次法其它鼓最大成型产能数-即工装数
     *
     * @return
     */
    public int getMaxLimitCapacityNumber() {
        int bigDrumCapacityNumber = BigDecimal.ZERO.intValue();
        if (StringUtils.isNotBlank(bigDrumCapacityValue)) {
            String[] capacityConfigurations = bigDrumCapacityValue.split(",");
            bigDrumCapacityNumber = capacityConfigurations.length;
        }
        return maxLimitCapacity - bigDrumCapacityNumber;
    }

    /**
     * 判断是否达到分配限制
     * 1、是否达到二次法18寸限制数
     * 因大鼓机台不可换其它鼓，故而非大鼓工装数 = 18寸二次法总工装数 - 大鼓工装数(特定机台)
     * 2、是否达到一次法20寸鼓的总数限制
     *
     * @param proSize            寸口
     * @param workWearType       工装类别
     * @param mouldMethod        成型法
     * @param tireFabricNumber   胎体布层级
     * @param moldingMachineCode 成型产能
     * @return
     */
    public boolean isReachLimitNumber(BigDecimal proSize, String workWearType, String mouldMethod, Integer tireFabricNumber, String moldingMachineCode) {
        //18寸、二次法的特殊校验
        if (WorkWearTypeEnum.PRO_SIZE_18.equals(proSize) && FormingMethodTypeEnum.TWO_STAGE_TIRE.getMethodValue().equals(mouldMethod)) {
            return isReachLimitBy18TwoStageTire(proSize, workWearType, mouldMethod, tireFabricNumber, moldingMachineCode);
        }
        //20寸，一次法 鼓只有1个
        if (WorkWearTypeEnum.PRO_SIZE_20.equals(proSize) && FormingMethodTypeEnum.SINGLE_STAGE_TIRE.getMethodValue().equals(mouldMethod)) {
            return isReachLimitBy20SingleStageTire(proSize, workWearType, mouldMethod, tireFabricNumber, moldingMachineCode);
        }
        return false;
    }

    /**
     * 寸口产能分配
     * 1、增加已分配的18寸二次法非大鼓成型信息
     * 用以控制18寸二次法总工装数限定
     * 2、增加已分配的20寸一次法成型信息
     * 用以控制20寸一次法总工装数限定
     *
     * @param proSize            寸口
     * @param workWearType       工装类别
     * @param mouldMethod        成型法
     * @param tireFabricNumber   胎体布层级
     * @param moldingMachineCode 成型产能
     */
    public void addSpecialRestriction(BigDecimal proSize, String workWearType, String mouldMethod, Integer tireFabricNumber, String moldingMachineCode) {
        //18寸、二次法的特殊校验
        if (WorkWearTypeEnum.PRO_SIZE_18.equals(proSize) && FormingMethodTypeEnum.TWO_STAGE_TIRE.getMethodValue().equals(mouldMethod)) {
            addSpecialRestrictionBy18TwoStageTire(proSize, workWearType, mouldMethod, tireFabricNumber, moldingMachineCode);
        }
        //20寸，一次法 鼓只有1个
        if (WorkWearTypeEnum.PRO_SIZE_20.equals(proSize) && FormingMethodTypeEnum.SINGLE_STAGE_TIRE.getMethodValue().equals(mouldMethod)) {
            addSpecialRestrictionBy20SingleStageTire(proSize, workWearType, mouldMethod, tireFabricNumber, moldingMachineCode);
        }
    }

    /**
     * 18寸二次法，非大鼓限制
     *
     * @param proSize            寸口
     * @param workWearType       工装类型
     * @param mouldMethod        成型法
     * @param tireFabricNumber   胎体层级
     * @param moldingMachineCode 成型产能
     * @return
     */
    private boolean isReachLimitBy18TwoStageTire(BigDecimal proSize, String workWearType, String mouldMethod, Integer tireFabricNumber, String moldingMachineCode) {
        //不是18寸
        if (!WorkWearTypeEnum.PRO_SIZE_18.equals(proSize)) {
            return false;
        }
        //不是二次法
        if (!FormingMethodTypeEnum.TWO_STAGE_TIRE.getMethodValue().equals(mouldMethod)) {
            return false;
        }
        //跳过大鼓
        if (WorkWearTypeEnum.BIG_DRUM.getTypeValue().equals(workWearType)) {
            return false;
        }
        //跳过已分配
        if (specialRestrictionSet.contains(moldingMachineCode)) {
            return false;
        }
        int max = getMaxLimitCapacityNumber() - BigDecimal.ONE.intValue();
        int size = specialRestrictionSet.size();
        if (size <= max) {
            return false;
        }
        return true;
    }

    /**
     * 20寸，一次法最大工装数限制
     * false 表示没有达到限制 true 表示达到限制
     *
     * @param proSize            寸口
     * @param workWearType       工装类型
     * @param mouldMethod        成型法
     * @param tireFabricNumber   胎体层级
     * @param moldingMachineCode 成型产能
     * @return
     */
    private boolean isReachLimitBy20SingleStageTire(BigDecimal proSize, String workWearType, String mouldMethod, Integer tireFabricNumber, String moldingMachineCode) {
        //不是20寸
        if (!WorkWearTypeEnum.PRO_SIZE_20.equals(proSize)) {
            return false;
        }
        //不是一次法
        if (!FormingMethodTypeEnum.SINGLE_STAGE_TIRE.getMethodValue().equals(mouldMethod)) {
            return false;
        }
        //跳过已分配
        if (limitRestrictionSet.contains(moldingMachineCode)) {
            return false;
        }
        int max = Integer.MAX_VALUE;
        if (null != maxWorkWeekNumber) {
            max = maxWorkWeekNumber;
        }
        int size = limitRestrictionSet.size();
        if (size < max) {
            return false;
        }
        return true;
    }

    /**
     * 18寸、二次法 非大鼓分配处理
     *
     * @param proSize            寸口
     * @param workWearType       工装类别
     * @param mouldMethod        成型法
     * @param tireFabricNumber   胎体层级
     * @param moldingMachineCode 成型产能
     */
    private void addSpecialRestrictionBy18TwoStageTire(BigDecimal proSize, String workWearType, String mouldMethod, Integer tireFabricNumber, String moldingMachineCode) {
        //已分配跳过
        if (specialRestrictionSet.contains(moldingMachineCode)) {
            return;
        }
        //不是18寸
        if (!WorkWearTypeEnum.PRO_SIZE_18.equals(proSize)) {
            return;
        }
        //不是二次法
        if (!FormingMethodTypeEnum.TWO_STAGE_TIRE.getMethodValue().equals(mouldMethod)) {
            return;
        }
        //大鼓
        if (WorkWearTypeEnum.BIG_DRUM.getTypeValue().equals(workWearType)) {
            return;
        }
        specialRestrictionSet.add(moldingMachineCode);
    }


    /**
     * 18寸、二次法 非大鼓分配处理
     *
     * @param proSize            寸口
     * @param workWearType       工装类别
     * @param mouldMethod        成型法
     * @param tireFabricNumber   胎体层级
     * @param moldingMachineCode 成型产能
     */
    private void addSpecialRestrictionBy20SingleStageTire(BigDecimal proSize, String workWearType, String mouldMethod, Integer tireFabricNumber, String moldingMachineCode) {
        //已分配跳过
        if (limitRestrictionSet.contains(moldingMachineCode)) {
            return;
        }
        //不是20寸
        if (!WorkWearTypeEnum.PRO_SIZE_20.equals(proSize)) {
            return;
        }
        //不是一次法
        if (!FormingMethodTypeEnum.SINGLE_STAGE_TIRE.getMethodValue().equals(mouldMethod)) {
            return;
        }
        limitRestrictionSet.add(moldingMachineCode);
    }
}
