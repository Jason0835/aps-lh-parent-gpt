package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.maindata.mapper.MdmMaterialInfoEntityMapper;
import com.zlt.aps.maindata.service.IMdmMonthSurplusService;
import com.zlt.aps.maindata.utils.RemoteImportExcelUtils;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mp.api.domain.entity.MdmMonthSurplus;
import com.zlt.aps.mp.api.service.IRemoteImportErrorLogService;
import com.zlt.aps.mp.api.service.IRemoteImportLogService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmMonthSurplusServiceImpl.java
 * 描    述：MdmMonthSurplusServiceImpl0140基础数据_月底计划余量业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-08
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class MdmMonthSurplusServiceImpl extends AbstractDocService<MdmMonthSurplus> implements IMdmMonthSurplusService {

  private final MdmMaterialInfoEntityMapper materialInfoEntityMapper;

    @Autowired
    private IRemoteImportLogService iRemoteImportLogService;

    @Autowired
    private IRemoteImportErrorLogService iRemoteImportErrorLogService;

  @Override
    protected String getDocTypeCode() {
        return "MDM0140";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MDM0140");
        return sysDocType;
    }

    @Override
    public String checkUnique(MdmMonthSurplus docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mdmMonthSurplus.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return new ArrayList<>(Arrays.asList("factoryCode", "year", "month", "materialCode"));
    }

    /**
     * 异步导入
     */
    @Async
    @Override
    public void importDataAsync(List<MdmMonthSurplus> list, boolean updateSupport, Long importLogId, ImportLog importLog, Date beginTime, ServletRequestAttributes attributes) {
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

    @Override
    protected Map<Object, Object> getServiceCheckParams(List<MdmMonthSurplus> list, List<MdmMonthSurplus> importList) {
        Map<Object, Object> serviceCheckParams = super.getServiceCheckParams(list, importList);
        // 关联物料表赋值规格、花纹、品牌
        Map<String, MdmMaterialInfo> materialInfoMap = new HashMap<>(16);
        List<String> materialCodeList = list.stream().map(MdmMonthSurplus::getMaterialCode).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(materialCodeList)) {
            List<List<String>> splitList = com.zlt.aps.maindata.utils.CollectionUtils.splitList(materialCodeList, 100);
            List<MdmMaterialInfo> materialInfoList = new ArrayList<>();
            for (List<String> codeList : splitList) {
                LambdaQueryWrapper<MdmMaterialInfo> wrapper = new LambdaQueryWrapper<MdmMaterialInfo>()
                        .in(MdmMaterialInfo::getMaterialCode, codeList)
                        .eq(MdmMaterialInfo::getFactoryCode, FactoryConstant.DEFAULT_FACTORY_CODE);
                materialInfoList.addAll(materialInfoEntityMapper.selectList(wrapper));
            }

            if (CollectionUtils.isNotEmpty(materialInfoList)) {
                materialInfoMap = materialInfoList.stream().collect(Collectors.toMap(item -> String.join("|", item.getFactoryCode(), item.getMaterialCode()), Function.identity(), (v1, v2) -> v1));
            }
            serviceCheckParams.put("materialInfoMap", materialInfoMap);
        }
        return serviceCheckParams;
    }

    @Override
    protected Boolean serviceCheckAndDataHandle(MdmMonthSurplus importDocEntity, List<ImportErrorLog> importErrorLogs, Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
        if (serviceCheckParams.containsKey("materialInfoMap")) {
            Map<String, MdmMaterialInfo> materialInfoMap = (Map<String, MdmMaterialInfo>) serviceCheckParams.get("materialInfoMap");
            String materialCode = FactoryConstant.DEFAULT_FACTORY_CODE + "|" + importDocEntity.getMaterialCode();
            if (materialInfoMap.containsKey(materialCode)) {
                MdmMaterialInfo materialInfo = materialInfoMap.get(materialCode);
                importDocEntity.setMaterialDesc(materialInfo.getMaterialDesc());
                importDocEntity.setBrand(materialInfo.getBrand());
                importDocEntity.setProductTypeCode(materialInfo.getProductTypeCode());
                importDocEntity.setStructureName(materialInfo.getStructureName());
            }
        }
        return super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
    }
}
