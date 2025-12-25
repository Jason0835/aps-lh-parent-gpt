package com.zlt.aps.factory.scheduling.cxcapacity;

import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.dto.*;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 在机结构，续作Sku续作模具数处理控制
 * 如果在机结构成型机台数 > 估算需要使用的成型机台数，则优先释放配比高的机台，其他成型机编号大的
 *
 * @author ZLT
 * @date 20251223
 */
@Slf4j
public class CxContinueSkuAllocationMouldHandler {
    /**
     * 根据结构在机机台及实际使用机台数，
     * 对续作Sku进行续作模具数调整
     * 并分配到成型机台
     * 1、先分配机台数少的，按成型机轮询分
     * 2、在分配机台数多的
     *
     * @param groupPlanInfo          结构计划信息
     * @param groupContinueInfo      在机结构续作信息
     * @param realWholeMachineNumber 需要的机台数
     */
    public static void allocationContinueSkuMouldNumber(ProductionPlanGroupInfo groupPlanInfo, CxContinueInfoHelper groupContinueInfo, Integer realWholeMachineNumber) {
        if (null == groupContinueInfo || CollectionUtils.isEmpty(groupContinueInfo.getCxMachineCodeSet())) {
            return;
        }
        //设置续作Sku的计划排产量：取高优先级还是总净需求量
        setContinueSkuPlanDemandQty(groupPlanInfo, groupContinueInfo);
        Set<String> cxMachineCodeSet = groupContinueInfo.getCxMachineCodeSet();
        if (isBuilderFullLhMachine(cxMachineCodeSet, realWholeMachineNumber)) {
            //todo 构建续作？
            allocationFullMachine(groupContinueInfo);
            return;
        }
        //构建配比大，机台编号大的需要释放的成型机台数，按sku续作模具数多的优先减，其次排产需求量少的优先减模具数，直到减到满足硫化配比机台数为止
        Integer releaseCount = cxMachineCodeSet.size() - realWholeMachineNumber;
        releaseContinueSkuMouldNumber(groupContinueInfo, releaseCount);
    }

    /**
     * 在机结构-成型机台不需要减
     *
     * @param groupContinueInfo
     */
    private static void allocationFullMachine(CxContinueInfoHelper groupContinueInfo) {
        Map<String, CxContinueSkuInfoHelper> continueSkuMouldNumberMap = groupContinueInfo.getContinueSkuMouldNumberMap();
        if (CollectionUtils.isEmpty(continueSkuMouldNumberMap)) {
            return;
        }
        Set<String> cxMachineCodeSet = groupContinueInfo.getCxMachineCodeSet();
        List<CxContinueSkuInfoHelper> continueSkuList = new ArrayList<>(continueSkuMouldNumberMap.values());
        //计算胎胚种类数
        Map<String, List<CxContinueSkuInfoHelper>> embryoSkuGroupMap = continueSkuList.stream().collect(Collectors.groupingBy(CxContinueSkuInfoHelper::getEmbryoCode));
        Map<String, Integer> embryoMouldNumberMap = new HashMap<>(embryoSkuGroupMap.size());
        embryoSkuGroupMap.forEach((embryoCode, skuList) -> embryoMouldNumberMap.put(embryoCode, skuList.stream().mapToInt(CxContinueSkuInfoHelper::getMouldNumber).sum()));

        Set<String> embryoSet = continueSkuList.stream().map(CxContinueSkuInfoHelper::getEmbryoCode).collect(Collectors.toSet());

    }

