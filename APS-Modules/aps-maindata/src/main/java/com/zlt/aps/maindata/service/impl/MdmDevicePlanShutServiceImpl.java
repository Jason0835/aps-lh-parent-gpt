package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.maindata.mapper.MdmDevicePlanShutEntityMapper;
import com.zlt.aps.maindata.service.IMdmDevicePlanShutService;
import com.zlt.aps.monthplan.api.domain.entity.MdmDevicePlanShut;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.List;

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
        String procCode = docEntityVO.getProcCode();
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
        LambdaQueryWrapper<MdmDevicePlanShut> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MdmDevicePlanShut::getFactoryCode, factoryCode);
        queryWrapper.eq(MdmDevicePlanShut::getProcCode, procCode);
        queryWrapper.eq(MdmDevicePlanShut::getMachineType, machineType);
        queryWrapper.eq(MdmDevicePlanShut::getMachineCode, machineCode);
        queryWrapper.ne(BaseEntity::getId, docEntityVO.getId());
        List<MdmDevicePlanShut> mdmDevicePlanShutList = entityMapper.selectList(queryWrapper);
        if (CollectionUtils.isNotEmpty(mdmDevicePlanShutList)) {
            for (MdmDevicePlanShut mdmDevicePlanShut : mdmDevicePlanShutList) {
                long dbBeginTime = mdmDevicePlanShut.getBeginDate().getTime();
                long dbEndTime = mdmDevicePlanShut.getEndDate().getTime();
                long beginTime = beginDate.getTime();
                long endTime = endDate.getTime();
                if (beginTime <= dbEndTime || endTime >= dbBeginTime) {
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
}
