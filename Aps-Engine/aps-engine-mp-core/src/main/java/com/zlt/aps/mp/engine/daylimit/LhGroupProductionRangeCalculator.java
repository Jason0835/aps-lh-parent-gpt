package com.zlt.aps.mp.engine.daylimit;

import com.zlt.aps.constant.StringConstant;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.domain.vo.ProductionMouldInfoVo;
import com.zlt.aps.mp.engine.handler.ContinuousProductionDayHandler;
import com.zlt.aps.mp.engine.logrecorder.DayLimitLogRecorder;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
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
     * 5、再依据胶囊卡盘总数限制的排产日集合，取得交集
     * 6、再依据日产能上限的排产日集合，取得交集
     * 7、再加入换模能力集合，取得交集
     *
     * @param productionContext 排产上下文
     * @param addSkuInfo        排产计划信息
     * @param lhGroupStartDay   预选硫化组开始排产日(通常为前一Sku的收尾日)
     * @param lhGroupEndDay     预选硫化组结束排产日(通常为结构收尾日)
     * @param selectedMould     选中的模具
     * @param dayLimitList      同结构下的日排产限制集合信息
     * @param stopDaySet        停工日集合(忽略机台时，传整体停工日，但机台时为机台停工日)
     * @param isChangeMould     是否需要换模 需要换模时要处理换模能力
     * @return
     */
    public static MouldProductionDayLimitHelper confirmProductionRange(TbrProductionContext productionContext, MonthPlanProductionRequirePlanVo addSkuInfo, Integer lhGroupStartDay, Integer lhGroupEndDay, List<ProductionMouldInfoVo> selectedMould, List<GroupPlanCxLhCapacityLimitHelper> dayLimitList, Set<Integer> stopDaySet, boolean isChangeMould) {
        String productionEmbryoCode = addSkuInfo.getEmbryoCode();
        String materialDesc = addSkuInfo.getMaterialDesc();
        //在满足硫化配比限制下，取得排产计划可排的满足胎胚种类数限制的排产范围
        List<GroupPlanCxLhCapacityLimitHelper> hasAddSkuList = dayLimitList.stream().filter(dayLimit -> !dayLimit.isReachLimitByEmbryoCode(productionEmbryoCode)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasAddSkuList)) {
            productionContext.addSkuProductionLimitInfo(materialDesc, MouldProductionLimitTypeEnum.EMBRYO_CODE_COUNT_LIMIT);
            //说明达到胎胚种类数限制
            return new MouldProductionDayLimitHelper(Collections.emptySet(), MouldProductionLimitTypeEnum.EMBRYO_CODE_COUNT_LIMIT);
        }
        //1、取得与胎胚种类数范围的交集
        Set<Integer> effectiveDaySet = new HashSet<>(productionContext.getMonthDays());
        Set<Integer> productionDaySet = hasAddSkuList.stream().map(GroupPlanCxLhCapacityLimitHelper::getDay).collect(Collectors.toSet());
        for (Integer effectiveDay = lhGroupStartDay; effectiveDay <= lhGroupEndDay; effectiveDay++) {
            if (productionDaySet.contains(effectiveDay)) {
                effectiveDaySet.add(effectiveDay);
            }
        }
        if (CollectionUtils.isEmpty(effectiveDaySet)) {
            productionContext.addSkuProductionLimitInfo(materialDesc, MouldProductionLimitTypeEnum.MOULD_LOCAL_LIMIT);
            return new MouldProductionDayLimitHelper(Collections.emptySet(), MouldProductionLimitTypeEnum.MOULD_LOCAL_LIMIT);
        }
        //2、取得与模具排产范围的交集
        Set<Integer> effectiveMouldSet = getEffectiveDay(effectiveDaySet, selectedMould);
        if (CollectionUtils.isEmpty(effectiveMouldSet)) {
            productionContext.addSkuProductionLimitInfo(materialDesc, MouldProductionLimitTypeEnum.EMBRYO_COUNT_AND_MOULD_LOCAL_LIMIT);
            return new MouldProductionDayLimitHelper(Collections.emptySet(), MouldProductionLimitTypeEnum.EMBRYO_COUNT_AND_MOULD_LOCAL_LIMIT);
        }
        //3、20260116 取得与模壳排产范围的交集
        Set<Integer> mouldShellSet = productionContext.getMouldShellRange(selectedMould.get(BigDecimal.ZERO.intValue()));
        if (CollectionUtils.isEmpty(mouldShellSet)) {
            productionContext.addSkuProductionLimitInfo(materialDesc, MouldProductionLimitTypeEnum.MOULD_SHELL_LIMIT);
            return new MouldProductionDayLimitHelper(Collections.emptySet(), MouldProductionLimitTypeEnum.MOULD_SHELL_LIMIT);
        }
        Set<Integer> intersectionSet = effectiveMouldSet.stream().filter(mouldShellSet::contains).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(intersectionSet)) {
            productionContext.addSkuProductionLimitInfo(materialDesc, MouldProductionLimitTypeEnum.MOULD_SHELL_DOUBLE_LIMIT);
            return new MouldProductionDayLimitHelper(Collections.emptySet(), MouldProductionLimitTypeEnum.MOULD_SHELL_DOUBLE_LIMIT);
        }
        //4、20260617 取得与模具分配比例排产范围的交集
        Set<Integer> mouldAllocationSet = productionContext.getMouldAllocationRange(addSkuInfo);
        if (CollectionUtils.isEmpty(mouldAllocationSet)) {
            productionContext.addSkuProductionLimitInfo(materialDesc, MouldProductionLimitTypeEnum.MOULD_ALLOCATION_LIMIT);
            return new MouldProductionDayLimitHelper(Collections.emptySet(), MouldProductionLimitTypeEnum.MOULD_ALLOCATION_LIMIT);
        }
        intersectionSet = intersectionSet.stream().filter(mouldAllocationSet::contains).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(intersectionSet)) {
            productionContext.addSkuProductionLimitInfo(materialDesc, MouldProductionLimitTypeEnum.MOULD_ALLOCATION_DOUBLE_LIMIT);
            return new MouldProductionDayLimitHelper(Collections.emptySet(), MouldProductionLimitTypeEnum.MOULD_ALLOCATION_DOUBLE_LIMIT);
        }
        //5、20260119 取得与胶囊卡盘总数排产范围的交集
        Set<Integer> capsuleChuckSet = productionContext.getCapsuleChuckRange(addSkuInfo);
        if (CollectionUtils.isEmpty(capsuleChuckSet)) {
            productionContext.addSkuProductionLimitInfo(materialDesc, MouldProductionLimitTypeEnum.CAPSULE_CHUCK_LIMIT);
            return new MouldProductionDayLimitHelper(Collections.emptySet(), MouldProductionLimitTypeEnum.CAPSULE_CHUCK_LIMIT);
        }
        intersectionSet = intersectionSet.stream().filter(capsuleChuckSet::contains).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(intersectionSet)) {
            productionContext.addSkuProductionLimitInfo(materialDesc, MouldProductionLimitTypeEnum.CAPSULE_CHUCK_DOUBLE_LIMIT);
            return new MouldProductionDayLimitHelper(Collections.emptySet(), MouldProductionLimitTypeEnum.CAPSULE_CHUCK_DOUBLE_LIMIT);
        }
        //6、20260126 取得与每日排产上限的排产范围的交集
        Set<Integer> dayCapacityLimitSet = productionContext.getDayCapacityLimitRange();
        if (CollectionUtils.isEmpty(dayCapacityLimitSet)) {
            productionContext.addSkuProductionLimitInfo(materialDesc, MouldProductionLimitTypeEnum.DAY_CAPACITY_LIMIT);
            return new MouldProductionDayLimitHelper(Collections.emptySet(), MouldProductionLimitTypeEnum.DAY_CAPACITY_LIMIT);
        }
        intersectionSet = intersectionSet.stream().filter(dayCapacityLimitSet::contains).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(intersectionSet)) {
            productionContext.addSkuProductionLimitInfo(materialDesc, MouldProductionLimitTypeEnum.DAY_CAPACITY_DOUBLE_LIMIT);
            return new MouldProductionDayLimitHelper(Collections.emptySet(), MouldProductionLimitTypeEnum.DAY_CAPACITY_DOUBLE_LIMIT);
        }
        Integer continueDays = 2;
