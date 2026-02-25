package com.zlt.aps.maindata.service.impl;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.maindata.mapper.MdmSkuConstructionRefEntityMapper;
import com.zlt.aps.maindata.service.IMdmSkuConstructionRefService;
import com.zlt.aps.mp.api.domain.entity.MdmSkuConstructionRef;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmSkuConstructionRefServiceImpl.java
 * 描    述：MdmSkuConstructionRefServiceImplSKU与施工（示方书）关系业务层处理
 *@author zlt
 *@date 2025-12-06
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
public class MdmSkuConstructionRefServiceImpl extends AbstractDocService<MdmSkuConstructionRef>  implements IMdmSkuConstructionRefService {

    @Autowired
    private MdmSkuConstructionRefEntityMapper skuConstructionRefEntityMapper;

    @Override
    protected String getDocTypeCode() {
        return "MDM0123";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MDM0123");
        return sysDocType;
    }

    @Override
    public String checkUnique(MdmSkuConstructionRef docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mdmSkuConstructionRef.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return new ArrayList<>(Arrays.asList("factoryCode", "materialCode", "trialStatus"));
    }

    /**
     * 更新胎胚描述到物料表
     *
     * @param queryVO 查询条件
     * @return 结果
     */
    @Override
    public AjaxResult updateMainMaterialDescToMaterialInfo(MdmSkuConstructionRef queryVO) {
        queryVO.setBaseVale(null);
        skuConstructionRefEntityMapper.updateMainMaterialDescToMaterialInfo(queryVO);
        return AjaxResult.success();
    }
}
