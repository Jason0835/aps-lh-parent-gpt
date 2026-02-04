package com.zlt.aps.monthplan.demand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import com.zlt.aps.monthplan.api.domain.entity.DpOrderOffsetDetail;
import com.zlt.aps.monthplan.api.domain.entity.DpOrderPoolSnapshot;
import com.zlt.aps.monthplan.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.entity.SalesOrderPool;
import com.zlt.aps.monthplan.api.domain.entity.SupplyOrderPool;
import com.zlt.aps.monthplan.common.utils.BatchInsertProcessor;
import com.zlt.aps.monthplan.demand.mapper.DpOrderPoolSnapshotEntityMapper;
import com.zlt.aps.monthplan.demand.service.IDpOrderPoolSnapshotService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;

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
    private final static String BRAND_DICT_TYPE = "biz_brand_type";
    private final DpOrderPoolSnapshotEntityMapper dpOrderPoolSnapshotEntityMapper;
    // 批量插入处理器
    private final BatchInsertProcessor<DpOrderPoolSnapshot> batchInsertProcessor;
    // 字典
    private final ISysDictDataCacheService sysDictDataService;

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
            // 品牌字典
            List<SysDictData> brandDictDatas = sysDictDataService.getType(BRAND_DICT_TYPE);
            Map<String,String> valueToLabelMap = this.convertToMap(brandDictDatas);
            supplyOrderPools.forEach(supplyOrder -> orderPoolSnapshots.add(buildOrderPoolSnapshot(createCondition,supplyOrder,valueToLabelMap)));
        }
        if(CollectionUtils.isNotEmpty(orderPoolSnapshots)){
            orderPoolSnapshots.sort(Comparator.comparing(DpOrderPoolSnapshot::getMaterialCode));
            this.batchInsertProcessor.batchInsert(orderPoolSnapshots);
        }
    }

    private Map<String, String> convertToMap(List<SysDictData> brandDictDatas) {
        if(CollectionUtils.isEmpty(brandDictDatas)){
            return Collections.emptyMap();
        }
        return brandDictDatas.stream()
            .filter(dict -> dict != null && dict.getDictValue() != null)
            .collect(Collectors.toMap(
                SysDictData::getDictValue,
                dict -> dict.getDictLabel() != null ? dict.getDictLabel() : "",
                (existing, replacement) -> {
                    return existing;
                },
                LinkedHashMap::new
            ));
    }

    @Override
    public List<DpOrderOffsetDetail> loadSupplyOrder(DpDemandPlan createCondition,MpFactoryProductionVersion finalVersion) {
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
        List<DpOrderOffsetDetail> result = Lists.newArrayList();
        list.forEach(orderPoolSnapshot -> result.add(buildSupplyOrderPool(createCondition,orderPoolSnapshot)));
        return result;
    }

    @Override
    public List<SupplyOrderPool> fetchSupplyOrder(MpFactoryProductionVersion finalVersion) {
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
        BeanUtils.copyProperties(orderPoolSnapshot,supplyOrderPool);
        supplyOrderPool.setId(null);
        supplyOrderPool.setBaseVale(null);
        supplyOrderPool.setOrderType(orderPoolSnapshot.getOrderPriority());
        supplyOrderPool.setQty(orderPoolSnapshot.getDemandQty());
        return supplyOrderPool;
    }

    private DpOrderOffsetDetail buildSupplyOrderPool(DpDemandPlan createCondition,DpOrderPoolSnapshot orderPoolSnapshot) {
        DpOrderOffsetDetail supplyOrderPool = new DpOrderOffsetDetail();
        BeanUtils.copyProperties(orderPoolSnapshot,supplyOrderPool);
        supplyOrderPool.setId(null);
        supplyOrderPool.setBaseVale(null);
        supplyOrderPool.setFactoryCode(createCondition.getFactoryCode());
        supplyOrderPool.setYear(createCondition.getYear());
        supplyOrderPool.setMonth(createCondition.getMonth());
        supplyOrderPool.setMonthPlanVersion(createCondition.getMonthPlanVersion());
        supplyOrderPool.setOrderQty(orderPoolSnapshot.getDemandQty());
        supplyOrderPool.setProduceQtyDue(orderPoolSnapshot.getDemandQty());
        supplyOrderPool.setOrderPriority(orderPoolSnapshot.getOrderPriority());
        supplyOrderPool.setScmPriority(orderPoolSnapshot.getScmPriority());
        return supplyOrderPool;
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

    private DpOrderPoolSnapshot buildOrderPoolSnapshot(DpDemandPlan createCondition, SupplyOrderPool supplyOrder,Map<String,String> valueToLabelMap) {
        DpOrderPoolSnapshot entity = new DpOrderPoolSnapshot();
        BeanUtils.copyProperties(supplyOrder, entity);
        if(StringUtils.isNotBlank(supplyOrder.getBrand()) && valueToLabelMap.containsKey(supplyOrder.getBrand())) {
            entity.setBrand(valueToLabelMap.get(supplyOrder.getBrand()));
        }
        entity.setId(null);
        entity.setBaseVale(null);
        entity.setYear(createCondition.getYear());
        entity.setMonth(createCondition.getMonth());
        entity.setMonthPlanVersion(createCondition.getMonthPlanVersion());
        entity.setOrderPriority(supplyOrder.getOrderType());
        entity.setScmPriority(supplyOrder.getOrderType());
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
