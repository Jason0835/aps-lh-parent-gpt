package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.mp.api.domain.entity.RawWarningConfig;
import com.zlt.aps.mp.api.domain.entity.RawWarningConfig;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.sysdef.domain.SysDocType;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;
import com.zlt.aps.maindata.service.IRawWarningConfigService;
import com.zlt.bill.common.service.AbstractDocService;
import com.ruoyi.common.exception.ServiceException;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：RawWarningConfigServiceImpl.java
 * 描    述：RawWarningConfigServiceImpl原材料预警配置业务层处理
 *@author zlt
 *@date 2025-12-17
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
public class RawWarningConfigServiceImpl extends AbstractDocService<RawWarningConfig>  implements IRawWarningConfigService {
    @Override
    protected String getDocTypeCode() {
        return "S3520";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("S3520");
        return sysDocType;
    }

    @Override
    public String checkUnique(RawWarningConfig docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.rawWarningConfig.notUnique"));
        }
        //materialCode 不能含有空格
        if (Objects.equals(docEntityVO.getWarningType(), ApsConstant.APS_STRING_1) && docEntityVO.getMaterialCode().contains(" ")) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.rawWarningConfig.notUnique1"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return new ArrayList<>(Arrays.asList("factoryCode", "materialCode","warningType"));
    }

    /**
     * 业务校验和数据处理
     * 根据预警类型判断原材料编码和名称是否必填:
     * - 预警类型为新材料预警(2)时,不需要原材料编码和名称
     * - 预警类型为用量偏差预警(1)时,需要原材料编码和名称
     */
    @Override
    protected Boolean serviceCheckAndDataHandle(RawWarningConfig importDocEntity, List<ImportErrorLog> importErrorLogs, Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
        String warningType = importDocEntity.getWarningType();
        
        // 预警类型为用量偏差预警(1)时,校验原材料编码和名称必填
        if (ApsConstant.APS_STRING_1.equals(warningType)) {
            // 校验原材料编码
            if (org.apache.commons.lang3.StringUtils.isBlank(importDocEntity.getMaterialCode())) {
                String message = I18nUtil.getMessage("ui.data.alert.rawWarningConfig.materialCodeRequired");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorRowNum, String.format(message, errorRowNum), importErrorLogs);
                return Boolean.FALSE;
            }
            
            // 校验原材料名称
            if (org.apache.commons.lang3.StringUtils.isBlank(importDocEntity.getMaterialDesc())) {
                String message = I18nUtil.getMessage("ui.data.alert.rawWarningConfig.materialDescRequired");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorRowNum, String.format(message, errorRowNum), importErrorLogs);
                return Boolean.FALSE;
            }
        }
        
        return super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
    }



}
