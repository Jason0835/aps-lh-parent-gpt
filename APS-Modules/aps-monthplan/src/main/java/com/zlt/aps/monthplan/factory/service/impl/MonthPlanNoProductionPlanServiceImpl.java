package com.zlt.aps.monthplan.factory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanNoProductionPlan;
import com.zlt.aps.monthplan.api.domain.vo.MonthPlanStatisticsVo;
import com.zlt.aps.monthplan.common.utils.JsonUtils;
import com.zlt.aps.monthplan.factory.mapper.MonthPlanNoProductionPlanMapper;
import com.zlt.aps.monthplan.factory.service.IMonthPlanNoProductionPlanService;
import com.zlt.common.utils.PubUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MonthPlanNoProductionPlanServiceImpl.java
 * 描    述：MonthPlanNoProductionPlanServiceImpl分厂月生产计划排产过程-未排产计划业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-21
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonthPlanNoProductionPlanServiceImpl implements IMonthPlanNoProductionPlanService {

    private final MonthPlanNoProductionPlanMapper monthPlanNoProductionPlanMapper;

    /**
     * 列表查询
     */
    @Override
    public List<MonthPlanNoProductionPlan> selectList(MonthPlanNoProductionPlan query) {
        QueryWrapper<MonthPlanNoProductionPlan> wrapper = new QueryWrapper<>();
        builderCondition(wrapper, query);
        List<MonthPlanNoProductionPlan> list = monthPlanNoProductionPlanMapper.selectList(wrapper);
        dealList(list);
        return list;
    }

    /**
     * 统计未排SAP总量
     */
    @Override
    public void statistics(MonthPlanStatisticsVo statisticsVo, MonthPlanNoProductionPlan query) {
        QueryWrapper<MonthPlanNoProductionPlan> wrapper = new QueryWrapper<>();
        builderCondition(wrapper, query);
        wrapper.select("sum(un_production_qty) as noProductionCount");

        List<Map<String, Object>> mapList = monthPlanNoProductionPlanMapper.selectMaps(wrapper);
        if (CollectionUtils.isNotEmpty(mapList)) {
            Map<String, Object> resultMap = mapList.get(0);
            if (resultMap != null && resultMap.get("noProductionCount") != null) {
                statisticsVo.setNoProductionCount(Long.parseLong(resultMap.get("noProductionCount").toString()));
            }
        }
    }

    /**
     * 处理语言包问题 将未排原因的json转换处理
     */
    private List<MonthPlanNoProductionPlan> dealList(List<MonthPlanNoProductionPlan> list) {
        if (CollectionUtils.isNotEmpty(list)) {
            //获取当前语言包
            Locale language = SecurityUtils.getUserLang();
            JsonUtils.parseJsonRemarkList(list, language.toString(), "reason");
            //模具、生胎代码 不再关联，采用生成时存储
            for (MonthPlanNoProductionPlan itemPlan : list) {
                Long factProdReqQty = itemPlan.getFactProdReqQty() == null ? 0L : itemPlan.getFactProdReqQty();
                Long unProductionQty = itemPlan.getUnProductionQty() == null ? 0L : itemPlan.getUnProductionQty();
                long totalQty = factProdReqQty - unProductionQty;
                itemPlan.setTotalQty(totalQty < 0 ? 0 : totalQty);
                itemPlan.setMouldNo(itemPlan.getMouldNoInfo());
                itemPlan.setEmbryoCode(itemPlan.getEmbryoCodeInfo());
            }
        }
        return list;
    }

    /**
     * 设置查询条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    protected void builderCondition(QueryWrapper<MonthPlanNoProductionPlan> queryWrapper, MonthPlanNoProductionPlan queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("year")), "YEAR", queryVO.getFieldValueByFieldName("year"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mouldMethod")), "MOULD_METHOD", queryVO.getFieldValueByFieldName("mouldMethod"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month")), "MONTH", queryVO.getFieldValueByFieldName("month"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("monthPlanVersion")), "MONTH_PLAN_VERSION", queryVO.getFieldValueByFieldName("monthPlanVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productionVersion")), "PRODUCTION_VERSION", queryVO.getFieldValueByFieldName("productionVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("planSeq")), "PLAN_SEQ", queryVO.getFieldValueByFieldName("planSeq"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("monthPlanId")), "MONTH_PLAN_ID", queryVO.getFieldValueByFieldName("monthPlanId"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productCode")), "PRODUCT_CODE", queryVO.getFieldValueByFieldName("productCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("constructionStage")), "CONSTRUCTION_STAGE", queryVO.getFieldValueByFieldName("constructionStage"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productDesc")), "PRODUCT_DESC", queryVO.getFieldValueByFieldName("productDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("locationType")), "LOCATION_TYPE", queryVO.getFieldValueByFieldName("locationType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("channel")), "CHANNEL", queryVO.getFieldValueByFieldName("channel"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("brand")), "BRAND", queryVO.getFieldValueByFieldName("brand"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specifications")), "SPECIFICATIONS", queryVO.getFieldValueByFieldName("specifications"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("pattern")), "PATTERN", queryVO.getFieldValueByFieldName("pattern"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("hierarchy")), "HIERARCHY", queryVO.getFieldValueByFieldName("hierarchy"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("proSize")), "PRO_SIZE", queryVO.getFieldValueByFieldName("proSize"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productTypeCode")), "PRODUCT_TYPE_CODE", queryVO.getFieldValueByFieldName("productTypeCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productTypeName")), "PRODUCT_TYPE_NAME", queryVO.getFieldValueByFieldName("productTypeName"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("prodReqPlan")), "PROD_REQ_PLAN", queryVO.getFieldValueByFieldName("prodReqPlan"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factProdReqQty")), "FACT_PROD_REQ_QTY", queryVO.getFieldValueByFieldName("factProdReqQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("levelCode")), "LEVEL_CODE", queryVO.getFieldValueByFieldName("levelCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("levelName")), "LEVEL_NAME", queryVO.getFieldValueByFieldName("levelName"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isContinue")), "IS_CONTINUE", queryVO.getFieldValueByFieldName("isContinue"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isImportantCustom")), "IS_IMPORTANT_CUSTOM", queryVO.getFieldValueByFieldName("isImportantCustom"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isEnsurePlan")), "IS_ENSURE_PLAN", queryVO.getFieldValueByFieldName("isEnsurePlan"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isEmergency")), "IS_EMERGENCY", queryVO.getFieldValueByFieldName("isEmergency"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isDebitPlan")), "IS_DEBIT_PLAN", queryVO.getFieldValueByFieldName("isDebitPlan"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isStockUp")), "IS_STOCK_UP", queryVO.getFieldValueByFieldName("isStockUp"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("deliveryDateDue")), "DELIVERY_DATE_DUE", queryVO.getFieldValueByFieldName("deliveryDateDue"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("profitGrade")), "PROFIT_GRADE", queryVO.getFieldValueByFieldName("profitGrade"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isImport")), "IS_IMPORT", queryVO.getFieldValueByFieldName("isImport"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("reason")), "REASON", queryVO.getFieldValueByFieldName("reason"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("unProductionQty")), "UN_PRODUCTION_QTY", queryVO.getFieldValueByFieldName("unProductionQty"));
        //模具、生胎
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mouldNo")), "MOULD_NO_INFO", queryVO.getFieldValueByFieldName("mouldNo"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("embryoCode")), "EMBRYO_CODE_INFO", queryVO.getFieldValueByFieldName("embryoCode"));
    }
}
