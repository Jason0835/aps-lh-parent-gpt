package com.zlt.aps.monthplan.demand.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;
import com.zlt.aps.monthplan.api.domain.entity.DpOrderPoolSnapshot;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import com.zlt.aps.monthplan.api.domain.entity.SalesOrderPool;
import com.zlt.aps.monthplan.api.domain.entity.SupplyOrderPool;
import com.zlt.aps.monthplan.demand.mapper.DpOrderPoolSnapshotEntityMapper;
import com.zlt.aps.monthplan.demand.service.IDpOrderPoolSnapshotService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.ruoyi.common.datasource.service.BaseService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
 * 文件名称：DpOrderPoolSnapshotServiceImpl.java
 * 描    述：DpOrderPoolSnapshotServiceImplS1-0206.订单池快照业务层处理
 *@author yelq
 *@date 2025-12-18
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@Slf4j
@Service
public class DpOrderPoolSnapshotServiceImpl extends BaseService<DpOrderPoolSnapshot>  implements IDpOrderPoolSnapshotService
{
    @Autowired
    private DpOrderPoolSnapshotEntityMapper dpOrderPoolSnapshotEntityMapper;

    /**
     * 查询S1-0206.订单池快照
     * 
     * @param id S1-0206.订单池快照主键
     * @return S1-0206.订单池快照
     */
    @Override
    public DpOrderPoolSnapshot selectDpOrderPoolSnapshotById(Long id)
    {
        return dpOrderPoolSnapshotEntityMapper.selectDpOrderPoolSnapshotById(id);
    }

    /**
     * 查询S1-0206.订单池快照列表
     * 
     * @param dpOrderPoolSnapshot S1-0206.订单池快照
     * @return S1-0206.订单池快照
     */
    @Override
    public List<DpOrderPoolSnapshot> selectDpOrderPoolSnapshotList(DpOrderPoolSnapshot dpOrderPoolSnapshot)
    {
        return dpOrderPoolSnapshotEntityMapper.selectDpOrderPoolSnapshotList(dpOrderPoolSnapshot);
    }

    /**
     * 批量查询S1-0206.订单池快照列表
     *
     * @param ids 需要查询的数据主键集合
     * @return S1-0206.订单池快照集合
     */
    @Override
    public List<DpOrderPoolSnapshot> selectDpOrderPoolSnapshotByIds(List<Long> ids)
    {
        return super.executeSelectIn(
                    dpOrderPoolSnapshotEntityMapper::selectDpOrderPoolSnapshotByIds
                    ,ids
        );
    }


    /**
     * 新增S1-0206.订单池快照
     * 
     * @param dpOrderPoolSnapshot S1-0206.订单池快照
     * @return 结果
     */
    @Override
    public int insertDpOrderPoolSnapshot(DpOrderPoolSnapshot dpOrderPoolSnapshot)
    {
        dpOrderPoolSnapshot.setBaseVale(null);
        return dpOrderPoolSnapshotEntityMapper.insert(dpOrderPoolSnapshot);
    }

    /**
     * 修改S1-0206.订单池快照
     * 
     * @param dpOrderPoolSnapshot S1-0206.订单池快照
     * @return 结果
     */
    @Override
    public int updateDpOrderPoolSnapshot(DpOrderPoolSnapshot dpOrderPoolSnapshot)
    {
        dpOrderPoolSnapshot.setBaseVale(dpOrderPoolSnapshot.getId());
        return dpOrderPoolSnapshotEntityMapper.update(dpOrderPoolSnapshot);
    }

    /**
     * 批量删除S1-0206.订单池快照
     * 
     * @param ids 需要删除的S1-0206.订单池快照主键
     * @return 结果
     */
    @Override
    public int deleteDpOrderPoolSnapshotByIds(Long[] ids)
    {
        return dpOrderPoolSnapshotEntityMapper.deleteDpOrderPoolSnapshotByIds(ids);
    }

    /**
     * 批量删除S1-0206.订单池快照
     *
     * @param ids 需要删除的S1-0206.订单池快照主键
     * @return 结果
     */
    @Override
    public int deleteDpOrderPoolSnapshotByIds(List<Long> ids)
    {
        Long[] arrayids = ids.toArray(new Long[0]);

        return this.deleteDpOrderPoolSnapshotByIds(arrayids);
    }

    /**
     * 删除S1-0206.订单池快照信息
     * 
     * @param id S1-0206.订单池快照主键
     * @return 结果
     */
    @Override
    public int deleteDpOrderPoolSnapshotById(Long id)
    {
        return dpOrderPoolSnapshotEntityMapper.deleteDpOrderPoolSnapshotById(id);
    }

    @Override
    public void insertBatchData(Collection<DpOrderPoolSnapshot> dataList) {

        this.insertBatchData(dataList, DpOrderPoolSnapshotEntityMapper.class);
    }

    @Override
    public void updateBatchData(Collection<DpOrderPoolSnapshot> dataList) {

        this.updateBatchData(dataList, DpOrderPoolSnapshotEntityMapper.class);
    }

    @Override
    public void mergerIntoBatchData(List<DpOrderPoolSnapshot> list) {
        this.mergerIntoBatchData(list, DpOrderPoolSnapshotEntityMapper.class);
    }

