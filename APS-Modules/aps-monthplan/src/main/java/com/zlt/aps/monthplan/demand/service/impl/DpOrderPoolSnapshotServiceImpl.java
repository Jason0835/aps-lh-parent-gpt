package com.zlt.aps.monthplan.demand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import com.zlt.aps.monthplan.api.domain.entity.DpOrderPoolSnapshot;
import com.zlt.aps.monthplan.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.entity.SalesOrderPool;
import com.zlt.aps.monthplan.api.domain.entity.SupplyOrderPool;
import com.zlt.aps.monthplan.demand.mapper.DpOrderPoolSnapshotEntityMapper;
import com.zlt.aps.monthplan.demand.service.IDpOrderPoolSnapshotService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.YearMonth;
import java.util.Collections;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import com.zlt.bill.common.service.AbstractDocService;
import com.ruoyi.common.exception.ServiceException;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：DpOrderPoolSnapshotServiceImpl.java
 * 描    述：DpOrderPoolSnapshotServiceImplS1-0206.订单池快照业务层处理
 *@author yelq
 *@date 2025-12-26
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class DpOrderPoolSnapshotServiceImpl extends AbstractDocService<DpOrderPoolSnapshot>  implements IDpOrderPoolSnapshotService {

    private final DpOrderPoolSnapshotEntityMapper dpOrderPoolSnapshotEntityMapper;
    @Override
    protected String getDocTypeCode() {
        return "2025122615";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("2025122615");
        return sysDocType;
    }

    @Override
    public String checkUnique(DpOrderPoolSnapshot docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.dpOrderPoolSnapshot.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }

    @Override
    public void saveOrderPoolSnapshot(DpDemandPlan createCondition, List<SalesOrderPool> salesOrders, List<SupplyOrderPool> supplyOrderPools) {
        List<DpOrderPoolSnapshot> orderPoolSnapshots = Lists.newArrayList();
        if(CollectionUtils.isNotEmpty(salesOrders)){
            salesOrders.forEach(saleOrder -> orderPoolSnapshots.add(buildOrderPoolSnapshot(createCondition,saleOrder)));
        }
        if(CollectionUtils.isNotEmpty(supplyOrderPools)){
            supplyOrderPools.forEach(supplyOrder -> orderPoolSnapshots.add(buildOrderPoolSnapshot(createCondition,supplyOrder)));
        }
        if(CollectionUtils.isNotEmpty(orderPoolSnapshots)){
            this.baseDao.insertBatch(orderPoolSnapshots);
        }
    }

    @Override
    public void saveOrderPoolSnapshot(String predictionVersion, YearMonth yearMonth, List<SupplyOrderPool> allStockUpOrders) {
        List<DpOrderPoolSnapshot> orderPoolSnapshots = Lists.newArrayList();
        if(CollectionUtils.isNotEmpty(allStockUpOrders)){
            allStockUpOrders.forEach(supplyOrder -> orderPoolSnapshots.add(buildOrderPoolSnapshot(predictionVersion,yearMonth,supplyOrder)));
        }
        if(CollectionUtils.isNotEmpty(orderPoolSnapshots)){
            this.baseDao.insertBatch(orderPoolSnapshots);
        }
    }

    @Override
    public List<SupplyOrderPool> fetchSupplyOrderPool(MpFactoryProductionVersion finalVersion) {
        LambdaQueryWrapper<DpOrderPoolSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DpOrderPoolSnapshot::getFactoryCode, finalVersion.getFactoryCode());
        wrapper.eq(DpOrderPoolSnapshot::getYear, finalVersion.getYear());
        wrapper.eq(DpOrderPoolSnapshot::getMonth, finalVersion.getMonth());
        wrapper.eq(DpOrderPoolSnapshot::getMonthPlanVersion,finalVersion.getMonthPlanVersion());
        wrapper.in(DpOrderPoolSnapshot::getOrderPriority,Lists.newArrayList(ApsConstant.SAL_PRIORITY_CYCLE_STOCK_UP,ApsConstant.SAL_PRIORITY_PRECEDENT_STOCK_UP));
        wrapper.eq(DpOrderPoolSnapshot::getIsDelete, YesOrNoEnum.NO.getValue());
        List<DpOrderPoolSnapshot> list =  this.dpOrderPoolSnapshotEntityMapper.selectList(wrapper);
        if(CollectionUtils.isEmpty(list)){
            return Collections.emptyList();
        }
        List<SupplyOrderPool> result = Lists.newArrayList();
        list.forEach(orderPoolSnapshot -> result.add(buildSupplyOrderPool(orderPoolSnapshot)));
        return result;
    }

    private SupplyOrderPool buildSupplyOrderPool(DpOrderPoolSnapshot orderPoolSnapshot) {
        SupplyOrderPool supplyOrderPool = new SupplyOrderPool();
        supplyOrderPool.setFactoryCode(orderPoolSnapshot.getFactoryCode());
        supplyOrderPool.setYear(orderPoolSnapshot.getYear());
        supplyOrderPool.setMonth(orderPoolSnapshot.getMonth());
        supplyOrderPool.setBrand(orderPoolSnapshot.getBrand());
        supplyOrderPool.setOrderType(orderPoolSnapshot.getOrderPriority());
        supplyOrderPool.setMaterialCode(orderPoolSnapshot.getMaterialCode());
        supplyOrderPool.setMaterialDesc(orderPoolSnapshot.getMaterialDesc());
        supplyOrderPool.setQty(orderPoolSnapshot.getDemandQty());
        supplyOrderPool.setSaleArea(orderPoolSnapshot.getAreaCode());
        return supplyOrderPool;
    }

    private DpOrderPoolSnapshot buildOrderPoolSnapshot(String predictionVersion, YearMonth yearMonth, SupplyOrderPool supplyOrder) {
        DpOrderPoolSnapshot entity = new DpOrderPoolSnapshot();
        BeanUtils.copyProperties(supplyOrder, entity);
        entity.setId(null);
        entity.setBaseVale(null);
        entity.setYear(yearMonth.getYear());
        entity.setMonth(yearMonth.getMonthValue());
        entity.setMonthPlanVersion(predictionVersion);
        entity.setOrderPriority(supplyOrder.getOrderType());
        // entity.setAreaCode();
        // entity.setCustomCode();
        // entity.setCustomName();
        // entity.setCustomNationCode();
        entity.setDemandQty(supplyOrder.getQty());
        // entity.setDestinationNationCode();
        // entity.setIsDynamicBalance();
        // entity.setIsUniformity();
        // entity.setPoNumber();
        // entity.setSubmitDate();
        // entity.setScmId();
        return entity;
    }


    private DpOrderPoolSnapshot buildOrderPoolSnapshot(DpDemandPlan createCondition, SalesOrderPool saleOrder) {
        DpOrderPoolSnapshot entity = new DpOrderPoolSnapshot();
        BeanUtils.copyProperties(saleOrder, entity);
        entity.setId(null);
        entity.setBaseVale(null);
        entity.setYear(createCondition.getYear());
        entity.setMonth(createCondition.getMonth());
        entity.setMonthPlanVersion(createCondition.getMonthPlanVersion());
        //entity.setMesMaterialCode(saleOrder.);
        entity.setMaterialCode(saleOrder.getOriMaterialCode());
        entity.setAreaCode(saleOrder.getArea());
        entity.setCustomCode(saleOrder.getSalCode());
        // entity.setCustomName();
        entity.setCustomNationCode(saleOrder.getSalNCode());
        entity.setDemandQty(saleOrder.getOrdQty() == null?0:saleOrder.getOrdQty().intValue());
        entity.setDestinationNationCode(saleOrder.getNatCode());
        entity.setIsDynamicBalance(saleOrder.getIsDynamicBalance());
        entity.setIsUniformity(saleOrder.getIsUniformity());
        entity.setPoNumber(saleOrder.getSalCodePo());
        entity.setSubmitDate(saleOrder.getBillDate());
        entity.setScmId(saleOrder.getScmDetailId());
        //entity.setPredictionVersion();
        return entity;
    }

    private DpOrderPoolSnapshot buildOrderPoolSnapshot(DpDemandPlan createCondition, SupplyOrderPool supplyOrder) {
        DpOrderPoolSnapshot entity = new DpOrderPoolSnapshot();
        BeanUtils.copyProperties(supplyOrder, entity);
        entity.setId(null);
        entity.setBaseVale(null);
        entity.setYear(createCondition.getYear());
        entity.setMonth(createCondition.getMonth());
        entity.setMonthPlanVersion(createCondition.getMonthPlanVersion());
        entity.setOrderPriority(supplyOrder.getOrderType());
        // entity.setAreaCode();
        // entity.setCustomCode();
        // entity.setCustomName();
        // entity.setCustomNationCode();
        entity.setDemandQty(supplyOrder.getQty());
        // entity.setDestinationNationCode();
        // entity.setIsDynamicBalance();
        // entity.setIsUniformity();
        // entity.setPoNumber();
        // entity.setSubmitDate();
        // entity.setScmId();
        return entity;
    }
}
