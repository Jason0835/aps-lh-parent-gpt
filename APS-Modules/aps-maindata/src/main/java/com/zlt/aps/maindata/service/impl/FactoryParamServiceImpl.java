package com.zlt.aps.maindata.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.constant.StringConstant;
import com.tlt.aps.enums.ProductTypeEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.maindata.domain.dto.ProductBrandDto;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.mapper.FactoryParamMapper;
import com.zlt.aps.maindata.mapper.FactoryParamTemplateMapper;
import com.zlt.aps.maindata.service.IFactoryParamService;
import com.zlt.aps.maindata.utils.FactoryParamUtils;
import com.zlt.aps.monthplan.api.domain.entity.FactoryParam;
import com.zlt.aps.monthplan.api.domain.entity.FactoryParamTemplate;
import com.zlt.aps.monthplan.api.domain.vo.FactoryParamVo;
import com.zlt.common.utils.PubUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FactoryParamServiceImpl.java
 * 描    述：FactoryParamServiceImpl系统参数（排产设定）业务层处理
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FactoryParamServiceImpl extends ServiceImpl<FactoryParamMapper, FactoryParam> implements IFactoryParamService {

    private final FactoryParamMapper factoryParamMapper;

    private final FactoryParamTemplateMapper factoryParamTemplateMapper;

    @Override
    public List<FactoryParam> getFacParamByList(FactoryParam entity) {
        return factoryParamMapper.getFacParamList(entity);
    }

    @Override
    public List<FactoryParam> getFactoryParamByCondition(String factoryCode, String productTypeCode, List<String> paramCodeList) {
        if (StringUtils.isBlank(factoryCode) || CollectionUtils.isEmpty(paramCodeList)) {
            return Collections.emptyList();
        }
        QueryWrapper<FactoryParam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", factoryCode);
        queryWrapper.in("PARAM_CODE", paramCodeList);
        queryWrapper.eq(StringUtils.isNotBlank(productTypeCode), "PRODUCT_TYPE_CODE", productTypeCode);
        return factoryParamMapper.selectList(queryWrapper);
    }

    @Override
    public AjaxResult copy(FactoryParamVo vo) {
        if (StringUtils.isBlank(vo.getFactoryCode()) || StringUtils.isBlank(vo.getProductTypeCode())
                || StringUtils.isBlank(vo.getFactoryCode1()) || StringUtils.isBlank(vo.getProductTypeCode1())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.config.error.value.isnull"));
        }

        //参数同步 chad 20220517
        syncSysParamtersFromTemplate(vo.getFactoryCode(), vo.getProductTypeCode());
        // merge分厂排产设定
        mergeFactoryParam(vo);
        return AjaxResult.success();
    }

    @Override
    public void syncSysParamtersFromTemplate(String factoryCode, String productTypeCode) {
        LambdaQueryWrapper<FactoryParamTemplate> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(FactoryParamTemplate::getIsDelete, ApsConstant.DEL_FLAG_NORMAL);
        List<FactoryParamTemplate> sysParaTemplateEntities = factoryParamTemplateMapper.selectList(lambdaQueryWrapper);
        if (PubUtil.isEmpty(sysParaTemplateEntities)) {
            return;
        }
        LambdaQueryWrapper<FactoryParam> factoryParamLambdaQueryWrapper = new LambdaQueryWrapper<>();
        factoryParamLambdaQueryWrapper.eq(FactoryParam::getIsDelete, ApsConstant.DEL_FLAG_NORMAL);
        factoryParamLambdaQueryWrapper.eq(FactoryParam::getFactoryCode, factoryCode);
        factoryParamLambdaQueryWrapper.eq(FactoryParam::getProductTypeCode, productTypeCode);
        List<FactoryParam> facParamEntities = getBaseMapper().selectList(factoryParamLambdaQueryWrapper);
        if (PubUtil.isEmpty(facParamEntities)) {
            facParamEntities = new ArrayList<>();
        }

        Map<String, FactoryParam> paramEntityMap = facParamEntities.stream()
                .collect(Collectors.toMap(FactoryParam::getParamCode, Function.identity(), (v1, v2) -> v2));

        List<FactoryParam> paramEntities = new ArrayList<>();
        sysParaTemplateEntities.stream().forEach(sysParaTemplateEntity -> {
            FactoryParam facParamEntity = paramEntityMap.get(sysParaTemplateEntity.getParamCode());
            if (PubUtil.isEmpty(facParamEntity)) {
                facParamEntity = new FactoryParam();
                BeanUtils.copyProperties(sysParaTemplateEntity, facParamEntity);
                facParamEntity.setId(null);
                facParamEntity.setFactoryCode(factoryCode);
                facParamEntity.setProductTypeCode(productTypeCode);
                facParamEntity.setParamValue(sysParaTemplateEntity.getDefauleValue());
                paramEntities.add(facParamEntity);
                return;
            }

            if (!facParamEntity.getParamName().equals(sysParaTemplateEntity.getParamName())
                    || !facParamEntity.getDataType().equals(sysParaTemplateEntity.getDataType())
                    || PubUtil.isNotEmpty(sysParaTemplateEntity.getDefauleValue()) && !sysParaTemplateEntity.getDefauleValue().equals(facParamEntity.getDefauleValue())) {
                facParamEntity.setParamName(sysParaTemplateEntity.getParamName());
                facParamEntity.setDataType(sysParaTemplateEntity.getDataType());
                facParamEntity.setDefauleValue(sysParaTemplateEntity.getDefauleValue());
                paramEntities.add(facParamEntity);
            }
        });

        saveBatch(paramEntities);
    }

    /**
     * 查询唯一的工厂系统参数
     */
    @Override
    public FactoryParam getFacParamSingle(FactoryParam factoryParam) {
        List<FactoryParam> paramList = factoryParamMapper.selectList(Wrappers.lambdaQuery(FactoryParam.class)
                .eq(FactoryParam::getFactoryCode, factoryParam.getFactoryCode())
                .eq(FactoryParam::getParamCode, factoryParam.getParamCode())
                .eq(FactoryParam::getProductTypeCode, factoryParam.getProductTypeCode())
                .eq(FactoryParam::getIsDelete, ApsConstant.DEL_FLAG_NORMAL));
        if (CollectionUtils.isEmpty(paramList)) {
            return null;
        }
        return paramList.get(0);
    }

    @Override
    public BigDecimal getSingleAddCuringTime(String factoryCode) {
        if (StringUtils.isBlank(factoryCode)) {
            return BigDecimal.ZERO;
        }
        FactoryParam addCuringTimeParam = new FactoryParam();
        addCuringTimeParam.setFactoryCode(factoryCode);
        addCuringTimeParam.setParamCode(FactoryConstant.SYS_PARAM_INTERVAL_TIME_OF_EMBRYO_EXCHANGE);
        FactoryParam addCuringTime = getFacParamSingle(addCuringTimeParam);
        if (null == addCuringTime && StringUtils.isBlank(addCuringTime.getParamValue())) {
            return BigDecimal.ZERO;
        }
        BigDecimal addCuringTimeValue = BigDecimal.valueOf((Integer) FactoryParamUtils.getParamValue(addCuringTime));
        return addCuringTimeValue.multiply(BigDecimal.valueOf(FactoryConstant.MINUTE_SECOND));
    }

    @Override
    public BigDecimal getChangeProductConsumeTime(String factoryCode) {
        if (StringUtils.isBlank(factoryCode)) {
            return BigDecimal.ZERO;
        }
        FactoryParam changeConsumeParam = new FactoryParam();
        changeConsumeParam.setFactoryCode(factoryCode);
        changeConsumeParam.setParamCode(FactoryConstant.SYS_CHANGE_PRODUCT_SUB_HOURS);
        FactoryParam changeConsumeValue = getFacParamSingle(changeConsumeParam);
        if (null == changeConsumeValue || StringUtils.isBlank(changeConsumeValue.getParamValue())) {
            return BigDecimal.ZERO;
        }
        BigDecimal consumeTimeValue = BigDecimal.valueOf((Integer) FactoryParamUtils.getParamValue(changeConsumeParam));
        return consumeTimeValue.multiply(BigDecimal.valueOf(FactoryConstant.HOUR_SECOND));
    }

    @Override
    public BigDecimal getDayMaxCuringTime(String factoryCode) {
        if (StringUtils.isBlank(factoryCode)) {
            return BigDecimal.ZERO;
        }
        FactoryParam dayWorkParam = new FactoryParam();
        dayWorkParam.setFactoryCode(factoryCode);
        dayWorkParam.setParamCode(FactoryConstant.SYS_PARAM_DAY_WORK_HOURS);
        FactoryParam dayWorkHourValue = getFacParamSingle(dayWorkParam);
        BigDecimal dayWorkHour = BigDecimal.valueOf(FactoryConstant.MAX_DAY_HOURS);
        if (null != dayWorkHourValue && !StringUtils.isBlank(dayWorkHourValue.getParamValue())) {
            dayWorkHour = (BigDecimal) FactoryParamUtils.getParamValue(dayWorkHourValue);
        }
        return dayWorkHour.multiply(BigDecimal.valueOf(FactoryConstant.HOUR_SECOND));
    }

    @Override
    public Map<String, Integer> getChangeSummerMonth(String factoryCode) {
        if (StringUtils.isBlank(factoryCode)) {
            return Collections.emptyMap();
        }
        QueryWrapper<FactoryParam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", factoryCode);
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        queryWrapper.in("PARAM_CODE", Arrays.asList(new String[]{FactoryConstant.SYS_PARAM_SUMMER_MONTH, FactoryConstant.SYS_PARAM_WINTER_MONTH}));
        List<FactoryParam> dataList = factoryParamMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(dataList)) {
            return Collections.emptyMap();
        }
        Map<String, Integer> configurationMap = new HashMap<>();
        dataList.forEach(configuration -> {
            if (StringUtils.isBlank(configuration.getParamValue())) {
                return;
            }
            configurationMap.put(configuration.getParamCode(), (Integer) FactoryParamUtils.getParamValue(configuration));
        });
        return configurationMap;
    }

    @Override
    public Integer getInformalConstructionCuringTime(String factoryCode) {
        if (StringUtils.isBlank(factoryCode)) {
            return BigDecimal.ZERO.intValue();
        }
        FactoryParam informalConstruction = new FactoryParam();
        informalConstruction.setFactoryCode(factoryCode);
        informalConstruction.setParamCode(FactoryConstant.SYS_PARAM_INFORMAL_CONSTRUCTION);
        informalConstruction.setProductTypeCode(ProductTypeEnum.SEMI_STEEL.getValue());
        FactoryParam informalConstructionParam = getFacParamSingle(informalConstruction);
        if (null == informalConstructionParam || StringUtils.isBlank(informalConstructionParam.getParamValue())) {
            return BigDecimal.ZERO.intValue();
        }
        Integer qty = (Integer) FactoryParamUtils.getParamValue(informalConstructionParam);
        BigDecimal dayWorkHour = BigDecimal.valueOf(FactoryConstant.MAX_DAY_HOURS).multiply(BigDecimal.valueOf(FactoryConstant.HOUR_SECOND));
        return dayWorkHour.divide(BigDecimal.valueOf(qty), 0, RoundingMode.DOWN).intValue();
    }

    @Override
    public Set<String> getNoStockUpPlanBrand(String factoryCode) {
        if (StringUtils.isBlank(factoryCode)) {
            return Collections.emptySet();
        }
        FactoryParam noStockUpPlanBrandConfiguration = new FactoryParam();
        noStockUpPlanBrandConfiguration.setFactoryCode(factoryCode);
        noStockUpPlanBrandConfiguration.setParamCode(FactoryConstant.SYS_PARAM_EXPORT_OEM_BRAND);
        noStockUpPlanBrandConfiguration.setProductTypeCode(ProductTypeEnum.SEMI_STEEL.getValue());
        FactoryParam noStockUpPlanBrandParam = getFacParamSingle(noStockUpPlanBrandConfiguration);
        if (null == noStockUpPlanBrandParam || StringUtils.isBlank(noStockUpPlanBrandParam.getParamValue())) {
            return Collections.emptySet();
        }
        String brandName = noStockUpPlanBrandParam.getParamValue();
        List<String> brandNameList = Arrays.asList(brandName.split(StringConstant.COMMA));
        if (CollectionUtils.isEmpty(brandNameList)) {
            return Collections.emptySet();
        }
        List<ProductBrandDto> brandParamList = factoryParamMapper.getParamByBrandList(brandNameList);
        if (CollectionUtils.isEmpty(brandParamList)) {
            return Collections.emptySet();
        }
        return brandParamList.stream().map(ProductBrandDto::getBrand).collect(Collectors.toSet());
    }

    @Override
    public Set<String> getForeignOemBrand(String factoryCode) {
        if (StringUtils.isBlank(factoryCode)) {
            return Collections.emptySet();
        }
        FactoryParam foreignOemBrandConfiguration = new FactoryParam();
        foreignOemBrandConfiguration.setFactoryCode(factoryCode);
        foreignOemBrandConfiguration.setParamCode(FactoryConstant.SYS_PARAM_FOREIGN_OEM_BRAND);
        foreignOemBrandConfiguration.setProductTypeCode(ProductTypeEnum.SEMI_STEEL.getValue());
        FactoryParam foreignOemBrandParam = getFacParamSingle(foreignOemBrandConfiguration);
        if (null == foreignOemBrandParam || StringUtils.isBlank(foreignOemBrandParam.getParamValue())) {
            return Collections.emptySet();
        }
        String brandName = foreignOemBrandParam.getParamValue();
        List<String> brandNameList = Arrays.asList(brandName.split(StringConstant.COMMA));
        if (CollectionUtils.isEmpty(brandNameList)) {
            return Collections.emptySet();
        }
        List<ProductBrandDto> brandParamList = factoryParamMapper.getParamByBrandList(brandNameList);
        if (CollectionUtils.isEmpty(brandParamList)) {
            return Collections.emptySet();
        }
        return brandParamList.stream().map(ProductBrandDto::getBrand).collect(Collectors.toSet());
    }

    @Override
    public boolean isOpenNoSubmitStockUp(String factoryCode) {
        if (StringUtils.isBlank(factoryCode)) {
            return false;
        }
        FactoryParam openNoSubmitStockUpConfiguration = new FactoryParam();
        openNoSubmitStockUpConfiguration.setFactoryCode(factoryCode);
        openNoSubmitStockUpConfiguration.setParamCode(FactoryConstant.SYS_PARAM_OPEN_NO_SUBMIT_STOCK_UP);
        openNoSubmitStockUpConfiguration.setProductTypeCode(ProductTypeEnum.SEMI_STEEL.getValue());
        FactoryParam openNoSubmitStockUpParam = getFacParamSingle(openNoSubmitStockUpConfiguration);
        if (null == openNoSubmitStockUpParam || StringUtils.isBlank(openNoSubmitStockUpParam.getParamValue())) {
            return false;
        }
        return FactoryConstant.YES_VALUE.equalsIgnoreCase(openNoSubmitStockUpParam.getParamValue());
    }

    @Override
    public String getDomesticStockUpType(String factoryCode) {
        if (StringUtils.isBlank(factoryCode)) {
            return "";
        }
        FactoryParam domesticStockUpTypeConfiguration = new FactoryParam();
        domesticStockUpTypeConfiguration.setFactoryCode(factoryCode);
        domesticStockUpTypeConfiguration.setParamCode(FactoryConstant.SYS_PARAM_DOMESTIC_STOCK_UP_TYPE);
        domesticStockUpTypeConfiguration.setProductTypeCode(ProductTypeEnum.SEMI_STEEL.getValue());
        FactoryParam domesticStockUpTypeParam = getFacParamSingle(domesticStockUpTypeConfiguration);
        if (null == domesticStockUpTypeParam || StringUtils.isBlank(domesticStockUpTypeParam.getParamValue())) {
            return "";
        }
        return domesticStockUpTypeParam.getParamValue();
    }

    @Override
    public Integer getStockUpLastMonth(String factoryCode) {
        if (StringUtils.isBlank(factoryCode)) {
            return BigDecimal.ZERO.intValue();
        }
        FactoryParam stockUpMonthConfiguration = new FactoryParam();
        stockUpMonthConfiguration.setFactoryCode(factoryCode);
        stockUpMonthConfiguration.setParamCode(FactoryConstant.SYS_PARAM_STOCK_UP_MONTH);
        stockUpMonthConfiguration.setProductTypeCode(ProductTypeEnum.SEMI_STEEL.getValue());
        FactoryParam stockUpMonthParam = getFacParamSingle(stockUpMonthConfiguration);
        if (null == stockUpMonthParam || StringUtils.isBlank(stockUpMonthParam.getParamValue())) {
            return BigDecimal.ZERO.intValue();
        }
        Integer month = (Integer) FactoryParamUtils.getParamValue(stockUpMonthParam);
        if (month < BigDecimal.ONE.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        return month;
    }

    @Override
    public Integer getMonthStartDay(String factoryCode, ProductTypeEnum productType) {
        if (StringUtils.isBlank(factoryCode) || null == productType) {
            return null;
        }
        FactoryParam monthStartDayConfiguration = new FactoryParam();
        monthStartDayConfiguration.setFactoryCode(factoryCode);
        monthStartDayConfiguration.setParamCode(MonthPlanEnums.PRODUCTION_CYCLE_START.getCode());
        monthStartDayConfiguration.setProductTypeCode(productType.getValue());
        FactoryParam monthStartDayParam = getFacParamSingle(monthStartDayConfiguration);
        if (null == monthStartDayParam || StringUtils.isBlank(monthStartDayParam.getParamValue())) {
            return null;
        }
        return (Integer) FactoryParamUtils.getParamValue(monthStartDayParam);
    }

    /**
     * merge分厂排产设定
     *
     * @param vo
     */
    private void mergeFactoryParam(FactoryParamVo vo) {
        List<FactoryParam> insertList = new ArrayList<>();
        List<FactoryParam> updateList = new ArrayList<>();
        LambdaQueryWrapper<FactoryParam> factoryParamLambdaQueryWrapper = new LambdaQueryWrapper<>();
        factoryParamLambdaQueryWrapper.eq(FactoryParam::getIsDelete, ApsConstant.DEL_FLAG_NORMAL);
        factoryParamLambdaQueryWrapper.eq(FactoryParam::getFactoryCode, vo.getFactoryCode());
        factoryParamLambdaQueryWrapper.eq(FactoryParam::getProductTypeCode, vo.getProductTypeCode());
        List<FactoryParam> factoryParams = getBaseMapper().selectList(factoryParamLambdaQueryWrapper);
        factoryParamLambdaQueryWrapper.clear();
        factoryParams.stream().forEach(factoryParam -> {
            factoryParam.setFactoryCode(vo.getFactoryCode1());
            factoryParam.setProductTypeCode(vo.getProductTypeCode1());
            factoryParamLambdaQueryWrapper.eq(FactoryParam::getIsDelete, ApsConstant.DEL_FLAG_NORMAL);
            factoryParamLambdaQueryWrapper.eq(FactoryParam::getFactoryCode, factoryParam.getFactoryCode());
            factoryParamLambdaQueryWrapper.eq(FactoryParam::getProductTypeCode, factoryParam.getProductTypeCode());
            factoryParamLambdaQueryWrapper.eq(FactoryParam::getParamCode, factoryParam.getParamCode());
            List<FactoryParam> existingRecords = getBaseMapper().selectList(factoryParamLambdaQueryWrapper);
            if (CollectionUtils.isEmpty(existingRecords)) {
                factoryParam.setId(null);
                factoryParam.setCreateBy(SecurityUtils.getUsername());
                factoryParam.setUpdateBy(SecurityUtils.getUsername());
                // 新增成型定点机台记录
                insertList.add(factoryParam);
            } else {
                // 更新已存在的成型定点机台记录
                updateExistingRecords(factoryParam, existingRecords, updateList);
            }
        });
        if (CollectionUtils.isNotEmpty(insertList)) {
            saveBatch(insertList);
        }
        if (CollectionUtils.isNotEmpty(updateList)) {
            updateBatchById(updateList);
        }
    }


    private void updateExistingRecords(FactoryParam factoryParam, List<FactoryParam> existingRecords, List<FactoryParam> updateList) {
        for (FactoryParam record : existingRecords) {
            // 根据月份、库位类型映射赋值订单数、销售数
            factoryParam.setId(record.getId());
            updateList.add(factoryParam);
        }
    }
}