    /**
     * 进行月初释放机台：优先释放配比大的机台
     * 1、在续作机台中，优先释放成型硫化配比大的机台
     * 2、根据剩余的机台，获取胎胚种类个数、硫化配比的模具总数
     * 2.1、如果续作Sku的胎胚个数没有超，续作Sku使用的模具总数也没有超，则不用调整
     * 2.2、如果续作Sku的胎胚个数没有超，续作Sku使用的模具总数超
     * 2.2.1、优先将没有计划量的Sku，移除，标记为非续作Sku，直到达到总模具数限制标准
     * 2.2.2、还超出模具数，则继续对有计划量的Sku，移除
     * 按模具数多，计划量多的排序，两副两副减，每减两副重新挑选排序，直到达到总模具数限制标准
     * 3、如果续作Sku的胎胚个数超，续作Sku使用的模具数没有超
     * 3.1、优先将没有计划量的胎胚，移除，该胎胚下所有续作Sku都标记为非续作Sku，直到达到胎胚个数限制标准
     * 3.2、还超出胎胚个数，则续作对有计划量的胎胚，移除
     * 3.2.1、优先选择胎胚的计划量使用模具数排产超过5天(可配置)
     * 按使用模具数少，计划量多的顺序优先，逐个胎胚释放，直到达到胎胚个数限制标准
     * 4、如果续作Sku的胎胚个数超，续作Sku使用的模具数也超，
     * 4.1、优先将没有计划量的胎胚，移除，该胎胚下所有续作Sku都标记为非续作Sku，直达达到胎胚个数限制标准
     * 4.2、还超出胎胚个数，继续对有计划量的胎胚，移除。判断是否超出模具总数限制
     * 4.2.1、如果模具总数没有超，则进行第3步
     * 4.2.2、如果模具总数也超，则按胎胚模具数多，计划量多的胎胚，移除一个胎胚。
     * 对应胎胚的所有续作Sku，移除，标记为非续作SKU
     * 4.2.3、继续判断剩余的情形，重复2~4的判断
     *
     *
     * @param groupContinueInfo 续作信息
     * @param releaseCount      需要释放的机台数
     */
    private static void releaseContinueSkuMouldNumber(CxContinueInfoHelper groupContinueInfo, Integer releaseCount) {
        if (null == releaseCount || releaseCount <= BigDecimal.ZERO.intValue()) {
            //todo 记录日志
            return;
        }
        List<ProductGroupCxCapacityInfo> cxCapacityInfoList = groupContinueInfo.getCxCapacityInfoList();
        if (CollectionUtils.isEmpty(cxCapacityInfoList)) {
            //todo 记录日志
            return;
        }
        if (releaseCount >= cxCapacityInfoList.size()) {
            //todo 记录日志
            return;
        }
        //按对应的硫化机台数多优先，成型机编号大的优先排序
        cxCapacityInfoList.sort(Comparator.comparing(ProductGroupCxCapacityInfo::getMaxLhMachineCount).thenComparing(ProductGroupCxCapacityInfo::getCxMachineCode));
        //保留下来的成型：对应可配置的胎胚种类数、成型硫化配比数
        List<ProductGroupCxCapacityInfo> reserveCxMachineList = cxCapacityInfoList.subList(releaseCount, cxCapacityInfoList.size() - BigDecimal.ONE.intValue());
        //更新机台信息，选中的成型机台
        groupContinueInfo.setCxMachineCodeSet(reserveCxMachineList.stream().map(ProductGroupCxCapacityInfo::getCxMachineCode).collect(Collectors.toSet()));
        groupContinueInfo.setCxCapacityInfoList(reserveCxMachineList);
        /**
         * 计算需要释放的sku及释放的模具数
         * 需要考虑胎胚种类数
         * 最大模具数则根据成型硫化配比叠加
         */
        Integer maxEmbryoCodeCount = reserveCxMachineList.stream().mapToInt(ProductGroupCxCapacityInfo::getMaxEmbryoCodeCount).sum();
        Integer maxCxLhCount = reserveCxMachineList.stream().mapToInt(ProductGroupCxCapacityInfo::getMaxLhMachineCount).sum();
        Integer maxMouldNumber = maxCxLhCount * ProductionConstant.DOUBLE_MOULD_PRODUCTION;
        //续作的胎胚种类数及模具数
        Map<String, CxContinueSkuInfoHelper> continueSkuMouldNumberMap = groupContinueInfo.getContinueSkuMouldNumberMap();
        List<CxContinueSkuInfoHelper> continueSkuInfoList = continueSkuMouldNumberMap.values().stream().collect(Collectors.toList());
        Set<String> continueEmbryoCodeSet = continueSkuInfoList.stream().map(CxContinueSkuInfoHelper::getEmbryoCode).collect(Collectors.toSet());
        Integer continueEmbryoCount = continueEmbryoCodeSet.size();
        Integer continueMouldNumber = continueSkuInfoList.stream().mapToInt(CxContinueSkuInfoHelper::getMouldNumber).sum();
        //直接释放，转移即可
        if (continueEmbryoCount <= maxEmbryoCodeCount && continueMouldNumber <= maxMouldNumber) {
            allocationFullMachine(groupContinueInfo);
            return;
        }
        //胎胚没有超，模具数(即硫化机台)多
        if (continueEmbryoCount <= maxEmbryoCodeCount && continueMouldNumber > maxMouldNumber) {
            Integer needReduceMouldNumber = continueMouldNumber - maxMouldNumber;
            reduceMouldNumber(groupContinueInfo, continueSkuMouldNumberMap, needReduceMouldNumber);
            return;
        }
        //需要移除的胎胚种类数
        Integer needReduceEmbryoCount = continueEmbryoCount - maxEmbryoCodeCount;
        //胎胚种类数超，则优先将胎胚没有计划的移除
        List<CxContinueEmbryoInfoHelper> continueEmbryoInfoList = getCxContinueEmbryoInfoList(continueSkuMouldNumberMap);
        //实际可移除的没有计划量的胎胚信息
        Set<String> reduceEmbryoSet = getReduceNoPlanQtyEmbryo(continueEmbryoInfoList, needReduceEmbryoCount);
        Map<String, CxContinueSkuInfoHelper> leftOverMap = getLeftOverContinueSku(continueSkuMouldNumberMap, reduceEmbryoSet);
        if (!CollectionUtils.isEmpty(reduceEmbryoSet)) {
            needReduceEmbryoCount = needReduceEmbryoCount - reduceEmbryoSet.size();
        }
        List<CxContinueSkuInfoHelper> leftOverContinueSkuInfoList = leftOverMap.values().stream().collect(Collectors.toList());
        Integer leftOverContinueMouldNumber = leftOverContinueSkuInfoList.stream().mapToInt(CxContinueSkuInfoHelper::getMouldNumber).sum();
        //剩余的续作Sku总模具数小于最大限制数
        if (needReduceEmbryoCount <= BigDecimal.ZERO.intValue() && leftOverContinueMouldNumber <= maxMouldNumber) {
            groupContinueInfo.setContinueSkuMouldNumberMap(leftOverMap);
            allocationFullMachine(groupContinueInfo);
            return;
        }
        //剩余的续作Sku总模具数超出最大限制数
        if (needReduceEmbryoCount <= BigDecimal.ZERO.intValue() && leftOverContinueMouldNumber > maxMouldNumber) {
            Integer needReduceMouldNumber = leftOverContinueMouldNumber - maxMouldNumber;
            reduceMouldNumber(groupContinueInfo, leftOverMap, needReduceMouldNumber);
            return;
        }
        List<CxContinueEmbryoInfoHelper> hasPlanList = continueEmbryoInfoList.stream().filter(continueEmbryoInfo -> continueEmbryoInfo.getPlanDemandQty() > BigDecimal.ZERO.longValue()).collect(Collectors.toList());
        //模具数没超
        if (leftOverContinueMouldNumber <= maxMouldNumber) {
            reduceEmbryoCount(groupContinueInfo, hasPlanList, leftOverMap, needReduceEmbryoCount);
            return;
        }
        //胎胚种类数超，模具数也超，则优先使用模具数多的，量大的
        hasPlanList.sort(Comparator.comparing(CxContinueEmbryoInfoHelper::getMouldNumber, Comparator.reverseOrder()).thenComparing(CxContinueEmbryoInfoHelper::getPlanDemandQty, Comparator.reverseOrder()));

        String embryoCode = hasPlanList.get(BigDecimal.ZERO.intValue()).getEmbryoCode();
        leftOverMap = getLeftOverContinueSku(leftOverMap, embryoCode);
        needReduceEmbryoCount = needReduceEmbryoCount - BigDecimal.ONE.intValue();
        leftOverContinueSkuInfoList = leftOverMap.values().stream().collect(Collectors.toList());
        leftOverContinueMouldNumber = leftOverContinueSkuInfoList.stream().mapToInt(CxContinueSkuInfoHelper::getMouldNumber).sum();
        if (needReduceEmbryoCount <= BigDecimal.ZERO.intValue() && leftOverContinueMouldNumber > maxMouldNumber) {
            Integer needReduceMouldNumber = leftOverContinueMouldNumber - maxMouldNumber;
            reduceMouldNumber(groupContinueInfo, leftOverMap, needReduceMouldNumber);
            return;
        }


    }

