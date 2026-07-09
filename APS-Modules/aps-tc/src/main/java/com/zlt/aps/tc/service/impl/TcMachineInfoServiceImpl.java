package com.zlt.aps.tc.service.impl;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tc.api.domain.entity.TcMachineInfo;
import com.zlt.aps.tc.mapper.TcMachineInfoMapper;
import com.zlt.aps.tc.service.ITcMachineInfoService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：TcMachineInfoServiceImpl.java
 * 描    述：TcMachineInfoServiceImpl胎侧机台基础表业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2026-07-07
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TcMachineInfoServiceImpl extends AbstractDocService<TcMachineInfo> implements ITcMachineInfoService {

    @Resource
    private TcMachineInfoMapper tcMachineInfoMapper;

    @Override
    protected String getDocTypeCode() {
        return "TC0903";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("TC0903");
        return sysDocType;
    }

    @Override
    public String checkUnique(TcMachineInfo query) {
        String unique = super.checkUnique(query);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tc.machineInfo.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("factoryCode", "machineCode"));
    }
}
