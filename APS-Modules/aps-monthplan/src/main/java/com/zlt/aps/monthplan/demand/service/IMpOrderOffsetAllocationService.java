package com.zlt.aps.monthplan.demand.service;

import java.util.List;
import com.ruoyi.common.datasource.service.IBaseService;
import com.zlt.aps.monthplan.api.domain.entity.MpOrderOffsetAllocation;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.web.domain.AjaxResult;
/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpOrderOffsetAllocationService.java
 * 描    述：IMpOrderOffsetAllocationService订单冲减分配后端接口
 *@author yelq
 *@date 2025-12-17
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
public interface IMpOrderOffsetAllocationService  extends IBaseService<MpOrderOffsetAllocation>
{
    /**
     * 查询订单冲减分配
     * 
     * @param id 订单冲减分配主键
     * @return 订单冲减分配
     */
    public MpOrderOffsetAllocation selectMpOrderOffsetAllocationById(Long id);

    /**
     * 查询订单冲减分配列表
     * 
     * @param mpOrderOffsetAllocation 订单冲减分配
     * @return 订单冲减分配集合
     */
    public List<MpOrderOffsetAllocation> selectMpOrderOffsetAllocationList(MpOrderOffsetAllocation mpOrderOffsetAllocation);

    /**
     * 批量查询订单冲减分配列表
     *
     * @param ids 需要查询的数据主键集合
     * @return 订单冲减分配集合
     */
    public List<MpOrderOffsetAllocation> selectMpOrderOffsetAllocationByIds(List<Long> ids);


    /**
     * 新增订单冲减分配
     * 
     * @param mpOrderOffsetAllocation 订单冲减分配
     * @return 结果
     */
    @Transactional
    public int insertMpOrderOffsetAllocation(MpOrderOffsetAllocation mpOrderOffsetAllocation);

    /**
     * 修改订单冲减分配
     * 
     * @param mpOrderOffsetAllocation 订单冲减分配
     * @return 结果
     */
    @Transactional
    public int updateMpOrderOffsetAllocation(MpOrderOffsetAllocation mpOrderOffsetAllocation);

    /**
     * 批量删除订单冲减分配
     * 
     * @param ids 需要删除的订单冲减分配主键集合
     * @return 结果
     */
   
    @Transactional
    public int deleteMpOrderOffsetAllocationByIds(Long[] ids);

    /**
     * 批量删除订单冲减分配
     *
     * @param ids 需要删除的订单冲减分配主键集合
     * @return 结果
     */

    @Transactional
    public int deleteMpOrderOffsetAllocationByIds(List<Long> ids);

    /**
     * 删除订单冲减分配信息
     * 
     * @param id 订单冲减分配主键
     * @return 结果
     */
    @Transactional
    public int deleteMpOrderOffsetAllocationById(Long id);

    /**
     * 校验订单冲减分配唯一性
     */
    public String checkMpOrderOffsetAllocationUnique(MpOrderOffsetAllocation mpOrderOffsetAllocation);

    /**
     * 导入订单冲减分配数据
     */
    @Transactional
    public AjaxResult importData(List<MpOrderOffsetAllocation> list, boolean updateSupport, Long importLogId);
}
