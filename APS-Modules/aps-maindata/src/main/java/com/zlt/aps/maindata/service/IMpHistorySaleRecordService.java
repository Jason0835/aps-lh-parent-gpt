package com.zlt.aps.maindata.service;

import java.util.List;
import com.ruoyi.common.datasource.service.IBaseService;
import com.zlt.aps.monthplan.api.domain.entity.MpHistorySaleRecord;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.web.domain.AjaxResult;
/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpHistorySaleRecordService.java
 * 描    述：IMpHistorySaleRecordService历史销售记录后端接口
 *@author yelq
 *@date 2025-12-11
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
public interface IMpHistorySaleRecordService  extends IBaseService<MpHistorySaleRecord>
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
     * 新增历史销售记录
     * 
     * @param mpHistorySaleRecord 历史销售记录
     * @return 结果
     */
    @Transactional
    public int insertMpHistorySaleRecord(MpHistorySaleRecord mpHistorySaleRecord);

    /**
     * 修改历史销售记录
     * 
     * @param mpHistorySaleRecord 历史销售记录
     * @return 结果
     */
    @Transactional
    public int updateMpHistorySaleRecord(MpHistorySaleRecord mpHistorySaleRecord);

    /**
     * 批量删除历史销售记录
     * 
     * @param ids 需要删除的历史销售记录主键集合
     * @return 结果
     */
   
    @Transactional
    public int deleteMpHistorySaleRecordByIds(Long[] ids);

    /**
     * 批量删除历史销售记录
     *
     * @param ids 需要删除的历史销售记录主键集合
     * @return 结果
     */

    @Transactional
    public int deleteMpHistorySaleRecordByIds(List<Long> ids);

    /**
     * 删除历史销售记录信息
     * 
     * @param id 历史销售记录主键
     * @return 结果
     */
    @Transactional
    public int deleteMpHistorySaleRecordById(Long id);

    /**
     * 校验历史销售记录唯一性
     */
    public String checkMpHistorySaleRecordUnique(MpHistorySaleRecord mpHistorySaleRecord);

    /**
     * 导入历史销售记录数据
     */
    @Transactional
    public AjaxResult importData(List<MpHistorySaleRecord> list, boolean updateSupport, Long importLogId);
}
