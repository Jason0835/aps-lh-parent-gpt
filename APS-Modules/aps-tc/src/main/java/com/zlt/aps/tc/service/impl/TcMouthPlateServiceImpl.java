package com.zlt.aps.tc.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tc.api.domain.entity.TcMachineInfo;
import com.zlt.aps.tc.api.domain.entity.TcMouthPlate;
import com.zlt.aps.tc.mapper.TcMachineInfoMapper;
import com.zlt.aps.tc.mapper.TcMouthPlateMapper;
import com.zlt.aps.tc.service.ITcMouthPlateService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TcMouthPlateServiceImpl extends AbstractDocService<TcMouthPlate> implements ITcMouthPlateService {

    @Resource
    private TcMouthPlateMapper tcMouthPlateMapper;

    @Resource
    private TcMachineInfoMapper tcMachineInfoMapper;

    @Override
    protected String getDocTypeCode() {
        return "TC0906";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("TC0906");
        return sysDocType;
    }

    @Override
    public String checkUnique(TcMouthPlate query) {
        String unique = super.checkUnique(query);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tc.mouthPlate.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("factoryCode", "mouthPlateCode", "machineCode"));
    }

    @Override
    protected Map<Object, Object> getServiceCheckParams(List<TcMouthPlate> list, List<TcMouthPlate> importList) {
        Map<Object, Object> serviceCheckParams = super.getServiceCheckParams(list, importList);
        // 提取所有非空、去重的机台编码
        List<String> machineCodeList = list.stream()
                .map(TcMouthPlate::getMachineCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        // 分批查询机台基础数据
        List<List<String>> splitList = ListUtil.split(machineCodeList, 500);
        List<TcMachineInfo> machineInfoList = new ArrayList<>();
        for (List<String> codes : splitList) {
            LambdaQueryWrapper<TcMachineInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(TcMachineInfo::getMachineCode, codes);
            machineInfoList.addAll(tcMachineInfoMapper.selectList(wrapper));
        }
        if (CollUtil.isNotEmpty(machineInfoList)) {
            serviceCheckParams.put("tcMachineCodeList",
                    machineInfoList.stream().map(TcMachineInfo::getMachineCode).collect(Collectors.toList()));
        }
        return serviceCheckParams;
    }

    @Override
    protected Boolean serviceCheckAndDataHandle(TcMouthPlate importDocEntity, List<ImportErrorLog> importErrorLogs,
                                                Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
        // 必填项校验：工厂编号、口型板编码、机台编码
        if (StringUtils.isBlank(importDocEntity.getFactoryCode())
                || StringUtils.isBlank(importDocEntity.getMouthPlateCode())
                || StringUtils.isBlank(importDocEntity.getMachineCode())) {
            String blankField;
            if (StringUtils.isBlank(importDocEntity.getFactoryCode())) {
                blankField = I18nUtil.getMessage("ui.data.column.tc.mouthPlate.factoryCode");
            } else if (StringUtils.isBlank(importDocEntity.getMouthPlateCode())) {
                blankField = I18nUtil.getMessage("ui.data.column.tc.mouthPlate.mouthPlateCode");
            } else {
                blankField = I18nUtil.getMessage("ui.data.column.tc.mouthPlate.machineCode");
            }
            String message = String.format(I18nUtil.getMessage("import.validated.required"), errorRowNum, blankField);
            ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(), errorRowNum, message, importErrorLogs);
            return Boolean.FALSE;
        }
        // 校验机台编码是否存在
        if (serviceCheckParams.containsKey("tcMachineCodeList")) {
            List<String> machineCodeList = (List<String>) serviceCheckParams.get("tcMachineCodeList");
            String machineCode = importDocEntity.getMachineCode();
            if (!machineCodeList.contains(machineCode)) {
                String message = I18nUtil.getMessage("ui.data.alert.tc.machineCodeNotExist");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(), errorRowNum, message, importErrorLogs);
                return Boolean.FALSE;
            }
        }
        return super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
    }
}