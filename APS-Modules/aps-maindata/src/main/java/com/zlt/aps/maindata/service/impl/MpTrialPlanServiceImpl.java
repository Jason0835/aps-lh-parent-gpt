package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.api.gateway.system.domain.SysUser;
import com.ruoyi.api.gateway.system.service.ISysUserService;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.maindata.mapper.MdmMaterialInfoEntityMapper;
import com.zlt.aps.maindata.mapper.MdmSkuConstructionRefEntityMapper;
import com.zlt.aps.maindata.service.IMpTrialPlanService;
import com.zlt.aps.monthplan.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmSkuConstructionRef;
import com.zlt.aps.monthplan.api.domain.entity.MpTrialPlan;
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
 * 文件名称：MpTrialPlanServiceImpl.java
 * 描    述：MpTrialPlanServiceImpl试制量试计划业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-11
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class MpTrialPlanServiceImpl extends AbstractDocService<MpTrialPlan> implements IMpTrialPlanService {

    @Autowired
    private MdmSkuConstructionRefEntityMapper skuConstructionRefEntityMapper;

    @Autowired
    private MdmMaterialInfoEntityMapper materialInfoEntityMapper;

    @Autowired
    private ISysUserService iSysUserService;

    @Override
    protected String getDocTypeCode() {
        return "MP0210";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MP0210");
        return sysDocType;
    }

    @Override
    public String checkUnique(MpTrialPlan docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mpTrialPlan.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }

    @Override
    protected Map<Object, Object> getServiceCheckParams(List<MpTrialPlan> list, List<MpTrialPlan> importList) {
        Map<Object, Object> serviceCheckParams = super.getServiceCheckParams(list, importList);
        List<String> materialCodeList = list.stream().map(MpTrialPlan::getMaterialCode).distinct().collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(materialCodeList)) {
            // 查询SKU与施工关系，用于写入 制造示方、文字示方、硫化示方
            LambdaQueryWrapper<MdmSkuConstructionRef> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(MdmSkuConstructionRef::getMaterialCode, materialCodeList);
            List<MdmSkuConstructionRef> mdmSkuConstructionRefList = skuConstructionRefEntityMapper.selectList(queryWrapper);
            Map<String, MdmSkuConstructionRef> skuConstructionRefMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(mdmSkuConstructionRefList)) {
                skuConstructionRefMap = mdmSkuConstructionRefList.stream().collect(Collectors.toMap(MdmSkuConstructionRef::getMaterialCode, Function.identity(), (old, now) -> old));
            }
            serviceCheckParams.put("skuConstructionRefMap", skuConstructionRefMap);

            // 查询物料信息
            LambdaQueryWrapper<MdmMaterialInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(MdmMaterialInfo::getMaterialCode, materialCodeList);
            List<MdmMaterialInfo> materialInfoList = materialInfoEntityMapper.selectList(wrapper);
            Map<String, MdmMaterialInfo> materialInfoMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(materialInfoList)) {
                materialInfoMap = materialInfoList.stream().collect(Collectors.toMap(MdmMaterialInfo::getMaterialCode, Function.identity(), (old, now) -> old));
            }
            serviceCheckParams.put("materialInfoMap", materialInfoMap);
        }
        SysUser sysUser = iSysUserService.selectUserByName(SecurityUtils.getUsername());
        serviceCheckParams.put("user", sysUser);
        return serviceCheckParams;
    }

    @Override
    protected Boolean serviceCheckAndDataHandle(MpTrialPlan importDocEntity, List<ImportErrorLog> importErrorLogs, Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
        importDocEntity.setImportTime(new Date());
        importDocEntity.setIsImport(ApsConstant.TRUE);
        if (serviceCheckParams.containsKey("user")) {
            SysUser sysUser = (SysUser) serviceCheckParams.get("user");
            importDocEntity.setDeptId(sysUser.getDeptId());
        }
        String materialCode = importDocEntity.getMaterialCode();
        if (serviceCheckParams.containsKey("skuConstructionRefMap")) {
            Map<String, MdmSkuConstructionRef> skuConstructionRefMap = (Map<String, MdmSkuConstructionRef>) serviceCheckParams.get("skuConstructionRefMap");
            if (skuConstructionRefMap.containsKey(materialCode)) {
                MdmSkuConstructionRef mdmSkuConstructionRef = skuConstructionRefMap.get(materialCode);
                importDocEntity.setEmbryoNo(mdmSkuConstructionRef.getEmbryoNo());
                importDocEntity.setMadeInfo(mdmSkuConstructionRef.getEmbryoNo());
                importDocEntity.setTextNo(mdmSkuConstructionRef.getTextNo());
                importDocEntity.setLhNo(mdmSkuConstructionRef.getLhNo());
            }
        }
        if (serviceCheckParams.containsKey("materialInfoMap")) {
            Map<String, MdmMaterialInfo> materialInfoMap = (Map<String, MdmMaterialInfo>) serviceCheckParams.get("materialInfoMap");
            if (materialInfoMap.containsKey(materialCode)) {
                MdmMaterialInfo materialInfo = materialInfoMap.get(materialCode);
                importDocEntity.setMaterialDesc(materialInfo.getMaterialDesc());
                importDocEntity.setPattern(materialInfo.getPattern());
                importDocEntity.setSpecifications(materialInfo.getSpecifications());
            }
        }
        return super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
    }
}