    /**
     * 对在结构减少reduceMouldNumber数量的模具数
     * 胎胚种类数不减少即Sku个数不减
     * 按模具数，且计划量多的优先减少，两副两副的减
     *
     * @param groupContinueInfo         在机结构-续作Sku信息
     * @param continueSkuMouldNumberMap 续作SkuMap信息
     * @param reduceMouldNumber         需要减少的模具数
     */
    private static void reduceMouldNumber(CxContinueInfoHelper groupContinueInfo, Map<String, CxContinueSkuInfoHelper> continueSkuMouldNumberMap, Integer reduceMouldNumber) {
        //没有计划量的优先减
        List<CxContinueSkuInfoHelper> continueSkuInfoList = continueSkuMouldNumberMap.values().stream().collect(Collectors.toList());
        List<CxContinueSkuInfoHelper> noPlanQtyList = continueSkuInfoList.stream().filter(continueSku -> continueSku.getPlanDemandQty() <= BigDecimal.ZERO.longValue()).collect(Collectors.toList());
        List<CxContinueSkuInfoHelper> hasPlanQtyList = continueSkuInfoList.stream().filter(continueSku -> continueSku.getPlanDemandQty() > BigDecimal.ZERO.longValue()).collect(Collectors.toList());
        Integer reduceNumber = BigDecimal.ZERO.intValue();
        if (!CollectionUtils.isEmpty(noPlanQtyList)) {
            reduceNumber = noPlanQtyList.stream().mapToInt(CxContinueSkuInfoHelper::getMouldNumber).sum();
        }
        if (reduceNumber >= reduceMouldNumber) {
            Map<String, CxContinueSkuInfoHelper> leftOverMap = hasPlanQtyList.stream().collect(Collectors.toMap(CxContinueSkuInfoHelper::getMaterialDesc, Function.identity()));
            groupContinueInfo.setContinueSkuMouldNumberMap(leftOverMap);
            allocationFullMachine(groupContinueInfo);
            return;
        }
        //模具数多，且计划数量多的先减少两副
        while (reduceNumber < reduceMouldNumber) {
            hasPlanQtyList.sort(Comparator.comparing(CxContinueSkuInfoHelper::getMouldNumber, Comparator.reverseOrder())
                    .thenComparing(CxContinueSkuInfoHelper::getPlanDemandQty, Comparator.reverseOrder()));
            //模具数最多，且计划数量多的优先减
            CxContinueSkuInfoHelper continueSkuInfo = hasPlanQtyList.get(BigDecimal.ZERO.intValue());
            Integer mouldNumber = continueSkuInfo.getMouldNumber();
            if (mouldNumber >= ProductionConstant.DOUBLE_MOULD_PRODUCTION) {
                mouldNumber = mouldNumber - ProductionConstant.DOUBLE_MOULD_PRODUCTION;
            }
            reduceNumber = reduceMouldNumber + ProductionConstant.DOUBLE_MOULD_PRODUCTION;
            continueSkuInfo.setMouldNumber(mouldNumber);
        }
        Map<String, CxContinueSkuInfoHelper> leftOverMap = hasPlanQtyList.stream().collect(Collectors.toMap(CxContinueSkuInfoHelper::getMaterialDesc, Function.identity()));
        groupContinueInfo.setContinueSkuMouldNumberMap(leftOverMap);
        allocationFullMachine(groupContinueInfo);
        return;
    }

