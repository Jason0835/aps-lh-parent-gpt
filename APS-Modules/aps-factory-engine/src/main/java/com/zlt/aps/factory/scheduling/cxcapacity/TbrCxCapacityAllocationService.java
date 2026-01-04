package com.zlt.aps.factory.scheduling.cxcapacity;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.Constant;
import com.tlt.aps.enums.ProductTypeEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.exception.BusinessException;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.*;
import com.zlt.aps.factory.domain.vo.*;
import com.zlt.aps.factory.enums.MouldRelationTypeEnum;
import com.zlt.aps.factory.handler.ContinueSkuCalculator;
import com.zlt.aps.factory.handler.GroupProductionConversionHandler;
import com.zlt.aps.factory.handler.MouldProductionResultHandler;
import com.zlt.aps.factory.scheduling.AbstractProductionBusinessService;
import com.zlt.aps.factory.scheduling.BaseDataContainer;
import com.zlt.aps.factory.scheduling.ProductionContext;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import com.zlt.aps.factory.scheduling.matching.MatchingProductionHandler;
import com.zlt.aps.factory.service.ProductionSchedulingDataService;
import com.zlt.aps.factory.utils.ProductionCycleUtils;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.monthplan.api.domain.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 工厂TBR业务轮胎成型产能分配
 * 主要完成按结构进行成型产能分配
 * 1、按结构汇总净需求量，粗算结构所需成型机台数
 * 2、从上个月的月度排产计划，获取在产结构-即续作结构
 * 3、如果续作结构需求量减少，导致需要提前释放续作结构的成型机台数，则优先释放配比机台数多的
 * 4、续作结构排产完毕后，对续作结构中收尾的成型机台，进行反向查找下个结构(需成型机台剩余产能能满足结构净需求)
 * 5、对剩余还有需求量的结构，按结构优先级，挑选优先级最高的结构，匹配能分配的成型机台
 * 5.1、固定机台优先，是否零度结构 = 零度供料架
 * 5.2、成型机当前排产结构是否与挑选结构含有同规格(结构向下SKU只要有一个同规格则认为同规格)
 * 5.3、成型机当前排产结构是否与挑选结构含有同英寸(结构向下SKU只要有一个同英寸则认为同英寸)
 * 5.4、成型机当前排产结构是否与挑选结构的断面宽±10优先
 * 5.5、近1个月历史结构在期日期近的优先(最后一个排产日)
 * 5.6、近3个月历史结构在机次数多的优先(一个月算一次)
 *
 * @author
 */
@Slf4j
@Service(value = "tbrCxCapacityAllocationService")
public class TbrCxCapacityAllocationService extends AbstractProductionBusinessService {


    public TbrCxCapacityAllocationService(ProductionSchedulingDataService dataService) {
        super(dataService);
    }

    /**
     * 结构排产
     * 1、构建排产Tbr排产上下文(设置排产版本信息、构建排产周期信息)
     *
     * @param context 排产上下文
     * @param userObj 用户数据
     */
    @Override
    public void run(Context context, Object userObj) {
        //创建排产上下文
        TbrProductionContext productionContext = (TbrProductionContext) buildProductionContext(context);
        //todo 记录日志-开始进行成型产能分配-结构排产
        //获取排产计划信息
        List<MonthPlanProductionRequirePlanVo> requirePlanList = getDataService().getFactoryMonthPlanManufacturing(productionContext);
        if (CollectionUtils.isEmpty(requirePlanList)) {
            throw new BusinessException(I18nUtil.getMessage("alg.data.initCheck.initEmpty"));
        }
        //设置初始的排产量数据信息
        requirePlanList.forEach(singlePlan -> singlePlan.initProductionDataInfo());
        //初始排产需要的基础数据，成型、模具关系、计划初始库销比
        initProductionBaseData(productionContext, requirePlanList);
        //获取结构的硫化配比
        List<MonthPlanStructureLhRatioVo> structureLhRatioList = getLhRatioConfiguration(productionContext, requirePlanList);
        //结构模具分配配比
        List<MouldAllocationInfoVo> mouldAllocationInfoList = getDataService().getMouldAllocationInfo(productionContext);
        //todo 记录日志-粗算成型机台数
        //按结构分组，汇总结构净需求量，粗算需要的机台数
        Map<String, ProductionPlanGroupInfo> estimateGroupCxAllocationMap = ProductionPlanGroupInfo.statisticsAndEstimateCxAllocationByGroup(context, requirePlanList, structureLhRatioList);
        productionContext.setGroupProductionInfo(estimateGroupCxAllocationMap);
        //todo 记录日志 续作结构排产分配
        //获取上个月度的月度定稿排产计划，得到在产结构及结构在产成型机、在产SKU和SKU在产模具数
        Map<String, CxContinueInfoHelper> cxContinueInfoMap = getContinueInfo(context, structureLhRatioList);
        //汇总续作Sku信息
        statisticsGroupContinueInfo(productionContext, estimateGroupCxAllocationMap, cxContinueInfoMap);
        //先对续作结构进行成型机台分配-并记录在机结构的收尾匹配
        productionContext.setReverseFindSet(new HashSet<>());
        //todo 采用新的逻辑进行分配在机结构的在产机台
//        Map<String, CxMachineAllocationPlanHelper> continueAllocationMap = CxCapacityAllocationHandler.continueGroupPlanAllocation(productionContext, estimateGroupCxAllocationMap, cxContinueInfoMap);
        List<CxMachineAllocationPlanHelper> continueAllocationList = CxContinueGroupAllocationHandler.allocationContinueAndProductionContinue(productionContext, estimateGroupCxAllocationMap, cxContinueInfoMap);
        //对成型机台进行模拟模具排产
//        mouldProductionByCxMachine(productionContext, continueAllocationMap, cxContinueInfoMap, productionContext.getBaseDataContainer().getMouldShellMap());
        mouldProductionByContinueGroup(productionContext, estimateGroupCxAllocationMap, continueAllocationList, cxContinueInfoMap);
        //对收尾成型机台，反向匹配待排结构
        CxCapacityAllocationHandler.reverseMachineAllocation(productionContext, estimateGroupCxAllocationMap);
        //对还需排产结构，获取优先级最高的结构--结构新增
        addNewGroupPlanHandler(productionContext, estimateGroupCxAllocationMap);
        //todo 记录日志
        //保存结构成型排程结果
        List<MpStructureAllocation> allAllocationList = saveStructureInfo(productionContext);
        //第二轮排产
        resetBeforeFormalProduction(productionContext, estimateGroupCxAllocationMap, cxContinueInfoMap, allAllocationList);
        FormalProductionHandler.productionContinueGroup(productionContext, estimateGroupCxAllocationMap, cxContinueInfoMap);
        //最后搭配排产
        MatchingProductionHandler.matchingProduction(productionContext, estimateGroupCxAllocationMap, structureLhRatioList);

        //保存模具排产结果
        saveMouldProductionInfo(productionContext);
    }

