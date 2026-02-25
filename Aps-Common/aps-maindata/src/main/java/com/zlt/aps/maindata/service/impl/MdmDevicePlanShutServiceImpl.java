package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.aps.maindata.mapper.MdmDevicePlanShutEntityMapper;
import com.zlt.aps.maindata.service.IMdmDevicePlanShutService;
import com.zlt.aps.mp.api.domain.entity.MdmDevicePlanShut;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmDevicePlanShutServiceImpl.java
 * 描    述：MdmDevicePlanShutServiceImpl0106基础数据_设备计划停机业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-04
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class MdmDevicePlanShutServiceImpl extends AbstractDocService<MdmDevicePlanShut> implements IMdmDevicePlanShutService {

    @Autowired
    private MdmDevicePlanShutEntityMapper entityMapper;

    @Override
    protected String getDocTypeCode() {
        return "MDM0106";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MDM0106");
        return sysDocType;
    }

    @Override
    public String checkUnique(MdmDevicePlanShut docEntityVO) {
        String factoryCode = docEntityVO.getFactoryCode();
//        String procCode = docEntityVO.getProcCode();
        String machineType = docEntityVO.getMachineType();
        String machineCode = docEntityVO.getMachineCode();

        Date beginDate = docEntityVO.getBeginDate();
        Date endDate = docEntityVO.getEndDate();
        if (beginDate == null || endDate == null) {
            throw new RuntimeException("开始时间或结束时间为空");
        }
        if (beginDate.after(endDate)) {
            throw new RuntimeException(I18nUtil.getMessage("ui.data.alert.DocDeviceMaintenancePlan.timeCheck"));
        }
        // 开始结束时间不能跨月
        Calendar beginCal = Calendar.getInstance();
        beginCal.setTime(beginDate);
        Calendar endCal = Calendar.getInstance();
        endCal.setTime(endDate);
        if (beginCal.get(Calendar.YEAR) != endCal.get(Calendar.YEAR) ||
                beginCal.get(Calendar.MONTH) != endCal.get(Calendar.MONTH)) {
            throw new RuntimeException(I18nUtil.getMessage("ui.data.alert.DocDeviceMaintenancePlan.yearAndMonthMustBeTheSame"));
        }
        LambdaQueryWrapper<MdmDevicePlanShut> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MdmDevicePlanShut::getFactoryCode, factoryCode);
//        queryWrapper.eq(MdmDevicePlanShut::getProcCode, procCode);
        queryWrapper.eq(MdmDevicePlanShut::getMachineType, machineType);
        queryWrapper.eq(MdmDevicePlanShut::getMachineCode, machineCode);
        queryWrapper.ne(Objects.nonNull(docEntityVO.getId()), BaseEntity::getId, docEntityVO.getId());
        List<MdmDevicePlanShut> mdmDevicePlanShutList = entityMapper.selectList(queryWrapper);
        if (CollectionUtils.isNotEmpty(mdmDevicePlanShutList)) {
            for (MdmDevicePlanShut mdmDevicePlanShut : mdmDevicePlanShutList) {
                long dbBeginTime = mdmDevicePlanShut.getBeginDate().getTime();
                long dbEndTime = mdmDevicePlanShut.getEndDate().getTime();
                long beginTime = beginDate.getTime();
                long endTime = endDate.getTime();
                if (!(beginTime >= dbEndTime || endTime <= dbBeginTime)) {
                    throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mdmDevicePlanShut.notUnique"));
                }
            }
        }
        return UserConstants.UNIQUE;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }

    @Override
    protected Map<Object, Object> getServiceCheckParams(List<MdmDevicePlanShut> list, List<MdmDevicePlanShut> importList) {
        Map<Object, Object> serviceCheckParams = super.getServiceCheckParams(list, importList);
        Map<String, List<MdmDevicePlanShut>> groupMap = list.stream().collect(Collectors.groupingBy(item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getMachineType(), item.getMachineCode())));
        serviceCheckParams.put("groupMap", groupMap);
        for (int i = 0; i < list.size(); i++) {
            MdmDevicePlanShut mdmDevicePlanShut = list.get(i);
            mdmDevicePlanShut.setSearchValue(i + "");
        }
        return serviceCheckParams;
    }

    @Override
    protected Boolean serviceCheckAndDataHandle(MdmDevicePlanShut importDocEntity, List<ImportErrorLog> importErrorLogs, Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
        if (serviceCheckParams.containsKey("groupMap")) {
            Map<String, List<MdmDevicePlanShut>> groupMap = (Map<String, List<MdmDevicePlanShut>>) serviceCheckParams.get("groupMap");
            String mapKey = GenerageMapKeyUtils.createMapKey(importDocEntity.getFactoryCode(), importDocEntity.getMachineType(), importDocEntity.getMachineCode());
            // excel内校验
            if (groupMap.containsKey(mapKey)) {
                List<MdmDevicePlanShut> mdmDevicePlanShutList = groupMap.get(mapKey);
                for (MdmDevicePlanShut mdmDevicePlanShut : mdmDevicePlanShutList) {
                    String searchValue = mdmDevicePlanShut.getSearchValue();
                    // 不一样的比较开始结束时间，看有没冲突区间
                    if (!searchValue.equals(importDocEntity.getSearchValue())) {
                        long dbBeginTime = mdmDevicePlanShut.getBeginDate().getTime();
                        long dbEndTime = mdmDevicePlanShut.getEndDate().getTime();
                        long beginTime = importDocEntity.getBeginDate().getTime();
                        long endTime = importDocEntity.getEndDate().getTime();
                        if (!(beginTime >= dbEndTime || endTime <= dbBeginTime)) {
                            String message = I18nUtil.getMessage("ui.data.alert.mdmDevicePlanShut.excel.notUnique");
                            String errorMsg = String.format(message, errorRowNum, Integer.parseInt(searchValue) + 2);
                            ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.REPEAT.getCode(), errorRowNum, errorMsg, importErrorLogs);
                            return Boolean.FALSE;
                        }
                    }
                }
            }
            // 数据库内校验
            String unique = null;
            try {
                unique = checkUnique(importDocEntity);
            } catch (Exception e) {
                logger.error("设备计划停机数据唯一性校验异常", e);
//                String uniqueMsg = I18nUtil.getMessage("import.validated.unique");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(), errorRowNum, e.getMessage(), importErrorLogs);
                return Boolean.FALSE;
            }
        }
        return super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
    }
}
