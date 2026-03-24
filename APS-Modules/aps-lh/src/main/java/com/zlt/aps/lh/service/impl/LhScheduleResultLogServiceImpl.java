package com.zlt.aps.lh.service.impl;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResultLog;
import com.zlt.aps.lh.mapper.LhScheduleResultLogEntityMapper;
import com.zlt.aps.lh.service.ILhScheduleResultLogService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.core.dao.basedao.BaseDao;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：LhScheduleResultLogServiceImpl.java
 * 描    述：LhScheduleResultLogServiceImpl硫化排程结果日志业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-27
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class LhScheduleResultLogServiceImpl extends AbstractDocService<LhScheduleResultLog> implements ILhScheduleResultLogService {


    @Autowired
    private LhScheduleResultLogEntityMapper lhScheduleResultLogEntityMapper;
    @Autowired
    private BaseDao baseDao;

    @Override
    protected String getDocTypeCode() {
        return "0202";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("0202");
        return sysDocType;
    }

    @Override
    public String checkUnique(LhScheduleResultLog docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.lhScheduleResultLog.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }

    @Override
    public int insertList(List<LhScheduleResultLog> insertList) {
        return baseDao.insertBatch(insertList);
    }
}
