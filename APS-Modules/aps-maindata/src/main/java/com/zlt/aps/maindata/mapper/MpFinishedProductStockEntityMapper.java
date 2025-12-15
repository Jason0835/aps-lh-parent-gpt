package com.zlt.aps.maindata.mapper;

import java.util.List;

import com.ruoyi.common.datasource.service.IBaseMapper;
import com.zlt.aps.monthplan.api.domain.entity.MpFinishedProductStock;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpFinishedProductStockMapper.java
 * 描    述：成品库存Mapper接口
 *@author yelq
 *@date 2025-12-15
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */

public interface MpFinishedProductStockEntityMapper extends IBaseMapper<MpFinishedProductStock>
{
    /**
     * 查询成品库存
     * 
     * @param id 成品库存主键
     * @return 成品库存
     */
    MpFinishedProductStock selectMpFinishedProductStockById(Long id);

    /**
     * 查询成品库存列表
     * 
     * @param mpFinishedProductStock 成品库存
     * @return 成品库存集合
     */
    List<MpFinishedProductStock> selectMpFinishedProductStockList(MpFinishedProductStock mpFinishedProductStock);

    /**
     * 批量查询成品库存列表
     *
     * @param ids 需要查询的数据主键集合
     * @return 成品库存集合
     */
    List<MpFinishedProductStock> selectMpFinishedProductStockByIds(List<Long> ids);

    /**
     * 删除成品库存
     * 
     * @param id 成品库存主键
     * @return 结果
     */
    int deleteMpFinishedProductStockById(Long id);

    /**
     * 批量删除成品库存
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    int deleteMpFinishedProductStockByIds(Long[] ids);
}
