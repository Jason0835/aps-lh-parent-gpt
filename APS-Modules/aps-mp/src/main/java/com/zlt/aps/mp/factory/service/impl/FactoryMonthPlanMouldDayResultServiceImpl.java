package com.zlt.aps.mp.factory.service.impl;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanMouldDayResult;
import com.zlt.aps.mp.factory.service.IFactoryMonthPlanMouldDayResultService;
import com.zlt.sysdef.domain.SysDocType;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import com.zlt.bill.common.service.AbstractDocService;
import com.ruoyi.common.exception.ServiceException;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FactoryMonthPlanMouldDayResultServiceImpl.java
 * 描    述：FactoryMonthPlanMouldDayResultServiceImplS2-0604.排产结果-生产计划排产结果业务层处理
 *@author zlt
 *@date 2025-12-31
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
public class FactoryMonthPlanMouldDayResultServiceImpl extends AbstractDocService<FactoryMonthPlanMouldDayResult>  implements IFactoryMonthPlanMouldDayResultService {
    @Override
    protected String getDocTypeCode() {
        return "11";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("11");
        return sysDocType;
    }

    @Override
    public String checkUnique(FactoryMonthPlanMouldDayResult docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.factoryMonthPlanMouldDayResult.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }
}
