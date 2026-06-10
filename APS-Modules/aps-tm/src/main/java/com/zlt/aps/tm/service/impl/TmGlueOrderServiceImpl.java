package com.zlt.aps.tm.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tm.api.domain.entity.TmGlueGroupOrder;
import com.zlt.aps.tm.api.domain.entity.TmGlueOrder;
import com.zlt.aps.tm.api.domain.entity.TmMachineInfo;
import com.zlt.aps.tm.mapper.TmGlueGroupOrderMapper;
import com.zlt.aps.tm.mapper.TmGlueOrderMapper;
import com.zlt.aps.tm.mapper.TmMachineInfoMapper;
import com.zlt.aps.tm.service.ITmGlueOrderService;
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
public class TmGlueOrderServiceImpl extends AbstractDocService<TmGlueOrder> implements ITmGlueOrderService {

    @Resource
    private TmGlueOrderMapper tmGlueOrderMapper;

    @Resource
    private TmMachineInfoMapper tmMachineInfoMapper;

    @Resource
    private TmGlueGroupOrderMapper tmGlueGroupOrderMapper;

    @Override
    protected String getDocTypeCode() {
        return "TM0809";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("TM0809");
        return sysDocType;
    }

    @Override
    public String checkUnique(TmGlueOrder query) {
        String unique = super.checkUnique(query);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.glueOrder.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("factoryCode", "glueCode", "machineCode"));
    }

    @Override
    protected Map<Object, Object> getServiceCheckParams(List<TmGlueOrder> list, List<TmGlueOrder> importList) {
        Map<Object, Object> serviceCheckParams = super.getServiceCheckParams(list, importList);
        // 提取所有非空、去重的机台编码
        List<String> machineCodeList = list.stream()
                .map(TmGlueOrder::getMachineCode)
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
        // 提取所有非空、去重的胶料组编码
        List<String> glueGroupCodeList = list.stream()
                .map(TmGlueOrder::getGlueGroupCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        // 分批查询胶料组顺序基础数据
        List<List<String>> glueGroupSplitList = ListUtil.split(glueGroupCodeList, 500);
        List<TmGlueGroupOrder> glueGroupOrderList = new ArrayList<>();
        for (List<String> codes : glueGroupSplitList) {
            LambdaQueryWrapper<TmGlueGroupOrder> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(TmGlueGroupOrder::getGlueGroupCode, codes);
            glueGroupOrderList.addAll(tmGlueGroupOrderMapper.selectList(wrapper));
        }
        if (CollUtil.isNotEmpty(glueGroupOrderList)) {
            serviceCheckParams.put("tmGlueGroupCodeList",
                    glueGroupOrderList.stream().map(TmGlueGroupOrder::getGlueGroupCode).collect(Collectors.toList()));
        }
        return serviceCheckParams;
    }

    @Override
    protected Boolean serviceCheckAndDataHandle(TmGlueOrder importDocEntity, List<ImportErrorLog> importErrorLogs,
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
        // 校验胶料组编码是否存在
        if (serviceCheckParams.containsKey("tmGlueGroupCodeList")) {
            List<String> glueGroupCodeList = (List<String>) serviceCheckParams.get("tmGlueGroupCodeList");
            String glueGroupCode = importDocEntity.getGlueGroupCode();
            if (!glueGroupCodeList.contains(glueGroupCode)) {
                String message = String.format(I18nUtil.getMessage("ui.data.alert.tm.glueGroupCodeNotExist"), glueGroupCode);
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(), errorRowNum, message, importErrorLogs);
                return Boolean.FALSE;
            }
        }
        return super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
    }
}