    /**
     * 构建业务排产上下文
     *
     * @param context
     * @return
     */
    @Override
    protected Context buildProductionContext(Context context) {
        //全钢业务
        if (ProductTypeEnum.WHOLE_STEEL == context.getProductType()) {
            return buildTbrProductionContext(context);
        }
        //主要为-半钢业务
        return buildDefaultProductionContext(context);
    }

    /**
     * 排产前基础数据初始化
     *
     * @param productionContext 排产上下文
     * @param requirePlanList   排产计划
     */
    private void initProductionBaseData(TbrProductionContext productionContext, List<MonthPlanProductionRequirePlanVo> requirePlanList) {
        //获取排产参数设定
        ProductionCapacityParamConfiguration paramConfiguration = createParamConfiguration(productionContext);
        productionContext.getBaseDataContainer().setParamConfiguration(paramConfiguration);
        //特殊材料的胎胚配置信息
        specialMaterialInfoHandler(productionContext);
        // 超6个成品库存信息
        overSixMonthStockHandler(productionContext);
        //初始化库销比、标记是否按总需求排产
        initProductionRequirePlanInfo(productionContext, requirePlanList);
        //获取周期内的生产日历信息
        setMonthProductionDays(productionContext);
        //获取成型机台信息--日产信息
        Map<String, CxMachineBaseInfoVo> cxMachineBaseInfo = getDataService().getCxMachineBaseInfo(productionContext);
        productionContext.getBaseDataContainer().setCxMachineBaseInfo(cxMachineBaseInfo);
        //获取SKU模具配置信息
        Map<String, List<MonthPlanProductMouldInfoVo>> mouldRelationMap = getProductionMouldInfo(productionContext);
        Map<String, ProductionMouldInfoVo> mouldInfoMap = createProductionMouldInfo(productionContext, mouldRelationMap);
        productionContext.getBaseDataContainer().setMouldInfoMap(mouldInfoMap);
        productionContext.getBaseDataContainer().setSkuMouldRelationMap(mouldRelationMap);
        //获取模壳配置信息
        Map<String, MouldShellBaseInfoVo> mouldShellMap = getMouldShellInfo(productionContext);
        productionContext.getBaseDataContainer().setMouldShellMap(mouldShellMap);
        if (CollectionUtils.isEmpty(requirePlanList)) {
            return;
        }
        Set<String> isSetStructureNameSet = new HashSet<>();
        //根据计划，补充模具关系中的物料结构名
        requirePlanList.forEach(requirePlan -> {
            String materialDesc = requirePlan.getMaterialDesc();
            if (StringUtils.isBlank(materialDesc)) {
                return;
            }
            if (isSetStructureNameSet.contains(materialDesc)) {
                return;
            }
            isSetStructureNameSet.add(materialDesc);
            List<MonthPlanProductMouldInfoVo> mouldRelationList = mouldRelationMap.get(requirePlan.getMaterialDesc());
            if (CollectionUtils.isEmpty(mouldRelationList)) {
                return;
            }
            mouldRelationList.forEach(mouldRelation -> {
                mouldRelation.setStructureName(requirePlan.getStructureName());
            });
        });
    }

    /**
     * 加载超6个月的库存信息
     *
     * @param productionContext
     */
    private void overSixMonthStockHandler(TbrProductionContext productionContext) {
        List<MdmProductStock> stockList = getDataService().getMdmProductStock(productionContext);
        Map<String, Integer> overSixMonthStockMap = stockList.stream()
                .filter(s -> StringUtils.isNotEmpty(s.getMaterialDesc()))
                .collect(Collectors.groupingBy(MdmProductStock::getMaterialDesc,
                        Collectors.collectingAndThen(Collectors.toList(),
                                list -> list.stream().filter(s -> ApsConstant.TRUE.equals(s.getIsExceedSixMonth()))
                                        .collect(Collectors.summingInt(MdmProductStock::getStockQty)))));
        productionContext.setOverSixMonthStockMap(overSixMonthStockMap);
    }

