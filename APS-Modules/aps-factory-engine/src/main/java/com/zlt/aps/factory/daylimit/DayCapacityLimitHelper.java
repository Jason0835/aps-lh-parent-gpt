package com.zlt.aps.factory.daylimit;

import com.tlt.aps.constant.StringConstant;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.factory.domain.vo.ProductionMouldInfoVo;
import com.zlt.aps.factory.logrecorder.DayLimitLogRecorder;
import com.zlt.aps.factory.scheduling.cxcapacity.ProductionCapacityParamConfiguration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 日产能控制对象
 * 排产日、最大产能上限、最低产能，产能比例
 *
 * @author ZLT
 * @date 20250106
 */
@Slf4j
@Getter
public class DayCapacityLimitHelper implements Serializable {

    /**
     * 排产日
     */
    private Integer productionDay;
    /**
     * 每日产能上限
     */
    private Integer maxCapacity;
    /**
     * 每日产能下限
     */
    private Integer minCapacity;
    /**
     * 每日分配产能上限--用于结构产能分配
     */
    private Integer maxAllocationCapacity;
    /**
     * 每日切换结构次数上限
     */
    private Integer maxChangeCxMachineCount;
    /**
     * 每日换模硫化机台数上限
     */
    private Integer maxChangeLhMachineCount;
    /**
     * 产能比例值
     */
    private Integer capacityRatio;
    /**
     * 切换结构使用次数
     */
    private Integer usedChangeCxMachineCount;
    /**
     * 切换换模硫化机台使用次数
     */
    private Integer usedChangeLhMachineCount;
    /**
     * 成型分配量
     */
    private Integer cxMachineAllocationQty;
    /**
     * 总的排产量(包含换模等导致的损耗)
     */
    private Integer sumProductionCapacityQty;
    /**
     * 存储分组分配-占用产能信息
     */
    private Set<String> groupAllocationInfo;
    /**
     * 存储Sku排产信息
     */
    private Map<String, DayProductionCapacityDetailHelper> skuProductionInfo;
    /**
     * 存储切换：机台-分组信息
     * 机台|*|分组
     */
    private Set<String> changeCxMachineInfo;
    /**
     * 存储切换：物料-模具信息
     * 物料|*|模具
     */
    private Set<String> changeMouldInfo;
    /**
     * 机台|*|结构
     */
    private static final String KEY_FORMAT = "%s|*|%s";

    /**
     * 根据产能比例，构建初始的日产能限制对象
     *
     * @param productionDay      排产日
     * @param paramConfiguration 参数
     * @param ratio              比例 1~100的值
     * @return
     */
    public static DayCapacityLimitHelper createInit(Integer productionDay, ProductionCapacityParamConfiguration paramConfiguration, Integer ratio) {
        DayCapacityLimitHelper initLimit = new DayCapacityLimitHelper(productionDay);
        if (null != ratio) {
            initLimit.capacityRatio = ratio;
        }
        Integer dayMaxCapacity = paramConfiguration.getDayMaxCapacity();
        if (null != dayMaxCapacity) {
            Integer realDayMaxCapacity = BigDecimal.valueOf(dayMaxCapacity).multiply(BigDecimal.valueOf(ratio)).divide(BigDecimal.valueOf(ProductionConstant.PERCENTAGE), 0, RoundingMode.UP).intValue();
            initLimit.maxCapacity = realDayMaxCapacity;
            initLimit.maxAllocationCapacity = dayMaxCapacity;
        }
        Integer dayMinCapacity = paramConfiguration.getDayMinCapacity();
        if (null != dayMinCapacity) {
            initLimit.minCapacity = dayMinCapacity;
        }
        Integer changeCxMachineCount = paramConfiguration.getDayChangeGroupCount();
        if (null != changeCxMachineCount) {
            initLimit.maxChangeCxMachineCount = changeCxMachineCount;
        }
        Integer changeLhMachineCount = paramConfiguration.getChangeMouldLhMachineNumber();
        if (null != changeLhMachineCount) {
            initLimit.maxChangeLhMachineCount = changeLhMachineCount;
        }
        return initLimit;
    }

