package com.zlt.aps.gdyy.service.impl;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.gdyy.api.domain.entity.GdyyOriginalLineSpec;
import com.zlt.aps.gdyy.service.IGdyyOriginalLineSpecService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：GdyyOriginalLineSpecServiceImpl.java
 * 描    述：GdyyOriginalLineSpecServiceImpl钢丝压延原线规格业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-07-08
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class GdyyOriginalLineSpecServiceImpl extends AbstractDocService<GdyyOriginalLineSpec> implements IGdyyOriginalLineSpecService {
    @Override
    protected String getDocTypeCode() {
        return "GDYY002";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("GDYY002");
        return sysDocType;
    }

    @Override
    public String checkUnique(GdyyOriginalLineSpec docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.gdyyOriginalLineSpec.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return new ArrayList<>(Collections.singletonList("originalLineCode"));
    }
}
