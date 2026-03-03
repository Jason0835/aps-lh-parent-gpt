package com.zlt.aps.mp.demand.service.impl;

import com.google.common.collect.Lists;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.api.domain.entity.DpDemandPlan;
import com.zlt.aps.mp.api.domain.entity.DpStockVersion;
import com.zlt.aps.mp.api.domain.entity.MdmProductStock;
import com.zlt.aps.mp.common.utils.BatchInsertProcessor;
import com.zlt.aps.mp.demand.mapper.DpStockVersionEntityMapper;
import com.zlt.aps.mp.demand.service.IDpStockVersionService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：DpStockVersionServiceImpl.java
 * 描    述：DpStockVersionServiceImpl需求计划_版本库存业务层处理
 *@author yelq
 *@date 2025-12-20
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
public class DpStockVersionServiceImpl extends AbstractDocService<DpStockVersion>  implements IDpStockVersionService {
    // 批量插入处理器
    private final BatchInsertProcessor<DpStockVersion> batchInsertProcessor;
    private final DpStockVersionEntityMapper dpStockVersionEntityMapper;
    @Override
    protected String getDocTypeCode() {
        return "2025122021";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("2025122021");
        return sysDocType;
    }

    @Override
    public String checkUnique(DpStockVersion docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.dpStockVersion.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }

    @Override
    public void insertBatchData(DpDemandPlan demandPlan,  Map<String, List<MdmProductStock>> finishedProductStockMap) {
        if (CollectionUtils.isEmpty(finishedProductStockMap)) {
            return;
        }
        List<DpStockVersion> list = Lists.newArrayList();
        List<MdmProductStock> finishedProductStocks = flattenStockMap(finishedProductStockMap);
        finishedProductStocks.forEach(finishedProductStock -> {
            DpStockVersion requireStock = this.buildRequireStock(demandPlan,finishedProductStock);
            list.add(requireStock);
        });
        list.sort(Comparator.comparing(DpStockVersion::getMaterialCode));
        this.batchInsertProcessor.batchInsert(list);
    }

    private DpStockVersion buildRequireStock(DpDemandPlan demandPlan,MdmProductStock finishedProductStock) {
        DpStockVersion requireStock = new DpStockVersion();
        BeanUtils.copyProperties(finishedProductStock, requireStock);
        requireStock.setId(null);
        requireStock.setRequireVersion(demandPlan.getMonthPlanVersion());
        requireStock.setIsDelete(YesOrNoEnum.NO.getValue());
        requireStock.setRemainingQty(finishedProductStock.getLeftOverQty());
        requireStock.setBaseVale(null);
        requireStock.setYear(demandPlan.getYear());
        requireStock.setMonth(demandPlan.getMonth());
        return requireStock;
    }

    /**
     * 将Map转换为List<MpFinishedProductStock>
     */
    public List<MdmProductStock> flattenStockMap(
        Map<String, List<MdmProductStock>> finishedProductStockMap) {
        // 使用Stream扁平化转换
        return finishedProductStockMap.values().stream()
            .flatMap(List::stream)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * 获取需求计划版本号列表
     *
     * @param queryCondition 查询条件
     * @return 需求计划版本号列表
     */
    @Override
    public List<String> findMonthPlanVersion(DpStockVersion queryCondition) {
        return dpStockVersionEntityMapper.selectDistinctMonthPlanVersion(
                queryCondition.getFactoryCode(),
                queryCondition.getYear(),
                queryCondition.getMonth(),
                YesOrNoEnum.NO.getValue()
        );
    }
}
