package com.zlt.aps.maindata.service.impl;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.annotation.DataImportCheck;
import com.zlt.sysdef.domain.SysDocType;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import com.zlt.aps.maindata.service.IRawSpecialMaterialStockService;
import com.zlt.aps.mp.api.domain.entity.RawSpecialMaterialStock;
import com.zlt.bill.common.service.AbstractDocService;
import com.ruoyi.common.exception.ServiceException;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：RawSpecialMaterialStockServiceImpl.java
 * 描    述：RawSpecialMaterialStockServiceImpl特殊材料库存业务层处理
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
public class RawSpecialMaterialStockServiceImpl extends AbstractDocService<RawSpecialMaterialStock>  implements IRawSpecialMaterialStockService {
    @Override
    protected String getDocTypeCode() {
        return "RAW9001";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("RAW9001");
        return sysDocType;
    }

    @Override
    public String checkUnique(RawSpecialMaterialStock docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.rawSpecialMaterialStock.notUnique"));
        }

        //materialCode 不能含有空格
        if (docEntityVO.getMaterialCode().contains(" ")) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.rawWarningConfig.notUnique1"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return new ArrayList<>(Arrays.asList("factoryCode", "materialCode", "year", "month"));
    }

    @DataImportCheck(
            maxCount = 5000,
            messageKey = "ui.data.import.count.exceed",
            params = {"#list.size()", "5000"}
    )
    @Override
    public AjaxResult importData(List<RawSpecialMaterialStock> list, boolean updateSupport, Long importLogId) {
        return super.importData(list, updateSupport, importLogId);
    }
}