    /**
     * 重置换模、换结构、排产量
     * 使用量
     */
    public void resetUsedQty() {
        initUsedInfo();
    }

    /**
     * 是否还有剩余产能可分配
     * cxMachineAllocationQty < maxAllocationCapacity
     *
     * @return
     */
    public boolean isAllocationCapacity() {
        return cxMachineAllocationQty < maxAllocationCapacity;
    }

    /**
     * 增加成型分配产能-天产能占用量
     *
     * @param context           排产上下文
     * @param productionDay     排产日
     * @param addAllocationPlan 分配信息
     */
    public void addCxMachineAllocationQty(Context context, Integer productionDay, CxMachineAllocationPlanHelper addAllocationPlan) {
        if (!checkBeforeOperateResult(productionDay, addAllocationPlan)) {
            return;
        }
        String allocationKey = addAllocationPlan.getDayAllocationKey();
        if (groupAllocationInfo.contains(allocationKey)) {
            return;
        }
        groupAllocationInfo.add(allocationKey);
        Integer allocationQty = addAllocationPlan.getDayMinAllocationQty();
        cxMachineAllocationQty = cxMachineAllocationQty + allocationQty;
        log.info(DayLimitLogRecorder.addCxMachineGroupUsedLog(context, productionDay, allocationKey, allocationQty, cxMachineAllocationQty));
    }

    /**
     * 释放成型分配产能-天产能占用量
     *
     * @param context        排产上下文
     * @param productionDay  排产日
     * @param allocationInfo 分配信息
     */
    public void deductionCxMachineAllocationQty(Context context, Integer productionDay, CxMachineAllocationPlanHelper allocationInfo) {
        if (!checkBeforeOperateResult(productionDay, allocationInfo)) {
            return;
        }
        String allocationKey = allocationInfo.getDayAllocationKey();
        if (!groupAllocationInfo.contains(allocationKey)) {
            return;
        }
        groupAllocationInfo.remove(allocationKey);
        Integer allocationQty = allocationInfo.getDayMinAllocationQty();
        cxMachineAllocationQty = cxMachineAllocationQty - allocationQty;
        if (cxMachineAllocationQty <= BigDecimal.ZERO.intValue()) {
            cxMachineAllocationQty = BigDecimal.ZERO.intValue();
        }
        log.info(DayLimitLogRecorder.addDeductionCxMachineGroupUsedLog(context, productionDay, allocationKey, allocationQty, cxMachineAllocationQty));
    }

    /**
     * 是否可增加Sku排产
     * true表示可以， false表示不可以
     *
     * @return
     */
    public boolean isAddSkuProduction() {
        if (maxCapacity <= BigDecimal.ZERO.intValue()) {
            return false;
        }
        if (sumProductionCapacityQty <= BigDecimal.ZERO.intValue()) {
            return true;
        }
        return sumProductionCapacityQty < maxCapacity;
    }

    /**
     * 增加排产量
     *
     * @param context        上下文
     * @param productionDay  排产日
     * @param productionPlan 排产计划
     * @param doubleMould    排产模具
     * @param productionQty  排产量
     * @param lossQty        损耗量
     */
    public void addSkuDayProductionQty(Context context, Integer productionDay, MonthPlanProductionRequirePlanVo productionPlan, List<ProductionMouldInfoVo> doubleMould, Integer productionQty, Integer lossQty) {
        if (!checkBeforeOperateResult(productionDay, productionPlan, doubleMould, productionQty, lossQty)) {
            return;
        }
        Integer realProductionQty = getRealProductionQty(productionQty, lossQty);
        sumProductionCapacityQty = sumProductionCapacityQty + realProductionQty;
        String materialDesc = productionPlan.getMaterialDesc();
        DayProductionCapacityDetailHelper skuInfo = skuProductionInfo.get(materialDesc);
        if (null == skuInfo) {
            skuInfo = DayProductionCapacityDetailHelper.createInitEmpty(productionDay, materialDesc);
            skuProductionInfo.put(materialDesc, skuInfo);
        }
        Set<String> doubleMouldCode = doubleMould.stream().map(ProductionMouldInfoVo::getMouldCode).collect(Collectors.toSet());
        String mouldCodeInfo = String.join(StringConstant.COMMA, doubleMouldCode);
        skuInfo.addProductionQty(doubleMouldCode, productionQty, lossQty);
        log.info(DayLimitLogRecorder.addDayProductionInfoLog(context, productionDay, mouldCodeInfo, materialDesc, realProductionQty, productionQty, lossQty, sumProductionCapacityQty));
    }

