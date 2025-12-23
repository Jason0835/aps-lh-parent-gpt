package com.zlt.aps.monthplan.factory.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.reflect.ReflectUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.Constant;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.constant.IncrementConstant;
import com.tlt.aps.enums.ChannelRequirementTypeEnum;
import com.tlt.aps.enums.ConstructionStageEnum;
import com.tlt.aps.enums.ProductTypeEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.exception.BusinessException;
import com.tlt.aps.utils.BeanCopyUtils;
import com.tlt.aps.utils.GenerageMapKeyUtils;
import com.tlt.aps.utils.IncrementService;
import com.zlt.aps.maindata.domain.dto.MdmProductConstructionDto;
import com.zlt.aps.maindata.mapper.MdmMaterialInfoEntityMapper;
import com.zlt.aps.maindata.mapper.MdmModelInfoEntityMapper;
import com.zlt.aps.maindata.mapper.MdmProductConstructionEntityMapper;
import com.zlt.aps.maindata.mapper.MdmProductModelRelationEntityMapper;
import com.zlt.aps.maindata.service.IFactoryParamService;
import com.zlt.aps.maindata.utils.ScmListUtils;
import com.zlt.aps.monthplan.api.domain.dto.FactoryMonthPlanProdFinalQueryDto;
import com.zlt.aps.monthplan.api.domain.dto.TrialProductionPlanDto;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.aps.monthplan.api.domain.vo.*;
import com.zlt.aps.monthplan.api.enums.MonthPlanAdjustNoticeStatusEnum;
import com.zlt.aps.monthplan.demand.mapper.OrderPlanAllocationMapper;
import com.zlt.aps.monthplan.demand.mapper.SaleMonthPlanRequireStockMapper;
import com.zlt.aps.monthplan.demand.service.IOrderPlanAllocationService;
import com.zlt.aps.monthplan.factory.dto.ExcelDataAnalysisDto;
import com.zlt.aps.monthplan.factory.helper.*;
import com.zlt.aps.monthplan.factory.mapper.*;
import com.zlt.aps.monthplan.factory.service.*;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zlt.aps.monthplan.factory.helper.MonthPlanSpecificationHelper.SERIAL_NUMBER_FORMAT;
import static com.zlt.common.utils.ImportExcelValidatedUtils.addImportErrorLog;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FactoryMonthPlanProductionFinalServiceImpl.java
 * 描    述：FactoryMonthPlanProductionFinalServiceImpl分厂月生产计划排产结果-生产计划排产结果-sku 业务层处理
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-09-22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FactoryMonthPlanProductionFinalServiceImpl extends ServiceImpl<FactoryMonthPlanProductionFinalMapper, MonthPlanProductionFinalResult> implements IFactoryMonthPlanProductionFinalService {

    private final MdmStockUpPlanMapper stockUpPlanMapper;

    private final MdmModelInfoEntityMapper modelInfoEntityMapper;

    private final OrderPlanAllocationMapper orderPlanAllocationMapper;

    private final FactoryProductionVersionMapper factoryProductionVersionMapper;

    private final MonthPlanProdDetailFinalMapper monthPlanProdDetailFinalMapper;

    private final MdmProductConstructionEntityMapper mdmProductConstructionMapper;

    private final SaleMonthPlanRequireStockMapper saleMonthPlanRequireStockMapper;

    private final MdmProductModelRelationEntityMapper productModelRelationEntityMapper;

    private final FactoryMonthPlanProductionFinalMapper factoryMonthPlanProductionFinalMapper;

    private final MonthPlanProductionDayResultMapper monthPlanProductionDayResultMapper;

    private final MdmMaterialInfoEntityMapper productInfoEntityMapper;

    private final BaseDao baseDao;

    private final IncrementService incrementService;

    private final IFactoryParamService factoryParamService;

    private final IFactoryProductionVersionService factoryProductionVersionService;

    private final IMonthPlanNoProductionPlanService iMonthPlanNoProductionPlanService;

    private final IOrderPlanAllocationService iOrderPlanAllocationService;

    private final IMonthPlanSurplusService monthPlanSurplusService;

    private final IFactoryMonthPlanProductionFinalExcelService factoryMonthPlanProductionFinalExcelService;

    private final String ERROR_MAX_DAY = "monthMaxDay";

    private final String ERROR_TOTAL_QTY = "totalQtyCheck";

    private final String ERROR_SAME_EMPTY = "sameEmpty";

    // 添加线程池注入
    private final Executor queryExecutor;

    /**
     * 将排产版本数据转化成定稿数据
     * 一条SKU，一条记录
     *
     * @param param
     */
    @Override
    public void saveFinalizedData(FactoryMonthPlanProdFinal param) {
        if (null == param) {
            return;
        }
        //年、月、分厂、需求计划版本、分厂月计划版本
        if (param.getYear() == null || param.getMonth() == null || StringUtils.isBlank(param.getFactoryCode()) || StringUtils.isBlank(param.getMonthPlanVersion()) || StringUtils.isBlank(param.getProductionVersion())) {
            return;
        }
        //根据排产版本-查询排产信息
        List<MonthPlanProductionDayResult> versionDataList = getProductionVersionData(param);
        if (CollectionUtils.isEmpty(versionDataList)) {
            return;
        }
        //获取需求及分配信息
        Map<String, MonthPlanProductionRequirementHelper> demandInfoMap = getOrderAndAllocationInfo(param);
        //获取库存信息
        Map<String, MonthPlanRequireStock> requirementStockMap = getStockInfoByRequirement(param);
        //备货信息
        Map<String, MonthPlanProductionStockUpRequirementHelper> stockUpGroupMap = getStockUpInfo(param);
        List<MonthPlanProductionFinalResult> finalResultDataList = buildFinalData(versionDataList, demandInfoMap, requirementStockMap, stockUpGroupMap);
        if (CollectionUtils.isEmpty(finalResultDataList)) {
            return;
        }
        //设置共用模具信息
        setShareMouldInfo(finalResultDataList);
        //补充排产单号
        String productionNoPrefix = getProductionNoPrefix();
        String productionNoFormat = "%s%s";
        String serialNumberFormat = "%06d";
        int index = 1;
        for (MonthPlanProductionFinalResult finalData : finalResultDataList) {
            String serialNumber = String.format(serialNumberFormat, index);
            String productionNo = String.format(productionNoFormat, productionNoPrefix, serialNumber);
            finalData.setProductionNo(productionNo);
            finalData.setId(null);
            finalData.setBaseVale(null);
            index = index + 1;
        }
        saveBatch(finalResultDataList);
    }

    @Override
    public List<MonthPlanProductionFinalResult> getList(Wrapper<MonthPlanProductionFinalResult> queryWrapper, boolean isHandler) {
        if (!isHandler) {
            return getBaseMapper().selectList(queryWrapper);
        }
        List<MonthPlanProductionFinalResult> resultData = getBaseMapper().selectList(queryWrapper);
        if (CollectionUtils.isEmpty(resultData)) {
            return resultData;
        }
        String productionVersion = resultData.get(0).getProductionVersion();
        if (StringUtils.isBlank(productionVersion)) {
            return resultData;
        }
        FactoryProductionVersion version = factoryProductionVersionService.getProductionVersion(productionVersion);
        ProductionPlanExcelUtils.handlerBeginAndEndDayBySku(version, resultData);
        MonthPlanProductionFinalUtils.dealList(resultData);
        return resultData;
    }

    @Override
    public MonthPlanStatisticsVo statistics(MonthPlanProductionFinalResult queryVO) {
        QueryWrapper<MonthPlanProductionFinalResult> queryWrapper = new QueryWrapper<>();
        //构建查询条件
        MonthPlanProductionFinalUtils.builderCondition(queryWrapper, queryVO);

        MonthPlanStatisticsVo statisticsVo = new MonthPlanStatisticsVo();

        // 查询排产SAP个数、已排SAP总量
        queryWrapper.select("count(distinct PRODUCT_CODE) as productionCount,sum(TOTAL_QTY) as productionSum");
        List<Map<String, Object>> mapList = getBaseMapper().selectMaps(queryWrapper);
        if (!CollectionUtils.isEmpty(mapList)) {
            Map<String, Object> resultMap = mapList.get(0);
            if (resultMap != null && resultMap.get("productionCount") != null) {
                statisticsVo.setProductionCount(Long.parseLong(resultMap.get("productionCount").toString()));
            }
            if (resultMap != null && resultMap.get("productionSum") != null) {
                statisticsVo.setProductionSum(Long.parseLong(resultMap.get("productionSum").toString()));
            }
        }
        // 根据年月、分厂查询首条定稿记录
        QueryWrapper<MonthPlanProductionFinalResult> firstWrapper = new QueryWrapper<>();
        firstWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        firstWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("year")), "YEAR", queryVO.getFieldValueByFieldName("year"));
        firstWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month")), "MONTH", queryVO.getFieldValueByFieldName("month"));
        firstWrapper.last("limit 1");
        List<MonthPlanProductionFinalResult> finalList = getBaseMapper().selectList(firstWrapper);
        if (CollectionUtils.isEmpty(finalList)) {
            return statisticsVo;
        }
        MonthPlanProductionFinalResult prodFinal = finalList.get(0);

        // 统计备货量
        if (StringUtils.isNotBlank(prodFinal.getProductionVersion())) {
            QueryWrapper<MonthPlanProdDetailFinal> detailWrapper = new QueryWrapper<>();
            detailWrapper.select("sum(TOTAL_QTY) as stockNum");
            MonthPlanProductionFinalUtils.builderCondition(detailWrapper, queryVO);
            detailWrapper.eq("PRODUCTION_VERSION", prodFinal.getProductionVersion());
            detailWrapper.eq("IS_STOCK_UP", Constant.TRUE);
            List<Map<String, Object>> detailMapList = monthPlanProdDetailFinalMapper.selectMaps(detailWrapper);
            if (!CollectionUtils.isEmpty(detailMapList)) {
                Map<String, Object> resultMap = detailMapList.get(0);
                if (resultMap != null && resultMap.get("stockNum") != null) {
                    statisticsVo.setStockNum(Long.parseLong(resultMap.get("stockNum").toString()));
                }
            }
        }
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

    @Override
    public List<DayProductionTotalVo> statisticsDay(MonthPlanProductionFinalResult query) {
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
        List<DayProductionTotalVo> dayTotalList = getBaseMapper().getStatisticsDay(productionVersion, days);
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
    public AjaxResult importTrialProductionPlan(List<TrialProductionPlanDto> list, long importLogId) {
        AjaxResult beforeCheckResult = beforeAnalysisCheck(list);
        if (!MonthPlanProductionFinalUtils.isPassCheck(beforeCheckResult)) {
            return beforeCheckResult;
        }
        FactoryProductionVersion productionVersion = (FactoryProductionVersion) beforeCheckResult.get(AjaxResult.DATA_TAG);
        String factoryCode = productionVersion.getFactoryCode();
        Integer curingTime = factoryParamService.getInformalConstructionCuringTime(factoryCode);
        //根据版本信息，调整起始日，开始日及day排产量的值
        ProductionPlanExcelUtils.handlerTrialProductionPlanDayQty(productionVersion, list);
        //业务数据解析
        Map<String, MdmProductConstruction> productConstructionMap = new HashMap<>();
        List<MonthPlanProductionFinalResult> informalConstructionStageList = new ArrayList<>();
        ExcelDataAnalysisDto analysisHelper = new ExcelDataAnalysisDto(importLogId);
        List<MonthPlanProductionFinalResult> importList = analysisData(list, analysisHelper, curingTime, productionVersion, productConstructionMap, informalConstructionStageList);
        //保存SAP与施工关系
        if (!CollectionUtils.isEmpty(informalConstructionStageList)) {
            saveMdmProductConstructionInfo(informalConstructionStageList, productConstructionMap, curingTime);
        }
        //保存业务数据
        List<ImportErrorLog> importErrorLogs = analysisHelper.getImportErrorLogs();
        int successNum;
        int failureNum = analysisHelper.getFailureNumber();
        if (!CollectionUtils.isEmpty(importList)) {
            //补充物料信息
            fullProductInfo(importList);
            try {
                successNum = importList.size();
                //保存试制量试计划，并更新月度剩余量
                factoryMonthPlanProductionFinalExcelService.saveImportTrialProductionPlan(importList);
            } catch (Exception e) {
                log.error("试制量试计划-导入异常", e);
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

    @Override
    public AjaxResult importAdjustPlan(List<MonthPlanProductionFinalResultVo> excelData, long importLogId) {
        List<MonthPlanProductionFinalResultVo> list = Lists.newArrayList();
        excelData.forEach(item -> {
            item.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
            list.add(item);
        });
        AjaxResult beforeCheckResult = beforeAnalysisAdjustCheck(list);
        if (!MonthPlanProductionFinalUtils.isPassCheck(beforeCheckResult)) {
            return beforeCheckResult;
        }
        FactoryProductionVersion productionVersion = (FactoryProductionVersion) beforeCheckResult.get(AjaxResult.DATA_TAG);
        //根据版本信息，调整起始日，开始日及day排产量的值
        ProductionPlanExcelUtils.handlerAdjustPlanDayQty(productionVersion, list);
        // 解析数据
        ExcelDataAnalysisDto analysisHelper = new ExcelDataAnalysisDto(importLogId);
        ProductionPlanExcelImportHelper excelHelper = analysisAdjustPlanData(list, analysisHelper, productionVersion);
        //保存业务数据
        List<ImportErrorLog> importErrorLogs = analysisHelper.getImportErrorLogs();
        int successNum = 0;
        int failureNum = analysisHelper.getFailureNumber();
        int configureMissNumber = analysisHelper.getConfigureMissNumber();
        if (!excelHelper.isImportDataEmpty()) {
            try {
                List<MonthPlanProductionFinalResult> insertList = excelHelper.getInsertList();
                //保存调整计划，并更新月度剩余量
                successNum = factoryMonthPlanProductionFinalExcelService.saveImportAdjustPlan(excelHelper, insertList, successNum);
            } catch (Exception e) {
                log.error("导入异常", e);
                successNum = 0;
                failureNum = list.size();
                importErrorLogs.clear();
                addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
            }
        }
        //返回提示信息及错误集合
        if (failureNum > 0 || configureMissNumber > 0) {
            return AjaxResult.error(String.format(I18nUtil.getMessage("ui.message.import.adjustPlan.fail") + "," + successNum + "," + failureNum + "," + configureMissNumber), importErrorLogs);
        }
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
    }

    /**
     * String、Integer、Long
     */
    private static final List<String> CHECK_NULL_FIELD_NAME = Arrays.asList("productCode", "mouldMethod", "specCode", "embryoCode", "isTrialProductionPlan",
            "mouldNo", "mouldQty", "orderQty", "prodReqPlan", "netDemandQty", "stockUpDemandQty", "factProdReqQty", "totalQty");

    private Map<String, String> buildAdjustPlanErrorInfo() {
        Map<String, String> resultMap = new HashMap<>(16);
        resultMap.put("productCode", I18nUtil.getMessage("ui.data.column.monthPlanProductionFinalResult.productCode"));
        resultMap.put("mouldMethod", I18nUtil.getMessage("ui.data.column.monthPlanProductionFinalResult.mouldMethod"));
        resultMap.put("specCode", I18nUtil.getMessage("ui.data.column.monthPlanProductionFinalResult.specCode"));
        resultMap.put("embryoCode", I18nUtil.getMessage("ui.data.column.monthPlanProductionFinalResult.embryoCode"));
        resultMap.put("isTrialProductionPlan", I18nUtil.getMessage("ui.data.column.monthPlanProductionFinalResult.isTrialProductionPlan"));
        resultMap.put("mouldNo", I18nUtil.getMessage("ui.data.column.monthPlanProductionFinalResult.mouldNo"));
        resultMap.put("mouldQty", I18nUtil.getMessage("ui.data.column.monthPlanProductionFinalResult.mouldQty"));
        resultMap.put("orderQty", I18nUtil.getMessage("ui.data.column.monthPlanProductionFinalResult.orderQty"));
        resultMap.put("prodReqPlan", I18nUtil.getMessage("ui.data.column.monthPlanProductionFinalResult.prodReqPlan"));
        resultMap.put("netDemandQty", I18nUtil.getMessage("ui.data.column.monthPlanProductionFinalResult.netDemandQty"));
        resultMap.put("stockUpDemandQty", I18nUtil.getMessage("ui.data.column.monthPlanProductionFinalResult.stockUpDemandQty"));
        resultMap.put("factProdReqQty", I18nUtil.getMessage("ui.data.column.monthPlanProductionFinalResult.factProdReqQty"));
        resultMap.put("totalQty", I18nUtil.getMessage("ui.data.column.monthPlanProductionFinalResult.totalQty"));
        return resultMap;
    }

    /**
     * 导入解析数据
     *
     * @param excelData         导入excel解析数据
     * @param analysisHelper    解析数据
     * @param productionVersion 排产版本
     * @return 结果
     */
    private ProductionPlanExcelImportHelper analysisAdjustPlanData(List<MonthPlanProductionFinalResultVo> excelData, ExcelDataAnalysisDto analysisHelper, FactoryProductionVersion productionVersion) {
        Map<String, String> fieldNameErrorInfo = buildAdjustPlanErrorInfo();
        Integer failureNumber = analysisHelper.getFailureNumber();
        Integer configureMissNumber = analysisHelper.getConfigureMissNumber();
        ProductionPlanExcelImportHelper importHelper = new ProductionPlanExcelImportHelper();
        List<MonthPlanProductionFinalResult> insertList = new ArrayList<>();
        List<MonthPlanProductionFinalResult> updateList = new ArrayList<>();

        List<List<MonthPlanProductionFinalResultVo>> splitList = ScmListUtils.getSplitList(excelData, 1000);

        //补充排产单号
        String productionNoPrefix = getProductionNoPrefix();
        String productionNoFormat = "%s%s";
        String serialNumberFormat = "%06d";
        int index = 1;

        Integer year = productionVersion.getYear();
        Integer month = productionVersion.getMonth();
        String factoryCode = productionVersion.getFactoryCode();
        String monthPlanVersion = productionVersion.getMonthPlanVersion();
        String finalProductionVersion = productionVersion.getProductionVersion();
        Integer yearAndMonth = Integer.valueOf(String.format("%s%02d", year, month));

        for (List<MonthPlanProductionFinalResultVo> resultVos : splitList) {
            List<String> productCodeList = resultVos.stream().map(MonthPlanProductionFinalResult::getProductCode).distinct().collect(Collectors.toList());
            Map<String, MdmProductConstruction> productConstructionMap = new HashMap<>(16);
            Map<String, List<MdmSkuMouldRel>> modelRelationMap = new HashMap<>(16);
            Map<String, MdmMaterialInfo> productInfoMap = new HashMap<>(16);
            if (!CollectionUtils.isEmpty(productCodeList)) {
                // 查询SAP与施工关系
                LambdaQueryWrapper<MdmProductConstruction> consWrapper = new LambdaQueryWrapper<>();
                consWrapper.in(MdmProductConstruction::getProductCode, productCodeList);
                List<MdmProductConstruction> productConstructionList = mdmProductConstructionMapper.selectList(consWrapper);
                if (!CollectionUtils.isEmpty(productConstructionList)) {
                    productConstructionMap = productConstructionList.stream().collect(Collectors.toMap(item -> GenerageMapKeyUtils.createMapKey(item.getProductCode(), item.getSpecCode(), item.getEmbryoCode()), Function.identity()));
                }
                // 查询SKU与模具关系
                List<MdmSkuMouldRel> modelRelationList = productModelRelationEntityMapper.select4ImportAdjustData(resultVos);
                if (!CollectionUtils.isEmpty(modelRelationList)) {
                    modelRelationMap = modelRelationList.stream().collect(Collectors.groupingBy(item -> GenerageMapKeyUtils.createMapKey(item.getMaterialCode(), item.getMouldNo())));
                }
                // 查询物料信息，用于校验、回填信息
                LambdaQueryWrapper<MdmMaterialInfo> productInfoWrapper = new LambdaQueryWrapper<>();
                productInfoWrapper.in(MdmMaterialInfo::getMaterialCode, productCodeList);
                List<MdmMaterialInfo> productInfoList = productInfoEntityMapper.selectList(productInfoWrapper);
                if (!CollectionUtils.isEmpty(productInfoList)) {
                    productInfoMap = productInfoList.stream().collect(Collectors.toMap(MdmMaterialInfo::getMaterialCode, Function.identity()));
                }
            }

            String productCodeNotExistMsg = I18nUtil.getMessage("ui.data.column.monthPlanMouldingDayResult.productCode.notExist");
            String requiredMsg = I18nUtil.getMessage("import.validated.required");

            List<ImportErrorLog> allImportErrorLogs = analysisHelper.getImportErrorLogs();

            for (int i = 0, excelDataSize = resultVos.size(); i < excelDataSize; i++) {
                boolean configureMissFlag = Boolean.FALSE;
                int errorNum = i + 2;
                MonthPlanProductionFinalResultVo excelVo = resultVos.get(i);
                Integer isTrialProductionPlan = excelVo.getIsTrialProductionPlan();
                // 校验SAP，非试产试制才校验必填
                List<ImportErrorLog> importErrorLogs = new ArrayList<>();
                for (String fieldName : CHECK_NULL_FIELD_NAME) {
                    // 试制量试计划，不校验SAP代码
                    if (YesOrNoEnum.YES.getValue().equals(isTrialProductionPlan) && "productCode".equals(fieldName)) {
                        continue;
                    }
                    Object fieldValue = ReflectUtils.getFieldValue(excelVo, fieldName);
                    String fieldValueStr = fieldValue == null ? "" : String.valueOf(fieldValue);
                    if (StringUtils.isBlank(fieldValueStr)) {
                        addImportErrorLog(analysisHelper.getImportLogId(), errorNum, String.format(requiredMsg, errorNum, fieldNameErrorInfo.get(fieldName)), importErrorLogs);
                    }
                }
                String productCode = excelVo.getProductCode();
                if (!productInfoMap.containsKey(productCode)) {
                    addImportErrorLog(analysisHelper.getImportLogId(), errorNum, productCodeNotExistMsg, importErrorLogs);
                } else {
                    MdmMaterialInfo productInfo = productInfoMap.get(productCode);
                    excelVo.setProductDesc(productInfo.getMaterialDesc());
                    excelVo.setBrand(productInfo.getBrand());
                    // excelVo.setProSize(productInfo.getProSize());
                    excelVo.setSpecifications(productInfo.getSpecifications());
                    excelVo.setPattern(productInfo.getPattern());
                    excelVo.setHierarchy(productInfo.getHierarchy());
                    excelVo.setProductTypeName(productInfo.getProductTypeName());
                    excelVo.setProductTypeCode(productInfo.getProductTypeCode());
                }
                if (!CollectionUtils.isEmpty(importErrorLogs)) {
                    failureNumber++;
                    allImportErrorLogs.addAll(importErrorLogs);
                    continue;
                }
                // 校验SAP与施工，不通过，数据添加到导入列表，仅提示
                String consMapKey = GenerageMapKeyUtils.createMapKey(productCode, excelVo.getSpecCode(), excelVo.getEmbryoCode());
                if (!productConstructionMap.containsKey(consMapKey)) {
                    configureMissFlag = Boolean.TRUE;
                    addImportErrorLog(analysisHelper.getImportLogId(), errorNum,
                            I18nUtil.getMessage("ui.data.column.monthPlanMouldingDayResult.constructionReal.notExist"), importErrorLogs);
                }
                // 校验SAP与模具，不通过，数据添加到导入列表，仅提示
                String modelMapKey = GenerageMapKeyUtils.createMapKey(productCode, excelVo.getMouldNo());
                if (modelRelationMap.containsKey(modelMapKey)) {
                    List<MdmSkuMouldRel> modelRelationList = modelRelationMap.get(modelMapKey);
                    long count = modelRelationList.stream().map(MdmSkuMouldRel::getMouldCode).distinct().count();
                    if (count < excelVo.getMouldQty()) {
                        configureMissFlag = Boolean.TRUE;
                        addImportErrorLog(analysisHelper.getImportLogId(), errorNum,
                                I18nUtil.getMessage("ui.data.column.monthPlanMouldingDayResult.modelReal.notExist"), importErrorLogs);
                    }
                } else {
                    configureMissFlag = Boolean.TRUE;
                    addImportErrorLog(analysisHelper.getImportLogId(), errorNum,
                            I18nUtil.getMessage("ui.data.column.monthPlanMouldingDayResult.modelReal.notExist"), importErrorLogs);
                }
                if (configureMissFlag) {
                    configureMissNumber++;
                }
                // 将数据转换回JSON格式存储
                MonthPlanProductionFinalUtils.convertLocationInfo(excelVo);
                MonthPlanProductionFinalUtils.convertChannelInfo(excelVo);

                //版本信息
                excelVo.setFactoryCode(factoryCode);
                excelVo.setYear(year);
                excelVo.setMonth(month);
                excelVo.setYearMonth(yearAndMonth);
                excelVo.setMonthPlanVersion(monthPlanVersion);
                excelVo.setProductionVersion(finalProductionVersion);

                excelVo.setIsImport(YesOrNoEnum.YES.getValue());
                MonthPlanProductionFinalResult result = new MonthPlanProductionFinalResult();
                BeanUtils.copyProperties(excelVo, result);
                result.setBaseVale(null);
                if (StringUtils.isNotBlank(result.getProductionNo())) {
                    updateList.add(result);
                } else {
                    String serialNumber = String.format(serialNumberFormat, index);
                    String productionNo = String.format(productionNoFormat, productionNoPrefix, serialNumber);
                    result.setProductionNo(productionNo);
                    index = index + 1;
                    insertList.add(result);
                }
                allImportErrorLogs.addAll(importErrorLogs);
            }
        }
        analysisHelper.setFailureNumber(failureNumber);
        analysisHelper.setConfigureMissNumber(configureMissNumber);
        importHelper.setInsertList(insertList);
        importHelper.setUpdateList(updateList);
        return importHelper;
    }

    @Override
    public List<FactoryMonthPlanDayProductionInfoVo> getMonthPlanDayProductionInfo(FactoryMonthPlanProdFinalQueryDto queryCondition) {
        Date productionDate = queryCondition.getProductionDate();
        //根据分厂，及日期确定排产版本计划
        FactoryProductionVersion finalVersion = factoryProductionVersionService.getFinalVersion(queryCondition.getFactoryCode(), productionDate);
        if (null == finalVersion) {
            return Collections.emptyList();
        }
        queryCondition.setYear(finalVersion.getYear());
        queryCondition.setMonth(finalVersion.getMonth());
        queryCondition.setMonthPlanVersion(finalVersion.getMonthPlanVersion());
        queryCondition.setProductionVersion(finalVersion.getProductionVersion());
        QueryWrapper<MonthPlanProductionFinalResult> queryWrapper = new QueryWrapper();
        MonthPlanProductionFinalUtils.builderCondition(queryWrapper, queryCondition);
        List<MonthPlanProductionFinalResult> dataList = getList(queryWrapper, false);
        if (org.apache.commons.collections4.CollectionUtils.isEmpty(dataList)) {
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
    public List<FactoryMonthPlanProdFinalVo> getProdResult(FactoryMonthPlanProdFinalQueryDto queryCondition) {
        QueryWrapper<MonthPlanProductionFinalResult> queryWrapper = new QueryWrapper();
        MonthPlanProductionFinalUtils.builderCondition(queryWrapper, queryCondition);
        List<MonthPlanProductionFinalResult> dataList = getList(queryWrapper, false);
        if (CollectionUtils.isEmpty(dataList)) {
            return Collections.emptyList();
        }
        List<FactoryMonthPlanProdFinalVo> resultList = new ArrayList<>(dataList.size());
        dataList.stream().forEach(result -> {
            FactoryMonthPlanProdFinalVo prodFinal = new FactoryMonthPlanProdFinalVo();
            BeanUtils.copyProperties(result, prodFinal);
            resultList.add(prodFinal);
        });
        return resultList;
    }

    @Override
    public List<FactoryMonthPlanProdFinalVo> getMonthPlanProdResult(FactoryMonthPlanProdFinalQueryDto queryCondition) {
        if (null == queryCondition || StringUtils.isBlank(queryCondition.getFactoryCode()) || null == queryCondition.getProductionDate()) {
            return Collections.emptyList();
        }
        FactoryProductionVersion finalVersion = factoryProductionVersionService.getFinalVersion(queryCondition.getFactoryCode(), queryCondition.getProductionDate());
        if (null == finalVersion) {
            return Collections.emptyList();
        }
        queryCondition.setYear(finalVersion.getYear());
        queryCondition.setMonth(finalVersion.getMonth());
        QueryWrapper<MonthPlanProductionFinalResult> queryWrapper = new QueryWrapper();
        MonthPlanProductionFinalUtils.builderCondition(queryWrapper, queryCondition);
        List<MonthPlanProductionFinalResult> dataList = getList(queryWrapper, false);
        if (CollectionUtils.isEmpty(dataList)) {
            return Collections.emptyList();
        }
        Date productionStartDate = finalVersion.getProductionStartDate();
        Date productionEndDate = finalVersion.getProductionEndDate();
        Integer startDays = com.zlt.aps.factory.utils.DateUtils.getDaysByMonth(productionStartDate);
        Integer maxDays = com.zlt.aps.factory.utils.DateUtils.getMaxDaysByMonth(productionStartDate);
        Integer addDays = maxDays - startDays;
        List<FactoryMonthPlanProdFinalVo> resultList = new ArrayList<>(dataList.size());
        dataList.stream().forEach(result -> {
            FactoryMonthPlanProdFinalVo prodFinal = new FactoryMonthPlanProdFinalVo();
            BeanUtils.copyProperties(result, prodFinal);
            prodFinal.setProductionStartDate(productionStartDate);
            prodFinal.setProductionEndDate(productionEndDate);
            if (YesOrNoEnum.YES.getValue().equals(finalVersion.getIsNaturalMonth())) {
                prodFinal.setAddDays(BigDecimal.ZERO.intValue());
            } else {
                prodFinal.setAddDays(addDays);
            }
            resultList.add(prodFinal);
        });
        return resultList;
    }

    @Override
    public MonthPlanProductionFinalResult linkProductInfoByProductCode(MonthPlanProductionFinalResult param) {
        // 参数校验
        validateParam(param);

        // 使用CompletableFuture并行处理独立查询
        CompletableFuture<List<MdmProductConstruction>> productConstructionFuture =
                getProductConstructionAsync(param.getProductCode());
        CompletableFuture<ProductModelRelationResult> modelRelationFuture =
                getProductModelRelationAsync(param.getProductCode());
        CompletableFuture<Optional<FactoryMonthPlanFinalVersionInfoVo>> finalVersionFuture =
                getFinalVersionInfoAsync(param.getFactoryCode(), param.getYear(), param.getMonth());
        CompletableFuture<List<MdmStockUpPlan>> stockUpPlansFuture =
                getStockUpPlansAsync(param);

        // 等待所有异步操作完成
        CompletableFuture.allOf(productConstructionFuture, modelRelationFuture,
                finalVersionFuture, stockUpPlansFuture).join();

        try {
            MonthPlanProductionFinalResult result = new MonthPlanProductionFinalResult();
            result.setProductCode(param.getProductCode());

            // 处理产品结构信息
            processProductConstructionInfo(productConstructionFuture.get(), result);

            // 处理模型关系信息
            processModelRelationInfo(modelRelationFuture.get(), result);

            // 处理库存信息
            processStockInfo(finalVersionFuture.get(), param, result);

            // 处理备货计划
            processStockUpPlans(stockUpPlansFuture.get(), result);

            return result;
        } catch (Exception e) {
            throw new BusinessException("数据查询处理失败", e);
        }
    }

    @Override
    public MonthPlanProductionFinalResult calculateByOrderQty(MonthPlanProductionFinalResult param) {
        validateCalculationParams(param);
        // 并行获取版本信息和海外品牌
        CompletableFuture<Optional<FactoryMonthPlanFinalVersionInfoVo>> versionFuture =
                CompletableFuture.supplyAsync(() ->
                        Optional.ofNullable(getFinalVersionInfo(param.getFactoryCode(), param.getYear(), param.getMonth())));

        CompletableFuture<Set<String>> foreignBrandFuture =
                CompletableFuture.supplyAsync(() ->
                        factoryParamService.getForeignOemBrand(param.getFactoryCode()));

        try {
            Optional<FactoryMonthPlanFinalVersionInfoVo> finalVersion = versionFuture.get(5, TimeUnit.SECONDS);
            Set<String> foreignOemBrandSet = foreignBrandFuture.get(5, TimeUnit.SECONDS);
            MonthPlanProductionRequirementHelper helper = null;
            if (finalVersion.isPresent()) {
                List<OrderPlanAllocation> allocations = queryOrderPlanAllocations(param, finalVersion.get());
                helper = build(allocations, foreignOemBrandSet);
            }
            if (null != helper) {
                applyRequirementHelperResult(helper, param);
                return param;
            } else {
                param.setAllocationQty(0L);
                param.setNetDemandQty(param.getOrderQty());
                param.setTotalQty(param.getOrderQty());
            }
            return param;
        } catch (Exception e) {
            log.warn("并行计算失败，使用同步降级方案", e);
            param.setAllocationQty(0L);
            param.setNetDemandQty(param.getOrderQty());
            param.setTotalQty(param.getOrderQty());
            return param;
        }
    }

    @Override
    public void addSpecifications(MonthPlanProductionFinalResult param) {
        // 参数校验
        validateParam(param);
        if (param.getOrderQty() == null || param.getOrderQty() <= 0) {
            throw new BusinessException("订单数量不为空且必须大于0");
        }
        if (null == param.getConstruction()) {
            throw new BusinessException("规格代号不能为空");
        }
        param.setSpecCode(param.getConstruction().getSpecCode());
        param.setConstructionCode(param.getConstruction().getConstructionCode());
        param.setEmbryoCode(param.getConstruction().getEmbryoCode());
        // 获取并验证定稿版本
        FactoryMonthPlanFinalVersionInfoVo finalVersion = getAndValidateFinalVersion(param);

        // 检查规格是否已存在
        checkSpecificationExists(param, finalVersion);

        // 处理库存对冲
        processStockOffset(param, finalVersion);

        // 处理剩余需求
        processRemainingDemand(param, finalVersion);
    }

    @Override
    public void editPlan(MonthPlanProductionFinalResult param) {
        long count = 0;
        // 获取并验证定稿版本
        MonthPlanProductionFinalResult finalData = getById(param.getId());
        long totalValue = 0;
        for (int day = FactoryConstant.MONTH_START_DAY; day <= FactoryConstant.MONTH_MAX_DAY; day++) {
            String fieldName = String.format("day%d", day);
            Object productionQty = param.getFieldValueByFieldName(fieldName);
            if (null == productionQty) {
                continue;
            }
            long value = Long.parseLong(productionQty.toString());
            if (value < 0 || value > 99999) {
                count += 1;
                break;
            }
            finalData.setFieldValueByFieldName(fieldName, value);
            totalValue += value;
        }
        if (count > 0) {
            throw new BusinessException("调整量范围只能在0到99999,请重新输入!");
        }
        finalData.setTotalQty(totalValue);
        long netDemandQty = null == finalData.getNetDemandQty() ? 0 : finalData.getNetDemandQty();
        finalData.setDifferenceQty(netDemandQty - totalValue);
        updateById(finalData);
        //重新汇总对应月度外胎汇总
        monthPlanSurplusService.finalUpdatePlanSurplusList(Lists.newArrayList(finalData));
    }

    @Override
    public void subtractSpecification(MonthPlanProductionFinalResult param) {
        MonthPlanProductionFinalResult finalData = getById(param.getId());
        MonthPlanAdjustInfoVo adjustControlInfo = this.getAdjustControlInfo(finalData);
        if (-1 == adjustControlInfo.getStartAdjustDay()) {
            return;
        }
        int startAdjustDay = adjustControlInfo.getStartAdjustDay();
        long totalValue = 0;
        for (int day = FactoryConstant.MONTH_START_DAY; day < startAdjustDay; day++) {
            String fieldName = String.format("day%d", day);
            Object productionQty = finalData.getFieldValueByFieldName(fieldName);
            if (null == productionQty) {
                continue;
            }
            totalValue += Long.parseLong(productionQty.toString());
        }
        for (int day = startAdjustDay; day <= FactoryConstant.MONTH_MAX_DAY; day++) {
            String fieldName = String.format("day%d", day);
            finalData.setFieldValueByFieldName(fieldName, 0);
        }
        finalData.setTotalQty(totalValue);
        finalData.setDifferenceQty(finalData.getNetDemandQty() - finalData.getTotalQty());
        updateById(finalData);
        //重新汇总对应月度外胎汇总
        monthPlanSurplusService.finalUpdatePlanSurplusList(Lists.newArrayList(finalData));
    }


    private MonthPlanAdjustInfoVo getAdjustControlInfo(MonthPlanProductionFinalResult query) {
        MonthPlanAdjustInfoVo adjust = new MonthPlanAdjustInfoVo();
        FactoryMonthPlanFinalVersionInfoVo finalVersion = getFinalVersionInfo(query.getFactoryCode(), query.getYear(), query.getMonth());
        if (null == finalVersion) {
            adjust.setStartAdjustDay(-1);
            return adjust;
        }
        Date currentDate = new Date();
        String dayFormat = com.ruoyi.common.core.utils.DateUtils.YYYY_MM_DD;
        String currentDateFormat = com.ruoyi.common.core.utils.DateUtils.parseDateToStr(dayFormat, currentDate);
        Date matchDate = com.ruoyi.common.core.utils.DateUtils.dateTime(dayFormat, currentDateFormat);
        Integer delayDays = getAdjustDelayDays(query.getFactoryCode(), ProductTypeEnum.SEMI_STEEL.getValue());
        Date adjustStartDate = com.ruoyi.common.core.utils.DateUtils.addDays(matchDate, delayDays);
        Date productionStartDate = finalVersion.getProductionStartDate();
        //版本开始日 <= 调整日期 <= 版本结束日
        if (productionStartDate.compareTo(adjustStartDate) >= 0 && finalVersion.getProductionEndDate().compareTo(adjustStartDate) <= 0) {
            adjust.setStartAdjustDay(-1);
            return adjust;
        }
        adjust.setFinalVersionInfo(finalVersion);
        adjust.setOperateDate(currentDate);
        //设置可调整的起始天数
        adjust.setStartAdjustDate(adjustStartDate);
        int diffDays = com.ruoyi.common.core.utils.DateUtils.getDayInterval(adjustStartDate, productionStartDate);
        adjust.setStartAdjustDay(diffDays + BigDecimal.ONE.intValue());
        return adjust;
    }


    private FactoryMonthPlanFinalVersionInfoVo getAndValidateFinalVersion(MonthPlanProductionFinalResult param) {
        FactoryMonthPlanFinalVersionInfoVo finalVersion = getFinalVersionInfo(
                param.getFactoryCode(), param.getYear(), param.getMonth());
        if (finalVersion == null) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.column.addSpecifications.checkFinal"));
        }
        return finalVersion;
    }

    private void checkSpecificationExists(MonthPlanProductionFinalResult param,
                                          FactoryMonthPlanFinalVersionInfoVo finalVersion) {
        LambdaQueryWrapper<MonthPlanProductionFinalResult> queryWrapper =
                MonthPlanSpecificationHelper.buildFinalResultQuery(
                                param.getFactoryCode(), param.getYear(), param.getMonth(),
                                finalVersion.getMonthPlanVersion(), finalVersion.getProductionVersion())
                        .eq(MonthPlanProductionFinalResult::getProductCode, param.getProductCode());

        List<MonthPlanProductionFinalResult> planProductionFinalResults = list(queryWrapper);
        if (!CollectionUtils.isEmpty(planProductionFinalResults)) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.column.addSpecifications.checkExist"));
        }
    }

    private void processStockOffset(MonthPlanProductionFinalResult param,
                                    FactoryMonthPlanFinalVersionInfoVo finalVersion) {
        LambdaQueryWrapper<MonthPlanRequireStock> stockQuery = new LambdaQueryWrapper<MonthPlanRequireStock>()
                .eq(MonthPlanRequireStock::getFactoryCode, param.getFactoryCode())
                .eq(MonthPlanRequireStock::getMonthPlanVersion, finalVersion.getMonthPlanVersion())
                .eq(MonthPlanRequireStock::getProductCode, param.getProductCode())
                .eq(BaseEntity::getIsDelete, YesOrNoEnum.NO.getValue());
        MonthPlanRequireStock stock = saleMonthPlanRequireStockMapper.selectOne(stockQuery);
        if (stock != null) {
            stock.setRemainingQty(param.getAllocationQty());
            saleMonthPlanRequireStockMapper.updateById(stock);
        }
    }

    private void processRemainingDemand(MonthPlanProductionFinalResult param,
                                        FactoryMonthPlanFinalVersionInfoVo finalVersion) {
        if (param.getNetDemandQty() > 0) {
            saveMonthPlanProductionFinalResult(param, finalVersion);
            saveMonthPlanNoticeOrder(param, finalVersion);
        }
    }

    private void saveMonthPlanNoticeOrder(MonthPlanProductionFinalResult param,
                                          FactoryMonthPlanFinalVersionInfoVo finalVersion) {
        MonthPlanNoticeOrder noticeOrder = BeanCopyUtils.copyBean(param, MonthPlanNoticeOrder.class);

        // 设置基础信息
        setNoticeOrderBaseInfo(noticeOrder, finalVersion);

        // 设置产品信息
        setProductInfo(noticeOrder, param);

        // 设置数量信息
        setNoticeOrderQuantityInfo(noticeOrder, param);

        baseDao.insert(noticeOrder);
    }

    private void setNoticeOrderBaseInfo(MonthPlanNoticeOrder noticeOrder,
                                        FactoryMonthPlanFinalVersionInfoVo finalVersion) {
        String noticeNo = MonthPlanSpecificationHelper.generateNoticeNo(incrementService);
        noticeOrder.setNoticeNo(String.format("%s%06d", noticeNo, 1));
        noticeOrder.setStatus(MonthPlanAdjustNoticeStatusEnum.CONFIRM.getStatus());
        noticeOrder.setMonthPlanVersion(finalVersion.getMonthPlanVersion());
        noticeOrder.setProductionVersion(finalVersion.getProductionVersion());
        noticeOrder.setIsImport(YesOrNoEnum.NO.getValue());
        noticeOrder.setId(null);
        noticeOrder.setBaseVale(null);
    }

    private void setProductInfo(MonthPlanNoticeOrder noticeOrder, MonthPlanProductionFinalResult param) {
        MdmMaterialInfo productInfo = productInfoEntityMapper.selectByProductCode(param.getProductCode());
        if (productInfo != null) {
            noticeOrder.setProductDesc(productInfo.getMaterialDesc());
            noticeOrder.setBrand(productInfo.getBrand());
            // noticeOrder.setProSize(productInfo.getProSize());
            noticeOrder.setProductTypeCode(productInfo.getProductTypeCode());
            noticeOrder.setProductTypeName(productInfo.getProductTypeName());
            noticeOrder.setSpecifications(productInfo.getSpecifications());
            noticeOrder.setPattern(productInfo.getPattern());
            noticeOrder.setHierarchy(productInfo.getHierarchy());
            noticeOrder.setLocationType(productInfo.getCommonType());
        }
    }

    private void setNoticeOrderQuantityInfo(MonthPlanNoticeOrder noticeOrder,
                                            MonthPlanProductionFinalResult param) {
        noticeOrder.setNeedQty(param.getOrderQty());
        noticeOrder.setStockAllocationQty(param.getAllocationQty());
        noticeOrder.setPlanQty(param.getNetDemandQty());
        noticeOrder.setProductionQty(MonthPlanSpecificationHelper.calculateProductionQty(param));
    }

    private void saveMonthPlanProductionFinalResult(MonthPlanProductionFinalResult param,
                                                    FactoryMonthPlanFinalVersionInfoVo finalVersion) {
        MonthPlanProductionFinalResult finalData = buildFinalResultData(finalVersion, param);
        String productionNo = generateProductionNo(param, finalVersion);
        finalData.setProductionNo(productionNo);
        save(finalData);
        //重新汇总对应月度外胎汇总
        monthPlanSurplusService.finalUpdatePlanSurplusList(Lists.newArrayList(finalData));
    }

    private String generateProductionNo(MonthPlanProductionFinalResult param,
                                        FactoryMonthPlanFinalVersionInfoVo finalVersion) {
        LambdaQueryWrapper<MonthPlanProductionFinalResult> queryWrapper =
                MonthPlanSpecificationHelper.buildFinalResultQuery(
                        param.getFactoryCode(), param.getYear(), param.getMonth(),
                        finalVersion.getMonthPlanVersion(), finalVersion.getProductionVersion());
        long count = count(queryWrapper);
        String productionNoPrefix = getProductionNoPrefix();
        String serialNumber = String.format(SERIAL_NUMBER_FORMAT, count + 1);
        return productionNoPrefix + serialNumber;
    }

    private MonthPlanProductionFinalResult buildFinalResultData(FactoryMonthPlanFinalVersionInfoVo finalVersion, MonthPlanProductionFinalResult param) {
        // 1. 创建基础对象
        MonthPlanProductionFinalResult finalData = createBaseFinalResult(param, finalVersion);

        // 2. 设置基础属性
        setBasicAttributes(finalData, param);

        // 4. 设置产品信息
        setProductInfo(finalData, param.getProductCode());

        // 5. 设置施工阶段
        setConstructionStage(finalData, param.getConstructionCode());

        // 6. 设置施工构造和硫化时间
        setConstructionAndCuringInfo(finalData, param);
        return finalData;
    }

    /**
     * 获取延迟天数参数
     *
     * @param factoryCode
     * @param productTypeCode
     * @return
     */
    private Integer getAdjustDelayDays(String factoryCode, String productTypeCode) {
        FactoryParam query = new FactoryParam();
        query.setFactoryCode(factoryCode);
        query.setProductTypeCode(productTypeCode);
        query.setParamCode(FactoryConstant.SYS_PARAM_ADJUST_DELAY_DAYS);
        FactoryParam result = factoryParamService.getFacParamSingle(query);
        if (null == result) {
            return BigDecimal.ZERO.intValue();
        }
        String paramValue = result.getParamValue();
        if (StringUtils.isBlank(paramValue)) {
            return BigDecimal.ZERO.intValue();
        }
        return Integer.parseInt(paramValue);
    }

    private void setBasicAttributes(MonthPlanProductionFinalResult finalData,
                                    MonthPlanProductionFinalResult param) {
        // 手动设置所有布尔标志 - JDK 1.8 兼容
        finalData.setIsDeliveryDate(YesOrNoEnum.NO.getValue());
        finalData.setIsContinue(YesOrNoEnum.NO.getValue());
        finalData.setIsImportantCustom(YesOrNoEnum.NO.getValue());
        finalData.setIsEnsurePlan(YesOrNoEnum.NO.getValue());
        finalData.setIsEmergency(YesOrNoEnum.NO.getValue());
        finalData.setIsDebitPlan(YesOrNoEnum.NO.getValue());
        finalData.setIsStockUp(YesOrNoEnum.NO.getValue());
        finalData.setIsImport(YesOrNoEnum.NO.getValue());
        finalData.setIsTrialProductionPlan(YesOrNoEnum.NO.getValue());
        // 设置数量相关属性
        finalData.setProdReqPlan(param.getNetDemandQty());
        finalData.setStockUpQty(0L);
        finalData.setFactProdReqQty(param.getNetDemandQty());
        finalData.setTotalQty(param.getTotalQty());
        finalData.setDifferenceQty(param.getNetDemandQty() - param.getTotalQty());
    }


    private void setProductInfo(MonthPlanProductionFinalResult finalData, String productCode) {
        MdmMaterialInfo productInfo = productInfoEntityMapper.selectByProductCode(productCode);
        if (productInfo != null) {
            // 手动设置产品属性，避免依赖特定 BeanUtils 实现
            finalData.setProductDesc(productInfo.getMaterialDesc());
            finalData.setBrand(productInfo.getBrand());
            // finalData.setProSize(productInfo.getProSize());
            finalData.setSpecifications(productInfo.getSpecifications());
            finalData.setPattern(productInfo.getPattern());
            finalData.setHierarchy(productInfo.getHierarchy());
            finalData.setProductTypeCode(productInfo.getProductTypeCode());
            finalData.setProductTypeName(productInfo.getProductTypeName());
        }
    }

    private void setConstructionStage(MonthPlanProductionFinalResult finalData, String constructionCode) {
        if (StringUtils.isNotBlank(constructionCode)) {
            ConstructionStageEnum stage = ConstructionStageEnum.matchByConstructionCode(constructionCode);
            if (stage != null) {
                finalData.setConstructionStage(stage.getStage());
            }
        }
    }

    private void setConstructionAndCuringInfo(MonthPlanProductionFinalResult finalData,
                                              MonthPlanProductionFinalResult param) {
        if (null != param.getConstruction()) {
            setConstructionDetails(finalData, param.getConstruction());
            calculateAndSetCuringTime(finalData, param, param.getConstruction());
        }
    }

    private void setConstructionDetails(MonthPlanProductionFinalResult finalData,
                                        MdmProductConstruction construction) {
        finalData.setMouldMethod(construction.getMouldMethod());

        // 设置规格代码信息
        ProductSpecInfoVo productSpecCodeInfo = BeanCopyUtils.copyBean(construction, ProductSpecInfoVo.class);
        List<ProductSpecInfoVo> productSpecCodeInfoList = new ArrayList<>();
        productSpecCodeInfoList.add(productSpecCodeInfo);
        finalData.setSpecCodeInfo(JSON.toJSONString(productSpecCodeInfoList));
    }

    private void calculateAndSetCuringTime(MonthPlanProductionFinalResult finalData,
                                           MonthPlanProductionFinalResult param,
                                           MdmProductConstruction construction) {
        Map<String, Integer> changeConfiguration = factoryParamService.getChangeSummerMonth(param.getFactoryCode());

        if (changeConfiguration == null || changeConfiguration.isEmpty()) {
            MdmProductConstructionDto productConstructionInfo =
                    BeanCopyUtils.copyBean(construction, MdmProductConstructionDto.class);

            // 计算基础硫化时间
            BigDecimal curingTime = BigDecimal.ZERO;
            if (changeConfiguration != null) {
                curingTime = productConstructionInfo.getRealCuringTime(
                        param.getMonth(),
                        changeConfiguration.get(FactoryConstant.SYS_PARAM_SUMMER_MONTH),
                        changeConfiguration.get(FactoryConstant.SYS_PARAM_WINTER_MONTH)
                );
            }

            // 添加额外硫化时间
            BigDecimal addCuringTimeValue = factoryParamService.getSingleAddCuringTime(param.getFactoryCode());
            if (null == addCuringTimeValue) {
                addCuringTimeValue = BigDecimal.ZERO;
            }
            BigDecimal finalCuringTime = BigDecimal.ZERO;
            if (curingTime != null) {
                finalCuringTime = curingTime.add(addCuringTimeValue);
            }
            finalData.setCuringTime(finalCuringTime);
            // 计算总硫化分钟数
            calculateTotalVulcanizationMinutes(finalData, param.getNetDemandQty(), finalCuringTime);
        }
    }

    private void calculateTotalVulcanizationMinutes(MonthPlanProductionFinalResult finalData,
                                                    Long netDemandQty, BigDecimal curingTime) {
        BigDecimal totalCuringTime = curingTime.multiply(BigDecimal.valueOf(netDemandQty));
        BigDecimal totalMinutes = totalCuringTime.divide(
                BigDecimal.valueOf(FactoryConstant.MINUTE_SECOND), 2, RoundingMode.HALF_UP);
        finalData.setTotalVulcanizationMinutes(totalMinutes);
    }

    private MonthPlanProductionFinalResult createBaseFinalResult(MonthPlanProductionFinalResult param,
                                                                 FactoryMonthPlanFinalVersionInfoVo finalVersion) {
        MonthPlanProductionFinalResult finalData = BeanCopyUtils.copyBean(param, MonthPlanProductionFinalResult.class);
        // 设置年月和版本信息
        String yearAndMonth = String.format("%s%02d", param.getYear(), param.getMonth());
        finalData.setYearMonth(Integer.valueOf(yearAndMonth));
        finalData.setMonthPlanVersion(finalVersion.getMonthPlanVersion());
        finalData.setProductionVersion(finalVersion.getProductionVersion());
        finalData.setBeginDate(FactoryConstant.MONTH_START_DAY);
        finalData.setEndDay(FactoryConstant.MONTH_MAX_DAY);
        long totalValue = 0;
        for (Integer day = FactoryConstant.MONTH_START_DAY; day <= FactoryConstant.MONTH_MAX_DAY; day++) {
            String fieldName = String.format("day%d", day);
            Object productionQty = param.getFieldValueByFieldName(fieldName);
            if (null == productionQty) {
                continue;
            }
            finalData.setFieldValueByFieldName(fieldName, productionQty);
            totalValue = totalValue + Long.parseLong(productionQty.toString());
        }
        finalData.setTotalQty(totalValue);
        return finalData;
    }

    /**
     * 查询订单计划分配数据
     */
    private List<OrderPlanAllocation> queryOrderPlanAllocations(MonthPlanProductionFinalResult param,
                                                                FactoryMonthPlanFinalVersionInfoVo finalVersion) {
        LambdaQueryWrapper<OrderPlanAllocation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderPlanAllocation::getFactoryCode, param.getFactoryCode())
                .eq(OrderPlanAllocation::getYear, param.getYear())
                .eq(OrderPlanAllocation::getMonth, param.getMonth())
                .eq(OrderPlanAllocation::getMonthPlanVersion, finalVersion.getMonthPlanVersion())
                .eq(OrderPlanAllocation::getProductCode, param.getProductCode())
                .eq(BaseEntity::getIsDelete, YesOrNoEnum.NO.getValue());

        return orderPlanAllocationMapper.selectList(queryWrapper);
    }

    /**
     * 应用需求助手计算结果
     */
    private void applyRequirementHelperResult(MonthPlanProductionRequirementHelper requirementHelper,
                                              MonthPlanProductionFinalResult param) {

        Long availableStock = requirementHelper.getAllocationQty();
        Long planQty = param.getOrderQty();
        Long allocationQty = calculateAllocationQty(availableStock, planQty);
        Long netDemandQty = planQty - allocationQty;
        param.setAllocationQty(allocationQty);
        param.setNetDemandQty(netDemandQty);
        param.setTotalQty(netDemandQty);
        //库位需求
        List<MonthPlanProductionRequirementLocationHelper> locationRequirementList = requirementHelper.getLocationRequirementList();
        if (!CollectionUtils.isEmpty(locationRequirementList)) {
            param.setLocationRequirementInfo(JSON.toJSONString(locationRequirementList));
        }
        //渠道需求
        List<MonthPlanProductionRequirementChannelHelper> channelRequirementList = requirementHelper.getChannelRequirementList();
        if (!CollectionUtils.isEmpty(channelRequirementList)) {
            param.setChannelRequirementInfo(JSON.toJSONString(channelRequirementList));
        }

    }

    /**
     * 计算库存分配数量
     */
    private Long calculateAllocationQty(Long availableStock, Long planQty) {
        if (availableStock == null || availableStock <= 0) {
            return 0L;
        }
        return Math.min(availableStock, planQty);
    }

    // ============ 参数校验和初始化 ============

    /**
     * 参数校验
     */
    private void validateCalculationParams(MonthPlanProductionFinalResult param) {
        if (param == null) {
            throw new BusinessException("计算参数不能为空");
        }
        if (StringUtils.isBlank(param.getProductCode())) {
            throw new BusinessException("SAP代码不能为空");
        }
        if (StringUtils.isBlank(param.getFactoryCode())) {
            throw new BusinessException("分厂编号不能为空");
        }
        if (param.getYear() == null || param.getMonth() == null) {
            throw new BusinessException("年份和月份不能为空");
        }
        if (param.getTotalQty() == null || param.getTotalQty() < 0) {
            throw new BusinessException("订单数量必须为非负数");
        }
    }

    /**
     * 异步获取产品结构信息
     */
    private CompletableFuture<List<MdmProductConstruction>> getProductConstructionAsync(String productCode) {
        return CompletableFuture.supplyAsync(() -> {
            LambdaQueryWrapper<MdmProductConstruction> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(BaseEntity::getIsDelete, YesOrNoEnum.NO.getValue())
                    .eq(MdmProductConstruction::getProductCode, productCode);
            List<MdmProductConstruction> constructions = mdmProductConstructionMapper.selectList(queryWrapper);
            if (CollectionUtils.isEmpty(constructions)) {
                return Collections.emptyList();
            }
            return constructions.stream().sorted(Comparator.comparing(MdmProductConstruction::getMouldMethod)).collect(Collectors.toList());
            // 使用专用线程池
        }, queryExecutor);
    }

    /**
     * 参数校验
     */
    private void validateParam(MonthPlanProductionFinalResult param) {
        if (param == null) {
            throw new BusinessException("参数不能为空");
        }
        if (StringUtils.isBlank(param.getProductCode())) {
            throw new BusinessException("分厂编码不能为空");
        }
        if (StringUtils.isBlank(param.getFactoryCode())) {
            throw new BusinessException("分厂编码不能为空");
        }
        if (param.getYear() == null || param.getMonth() == null) {
            throw new BusinessException("年份和月份不能为空");
        }
    }

    /**
     * 异步获取产品模型关系信息
     */
    private CompletableFuture<ProductModelRelationResult> getProductModelRelationAsync(String productCode) {
        return CompletableFuture.supplyAsync(() -> {
            ProductModelRelationResult result = new ProductModelRelationResult();

            // 查询产品模型关系
            LambdaQueryWrapper<MdmSkuMouldRel> relationQuery = new LambdaQueryWrapper<>();
            relationQuery.eq(MdmSkuMouldRel::getMaterialCode, productCode);
            List<MdmSkuMouldRel> relations = productModelRelationEntityMapper.selectList(relationQuery);

            if (!CollectionUtils.isEmpty(relations)) {
                result.setRelations(relations);
                result.setMouldNo(relations.get(0).getMouldCode());

                // 批量查询模具信息
                List<String> mouldCodes = relations.stream()
                        .map(MdmSkuMouldRel::getMouldCode)
                        .distinct()
                        .collect(Collectors.toList());

                LambdaQueryWrapper<MdmModelInfo> mouldQuery = new LambdaQueryWrapper<>();
                mouldQuery.in(MdmModelInfo::getMouldCode, mouldCodes);
                result.setMouldInfos(modelInfoEntityMapper.selectList(mouldQuery));
            }

            return result;
        }, queryExecutor);
    }

    /**
     * 异步获取最终版本信息
     */
    private CompletableFuture<Optional<FactoryMonthPlanFinalVersionInfoVo>> getFinalVersionInfoAsync(
            String factoryCode, Integer year, Integer month) {
        return CompletableFuture.supplyAsync(() ->
                        Optional.ofNullable(getFinalVersionInfo(factoryCode, year, month)),
                queryExecutor);
    }

    /**
     * 异步获取备货计划
     */
    private CompletableFuture<List<MdmStockUpPlan>> getStockUpPlansAsync(MonthPlanProductionFinalResult param) {
        return CompletableFuture.supplyAsync(() -> {
            LambdaQueryWrapper<MdmStockUpPlan> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(MdmStockUpPlan::getFactoryCode, param.getFactoryCode())
                    .eq(MdmStockUpPlan::getYear, param.getYear())
                    .eq(MdmStockUpPlan::getMonth, param.getMonth())
                    .eq(MdmStockUpPlan::getProductCode, param.getProductCode())
                    .eq(BaseEntity::getIsDelete, YesOrNoEnum.NO.getValue());

            return stockUpPlanMapper.selectList(queryWrapper);
        }, queryExecutor);
    }

    /**
     * 处理产品结构信息
     */
    private void processProductConstructionInfo(List<MdmProductConstruction> constructions,
                                                MonthPlanProductionFinalResult result) {
        result.setConstructions(constructions);
        if (!CollectionUtils.isEmpty(constructions)) {
            result.setSpecCode(constructions.get(0).getSpecCode());
            result.setConstructionCode(constructions.get(0).getConstructionCode());
            result.setEmbryoCode(constructions.get(0).getEmbryoCode());
        }
    }

    /**
     * 处理模型关系信息
     */
    private void processModelRelationInfo(ProductModelRelationResult relationResult,
                                          MonthPlanProductionFinalResult result) {
        // 设置模具号
        if (StringUtils.isNotBlank(relationResult.getMouldNo())) {
            result.setMouldNo(relationResult.getMouldNo());
        }

        // 设置模具数量
        if (!CollectionUtils.isEmpty(relationResult.getRelations())) {
            result.setMouldQty(relationResult.getRelations().size());
        }
        // 处理共用模具逻辑
        processSharedMold(relationResult, result);
    }

    /**
     * 处理共用模具逻辑
     */
    private void processSharedMold(ProductModelRelationResult relationResult, MonthPlanProductionFinalResult result) {
        if (CollectionUtils.isEmpty(relationResult.getMouldInfos()) || StringUtils.isBlank(result.getMouldNo()) || StringUtils.isBlank(result.getEmbryoCode())) {
            return;
        }
        // 批量查询共用模具情况（避免在循环中查询）
        List<MdmSkuMouldRel> sharedMoulds = productModelRelationEntityMapper.selectSameMouldNo();
        if (CollectionUtils.isEmpty(sharedMoulds)) {
            return;
        }
        List<String> mouldNos = relationResult.getMouldInfos().stream().filter(item -> StringUtils.isNotBlank(item.getMouldNo()) && result.getMouldNo().equals(item.getMouldCode())).map(MdmModelInfo::getMouldNo).distinct().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(mouldNos)) {
            return;
        }
        List<MdmSkuMouldRel> currentMouldRelations = sharedMoulds.stream().filter(item -> mouldNos.contains(item.getMouldNo()))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(currentMouldRelations)) {
            return;
        }
        // 过滤掉当前胚胎代码，生成共用模具描述
        String sharedEmbryoCodes = currentMouldRelations.stream().filter(item -> StringUtils.isNotBlank(item.getEmbryoCode())).flatMap(item -> Arrays.stream(StringUtils.split(item.getEmbryoCode(), "")))
                .filter(embryoCode -> !StringUtils.equals(embryoCode, result.getEmbryoCode()))
                .collect(Collectors.joining(","));

        if (StringUtils.isNotBlank(sharedEmbryoCodes)) {
            result.setShareMould("与" + sharedEmbryoCodes + "共用模具");
        }
    }

    /**
     * 处理库存信息
     */
    private void processStockInfo(Optional<FactoryMonthPlanFinalVersionInfoVo> finalVersionOpt,
                                  MonthPlanProductionFinalResult param, MonthPlanProductionFinalResult result) {
        finalVersionOpt.ifPresent(finalVersion -> {
            String monthPlanVersion = finalVersion.getMonthPlanVersion();

            LambdaQueryWrapper<MonthPlanRequireStock> stockQuery = new LambdaQueryWrapper<>();
            stockQuery.eq(MonthPlanRequireStock::getFactoryCode, param.getFactoryCode())
                    .eq(MonthPlanRequireStock::getMonthPlanVersion, monthPlanVersion)
                    .eq(MonthPlanRequireStock::getProductCode, param.getProductCode())
                    .eq(BaseEntity::getIsDelete, YesOrNoEnum.NO.getValue());

            List<MonthPlanRequireStock> stocks = saleMonthPlanRequireStockMapper.selectList(stockQuery);

            if (!CollectionUtils.isEmpty(stocks)) {
                result.setMonthStock(stocks.get(0).getStockQty());
                result.setAllocationQty(stocks.get(0).getRemainingQty());
            } else {
                result.setMonthStock(0);
                result.setAllocationQty(0L);
            }
        });
    }

    /**
     * 处理备货计划
     */
    private void processStockUpPlans(List<MdmStockUpPlan> stockUpPlans, MonthPlanProductionFinalResult result) {
        if (CollectionUtils.isEmpty(stockUpPlans)) {
            result.setAverageValue(0);
            result.setStockUpQty(0L);
            result.setFactor(BigDecimal.ZERO);
            return;
        }
        StockUpCalculationResult calculation = calculateStockUp(stockUpPlans);
        result.setAverageValue(calculation.getAverageValue());
        result.setStockUpQty(calculation.getStockUpQty());
        result.setFactor(calculation.getFactor());
    }

    /**
     * 计算备货统计结果
     */
    private StockUpCalculationResult calculateStockUp(List<MdmStockUpPlan> stockUpPlans) {
        StockUpCalculationResult result = new StockUpCalculationResult();

        // 使用Stream API进行统计计算
        Long totalStockUpQty = stockUpPlans.stream()
                .map(plan -> Optional.ofNullable(plan.getStockQty()).orElse(0L))
                .reduce(0L, Long::sum);

        Integer totalAverageValue = stockUpPlans.stream()
                .map(plan -> Optional.ofNullable(plan.getAverageValue()).orElse(0))
                .reduce(0, Integer::sum);

        result.setStockUpQty(totalStockUpQty);
        result.setAverageValue(totalAverageValue);

        // 计算系数
        if (totalAverageValue != 0) {
            BigDecimal factor = BigDecimal.valueOf(totalStockUpQty)
                    .divide(BigDecimal.valueOf(totalAverageValue), 2, RoundingMode.HALF_UP);
            result.setFactor(factor);
        } else {
            result.setFactor(BigDecimal.ZERO);
        }

        return result;
    }

    // ============ 内部辅助类 ============

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
    private boolean checkDataMouldInfoAndFullMouldNo(MonthPlanProductionFinalResult item, Long importLogId, Integer errorNum, List<ImportErrorLog> importErrorLogs, Map<String, List<MdmModelInfo>> mouldBaseInfoMap) {
        String mouldErrorInfo = I18nUtil.getMessage("ui.data.column.monthPlanMouldingDayResult.specCodeMouldNoErrorInfo");
        String mouldNumberErrorInfo = I18nUtil.getMessage("ui.data.column.monthPlanMouldingDayResult.specCodeMouldNumberErrorInfo");
        //根据规格代号，校验模具信息是否正确
        String factoryCode = item.getFactoryCode();
        String specCode = item.getSpecCode();
        Integer mouldQty = item.getMouldQty();
        if (mouldBaseInfoMap.containsKey(specCode)) {
            List<MdmModelInfo> mouldCodeList = mouldBaseInfoMap.get(specCode);
            if (CollectionUtils.isEmpty(mouldCodeList)) {
                addImportErrorLog(importLogId, errorNum, String.format(mouldErrorInfo, specCode), importErrorLogs);
                return false;
            }
            String mouldNo = mouldCodeList.get(0).getMouldNo();
            item.setMouldNo(mouldNo);
            if (mouldCodeList.size() < mouldQty) {
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
        mouldBaseInfoMap.put(specCode, mouldInfoList);
        if (modelRelationList.size() < mouldQty) {
            addImportErrorLog(importLogId, errorNum, String.format(mouldNumberErrorInfo, specCode, mouldQty), importErrorLogs);
            return false;
        }
        String mouldNo = mouldInfoList.get(0).getMouldNo();
        item.setMouldNo(mouldNo);
        if (mouldInfoList.size() < mouldQty) {
            addImportErrorLog(importLogId, errorNum, String.format(mouldNumberErrorInfo, specCode, mouldQty), importErrorLogs);
            return false;
        }
        return true;
    }

    /**
     * 备货计算结果封装
     */
    @Data
    private static class StockUpCalculationResult {
        private Long stockUpQty = 0L;
        private Integer averageValue = 0;
        private BigDecimal factor = BigDecimal.ZERO;
    }

    /**
     * 获取物料的订单提报、分配、库位需求、渠道需求等信息
     *
     * @param param 需求信息分组
     * @return
     */
    private Map<String, MonthPlanProductionRequirementHelper> getOrderAndAllocationInfo(FactoryMonthPlanProdFinal param) {
        Map<String, List<OrderPlanAllocation>> demandMap = getDemandInfo(param);
        if (CollectionUtils.isEmpty(demandMap)) {
            return Collections.emptyMap();
        }
        Set<String> foreignOemBrandSet = factoryParamService.getForeignOemBrand(param.getFactoryCode());
        Map<String, MonthPlanProductionRequirementHelper> helperMap = new HashMap<>();
        demandMap.forEach((productCode, allocationList) -> {
            MonthPlanProductionRequirementHelper helper = build(allocationList, foreignOemBrandSet);
            if (null == helper) {
                return;
            }
            helper.setProductCode(productCode);
            helperMap.put(productCode, helper);
        });
        return helperMap;
    }

    /**
     * 业务处理前的校验
     * 校验只能有一个分厂、年份、月份
     * 分厂、年份、月份不可为空
     * 定稿后才能导入试制量试计划
     *
     * @return
     */
    private AjaxResult beforeAnalysisCheck(List<TrialProductionPlanDto> excelData) {
        if (CollectionUtils.isEmpty(excelData)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanProductionFinalResult.importEmptyData"));
        }
        // 只能导入一个分厂版本
        Set<String> productionVersionSet = excelData.stream().map(TrialProductionPlanDto::getSameProductionVersionKey).collect(Collectors.toSet());
        if (productionVersionSet.size() > BigDecimal.ONE.intValue()) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanProductionFinalResult.importSameVersion"));
        }
        TrialProductionPlanDto first = excelData.get(0);
        String factoryCode = first.getFactoryCode();
        Integer year = first.getYear();
        Integer month = first.getMonth();
        if (StringUtils.isBlank(factoryCode) || null == year || null == month) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanProductionFinalResult.importFactoryAndYearMonthEmpty"));
        }
        FactoryProductionVersion productionVersion = factoryProductionVersionService.getFinalVersionByYearMonth(factoryCode, year, month);
        //没定稿前不能导入
        if (null == productionVersion) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanProductionFinalResult.noTrialProductionPlan"));
        }
        return AjaxResult.success(productionVersion);
    }

    /**
     * 业务处理前的校验
     * 校验只能有一个分厂、年份、月份
     * 分厂、年份、月份不可为空
     * 定稿后才能导入试制量试计划
     *
     * @return
     */
    private AjaxResult beforeAnalysisAdjustCheck(List<MonthPlanProductionFinalResultVo> excelData) {
        if (CollectionUtils.isEmpty(excelData)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanProductionFinalResult.importEmptyData"));
        }
        // 只能导入一个分厂版本
        Set<String> productionVersionSet = excelData.stream().map(MonthPlanProductionFinalResultVo::getSameProductionVersionKey).collect(Collectors.toSet());
        if (productionVersionSet.size() > BigDecimal.ONE.intValue()) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanProductionFinalResult.importSameVersion"));
        }
        MonthPlanProductionFinalResultVo first = excelData.get(0);
        String factoryCode = FactoryConstant.DEFAULT_FACTORY_CODE;
        Integer year = first.getYear();
        Integer month = first.getMonth();
        if (null == year || null == month) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanProductionFinalResult.importFactoryAndYearMonthEmpty"));
        }
        FactoryProductionVersion productionVersion = factoryProductionVersionService.getFinalVersionByYearMonth(factoryCode, year, month);
        //没定稿前不能导入
        if (null == productionVersion) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanProductionFinalResult.noTrialProductionPlan"));
        }
        return AjaxResult.success(productionVersion);
    }

    /**
     * excel数据解析及业务校验处理
     * 1、基础数据格式校验(必输、数据类型等)
     * 2、根据规格代号、生胎代码补充SAP代码，如果没有加入需要自动生成集合
     * 3、校验模具关系，不存在则提示错误，如果关系数量不符同样提示错误
     *
     * @param excelData                     excel原始数据
     * @param excelDataAnalysisHelper       excel处理辅助类
     * @param curingTime                    试制量试的硫化时间
     * @param productionVersion             版本信息
     * @param productConstructionMap        SAP与施工关系集合
     * @param informalConstructionStageList 需自动生成施工关系的集合
     * @return
     */
    private List<MonthPlanProductionFinalResult> analysisData(List<TrialProductionPlanDto> excelData, ExcelDataAnalysisDto excelDataAnalysisHelper, Integer curingTime, FactoryProductionVersion productionVersion, Map<String, MdmProductConstruction> productConstructionMap, List<MonthPlanProductionFinalResult> informalConstructionStageList) {
        Integer year = productionVersion.getYear();
        Integer month = productionVersion.getMonth();
        String factoryCode = productionVersion.getFactoryCode();
        String monthPlanVersion = productionVersion.getMonthPlanVersion();
        String finalProductionVersion = productionVersion.getProductionVersion();
        Integer yearAndMonth = Integer.valueOf(String.format("%s%02d", year, month));
        Long importLogId = excelDataAnalysisHelper.getImportLogId();
        List<ImportErrorLog> importErrorLogs = excelDataAnalysisHelper.getImportErrorLogs();
        //错误计数器
        int failureNum = excelDataAnalysisHelper.getFailureNumber();
        List<MonthPlanProductionFinalResult> importList = new ArrayList<>();
        // 国际化提示
        Map<String, String> errorInfoMap = buildErrorInfoMap();
        //错误行，从第2行开始
        int errorInitNum = 2;
        int rowSize = excelData.size();
        //流水号信息，下标、前缀及组成规则
        int addIndex = 1;
        String batchNo = "T" + DateUtils.dateTimeNow();
        String productionNoPrefix = getProductionNoPrefix();
        String productionNoFormat = "%s%s";
        String serialNumberFormat = "%06d";
        //模具匹配信息存储--临时存储
        Map<String, List<MdmModelInfo>> mouldBaseInfoMap = new HashMap<>();
        int productCodeIndex = 0;
        for (int i = 0; i < rowSize; i++) {
            int errorNum = i + errorInitNum;
            TrialProductionPlanDto item = excelData.get(i);
            TrialProductionPlanExcelHelper excelHelper = new TrialProductionPlanExcelHelper(item);
            //数据校验
            boolean checkDataResult = checkDataAndFullInfo(excelHelper, importLogId, errorNum, importErrorLogs, errorInfoMap, productConstructionMap);
            if (!checkDataResult) {
                failureNum++;
                continue;
            }
            MonthPlanProductionFinalResult resultRow = excelHelper.getFinalData();
            if (null == resultRow) {
                continue;
            }
            resultRow.setIsTrialProductionPlan(YesOrNoEnum.YES.getValue());
            String serialNumber = String.format(serialNumberFormat, addIndex);
            String productionNo = String.format(productionNoFormat, productionNoPrefix, serialNumber);
            resultRow.setProductionNo(productionNo);
            resultRow.setCuringTime(BigDecimal.valueOf(curingTime));
            //版本信息
            resultRow.setFactoryCode(factoryCode);
            resultRow.setYear(year);
            resultRow.setMonth(month);
            resultRow.setYearMonth(yearAndMonth);
            resultRow.setMonthPlanVersion(monthPlanVersion);
            resultRow.setProductionVersion(finalProductionVersion);
            addIndex = addIndex + 1;
            //构建试制量试数据可能需要自动生成SAP与施工关系数据集合
            productCodeIndex = addProductConstructionConfiguration(batchNo, productCodeIndex, resultRow, informalConstructionStageList);
            //校验模具并补充模具号
            boolean checkMouldInfoResult = checkDataMouldInfoAndFullMouldNo(resultRow, importLogId, errorNum, importErrorLogs, mouldBaseInfoMap);
            if (!checkMouldInfoResult) {
                failureNum++;
                continue;
            }
            //加入数据
            importList.add(resultRow);
        }
        excelDataAnalysisHelper.setFailureNumber(failureNum);
        return importList;
    }

    /**
     * 数据行校验
     *
     * @param excelHelper            数据行
     * @param importLogId            导入日志ID
     * @param errorNum               错误行
     * @param importErrorLogs        错误日志集合对象
     * @param buildErrorInfo         错误信息集合
     * @param productConstructionMap SAP与施工关系配置
     * @return
     */
    private boolean checkDataAndFullInfo(TrialProductionPlanExcelHelper excelHelper, Long importLogId, Integer errorNum, List<ImportErrorLog> importErrorLogs, Map<String, String> buildErrorInfo, Map<String, MdmProductConstruction> productConstructionMap) {
        TrialProductionPlanDto item = excelHelper.getExcelRowData();
        //数据基本校验
        List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, item);
        if (!CollectionUtils.isEmpty(validated)) {
            importErrorLogs.addAll(validated);
            return false;
        }
        MonthPlanProductionFinalResult finalData = new MonthPlanProductionFinalResult();
        BeanUtils.copyProperties(item, finalData);
        int dayOfMonth = LocalDate.of(item.getYear(), item.getMonth(), 1).with(TemporalAdjusters.lastDayOfMonth()).getDayOfMonth();
        if (ProductionPlanExcelUtils.isExceedMonthMaxDay(finalData, dayOfMonth)) {
            addImportErrorLog(importLogId, errorNum, String.format(buildErrorInfo.get(ERROR_MAX_DAY), dayOfMonth), importErrorLogs);
            return false;
        }
        //统计排产总值
        ProductionPlanExcelUtils.resetTotalProductionQty(finalData, true);
        // 生产实际排产量 > 0 ，模具数不能 <= 0
        if (finalData.getTotalQty() > 0 && finalData.getMouldQty() <= 0) {
            addImportErrorLog(importLogId, errorNum, buildErrorInfo.get(ERROR_TOTAL_QTY), importErrorLogs);
            return false;
        }
        //起始日、结束日
        Integer beginDay = finalData.getBeginDate();
        Integer endDay = finalData.getEndDay();
        if (!ProductionPlanExcelUtils.haseDoubleDayValue(beginDay, endDay)) {
            addImportErrorLog(importLogId, errorNum, buildErrorInfo.get(ERROR_SAME_EMPTY), importErrorLogs);
            return false;
        }
        //根据硫化规格生胎代号获取SAP代码
        String productConstructionKey = finalData.getProductConstructionKey();
        if (!productConstructionMap.containsKey(productConstructionKey)) {
            getProductConstructionInfo(productConstructionKey, finalData, productConstructionMap);
        }
        //如果没有
        if (StringUtils.isBlank(finalData.getProductCode())) {
            MdmProductConstruction configuration = productConstructionMap.get(productConstructionKey);
            if (null != configuration) {
                finalData.setProductCode(configuration.getProductCode());
            }
        }
        finalData.setIsImport(Constant.TRUE);
        excelHelper.setFinalData(finalData);
        return true;
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
    private int addProductConstructionConfiguration(String batchNo, int index, MonthPlanProductionFinalResult importFinalPlan, List<MonthPlanProductionFinalResult> informalConstructionStageList) {
        Integer constructionStage = importFinalPlan.getConstructionStage();
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
     * 产品模型关系查询结果封装
     */
    @Data
    private static class ProductModelRelationResult {
        private List<MdmSkuMouldRel> relations;
        private List<MdmModelInfo> mouldInfos;
        private String mouldNo;
    }

    /**
     * 保存SAP与施工关系
     *
     * @param informalConstructionStageList 调整计划集合
     * @param productConstructionMap        SAP与施工关系
     * @param curingTime                    试制量试的硫化时间
     */
    private void saveMdmProductConstructionInfo(List<MonthPlanProductionFinalResult> informalConstructionStageList, Map<String, MdmProductConstruction> productConstructionMap, Integer curingTime) {
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
            MdmProductConstruction addConfiguration = AdjustUtils.buildProductionConstructionConfiguration(informalConstructionPlan, curingTime);
            addProductConstructionList.add(addConfiguration);
        });
        if (!CollectionUtils.isEmpty(addProductConstructionList)) {
            baseDao.insertBatch(addProductConstructionList);
        }
    }

    /**
     * 获取需求对应的SKU的库存信息，并按SKU分组
     *
     * @param param
     * @return
     */
    private Map<String, MonthPlanRequireStock> getStockInfoByRequirement(FactoryMonthPlanProdFinal param) {
        List<MonthPlanRequireStock> requirementStockList = getRequirementStockByVersion(param);
        if (CollectionUtils.isEmpty(requirementStockList)) {
            return Collections.emptyMap();
        }
        return requirementStockList.stream().collect(Collectors.toMap(MonthPlanRequireStock::getProductCode, Function.identity()));
    }

    /**
     * 根据需求版本，获取对应的备货计划信息
     *
     * @param param
     * @return
     */
    private Map<String, MonthPlanProductionStockUpRequirementHelper> getStockUpInfo(FactoryMonthPlanProdFinal param) {
        List<MdmStockUpPlan> stockUpPlanList = getStockUpByVersion(param);
        if (CollectionUtils.isEmpty(stockUpPlanList)) {
            return Collections.emptyMap();
        }
        Map<String, MonthPlanProductionStockUpRequirementHelper> stockUpGroupMap = new HashMap<>();
        Map<String, List<MdmStockUpPlan>> productStockUpGroupMap = stockUpPlanList.stream().collect(Collectors.groupingBy(MdmStockUpPlan::getProductCode));
        productStockUpGroupMap.forEach((productCode, stockUpPlan) -> {
            if (CollectionUtils.isEmpty(stockUpPlan)) {
                return;
            }
            MonthPlanProductionStockUpRequirementHelper helper = stockUpGroupMap.get(productCode);
            if (null == helper) {
                helper = new MonthPlanProductionStockUpRequirementHelper();
                helper.setProductCode(productCode);
                helper.setAverageType(stockUpPlan.get(0).getAverageType());
                helper.setStockUpQty(BigDecimal.ZERO.longValue());
                helper.setAverageValue(BigDecimal.ZERO.intValue());
                helper.setFactor(BigDecimal.ZERO);
                stockUpGroupMap.put(productCode, helper);
            }
            MonthPlanProductionStockUpRequirementHelper stockUpHelper = stockUpGroupMap.get(productCode);
            stockUpPlan.stream().forEach(singlePlan -> {
                Long sumQty = stockUpHelper.getStockUpQty();
                if (null == sumQty) {
                    sumQty = BigDecimal.ZERO.longValue();
                }
                Integer sumAverageValue = stockUpHelper.getAverageValue();
                if (null == sumAverageValue) {
                    sumAverageValue = BigDecimal.ZERO.intValue();
                }
                Long stockUpQty = singlePlan.getStockQty();
                if (null == stockUpQty) {
                    stockUpQty = BigDecimal.ZERO.longValue();
                }
                Integer averageValue = singlePlan.getAverageValue();
                if (null == averageValue) {
                    averageValue = BigDecimal.ZERO.intValue();
                }
                sumQty = sumQty + stockUpQty;
                sumAverageValue = sumAverageValue + averageValue;
                stockUpHelper.setAverageValue(sumAverageValue);
                stockUpHelper.setStockUpQty(sumQty);
                if (BigDecimal.ZERO.intValue() != sumAverageValue) {
                    stockUpHelper.setFactor(BigDecimal.valueOf(sumQty).divide(BigDecimal.valueOf(sumAverageValue), 2, RoundingMode.HALF_UP));
                }
            });
        });
        return stockUpGroupMap;
    }

    /**
     * 获取版本需求信息，包含提报需求及分配的库存，并按SKU分组
     *
     * @param param
     * @return
     */
    private Map<String, List<OrderPlanAllocation>> getDemandInfo(FactoryMonthPlanProdFinal param) {
        List<OrderPlanAllocation> stockAllocationList = getRequirementByVersion(param);
        if (CollectionUtils.isEmpty(stockAllocationList)) {
            return Collections.emptyMap();
        }
        return stockAllocationList.stream().collect(Collectors.groupingBy(OrderPlanAllocation::getProductCode));
    }

    /**
     * 构建定稿数据
     *
     * @param versionDataList     排产版本数据
     * @param demandInfoMap       版本需求信息
     * @param requirementStockMap 版本库存信息
     * @param stockUpGroupMap     备货信息
     * @return
     */
    private List<MonthPlanProductionFinalResult> buildFinalData(List<MonthPlanProductionDayResult> versionDataList, Map<String, MonthPlanProductionRequirementHelper> demandInfoMap, Map<String, MonthPlanRequireStock> requirementStockMap, Map<String, MonthPlanProductionStockUpRequirementHelper> stockUpGroupMap) {
        if (CollectionUtils.isEmpty(versionDataList)) {
            return Collections.emptyList();
        }
        List<MonthPlanProductionFinalResult> finalResultDataList = new ArrayList<>();
        versionDataList.stream().forEach(versionData -> {
            String productCode = versionData.getProductCode();
            MonthPlanProductionRequirementHelper requirementHelper = demandInfoMap.get(productCode);
            MonthPlanRequireStock requireStock = requirementStockMap.get(productCode);
            MonthPlanProductionFinalResult finalData = buildFinalResultData(versionData, requirementHelper, requireStock);
            if (null == finalData) {
                return;
            }
            finalResultDataList.add(finalData);
            MonthPlanProductionStockUpRequirementHelper stockUpInfo = stockUpGroupMap.get(productCode);
            if (null == stockUpInfo) {
                return;
            }
            finalData.setAverageValue(stockUpInfo.getAverageValue());
            finalData.setFactor(stockUpInfo.getFactor());
            finalData.setStockUpQty(stockUpInfo.getStockUpQty());
        });
        return finalResultDataList;
    }

    /**
     * 设置共用模具信息
     *
     * @param finalResultDataList
     */
    private void setShareMouldInfo(List<MonthPlanProductionFinalResult> finalResultDataList) {
        if (CollectionUtils.isEmpty(finalResultDataList)) {
            return;
        }
        String shareMouldFormat = "与%s生胎共用模具";
        //按模具分组
        Map<String, List<MonthPlanProductionFinalResult>> shareMouldGroup = finalResultDataList.stream().collect(Collectors.groupingBy(MonthPlanProductionFinalResult::getMouldNo));
        Map<String, Set<String>> shareMouldEmbryoCodeGroup = new HashMap<>();
        shareMouldGroup.forEach((mouldNo, productionList) -> {
            if (CollectionUtils.isEmpty(productionList)) {
                return;
            }
            Set<String> embryoCodeSet = productionList.stream().map(MonthPlanProductionFinalResult::getEmbryoCode).collect(Collectors.toSet());
            shareMouldEmbryoCodeGroup.put(mouldNo, embryoCodeSet);
        });
        finalResultDataList.stream().forEach(finalData -> {
            String mouldNo = finalData.getMouldNo();
            if (StringUtils.isBlank(mouldNo) || StringUtils.isBlank(finalData.getEmbryoCode())) {
                return;
            }
            Set<String> embryoCodeSet = shareMouldEmbryoCodeGroup.get(mouldNo);
            if (CollectionUtils.isEmpty(embryoCodeSet)) {
                return;
            }
            if (embryoCodeSet.size() == BigDecimal.ONE.intValue()) {
                return;
            }
            List<String> shareEmbryoCodeList = new ArrayList<>();
            embryoCodeSet.forEach(embryoCode -> {
                if (finalData.getEmbryoCode().equals(embryoCode)) {
                    return;
                }
                shareEmbryoCodeList.add(embryoCode);
            });
            if (CollectionUtils.isEmpty(shareEmbryoCodeList)) {
                return;
            }
            String embryoCodeInfo = shareEmbryoCodeList.stream().collect(Collectors.joining());
            finalData.setShareMould(String.format(shareMouldFormat, embryoCodeInfo));
        });
    }

    /**
     * 根据物料编码，构建物料编码的提报总量，库位提报量，渠道提报量，分配总数
     *
     * @param allocationList     分配信息
     * @param foreignOemBrandSet 外销贴牌品牌集合
     * @return
     */
    private MonthPlanProductionRequirementHelper build(List<OrderPlanAllocation> allocationList, Set<String> foreignOemBrandSet) {
        if (CollectionUtils.isEmpty(allocationList)) {
            return null;
        }
        MonthPlanProductionRequirementHelper helper = new MonthPlanProductionRequirementHelper();
        helper.setAllocationQty(BigDecimal.ZERO.longValue());
        helper.setSumQty(BigDecimal.ZERO.longValue());
        Map<String, MonthPlanProductionRequirementLocationHelper> locationMap = new HashMap<>();
        Map<String, MonthPlanProductionRequirementChannelHelper> channelMap = new HashMap<>();
        allocationList.stream().forEach(allocationInfo -> {
            Long allocationQty = allocationInfo.getAllocationQty();
            Long qty = allocationInfo.getPlanQty();
            Long sumAllocationQty = helper.getAllocationQty();
            Long sumQty = helper.getSumQty();
            if (null != qty) {
                helper.setSumQty(sumQty + qty);
            }
            if (null != allocationQty) {
                helper.setAllocationQty(sumAllocationQty + allocationQty);
            }
            setLocationInfo(locationMap, allocationInfo);
            setChannelInfo(channelMap, allocationInfo, foreignOemBrandSet);
        });
        if (!CollectionUtils.isEmpty(locationMap)) {
            helper.setLocationRequirementList(new ArrayList<>(locationMap.values()));
        }
        if (!CollectionUtils.isEmpty(channelMap)) {
            helper.setChannelRequirementList(new ArrayList<>(channelMap.values()));
        }
        return helper;
    }

    /**
     * 构建定稿数据
     *
     * @param versionData       排产版本信息
     * @param requirementHelper 需求信息
     * @param requireStock      库存信息
     * @return
     */
    private MonthPlanProductionFinalResult buildFinalResultData(MonthPlanProductionDayResult versionData, MonthPlanProductionRequirementHelper requirementHelper, MonthPlanRequireStock requireStock) {
        MonthPlanProductionFinalResult finalData = new MonthPlanProductionFinalResult();
        BeanUtils.copyProperties(versionData, finalData);
        finalData.setId(null);
        Integer year = versionData.getYear();
        Integer month = versionData.getMonth();
        // 年月拼接
        if (null != year && null != month) {
            String yearAndMonth = String.format("%s%02d", year, month);
            finalData.setYearMonth(Integer.valueOf(yearAndMonth));
        }
        finalData.setIsImport(YesOrNoEnum.NO.getValue());
        if (null != requireStock) {
            finalData.setMonthStock(requireStock.getStockQty());
        }
        if (null == requirementHelper) {
            return finalData;
        }
        finalData.setOrderQty(requirementHelper.getSumQty());
        finalData.setAllocationQty(requirementHelper.getAllocationQty());
        //库位需求
        List<MonthPlanProductionRequirementLocationHelper> locationRequirementList = requirementHelper.getLocationRequirementList();
        if (!CollectionUtils.isEmpty(locationRequirementList)) {
            finalData.setLocationRequirementInfo(JSON.toJSONString(locationRequirementList));
        }
        //渠道需求
        List<MonthPlanProductionRequirementChannelHelper> channelRequirementList = requirementHelper.getChannelRequirementList();
        if (!CollectionUtils.isEmpty(channelRequirementList)) {
            finalData.setChannelRequirementInfo(JSON.toJSONString(channelRequirementList));
        }
        return finalData;
    }

    /**
     * 处理库位需求
     * 按库位汇总需求
     *
     * @param allocationInfo
     */
    private void setLocationInfo(Map<String, MonthPlanProductionRequirementLocationHelper> locationMap, OrderPlanAllocation allocationInfo) {
        String locationType = allocationInfo.getLocationType();
        if (StringUtils.isBlank(locationType)) {
            return;
        }
        MonthPlanProductionRequirementLocationHelper locationHelper = locationMap.get(locationType);
        if (null == locationHelper) {
            locationHelper = new MonthPlanProductionRequirementLocationHelper();
            locationHelper.setQty(BigDecimal.ZERO.longValue());
            locationHelper.setType(locationType);
            locationMap.put(locationType, locationHelper);
        }
        Long planQty = allocationInfo.getPlanQty();
        if (null == planQty) {
            planQty = BigDecimal.ZERO.longValue();
        }
        Long sumQty = locationHelper.getQty();
        if (null == sumQty) {
            sumQty = BigDecimal.ZERO.longValue();
        }
        locationHelper.setQty(sumQty + planQty);
    }

    /**
     * 处理渠道需求
     * 按渠道或是品牌汇总需求
     *
     * @param channelMap         SKU的统计渠道集合
     * @param allocationInfo     分配信息
     * @param foreignOemBrandSet 外销贴牌品牌集合
     */
    private void setChannelInfo(Map<String, MonthPlanProductionRequirementChannelHelper> channelMap, OrderPlanAllocation allocationInfo, Set<String> foreignOemBrandSet) {
        String locationType = allocationInfo.getLocationType();
        String channelCode = allocationInfo.getChannel();
        String brandCode = allocationInfo.getBrand();
        ChannelRequirementTypeEnum channelRequirementType = ChannelRequirementTypeEnum.getInstance(locationType, channelCode, brandCode, foreignOemBrandSet);
        if (null == channelRequirementType) {
            return;
        }
        String channelRequirementCode = channelRequirementType.getCode();
        MonthPlanProductionRequirementChannelHelper helper = channelMap.get(channelRequirementCode);
        if (null == helper) {
            helper = new MonthPlanProductionRequirementChannelHelper();
            helper.setQty(BigDecimal.ZERO.longValue());
            helper.setCode(channelRequirementCode);
            channelMap.put(channelRequirementCode, helper);
        }
        Long planQty = allocationInfo.getPlanQty();
        if (null == planQty) {
            planQty = BigDecimal.ZERO.longValue();
        }
        Long sumQty = helper.getQty();
        if (null == sumQty) {
            sumQty = BigDecimal.ZERO.longValue();
        }
        helper.setQty(sumQty + planQty);
    }

    /**
     * 构建检验错误提示信息集合
     *
     * @return
     */
    private Map<String, String> buildErrorInfoMap() {
        Map<String, String> errorInfoMap = new HashMap<>();
        String monthMaxDay = I18nUtil.getMessage("ui.data.column.factoryMonthPlanProdFinal.monthMaxDay");
        String totalQtyCheck = I18nUtil.getMessage("ui.data.column.monthPlanMouldingDayResult.totalQtyCheck");
        String sameEmpty = I18nUtil.getMessage("ui.data.column.factoryMonthPlanProdFinal.sameEmpty");
        errorInfoMap.put(ERROR_MAX_DAY, monthMaxDay);
        errorInfoMap.put(ERROR_TOTAL_QTY, totalQtyCheck);
        errorInfoMap.put(ERROR_SAME_EMPTY, sameEmpty);
        return errorInfoMap;
    }

    /**
     * 获取排产单号前缀
     *
     * @return
     */
    private String getProductionNoPrefix() {
        String prefix = String.format("%s%s", IncrementConstant.MONTH_FINAL_PRODUCT, DateUtils.dateTimeNow("yyyyMMdd"));
        return incrementService.getBillNoSequenceByExpire(prefix, 3, 60 * 24 * 7);
    }

    /**
     * 根据排产版本参数，获取对应的排产数据
     *
     * @param param 参数
     * @return
     */
    private List<MonthPlanProductionDayResult> getProductionVersionData(FactoryMonthPlanProdFinal param) {
        QueryWrapper<MonthPlanProductionDayResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", param.getFactoryCode());
        queryWrapper.eq("YEAR", param.getYear());
        queryWrapper.eq("MONTH", param.getMonth());
        queryWrapper.eq("MONTH_PLAN_VERSION", param.getMonthPlanVersion());
        queryWrapper.eq("PRODUCTION_VERSION", param.getProductionVersion());
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        return monthPlanProductionDayResultMapper.selectList(queryWrapper);

    }

    /**
     * 根据需求版本，获取对应需求版本的订单及分配信息
     *
     * @param param 版本参数
     * @return
     */
    private List<OrderPlanAllocation> getRequirementByVersion(FactoryMonthPlanProdFinal param) {
        QueryWrapper<OrderPlanAllocation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", param.getFactoryCode());
        queryWrapper.eq("YEAR", param.getYear());
        queryWrapper.eq("MONTH", param.getMonth());
        queryWrapper.eq("MONTH_PLAN_VERSION", param.getMonthPlanVersion());
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        return orderPlanAllocationMapper.selectList(queryWrapper);
    }

    /**
     * 根据需求版本，获取对应需求版本的库存信息
     *
     * @param param
     * @return
     */
    private List<MonthPlanRequireStock> getRequirementStockByVersion(FactoryMonthPlanProdFinal param) {
        QueryWrapper<MonthPlanRequireStock> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", param.getFactoryCode());
        queryWrapper.eq("MONTH_PLAN_VERSION", param.getMonthPlanVersion());
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        return saleMonthPlanRequireStockMapper.selectList(queryWrapper);
    }

    /**
     * 根据需求版本，获取备货计划信息
     *
     * @param param
     * @return
     */
    private List<MdmStockUpPlan> getStockUpByVersion(FactoryMonthPlanProdFinal param) {
        QueryWrapper<MdmStockUpPlan> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", param.getFactoryCode());
        queryWrapper.eq("YEAR", param.getYear());
        queryWrapper.eq("MONTH", param.getMonth());
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        return stockUpPlanMapper.selectList(queryWrapper);
    }

    /**
     * 根据分厂、年份、月份得到分厂版本信息对象
     *
     * @param factoryCode
     * @param year
     * @param month
     * @return
     */
    private FactoryMonthPlanFinalVersionInfoVo getFinalVersionInfo(String factoryCode, Integer year, Integer month) {
        if (com.ruoyi.common.utils.StringUtils.isBlank(factoryCode) || null == year || null == month) {
            return null;
        }
        QueryWrapper<FactoryProductionVersion> queryVersion = new QueryWrapper<>();
        queryVersion.eq("FACTORY_CODE", factoryCode);
        queryVersion.eq("YEAR", year);
        queryVersion.eq("MONTH", month);
        queryVersion.eq("IS_FINAL", YesOrNoEnum.YES.getValue());
        queryVersion.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        FactoryProductionVersion result = factoryProductionVersionMapper.selectOne(queryVersion);
        if (null == result) {
            return null;
        }
        FactoryMonthPlanFinalVersionInfoVo info = new FactoryMonthPlanFinalVersionInfoVo();
        BeanUtils.copyProperties(result, info);
        return info;
    }

    /**
     * 根据硫化规格及生胎号，获取SAP与施工配置
     *
     * @param key                    键值
     * @param item                   包含硫化时间
     * @param productConstructionMap 施工配置
     */
    private void getProductConstructionInfo(String key, MonthPlanProductionFinalResult item, Map<String, MdmProductConstruction> productConstructionMap) {
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
     * 根据SAP信息，补充物料基本信息
     * 物料描述、寸口、品牌
     * 规格、花纹、层级、胎别
     *
     * @param importList
     */
    private void fullProductInfo(List<MonthPlanProductionFinalResult> importList) {
        if (CollectionUtils.isEmpty(importList)) {
            return;
        }
        // 查询对应物料信息，根据分厂+SAP代码映射
        Map<String, MdmMaterialInfo> productInfoMap = getMdmMaterialInfoMap(importList);
        //补充物料信息
        importList.stream().forEach(item -> {
            //物料信息补充-不进行校验
            MdmMaterialInfo productInfo = productInfoMap.get(GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getProductCode()));
            if (null == productInfo) {
                return;
            }
            item.setProductDesc(productInfo.getMaterialDesc());
            //item.setProSize(productInfo.getProSize());
            item.setBrand(productInfo.getBrand());
            item.setSpecifications(productInfo.getSpecifications());
            item.setPattern(productInfo.getPattern());
            item.setHierarchy(productInfo.getHierarchy());
            item.setProductTypeCode(productInfo.getProductTypeCode());
            item.setProductTypeName(productInfo.getProductTypeName());
        });
    }

    /**
     * 查询对应物料信息，根据分厂+SAP代码映射
     * 批量查询
     *
     * @param list 排产计划结合
     */
    private Map<String, MdmMaterialInfo> getMdmMaterialInfoMap(List<MonthPlanProductionFinalResult> list) {
        if (CollectionUtils.isEmpty(list)) {
            return new HashMap<>();
        }
        List<String> factoryCodeList = list.stream().map(MonthPlanProductionFinalResult::getFactoryCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        List<String> productCodeList = list.stream().map(MonthPlanProductionFinalResult::getProductCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(factoryCodeList) && CollectionUtils.isEmpty(productCodeList)) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<MdmMaterialInfo> wrapper = Wrappers.lambdaQuery(MdmMaterialInfo.class)
                .in(!CollectionUtils.isEmpty(factoryCodeList), MdmMaterialInfo::getFactoryCode, factoryCodeList)
                .in(!CollectionUtils.isEmpty(productCodeList), MdmMaterialInfo::getMaterialCode, productCodeList);
        return productInfoEntityMapper.selectList(wrapper).stream().collect(Collectors.toMap(v -> GenerageMapKeyUtils.createMapKey(v.getFactoryCode(), v.getMaterialCode()), Function.identity(), (v1, v2) -> v1));
    }
}
