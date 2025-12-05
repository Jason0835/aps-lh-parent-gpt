package com.zlt.aps.monthplan.factory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.Constant;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.constant.StringConstant;
import com.tlt.aps.enums.*;
import com.tlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.service.IMonthPlanProductionSchedulingService;
import com.zlt.aps.maindata.service.IFactoryParamService;
import com.zlt.aps.maindata.service.IMdmProductInfoService;
import com.zlt.aps.maindata.service.IPlanOrderSortConfigurationService;
import com.zlt.aps.maindata.service.IProductMinConfigurationService;
import com.zlt.aps.maindata.utils.FactoryParamUtils;
import com.zlt.aps.monthplan.api.domain.dto.ProductStockInfo;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.aps.monthplan.api.domain.vo.*;
import com.zlt.aps.monthplan.demand.mapper.MonthPlanSaleOrderMapper;
import com.zlt.aps.monthplan.demand.mapper.SaleMonthPlanRequireMapper;
import com.zlt.aps.monthplan.demand.mapper.SaleMonthPlanRequireStockMapper;
import com.zlt.aps.monthplan.demand.service.IProductStockMonthService;
import com.zlt.aps.monthplan.enums.StockHedgingComparatorEnum;
import com.zlt.aps.monthplan.factory.dto.FactoryProductionPlanVersionDto;
import com.zlt.aps.monthplan.factory.dto.YearSaleMinProdVo;
import com.zlt.aps.monthplan.factory.helper.SaleRequirePlanHelper;
import com.zlt.aps.monthplan.factory.mapper.FactoryConsoleMapper;
import com.zlt.aps.monthplan.factory.mapper.FactoryProductionVersionMapper;
import com.zlt.aps.monthplan.factory.mapper.MdmStockUpPlanMapper;
import com.zlt.aps.monthplan.factory.service.IFactoryConsoleService;
import com.zlt.aps.monthplan.factory.service.IFactoryMonthPlanProdFinalService;
import com.zlt.aps.monthplan.factory.service.IMdmProductionGenerateService;
import com.zlt.aps.monthplan.factory.service.IMdmStockUpPlanService;
import com.zlt.aps.monthplan.mdm.service.IEstimateExceedShortService;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分厂控制台业务实现
 *
 * @author ZLT
 * @date 20250213
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FactoryConsoleServiceImpl implements IFactoryConsoleService {

    private final MdmStockUpPlanMapper stockUpPlanMapper;

    private final MonthPlanSaleOrderMapper monthPlanSaleOrderMapper;

    private final SaleMonthPlanRequireMapper saleMonthPlanRequireMapper;

    private final SaleMonthPlanRequireStockMapper saleMonthPlanRequireStockMapper;

    private final FactoryProductionVersionMapper factoryProductionVersionMapper;

    private final FactoryConsoleMapper factoryConsoleMapper;

    private final BaseDao baseDao;

    private final IMdmStockUpPlanService stockUpPlanService;

    private final IProductStockMonthService productStockMonthService;

    private final IEstimateExceedShortService estimateExceedShortService;

    private final IProductMinConfigurationService productMinConfigurationService;

    private final IPlanOrderSortConfigurationService planOrderSortConfigurationService;

    private final IMdmProductionGenerateService iMdmProductionGenerateService;

    private final IFactoryParamService iFactoryParamService;

    private final IMonthPlanProductionSchedulingService monthPlanProductionSchedulingService;

    private final IMdmProductInfoService iMdmProductInfoService;

    private final IFactoryMonthPlanProdFinalService factoryMonthPlanProdFinalService;

    /**
     * 不加超欠产量
     */
    private final static String NO_ADD_SHORT = "N";
    /**
     * 是否自动生成备货计划
     */
    private final static String IS_AUTO_CREATE_STOCK_UP_PLAN = "Y";

    @Override
    public List<FactoryProductionPlanVersionDto> getProductionVersionList(FactoryProductionPlanVo queryCondition) {
        if (null == queryCondition) {
            return Collections.emptyList();
        }
        if (null == queryCondition.getYear() || null == queryCondition.getMonth() || StringUtils.isBlank(queryCondition.getFactoryCode())) {
            return Collections.emptyList();
        }
        return factoryConsoleMapper.getProductionVersionList(queryCondition);
    }

    @Override
    public List<FactoryMonthPlanVersionVo> getNoSelectedVersionList(FactoryProductionPlanVo queryCondition) {
        if (null == queryCondition) {
            return Collections.emptyList();
        }
        if (null == queryCondition.getYear() || null == queryCondition.getMonth() || StringUtils.isBlank(queryCondition.getFactoryCode())) {
            return Collections.emptyList();
        }
        return factoryConsoleMapper.getNoSelectedVersionList(queryCondition);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult createSaleRequirePlan(MonthPlanSaleRequirePlanVo createCondition) {
        // 如果已经定稿，不能重新生成销售需求计划
        if (factoryProductionVersionMapper.selectCount(Wrappers.lambdaQuery(FactoryProductionVersion.class)
                .eq(FactoryProductionVersion::getFactoryCode, createCondition.getFactoryCode())
                .eq(FactoryProductionVersion::getYear, createCondition.getYear())
                .eq(FactoryProductionVersion::getMonth, createCondition.getMonth())
                .eq(FactoryProductionVersion::getIsFinal, Constant.TRUE)) > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.saleRequirePlan.checkFinal"));
        }
        String factoryCode = createCondition.getFactoryCode();
        Integer year = createCondition.getYear();
        Integer month = createCondition.getMonth();
        YearMonth requirePlanDate = YearMonth.of(year, month);
        YearMonth lastMothDate = requirePlanDate.minusMonths(1);
        String monthPlanVersion = DateUtils.dateTimeNow();
        QueryWrapper condition = new QueryWrapper();
        condition.eq("FACTORY_CODE", factoryCode);
        condition.eq("YEAR", year);
        condition.eq("MONTH", month);
        Long count = monthPlanSaleOrderMapper.selectCount(condition);
        if (count < 1) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.saleRequirePlan.noSaleOrder"));
        }
        //库存对冲顺序配置
        List<PlanOrderSortConfiguration> sortConfigurationList = planOrderSortConfigurationService.getStockHedgingConfiguration();
        if (CollectionUtils.isEmpty(sortConfigurationList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.saleRequirePlan.stockSort"));
        }
        //20250506 总备货阀值 = 库容阀值 - 月结库存 + 对冲库存量 获取库容阀值
        Integer storageCapacityThreshold = getStorageCapacityThreshold(factoryCode);
        if (storageCapacityThreshold == null || storageCapacityThreshold <= 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.saleRequirePlan.storageCapacityThreshold"));
        }
        //20250506 生成备货计划，参照上个月的配置
        autoCreateStockUpPlan(factoryCode, requirePlanDate);
        //20250427 根据近12个月总销量值更新上调控制水位值
        updateMinProdUpQty(factoryCode, lastMothDate.minusYears(1));
        Map<String, Map<SortHierarchyEnum, List<PlanOrderSortConfiguration>>> factoryGroupMap = getGroupStockHedgingConfiguration(sortConfigurationList);
        //更新补充物料基础数据、重要客户及必保计划标记
        monthPlanSaleOrderMapper.updateProductInfo(createCondition);
        monthPlanSaleOrderMapper.updateImportantCustomFlag(createCondition);
        //20250911 ZLT 必保采用订单导入直接使用
        // 生成超欠产
        iMdmProductionGenerateService.generateEstimateExceedShort(lastMothDate.getYear(), lastMothDate.getMonthValue(), createCondition.getFactoryCode());
        // 20250427 ZLT 计划时是否加入超欠产
        boolean isAddShort = isAddShort(factoryCode);
        //重新获取销售订单数据，并根据分厂+物料维度分组
        List<MonthPlanSaleOrder> saleOrderList = monthPlanSaleOrderMapper.selectList(condition);
        Map<String, List<MonthPlanSaleOrder>> saleOrderGroupMap = SaleRequirePlanHelper.getGroupOrder(saleOrderList, factoryGroupMap);
        //获取上个月度库存信息，并根据分厂+物料维度分组
        MonthPlanSaleRequirePlanVo lastMonthCondition = new MonthPlanSaleRequirePlanVo();
        lastMonthCondition.setFactoryCode(createCondition.getFactoryCode());
        lastMonthCondition.setYear(lastMothDate.getYear());
        lastMonthCondition.setMonth(lastMothDate.getMonthValue());
        List<ProductStockMonth> monthStockList = productStockMonthService.getMothStock(lastMonthCondition);
        Map<String, ProductStockInfo> stockReverseMap = SaleRequirePlanHelper.getProductMonthStock(monthStockList);
        //按照库存冲销顺序进行对冲
        List<OrderPlanAllocation> allocationList = calculateStockAllocation(monthPlanVersion, factoryGroupMap, saleOrderGroupMap, stockReverseMap);
        //保存库存分配结果
        baseDao.insertBatch(allocationList);
        //保存版本库存信息--月结库存及剩余库存量
        List<MonthPlanRequireStock> monthPlanRequireStockList = SaleRequirePlanHelper.buildRequireStock(stockReverseMap, monthPlanVersion);
        baseDao.insertBatch(monthPlanRequireStockList);
        saleMonthPlanRequireStockMapper.updateProductInfo(factoryCode, monthPlanVersion);
        //20250506 ZLT 计算总备货阀值: 总备货阀值 = 库容阀值 - （库存冲销后剩余的月结库存） = 库容阀值 - 月结库存 + 冲销库存总量
        Long totalStockThreshold = calculateStockThreshold(factoryCode, storageCapacityThreshold, allocationList, monthStockList);
        //销售提报量
        Map<String, Long> submissionQtyMap = getSubmissionQtyGroup(saleOrderGroupMap);
        //记录分厂+物料-对应分厂+胎别
        Map<String, String> productTypeCodeMap = saleOrderList.stream()
                .collect(Collectors.toMap(MonthPlanSaleOrder::getGroupKey, v -> getProductTypeCodeKey(v.getFactoryCode(), v.getProductTypeCode()), (v1, v2) -> v1));
        //最小批量配置（分厂+物料）、最小批量通配符配置（分厂+胎别）
        Map<String, ProductMinConfiguration> minConfigurationMap = new HashMap<>();
        Map<String, ProductMinConfiguration> minWildcardConfigMap = new HashMap<>();
        getMinConfigurationGroup(minConfigurationMap, minWildcardConfigMap);
        //获取备货量
        List<MdmStockUpPlan> stockUpPlanList = stockUpPlanService.getStockUpByYearAndMonth(year, month);
        //获取上个月的预计欠产量
        List<EstimateExceedShort> estimateExceedShortList = estimateExceedShortService.getEstimateExceedShortByYearAndMonth(lastMonthCondition.getYear(), lastMonthCondition.getMonth());
        //计算排产需求量，提报数量需大于等于最小批量的上调控制水位，再之后需要生产量 - 预计欠产量 +备货量 小于最小批量，则理论生产量 = 最小批量，否则 理论生产量 = 需要生产量 + 备货量 - 预计欠产量
        List<SaleMonthPlanRequire> requireResultList = getSaleMonthPlanRequire(factoryCode, isAddShort, monthPlanVersion, submissionQtyMap, minConfigurationMap, minWildcardConfigMap, productTypeCodeMap, stockUpPlanList, estimateExceedShortList, allocationList, totalStockThreshold, stockReverseMap);
        baseDao.insertBatch(requireResultList);
        //保存版本信息
        insertProductionVersion(createCondition.getFactoryCode(), year, month, monthPlanVersion, saleOrderList);
        return AjaxResult.success();
    }

    @Override
    public AjaxResult factoryWholeCourseProduction(FactoryProductionParamVo factoryProductionParam) {
        String factoryCode = factoryProductionParam.getFactoryCode();
        Integer year = factoryProductionParam.getYear();
        Integer month = factoryProductionParam.getMonth();
        String monthPlanVersion = factoryProductionParam.getMonthPlanVersion();
        if (StringUtils.isBlank(factoryCode) || null == year || null == month || StringUtils.isBlank(monthPlanVersion)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.requireVersionNoEmpty"));
        }
        QueryWrapper<FactoryProductionVersion> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", factoryCode);
        queryWrapper.eq("YEAR", year);
        queryWrapper.eq("MONTH", month);
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        queryWrapper.eq("IS_FINAL", YesOrNoEnum.YES.getValue());
        FactoryProductionVersion version = factoryProductionVersionMapper.selectOne(queryWrapper);
        if (null != version) {
            //分厂在%s-%s年月已定稿，不可重新排产
            String factoryIsFinalVersion = I18nUtil.getMessage("ui.data.query.param.factoryIsFinalVersion");
            return AjaxResult.error(String.format(factoryIsFinalVersion, year, month));
        }
        queryWrapper.clear();
        queryWrapper.eq("FACTORY_CODE", factoryCode);
        queryWrapper.eq("YEAR", year);
        queryWrapper.eq("MONTH", month);
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        queryWrapper.eq("MONTH_PLAN_VERSION", monthPlanVersion);
        List<FactoryProductionVersion> requireVersionList = factoryProductionVersionMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(requireVersionList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.noExistVersion"));
        }
        Context context = buildContext(factoryProductionParam);
        monthPlanProductionSchedulingService.general(context);
        return AjaxResult.success();
    }

    @Override
    public AjaxResult reinitializeMouldingProduction(FactoryProductionParamVo factoryProductionParam) {
        AjaxResult checkResult = checkParam(factoryProductionParam);
        //校验没通过
        if (AjaxResult.Type.ERROR.value() == (Integer) checkResult.get(AjaxResult.CODE_TAG)) {
            return checkResult;
        }
        Context context = buildContext(factoryProductionParam);
        monthPlanProductionSchedulingService.init(context);
        return AjaxResult.success();
    }

    @Override
    public AjaxResult reMouldingProduction(FactoryProductionParamVo factoryProductionParam) {
        AjaxResult checkResult = checkParam(factoryProductionParam);
        //校验没通过
        if (AjaxResult.Type.ERROR.value() == (Integer) checkResult.get(AjaxResult.CODE_TAG)) {
            return checkResult;
        }
        Context context = buildContext(factoryProductionParam);
        monthPlanProductionSchedulingService.mouldingScheduling(context);
        return AjaxResult.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult deleteMonthPlanRequire(FactoryProductionParamVo factoryProductionParam) {
        factoryProductionParam.setProductionVersion(null);
        boolean hasFinalVersion = isHasFinalVersion(factoryProductionParam);
        if (hasFinalVersion) {
            return AjaxResult.error(I18nUtil.getMessage("ui.check.monthPlan.finalVersion"));
        }
        factoryProductionVersionMapper.deletedMonthPlanRequireVersion(factoryProductionParam);
        factoryProductionVersionMapper.deletedProductionVersion(factoryProductionParam);
        return AjaxResult.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult deleteMonthPlanProductionVersion(FactoryProductionParamVo factoryProductionParam) {
        boolean isFinalVersion = isHasFinalVersion(factoryProductionParam);
        if (isFinalVersion) {
            return AjaxResult.error(I18nUtil.getMessage("ui.check.monthPlan.isFinalVersion"));
        }
        QueryWrapper<FactoryProductionVersion> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", factoryProductionParam.getFactoryCode());
        queryWrapper.eq("YEAR", factoryProductionParam.getYear());
        queryWrapper.eq("MONTH", factoryProductionParam.getMonth());
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        String monthPlanVersion = factoryProductionParam.getMonthPlanVersion();
        queryWrapper.eq("MONTH_PLAN_VERSION", monthPlanVersion);
        Long count = factoryProductionVersionMapper.selectCount(queryWrapper);
        if (count > 1) {
            factoryProductionVersionMapper.deletedProductionVersion(factoryProductionParam);
        } else {
            factoryProductionVersionMapper.deletedProductionVersionByLast(factoryProductionParam);
        }
        return AjaxResult.success();
    }

    /**
     * 20250506 自动生成备货计划
     * 如果已经有备货计划，则不自动生成
     * 否则取上个月的备货规则(近几个月)生成备份计划
     * 取备货月数
     *
     * @param factoryCode
     * @param yearMonth
     */
    private void autoCreateStockUpPlan(String factoryCode, YearMonth yearMonth) {
        Integer year = yearMonth.getYear();
        Integer month = yearMonth.getMonthValue();
        FactoryParam factoryParam = new FactoryParam();
        factoryParam.setFactoryCode(factoryCode);
        factoryParam.setParamCode(FactoryConstant.SYS_PARAM_IS_AUTO_CREATE_STOCK_UP);
        FactoryParam isAutoCreateParam = iFactoryParamService.getFacParamSingle(factoryParam);
        if (null == isAutoCreateParam || StringUtils.isBlank(isAutoCreateParam.getParamValue())) {
            log.info(String.format("没有开启自动生成备货计划功能，无需自动生成%s-%s备货计划", year, month));
            return;
        }
        if (!IS_AUTO_CREATE_STOCK_UP_PLAN.equalsIgnoreCase(isAutoCreateParam.getParamValue())) {
            log.info(String.format("没有开启自动生成备货计划功能，无需自动生成%s-%s备货计划", year, month));
            return;
        }
        QueryWrapper<MdmStockUpPlan> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", factoryCode);
        queryWrapper.eq("YEAR", year);
        queryWrapper.eq("MONTH", month);
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        Long exist = stockUpPlanMapper.selectCount(queryWrapper);
        if (exist > BigDecimal.ZERO.intValue()) {
            log.info(String.format("当前已经有备货计划，无需自动生成%s-%s备货计划", year, month));
            return;
        }
        //按上个月的规则 自动生成
        YearMonth lastMothDate = yearMonth.minusMonths(1);
        QueryWrapper<MdmStockUpPlan> lastQuery = new QueryWrapper<>();
        lastQuery.eq("FACTORY_CODE", factoryCode);
        lastQuery.eq("YEAR", lastMothDate.getYear());
        lastQuery.eq("MONTH", lastMothDate.getMonthValue());
        lastQuery.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        List<MdmStockUpPlan> planList = stockUpPlanMapper.selectList(lastQuery);
        if (CollectionUtils.isEmpty(planList)) {
            log.info(String.format("没有上个月的备货计划，故而不能自动生成%s-%s备货计划", year, month));
            return;
        }
        MdmStockUpPlan lastMonth = planList.get(0);
        Integer monthRange = lastMonth.getAverageType();
        if (null == monthRange) {
            log.info(String.format("上个月的备货计划没有备货规则，故而不能自动生成%s-%s备货计划", year, month));
            return;
        }
        QueryCalcStockingParamVo createCondition = new QueryCalcStockingParamVo();
        createCondition.setMonthRange(Long.valueOf(monthRange));
        createCondition.setFactoryCode(factoryCode);
        stockUpPlanService.createStockUpPlan(createCondition);
    }

    /**
     * 更新最小批量中的上调控制水位值
     * 根据系统参数SYS036的值，近12个月销量总值大于该值，则上调控制水位值 = 1，
     * 否则如果上调控制水位值 = 1 则更新为SYS040的值
     *
     * @param factoryCode 分厂编码
     * @param yearMonth   年份、月份
     */
    private void updateMinProdUpQty(String factoryCode, YearMonth yearMonth) {
        if (StringUtils.isBlank(factoryCode) || null == yearMonth) {
            return;
        }
        FactoryParam paramCondition = new FactoryParam();
        paramCondition.setFactoryCode(factoryCode);
        paramCondition.setProductTypeCode(ProductTypeEnum.SEMI_STEEL.getValue());
        paramCondition.setParamCode(FactoryConstant.SALE_TOTAL_QTY);
        FactoryParam saleTotalQtyParam = iFactoryParamService.getFacParamSingle(paramCondition);
        if (null == saleTotalQtyParam || StringUtils.isBlank(saleTotalQtyParam.getParamValue())) {
            return;
        }
        paramCondition.setParamCode(FactoryConstant.DEFAULT_UP_WATER_LEVEL);
        FactoryParam defaultUpQtyParam = iFactoryParamService.getFacParamSingle(paramCondition);
        if (null == defaultUpQtyParam || StringUtils.isBlank(defaultUpQtyParam.getParamValue())) {
            return;
        }
        Integer subtractMonth = iFactoryParamService.getStockUpLastMonth(factoryCode);
        //20250521 ZLT 会出现可能需要跨月提前值 ，因为近一个月月数据没有或是没有意义
        if (null != subtractMonth && subtractMonth > BigDecimal.ZERO.intValue()) {
            LocalDate date = LocalDate.of(yearMonth.getYear(), yearMonth.getMonthValue(), FactoryConstant.MONTH_START_DAY);
            date = date.minusMonths(subtractMonth);
            yearMonth = YearMonth.of(date.getYear(), date.getMonthValue());
        }
        Integer saleTotalQty = (Integer) FactoryParamUtils.getParamValue(saleTotalQtyParam);
        defaultUpQtyParam.setDataType(SysParamDataTypeEnum.INTEGER.getValue());
        Integer defaultUpQty = (Integer) FactoryParamUtils.getParamValue(defaultUpQtyParam);
        Integer year = yearMonth.getYear();
        Integer month = yearMonth.getMonthValue();
        //年销量超过saleTotalQty，又没有配置最小批量，则需要新增的最小批量配置
        List<YearSaleMinProdVo> needInsertList = monthPlanSaleOrderMapper.getNeedInsertProductMinConfigurationList(factoryCode, year, month, saleTotalQty);
        List<ProductMinConfiguration> insertMinConfigurationList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(needInsertList)) {
            needInsertList.stream().forEach(needInsert -> {
                ProductMinConfiguration minConfiguration = new ProductMinConfiguration();
                minConfiguration.setFactoryCode(factoryCode);
                minConfiguration.setProductCode(needInsert.getProductCode());
                minConfiguration.setProductDesc(needInsert.getProductDesc());
                //上调控制水位 = 1
                minConfiguration.setUpQty(BigDecimal.ONE.intValue());
                minConfiguration.setMinQty(defaultUpQty);
                //先默认为PCR
                minConfiguration.setProductType(ProductTypeEnum.SEMI_STEEL.getValue());
                insertMinConfigurationList.add(minConfiguration);
            });

        }
        //更新近12个月销售总量大于saleTotalQty的物料上调控制水位 =1
        monthPlanSaleOrderMapper.updateMinProdUpQtyToOne(factoryCode, year, month, saleTotalQty);
        //更新近12个月销售总量小于等于saleTotalQty的物料且上调控制水位 =1 的记录 上调控制水位=defaultUpQty
        monthPlanSaleOrderMapper.updateMinProdUpQtyToDefault(factoryCode, year, month, saleTotalQty, defaultUpQty);
        if (CollectionUtils.isEmpty(insertMinConfigurationList)) {
            return;
        }
        //年销量超过saleTotalQty，又没有配置最小批量，则需要新增的最小批量配置
        baseDao.insertBatch(insertMinConfigurationList);
    }

    /**
     * 保存版本信息
     */
    private void insertProductionVersion(String factoryCode, Integer year, Integer month, String monthPlanVersion, List<MonthPlanSaleOrder> saleOrderList) {
        FactoryProductionVersion version = new FactoryProductionVersion();
        version.setFactoryCode(factoryCode);
        version.setYear(year);
        version.setMonth(month);
        version.setMonthPlanVersion(monthPlanVersion);
        version.setIsFinal(Constant.FALSE);
        // 取销售订单的胎别 
        if (!CollectionUtils.isEmpty(saleOrderList)) {
            MonthPlanSaleOrder saleOrder = saleOrderList.get(0);
            version.setProductTypeCode(saleOrder.getProductTypeCode());
            version.setProductTypeName(saleOrder.getProductTypeName());
        }
        factoryProductionVersionMapper.insert(version);
    }

    /**
     * 按分厂+层级维度，获取库存对冲顺序分组信息
     *
     * @return
     */
    private Map<String, Map<SortHierarchyEnum, List<PlanOrderSortConfiguration>>> getGroupStockHedgingConfiguration(List<PlanOrderSortConfiguration> sortConfigurationList) {
        Map<String, Map<SortHierarchyEnum, List<PlanOrderSortConfiguration>>> factoryGroupMap = new HashMap<>();
        if (CollectionUtils.isEmpty(sortConfigurationList)) {
            return factoryGroupMap;
        }
        sortConfigurationList.stream().forEach(sortConfiguration -> {
            String key = sortConfiguration.getFactoryCode();
            Map<SortHierarchyEnum, List<PlanOrderSortConfiguration>> factoryConfiguration = factoryGroupMap.get(key);
            if (null == factoryConfiguration) {
                factoryConfiguration = new HashMap<>();
                factoryConfiguration.put(SortHierarchyEnum.FIRST_HIERARCHY, new ArrayList<>());
                factoryConfiguration.put(SortHierarchyEnum.SECOND_HIERARCHY, new ArrayList<>());
            }
            Integer hierarchy = sortConfiguration.getHierarchy();
            SortHierarchyEnum sortHierarchy = SortHierarchyEnum.getInstance(hierarchy);
            if (null == sortHierarchy) {
                return;
            }
            factoryConfiguration.get(sortHierarchy).add(sortConfiguration);
            factoryGroupMap.put(key, factoryConfiguration);
        });
        return factoryGroupMap;
    }

    /**
     * 判断是否存在定稿版本，存在则返回false
     *
     * @param factoryProductionParam
     * @return
     */
    private boolean isHasFinalVersion(FactoryProductionParamVo factoryProductionParam) {
        QueryWrapper<FactoryProductionVersion> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", factoryProductionParam.getFactoryCode());
        queryWrapper.eq("YEAR", factoryProductionParam.getYear());
        queryWrapper.eq("MONTH", factoryProductionParam.getMonth());
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        queryWrapper.eq("IS_FINAL", YesOrNoEnum.YES.getValue());
        String monthPlanVersion = factoryProductionParam.getMonthPlanVersion();
        String productionVersion = factoryProductionParam.getProductionVersion();
        if (!StringUtils.isBlank(monthPlanVersion)) {
            queryWrapper.eq("MONTH_PLAN_VERSION", monthPlanVersion);
        }
        if (!StringUtils.isBlank(productionVersion)) {
            queryWrapper.eq("PRODUCTION_VERSION", productionVersion);
        }
        Long count = factoryProductionVersionMapper.selectCount(queryWrapper);
        return count > 0;
    }

    /**
     * 根据库存对冲顺序配置，进行库存分配
     *
     * @param monthPlanVersion  版本计划
     * @param factoryGroupMap   按分厂分组的库存对冲顺序配置
     * @param saleOrderGroupMap 按分厂+物料分组维度的销售提报订单
     * @param stockReverseMap   按分厂+物料分组维度的库存信息
     * @return
     */
    private List<OrderPlanAllocation> calculateStockAllocation(String monthPlanVersion, Map<String, Map<SortHierarchyEnum, List<PlanOrderSortConfiguration>>> factoryGroupMap, Map<String, List<MonthPlanSaleOrder>> saleOrderGroupMap, Map<String, ProductStockInfo> stockReverseMap) {
        List<OrderPlanAllocation> allocationList = new ArrayList<>();
        saleOrderGroupMap.forEach((key, groupSaleOrder) -> {
            ProductStockInfo stockInfo = stockReverseMap.get(key);
            //没有库存
            if (null == stockInfo) {
                groupSaleOrder.stream().forEach(single -> {
                    OrderPlanAllocation allocation = new OrderPlanAllocation();
                    BeanUtils.copyProperties(single, allocation);
                    allocation.setId(null);
                    allocation.setCommonType(single.getCommonType());
                    allocation.setMonthPlanVersion(monthPlanVersion);
                    allocation.setAllocationQty(BigDecimal.ZERO.longValue());
                    allocation.setProduceQtyDue(allocation.getPlanQty() - allocation.getAllocationQty());
                    allocationList.add(allocation);
                });
                return;
            }
            String factoryCode = stockInfo.getFactoryCode();
            //根据库存对冲顺序配置进行排序对冲
            List<PlanOrderSortConfiguration> sortConfigurations = factoryGroupMap.get(factoryCode).get(SortHierarchyEnum.FIRST_HIERARCHY);
            Comparator comparatorConfiguration = getComparator(sortConfigurations);
            List<MonthPlanSaleOrder> sortList = (List<MonthPlanSaleOrder>) groupSaleOrder.stream().sorted(comparatorConfiguration).collect(Collectors.toList());
            Long sum = stockInfo.getSumStockQty();
            for (MonthPlanSaleOrder saleOrder : sortList) {
                OrderPlanAllocation allocation = new OrderPlanAllocation();
                BeanUtils.copyProperties(saleOrder, allocation);
                allocation.setId(null);
                allocation.setCommonType(saleOrder.getCommonType());
                allocation.setMonthPlanVersion(monthPlanVersion);
                Long planQty = saleOrder.getPlanQty();
                if (sum > 0) {
                    //可满足计划量
                    if (sum >= planQty) {
                        allocation.setAllocationQty(planQty);
                        sum = sum - planQty;
                    } else {
                        //不满足计划量
                        allocation.setAllocationQty(sum);
                        sum = 0L;
                    }
                } else {
                    allocation.setAllocationQty(BigDecimal.ZERO.longValue());
                }
                allocation.setProduceQtyDue(allocation.getPlanQty() - allocation.getAllocationQty());
                allocationList.add(allocation);
            }
            stockInfo.setLeftOverQty(sum);
        });
        return allocationList;
    }

    /**
     * 计算总备货阀值：总备货阀值 = 库容阀值 - 月结库存 + 库存冲销量(即库存分配量)
     *
     * @param factoryCode              分厂编码
     * @param storageCapacityThreshold 库容阀值
     * @param allocationList           库存冲销结果
     * @param monthStockList           月结库存
     * @return
     */
    private Long calculateStockThreshold(String factoryCode, Integer storageCapacityThreshold, List<OrderPlanAllocation> allocationList, List<ProductStockMonth> monthStockList) {
        Long sumAllocationQty = getTotalAllocationQty(factoryCode, allocationList);
        Long monthStockQty = getTotalMonthStockQty(factoryCode, monthStockList);
        log.info(String.format("库容阀值：%d; 月结库存：%d; 库存冲销总量：%d", storageCapacityThreshold, monthStockQty, sumAllocationQty));
        return Long.valueOf(storageCapacityThreshold) - monthStockQty + sumAllocationQty;
    }

    /**
     * 获取总的库存冲销值
     *
     * @param factoryCode    分厂编码
     * @param allocationList 库存冲销明细
     * @return
     */
    private Long getTotalAllocationQty(String factoryCode, List<OrderPlanAllocation> allocationList) {
        if (CollectionUtils.isEmpty(allocationList) || StringUtils.isBlank(factoryCode)) {
            return BigDecimal.ZERO.longValue();
        }
        Long totalQty = BigDecimal.ZERO.longValue();
        for (OrderPlanAllocation orderPlanAllocation : allocationList) {
            if (!factoryCode.equals(orderPlanAllocation.getFactoryCode())) {
                continue;
            }
            Long allocationQty = orderPlanAllocation.getAllocationQty();
            if (null == allocationQty) {
                allocationQty = BigDecimal.ZERO.longValue();
            }
            if (allocationQty > BigDecimal.ZERO.longValue()) {
                totalQty = totalQty + allocationQty;
            }
        }
        return totalQty;
    }

    /**
     * 获取月结库存：库存>0的值
     *
     * @param factoryCode    分厂编码
     * @param monthStockList 月结库存信息
     * @return
     */
    private Long getTotalMonthStockQty(String factoryCode, List<ProductStockMonth> monthStockList) {
        if (CollectionUtils.isEmpty(monthStockList) || StringUtils.isBlank(factoryCode)) {
            return BigDecimal.ZERO.longValue();
        }
        Long totalStockQty = BigDecimal.ZERO.longValue();
        for (ProductStockMonth monthStock : monthStockList) {
            if (!factoryCode.equals(monthStock.getFactoryCode())) {
                continue;
            }
            Integer stockQty = monthStock.getStockQty();
            if (null == stockQty) {
                stockQty = BigDecimal.ZERO.intValue();
            }
            if (stockQty > BigDecimal.ZERO.intValue()) {
                totalStockQty = totalStockQty + stockQty;
            }
        }
        return totalStockQty;
    }

    /**
     * 按分厂 + 物料 分组汇总提报量
     *
     * @param saleOrderGroupMap
     * @return
     */
    private Map<String, Long> getSubmissionQtyGroup(Map<String, List<MonthPlanSaleOrder>> saleOrderGroupMap) {
        Map<String, Long> submissionQtyGroup = new HashMap<>();
        saleOrderGroupMap.forEach((key, groupList) -> {
            Long sumQty = BigDecimal.ZERO.longValue();
            for (MonthPlanSaleOrder single : groupList) {
                sumQty = sumQty + single.getPlanQty();
            }
            submissionQtyGroup.put(key, sumQty);
        });
        return submissionQtyGroup;
    }

    /**
     * 获取最小批量设置
     * 非通配符福物料按照 分厂+物料 分组
     * 通配符福物料按照 分厂+胎别 分组
     *
     * @param minConfigurationMap  最小批量配置（分厂+物料）
     * @param minWildcardConfigMap 、最小批量通配符配置（分厂+胎别）
     */
    private void getMinConfigurationGroup(Map<String, ProductMinConfiguration> minConfigurationMap,
                                          Map<String, ProductMinConfiguration> minWildcardConfigMap) {
        List<ProductMinConfiguration> configurationList = productMinConfigurationService.getConfigurationList();
        if (CollectionUtils.isEmpty(configurationList)) {
            return;
        }
        configurationList.stream().forEach(configuration -> {
            if (StringUtils.isNotBlank(configuration.getProductCode()) && StringConstant.ALL_MATCH.equals(configuration.getProductCode())) {
                // 如果是通配符配置
                minWildcardConfigMap.put(configuration.getWildcardKey(), configuration);
            } else {
                // 正常物料配置
                minConfigurationMap.put(configuration.getGroupKey(), configuration);
            }
        });
    }

    /**
     * （1）获取最终的生产需求计划
     * 1、根据 分厂 + 物料编码 分组的销售订单提报总量
     * 2、根据 分厂 + 物料编码 或者 分厂+（通配符物料编码）+胎别 分组的上调控制水位 及 分组的最小批量
     * 当 提报总量 < 上调控制水位，则不生成，生产需求计划量 = 0
     * 当 提报总量 > 上调控制水位时
     * 根据 分厂 + 物料编码 + 库位 分别计算对应的备货量，根据计划排产量计算最小批量差值
     * 目前只有达到上调控制水位，对应计划有提报对应分厂+物料+库位的欠产和备货会被考虑计算
     * 1、外销/OE：销售需求计划=净需求-欠产量+备货量
     * 2、内销：
     * A （净需求-欠产量）>=理论备货量，不进行备货，销售需求计划=净需求-欠产量
     * B、（净需求-欠产量）<理论备货量，上调至理论备货量，实际备货量=理论备货量-（净需求-欠产量），销售需求计划=理论备货量
     * （2）考虑没有提报的部分
     * 1、计划量=备货量-预计超欠产，计划量大于0的部分
     * 2、如果对应分厂+物料的总计划量>=上调控制水位，先处理备货总阈值后的等比扣减，再计算对应分厂+物料的最小批量差值
     *
     * @param factoryCode             分厂编码
     * @param isAddShort              是否加入超欠产
     * @param monthPlanVersion        销售生产需求计划版本
     * @param submissionQtyMap        分组的销售订单提报量
     * @param minConfigurationMap     分组的上调控制水位及最小批量配置（分厂+物料）
     * @param minWildcardConfigMap    分组的上调控制水位及最小批量配置（分厂+胎别）
     * @param productTypeCodeMap      分厂+物料-对应分厂+胎别Map
     * @param stockUpPlanList         备货计划
     * @param estimateExceedShortList 预计欠产量
     * @param allocationList          销售订单分配集合
     * @param totalStockThreshold     总备货阀值
     * @param stockReverseMap         库存信息
     * @return
     */
    private List<SaleMonthPlanRequire> getSaleMonthPlanRequire(String factoryCode,
                                                               boolean isAddShort,
                                                               String monthPlanVersion,
                                                               Map<String, Long> submissionQtyMap,
                                                               Map<String, ProductMinConfiguration> minConfigurationMap,
                                                               Map<String, ProductMinConfiguration> minWildcardConfigMap,
                                                               Map<String, String> productTypeCodeMap,
                                                               List<MdmStockUpPlan> stockUpPlanList,
                                                               List<EstimateExceedShort> estimateExceedShortList,
                                                               List<OrderPlanAllocation> allocationList,
                                                               long totalStockThreshold,
                                                               Map<String, ProductStockInfo> stockReverseMap) {
        List<SaleMonthPlanRequire> requireList = new ArrayList<>();
        //获取达到生产门槛的分厂物料编码
        Map<String, ProductMinConfiguration> needProductMinConfiguration = getNeedProductMinConfiguration(submissionQtyMap, productTypeCodeMap, minConfigurationMap, minWildcardConfigMap);
        //达到生产门槛的记录 - 按 分厂 + 物料编码 + 库位 汇总生产需求量
        Map<String, Map<LocationTypeEnum, Long>> needProductQtyMap = getNeedProductQtyMap(allocationList, needProductMinConfiguration);
        //获取备货量
        Map<String, Long> stockUpQtyMap = new HashMap<>();
        Map<String, Map<LocationTypeEnum, Long>> locationStockUpQtyMap = new HashMap<>();
        setStockUpConfiguration(stockUpPlanList, stockUpQtyMap, locationStockUpQtyMap);
        //预计欠产量
        Map<String, Long> exceedShortQtyMap = new HashMap<>();
        Map<String, Map<LocationTypeEnum, Long>> locationExceedShortQtyMap = new HashMap<>();
        setEstimateExceedShortConfiguration(estimateExceedShortList, exceedShortQtyMap, locationExceedShortQtyMap);
        //实际备货量（分厂+物料+库位）和预计最小批量（分厂+物料+库位）
        Map<String, Long> actualStockMap = new HashMap<>();
        Map<String, Long> raiseQtyMap = new HashMap<>();
        //获取未提报的备货列表，记录对应备货量
        List<MdmStockUpPlan> noSubmitList = getNoSubmitList(factoryCode, minConfigurationMap, minWildcardConfigMap, stockUpPlanList, allocationList, actualStockMap, needProductMinConfiguration, stockReverseMap);
        //根据 分厂 + 物料编码 + 库位 计算对应的备货量，根据计划排产量计算是否需要上调到最小批量；同时计算未提报的备货计划量，需要上调到最小批量的部分
        calculateRaiseQty(factoryCode, isAddShort, actualStockMap, raiseQtyMap, needProductQtyMap, locationStockUpQtyMap, locationExceedShortQtyMap, needProductMinConfiguration, totalStockThreshold, noSubmitList, stockReverseMap);
        //加入备货、预计超欠产量、最小批量,并得到销售订单的生产需求计划
        List<SaleMonthPlanRequire> saleOrderRequireList = additionalRequireAndSaleOrderRequire(isAddShort, monthPlanVersion, allocationList, requireList, locationExceedShortQtyMap,
                needProductMinConfiguration, minConfigurationMap, raiseQtyMap, actualStockMap, noSubmitList);
        //对saleOrderRequireList进行合并处理
        if (!CollectionUtils.isEmpty(saleOrderRequireList)) {
            SaleRequirePlanHelper.mergeSaleOrderRequire(saleOrderRequireList, requireList);
        }
        return requireList;
    }

    /**
     * 获取未提报的备货列表，记录对应备货量和最小批量
     *
     * @param minConfigurationMap         分组的上调控制水位及最小批量配置（分厂+物料）
     * @param minWildcardConfigMap        分组的上调控制水位及最小批量配置（分厂+胎别）
     * @param stockUpPlanList             备货计划
     * @param allocationList              销售订单分配集合
     * @param actualStockMap              实际备货量（分厂+物料+库位）
     * @param needProductMinConfiguration 获取达到生产门槛的分厂物料编码
     */
    private List<MdmStockUpPlan> getNoSubmitList(String factoryCode,
                                                 Map<String, ProductMinConfiguration> minConfigurationMap,
                                                 Map<String, ProductMinConfiguration> minWildcardConfigMap,
                                                 List<MdmStockUpPlan> stockUpPlanList,
                                                 List<OrderPlanAllocation> allocationList,
                                                 Map<String, Long> actualStockMap,
                                                 Map<String, ProductMinConfiguration> needProductMinConfiguration,
                                                 Map<String, ProductStockInfo> stockReverseMap) {
        if (!iFactoryParamService.isOpenNoSubmitStockUp(factoryCode)) {
            return Collections.emptyList();
        }
        //将分厂未提报的物料，取出对应的备货记录，如果达到上调控制水位，提升备货量到最小批量进行排产
        Set<String> existPlanSet = allocationList.stream().map(OrderPlanAllocation::getGroupKey).collect(Collectors.toSet());
        // 记录有计划备货的记录
        List<MdmStockUpPlan> stockPlanList = new ArrayList<>();
        // 先计算出分厂+物料的总计划量（计划=备货）
        Map<String, Long> sumPlanMap = new HashMap<>();
        for (MdmStockUpPlan stockUpPlan : stockUpPlanList) {
            if (stockUpPlan.getStockQty() == null) {
                continue;
            }
            String groupKey = stockUpPlan.getGroupKey();
            // 如果计划已提报，无需处理
            if (existPlanSet.contains(groupKey)) {
                continue;
            }
            // 计算计划量=备货
            long planQty = stockUpPlan.getStockQty();
            if (planQty <= 0) {
                continue;
            }
            // 记录备货量为计划量
            stockUpPlan.setStockQty(planQty);
            stockPlanList.add(stockUpPlan);
            Long sumPlan = sumPlanMap.getOrDefault(groupKey, 0L);
            sumPlanMap.put(groupKey, sumPlan + planQty);
        }

        // 查询对应物料信息
        Map<String, MdmProductInfo> productInfoMap = getProductInfoMapByStockUp(stockPlanList);
        // 记录未提报的备货达到最小批量的部分
        List<MdmStockUpPlan> noSubmitList = new ArrayList<>();
        // 取出总备货达到上调控制水位的记录
        for (MdmStockUpPlan stockUpPlan : stockPlanList) {
            String groupKey = stockUpPlan.getGroupKey();
            Long sumPlan = sumPlanMap.get(groupKey);

            // 判断分厂+物料的总计划量是否达到上调控制水位，没达到无需排产
            ProductMinConfiguration minConfiguration = minConfigurationMap.get(groupKey);
            if (minConfiguration == null) {
                // 取通配符最小批量配置
                MdmProductInfo info = productInfoMap.get(GenerageMapKeyUtils.createMapKey(stockUpPlan.getFactoryCode(), stockUpPlan.getProductCode()));
                if (info != null) {
                    minConfiguration = minWildcardConfigMap.get(getProductTypeCodeKey(info.getFactoryCode(), info.getProductTypeCode()));
                }
            }
            if (sumPlan == null || minConfiguration == null || sumPlan < minConfiguration.getUpQty()) {
                continue;
            }
            // 记录达到生产门槛
            needProductMinConfiguration.put(groupKey, minConfiguration);
            if (stockUpPlan.getStockQty() != null && stockUpPlan.getStockQty() > 0) {
                noSubmitList.add(stockUpPlan);
            }
        }
        //未提报备货，需要与库存比较
        List<MdmStockUpPlan> realNoSubmitList = SaleRequirePlanHelper.handlerNoSubmitStockPlan(noSubmitList, stockReverseMap);
        noSubmitList = realNoSubmitList;
        // 汇总未提报的计划量
        if (!CollectionUtils.isEmpty(noSubmitList)) {
            noSubmitList.stream().forEach(noSubmitStockUpPlan -> {
                String stockGroupKey = SaleRequirePlanHelper.getStockGroupKey(noSubmitStockUpPlan.getGroupKey(), String.valueOf(noSubmitStockUpPlan.getLocationType()));
                long stockQty = actualStockMap.getOrDefault(stockGroupKey, 0L);
                stockQty = stockQty + noSubmitStockUpPlan.getStockQty();
                actualStockMap.put(stockGroupKey, stockQty);
            });
        }
        return noSubmitList;
    }

    /**
     * 20250506 ZLT 总备货阀值 = 库容阀值 - 月结库存 + 可对冲库存
     * 故而，改方法转为获取分厂的库容阀值
     *
     * @param factory 分厂编码
     * @return 库容阀值
     */
    private Integer getStorageCapacityThreshold(String factory) {
        FactoryParam factoryParam = new FactoryParam();
        factoryParam.setFactoryCode(factory);
        factoryParam.setParamCode(FactoryConstant.STORAGE_CAPACITY_THRESHOLD);
        FactoryParam facParamSingle = iFactoryParamService.getFacParamSingle(factoryParam);
        if (null == facParamSingle || StringUtils.isBlank(facParamSingle.getParamValue())) {
            return BigDecimal.ZERO.intValue();
        }
        return (Integer) FactoryParamUtils.getParamValue(facParamSingle);
    }

    /**
     * 根据 分厂 + 物料编码 + 库位 计算对应的备货量，根据计划排产量计算最小批量
     * （1）备货量需要分库位计算备货量
     * 1、外销/OE：销售需求计划=净需求-欠产量+备货量
     * 2、内销：
     * A （净需求-欠产量）>=理论备货量，不进行备货，销售需求计划=净需求-欠产量
     * B、（净需求-欠产量）<理论备货量，上调至理论备货量，实际备货量=理论备货量-（净需求-欠产量），销售需求计划=理论备货量
     *
     * @param isAddShort                  是否纳入超欠产
     * @param actualStockMap              实际备货量（分厂+物料+库位）
     * @param raiseQtyMap                 预计最小批量（分厂+物料+库位）
     * @param needProductQtyMap           已达到排产门槛的分厂+物料+库位信息
     * @param locationStockUpQtyMap       分厂+物料+库位的汇总备货量
     * @param locationExceedShortQtyMap   分厂+物料+库位的欠产量
     * @param needProductMinConfiguration 分厂+物料的最小批量配置信息
     * @param totalStockThreshold         总备货阀值
     * @param noSubmitList                未提报的备货计划
     * @param stockReverseMap             库存信息
     */
    private void calculateRaiseQty(String factoryCode,
                                   boolean isAddShort,
                                   Map<String, Long> actualStockMap,
                                   Map<String, Long> raiseQtyMap,
                                   Map<String, Map<LocationTypeEnum, Long>> needProductQtyMap,
                                   Map<String, Map<LocationTypeEnum, Long>> locationStockUpQtyMap,
                                   Map<String, Map<LocationTypeEnum, Long>> locationExceedShortQtyMap,
                                   Map<String, ProductMinConfiguration> needProductMinConfiguration,
                                   long totalStockThreshold,
                                   List<MdmStockUpPlan> noSubmitList,
                                   Map<String, ProductStockInfo> stockReverseMap) {
        // 汇总计算后的分厂+物料的计划排产量,用于最小批量差值
        Map<String, Long> sumPlanMap = new HashMap<>();
        // 记录计划量的分厂+物料+库位 : 分厂+物料 的Map
        Map<String, String> planStockMap = new HashMap<>();
        //内销备货方式
        String domesticStockUpType = iFactoryParamService.getDomesticStockUpType(factoryCode);
        // 计算实际备货量、排产量
        needProductQtyMap.forEach((groupKey, locationTypeMap) -> {
            // 没有达到生产门槛
            if (!needProductMinConfiguration.containsKey(groupKey)) {
                return;
            }
            //20250512 剩余库存 > 总备货量，则不进行备货
            ProductStockInfo productStockInfo = stockReverseMap.get(groupKey);
            Long leftOverQty = BigDecimal.ZERO.longValue();
            if (null != productStockInfo) {
                leftOverQty = productStockInfo.getLeftOverQty();
            }
            if (null == leftOverQty) {
                leftOverQty = BigDecimal.ZERO.longValue();
            }
            //  汇总排产量（此时不汇总备货部分，计算完备货阀值后再汇总）
            long sumPlan = sumPlanMap.getOrDefault(groupKey, 0L);
            Long sumStockUpQty = SaleRequirePlanHelper.getSumStockUpQty(locationStockUpQtyMap, groupKey);
            if (leftOverQty >= sumStockUpQty) {
                //20250523 无需备货时，直接汇总净需求
                for (Map.Entry<LocationTypeEnum, Long> entry : locationTypeMap.entrySet()) {
                    Long locationNeedProductQty = entry.getValue();
                    if (null == locationNeedProductQty) {
                        locationNeedProductQty = BigDecimal.ZERO.longValue();
                    }
                    sumPlan = sumPlan + locationNeedProductQty;
                }
                if (sumPlan > BigDecimal.ZERO.longValue()) {
                    sumPlanMap.put(groupKey, sumPlan);
                }
                return;
            }
            //库位备货计算，先外销，再OE，最后内销
            List<LocationTypeEnum> sortList = LocationTypeEnum.getStockUpSort();
            for (LocationTypeEnum locationType : sortList) {
                String locationGroupKey = SaleRequirePlanHelper.getStockGroupKey(groupKey, locationType.getValue());
                StockUpPlanVo locationStockUpPlan = SaleRequirePlanHelper.calculateLocationTypeStockUpQty(isAddShort, groupKey, domesticStockUpType, locationType, locationTypeMap, locationStockUpQtyMap, locationExceedShortQtyMap, leftOverQty);
                if (null == locationStockUpPlan) {
                    continue;
                }
                leftOverQty = locationStockUpPlan.getLeftOverQty();
                Long stockQty = locationStockUpPlan.getStockQty();
                Long planQty = locationStockUpPlan.getPlanQty();
                // 记录备货量
                actualStockMap.put(locationGroupKey, stockQty);
                planStockMap.put(locationGroupKey, groupKey);
                // 汇总计划排产量（此时不汇总备货部分）
                sumPlan += (planQty - stockQty);
            }
            if (sumPlan > BigDecimal.ZERO.longValue()) {
                sumPlanMap.put(groupKey, sumPlan);
            }
        });
        // 计算总备货，如果超过备货总阀值，需要按比例分摊
        processStockThreshold(actualStockMap, totalStockThreshold);

        // 叠加分厂+物料的总排产量（包括提报和非提报的备货）
        processStockSumPlan(actualStockMap, noSubmitList, sumPlanMap, planStockMap);

        // 计算最小批量（包括提报和未提报的）
        sumPlanMap.forEach((groupKey, sumQty) -> {
            //没有需求量，不用考虑最小批量
            if (sumQty <= BigDecimal.ZERO.longValue()) {
                return;
            }
            ProductMinConfiguration configuration = needProductMinConfiguration.get(groupKey);
            //没有达到生产门槛
            if (null == configuration) {
                return;
            }
            Long minQty = Long.valueOf(configuration.getMinQty());
            //达到最小批量
            if (sumQty >= minQty) {
                return;
            }
            //上调到最小批量，还需要的数量
            raiseQtyMap.put(groupKey, minQty - sumQty);
        });
    }

    /**
     * 叠加分厂+物料的总排产量（包括提报和非提报的备货）
     *
     * @param actualStockMap 实际备货量（分厂+物料+库位）
     * @param noSubmitList   未提报的备货计划
     * @param sumPlanMap     汇总计算后的分厂+物料的计划排产量
     * @param planStockMap   计划量的分厂+物料+库位 : 分厂+物料 的Map
     */
    private void processStockSumPlan(Map<String, Long> actualStockMap, List<MdmStockUpPlan> noSubmitList, Map<String, Long> sumPlanMap, Map<String, String> planStockMap) {
        // 记录未提报已计算过的分厂+物料+库位的记录
        Set<String> noSubmitSet = new HashSet<>();
        // 计算未提报的排产量
        for (MdmStockUpPlan stockUpPlan : noSubmitList) {
            String groupKey = stockUpPlan.getGroupKey();
            String stockGroupKey = SaleRequirePlanHelper.getStockGroupKey(groupKey, String.valueOf(stockUpPlan.getLocationType()));
            Long stockPlanQty = actualStockMap.get(stockGroupKey);
            if (stockPlanQty == null) {
                continue;
            }
            // 跳过已计算过分厂+物料+库位
            if (!noSubmitSet.add(stockGroupKey)) {
                continue;
            }

            // 汇总未提报的对应计划量
            long sumPlan = sumPlanMap.getOrDefault(groupKey, 0L);
            sumPlanMap.put(groupKey, sumPlan + stockPlanQty);
        }

        // 汇总已提报的排产备货量
        planStockMap.forEach((stockGroupKey, groupKey) -> {
            Long stockPlanQty = actualStockMap.getOrDefault(stockGroupKey, 0L);
            long sumPlan = sumPlanMap.getOrDefault(groupKey, 0L);
            sumPlanMap.put(groupKey, sumPlan + stockPlanQty);
        });
    }

    /**
     * 计算总备货，如果超过备货总阀值，需要按比例分摊
     *
     * @param actualStockMap      实际备货量（分厂+物料+库位）
     * @param totalStockThreshold 总备货阀值
     */
    private void processStockThreshold(Map<String, Long> actualStockMap, long totalStockThreshold) {
        long stockSum = actualStockMap.values().stream().mapToLong(Long::longValue).sum();
        if (stockSum > totalStockThreshold) {
            BigDecimal rate = BigDecimal.ONE.subtract(BigDecimal.valueOf(stockSum - totalStockThreshold).divide(BigDecimal.valueOf(stockSum), 4, RoundingMode.HALF_UP));
            for (String key : actualStockMap.keySet()) {
                Long value = actualStockMap.get(key);
                if (value != null) {
                    // 先向下取整，后续修正差值
                    actualStockMap.put(key, BigDecimal.valueOf(value).multiply(rate).setScale(0, RoundingMode.DOWN).longValue());
                }
            }
            // 重新计算备货量总量，如果比备货阀值小，如果把差值均分填充到备货记录
            stockSum = actualStockMap.values().stream().mapToLong(Long::longValue).sum();
            if (stockSum < totalStockThreshold) {
                // 条数
                int size = actualStockMap.size();
                // 总差值
                long diffSum = totalStockThreshold - stockSum;
                // 均摊的备货量
                long singleQty = diffSum / size;
                // 如果还有余数，前几条多加这部分
                long remainder = diffSum % size;
                for (String key : actualStockMap.keySet()) {
                    long addQty = singleQty;
                    if (remainder > 0) {
                        remainder--;
                        addQty += 1;
                    }
                    Long value = actualStockMap.get(key);
                    if (value != null) {
                        actualStockMap.put(key, value + addQty);
                    }
                }
            }
        }
    }

    /**
     * 组合分厂+胎别key
     */
    private String getProductTypeCodeKey(String factoryCode, String productTypeCode) {
        return String.format("%s|*|%s", factoryCode, productTypeCode);
    }

    /**
     * 根据分厂物料提报量，获取达到排产门槛的物料信息配置
     *
     * @param submissionQtyMap     分厂+物料的汇总提报量
     * @param productTypeCodeMap   分厂+物料-对应分厂+胎别Map
     * @param minConfigurationMap  分厂+ 物料 的最小批量配置
     * @param minWildcardConfigMap 分厂+ 胎别 的最小批量配置
     * @return
     */
    private Map<String, ProductMinConfiguration> getNeedProductMinConfiguration(Map<String, Long> submissionQtyMap,
                                                                                Map<String, String> productTypeCodeMap,
                                                                                Map<String, ProductMinConfiguration> minConfigurationMap,
                                                                                Map<String, ProductMinConfiguration> minWildcardConfigMap) {
        Map<String, ProductMinConfiguration> needProductMinConfiguration = new HashMap<>();
        submissionQtyMap.forEach((key, submissionQty) -> {
            ProductMinConfiguration configuration = minConfigurationMap.get(key);
            if (null == configuration) {
                String productTypeCodeKey = productTypeCodeMap.get(key);
                if (StringUtils.isNotEmpty(productTypeCodeKey)) {
                    configuration = minWildcardConfigMap.get(productTypeCodeKey);
                }
            }

            if (null != configuration && submissionQty >= configuration.getUpQty()) {
                needProductMinConfiguration.put(key, configuration);
            }
        });
        return needProductMinConfiguration;
    }

    /**
     * 根据销售订单库存对冲分配结果，按分厂+物料+库位分组汇总需要排产量
     *
     * @param allocationList              销售订单分配集合
     * @param needProductMinConfiguration 获取达到生产门槛的分厂物料编码
     */
    private Map<String, Map<LocationTypeEnum, Long>> getNeedProductQtyMap(List<OrderPlanAllocation> allocationList, Map<String, ProductMinConfiguration> needProductMinConfiguration) {
        Map<String, Map<LocationTypeEnum, Long>> needProductQtyMap = new HashMap<>();
        allocationList.stream()
                // 只有达到生成的门槛的记录才计算排产量
                .filter(allocation -> needProductMinConfiguration.containsKey(allocation.getGroupKey()))
                .forEach(needProductAllocation -> {
                    // 按分厂+物料取出库位分组，叠加对应的生产数量
                    String groupKey = needProductAllocation.getGroupKey();
                    LocationTypeEnum locationType = LocationTypeEnum.getEnumByValue(String.valueOf(needProductAllocation.getLocationType()));
                    Map<LocationTypeEnum, Long> locationTypeMap = needProductQtyMap.get(groupKey);
                    if (null == locationTypeMap) {
                        locationTypeMap = new HashMap<>();
                    }
                    Long productQty = locationTypeMap.get(locationType);
                    if (null == productQty) {
                        productQty = 0L;
                    }
                    locationTypeMap.put(locationType, productQty + needProductAllocation.getProduceQtyDue());
                    needProductQtyMap.put(groupKey, locationTypeMap);
                });
        return needProductQtyMap;
    }

    /**
     * 根据备货量配置，按分厂+物料分组汇总备货量及按库位分组的备货量
     *
     * @param stockUpPlanList       备货配置
     * @param stockUpQtyMap         按分厂+物料汇总备货量
     * @param locationStockUpQtyMap 按分厂+物料+库位的备货量
     */
    private void setStockUpConfiguration(List<MdmStockUpPlan> stockUpPlanList, Map<String, Long> stockUpQtyMap, Map<String, Map<LocationTypeEnum, Long>> locationStockUpQtyMap) {
        stockUpPlanList.stream().forEach(stockUpPlan -> {
            String groupKey = stockUpPlan.getGroupKey();
            LocationTypeEnum locationType = LocationTypeEnum.getEnumByValue(String.valueOf(stockUpPlan.getLocationType()));
            Long sumStockUpQty = stockUpQtyMap.get(groupKey);
            if (null == sumStockUpQty) {
                sumStockUpQty = BigDecimal.ZERO.longValue();
            }
            sumStockUpQty = sumStockUpQty + stockUpPlan.getStockQty();
            stockUpQtyMap.put(groupKey, sumStockUpQty);
            Map<LocationTypeEnum, Long> locationTypeMap = locationStockUpQtyMap.get(groupKey);
            if (null == locationTypeMap) {
                locationTypeMap = new HashMap<>();
            }
            locationTypeMap.put(locationType, stockUpPlan.getStockQty());
            locationStockUpQtyMap.put(groupKey, locationTypeMap);
        });
    }

    /**
     * 根据超欠产量配置，按分厂+物料分组汇超欠产量及按库位分组的超欠产量
     *
     * @param estimateExceedShortList   超欠产配置
     * @param exceedShortQtyMap         按分厂+物料分组汇超欠产量
     * @param locationExceedShortQtyMap 按分厂+物料+库位分组的超欠产量
     */
    private void setEstimateExceedShortConfiguration(List<EstimateExceedShort> estimateExceedShortList, Map<String, Long> exceedShortQtyMap, Map<String, Map<LocationTypeEnum, Long>> locationExceedShortQtyMap) {
        estimateExceedShortList.stream().forEach(estimateExceedShort -> {
            String groupKey = estimateExceedShort.getGroupKey();
            Long sumExceedShortQty = exceedShortQtyMap.get(groupKey);
            if (null == sumExceedShortQty) {
                sumExceedShortQty = BigDecimal.ZERO.longValue();
            }
            sumExceedShortQty = sumExceedShortQty + estimateExceedShort.getExceedShortQty();
            exceedShortQtyMap.put(groupKey, sumExceedShortQty);
            LocationTypeEnum locationType = LocationTypeEnum.getEnumByValue(estimateExceedShort.getLocationType());
            if (null == locationType) {
                return;
            }
            Map<LocationTypeEnum, Long> locationInfo = locationExceedShortQtyMap.get(groupKey);
            if (null == locationInfo) {
                locationInfo = new HashMap<>();
            }
            locationInfo.put(locationType, Long.valueOf(estimateExceedShort.getExceedShortQty()));
            locationExceedShortQtyMap.put(groupKey, locationInfo);
        });
    }

    // /**
    //  * 根据提报量达到排产门槛，获取需要上调到最小批量增加的值
    //  * 需要排产量 + 备货量 - 超欠产量 < 最小批量则为需要上调到最小批量的差值
    //  *
    //  * @param needProductQtyMap           已达到排产门槛的分厂+物料信息
    //  * @param needProductMinConfiguration 最小批量配置信息
    //  * @param stockUpQtyMap               分厂+物料的汇总备货量
    //  * @param exceedShortQtyMap           分厂+物料的汇总超欠产量
    //  * @return
    //  */
    // private Map<String, Long> getRaiseQtyByMinBatch(Map<String, Long> needProductQtyMap, Map<String, ProductMinConfiguration> needProductMinConfiguration, Map<String, Long> stockUpQtyMap, Map<String, Long> exceedShortQtyMap) {
    //     Map<String, Long> raiseQtyMap = new HashMap<>();
    //     needProductQtyMap.forEach((groupKey, needProductQty) -> {
    //         ProductMinConfiguration configuration = needProductMinConfiguration.get(groupKey);
    //         //没有达到生产门槛
    //         if (null == configuration) {
    //             return;
    //         }
    //         Long minQty = Long.valueOf(configuration.getMinQty());
    //         //达到生产门槛 获取备货量、预计超欠产
    //         Long stockUpQty = stockUpQtyMap.get(groupKey);
    //         if (null == stockUpQty) {
    //             stockUpQty = BigDecimal.ZERO.longValue();
    //         }
    //         Long estimateExceedShortQty = exceedShortQtyMap.get(groupKey);
    //         if (null == estimateExceedShortQty) {
    //             estimateExceedShortQty = BigDecimal.ZERO.longValue();
    //         }
    //         Long sumQty = needProductQty + stockUpQty - estimateExceedShortQty;
    //         //达到最小批量
    //         if (sumQty >= minQty) {
    //             return;
    //         }
    //         //上调到最小批量，还需要的数量
    //         raiseQtyMap.put(groupKey, minQty - sumQty);
    //     });
    //     return raiseQtyMap;
    // }

    /**
     * 根据销售订单库存对冲结果，结合上调控制水位信息、备货信息、欠产信息及最小批量，得到订单转换的销售生产需求集合
     * 及需要加入的备货量、超欠产量、最小批量差值的额外生产需求集合
     *
     * @param isAddShort                  是否加入超欠产
     * @param monthPlanVersion            销售需求计划版本
     * @param allocationList              订单库存对冲明细
     * @param requireList                 需要加入额外的生产需求集合集合
     * @param locationExceedShortQtyMap   超欠产量配置集合
     * @param needProductMinConfiguration 达到排产条件的集合配置
     * @param minConfigurationMap         最小批量配置，包含上调控制水位及最小批量配置
     * @param raiseQtyMap                 需要上调到最小批量的规格配置
     * @param actualStockMap              实际备货量（分厂+物料+库位）
     * @param noSubmitList                未提报物料的备货列表
     * @return
     */
    private List<SaleMonthPlanRequire> additionalRequireAndSaleOrderRequire(boolean isAddShort,
                                                                            String monthPlanVersion,
                                                                            List<OrderPlanAllocation> allocationList,
                                                                            List<SaleMonthPlanRequire> requireList,
                                                                            Map<String, Map<LocationTypeEnum, Long>> locationExceedShortQtyMap,
                                                                            Map<String, ProductMinConfiguration> needProductMinConfiguration,
                                                                            Map<String, ProductMinConfiguration> minConfigurationMap,
                                                                            Map<String, Long> raiseQtyMap,
                                                                            Map<String, Long> actualStockMap,
                                                                            List<MdmStockUpPlan> noSubmitList) {
        // 记录处理过的分厂+物料+库位的备货记录
        Set<String> addStockUpKey = new HashSet<>();
        // 记录处理过的分厂+物料+库位的欠产记录
        Set<String> addShortKey = new HashSet<>();
        // 记录处理过的分厂+物料的最小批量记录
        Set<String> addMinKey = new HashSet<>();
        List<SaleMonthPlanRequire> saleOrderRequireList = new ArrayList<>();
        allocationList.stream().forEach(orderPlanAllocation -> {
            SaleMonthPlanRequire require = new SaleMonthPlanRequire();
            BeanUtils.copyProperties(orderPlanAllocation, require);
            require.setId(null);
            require.setMonthPlanVersion(monthPlanVersion);
            String groupKey = orderPlanAllocation.getGroupKey();
            String stockGroupKey = orderPlanAllocation.getStockGroupKey();
            LocationTypeEnum locationType = LocationTypeEnum.getEnumByValue(orderPlanAllocation.getLocationType());
            Map<LocationTypeEnum, Long> shortFlagMap = locationExceedShortQtyMap.get(groupKey);
            // Map<LocationTypeEnum, Long> stockUpFlagMap = locationStockUpQtyMap.get(groupKey);
            Integer isDebitPlan = YesOrNoEnum.NO.getValue();
            if (!CollectionUtils.isEmpty(shortFlagMap) && shortFlagMap.containsKey(locationType)) {
                isDebitPlan = YesOrNoEnum.YES.getValue();
            }
            require.setIsDebitPlan(isDebitPlan);
            //没有达到生产门槛
            if (null == needProductMinConfiguration.get(groupKey)) {
                require.setPlanQty(BigDecimal.ZERO.longValue());
                if (null == minConfigurationMap.get(groupKey)) {
                    require.setRemark("没有配置最小批量-不排产");
                } else {
                    require.setRemark("提报量没有达到上调控制水位-不排产");
                }
                require.setDeliveryDateDue(null);
                require.setQty(orderPlanAllocation.getPlanQty());
                require.setNeedProduct(false);
                saleOrderRequireList.add(require);
                return;
            }
            //按照库位处理超欠产数据（没有提报和达到生产门槛不处理）根据参数是否纳入超欠产数据
            if (isAddShort && !CollectionUtils.isEmpty(shortFlagMap) && !addShortKey.contains(stockGroupKey) && shortFlagMap.containsKey(locationType)) {
                Long shortQty = shortFlagMap.get(locationType);
                if (shortQty != null && shortQty < 0) {
                    SaleMonthPlanRequire shortRequire = SaleRequirePlanHelper.buildShortRequire(monthPlanVersion, require, locationType, shortQty);
                    requireList.add(shortRequire);
                }
                addShortKey.add(stockGroupKey);
            }
            //按照库位处理备货量（没有提报和达到生产门槛不处理）
            if (actualStockMap.containsKey(stockGroupKey) && !addStockUpKey.contains(stockGroupKey)) {
                Long stockQty = actualStockMap.get(stockGroupKey);
                if (stockQty != null && stockQty > 0) {
                    SaleMonthPlanRequire stockUpRequire = SaleRequirePlanHelper.buildStockUpRequire(monthPlanVersion, require, locationType, stockQty);
                    requireList.add(stockUpRequire);
                    addStockUpKey.add(stockGroupKey);
                }
            }
            //处理最小批量
            if (raiseQtyMap.containsKey(groupKey) && !addMinKey.contains(groupKey)) {
                SaleMonthPlanRequire minRequire = SaleRequirePlanHelper.buildMinBatchRequire(monthPlanVersion, require, raiseQtyMap.get(groupKey));
                minRequire.setLocationType(ProductCommonTypeEnum.getLocationTypeByCode(orderPlanAllocation.getCommonType()).getValue());
                requireList.add(minRequire);
                addMinKey.add(groupKey);
            }
            require.setPlanQty(orderPlanAllocation.getProduceQtyDue());
            saleOrderRequireList.add(require);
        });

        // 处理未提报的物料的备货计划
        processNoSubmitList(monthPlanVersion, requireList, raiseQtyMap, actualStockMap, noSubmitList, addStockUpKey, addMinKey);

        return saleOrderRequireList;
    }

    /**
     * 处理未提报的物料的备货计划
     *
     * @param monthPlanVersion 销售需求计划版本
     * @param requireList      需要加入额外的生产需求集合集合
     * @param raiseQtyMap      需要上调到最小批量的规格配置
     * @param actualStockMap   实际备货量（分厂+物料+库位）
     * @param noSubmitList     未提报物料的备货列表
     * @param addStockUpKey    处理过的分厂+物料+库位的备货记录
     * @param addMinKey        记录处理过的分厂+物料的最小批量记录
     */
    private void processNoSubmitList(String monthPlanVersion,
                                     List<SaleMonthPlanRequire> requireList,
                                     Map<String, Long> raiseQtyMap,
                                     Map<String, Long> actualStockMap,
                                     List<MdmStockUpPlan> noSubmitList,
                                     Set<String> addStockUpKey,
                                     Set<String> addMinKey) {
        if (CollectionUtils.isEmpty(noSubmitList)) {
            return;
        }

        // 根据备货查询对应物料信息Map(分厂+SAP代码)
        Map<String, MdmProductInfo> productMap = getProductInfoMapByStockUp(noSubmitList);

        for (MdmStockUpPlan itemPlan : noSubmitList) {
            String groupKey = itemPlan.getGroupKey();
            String stockGroupKey = SaleRequirePlanHelper.getStockGroupKey(groupKey, String.valueOf(itemPlan.getLocationType()));
            LocationTypeEnum locationType = LocationTypeEnum.getEnumByValue(String.valueOf(itemPlan.getLocationType()));
            SaleMonthPlanRequire require = new SaleMonthPlanRequire();
            // 年月、分厂、物料编号
            require.setYear(itemPlan.getYear());
            require.setMonth(itemPlan.getMonth());
            require.setFactoryCode(itemPlan.getFactoryCode());
            require.setProductCode(itemPlan.getProductCode());
            MdmProductInfo productInfo = productMap.get(GenerageMapKeyUtils.createMapKey(itemPlan.getFactoryCode(), itemPlan.getProductCode()));
            String commonType = null;
            if (productInfo != null) {
                // 复制对应物料信息字段
                require.setBrand(productInfo.getBrand());
                require.setProductDesc(productInfo.getProductDesc());
                require.setProductTypeCode(productInfo.getProductTypeCode());
                require.setProductTypeName(productInfo.getProductTypeName());
                require.setProSize(productInfo.getProSize());
                require.setSpecifications(productInfo.getSpecifications());
                require.setPattern(productInfo.getPattern());
                require.setHierarchy(productInfo.getHierarchy());
                commonType = productInfo.getCommonType();
            }

            //按照库位处理备货量（没有提报和达到生产门槛不处理）
            if (actualStockMap.containsKey(stockGroupKey) && !addStockUpKey.contains(stockGroupKey)) {
                Long stockQty = actualStockMap.get(stockGroupKey);
                if (stockQty != null && stockQty > 0) {
                    SaleMonthPlanRequire stockUpRequire = SaleRequirePlanHelper.buildStockUpRequire(monthPlanVersion, require, locationType, stockQty);
                    requireList.add(stockUpRequire);
                    addStockUpKey.add(stockGroupKey);
                }
            }
            //处理最小批量
            if (raiseQtyMap.containsKey(groupKey) && !addMinKey.contains(groupKey)) {
                SaleMonthPlanRequire minRequire = SaleRequirePlanHelper.buildMinBatchRequire(monthPlanVersion, require, raiseQtyMap.get(groupKey));
                // 取物料公用规格对应的库位
                minRequire.setLocationType(ProductCommonTypeEnum.getLocationTypeByCode(commonType).getValue());
                requireList.add(minRequire);
                addMinKey.add(groupKey);
            }
        }
    }

    /**
     * 是否加入超欠产，根据控制参数SYS018
     * N表示不加，其它都是加入
     *
     * @param factoryCode 分厂编码
     * @return
     */
    private boolean isAddShort(String factoryCode) {
        FactoryParam factoryParam = new FactoryParam();
        factoryParam.setFactoryCode(factoryCode);
        factoryParam.setProductTypeCode(ProductTypeEnum.SEMI_STEEL.getValue());
        factoryParam.setParamCode(FactoryConstant.SYS_PARAM_IS_ADD_SHORT);
        FactoryParam addShortParam = iFactoryParamService.getFacParamSingle(factoryParam);
        if (null == addShortParam) {
            return true;
        }
        String addShortParamValue = addShortParam.getParamValue();
        if (StringUtils.isBlank(addShortParamValue)) {
            return true;
        }
        if (NO_ADD_SHORT.equalsIgnoreCase(addShortParamValue)) {
            return false;
        }
        return true;
    }

    /**
     * 根据备货查询对应物料信息Map(分厂+SAP代码)
     */
    private Map<String, MdmProductInfo> getProductInfoMapByStockUp(List<MdmStockUpPlan> list) {
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyMap();
        }
        List<String> factoryCodeList = list.stream().map(MdmStockUpPlan::getFactoryCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        List<String> productCodeList = list.stream().map(MdmStockUpPlan::getProductCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        List<MdmProductInfo> productInfoList = iMdmProductInfoService.selectListByFactoryProductCode(factoryCodeList, productCodeList);
        return productInfoList.stream().collect(Collectors.toMap(v -> GenerageMapKeyUtils.createMapKey(v.getFactoryCode(), v.getProductCode())
                , Function.identity(), (v1, v2) -> v1));
    }


    /**
     * 根据库存对冲顺序配置，构建排序比较器对象
     * 优先值越小则排序优先级越高，则表示越优先进行对冲
     *
     * @param sortConfigurations
     * @return
     */
    private Comparator getComparator(List<PlanOrderSortConfiguration> sortConfigurations) {
        //对配置按优先值升序排序，优先值越小的配置项排序优先级越高
        sortConfigurations.sort(Comparator.comparing(PlanOrderSortConfiguration::getPriority));
        PlanOrderSortConfiguration firstSort = sortConfigurations.get(0);
        StockHedgingComparatorEnum firstOptionComparator = StockHedgingComparatorEnum.getInstance(StockHedgingOptionsEnum.getInstance(firstSort.getOptionCode()));
        Comparator first = firstOptionComparator.getComparator();
        for (int index = 1; index < sortConfigurations.size(); index++) {
            PlanOrderSortConfiguration sortConfiguration = sortConfigurations.get(index);
            StockHedgingOptionsEnum optionEnum = StockHedgingOptionsEnum.getInstance(sortConfiguration.getOptionCode());
            Comparator optionComparator = StockHedgingComparatorEnum.getInstance(optionEnum).getComparator();
            first = first.thenComparing(optionComparator);
        }
        return first;
    }


    /**
     * 操作前的校验
     *
     * @param factoryProductionParam
     * @return
     */
    private AjaxResult checkParam(FactoryProductionParamVo factoryProductionParam) {
        String factoryCode = factoryProductionParam.getFactoryCode();
        Integer year = factoryProductionParam.getYear();
        Integer month = factoryProductionParam.getMonth();
        String monthPlanVersion = factoryProductionParam.getMonthPlanVersion();
        String productionVersion = factoryProductionParam.getProductionVersion();
        if (StringUtils.isBlank(factoryCode) || null == year || null == month || StringUtils.isBlank(monthPlanVersion) || StringUtils.isBlank(productionVersion)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.versionNoEmpty"));
        }
        QueryWrapper<FactoryProductionVersion> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", factoryCode);
        queryWrapper.eq("YEAR", year);
        queryWrapper.eq("MONTH", month);
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        queryWrapper.eq("MONTH_PLAN_VERSION", monthPlanVersion);
        queryWrapper.eq("PRODUCTION_VERSION", productionVersion);
        FactoryProductionVersion version = factoryProductionVersionMapper.selectOne(queryWrapper);
        if (null == version) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.noExistVersion"));
        }
        if (YesOrNoEnum.YES.getValue().equals(version.getIsFinal())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.finalVersionNoOperate"));
        }
        return AjaxResult.success();
    }

    /**
     * 构建半钢排产上下文对象
     *
     * @param factoryProductionParam
     * @return
     */
    private Context buildContext(FactoryProductionParamVo factoryProductionParam) {
        Context context = new Context();
        context.setFactoryCode(factoryProductionParam.getFactoryCode());
        context.setYear(factoryProductionParam.getYear());
        context.setMonth(factoryProductionParam.getMonth());
        context.setMonthPlanVersion(factoryProductionParam.getMonthPlanVersion());
        context.setProductionVersion(factoryProductionParam.getProductionVersion());
        context.setPrefixVersion(factoryProductionParam.getPrefixVersion());
        context.setProductType(ProductTypeEnum.SEMI_STEEL);
        return context;
    }

}
