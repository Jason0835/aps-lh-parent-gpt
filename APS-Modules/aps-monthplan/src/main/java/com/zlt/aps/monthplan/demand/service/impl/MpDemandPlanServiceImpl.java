package com.zlt.aps.monthplan.demand.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.tlt.aps.constant.Constant;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.exception.BusinessException;
import com.tlt.aps.utils.BeanCopyUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.maindata.service.IMdmAreaCapaAllocationService;
import com.zlt.aps.maindata.service.IMdmFinishStockService;
import com.zlt.aps.maindata.service.IMpFinishedProductStockService;
import com.zlt.aps.monthplan.api.domain.entity.FactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.entity.MdmAreaCapaAllocation;
import com.zlt.aps.monthplan.api.domain.entity.MpDemandPlan;
import com.zlt.aps.monthplan.api.domain.entity.MpFinishedProductStock;
import com.zlt.aps.monthplan.api.domain.entity.MpOrderOffsetAllocation;
import com.zlt.aps.monthplan.api.domain.entity.SalesOrderPool;
import com.zlt.aps.monthplan.api.domain.entity.SupplyOrderPool;
import com.zlt.aps.monthplan.common.utils.RequirementVersionService;
import com.zlt.aps.monthplan.demand.mapper.MpDemandPlanEntityMapper;
import com.zlt.aps.monthplan.demand.service.IMpDemandPlanService;
import com.zlt.aps.monthplan.demand.service.IMpOrderOffsetAllocationService;
import com.zlt.aps.monthplan.demand.service.IMpSkuProductionTypeService;
import com.zlt.aps.monthplan.demand.service.ISalesOrderPoolService;
import com.zlt.aps.monthplan.demand.service.ISupplyOrderPoolService;
import com.zlt.aps.monthplan.factory.helper.SaleRequirePlanHelper;
import com.zlt.aps.monthplan.factory.helper.StockAllocationHelper;
import com.zlt.aps.monthplan.factory.mapper.FactoryProductionVersionMapper;
import com.zlt.aps.monthplan.factory.service.IMpMonthPlanProdFinalService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.ruoyi.common.datasource.service.BaseService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.apache.commons.collections4.CollectionUtils;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import lombok.extern.slf4j.Slf4j;

import com.zlt.common.utils.ImportExcelValidatedUtils;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpDemandPlanServiceImpl.java
 * 描    述：MpDemandPlanServiceImpl需求计划业务层处理
 *@author yelq
 *@date 2025-12-12
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
public class MpDemandPlanServiceImpl extends BaseService<MpDemandPlan>  implements IMpDemandPlanService
{

    private final MpDemandPlanEntityMapper mpDemandPlanEntityMapper;
    private final FactoryProductionVersionMapper factoryProductionVersionMapper;
    private final RequirementVersionService requirementVersionService;
    private final ISalesOrderPoolService salesOrderPoolService;
    // 成品库存
    private final IMpFinishedProductStockService finishedProductStockService;
    // 定稿的月度排产计划
    private final IMpMonthPlanProdFinalService mpMonthPlanProdFinalService;
    // 订单分配表
    private final IMpOrderOffsetAllocationService mpOrderOffsetAllocationService;
    // 版本库存
    private final IMdmFinishStockService mdmFinishStockService;
    // 区域产能分配
    private final IMdmAreaCapaAllocationService mdmAreaCapaAllocationService;
    // SKU排产分类
    private final IMpSkuProductionTypeService mpSkuProductionTypeService;
    // 供应链订单
    private final ISupplyOrderPoolService supplyOrderPoolService;

    /**
     * 查询需求计划
     * 
     * @param id 需求计划主键
     * @return 需求计划
     */
    @Override
    public MpDemandPlan selectMpDemandPlanById(Long id)
    {
        return mpDemandPlanEntityMapper.selectMpDemandPlanById(id);
    }

    /**
     * 查询需求计划列表
     * 
     * @param mpDemandPlan 需求计划
     * @return 需求计划
     */
    @Override
    public List<MpDemandPlan> selectMpDemandPlanList(MpDemandPlan mpDemandPlan)
    {
        return mpDemandPlanEntityMapper.selectMpDemandPlanList(mpDemandPlan);
    }

