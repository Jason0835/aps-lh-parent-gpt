package com.zlt.aps.monthplan.mdm.handler;

import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.enums.FormingMethodTypeEnum;
import com.tlt.aps.enums.WorkWearTypeEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.domain.vo.MonthPlanManufacturingRequirementVo;
import com.zlt.aps.maindata.domain.vo.SizeCapacityParamVo;
import com.zlt.aps.monthplan.api.domain.entity.SizeCapacityConfiguration;
import com.zlt.aps.monthplan.api.domain.vo.BaseMoldingMachineInfoVo;
import com.zlt.aps.monthplan.mdm.dto.SizeCapacityAllocationResultDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 在产寸口优先，结合大寸口需求平衡分配成型产能
 * 1、先按成型法分组：1一次法先分配，2次法分配
 * 2、先对在产寸口(即续作寸口)的需求分配续作成型产能
 * 2.1、续作成型产能：严格按胎体布层级，多层与多层匹配，单层与单层匹配
 * 2.2、终止条件：续作寸口需求分配完毕或是续作成型产能分配完毕为止
 * 2、剩余还需要分配的需求中，按寸口由大到小开始分配
 * 2.1、优先分配有剩余产能的成型产能，如果没有了，
 * 则从已分配的续作成型产能中剔除寸口最小的已分配需求，留出的产能给到大寸口需求
 * 2.2、剔除挤占的需求，加入还需分配列表中，进行一下次2步骤的分配
 *
 * @author ZLT
 * @date 20250811
 */
@Service
public class ProductionProSizePriorityServiceImpl implements SizeCapacityAllocationService<BaseMoldingMachineInfoVo> {
    /**
     * 对需求按成型产能信息进行需求成型产能分配
     *
     * @param sizeCapacityRequireList    寸口产能需求集合
     * @param moldingMachineCapacityList 成型寸口产能集合
     * @param param                      分配参数
     * @return
     */
    @Override
    public List<SizeCapacityConfiguration> allocationMoldingMachineCapacity(List<MonthPlanManufacturingRequirementVo> sizeCapacityRequireList, List<BaseMoldingMachineInfoVo> moldingMachineCapacityList, SizeCapacityParamVo param) {
        if (CollectionUtils.isEmpty(sizeCapacityRequireList) || CollectionUtils.isEmpty(moldingMachineCapacityList)) {
            return Collections.emptyList();
        }
        //构建需求(按寸口+工装类别+成型法+胎体布层级分组)-产能分配结果对象(此时还没进行分配，即成型产能分配列表为空)
        List<SizeCapacityAllocationResultDto> emptyAllocationList = initNoAllocationConfigurationList(sizeCapacityRequireList);
        if (CollectionUtils.isEmpty(emptyAllocationList)) {
            return Collections.emptyList();
        }
        //全局需求集合
        Map<String, SizeCapacityAllocationResultDto> globalRequirementMap = emptyAllocationList.stream().collect(Collectors.toMap(SizeCapacityAllocationResultDto::getSizeCapacityGroupKey, Function.identity()));
        initMoldingMachineAllocationCapacityInfo(moldingMachineCapacityList, param);
        //需求按成型法分组
        Map<String, List<SizeCapacityAllocationResultDto>> mouldMethodGroupRequirementMap = emptyAllocationList.stream().collect(Collectors.groupingBy(SizeCapacityAllocationResultDto::getMouldMethod));
        //提取成型法
        List<String> mouldMethodList = new ArrayList<>(mouldMethodGroupRequirementMap.keySet());
        //先一次法，再二次法
        mouldMethodList.sort(Comparator.comparing(String::valueOf));
        //全局成型产能集合
        Map<String, BaseMoldingMachineInfoVo> globalCapacityMap = moldingMachineCapacityList.stream().collect(Collectors.toMap(BaseMoldingMachineInfoVo::getMoldingMachineCode, Function.identity()));
        //按成型法分组成型产能
        Map<String, List<BaseMoldingMachineInfoVo>> mouldMethodGroupMoldingMachineCapacityMap = moldingMachineCapacityList.stream().collect(Collectors.groupingBy(BaseMoldingMachineInfoVo::getMouldMethod));
        //按成型法，对成型法需求分配成型产能
        mouldMethodList.stream().forEach(mouldMethod -> {
            //成型法需求：包含寸口+工装类型+胎体布层级
            List<SizeCapacityAllocationResultDto> mouldMethodRequireList = mouldMethodGroupRequirementMap.get(mouldMethod);
            //成型法成型产能：包含在产寸口、可产寸口、支持的胎体布层级、特殊限定设置
            List<BaseMoldingMachineInfoVo> mouldMethodMoldingMachineCapacityList = mouldMethodGroupMoldingMachineCapacityMap.get(mouldMethod);
            //对需求分配成型产能
            moldingMachineCapacityAllocation(mouldMethod, param, globalRequirementMap, globalCapacityMap, mouldMethodMoldingMachineCapacityList, mouldMethodRequireList);
        });
        //配置转换
        List<SizeCapacityConfiguration> configurationList = SizeCapacityConfigurationUtils.buildConfigurationResult(emptyAllocationList, param, globalRequirementMap, globalCapacityMap);
        if (CollectionUtils.isEmpty(configurationList)) {
            return Collections.emptyList();
        }
        configurationList.stream().forEach(configuration -> {
            Integer remainingDays = configuration.getRemainingDays();
            if (null != remainingDays && remainingDays <= BigDecimal.ZERO.intValue()) {
                configuration.setRemainingDays(null);
            }
        });
        return configurationList;
    }

