package com.zlt.aps.factory.daylimit;

import com.tlt.aps.constant.StringConstant;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.Context;
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
     * 总的排产量(包含换模等导致的损耗)
     */
    private Integer sumProductionCapacityQty;
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
            Integer realDayMaxCapacity = BigDecimal.valueOf(dayMaxCapacity).multiply(BigDecimal.valueOf(ProductionConstant.PERCENTAGE)).divide(BigDecimal.valueOf(ratio), 0, RoundingMode.UP).intValue();
            initLimit.maxCapacity = realDayMaxCapacity;
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
     * 构建初始的
     *
     * @param productionDay
     */
    private DayCapacityLimitHelper(Integer productionDay) {
        this.productionDay = productionDay;
        this.maxCapacity = Integer.MAX_VALUE;
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
        this.usedChangeCxMachineCount = BigDecimal.ZERO.intValue();
        this.usedChangeLhMachineCount = BigDecimal.ZERO.intValue();
        this.sumProductionCapacityQty = BigDecimal.ZERO.intValue();
        this.changeCxMachineInfo = new HashSet<>();
        this.changeMouldInfo = new HashSet<>();
    }
}
