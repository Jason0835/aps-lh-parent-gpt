package com.zlt.aps.monthplan.demand.mapper;

import java.util.List;

import com.ruoyi.common.datasource.service.IBaseMapper;
import com.zlt.aps.monthplan.api.domain.entity.DpOrderPoolSnapshot;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：DpOrderPoolSnapshotMapper.java
 * 描    述：S1-0206.订单池快照Mapper接口
 *@author yelq
 *@date 2025-12-18
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */

public interface DpOrderPoolSnapshotEntityMapper extends IBaseMapper<DpOrderPoolSnapshot>
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
     * 删除S1-0206.订单池快照
     * 
     * @param id S1-0206.订单池快照主键
     * @return 结果
     */
    int deleteDpOrderPoolSnapshotById(Long id);

    /**
     * 批量删除S1-0206.订单池快照
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    int deleteDpOrderPoolSnapshotByIds(Long[] ids);
}
