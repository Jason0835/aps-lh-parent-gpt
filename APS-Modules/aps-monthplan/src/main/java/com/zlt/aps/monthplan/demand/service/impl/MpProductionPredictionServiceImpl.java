package com.zlt.aps.monthplan.demand.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.datasource.service.BaseService;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.Constant;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.exception.BusinessException;
import com.zlt.aps.maindata.mapper.MpProductionPredictionEntityMapper;
import com.zlt.aps.maindata.service.IMdmProductStockService;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.aps.monthplan.common.utils.MonthCalculator;
import com.zlt.aps.monthplan.common.utils.RequirementVersionService;
import com.zlt.aps.monthplan.demand.service.IDpOrderOffsetDetailService;
import com.zlt.aps.monthplan.demand.service.IDpStockVersionService;
import com.zlt.aps.monthplan.demand.service.IMpProductionPredictionService;
import com.zlt.aps.monthplan.demand.service.ISalesOrderPoolService;
import com.zlt.aps.monthplan.factory.helper.SaleRequirePlanHelper;
import com.zlt.aps.monthplan.factory.helper.StockAllocationHelper;
import com.zlt.aps.monthplan.factory.mapper.FactoryProductionVersionMapper;
import com.zlt.aps.monthplan.factory.service.IMonthPlanSurplusService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpProductionPredictionServiceImpl.java
 * 描    述：MpProductionPredictionServiceImplS2-1002.未来产量预测业务层处理
 *@author yelq
 *@date 2025-12-21
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
public class MpProductionPredictionServiceImpl extends BaseService<MpProductionPrediction>  implements IMpProductionPredictionService
{
    private static final String PREFIX = "PRE";
    private final RequirementVersionService requirementVersionService;

    private final MpProductionPredictionEntityMapper mpProductionPredictionEntityMapper;
    private final FactoryProductionVersionMapper factoryProductionVersionMapper;
    // 销售订单
    private final ISalesOrderPoolService salesOrderPoolService;
    // 成品库存
    private final IMdmProductStockService mdmProductStockService;
    // 月底计划余量
    private final IMonthPlanSurplusService monthPlanSurplusService;
    // 订单分配表
    private final IDpOrderOffsetDetailService dpOrderOffsetDetailService;
    // 版本库存
    private final IDpStockVersionService dpStockVersionService;

    /**
     * 查询S2-1002.未来产量预测
     *
     * @param id S2-1002.未来产量预测主键
     * @return S2-1002.未来产量预测
     */
    @Override
    public MpProductionPrediction selectMpProductionPredictionById(Long id)
    {
        return mpProductionPredictionEntityMapper.selectById(id);
    }

    /**
     * 查询S2-1002.未来产量预测列表
     *
     * @param mpProductionPrediction S2-1002.未来产量预测
     * @return S2-1002.未来产量预测
     */
    @Override
    public List<MpProductionPrediction> selectMpProductionPredictionList(MpProductionPrediction mpProductionPrediction)
    {
        return mpProductionPredictionEntityMapper.selectList(null);
    }

    /**
     * 批量查询S2-1002.未来产量预测列表
     *
     * @param ids 需要查询的数据主键集合
     * @return S2-1002.未来产量预测集合
     */
    @Override
    public List<MpProductionPrediction> selectMpProductionPredictionByIds(List<Long> ids)
    {
        return super.executeSelectIn(
                    mpProductionPredictionEntityMapper::selectBatchIds
                    ,ids
        );
    }


    /**
     * 新增S2-1002.未来产量预测
     *
     * @param mpProductionPrediction S2-1002.未来产量预测
     * @return 结果
     */
    @Override
    public int insertMpProductionPrediction(MpProductionPrediction mpProductionPrediction)
    {
        mpProductionPrediction.setBaseVale(null);
        return mpProductionPredictionEntityMapper.insert(mpProductionPrediction);
    }

    /**
     * 修改S2-1002.未来产量预测
     *
     * @param mpProductionPrediction S2-1002.未来产量预测
     * @return 结果
     */
    @Override
    public int updateMpProductionPrediction(MpProductionPrediction mpProductionPrediction)
    {
        mpProductionPrediction.setBaseVale(mpProductionPrediction.getId());
        return mpProductionPredictionEntityMapper.updateById(mpProductionPrediction);
    }