    /**
     * 获取初始化业务的参数设定
     *
     * @param productionContext
     * @return
     */
    private ProductionCapacityParamConfiguration createParamConfiguration(TbrProductionContext productionContext) {
        ProductionCapacityParamConfiguration configuration = new ProductionCapacityParamConfiguration();
        List<String> paramCodeList = new ArrayList<>(64);
        //日排产相关
        paramCodeList.add(MonthPlanEnums.DAY_CHANGE_GROUP_COUNT.getCode());
        paramCodeList.add(MonthPlanEnums.CHANGE_MOULD_LH_MACHINE_NUMBER.getCode());
        paramCodeList.add(MonthPlanEnums.CHANGE_MOULD_FIRST_QTY.getCode());
        paramCodeList.add(MonthPlanEnums.CHANGE_TYPE_BLOCK_QTY_DIFF.getCode());
        paramCodeList.add(MonthPlanEnums.CHANGE_TYPE_BLOCK_QTY.getCode());
        paramCodeList.add(MonthPlanEnums.CHANGE_TYPE_BLOCK_MAX_QTY.getCode());
        paramCodeList.add(MonthPlanEnums.SINGLE_CX_EMBRYO_CODE_COUNT.getCode());
        paramCodeList.add(MonthPlanEnums.DAY_MAX_CAPACITY.getCode());
        paramCodeList.add(MonthPlanEnums.DAY_MIN_CAPACITY.getCode());
        //排产控制相关
        paramCodeList.add(MonthPlanEnums.SUM_PRODUCTION_QTY.getCode());
        paramCodeList.add(MonthPlanEnums.HEIGHT_DIFF_QTY.getCode());
        paramCodeList.add(MonthPlanEnums.MOULD_SECOND_PRODUCTION.getCode());
        paramCodeList.add(MonthPlanEnums.BOOST_AVERAGE_VALUE.getCode());
        paramCodeList.add(MonthPlanEnums.MAX_BOOST_DAY.getCode());
        paramCodeList.add(MonthPlanEnums.MIN_PRODUCTION_DAYS.getCode());
        paramCodeList.add(MonthPlanEnums.MIN_ALLOCATION_DAYS.getCode());
        paramCodeList.add(MonthPlanEnums.NO_CYCLE_PRODUCTION_MIN_LH_MACHINE_NUMBER.getCode());
        //其他
        paramCodeList.add(MonthPlanEnums.SECTION_WIDTH_DIFF_VALUE.getCode());
        //获取数据
        Map<String, Object> paramConfigurationMap = getDataService().getFactoryParamByCondition(productionContext, paramCodeList);
        if (CollectionUtils.isEmpty(paramConfigurationMap)) {
            return configuration;
        }
        //其它
        configuration.setSectionWidthDiffValue((Integer) paramConfigurationMap.get(MonthPlanEnums.SECTION_WIDTH_DIFF_VALUE.getCode()));
        //排产控制相关
        configuration.setMinProductionDays((Integer) paramConfigurationMap.get(MonthPlanEnums.MIN_PRODUCTION_DAYS.getCode()));
        configuration.setMinAllocationDays((Integer) paramConfigurationMap.get(MonthPlanEnums.MIN_ALLOCATION_DAYS.getCode()));
        configuration.setNoCycleProductionMinLhMachineNumber((Integer) paramConfigurationMap.get(MonthPlanEnums.NO_CYCLE_PRODUCTION_MIN_LH_MACHINE_NUMBER.getCode()));
        configuration.setMaxBoostDay((Integer) paramConfigurationMap.get(MonthPlanEnums.MAX_BOOST_DAY.getCode()));
        configuration.setBoostAverageValue((Integer) paramConfigurationMap.get(MonthPlanEnums.BOOST_AVERAGE_VALUE.getCode()));
        configuration.setMouldSecondProduction((Integer) paramConfigurationMap.get(MonthPlanEnums.MOULD_SECOND_PRODUCTION.getCode()));
        configuration.setHeightDiffQty((Integer) paramConfigurationMap.get(MonthPlanEnums.HEIGHT_DIFF_QTY.getCode()));
        configuration.setSumProductionQty((Integer) paramConfigurationMap.get(MonthPlanEnums.SUM_PRODUCTION_QTY.getCode()));
        //日排产相关
        configuration.setDayChangeGroupCount((Integer) paramConfigurationMap.get(MonthPlanEnums.DAY_CHANGE_GROUP_COUNT.getCode()));
        configuration.setChangeMouldLhMachineNumber((Integer) paramConfigurationMap.get(MonthPlanEnums.CHANGE_MOULD_LH_MACHINE_NUMBER.getCode()));
        configuration.setChangeMouldFirstQty((Integer) paramConfigurationMap.get(MonthPlanEnums.CHANGE_MOULD_FIRST_QTY.getCode()));
        configuration.setChangeTypeBlockQtyDiff((Integer) paramConfigurationMap.get(MonthPlanEnums.CHANGE_TYPE_BLOCK_QTY_DIFF.getCode()));
        configuration.setChangeTypeBlockQty((Integer) paramConfigurationMap.get(MonthPlanEnums.CHANGE_TYPE_BLOCK_QTY.getCode()));
        configuration.setChangeTypeBlockMaxQty((Integer) paramConfigurationMap.get(MonthPlanEnums.CHANGE_TYPE_BLOCK_MAX_QTY.getCode()));
        configuration.setSingleCxEmbryoCodeCount((Integer) paramConfigurationMap.get(MonthPlanEnums.SINGLE_CX_EMBRYO_CODE_COUNT.getCode()));
        configuration.setDayMaxCapacity((Integer) paramConfigurationMap.get(MonthPlanEnums.DAY_MAX_CAPACITY.getCode()));
        configuration.setDayMinCapacity((Integer) paramConfigurationMap.get(MonthPlanEnums.DAY_MIN_CAPACITY.getCode()));
        return configuration;
    }

    /**
     * 获取需要排产的SKU的模具配置信息
     * key = materialDesc: value = List<MonthPlanProductMouldInfoVo>
     *
     * @param productionContext
     * @return
     */
    private Map<String, List<MonthPlanProductMouldInfoVo>> getProductionMouldInfo(TbrProductionContext productionContext) {
        List<MonthPlanProductMouldInfoVo> allMouldRelationInfoList = new ArrayList<>();
        //已有模具的配置关系
        List<MonthPlanProductMouldInfoVo> productMouldInfoList = getDataService().getEnableProductionMouldInfo(productionContext);
        if (!CollectionUtils.isEmpty(productMouldInfoList)) {
            allMouldRelationInfoList.addAll(productMouldInfoList);
        }
        //新模具到货计划关系
        List<MonthPlanProductMouldInfoVo> mouldDeliveryList = getDataService().getEnableProductionMouldDeliveryInfo(productionContext);
        if (!CollectionUtils.isEmpty(mouldDeliveryList)) {
            allMouldRelationInfoList.addAll(mouldDeliveryList);
        }
        if (CollectionUtils.isEmpty(allMouldRelationInfoList)) {
            return Collections.emptyMap();
        }
        return allMouldRelationInfoList.stream().collect(Collectors.groupingBy(MonthPlanProductMouldInfoVo::getMaterialDesc));
    }

