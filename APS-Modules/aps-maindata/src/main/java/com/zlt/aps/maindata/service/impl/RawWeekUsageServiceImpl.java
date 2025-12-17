package com.zlt.aps.maindata.service.impl;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

import org.springframework.transaction.annotation.Transactional;
import com.zlt.aps.maindata.service.IRawWeekUsageService;
import com.zlt.aps.maindata.domain.entity.RawWeekUsage;
import com.zlt.bill.common.service.AbstractDocService;
import com.ruoyi.common.exception.ServiceException;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：RawWeekUsageServiceImpl.java
 * 描    述：RawWeekUsageServiceImpl周维度原材料用量记录业务层处理
 *@author zlt
 *@date 2025-12-17
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
public class RawWeekUsageServiceImpl extends AbstractDocService<RawWeekUsage>  implements IRawWeekUsageService {
    @Override
    protected String getDocTypeCode() {
        return "S3522";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("S3522");
        return sysDocType;
    }

    @Override
    public String checkUnique(RawWeekUsage docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.rawWeekUsage.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }
}
