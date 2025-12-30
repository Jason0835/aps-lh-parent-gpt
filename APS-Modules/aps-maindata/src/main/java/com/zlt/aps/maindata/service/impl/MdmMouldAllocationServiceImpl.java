package com.zlt.aps.maindata.service.impl;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.maindata.service.IMdmMouldAllocationService;
import com.zlt.aps.monthplan.api.domain.entity.MdmMouldAllocation;
import com.zlt.sysdef.domain.SysDocType;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import com.zlt.bill.common.service.AbstractDocService;
import com.ruoyi.common.exception.ServiceException;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmMouldAllocationServiceImpl.java
 * 描    述：MdmMouldAllocationServiceImpl模具分配比例(同结构/不同结构)业务层处理
 *@author zlt
 *@date 2025-12-14
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
public class MdmMouldAllocationServiceImpl extends AbstractDocService<MdmMouldAllocation>  implements IMdmMouldAllocationService {
    @Override
    protected String getDocTypeCode() {
        return "MDM0118";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MDM0118");
        return sysDocType;
    }

    @Override
    public String checkUnique(MdmMouldAllocation docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            String message = StringUtils.format(I18nUtil.getMessage("ui.data.alert.mdmMouldAllocation.notUnique"),
                    docEntityVO.getStructureName(), docEntityVO.getFactoryCode(), docEntityVO.getYear(), docEntityVO.getMonth());
            throw new ServiceException(message);
        }
        return unique;
    }


    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return new ArrayList<>(Arrays.asList("factoryCode", "year", "month", "structureName"));
    }


}
