package com.zlt.aps.mp.factory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.maindata.mapper.MdmCycleSchStruConfEntityMapper;
import com.zlt.aps.maindata.mapper.MdmMaterialInfoEntityMapper;
import com.zlt.aps.maindata.utils.ScmListUtils;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanMouldDayResult;
import com.zlt.aps.mp.api.domain.entity.MdmCycleSchStruConf;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mp.api.domain.entity.MonthPlanNoProductionPlan;
import com.zlt.aps.mp.api.domain.vo.MonthPlanStatisticsVo;
import com.zlt.aps.mp.engine.mapper.FactoryMouldingDayResultMapper;
import com.zlt.aps.mp.factory.mapper.MonthPlanNoProductionPlanMapper;
import com.zlt.aps.mp.factory.service.IMonthPlanNoProductionPlanService;
import com.zlt.aps.utils.JsonI18nConvertUtils;
import com.zlt.aps.utils.JsonUtils;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

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
    private final ISysDictDataCacheService sysDictDataCacheService;

    private final MonthPlanNoProductionPlanMapper monthPlanNoProductionPlanMapper;
    private final MdmCycleSchStruConfEntityMapper mdmCycleSchStruConfEntityMapper;
    private final FactoryMouldingDayResultMapper factoryMouldingDayResultMapper;
    private final MdmMaterialInfoEntityMapper mdmMaterialInfoEntityMapper;
    /**
     * 结构类型
     */
    private final static String STRUCTURE_TYPE_CYCL = "01"; // 周期
    private final static String STRUCTURE_TYPE_COMMON = "02"; // 常规
    /**
     * 内外销类型
     */
    private final static String LOCATION_TYPE_OUT = "2"; // 外销

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
    /**
     * 列表查询
     */
    @Override
    public List<MonthPlanNoProductionPlan> selectList(MonthPlanNoProductionPlan query) {
        QueryWrapper<MonthPlanNoProductionPlan> wrapper = new QueryWrapper<>();
        builderCondition(wrapper, query);

        wrapper.select("FACTORY_CODE", "YEAR", "MONTH", "MONTH_PLAN_VERSION", "PRODUCTION_VERSION", "PRODUCT_TYPE_CODE",
                "MES_MATERIAL_CODE", "MATERIAL_CODE", "MATERIAL_DESC", "STRUCTURE_NAME", "PRO_SIZE", "PRODUCTION_TYPE",
                "CONSTRUCTION_STAGE", "MAIN_MATERIAL_DESC", "LOCATION_TYPE", "BRAND", "SPECIFICATIONS", "MAIN_PATTERN",
                "PATTERN", "max(STOCK_QTY) as stockQty", "max(AVERAGE_SALE_QTY) as averageSaleQty",
                "sum(NET_QTY) as netQty", "sum(POSTPONE_NET_QTY) as postponeNetQty",
                "sum(UN_POSTPONE_NET_QTY) as unPostponeNetQty", "sum(HEIGHT_QTY) as heightQty",
                "sum(MID_QTY) as midQty", "sum(POSTPONE_QTY) as postponeQty",
                "sum(CYCLE_RESERVE_QTY) as cycleReserveQty", "sum(CONVENTION_RESERVE_QTY) as conventionReserveQty",
                "sum(HEIGHT_LOSS_QTY) as heightLossQty", "sum(FACT_PROD_REQ_QTY) as factProdReqQty",
                "sum(UN_PRODUCTION_QTY) as unProductionQty", "GROUP_CONCAT(distinct reason SEPARATOR '|') as reason",
                "max(UPDATE_TIME) as UPDATE_TIME");
        wrapper.groupBy("FACTORY_CODE", "YEAR", "MONTH", "MONTH_PLAN_VERSION", "PRODUCTION_VERSION", "PRODUCT_TYPE_CODE",
                "MES_MATERIAL_CODE", "MATERIAL_CODE", "MATERIAL_DESC", "STRUCTURE_NAME", "PRO_SIZE", "PRODUCTION_TYPE",
                "CONSTRUCTION_STAGE", "MAIN_MATERIAL_DESC", "LOCATION_TYPE", "BRAND", "SPECIFICATIONS", "MAIN_PATTERN",
                "PATTERN");
        wrapper.orderBy(true, true, "PRO_SIZE", "SPECIFICATIONS", "STRUCTURE_NAME", "MAIN_PATTERN", "PATTERN", "MAIN_MATERIAL_DESC");
        wrapper.orderBy(true, false, "UPDATE_TIME");

        List<MonthPlanNoProductionPlan> list = monthPlanNoProductionPlanMapper.selectList(wrapper);

        if (CollectionUtils.isNotEmpty(list) && StringUtils.isNotBlank(query.getProductionVersion())) {
            LambdaQueryWrapper<FactoryMonthPlanMouldDayResult> resultQueryWrapper = new LambdaQueryWrapper<>();
            resultQueryWrapper.eq(FactoryMonthPlanMouldDayResult::getProductionVersion, query.getProductionVersion());
            Map<String, Integer> mouldingDayResultMap = factoryMouldingDayResultMapper
                    .selectList(resultQueryWrapper).stream().collect(Collectors
                            .toMap(FactoryMonthPlanMouldDayResult::getMaterialCode, FactoryMonthPlanMouldDayResult::getTotalQty, (r1, r2) -> r1));

            for (MonthPlanNoProductionPlan item : list) {
                int factProdQty = mouldingDayResultMap.getOrDefault(item.getMaterialCode(), 0);
                item.setTotalQty((long) factProdQty);
                long heightQty = item.getHeightQty() != null ? item.getHeightQty() : 0L;
                long midQty = item.getMidQty() != null ? item.getMidQty() : 0L;
                long actualOrderUnproduced = heightQty + midQty - factProdQty;
                item.setActualOrderUnproduced(actualOrderUnproduced >= 0 ? actualOrderUnproduced : 0L);
            }
        }

        if (CollectionUtils.isNotEmpty(list)) {
            Locale language = SecurityUtils.getUserLang();
            for (MonthPlanNoProductionPlan item : list) {
                String reason = item.getReason();
                if (StringUtils.isNotBlank(reason)) {
                    if (reason.contains("|")) {
                        String[] split = reason.split("\\|");
                        List<String> reasonList = new ArrayList<>(split.length);
                        for (String reasonI18n : split) {
                            String convertValue = JsonI18nConvertUtils.getConvertValue(reasonI18n, language);
                            reasonList.add(convertValue);
                        }
                        item.setReason(reasonList.stream().distinct().collect(Collectors.joining(",")));
                    } else {
                        String convertValue = JsonI18nConvertUtils.getConvertValue(reason, language);
                        item.setReason(convertValue);
                    }
                }

                Long factProdReqQty = item.getFactProdReqQty() == null ? 0L : item.getFactProdReqQty();
                Long unProductionQty = item.getUnProductionQty() == null ? 0L : item.getUnProductionQty();
                long totalQty = factProdReqQty - unProductionQty;
                item.setTotalQty(totalQty < 0 ? 0 : totalQty);
            }
        }
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
        // 产品分类字典
        List<SysDictData> productCategoryDatas = sysDictDataCacheService.getType("product_category");
        Map<String, String> productCategoryMap = productCategoryDatas.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel));
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
        // 结构类型
        List<SysDictData> structureTypeDatas = sysDictDataCacheService.getType("structure_type");
        Map<String, String> structureTypeDictMap = structureTypeDatas != null ? structureTypeDatas.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel)) : new HashMap<>();


        // 表头信息
        Map<String, Object> tableMap = new HashMap<>(16);
        // 列表数据
        List<List<Map<String, Object>>> excelDataList = new ArrayList<>();

        // 按查询条件中的年月取数，如果没有则使用当前年月
        Integer currentYear = queryVO.getYear();
        Integer currentMonth = queryVO.getMonth();
        if (currentYear == null || currentMonth == null) {
            Calendar calendar = Calendar.getInstance();
            currentYear = calendar.get(Calendar.YEAR);
            currentMonth = calendar.get(Calendar.MONTH) + 1;
        }

        // 查询数据 - 按物料编码分组汇总，只查询有@Excel注解的字段
        QueryWrapper<MonthPlanNoProductionPlan> wrapper = new QueryWrapper<>();
        builderCondition(wrapper, queryVO);
        // 按物料编码分组，只选择有@Excel注解的字段（共29列）
        wrapper.select("FACTORY_CODE", "YEAR", "MONTH", "MONTH_PLAN_VERSION", "PRODUCTION_VERSION", "PRODUCT_TYPE_CODE",
                "MES_MATERIAL_CODE", "MATERIAL_CODE", "MATERIAL_DESC", "STRUCTURE_NAME", "PRO_SIZE", "PRODUCTION_TYPE",
                "CONSTRUCTION_STAGE", "MAIN_MATERIAL_DESC", "LOCATION_TYPE", "BRAND", "SPECIFICATIONS", "MAIN_PATTERN",
                "PATTERN", "max(STOCK_QTY) as stockQty", "max(AVERAGE_SALE_QTY) as averageSaleQty",
                "sum(NET_QTY) as netQty", "sum(POSTPONE_NET_QTY) as postponeNetQty",
                "sum(UN_POSTPONE_NET_QTY) as unPostponeNetQty", "sum(HEIGHT_QTY) as heightQty",
                "sum(MID_QTY) as midQty", "sum(POSTPONE_QTY) as postponeQty",
                "sum(CYCLE_RESERVE_QTY) as cycleReserveQty", "sum(CONVENTION_RESERVE_QTY) as conventionReserveQty",
                "sum(HEIGHT_LOSS_QTY) as heightLossQty", "sum(FACT_PROD_REQ_QTY) as factProdReqQty",
                "sum(UN_PRODUCTION_QTY) as unProductionQty", "GROUP_CONCAT(distinct reason SEPARATOR '|') as reason",
                "max(UPDATE_TIME) as UPDATE_TIME");
        wrapper.groupBy("FACTORY_CODE", "YEAR", "MONTH", "MONTH_PLAN_VERSION", "PRODUCTION_VERSION", "PRODUCT_TYPE_CODE",
                "MES_MATERIAL_CODE", "MATERIAL_CODE", "MATERIAL_DESC", "STRUCTURE_NAME", "PRO_SIZE", "PRODUCTION_TYPE",
                "CONSTRUCTION_STAGE", "MAIN_MATERIAL_DESC", "LOCATION_TYPE", "BRAND", "SPECIFICATIONS", "MAIN_PATTERN",
                "PATTERN");
        wrapper.orderBy(true, true, "PRO_SIZE", "SPECIFICATIONS", "STRUCTURE_NAME", "MAIN_PATTERN", "PATTERN", "MAIN_MATERIAL_DESC");
        List<MonthPlanNoProductionPlan> dataList = monthPlanNoProductionPlanMapper.selectList(wrapper);

        // 加载月计划排产明细
        LambdaQueryWrapper<FactoryMonthPlanMouldDayResult> resultQueryWrapper = new LambdaQueryWrapper<>();
        resultQueryWrapper.eq(FactoryMonthPlanMouldDayResult::getProductionVersion, queryVO.getProductionVersion());
        Map<String, Integer> mouldingDayResultMap = factoryMouldingDayResultMapper
                .selectList(resultQueryWrapper).stream().collect(Collectors
                        .toMap(FactoryMonthPlanMouldDayResult::getMaterialCode, FactoryMonthPlanMouldDayResult::getTotalQty, (r1, r2) -> r1)); // 按模具编号对排产结果分组

        // 查询结构类型：通过结构名称关联T_DP_CYCLE_STRUCT_CONFIG表
        // 存在这个表就是周期性结构，否则都是常规结构
        Map<String, String> structureTypeMap = new HashMap<>();
        Map<String, String> materialProductCategoryMap = new HashMap<>();
        if (dataList != null && !dataList.isEmpty()) {
            // 获取所有不同的结构名称
            List<String> structureNames = dataList.stream()
                    .map(MonthPlanNoProductionPlan::getStructureName)
                    .distinct()
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toList());

            // 批量查询T_DP_CYCLE_STRUCT_CONFIG表
            if (!structureNames.isEmpty()) {
                QueryWrapper<MdmCycleSchStruConf> structWrapper = new QueryWrapper<>();
                structWrapper.in("STRUCTURE_NAME", structureNames);
                List<MdmCycleSchStruConf> structList = mdmCycleSchStruConfEntityMapper.selectList(structWrapper);

                // 构建结构类型Map：存在配置表的就是周期性结构，否则是常规结构
                Set<String> cycleStructNames = structList.stream()
                        .map(MdmCycleSchStruConf::getStructureName)
                        .collect(Collectors.toSet());

                for (String structureName : structureNames) {
                    structureTypeMap.put(structureName, cycleStructNames.contains(structureName) ? structureTypeDictMap.get(STRUCTURE_TYPE_CYCL) : structureTypeDictMap.get(STRUCTURE_TYPE_COMMON));
                }
            }

            // 加载物料表获取产品分类
            List<String> materialCodeList = dataList.stream()
                    .map(MonthPlanNoProductionPlan::getMaterialCode)
                    .filter(StringUtils::isNotBlank)
                    .distinct()
                    .collect(Collectors.toList());
            List<List<String>> materialCodeSplitList = ScmListUtils.getSplitList(materialCodeList, 1000); // 按每1000行查询，防止超出限制
            for (List<String> splitList : materialCodeSplitList) {
                QueryWrapper<MdmMaterialInfo> materialInfoWrapper = new QueryWrapper<>();
                materialInfoWrapper.in("MATERIAL_CODE", splitList);
                materialInfoWrapper.isNotNull("PRODUCT_CATEGORY");
                materialProductCategoryMap.putAll(mdmMaterialInfoEntityMapper.selectList(materialInfoWrapper).stream()
                        .collect(Collectors.toMap(MdmMaterialInfo::getMaterialCode, MdmMaterialInfo::getProductCategory,
                                (m1, m2) -> m1)));
            }
        }

        // 设置年月标题和工厂、产品品类（第一行）
        // 从查询结果中获取年月，如果没有数据则使用查询条件中的年月
        if (dataList != null && !dataList.isEmpty()) {
            MonthPlanNoProductionPlan firstItem = dataList.get(0);
            Integer dataYear = firstItem.getYear();
            Integer dataMonth = firstItem.getMonth();
            if (dataYear != null && dataMonth != null) {
                tableMap.put("yearAndMonth", dataYear + "年" + dataMonth + "月份");
            } else {
                tableMap.put("yearAndMonth", currentYear + "年" + currentMonth + "月份");
            }
            tableMap.put("factoryCode", factoryMap.getOrDefault(firstItem.getFactoryCode(), firstItem.getFactoryCode() != null ? firstItem.getFactoryCode() : ""));
            tableMap.put("productTypeCode", productTypeMap.getOrDefault(firstItem.getProductTypeCode(), firstItem.getProductTypeCode() != null ? firstItem.getProductTypeCode() : ""));
            tableMap.put("monthPlanVersion", firstItem.getMonthPlanVersion());
            tableMap.put("productionVersion", firstItem.getProductionVersion());
        } else {
            tableMap.put("yearAndMonth", currentYear + "年" + currentMonth + "月份");
        }

        if (dataList != null && !dataList.isEmpty()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (int i = 0; i < dataList.size(); i++) {
                Map<String, Object> listDataMap = new HashMap<>(16);
                MonthPlanNoProductionPlan item = dataList.get(i);

                // 转义字典值
                listDataMap.put("factoryCode", factoryMap.getOrDefault(item.getFactoryCode(), item.getFactoryCode() != null ? item.getFactoryCode() : ""));
                listDataMap.put("productionVersion", item.getProductionVersion() != null ? item.getProductionVersion() : "");
                listDataMap.put("productCategory", productCategoryMap.getOrDefault(materialProductCategoryMap.get(item.getMaterialCode()), ""));
                listDataMap.put("materialCode", item.getMaterialCode() != null ? item.getMaterialCode() : "");
                listDataMap.put("materialDesc", item.getMaterialDesc() != null ? item.getMaterialDesc() : "");
                listDataMap.put("structureName", item.getStructureName() != null ? item.getStructureName() : "");
                listDataMap.put("structureType", structureTypeMap.getOrDefault(item.getStructureName(), ""));
                listDataMap.put("proSize", item.getProSize() != null ? item.getProSize() : "");
                listDataMap.put("productionType", productionTypeMap.getOrDefault(item.getProductionType(), item.getProductionType() != null ? item.getProductionType() : ""));
                listDataMap.put("constructionStage", constructionStageMap.getOrDefault(item.getConstructionStage(), item.getConstructionStage() != null ? item.getConstructionStage() : ""));
                listDataMap.put("mainMaterialDesc", item.getMainMaterialDesc() != null ? item.getMainMaterialDesc() : "");
                listDataMap.put("locationType", locationTypeMap.getOrDefault(LOCATION_TYPE_OUT, "")); // TODO 与数据字典不一致，暂时固定取外销
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

                // 实际排产从 t_mp_moulding_day_result 表获取 totalQty
                int totalQty = mouldingDayResultMap.getOrDefault(item.getMaterialCode(), 0);
                listDataMap.put("totalQty", totalQty);

                // 实单未排产 = 高优先级 + 中优先级 - 实际排产，如果为负数则设为0
                long heightQty = item.getHeightQty() != null ? item.getHeightQty() : 0;
                long midQty = item.getMidQty() != null ? item.getMidQty() : 0;
                long actualOrderUnproduced = heightQty + midQty - totalQty;
                listDataMap.put("actualOrderUnproduced", actualOrderUnproduced >= 0 ? actualOrderUnproduced : 0);
                // 更新日期
                if (item.getUpdateTime() != null) {
                    listDataMap.put("updateTime", DateUtils.parseDateToStr("yyyy/MM/dd HH:mm", item.getUpdateTime()));
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
                        listDataMap.put("reason", reasonList.stream().distinct().collect(Collectors.joining(",")));
                    } else {
                        String convertValue = JsonI18nConvertUtils.getConvertValue(reason, locale);
                        listDataMap.put("reason", convertValue);
                    }
                } else {
                    listDataMap.put("reason", "");
                }

                list.add(listDataMap);
            }
            excelDataList.add(list);
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
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialCode")), "MATERIAL_CODE", queryVO.getFieldValueByFieldName("materialCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialDesc")), "MATERIAL_DESC", queryVO.getFieldValueByFieldName("materialDesc"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specifications")), "SPECIFICATIONS", queryVO.getFieldValueByFieldName("specifications"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("pattern")), "PATTERN", queryVO.getFieldValueByFieldName("pattern"));
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
