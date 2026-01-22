package com.zlt.aps.factory.daylimit;

import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.factory.domain.vo.ProductionMouldInfoVo;
import com.zlt.aps.factory.enums.MouldProductionLimitTypeEnum;
import com.zlt.aps.factory.handler.ContinuousProductionDayHandler;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 硫化组排产范围计算器
 * 根据硫化组的排产范围[lhGroupStartDay~lhGroupEndDay]
 * 计算最终可进行排产的排产日集合
 * 硫化组来源有两种：
 * 1、一种是按结构排产，数据有来源于ProductionPlanGroupInfo.dayProductionLimitInfo
 * 2、在模拟排产阶段，通过机台与分组计划互选时，数据来源于CxMachineBaseInfoVo.dayProductionLimitInfo
 * 2.1、在机结构在机机台不够时，需要新机台
 * 2.2、新增结构，需要新机台
 * 计算最终可排产日集合，需要考虑以下场景
 * 1、硫化组关联的机台对应的胎胚种类数的限制
 * 2、挑选的模具本身排产日的限制(模具可排产日以及模具已排日)
 * 3、挑选的模具对应的模壳总数限制
 * 4、挑选的模具对应的胶囊总数限制
 * 5、排产的计划，对应的结构+主花纹的模具分配比例限制
 *
 * @author ZLT
 * @date 20260117
 */
@Slf4j
public class LhGroupProductionRangeCalculator {

