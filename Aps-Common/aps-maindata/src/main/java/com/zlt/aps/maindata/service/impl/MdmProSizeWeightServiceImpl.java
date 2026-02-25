package com.zlt.aps.maindata.service.impl;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.maindata.service.IMdmProSizeWeightService;
import com.zlt.aps.mp.api.domain.entity.MdmProSizeWeight;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmProSizeWeightServiceImpl.java
 * 描    述：MdmProSizeWeightServiceImpl基础数据库位寸口重量业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-04-08
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class MdmProSizeWeightServiceImpl extends AbstractDocService<MdmProSizeWeight> implements IMdmProSizeWeightService {
    @Override
    protected String getDocTypeCode() {
        return "0149";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("0149");
        return sysDocType;
    }

    @Override
    public String checkUnique(MdmProSizeWeight docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mdmProSizeWeight.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return new ArrayList<>(Arrays.asList("channel", "proSize"));
    }


}
