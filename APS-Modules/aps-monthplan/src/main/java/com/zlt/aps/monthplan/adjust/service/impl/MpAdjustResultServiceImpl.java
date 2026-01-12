package com.zlt.aps.monthplan.adjust.service.impl;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.monthplan.adjust.service.IMpAdjustResultService;
import com.zlt.aps.monthplan.api.domain.entity.MpAdjustResult;
import com.zlt.sysdef.domain.SysDocType;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import com.zlt.bill.common.service.AbstractDocService;
import com.ruoyi.common.exception.ServiceException;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpAdjustResultServiceImpl.java
 * 描    述：MpAdjustResultServiceImpl调整-调整结果记录业务层处理
 *@author zlt
 *@date 2025-12-19
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
public class MpAdjustResultServiceImpl extends AbstractDocService<MpAdjustResult>  implements IMpAdjustResultService {
    @Override
    protected String getDocTypeCode() {
        return "MP0804";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MP0804");
        return sysDocType;
    }

    @Override
    public String checkUnique(MpAdjustResult docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mpAdjustResult.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }

    @Override
    public void saveMpAdjustResult(List<MpAdjustResult> mpAdjustResultList) {
        baseDao.insertBatch(mpAdjustResultList);
    }
}
