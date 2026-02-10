package com.zlt.aps.monthplan.factory.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.tlt.aps.constant.Constant;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.constant.IncrementConstant;
import com.tlt.aps.enums.ConstructionStageEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.utils.GenerageMapKeyUtils;
import com.tlt.aps.utils.IncrementService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.AjaxResultUtils;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.maindata.enums.ReleaseStatusEnum;
import com.zlt.aps.maindata.mapper.MdmMaterialInfoEntityMapper;
import com.zlt.aps.maindata.mapper.MdmModelInfoEntityMapper;
import com.zlt.aps.maindata.mapper.MdmProductConstructionEntityMapper;
import com.zlt.aps.maindata.mapper.MdmProductModelRelationEntityMapper;
import com.zlt.aps.maindata.service.IFactoryParamService;
import com.zlt.aps.maindata.utils.LambdaWrapperBuilder;
import com.zlt.aps.monthplan.api.domain.dto.FactoryMonthPlanProdFinalQueryDto;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.aps.monthplan.api.domain.vo.*;
import com.zlt.aps.monthplan.demand.mapper.SaleMonthPlanRequireStockMapper;
import com.zlt.aps.monthplan.demand.service.IOrderPlanAllocationService;
import com.zlt.aps.monthplan.factory.mapper.FactoryMonthPlanProdFinalMapper;
import com.zlt.aps.monthplan.factory.mapper.FactoryMonthPlanProductionFinalResultEntityMapper;
import com.zlt.aps.monthplan.factory.mapper.MpFactoryProductionVersionMapper;
import com.zlt.aps.monthplan.factory.service.IFactoryMonthPlanProdFinalService;
import com.zlt.aps.monthplan.factory.service.IFactoryProductionVersionService;
import com.zlt.aps.monthplan.factory.service.IMonthPlanNoProductionPlanService;
import com.zlt.aps.monthplan.factory.service.IMonthPlanSurplusService;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zlt.common.utils.ImportExcelValidatedUtils.addImportErrorLog;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FactoryMonthPlanProdFinalServiceImpl.java
 * 描    述：FactoryMonthPlanProdFinalServiceImpl分厂月生产计划排产结果-生产计划排产结果业务层处理
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-14
 */
@Slf4j
@Service
@Deprecated
@RequiredArgsConstructor
public class FactoryMonthPlanProdFinalServiceImpl implements IFactoryMonthPlanProdFinalService {

    private final BaseDao baseDao;

    private final MdmMaterialInfoEntityMapper productInfoEntityMapper;

    private final MpFactoryProductionVersionMapper factoryProductionVersionMapper;

    private final MdmProductConstructionEntityMapper mdmProductConstructionMapper;

    private final MdmModelInfoEntityMapper modelInfoEntityMapper;

    private final MdmProductModelRelationEntityMapper productModelRelationEntityMapper;

    private final FactoryMonthPlanProdFinalMapper factoryMonthPlanProdFinalMapper;

    private final IncrementService incrementService;

    private final IFactoryParamService factoryParamService;

    private final IFactoryProductionVersionService factoryProductionVersionService;

    private final IMonthPlanNoProductionPlanService iMonthPlanNoProductionPlanService;

    private final IOrderPlanAllocationService iOrderPlanAllocationService;

    private final IMonthPlanSurplusService monthPlanSurplusService;

    private final SaleMonthPlanRequireStockMapper saleMonthPlanRequireStockMapper;

//  private final IFactoryMonthPlanProductionFinalService factoryMonthPlanProductionFinalService;

    private final String ERROR_REPEAT = "repeat";

    private final String ERROR_PRODUCT_CODE = "productCodeNotExist";

    private final String ERROR_FACTORY_REQ_QTY = "factProdReqQtyMax";

    private final String ERROR_TOTAL_QTY = "totalQtyCheck";

    private final String ERROR_SAME_EMPTY = "sameEmpty";

    private final String ERROR_MAX_DAY = "monthMaxDay";

    private final String ERROR_END_DAY = "endDayCheck";

    private final String ERROR_EDN_MAX = "endDayMaxCheck";

    @Override
    public List<FactoryMonthPlanProdFinal> getList(Wrapper<FactoryMonthPlanProdFinal> queryWrapper) {
        return getList(queryWrapper, false);
    }

