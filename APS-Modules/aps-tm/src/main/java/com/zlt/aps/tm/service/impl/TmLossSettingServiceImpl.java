package com.zlt.aps.tm.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tm.api.domain.entity.TmLossSetting;
import com.zlt.aps.tm.api.domain.entity.TmMachineInfo;
import com.zlt.aps.tm.mapper.TmLossSettingMapper;
import com.zlt.aps.tm.mapper.TmMachineInfoMapper;
import com.zlt.aps.tm.service.ITmLossSettingService;
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
public class TmLossSettingServiceImpl extends AbstractDocService<TmLossSetting> implements ITmLossSettingService {

    @Resource
    private TmLossSettingMapper tmLossSettingMapper;

    @Resource
    private TmMachineInfoMapper tmMachineInfoMapper;

    @Override
    protected String getDocTypeCode() {
        return "TM0810";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("TM0810");
        return sysDocType;
    }

    @Override
    public String checkUnique(TmLossSetting query) {
        if (StringUtils.isBlank(query.getTreadCode()) && StringUtils.isBlank(query.getMachineCode())) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.lossSetting.bothEmpty"));
        }
        if (StringUtils.isBlank(query.getTreadCode())) {
            query.setTreadCode("");
        }
        if (StringUtils.isBlank(query.getMachineCode())) {
            query.setMachineCode("");
        }
        String unique = super.checkUnique(query);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.lossSetting.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("factoryCode", "treadCode", "machineCode"));
    }

    @Override
    protected Map<Object, Object> getServiceCheckParams(List<TmLossSetting> list, List<TmLossSetting> importList) {
        Map<Object, Object> serviceCheckParams = super.getServiceCheckParams(list, importList);
        // 提取所有非空、去重的机台编码
        List<String> machineCodeList = list.stream()
                .map(TmLossSetting::getMachineCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        // 分批查询机台基础数据
        List<List<String>> splitList = ListUtil.split(machineCodeList, 500);
        List<TmMachineInfo> machineInfoList = new ArrayList<>();
        for (List<String> codes : splitList) {
            LambdaQueryWrapper<TmMachineInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(TmMachineInfo::getMachineCode, codes);
            machineInfoList.addAll(tmMachineInfoMapper.selectList(wrapper));
        }
        if (CollUtil.isNotEmpty(machineInfoList)) {
            serviceCheckParams.put("tmMachineCodeList",
                    machineInfoList.stream().map(TmMachineInfo::getMachineCode).collect(Collectors.toList()));
        }
        return serviceCheckParams;
    }

    @Override
    protected Boolean serviceCheckAndDataHandle(TmLossSetting importDocEntity, List<ImportErrorLog> importErrorLogs,
                                                Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
        // 校验机台编码是否存在（为空时跳过）
        String machineCode = importDocEntity.getMachineCode();
        if (StringUtils.isNotBlank(machineCode) && serviceCheckParams.containsKey("tmMachineCodeList")) {
            List<String> machineCodeList = (List<String>) serviceCheckParams.get("tmMachineCodeList");
            if (!machineCodeList.contains(machineCode)) {
                String message = I18nUtil.getMessage("ui.data.alert.tm.machineCodeNotExist");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(), errorRowNum, message, importErrorLogs);
                return Boolean.FALSE;
            }
        }
        return super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
    }
}
