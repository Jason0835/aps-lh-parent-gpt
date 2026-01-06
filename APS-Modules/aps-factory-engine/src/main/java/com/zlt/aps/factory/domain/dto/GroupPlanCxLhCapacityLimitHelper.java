package com.zlt.aps.factory.domain.dto;

import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.factory.domain.vo.MonthPlanStructureLhRatioVo;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import com.zlt.aps.monthplan.api.domain.entity.MpStructureAllocation;
import lombok.Getter;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 分组计划 - TBR为结构，PCR为英寸(寸口、寸别)
 * 成型硫化产能限制信息对象
 * 最大胎胚种类数
 * 最大硫化机台数
 * 实单最低硫化机台数
 * 实际已排产的胎胚信息
 * 实际已排产的模具信息
 * <p>
 * 用以值传递，没有其它特殊含义
 *
 * @author ZLT
 * @date 20251229
 */
@Getter
public class GroupPlanCxLhCapacityLimitHelper {

    /**
     * 排产日 处于排产周期内第几天
     */
    private Integer day;

    /**
     * 最大胎胚种类数
     */
    private Integer maxEmbryoCodeCount;

    /**
     * 最大硫化机台数
     */
    private Integer maxLhMachineCount;

    /**
     * 实单最低硫化机台数,到机台需要进行组合
     */
    private Map<String, Integer> minLhMachineInfo;
    /**
     * 实际排产的胎胚信息
     */
    private Set<String> productionEmbryoCodeSet;

    /**
     * 实际排产的模具信息
     */
    private Set<String> productionMouldSet;
    /**
     * 排产的Sku排产量信息
     */
    private Map<String, SkuDayProductionInfoHelper> productionSkuQtyInfo;
    /**
     * 成型机台集合
     */
    private Set<String> cxMachineCodeSet;

