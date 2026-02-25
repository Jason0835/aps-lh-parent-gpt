package com.zlt.aps.maindata.service.impl;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.maindata.mapper.MdmStockFactorEntityMapper;
import com.zlt.aps.maindata.service.IMdmStockFactorService;
import com.zlt.aps.mp.api.domain.entity.MdmStockFactor;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmStockFactorServiceImpl.java
 * 描    述：MdmStockFactorServiceImpl备货系数配置业务层处理
 *@author zlt
 *@date 2025-02-28
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
public class MdmStockFactorServiceImpl extends AbstractDocService<MdmStockFactor>  implements IMdmStockFactorService {

    @Autowired
    private MdmStockFactorEntityMapper entityMapper;

    @Override
    protected String getDocTypeCode() {
        return "0134";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("0119");
        return sysDocType;
    }

    @Override
    public String checkUnique(MdmStockFactor docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.column.mdmStockFactor.checkUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一键待确认
        return Collections.emptyList();
    }

}


