package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.constant.StringConstant;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.maindata.domain.dto.MouldMonthUseDto;
import com.zlt.aps.maindata.enums.SystemBaseEnums;
import com.zlt.aps.maindata.mapper.*;
import com.zlt.aps.maindata.service.IMdmProductModelRelationService;
import com.zlt.aps.maindata.utils.ScmListUtils;
import com.zlt.aps.monthplan.api.domain.dto.ProductMouldConfigurationParam;
import com.zlt.aps.monthplan.api.domain.dto.ProductMouldRelationConfigurationParam;
import com.zlt.aps.monthplan.api.domain.entity.MdmModelInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmSkuMouldRel;
import com.zlt.aps.monthplan.api.domain.vo.ProductMouldConfigurationVo;
import com.zlt.aps.monthplan.api.domain.vo.ProductMouldInfoVo;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmProductModelRelationServiceImpl.java
 * 描    述：MdmProductModelRelationServiceImplSAP与模具关系业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class MdmProductModelRelationServiceImpl extends AbstractDocService<MdmSkuMouldRel> implements IMdmProductModelRelationService {

    private final MdmProductModelRelationEntityMapper entityMapper;

    private final MdmProductInfoEntityMapper productInfoEntityMapper;

    private final MdmMouldUseStatusEntityMapper mouldUseStatusMapper;

    private final MdmDeviceMaintenancePlanEntityMapper maintenanceMapper;

    private final MdmModelInfoEntityMapper modelInfoMapper;

    @Override
    protected String getDocTypeCode() {
        return "0114-1";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("0114-1");
        return sysDocType;
    }

    @Override
    public String checkUnique(MdmSkuMouldRel docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.productmodelrelation.notUnique"));
        }
        // 关联物料表赋值规格、花纹、品牌
        String productCode = docEntityVO.getMaterialCode();
        if (StringUtils.isNotEmpty(productCode)) {
            LambdaQueryWrapper<MdmProductInfo> wrapper = new LambdaQueryWrapper<MdmProductInfo>()
                    .eq(MdmProductInfo::getProductCode, productCode);
            List<MdmProductInfo> productInfoList = productInfoEntityMapper.selectList(wrapper);
            if (CollectionUtils.isNotEmpty(productInfoList)) {
                MdmProductInfo productInfo = productInfoList.get(0);
                docEntityVO.setMaterialDesc(productInfo.getProductDesc());
//            docEntityVO.setSpecCode(productInfo.getSpecCode());
                docEntityVO.setSpecifications(productInfo.getSpecifications());
                docEntityVO.setPattern(productInfo.getPattern());
                docEntityVO.setBrand(productInfo.getBrand());
                docEntityVO.setMouldCategory(productInfo.getMouldCategory());
            }
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("factoryCode", "materialCode", "mouldCode"));
    }

    @Override
    protected Map<Object, Object> getServiceCheckParams(List<MdmSkuMouldRel> list, List<MdmSkuMouldRel> importList) {
        Map<Object, Object> serviceCheckParams = super.getServiceCheckParams(list, importList);
        // 关联物料表赋值规格、花纹、品牌
        Map<String, MdmProductInfo> productInfoMap = new HashMap<>(16);
        List<String> productCodeList = list.stream().map(MdmSkuMouldRel::getMaterialCode).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(productCodeList)) {
            List<List<String>> splitList = com.zlt.aps.maindata.utils.CollectionUtils.splitList(productCodeList, 100);
            List<MdmProductInfo> productInfoList = new ArrayList<>();
            for (List<String> codeList : splitList) {
                LambdaQueryWrapper<MdmProductInfo> wrapper = new LambdaQueryWrapper<MdmProductInfo>()
                        .in(MdmProductInfo::getProductCode, codeList)
                        .eq(MdmProductInfo::getFactoryCode, FactoryConstant.DEFAULT_FACTORY_CODE);
                productInfoList.addAll(productInfoEntityMapper.selectList(wrapper));
            }

            if (CollectionUtils.isNotEmpty(productInfoList)) {
                productInfoMap = productInfoList.stream().collect(Collectors.toMap(item -> String.join("|", item.getFactoryCode(), item.getProductCode()), Function.identity(), (v1, v2) -> v1));
            }
            serviceCheckParams.put("productInfoMap", productInfoMap);
        }
        return serviceCheckParams;
    }

    @Override
    protected Boolean serviceCheckAndDataHandle(MdmSkuMouldRel importDocEntity, List<ImportErrorLog> importErrorLogs, Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
        if (serviceCheckParams.containsKey("productInfoMap")) {
            Map<String, MdmProductInfo> productInfoMap = (Map<String, MdmProductInfo>) serviceCheckParams.get("productInfoMap");
            String productCode = FactoryConstant.DEFAULT_FACTORY_CODE + "|" + importDocEntity.getMaterialCode();
            if (productInfoMap.containsKey(productCode)) {
                MdmProductInfo productInfo = productInfoMap.get(productCode);
                importDocEntity.setMaterialDesc(productInfo.getProductDesc());
                importDocEntity.setSpecifications(productInfo.getSpecifications());
                importDocEntity.setPattern(productInfo.getPattern());
                importDocEntity.setBrand(productInfo.getBrand());
                importDocEntity.setMouldCategory(productInfo.getMouldCategory());
            }
        }
        return super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
    }

    private Boolean checkOneRowProductModelRef(List<MdmSkuMouldRel> list, Long importLogId, AtomicInteger failureNum, List<MdmSkuMouldRel> importList, List<ImportErrorLog> importErrorLogs, MdmSkuMouldRel info) {
        int rownum;
        List<ImportErrorLog> validated;
        rownum = info.getIndex() + 2;
        validated = ImportExcelValidatedUtils.validated(importLogId, rownum, info);
        if (CollectionUtils.isNotEmpty(validated)) {
            failureNum.getAndIncrement();
            importErrorLogs.addAll(validated);
            return false;
        } else {
            importList.add(info);
            return true;
        }

    }

    public String checkProductModelRelationUnique(MdmSkuMouldRel productModelRelation, List<String> productModelKeysInDB) {
        if (productModelRelation == null) {
            return UserConstants.NOT_UNIQUE;
        }
        if (productModelKeysInDB.contains(productModelRelation.getUnikey())) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 查询规格对应的模具关系
     *
     * @param specCodes
     * @return
     */
    @Override
    public List<MdmSkuMouldRel> queryBySpecCodes(Set<String> specCodes, String factoryCode) {
        // 将 Set 转换为 List，便于切分批次
        List<String> codeList = new ArrayList<>(specCodes);
        //定义最终返回的List
        List<MdmSkuMouldRel> finalList = new ArrayList<>();
        //判断集合的长度是多少 如果超过900条则进行切分查询
        if (codeList.size() > SystemBaseEnums.SPLIT_LENGTH.getCode()) {
            List<List<String>> splitList = ScmListUtils.getSplitList(codeList, SystemBaseEnums.SPLIT_LENGTH.getCode());
            //将多次查询的结果汇总到finalList中
            for (List<String> splitItemList : splitList) {
                List<MdmSkuMouldRel> queryList = entityMapper.queryBySpecCodes(factoryCode, splitItemList);
                finalList.addAll(queryList);
            }
        } else {
            finalList = entityMapper.queryBySpecCodes(factoryCode, codeList);
        }
        return finalList;
    }

    @Override
    public ProductMouldInfoVo getProductMatchMould(ProductMouldConfigurationParam queryParam) {
        if (null == queryParam || StringUtils.isBlank(queryParam.getProductCode()) || StringUtils.isBlank(queryParam.getFactoryCode()) || null == queryParam.getYear() || null == queryParam.getMonth()) {
            return null;
        }
        ProductMouldInfoVo productMouldInfo = new ProductMouldInfoVo();
        String productCode = queryParam.getProductCode();
        String factoryCode = queryParam.getFactoryCode();
        //获取物料基础信息
        QueryWrapper<MdmProductInfo> productInfoQueryWrapper = new QueryWrapper<>();
        productInfoQueryWrapper.eq("FACTORY_CODE", factoryCode);
        productInfoQueryWrapper.eq("PRODUCT_CODE", productCode);
        productInfoQueryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        MdmProductInfo productInfo = productInfoEntityMapper.selectOne(productInfoQueryWrapper);
        if (null == productInfo) {
            return null;
        }
        productMouldInfo.setBrand(productInfo.getBrand());
        QueryWrapper<MdmSkuMouldRel> relationQueryWrapper = new QueryWrapper<>();
        relationQueryWrapper.eq("PRODUCT_CODE", productCode);
        List<MdmSkuMouldRel> relationList = entityMapper.selectList(relationQueryWrapper);
        if (CollectionUtils.isEmpty(relationList)) {
            productMouldInfo.setMouldConfigurationList(Collections.emptyList());
            return productMouldInfo;
        }
        Integer year = queryParam.getYear();
        Integer month = queryParam.getMonth();
        List<String> mouldCodeList = relationList.stream().map(MdmSkuMouldRel::getMouldCode).collect(Collectors.toList());
        List<MouldMonthUseDto> useStatusList = mouldUseStatusMapper.getMonthUsedMould(factoryCode, year, month, mouldCodeList);
        List<MouldMonthUseDto> maintenanceList = maintenanceMapper.getMonthMaintenanceMould(factoryCode, year, month, mouldCodeList);
        Map<String, String> mouldCodeMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(useStatusList)) {
            useStatusList.forEach(useStatus -> mouldCodeMap.put(useStatus.getMouldCode(), useStatus.getMouldNo()));
        }
        if (CollectionUtils.isNotEmpty(maintenanceList)) {
            maintenanceList.forEach(maintenance -> mouldCodeMap.put(maintenance.getMouldCode(), maintenance.getMouldNo()));
        }
        if (org.springframework.util.CollectionUtils.isEmpty(mouldCodeMap)) {
            productMouldInfo.setMouldConfigurationList(Collections.emptyList());
            return productMouldInfo;
        }
        Map<String, List<MdmSkuMouldRel>> groupMap = relationList.stream().collect(Collectors.groupingBy(MdmSkuMouldRel::getMouldCode));
        Map<String, Set<String>> configurationMap = new HashMap<>();
        mouldCodeMap.entrySet().forEach(entry -> {
            String mouldNo = entry.getValue();
            String mouldCode = entry.getKey();
            if (!groupMap.containsKey(mouldCode)) {
                return;
            }
            List<MdmSkuMouldRel> relationSpecList = groupMap.get(mouldCode);
            if (CollectionUtils.isEmpty(relationSpecList)) {
                return;
            }
            Set<String> specCodeSet = configurationMap.get(mouldNo);
            if (null == specCodeSet) {
                specCodeSet = new HashSet<>();
            }
            Set<String> relationSpecSet = relationSpecList.stream().map(MdmSkuMouldRel::getSpecCode).collect(Collectors.toSet());
            specCodeSet.addAll(relationSpecSet);
            configurationMap.put(mouldNo, specCodeSet);
        });
        if (org.springframework.util.CollectionUtils.isEmpty(configurationMap)) {
            productMouldInfo.setMouldConfigurationList(Collections.emptyList());
            return productMouldInfo;
        }
        List<ProductMouldConfigurationVo> configurationList = new ArrayList<>();
        configurationMap.entrySet().forEach(entry -> {
            ProductMouldConfigurationVo configuration = new ProductMouldConfigurationVo();
            configuration.setMouldNo(entry.getKey());
            configuration.setSpecCodeList(new ArrayList<>(entry.getValue()));
            configurationList.add(configuration);
        });
        productMouldInfo.setMouldConfigurationList(configurationList);
        return productMouldInfo;
    }

    @Override
    public AjaxResult configurationMouldRelation(ProductMouldRelationConfigurationParam configuration) {
        if (!requirementCheck(configuration)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.param.productModelRelation.noEmpty"));
        }
        String productCode = configuration.getProductCode();
        String factoryCode = configuration.getFactoryCode();
        MdmProductInfo productInfo = getProductInfo(productCode, factoryCode);
        if (null == productInfo) {
            String productCodeError = I18nUtil.getMessage("ui.data.column.mdmStockUpPlan.productCode.notExist");
            return AjaxResult.error(String.format(productCodeError, productCode));
        }
        String mouldNo = configuration.getMouldNo();
        Integer mouldNumber = configuration.getMouldNumber();
        List<MdmModelInfo> modelInfoList = getModelInfoList(mouldNo, factoryCode);
        if (CollectionUtils.isEmpty(modelInfoList) || modelInfoList.size() < mouldNumber) {
            String mouldNoError = I18nUtil.getMessage("ui.data.param.productModelRelation.mouldNo");
            return AjaxResult.error(String.format(mouldNoError, mouldNo, mouldNumber));
        }
        String specCodeConfiguration = configuration.getSpecCode();
        List<String> specCodeList = new ArrayList<>(new HashSet<>(Arrays.asList(specCodeConfiguration.split(StringConstant.COMMA))));
        List<MdmSkuMouldRel> relationList = new ArrayList<>();
        for (String specCode : specCodeList) {
            for (int index = 0; index < mouldNumber; index++) {
                MdmModelInfo modelInfo = modelInfoList.get(index);
                MdmSkuMouldRel relation = buildConfiguration(modelInfo, productInfo);
                relation.setMaterialCode(productCode);
                relation.setFactoryCode(factoryCode);
                relation.setSpecCode(specCode);
                relationList.add(relation);
            }
        }
        //先删除旧的
        QueryWrapper<MdmSkuMouldRel> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("FACTORY_CODE", factoryCode);
        deleteWrapper.eq("PRODUCT_CODE", productCode);
        entityMapper.delete(deleteWrapper);
        //重新建立关系
        save(relationList);
        return AjaxResult.success();
    }

    /**
     * 校验参数是否为空
     *
     * @param configuration
     * @return
     */
    private boolean requirementCheck(ProductMouldRelationConfigurationParam configuration) {
        if (StringUtils.isBlank(configuration.getFactoryCode()) || StringUtils.isBlank(configuration.getProductCode())) {
            return false;
        }
        if (StringUtils.isBlank(configuration.getMouldNo()) || null == configuration.getMouldNumber()) {
            return false;
        }
        if (StringUtils.isBlank(configuration.getSpecCode())) {
            return false;
        }
        if (configuration.getMouldNumber() <= BigDecimal.ZERO.intValue()) {
            return false;
        }
        return true;
    }


    /**
     * 获取物料信息
     *
     * @param productCode 物料编码
     * @param factoryCode 工厂编码
     * @return
     */
    private MdmProductInfo getProductInfo(String productCode, String factoryCode) {
        if (StringUtils.isBlank(productCode) || StringUtils.isBlank(factoryCode)) {
            return null;
        }
        QueryWrapper<MdmProductInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("PRODUCT_CODE", productCode);
        queryWrapper.eq("FACTORY_CODE", factoryCode);
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        return productInfoEntityMapper.selectOne(queryWrapper);
    }

    /**
     * 获取模具信息
     *
     * @param mouldNo
     * @param factoryCode
     * @return
     */
    private List<MdmModelInfo> getModelInfoList(String mouldNo, String factoryCode) {
        if (StringUtils.isBlank(mouldNo) || StringUtils.isBlank(factoryCode)) {
            return Collections.emptyList();
        }
        QueryWrapper<MdmModelInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("MOULD_NO", mouldNo);
        queryWrapper.eq("FACTORY_CODE", factoryCode);
        return modelInfoMapper.selectList(queryWrapper);
    }

    /**
     * 构建物料模具配置信息
     *
     * @param modelInfo   模具信息
     * @param productInfo 物料信息
     * @return
     */
    private MdmSkuMouldRel buildConfiguration(MdmModelInfo modelInfo, MdmProductInfo productInfo) {
        MdmSkuMouldRel relation = new MdmSkuMouldRel();
        //模具信息
        relation.setMouldCode(modelInfo.getMouldCode());
        relation.setMouldNo(modelInfo.getMouldNo());
        //物料信息
        relation.setMaterialDesc(productInfo.getProductDesc());
        relation.setMouldCategory(productInfo.getMouldCategory());
        relation.setSpecifications(productInfo.getSpecifications());
        relation.setPattern(productInfo.getPattern());
        relation.setBrand(productInfo.getBrand());
        return relation;
    }

    /**
     * 抓取MES数据
     *
     * @return 结果
     */
    @Override
    public AjaxResult mesCapture() {
        // steve's TODO 待接口完善后补充
        return AjaxResult.success();
    }
}

