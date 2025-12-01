package com.zlt.aps.factory.utils;

import com.tlt.aps.enums.FormingMethodTypeEnum;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.vo.MonthPlanManufacturingRequirementVo;
import com.zlt.aps.factory.enums.CapacityControlTypeEnum;
import com.zlt.aps.factory.scheduling.ProductionContext;
import com.zlt.aps.maindata.domain.vo.DaySizeCapacityVo;
import com.zlt.aps.maindata.utils.SizeCapacityUtils;
import com.zlt.aps.monthplan.api.domain.entity.SizeCapacityConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 产能控制工具类型
 *
 * @author ZLT
 * @date 20250605
 */
@Slf4j
public class CapacityControlUtils {

    /**
     * 根据寸口产能分配配置，构建产能控制信息
     * 包含寸口+成型法的月产能总量
     * 及细化到天的寸口+成型法的日产能量
     *
     * @param productionContext    排产上下文
     * @param daySizeCapacity      当前寸口+成型法的配置信息
     * @param startDay             起始天数
     * @param sizeMonthCapacityMap 寸口|*|工装类型|*|成型法|*|胎体布层级的月产能总量集合，需要将配置转换后加入到集合中
     * @param daySizeCapacityMap   寸口|*|工装类型|*|成型法|*|胎体布层级细化到天的产能控制集合，需要将配置转换后加入到集合中
     * @param monthWorkDays        月可工作的天数，月产能使用
     * @param monthDays            月份最大天数-日产能计算使用
     * @param dayMaxMouldQtyMap    寸口|*|工装类型|*|成型法|*|胎体布层级细化到天的模具数控制集合，需要将配置转换后加入到集合中
     */
    public static void buildSizeCapacityControlInfo(ProductionContext productionContext, DaySizeCapacityVo daySizeCapacity, Integer startDay, Map<String, Long> sizeMonthCapacityMap, Map<Integer, Map<String, Long>> daySizeCapacityMap, Integer monthWorkDays, Integer monthDays, Map<Integer, Map<String, Integer>> dayMaxMouldQtyMap) {
        SizeCapacityConfiguration sizeCapacityConfiguration = daySizeCapacity.getData();
        //设置月总产值
        settingMonthCapacityInfo(sizeCapacityConfiguration, sizeMonthCapacityMap, monthWorkDays);
        //日产能细化控制
        buildCapacityControlInfo(productionContext, sizeCapacityConfiguration, startDay, sizeMonthCapacityMap, daySizeCapacityMap, monthWorkDays, monthDays, dayMaxMouldQtyMap);
        DaySizeCapacityVo nextSizeCapacity = daySizeCapacity.getNextSize();
        //下一个寸口
        if (null != nextSizeCapacity) {
            Integer newStartDay = startDay + sizeCapacityConfiguration.getRemainingDays();
            buildSizeCapacityControlInfo(productionContext, nextSizeCapacity, newStartDay, sizeMonthCapacityMap, daySizeCapacityMap, monthWorkDays, monthDays, dayMaxMouldQtyMap);
        }
    }

    /**
     * 对某个 轮胎类型+寸口 进行产能控制
     *
     * @param tireCapacityEntry 轮胎类型+寸口的限制配置
     * @param tireRequireMap    轮胎类型+寸口的分组需求计划
     * @param productionContext 排产上下文
     */
    public static void tireCapacityLimit(Map.Entry<String, Long> tireCapacityEntry, Map<String, List<MonthPlanManufacturingRequirementVo>> tireRequireMap, ProductionContext productionContext) {
        List<MonthPlanManufacturingRequirementVo> tireCapacityRequireList = tireRequireMap.get(tireCapacityEntry.getKey());
        //没有需求
        if (CollectionUtils.isEmpty(tireCapacityRequireList)) {
            return;
        }
        Long sumProductionQty = tireCapacityRequireList.stream().mapToLong(MonthPlanManufacturingRequirementVo::getProductionQty).sum();
        Long sumLimitQty = tireCapacityEntry.getValue();
        //需求量小于限制值
        if (sumLimitQty >= sumProductionQty) {
            return;
        }
        //需要剔除的需求量
        Long needRemoveQty = sumProductionQty - sumLimitQty;
        capacityLimit(CapacityControlTypeEnum.TIRE_CAPACITY_CONTROL, needRemoveQty, tireCapacityRequireList, productionContext);
    }