    /**
     * 批量查询需求计划列表
     *
     * @param ids 需要查询的数据主键集合
     * @return 需求计划集合
     */
    @Override
    public List<MpDemandPlan> selectMpDemandPlanByIds(List<Long> ids)
    {
        return super.executeSelectIn(
                    mpDemandPlanEntityMapper::selectMpDemandPlanByIds
                    ,ids
        );
    }


    /**
     * 新增需求计划
     * 
     * @param mpDemandPlan 需求计划
     * @return 结果
     */
    @Override
    public int insertMpDemandPlan(MpDemandPlan mpDemandPlan)
    {
        mpDemandPlan.setBaseVale(null);
        return mpDemandPlanEntityMapper.insert(mpDemandPlan);
    }

    /**
     * 修改需求计划
     * 
     * @param mpDemandPlan 需求计划
     * @return 结果
     */
    @Override
    public int updateMpDemandPlan(MpDemandPlan mpDemandPlan)
    {
        mpDemandPlan.setBaseVale(mpDemandPlan.getId());
        return mpDemandPlanEntityMapper.update(mpDemandPlan);
    }

    /**
     * 批量删除需求计划
     * 
     * @param ids 需要删除的需求计划主键
     * @return 结果
     */
    @Override
    public int deleteMpDemandPlanByIds(Long[] ids)
    {
        return mpDemandPlanEntityMapper.deleteMpDemandPlanByIds(ids);
    }

    /**
     * 批量删除需求计划
     *
     * @param ids 需要删除的需求计划主键
     * @return 结果
     */
    @Override
    public int deleteMpDemandPlanByIds(List<Long> ids)
    {
        Long[] arrayids = ids.toArray(new Long[0]);

        return this.deleteMpDemandPlanByIds(arrayids);
    }

    /**
     * 删除需求计划信息
     * 
     * @param id 需求计划主键
     * @return 结果
     */
    @Override
    public int deleteMpDemandPlanById(Long id)
    {
        return mpDemandPlanEntityMapper.deleteMpDemandPlanById(id);
    }

    @Override
    public void insertBatchData(Collection<MpDemandPlan> dataList) {

        this.insertBatchData(dataList, MpDemandPlanEntityMapper.class);
    }

    @Override
    public void updateBatchData(Collection<MpDemandPlan> dataList) {

        this.updateBatchData(dataList, MpDemandPlanEntityMapper.class);
    }

    @Override
    public void mergerIntoBatchData(List<MpDemandPlan> list) {
        this.mergerIntoBatchData(list, MpDemandPlanEntityMapper.class);
    }

