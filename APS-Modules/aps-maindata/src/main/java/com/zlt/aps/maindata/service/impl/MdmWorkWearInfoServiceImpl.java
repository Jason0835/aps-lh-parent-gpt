package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.maindata.mapper.MdmMoldingMachineEntityMapper;
import com.zlt.aps.maindata.service.IMdmWorkWearInfoService;
import com.zlt.aps.maindata.utils.ScmListUtils;
import com.zlt.aps.monthplan.api.domain.entity.MdmMoldingMachine;
import com.zlt.aps.monthplan.api.domain.entity.MdmWorkWearInfo;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        return new ArrayList<>(Arrays.asList("factoryCode", "workWearName", "cxMachineBrandCode", "workWearType", "cxMachineTypeCode"));
    }

    @Override
    protected Map<Object, Object> getServiceCheckParams(List<MdmWorkWearInfo> list, List<MdmWorkWearInfo> importList) {
        Map<Object, Object> serviceCheckParams = super.getServiceCheckParams(list, importList);
        // 查询成型机列表，如果校验使用机型字段是否为编号
        List<String> machineCodeList = list.stream().map(MdmWorkWearInfo::getUsedType).collect(Collectors.toList());
        List<List<String>> splitList = ScmListUtils.getSplitList(machineCodeList, 1000);
        List<MdmMoldingMachine> moldingMachineList = new ArrayList<>();
        for (List<String> codeList : splitList) {
            LambdaQueryWrapper<MdmMoldingMachine> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(BaseEntity::getIsDelete, ApsConstant.DEL_FLAG_NORMAL);
            queryWrapper.in(MdmMoldingMachine::getMachineCode, codeList);
            moldingMachineList.addAll(moldingMachineEntityMapper.selectList(queryWrapper));
        }
        Map<String, MdmMoldingMachine> moldingMachineMap;
        if (CollectionUtils.isNotEmpty(moldingMachineList)) {
            moldingMachineMap = moldingMachineList.stream().collect(Collectors.toMap(item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getMachineCode()), Function.identity(), (v1, v2) -> v1));
            serviceCheckParams.put("moldingMachineMap", moldingMachineMap);
        }
        return serviceCheckParams;
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
        if (serviceCheckParams.containsKey("moldingMachineMap")) {
            Map<String, MdmMoldingMachine> moldingMachineMap = (Map<String, MdmMoldingMachine>) serviceCheckParams.get("moldingMachineMap");
            String mapKey = GenerageMapKeyUtils.createMapKey(importDocEntity.getFactoryCode(), importDocEntity.getUsedType());
            if (!moldingMachineMap.containsKey(mapKey)) {
                String message = I18nUtil.getMessage("ui.data.alert.mdmWorkWearInfo.useTypeNotExists");
                com.zlt.common.utils.ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(), errorRowNum, message, importErrorLogs);
                return Boolean.FALSE;
            }
        }
        return super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
    }
}
