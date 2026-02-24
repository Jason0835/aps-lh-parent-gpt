package com.zlt.aps.monthplan.factory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.zlt.aps.monthplan.api.domain.entity.ProductionMonthPlanInit;
import com.zlt.aps.utils.JsonUtils;
import com.zlt.aps.monthplan.factory.mapper.ProductionMonthPlanInitMapper;
import com.zlt.aps.monthplan.factory.service.IProductionMonthPlanInitService;
import com.zlt.common.utils.PubUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ProductionMonthPlanInitServiceImpl.java
 * 描    述：ProductionMonthPlanInitServiceImpl分厂月生产计划排产过程-计划初始化业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-17
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class ProductionMonthPlanInitServiceImpl implements IProductionMonthPlanInitService {

    private final ProductionMonthPlanInitMapper productionMonthPlanInitMapper;

    /**
     * 列表查询
     */
    @Override
    public List<ProductionMonthPlanInit> getDataList(ProductionMonthPlanInit condition) {
        QueryWrapper<ProductionMonthPlanInit> queryWrapper = new QueryWrapper<>();
        builderCondition(queryWrapper, condition);
        List<ProductionMonthPlanInit> list = productionMonthPlanInitMapper.selectList(queryWrapper);
        // 不排产原因
        dealList(list);
        return list;
    }

    /**
     * 解析不排产原因
     *
     * @param list
     */
    private void dealList(List<ProductionMonthPlanInit> list) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        Locale language = SecurityUtils.getUserLang();
        JsonUtils.parseJsonRemarkList(list, language.toString(), "noProductionReason");
    }

    /**
     * 构建查询条件
     *
     * @param queryWrapper 查询构建器
     * @param condition    查询条件值对象
     */
    protected void builderCondition(QueryWrapper<ProductionMonthPlanInit> queryWrapper, ProductionMonthPlanInit condition) {
        /**
         * 工厂、年份、月份、需求版本、排产版本、产品品类
         */
        queryWrapper.eq(PubUtil.isNotEmpty(condition.getFactoryCode()), "FACTORY_CODE", condition.getFactoryCode());
        queryWrapper.eq(PubUtil.isNotEmpty(condition.getYear()), "YEAR", condition.getYear());
        queryWrapper.eq(PubUtil.isNotEmpty(condition.getMonth()), "MONTH", condition.getMonth());
        queryWrapper.eq(PubUtil.isNotEmpty(condition.getMonthPlanVersion()), "MONTH_PLAN_VERSION", condition.getMonthPlanVersion());
        queryWrapper.eq(PubUtil.isNotEmpty(condition.getProductionVersion()), "PRODUCTION_VERSION", condition.getProductionVersion());
        queryWrapper.eq(PubUtil.isNotEmpty(condition.getProductTypeCode()), "PRODUCT_TYPE_CODE", condition.getProductTypeCode());
        /**
         * 物料相关
         */
        queryWrapper.like(PubUtil.isNotEmpty(condition.getMaterialCode()), "MATERIAL_CODE", condition.getMaterialCode());
        queryWrapper.like(PubUtil.isNotEmpty(condition.getMaterialDesc()), "MATERIAL_DESC", condition.getMaterialDesc());
        queryWrapper.eq(PubUtil.isNotEmpty(condition.getConstructionStage()), "CONSTRUCTION_STAGE", condition.getConstructionStage());
        queryWrapper.eq(PubUtil.isNotEmpty(condition.getLocationType()), "LOCATION_TYPE", condition.getLocationType());
        queryWrapper.eq(PubUtil.isNotEmpty(condition.getChannel()), "CHANNEL", condition.getChannel());
        queryWrapper.eq(PubUtil.isNotEmpty(condition.getBrand()), "BRAND", condition.getBrand());
        queryWrapper.eq(PubUtil.isNotEmpty(condition.getProSize()), "PRO_SIZE", condition.getProSize());
        queryWrapper.like(PubUtil.isNotEmpty(condition.getSpecifications()), "SPECIFICATIONS", condition.getSpecifications());
        queryWrapper.like(PubUtil.isNotEmpty(condition.getPattern()), "PATTERN", condition.getPattern());
        queryWrapper.like(PubUtil.isNotEmpty(condition.getHierarchy()), "HIERARCHY", condition.getHierarchy());
        queryWrapper.like(PubUtil.isNotEmpty(condition.getNoProductionReason()), "NO_PRODUCTION_REASON", condition.getNoProductionReason());
    }
}
