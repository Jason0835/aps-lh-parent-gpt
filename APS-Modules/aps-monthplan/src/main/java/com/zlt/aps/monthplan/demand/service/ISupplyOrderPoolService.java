package com.zlt.aps.monthplan.demand.service;

import java.util.List;
import com.ruoyi.common.datasource.service.IBaseService;
import com.zlt.aps.monthplan.api.domain.entity.SupplyOrderPool;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.web.domain.AjaxResult;
/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ISupplyOrderPoolService.java
 * 描    述：ISupplyOrderPoolService供应链订单池后端接口
 *@author zlt
 *@date 2025-12-06
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
public interface ISupplyOrderPoolService  extends IBaseService<SupplyOrderPool>
{
    /**
     * 查询供应链订单池
     * 
     * @param id 供应链订单池主键
     * @return 供应链订单池
     */
    public SupplyOrderPool selectSupplyOrderPoolById(Long id);

    /**
     * 查询供应链订单池列表
     * 
     * @param supplyOrderPool 供应链订单池
     * @return 供应链订单池集合
     */
    public List<SupplyOrderPool> selectSupplyOrderPoolList(SupplyOrderPool supplyOrderPool);

    /**
     * 批量查询供应链订单池列表
     *
     * @param ids 需要查询的数据主键集合
     * @return 供应链订单池集合
     */
    public List<SupplyOrderPool> selectSupplyOrderPoolByIds(List<Long> ids);


    /**
     * 新增供应链订单池
     * 
     * @param supplyOrderPool 供应链订单池
     * @return 结果
     */
    @Transactional
    public int insertSupplyOrderPool(SupplyOrderPool supplyOrderPool);

    /**
     * 修改供应链订单池
     * 
     * @param supplyOrderPool 供应链订单池
     * @return 结果
     */
    @Transactional
    public int updateSupplyOrderPool(SupplyOrderPool supplyOrderPool);

    /**
     * 批量删除供应链订单池
     * 
     * @param ids 需要删除的供应链订单池主键集合
     * @return 结果
     */
   
    @Transactional
    public int deleteSupplyOrderPoolByIds(Long[] ids);

    /**
     * 批量删除供应链订单池
     *
     * @param ids 需要删除的供应链订单池主键集合
     * @return 结果
     */

    @Transactional
    public int deleteSupplyOrderPoolByIds(List<Long> ids);

    /**
     * 删除供应链订单池信息
     * 
     * @param id 供应链订单池主键
     * @return 结果
     */
    @Transactional
    public int deleteSupplyOrderPoolById(Long id);

    /**
     * 校验供应链订单池唯一性
     */
    public String checkSupplyOrderPoolUnique(SupplyOrderPool supplyOrderPool);

    /**
     * 导入供应链订单池数据
     */
    @Transactional
    public AjaxResult importData(List<SupplyOrderPool> list, boolean updateSupport, Long importLogId);
    /**
     * 生成周期排产储备
     * @param supplyOrderPool
     */
    @Transactional
    void createCycleStockUp(SupplyOrderPool supplyOrderPool);
    /**
     *  生产常规储备
     * @param supplyOrderPool
     */
    void createPrecedentStockUp(SupplyOrderPool supplyOrderPool);
}