    /**
     * 批量删除S2-1002.未来产量预测
     *
     * @param ids 需要删除的S2-1002.未来产量预测主键
     * @return 结果
     */
    @Override
    public int deleteMpProductionPredictionByIds(Long[] ids)
    {
        return mpProductionPredictionEntityMapper.deleteBatchIds(Arrays.asList(ids));
    }

    /**
     * 批量删除S2-1002.未来产量预测
     *
     * @param ids 需要删除的S2-1002.未来产量预测主键
     * @return 结果
     */
    @Override
    public int deleteMpProductionPredictionByIds(List<Long> ids)
    {
        Long[] arrayids = ids.toArray(new Long[0]);

        return this.deleteMpProductionPredictionByIds(arrayids);
    }

    /**
     * 删除S2-1002.未来产量预测信息
     *
     * @param id S2-1002.未来产量预测主键
     * @return 结果
     */
    @Override
    public int deleteMpProductionPredictionById(Long id)
    {
        return mpProductionPredictionEntityMapper.deleteById(id);
    }

    @Override
    public void insertBatchData(Collection<MpProductionPrediction> dataList) {

       // this.mpProductionPredictionEntityMapper.insertBatchData(dataList, MpProductionPredictionEntityMapper.class);
    }

    @Override
    public void updateBatchData(Collection<MpProductionPrediction> dataList) {

        //this.updateBatchData(dataList, MpProductionPredictionEntityMapper.class);
    }

    @Override
    public void mergerIntoBatchData(List<MpProductionPrediction> list) {
        //this.mergerIntoBatchData(list, MpProductionPredictionEntityMapper.class);
    }

