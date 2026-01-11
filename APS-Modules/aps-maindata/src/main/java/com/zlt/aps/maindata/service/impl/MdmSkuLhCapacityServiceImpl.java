package com.zlt.aps.maindata.service.impl;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.maindata.service.IMdmSkuLhCapacityService;
import com.zlt.aps.monthplan.api.domain.entity.MdmSkuLhCapacity;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import com.zlt.bill.common.service.AbstractDocService;
import com.ruoyi.common.exception.ServiceException;

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
        super.checkUnique(docEntityVO);
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
        // 计算APS日硫化量：APS日硫化量 = 24 * 60 / 硫化总时间(min)
        sourceList.stream()
                .filter(skuCapacity -> {
                    Integer sum = skuCapacity.getSumVulcanization();
                    return sum != null && sum > 0;
                })
                .forEach(skuCapacity -> {
                    Integer sumVulcanization = skuCapacity.getSumVulcanization();
                    double divisionResult = (double) ApsConstant.MINUTES_PER_DAY / sumVulcanization;
                    double ceilResult = Math.ceil(divisionResult);
                    skuCapacity.setApsCapacity(Convert.toInt(ceilResult));
                });
        // 设置默认值
        sourceList.stream()
                .filter(skuCapacity -> {
                    Integer sum = skuCapacity.getSumVulcanization();
                    return sum == null || sum <= 0;
                })
                .forEach(skuCapacity -> skuCapacity.setApsCapacity(0));
    }

}
