package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.FactoryConstant;
import com.zlt.aps.maindata.mapper.FactoryParamMapper;
import com.zlt.aps.maindata.mapper.MdmMaterialInfoEntityMapper;
import com.zlt.aps.maindata.mapper.ProductMinConfigurationMapper;
import com.zlt.aps.maindata.service.IProductMinConfigurationService;
import com.zlt.aps.monthplan.api.domain.entity.FactoryParam;
import com.zlt.aps.monthplan.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.monthplan.api.domain.entity.ProductMinConfiguration;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ProductMinConfigurationServiceImpl.java
 * 描    述：ProductMinConfigurationServiceImpl最小批量业务层处理
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
public class ProductMinConfigurationServiceImpl extends AbstractDocService<ProductMinConfiguration> implements IProductMinConfigurationService {

    @Autowired
    private FactoryParamMapper factoryParamMapper;

    @Autowired
    private MdmMaterialInfoEntityMapper mdmMaterialInfoEntityMapper;

    private final ProductMinConfigurationMapper productMinConfigurationMapper;

    public ProductMinConfigurationServiceImpl(ProductMinConfigurationMapper productMinConfigurationMapper) {
        this.productMinConfigurationMapper = productMinConfigurationMapper;
    }

    @Override
    public List<ProductMinConfiguration> getConfigurationList() {
        QueryWrapper<ProductMinConfiguration> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("FACTORY_CODE", "PRODUCT_CODE");
        return productMinConfigurationMapper.selectList(queryWrapper);
    }

    @Override
    protected String getDocTypeCode() {
        return "0138";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("0138");
        return sysDocType;
    }

    @Override
    public String checkUnique(ProductMinConfiguration docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new RuntimeException(I18nUtil.getMessage("ui.data.alert.productMinConfiguration.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("factoryCode", "productCode", "productType"));
    }

    @Override
    protected Map<Object, Object> getServiceCheckParams(List<ProductMinConfiguration> list, List<ProductMinConfiguration> importList) {
        Map<Object, Object> serviceCheckParams = super.getServiceCheckParams(list, importList);
        List<String> productTypeList = new ArrayList<>();
        List<String> productCodeList = new ArrayList<>();
        for (ProductMinConfiguration productMinConfiguration : list) {
            productCodeList.add(productMinConfiguration.getProductCode());
            productTypeList.add(productMinConfiguration.getProductType());
        }
        if (CollectionUtils.isNotEmpty(productTypeList)) {
            LambdaQueryWrapper<FactoryParam> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(FactoryParam::getFactoryCode, FactoryConstant.DEFAULT_FACTORY_CODE);
            wrapper.eq(FactoryParam::getParamCode, FactoryConstant.DEFAULT_UP_WATER_LEVEL);
            wrapper.in(FactoryParam::getProductTypeCode, productTypeList);
            List<FactoryParam> factoryParams = factoryParamMapper.selectList(wrapper);
            Map<String, String> map = factoryParams.stream().collect(Collectors
                    .toMap(item -> String.join("|", item.getFactoryCode(), item.getProductTypeCode()),
                            item -> StringUtils.defaultIfBlank(item.getParamValue(), item.getDefauleValue()), (s1, s2) -> s1));
            serviceCheckParams.put(FactoryConstant.DEFAULT_UP_WATER_LEVEL, map);

            LambdaQueryWrapper<MdmMaterialInfo> productInfoWrapper = new LambdaQueryWrapper<>();
            productInfoWrapper.in(MdmMaterialInfo::getMaterialCode, productCodeList);
            List<MdmMaterialInfo> productInfoList = mdmMaterialInfoEntityMapper.selectList(productInfoWrapper);
            Map<String, String> productInfoMap = productInfoList.stream().collect(Collectors.toMap(MdmMaterialInfo::getMaterialCode, MdmMaterialInfo::getMaterialDesc, (s1, s2) -> s1));
            serviceCheckParams.put("productInfoMap", productInfoMap);
        }

        return serviceCheckParams;
    }

    @Override
    protected Boolean serviceCheckAndDataHandle(ProductMinConfiguration importDocEntity, List<ImportErrorLog> importErrorLogs, Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
//        String errorMsg = I18nUtil.getMessage("ui.data.alert.productMinConfiguration.notUnique");
//        ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.REPEAT.getCode(), errorRowNum, String.format(errorMsg, errorRowNum), importErrorLogs);

        if (serviceCheckParams.containsKey(FactoryConstant.DEFAULT_UP_WATER_LEVEL)) {
            Map<String, String> map = (Map<String, String>) serviceCheckParams.get(FactoryConstant.DEFAULT_UP_WATER_LEVEL);
            String defaultUpwardControlWaterLevel = map.getOrDefault(String.join("|", importDocEntity.getFactoryCode(), importDocEntity.getProductType()), "300");
            if (importDocEntity.getUpQty() == null) {
                importDocEntity.setUpQty(Integer.valueOf(defaultUpwardControlWaterLevel));
            }
        }

        if (serviceCheckParams.containsKey("productInfoMap")) {
            Map<String, String> productInfoMap = (Map<String, String>) serviceCheckParams.get("productInfoMap");
            String productDesc = productInfoMap.getOrDefault(importDocEntity.getProductCode(), "");
            importDocEntity.setProductDesc(productDesc);
        }

        return super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
    }
}

