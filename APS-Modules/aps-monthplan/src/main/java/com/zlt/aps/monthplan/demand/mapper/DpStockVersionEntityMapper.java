package com.zlt.aps.monthplan.demand.mapper;

import com.ruoyi.common.datasource.service.IBaseMapper;
import com.zlt.aps.monthplan.api.domain.entity.DpStockVersion;

import java.util.List;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：DpStockVersionMapper.java
 * 描    述：需求计划_版本库存Mapper接口
 *@author yelq
 *@date 2025-12-20
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */

public interface DpStockVersionEntityMapper extends IBaseMapper<DpStockVersion>
{
    /**
     * 查询需求计划_版本库存
     * 
     * @param id 需求计划_版本库存主键
     * @return 需求计划_版本库存
     */
    DpStockVersion selectDpStockVersionById(Long id);

    /**
     * 查询需求计划_版本库存列表
     * 
     * @param dpStockVersion 需求计划_版本库存
     * @return 需求计划_版本库存集合
     */
    List<DpStockVersion> selectDpStockVersionList(DpStockVersion dpStockVersion);

    /**
     * 批量查询需求计划_版本库存列表
     *
     * @param ids 需要查询的数据主键集合
     * @return 需求计划_版本库存集合
     */
    List<DpStockVersion> selectDpStockVersionByIds(List<Long> ids);

    /**
     * 删除需求计划_版本库存
     * 
     * @param id 需求计划_版本库存主键
     * @return 结果
     */
    int deleteDpStockVersionById(Long id);

    /**
     * 批量删除需求计划_版本库存
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    int deleteDpStockVersionByIds(Long[] ids);
}