    /**
     * 构建初始待分配需求信息：
     * 按寸口、成型法、胎体布层级分组汇总需求信息
     *
     * @param sizeCapacityRequireList
     * @return
     */
    private List<SizeCapacityAllocationResultDto> initNoAllocationConfigurationList(List<MonthPlanManufacturingRequirementVo> sizeCapacityRequireList) {
        if (CollectionUtils.isEmpty(sizeCapacityRequireList)) {
            return Collections.emptyList();
        }
        List<SizeCapacityAllocationResultDto> emptyAllocationList = new ArrayList<>();
        //按寸口、工装类别、成型法、胎体布层级分组
        Map<String, List<MonthPlanManufacturingRequirementVo>> groupKeyMap = sizeCapacityRequireList.stream().collect(Collectors.groupingBy(MonthPlanManufacturingRequirementVo::getSizeCapacityGroupKey));
        groupKeyMap.entrySet().stream().forEach(groupEntry -> {
            List<MonthPlanManufacturingRequirementVo> requirementList = groupEntry.getValue();
            if (CollectionUtils.isEmpty(requirementList)) {
                return;
            }
            emptyAllocationList.add(buildInitAllocationResultInfo(requirementList));
        });
        return emptyAllocationList;
    }

    /**
     * 初始化成型产能列表信息
     * 1、剩余可分配天数
     * 2、已分配列表
     * 3、当前排产生胎个数 -- 设置为空？
     *
     * @param moldingMachineInfoList 成型产能列表
     * @param param                  产能分配参数
     */
    private void initMoldingMachineAllocationCapacityInfo(List<BaseMoldingMachineInfoVo> moldingMachineInfoList, SizeCapacityParamVo param) {
        if (CollectionUtils.isEmpty(moldingMachineInfoList)) {
            return;
        }
        //20250917 ZLT 18寸二次法大鼓特殊限定配置
        Set<String> specialRestrictionSet = new HashSet<>();
        String bigDrumCapacityValue = param.getBigDrumCapacityValue();
        if (StringUtils.isNotBlank(bigDrumCapacityValue)) {
            String[] limitValues = bigDrumCapacityValue.split(",");
            for (String limitValue : limitValues) {
                specialRestrictionSet.add(limitValue);
            }
        }
        moldingMachineInfoList.stream().forEach(moldingMachineInfo -> {
            moldingMachineInfo.setLeftOverCapacityDays(param.getMonthMaxDays());
            moldingMachineInfo.setProSizeList(new ArrayList<>());
            moldingMachineInfo.setProSizeRequirementList(new ArrayList<>());
            moldingMachineInfo.setCurrentEmbryoCodeNumber(null);
            moldingMachineInfo.setFinishAllocation(false);
            //20250917 ZLT 18寸二次法大鼓特殊限定设置
            if (specialRestrictionSet.contains(moldingMachineInfo.getMoldingMachineCode())) {
                moldingMachineInfo.setSpecialRestrictions(YesOrNoEnum.YES.getValue());
            } else {
                moldingMachineInfo.setSpecialRestrictions(YesOrNoEnum.NO.getValue());
            }
        });
    }

    /**
     * 按成型法需求，分配成型产能：分两轮进行分配
     * 1、第一轮，提取成型产能的在产寸口，按在产寸口的月需求分配在产寸口的成型产能
     * 直到在产寸口需求分配完毕或是在产寸口成型产能分配满为止
     * 2、第二轮，对还有剩余需求没有分配的寸口需求，按寸口由大到小进行分配剩余的成型产能
     * 按先多层再单层，寸口由大到小对需求进行产能分配
     * 2.1、先分配有剩余产能的成型产能，如果产能不足，则从已分配满的在产寸口成型产能中将分配最小寸口的需求挤出
     * 2.2、将挤出的寸口需求，加入还需分配的需求列表中，等待下一次分配
     *
     * @param mouldMethod                成型法
     * @param param                      寸口产能分配参数
     * @param globalRequirementMap       所有需求信息
     * @param globalCapacityMap          所有成型产能信息
     * @param moldingMachineCapacityList 成型法-产能列表
     * @param mouldMethodRequirementList 成型法-需求列表
     */
    private void moldingMachineCapacityAllocation(String mouldMethod, SizeCapacityParamVo param, Map<String, SizeCapacityAllocationResultDto> globalRequirementMap, Map<String, BaseMoldingMachineInfoVo> globalCapacityMap, List<BaseMoldingMachineInfoVo> moldingMachineCapacityList, List<SizeCapacityAllocationResultDto> mouldMethodRequirementList) {
        if (CollectionUtils.isEmpty(mouldMethodRequirementList) || CollectionUtils.isEmpty(moldingMachineCapacityList)) {
            return;
        }
        //成型法
        FormingMethodTypeEnum mouldMethodType = FormingMethodTypeEnum.getInstance(mouldMethod);
        //提取胎体布层级信息
        List<Integer> tireFabricNumberList = new ArrayList<>(mouldMethodRequirementList.stream().map(SizeCapacityAllocationResultDto::getTireFabricNumber).collect(Collectors.toSet()));
        //获取在产寸口信息
        List<BigDecimal> productionProSizeList = getProductionProSizeInfo(moldingMachineCapacityList);
        //第一轮：在产寸口需求先分配匹配在产成型产能
        allocationProductionProSizeRequirement(mouldMethodType, productionProSizeList, param, globalCapacityMap, moldingMachineCapacityList, mouldMethodRequirementList, globalRequirementMap);
        //第二轮：寸口由大到小，优先分配有产能的成型产能，再次从续作寸口成型-最小寸口中分配，从而将小寸口需求量挤出给大寸口
        List<SizeCapacityAllocationResultDto> needAllocationRequirementList = mouldMethodRequirementList.stream().filter(mouldMethodRequirement -> mouldMethodRequirement.isNeedAllocation()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(needAllocationRequirementList)) {
            //20250928 ZLT 处理最后的剩余天数
            handlerLeftOverDays(mouldMethod, globalRequirementMap, moldingMachineCapacityList, param);
            return;
        }
        //先多层，再单层
        tireFabricNumberList.sort(Comparator.comparing(Integer::intValue, Comparator.reverseOrder()));
        tireFabricNumberList.stream().forEach(tireFabricNumber -> {
            Set<BigDecimal> allocationFinishProSizeSet = new HashSet<>();
            allocationMoldMachineCapacity(mouldMethodType, tireFabricNumber, globalRequirementMap, allocationFinishProSizeSet, needAllocationRequirementList, param, globalCapacityMap, moldingMachineCapacityList);
        });
        //20250928 ZLT 处理最后的剩余天数
        handlerLeftOverDays(mouldMethod, globalRequirementMap, moldingMachineCapacityList, param);
    }

