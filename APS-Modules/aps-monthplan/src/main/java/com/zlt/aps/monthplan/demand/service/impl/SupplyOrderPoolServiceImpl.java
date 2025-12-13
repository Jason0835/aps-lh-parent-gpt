package com.zlt.aps.monthplan.demand.service.impl;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.exception.BusinessException;
import com.zlt.aps.maindata.mapper.MdmMaterialInfoEntityMapper;
import com.zlt.aps.maindata.service.IMdmFinishStockService;
import com.zlt.aps.maindata.service.IMdmMonCycleSchStruConfService;
import com.zlt.aps.maindata.service.IMpMonthlySaleQtyService;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;
import com.zlt.aps.monthplan.api.domain.entity.MdmFinishStock;
import com.zlt.aps.monthplan.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmMonCycleSchStruConf;
import com.zlt.aps.monthplan.api.domain.entity.MpMonthlySaleQty;
import com.zlt.aps.monthplan.api.domain.entity.SalesOrderPool;
import com.zlt.aps.monthplan.api.domain.entity.SupplyOrderPool;
import com.zlt.aps.monthplan.demand.mapper.SupplyOrderPoolEntityMapper;
import com.zlt.aps.monthplan.demand.service.IMpOverdueSkuService;
import com.zlt.aps.monthplan.demand.service.ISalesOrderPoolService;
import com.zlt.aps.monthplan.demand.service.ISupplyOrderPoolService;
import com.zlt.aps.monthplan.enums.SupplyOrderTypeEnum;
import com.zlt.aps.monthplan.factory.service.IFactoryMonthPlanProdFinalService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.ruoyi.common.datasource.service.BaseService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
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
 * 文件名称：SupplyOrderPoolServiceImpl.java
 * 描    述：SupplyOrderPoolServiceImpl供应链订单池业务层处理
 *@author zlt
 *@date 2025-12-06
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupplyOrderPoolServiceImpl extends BaseService<SupplyOrderPool>  implements ISupplyOrderPoolService
{

    private final SupplyOrderPoolEntityMapper supplyOrderPoolEntityMapper;

    private final IMdmMonCycleSchStruConfService monCycleSchStruConfService;
    // 物料信息
    private final MdmMaterialInfoEntityMapper mdmMaterialInfoEntityMapper;
    // 超期SKU
    private final  IMpOverdueSkuService overdueSkuService;
    // 月均销量
    private final IMpMonthlySaleQtyService monthlySaleQtyService;
    // 成品库存
    private final IMdmFinishStockService finishStockService;
    // 销售订单
    private final ISalesOrderPoolService salesOrderPoolService;
    // 定稿的月度排产计划
    private final IFactoryMonthPlanProdFinalService factoryMonthPlanProdFinalService;


  /**
     * 查询供应链订单池
     * 
     * @param id 供应链订单池主键
     * @return 供应链订单池
     */
    @Override
    public SupplyOrderPool selectSupplyOrderPoolById(Long id)
    {
        return supplyOrderPoolEntityMapper.selectSupplyOrderPoolById(id);
    }

    /**
     * 查询供应链订单池列表
     * 
     * @param supplyOrderPool 供应链订单池
     * @return 供应链订单池
     */
    @Override
    public List<SupplyOrderPool> selectSupplyOrderPoolList(SupplyOrderPool supplyOrderPool)
    {
        return supplyOrderPoolEntityMapper.selectSupplyOrderPoolList(supplyOrderPool);
    }

    /**
     * 批量查询供应链订单池列表
     *
     * @param ids 需要查询的数据主键集合
     * @return 供应链订单池集合
     */
    @Override
    public List<SupplyOrderPool> selectSupplyOrderPoolByIds(List<Long> ids)
    {
        return super.executeSelectIn(
                    supplyOrderPoolEntityMapper::selectSupplyOrderPoolByIds
                    ,ids
        );
    }


    /**
     * 新增供应链订单池
     * 
     * @param supplyOrderPool 供应链订单池
     * @return 结果
     */
    @Override
    public int insertSupplyOrderPool(SupplyOrderPool supplyOrderPool)
    {
        supplyOrderPool.setBaseVale(null);
        return supplyOrderPoolEntityMapper.insert(supplyOrderPool);
    }

    /**
     * 修改供应链订单池
     * 
     * @param supplyOrderPool 供应链订单池
     * @return 结果
     */
    @Override
    public int updateSupplyOrderPool(SupplyOrderPool supplyOrderPool)
    {
        supplyOrderPool.setBaseVale(supplyOrderPool.getId());
        return supplyOrderPoolEntityMapper.update(supplyOrderPool);
    }

    /**
     * 批量删除供应链订单池
     * 
     * @param ids 需要删除的供应链订单池主键
     * @return 结果
     */
    @Override
    public int deleteSupplyOrderPoolByIds(Long[] ids)
    {
        return supplyOrderPoolEntityMapper.deleteSupplyOrderPoolByIds(ids);
    }

    /**
     * 批量删除供应链订单池
     *
     * @param ids 需要删除的供应链订单池主键
     * @return 结果
     */
    @Override
    public int deleteSupplyOrderPoolByIds(List<Long> ids)
    {
        Long[] arrayids = ids.toArray(new Long[0]);

        return this.deleteSupplyOrderPoolByIds(arrayids);
    }

    /**
     * 删除供应链订单池信息
     * 
     * @param id 供应链订单池主键
     * @return 结果
     */
    @Override
    public int deleteSupplyOrderPoolById(Long id)
    {
        return supplyOrderPoolEntityMapper.deleteSupplyOrderPoolById(id);
    }

    @Override
    public void insertBatchData(Collection<SupplyOrderPool> dataList) {

        this.insertBatchData(dataList, SupplyOrderPoolEntityMapper.class);
    }

    @Override
    public void updateBatchData(Collection<SupplyOrderPool> dataList) {

        this.updateBatchData(dataList, SupplyOrderPoolEntityMapper.class);
    }

    @Override
    public void mergerIntoBatchData(List<SupplyOrderPool> list) {
        this.mergerIntoBatchData(list, SupplyOrderPoolEntityMapper.class);
    }

    /**
     * 校验供应链订单池唯一性
     */
    @Override
    public String checkSupplyOrderPoolUnique(SupplyOrderPool supplyOrderPool) {
        if (supplyOrderPool == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<SupplyOrderPool> list = supplyOrderPoolEntityMapper.selectSupplyOrderPoolList(supplyOrderPool);
        if (CollectionUtils.isNotEmpty(list)) {
            long iCount = list.stream().filter(x->!x.getId().equals(supplyOrderPool.getId())).count();
            return iCount == 0 ? UserConstants.UNIQUE : UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }
    /**
     * 导入供应链订单池数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<SupplyOrderPool> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<SupplyOrderPool> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            SupplyOrderPool supplyOrderPool = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, supplyOrderPool);
            ImportExcelValidatedUtils.validatedRepeat(list,supplyOrderPool,i,2,importLogId,validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                supplyOrderPool.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                supplyOrderPool.setBaseVale(null);
                importList.add(supplyOrderPool);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                    supplyOrderPoolEntityMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    SupplyOrderPool supplyOrderPool = list.get(i);
                    // 错误记录跳过
                    if (supplyOrderPool.getId() != null && supplyOrderPool.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkSupplyOrderPoolUnique(supplyOrderPool);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertSupplyOrderPool(supplyOrderPool);
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
    public void createCycleStockUp(SupplyOrderPool supplyOrderPool) {
        // 1、查询当前周期性排产结构配置，如果没有周期性排产结构配置，则提示"当前没有周期性排产结构配置"；
        List<MdmMonCycleSchStruConf> cycleSchStruConfs = monCycleSchStruConfService.findCurrentCycleSchStruConf();
        if(CollectionUtils.isEmpty(cycleSchStruConfs)){
            throw new BusinessException(I18nUtil.getMessage("ui.message.createCycleStockUp.notExist.cycleProductionStructureConfig"));
        }
        Set<String> structures = cycleSchStruConfs.stream().map(MdmMonCycleSchStruConf::getStructureName).collect(Collectors.toSet());
        if(CollectionUtils.isEmpty(structures)){
            return;
        }
        LambdaQueryWrapper<MdmMaterialInfo> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(MdmMaterialInfo::getIsDelete, YesOrNoEnum.NO.getValue());
        List<MdmMaterialInfo>  materialInfos =   mdmMaterialInfoEntityMapper.selectList(wrapper);
        if(CollectionUtils.isEmpty(materialInfos)){
          return;
        }
        // 3、得到周期性排产结构后，获取结构下的所有SKU
        Set<String> skus = materialInfos.stream().filter(item -> structures.contains(item.getStructureName())).map(MdmMaterialInfo::getMaterialCode).collect(Collectors.toSet());
        if(CollectionUtils.isEmpty(skus)){
          return;
        }
        //   (1)  排除近12个月有周期性排产超期胎的SKU(超期SKU表.超期周期排产 = 1)，剩下的SKU则可生成到供应链订单池-周期排产储备
        //   其中，近12个月指当前月前一个月开始计算，往前12个月
        Set<String> overdueSkus =  overdueSkuService.excludeOverdueCycleProduction();
        if(CollectionUtils.isNotEmpty(overdueSkus)){
            skus = skus.stream().filter(item -> !overdueSkus.contains(item)).collect(Collectors.toSet());
        }
        if(CollectionUtils.isEmpty(skus)){
          return;
        }
        List<MpMonthlySaleQty> monthlySaleQtyList =   monthlySaleQtyService.findCurrentMonthlySaleQty();
        Map<String,MpMonthlySaleQty> sku2AverageSaleQty = sku2AverageSaleQty(monthlySaleQtyList);
        // 剩下的SKU则可生成到供应链订单池-周期排产储备
        // 4、周期性排产结构下可排产的SKU,计算SKU的周期性排产量：月均销量 * 周转月数 - 无订单库存
        List<SupplyOrderPool> supplyOrderPools = Lists.newArrayList();
        Map<String,MdmMaterialInfo> sku2StructureMap  = sku2Structure(materialInfos);
        Map<String, Integer> structure2TurnoverMonthMap = structure2TurnoverMonth(cycleSchStruConfs);
        List<MdmFinishStock> finishStocks = this.finishStockService.findCurrentFinishStock();
        Map<String,List<MdmFinishStock>> finishStockMap = this.getFinishStockMap(finishStocks);
        Map<String, Long> stockMap = this.convertToGroupedSumStockQtyMap(finishStocks);
        List<SalesOrderPool> salesOrderPools = this.salesOrderPoolService.findCurrentSalesOrderPool();
        Map<String, Long> saleOrderMap = this.convertToGroupedSumOrderQtyMap(salesOrderPools);
        List<FactoryMonthPlanProdFinal>  factoryMonthPlanProdFinals = this.factoryMonthPlanProdFinalService.findLastTwelveMonthProdFinalPlan();
        Map<String,Integer> countSkuMap = this.countSkuMap(factoryMonthPlanProdFinals);
        skus.forEach(sku -> supplyOrderPools.add(buildSupplyOrderPool(sku,sku2StructureMap,sku2AverageSaleQty,structure2TurnoverMonthMap,stockMap,saleOrderMap,finishStockMap,countSkuMap)));
        this.insertBatchData(supplyOrderPools);
    }

  private Map<String, Integer> countSkuMap(List<FactoryMonthPlanProdFinal> factoryMonthPlanProdFinals) {
    if (CollectionUtils.isEmpty(factoryMonthPlanProdFinals)) {
      return Collections.emptyMap();
    }

    return factoryMonthPlanProdFinals.stream()
        .filter(Objects::nonNull)
        .filter(item -> item.getProductCode() != null)
        .filter(item -> item.getYearMonth() != null)
        .collect(Collectors.groupingBy(
            FactoryMonthPlanProdFinal::getProductCode,
            Collectors.mapping(
                FactoryMonthPlanProdFinal::getYearMonth,
                Collectors.collectingAndThen(
                    Collectors.toSet(),
                    Set::size
                )
            )
        ));
  }


  private Map<String, Long> convertToGroupedSumOrderQtyMap(List<SalesOrderPool> salesOrderPools) {
    if (CollectionUtils.isEmpty(salesOrderPools)) {
      return Collections.emptyMap();
    }

    return salesOrderPools.stream()
        .filter(Objects::nonNull)
        .filter(salesOrder -> salesOrder.getOriMaterialCode() != null && salesOrder.getWeekYear() != null)
        .collect(Collectors.groupingBy(
            salesOrder -> createCompositeKey(
                salesOrder.getOriMaterialCode(),
                salesOrder.getWeekYear(),
                salesOrder.getDynamicBalance(),
                salesOrder.getUniformity()
            ),
            Collectors.summingLong(item -> item.getOrdQty().longValue())
        ));
  }

  private Map<String,List<MdmFinishStock>> getFinishStockMap(List<MdmFinishStock> finishStocks) {
    if (CollectionUtils.isEmpty(finishStocks)) {
      return Collections.emptyMap();
    }
    return finishStocks.stream()
        .filter(Objects::nonNull)
        .filter(material -> StringUtils.isNotBlank(material.getMaterialCode()))
        .collect(Collectors.groupingBy(MdmFinishStock::getMaterialCode));
  }

  public Map<String, Long> convertToGroupedSumStockQtyMap(List<MdmFinishStock> finishStocks) {
    if (CollectionUtils.isEmpty(finishStocks)) {
      return Collections.emptyMap();
    }

    return finishStocks.stream()
        .filter(Objects::nonNull)
        .filter(stock -> stock.getMaterialCode() != null && stock.getWeekYear() != null)
        .collect(Collectors.groupingBy(
            stock -> createCompositeKey(
                stock.getMaterialCode(),
                stock.getWeekYear(),
                stock.getDynamicBalance(),
                stock.getUniformity()
            ),
            Collectors.summingLong(MdmFinishStock::getStockQty)
        ));
  }

  private String createCompositeKey(String materialCode, String weekYear,
                                    String dynamicBalance, String uniformity) {
    // 使用分隔符连接四个维度，确保唯一性
    // 使用特殊分隔符避免与内容冲突
    return String.join("|",
        Optional.ofNullable(materialCode).orElse(""),
        Optional.ofNullable(weekYear).orElse(""),
        Optional.ofNullable(dynamicBalance).orElse(""),
        Optional.ofNullable(uniformity).orElse("")
    );
  }

  private Map<String, MpMonthlySaleQty> sku2AverageSaleQty(List<MpMonthlySaleQty> monthlySaleQtyList) {
    if (CollectionUtils.isEmpty(monthlySaleQtyList)) {
      return Collections.emptyMap();
    }
    return monthlySaleQtyList.stream()
        .filter(Objects::nonNull)
        .filter(monthlySaleQty -> StringUtils.isNotBlank(monthlySaleQty.getMaterialCode()))
        .collect(Collectors.toMap(
            MpMonthlySaleQty::getMaterialCode,
            monthlySaleQty -> monthlySaleQty,
            (existing, replacement) -> existing
        ));
  }

  private SupplyOrderPool buildSupplyOrderPool(String materialCode,Map<String,MdmMaterialInfo> sku2StructureMap,Map<String,MpMonthlySaleQty> sku2AverageSaleQty,Map<String, Integer> structure2TurnoverMonthMap,Map<String, Long> stockMap,Map<String, Long> saleOrderMap,Map<String,List<MdmFinishStock>> finishStockMap,Map<String,Integer> countSkuMap) {
         SupplyOrderPool entity = new SupplyOrderPool();
         MdmMaterialInfo materialInfo = sku2StructureMap.get(materialCode);
         String structureName = materialInfo.getStructureName();
         int turnoverMonth = structure2TurnoverMonthMap.getOrDefault(structureName,0);
         // SKU的周期性排产量：月均销量 * 周转月数 - 无订单库存
         entity.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
         entity.setOrderType(SupplyOrderTypeEnum.CYCLE_PRODUCTION_STOCK.getCode());
         entity.setMaterialCode(materialCode);
         entity.setBrand(materialInfo.getBrand());
         // 获取当前年月
         YearMonth nextYearMonth = YearMonth.now().plusMonths(1);
         entity.setYear(nextYearMonth.getYear());
         entity.setMonth(nextYearMonth.getMonthValue());
         String yearMonth = String.format("%s%02d", entity.getYear(), entity.getMonth());
         entity.setYearMonth(Integer.valueOf(yearMonth));
         entity.setLocationType(materialInfo.getCommonType());
         entity.setMaterialDesc(materialInfo.getMaterialDesc());
         entity.setMesMaterialCode(materialInfo.getMesMaterialCode());
         entity.setProductCategory(materialInfo.getProductCategory());
         entity.setProductTypeCode(materialInfo.getProductTypeCode());
         long averageSaleQty = 0;
         Long passThreeMonthSaleQty = null;
         Long passSixMonthSaleQty = null;
         Integer deliveryFrequency = null;
         String saleArea = null;
         if(sku2AverageSaleQty.containsKey(materialCode)) {
           MpMonthlySaleQty monthlySaleQty = sku2AverageSaleQty.get(materialCode);
           averageSaleQty = monthlySaleQty.getAverageSaleQty();
           passThreeMonthSaleQty = monthlySaleQty.getPassThreeMonthSaleQty();
           passSixMonthSaleQty = monthlySaleQty.getPassSixMonthSaleQty();
           deliveryFrequency = monthlySaleQty.getDeliveryFrequency();
           saleArea = monthlySaleQty.getSaleArea();
         }
         long notOrderStockQty = getNotOrderStockQty(materialCode,stockMap,saleOrderMap);
         // SKU的周期性排产量：月均销量 * 周转月数 - 无订单库存
         // 无订单库存 = 成品库存 - 销售订单池提报量(需结合年周号、动平衡、均匀性)
         entity.setQty(averageSaleQty * turnoverMonth - notOrderStockQty);
         entity.setBaseVale(null);
         entity.setIsDelete(YesOrNoEnum.NO.getValue());
         // 计算备库上限：备库上限/月均销量 * 30 = 30（天）注：第1个30，月度天数（固定）；第2个30，周转天数（可配置）；月均销量（6个月）。
         // 备库上限值 = 周转天数(全局参数) * 月均销量 / 30
        entity.setStockLimit(averageSaleQty * turnoverMonth);
        long threeOverdueStockQty = 0;
        long sixOverdueStockQty = 0;
        long nightOverdueStockQty = 0;
        long twelveOverdueStockQty = 0;
        // 6、查询成品库存表，汇总计算超期12个月的库存数、超期6个月的库存数、超期3个月的库存数
        if(finishStockMap.containsKey(materialCode)) {
            List<MdmFinishStock> finishStockList = finishStockMap.get(materialCode);
            threeOverdueStockQty = finishStockList.stream().filter(item -> YesOrNoEnum.YES.getCode().equals(item.getIsExceedThreeMonth())).mapToLong(MdmFinishStock::getStockQty).sum();
            sixOverdueStockQty = finishStockList.stream().filter(item -> YesOrNoEnum.YES.getCode().equals(item.getIsExceedSixMonth())).mapToLong(MdmFinishStock::getStockQty).sum();
            nightOverdueStockQty = finishStockList.stream().filter(item -> YesOrNoEnum.YES.getCode().equals(item.getIsExceedNineMonth())).mapToLong(MdmFinishStock::getStockQty).sum();
            twelveOverdueStockQty  = finishStockList.stream().filter(item -> YesOrNoEnum.YES.getCode().equals(item.getIsExceedTwelveMonth())).mapToLong(MdmFinishStock::getStockQty).sum();
        }
        entity.setThreeOverdueStockQty(threeOverdueStockQty);
        entity.setSixOverdueStockQty(sixOverdueStockQty);
        entity.setNightOverdueStockQty(nightOverdueStockQty);
        entity.setTwelveOverdueStockQty(twelveOverdueStockQty);
        // 7、从月均销量表中取得近3个月月均销量、近6个月月均销量、近12个月的发货频次、适销区域
        entity.setThreeAverageQty(passThreeMonthSaleQty);
        entity.setSixAverageQty(passSixMonthSaleQty);
        entity.setDeliveryFrequency(deliveryFrequency);
        entity.setSaleArea(saleArea);
        // 8、12个月结构上机频次 = 从定稿的月度排产计划，获取近12个月的已排产的月份个数
        entity.setStructureFrequency(countSkuMap.getOrDefault(materialCode,0));
        return entity;
  }

  private long getNotOrderStockQty(String materialCode, Map<String, Long> stockMap, Map<String, Long> saleOrderMap) {
    long notOrderStockQty = 0;
    if(org.springframework.util.CollectionUtils.isEmpty(stockMap)) {
      return notOrderStockQty;
    }
    for(Map.Entry<String,Long> entry : stockMap.entrySet()) {
       if(!entry.getKey().contains(materialCode)) {
          continue;
       }
      notOrderStockQty += entry.getValue() - saleOrderMap.getOrDefault(entry.getKey(),0L);
    }
    return notOrderStockQty;
  }

  public Map<String, MdmMaterialInfo> sku2Structure(List<MdmMaterialInfo> materialList) {
    if (CollectionUtils.isEmpty(materialList)) {
      return Collections.emptyMap();
    }
    return materialList.stream()
        .filter(Objects::nonNull)
        .filter(material -> StringUtils.isNotBlank(material.getMaterialCode()))
        .collect(Collectors.toMap(MdmMaterialInfo::getMaterialCode,
            material -> material,
            (existing, replacement) -> existing
        ));
  }

  public Map<String, Integer> structure2TurnoverMonth(List<MdmMonCycleSchStruConf> cycleSchStruConfs) {
    if (CollectionUtils.isEmpty(cycleSchStruConfs)) {
      return Collections.emptyMap();
    }
    return cycleSchStruConfs.stream()
        .filter(Objects::nonNull)
        .filter(cycleSchStruConf -> StringUtils.isNotBlank(cycleSchStruConf.getStructureName()))
        .collect(Collectors.toMap(
            MdmMonCycleSchStruConf::getStructureName,
            MdmMonCycleSchStruConf::getTurnoverMonth,
            (existing, replacement) -> existing
        ));
  }
}