    /**
     * 校验S2-1002.未来产量预测唯一性
     */
    @Override
    public String checkMpProductionPredictionUnique(MpProductionPrediction mpProductionPrediction) {
       /* if (mpProductionPrediction == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<MpProductionPrediction> list = mpProductionPredictionEntityMapper.selectMpProductionPredictionList(mpProductionPrediction);
        if (CollectionUtils.isNotEmpty(list)) {
            long iCount = list.stream().filter(x->!x.getId().equals(mpProductionPrediction.getId())).count();
            return iCount == 0 ? UserConstants.UNIQUE : UserConstants.NOT_UNIQUE;
        }*/
        return UserConstants.UNIQUE;
    }
    /**
     * 导入S2-1002.未来产量预测数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<MpProductionPrediction> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<MpProductionPrediction> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MpProductionPrediction mpProductionPrediction = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, mpProductionPrediction);
            ImportExcelValidatedUtils.validatedRepeat(list,mpProductionPrediction,i,2,importLogId,validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                mpProductionPrediction.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                mpProductionPrediction.setBaseVale(null);
                importList.add(mpProductionPrediction);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                   // mpProductionPredictionEntityMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    MpProductionPrediction mpProductionPrediction = list.get(i);
                    // 错误记录跳过
                    if (mpProductionPrediction.getId() != null && mpProductionPrediction.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkMpProductionPredictionUnique(mpProductionPrediction);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertMpProductionPrediction(mpProductionPrediction);
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
    public AjaxResult createMonthPrediction(MpProductionPrediction createCondition) {
        // 2、得到T月、T+1月、T+2月。T月 = 当前操作日所在年月(当月) +1 ；T+1月 = 在T月的基础上+1个月；T+2月 = 在T月的基础上+2个月
        MonthCalculator.MonthRangeResult monthRangeResult = MonthCalculator.calculateMonthRanges();
        // 3、检查是否已有T月月度计划(定稿)
        //   (1) 若 不存在T月月度计划，则提示"T月月度生产计划还未定稿，请先生成及定稿！"，系统不做任何处理。
        List<FactoryProductionVersion> finalVersions =  validateProductionVersionFinalized(monthRangeResult.getTMonth());
        if (CollectionUtils.isEmpty(finalVersions)) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.alert.productionPrediction.checkFinal"));
        }
        FactoryProductionVersion finalVersion =  finalVersions.get(0);
        createCondition.setMonthPlanVersion(finalVersion.getMonthPlanVersion());
        createCondition.setProductionVersion(finalVersion.getProductionVersion());
        // 4、生成预测版本号(PRE+yyyymmdd+3位流水号)
        String predictionVersion = requirementVersionService.generateVersion(PREFIX);
        createCondition.setPredictionVersion(predictionVersion);
        createCondition.setYear(monthRangeResult.getTMonth().getYear());
        createCondition.setMonth(monthRangeResult.getTMonth().getMonthValue());
        // 5. 并行获取数据
        DataCollection data = fetchRequiredDataInParallel();
        // 7、对销售订单池的订单(高优先级、中优先级、暂缓订单)，进行库存(包含当月底计划余量)冲减【参见生成需求计划的库存冲减逻辑】，注：暂缓订单也参与冲减
        //  (1) 得到对冲后的销售订单净需求数据(包含暂缓订单+高优先级+中优先级的净需求)
        //   (2) 同时，保存预测版本号T月的订单分配结果
        // 4. 处理销售订单分配
        OrderAllocationResult allocationResult = processSalesOrderAllocation(
            predictionVersion, data.getSalesOrders(), data.getFinishedProductStockMap(),
            data.getMonthSurplusMap());
        // 5. 批量保存分配结果
        saveAllocationResults(createCondition,allocationResult);

        return null;
    }


    /**
     * 处理销售订单分配
     */
    private OrderAllocationResult processSalesOrderAllocation(
        String monthPlanVersion,
        List<SalesOrderPool> allocationOrders,
        Map<String, List<MdmProductStock>> finishedProductStockMap,
        Map<String, Long> monthSurplusMap) {
        if (CollectionUtils.isEmpty(allocationOrders)) {
            return new OrderAllocationResult(
                Collections.emptyList(),
                Collections.emptyList(),
                finishedProductStockMap
            );
        }
        // 分组销售订单
        Map<String, List<SalesOrderPool>> saleOrderGroupMap =
            SaleRequirePlanHelper.getGroupSalesOrder(allocationOrders);
        // 计算库存分配
        List<DpOrderOffsetDetail> allocations = StockAllocationHelper.calculateStockAllocation(
            monthPlanVersion, saleOrderGroupMap, finishedProductStockMap, monthSurplusMap);
        // 过滤净需求
        List<DpOrderOffsetDetail> netDemands = allocations.stream()
            .filter(allocation -> allocation.getProducionQty() > 0)
            .collect(Collectors.toList());
        return new OrderAllocationResult(allocations, netDemands, finishedProductStockMap);
    }

    /**
     * 批量保存分配结果
     */
    private void saveAllocationResults(
        MpProductionPrediction createCondition,
        OrderAllocationResult allocationResult) {
        // 批量插入分配结果
        if (CollectionUtils.isNotEmpty(allocationResult.getAllocations())) {
            this.dpOrderOffsetDetailService.insertBatchData(allocationResult.getAllocations());
        }
        // 批量插入库存版本
        dpStockVersionService.insertBatchData(createCondition,allocationResult.getStockMap());
    }

    /**
     * 并行获取所有必要数
     */
    private DataCollection fetchRequiredDataInParallel() {
        CompletableFuture<List<SalesOrderPool>> salesOrdersFuture =
            CompletableFuture.supplyAsync(this::fetchSalesOrderPool);
        CompletableFuture<List<MdmProductStock>> stocksFuture =
            CompletableFuture.supplyAsync(this::fetchFinishedProductStocks);
        CompletableFuture<Map<String, Long>> monthSurplusFuture =
            CompletableFuture.supplyAsync(this::fetchMonthSurplusMap);

        // 等待所有任务完成
        CompletableFuture.allOf(
            salesOrdersFuture, stocksFuture, monthSurplusFuture
        ).join();

        try {
            List<SalesOrderPool> salesOrders = salesOrdersFuture.get();
            List<MdmProductStock> finishedProductStocks = stocksFuture.get();
            Map<String, Long> monthSurplusMap = monthSurplusFuture.get();
            // 处理成品库存映射
            Map<String, List<MdmProductStock>> finishedProductStockMap =
                CollectionUtils.isEmpty(finishedProductStocks) ?
                    new HashMap<>(16) :
                    finishedProductStocks.stream()
                        .collect(Collectors.groupingBy(MdmProductStock::getGroupKey));

            return new DataCollection(
                salesOrders,
                finishedProductStocks,
                finishedProductStockMap,
                monthSurplusMap
            );

        } catch (Exception e) {
            log.error("并行获取数据失败", e);
            throw new BusinessException("获取数据失败");
        }
    }

