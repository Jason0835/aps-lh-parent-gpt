package com.zlt.aps.maindata.service.impl;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.maindata.service.IMdmMoldingMachineClsService;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachineCls;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmMoldingMachineClsServiceImpl.java
 * 描    述：MdmMoldingMachineClsServiceImpl基础数据-成型机类型主业务层处理
 *@author zlt
 *@date 2025-02-18
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
public class MdmMoldingMachineClsServiceImpl extends AbstractDocService<MdmMoldingMachineCls>  implements IMdmMoldingMachineClsService {
    @Override
    protected String getDocTypeCode() {
        return "0118";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("0118");
        return sysDocType;
    }

    @Override
    public String checkUnique(MdmMoldingMachineCls docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mdmMoldingMachineCls.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "moldingMachineClassCode");
    }
}

