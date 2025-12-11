package com.zlt.aps.maindata.mapper;

import java.util.List;
import java.util.Collection;
import com.ruoyi.common.datasource.service.IBaseMapper;
import com.zlt.aps.monthplan.api.domain.entity.MpHistorySaleRecord;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpHistorySaleRecordMapper.java
 * 描    述：历史销售记录Mapper接口
 *@author yelq
 *@date 2025-12-11
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */

public interface MpHistorySaleRecordEntityMapper extends IBaseMapper<MpHistorySaleRecord>
{
    /**
     * 查询历史销售记录
     * 
     * @param id 历史销售记录主键
     * @return 历史销售记录
     */
    public MpHistorySaleRecord selectMpHistorySaleRecordById(Long id);

    /**
     * 查询历史销售记录列表
     * 
     * @param mpHistorySaleRecord 历史销售记录
     * @return 历史销售记录集合
     */
    public List<MpHistorySaleRecord> selectMpHistorySaleRecordList(MpHistorySaleRecord mpHistorySaleRecord);

    /**
     * 批量查询历史销售记录列表
     *
     * @param ids 需要查询的数据主键集合
     * @return 历史销售记录集合
     */
    public List<MpHistorySaleRecord> selectMpHistorySaleRecordByIds(List<Long> ids);

    /**
     * 删除历史销售记录
     * 
     * @param id 历史销售记录主键
     * @return 结果
     */
    public int deleteMpHistorySaleRecordById(Long id);

    /**
     * 批量删除历史销售记录
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMpHistorySaleRecordByIds(Long[] ids);


    /**
 * 合并操作，如果记录存在则更新，否则新增
 */
    @Override
    public int mergeSql(Collection<? extends MpHistorySaleRecord> collection);
}
