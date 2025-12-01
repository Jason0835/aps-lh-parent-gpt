package com.zlt.aps.factory.scheduling.moulding.group;

import com.tlt.aps.enums.ConstructionStageEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.ProductProductionHelper;
import com.zlt.aps.factory.domain.vo.*;
import com.zlt.aps.factory.mapper.FactoryMatchingProductionMapper;
import com.zlt.aps.factory.scheduling.AbstractProductionBusinessService;
import com.zlt.aps.factory.scheduling.ProductionContext;
import com.zlt.aps.factory.service.ProductionSchedulingDataService;
import com.zlt.aps.factory.utils.*;
import com.zlt.aps.monthplan.api.domain.entity.FactoryNoProduction;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductConstruction;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 排产完成后，搭配排产
 *
 * @author
 */
@Slf4j
@Service(value = "matchingProduction")
public class MatchingProductProductionService extends AbstractProductionBusinessService {

    private final FactoryMatchingProductionMapper factoryMatchingProductionMapper;

    public MatchingProductProductionService(ProductionSchedulingDataService dataService,
                                            FactoryMatchingProductionMapper factoryMatchingProductionMapper) {
        super(dataService);
        this.factoryMatchingProductionMapper = factoryMatchingProductionMapper;
    }

    @Override
    public void run(Context context, Object userObj) {
        ProductionContext productionContext = (ProductionContext) context;
        //排产流程日志记录 "=====分厂%s, 计划年月：%d-%d, 计划版本：%s，搭配排产开始===="
        ProductionLogUtils.addMatchingProductionLog(productionContext);
        List<MatchingProductionRequireVo> matchingProductionRequireList = getEnableRequireList(productionContext);
        if (CollectionUtils.isEmpty(matchingProductionRequireList)) {
            ProductionLogUtils.addNoNeedMatchingProductionLog(productionContext);
            return;
        }
        List<MatchingProductionConfigurationVo> enableMatchingProductList = getEnableMatchingProductList(productionContext);
        if (CollectionUtils.isEmpty(enableMatchingProductList)) {
            ProductionLogUtils.addNoMatchingProductDataLog(productionContext);
            return;
        }
        Map<String, List<MatchingProductionConfigurationVo>> matchingProductGroupMap = enableMatchingProductList.stream().collect(Collectors.groupingBy(MatchingProductionConfigurationVo::getGroupKey));
        //胎体层级--先多层级再单层级
        List<Integer> tireFabricNumberList = new ArrayList<>(matchingProductionRequireList.stream().map(MatchingProductionRequireVo::getTireFabricNumber).collect(Collectors.toSet()));
        tireFabricNumberList.sort(Comparator.comparing(Integer::intValue, Comparator.reverseOrder()));
        Map<Integer, List<MatchingProductionRequireVo>> tireFabricNumGroup = matchingProductionRequireList.stream().collect(Collectors.groupingBy(MatchingProductionRequireVo::getTireFabricNumber));
        tireFabricNumberList.stream().forEach(tireFabricNumber -> {
            List<MatchingProductionRequireVo> matchingRequireList = tireFabricNumGroup.get(tireFabricNumber);
            if (CollectionUtils.isEmpty(matchingRequireList)) {
                return;
            }
            //成型、寸口需求
            matchingRequireList.stream().forEach(matchingProductionRequire -> {
                List<MatchingProductionConfigurationVo> enableMatchingRequireList = matchingProductGroupMap.get(matchingProductionRequire.getGroupKey());
                if (CollectionUtils.isEmpty(enableMatchingRequireList)) {
                    return;
                }
                //开始进行搭配排产
                productionMatchingRequire(productionContext, enableMatchingRequireList, matchingProductionRequire);
            });
        });
    }

