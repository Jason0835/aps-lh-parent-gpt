package com.zlt.aps.tm.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tm.api.domain.entity.TmGlueMachineReal;
import com.zlt.aps.tm.api.domain.entity.TmMachineInfo;
import com.zlt.aps.tm.mapper.TmGlueMachineRealMapper;
import com.zlt.aps.tm.mapper.TmMachineInfoMapper;
import com.zlt.aps.tm.service.ITmGlueMachineRealService;
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
public class TmGlueMachineRealServiceImpl extends AbstractDocService<TmGlueMachineReal> implements ITmGlueMachineRealService {

    @Resource
    private TmGlueMachineRealMapper tmGlueMachineRealMapper;

    @Resource
    private TmMachineInfoMapper tmMachineInfoMapper;

    @Override
    protected String getDocTypeCode() {
        return "TM0802";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("TM0802");
        return sysDocType;
    }

    @Override
    public String checkUnique(TmGlueMachineReal query) {
        String unique = super.checkUnique(query);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            String msg = I18nUtil.getMessage("ui.data.alert.tm.glueMachineReal.notUnique");
            throw new ServiceException(com.ruoyi.common.utils.StringUtils.format(msg,
                    query.getFactoryCode(), query.getGlueCode(), query.getMachineCode()));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("factoryCode", "glueCode", "machineCode"));
    }

    @Override
    protected Map<Object, Object> getServiceCheckParams(List<TmGlueMachineReal> list, List<TmGlueMachineReal> importList) {
        Map<Object, Object> serviceCheckParams = super.getServiceCheckParams(list, importList);
        // 提取所有非空、去重的机台编码
        List<String> machineCodeList = list.stream()
                .map(TmGlueMachineReal::getMachineCode)
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
    protected Boolean serviceCheckAndDataHandle(TmGlueMachineReal importDocEntity, List<ImportErrorLog> importErrorLogs,
                                                Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
        // 校验机台编码是否存在
        if (serviceCheckParams.containsKey("tmMachineCodeList")) {
            List<String> machineCodeList = (List<String>) serviceCheckParams.get("tmMachineCodeList");
            String machineCode = importDocEntity.getMachineCode();
            if (!machineCodeList.contains(machineCode)) {
                String message = String.format(I18nUtil.getMessage("ui.data.alert.tm.machineCodeNotExist"), machineCode);
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(), errorRowNum, message, importErrorLogs);
                return Boolean.FALSE;
            }
        }
        return super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
    }
}