    /**
     * 一次法产能控制处理
     *
     * @param productionContext      排产上下文
     * @param onlyOneMouldMethodList 仅能一次法成型的计划集合
     * @param sizeMonthCapacityMap   寸口产能配置情况
     * @param occupiedMap            当前需求量
     * @param needChangeSet          需要切换成二次法的信息
     */
    public static void onlyOneMethodCapacityControl(ProductionContext productionContext, List<MonthPlanManufacturingRequirementVo> onlyOneMouldMethodList, Map<String, Long> sizeMonthCapacityMap, Map<String, Long> occupiedMap, Set<String> needChangeSet) {
        if (CollectionUtils.isEmpty(onlyOneMouldMethodList)) {
            return;
        }
        //20250714 ZLT 因一次法都是多层级胎体布产能，故而忽略胎体布层级匹配
        Map<String, List<MonthPlanManufacturingRequirementVo>> sizeCapacityMap = onlyOneMouldMethodList.stream().collect(Collectors.groupingBy(MonthPlanManufacturingRequirementVo::getSizeCapacityGroupKeyNoTireFabric));
        sizeCapacityMap.entrySet().stream().forEach(sizeCapacityEntry -> {
            //寸口|*|工装类别|*|成型法
            String sizeGroupKey = sizeCapacityEntry.getKey();
            //寸口|*|工装类别|*|成型法|*|胎体布层级--一次法胎体布产能统一为多层
            String sizeCapacityGroupKey = String.format("%s|*|%s", sizeGroupKey, ProductionConstant.MULTILAYER_TIRE_FABRIC);
            List<MonthPlanManufacturingRequirementVo> sizePlanList = sizeCapacityEntry.getValue();
            Long limitQty = sizeMonthCapacityMap.get(sizeCapacityGroupKey);
            if (null == limitQty) {
                limitQty = BigDecimal.ZERO.longValue();
            }
            Long productionQty = sizePlanList.stream().mapToLong(MonthPlanManufacturingRequirementVo::getProductionQty).sum();
            if (productionQty > limitQty) {
                needChangeSet.add(sizeGroupKey);
            } else {
                occupiedMap.put(sizeGroupKey, productionQty);
            }
            sizeCapacityLimit(sizeCapacityGroupKey, sizeMonthCapacityMap, sizePlanList, productionContext);
        });
    }

    /**
     * 一次法产能已满，需要将能转成形法的计划转成型
     *
     * @param hasChangeMouldMethodList 可转成型法的计划集合
     * @param needChangeSet            需要整体全部转换的寸口集合信息
     * @param occupiedMap              部分转换时，已占产能信息
     * @param sizeMonthCapacityMap     寸口月产能配置信息
     */
    public static void oneMethodChangeMethod(List<MonthPlanManufacturingRequirementVo> hasChangeMouldMethodList, Set<String> needChangeSet, Map<String, Long> occupiedMap, Map<String, Long> sizeMonthCapacityMap) {
        if (CollectionUtils.isEmpty(hasChangeMouldMethodList)) {
            return;
        }
        //获取当前为一次法的计划才处理,二次法则表示续作规格采用二次法，二次法的不处理
        List<MonthPlanManufacturingRequirementVo> oneMethodChangeMethodList = hasChangeMouldMethodList.stream().filter(canChangeMethod -> FormingMethodTypeEnum.SINGLE_STAGE_TIRE.getMethodValue().equals(canChangeMethod.getMouldMethod())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(oneMethodChangeMethodList)) {
            return;
        }
        Map<String, List<MonthPlanManufacturingRequirementVo>> sizeGroupMap = oneMethodChangeMethodList.stream().collect(Collectors.groupingBy(MonthPlanManufacturingRequirementVo::getSizeCapacityGroupKeyNoTireFabric));
        sizeGroupMap.entrySet().stream().forEach(sizeGroupEntry -> {
            String sizeGroupKey = sizeGroupEntry.getKey();
            //寸口|*|工装类别|*|成型法|*|胎体布层级--一次法胎体布产能统一为多层,故而获取产能控制也是使用多层胎体布
            String sizeCapacityGroupKey = String.format("%s|*|%s", sizeGroupKey, ProductionConstant.MULTILAYER_TIRE_FABRIC);
            List<MonthPlanManufacturingRequirementVo> changeMethodList = sizeGroupEntry.getValue();
            //整体转换
            if (needChangeSet.contains(sizeGroupKey)) {
                CapacityControlUtils.changeMethod(changeMethodList);
                return;
            }
            //部分转换
            Long needProductionQty = changeMethodList.stream().mapToLong(MonthPlanManufacturingRequirementVo::getProductionQty).sum();
            Long onlyNeedProductionQty = occupiedMap.get(sizeGroupKey);
            if (null == onlyNeedProductionQty) {
                onlyNeedProductionQty = BigDecimal.ZERO.longValue();
            }
            Long sumProductionQty = onlyNeedProductionQty + needProductionQty;
            Long limitQty = sizeMonthCapacityMap.get(sizeCapacityGroupKey);
            if (null == limitQty) {
                limitQty = BigDecimal.ZERO.longValue();
            }
            if (limitQty >= sumProductionQty) {
                return;
            }
            //进行转换成型法
            Long needChangeQty = sumProductionQty - limitQty;
            CapacityControlUtils.changeMethodBySizeCapacityLimit(needChangeQty, changeMethodList);
        });
    }

