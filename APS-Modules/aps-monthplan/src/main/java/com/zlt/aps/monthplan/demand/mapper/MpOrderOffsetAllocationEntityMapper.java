package com.zlt.aps.monthplan.demand.mapper;

import java.util.List;
import java.util.Collection;
import com.ruoyi.common.datasource.service.IBaseMapper;
import com.zlt.aps.monthplan.api.domain.entity.MpOrderOffsetAllocation;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpOrderOffsetAllocationMapper.java
 * 描    述：订单冲减分配Mapper接口
 *@author yelq
 *@date 2025-12-15
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */

public interface MpOrderOffsetAllocationEntityMapper extends IBaseMapper<MpOrderOffsetAllocation>
{
    /**
     * 查询订单冲减分配
     * 
     * @param id 订单冲减分配主键
     * @return 订单冲减分配
     */
    MpOrderOffsetAllocation selectMpOrderOffsetAllocationById(Long id);

    /**
     * 查询订单冲减分配列表
     * 
     * @param mpOrderOffsetAllocation 订单冲减分配
     * @return 订单冲减分配集合
     */
    List<MpOrderOffsetAllocation> selectMpOrderOffsetAllocationList(MpOrderOffsetAllocation mpOrderOffsetAllocation);

    /**
     * 批量查询订单冲减分配列表
     *
     * @param ids 需要查询的数据主键集合
     * @return 订单冲减分配集合
     */
    List<MpOrderOffsetAllocation> selectMpOrderOffsetAllocationByIds(List<Long> ids);

    /**
     * 删除订单冲减分配
     * 
     * @param id 订单冲减分配主键
     * @return 结果
     */
    int deleteMpOrderOffsetAllocationById(Long id);

    /**
     * 批量删除订单冲减分配
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    int deleteMpOrderOffsetAllocationByIds(Long[] ids);
}