    /**
     * 获取需要搭配补量的需求信息
     *
     * @param productionContext 排产上下文
     * @return
     */
    private List<MatchingProductionRequireVo> getEnableRequireList(ProductionContext productionContext) {
        //日产能配置信息
        Map<Integer, Map<String, Long>> daySizeCapacityMap = productionContext.getDaySizeCapacityMap();
        //提取sizeCapacityKey信息
        Set<String> sizeCapacitySet = new HashSet<>();
        daySizeCapacityMap.entrySet().stream().forEach(entry -> {
            Map<String, Long> capacityMap = entry.getValue();
            if (CollectionUtils.isEmpty(capacityMap)) {
                return;
            }
            sizeCapacitySet.addAll(capacityMap.keySet());
        });
        if (CollectionUtils.isEmpty(sizeCapacitySet)) {
            return Collections.emptyList();
        }
        //日信息
        Set<Integer> daySet = daySizeCapacityMap.keySet();
        //日排产信息
        Map<Integer, Map<String, Long>> dayProductionQtyMap = productionContext.getDayProductionQtyMap();
        List<MatchingProductionRequireVo> matchingProductionRequireList = new ArrayList<>();
        sizeCapacitySet.stream().forEach(sizeCapacityKey -> {
            Map<Integer, Long> dayMatchingNumberMap = new HashMap<>();
            daySet.stream().forEach(day -> {
                Long capacityQty = daySizeCapacityMap.get(day).get(sizeCapacityKey);
                Long productionQty = dayProductionQtyMap.get(day).get(sizeCapacityKey);
                if (capacityQty <= productionQty) {
                    return;
                }
                dayMatchingNumberMap.put(day, capacityQty - productionQty);
            });
            matchingProductionRequireList.add(new MatchingProductionRequireVo(sizeCapacityKey, dayMatchingNumberMap));
        });
        return matchingProductionRequireList;
    }

    /**
     * 获取可搭配生产的配置信息
     *
     * @param productionContext
     * @return
     */
    private List<MatchingProductionConfigurationVo> getEnableMatchingProductList(ProductionContext productionContext) {
        String factoryCode = productionContext.getFactoryCode();
        Integer year = productionContext.getYear();
        Integer month = productionContext.getMonth();
        String monthPlanVersion = productionContext.getMonthPlanVersion();
        //获取可搭配排产的SKU信息
        List<MatchingProductionConfigurationVo> enableMatchingProductList = factoryMatchingProductionMapper.getMatchingConfiguration(factoryCode, year, month, monthPlanVersion);
        if (CollectionUtils.isEmpty(enableMatchingProductList)) {
            return Collections.emptyList();
        }
        //获取搭配排产新增的SKU-物料施工信息，确认施工阶段
        setAddConstructionInfo(productionContext, factoryCode, year, month, monthPlanVersion);
        //搭配需求--对应可用模具信息
        setProductRelationMouldInfo(productionContext, factoryCode, year, month, monthPlanVersion);
        //设置--对应生胎、硫化代号、成型法等信息
        List<MatchingProductionConfigurationVo> realEnableConfigurationList = setProductionNecessaryInfo(productionContext, enableMatchingProductList);
        return realEnableConfigurationList;
    }

    /**
     * 对需进行搭配排产的需求，
     * 从enableMatchingRequireList中挑选物料进行搭配排产
     *
     * @param productionContext         排产上下文
     * @param enableMatchingRequireList 可搭配排产的物料集合
     * @param needMatchingRequire       需进行搭配排产的需求
     */
    private void productionMatchingRequire(ProductionContext productionContext, List<MatchingProductionConfigurationVo> enableMatchingRequireList, MatchingProductionRequireVo needMatchingRequire) {
        if (CollectionUtils.isEmpty(enableMatchingRequireList)) {
            return;
        }
        List<MatchingProductionConfigurationVo> realEnableMatchingRequireList = enableMatchingRequireList.stream().filter(matchingRequire -> null != matchingRequire.getCuringTime()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(realEnableMatchingRequireList)) {
            return;
        }
        Map<Integer, Long> daySurplusInfo = needMatchingRequire.getDaySurplusInfo();
        //开始对物料进行搭配排产
        realEnableMatchingRequireList.stream().forEach(matchingProductInfo -> {
            String productCode = matchingProductInfo.getProductCode();
            BigDecimal curingTime = MatchingProductionUtils.getSingleCuringTime(matchingProductInfo, productionContext);
            Long singleMouldCapacity = MouldUtils.getSingleMouldCapacity(productionContext, curingTime);
            List<MouldInfoVO> enableMouldList = ProductionPlanUtils.getPlanMaxEnableMouldInfo(productCode, productionContext);
            if (CollectionUtils.isEmpty(enableMouldList)) {
                return;
            }
            daySurplusInfo.entrySet().forEach(entry -> {
                Integer day = entry.getKey();
                Long needProductionQty = entry.getValue();

            });

        });
    }