    /**
     * 在胎胚种类数超的情形下，优先将没有计划的胎胚释放(从续作中移除)
     *
     * @param continueEmbryoInfoList 续作胎胚信息
     */
    private static Set<String> getReduceNoPlanQtyEmbryo(List<CxContinueEmbryoInfoHelper> continueEmbryoInfoList, Integer needReduceEmbryoCount) {
        //提取胎胚没有计划的
        List<CxContinueEmbryoInfoHelper> noPlanList = continueEmbryoInfoList.stream().filter(continueEmbryoInfo -> continueEmbryoInfo.getPlanDemandQty() <= BigDecimal.ZERO.longValue()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(noPlanList)) {
            return Collections.emptySet();
        }
        //优先移除胎胚计划为零的
        List<CxContinueEmbryoInfoHelper> reduceList;
        if (noPlanList.size() < needReduceEmbryoCount) {
            reduceList = noPlanList;
        } else {
            //模具数多的优先
            noPlanList.sort(Comparator.comparing(CxContinueEmbryoInfoHelper::getMouldNumber, Comparator.reverseOrder()));
            reduceList = noPlanList.subList(BigDecimal.ZERO.intValue(), needReduceEmbryoCount);
        }
        Set<String> reduceEmbryoSet = reduceList.stream().map(CxContinueEmbryoInfoHelper::getEmbryoCode).collect(Collectors.toSet());
        return reduceEmbryoSet;
    }

