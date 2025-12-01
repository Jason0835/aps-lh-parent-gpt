package com.zlt.aps.tc.service.impl;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tc.api.domain.entity.TcMachineInfo;
import com.zlt.aps.tc.api.domain.entity.TcMachineMaintenance;
import com.zlt.aps.tc.mapper.TcMachineInfoMapper;
import com.zlt.aps.tc.service.ITcMachineMaintenanceService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：TcMachineMaintenanceServiceImpl.java
 * 描    述：TcMachineMaintenanceServiceImpl胎侧机台维修计划业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-09-15
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TcMachineMaintenanceServiceImpl extends AbstractDocService<TcMachineMaintenance> implements ITcMachineMaintenanceService {

    @Autowired
    private TcMachineInfoMapper machineInfoMapper;

    @Override
    protected String getDocTypeCode() {
        return "TC1001";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("TC1001");
        return sysDocType;
    }

    @Override
    public String checkUnique(TcMachineMaintenance docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.machineMaintenance.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return new ArrayList<>(Arrays.asList("machineName", "stopDate", "stopShift"));
    }
    @Override
    protected Map<Object, Object> getServiceCheckParams(List<TcMachineMaintenance> list, List<TcMachineMaintenance> importList) {
        Map<Object, Object> serviceCheckParams = super.getServiceCheckParams(list, importList);
        List<TcMachineInfo> machineInfoList = machineInfoMapper.selectMachineInfoList(new TcMachineInfo());
        Map<String, TcMachineInfo> machineInfoMap = machineInfoList.stream().collect(Collectors.toMap(TcMachineInfo::getMachineName, Function.identity(), (s1, s2) -> s1));
        serviceCheckParams.put("machineMap", machineInfoMap);
        return serviceCheckParams;
    }

    @Override
    protected Boolean serviceCheckAndDataHandle(TcMachineMaintenance importDocEntity, List<ImportErrorLog> importErrorLogs, Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
        Map<String, TcMachineInfo> machineInfoMap = (Map<String, TcMachineInfo>) serviceCheckParams.get("machineMap");
        String mapKey = importDocEntity.getMachineName();
        if (!machineInfoMap.containsKey(mapKey)) {
            String message = I18nUtil.getMessage("ui.data.alert.tmGlueMachineReal.machineNotExist");
            ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                    errorRowNum, String.format(message, errorRowNum), importErrorLogs);
            return Boolean.FALSE;
        }
        TcMachineInfo machineInfo = machineInfoMap.get(mapKey);
        importDocEntity.setMachineId(machineInfo.getId());
        return super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
    }
}
