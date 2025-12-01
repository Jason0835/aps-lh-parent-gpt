package com.zlt.aps.tm.service.impl;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tm.api.domain.entity.TmMachineInfo;
import com.zlt.aps.tm.api.domain.entity.TmMachineMaintenance;
import com.zlt.aps.tm.mapper.TmMachineInfoMapper;
import com.zlt.aps.tm.service.ITmMachineMaintenanceService;
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
 * 文件名称：TmMachineMaintenanceServiceImpl.java
 * 描    述：TmMachineMaintenanceServiceImpl胎面机台维修计划业务层处理
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
public class TmMachineMaintenanceServiceImpl extends AbstractDocService<TmMachineMaintenance> implements ITmMachineMaintenanceService {

    @Autowired
    private TmMachineInfoMapper machineInfoMapper;

    @Override
    protected String getDocTypeCode() {
        return "TM1001";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("TM1001");
        return sysDocType;
    }

    @Override
    public String checkUnique(TmMachineMaintenance docEntityVO) {
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
    protected Map<Object, Object> getServiceCheckParams(List<TmMachineMaintenance> list, List<TmMachineMaintenance> importList) {
        Map<Object, Object> serviceCheckParams = super.getServiceCheckParams(list, importList);
        List<TmMachineInfo> machineInfoList = machineInfoMapper.selectMachineInfoList(new TmMachineInfo());
        Map<String, TmMachineInfo> machineInfoMap = machineInfoList.stream().collect(Collectors.toMap(TmMachineInfo::getMachineName, Function.identity(), (s1, s2) -> s1));
        serviceCheckParams.put("machineMap", machineInfoMap);
        return serviceCheckParams;
    }

    @Override
    protected Boolean serviceCheckAndDataHandle(TmMachineMaintenance importDocEntity, List<ImportErrorLog> importErrorLogs, Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
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
