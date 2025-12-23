package com.zlt.aps.monthplan.factory.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.DictUtils;
import com.ruoyi.common.core.utils.reflect.ReflectUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.tlt.aps.constant.Constant;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.enums.CommonTypeEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.exception.BusinessException;
import com.tlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.aps.factory.mapper.MonthPlanRequireMapper;
import com.zlt.aps.factory.scheduling.ProductionContext;
import com.zlt.aps.maindata.mapper.MdmMaterialInfoEntityMapper;
import com.zlt.aps.maindata.mapper.MdmProductModelRelationEntityMapper;
import com.zlt.aps.maindata.service.IFactoryParamService;
import com.zlt.aps.maindata.utils.FactoryParamUtils;
import com.zlt.aps.maindata.utils.LambdaWrapperBuilder;
import com.zlt.aps.maindata.utils.ScmListUtils;
import com.zlt.aps.monthplan.api.domain.dto.ChangeSpecCodeMouldingDayResultParam;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.aps.monthplan.api.domain.vo.*;
import com.zlt.aps.monthplan.demand.mapper.OrderPlanAllocationMapper;
import com.zlt.aps.monthplan.demand.service.IOrderPlanAllocationService;
import com.zlt.aps.monthplan.factory.mapper.FactoryProductionVersionMapper;
import com.zlt.aps.monthplan.factory.mapper.MonthPlanMouldingDayResultMapper;
import com.zlt.aps.monthplan.factory.mapper.MonthPlanProductionResultDetailMapper;
import com.zlt.aps.monthplan.factory.service.IFactoryProductionVersionService;
import com.zlt.aps.monthplan.factory.service.IMonthPlanMouldingDayResultService;
import com.zlt.aps.monthplan.factory.service.IMonthPlanNoProductionPlanService;
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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zlt.common.utils.ImportExcelValidatedUtils.addImportErrorLog;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MonthPlanMouldingDayResultServiceImpl.java
 * 描    述：MonthPlanMouldingDayResultServiceImpl分厂月生产计划排产过程-模具排产结果汇总业务层处理
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
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class MonthPlanMouldingDayResultServiceImpl implements IMonthPlanMouldingDayResultService {

    private final MonthPlanMouldingDayResultMapper baseMapper;

    private final MdmMaterialInfoEntityMapper productInfoEntityMapper;

    private final FactoryProductionVersionMapper factoryProductionVersionMapper;

    private final MonthPlanProductionResultDetailMapper dayResultDetailMapper;

    private final BaseDao baseDao;

    private final IOrderPlanAllocationService iOrderPlanAllocationService;

    private final IFactoryProductionVersionService factoryProductionVersionService;

    private final IMonthPlanNoProductionPlanService iMonthPlanNoProductionPlanService;

    @Autowired
    private MdmProductModelRelationEntityMapper mdmProductModelRelationEntityMapper;

    /**
     * 查询列表
     */
    @Override
    public List<MonthPlanMouldingDayResult> selectList(MonthPlanMouldingDayResult queryVO) {
        return selectList(queryVO, false);
    }

    /**
     * 查询列表
     * 需要对开始日期和结束日期
     */
    @Override
    public List<MonthPlanMouldingDayResult> selectList(MonthPlanMouldingDayResult queryVO, boolean isHandler) {
        String productionVersion = queryVO.getProductionVersion();
        QueryWrapper<MonthPlanMouldingDayResult> wrapper = new QueryWrapper<>();
        builderCondition(wrapper, queryVO);
        List<MonthPlanMouldingDayResult> resultData = baseMapper.selectList(wrapper);
        if (!isHandler) {
            return resultData;
        }
        handlerData(resultData, productionVersion);
        return resultData;
    }

    /**
     * 导入列表
     *
     * @param list          excel解析出的数据
     * @param updateSupport 是否覆盖更新
     * @param importLogId   导入操作日志主键
     */
    @Override
    public AjaxResult doImportData(List<MonthPlanMouldingDayResult> list, boolean updateSupport, long importLogId) {
        // 只能导入一个分厂排产版本
        Set<String> factoryProductionVersionSet = list.stream().map(MonthPlanMouldingDayResult::getSameProductionVersionKey).collect(Collectors.toSet());
        if (factoryProductionVersionSet.size() > 1) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanMouldingDayResult.importSameVersion"));
        }
        //校验对应版本号在版本表是否存在，如果不存在后面需要插入一条记录
        FactoryProductionVersion productionVersion = checkInsertVersion(list);
        //根据版本信息，调整起始日，开始日及day排产量的值
//        ProductionPlanExcelUtils.handlerProductionDayQty(productionVersion, list);
        //定稿后不能导入
        Optional<MonthPlanMouldingDayResult> first = list.stream()
                .filter(v -> StringUtils.isNotBlank(v.getProductionVersion()) && v.getYear() != null && v.getMonth() != null)
                .findFirst();
        if (first.isPresent()) {
            MonthPlanMouldingDayResult item = first.get();
            if (isFinal(item.getFactoryCode(), item.getYear(), item.getMonth())) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanMouldingDayResult.checkFinal"));
            }
        }
        // 初始化
        int successNum = 0;
        int failureNum = 0;
        List<MonthPlanMouldingDayResult> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        // 国际化提示
        String repeat = I18nUtil.getMessage("ui.data.column.monthPlanMouldingDayResult.repeat");
        String productCodeNotExist = I18nUtil.getMessage("ui.data.column.monthPlanMouldingDayResult.productCode.notExist");
        String factProdReqQtyMax = I18nUtil.getMessage("ui.data.column.monthPlanMouldingDayResult.factProdReqQtyMax");
        String totalQtyCheck = I18nUtil.getMessage("ui.data.column.monthPlanMouldingDayResult.totalQtyCheck");
        String endDayCheck = I18nUtil.getMessage("ui.data.column.monthPlanMouldingDayResult.endDayCheck");
        String endDayMaxCheck = I18nUtil.getMessage("ui.data.column.monthPlanMouldingDayResult.endDayMaxCheck");

        // 唯一键分组
        Function<MonthPlanMouldingDayResult, String> keyFunc = MonthPlanMouldingDayResult::getImportDuplicateKey;
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(keyFunc, Collectors.counting()));

        // 查询对应物料信息，根据分厂+SAP代码映射
        Map<String, MdmMaterialInfo> productInfoMap = getMdmMaterialInfoMap(list);

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MonthPlanMouldingDayResult item = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, item);
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                importErrorLogs.addAll(validated);
                continue;
            }

            // 重复记录校验
            Long hasValue = groupMap.get(keyFunc.apply(item));
            if (hasValue > 1) {
                failureNum++;
                addImportErrorLog(importLogId, errorNum, repeat, importErrorLogs);
                continue;
            }

            // 实际生产需求(含损耗)必须 >= 生产需求计划、生产实际排产量
            if (item.getFactProdReqQty() < item.getProdReqPlan()) {
                failureNum++;
                addImportErrorLog(importLogId, errorNum, factProdReqQtyMax, importErrorLogs);
                continue;
            }

            // 生产实际排产量 > 0 ，模具数不能 <= 0
            if (item.getTotalQty() > 0 && item.getTypeBlockQty() <= 0) {
                failureNum++;
                addImportErrorLog(importLogId, errorNum, totalQtyCheck, importErrorLogs);
                continue;
            }

            // 开始不能大于结束时间，结束时间不能大于月份最大天数
            if (item.getBeginDay() > item.getEndDay()) {
                failureNum++;
                addImportErrorLog(importLogId, errorNum, endDayCheck, importErrorLogs);
                continue;
            }
            //TODO 先去除最大日期验证
