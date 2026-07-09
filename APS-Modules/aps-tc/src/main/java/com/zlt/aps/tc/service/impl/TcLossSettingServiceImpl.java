package com.zlt.aps.tc.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tc.api.domain.entity.TcLossSetting;
import com.zlt.aps.tc.api.domain.entity.TcMachineInfo;
import com.zlt.aps.tc.mapper.TcLossSettingMapper;
import com.zlt.aps.tc.mapper.TcMachineInfoMapper;
import com.zlt.aps.tc.service.ITcLossSettingService;
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
public class TcLossSettingServiceImpl extends AbstractDocService<TcLossSetting> implements ITcLossSettingService {

    @Resource
    private TcLossSettingMapper tcLossSettingMapper;

    @Resource
    private TcMachineInfoMapper tcMachineInfoMapper;

    @Override
    protected String getDocTypeCode() {
        return "TC0910";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("TC0910");
        return sysDocType;
    }

    @Override
    public String checkUnique(TcLossSetting query) {
        // 校验胎侧编码与机台编码不能同时为空
        if (StringUtils.isBlank(query.getSidewallCode()) && StringUtils.isBlank(query.getMachineCode())) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tc.lossSetting.bothEmpty"));
        }
        if (StringUtils.isBlank(query.getSidewallCode())) {
            query.setSidewallCode("");
        }
        if (StringUtils.isBlank(query.getMachineCode())) {
            query.setMachineCode("");
        }
        String unique = super.checkUnique(query);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tc.lossSetting.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("factoryCode", "sidewallCode", "machineCode"));
    }

    @Override
    protected Map<Object, Object> getServiceCheckParams(List<TcLossSetting> list, List<TcLossSetting> importList) {
        Map<Object, Object> serviceCheckParams = super.getServiceCheckParams(list, importList);
        // 提取所有非空、去重的机台编码
        List<String> machineCodeList = list.stream()
                .map(TcLossSetting::getMachineCode)
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
    protected Boolean serviceCheckAndDataHandle(TcLossSetting importDocEntity, List<ImportErrorLog> importErrorLogs,
                                                Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
        // 校验胎侧编码与机台编码不能同时为空
        String sidewallCode = importDocEntity.getSidewallCode();
        String machineCode = importDocEntity.getMachineCode();
        if (StringUtils.isBlank(sidewallCode) && StringUtils.isBlank(machineCode)) {
            String message = I18nUtil.getMessage("ui.data.alert.tc.lossSetting.bothEmpty");
            ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(), errorRowNum, message, importErrorLogs);
            return Boolean.FALSE;
        }
        // 校验机台编码是否存在（非空时校验）
        if (StringUtils.isNotBlank(machineCode) && serviceCheckParams.containsKey("tcMachineCodeList")) {
            List<String> machineCodeList = (List<String>) serviceCheckParams.get("tcMachineCodeList");
            if (!machineCodeList.contains(machineCode)) {
                String message = I18nUtil.getMessage("ui.data.alert.tc.machineCodeNotExist");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(), errorRowNum, message, importErrorLogs);
                return Boolean.FALSE;
            }
        }
        return super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
    }
}