    /**
     * 设置搭配排产的施工信息，并更新施工缓存信息
     *
     * @param productionContext 排产上下文
     * @param factoryCode       分厂
     * @param year              年份
     * @param month             月份
     * @param monthPlanVersion  需求版本
     */
    private void setAddConstructionInfo(ProductionContext productionContext, String factoryCode, Integer year, Integer month, String monthPlanVersion) {
        List<MdmProductConstruction> productConstructionList = factoryMatchingProductionMapper.getConstructionByMatchingRequire(factoryCode, year, month, monthPlanVersion);
        if (CollectionUtils.isEmpty(productConstructionList)) {
            return;
        }
        Map<String, ConstructionStageEnum> constructionStageMap = new HashMap<>();
        Map<String, Map<String, ProductConstructionInfoVo>> constructionConfigurationMap = new HashMap<>();
        productConstructionList.stream().forEach(productConstruction -> {
            String productCode = productConstruction.getProductCode();
            String constructionCode = productConstruction.getConstructionCode();
            ConstructionStageEnum stage = ConstructionStageEnum.matchByConstructionCode(constructionCode);
            if (null != stage) {
                constructionStageMap.put(productCode, stage);
            }
            String specCode = productConstruction.getSpecCode();
            Map<String, ProductConstructionInfoVo> productConstructionConfigurationMap = constructionConfigurationMap.get(productCode);
            if (null == productConstructionConfigurationMap) {
                productConstructionConfigurationMap = new HashMap<>();
            }
            ProductConstructionInfoVo productConstructionInfo = productConstructionConfigurationMap.get(specCode);
            if (null != productConstructionInfo) {
                return;
            }
            productConstructionInfo = new ProductConstructionInfoVo();
            productConstructionInfo.setProductCode(productCode);
            productConstructionInfo.setConstructionCode(constructionCode);
            productConstructionInfo.setSpecCode(specCode);
            productConstructionInfo.setEmbryoCode(productConstruction.getEmbryoCode());
            productConstructionInfo.setMouldMethod(productConstruction.getMouldMethod());
            productConstructionInfo.setSummerCuringTime(productConstruction.getCuringTime());
            productConstructionInfo.setWinterCuringTime(productConstruction.getCuringTime2());
            productConstructionInfo.setMouldClampingPressure(productConstruction.getMouldClampingPressure());
            productConstructionInfo.setMoldCavity(productConstruction.getMoldCavity());
            productConstructionInfo.setConstructionStage(stage);
            productConstructionConfigurationMap.put(specCode, productConstructionInfo);
            constructionConfigurationMap.put(productCode, productConstructionConfigurationMap);
        });
        Map<String, ConstructionStageEnum> cacheConstructionStageMap = productionContext.getConstructionStageMap();
        constructionStageMap.entrySet().stream().forEach(constructionEntry -> {
            String productCode = constructionEntry.getKey();
            if (cacheConstructionStageMap.containsKey(productCode)) {
                return;
            }
            //新增的加入缓存
            cacheConstructionStageMap.put(productCode, constructionEntry.getValue());
        });
        Map<String, Map<String, ProductConstructionInfoVo>> cacheConstructionConfigurationMap = productionContext.getConstructionConfigurationMap();
        constructionConfigurationMap.entrySet().stream().forEach(constructionConfigurationEntry -> {
            String productCode = constructionConfigurationEntry.getKey();
            if (cacheConstructionConfigurationMap.containsKey(productCode)) {
                return;
            }
            cacheConstructionConfigurationMap.put(productCode, constructionConfigurationEntry.getValue());
        });
    }

