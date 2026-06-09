package com.zlt.aps.maindata.service.impl;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.maindata.mapper.MdmConstructionInfoEntityMapper;
import com.zlt.aps.maindata.service.IMdmConstructionInfoService;
import com.zlt.aps.maindata.utils.RemoteImportExcelUtils;
import com.zlt.aps.mp.api.domain.entity.MdmConstructionInfo;
import com.zlt.aps.mp.api.service.IRemoteImportErrorLogService;
import com.zlt.aps.mp.api.service.IRemoteImportLogService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmConstructionInfoServiceImpl.java
 * 描    述：MdmConstructionInfoServiceImpl投产胎胚施工信息业务层处理
 *@author zlt
 *@date 2025-12-10
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
public class MdmConstructionInfoServiceImpl extends AbstractDocService<MdmConstructionInfo>  implements IMdmConstructionInfoService {

    private static final Pattern TIRE_FABRIC_CODE_GETTER = Pattern.compile("getTireFabricCode\\d+");

    @Autowired
    private IRemoteImportLogService iRemoteImportLogService;

    @Autowired
    private IRemoteImportErrorLogService iRemoteImportErrorLogService;

    @Autowired
    private MdmConstructionInfoEntityMapper mdmConstructionInfoEntityMapper;

    @Override
    protected String getDocTypeCode() {
        return "MDM0124";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MDM0124");
        return sysDocType;
    }

    @Override
    public String checkUnique(MdmConstructionInfo docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mdmConstructionInfo.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return new ArrayList<>(Arrays.asList("factoryCode", "constructionCode", "constructionVersion"));
    }

    @Override
    protected Boolean serviceCheckAndDataHandle(MdmConstructionInfo importDocEntity, List<ImportErrorLog> importErrorLogs, Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
        Boolean result = super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
        importDocEntity.setMaterialCode(importDocEntity.getSpecCode());
        importDocEntity.setMesMaterialCode(importDocEntity.getSpecCode());
        return result;
    }

    @Override
    public List<String> listTireFabricCodes() {
        List<MdmConstructionInfo> constructionInfos = mdmConstructionInfoEntityMapper.selectList(null);
        return collectTireFabricCodes(constructionInfos);
    }

    List<String> collectTireFabricCodes(List<MdmConstructionInfo> constructionInfos) {
        if (constructionInfos == null || constructionInfos.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> codeSet = new TreeSet<>();
        Method[] methods = MdmConstructionInfo.class.getMethods();
        for (MdmConstructionInfo constructionInfo : constructionInfos) {
            for (Method method : methods) {
                if (!TIRE_FABRIC_CODE_GETTER.matcher(method.getName()).matches()) {
                    continue;
                }
                collectTireFabricCode(codeSet, constructionInfo, method);
            }
        }
        return new ArrayList<>(codeSet);
    }

    private void collectTireFabricCode(Set<String> codeSet, MdmConstructionInfo constructionInfo, Method method) {
        try {
            Object value = method.invoke(constructionInfo);
            if (value == null) {
                return;
            }
            String code = value.toString().trim();
            if (!code.isEmpty()) {
                codeSet.add(code);
            }
        } catch (ReflectiveOperationException e) {
            log.warn("读取投产胎胚施工胎体布代号失败，方法：{}", method.getName(), e);
        }
    }

    @Async
    @Override
    public void importDataAsync(List<MdmConstructionInfo> list, boolean updateSupport, long importLogId, ImportLog importLog, Date beginTime, ServletRequestAttributes attributes) {
        try {
            RequestContextHolder.setRequestAttributes(attributes, true);

            AjaxResult result = this.importData(list, updateSupport, importLogId);
            Date endTime = DateUtils.getNowDate();
            importLog.setRowCount(list.size());
            importLog.setBeginTime(beginTime);
            importLog.setEndTime(endTime);
            importLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
            RemoteImportExcelUtils.updateImportLogAndFormatMsg(importLog, result, iRemoteImportLogService);
            RemoteImportExcelUtils.saveImportErrorLogs(result, iRemoteImportErrorLogService);
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }
}
