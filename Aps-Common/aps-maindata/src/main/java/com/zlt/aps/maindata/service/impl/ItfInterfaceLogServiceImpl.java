package com.zlt.aps.maindata.service.impl;

import com.ruoyi.common.constant.UserConstants;
import com.zlt.aps.maindata.service.IItfInterfaceLogService;
import com.zlt.aps.mp.api.domain.itf.ItfInterfaceLog;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ItfInterfaceLogServiceImpl.java
 * 描    述：ItfInterfaceLogServiceImpl接口请求日志业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-04-10
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class ItfInterfaceLogServiceImpl extends AbstractDocService<ItfInterfaceLog> implements IItfInterfaceLogService {
    @Override
    protected String getDocTypeCode() {
        return "9999";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("9999");
        return sysDocType;
    }

    @Override
    public String checkUnique(ItfInterfaceLog docEntityVO) {
        return UserConstants.NOT_UNIQUE;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }
}