    /**
     * 设置搭配生产需求product的模具信息
     *
     * @param productionContext 排产上下文
     * @param factoryCode       分厂
     * @param year              年份
     * @param month             月份
     * @param monthPlanVersion  需求版本
     */
    private void setProductRelationMouldInfo(ProductionContext productionContext, String factoryCode, Integer year, Integer month, String monthPlanVersion) {
        Date startDate = productionContext.getProductionStartDate();
        Date endDate = productionContext.getProductionEndDate();
        //搭配需求-->可用模具信息
        Map<String, MouldInfoVO> enableMap = new HashMap<>();
        //根据制造需求计划得到模具月度可用且本身可用列表--月度可用
        List<MouldInfoVO> monthEnableList = factoryMatchingProductionMapper.getMonthEnableMouldConfigurationByMatchingRequire(factoryCode, year, month, monthPlanVersion);
        if (!CollectionUtils.isEmpty(monthEnableList)) {
            monthEnableList.stream().forEach(monthEnable -> enableMap.put(monthEnable.getMouldCode(), monthEnable));
        }
        //根据制造需求计划得到模具维修返厂列表--月度配置有覆盖月度配置，没有则加入，防止月度配错
        List<MouldMaintenanceConfigurationVo> maintenanceConfigurationList = factoryMatchingProductionMapper.getMouldMaintenanceConfigurationByMatchingRequireDateRange(factoryCode, startDate, endDate, monthPlanVersion);
        List<MouldInfoVO> maintenanceEnableList = getAddMouldMaintenanceConfiguration(maintenanceConfigurationList, productionContext);
        if (!CollectionUtils.isEmpty(maintenanceEnableList)) {
            maintenanceEnableList.stream().forEach(returnEnable -> enableMap.put(returnEnable.getMouldCode(), returnEnable));
        }
        //物料配置的模具列表
        List<ProductMouldConfigurationVo> productMouldConfigurationList = factoryMatchingProductionMapper.getProductionMouldRelationByMatchingRequire(factoryCode, year, month, monthPlanVersion);
        Map<String, Set<String>> productRelationMouldMap = new HashMap<>();
        Map<String, Map<String, Set<String>>> productRelationSpecCodeMouldMap = new HashMap<>();
        Map<String, Set<String>> mouldRelationProductMap = new HashMap<>();
        setRelationInfo(productRelationMouldMap, productRelationSpecCodeMouldMap, mouldRelationProductMap, productMouldConfigurationList, enableMap.keySet());
        //更新缓存信息
        updateCacheInfoByMould(productionContext, enableMap, productRelationMouldMap, productRelationSpecCodeMouldMap, mouldRelationProductMap);
    }

