package com.zlt.aps.mp.adjust.service.impl;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.mp.adjust.mapper.MpAdjustMaterialLogEntityMapper;
import com.zlt.aps.mp.adjust.service.IMpAdjustMaterialLogService;
import com.zlt.aps.mp.api.domain.entity.MpAdjustMaterialLog;
import com.zlt.sysdef.domain.SysDocType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import com.zlt.bill.common.service.AbstractDocService;
import com.ruoyi.common.exception.ServiceException;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpAdjustMaterialLogServiceImpl.java
 * 描    述：MpAdjustMaterialLogServiceImplS2-0808.调整-调整日志（未调整及已调整）业务层处理
 *@author zlt
 *@date 2026-02-09
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
public class MpAdjustMaterialLogServiceImpl extends AbstractDocService<MpAdjustMaterialLog>  implements IMpAdjustMaterialLogService {

    @Autowired
    protected MpAdjustMaterialLogEntityMapper mpAdjustMaterialLogEntityMapper;

    @Override
    protected String getDocTypeCode() {
        return "S2-0808";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("S2-0808");
        return sysDocType;
    }

    @Override
    public String checkUnique(MpAdjustMaterialLog docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mpAdjustMaterialLog.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }

    @Override
    public void deleteAdjustProcLogByVersion(String factoryCode, String year, String month, String version) {
        mpAdjustMaterialLogEntityMapper.deleteAdjustProcLogByVersion(factoryCode,year,month,version);
    }
}