    /**
     * 根据排产信息，获取特殊原材料的配置信息
     * 包含：1、特殊原材料的胎胚
     * 2、特殊原材料的库存及可转化的轮胎条数
     *
     * @param productionContext 排产单位
     */
    private void specialMaterialInfoHandler(TbrProductionContext productionContext) {
        List<EmbryoSpecialMaterialInfoVo> specialMaterialInfoList = getDataService().getEmbryoSpecialMaterialInfo(productionContext);
        if (CollectionUtils.isEmpty(specialMaterialInfoList)) {
            return;
        }
        //转化胎胚号-特殊材料
        Map<String, Map<String, BigDecimal>> embryoSpecialMaterialMap = new HashMap<>();
        Map<String, SpecialMaterialInfoVo> specialMaterialInfoMap = new HashMap<>();
        Map<String, BigDecimal> specialMaterialMaxMap = new HashMap<>();
        Map<String, List<EmbryoSpecialMaterialInfoVo>> allSpecialMaterialMap = specialMaterialInfoList.stream().collect(Collectors.groupingBy(EmbryoSpecialMaterialInfoVo::getEmbryoCode));
        allSpecialMaterialMap.forEach((embryoCode, rawMaterialList) -> {
            if (CollectionUtils.isEmpty(rawMaterialList)) {
                return;
            }
            Map<String, BigDecimal> rawMaterialConfigurationMap = embryoSpecialMaterialMap.get(embryoCode);
            if (null == rawMaterialConfigurationMap) {
                rawMaterialConfigurationMap = new HashMap<>();
                embryoSpecialMaterialMap.put(embryoCode, rawMaterialConfigurationMap);
            }
            for (EmbryoSpecialMaterialInfoVo embryoSpecialMaterialInfo : rawMaterialList) {
                String specialMaterialCode = embryoSpecialMaterialInfo.getChildMaterialCode();
                if (StringUtils.isBlank(specialMaterialCode)) {
                    continue;
                }
                BigDecimal dosage = embryoSpecialMaterialInfo.getDosage();
                rawMaterialConfigurationMap.put(specialMaterialCode, dosage);
                //构建特殊原材料初始化信息
                if (!specialMaterialInfoMap.containsKey(specialMaterialCode)) {
                    specialMaterialInfoMap.put(specialMaterialCode, SpecialMaterialInfoVo.createInitInfo(specialMaterialCode, embryoSpecialMaterialInfo.getChildMaterialName()));
                }
                BigDecimal maxDosage = specialMaterialMaxMap.get(specialMaterialCode);
                if (null == maxDosage) {
                    specialMaterialMaxMap.put(specialMaterialCode, dosage);
                } else {
                    if (maxDosage.compareTo(dosage) < BigDecimal.ZERO.intValue()) {
                        specialMaterialMaxMap.put(specialMaterialCode, dosage);
                    }
                }
            }
        });
        productionContext.getBaseDataContainer().setEmbryoSpecialMaterialInfoMap(embryoSpecialMaterialMap);
        productionContext.setSpecialMaterialInfoMap(specialMaterialInfoMap);
        //同时获取特殊材料库存信息
        List<SpecialMaterialStockVo> specialMaterialStockList = getDataService().getSpecialMaterialStockInfo(productionContext);
        if (CollectionUtils.isEmpty(specialMaterialStockList)) {
            return;
        }
        //构建库存对应的可生产量
        specialMaterialStockList.forEach(specialMaterialStockInfo -> {
            String specialMaterialCode = specialMaterialStockInfo.getMaterialCode();
            if (StringUtils.isBlank(specialMaterialCode)) {
                return;
            }
            Long stock = specialMaterialStockInfo.getStock();
            if (!specialMaterialInfoMap.containsKey(specialMaterialCode)) {
                return;
            }
            BigDecimal maxDosage = specialMaterialMaxMap.get(specialMaterialCode);
            specialMaterialInfoMap.get(specialMaterialCode).addInventoryCapacity(stock, maxDosage);
        });
    }

    /**
     * 根据物料可用模具关系，构建排产信息
     *
     * @param productionContext   排产上下文
     * @param mouldAssociationMap sku模具关系(包含新模具到货计划)
     * @return
     */
    private Map<String, ProductionMouldInfoVo> createProductionMouldInfo(TbrProductionContext productionContext, Map<String, List<MonthPlanProductMouldInfoVo>> mouldAssociationMap) {
        if (CollectionUtils.isEmpty(mouldAssociationMap)) {
            return Collections.emptyMap();
        }
        Map<String, ProductionMouldInfoVo> mouldInfoMap = new HashMap<>();
        mouldAssociationMap.forEach((materialDesc, associationList) -> {
            if (CollectionUtils.isEmpty(associationList)) {
                return;
            }
            //关系信息
            associationList.forEach(associationInfo -> {
                String mouldCode = associationInfo.getMouldCode();
                if (StringUtils.isBlank(mouldCode)) {
                    return;
                }
                MouldRelationTypeEnum relationType = MouldRelationTypeEnum.getInstance(associationInfo.getRelationType());
                ProductionMouldInfoVo productionMouldInfo = mouldInfoMap.get(mouldCode);
                if (null == productionMouldInfo) {
                    productionMouldInfo = ProductionMouldInfoVo.createEmptyProductionMouldInfo(mouldCode, relationType);
                    if (null == productionMouldInfo) {
                        return;
                    }
                    //设置模具的可排产日集合
                    productionMouldInfo.setProductionDayInfo(productionContext, associationInfo.getBoardingDate());
                    mouldInfoMap.put(mouldCode, productionMouldInfo);
                }
                //加入关联关系
                productionMouldInfo.getAssociationMaterialSet().add(materialDesc);
            });
        });
        return mouldInfoMap;
    }

    /**
     * 获取模壳台账信息，并加入新模具到货的模壳默认无上限
     *
     * @param context
     * @return
     */
    private Map<String, MouldShellBaseInfoVo> getMouldShellInfo(Context context) {
        Map<String, MouldShellBaseInfoVo> mouldShellMap = new HashMap<>();
        MouldShellBaseInfoVo noLimit = MouldShellBaseInfoVo.createNoLimit(ProductionConstant.NEW_MOULD_DELIVERY_SHELL);
        mouldShellMap.put(noLimit.getMoldModelCode(), noLimit);
        List<MouldShellBaseInfoVo> mouldShellList = getDataService().getMouldShellInfo(context);
        if (CollectionUtils.isEmpty(mouldShellList)) {
            return mouldShellMap;
        }
        for (MouldShellBaseInfoVo mouldShell : mouldShellList) {
            mouldShellMap.put(mouldShell.getMoldModelCode(), mouldShell);
        }
        return mouldShellMap;
    }