    /**
     * 根据排产计划，选中的模具及符合胎胚种类数限制集合
     * 得到最终可排产的范围集合
     * 1、先取得符合胎胚种类数限制的排产日集合，取得交集
     * 2、再根据选择模具的排产日集合，取得交集
     * 3、再根据模壳数量限制的排产日集合，取得交集
     * 4、再依据模具分配比例限制的排产日集合，取得交集
     *
     * @param productionContext 排产上下文
     * @param addSkuInfo        排产计划信息
     * @param lhGroupStartDay   预选硫化组开始排产日(通常为前一Sku的收尾日)
     * @param lhGroupEndDay     预选硫化组结束排产日(通常为结构收尾日)
     * @param selectedMould     选中的模具
     * @param dayLimitList      同结构下的日排产限制集合信息
     * @param stopDaySet        停工日集合(忽略机台时，传整体停工日，但机台时为机台停工日)
     * @return
     */
    public static MouldProductionDayLimitHelper confirmProductionRange(TbrProductionContext productionContext, MonthPlanProductionRequirePlanVo addSkuInfo, Integer lhGroupStartDay, Integer lhGroupEndDay, List<ProductionMouldInfoVo> selectedMould, List<GroupPlanCxLhCapacityLimitHelper> dayLimitList, Set<Integer> stopDaySet) {
        String productionEmbryoCode = addSkuInfo.getEmbryoCode();
        //在满足硫化配比限制下，取得排产计划可排的满足胎胚种类数限制的排产范围
        List<GroupPlanCxLhCapacityLimitHelper> hasAddSkuList = dayLimitList.stream().filter(dayLimit -> !dayLimit.isReachLimitByEmbryoCode(productionEmbryoCode)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasAddSkuList)) {
            //说明达到胎胚种类数限制
            return new MouldProductionDayLimitHelper(Collections.emptySet(), MouldProductionLimitTypeEnum.EMBRYO_CODE_COUNT_LIMIT);
        }
        //取得与胎胚种类数范围的交集
        Set<Integer> effectiveDaySet = new HashSet<>(productionContext.getMonthDays());
        Set<Integer> productionDaySet = hasAddSkuList.stream().map(GroupPlanCxLhCapacityLimitHelper::getDay).collect(Collectors.toSet());
        for (Integer effectiveDay = lhGroupStartDay; effectiveDay <= lhGroupEndDay; effectiveDay++) {
            if (productionDaySet.contains(effectiveDay)) {
                effectiveDaySet.add(effectiveDay);
            }
        }
        if (CollectionUtils.isEmpty(effectiveDaySet)) {
            return new MouldProductionDayLimitHelper(Collections.emptySet(), MouldProductionLimitTypeEnum.MOULD_LOCAL_LIMIT);
        }
        //取得与模具排产范围的交集
        Set<Integer> effectiveMouldSet = getEffectiveDay(effectiveDaySet, selectedMould);
        if (CollectionUtils.isEmpty(effectiveMouldSet)) {
            return new MouldProductionDayLimitHelper(Collections.emptySet(), MouldProductionLimitTypeEnum.EMBRYO_COUNT_AND_MOULD_LOCAL_LIMIT);
        }
        //20260116 取得与模壳排产范围的交集
        Set<Integer> mouldShellSet = productionContext.getMouldShellRange(selectedMould.get(BigDecimal.ZERO.intValue()));
        if (CollectionUtils.isEmpty(mouldShellSet)) {
            return new MouldProductionDayLimitHelper(Collections.emptySet(), MouldProductionLimitTypeEnum.MOULD_SHELL_LIMIT);
        }
        Set<Integer> intersectionSet = effectiveMouldSet.stream().filter(mouldShellSet::contains).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(intersectionSet)) {
            return new MouldProductionDayLimitHelper(Collections.emptySet(), MouldProductionLimitTypeEnum.MOULD_SHELL_DOUBLE_LIMIT);
        }
        //20260617 取得与模具分配比例排产范围的交集
        Set<Integer> mouldAllocationSet = productionContext.getMouldAllocationRange(addSkuInfo);
        if (CollectionUtils.isEmpty(mouldAllocationSet)) {
            return new MouldProductionDayLimitHelper(Collections.emptySet(), MouldProductionLimitTypeEnum.MOULD_ALLOCATION_LIMIT);
        }
        intersectionSet = intersectionSet.stream().filter(mouldAllocationSet::contains).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(intersectionSet)) {
            return new MouldProductionDayLimitHelper(Collections.emptySet(), MouldProductionLimitTypeEnum.MOULD_ALLOCATION_DOUBLE_LIMIT);
        }
        //20260119 取得与胶囊卡盘总数排产范围的交集
        Set<Integer> capsuleChuckSet = productionContext.getCapsuleChuckRange(addSkuInfo);
        if (CollectionUtils.isEmpty(capsuleChuckSet)) {
            return new MouldProductionDayLimitHelper(Collections.emptySet(), MouldProductionLimitTypeEnum.CAPSULE_CHUCK_LIMIT);
        }
        intersectionSet = intersectionSet.stream().filter(capsuleChuckSet::contains).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(intersectionSet)) {
            return new MouldProductionDayLimitHelper(Collections.emptySet(), MouldProductionLimitTypeEnum.CAPSULE_CHUCK_DOUBLE_LIMIT);
        }
        //取得最早的一段连续时间
        Set<Integer> earliestContinuousSet = ContinuousProductionDayHandler.getEarliestContinuousRange(intersectionSet, stopDaySet);
        //剔除停机日
        Set<Integer> result = new HashSet<>();
        earliestContinuousSet.forEach(productionDay -> {
            if (stopDaySet.contains(productionDay)) {
                return;
            }
            result.add(productionDay);
        });
        return new MouldProductionDayLimitHelper(result, MouldProductionLimitTypeEnum.NO_LIMIT);
    }

    /**
     * 获取取得的有效排产日与模具可排产日的交集
     *
     * @param limitProductionDaySet 符合条件的排产日范围(硫化组、胎胚种类数)
     * @param selectedMould         选中的模具
     * @return
     */
    private static Set<Integer> getEffectiveDay(Set<Integer> limitProductionDaySet, List<ProductionMouldInfoVo> selectedMould) {
        Set<Integer> firstProductionDaySet = selectedMould.get(BigDecimal.ZERO.intValue()).getProductionDaySet();
        Set<Integer> secondProductionDaySet = selectedMould.get(BigDecimal.ONE.intValue()).getProductionDaySet();
        if (CollectionUtils.isEmpty(firstProductionDaySet) || CollectionUtils.isEmpty(secondProductionDaySet)) {
            return Collections.emptySet();
        }
        //取两个模具的排产日交集
        Set<Integer> intersectionSet = firstProductionDaySet.stream().filter(secondProductionDaySet::contains).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(intersectionSet)) {
            return Collections.emptySet();
        }
        return limitProductionDaySet.stream().filter(intersectionSet::contains).collect(Collectors.toSet());
    }

}
