package com.zlt.aps.mdm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.mdm.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuConstructionRef;
import com.zlt.aps.mdm.mapper.MdmMaterialInfoEntityMapper;
import com.zlt.aps.mdm.mapper.MdmSkuConstructionRefEntityMapper;
import com.zlt.aps.mdm.service.IMdmSkuConstructionRefService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmSkuConstructionRefServiceImpl.java
 * 描    述：MdmSkuConstructionRefServiceImplSKU与施工（示方书）关系业务层处理
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
@Transactional(rollbackFor = Exception.class)
public class MdmSkuConstructionRefServiceImpl extends AbstractDocService<MdmSkuConstructionRef>  implements IMdmSkuConstructionRefService {

    @Autowired
    private MdmSkuConstructionRefEntityMapper skuConstructionRefEntityMapper;

    @Autowired
    private MdmMaterialInfoEntityMapper materialInfoEntityMapper;

    @Override
    protected String getDocTypeCode() {
        return "MDM0123";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MDM0123");
        return sysDocType;
    }

    @Override
    public String checkUnique(MdmSkuConstructionRef docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mdmSkuConstructionRef.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return new ArrayList<>(Arrays.asList("factoryCode", "materialCode", "trialStatus"));
    }

    @Override
    protected Map<Object, Object> getServiceCheckParams(List<MdmSkuConstructionRef> list, List<MdmSkuConstructionRef> importList) {
        Map<Object, Object> serviceCheckParams = super.getServiceCheckParams(list, importList);
        // 关联物料表赋值规格、花纹、品牌
        Map<String, MdmMaterialInfo> materialInfoMap = new HashMap<>(16);
        List<String> materialCodeList = list.stream().map(MdmSkuConstructionRef::getMaterialCode).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(materialCodeList)) {
            List<List<String>> splitList = com.zlt.aps.mdm.utils.CollectionUtils.splitList(materialCodeList, 100);
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
    protected Boolean serviceCheckAndDataHandle(MdmSkuConstructionRef importDocEntity, List<ImportErrorLog> importErrorLogs, Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
        if (serviceCheckParams.containsKey("materialInfoMap")) {
            Map<String, MdmMaterialInfo> materialInfoMap = (Map<String, MdmMaterialInfo>) serviceCheckParams.get("materialInfoMap");
            String materialCode = FactoryConstant.DEFAULT_FACTORY_CODE + "|" + importDocEntity.getMaterialCode();
            if (materialInfoMap.containsKey(materialCode)) {
                MdmMaterialInfo materialInfo = materialInfoMap.get(materialCode);
                importDocEntity.setMaterialDesc(materialInfo.getMaterialDesc());
            }
        }
        return super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
    }

    /**
     * 更新胎胚描述到物料表
     *
     * @param queryVO 查询条件
     * @return 结果
     */
    @Override
    public AjaxResult updateMainMaterialDescToMaterialInfo(MdmSkuConstructionRef queryVO) {
        queryVO.setBaseVale(null);
        skuConstructionRefEntityMapper.updateMainMaterialDescToMaterialInfo(queryVO);
        return AjaxResult.success();
    }
}
