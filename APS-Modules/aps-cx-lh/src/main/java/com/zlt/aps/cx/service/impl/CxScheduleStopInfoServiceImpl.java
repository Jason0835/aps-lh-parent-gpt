package com.zlt.aps.cx.service.impl;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cx.mapper.entity.CxScheduleStopInfoEntityMapper;
import com.zlt.aps.cx.service.ICxScheduleStopInfoService;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxScheduleStopInfo;
import com.zlt.aps.cxlh.cx.api.domain.vo.LhAlgorithmScheduleResultDto;
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
 * 文件名称：CxScheduleStopInfoServiceImpl.java
 * 描    述：CxScheduleStopInfoServiceImpl成型机台自动停排信息业务层处理
 *@author zlt
 *@date 2025-03-11
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
public class CxScheduleStopInfoServiceImpl extends AbstractDocService<CxScheduleStopInfo>  implements ICxScheduleStopInfoService {

    @Autowired
    private CxScheduleStopInfoEntityMapper cxScheduleStopInfoEntityMapper;

    @Override
    protected String getDocTypeCode() {
        return "CX9210";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("CX9210");
        return sysDocType;
    }

    @Override
    public String checkUnique(CxScheduleStopInfo docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.cxScheduleStopInfo.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }

    @Override
    public CxScheduleStopInfo createCxScheduleStopInfo(LhAlgorithmScheduleResultDto item) {
        CxScheduleStopInfo cxScheduleStopInfo = new CxScheduleStopInfo();
        cxScheduleStopInfo.setFactoryCode("116");
        cxScheduleStopInfo.setCxBatchNo(item.getBatchNo());
        cxScheduleStopInfo.setEmbryoCode(item.getLhScheduleResult().getEmbryoCode());
        cxScheduleStopInfo.setUnScheduleNum(item.getTaskPlanQuantity());
        cxScheduleStopInfo.setSapCode(item.getLhScheduleResult().getProductCode());
        cxScheduleStopInfo.setSpecCode(item.getLhScheduleResult().getSpecCode());
        cxScheduleStopInfo.setBomDataVersion(item.getLhScheduleResult().getBomVersion());
        cxScheduleStopInfo.setScheduleDate(item.getLhScheduleResult().getScheduleDate());
        cxScheduleStopInfo.setSpec(item.getLhScheduleResult().getSpecDesc());
        cxScheduleStopInfo.setStopReason(item.getStopScheduleReason());
        cxScheduleStopInfoEntityMapper.insert(cxScheduleStopInfo);
        return cxScheduleStopInfo;
    }
}
