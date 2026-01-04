package com.zlt.aps.maindata.service.impl;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.maindata.mapper.MdmMoldingMachineEntityMapper;
import com.zlt.aps.maindata.service.IMdmWorkWearInfoService;
import com.zlt.aps.monthplan.api.domain.entity.MdmWorkWearInfo;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmWorkWearInfoServiceImpl.java
 * 描    述：MdmWorkWearInfoServiceImpl成型鼓(工装)台账业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-12
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class MdmWorkWearInfoServiceImpl extends AbstractDocService<MdmWorkWearInfo> implements IMdmWorkWearInfoService {

    @Autowired
    private MdmMoldingMachineEntityMapper moldingMachineEntityMapper;

    @Override
    protected String getDocTypeCode() {
        return "MDM0132";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MDM0132");
        return sysDocType;
    }

    @Override
    public String checkUnique(MdmWorkWearInfo docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mdmWorkWearInfo.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return new ArrayList<>(Arrays.asList("factoryCode", "workWearName", "cxMachineBrandCode", "workWearType", "cxMachineTypeCode", "specifications", "perimeterMax", "perimeterMin"));
    }

    @Override
    protected Boolean serviceCheckAndDataHandle(MdmWorkWearInfo importDocEntity, List<ImportErrorLog> importErrorLogs, Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
        Integer perimeterMin = importDocEntity.getPerimeterMin();
        Integer perimeterMax = importDocEntity.getPerimeterMax();
        if (perimeterMin != null && perimeterMax != null && perimeterMin > perimeterMax) {
            String message = I18nUtil.getMessage("ui.data.alert.mdmWorkWearInfo.perimeterCheck");
            com.zlt.common.utils.ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(), errorRowNum, message, importErrorLogs);
            return Boolean.FALSE;
        }
        return super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
    }
}