    /**
     * 对某个成型法分组，进行胎体布层级产能分配
     * 即多层级有剩余产能时，可将单层级转换到多层级，而多层级不可转换到单层级
     *
     * @param mouldMethodGroupList 某个成型法分组需求
     * @param productionContext    排产上下文
     */
    public static void capacityAllocationByTireFabric(List<MonthPlanManufacturingRequirementVo> mouldMethodGroupList, ProductionContext productionContext) {
        if (CollectionUtils.isEmpty(mouldMethodGroupList)) {
            return;
        }
        Map<String, Long> sizeMonthCapacityMap = productionContext.getSizeMonthCapacityMap();
        //多层级还有产能的寸口集合
        Map<String, Long> surplusMap = new HashMap<>();
        //多层级已经满产能的寸口集合
        Set<String> noChangeTireFabricSet = new HashSet<>();
        //多层胎体布
        List<MonthPlanManufacturingRequirementVo> multilayerTireFabricList = mouldMethodGroupList.stream().filter(oneMouldMethodRequirement -> ProductionConstant.MULTILAYER_TIRE_FABRIC.equals(oneMouldMethodRequirement.getTireFabricNumber())).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(multilayerTireFabricList)) {
            Map<String, List<MonthPlanManufacturingRequirementVo>> multilayerSizeCapacityMap = multilayerTireFabricList.stream().collect(Collectors.groupingBy(MonthPlanManufacturingRequirementVo::getSizeCapacityGroupKey));
            multilayerSizeCapacityMap.entrySet().stream().forEach(sizeTireFabricEntry -> {
                String sizeGroupKey = sizeTireFabricEntry.getKey();
                List<MonthPlanManufacturingRequirementVo> sizePlanList = sizeTireFabricEntry.getValue();
                Long limitQty = sizeMonthCapacityMap.get(sizeGroupKey);
                if (null == limitQty) {
                    limitQty = BigDecimal.ZERO.longValue();
                }
                Long productionQty = sizePlanList.stream().mapToLong(MonthPlanManufacturingRequirementVo::getProductionQty).sum();
                if (productionQty < limitQty) {
                    surplusMap.put(sizeGroupKey, limitQty - productionQty);
                    return;
                }
                noChangeTireFabricSet.add(sizeGroupKey);
                //20250714 ZLT 胎体布层级产能控制
                if (productionQty > limitQty) {
                    CapacityControlUtils.sizeCapacityLimit(sizeGroupKey, sizeMonthCapacityMap, sizePlanList, productionContext);
                }
            });
        }
        //单层胎体布
        List<MonthPlanManufacturingRequirementVo> singleTireFabricList = mouldMethodGroupList.stream().filter(oneMouldMethodRequirement -> !ProductionConstant.MULTILAYER_TIRE_FABRIC.equals(oneMouldMethodRequirement.getTireFabricNumber())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(singleTireFabricList)) {
            return;
        }
        Map<String, List<MonthPlanManufacturingRequirementVo>> singleTireFabricSizeCapacityMap = singleTireFabricList.stream().collect(Collectors.groupingBy(MonthPlanManufacturingRequirementVo::getSizeCapacityGroupKey));
        singleTireFabricSizeCapacityMap.entrySet().stream().forEach(singleTireFabricEntry -> {
            String sizeGroupKey = singleTireFabricEntry.getKey();
            List<MonthPlanManufacturingRequirementVo> sizePlanList = singleTireFabricEntry.getValue();
            Long limitQty = sizeMonthCapacityMap.get(sizeGroupKey);
            if (null == limitQty) {
                limitQty = BigDecimal.ZERO.longValue();
            }
            Long productionQty = sizePlanList.stream().mapToLong(MonthPlanManufacturingRequirementVo::getProductionQty).sum();
            if (productionQty <= limitQty) {
                return;
            }
            String changeTireFabricKey = sizePlanList.get(0).getMultilayerTireFabricMultilayerKey(ProductionConstant.MULTILAYER_TIRE_FABRIC);
            if (!surplusMap.containsKey(changeTireFabricKey)) {
                return;
            }
            //需转数量--即超出单层产能量
            Long exceedingPartQty = productionQty - limitQty;
            //可转数量--多层最多可转入量
            Long convertiblePartQty = surplusMap.get(changeTireFabricKey);
            changeTireFabricAndLimitCapacity(productionContext, sizePlanList, convertiblePartQty, exceedingPartQty);
        });
    }

    /**
     * 对某个 寸口+成形法 进行产能控制
     *
     * @param groupKey                寸口|*|成型法|*|胎体布层级分组
     * @param sizeMonthCapacityMap    产能限制配置
     * @param sizeCapacityRequireList 需求计划集合
     * @param productionContext       排产上下文
     */
    public static void sizeCapacityLimit(String groupKey, Map<String, Long> sizeMonthCapacityMap, List<MonthPlanManufacturingRequirementVo> sizeCapacityRequireList, ProductionContext productionContext) {
        //没有需求
        if (CollectionUtils.isEmpty(sizeCapacityRequireList)) {
            return;
        }
        Long sumProductionQty = sizeCapacityRequireList.stream().mapToLong(MonthPlanManufacturingRequirementVo::getProductionQty).sum();
        Long sumLimitQty = sizeMonthCapacityMap.get(groupKey);
        if (null == sumLimitQty) {
            sumLimitQty = BigDecimal.ZERO.longValue();
        }
        //需求量小于限制值
        if (sumLimitQty >= sumProductionQty) {
            return;
        }
        Long needRemoveQty = sumProductionQty - sumLimitQty;
        capacityLimit(CapacityControlTypeEnum.SIZE_CAPACITY_CONTROL, needRemoveQty, sizeCapacityRequireList, productionContext);
    }

    /**
     * 根据某个寸口+成型法的产能，进行成型法转换
     *
     * @param needChangeQty    最大需要转换的量
     * @param changeMethodList 可换成型法的计划
     */
    public static void changeMethodBySizeCapacityLimit(Long needChangeQty, List<MonthPlanManufacturingRequirementVo> changeMethodList) {
        //没有需求
        if (CollectionUtils.isEmpty(changeMethodList)) {
            return;
        }
        if (needChangeQty <= BigDecimal.ZERO.longValue()) {
            return;
        }
        //按排产顺序倒序排产，优先级低的先剔除
        changeMethodList.sort(Comparator.comparing(MonthPlanManufacturingRequirementVo::getProductionSequence, Comparator.reverseOrder()));
        //按SAP代码分组
        Map<String, List<MonthPlanManufacturingRequirementVo>> productCodeGroupMap = changeMethodList.stream().collect(Collectors.groupingBy(MonthPlanManufacturingRequirementVo::getProductCode));
        for (MonthPlanManufacturingRequirementVo needChangePlan : changeMethodList) {
            if (needChangeQty <= BigDecimal.ZERO.longValue()) {
                break;
            }
            Long productionQty = needChangePlan.getProductionQty();
            if (productionQty <= BigDecimal.ZERO.longValue()) {
                continue;
            }
            String productCode = needChangePlan.getProductCode();
            String mouldMethod = needChangePlan.getMouldMethod();
            //转换成型法
            FormingMethodTypeEnum changeMethod = FormingMethodTypeEnum.getChangeType(mouldMethod);
            //整个规格转换
            List<MonthPlanManufacturingRequirementVo> productGroupList = productCodeGroupMap.get(productCode);
            Long sumProductQty = productGroupList.stream().mapToLong(MonthPlanManufacturingRequirementVo::getProductionQty).sum();
            productGroupList.stream().forEach(sameProductPlan -> sameProductPlan.changeSpecCode(changeMethod));
            needChangeQty = needChangeQty - sumProductQty;
        }
    }

    /**
     * 需要由一次法转二次法的计划
     *
     * @param needChangeList 需要更换成型法的计划集合
     */
    public static void changeMethod(List<MonthPlanManufacturingRequirementVo> needChangeList) {
        if (CollectionUtils.isEmpty(needChangeList)) {
            return;
        }
        needChangeList.stream().forEach(needChangePlan -> {
            String mouldMethod = needChangePlan.getMouldMethod();
            //转换成型法
            FormingMethodTypeEnum changeMethod = FormingMethodTypeEnum.getChangeType(mouldMethod);
            needChangePlan.changeSpecCode(changeMethod);
        });
    }

    /**
     * 根据配置，设置月产能
     *
     * @param sizeCapacityConfiguration 寸口产能配置
     * @param sizeMonthCapacityMap      分组 月总产能
     * @param monthWorkDays             月工作天数
     */
    private static void settingMonthCapacityInfo(SizeCapacityConfiguration sizeCapacityConfiguration, Map<String, Long> sizeMonthCapacityMap, Integer monthWorkDays) {
        //寸口|*|工装类型|*|成型法|*|胎体布层级
        String sizeCapacityKey = sizeCapacityConfiguration.getGroupKey();
        Integer wholeMonthNumber = sizeCapacityConfiguration.getWholeMachineNumber();
        if (null == wholeMonthNumber) {
            wholeMonthNumber = BigDecimal.ZERO.intValue();
        }
        Integer remainingDays = sizeCapacityConfiguration.getRemainingDays();
        if (null == remainingDays) {
            remainingDays = BigDecimal.ZERO.intValue();
        }
        BigDecimal days = BigDecimal.valueOf(wholeMonthNumber).multiply(BigDecimal.valueOf(monthWorkDays)).add(BigDecimal.valueOf(remainingDays));
        Integer dayCapacity = sizeCapacityConfiguration.getDayCapacity();
        //月总产能
        BigDecimal monthCapacity = days.multiply(BigDecimal.valueOf(dayCapacity));
        Long realMonthCapacity = monthCapacity.setScale(BigDecimal.ZERO.intValue(), RoundingMode.UP).longValue();
        //20250714 ZLT 因配置到成型产能类型和天产能，故而会有不同产能值
        Long assignedDayCapacityQty = sizeMonthCapacityMap.get(sizeCapacityKey);
        if (null == assignedDayCapacityQty) {
            assignedDayCapacityQty = BigDecimal.ZERO.longValue();
        }
        sizeMonthCapacityMap.put(sizeCapacityKey, assignedDayCapacityQty + realMonthCapacity);
    }

    /**
     * 根据寸口产能分配配置，构建产能控制信息
     * 包含分组(寸口+工装类别+成型法+胎体布层级)的月产能总量
     * 及分组细化到天(寸口+工装类别+成型法+胎体布层级)的日产能量
     *
     * @param productionContext         排产上下文
     * @param sizeCapacityConfiguration 当前寸口+成型法的配置信息
     * @param startDay                  起始小数部分，用以计算开始天数
     * @param sizeMonthCapacityMap      寸口+成型法的月产能总量集合，需要将配置转换后加入到集合中
     * @param daySizeCapacityMap        寸口+成型法细化到天的产能控制集合，需要将配置转换后加入到集合中
     * @param monthWorkDays             月可工作的天数，月产能使用
     * @param monthDays                 月份最大天数-日产能计算使用
     */
    private static void buildCapacityControlInfo(ProductionContext productionContext, SizeCapacityConfiguration sizeCapacityConfiguration, Integer startDay, Map<String, Long> sizeMonthCapacityMap, Map<Integer, Map<String, Long>> daySizeCapacityMap, Integer monthWorkDays, Integer monthDays, Map<Integer, Map<String, Integer>> dayMaxMouldQtyMap) {
        //寸口|*|工装类别|*|成型法|*|胎体布层级
        String sizeCapacityKey = sizeCapacityConfiguration.getGroupKey();
        Integer remainingDays = sizeCapacityConfiguration.getRemainingDays();
        if (null == remainingDays) {
            remainingDays = BigDecimal.ZERO.intValue();
        }
        Integer wholeMonthNumber = sizeCapacityConfiguration.getWholeMachineNumber();
        //能整月排产
        if (remainingDays == BigDecimal.ZERO.intValue() && wholeMonthNumber != BigDecimal.ZERO.intValue()) {
            SizeCapacityUtils.buildWholeMonth(productionContext.getFactoryStopDays(), sizeCapacityKey, monthDays, daySizeCapacityMap, sizeCapacityConfiguration, dayMaxMouldQtyMap);
            return;
        }
        //部分整月，部分中途换寸口
        if (wholeMonthNumber > BigDecimal.ZERO.intValue() && remainingDays > BigDecimal.ZERO.intValue()) {
            SizeCapacityUtils.buildPartWholeMonthPartDaysMonth(productionContext.getFactoryStopDays(), sizeCapacityKey, monthDays, daySizeCapacityMap, sizeCapacityConfiguration, startDay, dayMaxMouldQtyMap);
            return;
        }
        //全部部分天排产
        SizeCapacityUtils.buildPartDaysMonth(productionContext.getFactoryStopDays(), sizeCapacityKey, monthDays, daySizeCapacityMap, sizeCapacityConfiguration, startDay, dayMaxMouldQtyMap);
    }

    /**
     * 对单层胎体布需求转到多层
     *
     * @param productionContext    排产上下文
     * @param singleTireFabricList 单层胎体布需求计划集合
     * @param convertiblePartQty   可转入的量--即多层最多可转入的量
     * @param exceedingPartQty     需要转入的量--即超出单层产能量
     */
    private static void changeTireFabricAndLimitCapacity(ProductionContext productionContext, List<MonthPlanManufacturingRequirementVo> singleTireFabricList, Long convertiblePartQty, Long exceedingPartQty) {
        if (CollectionUtils.isEmpty(singleTireFabricList)) {
            return;
        }
        if (null == convertiblePartQty || convertiblePartQty <= BigDecimal.ZERO.longValue()) {
            return;
        }
        //按排产顺序倒序排产，优先级低的剔除
        Long needRemoveQty = exceedingPartQty - convertiblePartQty;
        capacityLimit(CapacityControlTypeEnum.SIZE_CAPACITY_CONTROL, needRemoveQty, singleTireFabricList, productionContext);
        singleTireFabricList.sort(Comparator.comparing(MonthPlanManufacturingRequirementVo::getProductionSequence, Comparator.reverseOrder()));
        for (MonthPlanManufacturingRequirementVo effectivePlan : singleTireFabricList) {
            if (convertiblePartQty <= BigDecimal.ZERO.longValue()) {
                break;
            }
            Long productionQty = effectivePlan.getProductionQty();
            if (productionQty <= BigDecimal.ZERO.longValue()) {
                continue;
            }
            //单层转多层
            if (productionQty <= convertiblePartQty) {
                effectivePlan.changedMultilayerTireFabric();
                convertiblePartQty = convertiblePartQty - productionQty;
            }
        }
        //还有没能转的量，则需要继续剔除
        if (convertiblePartQty <= BigDecimal.ZERO.longValue()) {
            return;
        }
        capacityLimit(CapacityControlTypeEnum.SIZE_CAPACITY_CONTROL, convertiblePartQty, singleTireFabricList, productionContext);
    }

    /**
     * 产能控制处理：
     * 对requirementPlanList计划按优先级从低到高，总共剔除needRemoveQty的量
     * 如果某个ProductCode最终计划量小于最小批量，则该ProductCode所有的计划都剔除
     *
     * @param controlType         产能控制类型
     * @param needRemoveQty       需要剔除的计划总量
     * @param requirementPlanList 可剔除的需求计划集合
     * @param productionContext   排产上下文
     */
    private static void capacityLimit(CapacityControlTypeEnum controlType, Long needRemoveQty, List<MonthPlanManufacturingRequirementVo> requirementPlanList, ProductionContext productionContext) {
        //没有需求
        if (CollectionUtils.isEmpty(requirementPlanList) || needRemoveQty <= BigDecimal.ZERO.longValue()) {
            return;
        }
        //按SAP代码分组
        Map<String, List<MonthPlanManufacturingRequirementVo>> productCodeGroupMap = requirementPlanList.stream().collect(Collectors.groupingBy(MonthPlanManufacturingRequirementVo::getProductCode));
        //按排产顺序倒序排产，优先级低的先剔除
        requirementPlanList.sort(Comparator.comparing(MonthPlanManufacturingRequirementVo::getProductionSequence, Comparator.reverseOrder()));
        for (MonthPlanManufacturingRequirementVo effectivePlan : requirementPlanList) {
            if (needRemoveQty <= BigDecimal.ZERO.longValue()) {
                break;
            }
            Long productionQty = effectivePlan.getProductionQty();
            if (productionQty <= BigDecimal.ZERO.longValue()) {
                continue;
            }
            //本身剔除
            if (needRemoveQty >= productionQty) {
                String noProductionReason;
                if (CapacityControlTypeEnum.SIZE_CAPACITY_CONTROL == controlType) {
                    noProductionReason = NoProductionReasonUtils.getSizeCapacityLimit(productionQty);
                } else {
                    noProductionReason = NoProductionReasonUtils.getTireCapacityLimit(productionQty);
                }
                effectivePlan.addNoProductionReasonAndQty(noProductionReason, productionQty);
                effectivePlan.setProductionQty(BigDecimal.ZERO.longValue());
                needRemoveQty = needRemoveQty - productionQty;
            } else {
                //剩余量
                Long leftOverQty = productionQty - needRemoveQty;
                String noProductionReason;
                if (CapacityControlTypeEnum.SIZE_CAPACITY_CONTROL == controlType) {
                    noProductionReason = NoProductionReasonUtils.getSizeCapacityLimit(needRemoveQty);
                } else {
                    noProductionReason = NoProductionReasonUtils.getTireCapacityLimit(needRemoveQty);
                }
                effectivePlan.addNoProductionReasonAndQty(noProductionReason, needRemoveQty);
                needRemoveQty = BigDecimal.ZERO.longValue();
                effectivePlan.setProductionQty(leftOverQty);
            }
            //汇总需求，与最小批量比较
            String productCode = effectivePlan.getProductCode();
            List<MonthPlanManufacturingRequirementVo> allPlan = productCodeGroupMap.get(productCode);
            Long sumLeftOverQty = allPlan.stream().mapToLong(MonthPlanManufacturingRequirementVo::getProductionQty).sum();
            Long minQty = productionContext.getMinimumLotSizeMap().get(productCode);
            if (sumLeftOverQty >= minQty) {
                continue;
            }
            //总值小于最小批量，则所有的都扣除
            for (MonthPlanManufacturingRequirementVo sameProductPlan : allPlan) {
                Long leftOverProductionQty = sameProductPlan.getProductionQty();
                if (leftOverProductionQty <= BigDecimal.ZERO.longValue()) {
                    continue;
                }
                String noProductionReason;
                if (CapacityControlTypeEnum.SIZE_CAPACITY_CONTROL == controlType) {
                    noProductionReason = NoProductionReasonUtils.getSizeCapacityMinLimit(leftOverProductionQty);
                } else {
                    noProductionReason = NoProductionReasonUtils.getTireCapacityMinLimit(leftOverProductionQty);
                }
                sameProductPlan.addNoProductionReasonAndQty(noProductionReason, leftOverProductionQty);
                sameProductPlan.setProductionQty(BigDecimal.ZERO.longValue());
                needRemoveQty = needRemoveQty - leftOverProductionQty;
            }
        }
    }

    private CapacityControlUtils() {

    }
}
