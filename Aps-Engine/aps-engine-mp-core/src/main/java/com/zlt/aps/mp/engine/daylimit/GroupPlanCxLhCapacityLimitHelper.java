package com.zlt.aps.mp.engine.daylimit;

import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.mp.engine.domain.dto.SkuDayProductionInfoHelper;
import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanStructureLhRatioVo;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import lombok.Data;
import lombok.Getter;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

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
     * 日控的 本结构剩余的最大硫化机台数
     * sandy+ 2026.3.22
     */
    private Integer remainMaxLhMachineCount;

    /**
     * 理论最大硫化机台(因切换结构首日减机台，引入)
     */
    private Integer maxTheoryLhMachineCount;

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
     * 各Sku实际排产的模具信息
     */
    private Map<String, Set<String>> skuProductionMouldMap;
    /**
     * 排产的Sku排产量信息
     */
    private Map<String, SkuDayProductionInfoHelper> productionSkuQtyInfo;
    /**
     * 各Sku排产的明细信息
     */
    private Map<String, List<SkuDayProductionInfoHelper>> skuProductionDetailInfo;
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
     * 根据机台分配情况，构建日排产限制数据对象
     *
     * @param context        排产上下文
     * @param productionDay  排产日
     * @param allocationInfo 分配信息
     * @return
     */
    public static GroupPlanCxLhCapacityLimitHelper buildByCxMachineAllocation(Context context, CxMachineBaseInfoVo cxMachineInfo, Integer productionDay, CxMachineAllocationPlanHelper allocationInfo) {
        if (context.getStopDays().contains(productionDay)) {
            return null;
        }
        Integer minLimit = BigDecimal.ZERO.intValue();
        GroupPlanCxLhCapacityLimitHelper initLimitHelper = buildEmptyData(productionDay, minLimit, minLimit);
        if (null == allocationInfo) {
            return initLimitHelper;
        }
        updateBaseLimitInfo(initLimitHelper, cxMachineInfo, allocationInfo);
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
            updateBaseLimitInfo(context, initLimitHelper, cxMachineInfo, singleCxMachineAllocation);
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
     * 更新数据
     * 剩余的最大硫化机台数
     *
     * @param machines
     */
    public void updateRemainMaxLhMachines(Integer machines) {
        remainMaxLhMachineCount = machines;
    }

    /**
     * 获取最早收尾的硫化组信息
     *
     * @param context               排产上下文
     * @param previousLimit         前一日的排产限制情况
     * @param releaseLhMachineCount 需要释放的硫化组机台数
     * @return
     */
    public SkuDayProductionInfoHelper getEarliestConclusionSkuInfo(Context context, GroupPlanCxLhCapacityLimitHelper previousLimit, Integer releaseLhMachineCount) {
        Map<String, Integer> previousSkuUsedMachine = previousLimit.getSkuTheoryUsedMachine();
        Map<String, Integer> currentSkuUsedMachine = getSkuUsedMachineRejectLeftOver(context);
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
        if (CollectionUtils.isEmpty(reductionSkuList)) {
            return null;
        }
        reductionSkuList.sort(Comparator.naturalOrder());
        int selectedIndex = releaseLhMachineCount - BigDecimal.ONE.intValue();
        if (selectedIndex >= reductionSkuList.size()) {
            return null;
        }
        String selected = reductionSkuList.get(selectedIndex);
        return getProductionSkuQtyInfo().get(selected);
    }

    /**
     * 判断使用模具数是否低于要求的模具数
     * 最低硫化配比使用
     *
     * @param minMouldNumber 最低硫化配比的模具数
     * @return
     */
    public boolean isLowMinMouldNumber(int minMouldNumber) {
        if (CollectionUtils.isEmpty(productionMouldSet)) {
            return true;
        }
        return productionMouldSet.size() < minMouldNumber;
    }

    /**
     * 判断能否加一台硫化
     * 如果该日硫化机台已经达到限制，则不用判断生胎
     * 否则需要判断生胎种类数是否达到限制
     *
     * @return
     */
    public boolean isAddOneLhMachine(String embryoCode) {
        Integer currentLhMachineCount = getProductionLhMachineCountByQty();
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
        if (currentEmbryoCodeCount >= maxEmbryoCodeCount) {
            return true;
        }
        //按量
        Integer currentLhMachineCount = getProductionLhMachineCountByQty();
        if (currentLhMachineCount >= maxLhMachineCount) {
            return true;
        }
        return false;
    }

    /**
     * 判断是否达到限制条件
     * 需要根据前一日的情况来判断
     * 胎胚种类数没有超
     * 硫化配比数没有超
     * 先挑选还有硫化组的，胎胚种类数也没超的
     *
     * @param context              排产上下文
     * @param previousDayLimitInfo 前一日的限制情况
     * @param nextDayLimitInfo     后一日的限制情况
     * @return
     */
    public boolean isReachLimitByMouldNumber(Context context, GroupPlanCxLhCapacityLimitHelper previousDayLimitInfo, GroupPlanCxLhCapacityLimitHelper nextDayLimitInfo) {
        //实际的最大硫化机台数 = min(初始的最大硫化机台数,结构剩余可用的最大硫化机台数 sandy+ 2026.03.22
        Integer realMaxLhMachineCount = maxLhMachineCount > remainMaxLhMachineCount ? remainMaxLhMachineCount : maxLhMachineCount;
        //按模具数
        Integer currentLhMachineCount = getProductionLhMachineCountByMouldNumber();
        //如果前日没有排产信息，则表示结构排产首日
        if (null == previousDayLimitInfo) {
            return currentLhMachineCount >= realMaxLhMachineCount;
        }
        //当日没有硫化组信息
        Map<String, Integer> currentDaySkuLhMachineInfoMap = getSkuUsedLhMachineCountByMouldNumber();
        if (CollectionUtils.isEmpty(currentDaySkuLhMachineInfoMap)) {
            return false;
        }
        Integer theoryUsedLhMachineCount = currentDaySkuLhMachineInfoMap.values().stream().mapToInt(Integer::intValue).sum();
        Integer realUsedLhMachineCount;
        if (null == nextDayLimitInfo) {
            //结构收尾日
            Integer previousDayChangeQty = getChangeUsedLhMachineQtyByPreviousDay(context, previousDayLimitInfo);
            realUsedLhMachineCount = theoryUsedLhMachineCount + previousDayChangeQty;
        } else {
            //中间排产
            Integer nextDayChangeQty = getChangeUsedLhMachineQtyByNextDayMouldNumber(context, nextDayLimitInfo);
            realUsedLhMachineCount = theoryUsedLhMachineCount + nextDayChangeQty;
        }
        return realUsedLhMachineCount >= realMaxLhMachineCount;
    }

    /**
     * 根据后一天的排产信息，获取可释放的机台信息
     *
     * @param context     排产上下文
     * @param nextDayInfo 后一天排产信息
     * @return
     */
    public Integer getReleaseLhMachineCount(Context context, GroupPlanCxLhCapacityLimitHelper nextDayInfo) {
        Map<String, SkuUsedLhMachineInfo> previousDaySkuLhMachineDetailMap = getSkuUsedDetailInfoByQty(context);
        Map<String, SkuUsedLhMachineInfo> nextDaySkuLhMachineDetailMap = nextDayInfo.getSkuUsedDetailInfoByQty(context);
        Map<String, Integer> changeMap = new HashMap<>();
        //前日变化
        previousDaySkuLhMachineDetailMap.forEach((materialDesc, previousUsedMachineDetail) -> {
            SkuUsedLhMachineInfo currentDetail = nextDaySkuLhMachineDetailMap.get(materialDesc);
            changeMap.put(materialDesc, previousUsedMachineDetail.getChangeMachineCount(currentDetail));
        });
        //后日新增
        nextDaySkuLhMachineDetailMap.forEach((materialDesc, nextUsedMachineDetail) -> {
            SkuUsedLhMachineInfo previousDetail = previousDaySkuLhMachineDetailMap.get(materialDesc);
            if (null != previousDetail) {
                return;
            }
            changeMap.put(materialDesc, nextUsedMachineDetail.getLeftOverMachineCount());
        });
        Integer initChangeCount = getInitChangeLhMachineCount();
        if (CollectionUtils.isEmpty(changeMap)) {
            return Math.abs(initChangeCount);
        }
        Integer reduction = changeMap.values().stream().mapToInt(Integer::intValue).sum();
        reduction = reduction + initChangeCount;
        return Math.abs(reduction);
    }

    /**
     * 判断增加胎胚是否达到胎胚种类数限制
     * true 表示达到限制，不可增加
     * false 表示没有达到限制，可增加
     *
     * @param addEmbryoCode 要加入的胎胚种类数
     * @return
     */
    public boolean isReachLimitByEmbryoCode(String addEmbryoCode) {
        if (productionEmbryoCodeSet.contains(addEmbryoCode)) {
            return false;
        }
        Integer currentEmbryoCodeCount = productionEmbryoCodeSet.size();
        return currentEmbryoCodeCount + BigDecimal.ONE.intValue() > maxEmbryoCodeCount;
    }

    /**
     * 获取当前使用的硫化机台数
     *
     * @return
     */
    public Integer getUsedLhMachineCount() {
        return getProductionLhMachineCountByQty();
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
        this.skuProductionMouldMap = new HashMap<>();
        this.productionSkuQtyInfo = new HashMap<>();
        this.skuProductionDetailInfo = new HashMap<>();
    }

    /**
     * 获取Sku占用的硫化机台数，忽略余量
     *
     * @return
     */
    private Map<String, Integer> getSkuUsedMachineRejectLeftOver(Context context) {
        if (CollectionUtils.isEmpty(skuProductionDetailInfo)) {
            return Collections.emptyMap();
        }
        Map<String, Integer> skuUsedLhMachine = new HashMap<>();
        skuProductionDetailInfo.forEach((materialDesc, skuProductionInfoList) -> {
            if (CollectionUtils.isEmpty(skuProductionInfoList)) {
                return;
            }
            Integer wholeNumber = BigDecimal.ZERO.intValue();
            for (SkuDayProductionInfoHelper skuProductionInfo : skuProductionInfoList) {
                Integer productionQty = skuProductionInfo.getSumProductionQty();
                //表示换模或是换活字块 开产日
                Integer lhMachineQty = context.getOpenDayMaxQty(day, skuProductionInfo.getDayLhMachineQty());
                if (productionQty < lhMachineQty) {
                    continue;
                }
                wholeNumber = wholeNumber + BigDecimal.ONE.intValue();
            }
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
            Set<String> usedMouldSet = skuProductionInfo.getUsedMouldSet();
            Integer usedMachineCount;
            if (CollectionUtils.isEmpty(usedMouldSet)) {
                usedMachineCount = BigDecimal.ZERO.intValue();
            } else {
                usedMachineCount = usedMouldSet.size() / ProductionConstant.DOUBLE_MOULD_PRODUCTION;
            }
//            Integer productionQty = skuProductionInfo.getSumProductionQty();
//            Integer lhMachineQty = skuProductionInfo.getDayLhMachineQty();
//            int upMachineCount = BigDecimal.valueOf(productionQty).divide(BigDecimal.valueOf(lhMachineQty), 0, RoundingMode.UP).intValue();
            skuUsedLhMachine.put(materialDesc, usedMachineCount);
        });
        return skuUsedLhMachine;
    }


    /**
     * 获取使用的硫化机台数
     * 采用排产量来估算
     *
     * @return
     */
    private Integer getProductionLhMachineCountByQty() {
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
     * 获取使用的硫化机台数
     * 使用模具数来测算
     *
     * @return
     */
    private Integer getProductionLhMachineCountByMouldNumber() {
        if (CollectionUtils.isEmpty(productionSkuQtyInfo) || CollectionUtils.isEmpty(skuProductionMouldMap)) {
            return BigDecimal.ZERO.intValue();
        }
        Map<String, Integer> skuLhMachineCountMap = getSkuUsedLhMachineCountByMouldNumber();
        if (CollectionUtils.isEmpty(skuLhMachineCountMap)) {
            return BigDecimal.ZERO.intValue();
        }
        return skuLhMachineCountMap.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * 根据各Sku排产的模具数，得到各Sku使用的硫化组数
     *
     * @return
     */
    private Map<String, Integer> getSkuUsedLhMachineCountByMouldNumber() {
        if (CollectionUtils.isEmpty(productionSkuQtyInfo) || CollectionUtils.isEmpty(skuProductionMouldMap)) {
            return Collections.emptyMap();
        }
        Map<String, Integer> skuLhMachineCountMap = new HashMap<>();
        skuProductionMouldMap.forEach((materialDesc, usedMouldSet) -> {
            Integer mouldNumber = usedMouldSet.size();
            Integer lhMachineCount = BigDecimal.valueOf(mouldNumber).divide(BigDecimal.valueOf(ProductionConstant.DOUBLE_MOULD_PRODUCTION), 0, RoundingMode.UP).intValue();
            if (lhMachineCount <= BigDecimal.ZERO.intValue()) {
                return;
            }
            skuLhMachineCountMap.put(materialDesc, lhMachineCount);
        });
        if (CollectionUtils.isEmpty(skuLhMachineCountMap)) {
            return Collections.emptyMap();
        }
        return skuLhMachineCountMap;
    }

    /**
     * 根据当日各Sku使用的硫化组数，结合前一日排产情况，计算实际变化的硫化组数
     * 即新增硫化组 + 减的硫化组数
     *
     * @param context              排产上下文
     * @param previousDayLimitInfo 前一日排产情况信息
     * @return
     */
    private Integer getChangeUsedLhMachineQtyByPreviousDay(Context context, GroupPlanCxLhCapacityLimitHelper previousDayLimitInfo) {
        //根据前日排产，使用各Sku排产量计算各Sku使用的硫化组数
        Map<String, SkuUsedLhMachineInfo> previousDaySkuLhMachineDetailMap = previousDayLimitInfo.getSkuUsedDetailInfoByQty(context);
        //当前日Sku使用的硫化组数-根据模具数
        Map<String, Integer> currentDaySkuLhMachineInfoMap = getSkuUsedLhMachineCountByMouldNumber();
        //当前日排产，使用各Sku排产量计算各Sku使用的硫化组数
        Map<String, SkuUsedLhMachineInfo> currentDaySkuLhMachineDetailMap = getSkuUsedDetailInfoByQty(context);
        //新增Sku-使用的硫化组数
        Map<String, Integer> addSkuMap = new HashMap<>();
        currentDaySkuLhMachineInfoMap.forEach((materialDesc, addQty) -> {
            //前一日没有，则表示新增Sku
            if (!previousDaySkuLhMachineDetailMap.containsKey(materialDesc)) {
                addSkuMap.put(materialDesc, addQty);
                return;
            }
        });
        //Sku减量的硫化组数
        Map<String, Integer> changeMap = new HashMap<>();
        previousDaySkuLhMachineDetailMap.forEach((materialDesc, previousUsedMachineDetail) -> {
            SkuUsedLhMachineInfo currentDetail = currentDaySkuLhMachineDetailMap.get(materialDesc);
            changeMap.put(materialDesc, previousUsedMachineDetail.getChangeMachineCount(currentDetail));
        });
        return calculateChangeLhMachineQty(addSkuMap, changeMap);
    }

    /**
     * 根据当日各Sku使用的硫化组数，结合前一日排产情况，计算实际变化的硫化组数
     * 即新增硫化组 + 减的硫化组数
     *
     * @param context          排产上下文
     * @param nextDayLimitInfo 后一日排产情况信息
     * @return
     */
    private Integer getChangeUsedLhMachineQtyByNextDay(Context context, GroupPlanCxLhCapacityLimitHelper nextDayLimitInfo) {
        //根据后一日排产，使用各Sku排产量计算各Sku使用的硫化组数
        Map<String, SkuUsedLhMachineInfo> nextDaySkuLhMachineDetailMap = nextDayLimitInfo.getSkuUsedDetailInfoByQty(context);
        //后一日Sku使用的硫化组数-根据模具数
        Map<String, Integer> nextDaySkuLhMachineInfoMap = nextDayLimitInfo.getSkuUsedLhMachineCountByMouldNumber();
        //当前日排产，使用各Sku排产量计算各Sku使用的硫化组数
        Map<String, SkuUsedLhMachineInfo> currentDaySkuLhMachineDetailMap = getSkuUsedDetailInfoByQty(context);
        //新增Sku-使用的硫化组数
        Map<String, Integer> addSkuMap = new HashMap<>();
        nextDaySkuLhMachineInfoMap.forEach((materialDesc, addQty) -> {
            //当前日没有，则表示新增Sku
            if (!currentDaySkuLhMachineDetailMap.containsKey(materialDesc)) {
                addSkuMap.put(materialDesc, addQty);
            }
        });
        //Sku减量的硫化组数
        Map<String, Integer> changeMap = new HashMap<>();
        currentDaySkuLhMachineDetailMap.forEach((materialDesc, currentDetail) -> {
            SkuUsedLhMachineInfo nextDayDetail = nextDaySkuLhMachineDetailMap.get(materialDesc);
            //有余量，则需要看是否减量
            if (currentDetail.getLeftOverMachineCount() > BigDecimal.ZERO.intValue()) {
                changeMap.put(materialDesc, currentDetail.getChangeMachineCount(nextDayDetail));
            }
        });
        return calculateChangeLhMachineQty(addSkuMap, changeMap);
    }

    /**
     * 根据后一日的排产信息，获取硫化变化组数
     *
     * @param context          排产上下文
     * @param nextDayLimitInfo 下一日排产信息
     * @return
     */
    private Integer getChangeUsedLhMachineQtyByNextDayMouldNumber(Context context, GroupPlanCxLhCapacityLimitHelper nextDayLimitInfo) {
        //后一日Sku使用的硫化组数-根据模具数
        Map<String, Integer> nextDaySkuLhMachineInfoMap = nextDayLimitInfo.getSkuUsedLhMachineCountByMouldNumber();
        Map<String, Integer> currentDaySkuLhMachineInfoMap = getSkuUsedLhMachineCountByMouldNumber();
        Map<String, Integer> currentDayFullSkuLhMachineInfoMap = getCurrentDayFullLhMachineInfoMap(context);
        //相比前一日，新增Sku增加的机台数
        Map<String, Integer> addSkuMap = new HashMap<>();
        Map<String, Integer> reductionSkuMap = new HashMap<>();
        currentDaySkuLhMachineInfoMap.forEach((materialDesc, lhMachineQty) -> {
            Integer nextDayLhMachineQty = nextDaySkuLhMachineInfoMap.get(materialDesc);
            Integer fullLhMachineQty = currentDayFullSkuLhMachineInfoMap.get(materialDesc);
            if (null == nextDayLhMachineQty) {
                nextDayLhMachineQty = BigDecimal.ZERO.intValue();
            }
            if (null == lhMachineQty) {
                lhMachineQty = BigDecimal.ZERO.intValue();
            }
            //20260323 满台释放，则不减
            if (lhMachineQty > nextDayLhMachineQty && lhMachineQty.equals(fullLhMachineQty)) {
                return;
            }
            if (lhMachineQty > nextDayLhMachineQty) {
                reductionSkuMap.put(materialDesc, nextDayLhMachineQty - lhMachineQty);
            }
            if (lhMachineQty < nextDayLhMachineQty) {
                addSkuMap.put(materialDesc, nextDayLhMachineQty - lhMachineQty);
            }
        });
        return calculateChangeLhMachineQty(addSkuMap, reductionSkuMap);
    }

    /**
     * 获取当前日Sku占满产的硫化机台数
     *
     * @return
     */
    private Map<String, Integer> getCurrentDayFullLhMachineInfoMap(Context context) {
        if (CollectionUtils.isEmpty(skuProductionDetailInfo)) {
            return Collections.emptyMap();
        }
        Map<String, Integer> fullSkuMap = new HashMap<>(16);
        skuProductionDetailInfo.forEach((materialDesc, detailList) -> {
            if (CollectionUtils.isEmpty(detailList)) {
                return;
            }
            List<SkuDayProductionInfoHelper> fullList = detailList.stream().filter(single -> single.isFullProduction(context)).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(fullList)) {
                return;
            }
            fullSkuMap.put(materialDesc, fullList.size());
        });
        return fullSkuMap;
    }

    /**
     * 计算变化的硫化组信息
     *
     * @param addSkuMap       Sku新增硫化组数
     * @param reductionSkuMap Sku减的硫化组数
     * @return
     */
    private Integer calculateChangeLhMachineQty(Map<String, Integer> addSkuMap, Map<String, Integer> reductionSkuMap) {
        Integer realChangeLhMachineCount = BigDecimal.ZERO.intValue();
        if (!CollectionUtils.isEmpty(reductionSkuMap)) {
            Integer reduction = reductionSkuMap.values().stream().mapToInt(Integer::intValue).sum();
            realChangeLhMachineCount = realChangeLhMachineCount + reduction;
        }
        if (!CollectionUtils.isEmpty(addSkuMap)) {
            Integer add = addSkuMap.values().stream().mapToInt(Integer::intValue).sum();
            realChangeLhMachineCount = realChangeLhMachineCount + add;
        }
        return realChangeLhMachineCount;
    }

    /**
     * 获取各Sku使用的硫化机台明细信息，根据排产量
     *
     * @param context 排产上下文
     * @return
     */
    private Map<String, SkuUsedLhMachineInfo> getSkuUsedDetailInfoByQty(Context context) {
        if (CollectionUtils.isEmpty(skuProductionDetailInfo)) {
            return Collections.emptyMap();
        }
        Map<String, SkuUsedLhMachineInfo> skuUsedLhMachineDetailMap = new HashMap<>();
        skuProductionDetailInfo.forEach((materialDesc, skuProductionDetailList) -> {
            if (CollectionUtils.isEmpty(skuProductionDetailList)) {
                return;
            }
            Integer wholeNumber = BigDecimal.ZERO.intValue();
            Integer remainder = BigDecimal.ZERO.intValue();
            for (SkuDayProductionInfoHelper lhDetail : skuProductionDetailList) {
                //需要考虑开产日
                Integer dayLhMachineQty = context.getOpenDayMaxQty(day, lhDetail.getDayLhMachineQty());
                if (lhDetail.getSumProductionQty().equals(dayLhMachineQty)) {
                    wholeNumber = wholeNumber + BigDecimal.ONE.intValue();
                } else {
                    remainder = remainder + BigDecimal.ONE.intValue();
                }
            }
            SkuUsedLhMachineInfo usedDetail = SkuUsedLhMachineInfo.buildLhCount(materialDesc, wholeNumber, remainder);
            skuUsedLhMachineDetailMap.put(materialDesc, usedDetail);
        });
//        if (CollectionUtils.isEmpty(productionSkuQtyInfo)) {
//            return Collections.emptyMap();
//        }
//        Map<String, SkuUsedLhMachineInfo> skuUsedLhMachineDetailMap = new HashMap<>();
//        productionSkuQtyInfo.forEach((materialDesc, skuProductionInfo) -> {
//            Integer productionQty = skuProductionInfo.getSumProductionQty();
//            Integer lhMachineQty = skuProductionInfo.getDayLhMachineQty();
//            int remainder = productionQty % lhMachineQty;
//            int wholeNumber = productionQty / lhMachineQty;
//            SkuUsedLhMachineInfo usedDetail = SkuUsedLhMachineInfo.build(materialDesc, wholeNumber, remainder);
//            skuUsedLhMachineDetailMap.put(materialDesc, usedDetail);
//        });
//        if (CollectionUtils.isEmpty(skuUsedLhMachineDetailMap)) {
//            return Collections.emptyMap();
//        }
        return skuUsedLhMachineDetailMap;
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
        maxEmbryoCodeCount = maxEmbryoCodeCount + lhRatio.getMaxEmbryoQty();
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
    private static void updateBaseLimitInfo(Context context, GroupPlanCxLhCapacityLimitHelper initLimitHelper, CxMachineBaseInfoVo cxMachineInfo, MpStructureAllocation singleCxMachineAllocation) {
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
        Integer singleCxMachineCount = singleCxMachineAllocation.getMaxLhMachineCount();
        TbrProductionContext productionContext = (TbrProductionContext) context;
        String continueStruct = productionContext.getContinueStructureMap() == null ? "" : productionContext.getContinueStructureMap().get(singleCxMachineAllocation.getCxMachineCode());
        if (singleCxMachineAllocation.getBeginDay().equals(productionDay)) {
            if (!(singleCxMachineAllocation.getBeginDay().equals(FactoryConstant.MONTH_START_DAY) &&
                    singleCxMachineAllocation.getStructureName().equals(continueStruct))) {
                //若非（1号且续作结构）
                //若是结构开产首日，将最大成型机数-3 sandy+ 2026.3.19
                Integer decLhMachines = productionContext.getBaseDataContainer().getParamConfiguration().getDeductionLhMachineCount();
                decLhMachines = (decLhMachines > singleCxMachineCount) ? singleCxMachineCount : decLhMachines;
                singleCxMachineCount -= decLhMachines;
            }
        }
        maxLhMachineCount = maxLhMachineCount + singleCxMachineCount;
        initLimitHelper.maxLhMachineCount = maxLhMachineCount;
        //最低硫化配比
        initLimitHelper.getMinLhMachineInfo().put(cxMachineCode, singleCxMachineAllocation.getMinLhMachineCount());
        //最大胎胚种类数
        Integer maxEmbryoCodeCount = initLimitHelper.getMaxEmbryoCodeCount();
        maxEmbryoCodeCount = maxEmbryoCodeCount + singleCxMachineAllocation.getMaxEmbryoCodeCount();
        initLimitHelper.maxEmbryoCodeCount = maxEmbryoCodeCount;
        //理论最大硫化配比
        Integer maxTheoryLhMachineCount = Optional.ofNullable(initLimitHelper.getMaxTheoryLhMachineCount()).orElse(BigDecimal.ZERO.intValue());
        maxTheoryLhMachineCount = maxTheoryLhMachineCount + Optional.ofNullable(singleCxMachineAllocation.getMaxLhMachineCount()).orElse(BigDecimal.ZERO.intValue());
        initLimitHelper.maxTheoryLhMachineCount = maxTheoryLhMachineCount;
    }

    /**
     * 获取初始的硫化机台变化数
     * 因切换结构日需要减机台，引入
     *
     * @return
     */
    private Integer getInitChangeLhMachineCount() {
        Integer realMaxLhMachineCount = Optional.ofNullable(maxLhMachineCount).orElse(BigDecimal.ZERO.intValue());
        Integer theoryMaxLhMachineCount = Optional.ofNullable(maxTheoryLhMachineCount).orElse(realMaxLhMachineCount);
        //Integer realRemainMaxLhMachineCount = Optional.ofNullable(remainMaxLhMachineCount).orElse(BigDecimal.ZERO.intValue());
        //theoryMaxLhMachineCount = theoryMaxLhMachineCount > realRemainMaxLhMachineCount ? realRemainMaxLhMachineCount : theoryMaxLhMachineCount;
        return realMaxLhMachineCount - theoryMaxLhMachineCount;
    }
}

/**
 * Sku使用硫化组信息对象
 *
 * @author ZLT
 * @date 2026-01-12
 */
@Data
class SkuUsedLhMachineInfo {
    /**
     * Sku信息
     */
    private String materialDesc;
    /**
     * 整台硫化组数
     */
    private Integer wholeMachineCount;
    /**
     * 非整台硫化组数
     */
    private Integer leftOverMachineCount;

    /**
     * 构建Sku使用硫化机台数明细对象
     *
     * @param materialDesc          Sku
     * @param wholeMachineCount     整数台
     * @param leftOverProductionQty 收尾的余量
     * @return
     */
    public static SkuUsedLhMachineInfo build(String materialDesc, Integer wholeMachineCount, Integer leftOverProductionQty) {
        SkuUsedLhMachineInfo detail = new SkuUsedLhMachineInfo();
        detail.setMaterialDesc(materialDesc);
        if (null == wholeMachineCount) {
            detail.setWholeMachineCount(BigDecimal.ZERO.intValue());
        } else {
            detail.setWholeMachineCount(wholeMachineCount);
        }
        if (null == leftOverProductionQty || leftOverProductionQty <= BigDecimal.ZERO.intValue()) {
            detail.setLeftOverMachineCount(BigDecimal.ZERO.intValue());
        } else {
            detail.setLeftOverMachineCount(BigDecimal.ONE.intValue());
        }
        return detail;
    }

    /**
     * 构建Sku使用硫化机台数明细对象
     *
     * @param materialDesc      Sku
     * @param wholeMachineCount 整数台
     * @param leftOverCount     余量台
     * @return
     */
    public static SkuUsedLhMachineInfo buildLhCount(String materialDesc, Integer wholeMachineCount, Integer leftOverCount) {
        SkuUsedLhMachineInfo detail = new SkuUsedLhMachineInfo();
        detail.setMaterialDesc(materialDesc);
        if (null == wholeMachineCount) {
            detail.setWholeMachineCount(BigDecimal.ZERO.intValue());
        } else {
            detail.setWholeMachineCount(wholeMachineCount);
        }
        if (null == leftOverCount) {
            detail.setLeftOverMachineCount(BigDecimal.ZERO.intValue());
        } else {
            detail.setLeftOverMachineCount(leftOverCount);
        }
        return detail;
    }

    /**
     * 获取整数机台变化数
     * 如果nextDayInfo 没有，则表示整数台+余量台都减
     * 否则看整数台的值变化量
     * 得到后一天的机台数变化
     *
     * @param nextDayInfo 后一天的排产明细
     * @return
     */
    public Integer getChangeMachineCount(SkuUsedLhMachineInfo nextDayInfo) {
        Integer allMachineCount = getWholeMachineCount() + getLeftOverMachineCount();
        if (null == nextDayInfo) {
            return BigDecimal.ZERO.intValue() - allMachineCount;
        }
        Integer nextAllMachineCount = nextDayInfo.getWholeMachineCount() + nextDayInfo.getLeftOverMachineCount();
        if (allMachineCount.equals(nextAllMachineCount)) {
            return getSingleChange(nextDayInfo);
        }
        return nextDayInfo.getWholeMachineCount() - getWholeMachineCount();
    }

    /**
     * 单台变化处理
     *
     * @param nextDayInfo
     * @return
     */
    private Integer getSingleChange(SkuUsedLhMachineInfo nextDayInfo) {
        //如果是单台
        if (getWholeMachineCount() == BigDecimal.ZERO.intValue() && nextDayInfo.getWholeMachineCount() == BigDecimal.ZERO.intValue()) {
            return getWholeMachineCount() - nextDayInfo.getLeftOverMachineCount();
        }
        if (getWholeMachineCount() == BigDecimal.ZERO.intValue() && nextDayInfo.getWholeMachineCount() == BigDecimal.ONE.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        if (getWholeMachineCount() == BigDecimal.ONE.intValue() && nextDayInfo.getWholeMachineCount() == BigDecimal.ZERO.intValue()) {
            return BigDecimal.ZERO.intValue() - nextDayInfo.getLeftOverMachineCount();
        }
        if (getWholeMachineCount() == BigDecimal.ONE.intValue() && nextDayInfo.getWholeMachineCount() == BigDecimal.ONE.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        //多台，看余量变，减
        return BigDecimal.ZERO.intValue() - nextDayInfo.getLeftOverMachineCount();
    }
}