    /**
     * 对在机结构进行新增Sku的模具排产
     *
     * @param context                排产上下文
     * @param allGroupPlanMap        所有分组排产计划
     * @param continueAllocationList 在机机台产能分配
     * @param allContinueMap         续作信息
     */
    private void mouldProductionByContinueGroup(Context context, Map<String, ProductionPlanGroupInfo> allGroupPlanMap, List<CxMachineAllocationPlanHelper> continueAllocationList, Map<String, CxContinueInfoHelper> allContinueMap) {
        if (CollectionUtils.isEmpty(allContinueMap)) {
            return;
        }
        Map<ProductionPlanGroupInfo, List<CxMachineAllocationPlanHelper>> groupPlanMap = continueAllocationList.stream().collect(Collectors.groupingBy(CxMachineAllocationPlanHelper::getProductionPlanInfo));
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Map<String, CxMachineBaseInfoVo> allCxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        //在机结构-在产机台限制
        allContinueMap.forEach((structureName, cxContinueInfo) -> {
            ProductionPlanGroupInfo groupPlanInfo = allGroupPlanMap.get(structureName);
            List<CxMachineAllocationPlanHelper> continueCxMachineAllocation = groupPlanMap.get(groupPlanInfo);
            if (CollectionUtils.isEmpty(continueCxMachineAllocation)) {
                return;
            }
            groupPlanInfo.buildDayProductionLimitInfoByContinue(context, continueCxMachineAllocation);
            //首先设置可排产的计划在本轮次可进行排产
            groupPlanInfo.setThisRoundCanProduction();
            //todo 在机结构-新增Sku模拟排产
            CxAddSkuProductionHandler.productionAddSkuByContinueCxMachine(context, groupPlanInfo);
            //再次设置可排产的计划在本轮次可进行排产
            groupPlanInfo.setThisRoundCanProduction();
            //todo 处理需要提前收尾(需要调整到成型机台下的收尾点，包含成型机台最后一个配置的分配信息和成型机台剩余时间调整)
            //设置收尾机台
            continueCxMachineAllocation.forEach(cxMachineAllocation -> {
                String cxMachineCode = cxMachineAllocation.getCxMachineCode();
                CxMachineBaseInfoVo machineInfo = allCxMachineInfo.get(cxMachineCode);
                Integer newRemainingDays = machineInfo.getRemainingDays();
                //加入收尾匹配
                if (newRemainingDays > BigDecimal.ZERO.intValue()) {
                    productionContext.addReverseMachine(machineInfo.getCxMachineCode());
                }
            });
        });
    }

    /**
     * 对在机结构进行模具排产
     *
     * @param context               排产上下文
     * @param continueAllocationMap 在机结构排产分配信息
     * @param cxContinueInfoMap     在机结构续作信息
     * @param mouldShellMap         模壳台账信息
     */
    @Deprecated
    private void mouldProductionByCxMachine(Context context, Map<String, CxMachineAllocationPlanHelper> continueAllocationMap, Map<String, CxContinueInfoHelper> cxContinueInfoMap, Map<String, MouldShellBaseInfoVo> mouldShellMap) {
        if (CollectionUtils.isEmpty(continueAllocationMap)) {
            return;
        }
        //todo 降膜排产？
        Map<String, List<MonthPlanProductMouldInfoVo>> mouldInfoMap = ((TbrProductionContext) context).getBaseDataContainer().getSkuMouldRelationMap();
        continueAllocationMap.forEach((cxMachineCode, productionGroupPlan) -> {
            String structureName = productionGroupPlan.getProductionPlanInfo().getGroupName();
            CxContinueInfoHelper cxContinueInfoHelper = cxContinueInfoMap.get(structureName);
            if (null == cxContinueInfoHelper) {
                //todo 记录日志
                return;
            }
            Map<String, CxContinueProductInfoHelper> continueSkuMap = cxContinueInfoHelper.getCxMachineGroup().get(cxMachineCode);
            if (CollectionUtils.isEmpty(continueSkuMap)) {
                //todo 记录日志
                return;
            }
            //在机结构在机机台排产
            CxMouldProductionHandler.continueGroupPlanMouldProduction(context, cxMachineCode, productionGroupPlan, cxContinueInfoHelper, mouldInfoMap, mouldShellMap);
        });
    }

    /**
     * 对还需排产的结构，获取优先级最高的结构进行机台匹配排产
     *
     * @param context                      排产上下文
     * @param estimateGroupCxAllocationMap 分组计划需求量
     */
    private void addNewGroupPlanHandler(Context context, Map<String, ProductionPlanGroupInfo> estimateGroupCxAllocationMap) {
        ProductionPlanGroupInfo addNewGroupPlan = CxCapacityAllocationHandler.getInsertNewGroupPlan(context, estimateGroupCxAllocationMap);
        if (null == addNewGroupPlan) {
            //todo 记录日志
            return;
        }
        //对挑选出的机构，匹配还有排产量的成型机台
        CxMachineBaseInfoVo selectedCxMachine = CxCapacityAllocationHandler.selectedCxMachine(context, addNewGroupPlan);
        if (null == selectedCxMachine) {
            //todo 记录日志
            //todo 结构标记不可排产
            return;
        }
        //分配产能
        ProductGroupCxCapacityInfo lhRatioInfo = addNewGroupPlan.getLhRatioByCxMachine(selectedCxMachine);
        //selectedCxMachine.getRatio()
        Integer lhRatio = lhRatioInfo.getMaxLhMachineCount();
        Integer remainingDays = selectedCxMachine.getRemainingDays();
        //todo 判断成型鼓是否符合条件
        Integer needAllocationDays = addNewGroupPlan.calculateNeedDays(lhRatio);
        Integer realAllocationDays = Math.min(remainingDays, needAllocationDays);
        //更新剩余天数
        Integer leftOver = remainingDays - realAllocationDays;
        selectedCxMachine.setRemainingDays(leftOver);
        List<CxMachineAllocationPlanHelper> allocationList = selectedCxMachine.getAllocationList();
        CxMachineAllocationPlanHelper lastHelper = allocationList.get(allocationList.size() - BigDecimal.ONE.intValue());
        Integer startDay = lastHelper.getEndDay() + BigDecimal.ONE.intValue();
        CxMachineAllocationPlanHelper addHelper = CxCapacityAllocationHandler.createAllocationPlanHelper(selectedCxMachine, lhRatioInfo, addNewGroupPlan, null, needAllocationDays, startDay, context.getMonthDays());
        selectedCxMachine.addAllocationPlanInfo(addHelper);
        //TODO 对成型机台进行模拟模具排产
        CxMouldProductionHandler.noContinueGroupPlanMouldProduction(context, selectedCxMachine.getCxMachineCode(), addHelper);
        //反向机台匹配结构计划
        if (leftOver > BigDecimal.ZERO.intValue()) {
            CxCapacityAllocationHandler.selectedGroupPlanByCxMachine(context, estimateGroupCxAllocationMap, selectedCxMachine);
        }
        //下一新增结构
        addNewGroupPlanHandler(context, estimateGroupCxAllocationMap);
    }

