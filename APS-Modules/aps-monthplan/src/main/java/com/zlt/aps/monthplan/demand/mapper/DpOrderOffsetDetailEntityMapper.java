package com.zlt.aps.monthplan.demand.mapper;

import java.util.List;

import com.ruoyi.common.datasource.service.IBaseMapper;
import com.zlt.aps.monthplan.api.domain.entity.DpOrderOffsetDetail;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：DpOrderOffsetDetailMapper.java
 * 描    述：S1-0604订单冲减分配Mapper接口
 *@author yelq
 *@date 2025-12-21
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */

public interface DpOrderOffsetDetailEntityMapper extends IBaseMapper<DpOrderOffsetDetail>
{
    /**
     * 查询S1-0604订单冲减分配
     * 
     * @param id S1-0604订单冲减分配主键
     * @return S1-0604订单冲减分配
     */
    DpOrderOffsetDetail selectDpOrderOffsetDetailById(Long id);

    /**
     * 查询S1-0604订单冲减分配列表
     * 
     * @param dpOrderOffsetDetail S1-0604订单冲减分配
     * @return S1-0604订单冲减分配集合
     */
    List<DpOrderOffsetDetail> selectDpOrderOffsetDetailList(DpOrderOffsetDetail dpOrderOffsetDetail);

    /**
     * 批量查询S1-0604订单冲减分配列表
     *
     * @param ids 需要查询的数据主键集合
     * @return S1-0604订单冲减分配集合
     */
    List<DpOrderOffsetDetail> selectDpOrderOffsetDetailByIds(List<Long> ids);

    /**
     * 删除S1-0604订单冲减分配
     * 
     * @param id S1-0604订单冲减分配主键
     * @return 结果
     */
    int deleteDpOrderOffsetDetailById(Long id);

    /**
     * 批量删除S1-0604订单冲减分配
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    int deleteDpOrderOffsetDetailByIds(Long[] ids);
}
