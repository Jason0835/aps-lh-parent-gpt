package com.zlt.aps.factory.domain.dto;

import com.tlt.aps.enums.ProductTypeEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.factory.domain.vo.MonthPlanStructureLhRatioVo;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 排产计划分组信息对象
 * TBR 则分组名为结构
 *
 * @author ZLT
 * @date 20251212
 */
@Data
public class ProductionPlanGroupInfo {
    /**
     * 产品品类 TBR 全钢 PCR 半钢
     */
    private ProductTypeEnum productType;
    /**
     * 分组值
     */
    private String groupName;
    /**
     * 分配产能的总需求量
     */
    private Long sumPlanQty;
    /**
     * 最小硫化机台数(结构与硫化配比，取最小)
     */
    private Integer minLhMachineCount;
    /**
     * 结构的SKU中最小的日硫化产能
     */
    private Long minLhDayCapacityQty;
    /**
     * 分组的计划信息
     */
    private List<MonthPlanProductionRequirePlanVo> groupPlanData;
    /**
     * 估算需要的机台数
     */
    private BigDecimal needCxCapacityMachineCount;

    /**
     * 粗步计算 结构需求量需要的成型产能分配
     * 结构总需求量/(结构下SKU最小日硫化量 * 结构最小硫化配比值 * 月份生产天数
     * 保留1位小数
     * 如果 小数部分 > 0.9，则向上取整
     *
     * @param context         排产上下文
     * @param requirePlanList 需排产的计划
     * @param minLhRatioMap   结构下最小的硫化配比信息
     * @return
     */
    public static Map<String, ProductionPlanGroupInfo> statisticsAndEstimateCxAllocationByGroup(Context context, List<MonthPlanProductionRequirePlanVo> requirePlanList, Map<String, MonthPlanStructureLhRatioVo> minLhRatioMap) {
        if (CollectionUtils.isEmpty(requirePlanList)) {
            return Collections.emptyMap();
        }
        //1、对计划按结构分组
        Map<String, List<MonthPlanProductionRequirePlanVo>> groupPlanMap = requirePlanList.stream().collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getStructureName));
        Map<String, ProductionPlanGroupInfo> groupInfoMap = new HashMap<>(groupPlanMap.size());
        groupPlanMap.forEach((structureName, planList) -> {
            ProductionPlanGroupInfo groupInfo = new ProductionPlanGroupInfo();
            groupInfo.setGroupName(structureName);
            groupInfo.setProductType(context.getProductType());
            groupInfo.setGroupPlanData(planList);
            groupInfoMap.put(structureName, groupInfo);
        });
        //2、提取有效净需求--剔除不可排产的-汇总需求量，并获得分组下最小日硫化产能
        groupInfoMap.forEach((structureName, groupInfo) -> {
            List<MonthPlanProductionRequirePlanVo> groupPlanData = groupInfo.getGroupPlanData();
            if (CollectionUtils.isEmpty(groupPlanData)) {
                groupInfo.setSumPlanQty(BigDecimal.ZERO.longValue());
                return;
            }
            //剔除不排产的计划
            List<MonthPlanProductionRequirePlanVo> productionPlanList = groupPlanData.stream().filter(productionPlan -> YesOrNoEnum.YES.getCode().equals(productionPlan.getIsProduction())).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(productionPlanList)) {
                groupInfo.setSumPlanQty(BigDecimal.ZERO.longValue());
                return;
            }
            Long sumPlanQty = productionPlanList.stream().mapToLong(MonthPlanProductionRequirePlanVo::getCxCapacityRequireQty).sum();
            Long minDayLhCapacity = productionPlanList.stream().mapToLong(MonthPlanProductionRequirePlanVo::getDayVulcanizationQty).min().getAsLong();
            groupInfo.setSumPlanQty(sumPlanQty);
            groupInfo.setMinLhDayCapacityQty(minDayLhCapacity);
        });
        //3、根据结构的硫化配比及最小的硫化机台数 估算需要的成型机台数
        groupInfoMap.forEach((structureName, groupInfo) -> {
            MonthPlanStructureLhRatioVo ratioInfo = minLhRatioMap.get(structureName);
            if (null == ratioInfo) {
                return;
            }
            groupInfo.setMinLhMachineCount(ratioInfo.getLhMachineMaxQty());
            groupInfo.calculateNeedCxCapacityMachineCount(context.getMaxProductionDays());
        });
        return groupInfoMap;
    }

    /**
     * 计算需要分配的成型产能机台数，保留1位小数
     * 双模方式
     * 总需求量 / (SKU最小日硫化量 * 2 * 结构最小硫化配比 * 月度可排产天数),两位小数
     * 如果 小数部分 >0.9，则向上取整
     * 否则 = 保留1位小数
     *
     * @param monthMaxProductionDays 月度最大可生产天数
     */
    public void calculateNeedCxCapacityMachineCount(Integer monthMaxProductionDays) {
        if (sumPlanQty <= BigDecimal.ZERO.intValue()) {
            needCxCapacityMachineCount = BigDecimal.ZERO;
        }
        if (minLhMachineCount <= BigDecimal.ZERO.intValue()) {
            needCxCapacityMachineCount = BigDecimal.ZERO;
        }
        if (minLhDayCapacityQty <= BigDecimal.ZERO.intValue()) {
            needCxCapacityMachineCount = BigDecimal.ZERO;
        }
        //单台成型月产能 = 最低硫化机台数 * 最小硫化量(单模) * 2 * 月份可排产天数
        BigDecimal singleCxMonthCapacity = BigDecimal.valueOf(minLhMachineCount).multiply(BigDecimal.valueOf(minLhDayCapacityQty)).multiply(BigDecimal.valueOf(ProductionConstant.DOUBLE_MOULD_PRODUCTION)).multiply(BigDecimal.valueOf(Long.valueOf(monthMaxProductionDays)));
        BigDecimal machineCount = BigDecimal.valueOf(sumPlanQty).divide(singleCxMonthCapacity, 2, RoundingMode.HALF_UP);
        //取整数部分，向下取整
        BigDecimal integerPart = machineCount.setScale(0, RoundingMode.DOWN);
        //小数部分
        BigDecimal decimalPart = machineCount.subtract(integerPart);
        if (decimalPart.compareTo(BigDecimal.valueOf(ProductionConstant.REPAIR_WHOLE)) > BigDecimal.ZERO.intValue()) {
            needCxCapacityMachineCount = integerPart.add(BigDecimal.ONE);
            return;
        }
        needCxCapacityMachineCount = machineCount.setScale(1, RoundingMode.UP);
    }
}
