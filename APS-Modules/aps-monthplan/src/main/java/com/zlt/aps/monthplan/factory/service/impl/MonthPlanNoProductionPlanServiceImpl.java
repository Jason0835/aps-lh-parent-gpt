package com.zlt.aps.monthplan.factory.service.impl;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanNoProductionPlan;
import com.zlt.aps.monthplan.factory.service.IMonthPlanNoProductionPlanService;
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
 * 文件名称：MonthPlanNoProductionPlanServiceImpl.java
 * 描    述：MonthPlanNoProductionPlanServiceImplS2-0606.排产结果-未排产计划业务层处理
 *@author yelq
 *@date 2026-01-21
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class MonthPlanNoProductionPlanServiceImpl extends AbstractDocService<MonthPlanNoProductionPlan>  implements IMonthPlanNoProductionPlanService {
    @Override
    protected String getDocTypeCode() {
        return "2026012110";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("2026012110");
        return sysDocType;
    }

    @Override
    public String checkUnique(MonthPlanNoProductionPlan docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.monthPlanNoProductionPlan.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }
}
