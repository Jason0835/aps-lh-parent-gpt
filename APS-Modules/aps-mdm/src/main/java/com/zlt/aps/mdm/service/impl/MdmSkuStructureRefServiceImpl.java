package com.zlt.aps.mdm.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuStructureRef;
import com.zlt.aps.mdm.mapper.MdmSkuStructureRefEntityMapper;
import com.zlt.aps.mdm.mapper.MdmStructureNameEntityMapper;
import com.zlt.aps.mdm.service.IMdmSkuStructureRefService;
import com.zlt.aps.mp.api.domain.entity.MdmStructureName;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmSkuStructureRefServiceImpl.java
 * 描    述：MdmSkuStructureRefServiceImplSKU与结构关系业务层处理
 *@author zlt
 *@date 2025-12-06
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class MdmSkuStructureRefServiceImpl extends AbstractDocService<MdmSkuStructureRef>  implements IMdmSkuStructureRefService {

    private final MdmSkuStructureRefEntityMapper mdmSkuStructureRefEntityMapper;

    private final MdmStructureNameEntityMapper mdmStructureNameEntityMapper;

    @Override
    protected String getDocTypeCode() {
        return "MDM0134";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MDM0134");
        return sysDocType;
    }

    @Override
    public String checkUnique(MdmSkuStructureRef docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mdmSkuStructureRef.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return new ArrayList<>(Arrays.asList("factoryCode", "structureName","mainMaterialDesc"));
    }

    @Override
    protected Map<Object, Object> getServiceCheckParams(List<MdmSkuStructureRef> list, List<MdmSkuStructureRef> importList) {
        Map<Object, Object> serviceCheckParams = super.getServiceCheckParams(list, importList);
        List<String> structureNameList = list.stream().map(MdmSkuStructureRef::getStructureName).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        List<List<String>> splitList = ListUtil.split(structureNameList, 500);
        List<MdmStructureName> mdmStructureNameList = new ArrayList<>();
        for (List<String> structureList : splitList) {
            LambdaQueryWrapper<MdmStructureName> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(MdmStructureName::getStructureName, structureList);
            mdmStructureNameList.addAll(mdmStructureNameEntityMapper.selectList(wrapper));
        }
        if (CollUtil.isNotEmpty(mdmStructureNameList)) {
            serviceCheckParams.put("mdmStructureNameList", mdmStructureNameList.stream().map(MdmStructureName::getStructureName).collect(Collectors.toList()));
        }
        return serviceCheckParams;
    }

    @Override
    protected Boolean serviceCheckAndDataHandle(MdmSkuStructureRef importDocEntity, List<ImportErrorLog> importErrorLogs, Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
        if (serviceCheckParams.containsKey("mdmStructureNameList")) {
            List<String> mdmStructureNameList = (List<String>) serviceCheckParams.get("mdmStructureNameList");
            String structureName = importDocEntity.getStructureName();
            if (!mdmStructureNameList.contains(structureName)) {
                String message = I18nUtil.getMessage("ui.data.alert.mdmSkuStructureRef.structureNotExist");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(), errorRowNum, message, importErrorLogs);
                return Boolean.FALSE;
            }
        }
        return super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
    }

    /**
     * 更新结构到物料
     * @param queryVO 查询条件
     * @return 结果
     */
    @Override
    public AjaxResult updateStructureToMaterial(MdmSkuStructureRef queryVO) {
        queryVO.setBaseVale(null);
        mdmSkuStructureRefEntityMapper.updateStructureToMaterial(queryVO);
        return AjaxResult.success();
    }
}
