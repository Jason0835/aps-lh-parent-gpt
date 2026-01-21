package com.zlt.aps.monthplan.factory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.utils.JsonUtils;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanNoProductionPlan;
import com.zlt.aps.monthplan.api.domain.vo.MonthPlanStatisticsVo;
import com.zlt.aps.monthplan.factory.mapper.MonthPlanNoProductionPlanMapper;
import com.zlt.aps.monthplan.factory.service.IMonthPlanNoProductionPlanService;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.transaction.annotation.Transactional;

import com.zlt.bill.common.service.AbstractDocService;
import com.ruoyi.common.exception.ServiceException;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MonthPlanNoProductionPlanServiceImpl.java
 * 描    述：MonthPlanNoProductionPlanServiceImplS2-0606.排产结果-未排产计划业务层处理
 *@author yelq
 *@date 2026-01-21
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
public class MonthPlanNoProductionPlanServiceImpl extends AbstractDocService<MonthPlanNoProductionPlan>  implements IMonthPlanNoProductionPlanService {
    private final MonthPlanNoProductionPlanMapper monthPlanNoProductionPlanMapper;
    @Override
    protected String getDocTypeCode() {
        return "2026012110";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("2026012110");
        return sysDocType;
    }

    @Override
    public String checkUnique(MonthPlanNoProductionPlan docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.monthPlanNoProductionPlan.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }

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

            }
        }
        return list;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    private void builderCondition(QueryWrapper<MonthPlanNoProductionPlan> queryWrapper, MonthPlanNoProductionPlan queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("year")), "YEAR", queryVO.getFieldValueByFieldName("year"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month")), "MONTH", queryVO.getFieldValueByFieldName("month"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("monthPlanVersion")), "MONTH_PLAN_VERSION", queryVO.getFieldValueByFieldName("monthPlanVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productionVersion")), "PRODUCTION_VERSION", queryVO.getFieldValueByFieldName("productionVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("monthPlanId")), "MONTH_PLAN_ID", queryVO.getFieldValueByFieldName("monthPlanId"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productTypeCode")), "PRODUCT_TYPE_CODE", queryVO.getFieldValueByFieldName("productTypeCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("locationType")), "LOCATION_TYPE", queryVO.getFieldValueByFieldName("locationType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("brand")), "BRAND", queryVO.getFieldValueByFieldName("brand"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isPrioritize")), "IS_PRIORITIZE", queryVO.getFieldValueByFieldName("isPrioritize"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("structureName")), "STRUCTURE_NAME", queryVO.getFieldValueByFieldName("structureName"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mainMaterialDesc")), "MAIN_MATERIAL_DESC", queryVO.getFieldValueByFieldName("mainMaterialDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mesMaterialCode")), "MES_MATERIAL_CODE", queryVO.getFieldValueByFieldName("mesMaterialCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialCode")), "MATERIAL_CODE", queryVO.getFieldValueByFieldName("materialCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialDesc")), "MATERIAL_DESC", queryVO.getFieldValueByFieldName("materialDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specifications")), "SPECIFICATIONS", queryVO.getFieldValueByFieldName("specifications"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("pattern")), "PATTERN", queryVO.getFieldValueByFieldName("pattern"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productionType")), "PRODUCTION_TYPE", queryVO.getFieldValueByFieldName("productionType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("constructionStage")), "CONSTRUCTION_STAGE", queryVO.getFieldValueByFieldName("constructionStage"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("yearWeek")), "YEAR_WEEK", queryVO.getFieldValueByFieldName("yearWeek"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isDynamicBalance")), "IS_DYNAMIC_BALANCE", queryVO.getFieldValueByFieldName("isDynamicBalance"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isTrialPlan")), "IS_TRIAL_PLAN", queryVO.getFieldValueByFieldName("isTrialPlan"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isUniformity")), "IS_UNIFORMITY", queryVO.getFieldValueByFieldName("isUniformity"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isProduction")), "IS_PRODUCTION", queryVO.getFieldValueByFieldName("isProduction"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("orderQty")), "ORDER_QTY", queryVO.getFieldValueByFieldName("orderQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("stockQty")), "STOCK_QTY", queryVO.getFieldValueByFieldName("stockQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("averageSaleQty")), "AVERAGE_SALE_QTY", queryVO.getFieldValueByFieldName("averageSaleQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("plannedSurplus")), "PLANNED_SURPLUS", queryVO.getFieldValueByFieldName("plannedSurplus"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("netQty")), "NET_QTY", queryVO.getFieldValueByFieldName("netQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("postponeNetQty")), "POSTPONE_NET_QTY", queryVO.getFieldValueByFieldName("postponeNetQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("unPostponeNetQty")), "UN_POSTPONE_NET_QTY", queryVO.getFieldValueByFieldName("unPostponeNetQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("heightQty")), "HEIGHT_QTY", queryVO.getFieldValueByFieldName("heightQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("midQty")), "MID_QTY", queryVO.getFieldValueByFieldName("midQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("postponeQty")), "POSTPONE_QTY", queryVO.getFieldValueByFieldName("postponeQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("cycleReserveQty")), "CYCLE_RESERVE_QTY", queryVO.getFieldValueByFieldName("cycleReserveQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("conventionReserveQty")), "CONVENTION_RESERVE_QTY", queryVO.getFieldValueByFieldName("conventionReserveQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("heightLossQty")), "HEIGHT_LOSS_QTY", queryVO.getFieldValueByFieldName("heightLossQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factProdReqQty")), "FACT_PROD_REQ_QTY", queryVO.getFieldValueByFieldName("factProdReqQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("unProductionQty")), "UN_PRODUCTION_QTY", queryVO.getFieldValueByFieldName("unProductionQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("reason")), "REASON", queryVO.getFieldValueByFieldName("reason"));
    }
}