    /**
     * 减少胎胚种类数
     *
     * @param groupContinueInfo         续作计划信息
     * @param continueEmbryoInfoList    续作胎胚集合信息
     * @param continueSkuMouldNumberMap 续作sku信息
     * @param reduceEmbryoCount         需要减少的胎胚种类数
     */
    private static void reduceEmbryoCount(CxContinueInfoHelper groupContinueInfo, List<CxContinueEmbryoInfoHelper> continueEmbryoInfoList, Map<String, CxContinueSkuInfoHelper> continueSkuMouldNumberMap, Integer reduceEmbryoCount) {
        //模具数少的优先，其次是计划量多的优先
        continueEmbryoInfoList.sort(Comparator.comparing(CxContinueEmbryoInfoHelper::getMouldNumber).thenComparing(CxContinueEmbryoInfoHelper::getPlanDemandQty, Comparator.reverseOrder()));
        //提取需要移除的续作胎胚信息
        List<CxContinueEmbryoInfoHelper> reduceList = continueEmbryoInfoList.subList(BigDecimal.ZERO.intValue(), reduceEmbryoCount);
        Set<String> reduceEmbryoSet = reduceList.stream().map(CxContinueEmbryoInfoHelper::getEmbryoCode).collect(Collectors.toSet());
        //将对应的胎胚续作Sku移除
        Map<String, CxContinueSkuInfoHelper> leftOverMap = getLeftOverContinueSku(continueSkuMouldNumberMap, reduceEmbryoSet);
        groupContinueInfo.setContinueSkuMouldNumberMap(leftOverMap);
        allocationFullMachine(groupContinueInfo);
    }

