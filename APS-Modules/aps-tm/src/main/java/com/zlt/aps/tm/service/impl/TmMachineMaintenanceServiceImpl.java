package com.zlt.aps.tm.service.impl;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tm.api.domain.entity.TmMachineInfo;
import com.zlt.aps.tm.api.domain.entity.TmMachineMaintenance;
import com.zlt.aps.tm.mapper.TmMachineInfoMapper;
import com.zlt.aps.tm.mapper.TmMachineMaintenanceMapper;
import com.zlt.aps.tm.service.ITmMachineMaintenanceService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TmMachineMaintenanceServiceImpl extends AbstractDocService<TmMachineMaintenance> implements ITmMachineMaintenanceService {

    @Resource
    private TmMachineMaintenanceMapper tmMachineMaintenanceMapper;

    @Autowired
    private TmMachineInfoMapper machineInfoMapper;

    @Override
    protected String getDocTypeCode() {
        return "TM0804";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("TM0804");
        return sysDocType;
    }

    @Override
    public String checkUnique(TmMachineMaintenance query) {
        String unique = super.checkUnique(query);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            String msg = I18nUtil.getMessage("ui.data.alert.tm.machineMaintenance.notUnique");
            throw new ServiceException(com.ruoyi.common.utils.StringUtils.format(msg,
                    query.getFactoryCode()));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("machineCode", "stopStartTime", "stopShift"));
    }

    @Override
    protected Map<Object, Object> getServiceCheckParams(List<TmMachineMaintenance> list, List<TmMachineMaintenance> importList) {
        Map<Object, Object> serviceCheckParams = super.getServiceCheckParams(list, importList);
        List<TmMachineInfo> machineInfoList = machineInfoMapper.selectList(null);
        Map<String, TmMachineInfo> machineInfoMap = machineInfoList.stream().collect(Collectors.toMap(TmMachineInfo::getMachineCode, Function.identity(), (s1, s2) -> s1));
        serviceCheckParams.put("machineMap", machineInfoMap);
        return serviceCheckParams;
    }

    @Override
    protected Boolean serviceCheckAndDataHandle(TmMachineMaintenance importDocEntity, List<com.ruoyi.api.gateway.system.domain.ImportErrorLog> importErrorLogs, Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
        @SuppressWarnings("unchecked")
        Map<String, TmMachineInfo> machineInfoMap = (Map<String, TmMachineInfo>) serviceCheckParams.get("machineMap");
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
