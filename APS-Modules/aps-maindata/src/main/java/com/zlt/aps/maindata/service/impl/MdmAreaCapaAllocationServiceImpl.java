package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.maindata.mapper.MdmAreaCapaAllocationEntityMapper;
import com.zlt.aps.maindata.service.IMdmAreaCapaAllocationService;
import com.zlt.aps.monthplan.api.domain.entity.MdmAreaCapaAllocation;
import com.zlt.aps.monthplan.api.domain.entity.MpDemandPlan;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmAreaCapaAllocationServiceImpl.java
 * 描    述：MdmAreaCapaAllocationServiceImpl区域产能分配业务层处理
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
public class MdmAreaCapaAllocationServiceImpl extends AbstractDocService<MdmAreaCapaAllocation> implements IMdmAreaCapaAllocationService {

    @Autowired
    private MdmAreaCapaAllocationEntityMapper mdmAreaCapaAllocationEntityMapper;

    @Override
    protected String getDocTypeCode() {
        return "MDM0141";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MDM0141");
        return sysDocType;
    }

    @Override
    public String checkUnique(MdmAreaCapaAllocation docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mdmAreaCapaAllocation.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return new ArrayList<>(Arrays.asList("factoryCode", "year", "month", "areaCode"));
    }

    /**
     * 复制
     *
     * @param entity 参数
     * @return 结果
     */
    @Override
    public AjaxResult copy(MdmAreaCapaAllocation entity) {
        String targetFactoryCode = entity.getTargetFactoryCode();
        Integer targetYear = entity.getTargetYear();
        Integer targetMonth = entity.getTargetMonth();
        LambdaUpdateWrapper<MdmAreaCapaAllocation> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(MdmAreaCapaAllocation::getFactoryCode, targetFactoryCode)
                .eq(MdmAreaCapaAllocation::getYear, targetYear)
                .eq(MdmAreaCapaAllocation::getMonth, targetMonth)
                .set(BaseEntity::getIsDelete, ApsConstant.DEL_FLAG_DEL);
        mdmAreaCapaAllocationEntityMapper.update(null, updateWrapper);
        mdmAreaCapaAllocationEntityMapper.copy(entity);
        return AjaxResult.success();
    }

    /**
     * 复制前校验
     *
     * @param entity 参数
     * @return 结果
     */
    @Override
    public AjaxResult checkBeforeCopy(MdmAreaCapaAllocation entity) {
        String sourceFactoryCode = entity.getSourceFactoryCode();
        Integer sourceYear = entity.getSourceYear();
        Integer sourceMonth = entity.getSourceMonth();
        String targetFactoryCode = entity.getTargetFactoryCode();
        Integer targetYear = entity.getTargetYear();
        Integer targetMonth = entity.getTargetMonth();
        if (sourceFactoryCode.equals(targetFactoryCode) && sourceYear.equals(targetYear) && sourceMonth.equals(targetMonth)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.mdmAreaCapaAllocation.sourceAndTargetEqual"), ApsConstant.APS_YES_NO_0);
        }
        List<MdmAreaCapaAllocation> sourceList = selectByFactoryAndYearMonth(sourceFactoryCode, sourceYear, sourceMonth);
        if (CollectionUtils.isEmpty(sourceList)) {
            return AjaxResult.error(String.format(I18nUtil.getMessage("ui.data.alert.mdmAreaCapaAllocation.sourceNotExist"), sourceYear, sourceMonth), ApsConstant.APS_YES_NO_0);
        }
        List<MdmAreaCapaAllocation> targetList = selectByFactoryAndYearMonth(targetFactoryCode, targetYear, targetMonth);
        if (CollectionUtils.isNotEmpty(targetList)) {
            return AjaxResult.success(String.format(I18nUtil.getMessage("ui.data.alert.mdmAreaCapaAllocation.targetExists"), targetYear, targetMonth), ApsConstant.APS_YES_NO_1);
        }
        return AjaxResult.success(ApsConstant.APS_YES_NO_1);
    }

    @Override
    public List<MdmAreaCapaAllocation> findAreaCapaAllocation(MpDemandPlan createCondition) {
        LambdaQueryWrapper<MdmAreaCapaAllocation> sourceWrapper = new LambdaQueryWrapper<>();
        sourceWrapper
            .eq(MdmAreaCapaAllocation::getYear, createCondition.getYear())
            .eq(MdmAreaCapaAllocation::getMonth, createCondition.getMonth())
            .eq(MdmAreaCapaAllocation::getIsDelete, YesOrNoEnum.NO.getValue());
        return mdmAreaCapaAllocationEntityMapper.selectList(sourceWrapper);
    }


    private List<MdmAreaCapaAllocation> selectByFactoryAndYearMonth(String factoryCode, Integer year, Integer month) {
        LambdaQueryWrapper<MdmAreaCapaAllocation> sourceWrapper = new LambdaQueryWrapper<>();
        sourceWrapper.eq(MdmAreaCapaAllocation::getFactoryCode, factoryCode)
                .eq(MdmAreaCapaAllocation::getYear, year)
                .eq(MdmAreaCapaAllocation::getMonth, month);
        return mdmAreaCapaAllocationEntityMapper.selectList(sourceWrapper);
    }
}