    private Map<String, Long> fetchMonthSurplusMap() {
        List<MdmMonthSurplus> mdmMonthSurpluses = monthPlanSurplusService.findCurrentMonthPlanSurplus();
        if(CollectionUtils.isEmpty(mdmMonthSurpluses)){
            return Collections.emptyMap();
        }
        return mdmMonthSurpluses.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(
                MdmMonthSurplus::getGroupKey,
                Collectors.summingLong(MdmMonthSurplus::getPlanSurplusQty)
            ));
    }

    /**
     *  6、从成品库存表中获取库存；同时，获取T-1月新的月底计划余量(如果库存日期 > T-1月，则月底计划余量 = 0)；
     * @return 成品库存
     */
    private List<MdmProductStock> fetchFinishedProductStocks() {
        return mdmProductStockService.findCurrentFinishStock();
    }

    /**
     *  5、查询截止预测日，在销售订单池中的所有订单；
     * @return 销售订单
     */
    private List<SalesOrderPool> fetchSalesOrderPool() {
        return salesOrderPoolService.findCurrentSalesOrderPool();
    }

    /**
     *   3、检查是否已有T月月度计划(定稿)
     *       (1) 若 不存在T月月度计划，则提示"T月月度生产计划还未定稿，请先生成及定稿！"，系统不做任何处理。
     * @param tMonth T月
     */
    private List<FactoryProductionVersion> validateProductionVersionFinalized(YearMonth tMonth) {
      return factoryProductionVersionMapper.selectList(
          Wrappers.<FactoryProductionVersion>lambdaQuery()
              .eq(FactoryProductionVersion::getFactoryCode, FactoryConstant.DEFAULT_FACTORY_CODE)
              .eq(FactoryProductionVersion::getYear, tMonth.getYear())
              .eq(FactoryProductionVersion::getMonth, tMonth.getMonthValue())
              .eq(FactoryProductionVersion::getIsFinal, Constant.TRUE)
      );
    }

    /**
     * 数据集合
     */
    @Getter
    private static class DataCollection {
        private final List<SalesOrderPool> salesOrders;
        private final List<MdmProductStock> finishedProductStocks;
        private final Map<String, List<MdmProductStock>> finishedProductStockMap;
        private final Map<String, Long> monthSurplusMap;

        public DataCollection(
            List<SalesOrderPool> salesOrders,
            List<MdmProductStock> finishedProductStocks,
            Map<String, List<MdmProductStock>> finishedProductStockMap,
            Map<String, Long> monthSurplusMap) {
            this.salesOrders = CollectionUtils.isNotEmpty(salesOrders)? salesOrders : Collections.emptyList();
            this.finishedProductStocks = finishedProductStocks != null ? finishedProductStocks : Collections.emptyList();
            this.finishedProductStockMap = finishedProductStockMap != null ? finishedProductStockMap : new HashMap<>();
            this.monthSurplusMap = monthSurplusMap != null ? monthSurplusMap : new HashMap<>();
        }
    }

    /**
     * 订单分配结果
     */
    @Getter
    private static class OrderAllocationResult {
        private final List<DpOrderOffsetDetail> allocations;
        private final List<DpOrderOffsetDetail> netDemands;
        private final Map<String, List<MdmProductStock>> stockMap;

        public OrderAllocationResult(
            List<DpOrderOffsetDetail> allocations,
            List<DpOrderOffsetDetail> netDemands,
            Map<String, List<MdmProductStock>> stockMap) {
            this.allocations = allocations != null ? allocations : Collections.emptyList();
            this.netDemands = netDemands != null ? netDemands : Collections.emptyList();
            this.stockMap = stockMap != null ? stockMap : new HashMap<>();
        }
    }

}
