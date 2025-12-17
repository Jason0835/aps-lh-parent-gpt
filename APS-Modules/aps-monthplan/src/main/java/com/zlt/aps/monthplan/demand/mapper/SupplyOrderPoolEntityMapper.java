package com.zlt.aps.monthplan.demand.mapper;

import java.util.List;
import java.util.Collection;
import com.ruoyi.common.datasource.service.IBaseMapper;
import com.zlt.aps.monthplan.api.domain.entity.SupplyOrderPool;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：SupplyOrderPoolMapper.java
 * 描    述：供应链订单池Mapper接口
 *@author zlt
 *@date 2025-12-06
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

public interface SupplyOrderPoolEntityMapper extends IBaseMapper<SupplyOrderPool>
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
     * 删除供应链订单池
     * 
     * @param id 供应链订单池主键
     * @return 结果
     */
    public int deleteSupplyOrderPoolById(Long id);

    /**
     * 批量删除供应链订单池
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSupplyOrderPoolByIds(Long[] ids);
}
