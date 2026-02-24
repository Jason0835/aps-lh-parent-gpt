package com.zlt.aps.monthplan.demand.service.impl;

import com.google.common.collect.Lists;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import com.zlt.aps.monthplan.api.domain.entity.DpStockVersion;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductStock;
import com.zlt.aps.monthplan.common.utils.BatchInsertProcessor;
import com.zlt.aps.monthplan.demand.service.IDpStockVersionService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import com.zlt.bill.common.service.AbstractDocService;
import com.ruoyi.common.exception.ServiceException;
import org.springframework.util.CollectionUtils;

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
    public void insertBatchData(DpDemandPlan createCondition, String monthPlanVersion, Map<String, List<MdmProductStock>> finishedProductStockMap) {
        if (CollectionUtils.isEmpty(finishedProductStockMap)) {
            return;
        }
        List<DpStockVersion> list = Lists.newArrayList();
        List<MdmProductStock> finishedProductStocks = flattenStockMap(finishedProductStockMap);
        finishedProductStocks.forEach(finishedProductStock -> {
            DpStockVersion requireStock = this.buildRequireStock(createCondition,monthPlanVersion,finishedProductStock);
            list.add(requireStock);
        });
        list.sort(Comparator.comparing(DpStockVersion::getMaterialCode));
        this.batchInsertProcessor.batchInsert(list);
    }

    private DpStockVersion buildRequireStock(DpDemandPlan createCondition, String monthPlanVersion, MdmProductStock finishedProductStock) {
        DpStockVersion requireStock = new DpStockVersion();
        BeanUtils.copyProperties(finishedProductStock, requireStock);
        requireStock.setId(null);
        requireStock.setRequireVersion(monthPlanVersion);
        requireStock.setIsDelete(YesOrNoEnum.NO.getValue());
        requireStock.setRemainingQty(finishedProductStock.getLeftOverQty());
        requireStock.setBaseVale(null);
        requireStock.setYear(createCondition.getYear());
        requireStock.setMonth(createCondition.getMonth());
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
}
