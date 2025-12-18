package com.zlt.aps.monthplan.demand.service;

import java.util.List;
import com.ruoyi.common.datasource.service.IBaseService;
import com.zlt.aps.monthplan.api.domain.entity.DpOrderPoolSnapshot;
import com.zlt.aps.monthplan.api.domain.entity.MpDemandPlan;
import com.zlt.aps.monthplan.api.domain.entity.SalesOrderPool;
import com.zlt.aps.monthplan.api.domain.entity.SupplyOrderPool;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.web.domain.AjaxResult;
/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IDpOrderPoolSnapshotService.java
 * 描    述：IDpOrderPoolSnapshotServiceS1-0206.订单池快照后端接口
 *@author yelq
 *@date 2025-12-18
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
public interface IDpOrderPoolSnapshotService  extends IBaseService<DpOrderPoolSnapshot>
{
    /**
     * 查询S1-0206.订单池快照
     * 
     * @param id S1-0206.订单池快照主键
     * @return S1-0206.订单池快照
     */
    DpOrderPoolSnapshot selectDpOrderPoolSnapshotById(Long id);

    /**
     * 查询S1-0206.订单池快照列表
     * 
     * @param dpOrderPoolSnapshot S1-0206.订单池快照
     * @return S1-0206.订单池快照集合
     */
    List<DpOrderPoolSnapshot> selectDpOrderPoolSnapshotList(DpOrderPoolSnapshot dpOrderPoolSnapshot);

    /**
     * 批量查询S1-0206.订单池快照列表
     *
     * @param ids 需要查询的数据主键集合
     * @return S1-0206.订单池快照集合
     */
    List<DpOrderPoolSnapshot> selectDpOrderPoolSnapshotByIds(List<Long> ids);


    /**
     * 新增S1-0206.订单池快照
     * 
     * @param dpOrderPoolSnapshot S1-0206.订单池快照
     * @return 结果
     */
    @Transactional
    int insertDpOrderPoolSnapshot(DpOrderPoolSnapshot dpOrderPoolSnapshot);

    /**
     * 修改S1-0206.订单池快照
     * 
     * @param dpOrderPoolSnapshot S1-0206.订单池快照
     * @return 结果
     */
    @Transactional
    int updateDpOrderPoolSnapshot(DpOrderPoolSnapshot dpOrderPoolSnapshot);

    /**
     * 批量删除S1-0206.订单池快照
     * 
     * @param ids 需要删除的S1-0206.订单池快照主键集合
     * @return 结果
     */
   
    @Transactional
    int deleteDpOrderPoolSnapshotByIds(Long[] ids);

    /**
     * 批量删除S1-0206.订单池快照
     *
     * @param ids 需要删除的S1-0206.订单池快照主键集合
     * @return 结果
     */

    @Transactional
    int deleteDpOrderPoolSnapshotByIds(List<Long> ids);

    /**
     * 删除S1-0206.订单池快照信息
     * 
     * @param id S1-0206.订单池快照主键
     * @return 结果
     */
    @Transactional
    int deleteDpOrderPoolSnapshotById(Long id);

    /**
     * 校验S1-0206.订单池快照唯一性
     */
    String checkDpOrderPoolSnapshotUnique(DpOrderPoolSnapshot dpOrderPoolSnapshot);
    /**
     * 导入S1-0206.订单池快照数据
     */
    @Transactional
    AjaxResult importData(List<DpOrderPoolSnapshot> list, boolean updateSupport, Long importLogId);
    /**
     * 保存订单池快照数据
     * @param createCondition
     * @param salesOrders
     * @param supplyOrderPools
     */
    void saveOrderPoolSnapshot(MpDemandPlan createCondition, List<SalesOrderPool> salesOrders, List<SupplyOrderPool> supplyOrderPools);
}
