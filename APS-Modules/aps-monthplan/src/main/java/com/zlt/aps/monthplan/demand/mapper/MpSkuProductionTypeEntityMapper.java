package com.zlt.aps.monthplan.demand.mapper;

import java.util.List;

import com.ruoyi.common.datasource.service.IBaseMapper;
import com.zlt.aps.monthplan.api.domain.entity.MpSkuProductionType;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpSkuProductionTypeMapper.java
 * 描    述：SKU排产分类Mapper接口
 *@author yelq
 *@date 2025-12-17
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */

public interface MpSkuProductionTypeEntityMapper extends IBaseMapper<MpSkuProductionType>
{
    /**
     * 查询SKU排产分类
     * 
     * @param id SKU排产分类主键
     * @return SKU排产分类
     */
    MpSkuProductionType selectMpSkuProductionTypeById(Long id);

    /**
     * 查询SKU排产分类列表
     * 
     * @param mpSkuProductionType SKU排产分类
     * @return SKU排产分类集合
     */
    List<MpSkuProductionType> selectMpSkuProductionTypeList(MpSkuProductionType mpSkuProductionType);

    /**
     * 批量查询SKU排产分类列表
     *
     * @param ids 需要查询的数据主键集合
     * @return SKU排产分类集合
     */
    List<MpSkuProductionType> selectMpSkuProductionTypeByIds(List<Long> ids);

    /**
     * 删除SKU排产分类
     * 
     * @param id SKU排产分类主键
     * @return 结果
     */
    int deleteMpSkuProductionTypeById(Long id);

    /**
     * 批量删除SKU排产分类
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    int deleteMpSkuProductionTypeByIds(Long[] ids);
}
