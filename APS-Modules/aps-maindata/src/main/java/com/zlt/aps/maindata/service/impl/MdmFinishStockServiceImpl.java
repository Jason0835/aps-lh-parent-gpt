package com.zlt.aps.maindata.service.impl;

import com.google.common.collect.Lists;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.maindata.mapper.MdmFinishStockEntityMapper;
import com.zlt.aps.maindata.service.IMdmFinishStockService;
import com.zlt.aps.monthplan.api.domain.entity.MdmFinishStock;
import com.zlt.aps.monthplan.api.domain.entity.MpDemandPlan;
import com.zlt.aps.monthplan.api.domain.entity.MpFinishedProductStock;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmFinishStockServiceImpl.java
 * 描    述：MdmFinishStockServiceImpl成品库存业务层处理
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
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class MdmFinishStockServiceImpl extends AbstractDocService<MdmFinishStock> implements IMdmFinishStockService {

    private final MdmFinishStockEntityMapper finishStockEntityMapper;
    @Override
    protected String getDocTypeCode() {
        return "MDM0139";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MDM0139");
        return sysDocType;
    }

    @Override
    public String checkUnique(MdmFinishStock docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mdmFinishStock.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return new ArrayList<>(Arrays.asList("factoryCode", "year", "month", "requireVersion", "materialCode"));
    }

    /**
     * 查询MES实时成品库存列表
     *
     * @param queryVO 查询参数
     * @return 结果
     */
    @Override
    public List<MdmFinishStock> list4Mes(MdmFinishStock queryVO) {
        // steve's TODO 查询MES实时成品库存列表
        return Collections.emptyList();
    }

    @Override
    public void insertBatchData(MpDemandPlan createCondition, String monthPlanVersion, Map<String, List<MpFinishedProductStock>> finishedProductStockMap) {
        if (CollectionUtils.isEmpty(finishedProductStockMap)) {
            return;
        }
        List<MdmFinishStock> list = Lists.newArrayList();
        List<MpFinishedProductStock> finishedProductStocks = flattenStockMap(finishedProductStockMap);
        finishedProductStocks.forEach(finishedProductStock -> {
            MdmFinishStock requireStock = this.buildRequireStock(createCondition,monthPlanVersion,finishedProductStock);
            list.add(requireStock);
        });
        this.baseDao.insertBatch(list);
    }

    private MdmFinishStock buildRequireStock(MpDemandPlan createCondition, String monthPlanVersion, MpFinishedProductStock finishedProductStock) {
        MdmFinishStock requireStock = new MdmFinishStock();
        BeanUtils.copyProperties(finishedProductStock, requireStock);
        requireStock.setId(null);
        requireStock.setRequireVersion(monthPlanVersion);
        requireStock.setIsDelete(YesOrNoEnum.NO.getValue());
        requireStock.setRemainingQty(finishedProductStock.getLeftOverQty());
        requireStock.setBaseVale(null);
        requireStock.setYear(createCondition.getYear());
        requireStock.setMonth(createCondition.getMonth());
        requireStock.setDomesticExportSale(finishedProductStock.getLocationType());
        return requireStock;
    }

    /**
     * 将Map转换为List<MpFinishedProductStock>
     */
    public List<MpFinishedProductStock> flattenStockMap(
        Map<String, List<MpFinishedProductStock>> finishedProductStockMap) {
        // 使用Stream扁平化转换
        return finishedProductStockMap.values().stream()
            .flatMap(List::stream)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
