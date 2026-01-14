package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.maindata.mapper.MpMonthPlanMonitorEntityMapper;
import com.zlt.aps.maindata.service.IMpMonthPlanMonitorService;
import com.zlt.aps.monthplan.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.entity.MpMonthPlanMonitor;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import com.zlt.bill.common.service.AbstractDocService;
import com.ruoyi.common.exception.ServiceException;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpMonthPlanMonitorServiceImpl.java
 * 描    述：MpMonthPlanMonitorServiceImpl月度硫化监控业务层处理
 *@author zlt
 *@date 2025-12-24
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(rollbackFor = Exception.class)
public class MpMonthPlanMonitorServiceImpl extends AbstractDocService<MpMonthPlanMonitor>  implements IMpMonthPlanMonitorService {

    private final MpMonthPlanMonitorEntityMapper mpMonthPlanMonitorEntityMapper;
    @Override
    protected String getDocTypeCode() {
        return "MONTH0612";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MONTH0612");
        return sysDocType;
    }

    @Override
    public String checkUnique(MpMonthPlanMonitor docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mpMonthPlanMonitor.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }

    @Override
    public List<MpMonthPlanMonitor> findCompleteQty(MpFactoryProductionVersion finalVersion) {
        LambdaQueryWrapper<MpMonthPlanMonitor> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MpMonthPlanMonitor::getFactoryCode, finalVersion.getFactoryCode());
        wrapper.eq(MpMonthPlanMonitor::getYear, finalVersion.getYear());
        wrapper.eq(MpMonthPlanMonitor::getMonth, finalVersion.getMonth());
        wrapper.eq(MpMonthPlanMonitor::getMonthPlanVersion, finalVersion.getMonthPlanVersion());
        wrapper.eq(MpMonthPlanMonitor::getIsDelete, YesOrNoEnum.NO.getValue());
        return this.mpMonthPlanMonitorEntityMapper.selectList(wrapper);
    }
}
