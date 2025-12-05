package com.zlt.aps.monthplan.factory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.aps.factory.domain.dto.VulcanizingProductInfoDto;
import com.zlt.aps.factory.mapper.FactoryProductionMouldConfigurationMapper;
import com.zlt.aps.factory.utils.DateUtils;
import com.zlt.aps.maindata.mapper.MdmModelInfoEntityMapper;
import com.zlt.aps.maindata.mapper.MdmProductInfoEntityMapper;
import com.zlt.aps.maindata.mapper.MdmProductModelRelationEntityMapper;
import com.zlt.aps.maindata.utils.LambdaWrapperBuilder;
import com.zlt.aps.monthplan.api.domain.dto.FactoryMouldingProductParamDto;
import com.zlt.aps.monthplan.api.domain.entity.MdmModelInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmSkuMouldRel;
import com.zlt.aps.monthplan.api.domain.entity.ProductionMouldConfiguration;
import com.zlt.aps.monthplan.factory.helper.MouldRelationProductHelper;
import com.zlt.aps.monthplan.factory.service.IProductionMouldConfigurationService;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zlt.common.utils.ImportExcelValidatedUtils.addImportErrorLog;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ProductionMouldConfigurationServiceImpl.java
 * 描    述：ProductionMouldConfigurationServiceImpl模具正在生产的品种业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductionMouldConfigurationServiceImpl extends ServiceImpl<FactoryProductionMouldConfigurationMapper, ProductionMouldConfiguration> implements IProductionMouldConfigurationService {

    private final FactoryProductionMouldConfigurationMapper mapper;

    private final MdmProductInfoEntityMapper mdmProductInfoEntityMapper;

    private final MdmProductModelRelationEntityMapper productModelRelationMapper;

    private final MdmModelInfoEntityMapper mdmModelInfoEntityMapper;

    private final BaseDao baseDao;

    /**
     * 列表查询
     */
    @Override
    public List<ProductionMouldConfiguration> selectList(ProductionMouldConfiguration queryVO) {
        QueryWrapper<ProductionMouldConfiguration> wrapper = new QueryWrapper<>();
        builderCondition(wrapper, queryVO);
        return mapper.selectList(wrapper);
    }

    /**
     * 保存
     */
    @Override
    public AjaxResult saveConfiguration(ProductionMouldConfiguration billVO) {
        // 校验物料信息和模具信息存在
        AjaxResult errorResult = checkField(billVO);
        if (errorResult != null) {
            return errorResult;
        }

        if (billVO.getId() == null) {
            mapper.insert(billVO);
        } else {
            mapper.updateById(billVO);
        }

        return AjaxResult.success();
    }

    /**
     * 根据ID列表删除
     */
    @Override
    public int removeByIds(List<Long> ids) {
        return mapper.deleteBatchIds(ids);
    }

    /**
     * 导入
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult doImportData(List<ProductionMouldConfiguration> list, boolean updateSupport, long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<ProductionMouldConfiguration> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String productInfoStr = I18nUtil.getMessage("ui.data.column.productionMouldConfiguration.notExist.productInfo");
        String mouldCodeStr = I18nUtil.getMessage("ui.data.column.productionMouldConfiguration.notExist.mouldCode");
        String repeatStr = I18nUtil.getMessage("ui.data.column.productionMouldConfiguration.repeat");

        // 重复记录
        Function<ProductionMouldConfiguration, String> keyFunc = v -> GenerageMapKeyUtils.createMapKey(v.getYear(), v.getMonth(), v.getFactoryCode(), v.getProductCode(), v.getMouldCode());
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(keyFunc, Collectors.counting()));

        // 获取对应物料信息、模具信息
        Map<String, MdmProductInfo> productInfoMap = getMdmProductInfoMap(list);
        Map<String, MdmModelInfo> modelInfoMap = getMdmModelInfoMap(list);

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            ProductionMouldConfiguration item = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, item);
            if (!CollectionUtils.isEmpty(validated)) {
                item.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
                continue;
            }

            // 物料信息校验
            if (!productInfoMap.containsKey(GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getProductCode()))) {
                item.setId(-999L);
                failureNum++;
                addImportErrorLog(importLogId, errorNum, productInfoStr, importErrorLogs);
                continue;
            }

            // 模具信息校验
            if (!modelInfoMap.containsKey(GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getMouldCode()))) {
                item.setId(-999L);
                failureNum++;
                addImportErrorLog(importLogId, errorNum, mouldCodeStr, importErrorLogs);
                continue;
            }

            // 重复记录校验
            String key = keyFunc.apply(item);
            Long count = groupMap.get(key);
            if (count != null && count > 1) {
                item.setId(-999L);
                failureNum++;
                addImportErrorLog(importLogId, errorNum, repeatStr, importErrorLogs);
                continue;
            }

            importList.add(item);
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            successNum = importList.size();
            this.mergeByList(importList);
        } catch (Exception e) {
            e.printStackTrace();
            successNum = 0;
            failureNum = list.size();
            importErrorLogs.clear();
            ImportExcelValidatedUtils.addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
        }
        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    /**
     * 校验唯一性
     */
    @Override
    public String checkUnique(ProductionMouldConfiguration queryVO) {
        QueryWrapper<ProductionMouldConfiguration> wrapper = new QueryWrapper<>();
        wrapper.ne(queryVO.getId() != null, "id", queryVO.getId());
        wrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("year")), "YEAR", queryVO.getFieldValueByFieldName("year"));
        wrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month")), "MONTH", queryVO.getFieldValueByFieldName("month"));
        wrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        wrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productCode")), "PRODUCT_CODE", queryVO.getFieldValueByFieldName("productCode"));
        wrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mouldCode")), "MOULD_CODE", queryVO.getFieldValueByFieldName("mouldCode"));
        return mapper.selectCount(wrapper) > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    @Override
    public AjaxResult buildMouldingProduct(FactoryMouldingProductParamDto param) {
        String factoryCode = param.getFactoryCode();
        Date vulcanizingDate = param.getVulcanizingDate();
        LocalDate nextMonth = DateUtils.getNextMonth();
        int yearValue = nextMonth.getYear();
        int monthValue = nextMonth.getMonthValue();
        List<VulcanizingProductInfoDto> vulcanizingProductInfoList = mapper.getVulcanizingProduct(factoryCode, vulcanizingDate);
        if (CollectionUtils.isEmpty(vulcanizingProductInfoList)) {
            return AjaxResult.success();
        }
        //先删除
        QueryWrapper<ProductionMouldConfiguration> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("FACTORY_CODE", factoryCode);
        deleteWrapper.eq("YEAR", yearValue);
        deleteWrapper.eq("MONTH", monthValue);
        deleteWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        mapper.delete(deleteWrapper);
        //续作物料，一个规格代号对应唯一一个物料编码即productCode
        Map<String, String> specCodeProductMap = new HashMap<>();
        //续作物料，一个规格代号对应唯一一个胎胚号及embryoCode
        Map<String, String> specCodeEmbryoMap = new HashMap<>();
        //续作物料对应的模台数即续作模具 key specCode value:机台编号，排产信息(排产模台、机台模台)
        Map<String, Map<String, ProductionMouldConfigurationGroupVo>> specCodeMouldQtyMap = new HashMap<>();
        //物料对应的规格代号配置的模具关系 key productCode value:specCode,mouldCode
        Map<String, Map<String, Set<String>>> productMouldMap = new HashMap<>();
        buildRelationInfo(vulcanizingProductInfoList, specCodeProductMap, specCodeMouldQtyMap, productMouldMap, specCodeEmbryoMap);
        if (CollectionUtils.isEmpty(specCodeProductMap)) {
            return AjaxResult.success();
        }
        //模具关联的SAP个数
        Map<String, Integer> mouldRelationCountMap = getMouldRelationCount(factoryCode);
        //续作的物料及对应模具
        List<ProductionMouldConfiguration> continueProductList = buildContinueConfiguration(mouldRelationCountMap, specCodeProductMap, specCodeMouldQtyMap, productMouldMap, specCodeEmbryoMap);
        if (CollectionUtils.isEmpty(continueProductList)) {
            return AjaxResult.success();
        }
        continueProductList.stream().forEach(continueProduct -> {
            continueProduct.setFactoryCode(factoryCode);
            continueProduct.setYear(yearValue);
            continueProduct.setMonth(monthValue);
        });
        saveBatch(continueProductList);
        return AjaxResult.success();
    }

    /**
     * 根据查询条件，构建查询器信息
     *
     * @param queryWrapper
     * @param queryVO
     */
    protected void builderCondition(QueryWrapper<ProductionMouldConfiguration> queryWrapper, ProductionMouldConfiguration queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("year")), "YEAR", queryVO.getFieldValueByFieldName("year"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month")), "MONTH", queryVO.getFieldValueByFieldName("month"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productCode")), "PRODUCT_CODE", queryVO.getFieldValueByFieldName("productCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mouldCode")), "MOULD_CODE", queryVO.getFieldValueByFieldName("mouldCode"));
    }

    /**
     * 校验物料信息和模具信息存在
     */
    private AjaxResult checkField(ProductionMouldConfiguration billVO) {
        Map<String, MdmProductInfo> productInfoMap = getMdmProductInfoMap(Collections.singletonList(billVO));
        if (!productInfoMap.containsKey(GenerageMapKeyUtils.createMapKey(billVO.getFactoryCode(), billVO.getProductCode()))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.productionMouldConfiguration.notExist.productInfo"));
        }

        Map<String, MdmModelInfo> modelInfoMap = getMdmModelInfoMap(Collections.singletonList(billVO));
        if (!modelInfoMap.containsKey(GenerageMapKeyUtils.createMapKey(billVO.getFactoryCode(), billVO.getMouldCode()))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.productionMouldConfiguration.notExist.mouldCode"));
        }

        return null;
    }

    /**
     * 获取对应物料信息、模具信息
     */
    private Map<String, MdmModelInfo> getMdmModelInfoMap(List<ProductionMouldConfiguration> list) {
        List<String> factoryCodeList = list.stream().map(ProductionMouldConfiguration::getFactoryCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        List<String> mouldCodeList = list.stream().map(ProductionMouldConfiguration::getMouldCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(mouldCodeList)) {
            return Collections.emptyMap();
        }

        LambdaQueryWrapper<MdmModelInfo> wrapper = Wrappers.lambdaQuery(MdmModelInfo.class)
                .in(!CollectionUtils.isEmpty(factoryCodeList), MdmModelInfo::getFactoryCode, factoryCodeList)
                .in(!CollectionUtils.isEmpty(mouldCodeList), MdmModelInfo::getMouldCode, mouldCodeList);
        return mdmModelInfoEntityMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(v -> GenerageMapKeyUtils.createMapKey(v.getFactoryCode(), v.getMouldCode()), Function.identity(), (v1, v2) -> v1));
    }

    /**
     * 查询对应物料信息
     */
    private Map<String, MdmProductInfo> getMdmProductInfoMap(List<ProductionMouldConfiguration> list) {
        List<String> factoryCodeList = list.stream().map(ProductionMouldConfiguration::getFactoryCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        List<String> productCodeList = list.stream().map(ProductionMouldConfiguration::getProductCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(productCodeList)) {
            return Collections.emptyMap();
        }

        LambdaQueryWrapper<MdmProductInfo> wrapper = Wrappers.lambdaQuery(MdmProductInfo.class)
                .in(!CollectionUtils.isEmpty(factoryCodeList), MdmProductInfo::getFactoryCode, factoryCodeList)
                .in(!CollectionUtils.isEmpty(productCodeList), MdmProductInfo::getProductCode, productCodeList);
        return mdmProductInfoEntityMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(v -> GenerageMapKeyUtils.createMapKey(v.getFactoryCode(), v.getProductCode()), Function.identity(), (v1, v2) -> v1));
    }

    /**
     * 删除旧年、月、分厂的记录, 插入新数据
     */
    private void mergeByList(List<ProductionMouldConfiguration> importList) {
        if (CollectionUtils.isEmpty(importList)) {
            return;
        }
        // 删除旧年、月、分厂的记录
        LambdaQueryWrapper<ProductionMouldConfiguration> wrapper = LambdaWrapperBuilder.buildWrapperByFunction(importList, ProductionMouldConfiguration::getYear,
                ProductionMouldConfiguration::getMonth,
                ProductionMouldConfiguration::getFactoryCode);
        mapper.delete(wrapper);

        this.baseDao.insertBatch(importList);
    }

    /**
     * 根据硫化规格代号及机台模台数，构建续作信息的中间关系数据
     *
     * @param vulcanizingProductInfoList 硫化规格代号及硫化机台和硫化模数
     * @param specCodeProductMap         一个规格代号对应唯一一个物料编码即productCode
     * @param specCodeMouldQtyMap        物料对应的模台数即续作模具 key specCode value:机台编号，模台数
     * @param productMouldMap            规格代号配置的模具关系 key productCode value:specCode,mouldCode
     */
    private void buildRelationInfo(List<VulcanizingProductInfoDto> vulcanizingProductInfoList, Map<String, String> specCodeProductMap, Map<String, Map<String, ProductionMouldConfigurationGroupVo>> specCodeMouldQtyMap, Map<String, Map<String, Set<String>>> productMouldMap, Map<String, String> specCodeEmbryoMap) {
        vulcanizingProductInfoList.stream().forEach(vulcanizingProductInfo -> {
            String specCode = vulcanizingProductInfo.getSpecCode();
            String constructionProductCode = vulcanizingProductInfo.getConstructionProductCode();
            String mouldCode = vulcanizingProductInfo.getMouldCode();
            String embryoCode = vulcanizingProductInfo.getEmbryoCode();
            if (StringUtils.isBlank(constructionProductCode) || StringUtils.isBlank(mouldCode) || StringUtils.isBlank(embryoCode)) {
                return;
            }
            //规格代号找到的唯一SAP物料编码
            specCodeProductMap.put(specCode, constructionProductCode);
            //规格代号找到的唯一胚胎号
            specCodeEmbryoMap.put(specCode, embryoCode);
            //续作的模台数数据
            Map<String, ProductionMouldConfigurationGroupVo> vulcanizingMouldMap = specCodeMouldQtyMap.get(specCode);
            if (null == vulcanizingMouldMap) {
                vulcanizingMouldMap = new HashMap<>();
            }
            ProductionMouldConfigurationGroupVo productionGroup = new ProductionMouldConfigurationGroupVo(vulcanizingProductInfo.getMoldQty(), vulcanizingProductInfo.getMouldNumber());
            vulcanizingMouldMap.put(vulcanizingProductInfo.getLhMachineCode(), productionGroup);
            specCodeMouldQtyMap.put(specCode, vulcanizingMouldMap);
            //SAP物料编码+硫化规格配置的模具
            Map<String, Set<String>> productMouldRelation = productMouldMap.get(constructionProductCode);
            if (null == productMouldRelation) {
                productMouldRelation = new HashMap<>();
            }
            Set<String> mouldRelationSet = productMouldRelation.get(specCode);
            if (null == mouldRelationSet) {
                mouldRelationSet = new HashSet<>();
            }
            mouldRelationSet.add(mouldCode);
            productMouldRelation.put(specCode, mouldRelationSet);
            productMouldMap.put(constructionProductCode, productMouldRelation);
        });
    }

    /**
     * 获取分厂对应模具关联的SAP个数
     *
     * @param factoryCode
     * @return
     */
    private Map<String, Integer> getMouldRelationCount(String factoryCode) {
        if (StringUtils.isEmpty(factoryCode)) {
            return Collections.emptyMap();
        }
        QueryWrapper<MdmSkuMouldRel> queryRelationWrapper = new QueryWrapper<>();
        queryRelationWrapper.eq("FACTORY_CODE", factoryCode);
        List<MdmSkuMouldRel> relationList = productModelRelationMapper.selectList(queryRelationWrapper);
        if (CollectionUtils.isEmpty(relationList)) {
            return Collections.emptyMap();
        }
        Map<String, Set<String>> mouldRelationMap = new HashMap<>();
        relationList.stream().forEach(mouldRelation -> {
            String mouldCode = mouldRelation.getMouldCode();
            String productCode = mouldRelation.getMaterialCode();
            if (StringUtils.isBlank(mouldCode) || StringUtils.isBlank(productCode)) {
                return;
            }
            Set<String> relationProductSet = mouldRelationMap.get(mouldCode);
            if (null == relationProductSet) {
                relationProductSet = new HashSet<>();
            }
            relationProductSet.add(productCode);
            mouldRelationMap.put(mouldCode, relationProductSet);
        });
        if (CollectionUtils.isEmpty(mouldRelationMap)) {
            return Collections.emptyMap();
        }
        Map<String, Integer> mouldRelationCountMap = new HashMap<>();
        mouldRelationMap.entrySet().stream().forEach(entry -> {
            String mouldCode = entry.getKey();
            Integer count = entry.getValue().size();
            mouldRelationCountMap.put(mouldCode, count);
        });
        return mouldRelationCountMap;
    }

    /**
     * 构建续作物料，及对应续作的模具信息
     *
     * @param mouldRelationCountMap 模具关联个数信息
     * @param specCodeProductMap    续作物料：一个规格代号对应唯一一个物料编码即productCode
     * @param specCodeMouldQtyMap   物料对应的模台数即续作模具 key specCode value:机台编号，模台数
     * @param productMouldMap       规格代号配置的模具关系 key productCode value:specCode,mouldCode
     * @param specCodeEmbryoMap     续作物料：一个规格代号对应唯一一个生胎代号即embryoCode
     * @return 续作模具上的物料集合
     */
    private List<ProductionMouldConfiguration> buildContinueConfiguration(Map<String, Integer> mouldRelationCountMap, Map<String, String> specCodeProductMap, Map<String, Map<String, ProductionMouldConfigurationGroupVo>> specCodeMouldQtyMap, Map<String, Map<String, Set<String>>> productMouldMap, Map<String, String> specCodeEmbryoMap) {
        List<ProductionMouldConfiguration> continueProductList = new ArrayList<>();
        Set<String> isContinueSet = new HashSet<>();
        specCodeProductMap.entrySet().stream().forEach(entry -> {
            String specCode = entry.getKey();
            String productCode = entry.getValue();
            Map<String, ProductionMouldConfigurationGroupVo> vulcanizingMouldQtyMap = specCodeMouldQtyMap.get(specCode);
            if (CollectionUtils.isEmpty(vulcanizingMouldQtyMap)) {
                return;
            }
            Map<String, Set<String>> mouldRelationMap = productMouldMap.get(productCode);
            if (CollectionUtils.isEmpty(mouldRelationMap)) {
                return;
            }
            Set<String> mouldRelationSet = mouldRelationMap.get(specCode);
            if (CollectionUtils.isEmpty(mouldRelationSet)) {
                return;
            }
            List<MouldRelationProductHelper> mouldRelationList = new ArrayList<>();
            mouldRelationSet.stream().forEach(mouldCode -> {
                Integer count = mouldRelationCountMap.get(mouldCode);
                if (null == count || count == 0) {
                    return;
                }
                MouldRelationProductHelper helper = new MouldRelationProductHelper();
                helper.setMouldCode(mouldCode);
                helper.setRelationCount(count);
                mouldRelationList.add(helper);
            });
            //按关联数升序排序
            mouldRelationList.sort(Comparator.comparing(MouldRelationProductHelper::getRelationCount));
            int maxQty = mouldRelationList.size();
            int selectedQty = BigDecimal.ZERO.intValue();
            for (Map.Entry<String, ProductionMouldConfigurationGroupVo> vulcanizingMould : vulcanizingMouldQtyMap.entrySet()) {
                //不可能超出最大
                if (selectedQty >= maxQty) {
                    break;
                }
                ProductionMouldConfigurationGroupVo productionGroup = vulcanizingMould.getValue();
                String productionGroupValue = vulcanizingMould.getKey();
                Integer currentMouldQty = productionGroup.getMoldQty();
                for (int index = 0; index < currentMouldQty; index++) {
                    //排除已经挑选的 拼模时
                    for (int selectIndex = 0; selectIndex < maxQty; selectIndex++) {
                        String mouldCode = mouldRelationList.get(selectIndex).getMouldCode();
                        if (isContinueSet.contains(mouldCode)) {
                            continue;
                        }
                        isContinueSet.add(mouldCode);
                        selectedQty = selectedQty + BigDecimal.ONE.intValue();
                        ProductionMouldConfiguration configuration = new ProductionMouldConfiguration();
                        configuration.setProductCode(productCode);
                        configuration.setMouldCode(mouldCode);
                        configuration.setProductionGroupValue(productionGroupValue);
                        configuration.setMouldQty(currentMouldQty);
                        configuration.setMouldNumber(productionGroup.getMouldNumber());
                        configuration.setSpecCode(specCode);
                        configuration.setEmbryoCode(specCodeEmbryoMap.get(specCode));
                        continueProductList.add(configuration);
                        break;
                    }
                }
            }
        });
        if (CollectionUtils.isEmpty(continueProductList)) {
            return Collections.emptyList();
        }
        Map<String, ProductionMouldConfiguration> clearDuplicateMap = continueProductList.stream().collect(Collectors.toMap(ProductionMouldConfiguration::getDuplicateKey, Function.identity(), (o1, o2) -> o1));
        return clearDuplicateMap.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList());
    }
}
