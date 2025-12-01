package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.enums.*;
import com.tlt.aps.utils.DictDataUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.maindata.mapper.LocationChannelConfigurationMapper;
import com.zlt.aps.maindata.mapper.PlanOrderSortConfigurationMapper;
import com.zlt.aps.maindata.service.IPlanOrderSortConfigurationService;
import com.zlt.aps.monthplan.api.domain.entity.LocationChannelConfiguration;
import com.zlt.aps.monthplan.api.domain.entity.PlanOrderSortConfiguration;
import com.zlt.aps.monthplan.api.domain.vo.PlanOrderSortConfigurationVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：PlanOrderSortConfigurationServiceImpl.java
 * 描    述：PlanOrderSortConfigurationServiceImpl业务排序配置业务层处理
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-17
 */
@Slf4j
@Service
public class PlanOrderSortConfigurationServiceImpl extends ServiceImpl<PlanOrderSortConfigurationMapper, PlanOrderSortConfiguration> implements IPlanOrderSortConfigurationService {

    @Autowired
    private LocationChannelConfigurationMapper locationChannelConfigurationMapper;

    @Autowired
    private ISysDictDataCacheService sysDictDataService;

    private final static String LOCATION_DICT_TYPE = "biz_stor_type"; // 库位类别字典类型

    private final static String CHANNEL_DICT_TYPE = "biz_channel_type"; // 渠道字典类型

    private final static String BRAND_DICT_TYPE = "biz_brand_type"; // 品牌字典类型

    @Override
    public List<PlanOrderSortConfiguration> getStockHedgingConfiguration() {
        QueryWrapper<PlanOrderSortConfiguration> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(true, "BUSINESS_TYPE", BusinessSortTypeEnum.STOCK_HEDGING.getCode());
        queryWrapper.orderByAsc("FACTORY_CODE", "PRIORITY");
        return getBaseMapper().selectList(queryWrapper);
    }

    @Override
    public List<PlanOrderSortConfiguration> getProductionConfiguration(String factoryCode) {
        QueryWrapper<PlanOrderSortConfiguration> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(true, "BUSINESS_TYPE", BusinessSortTypeEnum.PRODUCE_PRODUCTION.getCode());
        queryWrapper.eq(true, "FACTORY_CODE", factoryCode);
        queryWrapper.orderByAsc("FACTORY_CODE", "PRIORITY");
        return getBaseMapper().selectList(queryWrapper);
    }

    @Override
    public List<PlanOrderSortConfiguration> getStockHedgingFirstSortConfiguration() {
        QueryWrapper<PlanOrderSortConfiguration> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(true, "BUSINESS_TYPE", BusinessSortTypeEnum.STOCK_HEDGING.getCode());
        queryWrapper.eq(true, "HIERARCHY", SortHierarchyEnum.FIRST_HIERARCHY.getCode());
        queryWrapper.orderByAsc("FACTORY_CODE", "PRIORITY");
        return getBaseMapper().selectList(queryWrapper);
    }

    @Override
    public List<PlanOrderSortConfiguration> getStockHedgingSecondSortConfiguration() {
        QueryWrapper<PlanOrderSortConfiguration> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(true, "BUSINESS_TYPE", BusinessSortTypeEnum.STOCK_HEDGING.getCode());
        queryWrapper.eq(true, "HIERARCHY", SortHierarchyEnum.SECOND_HIERARCHY.getCode());
        queryWrapper.orderByAsc("FACTORY_CODE", "PRIORITY");
        return getBaseMapper().selectList(queryWrapper);
    }

