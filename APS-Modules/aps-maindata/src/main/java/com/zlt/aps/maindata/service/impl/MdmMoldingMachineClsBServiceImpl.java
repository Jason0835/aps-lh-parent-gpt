package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.FactoryConstant;
import com.zlt.aps.maindata.mapper.MdmMoldingMachineClsEntityMapper;
import com.zlt.aps.maindata.service.IMdmMoldingMachineClsBService;
import com.zlt.aps.monthplan.api.domain.entity.MdmMoldingMachineCls;
import com.zlt.aps.monthplan.api.domain.entity.MdmMoldingMachineClsB;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmMoldingMachineClsBServiceImpl.java
 * 描    述：MdmMoldingMachineClsBServiceImpl基础数据-成型机类型子业务层处理
 *@author zlt
 *@date 2025-02-18
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class MdmMoldingMachineClsBServiceImpl extends AbstractDocService<MdmMoldingMachineClsB>  implements IMdmMoldingMachineClsBService {

    @Autowired
    private MdmMoldingMachineClsEntityMapper mdmMoldingMachineClsMapper;

    @Override
    protected String getDocTypeCode() {
        return "0119";
    }


    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("0119");
        return sysDocType;
    }

    @Override
    public String checkUnique(MdmMoldingMachineClsB docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mdmMoldingMachineClsB.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("moldingMachineClassId", "proSize");
    }

    @Override
    protected Map<Object, Object> getServiceCheckParams(List<MdmMoldingMachineClsB> list, List<MdmMoldingMachineClsB> importList) {
        Map<Object, Object> serviceCheckParams = super.getServiceCheckParams(list, importList);

        List<String> codeList = list.stream().map(MdmMoldingMachineClsB::getMoldingMachineClassCode).collect(Collectors.toList());
        LambdaQueryWrapper<MdmMoldingMachineCls> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(MdmMoldingMachineCls::getMoldingMachineClassCode, codeList);
        List<MdmMoldingMachineCls> mdmMoldingMachineCls = mdmMoldingMachineClsMapper.selectList(wrapper);
        Map<String, Long> moldingMachineClsCodeMap = mdmMoldingMachineCls.stream().collect(Collectors
                .toMap(item -> String.join("|", item.getFactoryCode(), item.getMoldingMachineClassCode()),
                        MdmMoldingMachineCls::getId));
        serviceCheckParams.put("moldingMachineClassCodeMap", moldingMachineClsCodeMap);
        return serviceCheckParams;
    }

    @Override
    protected Boolean serviceCheckAndDataHandle(MdmMoldingMachineClsB importDocEntity, List<ImportErrorLog> importErrorLogs, Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
        Map<String, Long> moldingMachineClsCodeMap = (Map<String, Long>) serviceCheckParams.get("moldingMachineClassCodeMap");
        String moldingMachineClassCode = String.join("|", FactoryConstant.DEFAULT_FACTORY_CODE, importDocEntity.getMoldingMachineClassCode());
        if (!moldingMachineClsCodeMap.containsKey(moldingMachineClassCode)) {
            String message = I18nUtil.getMessage("ui.data.alert.mdmMoldingMachineClsB.moldingMachineClassCodeNotExist");
            ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                    errorRowNum, String.format(message, errorRowNum), importErrorLogs);
            return Boolean.FALSE;
        }
        Long classId = moldingMachineClsCodeMap.get(moldingMachineClassCode);
        importDocEntity.setMoldingMachineClassId(classId);
        return super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
    }
}