//            int dayOfMonth = LocalDate.of(item.getYear(), item.getMonth(), 1).with(TemporalAdjusters.lastDayOfMonth()).getDayOfMonth();
//            if (item.getEndDay() > dayOfMonth) {
//                failureNum++;
//                addImportErrorLog(importLogId, errorNum, endDayMaxCheck, importErrorLogs);
//                continue;
//            }


            // 物料信息不存在跳过
//            MdmMaterialInfo productInfo = productInfoMap.get(GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getProductCode()));
//            if (productInfo == null) {
//                failureNum++;
//                addImportErrorLog(importLogId, errorNum, productCodeNotExist, importErrorLogs);
//                continue;
//            }
//            item.setProductDesc(productInfo.getMaterialDesc());
//            item.setProSize(productInfo.getProSize());
//            item.setSpecifications(productInfo.getSpecifications());
//            item.setPattern(productInfo.getPattern());
//            item.setHierarchy(productInfo.getHierarchy());
//            item.setProductTypeCode(productInfo.getProductTypeCode());
//            item.setProductTypeName(productInfo.getProductTypeName());
//            item.setBrand(productInfo.getBrand());
//            item.setIsImport(Constant.TRUE);

            importList.add(item);
        }

        try {
            //需要新增版本号记录
            if (null != productionVersion && null == productionVersion.getId()) {
                this.baseDao.insert(productionVersion);
            }
            successNum = importList.size();
            mergeByList(importList, keyFunc);


        } catch (Exception e) {
            log.error("模具排产结果汇总-导入异常", e);
            successNum = 0;
            failureNum = list.size();
            importErrorLogs.clear();
            addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
        }
        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    /**
     * 查询对应年月+分厂+需求计划版本的分厂月计划版本
     */
    @Override
    public List<String> productionVersionList(MonthPlanMouldingDayResult query) {
        if (query.getYear() == null || query.getMonth() == null || StringUtils.isBlank(query.getFactoryCode()) || StringUtils.isBlank(query.getMonthPlanVersion())) {
            return Collections.emptyList();
        }
        return baseMapper.productionVersionList(query);
    }

    /**
     * 先根据ID，获取计划，计划存在则换硫化规格代号
     * 需同步更新明细的规格代号，切计划对应的月份不可定稿
     *
     * @param changeParam 需切换的计划
     * @return
     */
    @Override
    public AjaxResult changePlanSpecCode(ChangeSpecCodeMouldingDayResultParam changeParam) {
        Long planId = changeParam.getProductionId();
        String specCode = changeParam.getSpecCode();
        if (null == planId || StringUtils.isBlank(specCode)) {
            return AjaxResult.success();
        }
        MonthPlanMouldingDayResult productionPlan = baseMapper.selectById(planId);
        if (null == productionPlan) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanMouldingDayResult.planNoExists"));
        }
        if (specCode.equals(productionPlan.getSpecCode())) {
            return AjaxResult.success();
        }
        String factoryCode = productionPlan.getFactoryCode();
        Integer year = productionPlan.getYear();
        Integer month = productionPlan.getMonth();
        if (isFinal(factoryCode, year, month)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanMouldingDayResult.checkFinal"));
        }
