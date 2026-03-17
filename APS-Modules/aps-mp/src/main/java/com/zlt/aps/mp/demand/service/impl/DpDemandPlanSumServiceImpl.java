package com.zlt.aps.mp.demand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.enums.ProductionPlanType;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.api.domain.entity.DpDemandPlan;
import com.zlt.aps.mp.api.domain.entity.DpDemandPlanSum;
import com.zlt.aps.mp.api.domain.vo.FactoryProductionPlanVo;
import com.zlt.aps.mp.demand.mapper.DpDemandPlanEntityMapper;
import com.zlt.aps.mp.demand.mapper.DpDemandPlanSumEntityMapper;
import com.zlt.aps.mp.demand.service.IDpDemandPlanSumService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：DpDemandPlanSumServiceImpl.java
 * 描    述：DpDemandPlanSumServiceImpl需求计划汇总业务层处理
 *@author yelq
 *@date 2026-01-22
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class DpDemandPlanSumServiceImpl extends AbstractDocService<DpDemandPlanSum>  implements IDpDemandPlanSumService {
    private final DpDemandPlanEntityMapper demandPlanEntityMapper;
    private final DpDemandPlanSumEntityMapper dpDemandPlanSumEntityMapper;
    @Override
    protected String getDocTypeCode() {
        return "2026012216";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("2026012216");
        return sysDocType;
    }

    @Override
    public String checkUnique(DpDemandPlanSum docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.dpDemandPlanSum.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }

    @Override
    public void batchUpdateForDemand(DpDemandPlanSum billVO) {
        DpDemandPlanSum existObj =  dpDemandPlanSumEntityMapper.selectById(billVO.getId());
        updateDpDemandPlanSum(billVO);
        List<DpDemandPlan> list = this.findDemandPlan(existObj);
        if(CollectionUtils.isEmpty(list)) {
            return;
        }
        if(StringUtils.isNotBlank(billVO.getScmPriority())) {
            list.forEach(dpDemandPlan -> dpDemandPlan.setScmPriority(billVO.getScmPriority()));
            this.baseDao.updateBatch(list);
            return;
        }
        list.forEach(dpDemandPlan -> dpDemandPlan.setIsProduction(billVO.getIsProduction()));
        this.baseDao.updateBatch(list);
    }

    @Override
    public List<String> findMonthPlanVersion(DpDemandPlanSum queryCondition) {
        return dpDemandPlanSumEntityMapper.selectDistinctMonthPlanVersion(
            queryCondition.getFactoryCode(),
            queryCondition.getYear(),
            queryCondition.getMonth(),
            ProductionPlanType.NORMAL.getPlanType(),
            YesOrNoEnum.NO.getValue()
        );
    }

    @Override
    public DpDemandPlanSum getDpDemandPlanSumByParam(FactoryProductionPlanVo selectedRequireVersion) {
        LambdaQueryWrapper<DpDemandPlanSum> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(DpDemandPlanSum::getFactoryCode, selectedRequireVersion.getFactoryCode());
        wrapper.eq(DpDemandPlanSum::getYear, selectedRequireVersion.getYear());
        wrapper.eq(DpDemandPlanSum::getMonth, selectedRequireVersion.getMonth());
        wrapper.eq(DpDemandPlanSum::getMonthPlanVersion, selectedRequireVersion.getMonthPlanVersion());
        wrapper.eq(DpDemandPlanSum::getIsDelete, YesOrNoEnum.NO.getValue());
        List<DpDemandPlanSum>  list = dpDemandPlanSumEntityMapper.selectList(wrapper);
        if(CollectionUtils.isEmpty(list)) {
            return null;
        }
        return list.get(0);
    }

    private void updateDpDemandPlanSum(DpDemandPlanSum billVO) {
        if(StringUtils.isNotBlank(billVO.getScmPriority())) {
            billVO.setScmPriority(billVO.getScmPriority());
            this.dpDemandPlanSumEntityMapper.updateById(billVO);
            return;
        }
        billVO.setIsProduction(billVO.getIsProduction());
        this.dpDemandPlanSumEntityMapper.updateById(billVO);

    }

    private List<DpDemandPlan> findDemandPlan(DpDemandPlanSum existObj) {
        LambdaQueryWrapper<DpDemandPlan> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(DpDemandPlan::getFactoryCode, existObj.getFactoryCode());
        wrapper.eq(DpDemandPlan::getYear, existObj.getYear());
        wrapper.eq(DpDemandPlan::getMonth, existObj.getMonth());
        wrapper.eq(DpDemandPlan::getProductTypeCode, existObj.getProductTypeCode());
        wrapper.eq(DpDemandPlan::getMonthPlanVersion, existObj.getMonthPlanVersion());
        wrapper.eq(DpDemandPlan::getMaterialDesc, existObj.getMaterialDesc());
        wrapper.eq(DpDemandPlan::getIsDelete, YesOrNoEnum.NO.getValue());
        return demandPlanEntityMapper.selectList(wrapper);

    }
}
