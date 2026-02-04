package com.zlt.aps.monthplan.demand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Sets;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.enums.ProductTypeEnum;
import com.tlt.aps.enums.ProductionPlanType;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.exception.BusinessException;
import com.tlt.aps.utils.JsonI18nConvertUtils;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.service.*;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.aps.monthplan.api.domain.vo.AreaConvertVo;
import com.zlt.aps.monthplan.common.utils.StockAllocationService;
import com.zlt.aps.monthplan.demand.mapper.SupplyOrderPoolEntityMapper;
import com.zlt.aps.monthplan.demand.service.ISalesOrderPoolService;
import com.zlt.aps.monthplan.demand.service.ISupplyOrderPoolService;
import com.zlt.aps.monthplan.factory.service.IFactoryMonthPlanProductionFinalResultService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.exception.QueryExprException;
import com.zlt.core.queryformulas.QueryFormulaUtil;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：SupplyOrderPoolServiceImpl.java
 * 描    述：SupplyOrderPoolServiceImpl供应链订单池业务层处理
 *@author yelq
 *@date 2025-12-22
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class SupplyOrderPoolServiceImpl extends AbstractDocService<SupplyOrderPool>  implements ISupplyOrderPoolService {
    private static final int DAYS_PER_MONTH = 30;

    private final SupplyOrderPoolEntityMapper supplyOrderPoolEntityMapper;
    // 物料信息
    private final IMdmMaterialInfoService materialInfoService;
    // 超期SKU
    private final IMpOverdueSkuService overdueSkuService;
    // 月均销量
    private final IMpMonthlySaleQtyService monthlySaleQtyService;
    // 成品库存
    private final IMdmProductStockService mdmProductStockService;
    // 销售订单
    private final ISalesOrderPoolService salesOrderPoolService;
    // 定稿的月度排产计划
    private final IFactoryMonthPlanProductionFinalResultService factoryMonthPlanProductionFinalResultService;
    // 排产设定
    private final IFactoryParamService iFactoryParamService;

    private final StockAllocationService stockAllocationService;


    @Override
    protected String getDocTypeCode() {
        return "2025122214";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("2025122214");
        return sysDocType;
    }

    @Override
    public String checkUnique(SupplyOrderPool docEntityVO) {
        //  (1).根据SKU、订单类型进行唯一性校验，如果存在，提示信息"xxx物料的周期排产/常规储备已经存在，请确认"，系统不做处理
        //  (2). 根据选择的储备类型校验近12个月是否出现过超期周期排产储备/超期常规储备，如果出现过，则提示信息“近12个月有出现过超期胎，不可新增”
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            String notUniqueMsg =  com.ruoyi.common.utils.StringUtils.format(I18nUtil.getMessage("ui.data.alert.supplyOrderPool.notUnique"),docEntityVO.getMaterialCode());
            throw new BusinessException(notUniqueMsg);
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("factoryCode", "materialCode","year","month","sourceType"));
    }


    private Map<String, Integer> calculateStockWithoutOrder(List<MdmProductStock> finishedProductStocks, List<SalesOrderPool> salesOrderPools) {
        if(CollectionUtils.isEmpty(finishedProductStocks)){
            return Collections.emptyMap();
        }
          // 20260110 修改原来是完全匹配年周，物料，动平衡，均匀性，现在改为物料满足, 年周满足即可, 动平衡，均匀性属于优先扣减，不满足时，再扣减其他库存
          return stockAllocationService.calculateStockWithoutOrder(finishedProductStocks,salesOrderPools);
    }


    /**
     * 新增周期排产储备时候，输入储备数量的时候，需要加一个提示用户无订单库存有多少，月底计划余量有多少
     * @param supplyOrderPool 入参
     */
    @Override
    public AjaxResult calculateStockMsg(SupplyOrderPool supplyOrderPool) {
        String yearMonth = String.format("%s%02d", supplyOrderPool.getYear(), supplyOrderPool.getMonth());

        int days = YearMonth.of(supplyOrderPool.getYear(), supplyOrderPool.getMonth()).lengthOfMonth();
        // 获取当前年月
        Set<String> skus = Sets.newHashSet(supplyOrderPool.getMaterialCode());
        // 1.计算无订单库存
        List<SalesOrderPool> salesOrderPools = this.salesOrderPoolService.findCurrentSalesOrderPool(supplyOrderPool.getFactoryCode(),skus);
        List<MdmProductStock> finishedProductStocks = this.mdmProductStockService.findCurrentFinishStock(supplyOrderPool.getFactoryCode(),skus);
        Map<String, Integer> stockWithoutOrderMap = calculateStockWithoutOrder(finishedProductStocks,salesOrderPools);

        StringBuilder msg = new StringBuilder();
        msg.append(I18nUtil.getMessage("ui.data.column.supplyOrderPool.noOrderQty")).append(stockWithoutOrderMap.get(supplyOrderPool.getMaterialCode()) == null ? 0 : stockWithoutOrderMap.get(supplyOrderPool.getMaterialCode()));

        // 2.计算月底计划余量
        Map<String, Integer> monthSurplusMap = this.factoryMonthPlanProductionFinalResultService.calculateMonthSurplusNoSave(finishedProductStocks, yearMonth, days);
        msg.append(I18nUtil.getMessage("ui.data.column.supplyOrderPool.monthSurplusQty")).append(monthSurplusMap.get(supplyOrderPool.getMaterialCode()) == null ? 0 : monthSurplusMap.get(supplyOrderPool.getMaterialCode()));

        return AjaxResult.success(msg.toString());
    }


    @Override
    public SupplyOrderPool queryRelationByMaterialCode(SupplyOrderPool supplyOrderPool) {
        MdmMaterialInfo  materialInfo =   this.materialInfoService.getMaterialInfoByMaterialCode(supplyOrderPool.getFactoryCode(),supplyOrderPool.getMaterialCode());
        if(null == materialInfo) {
            throw new BusinessException(I18nUtil.getMessage("ui.message.supplyOrderPool.notFound.materialInfo"));
        }
        // (1)通过物料表，带出物料描述、品牌、产品品类
        // 获取当前年月 2、工厂：默认116；年-月：当前系统日所在年月；内外销：默认外销；
        supplyOrderPool.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        YearMonth now = YearMonth.now();
        supplyOrderPool.setYear(now.getYear());
        supplyOrderPool.setMonth(now.getMonthValue());
        supplyOrderPool.setLocationType(materialInfo.getCommonType());
        supplyOrderPool.setMaterialDesc(materialInfo.getMaterialDesc());
        supplyOrderPool.setBrand(materialInfo.getBrand());
        supplyOrderPool.setProductTypeCode(materialInfo.getProductTypeCode());
        supplyOrderPool.setProductCategory(materialInfo.getProductCategory());
        // (2)通过月均销量表，带出近3个月月均销量、近6个月月均销量、近12个月的发货频次、适销区域、备库上限/月均销量 * 30 = 30（天）
        MpMonthlySaleQty monthlySaleQty =   monthlySaleQtyService.getMpMonthlySaleQtyByMaterialCode(supplyOrderPool);
        if(null != monthlySaleQty) {
            supplyOrderPool.setThreeAverageQty(monthlySaleQty.getPassThreeMonthSaleQty());
            supplyOrderPool.setSixAverageQty(monthlySaleQty.getPassSixMonthSaleQty());
            supplyOrderPool.setDeliveryFrequency(monthlySaleQty.getDeliveryFrequency());
            supplyOrderPool.setAverageSaleQty(monthlySaleQty.getAverageSaleQty());
            BigDecimal stockLimit = calculateStockLimit(monthlySaleQty);
            supplyOrderPool.setStockLimit(stockLimit.intValue());
            supplyOrderPool.setSaleArea(monthlySaleQty.getSaleArea());
            getSaleAreaByMonthlySaleQty(supplyOrderPool);
        }else{
            supplyOrderPool.setThreeAverageQty(BigDecimal.ZERO.intValue());
            supplyOrderPool.setSixAverageQty(BigDecimal.ZERO.intValue());
            supplyOrderPool.setDeliveryFrequency(BigDecimal.ZERO.intValue());
            supplyOrderPool.setAverageSaleQty(BigDecimal.ZERO.intValue());
            supplyOrderPool.setStockLimit(BigDecimal.ZERO.intValue());
        }
        //   (3)通过成品库存表，获取超期12个月的库存数、超期6个月的库存数、超期3个月的库存数
        List<MdmProductStock> finishedProductStocks = this.mdmProductStockService.getMpFinishedProductStockByMaterialCode(supplyOrderPool.getMaterialCode());
        if(CollectionUtils.isNotEmpty(finishedProductStocks)) {
            int threeOverdueStockQty = finishedProductStocks.stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedThreeMonth())).mapToInt(MdmProductStock::getStockQty).sum();
            int sixOverdueStockQty = finishedProductStocks.stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedSixMonth())).mapToInt(MdmProductStock::getStockQty).sum();
            int nightOverdueStockQty = finishedProductStocks.stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedNineMonth())).mapToInt(MdmProductStock::getStockQty).sum();
            int twelveOverdueStockQty  = finishedProductStocks.stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedTwelveMonth())).mapToInt(MdmProductStock::getStockQty).sum();
            supplyOrderPool.setThreeOverdueStockQty(threeOverdueStockQty);
            supplyOrderPool.setSixOverdueStockQty(sixOverdueStockQty);
            supplyOrderPool.setNightOverdueStockQty(nightOverdueStockQty);
            supplyOrderPool.setTwelveOverdueStockQty(twelveOverdueStockQty);
        }else{
            supplyOrderPool.setThreeOverdueStockQty(BigDecimal.ZERO.intValue());
            supplyOrderPool.setSixOverdueStockQty(BigDecimal.ZERO.intValue());
            supplyOrderPool.setNightOverdueStockQty(BigDecimal.ZERO.intValue());
            supplyOrderPool.setTwelveOverdueStockQty(BigDecimal.ZERO.intValue());
        }
        //通过月度生产计划表，获取近12个月有排产的月份个数
        // 8、12个月结构上机频次 = 从定稿的月度排产计划，获取近12个月的已排产的月份个数
        int  productionMonth = this.factoryMonthPlanProductionFinalResultService.calculateStructureFrequency(supplyOrderPool.getMaterialCode());
        supplyOrderPool.setStructureFrequency(productionMonth);
        return supplyOrderPool;
    }

    private void getSaleAreaByMonthlySaleQty(SupplyOrderPool monthlySaleQty) {
        List<SupplyOrderPool> list = new ArrayList<>();
        list.add(monthlySaleQty);
        // 把区域都转成名称
        List<AreaConvertVo> convertVoList = list.stream().map(SupplyOrderPool::getSaleArea)
                .flatMap(item -> Arrays.stream(item.split(",")))
                .distinct()
                .filter(com.ruoyi.common.utils.StringUtils::isNotBlank)
                .map(item -> {
                    AreaConvertVo areaConvertVo = new AreaConvertVo();
                    areaConvertVo.setAreaCode(item);
                    return areaConvertVo;
                })
                .sorted(Comparator.comparing(AreaConvertVo::getAreaCode))
                .collect(Collectors.toList());
        Map<String, String> areaNameMap = getAreaNameMap(convertVoList);
        for (SupplyOrderPool supplyOrderPool : list) {
            String saleArea = supplyOrderPool.getSaleArea();
            String[] areaSplitArr = saleArea.split(",");
            List<String> areaNameList = new ArrayList<>();
            for (String areaCode : areaSplitArr) {
                if (areaNameMap.containsKey(areaCode)) {
                    String name = areaNameMap.get(areaCode);
                    areaNameList.add(name);
                }
            }
            supplyOrderPool.setSaleAreaName(String.join(",", areaNameList));
        }
    }

    private Map<String, String> getAreaNameMap(List<AreaConvertVo> convertVoList) {
        // 执行表达式，转义区域
        try {
            QueryFormulaUtil.execFormula(convertVoList, new String[]{
                    "areaCodeName->getcolvaluewithcondition(t_dp_area, area_name, area_code, areaCode, is_delete = 0)",
            });
        } catch (QueryExprException e) {
            this.logger.error(e.getMessage(), e);
            throw new ServiceException("转换区域，执行查询公式时发生错误.");
        }
        JsonI18nConvertUtils.conventJsonI18n(convertVoList, AreaConvertVo.class);
        return convertVoList.stream().filter(item -> com.ruoyi.common.utils.StringUtils.isNotBlank(item.getAreaCodeNameI18n()))
                .collect(Collectors.toMap(AreaConvertVo::getAreaCode, AreaConvertVo::getAreaCodeNameI18n, (k1, k2) -> k1));
    }


    @Override
    public List<SupplyOrderPool> findCurrentSupplyOrderPool(DpDemandPlan createCondition) {
        LambdaQueryWrapper<SupplyOrderPool> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SupplyOrderPool::getFactoryCode,createCondition.getFactoryCode());
        wrapper.eq(SupplyOrderPool::getYear, createCondition.getYear());
        wrapper.eq(SupplyOrderPool::getMonth, createCondition.getMonth());
        wrapper.eq(SupplyOrderPool::getSourceType,ProductionPlanType.NORMAL.getPlanType());
        wrapper.eq(SupplyOrderPool::getIsDelete, YesOrNoEnum.NO.getValue());
        return this.supplyOrderPoolEntityMapper.selectList(wrapper);
    }

    @Override
    public AjaxResult checkOverdue(SupplyOrderPool supplyOrderPool) {
        // (2). 根据选择的储备类型校验近12个月是否出现过超期周期排产储备/超期常规储备，如果出现过，则提示信息“近12个月有出现过超期胎，不可新增”
        boolean checkOverDue = overdueSkuService.checkOverdue(supplyOrderPool);
        return checkOverDue?AjaxResult.error(I18nUtil.getMessage("ui.data.alert.supplyOrderPool.overdue")):AjaxResult.success();
    }

    @Override
    public List<SupplyOrderPool> findAdjustSupplyOrderPool(DpDemandPlan createCondition) {
        return this.supplyOrderPoolEntityMapper.findAdjustSupplyOrderPool(createCondition);
    }

    /**
     * 获取配置信息
     *
     * @return 周转天数
     */
    private BigDecimal getTurnOverDays() {
        FactoryParam factoryParam = new FactoryParam();
        factoryParam.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        factoryParam.setParamCode(MonthPlanEnums.TURN_OVER_DAYS.getCode());
        factoryParam.setProductTypeCode(ProductTypeEnum.WHOLE_STEEL.getValue());
        FactoryParam param = iFactoryParamService.getFacParamSingle(factoryParam);
        String paramValue;
        if (param == null) {
          return BigDecimal.ZERO;
        }
        paramValue = StringUtils.isNotEmpty(param.getParamValue()) ? param.getParamValue() : param.getDefauleValue();
        return BigDecimalUtils.valueOf(paramValue);
    }


    /**
     * 计算备库上限值
     */
    private BigDecimal calculateStockLimit(MpMonthlySaleQty monthlySaleQty) {
        BigDecimal turnoverDays = getTurnOverDays();
        if(null == turnoverDays || null == monthlySaleQty || null == monthlySaleQty.getAverageSaleQty()) {
            return BigDecimal.ZERO;
        }
        return turnoverDays.multiply(BigDecimal.valueOf(monthlySaleQty.getAverageSaleQty()))
            .divide(BigDecimal.valueOf(DAYS_PER_MONTH), 0, RoundingMode.HALF_UP);
    }


}
