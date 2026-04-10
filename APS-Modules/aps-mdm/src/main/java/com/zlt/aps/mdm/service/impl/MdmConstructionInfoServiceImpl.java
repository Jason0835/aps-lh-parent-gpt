package com.zlt.aps.mdm.service.impl;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.mdm.api.domain.entity.MdmConstructionInfo;
import com.zlt.aps.mdm.service.IMdmConstructionInfoService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmConstructionInfoServiceImpl.java
 * 描    述：MdmConstructionInfoServiceImpl投产胎胚施工信息业务层处理
 *@author zlt
 *@date 2025-12-10
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
public class MdmConstructionInfoServiceImpl extends AbstractDocService<MdmConstructionInfo>  implements IMdmConstructionInfoService {
    @Override
    protected String getDocTypeCode() {
        return "MDM0124";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MDM0124");
        return sysDocType;
    }

    @Override
    public String checkUnique(MdmConstructionInfo docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mdmConstructionInfo.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }

    @Override
    protected Boolean serviceCheckAndDataHandle(MdmConstructionInfo importDocEntity, List<ImportErrorLog> importErrorLogs, Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
        Boolean result = super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
        importDocEntity.setMaterialCode(importDocEntity.getSpecCode());
        importDocEntity.setMesMaterialCode(importDocEntity.getSpecCode());
        return result;
    }
}