    /**
     * 释放-排产量
     *
     * @param context       上下文
     * @param productionDay 排产日
     * @param materialDesc  排产计划
     * @param usedMouldSet  排产模具
     * @param productionQty 排产量
     * @param lossQty       损耗量
     */
    public void deductionSkuDayProductionQty(Context context, Integer productionDay, String materialDesc, Set<String> usedMouldSet, Integer productionQty, Integer lossQty) {
        if (null == productionDay || StringUtils.isBlank(materialDesc) || CollectionUtils.isEmpty(usedMouldSet)) {
            return;
        }
        Integer realProductionQty = getRealProductionQty(productionQty, lossQty);
        if (realProductionQty <= BigDecimal.ZERO.intValue()) {
            return;
        }
        DayProductionCapacityDetailHelper skuInfo = skuProductionInfo.get(materialDesc);
        if (null == skuInfo) {
            return;
        }
        String mouldCodeInfo = String.join(StringConstant.COMMA, usedMouldSet);
        sumProductionCapacityQty = sumProductionCapacityQty - realProductionQty;
        if (sumProductionCapacityQty <= BigDecimal.ZERO.intValue()) {
            sumProductionCapacityQty = BigDecimal.ZERO.intValue();
        }
        skuInfo.deductionProductionQty(usedMouldSet, productionQty, lossQty);
        log.info(DayLimitLogRecorder.addDeductionDayProductionInfoLog(context, productionDay, mouldCodeInfo, materialDesc, realProductionQty, productionQty, lossQty, sumProductionCapacityQty));
        if (skuInfo.getLossQty() == BigDecimal.ZERO.intValue() && skuInfo.getProductionQty() == BigDecimal.ZERO.intValue()) {
            skuProductionInfo.remove(materialDesc);
        }
    }

    /**
     * 获取剩余可使用切换分组量
     */
    public Integer getLeftOverUsedChangeGroupQty() {
        if (null == maxChangeCxMachineCount) {
            return BigDecimal.ZERO.intValue();
        }
        if (null == usedChangeCxMachineCount) {
            return maxChangeCxMachineCount;
        }
        if (maxChangeCxMachineCount <= usedChangeCxMachineCount) {
            return BigDecimal.ZERO.intValue();
        }
        return maxChangeCxMachineCount - usedChangeCxMachineCount;
    }

    /**
     * 增加切换分组使用数
     *
     * @param cxMachineCode 成型机台
     * @param groupName     分组计划名
     */
    public void addChangeGroupUsedQty(Context context, String cxMachineCode, String groupName) {
        if (StringUtils.isBlank(cxMachineCode) || StringUtils.isBlank(groupName)) {
            return;
        }
        String changeKey = String.format(KEY_FORMAT, cxMachineCode, groupName);
        if (changeCxMachineInfo.contains(changeKey)) {
            return;
        }
        changeCxMachineInfo.add(changeKey);
        usedChangeCxMachineCount = usedChangeLhMachineCount + BigDecimal.ONE.intValue();
        log.info(DayLimitLogRecorder.addChangeGroupUsedLog(context, productionDay, changeKey, usedChangeCxMachineCount));
    }

