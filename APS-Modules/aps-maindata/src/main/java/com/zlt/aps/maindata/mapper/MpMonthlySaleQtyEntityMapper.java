package com.zlt.aps.maindata.mapper;

import java.util.List;
import java.util.Collection;
import com.ruoyi.common.datasource.service.IBaseMapper;
import com.zlt.aps.monthplan.api.domain.entity.MpMonthlySaleQty;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpMonthlySaleQtyMapper.java
 * 描    述：月均销量Mapper接口
 *@author yelq
 *@date 2025-12-11
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */

public interface MpMonthlySaleQtyEntityMapper extends IBaseMapper<MpMonthlySaleQty>
{
    /**
     * 查询月均销量
     * 
     * @param id 月均销量主键
     * @return 月均销量
     */
    public MpMonthlySaleQty selectMpMonthlySaleQtyById(Long id);

    /**
     * 查询月均销量列表
     * 
     * @param mpMonthlySaleQty 月均销量
     * @return 月均销量集合
     */
    public List<MpMonthlySaleQty> selectMpMonthlySaleQtyList(MpMonthlySaleQty mpMonthlySaleQty);

    /**
     * 批量查询月均销量列表
     *
     * @param ids 需要查询的数据主键集合
     * @return 月均销量集合
     */
    public List<MpMonthlySaleQty> selectMpMonthlySaleQtyByIds(List<Long> ids);

    /**
     * 删除月均销量
     * 
     * @param id 月均销量主键
     * @return 结果
     */
    public int deleteMpMonthlySaleQtyById(Long id);

    /**
     * 批量删除月均销量
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMpMonthlySaleQtyByIds(Long[] ids);


    /**
 * 合并操作，如果记录存在则更新，否则新增
 */
    @Override
    public int mergeSql(Collection<? extends MpMonthlySaleQty> collection);
}