    @Override
    public List<FactoryMonthPlanProdFinal> getList(Wrapper<FactoryMonthPlanProdFinal> queryWrapper, boolean isHandler) {
        if (!isHandler) {
            return factoryMonthPlanProdFinalMapper.selectList(queryWrapper);
        }
        List<FactoryMonthPlanProdFinal> resultData = factoryMonthPlanProdFinalMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(resultData)) {
            return resultData;
        }
        String productionVersion = resultData.get(0).getProductionVersion();
        if (StringUtils.isBlank(productionVersion)) {
            return resultData;
        }
        MpFactoryProductionVersion version = factoryProductionVersionService.getProductionVersion(productionVersion);
//    ProductionPlanExcelUtils.handlerBeginAndEndDay(version, resultData);
        return resultData;
    }

    @Override
    public List<FactoryMonthPlanDayProductionInfoVo> getMonthPlanDayProductionInfo(FactoryMonthPlanProdFinalQueryDto queryCondition) {
        Date productionDate = queryCondition.getProductionDate();
        //根据分厂，及日期确定排产版本计划
        MpFactoryProductionVersion finalVersion = factoryProductionVersionService.getFinalVersion(queryCondition.getFactoryCode(), productionDate);
        if (null == finalVersion) {
            return Collections.emptyList();
        }
        queryCondition.setYear(finalVersion.getYear());
        queryCondition.setMonth(finalVersion.getMonth());
        queryCondition.setMonthPlanVersion(finalVersion.getMonthPlanVersion());
        queryCondition.setProductionVersion(finalVersion.getProductionVersion());
        QueryWrapper<FactoryMonthPlanProdFinal> queryWrapper = new QueryWrapper();
        builderCondition(queryWrapper, queryCondition);
        List<FactoryMonthPlanProdFinal> dataList = getList(queryWrapper);
        if (CollectionUtils.isEmpty(dataList)) {
            return Collections.emptyList();
        }
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDate productionDay = productionDate.toInstant().atZone(zoneId).toLocalDate();
        LocalDate startDay = finalVersion.getProductionStartDate().toInstant().atZone(zoneId).toLocalDate();
        Long diffDays = Math.abs(ChronoUnit.DAYS.between(productionDay, startDay));
        Long day = diffDays + 1;
        List<FactoryMonthPlanDayProductionInfoVo> resultList = new ArrayList<>();
        String fieldName = String.format("day%s", day);
        dataList.stream().forEach(productData -> {
            Object value = productData.getFieldValueByFieldName(fieldName);
            if (null == value) {
                return;
            }
            Long productionQty = (Long) value;
            if (productionQty < BigDecimal.ONE.longValue()) {
                return;
            }
            FactoryMonthPlanDayProductionInfoVo productionInfo = new FactoryMonthPlanDayProductionInfoVo();
            BeanUtils.copyProperties(productData, productionInfo);
            productionInfo.setDayQty(productionQty);
            resultList.add(productionInfo);
        });
        return resultList;
    }

    @Override
    public FactoryMonthPlanFinalVersionInfoVo getFinalVersionInfo(String factoryCode, Integer year, Integer month) {
        if (StringUtils.isBlank(factoryCode) || null == year || null == month) {
            return null;
        }
        QueryWrapper<MpFactoryProductionVersion> queryVersion = new QueryWrapper<>();
        queryVersion.eq("FACTORY_CODE", factoryCode);
        queryVersion.eq("YEAR", year);
        queryVersion.eq("MONTH", month);
        queryVersion.eq("IS_FINAL", YesOrNoEnum.YES.getCode());
        queryVersion.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        MpFactoryProductionVersion result = factoryProductionVersionMapper.selectOne(queryVersion);
        if (null == result) {
            return null;
        }
        FactoryMonthPlanFinalVersionInfoVo info = new FactoryMonthPlanFinalVersionInfoVo();
        BeanUtils.copyProperties(result, info);
        return info;
    }

    @Override
    public FactoryMonthPlanFinalVersionInfoVo getFinalVersionInfoByDate(String factoryCode, Date date) {
        //根据分厂，及日期确定排产版本计划
        MpFactoryProductionVersion finalVersion = factoryProductionVersionService.getFinalVersion(factoryCode, date);
        if (null == finalVersion) {
            return null;
        }
        FactoryMonthPlanFinalVersionInfoVo versionInfo = new FactoryMonthPlanFinalVersionInfoVo();
        BeanUtils.copyProperties(finalVersion, versionInfo);
        return versionInfo;
    }

    /**
     * 定稿 - 年月+分厂+需求计划版本+分厂月计划版本
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult finalized(FactoryMonthPlanProdFinal param) {
        // 保证填写完整：年月、分厂、需求计划版本、分厂月计划版本
        if (param.getYear() == null || param.getMonth() == null || StringUtils.isBlank(param.getFactoryCode())
                || StringUtils.isBlank(param.getMonthPlanVersion()) || StringUtils.isBlank(param.getProductionVersion())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.finalized.checkParam"));
        }

        // 如果对应年月、分厂的最终计划数据已经存在
        Long finalCount = factoryMonthPlanProdFinalMapper.selectCount(Wrappers.lambdaQuery(FactoryMonthPlanProdFinal.class)
                .eq(FactoryMonthPlanProdFinal::getYear, param.getYear())
                .eq(FactoryMonthPlanProdFinal::getMonth, param.getMonth())
                .eq(FactoryMonthPlanProdFinal::getFactoryCode, param.getFactoryCode()));
        if (finalCount > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.finalized.exist"));
        }

        // 查询对应年月、分厂的分厂月计划排产汇总结果
//        List<MonthPlanMouldingDayResult> resultList = monthPlanMouldingDayResultMapper.selectList(Wrappers.lambdaQuery(MonthPlanMouldingDayResult.class)
//                .eq(MonthPlanMouldingDayResult::getYear, param.getYear())
//                .eq(MonthPlanMouldingDayResult::getMonth, param.getMonth())
//                .eq(MonthPlanMouldingDayResult::getFactoryCode, param.getFactoryCode())
//                .eq(MonthPlanMouldingDayResult::getMonthPlanVersion, param.getMonthPlanVersion())
//                .eq(MonthPlanMouldingDayResult::getProductionVersion, param.getProductionVersion()));
//        if (CollectionUtils.isEmpty(resultList)) {
//            // 如果对应年月、分厂的分厂月计划排产结果不存在，提示错误信息
//            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.finalized.resultNotFound"));
//        }

        // 更新版本表-是否定稿
        MpFactoryProductionVersion productionVersion = new MpFactoryProductionVersion();
        productionVersion.setIsFinal(YesOrNoEnum.YES.getCode());
        factoryProductionVersionMapper.update(productionVersion, Wrappers.lambdaQuery(MpFactoryProductionVersion.class)
                .eq(MpFactoryProductionVersion::getYear, param.getYear())
                .eq(MpFactoryProductionVersion::getMonth, param.getMonth())
                .eq(MpFactoryProductionVersion::getFactoryCode, param.getFactoryCode())
                .eq(MpFactoryProductionVersion::getMonthPlanVersion, param.getMonthPlanVersion())
                .eq(MpFactoryProductionVersion::getProductionVersion, param.getProductionVersion()));

        String monthPlanVersion = incrementService
                .getBillNoSequenceByExpire(IncrementConstant.MONTH_FINAL + DateUtils.dateTimeNow("yyyyMMdd"), 3, 60 * 24 * 7);
        int index = 1;
        // 构建对应计划排产汇总结果为最终排产表记录
        List<FactoryMonthPlanProdFinal> finalList = new ArrayList<>();
        Map<String, String> locationTypeMap = Maps.newHashMap();
//        for (int i = 0; i < resultList.size(); i++) {
//            MonthPlanMouldingDayResult itemResult = resultList.get(i);
//            FactoryMonthPlanProdFinal itemFinal = new FactoryMonthPlanProdFinal();
//            BeanUtils.copyProperties(itemResult, itemFinal);
//            Integer year = itemResult.getYear();
//            Integer month = itemResult.getMonth();
//            // 年月拼接
//            if (null != year && null != month) {
//                String yearAndMonth = String.format("%s%02d", year, month);
//                itemFinal.setYearMonth(Integer.valueOf(yearAndMonth));
//            }
//            itemFinal.setIsImport(String.valueOf(Constant.FALSE));
//            // 排产单号
//            itemFinal.setProductionNo(monthPlanVersion + String.format("%06d", index));
//            index++;
//            itemFinal.setId(null);
//            itemFinal.setBaseVale(null);
//            finalList.add(itemFinal);
//            //            locationTypeMap.put(itemResult.getProductCode(), itemResult.getLocationType());
//        }
        // 插入最终排产表
        baseDao.insertBatch(finalList);
        //20250922 ZLT 按SKU一条记录的排产结果表
//    factoryMonthPlanProductionFinalService.saveFinalizedData(param);

        // 将排产结果表复制到最终明细表
        factoryMonthPlanProdFinalMapper.copyDetailByResult(param);
        List<FactoryMonthPlanProdFinal> result = buildPlanProdFinal(param, locationTypeMap);
        // 按照年月、分厂、物料、规格汇总保存到外胎汇总表，再按照库位保存到外胎汇总明细表
//        this.monthPlanSurplusService.savePlanSurplusList(result);
        return AjaxResult.success();
    }

    /**
     * 封装SKU最终排产结果
     *
     * @param param
     * @param locationTypeMap
     * @return
     */
    private List<FactoryMonthPlanProdFinal> buildPlanProdFinal(FactoryMonthPlanProdFinal param, Map<String, String> locationTypeMap) {
        List<FactoryMonthPlanProdFinal> result = Lists.newArrayList();
//    LambdaQueryWrapper<MonthPlanProductionFinalResult> queryWrapper =
//        MonthPlanSpecificationHelper.buildFinalResultQuery(
//            param.getFactoryCode(), param.getYear(), param.getMonth(),
//            param.getMonthPlanVersion(), param.getProductionVersion());
//    List<MonthPlanProductionFinalResult> planProductionFinalResults = factoryMonthPlanProductionFinalService.list(queryWrapper);
//    if (CollectionUtils.isEmpty(planProductionFinalResults)) {
//      return result;
//    }
//    if (CollectionUtils.isNotEmpty(planProductionFinalResults)) {
//      planProductionFinalResults.forEach(item -> {
//        FactoryMonthPlanProdFinal itemFinal = new FactoryMonthPlanProdFinal();
//        BeanUtils.copyProperties(item, itemFinal);
//        if (locationTypeMap.containsKey(item.getProductCode())) {
//          itemFinal.setLocationType(locationTypeMap.get(item.getProductCode()));
//        } else {
//          itemFinal.setLocationType(LocationTypeEnum.DOMESTIC_LOCATION.getValue());
//        }
//        result.add(itemFinal);
//      });
//    }
        return result;
    }

    /**
     * 定稿调整-更新月度外胎汇总
     *
     * @param finalList 定稿的调整记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finalUpdatePlanSurplusList(List<FactoryMonthPlanProdFinal> finalList) {
        // 查询历史分厂、年、月、SAP代码、规格的定稿列表
        List<FactoryMonthPlanProdFinal> allList = factoryMonthPlanProdFinalMapper.selectParamList(finalList);
        // 重新汇总对应月度外胎汇总
//        this.monthPlanSurplusService.savePlanSurplusList(allList);
    }

    /**
     * 统计分厂月生产计划排产结果-排产结果列表
     */
    @Override
    public MonthPlanStatisticsVo statistics(FactoryMonthPlanProdFinal queryVO) {
        QueryWrapper<FactoryMonthPlanProdFinal> queryWrapper = new QueryWrapper<>();
        //构建查询条件
        this.builderCondition(queryWrapper, queryVO);

        MonthPlanStatisticsVo statisticsVo = new MonthPlanStatisticsVo();

        // 查询排产SAP个数、已排SAP总量
        queryWrapper.select("count(distinct PRODUCT_CODE) as productionCount,sum(TOTAL_QTY) as productionSum");
        List<Map<String, Object>> mapList = factoryMonthPlanProdFinalMapper.selectMaps(queryWrapper);
        if (CollectionUtils.isNotEmpty(mapList)) {
            Map<String, Object> resultMap = mapList.get(0);
            if (resultMap != null && resultMap.get("productionCount") != null) {
                statisticsVo.setProductionCount(Long.parseLong(resultMap.get("productionCount").toString()));
            }
            if (resultMap != null && resultMap.get("productionSum") != null) {
                statisticsVo.setProductionSum(Long.parseLong(resultMap.get("productionSum").toString()));
            }
        }

        // 根据年月、分厂查询首条定稿记录
        QueryWrapper<FactoryMonthPlanProdFinal> firstWrapper = new QueryWrapper<>();
        firstWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        firstWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("year")), "YEAR", queryVO.getFieldValueByFieldName("year"));
        firstWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month")), "MONTH", queryVO.getFieldValueByFieldName("month"));
        firstWrapper.last("limit 1");
        List<FactoryMonthPlanProdFinal> finalList = factoryMonthPlanProdFinalMapper.selectList(firstWrapper);
        if (CollectionUtils.isEmpty(finalList)) {
            return statisticsVo;
        }

        FactoryMonthPlanProdFinal prodFinal = finalList.get(0);

        // 统计备货量