//        return handlerChangeMouldContinueControl(productionContext, intersectionSet, stopDaySet, materialDesc, isChangeMould, continueDays);
        return handlerChangeMouldNoContinue(productionContext, intersectionSet, stopDaySet, materialDesc, isChangeMould);
    }

    /**
     * 换模处理，需要连续排两天
     *
     * @param productionContext 排产上下文
     * @param intersectionSet   可排产天数集合
     * @param stopDaySet        停产天数集合
     * @param materialDesc      排产Sku
     * @param isChangeMould     是否换模
     * @param continueDays      连续排产天数控制
     * @return
     */
    private static MouldProductionDayLimitHelper handlerChangeMouldContinueControl(TbrProductionContext productionContext, Set<Integer> intersectionSet, Set<Integer> stopDaySet, String materialDesc, boolean isChangeMould, Integer continueDays) {
        //取得最早的一段连续时间
        Set<Integer> earliestContinuousSet = ContinuousProductionDayHandler.getEarliestContinuousRange(productionContext, continueDays, intersectionSet, stopDaySet);
        //7、20260313 换模能力
        MouldProductionDayLimitHelper handlerMould = handlerChangeMouldCapacity(isChangeMould, productionContext, continueDays, materialDesc, earliestContinuousSet, stopDaySet);
        if (MouldProductionLimitTypeEnum.NO_LIMIT != handlerMould.getLimitType()) {
            return handlerMould;
        }
        intersectionSet = handlerMould.getProductionDaySet();
        //剔除停机日
        Set<Integer> result = new HashSet<>();
        intersectionSet.forEach(productionDay -> {
            if (stopDaySet.contains(productionDay)) {
                return;
            }
            result.add(productionDay);
        });
        return new MouldProductionDayLimitHelper(result, MouldProductionLimitTypeEnum.NO_LIMIT);
    }

    /**
     * 换模处理，需要连续排两天
     *
     * @param productionContext 排产上下文
     * @param intersectionSet   可排产天数集合
     * @param stopDaySet        停产天数集合
     * @param materialDesc      排产Sku
     * @param isChangeMould     是否换模
     * @return
     */
    private static MouldProductionDayLimitHelper handlerChangeMouldNoContinue(TbrProductionContext productionContext, Set<Integer> intersectionSet, Set<Integer> stopDaySet, String materialDesc, boolean isChangeMould) {
        //7、20260313 换模能力
        MouldProductionDayLimitHelper handlerMould = handlerChangeMouldCapacity(isChangeMould, productionContext, materialDesc, intersectionSet);
        if (MouldProductionLimitTypeEnum.NO_LIMIT != handlerMould.getLimitType()) {
            return handlerMould;
        }
        intersectionSet = handlerMould.getProductionDaySet();
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

    /**
     * 换模能力的处理
     *
     * @param isChangeMould     是否换模
     * @param productionContext 排产上下文
     * @param materialDesc      物料描述
     * @param intersectionSet   已经有的交集
     * @return
     */
    private static MouldProductionDayLimitHelper handlerChangeMouldCapacity(boolean isChangeMould, TbrProductionContext productionContext, String materialDesc, Set<Integer> intersectionSet) {
        if (!isChangeMould) {
            return new MouldProductionDayLimitHelper(intersectionSet, MouldProductionLimitTypeEnum.NO_LIMIT);
        }
        //20260313 换模能力
        DayCapacityLimitVo dayCapacityLimit = productionContext.getBaseDataContainer().getDayCapacityLimit();
        Set<Integer> hasChangeMouldDaySet = dayCapacityLimit.getHasChangeMouldProductionDay(productionContext);
        if (CollectionUtils.isEmpty(hasChangeMouldDaySet)) {
            productionContext.addSkuProductionLimitInfo(materialDesc, MouldProductionLimitTypeEnum.CHANGE_MOULD_LIMIT);
            return new MouldProductionDayLimitHelper(Collections.emptySet(), MouldProductionLimitTypeEnum.CHANGE_MOULD_LIMIT);
        }
        //换模能力天数
        String dayInfo = hasChangeMouldDaySet.stream().map(String::valueOf).collect(Collectors.joining(StringConstant.COMMA));
        DayLimitLogRecorder.addEnableChangeMouldDayLog(productionContext, dayInfo);
        //取得最早有换模能力的天数
        List<Integer> canProductionDayList = new ArrayList<>(intersectionSet);
        canProductionDayList.sort(Comparator.comparing(Integer::intValue));
        Integer earliestDay = null;
        for (Integer canProductionDay : canProductionDayList) {
            if (hasChangeMouldDaySet.contains(canProductionDay)) {
                earliestDay = canProductionDay;
                break;
            }
        }
        //没有换模能力
        if (null == earliestDay) {
            productionContext.addSkuProductionLimitInfo(materialDesc, MouldProductionLimitTypeEnum.CHANGE_MOULD_CAPACITY_DOUBLE_LIMIT);
            return new MouldProductionDayLimitHelper(Collections.emptySet(), MouldProductionLimitTypeEnum.CHANGE_MOULD_CAPACITY_DOUBLE_LIMIT);
        }
        Integer selectedDay = earliestDay;
        intersectionSet = canProductionDayList.stream().filter(canProductionDay -> canProductionDay >= selectedDay).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(intersectionSet)) {
            productionContext.addSkuProductionLimitInfo(materialDesc, MouldProductionLimitTypeEnum.CHANGE_MOULD_CAPACITY_DOUBLE_LIMIT);
            return new MouldProductionDayLimitHelper(Collections.emptySet(), MouldProductionLimitTypeEnum.CHANGE_MOULD_CAPACITY_DOUBLE_LIMIT);
        }
        return new MouldProductionDayLimitHelper(intersectionSet, MouldProductionLimitTypeEnum.NO_LIMIT);
    }

    /**
     * 换模能力的处理
     *
     * @param isChangeMould     是否换模
     * @param productionContext 排产上下文
     * @param continueDays      可连续排产天数
     * @param materialDesc      物料描述
     * @param intersectionSet   已经有的交集
     * @param stopDays          停产天数集合
     * @return
     */
    private static MouldProductionDayLimitHelper handlerChangeMouldCapacity(boolean isChangeMould, TbrProductionContext productionContext, Integer continueDays, String materialDesc, Set<Integer> intersectionSet, Set<Integer> stopDays) {
        if (!isChangeMould) {
            return new MouldProductionDayLimitHelper(intersectionSet, MouldProductionLimitTypeEnum.NO_LIMIT);
        }
        //20260313 换模能力
        DayCapacityLimitVo dayCapacityLimit = productionContext.getBaseDataContainer().getDayCapacityLimit();
        Set<Integer> hasChangeMouldDaySet = dayCapacityLimit.getHasChangeMouldProductionDay(productionContext);
        if (CollectionUtils.isEmpty(hasChangeMouldDaySet)) {
            productionContext.addSkuProductionLimitInfo(materialDesc, MouldProductionLimitTypeEnum.CHANGE_MOULD_LIMIT);
            return new MouldProductionDayLimitHelper(Collections.emptySet(), MouldProductionLimitTypeEnum.CHANGE_MOULD_LIMIT);
        }
        //换模能力天数
        String dayInfo = hasChangeMouldDaySet.stream().map(String::valueOf).collect(Collectors.joining(StringConstant.COMMA));
        DayLimitLogRecorder.addEnableChangeMouldDayLog(productionContext, dayInfo);
        Set<Integer> realProductionDay = ContinuousProductionDayHandler.extractRetainDay(intersectionSet, stopDays);
        //取得最早有换模能力的天数
        List<Integer> canProductionDayList = new ArrayList<>(realProductionDay);
        canProductionDayList.sort(Comparator.comparing(Integer::intValue));
        Integer earliestDay = null;
        for (Integer canProductionDay : canProductionDayList) {
            if (hasChangeMouldDaySet.contains(canProductionDay)) {
                earliestDay = canProductionDay;
                break;
            }
        }
        //没有换模能力
        if (null == earliestDay) {
            productionContext.addSkuProductionLimitInfo(materialDesc, MouldProductionLimitTypeEnum.CHANGE_MOULD_CAPACITY_DOUBLE_LIMIT);
            return new MouldProductionDayLimitHelper(Collections.emptySet(), MouldProductionLimitTypeEnum.CHANGE_MOULD_CAPACITY_DOUBLE_LIMIT);
        }
        Integer selectedDay = earliestDay;
        Set<Integer> newProductionDaySet = canProductionDayList.stream().filter(canProductionDay -> canProductionDay >= selectedDay).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(newProductionDaySet)) {
            productionContext.addSkuProductionLimitInfo(materialDesc, MouldProductionLimitTypeEnum.CHANGE_MOULD_CAPACITY_DOUBLE_LIMIT);
            return new MouldProductionDayLimitHelper(Collections.emptySet(), MouldProductionLimitTypeEnum.CHANGE_MOULD_CAPACITY_DOUBLE_LIMIT);
        }
        Set<Integer> realDaySet = ContinuousProductionDayHandler.getGreaterDayRange(productionContext, newProductionDaySet, continueDays);
        if (CollectionUtils.isEmpty(realDaySet)) {
            productionContext.addSkuProductionLimitInfo(materialDesc, MouldProductionLimitTypeEnum.CHANGE_MOULD_CAPACITY_DOUBLE_LIMIT);
            return new MouldProductionDayLimitHelper(Collections.emptySet(), MouldProductionLimitTypeEnum.CHANGE_MOULD_CAPACITY_DOUBLE_LIMIT);
        }
        return new MouldProductionDayLimitHelper(realDaySet, MouldProductionLimitTypeEnum.NO_LIMIT);
    }

}