    /**
     * 校验S1-0206.订单池快照唯一性
     */
    @Override
    public String checkDpOrderPoolSnapshotUnique(DpOrderPoolSnapshot dpOrderPoolSnapshot) {
        if (dpOrderPoolSnapshot == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<DpOrderPoolSnapshot> list = dpOrderPoolSnapshotEntityMapper.selectDpOrderPoolSnapshotList(dpOrderPoolSnapshot);
        if (CollectionUtils.isNotEmpty(list)) {
            long iCount = list.stream().filter(x->!x.getId().equals(dpOrderPoolSnapshot.getId())).count();
            return iCount == 0 ? UserConstants.UNIQUE : UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }
    /**
     * 导入S1-0206.订单池快照数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<DpOrderPoolSnapshot> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<DpOrderPoolSnapshot> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            DpOrderPoolSnapshot dpOrderPoolSnapshot = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, dpOrderPoolSnapshot);
            ImportExcelValidatedUtils.validatedRepeat(list,dpOrderPoolSnapshot,i,2,importLogId,validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                dpOrderPoolSnapshot.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                dpOrderPoolSnapshot.setBaseVale(null);
                importList.add(dpOrderPoolSnapshot);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                    dpOrderPoolSnapshotEntityMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    DpOrderPoolSnapshot dpOrderPoolSnapshot = list.get(i);
                    // 错误记录跳过
                    if (dpOrderPoolSnapshot.getId() != null && dpOrderPoolSnapshot.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkDpOrderPoolSnapshotUnique(dpOrderPoolSnapshot);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertDpOrderPoolSnapshot(dpOrderPoolSnapshot);
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
    public void saveOrderPoolSnapshot(DpDemandPlan createCondition, List<SalesOrderPool> salesOrders, List<SupplyOrderPool> supplyOrderPools) {
        List<DpOrderPoolSnapshot> orderPoolSnapshots = Lists.newArrayList();
        if(CollectionUtils.isNotEmpty(salesOrders)){
            salesOrders.forEach(saleOrder -> orderPoolSnapshots.add(buildOrderPoolSnapshot(createCondition,saleOrder)));
        }
        if(CollectionUtils.isNotEmpty(supplyOrderPools)){
            supplyOrderPools.forEach(supplyOrder -> orderPoolSnapshots.add(buildOrderPoolSnapshot(createCondition,supplyOrder)));
        }
        if(CollectionUtils.isNotEmpty(orderPoolSnapshots)){
            this.insertBatchData(orderPoolSnapshots,DpOrderPoolSnapshotEntityMapper.class);
        }
    }



    private DpOrderPoolSnapshot buildOrderPoolSnapshot(DpDemandPlan createCondition, SalesOrderPool saleOrder) {
        DpOrderPoolSnapshot entity = new DpOrderPoolSnapshot();
        BeanUtils.copyProperties(saleOrder, entity);
        entity.setId(null);
        entity.setBaseVale(null);
        entity.setYear(createCondition.getYear());
        entity.setMonth(createCondition.getMonth());
        entity.setMonthPlanVersion(createCondition.getMonthPlanVersion());
        //entity.setMesMaterialCode(saleOrder.);
        entity.setMaterialCode(saleOrder.getOriMaterialCode());
        entity.setAreaCode(saleOrder.getArea());
        entity.setCustomCode(saleOrder.getSalCode());
        // entity.setCustomName();
        entity.setCustomNationCode(saleOrder.getSalNCode());
        entity.setDemandQty(saleOrder.getOrdQty() == null?0L:saleOrder.getOrdQty().longValue());
        entity.setDestinationNationCode(saleOrder.getNatCode());
        entity.setIsDynamicBalance(saleOrder.getDynamicBalance());
        entity.setIsUniformity(saleOrder.getUniformity());
        entity.setPoNumber(saleOrder.getSalCodePo());
        entity.setSubmitDate(saleOrder.getBillDate());
        entity.setScmId(saleOrder.getScmDetailId());
        //entity.setPredictionVersion();
        return entity;
    }

    private DpOrderPoolSnapshot buildOrderPoolSnapshot(DpDemandPlan createCondition, SupplyOrderPool supplyOrder) {
        DpOrderPoolSnapshot entity = new DpOrderPoolSnapshot();
        BeanUtils.copyProperties(supplyOrder, entity);
        entity.setId(null);
        entity.setBaseVale(null);
        entity.setYear(createCondition.getYear());
        entity.setMonth(createCondition.getMonth());
        entity.setMonthPlanVersion(createCondition.getMonthPlanVersion());
        entity.setOrderPriority(supplyOrder.getOrderType());
        // entity.setAreaCode();
        // entity.setCustomCode();
        // entity.setCustomName();
        // entity.setCustomNationCode();
        entity.setDemandQty(supplyOrder.getQty());
        // entity.setDestinationNationCode();
        // entity.setIsDynamicBalance();
        // entity.setIsUniformity();
        // entity.setPoNumber();
        // entity.setSubmitDate();
        // entity.setScmId();
        return entity;
    }
}
