package com.zlt.aps.maindata.service.impl;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.maindata.mapper.MdmSkuStructureRefEntityMapper;
import com.zlt.aps.maindata.service.IMdmStructureLhRatioService;
import com.zlt.aps.monthplan.api.domain.entity.MdmSkuStructureRef;
import com.zlt.aps.monthplan.api.domain.entity.MdmStructureLhRatio;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;
import com.zlt.bill.common.service.AbstractDocService;
import com.ruoyi.common.exception.ServiceException;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmStructureLhRatioServiceImpl.java
 * 描    述：MdmStructureLhRatioServiceImpl成型结构硫化配比业务层处理
 *@author zlt
 *@date 2025-12-08
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
@RequiredArgsConstructor
public class MdmStructureLhRatioServiceImpl extends AbstractDocService<MdmStructureLhRatio>  implements IMdmStructureLhRatioService {

    private final MdmSkuStructureRefEntityMapper mdmSkuStructureRefEntityMapper;

    @Override
    protected String getDocTypeCode() {
        return "MDM0136";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MDM0136");
        return sysDocType;
    }

    @Override
    public String checkUnique(MdmStructureLhRatio docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mdmStructureLhRatio.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return new ArrayList<>(Arrays.asList("factoryCode", "cxMachineBrandCode", "structureName"));
    }

    @Override
    protected Map<Object, Object> getServiceCheckParams(List<MdmStructureLhRatio> list, List<MdmStructureLhRatio> importList) {
        Map<Object, Object> serviceCheckParams = new HashMap<>();
        if (PubUtil.isEmpty(list)) {
            return serviceCheckParams;
        }

        String factoryCode = list.stream()
                .findFirst()
                .map(MdmStructureLhRatio::getFactoryCode)
                .orElse(null);
        MdmSkuStructureRef queryVo = new MdmSkuStructureRef();
        queryVo.setFactoryCode(factoryCode);
        List<MdmSkuStructureRef> MdmSkuStructureRefList = mdmSkuStructureRefEntityMapper.getStructureSelectList(queryVo);
        if (PubUtil.isEmpty(MdmSkuStructureRefList)) {
            return serviceCheckParams;
        }

        String structureNameNotExistsMessage = I18nUtil.getMessage("ui.data.alert.mdmStructureLhRatio.structureNameNotExists");
        serviceCheckParams.put("structureNameNotExistsMessage", structureNameNotExistsMessage);
        Set<String> structureNameSet = MdmSkuStructureRefList.stream().map(MdmSkuStructureRef::getStructureName).collect(Collectors.toSet());
        serviceCheckParams.put("structureNameSet", structureNameSet);
        return serviceCheckParams;
    }

    @Override
    protected Boolean serviceCheckAndDataHandle(MdmStructureLhRatio importDocEntity, List<ImportErrorLog> importErrorLogs, Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
        Set<String> structureNameSet = (Set<String>) serviceCheckParams.get("structureNameSet");
        if (PubUtil.isEmpty(structureNameSet) || structureNameSet.contains(importDocEntity.getStructureName())) {
            return Boolean.TRUE;
        }
        String structureNameNotExistsMessage = serviceCheckParams.get("structureNameNotExistsMessage").toString();
        ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                errorRowNum, structureNameNotExistsMessage, importErrorLogs);
        return Boolean.FALSE;
    }

}