    @Override
    public Map<Integer, List<PlanOrderSortConfiguration>> getStockHedgingConfigurationList() {
        Map<Integer, List<PlanOrderSortConfiguration>> resultMap = new HashMap<>();

        // 库存对冲第一排产顺序
        resultMap.put(SortHierarchyEnum.FIRST_HIERARCHY.getCode(), getStockHedgingSortConfigurations(SortHierarchyEnum.FIRST_HIERARCHY));

        // 库存对冲第二排产顺序
        resultMap.put(SortHierarchyEnum.SECOND_HIERARCHY.getCode(), getLocationChannelConfigurations()
                .stream()
                .sorted(Comparator.comparingInt(LocationChannelConfiguration::getLocationType))
                .map(this::toStockHedgingSortConfiguration)
                .sorted(Comparator.comparing(PlanOrderSortConfiguration::getPriority, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList()));

        return resultMap;
    }

    @Override
    public Map<Integer, List<PlanOrderSortConfiguration>> getPlanOrderSortConfigurationList() {
        Map<Integer, List<PlanOrderSortConfiguration>> resultMap = new HashMap<>();
        // 月份第一排产顺序
        resultMap.put(SortHierarchyEnum.FIRST_HIERARCHY.getCode(), getPlanOrderSortConfigurations(SortHierarchyEnum.FIRST_HIERARCHY));
        // 月份第二排产顺序
        resultMap.put(SortHierarchyEnum.SECOND_HIERARCHY.getCode(), getPlanOrderSortConfigurations(SortHierarchyEnum.SECOND_HIERARCHY));
        // 月份第三排产顺序
        resultMap.put(SortHierarchyEnum.THIRD_HIERARCHY.getCode(), getLocationChannelConfigurations()
                .stream()
                .sorted(Comparator.comparingInt(LocationChannelConfiguration::getLocationType))
                .map(this::toPlanOrderSortConfiguration)
                .sorted(Comparator.comparing(PlanOrderSortConfiguration::getPriority, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList()));
        return resultMap;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveStockHedgingConfiguration(PlanOrderSortConfigurationVo planOrderSortConfigurationVo) {
        // 库存第二对冲顺序校验
        // checkSortConfiguration(planOrderSortConfigurationVo.getSecondStockHedgingSortConfigurations());
        // 第一排产顺序先删除后新增
        insertNewOrderSort(planOrderSortConfigurationVo.getFirstStockHedgingSortConfigurations());
        // 第二排产顺序先删除后新增
        insertNewOrderSort(planOrderSortConfigurationVo.getSecondStockHedgingSortConfigurations());
    }

    /**
     * 先删除历史记录，再插入新记录
     * 唯一键 分厂+业务类型+层级
     */
    private void insertNewOrderSort(List<PlanOrderSortConfiguration> stockHedgingSortConfigurations) {
        if (CollectionUtils.isEmpty(stockHedgingSortConfigurations)) {
            return;
        }

        List<String> factoryCodeList = stockHedgingSortConfigurations.stream().map(PlanOrderSortConfiguration::getFactoryCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        List<String> businessTypeList = stockHedgingSortConfigurations.stream().map(PlanOrderSortConfiguration::getBusinessType).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        List<Integer> hierarchyList = stockHedgingSortConfigurations.stream().map(PlanOrderSortConfiguration::getHierarchy).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        locationChannelConfigurationMapper.deleteByParamList(factoryCodeList, businessTypeList, hierarchyList);

        stockHedgingSortConfigurations.forEach(v -> v.setId(null));
        saveOrderSortConfigurations(stockHedgingSortConfigurations);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savePlanOrderConfiguration(PlanOrderSortConfigurationVo planOrderSortConfigurationVo) {
        // 月份第三排产顺序校验
        // checkSortConfiguration(planOrderSortConfigurationVo.getThirdPlanOrderSortConfigurations());
        // 保存对冲顺序配置
        insertNewOrderSort(planOrderSortConfigurationVo.getFirstPlanOrderSortConfigurations());
        insertNewOrderSort(planOrderSortConfigurationVo.getSecondPlanOrderSortConfigurations());
        insertNewOrderSort(planOrderSortConfigurationVo.getThirdPlanOrderSortConfigurations());
    }

    private void saveOrderSortConfigurations(List<PlanOrderSortConfiguration> configurations) {
        if (CollectionUtils.isNotEmpty(configurations)) {
            try {
                saveOrUpdateBatch(configurations);
            } catch (Exception e) {
                throw e;
            }
        }
    }

    /**
     * 校验顺序配置
     *
     * @param sortConfigurations
     */
    private void checkSortConfiguration(List<PlanOrderSortConfiguration> sortConfigurations) {
        sortConfigurations.sort(Comparator.comparing(PlanOrderSortConfiguration::getOptionCode));
        Map<String, List<PlanOrderSortConfiguration>> groupedByLocation = sortConfigurations.stream()
                .collect(Collectors.groupingBy(item -> item.getOptionCode().split(";")[0]));
        for (Map.Entry<String, List<PlanOrderSortConfiguration>> planOrderSortConfigMap : groupedByLocation.entrySet()) {
            List<PlanOrderSortConfiguration> planOrderSortConfigurations = planOrderSortConfigMap.getValue();
            // 按name拆分后数组的长度进行排序
            List<PlanOrderSortConfiguration> sortedItems = planOrderSortConfigurations.stream()
                    .sorted(Comparator.comparingInt(item -> item.getOptionName().split("-").length))
                    .collect(Collectors.toList());
            // 检查value值是否满足条件
            for (int i = 0; i < sortedItems.size() - 1; i++) {
                PlanOrderSortConfiguration current = sortedItems.get(i);
                PlanOrderSortConfiguration next = sortedItems.get(i + 1);
                if (current.getOptionName().split("-").length < next.getOptionName().split("-").length &&
                        current.getPriority() <= next.getPriority()) {
                    throw new IllegalArgumentException("" + current.getOptionName() + "的排产顺序值必须大于" + next.getOptionName() + "的排产顺序值");
                }
            }
        }
    }

    /**
     * 获取库存对冲第一排产顺序
     *
     * @param hierarchy
     * @return
     */
    private List<PlanOrderSortConfiguration> getStockHedgingSortConfigurations(SortHierarchyEnum hierarchy) {
        return Stream.of(StockHedgingOptionsEnum.values())
                .map(option -> createStockHedgingSortConfiguration(option, hierarchy))
                .sorted(Comparator.comparing(
                        PlanOrderSortConfiguration::getPriority,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    /**
     * 获取月份排产顺序配置
     *
     * @param hierarchy 排序层级
     * @return 排产顺序配置列表
     */
    private List<PlanOrderSortConfiguration> getPlanOrderSortConfigurations(SortHierarchyEnum hierarchy) {
        Enum<? extends Enum<?>>[] options = hierarchy == SortHierarchyEnum.FIRST_HIERARCHY
                ? ProductionFirstSortOptionsEnum.values()
                : ProductionSecondSortOptionsEnum.values();

        return Arrays.stream(options)
                .map(option -> createPlanOrderSortConfiguration(option, hierarchy))
                .sorted(Comparator.comparing(PlanOrderSortConfiguration::getPriority, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    /**
     * 创建月份排产顺序配置
     *
     * @param option    排序选项
     * @param hierarchy 排序层级
     * @return 排产顺序配置
     */
    private PlanOrderSortConfiguration createPlanOrderSortConfiguration(Enum<? extends Enum<?>> option, SortHierarchyEnum hierarchy) {
        PlanOrderSortConfiguration planOrderSortConfiguration = new PlanOrderSortConfiguration();
        planOrderSortConfiguration.setBusinessType(BusinessSortTypeEnum.PRODUCE_PRODUCTION.getCode()); // 业务类型
        planOrderSortConfiguration.setHierarchy(hierarchy.getCode()); // 层级
        if (option instanceof ProductionFirstSortOptionsEnum) {
            ProductionFirstSortOptionsEnum firstOption = (ProductionFirstSortOptionsEnum) option;
            String optionName = I18nUtil.getMessage(firstOption.getOptionDescI18nKey(), Locale.SIMPLIFIED_CHINESE);
            planOrderSortConfiguration.setOptionCode(firstOption.getOptionCode()); // 配置项编码
            planOrderSortConfiguration.setOptionName(optionName); // 配置项名称
            if (StringUtils.equals(firstOption.getSortType(), "ASC")) {
                planOrderSortConfiguration.setSortOrder(ApsConstant.SORT_ASC);
            } else {
                planOrderSortConfiguration.setSortOrder(ApsConstant.SORT_DESC);
            }
        } else if (option instanceof ProductionSecondSortOptionsEnum) {
            ProductionSecondSortOptionsEnum secondOption = (ProductionSecondSortOptionsEnum) option;
            String optionName = I18nUtil.getMessage(secondOption.getOptionDescI18nKey(), Locale.SIMPLIFIED_CHINESE);
            planOrderSortConfiguration.setOptionCode(secondOption.getOptionCode()); // 配置项编码
            planOrderSortConfiguration.setOptionName(optionName); // 配置项名称
            if (StringUtils.equals(secondOption.getSortType(), "ASC")) {
                planOrderSortConfiguration.setSortOrder(ApsConstant.SORT_ASC);
            } else {
                planOrderSortConfiguration.setSortOrder(ApsConstant.SORT_DESC);
            }
        }
        LambdaQueryWrapper<PlanOrderSortConfiguration> lambdaQueryWrapper = createQueryWrapper(option, hierarchy);
        PlanOrderSortConfiguration entity = getBaseMapper().selectOne(lambdaQueryWrapper);
        if (!Objects.isNull(entity)) {
            planOrderSortConfiguration.setId(entity.getId());
            planOrderSortConfiguration.setFactoryCode(entity.getFactoryCode());
            planOrderSortConfiguration.setPriority(entity.getPriority());
            planOrderSortConfiguration.setRemark(entity.getRemark());
        } else {
            planOrderSortConfiguration.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
//        populateConfiguration(planOrderSortConfiguration, option, hierarchy, optionName, entity);
        return planOrderSortConfiguration;
    }

//        lambdaQueryWrapper.eq(PlanOrderSortConfiguration::getFactoryCode, "AH01");

    /**
     * 创建查询包装器
     *
     * @param option    排序选项
     * @param hierarchy 排序层级
     * @return 查询包装器
     */
    private LambdaQueryWrapper<PlanOrderSortConfiguration> createQueryWrapper(Enum<?> option, SortHierarchyEnum hierarchy) {
        LambdaQueryWrapper<PlanOrderSortConfiguration> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PlanOrderSortConfiguration::getIsDelete, ApsConstant.DEL_FLAG_NORMAL);
        queryWrapper.eq(PlanOrderSortConfiguration::getBusinessType, BusinessSortTypeEnum.PRODUCE_PRODUCTION.getCode());
        queryWrapper.eq(PlanOrderSortConfiguration::getHierarchy, hierarchy.getCode());
        String factoryCode = SecurityUtils.getUserCurrentFactory() == null ? FactoryConstant.DEFAULT_FACTORY_CODE : SecurityUtils.getUserCurrentFactory();
        queryWrapper.eq(PlanOrderSortConfiguration::getFactoryCode, factoryCode);
        // 假设枚举类中有 getCode 方法，且 PlanOrderSortConfiguration 实体类中有对应的 code 字段
        if (option instanceof ProductionFirstSortOptionsEnum) {
            ProductionFirstSortOptionsEnum firstOption = (ProductionFirstSortOptionsEnum) option;
            queryWrapper.eq(PlanOrderSortConfiguration::getOptionCode, firstOption.getOptionCode());
        } else if (option instanceof ProductionSecondSortOptionsEnum) {
            ProductionSecondSortOptionsEnum secondOption = (ProductionSecondSortOptionsEnum) option;
            queryWrapper.eq(PlanOrderSortConfiguration::getOptionCode, secondOption.getOptionCode());
        }
        return queryWrapper;
    }

    /**
     * 创建库存对冲顺序对象
     *
     * @param option
     * @param hierarchy
     * @return
     */
    private PlanOrderSortConfiguration createStockHedgingSortConfiguration(StockHedgingOptionsEnum option, SortHierarchyEnum hierarchy) {
        PlanOrderSortConfiguration planOrderSortConfiguration = new PlanOrderSortConfiguration();
        // 配置项名称
        String optionName = I18nUtil.getMessage(option.getOptionDescI18nKey(), Locale.SIMPLIFIED_CHINESE);
        LambdaQueryWrapper<PlanOrderSortConfiguration> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(PlanOrderSortConfiguration::getIsDelete, ApsConstant.DEL_FLAG_NORMAL);
        String factoryCode = SecurityUtils.getUserCurrentFactory() == null ? FactoryConstant.DEFAULT_FACTORY_CODE : SecurityUtils.getUserCurrentFactory();
        lambdaQueryWrapper.eq(PlanOrderSortConfiguration::getFactoryCode, factoryCode);
        lambdaQueryWrapper.eq(PlanOrderSortConfiguration::getBusinessType, BusinessSortTypeEnum.STOCK_HEDGING.getCode());
        lambdaQueryWrapper.eq(PlanOrderSortConfiguration::getHierarchy, hierarchy.getCode());
        lambdaQueryWrapper.eq(PlanOrderSortConfiguration::getOptionCode, option.getOptionCode());
        PlanOrderSortConfiguration entity = getBaseMapper().selectOne(lambdaQueryWrapper);
        planOrderSortConfiguration.setBusinessType(BusinessSortTypeEnum.STOCK_HEDGING.getCode());
        planOrderSortConfiguration.setHierarchy(hierarchy.getCode()); // 层级
        planOrderSortConfiguration.setOptionCode(option.getOptionCode()); // 配置项编码
        planOrderSortConfiguration.setOptionName(optionName); // 配置项名称
        if (StringUtils.equals(option.getSortType(), "ASC")) {
            planOrderSortConfiguration.setSortOrder(ApsConstant.SORT_ASC);
        } else {
            planOrderSortConfiguration.setSortOrder(ApsConstant.SORT_DESC);
        }
        if (!Objects.isNull(entity)) {
            planOrderSortConfiguration.setId(entity.getId());
            planOrderSortConfiguration.setFactoryCode(entity.getFactoryCode());
            planOrderSortConfiguration.setPriority(entity.getPriority());
            planOrderSortConfiguration.setRemark(entity.getRemark());
        } else {
            planOrderSortConfiguration.setFactoryCode(factoryCode);
        }
        return planOrderSortConfiguration;
    }


    /**
     * 获取库位渠道配置表数据
     *
     * @return
     */
    private List<LocationChannelConfiguration> getLocationChannelConfigurations() {
        LambdaQueryWrapper<LocationChannelConfiguration> locationChannelConfigurationLambdaQueryWrapper = new LambdaQueryWrapper();
        locationChannelConfigurationLambdaQueryWrapper.eq(LocationChannelConfiguration::getIsDelete, ApsConstant.DEL_FLAG_NORMAL);
        String factoryCode = SecurityUtils.getUserCurrentFactory() == null ? FactoryConstant.DEFAULT_FACTORY_CODE : SecurityUtils.getUserCurrentFactory();
        locationChannelConfigurationLambdaQueryWrapper.eq(LocationChannelConfiguration::getFactoryCode, factoryCode);
        return locationChannelConfigurationMapper.selectList(locationChannelConfigurationLambdaQueryWrapper);
    }

    /**
     * 库存对冲第二排产顺序对象赋值包含库位类别、渠道、品牌字典转换
     *
     * @param locationChannelConfiguration
     * @return
     */
    private PlanOrderSortConfiguration toStockHedgingSortConfiguration(LocationChannelConfiguration locationChannelConfiguration) {
        PlanOrderSortConfiguration planOrderSortConfiguration = new PlanOrderSortConfiguration();
        planOrderSortConfiguration.setBusinessType(BusinessSortTypeEnum.STOCK_HEDGING.getCode()); // 库存冲销
        planOrderSortConfiguration.setHierarchy(SortHierarchyEnum.SECOND_HIERARCHY.getCode()); // 层级
        // 第二对冲配置项编码
        String optionCode = locationChannelConfiguration.getLocationType() + ";" + locationChannelConfiguration.getChannel() + ";" + locationChannelConfiguration.getBrand();
        LambdaQueryWrapper<PlanOrderSortConfiguration> lambdaQueryWrapper = new LambdaQueryWrapper();
        lambdaQueryWrapper.eq(PlanOrderSortConfiguration::getIsDelete, ApsConstant.DEL_FLAG_NORMAL);
        String factoryCode = SecurityUtils.getUserCurrentFactory() == null ? FactoryConstant.DEFAULT_FACTORY_CODE : SecurityUtils.getUserCurrentFactory();
        lambdaQueryWrapper.eq(PlanOrderSortConfiguration::getFactoryCode, factoryCode);
        lambdaQueryWrapper.eq(PlanOrderSortConfiguration::getBusinessType, BusinessSortTypeEnum.STOCK_HEDGING.getCode());
        lambdaQueryWrapper.eq(PlanOrderSortConfiguration::getHierarchy, SortHierarchyEnum.SECOND_HIERARCHY.getCode());
        lambdaQueryWrapper.eq(PlanOrderSortConfiguration::getOptionCode, optionCode);
        PlanOrderSortConfiguration entity = getBaseMapper().selectOne(lambdaQueryWrapper);
        // 库位类别字典
        List<SysDictData> locationDictDatas = sysDictDataService.getType(LOCATION_DICT_TYPE);
        // 渠道字典
        List<SysDictData> channelDictDatas = sysDictDataService.getType(CHANNEL_DICT_TYPE);
        // 品牌字典
        List<SysDictData> brandDictDatas = sysDictDataService.getType(BRAND_DICT_TYPE);
        // 第二对冲配置项名称
        String optionName = DictDataUtil.getLabel4DictValue(String.valueOf(locationChannelConfiguration.getLocationType()), locationDictDatas) + "-" + (StringUtils.equals(locationChannelConfiguration.getChannel(), "*") ? "*" : DictDataUtil.getLabel4DictValue(locationChannelConfiguration.getChannel(), channelDictDatas));
        // 品牌
        if (StringUtils.isNotEmpty(locationChannelConfiguration.getBrand()) && !StringUtils.equals(locationChannelConfiguration.getBrand(), "*")) {
            optionName += (StringUtils.isBlank(optionName) ? "" : "-") + DictDataUtil.getLabel4DictValue(String.valueOf(locationChannelConfiguration.getBrand()), brandDictDatas);
        }
        planOrderSortConfiguration.setOptionCode(optionCode); // 配置项编码
        planOrderSortConfiguration.setOptionName(optionName); // 配置项名称
        if (!Objects.isNull(entity)) {
            planOrderSortConfiguration.setId(entity.getId());
            planOrderSortConfiguration.setFactoryCode(entity.getFactoryCode());
            planOrderSortConfiguration.setPriority(entity.getPriority());
            planOrderSortConfiguration.setRemark(entity.getRemark());
        } else {
            planOrderSortConfiguration.setFactoryCode(factoryCode);
        }
        return planOrderSortConfiguration;
    }

    /**
     * 月份第三排产顺序对象赋值包含库位类别、渠道、品牌字典转换
     *
     * @param locationChannelConfiguration
     * @return
     */
    private PlanOrderSortConfiguration toPlanOrderSortConfiguration(LocationChannelConfiguration locationChannelConfiguration) {
        PlanOrderSortConfiguration planOrderSortConfiguration = new PlanOrderSortConfiguration();
        planOrderSortConfiguration.setBusinessType(BusinessSortTypeEnum.PRODUCE_PRODUCTION.getCode()); // 生产排产
        planOrderSortConfiguration.setHierarchy(SortHierarchyEnum.THIRD_HIERARCHY.getCode()); // 层级
        // 月份第三排产顺序配置项编码
        String optionCode = locationChannelConfiguration.getLocationType() + ";" + locationChannelConfiguration.getChannel() + ";" + locationChannelConfiguration.getBrand();
        LambdaQueryWrapper<PlanOrderSortConfiguration> lambdaQueryWrapper = new LambdaQueryWrapper();
        lambdaQueryWrapper.eq(PlanOrderSortConfiguration::getIsDelete, ApsConstant.DEL_FLAG_NORMAL);
        String factoryCode = SecurityUtils.getUserCurrentFactory() == null ? FactoryConstant.DEFAULT_FACTORY_CODE : SecurityUtils.getUserCurrentFactory();
        lambdaQueryWrapper.eq(PlanOrderSortConfiguration::getFactoryCode, factoryCode);
        lambdaQueryWrapper.eq(PlanOrderSortConfiguration::getBusinessType, BusinessSortTypeEnum.PRODUCE_PRODUCTION.getCode());
        lambdaQueryWrapper.eq(PlanOrderSortConfiguration::getHierarchy, SortHierarchyEnum.THIRD_HIERARCHY.getCode());
        lambdaQueryWrapper.eq(PlanOrderSortConfiguration::getOptionCode, optionCode);
        PlanOrderSortConfiguration entity = getBaseMapper().selectOne(lambdaQueryWrapper);
        // 库位类别字典
        List<SysDictData> locationDictDatas = sysDictDataService.getType(LOCATION_DICT_TYPE);
        // 渠道字典
        List<SysDictData> channelDictDatas = sysDictDataService.getType(CHANNEL_DICT_TYPE);
        // 品牌字典
        List<SysDictData> brandDictDatas = sysDictDataService.getType(BRAND_DICT_TYPE);
        // 月份第三排产顺序配置项名称
        String optionName = DictDataUtil.getLabel4DictValue(String.valueOf(locationChannelConfiguration.getLocationType()), locationDictDatas) + "-" + (StringUtils.equals(locationChannelConfiguration.getChannel(), "*") ? "*" : DictDataUtil.getLabel4DictValue(locationChannelConfiguration.getChannel(), channelDictDatas));
        // 品牌
        if (StringUtils.isNotEmpty(locationChannelConfiguration.getBrand()) && !StringUtils.equals(locationChannelConfiguration.getBrand(), "*")) {
            optionName += (StringUtils.isBlank(optionName) ? "" : "-") + DictDataUtil.getLabel4DictValue(String.valueOf(locationChannelConfiguration.getBrand()), brandDictDatas);
        }
        planOrderSortConfiguration.setOptionCode(optionCode); // 配置项编码
        planOrderSortConfiguration.setOptionName(optionName); // 配置项名称
        if (!Objects.isNull(entity)) {
            planOrderSortConfiguration.setId(entity.getId());
            planOrderSortConfiguration.setFactoryCode(entity.getFactoryCode());
            planOrderSortConfiguration.setPriority(entity.getPriority());
            planOrderSortConfiguration.setRemark(entity.getRemark());
        } else {
            planOrderSortConfiguration.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return planOrderSortConfiguration;
    }
}
