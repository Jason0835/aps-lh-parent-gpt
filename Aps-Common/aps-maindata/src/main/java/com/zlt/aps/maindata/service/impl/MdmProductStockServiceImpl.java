package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.ApsNumberUtils;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.maindata.mapper.MdmProductStockEntityMapper;
import com.zlt.aps.maindata.service.IMdmProductStockService;
import com.zlt.aps.mp.api.domain.entity.MdmOutbountOrdersNotScan;
import com.zlt.aps.mp.api.domain.entity.MdmProductStock;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmProductStockServiceImpl.java
 * 描    述：MdmProductStockServiceImpl成品库存业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-22
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class MdmProductStockServiceImpl extends AbstractDocService<MdmProductStock> implements IMdmProductStockService {

    @Autowired
    private MdmProductStockEntityMapper mdmProductStockEntityMapper;

    @Autowired
    private IMesItfService mesItfService;

    private final static String ZERO_YEAR_WEEK = "0000";

    @Override
    protected String getDocTypeCode() {
        return "MDM0216";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MDM0216");
        return sysDocType;
    }

    @Override
    public String checkUnique(MdmProductStock docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mdmProductStock.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }

    @Override
    public List<MdmProductStock> findCurrentFinishStock(String factoryCode) {
        LambdaQueryWrapper<MdmProductStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MdmProductStock::getFactoryCode, factoryCode);
        wrapper.eq(MdmProductStock::getIsDelete, YesOrNoEnum.NO.getValue());
        return this.mdmProductStockEntityMapper.selectList(wrapper);
    }

    @Override
    public List<MdmProductStock> getMpFinishedProductStockByMaterialCode(String materialCode) {
        LambdaQueryWrapper<MdmProductStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MdmProductStock::getMaterialCode, materialCode);
        wrapper.eq(MdmProductStock::getIsDelete, YesOrNoEnum.NO.getValue());
        return this.mdmProductStockEntityMapper.selectList(wrapper);
    }

    @Override
    public List<MdmProductStock> findCurrentFinishStock(String factoryCode, Set<String> skus) {
        List<MdmProductStock> result = Lists.newArrayList();
        final int batchSize = 1000;
        List<String> skuList = new ArrayList<>(skus);
        for (int i = 0; i < skus.size(); i += batchSize) {
            int end = Math.min(i + batchSize, skus.size());
            List<String> batchSkus = skuList.subList(i, end);
            LambdaQueryWrapper<MdmProductStock> wrapper =
                Wrappers.lambdaQuery(MdmProductStock.class)
                    .eq(MdmProductStock::getFactoryCode, factoryCode)
                    .in(MdmProductStock::getMaterialCode, batchSkus)
                    .eq(MdmProductStock::getIsDelete, ApsConstant.APS_YES_NO_0);
            result.addAll(mdmProductStockEntityMapper.selectList(wrapper));
        }
        return result;
    }
    

    /**
     * 库存冲减未扫描订单
     * 
     * @param finishedProductStocks 成品库存列表
     * @param notScanOrderList      未扫描订单列表
     */
    @Override
    public void reduceInventoryByNotScanOrder(List<MdmProductStock> finishedProductStocks, List<MdmOutbountOrdersNotScan> notScanOrderList) {
        if (CollectionUtils.isEmpty(finishedProductStocks)) {
            return;
        }
        // 1、先按物料号分组，再按年周号顺序分组
        Map<String, TreeMap<String, List<MdmProductStock>>> stockGroupMap = finishedProductStocks.stream()
                .collect(Collectors.groupingBy(MdmProductStock::getMaterialCode, // 第一层分组：物料号
                        Collectors.collectingAndThen(Collectors.toList(),
                                list -> list.stream().collect(Collectors.groupingBy(finishedProductStock -> this
                                        .getWeekYearCompareKey(finishedProductStock.getWeekYear()) // 第二层分组：重构后的年周号，把年份放前面，周次放后面，方便比较
                                        , TreeMap::new, Collectors.toList()))))); // 使用treeMap分组，年周号作为key，可以快速找到与年周号要求最接近的库存数据

        // 2、遍历未扫描订单列表，依次扣减库存
        for (MdmOutbountOrdersNotScan notScanOrder : notScanOrderList) {
            String dot = this.getWeekYearCompareKey(notScanOrder.getDot()); // 未扫描订单年周号要求
            String sapCode = notScanOrder.getSapCode(); // NC物料号
            Integer noscanAmount = BigDecimalUtils.valueOf(notScanOrder.getNoscanAmount()).intValue(); // 未扫描数量
            // 2.1、取出物料各年周号的库存列表
            TreeMap<String, List<MdmProductStock>> stockYearWeekGroupMap = stockGroupMap.get(sapCode);
            // 2.2、按年周号由低到高依次依次冲减，一个年周号的库存不够则继续取更新年周号的库存，直到库存耗尽
            while (noscanAmount > 0 && !CollectionUtils.isEmpty(stockYearWeekGroupMap)) {
                // 2.2.1、取大于且最接近订单年周要求的库存数据
                String stockWeekYear = stockYearWeekGroupMap.ceilingKey(dot);
                if (StringUtils.isEmpty(stockWeekYear)) { // 没有符合条件的要求
                    break;
                }
                // 2.2.2、根据年周号取出库存列表
                List<MdmProductStock> stockList = stockYearWeekGroupMap.get(stockWeekYear);
                if (CollectionUtils.isEmpty(stockList)) {
                    stockYearWeekGroupMap.remove(stockWeekYear);
                    continue;
                }
                // 2.3.3、内层循环，依次冲减未扫描数量，直到库存耗尽或者冲减完毕，每一笔耗尽的库存记录需要从列表中删除
                for (int i = stockList.size() - 1; i >= 0; i--) { // 由于有移除操作，需要倒序遍历
                    // 2.3.3.1、取出剩余库存执行库存冲减运算
                    MdmProductStock stockInfo = stockList.get(i);
                    Integer stockQty = ApsNumberUtils.intValue(stockInfo.getStockQty());
                    Integer allocationStockQty = Math.min(stockQty, noscanAmount);
                    stockQty -= allocationStockQty;
                    noscanAmount -= allocationStockQty;
                    stockInfo.setStockQty(stockQty);
                    // 2.3.3.2、剩余库存不足，则从列表移除改库存记录
                    if (stockQty <= 0) {
                        stockList.remove(i);
                    }
                    // 2.3.3.3、冲减完毕，结束内层循环
                    if (noscanAmount <= 0) {
                        break;
                    }
                }
                // 同一年周号的库存全部耗尽，从列表移除该年周号记录
                if (CollectionUtils.isEmpty(stockList)) {
                    stockYearWeekGroupMap.remove(stockWeekYear);
                }
            }
        }

        // 3、初始化库存剩余量 = 库存量
        finishedProductStocks.forEach(finishedProductStock -> {
            Integer leftOverQty = ApsNumberUtils.intValue(finishedProductStock.getStockQty());
            finishedProductStock.setLeftOverQty(leftOverQty);
        });
        if (CollectionUtils.isEmpty(notScanOrderList)) {
            return;
        }
    }

    /**
     * 重构年周号，把年份放前面，周次放后面，方便比较
     * @param weekYear
     * @return
     */
    private String getWeekYearCompareKey(String weekYear) {
        String newWeekYearStr = null;
        if (StringUtils.isEmpty(weekYear)) {
            newWeekYearStr = ZERO_YEAR_WEEK;
        } else if (weekYear.equals(ZERO_YEAR_WEEK)) {
            newWeekYearStr = weekYear;
        } else if (StringUtils.length(weekYear) == 4) {
            newWeekYearStr = StringUtils.join(weekYear.charAt(2), weekYear.charAt(3), // 原先的后两位年份，前置
                    weekYear.charAt(0), weekYear.charAt(1));// 原先的前两位周次，后置
        }
        return newWeekYearStr;
    }
}
