package com.zlt.aps.mp.engine.scheduling.cxcapacity;

import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanMouldDayResult;
import com.zlt.aps.mp.api.domain.entity.SpecialMaterialResult;
import com.zlt.aps.mp.engine.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.mp.engine.domain.vo.SpecialMaterialBomRelationVo;
import com.zlt.aps.mp.engine.domain.vo.SpecialMaterialInfoVo;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 结构原特殊材料业务处理
 *
 * @author ZLT
 * @date 20260206
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpecialMaterialScheduleHandler {

    /**
     * 根据结构特殊材料情况重算特殊结构需要排产的天数
     *
     * @param allocationHelper   分配信息
     * @param productionContext  排产上下文
     * @param productionPlanInfo 分组计划
     * @return
     */
    public Integer calculateConfirmAllocationDaysBySpecialMaterial(CxMachineAllocationPlanHelper allocationHelper, TbrProductionContext productionContext, ProductionPlanGroupInfo productionPlanInfo) {
        if (null == allocationHelper || null == productionPlanInfo) {
            return null;
        }
        //理论分配天数
        Integer needAllocationDays = allocationHelper.getAllocationDay();
        boolean isContinue = hasFindOtherGroupBySpecialMaterial(productionContext, productionPlanInfo, allocationHelper);
        if( !isContinue){
            return needAllocationDays;
        }
        //取出与本结构使用相同特殊材料的其他结构信息(过滤使用相同特殊材料的结构)
        List<ProductionPlanGroupInfo> otherNeedProductionSpecialPlanList = getSameSpecialMaterialOtherGroupList(productionContext, productionPlanInfo);
        //其他特殊规格有任意一个没有排完，说明还不是最后一个结构，跳过
        if (!isLastSpecialMaterialGroup(otherNeedProductionSpecialPlanList)) {
            return needAllocationDays;
        }
        //打上最后一个分组的标记
        productionPlanInfo.setIsLatestSpecialMaterial(true);
        //本结构涉及的特殊材料清单
        Map<String, BigDecimal> materialMap = productionPlanInfo.getEmbryoSpecialMaterialInfoMap();
        //特殊材料库存列表
        Map<String, Map<Long, SpecialMaterialInfoVo>> specialMaterialInfoMap = productionContext.getSpecialMaterialInfoMap();
        //特殊材料库存的可生产上限
        Integer limitProductionQty = calculateLimitProductionQtyByStock(materialMap, specialMaterialInfoMap);
        //可生产上限不足，则不能排产
        if (limitProductionQty <= BigDecimal.ZERO.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        /**
         * 判断同特殊材料排排产量落在哪个区间：
         * 1、计划量*单号模除标准长度，如果余数小于日硫化量 * 配比*单耗：舍弃余数部分
         * 2、计划量*单号模除标准长度，如果余数大于等于日硫化量 * 配比*单耗：计划量补标准长度 - 日硫化量 * 配比*单耗
         * 统计已排量 = 日硫化量 * 配比 * 已排天数
         */
//        Integer otherAllocationQty = otherNeedProductionSpecialPlanList.stream().mapToInt(ProductionPlanGroupInfo::getSumPlanQty).sum();
        Long otherAllocationQty = productionContext.getSpecialMaterialSumProductionQty(productionPlanInfo);
        // 同特殊材料结构总预计排产量 productionPlanInfo.getSumPlanQty()
//        Integer sumPlanQty = otherAllocationQty + productionPlanInfo.getSumPlanQty();
        // 取出各结构的特殊材料清单交集
        Map<String, BigDecimal> specialIntersectionMap = new HashMap<>();
        specialIntersectionMap.putAll(materialMap);
        for (ProductionPlanGroupInfo plan : otherNeedProductionSpecialPlanList) {
            plan.getEmbryoSpecialMaterialInfoMap().keySet().stream().forEach(materialCode -> {
                if (!specialIntersectionMap.containsKey(materialCode)) {
                    specialIntersectionMap.remove(materialCode);
                }
            });
        }
        // 都没有交集，直接重置为本结构的物料清单
        if (CollectionUtils.isEmpty(specialIntersectionMap)) {
            specialIntersectionMap.putAll(materialMap);
        }
        Map.Entry<String, BigDecimal> entry = specialIntersectionMap.entrySet().stream().findFirst().get();
        // 单耗
        BigDecimal unitConsumeLength = entry.getValue();
        // 标准长度
        BigDecimal standardLength = specialMaterialInfoMap.get(entry.getKey()).keySet().stream().findFirst().map(BigDecimalUtils::valueOf).get();
        Integer standardQty = standardLength.divide(unitConsumeLength, BigDecimal.ZERO.intValue(), RoundingMode.DOWN).intValue();
        // 需求量
        Integer productionQty = Math.min(limitProductionQty, productionPlanInfo.getSumPlanQty()); // 取生产上限与需求量的较小值
//      Integer productionQty = productionPlanInfo.getRemainingMaxProductionQty();
        BigDecimal sumPlanQty = BigDecimalUtils.multiply(productionQty, unitConsumeLength);
        sumPlanQty = BigDecimalUtils.add(sumPlanQty, otherAllocationQty); // 加上已排量
        //计算余数
        BigDecimal remainderLength = sumPlanQty.remainder(standardLength); // 长度
        BigDecimal remainderQty = BigDecimalUtils.div(remainderLength, unitConsumeLength, 0); // 条数
        // 区间阈值 = 硫化量 * 配比*单耗
        Integer floatThreshold = productionPlanInfo.getThreshold();
        // 区间阈值 = 硫化量 * 配比*单耗
        BigDecimal thresholdLength = BigDecimalUtils.multiply(floatThreshold, unitConsumeLength);
        boolean isAddQty = false;
        // 重算实际的量
        Integer realProductionQty = BigDecimal.ZERO.intValue();
        // 超过阈值，尝试补量
        if (remainderLength.compareTo(thresholdLength) >= BigDecimal.ZERO.intValue()) {
            if (limitProductionQty >= productionQty + standardQty - floatThreshold) {
                //检查补量后不超过可生产上限才进行补量
                isAddQty = true;
                realProductionQty = productionQty + standardQty - floatThreshold;
            }
        }
        // 不补量，则需要将计划量扣减掉余数部分
        if (!isAddQty) {
            realProductionQty = productionQty - remainderQty.intValue();
        }
        // 可生产上限不足，则不能排产
        if (realProductionQty <= BigDecimal.ZERO.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        //计算新的排产天数 = ceil(计划量 / 日硫化量 / 配比)
        Integer firstQty = productionContext.getBaseDataContainer().getParamConfiguration().getChangeMouldFirstQty();
        BigDecimal lhMachineCount = BigDecimalUtils.valueOf(productionPlanInfo.getMinLhMachineCountByMould());
        BigDecimal firstDayProductionQty = BigDecimalUtils.multiply(firstQty, lhMachineCount); // 首日排产量
        Integer resultTheoryDays = 0;
        if (realProductionQty > firstDayProductionQty.intValue()) { // 需排产量大于首日排产量，则计算除了首日之外的计划所需天数
            BigDecimal otherDayProductionQty = BigDecimalUtils.sub(realProductionQty, firstDayProductionQty); // 其余日排产量
            BigDecimal theoryDays = BigDecimalUtils.div(otherDayProductionQty, floatThreshold, 2, false);
            resultTheoryDays = theoryDays.setScale(0, RoundingMode.UP).intValue() + BigDecimal.ONE.intValue(); // 实际天数=其余排产量所需天数 + 首日天数（1天）
        } else {
            resultTheoryDays = BigDecimal.ONE.intValue(); // 低于首日的，只排一天
        }
        // 算其他特殊材料有交集的结构已排天数，不到最小排产天数的给加到最小排产天数
        Integer otherTheoryDays = otherNeedProductionSpecialPlanList.stream().mapToInt(g -> g.getTheoryDays()).sum();
        Integer minAllocationDays = productionContext.getBaseDataContainer().getParamConfiguration().getMinAllocationDays();
        // 根据最大浮动余量计算结构排产的最低天数
        Integer maxFloatThreshold = productionPlanInfo.getMaxThreshold(); // 最大浮动余量
        BigDecimal minTheoryDays = BigDecimalUtils.div(realProductionQty, maxFloatThreshold, 2, false);
        if (minTheoryDays.intValue() + otherTheoryDays < minAllocationDays) { // 最低浮动天数低于最小分配天数，直接按低于最小分配天数
            resultTheoryDays = minAllocationDays;
        } else if (resultTheoryDays + otherTheoryDays < minAllocationDays) { // 浮动天数低于最小分配天数，直接按低于最小分配天数
            resultTheoryDays = minAllocationDays;
        }
        if (isAddQty) {
            //拉量时，不能进行提前收尾处理
            productionPlanInfo.setHasBeforeConclusionHandler(false);
        }
        return resultTheoryDays;
    }

    /**
     * 计算特殊材料排程结果<br/>
     * 通过 BOM 关联 VO 获取胎胚、半部件、原材料、工艺信息的完整链路，计算每种原材料的需求量（米），
     * 公式：planList.totalQty * 长度 * 宽度 / 幅宽<br/>
     * 按 childMaterialCode 合并总需求量后，结合库存计算出实际可领取量（整 standardLength 倍数）。
     *
     * @param planList        生产计划列表
     * @param bomRelationList BOM 关联 VO 列表（含胎胚→半部件→原材料→工艺信息）
     * @param stockList       特殊材料库存列表
     * @return 特殊材料排程结果
     */
    public List<SpecialMaterialResult> calSpecialMaterialResult(List<FactoryMonthPlanMouldDayResult> planList,
            List<SpecialMaterialBomRelationVo> bomRelationList, List<SpecialMaterialInfoVo> stockList) {
        if (CollectionUtils.isEmpty(planList) || CollectionUtils.isEmpty(bomRelationList)) {
            return Collections.emptyList();
        }

        // ========== 步骤1：按胎胚汇总计划量 ==========
        Map<String, Long> embryoTotalQtyMap = planList.stream()
                .filter(p -> p.getTotalQty() != null && p.getTotalQty() > 0)
                .collect(Collectors.groupingBy(
                        FactoryMonthPlanMouldDayResult::getEmbryoCode,
                        Collectors.summingLong(FactoryMonthPlanMouldDayResult::getTotalQty)
                ));

        // ========== 步骤2：按 embryoCode 分组 VO，计算每种原材料的需求量 ==========
        Map<String, List<SpecialMaterialBomRelationVo>> embryoVoMap = bomRelationList.stream()
                .collect(Collectors.groupingBy(SpecialMaterialBomRelationVo::getEmbryoCode));

        Map<String, Long> demandMap = new HashMap<>();
        for (Map.Entry<String, Long> entry : embryoTotalQtyMap.entrySet()) {
            String embryoCode = entry.getKey();
            Long planQty = entry.getValue();
            List<SpecialMaterialBomRelationVo> vos = embryoVoMap.get(embryoCode);
            if (CollectionUtils.isEmpty(vos)) {
                continue;
            }
            for (SpecialMaterialBomRelationVo vo : vos) {
                // 需求量（米）= 计划量 * 长度 * 宽度 / 幅宽，向上取整
                BigDecimal length = new BigDecimal(vo.getProcessLength());
                BigDecimal width = new BigDecimal(vo.getProcessWidth());
                BigDecimal fabricWidth = new BigDecimal(vo.getProcessFabricWidth());
                BigDecimal demand = BigDecimalUtils.valueOf(planQty)
                        .multiply(length).multiply(width)
                        .divide(fabricWidth, 0, RoundingMode.CEILING);
                demandMap.merge(vo.getChildMaterialCode(), demand.longValue(), Long::sum);
            }
        }

        // ========== 步骤3：按库存计算实际可领取量（整 standardLength 倍数）==========
        Map<String, List<SpecialMaterialInfoVo>> stockGroupMap = Collections.emptyMap();
        if (!CollectionUtils.isEmpty(stockList)) {
            stockGroupMap = stockList.stream()
                    .filter(s -> s.getStandardLength() != null && s.getStandardLength() > 0)
                    .collect(Collectors.groupingBy(SpecialMaterialInfoVo::getMaterialCode));
        }

        // ========== 步骤4：生成结果（按标准长度拆分，每条标准长度生成一笔记录）==========
        List<SpecialMaterialResult> resultList = new ArrayList<>();
        for (Map.Entry<String, Long> entry : demandMap.entrySet()) {
            String materialCode = entry.getKey();
            Long demand = entry.getValue();
            if (demand <= 0) {
                continue;
            }

            List<SpecialMaterialInfoVo> stocks = stockGroupMap.getOrDefault(materialCode, Collections.emptyList());

            if (CollectionUtils.isEmpty(stocks)) {
                // 无库存，按需求量作为总需求
                SpecialMaterialResult result = new SpecialMaterialResult();
                result.setMaterialCode(materialCode);
                result.setMaterialDesc("");
                result.setStandardLength(0L);
                result.setOriStandardLength(0L);
                result.setTotalQty(demand);
                resultList.add(result);
            } else {
                // 最优搭配明细：standardLength → 总长度
                Map<Long, Long> breakdown = buildOptimalSupplyBreakdown(demand, stocks);
                long totalSupply = breakdown.values().stream()
                        .mapToLong(Long::longValue).sum();

                if (totalSupply <= demand) {
                    // 总供应不足或刚好，直接使用供应量作为 totalQty
                    for (Map.Entry<Long, Long> be : breakdown.entrySet()) {
                        addResultItem(resultList, materialCode, stocks, be.getKey(), be.getValue());
                    }
                } else {
                    // 总供应超出需求量，将需求量分配到各笔（按标准长降序，每笔不超过其供应量）
                    long remainingDemand = demand;
                    List<Map.Entry<Long, Long>> sortedEntries = new ArrayList<>(breakdown.entrySet());
                    sortedEntries.sort((a, b) -> Long.compare(b.getKey(), a.getKey()));
                    for (Map.Entry<Long, Long> be : sortedEntries) {
                        Long stdLen = be.getKey();
                        Long supply = be.getValue();
                        long allocQty = Math.min(supply, remainingDemand);
                        if (allocQty <= 0) continue;
                        addResultItem(resultList, materialCode, stocks, stdLen, allocQty);
                        remainingDemand -= allocQty;
                    }
                }
            }
        }

        return resultList;
    }

    /**
     * 最优搭配计算（带明细拆分）：使用受限背包DP（二进制拆分优化），
     * 找出最接近需求量的库存领取组合，返回 {standardLength → 领取总长度} 映射。
     * <p>
     * 示例：需求2200米，库存有600米(2卷)、1000米(2卷)，最优组合为1000×1+600×2=2200，
     * 返回 {1000 → 1000, 600 → 1200}。
     * </p>
     *
     * @param demand 需求量（米）
     * @param stocks 库存列表（不同 standardLength）
     * @return 最优搭配明细映射：standardLength → 领取总长度
     */
    private Map<Long, Long> buildOptimalSupplyBreakdown(long demand, List<SpecialMaterialInfoVo> stocks) {
        // 所有库存可供应总量
        long totalAvailable = stocks.stream()
                .mapToLong(s -> {
                    long sl = s.getStandardLength();
                    long avail = s.getStock() != null ? s.getStock() : 0L;
                    return sl * avail;
                }).sum();
        // 库存全部用完
        if (totalAvailable <= demand) {
            Map<Long, Long> fullMap = new HashMap<>();
            for (SpecialMaterialInfoVo s : stocks) {
                long sl = s.getStandardLength();
                long avail = s.getStock() != null ? s.getStock() : 0L;
                fullMap.merge(sl, sl * avail, Long::sum);
            }
            return fullMap;
        }
        // 最大标准长度（决定DP上界）
        long maxSl = stocks.stream()
                .mapToLong(SpecialMaterialInfoVo::getStandardLength)
                .max().orElse(0L);
        int limit = (int) Math.min(demand + maxSl, totalAvailable);

        // 二进制拆分构建物品列表，每件物品记录其标准长度和总长度
        List<Long> itemStdLengths = new ArrayList<>();  // 每件物品的标准长度
        List<Long> itemAmounts = new ArrayList<>();     // 每件物品的总长度

        for (SpecialMaterialInfoVo stock : stocks) {
            long sl = stock.getStandardLength();
            int avail = stock.getStock() != null ? stock.getStock().intValue() : 0;
            if (avail <= 0) continue;

            int remaining = avail;
            for (int bit = 1; bit <= remaining; bit <<= 1) {
                remaining -= bit;
                long addAmount = sl * bit;
                if (addAmount > limit) break;
                itemStdLengths.add(sl);
                itemAmounts.add(addAmount);
            }
            if (remaining > 0) {
                long addAmount = sl * remaining;
                if (addAmount <= limit) {
                    itemStdLengths.add(sl);
                    itemAmounts.add(addAmount);
                }
            }
        }

        // DP 求可达值 + 记录路径（prev[v] = 使v可达的物品下标）
        boolean[] dp = new boolean[limit + 1];
        int[] prev = new int[limit + 1];
        Arrays.fill(prev, -1);
        dp[0] = true;

        for (int i = 0; i < itemAmounts.size(); i++) {
            long addAmount = itemAmounts.get(i);
            if (addAmount > limit) continue;
            for (int v = limit; v >= addAmount; v--) {
                if (dp[v - (int) addAmount] && !dp[v]) {
                    dp[v] = true;
                    prev[v] = i;
                }
            }
        }

        // 找 >= demand 的最小可达值
        int bestV = (int) demand;
        while (bestV <= limit && !dp[bestV]) {
            bestV++;
        }
        if (bestV > limit) {
            bestV = (int) totalAvailable;
        }

        // 回溯还原组合
        Map<Long, Long> breakdown = new HashMap<>();
        int v = bestV;
        while (v > 0) {
            int idx = prev[v];
            if (idx < 0) break;
            long stdLen = itemStdLengths.get(idx);
            long amount = itemAmounts.get(idx);
            breakdown.merge(stdLen, amount, Long::sum);
            v -= (int) amount;
        }
        return breakdown;
    }

    /**
     * 添加一笔 SpecialMaterialResult 到结果列表
     *
     * @param resultList   结果列表
     * @param materialCode 原材料编码
     * @param stocks       该原材料的库存列表（用于查找描述信息）
     * @param stdLen       标准长度
     * @param totalQty     分配量（米）
     */
    private void addResultItem(List<SpecialMaterialResult> resultList, String materialCode,
            List<SpecialMaterialInfoVo> stocks, Long stdLen, Long totalQty) {
        SpecialMaterialInfoVo match = stocks.stream()
                .filter(s -> s.getStandardLength() != null
                        && s.getStandardLength().equals(stdLen))
                .findFirst().orElse(null);
        SpecialMaterialResult result = new SpecialMaterialResult();
        result.setMaterialCode(materialCode);
        result.setMaterialDesc(match != null ? match.getMaterialDesc() : "");
        result.setStandardLength(stdLen);
        result.setOriStandardLength(match != null ? match.getOriStandardLength() : 0L);
        result.setTotalQty(totalQty);
        resultList.add(result);
    }

    /**
     * 是否需要执行查找其它还需排产的特殊材料分组计划
     * true 不需要执行 false 需要执行
     *
     * @param productionContext  排产上下文
     * @param productionPlanInfo 当前排产分组
     * @param allocationHelper   分配情况
     * @return
     */
    private boolean hasFindOtherGroupBySpecialMaterial(TbrProductionContext productionContext, ProductionPlanGroupInfo productionPlanInfo, CxMachineAllocationPlanHelper allocationHelper) {
        //非特殊结构直接跳过
        if (!productionPlanInfo.isSpecialMaterial()) {
            return false;
        }
        //如果有特殊材料分组(结构)已经排产到月底，则不需要拉量或是舍弃
        if (hasLastDayProductionSpecialMaterial(productionContext)) {
            return false;
        }
        //如果自己是本月排产最后一天，直接跳过，不需要拉量或者舍弃
        Integer endDay = allocationHelper.getEndDay();
        if (productionContext.isProductionEndDay(endDay)) {
            return false;
        }
        return true;
    }

    /**
     * 获取与当前分组计划相同原材料的其他还有排产的分组计划
     * TBR 为结构
     *
     * @param productionContext      排产上下文
     * @param currentProductionGroup 当前排产分组
     */
    private List<ProductionPlanGroupInfo> getSameSpecialMaterialOtherGroupList(TbrProductionContext productionContext, ProductionPlanGroupInfo currentProductionGroup) {
        //当前排产分组的特殊材料清单
        Map<String, BigDecimal> materialMap = currentProductionGroup.getEmbryoSpecialMaterialInfoMap();
        //所有分组计划
        List<ProductionPlanGroupInfo> allGroupPlanList = productionContext.getGroupProductionInfo().values().stream().collect(Collectors.toList());
        // 取出与本结构使用相同特殊材料的其他结构信息(过滤使用相同特殊材料的结构)
        List<ProductionPlanGroupInfo> specialPlanList = allGroupPlanList.stream().filter(plan -> {
            //检查本结构之外的特殊结构
            if (plan == currentProductionGroup) {
                return false;
            }
            if (null == plan.getMinLhDayCapacityQty() || null == plan.getMinLhMachineCount()) {
                return false;
            }
//            if (plan.getRemainingNeedAllocationDays() <= BigDecimal.ZERO.intValue()) {
//                return false;
//            }
            //涉及的特殊材料清单
            Map<String, BigDecimal> otherMaterialMap = plan.getEmbryoSpecialMaterialInfoMap();
            if (CollectionUtils.isEmpty(otherMaterialMap)) {
                return false;
            }
            //特殊材料与新增结构的特殊材料清单有交集
            return materialMap.keySet().stream().anyMatch(material -> otherMaterialMap.containsKey(material));
        }).collect(Collectors.toList());
        return specialPlanList;
    }

    /**
     * 判断是否为最后一个特殊材料排产
     * 1、没有其它特殊材料的结构 = true
     * 2、其它特殊材料的结构没有还需排产的量(剩余需分配的量) = true
     *
     * @param otherSpecialMaterialGroupList 其它特殊材料的结构
     * @return
     */
    private boolean isLastSpecialMaterialGroup(List<ProductionPlanGroupInfo> otherSpecialMaterialGroupList) {
        if (CollectionUtils.isEmpty(otherSpecialMaterialGroupList)) {
            return true;
        }
        return !otherSpecialMaterialGroupList.stream().anyMatch(plan -> plan.getRemainingNeedAllocationDays() > BigDecimal.ZERO.intValue());
    }

    /**
     * 计算特殊材料库存的最大可排产量
     *
     * @param materialMap            特殊材料用量清单
     * @param specialMaterialInfoMap 特殊材料库存
     * @return
     */
    private Integer calculateLimitProductionQtyByStock(Map<String, BigDecimal> materialMap, Map<String, Map<Long, SpecialMaterialInfoVo>> specialMaterialInfoMap) {
        //根据各材料的库存使用情况限制排产量
        Integer limitProductionQty = null;
        // 预估本结构的用量
        for (Map.Entry<String, BigDecimal> entry : materialMap.entrySet()) {
            //特殊材料物料
            String materialCode = entry.getKey();
            //单胎消耗量
            BigDecimal unitConsumeQty = entry.getValue();
            //取出各标准用量的特殊材料库存
            Map<Long, SpecialMaterialInfoVo> specialMaterialInfo = specialMaterialInfoMap.get(materialCode);
            if (null == specialMaterialInfo) {
                limitProductionQty = BigDecimal.ZERO.intValue();
                break;
            }
            // 累计可用库存
            Long totalStock = specialMaterialInfo.values().stream().mapToLong(s -> s.getStock() - s.getSumProductionQty()).sum();
            if (totalStock <= BigDecimal.ZERO.intValue()) {
                limitProductionQty = BigDecimal.ZERO.intValue();
                break;
            }
            // 换算成成品数
            Integer stockCanProductionQty = BigDecimalUtils.div(totalStock, unitConsumeQty, 2)
                    .setScale(0, RoundingMode.DOWN).intValue();
            // 如果未分配量还有剩余，需要更新可排产量
            if (null == limitProductionQty) {
                limitProductionQty = stockCanProductionQty;
            } else {
                limitProductionQty = Math.min(limitProductionQty, stockCanProductionQty);
            }
        }
        return limitProductionQty;
    }

    /**
     * 判断已排产的特殊材料分组(结构)是否有月底最后一天
     * 只要已排产的特殊材料分组(结构)中有一个在月底最后一天排产，则表示排产到月底
     * 看分配的天数是否到月底
     *
     * @param productionContext 排产上下文
     * @return
     */
    private boolean hasLastDayProductionSpecialMaterial(TbrProductionContext productionContext) {
        Map<String, CxMachineBaseInfoVo> allCxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        if (CollectionUtils.isEmpty(allCxMachineInfo)) {
            return false;
        }
        return allCxMachineInfo.values().stream().anyMatch(singleCxMachine -> {
            //判断只要有一台成型机分配的特殊材料在月底，就表示排产到月底
            List<CxMachineAllocationPlanHelper> allocationList = singleCxMachine.getAllocationList();
            if (CollectionUtils.isEmpty(allocationList)) {
                return false;
            }
            return allocationList.stream().anyMatch(singleAllocation -> {
                //判断只要有一段排产的特殊材料分组(结构)在月底，则表示月底
                ProductionPlanGroupInfo productionPlanInfo = singleAllocation.getProductionPlanInfo();
                if (null == productionPlanInfo || !productionPlanInfo.isSpecialMaterial()) {
                    return false;
                }
                if (singleAllocation.getAllocationDay() <= BigDecimal.ZERO.intValue()) {
                    return false;
                }
                return productionContext.isProductionEndDay(singleAllocation.getEndDay());
            });
        });
    }

    
    
}