    /**
     * 胎胚种类数超且模具数也超，则先移除胎胚模具数多的(同模具数下，计划量多的优先)
     *
     * @param groupContinueInfo      续作计划信息
     * @param continueEmbryoInfoList 续作胎胚信息集合
     * @param leftOverMap            剩余续作sku信息集合
     * @param needReduceEmbryoCount  需要减少的胎胚个数
     * @param maxMouldNumber         最大模具数
     */
    private static void reduceEmbryoCountAndMouldNumber(CxContinueInfoHelper groupContinueInfo, List<CxContinueEmbryoInfoHelper> continueEmbryoInfoList, Map<String, CxContinueSkuInfoHelper> leftOverMap, Integer needReduceEmbryoCount, Integer maxMouldNumber) {
        //胎胚种类数超，模具数也超，则优先使用模具数多的，量大的
        continueEmbryoInfoList.sort(Comparator.comparing(CxContinueEmbryoInfoHelper::getMouldNumber, Comparator.reverseOrder()).thenComparing(CxContinueEmbryoInfoHelper::getPlanDemandQty, Comparator.reverseOrder()));
        //需要从续作胎胚中移除的
        String embryoCode = continueEmbryoInfoList.get(BigDecimal.ZERO.intValue()).getEmbryoCode();
        Map<String, CxContinueSkuInfoHelper> newLeftOverMap = getLeftOverContinueSku(leftOverMap, embryoCode);
        List<CxContinueSkuInfoHelper> leftOverContinueSkuInfoList = newLeftOverMap.values().stream().collect(Collectors.toList());
        //需移除的胎胚数-1
        needReduceEmbryoCount = needReduceEmbryoCount - BigDecimal.ONE.intValue();
        //剩余的续作模具数-即硫化机台数
        Integer leftOverContinueMouldNumber = leftOverContinueSkuInfoList.stream().mapToInt(CxContinueSkuInfoHelper::getMouldNumber).sum();
        //胎胚不用再移除，模具数也没超
        if (needReduceEmbryoCount <= BigDecimal.ZERO.intValue() && leftOverContinueMouldNumber <= maxMouldNumber) {
            groupContinueInfo.setContinueSkuMouldNumberMap(newLeftOverMap);
            allocationFullMachine(groupContinueInfo);
            return;
        }
        //胎胚不用再移除，模具数超
        if (needReduceEmbryoCount <= BigDecimal.ZERO.intValue() && leftOverContinueMouldNumber > maxMouldNumber) {
            Integer needReduceMouldNumber = leftOverContinueMouldNumber - maxMouldNumber;
            reduceMouldNumber(groupContinueInfo, newLeftOverMap, needReduceMouldNumber);
            return;
        }
        //模具数没超，胎胚个数超
        List<CxContinueEmbryoInfoHelper> leftOverEmbryoList = continueEmbryoInfoList.subList(BigDecimal.ONE.intValue(), continueEmbryoInfoList.size() - BigDecimal.ONE.intValue());
        if (needReduceEmbryoCount > BigDecimal.ZERO.intValue() && leftOverContinueMouldNumber <= maxMouldNumber) {
            reduceEmbryoCount(groupContinueInfo, leftOverEmbryoList, newLeftOverMap, needReduceEmbryoCount);
            return;
        }
        reduceEmbryoCountAndMouldNumber(groupContinueInfo, leftOverEmbryoList, newLeftOverMap, needReduceEmbryoCount, maxMouldNumber);
    }