    /**
     * 设置排产重要信息
     * 生胎代码、成型法、硫化代号
     * 胎体层级
     *
     * @param productionContext
     * @param enableMatchingProductList
     */
    private List<MatchingProductionConfigurationVo> setProductionNecessaryInfo(ProductionContext productionContext, List<MatchingProductionConfigurationVo> enableMatchingProductList) {
        if (CollectionUtils.isEmpty(enableMatchingProductList)) {
            return Collections.emptyList();
        }
        Map<String, FactoryNoProduction> factoryNoProductionMap = productionContext.getFactoryNoProductionMap();
        Map<String, MonthPlanManufacturingRequirementVo> productGroupPlanMap = new HashMap<>();
        List<MonthPlanManufacturingRequirementVo> allProductionPlanList = productionContext.getMonthPlanInitMap().values().stream().collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(allProductionPlanList)) {
            productGroupPlanMap = allProductionPlanList.stream().collect(Collectors.toMap(MonthPlanManufacturingRequirementVo::getProductCode, Function.identity(), (p1, p2) -> p1));
        }
        List<MatchingProductionConfigurationVo> configurationList = new ArrayList<>();
        Map<String, ProductProductionHelper> productionHelperMap = new HashMap<>();
        //为了支持流式写法
        Map<String, MonthPlanManufacturingRequirementVo> finalProductGroupPlan = productGroupPlanMap;
        enableMatchingProductList.stream().forEach(enableMatchingProduct -> {
            String productCode = enableMatchingProduct.getProductCode();
            if (factoryNoProductionMap.containsKey(productCode)) {
                return;
            }
            configurationList.add(enableMatchingProduct);
            MonthPlanManufacturingRequirementVo planRequirement = finalProductGroupPlan.get(productCode);
            if (null != planRequirement) {
                //采用已有的
                MatchingProductionUtils.setProductionNecessaryInfo(enableMatchingProduct, planRequirement);
                return;
            }
            //新增的搭配-物料
            ProductProductionHelper helper = productionHelperMap.get(productCode);
            if (null == helper) {
                helper = ProductionPlanUtils.getProductProductionInfo(productCode, productionContext, BigDecimal.ONE.longValue());
                productionHelperMap.put(productCode, helper);
            }
            MatchingProductionUtils.setProductionNecessaryInfo(productionContext, enableMatchingProduct, helper);
        });
        return configurationList;
    }

    /**
     * 设置关联关系
     *
     * @param productRelationMouldMap         物料配置的模具信息 key=productCode ：value=模具号|*|规格代号
     * @param productRelationSpecCodeMouldMap 物料配置的模具信息 key=productCode ：值={规格代号:模具号}-> key=规格代码 ：值=模具号
     * @param mouldRelationProductMap         模具可硫化物料信息 key=模具号 ：value=物料代码
     * @param productMouldConfigurationList   物料模具关系列表
     * @param enableMouldSet                  可用的模具信息
     */
    private void setRelationInfo(Map<String, Set<String>> productRelationMouldMap,
                                 Map<String, Map<String, Set<String>>> productRelationSpecCodeMouldMap,
                                 Map<String, Set<String>> mouldRelationProductMap,
                                 List<ProductMouldConfigurationVo> productMouldConfigurationList,
                                 Set<String> enableMouldSet) {
        productMouldConfigurationList.stream().forEach(configuration -> {
            String productCode = configuration.getProductCode();
            String mouldCode = configuration.getMouldCode();
            String specCode = configuration.getSpecCode();
            if (StringUtils.isBlank(productCode) || StringUtils.isBlank(mouldCode) || StringUtils.isBlank(specCode)) {
                return;
            }
            //不可用，不纳入
            if (!enableMouldSet.contains(mouldCode)) {
                return;
            }
            //物料关联的硫化模具
            ProductUtils.setProductRelationMould(productRelationMouldMap, productRelationSpecCodeMouldMap, productCode, mouldCode, specCode);
            //模具关联的物料编码
            MouldBaseUtils.setMouldRelationProduct(mouldRelationProductMap, mouldCode, productCode);
        });
    }

    /**
     * 更新缓存信息，主要更新新加的模具
     * 物料与模具间的关联关系
     *
     * @param productionContext               排产上下文
     * @param enableMap                       可能增加的模具信息
     * @param productRelationMouldMap         物料配置的模具信息 key=productCode ：value=模具号|*|规格代号
     * @param productRelationSpecCodeMouldMap 物料配置的模具信息 key=productCode ：值={规格代号:模具号}-> key=规格代码 ：值=模具号
     * @param mouldRelationProductMap         模具可硫化物料信息 key=模具号 ：value=物料代码
     */
    private void updateCacheInfoByMould(ProductionContext productionContext,
                                        Map<String, MouldInfoVO> enableMap,
                                        Map<String, Set<String>> productRelationMouldMap,
                                        Map<String, Map<String, Set<String>>> productRelationSpecCodeMouldMap,
                                        Map<String, Set<String>> mouldRelationProductMap) {
        //物料信息配置的硫化模具信息
        Map<String, Set<String>> cacheProductRelationMap = productionContext.getProductRelationMouldMap();
        if (!CollectionUtils.isEmpty(productRelationMouldMap)) {
            productRelationMouldMap.entrySet().stream().forEach(entry -> {
                String productCode = entry.getKey();
                if (cacheProductRelationMap.containsKey(productCode)) {
                    return;
                }
                cacheProductRelationMap.put(productCode, entry.getValue());
            });
        }
        //物料信息配置的规格代码，模具信息
        Map<String, Map<String, Set<String>>> cacheProductRelationSpecCodeMap = productionContext.getProductRelationSpecCodeMouldMap();
        if (!CollectionUtils.isEmpty(productRelationSpecCodeMouldMap)) {
            productRelationSpecCodeMouldMap.entrySet().stream().forEach(entry -> {
                String productCode = entry.getKey();
                if (cacheProductRelationSpecCodeMap.containsKey(productCode)) {
                    return;
                }
                cacheProductRelationSpecCodeMap.put(productCode, entry.getValue());
            });
        }
        if (CollectionUtils.isEmpty(enableMap)) {
            return;
        }
        //模具缓存信息
        Map<String, MouldInfoVO> cacheMouldInfoMap = productionContext.getMouldInfoMap();
        Map<String, Set<String>> cacheMouldRelationMap = productionContext.getMouldRelationProductMap();
        enableMap.entrySet().stream().forEach(mouldInfoEntry -> {
            String mouldCode = mouldInfoEntry.getKey();
            Set<String> relationProductInfo = mouldRelationProductMap.get(mouldCode);
            //模具信息存在，只更新新的物料关联信息，其它不动
            if (cacheMouldInfoMap.containsKey(mouldCode)) {
                MouldInfoVO mouldInfo = cacheMouldInfoMap.get(mouldCode);
                Set<String> cacheRelationProductInfo = cacheMouldRelationMap.get(mouldCode);
                cacheRelationProductInfo.addAll(relationProductInfo);
                mouldInfo.setAssocaiationCount(cacheRelationProductInfo.size());
                return;
            }
            MouldInfoVO newMouldInfo = mouldInfoEntry.getValue();
            Integer assocaiationCount = BigDecimal.ZERO.intValue();
            if (!CollectionUtils.isEmpty(relationProductInfo)) {
                assocaiationCount = relationProductInfo.size();
            }
            newMouldInfo.setIsContinue(YesOrNoEnum.NO.getValue());
            newMouldInfo.setAssocaiationCount(assocaiationCount);
            cacheMouldInfoMap.put(mouldCode, newMouldInfo);
            cacheMouldRelationMap.put(mouldCode, relationProductInfo);
        });
    }

    /**
     * 获取维修-模具信息
     *
     * @param maintenanceList   维修模具配置
     * @param productionContext 排产上下文
     * @return
     */
    private List<MouldInfoVO> getAddMouldMaintenanceConfiguration(List<MouldMaintenanceConfigurationVo> maintenanceList, ProductionContext productionContext) {
        if (CollectionUtils.isEmpty(maintenanceList)) {
            return Collections.emptyList();
        }
        Map<String, MouldInfoVO> maintenanceMouldMap = new HashMap<>();
        List<MouldInfoVO> mouldInfoList = new ArrayList<>();
        ZoneId zoneId = ZoneId.systemDefault();
        maintenanceList.stream().forEach(maintenanceConfiguration -> {
            String mouldCode = maintenanceConfiguration.getMouldCode();
            MouldInfoVO mouldInfo = MouldBaseUtils.buildMouldInfo(maintenanceMouldMap.get(mouldCode), maintenanceConfiguration, zoneId, productionContext);
            mouldInfoList.add(mouldInfo);
        });
        return mouldInfoList;
    }

}