    /**
     * 构建某分组：寸口|*|工装类别|*|成型法|*|胎体布层级，
     * 还没进行成型产能分配的需求分配结果对象
     *
     * @param requirementList 需求列表
     * @return
     */
    private SizeCapacityAllocationResultDto buildInitAllocationResultInfo(List<MonthPlanManufacturingRequirementVo> requirementList) {
        //总需求量
        Long sumRequirementQty = requirementList.stream().mapToLong(MonthPlanManufacturingRequirementVo::getProductionQty).sum();
        //净需求量
        List<MonthPlanManufacturingRequirementVo> netRequirementList = requirementList.stream().filter(requirement -> YesOrNoEnum.NO.getValue().equals(requirement.getIsStockUp())).collect(Collectors.toList());
        Long netDemandQty = BigDecimal.ZERO.longValue();
        if (!CollectionUtils.isEmpty(netRequirementList)) {
            netDemandQty = netRequirementList.stream().mapToLong(MonthPlanManufacturingRequirementVo::getProductionQty).sum();
        }
        //备货量
        List<MonthPlanManufacturingRequirementVo> stockUpRequirementList = requirementList.stream().filter(requirement -> YesOrNoEnum.YES.getValue().equals(requirement.getIsStockUp())).collect(Collectors.toList());
        Long stockUpDemandQty = BigDecimal.ZERO.longValue();
        if (!CollectionUtils.isEmpty(stockUpRequirementList)) {
            stockUpDemandQty = stockUpRequirementList.stream().mapToLong(MonthPlanManufacturingRequirementVo::getProductionQty).sum();
        }
        MonthPlanManufacturingRequirementVo requirement = requirementList.get(0);
        return SizeCapacityAllocationResultDto.buildEmptyAllocationInfo(requirement.getProSize(), requirement.getWorkWearTypeValue(), requirement.getMouldMethod(), requirement.getTireFabricNumber(), sumRequirementQty, netDemandQty, stockUpDemandQty);
    }

    /**
     * 获取成型产能在产寸口信息
     *
     * @param moldingMachineCapacityList 成型产能集合信息
     * @return
     */
    private List<BigDecimal> getProductionProSizeInfo(List<BaseMoldingMachineInfoVo> moldingMachineCapacityList) {
        if (CollectionUtils.isEmpty(moldingMachineCapacityList)) {
            return Collections.emptyList();
        }
        //提取在产寸口信息不为空的集合
        List<BaseMoldingMachineInfoVo> productionProSizeMoldingMachineList = moldingMachineCapacityList.stream().filter(moldingMachineCapacity -> null != moldingMachineCapacity.getCurrentProSize()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(productionProSizeMoldingMachineList)) {
            return Collections.emptyList();
        }
        //获取在产寸口信息
        return new ArrayList<>(productionProSizeMoldingMachineList.stream().map(BaseMoldingMachineInfoVo::getCurrentProSize).collect(Collectors.toSet()));
    }

    /**
     * 按成型法-在产寸口需求分配在产成型产能
     * 前提条件：在产寸口需求只匹配在产寸口一致的成型产能：
     * 即在产寸口15寸的成型产能，只分配15寸口的需求，空成型产能在此轮不参与分配
     * 1、先按胎体布层级严格匹配即多层胎体布需求匹配多层胎体布成型产能
     * 单层胎体布需求匹配单层胎体布成型产能
     * 2、然后，寸口对应有剩余的单层胎体布需求，匹配多层胎体布成型产能
     *
     * @param mouldMethodType            成型法
     * @param productionProSizeList      在产寸口列表
     * @param param                      寸口产能分配参数
     * @param globalCapacityMap          所有成型产能集合
     * @param moldingMachineCapacityList 成型法-成型产能集合
     * @param mouldMethodRequirementList 成型法-寸口需求集合
     * @param globalRequirementMap       所有需求信息
     */
    private void allocationProductionProSizeRequirement(FormingMethodTypeEnum mouldMethodType, List<BigDecimal> productionProSizeList, SizeCapacityParamVo param, Map<String, BaseMoldingMachineInfoVo> globalCapacityMap, List<BaseMoldingMachineInfoVo> moldingMachineCapacityList, List<SizeCapacityAllocationResultDto> mouldMethodRequirementList, Map<String, SizeCapacityAllocationResultDto> globalRequirementMap) {
        if (CollectionUtils.isEmpty(productionProSizeList) || CollectionUtils.isEmpty(mouldMethodRequirementList) || CollectionUtils.isEmpty(moldingMachineCapacityList)) {
            return;
        }
        Set<BigDecimal> productionProSizeSet = productionProSizeList.stream().collect(Collectors.toSet());
        //成型法，按寸口需求分组
        Map<BigDecimal, List<SizeCapacityAllocationResultDto>> proSizeRequirementMap = mouldMethodRequirementList.stream().collect(Collectors.groupingBy(SizeCapacityAllocationResultDto::getProSize));
        //提取在产寸口成型产能集合
        List<BaseMoldingMachineInfoVo> productionProSizeMoldingMachineList = moldingMachineCapacityList.stream().filter(moldingMachineCapacity -> null != moldingMachineCapacity.getCurrentProSize()).collect(Collectors.toList());
        //按在产寸口进行成型产能分组
        Map<BigDecimal, List<BaseMoldingMachineInfoVo>> productionProSizeCapacityMap = productionProSizeMoldingMachineList.stream().collect(Collectors.groupingBy(BaseMoldingMachineInfoVo::getCurrentProSize));
        //寸口由大到小，按胎体布层级严格匹配(此时只进行在产寸口需求分配)
        productionProSizeList.sort(Comparator.comparing(BigDecimal::intValue, Comparator.reverseOrder()));
        productionProSizeList.stream().forEach(productionProSize -> {
            List<SizeCapacityAllocationResultDto> requirementList = proSizeRequirementMap.get(productionProSize);
            //在产寸口没有需求
            if (CollectionUtils.isEmpty(requirementList)) {
                return;
            }
            List<BaseMoldingMachineInfoVo> productionMoldingMachineList = productionProSizeCapacityMap.get(productionProSize);
            //在产寸口没有成型产能
            if (CollectionUtils.isEmpty(productionMoldingMachineList)) {
                return;
            }
            //设置成型产能对应寸口最大天产能 20251013 ZLT 成型硫化配比信息
            SizeCapacityAllocationUtils.setDayMaxProSizeCapacityInfo(productionMoldingMachineList, productionProSize);
            allocationMoldingMachineCapacity(mouldMethodType, productionProSize, param, globalCapacityMap, productionMoldingMachineList, requirementList);
        });
        //处理在产寸口单层胎体布还有剩余的需求--此时分配的是多层的在产成型产能
        handlerSingleTireFabricNumberLeftOver(productionProSizeSet, mouldMethodRequirementList, productionProSizeCapacityMap, param, globalCapacityMap);
    }

