package com.zlt.aps.monthplan.adjust.service.impl;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.monthplan.adjust.mapper.MpAdjustResultEntityMapper;
import com.zlt.aps.monthplan.adjust.mapper.MpAdjustStructureLogEntityMapper;
import com.zlt.aps.monthplan.adjust.service.IMpAdjustStructureLogService;
import com.zlt.aps.monthplan.api.domain.entity.MpAdjustStructureLog;
import com.zlt.sysdef.domain.SysDocType;
import org.springframework.beans.factory.annotation.Autowired;
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
 * 文件名称：MpAdjustStructureLogServiceImpl.java
 * 描    述：MpAdjustStructureLogServiceImpl调整-操作日志业务层处理
 *@author zlt
 *@date 2025-12-19
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
public class MpAdjustStructureLogServiceImpl extends AbstractDocService<MpAdjustStructureLog>  implements IMpAdjustStructureLogService {

    @Autowired
    protected MpAdjustStructureLogEntityMapper adjustStructureLogEntityMapper;

    @Override
    protected String getDocTypeCode() {
        return "MP0808";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MP0808");
        return sysDocType;
    }

    @Override
    public String checkUnique(MpAdjustStructureLog docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mpAdjustStructureLog.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }

    @Override
    public void deleteAdjustLogByVersion(String factoryCode, String year, String month, String version) {
        adjustStructureLogEntityMapper.deleteAdjustLogByVersion(factoryCode,year,month,version);
    }
}