    /**
     * 增加切换分组使用数
     *
     * @param cxMachineCode 成型机台
     * @param groupName     分组计划名
     */
    public void deductionChangeGroupUsedQty(Context context, String cxMachineCode, String groupName) {
        if (StringUtils.isBlank(cxMachineCode) || StringUtils.isBlank(groupName)) {
            return;
        }
        String changeKey = String.format(KEY_FORMAT, cxMachineCode, groupName);
        if (!changeCxMachineInfo.contains(changeKey)) {
            return;
        }
        changeCxMachineInfo.remove(changeKey);
        usedChangeCxMachineCount = usedChangeLhMachineCount - BigDecimal.ONE.intValue();
        if (usedChangeCxMachineCount <= BigDecimal.ZERO.intValue()) {
            usedChangeCxMachineCount = BigDecimal.ZERO.intValue();
        }
        log.info(DayLimitLogRecorder.addDeductionChangeGroupUsedLog(context, productionDay, changeKey, sumProductionCapacityQty));
    }

    /**
     * 获取剩余可使用的换模次数量(机台)
     *
     * @return
     */
    public Integer getLeftOverUsedChangeMouldQty() {
        if (null == maxChangeLhMachineCount) {
            return BigDecimal.ZERO.intValue();
        }
        if (null == usedChangeLhMachineCount) {
            return maxChangeLhMachineCount;
        }
        if (maxChangeLhMachineCount <= usedChangeLhMachineCount) {
            return BigDecimal.ZERO.intValue();
        }
        return maxChangeLhMachineCount - usedChangeLhMachineCount;
    }

    /**
     * 增加换模使用量
     *
     * @param context      排产上下文
     * @param materialDesc Sku
     * @param mouldCodeSet 模具
     */
    public void addChangeMouldUsedQty(Context context, String materialDesc, Set<String> mouldCodeSet) {
        if (StringUtils.isBlank(materialDesc) || CollectionUtils.isEmpty(mouldCodeSet)) {
            return;
        }
        List<String> mouldCodeList = new ArrayList<>(mouldCodeSet);
        Collections.sort(mouldCodeList);
        String mouldInfo = String.join(StringConstant.COMMA, mouldCodeList);
        String changeKey = String.format(KEY_FORMAT, materialDesc, mouldInfo);
        if (changeMouldInfo.contains(changeKey)) {
            return;
        }
        changeMouldInfo.add(changeKey);
        usedChangeLhMachineCount = usedChangeLhMachineCount + BigDecimal.ONE.intValue();
        log.info(DayLimitLogRecorder.addChangeMouldUsedLog(context, productionDay, changeKey, usedChangeLhMachineCount));
    }

    /**
     * 增加换模使用量
     *
     * @param context      排产上下文
     * @param materialDesc Sku
     * @param mouldCode    模具
     */
    public void deductionChangeMouldUsedQty(Context context, String materialDesc, String mouldCode) {
        if (StringUtils.isBlank(materialDesc) || StringUtils.isBlank(mouldCode)) {
            return;
        }
        if (CollectionUtils.isEmpty(changeMouldInfo)) {
            return;
        }
        String startWith = String.format(KEY_FORMAT, materialDesc, "");
        List<String> findList = changeMouldInfo.stream().filter(singleChange -> singleChange.startsWith(startWith)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(findList)) {
            return;
        }
        List<String> confirmList = new ArrayList<>();
        findList.forEach(singleChange -> {
            String[] jointArrays = singleChange.split(ProductionConstant.JOINT_SPLIT);
            String mouldInfo = jointArrays[BigDecimal.ONE.intValue()];
            if (StringUtils.isBlank(mouldInfo)) {
                return;
            }
            Set<String> mouldCodeSet = Stream.of(mouldInfo.split(StringConstant.COMMA)).collect(Collectors.toSet());
            if (mouldCodeSet.contains(mouldCode)) {
                confirmList.add(singleChange);
            }
        });
        if (CollectionUtils.isEmpty(confirmList)) {
            return;
        }
        String changeKey = confirmList.get(BigDecimal.ZERO.intValue());
        changeMouldInfo.remove(changeKey);
        usedChangeLhMachineCount = usedChangeLhMachineCount - BigDecimal.ONE.intValue();
        if (usedChangeLhMachineCount <= BigDecimal.ZERO.intValue()) {
            usedChangeLhMachineCount = BigDecimal.ZERO.intValue();
        }
        log.info(DayLimitLogRecorder.addDeductionChangeMouldUsedLog(context, productionDay, changeKey, usedChangeLhMachineCount));
    }