    /**
     * 按寸口由大到小进行分配
     * 大寸口产能分配剩余成型产能，
     * 如果分配不足，则从已分配完的成型产能中挑选最小寸口的成型产能，将其分配产能挤出(开关控制)
     * 直到大寸口产能分配完成。将挤出的需求，加入到剩余分配量中，进行下一次分配
     *
     * @param mouldMethodType               成型法
     * @param tireFabricNumber              胎体布层级
     * @param globalRequirementMap          所有需求信息
     * @param allocationFinishProSizeSet    分配完成的寸口集合
     * @param needAllocationRequirementList 还需分配的需求
     * @param param                         寸口分配参数
     * @param globalCapacityMap             所有成型产能集合
     * @param moldingMachineCapacityList    成型法-产能列表
     */
    private void allocationMoldMachineCapacity(FormingMethodTypeEnum mouldMethodType, Integer tireFabricNumber, Map<String, SizeCapacityAllocationResultDto> globalRequirementMap, Set<BigDecimal> allocationFinishProSizeSet, List<SizeCapacityAllocationResultDto> needAllocationRequirementList, SizeCapacityParamVo param, Map<String, BaseMoldingMachineInfoVo> globalCapacityMap, List<BaseMoldingMachineInfoVo> moldingMachineCapacityList) {
        if (CollectionUtils.isEmpty(needAllocationRequirementList) && CollectionUtils.isEmpty(moldingMachineCapacityList)) {
            return;
        }
        //胎体布层级匹配
        List<SizeCapacityAllocationResultDto> tireFabricNumberGroupRequirementList = needAllocationRequirementList.stream().filter(needAllocationRequirement -> tireFabricNumber.equals(needAllocationRequirement.getTireFabricNumber())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(tireFabricNumberGroupRequirementList)) {
            return;
        }
        //提取寸口
        List<BigDecimal> proSizeList = new ArrayList<>(tireFabricNumberGroupRequirementList.stream().map(SizeCapacityAllocationResultDto::getProSize).collect(Collectors.toSet()));
        proSizeList.sort(Comparator.comparing(BigDecimal::intValue, Comparator.reverseOrder()));
        //寸口由大到小
        proSizeList.stream().forEach(proSize -> {
            if (allocationFinishProSizeSet.contains(proSize)) {
                return;
            }
            //提取还有剩余产能的可排产成型产能
            List<SizeCapacityAllocationResultDto> needProSizeRequirementList = tireFabricNumberGroupRequirementList.stream().filter(needProSizeRequirement -> needProSizeRequirement.isNeedAllocation(tireFabricNumber, proSize, mouldMethodType.getMethodValue())).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(needProSizeRequirementList)) {
                return;
            }
            //20250918 ZLT 18寸二次法有可能多条，其它此时最多只会有一条(成型和胎体布层级已定)
            needProSizeRequirementList.stream().forEach(needProSizeRequirement -> {
                allocationMoldingMachineCapacity(mouldMethodType, proSize, tireFabricNumber, globalRequirementMap, allocationFinishProSizeSet, needProSizeRequirement, param, globalCapacityMap, moldingMachineCapacityList);
                SizeCapacityAllocationResultDto originRequirement = globalRequirementMap.get(needProSizeRequirement.getSizeCapacityGroupKey());
                if (null != originRequirement) {
                    originRequirement.setFinishAllocation(true);
                }
            });
            //重新获取还有剩余量的分配需求(可能包含被挤占出来的续作寸口需求)
            List<SizeCapacityAllocationResultDto> hasNeedRequirementList = SizeCapacityAllocationUtils.getNeedAllocationRequirementList(globalRequirementMap, mouldMethodType, tireFabricNumber, proSize);
            if (CollectionUtils.isEmpty(hasNeedRequirementList)) {
                return;
            }
            allocationMoldMachineCapacity(mouldMethodType, tireFabricNumber, globalRequirementMap, allocationFinishProSizeSet, hasNeedRequirementList, param, globalCapacityMap, moldingMachineCapacityList);
        });
    }

    /**
     * 第一轮分配：按需分配成型产能，严格按胎体布层级匹配
     * 即多层匹配多层，单层匹配单层
     *
     * @param mouldMethodType            成型法
     * @param proSize                    寸口
     * @param param                      寸口产能分配参数
     * @param globalCapacityMap          所有成型产能
     * @param moldingMachineCapacityList 成型法-在产寸口成型产能集合
     * @param mouldMethodRequirementList 成型法-寸口需求集合
     */
    private void allocationMoldingMachineCapacity(FormingMethodTypeEnum mouldMethodType, BigDecimal proSize, SizeCapacityParamVo param, Map<String, BaseMoldingMachineInfoVo> globalCapacityMap, List<BaseMoldingMachineInfoVo> moldingMachineCapacityList, List<SizeCapacityAllocationResultDto> mouldMethodRequirementList) {
        //按胎体布层级分组
        Map<Integer, List<SizeCapacityAllocationResultDto>> tireFabricNumberGroupRequirementMap = mouldMethodRequirementList.stream().collect(Collectors.groupingBy(SizeCapacityAllocationResultDto::getTireFabricNumber));
        List<Integer> tireFabricNumberList = new ArrayList<>(tireFabricNumberGroupRequirementMap.keySet());
        //先多层，再单层
        tireFabricNumberList.sort(Comparator.comparing(Integer::intValue, Comparator.reverseOrder()));
        tireFabricNumberList.stream().forEach(tireFabricNumber -> {
            List<SizeCapacityAllocationResultDto> requirementList = tireFabricNumberGroupRequirementMap.get(tireFabricNumber);
            if (CollectionUtils.isEmpty(requirementList)) {
                return;
            }
            //20250917 ZLT 因增加了工装类型，二次法18寸可能不止一条数据(其它还是一条数据)
            requirementList.stream().forEach(needAllocationRequirement -> {
                List<BaseMoldingMachineInfoVo> enableAllocationCapacityList = SizeCapacityAllocationUtils.getEnableAllocationMoldingMachineByStrictMatch(proSize, needAllocationRequirement.getWorkWearType(), mouldMethodType.getMethodValue(), tireFabricNumber, moldingMachineCapacityList);
                if (CollectionUtils.isEmpty(enableAllocationCapacityList)) {
                    return;
                }
                allocationMoldingMachineCapacityByRequirement(globalCapacityMap, param, needAllocationRequirement, enableAllocationCapacityList);
            });
        });
    }

    /**
     * 第一轮，处理单层剩余需求量：成型产能寸口还是只有一个
     * 进行在产寸口，单层胎体布剩余需求分配成型产能，可分配到有剩余产能的成型产能
     * 此时，成型产能可能是多层，也可能是单层
     *
     * @param productionProSizeSet         在产寸口
     * @param mouldMethodRequirementList   需求集合
     * @param productionProSizeCapacityMap 在产寸口成型产能集合
     * @param param                        寸口产能分配参数
     * @param globalCapacityMap            所有成型产能集合
     */
    private void handlerSingleTireFabricNumberLeftOver(Set<BigDecimal> productionProSizeSet, List<SizeCapacityAllocationResultDto> mouldMethodRequirementList, Map<BigDecimal, List<BaseMoldingMachineInfoVo>> productionProSizeCapacityMap, SizeCapacityParamVo param, Map<String, BaseMoldingMachineInfoVo> globalCapacityMap) {
        //获取单层还有剩余的需求
        List<SizeCapacityAllocationResultDto> productionProSizeRequirementLeftOverList = mouldMethodRequirementList.stream().filter(requirement -> requirement.isNeedAllocationSingleTireFabricNumber(productionProSizeSet)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(productionProSizeRequirementLeftOverList)) {
            return;
        }
        productionProSizeRequirementLeftOverList.stream().forEach(needAllocationRequirement -> {
            List<BaseMoldingMachineInfoVo> productionMoldingMachineList = productionProSizeCapacityMap.get(needAllocationRequirement.getProSize());
            if (CollectionUtils.isEmpty(productionMoldingMachineList)) {
                return;
            }
            List<BaseMoldingMachineInfoVo> enableMoldingMachineList = productionMoldingMachineList.stream().filter(productionMoldingMachineCapacity -> productionMoldingMachineCapacity.isHasCapacity()).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(enableMoldingMachineList)) {
                return;
            }
            //20250917 ZLT 二次法18寸-大鼓需求特殊处理
            boolean isSpecialRestrictions = needAllocationRequirement.isSpecialRestrictions();
            if (isSpecialRestrictions) {
                if (WorkWearTypeEnum.BIG_DRUM.getTypeValue().equals(needAllocationRequirement.getWorkWearType())) {
                    enableMoldingMachineList = enableMoldingMachineList.stream().filter(productionMoldingMachineCapacity -> YesOrNoEnum.YES.getValue().equals(productionMoldingMachineCapacity.getSpecialRestrictions())).collect(Collectors.toList());
                } else {
                    enableMoldingMachineList = enableMoldingMachineList.stream().filter(productionMoldingMachineCapacity -> YesOrNoEnum.NO.getValue().equals(productionMoldingMachineCapacity.getSpecialRestrictions())).collect(Collectors.toList());
                }
                if (CollectionUtils.isEmpty(enableMoldingMachineList)) {
                    return;
                }
            }
            //无限制 > 剩余天数多 > 天产能大 > 成型产能编号
            enableMoldingMachineList.sort(SizeCapacityAllocationUtils.getMoldingMachineCapacitySort());
            enableMoldingMachineList.stream().forEach(moldingMachineInfo -> {
                if (needAllocationRequirement.getLeftOverQty() <= BigDecimal.ZERO.longValue()) {
                    return;
                }
                String key = moldingMachineInfo.getMoldingMachineCode();
                BaseMoldingMachineInfoVo moldingMachineCapacity = globalCapacityMap.get(key);
                SizeCapacityAllocationUtils.allocation(moldingMachineCapacity, param, needAllocationRequirement);
            });
        });
    }

    /**
     * 20250916 ZLT 处理剩余天数，最后的剩余天数全部配置给最后一个分配信息
     *
     * @param mouldMethod                成型法
     * @param globalMap                  所有需求信息
     * @param moldingMachineCapacityList 成型法成型产能
     * @param param                      参数
     */
    private void handlerLeftOverDays(String mouldMethod, Map<String, SizeCapacityAllocationResultDto> globalMap, List<BaseMoldingMachineInfoVo> moldingMachineCapacityList, SizeCapacityParamVo param) {
        if (CollectionUtils.isEmpty(moldingMachineCapacityList) || null == param) {
            return;
        }
        moldingMachineCapacityList.stream().forEach(moldingMachineCapacity -> {
            if (moldingMachineCapacity.getLeftOverCapacityDays() <= BigDecimal.ZERO.intValue()) {
                return;
            }
            //处理无需切换
            SizeCapacityAllocationUtils.noChangeProSizeByLeftOverDays(moldingMachineCapacity, globalMap);
        });
    }

    /**
     * 按需分配成型产能，先查找有剩余产能的，没有再从续作寸口排满中获取寸口最小的
     * 1、获取
     *
     * @param mouldMethodType            成型法
     * @param proSize                    寸口
     * @param tireFabricNumber           胎体布层级
     * @param globalRequirementMap       所有需求信息
     * @param needAllocationRequirement  还需分配的需求信息
     * @param allocationFinishProSizeSet 分配完成的寸口
     * @param param                      寸口产能分配参数
     * @param globalCapacityMap          所有成型产能
     * @param moldingMachineCapacityList 成型法-成型产能集合
     */
    private void allocationMoldingMachineCapacity(FormingMethodTypeEnum mouldMethodType, BigDecimal proSize, Integer tireFabricNumber, Map<String, SizeCapacityAllocationResultDto> globalRequirementMap, Set<BigDecimal> allocationFinishProSizeSet, SizeCapacityAllocationResultDto needAllocationRequirement, SizeCapacityParamVo param, Map<String, BaseMoldingMachineInfoVo> globalCapacityMap, List<BaseMoldingMachineInfoVo> moldingMachineCapacityList) {
        //20250916 ZLT 没有开启挤占时，则不进行挤占
        if (!FactoryConstant.YES_VALUE.equals(param.getOpenCrowdOut())) {
            allocationMoldingMachineCapacityByNoCrowdOut(mouldMethodType, proSize, tireFabricNumber, globalRequirementMap, allocationFinishProSizeSet, needAllocationRequirement, param, globalCapacityMap, moldingMachineCapacityList);
            return;
        }
        //获取有剩余产能的可分配的成型产能集合--会设置相应的寸口产能
        List<BaseMoldingMachineInfoVo> haseLeftOverCapacityList = SizeCapacityAllocationUtils.getEnableAllocationMoldingMachineCapacity(proSize, mouldMethodType.getMethodValue(), tireFabricNumber, moldingMachineCapacityList);
        if (CollectionUtils.isEmpty(haseLeftOverCapacityList)) {
            //获取在产寸口成型产能已排满的成型产能集合
            allocationProductionProSizeCapacity(mouldMethodType, proSize, tireFabricNumber, globalRequirementMap, needAllocationRequirement, param, globalCapacityMap, moldingMachineCapacityList);
            return;
        }
        //计算各成型产能针对需求需要的天数
        SizeCapacityAllocationUtils.setRequirementQtyNeedDays(haseLeftOverCapacityList, param, needAllocationRequirement.getLeftOverQty());
        //提取可一次性满足分配量的成型产能
        List<BaseMoldingMachineInfoVo> hasNoPassWholeMoldingMachineList = SizeCapacityAllocationUtils.getSatisfyByOneMoldingMachineInfo(haseLeftOverCapacityList, param);
        //没有可一次性满足需求的成型产能
        if (CollectionUtils.isEmpty(hasNoPassWholeMoldingMachineList)) {
            //需要找合适的成型产能
            allocationProductionCapacity(mouldMethodType, proSize, tireFabricNumber, globalRequirementMap, needAllocationRequirement, param, globalCapacityMap, moldingMachineCapacityList);
            return;
        }
        //取天数相差最接近的
        hasNoPassWholeMoldingMachineList.sort(Comparator.comparing(BaseMoldingMachineInfoVo::getDifferenceDays));
        BaseMoldingMachineInfoVo allocationMoldingMachineInfo = hasNoPassWholeMoldingMachineList.get(0);
        //20251027 ZLT 特殊限制控制 1、二次法18寸是否还可进行分配 2、一次法20寸是否还可进行分配
        boolean isReachLimit = SizeCapacityAllocationUtils.isReachLimitNumber(allocationMoldingMachineInfo, param, needAllocationRequirement);
        if (isReachLimit) {
            needAllocationRequirement.setFinishAllocation(true);
            return;
        }
        allocationCapacityByRequirement(allocationMoldingMachineInfo, needAllocationRequirement, param);
    }

    /**
     * 非挤占式-分配
     *
     * @param mouldMethodType            成型法
     * @param proSize                    寸口
     * @param tireFabricNumber           胎体布层级
     * @param globalRequirementMap       所有需求信息
     * @param needAllocationRequirement  还需分配的需求信息
     * @param allocationFinishProSizeSet 分配完成的寸口
     * @param param                      寸口产能分配参数
     * @param globalCapacityMap          所有成型产能
     * @param moldingMachineCapacityList 成型法-成型产能集合
     */
    private void allocationMoldingMachineCapacityByNoCrowdOut(FormingMethodTypeEnum mouldMethodType, BigDecimal proSize, Integer tireFabricNumber, Map<String, SizeCapacityAllocationResultDto> globalRequirementMap, Set<BigDecimal> allocationFinishProSizeSet, SizeCapacityAllocationResultDto needAllocationRequirement, SizeCapacityParamVo param, Map<String, BaseMoldingMachineInfoVo> globalCapacityMap, List<BaseMoldingMachineInfoVo> moldingMachineCapacityList) {
        //获取有剩余产能的可分配的成型产能集合--会设置相应的寸口产能
        List<BaseMoldingMachineInfoVo> haseLeftOverCapacityList = SizeCapacityAllocationUtils.getEnableAllocationMoldingMachineCapacityByNoCrowdOut(proSize, mouldMethodType.getMethodValue(), tireFabricNumber, moldingMachineCapacityList);
        if (CollectionUtils.isEmpty(haseLeftOverCapacityList)) {
            needAllocationRequirement.setFinishAllocation(true);
            return;
        }
        //计算各成型产能针对需求需要的天数
        SizeCapacityAllocationUtils.setRequirementQtyNeedDays(haseLeftOverCapacityList, param, needAllocationRequirement.getLeftOverQty());
        //提取可一次性满足分配量的成型产能
        List<BaseMoldingMachineInfoVo> hasNoPassWholeMoldingMachineList = SizeCapacityAllocationUtils.getSatisfyByOneMoldingMachineInfo(haseLeftOverCapacityList, param);
        //没有可一次性满足需求的成型产能
        if (CollectionUtils.isEmpty(hasNoPassWholeMoldingMachineList)) {
            //需要找合适的成型产能
            allocationProductionCapacity(mouldMethodType, proSize, tireFabricNumber, globalRequirementMap, needAllocationRequirement, param, globalCapacityMap, moldingMachineCapacityList);
            return;
        }
        //取天数相差最接近的
        hasNoPassWholeMoldingMachineList.sort(Comparator.comparing(BaseMoldingMachineInfoVo::getDifferenceDays));
        BaseMoldingMachineInfoVo allocationMoldingMachineInfo = hasNoPassWholeMoldingMachineList.get(0);
        //20251027 ZLT 特殊限制控制 1、二次法18寸是否还可进行分配 2、一次法20寸是否还可进行分配
        boolean isReachLimit = SizeCapacityAllocationUtils.isReachLimitNumber(allocationMoldingMachineInfo, param, needAllocationRequirement);
        if (isReachLimit) {
            needAllocationRequirement.setFinishAllocation(true);
            return;
        }
        allocationCapacityByRequirement(allocationMoldingMachineInfo, needAllocationRequirement, param);
    }

    /**
     * 可用一台成型产能分配完剩余需求
     *
     * @param allocationMoldingMachineInfo 成型产能信息
     * @param needAllocationRequirement    剩余需求信息
     * @param param                        寸口产能分配参数
     */
    private void allocationCapacityByRequirement(BaseMoldingMachineInfoVo allocationMoldingMachineInfo, SizeCapacityAllocationResultDto needAllocationRequirement, SizeCapacityParamVo param) {
        Set<BigDecimal> assignedProSizeSet = allocationMoldingMachineInfo.getAssignedProSize();
        //需要天数
        Integer needDays = allocationMoldingMachineInfo.getRequirementNeedDays();
        //剩余天数
        Integer leftOverDays = allocationMoldingMachineInfo.getLeftOverCapacityDays();
        //实际天产能
        Long dayCapacityQty = SizeCapacityAllocationUtils.calculateDayCapacity(allocationMoldingMachineInfo, param);
        //从来没有分配过,且分配完后成型产能不会有剩余产能
        if (CollectionUtils.isEmpty(assignedProSizeSet) && needDays >= leftOverDays) {
            allocationMoldingMachineInfo.setLeftOverCapacityDays(BigDecimal.ZERO.intValue());
            SizeCapacityAllocationUtils.updateAllocationInfo(param, allocationMoldingMachineInfo, needAllocationRequirement, needAllocationRequirement.getLeftOverQty(), dayCapacityQty, leftOverDays);
            needAllocationRequirement.setFinishAllocation(true);
            allocationMoldingMachineInfo.setFinishAllocation(true);
            return;
        }
        //从来没有分配过,且分配完后成型产能还有剩余产能
        if (CollectionUtils.isEmpty(assignedProSizeSet) && needDays < leftOverDays) {
            allocationMoldingMachineInfo.setLeftOverCapacityDays(leftOverDays - needDays);
            SizeCapacityAllocationUtils.updateAllocationInfo(param, allocationMoldingMachineInfo, needAllocationRequirement, needAllocationRequirement.getLeftOverQty(), dayCapacityQty, needDays);
            needAllocationRequirement.setFinishAllocation(true);
            return;
        }
        //有分配过，且寸口一致,分配完后成型产能不会有剩余产能
        if (!CollectionUtils.isEmpty(assignedProSizeSet) && assignedProSizeSet.contains(needAllocationRequirement.getProSize()) && needDays >= leftOverDays) {
            allocationMoldingMachineInfo.setLeftOverCapacityDays(BigDecimal.ZERO.intValue());
            SizeCapacityAllocationUtils.updateAllocationInfo(param, allocationMoldingMachineInfo, needAllocationRequirement, needAllocationRequirement.getLeftOverQty(), dayCapacityQty, leftOverDays);
            needAllocationRequirement.setFinishAllocation(true);
            allocationMoldingMachineInfo.setFinishAllocation(true);
            return;
        }
        //有分配过，且寸口一致,分配完后成型产能还有剩余产能
        if (!CollectionUtils.isEmpty(assignedProSizeSet) && assignedProSizeSet.contains(needAllocationRequirement.getProSize()) && needDays < leftOverDays) {
            allocationMoldingMachineInfo.setLeftOverCapacityDays(leftOverDays - needDays);
            SizeCapacityAllocationUtils.updateAllocationInfo(param, allocationMoldingMachineInfo, needAllocationRequirement, needAllocationRequirement.getLeftOverQty(), dayCapacityQty, needDays);
            needAllocationRequirement.setFinishAllocation(true);
            return;
        }
        //有分配过，且寸口不一致，则一次性分配完毕即将剩余天数全部分配出去--一台成型产能只换一次寸口
        if (!CollectionUtils.isEmpty(assignedProSizeSet) && !assignedProSizeSet.contains(needAllocationRequirement.getProSize())) {
            allocationMoldingMachineInfo.setLeftOverCapacityDays(BigDecimal.ZERO.intValue());
            SizeCapacityAllocationUtils.updateAllocationInfo(param, allocationMoldingMachineInfo, needAllocationRequirement, needAllocationRequirement.getLeftOverQty(), dayCapacityQty, leftOverDays);
            needAllocationRequirement.setFinishAllocation(true);
            allocationMoldingMachineInfo.setFinishAllocation(true);
            return;
        }
    }

    /**
     * 按需求分配成型产能
     *
     * @param globalCapacityMap            所有成型产能集合
     * @param param                        分配参数
     * @param needAllocationRequirement    需分配的需求
     * @param enableAllocationCapacityList 可分配的成型产能集合
     */
    private void allocationMoldingMachineCapacityByRequirement(Map<String, BaseMoldingMachineInfoVo> globalCapacityMap, SizeCapacityParamVo param, SizeCapacityAllocationResultDto needAllocationRequirement, List<BaseMoldingMachineInfoVo> enableAllocationCapacityList) {
        if (null == needAllocationRequirement || needAllocationRequirement.getLeftOverQty() <= BigDecimal.ZERO.longValue()) {
            return;
        }
        if (CollectionUtils.isEmpty(enableAllocationCapacityList)) {
            return;
        }
        //无限制 > 剩余天数多 > 天产能大 > 成型产能编号
        enableAllocationCapacityList.sort(SizeCapacityAllocationUtils.getMoldingMachineCapacitySort());
        enableAllocationCapacityList.stream().forEach(moldingMachineInfo -> {
            if (needAllocationRequirement.getLeftOverQty() <= BigDecimal.ZERO.longValue()) {
                return;
            }
            String key = moldingMachineInfo.getMoldingMachineCode();
            BaseMoldingMachineInfoVo moldingMachineCapacity = globalCapacityMap.get(key);
            SizeCapacityAllocationUtils.allocation(moldingMachineCapacity, param, needAllocationRequirement);
        });
    }

    /**
     * 没有剩余产能的成型产能，需从已在产排完的成型产能中将小寸口需求挤出来
     * 一个成型产能，只进行一次寸口切换。故而进行了两个寸口分配的成型产能不再参与分配
     *
     * @param mouldMethodType            成型法
     * @param proSize                    寸口
     * @param tireFabricNumber           胎体层级
     * @param globalRequirementMap       所有需求信息
     * @param needAllocationRequirement  需分配需求信息
     * @param param                      寸口参数
     * @param globalCapacityMap          所有成型产能集合
     * @param moldingMachineCapacityList 寸口成型产能集合
     */
    private void allocationProductionProSizeCapacity(FormingMethodTypeEnum mouldMethodType, BigDecimal proSize, Integer tireFabricNumber, Map<String, SizeCapacityAllocationResultDto> globalRequirementMap, SizeCapacityAllocationResultDto needAllocationRequirement, SizeCapacityParamVo param, Map<String, BaseMoldingMachineInfoVo> globalCapacityMap, List<BaseMoldingMachineInfoVo> moldingMachineCapacityList) {
        //获取可产寸口需求的成型产能
        List<BaseMoldingMachineInfoVo> productionProSizeList = moldingMachineCapacityList.stream().filter(moldingMachineCapacity -> moldingMachineCapacity.isCrowdOutAllocation(proSize, tireFabricNumber)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(productionProSizeList)) {
            needAllocationRequirement.setFinishAllocation(true);
            return;
        }
        //设置寸口天产能 20251013 ZLT 成型硫化配比信息
        SizeCapacityAllocationUtils.setDayMaxProSizeCapacityInfo(productionProSizeList, proSize);
        //获取其中已分配寸口按寸口由小到大排序-->剩余天数多-->天产能高-->机器编号
        productionProSizeList.sort(SizeCapacityAllocationUtils.getCrowdOutSort());
        //成型产能-分配新的寸口，需要挤出
        productionProSizeList.stream().forEach(moldingMachineCapacity -> {
            SizeCapacityAllocationUtils.allocationByCrowdOut(moldingMachineCapacity, param, globalRequirementMap, needAllocationRequirement);
        });
    }

    /**
     * 剩余产能没有可一次性分配完的成型产能，需挤前面已分配的小寸口需求
     *
     * @param mouldMethodType            成型法
     * @param proSize                    寸口
     * @param tireFabricNumber           胎体层级
     * @param globalRequirementMap       所有需求信息
     * @param needAllocationRequirement  需分配需求信息
     * @param param                      寸口参数
     * @param globalCapacityMap          所有成型产能集合
     * @param moldingMachineCapacityList 寸口成型产能集合
     */
    private void allocationProductionCapacity(FormingMethodTypeEnum mouldMethodType, BigDecimal proSize, Integer tireFabricNumber, Map<String, SizeCapacityAllocationResultDto> globalRequirementMap, SizeCapacityAllocationResultDto needAllocationRequirement, SizeCapacityParamVo param, Map<String, BaseMoldingMachineInfoVo> globalCapacityMap, List<BaseMoldingMachineInfoVo> moldingMachineCapacityList) {
        //获取可产寸口需求的成型产能
        List<BaseMoldingMachineInfoVo> productionProSizeList;
        if (YesOrNoEnum.YES.equals(param.getOpenCrowdOut())) {
            productionProSizeList = moldingMachineCapacityList.stream().filter(moldingMachineCapacity -> moldingMachineCapacity.isCrowdOutAllocation(proSize, tireFabricNumber)).collect(Collectors.toList());
        } else {
            productionProSizeList = moldingMachineCapacityList.stream().filter(moldingMachineCapacity -> moldingMachineCapacity.isNoCrowdOutAllocation(proSize, tireFabricNumber)).collect(Collectors.toList());
        }
        if (CollectionUtils.isEmpty(productionProSizeList)) {
            needAllocationRequirement.setFinishAllocation(true);
            return;
        }
        List<BaseMoldingMachineInfoVo> emptyCapacityList = productionProSizeList.stream().filter(moldingMachineCapacity -> CollectionUtils.isEmpty(moldingMachineCapacity.getProSizeRequirementList())).collect(Collectors.toList());
        List<BaseMoldingMachineInfoVo> noEmptyCapacityList = productionProSizeList.stream().filter(moldingMachineCapacity -> !CollectionUtils.isEmpty(moldingMachineCapacity.getProSizeRequirementList())).collect(Collectors.toList());
        //先分配空成型产能
        if (!CollectionUtils.isEmpty(emptyCapacityList)) {
            //设置寸口天产能 20251013 ZLT 成型硫化配比信息
            SizeCapacityAllocationUtils.setDayMaxProSizeCapacityInfo(emptyCapacityList, proSize);
            //先从空余天数多的挤占
            emptyCapacityList.sort(SizeCapacityAllocationUtils.getMoldingMachineCapacitySort());
            //整台占用
            emptyCapacityList.stream().forEach(moldingMachineCapacity -> {
                SizeCapacityAllocationUtils.allocation(moldingMachineCapacity, param, needAllocationRequirement);
            });
        }
        //已经分配完成或是没有可分配的成型产能
        if (needAllocationRequirement.getLeftOverQty() <= BigDecimal.ZERO.intValue() || CollectionUtils.isEmpty(noEmptyCapacityList)) {
            needAllocationRequirement.setFinishAllocation(true);
            return;
        }
        //设置寸口天产能 20251013 ZLT 成型硫化配比信息
        SizeCapacityAllocationUtils.setDayMaxProSizeCapacityInfo(noEmptyCapacityList, proSize);
        //获取其中已分配寸口按寸口由小到大排序-->剩余天数多-->天产能高-->机器编号
        noEmptyCapacityList.sort(SizeCapacityAllocationUtils.getCrowdOutSort());
        //成型产能-分配新的寸口，需要挤出
        noEmptyCapacityList.stream().forEach(moldingMachineCapacity -> {
            SizeCapacityAllocationUtils.allocationByCrowdOut(moldingMachineCapacity, param, globalRequirementMap, needAllocationRequirement);
        });
    }

}