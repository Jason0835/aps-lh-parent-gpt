package com.zlt.aps.factory.scheduling.cxcapacity;

import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.factory.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.factory.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.factory.domain.vo.SpecialMaterialInfoVo;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
     * 根据结构特殊材料情况重算结束日期
     *
     * @param allocationHelper   分配信息
     * @param productionContext  排产上下文
     * @param productionPlanInfo 分组计划
     * @return
     */
    public Integer calculateConfirmAllocationDaysBySpecialMaterial(CxMachineAllocationPlanHelper allocationHelper, TbrProductionContext productionContext, ProductionPlanGroupInfo productionPlanInfo) {
        //理论分配天数
        Integer needAllocationDays = allocationHelper.getAllocationDay();
        if (null == allocationHelper || null == productionPlanInfo) {
            return needAllocationDays;
        }
        //非特殊结构直接跳过
        if (!productionPlanInfo.isSpecialMaterial()) {
            return needAllocationDays;
        }
        Integer endDay = allocationHelper.getEndDay();
        //如果是本月排产最后一天，直接跳过，不需要拉量或者舍弃
        if (endDay.equals(productionContext.getProductionEndDay())) {
            return needAllocationDays;
        }
        // 判断如果是特殊结构，需要判断是否最后一个结构-本结构涉及的特殊材料清单
        Map<String, BigDecimal> materialMap = productionPlanInfo.getEmbryoSpecialMaterialInfoMap();
        List<ProductionPlanGroupInfo> allGroupPlanList = productionContext.getGroupProductionInfo().values().stream().collect(Collectors.toList());
        // 取出与本结构使用相同特殊材料的其他结构信息(过滤使用相同特殊材料的结构)
        List<ProductionPlanGroupInfo> specialPlanList = allGroupPlanList.stream().filter(plan -> {
            //检查本结构之外的特殊结构
            if (plan == productionPlanInfo) {
                return false;
            }
            //涉及的特殊材料清单
            Map<String, BigDecimal> otherMaterialMap = plan.getEmbryoSpecialMaterialInfoMap();
            if (CollectionUtils.isEmpty(otherMaterialMap)) {
                return false;
            }
            //特殊材料与新增结构的特殊材料清单有交集
            return materialMap.keySet().stream().anyMatch(material -> otherMaterialMap.containsKey(material));
        }).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(specialPlanList)) {
            return needAllocationDays;
        }
        //其他特殊规格有任意一个没有排完，说明还不是最后一个结构，跳过
        if (specialPlanList.stream().anyMatch(plan -> plan.getLeftOverNeedAllocationDays() > BigDecimal.ZERO.intValue())) {
            return needAllocationDays;
        }
        //打上最后一个规格的标记
        productionPlanInfo.setIsLatestSpecialMaterial(true);
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
        Integer allocationQty = specialPlanList.stream().mapToInt(plan -> plan.getMinLhDayCapacityQty() * plan.getMinLhMachineCount() * plan.getTheoryDays()).sum();
        // 同特殊材料结构总预计排产量
        Integer sumPlanQty = allocationQty + productionPlanInfo.getSumPlanQty();
        // 取出各结构的特殊材料清单交集
        Map<String, BigDecimal> specialIntersectionMap = new HashMap<>();
        specialIntersectionMap.putAll(materialMap);
        for (ProductionPlanGroupInfo plan : specialPlanList) {
            plan.getEmbryoSpecialMaterialInfoMap().keySet().stream().forEach(materialCode -> {
                if (!specialIntersectionMap.containsKey(materialCode)) {
                    specialIntersectionMap.remove(materialCode);
                }
            });
        }
        // 都没有交集，直接重置未本结构的物料清单
        if (CollectionUtils.isEmpty(specialIntersectionMap)) {
            specialIntersectionMap.putAll(materialMap);
        }
        Map.Entry<String, BigDecimal> entry = specialIntersectionMap.entrySet().stream().findFirst().get();
        // 单耗
        BigDecimal unitConsumeQty = entry.getValue();
        // 标准长度
        Long standardLength = specialMaterialInfoMap.get(entry.getKey()).keySet().stream().findFirst().get();
        //计算余数
        BigDecimal remainderQty = BigDecimalUtils.multiply(sumPlanQty, unitConsumeQty).remainder(BigDecimalUtils.valueOf(standardLength));
        // 区间阈值 = 硫化量 * 配比*单耗
        BigDecimal threshold = BigDecimalUtils.multiply(productionPlanInfo.getMinLhDayCapacityQty(),
                productionPlanInfo.getMinLhMachineCount(), unitConsumeQty);
        boolean isAddQty = false;
        Integer productionQty = productionPlanInfo.getSumPlanQty();
        // 重算实际的量
        Integer realProductionQty = 0;
        // 超过阈值，尝试补量
        if (remainderQty.compareTo(threshold) >= 0) {
            if (limitProductionQty >= productionQty + standardLength - threshold.intValue()) {
                //检查补量后不超过可生产上限才进行补量
                isAddQty = true;
                realProductionQty = (int) (productionQty + standardLength - threshold.intValue());
            }
        }
        // 不补量，则需要将计划量扣减掉余数部分
        if (!isAddQty) {
            realProductionQty = productionQty - threshold.intValue();
        }
        // 可生产上限不足，则不能排产
        if (realProductionQty <= 0) {
            return BigDecimal.ZERO.intValue();
        }
        //计算排产天数 = ceil(计划量 / 日硫化量 / 配比)
        BigDecimal theoryDays = BigDecimalUtils.div(realProductionQty, BigDecimalUtils
                        .multiply(productionPlanInfo.getMinLhDayCapacityQty(), productionPlanInfo.getMinLhMachineCount(), true),
                2, false);
        theoryDays = theoryDays.setScale(0, RoundingMode.UP);
        return needAllocationDays + theoryDays.intValue();
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
            if (specialMaterialInfo == null) {
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
            if (limitProductionQty == null) {
                limitProductionQty = stockCanProductionQty;
            } else {
                limitProductionQty = Math.min(limitProductionQty, stockCanProductionQty);
            }
        }
        return limitProductionQty;
    }

}
