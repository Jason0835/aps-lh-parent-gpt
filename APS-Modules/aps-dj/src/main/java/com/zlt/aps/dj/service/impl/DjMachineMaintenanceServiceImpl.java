package com.zlt.aps.dj.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.dj.api.domain.entity.DjMachineInfo;
import com.zlt.aps.dj.api.domain.entity.DjMachineMaintenance;
import com.zlt.aps.dj.mapper.DjMachineInfoMapper;
import com.zlt.aps.dj.service.IDjMachineMaintenanceService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.sysdef.domain.SysDocType;

import lombok.extern.slf4j.Slf4j;

/**
 * 垫胶机台维修计划ServiceImpl业务层处理
 *
 * @author zlt
 * @date 2026-06-11
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class DjMachineMaintenanceServiceImpl extends AbstractDocService<DjMachineMaintenance> implements IDjMachineMaintenanceService {

    @Autowired
    private DjMachineInfoMapper machineInfoMapper;

    @Override
    protected String getDocTypeCode() {
        return "DJ1001";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("DJ1001");
        return sysDocType;
    }

    @Override
    public String checkUnique(DjMachineMaintenance docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.machineMaintenance.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("machineCode", "stopStartTime", "stopShift"));
    }

    @Override
    protected Map<Object, Object> getServiceCheckParams(List<DjMachineMaintenance> list, List<DjMachineMaintenance> importList) {
        Map<Object, Object> serviceCheckParams = super.getServiceCheckParams(list, importList);
        List<DjMachineInfo> machineInfoList = machineInfoMapper.selectList(null);
        Map<String, DjMachineInfo> machineInfoMap = machineInfoList.stream().collect(Collectors.toMap(DjMachineInfo::getMachineCode, Function.identity(), (s1, s2) -> s1));
        serviceCheckParams.put("machineMap", machineInfoMap);
        return serviceCheckParams;
    }

    @Override
    protected Boolean serviceCheckAndDataHandle(DjMachineMaintenance importDocEntity, List<com.ruoyi.api.gateway.system.domain.ImportErrorLog> importErrorLogs, Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
        @SuppressWarnings("unchecked")
        Map<String, DjMachineInfo> machineInfoMap = (Map<String, DjMachineInfo>) serviceCheckParams.get("machineMap");
        String mapKey = importDocEntity.getMachineCode();
        if (!machineInfoMap.containsKey(mapKey)) {
            String message = I18nUtil.getMessage("ui.data.alert.tmGlueMachineReal.machineNotExist");
            ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                    errorRowNum, String.format(message, errorRowNum), importErrorLogs);
            return Boolean.FALSE;
        }
        return super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
    }
}