    /**
     * 操作前的检查结果
     * 检查排产日 是否与当前日符合
     * 检查productionDay与allocationInfo是否为空对象
     * 检查allocationInfo是否有分组信息和成型机台信息
     *
     * @param productionDay  排产日
     * @param allocationInfo 分配信息
     * @return
     */
    private boolean checkBeforeOperateResult(Integer productionDay, CxMachineAllocationPlanHelper allocationInfo) {
        if (null == allocationInfo || null == productionDay) {
            return false;
        }
        if (!productionDay.equals(this.productionDay)) {
            return false;
        }
        if (null == allocationInfo.getProductionPlanInfo() || StringUtils.isBlank(allocationInfo.getCxMachineCode())) {
            return false;
        }
        return true;
    }

    /**
     * 是否有效参数
     * <p>
     * true 有效 false 无效
     *
     * @param productionDay  排产日
     * @param productionPlan 排产计划(不关注具体的计划ID)
     * @param doubleMould    排产模具
     * @param productionQty  排产量
     * @param lossQty        损耗量
     * @return
     */
    private boolean checkBeforeOperateResult(Integer productionDay, MonthPlanProductionRequirePlanVo productionPlan, List<ProductionMouldInfoVo> doubleMould, Integer productionQty, Integer lossQty) {
        if (null == productionDay || null == productionPlan || CollectionUtils.isEmpty(doubleMould)) {
            return false;
        }
        Set<String> mouldCodeSet = doubleMould.stream().map(ProductionMouldInfoVo::getMouldCode).collect(Collectors.toSet());
        if (StringUtils.isBlank(productionPlan.getMaterialDesc()) || CollectionUtils.isEmpty(mouldCodeSet) || ProductionConstant.DOUBLE_MOULD_PRODUCTION != mouldCodeSet.size()) {
            return false;
        }
        Integer realProductionQty = getRealProductionQty(productionQty, lossQty);
        return realProductionQty > BigDecimal.ZERO.intValue();
    }

    /**
     * 获取实际排产量
     * 实际排产量 = 排产量 + 损耗量
     *
     * @param productionQty 排产量
     * @param lossQty       损耗量
     * @return
     */
    private Integer getRealProductionQty(Integer productionQty, Integer lossQty) {
        Integer realProductionQty = BigDecimal.ZERO.intValue();
        if (null != productionQty && productionQty > BigDecimal.ZERO.intValue()) {
            realProductionQty = realProductionQty + productionQty;
        }
        if (null != lossQty && lossQty > BigDecimal.ZERO.intValue()) {
            realProductionQty = realProductionQty + lossQty;
        }
        return realProductionQty;
    }

    /**
     * 构建初始的
     *
     * @param productionDay
     */
    private DayCapacityLimitHelper(Integer productionDay) {
        this.productionDay = productionDay;
        this.maxCapacity = Integer.MAX_VALUE;
        this.maxAllocationCapacity = Integer.MAX_VALUE;
        this.minCapacity = BigDecimal.ZERO.intValue();
        this.capacityRatio = ProductionConstant.PERCENTAGE;
        this.maxChangeCxMachineCount = Integer.MAX_VALUE;
        this.maxChangeLhMachineCount = Integer.MAX_VALUE;
        this.initUsedInfo();
    }

    /**
     * 初始化使用量信息
     */
    private void initUsedInfo() {
        this.cxMachineAllocationQty = BigDecimal.ZERO.intValue();
        this.sumProductionCapacityQty = BigDecimal.ZERO.intValue();
        this.usedChangeCxMachineCount = BigDecimal.ZERO.intValue();
        this.usedChangeLhMachineCount = BigDecimal.ZERO.intValue();
        this.groupAllocationInfo = new HashSet<>();
        this.skuProductionInfo = new HashMap<>();
        this.changeCxMachineInfo = new HashSet<>();
        this.changeMouldInfo = new HashSet<>();
    }
}
