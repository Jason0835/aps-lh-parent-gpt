package com.zlt.aps.tm.service.impl;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tm.api.domain.entity.TmCurlRoll;
import com.zlt.aps.tm.service.ITmCurlRollService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TmCurlRollServiceImpl extends AbstractDocService<TmCurlRoll> implements ITmCurlRollService {

    @Override
    protected String getDocTypeCode() {
        return "TM0811";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("TM0811");
        return sysDocType;
    }

    @Override
    public String checkUnique(TmCurlRoll query) {
        String unique = super.checkUnique(query);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.curlRoll.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("factoryCode", "treadCode"));
    }

    @Override
    protected Boolean serviceCheckAndDataHandle(TmCurlRoll importDocEntity, List<ImportErrorLog> importErrorLogs,
                                                Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
        // 必填项校验：工厂编号、胎面编码、卷曲长度
        if (StringUtils.isBlank(importDocEntity.getFactoryCode())
                || StringUtils.isBlank(importDocEntity.getTreadCode())
                || importDocEntity.getCurlLength() == null) {
            String blankField;
            if (StringUtils.isBlank(importDocEntity.getFactoryCode())) {
                blankField = I18nUtil.getMessage("ui.data.column.tm.curlRoll.factoryCode");
            } else if (StringUtils.isBlank(importDocEntity.getTreadCode())) {
                blankField = I18nUtil.getMessage("ui.data.column.tm.curlRoll.treadCode");
            } else {
                blankField = I18nUtil.getMessage("ui.data.column.tm.curlRoll.curlLength");
            }
            String message = String.format(I18nUtil.getMessage("import.validated.required"), errorRowNum, blankField);
            ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(), errorRowNum, message, importErrorLogs);
            return Boolean.FALSE;
        }
        return super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
    }
}
