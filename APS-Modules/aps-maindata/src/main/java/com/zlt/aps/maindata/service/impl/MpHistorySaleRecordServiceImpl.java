package com.zlt.aps.maindata.service.impl;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.maindata.service.IMpHistorySaleRecordService;
import com.zlt.aps.monthplan.api.domain.entity.MpHistorySaleRecord;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpHistorySaleRecordServiceImpl.java
 * 描    述：MpHistorySaleRecordServiceImpl历史销售记录业务层处理
 *@author zlt
 *@date 2025-12-11
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
public class MpHistorySaleRecordServiceImpl extends AbstractDocService<MpHistorySaleRecord> implements IMpHistorySaleRecordService {
    @Override
    protected String getDocTypeCode() {
        return "MP1215";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MP1215");
        return sysDocType;
    }

    @Override
    public String checkUnique(MpHistorySaleRecord docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mpHistorySaleRecord.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return new ArrayList<>(Arrays.asList("year", "month", "areaCode", "materialCode"));
    }
}
