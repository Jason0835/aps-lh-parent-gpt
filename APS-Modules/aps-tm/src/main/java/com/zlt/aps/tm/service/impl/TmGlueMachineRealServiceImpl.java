package com.zlt.aps.tm.service.impl;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tm.api.domain.entity.TmGlueMachineReal;
import com.zlt.aps.tm.api.domain.entity.TmMachineInfo;
import com.zlt.aps.tm.mapper.TmMachineInfoMapper;
import com.zlt.aps.tm.service.ITmGlueMachineRealService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：TmGlueMachineRealServiceImpl.java
 * 描    述：TmGlueMachineRealServiceImpl胎面胶料与机台关系业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-07-08
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TmGlueMachineRealServiceImpl extends AbstractDocService<TmGlueMachineReal> implements ITmGlueMachineRealService {

    @Autowired
    private TmMachineInfoMapper machineInfoMapper;

    @Override
    protected String getDocTypeCode() {
        return "TM0100";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("TM0100");
        return sysDocType;
    }

    @Override
    public String checkUnique(TmGlueMachineReal docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tmGlueMachineReal.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return new ArrayList<>(Collections.singletonList("glueCode"));
    }

    @Override
    protected Map<Object, Object> getServiceCheckParams(List<TmGlueMachineReal> list, List<TmGlueMachineReal> importList) {
        Map<Object, Object> serviceCheckParams = super.getServiceCheckParams(list, importList);
        List<TmMachineInfo> machineInfoList = machineInfoMapper.selectMachineInfoList(new TmMachineInfo());
        Map<String, TmMachineInfo> machineInfoMap = machineInfoList.stream().collect(Collectors.toMap(TmMachineInfo::getMachineName, Function.identity(), (s1, s2) -> s1));
        serviceCheckParams.put("machineMap", machineInfoMap);
        return serviceCheckParams;
    }

    @Override
    protected Boolean serviceCheckAndDataHandle(TmGlueMachineReal importDocEntity, List<ImportErrorLog> importErrorLogs, Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
        Map<String, TmMachineInfo> machineInfoMap = (Map<String, TmMachineInfo>) serviceCheckParams.get("machineMap");
        String mapKey = importDocEntity.getMachineName();
        if (!machineInfoMap.containsKey(mapKey)) {
            String message = I18nUtil.getMessage("ui.data.alert.tmGlueMachineReal.machineNotExist");
            ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                    errorRowNum, String.format(message, errorRowNum), importErrorLogs);
            return Boolean.FALSE;
        }
        TmMachineInfo machineInfo = machineInfoMap.get(mapKey);
        importDocEntity.setMachineId(machineInfo.getId());
        return super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
    }
}