    /**
     * 根据续作sku信息，转化成续作胎胚信息
     *
     * @param continueSkuMouldNumberMap 续作Sku的信息，包含续作使用模具数以及新的计划量
     * @return
     */
    private static List<CxContinueEmbryoInfoHelper> getCxContinueEmbryoInfoList(Map<String, CxContinueSkuInfoHelper> continueSkuMouldNumberMap) {
        List<CxContinueSkuInfoHelper> continueSkuInfoList = continueSkuMouldNumberMap.values().stream().collect(Collectors.toList());
        Map<String, List<CxContinueSkuInfoHelper>> embryoGroupMap = continueSkuInfoList.stream().collect(Collectors.groupingBy(CxContinueSkuInfoHelper::getEmbryoCode));
        List<CxContinueEmbryoInfoHelper> continueEmbryoInfoList = new ArrayList<>();
        embryoGroupMap.forEach((embryoCode, continueSkuList) -> {
            CxContinueEmbryoInfoHelper continueEmbryoInfo = CxContinueEmbryoInfoHelper.buildEmpty(continueSkuList.get(BigDecimal.ZERO.intValue()));
            Integer mouldNumber = continueSkuList.stream().mapToInt(CxContinueSkuInfoHelper::getMouldNumber).sum();
            Long planDemandQty = continueSkuList.stream().mapToLong(CxContinueSkuInfoHelper::getPlanDemandQty).sum();
            continueEmbryoInfo.setPlanDemandQty(planDemandQty);
            continueEmbryoInfo.setMouldNumber(mouldNumber);
            continueEmbryoInfoList.add(continueEmbryoInfo);
        });
        return continueEmbryoInfoList;
    }

    /**
     * 需要需要从续作胎胚种类数中移除的胎胚信息
     * 先移除没有计划量的，再移除模具数少的，最后移除计划量多的
     *
     * @param continueEmbryoInfoList 所有胎胚种类
     * @param reduceEmbryoCount      需要移除的个数
     * @return
     */
    private static List<CxContinueEmbryoInfoHelper> getReduceEmbryoInfo(List<CxContinueEmbryoInfoHelper> continueEmbryoInfoList, Integer reduceEmbryoCount) {
        //提取计划为零的
        List<CxContinueEmbryoInfoHelper> noPlanList = continueEmbryoInfoList.stream().filter(continueEmbryoInfo -> continueEmbryoInfo.getPlanDemandQty() <= BigDecimal.ZERO.longValue()).collect(Collectors.toList());
        if (noPlanList.size() >= reduceEmbryoCount) {
            noPlanList.sort(Comparator.comparing(CxContinueEmbryoInfoHelper::getMouldNumber));
            return noPlanList.subList(BigDecimal.ZERO.intValue(), reduceEmbryoCount);
        }
        Integer needReduceEmbryoCount = reduceEmbryoCount - noPlanList.size();
        List<CxContinueEmbryoInfoHelper> hasPlanList = continueEmbryoInfoList.stream().filter(continueEmbryoInfo -> continueEmbryoInfo.getPlanDemandQty() > BigDecimal.ZERO.longValue()).collect(Collectors.toList());
        //模具数少的优先，其次是计划量多的优先
        hasPlanList.sort(Comparator.comparing(CxContinueEmbryoInfoHelper::getMouldNumber).thenComparing(CxContinueEmbryoInfoHelper::getPlanDemandQty, Comparator.reverseOrder()));
        noPlanList.addAll(hasPlanList.subList(BigDecimal.ZERO.intValue(), needReduceEmbryoCount));
        return noPlanList;
    }

    /**
     * 在continueSkuMouldNumberMap续作Sku中剔除
     * 生胎在reduceEmbryoSet集合中的续作Sku
     *
     * @param originContinueSkuInfo 原有的续作sku信息
     * @param reduceEmbryoSet       需要从续作sku中移除的胎胚集合
     * @return
     */
    private static Map<String, CxContinueSkuInfoHelper> getLeftOverContinueSku(Map<String, CxContinueSkuInfoHelper> originContinueSkuInfo, Set<String> reduceEmbryoSet) {
        if (CollectionUtils.isEmpty(reduceEmbryoSet)) {
            return originContinueSkuInfo;
        }
        Map<String, CxContinueSkuInfoHelper> leftOverMap = new HashMap<>();
        originContinueSkuInfo.forEach((material, cxContinueSkuInfo) -> {
            if (reduceEmbryoSet.contains(cxContinueSkuInfo.getEmbryoCode())) {
                return;
            }
            leftOverMap.put(material, cxContinueSkuInfo);
        });
        return leftOverMap;
    }

