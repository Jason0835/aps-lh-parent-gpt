package com.zlt.aps.mp.factory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.baseVo.excelVo.CellStyle;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.utils.JsonI18nConvertUtils;
import com.zlt.aps.utils.JsonUtils;
import com.zlt.aps.mp.api.domain.entity.MonthPlanNoProductionPlan;
import com.zlt.aps.mp.api.domain.vo.MonthPlanStatisticsVo;
import com.zlt.aps.mp.factory.mapper.MonthPlanNoProductionPlanMapper;
import com.zlt.aps.mp.factory.service.IMonthPlanNoProductionPlanService;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import com.zlt.bill.common.service.AbstractDocService;
import com.ruoyi.common.exception.ServiceException;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MonthPlanNoProductionPlanServiceImpl.java
 * 描    述：MonthPlanNoProductionPlanServiceImplS2-0606.排产结果-未排产计划业务层处理
 *
 * @author yelq
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：yelq
 * 修改内容：...
 * @date 2026-01-21
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class MonthPlanNoProductionPlanServiceImpl extends AbstractDocService<MonthPlanNoProductionPlan> implements IMonthPlanNoProductionPlanService {
    private final MonthPlanNoProductionPlanMapper monthPlanNoProductionPlanMapper;
    private final ISysDictDataCacheService sysDictDataCacheService;

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
     * 导出未排产数据
     *
     * @param queryVO 查询条件
     * @return 导出的字节数组
     * @throws IOException IO异常
     */
    @Override
    public byte[] exportMonthPlanNoProductionPlan(MonthPlanNoProductionPlan queryVO) throws IOException {
        // 获取模板
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("excelModel/monthPlanNoProductionPlan.xlsx");
        if (inputStream == null) {
            throw new IOException("模板文件不存在");
        }

        // 加载字典数据
        // 工厂名称字典
        List<SysDictData> factoryDatas = sysDictDataCacheService.getType("biz_factory_name");
        Map<String, String> factoryMap = factoryDatas != null ? factoryDatas.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel)) : new HashMap<>();
        // 产品品类字典
        List<SysDictData> productTypeDatas = sysDictDataCacheService.getType("biz_product_type");
        Map<String, String> productTypeMap = productTypeDatas != null ? productTypeDatas.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel)) : new HashMap<>();
        // 排产分类字典
        List<SysDictData> productionTypeDatas = sysDictDataCacheService.getType("biz_schedule_type");
        Map<String, String> productionTypeMap = productionTypeDatas != null ? productionTypeDatas.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel)) : new HashMap<>();
        // 施工阶段字典
        List<SysDictData> constructionStageDatas = sysDictDataCacheService.getType("biz_construction_stage");
        Map<String, String> constructionStageMap = constructionStageDatas != null ? constructionStageDatas.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel)) : new HashMap<>();
        // 内外销字典
        List<SysDictData> locationTypeDatas = sysDictDataCacheService.getType("biz_stor_type");
        Map<String, String> locationTypeMap = locationTypeDatas != null ? locationTypeDatas.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel)) : new HashMap<>();
        // 品牌字典
        List<SysDictData> brandDatas = sysDictDataCacheService.getType("biz_brand_type");
        Map<String, String> brandMap = brandDatas != null ? brandDatas.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel)) : new HashMap<>();
        // 是否排产字典
        List<SysDictData> isProductionDatas = sysDictDataCacheService.getType("biz_yes_no");
        Map<String, String> isProductionMap = isProductionDatas != null ? isProductionDatas.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel)) : new HashMap<>();

        // 表头信息
        Map<String, Object> tableMap = new HashMap<>(16);
        // 列表数据
        List<List<Map<String, Object>>> excelDataList = new ArrayList<>();
        List<CellStyle> cellStyleList = new ArrayList<>();

        // 按当前年月取数
        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH) + 1;

        // 查询数据 - 按物料编码分组汇总，只查询有@Excel注解的字段
        QueryWrapper<MonthPlanNoProductionPlan> wrapper = new QueryWrapper<>();
        builderCondition(wrapper, queryVO);
        // 按物料编码分组，只选择有@Excel注解的字段（共29列）
        wrapper.select("FACTORY_CODE", "YEAR", "MONTH", "MONTH_PLAN_VERSION", "PRODUCTION_VERSION", "PRODUCT_TYPE_CODE", "MES_MATERIAL_CODE", "MATERIAL_CODE", "MATERIAL_DESC", "REASON", "UPDATE_TIME", "STRUCTURE_NAME", "PRO_SIZE", "PRODUCTION_TYPE", "CONSTRUCTION_STAGE", "MAIN_MATERIAL_DESC", "LOCATION_TYPE", "BRAND", "SPECIFICATIONS", "MAIN_PATTERN", "PATTERN", "IS_PRODUCTION", "sum(STOCK_QTY) as stockQty", "sum(AVERAGE_SALE_QTY) as averageSaleQty", "sum(NET_QTY) as netQty", "sum(POSTPONE_NET_QTY) as postponeNetQty", "sum(UN_POSTPONE_NET_QTY) as unPostponeNetQty", "sum(HEIGHT_QTY) as heightQty", "sum(MID_QTY) as midQty", "sum(POSTPONE_QTY) as postponeQty", "sum(CYCLE_RESERVE_QTY) as cycleReserveQty", "sum(CONVENTION_RESERVE_QTY) as conventionReserveQty", "sum(HEIGHT_LOSS_QTY) as heightLossQty", "sum(FACT_PROD_REQ_QTY) as factProdReqQty", "sum(UN_PRODUCTION_QTY) as unProductionQty");
        wrapper.groupBy("MATERIAL_CODE");
        List<MonthPlanNoProductionPlan> dataList = monthPlanNoProductionPlanMapper.selectList(wrapper);

        // 设置年月标题和工厂、产品品类（第一行）
        tableMap.put("yearAndMonth", currentYear + "年" + currentMonth + "月份");
        // 第一行的工厂和产品品类需要字典转义，从查询结果第一条获取
        if (dataList != null && !dataList.isEmpty()) {
            MonthPlanNoProductionPlan firstItem = dataList.get(0);
            tableMap.put("factoryCode", factoryMap.getOrDefault(firstItem.getFactoryCode(), firstItem.getFactoryCode() != null ? firstItem.getFactoryCode() : ""));
            tableMap.put("productTypeCode", productTypeMap.getOrDefault(firstItem.getProductTypeCode(), firstItem.getProductTypeCode() != null ? firstItem.getProductTypeCode() : ""));
            tableMap.put("monthPlanVersion", productTypeMap.getOrDefault(firstItem.getMonthPlanVersion(), firstItem.getMonthPlanVersion() != null ? firstItem.getMonthPlanVersion() : ""));
            tableMap.put("productionVersion", productTypeMap.getOrDefault(firstItem.getProductionVersion(), firstItem.getProductionVersion() != null ? firstItem.getProductionVersion() : ""));
        }

        if (dataList != null && !dataList.isEmpty()) {
            List<Map<String, Object>> list = new ArrayList<>();
            // 根据模板结构：第1行标题，第2行表头，第3行合计，第4行开始数据
            // Excel行号从0开始，所以数据从第3行开始
            int dataStartRowIndex = 3; // 对应Excel第4行（数据开始行）

            for (int i = 0; i < dataList.size(); i++) {
                Map<String, Object> listDataMap = new HashMap<>(16);
                MonthPlanNoProductionPlan item = dataList.get(i);

                // 转义字典值
                listDataMap.put("factoryCode", factoryMap.getOrDefault(item.getFactoryCode(), item.getFactoryCode() != null ? item.getFactoryCode() : ""));
                listDataMap.put("year", item.getYear() != null ? item.getYear() : "");
                listDataMap.put("month", item.getMonth() != null ? item.getMonth() : "");
                listDataMap.put("monthPlanVersion", item.getMonthPlanVersion() != null ? item.getMonthPlanVersion() : "");
                listDataMap.put("productionVersion", item.getProductionVersion() != null ? item.getProductionVersion() : "");
                listDataMap.put("productTypeCode", productTypeMap.getOrDefault(item.getProductTypeCode(), item.getProductTypeCode() != null ? item.getProductTypeCode() : ""));
                listDataMap.put("mesMaterialCode", item.getMesMaterialCode() != null ? item.getMesMaterialCode() : "");
                listDataMap.put("materialCode", item.getMaterialCode() != null ? item.getMaterialCode() : "");
                listDataMap.put("materialDesc", item.getMaterialDesc() != null ? item.getMaterialDesc() : "");
                listDataMap.put("structureName", item.getStructureName() != null ? item.getStructureName() : "");
                listDataMap.put("proSize", item.getProSize() != null ? item.getProSize() : "");
                listDataMap.put("productionType", productionTypeMap.getOrDefault(item.getProductionType(), item.getProductionType() != null ? item.getProductionType() : ""));
                listDataMap.put("constructionStage", constructionStageMap.getOrDefault(item.getConstructionStage(), item.getConstructionStage() != null ? item.getConstructionStage() : ""));
                listDataMap.put("mainMaterialDesc", item.getMainMaterialDesc() != null ? item.getMainMaterialDesc() : "");
                listDataMap.put("locationType", locationTypeMap.getOrDefault(item.getLocationType(), item.getLocationType() != null ? item.getLocationType() : ""));
                listDataMap.put("brand", brandMap.getOrDefault(item.getBrand(), item.getBrand() != null ? item.getBrand() : ""));
                listDataMap.put("specifications", item.getSpecifications() != null ? item.getSpecifications() : "");
                listDataMap.put("mainPattern", item.getMainPattern() != null ? item.getMainPattern() : "");
                listDataMap.put("pattern", item.getPattern() != null ? item.getPattern() : "");
                listDataMap.put("isProduction", isProductionMap.getOrDefault(item.getIsProduction(), item.getIsProduction() != null ? item.getIsProduction() : ""));
                listDataMap.put("stockQty", item.getStockQty() != null ? item.getStockQty() : 0);
                listDataMap.put("averageSaleQty", item.getAverageSaleQty() != null ? item.getAverageSaleQty() : 0);
                listDataMap.put("netQty", item.getNetQty() != null ? item.getNetQty() : 0);
                listDataMap.put("postponeNetQty", item.getPostponeNetQty() != null ? item.getPostponeNetQty() : 0);
                listDataMap.put("unPostponeNetQty", item.getUnPostponeNetQty() != null ? item.getUnPostponeNetQty() : 0);
                listDataMap.put("heightQty", item.getHeightQty() != null ? item.getHeightQty() : 0);
                listDataMap.put("midQty", item.getMidQty() != null ? item.getMidQty() : 0);
                listDataMap.put("postponeQty", item.getPostponeQty() != null ? item.getPostponeQty() : 0);
                listDataMap.put("cycleReserveQty", item.getCycleReserveQty() != null ? item.getCycleReserveQty() : 0);
                listDataMap.put("conventionReserveQty", item.getConventionReserveQty() != null ? item.getConventionReserveQty() : 0);
                listDataMap.put("heightLossQty", item.getHeightLossQty() != null ? item.getHeightLossQty() : 0);
                listDataMap.put("factProdReqQty", item.getFactProdReqQty() != null ? item.getFactProdReqQty() : 0);
                listDataMap.put("unProductionQty", item.getUnProductionQty() != null ? item.getUnProductionQty() : 0);
                // 实单未排产 = 高优先级 + 中优先级 - 实际生产量
                long heightQty = item.getHeightQty() != null ? item.getHeightQty() : 0;
                long midQty = item.getMidQty() != null ? item.getMidQty() : 0;
                long factProdReqQty = item.getFactProdReqQty() != null ? item.getFactProdReqQty() : 0;
                long actualOrderUnproduced = heightQty + midQty - factProdReqQty;
                listDataMap.put("actualOrderUnproduced", actualOrderUnproduced > 0 ? actualOrderUnproduced : 0);
                // 更新日期
                if (item.getUpdateTime() != null) {
                    listDataMap.put("updateTime", DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, item.getUpdateTime()));
                } else {
                    listDataMap.put("updateTime", "");
                }
                // 未排产原因 - 解析国际化json数据
                String reason = item.getReason();
                if (StringUtils.isNotBlank(reason)) {
                    Locale locale = I18nUtil.getLocaleFromRedis();
                    if (reason.contains("|")) {
                        String[] split = reason.split("\\|");
                        List<String> reasonList = new ArrayList<>(split.length);
                        for (String reasonI18n : split) {
                            String convertValue = JsonI18nConvertUtils.getConvertValue(reasonI18n, locale);
                            reasonList.add(convertValue);
                        }
                        listDataMap.put("reason", String.join(",", reasonList));
                    } else {
                        String convertValue = JsonI18nConvertUtils.getConvertValue(reason, locale);
                        listDataMap.put("reason", convertValue);
                    }
                } else {
                    listDataMap.put("reason", "");
                }

                // 计算行号（Excel行号从0开始）
                int rowNum = dataStartRowIndex + i;

                // 添加样式（确保样式作用在实际的数据行上）
                cellStyleList.add(new CellStyle(rowNum, rowNum, 0, 28, "#FFFFFF", true, false, "等线"));

                list.add(listDataMap);
            }
            excelDataList.add(list);
        }

        // 将单元格样式放入context
        if (PubUtil.isNotEmpty(cellStyleList)) {
            tableMap.put("CELL_STYLE", cellStyleList);
        }

        // 写到文件
        return ExcelUtils.writeMultiList(inputStream, 0, tableMap, excelDataList);
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
