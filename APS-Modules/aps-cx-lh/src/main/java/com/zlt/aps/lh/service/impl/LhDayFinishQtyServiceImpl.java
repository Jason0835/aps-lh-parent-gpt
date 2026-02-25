package com.zlt.aps.lh.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.zlt.aps.mp.api.domain.entity.MpFactoryProductionVersion;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.exception.BusinessException;
import com.zlt.aps.lh.api.domain.entity.LhDayFinishQty;
import com.zlt.aps.lh.mapper.LhDayFinishQtyMapper;
import com.zlt.aps.lh.service.ILhDayFinishQtyService;
import com.zlt.aps.mp.api.domain.dto.FactoryFinalVersionQueryDto;
import com.zlt.aps.mp.api.service.IFactoryConsoleRemoteService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;

import lombok.extern.slf4j.Slf4j;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：LhDayFinishQtyServiceImpl.java
 * 描    述：LhDayFinishQtyServiceImpl硫化排程日完成量业务层处理
 *@author zlt
 *@date 2025-02-21
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
public class LhDayFinishQtyServiceImpl extends AbstractDocService<LhDayFinishQty>  implements ILhDayFinishQtyService {

    @Autowired
    private LhDayFinishQtyMapper lhDayFinishQtyMapper;

    @Autowired
    private IFactoryConsoleRemoteService factoryConsoleRemoteService;

    @Override
    protected String getDocTypeCode() {
        return "0105";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("0105");
        return sysDocType;
    }

    @Override
    public String checkUnique(LhDayFinishQty docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.lhDayFinishQty.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("factoryCode", "finishDate", "specCode"));
    }

    /**
     * 导入数据，并保存记录
     *
     * @param list        要导入数据
     * @param importLogId 导入日志id
     * @return 导入后提示信息
     */
    @Override
    public AjaxResult importFinishQty(List<LhDayFinishQty> list, Long importLogId) {
        AjaxResult ajaxResult = super.importData(list, true, importLogId);
        // 更新月计划的硫化剩余量
        if (CollectionUtils.isNotEmpty(list)) {
            //lhDayFinishQtyMapper.updateMonthPlanSurplus(list.get(0).getFinishDate());
        }
        return ajaxResult;
    }

    /**
     * 更新月计划的硫化剩余量
     */
    @Override
    public void updateMonthPlanSurplus(String factoryCode,Date scheduleDate) {
        FactoryFinalVersionQueryDto queryDto = new FactoryFinalVersionQueryDto();
        queryDto.setFactoryCode(factoryCode);
        queryDto.setProductionDate(scheduleDate);
        AjaxResult ajaxResult = factoryConsoleRemoteService.getFinalVersion(queryDto);
        MpFactoryProductionVersion productionVersion = new ObjectMapper().convertValue(ajaxResult.get(AjaxResult.DATA_TAG), MpFactoryProductionVersion.class);
        if (productionVersion == null){
            throw new BusinessException("获取不到月度计划定稿版本！");
        }else{
            lhDayFinishQtyMapper.updateByScheduleResult(productionVersion.getYear(),productionVersion.getMonth(),scheduleDate);
            lhDayFinishQtyMapper.insertByScheduleResultNotExist(productionVersion.getYear(),productionVersion.getMonth(),scheduleDate);
            lhDayFinishQtyMapper.updateMonthPlanSurplus(productionVersion.getYear(),productionVersion.getMonth());
        }

    }
}
