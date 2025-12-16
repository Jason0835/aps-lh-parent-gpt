package com.zlt.aps.monthplan.demand.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tlt.aps.constant.Constant;
import com.tlt.aps.exception.BusinessException;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.maindata.service.IMdmMonthSurplusService;
import com.zlt.aps.maindata.service.IMpFinishedProductStockService;
import com.zlt.aps.monthplan.api.domain.entity.FactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.entity.MpDemandPlan;
import com.zlt.aps.monthplan.api.domain.entity.MpFinishedProductStock;
import com.zlt.aps.monthplan.api.domain.entity.MpOrderOffsetAllocation;
import com.zlt.aps.monthplan.api.domain.entity.SalesOrderPool;
import com.zlt.aps.monthplan.common.utils.RequirementVersionService;
import com.zlt.aps.monthplan.demand.mapper.MpDemandPlanEntityMapper;
import com.zlt.aps.monthplan.demand.service.IMpDemandPlanService;
import com.zlt.aps.monthplan.demand.service.ISalesOrderPoolService;
import com.zlt.aps.monthplan.factory.helper.SaleRequirePlanHelper;
import com.zlt.aps.monthplan.factory.helper.StockAllocationHelper;
import com.zlt.aps.monthplan.factory.mapper.FactoryProductionVersionMapper;
import com.zlt.aps.monthplan.factory.service.IMpMonthPlanProdFinalService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.ruoyi.common.datasource.service.BaseService;
import lombok.RequiredArgsConstructor;
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
        // 2、查询获取销售订单池中的所有订单
        List<SalesOrderPool> salesOrders = this.salesOrderPoolService.findCurrentSalesOrderPool();
        // 3、查询获取所有成品库存
        List<MpFinishedProductStock> finishedProductStocks = this.finishedProductStockService.findCurrentFinishStock();
        Map<String,List<MpFinishedProductStock>> finishedProductStockMap = this.getFinishedProductStockMap(finishedProductStocks);
        // 计算月底计划余量 查询获取所有成品库存；同时计算月底计划余量：库存抓取日~（同月）月底的月度计划量汇总
        Map<String,Long> mdmMonthSurplusMap =  mpMonthPlanProdFinalService.calculateMonthSurplus(monthPlanVersion);
        // 4、从获取的销售订单池数据中筛选高、中优先级列表(供应链优先级 !=暂缓订单(5))
        if(CollectionUtils.isNotEmpty(salesOrders)){
            salesOrders = salesOrders
                .stream()
                .filter(item -> ApsConstant.SAL_PRIORITY_HIGHT.equals(item.getOrderPriority())
                    || ApsConstant.SAL_PRIORITY_MID.equals(item.getOrderPriority()))
                .collect(Collectors.toList());
        }
        Map<String, List<SalesOrderPool>> saleOrderGroupMap = SaleRequirePlanHelper.getGroupSalesOrder(salesOrders);
        //按照库存冲销顺序进行对冲
        List<MpOrderOffsetAllocation> orderOffsetAllocations = StockAllocationHelper.calculateStockAllocation(monthPlanVersion,saleOrderGroupMap,finishedProductStockMap,mdmMonthSurplusMap);
        // 7、将分配冲减后的结果记录到订单分配表中(以需求版本号的维度)；


    }

    private Map<String,List<MpFinishedProductStock>> getFinishedProductStockMap(List<MpFinishedProductStock> finishedProductStocks) {
        if (CollectionUtils.isEmpty(finishedProductStocks)) {
            return Collections.emptyMap();
        }
      return finishedProductStocks
          .parallelStream()
          .filter(Objects::nonNull)
          .filter(finishedProductStock -> finishedProductStock.getGroupKey() != null)
          .collect(Collectors.groupingByConcurrent(
              MpFinishedProductStock::getGroupKey,
              Collectors.toCollection(ArrayList::new)
          ));
    }


}
