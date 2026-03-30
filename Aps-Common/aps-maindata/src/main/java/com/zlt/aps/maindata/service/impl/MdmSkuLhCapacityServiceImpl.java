package com.zlt.aps.maindata.service.impl;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.enums.ProductTypeEnum;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.mapper.MdmSkuLhCapacityEntityMapper;
import com.zlt.aps.maindata.service.IFactoryParamService;
import com.zlt.aps.maindata.service.IMdmSkuLhCapacityService;
import com.zlt.aps.mp.api.domain.entity.FactoryParam;
import com.zlt.aps.mp.api.domain.entity.MdmSkuLhCapacity;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmSkuLhCapacityServiceImpl.java
 * 描    述：MdmSkuLhCapacityServiceImplSKU日硫化产能业务层处理
 *@author zlt
 *@date 2025-12-08
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class MdmSkuLhCapacityServiceImpl extends AbstractDocService<MdmSkuLhCapacity>  implements IMdmSkuLhCapacityService {

    @Autowired
    private MdmSkuLhCapacityEntityMapper mapper;

    @Autowired
    private IFactoryParamService factoryParamService;

    @Override
    protected String getDocTypeCode() {
        return "MDM0135";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MDM0135");
        return sysDocType;
    }

    @Override
    public int save(MdmSkuLhCapacity docEntityVO) {
        docEntityVO.setBaseVale(null);
        this.checkUnique(docEntityVO);
        // 计算APS日硫化量
        List<MdmSkuLhCapacity> mdmSkuLhCapacityList = new ArrayList<>();
        mdmSkuLhCapacityList.add(docEntityVO);
        calculateApsCapacity(mdmSkuLhCapacityList);
        return baseDao.save(docEntityVO);
    }

    @Override
    public String checkUnique(MdmSkuLhCapacity docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mdmSkuLhCapacity.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return new ArrayList<>(Arrays.asList("factoryCode", "materialCode"));
    }


    @Override
    public AjaxResult importData(List<MdmSkuLhCapacity> list, boolean updateSupport, Long importLogId) {
        // 计算APS日硫化量
        calculateApsCapacity(list);
        return super.importData(list, updateSupport, importLogId);
    }

    /**
     * 计算APS日硫化量
     */
    private void calculateApsCapacity(List<MdmSkuLhCapacity> sourceList) {
        if (PubUtil.isEmpty(sourceList)) {
            return;
        }

        int paramValue = getParamValue();

        /**
         * 模具产能：向下取整（ 24 * 60 * 60 /（ 硫化总时间（s）+ 机械动作时间（s）))
         * APS日硫化产能计算：模具产能 * 2
         */
        sourceList.stream()
                .filter(skuCapacity -> {
                    Integer vulcanizationTime = Convert.toInt(skuCapacity.getVulcanizationTime(), 0);
                    Integer mechanicalTime = Convert.toInt(skuCapacity.getMechanicalTime(), 0);
                    return (vulcanizationTime + mechanicalTime) > 0;
                })
                .forEach(skuCapacity -> {
                    Integer vulcanizationTime = Convert.toInt(skuCapacity.getVulcanizationTime(), 0);
                    Integer mechanicalTime = Convert.toInt(skuCapacity.getMechanicalTime(), 0);
                    double divisionResult = (double) ApsConstant.SECOND_PER_DAY / (vulcanizationTime + mechanicalTime);
                    double ceilResult = Math.floor(divisionResult);
                    skuCapacity.setApsCapacity(Convert.toInt(ceilResult) * 2);

                    this.setClassCapacity(skuCapacity);
                });
        // 设置默认值
        sourceList.stream()
                .filter(skuCapacity -> {
                    Integer vulcanizationTime = Convert.toInt(skuCapacity.getVulcanizationTime(), 0);
                    Integer mechanicalTime = Convert.toInt(skuCapacity.getMechanicalTime(), 0);
                    return (vulcanizationTime + mechanicalTime) == 0;
                })
                .forEach(skuCapacity -> skuCapacity.setApsCapacity(0));
    }

    /**
     * 计算日标准产量
     * @param billVO 要计算的对象
     */
    @Override
    public void setClassCapacity(MdmSkuLhCapacity billVO) {
        int paramValue = this.getParamValue();
        Integer standardCapacity = billVO.getStandardCapacity();
        if (standardCapacity != null && paramValue != 0) {
            BigDecimal result = new BigDecimal(standardCapacity).divide(new BigDecimal(paramValue), RoundingMode.UP);
            billVO.setClassCapacity(result.intValue());
        } else {
            billVO.setClassCapacity(0);
        }
    }

    private int getParamValue() {
        // 取参数
        FactoryParam factoryParam = new FactoryParam();
        factoryParam.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        factoryParam.setParamCode(MonthPlanEnums.APS_GENERAL_SHIFT.getCode());
        factoryParam.setProductTypeCode(ProductTypeEnum.WHOLE_STEEL.getValue());
        FactoryParam param = factoryParamService.getFacParamSingle(factoryParam);
        int paramValue;
        if (param == null) {
            paramValue = BigDecimal.ZERO.intValue();
        } else {
            paramValue = Integer.parseInt(StringUtils.isNotEmpty(param.getParamValue()) ? param.getParamValue() : param.getDefauleValue());
        }
        return paramValue;
    }
}
