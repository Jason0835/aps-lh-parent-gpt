package com.zlt.aps.monthplan.demand.service.impl;

import java.math.BigDecimal;
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
import com.google.common.collect.Sets;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.enums.ProductTypeEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.exception.BusinessException;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.mapper.MdmMaterialInfoEntityMapper;
import com.zlt.aps.maindata.service.IFactoryParamService;
import com.zlt.aps.maindata.service.IMdmMonCycleSchStruConfService;
import com.zlt.aps.maindata.service.IMpFinishedProductStockService;
import com.zlt.aps.maindata.service.IMpHistorySaleRecordService;
import com.zlt.aps.maindata.service.IMpMonthlySaleQtyService;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;
import com.zlt.aps.monthplan.api.domain.entity.FactoryParam;
import com.zlt.aps.monthplan.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmMonCycleSchStruConf;
import com.zlt.aps.monthplan.api.domain.entity.MpFinishedProductStock;
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
    private final IMpFinishedProductStockService finishedProductStockService;
    // 销售订单
    private final ISalesOrderPoolService salesOrderPoolService;
    // 定稿的月度排产计划
    private final IFactoryMonthPlanProdFinalService factoryMonthPlanProdFinalService;
    // 历史销售记录
    private final IMpHistorySaleRecordService historySaleRecordService;
    // 排产设定
    private final IFactoryParamService iFactoryParamService;


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
          throw new BusinessException(I18nUtil.getMessage("ui.message.createCycleStockUp.notExist.cycleProductionStructureConfig"));
        }
        LambdaQueryWrapper<MdmMaterialInfo> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(MdmMaterialInfo::getIsDelete, YesOrNoEnum.NO.getValue());
        List<MdmMaterialInfo>  materialInfos =   mdmMaterialInfoEntityMapper.selectList(wrapper);
        if(CollectionUtils.isEmpty(materialInfos)){
          throw new BusinessException(I18nUtil.getMessage("ui.message.createCycleStockUp.notExist.cycleStockUpMaterial"));
        }
        // 3、得到周期性排产结构后，获取结构下的所有SKU
        Set<String> skus = materialInfos.stream().filter(item -> structures.contains(item.getStructureName())).map(MdmMaterialInfo::getMaterialCode).collect(Collectors.toSet());
        if(CollectionUtils.isEmpty(skus)){
          throw new BusinessException(I18nUtil.getMessage("ui.message.createCycleStockUp.notExist.cycleStockUpMaterial"));
        }
        //   (1)  排除近12个月有周期性排产超期胎的SKU(超期SKU表.超期周期排产 = 1)，剩下的SKU则可生成到供应链订单池-周期排产储备
        //   其中，近12个月指当前月前一个月开始计算，往前12个月
        Set<String> overdueSkus =  overdueSkuService.excludeOverdueCycleProduction();
        if(CollectionUtils.isNotEmpty(overdueSkus)){
            skus = skus.stream().filter(item -> !overdueSkus.contains(item)).collect(Collectors.toSet());
        }
        if(CollectionUtils.isEmpty(skus)){
          throw new BusinessException(I18nUtil.getMessage("ui.message.createCycleStockUp.notExist.cycleStockUpMaterial"));
        }
        this.deleteSupplyOrderPool(SupplyOrderTypeEnum.CYCLE_PRODUCTION_STOCK.getCode());
        List<MpMonthlySaleQty> monthlySaleQtyList =   monthlySaleQtyService.findCurrentMonthlySaleQty();
        Map<String,MpMonthlySaleQty> sku2AverageSaleQty = sku2AverageSaleQty(monthlySaleQtyList);
        // 剩下的SKU则可生成到供应链订单池-周期排产储备
        // 4、周期性排产结构下可排产的SKU,计算SKU的周期性排产量：月均销量 * 周转月数 - 无订单库存
        List<SupplyOrderPool> supplyOrderPools = Lists.newArrayList();
        Map<String,MdmMaterialInfo> sku2StructureMap  = sku2Structure(materialInfos);
        Map<String, Integer> structure2TurnoverMonthMap = structure2TurnoverMonth(cycleSchStruConfs);
        List<MpFinishedProductStock> finishedProductStocks = this.finishedProductStockService.findCurrentFinishStock();
        Map<String,List<MpFinishedProductStock>> finishedProductStockMap = this.getFinishedProductMap(finishedProductStocks);
        Map<String, Long> stockMap = this.convertToGroupedSumStockQtyMap(finishedProductStocks);
        List<SalesOrderPool> salesOrderPools = this.salesOrderPoolService.findCurrentSalesOrderPool();
        Map<String, Long> saleOrderMap = this.convertToGroupedSumOrderQtyMap(salesOrderPools);
        List<FactoryMonthPlanProdFinal> factoryMonthPlanProdFinals = this.factoryMonthPlanProdFinalService.findLastTwelveMonthProdFinalPlan();
        Map<String,Integer> countSkuMap = this.countSkuMap(factoryMonthPlanProdFinals);
        skus.forEach(sku -> supplyOrderPools.add(buildSupplyOrderPool(sku,sku2StructureMap,sku2AverageSaleQty,structure2TurnoverMonthMap,stockMap,saleOrderMap,finishedProductStockMap,countSkuMap)));
        this.insertBatchData(supplyOrderPools);
    }

  @Override
  public void createPrecedentStockUp(SupplyOrderPool supplyOrderPool) {
        // 1、检查储备排产是否在生成
        // 2、获取可常规储备的“SKU列表”
        //  (1) 获取所有不在周期排产结构配置表中的结构下的“SKU列表1”
        //  (2)查询近12个月的月均销量大于零的月份数 > 8 的“SKU列表2”(其中，近12个月指当前月前一个月开始计算，往前12个月)
        //  (3)SKU列表 = “SKU列表1”与“SKU列表2”取交集
        LambdaQueryWrapper<MdmMaterialInfo> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(MdmMaterialInfo::getIsDelete, YesOrNoEnum.NO.getValue());
        List<MdmMaterialInfo>  materialInfos =   mdmMaterialInfoEntityMapper.selectList(wrapper);
        if(CollectionUtils.isEmpty(materialInfos)){
          throw new BusinessException(I18nUtil.getMessage("ui.message.createPrecedentStockUp.notExist.precedentStockUpMaterial"));
        }
        List<MdmMonCycleSchStruConf> cycleSchStruConfs = monCycleSchStruConfService.findCurrentCycleSchStruConf();
        Set<String> structures = Sets.newHashSet();
        if(CollectionUtils.isNotEmpty(cycleSchStruConfs)){
          structures = cycleSchStruConfs.stream().map(MdmMonCycleSchStruConf::getStructureName).collect(Collectors.toSet());
        }
        final Set<String> finalStructures = structures;
        // (1) 获取所有不在周期排产结构配置表中的结构下的“SKU列表1”
        Set<String> skus = materialInfos.stream().filter(item -> !finalStructures.contains(item.getStructureName())).map(MdmMaterialInfo::getMaterialCode).collect(Collectors.toSet());
        if(CollectionUtils.isEmpty(skus)){
          throw new BusinessException(I18nUtil.getMessage("ui.message.createPrecedentStockUp.notExist.precedentStockUpMaterial"));
        }
        Set<String> skusByAverageSaleQty = historySaleRecordService.findSkuInLastTwelveMonth();
        if(CollectionUtils.isEmpty(skusByAverageSaleQty)){
          throw new BusinessException(I18nUtil.getMessage("ui.message.createPrecedentStockUp.notExist.precedentStockUpMaterial"));
        }
        // (3)SKU列表 = “SKU列表1”与“SKU列表2”取交集
        Set<String> intersections  =  skus.stream()
              .filter(skusByAverageSaleQty::contains)
              .collect(Collectors.toSet());
        // (4) 如果没有获取到SKU列表，则提示“当前没有常规储备物料"，系统不作处理
        if(CollectionUtils.isEmpty(intersections)){
          throw new BusinessException(I18nUtil.getMessage("ui.message.createPrecedentStockUp.notExist.precedentStockUpMaterial"));
        }
        // 3、从步骤2中的SKU列表中排除近12个月有常规储备超期胎的SKU(超期SKU表.超期储备排产 = 1)，剩下的SKU则可生成到供应链订单池-常规储备排产
        Set<String> overdueSkus =  overdueSkuService.excludeOverduePrecedentProduction();
        if(CollectionUtils.isNotEmpty(overdueSkus)){
          intersections = intersections.stream().filter(item -> !overdueSkus.contains(item)).collect(Collectors.toSet());
        }
        if(CollectionUtils.isEmpty(intersections)){
          throw new BusinessException(I18nUtil.getMessage("ui.message.createPrecedentStockUp.notExist.precedentStockUpMaterial"));
        }
        this.deleteSupplyOrderPool(SupplyOrderTypeEnum.PRECEDENT_STOCK.getCode());
        // 4、计算常规储备SKU的排产量： (周转天数/30) * 月均销量 - 无订单库存 无订单库存 = 成品库存 - 销售订单池提报量(需结合年周号、动平衡、均匀性)；
        List<MpMonthlySaleQty> monthlySaleQtyList =   monthlySaleQtyService.findCurrentMonthlySaleQty();
        Map<String,MpMonthlySaleQty> sku2AverageSaleQty = sku2AverageSaleQty(monthlySaleQtyList);
        // 剩下的SKU则可生成到供应链订单池-周期排产储备
        // 4、周期性排产结构下可排产的SKU,计算SKU的周期性排产量：月均销量 * 周转月数 - 无订单库存
        List<SupplyOrderPool> supplyOrderPools = Lists.newArrayList();
        Map<String,MdmMaterialInfo> sku2StructureMap  = sku2Structure(materialInfos);
        Map<String, Integer> structure2TurnoverMonthMap = structure2TurnoverMonth(cycleSchStruConfs);
        List<MpFinishedProductStock> finishedProductStocks = this.finishedProductStockService.findCurrentFinishStock();
        Map<String,List<MpFinishedProductStock>> finishedProductStockMap  = this.getFinishedProductMap(finishedProductStocks);
        Map<String, Long> stockMap = this.convertToGroupedSumStockQtyMap(finishedProductStocks);
        List<SalesOrderPool> salesOrderPools = this.salesOrderPoolService.findCurrentSalesOrderPool();
        Map<String, Long> saleOrderMap = this.convertToGroupedSumOrderQtyMap(salesOrderPools);
        List<FactoryMonthPlanProdFinal> factoryMonthPlanProdFinals = this.factoryMonthPlanProdFinalService.findLastTwelveMonthProdFinalPlan();
        Map<String,Integer> countSkuMap = this.countSkuMap(factoryMonthPlanProdFinals);
        intersections.forEach(sku -> supplyOrderPools.add(buildPrecedentOrder(sku,sku2StructureMap,sku2AverageSaleQty,structure2TurnoverMonthMap,stockMap,saleOrderMap,finishedProductStockMap,countSkuMap)));
        this.insertBatchData(supplyOrderPools);
  }

  @Override
  public SupplyOrderPool queryRelationByMaterialCode(SupplyOrderPool supplyOrderPool) {
    LambdaQueryWrapper<MdmMaterialInfo> wrapper = Wrappers.lambdaQuery();
    wrapper.eq(MdmMaterialInfo::getMaterialCode, supplyOrderPool.getMaterialCode());
    wrapper.eq(MdmMaterialInfo::getIsDelete, YesOrNoEnum.NO.getValue());
    List<MdmMaterialInfo>  materialInfos =   mdmMaterialInfoEntityMapper.selectList(wrapper);
    if(CollectionUtils.isEmpty(materialInfos)){
      throw new BusinessException(I18nUtil.getMessage("ui.message.supplyOrderPool.notFound.materialInfo"));
    }
    // (1)通过物料表，带出物料描述、品牌、产品品类
    MdmMaterialInfo materialInfo = materialInfos.get(0);
    // 获取当前年月 2、工厂：默认116；年-月：当前系统日所在年月；内外销：默认外销；
    supplyOrderPool.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
    YearMonth now = YearMonth.now();
    supplyOrderPool.setYear(now.getYear());
    supplyOrderPool.setMonth(now.getMonthValue());
    String yearMonth = String.format("%s%02d", supplyOrderPool.getYear(), supplyOrderPool.getMonth());
    supplyOrderPool.setYearMonth(Integer.valueOf(yearMonth));
    supplyOrderPool.setLocationType(materialInfo.getCommonType());
    supplyOrderPool.setMaterialDesc(materialInfo.getMaterialDesc());
    supplyOrderPool.setBrand(materialInfo.getBrand());
    supplyOrderPool.setProductTypeCode(materialInfo.getProductTypeCode());
    supplyOrderPool.setMesMaterialCode(materialInfo.getMesMaterialCode());
    supplyOrderPool.setProductCategory(materialInfo.getProductCategory());
    // (2)通过月均销量表，带出近3个月月均销量、近6个月月均销量、近12个月的发货频次、适销区域、备库上限/月均销量 * 30 = 30（天）
    MpMonthlySaleQty monthlySaleQty =   monthlySaleQtyService.getMpMonthlySaleQtyByMaterialCode(supplyOrderPool.getMaterialCode());
    if(null != monthlySaleQty) {
      supplyOrderPool.setThreeAverageQty(monthlySaleQty.getPassThreeMonthSaleQty());
      supplyOrderPool.setSixAverageQty(monthlySaleQty.getPassSixMonthSaleQty());
      supplyOrderPool.setDeliveryFrequency(monthlySaleQty.getDeliveryFrequency());
      supplyOrderPool.setSaleArea(monthlySaleQty.getSaleArea());
      supplyOrderPool.setAverageSaleQty(monthlySaleQty.getAverageSaleQty());
      // 周转天数
      String turnOverDaysStr = this.getTurnOverDays(supplyOrderPool.getFactoryCode(), ProductTypeEnum.WHOLE_STEEL.getValue());
      // 备库上限值 = 周转天数(全局参数) * 月均销量 / 30
      BigDecimal turnOverDays = BigDecimalUtils.valueOf(turnOverDaysStr);
      long stockLimit = BigDecimalUtils.multiply(turnOverDays,BigDecimal.valueOf(monthlySaleQty.getAverageSaleQty()))
          .divideToIntegralValue(BigDecimal.valueOf(30)).longValue();
      supplyOrderPool.setStockLimit(stockLimit);
    }
    //   (3)通过成品库存表，获取超期12个月的库存数、超期6个月的库存数、超期3个月的库存数
    List<MpFinishedProductStock> finishedProductStocks = this.finishedProductStockService.getMpFinishedProductStockByMaterialCode(supplyOrderPool.getMaterialCode());
    if(CollectionUtils.isNotEmpty(finishedProductStocks)) {
      long threeOverdueStockQty = finishedProductStocks.stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedThreeMonth())).mapToLong(MpFinishedProductStock::getStockQty).sum();
      long sixOverdueStockQty = finishedProductStocks.stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedSixMonth())).mapToLong(MpFinishedProductStock::getStockQty).sum();
      long nightOverdueStockQty = finishedProductStocks.stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedNineMonth())).mapToLong(MpFinishedProductStock::getStockQty).sum();
      long twelveOverdueStockQty  = finishedProductStocks.stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedTwelveMonth())).mapToLong(MpFinishedProductStock::getStockQty).sum();
      supplyOrderPool.setThreeOverdueStockQty(threeOverdueStockQty);
      supplyOrderPool.setSixOverdueStockQty(sixOverdueStockQty);
      supplyOrderPool.setNightOverdueStockQty(nightOverdueStockQty);
      supplyOrderPool.setTwelveOverdueStockQty(twelveOverdueStockQty);
    }
    //通过月度生产计划表，获取近12个月有排产的月份个数
    // 8、12个月结构上机频次 = 从定稿的月度排产计划，获取近12个月的已排产的月份个数
    int  productionMonth = factoryMonthPlanProdFinalService.getProductionMonthInLastTwelveMonth(supplyOrderPool.getMaterialCode());
    supplyOrderPool.setStructureFrequency(productionMonth);
    return supplyOrderPool;
  }

  @Override
  public List<SupplyOrderPool> findCurrentSupplyOrderPool() {
    SupplyOrderPool param = new SupplyOrderPool();
    YearMonth yearMonth = YearMonth.now();
    param.setYear(yearMonth.getYear());
    param.setMonth(yearMonth.getMonthValue());
    param.setIsDelete(YesOrNoEnum.NO.getValue());
    return this.selectSupplyOrderPoolList(param);
  }

  /**
   * 获取配置信息
   *
   * @return
   */
  private String getTurnOverDays(String factoryCode, String productTypeCode) {
    FactoryParam factoryParam = new FactoryParam();
    factoryParam.setFactoryCode(factoryCode);
    factoryParam.setParamCode(MonthPlanEnums.TURN_OVER_DAYS.getCode());
    factoryParam.setProductTypeCode(productTypeCode);
    FactoryParam param = iFactoryParamService.getFacParamSingle(factoryParam);
    String paramValue = null;
    if (param != null) {
      paramValue = StringUtils.isNotEmpty(param.getParamValue()) ? param.getParamValue()
          : param.getDefauleValue();
    }
    return paramValue;
  }

  /**
   *  删除
   */
  private void deleteSupplyOrderPool(String orderType) {
    SupplyOrderPool param = new SupplyOrderPool();
    param.setOrderType(orderType);
    // 获取当前年月
    YearMonth nextYearMonth = YearMonth.now().plusMonths(1);
    param.setYear(nextYearMonth.getYear());
    param.setMonth(nextYearMonth.getMonthValue());
    List<SupplyOrderPool>  deleteList =  this.selectSupplyOrderPoolList(param);
    if(CollectionUtils.isEmpty(deleteList)){
      return;
    }
    List<Long> list = deleteList.stream().map(SupplyOrderPool::getId).collect(Collectors.toList());
    this.deleteSupplyOrderPoolByIds(list);
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

  private Map<String,List<MpFinishedProductStock>> getFinishedProductMap(List<MpFinishedProductStock> finishedProductStocks) {
    if (CollectionUtils.isEmpty(finishedProductStocks)) {
      return Collections.emptyMap();
    }
    return finishedProductStocks.stream()
        .filter(Objects::nonNull)
        .filter(monthlySaleQty -> StringUtils.isNotBlank(monthlySaleQty.getMaterialCode()))
        .collect(Collectors.groupingBy(MpFinishedProductStock::getMaterialCode));
  }

  public Map<String, Long> convertToGroupedSumStockQtyMap(List<MpFinishedProductStock> finishedProductStocks) {
    if (CollectionUtils.isEmpty(finishedProductStocks)) {
      return Collections.emptyMap();
    }

    return finishedProductStocks.stream()
        .filter(Objects::nonNull)
        .filter(stock -> stock.getMaterialCode() != null && stock.getWeekYear() != null)
        .collect(Collectors.groupingBy(
            stock -> createCompositeKey(
                stock.getMaterialCode(),
                stock.getWeekYear(),
                stock.getDynamicBalance(),
                stock.getUniformity()
            ),
            Collectors.summingLong(MpFinishedProductStock::getStockQty)
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

  private SupplyOrderPool buildSupplyOrderPool(String materialCode,Map<String,MdmMaterialInfo> sku2StructureMap,Map<String,MpMonthlySaleQty> sku2AverageSaleQty,Map<String, Integer> structure2TurnoverMonthMap,Map<String, Long> stockMap,Map<String, Long> saleOrderMap,Map<String,List<MpFinishedProductStock>> finishedProductStockMap,Map<String,Integer> countSkuMap) {
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
           // 周转天数
           String turnOverDaysStr = this.getTurnOverDays(entity.getFactoryCode(), ProductTypeEnum.WHOLE_STEEL.getValue());
           // 备库上限值 = 周转天数(全局参数) * 月均销量 / 30
           BigDecimal turnOverDays = BigDecimalUtils.valueOf(turnOverDaysStr);
           long stockLimit = BigDecimalUtils.multiply(turnOverDays,BigDecimal.valueOf(monthlySaleQty.getAverageSaleQty()))
               .divideToIntegralValue(BigDecimal.valueOf(30)).longValue();
           entity.setStockLimit(stockLimit);
         }
         long notOrderStockQty = getNotOrderStockQty(materialCode,stockMap,saleOrderMap);
         // SKU的周期性排产量：月均销量 * 周转月数 - 无订单库存
         // 无订单库存 = 成品库存 - 销售订单池提报量(需结合年周号、动平衡、均匀性)
         entity.setQty(averageSaleQty * turnoverMonth - notOrderStockQty);
         entity.setBaseVale(null);
         entity.setIsDelete(YesOrNoEnum.NO.getValue());
        long threeOverdueStockQty = 0;
        long sixOverdueStockQty = 0;
        long nightOverdueStockQty = 0;
        long twelveOverdueStockQty = 0;
        // 6、查询成品库存表，汇总计算超期12个月的库存数、超期6个月的库存数、超期3个月的库存数
        if(finishedProductStockMap.containsKey(materialCode)) {
            List<MpFinishedProductStock> finishedProductStocks = finishedProductStockMap.get(materialCode);
            threeOverdueStockQty = finishedProductStocks.stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedThreeMonth())).mapToLong(MpFinishedProductStock::getStockQty).sum();
            sixOverdueStockQty = finishedProductStocks.stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedSixMonth())).mapToLong(MpFinishedProductStock::getStockQty).sum();
            nightOverdueStockQty = finishedProductStocks.stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedNineMonth())).mapToLong(MpFinishedProductStock::getStockQty).sum();
            twelveOverdueStockQty  = finishedProductStocks.stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedTwelveMonth())).mapToLong(MpFinishedProductStock::getStockQty).sum();
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

  private SupplyOrderPool buildPrecedentOrder(String materialCode, Map<String, MdmMaterialInfo> sku2StructureMap, Map<String, MpMonthlySaleQty> sku2AverageSaleQty, Map<String, Integer> structure2TurnoverMonthMap, Map<String, Long> stockMap, Map<String, Long> saleOrderMap,Map<String,List<MpFinishedProductStock>> finishedProductStockMap, Map<String, Integer> countSkuMap) {
    SupplyOrderPool entity = new SupplyOrderPool();
    MdmMaterialInfo materialInfo = sku2StructureMap.get(materialCode);
    String structureName = materialInfo.getStructureName();
    int turnoverMonth = structure2TurnoverMonthMap.getOrDefault(structureName,0);
    // SKU的周期性排产量：月均销量 * 周转月数 - 无订单库存
    entity.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
    entity.setOrderType(SupplyOrderTypeEnum.PRECEDENT_STOCK.getCode());
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
    // 7、从月均销量表中取得近3个月月均销量、近6个月月均销量、近12个月的发货频次、适销区域
    if(sku2AverageSaleQty.containsKey(materialCode)) {
      MpMonthlySaleQty monthlySaleQty = sku2AverageSaleQty.get(materialCode);
      averageSaleQty = monthlySaleQty.getAverageSaleQty();
      passThreeMonthSaleQty = monthlySaleQty.getPassThreeMonthSaleQty();
      passSixMonthSaleQty = monthlySaleQty.getPassSixMonthSaleQty();
      deliveryFrequency = monthlySaleQty.getDeliveryFrequency();
      saleArea = monthlySaleQty.getSaleArea();
      // 周转天数
      String turnOverDaysStr = this.getTurnOverDays(entity.getFactoryCode(), ProductTypeEnum.WHOLE_STEEL.getValue());
      // 备库上限值 = 周转天数(全局参数) * 月均销量 / 30
      BigDecimal turnOverDays = BigDecimalUtils.valueOf(turnOverDaysStr);
      long stockLimit = BigDecimalUtils.multiply(turnOverDays,BigDecimal.valueOf(monthlySaleQty.getAverageSaleQty()))
          .divideToIntegralValue(BigDecimal.valueOf(30)).longValue();
      entity.setStockLimit(stockLimit);
    }
    long notOrderStockQty = getNotOrderStockQty(materialCode,stockMap,saleOrderMap);
    // 排产量 = (周转天数/30) * 月均销量 - 无订单库存
    // 无订单库存 = 成品库存 - 销售订单池提报量(需结合年周号、动平衡、均匀性)
    entity.setQty(averageSaleQty * turnoverMonth - notOrderStockQty);
    entity.setBaseVale(null);
    entity.setIsDelete(YesOrNoEnum.NO.getValue());
    long threeOverdueStockQty = 0;
    long sixOverdueStockQty = 0;
    long nightOverdueStockQty = 0;
    long twelveOverdueStockQty = 0;
    // 6、查询成品库存表，汇总计算超期12个月的库存数、超期6个月的库存数、超期3个月的库存数
    if(finishedProductStockMap.containsKey(materialCode)) {
      List<MpFinishedProductStock> finishedProductStocks = finishedProductStockMap.get(materialCode);
      threeOverdueStockQty = finishedProductStocks.stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedThreeMonth())).mapToLong(MpFinishedProductStock::getStockQty).sum();
      sixOverdueStockQty = finishedProductStocks.stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedSixMonth())).mapToLong(MpFinishedProductStock::getStockQty).sum();
      nightOverdueStockQty = finishedProductStocks.stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedNineMonth())).mapToLong(MpFinishedProductStock::getStockQty).sum();
      twelveOverdueStockQty  = finishedProductStocks.stream().filter(finishedProductStock -> YesOrNoEnum.YES.getCode().equals(finishedProductStock.getIsExceedTwelveMonth())).mapToLong(MpFinishedProductStock::getStockQty).sum();
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
    // 8、12个月结构上机频次 = 从月度排产计划，获取近12个月的已排产的月份个数
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