    /**
     * 根据成型信息，得到结构排产结果
     * 即结构转产信息
     *
     * @param productionContext
     */
    private List<MpStructureAllocation> saveStructureInfo(TbrProductionContext productionContext) {
        List<MpStructureAllocation> allAllocationList = GroupProductionConversionHandler.getFinalResult(productionContext);
        if (CollectionUtils.isEmpty(allAllocationList)) {
            return Collections.emptyList();
        }
        getDataService().saveGroupConversionResult(allAllocationList);
        return allAllocationList;
    }

    /**
     * 在正式排产前进行重置数据处理
     *
     * @param context            排产上下文
     * @param allGroupPlanInfo   所有分组计划对象
     * @param allContinueSkuInfo 所有续作计划信息
     * @param allAllocationList  分组转产配置
     */
    private void resetBeforeFormalProduction(Context context, Map<String, ProductionPlanGroupInfo> allGroupPlanInfo, Map<String, CxContinueInfoHelper> allContinueSkuInfo, List<MpStructureAllocation> allAllocationList) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        //根据分组转产配置，重新构建分组的限制信息
        allGroupPlanInfo.forEach((groupName, groupProductionInfo) -> {
            List<MpStructureAllocation> groupAllocationList;
            if (CollectionUtils.isEmpty(allAllocationList)) {
                groupAllocationList = new ArrayList<>();
            } else {
                groupAllocationList = allAllocationList.stream().filter(singleAllocation -> groupName.equals(singleAllocation.getStructureName())).collect(Collectors.toList());
            }
            groupProductionInfo.buildDayProductionLimitInfoByStructureAllocation(context, groupAllocationList);
        });
        //处理计划的待排产量及排产标记重置
        Map<Long, MonthPlanProductionRequirePlanVo> allSinglePlanMap = productionContext.getAllProductionPlan();
        if (!CollectionUtils.isEmpty(allSinglePlanMap)) {
            allSinglePlanMap.forEach((monthPlanId, singlePlan) -> singlePlan.resetProductionDataInfo());
        }
        //重新构建模具排产信息，全部清空
        Map<String, ProductionMouldInfoVo> allMouldInfoMap = productionContext.getBaseDataContainer().getMouldInfoMap();
        if (!CollectionUtils.isEmpty(allMouldInfoMap)) {
            allMouldInfoMap.forEach((mouldCode, singleMouldInfo) -> {
                singleMouldInfo.setFinishDaySet(new HashSet<>());
                singleMouldInfo.setDayProductionInfo(new HashMap<>());
            });
        }
    }

    /**
     * 根据模具信息，保存模具排产结果
     *
     * @param productionContext
     */
    private void saveMouldProductionInfo(TbrProductionContext productionContext) {
        //模具排产明细日志
        List<FactoryMonthPlanMouldDayDetail> detailLogList = MouldProductionResultHandler.getMouldProductionResult(productionContext);
        if (CollectionUtils.isEmpty(detailLogList)) {
            return;
        }
        getDataService().saveMouldProductionDetailLog(detailLogList);
        //构建未排信息

        //构建汇总的排产结果
        List<FactoryMonthPlanMouldDayResult> dayResultList = MouldProductionResultHandler.getSummaryBySkuResult(detailLogList, productionContext);
        getDataService().saveMouldProductionDetailLog(detailLogList);
    }

    /**
     * 初始化排产计划，主要进行按sku分组和初始化库销比
     *
     * @param productionContext 排产上下文
     * @param requirePlanList   需求计划列表
     */
    private void initProductionRequirePlanInfo(TbrProductionContext productionContext, List<MonthPlanProductionRequirePlanVo> requirePlanList) {
        productionContext.setAllProductionPlan(new HashMap<>());
        productionContext.setAllSkuProductionPlan(new HashMap<>());
        productionContext.setSkuPlannedQtyMap(new HashMap<>());
        productionContext.setSkuWastageQtyMap(new HashMap<>());
        if (CollectionUtils.isEmpty(requirePlanList)) {
            return;
        }
        //按计划Id分组，全局存储
        productionContext.setAllProductionPlan(requirePlanList.stream().collect(Collectors.toMap(MonthPlanProductionRequirePlanVo::getMonthPlanId, Function.identity())));
        //按物料描述分组，全局存储
        Map<String, List<MonthPlanProductionRequirePlanVo>> skuRequirePlanMap = requirePlanList.stream().collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getMaterialDesc));
        productionContext.setAllSkuProductionPlan(skuRequirePlanMap);
        //已排产量和损耗量为零
        ProductionCapacityParamConfiguration param = productionContext.getBaseDataContainer().getParamConfiguration();
        skuRequirePlanMap.forEach((materialDesc, productionPlanList) -> {
            productionContext.getSkuPlannedQtyMap().put(materialDesc, BigDecimal.ZERO.intValue());
            productionContext.getSkuWastageQtyMap().put(materialDesc, BigDecimal.ZERO.intValue());
            if (CollectionUtils.isEmpty(productionPlanList)) {
                return;
            }
            //是否含有特殊原材料的SKU 是否按总需求排产-默认 = 否
            productionPlanList.forEach(requirePlan -> {
                requirePlan.setIsProductionBySum(Constant.FALSE);
                requirePlan.setIsSpecialMaterials(YesOrNoEnum.NO.getCode());
                if (productionContext.getBaseDataContainer().getEmbryoSpecialMaterialInfoMap().containsKey(requirePlan.getEmbryoCode())) {
                    requirePlan.setIsSpecialMaterials(YesOrNoEnum.YES.getCode());
                }
            });
            List<MonthPlanProductionRequirePlanVo> effectiveList = productionPlanList.stream().filter(plan -> plan.hasProduction()).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(effectiveList)) {
                return;
            }
            //总需求量小于一定值
            Long sumProductionQty = effectiveList.stream().mapToLong(MonthPlanProductionRequirePlanVo::getNetQty).sum();
            if (sumProductionQty <= param.getSumProductionQty()) {
                productionPlanList.forEach(requirePlan -> requirePlan.setIsProductionBySum(Constant.TRUE));
            }
            //总需求量与高优先级量差值小于一定值
            Long sumHeightProductionQty = effectiveList.stream().mapToLong(MonthPlanProductionRequirePlanVo::getHeightQty).sum();
            if (sumProductionQty - sumHeightProductionQty <= param.getHeightDiffQty()) {
                productionPlanList.forEach(requirePlan -> requirePlan.setIsProductionBySum(Constant.TRUE));
            }
        });
        //计算初始的库销比
        requirePlanList.forEach(requirePlan -> requirePlan.calculateInventorySalesRatio(BigDecimal.ZERO.intValue()));
    }

    /**
     * 设置工厂的排产日信息
     * 包含 停产日及开停产的产能比例
     * t_mdm_work_calendar
     *
     * @param context
     */
    private void setMonthProductionDays(Context context) {
        List<ProductionDayInfoVo> productionDayInfoList = getDataService().getProductCalendar(context);
        if (CollectionUtils.isEmpty(productionDayInfoList)) {
            context.setStopDays(Collections.emptySet());
            return;
        }
        //排产开始日
        Date productionStartDate = context.getProductionStartDate();
        //开产比例设置
        Map<Integer, Integer> startProductionRatioMap = new HashMap<>(context.getMonthDays());
        List<ProductionDayInfoVo> startProductionDays = productionDayInfoList.stream().filter(productionDayInfo -> YesOrNoEnum.YES.getCode().equals(productionDayInfo.getDayFlag())).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(startProductionDays)) {
            startProductionDays.forEach(startProductionInfo -> {
                Date startProduction = startProductionInfo.getProductionDate();
                Integer startDay = com.zlt.aps.factory.utils.DateUtils.getIntervalDays(productionStartDate, startProduction);
                startProductionRatioMap.put(startDay, startProductionInfo.getRate());
            });
        }
        context.setCapacityRatioMap(startProductionRatioMap);
        //停产设置
        List<ProductionDayInfoVo> stopDays = productionDayInfoList.stream().filter(productionDayInfo -> YesOrNoEnum.NO.getCode().equals(productionDayInfo.getDayFlag())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(stopDays)) {
            context.setStopDays(Collections.emptySet());
            return;
        }
        Set<Integer> stopDaySet = new HashSet<>(context.getMonthDays());
        stopDays.forEach(stopProductionInfo -> {
            Date stopProduction = stopProductionInfo.getProductionDate();
            Integer stopDay = com.zlt.aps.factory.utils.DateUtils.getIntervalDays(productionStartDate, stopProduction);
            stopDaySet.add(stopDay);
        });
        context.setStopDays(stopDaySet);
    }

    /**
     * 获取计划对应结构成型硫化配比信息
     * 计划内的结构
     *
     * @param context         排产上下文
     * @param requirePlanList 需求计划信息
     * @return
     */
    private List<MonthPlanStructureLhRatioVo> getLhRatioConfiguration(Context context, List<MonthPlanProductionRequirePlanVo> requirePlanList) {
        if (CollectionUtils.isEmpty(requirePlanList)) {
            return Collections.emptyList();
        }
        //提取结构查询条件
        Set<String> structureNameMap = requirePlanList.stream().map(MonthPlanProductionRequirePlanVo::getStructureName).collect(Collectors.toSet());
        List<String> structureNameList = new ArrayList<>(structureNameMap);
        List<MonthPlanStructureLhRatioVo> structureLhRatioList = getDataService().getLhRatioInfo(context, structureNameList);
        if (CollectionUtils.isEmpty(structureLhRatioList)) {
            return Collections.emptyList();
        }
        //周期结构硫化配比
        List<CycleStructureMinLhMachineQtyVo> cycleStructureMinLhRatioList = getDataService().getCycleLhRatioInfo(context);
        Map<String, Integer> cycleStructureMinLhRatioMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(cycleStructureMinLhRatioList)) {
            cycleStructureMinLhRatioList.forEach(cycleStructureMinLhRatio -> {
                cycleStructureMinLhRatioMap.put(cycleStructureMinLhRatio.getStructureName(), null == cycleStructureMinLhRatio.getMonthMinLhMachineQty() ? cycleStructureMinLhRatio.getMinLhMachineQty() : cycleStructureMinLhRatio.getMonthMinLhMachineQty());
            });
        }
        //常规结构的最低硫化配比
        Integer defaultMinLhRatio = ((TbrProductionContext) context).getBaseDataContainer().getParamConfiguration().getNoCycleProductionMinLhMachineNumber();
        structureLhRatioList.forEach(structureLhRatio -> {
            String structureName = structureLhRatio.getStructureName();
            structureLhRatio.setLhMachineMinQty(defaultMinLhRatio);
            //如果是周期，则换成周期
            if (cycleStructureMinLhRatioMap.containsKey(structureName)) {
                structureLhRatio.setLhMachineMinQty(cycleStructureMinLhRatioMap.get(structureName));
                return;
            }
        });
        return structureLhRatioList;
    }

    /**
     * 获取续作排产信息
     * 续作的分组信息(结构)，对应的成型产能机台和续作的SKU，使用模具-硫化机台数
     * key = structureName(TBR)
     * CxContinueInfoHelper.cxMachineGroup = { key = cxMachineCode : value = {key = materialDesc : value = 硫化机台数(模具数)}}
     *
     * @param context              排产上下文
     * @param structureLhRatioList 成型结构硫化配比信息
     * @return
     */
    private Map<String, CxContinueInfoHelper> getContinueInfo(Context context, List<MonthPlanStructureLhRatioVo> structureLhRatioList) {
        //获取前一个月的排产版本信息
        String factoryCode = context.getFactoryCode();
        LocalDate previousMonth = context.getPreviousMonth();
        Integer year = previousMonth.getYear();
        Integer month = previousMonth.getMonthValue();
        MpFactoryProductionVersion previousVersion = getDataService().getFinalVersion(factoryCode, year, month);
        if (null == previousVersion) {
            return Collections.emptyMap();
        }
        //根据排产版本信息，确认最后一天的排产SKU信息(包含结构、SKU、使用模具数)
        Context previousContext = new Context();
        previousContext.setFactoryCode(factoryCode);
        previousContext.setYear(year);
        previousContext.setMonth(month);
        previousContext.setProductionStartDate(previousVersion.getProductionStartDate());
        previousContext.setProductionEndDate(previousVersion.getProductionEndDate());
        //获取上个排产周期的工作日历
        List<ProductionDayInfoVo> previousProductionDayInfo = getDataService().getProductCalendar(previousContext);
        //确认最后排产日
        Integer lastDay = ProductionCycleUtils.getLastProductionDay(previousVersion, previousProductionDayInfo);
        if (lastDay <= BigDecimal.ZERO.intValue()) {
            return Collections.emptyMap();
        }
        //获取上个排产周期最后排产日的排产信息
        List<ContinueProductInfo> continueProductionInfoList = getDataService().getContinueProductionInfo(factoryCode, year, month, lastDay);
        //获取续作结构--结构转产表
        Map<String, Set<String>> continueGroupInfo = getContinueGroupInfo(factoryCode, year, month, lastDay);
        //构建续作分组信息(TBR为结构，PCR为英寸)
        Map<String, CxMachineBaseInfoVo> cxMachineBaseInfo = ((TbrProductionContext) context).getBaseDataContainer().getCxMachineBaseInfo();
        setContinueGroupByProduct(continueProductionInfoList, continueGroupInfo);
        return CxContinueInfoHelper.createGroupInfo(continueProductionInfoList, cxMachineBaseInfo, structureLhRatioList);
    }

    /**
     * 汇总续作信息
     * 在机结构-续作Sku有排产量的胎胚和使用模具数
     * 机构计划-在产成型机信息初始化
     *
     * @param allGroupPlanMap      分组计划信息
     * @param allCxContinueInfoMap 续作分组信息
     */
    private void statisticsGroupContinueInfo(Context context, Map<String, ProductionPlanGroupInfo> allGroupPlanMap, Map<String, CxContinueInfoHelper> allCxContinueInfoMap) {
        if (CollectionUtils.isEmpty(allGroupPlanMap) || CollectionUtils.isEmpty(allGroupPlanMap)) {
            return;
        }
        allGroupPlanMap.forEach((structureName, groupPlanInfo) -> {
            CxContinueInfoHelper cxContinueInfoHelper = allCxContinueInfoMap.get(structureName);
            if (null == cxContinueInfoHelper) {
                return;
            }
            ContinueSkuCalculator.setContinueSkuPlanDemandQty(groupPlanInfo, cxContinueInfoHelper);
            ContinueSkuCalculator.initContinueCxMachineLimit(context, groupPlanInfo, cxContinueInfoHelper);
        });
    }

    /**
     * 获取工厂年份-月份的最后一天排产的分组信息
     * TBR-结构
     * PCR-英寸、寸别、寸口
     *
     * @param factoryCode 工厂
     * @param year        年份
     * @param month       月份
     * @param lastDay     最后一天
     * @return
     */
    private Map<String, Set<String>> getContinueGroupInfo(String factoryCode, Integer year, Integer month, Integer lastDay) {
        List<ContinueGroupInfo> continueGroupInfoList = getDataService().getContinueGroupInfo(factoryCode, year, month, lastDay);
        if (!CollectionUtils.isEmpty(continueGroupInfoList)) {
            return Collections.emptyMap();
        }
        Map<String, List<ContinueGroupInfo>> continueGroupInfoMap = continueGroupInfoList.stream().collect(Collectors.groupingBy(ContinueGroupInfo::getGroupName));
        Map<String, Set<String>> continueGroupInfo = new HashMap<>();
        continueGroupInfoMap.forEach((groupName, continueCxMachineInfoList) -> {
            if (CollectionUtils.isEmpty(continueCxMachineInfoList)) {
                return;
            }
            Set<String> continueCxMachineSet = continueCxMachineInfoList.stream().map(ContinueGroupInfo::getCxMachineCode).collect(Collectors.toSet());
            continueGroupInfo.put(groupName, continueCxMachineSet);
        });
        return continueGroupInfo;
    }

    /**
     * 对续作的Sku设置分组信息
     * 按分组名匹配
     * TRB为结构
     * PCR为英寸
     *
     * @param continueSkuInfo   续作的Sku规格
     * @param continueGroupInfo 续作的分组信息-含机台
     */
    private void setContinueGroupByProduct(List<ContinueProductInfo> continueSkuInfo, Map<String, Set<String>> continueGroupInfo) {
        if (CollectionUtils.isEmpty(continueGroupInfo) || CollectionUtils.isEmpty(continueGroupInfo)) {
            return;
        }
        continueSkuInfo.forEach(continueSku -> {
            String groupName = continueSku.getGroupName();
            if (StringUtils.isBlank(groupName)) {
                return;
            }
            continueSku.setContinueCxMachineCodeSet(continueGroupInfo.get(groupName));
        });
    }

    /**
     * 构建全钢排产上下文
     * 设置排产版本号：为空时生产排产版本号
     * 设置操作批次号
     * 设置日志记录器实例
     * 设置排产周期信息
     *
     * @param context
     * @return
     */
    private TbrProductionContext buildTbrProductionContext(Context context) {
        TbrProductionContext productionContext = new TbrProductionContext();
        BeanUtils.copyProperties(context, productionContext);
        //基础数据容器存储
        productionContext.setBaseDataContainer(new BaseDataContainer());
        context.setProductionVersion(productionContext.createNewProductionVersion());
        context.setOperationWorkNo(productionContext.createNewOperationWorkNo());
        StringBuilder logBuilder = new StringBuilder();
        context.setLogBuilder(logBuilder);
        productionContext.setLogBuilder(logBuilder);
        setProductionCycleInfo(productionContext);
        return productionContext;
    }

    /**
     * 构建默认的排产上下文
     * 主要为半钢业务
     * 设置排产版本号：为空时生产排产版本号
     * 设置操作批次号
     * 设置日志记录器实例
     * 设置排产周期信息
     *
     * @param context
     * @return
     */
    private ProductionContext buildDefaultProductionContext(Context context) {
        ProductionContext productionContext = new ProductionContext();
        BeanUtils.copyProperties(context, productionContext);
        context.setProductionVersion(productionContext.createNewProductionVersion());
        context.setOperationWorkNo(productionContext.createNewOperationWorkNo());
        StringBuilder logBuilder = new StringBuilder();
        context.setLogBuilder(logBuilder);
        productionContext.setLogBuilder(logBuilder);
        setProductionCycleInfo(productionContext);
        return productionContext;
    }

    /**
     * 设置排产周期信息等信息
     * 根据排产版本号，得到排产版本包含排产周期，
     *
     * @param context
     */
    private void setProductionCycleInfo(Context context) {
        MpFactoryProductionVersion productionVersion = getDataService().getFactoryMonthPlanVersion(context);
        if (null == productionVersion) {
            return;
        }
        Date productionStartDate = productionVersion.getProductionStartDate();
        context.setProductionStartDate(productionStartDate);
        context.setStartDay(com.zlt.aps.factory.utils.DateUtils.getDaysByMonth(productionStartDate));
        context.setProductionEndDate(productionVersion.getProductionEndDate());
    }
}