    /**
     * 根据续作在产机台分配情况，构建日排产限制数据对象
     *
     * @param context                     排产上下文
     * @param productionDay               排产日
     * @param continueCxMachineAllocation 在产机台信息
     * @return
     */
    public static GroupPlanCxLhCapacityLimitHelper buildByContinueCxMachineAllocation(Context context, Integer productionDay, List<CxMachineAllocationPlanHelper> continueCxMachineAllocation) {
        if (context.getStopDays().contains(productionDay)) {
            return null;
        }
        Integer minLimit = BigDecimal.ZERO.intValue();
        GroupPlanCxLhCapacityLimitHelper initLimitHelper = buildEmptyData(productionDay, minLimit, minLimit);
        if (CollectionUtils.isEmpty(continueCxMachineAllocation)) {
            return initLimitHelper;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Map<String, CxMachineBaseInfoVo> cxMachineBaseInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        continueCxMachineAllocation.forEach(singleCxMachineAllocation -> {
            String cxMachineCode = singleCxMachineAllocation.getCxMachineCode();
            CxMachineBaseInfoVo cxMachineInfo = cxMachineBaseInfo.get(cxMachineCode);
            updateBaseLimitInfo(initLimitHelper, cxMachineInfo, singleCxMachineAllocation);
        });
        return initLimitHelper;
    }

    /**
     * 根据结构转产配置，构建分组某日的限制对象信息
     *
     * @param context             排产上下文
     * @param productionDay       排产日
     * @param groupAllocationList 分组产能配置
     * @return
     */
    public static GroupPlanCxLhCapacityLimitHelper buildByStructureAllocation(Context context, Integer productionDay, List<MpStructureAllocation> groupAllocationList) {
        if (context.getStopDays().contains(productionDay)) {
            return null;
        }
        Integer minLimit = BigDecimal.ZERO.intValue();
        GroupPlanCxLhCapacityLimitHelper initLimitHelper = buildEmptyData(productionDay, minLimit, minLimit);
        if (CollectionUtils.isEmpty(groupAllocationList)) {
            return initLimitHelper;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Map<String, CxMachineBaseInfoVo> cxMachineBaseInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        groupAllocationList.forEach(singleCxMachineAllocation -> {
            String cxMachineCode = singleCxMachineAllocation.getCxMachineCode();
            CxMachineBaseInfoVo cxMachineInfo = cxMachineBaseInfo.get(cxMachineCode);
            updateBaseLimitInfo(initLimitHelper, cxMachineInfo, singleCxMachineAllocation);
        });
        return initLimitHelper;
    }

    /**
     * 更新数据
     * 最大胎胚种类数
     * 最大硫化机台数
     * 实单最小硫化机台数
     *
     * @param updateInfo
     */
    public void updateInfo(GroupPlanCxLhCapacityLimitHelper updateInfo) {
        if (!day.equals(updateInfo.getDay())) {
            return;
        }
        maxEmbryoCodeCount = updateInfo.getMaxEmbryoCodeCount();
        maxLhMachineCount = updateInfo.getMaxLhMachineCount();
        minLhMachineInfo.putAll(updateInfo.getMinLhMachineInfo());
        cxMachineCodeSet.addAll(updateInfo.getCxMachineCodeSet());
    }

    /**
     * @param previousLimit
     * @param releaseLhMachineCount
     * @return
     */
    public SkuDayProductionInfoHelper getEarliestConclusionSkuInfo(GroupPlanCxLhCapacityLimitHelper previousLimit, Integer releaseLhMachineCount) {
        Map<String, Integer> previousSkuUsedMachine = previousLimit.getSkuTheoryUsedMachine();
        Map<String, Integer> currentSkuUsedMachine = getSkuUsedMachineRejectLeftOver();
        List<String> reductionSkuList = new ArrayList<>();
        previousSkuUsedMachine.forEach((materialDesc, usedMachineCount) -> {
            Integer leaveCount = currentSkuUsedMachine.get(materialDesc);
            if (null == leaveCount) {
                leaveCount = BigDecimal.ZERO.intValue();
            }
            Integer reductionCount = usedMachineCount - leaveCount;
            if (reductionCount <= BigDecimal.ZERO.intValue()) {
                return;
            }
            for (int index = BigDecimal.ONE.intValue(); index <= reductionCount; index++) {
                reductionSkuList.add(materialDesc);
            }
        });
        reductionSkuList.sort(Comparator.naturalOrder());
        String selected = reductionSkuList.get(releaseLhMachineCount - BigDecimal.ONE.intValue());
        return previousLimit.getProductionSkuQtyInfo().get(selected);
    }

    /**
     * 判断能否加一台硫化
     * 如果改日已经
     *
     * @return
     */
    public boolean isAddOneLhMachine(String embryoCode) {
        Integer currentLhMachineCount = getProductionLhMachineCount();
        Integer currentEmbryoCodeCount = productionEmbryoCodeSet.size();
        if (!productionMouldSet.contains(embryoCode) && currentEmbryoCodeCount >= maxEmbryoCodeCount) {
            return false;
        }

        if (currentLhMachineCount >= maxLhMachineCount) {
            return false;
        }
        return true;
    }

    /**
     * 判断是否达到限制条件
     * 胎胚种类数没有超
     * 硫化机台数没有超
     *
     * @return
     */
    public boolean isReachLimit() {
        Integer currentEmbryoCodeCount = productionEmbryoCodeSet.size();
        Integer currentLhMachineCount = getProductionLhMachineCount();
        if (currentEmbryoCodeCount >= maxEmbryoCodeCount) {
            return true;
        }
        if (currentLhMachineCount >= maxLhMachineCount) {
            return true;
        }
        return false;
    }

    /**
     * 获取当前使用的硫化机台数
     *
     * @return
     */
    public Integer getUsedLhMachineCount() {
        return getProductionLhMachineCount();
    }

    /**
     * 构建空数据对象实例
     *
     * @param day                排产日
     * @param maxEmbryoCodeCount 最大胎胚种类数
     * @param maxLhMachineCount  最大硫化机台数
     * @return
     */
    public static GroupPlanCxLhCapacityLimitHelper buildEmptyData(Integer day, Integer maxEmbryoCodeCount, Integer maxLhMachineCount) {
        if (null == day) {
            return null;
        }
        GroupPlanCxLhCapacityLimitHelper limitHelper = new GroupPlanCxLhCapacityLimitHelper(day, maxEmbryoCodeCount, maxLhMachineCount);
        return limitHelper;
    }

    /**
     * 构造函数
     *
     * @param day                排产日
     * @param maxEmbryoCodeCount 最大胎胚种类数
     * @param maxLhMachineCount  最大硫化机台数
     */
    public GroupPlanCxLhCapacityLimitHelper(Integer day, Integer maxEmbryoCodeCount, Integer maxLhMachineCount) {
        this.day = day;
        this.maxEmbryoCodeCount = maxEmbryoCodeCount;
        this.maxLhMachineCount = maxLhMachineCount;
        this.minLhMachineInfo = new HashMap<>();
        this.productionEmbryoCodeSet = new HashSet<>();
        this.productionMouldSet = new HashSet<>();
        this.cxMachineCodeSet = new HashSet<>();
        this.productionSkuQtyInfo = new HashMap<>();
    }

    /**
     * 获取Sku占用的硫化机台数，忽略余量
     *
     * @return
     */
    private Map<String, Integer> getSkuUsedMachineRejectLeftOver() {
        if (CollectionUtils.isEmpty(productionSkuQtyInfo)) {
            return Collections.emptyMap();
        }
        Map<String, Integer> skuUsedLhMachine = new HashMap<>();
        productionSkuQtyInfo.forEach((materialDesc, skuProductionInfo) -> {
            Integer productionQty = skuProductionInfo.getSumProductionQty();
            Integer lhMachineQty = skuProductionInfo.getDayLhMachineQty();
            //表示换模或是换活字块
            if (productionQty < lhMachineQty) {
                return;
            }
            int wholeNumber = productionQty / lhMachineQty;
            if (wholeNumber >= BigDecimal.ONE.intValue()) {
                skuUsedLhMachine.put(materialDesc, wholeNumber);
            }
        });
        return skuUsedLhMachine;
    }

    /**
     * 获取Sku理论占用的硫化机台数，不忽略余量
     *
     * @return
     */
    private Map<String, Integer> getSkuTheoryUsedMachine() {
        if (CollectionUtils.isEmpty(productionSkuQtyInfo)) {
            return Collections.emptyMap();
        }
        Map<String, Integer> skuUsedLhMachine = new HashMap<>();
        productionSkuQtyInfo.forEach((materialDesc, skuProductionInfo) -> {
            Integer productionQty = skuProductionInfo.getSumProductionQty();
            Integer lhMachineQty = skuProductionInfo.getDayLhMachineQty();
            int upMachineCount = BigDecimal.valueOf(productionQty).divide(BigDecimal.valueOf(lhMachineQty), 0, RoundingMode.UP).intValue();
            skuUsedLhMachine.put(materialDesc, upMachineCount);
        });
        return skuUsedLhMachine;
    }

    /**
     * 获取使用的硫化机台数
     *
     * @return
     */
    private Integer getProductionLhMachineCount() {
        if (CollectionUtils.isEmpty(productionSkuQtyInfo)) {
            return BigDecimal.ZERO.intValue();
        }
        Map<String, Integer> wholeLhMap = new HashMap<>();
        Set<String> passLeftOverSet = new HashSet<>();
        Set<String> noWholeSet = new HashSet<>();
        productionSkuQtyInfo.forEach((materialDesc, skuProductionInfo) -> {
            Integer productionQty = skuProductionInfo.getSumProductionQty();
            Integer lhMachineQty = skuProductionInfo.getDayLhMachineQty();
            //表示换模或是换活字块
            if (productionQty < lhMachineQty) {
                noWholeSet.add(materialDesc);
                return;
            }
            //表示自己整台或是有余量
            int remainder = productionQty % lhMachineQty;
            int wholeNumber = productionQty / lhMachineQty;
            if (remainder > BigDecimal.ZERO.intValue()) {
                passLeftOverSet.add(materialDesc);
            }
            wholeLhMap.put(materialDesc, wholeNumber);
        });
        Integer sumCount = BigDecimal.ZERO.intValue();
        if (!CollectionUtils.isEmpty(wholeLhMap)) {
            sumCount = sumCount + wholeLhMap.values().stream().mapToInt(Integer::intValue).sum();
        }
        Integer leftOver = Math.max(passLeftOverSet.size(), noWholeSet.size());
        return sumCount + leftOver;
    }

    /**
     * 根据结构转产配置，更新基础的限制信息
     * 胎胚种类数
     * 最大硫化配比
     * 实单最低硫化配比
     *
     * @param initLimitHelper           初始的限制信息
     * @param cxMachineInfo             成型机台信息
     * @param singleCxMachineAllocation 某条转产配置
     */
    private static void updateBaseLimitInfo(GroupPlanCxLhCapacityLimitHelper initLimitHelper, CxMachineBaseInfoVo cxMachineInfo, CxMachineAllocationPlanHelper singleCxMachineAllocation) {
        Integer productionDay = initLimitHelper.getDay();
        String cxMachineCode = cxMachineInfo.getCxMachineCode();
        if (null == cxMachineInfo) {
            return;
        }
        if (cxMachineInfo.getStopDayInfo().contains(productionDay)) {
            return;
        }
        if (initLimitHelper.getCxMachineCodeSet().contains(cxMachineCode)) {
            return;
        }
        MonthPlanStructureLhRatioVo lhRatio = singleCxMachineAllocation.getProductionPlanInfo().getLhRatio(cxMachineInfo);
        if (null == lhRatio) {
            return;
        }
        initLimitHelper.getCxMachineCodeSet().add(cxMachineCode);
        //最大硫化配比
        Integer maxLhMachineCount = initLimitHelper.getMaxLhMachineCount();
        maxLhMachineCount = maxLhMachineCount + lhRatio.getLhMachineMaxQty();
        initLimitHelper.maxLhMachineCount = maxLhMachineCount;
        //最低硫化配比
        initLimitHelper.getMinLhMachineInfo().put(cxMachineCode, lhRatio.getLhMachineMinQty());
        //最大胎胚种类数
        Integer maxEmbryoCodeCount = initLimitHelper.getMaxEmbryoCodeCount();
        maxEmbryoCodeCount = maxLhMachineCount + lhRatio.getMaxEmbryoQty();
        initLimitHelper.maxEmbryoCodeCount = maxEmbryoCodeCount;
    }

    /**
     * 根据结构转产配置，更新基础的限制信息
     * 胎胚种类数
     * 最大硫化配比
     * 实单最低硫化配比
     *
     * @param initLimitHelper           初始的限制信息
     * @param cxMachineInfo             成型机台信息
     * @param singleCxMachineAllocation 某条转产配置
     */
    private static void updateBaseLimitInfo(GroupPlanCxLhCapacityLimitHelper initLimitHelper, CxMachineBaseInfoVo cxMachineInfo, MpStructureAllocation singleCxMachineAllocation) {
        Integer productionDay = initLimitHelper.getDay();
        String cxMachineCode = cxMachineInfo.getCxMachineCode();
        if (null == cxMachineInfo) {
            return;
        }
        if (cxMachineInfo.getStopDayInfo().contains(productionDay)) {
            return;
        }
        //理论不可重复
        if (initLimitHelper.getCxMachineCodeSet().contains(cxMachineCode)) {
            return;
        }
        if (!singleCxMachineAllocation.hasRange(productionDay)) {
            return;
        }
        initLimitHelper.getCxMachineCodeSet().add(cxMachineCode);
        //最大硫化配比
        Integer maxLhMachineCount = initLimitHelper.getMaxLhMachineCount();
        maxLhMachineCount = maxLhMachineCount + singleCxMachineAllocation.getMaxLhMachineCount();
        initLimitHelper.maxLhMachineCount = maxLhMachineCount;
        //最低硫化配比
        initLimitHelper.getMinLhMachineInfo().put(cxMachineCode, singleCxMachineAllocation.getMinLhMachineCount());
        //最大胎胚种类数
        Integer maxEmbryoCodeCount = initLimitHelper.getMaxEmbryoCodeCount();
        maxEmbryoCodeCount = maxLhMachineCount + singleCxMachineAllocation.getMaxEmbryoCodeCount();
        initLimitHelper.maxEmbryoCodeCount = maxEmbryoCodeCount;
    }
}