    /**
     * 在continueSkuMouldNumberMap续作Sku中剔除
     * 生胎=reduceEmbryoCode的续作Sku
     *
     * @param originContinueSkuInfo 原因的续作sku信息
     * @param reduceEmbryoCode      需要从续作sku中移除的胎胚
     * @return
     */
    private static Map<String, CxContinueSkuInfoHelper> getLeftOverContinueSku(Map<String, CxContinueSkuInfoHelper> originContinueSkuInfo, String reduceEmbryoCode) {
        Map<String, CxContinueSkuInfoHelper> leftOverMap = new HashMap<>();
        originContinueSkuInfo.forEach((material, cxContinueSkuInfo) -> {
            if (reduceEmbryoCode.equals(cxContinueSkuInfo.getEmbryoCode())) {
                return;
            }
            leftOverMap.put(material, cxContinueSkuInfo);
        });
        return leftOverMap;
    }

    /**
     * 设置续作sku的计划量
     * 取高优先级量还是总净需求量？
     *
     * @param groupPlanInfo     分组计划-TBR为结构
     * @param groupContinueInfo 结构对应的续作信息
     */
    private static void setContinueSkuPlanDemandQty(ProductionPlanGroupInfo groupPlanInfo, CxContinueInfoHelper groupContinueInfo) {
        List<MonthPlanProductionRequirePlanVo> groupPlanList = groupPlanInfo.getGroupPlanData();
        if (CollectionUtils.isEmpty(groupPlanList)) {
            //todo 记录日志
            return;
        }
        Map<String, CxContinueSkuInfoHelper> continueSkuMouldNumberMap = groupContinueInfo.getContinueSkuMouldNumberMap();
        if (CollectionUtils.isEmpty(continueSkuMouldNumberMap)) {
            //todo 记录日志
            return;
        }
        Set<String> skuMaterialDescSet = continueSkuMouldNumberMap.keySet();
        List<MonthPlanProductionRequirePlanVo> continueSkuPlanList = groupPlanList.stream().filter(groupPlan -> groupPlan.hasProduction() && skuMaterialDescSet.contains(groupPlan.getMaterialDesc())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(continueSkuPlanList)) {
            //todo 记录日志
            return;
        }
        Map<String, List<MonthPlanProductionRequirePlanVo>> continueSkuGroupMap = continueSkuPlanList.stream().collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getMaterialDesc));
        Map<String, Long> continueSkuProductionQtyMap = new HashMap<>();
        continueSkuGroupMap.forEach((materialDesc, planList) -> continueSkuProductionQtyMap.put(materialDesc, planList.stream().mapToLong(MonthPlanProductionRequirePlanVo::getHeightProductionQty).sum()));
        continueSkuMouldNumberMap.forEach((materialDesc, cxContinueSkuInfo) -> {
            Long planDemandQty = continueSkuProductionQtyMap.get(materialDesc);
            if (null == planDemandQty) {
                planDemandQty = BigDecimal.ZERO.longValue();
            }
            cxContinueSkuInfo.setPlanDemandQty(planDemandQty);
        });
    }

    /**
     * 是否需要构建满机台的续作信息
     *
     * @param cxMachineCodeSet       续作机台信息
     * @param realWholeMachineNumber 需要使用的机台数
     * @return
     */
    private static boolean isBuilderFullLhMachine(Set<String> cxMachineCodeSet, Integer realWholeMachineNumber) {
        if (null == realWholeMachineNumber || realWholeMachineNumber <= BigDecimal.ZERO.intValue()) {
            return true;
        }
        return cxMachineCodeSet.size() <= realWholeMachineNumber;
    }
}
