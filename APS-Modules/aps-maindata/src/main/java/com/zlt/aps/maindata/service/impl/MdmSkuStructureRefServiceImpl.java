package com.zlt.aps.maindata.service.impl;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.maindata.service.IMdmSkuStructureRefService;
import com.zlt.aps.monthplan.api.domain.entity.MdmSkuStructureRef;
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
 * 文件名称：MdmSkuStructureRefServiceImpl.java
 * 描    述：MdmSkuStructureRefServiceImplSKU与结构关系业务层处理
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
public class MdmSkuStructureRefServiceImpl extends AbstractDocService<MdmSkuStructureRef>  implements IMdmSkuStructureRefService {
    @Override
    protected String getDocTypeCode() {
        return "MDM0134";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MDM0134");
        return sysDocType;
    }

    @Override
    public String checkUnique(MdmSkuStructureRef docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mdmSkuStructureRef.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }
}
