package com.zlt.aps.lh.service.impl;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.lh.api.domain.entity.LhMoldChangePlanLog;
import com.zlt.aps.lh.service.ILhMoldChangePlanLogService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：LhMoldChangePlanLogServiceImpl.java
 * 描    述：LhMoldChangePlanLogServiceImpl模具变动单日志业务层处理
 *@author zlt
 *@date 2025-03-17
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
public class LhMoldChangePlanLogServiceImpl extends AbstractDocService<LhMoldChangePlanLog>  implements ILhMoldChangePlanLogService {
    @Override
    protected String getDocTypeCode() {
        return "0302";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("0302");
        return sysDocType;
    }

    @Override
    public String checkUnique(LhMoldChangePlanLog docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.lhMoldChangePlanLog.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }
}
