package com.zlt.aps.monthplan.demand.mapper;

import java.util.List;

import com.ruoyi.common.datasource.service.IBaseMapper;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductStock;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmProductStockMapper.java
 * 描    述：成品库存Mapper接口
 *@author yelq
 *@date 2025-12-20
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */

public interface MdmProductStockEntityMapper extends IBaseMapper<MdmProductStock>
{
    /**
     * 查询成品库存
     * 
     * @param id 成品库存主键
     * @return 成品库存
     */
    MdmProductStock selectMdmProductStockById(Long id);

    /**
     * 查询成品库存列表
     * 
     * @param mdmProductStock 成品库存
     * @return 成品库存集合
     */
    List<MdmProductStock> selectMdmProductStockList(MdmProductStock mdmProductStock);

    /**
     * 批量查询成品库存列表
     *
     * @param ids 需要查询的数据主键集合
     * @return 成品库存集合
     */
    List<MdmProductStock> selectMdmProductStockByIds(List<Long> ids);

    /**
     * 删除成品库存
     * 
     * @param id 成品库存主键
     * @return 结果
     */
    int deleteMdmProductStockById(Long id);

    /**
     * 批量删除成品库存
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    int deleteMdmProductStockByIds(Long[] ids);
}
