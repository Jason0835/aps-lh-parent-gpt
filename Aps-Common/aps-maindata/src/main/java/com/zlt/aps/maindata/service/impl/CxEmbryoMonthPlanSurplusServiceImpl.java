package com.zlt.aps.maindata.service.impl;

import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.constant.UserConstants;
import com.zlt.aps.maindata.mapper.CxEmbryoMonthPlanSurplusEntityMapper;
import com.zlt.aps.maindata.mapper.CxMonthStockEntityMapper;
import com.zlt.aps.maindata.service.ICxEmbryoMonthPlanSurplusService;
import com.zlt.aps.maindata.utils.ScmListUtils;
import com.zlt.aps.monthplan.api.domain.entity.CxEmbryoMonthPlanSurplus;
import com.zlt.aps.monthplan.api.domain.entity.CxMonthStock;
import com.zlt.aps.monthplan.api.domain.entity.LhMonthPlanSurplus;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.PubUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：CxEmbryoMonthPlanSurplusServiceImpl.java
 * 描    述：CxEmbryoMonthPlanSurplusServiceImpl成型工序胎胚计划量汇总表业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-07
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class CxEmbryoMonthPlanSurplusServiceImpl extends AbstractDocService<CxEmbryoMonthPlanSurplus> implements ICxEmbryoMonthPlanSurplusService {

    @Resource
    private CxEmbryoMonthPlanSurplusEntityMapper cxEmbryoMonthPlanSurplusEntityMapper;
    @Resource
    private CxMonthStockEntityMapper cxMonthStockEntityMapper;

    @Override
    protected String getDocTypeCode() {
        return "CX9001";
    }

    /**
     * 列表查询
     */
    @Override
    public List<CxEmbryoMonthPlanSurplus> selectList(CxEmbryoMonthPlanSurplus queryVO) {
        QueryWrapper<CxEmbryoMonthPlanSurplus> wrapper = new QueryWrapper<>();
        builderConditionBase(wrapper, queryVO);
        return cxEmbryoMonthPlanSurplusEntityMapper.selectList(wrapper);
    }

    @Override
    public void builderCondition(QueryWrapper<CxEmbryoMonthPlanSurplus> queryWrapper, CxEmbryoMonthPlanSurplus queryVO) {
        builderConditionBase(queryWrapper, queryVO);
    }

    @Override
    public void batchSaveOrUpdate(ArrayList<LhMonthPlanSurplus> lhMonthPlanSurpluses) {
        if (CollectionUtils.isEmpty(lhMonthPlanSurpluses)) {
            return;
        }

        //判断embryoList是否大于900 如果大于进行分割
        if (!lhMonthPlanSurpluses.isEmpty()) {
            List<List<LhMonthPlanSurplus>> splitList = ScmListUtils.getSplitList(lhMonthPlanSurpluses, 900);
            for (List<LhMonthPlanSurplus> embryoSplitItemList : splitList) {

                // 1. 提取关键字段组合作为唯一标识
                List<String> uniqueKeys = lhMonthPlanSurpluses.stream()
                        .map(item -> String.format("%s_%s_%s_%s_%s",
                                item.getYear(),
                                item.getMonth(),
                                item.getFactoryCode(),
                                item.getEmbryoCode(),
                                item.getBomDataVersion()))
                        .collect(Collectors.toList());

                // 2. 批量查询已存在记录
                List<CxEmbryoMonthPlanSurplus> existingRecords = cxEmbryoMonthPlanSurplusEntityMapper
                        .selectByUniqueKeys(uniqueKeys);

                // 3. 构建存在记录的映射表
                Map<String, CxEmbryoMonthPlanSurplus> existingMap = existingRecords.stream()
                        .collect(Collectors.toMap(
                                item -> String.format("%s_%s_%s_%s_%s",
                                        item.getYear(),
                                        item.getMonth(),
                                        item.getFactoryCode(),
                                        item.getMaterialCode(),
                                        item.getBomDataVersion()),
                                Function.identity()
                        ));

                // 4. 准备批量插入和更新的数据
                List<CxEmbryoMonthPlanSurplus> toInsert = new ArrayList<>();
                List<CxEmbryoMonthPlanSurplus> toUpdate = new ArrayList<>();

                for (LhMonthPlanSurplus surplus : lhMonthPlanSurpluses) {
                    String key = String.format("%s_%s_%s_%s_%s",
                            surplus.getYear(),
                            surplus.getMonth(),
                            surplus.getFactoryCode(),
                            surplus.getEmbryoCode(),
                            surplus.getBomDataVersion());

                    CxEmbryoMonthPlanSurplus entity = convertToEntity(surplus);

                    if (existingMap.containsKey(key)) {
                        // 更新记录
                        CxEmbryoMonthPlanSurplus existing = existingMap.get(key);
                        existing.setMonthPlanQty(surplus.getMonthPlanQty());
                        existing.setMonthRemainQty(surplus.getMonthPlanQty() - surplus.getMonthFinishQty());
                        toUpdate.add(existing);
                    } else {
                        // 新增记录
                        toInsert.add(entity);
                    }
                }

                // 5. 执行批量操作
                if (!toInsert.isEmpty()) {
                    cxEmbryoMonthPlanSurplusEntityMapper.batchInsert(toInsert);
                }

                if (!toUpdate.isEmpty()) {
                    cxEmbryoMonthPlanSurplusEntityMapper.batchUpdate(toUpdate);
                }
            }
        }
    }

    private CxEmbryoMonthPlanSurplus convertToEntity(LhMonthPlanSurplus surplus) {
        CxEmbryoMonthPlanSurplus entity = new CxEmbryoMonthPlanSurplus();
        entity.setYear(String.valueOf(surplus.getYear()));
        entity.setMonth(String.valueOf(surplus.getMonth()));
        entity.setFactoryCode(surplus.getFactoryCode());
        entity.setMaterialCode(surplus.getEmbryoCode());
        entity.setBomDataVersion(surplus.getBomDataVersion());
        entity.setMonthPlanQty(surplus.getMonthPlanQty());
        entity.setMonthRemainQty(surplus.getMonthPlanQty());
        entity.setLastMonthStock(surplus.getLastMonthStock());


        QueryWrapper<CxMonthStock> stockQueryWrapper = getCxMonthStockQueryWrapper(entity);
        CxMonthStock stock = cxMonthStockEntityMapper.selectOne(stockQueryWrapper);
        if (stock != null) {
            int stockNum = stock.getStockNum() == null ? 0 : stock.getStockNum();
            int overTimeStock = stock.getOverTimeStock() == null ? 0 : stock.getOverTimeStock();
            entity.setLastMonthStock(stockNum - overTimeStock);
        }

        entity.setMonthFinishQty(0);
        entity.setDataSource(String.valueOf(0));
        return entity;
    }

    private QueryWrapper<CxMonthStock> getCxMonthStockQueryWrapper(CxEmbryoMonthPlanSurplus entity) {
        QueryWrapper<CxMonthStock> stockQueryWrapper = new QueryWrapper<>();
        // 创建 Calendar 实例
        Calendar calendar = Calendar.getInstance();
        // 设置年份（例如：2023）
        calendar.set(Calendar.YEAR, Integer.parseInt(entity.getYear()));
        // 设置月份（注意：月份从0开始，0=一月，11=十二月）
        calendar.set(Calendar.MONTH, Integer.parseInt(entity.getMonth()));
        // 设置日为1号（月初）
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        // 清除时分秒毫秒
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // 转换为 Date 对象
        Date month = calendar.getTime();
        stockQueryWrapper.eq("STOCK_MONTH", month);
        stockQueryWrapper.eq("FACTORY_CODE", entity.getFactoryCode());
        stockQueryWrapper.eq("EMBRYO_CODE", entity.getMaterialCode());
        stockQueryWrapper.eq("BOM_DATA_VERSION", entity.getBomDataVersion());
        return stockQueryWrapper;
    }

    public void builderConditionBase(QueryWrapper<CxEmbryoMonthPlanSurplus> queryWrapper, CxEmbryoMonthPlanSurplus queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("monthPlanVersion")), "MONTH_PLAN_VERSION", queryVO.getFieldValueByFieldName("monthPlanVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("year")), "YEAR", queryVO.getFieldValueByFieldName("year"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month")), "MONTH", queryVO.getFieldValueByFieldName("month"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("bomDataVersion")), "BOM_DATA_VERSION", queryVO.getFieldValueByFieldName("bomDataVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialCode")), "MATERIAL_CODE", queryVO.getFieldValueByFieldName("materialCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("monthPlanQty")), "MONTH_PLAN_QTY", queryVO.getFieldValueByFieldName("monthPlanQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lastMonthStock")), "LAST_MONTH_STOCK", queryVO.getFieldValueByFieldName("lastMonthStock"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("monthFinishQty")), "MONTH_FINISH_QTY", queryVO.getFieldValueByFieldName("monthFinishQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("monthRemainQty")), "MONTH_REMAIN_QTY", queryVO.getFieldValueByFieldName("monthRemainQty"));
    }

    @Override
    public String checkUnique(CxEmbryoMonthPlanSurplus cxEmbryoMonthPlanSurplus) {
        LambdaQueryWrapper<CxEmbryoMonthPlanSurplus> lqw = Wrappers.lambdaQuery();
        lqw.ne(cxEmbryoMonthPlanSurplus.getId() != null, CxEmbryoMonthPlanSurplus::getId, cxEmbryoMonthPlanSurplus.getId());
        lqw.eq(cxEmbryoMonthPlanSurplus.getFactoryCode() != null, CxEmbryoMonthPlanSurplus::getFactoryCode, cxEmbryoMonthPlanSurplus.getFactoryCode());
        lqw.eq(cxEmbryoMonthPlanSurplus.getYear() != null, CxEmbryoMonthPlanSurplus::getYear, cxEmbryoMonthPlanSurplus.getYear());
        lqw.eq(cxEmbryoMonthPlanSurplus.getMonth() != null, CxEmbryoMonthPlanSurplus::getMonth, cxEmbryoMonthPlanSurplus.getMonth());
        lqw.eq(cxEmbryoMonthPlanSurplus.getMaterialCode() != null, CxEmbryoMonthPlanSurplus::getMaterialCode, cxEmbryoMonthPlanSurplus.getMaterialCode());
        lqw.eq(cxEmbryoMonthPlanSurplus.getBomDataVersion() != null, CxEmbryoMonthPlanSurplus::getBomDataVersion, cxEmbryoMonthPlanSurplus.getBomDataVersion());
        if (cxEmbryoMonthPlanSurplusEntityMapper.selectCount(lqw) > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode","year","month","materialCode","bomDataVersion");
    }

}