    /**
     * 校验需求计划唯一性
     */
    @Override
    public String checkMpDemandPlanUnique(MpDemandPlan mpDemandPlan) {
        if (mpDemandPlan == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<MpDemandPlan> list = mpDemandPlanEntityMapper.selectMpDemandPlanList(mpDemandPlan);
        if (CollectionUtils.isNotEmpty(list)) {
            long iCount = list.stream().filter(x->!x.getId().equals(mpDemandPlan.getId())).count();
            return iCount == 0 ? UserConstants.UNIQUE : UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }
    /**
     * 导入需求计划数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<MpDemandPlan> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<MpDemandPlan> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MpDemandPlan mpDemandPlan = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, mpDemandPlan);
            ImportExcelValidatedUtils.validatedRepeat(list,mpDemandPlan,i,2,importLogId,validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                mpDemandPlan.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                mpDemandPlan.setBaseVale(null);
                importList.add(mpDemandPlan);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                    mpDemandPlanEntityMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    MpDemandPlan mpDemandPlan = list.get(i);
                    // 错误记录跳过
                    if (mpDemandPlan.getId() != null && mpDemandPlan.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkMpDemandPlanUnique(mpDemandPlan);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertMpDemandPlan(mpDemandPlan);
                    } else {
                        failureNum++;
                        //TODO:此处需手动填写唯一校验失败国际化信息
                        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");
                        ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.REPEAT.getCode(),i + 2,
                                String.format(uniqueMsg, i + 2), importErrorLogs);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            successNum = 0;
            failureNum = list.size();
            importErrorLogs.clear();
            ImportExcelValidatedUtils.addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
        }
        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    @Override
    public void createMonthRequire(MpDemandPlan createCondition) {
        // 如果已经定稿，不能重新生成需求计划
        if (factoryProductionVersionMapper.selectCount(Wrappers.lambdaQuery(FactoryProductionVersion.class)
            .eq(FactoryProductionVersion::getFactoryCode, createCondition.getFactoryCode())
            .eq(FactoryProductionVersion::getYear, createCondition.getYear())
            .eq(FactoryProductionVersion::getMonth, createCondition.getMonth())
            .eq(FactoryProductionVersion::getIsFinal, Constant.TRUE)) > 0) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.alert.demandPlan.checkFinal"));
        }

        // 1、创建新的需求版本号(REQ+yyyymmdd+3位流水号)，
        String monthPlanVersion = requirementVersionService.generateVersion();
        createCondition.setMonthPlanVersion(monthPlanVersion);
        // 2、查询获取销售订单池中的所有订单
        List<SalesOrderPool> salesOrders = this.salesOrderPoolService.findCurrentSalesOrderPool();
        // 3、查询获取所有成品库存
        List<MpFinishedProductStock> finishedProductStocks = this.finishedProductStockService.findCurrentFinishStock();
        Map<String,Long> stockQtyMap = this.getStockQtyMap(finishedProductStocks);
        Map<String,List<MpFinishedProductStock>> finishedProductStockMap = this.getFinishedProductStockMap(finishedProductStocks);
        // 计算月底计划余量 查询获取所有成品库存；同时计算月底计划余量：库存抓取日~（同月）月底的月度计划量汇总
        Map<String,Long> mdmMonthSurplusMap =  mpMonthPlanProdFinalService.calculateMonthSurplus(monthPlanVersion);
        // 4、从获取的销售订单池数据中筛选高、中优先级列表(供应链优先级 !=暂缓订单(5))
        List<SalesOrderPool> allocationOrders = Lists.newArrayList();
        List<SalesOrderPool> postponeOrders = Lists.newArrayList();
        if(CollectionUtils.isNotEmpty(salesOrders)){
            allocationOrders = salesOrders
                .stream()
                .filter(item -> !ApsConstant.SAL_PRIORITY_POSTPONE.equals(item.getOrderPriority()))
                .collect(Collectors.toList());
            postponeOrders = salesOrders.stream().filter(item -> ApsConstant.SAL_PRIORITY_POSTPONE.equals(item.getOrderPriority())).collect(Collectors.toList());
        }
        Map<String, List<SalesOrderPool>> saleOrderGroupMap = SaleRequirePlanHelper.getGroupSalesOrder(allocationOrders);

        //按照库存冲销顺序进行对冲
        List<MpOrderOffsetAllocation> orderOffsetAllocations = StockAllocationHelper.calculateStockAllocation(monthPlanVersion,saleOrderGroupMap,finishedProductStockMap,mdmMonthSurplusMap);
        // 6、分配完成后，得到销售订单的净需求(即库存+月底计划余量不满足订单量，还需要安排生产来满足订单需求)，此时需要查询区域产能分配是否有配置
        List<MpOrderOffsetAllocation> netDemands = orderOffsetAllocations.stream().filter(orderOffsetAllocation -> orderOffsetAllocation.getProduceQtyDue() > 0).collect(Collectors.toList());
        Map<String, String> productionTypeMap = mpSkuProductionTypeService.skuToProductionType();
        // 7、将分配冲减后的结果记录到订单分配表中(以需求版本号的维度)；
        mpOrderOffsetAllocationService.insertBatchData(orderOffsetAllocations);
        // 8、将分配时的成品库存记录到库存版本表中(以需求版本号的维度)；
        mdmFinishStockService.insertBatchData(createCondition,monthPlanVersion,finishedProductStockMap);
        List<MpDemandPlan> demandPlans = Lists.newArrayList();
        if(CollectionUtils.isNotEmpty(netDemands)){
            // 9、将销售订单池中高优先级、中优先级订单冲减后还有需求量的订单，纳入需求计划；并根据SKU排产分类表中的SKU分类，填充排产分类类型值
            List<MdmAreaCapaAllocation>  areaCapaAllocations =  mdmAreaCapaAllocationService.findAreaCapaAllocation(createCondition);
            //  (1)区域产能没有配置，则销售订单净需求直接转化为需求计划：需求计划.订单类型 = 销售订单.订单类型，需求计划.供应链优先级 = 销售订单.供应链优先级
            if(CollectionUtils.isEmpty(areaCapaAllocations)){
                transformSaleOrderToDemandPlan(netDemands,demandPlans,productionTypeMap);
            }else{
                // (2)区域产能有配置，则对销售订单净需求按区域维度分组并汇总各销售区域净需求量T，根据销售需求量与区域产能配置量进行销售订单净需求.供应链优先级调整
                Map<String,List<MpOrderOffsetAllocation>> netDemandsGroupByArea = netDemands.stream().collect(Collectors.groupingBy(MpOrderOffsetAllocation::getAreaCode));
                Map<String,List<MdmAreaCapaAllocation>>  areaCapaAllocationsGroupByArea =   areaCapaAllocations.stream().collect(Collectors.groupingBy(MdmAreaCapaAllocation::getAreaCode));
                netDemandsGroupByArea.forEach((key,value) -> {
                    // （2.1) 汇总销售区域净需求 T = SUM(销售订单.区域 = 销售订单池.区域)销售订单.净需求
                    // （2.2) 并对销售区域净需求列表，按供应链优先级升序 -> 提报时间升序 -> 净需求量降序 排序
                    List<MpOrderOffsetAllocation> sortedOrders = getSortedOrders(value);
                    // (2.3) 匹配区域产能配置(销售订单.区域 = 区域产能配置.区域)，调整销售订单净需求的供应链优先级
                    if(!areaCapaAllocationsGroupByArea.containsKey(key)){
                        transformSaleOrderToDemandPlan(sortedOrders,demandPlans,productionTypeMap);
                        return;
                    }
                    List<MdmAreaCapaAllocation> areaCapaAllocationList = areaCapaAllocationsGroupByArea.get(key);
                    long capacityAllocation = areaCapaAllocationList.stream().mapToLong(MdmAreaCapaAllocation::getCapacityAllocation).sum();
                    long totalNetDemandQty = sortedOrders.stream().mapToLong(MpOrderOffsetAllocation::getProduceQtyDue).sum();
                    // (2.4) 销售订单区域净需求超出区域产能，则销售订单超出部分的净需求，需求计划.供应链优先级 = 中优先级
                    if(totalNetDemandQty >= capacityAllocation){
                        long overAreaCapacityValue = totalNetDemandQty - capacityAllocation;
                        processDemandPriorityExcludingLast(sortedOrders, overAreaCapacityValue);
                        transformSaleOrderToDemandPlan(sortedOrders,demandPlans,productionTypeMap);
                        return;
                    }
                    //  (2.2) 区域净需求少于区域产能，则需求计划.供应链优先级，统一调整到高优先级
                    sortedOrders.forEach(orderOffsetAllocation -> orderOffsetAllocation.setOrderPriority(ApsConstant.SAL_PRIORITY_HIGHT));
                    transformSaleOrderToDemandPlan(sortedOrders,demandPlans,productionTypeMap);
                });
            }
        }

        // 10、同时将销售订单池中的暂缓订单、供应链订单池中的SKU，也纳入需求计划；并根据SKU排产分类表中的SKU分类，填充排产分类类型值
        if(CollectionUtils.isNotEmpty(postponeOrders)) {
            postponeOrders.forEach(postponeOrder -> {
                demandPlans.add(buildDemandPlan(postponeOrder,createCondition,productionTypeMap,stockQtyMap,mdmMonthSurplusMap));
            });
        }
        List<SupplyOrderPool> supplyOrderPools = supplyOrderPoolService.findCurrentSupplyOrderPool();
        if(CollectionUtils.isNotEmpty(supplyOrderPools)) {
            supplyOrderPools.forEach(supplyOrder -> {
                demandPlans.add(buildDemandPlan(supplyOrder,createCondition,productionTypeMap,stockQtyMap,mdmMonthSurplusMap));
            });
        }
        // 11、对需求计划，按SKU、动平衡、均匀性、年周号为维度分组合并(形成需求版本号)，并写入需求计划表
        List<MpDemandPlan> mergedDemandPlans = mergedDemandPlan(demandPlans);
        if(CollectionUtils.isNotEmpty(mergedDemandPlans)){
            this.insertBatchData(mergedDemandPlans);
        }
    }

    private List<MpDemandPlan> mergedDemandPlan(List<MpDemandPlan> demandPlans) {
        if(CollectionUtils.isEmpty(demandPlans)){
            return Lists.newArrayList();
        }
        List<MpDemandPlan> mergedDemandPlans = Lists.newArrayList();
        Map<String,List<MpDemandPlan>>  mapByGroupKey = demandPlans.stream().collect(Collectors.groupingBy(MpDemandPlan::getGroupKey));
        mapByGroupKey.forEach((key,value) -> {
            MpDemandPlan entity = BeanCopyUtils.copyBean(value.get(0),MpDemandPlan.class);

            // entity.setMesMaterialCode();
            long  heightQty = value.stream()
                .filter(item ->
                    ApsConstant.SAL_PRIORITY_HIGHT.equals(item.getOrderPriority()))
                .mapToLong(MpDemandPlan::getNetQty).sum();
            long  midQty = value.stream()
                .filter(item ->
                    ApsConstant.SAL_PRIORITY_MID.equals(item.getOrderPriority()))
                .mapToLong(MpDemandPlan::getNetQty).sum();
            long  postponeQty = value.stream()
                .filter(item ->
                    ApsConstant.SAL_PRIORITY_POSTPONE.equals(item.getOrderPriority()))
                .mapToLong(MpDemandPlan::getNetQty).sum();
            long  cycleReserveQty = value.stream()
                .filter(item ->
                    ApsConstant.SAL_PRIORITY_CYCLE_STOCK_UP.equals(item.getOrderPriority()))
                .mapToLong(MpDemandPlan::getNetQty).sum();
            long  conventionReserveQty = value.stream()
                .filter(item ->
                    ApsConstant.SAL_PRIORITY_PRECEDENT_STOCK_UP.equals(item.getOrderPriority()))
                .mapToLong(MpDemandPlan::getNetQty).sum();
            long  netQty = value.stream().mapToLong(MpDemandPlan::getNetQty).sum();
            entity.setNetQty(netQty);
            //    (8)净需求(含暂缓) = 高优先级净需求量 + 中优先级净需求量+暂缓订单需求量
            entity.setPostponeNetQty(heightQty + midQty + postponeQty);
            //   (9)净需求(不含暂缓) = 高优先级净需求量 + 中优先级净需求量
            entity.setUnPostponeNetQty(heightQty + midQty);
            //  (10)供应链优先级 = ""
            if(value.size()>1){
                entity.setScmPriority(StringUtils.EMPTY);
            }
            entity.setHeightQty(heightQty);
            entity.setMidQty(midQty);
            entity.setPostponeQty(postponeQty);
            entity.setCycleReserveQty(cycleReserveQty);
            entity.setConventionReserveQty(conventionReserveQty);
            // demandPlan.setIsReachMinProductionQty();
            // demandPlan.setMinProductionQty();
            entity.setPlanType(ApsConstant.APS_ZERO_1);
            // demandPlan.setChannel();
            // demandPlan.setProSize();
            // demandPlan.setSpecifications();
            // demandPlan.setPattern();
            // demandPlan.setHierarchy();
            // demandPlan.setSpeed();
            // demandPlan.setIsImportantCustom();
            // demandPlan.setIsEnsurePlan();
            // demandPlan.setIsEmergency();
            // demandPlan.setIsDebitPlan();
            // demandPlan.setDeliveryDateDue();
            entity.setIsImport(YesOrNoEnum.NO.getCode());
            mergedDemandPlans.add(entity);

        });
        return mergedDemandPlans;
    }

    private MpDemandPlan buildDemandPlan(SupplyOrderPool supplyOrder, MpDemandPlan createCondition, Map<String, String> productionTypeMap, Map<String, Long> stockQtyMap, Map<String, Long> mdmMonthSurplusMap) {
        MpDemandPlan demandPlan = new MpDemandPlan();
        demandPlan.setFactoryCode(supplyOrder.getFactoryCode());
        demandPlan.setYear(createCondition.getYear());
        demandPlan.setMonth(createCondition.getMonth());
        demandPlan.setMonthPlanVersion(createCondition.getMonthPlanVersion());
        demandPlan.setOrderPriority(supplyOrder.getOrderType());
        // demandPlan.setIsAlternateMaterial();
        demandPlan.setProductTypeCode(supplyOrder.getProductTypeCode());
        demandPlan.setLocationType(supplyOrder.getLocationType());
        demandPlan.setBrand(supplyOrder.getBrand());
        // demandPlan.setScmPriority(supplyOrder.getScmPriority());
        // demandPlan.setStructureName();
        // demandPlan.setMainPattern();
        demandPlan.setMaterialCode(supplyOrder.getMaterialCode());
        demandPlan.setMaterialDesc(supplyOrder.getMaterialDesc());
        demandPlan.setProductionType(productionTypeMap.get(supplyOrder.getMaterialCode()));
        // demandPlan.setYearWeek(postponeOrder.getWeekYear());
        // demandPlan.setIsDynamicBalance(postponeOrder.getDynamicBalance());
        // demandPlan.setIsUniformity(postponeOrder.getUniformity());
        demandPlan.setOrderQty(supplyOrder.getQty()==null? BigDecimal.ZERO.longValue() : supplyOrder.getQty());
        demandPlan.setStockQty(stockQtyMap.getOrDefault(supplyOrder.getGroupKey(),0L));
        // demandPlan.setPlannedSurplus();
        demandPlan.setNetQty(demandPlan.getOrderQty());
        demandPlan.setIsProduction(YesOrNoEnum.YES.getCode());
        // demandPlan.setPostponeNetQty();
        // demandPlan.setUnPostponeNetQty();
        // demandPlan.setHeightQty();
        // demandPlan.setMidQty();
        // demandPlan.setPostponeQty();
        // demandPlan.setCycleReserveQty();
        // demandPlan.setConventionReserveQty();
        // demandPlan.setIsReachMinProductionQty();
        // demandPlan.setMinProductionQty();
        // demandPlan.setPlanType();
        // demandPlan.setChannel();
        // demandPlan.setProSize();
        // demandPlan.setSpecifications();
        // demandPlan.setPattern();
        // demandPlan.setHierarchy();
        // demandPlan.setSpeed();
        // demandPlan.setIsImportantCustom();
        // demandPlan.setIsEnsurePlan();
        // demandPlan.setIsEmergency();
        // demandPlan.setIsDebitPlan();
        // demandPlan.setDeliveryDateDue();
        demandPlan.setIsImport(YesOrNoEnum.NO.getCode());
        return demandPlan;
    }

    private Map<String, Long> getStockQtyMap(List<MpFinishedProductStock> finishedProductStocks) {
        if(CollectionUtils.isEmpty(finishedProductStocks)){
            return Collections.emptyMap();
        }
        return finishedProductStocks.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(
                MpFinishedProductStock::getGroupKey,
                Collectors.summingLong(MpFinishedProductStock::getStockQty)
            ));
    }

    private MpDemandPlan buildDemandPlan(SalesOrderPool postponeOrder,MpDemandPlan createCondition,Map<String, String> productionTypeMap,Map<String,Long> stockQtyMap,Map<String,Long> mdmMonthSurplusMap) {
        MpDemandPlan demandPlan = new MpDemandPlan();
        demandPlan.setFactoryCode(postponeOrder.getFactoryCode());
        demandPlan.setYear(createCondition.getYear());
        demandPlan.setMonth(createCondition.getMonth());
        demandPlan.setMonthPlanVersion(createCondition.getMonthPlanVersion());
        demandPlan.setOrderPriority(postponeOrder.getOrderPriority());
        // demandPlan.setIsAlternateMaterial();
        demandPlan.setProductTypeCode(postponeOrder.getProductType());
        // demandPlan.setLocationType();
        demandPlan.setBrand(postponeOrder.getBrand());
        demandPlan.setScmPriority(postponeOrder.getScmPriority());
        // demandPlan.setStructureName();
        // demandPlan.setMainPattern();
        demandPlan.setMaterialCode(postponeOrder.getOriMaterialCode());
        demandPlan.setMaterialDesc(postponeOrder.getMaterialDesc());
        demandPlan.setProductionType(productionTypeMap.get(postponeOrder.getOriMaterialCode()));
        demandPlan.setYearWeek(postponeOrder.getWeekYear());
        demandPlan.setIsDynamicBalance(postponeOrder.getDynamicBalance());
        demandPlan.setIsUniformity(postponeOrder.getUniformity());
        demandPlan.setOrderQty(postponeOrder.getOrdQty()==null? BigDecimal.ZERO.longValue() : postponeOrder.getOrdQty().longValue());
        demandPlan.setStockQty(stockQtyMap.getOrDefault(postponeOrder.getGroupKey(),0L));
        // demandPlan.setPlannedSurplus();
        demandPlan.setNetQty(demandPlan.getOrderQty());
        demandPlan.setIsProduction(YesOrNoEnum.YES.getCode());
        // demandPlan.setPostponeNetQty();
        // demandPlan.setUnPostponeNetQty();
        // demandPlan.setHeightQty();
        // demandPlan.setMidQty();
        // demandPlan.setPostponeQty();
        // demandPlan.setCycleReserveQty();
        // demandPlan.setConventionReserveQty();
        // demandPlan.setIsReachMinProductionQty();
        // demandPlan.setMinProductionQty();
        // demandPlan.setPlanType();
        // demandPlan.setChannel();
        // demandPlan.setProSize();
        // demandPlan.setSpecifications();
        // demandPlan.setPattern();
        // demandPlan.setHierarchy();
        // demandPlan.setSpeed();
        // demandPlan.setIsImportantCustom();
        // demandPlan.setIsEnsurePlan();
        // demandPlan.setIsEmergency();
        // demandPlan.setIsDebitPlan();
        // demandPlan.setDeliveryDateDue();
        demandPlan.setIsImport(YesOrNoEnum.NO.getCode());
        return demandPlan;
    }

    /**
     * 从列表尾端开始累加净需求量，直到达到或超过指定值
     * 注意：跳出循环的那个订单不修改优先级
     *
     * @param sortedOrders 排序后的净需求列表
     * @param overAreaCapacityValue 超出区域产能值
     */
    public void processDemandPriorityExcludingLast(
        List<MpOrderOffsetAllocation> sortedOrders,
        long overAreaCapacityValue) {

        if (CollectionUtils.isEmpty(sortedOrders) || overAreaCapacityValue <= 0) {
            return;
        }
        long accumulatedQty = 0;
        // 从列表尾端开始遍历
        for (int i = sortedOrders.size() - 1; i >= 0; i--) {
            MpOrderOffsetAllocation order = sortedOrders.get(i);
            // 跳过已处理或无效的订单
            if (order == null || order.getProduceQtyDue() == null || order.getProduceQtyDue() <= 0) {
                continue;
            }
            // 检查当前累加值是否已经达到或超过阈值
            // 注意：先检查，再累加
            long currentOrderQty = order.getProduceQtyDue();
            if (accumulatedQty + currentOrderQty >= overAreaCapacityValue) {
                break;
            } else {
                // 累加净需求量并设置优先级
                accumulatedQty += currentOrderQty;
                order.setOrderPriority(ApsConstant.SAL_PRIORITY_MID);
            }
        }
    }

    private List<MpOrderOffsetAllocation> getSortedOrders(List<MpOrderOffsetAllocation> saleOrders) {
        return saleOrders.stream()
            .sorted(getHighPerformanceComparator())
            .collect(Collectors.toList());
    }

    /**
     * 高性能自定义比较器（适用于大数据量）
     */
    private  Comparator<MpOrderOffsetAllocation> getHighPerformanceComparator() {
        return new SalesOrderComparator();
    }

    /**
     * 自定义高性能比较器实现
     * 避免重复解析和lambda开销
     */
    private static class SalesOrderComparator implements Comparator<MpOrderOffsetAllocation> {

        @Override
        public int compare(MpOrderOffsetAllocation o1, MpOrderOffsetAllocation o2) {
            // 1. 比较供应链优先级
            int scmPriorityCompare = compareScmPriority(o1, o2);
            if (scmPriorityCompare != 0) {
                return scmPriorityCompare;
            }

            // 2. 比较提报日期
            int dateCompare = compareBillDate(o1, o2);
            if (dateCompare != 0) {
                return dateCompare;
            }

            // 3. 比较提报量
            return compareOrdQty(o1, o2);
        }

        private int compareScmPriority(MpOrderOffsetAllocation o1, MpOrderOffsetAllocation o2) {
            Integer p1 = parseScmPriority(o1.getScmPriority());
            Integer p2 = parseScmPriority(o2.getScmPriority());

            if (p1 == null && p2 == null) {
                return 0;
            }
            if (p1 == null) {
                return 1; // null排最后
            }
            if (p2 == null) {
                return -1;
            }

            return Integer.compare(p1, p2);
        }

        private int compareBillDate(MpOrderOffsetAllocation o1, MpOrderOffsetAllocation o2) {
            Date d1 = o1.getBillDate();
            Date d2 = o2.getBillDate();

            if (d1 == null && d2 == null) {
                return 0;
            }
            // null排最后
            if (d1 == null) {
                return 1;
            }
            if (d2 == null) {
                return -1;
            }

            return d1.compareTo(d2);
        }

        private int compareOrdQty(MpOrderOffsetAllocation o1, MpOrderOffsetAllocation o2) {
            Long q1 = o1.getProduceQtyDue();
            Long q2 = o2.getProduceQtyDue();

            if (q1 == null && q2 == null) {
                return 0;
            }
            // null排最后
            if (q1 == null) {
                return 1;
            }
            if (q2 == null) {
                return -1;
            }
            return q1.compareTo(q2);
        }

        private Integer parseScmPriority(String scmPriority) {
            if (scmPriority == null || scmPriority.trim().isEmpty()) {
                return null;
            }
            try {
                return Integer.parseInt(scmPriority.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }


    private void transformSaleOrderToDemandPlan(List<MpOrderOffsetAllocation> netDemands, List<MpDemandPlan> demandPlans,Map<String, String> productionTypeMap) {
        netDemands.forEach(netDemand -> demandPlans.add(buildDemandPlan(netDemand,productionTypeMap)));
    }

    private MpDemandPlan buildDemandPlan(MpOrderOffsetAllocation netDemand,Map<String, String> productionTypeMap) {
        MpDemandPlan demandPlan = new MpDemandPlan();
        BeanUtils.copyProperties(netDemand, demandPlan);
        demandPlan.setId(null);
        demandPlan.setBaseVale(null);
        // demandPlan.setIsAlternateMaterial();
        // demandPlan.setStructureName();
        // demandPlan.setMainPattern();
        // 数据字典：biz_product_characteristics
        //1 主销产品 2 常规产品 3 周期排产产品 4 波动性产品
        //5 按单排产产品
        demandPlan.setProductionType(productionTypeMap.getOrDefault(netDemand.getMaterialCode(), StringUtils.EMPTY));
        demandPlan.setYearWeek(netDemand.getWeekYear());
        demandPlan.setIsDynamicBalance(netDemand.getDynamicBalance());
        demandPlan.setIsUniformity(netDemand.getUniformity());
        demandPlan.setNetQty(netDemand.getProduceQtyDue());
        demandPlan.setIsProduction(YesOrNoEnum.YES.getCode());
        // demandPlan.setPostponeNetQty();
        // demandPlan.setUnPostponeNetQty();
        if(ApsConstant.SAL_PRIORITY_HIGHT.equals(netDemand.getOrderPriority())){
            demandPlan.setHeightQty(netDemand.getProduceQtyDue());
        }else if(ApsConstant.SAL_PRIORITY_MID.equals(netDemand.getOrderPriority())) {
            demandPlan.setMidQty(netDemand.getProduceQtyDue());
        }
        // demandPlan.setPostponeQty();
        // demandPlan.setCycleReserveQty();
        // demandPlan.setConventionReserveQty();
        // demandPlan.setIsReachMinProductionQty();
        // demandPlan.setMinProductionQty();
        // demandPlan.setPlanType();
        // demandPlan.setChannel();
        // demandPlan.setProSize();
        // demandPlan.setSpecifications();
        // demandPlan.setPattern();
        // demandPlan.setHierarchy();
        // demandPlan.setSpeed();
        // demandPlan.setIsImportantCustom();
        // demandPlan.setIsEnsurePlan();
        // demandPlan.setIsEmergency();
        // demandPlan.setIsDebitPlan();
        // demandPlan.setDeliveryDateDue();
        demandPlan.setIsImport(YesOrNoEnum.NO.getCode());
        return demandPlan;
    }

    private Map<String,List<MpFinishedProductStock>> getFinishedProductStockMap(List<MpFinishedProductStock> finishedProductStocks) {
        if (CollectionUtils.isEmpty(finishedProductStocks)) {
            return Collections.emptyMap();
        }
      return finishedProductStocks
          .parallelStream()
          .filter(Objects::nonNull)
          .filter(finishedProductStock -> finishedProductStock.getGroupKey() != null)
          .peek(item -> item.setLeftOverQty(item.getStockQty()))
          .collect(Collectors.groupingByConcurrent(
              MpFinishedProductStock::getGroupKey,
              Collectors.toCollection(ArrayList::new)
          ));
    }


}
