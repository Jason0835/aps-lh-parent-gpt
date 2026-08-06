package com.zlt.aps.tc.service.impl;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tc.api.domain.entity.TcMachineInfo;
import com.zlt.aps.tc.mapper.TcMachineInfoMapper;
import com.zlt.aps.tc.service.ITcMachineInfoService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：TcMachineInfoServiceImpl.java
 * 描    述：TcMachineInfoServiceImpl胎侧机台基础表业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2026-07-07
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TcMachineInfoServiceImpl extends AbstractDocService<TcMachineInfo> implements ITcMachineInfoService {

    @Resource
    private TcMachineInfoMapper tcMachineInfoMapper;

    @Override
    protected String getDocTypeCode() {
        return "TC0903";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("TC0903");
        return sysDocType;
    }

    @Override
    public String checkUnique(TcMachineInfo query) {
        String unique = super.checkUnique(query);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tc.machineInfo.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("factoryCode", "machineCode"));
    }

    @Override
    protected Boolean serviceCheckAndDataHandle(TcMachineInfo importDocEntity, List<ImportErrorLog> importErrorLogs,
                                                Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
        // 必填项校验：工厂编号、机台编码、机台名称
        if (StringUtils.isBlank(importDocEntity.getFactoryCode())
                || StringUtils.isBlank(importDocEntity.getMachineCode())
                || StringUtils.isBlank(importDocEntity.getMachineName())) {
            String blankField;
            if (StringUtils.isBlank(importDocEntity.getFactoryCode())) {
                blankField = I18nUtil.getMessage("ui.data.column.tc.machineInfo.factoryCode");
            } else if (StringUtils.isBlank(importDocEntity.getMachineCode())) {
                blankField = I18nUtil.getMessage("ui.data.column.tc.machineInfo.machineCode");
            } else {
                blankField = I18nUtil.getMessage("ui.data.column.tc.machineInfo.machineName");
            }
            String message = String.format(I18nUtil.getMessage("import.validated.required"), errorRowNum, blankField);
            ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(), errorRowNum, message, importErrorLogs);
            return Boolean.FALSE;
        }
        return super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
    }

    @Override
    public int removeByIds(List<Long> ids) {
        if (PubUtil.isEmpty(ids)) {
            return 0;
        }
        // 逻辑删除全局配置下 selectBatchIds 仅返回 IS_DELETE=0 的活跃记录
        List<TcMachineInfo> machines = tcMachineInfoMapper.selectBatchIds(ids);
        // 清理同 (FACTORY_CODE, MACHINE_CODE) 的历史墓碑，避免逻辑删除 0->1 时
        // 唯一索引 uk_tc_machine_info_factory_machine 冲突（#23277）
        for (TcMachineInfo machine : machines) {
            tcMachineInfoMapper.physicalDeleteTombstones(machine.getFactoryCode(), machine.getMachineCode());
        }
        return super.removeByIds(ids);
    }
}