//        String productCode = productionPlan.getProductCode();
        String productCode = "productionPlan.getProductCode()";
        boolean isChangeSpecCode = productionPlan.getHasChangeSpecCode();
        if (!isChangeSpecCode) {
            String errorInfo = I18nUtil.getMessage("ui.data.column.monthPlanMouldingDayResult.noChangeSpecCode");
            return AjaxResult.error(String.format(errorInfo, productCode));
        }
        List<ProductSpecInfoVo> productSpecInfoList = productionPlan.getProductSpecInfos();
        Map<String, ProductSpecInfoVo> productSpecInfoMap = productSpecInfoList.stream().collect(Collectors.toMap(ProductSpecInfoVo::getSpecCode, Function.identity()));
        if (!productSpecInfoMap.containsKey(specCode)) {
            String errorInfo = I18nUtil.getMessage("ui.data.column.monthPlanMouldingDayResult.noMatchSpecCode");
            return AjaxResult.error(String.format(errorInfo, productCode, specCode));
        }
        String embryoCode = "";
        String mouldMethod = "";
        ProductSpecInfoVo productSpecInfo = productSpecInfoMap.get(specCode);
        if (null != productSpecInfo) {
            embryoCode = productSpecInfo.getEmbryoCode();
            mouldMethod = productSpecInfo.getMouldMethod();
        }
        //修改明细的硫化规格代号及生胎代码
        dayResultDetailMapper.changeSpecCode(productionPlan, specCode, embryoCode);
        productionPlan.setSpecCode(specCode);
        productionPlan.setEmbryoCode(embryoCode);
        productionPlan.setMouldMethod(mouldMethod);
        baseDao.update(productionPlan);
        return AjaxResult.success();
    }

    @Autowired
    private MonthPlanRequireMapper monthPlanRequireMapper;

    @Autowired
    private OrderPlanAllocationMapper orderPlanAllocationMapper;

    /**
     * 统计分厂月生产计划排产
     */
    @Override
    public MonthPlanStatisticsVo statistics(MonthPlanMouldingDayResult queryVO) {
        QueryWrapper<MonthPlanMouldingDayResult> queryWrapper = new QueryWrapper<>();
        builderCondition(queryWrapper, queryVO);

        MonthPlanStatisticsVo statisticsVo = new MonthPlanStatisticsVo();

        // 查询排产SAP个数、已排SAP总量
        queryWrapper.select("count(distinct PRODUCT_CODE) as productionCount,sum(TOTAL_QTY) as productionSum");
        List<Map<String, Object>> mapList = baseMapper.selectMaps(queryWrapper);
        if (CollectionUtils.isNotEmpty(mapList)) {
            Map<String, Object> resultMap = mapList.get(0);
            if (resultMap != null && resultMap.get("productionCount") != null) {
                statisticsVo.setProductionCount(Long.parseLong(resultMap.get("productionCount").toString()));
            }
            if (resultMap != null && resultMap.get("productionSum") != null) {
                statisticsVo.setProductionSum(Long.parseLong(resultMap.get("productionSum").toString()));
            }
        }

        // 统计备货量
        if (StringUtils.isNotBlank(queryVO.getProductionVersion())) {
            QueryWrapper<MonthPlanProductionResultDetail> detailWrapper = new QueryWrapper<>();
            detailWrapper.select("sum(TOTAL_QTY) as stockNum");
            commonBuilderCondition(detailWrapper, queryVO);
            detailWrapper.eq("PRODUCTION_VERSION", queryVO.getProductionVersion());
            detailWrapper.eq("IS_STOCK_UP", Constant.TRUE);
            List<Map<String, Object>> detailMapList = dayResultDetailMapper.selectMaps(detailWrapper);
            if (CollectionUtils.isNotEmpty(detailMapList)) {
                Map<String, Object> resultMap = detailMapList.get(0);
                if (resultMap != null && resultMap.get("stockNum") != null) {
                    statisticsVo.setStockNum(Long.parseLong(resultMap.get("stockNum").toString()));
                }
            }
        }

        // 根据分厂版本号查询排产结果
        QueryWrapper<MonthPlanMouldingDayResult> firstWrapper = new QueryWrapper<>();
        firstWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productionVersion")), "PRODUCTION_VERSION", queryVO.getFieldValueByFieldName("productionVersion"));
        firstWrapper.last("limit 1");
        List<MonthPlanMouldingDayResult> resultList = baseMapper.selectList(firstWrapper);
        if (CollectionUtils.isEmpty(resultList)) {
            return statisticsVo;
        }

        MonthPlanMouldingDayResult result = resultList.get(0);

        // 查询未排的SAP总量
        MonthPlanNoProductionPlan noProductionPlan = new MonthPlanNoProductionPlan();
        BeanUtils.copyProperties(queryVO, noProductionPlan);
        noProductionPlan.setMonthPlanVersion(result.getMonthPlanVersion());
        noProductionPlan.setProductionVersion(result.getProductionVersion());
        iMonthPlanNoProductionPlanService.statistics(statisticsVo, noProductionPlan);

        // 查询提报的SAP个数、提报的SAP总量
        OrderPlanAllocation orderPlanAllocation = new OrderPlanAllocation();
        BeanUtils.copyProperties(queryVO, orderPlanAllocation);
        orderPlanAllocation.setMonthPlanVersion(result.getMonthPlanVersion());
        iOrderPlanAllocationService.statistics(statisticsVo, orderPlanAllocation);

        // 销售需求总量
        QueryWrapper<SaleMonthPlanRequire> productRequirePlanWrapper = new QueryWrapper<>();
        productRequirePlanWrapper.select("sum(PLAN_QTY) as planQty");
        productRequirePlanWrapper.eq("MONTH_PLAN_VERSION", queryVO.getMonthPlanVersion());
        List<Map<String, Object>> requireMaps = monthPlanRequireMapper.selectMaps(productRequirePlanWrapper);
        if (CollectionUtils.isNotEmpty(requireMaps)) {
            Map<String, Object> resultMap = requireMaps.get(0);
            if (resultMap != null && resultMap.get("planQty") != null) {
                statisticsVo.setProdReqPlan(Long.parseLong(resultMap.get("planQty").toString()));
            }
        }

        // 净需求
        QueryWrapper<SaleMonthPlanRequire> productRequirePlanWrapper1 = new QueryWrapper<>();
        productRequirePlanWrapper1.select("sum(PLAN_QTY) as planQty");
        productRequirePlanWrapper1.eq("IS_STOCK_UP", Constant.FALSE);
        productRequirePlanWrapper1.eq("MONTH_PLAN_VERSION", queryVO.getMonthPlanVersion());
        List<Map<String, Object>> requireMaps1 = monthPlanRequireMapper.selectMaps(productRequirePlanWrapper1);
        if (CollectionUtils.isNotEmpty(requireMaps1)) {
            Map<String, Object> resultMap = requireMaps1.get(0);
            if (resultMap != null && resultMap.get("planQty") != null) {
                statisticsVo.setNetDemandQty(Long.parseLong(resultMap.get("planQty").toString()));
            }
        }
        // 缺口
        QueryWrapper<OrderPlanAllocation> orderAllocationWrapper = new QueryWrapper<>();
        orderAllocationWrapper.select("sum(PRODUCE_QTY_DUE) as planQty");
        orderAllocationWrapper.eq("MONTH_PLAN_VERSION", queryVO.getMonthPlanVersion());
        List<Map<String, Object>> allocationMaps = orderPlanAllocationMapper.selectMaps(orderAllocationWrapper);
        if (CollectionUtils.isNotEmpty(allocationMaps)) {
            Map<String, Object> resultMap = allocationMaps.get(0);
            if (resultMap != null && resultMap.get("planQty") != null) {
                statisticsVo.setGapQty(Long.parseLong(resultMap.get("planQty").toString()));
            }
        }

        return statisticsVo;
    }

    @Override
    public List<DayProductionTotalVo> statisticsDay(String productionVersion) {
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
        List<DayProductionTotalVo> dayTotalList = baseMapper.getStatisticsDay(productionVersion, days);
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

    /**
     * 有则更新，无则插入
     */
    private void mergeByList(List<MonthPlanMouldingDayResult> importList, Function<MonthPlanMouldingDayResult, String> keyFunc) {
        if (CollectionUtils.isEmpty(importList)) {
            return;
        }

        LambdaQueryWrapper<MonthPlanMouldingDayResult> wrapper = LambdaWrapperBuilder.buildWrapperByFunction(importList,
                MonthPlanMouldingDayResult::getProductionVersion
//                MonthPlanMouldingDayResult::getProductCode,
//                MonthPlanMouldingDayResult::getLocationType,
//                MonthPlanMouldingDayResult::getBrand,
//                MonthPlanMouldingDayResult::getChannel,
//                MonthPlanMouldingDayResult::getIsDeliveryDate,
//                MonthPlanMouldingDayResult::getSpecCode
        );
        List<MonthPlanMouldingDayResult> oldList = baseMapper.selectList(wrapper);
        Map<String, Long> oldMap = oldList.stream().collect(Collectors.toMap(keyFunc, MonthPlanMouldingDayResult::getId, (v1, v2) -> v1));

        List<MonthPlanMouldingDayResult> updateList = new ArrayList<>();
        List<MonthPlanMouldingDayResult> insertList = new ArrayList<>();
        for (MonthPlanMouldingDayResult item : importList) {
            String key = keyFunc.apply(item);
            if (oldMap.containsKey(key)) {
                item.setId(oldMap.get(key));
                updateList.add(item);
            } else {
                insertList.add(item);
            }
        }

        baseDao.insertBatch(insertList);
        baseDao.updateBatch(updateList);
    }

    /**
     * 校验对应版本号在版本表是否存在，如果不存在需要插入一条记录
     */

    /**
     * 检查对应版本号是否在版本表中，如果存在则返回已有版本号，
     * 如果不存在则返回需要新增的版本信息
     *
     * @param importList
     * @return
     */
    private FactoryProductionVersion checkInsertVersion(List<MonthPlanMouldingDayResult> importList) {
        Set<String> productionVersionSet = importList.stream().map(MonthPlanMouldingDayResult::getProductionVersion).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(productionVersionSet)) {
            return null;
        }
        List<FactoryProductionVersion> versionList = factoryProductionVersionMapper.selectList(Wrappers.lambdaQuery(FactoryProductionVersion.class)
                .in(FactoryProductionVersion::getProductionVersion, new ArrayList<>(productionVersionSet)));
        Map<String, FactoryProductionVersion> versionMap = versionList.stream().collect(Collectors.toMap(v -> GenerageMapKeyUtils.createMapKey(v.getProductionVersion()), Function.identity(), (v1, v2) -> v1));

        // 记录错误的年月的分厂版本号
        Set<String> errorVersionList = new HashSet<>();
        // 校验第一条即可
        MonthPlanMouldingDayResult itemResult = importList.get(0);
        String mapKey = GenerageMapKeyUtils.createMapKey(itemResult.getProductionVersion());
        FactoryProductionVersion productionVersion = versionMap.get(mapKey);
        if (productionVersion == null) {
            FactoryProductionVersion newVersion = new FactoryProductionVersion();
            newVersion.setFactoryCode(itemResult.getFactoryCode());
            newVersion.setProductTypeCode(itemResult.getProductTypeCode());
            newVersion.setYear(itemResult.getYear());
            newVersion.setMonth(itemResult.getMonth());
            newVersion.setMonthPlanVersion(itemResult.getMonthPlanVersion());
            newVersion.setProductionInitVersion(itemResult.getProductionVersion());
            newVersion.setProductionVersion(itemResult.getProductionVersion());
            //20250526 ZLT 设置排产版本的周期日期及是否非自然月标记
            factoryProductionVersionService.setProductionVersionCycleDate(newVersion);
            versionMap.put(mapKey, newVersion);
            return newVersion;
        }
        // 如果年月、分厂有一个不相同，为重复的版本号记录
        if (!StringUtils.equals(itemResult.getFactoryCode(), productionVersion.getFactoryCode())
                || !itemResult.getYear().equals(productionVersion.getYear())
                || !itemResult.getMonth().equals(productionVersion.getMonth())) {
            errorVersionList.add(itemResult.getProductionVersion());
        }
        if (CollectionUtils.isNotEmpty(errorVersionList)) {
            throw new BusinessException(String.format(I18nUtil.getMessage("ui.data.column.monthPlanMouldingDayResult.repeatVersion"),
                    String.join(",", errorVersionList)));
        }
        return productionVersion;
    }

    /**
     * 查询对应物料信息，根据分厂+SAP代码映射
     */
    private Map<String, MdmMaterialInfo> getMdmMaterialInfoMap(List<MonthPlanMouldingDayResult> list) {
        if (CollectionUtils.isEmpty(list)) {
            return new HashMap<>();
        }
        List<String> factoryCodeList = list.stream().map(MonthPlanMouldingDayResult::getFactoryCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        List<String> productCodeList = list.stream().map(MonthPlanMouldingDayResult::getMaterialCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(factoryCodeList) && CollectionUtils.isEmpty(productCodeList)) {
            return Collections.emptyMap();
        }

        LambdaQueryWrapper<MdmMaterialInfo> wrapper = Wrappers.lambdaQuery(MdmMaterialInfo.class)
                .in(CollectionUtils.isNotEmpty(factoryCodeList), MdmMaterialInfo::getFactoryCode, factoryCodeList)
                .in(CollectionUtils.isNotEmpty(productCodeList), MdmMaterialInfo::getMaterialCode, productCodeList);
        return productInfoEntityMapper.selectList(wrapper).stream().collect(Collectors.toMap(v -> GenerageMapKeyUtils.createMapKey(v.getFactoryCode(), v.getMaterialCode()), Function.identity(), (v1, v2) -> v1));
    }

    /**
     * 主表和子表通用的查询条件
     */
    protected void commonBuilderCondition(QueryWrapper<?> queryWrapper, MonthPlanMouldingDayResult queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("year")), "YEAR", queryVO.getFieldValueByFieldName("year"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month")), "MONTH", queryVO.getFieldValueByFieldName("month"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("monthPlanVersion")), "MONTH_PLAN_VERSION", queryVO.getFieldValueByFieldName("monthPlanVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productionVersion")), "PRODUCTION_VERSION", queryVO.getFieldValueByFieldName("productionVersion"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productCode")), "PRODUCT_CODE", queryVO.getFieldValueByFieldName("productCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productDesc")), "PRODUCT_DESC", queryVO.getFieldValueByFieldName("productDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("locationType")), "LOCATION_TYPE", queryVO.getFieldValueByFieldName("locationType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("channel")), "CHANNEL", queryVO.getFieldValueByFieldName("channel"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("brand")), "BRAND", queryVO.getFieldValueByFieldName("brand"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("proSize")), "PRO_SIZE", queryVO.getFieldValueByFieldName("proSize"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specifications")), "SPECIFICATIONS", queryVO.getFieldValueByFieldName("specifications"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("pattern")), "PATTERN", queryVO.getFieldValueByFieldName("pattern"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("hierarchy")), "HIERARCHY", queryVO.getFieldValueByFieldName("hierarchy"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("levelCode")), "LEVEL_CODE", queryVO.getFieldValueByFieldName("levelCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("levelName")), "LEVEL_NAME", queryVO.getFieldValueByFieldName("levelName"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productTypeCode")), "PRODUCT_TYPE_CODE", queryVO.getFieldValueByFieldName("productTypeCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productTypeName")), "PRODUCT_TYPE_NAME", queryVO.getFieldValueByFieldName("productTypeName"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isImport")), "IS_IMPORT", queryVO.getFieldValueByFieldName("isImport"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("reason")), "REASON", queryVO.getFieldValueByFieldName("reason"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mouldNo")), "MOULD_NO", queryVO.getFieldValueByFieldName("mouldNo"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specCode")), "SPEC_CODE", queryVO.getFieldValueByFieldName("specCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("embryoCode")), "EMBRYO_CODE", queryVO.getFieldValueByFieldName("embryoCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("beginDate")), "BEGIN_DATE", queryVO.getFieldValueByFieldName("beginDate"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("totalVulcanizationMinutes")), "TOTAL_VULCANIZATION_MINUTES", queryVO.getFieldValueByFieldName("totalVulcanizationMinutes"));
    }

    protected void builderCondition(QueryWrapper<MonthPlanMouldingDayResult> queryWrapper, MonthPlanMouldingDayResult queryVO) {
        commonBuilderCondition(queryWrapper, queryVO);
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isDeliveryDate")), "IS_DELIVERY_DATE", queryVO.getFieldValueByFieldName("isDeliveryDate"));
        // 查询是否排产
        if (queryVO.getIsProduction() != null) {
            if (Constant.TRUE.equals(queryVO.getIsProduction())) {
                queryWrapper.gt("TOTAL_QTY", 0L);
            } else if (Constant.FALSE.equals(queryVO.getIsProduction())) {
                queryWrapper.le("TOTAL_QTY", 0L);
            }
        }
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
        Long finalVersionCount = factoryProductionVersionMapper.selectCount(Wrappers.lambdaQuery(FactoryProductionVersion.class)
                .eq(FactoryProductionVersion::getFactoryCode, factoryCode)
                .eq(FactoryProductionVersion::getYear, year)
                .eq(FactoryProductionVersion::getMonth, month)
                .eq(FactoryProductionVersion::getIsFinal, Constant.TRUE));
        return finalVersionCount > 0;
    }

    /**
     * 对数据的开始日期和结束日期展现处理
     *
     * @param resultData        查询结果数据
     * @param productionVersion 排产版本
     */
    private void handlerData(List<MonthPlanMouldingDayResult> resultData, String productionVersion) {
        if (StringUtils.isBlank(productionVersion) || CollectionUtils.isEmpty(resultData)) {
            return;
        }
        FactoryProductionVersion version = factoryProductionVersionService.getProductionVersion(productionVersion);
        if (null == version) {
            return;
        }
        if (YesOrNoEnum.YES.getValue().equals(version.getIsNaturalMonth())) {
            return;
        }
        List<Integer> daySortList = null;
        Integer monthMaxDays = daySortList.size();
        resultData.stream().forEach(queryData -> {
            Integer startDay = queryData.getBeginDay();
            if (null != startDay && startDay <= monthMaxDays) {
                queryData.setBeginDay(daySortList.get(startDay - BigDecimal.ONE.intValue()));
            }
            Integer endDay = queryData.getEndDay();
            if (null != endDay && endDay <= monthMaxDays) {
                queryData.setEndDay(daySortList.get(endDay - BigDecimal.ONE.intValue()));
            }
        });
    }

    @Autowired
    private IFactoryParamService factoryParamService;


    /**
     * 查询分厂月生产计划合并SKU-合并SKU
     *
     * @param queryVO 查询条件
     * @return 列表
     */
    @Override
    public List<MonthPlanMouldingDayResultVo> listFacProduct(MonthPlanMouldingDayResult queryVO) {
        List<MonthPlanMouldingDayResultVo> resultList = baseMapper.listFacProduct(queryVO);
        if (CollectionUtils.isEmpty(resultList)) {
            return resultList;
        }
        // 查询分厂排产设定，参数
        FactoryParam param = new FactoryParam();
        param.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        param.setParamCode("SYS029");
        FactoryParam factoryParam = factoryParamService.getFacParamSingle(param);
        String paramValue = factoryParam.getParamValue();
        List<String> brandList = Arrays.asList(paramValue.split(","));
        Locale locale = I18nUtil.getLocaleFromRedis();
        String language = locale.toString();
        List<SysDictData> brandSysDictData = DictUtils.getDictCache("biz_brand_type");
        Map<String, String> brandDictMap = brandSysDictData.stream()
                .filter(item -> language.equals(item.getLocale())).collect(Collectors
                        .toMap(SysDictData::getDictValue, SysDictData::getDictLabel));

        // 处理开始结束日期
        handlerData4Vo(resultList, queryVO.getProductionVersion());
        return resultList;
    }

    /**
     * 对数据的开始日期和结束日期展现处理
     *
     * @param resultData        查询结果数据
     * @param productionVersion 排产版本
     */
    private void handlerData4Vo(List<MonthPlanMouldingDayResultVo> resultData, String productionVersion) {
        if (StringUtils.isBlank(productionVersion) || CollectionUtils.isEmpty(resultData)) {
            return;
        }
        FactoryProductionVersion version = factoryProductionVersionService.getProductionVersion(productionVersion);
        if (null == version) {
            return;
        }
        if (YesOrNoEnum.YES.getValue().equals(version.getIsNaturalMonth())) {
            return;
        }

    }

    /**
     * 查询月计划排产统计列表
     *
     * @param queryVO 查询条件
     * @return 列表
     */
    @Override
    public List<MonthPlanDayResultStatisticsVo> listFacProductStatistics(MonthPlanMouldingDayResult queryVO) {
        List<MonthPlanDayResultStatisticsVo> statisticsVos = baseMapper.listFacProductStatistics(queryVO);
        if (CollectionUtils.isEmpty(statisticsVos)) {
            return statisticsVos;
        }
        // 查询分厂排产设定，参数
        FactoryParam param = new FactoryParam();
        List<FactoryParam> factoryParamList = factoryParamService.getFacParamByList(param);
        List<FactoryParam> brandParamList = factoryParamList.stream().filter(item -> "SYS029".equals(item.getParamCode())).collect(Collectors.toList());
        Map<String, String> brandDictMap = new HashMap<>(16);
        List<String> brandList = new ArrayList<>();
        Locale locale = I18nUtil.getLocaleFromRedis();
        if (CollectionUtils.isNotEmpty(brandParamList)) {
            FactoryParam factoryParam = brandParamList.get(0);
            String paramValue = factoryParam.getParamValue();
            brandList = Arrays.asList(paramValue.split(","));
            String language = locale.toString();
            List<SysDictData> brandSysDictData = DictUtils.getDictCache("biz_brand_type");
            brandDictMap = brandSysDictData.stream()
                    .filter(item -> language.equals(item.getLocale())).collect(Collectors
                            .toMap(SysDictData::getDictValue, SysDictData::getDictLabel));
        }

        ProductionContext productionContext = new ProductionContext();
        Map<String, Object> paramMap = new HashMap<>(16);
        Map<String, FactoryParam> factoryParamMap = factoryParamList.stream().collect(Collectors.toMap(FactoryParam::getParamCode, Function.identity()));
        for (Map.Entry<String, FactoryParam> entry : factoryParamMap.entrySet()) {
            String paramCode = entry.getKey();
            paramMap.put(paramCode, FactoryParamUtils.getParamValue(entry.getValue()));
        }
        productionContext.setFactoryParams(paramMap);

        int currentMonth = DateUtils.getMonth(new Date());
        Object summerMonthParam = paramMap.get(FactoryConstant.SYS_PARAM_SUMMER_MONTH) == null ? 0 : paramMap.get(FactoryConstant.SYS_PARAM_SUMMER_MONTH);
        Object winterMonthParam = paramMap.get(FactoryConstant.SYS_PARAM_WINTER_MONTH) == null ? 0 : paramMap.get(FactoryConstant.SYS_PARAM_WINTER_MONTH);

        List<List<MonthPlanDayResultStatisticsVo>> splitList = ScmListUtils.getSplitList(statisticsVos, 1000);

        // 查询共用模具情况
        List<MdmSkuMouldRel> modelRelationList = mdmProductModelRelationEntityMapper.selectSameMouldNo();
        Map<String, String> modelRelationMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(modelRelationList)) {
            modelRelationMap = modelRelationList.stream().collect(Collectors.toMap(MdmSkuMouldRel::getMouldNo, MdmSkuMouldRel::getEmbryoCode));
        }
        return statisticsVos;
    }

    /**
     * 将毛利率json字段转成前端展示字段
     *
     * @param productInfoList 要转换的物料信息
     */
    public void transformJsonField(List<MonthPlanDayResultStatisticsVo> productInfoList) {
        if (CollectionUtils.isNotEmpty(productInfoList)) {
            for (MonthPlanDayResultStatisticsVo statisticsVo : productInfoList) {
                String grossRateJson = statisticsVo.getGrossRateJson();
                if (StringUtils.isNotBlank(grossRateJson)) {
                    List<MaterialInfoGrossRateJsonVo> materialInfoGrossRateJsonVoList = JSON.parseArray(grossRateJson, MaterialInfoGrossRateJsonVo.class);
                    for (MaterialInfoGrossRateJsonVo materialInfoGrossRateJsonVo : materialInfoGrossRateJsonVoList) {
                        Object fieldValue = ReflectUtils.getFieldValue(materialInfoGrossRateJsonVo, "grossRate");
                        String commonType = materialInfoGrossRateJsonVo.getCommonType();
                        String fieldNameByCommonType = CommonTypeEnum.getFieldNameByCommonType(Integer.valueOf(commonType));
                        ReflectUtils.setFieldValue(statisticsVo, fieldNameByCommonType, fieldValue);

                    }
                }
            }
        }
    }
}