//        if (StringUtils.isNotBlank(prodFinal.getProductionVersion())) {
//            QueryWrapper<MonthPlanProdDetailFinal> detailWrapper = new QueryWrapper<>();
//            detailWrapper.select("sum(TOTAL_QTY) as stockNum");
//            builderCondition(detailWrapper, queryVO);
//            detailWrapper.eq("PRODUCTION_VERSION", prodFinal.getProductionVersion());
//            detailWrapper.eq("IS_STOCK_UP", Constant.TRUE);
//            List<Map<String, Object>> detailMapList = monthPlanProdDetailFinalMapper.selectMaps(detailWrapper);
//            if (CollectionUtils.isNotEmpty(detailMapList)) {
//                Map<String, Object> resultMap = detailMapList.get(0);
//                if (resultMap != null && resultMap.get("stockNum") != null) {
//                    statisticsVo.setStockNum(Long.parseLong(resultMap.get("stockNum").toString()));
//                }
//            }
//        }

        // 查询未排的SAP总量
        MonthPlanNoProductionPlan noProductionPlan = new MonthPlanNoProductionPlan();
        BeanUtils.copyProperties(queryVO, noProductionPlan);
        noProductionPlan.setMonthPlanVersion(prodFinal.getMonthPlanVersion());
        noProductionPlan.setProductionVersion(prodFinal.getProductionVersion());
        iMonthPlanNoProductionPlanService.statistics(statisticsVo, noProductionPlan);

        // 查询提报的SAP个数、提报的SAP总量
        OrderPlanAllocation orderPlanAllocation = new OrderPlanAllocation();
        BeanUtils.copyProperties(queryVO, orderPlanAllocation);
        orderPlanAllocation.setMonthPlanVersion(prodFinal.getMonthPlanVersion());
        iOrderPlanAllocationService.statistics(statisticsVo, orderPlanAllocation);

        return statisticsVo;
    }

    /**
     * 构建查询条件
     */
    @Override
    public void builderCondition(QueryWrapper<?> queryWrapper, FactoryMonthPlanProdFinal queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("year")), "YEAR", queryVO.getFieldValueByFieldName("year"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month")), "MONTH", queryVO.getFieldValueByFieldName("month"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("yearMonth")), "YEAR_MONTH", queryVO.getFieldValueByFieldName("yearMonth"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("monthPlanVersion")), "MONTH_PLAN_VERSION", queryVO.getFieldValueByFieldName("monthPlanVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productionVersion")), "PRODUCTION_VERSION", queryVO.getFieldValueByFieldName("productionVersion"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productCode")), "PRODUCT_CODE", queryVO.getFieldValueByFieldName("productCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productDesc")), "PRODUCT_DESC", queryVO.getFieldValueByFieldName("productDesc"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specifications")), "SPECIFICATIONS", queryVO.getFieldValueByFieldName("specifications"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("pattern")), "PATTERN", queryVO.getFieldValueByFieldName("pattern"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("hierarchy")), "HIERARCHY", queryVO.getFieldValueByFieldName("hierarchy"));

        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("levelCode")), "LEVEL_CODE", queryVO.getFieldValueByFieldName("levelCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("levelName")), "LEVEL_NAME", queryVO.getFieldValueByFieldName("levelName"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("proSize")), "PRO_SIZE", queryVO.getFieldValueByFieldName("proSize"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("locationType")), "LOCATION_TYPE", queryVO.getFieldValueByFieldName("locationType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("channel")), "CHANNEL", queryVO.getFieldValueByFieldName("channel"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("brand")), "BRAND", queryVO.getFieldValueByFieldName("brand"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productTypeCode")), "PRODUCT_TYPE_CODE", queryVO.getFieldValueByFieldName("productTypeCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productTypeName")), "PRODUCT_TYPE_NAME", queryVO.getFieldValueByFieldName("productTypeName"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("reason")), "REASON", queryVO.getFieldValueByFieldName("reason"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mouldNo")), "MOULD_NO", queryVO.getFieldValueByFieldName("mouldNo"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specCode")), "SPEC_CODE", queryVO.getFieldValueByFieldName("specCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("embryoCode")), "EMBRYO_CODE", queryVO.getFieldValueByFieldName("embryoCode"));
    }

    @Override
    public List<DayProductionTotalVo> statisticsDay(FactoryMonthPlanProdFinal query) {
        FactoryMonthPlanFinalVersionInfoVo finalVersion = getFinalVersionInfo(query.getFactoryCode(), query.getYear(), query.getMonth());
        if (null == finalVersion) {
            return Collections.emptyList();
        }
        String productionVersion = finalVersion.getProductionVersion();
        if (StringUtils.isBlank(productionVersion)) {
            return Collections.emptyList();
        }
        Integer[] dayArrays = FactoryConstant.PRODUCTION_CYCLE;
        List<Integer> days = new ArrayList<>();
        for (Integer day : dayArrays) {
            if (day > FactoryConstant.MONTH_START_DAY) {
                days.add(day);
            }
        }
        List<DayProductionTotalVo> dayTotalList = factoryMonthPlanProdFinalMapper.getStatisticsDay(productionVersion, days);
        if (CollectionUtils.isEmpty(dayTotalList)) {
            return Collections.emptyList();
        }
        dayTotalList.stream().forEach(dayTotal -> {
            if (null == dayTotal.getQty()) {
                dayTotal.setQty(BigDecimal.ZERO.intValue());
            }
        });
        return dayTotalList;
    }

    @Override
    public FactoryMonthPlanTypeVo getProductionMonthType(FactoryMonthPlanProdFinal query) {
        FactoryMonthPlanTypeVo type = new FactoryMonthPlanTypeVo();
        FactoryMonthPlanFinalVersionInfoVo finalVersion = getFinalVersionInfo(query.getFactoryCode(), query.getYear(), query.getMonth());
        if (null == finalVersion) {
            return type;
        }
        String productionVersion = finalVersion.getProductionVersion();
        if (StringUtils.isBlank(productionVersion)) {
            return type;
        }
        if (YesOrNoEnum.YES.getValue().equals(finalVersion.getIsNaturalMonth())) {
            return type;
        }
        type.setProductionStartDate(finalVersion.getProductionStartDate());
        return type;
    }

    @Override
    public List<MonthPlanRequireStock> getSaleMonthPlanRequireStock(String monthPlanVersion) {
        LambdaQueryWrapper<MonthPlanRequireStock> query = Wrappers.lambdaQuery(MonthPlanRequireStock.class)
                .eq(MonthPlanRequireStock::getMonthPlanVersion, monthPlanVersion)
                .eq(MonthPlanRequireStock::getIsDelete, ApsConstant.APS_YES_NO_0);

        return saleMonthPlanRequireStockMapper.selectList(query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult doImportData(List<FactoryMonthPlanProdFinal> list, boolean updateSupport, long importLogId) {
        // 只能导入一个分厂版本
        Set<String> productionVersionSet = list.stream().map(FactoryMonthPlanProdFinal::getSameProductionVersionKey).collect(Collectors.toSet());
        if (productionVersionSet.size() > BigDecimal.ONE.intValue()) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanMouldingDayResult.importSameVersion"));
        }
        String yearAndMonth = "";
        //没定稿前不能导入
        Optional<FactoryMonthPlanProdFinal> first = list.stream().filter(v -> StringUtils.isNotBlank(v.getProductionVersion()) && v.getYear() != null && v.getMonth() != null).findFirst();
        if (first.isPresent()) {
            FactoryMonthPlanProdFinal item = first.get();
            Integer year = item.getYear();
            Integer month = item.getMonth();
            if (!isFinal(item.getFactoryCode(), year, month)) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.finalized.noFinalized"));
            }
            // 年月拼接
            if (null != year && null != month) {
                yearAndMonth = String.format("%s%02d", year, month);
            }
        }
        //根据版本信息，调整起始日，开始日及day排产量的值
        MpFactoryProductionVersion productionVersion = factoryProductionVersionService.getProductionVersion(list.get(0).getProductionVersion());
//    ProductionPlanExcelUtils.handlerFinalProductionDayQty(productionVersion, list);
        // 国际化提示
        Map<String, String> errorInfoMap = buildErrorInfoMap();
        // 初始化
        int successNum;
        int failureNum = 0;
        List<FactoryMonthPlanProdFinal> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String productionNoPrefix = incrementService.getBillNoSequenceByExpire(IncrementConstant.MONTH_FINAL + DateUtils.dateTimeNow("yyyyMMdd"), 3, 60 * 24 * 7);
        //唯一键分组
        Function<FactoryMonthPlanProdFinal, String> duplicateKeyFunction = FactoryMonthPlanProdFinal::getImportDuplicateKey;
        Map<String, Long> duplicateGroupMap = list.stream().collect(Collectors.groupingBy(duplicateKeyFunction, Collectors.counting()));
        Map<String, MdmProductConstruction> productConstructionMap = new HashMap<>();
        Map<String, List<MdmSkuMouldRel>> mouldBaseInfoMap = new HashMap<>();
        List<FactoryMonthPlanProdFinal> informalConstructionStageList = new ArrayList<>();
        String batchNo = "T" + DateUtils.dateTimeNow();
        int productCodeIndex = 0;
        //构建制造单号流水号
        int addIndex = 1;
        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            FactoryMonthPlanProdFinal item = list.get(i);
            String productionNo = item.getProductionNo();
            if (StringUtils.isBlank(productionNo)) {
                String createProductionNo = productionNoPrefix + String.format("%06d", addIndex);
                item.setProductionNo(createProductionNo);
                addIndex = addIndex + 1;
            }
            if (StringUtils.isNotBlank(yearAndMonth)) {
                item.setYearMonth(Integer.valueOf(yearAndMonth));
            }
            //数据校验
            boolean checkDataResult = checkDataAndFullInfo(item, importLogId, errorNum, importErrorLogs, errorInfoMap, duplicateGroupMap, duplicateKeyFunction, productConstructionMap);
            if (!checkDataResult) {
                failureNum++;
                continue;
            }
            //构建试制量试数据可能需要自动生成SAP与施工关系数据集合
            productCodeIndex = addProductConstructionConfiguration(batchNo, productCodeIndex, item, informalConstructionStageList);
            //校验模具并补充模具号
            boolean checkMouldInfoResult = checkDataMouldInfoAndFullMouldNo(item, importLogId, errorNum, importErrorLogs, mouldBaseInfoMap);
            if (!checkMouldInfoResult) {
                failureNum++;
                continue;
            }
            //加入数据
            importList.add(item);
        }
        //补充物料信息
        if (CollectionUtils.isNotEmpty(importList)) {
            // 查询对应物料信息，根据分厂+SAP代码映射
            Map<String, MdmMaterialInfo> productInfoMap = getMdmMaterialInfoMap(importList);
            fullProductInfo(importList, productInfoMap);
        }
        //保存SAP与施工关系
        if (!CollectionUtils.isEmpty(informalConstructionStageList)) {
            saveMdmProductConstructionInfo(informalConstructionStageList, productConstructionMap);
        }
        if (!CollectionUtils.isEmpty(importList)) {
            try {
                successNum = importList.size();
                saveImportData(importList);
                // 重新汇总对应月度外胎汇总
                finalUpdatePlanSurplusList(importList);
            } catch (Exception e) {
                log.error("计划调整-导入异常", e);
                successNum = 0;
                failureNum = list.size();
                importErrorLogs.clear();
                addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
            }
        } else {
            successNum = 0;
        }
        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
    }


    /**
     * 判断分厂、年月是否已经定稿
     *
     * @param factoryCode 分厂编码
     * @param year        年份
     * @param month       月份
     * @return true已定稿， false未定稿
     */
    private boolean isFinal(String factoryCode, Integer year, Integer month) {
        Long finalVersionCount = factoryProductionVersionMapper.selectCount(Wrappers.lambdaQuery(MpFactoryProductionVersion.class)
                .eq(MpFactoryProductionVersion::getFactoryCode, factoryCode)
                .eq(MpFactoryProductionVersion::getYear, year)
                .eq(MpFactoryProductionVersion::getMonth, month)
                .eq(MpFactoryProductionVersion::getIsFinal, Constant.TRUE));
        return finalVersionCount > 0;
    }

    /**
     * 查询对应物料信息，根据分厂+SAP代码映射
     *
     * @param list 排产计划结合
     */
    private Map<String, MdmMaterialInfo> getMdmMaterialInfoMap(List<FactoryMonthPlanProdFinal> list) {
        if (CollectionUtils.isEmpty(list)) {
            return new HashMap<>();
        }
        List<String> factoryCodeList = list.stream().map(FactoryMonthPlanProdFinal::getFactoryCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        List<String> productCodeList = list.stream().map(FactoryMonthPlanProdFinal::getProductCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(factoryCodeList) && CollectionUtils.isEmpty(productCodeList)) {
            return Collections.emptyMap();
        }

        LambdaQueryWrapper<MdmMaterialInfo> wrapper = Wrappers.lambdaQuery(MdmMaterialInfo.class)
                .in(CollectionUtils.isNotEmpty(factoryCodeList), MdmMaterialInfo::getFactoryCode, factoryCodeList)
                .in(CollectionUtils.isNotEmpty(productCodeList), MdmMaterialInfo::getMaterialCode, productCodeList);
        return productInfoEntityMapper.selectList(wrapper).stream().collect(Collectors.toMap(v -> GenerageMapKeyUtils.createMapKey(v.getFactoryCode(), v.getMaterialCode()), Function.identity(), (v1, v2) -> v1));
    }

    /**
     * 构建检验错误提示信息集合
     *
     * @return
     */
    private Map<String, String> buildErrorInfoMap() {
        Map<String, String> errorInfoMap = new HashMap<>();
        String repeat = I18nUtil.getMessage("ui.data.column.monthPlanMouldingDayResult.repeat");
        String productCodeNotExist = I18nUtil.getMessage("ui.data.column.monthPlanMouldingDayResult.productCode.notExist");
        String factProdReqQtyMax = I18nUtil.getMessage("ui.data.column.monthPlanMouldingDayResult.factProdReqQtyMax");
        String totalQtyCheck = I18nUtil.getMessage("ui.data.column.monthPlanMouldingDayResult.totalQtyCheck");
        String endDayCheck = I18nUtil.getMessage("ui.data.column.monthPlanMouldingDayResult.endDayCheck");
        String endDayMaxCheck = I18nUtil.getMessage("ui.data.column.monthPlanMouldingDayResult.endDayMaxCheck");
        String sameEmpty = I18nUtil.getMessage("ui.data.column.factoryMonthPlanProdFinal.sameEmpty");
        String monthMaxDay = I18nUtil.getMessage("ui.data.column.factoryMonthPlanProdFinal.monthMaxDay");
        errorInfoMap.put(ERROR_REPEAT, repeat);
        errorInfoMap.put(ERROR_PRODUCT_CODE, productCodeNotExist);
        errorInfoMap.put(ERROR_FACTORY_REQ_QTY, factProdReqQtyMax);
        errorInfoMap.put(ERROR_TOTAL_QTY, totalQtyCheck);
        errorInfoMap.put(ERROR_END_DAY, endDayCheck);
        errorInfoMap.put(ERROR_EDN_MAX, endDayMaxCheck);
        errorInfoMap.put(ERROR_SAME_EMPTY, sameEmpty);
        errorInfoMap.put(ERROR_MAX_DAY, monthMaxDay);
        return errorInfoMap;
    }

    /**
     * 数据行校验
     *
     * @param item                   数据行
     * @param importLogId            导入日志ID
     * @param errorNum               错误行
     * @param importErrorLogs        错误日志集合对象
     * @param buildErrorInfo         错误信息集合
     * @param duplicateGroupMap      重复数据集合
     * @param duplicateKeyFunction   重复键集合
     * @param productConstructionMap SAP与施工关系配置
     * @return
     */
    private boolean checkDataAndFullInfo(FactoryMonthPlanProdFinal item, Long importLogId, Integer errorNum, List<ImportErrorLog> importErrorLogs, Map<String, String> buildErrorInfo, Map<String, Long> duplicateGroupMap, Function<FactoryMonthPlanProdFinal, String> duplicateKeyFunction, Map<String, MdmProductConstruction> productConstructionMap) {
        //数据基本校验
        List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, item);
        if (CollectionUtils.isNotEmpty(validated)) {
            importErrorLogs.addAll(validated);
            return false;
        }
        int dayOfMonth = LocalDate.of(item.getYear(), item.getMonth(), 1).with(TemporalAdjusters.lastDayOfMonth()).getDayOfMonth();
        if (isExceedMonthMaxDay(item, dayOfMonth)) {
            addImportErrorLog(importLogId, errorNum, String.format(buildErrorInfo.get(ERROR_MAX_DAY), dayOfMonth), importErrorLogs);
            return false;
        }
        //统计排产总值
        resetTotalProductionQty(item);
        //实际生产需求(含损耗)必须 >= 生产需求计划、生产实际排产量
        /*if (item.getFactProdReqQty() < item.getProdReqPlan() || item.getFactProdReqQty() < item.getTotalQty()) {
            addImportErrorLog(importLogId, errorNum, buildErrorInfo.get(ERROR_FACTORY_REQ_QTY), importErrorLogs);
            return false;
        }*/
        // 生产实际排产量 > 0 ，模具数不能 <= 0
        if (item.getTotalQty() > 0 && item.getMouldQty() <= 0) {
            addImportErrorLog(importLogId, errorNum, buildErrorInfo.get(ERROR_TOTAL_QTY), importErrorLogs);
            return false;
        }
        Integer beginDay = item.getBeginDate();
        Integer endDay = item.getEndDay();
        if (!haseDoubleDayValue(beginDay, endDay)) {
            addImportErrorLog(importLogId, errorNum, buildErrorInfo.get(ERROR_SAME_EMPTY), importErrorLogs);
            return false;
        }
        if (null != beginDay && null != endDay) {
            // 开始不能大于结束时间，结束时间不能大于月份最大天数
            if (beginDay > endDay) {
                addImportErrorLog(importLogId, errorNum, buildErrorInfo.get(ERROR_END_DAY), importErrorLogs);
                return false;
            }
            if (endDay > dayOfMonth) {
                addImportErrorLog(importLogId, errorNum, buildErrorInfo.get(ERROR_EDN_MAX), importErrorLogs);
                return false;
            }
        }
        //根据硫化规格生胎代号获取SAP代码
        String productConstructionKey = item.getProductConstructionKey();
        if (!productConstructionMap.containsKey(productConstructionKey)) {
            getProductConstructionInfo(productConstructionKey, item, productConstructionMap);
        }
        //如果没有
        if (StringUtils.isBlank(item.getProductCode())) {
            MdmProductConstruction configuration = productConstructionMap.get(productConstructionKey);
            if (null != configuration) {
                item.setProductCode(configuration.getProductCode());
            }
        }
        item.setIsImport(String.valueOf(Constant.TRUE));
        return true;
    }

    /**
     * 根据硫化规格及生胎号，获取SAP与施工配置
     *
     * @param key                    键值
     * @param item                   包含硫化时间
     * @param productConstructionMap 施工配置
     */
    private void getProductConstructionInfo(String key, FactoryMonthPlanProdFinal item, Map<String, MdmProductConstruction> productConstructionMap) {
        QueryWrapper<MdmProductConstruction> productConstructionQuery = new QueryWrapper<>();
        productConstructionQuery.eq("FACTORY_CODE", item.getFactoryCode());
        productConstructionQuery.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        productConstructionQuery.eq("SPEC_CODE", item.getSpecCode());
        productConstructionQuery.eq("EMBRYO_CODE", item.getEmbryoCode());
        List<MdmProductConstruction> configurationList = mdmProductConstructionMapper.selectList(productConstructionQuery);
        if (CollectionUtils.isEmpty(configurationList)) {
            productConstructionMap.put(key, null);
            return;
        }
        productConstructionMap.put(key, configurationList.get(0));
    }

    /**
     * 填充物料信息
     *
     * @param importList     需要导入的计划集合
     * @param productInfoMap 物料信息
     */
    private void fullProductInfo(List<FactoryMonthPlanProdFinal> importList, Map<String, MdmMaterialInfo> productInfoMap) {
        importList.stream().forEach(item -> {
            //物料信息补充-不进行校验
            MdmMaterialInfo productInfo = productInfoMap.get(GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getProductCode()));
            if (null == productInfo) {
                return;
            }
            item.setProductDesc(productInfo.getMaterialDesc());
            item.setProSize(String.valueOf(productInfo.getProSize()));
            item.setSpecifications(productInfo.getSpecifications());
            item.setPattern(productInfo.getPattern());
            item.setHierarchy(productInfo.getHierarchy());
            item.setProductTypeCode(productInfo.getProductTypeCode());
            item.setProductTypeName(productInfo.getProductTypeName());
            item.setBrand(productInfo.getBrand());
        });
    }

    /**
     * 校验模具信息是否存在，试制量试
     *
     * @param item
     * @param importLogId
     * @param errorNum
     * @param importErrorLogs
     * @param mouldBaseInfoMap
     * @return
     */
    private boolean checkDataMouldInfoAndFullMouldNo(FactoryMonthPlanProdFinal item, Long importLogId, Integer errorNum, List<ImportErrorLog> importErrorLogs, Map<String, List<MdmSkuMouldRel>> mouldBaseInfoMap) {
        String mouldErrorInfo = I18nUtil.getMessage("ui.data.column.monthPlanMouldingDayResult.specCodeMouldNoErrorInfo");
        String mouldNumberErrorInfo = I18nUtil.getMessage("ui.data.column.monthPlanMouldingDayResult.specCodeMouldNumberErrorInfo");
        String constructionStage = item.getConstructionStage();
        ConstructionStageEnum stage = ConstructionStageEnum.getInstance(constructionStage);
        if (null == stage) {
            return true;
        }
        if (ConstructionStageEnum.FORMAL_PRODUCTION == stage) {
            return true;
        }
        String factoryCode = item.getFactoryCode();
        String specCode = item.getSpecCode();
        Integer mouldQty = item.getMouldQty();
        if (mouldBaseInfoMap.containsKey(specCode)) {
            List<MdmSkuMouldRel> modelRelationList = mouldBaseInfoMap.get(specCode);
            if (CollectionUtils.isEmpty(modelRelationList)) {
                addImportErrorLog(importLogId, errorNum, String.format(mouldErrorInfo, specCode), importErrorLogs);
                return false;
            }
            if (modelRelationList.size() < mouldQty) {
                addImportErrorLog(importLogId, errorNum, String.format(mouldNumberErrorInfo, specCode, mouldQty), importErrorLogs);
                return false;
            }
            return true;
        }
        QueryWrapper<MdmSkuMouldRel> modelRelationQuery = new QueryWrapper<>();
        modelRelationQuery.eq("FACTORY_CODE", factoryCode);
        modelRelationQuery.eq("SPEC_CODE", specCode);
        List<MdmSkuMouldRel> modelRelationList = productModelRelationEntityMapper.selectList(modelRelationQuery);
        if (CollectionUtils.isEmpty(modelRelationList)) {
            mouldBaseInfoMap.put(specCode, Collections.emptyList());
            addImportErrorLog(importLogId, errorNum, String.format(mouldErrorInfo, specCode), importErrorLogs);
            return false;
        }
        mouldBaseInfoMap.put(specCode, modelRelationList);
        if (modelRelationList.size() < mouldQty) {
            addImportErrorLog(importLogId, errorNum, String.format(mouldNumberErrorInfo, specCode, mouldQty), importErrorLogs);
            return false;
        }
        List<String> mouldCodeList = modelRelationList.stream().map(MdmSkuMouldRel::getMouldCode).collect(Collectors.toList());
        QueryWrapper<MdmModelInfo> mouldNoQuery = new QueryWrapper<>();
        mouldNoQuery.eq("FACTORY_CODE", item.getFactoryCode());
        mouldNoQuery.in("MOULD_CODE", mouldCodeList);
        List<MdmModelInfo> mouldInfoList = modelInfoEntityMapper.selectList(mouldNoQuery);
        if (CollectionUtils.isEmpty(mouldInfoList)) {
            mouldBaseInfoMap.put(specCode, Collections.emptyList());
            addImportErrorLog(importLogId, errorNum, String.format(mouldErrorInfo, specCode), importErrorLogs);
            return false;
        }
        if (mouldInfoList.size() < mouldQty) {
            addImportErrorLog(importLogId, errorNum, String.format(mouldNumberErrorInfo, specCode, mouldQty), importErrorLogs);
            return false;
        }
        String mouldNo = mouldInfoList.get(0).getMouldNo();
        item.setMouldNo(mouldNo);
        return true;
    }

    /**
     * 有则更新，无则插入
     */
    private void saveImportData(List<FactoryMonthPlanProdFinal> importList) {
        if (CollectionUtils.isEmpty(importList)) {
            return;
        }
        //查询旧数据
        LambdaQueryWrapper<FactoryMonthPlanProdFinal> wrapper = LambdaWrapperBuilder.buildWrapperByFunction(importList, FactoryMonthPlanProdFinal::getProductionNo);
        List<FactoryMonthPlanProdFinal> oldList = factoryMonthPlanProdFinalMapper.selectList(wrapper);
        Map<String, FactoryMonthPlanProdFinal> oldMap = oldList.stream().collect(Collectors.toMap(FactoryMonthPlanProdFinal::getProductionNo, Function.identity(), (v1, v2) -> v1));
        //分组更新和插入
        List<FactoryMonthPlanProdFinal> updateList = new ArrayList<>();
        List<FactoryMonthPlanProdFinal> insertList = new ArrayList<>();
        for (FactoryMonthPlanProdFinal item : importList) {
            String productionNo = item.getProductionNo();
            if (oldMap.containsKey(productionNo)) {
                setOldValue(item, oldMap.get(productionNo));
                updateList.add(item);
            } else {
                insertList.add(item);
            }
        }
        baseDao.insertBatch(insertList);
        baseDao.updateBatch(updateList);
    }

    /**
     * 获取非正式施工计划
     *
     * @param importList
     * @return
     */
    private List<FactoryMonthPlanProdFinal> getInformalConstructionStagePlan(List<FactoryMonthPlanProdFinal> importList) {
        List<FactoryMonthPlanProdFinal> informalConstructionStageList = new ArrayList<>();
        String batchNo = "T" + DateUtils.dateTimeNow();
        //自动生成productCode的流水号--没有物料编码时
        int index = 0;
        for (FactoryMonthPlanProdFinal finalPlan : importList) {
            String constructionStage = finalPlan.getConstructionStage();
            ConstructionStageEnum stage = ConstructionStageEnum.getInstance(constructionStage);
            if (null == stage) {
                continue;
            }
            if (ConstructionStageEnum.FORMAL_PRODUCTION == stage) {
                continue;
            }
            String productCode = finalPlan.getProductCode();
            if (StringUtils.isBlank(productCode)) {
                index = index + 1;
                finalPlan.setProductCode(batchNo + String.format("%03d", index));
            }
            informalConstructionStageList.add(finalPlan);
        }
        return informalConstructionStageList;
    }

    /**
     * 加入可能需要自动构建SAP与施工信息的集合数据中
     *
     * @param batchNo                       批次号
     * @param index                         流水号
     * @param importFinalPlan               计划
     * @param informalConstructionStageList 构建SAP与施工关系的数据集合
     * @return
     */
    private int addProductConstructionConfiguration(String batchNo, int index, FactoryMonthPlanProdFinal importFinalPlan, List<FactoryMonthPlanProdFinal> informalConstructionStageList) {
        String constructionStage = importFinalPlan.getConstructionStage();
        ConstructionStageEnum stage = ConstructionStageEnum.getInstance(constructionStage);
        if (null == stage) {
            return index;
        }
        if (ConstructionStageEnum.FORMAL_PRODUCTION == stage) {
            return index;
        }
        String productCode = importFinalPlan.getProductCode();
        if (StringUtils.isBlank(productCode)) {
            index = index + 1;
            importFinalPlan.setProductCode(batchNo + String.format("%03d", index));
        }
        informalConstructionStageList.add(importFinalPlan);
        return index;
    }

    /**
     * 保存SAP与施工关系
     *
     * @param informalConstructionStageList 调整计划集合
     * @param productConstructionMap        SAP与施工关系
     */
    private void saveMdmProductConstructionInfo(List<FactoryMonthPlanProdFinal> informalConstructionStageList, Map<String, MdmProductConstruction> productConstructionMap) {
        String factoryCode = informalConstructionStageList.get(0).getFactoryCode();
        Integer curingTime = factoryParamService.getInformalConstructionCuringTime(factoryCode);
        Set<String> addConfigurationSet = new HashSet<>();
        List<MdmProductConstruction> addProductConstructionList = new ArrayList<>();
        informalConstructionStageList.stream().forEach(informalConstructionPlan -> {
            String key = informalConstructionPlan.getProductConstructionKey();
            MdmProductConstruction configuration = productConstructionMap.get(key);
            if (null != configuration) {
                return;
            }
            if (addConfigurationSet.contains(key)) {
                return;
            }
            addConfigurationSet.add(key);
            MdmProductConstruction addConfiguration = null;
//      MdmProductConstruction addConfiguration = AdjustUtils.buildProductionConstructionConfiguration(informalConstructionPlan, curingTime);
            addProductConstructionList.add(addConfiguration);
        });
        if (!CollectionUtils.isEmpty(addProductConstructionList)) {
            baseDao.insertBatch(addProductConstructionList);
        }
    }

    /**
     * 保存SAP与模具关系
     *
     * @param informalMouldList 试制量试计划
     * @param mouldBaseInfoMap  模具基础信息集合
     */
    private void saveMouldRelationInfo(List<FactoryMonthPlanProdFinal> informalMouldList, Map<String, List<MdmModelInfo>> mouldBaseInfoMap) {
        Set<String> addConfigurationSet = new HashSet<>();
        List<MdmSkuMouldRel> addProductModelList = new ArrayList<>();
        informalMouldList.stream().forEach(informalMouldPlan -> {
            String key = informalMouldPlan.getProductMouldKey();
            if (addConfigurationSet.contains(key)) {
                return;
            }
            String mouldNo = informalMouldPlan.getMouldNo();
            Integer mouldQty = informalMouldPlan.getMouldQty();
            String specCode = informalMouldPlan.getSpecCode();
            if (StringUtils.isBlank(mouldNo) || null == mouldQty) {
                return;
            }
            List<MdmModelInfo> modelInfoList = mouldBaseInfoMap.get(specCode);
            if (CollectionUtils.isEmpty(modelInfoList)) {
                return;
            }
            if (modelInfoList.size() < mouldQty) {
                return;
            }
            for (int index = 0; index < mouldQty; index++) {
                MdmModelInfo mouldInfo = modelInfoList.get(index);
                MdmSkuMouldRel addConfiguration = new MdmSkuMouldRel();
                addConfiguration.setFactoryCode(informalMouldPlan.getFactoryCode());
                addConfiguration.setMaterialCode(informalMouldPlan.getProductCode());
                addConfiguration.setMouldCode(mouldInfo.getMouldCode());
                addConfiguration.setMouldNo(mouldNo);
                addConfiguration.setBrand(informalMouldPlan.getBrand());
                addConfiguration.setMaterialDesc(informalMouldPlan.getProductDesc());
                addConfiguration.setSpecCode(informalMouldPlan.getSpecCode());
                addConfiguration.setPattern(informalMouldPlan.getPattern());
                addProductModelList.add(addConfiguration);
            }
            addConfigurationSet.add(key);
        });
        LambdaQueryWrapper<MdmSkuMouldRel> wrapper = LambdaWrapperBuilder.buildWrapperByFunction(addProductModelList, MdmSkuMouldRel::getMaterialCode, MdmSkuMouldRel::getSpecCode, MdmSkuMouldRel::getMouldCode);
        List<MdmSkuMouldRel> oldList = productModelRelationEntityMapper.selectList(wrapper);
        Map<String, MdmSkuMouldRel> oldMap = oldList.stream().collect(Collectors.toMap(MdmSkuMouldRel::getUpdateGroupKey, Function.identity(), (v1, v2) -> v1));
        List<MdmSkuMouldRel> insertList = new ArrayList<>();
        for (MdmSkuMouldRel addConfiguration : addProductModelList) {
            String key = addConfiguration.getUpdateGroupKey();
            if (oldMap.containsKey(key)) {
                continue;
            }
            insertList.add(addConfiguration);
        }
        if (!CollectionUtils.isEmpty(insertList)) {
            baseDao.insertBatch(insertList);
        }
    }

    /**
     * 是否超出了月份最大天数排产
     *
     * @param item        排产计划
     * @param monthMaxDay 月最大天数
     * @return
     */
    private boolean isExceedMonthMaxDay(FactoryMonthPlanProdFinal item, Integer monthMaxDay) {
        if (FactoryConstant.MONTH_MAX_DAY.equals(monthMaxDay)) {
            return false;
        }
        //超出月最大天数
        String fieldName;
        for (int day = monthMaxDay + 1; day <= monthMaxDay; day++) {
            fieldName = String.format("day%d", day);
            Long productionQty = (Long) item.getFieldValueByFieldName(fieldName);
            if (!(null == productionQty || productionQty == 0)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 统计总排产量
     *
     * @param item
     */
    private void resetTotalProductionQty(FactoryMonthPlanProdFinal item) {
        String fieldName;
        Long totalQty = BigDecimal.ZERO.longValue();
        for (int day = FactoryConstant.MONTH_START_DAY; day <= FactoryConstant.MONTH_MAX_DAY; day++) {
            fieldName = String.format("day%d", day);
            Long productionQty = (Long) item.getFieldValueByFieldName(fieldName);
            if (null == productionQty) {
                productionQty = BigDecimal.ZERO.longValue();
            }
            totalQty = totalQty + productionQty;
        }
//    item.setTotalQty(totalQty);
//    Long requireQty = item.getProdReqPlan();
//    if (null == requireQty) {
//      requireQty = BigDecimal.ZERO.longValue();
//    }
//    if (null == item.getFactProdReqQty()) {
//      item.setFactProdReqQty(requireQty);
//    }
//    item.setDifferenceQty(item.getFactProdReqQty() - totalQty);
    }

    /**
     * 开始日期、结束日期要么都有值，要么都没值
     *
     * @param beginDay
     * @param endDay
     * @return
     */
    private boolean haseDoubleDayValue(Integer beginDay, Integer endDay) {
        if (null == beginDay && null != endDay) {
            return false;
        }
        if (null != beginDay && null == endDay) {
            return false;
        }
        return true;
    }

    /**
     * 需要保留的字段值
     *
     * @param item
     * @param old
     */
    private void setOldValue(FactoryMonthPlanProdFinal item, FactoryMonthPlanProdFinal old) {
        item.setId(old.getId());
        item.setCuringTime(old.getCuringTime());
        item.setSpecCodeInfo(old.getSpecCodeInfo());
        item.setMergeInfo(old.getMergeInfo());
        item.setMouldInfo(old.getMouldInfo());
        item.setConstructionStage(old.getConstructionStage());
    }

    /**
     * 判断日期是否与排产月一直
     *
     * @param finalVersion 定稿版本信息
     * @param date         停工日
     * @return true 表示一直， false表示不一致
     */
    private boolean isProductionMonth(MpFactoryProductionVersion finalVersion, LocalDate date) {
        Integer year = finalVersion.getYear();
        Integer month = finalVersion.getMonth();
        Integer dateYear = date.getYear();
        Integer dateMonth = date.getMonthValue();
        return year.equals(dateYear) && month.equals(dateMonth);
    }

    /**
     * 构建查询条件
     *
     * @param queryWrapper
     * @param queryCondition
     */
    private void builderCondition(QueryWrapper<FactoryMonthPlanProdFinal> queryWrapper, FactoryMonthPlanProdFinalQueryDto queryCondition) {
        String factoryCode = queryCondition.getFactoryCode();
        queryWrapper.eq(PubUtil.isNotEmpty(factoryCode), "FACTORY_CODE", factoryCode);
        Integer year = queryCondition.getYear();
        queryWrapper.eq(PubUtil.isNotEmpty(year), "YEAR", year);
        Integer month = queryCondition.getMonth();
        queryWrapper.eq(PubUtil.isNotEmpty(month), "MONTH", month);
        String monthPlanVersion = queryCondition.getMonthPlanVersion();
        queryWrapper.eq(PubUtil.isNotEmpty(monthPlanVersion), "MONTH_PLAN_VERSION", monthPlanVersion);
        String productionVersion = queryCondition.getProductionVersion();
        queryWrapper.eq(PubUtil.isNotEmpty(productionVersion), "PRODUCTION_VERSION", productionVersion);
        String productCode = queryCondition.getProductCode();
        queryWrapper.eq(PubUtil.isNotEmpty(productCode), "PRODUCT_CODE", productCode);
        String productDesc = queryCondition.getProductDesc();
        queryWrapper.eq(PubUtil.isNotEmpty(productDesc), "PRODUCT_DESC", productDesc);
        String specifications = queryCondition.getSpecifications();
        queryWrapper.eq(PubUtil.isNotEmpty(specifications), "SPECIFICATIONS", specifications);
        String pattern = queryCondition.getPattern();
        queryWrapper.eq(PubUtil.isNotEmpty(pattern), "PATTERN", pattern);
        BigDecimal proSize = queryCondition.getProSize();
        queryWrapper.eq(PubUtil.isNotEmpty(proSize), "PRO_SIZE", proSize);
        String locationType = queryCondition.getLocationType();
        queryWrapper.eq(PubUtil.isNotEmpty(locationType), "LOCATION_TYPE", locationType);
        String channel = queryCondition.getChannel();
        queryWrapper.eq(PubUtil.isNotEmpty(channel), "CHANNEL", channel);
        String brand = queryCondition.getBrand();
        queryWrapper.eq(PubUtil.isNotEmpty(brand), "BRAND", brand);
    }

    @Autowired
    private IMesItfService mesItfService;

    @Autowired
    private FactoryMonthPlanProductionFinalResultEntityMapper factoryMonthPlanProductionFinalResultEntityMapper;

    /**
     * 下发月计划
     *
     * @param param 参数
     * @return 结果
     */
    @Override
    public AjaxResult issueMonthPlan(FactoryMonthPlanProdFinal param) {
        // 保证填写完整：年月、分厂、需求计划版本、分厂月计划版本
        if (param.getYear() == null || param.getMonth() == null || StringUtils.isBlank(param.getFactoryCode())
                || StringUtils.isBlank(param.getMonthPlanVersion()) || StringUtils.isBlank(param.getProductionVersion())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.finalized.checkParam"));
        }
        // 查询可发布的数据
        LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FactoryMonthPlanProductionFinalResult::getFactoryCode, param.getFactoryCode());
        wrapper.eq(FactoryMonthPlanProductionFinalResult::getYear, param.getYear());
        wrapper.eq(FactoryMonthPlanProductionFinalResult::getMonth, param.getMonth());
        wrapper.eq(FactoryMonthPlanProductionFinalResult::getMonthPlanVersion, param.getMonthPlanVersion());
        wrapper.eq(FactoryMonthPlanProductionFinalResult::getProductionVersion, param.getProductionVersion());
        wrapper.in(FactoryMonthPlanProductionFinalResult::getIsRelease,
                Arrays.asList(ReleaseStatusEnum.UN_RELEASE.getCode(), ReleaseStatusEnum.RELEASE_FAIL.getCode(),
                        ReleaseStatusEnum.TIME_OUT_FAIL.getCode(), ReleaseStatusEnum.WAIT_RELEASE.getCode()));
        List<FactoryMonthPlanProductionFinalResult> monthPlanProdFinalList = factoryMonthPlanProductionFinalResultEntityMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(monthPlanProdFinalList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.finalized.noData"));
        }
        // 更新发布状态=发布中
        LambdaUpdateWrapper<FactoryMonthPlanProductionFinalResult> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper
                .eq(FactoryMonthPlanProductionFinalResult::getYear, param.getYear())
                .eq(FactoryMonthPlanProductionFinalResult::getMonth, param.getMonth())
                .eq(FactoryMonthPlanProductionFinalResult::getMonthPlanVersion, param.getMonthPlanVersion())
                .eq(FactoryMonthPlanProductionFinalResult::getProductionVersion, param.getProductionVersion())
                .eq(FactoryMonthPlanProductionFinalResult::getFactoryCode, param.getFactoryCode())
                .set(FactoryMonthPlanProductionFinalResult::getIsRelease, ReleaseStatusEnum.RELEASING.getCode());
        factoryMonthPlanProductionFinalResultEntityMapper.update(null, updateWrapper);
        AjaxResult ajaxResult = mesItfService.issueMonthPlan(monthPlanProdFinalList);
        if (AjaxResultUtils.checkAjaxError(ajaxResult)) {
            throw new RuntimeException(String.valueOf(ajaxResult.get(AjaxResult.MSG_TAG)));
        }
        // 更新发布状态=已发布
        updateWrapper.clear();
        updateWrapper
                .eq(FactoryMonthPlanProductionFinalResult::getYear, param.getYear())
                .eq(FactoryMonthPlanProductionFinalResult::getMonth, param.getMonth())
                .eq(FactoryMonthPlanProductionFinalResult::getMonthPlanVersion, param.getMonthPlanVersion())
                .eq(FactoryMonthPlanProductionFinalResult::getProductionVersion, param.getProductionVersion())
                .eq(FactoryMonthPlanProductionFinalResult::getFactoryCode, param.getFactoryCode())
                .set(FactoryMonthPlanProductionFinalResult::getIsRelease, ReleaseStatusEnum.RELEASE.getCode());
        factoryMonthPlanProductionFinalResultEntityMapper.update(null, updateWrapper);
        return AjaxResult.success();
    }
}
