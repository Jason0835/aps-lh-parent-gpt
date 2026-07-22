package com.zlt.aps.mdm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.mdm.mapper.MdmDevicePlanShutEntityMapper;
import com.zlt.aps.mdm.service.IMdmDevicePlanShutService;
import com.zlt.aps.mdm.api.domain.entity.MdmDevicePlanShut;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.hutool.core.date.DateUtil;
import java.util.*;

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
        // 唯一性校验：同工厂+机台类型+停机类型+机台，不允许时间区间重叠
        LambdaQueryWrapper<MdmDevicePlanShut> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MdmDevicePlanShut::getFactoryCode, docEntityVO.getFactoryCode());
        queryWrapper.eq(MdmDevicePlanShut::getMachineType, docEntityVO.getMachineType());
        queryWrapper.eq(MdmDevicePlanShut::getMachineStopType, docEntityVO.getMachineStopType());
        queryWrapper.eq(MdmDevicePlanShut::getMachineCode, docEntityVO.getMachineCode());
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
        // 唯一校验字段：导入时按此匹配更新，必须返回可变 List（基类会 add("id")）
        return new ArrayList<>(Arrays.asList("factoryCode", "machineType", "machineStopType", "machineCode"));
    }

    @Override
    public AjaxResult importData(List<MdmDevicePlanShut> list, boolean updateSupport, Long importLogId) {
        // 导入前：删除 beginDate < 今天0点的过期计划（逻辑删除）
        Date todayBegin = DateUtil.beginOfDay(new Date());
        LambdaQueryWrapper<MdmDevicePlanShut> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.lt(MdmDevicePlanShut::getBeginDate, todayBegin);
        entityMapper.delete(deleteWrapper);

        return super.importData(list, updateSupport, importLogId);
    }

    @Override
    protected Boolean serviceCheckAndDataHandle(MdmDevicePlanShut importDocEntity, List<ImportErrorLog> importErrorLogs, Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
        Date beginDate = importDocEntity.getBeginDate();
        Date endDate = importDocEntity.getEndDate();

        // 校验开始时间不大于结束时间
        if (beginDate != null && endDate != null && beginDate.after(endDate)) {
            String message = I18nUtil.getMessage("ui.data.alert.DocDeviceMaintenancePlan.timeCheck");
            ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                    errorRowNum, message, importErrorLogs);
            return Boolean.FALSE;
        }

        // 校验开始结束时间不能跨月
        if (beginDate != null && endDate != null) {
            Calendar beginCal = Calendar.getInstance();
            beginCal.setTime(beginDate);
            Calendar endCal = Calendar.getInstance();
            endCal.setTime(endDate);
            if (beginCal.get(Calendar.YEAR) != endCal.get(Calendar.YEAR) ||
                    beginCal.get(Calendar.MONTH) != endCal.get(Calendar.MONTH)) {
                String message = I18nUtil.getMessage("ui.data.alert.DocDeviceMaintenancePlan.yearAndMonthMustBeTheSame");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorRowNum, message, importErrorLogs);
                return Boolean.FALSE;
            }
        }

        return super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
    }
}
